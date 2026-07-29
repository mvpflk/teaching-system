-- V059__agent_user_memory.sql
-- Agent 自我进化二期：用户记忆表 — 从反馈中学习，让 AI 越来越懂用户

CREATE TABLE IF NOT EXISTS agent_user_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    memory_type VARCHAR(20) NOT NULL COMMENT 'PREFERENCE=偏好 PATTERN=模式 CORRECTION=纠正 FACT=事实',
    memory_key VARCHAR(128) NOT NULL COMMENT '记忆键：answer_style / effective_tools / weak_point 等',
    memory_value JSON NOT NULL COMMENT '记忆值（结构化JSON）',
    confidence DECIMAL(3,2) NOT NULL DEFAULT 0.50 COMMENT '置信度 0.00-1.00，<0.6 不注入prompt',
    evidence_count INT NOT NULL DEFAULT 1 COMMENT '证据次数',
    last_evidence_at DATETIME COMMENT '最后证据时间',
    source_session_id VARCHAR(36) COMMENT '来源会话ID',
    expires_at DATETIME COMMENT '过期时间：PREFERENCE/PATTERN 30天，FACT/CORRECTION 不过期',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / SUPERSEDED / EXPIRED',
    school_id BIGINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_memory (user_id, memory_type, memory_key),
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_confidence (user_id, confidence DESC),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent用户记忆表 — 自我进化核心';
