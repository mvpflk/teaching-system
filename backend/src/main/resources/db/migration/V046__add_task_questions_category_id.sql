-- v046: task_questions 加 category_id，支撑学情分析按知识点聚合
-- 背景：T-02 knowledgeNodeId 发布链路丢失，学情分析断裂
-- 只加列，不设 NOT NULL（历史数据无此信息，不影响已有查询）

ALTER TABLE `task_questions`
  ADD COLUMN `category_id` bigint(20) DEFAULT NULL COMMENT '知识点分类ID（knowledge_nodes.id），源自 question_bank.category_id',
  ADD INDEX `idx_task_questions_category_id` (`category_id`);
