-- V065__prompt_template.sql
-- AI 提示词模板 & Agent 对话提示词版本记录

CREATE TABLE IF NOT EXISTS prompt_template (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    type          VARCHAR(10) NOT NULL COMMENT 'TEMPLATE / FINAL',
    name          VARCHAR(50) NOT NULL COMMENT '模板唯一标识，如 lesson_prep',
    label         VARCHAR(100) NOT NULL COMMENT '中文名称',
    subject       VARCHAR(20) DEFAULT NULL COMMENT '关联学科，NULL=通用',
    version       INT NOT NULL DEFAULT 1,
    content       TEXT NOT NULL COMMENT '提示词正文',
    is_active     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '当前生效版本',
    created_by    VARCHAR(50) DEFAULT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name_version (name, version),
    INDEX idx_active (name, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 提示词模板';

CREATE TABLE IF NOT EXISTS agent_session_prompt (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id    VARCHAR(64) NOT NULL,
    template_name VARCHAR(50) NOT NULL,
    subject       VARCHAR(20) DEFAULT NULL COMMENT '方便按学科维度分析',
    version       INT NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_template (template_name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 对话使用的提示词版本';
