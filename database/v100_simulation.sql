-- v83: Windows 仿真模块 — 任务定义 + 操作录制
CREATE TABLE IF NOT EXISTS simulation_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT COMMENT '关联 tasks.id',
    node_id BIGINT COMMENT '关联 knowledge_nodes.id',
    task_json MEDIUMTEXT NOT NULL COMMENT '任务定义 JSON（steps+initialState+successCriteria）',
    initial_vfs MEDIUMTEXT COMMENT '初始文件系统快照 JSON',
    mode VARCHAR(20) NOT NULL DEFAULT 'practice' COMMENT 'practice/exam',
    difficulty INT DEFAULT 1 COMMENT '1-5',
    time_limit INT DEFAULT 120 COMMENT '时限(秒)',
    created_by BIGINT,
    school_id BIGINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_node_id (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS simulation_recordings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT COMMENT '关联 task_submissions.id',
    student_id BIGINT NOT NULL,
    events_json MEDIUMTEXT COMMENT '操作事件数组 JSON',
    event_count INT DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    success TINYINT DEFAULT 0,
    auto_score DECIMAL(5,2) DEFAULT 0.00,
    teacher_notes TEXT,
    school_id BIGINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_submission (submission_id),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
