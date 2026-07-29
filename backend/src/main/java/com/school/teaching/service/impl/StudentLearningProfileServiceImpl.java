package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.mapper.*;
import com.school.teaching.service.StudentLearningProfileService;
import com.school.teaching.utils.ScoreUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentLearningProfileServiceImpl implements StudentLearningProfileService {

    @Autowired private TaskSubmissionMapper submissionMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private StudentAnswerMapper answerMapper;
    @Autowired private QuestionBankMapper questionBankMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private KnowledgeNodeMapper nodeMapper;
    @Autowired private com.school.teaching.service.CreditService creditService;

    @Override
    public Map<String, Object> getLearningProfile(Long studentId, String subject) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 获取该学生所有提交
        List<TaskSubmission> submissions = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getStudentId, studentId));

        // 获取关联的任务
        Map<Long, Task> taskMap = new HashMap<>();
        if (!submissions.isEmpty()) {
            Set<Long> taskIds = submissions.stream()
                .map(TaskSubmission::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (!taskIds.isEmpty()) {
                taskMapper.selectBatchIds(taskIds)
                    .forEach(t -> taskMap.put(t.getId(), t));
            }
        }

        // 学科过滤
        List<TaskSubmission> filteredSubs = submissions;
        if (subject != null && !subject.isEmpty()) {
            filteredSubs = submissions.stream()
                .filter(s -> {
                    Task t = taskMap.get(s.getTaskId());
                    return t != null && subject.equals(t.getSubject());
                })
                .collect(Collectors.toList());
        }

        // a. 总览数据
        Map<String, Object> overview = buildOverview(studentId, filteredSubs, taskMap);
        result.put("overview", overview);

        // b. 学科均分趋势（最近6个月）
        List<Map<String, Object>> scoreTrend = buildScoreTrend(submissions, taskMap);
        result.put("scoreTrend", scoreTrend);

        // c. 题型能力
        List<Map<String, Object>> typeAbility = buildTypeAbility(studentId, filteredSubs);
        result.put("typeAbility", typeAbility);

        // d. 最优/最弱知识点
        Map<String, List<Map<String, Object>>> knowledgeAnalysis = buildKnowledgeAnalysis(studentId, filteredSubs);
        result.put("topKnowledge", knowledgeAnalysis.get("top"));
        result.put("weakKnowledge", knowledgeAnalysis.get("weak"));

        // e. 提交行为
        Map<String, Object> submitBehavior = buildSubmitBehavior(filteredSubs, taskMap);
        result.put("submitBehavior", submitBehavior);

        return result;
    }

    private Map<String, Object> buildOverview(Long studentId, List<TaskSubmission> submissions, Map<Long, Task> taskMap) {
        Map<String, Object> overview = new LinkedHashMap<>();
        int totalTasks = submissions.size();
        overview.put("totalTasks", totalTasks);

        long completedCount = submissions.stream()
            .filter(s -> "SUBMITTED".equals(s.getStatus()) || "GRADED".equals(s.getStatus()))
            .count();
        double completionRate = totalTasks > 0 ? (double) completedCount / totalTasks : 0;
        overview.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        double avgScore = 0;
        int scoreCount = 0;
        for (TaskSubmission s : submissions) {
            if (s.getScore() != null) {
                avgScore += s.getScore().doubleValue();
                scoreCount++;
            }
        }
        overview.put("avgScore", scoreCount > 0 ? Math.round(avgScore / scoreCount * 10.0) / 10.0 : 0);

        // 积分排名
        try {
            Map<String, Object> creditInfo = creditService.getCreditInfo(studentId);
            if (creditInfo != null) {
                overview.put("totalPoints", creditInfo.getOrDefault("balance", 0));
                // 排名从排行榜获取
                List<Map<String, Object>> ranking = creditService.getRanking("ALL", 9999, null, null, null);
                int rank = -1;
                for (int i = 0; i < ranking.size(); i++) {
                    Object sid = ranking.get(i).get("studentId");
                    if (sid != null && sid.toString().equals(studentId.toString())) {
                        rank = i + 1;
                        break;
                    }
                }
                overview.put("ranking", rank > 0 ? rank : null);
            } else {
                overview.put("ranking", null);
                overview.put("totalPoints", 0);
            }
        } catch (Exception e) {
            overview.put("ranking", null);
            overview.put("totalPoints", 0);
        }

        // 薄弱知识点数量
        int weakCount = 0;
        try {
            // 使用submissions对应的questionIds查错题统计
            weakCount = submissions.size() > 0 ? 1 : 0; // placeholder
        } catch (Exception e) { /* ignore */ }
        overview.put("weakKnowledgeCount", weakCount);

        return overview;
    }

    private List<Map<String, Object>> buildScoreTrend(List<TaskSubmission> submissions, Map<Long, Task> taskMap) {
        // 按月份+学科聚合AVG(score)
        Map<String, Map<String, List<Double>>> monthSubjectScores = new LinkedHashMap<>();
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        for (TaskSubmission s : submissions) {
            if (s.getScore() == null || s.getSubmittedAt() == null) continue;
            if (s.getSubmittedAt().isBefore(sixMonthsAgo)) continue;
            Task t = taskMap.get(s.getTaskId());
            if (t == null || t.getSubject() == null) continue;

            String month = s.getSubmittedAt().toLocalDate().toString().substring(0, 7);
            monthSubjectScores
                .computeIfAbsent(month, k -> new LinkedHashMap<>())
                .computeIfAbsent(t.getSubject(), k -> new ArrayList<>())
                .add(s.getScore().doubleValue());
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Double>>> monthEntry : monthSubjectScores.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", monthEntry.getKey());
            for (Map.Entry<String, List<Double>> subjEntry : monthEntry.getValue().entrySet()) {
                double avg = ScoreUtils.avgDouble(subjEntry.getValue());
                item.put(subjEntry.getKey(), Math.round(avg * 10.0) / 10.0);
            }
            trend.add(item);
        }
        trend.sort(Comparator.comparing(m -> (String) m.get("month")));

        return trend;
    }

    private List<Map<String, Object>> buildTypeAbility(Long studentId, List<TaskSubmission> submissions) {
        if (submissions.isEmpty()) return List.of();

        Set<Long> subIds = submissions.stream().map(TaskSubmission::getId).collect(Collectors.toSet());

        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>()
                .in(StudentAnswer::getSubmissionId, subIds));

        if (answers.isEmpty()) return List.of();

        Set<Long> qids = answers.stream().map(StudentAnswer::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        if (!qids.isEmpty()) {
            questionBankMapper.selectBatchIds(qids).forEach(q -> qMap.put(q.getId(), q));
        }

        Map<String, int[]> typeStats = new LinkedHashMap<>(); // [correct, total]
        for (StudentAnswer a : answers) {
            QuestionBank q = qMap.get(a.getQuestionId());
            if (q == null || q.getQuestionType() == null) continue;
            String type = q.getQuestionType();
            int[] stats = typeStats.computeIfAbsent(type, k -> new int[2]);
            stats[1]++;
            if (a.getIsCorrect() != null && a.getIsCorrect() == 1) stats[0]++;
        }

        return typeStats.entrySet().stream()
            .map(e -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", e.getKey());
                item.put("accuracy", e.getValue()[1] > 0 ? Math.round((double) e.getValue()[0] / e.getValue()[1] * 100.0) / 100.0 : 0);
                item.put("total", e.getValue()[1]);
                return item;
            })
            .sorted((a, b) -> Double.compare((Double) b.get("accuracy"), (Double) a.get("accuracy")))
            .collect(Collectors.toList());
    }

    private Map<String, List<Map<String, Object>>> buildKnowledgeAnalysis(Long studentId, List<TaskSubmission> submissions) {
        List<Map<String, Object>> top = new ArrayList<>();
        List<Map<String, Object>> weak = new ArrayList<>();

        if (submissions.isEmpty()) return Map.of("top", top, "weak", weak);

        Set<Long> subIds = submissions.stream().map(TaskSubmission::getId).collect(Collectors.toSet());

        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>()
                .in(StudentAnswer::getSubmissionId, subIds));

        if (answers.isEmpty()) return Map.of("top", top, "weak", weak);

        Set<Long> qids = answers.stream().map(StudentAnswer::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = new HashMap<>();
        if (!qids.isEmpty()) {
            questionBankMapper.selectBatchIds(qids).forEach(q -> qMap.put(q.getId(), q));
        }

        // 按categoryId聚合
        Map<Long, List<Boolean>> nodeCorrects = new LinkedHashMap<>();
        for (StudentAnswer a : answers) {
            QuestionBank q = qMap.get(a.getQuestionId());
            if (q == null || q.getCategoryId() == null) continue;
            nodeCorrects.computeIfAbsent(q.getCategoryId(), k -> new ArrayList<>())
                .add(a.getIsCorrect() != null && a.getIsCorrect() == 1);
        }

        // 查知识节点名称
        Map<Long, String> nodeNameMap = new HashMap<>();
        if (!nodeCorrects.isEmpty()) {
            nodeMapper.selectBatchIds(nodeCorrects.keySet())
                .forEach(n -> nodeNameMap.put(n.getId(), n.getName()));
        }

        // 计算每个节点的正确率并排序
        List<Map.Entry<Long, Double>> nodeRates = new ArrayList<>();
        for (Map.Entry<Long, List<Boolean>> entry : nodeCorrects.entrySet()) {
            List<Boolean> list = entry.getValue();
            if (list.isEmpty()) continue;
            long correct = list.stream().filter(Boolean::booleanValue).count();
            double rate = (double) correct / list.size();
            nodeRates.add(new AbstractMap.SimpleEntry<>(entry.getKey(), rate));
        }
        nodeRates.sort(Map.Entry.comparingByValue());

        // TOP5
        for (int i = nodeRates.size() - 1; i >= Math.max(0, nodeRates.size() - 5); i--) {
            Map.Entry<Long, Double> e = nodeRates.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", nodeNameMap.getOrDefault(e.getKey(), "知识点" + e.getKey()));
            item.put("accuracy", Math.round(e.getValue() * 100.0) / 100.0);
            item.put("knowledgeNodeId", e.getKey());
            top.add(item);
        }

        // BOTTOM5
        for (int i = 0; i < Math.min(5, nodeRates.size()); i++) {
            Map.Entry<Long, Double> e = nodeRates.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", nodeNameMap.getOrDefault(e.getKey(), "知识点" + e.getKey()));
            item.put("accuracy", Math.round(e.getValue() * 100.0) / 100.0);
            item.put("knowledgeNodeId", e.getKey());
            weak.add(item);
        }

        return Map.of("top", top, "weak", weak);
    }

    private Map<String, Object> buildSubmitBehavior(List<TaskSubmission> submissions, Map<Long, Task> taskMap) {
        int onTime = 0, late = 0, missing = 0;

        for (TaskSubmission s : submissions) {
            if ("SUBMITTED".equals(s.getStatus()) || "GRADED".equals(s.getStatus())) {
                Task t = taskMap.get(s.getTaskId());
                if (t != null && t.getDeadline() != null && s.getSubmittedAt() != null) {
                    if (s.getSubmittedAt().isAfter(t.getDeadline())) {
                        late++;
                    } else {
                        onTime++;
                    }
                } else {
                    onTime++;
                }
            } else if ("PENDING".equals(s.getStatus())) {
                Task t = taskMap.get(s.getTaskId());
                if (t != null && t.getDeadline() != null && LocalDateTime.now().isAfter(t.getDeadline())) {
                    missing++;
                }
            }
        }

        return Map.of("onTime", onTime, "late", late, "missing", missing);
    }
}
