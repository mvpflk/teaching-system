package com.school.teaching.security;

import com.school.teaching.utils.RedisRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    @Autowired(required = false)
    private RedisRateLimiter redisRateLimiter;

    /**
     * 检查 IP 或用户 ID 是否被封禁
     * @param ip      客户端 IP
     * @param userId  登录用户名（非数字 ID，登录时尚未认证）
     */
    public boolean isBlocked(String ip, String userId) {
        if (redisRateLimiter != null) {
            return redisRateLimiter.isBlocked(ip, userId);
        }
        return false;
    }

    /**
     * 记录登录尝试
     * @param ip      客户端 IP
     * @param userId  登录用户名
     * @param success true=登录成功，false=失败
     */
    public void recordAttempt(String ip, String userId, boolean success) {
        if (redisRateLimiter != null) {
            redisRateLimiter.recordAttempt(ip, userId, success);
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        if (redisRateLimiter != null) {
            redisRateLimiter.cleanupLocal();
        }
    }
}
