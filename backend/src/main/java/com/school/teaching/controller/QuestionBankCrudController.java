package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.common.R;
import com.school.teaching.dto.QuestionBankVO;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.QuestionBankService;
import com.school.teaching.service.AiQuestionGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题库 CRUD 控制器
 * 拆分自 QuestionBankController：题目增删改查、审核、导入。
 * 导出/模板/组卷相关端点见 {@link QuestionBankExportController}。
 */
@RestController
@RequestMapping("/question-bank")
@Tag(name = "题库管理", description = "题目CRUD、导入、AI审核")
public class QuestionBankCrudController {

    @Autowired
    private QuestionBankService bankService;

    @Autowired
    private AiQuestionGeneratorService aiQuestionGeneratorService;

    /** 获取单个题目 */
    @GetMapping("/{id}")
    @Operation(summary = "获取题目详情", description = "根据题目ID获取题目详细信息")
    @Parameter(name = "id", description = "题目ID", required = true, example = "123")
    public R<QuestionBankVO> getById(@PathVariable Long id) {
        QuestionBank q = bankService.getById(id);
        if (q == null) return R.notFound("题目不存在");
        String role = SecurityUtils.getCurrentRole();
        if ("STUDENT".equals(role) || role == null) {
            return R.ok(QuestionBankVO.fromEntity(q));
        }
        return R.ok(QuestionBankVO.fromEntityForTeacher(q));
    }

    /** 获取题库列表（支持分页 + 筛选） */
    @GetMapping("/list")
    @Operation(summary = "分页获取题库列表", description = "获取题目列表，支持按学科、类型、难度等筛选")
    @Parameter(name = "subject", description = "学科筛选", example = "数学[职高]")
    @Parameter(name = "categoryId", description = "分类ID")
    @Parameter(name = "questionType", description = "题型: SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/FILL_IN/ESSAY")
    @Parameter(name = "difficultyLevel", description = "难度: 1=简单 2=中等 3=困难")
    @Parameter(name = "tier", description = "题目层级: BASIC/MEDIUM/ADVANCED")
    @Parameter(name = "knowledgeDim", description = "考纲维度: THEORY/PRACTICE")
    @Parameter(name = "source", description = "入库途径: MANUAL/AI/WORD_IMPORT/EXCEL_IMPORT")
    @Parameter(name = "sort", description = "排序: latest(默认)/mostUsed")
    @Parameter(name = "keyword", description = "搜索关键词")
    @Parameter(name = "status", description = "状态: 0=草稿 1=正常 2=删除")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "pageSize", description = "每页数量", example = "20")
    public R<Map<String, Object>> list(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer difficultyLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String knowledgeDim,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        var pageObj = bankService.pageQuestions(subject, categoryId, questionType, difficultyLevel, keyword, status,
            tier, knowledgeDim, source, sort, page, pageSize);
        String role = SecurityUtils.getCurrentRole();
        boolean hideAnswer = "STUDENT".equals(role) || role == null;
        List<?> voList = pageObj.getRecords().stream()
            .map(q -> hideAnswer ? QuestionBankVO.fromEntity((QuestionBank) q) : QuestionBankVO.fromEntityForTeacher((QuestionBank) q))
            .toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", voList);
        data.put("total", pageObj.getTotal());
        data.put("pageNum", pageObj.getCurrent());
        data.put("pageSize", pageObj.getSize());
        return R.ok(data);
    }

    /** 按批次ID获取题目（AI生成后编辑用） */
    @GetMapping("/actions/by-batch/{batchId}")
    @Operation(summary = "按批次获取题目", description = "获取指定批次ID的题目列表")
    @Parameter(name = "batchId", description = "批次ID", required = true)
    public R<List<QuestionBank>> getByBatch(@PathVariable String batchId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(bankService.listByBatchId(batchId, userId));
    }

    /** 创建题目 */
    @PostMapping
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建题库题目")
    @Operation(summary = "创建题目", description = "教师创建新的题库题目")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "403", description = "仅教师可创建")
    })
    public R<QuestionBank> create(@RequestBody @jakarta.validation.Valid QuestionBank question) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可创建题目");
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(bankService.createQuestion(userId, question), "创建成功");
    }

    /** 批量保存题目（一键示例卷使用） */
    @PostMapping("/batch-save")
    @Operation(summary = "批量保存题目", description = "批量创建题目（一键示例卷使用）")
    public R<?> batchSave(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Long userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        String subject = (String) body.getOrDefault("subject", "");
        if (questions == null || questions.isEmpty()) return R.error(400, "题目列表为空");

        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> q : questions) {
            QuestionBank qb = new QuestionBank();
            qb.setQuestionText(String.valueOf(q.getOrDefault("questionText", "")));
            qb.setQuestionType(String.valueOf(q.getOrDefault("questionType", "SINGLE_CHOICE")));
            qb.setSubject(subject);
            qb.setStatus(1);
            qb.setSchoolId(1L);

            @SuppressWarnings("unchecked")
            List<String> opts = (List<String>) q.get("options");
            if (opts != null && !opts.isEmpty()) {
                try { qb.setOptions(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(opts)); }
                catch (Exception ignored) {
                    // 静默降级：options 序列化失败则跳过
                }
            }
            qb.setCorrectAnswer(String.valueOf(q.getOrDefault("correctAnswer", "")));
            qb.setExplanation(String.valueOf(q.getOrDefault("explanation", "")));
            Object diff = q.get("difficultyLevel");
            if (diff instanceof Number n) qb.setDifficultyLevel(n.intValue());
            else qb.setDifficultyLevel(2);

            QuestionBank saved = bankService.createQuestion(userId, qb);
            if (saved != null && saved.getId() != null) ids.add(saved.getId());
        }
        return R.ok(Map.of("ids", ids, "count", ids.size()), "已保存 " + ids.size() + " 题");
    }

    /** 更新题目 */
    @PutMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新题库题目")
    @Operation(summary = "更新题目", description = "更新指定题目的信息")
    @Parameter(name = "id", description = "题目ID", required = true, example = "123")
    public R<QuestionBank> update(@PathVariable Long id, @RequestBody QuestionBank question) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        QuestionBank updated = bankService.updateQuestion(id, question, userId,
            SecurityUtils.isAdmin() || SecurityUtils.isSuperAdmin());
        if (updated == null) return R.notFound("题目不存在");
        return R.ok(updated, "更新成功");
    }

    /** AI题目审核通过 */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审核通过题目", description = "将AI生成的题目标记为审核通过")
    @Parameter(name = "id", description = "题目ID", required = true, example = "123")
    public R<?> approveQuestion(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        aiQuestionGeneratorService.approve(id);
        return R.ok(null, "已通过");
    }

    /** AI题目审核拒绝 */
    @DeleteMapping("/{id}/reject")
    @Operation(summary = "驳回题目", description = "将AI生成的题目标记为驳回")
    @Parameter(name = "id", description = "题目ID", required = true, example = "123")
    public R<?> rejectQuestion(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        aiQuestionGeneratorService.reject(id);
        return R.ok(null, "已驳回");
    }

    /** 删除题目（软删除） */
    @DeleteMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除题库题目")
    @Operation(summary = "删除题目", description = "软删除指定题目")
    @Parameter(name = "id", description = "题目ID", required = true, example = "123")
    public R<String> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        bankService.deleteQuestion(id, userId, SecurityUtils.isAdmin());
        return R.ok("已删除");
    }

    /** 批量清空题库（管理员专属） */
    @DeleteMapping("/actions/batch-clear")
    @AuditLog(eventType = AuditEventType.OTHER, description = "批量清空题库")
    @Operation(summary = "批量清空题库", description = "管理员清空所有题目")
    public R<String> batchClear() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可操作");
        int count = bankService.batchClearQuestions();
        return R.ok("已清空 " + count + " 道题目");
    }

    /** 批量删除题目（软删除） */
    @PostMapping("/actions/batch-delete")
    @AuditLog(eventType = AuditEventType.OTHER, description = "批量删除题目")
    @Operation(summary = "批量删除题目", description = "软删除选中的题目")
    public R<String> batchDelete(@RequestBody List<Long> ids) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (ids == null || ids.isEmpty()) return R.error(400, "请选择题目");
        int count = 0;
        for (Long id : ids) {
            try { bankService.deleteQuestion(id, userId, SecurityUtils.isAdmin()); count++; }
            catch (Exception ignored) { /* 跳过无权限或已删除的题目 */ }
        }
        return R.ok("成功删除 " + count + " 道题目");
    }

    /** 从Word导入题目 */
    @PostMapping("/actions/import-word")
    @AuditLog(eventType = AuditEventType.OTHER, description = "Word导入题库")
    @Operation(summary = "Word导入题目", description = "从Word文件导入题目到题库")
    @Parameter(name = "file", description = "Word文件", required = true)
    @Parameter(name = "categoryId", description = "目标分类ID")
    public R<Map<String, Object>> importWord(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) Long categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        Map<String, Object> result = bankService.importFromWord(file, categoryId, userId);
        return R.ok(result, "成功导入 " + result.get("imported") + " 道题");
    }

    /** 批量Word导入：多文件 + 每个文件对应一个categoryId（JSON数组 [{"categoryId":1,"filename":"xxx.docx"}]） */
    @PostMapping("/actions/import-word-batch")
    @AuditLog(eventType = AuditEventType.OTHER, description = "Word批量导入题库")
    @Operation(summary = "Word批量导入", description = "多文件批量导入题目到题库")
    @Parameter(name = "files", description = "Word文件列表", required = true)
    @Parameter(name = "mappings", description = "文件-分类映射JSON")
    public R<Map<String, Object>> importWordBatch(@RequestParam("files") List<MultipartFile> files,
                                                    @RequestParam(required = false) String mappings) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        Map<String, Object> result = bankService.importFromWordBatch(files, mappings, userId);
        return R.ok(result, "批量导入完成：" + result.get("imported") + " 道题");
    }

    /** 从Excel导入题目 */
    @PostMapping("/actions/import-excel")
    @AuditLog(eventType = AuditEventType.OTHER, description = "Excel导入题库")
    @Operation(summary = "Excel导入题目", description = "从Excel文件导入题目到题库")
    @Parameter(name = "file", description = "Excel文件", required = true)
    @Parameter(name = "categoryId", description = "目标分类ID")
    public R<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file,
                                               @RequestParam(required = false) Long categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        Map<String, Object> result = bankService.importFromExcel(file, categoryId, userId);
        return R.ok(result, "成功导入 " + result.get("imported") + " 道题");
    }

    /** AI智能审核题目 */
    @PostMapping("/actions/ai-review")
    @Operation(summary = "AI智能审核", description = "使用AI自动审核题目质量")
    public R<Map<String, Object>> aiReview(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        @SuppressWarnings("unchecked")
        List<Integer> idList = (List<Integer>) body.get("questionIds");
        if (idList == null || idList.isEmpty()) return R.error(400, "请选择题目");
        List<Long> questionIds = idList.stream().map(Integer::longValue).toList();
        boolean autoApprove = Boolean.TRUE.equals(body.get("autoApprove"));
        return R.ok(bankService.aiReview(questionIds, autoApprove, userId));
    }

    /** 批量审核通过 */
    @PostMapping("/actions/batch-approve")
    @AuditLog(eventType = AuditEventType.OTHER, description = "批量审核通过题目")
    @Operation(summary = "批量审核通过", description = "批量通过选中的题目")
    public R<Map<String, Object>> batchApprove(@RequestBody List<Integer> idList) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        if (idList == null || idList.isEmpty()) return R.error(400, "请选择题目");
        List<Long> questionIds = idList.stream().map(Integer::longValue).toList();
        return R.ok(bankService.batchApprove(questionIds), "批量通过成功");
    }

    /** 批量驳回 */
    @PostMapping("/actions/batch-reject")
    @AuditLog(eventType = AuditEventType.OTHER, description = "批量驳回题目")
    @Operation(summary = "批量驳回", description = "批量驳回选中的题目")
    public R<Map<String, Object>> batchReject(@RequestBody List<Integer> idList) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        if (idList == null || idList.isEmpty()) return R.error(400, "请选择题目");
        List<Long> questionIds = idList.stream().map(Integer::longValue).toList();
        return R.ok(bankService.batchReject(questionIds, userId), "批量驳回成功");
    }

    /** 批量统计题目组卷使用次数 */
    @PostMapping("/actions/usage-stats")
    @Operation(summary = "题目使用次数统计", description = "批量统计题目被 task_questions 引用的次数")
    public R<java.util.Map<Long, Long>> usageStats(@RequestBody java.util.Map<String, java.util.List<Long>> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可访问");
        java.util.List<Long> ids = body != null ? body.get("ids") : null;
        return R.ok(bankService.usageStats(ids));
    }

    /** 按 ID 批量取题（试题篮补全），POST 避免 URL 长度超限（200 题≈1600+ 字符） */
    @PostMapping("/actions/by-ids")
    @Operation(summary = "按ID批量取题", description = "单次上限200，教师视角含答案，POST 避免 URL 过长")
    public R<java.util.List<QuestionBankVO>> byIds(@RequestBody java.util.Map<String, java.util.List<Long>> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可访问");
        java.util.List<Long> idList = body != null ? body.get("ids") : null;
        if (idList == null) return R.ok(java.util.List.of());
        return R.ok(bankService.listByIds(idList).stream()
            .map(QuestionBankVO::fromEntityForTeacher).toList());
    }
}
