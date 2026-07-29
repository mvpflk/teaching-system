-- Original file: v84_knowledge_nodes_grammar_extension.sql (DDL extracted from MIXED file)
-- ============================================================

-- v84: knowledge_nodes 扩展语法树支持
-- Applied: 2026-06-05
-- 新增 grammar_category + unlock_stage 两列
-- 插入 6 大类 + 20 子节点语法树种子数据
SET NAMES utf8mb4;

ALTER TABLE knowledge_nodes
  ADD COLUMN grammar_category VARCHAR(30) DEFAULT NULL
    COMMENT '语法分类: tense/passive/clause/non_finite/lexical/sentence',
  ADD COLUMN unlock_stage INT DEFAULT NULL
    COMMENT '解锁所需阶段 1-7 (仅语法节点)';

ALTER TABLE question_bank
  ADD COLUMN grammar_node_id BIGINT DEFAULT NULL
    COMMENT '关联 knowledge_nodes.id (语法节点)';