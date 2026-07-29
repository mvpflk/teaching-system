package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.InspectorService;
import com.school.teaching.utils.ScoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InspectorServiceImpl implements InspectorService {

    private static final Logger log = LoggerFactory.getLogger(InspectorServiceImpl.class);

    @Autowired private UserMapper userMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private TaskSubmissionMapper taskSubmissionMapper;
    @Autowired private TaskQuestionMapper taskQuestionMapper;
    @Autowired private StudentAnswerMapper studentAnswerMapper;
    @Autowired private CreditTransactionMapper creditTransactionMapper;
    @Autowired private SignRecordMapper signRecordMapper;
    @Autowired private BbsPostMapper bbsPostMapper;
    @Autowired private PeerReviewMapper peerReviewMapper;
    @Autowired private TeachingGroupMapper teachingGroupMapper;
    @Autowired private LessonPrepGroupMapper lessonPrepGroupMapper;
    @Autowired private GroupMemberMapper groupMemberMapper;
    @Autowired private InspectionIssueMapper inspectionIssueMapper;
    @Autowired private ClassroomPatrolMapper classroomPatrolMapper;
    @Autowired private MoralInspectionMapper moralInspectionMapper;
    @Autowired private TeachingResearchActivityMapper teachingResearchActivityMapper;
    @Autowired private ParentFeedbackSummaryMapper parentFeedbackSummaryMapper;
    @Autowired private PracticeSubmissionMapper practiceSubmissionMapper;

    // ── Dashboard ──────────────────────────────────────────

    @Override
    public Map<String, Object> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("studentCount", studentMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("teacherCount", teacherMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("classCount", classesMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("bbsPostCount", bbsPostMapper.selectCount(new LambdaQueryWrapper<>()));

        // 统一任务统计
        long totalTasks = taskMapper.selectCount(new LambdaQueryWrapper<>());
        long publishedTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, "PUBLISHED"));
        long ongoingTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, "ONGOING"));
        long closedTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, "CLOSED"));
        data.put("totalTasks", totalTasks);
        data.put("publishedTasks", publishedTasks);
        data.put("ongoingTasks", ongoingTasks);
        data.put("closedTasks", closedTasks);

        long totalSubmissions = taskSubmissionMapper.selectCount(new LambdaQueryWrapper<>());
        long gradedSubmissions = taskSubmissionMapper.selectCount(new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getStatus, "GRADED"));
        long submittedCount = taskSubmissionMapper.selectCount(new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getStatus, "SUBMITTED"));
        data.put("totalSubmissions", totalSubmissions);
        data.put("gradedSubmissions", gradedSubmissions);
        data.put("pendingSubmissions", Math.max(0, totalSubmissions - gradedSubmissions));

        // 审核中的任务
        long pendingReview = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
            .in(Task::getReviewStatus, List.of("PENDING_GROUP", "PENDING_TEACHING")));
        data.put("pendingReviewTasks", pendingReview);
        long pendingGroupReviews = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
            .eq(Task::getReviewStatus, "PENDING_GROUP"));
        long pendingTeachingReviews = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
            .eq(Task::getReviewStatus, "PENDING_TEACHING"));
        data.put("pendingGroupReviews", pendingGroupReviews);
        data.put("pendingTeachingReviews", pendingTeachingReviews);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Task> recentReviewed = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .in(Task::getReviewStatus, List.of("APPROVED", "REJECTED"))
            .ge(Task::getUpdatedAt, thirtyDaysAgo));
        double avgReviewHours = 0;
        long rejectedCount = 0;
        if (!recentReviewed.isEmpty()) {
            double totalHours = 0;
            int count = 0;
            for (Task t : recentReviewed) {
                if (t.getCreatedAt() != null && t.getUpdatedAt() != null) {
                    totalHours += java.time.Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes() / 60.0;
                    count++;
                }
                if ("REJECTED".equals(t.getReviewStatus())) rejectedCount++;
            }
            avgReviewHours = count > 0 ? Math.round(totalHours / count * 10) / 10.0 : 0;
        }
        double rejectionRate = recentReviewed.size() > 0
            ? Math.round(rejectedCount * 1000.0 / recentReviewed.size()) / 10.0 : 0;
        data.put("avgReviewHours", avgReviewHours);
        data.put("rejectionRate", rejectionRate);

        // 互评统计
        long totalPeerReviews = peerReviewMapper.selectCount(new LambdaQueryWrapper<>());
        long submittedPeerReviews = peerReviewMapper.selectCount(new LambdaQueryWrapper<PeerReview>().isNotNull(PeerReview::getSubmittedAt));
        data.put("totalPeerReviews", totalPeerReviews);
        data.put("submittedPeerReviews", submittedPeerReviews);

        // 积分
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        double totalCredits = creditTransactionMapper.selectList(null).stream()
            .filter(t -> "earn".equals(t.getTransactionType()))
            .mapToLong(t -> t.getCreditAmount() != null ? t.getCreditAmount() : 0).sum();
        double monthCredits = creditTransactionMapper.selectList(
            new LambdaQueryWrapper<CreditTransaction>().ge(CreditTransaction::getCreateTime, thisMonth)).stream()
            .filter(t -> "earn".equals(t.getTransactionType()))
            .mapToLong(t -> t.getCreditAmount() != null ? t.getCreditAmount() : 0).sum();
        data.put("totalCreditsAwarded", totalCredits);
        data.put("monthCreditsAwarded", monthCredits);

        data.put("todaySignCount", signRecordMapper.selectCount(
            new LambdaQueryWrapper<SignRecord>().likeRight(SignRecord::getSignDate, LocalDate.now().toString())));
        data.put("todayPosts", bbsPostMapper.selectCount(
            new LambdaQueryWrapper<BbsPost>().ge(BbsPost::getCreateTime, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))));

        // Phase C: 课堂巡课/德育/教研/家长反馈统计
        LocalDate weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        data.put("patrolsThisWeek", classroomPatrolMapper.selectCount(
            new LambdaQueryWrapper<ClassroomPatrol>().ge(ClassroomPatrol::getPatrolDate, weekStart)));
        List<ClassroomPatrol> weekPatrols = classroomPatrolMapper.selectList(
            new LambdaQueryWrapper<ClassroomPatrol>().ge(ClassroomPatrol::getPatrolDate, weekStart)
                .isNotNull(ClassroomPatrol::getDisciplineScore));
        double avgDiscipline = weekPatrols.stream().filter(p -> p.getDisciplineScore() != null)
            .mapToInt(ClassroomPatrol::getDisciplineScore).average().orElse(0);
        data.put("avgDisciplineScore", Math.round(avgDiscipline * 10) / 10.0);
        List<MoralInspection> monthMorals = moralInspectionMapper.selectList(
            new LambdaQueryWrapper<MoralInspection>().ge(MoralInspection::getInspectionDate, monthStart)
                .isNotNull(MoralInspection::getScore));
        double avgMoral = monthMorals.stream().filter(m -> m.getScore() != null)
            .mapToInt(MoralInspection::getScore).average().orElse(0);
        data.put("avgMoralScore", Math.round(avgMoral * 10) / 10.0);
        data.put("researchActivitiesThisMonth", teachingResearchActivityMapper.selectCount(
            new LambdaQueryWrapper<TeachingResearchActivity>().ge(TeachingResearchActivity::getActivityDate, monthStart)));
        data.put("parentFeedbackCount", parentFeedbackSummaryMapper.selectCount(
            new LambdaQueryWrapper<ParentFeedbackSummary>()));

        data.put("gradeDistribution", classesMapper.selectList(null).stream()
            .filter(c -> c.getGrade() != null)
            .collect(Collectors.groupingBy(Classes::getGrade, Collectors.counting())));
        return data;
    }

    // ── 成绩分析（新表：tasks + task_submissions） ─────────

    @Override
    public Map<String, Object> scoreAnalysis(Long stageId, String grade, Long classId, Long taskId) {
        // 查任务
        LambdaQueryWrapper<Task> taskW = new LambdaQueryWrapper<>();
        if (taskId != null) taskW.eq(Task::getId, taskId);
        else taskW.in(Task::getStatus, List.of("PUBLISHED", "ONGOING", "CLOSED"));
        List<Task> tasks = taskMapper.selectList(taskW);
        if (tasks.isEmpty()) return Map.of("classes", List.of(), "summary", Map.of());

        // 所有提交
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskSubmission> allSubs = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds));
        Map<Long, List<TaskSubmission>> subsByTask = allSubs.stream().collect(Collectors.groupingBy(TaskSubmission::getTaskId));

        // 班级映射 + 学生映射
        Map<Long, Classes> classMap = classesMapper.selectList(null).stream()
            .collect(Collectors.toMap(Classes::getId, c -> c));
        Map<Long, Long> studentClassMap = studentMapper.selectList(null).stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.toMap(Student::getId, Student::getClassId, (a, b) -> a));

        // 按班级分组聚合
        Map<Long, List<TaskSubmission>> subsByClass = new HashMap<>();
        for (TaskSubmission sub : allSubs) {
            Long sid = sub.getStudentId();
            Long cid = studentClassMap.get(sid);
            if (cid == null) continue;
            if (classId != null && !cid.equals(classId)) continue;
            if (grade != null && !grade.isEmpty()) {
                Classes cls = classMap.get(cid);
                if (cls == null || cls.getGrade() == null || !cls.getGrade().equals(grade)) continue;
            }
            subsByClass.computeIfAbsent(cid, k -> new ArrayList<>()).add(sub);
        }

        // 班级统计
        List<Map<String, Object>> classStats = new ArrayList<>();
        int[] distBuckets = new int[5]; // 0-59, 60-69, 70-79, 80-89, 90-100
        double totalAvg = 0;
        long totalPassed = 0;
        long totalGraded = 0;
        long totalShouldSubmit = 0;
        long totalSubmitted = 0;
        int classCount = 0;

        for (Map.Entry<Long, List<TaskSubmission>> entry : subsByClass.entrySet()) {
            Long cid = entry.getKey();
            List<TaskSubmission> subs = entry.getValue();
            Classes cls = classMap.get(cid);

            List<TaskSubmission> graded = subs.stream()
                .filter(s -> s.getScore() != null && "GRADED".equals(s.getStatus())).toList();
            if (graded.isEmpty()) continue;

            List<BigDecimal> officialScores = graded.stream()
                .filter(s -> s.getIsOfficial() == null || Boolean.TRUE.equals(s.getIsOfficial()))
                .map(TaskSubmission::getScore)
                .collect(Collectors.toList());
            double avg = ScoreUtils.avg(officialScores);
            double max = graded.stream()
                .filter(s -> s.getIsOfficial() == null || Boolean.TRUE.equals(s.getIsOfficial()))
                .mapToDouble(s -> s.getScore().doubleValue()).max().orElse(0);
            double min = graded.stream()
                .filter(s -> s.getIsOfficial() == null || Boolean.TRUE.equals(s.getIsOfficial()))
                .mapToDouble(s -> s.getScore().doubleValue()).min().orElse(0);
            // 使用任务的 passRate（如果启用），回退 60
            double effectivePassRate = !tasks.isEmpty() && tasks.get(0).getPassRate() != null && tasks.get(0).getPassRate() > 0
                ? tasks.get(0).getPassRate() : 60;
            double totalScoreVal = !tasks.isEmpty() && tasks.get(0).getTotalScore() != null ? tasks.get(0).getTotalScore().doubleValue() : 100.0;
            double passThreshold = totalScoreVal * effectivePassRate / 100.0;
            long passCount = graded.stream()
                .filter(s -> s.getScore() != null && s.getScore().doubleValue() >= passThreshold)
                .count();
            double passRate = graded.size() > 0 ? Math.round(passCount * 1000.0 / graded.size()) / 10.0 : 0;

            Map<String, Object> cs = new HashMap<>();
            cs.put("classId", cid);
            cs.put("className", cls != null ? cls.getClassName() : "未知");
            cs.put("grade", cls != null ? cls.getGrade() : "未知");
            cs.put("submittedCount", subs.size());
            cs.put("gradedCount", graded.size());
            cs.put("avgScore", Math.round(avg * 10) / 10.0);
            cs.put("maxScore", max);
            cs.put("minScore", min);
            cs.put("passCount", passCount);
            cs.put("passRate", passRate);
            // 班级学生总数
            long studentCount = studentClassMap.entrySet().stream()
                .filter(e -> cid.equals(e.getValue())).count();
            cs.put("studentCount", (int) studentCount);
            cs.put("submitRate", studentCount > 0 ? Math.round(subs.size() * 1000.0 / studentCount) / 10.0 : 0);

            // 分数分布
            int[] localDist = new int[5];
            for (TaskSubmission s : graded) {
                double sc = s.getScore().doubleValue();
                if (sc < 60) localDist[0]++;
                else if (sc < 70) localDist[1]++;
                else if (sc < 80) localDist[2]++;
                else if (sc < 90) localDist[3]++;
                else localDist[4]++;
                distBuckets[(int)Math.min(4, Math.max(0, (sc / 10) - 5))]++;
            }
            cs.put("scoreDistribution", List.of(
                Map.of("range", "0-59", "count", localDist[0]),
                Map.of("range", "60-69", "count", localDist[1]),
                Map.of("range", "70-79", "count", localDist[2]),
                Map.of("range", "80-89", "count", localDist[3]),
                Map.of("range", "90-100", "count", localDist[4])
            ));

            totalAvg += avg;
            totalPassed += passCount;
            totalGraded += graded.size();
            totalSubmitted += subs.size();
            classCount++;
            classStats.add(cs);
        }
        classStats.sort((a, b) -> Double.compare((double)b.get("avgScore"), (double)a.get("avgScore")));

        Map<String, Object> summary = new HashMap<>();
        summary.put("classCount", classCount);
        summary.put("totalGraded", totalGraded);
        summary.put("totalSubmitted", totalSubmitted);
        summary.put("overallAvgScore", classCount > 0 ? Math.round(totalAvg / classCount * 10) / 10.0 : 0);
        summary.put("overallPassRate", totalGraded > 0 ? Math.round(totalPassed * 1000.0 / totalGraded) / 10.0 : 0);
        summary.put("overallScoreDistribution", List.of(
            Map.of("range", "0-59", "count", distBuckets[0]),
            Map.of("range", "60-69", "count", distBuckets[1]),
            Map.of("range", "70-79", "count", distBuckets[2]),
            Map.of("range", "80-89", "count", distBuckets[3]),
            Map.of("range", "90-100", "count", distBuckets[4])
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("classStats", classStats);
        result.put("classes", classStats);
        result.put("summary", summary);
        // 附加首个任务的基本信息
        if (!tasks.isEmpty()) {
            Task first = tasks.get(0);
            result.put("taskId", first.getId());
            result.put("taskTitle", first.getTitle());
            result.put("subject", first.getSubject());
            User teacher = userMapper.selectById(first.getTeacherId());
            result.put("teacherName", teacher != null ? teacher.getRealName() : "");
        }
        return result;
    }

    // ── 互评监控 ────────────────────────────────────────

    @Override
    public Map<String, Object> peerReviewStats(Long stageId, String grade) {
        List<PeerReview> allReviews = peerReviewMapper.selectList(null);
        if (allReviews.isEmpty()) return Map.of("classes", List.of(), "summary", Map.of());

        Map<Long, Classes> classMap = classesMapper.selectList(null).stream()
            .collect(Collectors.toMap(Classes::getId, c -> c));
        Map<Long, Long> studentClassMap = studentMapper.selectList(null).stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.toMap(Student::getId, Student::getClassId, (a, b) -> a));

        // 按班级分组
        Map<Long, List<PeerReview>> byClass = new HashMap<>();
        for (PeerReview pr : allReviews) {
            Long cid = studentClassMap.get(pr.getReviewerId());
            if (cid == null) continue;
            if (grade != null && !grade.isEmpty()) {
                Classes cls = classMap.get(cid);
                if (cls == null || cls.getGrade() == null || !cls.getGrade().equals(grade)) continue;
            }
            byClass.computeIfAbsent(cid, k -> new ArrayList<>()).add(pr);
        }

        List<Map<String, Object>> classStats = new ArrayList<>();
        for (Map.Entry<Long, List<PeerReview>> entry : byClass.entrySet()) {
            Long cid = entry.getKey();
            List<PeerReview> reviews = entry.getValue();
            Classes cls = classMap.get(cid);

            long submitted = reviews.stream().filter(r -> r.getSubmittedAt() != null).count();
            double participationRate = reviews.size() > 0 ? Math.round(submitted * 1000.0 / reviews.size()) / 10.0 : 0;

            // 评分离散度（标准差）
            List<Double> allScores = new ArrayList<>();
            for (PeerReview pr : reviews) {
                if (pr.getScoreJson() == null) continue;
                try {
                    Map<String, Object> sj = com.school.teaching.utils.JsonUtils.parseMap(pr.getScoreJson());
                    if (sj != null) sj.values().forEach(v -> {
                        if (v instanceof Number) allScores.add(((Number) v).doubleValue());
                    });
                } catch (Exception ignored) { log.error("解析互评分数JSON失败", ignored); }
            }
            double avgScore = ScoreUtils.avgDouble(allScores);
            double stdDev = ScoreUtils.stdDev(allScores);

            Map<String, Object> cs = new HashMap<>();
            cs.put("classId", cid);
            cs.put("className", cls != null ? cls.getClassName() : "未知");
            cs.put("grade", cls != null ? cls.getGrade() : "未知");
            cs.put("totalReviews", reviews.size());
            cs.put("submittedReviews", submitted);
            cs.put("participationRate", participationRate);
            cs.put("avgScore", Math.round(avgScore * 10) / 10.0);
            cs.put("scoreStdDev", Math.round(stdDev * 10) / 10.0);
            classStats.add(cs);
        }
        classStats.sort((a, b) -> Double.compare((double)b.get("participationRate"), (double)a.get("participationRate")));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalReviews", allReviews.size());
        summary.put("submittedReviews", allReviews.stream().filter(r -> r.getSubmittedAt() != null).count());
        return Map.of("classes", classStats, "summary", summary);
    }

    // ── 审核流程监控 ─────────────────────────────────────

    @Override
    public Map<String, Object> reviewProgress() {
        // 按审核状态分组
        Map<String, Long> statusCounts = new HashMap<>();
        for (String status : List.of("NOT_SUBMITTED", "PENDING_GROUP", "PENDING_TEACHING", "APPROVED", "REJECTED")) {
            long cnt = taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getReviewStatus, status));
            statusCounts.put(status, cnt);
        }

        // 各教研组审核通过率
        List<TeachingGroup> groups = teachingGroupMapper.selectList(null);
        Map<Long, String> groupNameMap = groups.stream().collect(Collectors.toMap(TeachingGroup::getId, TeachingGroup::getName));
        List<Map<String, Object>> groupStats = new ArrayList<>();
        // 简化: 按教研组ID统计（通过备课组关联）
        List<LessonPrepGroup> lpgs = lessonPrepGroupMapper.selectList(null);
        for (LessonPrepGroup lpg : lpgs) {
            Long tgId = lpg.getTeachingGroupId();
            List<Task> groupTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getGradeId, lpg.getGradeId()));
            if (groupTasks.isEmpty()) continue;
            long approved = groupTasks.stream().filter(t -> "APPROVED".equals(t.getReviewStatus())).count();
            long reviewed = groupTasks.stream().filter(t -> !"NOT_SUBMITTED".equals(t.getReviewStatus())
                && !"PENDING_GROUP".equals(t.getReviewStatus())).count();
            Map<String, Object> gs = new HashMap<>();
            gs.put("groupName", groupNameMap.getOrDefault(tgId, "教研组" + tgId));
            gs.put("totalTasks", groupTasks.size());
            gs.put("approvedCount", approved);
            gs.put("approvalRate", groupTasks.size() > 0 ? Math.round(approved * 1000.0 / groupTasks.size()) / 10.0 : 0);
            gs.put("avgReviewHours", 0); // 需要审核时间记录才能算
            groupStats.add(gs);
        }

        return Map.of("statusCounts", statusCounts, "groupStats", groupStats);
    }

    // ── 积分监控 ─────────────────────────────────────────

    @Override
    public Map<String, Object> creditStats() {
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate thisTerm = LocalDate.now().withDayOfYear(1); // 简化: 年初为本学期开始

        List<CreditTransaction> allTxns = creditTransactionMapper.selectList(null);
        long totalIssued = allTxns.stream().filter(t -> "earn".equals(t.getTransactionType()))
            .mapToLong(t -> t.getCreditAmount() != null ? t.getCreditAmount() : 0).sum();
        long monthIssued = allTxns.stream()
            .filter(t -> "earn".equals(t.getTransactionType()) && t.getCreateTime() != null
                && t.getCreateTime().toLocalDate().compareTo(thisMonth) >= 0)
            .mapToLong(t -> t.getCreditAmount() != null ? t.getCreditAmount() : 0).sum();

        // 班级人均积分排行
        Map<Long, Long> studentClassMap = studentMapper.selectList(null).stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.toMap(Student::getId, Student::getClassId, (a, b) -> a));
        Map<Long, Classes> classMap = classesMapper.selectList(null).stream()
            .collect(Collectors.toMap(Classes::getId, c -> c));

        Map<Long, Long> classCredits = new HashMap<>();
        for (CreditTransaction t : allTxns) {
            if (!"earn".equals(t.getTransactionType()) || t.getStudentId() == null) continue;
            Long cid = studentClassMap.get(t.getStudentId());
            if (cid == null) continue;
            classCredits.merge(cid, (long)(t.getCreditAmount() != null ? t.getCreditAmount() : 0), Long::sum);
        }
        Map<Long, Long> classStudentCounts = studentMapper.selectList(null).stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.groupingBy(Student::getClassId, Collectors.counting()));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, Long> e : classCredits.entrySet()) {
            Long cid = e.getKey();
            long total = e.getValue();
            long count = classStudentCounts.getOrDefault(cid, 1L);
            Classes cls = classMap.get(cid);
            Map<String, Object> r = new HashMap<>();
            r.put("classId", cid);
            r.put("className", cls != null ? cls.getClassName() : "未知");
            r.put("grade", cls != null ? cls.getGrade() : "未知");
            r.put("totalCredits", total);
            r.put("avgCredits", Math.round(total * 10.0 / count) / 10.0);
            ranking.add(r);
        }
        ranking.sort((a, b) -> Double.compare((double)b.get("avgCredits"), (double)a.get("avgCredits")));

        // 异常积分（单日超500）
        long threshold = 500;
        List<Map<String, Object>> anomalies = new ArrayList<>();
        Map<String, List<CreditTransaction>> byDay = allTxns.stream()
            .filter(t -> t.getCreateTime() != null && "earn".equals(t.getTransactionType()))
            .collect(Collectors.groupingBy(t -> t.getCreateTime().toLocalDate().toString()));
        for (var entry : byDay.entrySet()) {
            long dayTotal = entry.getValue().stream().mapToLong(t -> t.getCreditAmount() != null ? t.getCreditAmount() : 0).sum();
            if (dayTotal > threshold) {
                Map<String, Object> a = new HashMap<>();
                a.put("date", entry.getKey());
                a.put("amount", dayTotal);
                a.put("count", entry.getValue().size());
                anomalies.add(a);
            }
        }
        anomalies.sort((a, b) -> Double.compare((double)b.get("amount"), (double)a.get("amount")));

        return Map.of(
            "totalIssued", totalIssued,
            "monthIssued", monthIssued,
            "termIssued", totalIssued,
            "classRanking", ranking,
            "anomalies", anomalies
        );
    }

    @Override
    public List<Map<String, Object>> teacherActivity() {
        List<Teacher> teachers = teacherMapper.selectList(null);
        if (teachers.isEmpty()) return List.of();
        Set<Long> teacherIds = teachers.stream().map(Teacher::getId).collect(Collectors.toSet());
        Set<Long> userIds = teachers.stream().map(Teacher::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        // 任务统计（按教师）
        Map<Long, Long> taskCountMap = taskMapper.selectList(
            new LambdaQueryWrapper<Task>().select(Task::getId, Task::getTeacherId).in(Task::getTeacherId, teacherIds))
            .stream().collect(Collectors.groupingBy(Task::getTeacherId, Collectors.counting()));

        // 提交批改统计
        List<Task> allTasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>().in(Task::getTeacherId, teacherIds));
        Set<Long> allTaskIds = allTasks.stream().map(Task::getId).collect(Collectors.toSet());
        Map<Long, List<TaskSubmission>> subByTask = allTaskIds.isEmpty() ? Map.of() :
            taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmission>()
                .in(TaskSubmission::getTaskId, allTaskIds))
                .stream().collect(Collectors.groupingBy(TaskSubmission::getTaskId));
        Map<Long, List<Task>> tasksByTeacher = allTasks.stream()
            .collect(Collectors.groupingBy(Task::getTeacherId));
        Map<Long, User> finalUserMap = userMap;

        // 按时批改统计（24h内）
        Map<Long, Long> gradedOnTimeCountMap = new HashMap<>();
        Map<Long, Double> avgResponseHoursMap = new HashMap<>();
        for (Map.Entry<Long, List<Task>> entry : tasksByTeacher.entrySet()) {
            Long tId = entry.getKey();
            List<TaskSubmission> allGraded = entry.getValue().stream()
                .flatMap(tk -> subByTask.getOrDefault(tk.getId(), List.of()).stream())
                .filter(s -> "GRADED".equals(s.getStatus()) && s.getGradedAt() != null)
                .toList();
            long onTime = allGraded.stream()
                .filter(s -> s.getSubmittedAt() != null && s.getGradedAt() != null
                    && s.getGradedAt().isBefore(s.getSubmittedAt().plusHours(24)))
                .count();
            gradedOnTimeCountMap.put(tId, onTime);
            double avgHours = allGraded.stream()
                .filter(s -> s.getSubmittedAt() != null && s.getGradedAt() != null)
                .mapToDouble(s -> java.time.Duration.between(s.getSubmittedAt(), s.getGradedAt()).toMinutes() / 60.0)
                .average().orElse(0);
            avgResponseHoursMap.put(tId, Math.round(avgHours * 10) / 10.0);
        }

        // 整改完成数
        Map<Long, Long> rectificationMap = inspectionIssueMapper.selectList(
            new LambdaQueryWrapper<InspectionIssue>()
                .in(InspectionIssue::getResolvedBy, teacherIds)
                .eq(InspectionIssue::getStatus, "VERIFIED"))
            .stream().collect(Collectors.groupingBy(InspectionIssue::getResolvedBy, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Teacher t : teachers) {
            Map<String, Object> item = new HashMap<>();
            item.put("teacherId", t.getId());
            User u = finalUserMap.get(t.getUserId());
            item.put("teacherName", u != null ? u.getRealName() : "未知");
            long tasksCreated = taskCountMap.getOrDefault(t.getId(), 0L);
            List<Task> tTasks = tasksByTeacher.getOrDefault(t.getId(), List.of());
            long totalSubs = 0, totalGraded = 0;
            for (Task tk : tTasks) {
                List<TaskSubmission> subs = subByTask.getOrDefault(tk.getId(), List.of());
                totalSubs += subs.size();
                totalGraded += subs.stream().filter(s -> "GRADED".equals(s.getStatus())).count();
            }
            long gradedOnTime = gradedOnTimeCountMap.getOrDefault(t.getId(), 0L);
            long rectCompleted = rectificationMap.getOrDefault(t.getId(), 0L);
            double gradedOnTimeRate = totalGraded > 0 ? Math.round(gradedOnTime * 1000.0 / totalGraded) / 10.0 : 0;
            int activityScore = (int)(tasksCreated * 8 + totalGraded * 4 + gradedOnTime * 6 + rectCompleted * 5);

            item.put("tasksCreated", tasksCreated);
            item.put("submissionsReceived", totalSubs);
            item.put("submissionsGraded", totalGraded);
            item.put("gradedOnTimeRate", gradedOnTimeRate);
            item.put("avgResponseHours", avgResponseHoursMap.getOrDefault(t.getId(), 0.0));
            item.put("rectificationCompleted", rectCompleted);
            item.put("peerReviewCount", 0);
            item.put("activityScore", activityScore);
            result.add(item);
        }
        result.sort((a, b) -> Integer.compare((int)b.get("activityScore"), (int)a.get("activityScore")));
        return result;
    }

    @Override
    public Map<String, Object> getDashboardTrend(String period) {
        LocalDate today = LocalDate.now();
        LocalDate curStart, curEnd, preStart, preEnd;
        if ("MONTHLY".equals(period)) {
            curStart = today.withDayOfMonth(1);
            curEnd = today.with(TemporalAdjusters.lastDayOfMonth());
            preStart = curStart.minusMonths(1);
            preEnd = curStart.minusDays(1);
        } else {
            curStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            curEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            preStart = curStart.minusDays(7);
            preEnd = curEnd.minusDays(7);
        }

        Map<String, Object> cur = calcPeriodStats(curStart, curEnd);
        Map<String, Object> pre = calcPeriodStats(preStart, preEnd);

        double curSubRate = (double) cur.get("submissionRate");
        double preSubRate = (double) pre.get("submissionRate");
        double curPassRate = (double) cur.get("passRate");
        double prePassRate = (double) pre.get("passRate");
        double curAvg = (double) cur.get("avgScore");
        double preAvg = (double) pre.get("avgScore");

        Map<String, Object> trend = new HashMap<>();
        trend.put("submissionRateChange", preSubRate > 0 ? Math.round((curSubRate - preSubRate) * 10) / 10.0 : 0);
        trend.put("passRateChange", prePassRate > 0 ? Math.round((curPassRate - prePassRate) * 10) / 10.0 : 0);
        trend.put("avgScoreChange", preAvg > 0 ? Math.round((curAvg - preAvg) * 10) / 10.0 : 0);

        Map<String, Object> result = new HashMap<>();
        result.put("currentPeriod", cur);
        result.put("previousPeriod", pre);
        result.put("trend", trend);
        return result;
    }

    private Map<String, Object> calcPeriodStats(LocalDate start, LocalDate end) {
        List<TaskSubmission> graded = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getStatus, "GRADED")
                        .isNotNull(TaskSubmission::getScore)
                        .ge(TaskSubmission::getGradedAt, start.atStartOfDay())
                        .lt(TaskSubmission::getGradedAt, end.plusDays(1).atStartOfDay()));
        long totalSubs = graded.size();
        List<BigDecimal> officialScores = graded.stream()
            .filter(s -> s.getIsOfficial() == null || Boolean.TRUE.equals(s.getIsOfficial()))
            .map(TaskSubmission::getScore)
            .collect(Collectors.toList());
        double avgScore = ScoreUtils.avg(officialScores);
        long passCount = graded.stream()
            .filter(s -> s.getScore() != null && s.getScore().doubleValue() >= 60)
            .count();
        double passRate = totalSubs > 0 ? Math.round(passCount * 1000.0 / totalSubs) / 10.0 : 0;
        double submissionRate = 0;
        long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<>());
        if (studentCount > 0) {
            List<TaskSubmission> allInPeriod = taskSubmissionMapper.selectList(
                    new LambdaQueryWrapper<TaskSubmission>()
                            .ge(TaskSubmission::getSubmittedAt, start.atStartOfDay())
                            .lt(TaskSubmission::getSubmittedAt, end.plusDays(1).atStartOfDay()));
            submissionRate = studentCount > 0 ? Math.round(allInPeriod.size() * 1000.0 / studentCount) / 10.0 : 0;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("submissionRate", submissionRate);
        data.put("passRate", passRate);
        data.put("avgScore", Math.round(avgScore * 10) / 10.0);
        data.put("totalSubmissions", totalSubs);
        return data;
    }

    @Override
    public Map<String, Object> getTeacherProfile(Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) return Map.of();
        User user = userMapper.selectById(teacher.getUserId());

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getTeacherId, teacherId));
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());

        long tasksCreated = tasks.size();
        long submissionsReceived = 0, submissionsGraded = 0, gradedOnTime = 0;
        double totalResponseHours = 0;
        int responseCount = 0;

        if (!taskIds.isEmpty()) {
            List<TaskSubmission> subs = taskSubmissionMapper.selectList(
                    new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds));
            submissionsReceived = subs.size();
            submissionsGraded = subs.stream().filter(s -> "GRADED".equals(s.getStatus())).count();
            for (TaskSubmission s : subs) {
                if ("GRADED".equals(s.getStatus()) && s.getSubmittedAt() != null && s.getGradedAt() != null) {
                    double hours = java.time.Duration.between(s.getSubmittedAt(), s.getGradedAt()).toMinutes() / 60.0;
                    totalResponseHours += hours;
                    responseCount++;
                    if (hours <= 24) gradedOnTime++;
                }
            }
        }

        double avgResponseHours = responseCount > 0 ? Math.round(totalResponseHours / responseCount * 10) / 10.0 : 0;
        double gradedOnTimeRate = submissionsGraded > 0 ? Math.round(gradedOnTime * 1000.0 / submissionsGraded) / 10.0 : 0;

        long rectCompleted = inspectionIssueMapper.selectCount(
                new LambdaQueryWrapper<InspectionIssue>()
                        .eq(InspectionIssue::getResolvedBy, teacherId)
                        .eq(InspectionIssue::getStatus, "VERIFIED"));

        int activityScore = (int)(tasksCreated * 8 + submissionsGraded * 4 + gradedOnTime * 6
                + rectCompleted * 5);

        // 近4周活跃度趋势
        List<Map<String, Object>> activityTrend = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            LocalDate wkStart = LocalDate.now().minusWeeks(i)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate wkEnd = wkStart.plusDays(6);
            String weekLabel = "W" + (wkStart.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            List<Task> wkTasks = taskMapper.selectList(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getTeacherId, teacherId)
                            .ge(Task::getCreatedAt, wkStart.atStartOfDay())
                            .lt(Task::getCreatedAt, wkEnd.plusDays(1).atStartOfDay()));
            long wkTaskIds = wkTasks.size();
            Set<Long> wkTidSet = wkTasks.stream().map(Task::getId).collect(Collectors.toSet());
            long wkGraded = 0;
            if (!wkTidSet.isEmpty()) {
                wkGraded = taskSubmissionMapper.selectCount(
                        new LambdaQueryWrapper<TaskSubmission>()
                                .in(TaskSubmission::getTaskId, wkTidSet)
                                .eq(TaskSubmission::getStatus, "GRADED"));
            }
            int wkScore = (int)(wkTaskIds * 8 + wkGraded * 4);
            Map<String, Object> wk = new HashMap<>();
            wk.put("week", weekLabel);
            wk.put("score", wkScore);
            activityTrend.add(wk);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("teacherId", teacherId);
        result.put("teacherName", user != null ? user.getRealName() : "未知");
        result.put("tasksCreated", tasksCreated);
        result.put("submissionsReceived", submissionsReceived);
        result.put("submissionsGraded", submissionsGraded);
        result.put("avgResponseHours", avgResponseHours);
        result.put("gradedOnTimeRate", gradedOnTimeRate);
        result.put("peerReviewsCompleted", 0);
        result.put("rectificationCompleted", rectCompleted);
        result.put("activityScore", activityScore);
        result.put("activityTrend", activityTrend);

        // 所属组织
        List<GroupMember> members = groupMemberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getTeacherId, teacherId));
        List<Map<String, Object>> teachingGroups = new ArrayList<>();
        List<Map<String, Object>> lessonPrepGroups = new ArrayList<>();
        if (!members.isEmpty()) {
            Set<Long> tgIds = new HashSet<>();
            Set<Long> lpgIds = new HashSet<>();
            Map<Long, String> tgRoleMap = new HashMap<>();
            Map<Long, String> lpgRoleMap = new HashMap<>();
            for (GroupMember gm : members) {
                if ("TEACHING".equals(gm.getGroupType())) {
                    tgIds.add(gm.getGroupId());
                    tgRoleMap.put(gm.getGroupId(), gm.getRole());
                } else if ("LESSON_PREP".equals(gm.getGroupType())) {
                    lpgIds.add(gm.getGroupId());
                    lpgRoleMap.put(gm.getGroupId(), gm.getRole());
                }
            }
            if (!tgIds.isEmpty()) {
                Map<Long, String> tgNameMap = teachingGroupMapper.selectBatchIds(tgIds).stream()
                    .collect(Collectors.toMap(TeachingGroup::getId, TeachingGroup::getName));
                for (Long id : tgIds) {
                    String name = tgNameMap.get(id);
                    if (name != null) {
                        teachingGroups.add(Map.of("id", id, "name", name,
                            "role", tgRoleMap.getOrDefault(id, "MEMBER")));
                    }
                }
            }
            if (!lpgIds.isEmpty()) {
                Map<Long, String> lpgNameMap = lessonPrepGroupMapper.selectBatchIds(lpgIds).stream()
                    .collect(Collectors.toMap(LessonPrepGroup::getId, LessonPrepGroup::getName));
                for (Long id : lpgIds) {
                    String name = lpgNameMap.get(id);
                    if (name != null) {
                        lessonPrepGroups.add(Map.of("id", id, "name", name,
                            "role", lpgRoleMap.getOrDefault(id, "MEMBER")));
                    }
                }
            }
        }
        result.put("teachingGroups", teachingGroups);
        result.put("lessonPrepGroups", lessonPrepGroups);

        return result;
    }

    @Override
    public Map<String, Object> getClassProfile(Long classId) {
        Classes cls = classesMapper.selectById(classId);
        if (cls == null) return Map.of();

        Map<Long, Long> studentClassMap = studentMapper.selectList(null).stream()
                .filter(s -> s.getClassId() != null)
                .collect(Collectors.toMap(Student::getId, Student::getClassId, (a, b) -> a));
        List<Classes> allClasses = classesMapper.selectList(null);
        Map<Long, String> classNameMap = allClasses.stream().collect(Collectors.toMap(Classes::getId, Classes::getClassName));
        List<Long> thisClassStudents = studentClassMap.entrySet().stream()
                .filter(e -> e.getValue().equals(classId))
                .map(Map.Entry::getKey).toList();
        long studentCount = thisClassStudents.size();

        // 教学成绩百分位 — 基于平均分排名
        List<TaskSubmission> gradedAll = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getStatus, "GRADED")
                        .isNotNull(TaskSubmission::getScore));
        Map<Long, List<Double>> scoresByClass = new HashMap<>();
        for (TaskSubmission s : gradedAll) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid != null) scoresByClass.computeIfAbsent(cid, k -> new ArrayList<>()).add(s.getScore().doubleValue());
        }
        List<Map.Entry<Long, Double>> classAvgs = new ArrayList<>();
        for (Map.Entry<Long, List<Double>> e : scoresByClass.entrySet()) {
            classAvgs.add(Map.entry(e.getKey(), ScoreUtils.avgDouble(e.getValue())));
        }
        classAvgs.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        double teachingScore = 50;
        List<Double> thisClassScores = scoresByClass.getOrDefault(classId, List.of());
        double thisAvg = ScoreUtils.avgDouble(thisClassScores);
        if (!classAvgs.isEmpty()) {
            int rank = 0;
            for (int i = 0; i < classAvgs.size(); i++) {
                if (classAvgs.get(i).getKey().equals(classId)) { rank = i + 1; break; }
            }
            teachingScore = Math.round((classAvgs.size() - rank + 1) * 100.0 / classAvgs.size());
        }

        // 作业完成 — 基于提交率
        Map<Long, Long> classStudentCountMap = studentMapper.selectList(null).stream()
                .filter(s -> s.getClassId() != null)
                .collect(Collectors.groupingBy(Student::getClassId, Collectors.counting()));
        List<TaskSubmission> allSubs = taskSubmissionMapper.selectList(null);
        Map<Long, Long> subsByClass = new HashMap<>();
        for (TaskSubmission s : allSubs) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid != null) subsByClass.merge(cid, 1L, Long::sum);
        }
        List<Map.Entry<Long, Double>> submitRates = new ArrayList<>();
        for (Long cid : classStudentCountMap.keySet()) {
            long total = classStudentCountMap.get(cid);
            long sub = subsByClass.getOrDefault(cid, 0L);
            submitRates.add(Map.entry(cid, total > 0 ? sub * 100.0 / total : 0));
        }
        submitRates.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        double homeworkScore = 50;
        for (int i = 0; i < submitRates.size(); i++) {
            if (submitRates.get(i).getKey().equals(classId)) {
                homeworkScore = Math.round((submitRates.size() - i) * 100.0 / submitRates.size());
                break;
            }
        }

        // 互评参与百分位
        List<PeerReview> allReviews = peerReviewMapper.selectList(null);
        Map<Long, Long> reviewByClass = new HashMap<>();
        Set<Long> reviewedStudents = new HashSet<>();
        for (PeerReview pr : allReviews) {
            Long cid = studentClassMap.get(pr.getReviewerId());
            if (cid != null) {
                reviewByClass.merge(cid, 1L, Long::sum);
                reviewedStudents.add(pr.getReviewerId());
            }
        }
        Map<Long, Double> peerParticipation = new HashMap<>();
        for (Long cid : classStudentCountMap.keySet()) {
            long total = classStudentCountMap.get(cid);
            if (total == 0) continue;
            long studentsWithReview = studentClassMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(cid) && reviewedStudents.contains(e.getKey())).count();
            peerParticipation.put(cid, studentsWithReview * 100.0 / total);
        }
        double peerScore = 50;
        List<Map.Entry<Long, Double>> peerSorted = new ArrayList<>(peerParticipation.entrySet());
        peerSorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < peerSorted.size(); i++) {
            if (peerSorted.get(i).getKey().equals(classId)) {
                peerScore = Math.round((peerSorted.size() - i) * 100.0 / peerSorted.size());
                break;
            }
        }

        // 课堂纪律（基于课堂巡课 discipline_score）
        List<ClassroomPatrol> classPatrols = classroomPatrolMapper.selectList(
            new LambdaQueryWrapper<ClassroomPatrol>().eq(ClassroomPatrol::getClassId, classId)
                .isNotNull(ClassroomPatrol::getDisciplineScore));
        double avgClassDiscipline = classPatrols.stream().filter(p -> p.getDisciplineScore() != null)
            .mapToInt(ClassroomPatrol::getDisciplineScore).average().orElse(0);
        double disciplineScore = avgClassDiscipline > 0 ? Math.round(avgClassDiscipline * 20) : 50;

        // 积分表现百分位
        List<CreditTransaction> creditTxns = creditTransactionMapper.selectList(
                new LambdaQueryWrapper<CreditTransaction>().eq(CreditTransaction::getTransactionType, "earn"));
        Map<Long, Long> creditsByClass = new HashMap<>();
        for (CreditTransaction ct : creditTxns) {
            if (ct.getStudentId() == null) continue;
            Long cid = studentClassMap.get(ct.getStudentId());
            if (cid != null) creditsByClass.merge(cid, (long)(ct.getCreditAmount() != null ? ct.getCreditAmount() : 0), Long::sum);
        }
        List<Map.Entry<Long, Double>> creditPerCapita = new ArrayList<>();
        for (Long cid : classStudentCountMap.keySet()) {
            long total = classStudentCountMap.get(cid);
            long credits = creditsByClass.getOrDefault(cid, 0L);
            creditPerCapita.add(Map.entry(cid, total > 0 ? credits * 1.0 / total : 0));
        }
        creditPerCapita.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        double creditScore = 50;
        for (int i = 0; i < creditPerCapita.size(); i++) {
            if (creditPerCapita.get(i).getKey().equals(classId)) {
                creditScore = Math.round((creditPerCapita.size() - i) * 100.0 / creditPerCapita.size());
                break;
            }
        }

        // 近5周均分趋势
        List<Map<String, Object>> recentScores = new ArrayList<>();
        List<Map<String, Object>> recentRates = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            LocalDate wkStart = LocalDate.now().minusWeeks(i)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate wkEnd = wkStart.plusDays(6);
            String weekLabel = "W" + (wkStart.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            List<TaskSubmission> wkGraded = taskSubmissionMapper.selectList(
                    new LambdaQueryWrapper<TaskSubmission>()
                            .eq(TaskSubmission::getStatus, "GRADED")
                            .isNotNull(TaskSubmission::getScore)
                            .ge(TaskSubmission::getGradedAt, wkStart.atStartOfDay())
                            .lt(TaskSubmission::getGradedAt, wkEnd.plusDays(1).atStartOfDay()));
            List<Double> wkScores = wkGraded.stream()
                    .filter(s -> classId.equals(studentClassMap.get(s.getStudentId())))
                    .map(s -> s.getScore().doubleValue()).toList();
            double wkAvg = ScoreUtils.avgDouble(wkScores);
            recentScores.add(Map.of("period", weekLabel, "avgScore", Math.round(wkAvg * 10) / 10.0));

            long thisClassSubs = wkGraded.stream()
                    .filter(s -> classId.equals(studentClassMap.get(s.getStudentId()))).count();
            double rate = studentCount > 0 ? Math.round(thisClassSubs * 1000.0 / studentCount) / 10.0 : 0;
            recentRates.add(Map.of("period", weekLabel, "rate", rate));
        }

        Map<String, Object> dimensions = new LinkedHashMap<>();
        dimensions.put("teaching", Map.of("score", teachingScore, "label", "教学成绩"));
        dimensions.put("discipline", Map.of("score", disciplineScore, "label", "课堂纪律"));
        dimensions.put("homework", Map.of("score", homeworkScore, "label", "作业完成"));
        dimensions.put("peerReview", Map.of("score", peerScore, "label", "互评参与"));
        dimensions.put("credit", Map.of("score", creditScore, "label", "积分表现"));

        Map<String, Object> result = new HashMap<>();
        result.put("classId", classId);
        result.put("className", cls.getClassName());
        result.put("grade", cls.getGrade());
        result.put("studentCount", (int) studentCount);
        result.put("dimensions", dimensions);
        result.put("recentScores", recentScores);
        result.put("recentSubmissionRates", recentRates);
        return result;
    }

    @Override
    public List<Map<String, Object>> getTeachingGroupQuality() {
        List<TeachingGroup> groups = teachingGroupMapper.selectList(null);
        if (groups.isEmpty()) return List.of();

        LocalDate since = LocalDate.now().minusDays(30);
        List<Long> groupIds = groups.stream().map(TeachingGroup::getId).collect(Collectors.toList());

        Map<Long, List<GroupMember>> membersByGroup = groupMemberMapper.selectList(
            new LambdaQueryWrapper<GroupMember>()
                .in(GroupMember::getGroupId, groupIds)
                .eq(GroupMember::getGroupType, "TEACHING"))
            .stream().collect(Collectors.groupingBy(GroupMember::getGroupId));

        Map<Long, List<TeachingResearchActivity>> activitiesByGroup = teachingResearchActivityMapper.selectList(
            new LambdaQueryWrapper<TeachingResearchActivity>()
                .in(TeachingResearchActivity::getTeachingGroupId, groupIds)
                .ge(TeachingResearchActivity::getActivityDate, since))
            .stream().collect(Collectors.groupingBy(TeachingResearchActivity::getTeachingGroupId));

        // 获取组内教师的 teacherId 列表 → task 审核通过率 + 班级及格率
        Map<Long, Set<Long>> teacherIdsByGroup = new HashMap<>();
        for (Map.Entry<Long, List<GroupMember>> e : membersByGroup.entrySet()) {
            teacherIdsByGroup.put(e.getKey(),
                e.getValue().stream().map(GroupMember::getTeacherId).collect(Collectors.toSet()));
        }
        Set<Long> allTeacherIds = teacherIdsByGroup.values().stream().flatMap(Set::stream).collect(Collectors.toSet());

        // 教师最近30天的任务
        List<Task> groupTasks = allTeacherIds.isEmpty() ? List.of() : taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .in(Task::getTeacherId, allTeacherIds)
                .ge(Task::getCreatedAt, since.atStartOfDay()));
        Map<Long, List<Task>> tasksByTeacher = groupTasks.stream()
            .collect(Collectors.groupingBy(Task::getTeacherId));

        // 任务的提交批改情况
        Set<Long> groupTaskIds = groupTasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<TaskSubmission> subs = groupTaskIds.isEmpty() ? List.of() : taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .in(TaskSubmission::getTaskId, groupTaskIds));
        Map<Long, List<TaskSubmission>> subsByTask = subs.stream()
            .collect(Collectors.groupingBy(TaskSubmission::getTaskId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (TeachingGroup g : groups) {
            List<GroupMember> members = membersByGroup.getOrDefault(g.getId(), List.of());
            int totalMembers = members.size();
            List<TeachingResearchActivity> acts = activitiesByGroup.getOrDefault(g.getId(), List.of());
            int activityCount = acts.size();
            double avgParticipation = acts.stream().filter(a -> a.getParticipantCount() != null)
                .mapToInt(TeachingResearchActivity::getParticipantCount).average().orElse(0);

            // a. 活动频率
            double freqScore = Math.min(100, activityCount / 4.0 * 100);

            // b. 参与率
            double partScore = totalMembers > 0 && !acts.isEmpty() ? avgParticipation / totalMembers * 100 : 0;
            partScore = Math.min(100, partScore);

            // c. 产出质量
            long withSummary = acts.stream().filter(a -> a.getSummary() != null && !a.getSummary().isBlank()).count();
            double qualityScore = activityCount > 0 ? withSummary * 100.0 / activityCount : 0;

            // d. 审核时效 — 该组教师创建的任务中已批改率
            Set<Long> gTeacherIds = teacherIdsByGroup.getOrDefault(g.getId(), Set.of());
            long totalGroupSubs = 0, gradedGroupSubs = 0;
            for (Long tid : gTeacherIds) {
                for (Task t : tasksByTeacher.getOrDefault(tid, List.of())) {
                    List<TaskSubmission> tSubs = subsByTask.getOrDefault(t.getId(), List.of());
                    totalGroupSubs += tSubs.size();
                    gradedGroupSubs += tSubs.stream().filter(s -> "GRADED".equals(s.getStatus())).count();
                }
            }
            double reviewScore = totalGroupSubs > 0 ? gradedGroupSubs * 100.0 / totalGroupSubs : 60;

            // e. 教学成绩
            double totalPassRate = 0;
            int classCount = 0;
            for (Long tid : gTeacherIds) {
                List<Task> tTasks = tasksByTeacher.getOrDefault(tid, List.of());
                for (Task t : tTasks) {
                    List<TaskSubmission> tSubs = subsByTask.getOrDefault(t.getId(), List.of());
                    double taskPassRate = t.getPassRate() != null && t.getPassRate() > 0 ? t.getPassRate() : 60;
                    double baseScore = t.getTotalScore() != null ? t.getTotalScore().doubleValue() : 0;
                    double passThresh = baseScore > 0 ? baseScore * taskPassRate / 100.0 : 60;
                    long passCount = tSubs.stream()
                        .filter(s -> s.getScore() != null && t.getTotalScore() != null
                            && t.getTotalScore().doubleValue() > 0
                            && s.getScore().doubleValue() >= passThresh)
                        .count();
                    if (!tSubs.isEmpty()) {
                        totalPassRate += passCount * 100.0 / tSubs.size();
                        classCount++;
                    }
                }
            }
            double teachScore = classCount > 0 ? totalPassRate / classCount : 60;

            double totalScore = freqScore * 0.25 + partScore * 0.25 + qualityScore * 0.20
                + reviewScore * 0.15 + teachScore * 0.15;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("groupId", g.getId());
            item.put("groupName", g.getName());
            item.put("scores", Map.of(
                "freq", Math.round(freqScore * 10) / 10.0,
                "participation", Math.round(partScore * 10) / 10.0,
                "quality", Math.round(qualityScore * 10) / 10.0,
                "review", Math.round(reviewScore * 10) / 10.0,
                "teaching", Math.round(teachScore * 10) / 10.0));
            item.put("totalScore", Math.round(totalScore * 10) / 10.0);
            result.add(item);
        }

        result.sort((a, b) -> Double.compare(
            ((Number) b.get("totalScore")).doubleValue(),
            ((Number) a.get("totalScore")).doubleValue()));
        for (int i = 0; i < result.size(); i++) result.get(i).put("rank", i + 1);
        return result;
    }

    @Override
    public Map<String, Object> getPracticeStats() {
        List<Task> practiceTasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>().eq(Task::getTaskType, "PRACTICE")
                .orderByDesc(Task::getCreatedAt));

        if (practiceTasks.isEmpty()) return Map.of("summary", Map.of("totalTasks", 0, "avgSubmitRate", 0, "avgScore", 0, "overdueCount", 0), "classes", List.of());

        List<Classes> allClasses = classesMapper.selectList(null);
        Map<Long, String> classNameMap = allClasses.stream().collect(Collectors.toMap(Classes::getId, Classes::getClassName, (a, b) -> a));
        Map<Long, Long> classStudentCount = allClasses.stream().collect(Collectors.toMap(Classes::getId, c -> studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getClassId, c.getId())), (a, b) -> a));

        Set<Long> taskIds = practiceTasks.stream().map(Task::getId).collect(Collectors.toSet());
        List<PracticeSubmission> allSubs = taskIds.isEmpty() ? List.of() : practiceSubmissionMapper.selectList(
            new LambdaQueryWrapper<PracticeSubmission>().in(PracticeSubmission::getTaskId, taskIds));

        Map<Long, List<PracticeSubmission>> subsByTask = allSubs.stream()
            .collect(Collectors.groupingBy(PracticeSubmission::getTaskId));

        Map<Long, List<Task>> tasksByClass = new HashMap<>();
        for (Task t : practiceTasks) {
            if ("CLASS".equals(t.getTargetType()) && t.getTargetId() != null) {
                tasksByClass.computeIfAbsent(t.getTargetId(), k -> new ArrayList<>()).add(t);
            }
        }

        long overdueCount = 0;
        List<Map<String, Object>> classStats = new ArrayList<>();
        double totalSubmitRate = 0; int classWithData = 0;
        double totalScoreSum = 0; int scoredCount = 0;

        for (Map.Entry<Long, List<Task>> entry : tasksByClass.entrySet()) {
            Long cid = entry.getKey();
            List<Task> tasks = entry.getValue();
            long totalAssigned = classStudentCount.getOrDefault(cid, 0L) * tasks.size();
            if (totalAssigned == 0) continue;

            long submitted = 0; double sumScore = 0; int sc = 0;
            for (Task t : tasks) {
                List<PracticeSubmission> subs = subsByTask.getOrDefault(t.getId(), List.of());
                submitted += subs.size();
                for (PracticeSubmission s : subs) {
                    if (s.getOverallScore() != null) { sumScore += s.getOverallScore().doubleValue(); sc++; }
                    if (t.getDeadline() != null && t.getDeadline().isBefore(LocalDateTime.now())
                        && !"GRADED".equals(s.getStatus())) overdueCount++;
                }
            }

            double rate = totalAssigned > 0 ? submitted * 100.0 / totalAssigned : 0;
            totalSubmitRate += rate; classWithData++;
            double avgScore = sc > 0 ? sumScore / sc : 0;
            if (sc > 0) { totalScoreSum += avgScore; scoredCount++; }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("classId", cid);
            item.put("className", classNameMap.getOrDefault(cid, "?"));
            item.put("taskCount", tasks.size());
            item.put("submittedCount", submitted);
            item.put("totalAssigned", totalAssigned);
            item.put("submitRate", Math.round(rate * 10) / 10.0);
            item.put("avgScore", Math.round(avgScore * 10) / 10.0);
            classStats.add(item);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTasks", practiceTasks.size());
        summary.put("avgSubmitRate", classWithData > 0 ? Math.round(totalSubmitRate / classWithData * 10) / 10.0 : 0);
        summary.put("avgScore", scoredCount > 0 ? Math.round(totalScoreSum / scoredCount * 10) / 10.0 : 0);
        summary.put("overdueCount", overdueCount);

        return Map.of("summary", summary, "classes", classStats);
    }
}
