-- ============================================================
-- v151: 偏科提分模块 — 关键查询性能索引
-- 创建时间: 2026-06-06
-- ============================================================

-- 辅助存储过程：仅在索引不存在时创建
DROP PROCEDURE IF EXISTS add_idx_if_missing;
DELIMITER //
CREATE PROCEDURE add_idx_if_missing(
  IN tbl VARCHAR(64), IN idx_name VARCHAR(64), IN idx_def VARCHAR(512))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND INDEX_NAME = idx_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', tbl, ' ADD INDEX ', idx_name, ' ', idx_def);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

-- 1. precision_progress: 查某学生某学科的掌握进度（按掌握度排序取薄弱点）
CALL add_idx_if_missing('precision_progress', 'idx_student_subject_mastery',
  '(`student_id`, `subject`, `mastery_percent`)');

-- 2. precision_progress: 查某班级学生某学科的薄弱知识点（教师端 TOP N）
CALL add_idx_if_missing('precision_progress', 'idx_subject_mastery',
  '(`subject`, `mastery_percent`)');

-- 3. precision_vocabulary: 查某学生的未掌握单词（按掌握等级+下次复习时间）
CALL add_idx_if_missing('precision_vocabulary', 'idx_student_master_review',
  '(`student_id`, `master_level`, `next_review_at`)');

-- 4. wrong_questions: 查某学生未掌握的错题（班级薄弱分析用）
CALL add_idx_if_missing('wrong_questions', 'idx_student_mastered',
  '(`student_id`, `is_mastered`)');

-- 5. wrong_questions: 按错题来源批量统计
CALL add_idx_if_missing('wrong_questions', 'idx_source_type',
  '(`source_type`)');

DROP PROCEDURE IF EXISTS add_idx_if_missing;
