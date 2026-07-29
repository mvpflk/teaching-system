-- ============================================================================
-- v82: 成套试卷库 exam_papers 表
-- 支持教师导入Word/Excel/TXT成套试卷，存储解析结果方便复用
-- ============================================================================
CREATE TABLE IF NOT EXISTS `exam_papers` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`           VARCHAR(200) NOT NULL COMMENT '试卷标题',
  `subject`         VARCHAR(100) DEFAULT NULL COMMENT '学科名称',
  `question_ids`    JSON         DEFAULT NULL COMMENT '题目ID数组',
  `question_count`  INT          DEFAULT 0 COMMENT '总题数',
  `type_stats`      JSON         DEFAULT NULL COMMENT '题型统计 {"SINGLE_CHOICE":15,"MULTI_CHOICE":10}',
  `score_presets`   JSON         DEFAULT NULL COMMENT '分值预设 {"SINGLE_CHOICE":2,"ESSAY":20}',
  `total_score`     DECIMAL(6,1) DEFAULT 100.0 COMMENT '总分',
  `exam_config`     JSON         DEFAULT NULL COMMENT '考试配置(时长/乱序/防作弊)',
  `duration_minutes` INT         DEFAULT 60 COMMENT '考试时长(分钟)',
  `source_file`     VARCHAR(500) DEFAULT NULL COMMENT '原始文件名',
  `content_hash`    VARCHAR(64)  DEFAULT NULL COMMENT 'MD5去重',
  `creator_id`      BIGINT       NOT NULL COMMENT '创建者用户ID',
  `status`          TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
  `use_count`       INT          DEFAULT 0 COMMENT '被使用次数',
  `last_task_id`    BIGINT       DEFAULT NULL COMMENT '最近一次生成的任务ID',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `school_id`       BIGINT       DEFAULT 1,
  PRIMARY KEY (`id`),
  INDEX `idx_creator` (`creator_id`),
  INDEX `idx_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成套试卷库';
