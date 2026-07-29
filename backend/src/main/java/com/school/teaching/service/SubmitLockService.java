package com.school.teaching.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 考试提交分布式锁服务
 *
 * 设计目标：
 *   1. 防止同一学生同一考试并发重复提交（幂等性）
 *   2. 双击提交场景：第二次请求等待首次完成后返回已有结果，不报错
 *   3. Redis 可用时使用 SETNX 分布式锁；不可用时降级为本地 JVM 锁
 *   4. 锁 TTL=30s，防止死锁
 */
@Service
public class SubmitLockService {

    private static final Logger log = LoggerFactory.getLogger(SubmitLockService.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "exam:submit:";
    private static final long LOCK_TTL_SECONDS = 30;
    private static final long WAIT_TIMEOUT_MS = 5000; // 等待首次提交完成的超时

    // Redis 不可用时的本地锁回退
    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    /** 字符串键版 — 用于统一任务系统（key = "task:submit:{taskId}:{studentId}"） */
    public SubmitLock tryLockString(String key) {
        return tryLockInternal("task:submit:" + key);
    }

    /** 任务编辑锁 — 防止并发编辑同一任务（key = "task:edit:{taskId}"） */
    public SubmitLock tryLockTaskEdit(Long taskId) {
        return tryLockInternal("task:edit:" + taskId);
    }

    /** @deprecated 保留兼容考试旧链路，新代码使用 tryLockString */
    public SubmitLock tryLock(Long resultId) {
        return tryLockInternal(LOCK_PREFIX + resultId);
    }

    private SubmitLock tryLockInternal(String key) {
        boolean acquired = false;

        // 优先 Redis
        if (redisTemplate != null) {
            try {
                acquired = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.error("Redis分布式锁获取失败，降级到本地锁", e);
            }
        }

        // Redis 未获取到或不可用 → 本地锁
        if (!acquired) {
            ReentrantLock lock = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
            try {
                acquired = lock.tryLock(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (acquired) {
                return new SubmitLock(key, lock, null);
            }
            return null;
        }

        // Redis 获取成功
        // 同时获取本地锁防止同 JVM 内并发
        ReentrantLock localLock = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            localLock.tryLock(100, TimeUnit.MILLISECONDS); // 尽力获取，失败也无妨（Redis 已保护）
        } catch (InterruptedException ignored) { log.error("本地锁获取被中断", ignored); }
        return new SubmitLock(key, localLock, redisTemplate);
    }

    /**
     * 分布式锁封装
     */
    public static class SubmitLock implements AutoCloseable {
        private final String key;
        private final ReentrantLock localLock;
        private final StringRedisTemplate redis;

        SubmitLock(String key, ReentrantLock localLock, StringRedisTemplate redis) {
            this.key = key;
            this.localLock = localLock;
            this.redis = redis;
        }

        @Override
        public void close() {
            // 释放本地锁
            if (localLock != null && localLock.isHeldByCurrentThread()) {
                localLock.unlock();
            }
            // 释放 Redis 锁
            if (redis != null) {
                try { redis.delete(key); } catch (Exception ignored) { log.error("释放Redis锁失败 key={}", key, ignored); }
            }
        }
    }
}
