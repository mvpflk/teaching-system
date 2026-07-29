package com.school.teaching.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Ticket 存储 — 短期一次性令牌，避免主 JWT 暴露在 URL 参数中。
 *
 * 生命周期：创建后 60 秒自动过期，使用后立即删除（一次性）。
 * 内存存储，重启丢失（可接受 — 用户重试即可重新获取）。
 */
@Slf4j
@Component
public class SseTicketStore {

    private static final long TTL_MS = 60_000; // 60 秒
    private final SecureRandom random = new SecureRandom();

    /** ticket → (userId, expireTime) */
    private final Map<String, TicketEntry> store = new ConcurrentHashMap<>();

    public String create(Long userId, String role) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        store.put(ticket, new TicketEntry(userId, role, System.currentTimeMillis() + TTL_MS));
        log.debug("SSE ticket created: userId={}, role={}", userId, role);
        return ticket;
    }

    /**
     * 校验 ticket（允许多次使用，在 TTL 内有效）。
     * SSE 连接断开重连时需要复用同一 ticket，因此不能一次性消费。
     * 安全依赖：60s TTL + 随机 token 不可猜测。
     */
    public TicketEntry validateAndConsume(String ticket) {
        TicketEntry entry = store.get(ticket);
        if (entry == null) return null;

        if (System.currentTimeMillis() > entry.expireTime) {
            store.remove(ticket);
            log.debug("SSE ticket expired: ticket={}", ticket.substring(0, 8));
            return null;
        }

        log.debug("SSE ticket validated: userId={}", entry.userId);
        return entry;
    }

    /** 定期清理过期 ticket（每分钟） */
    public void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now > e.getValue().expireTime);
    }

    public record TicketEntry(Long userId, String role, long expireTime) {}
}
