-- V056__question_bank_add_source.sql
-- 幂等：列已存在则跳过（手动执行过或从旧 V052 迁移的场景）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question_bank' AND COLUMN_NAME = 'source');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `question_bank` ADD COLUMN `source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''入库途径: MANUAL/AI/WORD_IMPORT/EXCEL_IMPORT/PAPER_IMPORT'' AFTER `knowledge_dim`',
  'SELECT ''column source already exists, skipping''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
