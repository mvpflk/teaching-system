-- ============================================================
-- v162: P0-1 试卷锁定功能 — 标准化试卷标记+锁定+平行卷关联
-- 创建时间: 2026-06-20
-- 功能: 1) exam_papers 新增研究标记字段
--       2) research_baseline 学生基线快照表
-- ============================================================

-- 1. exam_papers 新增标准化试卷字段
ALTER TABLE `exam_papers`
  ADD COLUMN `is_standardized` TINYINT DEFAULT 0 COMMENT '1=标准化研究试卷(锁定不可编辑)' AFTER `school_id`,
  ADD COLUMN `paper_role` VARCHAR(20) DEFAULT NULL COMMENT 'PRETEST/POSTTEST/MIDTEST/COMMON' AFTER `is_standardized`,
  ADD COLUMN `parallel_paper_id` BIGINT DEFAULT NULL COMMENT '平行卷ID→exam_papers.id（前测/后测配对）' AFTER `paper_role`,
  ADD COLUMN `locked_at` DATETIME DEFAULT NULL COMMENT '锁定时间' AFTER `parallel_paper_id`,
  ADD COLUMN `locked_by` BIGINT DEFAULT NULL COMMENT '锁定操作者用户ID' AFTER `locked_at`;

-- 2. 为锁定功能添加索引
CREATE INDEX `idx_standardized` ON `exam_papers` (`is_standardized`);
CREATE INDEX `idx_paper_role` ON `exam_papers` (`paper_role`);

-- 3. 学生基线快照表（P0-2）
CREATE TABLE IF NOT EXISTS `research_baseline` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`       BIGINT       NOT NULL COMMENT '学生ID → students.id',
  `student_name`     VARCHAR(50)  DEFAULT NULL COMMENT '学生姓名（冗余，便于导出）',
  `class_id`         BIGINT       DEFAULT NULL COMMENT '班级ID → classes.id',
  `class_name`       VARCHAR(50)  DEFAULT NULL COMMENT '班级名称（冗余）',
  `research_group`   VARCHAR(20)  DEFAULT NULL COMMENT 'EXPERIMENT/CONTROL',
  `subject`          VARCHAR(50)  NOT NULL COMMENT '学科名',
  `node_id`          BIGINT       NOT NULL COMMENT 'knowledge_nodes.id',
  `node_name`        VARCHAR(200) DEFAULT NULL COMMENT '知识点名称（冗余）',
  `node_level`       INT          DEFAULT 4 COMMENT '知识点层级',
  `mastery_percent`  DECIMAL(5,2) DEFAULT 0 COMMENT '基线掌握度',
  `total_attempts`   INT          DEFAULT 0 COMMENT '基线答题次数',
  `total_correct`    INT          DEFAULT 0 COMMENT '基线正确次数',
  `status`           VARCHAR(20)  DEFAULT 'weak' COMMENT '基线状态',
  `snapshot_time`    DATETIME     NOT NULL COMMENT '快照时间',
  `snapshot_label`   VARCHAR(50)  DEFAULT 'PRETEST' COMMENT '快照标签: PRETEST/MIDTEST/POSTTEST',
  `school_id`        BIGINT       DEFAULT 1,
  `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_student_subject` (`student_id`, `subject`),
  INDEX `idx_snapshot_time` (`snapshot_time`),
  INDEX `idx_class_research` (`class_id`, `research_group`),
  INDEX `idx_node` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课题研究-学生基线快照';

-- 4. 已有试卷标记为普通类型（非标准化）
UPDATE `exam_papers` SET `paper_role` = 'COMMON' WHERE `paper_role` IS NULL;
