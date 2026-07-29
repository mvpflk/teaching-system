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

-- 2. 找到词汇积累 L2 节点 (subject_id=24)
SET @l2_vocab = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='词汇积累' AND level=2 LIMIT 1);

-- 3. 插入单词记忆 L3 节点 (幂等)
INSERT IGNORE INTO knowledge_nodes
  (parent_id, subject_id, level, name, content, sort_order, status, render_type)
VALUES
  (@l2_vocab, 24, 3, '单词记忆', '', 99, 'ACTIVE', 'vocab_drill');

SELECT CONCAT('v151: render_type added, vocab_drill node inserted under vocab (parent=', @l2_vocab, ')') AS result;
