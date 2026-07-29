package com.school.teaching.service;

import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskSubmission;

import java.util.List;
import java.util.Map;

public interface TaskReviewService {

    byte[] exportScores(Long taskId);

    void submitForReview(Long taskId);

    void approveReview(Long taskId, Long reviewerId);

    void rejectReview(Long taskId, Long reviewerId, String reason);

    List<Task> getPendingReviews(Long teacherId);

    List<Map<String, Object>> getSubmissionAnswers(Long taskId, Long submissionId);

    Map<String, Object> batchRegrade(List<Long> submissionIds);

    List<Long> findSubmissionIdsByQuestionId(Long questionId);

    List<Long> findSubmissionIdsByTaskId(Long taskId);

    TaskSubmission getSubmissionById(Long submissionId);

    Map<String, Object> getSurveyStats(Long taskId);

    byte[] exportSurvey(Long taskId, boolean blinded);

    Map<String, Object> getTaskStats(Long taskId);
}