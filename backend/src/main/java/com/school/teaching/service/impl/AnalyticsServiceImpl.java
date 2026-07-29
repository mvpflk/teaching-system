package com.school.teaching.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.AnalyticsService;
import com.school.teaching.utils.ScoreUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 轻量成长分析服务 — 复用现有表数据，不建新表
 */
@Slf4j
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired private PrecisionProgressMapper progressMapper;
    @Autowired private WrongQuestionMapper wrongMapper;
    @Autowired private CreditTransactionMapper creditMapper;
    @Autowired private TaskSubmissionMapper submissionMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private KnowledgeNodeMapper knowledgeNodeMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired(required = false) private AiCallLogMapper aiCallLogMapper;
    @Autowired(required = false) private TeacherActivityMapper teacherActivityMapper;
    @Autowired(required = false) private ClassesMapper classesMapper;

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public List<Map<String, Object>> getGrowthCurve(Long studentId, String subject) {
        List<PrecisionProgress> list = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(subject != null && !subject.isEmpty(), PrecisionProgress::getSubject, subject)
                .orderByAsc(PrecisionProgress::getUpdateTime));
        if (list.isEmpty()) return List.of();
        return aggregateCurve(list);
    }

    /** 从 precision_progress 列表按周聚合掌握度 */
    private List<Map<String, Object>> aggregateCurve(List<PrecisionProgress> list) {
        DateTimeFormatter weekFmt = DateTimeFormatter.ofPattern("yyyy-'W'ww", Locale.ENGLISH);
        Map<String, Double> weekAvg = list.stream()
            .filter(p -> p.getUpdateTime() != null && p.getMasteryPercent() != null)
            .collect(Collectors.groupingBy(
                p -> weekFmt.format(p.getUpdateTime()),
                LinkedHashMap::new,
                Collectors.averagingDouble(p -> p.getMasteryPercent().doubleValue())));

        double prev = -1;
        List<Map<String, Object>> curve = new ArrayList<>();
        for (Map.Entry<String, Double> e : weekAvg.entrySet()) {
            int val = (int) Math.round(e.getValue());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("week", e.getKey());
            point.put("masteryPercent", val);
            if (prev >= 0) point.put("change", val - (int) prev);
            prev = val;
            curve.add(point);
        }
        return curve;
    }

    @Override
    public List<Map<String, Object>> getKnowledgeRadar(Long studentId, String subject) {
        // 从 precision_progress 提取每个知识点的掌握度
        List<PrecisionProgress> list = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .eq(subject != null && !subject.isEmpty(), PrecisionProgress::getSubject, subject));

        if (list.isEmpty()) return List.of();

        // 批量查询节点名称
        Set<Long> nodeIds = list.stream().map(PrecisionProgress::getNodeId).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, String> nameMap;
        if (!nodeIds.isEmpty()) {
            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>().in(KnowledgeNode::getId, nodeIds));
            nameMap = nodes.stream().collect(Collectors.toMap(KnowledgeNode::getId, KnowledgeNode::getName, (a, b) -> a));
        } else {
            nameMap = Collections.emptyMap();
        }

        return list.stream()
            .filter(p -> p.getMasteryPercent() != null)
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nodeId", p.getNodeId());
                m.put("nodeName", nameMap.getOrDefault(p.getNodeId(), "知识点" + p.getNodeId()));
                m.put("masteryPercent", p.getMasteryPercent().intValue());
                m.put("status", p.getStatus());
                return m;
            }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAchievements(Long studentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> allAchievements = new ArrayList<>();
        long totalEarned = 0;
        int taskCount = 0;
        boolean hasA = false;

        // 累计积分
        List<CreditTransaction> credits = creditMapper.selectList(
            new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getStudentId, studentId)
                .eq(CreditTransaction::getTransactionType, "earn"));
        for (CreditTransaction ct : credits) {
            if (ct.getCreditAmount() != null) totalEarned += ct.getCreditAmount();
        }
        // 任务完成数
        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId)
                .eq(TaskSubmission::getStatus, "SUBMITTED"));
        taskCount = subs.size();
        for (TaskSubmission s : subs) {
            if ("A".equals(s.getGradeLevel())) { hasA = true; break; }
        }
        // 签到连续天数
        Student st = studentMapper.selectById(studentId);
        int streak = st != null && st.getCurrentStreak() != null ? st.getCurrentStreak() : 0;

        // 错题统计
        Long masteredCount = wrongMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getIsMastered, 1));
        Long totalWrong = wrongMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId));

        // 偏科提分统计
        LambdaQueryWrapper<PrecisionProgress> progW = new LambdaQueryWrapper<PrecisionProgress>()
            .eq(PrecisionProgress::getStudentId, studentId);
        List<PrecisionProgress> allProgress = progressMapper.selectList(progW);
        boolean has60Plus = false;
        boolean has80Mastery = false;
        int distinctPracticeDays = 0;
        boolean hasPhotoUpload = false;
        java.util.Set<String> practiceDates = new java.util.LinkedHashSet<>();
        for (PrecisionProgress p : allProgress) {
            if (p.getMasteryPercent() != null) {
                if (p.getMasteryPercent().compareTo(new java.math.BigDecimal("60")) >= 0) has60Plus = true;
                if (p.getMasteryPercent().compareTo(new java.math.BigDecimal("80")) >= 0) has80Mastery = true;
            }
            if (p.getLastPracticeAt() != null) {
                practiceDates.add(p.getLastPracticeAt().toLocalDate().toString());
            }
            if (p.getStatus() != null && p.getStatus().contains("photo")) hasPhotoUpload = true;
        }
        // 连续练习天数：从 precision_progress 的 lastPracticeAt 统计
        distinctPracticeDays = practiceDates.size();

        // 检查全部成就（逐项判定 earned=true/false）
        AchievementDef[] defs = {
            new AchievementDef("积分新手", "累计获得10积分", "credit", totalEarned >= 10),
            new AchievementDef("积分达人", "累计获得50积分", "credit", totalEarned >= 50),
            new AchievementDef("积分大师", "累计获得100积分", "credit", totalEarned >= 100),
            new AchievementDef("积分传奇", "累计获得200积分", "credit", totalEarned >= 200),
            new AchievementDef("初次任务", "完成第1个任务", "task", taskCount >= 1),
            new AchievementDef("勤奋好学", "完成5个任务", "task", taskCount >= 5),
            new AchievementDef("学霸", "完成10个任务", "task", taskCount >= 10),
            new AchievementDef("初来乍到", "连续签到3天", "sign", streak >= 3),
            new AchievementDef("坚持不懈", "连续签到7天", "sign", streak >= 7),
            new AchievementDef("签到王者", "连续签到30天", "sign", streak >= 30),
            new AchievementDef("A+达人", "获得过A级评价", "grade", hasA),
        };
        // 错题成就（需要查询数据）
        boolean halfMastered = totalWrong > 0 && masteredCount * 2 >= totalWrong;
        boolean allMastered = totalWrong > 0 && masteredCount >= totalWrong;
        AchievementDef[] wrongDefs = {
            new AchievementDef("半壁江山", "错题掌握过半", "wrong", halfMastered),
            new AchievementDef("横扫千军", "全部错题已掌握", "wrong", allMastered),
        };
        // R97+ 偏科提分专项成就
        AchievementDef[] precisionDefs = {
            new AchievementDef("坚持不懈", "连续练习7天", "practice", distinctPracticeDays >= 7),
            new AchievementDef("初露锋芒", "首次突破60分", "practice", has60Plus),
            new AchievementDef("融会贯通", "知识点掌握率≥80%", "practice", has80Mastery),
            new AchievementDef("科技达人", "拍照上传首次使用", "photo", hasPhotoUpload),
        };

        for (AchievementDef d : defs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.name); m.put("description", d.desc); m.put("category", d.cat); m.put("earned", d.earned);
            allAchievements.add(m);
        }
        for (AchievementDef d : wrongDefs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.name); m.put("description", d.desc); m.put("category", d.cat); m.put("earned", d.earned);
            allAchievements.add(m);
        }
        for (AchievementDef d : precisionDefs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.name); m.put("description", d.desc); m.put("category", d.cat); m.put("earned", d.earned);
            allAchievements.add(m);
        }

        result.put("all", allAchievements);
        long earnedCount = allAchievements.stream().filter(a -> Boolean.TRUE.equals(a.get("earned"))).count();
        result.put("earnedCount", earnedCount);
        result.put("totalCount", allAchievements.size());
        result.put("totalEarned", totalEarned);
        result.put("taskCount", taskCount);
        result.put("streak", streak);
        result.put("masteredWrong", masteredCount);
        result.put("totalWrong", totalWrong);
        return result;
    }

    private record AchievementDef(String name, String desc, String cat, boolean earned) {}

    @Override
    public Map<String, Object> getStudentSummary(Long studentId, String subject) {
        Map<String, Object> s = new LinkedHashMap<>();
        LambdaQueryWrapper<PrecisionProgress> wrapper = new LambdaQueryWrapper<PrecisionProgress>()
            .eq(PrecisionProgress::getStudentId, studentId);
        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(PrecisionProgress::getSubject, subject);
        }
        List<PrecisionProgress> progs = progressMapper.selectList(wrapper);
        long totalAttempts = progs.stream().mapToLong(p -> p.getTotalAttempts() != null ? p.getTotalAttempts() : 0).sum();
        long masteredNodes = progs.stream().filter(p -> "mastered".equals(p.getStatus())).count();
        double avgMastery = progs.stream().mapToDouble(p -> p.getMasteryPercent() != null ? p.getMasteryPercent().doubleValue() : 0)
            .average().orElse(0);
        int streakWeeks = 0, lastDiagnoseScore = 0;
        Student st = studentMapper.selectById(studentId);
        if (st != null && st.getPrecisionProfile() != null) {
            try {
                Map<String, Object> pf = om.readValue(st.getPrecisionProfile(), new TypeReference<Map<String, Object>>() {});
                for (Object val : pf.values()) {
                    if (val instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sp = (Map<String, Object>) val;
                        if (sp.get("streakWeeks") instanceof Integer sw) streakWeeks = Math.max(streakWeeks, sw);
                        if (sp.get("diagnoseScore") instanceof Integer ds) lastDiagnoseScore = Math.max(lastDiagnoseScore, ds);
                    }
                }
            } catch (Exception ignored) { log.warn("偏科profile解析失败", ignored); }
        }
        s.put("totalPractices", totalAttempts);
        s.put("masteredNodes", masteredNodes);
        s.put("totalNodes", progs.size());
        s.put("avgMastery", Math.round(avgMastery));
        s.put("streakWeeks", streakWeeks);
        s.put("lastDiagnoseScore", lastDiagnoseScore);
        return s;
    }

    @Override
    public List<String> getStudentAvailableSubjects(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) {
            log.warn("学生 {} 无班级信息，无法获取开设学科", studentId);
            return List.of();
        }
        // 1. 从 teacher_classes 表查该班级所有教师任教学科
        List<TeacherClass> teacherClasses = teacherClassMapper.selectList(
            new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getClassId, student.getClassId()));
        if (!teacherClasses.isEmpty()) {
            java.util.LinkedHashSet<String> subjects = new java.util.LinkedHashSet<>();
            for (TeacherClass tc : teacherClasses) {
                if (tc.getSubject() != null && !tc.getSubject().isEmpty()) {
                    for (String part : tc.getSubject().split("[,，、]")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) subjects.add(trimmed);
                    }
                }
            }
            if (!subjects.isEmpty()) {
                log.info("学生 {} 班级 {} 开设学科: {}", studentId, student.getClassId(), subjects);
                return new ArrayList<>(subjects);
            }
        }
        // 2. 兜底：从 precision_progress 中取该学生的学科
        List<PrecisionProgress> progs = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .eq(PrecisionProgress::getStudentId, studentId)
                .select(PrecisionProgress::getSubject));
        return progs.stream()
            .map(PrecisionProgress::getSubject)
            .filter(s -> s != null && !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getDailyEncouragement(Long studentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 查未掌握错题数 → 判断阶段
        Long unmastered = wrongMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, studentId)
                .eq(WrongQuestion::getIsMastered, 0));

        // 查最近7天是否做了诊断
        Student st = studentMapper.selectById(studentId);
        boolean hasRecentActivity = false;
        double recentScore = 0;
        if (st != null && st.getPrecisionProfile() != null) {
            try {
                Map<String, Object> pf = om.readValue(st.getPrecisionProfile(),
                    new TypeReference<Map<String, Object>>() {});
                for (Object val : pf.values()) {
                    if (val instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sp = (Map<String, Object>) val;
                        if (sp.get("lastDiagnoseAt") instanceof String lastAt) {
                            LocalDate ld = LocalDate.parse(lastAt.substring(0, 10));
                            if (ld.isAfter(LocalDate.now().minusDays(7))) {
                                hasRecentActivity = true;
                                if (sp.get("diagnoseScore") instanceof Number ns) {
                                    recentScore = Math.max(recentScore, ns.doubleValue());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) { log.warn("偏科统计解析失败", ignored); }
        }

        // 鼓励模板 — 根据阶段匹配
        String title, detail;
        if (unmastered == null || unmastered == 0) {
            title = "🎉 太棒了！所有错题都已掌握";
            detail = "继续保持，挑战更高目标！";
        } else if (unmastered <= 3) {
            title = "💪 只差临门一脚";
            detail = "还有 " + unmastered + " 道错题，今天花5分钟就能清零！";
        } else if (unmastered <= 10) {
            title = "📈 稳扎稳打，持续进步";
            detail = "已掌握大部分知识点，" + unmastered + " 道错题待攻克，按计划每天练习。";
        } else {
            title = "🌱 每一步都算数";
            detail = "还有 " + unmastered + " 道错题，从最薄弱的知识点开始，每天3道题，坚持就是胜利。";
        }
        // 如果有最近活动且分数不错，替换为更积极的版本
        if (hasRecentActivity && recentScore >= 60) {
            title = "🔥 你正在进步！";
            detail = "最近诊断得分 " + (int) recentScore + "%，比之前有明显提升，继续保持节奏！";
        }

        result.put("title", title);
        result.put("detail", detail);
        result.put("unmastered", unmastered);
        result.put("hasRecentActivity", hasRecentActivity);
        return result;
    }

    @Override
    public List<Map<String, Object>> getClassGrowthCurves(Long classId, String subject) {
        // 1. 查询该班所有学生
        List<Student> classStudents = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        if (classStudents.isEmpty()) return List.of();

        List<Long> studentIds = classStudents.stream().map(Student::getId).collect(Collectors.toList());

        // 2. 批量查询学生真实姓名 (users 表)
        List<Long> userIds = classStudents.stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, String> nameMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            nameMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));
        }

        // 3. 批量查询全班 progress 数据（一次 SQL）
        List<PrecisionProgress> allProgress = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .in(PrecisionProgress::getStudentId, studentIds)
                .eq(subject != null && !subject.isEmpty(), PrecisionProgress::getSubject, subject)
                .orderByAsc(PrecisionProgress::getUpdateTime));

        // 4. 按学生分组 → 各自聚合曲线
        Map<Long, List<PrecisionProgress>> grouped = allProgress.stream()
            .collect(Collectors.groupingBy(PrecisionProgress::getStudentId));

        List<Map<String, Object>> result = new ArrayList<>();
        // 班级平均分（跨学生）
        List<Map<String, Object>> classAvgCurve = buildClassAverageCurve(allProgress);
        if (!classAvgCurve.isEmpty()) {
            Map<String, Object> avgEntry = new LinkedHashMap<>();
            avgEntry.put("studentId", 0);
            avgEntry.put("studentName", "班级平均");
            avgEntry.put("curve", classAvgCurve);
            avgEntry.put("isAverage", true);
            result.add(avgEntry);
        }

        for (Student s : classStudents) {
            Long uid = s.getUserId();
            String name = nameMap.getOrDefault(uid, "学生" + s.getId());
            List<PrecisionProgress> sp = grouped.getOrDefault(s.getId(), List.of());
            List<Map<String, Object>> curve = sp.isEmpty() ? List.of() : aggregateCurve(sp);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("studentId", s.getId());
            entry.put("studentName", name);
            entry.put("curve", curve);
            entry.put("isAverage", false);
            result.add(entry);
        }
        return result;
    }

    /** 按周计算全班平均掌握度曲线 */
    private List<Map<String, Object>> buildClassAverageCurve(List<PrecisionProgress> all) {
        if (all.isEmpty()) return List.of();
        DateTimeFormatter weekFmt = DateTimeFormatter.ofPattern("yyyy-'W'ww", Locale.ENGLISH);
        // 按周分组，取每个学生在该周的平均掌握度，再跨学生平均
        Map<String, Double> weekAvg = all.stream()
            .filter(p -> p.getUpdateTime() != null && p.getMasteryPercent() != null)
            .collect(Collectors.groupingBy(
                p -> weekFmt.format(p.getUpdateTime()),
                LinkedHashMap::new,
                Collectors.averagingDouble(p -> p.getMasteryPercent().doubleValue())));

        double prev = -1;
        List<Map<String, Object>> curve = new ArrayList<>();
        for (Map.Entry<String, Double> e : weekAvg.entrySet()) {
            int val = (int) Math.round(e.getValue());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("week", e.getKey());
            point.put("masteryPercent", val);
            if (prev >= 0) point.put("change", val - (int) prev);
            prev = val;
            curve.add(point);
        }
        return curve;
    }

    public String exportScores(Long classId, String subject, LocalDate startDate, LocalDate endDate, boolean blinded) {
        // 1. 获取班级学生
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        if (students.isEmpty()) return "";
        List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
        // 学生姓名从users表获取
        List<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toList());
        Map<Long, String> studentNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            Map<Long, String> userNames = users.stream().collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));
            for (Student s : students) {
                String name = userNames.get(s.getUserId());
                studentNames.put(s.getId(), name != null ? name : "学生" + s.getId());
            }
        }
        // 2. 获取班级任务
        var taskQuery = new LambdaQueryWrapper<Task>().eq(Task::getTargetType, "CLASS").eq(Task::getTargetId, classId);
        if (startDate != null) taskQuery.ge(Task::getCreatedAt, startDate.atStartOfDay());
        if (endDate != null) taskQuery.le(Task::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        if (subject != null && !subject.isEmpty()) taskQuery.eq(Task::getSubject, subject);
        taskQuery.orderByAsc(Task::getCreatedAt);
        List<Task> tasks = taskMapper.selectList(taskQuery);
        if (tasks.isEmpty()) return "";
        List<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toList());
        // 3. 获取提交记录
        var subQuery = new LambdaQueryWrapper<TaskSubmission>().in(TaskSubmission::getTaskId, taskIds).in(TaskSubmission::getStudentId, studentIds);
        List<TaskSubmission> subs = submissionMapper.selectList(subQuery);
        // 4. 构建得分矩阵
        Map<String, Map<Long, Double>> matrix = new LinkedHashMap<>();
        for (Student s : students) {
            matrix.put(studentNames.getOrDefault(s.getId(), "学生" + s.getId()), new LinkedHashMap<>());
        }
        for (TaskSubmission sub : subs) {
            String name = studentNames.get(sub.getStudentId());
            if (name != null && matrix.containsKey(name)) {
                matrix.get(name).put(sub.getTaskId(), sub.getScore() != null ? sub.getScore().doubleValue() : 0);
            }
        }
        // 5. 盲化映射（若启用）
        Map<String, String> blindNameMap = new LinkedHashMap<>();
        Map<Long, String> blindTaskMap = new LinkedHashMap<>();
        if (blinded) {
            int si = 1;
            java.util.List<String> sortedNames = new java.util.ArrayList<>(matrix.keySet());
            java.util.Collections.sort(sortedNames);
            for (String name : sortedNames) blindNameMap.put(name, "S" + (si++));
            int ti = 1;
            for (Task t : tasks) blindTaskMap.put(t.getId(), "任务" + (ti++));
        }

        // 5. 输出CSV
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM for Excel
        sb.append(blinded ? "学生编码" : "学生姓名");
        for (Task t : tasks) {
            sb.append(",").append(escapeCsv(blinded ? blindTaskMap.getOrDefault(t.getId(), t.getTitle()) : t.getTitle()));
        }
        sb.append("\n");
        for (Map.Entry<String, Map<Long, Double>> row : matrix.entrySet()) {
            String label = blinded ? blindNameMap.getOrDefault(row.getKey(), row.getKey()) : row.getKey();
            sb.append(escapeCsv(label));
            for (Task t : tasks) {
                Double score = row.getValue().get(t.getId());
                sb.append(",").append(score != null ? score : "");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    // ── E5: 课题研究数据一键导出 ──
    @Override
    public byte[] exportResearchData(boolean blinded) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. 系统概况
        long totalUsers = userMapper.selectCount(null);
        long totalStudents = studentMapper.selectCount(null);
        int totalTeachers = teacherClassMapper != null ? teacherClassMapper.selectList(null).stream()
            .map(tc -> tc.getTeacherId()).collect(java.util.stream.Collectors.toSet()).size() : 0;
        long totalClasses = classesMapper != null ? classesMapper.selectCount(null) : 0;
        java.time.LocalDate firstDay = getFirstActivityDate();
        long runDays = java.time.temporal.ChronoUnit.DAYS.between(firstDay, java.time.LocalDate.now()) + 1;
        data.put("sysUsers", totalUsers);
        data.put("sysStudents", totalStudents);
        data.put("sysTeachers", totalTeachers);
        data.put("sysClasses", totalClasses);
        data.put("sysRunDays", runDays);
        data.put("sysFirstDay", firstDay.toString());

        // 2. 任务数据
        long totalTasks = taskMapper.selectCount(null);
        data.put("tasksTotal", totalTasks);
        long totalSubmissions = submissionMapper.selectCount(null);
        data.put("submissionsTotal", totalSubmissions);

        // 提交率：按实际有提交的任务和学生统计
        // 查询所有提交，计算每个任务平均提交学生数/每个学生平均提交任务数
        List<TaskSubmission> allSubmissions = submissionMapper.selectList(null);
        Set<String> uniquePairs = allSubmissions.stream()
            .map(s -> s.getTaskId() + "_" + s.getStudentId())
            .collect(Collectors.toSet());
        Set<Long> activeTasks = allSubmissions.stream()
            .map(TaskSubmission::getTaskId)
            .collect(Collectors.toSet());
        Set<Long> activeStudents = allSubmissions.stream()
            .map(TaskSubmission::getStudentId)
            .collect(Collectors.toSet());
        double avgSubmissionsPerTask = activeTasks.isEmpty() ? 0
            : Math.round((double) uniquePairs.size() / activeTasks.size() * 100.0) / 100.0;
        double avgSubmissionsPerStudent = activeStudents.isEmpty() ? 0
            : Math.round((double) uniquePairs.size() / activeStudents.size() * 100.0) / 100.0;
        data.put("avgSubmissionsPerTask", avgSubmissionsPerTask);
        data.put("avgSubmissionsPerStudent", avgSubmissionsPerStudent);
        data.put("submissionRateNote", "avgSubmissionsPerTask = 平均每个活跃任务的学生提交数; avgSubmissionsPerStudent = 平均每个活跃学生的任务提交数");

        // 3. AI 调用
        if (aiCallLogMapper != null) {
            long aiTotal = aiCallLogMapper.selectCount(null);
            data.put("aiTotal", aiTotal);
            // 按capability分布
            List<AiCallLog> aiLogs = aiCallLogMapper.selectList(null);
            Map<String, Long> aiByCap = aiLogs.stream().collect(java.util.stream.Collectors.groupingBy(
                l -> l.getCapability() != null ? l.getCapability() : "UNKNOWN", java.util.stream.Collectors.counting()));
            data.put("aiByCapability", aiByCap);
            long totalTokens = aiLogs.stream().filter(l -> l.getTokensUsed() != null).mapToLong(AiCallLog::getTokensUsed).sum();
            data.put("aiTokens", totalTokens);
            double avgLatency = aiLogs.stream().filter(l -> l.getLatencyMs() != null).mapToInt(AiCallLog::getLatencyMs).average().orElse(0);
            data.put("aiAvgLatency", Math.round(avgLatency));
        }

        // 4. 偏科诊断
        long diagCount = progressMapper.selectCount(null);
        data.put("precisionTotal", diagCount);
        List<PrecisionProgress> allProgress = progressMapper.selectList(null);
        List<BigDecimal> masteryList = allProgress.stream()
            .filter(p -> p.getMasteryPercent() != null)
            .map(PrecisionProgress::getMasteryPercent)
            .collect(Collectors.toList());
        double avgMastery = ScoreUtils.avg(masteryList);
        data.put("precisionAvgMastery", Math.round(avgMastery * 10) / 10.0);

        // 5. 知识点覆盖
        long l4Count = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>().eq(KnowledgeNode::getLevel, 4)).size();
        data.put("kpL4Count", l4Count);

        // 6. 错题数据
        long wrongTotal = wrongMapper.selectCount(null);
        long wrongMastered = wrongMapper.selectCount(
            new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getIsMastered, 1));
        data.put("wrongTotal", wrongTotal);
        data.put("wrongMasteredRate", wrongTotal > 0 ? Math.round((double) wrongMastered / wrongTotal * 1000) / 10.0 : 0);

        // 7. 教师活跃度（E2数据）
        List<Map<String, Object>> teacherStats = new ArrayList<>();
        if (teacherActivityMapper != null) {
            List<TeacherActivity> acts = teacherActivityMapper.selectList(null);
            Map<String, Long> byAction = acts.stream().collect(java.util.stream.Collectors.groupingBy(
                a -> a.getAction() != null ? a.getAction() : "UNKNOWN", java.util.stream.Collectors.counting()));
            byAction.forEach((k, v) -> teacherStats.add(Map.of("action", k, "count", v)));
        }
        data.put("teacherActivity", teacherStats);

        // 8. 课题组别
        if (classesMapper != null) {
            List<Classes> allClasses = classesMapper.selectList(null);
            long expCount = allClasses.stream().filter(c -> "EXPERIMENT".equals(c.getResearchGroup())).count();
            long ctrlCount = allClasses.stream().filter(c -> "CONTROL".equals(c.getResearchGroup())).count();
            data.put("experimentClasses", expCount);
            data.put("controlClasses", ctrlCount);
        }

        // 输出CSV（简单汇总格式）
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("指标,数值\n");
        data.forEach((k, v) -> {
            sb.append(k).append(",");
            if (v instanceof Map<?,?> m) {
                sb.append(m.toString().replace(",", ";"));
            } else {
                sb.append(String.valueOf(v));
            }
            sb.append("\n");
        });

        // 追加明细：AI能力分布
        if (data.get("aiByCapability") instanceof Map<?,?> aiCap) {
            sb.append("\nAI能力,调用次数\n");
            aiCap.forEach((k, v) -> sb.append(k).append(",").append(v).append("\n"));
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ── E6: 知识点掌握趋势 ──
    @Override
    public List<Map<String, Object>> getKnowledgeTrend(Long classId, Long knowledgeNodeId, String subject,
            LocalDate startDate, LocalDate endDate) {
        // 获取班级学生
        List<Student> classStudents = studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId));
        if (classStudents.isEmpty()) return List.of();
        List<Long> studentIds = classStudents.stream().map(Student::getId).toList();

        // 查询进度数据
        var qw = new LambdaQueryWrapper<PrecisionProgress>().in(PrecisionProgress::getStudentId, studentIds);
        if (knowledgeNodeId != null) qw.eq(PrecisionProgress::getNodeId, knowledgeNodeId);
        if (startDate != null) qw.ge(PrecisionProgress::getUpdateTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(PrecisionProgress::getUpdateTime, endDate.plusDays(1).atStartOfDay());
        qw.orderByAsc(PrecisionProgress::getUpdateTime);
        List<PrecisionProgress> records = progressMapper.selectList(qw);
        if (records.isEmpty()) return List.of();

        // 按周聚合
        DateTimeFormatter weekFmt = DateTimeFormatter.ofPattern("yyyy-'W'ww", java.util.Locale.ENGLISH);
        Map<String, List<BigDecimal>> weekData = new LinkedHashMap<>();
        for (PrecisionProgress p : records) {
            if (p.getUpdateTime() == null || p.getMasteryPercent() == null) continue;
            String week = p.getUpdateTime().format(weekFmt);
            weekData.computeIfAbsent(week, k -> new ArrayList<>()).add(p.getMasteryPercent());
        }

        // 每周平均
        List<Map<String, Object>> result = new ArrayList<>();
        for (var e : weekData.entrySet()) {
            double avg = ScoreUtils.avg(e.getValue());
            result.add(Map.of("week", e.getKey(), "masteryPercent", Math.round(avg * 10) / 10.0));
        }
        return result;
    }

    /**
     * 获取系统最早活动日期（从 task 或 submission 的最小创建时间推断）
     */
    private java.time.LocalDate getFirstActivityDate() {
        try {
            // 尝试从最早的任务创建时间获取
            Task earliestTask = taskMapper.selectOne(
                new LambdaQueryWrapper<Task>()
                    .orderByAsc(Task::getCreatedAt)
                    .last("LIMIT 1"));
            if (earliestTask != null && earliestTask.getCreatedAt() != null) {
                return earliestTask.getCreatedAt().toLocalDate();
            }
        } catch (Exception e) {
            log.debug("无法从任务推断起始日期: {}", e.getMessage());
        }
        try {
            // 回退到最早的提交时间
            TaskSubmission earliestSub = submissionMapper.selectOne(
                new LambdaQueryWrapper<TaskSubmission>()
                    .orderByAsc(TaskSubmission::getSubmittedAt)
                    .last("LIMIT 1"));
            if (earliestSub != null && earliestSub.getSubmittedAt() != null) {
                return earliestSub.getSubmittedAt().toLocalDate();
            }
        } catch (Exception e) {
            log.debug("无法从提交推断起始日期: {}", e.getMessage());
        }
        // 最后回退：取当前日期前30天
        return java.time.LocalDate.now().minusDays(30);
    }
}
