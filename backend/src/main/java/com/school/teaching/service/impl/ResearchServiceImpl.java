package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.ResearchService;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.utils.ScoreUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResearchServiceImpl extends ServiceImpl<ResearchBaselineMapper, ResearchBaseline> implements ResearchService {

    private final PrecisionProgressMapper progressMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final ClassesMapper classesMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;

    public ResearchServiceImpl(PrecisionProgressMapper progressMapper, StudentMapper studentMapper,
                               UserMapper userMapper, ClassesMapper classesMapper,
                               KnowledgeNodeMapper knowledgeNodeMapper,
                               TaskSubmissionMapper submissionMapper, TaskMapper taskMapper) {
        this.progressMapper = progressMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.classesMapper = classesMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> captureBaseline(String snapshotLabel) {
        if (snapshotLabel == null || snapshotLabel.isEmpty()) {
            snapshotLabel = "PRETEST";
        }
        if (!List.of("PRETEST", "MIDTEST", "POSTTEST").contains(snapshotLabel)) {
            throw new BusinessException(400, "无效的快照标签: " + snapshotLabel);
        }

        LocalDateTime snapshotTime = LocalDateTime.now();

        // 检查是否已有同标签快照（防止重复拍摄）
        long existingCount = baseMapper.selectCount(
            new LambdaQueryWrapper<ResearchBaseline>()
                .eq(ResearchBaseline::getSnapshotLabel, snapshotLabel));
        if (existingCount > 0) {
            throw new BusinessException(409, "已存在 '" + snapshotLabel + "' 标签的快照（" + existingCount + " 条记录），请先确认是否需要重新拍摄。如需覆盖，请先清除旧快照。");
        }

        // 获取所有有进度数据的学生
        List<PrecisionProgress> allProgress = progressMapper.selectList(null);
        if (allProgress.isEmpty()) {
            throw new BusinessException(400, "precision_progress 表无数据，无法拍摄基线快照");
        }

        // 获取学生信息
        Set<Long> studentIds = allProgress.stream()
            .map(PrecisionProgress::getStudentId)
            .collect(Collectors.toSet());
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        Map<Long, Student> studentMap = students.stream()
            .collect(Collectors.toMap(Student::getId, s -> s));

        // 获取班级信息，仅保留参研班级（research_group 不为空）
        Set<Long> allClassIds = students.stream()
            .map(Student::getClassId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<Classes> allClasses = allClassIds.isEmpty() ? List.of()
            : classesMapper.selectBatchIds(allClassIds);
        Map<Long, Classes> classMap = allClasses.stream()
            .collect(Collectors.toMap(Classes::getId, c -> c));

        // 过滤：仅保留参研班级（EXPERIMENT/CONTROL）的学生
        Set<Long> researchStudentIds = students.stream()
            .filter(s -> {
                Classes cls = classMap.get(s.getClassId());
                return cls != null && cls.getResearchGroup() != null && !cls.getResearchGroup().isEmpty();
            })
            .map(Student::getId)
            .collect(Collectors.toSet());
        if (researchStudentIds.isEmpty()) {
            throw new BusinessException(400, "无参研班级学生（班级 research_group 字段均为空），请先在班级管理中设置课题组别后再拍摄基线快照");
        }

        // 获取学生姓名（从 users 表）
        Set<Long> userIds = students.stream()
            .map(Student::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> studentNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) {
                studentNameMap.put(u.getId(), u.getRealName());
            }
        }

        // 获取知识点信息（仅参研学生的知识点）
        Set<Long> nodeIds = allProgress.stream()
            .map(PrecisionProgress::getNodeId)
            .collect(Collectors.toSet());
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectBatchIds(nodeIds);
        Map<Long, KnowledgeNode> nodeMap = nodes.stream()
            .collect(Collectors.toMap(KnowledgeNode::getId, n -> n));

        // 批量创建基线快照记录
        List<ResearchBaseline> baselines = new ArrayList<>();
        for (PrecisionProgress p : allProgress) {
            // 跳过非参研班级学生
            if (!researchStudentIds.contains(p.getStudentId())) continue;
            Student student = studentMap.get(p.getStudentId());
            if (student == null) continue;

            Classes cls = classMap.get(student.getClassId());
            KnowledgeNode node = nodeMap.get(p.getNodeId());

            ResearchBaseline baseline = new ResearchBaseline();
            baseline.setStudentId(p.getStudentId());
            baseline.setStudentName(studentNameMap.get(student.getUserId()));
            baseline.setClassId(student.getClassId());
            baseline.setClassName(cls != null ? cls.getClassName() : null);
            baseline.setResearchGroup(cls != null ? cls.getResearchGroup() : null);
            baseline.setSubject(p.getSubject());
            baseline.setNodeId(p.getNodeId());
            baseline.setNodeName(node != null ? node.getName() : null);
            baseline.setNodeLevel(node != null ? node.getLevel() : 4);
            baseline.setMasteryPercent(p.getMasteryPercent() != null ? p.getMasteryPercent() : BigDecimal.ZERO);
            baseline.setTotalAttempts(p.getTotalAttempts() != null ? p.getTotalAttempts() : 0);
            baseline.setTotalCorrect(p.getTotalCorrect() != null ? p.getTotalCorrect() : 0);
            baseline.setStatus(p.getStatus());
            baseline.setSnapshotTime(snapshotTime);
            baseline.setSnapshotLabel(snapshotLabel);
            baseline.setSchoolId(1L);
            baselines.add(baseline);
        }

        // 批量插入（MyBatis-Plus saveBatch 内部走 ExecutorType.BATCH，500条/批flush）
        saveBatch(baselines, 500);

        log.info("基线快照完成: label={}, records={}", snapshotLabel, baselines.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("snapshotLabel", snapshotLabel);
        result.put("snapshotTime", snapshotTime.toString());
        result.put("totalRecords", baselines.size());
        result.put("studentCount", researchStudentIds.size());
        result.put("nodeCount", nodeIds.size());
        result.put("excludedNonResearch", allProgress.size() - baselines.size());
        return result;
    }

    @Override
    public byte[] exportBaselineCsv(String snapshotLabel, String researchGroup) {
        if (snapshotLabel == null || snapshotLabel.isEmpty()) {
            snapshotLabel = "PRETEST";
        }

        LambdaQueryWrapper<ResearchBaseline> qw = new LambdaQueryWrapper<ResearchBaseline>()
            .eq(ResearchBaseline::getSnapshotLabel, snapshotLabel);
        if (researchGroup != null && !researchGroup.isEmpty()) {
            qw.eq(ResearchBaseline::getResearchGroup, researchGroup);
        }
        qw.orderByAsc(ResearchBaseline::getClassName)
          .orderByAsc(ResearchBaseline::getStudentId)
          .orderByAsc(ResearchBaseline::getNodeId);
        List<ResearchBaseline> records = baseMapper.selectList(qw);

        if (records.isEmpty()) {
            throw new BusinessException(404, "无 '" + snapshotLabel + "' 标签的基线数据");
        }

        // CSV header
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM for Excel
        sb.append("学生ID,学生姓名,班级ID,班级名称,课题组别,学科,知识点ID,知识点名称,知识点层级,基线掌握度,答题次数,正确次数,状态,快照时间\n");

        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ResearchBaseline r : records) {
            sb.append(r.getStudentId()).append(",")
              .append(escape(r.getStudentName())).append(",")
              .append(r.getClassId() != null ? r.getClassId() : "").append(",")
              .append(escape(r.getClassName())).append(",")
              .append(escape(r.getResearchGroup())).append(",")
              .append(escape(r.getSubject())).append(",")
              .append(r.getNodeId()).append(",")
              .append(escape(r.getNodeName())).append(",")
              .append(r.getNodeLevel() != null ? r.getNodeLevel() : 4).append(",")
              .append(r.getMasteryPercent() != null ? r.getMasteryPercent() : 0).append(",")
              .append(r.getTotalAttempts() != null ? r.getTotalAttempts() : 0).append(",")
              .append(r.getTotalCorrect() != null ? r.getTotalCorrect() : 0).append(",")
              .append(escape(r.getStatus())).append(",")
              .append(r.getSnapshotTime() != null ? r.getSnapshotTime().format(dtFmt) : "").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, Object> getBaselineSummary(String snapshotLabel, String researchGroup) {
        if (snapshotLabel == null || snapshotLabel.isEmpty()) {
            snapshotLabel = "PRETEST";
        }

        LambdaQueryWrapper<ResearchBaseline> qw = new LambdaQueryWrapper<ResearchBaseline>()
            .eq(ResearchBaseline::getSnapshotLabel, snapshotLabel);
        if (researchGroup != null && !researchGroup.isEmpty()) {
            qw.eq(ResearchBaseline::getResearchGroup, researchGroup);
        }
        List<ResearchBaseline> records = baseMapper.selectList(qw);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("snapshotLabel", snapshotLabel);
        summary.put("totalRecords", records.size());

        if (records.isEmpty()) {
            summary.put("studentCount", 0);
            summary.put("avgMastery", 0);
            return summary;
        }

        // 去重学生数
        long studentCount = records.stream()
            .map(ResearchBaseline::getStudentId)
            .distinct().count();
        summary.put("studentCount", studentCount);

        // 平均掌握度
        double avgMastery = records.stream()
            .filter(r -> r.getMasteryPercent() != null)
            .mapToDouble(r -> r.getMasteryPercent().doubleValue())
            .average().orElse(0);
        summary.put("avgMastery", Math.round(avgMastery * 10) / 10.0);

        // 按学科统计
        Map<String, Long> bySubject = records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getSubject() != null ? r.getSubject() : "unknown",
                Collectors.counting()));
        summary.put("bySubject", bySubject);

        // 按课题组别统计
        Map<String, Long> byGroup = records.stream()
            .collect(Collectors.groupingBy(
                r -> r.getResearchGroup() != null ? r.getResearchGroup() : "UNSET",
                Collectors.counting()));
        summary.put("byResearchGroup", byGroup);

        // 快照时间
        summary.put("snapshotTime", records.get(0).getSnapshotTime());

        return summary;
    }

    @Override
    public Map<String, Object> validateMastery(Long taskId, String subject) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        // 1. 获取所有提交及其分数
        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .isNotNull(TaskSubmission::getScore));

        if (submissions.size() < 10) {
            throw new BusinessException(400, "有效提交数不足（需≥10），当前仅 " + submissions.size() + " 条");
        }

        // 2. 获取学生→userId映射
        Set<Long> studentIds = submissions.stream()
            .map(TaskSubmission::getStudentId).collect(Collectors.toSet());
        Map<Long, Long> studentUserIdMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            studentMapper.selectBatchIds(studentIds)
                .forEach(s -> studentUserIdMap.put(s.getId(), s.getUserId()));
        }

        // 3. 获取每个学生的masteryPercent（任务发布时间前后最接近的快照）
        Set<Long> userIds = new HashSet<>(studentUserIdMap.values());
        List<PrecisionProgress> allProgress = progressMapper.selectList(
            new LambdaQueryWrapper<PrecisionProgress>()
                .in(PrecisionProgress::getStudentId, studentIds)
                .eq(subject != null && !subject.isEmpty(), PrecisionProgress::getSubject, subject));
        // 按学生聚合平均掌握度
        Map<Long, Double> studentMasteryAvg = new HashMap<>();
        Map<Long, List<Double>> studentMasteryList = new HashMap<>();
        for (PrecisionProgress p : allProgress) {
            if (p.getMasteryPercent() != null) {
                studentMasteryList.computeIfAbsent(p.getStudentId(), k -> new ArrayList<>())
                    .add(p.getMasteryPercent().doubleValue());
            }
        }
        for (var e : studentMasteryList.entrySet()) {
            double avg = ScoreUtils.avgDouble(e.getValue());
            studentMasteryAvg.put(e.getKey(), avg);
        }

        // 4. 构建配对数据: (考试分数, masteryPercent均值)
        List<double[]> pairs = new ArrayList<>();
        for (TaskSubmission sub : submissions) {
            Double mastery = studentMasteryAvg.get(sub.getStudentId());
            if (mastery == null || sub.getScore() == null) continue;
            pairs.add(new double[]{sub.getScore().doubleValue(), mastery});
        }

        if (pairs.size() < 10) {
            throw new BusinessException(400, "有效配对数据不足（需≥10），当前仅 " + pairs.size() + " 对");
        }

        // 5. 计算 Pearson r
        int n = pairs.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        for (double[] p : pairs) {
            sumX += p[0]; sumY += p[1];
            sumXY += p[0] * p[1];
            sumX2 += p[0] * p[0]; sumY2 += p[1] * p[1];
        }
        double meanX = sumX / n, meanY = sumY / n;
        double num = sumXY - n * meanX * meanY;
        double den = Math.sqrt((sumX2 - n * meanX * meanX) * (sumY2 - n * meanY * meanY));
        double r = den == 0 ? 0 : num / den;
        double rSquared = Math.round(r * r * 1000.0) / 1000.0;
        r = Math.round(r * 1000.0) / 1000.0;

        // 6. 近似显著性判断（基于 |r| > 0.4 阈值）
        boolean thresholdMet = Math.abs(r) >= 0.4;

        // 7. 散点图数据（最多100点）
        List<Map<String, Object>> scatter = new ArrayList<>();
        for (int i = 0; i < Math.min(pairs.size(), 100); i++) {
            double[] p = pairs.get(i);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("score", p[0]);
            pt.put("mastery", Math.round(p[1] * 10.0) / 10.0);
            scatter.add(pt);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("taskTitle", task.getTitle());
        result.put("subject", subject != null ? subject : task.getSubject());
        result.put("sampleSize", n);
        result.put("pearsonR", r);
        result.put("rSquared", rSquared);
        result.put("thresholdMet", thresholdMet);
        result.put("interpretation", thresholdMet
            ? "Pearson r=" + r + " ≥ 0.4 — masteryPercent与标准化考试分数呈中等及以上相关，掌握度指标效度可接受。"
            : "Pearson r=" + r + " < 0.4 — masteryPercent与标准化考试分数相关性较弱，需审视掌握度指标的构成逻辑或考试的内容效度。");
        result.put("strength", Math.abs(r) >= 0.7 ? "强相关" : Math.abs(r) >= 0.4 ? "中等相关" : "弱相关");
        result.put("scatter", scatter);

        log.info("masteryPercent效度验证: taskId={}, n={}, r={}, thresholdMet={}", taskId, n, r, thresholdMet);
        return result;
    }

    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
