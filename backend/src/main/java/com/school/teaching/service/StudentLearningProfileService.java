package com.school.teaching.service;

import java.util.Map;

public interface StudentLearningProfileService {
    Map<String, Object> getLearningProfile(Long studentId, String subject);
}
