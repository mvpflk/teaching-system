package com.school.teaching.service;

import com.school.teaching.entity.ParentFeedbackResponse;

import java.util.List;
import java.util.Map;

public interface ParentFeedbackResponseService {
    ParentFeedbackResponse submit(ParentFeedbackResponse response);
    List<ParentFeedbackResponse> getByForm(Long formId);
    List<Map<String, Object>> getPendingForParent(Long parentId);
}
