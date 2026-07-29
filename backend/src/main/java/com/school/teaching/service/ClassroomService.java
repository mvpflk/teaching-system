package com.school.teaching.service;

import com.school.teaching.entity.ClassroomQuestion;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface ClassroomService {

    /** SSE订阅 — 学生端连接班级互动频道，userId+studentId用于在线追踪和身份告知 */
    SseEmitter subscribeClassroom(Long classId, Long userId, Long studentId);

    /** 发起抽问会话，返回被抽中学生信息 */
    Map<String, Object> startQuiz(Long classId, Long teacherId, Long questionId,
                                  String questionText, String sceneMode,
                                  String questionType, String options,
                                  List<Long> excludeStudentIds,
                                  Map<Long, Double> studentWeights);

    /** 学生提交文字答案（抽问），更新 participation.response */
    Map<String, Object> submitQuizAnswer(Long sessionId, Long studentId, Long userId, String answerText);

    /** 评分：1=正确 2=部分正确 0=错误，score 为教师指定的分值 */
    Map<String, Object> gradeQuiz(Long sessionId, Long studentId,
                                  int result, String response, int score);

    /** 发起抢答 */
    Map<String, Object> startBuzz(Long classId, Long teacherId,
                                  String questionText, int scoreReward);

    /** 学生抢答（记录毫秒时间戳） */
    Map<String, Object> submitBuzz(Long sessionId, Long studentId, Long userId);

    /** 抢答评分：1=正确 0=错误 */
    Map<String, Object> gradeBuzz(Long sessionId, Long studentId,
                                  int result, String response);

    /** 发起投票 */
    Map<String, Object> startPoll(Long classId, Long teacherId,
                                  String questionText, List<String> options,
                                  int durationSeconds, boolean anonymous);

    /** 学生投票 */
    Map<String, Object> submitVote(Long sessionId, Long studentId,
                                   Long userId, int optionIndex);

    /** 结束投票，返回统计结果。manualCounts 用于CLASSROOM模式手动计票 */
    Map<String, Object> endPoll(Long sessionId, List<Integer> manualCounts);

    /** 获取班级互动会话列表 */
    List<Map<String, Object>> getSessions(Long classId, int limit);

    /** 获取班级本堂课积分排行 */
    List<Map<String, Object>> getClassroomScores(Long classId);

    // ---- 抽问题库 CRUD ----

    /** 分页查询抽问题库，按任务节点(taskId)过滤，支持 keyword 模糊搜索 content */
    Map<String, Object> getQuestions(Long teacherId, Long taskId,
                                     String keyword, Integer page, Integer pageSize);

    /** 获取当前教师抽问题库的筛选项（学科/章节/标签去重列表） */
    Map<String, Object> getQuestionFilters(Long teacherId);

    Map<String, Object> addQuestion(Long teacherId, com.school.teaching.dto.request.ClassroomQuestionRequest request);

    Map<String, Object> updateQuestion(Long id, com.school.teaching.dto.request.ClassroomQuestionRequest request);

    void deleteQuestion(Long id);

    List<Map<String, Object>> batchImportQuestions(Long teacherId, List<Map<String, Object>> rows);

    List<Map<String, Object>> importFromQuestionBank(Long teacherId, List<Long> questionBankIds);

    /** 解析Excel文件为题目行列表（Apache POI） */
    List<Map<String, Object>> parseExcelRows(org.springframework.web.multipart.MultipartFile file) throws Exception;

    /** txt文件批量导入抽问题目（每行一题，关联到知识节点taskId） */
    List<Map<String, Object>> batchImportTxt(Long teacherId, MultipartFile file, Long taskId) throws Exception;

    // ---- 缺席学生管理（DB持久化 + 内存热缓存） ----

    void markAbsentStudents(Long classId, List<Long> studentIds);

    void unmarkAbsentStudent(Long classId, Long studentId);

    List<Long> getAbsentStudents(Long classId);

    void clearAbsentStudents(Long classId);

    /** 将学生从当前抽问池中移除（记入缺席表，防止再次被抽到） */
    void removeFromQuizPool(Long classId, Long studentId);

    /** 重置抽问池：清空最近2小时的抽问参与记录，开始新一轮 */
    void resetQuizPool(Long classId);

    /** SSE广播任务启动通知到班级 */
    void broadcastTaskStarted(Long classId, Long taskId, String taskTitle);

    /** 课堂互动数据分析 */
    Map<String, Object> getClassroomAnalytics(Long classId, String dateRange);

    /** AI推荐题目列表 */
    List<ClassroomQuestion> getAiRecommended(Long teacherId, String subject, String tag);

    /** 获取当前活动状态（短轮询降级用，无活动返回 hasActivity=false） */
    Map<String, Object> getActiveSessionState(Long classId);

    /** 获取班级当前在线SSE连接数 */
    int getClassroomOnlineCount(Long classId);

    // ── 随堂速答 (LIVE_QUIZ) ──
    Map<String, Object> startLiveQuiz(Long classId, Long teacherId,
                                      String questionText, String mode,
                                      List<String> options, String correctAnswer,
                                      int durationSeconds);
    Map<String, Object> submitLiveQuizAnswer(Long sessionId, Long studentId, Long userId, String answer);
    Map<String, Object> endLiveQuiz(Long sessionId, boolean revealAnswer);
    Map<String, Object> pickLiveQuizStudent(Long classId, Long teacherId, List<Long> excludeStudentIds);

    /** 保存AI生成的课堂题目到 classroom_questions 表 */
    void insertAiGeneratedQuestion(ClassroomQuestion question);
}
