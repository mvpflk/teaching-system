package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface WrongQuestionService {
    Map<String, Object> listWrongQuestions(Long studentId, int mastered, int page, int pageSize, String sourceType);
    void markMastered(Long id, Long studentId);
    void markMasteredWithSource(Long id, Long studentId, String source);
    void markUnmastered(Long id, Long studentId);
    /** 记录一次练习行为（更新 practice_count + last_practice_time，correct 非 null 时触发遗忘曲线） */
    void recordPractice(Long wrongId, Long studentId);
    void recordPractice(Long wrongId, Long studentId, Boolean correct);
    List<Map<String, Object>> getPracticeList(Long studentId);
    Map<String, Object> submitPractice(Long wrongId, String answer, Long studentId);

    /** 衍生练习：聚合薄弱知识点 → 混合检索 → 创建会话 */
    Map<String, Object> generateDerivedPractice(Long studentId);

    /** 获取练习会话详情（不含正确答案） */
    Map<String, Object> getPracticeSession(Long sessionId, Long studentId);

    /** 提交练习答案 → 自动判分 → 错题回写 */
    Map<String, Object> submitPracticeSession(Long sessionId, Long studentId, List<Map<String, Object>> answers);

    /** 薄弱知识点排行 */
    List<Map<String, Object>> getWeakPoints(Long studentId);

    /** 删除单条错题 */
    void deleteWrongQuestion(Long id, Long studentId);

    /** 批量删除错题 */
    void batchDeleteWrongQuestions(List<Long> ids, Long studentId);

    /** 供状态轮询用，仅查会话基本信息 */
    com.school.teaching.entity.PracticeSession getSessionById(Long sessionId);

    /** 薄弱知识点分析 — 按知识节点聚合错误次数、最近错误时间、错误率，返回TOP10 */
    List<Map<String, Object>> weaknessAnalysis(Long studentId, String subject);

    // ── 教师监督 ──
    /** 教师端：班级错题汇总统计 */
    Map<String, Object> teacherSummary(Long teacherUserId);

    /** 教师端：按学生维度聚合错题情况 */
    List<Map<String, Object>> teacherStudentList(Long teacherUserId);

    /** 教师端：班级薄弱知识点聚合 */
    List<Map<String, Object>> teacherWeakPoints(Long teacherUserId);

    /** 学生端：获取错题本统计数据（总数/未掌握数/连续练习天数） */
    Map<String, Object> getStudentStats(Long studentId);

    // ── 教师干预（Phase 2）──
    /** 教师端：查看某学生的错题明细，含题文/题型/错次/考纲维度 */
    List<Map<String, Object>> teacherStudentWrongDetail(Long teacherUserId, Long studentId, int mastered);

    /** 教师端：发送错题复习提醒通知 */
    void notifyStudentReview(Long teacherUserId, Long studentId);

    /** 教师端：薄弱知识点趋势对比（本周 vs 前N周） */
    Map<String, Object> teacherWeakPointsTrend(Long teacherUserId, int weeks);

    /** 获取到期复习的错题列表（next_review_at <= now） */
    List<Map<String, Object>> getDueReviews(Long studentId);
}
