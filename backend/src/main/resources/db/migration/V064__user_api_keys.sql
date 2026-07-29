-- V064__user_api_keys.sql
-- 用户自有 API Key 管理 — BYOK (Bring Your Own Key)
-- 每个用户可以配置自己的 API Key，当免费额度用完后自动降级使用

CREATE TABLE IF NOT EXISTS user_api_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    label VARCHAR(50) NOT NULL COMMENT '用户自定义标识，如"我的DeepSeek"',
    base_url VARCHAR(256) NOT NULL COMMENT 'API base URL',
    encrypted_key VARCHAR(512) NOT NULL COMMENT 'AES-256-GCM 加密的 API Key',
    model VARCHAR(64) NOT NULL COMMENT '模型名，如 deepseek-chat',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    call_count INT DEFAULT 0 COMMENT '今日调用次数',
    last_used_at DATETIME COMMENT '最后调用时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_key (user_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户自有API Key — 免费额度耗尽后自动降级';