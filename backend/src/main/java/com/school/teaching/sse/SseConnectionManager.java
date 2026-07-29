package com.school.teaching.sse;

import com.school.teaching.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器 — 维护 userId → SseEmitter 映射。
 * 连接超时 120s，前端自动重连。推送失败时自动清理死连接。
 */
@Slf4j
@Component
public class SseConnectionManager {

    private final ConcurrentHashMap<Long, SseEmitter> connections = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        connections.put(userId, emitter);
        emitter.onCompletion(() -> { connections.remove(userId); log.debug("SSE complete: userId={}", userId); });
        emitter.onTimeout(() -> { connections.remove(userId); log.debug("SSE timeout: userId={}", userId); });
        emitter.onError(e -> { connections.remove(userId); log.debug("SSE error: userId={}", userId); });
        return emitter;
    }

    /** 推送通知到指定用户，用户不在线则静默跳过 */
    public void push(Long userId, Notification notification) {
        SseEmitter emitter = connections.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                .name("notification")
                .data(Map.of(
                    "id", notification.getId() != null ? notification.getId() : 0,
                    "title", notification.getTitle() != null ? notification.getTitle() : "",
                    "content", notification.getContent() != null ? notification.getContent() : "",
                    "type", notification.getType() != null ? notification.getType() : "",
                    "relatedId", notification.getRelatedId() != null ? notification.getRelatedId() : 0,
                    "createTime", notification.getCreateTime() != null ? notification.getCreateTime().toString() : ""
                )));
        } catch (IOException e) {
            connections.remove(userId);
        }
    }

    public int getConnectionCount() {
        return connections.size();
    }

    // ── 竞赛频道广播 ──
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, SseEmitter>> competitionSubscribers = new ConcurrentHashMap<>();

    public void subscribeCompetition(Long compId, Long userId, SseEmitter emitter) {
        competitionSubscribers.computeIfAbsent(compId, k -> new ConcurrentHashMap<>()).put(userId, emitter);
        emitter.onCompletion(() -> unsubscribeCompetition(compId, userId));
        emitter.onTimeout(() -> unsubscribeCompetition(compId, userId));
        emitter.onError(e -> unsubscribeCompetition(compId, userId));
    }

    public void unsubscribeCompetition(Long compId, Long userId) {
        ConcurrentHashMap<Long, SseEmitter> subs = competitionSubscribers.get(compId);
        if (subs != null) {
            subs.remove(userId);
            if (subs.isEmpty()) competitionSubscribers.remove(compId);
        }
    }

    /** 向竞赛频道的所有订阅者广播事件 */
    public void broadcastCompetition(Long compId, String eventName, Object data) {
        ConcurrentHashMap<Long, SseEmitter> subs = competitionSubscribers.get(compId);
        if (subs == null) return;
        subs.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                unsubscribeCompetition(compId, userId);
            }
        });
    }

    // ── 打字竞赛公告频道(全局) ──
    private final ConcurrentHashMap<Long, SseEmitter> typingAnnouncementSubscribers = new ConcurrentHashMap<>();

    public void subscribeTypingAnnouncements(Long userId, SseEmitter emitter) {
        typingAnnouncementSubscribers.put(userId, emitter);
        emitter.onCompletion(() -> typingAnnouncementSubscribers.remove(userId));
        emitter.onTimeout(() -> typingAnnouncementSubscribers.remove(userId));
        emitter.onError(e -> typingAnnouncementSubscribers.remove(userId));
    }

    /** 向所有打字页面在线学生广播公告 */
    public void broadcastTypingAnnouncement(String eventName, Object data) {
        typingAnnouncementSubscribers.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                typingAnnouncementSubscribers.remove(userId);
            }
        });
    }
}
