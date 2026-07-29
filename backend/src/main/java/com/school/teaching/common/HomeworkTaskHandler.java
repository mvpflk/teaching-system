package com.school.teaching.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.SubmitLockService;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业/预习/随堂任务处理器 — 覆盖 PRE_CLASS、IN_CLASS、AFTER_CLASS。
 *
 * 核心差异（vs 考试）：迟交扣分、支持打回重交、教师手工评分。
 * 客观题（单选/多选/判断/填空）在提交时自动判分，主观题（简答/作文）等教师手工。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkTaskHandler implements TaskTypeHandler {

    private final TaskSubmissionMapper submissionMapper;
    private final SubmitLockService submitLockService;
    private final TaskQuestionMapper taskQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final StudentAnswerMapper studentAnswerMapper;

    private static final Set<String> AUTO_GRADE_TYPES = Set.of(
        "SINGLE_CHOICE", "MULTI_CHOICE", "TRUE_FALSE", "FILL_IN");

    @Override
    public Set<TaskCategory> getCategories() {
        return Set.of(TaskCategory.PRE_CLASS, TaskCategory.IN_CLASS, TaskCategory.AFTER_CLASS,
                      TaskCategory.MORAL, TaskCategory.LABOR);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSubmission onSubmit(TaskContext ctx) {
        Long taskId = ctx.taskId();
        Long studentId = ctx.studentId();

        // 获取提交锁（与考试提交一致的防并发机制）
        var submitLock = submitLockService.tryLockString(taskId + ":" + studentId);
        if (submitLock == null) throw new BusinessException(409, "正在处理您的提交，请勿重复操作");
        try {

        Task task = (Task) ctx.extras().get("task");
        if (task == null) throw new BusinessException(500, "任务上下文缺失");
        if (!"PUBLISHED".equals(task.getStatus()) && !"ONGOING".equals(task.getStatus()))
            throw new BusinessException(400, "任务状态不可提交");

        // 检查是否已有已完成的提交 → 看是否允许重交（跳过PENDING状态的预创建记录）
        TaskSubmission existing = submissionMapper.selectOne(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .ne(TaskSubmission::getStatus, "PENDING")
                .orderByDesc(TaskSubmission::getAttemptNumber).last("LIMIT 1"));

        boolean isLate = isLateSubmission(task);
        BigDecimal penaltyRatio = getLatePenaltyRatio(task);

        // 使用预创建的提交记录
        Long submissionId = ctx.extras() != null ? (Long) ctx.extras().get("submissionId") : null;
        TaskSubmission sub = submissionId != null ? submissionMapper.selectById(submissionId) : null;
        if (sub == null) throw new BusinessException(500, "提交记录初始化失败");

        @SuppressWarnings("unchecked")
        Map<String, Object> subData = (Map<String, Object>) ctx.submission();
        if (subData != null) {
            sub.setContent((String) subData.get("content"));
            Object att = subData.get("attachments");
            if (att instanceof String) sub.setAttachments((String) att);
            else if (att != null) sub.setAttachments(toJson(att));
        }

        sub.setStatus("SUBMITTED");
        sub.setSubmittedAt(LocalDateTime.now());

        // 重交链
        if (existing != null && task.getAllowResubmit() != null && task.getAllowResubmit() == 1) {
            sub.setResubmissionOf(existing.getId());
        } else if (existing != null) {
            throw new BusinessException(409, "已提交，不支持重复提交");
        }

        // 自动判分客观题（与 ExamTaskHandler 一致）
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = subData != null
            ? (Map<String, Object>) subData.get("answers") : null;
        if (answers != null && !answers.isEmpty()) {
            BigDecimal autoScore = doAutoGrade(sub, taskId, answers);
            sub.setObjectiveScore(autoScore);
        }

        // 迟交惩罚
        if (isLate && penaltyRatio.compareTo(BigDecimal.ONE) < 0) {
            sub.setScore(task.getTotalScore() != null
                ? task.getTotalScore().multiply(penaltyRatio).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        }

        submissionMapper.updateById(sub);
        return sub;
        } finally {
            submitLock.close();
        }
    }

    /**
     * 自动判分作业中的客观题
     */
    private BigDecimal doAutoGrade(TaskSubmission sub, Long taskId, Map<String, Object> answers) {
        List<TaskQuestion> tqList = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>()
                .eq(TaskQuestion::getTaskId, taskId));
        if (tqList.isEmpty()) return BigDecimal.ZERO;

        Set<Long> qIds = tqList.stream().map(TaskQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, QuestionBank> qMap = questionBankMapper.selectBatchIds(qIds).stream()
            .collect(Collectors.toMap(QuestionBank::getId, q -> q, (a, b) -> a));

        BigDecimal totalAuto = BigDecimal.ZERO;
        List<StudentAnswer> answerList = new ArrayList<>();

        for (TaskQuestion tq : tqList) {
            QuestionBank q = qMap.get(tq.getQuestionId());
            if (q == null || q.getCorrectAnswer() == null) continue;

            String qIndex = String.valueOf(tq.getSortOrder()); // 与前端 answers key 一致：序号
            String studentAnswer = answers.containsKey(qIndex)
                ? String.valueOf(answers.get(qIndex)).trim()
                : "";

            boolean isAutoGradable = AUTO_GRADE_TYPES.contains(q.getQuestionType());
            boolean isCorrect = isAutoGradable && !studentAnswer.isEmpty()
                && ExamTaskHandler.answersMatch(q.getQuestionType(), q.getCorrectAnswer().trim(), studentAnswer);

            StudentAnswer sa = new StudentAnswer();
            sa.setSubmissionId(sub.getId());
            sa.setTaskId(taskId);
            sa.setQuestionId(tq.getQuestionId());
            sa.setStudentAnswer(studentAnswer);
            if (isAutoGradable) {
                sa.setIsCorrect(isCorrect ? 1 : 0);
                if (isCorrect && tq.getScore() != null) {
                    sa.setAutoScore(tq.getScore());
                } else {
                    sa.setAutoScore(BigDecimal.ZERO);
                }
            } else {
                // 主观题标记待教师评分
                sa.setIsCorrect(2);
                sa.setAutoScore(BigDecimal.ZERO);
            }
            sa.setSchoolId(1L);
            sa.setAnswerTime(LocalDateTime.now());
            answerList.add(sa);

            if (isCorrect && tq.getScore() != null) {
                totalAuto = totalAuto.add(tq.getScore());
            }
        }

        // 批量写入 student_answers
        for (StudentAnswer sa : answerList) {
            studentAnswerMapper.insert(sa);
        }

        return totalAuto;
    }

    /**
     * 教师评分 — 用于作业/预习/随堂的手工评分。
     * @return 更新后的提交记录
     */
    public TaskSubmission gradeByTeacher(Long submissionId, BigDecimal score,
                                         String gradeLevel, Long teacherId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交记录不存在");
        if ("GRADED".equals(sub.getStatus())) throw new BusinessException(409, "已评分，不可重复评分");

        sub.setScore(score);
        sub.setGradeLevel(gradeLevel);
        sub.setGradeType("TEACHER");
        sub.setGradedBy(teacherId);
        sub.setGradedAt(LocalDateTime.now());
        sub.setStatus("GRADED");
        submissionMapper.updateById(sub);
        return sub;
    }

    /** 批量教师评分 — 对一次任务的所有提交手动打分 */
    public int batchGradeByTeacher(Long taskId, Map<Long, BigDecimal> submissionScores,
                                    Long teacherId) {
        List<TaskSubmission> subs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStatus, "SUBMITTED"));
        int count = 0;
        for (TaskSubmission sub : subs) {
            BigDecimal score = submissionScores.get(sub.getId());
            if (score == null) continue;
            sub.setScore(score);
            sub.setGradeType("TEACHER");
            sub.setGradedBy(teacherId);
            sub.setGradedAt(LocalDateTime.now());
            sub.setStatus("GRADED");
            submissionMapper.updateById(sub);
            count++;
        }
        return count;
    }

    // -- private helpers --

    private boolean isLateSubmission(Task task) {
        return task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline());
    }

    @SuppressWarnings("unchecked")
    private BigDecimal getLatePenaltyRatio(Task task) {
        try {
            String json = task.getTaskConfig();
            if (json == null || json.isBlank()) return BigDecimal.ONE;
            Map<String, Object> config = JsonUtils.parseMap(json);
            Object ratio = config.get("late_penalty_ratio");
            if (ratio instanceof Number) return new BigDecimal(ratio.toString());
            return BigDecimal.ONE;
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    private static String toJson(Object obj) {
        return JsonUtils.toJson(obj);
    }
}
