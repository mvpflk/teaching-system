package com.school.teaching.scheduler;

import com.school.teaching.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskPublishScheduler.class);

    @Autowired private TaskService taskService;

    /** 每分钟扫描一次，发布到时的定时任务；乐观锁防重复 */
    @Async
    @Scheduled(cron = "0 */1 * * * *")
    @SchedulerLock(name = "task_publish", lockAtMostFor = "2m")
    public void publishDueTasks() {
        try {
            int count = taskService.publishScheduledTasks();
            if (count > 0) log.info("[定时发布] 发布 {} 个任务", count);
        } catch (Exception e) {
            log.error("[定时发布] 扫描失败", e);
        }
    }
}
