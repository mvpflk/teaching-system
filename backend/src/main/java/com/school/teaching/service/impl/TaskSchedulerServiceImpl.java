package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.common.TaskCategory;
import com.school.teaching.entity.Student;
import com.school.teaching.entity.Task;
import com.school.teaching.entity.TaskSubmission;
import com.school.teaching.event.TaskEvent;
import com.school.teaching.mapper.StudentMapper;
import com.school.teaching.mapper.TaskMapper;
import com.school.teaching.mapper.TaskSubmissionMapper;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.TaskSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final StudentMapper studentMapper;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    /** 无 deadline 的教学任务（考试/作业/问卷）自发布起多少天后自动关闭 */
    private static final long NO_DEADLINE_EXPIRE_DAYS = 7;
    /** 无 deadline 的实训/仿真任务自发布起多少天后自动关闭（长期任务保留更久） */
    private static final long PRACTICE_EXPIRE_DAYS = 30;
    /** 实训/仿真任务的 taskType 集合 */
    private static final Set<String> PRACTICE_TYPES = Set.of("PRACTICE", "SIMULATION");

    /** 每5分钟: 关闭所有已过 deadline 的进行中任务 */
    @Override @Transactional
    @Scheduled(cron = "7 */5 * * * *")
    public void closeExpiredTasks() {
        // 1. 有关 deadline 且已过期的任务 — 立即关闭
        List<Task> expired = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .lt(Task::getDeadline, LocalDateTime.now())
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING")));

        // 2. 无 deadline 的教学任务（考试/作业/问卷）— 7天自动关闭
        List<Task> noDeadlineTeaching = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .isNull(Task::getDeadline)
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING"))
            .notIn(Task::getTaskType, PRACTICE_TYPES)
            .lt(Task::getCreatedAt, LocalDateTime.now().minusDays(NO_DEADLINE_EXPIRE_DAYS)));

        // 3. 无 deadline 的实训/仿真任务 — 30天自动关闭
        List<Task> noDeadlinePractice = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .isNull(Task::getDeadline)
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING"))
            .in(Task::getTaskType, PRACTICE_TYPES)
            .lt(Task::getCreatedAt, LocalDateTime.now().minusDays(PRACTICE_EXPIRE_DAYS)));

        List<Task> allToClose = new java.util.ArrayList<>();
        allToClose.addAll(expired);
        allToClose.addAll(noDeadlineTeaching);
        allToClose.addAll(noDeadlinePractice);
        if (allToClose.isEmpty()) return;

        // 逐任务关闭（避免单个任务失败导致整批回滚）
        int closed = 0;
        List<Long> closedIds = new java.util.ArrayList<>();
        for (Task t : allToClose) {
            try {
                taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                    .eq(Task::getId, t.getId())
                    .set(Task::getStatus, "CLOSED"));

                // 自动豁免 PENDING 状态提交
                submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
                    .eq(TaskSubmission::getTaskId, t.getId())
                    .eq(TaskSubmission::getStatus, "PENDING")
                    .set(TaskSubmission::getStatus, "EXEMPTED"));

                eventPublisher.publishEvent(TaskEvent.closed(this, t.getId(),
                    TaskCategory.valueOf(t.getTaskType()), t.getTeacherId(),
                    Map.of("targetId", Objects.toString(t.getTargetId(), ""))));
                closedIds.add(t.getId());
                closed++;
            } catch (Exception e) {
                log.error("关闭过期任务失败 taskId={}: {}", t.getId(), e.getMessage(), e);
            }
        }
        if (!allToClose.isEmpty()) log.info("定时关闭过期任务: deadline过期{}个, 教学任务超{}天{}个, 实训仿真超{}天{}个",
            expired.size(), NO_DEADLINE_EXPIRE_DAYS, noDeadlineTeaching.size(), PRACTICE_EXPIRE_DAYS, noDeadlinePractice.size());
    }

    /** 每10分钟: 截止前1小时内未提交学生发送提醒 */
    @Override
    @Transactional
    @Scheduled(cron = "37 */10 * * * *")
    public void sendDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime window = now.plusHours(1);

        List<Task> approaching = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .ge(Task::getDeadline, now)
            .lt(Task::getDeadline, window)
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING")));
        if (approaching.isEmpty()) return;

        // 批量查所有相关提交，避免 N+1
        Set<Long> taskIds = approaching.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskSubmission> allSubs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .in(TaskSubmission::getTaskId, taskIds)
                .ne(TaskSubmission::getStatus, "EXEMPTED"));
        Set<String> submitted = allSubs.stream()
            .map(s -> s.getTaskId() + "_" + s.getStudentId()).collect(Collectors.toSet());

        int reminded = 0;
        for (Task t : approaching) {
            if (!"CLASS".equals(t.getTargetType()) || t.getTargetId() == null) continue;
            List<Student> classStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, t.getTargetId()));
            List<Long> unsubmittedUserIds = classStudents.stream()
                .filter(s -> !submitted.contains(t.getId() + "_" + s.getId()))
                .map(Student::getUserId)
                .filter(Objects::nonNull)
                .toList();
            if (!unsubmittedUserIds.isEmpty()) {
                notificationService.notifyBatch(unsubmittedUserIds,
                    "TASK_DEADLINE", "任务即将截止",
                    "任务「" + t.getTitle() + "」将在1小时内截止，请尽快提交", t.getId());
                reminded += unsubmittedUserIds.size();
            }
        }
        if (reminded > 0) log.info("截止提醒: {} 个任务, 共提醒 {} 名未提交学生", approaching.size(), reminded);
    }

    @Override @Scheduled(cron = "53 0 * * * *")
    public void autoCloseAndGrade() {
        closeExpiredTasks();
    }

    /** 每1分钟: 扫描考试时长超限的 PENDING 提交，自动终止并记0分 */
    @Override @Transactional
    @Scheduled(cron = "19 * * * * *")
    public void terminateExpiredExams() {
        // 1. 找到所有考试类型的 PENDING 提交
        List<TaskSubmission> pendingSubs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStatus, "PENDING")
                .isNotNull(TaskSubmission::getCreatedAt)
                .gt(TaskSubmission::getCreatedAt, LocalDateTime.now().minusDays(1))); // 只看近24h，避免全表扫描

        if (pendingSubs.isEmpty()) return;

        // 2. 批量加载关联任务（只取考试类型）
        Set<Long> taskIds = pendingSubs.stream()
            .map(TaskSubmission::getTaskId).collect(Collectors.toSet());
        List<Task> tasks = taskMapper.selectBatchIds(taskIds).stream()
            .filter(t -> "FORMATIVE".equals(t.getTaskType()) || "SUMMATIVE".equals(t.getTaskType()))
            .toList();
        Map<Long, Task> taskMap = tasks.stream()
            .collect(Collectors.toMap(Task::getId, t -> t));

        int terminated = 0;
        for (TaskSubmission sub : pendingSubs) {
            Task task = taskMap.get(sub.getTaskId());
            if (task == null) continue;

            int durationMinutes = getDurationMinutes(task);
            if (durationMinutes <= 0) continue;

            if (LocalDateTime.now().isAfter(sub.getCreatedAt().plusMinutes(durationMinutes))) {
                submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
                    .eq(TaskSubmission::getId, sub.getId())
                    .set(TaskSubmission::getStatus, "TERMINATED")
                    .set(TaskSubmission::getCheatTerminated, 0)
                    .set(TaskSubmission::getScore, java.math.BigDecimal.ZERO)
                    .set(TaskSubmission::getSubmittedAt, LocalDateTime.now())
                    .set(TaskSubmission::getGradedAt, LocalDateTime.now())
                    .set(TaskSubmission::getGradingMessage,
                        "考试时间到（限时" + durationMinutes + "分钟），系统自动终止答题并记为0分。"));
                terminated++;
            }
        }
        if (terminated > 0) log.info("定时终止超时考试: {} 个PENDING提交已自动终止", terminated);
    }

    /** 从 taskConfig 中解析考试时长（分钟），解析失败返回 0 */
    private int getDurationMinutes(Task task) {
        try {
            if (task.getTaskConfig() == null || task.getTaskConfig().isBlank()) return 0;
            Map<String, Object> cfg = com.school.teaching.utils.JsonUtils.parseMap(task.getTaskConfig());
            if (cfg.containsKey("durationMinutes")) {
                return Integer.parseInt(cfg.get("durationMinutes").toString());
            }
        } catch (Exception ignored) { log.warn("任务配置解析失败", ignored); }
        return 0;
    }
}
