-- V057__agent_support.sql
-- Agent 子系统支撑表：会话元数据 + 工具调用审计日志

CREATE TABLE IF NOT EXISTS agent_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL COMMENT '会话UUID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    agent_type VARCHAR(20) NOT NULL COMMENT 'Agent类型：LESSON_PREP/STUDY_BUDDY/ANALYTICS',
    title VARCHAR(60) NOT NULL DEFAULT '新对话' COMMENT '会话标题（模型自动生成, 最多10字）',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数',
    token_count INT NOT NULL DEFAULT 0 COMMENT 'Token估算',
    created_at DATETIME DEFAULT NOW(),
    updated_at DATETIME DEFAULT NOW() ON UPDATE NOW(),
    INDEX idx_user (user_id, updated_at DESC),
    INDEX idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话元数据';

CREATE TABLE IF NOT EXISTS agent_tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    tool_name VARCHAR(64) NOT NULL COMMENT '工具名称',
    tool_args JSON COMMENT '工具参数',
    access_granted TINYINT(1) NOT NULL COMMENT '1=通过 0=拒绝',
    deny_reason VARCHAR(200) COMMENT '拒绝原因',
    execution_time_ms INT COMMENT '执行耗时(ms)',
    result_summary VARCHAR(500) COMMENT '结果摘要',
    created_at DATETIME DEFAULT NOW(),
    INDEX idx_user (user_id, created_at),
    INDEX idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具调用审计日志';
