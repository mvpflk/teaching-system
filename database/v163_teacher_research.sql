-- ============================================================
-- v163: 教师教研活动表（修复教师首页500错误）
-- 前端调用 /teacher/research/* 端点但后端缺少对应Controller
-- ============================================================

CREATE TABLE IF NOT EXISTS `teaching_research_activities` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teaching_group_id` BIGINT      NOT NULL COMMENT '教研组ID',
  `activity_type`     VARCHAR(50) NOT NULL COMMENT '类型: GROUP_LESSON_PREP/PUBLIC_CLASS/TEACHING_COMPETITION/SEMINAR',
  `title`             VARCHAR(200) NOT NULL COMMENT '活动标题',
  `activity_date`     DATE        NOT NULL COMMENT '活动日期',
  `participant_count` INT         DEFAULT 0 COMMENT '参与人数',
  `summary`           TEXT        DEFAULT NULL COMMENT '活动纪要',
  `recorded_by`       BIGINT      DEFAULT NULL COMMENT '记录人用户ID',
  `school_id`         BIGINT      DEFAULT 1,
  `created_at`        DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_group_date`    (`teaching_group_id`, `activity_date`),
  INDEX `idx_activity_type` (`activity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教研活动记录表';
