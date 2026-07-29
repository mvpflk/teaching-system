-- ============================================================================
-- v3: 升学考试考纲管理
-- 职高单招/对口升学，应知/应会维度
-- ============================================================================

CREATE TABLE `exam_syllabus` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id`      BIGINT       NOT NULL COMMENT '所属学科ID → dict_subject.id',
  `exam_type`       VARCHAR(30)  NOT NULL DEFAULT 'GENERAL' COMMENT '考试类型: SINGLE_RECRUIT(单招)/COUNTERPART(对口升学)/GENERAL(通用)',
  `knowledge_dim`   VARCHAR(20)  NOT NULL DEFAULT 'BOTH' COMMENT '维度: THEORY(应知)/PRACTICE(应会)/BOTH(综合)',
  `title`           VARCHAR(200) NOT NULL COMMENT '考纲标题',
  `content`         MEDIUMTEXT   NOT NULL COMMENT '考纲正文(Markdown)',
  `version`         VARCHAR(20)  DEFAULT '1.0' COMMENT '考纲版本号',
  `status`          TINYINT      DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
  `created_by`      BIGINT       DEFAULT NULL COMMENT '创建人用户ID',
  `updated_by`      BIGINT       DEFAULT NULL COMMENT '最后修改人用户ID',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_exam` (`subject_id`, `exam_type`),
  INDEX `idx_subject_id` (`subject_id`),
  INDEX `idx_status` (`status`),
  CONSTRAINT `fk_es_subject` FOREIGN KEY (`subject_id`) REFERENCES `dict_subject` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='升学考试考纲表';

SELECT 'exam_syllabus表创建完成' AS result;
