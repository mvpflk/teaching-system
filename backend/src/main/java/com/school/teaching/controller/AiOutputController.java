package com.school.teaching.controller;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.entity.AiOutput;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AiContentGeneratorService;
import com.school.teaching.service.AiTaskStore;
import com.school.teaching.service.SystemService;
import com.school.teaching.service.impl.AsyncTaskService;
import com.school.teaching.service.impl.AiExamPaperServiceImpl;
import com.school.teaching.service.WordExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai-output")
@Tag(name = "AI产出", description = "AI生成内容管理：诊断、组卷、巩固材料、SSE流式输出")
public class AiOutputController {

    @Autowired private AiContentGeneratorService aiContentService;
    @Autowired private AiTaskStore taskStore;
    @Autowired private SystemService systemService;
    @Autowired private AsyncTaskService asyncTaskService;
    @Autowired private WordExportService wordExportService;
    @Autowired private AiExamPaperServiceImpl examPaperService;
    @Autowired
    private com.school.teaching.service.impl.ConsolidationMaterialService consolidationService;

    private static final Logger log = LoggerFactory.getLogger(AiOutputController.class);

    private boolean aiContentEnabled() {
        return systemService.getBooleanConfig("feature.ai_content_enabled", true);
    }

    @PostMapping("/generate")
    @Operation(summary = "提交AI生成任务", description = "提交异步AI生成任务，返回taskId供轮询")
    public R<?> submitGeneration(@RequestBody Map<String, Object> params) {
        if (!aiContentEnabled()) return R.error(410, "AI教学助手功能未启用");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");

        // 前置校验：文化课教师不能生成实训方案
        String contentType = (String) params.get("contentType");
        if ("PRACTICE_PLAN".equals(contentType)) {
            String subject = (String) params.getOrDefault("subject", "");
            if (isCultureSubject(subject)) {
                return R.error(400, "语文/数学/英语学科不支持实训方案，请使用综合练习或组卷功能");
            }
        }

        Long teacherId = SecurityUtils.getCurrentUserId();
        String taskId = aiContentService.submitGeneration(teacherId, params);
        return R.ok(Map.of("taskId", taskId, "status", "PENDING"));
    }

    private boolean isCultureSubject(String subject) {
        if (subject == null) return false;
        String bare = subject.replaceAll("\\[.*?\\]", "").trim();
        return bare.equals("语文") || bare.equals("数学") || bare.equals("英语");
    }

    @GetMapping("/result/{taskId}")
    @Operation(summary = "获取任务结果", description = "查询AI生成任务的结果状态")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    public R<?> getResult(@PathVariable String taskId) {
        AiTaskStore.TaskEntry entry = taskStore.get(taskId);
        if (entry == null) return R.error(404, "任务不存在");
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("status", entry.status);
        data.put("result", entry.result);
        data.put("error", entry.error);
        return R.ok(data);
    }

    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式获取结果", description = "通过Server-Sent Events实时获取AI生成结果")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    public SseEmitter stream(@PathVariable String taskId) {
        boolean sseEnabled = systemService.getBooleanConfig("feature.sse_enabled", true);
        if (!sseEnabled) throw new com.school.teaching.exception.BusinessException(503, "SSE未启用");
        AiTaskStore.TaskEntry entry = taskStore.get(taskId);
        if (entry != null && !"PENDING".equals(entry.status) && !"RUNNING".equals(entry.status)) {
            // 任务已完成/失败/超时，直接返回（RUNNING=PENDING，走长连接等待）
            SseEmitter immediate = new SseEmitter();
            Map<String, Object> event = new java.util.HashMap<>();
            event.put("status", entry.status);
            event.put("taskId", taskId);
            event.put("result", entry.result);
            event.put("error", entry.error);
            try {
                immediate.send(SseEmitter.event().name("task-result").data(event));
                immediate.complete();
            } catch (IOException e) { immediate.completeWithError(e); }
            return immediate;
        }
        log.debug("SSE注册 taskId={}", taskId);
        return taskStore.registerSse(taskId, 1_200_000); // 20分钟，支持55-100题分批生成
    }

    private static final Set<String> QUESTION_TYPES = Set.of("SYNC_EXERCISES", "COMPREHENSIVE_EXERCISES", "CLASSROOM_QUESTIONS", "KNOWLEDGE_PRACTICE");
    private static final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

    @GetMapping("/list")
    @Operation(summary = "分页获取AI产出列表", description = "获取AI生成内容的列表，支持类型和关键词筛选")
    @Parameter(name = "outputType", description = "产出类型: DIAGNOSIS/CONSOLIDATION_MATERIAL/SYNC_EXERCISES等")
    @Parameter(name = "keyword", description = "搜索关键词")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "pageSize", description = "每页数量", example = "20")
    public R<?> list(@RequestParam(required = false) String outputType,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "20") Integer pageSize) {
        if (!aiContentEnabled()) return R.error(410, "AI教学助手功能未启用");
        Long teacherId = SecurityUtils.getCurrentUserId();

        // 题目类型：逻辑已下沉到 AiContentGeneratorService.listQuestionBatches()
        if (outputType != null && QUESTION_TYPES.contains(outputType)) {
            return R.ok(aiContentService.listQuestionBatches(teacherId, outputType, keyword, page, pageSize));
        }

        // 非题目类型：从 ai_outputs 表查询
        return R.ok(aiContentService.listOutputs(teacherId, outputType, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取AI产出详情", description = "根据ID获取AI产出内容详情")
    @Parameter(name = "id", description = "产出ID", required = true, example = "123")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(aiContentService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新AI产出", description = "更新AI产出内容的标题或状态")
    @Parameter(name = "id", description = "产出ID", required = true, example = "123")
    @AuditLog(eventType = AuditEventType.OTHER, description = "编辑AI产出内容", targetTable = "ai_outputs")
    public R<?> updateOutput(@PathVariable Long id, @RequestBody AiOutput update) {
        aiContentService.updateOutput(id, update);
        return R.ok("已更新");
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布AI产出", description = "将AI产出发布为可见状态")
    @Parameter(name = "id", description = "产出ID", required = true, example = "123")
    @AuditLog(eventType = AuditEventType.TASK_PUBLISH, description = "发布AI产出", targetTable = "ai_outputs")
    public R<?> publish(@PathVariable Long id) {
        aiContentService.publish(id);
        return R.ok("已发布");
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档AI产出", description = "将AI产出归档")
    @Parameter(name = "id", description = "产出ID", required = true, example = "123")
    @AuditLog(eventType = AuditEventType.OTHER, description = "归档AI产出", targetTable = "ai_outputs")
    public R<?> archive(@PathVariable Long id) {
        aiContentService.archive(id);
        return R.ok("已归档");
    }

    // ═══════════ 异步任务管理 ═══════════

    /** 取消进行中的任务 */
    @PostMapping("/task/{taskId}/cancel")
    @Operation(summary = "取消AI任务", description = "取消进行中的AI生成任务")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @AuditLog(eventType = AuditEventType.OTHER, description = "取消AI生成任务", targetTable = "ai_task_store")
    public R<?> cancelTask(@PathVariable String taskId) {
        asyncTaskService.markCancelled(taskId);
        return R.ok("已取消");
    }

    /** 手动重试失败/超时任务 */
    @PostMapping("/task/{taskId}/retry")
    @Operation(summary = "重试AI任务", description = "手动重试失败或超时的AI生成任务")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    public R<?> retryTask(@PathVariable String taskId) {
        asyncTaskService.retry(taskId);
        return R.ok("已加入重试队列");
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "导出AI产出为Word", description = "将AI生成的内容导出为Word文档")
    @Parameter(name = "id", description = "产出ID", required = true, example = "123")
    public void exportWord(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) {
        AiOutput output = aiContentService.getById(id);
        if (output == null) { response.setStatus(404); return; }
        try {
            String markdown = output.getContent();
            // 题目类型：尝试从 content JSON 解析完整题目并生成带选项的 markdown
            if (output.getOutputType() != null && QUESTION_TYPES.contains(output.getOutputType())
                    && markdown != null && markdown.startsWith("{")) {
                try {
                    var root = om.readTree(markdown);
                    if (root.has("questions")) {
                        var qs = root.get("questions");
                        var sb = new StringBuilder();
                        sb.append("# ").append(output.getTitle() != null ? output.getTitle() : "AI出题").append("\n\n");
                        for (int i = 0; i < qs.size(); i++) {
                            var q = qs.get(i);
                            String qType = q.has("questionType") ? q.get("questionType").asText() : "";
                            String qText = q.has("questionText") ? q.get("questionText").asText() : "";
                            sb.append(i + 1).append(". ").append(qText).append("\n");
                            if (q.has("options") && q.get("options").isArray()) {
                                var opts = q.get("options");
                                String[] labels = {"A", "B", "C", "D", "E", "F"};
                                for (int j = 0; j < opts.size() && j < labels.length; j++) {
                                    sb.append("   ").append(labels[j]).append(". ").append(opts.get(j).asText()).append("\n");
                                }
                            }
                            if (q.has("correctAnswer")) sb.append("   **答案：").append(q.get("correctAnswer").asText()).append("**\n");
                            if (q.has("explanation")) sb.append("   解析：").append(q.get("explanation").asText()).append("\n");
                            sb.append("\n");
                        }
                        markdown = sb.toString();
                    }
                } catch (Exception ignored) { /* 回退到原始 content */ }
            }
            byte[] docx = wordExportService.exportMarkdown(output.getTitle(), markdown);
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                java.net.URLEncoder.encode((output.getTitle() != null ? output.getTitle() : "AI产出"), "UTF-8") + ".docx\"");
            response.getOutputStream().write(docx);
        } catch (Exception e) { response.setStatus(500); }
    }

    @PostMapping("/{id}/rate")
    @Operation(summary = "评价AI产出", description = "对AI生成的内容进行评分和反馈")
    @Parameter(name = "id", description = "产出ID", required = true, example = "123")
    @AuditLog(eventType = AuditEventType.OTHER, description = "评价AI产出", targetTable = "ai_outputs")
    public R<?> rate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer rating = body.get("rating") instanceof Number n ? n.intValue() : null;
        String feedback = body.get("feedback") instanceof String s ? s : null;
        if (rating != null && (rating < 1 || rating > 5)) return R.error(400, "评分需在1-5之间");
        aiContentService.rate(id, rating, feedback);
        return R.ok("已评价");
    }

    @PostMapping("/{id}/publish-as-task")
    @Operation(summary = "发布为实训任务", description = "将AI产出发布为实训任务")
    @Parameter(name = "id", description = "产出ID", required = true, example = "123")
    public R<?> publishAsTask(@PathVariable Long id, @RequestBody Map<String, Object> config) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Map<String, Object> result = aiContentService.publishAsTask(id, config);
        return R.ok(result, "实训任务已创建");
    }

    @PostMapping("/publish-questions-as-exam")
    @Operation(summary = "发布题目为考试", description = "将选中的题目发布为考试任务")
    public R<?> publishQuestionsAsExam(@RequestBody Map<String, Object> config) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        @SuppressWarnings("unchecked")
        var qids = (java.util.List<Integer>) config.get("questionIds");
        List<Long> ids = qids.stream().map(Integer::longValue).toList();
        Long teacherId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = aiContentService.publishQuestionsAsExam(teacherId, ids, config);
        return R.ok(result, "考试任务已创建");
    }

    /** AI 智能组卷 — 提交生成任务（异步，返回 taskId 供 SSE 轮询） */
    @PostMapping("/actions/generate")
    @Operation(summary = "AI智能组卷", description = "提交AI组卷任务，支持按知识点、难度等条件生成试卷")
    public R<?> submitExamGeneration(@RequestBody Map<String, Object> params) {
        if (!aiContentEnabled()) return R.error(410, "AI教学助手功能未启用");
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long teacherId = SecurityUtils.getCurrentUserId();

        // 规范化前端参数字段 → PromptBuilder 期望的字段
        if (params.containsKey("mode") && !params.containsKey("examMode")) {
            params.put("examMode", params.get("mode"));
        }
        params.put("contentType", "EXAM_PAPER");

        // 从 categoryId 解析知识范围，注入 nodeId 供 RAG 上下文构建
        if (params.get("categoryId") instanceof Number n && n.longValue() > 0) {
            params.putIfAbsent("nodeId", n.longValue());
        }

        String taskId = aiContentService.submitGeneration(teacherId, params);
        return R.ok(Map.of("taskId", taskId, "status", "PENDING"));
    }

    /** 触发 AI 诊断 */
    @PostMapping("/actions/diagnose/{taskId}")
    @Operation(summary = "触发AI诊断", description = "对指定任务触发AI学情诊断分析")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    public R<java.util.Map<String, Object>> startDiagnosis(@PathVariable Long taskId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long teacherId = SecurityUtils.getCurrentUserId();
        java.util.Map<String, Object> diagnosisData = examPaperService.buildDiagnosisData(taskId);
        java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("contentType", "DIAGNOSIS");
        params.put("taskId", taskId);
        params.put("diagnosisData", diagnosisData);
        String asyncTaskId = aiContentService.submitGeneration(teacherId, params);
        return R.ok(java.util.Map.of("asyncTaskId", asyncTaskId, "message", "诊断任务已提交"));
    }

    /** 获取诊断结果（Mapper 查询已下沉到 AiContentGeneratorService） */
    @GetMapping("/actions/diagnose/{taskId}/result")
    @Operation(summary = "获取诊断结果", description = "获取AI诊断分析的结果")
    @Parameter(name = "taskId", description = "任务ID", required = true, example = "123")
    public R<java.util.Map<String, Object>> getDiagnosisResult(@PathVariable Long taskId) {
        java.util.Map<String, Object> result = aiContentService.getDiagnosisResult(taskId);
        if (result == null) {
            return R.ok(null, "诊断尚未完成或未找到");
        }
        return R.ok(result);
    }

    @PostMapping("/consolidation")
    @Operation(summary = "生成巩固材料", description = "根据知识点生成个性化巩固材料")
    public R<?> startConsolidation(@RequestBody java.util.Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "权限不足");
        Long teacherId = SecurityUtils.getCurrentUserId();
        Long taskId = body.get("taskId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        java.util.List<Integer> kpIds = (java.util.List<Integer>) body.get("knowledgeNodeIds");
        String subject = (String) body.getOrDefault("subject", "数学");
        String commonMistakes = (String) body.getOrDefault("commonMistakes", "");
        String ragContext = (String) body.getOrDefault("ragContext", "");
        java.util.List<Long> kpIdLongs = kpIds != null ? kpIds.stream().map(Long::valueOf).toList() : java.util.List.of();
        String asyncTaskId = consolidationService.generateMaterial(teacherId, taskId, kpIdLongs, subject, commonMistakes, ragContext);
        return R.ok(java.util.Map.of("asyncTaskId", asyncTaskId, "message", "巩固材料生成任务已提交"));
    }
}
