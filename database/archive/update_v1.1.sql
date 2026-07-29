-- ============================================
-- 职高计算机教学管理系统 - 数据库更新脚本
-- 版本: 1.1.0
-- 日期: 2026-05-03
-- 说明: 添加 students 表缺失字段，可重复执行
-- ============================================

USE teaching_system;

DELIMITER //

-- 安全添加列（列不存在时才添加）
CREATE PROCEDURE IF NOT EXISTS add_column_if_not_exists(
    IN tbl VARCHAR(128), IN col VARCHAR(128), IN col_def VARCHAR(512)
)
BEGIN
    SET @cnt = 0;
    SELECT COUNT(*) INTO @cnt
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'teaching_system'
      AND TABLE_NAME = tbl
      AND COLUMN_NAME = col;

    IF @cnt = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', col, ' ', col_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

-- 安全添加外键约束
CREATE PROCEDURE IF NOT EXISTS add_fk_if_not_exists(
    IN tbl VARCHAR(128), IN fk_name VARCHAR(128),
    IN fk_col VARCHAR(128), IN ref_tbl VARCHAR(128), IN ref_col VARCHAR(128)
)
BEGIN
    SET @cnt = 0;
    SELECT COUNT(*) INTO @cnt
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = 'teaching_system'
      AND TABLE_NAME = tbl
      AND CONSTRAINT_NAME = fk_name;

    IF @cnt = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tbl, ' ADD CONSTRAINT ', fk_name,
                          ' FOREIGN KEY (', fk_col, ') REFERENCES ', ref_tbl, '(', ref_col, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- 1. 添加班级ID字段
CALL add_column_if_not_exists('students', 'class_id',
    'BIGINT COMMENT "所属班级ID" AFTER enrollment_year');

-- 添加索引
SET @idx_exists = 0;
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'teaching_system'
  AND TABLE_NAME = 'students'
  AND INDEX_NAME = 'idx_class_id';

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE students ADD INDEX idx_class_id (class_id)',
    'SELECT "idx_class_id already exists" AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加外键
CALL add_fk_if_not_exists('students', 'fk_student_class', 'class_id', 'classes', 'id');

-- 2. 添加自定义称号字段
CALL add_column_if_not_exists('students', 'custom_title',
    'VARCHAR(20) COMMENT "自定义称号" AFTER current_streak');
CALL add_column_if_not_exists('students', 'custom_title_set_at',
    'DATETIME COMMENT "自定义称号设置时间" AFTER custom_title');

-- 3. 根据 class_students 表回填 class_id
UPDATE students s
SET s.class_id = (
    SELECT cs.class_id
    FROM class_students cs
    WHERE cs.student_id = s.id AND cs.status = 1
    LIMIT 1
)
WHERE s.class_id IS NULL;

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DROP PROCEDURE IF EXISTS add_fk_if_not_exists;

SELECT '数据库更新 v1.1 完成！' AS message;
