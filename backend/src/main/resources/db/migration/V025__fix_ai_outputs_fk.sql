-- v147: 修复 ai_outputs.node_id 外键约束阻止诊断/巩固材料写入
-- DIAGNOSIS 和 CONSOLIDATION_MATERIAL 复用 node_id 存 taskId（非知识节点ID）
-- FK 约束导致 INSERT 失败：Cannot add or update a child row: a foreign key constraint fails
ALTER TABLE ai_outputs DROP FOREIGN KEY IF EXISTS fk_ao_node;
-- 注意：init.sql 中也需同步移除此约束，已更新
