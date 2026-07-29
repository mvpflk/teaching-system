package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.PeerReviewService;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.utils.ScoreUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeerReviewServiceImpl implements PeerReviewService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final PeerReviewMapper peerReviewMapper;
    private final StudentMapper studentMapper;
    private final RubricDimensionMapper dimMapper;

    /** 随机分配互评 — 每人评 N 份，排除自己和已评 */
    @Override @Transactional
    public int assignReviews(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        // 从 task_config 读互评配置
        int reviewsPerStudent = 3;
        boolean anonymous = true;
        try {
            if (task.getTaskConfig() != null) {
                var om = new com.fasterxml.jackson.databind.ObjectMapper();
                var cfg = om.readValue(task.getTaskConfig(), Map.class);
                var peer = (Map<?,?>) cfg.get("peer_review");
                if (peer != null) {
                    if (peer.get("reviews_per_student") instanceof Number n) reviewsPerStudent = n.intValue();
                }
            }
        } catch (Exception e) {
            log.warn("互评配置解析失败: taskId={}", taskId, e);
        }

        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId)
                .ne(TaskSubmission::getStatus, "EXEMPTED"));
        if (subs.size() < reviewsPerStudent + 1)
            throw new BusinessException(400, "提交数不足，无法启动互评（需至少" + (reviewsPerStudent + 1) + "份）");

        // 检查是否有已提交的互评，有则拒绝重置
        long submittedCount = peerReviewMapper.selectCount(
            new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getTaskId, taskId)
                .isNotNull(PeerReview::getSubmittedAt));
        if (submittedCount > 0)
            throw new BusinessException(409, "已有 " + submittedCount + " 份互评已提交，不可重新分配。如需重分请先联系管理员清除数据");

        // 清除已有未提交的分配
        peerReviewMapper.delete(new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getTaskId, taskId));

        List<Long> studentIds = subs.stream().map(TaskSubmission::getStudentId).distinct().toList();
        Map<Long, Long> studentToSub = subs.stream()
            .collect(Collectors.toMap(TaskSubmission::getStudentId, TaskSubmission::getId, (a, b) -> a));

        // 均衡分配：每人review N份，同时确保每人被review约N次
        Map<Long, Integer> reviewCount = new HashMap<>(); // 每个学生被评次数
        studentIds.forEach(sid -> reviewCount.put(sid, 0));
        List<Long> pool = new ArrayList<>(studentIds);
        int count = 0;

        List<PeerReview> batch = new ArrayList<>();
        for (Long reviewer : studentIds) {
            List<Long> candidates = new ArrayList<>(pool);
            candidates.remove(reviewer);
            candidates.sort(Comparator.comparingInt(reviewCount::get));
            int assigned = 0;
            for (Long target : candidates) {
                if (assigned >= reviewsPerStudent) break;
                PeerReview pr = new PeerReview();
                pr.setTaskId(taskId);
                pr.setReviewerId(reviewer);
                pr.setSubmissionId(studentToSub.get(target));
                batch.add(pr);
                reviewCount.merge(target, 1, Integer::sum);
                assigned++; count++;
            }
        }
        for (PeerReview pr : batch) peerReviewMapper.insert(pr);
        return count;
    }

    @Override
    public List<Map<String, Object>> getPendingReviews(Long studentId) {
        List<PeerReview> reviews = peerReviewMapper.selectList(
            new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getReviewerId, studentId)
                .isNull(PeerReview::getSubmittedAt));
        if (reviews.isEmpty()) return List.of();

        Set<Long> subIds = reviews.stream().map(PeerReview::getSubmissionId).collect(Collectors.toSet());
        Map<Long, TaskSubmission> subMap = submissionMapper.selectBatchIds(subIds).stream()
            .collect(Collectors.toMap(TaskSubmission::getId, s -> s));
        Set<Long> taskIds = subMap.values().stream().map(TaskSubmission::getTaskId).collect(Collectors.toSet());
        Map<Long, Task> taskMap = taskMapper.selectBatchIds(taskIds).stream()
            .collect(Collectors.toMap(Task::getId, t -> t));
        // 批量加载量规维度
        Set<Long> rubricIds = taskMap.values().stream().map(Task::getRubricId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, List<RubricDimension>> allDims = rubricIds.isEmpty() ? Map.of() :
            dimMapper.selectList(new LambdaQueryWrapper<RubricDimension>().in(RubricDimension::getRubricId, rubricIds))
                .stream().collect(Collectors.groupingBy(RubricDimension::getRubricId));
        Map<Long, List<RubricDimension>> rubricDims = new HashMap<>();
        for (Task t : taskMap.values()) {
            if (t.getRubricId() != null) rubricDims.put(t.getId(), allDims.getOrDefault(t.getRubricId(), List.of()));
        }

        // 批量加载被评人的姓名
        Set<Long> revieweeIds = subMap.values().stream().map(TaskSubmission::getStudentId).collect(Collectors.toSet());
        Map<Long, String> revieweeNames = new HashMap<>();
        if (!revieweeIds.isEmpty()) {
            List<Student> reviewees = studentMapper.selectList(new LambdaQueryWrapper<Student>().in(Student::getId, revieweeIds));
            reviewees.forEach(s -> revieweeNames.put(s.getId(), s.getStudentNumber() + " " + (s.getUserId() != null ? "" : "")));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PeerReview r : reviews) {
            TaskSubmission sub = subMap.get(r.getSubmissionId());
            if (sub == null) continue;
            Task t = taskMap.get(sub.getTaskId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewId", r.getId());
            item.put("taskTitle", t != null ? t.getTitle() : "");
            item.put("scoreType", t != null ? t.getScoreType() : "POINT_100");
            // 优先用 content，为空时用得分信息作为摘要
            String summary = sub.getContent();
            if (summary == null || summary.isBlank()) {
                summary = "得分: " + (sub.getScore() != null ? sub.getScore() : "未评分")
                    + " / 满分: " + (t != null && t.getTotalScore() != null ? t.getTotalScore() : "100");
            }
            item.put("content", summary);
            item.put("studentScore", sub.getScore());
            item.put("totalScore", t != null ? t.getTotalScore() : 100);
            // 有量规则给维度，否则给简单维度（总分）
            List<Map<String, Object>> dims = new ArrayList<>();
            List<RubricDimension> rd = rubricDims.getOrDefault(sub.getTaskId(), List.of());
            if (!rd.isEmpty()) {
                rd.forEach(d -> dims.add(Map.of("id", d.getId(), "name", d.getName(), "weight", d.getWeight() != null ? d.getWeight().doubleValue() : 1.0, "maxScore", 100)));
            } else {
                dims.add(Map.of("id", 0, "name", "总分", "weight", 1.0, "maxScore", t != null && t.getTotalScore() != null ? t.getTotalScore() : 100));
            }
            item.put("dimensions", dims);
            item.put("instructions", !rd.isEmpty()
                ? "请根据各项维度认真评分，评分质量将影响你的互评信用"
                : "请根据作品质量给出合理分数，公平公正评分");
            result.add(item);
        }
        return result;
    }

    @Override @Transactional
    public Map<String, Object> submitReview(Long reviewId, Map<String, Object> scoreData, Long reviewerId) {
        PeerReview r = peerReviewMapper.selectById(reviewId);
        if (r == null) throw new BusinessException(404, "互评记录不存在");
        if (!r.getReviewerId().equals(reviewerId)) throw new BusinessException(403, "无权评分");
        if (r.getSubmittedAt() != null) throw new BusinessException(409, "已提交过评分");

        try {
            r.setScoreJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(scoreData));
        } catch (Exception e) { throw new BusinessException(500, "评分数据序列化失败"); }
        r.setSubmittedAt(LocalDateTime.now());
        peerReviewMapper.updateById(r);

        // 计算该提交的互评均分
        TaskSubmission sub = submissionMapper.selectById(r.getSubmissionId());
        if (sub != null) {
            List<PeerReview> all = peerReviewMapper.selectList(
                new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getSubmissionId, sub.getId())
                    .isNotNull(PeerReview::getSubmittedAt));
            BigDecimal sum = BigDecimal.ZERO;
            for (PeerReview pr : all) {
                try {
                    var om = new com.fasterxml.jackson.databind.ObjectMapper();
                    var data = om.readValue(pr.getScoreJson(), Map.class);
                    if (data.get("totalScore") instanceof Number n) sum = sum.add(BigDecimal.valueOf(n.doubleValue()));
                } catch (Exception e) {
                    log.warn("互评分数解析失败: reviewId={}", pr.getId(), e);
                }
            }
            if (!all.isEmpty()) {
                sub.setPeerScore(sum.divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP));
                submissionMapper.updateById(sub);
            }
        }
        return Map.of("reviewId", reviewId, "submitted", true);
    }

    @Override
    public Map<String, Object> getProgress(Long taskId) {
        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
        List<PeerReview> reviews = peerReviewMapper.selectList(
            new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getTaskId, taskId));

        long totalReviews = reviews.size();
        long completed = reviews.stream().filter(r -> r.getSubmittedAt() != null).count();
        Map<Long, Long> perSubmission = reviews.stream()
            .collect(Collectors.groupingBy(PeerReview::getSubmissionId, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStudents", subs.size());
        result.put("totalReviews", totalReviews);
        result.put("completedReviews", completed);
        result.put("progress", totalReviews > 0 ? (double) completed / totalReviews : 0);
        result.put("perSubmission", perSubmission);
        return result;
    }

    @Override
    public List<Map<String, Object>> getReviewDetails(Long submissionId) {
        List<PeerReview> reviews = peerReviewMapper.selectList(
            new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getSubmissionId, submissionId)
                .orderByDesc(PeerReview::getSubmittedAt));
        // 检查是否匿名模式
        boolean anonymous = isAnonymousMode(reviews.isEmpty() ? null : reviews.get(0).getTaskId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (PeerReview r : reviews) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewId", r.getId());
            item.put("reviewerId", anonymous ? null : r.getReviewerId());
            item.put("submittedAt", r.getSubmittedAt());
            try {
                if (r.getScoreJson() != null)
                    item.put("scores", new com.fasterxml.jackson.databind.ObjectMapper().readValue(r.getScoreJson(), Map.class));
            } catch (Exception e) {
                log.warn("互评详情分数解析失败: reviewId={}", r.getId(), e);
            }
            result.add(item);
        }
        return result;
    }

    @Override @Transactional
    public int fuseScores(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        double peerWeight = 0.3;
        try {
            if (task.getTaskConfig() != null) {
                var om = new com.fasterxml.jackson.databind.ObjectMapper();
                var cfg = om.readValue(task.getTaskConfig(), Map.class);
                var peer = (Map<?,?>) cfg.get("peer_review");
                if (peer != null && peer.get("weight") instanceof Number n) peerWeight = n.doubleValue();
            }
        } catch (Exception e) {
            log.warn("互评权重配置解析失败: taskId={}", taskId, e);
        }

        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
        int updated = 0;
        for (TaskSubmission sub : subs) {
            if (sub.getScore() == null && sub.getPeerScore() == null) continue;
            BigDecimal teacher = sub.getScore() != null ? sub.getScore() : BigDecimal.ZERO;
            BigDecimal peer = sub.getPeerScore() != null ? sub.getPeerScore() : BigDecimal.ZERO;
            BigDecimal fused = teacher.multiply(BigDecimal.valueOf(1 - peerWeight))
                .add(peer.multiply(BigDecimal.valueOf(peerWeight)))
                .setScale(1, RoundingMode.HALF_UP);
            // 保留教师原始分到 scoreJson 后再覆盖 score（前端展示融合分，scoreJson 可回溯）
            try {
                var om = new com.fasterxml.jackson.databind.ObjectMapper();
                sub.setScoreJson(om.writeValueAsString(Map.of(
                    "teacherScore", teacher, "peerAvgScore", peer,
                    "peerWeight", peerWeight, "fusedScore", fused)));
            } catch (Exception ignored) { log.warn("评分JSON序列化失败: {}", ignored.getMessage()); }
            sub.setScore(fused);
            sub.setPeerScore(fused);
            submissionMapper.updateById(sub);
            updated++;
        }
        return updated;
    }

    // ── 互评质量分析 ──────────────────────────
    @Override
    public Map<String, Object> getQualityAnalysis(Long taskId) {
        List<PeerReview> all = peerReviewMapper.selectList(
            new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getTaskId, taskId)
                .isNotNull(PeerReview::getSubmittedAt));
        if (all.isEmpty()) return Map.of("hasData", false);

        // 班级均分
        double classAvg = 0; int totalScores = 0;
        Map<Long, List<Double>> reviewerScores = new HashMap<>();
        for (PeerReview pr : all) {
            Double score = extractTotalScore(pr.getScoreJson());
            if (score == null) continue;
            classAvg += score; totalScores++;
            reviewerScores.computeIfAbsent(pr.getReviewerId(), k -> new ArrayList<>()).add(score);
        }
        if (totalScores == 0) return Map.of("hasData", false);
        classAvg /= totalScores;

        // 加载学生学号
        Set<Long> reviewerIds = reviewerScores.keySet();
        Map<Long, String> studentLabels = new HashMap<>();
        if (!reviewerIds.isEmpty()) {
            studentMapper.selectList(new LambdaQueryWrapper<Student>().in(Student::getId, reviewerIds))
                .forEach(s -> studentLabels.put(s.getId(), s.getStudentNumber() != null ? s.getStudentNumber() : "学生" + s.getId()));
        }

        List<Map<String, Object>> reviewers = new ArrayList<>();
        for (var entry : reviewerScores.entrySet()) {
            Long rid = entry.getKey();
            List<Double> scores = entry.getValue();
            double avg = ScoreUtils.avgDouble(scores);
            double deviation = Math.round((avg - classAvg) * 10) / 10.0;
            long fullCount = scores.stream().filter(s -> s >= 100).count();
            long zeroCount = scores.stream().filter(s -> s <= 0).count();
            double stdDev = scores.size() > 1 ? ScoreUtils.stdDev(scores) : 0;

            List<String> flags = new ArrayList<>();
            if (Math.abs(deviation) > 20) flags.add("评分偏离大(" + (deviation > 0 ? "偏高" : "偏低") + ")");
            if (fullCount >= scores.size() * 0.8) flags.add("疑似敷衍(全满分)");
            if (zeroCount >= scores.size() * 0.8) flags.add("疑似敷衍(全零分)");
            if (stdDev < 1 && scores.size() >= 3) flags.add("区分度低(分数过于集中)");

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("reviewerId", rid);
            r.put("reviewerName", studentLabels.getOrDefault(rid, "学生" + rid));
            r.put("reviewCount", scores.size());
            r.put("avgScore", Math.round(avg * 10) / 10.0);
            r.put("classAvg", Math.round(classAvg * 10) / 10.0);
            r.put("deviation", deviation);
            r.put("fullMarkCount", fullCount);
            r.put("zeroCount", zeroCount);
            r.put("flags", flags);
            reviewers.add(r);
        }

        return Map.of("hasData", true, "classAvg", Math.round(classAvg * 10) / 10.0,
            "totalReviewers", reviewers.size(), "reviewers", reviewers);
    }

    private Double extractTotalScore(String scoreJson) {
        if (scoreJson == null) return null;
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readTree(scoreJson).get("totalScore").asDouble(); }
        catch (Exception e) { return null; }
    }

    private boolean isAnonymousMode(Long taskId) {
        if (taskId == null) return true;
        try {
            Task t = taskMapper.selectById(taskId);
            if (t != null && t.getTaskConfig() != null) {
                var cfg = new com.fasterxml.jackson.databind.ObjectMapper().readValue(t.getTaskConfig(), Map.class);
                var peer = (Map<?,?>) cfg.get("peer_review");
                if (peer != null && peer.get("anonymous") instanceof Boolean b) return b;
            }
        } catch (Exception ignored) { log.warn("任务配置解析失败: taskId={}", taskId); }
        return true; // 默认匿名
    }

    // ── 学生查看互评评语 ──────────────────────
    @Override
    public List<Map<String, Object>> getPeerCommentsForStudent(Long submissionId, Long studentId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) return List.of();
        // 仅提交者本人可看
        if (!sub.getStudentId().equals(studentId)) return List.of();

        List<PeerReview> reviews = peerReviewMapper.selectList(
            new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getSubmissionId, submissionId)
                .isNotNull(PeerReview::getSubmittedAt).orderByDesc(PeerReview::getSubmittedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PeerReview r : reviews) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewId", r.getId());
            try {
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.getScoreJson());
                item.put("score", node.has("totalScore") ? node.get("totalScore").asDouble() : null);
                item.put("comment", node.has("comment") ? node.get("comment").asText() : "");
            } catch (Exception e) {
                item.put("score", null); item.put("comment", "");
            }
            item.put("submittedAt", r.getSubmittedAt());
            result.add(item);
        }
        return result;
    }

    // ── 互评截止提醒 ──────────────────────────
    @Override
    public List<Map<String, Object>> findPendingReminders() {
        // 查所有有互评分配的任务，deadline在24h内
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .in(Task::getTaskType, List.of("SUMMATIVE", "FORMATIVE"))
            .in(Task::getStatus, List.of("PUBLISHED", "ONGOING"))
            .isNotNull(Task::getDeadline));
        if (tasks.isEmpty()) return List.of();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusHours(24);

        List<Map<String, Object>> reminders = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getDeadline().isBefore(now) || t.getDeadline().isAfter(cutoff)) continue;
            // 查该任务有互评分配但未提交的学生
            List<PeerReview> pending = peerReviewMapper.selectList(
                new LambdaQueryWrapper<PeerReview>().eq(PeerReview::getTaskId, t.getId())
                    .isNull(PeerReview::getSubmittedAt));
            if (pending.isEmpty()) continue;

            Set<Long> reminderIds = pending.stream().map(PeerReview::getReviewerId).collect(Collectors.toSet());
            // 查这些学生的 userId
            List<Student> students = studentMapper.selectBatchIds(reminderIds);
            for (Student s : students) {
                if (s.getUserId() == null) continue;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("userId", s.getUserId());
                r.put("taskId", t.getId());
                r.put("taskTitle", t.getTitle());
                r.put("deadline", t.getDeadline().toString());
                reminders.add(r);
            }
        }
        return reminders;
    }

    @Override
    public boolean isSubmissionOwner(Long submissionId, Long studentId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        return sub != null && studentId.equals(sub.getStudentId());
    }
}
