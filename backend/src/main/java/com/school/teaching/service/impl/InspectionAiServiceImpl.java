package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.AiServiceGateway;
import com.school.teaching.service.InspectionAiService;
import com.school.teaching.utils.ScoreUtils;
import com.school.teaching.service.InspectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionAiServiceImpl implements InspectionAiService {

    private final InspectorService inspectorService;
    private final AiServiceGateway aiGateway;
    private final InspectionAlertLogMapper alertLogMapper;
    private final InspectionIssueMapper issueMapper;
    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final ClassesMapper classesMapper;
    private final StudentMapper studentMapper;
    private final TeachingGroupMapper teachingGroupMapper;
    private final LessonPrepGroupMapper lessonPrepGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final TeachingResearchActivityMapper teachingResearchActivityMapper;
    private final LessonPrepRecordMapper lessonPrepRecordMapper;

    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public String generateWeeklySummary(LocalDate weekStart, LocalDate weekEnd) {
        try {
            Map<String, Object> dash = inspectorService.dashboard();
            long studentCount = ((Number) dash.getOrDefault("studentCount", 0)).longValue();
            long teacherCount = ((Number) dash.getOrDefault("teacherCount", 0)).longValue();
            long publishedTasks = ((Number) dash.getOrDefault("publishedTasks", 0)).longValue();
            long totalSubmissions = ((Number) dash.getOrDefault("totalSubmissions", 0)).longValue();
            long gradedSubmissions = ((Number) dash.getOrDefault("gradedSubmissions", 0)).longValue();
            double submitRate = studentCount > 0 ? Math.round(totalSubmissions * 1000.0 / studentCount) / 10.0 : 0;
            double gradeRate = totalSubmissions > 0 ? Math.round(gradedSubmissions * 1000.0 / totalSubmissions) / 10.0 : 0;

            Map<String, Object> score = inspectorService.scoreAnalysis(null, null, null, null);
            Map<String, Object> summary = (Map<String, Object>) score.getOrDefault("summary", Map.of());
            double avgScore = ((Number) summary.getOrDefault("overallAvgScore", 0)).doubleValue();
            double passRate = ((Number) summary.getOrDefault("overallPassRate", 0)).doubleValue();

            long weekAlerts = alertLogMapper.selectCount(
                new LambdaQueryWrapper<InspectionAlertLog>()
                    .ge(InspectionAlertLog::getTriggedAt, weekStart.atStartOfDay())
                    .lt(InspectionAlertLog::getTriggedAt, weekEnd.plusDays(1).atStartOfDay()));

            long newIssues = issueMapper.selectCount(
                new LambdaQueryWrapper<InspectionIssue>()
                    .ge(InspectionIssue::getCreatedAt, weekStart.atStartOfDay())
                    .lt(InspectionIssue::getCreatedAt, weekEnd.plusDays(1).atStartOfDay()));
            long resolvedIssues = issueMapper.selectCount(
                new LambdaQueryWrapper<InspectionIssue>()
                    .eq(InspectionIssue::getStatus, "VERIFIED")
                    .ge(InspectionIssue::getVerifiedAt, weekStart.atStartOfDay())
                    .lt(InspectionIssue::getVerifiedAt, weekEnd.plusDays(1).atStartOfDay()));

            List<Map<String, Object>> teacherActivity = inspectorService.teacherActivity();
            List<String> top3 = teacherActivity.stream().limit(3)
                .map(t -> String.valueOf(t.getOrDefault("teacherName", "未知")))
                .toList();

            String prompt = String.format(
                "你是教学管理AI助手。请根据以下数据生成一份简洁的教学运行周报（Markdown格式，300-500字）。\n\n本周数据：\n" +
                "- 学生总数: %d, 教师总数: %d\n" +
                "- 发布任务: %d, 提交率: %d%%, 批改率: %d%%\n" +
                "- 均分: %.1f, 及格率: %.1f%%\n" +
                "注：班级平均分基于首次成绩（可比较口径），达标率基于最终成绩（含重测通过）\n" +
                "- 新增问题: %d, 解决问题: %d\n" +
                "- 预警次数: %d\n" +
                "- 教师活跃TOP3: %s\n\n" +
                "请包含：\n1. 本周教学运行概况（2-3句）\n2. 需要关注的问题（如有异常数据）\n3. 建议措施\n\n用中文，语气正式但不刻板。",
                studentCount, teacherCount, publishedTasks, (int) submitRate, (int) gradeRate,
                avgScore, passRate,
                newIssues, resolvedIssues, weekAlerts,
                String.join("、", top3));

            Map<String, Object> params = new HashMap<>();
            params.put("prompt", prompt);
            params.put("maxTokens", 2000);
            params.put("temperature", 0.7);
            return aiGateway.generateContent(params);
        } catch (Exception e) {
            log.error("生成周报失败", e);
            return "周报生成失败，请稍后重试";
        }
    }

    @Override
    public Map<String, Object> detectAnomalies() {
        Map<String, Object> result = new HashMap<>();
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        // 成绩下滑班级：本周均分环比下降 >5 分
        List<Map<String, Object>> scoreDropClasses = new ArrayList<>();
        List<TaskSubmission> thisWeekGraded = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStatus, "GRADED")
                .isNotNull(TaskSubmission::getScore)
                .ge(TaskSubmission::getGradedAt, weekStart.atStartOfDay())
                .lt(TaskSubmission::getGradedAt, weekEnd.plusDays(1).atStartOfDay()));
        List<TaskSubmission> lastWeekGraded = taskSubmissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStatus, "GRADED")
                .isNotNull(TaskSubmission::getScore)
                .ge(TaskSubmission::getGradedAt, weekStart.minusDays(7).atStartOfDay())
                .lt(TaskSubmission::getGradedAt, weekStart.atStartOfDay()));
        Map<Long, Long> studentClassMap = studentMapper.selectList(null).stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.toMap(Student::getId, Student::getClassId, (a, b) -> a));
        Map<Long, String> classNames = classesMapper.selectList(null).stream()
            .collect(Collectors.toMap(Classes::getId, Classes::getClassName));
        Map<Long, List<Double>> thisWeekByClass = new HashMap<>();
        for (TaskSubmission s : thisWeekGraded) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid != null) thisWeekByClass.computeIfAbsent(cid, k -> new ArrayList<>()).add(s.getScore().doubleValue());
        }
        Map<Long, List<Double>> lastWeekByClass = new HashMap<>();
        for (TaskSubmission s : lastWeekGraded) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid != null) lastWeekByClass.computeIfAbsent(cid, k -> new ArrayList<>()).add(s.getScore().doubleValue());
        }
        for (Long cid : thisWeekByClass.keySet()) {
            List<Double> tw = thisWeekByClass.get(cid);
            List<Double> lw = lastWeekByClass.get(cid);
            if (tw == null || tw.isEmpty() || lw == null || lw.isEmpty()) continue;
            double twAvg = ScoreUtils.avgDouble(tw);
            double lwAvg = ScoreUtils.avgDouble(lw);
            double drop = lwAvg - twAvg;
            if (drop > 5) {
                Map<String, Object> item = new HashMap<>();
                item.put("classId", cid);
                item.put("className", classNames.getOrDefault(cid, "未知"));
                item.put("drop", Math.round(drop * 10) / 10.0);
                scoreDropClasses.add(item);
            }
        }

        // 提交率 <50% 的班级
        List<Map<String, Object>> lowSubmitClasses = new ArrayList<>();
        Map<Long, Long> classStudentCounts = studentMapper.selectList(null).stream()
            .filter(s -> s.getClassId() != null)
            .collect(Collectors.groupingBy(Student::getClassId, Collectors.counting()));
        Map<Long, Long> classSubmits = new HashMap<>();
        for (TaskSubmission s : thisWeekGraded) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid != null) classSubmits.merge(cid, 1L, Long::sum);
        }
        for (Map.Entry<Long, Long> entry : classStudentCounts.entrySet()) {
            Long cid = entry.getKey();
            long total = entry.getValue();
            long sub = classSubmits.getOrDefault(cid, 0L);
            double rate = total > 0 ? sub * 100.0 / total : 0;
            if (rate < 50 && total > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("classId", cid);
                item.put("className", classNames.getOrDefault(cid, "未知"));
                item.put("rate", Math.round(rate * 10) / 10.0);
                lowSubmitClasses.add(item);
            }
        }

        // 5天以上无活动的教师
        List<Map<String, Object>> inactiveTeachers = new ArrayList<>();
        LocalDate daysAgo5 = LocalDate.now().minusDays(5);
        List<Teacher> allTeachers = teacherMapper.selectList(null);
        Set<Long> teacherIds = allTeachers.stream().map(Teacher::getId).collect(Collectors.toSet());
        if (!teacherIds.isEmpty()) {
            Set<Long> activeTeacherIds = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                    .in(Task::getTeacherId, teacherIds)
                    .ge(Task::getCreatedAt, daysAgo5.atStartOfDay()))
                .stream().map(Task::getTeacherId).collect(Collectors.toSet());
            for (Teacher t : allTeachers) {
                if (!activeTeacherIds.contains(t.getId())) {
                    User u = userMapper.selectById(t.getUserId());
                    inactiveTeachers.add(Map.of(
                        "teacherId", t.getId(),
                        "teacherName", u != null ? u.getRealName() : "未知",
                        "days", (int) (LocalDate.now().toEpochDay() - taskMapper.selectList(
                            new LambdaQueryWrapper<Task>().eq(Task::getTeacherId, t.getId())
                                .orderByDesc(Task::getCreatedAt).last("LIMIT 1"))
                            .stream().findFirst().map(tk -> tk.getCreatedAt().toLocalDate().toEpochDay())
                            .orElse(LocalDate.now().minusDays(30).toEpochDay()))
                    ));
                }
            }
        }

        // 待批改 >20 的教师
        List<Map<String, Object>> backlogTeachers = new ArrayList<>();
        for (Teacher t : allTeachers) {
            List<Task> tTasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>().eq(Task::getTeacherId, t.getId()));
            if (tTasks.isEmpty()) continue;
            Set<Long> tTaskIds = tTasks.stream().map(Task::getId).collect(Collectors.toSet());
            long pendingCount = taskSubmissionMapper.selectCount(
                new LambdaQueryWrapper<TaskSubmission>()
                    .in(TaskSubmission::getTaskId, tTaskIds)
                    .eq(TaskSubmission::getStatus, "SUBMITTED"));
            if (pendingCount > 20) {
                User u = userMapper.selectById(t.getUserId());
                backlogTeachers.add(Map.of(
                    "teacherId", t.getId(),
                    "teacherName", u != null ? u.getRealName() : "未知",
                    "count", pendingCount));
            }
        }

        // 超期未整改的问题（deadline已过且status不是VERIFIED）
        List<Map<String, Object>> overdueIssues = new ArrayList<>();
        List<InspectionIssue> overdue = issueMapper.selectList(
            new LambdaQueryWrapper<InspectionIssue>()
                .isNotNull(InspectionIssue::getDeadline)
                .lt(InspectionIssue::getDeadline, LocalDate.now())
                .ne(InspectionIssue::getStatus, "VERIFIED")
                .ne(InspectionIssue::getStatus, "CLOSED"));
        for (InspectionIssue iss : overdue) {
            long days = LocalDate.now().toEpochDay() - iss.getDeadline().toEpochDay();
            overdueIssues.add(Map.of(
                "issueId", iss.getId(),
                "title", iss.getTitle() != null ? iss.getTitle() : "",
                "days", (int) days));
        }

        long totalAlerts = alertLogMapper.selectCount(
            new LambdaQueryWrapper<InspectionAlertLog>().eq(InspectionAlertLog::getIsRead, 0));

        result.put("scoreDropClasses", scoreDropClasses);
        result.put("lowSubmitClasses", lowSubmitClasses);
        result.put("inactiveTeachers", inactiveTeachers);
        result.put("backlogTeachers", backlogTeachers);
        result.put("overdueIssues", overdueIssues);
        result.put("totalAlerts", totalAlerts);
        return result;
    }

    @Override
    public Map<String, Object> getRecommendations() {
        Map<String, Object> anomalies = detectAnomalies();
        try {
            String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(anomalies);
            String prompt = String.format(
                "你是教学管理AI助手。根据以下异常数据，推荐本周巡视员应重点关注的3-5个对象（班级或教师），每个附带简短理由。\n\n异常数据：\n%s\n\n" +
                "返回JSON格式：\n[\n  {\"type\": \"CLASS\"/\"TEACHER\", \"id\": X, \"name\": \"X\", \"reason\": \"X\", \"priority\": 1-5}\n]\n" +
                "仅返回JSON数组，不要其他文本。",
                json);
            Map<String, Object> params = new HashMap<>();
            params.put("prompt", prompt);
            params.put("maxTokens", 2000);
            params.put("temperature", 0.3);
            String raw = aiGateway.generateContent(params);
            List<Map<String, Object>> recommendations = om.readValue(raw,
                new TypeReference<List<Map<String, Object>>>() {});
            Map<String, Object> result = new HashMap<>();
            result.put("recommendations", recommendations);
            result.put("anomalies", anomalies);
            return result;
        } catch (Exception e) {
            log.warn("AI推荐解析失败，使用兜底排序: {}", e.getMessage());
            return buildFallbackRecommendations(anomalies);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildFallbackRecommendations(Map<String, Object> anomalies) {
        List<Map<String, Object>> recs = new ArrayList<>();
        PriorityQueue<Map<String, Object>> pq = new PriorityQueue<>(
            (a, b) -> Integer.compare((int) b.getOrDefault("priority", 0), (int) a.getOrDefault("priority", 0)));

        for (Map<String, Object> cls : (List<Map<String, Object>>) anomalies.getOrDefault("scoreDropClasses", List.of())) {
            Map<String, Object> r = new HashMap<>();
            r.put("type", "CLASS");
            r.put("id", cls.get("classId"));
            r.put("name", cls.get("className"));
            r.put("reason", "均分下降" + cls.get("drop") + "分");
            r.put("priority", 5);
            pq.add(r);
        }
        for (Map<String, Object> cls : (List<Map<String, Object>>) anomalies.getOrDefault("lowSubmitClasses", List.of())) {
            Map<String, Object> r = new HashMap<>();
            r.put("type", "CLASS");
            r.put("id", cls.get("classId"));
            r.put("name", cls.get("className"));
            r.put("reason", "提交率仅" + cls.get("rate") + "%");
            r.put("priority", 4);
            pq.add(r);
        }
        for (Map<String, Object> t : (List<Map<String, Object>>) anomalies.getOrDefault("inactiveTeachers", List.of())) {
            Map<String, Object> r = new HashMap<>();
            r.put("type", "TEACHER");
            r.put("id", t.get("teacherId"));
            r.put("name", t.get("teacherName"));
            r.put("reason", t.get("days") + "天无教学活动");
            r.put("priority", 3);
            pq.add(r);
        }
        for (Map<String, Object> t : (List<Map<String, Object>>) anomalies.getOrDefault("backlogTeachers", List.of())) {
            Map<String, Object> r = new HashMap<>();
            r.put("type", "TEACHER");
            r.put("id", t.get("teacherId"));
            r.put("name", t.get("teacherName"));
            r.put("reason", "待批改" + t.get("count") + "份");
            r.put("priority", 3);
            pq.add(r);
        }
        for (Map<String, Object> iss : (List<Map<String, Object>>) anomalies.getOrDefault("overdueIssues", List.of())) {
            Map<String, Object> r = new HashMap<>();
            r.put("type", "ISSUE");
            r.put("id", iss.get("issueId"));
            r.put("name", iss.get("title"));
            r.put("reason", "超期" + iss.get("days") + "天未整改");
            r.put("priority", 4);
            pq.add(r);
        }
        int count = 0;
        while (!pq.isEmpty() && count < 5) {
            recs.add(pq.poll());
            count++;
        }
        return Map.of("recommendations", recs, "anomalies", anomalies);
    }

    @Override
    public String analyzeTeachingResearch() {
        try {
            List<TeachingGroup> groups = teachingGroupMapper.selectList(null);
            StringBuilder ctx = new StringBuilder();
            for (TeachingGroup g : groups) {
                List<TeachingResearchActivity> acts = teachingResearchActivityMapper.selectList(
                    new LambdaQueryWrapper<TeachingResearchActivity>()
                        .eq(TeachingResearchActivity::getTeachingGroupId, g.getId())
                        .orderByDesc(TeachingResearchActivity::getActivityDate)
                        .last("LIMIT 5"));
                long totalMembers = groupMemberMapper.selectCount(
                    new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, g.getId())
                        .eq(GroupMember::getGroupType, "TEACHING"));
                double avgPart = acts.stream().filter(a -> a.getParticipantCount() != null)
                    .mapToInt(TeachingResearchActivity::getParticipantCount).average().orElse(0);
                double partRate = totalMembers > 0 ? avgPart / totalMembers * 100 : 0;

                ctx.append("教研组: ").append(g.getName()).append(", 成员数: ").append(totalMembers);
                ctx.append(", 平均参与率: ").append(String.format("%.0f", partRate)).append("%");
                ctx.append(", 最近活动: ");
                if (acts.isEmpty()) { ctx.append("无活动记录"); } else {
                    for (TeachingResearchActivity a : acts) {
                        ctx.append("[").append(a.getTitle()).append(" 参与")
                            .append(a.getParticipantCount()).append("人");
                        if (a.getSummary() != null && !a.getSummary().isBlank()) ctx.append(" ✓有纪要");
                        ctx.append("] ");
                    }
                }
                ctx.append("\n");
            }

            String prompt = "你是教学管理AI助手。以下是各教研组的活动数据，请分析：\n1) 各教研组活动质量评价\n2) 存在的问题\n3) 改进建议\n\n数据：\n"
                + ctx + "\n请用Markdown格式输出，简洁有力。";
            Map<String, Object> params = new HashMap<>();
            params.put("prompt", prompt);
            params.put("maxTokens", 2000);
            params.put("temperature", 0.7);
            return aiGateway.generateContent(params);
        } catch (Exception e) {
            log.error("教研分析失败", e);
            return "教研分析生成失败，请稍后重试";
        }
    }

    @Override
    public String analyzeLessonPrep() {
        try {
            List<LessonPrepGroup> groups = lessonPrepGroupMapper.selectList(null);
            StringBuilder ctx = new StringBuilder();
            for (LessonPrepGroup g : groups) {
                List<LessonPrepRecord> recs = lessonPrepRecordMapper.selectList(
                    new LambdaQueryWrapper<LessonPrepRecord>()
                        .eq(LessonPrepRecord::getLessonPrepGroupId, g.getId())
                        .orderByDesc(LessonPrepRecord::getRecordDate)
                        .last("LIMIT 5"));
                long totalMembers = groupMemberMapper.selectCount(
                    new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupId, g.getId())
                        .eq(GroupMember::getGroupType, "LESSON_PREP"));
                double avgPart = recs.stream().filter(r -> r.getParticipantCount() != null)
                    .mapToInt(LessonPrepRecord::getParticipantCount).average().orElse(0);
                double partRate = totalMembers > 0 ? avgPart / totalMembers * 100 : 0;

                ctx.append("备课组: ").append(g.getName()).append(", 成员数: ").append(totalMembers);
                ctx.append(", 平均参与率: ").append(String.format("%.0f", partRate)).append("%");
                ctx.append(", 最近备课: ");
                if (recs.isEmpty()) { ctx.append("无备课记录"); } else {
                    for (LessonPrepRecord r : recs) {
                        ctx.append("[").append(r.getTitle()).append(" 参与")
                            .append(r.getParticipantCount()).append("人");
                        if (r.getContent() != null && !r.getContent().isBlank()) ctx.append(" ✓有内容");
                        ctx.append("] ");
                    }
                }
                ctx.append("\n");
            }

            String prompt = "你是教学管理AI助手。以下是各备课组的备课数据，请分析：\n1) 各备课组备课情况评价\n2) 存在的问题\n3) 改进建议\n\n数据：\n"
                + ctx + "\n请用Markdown格式输出，简洁有力。";
            Map<String, Object> params = new HashMap<>();
            params.put("prompt", prompt);
            params.put("maxTokens", 2000);
            params.put("temperature", 0.7);
            return aiGateway.generateContent(params);
        } catch (Exception e) {
            log.error("备课分析失败", e);
            return "备课分析生成失败，请稍后重试";
        }
    }
}
