package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.AsyncTaskType;
import com.school.teaching.entity.AsyncTask;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.AsyncTaskMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务持久化服务 — DB 为权威来源，Redis 为可选热缓存
 */
@Slf4j
@Service
public class AsyncTaskService {

    private final AsyncTaskMapper asyncTaskMapper;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "async:task:";
    private static final int CACHE_TTL_SECONDS = 120;
    private static final int MAX_RETRIES = 3;
    private static final int COMPLETED_RETENTION_DAYS = 30;

    public AsyncTaskService(AsyncTaskMapper asyncTaskMapper, ObjectMapper objectMapper) {
        this.asyncTaskMapper = asyncTaskMapper;
        this.objectMapper = objectMapper;
    }

    // ═══════════ 创建任务 ═══════════

    @Transactional
    public AsyncTask createTask(AsyncTaskType type, Long createdBy) {
        return createTask(type, type.getDefaultTimeoutSeconds(), createdBy);
    }

    @Transactional
    public AsyncTask createTask(AsyncTaskType type, int timeoutSeconds, Long createdBy) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        LocalDateTime now = LocalDateTime.now();

        AsyncTask task = new AsyncTask();
        task.setTaskId(taskId);
        task.setTaskType(type.name());
        task.setStatus("PENDING");
        task.setRunState("PENDING");
        task.setRetryCount(0);
        task.setMaxRetries(MAX_RETRIES);
        task.setTimeoutSeconds(timeoutSeconds);
        task.setTimeoutAt(now.plusSeconds(timeoutSeconds));
        task.setCreatedBy(createdBy);
        task.setSchoolId(1L);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        asyncTaskMapper.insert(task);
        cacheTask(task);
        log.debug("任务已创建: taskId={}, type={}", taskId, type.name());
        return task;
    }

    // ═══════════ 状态变更 ═══════════

    @Transactional
    public void markRunning(String taskId) {
        AsyncTask task = findByTaskIdRequired(taskId);
        if (!"PENDING".equals(task.getStatus())) return;
        task.setStatus("RUNNING");
        task.setRunState("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        asyncTaskMapper.updateById(task);
        cacheTask(task);
    }

    @Transactional
    public void markCompleted(String taskId, Object result) {
        AsyncTask task = findByTaskIdRequired(taskId);
        task.setStatus("COMPLETED");
        task.setRunState("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        if (result != null) {
            try {
                task.setResultJson(objectMapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                log.error("任务结果序列化失败: taskId={}", taskId, e);
                task.setResultJson("{\"error\":\"序列化失败\"}");
            }
        }
        asyncTaskMapper.updateById(task);
        cacheTask(task);
    }

    @Transactional
    public void markFailed(String taskId, String errorMessage) {
        AsyncTask task = findByTaskIdRequired(taskId);
        int nextRetry = (task.getRetryCount() != null ? task.getRetryCount() : 0) + 1;

        if (canRetry(task, nextRetry)) {
            task.setStatus("PENDING");
            task.setRunState("PENDING");
            task.setRetryCount(nextRetry);
            task.setErrorMessage(errorMessage);
            log.info("任务 {} 失败，将自动重试 ({}/{})", taskId, nextRetry, task.getMaxRetries());
        } else {
            task.setStatus("FAILED");
            task.setRunState("FAILED");
            task.setErrorMessage(errorMessage);
            task.setCompletedAt(LocalDateTime.now());
            log.warn("任务 {} 最终失败: {}", taskId, errorMessage);
        }
        asyncTaskMapper.updateById(task);
        cacheTask(task);
    }

    @Transactional
    public void markCancelled(String taskId) {
        AsyncTask task = findByTaskIdRequired(taskId);
        if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
            throw new BusinessException(409, "任务已完成，无法取消");
        }
        task.setStatus("CANCELLED");
        task.setRunState("CANCELLED");
        task.setCompletedAt(LocalDateTime.now());
        asyncTaskMapper.updateById(task);
        cacheTask(task);
    }

    @Transactional
    public void markTimeout(String taskId) {
        AsyncTask task = findByTaskIdRequired(taskId);
        task.setStatus("TIMEOUT");
        task.setRunState("TIMEOUT");
        task.setErrorMessage("任务执行超时");
        task.setCompletedAt(LocalDateTime.now());
        asyncTaskMapper.updateById(task);
        evictCache(taskId);
    }

    // ═══════════ 查询 ═══════════

    public AsyncTask findByTaskId(String taskId) {
        AsyncTask cached = getCachedTask(taskId);
        if (cached != null) return cached;

        AsyncTask task = asyncTaskMapper.selectOne(
            new LambdaQueryWrapper<AsyncTask>().eq(AsyncTask::getTaskId, taskId));
        if (task != null) cacheTask(task);
        return task;
    }

    public List<AsyncTask> findTimeoutTasks() {
        return asyncTaskMapper.selectList(
            new LambdaQueryWrapper<AsyncTask>()
                .in(AsyncTask::getStatus, "PENDING", "RUNNING")
                .lt(AsyncTask::getTimeoutAt, LocalDateTime.now()));
    }

    // ═══════════ 重试 ═══════════

    @Transactional
    public AsyncTask retry(String taskId) {
        AsyncTask task = findByTaskIdRequired(taskId);
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException(409, "任务已完成，无需重试");
        }
        if (!"FAILED".equals(task.getStatus()) && !"TIMEOUT".equals(task.getStatus())) {
            throw new BusinessException(409, "仅失败或超时任务可手动重试");
        }
        task.setStatus("PENDING");
        task.setRunState("PENDING");
        task.setRetryCount(1);
        task.setTimeoutAt(LocalDateTime.now().plusSeconds(
            task.getTimeoutSeconds() != null ? task.getTimeoutSeconds() : 30));
        task.setErrorMessage(null);
        task.setResultJson(null);
        task.setCompletedAt(null);
        task.setStartedAt(null);
        asyncTaskMapper.updateById(task);
        evictCache(taskId);
        log.info("任务 {} 手动重试", taskId);
        return task;
    }

    // ═══════════ 定时任务 ═══════════

    /** 硬超时扫描 — 每 30 秒 */
    @Scheduled(fixedDelay = 30_000)
    public void scanTimeoutTasks() {
        List<AsyncTask> timeoutTasks = findTimeoutTasks();
        if (timeoutTasks.isEmpty()) return;
        for (AsyncTask task : timeoutTasks) {
            try {
                markTimeout(task.getTaskId());
                log.info("超时标记: taskId={}, type={}", task.getTaskId(), task.getTaskType());
            } catch (Exception e) {
                log.error("超时标记失败: taskId={}", task.getTaskId(), e);
            }
        }
    }

    /** 清理过期已完成任务 — 每天 03:17 */
    @Scheduled(cron = "0 17 3 * * *")
    @Transactional
    public void cleanupCompletedTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(COMPLETED_RETENTION_DAYS);
        List<AsyncTask> oldTasks = asyncTaskMapper.selectList(
            new LambdaQueryWrapper<AsyncTask>()
                .in(AsyncTask::getStatus, "COMPLETED", "FAILED", "TIMEOUT", "CANCELLED")
                .le(AsyncTask::getCompletedAt, cutoff));
        if (oldTasks.isEmpty()) return;

        List<Long> ids = oldTasks.stream().map(AsyncTask::getId).toList();
        asyncTaskMapper.deleteBatchIds(ids);
        for (AsyncTask t : oldTasks) evictCache(t.getTaskId());
        log.info("清理过期任务: {} 条", ids.size());
    }

    /** 服务启动时恢复：将中断的 PENDING/RUNNING 任务标记为 FAILED */
    @PostConstruct
    public void recoverInterruptedTasksOnStartup() {
        List<AsyncTask> interrupted = asyncTaskMapper.selectList(
            new LambdaQueryWrapper<AsyncTask>()
                .in(AsyncTask::getStatus, "PENDING", "RUNNING"));
        if (interrupted.isEmpty()) return;

        for (AsyncTask task : interrupted) {
            task.setStatus("FAILED");
            task.setErrorMessage("服务重启，任务被中断");
            task.setCompletedAt(LocalDateTime.now());
            asyncTaskMapper.updateById(task);
        }
        log.info("启动恢复: {} 个中断任务已标记 FAILED", interrupted.size());
    }

    // ═══════════ 缓存辅助 ═══════════

    private void cacheTask(AsyncTask task) {
        if (redisTemplate == null) return;
        try {
            String json = objectMapper.writeValueAsString(task);
            redisTemplate.opsForValue().set(
                CACHE_PREFIX + task.getTaskId(), json,
                CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Redis 缓存写入失败: taskId={}", task.getTaskId());
        }
    }

    private AsyncTask getCachedTask(String taskId) {
        if (redisTemplate == null) return null;
        try {
            String json = redisTemplate.opsForValue().get(CACHE_PREFIX + taskId);
            if (json == null) return null;
            return objectMapper.readValue(json, AsyncTask.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void evictCache(String taskId) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(CACHE_PREFIX + taskId);
        } catch (Exception ignored) { log.warn("Redis缓存清除失败: taskId={}", taskId); }
    }

    // ═══════════ 内部方法 ═══════════

    private AsyncTask findByTaskIdRequired(String taskId) {
        AsyncTask task = asyncTaskMapper.selectOne(
            new LambdaQueryWrapper<AsyncTask>().eq(AsyncTask::getTaskId, taskId));
        if (task == null) throw new BusinessException(404, "任务不存在: " + taskId);
        return task;
    }

    private boolean canRetry(AsyncTask task, int nextRetry) {
        return nextRetry <= (task.getMaxRetries() != null ? task.getMaxRetries() : MAX_RETRIES);
    }
}
