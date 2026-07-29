package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.event.TaskEvent;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.ParentNotificationService;
import com.school.teaching.service.TaskPublishService;
import com.school.teaching.common.TaskTypeHandlerSelector;
import com.school.teaching.common.TaskTypeHandler;
import com.school.teaching.common.TaskCategory;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskPublishServiceImpl implements TaskPublishService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;
    private final NotificationService notificationService;
    private final ParentNotificationService parentNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskTypeHandlerSelector handlerSelector;

    private static final Logger log = LoggerFactory.getLogger(TaskPublishServiceImpl.class);

    @Override
    @Transactional
    @CacheEvict(value = "task_list", allEntries = true)
    public Task publish(Long id) {
        Task t = taskMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "任务不存在");
        if (!"DRAFT".equals(t.getStatus())) throw new BusinessException(409, "仅草稿可发布");

        if (t.getIsCompetitionMode() != null && t.getIsCompetitionMode() == 1) {
            Map<String, Object> config = parseConfig(t.getTaskConfig());
            config.putIfAbsent("maxCheatWarnings", 1);
            try {
                t.setTaskConfig(JsonUtils.MAPPER.writeValueAsString(config));
            } catch (Exception ignored) { /* 序列化失败不影响发布 */ }
        }

        t.setStatus("PUBLISHED");
        taskMapper.updateById(t);

        parentNotificationService.notifyParentsForTask(t);

        TaskTypeHandler handler = handlerSelector.get(TaskCategory.valueOf(t.getTaskType()));
        if (handler != null) {
            handler.onPublish(new TaskTypeHandler.TaskContext(id, null, t.getTeacherId(),
                parseConfig(t.getTaskConfig()), null, Map.of("title", t.getTitle())));
        }

        if (t.getIsForced() != null && t.getIsForced() == 1) {
            String forcedGrade = null;
            try {
                if (t.getTaskConfig() != null) {
                    Map<?,?> cfg = JsonUtils.MAPPER.readValue(t.getTaskConfig(), Map.class);
                    forcedGrade = (String) cfg.get("forcedGrade");
                }
            } catch (Exception ignored) { /* taskConfig 解析失败则默认全校 */ }
            LambdaQueryWrapper<Classes> w = new LambdaQueryWrapper<Classes>().eq(Classes::getStatus, 1);
            if (forcedGrade != null && !forcedGrade.isBlank()) {
                w.eq(Classes::getGrade, forcedGrade);
            }
            List<Classes> targetClasses = classesMapper.selectList(w);
            if (targetClasses.isEmpty()) {
                log.info("强制任务 {} 无匹配班级（年级={}）", id, forcedGrade);
            } else {
                List<Long> classIds = targetClasses.stream().map(Classes::getId).toList();
                List<Student> allStudents = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().in(Student::getClassId, classIds));

                List<Long> allStudentIds = allStudents.stream().map(Student::getId).toList();
                List<TaskSubmission> existingSubs = submissionMapper.selectList(
                    new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskId, id)
                        .in(TaskSubmission::getStudentId, allStudentIds));
                Set<Long> existingStudentIds = existingSubs.stream()
                    .map(TaskSubmission::getStudentId)
                    .collect(Collectors.toSet());

                int created = 0;
                for (Student stu : allStudents) {
                    if (existingStudentIds.contains(stu.getId())) continue;
                    TaskSubmission sub = new TaskSubmission();
                    sub.setTaskId(id);
                    sub.setStudentId(stu.getId());
                    sub.setStatus("PENDING");
                    sub.setSchoolId(t.getSchoolId() != null ? t.getSchoolId() : 1L);
                    sub.setIsOfficial(true);
                    sub.setAttemptNumber(1);
                    submissionMapper.insert(sub);
                    created++;
                }
                log.info("强制任务 {} 覆盖 {} 个班级，{} 名学生，创建 {} 条PENDING记录",
                    id, targetClasses.size(), allStudents.size(), created);
            }
        }

        eventPublisher.publishEvent(TaskEvent.published(this, id,
            TaskCategory.valueOf(t.getTaskType()), t.getTeacherId(),
            Map.of("title", t.getTitle(), "targetId", Objects.toString(t.getTargetId(), ""))));
        return t;
    }

    @Override
    @Transactional
    @CacheEvict(value = "task_list", allEntries = true)
    public Task close(Long id) {
        Task t = taskMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "任务不存在");
        if (!List.of("PUBLISHED", "ONGOING").contains(t.getStatus()))
            throw new BusinessException(409, "仅已发布/进行中的任务可关闭");
        t.setStatus("CLOSED");
        taskMapper.updateById(t);

        submissionMapper.update(null,
            new LambdaUpdateWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, id)
                .eq(TaskSubmission::getStatus, "PENDING")
                .set(TaskSubmission::getStatus, "EXEMPTED"));

        eventPublisher.publishEvent(TaskEvent.closed(this, id,
            TaskCategory.valueOf(t.getTaskType()), t.getTeacherId(),
            Map.of("targetId", Objects.toString(t.getTargetId(), ""))));
        return t;
    }

    @Override
    @Transactional
    @CacheEvict(value = "task_list", allEntries = true)
    public Task reopen(Long id) {
        Task t = taskMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "任务不存在");
        if (!"CLOSED".equals(t.getStatus()))
            throw new BusinessException(409, "仅已关闭的任务可重新打开");
        t.setStatus("DRAFT");
        taskMapper.updateById(t);

        submissionMapper.delete(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, id)
                .eq(TaskSubmission::getStatus, "EXEMPTED"));

        return t;
    }

    @Override
    public int publishScheduledTasks() {
        List<Task> due = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "DRAFT")
                        .isNotNull(Task::getScheduledPublishAt)
                        .le(Task::getScheduledPublishAt, LocalDateTime.now()));

        int count = 0;
        for (Task t : due) {
            int rows = taskMapper.update(null,
                    new LambdaUpdateWrapper<Task>()
                            .eq(Task::getId, t.getId())
                            .eq(Task::getStatus, "DRAFT")
                            .set(Task::getStatus, "PUBLISHED"));
            if (rows > 0) {
                count++;
                try {
                    TaskTypeHandler handler = handlerSelector.get(TaskCategory.valueOf(t.getTaskType()));
                    if (handler != null) {
                        handler.onPublish(new TaskTypeHandler.TaskContext(t.getId(), null, t.getTeacherId(),
                                parseConfig(t.getTaskConfig()), null, Map.of("title", t.getTitle())));
                    }
                    if ("CLASS".equals(t.getTargetType()) && t.getTargetId() != null) {
                        notificationService.notifyClassStudents(t.getTargetId(),
                                "task_published", "新任务发布", t.getTitle(), t.getId());
                    }
                } catch (Exception e) {
                    log.warn("[定时发布] 任务 {} 发布后处理异常: {}", t.getId(), e.getMessage());
                }
            }
        }
        return count;
    }

    @Override
    @Transactional
    public Map<String, Object> resendToPending(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        
        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .ne(TaskSubmission::getStatus, "SUBMITTED")
                .ne(TaskSubmission::getStatus, "GRADED"));
        if (subs.isEmpty()) return Map.of("count", 0);

        List<Long> subIds = subs.stream().map(TaskSubmission::getId).toList();
        List<Long> studentIds = subs.stream().map(TaskSubmission::getStudentId).distinct().toList();
        submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
            .in(TaskSubmission::getId, subIds)
            .set(TaskSubmission::getExtraSubmitAllowed, 1));

        if (!studentIds.isEmpty()) {
            List<Student> students = studentMapper.selectBatchIds(studentIds);
            Map<Long, Student> stuMap = students.stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
            for (Long sid : studentIds) {
                Student stu = stuMap.get(sid);
                if (stu != null) {
                    try { notificationService.notify(stu.getUserId(), "TASK_DEADLINE",
                        "提交入口已重新开放", "老师已为你重新开放「" + task.getTitle() + "」的提交入口，请及时完成。", taskId); } catch (Exception ignored) { /* 单个学生通知发送失败不影响批量操作 */ }
                }
            }
        }
        return Map.of("count", subs.size());
    }

    @Override
    public Map<String, Object> forcedPreview(String grade) {
        LambdaQueryWrapper<Classes> w = new LambdaQueryWrapper<Classes>().eq(Classes::getStatus, 1);
        if (grade != null && !grade.isBlank()) {
            w.eq(Classes::getGrade, grade);
        }
        List<Classes> targetClasses = classesMapper.selectList(w);

        int classCount = targetClasses.size();
        int studentCount = 0;
        if (!targetClasses.isEmpty()) {
            List<Long> classIds = targetClasses.stream().map(Classes::getId).toList();
            studentCount = Math.toIntExact(studentMapper.selectCount(
                new LambdaQueryWrapper<Student>()
                    .in(Student::getClassId, classIds)
                    .eq(Student::getStatus, "active")));
        }

        String scope = (grade != null && !grade.isBlank()) ? (grade + "强制任务") : "全校强制任务";
        return Map.of("classCount", classCount, "studentCount", studentCount, "scope", scope);
    }

    @Override
    @Transactional
    public Map<String, Object> handleStudentClassChange(Long studentId, Long oldClassId, Long newClassId) {
        List<Task> oldTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, oldClassId)
            .eq(Task::getStatus, "ONGOING"));
        java.util.Set<Long> oldTaskIds = oldTasks.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        if (!oldTaskIds.isEmpty()) {
            List<TaskSubmission> oldSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                    .in(TaskSubmission::getTaskId, oldTaskIds)
                    .eq(TaskSubmission::getStudentId, studentId)
                    .eq(TaskSubmission::getStatus, "SUBMITTED"));
            submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
                .in(TaskSubmission::getId, oldSubs.stream().map(TaskSubmission::getId).toList())
                .set(TaskSubmission::getStatus, "EXEMPTED"));
        }

        List<Task> newTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, newClassId)
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING")));
        int created = 0;
        List<TaskSubmission> newSubs = new java.util.ArrayList<>();
        for (Task t : newTasks) {
            TaskSubmission sub = new TaskSubmission();
            sub.setTaskId(t.getId()); sub.setStudentId(studentId);
            sub.setSchoolId(t.getSchoolId()); sub.setStageId(t.getStageId());
            sub.setStatus("PENDING");
            sub.setIsOfficial(true);
            sub.setAttemptNumber(1);
            newSubs.add(sub);
        }
        if (!newSubs.isEmpty()) {
            com.baomidou.mybatisplus.extension.toolkit.Db.saveBatch(newSubs);
            created = newSubs.size();
        }

        return Map.of("exempted", oldTaskIds.size(), "newTasks", created);
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) return Map.of();
        return JsonUtils.parseMap(json);
    }
}