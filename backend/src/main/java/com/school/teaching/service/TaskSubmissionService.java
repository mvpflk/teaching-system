package com.school.teaching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.teaching.entity.TaskSubmission;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TaskSubmissionService {

    /** 学生开始答题 — 创建 PENDING 状态记录，返回 {submissionId, taskConfig} */
    Map<String, Object> startExam(Long taskId, Long studentId);

    /** 学生提交任务 — 自动选择 Handler 处理 */
    TaskSubmission submit(Long taskId, Long studentId, Map<String, Object> payload);

    /** 教师评分 */
    TaskSubmission grade(Long submissionId, BigDecimal score, String gradeLevel, Long gradedBy);

    /** 教师评分（含 exemplary 标记，自动推荐到展示墙） */
    TaskSubmission grade(Long submissionId, BigDecimal score, String gradeLevel, Long gradedBy, Integer isExemplar, String comment);

    /** 评分回退 — 将 GRADED 重置为 SUBMITTED */
    TaskSubmission regrade(Long submissionId);

    TaskSubmission getByTaskAndStudent(Long taskId, Long studentId);
    List<TaskSubmission> getByTaskId(Long taskId);
    IPage<TaskSubmission> pageByTaskId(Long taskId, Page<TaskSubmission> page, String keyword);

    /** 特许单个学生补交（即使任务已截止） */
    boolean allowExtraSubmit(Long taskId, Long studentId);

    /** 更新提交的 scoreJson（评分理由等） */
    void updateScoreJson(Long submissionId, String scoreJson);

    /** 更新已评分提交的评语/推荐等元数据（不改变分数和状态） */
    void updateMeta(Long submissionId, String comment, Integer isExemplar);

    /** 保存学习反思 — 校验学生本人操作 */
    void saveReflection(Long submissionId, Long studentId, String reflection);

    /** 提交看板：班级全员提交状态 + 统计 + 异常行为 */
    Map<String, Object> getSubmissionBoard(Long taskId);

    /** 一键提醒未提交学生 */
    Map<String, Object> remindUnsubmitted(Long taskId);

    /** 一键重启未完成学生 — 重置所有未交/终止学生的考试权限，已完成学生不受影响 */
    Map<String, Object> restartUnfinished(Long taskId);

    /** 教师手动触发学生重测。用于首次因病/设备异常等情况。返回新创建的 PENDING submission。 */
    TaskSubmission manualRetake(Long taskId, Long studentId, Long teacherId);

    /** 教师手动标记学生通过。不影响首次成绩（isOfficial 不变）。记录审计日志。 */
    void manualPass(Long taskId, Long studentId, Long teacherId, String reason);

    /** 关闭该生的重测通道。将未完成的 PENDING 标记为 EXEMPTED。 */
    void closeRetake(Long taskId, Long studentId, Long teacherId);

    /** 统计指定任务的提交数（按状态过滤） */
    long countSubmissionsByTaskAndStatus(Long taskId, List<String> statuses);
}