package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.security.StudentResolver;
import com.school.teaching.service.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskQueryServiceImpl implements TaskQueryService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final TeacherMapper teacherMapper;
    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final StudentResolver studentResolver;
    private final StudentGroupMemberMapper studentGroupMemberMapper;
    private final TaskGroupVisibilityMapper taskGroupVisibilityMapper;

    @Override
    public List<Task> getAccessibleTasks(Long userId) {
        Long schoolId = SecurityUtils.getCurrentSchoolId();
        Long stageId = SecurityUtils.getCurrentStageId();
        String role = SecurityUtils.getCurrentRole();

        if ("STUDENT".equals(role)) {
            Long studentId = studentResolver.resolveCurrentStudentId();
            return studentId != null ? getStudentTasks(studentId) : List.of();
        }
        if ("TEACHER".equals(role) || "HEAD_TEACHER".equals(role)) {
            Long teacherId = resolveTeacherId();
            return getTeacherTasks(teacherId);
        }
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>()
            .eq(Task::getSchoolId, schoolId).eq(Task::getStageId, stageId);
        return taskMapper.selectList(q);
    }

    @Override
    public List<Task> getTeacherTasks(Long teacherId) {
        List<Task> own = taskMapper.selectList(
            new LambdaQueryWrapper<Task>().eq(Task::getTeacherId, teacherId));

        Teacher teacher = teacherMapper.selectById(teacherId);
        Long userId = teacher != null ? teacher.getUserId() : null;

        List<Classes> managed = classesMapper.selectList(
            new LambdaQueryWrapper<Classes>().eq(Classes::getHeadTeacherId, userId));
        if (!managed.isEmpty()) {
            Set<Long> classIds = managed.stream().map(Classes::getId).collect(Collectors.toSet());
            Set<Long> ownIds = own.stream().map(Task::getId).collect(Collectors.toSet());
            List<Task> classTasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                    .eq(Task::getTargetType, "CLASS").in(Task::getTargetId, classIds)
                    .in(Task::getStatus, List.of("PUBLISHED", "ONGOING")));
            for (Task t : classTasks) {
                if (!ownIds.contains(t.getId())) own.add(t);
            }
        }
        return own;
    }

    @Override
    public List<Task> getStudentTasks(Long studentId) {
        Long classId = resolveCurrentClassId(studentId);
        if (classId == null) return List.of();

        Set<Long> doneTaskIds = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId)
                .in(TaskSubmission::getStatus, List.of("SUBMITTED", "GRADED", "EXEMPTED")))
            .stream().map(TaskSubmission::getTaskId).collect(Collectors.toSet());

        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId)
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING")));
        tasks = tasks.stream()
            .filter(t -> !doneTaskIds.contains(t.getId()))
            .collect(Collectors.toList());
        tasks = filterByGroupVisibility(tasks, studentId);

        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId));
        Map<Long, TaskSubmission> subMap = subs.stream()
            .collect(Collectors.toMap(TaskSubmission::getTaskId, s -> s, (a, b) -> b));
        for (Task t : tasks) {
            TaskSubmission sub = subMap.get(t.getId());
            if (sub != null) {
                t.setSubmissionStatus(sub.getStatus());
                t.setScore(sub.getScore());
                t.setScoreJson(sub.getScoreJson());
            }
        }
        return tasks;
    }

    @Override
    public List<Task> getStudentCompletedTasks(Long studentId) {
        Long classId = resolveCurrentClassId(studentId);
        if (classId == null) return List.of();

        List<TaskSubmission> doneSubs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId)
                .in(TaskSubmission::getStatus, List.of("SUBMITTED", "GRADED")));
        Set<Long> doneTaskIds = doneSubs.stream()
            .map(TaskSubmission::getTaskId).collect(Collectors.toSet());

        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId)
            .and(w -> w.in(Task::getStatus, List.of("PUBLISHED", "ONGOING"))
                .or().eq(Task::getStatus, "CLOSED")));
        tasks = tasks.stream()
            .filter(t -> doneTaskIds.contains(t.getId()) || "CLOSED".equals(t.getStatus()))
            .collect(Collectors.toList());
        tasks = filterByGroupVisibility(tasks, studentId);

        List<TaskSubmission> allSubs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId));
        Map<Long, TaskSubmission> subMap = allSubs.stream()
            .collect(Collectors.toMap(TaskSubmission::getTaskId, s -> s, (a, b) -> b));
        for (Task t : tasks) {
            TaskSubmission sub = subMap.get(t.getId());
            if (sub != null) {
                t.setSubmissionStatus(sub.getStatus());
                t.setScore(sub.getScore());
                t.setScoreJson(sub.getScoreJson());
            }
        }
        return tasks;
    }

    @Override
    @Cacheable(value = "task_list", key = "'teacher:' + #teacherId + ':' + #page.current + ':' + #page.size + ':' + (#status ?: 'all')")
    public IPage<Task> pageByTeacher(Long teacherId, Page<Task> page, String status) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        Long userId = teacher != null ? teacher.getUserId() : null;
        final List<Long> headClassIds;
        final Set<Long> adminTeacherIds = new HashSet<>();
        if (userId != null) {
            headClassIds = classesMapper.selectList(
                new LambdaQueryWrapper<Classes>().eq(Classes::getHeadTeacherId, userId))
                .stream().map(Classes::getId).toList();
            if (!headClassIds.isEmpty()) {
                List<User> adminUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
                        .notIn(User::getRoleName, List.of("TEACHER", "HEAD_TEACHER")));
                Set<Long> adminUids = adminUsers.stream().map(User::getId).collect(Collectors.toSet());
                if (!adminUids.isEmpty()) {
                    adminTeacherIds.addAll(teacherMapper.selectList(
                        new LambdaQueryWrapper<Teacher>().in(Teacher::getUserId, adminUids))
                        .stream().map(Teacher::getId).collect(Collectors.toSet()));
                }
            }
        } else {
            headClassIds = List.of();
        }

        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>();
        if (!headClassIds.isEmpty()) {
            q.and(w -> {
                w.eq(Task::getTeacherId, teacherId);
                w.or(w2 -> {
                    w2.eq(Task::getTargetType, "CLASS").in(Task::getTargetId, headClassIds);
                    if (!adminTeacherIds.isEmpty()) w2.notIn(Task::getTeacherId, adminTeacherIds);
                });
            });
        } else {
            q.eq(Task::getTeacherId, teacherId);
        }
        if (status != null && !status.isEmpty()) q.eq(Task::getStatus, status);
        return taskMapper.selectPage(page, q.orderByDesc(Task::getCreatedAt));
    }

    @Override
    @Cacheable(value = "task_list", key = "'student:' + #studentId + ':' + #page.current + ':' + #page.size + ':' + (#status ?: 'all')")
    public IPage<Task> pageByStudent(Long studentId, Page<Task> page, String status) {
        Long classId = resolveCurrentClassId(studentId);
        if (classId == null) return new Page<>();
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId);
        if (status != null && !status.isEmpty()) q.eq(Task::getStatus, status);
        IPage<Task> pg = taskMapper.selectPage(page, q.orderByDesc(Task::getCreatedAt));
        pg.setRecords(filterByGroupVisibility(pg.getRecords(), studentId));
        return pg;
    }

    @Override
    @Cacheable(value = "task_list", key = "'admin:' + #page.current + ':' + #page.size + ':' + (#status ?: 'all')")
    public IPage<Task> pageByAdmin(Page<Task> page, String status) {
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>()
            .orderByDesc(Task::getCreatedAt);
        if (status != null && !status.isEmpty()) q.eq(Task::getStatus, status);
        return taskMapper.selectPage(page, q);
    }

    @Override
    public void enrichTasks(List<Task> tasks, Long currentTeacherId) {
        if (tasks == null || tasks.isEmpty()) return;
        Set<Long> cids = tasks.stream().map(Task::getTargetId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Classes> cmap = cids.isEmpty() ? Map.of() :
            classesMapper.selectBatchIds(cids).stream().collect(Collectors.toMap(Classes::getId, c -> c));
        Set<Long> tids = tasks.stream().map(Task::getTeacherId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> tnameMap = new HashMap<>();
        if (!tids.isEmpty()) {
            List<Teacher> teachers = teacherMapper.selectBatchIds(tids);
            Set<Long> uids = teachers.stream().map(Teacher::getUserId).collect(Collectors.toSet());
            Map<Long, String> unameMap = new HashMap<>();
            if (!uids.isEmpty()) {
                userMapper.selectBatchIds(uids).forEach(u -> unameMap.put(u.getId(),
                    u.getRealName() != null ? u.getRealName() : ""));
            }
            for (Teacher t : teachers) tnameMap.put(t.getId(), unameMap.getOrDefault(t.getUserId(), "未知"));
        }
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskSubmission> allSubs = taskIds.isEmpty() ? List.of() :
            submissionMapper.selectList(new LambdaQueryWrapper<TaskSubmission>()
                .in(TaskSubmission::getTaskId, taskIds)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED", "RETURNED"));
        Map<Long, Long> pendingMap = new HashMap<>();
        Map<Long, Long> submittedMap = new HashMap<>();
        Map<Long, java.math.BigDecimal> scoreSumMap = new HashMap<>();
        Map<Long, Integer> scoreCountMap = new HashMap<>();
        for (TaskSubmission sub : allSubs) {
            if ("SUBMITTED".equals(sub.getStatus())) {
                pendingMap.merge(sub.getTaskId(), 1L, Long::sum);
            }
            submittedMap.merge(sub.getTaskId(), 1L, Long::sum);
            if (sub.getScore() != null && ("GRADED".equals(sub.getStatus()) || "RETURNED".equals(sub.getStatus()))
                && (sub.getIsOfficial() == null || Boolean.TRUE.equals(sub.getIsOfficial()))) {
                scoreSumMap.merge(sub.getTaskId(), sub.getScore(), java.math.BigDecimal::add);
                scoreCountMap.merge(sub.getTaskId(), 1, Integer::sum);
            }
        }
        Map<Long, Long> studentCountMap = new HashMap<>();
        if (!cids.isEmpty()) {
            // Batch query + in-memory GROUP BY (替代 N+1 循环 selectCount)
            List<Student> classStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                    .select(Student::getId, Student::getClassId)
                    .in(Student::getClassId, cids));
            for (Student s : classStudents) {
                studentCountMap.merge(s.getClassId(), 1L, Long::sum);
            }
        }
        for (Task t : tasks) {
            t.setIsOwner(currentTeacherId != null && currentTeacherId.equals(t.getTeacherId()));
            if (t.getTargetId() != null) {
                Classes c = cmap.get(t.getTargetId());
                if (c != null) { t.setClassName(c.getClassName()); t.setGrade(c.getGrade()); }
            }
            if (t.getTeacherId() != null) t.setTeacherName(tnameMap.getOrDefault(t.getTeacherId(), ""));
            t.setPendingGradingCount(pendingMap.getOrDefault(t.getId(), 0L));
            t.setSubmittedCount(submittedMap.getOrDefault(t.getId(), 0L));
            t.setTotalStudents(studentCountMap.getOrDefault(t.getTargetId(), 0L));
            Integer sc = scoreCountMap.get(t.getId());
            if (sc != null && sc > 0) {
                java.math.BigDecimal sum = scoreSumMap.getOrDefault(t.getId(), java.math.BigDecimal.ZERO);
                t.setAvgScore(Math.round(sum.doubleValue() / sc * 10.0) / 10.0);
            }
        }
    }

    @Override
    @Cacheable(value = "pending_count", key = "#studentId")
    public Map<String, Object> getPendingCount(Long studentId) {
        Long classId = resolveCurrentClassId(studentId);
        if (classId == null) return Map.of("count", 0, "urgent", 0, "warning", 0);

        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId)
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING")));
        tasks = filterByGroupVisibility(tasks, studentId);

        Set<Long> doneTaskIds = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId)
                .in(TaskSubmission::getStatus, List.of("SUBMITTED", "GRADED", "EXEMPTED")))
            .stream().map(TaskSubmission::getTaskId).collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate sevenDays = today.plusDays(7);

        long count = 0, urgent = 0, warning = 0;
        for (Task task : tasks) {
            if (doneTaskIds.contains(task.getId())) continue;
            if (task.getDeadline() != null && !task.getDeadline().isAfter(now)) continue;
            count++;
            if (task.getDeadline() != null) {
                LocalDate dl = task.getDeadline().toLocalDate();
                if (!dl.isAfter(tomorrow)) urgent++;
                if (!dl.isAfter(sevenDays)) warning++;
            }
        }
        return Map.of("count", count, "urgent", urgent, "warning", warning);
    }

    @Override
    public Map<String, Object> getStudentTasksWithSubmission(Long studentId, int page, int size) {
        List<Task> tasks = getStudentTasks(studentId);
        enrichTasks(tasks, null);
        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId));
        Map<Long, TaskSubmission> subMap = new HashMap<>();
        for (TaskSubmission s : subs) subMap.put(s.getTaskId(), s);
        // 按 taskId 分组所有提交（用于 needsRetake 判断）
        Map<Long, List<TaskSubmission>> subsByTask = subs.stream()
            .collect(Collectors.groupingBy(TaskSubmission::getTaskId));
        for (Task t : tasks) {
            TaskSubmission sub = subMap.get(t.getId());
            if (sub != null) {
                t.setSubmissionStatus(sub.getStatus());
                t.setScore(sub.getScore());
                t.setScoreJson(sub.getScoreJson());
            }
            // 计算 needsRetake
            t.setNeedsRetake(computeNeedsRetake(t, subsByTask.get(t.getId())));
        }
        int total = tasks.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Task> pageData = from < total ? tasks.subList(from, to) : List.of();
        return Map.of("records", pageData, "total", total, "page", page, "size", size);
    }

    /** 判断学生是否需要对该任务进行重测 */
    private boolean computeNeedsRetake(Task task, List<TaskSubmission> subs) {
        if (task == null || subs == null || subs.isEmpty()) return false;
        if (task.getPassRate() == null || task.getPassRate() <= 0) return false;
        if (task.getMaxAttempts() == null || task.getMaxAttempts() < 2) return false;
        BigDecimal total = task.getTotalScore() != null ? task.getTotalScore() : BigDecimal.valueOf(100);
        if (total.compareTo(BigDecimal.ZERO) <= 0) return false;

        // 按 attemptNumber 排序
        subs.sort(Comparator.comparingInt(s -> s.getAttemptNumber() != null ? s.getAttemptNumber() : 1));
        TaskSubmission latest = subs.get(subs.size() - 1);

        // 已达标的不需要重测
        BigDecimal latestScore = latest.getScore();
        if (latestScore != null) {
            if (latestScore.doubleValue() / total.doubleValue() * 100 >= task.getPassRate()) {
                return false;
            }
        }

        // 是否有任意轮次达标
        for (TaskSubmission s : subs) {
            BigDecimal sc = s.getScore();
            if (sc != null && sc.doubleValue() / total.doubleValue() * 100 >= task.getPassRate()) {
                return false;
            }
        }

        int attemptNum = latest.getAttemptNumber() != null ? latest.getAttemptNumber() : 1;
        // 已达最大次数
        if (attemptNum >= task.getMaxAttempts()) return false;

        // 截止时间检查
        if (task.getRetakeDeadlineHours() != null) {
            TaskSubmission firstSub = subs.stream()
                .filter(s -> s.getAttemptNumber() != null && s.getAttemptNumber() == 1)
                .findFirst().orElse(null);
            if (firstSub != null && firstSub.getSubmittedAt() != null) {
                if (LocalDateTime.now().isAfter(firstSub.getSubmittedAt()
                    .plusHours(task.getRetakeDeadlineHours()))) {
                    return false;
                }
            }
        }

        // 最新提交不是 PENDING（如果是 PENDING 说明已经在做重测/首次考试中）
        if ("PENDING".equals(latest.getStatus())) return false;

        return true;
    }

    @Override
    public boolean isTaskAccessibleByStudent(Long taskId, Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) return false;
        Task task = taskMapper.selectById(taskId);
        if (task == null) return false;
        if (task.getIsForced() != null && task.getIsForced() == 1) {
            return true;
        }
        if ("CLASS".equals(task.getTargetType()) && task.getTargetId() != null) {
            return task.getTargetId().equals(student.getClassId());
        }
        return false;
    }

    @Override
    public IPage<Task> pageTasksForReview(String reviewStatus, String startDate, String endDate, int page, int pageSize) {
        Page<Task> taskPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<>();
        q.isNotNull(Task::getReviewStatus)
         .ne(Task::getReviewStatus, "NOT_SUBMITTED");
        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            q.eq(Task::getReviewStatus, reviewStatus);
        }
        if (startDate != null && !startDate.isEmpty()) {
            q.ge(Task::getUpdatedAt, java.time.LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            q.le(Task::getUpdatedAt, java.time.LocalDateTime.parse(endDate + "T23:59:59"));
        }
        q.orderByDesc(Task::getUpdatedAt);
        return taskMapper.selectPage(taskPage, q);
    }

    @Override
    public List<Task> getTasksByTeacherIds(java.util.Collection<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) return List.of();
        return taskMapper.selectList(
            new LambdaQueryWrapper<Task>().in(Task::getTeacherId, teacherIds));
    }

    @Override
    public List<Task> getActiveClassTasks(Long classId) {
        return taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getTargetId, classId)
                .eq(Task::getTaskType, "IN_CLASS")
                .in(Task::getStatus, "PUBLISHED", "ONGOING")
                .orderByDesc(Task::getCreatedAt)
                .last("LIMIT 20"));
    }

    private Long resolveTeacherId() {
        return resolveTeacherId(SecurityUtils.getCurrentUserId());
    }

    private Long resolveTeacherId(Long userId) {
        if (userId == null) return null;
        Teacher t = teacherMapper.selectOne(
            new LambdaQueryWrapper<Teacher>().eq(Teacher::getUserId, userId));
        if (t != null) return t.getId();
        return SecurityUtils.isAdmin() || SecurityUtils.isInspector() ? 0L : null;
    }

    private Long resolveCurrentClassId(Long studentId) {
        return studentResolver.resolveCurrentClassId(studentId);
    }

    private List<Task> filterByGroupVisibility(List<Task> tasks, Long studentId) {
        if (tasks.isEmpty()) return tasks;

        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskGroupVisibility> allMappings = taskGroupVisibilityMapper.selectList(
                new LambdaQueryWrapper<TaskGroupVisibility>()
                        .in(TaskGroupVisibility::getTaskId, taskIds));
        Set<Long> restrictedTaskIds = allMappings.stream()
                .map(TaskGroupVisibility::getTaskId).collect(Collectors.toSet());
        if (restrictedTaskIds.isEmpty()) return tasks;

        Set<Long> myGroupIds = studentGroupMemberMapper.selectList(
                new LambdaQueryWrapper<StudentGroupMember>()
                        .eq(StudentGroupMember::getStudentId, studentId))
                .stream().map(StudentGroupMember::getGroupId).collect(Collectors.toSet());

        Set<Long> visibleRestrictedIds;
        if (myGroupIds.isEmpty()) {
            visibleRestrictedIds = Set.of();
        } else {
            visibleRestrictedIds = allMappings.stream()
                    .filter(m -> myGroupIds.contains(m.getGroupId()))
                    .map(TaskGroupVisibility::getTaskId)
                    .collect(Collectors.toSet());
        }

        return tasks.stream()
                .filter(t -> !restrictedTaskIds.contains(t.getId()) || visibleRestrictedIds.contains(t.getId()))
                .collect(Collectors.toList());
    }
}