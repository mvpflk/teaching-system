-- ============================================================================
-- v6: 巡视范围扩展 — 课堂巡课/德育巡视/教研活动/家长反馈汇总
-- 扩展巡视员系统覆盖课堂教学、德育、教研、家校四大维度
-- ============================================================================

DROP TABLE IF EXISTS `parent_feedback_summaries`;
DROP TABLE IF EXISTS `teaching_research_activities`;
DROP TABLE IF EXISTS `moral_inspections`;
DROP TABLE IF EXISTS `classroom_patrols`;

-- ----------------------------------------------------------------------------
-- classroom_patrols: 课堂巡课记录
-- ----------------------------------------------------------------------------
CREATE TABLE `classroom_patrols` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_id`          BIGINT      NOT NULL COMMENT '班级ID',
  `teacher_id`        BIGINT      NOT NULL COMMENT '授课教师ID',
  `inspector_id`      BIGINT      NOT NULL COMMENT '巡视员用户ID',
  `subject`           VARCHAR(50)  DEFAULT NULL COMMENT '科目',
  `patrol_date`       DATE        NOT NULL COMMENT '巡课日期',
  `period`            VARCHAR(20)  DEFAULT NULL COMMENT '节次(1-8)',
  `discipline_score`  TINYINT      DEFAULT NULL COMMENT '课堂纪律评分(1-5)',
  `teaching_score`    TINYINT      DEFAULT NULL COMMENT '教学规范评分(1-5)',
  `interaction_score` TINYINT      DEFAULT NULL COMMENT '师生互动评分(1-5)',
  `note`              TEXT         DEFAULT NULL COMMENT '巡课备注',
  `created_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_class_date`    (`class_id`, `patrol_date`),
  INDEX `idx_teacher_date`  (`teacher_id`, `patrol_date`),
  INDEX `idx_inspector`     (`inspector_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课堂巡课记录表';

-- ----------------------------------------------------------------------------
-- moral_inspections: 德育巡视记录
-- ----------------------------------------------------------------------------
CREATE TABLE `moral_inspections` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_id`        BIGINT      NOT NULL COMMENT '班级ID',
  `inspector_id`    BIGINT      NOT NULL COMMENT '巡视员用户ID',
  `inspection_date` DATE        NOT NULL COMMENT '检查日期',
  `category`        VARCHAR(50) NOT NULL COMMENT '类别: HYGIENE(卫生)/APPEARANCE(仪容仪表)/BREAK_DISCIPLINE(课间纪律)/MORNING_READING(早读)/EYE_EXERCISE(眼保健操)',
  `score`           TINYINT      DEFAULT NULL COMMENT '评分(1-5)',
  `description`     TEXT         DEFAULT NULL COMMENT '描述',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_class_date`    (`class_id`, `inspection_date`),
  INDEX `idx_category`     (`category`),
  INDEX `idx_inspector`    (`inspector_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='德育巡视记录表';

-- ----------------------------------------------------------------------------
-- teaching_research_activities: 教研活动记录
-- ----------------------------------------------------------------------------
CREATE TABLE `teaching_research_activities` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_group_id` BIGINT      NOT NULL COMMENT '教研组ID',
  `activity_type`     VARCHAR(50) NOT NULL COMMENT '类型: GROUP_LESSON_PREP(集体备课)/PUBLIC_CLASS(公开课)/TEACHING_COMPETITION(教学竞赛)/SEMINAR(研讨会)',
  `title`             VARCHAR(200) NOT NULL COMMENT '活动标题',
  `activity_date`     DATE        NOT NULL COMMENT '活动日期',
  `participant_count` INT         DEFAULT 0 COMMENT '参与人数',
  `summary`           TEXT        DEFAULT NULL COMMENT '活动纪要',
  `recorded_by`       BIGINT      DEFAULT NULL COMMENT '记录人(巡视员)',
  `created_at`        DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_group_date`    (`teaching_group_id`, `activity_date`),
  INDEX `idx_activity_type` (`activity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教研活动记录表';

-- ----------------------------------------------------------------------------
-- parent_feedback_summaries: 家长反馈汇总
-- ----------------------------------------------------------------------------
CREATE TABLE `parent_feedback_summaries` (
  `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `class_id`         BIGINT      NOT NULL COMMENT '班级ID',
  `period`           VARCHAR(30) NOT NULL COMMENT '周期: 如2026-W22或2026-M05',
  `total_feedback`   INT         DEFAULT 0 COMMENT '反馈总数',
  `positive_count`   INT         DEFAULT 0 COMMENT '正面反馈数',
  `negative_count`   INT         DEFAULT 0 COMMENT '负面反馈数',
  `categories_json`  TEXT        DEFAULT NULL COMMENT '分类统计JSON',
  `summary_text`     TEXT        DEFAULT NULL COMMENT '摘要',
  `created_at`       DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_class_period` (`class_id`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家长反馈汇总表';

-- ============================================================================
-- 完成标记
-- ============================================================================
SELECT 'v6_inspector_expansion 巡视范围扩展4张表创建完成' AS result;
