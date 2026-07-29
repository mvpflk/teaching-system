package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.school.teaching.common.ExamTaskHandler;
import com.school.teaching.entity.*;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.service.TaskGradingService;
import com.school.teaching.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskGradingServiceImpl implements TaskGradingService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final StudentAnswerMapper answerMapper;
    private final QuestionBankMapper questionBankMapper;
    private final TaskQuestionMapper taskQuestionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private static final Set<String> OBJECTIVE_TYPES = com.school.teaching.common.QuestionTypeEnum.OBJECTIVE_TYPES.stream()
        .map(com.school.teaching.common.QuestionTypeEnum::name).collect(java.util.stream.Collectors.toSet());

    @Override @Transactional
    public int autoGradeObjective(Long submissionId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");

        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getSubmissionId, submissionId));
        if (answers.isEmpty()) return 0;

        // 批量加载题目 → Map
        List<Long> qIds = answers.stream().map(StudentAnswer::getQuestionId).toList();
        Map<Long, QuestionBank> qMap = questionBankMapper.selectBatchIds(qIds).stream()
            .collect(java.util.stream.Collectors.toMap(QuestionBank::getId, q -> q));

        // 加载题目分值用于重算总分
        List<TaskQuestion> tqList = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, sub.getTaskId()));
        Map<Long, BigDecimal> scoreMap = new java.util.HashMap<>();
        for (TaskQuestion tq : tqList)
            scoreMap.put(tq.getQuestionId(), tq.getScore() != null ? tq.getScore() : BigDecimal.ONE);

        int scored = 0;
        List<StudentAnswer> toUpdate = new ArrayList<>();
        for (StudentAnswer a : answers) {
            QuestionBank q = qMap.get(a.getQuestionId());
            if (q == null || !OBJECTIVE_TYPES.contains(q.getQuestionType())) continue;
            boolean correct = ExamTaskHandler.answersMatch(
                q.getQuestionType(), q.getCorrectAnswer(), a.getStudentAnswer());
            a.setIsCorrect(correct ? 1 : 0);
            // 同时写入 autoScore 以保持与 ExamTaskHandler.onSubmit 一致
            BigDecimal qScore = scoreMap.getOrDefault(a.getQuestionId(), BigDecimal.ONE);
            a.setAutoScore(correct ? qScore : BigDecimal.ZERO);
            toUpdate.add(a);
            scored++;
        }
        for (StudentAnswer a : toUpdate) answerMapper.updateById(a);
        // 重算提交总分（含主观题已有的 teacherScore）
        BigDecimal finalScore = recalcScore(submissionId);
        // 自动评分后标记为已批改 — 必须重新加载 sub，因为 recalcScore 内部已 updateById
        // 直接 setScore 后再 updateById 会导致脏读覆写正确分数
        TaskSubmission freshSub = submissionMapper.selectById(submissionId);
        if (freshSub != null) {
            freshSub.setScore(finalScore);
            freshSub.setStatus("GRADED");
            // 保留教师手动评分的类型标记
            if (!"TEACHER".equals(freshSub.getGradeType())) {
                freshSub.setGradeType("AUTO");
            }
            freshSub.setGradedAt(java.time.LocalDateTime.now());
            submissionMapper.updateById(freshSub);
        }
        return scored;
    }

    @Override @Transactional
    public void manualGradeSubjective(Long submissionId, Map<Long, BigDecimal> questionScores, Long teacherId) {
        List<StudentAnswer> all = answerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
            .eq(StudentAnswer::getSubmissionId, submissionId)
            .in(StudentAnswer::getQuestionId, questionScores.keySet()));
        Map<Long, StudentAnswer> answerMap = all.stream()
            .collect(java.util.stream.Collectors.toMap(StudentAnswer::getQuestionId, a -> a));

        for (Map.Entry<Long, BigDecimal> e : questionScores.entrySet()) {
            StudentAnswer a = answerMap.get(e.getKey());
            if (a != null) {
                a.setTeacherScore(e.getValue());
                a.setIsCorrect(e.getValue().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
                answerMapper.updateById(a);
            }
        }
        recalcScore(submissionId);
        syncWrongQuestions(submissionId);
    }

    @Override
    public BigDecimal calculateTotalScore(Long submissionId) {
        return recalcScore(submissionId);
    }

    @Override
    public boolean isPassed(Long submissionId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");
        Task task = taskMapper.selectById(sub.getTaskId());
        if (task == null) return true;

        // 优先使用 passRate（新设计，2026-07-03）
        if (task.getPassRate() != null && task.getPassRate() > 0) {
            BigDecimal totalScore = task.getTotalScore() != null ? task.getTotalScore() : BigDecimal.valueOf(100);
            if (totalScore.compareTo(BigDecimal.ZERO) <= 0) return true;
            // threshold = totalScore * passRate / 100
            BigDecimal threshold = totalScore.multiply(BigDecimal.valueOf(task.getPassRate()))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            return sub.getScore() != null && sub.getScore().compareTo(threshold) >= 0;
        }

        // 回退: 从 taskConfig JSON 读取 passingScore（兼容旧数据）
        BigDecimal passingScore = getPassingScoreFromConfig(task);
        if (passingScore == null) return true;
        return sub.getScore() != null && sub.getScore().compareTo(passingScore) >= 0;
    }

    private BigDecimal getPassingScoreFromConfig(Task task) {
        if (task.getTaskConfig() == null) return null;
        Map<String, Object> config = JsonUtils.parseMap(task.getTaskConfig());
        Object val = config.get("passingScore");
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return null;
    }

    /** 将作答错误的题目同步到错题本（受 autoWrongbook 开关控制） */
    @Override
    public void syncWrongQuestions(Long submissionId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null || sub.getStudentId() == null) return;

        // 检查任务是否开启自动收录错题（默认开启）
        Task task = taskMapper.selectById(sub.getTaskId());
        if (task != null && task.getAutoWrongbook() != null && task.getAutoWrongbook() == 0) return;

        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getSubmissionId, submissionId));
        if (answers.isEmpty()) return;

        // 加载题目分值
        List<Long> qIds = answers.stream().map(StudentAnswer::getQuestionId).distinct().toList();
        List<TaskQuestion> tqs = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>()
                .eq(TaskQuestion::getTaskId, sub.getTaskId())
                .in(TaskQuestion::getQuestionId, qIds));
        Map<Long, BigDecimal> scoreMap = new java.util.HashMap<>();
        for (TaskQuestion tq : tqs) scoreMap.put(tq.getQuestionId(), tq.getScore() != null ? tq.getScore() : BigDecimal.ONE);

        for (StudentAnswer a : answers) {
            // 只记录错误的（isCorrect=0），与 ExamTaskHandler 保持一致
            boolean isWrong = a.getIsCorrect() != null && a.getIsCorrect() == 0;
            if (!isWrong) continue;

            // upsert 错题记录
            WrongQuestion existing = wrongQuestionMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getStudentId, sub.getStudentId())
                    .eq(WrongQuestion::getQuestionId, a.getQuestionId()));
            if (existing != null) {
                existing.setWrongCount(existing.getWrongCount() != null ? existing.getWrongCount() + 1 : 1);
                existing.setLastWrongTime(java.time.LocalDateTime.now());
                wrongQuestionMapper.updateById(existing);
            } else {
                WrongQuestion wq = new WrongQuestion();
                wq.setStudentId(sub.getStudentId());
                wq.setQuestionId(a.getQuestionId());
                wq.setWrongCount(1);
                wq.setLastWrongTime(java.time.LocalDateTime.now());
                wq.setIsMastered(0);
                wq.setSourceTaskId(sub.getTaskId());
                wq.setSourceType(task != null ? inferWrongSourceType(task.getTaskType()) : null);
                wrongQuestionMapper.insert(wq);
            }
        }
    }

    /** 根据任务类型推断错题来源标签 */
    private String inferWrongSourceType(String taskType) {
        if (taskType == null) return null;
        return switch (taskType) {
            case "FORMATIVE", "SUMMATIVE" -> "EXAM";
            case "PRE_CLASS", "IN_CLASS", "AFTER_CLASS", "MORAL", "LABOR" -> "HOMEWORK";
            default -> null;
        };
    }

    private BigDecimal recalcScore(Long submissionId) {
        List<StudentAnswer> answers = answerMapper.selectList(
            new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getSubmissionId, submissionId));
        BigDecimal total = BigDecimal.ZERO;
        for (StudentAnswer a : answers) {
            if (a.getTeacherScore() != null) total = total.add(a.getTeacherScore());
            else if (a.getAutoScore() != null) total = total.add(a.getAutoScore());
        }
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub != null) {
            // 上限截断：不超过任务设定总分
            if (sub.getTaskId() != null) {
                Task task = taskMapper.selectById(sub.getTaskId());
                if (task != null && task.getTotalScore() != null) {
                    BigDecimal maxScore = task.getTotalScore();
                    if (total.compareTo(maxScore) > 0) total = maxScore;
                }
            }
            sub.setScore(total);
            submissionMapper.updateById(sub);
        }
        return total;
    }

    @Override
    @Transactional
    public BigDecimal gradeItems(Long submissionId, Map<Long, BigDecimal> answerScores, Long teacherId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");
        Task task = taskMapper.selectById(sub.getTaskId());
        String srcType = task != null ? inferWrongSourceType(task.getTaskType()) : null;

        // 批量加载所有涉及的答案
        List<Long> answerIds = new ArrayList<>(answerScores.keySet());
        List<StudentAnswer> answers = answerMapper.selectBatchIds(answerIds);
        Map<Long, StudentAnswer> answerMap = answers.stream()
            .collect(java.util.stream.Collectors.toMap(StudentAnswer::getId, a -> a));

        // 批量加载题目分值
        List<TaskQuestion> tqList = taskQuestionMapper.selectList(
            new LambdaQueryWrapper<TaskQuestion>().eq(TaskQuestion::getTaskId, sub.getTaskId()));
        Map<Long, BigDecimal> scoreMap = new java.util.HashMap<>();
        for (TaskQuestion tq : tqList)
            scoreMap.put(tq.getQuestionId(), tq.getScore() != null ? tq.getScore() : BigDecimal.ONE);

        // 批量更新答案 + 收集错题
        List<StudentAnswer> toUpdate = new ArrayList<>();
        List<WrongQuestion> wrongToUpsert = new ArrayList<>();
        List<Long> wrongQids = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> e : answerScores.entrySet()) {
            StudentAnswer a = answerMap.get(e.getKey());
            if (a == null) continue;
            BigDecimal score = e.getValue();
            a.setTeacherScore(score);
            a.setIsCorrect(score.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
            toUpdate.add(a);

            // 错题收录：统一为 isCorrect=0 就收录，与 ExamTaskHandler 保持一致
            if (a.getIsCorrect() != null && a.getIsCorrect() == 0) {
                WrongQuestion wq = new WrongQuestion();
                wq.setStudentId(sub.getStudentId());
                wq.setQuestionId(a.getQuestionId());
                wq.setWrongCount(1);
                wq.setLastWrongTime(java.time.LocalDateTime.now());
                wq.setIsMastered(0);
                wq.setSourceTaskId(sub.getTaskId());
                wq.setSourceType(srcType);
                wrongToUpsert.add(wq);
                wrongQids.add(a.getQuestionId());
            }
        }

        // 批量更新答案
        for (StudentAnswer a : toUpdate) answerMapper.updateById(a);

        // 批量同步错题本
        if (!wrongToUpsert.isEmpty()) {
            Map<String, WrongQuestion> existing = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getStudentId, sub.getStudentId())
                    .in(WrongQuestion::getQuestionId, wrongQids)).stream()
                .collect(java.util.stream.Collectors.toMap(
                    w -> w.getStudentId() + "_" + w.getQuestionId(), w -> w, (a, b) -> a));

            for (WrongQuestion wq : wrongToUpsert) {
                String key = wq.getStudentId() + "_" + wq.getQuestionId();
                WrongQuestion exist = existing.get(key);
                if (exist != null) {
                    exist.setWrongCount(exist.getWrongCount() != null ? exist.getWrongCount() + 1 : 1);
                    exist.setLastWrongTime(java.time.LocalDateTime.now());
                    wrongQuestionMapper.updateById(exist);
                } else {
                    wrongQuestionMapper.insert(wq);
                }
            }
        }

        // 重算总分并更新提交状态（recalcScore 内部已 updateById，此处仅更新非分数字段）
        BigDecimal total = recalcScore(submissionId);
        LambdaUpdateWrapper<TaskSubmission> metaUpdate = new LambdaUpdateWrapper<TaskSubmission>()
            .eq(TaskSubmission::getId, submissionId)
            .set(TaskSubmission::getStatus, "GRADED")
            .set(TaskSubmission::getGradeType, "TEACHER")
            .set(TaskSubmission::getGradedBy, teacherId)
            .set(TaskSubmission::getGradedAt, java.time.LocalDateTime.now());
        submissionMapper.update(null, metaUpdate);
        return total;
    }
}
