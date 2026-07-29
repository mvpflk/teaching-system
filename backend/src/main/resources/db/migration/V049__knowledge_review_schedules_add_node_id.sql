-- 闯关练习 SM-2：knowledge_review_schedules 新增 node_id / source_type / last_push_at
-- 支持闯关关键词按知识节点维度执行 SM-2 自适应间隔

ALTER TABLE knowledge_review_schedules
  ADD COLUMN node_id        BIGINT      DEFAULT NULL COMMENT '知识节点ID（闯关使用，与flashcard_id二选一）' AFTER flashcard_id,
  ADD COLUMN source_type    VARCHAR(20) DEFAULT 'FLASHCARD' COMMENT '来源类型：FLASHCARD=知识库卡片, CHECKPOINT=闯关关键词' AFTER node_id,
  ADD COLUMN last_push_at   DATETIME    DEFAULT NULL COMMENT '上次推送时间（防重复推送）' AFTER is_mastered,
  ADD INDEX idx_krs_student_node (student_id, node_id) COMMENT '支持按知识节点查询排程';
