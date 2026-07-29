-- Original file: v150_knowledge_learning_resources.sql (DDL extracted from MIXED file)
-- ============================================================
-- v150: knowledge_nodes 新增 learning_resources 列（JSON类型，幂等安全）
-- DML 部分（知识点学习资源填充）保留在 database/v150_knowledge_learning_resources.sql

SET NAMES utf8mb4;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_nodes'
  AND COLUMN_NAME = 'learning_resources');
SET @sql_cmd = IF(@col_exists = 0,
  'ALTER TABLE knowledge_nodes ADD COLUMN learning_resources JSON DEFAULT NULL COMMENT ''学习资源:{videoUrl,exampleIds:[],practiceIds:[]}'' AFTER deprecation_note',
  'SELECT ''SKIP'' AS msg');
PREPARE stmt FROM @sql_cmd; EXECUTE stmt; DEALLOCATE PREPARE stmt;
