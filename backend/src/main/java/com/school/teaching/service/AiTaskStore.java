package com.school.teaching.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.common.AsyncTaskType;
import com.school.teaching.entity.AsyncTask;
import com.school.teaching.service.impl.AsyncTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 异步任务存储 — 委托 AsyncTaskService（DB 持久化 + Redis 缓存），
 * 仅保留 SSE 会话管理在内存中。
 */
@Slf4j
@Component
public class AiTaskStore {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private AsyncTaskService asyncTaskService;

    // ── SSE 会话管理（不持久化，重启后重新连接）──
    private final ConcurrentHashMap<String, SseEmitter> sseSessions = new ConcurrentHashMap<>();

    // ═══════════ 创建任务 ═══════════

    public String create() {
        return create(DEFAULT_TIMEOUT_SECONDS);
    }

    public String create(int timeoutSeconds) {
        AsyncTask task = asyncTaskService.createTask(AsyncTaskType.AI_GENERATE, timeoutSeconds, null);
        return task.getTaskId();
    }

    public String create(AsyncTaskType type, int timeoutSeconds, Long createdBy) {
        AsyncTask task = asyncTaskService.createTask(type, timeoutSeconds, createdBy);
        return task.getTaskId();
    }

    // ═══════════ 状态变更 ═══════════

    public void complete(String taskId, Object result) {
        asyncTaskService.markCompleted(taskId, result);
        notifySse(taskId, "COMPLETED", result, null);
    }

    public void fail(String taskId, String error) {
        asyncTaskService.markFailed(taskId, error);
        notifySse(taskId, "FAILED", null, error);
    }

    public void markRunning(String taskId) {
        try {
            asyncTaskService.markRunning(taskId);
        } catch (Exception e) {
            log.error("markRunning DB写入失败: taskId={}", taskId, e);
        }
    }

    // ═══════════ 查询 ═══════════

    public TaskEntry get(String taskId) {
        AsyncTask entity = asyncTaskService.findByTaskId(taskId);
        if (entity == null) return null;

        TaskEntry entry = TaskEntry.fromEntity(entity);
        // 懒检测超时（双重保险：Scheduled 扫描 + 读取时检测）
        if ("PENDING".equals(entry.status) && entity.getTimeoutAt() != null
            && LocalDateTime.now().isAfter(entity.getTimeoutAt())) {
            asyncTaskService.markTimeout(taskId);
            entry.status = "TIMEOUT";
            entry.error = "AI处理超时，请重试";
        }
        return entry;
    }

    // ═══════════ SSE 会话 ═══════════

    public SseEmitter registerSse(String taskId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        sseSessions.put(taskId, emitter);
        emitter.onCompletion(() -> sseSessions.remove(taskId));
        emitter.onTimeout(() -> { sseSessions.remove(taskId); emitter.complete(); });
        emitter.onError(e -> sseSessions.remove(taskId));
        return emitter;
    }

    private void notifySse(String taskId, String status, Object result, String error) {
        SseEmitter emitter = sseSessions.remove(taskId);
        if (emitter == null) return;
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("status", status);
            event.put("taskId", taskId);
            if (result != null) event.put("result", result);
            if (error != null) event.put("error", error);
            emitter.send(SseEmitter.event().name("task-result").data(event));
            emitter.complete();
        } catch (Exception e) {
            try { emitter.completeWithError(e); } catch (Exception ignored) {
                // 静默降级：emitter 完成失败说明连接已断开，无需处理
            }
        }
    }

    // ═══════════ 清理（no-op，AsyncTaskService 负责 DB + Redis TTL 清理） ═══════════

    public void cleanup() { }

    // ═══════════ TaskEntry DTO ═══════════

    public static class TaskEntry {
        public String taskId;
        public String status;
        public Object result;
        public String error;
        public long createdAt;
        public long timeoutAt;

        public TaskEntry() {}

        /** 从 DB 实体构建 TaskEntry */
        public static TaskEntry fromEntity(AsyncTask entity) {
            TaskEntry entry = new TaskEntry();
            entry.taskId = entity.getTaskId();
            entry.status = entity.getStatus();
            entry.error = entity.getErrorMessage();
            if (entity.getCreatedAt() != null) {
                entry.createdAt = entity.getCreatedAt()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            if (entity.getTimeoutAt() != null) {
                entry.timeoutAt = entity.getTimeoutAt()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            if (entity.getResultJson() != null && !entity.getResultJson().isEmpty()) {
                try {
                    entry.result = new ObjectMapper().readValue(entity.getResultJson(), Object.class);
                } catch (Exception e) {
                    entry.result = entity.getResultJson();
                }
            }
            return entry;
        }
    }
}
