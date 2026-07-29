package com.school.teaching.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface AnswerSheetOcrService {
    /**
     * OCR识别答题卡照片 — Vision API → 解析答案 → 自动判分
     * @param taskId 任务ID
     * @param studentId 学生ID（教师拍照时指定是哪位学生的答题卡）
     * @param file 答题卡照片
     */
    Map<String, Object> ocrSheet(Long taskId, Long studentId, MultipartFile file);

    /**
     * 手动录入答案（OCR失败的降级方案）
     * @param taskId 任务ID
     * @param studentId 学生ID
     * @param answers 手动录入的答案列表
     * @return {autoGradeResult}
     */
    Map<String, Object> manualEntry(Long taskId, Long studentId, java.util.List<Map<String, Object>> answers);

    /**
     * 获取某任务的OCR记录列表
     */
    java.util.List<Map<String, Object>> listOcrRecords(Long taskId, String status);

    /**
     * 教师复核OCR结果（置信度低的需要复核）
     */
    void reviewOcr(Long ocrId, Long reviewerId, boolean confirmed, String note);

    /**
     * OCR准确率统计 — 对比OCR识别结果与题库正确答案
     * @return {overallAccuracy, byQuestionType, confidenceCorrelation, lowConfidenceStats, totalRecords}
     */
    Map<String, Object> accuracyStats(Long taskId);
}
