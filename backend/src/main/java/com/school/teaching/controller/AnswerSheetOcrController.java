package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AnswerSheetOcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * P0-1: 答题卡OCR识别控制器
 * 打印答题卡→拍照→Vision OCR→自动判分
 */
@RestController
@RequestMapping("/task/{taskId}/answer-sheet")
public class AnswerSheetOcrController {

    @Autowired private AnswerSheetOcrService ocrService;

    /**
     * 批量OCR识别答题卡
     * POST /task/{taskId}/answer-sheet/ocr
     * multipart/form-data: { studentId, file }
     */
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, Object>> ocrSheet(
            @PathVariable Long taskId,
            @RequestParam("studentId") Long studentId,
            @RequestParam("file") MultipartFile file) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        return R.ok(ocrService.ocrSheet(taskId, studentId, file));
    }

    /**
     * 手动录入答案（OCR失败的降级方案）
     * POST /task/{taskId}/answer-sheet/manual-entry
     * Body: { "studentId": 1, "answers": [{"questionNo":1,"questionId":100,"answer":"A"}] }
     */
    @PostMapping("/manual-entry")
    public R<Map<String, Object>> manualEntry(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        Long studentId = body.get("studentId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.getOrDefault("answers", List.of());
        return R.ok(ocrService.manualEntry(taskId, studentId, answers));
    }

    /**
     * 获取OCR记录列表
     * GET /task/{taskId}/answer-sheet/records?status=parsed
     */
    @GetMapping("/records")
    public R<List<Map<String, Object>>> listRecords(
            @PathVariable Long taskId,
            @RequestParam(required = false) String status) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        return R.ok(ocrService.listOcrRecords(taskId, status));
    }

    /**
     * 教师复核OCR结果
     * POST /task/{taskId}/answer-sheet/review/{ocrId}
     * Body: { "confirmed": true, "note": "确认无误" }
     */
    @PostMapping("/review/{ocrId}")
    public R<Void> reviewOcr(
            @PathVariable Long taskId,
            @PathVariable Long ocrId,
            @RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        boolean confirmed = Boolean.TRUE.equals(body.get("confirmed"));
        String note = (String) body.getOrDefault("note", "");
        ocrService.reviewOcr(ocrId, SecurityUtils.getCurrentUserId(), confirmed, note);
        return R.ok();
    }

    /**
     * OCR准确率统计 — 对比识别答案与题库正确答案
     * GET /task/{taskId}/answer-sheet/accuracy
     */
    @GetMapping("/accuracy")
    public R<Map<String, Object>> accuracyStats(@PathVariable Long taskId) {
        if (!SecurityUtils.isTeacherOrAdmin()) return R.error(403, "仅教师可操作");
        return R.ok(ocrService.accuracyStats(taskId));
    }
}
