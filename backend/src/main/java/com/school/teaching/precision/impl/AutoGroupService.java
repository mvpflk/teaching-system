package com.school.teaching.precision.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AutoGroupService {

    @Autowired private StudentMapper studentMapper;
    @Autowired private StudentGroupMapper groupMapper;
    @Autowired private StudentGroupMemberMapper memberMapper;
    @Autowired private PrecisionProgressMapper progressMapper;
    @Autowired private NotificationService notificationService;
    @Autowired private com.school.teaching.service.SystemService systemService;
    @Autowired(required = false) private com.school.teaching.service.CreditService creditService;
    @Autowired(required = false) private CreditTransactionMapper creditTransactionMapper;
    @Autowired(required = false) private com.school.teaching.mapper.DictSubjectMapper dictSubjectMapper;

    // 幂等性保障：记录定时任务执行状态
    private volatile boolean isRunning = false;
    private volatile long lastRunTime = 0;
    private static final long MIN_INTERVAL_MS = 60 * 60 * 1000L; // 最小间隔 1 小时

    /** 从 dict_subject 动态获取所有职高学科名称，避免硬编码 */
    private List<String> getVocationalSubjects() {
        try {
            if (dictSubjectMapper != null) {
                List<com.school.teaching.entity.DictSubject> subjects = dictSubjectMapper.selectList(
                    new LambdaQueryWrapper<com.school.teaching.entity.DictSubject>()
                        .like(com.school.teaching.entity.DictSubject::getSubjectName, "[职高]")
                        .eq(com.school.teaching.entity.DictSubject::getStatus, 1));
                if (!subjects.isEmpty()) {
                    return subjects.stream()
                        .map(com.school.teaching.entity.DictSubject::getSubjectName)
                        .collect(Collectors.toList());
                }
            }
        } catch (Exception e) { log.warn("查询职高学科列表失败，使用默认", e); }
        // 降级：硬编码兜底
        return List.of("数学[职高]", "英语[职高]", "信息技术应用基础[职高]");
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper OM = new com.fasterxml.jackson.databind.ObjectMapper();
    private static final int PAGE_SIZE = 100;

    /** R112修复：安全整数转换，兼容 Integer/Long（JSON 反序列化类型不确定） */
    private static int toInt(Object val, int def) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (Exception e) {} }
        return def;
    }

    /**
     * 分页加载有偏科画像的学生（避免全表扫描）
     * @param consumer 处理每个学生的回调，返回 true 表示 profile 已修改需写回
     */
    private void forEachProfileStudent(java.util.function.Function<Student, Boolean> consumer) {
        int page = 0;
        while (true) {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Student> pg =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page + 1, PAGE_SIZE);
            var result = studentMapper.selectPage(pg,
                new LambdaQueryWrapper<Student>().isNotNull(Student::getPrecisionProfile));
            List<Student> batch = result.getRecords();
            if (batch.isEmpty()) break;
            for (Student st : batch) {
                try {
                    boolean changed = consumer.apply(st);
                    if (changed) {
                        studentMapper.updateById(st);
                    }
                } catch (Exception e) {
                    log.debug("处理学生 {} profile 失败: {}", st.getId(), e.getMessage());
                }
            }
            page++;
        }
    }

    /** 诊断后立即调用的单生入组检查（不等待周日定时任务） */
    public void addSingleStudent(Long studentId, String subject, int score) {
        int threshold = systemService.getIntConfig("remedial.auto_group_threshold", 50);
        if (score >= threshold) return;
        Student st = studentMapper.selectById(studentId);
        if (st == null) return;
        boolean newlyAdded = ensureStudentInGroup(st, subject, "REMEDIAL");
        if (newlyAdded) {
            try {
                notificationService.notify(st.getUserId(), "remedial_group",
                    "偏科提分·加入分组",
                    "根据诊断结果（" + subject + "：" + score + "分），你已被加入" + subject + "偏科组，系统将为你推送针对性学习内容。", null);
            } catch (Exception ignored) { log.warn("偏科组通知发送失败", ignored); }
        }
    }

    /** 每周日凌晨 2:00 执行自动分组维护 */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void autoGroupMaintenance() {
        // 幂等性检查：防止重复执行
        long now = System.currentTimeMillis();
        if (isRunning) {
            log.warn("AutoGroup 正在执行中，跳过本次调用");
            return;
        }
        if (now - lastRunTime < MIN_INTERVAL_MS) {
            log.warn("AutoGroup 距离上次执行不足 1 小时，跳过本次调用");
            return;
        }

        isRunning = true;
        log.info("AutoGroup 自动分组维护开始");
        try {
            addDiagnosisStudents();
            addWarningStudents();
            removeMasteredStudents();
            settleEnglishWeekly();
            lastRunTime = now;
            log.info("AutoGroup 自动分组维护完成");
        } catch (Exception e) {
            log.error("AutoGroup 执行异常", e);
        } finally {
            isRunning = false;
        }
    }

    /** 英语周结算：连续7天打卡→+5 · 周进步→+8 · bizKey防重 · 分页处理 */
    private void settleEnglishWeekly() {
        if (creditService == null) return;
        java.time.LocalDate today = java.time.LocalDate.now();
        log.info("AutoGroup 英语周结算开始");
        forEachProfileStudent(st -> {
            boolean changed = false;
            Map<String, Object> full = null;
            try { full = OM.readValue(st.getPrecisionProfile(), Map.class); }
            catch (Exception ignored) { return false; }
            // 动态查找英语 profile key（优先"英语[职高]"，其次包含"英语"的任意key）
            String engKey = null;
            for (String key : full.keySet()) {
                if (key.contains("英语")) { engKey = key; break; }
            }
            if (engKey == null) return false;
            @SuppressWarnings("unchecked")
            Map<String, Object> eng = (Map<String, Object>) full.getOrDefault(engKey, new LinkedHashMap<>());
            if (eng.isEmpty()) return false;
            int streak = toInt(eng.get("streak"), 0);
            long sid = st.getId();

            // 连续7天打卡→+5
            if (streak >= 7) {
                String bizKey = "engl_7streak:" + sid + ":" + today;
                if (creditTransactionMapper == null
                    || creditTransactionMapper.selectCount(
                        new LambdaQueryWrapper<CreditTransaction>()
                            .eq(CreditTransaction::getStudentId, sid)
                            .eq(CreditTransaction::getDescription, bizKey)) == 0) {
                    creditService.awardMoralCredit(sid, 5, bizKey);
                }
            }

            // 周进步之星→+8
            int weekCorrect = toInt(eng.get("thisWeekCorrect"), 0);
            int weekTotal = toInt(eng.get("thisWeekTotal"), 0);
            int lastRate = eng.containsKey("lastWeekCorrectRate") ? toInt(eng.get("lastWeekCorrectRate"), -1) : -1;
            if (weekTotal >= 10 && lastRate >= 0) {
                int thisRate = weekTotal > 0 ? (weekCorrect * 100 / weekTotal) : 0;
                if (thisRate >= lastRate + 10) {
                    String improvementKey = "engl_improve:" + sid + ":" + today;
                    if (creditTransactionMapper == null
                        || creditTransactionMapper.selectCount(
                            new LambdaQueryWrapper<CreditTransaction>()
                                .eq(CreditTransaction::getStudentId, sid)
                                .eq(CreditTransaction::getDescription, improvementKey)) == 0) {
                        creditService.awardMoralCredit(sid, 8, improvementKey);
                    }
                }
                eng.put("lastWeekCorrectRate", thisRate);
                changed = true;
            } else if (weekTotal >= 10 && lastRate < 0) {
                eng.put("lastWeekCorrectRate", weekCorrect * 100 / weekTotal);
                changed = true;
            }

            // 重置本周计数（仅当有计数时写回）
            if (weekTotal > 0 || weekCorrect > 0) {
                eng.put("thisWeekCorrect", 0);
                eng.put("thisWeekTotal", 0);
                changed = true;
            }

            if (changed) {
                // B4: 使用动态 engKey 而非硬编码 "英语[职高]"，防止写入错误的 profile 键
                full.put(engKey, eng);
                try { st.setPrecisionProfile(OM.writeValueAsString(full)); }
                catch (Exception e) { log.debug("序列化 profile 失败 sid={}: {}", st.getId(), e.getMessage()); }
            }
            return changed;
        });
        log.info("AutoGroup 英语周结算完成");
    }

    /** 诊断分 < 阈值 → 加入偏科组（分页处理，非当天诊断才发通知） */
    private void addDiagnosisStudents() {
        int threshold = systemService.getIntConfig("remedial.auto_group_threshold", 50);
        java.time.LocalDate today = java.time.LocalDate.now();
        forEachProfileStudent(st -> {
            Map<String, Object> profile = null;
            try { profile = OM.readValue(st.getPrecisionProfile(), Map.class); }
            catch (Exception ignored) { return false; }
            for (String subject : getVocationalSubjects()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sp = (Map<String, Object>) profile.get(subject);
                if (sp == null) continue;
                int score = toInt(sp.get("diagnoseScore"), 0);
                if (score > 0 && score < threshold) {
                    ensureStudentInGroup(st, subject, "REMEDIAL");
                    String lastDiagStr = (String) sp.get("lastDiagnoseAt");
                    boolean isTodayDiag = lastDiagStr != null && lastDiagStr.equals(today.toString());
                    if (!isTodayDiag) {
                        notificationService.notify(st.getUserId(), "remedial_group",
                            "偏科提分·加入分组",
                            "根据诊断结果（" + subject + "：" + score + "分），你已被加入" + subject + "偏科组，系统将为你推送针对性学习内容。", null);
                    }
                }
            }
            return false; // 不修改 profile
        });
    }

    /** 连续 N 周未提交 → 加入预警组（分页处理） */
    private void addWarningStudents() {
        int warnWeeks = systemService.getIntConfig("remedial.streak_warn_weeks", 2);
        java.time.LocalDate now = java.time.LocalDate.now();
        forEachProfileStudent(st -> {
            Map<String, Object> profile = null;
            try { profile = OM.readValue(st.getPrecisionProfile(), Map.class); }
            catch (Exception ignored) { return false; }
            for (String subject : getVocationalSubjects()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sp = (Map<String, Object>) profile.get(subject);
                if (sp == null) continue;
                int diagScore = toInt(sp.get("diagnoseScore"), 0);
                if (diagScore <= 0) continue;
                String lastSubmitStr = (String) sp.get("lastSubmitDate");
                long weeksSince = 999;
                if (lastSubmitStr != null && !lastSubmitStr.isEmpty()) {
                    try {
                        java.time.LocalDate lastSubmit = java.time.LocalDate.parse(lastSubmitStr);
                        long days = java.time.temporal.ChronoUnit.DAYS.between(lastSubmit, now);
                        weeksSince = Math.max(1, days / 7);
                    } catch (Exception e) { log.warn("解析提交日期失败: {}", lastSubmitStr); }
                }
                if (weeksSince >= warnWeeks) {
                    // B5: 仅当新加入预警组时才通知，避免每周重复通知
                    boolean newlyAdded = ensureStudentInGroup(st, subject, "PRECISION_WARNING");
                    if (newlyAdded) {
                        notificationService.notify(st.getUserId(), "remedial_warning",
                            "偏科提分·预警通知",
                            subject + "已连续 " + weeksSince + " 周未完成线上小测，请尽快登录系统提交。", null);
                    }
                }
            }
            return false;
        });
    }

    /** 连续 4 周达标 → 移出偏科组（分页处理） */
    private void removeMasteredStudents() {
        forEachProfileStudent(st -> {
            Map<String, Object> profile = null;
            try { profile = OM.readValue(st.getPrecisionProfile(), Map.class); }
            catch (Exception ignored) { return false; }
            for (String subject : getVocationalSubjects()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sp = (Map<String, Object>) profile.get(subject);
                if (sp == null) continue;
                int streak = toInt(sp.get("streakWeeks"), 0);
                int lastScore = toInt(sp.get("lastOnlineTestScore"), 0);
                if (streak >= 4 && lastScore >= 80) {
                    removeStudentFromGroup(st, subject);
                }
            }
            return false;
        });
    }

    private boolean ensureStudentInGroup(Student st, String subject, String groupType) {
        String groupName = subject + (groupType.equals("PRECISION_WARNING") ? "-预警组" : "-偏科组");
        // 查找或创建分组
        StudentGroup group = findOrCreateGroup(st.getClassId(), groupName, subject, groupType);
        // 检查是否已是成员
        Long cnt = memberMapper.selectCount(new LambdaQueryWrapper<StudentGroupMember>()
            .eq(StudentGroupMember::getGroupId, group.getId())
            .eq(StudentGroupMember::getStudentId, st.getId()));
        if (cnt > 0) return false;
        StudentGroupMember m = new StudentGroupMember();
        m.setGroupId(group.getId()); m.setStudentId(st.getId());
        memberMapper.insert(m);
        return true;
    }

    private void removeStudentFromGroup(Student st, String subject) {
        String groupName = subject + "-偏科组";
        StudentGroup group = groupMapper.selectOne(
            new LambdaQueryWrapper<StudentGroup>()
                .eq(StudentGroup::getClassId, st.getClassId())
                .eq(StudentGroup::getName, groupName));
        if (group == null) return;
        memberMapper.delete(new LambdaQueryWrapper<StudentGroupMember>()
            .eq(StudentGroupMember::getGroupId, group.getId())
            .eq(StudentGroupMember::getStudentId, st.getId()));
        notificationService.notify(st.getUserId(), "remedial_graduated",
            "偏科提分达成", "恭喜！你已连续4周达标，已移出「" + subject + "」偏科组。", null);
    }

    private StudentGroup findOrCreateGroup(Long classId, String name, String subject, String groupType) {
        StudentGroup g = groupMapper.selectOne(
            new LambdaQueryWrapper<StudentGroup>()
                .eq(StudentGroup::getClassId, classId)
                .eq(StudentGroup::getName, name));
        if (g == null) {
            g = new StudentGroup();
            g.setClassId(classId); g.setName(name);
            g.setSubjectId(getSubjectId(subject));
            g.setGroupType(groupType);
            g.setSortOrder(99);
            groupMapper.insert(g);
        }
        return g;
    }

    private Long getSubjectId(String subject) {
        if (subject == null) return null;
        try {
            if (dictSubjectMapper != null) {
                com.school.teaching.entity.DictSubject ds = dictSubjectMapper.selectOne(
                    new LambdaQueryWrapper<com.school.teaching.entity.DictSubject>()
                        .eq(com.school.teaching.entity.DictSubject::getSubjectName, subject));
                if (ds != null) return ds.getId();
            }
        } catch (Exception e) { log.warn("查询学科ID失败: {}", subject, e); }
        // 降级：硬编码兜底
        if (subject.contains("数学")) return 22L;
        if (subject.contains("英语")) return 24L;
        return null;
    }
}
