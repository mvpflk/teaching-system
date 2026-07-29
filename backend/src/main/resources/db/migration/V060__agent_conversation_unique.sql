-- V060__agent_conversation_unique.sql
-- 为 agent_conversations 加唯一索引，保证 upsert 原子性

ALTER TABLE agent_conversations ADD UNIQUE KEY uk_session (session_id);
