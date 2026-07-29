package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ClassHomeService;
import com.school.teaching.utils.ScoreUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassHomeServiceImpl implements ClassHomeService {

    @Autowired private ClassesMapper classesMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private CreditTransactionMapper creditTransactionMapper;
    @Autowired private com.school.teaching.mapper.TeacherClassMapper teacherClassMapper;
    @Autowired private TitleLevelMapper titleLevelMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private TaskSubmissionMapper taskSubmissionMapper;
    @Autowired private BbsPostMapper bbsPostMapper;
    @Autowired private ShowcaseWorkMapper showcaseWorkMapper;
    @Autowired private NotificationMapper notificationMapper;
    @Autowired private DictGradeMapper dictGradeMapper;
    @Autowired private ClassTypeConfigMapper classTypeConfigMapper;
    @Autowired private DictMajorMapper dictMajorMapper;
    @Autowired private com.school.teaching.mapper.TaskQuestionMapper taskQuestionMapper;
    @Autowired private com.school.teaching.mapper.QuestionBankMapper questionBankMapper;
    @Autowired private com.school.teaching.mapper.StudentAnswerMapper studentAnswerMapper;
    @Autowired private com.school.teaching.mapper.KnowledgeNodeMapper knowledgeNodeMapper;

    @Override
    public boolean isHeadTeacherOfClass(Long classId, Long userId) {
        Classes cls = classesMapper.selectById(classId);
        return cls != null && cls.getHeadTeacherId() != null && cls.getHeadTeacherId().equals(userId);
    }

    @Override
    public boolean isStudentOfClass(Long classId, Long userId) {
        return studentMapper.selectCount(new LambdaQueryWrapper<Student>()
            .eq(Student::getClassId, classId).eq(Student::getUserId, userId)) > 0;
    }

    @Override
    @Cacheable(value = "classHome", key = "#classId", unless = "#result.isEmpty()")
    public Map<String, Object> getClassHomeData(Long classId, Long currentUserId) {
        Classes cls = classesMapper.selectById(classId);
        if (cls == null) return Map.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("basicInfo", buildBasicInfo(cls));
        result.put("students", buildStudentList(classId));
        result.put("tasks", buildTaskList(classId, currentUserId));
        result.put("scoreOverview", buildScoreOverview(classId));
        result.put("activities", buildActivities(classId));
        result.put("honorWall", buildHonorWall(classId));
        return result;
    }

    // ── 班级基础信息 ──────────────────────────
    private Map<String, Object> buildBasicInfo(Classes cls) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("classId", cls.getId());
        info.put("className", cls.getClassName());
        info.put("grade", cls.getGrade());
        info.put("classType", cls.getClassType());
        info.put("major", cls.getMajor());
        info.put("headTeacherId", cls.getHeadTeacherId());

        // 年级名称
        if (cls.getGrade() != null) {
            DictGrade dg = dictGradeMapper.selectOne(new LambdaQueryWrapper<DictGrade>()
                .eq(DictGrade::getGradeName, cls.getGrade()));
            if (dg != null) info.put("gradeName", dg.getGradeName());
        }

        // 班级类型名称
        if (cls.getClassType() != null && cls.getStageId() != null) {
            ClassTypeConfig ctc = classTypeConfigMapper.selectOne(new LambdaQueryWrapper<ClassTypeConfig>()
                .eq(ClassTypeConfig::getTypeCode, cls.getClassType())
                .eq(ClassTypeConfig::getStageId, cls.getStageId()));
            if (ctc != null) info.put("classTypeName", ctc.getTypeName());
        }

        // 专业名称（仅职高）
        if (cls.getMajor() != null) {
            DictMajor dm = dictMajorMapper.selectOne(new LambdaQueryWrapper<DictMajor>()
                .eq(DictMajor::getMajorName, cls.getMajor()));
            if (dm != null) info.put("majorName", dm.getMajorName());
        }

        // 班主任姓名
        if (cls.getHeadTeacherId() != null) {
            Teacher ht = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, cls.getHeadTeacherId()));
            if (ht != null) {
                User hu = userMapper.selectById(cls.getHeadTeacherId());
                if (hu != null) info.put("headTeacherName", hu.getRealName());
            }
        }

        // 学生总人数
        long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
            .eq(Student::getClassId, cls.getId())
            .eq(Student::getStatus, "active"));
        info.put("studentCount", studentCount);

        return info;
    }

    // ── 学生列表（积分降序）───────────────────
    private List<Map<String, Object>> buildStudentList(Long classId) {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
            .eq(Student::getClassId, classId).eq(Student::getStatus, "active"));
        if (students.isEmpty()) return List.of();

        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, String> userNames = userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));

        // 积分聚合 — 一次 GROUP BY
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<CreditTransaction> txns = creditTransactionMapper.selectList(
            new LambdaQueryWrapper<CreditTransaction>().in(CreditTransaction::getStudentId, studentIds));
        Map<Long, Long> credits = new HashMap<>();
        for (CreditTransaction t : txns) {
            if ("earn".equals(t.getTransactionType()) && t.getStudentId() != null) {
                credits.merge(t.getStudentId(), (long)(t.getCreditAmount() != null ? t.getCreditAmount() : 0), Long::sum);
            }
        }

        // 称号 — 查全部 title_levels，按积分匹配
        List<TitleLevel> titles = titleLevelMapper.selectList(
            new LambdaQueryWrapper<TitleLevel>().orderByDesc(TitleLevel::getMinCredits));
        Map<Long, String> studentTitles = new HashMap<>();
        for (Student s : students) {
            long total = credits.getOrDefault(s.getId(), 0L);
            for (TitleLevel tl : titles) {
                if (total >= tl.getMinCredits() && (tl.getMaxCredits() == null || total <= tl.getMaxCredits())) {
                    studentTitles.put(s.getId(), tl.getLevelName());
                    break;
                }
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", s.getId());
            item.put("userId", s.getUserId());
            item.put("name", userNames.getOrDefault(s.getUserId(), "未知"));
            item.put("studentNumber", s.getStudentNumber());
            item.put("currentType", s.getCurrentType());
            item.put("credits", credits.getOrDefault(s.getId(), 0L));
            item.put("title", studentTitles.getOrDefault(s.getId(), ""));
            list.add(item);
        }
        list.sort((a, b) -> Long.compare((long)b.get("credits"), (long)a.get("credits")));
        return list;
    }

    // ── 任务动态（最近10条）───────────────────
    private List<Map<String, Object>> buildTaskList(Long classId, Long currentUserId) {
        // 非班主任教师 → 只看到自己布置的任务
        Classes cls = classesMapper.selectById(classId);
        boolean isHead = cls != null && cls.getHeadTeacherId() != null && cls.getHeadTeacherId().equals(currentUserId);
        Long filterTeacherId = null;
        if (!isHead) {
            Teacher t = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, currentUserId));
            filterTeacherId = t != null ? t.getId() : null;
        }

        List<Task> allTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId)
            .orderByDesc(Task::getCreatedAt));
        if (allTasks.isEmpty()) return List.of();

        // 非班主任 → 过滤出自己布置的任务
        List<Task> tasks = allTasks;
        if (filterTeacherId != null) {
            final Long ftid = filterTeacherId;
            tasks = allTasks.stream().filter(t -> ftid.equals(t.getTeacherId())).toList();
        }
        if (tasks.isEmpty()) return List.of();

        // 提交统计 — 一次 GROUP BY
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskSubmission> subs = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds));
        Map<Long, List<TaskSubmission>> subByTask = subs.stream()
            .collect(Collectors.groupingBy(TaskSubmission::getTaskId));

        long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
            .eq(Student::getClassId, classId).eq(Student::getStatus, "active"));

        // 教师姓名映射
        Set<Long> tids = tasks.stream().map(Task::getTeacherId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> teacherNames = new HashMap<>();
        if (!tids.isEmpty()) {
            userMapper.selectBatchIds(tids).forEach(u -> teacherNames.put(u.getId(), u.getRealName()));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Task t : tasks) {
            List<TaskSubmission> ts = subByTask.getOrDefault(t.getId(), List.of());
            long submitted = ts.size();
            long graded = ts.stream().filter(s -> "GRADED".equals(s.getStatus())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", t.getId());
            item.put("title", t.getTitle());
            item.put("subject", t.getSubject());
            item.put("taskType", t.getTaskType());
            item.put("deadline", t.getDeadline());
            item.put("teacherName", teacherNames.getOrDefault(t.getTeacherId(), "未知"));
            item.put("totalStudents", studentCount);
            item.put("submitted", submitted);
            item.put("graded", graded);
            item.put("submitRate", studentCount > 0 ? Math.round(submitted * 1000.0 / studentCount) / 10.0 : 0);
            item.put("gradeRate", submitted > 0 ? Math.round(graded * 1000.0 / submitted) / 10.0 : 0);
            list.add(item);
        }
        return list.stream().limit(10).toList();
    }

    // ── 成绩概览（最近一次考试） ──────────────
    private Map<String, Object> buildScoreOverview(Long classId) {
        // 找最近一个 SUMMATIVE/FORMATIVE 任务
        List<Task> examTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId)
            .in(Task::getTaskType, List.of("SUMMATIVE", "FORMATIVE"))
            .orderByDesc(Task::getCreatedAt));
        if (examTasks.isEmpty()) return Map.of("hasData", false);

        Task exam = examTasks.get(0);
        double totalScore = exam.getTotalScore() != null ? exam.getTotalScore().doubleValue() : 100.0;
        List<TaskSubmission> subs = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, exam.getId()));

        List<TaskSubmission> graded = subs.stream()
            .filter(s -> s.getScore() != null && "GRADED".equals(s.getStatus())).toList();
        if (graded.isEmpty()) return Map.of("hasData", false, "taskTitle", exam.getTitle());

        // 1. 只取首次（official）算均分、最大最小
        List<TaskSubmission> official = graded.stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsOfficial()))
            .toList();

        double avg = official.stream().mapToDouble(s -> s.getScore().doubleValue() / totalScore * 100)
            .average().orElse(0);
        double max = official.stream().mapToDouble(s -> s.getScore().doubleValue() / totalScore * 100)
            .max().orElse(0);
        double min = official.stream().mapToDouble(s -> s.getScore().doubleValue() / totalScore * 100)
            .min().orElse(0);

        // 2. 全量统计达标率（任意一轮达标即通过）
        int effectivePassRate = exam.getPassRate() != null && exam.getPassRate() > 0 ? exam.getPassRate() : 60;
        Map<Long, Double> bestByStudent = new HashMap<>();
        for (TaskSubmission s : graded) {
            if (s.getScore() == null) continue;
            double rate = s.getScore().doubleValue() / totalScore * 100;
            bestByStudent.merge(s.getStudentId(), rate, Math::max);
        }
        long passCount = bestByStudent.values().stream().filter(r -> r >= effectivePassRate).count();
        double passRate = Math.round(passCount * 1000.0 / Math.max(bestByStudent.size(), 1)) / 10.0;

        int[] dist = new int[5];
        for (TaskSubmission s : official) {
            double sc = s.getScore().doubleValue() / totalScore * 100;
            if (sc < 60) dist[0]++;
            else if (sc < 70) dist[1]++;
            else if (sc < 80) dist[2]++;
            else if (sc < 90) dist[3]++;
            else dist[4]++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasData", true);
        result.put("taskTitle", exam.getTitle());
        result.put("taskId", exam.getId());
        result.put("avgScore", Math.round(avg * 10) / 10.0);
        result.put("maxScore", max);
        result.put("minScore", min);
        result.put("passRate", passRate);
        result.put("passCount", passCount);
        result.put("totalGraded", graded.size());
        result.put("scoreDistribution", List.of(
            Map.of("range", "0-59%", "count", dist[0]),
            Map.of("range", "60-69%", "count", dist[1]),
            Map.of("range", "70-79%", "count", dist[2]),
            Map.of("range", "80-89%", "count", dist[3]),
            Map.of("range", "90-100%", "count", dist[4])
        ));
        return result;
    }

    // ── 活动记录（BBS + 展示墙）───────────────
    private Map<String, Object> buildActivities(Long classId) {
        // 本班学生的 userId 集合
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
            .eq(Student::getClassId, classId).eq(Student::getStatus, "active"));
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) return Map.of("bbsPosts", List.of(), "showcaseWorks", List.of());

        // BBS 帖子
        List<BbsPost> posts = bbsPostMapper.selectList(new LambdaQueryWrapper<BbsPost>()
            .in(BbsPost::getAuthorId, userIds).orderByDesc(BbsPost::getCreateTime));
        List<Map<String, Object>> bbsPosts = posts.stream().limit(5).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId()); m.put("title", p.getTitle());
            m.put("authorId", p.getAuthorId()); m.put("createTime", p.getCreateTime());
            return m;
        }).toList();

        // 展示墙
        List<ShowcaseWork> works = showcaseWorkMapper.selectList(new LambdaQueryWrapper<ShowcaseWork>()
            .in(ShowcaseWork::getStudentId, userIds).eq(ShowcaseWork::getStatus, 1)
            .orderByDesc(ShowcaseWork::getCreateTime));
        List<Map<String, Object>> showcaseWorks = works.stream().limit(5).map(w -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId()); m.put("title", w.getTitle());
            m.put("authorId", w.getStudentId()); m.put("createTime", w.getCreateTime());
            return m;
        }).toList();

        Map<Long, String> authorNames = new HashMap<>();
        if (!userIds.isEmpty()) userMapper.selectBatchIds(userIds).forEach(u -> authorNames.put(u.getId(), u.getRealName()));
        bbsPosts.forEach(p -> p.put("authorName", authorNames.getOrDefault(p.get("authorId"), "")));
        showcaseWorks.forEach(w -> w.put("authorName", authorNames.getOrDefault(w.get("authorId"), "")));

        return Map.of("bbsPosts", bbsPosts, "showcaseWorks", showcaseWorks);
    }

    // ── 荣誉墙 ──────────────────────────────
    private List<Map<String, Object>> buildHonorWall(Long classId) {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
            .eq(Student::getClassId, classId));
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        Map<Long, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) userMapper.selectBatchIds(userIds).forEach(u -> userNames.put(u.getId(), u.getRealName()));

        List<Map<String, Object>> honors = new ArrayList<>();

        // 德育表扬通知
        List<Notification> praiseNotifications = notificationMapper.selectList(
            new LambdaQueryWrapper<Notification>()
                .in(Notification::getUserId, userIds)
                .eq(Notification::getType, "MORAL_PRAISE")
                .orderByDesc(Notification::getCreateTime));
        for (Notification n : praiseNotifications) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("type", "德育表扬");
            h.put("studentName", userNames.getOrDefault(n.getUserId(), "未知"));
            h.put("reason", n.getContent());
            h.put("time", n.getCreateTime());
            honors.add(h);
        }

        // 称号升级（最近30天 credit_transactions 中 level_up 类型）
        List<CreditTransaction> levelUps = creditTransactionMapper.selectList(
            new LambdaQueryWrapper<CreditTransaction>()
                .in(CreditTransaction::getStudentId, students.stream().map(Student::getId).collect(Collectors.toSet()))
                .eq(CreditTransaction::getTransactionType, "level_up")
                .ge(CreditTransaction::getCreateTime, LocalDate.now().minusDays(30))
                .orderByDesc(CreditTransaction::getCreateTime));
        for (CreditTransaction ct : levelUps) {
            Student s = students.stream().filter(st -> st.getId().equals(ct.getStudentId())).findFirst().orElse(null);
            if (s == null) continue;
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("type", "称号升级");
            h.put("studentName", s.getUserId() != null ? userNames.getOrDefault(s.getUserId(), "未知") : "未知");
            h.put("reason", ct.getDescription() != null ? ct.getDescription() : "获得新称号");
            h.put("time", ct.getCreateTime());
            honors.add(h);
        }

        honors.sort((a, b) -> {
            Comparable ta = (Comparable) a.get("time");
            Comparable tb = (Comparable) b.get("time");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return honors.stream().limit(10).toList();
    }

    // ── 学生成绩趋势 ──────────────────────────
    @Override
    public List<Map<String, Object>> getStudentScoreTrend(Long classId, Long studentId, String subject) {
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId)
            .in(Task::getTaskType, List.of("SUMMATIVE", "FORMATIVE"))
            .eq(subject != null && !subject.isEmpty(), Task::getSubject, subject)
            .orderByAsc(Task::getCreatedAt));
        if (tasks.isEmpty()) return List.of();

        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskSubmission> allSubs = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds));
        Map<Long, List<TaskSubmission>> subByTask = allSubs.stream()
            .collect(Collectors.groupingBy(TaskSubmission::getTaskId));

        // task_submissions.student_id = students.id，直接用 studentId 比对
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task t : tasks) {
            List<TaskSubmission> subs = subByTask.getOrDefault(t.getId(), List.of());
            List<TaskSubmission> graded = subs.stream()
                .filter(s -> s.getScore() != null && "GRADED".equals(s.getStatus())).toList();
            if (graded.isEmpty()) continue;

            double totalScore = t.getTotalScore() != null ? t.getTotalScore().doubleValue() : 100.0;
            double classAvgScore = ScoreUtils.avg(graded.stream().map(TaskSubmission::getScore).toList());
            double classAvgRate = Math.round(classAvgScore / totalScore * 1000) / 10.0;
            Double studentScore = null;
            Double studentRate = null;
            Integer rank = null;
            if (studentId != null) {
                var opt = graded.stream().filter(s -> studentId.equals(s.getStudentId())).findFirst();
                if (opt.isPresent()) {
                    studentScore = opt.get().getScore().doubleValue();
                    studentRate = Math.round(studentScore / totalScore * 1000) / 10.0;
                    // 排名（同分并列，从1开始）
                    var sorted = graded.stream().sorted((a, b) -> b.getScore().compareTo(a.getScore())).toList();
                    double prev = -1; int prevRank = 0;
                    for (int i = 0; i < sorted.size(); i++) {
                        double sc = sorted.get(i).getScore().doubleValue();
                        int curRank = (sc == prev) ? prevRank : i + 1;
                        if (sorted.get(i).getStudentId().equals(studentId)) { rank = curRank; break; }
                        prev = sc; prevRank = curRank;
                    }
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", t.getId());
            item.put("taskTitle", t.getTitle());
            item.put("examDate", t.getCreatedAt());
            item.put("subject", t.getSubject()); // 用于前端学科筛选
            item.put("totalScore", totalScore);
            item.put("studentScore", studentScore != null ? Math.round(studentScore * 100) / 100.0 : null);
            item.put("studentRate", studentRate);
            item.put("classAvgScore", Math.round(classAvgScore * 10) / 10.0);
            item.put("classAvgRate", classAvgRate);
            item.put("totalGraded", graded.size());
            item.put("rank", rank);
            result.add(item);
        }
        return result;
    }

    // ── 考试全班分析 ──────────────────────────
    @Override
    public Map<String, Object> getExamAnalysis(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return Map.of("hasData", false);

        List<TaskSubmission> subs = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
        List<TaskSubmission> graded = subs.stream()
            .filter(s -> s.getScore() != null && "GRADED".equals(s.getStatus())).toList();

        // 取每生最高分（与 compareMultiTasks 保持一致）
        Map<Long, TaskSubmission> bestByStudent = new LinkedHashMap<>();
        for (TaskSubmission s : graded) {
            TaskSubmission existing = bestByStudent.get(s.getStudentId());
            if (existing == null || s.getScore().compareTo(existing.getScore()) > 0) {
                bestByStudent.put(s.getStudentId(), s);
            }
        }
        List<TaskSubmission> bestGraded = new ArrayList<>(bestByStudent.values());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("taskTitle", task.getTitle());
        result.put("totalScore", task.getTotalScore());
        result.put("participantCount", subs.size());
        result.put("gradedCount", bestGraded.size());
        result.put("hasData", !bestGraded.isEmpty());

        if (bestGraded.isEmpty()) return result;

        double totalScore = task.getTotalScore() != null ? task.getTotalScore().doubleValue() : 100.0;
        // 得分率统计（基于每生最高分，与 compareMultiTasks 一致）
        double avgRate = ScoreUtils.avg(bestGraded.stream().mapToDouble(s -> s.getScore().doubleValue() / totalScore * 100));
        double maxRate = bestGraded.stream().mapToDouble(s -> s.getScore().doubleValue() / totalScore * 100).max().orElse(0);
        double minRate = bestGraded.stream().mapToDouble(s -> s.getScore().doubleValue() / totalScore * 100).min().orElse(0);
        long passCount = bestGraded.stream().filter(s -> s.getScore().doubleValue() / totalScore >= 0.6).count();

        result.put("totalScore", totalScore);
        result.put("avgRate", Math.round(avgRate * 10) / 10.0);
        result.put("maxRate", Math.round(maxRate * 10) / 10.0);
        result.put("minRate", Math.round(minRate * 10) / 10.0);
        result.put("passRate", Math.round(passCount * 1000.0 / bestGraded.size()) / 10.0);

        // 得分率分布
        int[] dist = new int[5];
        for (TaskSubmission s : bestGraded) {
            double rate = s.getScore().doubleValue() / totalScore * 100;
            if (rate < 60) dist[0]++; else if (rate < 70) dist[1]++; else if (rate < 80) dist[2]++; else if (rate < 90) dist[3]++; else dist[4]++;
        }
        result.put("distribution", List.of(
            Map.of("label", "0-59%", "count", dist[0]),
            Map.of("label", "60-69%", "count", dist[1]),
            Map.of("label", "70-79%", "count", dist[2]),
            Map.of("label", "80-89%", "count", dist[3]),
            Map.of("label", "90-100%", "count", dist[4])
        ));

        // 逐题正确率
        List<TaskQuestion> tqs = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        Set<Long> qIds = tqs.stream().map(TaskQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = qIds.isEmpty() ? Map.of() : questionBankMapper.selectBatchIds(qIds).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q));

        // 学生答案：按 submissionId 查询，兜底按 taskId 查询
        Set<Long> submissionIds = subs.stream().map(TaskSubmission::getId).collect(Collectors.toSet());
        Map<Long, List<StudentAnswer>> answersByQ = new HashMap<>();
        if (!submissionIds.isEmpty()) {
            List<StudentAnswer> answers = studentAnswerMapper.selectList(
                new LambdaQueryWrapper<StudentAnswer>().in(StudentAnswer::getSubmissionId, submissionIds));
            // 兜底：部分数据可能只通过 taskId 关联
            if (answers.isEmpty()) {
                answers = studentAnswerMapper.selectList(
                    new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getTaskId, taskId));
            }
            answersByQ = answers.stream().collect(Collectors.groupingBy(StudentAnswer::getQuestionId));
        }

        // Build submissionId → submission map for studentId + attemptNumber
        Map<Long, TaskSubmission> subMap = subs.stream()
            .collect(Collectors.toMap(TaskSubmission::getId, s -> s, (a, b) -> a));

        List<Map<String, Object>> qAccuracy = new ArrayList<>();
        int maxWrong = 0;
        for (TaskQuestion tq : tqs) {
            QuestionBank q = qMap.get(tq.getQuestionId());
            List<StudentAnswer> ans = answersByQ.getOrDefault(tq.getQuestionId(), List.of());

            // 按 (studentId) 分组，取 attemptNumber 最大的答题结果
            Map<Long, StudentAnswer> lastByStudent = new HashMap<>();
            Map<Long, Integer> lastAttempt = new HashMap<>();
            for (StudentAnswer a : ans) {
                TaskSubmission s = subMap.get(a.getSubmissionId());
                if (s == null) continue;
                Long studentId = s.getStudentId();
                int attempt = s.getAttemptNumber() != null ? s.getAttemptNumber() : 1;
                Integer prev = lastAttempt.get(studentId);
                if (prev == null || attempt > prev) {
                    lastByStudent.put(studentId, a);
                    lastAttempt.put(studentId, attempt);
                }
            }
            long correct = lastByStudent.values().stream()
                .filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1)
                .count();
            long total = lastByStudent.size();
            double acc = total > 0 ? Math.round(correct * 1000.0 / total) / 10.0 : 0;
            long wrong = total - correct;
            if (wrong > maxWrong) maxWrong = (int) wrong;
            Map<String, Object> qi = new LinkedHashMap<>();
            qi.put("questionId", tq.getQuestionId());
            qi.put("questionText", q != null ? q.getQuestionText() : "题目" + tq.getQuestionId());
            qi.put("questionType", tq.getQuestionType());
            qi.put("score", tq.getScore());
            qi.put("accuracy", acc);
            qi.put("correctCount", correct);
            qi.put("wrongCount", wrong);
            // 客观题传递选项和正确答案，供前端成绩分析弹窗展开查看
            if (q != null && q.getOptions() != null) {
                qi.put("options", q.getOptions());
                qi.put("correctAnswer", q.getCorrectAnswer());
            }
            qAccuracy.add(qi);
        }
        // 标记错最多的题
        final int mw = maxWrong;
        qAccuracy.forEach(qi -> qi.put("isWorst", mw > 0 && ((Number)qi.get("wrongCount")).intValue() == mw));

        // 逐题学生答题明细（供前端点击展开查看）— 批量加载防N+1
        Set<Long> allStudentIds = subs.stream().map(TaskSubmission::getStudentId).collect(Collectors.toSet());
        Map<Long, String> studentNameMap = new LinkedHashMap<>();
        if (!allStudentIds.isEmpty()) {
            List<Student> stus = studentMapper.selectBatchIds(allStudentIds);
            Set<Long> userIds = stus.stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, String> userNames = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : "未知"));
            for (Student s : stus) {
                studentNameMap.put(s.getId(), userNames.getOrDefault(s.getUserId(), "未知"));
            }
        }
        for (Map<String, Object> qi : qAccuracy) {
            Long qId = (Long) qi.get("questionId");
            List<StudentAnswer> ans = answersByQ.getOrDefault(qId, List.of());
            List<Map<String, Object>> details = new ArrayList<>();
            for (StudentAnswer a : ans) {
                // 按 submissionId 查找提交记录（submissionId 不存在则跳过）
                if (a.getSubmissionId() == null) continue;
                TaskSubmission sub = subs.stream()
                    .filter(s -> s.getId().equals(a.getSubmissionId())).findFirst().orElse(null);
                if (sub == null) continue;
                details.add(Map.of(
                    "studentName", studentNameMap.getOrDefault(sub.getStudentId(), "未知"),
                    "studentAnswer", a.getStudentAnswer() != null ? a.getStudentAnswer() : "",
                    "isCorrect", a.getIsCorrect() != null && a.getIsCorrect() == 1,
                    "score", a.getAutoScore() != null ? a.getAutoScore() : java.math.BigDecimal.ZERO
                ));
            }
            qi.put("studentAnswers", details);
        }
        // 按知识点聚合薄弱分析（供AI衍生训练使用）
        // 优先用 categoryId 分组，兜底用 questionId 当虚拟节点
        Map<String, List<Double>> nodeAccMap = new LinkedHashMap<>(); // key: "nodeId_name" 或 "qid_text"
        Map<String, Integer> nodeQCount = new LinkedHashMap<>();
        Map<String, Object> nodeMeta = new LinkedHashMap<>(); // key→{nodeId, name}
        for (Map<String, Object> qi : qAccuracy) {
            Long qId = (Long) qi.get("questionId");
            QuestionBank qb = qMap.get(qId);
            Long nodeId = qb != null ? qb.getCategoryId() : null;
            String key;
            if (nodeId != null) {
                key = "n_" + nodeId;
                nodeMeta.putIfAbsent(key, Map.of("nodeId", nodeId, "name", ""));
            } else {
                key = "q_" + qId;
                String text = qb != null ? qb.getQuestionText() : ("题目" + qId);
                nodeMeta.putIfAbsent(key, Map.of("nodeId", -(qId != null ? qId : 0L), "name",
                    text != null && text.length() > 20 ? text.substring(0, 20) + "…" : (text != null ? text : "未知")));
            }
            double acc = ((Number) qi.get("accuracy")).doubleValue();
            nodeAccMap.computeIfAbsent(key, k -> new ArrayList<>()).add(acc);
            nodeQCount.merge(key, 1, Integer::sum);
        }
        List<Map<String, Object>> weakNodeSummary = new ArrayList<>();
        if (!nodeAccMap.isEmpty()) {
            // 查询知识点名称（按节点ID批量查）
            @SuppressWarnings("unchecked")
            Set<Long> knownNodeIds = nodeMeta.values().stream()
                .map(m -> (Map<String, Object>) m)
                .filter(m -> m.get("nodeId") instanceof Long && (Long) m.get("nodeId") > 0)
                .map(m -> (Long) m.get("nodeId"))
                .collect(Collectors.toSet());
            Map<Long, String> nodeNameMap = knownNodeIds.isEmpty() ? Map.of()
                : knowledgeNodeMapper.selectBatchIds(knownNodeIds).stream()
                    .filter(n -> n != null)
                    .collect(Collectors.toMap(com.school.teaching.entity.KnowledgeNode::getId,
                        n -> n.getName() != null ? n.getName() : "未知", (a, b) -> a));
            for (Map.Entry<String, List<Double>> e : nodeAccMap.entrySet()) {
                String key = e.getKey();
                double avgAcc = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(100);
                if (avgAcc >= 60) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = (Map<String, Object>) nodeMeta.get(key);
                Long nid = meta != null && meta.get("nodeId") instanceof Number n ? n.longValue() : 0L;
                String rawName = meta != null && meta.get("name") instanceof String s ? s : "未知";
                String displayName = nid > 0 ? nodeNameMap.getOrDefault(nid, rawName) : rawName;
                Map<String, Object> wm = new LinkedHashMap<>();
                wm.put("nodeId", nid);
                wm.put("name", displayName);
                wm.put("accuracy", Math.round(avgAcc * 10.0) / 10.0);
                wm.put("questionCount", nodeQCount.getOrDefault(key, 0));
                wm.put("severity", avgAcc < 40 ? "SEVERE" : "WEAK");
                weakNodeSummary.add(wm);
            }
            weakNodeSummary.sort((a, b) ->
                Double.compare(((Number) a.get("accuracy")).doubleValue(), ((Number) b.get("accuracy")).doubleValue()));
        }
        result.put("weakNodeSummary", weakNodeSummary);
        result.put("questionAccuracy", qAccuracy);

        // 达标概况（设计文档 §4.4）
        double totalScoreForRetake = task.getTotalScore() != null ? task.getTotalScore().doubleValue() : 100.0;
        int effectivePassRate = task.getPassRate() != null && task.getPassRate() > 0 ? task.getPassRate() : 60;

        // 取每生最高分判断是否达标
        Map<Long, Boolean> studentPassed = new HashMap<>();
        for (TaskSubmission s : graded) {
            if (s.getScore() == null) continue;
            boolean passed = s.getScore().doubleValue() / totalScoreForRetake * 100 >= effectivePassRate;
            studentPassed.merge(s.getStudentId(), passed, (a, b) -> a || b);
        }
        long firstPassed = graded.stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsOfficial())
                && s.getScore() != null
                && s.getScore().doubleValue() / totalScoreForRetake * 100 >= effectivePassRate)
            .count();
        long totalPassed = studentPassed.values().stream().filter(b -> b).count();

        result.put("retakeOverview", Map.of(
            "totalStudents", studentPassed.size(),
            "firstPassed", firstPassed,
            "retakePassed", Math.max(0, totalPassed - firstPassed),
            "stillFailing", Math.max(0, studentPassed.size() - totalPassed)
        ));

        return result;
    }

    // ── 聚合任务详情 ──────────────────────────
    @Override
    public Map<String, Object> getTaskDetail(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) return Map.of("hasData", false);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.getId());
        result.put("title", task.getTitle());
        result.put("taskType", task.getTaskType());
        result.put("subject", task.getSubject());
        result.put("totalScore", task.getTotalScore());
        result.put("deadline", task.getDeadline());
        result.put("status", task.getStatus());
        result.put("description", task.getDescription());

        // 教师姓名
        if (task.getTeacherId() != null) {
            Teacher t = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>().eq(Teacher::getId, task.getTeacherId()));
            if (t != null) {
                User u = userMapper.selectById(t.getUserId());
                result.put("teacherName", u != null ? u.getRealName() : "未知");
            }
        }

        // 题目数
        Long questionCount = taskQuestionMapper.selectCount(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        result.put("questionCount", questionCount);

        // 提交与批改进度
        List<TaskSubmission> subs = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
        long submitted = subs.size();
        long graded = subs.stream().filter(s -> "GRADED".equals(s.getStatus())).count();
        result.put("submittedCount", submitted);
        result.put("gradedCount", graded);
        result.put("pendingCount", submitted - graded);

        // 成绩概览（复用 getExamAnalysis 已有逻辑）
        Map<String, Object> analysis = getExamAnalysis(taskId);
        // 合并 analysis 中的字段到 result
        if (analysis.containsKey("avgRate")) result.put("avgRate", analysis.get("avgRate"));
        if (analysis.containsKey("maxRate")) result.put("maxRate", analysis.get("maxRate"));
        if (analysis.containsKey("minRate")) result.put("minRate", analysis.get("minRate"));
        if (analysis.containsKey("passRate")) result.put("passRate", analysis.get("passRate"));
        if (analysis.containsKey("distribution")) result.put("distribution", analysis.get("distribution"));
        if (analysis.containsKey("questionAccuracy")) result.put("questionAccuracy", analysis.get("questionAccuracy"));
        if (analysis.containsKey("participantCount")) result.put("participantCount", analysis.get("participantCount"));
        if (analysis.containsKey("gradedCount")) result.put("analysisGradedCount", analysis.get("gradedCount"));
        if (analysis.containsKey("retakeOverview")) result.put("retakeOverview", analysis.get("retakeOverview"));

        result.put("hasData", true);
        return result;
    }
}
