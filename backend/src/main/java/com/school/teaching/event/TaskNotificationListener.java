package com.school.teaching.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * 任务事件 → 通知 监听器。
 * 全部使用 @TransactionalEventListener(afterCommit) + @Async 解耦，
 * 确保通知失败不回滚主业务事务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskNotificationListener {

    private final NotificationService notificationService;
    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;

    // ── 任务发布 → 通知班级学生 ──────────────────────

    @Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskPublished(TaskEvent event) {
        if (!"task_published".equals(event.getEventType())) return;
        Map<String, Object> data = event.getData();
        Long targetId = data.get("targetId") instanceof String s && !s.isEmpty() ? Long.valueOf(s) : null;
        if (targetId != null) {
            notificationService.notifyClassStudents(targetId, "TASK_PUBLISHED",
                "新任务发布", "你有新的任务: " + data.getOrDefault("title", ""), event.getTaskId());
        }
    }

    // ── 任务提交 → 通知教师 ─────────────────────────

    @Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskSubmitted(TaskEvent event) {
        if (!"task_submitted".equals(event.getEventType())) return;
        if (event.getTeacherId() != null) {
            notificationService.notify(event.getTeacherId(), "TASK_SUBMITTED",
                "任务提交", "有学生提交了任务", event.getTaskId());
        }
    }

    // ── 任务评分 → 通知学生 ─────────────────────────

    @Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskGraded(TaskEvent event) {
        if (!"task_graded".equals(event.getEventType())) return;
        if (event.getStudentId() != null) {
            Student student = studentMapper.selectById(event.getStudentId());
            if (student == null) return;
            String score = String.valueOf(event.getData().getOrDefault("score", ""));
            notificationService.notify(student.getUserId(), "TASK_GRADED",
                "成绩已公布", "你的任务得分: " + score + " 分", event.getTaskId());
        }
    }

    // ── 任务关闭 → 通知班级学生 ─────────────────────

    @Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskClosed(TaskEvent event) {
        if (!"task_closed".equals(event.getEventType())) return;
        Map<String, Object> data = event.getData();
        Long targetId = data.get("targetId") instanceof String s && !s.isEmpty() ? Long.valueOf(s) : null;
        if (targetId != null) {
            notificationService.notifyClassStudents(targetId, "TASK_CLOSED",
                "任务已关闭", "任务已截止", event.getTaskId());
        }
    }
}
