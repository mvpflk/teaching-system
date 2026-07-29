package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface ClassHomeService {
    Map<String, Object> getClassHomeData(Long classId, Long currentUserId);
    boolean isHeadTeacherOfClass(Long classId, Long userId);
    boolean isStudentOfClass(Long classId, Long userId);

    /** 单个学生成绩趋势 — 历次考试分数+班级均分 */
    List<Map<String, Object>> getStudentScoreTrend(Long classId, Long studentId, String subject);

    /** 某次考试全班分析 — 分数分布+逐题正确率 */
    Map<String, Object> getExamAnalysis(Long taskId);

    /** 聚合任务详情 — 供班级主页任务详情面板使用 */
    Map<String, Object> getTaskDetail(Long taskId);
}
