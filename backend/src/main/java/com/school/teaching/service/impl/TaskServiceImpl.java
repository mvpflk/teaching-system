package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskQuestion;
import com.school.teaching.entity.TaskSubmission;
import com.school.teaching.common.ExamTaskHandler;
import com.school.teaching.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskCrudService taskCrudService;
    private final TaskPublishService taskPublishService;
    private final TaskQueryService taskQueryService;
    private final TaskQuestionService taskQuestionService;
    private final TaskReviewService taskReviewService;
    private final TaskCheatService taskCheatService;
    private final ClassService classService;

    private final ExamTaskHandler examTaskHandler;

    public TaskServiceImpl(TaskCrudService taskCrudService,
                           TaskPublishService taskPublishService,
                           TaskQueryService taskQueryService,
                           TaskQuestionService taskQuestionService,
                           TaskReviewService taskReviewService,
                           TaskCheatService taskCheatService,
                           ClassService classService,
                           @Lazy ExamTaskHandler examTaskHandler) {
        this.taskCrudService = taskCrudService;
        this.taskPublishService = taskPublishService;
        this.taskQueryService = taskQueryService;
        this.taskQuestionService = taskQuestionService;
        this.taskReviewService = taskReviewService;
        this.taskCheatService = taskCheatService;
        this.classService = classService;
        this.examTaskHandler = examTaskHandler;
    }

    @Override
    public Task getById(Long id) {
        return taskCrudService.getById(id);
    }

    @Override
    @Transactional
    public Task create(Task task) {
        return taskCrudService.create(task);
    }

    @Override
    @Transactional
    public Task update(Long id, Task task) {
        return taskCrudService.update(id, task);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        taskCrudService.delete(id);
    }

    @Override
    @Transactional
    public Task publish(Long id) {
        return taskPublishService.publish(id);
    }

    @Override
    @Transactional
    public Task close(Long id) {
        return taskPublishService.close(id);
    }

    @Override
    @Transactional
    public Task reopen(Long id) {
        return taskPublishService.reopen(id);
    }

    @Override
    public List<Task> getAccessibleTasks(Long userId) {
        return taskQueryService.getAccessibleTasks(userId);
    }

    @Override
    public List<Task> getTeacherTasks(Long teacherId) {
        return taskQueryService.getTeacherTasks(teacherId);
    }

    @Override
    public List<Task> getStudentTasks(Long studentId) {
        return taskQueryService.getStudentTasks(studentId);
    }

    @Override
    public List<Task> getStudentCompletedTasks(Long studentId) {
        return taskQueryService.getStudentCompletedTasks(studentId);
    }

    @Override
    public List<TaskQuestion> getQuestions(Long taskId) {
        return taskQuestionService.getQuestions(taskId);
    }

    @Override
    public List<Map<String, Object>> getQuestionsWithDetails(Long taskId) {
        return taskQuestionService.getQuestionsWithDetails(taskId);
    }

    @Override
    public List<Map<String, Object>> getStudentQuestions(Long taskId) {
        return taskQuestionService.getStudentQuestions(taskId);
    }

    @Override
    public IPage<Task> pageByTeacher(Long teacherId, Page<Task> page, String status) {
        return taskQueryService.pageByTeacher(teacherId, page, status);
    }

    @Override
    public IPage<Task> pageByStudent(Long studentId, Page<Task> page, String status) {
        return taskQueryService.pageByStudent(studentId, page, status);
    }

    @Override
    public IPage<Task> pageByAdmin(Page<Task> page, String status) {
        return taskQueryService.pageByAdmin(page, status);
    }

    @Override
    public void enrichTasks(List<Task> tasks, Long currentTeacherId) {
        taskQueryService.enrichTasks(tasks, currentTeacherId);
    }

    @Override
    public byte[] exportScores(Long taskId) {
        return taskReviewService.exportScores(taskId);
    }

    @Override
    @Transactional
    public Map<String, Object> handleStudentClassChange(Long studentId, Long oldClassId, Long newClassId) {
        return taskPublishService.handleStudentClassChange(studentId, oldClassId, newClassId);
    }

    @Override
    @Transactional
    public void addQuestions(Long taskId, List<Long> questionIds) {
        taskQuestionService.addQuestions(taskId, questionIds);
    }

    @Override
    @Transactional
    public void removeQuestions(Long taskId, List<Long> questionIds) {
        taskQuestionService.removeQuestions(taskId, questionIds);
    }

    @Override
    @Transactional
    public void submitForReview(Long taskId) {
        taskReviewService.submitForReview(taskId);
    }

    @Override
    @Transactional
    public void approveReview(Long taskId, Long reviewerId) {
        taskReviewService.approveReview(taskId, reviewerId);
    }

    @Override
    @Transactional
    public void rejectReview(Long taskId, Long reviewerId, String reason) {
        taskReviewService.rejectReview(taskId, reviewerId, reason);
    }

    @Override
    public List<Task> getPendingReviews(Long teacherId) {
        return taskReviewService.getPendingReviews(teacherId);
    }

    @Override
    @Transactional
    public Map<String, Object> recordCheatWarning(Long taskId, Long studentId, String eventType, boolean syncOnly) {
        return taskCheatService.recordCheatWarning(taskId, studentId, eventType, syncOnly);
    }

    @Override
    public Map<String, Object> forcedPreview(String grade) {
        return taskPublishService.forcedPreview(grade);
    }

    @Override
    public Map<String, Object> getPendingCount(Long studentId) {
        return taskQueryService.getPendingCount(studentId);
    }

    @Override
    public Map<String, Object> getStudentTasksWithSubmission(Long studentId, int page, int size) {
        return taskQueryService.getStudentTasksWithSubmission(studentId, page, size);
    }

    @Override
    @Transactional
    public Map<String, Object> resendToPending(Long taskId) {
        return taskPublishService.resendToPending(taskId);
    }

    @Override
    public List<Map<String, Object>> getSubmissionAnswers(Long taskId, Long submissionId) {
        return taskReviewService.getSubmissionAnswers(taskId, submissionId);
    }

    @Override
    public Map<String, Object> getSurveyStats(Long taskId) {
        return taskReviewService.getSurveyStats(taskId);
    }

    @Override
    public byte[] exportSurvey(Long taskId, boolean blinded) {
        return taskReviewService.exportSurvey(taskId, blinded);
    }

    @Override
    public Map<String, Object> getTaskStats(Long taskId) {
        return taskReviewService.getTaskStats(taskId);
    }

    @Override
    @Transactional
    public Task copyTask(Long sourceTaskId, Long userId) {
        return taskCrudService.copyTask(sourceTaskId, userId);
    }

    @Override
    public int publishScheduledTasks() {
        return taskPublishService.publishScheduledTasks();
    }

    @Override
    public boolean isTaskAccessibleByStudent(Long taskId, Long studentId) {
        return taskQueryService.isTaskAccessibleByStudent(taskId, studentId);
    }

    @Override
    public List<Long> findSubmissionIdsByQuestionId(Long questionId) {
        return taskReviewService.findSubmissionIdsByQuestionId(questionId);
    }

    @Override
    public List<Long> findSubmissionIdsByTaskId(Long taskId) {
        return taskReviewService.findSubmissionIdsByTaskId(taskId);
    }

    @Override
    public Map<String, Object> batchRegrade(List<Long> submissionIds) {
        return taskReviewService.batchRegrade(submissionIds);
    }

    @Override
    public TaskSubmission getSubmissionById(Long submissionId) {
        return taskReviewService.getSubmissionById(submissionId);
    }

    @Override
    public String getClassMajor(Long classId) {
        return classService.getClassMajor(classId);
    }

    @Override
    public void fixQuestionStatus(List<Map<String, Object>> questions, String subject, Long schoolId) {
        taskQuestionService.fixQuestionStatus(questions, subject, schoolId);
    }

    @Override
    public List<Task> getActiveClassTasks(Long classId) {
        return taskQueryService.getActiveClassTasks(classId);
    }

    @Override
    public IPage<Task> pageTasksForReview(String reviewStatus, String startDate, String endDate, int page, int pageSize) {
        return taskQueryService.pageTasksForReview(reviewStatus, startDate, endDate, page, pageSize);
    }

    @Override
    public List<Task> getTasksByTeacherIds(java.util.Collection<Long> teacherIds) {
        return taskQueryService.getTasksByTeacherIds(teacherIds);
    }
}
