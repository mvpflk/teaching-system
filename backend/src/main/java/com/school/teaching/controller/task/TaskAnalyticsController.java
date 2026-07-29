package com.school.teaching.controller.task;

import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.annotation.AuditLog;
import com.school.teaching.common.R;
import com.school.teaching.dto.request.*;
import com.school.teaching.entity.Task;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务统计 / 导出 / 问卷 / AI衍生训练。
 * 从 TaskController 拆分（W-3）。
 */
@RestController
@RequestMapping("/task")
@Tag(name = "任务管理-分析", description = "任务统计分析、成绩导出、问卷统计、AI衍生训练")
public class TaskAnalyticsController {

    private final TaskAccessChecker access;
    private final TaskCrudService taskCrudService;
    private final TaskPublishService taskPublishService;
    private final TaskReviewService taskReviewService;
    private final TaskService taskService;
    private final TaskQuestionService taskQuestionService;
    private final com.school.teaching.service.ClassHomeService classHomeService;
    @Autowired(required = false) private com.school.teaching.service.AiQuestionGeneratorService aiQuestionService;
    @Autowired(required = false) private com.school.teaching.service.TypingService typingService;

    public TaskAnalyticsController(TaskAccessChecker access,
                                    TaskCrudService taskCrudService,
                                    TaskPublishService taskPublishService,
                                    TaskReviewService taskReviewService,
                                    TaskService taskService,
                                    TaskQuestionService taskQuestionService,
                                    com.school.teaching.service.ClassHomeService classHomeService) {
        this.access = access;
        this.taskCrudService = taskCrudService;
        this.taskPublishService = taskPublishService;
        this.taskReviewService = taskReviewService;
        this.taskService = taskService;
        this.taskQuestionService = taskQuestionService;
        this.classHomeService = classHomeService;
    }

    // ═══════════ 统计 ═══════════

    @GetMapping("/{id:[0-9]+}/stats")
    @Operation(summary = "任务统计分析", description = "获取任务的统计分析数据（每日提交、均分、分布）")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> taskStats(@PathVariable Long id) {
        return R.ok(taskReviewService.getTaskStats(id));
    }

    @GetMapping("/{id}/actions/score-analysis")
    @Operation(summary = "成绩分析", description = "获取任务的成绩分析数据（分数分布+逐题正确率）")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> scoreAnalysis(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin() && !SecurityUtils.isInspector()) return R.error(403, "无权限");
        return R.ok(classHomeService.getExamAnalysis(id));
    }

    // ═══════════ 导出 ═══════════

    @GetMapping("/{id}/actions/score-analysis/export")
    @Operation(summary = "导出成绩分析", description = "将成绩分析数据导出为CSV文件")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public ResponseEntity<byte[]> exportScoreAnalysis(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin() && !SecurityUtils.isInspector()) return ResponseEntity.status(403).build();
        Map<String, Object> data = classHomeService.getExamAnalysis(id);
        StringBuilder csv = new StringBuilder();
        csv.append("考试名称,").append(data.getOrDefault("taskTitle", "")).append("\n");
        csv.append("总分,").append(data.getOrDefault("totalScore", "")).append("\n");
        csv.append("参考人数,").append(data.getOrDefault("participantCount", "")).append("\n");
        csv.append("已批改,").append(data.getOrDefault("gradedCount", "")).append("\n\n");
        csv.append("指标,值\n");
        csv.append("平均得分率(%)").append(",").append(data.getOrDefault("avgRate", "")).append("\n");
        csv.append("最高得分率(%)").append(",").append(data.getOrDefault("maxRate", "")).append("\n");
        csv.append("最低得分率(%)").append(",").append(data.getOrDefault("minRate", "")).append("\n");
        csv.append("及格率(%)").append(",").append(data.getOrDefault("passRate", "")).append("\n\n");
        csv.append("分数段,人数\n");
        for (var d : (java.util.List<Map<String, Object>>) data.getOrDefault("distribution", List.of())) {
            csv.append(d.get("label")).append(",").append(d.get("count")).append("\n");
        }
        csv.append("\n题目,题型,正确率(%),正确/总数\n");
        for (var q : (java.util.List<Map<String, Object>>) data.getOrDefault("questionAccuracy", List.of())) {
            csv.append(q.get("questionText")).append(",");
            csv.append(q.get("questionType")).append(",");
            csv.append(q.get("accuracy")).append(",");
            csv.append(q.get("correctCount")).append("/").append(q.get("totalCount").toString()).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=score-analysis-" + id + ".csv")
            .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
            .body(bytes);
    }

    @AuditLog(eventType = AuditEventType.DATA_EXPORT, description = "导出任务成绩")
    @GetMapping("/{id}/actions/export")
    @Operation(summary = "导出任务成绩", description = "将任务成绩导出为Excel文件")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public ResponseEntity<byte[]> exportScores(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin()) {
            return ResponseEntity.status(403).build();
        }
        byte[] data = taskReviewService.exportScores(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=task_scores_" + id + ".xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }

    // ═══════════ 问卷 ═══════════

    @GetMapping("/{id:[0-9]+}/survey-stats")
    @Operation(summary = "问卷结果统计", description = "获取问卷任务的统计数据")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> surveyStats(@PathVariable Long id) {
        Task task = taskCrudService.getById(id);
        if (task == null || !"SURVEY".equals(task.getTaskType())) return R.error(400, "仅问卷任务支持");
        return R.ok(taskReviewService.getSurveyStats(id));
    }

    @GetMapping("/{id:[0-9]+}/survey-export")
    @Operation(summary = "导出问卷数据", description = "将问卷原始数据导出为CSV文件")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    @Parameter(name = "blinded", description = "是否匿名导出", example = "false")
    public ResponseEntity<byte[]> exportSurvey(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean blinded) {
        if (!SecurityUtils.isTeacherOrAdmin()) return ResponseEntity.status(403).build();
        byte[] csv = taskReviewService.exportSurvey(id, blinded);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + (blinded ? "survey-blinded.csv" : "survey.csv"))
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    // ═══════════ AI衍生训练 ═══════════

    @PostMapping("/{id}/actions/generate-remedial")
    @Operation(summary = "生成AI衍生训练", description = "根据薄弱知识点自动生成衍生训练任务")
    @Parameter(name = "id", description = "任务ID", required = true, example = "123")
    public R<Map<String, Object>> generateRemedialTask(@PathVariable Long id,
            @RequestBody(required = false) RemedialGenerateRequest request) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Map<String, Object> analysis = classHomeService.getExamAnalysis(id);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weakNodes = (List<Map<String, Object>>) analysis.get("weakNodeSummary");
        if (weakNodes == null || weakNodes.isEmpty()) return R.error("暂无薄弱知识点");

        List<Integer> selectedIds = request != null ? request.getNodeIds() : null;
        if (selectedIds != null && !selectedIds.isEmpty()) {
            weakNodes = weakNodes.stream()
                .filter(n -> selectedIds.contains(((Number) n.get("nodeId")).intValue()))
                .collect(Collectors.toList());
        }
        if (weakNodes.isEmpty()) return R.error("请选择至少一个薄弱知识点");

        int totalTarget = 10;
        Map<Long, Integer> nodeQuestionCount = new LinkedHashMap<>();
        List<Long> nodeIds = new ArrayList<>();
        List<String> weakLabels = new ArrayList<>();
        double totalWeight = 0;
        for (Map<String, Object> n : weakNodes) {
            long nid = ((Number) n.get("nodeId")).longValue();
            double acc = ((Number) n.get("accuracy")).doubleValue();
            double weight = Math.max(60 - acc, 5);
            totalWeight += weight;
            nodeIds.add(nid);
            weakLabels.add(String.valueOf(n.get("name")));
        }
        int allocated = 0;
        for (int i = 0; i < weakNodes.size(); i++) {
            long nid = nodeIds.get(i);
            double acc = ((Number) weakNodes.get(i).get("accuracy")).doubleValue();
            double weight = Math.max(60 - acc, 5);
            int count = i == weakNodes.size() - 1
                ? totalTarget - allocated
                : Math.max(1, (int) Math.round(weight / totalWeight * totalTarget));
            if (count < 1) count = 1;
            allocated += count;
            nodeQuestionCount.put(nid, count);
        }

        Task originTask = taskCrudService.getById(id);
        String subject = originTask != null && originTask.getSubject() != null
            ? originTask.getSubject()
            : String.valueOf(analysis.getOrDefault("taskTitle", ""));

        List<Map<String, Object>> allQuestions = new ArrayList<>();
        int failCount = 0;
        for (Map.Entry<Long, Integer> entry : nodeQuestionCount.entrySet()) {
            Long nodeId = entry.getKey();
            int count = entry.getValue();
            List<Integer> singleNodeList = List.of(nodeId.intValue());
            Map<String, Object> remedial = aiQuestionService != null
                ? aiQuestionService.generateRemedial(SecurityUtils.getCurrentUserId(), singleNodeList, subject)
                : Map.of("questions", List.of());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> qs = (List<Map<String, Object>>) remedial.get("questions");
            if (qs == null || qs.isEmpty()) { failCount++; continue; }
            allQuestions.addAll(qs.size() > count ? qs.subList(0, count) : qs);
        }
        if (allQuestions.isEmpty()) return R.error("AI生成失败，请稍后重试");

        Long schoolId = SecurityUtils.getCurrentSchoolId() != null ? SecurityUtils.getCurrentSchoolId() : 1L;
        taskQuestionService.fixQuestionStatus(allQuestions, subject, schoolId);

        Task task = originTask;
        Task newTask = new Task();
        String nodeLabel = weakLabels.size() > 2
            ? weakLabels.get(0) + "+" + weakLabels.get(1) + "+等" + weakLabels.size() + "知识点"
            : String.join("+", weakLabels);
        newTask.setTitle("衍生训练 - " + nodeLabel);
        newTask.setTaskType("AFTER_CLASS");
        newTask.setScoreType("POINT_100");
        newTask.setTargetType("CLASS");
        newTask.setTargetId(task != null ? task.getTargetId() : null);
        newTask.setSubject(subject);
        newTask.setQuestionIds(allQuestions.stream()
            .map(q -> q.get("id") instanceof Number n ? n.longValue() : null)
            .filter(Objects::nonNull).collect(Collectors.toList()));
        Task created = taskCrudService.create(newTask);
        Long newTaskId = created != null ? created.getId() : newTask.getId();
        if (newTaskId == null) return R.error(500, "任务创建失败");
        taskPublishService.publish(newTaskId);

        String major = taskService.getClassMajor(task != null ? task.getTargetId() : null);
        boolean isCs = major != null && (major.contains("计算机") || major.contains("信息技术"));
        if (isCs && typingService != null && !weakLabels.isEmpty()) {
            com.school.teaching.entity.TypingText tt = new com.school.teaching.entity.TypingText();
            tt.setTitle("薄弱知识点-" + (task != null ? task.getTitle() : "练习"));
            tt.setContent(String.join("。\n", weakLabels) + "（请反复练习以上知识点对应的关键术语，加深理解）");
            tt.setLanguage("zh");
            tt.setDifficulty(1);
            tt.setCategory("薄弱知识点");
            tt.setType("practice");
            typingService.addText(tt);
        }

        return R.ok(Map.of("taskTitle", newTask.getTitle(), "questionCount", allQuestions.size(),
            "typingGenerated", isCs));
    }

}
