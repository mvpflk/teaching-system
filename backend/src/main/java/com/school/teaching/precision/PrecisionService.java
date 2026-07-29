package com.school.teaching.precision;

import java.util.List;
import java.util.Map;

public interface PrecisionService {

    /** 学生仪表盘概览（数学+英语） */
    Map<String, Object> getDashboard(Long studentId);

    /** 获取/创建诊断测试 */
    Map<String, Object> getDiagnosis(Long studentId, String subject);

    /** 提交诊断答案 → 生成画像 */
    Map<String, Object> submitDiagnosis(Long studentId, String subject, List<Map<String, Object>> answers);

    /** 逐题判分 — 诊断即时反馈，返回对错+正确答案+解析 */
    Map<String, Object> gradeOneAnswer(Long studentId, Long questionId, String answer, String subject, String questionType);

    /** 获取本周学习包（HTML 完整文档） */
    String getWeeklyPackHtml(Long studentId, String subject, int weekNo);

    /** 获取学习包结构化题目数据（在线答题模式） */
    List<Map<String, Object>> getPackQuestions(Long studentId, String subject);

    /** 获取周末线上小测 */
    Map<String, Object> getOnlineTest(Long studentId, String subject);

    /** 提交线上小测答案 */
    Map<String, Object> submitOnlineTest(Long studentId, Map<String, Object> body);

    /** 进步报告数据 */
    Map<String, Object> getReport(Long studentId, String subject);

    /** 考点地图数据 */
    List<Map<String, Object>> getSyllabusMap(Long studentId, String subject);

    /** 获取知识点对应的练习题目 */
    List<Map<String, Object>> getPracticeQuestions(Long studentId, Long nodeId, String subject);

    /** 数学周末 AI 答疑 */
    Map<String, Object> aiQa(Long studentId, String question);

    // ── 教师端 ──

    /** 班级偏科概览 */
    Map<String, Object> teacherOverview(Long teacherUserId);

    /** 学生列表（支持分组筛选） */
    List<Map<String, Object>> teacherStudents(Long teacherUserId, Long groupId, String subject);

    /** 一键提醒未提交学生 */
    int remindAll(Long teacherUserId, String subject);

    /** 为分组或班级生成补强练习卷（classId 用于按班级薄弱点出题） */
    Map<String, Object> composeRemedialTask(Long teacherUserId, Long groupId, Long classId, String subject);

    /** 班级薄弱知识点 TOP N */
    List<Map<String, Object>> teacherWeakTop(Long teacherUserId, String subject, int topN);

    /** 提醒单个学生 */
    boolean remindStudent(Long teacherUserId, Long studentId, String subject);

    // ── 班级监控 ──

    /**
     * 班级薄弱知识点分布 + 诊断趋势
     * @return Map with keys: "weakNodes" (List of {name, errorCount}), "diagnosisTrend" (List of {date, averageScore})
     */
    Map<String, Object> getClassWeaknesses(Long classId);

    /** 质量分析打通 — 查询某学生某知识点的偏科提分状态 */
    Map<String, Object> getStudentKpStatus(Long studentId, Long kpId);

    /** 质量分析打通 — 为某学生某知识点创建偏科提分学习包 */
    Map<String, Object> ensureFromQuality(Long studentId, Long kpId, String subject);

    /** 拍照上传数学解答 — Vision OCR 识别并存储 */
    Map<String, Object> uploadAnswerPhoto(Long studentId, Long questionId, String questionType,
                                          org.springframework.web.multipart.MultipartFile file);

    /** 校验学生是否属于当前教师管辖 */
    void assertTeacherOwnsStudent(Long teacherUserId, Long studentId);

    /** 校验班级是否属于当前教师管辖 */
    void assertTeacherOwnsClass(Long teacherUserId, Long classId);
}
