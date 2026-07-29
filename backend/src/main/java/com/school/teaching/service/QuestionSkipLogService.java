package com.school.teaching.service;

import com.school.teaching.entity.QuestionSkipLog;

public interface QuestionSkipLogService {
    void logSkip(Long studentId, QuestionSkipLog log);
}
