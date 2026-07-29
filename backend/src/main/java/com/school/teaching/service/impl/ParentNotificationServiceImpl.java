package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ParentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentNotificationServiceImpl implements ParentNotificationService {

    private final StudentMapper studentMapper;
    private final ParentChildRelationMapper parentChildRelationMapper;
    private final NotificationMapper notificationMapper;
    private final ClassesMapper classesMapper;
    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void notifyParentsForTask(Task task) {
        if (task.getNotifyParents() == null || task.getNotifyParents() != 1) return;

        List<Long> classIds = resolveClassIds(task.getTargetType(), task.getTargetId(),
            task.getIsForced(), task.getTaskConfig());
        if (classIds.isEmpty()) {
            log.warn("任务{}未关联任何班级，跳过家长通知", task.getId());
            return;
        }

        // 查已有通知避免重复
        Set<String> existingKeys = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getType, "TASK_NOTIFY")
                    .eq(Notification::getRelatedId, task.getId()))
            .stream()
            .map(n -> n.getUserId() + ":" + n.getRelatedId())
            .collect(Collectors.toSet());

        int totalSent = 0;
        for (Long classId : classIds) {
            List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
            if (students.isEmpty()) continue;

            List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
            Map<Long, Student> studentMap = students.stream().collect(Collectors.toMap(Student::getId, s -> s));

            List<ParentChildRelation> relations = parentChildRelationMapper.selectList(
                new LambdaQueryWrapper<ParentChildRelation>().in(ParentChildRelation::getStudentId, studentIds));

            Map<Long, String> studentNameMap = resolveStudentNames(students);

            for (ParentChildRelation r : relations) {
                String key = r.getParentId() + ":" + task.getId();
                if (existingKeys.contains(key)) continue;

                Student stu = studentMap.get(r.getStudentId());
                String studentName = stu != null ? studentNameMap.getOrDefault(stu.getId(), "孩子") : "孩子";

                Notification n = new Notification();
                n.setUserId(r.getParentId());
                n.setTitle("新任务通知");
                n.setContent("您的" + (r.getRelation() != null ? r.getRelation() : "孩子")
                    + studentName + "有一个新任务「" + (task.getTitle() != null ? task.getTitle() : "") + "」，请关注。");
                n.setType("TASK_NOTIFY");
                n.setRelatedId(task.getId());
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(n);
                totalSent++;
            }
        }
        if (totalSent > 0) {
            log.info("任务{}家长通知已投递{}条", task.getId(), totalSent);
        }
    }

    @Override
    public void notifyParentsForExam(Exam exam) {
        log.warn("Exam通知暂未实现：id={}", exam != null ? exam.getId() : null);
    }

    @Override
    public void notifyParentsForHomework(HomeworkAssignment hw) {
        log.warn("Homework通知暂未实现：id={}", hw != null ? hw.getId() : null);
    }

    private List<Long> resolveClassIds(String targetType, Long targetId, Integer isForced, String taskConfig) {
        if (isForced != null && isForced == 1) {
            LambdaQueryWrapper<Classes> w = new LambdaQueryWrapper<Classes>().eq(Classes::getStatus, 1);
            String forcedGrade = null;
            try {
                if (taskConfig != null) {
                    Map<?, ?> cfg = com.school.teaching.utils.JsonUtils.MAPPER.readValue(taskConfig, Map.class);
                    forcedGrade = (String) cfg.get("forcedGrade");
                }
            } catch (Exception ignored) { log.warn("强制年级解析失败: {}", ignored.getMessage()); }
            if (forcedGrade != null && !forcedGrade.isBlank()) {
                w.eq(Classes::getGrade, forcedGrade);
            }
            return classesMapper.selectList(w).stream().map(Classes::getId).collect(Collectors.toList());
        }
        if ("CLASS".equals(targetType) && targetId != null) {
            return List.of(targetId);
        }
        if (targetId != null) {
            return List.of(targetId);
        }
        return List.of();
    }

    private Map<Long, String> resolveStudentNames(List<Student> students) {
        List<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) return Map.of();
        Map<Long, Long> studentUserMap = students.stream()
            .collect(Collectors.toMap(Student::getId, Student::getUserId));
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().in(User::getId, userIds));
        Map<Long, String> userNameMap = users.stream()
            .collect(Collectors.toMap(User::getId, User::getRealName));
        Map<Long, String> result = new HashMap<>();
        for (Student s : students) {
            result.put(s.getId(), userNameMap.getOrDefault(s.getUserId(), ""));
        }
        return result;
    }
}
