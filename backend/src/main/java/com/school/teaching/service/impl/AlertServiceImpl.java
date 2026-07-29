package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.common.NotificationType;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.common.EncodingUtils;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.AlertService;
import com.school.teaching.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    @Autowired private AlertRuleMapper ruleMapper;
    @Autowired private AlertRecordMapper recordMapper;
    @Autowired private AlertLastScanMapper scanMapper;
    @Autowired private TaskSubmissionMapper submissionMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ClassesMapper classesMapper;
    @Autowired private ParentChildRelationMapper relationMapper;
    @Autowired private NotificationService notificationService;

    // ──────────────── 规则管理 ────────────────

    @Override
    public List<AlertRule> getEnabledRules() {
        List<AlertRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getIsEnabled, 1));
        for (AlertRule r : rules) {
            r.setName(EncodingUtils.fix(r.getName()));
            r.setDescription(EncodingUtils.fix(r.getDescription()));
        }
        return rules;
    }

    @Override
    public List<AlertRule> getRules(Long teacherUserId) {
        List<AlertRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .and(w -> w.eq(AlertRule::getIsBuiltin, 1)
                        .or(w2 -> w2.eq(AlertRule::getCreatedBy, teacherUserId))));
        for (AlertRule r : rules) {
            r.setName(EncodingUtils.fix(r.getName()));
            r.setDescription(EncodingUtils.fix(r.getDescription()));
        }
        return rules;
    }

    @Override
    @Transactional
    public AlertRule saveRule(AlertRule rule, Long operatorUserId) {
        if (rule.getId() == null) {
            rule.setIsBuiltin(0);
            rule.setCreatedBy(operatorUserId);
            ruleMapper.insert(rule);
        } else {
            AlertRule existing = ruleMapper.selectById(rule.getId());
            if (existing == null) throw new BusinessException(404, "规则不存在");
            if (existing.getIsBuiltin() == 1) {
                // 内置规则仅允许修改参数
                existing.setMinConsecutive(rule.getMinConsecutive());
                existing.setScoreThreshold(rule.getScoreThreshold());
                existing.setCooldownDays(rule.getCooldownDays());
                existing.setIsEnabled(rule.getIsEnabled());
                existing.setDaysLookback(rule.getDaysLookback());
                existing.setTaskTypes(rule.getTaskTypes());
                ruleMapper.updateById(existing);
                rule = existing;
            } else {
                if (!Objects.equals(existing.getCreatedBy(), operatorUserId))
                    throw new BusinessException(403, "只能修改自己创建的规则");
                ruleMapper.updateById(rule);
            }
        }
        return ruleMapper.selectById(rule.getId());
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId, Long operatorUserId) {
        AlertRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) return;
        if (rule.getIsBuiltin() == 1) throw new BusinessException(403, "内置规则不可删除");
        if (!Objects.equals(rule.getCreatedBy(), operatorUserId))
            throw new BusinessException(403, "只能删除自己创建的规则");
        ruleMapper.deleteById(ruleId);
    }

    // ──────────────── 预警查询 ────────────────

    @Override
    public Map<String, Object> getAlertRecords(Long teacherUserId, Long classId, String alertType,
                                                String handledStatus, String studentName, int page, int pageSize) {
        // F7: 管理员可查看全校预警
        boolean isAdmin = SecurityUtils.isAdmin();
        Set<Long> classIds;
        if (isAdmin) {
            List<Classes> allClasses = classesMapper.selectList(null);
            classIds = allClasses.stream().map(Classes::getId).collect(Collectors.toSet());
        } else {
            List<Classes> teacherClasses = classesMapper.selectList(
                    new LambdaQueryWrapper<Classes>()
                            .eq(Classes::getHeadTeacherId, teacherUserId));
            classIds = teacherClasses.stream().map(Classes::getId).collect(Collectors.toSet());
        }

        LambdaQueryWrapper<AlertRecord> w = new LambdaQueryWrapper<>();
        if (classId != null && classIds.contains(classId)) {
            w.eq(AlertRecord::getClassId, classId);
        } else if (!classIds.isEmpty()) {
            w.in(AlertRecord::getClassId, classIds);
        } else {
            return Map.of("records", List.of(), "total", 0L, "summary", Map.of("unread",0,"lowScore",0,"missing",0,"contacted",0));
        }
        if (alertType != null && !alertType.isEmpty()) {
            List<AlertRule> matchedRules = ruleMapper.selectList(
                    new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getAlertType, alertType));
            if (!matchedRules.isEmpty()) {
                w.in(AlertRecord::getRuleId, matchedRules.stream()
                        .map(AlertRule::getId).collect(Collectors.toList()));
            } else {
                return Map.of("records", List.of(), "total", 0L, "summary", Map.of("unread",0,"lowScore",0,"missing",0,"contacted",0));
            }
        }
        if (handledStatus != null && !handledStatus.isEmpty()) {
            w.eq(AlertRecord::getHandledStatus, handledStatus);
        } else {
            w.ne(AlertRecord::getHandledStatus, "IGNORED");
        }
        // F10: 学生姓名模糊搜索 — 先收集姓名匹配的学生ID
        Set<Long> nameFilteredStudentIds = null;
        if (studentName != null && !studentName.isEmpty()) {
            List<User> matchedUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>().like(User::getRealName, studentName));
            Set<Long> matchedUserIds = matchedUsers.stream().map(User::getId).collect(Collectors.toSet());
            if (!matchedUserIds.isEmpty()) {
                List<Student> matchedStudents = studentMapper.selectList(
                        new LambdaQueryWrapper<Student>().in(Student::getUserId, matchedUserIds));
                nameFilteredStudentIds = matchedStudents.stream().map(Student::getId).collect(Collectors.toSet());
            }
            if (nameFilteredStudentIds == null || nameFilteredStudentIds.isEmpty()) {
                return Map.of("records", List.of(), "total", 0L, "summary", Map.of("unread",0,"lowScore",0,"missing",0,"contacted",0));
            }
            w.in(AlertRecord::getStudentId, nameFilteredStudentIds);
        }
        w.orderByDesc(AlertRecord::getCreateTime);

        int pg = page > 0 ? page : 1, ps = pageSize > 0 ? pageSize : 20;
        Page<AlertRecord> p = new Page<>(pg, ps);
        p = recordMapper.selectPage(p, w);
        long total = p.getTotal();
        List<AlertRecord> records = p.getRecords();

        // 批量加载关联数据
        Set<Long> studentIds = records.stream().map(AlertRecord::getStudentId).collect(Collectors.toSet());
        Set<Long> ruleIds = records.stream().map(AlertRecord::getRuleId).collect(Collectors.toSet());
        Map<Long, Student> sMap = studentIds.isEmpty() ? Map.of() :
                studentMapper.selectBatchIds(studentIds).stream().collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, User> uMap = studentIds.isEmpty() ? Map.of() : loadUsersByStudentIds(studentIds);
        Map<Long, AlertRule> rMap = ruleIds.isEmpty() ? Map.of() :
                ruleMapper.selectBatchIds(ruleIds).stream().collect(Collectors.toMap(AlertRule::getId, r -> r));

        List<Map<String, Object>> items = new ArrayList<>();
        for (AlertRecord rec : records) {
            Student s = sMap.get(rec.getStudentId());
            User u = uMap.get(s != null ? s.getUserId() : null);
            AlertRule r = rMap.get(rec.getRuleId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rec.getId());
            item.put("studentId", rec.getStudentId());
            item.put("studentName", u != null ? u.getRealName() : "?");
            item.put("className", s != null ? getClassName(s.getClassId()) : null);
            item.put("classId", rec.getClassId());
            item.put("ruleName", r != null ? EncodingUtils.fix(r.getName()) : "?");
            item.put("alertType", r != null ? r.getAlertType() : null);
            item.put("minConsecutive", r != null ? r.getMinConsecutive() : null);
            item.put("scoreThreshold", r != null ? r.getScoreThreshold() : null);
            item.put("alertSummary", rec.getAlertSummary());
            item.put("handledStatus", rec.getHandledStatus());
            item.put("notifiedTeacher", rec.getNotifiedTeacher());
            item.put("notifiedParents", rec.getNotifiedParents());
            item.put("createTime", rec.getCreateTime());
            items.add(item);
        }
        // F4: 全量摘要统计（不受分页限制）
        LambdaQueryWrapper<AlertRecord> summaryW = new LambdaQueryWrapper<>();
        if (!classIds.isEmpty()) summaryW.in(AlertRecord::getClassId, classIds);
        if (nameFilteredStudentIds != null) summaryW.in(AlertRecord::getStudentId, nameFilteredStudentIds);
        List<AlertRecord> allForSummary = recordMapper.selectList(summaryW);
        long summaryUnread = allForSummary.stream().filter(r -> "UNREAD".equals(r.getHandledStatus())).count();
        long summaryContacted = allForSummary.stream().filter(r -> "CONTACTED".equals(r.getHandledStatus())).count();
        // Low score / missing need rule lookup
        Set<Long> allRuleIds = allForSummary.stream().map(AlertRecord::getRuleId).collect(Collectors.toSet());
        Map<Long, AlertRule> allRMap = allRuleIds.isEmpty() ? Map.of() :
                ruleMapper.selectBatchIds(allRuleIds).stream().collect(Collectors.toMap(AlertRule::getId, r -> r));
        long summaryLowScore = allForSummary.stream()
                .filter(r -> { AlertRule rule = allRMap.get(r.getRuleId()); return rule != null && "LOW_SCORE".equals(rule.getAlertType()); }).count();
        long summaryMissing = allForSummary.stream()
                .filter(r -> { AlertRule rule = allRMap.get(r.getRuleId()); return rule != null && "MISSING".equals(rule.getAlertType()); }).count();
        Map<String, Object> summary = Map.of("unread", summaryUnread, "lowScore", summaryLowScore,
                "missing", summaryMissing, "contacted", summaryContacted);

        return Map.of("records", items, "total", total, "page", pg, "pageSize", ps, "summary", summary);
    }

    @Override
    @Transactional
    public void handleAlert(Long recordId, String status, Long operatorUserId) {
        AlertRecord rec = recordMapper.selectById(recordId);
        if (rec == null) return;
        rec.setHandledStatus(status);
        rec.setHandledBy(operatorUserId);
        rec.setHandledAt(LocalDateTime.now());
        recordMapper.updateById(rec);
    }

    // ──────────────── 家长端 ────────────────

    @Override
    public List<Map<String, Object>> getChildAlerts(Long parentUserId) {
        List<ParentChildRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ParentChildRelation>().eq(ParentChildRelation::getParentId, parentUserId));
        if (relations.isEmpty()) return List.of();
        Set<Long> studentIds = relations.stream().map(ParentChildRelation::getStudentId).collect(Collectors.toSet());

        List<AlertRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .orderByDesc(AlertRecord::getCreateTime)
                        .last("LIMIT 10"));
        if (records.isEmpty()) return List.of();

        Set<Long> sids = records.stream().map(AlertRecord::getStudentId).collect(Collectors.toSet());
        Map<Long, User> uMap = loadUsersByStudentIds(sids);
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(sids).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, AlertRule> rMap = ruleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getIsEnabled, 1)).stream()
                .collect(Collectors.toMap(AlertRule::getId, r -> r));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AlertRecord rec : records) {
            Student s = studentMap.get(rec.getStudentId());
            User u = uMap.get(s != null ? s.getUserId() : null);
            AlertRule r = rMap.get(rec.getRuleId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rec.getId());
            item.put("studentId", rec.getStudentId());
            item.put("studentName", u != null ? u.getRealName() : "?");
            item.put("ruleName", r != null ? EncodingUtils.fix(r.getName()) : "?");
            item.put("alertSummary", rec.getAlertSummary());
            item.put("createTime", rec.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public int getChildUnreadAlertCount(Long parentUserId) {
        List<ParentChildRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ParentChildRelation>().eq(ParentChildRelation::getParentId, parentUserId));
        if (relations.isEmpty()) return 0;
        Set<Long> studentIds = relations.stream().map(ParentChildRelation::getStudentId).collect(Collectors.toSet());
        Long c = recordMapper.selectCount(
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .eq(AlertRecord::getHandledStatus, "UNREAD"));
        return c.intValue();
    }

    // ──────────────── 扫描核心逻辑 ────────────────

    @Override
    public int scanAllStudents() {
        return doScan(null, null);
    }

    @Override
    public int scanTeacherClasses(Long teacherUserId) {
        // 获取该教师管辖班级的学生ID
        List<Classes> teacherClasses = classesMapper.selectList(
                new LambdaQueryWrapper<Classes>()
                        .eq(Classes::getHeadTeacherId, teacherUserId));
        if (teacherClasses.isEmpty()) return 0;
        Set<Long> classIds = teacherClasses.stream().map(Classes::getId).collect(Collectors.toSet());
        Set<Long> studentIds = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .in(Student::getClassId, classIds)
                        .eq(Student::getStatus, "active")
                        .select(Student::getId))
                .stream().map(Student::getId).collect(Collectors.toSet());
        if (studentIds.isEmpty()) return 0;
        return doScan(null, studentIds);
    }

    @Override
    public int scanIncremental() {
        // 读取上次增量扫描的 submission_id
        AlertLastScan last = scanMapper.selectOne(
                new LambdaQueryWrapper<AlertLastScan>()
                        .eq(AlertLastScan::getScanType, "INCREMENTAL")
                        .orderByDesc(AlertLastScan::getId)
                        .last("LIMIT 1"));
        Long lastSubId = last != null ? last.getLastSubmissionId() : 0L;
        return doScan(lastSubId, null);
    }

    private int doScan(Long sinceSubmissionId, Set<Long> restrictStudentIds) {
        List<AlertRule> rules = getEnabledRules();
        if (rules.isEmpty()) return 0;

        // 获取需要扫描的学生ID集合
        Set<Long> candidateStudentIds;
        if (restrictStudentIds != null) {
            candidateStudentIds = restrictStudentIds;
        } else if (sinceSubmissionId != null) {
            candidateStudentIds = submissionMapper.selectList(
                    new LambdaQueryWrapper<TaskSubmission>()
                            .gt(TaskSubmission::getId, sinceSubmissionId)
                            .select(TaskSubmission::getStudentId))
                    .stream().map(TaskSubmission::getStudentId).collect(Collectors.toSet());
        } else {
            candidateStudentIds = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>()
                        .eq(Student::getStatus, "active")
                        .select(Student::getId))
                    .stream().map(Student::getId).collect(Collectors.toSet());
        }
        if (candidateStudentIds.isEmpty()) return 0;

        // 获取最新 submission id 用于记录
        Long maxSubId = sinceSubmissionId;

        int totalAlerts = 0;
        int batchSize = 200;
        List<Long> studentList = new ArrayList<>(candidateStudentIds);

        for (int i = 0; i < studentList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, studentList.size());
            for (int j = i; j < end; j++) {
                Long studentId = studentList.get(j);
                for (AlertRule rule : rules) {
                    try {
                        if (checkStudentRule(studentId, rule)) {
                            totalAlerts++;
                            if (totalAlerts >= 2000) { log.warn("[学业预警] 单次扫描达到 {} 条上限，仍有学生未检查", totalAlerts); break; }
                        }
                    } catch (Exception e) {
                        log.debug("[学业预警] 检查学生 {} 规则 {} 失败: {}", studentId, rule.getId(), e.getMessage());
                    }
                }
                if (totalAlerts >= 100) break;
            }
            if (totalAlerts >= 100) break;
            // 批次间释放
            if (end < studentList.size()) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }

        // 记录扫描状态
        AlertLastScan scan = new AlertLastScan();
        scan.setScanType(sinceSubmissionId != null ? "INCREMENTAL" : "FULL");
        scan.setLastScanTime(LocalDateTime.now());
        scan.setScannedCount(studentList.size());
        scan.setAlertCount(totalAlerts);
        scan.setStatus("COMPLETED");
        if (sinceSubmissionId != null) {
            // 更新 max submission id
            TaskSubmission latest = submissionMapper.selectList(
                    new LambdaQueryWrapper<TaskSubmission>()
                            .orderByDesc(TaskSubmission::getId)
                            .select(TaskSubmission::getId)
                            .last("LIMIT 1")).stream().findFirst().orElse(null);
            scan.setLastSubmissionId(latest != null ? latest.getId() : maxSubId);
        }
        scanMapper.insert(scan);
        return totalAlerts;
    }

    /** 检查单个学生是否触发某条规则 */
    private boolean checkStudentRule(Long studentId, AlertRule rule) {
        // 冷却检查
        Long count = recordMapper.selectCount(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getRuleId, rule.getId())
                        .eq(AlertRecord::getStudentId, studentId)
                        .ge(AlertRecord::getCreateTime, LocalDateTime.now().minusDays(rule.getCooldownDays())));
        if (count > 0) return false;

        if ("LOW_SCORE".equals(rule.getAlertType())) {
            return checkLowScore(studentId, rule);
        } else if ("MISSING".equals(rule.getAlertType())) {
            return checkMissing(studentId, rule);
        } else if ("SCORE_DROP".equals(rule.getAlertType())) {
            return checkScoreDrop(studentId, rule);
        }
        return false;
    }

    private boolean checkLowScore(Long studentId, AlertRule rule) {
        int n = rule.getMinConsecutive();
        BigDecimal threshold = rule.getScoreThreshold();
        List<String> taskTypes = parseTaskTypes(rule.getTaskTypes());

        List<TaskSubmission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getStudentId, studentId)
                        .eq(TaskSubmission::getStatus, "GRADED")
                        // 缺考（score=null）不参与"连续不及格"统计，仅计入已评分且有分数的提交
                        .isNotNull(TaskSubmission::getScore)
                        .ge(TaskSubmission::getGradedAt, LocalDateTime.now().minusDays(rule.getDaysLookback()))
                        .orderByDesc(TaskSubmission::getGradedAt));

        if (subs.size() < n) return false;

        // 过滤：只看指定任务类型的提交
        List<TaskSubmission> filtered = new ArrayList<>();
        Set<Long> taskIds = subs.stream().map(TaskSubmission::getTaskId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskIds.isEmpty() ? Map.of() :
                taskMapper.selectBatchIds(taskIds).stream().collect(Collectors.toMap(Task::getId, t -> t));

        for (TaskSubmission sub : subs) {
            Task task = taskMap.get(sub.getTaskId());
            if (task == null) continue;
            if (!taskTypes.isEmpty() && !taskTypes.contains(task.getTaskType())) continue;
            filtered.add(sub);
            if (filtered.size() >= n) break;
        }
        if (filtered.size() < n) return false;

        // 检查最近n条是否全部低于阈值
        List<TaskSubmission> lastN = filtered.subList(0, n);
        for (TaskSubmission sub : lastN) {
            if (sub.getScore() == null || sub.getScore().compareTo(threshold) >= 0) return false;
        }

        // 生成预警记录
        List<Long> matchedIds = lastN.stream().map(TaskSubmission::getId).collect(Collectors.toList());
        String scores = lastN.stream()
                .map(s -> String.valueOf(s.getScore().intValue())).collect(Collectors.joining("→"));
        String summary = "连续" + n + "次得分低于" + threshold.intValue() + "分（" + scores + "）";
        createAlertRecord(rule, studentId, matchedIds, summary);
        return true;
    }

    private boolean checkMissing(Long studentId, AlertRule rule) {
        int n = rule.getMinConsecutive();
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getClassId() == null) return false;
        List<String> taskTypes = parseTaskTypes(rule.getTaskTypes());

        // 获取被检查的任务：班级定向任务 + 全校强制任务，截止时间已过24h且在回溯期内
        LocalDateTime lookbackSince = LocalDateTime.now().minusDays(rule.getDaysLookback());
        LocalDateTime deadlineBefore = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<Task>()
                .and(w -> w.eq(Task::getTargetType, "CLASS")
                        .and(w2 -> w2.eq(Task::getTargetId, student.getClassId())
                            .or(w3 -> w3.eq(Task::getIsForced, 1))))
                .in(Task::getStatus, "PUBLISHED", "ONGOING")
                .isNotNull(Task::getDeadline)
                .lt(Task::getDeadline, deadlineBefore)
                .ge(Task::getDeadline, lookbackSince);
        List<Task> tasks = taskMapper.selectList(taskWrapper);
        if (tasks.isEmpty()) return false;

        tasks.sort((a, b) -> b.getDeadline().compareTo(a.getDeadline()));

        List<Task> consecutiveMissed = new ArrayList<>();
        for (Task task : tasks) {
            if (!taskTypes.isEmpty() && !taskTypes.contains(task.getTaskType())) continue;
            Long subCount = submissionMapper.selectCount(
                    new LambdaQueryWrapper<TaskSubmission>()
                            .eq(TaskSubmission::getTaskId, task.getId())
                            .eq(TaskSubmission::getStudentId, studentId)
                            .ne(TaskSubmission::getStatus, "PENDING"));
            if (subCount == 0) {
                consecutiveMissed.add(task);
                if (consecutiveMissed.size() >= n) break;
            } else {
                consecutiveMissed.clear(); // 中断连续性
            }
        }
        if (consecutiveMissed.size() < n) return false;

        List<Long> matchedIds = consecutiveMissed.stream().map(Task::getId).collect(Collectors.toList());
        String titles = consecutiveMissed.stream().map(Task::getTitle).collect(Collectors.joining("、"));
        String summary = "连续" + n + "次缺交（" + titles + "）";
        createAlertRecord(rule, studentId, matchedIds, summary);
        return true;
    }

    /** F9: 检查成绩骤降 — 最近一次考试比前一次下降超过阈值 */
    private boolean checkScoreDrop(Long studentId, AlertRule rule) {
        BigDecimal dropThreshold = rule.getScoreThreshold() != null ? rule.getScoreThreshold() : new BigDecimal("30");
        int daysLookback = rule.getDaysLookback() > 0 ? rule.getDaysLookback() : 60;

        // 取该学生最近两次考试提交（已评分的）
        List<TaskSubmission> subs = submissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getStudentId, studentId)
                        .eq(TaskSubmission::getStatus, "GRADED")
                        .isNotNull(TaskSubmission::getScore)
                        .ge(TaskSubmission::getGradedAt, LocalDateTime.now().minusDays(daysLookback))
                        .orderByDesc(TaskSubmission::getGradedAt));
        if (subs.size() < 2) return false;

        // 前两条就是最近两次
        BigDecimal latest = subs.get(0).getScore();
        BigDecimal previous = subs.get(1).getScore();
        if (latest == null || previous == null) return false;

        // 下降幅度 = 前次 - 本次
        BigDecimal drop = previous.subtract(latest);
        if (drop.compareTo(dropThreshold) < 0) return false;

        String summary = "成绩骤降：上次" + previous.intValue() + "分 → 本次" + latest.intValue()
                + "分（下降" + drop.intValue() + "分）";
        List<Long> matchedIds = List.of(subs.get(0).getId(), subs.get(1).getId());
        createAlertRecord(rule, studentId, matchedIds, summary);
        return true;
    }

    @Transactional
    private void createAlertRecord(AlertRule rule, Long studentId, List<Long> submissionIds, String summary) {
        Student student = studentMapper.selectById(studentId);
        Long classId = student != null ? student.getClassId() : null;
        User studentUser = student != null ? userMapper.selectById(student.getUserId()) : null;
        String studentName = studentUser != null ? studentUser.getRealName() : "?";

        AlertRecord rec = new AlertRecord();
        rec.setRuleId(rule.getId());
        rec.setStudentId(studentId);
        rec.setClassId(classId);
        rec.setMatchedSubmissionIds(submissionIds.stream()
                .map(String::valueOf).collect(Collectors.joining(",", "[", "]")));
        rec.setAlertSummary(summary);
        rec.setHandledStatus("UNREAD");
        recordMapper.insert(rec);

        // 通知班主任
        if (classId != null) {
            Classes cls = classesMapper.selectById(classId);
            if (cls != null && cls.getHeadTeacherId() != null) {
                try {
                    notificationService.notify(cls.getHeadTeacherId(), NotificationType.ACADEMIC_ALERT,
                            "[学业预警] " + studentName + " — " + rule.getName(),
                            summary + "\n班级：" + (cls.getClassName() != null ? cls.getClassName() : ""),
                            rec.getId());
                    rec.setNotifiedTeacher(1);
                } catch (Exception e) {
                    // 通知失败不影响预警记录创建
                }
            }
        }

        // 通知家长
        List<ParentChildRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ParentChildRelation>().eq(ParentChildRelation::getStudentId, studentId));
        if (!relations.isEmpty()) {
            try {
                for (ParentChildRelation rel : relations) {
                    notificationService.notify(rel.getParentId(), NotificationType.ACADEMIC_ALERT,
                            "[学业预警] " + studentName + " -- " + rule.getName(),
                            "您的孩子" + studentName + "触发了学业预警：" + summary,
                            rec.getId());
                }
                rec.setNotifiedParents(1);
            } catch (Exception e) {
                // 通知失败不影响预警记录创建
            }
        }
        recordMapper.updateById(rec);
    }

    // ──────────────── 工具方法 ────────────────

    private List<String> parseTaskTypes(String taskTypes) {
        if (taskTypes == null || taskTypes.isBlank()) return List.of();
        return Arrays.stream(taskTypes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private Map<Long, User> loadUsersByStudentIds(Set<Long> studentIds) {
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Set<Long> userIds = students.stream().map(Student::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private final Map<Long, String> classCache = new HashMap<>();
    private String getClassName(Long classId) {
        if (classId == null) return null;
        return classCache.computeIfAbsent(classId, id -> {
            Classes c = classesMapper.selectById(id);
            return c != null ? c.getClassName() : null;
        });
    }

    @Override
    public List<Map<String, Object>> getAlertTrend(Long teacherUserId) {
        // F7: 管理员可查看全校趋势
        boolean isAdmin = SecurityUtils.isAdmin();
        Set<Long> classIds;
        if (isAdmin) {
            List<Classes> allClasses = classesMapper.selectList(null);
            classIds = allClasses.stream().map(Classes::getId).collect(Collectors.toSet());
        } else {
            List<Classes> teacherClasses = classesMapper.selectList(
                    new LambdaQueryWrapper<Classes>()
                            .eq(Classes::getHeadTeacherId, teacherUserId));
            classIds = teacherClasses.stream().map(Classes::getId).collect(Collectors.toSet());
        }
        if (classIds.isEmpty()) return List.of();

        // F5: 规则列表查询提到循环外部，消除N+1
        List<AlertRule> lowRules = ruleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getAlertType, "LOW_SCORE"));
        List<AlertRule> missRules = ruleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getAlertType, "MISSING"));
        List<AlertRule> dropRules = ruleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getAlertType, "SCORE_DROP"));
        List<Long> lowRuleIds = lowRules.stream().map(AlertRule::getId).collect(Collectors.toList());
        List<Long> missRuleIds = missRules.stream().map(AlertRule::getId).collect(Collectors.toList());
        List<Long> dropRuleIds = dropRules.stream().map(AlertRule::getId).collect(Collectors.toList());

        List<Map<String, Object>> trend = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            java.time.LocalDateTime dayStart = day.atStartOfDay();
            java.time.LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            // 低分预警
            long lowScore = 0;
            if (!lowRuleIds.isEmpty()) {
                lowScore = recordMapper.selectCount(
                        new LambdaQueryWrapper<AlertRecord>()
                                .in(AlertRecord::getClassId, classIds)
                                .in(AlertRecord::getRuleId, lowRuleIds)
                                .ge(AlertRecord::getCreateTime, dayStart)
                                .lt(AlertRecord::getCreateTime, dayEnd));
            }

            // 缺交预警
            long missing = 0;
            if (!missRuleIds.isEmpty()) {
                missing = recordMapper.selectCount(
                        new LambdaQueryWrapper<AlertRecord>()
                                .in(AlertRecord::getClassId, classIds)
                                .in(AlertRecord::getRuleId, missRuleIds)
                                .ge(AlertRecord::getCreateTime, dayStart)
                                .lt(AlertRecord::getCreateTime, dayEnd));
            }

            // F9: 成绩骤降预警
            long scoreDrop = 0;
            if (!dropRuleIds.isEmpty()) {
                scoreDrop = recordMapper.selectCount(
                        new LambdaQueryWrapper<AlertRecord>()
                                .in(AlertRecord::getClassId, classIds)
                                .in(AlertRecord::getRuleId, dropRuleIds)
                                .ge(AlertRecord::getCreateTime, dayStart)
                                .lt(AlertRecord::getCreateTime, dayEnd));
            }

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", day.toString());
            dayData.put("lowScore", lowScore);
            dayData.put("missing", missing);
            dayData.put("scoreDrop", scoreDrop);
            dayData.put("total", lowScore + missing + scoreDrop);
            trend.add(dayData);
        }
        return trend;
    }
}
