package com.school.teaching.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异步任务实体 — 持久化到 async_task 表
 */
@Data
@TableName("async_task")
public class AsyncTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外暴露的任务ID（8位UUID短码） */
    private String taskId;

    /** 任务类型: AI_GENERATE / AI_GRADING / ZIP_EXPORT / AI_SUPPLEMENT */
    private String taskType;

    /** PENDING / RUNNING / COMPLETED / FAILED / TIMEOUT / CANCELLED */
    private String status;

    /** 执行状态（与 status 一致，冗余用于后续工作流扩展） */
    private String runState;

    /** 结果 JSON（Object 序列化） */
    private String resultJson;

    /** 错误信息 */
    private String errorMessage;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetries;

    /** 超时秒数 */
    private Integer timeoutSeconds;

    /** 硬超时时间点 */
    private LocalDateTime timeoutAt;

    /** 创建者用户ID */
    private Long createdBy;

    /** 学校ID */
    private Long schoolId;

    /** 开始执行时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
