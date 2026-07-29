package com.school.teaching.event;

import com.school.teaching.entity.Notification;
import com.school.teaching.mapper.NotificationMapper;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.PeerReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 互评截止提醒 — 每小时检查一次，截止前24h内发提醒，每人仅一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PeerReviewReminderScheduler {

    private final PeerReviewService peerReviewService;
    private final NotificationMapper notificationMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *") // 每小时整点
    public void checkAndRemind() {
        try {
            List<Map<String, Object>> reminders = peerReviewService.findPendingReminders();
            int sent = 0;
            for (Map<String, Object> r : reminders) {
                Long userId = ((Number) r.get("userId")).longValue();
                Long taskId = ((Number) r.get("taskId")).longValue();
                // 检查是否已发过（同一任务+同一用户只发一次）
                long count = notificationMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getType, "PEER_REVIEW_REMINDER")
                        .eq(Notification::getRelatedId, taskId));
                if (count > 0) continue;

                notificationService.notify(userId, "PEER_REVIEW_REMINDER", "互评提醒",
                    "你还有互评任务未完成，请在 " + r.get("deadline") + " 前完成「" + r.get("taskTitle") + "」的互评。",
                    taskId);
                sent++;
            }
            if (sent > 0) log.info("互评提醒已发送 {} 条", sent);
        } catch (Exception e) {
            log.warn("互评提醒定时任务异常: {}", e.getMessage());
        }
    }
}
