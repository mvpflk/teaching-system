-- V061__agent_feedback_indexes.sql
-- 审查反馈：补充复合索引优化 extractFromFeedback 查询性能

-- agent_feedback: (user_id, created_at) 复合索引，覆盖按用户+时间的标签统计查询
ALTER TABLE agent_feedback ADD INDEX idx_user_created (user_id, created_at);

-- agent_user_memory: 替换单列索引为覆盖索引，优化 getMemoriesForPrompt 查询
-- 原 idx_user_confidence (user_id, confidence) → 改为 (user_id, status, confidence)
ALTER TABLE agent_user_memory DROP INDEX idx_user_confidence;
ALTER TABLE agent_user_memory ADD INDEX idx_user_status_conf (user_id, status, confidence);
