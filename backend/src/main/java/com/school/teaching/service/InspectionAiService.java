package com.school.teaching.service;

import java.time.LocalDate;
import java.util.Map;

public interface InspectionAiService {
    String generateWeeklySummary(LocalDate weekStart, LocalDate weekEnd);
    Map<String, Object> detectAnomalies();
    Map<String, Object> getRecommendations();
    String analyzeTeachingResearch();
    String analyzeLessonPrep();
}
