-- ============================================================
-- v167: 知识卡片系统增强 — 卡片质量+考纲权重
-- 在现有表上扩展字段，不新建表
-- 日期: 2026-06-22
-- ============================================================
SET NAMES utf8mb4;

-- knowledge_flashcards 扩展
ALTER TABLE knowledge_flashcards
  ADD COLUMN card_type          VARCHAR(20)  DEFAULT 'DEFINITION' COMMENT '卡片类型: DEFINITION/PROCEDURE/COMPARISON/APPLICATION/SCENARIO',
  ADD COLUMN quality_score      DECIMAL(3,2) DEFAULT NULL     COMMENT 'AI综合评分 0-100, NULL=未评估',
  ADD COLUMN ai_comment         VARCHAR(500) DEFAULT NULL     COMMENT 'AI评语JSON',
  ADD COLUMN review_status      VARCHAR(20)  DEFAULT 'PENDING' COMMENT '审核状态: PENDING/APPROVED/REJECTED',
  ADD COLUMN linked_question_id BIGINT       DEFAULT NULL     COMMENT '关联验证题 → question_bank.id',
  ADD COLUMN context_path       VARCHAR(300) DEFAULT NULL     COMMENT '知识点完整路径',
  ADD COLUMN reviewed_by        BIGINT       DEFAULT NULL     COMMENT '审核教师 → users.id',
  ADD COLUMN reviewed_at        DATETIME     DEFAULT NULL     COMMENT '审核时间';

CREATE INDEX idx_fc_review  ON knowledge_flashcards(review_status, quality_score);
CREATE INDEX idx_fc_type    ON knowledge_flashcards(card_type);
CREATE INDEX idx_fc_linked  ON knowledge_flashcards(linked_question_id);

-- 存量数据：已有卡片默认 APPROVED，不阻塞现有功能
UPDATE knowledge_flashcards SET review_status = 'APPROVED' WHERE review_status = 'PENDING';

-- knowledge_nodes 扩展
ALTER TABLE knowledge_nodes
  ADD COLUMN exam_weight VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '考纲权重: HIGH/MEDIUM/LOW, 教师标记';

CREATE INDEX idx_node_weight ON knowledge_nodes(exam_weight);
