-- ============================================================================
-- v123: 实训向导重构 — 新增字段和模板表
-- 需要 mysql --default-character-set=utf8mb4 执行
-- 注意: v83 已被 v83_network_syllabus_nodes.sql 占用，故使用 v123
-- ============================================================================

-- 1. practice_plans 新增字段（支持向导草稿和自动评分）
ALTER TABLE practice_plans
  ADD COLUMN status VARCHAR(20) DEFAULT 'PUBLISHED' COMMENT '草稿状态: DRAFT/READY/PUBLISHED',
  ADD COLUMN steps_json LONGTEXT COMMENT '步骤列表JSON（替代description中嵌入步骤）',
  ADD COLUMN auto_grading_enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用自动评分',
  ADD COLUMN grading_strategy VARCHAR(30) DEFAULT NULL COMMENT 'AUTO_FULL/AUTO_ASSIST/MANUAL';

-- 旧数据默认 PUBLISHED（已发布或已保存的方案）
UPDATE practice_plans SET status = 'PUBLISHED' WHERE status IS NULL;

-- 2. practice_steps 新增字段（支持附件模式和模板比对）
ALTER TABLE practice_steps
  ADD COLUMN attachment_mode VARCHAR(20) DEFAULT 'REFERENCE' COMMENT 'REFERENCE=参考范例, TEMPLATE=任务模板',
  ADD COLUMN reference_attachments JSON COMMENT '教师上传的参考附件列表 [{name,url,size}]',
  ADD COLUMN template_file VARCHAR(500) COMMENT '任务模板文件路径（用于自动比对）',
  ADD COLUMN template_checksum VARCHAR(64) COMMENT '模板文件SHA-256（检测是否被修改）';

-- 3. practice_templates 预置模板库表
CREATE TABLE IF NOT EXISTS practice_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT COMMENT '简要描述',
  subject VARCHAR(50) COMMENT '学科',
  category VARCHAR(50) COMMENT '分类标签: word/excel/ppt/operation/design',
  steps_json LONGTEXT COMMENT '步骤模板JSON',
  rubrics_json TEXT COMMENT '评分标准JSON',
  scoring_model VARCHAR(30) DEFAULT 'DUAL_DIMENSION',
  source VARCHAR(30) DEFAULT 'SYSTEM' COMMENT 'SYSTEM/SHARED/IMPORTED',
  source_plan_id BIGINT COMMENT '来源方案ID（SHARED时关联）',
  use_count INT DEFAULT 0 COMMENT '被使用次数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_subject (subject),
  INDEX idx_source (source),
  INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实训预置模板库';
