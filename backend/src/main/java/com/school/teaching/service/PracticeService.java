package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface PracticeService {
    /** 新增步骤，返回 stepId */
    Map<String, Object> createStep(Long studentId, Map<String, Object> params);

    /** 更新步骤（仅本人） */
    void updateStep(Long stepId, Long studentId, Map<String, Object> params);

    /** 删除步骤并重排 index */
    void deleteStep(Long stepId, Long studentId);

    /** 调整步骤顺序 */
    void reorderSteps(Long taskId, Long studentId, List<Long> stepIds);

    /** 获取该学生某任务的所有步骤 */
    List<Map<String, Object>> listSteps(Long taskId, Long studentId);

    /** 提交实训 */
    Map<String, Object> submit(Long taskId, Long studentId);

    /** 撤回提交 */
    void withdraw(Long taskId, Long studentId);

    /** 异步生成实训下载ZIP，返回taskId */
    String startDownload(Long taskId, Long classId, List<Long> studentIds);

    /** 查询下载任务状态 */
    AiTaskStore.TaskEntry getDownloadStatus(String taskId);

    /** 教师评分 */
    Map<String, Object> grade(Long submissionId, java.math.BigDecimal overallScore,
        String overallComment, java.util.List<java.util.Map<String, Object>> stepGrades);

    /** 获取实训提交列表（教师用） */
    java.util.List<java.util.Map<String, Object>> getSubmissions(Long taskId);
}
