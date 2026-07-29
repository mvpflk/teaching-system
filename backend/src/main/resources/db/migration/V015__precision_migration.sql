-- Original file: v92_precision_migration.sql (DDL extracted from MIXED file)
-- ============================================================

-- ============================================================
-- v92: 偏科精准提分平台 — 数据库迁移
-- 创建时间: 2026-05-31
-- 依赖: knowledge_nodes 表已存在 (id 10=数学[职高], id 12=英语[职高])
-- 注意: 生产环境执行前请先备份数据库
-- ============================================================

-- 1. students 表新增偏科画像字段（TEXT类型兼容所有MySQL版本）
-- 应用层通过 Jackson 序列化/反序列化 JSON
-- 先检查列是否存在，不存在则添加（存储过程兜底避免重复执行报错）
DROP PROCEDURE IF EXISTS add_col_if_missing;
DELIMITER //
CREATE PROCEDURE add_col_if_missing(
  IN tbl VARCHAR(64), IN col VARCHAR(64), IN col_def VARCHAR(256))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', col, ' ', col_def);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CREATE TABLE IF NOT EXISTS precision_vocabulary_seeds (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  word            VARCHAR(100) NOT NULL COMMENT '单词',
  meaning         VARCHAR(255) NOT NULL COMMENT '中文释义',
  phonetic        VARCHAR(100) DEFAULT NULL COMMENT '音标',
  example         VARCHAR(500) DEFAULT NULL COMMENT '例句',
  frequency_rank  INT DEFAULT 999 COMMENT '考纲频率排名(越小越高频)',
  level           TINYINT DEFAULT 1 COMMENT '难度等级1-5',
  create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='偏科英语-词汇种子词库';

-- 3. 创建英语词汇掌握表
CREATE TABLE IF NOT EXISTS precision_vocabulary (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id      BIGINT NOT NULL COMMENT '学生ID',
  word            VARCHAR(100) NOT NULL COMMENT '单词',
  master_level    TINYINT DEFAULT 0 COMMENT '0未学 1见过 2认识 3会拼 4会用',
  last_review_at  DATETIME DEFAULT NULL COMMENT '最近复习时间',
  next_review_at  DATETIME DEFAULT NULL COMMENT '遗忘曲线下次复习时间',
  correct_count   INT DEFAULT 0,
  wrong_count     INT DEFAULT 0,
  create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student_word (student_id, word),
  KEY idx_next_review (next_review_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='偏科英语-学生词汇掌握';

-- 3. 创建知识点掌握进度表（数学+英语共用）
CREATE TABLE IF NOT EXISTS precision_progress (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id          BIGINT NOT NULL COMMENT '学生ID',
  subject             VARCHAR(50) NOT NULL COMMENT '学科名，如数学[职高]',
  node_id             BIGINT NOT NULL COMMENT 'knowledge_nodes.id',
  mastery_percent     DECIMAL(5,2) DEFAULT 0 COMMENT '掌握度 0.00-100.00',
  total_attempts      INT DEFAULT 0 COMMENT '总答题次数',
  total_correct       INT DEFAULT 0 COMMENT '正确次数',
  consecutive_correct INT DEFAULT 0 COMMENT '连续正确次数',
  step_progress       TEXT DEFAULT NULL COMMENT '解答题步骤进度JSON',
  last_practice_at    DATETIME DEFAULT NULL,
  next_review_at      DATETIME DEFAULT NULL COMMENT '遗忘曲线下次复习时间',
  status              VARCHAR(20) DEFAULT 'weak' COMMENT 'weak/learning/mastered',
  create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student_node (student_id, node_id),
  KEY idx_next_review (next_review_at),
  KEY idx_subject_student (subject, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='偏科提分-知识点掌握进度';

-- 4. student_groups 表扩展（支持学科偏科分组）— 使用存储过程安全添加
CALL add_col_if_missing('student_groups', 'subject_id',
  "BIGINT DEFAULT NULL COMMENT '学科ID → dict_subject.id，NULL=通用分组'");
CALL add_col_if_missing('student_groups', 'group_type',
  "VARCHAR(20) DEFAULT 'GENERAL' COMMENT '分组类型: GENERAL/REMEDIAL/PRECISION_WARNING'");

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- 5. 系统配置项
INSERT IGNORE INTO system_settings (setting_key, setting_value, category, description) VALUES
('feature.remedial_enabled',     'true',   'feature',  '偏科提分模块总开关'),
('feature.vant_mobile_enabled',  'true',   'feature',  'Vant 4 移动组件库'),
('remedial.daily_math_min',      '10',     'remedial', '数学每日建议时长(分钟)'),
('remedial.daily_english_min',   '15',     'remedial', '英语每日建议时长(分钟)'),
('remedial.weekly_new_words',    '20',     'remedial', '英语每周新词量'),
('remedial.pass_mastery',        '80',     'remedial', '掌握达标正确率%'),
('remedial.streak_warn_weeks',   '2',      'remedial', '连续未达标预警周数'),
('remedial.auto_group_threshold','50',     'remedial', '诊断分<该值自动入偏科组'),
('remedial.math_start_week',     '6',      'remedial', '数学开始推送学习包的周数'),
('remedial.test_unlock_days',    '4',      'remedial', '诊断后N天才可参加线上小测'),
('ai.remedial_daily_limit',      '20',     'remedial', 'AI补强每日限次');
