package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务提交表 — 学生提交作业/考试等统一记录。
 */
@Data
@TableName("task_submissions")
public class TaskSubmission implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long studentId;

    private Long schoolId;
    private Long stageId;

    private String content;
    private String attachments;

    private BigDecimal score;
    private String gradeLevel;
    private String scoreJson;

    private String status;

    private Long gradedBy;
    private String gradeType;

    private Integer cheatWarnings;
    private Integer cheatTerminated;

    private Long resubmissionOf;
    private Integer includeInPortfolio;
    private BigDecimal peerScore;
    private Integer isExemplar;
    private Integer extraSubmitAllowed;

    private String reflection;

    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String grade;

    @TableField(exist = false)
    private BigDecimal objectiveScore;
    @TableField(exist = false)
    private Integer subjectivePendingCount;

    // ── 重测相关 transient 字段（由 enrichSubmissions / submit 填充） ──
    /** 是否达标（score/totalScore × 100 ≥ passRate） */
    @TableField(exist = false)
    private Boolean passed;
    /** 是否可重测（未达标 + attemptNumber < maxAttempts + 截止未过） */
    @TableField(exist = false)
    private Boolean canRetake;
    /** 任务最大尝试次数（来自 Task.maxAttempts） */
    @TableField(exist = false)
    private Integer maxAttempts;
    /** 与首次得分的差值 */
    @TableField(exist = false)
    private Integer scoreImprove;
    /** 达标类型：'first' | 'retake' */
    @TableField(exist = false)
    private String passType;
    /** 重测历史（同任务+同学生的所有 submission 摘要） */
    @TableField(exist = false)
    private List<java.util.Map<String, Object>> retakeHistory;
    /** 达标得分率（来自 Task.passRate） */
    @TableField(exist = false)
    private Integer passRate;
    /** 剩余重测次数 */
    @TableField(exist = false)
    private Integer remainingAttempts;
    /** 重测截止时间（首次提交时间 + retakeDeadlineHours） */
    @TableField(exist = false)
    private String retakeDeadline;

    /** 评分/终止提示信息 */
    private String gradingMessage;
    /** 第几次作答（1=首次，2=重测1，3=重测2）。对应 task_submissions.attempt_number */
    private Integer attemptNumber;
    /** 是否为正式成绩（首次=true，重测=false）。对应 task_submissions.is_official */
    private Boolean isOfficial;
    private BigDecimal rubricTotalScore;
    private LocalDateTime rubricScoredAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
