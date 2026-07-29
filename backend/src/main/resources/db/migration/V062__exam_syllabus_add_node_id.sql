-- ============================================================================
-- V062: exam_syllabus 增加 node_id 字段
-- 支持考纲条目精确挂载到 knowledge_nodes.id，使 syllabus_lookup 从模糊LIKE升级为精确查询
-- 幂等：用存储过程检测字段/索引是否存在
-- ============================================================================
START TRANSACTION;

-- 1. 添加 node_id 字段（幂等）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_syllabus' AND COLUMN_NAME = 'node_id');
SET @sql1 = IF(@col_exists = 0,
    'ALTER TABLE exam_syllabus ADD COLUMN node_id BIGINT DEFAULT NULL COMMENT ''关联知识点节点ID(knowledge_nodes.id)，支持精确考纲→知识点映射'' AFTER knowledge_dim',
    'SELECT "V062: node_id column already exists" AS status');
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

-- 2. 添加索引（幂等）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_syllabus' AND INDEX_NAME = 'idx_es_node_id');
SET @sql2 = IF(@idx_exists = 0,
    'ALTER TABLE exam_syllabus ADD INDEX idx_es_node_id (node_id)',
    'SELECT "V062: idx_es_node_id already exists" AS status');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

COMMIT;
