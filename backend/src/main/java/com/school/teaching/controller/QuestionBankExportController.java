package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;
import com.school.teaching.common.R;
import com.school.teaching.entity.QuestionBank;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.QuestionBankService;
import com.school.teaching.service.WordExportService;
import com.school.teaching.service.impl.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 题库导出/模板/组卷控制器
 * 拆分自 QuestionBankController：Word/Excel 导出、模板下载、组卷、选题匹配。
 * 路由前缀 /question-bank 保持不变，前端调用路径无需改动。
 */
@RestController
@RequestMapping("/question-bank")
@Tag(name = "题库导出组卷", description = "Word/Excel导出、模板下载、组卷、选题匹配")
public class QuestionBankExportController {

    private static final Logger log = LoggerFactory.getLogger(QuestionBankExportController.class);

    @Autowired
    private QuestionBankService bankService;
    @Autowired
    private WordExportService wordExportService;
    @Autowired
    private TemplateService templateService;

    /** 批次导出Word试卷（可直接打印） */
    @GetMapping("/actions/by-batch/{batchId}/export")
    @Operation(summary = "按批次导出Word试卷", description = "将指定批次的题目导出为Word试卷")
    @Parameter(name = "batchId", description = "批次ID", required = true)
    public void exportBatchWord(@PathVariable String batchId, jakarta.servlet.http.HttpServletResponse response) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) { response.setStatus(401); return; }
        List<QuestionBank> questions = bankService.listByBatchId(batchId, userId);
        if (questions.isEmpty()) { response.setStatus(404); return; }
        try {
            byte[] docx = wordExportService.exportExamPaper(
                questions.get(0).getSubject() != null ? questions.get(0).getSubject() + " - 试卷" : "AI组卷",
                questions);
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                java.net.URLEncoder.encode("试卷_" + batchId + ".docx", "UTF-8") + "\"");
            response.getOutputStream().write(docx);
        } catch (Exception e) {
            log.error("导出试卷失败: batchId={}", batchId, e);
            response.setStatus(500);
        }
    }

    /**
     * 按题目ID列表导出Word试卷（支持自定义标题/满分/时长/题型分值）
     * 用于 ComposeExamWizard 组卷后直接下载可打印的 DOCX
     */
    @PostMapping("/actions/export-word")
    @Operation(summary = "按题目ID导出Word试卷", description = "将指定题目列表导出为Word试卷")
    public void exportWordByQuestionIds(@RequestBody Map<String, Object> body,
                                         jakarta.servlet.http.HttpServletResponse response) {
        if (!SecurityUtils.isTeacherOrAdmin()) { response.setStatus(403); return; }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) { response.setStatus(401); return; }

        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.get("questionIds");
        if (rawIds == null || rawIds.isEmpty()) { response.setStatus(400); return; }
        List<Long> questionIds = rawIds.stream().map(Number::longValue).toList();

        String title = body.get("title") instanceof String s && !s.isBlank() ? s : "组卷试卷";
        int totalScore = body.get("totalScore") instanceof Number n ? n.intValue() : 100;
        int durationMinutes = body.get("durationMinutes") instanceof Number n ? n.intValue() : 90;

        // 解析题型分值
        java.util.Map<String, Integer> perTypeScores = new java.util.HashMap<>();
        if (body.get("perTypeScores") instanceof java.util.Map<?, ?> pts) {
            for (var entry : pts.entrySet()) {
                if (entry.getValue() instanceof Number v) {
                    perTypeScores.put(String.valueOf(entry.getKey()), v.intValue());
                }
            }
        }

        // 查询题目
        List<QuestionBank> allQuestions = bankService.listByIds(questionIds);
        if (allQuestions.isEmpty()) { response.setStatus(404); return; }

        // 按请求的ID顺序排序（保持选题时的顺序）
        java.util.Map<Long, QuestionBank> qMap = allQuestions.stream()
            .collect(java.util.stream.Collectors.toMap(QuestionBank::getId, q -> q));
        List<QuestionBank> ordered = new ArrayList<>();
        for (Long id : questionIds) {
            QuestionBank q = qMap.get(id);
            if (q != null) ordered.add(q);
        }

        try {
            byte[] docx = wordExportService.exportExamPaper(title, ordered, totalScore, durationMinutes, perTypeScores);
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                java.net.URLEncoder.encode(sanitizeFilename(title) + ".docx", "UTF-8") + "\"");
            response.setContentLength(docx.length);
            response.getOutputStream().write(docx);
        } catch (Exception e) {
            log.error("导出试卷Word失败: title={}", title, e);
            response.setStatus(500);
        }
    }

    /** 获取学科列表 */
    @GetMapping("/actions/subjects")
    @Operation(summary = "获取题库学科列表", description = "获取所有包含题目的学科列表")
    public R<List<Map<String, Object>>> getSubjects() {
        return R.ok(bankService.getSubjects());
    }

    /** 下载Word导入模板 — 全题型覆盖+图片示例 */
    @GetMapping("/actions/template/download")
    @Operation(summary = "下载Word导入模板", description = "下载包含所有题型示例的Word导入模板")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        return templateService.buildWordTemplate();
    }

    /** 从题库选题加入试卷 */
    @PostMapping("/actions/add-to-exam/{examId}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "题库选题添加到试卷")
    @Operation(summary = "添加题目到试卷", description = "从题库选题添加到指定试卷")
    @Parameter(name = "examId", description = "试卷ID", required = true, example = "123")
    public R<String> addToExam(@PathVariable Long examId, @RequestBody List<Long> questionIds) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        int added = bankService.addToExam(examId, questionIds, userId, SecurityUtils.isAdmin());
        return R.ok("成功添加 " + added + " 道题到试卷");
    }

    /** 下载 Excel 导入模板 */
    @GetMapping("/actions/excel-template/download")
    @Operation(summary = "下载Excel导入模板", description = "下载题目导入的Excel模板")
    public ResponseEntity<byte[]> downloadExcelTemplate() throws IOException {
        return templateService.buildExcelTemplate();
    }

    /** 一键组卷 */
    @PostMapping("/actions/compose-exam")
    @AuditLog(eventType = AuditEventType.OTHER, description = "题库组卷")
    @Operation(summary = "一键组卷", description = "根据条件自动从题库选题组成试卷")
    public R<Map<String, Object>> composeExam(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return R.error(401, "未登录");
        String role = SecurityUtils.getCurrentRole();
        return R.ok(bankService.composeExam(body, userId, role), "组卷成功");
    }

    /** 自由组题：按知识点+题型从题库匹配题目 */
    @PostMapping("/actions/match")
    @Operation(summary = "自由组题", description = "按知识点和题型从题库匹配题目")
    public R<List<Map<String, Object>>> matchQuestions(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "无权限");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> kps = (List<Map<String, Object>>) body.get("knowledgePoints");
        if (kps == null || kps.isEmpty()) return R.error(400, "请选择知识点");
        List<Long> excludeIds = new ArrayList<>();
        Object exclRaw = body.get("excludeIds");
        if (exclRaw instanceof List<?> list) {
            for (Object id : list) {
                try { excludeIds.add(Long.valueOf(id.toString())); }
                catch (NumberFormatException ignored) {
                    // 静默降级：非数字 ID 跳过
                }
            }
        }
        return R.ok(bankService.matchQuestions(kps, excludeIds));
    }

    /** 清理文件名中的非法字符，用于 Content-Disposition header */
    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "试卷";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
