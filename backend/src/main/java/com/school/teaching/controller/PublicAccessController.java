package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.service.ExternalReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公开访问端点 — 无需登录，通过一次性 token 隔离数据。
 * 仅暴露 /access/external-review?token=xxx 一个 GET 和一个 POST。
 */
@RestController
@RequestMapping("/access")
public class PublicAccessController {

    @Autowired private ExternalReviewService service;

    @GetMapping("/external-review")
    public R<?> view(@RequestParam String token) {
        return R.ok(service.getByToken(token));
    }

    @PostMapping("/external-review")
    @AuditLog(eventType = AuditEventType.OTHER, description = "外部评阅提交")
    public R<?> submit(@RequestParam String token, @RequestBody Map<String, Object> scoreData) {
        service.submitReview(token, scoreData);
        return R.ok(Map.of("submitted", true));
    }
}
