package com.school.teaching.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.KnowledgeReviewSchedule;
import com.school.teaching.mapper.KnowledgeReviewScheduleMapper;
import com.school.teaching.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MemoryCardPushScheduler {

    @Autowired private KnowledgeReviewScheduleMapper scheduleMapper;
    @Autowired private NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    public void pushMemoryCardReminders() {
        List<KnowledgeReviewSchedule> dueSchedules = scheduleMapper.selectList(
            new LambdaQueryWrapper<KnowledgeReviewSchedule>()
                .eq(KnowledgeReviewSchedule::getSourceType, "CHECKPOINT")
                .le(KnowledgeReviewSchedule::getNextReviewAt, LocalDateTime.now()));

        if (dueSchedules.isEmpty()) {
            log.info("记忆卡推送：无到期复习的闯关知识点");
            return;
        }

        Map<Long, Long> studentDueCount = dueSchedules.stream()
            .collect(Collectors.groupingBy(KnowledgeReviewSchedule::getStudentId, Collectors.counting()));

        int pushedStudents = 0;
        for (Map.Entry<Long, Long> entry : studentDueCount.entrySet()) {
            Long studentId = entry.getKey();
            long count = entry.getValue();

            String title = "考点记忆卡待复习";
            String content = "你有 " + count + " 个知识点到期复习，打开闯关学习看看吧";

            try {
                notificationService.notify(studentId, "MEMORY_CARD_REVIEW", title, content, null);
                pushedStudents++;
            } catch (Exception e) {
                log.warn("记忆卡推送通知失败 studentId={}", studentId, e);
            }
        }

        log.info("记忆卡推送完成：{} 个学生收到提醒，共 {} 个到期知识点",
            pushedStudents, dueSchedules.size());
    }
}
