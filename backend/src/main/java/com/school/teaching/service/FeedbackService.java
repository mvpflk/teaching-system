package com.school.teaching.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.Feedback;

import java.util.Map;

public interface FeedbackService {
    void submit(Long userId, Map<String, String> body);
    Page<Feedback> list(int page, int size, String status);
    Feedback getById(Long id);
    void update(Long id, Map<String, Object> body, Long resolvedBy);
}
