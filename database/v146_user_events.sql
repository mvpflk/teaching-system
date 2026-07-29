-- v146: 用户行为事件埋点表 — R90组卷/诊断功能使用数据采集
CREATE TABLE IF NOT EXISTS user_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_name VARCHAR(30),
    event_type VARCHAR(50) NOT NULL COMMENT 'EXAM_PAPER_GENERATE/DIAGNOSIS_START/DIAGNOSIS_SATISFACTION...',
    event_data JSON COMMENT '灵活元数据: {subject,mode,questionCount,editedCount,layer,satisfied,...}',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_type (event_type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
