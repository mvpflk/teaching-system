-- ============================================================================
-- v2: 知识库节点 + AI产出表
-- 替换旧的 question_categories 和 ai_generated_content
-- ============================================================================

DROP TABLE IF EXISTS `ai_generated_content`;
DROP TABLE IF EXISTS `question_categories`;

-- ----------------------------------------------------------------------------
-- knowledge_nodes: 知识库节点（学科→章节→任务→知识点 四级树）
-- ----------------------------------------------------------------------------
CREATE TABLE `knowledge_nodes` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`   BIGINT       DEFAULT NULL COMMENT '父节点ID → knowledge_nodes.id',
  `subject_id`  BIGINT       DEFAULT NULL COMMENT '所属学科ID → dict_subject.id',
  `level`       INT          NOT NULL DEFAULT 2 COMMENT '层级: 1=学科, 2=章节, 3=任务, 4=知识点',
  `name`        VARCHAR(200) NOT NULL COMMENT '节点名称',
  `content`     MEDIUMTEXT   DEFAULT NULL COMMENT 'Markdown知识库内容',
  `sort_order`  INT          DEFAULT 0 COMMENT '排序',
  `version`     INT          DEFAULT 1 COMMENT '乐观锁版本号',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_parent_name` (`parent_id`, `name`),
  INDEX `idx_parent_id`  (`parent_id`),
  INDEX `idx_subject_id` (`subject_id`),
  INDEX `idx_level`      (`level`),
  CONSTRAINT `fk_kn_parent` FOREIGN KEY (`parent_id`) REFERENCES `knowledge_nodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库节点表';

-- ----------------------------------------------------------------------------
-- ai_outputs: AI教学产出记录
-- ----------------------------------------------------------------------------
CREATE TABLE `ai_outputs` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `node_id`     BIGINT       NOT NULL COMMENT '关联知识节点ID → knowledge_nodes.id',
  `teacher_id`  BIGINT       NOT NULL COMMENT '教师用户ID',
  `output_type` VARCHAR(30)  NOT NULL COMMENT '产出类型（如 lesson_plan/quiz/slide/summary 等）',
  `content`     LONGTEXT     DEFAULT NULL COMMENT 'AI生成的完整内容',
  `is_latest`   TINYINT(1)   DEFAULT 1 COMMENT '是否最新版本（1=最新, 0=历史）',
  `version_seq` INT          DEFAULT 1 COMMENT '版本序号（1=最新，同node+type内递增，>5自动清理）',
  `status`      TINYINT      DEFAULT 0 COMMENT '状态: 0=草稿, 1=已发布, 2=已归档',
  `tokens_used` INT          DEFAULT 0 COMMENT '消耗token数',
  `latency_ms`  INT          DEFAULT 0 COMMENT '响应延迟（毫秒）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_node_type_latest` (`node_id`, `output_type`, `is_latest`),
  INDEX `idx_teacher_id`       (`teacher_id`),
  CONSTRAINT `fk_ao_node` FOREIGN KEY (`node_id`) REFERENCES `knowledge_nodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI教学产出表';

-- ----------------------------------------------------------------------------
-- 种子数据: 从 dict_subject 表拉取 level=1 学科节点
-- ----------------------------------------------------------------------------
INSERT INTO `knowledge_nodes` (`parent_id`, `subject_id`, `level`, `name`, `sort_order`)
SELECT NULL, `id`, 1, `subject_name`, `sort_order`
FROM `dict_subject`
WHERE `status` = 1;

-- ----------------------------------------------------------------------------
-- 清理旧引用: question_bank.category_id 原本指向已删除的 question_categories 表
-- ----------------------------------------------------------------------------
UPDATE `question_bank` SET `category_id` = NULL;

-- ----------------------------------------------------------------------------
-- 完成标记
-- ----------------------------------------------------------------------------
-- 唯一索引：防止同一父节点下出现同名子节点（仅脚本创建时生效，已存在的表需单独执行 ALTER TABLE）
ALTER TABLE `knowledge_nodes` ADD UNIQUE INDEX IF NOT EXISTS `uk_parent_name` (`parent_id`, `name`);

SELECT 'knowledge_nodes和ai_outputs表创建完成' AS result;
