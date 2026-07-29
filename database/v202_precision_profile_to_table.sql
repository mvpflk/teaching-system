-- database/v202_precision_profile_to_table.sql
-- 迁移 precision_profile JSON 字段到独立表
-- 解决：查询性能差 + 并发覆盖 + 字段混用问题

-- 1. 创建新的 precision_profile 表
CREATE TABLE IF NOT EXISTS `precision_profile` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT(20) NOT NULL COMMENT '学生ID',
  `subject` VARCHAR(100) NOT NULL COMMENT '学科名称(如: 数学[职高], 英语[职高])',
  `diagnose_score` INT(11) DEFAULT 0 COMMENT '诊断分数',
  `estimated_score` INT(11) DEFAULT 0 COMMENT '预估高考分',
  `last_diagnose_at` DATE DEFAULT NULL COMMENT '上次诊断日期',
  `streak_weeks` INT(11) DEFAULT 0 COMMENT '连续学习周数',
  `last_online_test_score` INT(11) DEFAULT 0 COMMENT '最近小测分数',
  `last_submit_date` DATE DEFAULT NULL COMMENT '上次提交日期',
  `last_pack_question_ids` JSON DEFAULT NULL COMMENT '学习包题目ID列表',
  `last_pack_week_no` INT(11) DEFAULT NULL COMMENT '学习包周数',
  `extra_data` JSON DEFAULT NULL COMMENT '扩展数据(如英语阶段/词汇量等)',
  `version` INT(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_subject` (`student_id`, `subject`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生偏科提分画像';

-- 2. 数据迁移：从 students.precision_profile JSON 迁移到新表
-- 注意：此脚本需要在应用启动时执行，或作为一次性迁移脚本
-- 由于 MySQL 不支持直接解析 JSON，需要应用层完成迁移

-- 3. 添加数据完整性约束（可选，建议在应用层验证后执行）
-- ALTER TABLE `precision_profile`
--   ADD CONSTRAINT `fk_profile_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;

-- 4. 创建索引优化常见查询（使用项目统一 add_idx_if_missing 模式）
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

CALL add_idx_if_missing('precision_profile', 'idx_profile_diagnose_score',
  '(`diagnose_score`)');
CALL add_idx_if_missing('precision_profile', 'idx_profile_streak',
  '(`streak_weeks`)');

DROP PROCEDURE IF EXISTS add_idx_if_missing;
