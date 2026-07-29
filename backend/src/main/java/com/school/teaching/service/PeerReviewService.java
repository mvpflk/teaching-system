package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface PeerReviewService {

    /** 为任务启动互评分配。返回分配的记录数 */
    int assignReviews(Long taskId);

    /** 获取学生的待评列表 */
    List<Map<String, Object>> getPendingReviews(Long studentId);

    /** 学生提交互评 */
    Map<String, Object> submitReview(Long reviewId, Map<String, Object> scoreData, Long reviewerId);

    /** 教师查看互评进度 */
    Map<String, Object> getProgress(Long taskId);

    /** 教师查看某份提交的互评详情 */
    List<Map<String, Object>> getReviewDetails(Long submissionId);

    /** 计算并更新最终分数（融合教师评分和互评均分） */
    int fuseScores(Long taskId);

    /** 互评质量分析：评分偏差/满分零分/异常标记 */
    Map<String, Object> getQualityAnalysis(Long taskId);

    /** 获取提交的所有互评详情（含匿名评语，供学生查看） */
    List<Map<String, Object>> getPeerCommentsForStudent(Long submissionId, Long studentId);

    /** 互评截止提醒：查24h内截止且未完成互评的学生 */
    List<Map<String, Object>> findPendingReminders();

    /** 检查提交是否属于指定学生 */
    boolean isSubmissionOwner(Long submissionId, Long studentId);
}
