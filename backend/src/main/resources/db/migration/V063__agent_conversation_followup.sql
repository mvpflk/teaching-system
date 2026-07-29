-- A-4 学习效果闭环：Agent 会话关联后续任务
ALTER TABLE agent_conversations
    ADD COLUMN follow_up_task_id BIGINT DEFAULT NULL COMMENT '关联的任务ID（create_task 工具创建）' AFTER token_count;

ALTER TABLE agent_conversations
    ADD INDEX idx_followup_task (follow_up_task_id);