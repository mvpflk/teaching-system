package com.school.teaching.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> teacherStats(Long classId);
}
