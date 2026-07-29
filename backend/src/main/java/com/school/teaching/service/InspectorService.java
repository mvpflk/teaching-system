package com.school.teaching.service;

import java.util.List;
import java.util.Map;

public interface InspectorService {
    Map<String, Object> dashboard();
    Map<String, Object> scoreAnalysis(Long stageId, String grade, Long classId, Long taskId);
    Map<String, Object> peerReviewStats(Long stageId, String grade);
    Map<String, Object> reviewProgress();
    Map<String, Object> creditStats();
    List<Map<String, Object>> teacherActivity();
    Map<String, Object> getDashboardTrend(String period);
    Map<String, Object> getTeacherProfile(Long teacherId);
    Map<String, Object> getClassProfile(Long classId);
    List<Map<String, Object>> getTeachingGroupQuality();
    Map<String, Object> getPracticeStats();
}
