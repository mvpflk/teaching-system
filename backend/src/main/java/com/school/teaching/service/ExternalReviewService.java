package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExternalReviewService {

    private static final Logger log = LoggerFactory.getLogger(ExternalReviewService.class);

    private final ExternalReviewMapper externalReviewMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final RubricDimensionMapper dimMapper;

    /** 教师生成外部评阅链接 */
    @Transactional
    public ExternalReview generateLink(Long taskId, Long submissionId, int validHours, String reviewerName) {
        ExternalReview r = new ExternalReview();
        r.setTaskId(taskId); r.setSubmissionId(submissionId);
        r.setToken(UUID.randomUUID().toString().replace("-", ""));
        r.setReviewerName(reviewerName);
        r.setStatus("PENDING");
        r.setExpiresAt(LocalDateTime.now().plusHours(validHours > 0 ? validHours : 72));
        externalReviewMapper.insert(r);
        return r;
    }

    /** 通过 token 获取评阅信息（公开访问） */
    public Map<String, Object> getByToken(String token) {
        ExternalReview r = externalReviewMapper.selectOne(
            new LambdaQueryWrapper<ExternalReview>().eq(ExternalReview::getToken, token));
        if (r == null) throw new BusinessException(404, "链接无效");
        if (r.getExpiresAt().isBefore(LocalDateTime.now())) throw new BusinessException(410, "链接已过期");
        if ("SUBMITTED".equals(r.getStatus())) throw new BusinessException(409, "该链接已提交过评分");

        TaskSubmission sub = submissionMapper.selectById(r.getSubmissionId());
        if (sub == null) throw new BusinessException(404, "提交不存在");
        Task task = taskMapper.selectById(r.getTaskId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("reviewerName", r.getReviewerName());
        result.put("content", sub.getContent());
        result.put("taskTitle", task != null ? task.getTitle() : "");
        result.put("scoreType", task != null ? task.getScoreType() : "POINT_100");
        result.put("totalScore", task != null ? task.getTotalScore() : null);
        if (task != null && task.getRubricId() != null) {
            result.put("dimensions", dimMapper.selectList(
                new LambdaQueryWrapper<RubricDimension>().eq(RubricDimension::getRubricId, task.getRubricId())));
        }
        return result;
    }

    /** 外部评审人提交评分 */
    @Transactional
    public void submitReview(String token, Map<String, Object> scoreData) {
        ExternalReview r = externalReviewMapper.selectOne(
            new LambdaQueryWrapper<ExternalReview>().eq(ExternalReview::getToken, token));
        if (r == null) throw new BusinessException(404, "链接无效");
        if (r.getExpiresAt().isBefore(LocalDateTime.now())) throw new BusinessException(410, "链接已过期");
        if ("SUBMITTED".equals(r.getStatus())) throw new BusinessException(409, "已提交");

        try {
            r.setScoreJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(scoreData));
        } catch (Exception e) { throw new BusinessException(500, "数据保存失败"); }
        r.setStatus("SUBMITTED");
        r.setSubmittedAt(LocalDateTime.now());
        externalReviewMapper.updateById(r);
    }

    /** 教师查看某任务的外部评阅列表 */
    public List<ExternalReview> listByTask(Long taskId) {
        return externalReviewMapper.selectList(
            new LambdaQueryWrapper<ExternalReview>().eq(ExternalReview::getTaskId, taskId));
    }

    /** 教师将外部评阅合并到提交分数 */
    @Transactional
    public void mergeToScore(Long reviewId, double weight) {
        ExternalReview r = externalReviewMapper.selectById(reviewId);
        if (r == null || r.getScoreJson() == null) return;
        TaskSubmission sub = submissionMapper.selectById(r.getSubmissionId());
        if (sub == null) return;

        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            var data = om.readValue(r.getScoreJson(), Map.class);
            double extScore = data.get("totalScore") instanceof Number n ? n.doubleValue() : 0;
            double teacherScore = sub.getScore() != null ? sub.getScore().doubleValue() : 0;
            double merged = teacherScore * (1 - weight) + extScore * weight;
            sub.setScore(java.math.BigDecimal.valueOf(merged).setScale(1, java.math.RoundingMode.HALF_UP));
            submissionMapper.updateById(sub);
        } catch (Exception e) { throw new BusinessException(500, "合并失败"); }
    }

    /** 批量合并某任务的所有未合并外部评阅 */
    @Transactional
    public int batchMergeByTask(Long taskId, double weight) {
        List<ExternalReview> reviews = externalReviewMapper.selectList(
            new LambdaQueryWrapper<ExternalReview>().eq(ExternalReview::getTaskId, taskId));
        int count = 0;
        for (ExternalReview r : reviews) {
            try { mergeToScore(r.getId(), weight); count++; } catch (Exception ignored) { log.error("合并外部评阅失败 reviewId={}", r.getId(), ignored); }
        }
        return count;
    }
}
