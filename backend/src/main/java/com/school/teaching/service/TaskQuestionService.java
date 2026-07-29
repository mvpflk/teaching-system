package com.school.teaching.service;

import com.school.teaching.entity.TaskQuestion;

import java.util.List;
import java.util.Map;

public interface TaskQuestionService {

    List<TaskQuestion> getQuestions(Long taskId);

    List<Map<String, Object>> getQuestionsWithDetails(Long taskId);

    void batchSaveQuestions(List<TaskQuestion> questions);

    void addQuestions(Long taskId, List<Long> questionIds);

    void removeQuestions(Long taskId, List<Long> questionIds);

    void recalcTaskTotalScore(Long taskId);

    List<Map<String, Object>> getStudentQuestions(Long taskId);

    void fixQuestionStatus(List<Map<String, Object>> questions, String subject, Long schoolId);
}