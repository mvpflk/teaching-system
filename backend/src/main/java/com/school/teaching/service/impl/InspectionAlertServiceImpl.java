package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.InspectionAlertService;
import com.school.teaching.utils.ScoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InspectionAlertServiceImpl implements InspectionAlertService {

    private static final Logger log = LoggerFactory.getLogger(InspectionAlertServiceImpl.class);

    @Autowired private InspectionAlertRuleMapper alertRuleMapper;
    @Autowired private InspectionAlertLogMapper alertLogMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private TaskSubmissionMapper taskSubmissionMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private CreditTransactionMapper creditTransactionMapper;
    @Autowired private PeerReviewMapper peerReviewMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private NotificationMapper notificationMapper;
    @Autowired private TeachingResearchActivityMapper teachingResearchActivityMapper;
    @Autowired private LessonPrepRecordMapper lessonPrepRecordMapper;
    @Autowired private LessonPrepGroupMapper lessonPrepGroupMapper;
    @Autowired private TeachingGroupMapper teachingGroupMapper;
    @Autowired private GroupMemberMapper groupMemberMapper;

    @Override
    public void checkAll() {
        List<InspectionAlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<InspectionAlertRule>().eq(InspectionAlertRule::getEnabled, 1));
        if (rules.isEmpty()) return;

        int anomalyCount = 0;
        for (InspectionAlertRule rule : rules) {
            try {
                switch (rule.getRuleType()) {
                    case "SCORE_AVG_DROP": anomalyCount += checkScoreAvgDrop(rule); break;
                    case "SUBMIT_RATE_LOW": anomalyCount += checkSubmitRateLow(rule); break;
                    case "PASS_RATE_LOW": anomalyCount += checkPassRateLow(rule); break;
                    case "CREDIT_ANOMALY": anomalyCount += checkCreditAnomaly(rule); break;
                    case "PEER_STDDEV_HIGH": anomalyCount += checkPeerStddevHigh(rule); break;
                    case "TEACHER_GRADING_BACKLOG": anomalyCount += checkGradingBacklog(rule); break;
                    case "TEACHER_INACTIVE": anomalyCount += checkTeacherInactive(rule); break;
                    case "GROUP_REVIEW_BACKLOG": anomalyCount += checkGroupReviewBacklog(rule); break;
                    case "TEACHING_RESEARCH_INACTIVE": anomalyCount += checkTeachingResearchInactive(rule); break;
                    case "LESSON_PREP_INACTIVE": anomalyCount += checkLessonPrepInactive(rule); break;
                    default: log.warn("未知预警规则类型: {}", rule.getRuleType());
                }
            } catch (Exception e) {
                log.error("预警规则检测失败: ruleId={}, ruleType={}", rule.getId(), rule.getRuleType(), e);
            }
        }
        log.info("预警检测完成，发现{}条异常", anomalyCount);
    }

    private int checkScoreAvgDrop(InspectionAlertRule rule) {
        Map<Long, Long> studentClassMap = getStudentClassMap();
        Map<Long, Classes> classMap = getClassMap();
        LocalDate[] weeks = getWeekRange(rule.getTimeWindow());
        String curLabel = weeks[0].toString();
        String preLabel = weeks[2].toString();

        Map<Long, Double> curAvgs = calcClassAvgScores(weeks[0], weeks[1]);
        Map<Long, Double> preAvgs = calcClassAvgScores(weeks[2], weeks[3]);

        int count = 0;
        for (Long cid : curAvgs.keySet()) {
            double cur = curAvgs.get(cid);
            double pre = preAvgs.getOrDefault(cid, cur);
            double drop = pre - cur;
            if (drop > rule.getThreshold().doubleValue()) {
                Classes cls = classMap.get(cid);
                String msg = String.format("%s班级本周均分%.1f分，较上周均分%.1f分下降%.1f分，触发预警",
                        cls != null ? cls.getClassName() : cid, cur, pre, drop);
                createAlert(rule, msg, cid, null, BigDecimal.valueOf(cur), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkSubmitRateLow(InspectionAlertRule rule) {
        Map<Long, Long> studentClassMap = getStudentClassMap();
        Map<Long, Classes> classMap = getClassMap();

        Map<Long, Long> classStudentCount = studentMapper.selectList(null).stream()
                .filter(s -> s.getClassId() != null)
                .collect(Collectors.groupingBy(Student::getClassId, Collectors.counting()));

        Map<Long, Long> classSubmissionCount = getClassSubmissionCount();
        int count = 0;
        for (Long cid : classStudentCount.keySet()) {
            long total = classStudentCount.get(cid);
            long submitted = classSubmissionCount.getOrDefault(cid, 0L);
            if (total == 0) continue;
            double rate = submitted * 100.0 / total;
            if (rate < rule.getThreshold().doubleValue()) {
                Classes cls = classMap.get(cid);
                String msg = String.format("%s班级作业提交率%.1f%%，低于阈值%s%%，触发预警",
                        cls != null ? cls.getClassName() : cid, rate, rule.getThreshold().stripTrailingZeros().toPlainString());
                createAlert(rule, msg, cid, null, BigDecimal.valueOf(rate), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkPassRateLow(InspectionAlertRule rule) {
        Map<Long, Long> studentClassMap = getStudentClassMap();
        Map<Long, Classes> classMap = getClassMap();
        Map<Long, Classes> currentClassMap = classMap;
        Map<Long, Long> currentStudentClassMap = studentClassMap;

        LocalDate[] range = getWeekRange(rule.getTimeWindow());
        List<TaskSubmission> gradedSubs = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getStatus, "GRADED")
                        .isNotNull(TaskSubmission::getScore)
                        .ge(TaskSubmission::getGradedAt, range[0].atStartOfDay())
                        .lt(TaskSubmission::getGradedAt, range[1].plusDays(1).atStartOfDay()));

        Map<Long, List<TaskSubmission>> byClass = new HashMap<>();
        for (TaskSubmission s : gradedSubs) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid == null) continue;
            byClass.computeIfAbsent(cid, k -> new ArrayList<>()).add(s);
        }

        int count = 0;
        for (Map.Entry<Long, List<TaskSubmission>> e : byClass.entrySet()) {
            List<TaskSubmission> subs = e.getValue();
            long pass = subs.stream().filter(s -> s.getScore().doubleValue() >= 60).count();
            double rate = pass * 100.0 / subs.size();
            if (rate < rule.getThreshold().doubleValue()) {
                Classes cls = classMap.get(e.getKey());
                String msg = String.format("%s班级及格率%.1f%%，低于阈值%s%%，触发预警",
                        cls != null ? cls.getClassName() : e.getKey(), rate,
                        rule.getThreshold().stripTrailingZeros().toPlainString());
                createAlert(rule, msg, e.getKey(), null, BigDecimal.valueOf(rate), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkCreditAnomaly(InspectionAlertRule rule) {
        double threshold = rule.getThreshold().doubleValue();
        List<CreditTransaction> txns = creditTransactionMapper.selectList(
                new LambdaQueryWrapper<CreditTransaction>()
                        .eq(CreditTransaction::getTransactionType, "earn")
                        .ge(CreditTransaction::getCreateTime, LocalDate.now().atStartOfDay()));

        Map<String, List<CreditTransaction>> byDay = txns.stream()
                .filter(t -> t.getCreateTime() != null)
                .collect(Collectors.groupingBy(t -> t.getCreateTime().toLocalDate().toString()));

        int count = 0;
        for (Map.Entry<String, List<CreditTransaction>> e : byDay.entrySet()) {
            long dayTotal = e.getValue().stream()
                    .mapToLong(t -> t.getCreditAmount() != null ? t.getCreditAmount() : 0).sum();
            if (dayTotal > threshold) {
                String msg = String.format("%s日积分发放%d，超过阈值%s，触发预警",
                        e.getKey(), dayTotal, rule.getThreshold().stripTrailingZeros().toPlainString());
                createAlert(rule, msg, null, null, BigDecimal.valueOf(dayTotal), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkPeerStddevHigh(InspectionAlertRule rule) {
        Map<Long, Long> studentClassMap = getStudentClassMap();
        Map<Long, Classes> classMap = getClassMap();
        double threshold = rule.getThreshold().doubleValue();

        List<PeerReview> all = peerReviewMapper.selectList(
                new LambdaQueryWrapper<PeerReview>().isNotNull(PeerReview::getSubmittedAt));
        Map<Long, List<Double>> scoresByClass = new HashMap<>();
        for (PeerReview pr : all) {
            Long cid = studentClassMap.get(pr.getReviewerId());
            if (cid == null) continue;
            if (pr.getScoreJson() == null) continue;
            try {
                Map<String, Object> sj = com.school.teaching.utils.JsonUtils.parseMap(pr.getScoreJson());
                if (sj != null) {
                    for (Object v : sj.values()) {
                        if (v instanceof Number) {
                            scoresByClass.computeIfAbsent(cid, k -> new ArrayList<>())
                                    .add(((Number) v).doubleValue());
                        }
                    }
                }
            } catch (Exception ignored) { log.warn("成绩JSON解析失败: {}", ignored.getMessage()); }
        }

        int count = 0;
        for (Map.Entry<Long, List<Double>> e : scoresByClass.entrySet()) {
            List<Double> scores = e.getValue();
            if (scores.size() < 2) continue;
            double mean = ScoreUtils.avgDouble(scores);
            double std = ScoreUtils.stdDev(scores);
            if (std > threshold) {
                Classes cls = classMap.get(e.getKey());
                String msg = String.format("%s班级互评标准差%.1f，超过阈值%s，触发预警",
                        cls != null ? cls.getClassName() : e.getKey(), std,
                        rule.getThreshold().stripTrailingZeros().toPlainString());
                createAlert(rule, msg, e.getKey(), null, BigDecimal.valueOf(std), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkGradingBacklog(InspectionAlertRule rule) {
        double threshold = rule.getThreshold().doubleValue();
        List<Teacher> teachers = teacherMapper.selectList(null);
        Map<Long, User> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .in(Task::getTeacherId, teachers.stream().map(Teacher::getId).collect(Collectors.toSet())));
        Set<Long> taskIds = tasks.stream().map(Task::getId).collect(Collectors.toSet());
        if (taskIds.isEmpty()) return 0;

        List<TaskSubmission> pending = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .in(TaskSubmission::getTaskId, taskIds)
                        .eq(TaskSubmission::getStatus, "SUBMITTED"));
        Map<Long, Long> taskTeacherMap = tasks.stream()
                .collect(Collectors.toMap(Task::getId, Task::getTeacherId, (a, b) -> a));
        Map<Long, Long> pendingByTeacher = new HashMap<>();
        for (TaskSubmission s : pending) {
            Long tid = taskTeacherMap.get(s.getTaskId());
            if (tid != null) pendingByTeacher.merge(tid, 1L, Long::sum);
        }

        int count = 0;
        for (Map.Entry<Long, Long> e : pendingByTeacher.entrySet()) {
            if (e.getValue() > threshold) {
                Teacher t = teachers.stream().filter(tc -> tc.getId().equals(e.getKey())).findFirst().orElse(null);
                String name = t != null && userMap.get(t.getUserId()) != null ?
                        userMap.get(t.getUserId()).getRealName() : String.valueOf(e.getKey());
                String msg = String.format("教师%s待批改%d份，超过阈值%s，触发预警",
                        name, e.getValue(), rule.getThreshold().stripTrailingZeros().toPlainString());
                createAlert(rule, msg, null, e.getKey(), BigDecimal.valueOf(e.getValue()), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkTeacherInactive(InspectionAlertRule rule) {
        double threshold = rule.getThreshold().doubleValue();
        List<Teacher> teachers = teacherMapper.selectList(null);
        Map<Long, User> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        LocalDate cutoff = LocalDate.now().minusDays((int) threshold);

        int count = 0;
        for (Teacher t : teachers) {
            long recentTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                    .eq(Task::getTeacherId, t.getId())
                    .ge(Task::getCreatedAt, cutoff.atStartOfDay()));
            if (recentTasks > 0) continue;

            List<Task> tTasks = taskMapper.selectList(
                    new LambdaQueryWrapper<Task>().eq(Task::getTeacherId, t.getId()));
            if (!tTasks.isEmpty()) {
                Set<Long> tids = tTasks.stream().map(Task::getId).collect(Collectors.toSet());
                long recentGrading = taskSubmissionMapper.selectCount(new LambdaQueryWrapper<TaskSubmission>()
                        .in(TaskSubmission::getTaskId, tids)
                        .ge(TaskSubmission::getGradedAt, cutoff.atStartOfDay())
                        .isNotNull(TaskSubmission::getGradedAt));
                if (recentGrading > 0) continue;
            }

            if (recentTasks == 0) {
                User u = userMap.get(t.getUserId());
                String name = u != null ? u.getRealName() : String.valueOf(t.getId());
                String msg = String.format("教师%s已连续%.0f天无教学活动，触发预警", name, threshold);
                createAlert(rule, msg, null, t.getId(), BigDecimal.valueOf(threshold), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkGroupReviewBacklog(InspectionAlertRule rule) {
        double threshold = rule.getThreshold().doubleValue();
        int hours = (int) threshold;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        List<Task> backlogTasks = taskMapper.selectList(
            new LambdaQueryWrapper<Task>()
                .eq(Task::getReviewStatus, "PENDING_GROUP")
                .lt(Task::getUpdatedAt, cutoff));

        int count = 0;
        for (Task task : backlogTasks) {
            String msg = String.format("任务「%s」已提交超过%d小时，备课组长尚未审核",
                task.getTitle() != null ? task.getTitle() : "未命名", hours);
            createAlert(rule, msg, null, task.getTeacherId(), BigDecimal.valueOf(hours), rule.getThreshold());
            count++;
        }
        return count;
    }

    private int checkTeachingResearchInactive(InspectionAlertRule rule) {
        double threshold = rule.getThreshold().doubleValue();
        long days = (long) threshold;
        LocalDate cutoff = LocalDate.now().minusDays(days);

        List<TeachingGroup> groups = teachingGroupMapper.selectList(null);
        Map<Long, TeachingResearchActivity> latestActivity = new HashMap<>();
        List<TeachingResearchActivity> allActivities = teachingResearchActivityMapper.selectList(null);
        for (TeachingResearchActivity a : allActivities) {
            if (a.getActivityDate() == null) continue;
            TeachingResearchActivity prev = latestActivity.get(a.getTeachingGroupId());
            if (prev == null || a.getActivityDate().isAfter(prev.getActivityDate())) {
                latestActivity.put(a.getTeachingGroupId(), a);
            }
        }

        int count = 0;
        for (TeachingGroup g : groups) {
            TeachingResearchActivity last = latestActivity.get(g.getId());
            if (last == null) {
                String msg = String.format("教研组「%s」从未开展教研活动，已超过%.0f天无教研活动",
                    g.getName() != null ? g.getName() : "未命名", threshold);
                Long leaderId = findGroupLeader("TEACHING", g.getId());
                createAlert(rule, msg, null, leaderId, BigDecimal.valueOf(days), rule.getThreshold());
                count++;
                continue;
            }
            long inactiveDays = ChronoUnit.DAYS.between(last.getActivityDate(), LocalDate.now());
            if (inactiveDays > days) {
                String msg = String.format("教研组「%s」已超过%.0f天无教研活动（最近活动：%s）",
                    g.getName() != null ? g.getName() : "未命名", threshold, last.getActivityDate());
                Long leaderId = findGroupLeader("TEACHING", g.getId());
                createAlert(rule, msg, null, leaderId, BigDecimal.valueOf(inactiveDays), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private int checkLessonPrepInactive(InspectionAlertRule rule) {
        double threshold = rule.getThreshold().doubleValue();
        long days = (long) threshold;
        LocalDate cutoff = LocalDate.now().minusDays(days);

        List<LessonPrepGroup> groups = lessonPrepGroupMapper.selectList(null);
        Map<Long, LessonPrepRecord> latestRecord = new HashMap<>();
        List<LessonPrepRecord> allRecords = lessonPrepRecordMapper.selectList(null);
        for (LessonPrepRecord r : allRecords) {
            if (r.getRecordDate() == null) continue;
            LessonPrepRecord prev = latestRecord.get(r.getLessonPrepGroupId());
            if (prev == null || r.getRecordDate().isAfter(prev.getRecordDate())) {
                latestRecord.put(r.getLessonPrepGroupId(), r);
            }
        }

        int count = 0;
        for (LessonPrepGroup g : groups) {
            LessonPrepRecord last = latestRecord.get(g.getId());
            if (last == null) {
                String msg = String.format("备课组「%s」从未开展备课活动，已超过%.0f天无备课活动",
                    g.getName() != null ? g.getName() : "未命名", threshold);
                Long leaderId = findGroupLeader("LESSON_PREP", g.getId());
                createAlert(rule, msg, null, leaderId, BigDecimal.valueOf(days), rule.getThreshold());
                count++;
                continue;
            }
            long inactiveDays = ChronoUnit.DAYS.between(last.getRecordDate(), LocalDate.now());
            if (inactiveDays > days) {
                String msg = String.format("备课组「%s」已超过%.0f天无备课活动（最近记录：%s）",
                    g.getName() != null ? g.getName() : "未命名", threshold, last.getRecordDate());
                Long leaderId = findGroupLeader("LESSON_PREP", g.getId());
                createAlert(rule, msg, null, leaderId, BigDecimal.valueOf(inactiveDays), rule.getThreshold());
                count++;
            }
        }
        return count;
    }

    private Long findGroupLeader(String groupType, Long groupId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<GroupMember> qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("group_type", groupType).eq("group_id", groupId).eq("role", "LEADER").last("LIMIT 1");
        GroupMember gm = groupMemberMapper.selectOne(qw);
        return gm != null ? gm.getTeacherId() : null;
    }

    private void createAlert(InspectionAlertRule rule, String message, Long classId, Long teacherId,
                              BigDecimal metricValue, BigDecimal threshold) {
        InspectionAlertLog logEntry = new InspectionAlertLog();
        logEntry.setRuleId(rule.getId());
        logEntry.setRuleName(rule.getRuleName());
        logEntry.setAlertMessage(message);
        logEntry.setTargetClassId(classId);
        logEntry.setTargetTeacherId(teacherId);
        logEntry.setMetricValue(metricValue);
        logEntry.setThreshold(threshold);
        logEntry.setIsRead(0);
        logEntry.setTriggedAt(LocalDateTime.now());
        alertLogMapper.insert(logEntry);

        if (rule.getNotifyInspector() != null && rule.getNotifyInspector() == 1) {
            List<User> inspectors = userMapper.selectList(
                    new LambdaQueryWrapper<User>().eq(User::getRoleName, "INSPECTOR"));
            for (User u : inspectors) {
                Notification n = new Notification();
                n.setUserId(u.getId());
                n.setTitle("预警通知：" + rule.getRuleName());
                n.setContent(message);
                n.setType("INSPECTOR_ALERT");
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(n);
            }
        }
        if (rule.getNotifyTeacher() != null && rule.getNotifyTeacher() == 1 && teacherId != null) {
            Teacher t = teacherMapper.selectById(teacherId);
            if (t != null) {
                Notification n = new Notification();
                n.setUserId(t.getUserId());
                n.setTitle("预警通知：" + rule.getRuleName());
                n.setContent(message);
                n.setType("TEACHER_ALERT");
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(n);
            }
        }
    }

    private LocalDate[] getWeekRange(String timeWindow) {
        LocalDate today = LocalDate.now();
        LocalDate curMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate curSun = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate preMon = curMon.minusDays(7);
        LocalDate preSun = curSun.minusDays(7);

        if ("LAST_WEEK".equals(timeWindow)) return new LocalDate[]{preMon, preSun, preMon.minusDays(7), preSun.minusDays(7)};
        if ("CURRENT_MONTH".equals(timeWindow)) {
            LocalDate curMonthStart = today.withDayOfMonth(1);
            LocalDate curMonthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
            LocalDate preMonthStart = curMonthStart.minusMonths(1);
            LocalDate preMonthEnd = curMonthStart.minusDays(1);
            return new LocalDate[]{curMonthStart, curMonthEnd, preMonthStart, preMonthEnd};
        }
        if ("LAST_MONTH".equals(timeWindow)) {
            LocalDate preMonthStart = today.minusMonths(1).withDayOfMonth(1);
            LocalDate preMonthEnd = today.withDayOfMonth(1).minusDays(1);
            LocalDate pre2Start = preMonthStart.minusMonths(1);
            LocalDate pre2End = preMonthStart.minusDays(1);
            return new LocalDate[]{preMonthStart, preMonthEnd, pre2Start, pre2End};
        }
        return new LocalDate[]{curMon, curSun, preMon, preSun};
    }

    private Map<Long, Long> getStudentClassMap() {
        return studentMapper.selectList(null).stream()
                .filter(s -> s.getClassId() != null)
                .collect(Collectors.toMap(Student::getId, Student::getClassId, (a, b) -> a));
    }

    private Map<Long, Classes> getClassMap() {
        return classesMapper.selectList(null).stream()
                .collect(Collectors.toMap(Classes::getId, c -> c));
    }

    private Map<Long, Double> calcClassAvgScores(LocalDate start, LocalDate end) {
        Map<Long, Long> studentClassMap = getStudentClassMap();
        List<TaskSubmission> graded = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getStatus, "GRADED")
                        .isNotNull(TaskSubmission::getScore)
                        .ge(TaskSubmission::getGradedAt, start.atStartOfDay())
                        .lt(TaskSubmission::getGradedAt, end.plusDays(1).atStartOfDay()));

        Map<Long, List<Double>> scoresByClass = new HashMap<>();
        for (TaskSubmission s : graded) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid != null) {
                scoresByClass.computeIfAbsent(cid, k -> new ArrayList<>())
                        .add(s.getScore().doubleValue());
            }
        }
        Map<Long, Double> result = new HashMap<>();
        for (Map.Entry<Long, List<Double>> e : scoresByClass.entrySet()) {
            double avg = ScoreUtils.avgDouble(e.getValue());
            result.put(e.getKey(), Math.round(avg * 10) / 10.0);
        }
        return result;
    }

    private Map<Long, Long> getClassSubmissionCount() {
        Map<Long, Long> studentClassMap = getStudentClassMap();
        List<TaskSubmission> subs = taskSubmissionMapper.selectList(null);
        Map<Long, Long> result = new HashMap<>();
        for (TaskSubmission s : subs) {
            Long cid = studentClassMap.get(s.getStudentId());
            if (cid != null) result.merge(cid, 1L, Long::sum);
        }
        return result;
    }

    @Override
    public List<InspectionAlertRule> getRules() {
        return alertRuleMapper.selectList(null);
    }

    @Override
    public InspectionAlertRule updateRule(Long id, InspectionAlertRule rule) {
        InspectionAlertRule existing = alertRuleMapper.selectById(id);
        if (existing == null) throw new BusinessException(404, "规则不存在");
        if (rule.getRuleName() != null) existing.setRuleName(rule.getRuleName());
        if (rule.getThreshold() != null) existing.setThreshold(rule.getThreshold());
        if (rule.getComparison() != null) existing.setComparison(rule.getComparison());
        if (rule.getEnabled() != null) existing.setEnabled(rule.getEnabled());
        if (rule.getNotifyInspector() != null) existing.setNotifyInspector(rule.getNotifyInspector());
        if (rule.getNotifyTeacher() != null) existing.setNotifyTeacher(rule.getNotifyTeacher());
        if (rule.getTimeWindow() != null) existing.setTimeWindow(rule.getTimeWindow());
        alertRuleMapper.updateById(existing);
        return existing;
    }

    @Override
    public IPage<InspectionAlertLog> getLogs(int page, int size, Boolean isRead) {
        LambdaQueryWrapper<InspectionAlertLog> w = new LambdaQueryWrapper<>();
        if (isRead != null) w.eq(InspectionAlertLog::getIsRead, isRead ? 1 : 0);
        w.orderByDesc(InspectionAlertLog::getTriggedAt);
        return alertLogMapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public int markAsRead(Long id) {
        InspectionAlertLog logEntry = alertLogMapper.selectById(id);
        if (logEntry == null) return 0;
        logEntry.setIsRead(1);
        return alertLogMapper.updateById(logEntry);
    }

    @Override
    public int markAllAsRead() {
        List<InspectionAlertLog> unread = alertLogMapper.selectList(
                new LambdaQueryWrapper<InspectionAlertLog>().eq(InspectionAlertLog::getIsRead, 0));
        for (InspectionAlertLog logEntry : unread) {
            logEntry.setIsRead(1);
            alertLogMapper.updateById(logEntry);
        }
        return unread.size();
    }
}
