package com.school.teaching.controller;

import com.school.teaching.common.R;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.sse.SseConnectionManager;
import com.school.teaching.sse.SseTicketStore;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE 实时推送端点。
 *
 * 认证方式：前端先请求 /sse/ticket 获取短期一次性 token（60秒过期），
 * 再用该 ticket 作为 URL 参数连接 SSE，避免主 JWT 暴露在 URL 和日志中。
 */
@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseConnectionManager sseConnectionManager;
    private final SseTicketStore ticketStore;

    /** 获取 SSE 短期 ticket — 前端用此替代主 JWT 放入 EventSource URL */
    @GetMapping("/sse/ticket")
    public R<Map<String, Object>> ticket() {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        String ticket = ticketStore.create(userId, role);
        return R.ok(Map.of("ticket", ticket, "expiresIn", 60));
    }

    @GetMapping("/sse/subscribe")
    public SseEmitter subscribe() {
        Long userId = SecurityUtils.getCurrentUserId();
        return sseConnectionManager.createEmitter(userId);
    }

    /** 每分钟清理过期 ticket */
    @Scheduled(fixedRate = 60_000)
    public void cleanupTickets() {
        ticketStore.cleanup();
    }
}
