package com.school.teaching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.common.*;
import com.school.teaching.common.handler.QuestionTypeHandlerRegistry;
import com.school.teaching.entity.*;
import com.school.teaching.event.TaskEvent;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 评分流水线引擎 — 自动评分 + 维度评分 + 传统直接打分，统一出口。
 */
@Service
@RequiredArgsConstructor
public class GradingEngine {

    private static final Logger log = LoggerFactory.getLogger(GradingEngine.class);

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final StudentAnswerMapper answerMapper;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionTypeHandlerRegistry handlerRegistry;
    private final RubricDimensionMapper dimMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ── 自动评分 ──────────────────────────────────────

    /** 对一份提交的客观题进行自动评分，结果写入 student_answers.score_json */
    @Transactional
    public int autoGrade(Long submissionId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");

        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getSubmissionId, submissionId));
        if (answers.isEmpty()) return 0;

        List<Long> qIds = answers.stream().map(StudentAnswer::getQuestionId).toList();
        Map<Long, QuestionBank> qMap = questionBankMapper.selectBatchIds(qIds).stream()
            .collect(java.util.stream.Collectors.toMap(QuestionBank::getId, q -> q));

        int scored = 0;
        for (StudentAnswer a : answers) {
            QuestionBank q = qMap.get(a.getQuestionId());
            if (q == null) continue;
            QuestionTypeEnum type = QuestionTypeEnum.fromString(q.getQuestionType());
            if (type == null || !type.isObjective()) continue;

            QuestionTypeHandler handler = handlerRegistry.getOrNull(type);
            if (handler == null) continue;

            BigDecimal score = handler.scoreAnswer(q, a.getStudentAnswer());
            if (score != null) {
                a.setAutoScore(score);
                a.setIsCorrect(score.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
                answerMapper.updateById(a);
                scored++;
            }
        }
        return scored;
    }

    // ── 维度评分（Rubric） ──────────────────────────

    /**
     * 教师按维度评分。
     * @param submissionId 提交 ID
     * @param dimensionScores key=dimensionId, value=得分(0~levelsJson定义的最大值)
     * @param comment 评语
     * @param teacherId 评分教师ID
     */
    @Transactional
    public TaskSubmission gradeByRubric(Long submissionId, Map<Long, BigDecimal> dimensionScores,
                                         String comment, Long teacherId) {
        if (!SecurityUtils.isTeacherOrAdmin()) throw new BusinessException(403, "无评分权限");

        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");

        Task task = taskMapper.selectById(sub.getTaskId());
        if (task == null) throw new BusinessException(404, "任务不存在");

        // 加载量规维度
        List<RubricDimension> dims;
        if (task.getRubricId() != null) {
            dims = dimMapper.selectList(
                new LambdaQueryWrapper<RubricDimension>().eq(RubricDimension::getRubricId, task.getRubricId()));
        } else {
            dims = List.of();
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        Map<String, Object> scoreDetail = new LinkedHashMap<>();

        if (!dims.isEmpty()) {
            // 加权计算
            for (RubricDimension dim : dims) {
                BigDecimal score = dimensionScores.getOrDefault(dim.getId(), BigDecimal.ZERO);
                BigDecimal weighted = score.multiply(dim.getWeight()).setScale(2, RoundingMode.HALF_UP);
                totalScore = totalScore.add(weighted);
                Map<String, Object> ds = new LinkedHashMap<>();
                ds.put("name", dim.getName()); ds.put("score", score);
                ds.put("weight", dim.getWeight()); ds.put("weighted", weighted);
                scoreDetail.put(String.valueOf(dim.getId()), ds);
            }
        } else {
            // 传统模式：无维度，直接取传过来的分数
            totalScore = dimensionScores.getOrDefault(0L, BigDecimal.ZERO);
            scoreDetail.put("direct", Map.of("score", totalScore));
        }

        scoreDetail.put("comment", comment != null ? comment : "");

        try {
            sub.setScoreJson(JsonUtils.toJson(scoreDetail));
        } catch (Exception ignored) { log.error("保存评分明细JSON失败", ignored); }

        sub.setScore(totalScore);
        sub.setGradeType("TEACHER");
        sub.setGradedBy(teacherId);
        sub.setGradedAt(LocalDateTime.now());
        sub.setStatus("GRADED");
        submissionMapper.updateById(sub);

        // 发布事件
        eventPublisher.publishEvent(TaskEvent.graded(this, task.getId(),
            TaskCategory.valueOf(task.getTaskType()), sub.getStudentId(), teacherId,
            Map.of("submissionId", sub.getId(), "score", totalScore.toString(),
                "gradeLevel", "")));

        return sub;
    }

    // ── 传统直接打分（兼容） ──────────────────────────

    @Transactional
    public TaskSubmission gradeDirect(Long submissionId, BigDecimal score, String gradeLevel,
                                       String comment, Long teacherId) {
        if (!SecurityUtils.isTeacherOrAdmin()) throw new BusinessException(403, "无评分权限");

        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");

        sub.setScore(score);
        sub.setGradeLevel(gradeLevel);
        sub.setGradeType("TEACHER");
        sub.setGradedBy(teacherId);
        sub.setGradedAt(LocalDateTime.now());
        sub.setStatus("GRADED");

        sub.setScoreJson(JsonUtils.toJson(Map.of("direct", score, "comment", comment != null ? comment : "")));

        submissionMapper.updateById(sub);

        Task task = taskMapper.selectById(sub.getTaskId());
        if (task != null) {
            eventPublisher.publishEvent(TaskEvent.graded(this, task.getId(),
                TaskCategory.valueOf(task.getTaskType()), sub.getStudentId(), teacherId,
                Map.of("submissionId", sub.getId(), "score", score.toString(),
                    "gradeLevel", gradeLevel != null ? gradeLevel : "")));
        }
        return sub;
    }
}
