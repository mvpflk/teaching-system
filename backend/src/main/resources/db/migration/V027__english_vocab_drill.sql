-- Original file: v151_english_vocab_drill.sql (DDL extracted from MIXED file)
-- ============================================================

-- ============================================================================
-- v151: 英语[职高]知识库 — 单词记忆模块 (render_type=vocab_drill)
-- 1. knowledge_nodes 增加 render_type 列
-- 2. 词汇积累下新增 L3 节点 "单词记忆" (render_type='vocab_drill')
-- 幂等: ALTER IGNORE + INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;

-- 1. 增加 render_type 列
ALTER TABLE knowledge_nodes
  ADD COLUMN render_type VARCHAR(50) DEFAULT NULL
  COMMENT '自定义渲染类型: vocab_drill=背单词模块, NULL=标准Markdown文章';