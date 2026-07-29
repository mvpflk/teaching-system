-- v124: 异步任务持久化 (2026-06-04)
-- 将异步任务从 Redis/内存迁移到 MySQL，保留 Redis 作为热缓存层

CREATE TABLE IF NOT EXISTS async_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         VARCHAR(32)     NOT NULL COMMENT '对外暴露的任务ID（8位UUID短码）',
    task_type       VARCHAR(30)     NOT NULL COMMENT '任务类型: AI_GENERATE/AI_GRADING/ZIP_EXPORT/AI_SUPPLEMENT',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/TIMEOUT/CANCELLED',
    run_state       VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '执行状态',
    result_json     MEDIUMTEXT      NULL     COMMENT '结果JSON',
    error_message   VARCHAR(1000)   NULL     COMMENT '错误信息',
    retry_count     INT             NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retries     INT             NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    timeout_seconds INT             NOT NULL DEFAULT 30 COMMENT '超时秒数',
    timeout_at      DATETIME        NULL     COMMENT '硬超时时间点',
    created_by      BIGINT          NULL     COMMENT '创建者用户ID',
    school_id       BIGINT          NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at      DATETIME        NULL     COMMENT '开始执行时间',
    completed_at    DATETIME        NULL     COMMENT '完成时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX uk_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_task_type (task_type),
    INDEX idx_created_at (created_at),
    INDEX idx_timeout (status, timeout_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务持久化表';
