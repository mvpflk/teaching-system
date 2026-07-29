package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.UserEvent;
import com.school.teaching.mapper.UserEventMapper;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.BehaviorTrackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class BehaviorTrackServiceImpl implements BehaviorTrackService {

    @Autowired private UserEventMapper userEventMapper;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void track(String eventType, Map<String, Object> eventData) {
        try {
            UserEvent event = new UserEvent();
            event.setUserId(SecurityUtils.getCurrentUserId());
            event.setRoleName(SecurityUtils.getCurrentRole());
            event.setEventType(eventType);
            event.setEventData(om.writeValueAsString(eventData));
            userEventMapper.insert(event);
        } catch (Exception e) {
            log.warn("BehaviorTrack failed: {}", e.getMessage());
        }
    }

    @Override
    @Async
    public void trackAsync(Long userId, String roleName, String eventType, Object eventData) {
        try {
            UserEvent event = new UserEvent();
            event.setUserId(userId);
            event.setRoleName(roleName);
            event.setEventType(eventType);
            event.setEventData(om.writeValueAsString(eventData != null ? eventData : Map.of()));
            event.setCreatedAt(LocalDateTime.now());
            userEventMapper.insert(event);
        } catch (Exception e) {
            // 埋点失败不影响业务，静默处理
            log.warn("埋点写入失败 userId={} eventType={}: {}", userId, eventType, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> stats(String eventType, int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();
        LambdaQueryWrapper<UserEvent> w = new LambdaQueryWrapper<UserEvent>()
                .ge(UserEvent::getCreatedAt, since);
        if (eventType != null && !eventType.isBlank()) w.eq(UserEvent::getEventType, eventType);

        List<UserEvent> events = userEventMapper.selectList(w);

        // 聚合统计
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEvents", events.size());
        stats.put("days", days);

        // 各事件类型计数
        Map<String, Long> byType = new LinkedHashMap<>();
        for (UserEvent e : events) {
            byType.merge(e.getEventType(), 1L, Long::sum);
        }
        stats.put("byType", byType);

        // 各学科计数
        Map<String, Long> bySubject = new LinkedHashMap<>();
        for (UserEvent e : events) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = om.readValue(e.getEventData(), Map.class);
                Object subjObj = data.getOrDefault("subject", "未知");
                String subj = subjObj != null ? subjObj.toString() : "未知";
                bySubject.merge(subj, 1L, Long::sum);
            } catch (Exception ignored) { log.warn("行为事件JSON解析失败: eventId={}, error={}", e.getId(), ignored.getMessage()); }
        }
        stats.put("bySubject", bySubject);

        // 活跃用户数
        long uniqueUsers = events.stream().map(UserEvent::getUserId).distinct().count();
        stats.put("uniqueUsers", uniqueUsers);

        // 每日趋势
        Map<String, Long> daily = new LinkedHashMap<>();
        for (UserEvent e : events) {
            String day = e.getCreatedAt().toLocalDate().toString();
            daily.merge(day, 1L, Long::sum);
        }
        stats.put("dailyTrend", daily);

        return stats;
    }
}
