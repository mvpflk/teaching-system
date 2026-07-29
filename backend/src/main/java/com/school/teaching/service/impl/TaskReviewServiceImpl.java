package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.common.ExamTaskHandler;
import com.school.teaching.common.NotificationType;
import com.school.teaching.common.TaskCategory;
import com.school.teaching.entity.*;
import com.school.teaching.event.TaskEvent;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.NotificationService;
import com.school.teaching.service.TaskReviewService;
import com.school.teaching.service.TeachingGroupService;
import com.school.teaching.service.LessonPrepGroupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskReviewServiceImpl implements TaskReviewService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final QuestionBankMapper questionBankMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final NotificationService notificationService;
    private final TeachingGroupService teachingGroupService;
    private final LessonPrepGroupService lessonPrepGroupService;
    private final DictGradeMapper dictGradeMapper;
    private final DictSubjectMapper dictSubjectMapper;
    private final com.school.teaching.service.SystemService systemService;
    private final ClassesMapper classesMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ExamTaskHandler examTaskHandler;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final KnowledgeNodeMapper nodeMapper;

    private static final Logger log = LoggerFactory.getLogger(TaskReviewServiceImpl.class);

    @Override
    public byte[] exportScores(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        
        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));

        Set<Long> sids = subs.stream().map(TaskSubmission::getStudentId).collect(Collectors.toSet());
        Map<Long, Student> stuMap = sids.isEmpty() ? Map.of() :
            studentMapper.selectBatchIds(sids).stream().collect(Collectors.toMap(Student::getId, s -> s));
        Set<Long> userIds = stuMap.values().stream().map(Student::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
            userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, List<TaskSubmission>> byStudent = subs.stream()
            .collect(Collectors.groupingBy(TaskSubmission::getStudentId));

        try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
            var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            var sheet = wb.createSheet(task.getTitle() != null ? task.getTitle() : "成绩");
            var header = sheet.createRow(0);
            String[] cols = {"学号", "姓名", "首次得分", "重测次数", "状态", "最后提交时间"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            int rowIdx = 1;
            for (var entry : byStudent.entrySet()) {
                List<TaskSubmission> studentSubs = entry.getValue();
                var row = sheet.createRow(rowIdx++);

                TaskSubmission official = studentSubs.stream()
                    .filter(s -> s.getIsOfficial() != null && s.getIsOfficial())
                    .findFirst().orElse(null);
                TaskSubmission latest = studentSubs.stream()
                    .max(Comparator.comparingInt(s -> s.getAttemptNumber() != null ? s.getAttemptNumber() : 1))
                    .orElse(null);

                Student stu = stuMap.get(entry.getKey());
                User u = stu != null ? userMap.get(stu.getUserId()) : null;
                row.createCell(0).setCellValue(stu != null ? stu.getStudentNumber() : "");
                row.createCell(1).setCellValue(u != null ? u.getRealName() : "");
                row.createCell(2).setCellValue(official != null && official.getScore() != null ? official.getScore().doubleValue() : 0);
                row.createCell(3).setCellValue(Math.max(0, studentSubs.size() - 1));
                row.createCell(4).setCellValue(latest != null ? (latest.getStatus() != null ? latest.getStatus() : "") : "");
                row.createCell(5).setCellValue(latest != null && latest.getSubmittedAt() != null ? latest.getSubmittedAt().toString() : "");
            }
            wb.write(bos);
            wb.close();
            return bos.toByteArray();
        } catch (java.io.IOException e) {
            throw new BusinessException(500, "导出失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void submitForReview(Long taskId) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(404, "任务不存在");
        if (!isExamType(t)) throw new BusinessException(400, "仅考试类任务可提交审核");
        if (!"NOT_SUBMITTED".equals(t.getReviewStatus()) && !"REJECTED".equals(t.getReviewStatus()))
            throw new BusinessException(409, "当前审核状态不可提交");
        if (!isReviewEnabled()) { 
            t.setStatus("PUBLISHED");
            taskMapper.updateById(t);
            return; 
        }

        t.setReviewStatus("PENDING_GROUP");
        Long teacherId = t.getTeacherId();
        Classes cls = t.getTargetId() != null ? classesMapper.selectById(t.getTargetId()) : null;
        LessonPrepGroup lpg = findLessonPrepGroup(t, cls);
        Long lpgLeader = lpg != null ? lessonPrepGroupService.getFirstLeaderId(lpg.getId()) : null;

        if (lpgLeader != null && !lpgLeader.equals(teacherId)) {
            t.setReviewStatus("PENDING_GROUP");
            taskMapper.updateById(t);
            notificationService.notify(lpgLeader, NotificationType.TASK_SUBMITTED_FOR_REVIEW,
                "新任务待审核", t.getTitle(), taskId);
            eventPublisher.publishEvent(TaskEvent.submittedForReview(this, taskId,
                TaskCategory.valueOf(t.getTaskType()), teacherId, Map.of("level", "GROUP")));
            return;
        }
        Long tgLeader = lpg != null && lpg.getTeachingGroupId() != null ?
            teachingGroupService.getFirstLeaderId(lpg.getTeachingGroupId()) : null;
        if (tgLeader != null && !tgLeader.equals(teacherId)) {
            t.setReviewStatus("PENDING_TEACHING");
            taskMapper.updateById(t);
            notificationService.notify(tgLeader, NotificationType.TASK_SUBMITTED_FOR_REVIEW,
                "新任务待审核", t.getTitle(), taskId);
            eventPublisher.publishEvent(TaskEvent.submittedForReview(this, taskId,
                TaskCategory.valueOf(t.getTaskType()), teacherId, Map.of("level", "TEACHING")));
            return;
        }
        t.setReviewStatus("APPROVED");
        taskMapper.updateById(t);
    }

    @Override
    @Transactional
    public void approveReview(Long taskId, Long reviewerId) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(404, "任务不存在");
        String rs = t.getReviewStatus();
        if (!"PENDING_GROUP".equals(rs) && !"PENDING_TEACHING".equals(rs))
            throw new BusinessException(409, "当前审核状态不可通过");
        if ("PENDING_GROUP".equals(rs)) {
            Classes cls2 = t.getTargetId() != null ? classesMapper.selectById(t.getTargetId()) : null;
            LessonPrepGroup lpg = findLessonPrepGroup(t, cls2);
            Long tgLeader = lpg != null && lpg.getTeachingGroupId() != null ?
                teachingGroupService.getFirstLeaderId(lpg.getTeachingGroupId()) : null;
            if (tgLeader != null && !tgLeader.equals(t.getTeacherId())) {
                t.setReviewStatus("PENDING_TEACHING"); taskMapper.updateById(t);
                notificationService.notify(tgLeader, NotificationType.TASK_REVIEW_APPROVED,
                    "备课组长已通过，请审核", t.getTitle(), taskId);
                return;
            }
        }
        t.setReviewStatus("APPROVED"); taskMapper.updateById(t);
        notificationService.notify(t.getTeacherId(), NotificationType.TASK_REVIEW_APPROVED,
            "任务审核已通过并发布", t.getTitle(), taskId);
    }

    @Override
    @Transactional
    public void rejectReview(Long taskId, Long reviewerId, String reason) {
        Task t = taskMapper.selectById(taskId);
        if (t == null) throw new BusinessException(404, "任务不存在");
        if (!"PENDING_GROUP".equals(t.getReviewStatus()) && !"PENDING_TEACHING".equals(t.getReviewStatus()))
            throw new BusinessException(409, "当前审核状态不可拒绝");
        t.setReviewStatus("REJECTED"); taskMapper.updateById(t);
        notificationService.notify(t.getTeacherId(), NotificationType.TASK_REVIEW_REJECTED,
            "任务审核被退回: " + (reason != null ? reason : ""), t.getTitle(), taskId);
        eventPublisher.publishEvent(TaskEvent.reviewRejected(this, taskId,
            TaskCategory.valueOf(t.getTaskType()), t.getTeacherId(), reviewerId,
            Map.of("reason", reason != null ? reason : "")));
    }

    @Override
    public List<Task> getPendingReviews(Long teacherId) {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(teacherId != null, Task::getTeacherId, teacherId)
            .in(Task::getReviewStatus, List.of("PENDING_GROUP", "PENDING_TEACHING")));
    }

    @Override
    public List<Map<String, Object>> getSubmissionAnswers(Long taskId, Long submissionId) {
        List<StudentAnswer> answers = studentAnswerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getTaskId, taskId)
                .eq(StudentAnswer::getSubmissionId, submissionId));
        List<Long> qids = answers.stream().map(StudentAnswer::getQuestionId).distinct().toList();
        Map<Long, QuestionBank> qmap = qids.isEmpty() ? Map.of()
            : questionBankMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(QuestionBank::getId, q -> q));
        List<TaskQuestion> tqs = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, taskId));
        Map<Long, BigDecimal> scoreMap = tqs.stream().collect(Collectors.toMap(
            TaskQuestion::getQuestionId, tq -> tq.getScore() != null ? tq.getScore() : BigDecimal.ONE));

        Map<Long, String> kpNameMap = new HashMap<>();
        Set<Long> cids = qmap.values().stream().map(QuestionBank::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!cids.isEmpty()) {
            nodeMapper.selectBatchIds(cids).forEach(n -> kpNameMap.put(n.getId(), n.getName()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentAnswer a : answers) {
            QuestionBank q = qmap.get(a.getQuestionId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("questionId", a.getQuestionId());
            item.put("questionText", q != null ? q.getQuestionText() : "");
            item.put("questionType", q != null ? q.getQuestionType() : "");
            item.put("correctAnswer", q != null ? q.getCorrectAnswer() : "");
            item.put("options", q != null ? q.getOptions() : "[]");
            item.put("score", scoreMap.getOrDefault(a.getQuestionId(), BigDecimal.ONE));
            item.put("studentAnswer", a.getStudentAnswer());
            item.put("isCorrect", a.getIsCorrect());
            item.put("autoScore", a.getAutoScore());
            item.put("explanation", q != null ? q.getExplanation() : "");
            item.put("categoryId", q != null ? q.getCategoryId() : null);
            item.put("knowledgePointName", q != null && q.getCategoryId() != null ? kpNameMap.getOrDefault(q.getCategoryId(), "") : "");
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> batchRegrade(List<Long> submissionIds) {
        int totalChanged = 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", submissionIds.size());
        List<Map<String, Object>> details = new ArrayList<>();
        for (Long sid : submissionIds) {
            try {
                Map<String, Object> r = examTaskHandler.regradeSubmission(sid);
                int changed = (int) r.getOrDefault("changed", 0);
                if (changed > 0) {
                    totalChanged++;
                    details.add(Map.of("submissionId", sid, "changed", changed, "newTotal", r.get("newTotal")));
                }
            } catch (Exception e) {
                details.add(Map.of("submissionId", sid, "error", e.getMessage()));
            }
        }
        result.put("affected", totalChanged);
        result.put("details", details);
        return result;
    }

    @Override
    public List<Long> findSubmissionIdsByQuestionId(Long questionId) {
        return studentAnswerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getQuestionId, questionId))
            .stream()
            .map(StudentAnswer::getSubmissionId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> findSubmissionIdsByTaskId(Long taskId) {
        return studentAnswerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getTaskId, taskId))
            .stream()
            .map(StudentAnswer::getSubmissionId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public TaskSubmission getSubmissionById(Long submissionId) {
        return submissionMapper.selectById(submissionId);
    }

    @Override
    public Map<String, Object> getSurveyStats(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !"SURVEY".equals(task.getTaskType()))
            throw new BusinessException(400, "仅问卷任务支持");

        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStatus, "SUBMITTED"));
        int totalSubs = subs.size();
        List<Map<String, Object>> qStats = new ArrayList<>();
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

        List<SurveyQuestion> sqList = surveyQuestionMapper.selectList(
            new LambdaQueryWrapper<SurveyQuestion>()
                .eq(SurveyQuestion::getTaskId, taskId)
                .orderByAsc(SurveyQuestion::getQuestionOrder));
        if (!sqList.isEmpty()) {
            List<com.fasterxml.jackson.databind.JsonNode> parsedContents = new ArrayList<>();
            for (TaskSubmission s : subs) {
                if (s.getContent() != null && !s.getContent().isBlank()) {
                    try { parsedContents.add(om.readTree(s.getContent())); }
                    catch (Exception e) { parsedContents.add(null); }
                } else {
                    parsedContents.add(null);
                }
            }
            for (SurveyQuestion sq : sqList) {
                Map<String, Object> stat = new LinkedHashMap<>();
                stat.put("id", "q" + sq.getId());
                stat.put("type", sq.getQuestionType());
                stat.put("label", sq.getTitle());
                String key = "q" + sq.getId();
                if ("textarea".equals(sq.getQuestionType())) {
                    long answered = parsedContents.stream()
                        .filter(n -> n != null && n.has(key)).count();
                    stat.put("responded", answered);
                    stat.put("skipped", totalSubs - answered);
                } else {
                    Map<String, Integer> counts = new LinkedHashMap<>();
                    if (sq.getOptions() != null)
                        try { om.readTree(sq.getOptions()).forEach(opt -> counts.put(opt.asText(), 0)); } catch (Exception e) { /* ignored */ }
                    for (com.fasterxml.jackson.databind.JsonNode node : parsedContents) {
                        if (node == null || !node.has(key)) continue;
                        var val = node.get(key);
                        if (val.isArray()) val.forEach(v -> counts.merge(v.asText(), 1, Integer::sum));
                        else counts.merge(val.asText(), 1, Integer::sum);
                    }
                    stat.put("counts", counts);
                }
                qStats.add(stat);
            }
        }
        return Map.of("totalSubmissions", totalSubs, "questions", qStats);
    }

    @Override
    public byte[] exportSurvey(Long taskId, boolean blinded) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !"SURVEY".equals(task.getTaskType()))
            throw new BusinessException(400, "仅问卷任务支持");

        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStatus, "SUBMITTED"));
        List<SurveyQuestion> sqList = surveyQuestionMapper.selectList(
            new LambdaQueryWrapper<SurveyQuestion>()
                .eq(SurveyQuestion::getTaskId, taskId)
                .orderByAsc(SurveyQuestion::getQuestionOrder));

        List<Long> studentIds = subs.stream().map(TaskSubmission::getStudentId).distinct().toList();
        Map<Long, String> studentNames = new HashMap<>();
        Map<Long, String> studentClasses = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<Student> students = studentMapper.selectBatchIds(studentIds);
            Map<Long, Long> sidToUid = students.stream().collect(Collectors.toMap(Student::getId, Student::getUserId, (a,b)->a));
            Map<Long, Long> sidToCid = students.stream().collect(Collectors.toMap(Student::getId, Student::getClassId, (a,b)->a));
            if (!sidToUid.isEmpty()) {
                List<User> users = userMapper.selectBatchIds(new ArrayList<>(sidToUid.values()));
                Map<Long, String> uidToName = users.stream().collect(Collectors.toMap(User::getId, User::getRealName, (a,b)->a));
                sidToUid.forEach((sid, uid) -> studentNames.put(sid, uidToName.getOrDefault(uid, "学生"+sid)));
            }
            if (!sidToCid.isEmpty()) {
                List<Long> cids = new ArrayList<>(new HashSet<>(sidToCid.values()));
                List<Classes> classes = classesMapper.selectBatchIds(cids);
                Map<Long, String> cidToName = classes.stream().collect(Collectors.toMap(Classes::getId, Classes::getClassName, (a,b)->a));
                sidToCid.forEach((sid, cid) -> studentClasses.put(sid, cidToName.getOrDefault(cid, "班级"+cid)));
            }
        }

        Map<String, String> blindClassMap = new LinkedHashMap<>();
        if (blinded) {
            int bi = 1;
            List<String> sortedClasses = new ArrayList<>(new HashSet<>(studentClasses.values()));
            java.util.Collections.sort(sortedClasses);
            for (String cn : sortedClasses) blindClassMap.put(cn, "班级" + (bi++));
        }

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(blinded ? "学生编码" : "学生姓名").append(",")
          .append(blinded ? "班级编码" : "班级").append(",提交时间");
        for (SurveyQuestion sq : sqList) {
            sb.append(",").append(escapeCsvStr(sq.getTitle()));
        }
        sb.append("\n");

        int si = 1;
        for (TaskSubmission sub : subs) {
            String name = studentNames.getOrDefault(sub.getStudentId(), "学生"+sub.getStudentId());
            String cls = studentClasses.getOrDefault(sub.getStudentId(), "");
            sb.append(blinded ? ("S" + (si++)) : escapeCsvStr(name)).append(",");
            sb.append(blinded ? blindClassMap.getOrDefault(cls, cls) : escapeCsvStr(cls)).append(",");
            sb.append(sub.getSubmittedAt() != null ? sub.getSubmittedAt().toString() : "");
            if (sub.getContent() != null && !sub.getContent().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode content = om.readTree(sub.getContent());
                    for (SurveyQuestion sq : sqList) {
                        String key = "q" + sq.getId();
                        String val = content.has(key) ? content.get(key).asText("") : "";
                        sb.append(",").append(escapeCsvStr(val));
                    }
                } catch (Exception e) {
                    for (SurveyQuestion sq : sqList) sb.append(",");
                }
            } else {
                for (SurveyQuestion sq : sqList) sb.append(",");
            }
            sb.append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, Object> getTaskStats(Long taskId) {
        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
        Task task = taskMapper.selectById(taskId);
        Map<String, Object> r = new LinkedHashMap<>();

        int effectivePassRate = task.getPassRate() != null && task.getPassRate() > 0
            ? task.getPassRate() : 60;
        BigDecimal taskTotal = task.getTotalScore() != null ? task.getTotalScore() : BigDecimal.valueOf(100);
        BigDecimal passThreshold = taskTotal.multiply(BigDecimal.valueOf(effectivePassRate))
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        Map<String, Long> daily = new LinkedHashMap<>();
        List<Long> studentIds = subs.stream().map(TaskSubmission::getStudentId).distinct().toList();
        Map<Long, Student> stuMap = studentIds.isEmpty() ? Map.of()
            : studentMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        int total = subs.size();
        int graded = 0, passCount = 0;
        BigDecimal sumScore = BigDecimal.ZERO;
        BigDecimal maxScore = null, minScore = null;
        int[] d = new int[4];

        Map<Long, List<BigDecimal>> cs = new HashMap<>();
        for (TaskSubmission s : subs) {
            if (s.getSubmittedAt() != null)
                daily.merge(s.getSubmittedAt().toLocalDate().toString(), 1L, Long::sum);
            boolean isOfficial = s.getIsOfficial() == null || Boolean.TRUE.equals(s.getIsOfficial());
            if (s.getScore() != null && isOfficial) {
                graded++;
                BigDecimal sc = s.getScore();
                sumScore = sumScore.add(sc);
                if (maxScore == null || sc.compareTo(maxScore) > 0) maxScore = sc;
                if (minScore == null || sc.compareTo(minScore) < 0) minScore = sc;
                if (sc.compareTo(passThreshold) >= 0) passCount++;

                if (sc.compareTo(BigDecimal.valueOf(90)) >= 0) d[0]++;
                else if (sc.compareTo(BigDecimal.valueOf(75)) >= 0) d[1]++;
                else if (sc.compareTo(BigDecimal.valueOf(60)) >= 0) d[2]++;
                else d[3]++;

                Student stu = stuMap.get(s.getStudentId());
                Long cid = stu != null ? stu.getClassId() : null;
                cs.computeIfAbsent(cid != null ? cid : 0L, k -> new ArrayList<>()).add(sc);
            }
        }

        r.put("daily", daily.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .map(e -> Map.of("date", e.getKey(), "count", e.getValue())).toList());

        r.put("total", total);
        r.put("graded", graded);
        r.put("avgScore", graded > 0 ? sumScore.divide(BigDecimal.valueOf(graded), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        r.put("maxScore", maxScore != null ? maxScore : BigDecimal.ZERO);
        r.put("minScore", minScore != null ? minScore : BigDecimal.ZERO);
        r.put("passRate", graded > 0 ? BigDecimal.valueOf(passCount * 100.0 / graded).setScale(1, java.math.RoundingMode.HALF_UP) + "%" : "0%");

        List<Map<String, Object>> classStats = new ArrayList<>();
        for (var entry : cs.entrySet()) {
            var cls = classesMapper.selectById(entry.getKey());
            var scores = entry.getValue();
            var avg = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, java.math.RoundingMode.HALF_UP);
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("classId", entry.getKey());
            cm.put("className", cls != null ? cls.getClassName() : "未知班级");
            cm.put("count", scores.size());
            cm.put("avgScore", avg);
            classStats.add(cm);
        }
        r.put("classStats", classStats);
        r.put("dist", List.of(
            Map.of("name", "优秀(≥90)", "value", d[0]),
            Map.of("name", "良好(≥75)", "value", d[1]),
            Map.of("name", "及格(≥60)", "value", d[2]),
            Map.of("name", "不及格(<60)", "value", d[3])));
        return r;
    }

    private boolean isExamType(Task t) {
        return List.of("FORMATIVE", "SUMMATIVE").contains(t.getTaskType());
    }

    private boolean isReviewEnabled() {
        try {
            String val = systemService.getAllSettings().get("feature.review_enabled");
            return "true".equalsIgnoreCase(val);
        } catch (Exception e) { return false; }
    }

    private LessonPrepGroup findLessonPrepGroup(Task t, Classes cls) {
        if (cls == null) return null;
        Long gradeId = null;
        if (cls.getGrade() != null) {
            DictGrade dg = dictGradeMapper.selectOne(new LambdaQueryWrapper<DictGrade>()
                .eq(DictGrade::getGradeName, cls.getGrade()));
            if (dg != null) gradeId = dg.getId();
        }
        Long subjectId = null;
        if (t.getSubject() != null) {
            DictSubject ds = dictSubjectMapper.selectOne(new LambdaQueryWrapper<DictSubject>()
                .eq(DictSubject::getSubjectName, t.getSubject()));
            if (ds != null) subjectId = ds.getId();
        }
        return lessonPrepGroupService.findByClassInfo(cls.getStageId(), gradeId, subjectId);
    }

    private String escapeCsvStr(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}