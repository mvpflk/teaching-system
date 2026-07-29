-- V058__agent_feedback.sql
-- Agent 自我进化一期：用户反馈收集

CREATE TABLE IF NOT EXISTS agent_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL COMMENT '会话UUID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    message_index INT NOT NULL DEFAULT 0 COMMENT '消息序号（第几轮对话）',
    rating TINYINT NOT NULL COMMENT '评分：1=踩 3=中性 5=赞',
    feedback_tags VARCHAR(500) COMMENT '标签，逗号分隔：准确,实用,太啰嗦,太简略,答非所问,有错误',
    comment TEXT COMMENT '用户可选备注',
    user_question TEXT COMMENT '用户当时的问题（截取前500字）',
    agent_answer_snippet VARCHAR(1000) COMMENT 'AI回答摘要（截取前200字）',
    tools_used VARCHAR(500) COMMENT '该轮使用的工具名，逗号分隔',
    school_id BIGINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_rating (rating),
    INDEX idx_role_name (role_name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent用户反馈表 — 自我进化数据基础';
