package com.school.teaching.service;

import java.util.Map;

/** E10: 非任务行为记录服务 — 向 user_events 表写入事件 */
public interface BehaviorTrackService {
    void track(String eventType, Map<String, Object> eventData);

    /** 异步写入事件（不阻塞主请求，需显式传入 userId/roleName 因为 @Async 线程不继承 SecurityContext） */
    void trackAsync(Long userId, String roleName, String eventType, Object eventData);

    /** 查询事件统计（管理员/教师专用） */
    Map<String, Object> stats(String eventType, int days);
}
