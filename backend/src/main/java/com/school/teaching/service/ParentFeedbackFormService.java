package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.teaching.entity.ParentFeedbackForm;

import java.util.List;
import java.util.Map;

public interface ParentFeedbackFormService {
    ParentFeedbackForm create(ParentFeedbackForm form);
    ParentFeedbackForm update(Long id, ParentFeedbackForm form);
    void delete(Long id);
    ParentFeedbackForm getById(Long id);
    IPage<ParentFeedbackForm> getPage(Long classId, String period, int page, int size);
    void send(Long formId);
    void close(Long formId);
    Map<String, Object> getStats(Long formId);
    Map<String, Object> generateSummary(Long formId);
}
