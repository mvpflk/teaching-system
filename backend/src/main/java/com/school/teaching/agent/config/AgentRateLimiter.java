package com.school.teaching.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Agent 每日调用次数限制（Redis 实现）。
 * 教师 20 次/天，学生 10 次/天。
 */
@Slf4j
@Component
public class AgentRateLimiter {

    @Value("${agent.rate-limit.teacher:20}")
    private int teacherLimit;

    @Value("${agent.rate-limit.student:10}")
    private int studentLimit;

    private static final String PREFIX = "agent:ratelimit:";

    private final StringRedisTemplate redis;

    public AgentRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 检查是否可以调用。
     * @return null 表示可以调用；非 null 返回提示消息
     */
    public String checkLimit(Long userId, String roleName) {
        boolean isStudent = "STUDENT".equals(roleName);
        int limit = isStudent ? studentLimit : teacherLimit;

        String key = PREFIX + userId + ":" + LocalDate.now();

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofSeconds(secondsUntilEndOfDay()));
        }

        if (count != null && count > limit) {
            String msg = "今日 AI 调用次数已用完（" + limit + "次/天）。请明天再试或联系管理员提升配额。";
            log.info("RateLimit: userId={}, count={}/{}, denied", userId, count, limit);
            return msg;
        }

        log.debug("RateLimit: userId={}, count={}/{}", userId, count, limit);
        return null;
    }

    /**
     * 查询免费额度是否还有剩余（不消耗额度）。
     * @return true = 还有免费额度可用
     */
    public boolean hasFreeQuota(Long userId, String roleName) {
        boolean isStudent = "STUDENT".equals(roleName);
        int limit = isStudent ? studentLimit : teacherLimit;
        String key = PREFIX + userId + ":" + LocalDate.now();
        String val = redis.opsForValue().get(key);
        long used = val != null ? Long.parseLong(val) : 0;
        return used < limit;
    }

    /** 获取当前用户的调用计数和限额（用于前端展示） */
    public Map<String, Object> getDailyUsage(Long userId, String roleName) {
        boolean isStudent = "STUDENT".equals(roleName);
        int limit = isStudent ? studentLimit : teacherLimit;
        String key = PREFIX + userId + ":" + LocalDate.now();
        String val = redis.opsForValue().get(key);
        long used = val != null ? Long.parseLong(val) : 0;
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("used", used);
        result.put("limit", limit);
        result.put("remaining", Math.max(0, limit - used));
        return result;
    }

    private long secondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = now.toLocalDate().plusDays(1).atStartOfDay();
        return ChronoUnit.SECONDS.between(now, endOfDay) + 1;
    }
}
