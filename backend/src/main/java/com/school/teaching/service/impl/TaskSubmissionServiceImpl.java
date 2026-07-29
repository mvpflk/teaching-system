package com.school.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.common.*;
import com.school.teaching.entity.*;
import com.school.teaching.event.TaskEvent;
import com.school.teaching.exception.BusinessException;
import com.school.teaching.mapper.*;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.ShowcaseWorkService;
import com.school.teaching.service.TaskSubmissionService;
import com.school.teaching.utils.JsonUtils;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    private final TaskMapper taskMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final StudentMapper studentMapper;
    private final StudentClassHistoryMapper classHistoryMapper;
    private final com.school.teaching.security.StudentResolver studentResolver;
    private final TaskTypeHandlerSelector handlerSelector;
    private final ApplicationEventPublisher eventPublisher;
    private final ShowcaseWorkService showcaseWorkService;
    private final com.school.teaching.mapper.UserMapper userMapper;
    private final com.school.teaching.mapper.ClassesMapper classesMapper;
    private final com.school.teaching.mapper.NotificationMapper notificationMapper;
    private final com.school.teaching.service.StudentTimelineService studentTimelineService;
    private final com.school.teaching.mapper.StudentAnswerMapper studentAnswerMapper;
    private final com.school.teaching.service.TaskGradingService gradingService;
    private final com.school.teaching.mapper.WrongQuestionMapper wrongQuestionMapper;
    private final CacheManager cacheManager;

    // startExam 防重复点击锁注册表（替代 String.intern()，避免 Metaspace 泄漏）
    private final ConcurrentHashMap<String, Object> startExamLocks = new ConcurrentHashMap<>();

    /** 每小时清理无争用的锁对象，防止 Map 无限增长 */
    @Scheduled(fixedRate = 3600_000)
    void purgeIdleLocks() {
        // 保守策略：清理后可能短暂失去对同一 key 的互斥，但 startExam 操作几秒内完成，
        // 一小时后未释放的锁对象早已无用
        startExamLocks.clear();
    }

    @Override @Transactional
    @CacheEvict(value = "submission_status", key = "#studentId + ':' + #taskId")
    public Map<String, Object> startExam(Long taskId, Long studentId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        if (!"FORMATIVE".equals(task.getTaskType()) && !"SUMMATIVE".equals(task.getTaskType()))
            throw new BusinessException(400, "非考试任务无需开始答题");

        if (!List.of("PUBLISHED", "ONGOING").contains(task.getStatus()))
            throw new BusinessException(400, "任务当前状态不可开始答题");

        // 检查截止日期
        assertNotExpired(task, "开始答题");

        // 检查班级归属
        Long classId = resolveCurrentClassId(studentId);
        if ("CLASS".equals(task.getTargetType()) && !task.getTargetId().equals(classId))
            throw new BusinessException(403, "你不属于该任务的目标班级");

        // 同步块防止 TOCTOU 竞态（双击→重复 PENDING）
        // 用 ConcurrentHashMap 替代 String.intern()，避免常量池内存泄漏
        String lockKey = taskId + ":" + studentId;
        Object mutex = startExamLocks.computeIfAbsent(lockKey, k -> new Object());
        synchronized (mutex) {
            // 已有提交记录处理
            TaskSubmission existing = submissionMapper.selectOne(
                new LambdaQueryWrapper<TaskSubmission>()
                    .eq(TaskSubmission::getTaskId, taskId)
                    .eq(TaskSubmission::getStudentId, studentId)
                    .ne(TaskSubmission::getStatus, "EXEMPTED")
                    .orderByDesc(TaskSubmission::getCreatedAt)
                    .last("LIMIT 1"));
            if (existing != null) {
                // 特许重做：消耗 extraSubmitAllowed 标志，重置计时和作弊状态
                if (existing.getExtraSubmitAllowed() != null && existing.getExtraSubmitAllowed() == 1) {
                    // 先清理该学生此任务的所有旧 PENDING，防止 selectOne 重复记录
                    submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskId, taskId)
                        .eq(TaskSubmission::getStudentId, studentId)
                        .eq(TaskSubmission::getStatus, "PENDING")
                        .set(TaskSubmission::getStatus, "EXPIRED"));
                    // 消耗特许标记（防止下一次 startExam 再次进入特许流程造成无限循环）
                    submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getId, existing.getId())
                        .set(TaskSubmission::getExtraSubmitAllowed, 0));
                    // 创建新的 PENDING 记录
                    // 计算下一个 attempt_number，避免与已有记录的唯一索引 uk_task_student_attempt 冲突
                    int maxAttempt = submissionMapper.selectList(
                        new LambdaQueryWrapper<TaskSubmission>()
                            .eq(TaskSubmission::getTaskId, taskId)
                            .eq(TaskSubmission::getStudentId, studentId))
                        .stream()
                        .mapToInt(s -> s.getAttemptNumber() != null ? s.getAttemptNumber() : 0)
                        .max()
                        .orElse(0);
                    TaskSubmission fresh = new TaskSubmission();
                    fresh.setTaskId(taskId);
                    fresh.setStudentId(studentId);
                    fresh.setStatus("PENDING");
                    fresh.setSchoolId(task.getSchoolId());
                    fresh.setStageId(task.getStageId());
                    fresh.setResubmissionOf(existing.getId());
                    fresh.setIsOfficial(true);
                    fresh.setAttemptNumber(maxAttempt + 1);
                    // 注意：不在新记录上设 extraSubmitAllowed，特许标记已被消耗
                    submissionMapper.insert(fresh);
                    return Map.of("submissionId", fresh.getId(), "taskConfig", task.getTaskConfig() != null ? task.getTaskConfig() : "{}",
                        "existing", false);
                }
                // 如果旧记录已是终止/已提交/已退回/已评分状态且不允许重交，拒绝开始
                if ("TERMINATED".equals(existing.getStatus()) || "SUBMITTED".equals(existing.getStatus())
                    || "GRADED".equals(existing.getStatus()) || "RETURNED".equals(existing.getStatus())) {
                    if (task.getAllowResubmit() == null || task.getAllowResubmit() != 1) {
                        throw new BusinessException(410, "该任务已完成，不可重新开始。如需重考请联系教师特许。");
                    }
                }
                // PENDING 记录复用但刷新 createdAt 防止超时误判
                if ("PENDING".equals(existing.getStatus())) {
                    existing.setCreatedAt(java.time.LocalDateTime.now());
                    submissionMapper.updateById(existing);
                }
                return Map.of("submissionId", existing.getId(), "taskConfig", task.getTaskConfig() != null ? task.getTaskConfig() : "{}",
                    "existing", true);
            }

            // 首次开始 → 创建 PENDING 记录
            TaskSubmission sub = new TaskSubmission();
            sub.setTaskId(taskId);
            sub.setStudentId(studentId);
            sub.setStatus("PENDING");
            sub.setSchoolId(task.getSchoolId());
            sub.setStageId(task.getStageId());
            sub.setIsOfficial(true);
            sub.setAttemptNumber(1);
            submissionMapper.insert(sub);

            return Map.of("submissionId", sub.getId(), "taskConfig", task.getTaskConfig() != null ? task.getTaskConfig() : "{}",
                "existing", false);
        }
    }

    @Override @Transactional
    @Caching(evict = {
        @CacheEvict(value = "pending_count", key = "#studentId"),
        @CacheEvict(value = "submission_status", key = "#studentId + ':' + #taskId")
    })
    public TaskSubmission submit(Long taskId, Long studentId, Map<String, Object> payload) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        // SURVEY 类型校验：content 必须是合法 JSON 对象
        if ("SURVEY".equals(task.getTaskType())) {
            String content = (String) payload.getOrDefault("content", "");
            if (content == null || content.isBlank())
                throw new BusinessException(400, "问卷答案不能为空");
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                om.readTree(content);
            } catch (Exception e) {
                throw new BusinessException(400, "问卷答案格式不合法");
            }
        }

        // 检查截止日期
        assertNotExpired(task, "提交");

        // 检查班级归属
        Long classId = resolveCurrentClassId(studentId);
        if ("CLASS".equals(task.getTargetType()) && !task.getTargetId().equals(classId))
            throw new BusinessException(403, "你不属于该任务的目标班级");

        // 检查是否已提交：PENDING(仅开始答题未提交)可提交，SUBMITTED/GRADED未开重交则拒绝
        TaskSubmission existingSubmitted = submissionMapper.selectOne(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED")
                .orderByDesc(TaskSubmission::getCreatedAt)
                .last("LIMIT 1"));

        // 检查是否有重测 PENDING（最高 attemptNumber）— 有则说明是自动/手动创建的重测，允许提交
        TaskSubmission pendingRetake = submissionMapper.selectOne(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .eq(TaskSubmission::getStatus, "PENDING")
                .orderByDesc(TaskSubmission::getAttemptNumber)
                .last("LIMIT 1"));
        boolean isRetakeSubmit = pendingRetake != null;

        if (!isRetakeSubmit && existingSubmitted != null
            && (task.getAllowResubmit() == null || task.getAllowResubmit() != 1)
            && (existingSubmitted.getExtraSubmitAllowed() == null || existingSubmitted.getExtraSubmitAllowed() != 1)) {
            throw new BusinessException(409, "已提交过，不允许重复提交");
        }

        // 如果任务还在 PUBLISHED → 首次提交时改为 ONGOING
        if ("PUBLISHED".equals(task.getStatus())) {
            task.setStatus("ONGOING");
            taskMapper.updateById(task);
        }

        // 复用或创建提交记录
        TaskSubmission preSub = submissionMapper.selectOne(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .eq(TaskSubmission::getStatus, "PENDING")
                .orderByDesc(TaskSubmission::getCreatedAt)
                .last("LIMIT 1"));
        if (preSub == null) {
            // 考试类型必须先调用 startExam 创建 PENDING 记录（启用防作弊追踪）
            if ("FORMATIVE".equals(task.getTaskType()) || "SUMMATIVE".equals(task.getTaskType())) {
                throw new BusinessException(400, "请先点击「开始答题」后再提交");
            }
            preSub = new TaskSubmission();
            preSub.setTaskId(taskId);
            preSub.setStudentId(studentId);
            preSub.setSchoolId(task.getSchoolId());
            preSub.setStageId(task.getStageId());
            preSub.setStatus("PENDING");
            preSub.setIsOfficial(true);
            preSub.setAttemptNumber(1);
            if (existingSubmitted != null) preSub.setResubmissionOf(existingSubmitted.getId());
            submissionMapper.insert(preSub);
        }

        // 考试时长校验：超时自动终止（服务端权威计时，前端倒计时仅作展示）
        if ("FORMATIVE".equals(task.getTaskType()) || "SUMMATIVE".equals(task.getTaskType())) {
            int durationMinutes = getDurationMinutes(task);
            if (durationMinutes > 0 && preSub.getCreatedAt() != null
                && java.time.LocalDateTime.now().isAfter(preSub.getCreatedAt().plusMinutes(durationMinutes))) {
                log.warn("考试超时自动终止: taskId={}, studentId={}, duration={}min, startedAt={}",
                    taskId, studentId, durationMinutes, preSub.getCreatedAt());
                submissionMapper.update(null, new LambdaUpdateWrapper<TaskSubmission>()
                    .eq(TaskSubmission::getId, preSub.getId())
                    .set(TaskSubmission::getStatus, "TERMINATED")
                    .set(TaskSubmission::getCheatTerminated, 0)
                    .set(TaskSubmission::getScore, java.math.BigDecimal.ZERO)
                    .set(TaskSubmission::getSubmittedAt, java.time.LocalDateTime.now())
                    .set(TaskSubmission::getGradedAt, java.time.LocalDateTime.now())
                    .set(TaskSubmission::getGradingMessage,
                        "考试时间到（限时" + durationMinutes + "分钟），系统自动终止答题并记为0分。"));
                throw new BusinessException(400, "考试时间已到，答题已自动终止。本次记为0分。");
            }
        }

        TaskTypeHandler handler = handlerSelector.get(TaskCategory.valueOf(task.getTaskType()));
        TaskSubmission sub;
        if (handler != null) {
            java.util.HashMap<String, Object> extras = new java.util.HashMap<>();
            extras.put("schoolId", task.getSchoolId());
            extras.put("stageId", task.getStageId());
            extras.put("submissionId", preSub.getId());
            extras.put("task", task);
            TaskTypeHandler.TaskContext ctx = new TaskTypeHandler.TaskContext(
                taskId, studentId, task.getTeacherId(),
                parseConfig(task.getTaskConfig()),
                payload,
                extras
            );
            sub = handler.onSubmit(ctx);
            if (sub == null) throw new BusinessException(500, "Handler 返回空提交");
        } else {
            // A3: 实训任务必须通过实训页面提交，拦截通用接口
            if ("PRACTICE".equals(task.getTaskType())) {
                throw new BusinessException(400, "实训任务请通过实训页面提交");
            }
            // 无Handler的任务类型（MORAL/LABOR/SURVEY）：手动处理提交
            if (!List.of("PUBLISHED", "ONGOING").contains(task.getStatus()))
                throw new BusinessException(400, "任务当前状态不可提交");
            preSub.setStatus("SUBMITTED");
            preSub.setSubmittedAt(LocalDateTime.now());
            // MORAL/LABOR 分数留空待教师评星，SURVEY/PRACTICE 也不预设分数
            if (!"MORAL".equals(task.getTaskType()) && !"LABOR".equals(task.getTaskType())) {
                preSub.setScore(java.math.BigDecimal.ZERO);
            }
            // SURVEY：保存问卷答案到 content 字段
            if ("SURVEY".equals(task.getTaskType())) {
                preSub.setContent((String) payload.getOrDefault("content", ""));
            }
            submissionMapper.updateById(preSub);
            sub = preSub;
        }

        // 事件驱动通知
        eventPublisher.publishEvent(TaskEvent.submitted(this, taskId,
            TaskCategory.valueOf(task.getTaskType()), studentId, task.getTeacherId(),
            Map.of("submissionId", sub.getId(), "score", sub.getScore() != null ? sub.getScore().toString() : "0")));

        // 成长足迹：提交任务
        try { studentTimelineService.recordEvent(studentId, "submit", "提交了任务：" + task.getTitle(), null, null); } catch (Exception ignored) { /* 成长足迹记录失败不影响提交流程 */ }

        // ── 填充重测相关 transient 字段（供前端重测弹窗使用） ──
        enrichSubmitReturn(sub, task, studentId);

        return sub;
    }

    /** 填充 submit 返回值的重测 transient 字段 */
    private void enrichSubmitReturn(TaskSubmission sub, Task task, Long studentId) {
        if (task.getPassRate() == null || task.getPassRate() <= 0) return;
        if (task.getMaxAttempts() == null || task.getMaxAttempts() < 1) return;
        BigDecimal total = task.getTotalScore() != null ? task.getTotalScore() : BigDecimal.valueOf(100);
        if (total.compareTo(BigDecimal.ZERO) <= 0) return;

        int passRateVal = task.getPassRate();
        int maxAttempts = task.getMaxAttempts();
        int attemptNum = sub.getAttemptNumber() != null ? sub.getAttemptNumber() : 1;

        // 根据 passMode 选择判定分数
        BigDecimal score;
        if ("all".equals(task.getPassMode())) {
            score = sub.getScore();
        } else {
            score = sub.getObjectiveScore() != null ? sub.getObjectiveScore() : sub.getScore();
        }
        boolean passed = false;
        if (score != null) {
            passed = score.doubleValue() / total.doubleValue() * 100 >= passRateVal;
        }
        sub.setPassed(passed);
        sub.setPassType(attemptNum <= 1 ? "first" : "retake");

        // passRate / remainingAttempts / maxAttempts
        sub.setPassRate(passRateVal);
        sub.setMaxAttempts(maxAttempts);
        sub.setRemainingAttempts(Math.max(0, maxAttempts - attemptNum));

        // 查询该学生该任务的所有提交（用于计算 anyPassed / retakeHistory / canRetake）
        java.util.List<TaskSubmission> studentSubs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, task.getId())
                .eq(TaskSubmission::getStudentId, studentId));
        studentSubs.sort(java.util.Comparator.comparingInt(
            s -> s.getAttemptNumber() != null ? s.getAttemptNumber() : 1));

        // 是否已有任意轮次达标
        boolean anyPassed = hasAnyAttemptPassed(studentSubs, total, passRateVal, task.getPassMode());

        // 重测历史
        java.util.List<java.util.Map<String, Object>> history = new java.util.ArrayList<>();
        TaskSubmission firstSub = null;
        for (var ss : studentSubs) {
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", ss.getId());
            entry.put("score", ss.getScore());
            entry.put("totalScore", total);
            BigDecimal ssScore = "all".equals(task.getPassMode()) ? ss.getScore()
                : (ss.getObjectiveScore() != null ? ss.getObjectiveScore() : ss.getScore());
            boolean ssPassed = false;
            if (ssScore != null && total.compareTo(BigDecimal.ZERO) > 0) {
                ssPassed = ssScore.doubleValue() / total.doubleValue() * 100 >= passRateVal;
            }
            entry.put("passed", ssPassed);
            history.add(entry);
            if (firstSub == null) firstSub = ss;
        }
        sub.setRetakeHistory(history);

        // canRetake
        boolean canRetake = !passed && !anyPassed && attemptNum < maxAttempts;
        if (canRetake && task.getRetakeDeadlineHours() != null && firstSub != null && firstSub.getSubmittedAt() != null) {
            java.time.LocalDateTime deadline = firstSub.getSubmittedAt().plusHours(task.getRetakeDeadlineHours());
            if (java.time.LocalDateTime.now().isAfter(deadline)) {
                canRetake = false;
            }
        }
        sub.setCanRetake(canRetake);

        // scoreImprove（当前分数相对首次的进步）
        BigDecimal firstScore = firstSub != null ? firstSub.getScore() : null;
        if (firstScore != null && score != null && attemptNum > 1) {
            sub.setScoreImprove(score.subtract(firstScore).intValue());
        } else {
            sub.setScoreImprove(0);
        }

        // retakeDeadline
        if (task.getRetakeDeadlineHours() != null && firstSub != null && firstSub.getSubmittedAt() != null) {
            sub.setRetakeDeadline(firstSub.getSubmittedAt()
                .plusHours(task.getRetakeDeadlineHours()).toString());
        } else {
            sub.setRetakeDeadline("");
        }
    }

    @Override @Transactional
    public TaskSubmission grade(Long submissionId, BigDecimal score, String gradeLevel, Long gradedBy) {
        return doGrade(submissionId, score, gradeLevel, gradedBy, null, null);
    }

    @Override @Transactional
    public TaskSubmission grade(Long submissionId, BigDecimal score, String gradeLevel, Long gradedBy,
                                 Integer isExemplar, String comment) {
        return doGrade(submissionId, score, gradeLevel, gradedBy, isExemplar, comment);
    }

    /** 评分核心逻辑：乐观锁更新 → 事件发布 → 成长足迹 → 展示墙推荐 → 错题本同步 */
    private TaskSubmission doGrade(Long submissionId, BigDecimal score, String gradeLevel,
                                    Long gradedBy, Integer isExemplar, String comment) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");
        LambdaUpdateWrapper<TaskSubmission> uw = new LambdaUpdateWrapper<TaskSubmission>()
            .eq(TaskSubmission::getId, submissionId)
            .ne(TaskSubmission::getStatus, "GRADED")
            .set(TaskSubmission::getStatus, "GRADED")
            .set(TaskSubmission::getScore, score)
            .set(TaskSubmission::getGradeLevel, gradeLevel)
            .set(TaskSubmission::getGradeType, "TEACHER")
            .set(TaskSubmission::getGradedBy, gradedBy)
            .set(TaskSubmission::getGradedAt, java.time.LocalDateTime.now());
        if (isExemplar != null && isExemplar == 1) uw.set(TaskSubmission::getIsExemplar, 1);
        boolean updated = submissionMapper.update(null, uw) > 0;
        if (!updated) throw new BusinessException(409, "该提交已评分，不可重复评分");
        // 更新内存对象以返回给调用方
        sub.setScore(score); sub.setGradeLevel(gradeLevel); sub.setGradeType("TEACHER");
        sub.setGradedBy(gradedBy); sub.setStatus("GRADED"); sub.setGradedAt(java.time.LocalDateTime.now());
        if (isExemplar != null && isExemplar == 1) sub.setIsExemplar(1);

        Task task = taskMapper.selectById(sub.getTaskId());
        eventPublisher.publishEvent(TaskEvent.graded(this, sub.getTaskId(),
            TaskCategory.valueOf(task.getTaskType()), sub.getStudentId(), gradedBy,
            Map.of("submissionId", sub.getId(), "score", score != null ? score.toString() : "0",
                "gradeLevel", gradeLevel != null ? gradeLevel : "")));

        // 成长足迹：及格
        if (score != null && score.compareTo(java.math.BigDecimal.valueOf(60)) >= 0) {
            try { studentTimelineService.recordEvent(sub.getStudentId(), "grade",
                "任务「" + task.getTitle() + "」得分：" + score, null, null); } catch (Exception ignored) { /* 成长足迹记录失败不影响评分流程 */ }
        }

        // 自动推荐到展示墙
        if (isExemplar != null && isExemplar == 1) {
            try {
                var showcaseReq = new java.util.HashMap<String, Object>();
                showcaseReq.put("title", task.getTitle());
                showcaseReq.put("sourceType", "TASK");
                showcaseReq.put("sourceId", task.getId());
                showcaseReq.put("studentId", sub.getStudentId());
                showcaseReq.put("subject", task.getSubject());
                showcaseReq.put("teacherComment", comment != null ? comment : "");
                showcaseReq.put("showScope", "CLASS");
                Long classId = resolveCurrentClassId(sub.getStudentId());
                if (classId != null) showcaseReq.put("classId", classId);
                showcaseWorkService.recommendWork(showcaseReq);
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(getClass().getName())
                    .warning("自动推荐展示墙失败: " + e.getMessage());
            }
        }

        // 同步错题本
        try { gradingService.syncWrongQuestions(submissionId); } catch (Exception ignored) { /* 错题本同步失败不影响评分流程 */ }

        // 填充重测 transient 字段（供前端实时刷新达标状态）
        enrichSubmitReturn(sub, task, sub.getStudentId());

        return sub;
    }

    @Override @Transactional
    public TaskSubmission regrade(Long submissionId) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交不存在");
        if (!"GRADED".equals(sub.getStatus())) throw new BusinessException(400, "仅已评分的提交可回退");
        sub.setStatus("SUBMITTED");
        sub.setGradeType(null);
        sub.setGradedBy(null);
        sub.setGradedAt(null);
        sub.setGradeLevel(null);
        sub.setScore(null);
        sub.setIsExemplar(null);
        submissionMapper.updateById(sub);
        // 重置主观题/复杂题的教师评分（isCorrect=2→保持待评分，isCorrect=1/0→重置isCorrect=2，清空teacherScore）
        studentAnswerMapper.update(null,
            new LambdaUpdateWrapper<com.school.teaching.entity.StudentAnswer>()
                .eq(com.school.teaching.entity.StudentAnswer::getSubmissionId, submissionId)
                .in(com.school.teaching.entity.StudentAnswer::getIsCorrect, 0, 1)
                .set(com.school.teaching.entity.StudentAnswer::getIsCorrect, 2)
                .set(com.school.teaching.entity.StudentAnswer::getTeacherScore, null));
        // 清除该提交相关的错题本记录（回退后重新评分将重新收录）
        wrongQuestionMapper.delete(new LambdaUpdateWrapper<com.school.teaching.entity.WrongQuestion>()
            .eq(com.school.teaching.entity.WrongQuestion::getStudentId, sub.getStudentId())
            .eq(com.school.teaching.entity.WrongQuestion::getSourceTaskId, sub.getTaskId()));
        return sub;
    }

    @Override
    @Cacheable(value = "submission_status", key = "#studentId + ':' + #taskId")
    public TaskSubmission getByTaskAndStudent(Long taskId, Long studentId) {
        return submissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmission>()
            .eq(TaskSubmission::getTaskId, taskId)
            .eq(TaskSubmission::getStudentId, studentId)
            .orderByDesc(TaskSubmission::getAttemptNumber)
            .orderByDesc(TaskSubmission::getCreatedAt)
            .last("LIMIT 1"));
    }

    @Override
    public List<TaskSubmission> getByTaskId(Long taskId) {
        return submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
    }

    @Override
    public IPage<TaskSubmission> pageByTaskId(Long taskId, Page<TaskSubmission> page, String keyword) {
        // 关键词搜索：先按 student_answers 内容过滤出匹配的 submissionId
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            List<com.school.teaching.entity.StudentAnswer> answers = studentAnswerMapper.selectList(
                new LambdaQueryWrapper<com.school.teaching.entity.StudentAnswer>()
                    .eq(com.school.teaching.entity.StudentAnswer::getTaskId, taskId)
                    .like(com.school.teaching.entity.StudentAnswer::getStudentAnswer, kw));
            if (answers.isEmpty()) {
                page.setRecords(java.util.List.of()); page.setTotal(0); return page;
            }
            java.util.Set<Long> matchedSubIds = answers.stream()
                .map(com.school.teaching.entity.StudentAnswer::getSubmissionId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            IPage<TaskSubmission> pg = submissionMapper.selectPage(page,
                new LambdaQueryWrapper<TaskSubmission>()
                    .eq(TaskSubmission::getTaskId, taskId)
                    .in(TaskSubmission::getId, matchedSubIds));
            enrichSubmissions(pg.getRecords());
            return pg;
        }
        IPage<TaskSubmission> pg = submissionMapper.selectPage(page,
            new LambdaQueryWrapper<TaskSubmission>().eq(TaskSubmission::getTaskId, taskId));
        enrichSubmissions(pg.getRecords());
        return pg;
    }

    @Deprecated
    public IPage<TaskSubmission> pageByTaskId(Long taskId, Page<TaskSubmission> page) {
        return pageByTaskId(taskId, page, null);
    }

    /** 批量填充学生姓名、班级、年级，以及重测相关字段 */
    private void enrichSubmissions(List<TaskSubmission> subs) {
        if (subs == null || subs.isEmpty()) return;
        var sids = subs.stream().map(TaskSubmission::getStudentId).distinct().toList();
        var smap = studentMapper.selectBatchIds(sids).stream()
            .collect(java.util.stream.Collectors.toMap(com.school.teaching.entity.Student::getId, s -> s));
        var uids = smap.values().stream().map(com.school.teaching.entity.Student::getUserId).distinct().toList();
        var umap = uids.isEmpty() ? java.util.Map.<Long, com.school.teaching.entity.User>of()
            : userMapper.selectBatchIds(uids).stream()
                .collect(java.util.stream.Collectors.toMap(com.school.teaching.entity.User::getId, u -> u));
        var classIds = smap.values().stream().map(com.school.teaching.entity.Student::getClassId).filter(c -> c != null).distinct().toList();
        var cmap = classIds.isEmpty() ? java.util.Map.<Long, Classes>of()
            : classesMapper.selectBatchIds(classIds).stream()
                .collect(java.util.stream.Collectors.toMap(Classes::getId, c -> c));
        for (var sub : subs) {
            var s = smap.get(sub.getStudentId());
            if (s != null) {
                var u = umap.get(s.getUserId());
                if (u != null) sub.setStudentName(u.getRealName());
                var c = cmap.get(s.getClassId());
                if (c != null) { sub.setClassName(c.getClassName()); sub.setGrade(c.getGrade()); }
            }
        }

        // ── 重测相关字段填充 ──
        enrichRetakeFields(subs);
    }

    /** 填充重测相关 transient 字段 */
    private void enrichRetakeFields(List<TaskSubmission> subs) {
        if (subs == null || subs.isEmpty()) return;
        Long taskId = subs.get(0).getTaskId();
        if (taskId == null) return;
        Task task = taskMapper.selectById(taskId);
        if (task == null) return;
        if (task.getPassRate() == null || task.getPassRate() <= 0) return;
        if (task.getMaxAttempts() == null || task.getMaxAttempts() < 1) return;

        BigDecimal totalScore = task.getTotalScore() != null ? task.getTotalScore() : BigDecimal.valueOf(100);
        if (totalScore.compareTo(BigDecimal.ZERO) <= 0) return;
        int passRate = task.getPassRate();
        int maxAttempts = task.getMaxAttempts();
        Integer retakeDeadlineHours = task.getRetakeDeadlineHours();

        // 一次性查出该任务所有学生的全部提交记录
        List<TaskSubmission> allTaskSubs = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId));
        // 按 studentId 分组
        java.util.Map<Long, List<TaskSubmission>> studentSubsMap = allTaskSubs.stream()
            .collect(java.util.stream.Collectors.groupingBy(TaskSubmission::getStudentId));

        for (var sub : subs) {
            Long studentId = sub.getStudentId();
            List<TaskSubmission> studentSubs = studentSubsMap.get(studentId);
            if (studentSubs == null) continue;

            // 按 attemptNumber 排序
            studentSubs.sort(java.util.Comparator.comparingInt(
                s -> s.getAttemptNumber() != null ? s.getAttemptNumber() : 1));

            // 计算达标状态（根据 passMode 选择判定分数）
            BigDecimal score;
            if ("all".equals(task.getPassMode())) {
                score = sub.getScore();
            } else {
                score = sub.getObjectiveScore() != null ? sub.getObjectiveScore() : sub.getScore();
            }
            boolean passed = false;
            if (score != null && totalScore.compareTo(BigDecimal.ZERO) > 0) {
                double rate = score.doubleValue() / totalScore.doubleValue() * 100;
                passed = rate >= passRate;
            }
            sub.setPassed(passed);

            // 是否已有任意轮次达标
            boolean anyPassed = hasAnyAttemptPassed(studentSubs, totalScore, passRate, task.getPassMode());

            // 重测历史
            List<java.util.Map<String, Object>> history = new java.util.ArrayList<>();
            TaskSubmission firstSub = null;
            for (var ss : studentSubs) {
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("id", ss.getId());
                entry.put("score", ss.getScore());
                entry.put("totalScore", totalScore);
                BigDecimal ssScore = "all".equals(task.getPassMode()) ? ss.getScore()
                    : (ss.getObjectiveScore() != null ? ss.getObjectiveScore() : ss.getScore());
                boolean ssPassed = false;
                if (ssScore != null && totalScore.compareTo(BigDecimal.ZERO) > 0) {
                    ssPassed = ssScore.doubleValue() / totalScore.doubleValue() * 100 >= passRate;
                }
                entry.put("passed", ssPassed);
                history.add(entry);
                if (firstSub == null) firstSub = ss;
            }
            sub.setRetakeHistory(history);

            // 首次得分（用于计算 scoreImprove）
            BigDecimal firstScore = firstSub != null ? firstSub.getScore() : null;

            // passType & scoreImprove
            int attemptNum = sub.getAttemptNumber() != null ? sub.getAttemptNumber() : 1;
            sub.setPassType(attemptNum <= 1 ? "first" : "retake");
            if (firstScore != null && score != null && attemptNum > 1) {
                sub.setScoreImprove(score.subtract(firstScore).intValue());
            } else {
                sub.setScoreImprove(0);
            }

            // maxAttempts
            sub.setMaxAttempts(maxAttempts);

            // canRetake: !passed && attemptNum < maxAttempts && 截止未过
            boolean canRetake = !passed && !anyPassed && attemptNum < maxAttempts;
            // 重测截止时间检查
            if (canRetake && retakeDeadlineHours != null && firstSub != null && firstSub.getSubmittedAt() != null) {
                java.time.LocalDateTime deadline = firstSub.getSubmittedAt().plusHours(retakeDeadlineHours);
                if (java.time.LocalDateTime.now().isAfter(deadline)) {
                    canRetake = false;
                }
            }
            sub.setCanRetake(canRetake);

            // passRate（用于前端重测弹窗）
            sub.setPassRate(passRate);

            // remainingAttempts
            sub.setRemainingAttempts(Math.max(0, maxAttempts - attemptNum));

            // retakeDeadline
            if (retakeDeadlineHours != null && firstSub != null && firstSub.getSubmittedAt() != null) {
                sub.setRetakeDeadline(firstSub.getSubmittedAt().plusHours(retakeDeadlineHours).toString());
            } else {
                sub.setRetakeDeadline("");
            }
        }
    }

    /** 判断该学生的所有提交中是否有任意轮次达标 */
    private boolean hasAnyAttemptPassed(List<TaskSubmission> studentSubs,
                                         BigDecimal totalScore, int passRate, String passMode) {
        if (studentSubs == null) return false;
        for (var s : studentSubs) {
            BigDecimal sc = "all".equals(passMode) ? s.getScore()
                : (s.getObjectiveScore() != null ? s.getObjectiveScore() : s.getScore());
            if (sc != null && totalScore.compareTo(BigDecimal.ZERO) > 0) {
                if (sc.doubleValue() / totalScore.doubleValue() * 100 >= passRate) {
                    return true;
                }
            }
        }
        return false;
    }

    private Long resolveCurrentClassId(Long studentId) {
        return studentResolver.resolveCurrentClassId(studentId);
    }

    /** 检查任务截止日期是否已过，过期则抛 BusinessException */
    private void assertNotExpired(Task task, String action) {
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now()))
            throw new BusinessException(400, "任务已过截止日期，不可" + action);
    }

    @Override
    public void updateScoreJson(Long submissionId, String scoreJson) {
        TaskSubmission sub = new TaskSubmission();
        sub.setId(submissionId);
        sub.setScoreJson(scoreJson);
        submissionMapper.updateById(sub);
    }

    @Override
    public void updateMeta(Long submissionId, String comment, Integer isExemplar) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) return;
        // 评语合并到 scoreJson
        if (comment != null && !comment.isBlank()) {
            try {
                String existing = sub.getScoreJson();
                java.util.Map<String, Object> sj;
                if (existing != null && !existing.isBlank()) {
                    sj = new com.fasterxml.jackson.databind.ObjectMapper().readValue(existing, java.util.Map.class);
                } else {
                    sj = new java.util.LinkedHashMap<>();
                }
                sj.put("comment", comment);
                sub.setScoreJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(sj));
            } catch (Exception e) {
                log.warn("更新提交元数据JSON失败: submissionId={}", submissionId, e);
            }
        }
        if (isExemplar != null) sub.setIsExemplar(isExemplar);
        submissionMapper.updateById(sub);
    }

    @Override
    @CacheEvict(value = "submission_status", key = "#studentId + ':' + #taskId")
    public boolean allowExtraSubmit(Long taskId, Long studentId) {
        // 1. 设置特许标记+清除作弊状态（所有提交记录）
        int flagCount = submissionMapper.update(null,
            new LambdaUpdateWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .set(TaskSubmission::getExtraSubmitAllowed, 1)
                .set(TaskSubmission::getCheatTerminated, 0)
                .set(TaskSubmission::getCheatWarnings, 0));
        // 2. 仅将 TERMINATED 状态重置为 PENDING，不误改已提交/已评分记录
        int resetCount = submissionMapper.update(null,
            new LambdaUpdateWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .eq(TaskSubmission::getStatus, "TERMINATED")
                .set(TaskSubmission::getStatus, "PENDING")
                .set(TaskSubmission::getScore, null)
                .set(TaskSubmission::getGradingMessage, null)
                .set(TaskSubmission::getSubmittedAt, null)
                .set(TaskSubmission::getGradedAt, null)
                .set(TaskSubmission::getCreatedAt, java.time.LocalDateTime.now()));
        return flagCount > 0 || resetCount > 0;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskSubmissionServiceImpl.class);

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) return Map.of();
        return JsonUtils.parseMap(json);
    }

    /** 从 taskConfig 中解析考试时长（分钟），解析失败返回 0 */
    private int getDurationMinutes(Task task) {
        try {
            Map<String, Object> cfg = parseConfig(task.getTaskConfig());
            if (cfg.containsKey("durationMinutes")) {
                return Integer.parseInt(cfg.get("durationMinutes").toString());
            }
        } catch (Exception e) {
            log.warn("解析考试时长失败，使用默认值0: taskId={}", task.getId(), e);
        }
        return 0;
    }

    @Override
    @Transactional
    public void saveReflection(Long submissionId, Long studentId, String reflection) {
        TaskSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(404, "提交记录不存在");
        if (!sub.getStudentId().equals(studentId)) throw new BusinessException(403, "无权操作他人的提交");
        sub.setReflection(reflection);
        submissionMapper.updateById(sub);
    }

    @Override
    public Map<String, Object> getSubmissionBoard(Long taskId) {
        com.school.teaching.entity.Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        // 获取班级所有学生
        Long classId = task.getTargetId();
        java.util.List<com.school.teaching.entity.Student> classStudents = studentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.Student>()
                .eq(com.school.teaching.entity.Student::getClassId, classId));

        // 获取该任务的所有提交
        java.util.List<com.school.teaching.entity.TaskSubmission> subs = submissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                .eq(com.school.teaching.entity.TaskSubmission::getTaskId, taskId));

        // 填充达标相关字段（passed/passRate/canRetake 等），供看板显示
        enrichRetakeFields(subs);

        // 建立 studentId -> submission 映射（按状态优先级 + submittedAt 时间戳排重）
        java.util.Map<Long, com.school.teaching.entity.TaskSubmission> subMap = subs.stream()
            .collect(java.util.stream.Collectors.toMap(
                com.school.teaching.entity.TaskSubmission::getStudentId,
                s -> s, (a, b) -> pickBetterSubmission(a, b)));

        // 批量查学生姓名
        java.util.Set<Long> userIds = classStudents.stream()
            .map(com.school.teaching.entity.Student::getUserId).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                com.school.teaching.entity.User::getId,
                com.school.teaching.entity.User::getRealName,
                (a, b) -> a));

        // 构建看板数据
        int submitted = 0, unsubmitted = 0, graded = 0, terminated = 0, cheating = 0;
        double totalScore = 0;
        int scoredCount = 0;
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();

        for (com.school.teaching.entity.Student student : classStudents) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("studentName", nameMap.getOrDefault(student.getUserId(), "未知"));
            row.put("studentNo", student.getStudentNumber());

            com.school.teaching.entity.TaskSubmission sub = subMap.get(student.getId());
            if (sub == null) {
                row.put("status", "NOT_STARTED");  // 完全未开始
                row.put("statusLabel", "未提交");
                unsubmitted++;
            } else {
                String st = sub.getStatus();
                row.put("status", st);
                row.put("submissionId", sub.getId());
                row.put("score", sub.getScore());
                row.put("gradeLevel", sub.getGradeLevel());
                row.put("submittedAt", sub.getSubmittedAt());
                row.put("createdAt", sub.getCreatedAt());
                row.put("cheatWarnings", sub.getCheatWarnings());
                row.put("cheatTerminated", sub.getCheatTerminated());

                switch (st) {
                    case "SUBMITTED", "GRADED", "RETURNED" -> {
                        row.put("statusLabel", "已提交");
                        submitted++;
                        if ("GRADED".equals(st)) graded++;
                    }
                    case "PENDING" -> {
                        row.put("statusLabel", "进行中");
                        unsubmitted++;
                    }
                    case "TERMINATED" -> {
                        row.put("statusLabel", "作弊终止");
                        terminated++;
                    }
                    case "EXEMPTED" -> {
                        row.put("statusLabel", "已豁免");
                    }
                    default -> {
                        row.put("statusLabel", st);
                        unsubmitted++;
                    }
                }
                if (sub.getCheatWarnings() != null && sub.getCheatWarnings() > 0) cheating++;
                if (sub.getScore() != null) { totalScore += sub.getScore().doubleValue(); scoredCount++; }

                // 达标相关字段
                row.put("passed", sub.getPassed());
                row.put("passRate", sub.getPassRate());
                row.put("canRetake", sub.getCanRetake());
                row.put("maxAttempts", sub.getMaxAttempts());
                row.put("attemptNumber", sub.getAttemptNumber());
                row.put("passType", sub.getPassType());
                row.put("scoreImprove", sub.getScoreImprove());
            }
            rows.add(row);
        }

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", classStudents.size());
        result.put("submitted", submitted);
        result.put("unsubmitted", unsubmitted);
        result.put("graded", graded);
        result.put("terminated", terminated);
        result.put("cheating", cheating);
        result.put("avgScore", scoredCount > 0 ? Math.round(totalScore / scoredCount * 10) / 10.0 : 0);
        com.school.teaching.entity.Classes cls = classesMapper.selectById(classId);
        result.put("className", cls != null ? cls.getClassName() : "");
        result.put("taskTitle", task.getTitle());
        result.put("deadline", task.getDeadline());
        return result;
    }

    /**
     * 当同一学生有多个提交记录时，按状态优先级选择最佳记录。
     * 优先级: GRADED > SUBMITTED > PENDING > TERMINATED > EXPIRED > 其他
     * 同状态时用 submittedAt 时间戳（最新优先）排重。
     */
    private com.school.teaching.entity.TaskSubmission pickBetterSubmission(
            com.school.teaching.entity.TaskSubmission a, com.school.teaching.entity.TaskSubmission b) {
        int pa = statusPriority(a.getStatus());
        int pb = statusPriority(b.getStatus());
        if (pa != pb) return pa > pb ? a : b;
        // 同状态：比较 submittedAt，null 视为最旧
        if (a.getSubmittedAt() == null && b.getSubmittedAt() == null) return a;
        if (a.getSubmittedAt() == null) return b;
        if (b.getSubmittedAt() == null) return a;
        return a.getSubmittedAt().isAfter(b.getSubmittedAt()) ? a : b;
    }

    private int statusPriority(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "GRADED", "RETURNED" -> 6;
            case "SUBMITTED" -> 5;
            case "PENDING" -> 3;
            case "TERMINATED" -> 2;
            case "EXPIRED" -> 1;
            default -> 0;
        };
    }

    @Override
    public Map<String, Object> remindUnsubmitted(Long taskId) {
        com.school.teaching.entity.Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        Long classId = task.getTargetId();
        java.util.List<com.school.teaching.entity.Student> classStudents = studentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.Student>()
                .eq(com.school.teaching.entity.Student::getClassId, classId));

        java.util.List<com.school.teaching.entity.TaskSubmission> subs = submissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                .eq(com.school.teaching.entity.TaskSubmission::getTaskId, taskId));

        java.util.Set<Long> submittedIds = subs.stream()
            .filter(s -> "SUBMITTED".equals(s.getStatus()) || "GRADED".equals(s.getStatus()) || "RETURNED".equals(s.getStatus()))
            .map(com.school.teaching.entity.TaskSubmission::getStudentId)
            .collect(java.util.stream.Collectors.toSet());

        int reminded = 0;
        for (com.school.teaching.entity.Student student : classStudents) {
            if (!submittedIds.contains(student.getId())) {
                try {
                    com.school.teaching.entity.Notification notif = new com.school.teaching.entity.Notification();
                    notif.setUserId(student.getUserId());
                    notif.setTitle("任务提醒");
                    notif.setContent("老师提醒你及时完成「" + task.getTitle() + "」，截止时间：" +
                        (task.getDeadline() != null ? task.getDeadline().toString() : "无"));
                    notif.setType("TASK_REMIND");
                    notif.setRelatedId(taskId);
                    notif.setIsRead(0);
                    notif.setCreateTime(java.time.LocalDateTime.now());
                    notificationMapper.insert(notif);
                    reminded++;
                } catch (Exception e) {
                    log.warn("提醒通知发送失败: studentId={}", student.getId(), e);
                }
            }
        }

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("reminded", reminded);
        result.put("total", classStudents.size() - submittedIds.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> restartUnfinished(Long taskId) {
        com.school.teaching.entity.Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        // 获取该任务的所有提交记录
        java.util.List<com.school.teaching.entity.TaskSubmission> allSubs = submissionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.school.teaching.entity.TaskSubmission>()
                .eq(com.school.teaching.entity.TaskSubmission::getTaskId, taskId));

        int restarted = 0;
        int skipped = 0;

        for (com.school.teaching.entity.TaskSubmission sub : allSubs) {
            String st = sub.getStatus();
            // 已完成的学生不受影响
            if ("SUBMITTED".equals(st) || "GRADED".equals(st) || "RETURNED".equals(st) || "EXEMPTED".equals(st)) {
                skipped++;
                continue;
            }

            // TERMINATED：清除作弊标记+特许重做，让学生重新开始
            if ("TERMINATED".equals(st)) {
                // 先清理该生已有的 PENDING 记录（防重复，restartUnfinished 多次调用时积累）
                submissionMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.school.teaching.entity.TaskSubmission>()
                        .eq(com.school.teaching.entity.TaskSubmission::getTaskId, taskId)
                        .eq(com.school.teaching.entity.TaskSubmission::getStudentId, sub.getStudentId())
                        .eq(com.school.teaching.entity.TaskSubmission::getStatus, "PENDING")
                        .set(com.school.teaching.entity.TaskSubmission::getStatus, "EXPIRED"));
                submissionMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.school.teaching.entity.TaskSubmission>()
                        .eq(com.school.teaching.entity.TaskSubmission::getId, sub.getId())
                        .set(com.school.teaching.entity.TaskSubmission::getExtraSubmitAllowed, 1)
                        .set(com.school.teaching.entity.TaskSubmission::getCheatTerminated, 0)
                        .set(com.school.teaching.entity.TaskSubmission::getCheatWarnings, 0)
                        .set(com.school.teaching.entity.TaskSubmission::getStatus, "PENDING")
                        .set(com.school.teaching.entity.TaskSubmission::getScore, null)
                        .set(com.school.teaching.entity.TaskSubmission::getGradingMessage, null)
                        .set(com.school.teaching.entity.TaskSubmission::getSubmittedAt, null)
                        .set(com.school.teaching.entity.TaskSubmission::getGradedAt, null)
                        .set(com.school.teaching.entity.TaskSubmission::getCreatedAt, java.time.LocalDateTime.now()));
                // 清除缓存，确保学生端能看到最新状态
                Cache cache = cacheManager.getCache("submission_status");
                if (cache != null) cache.evict(sub.getStudentId() + ":" + taskId);
                restarted++;
                continue;
            }

            // PENDING：刷新开始时间防止超时误判
            if ("PENDING".equals(st)) {
                submissionMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.school.teaching.entity.TaskSubmission>()
                        .eq(com.school.teaching.entity.TaskSubmission::getId, sub.getId())
                        .set(com.school.teaching.entity.TaskSubmission::getCheatWarnings, 0)
                        .set(com.school.teaching.entity.TaskSubmission::getCheatTerminated, 0)
                        .set(com.school.teaching.entity.TaskSubmission::getExtraSubmitAllowed, 1)
                        .set(com.school.teaching.entity.TaskSubmission::getCreatedAt, java.time.LocalDateTime.now()));
                // 清除缓存，确保学生端能看到最新状态
                Cache cache = cacheManager.getCache("submission_status");
                if (cache != null) cache.evict(sub.getStudentId() + ":" + taskId);
                restarted++;
            }
        }

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("restarted", restarted);
        result.put("skipped", skipped);
        result.put("message", "已重启 " + restarted + " 名学生的考试权限，" + skipped + " 名已完成学生不受影响");
        return result;
    }

    @Override @Transactional
    public TaskSubmission manualRetake(Long taskId, Long studentId, Long teacherId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");
        if (task.getPassRate() == null || task.getPassRate() <= 0)
            throw new BusinessException(400, "该任务未启用达标模式，无需重测");

        // 查找该生最高 attempt 记录
        TaskSubmission latest = submissionMapper.selectOne(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .orderByDesc(TaskSubmission::getAttemptNumber)
                .last("LIMIT 1"));
        if (latest == null) throw new BusinessException(404, "该学生无提交记录");

        int nextAttempt = (latest.getAttemptNumber() != null ? latest.getAttemptNumber() : 0) + 1;
        if (nextAttempt > (task.getMaxAttempts() != null ? task.getMaxAttempts() : 2))
            throw new BusinessException(400, "已达最大重测次数，无法手动触发");

        // 快照 passRate
        java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("passRateAtCreation", task.getPassRate());
        meta.put("teacherManualTrigger", true);
        meta.put("triggeredBy", teacherId);
        String jsonSnapshot = null;
        try {
            jsonSnapshot = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(meta);
        } catch (Exception e) {
            log.warn("重测元数据JSON序列化失败: taskId={}, studentId={}", taskId, studentId, e);
        }

        TaskSubmission retake = new TaskSubmission();
        retake.setTaskId(taskId);
        retake.setStudentId(studentId);
        retake.setAttemptNumber(nextAttempt);
        retake.setIsOfficial(false);
        retake.setResubmissionOf(latest.getResubmissionOf() != null ? latest.getResubmissionOf() : latest.getId());
        retake.setStatus("PENDING");
        retake.setScoreJson(jsonSnapshot);
        submissionMapper.insert(retake);

        log.info("教师手动触发重测: taskId={}, studentId={}, attempt={}, teacherId={}",
            taskId, studentId, nextAttempt, teacherId);
        return retake;
    }

    @Override @Transactional
    public void manualPass(Long taskId, Long studentId, Long teacherId, String reason) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        // 找到该生最新一条 SUBMITTED/GRADED 记录
        TaskSubmission latest = submissionMapper.selectOne(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .in(TaskSubmission::getStatus, "SUBMITTED", "GRADED")
                .orderByDesc(TaskSubmission::getAttemptNumber)
                .last("LIMIT 1"));
        if (latest == null) throw new BusinessException(404, "该学生无已提交记录");

        // 在 scoreJson 中标记教师手动通过
        java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
        try {
            if (latest.getScoreJson() != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> existing = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(latest.getScoreJson(), java.util.Map.class);
                meta.putAll(existing);
            }
        } catch (Exception e) {
            log.warn("解析提交元数据JSON失败，使用空元数据: submissionId={}", latest.getId(), e);
        }
        meta.put("teacherManualPass", true);
        meta.put("teacherManualPassReason", reason != null ? reason : "");
        meta.put("teacherManualPassBy", teacherId);
        meta.put("teacherManualPassAt", java.time.LocalDateTime.now().toString());
        try {
            latest.setScoreJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(meta));
        } catch (Exception e) {
            log.warn("序列化提交元数据JSON失败: submissionId={}", latest.getId(), e);
        }

        // 如果该提交不是 GRADED 状态，更新为 GRADED
        if (!"GRADED".equals(latest.getStatus())) {
            latest.setStatus("GRADED");
            latest.setGradedAt(java.time.LocalDateTime.now());
            latest.setGradedBy(teacherId);
        }
        submissionMapper.updateById(latest);

        // 关闭该生其余 PENDING 重测
        List<TaskSubmission> pendings = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .eq(TaskSubmission::getStatus, "PENDING"));
        for (TaskSubmission p : pendings) {
            p.setStatus("EXEMPTED");
            p.setGradingMessage("教师手动通过，重测已豁免");
            submissionMapper.updateById(p);
        }

        log.info("教师手动标记通过: taskId={}, studentId={}, teacherId={}, reason={}",
            taskId, studentId, teacherId, reason);
    }

    @Override @Transactional
    public void closeRetake(Long taskId, Long studentId, Long teacherId) {
        List<TaskSubmission> pendings = submissionMapper.selectList(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .eq(TaskSubmission::getStudentId, studentId)
                .eq(TaskSubmission::getStatus, "PENDING"));
        if (pendings.isEmpty()) return;

        for (TaskSubmission p : pendings) {
            p.setStatus("EXEMPTED");
            p.setGradingMessage("教师手动关闭重测通道 (teacherId=" + teacherId + ")");
            submissionMapper.updateById(p);
        }

        log.info("教师关闭重测通道: taskId={}, studentId={}, teacherId={}, closedCount={}",
            taskId, studentId, teacherId, pendings.size());
    }

    @Override
    public long countSubmissionsByTaskAndStatus(Long taskId, List<String> statuses) {
        return submissionMapper.selectCount(
            new LambdaQueryWrapper<TaskSubmission>()
                .eq(TaskSubmission::getTaskId, taskId)
                .in(TaskSubmission::getStatus, statuses));
    }
}
