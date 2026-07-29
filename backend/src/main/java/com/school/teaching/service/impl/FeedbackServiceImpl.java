package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.Feedback;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.FeedbackMapper;
import com.school.teaching.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackMapper feedbackMapper;

    @Override
    public void submit(Long userId, Map<String, String> body) {
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setType(body.getOrDefault("type", "OTHER"));
        fb.setTitle(body.get("title"));
        fb.setContent(body.get("content"));
        fb.setPageUrl(body.get("pageUrl"));
        fb.setBrowserInfo(body.get("browserInfo"));
        fb.setStatus("OPEN");
        fb.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(fb);
    }

    @Override
    public Page<Feedback> list(int page, int size, String status) {
        LambdaQueryWrapper<Feedback> q = new LambdaQueryWrapper<Feedback>()
                .orderByDesc(Feedback::getCreatedAt);
        if (status != null && !status.isEmpty()) q.eq(Feedback::getStatus, status);
        return feedbackMapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public Feedback getById(Long id) {
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) throw new BusinessException(404, "反馈不存在");
        return fb;
    }

    @Override
    public void update(Long id, Map<String, Object> body, Long resolvedBy) {
        Feedback fb = getById(id);
        if (body.containsKey("status")) fb.setStatus((String) body.get("status"));
        if (body.containsKey("adminNote")) fb.setAdminNote((String) body.get("adminNote"));
        if ("RESOLVED".equals(body.get("status")) || "CLOSED".equals(body.get("status"))) {
            fb.setResolvedBy(resolvedBy);
            fb.setResolvedAt(LocalDateTime.now());
        }
        feedbackMapper.updateById(fb);
    }
}
