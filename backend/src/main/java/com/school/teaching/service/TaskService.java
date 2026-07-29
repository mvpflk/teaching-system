package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskQuestion;
import com.school.teaching.entity.TaskSubmission;

import java.util.List;
import java.util.Map;

public interface TaskService {

    Task getById(Long id);
    Task create(Task task);
    Task update(Long id, Task task);
    void delete(Long id);

    Task publish(Long id);
    Task close(Long id);
    Task reopen(Long id);

    List<Task> getAccessibleTasks(Long userId);
    List<Task> getTeacherTasks(Long teacherId);
    List<Task> getStudentTasks(Long studentId);
    List<Task> getStudentCompletedTasks(Long studentId);
    List<TaskQuestion> getQuestions(Long taskId);
    /** 教师端试题（含正确答案+选项，从 question_bank 补全） */
    List<java.util.Map<String, Object>> getQuestionsWithDetails(Long taskId);

    /** 巡视管理：分页查询已提交审核的任务 */
    IPage<Task> pageTasksForReview(String reviewStatus, String startDate, String endDate, int page, int pageSize);

    /** 根据教师 ID 列表获取任务 */
    List<Task> getTasksByTeacherIds(java.util.Collection<Long> teacherIds);

    /** 学生端试题（不含正确答案） */
    List<Map<String, Object>> getStudentQuestions(Long taskId);

    IPage<Task> pageByTeacher(Long teacherId, Page<Task> page, String status);
    IPage<Task> pageByStudent(Long studentId, Page<Task> page, String status);
    IPage<Task> pageByAdmin(Page<Task> page, String status);

    /** 批量填充 className/grade/teacherName/isOwner，防 N+1 */
    void enrichTasks(List<Task> tasks, Long currentTeacherId);

    /** 导出任务成绩为Excel */
    byte[] exportScores(Long taskId);

    /** 学生班级变动时处理进行中任务：旧班级任务豁免 + 新班级任务注册 */
    Map<String, Object> handleStudentClassChange(Long studentId, Long oldClassId, Long newClassId);

    /** 向已有任务批量添加题目 */
    void addQuestions(Long taskId, List<Long> questionIds);

    /** 从任务批量移除题目 */
    void removeQuestions(Long taskId, List<Long> questionIds);

    /** 教师提交审核 */
    void submitForReview(Long taskId);
    /** 组长通过审核 */
    void approveReview(Long taskId, Long reviewerId);
    /** 组长拒绝审核 */
    void rejectReview(Long taskId, Long reviewerId, String reason);
    /** 组长获取待审核任务列表 */
    List<Task> getPendingReviews(Long teacherId);

    /** 记录切屏警告（考试防作弊），syncOnly=true 时仅查询当前状态不递增计数，返回 {cheatWarnings, maxCheatWarnings, terminated} */
    Map<String, Object> recordCheatWarning(Long taskId, Long studentId, String eventType, boolean syncOnly);

    /** 预览强制任务覆盖范围：返回 {classCount, studentCount, scope} */
    Map<String, Object> forcedPreview(String grade);

    /** 学生待办任务计数 — 返回 {count, urgent, warning} */
    Map<String, Object> getPendingCount(Long studentId);

    /** 学生任务列表（含提交状态，支持分页）— 返回 {records, total, page, size} */
    Map<String, Object> getStudentTasksWithSubmission(Long studentId, int page, int size);

    /** 一键重发未提交学生：批量开放补交权限+发送通知 → 返回 {count} */
    Map<String, Object> resendToPending(Long taskId);

    /** 获取提交的答题详情列表 — 每题含题目文本、选项、正确答案、学生答案、得分 */
    List<Map<String, Object>> getSubmissionAnswers(Long taskId, Long submissionId);

    /** 问卷统计 → 返回 {totalSubs, questions: [{id, type, label, ...}]} */
    Map<String, Object> getSurveyStats(Long taskId);
    /** P0-4: 问卷原始数据CSV导出 */
    byte[] exportSurvey(Long taskId, boolean blinded);

    /** 任务统计 → 返回 {avgScore, maxScore, minScore, passRate, distribution} */
    Map<String, Object> getTaskStats(Long taskId);

    /** 复制任务（题目+配置，不含提交记录）→ 返回新任务 */
    Task copyTask(Long sourceTaskId, Long userId);

    /** 定时发布扫描：把所有到时间的草稿任务自动发布 → 返回发布数量 */
    int publishScheduledTasks();

    /** 检查学生是否有权访问任务（所在班级匹配或强制任务覆盖） */
    boolean isTaskAccessibleByStudent(Long taskId, Long studentId);

    /** 根据题目ID查找关联的提交ID列表 */
    List<Long> findSubmissionIdsByQuestionId(Long questionId);

    /** 根据任务ID查找关联的提交ID列表 */
    List<Long> findSubmissionIdsByTaskId(Long taskId);

    /** 批量重新评分 — 逐提交调用 examTaskHandler.regradeSubmission */
    Map<String, Object> batchRegrade(List<Long> submissionIds);

    /** 根据ID获取提交记录 */
    TaskSubmission getSubmissionById(Long submissionId);

    /** 获取班级的专业名称 */
    String getClassMajor(Long classId);

    /** 批量修复题目状态和学科字段 */
    void fixQuestionStatus(List<Map<String, Object>> questions, String subject, Long schoolId);

    /** 获取班级当前进行中的课堂任务（IN_CLASS + PUBLISHED/ONGOING，最多20条） */
    List<Task> getActiveClassTasks(Long classId);
}