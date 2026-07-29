package com.school.teaching.service.impl;

import com.school.teaching.entity.QuestionSkipLog;
import com.school.teaching.mapper.QuestionSkipLogMapper;
import com.school.teaching.service.QuestionSkipLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionSkipLogServiceImpl implements QuestionSkipLogService {

    private final QuestionSkipLogMapper skipLogMapper;

    @Override
    public void logSkip(Long studentId, QuestionSkipLog log) {
        log.setStudentId(studentId);
        skipLogMapper.insert(log);
    }
}
