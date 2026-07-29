package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.DashboardService;
import com.school.teaching.service.TaskComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired private StudentMapper studentMapper;
    @Autowired private SignRecordMapper signRecordMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private com.school.teaching.mapper.TaskMapper taskMapper;
    @Autowired private com.school.teaching.mapper.TaskSubmissionMapper taskSubmissionMapper;
    @Autowired private com.school.teaching.mapper.QuestionBankMapper questionBankMapper;
    @Autowired private TaskComparisonService taskComparisonService;
    @Autowired private com.school.teaching.mapper.TeacherMapper teacherMapper;
    @Autowired private com.school.teaching.service.SystemService systemService;
    @Autowired private KnowledgeNodeMapper nodeMapper;

    @Override
    public Map<String, Object> teacherStats(Long classId) {
        Map<String, Object> data = new HashMap<>();
        data.put("classId", classId);

        List<Long> studentIdsInClass = getStudentIdsByClass(classId);

        buildTaskStats(data, classId, studentIdsInClass);
        data.put("signTrend", buildSignTrend(classId, studentIdsInClass));
        data.put("creditDistribution", buildCreditDistribution(classId));
        buildPendingStats(data, classId, studentIdsInClass);

        // 质量预警（无 classId 限制，按教师全局扫描）
        try {
            Long userId = com.school.teaching.security.SecurityUtils.getCurrentUserId();
            com.school.teaching.entity.Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<com.school.teaching.entity.Teacher>().eq(com.school.teaching.entity.Teacher::getUserId, userId));
            if (teacher != null) {
                int scanDays = 30;
                double scoreThreshold = 15;
                double kpThreshold = 30;
                int maxVisible = 3;
                try {
                    Map<String, String> settings = systemService.getAllSettings();
                    if (settings.containsKey("quality.alert.days"))
                        scanDays = Integer.parseInt(settings.get("quality.alert.days"));
                    if (settings.containsKey("quality.alert.score_threshold"))
                        scoreThreshold = Double.parseDouble(settings.get("quality.alert.score_threshold"));
                    if (settings.containsKey("quality.alert.knowledge_threshold"))
                        kpThreshold = Double.parseDouble(settings.get("quality.alert.knowledge_threshold"));
                    if (settings.containsKey("quality.alert.max_visible"))
                        maxVisible = Integer.parseInt(settings.get("quality.alert.max_visible"));
                } catch (Exception ignored) { /* 配置解析失败用默认值 */ }
                data.put("qualityAlerts", taskComparisonService.getQualityAlerts(
                    teacher.getId(), scanDays, scoreThreshold, kpThreshold, maxVisible));
            }
        } catch (Exception ignored) { /* 预警获取失败不影响主数据 */ }

        return data;
    }

    private List<Long> getStudentIdsByClass(Long classId) {
        if (classId == null) return new ArrayList<>();
        return studentMapper.selectList(
            new LambdaQueryWrapper<Student>().eq(Student::getClassId, classId))
            .stream().map(Student::getId).toList();
    }

    private void buildTaskStats(Map<String, Object> data, Long classId, List<Long> studentIdsInClass) {
        long totalStudents, totalHomework, submittedStudents, taskPassed;
        LambdaQueryWrapper<com.school.teaching.entity.Task> taskW = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Student> studentW = new LambdaQueryWrapper<>();

        if (classId != null) {
            taskW.eq(com.school.teaching.entity.Task::getTargetType, "CLASS")
                 .eq(com.school.teaching.entity.Task::getTargetId, classId);
            studentW.eq(Student::getClassId, classId);
        }

        totalStudents = studentMapper.selectCount(studentW);
        totalHomework = taskMapper.selectCount(taskW);

        // 去重统计：提交人数 + 通过人数（均为去重学生数，防止一学生多任务虚增）
        java.util.Set<Long> allSubStudentIds = new java.util.HashSet<>();
        java.util.Set<Long> passedStudentIds = new java.util.HashSet<>();
        java.util.Set<Long> classStudentIdSet = new java.util.HashSet<>(studentIdsInClass);
        if (classId != null && !studentIdsInClass.isEmpty()) {
            List<com.school.teaching.entity.Task> tasks = taskMapper.selectList(taskW);
            java.util.Set<Long> tids = tasks.stream().map(com.school.teaching.entity.Task::getId).collect(Collectors.toSet());
            if (!tids.isEmpty()) {
                List<com.school.teaching.entity.TaskSubmission> subs = taskSubmissionMapper.selectList(
                    new LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                        .in(com.school.teaching.entity.TaskSubmission::getTaskId, tids));
                for (com.school.teaching.entity.TaskSubmission s : subs) {
                    if (s.getStudentId() == null || !classStudentIdSet.contains(s.getStudentId())) continue;
                    allSubStudentIds.add(s.getStudentId());
                    if (s.getScore() != null && s.getScore().compareTo(java.math.BigDecimal.valueOf(60)) >= 0) {
                        passedStudentIds.add(s.getStudentId());
                    }
                }
            }
        } else {
            List<com.school.teaching.entity.TaskSubmission> allSubs = taskSubmissionMapper.selectList(new LambdaQueryWrapper<>());
            for (com.school.teaching.entity.TaskSubmission s : allSubs) {
                if (s.getStudentId() == null) continue;
                allSubStudentIds.add(s.getStudentId());
                if (s.getScore() != null && s.getScore().compareTo(java.math.BigDecimal.valueOf(60)) >= 0) {
                    passedStudentIds.add(s.getStudentId());
                }
            }
        }
        submittedStudents = allSubStudentIds.size();
        taskPassed = passedStudentIds.size();

        double homeworkSubmissionRate = totalStudents > 0 ? (double) submittedStudents / totalStudents : 0;
        double examPassRate = submittedStudents > 0 ? (double) taskPassed / submittedStudents : 0;

        data.put("totalStudents", totalStudents);
        data.put("totalHomework", totalHomework);
        data.put("submittedStudents", submittedStudents);
        data.put("homeworkSubmissionRate", homeworkSubmissionRate);
        data.put("examPassRate", examPassRate);
        // 兼容旧字段名
        data.put("totalTasks", totalHomework);
        data.put("taskPassRate", examPassRate);
    }

    private List<Map<String, Object>> buildSignTrend(Long classId, List<Long> studentIdsInClass) {
        List<Map<String, Object>> signTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LambdaQueryWrapper<SignRecord> signW = new LambdaQueryWrapper<>();
            signW.eq(SignRecord::getSignDate, date);
            if (classId != null && !studentIdsInClass.isEmpty()) {
                signW.in(SignRecord::getStudentId, studentIdsInClass);
            }
            long count = signRecordMapper.selectCount(signW);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", count);
            signTrend.add(item);
        }
        return signTrend;
    }

    private List<Map<String, Object>> buildCreditDistribution(Long classId) {
        List<Map<String, Object>> creditDist = new ArrayList<>();
        int[] ranges = {0, 50, 100, 200, 500, 10000};
        String[] labels = {"0-50", "51-100", "101-200", "201-500", "500+"};
        for (int i = 0; i < ranges.length - 1; i++) {
            LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
            w.ge(Student::getTotalCredits, ranges[i]).lt(Student::getTotalCredits, ranges[i + 1]);
            if (classId != null) w.eq(Student::getClassId, classId);
            long cnt = studentMapper.selectCount(w);
            Map<String, Object> item = new HashMap<>();
            item.put("range", labels[i]);
            item.put("count", cnt);
            creditDist.add(item);
        }
        return creditDist;
    }

    /** 待办统计：待批阅/待发布/待审核AI试题 */
    private void buildPendingStats(Map<String, Object> data, Long classId, List<Long> studentIdsInClass) {
        // 1. 待批阅：已提交但未评分的 submission
        LambdaQueryWrapper<TaskSubmission> pendingReviewW = new LambdaQueryWrapper<>();
        pendingReviewW.eq(TaskSubmission::getStatus, "SUBMITTED")
                       .isNull(TaskSubmission::getScore);
        if (classId != null && !studentIdsInClass.isEmpty()) {
            pendingReviewW.in(TaskSubmission::getStudentId, studentIdsInClass);
        }
        long pendingReview = taskSubmissionMapper.selectCount(pendingReviewW);
        data.put("pendingReview", pendingReview);

        // 2. 待发布：草稿状态的任务
        LambdaQueryWrapper<Task> draftW = new LambdaQueryWrapper<>();
        draftW.eq(Task::getStatus, "DRAFT");
        if (classId != null) draftW.eq(Task::getTargetId, classId).eq(Task::getTargetType, "CLASS");
        long pendingPublish = taskMapper.selectCount(draftW);
        data.put("pendingPublish", pendingPublish);

        // 3. 待审核AI试题：status=0（AI生成后默认值，待审核通过）
        LambdaQueryWrapper<QuestionBank> aiQw = new LambdaQueryWrapper<>();
        aiQw.eq(QuestionBank::getStatus, 0);
        long pendingAiReview = questionBankMapper.selectCount(aiQw);
        data.put("pendingAiReview", pendingAiReview);

        // 4. 待审核学习资源
        LambdaQueryWrapper<KnowledgeNode> lrW = new LambdaQueryWrapper<>();
        lrW.eq(KnowledgeNode::getResourceStatus, "PENDING");
        long pendingResourceReview = nodeMapper.selectCount(lrW);
        data.put("pendingResourceReview", pendingResourceReview);
    }
}
