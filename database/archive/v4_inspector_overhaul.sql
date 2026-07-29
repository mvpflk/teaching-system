-- ============================================================================
-- v4: 巡视员系统全面升级 — 巡视记录/问题台账/整改通知书/巡视报告/沟通记录
-- 配合 INSPECTOR 角色权限丰富化
-- ============================================================================

DROP TABLE IF EXISTS `inspection_issue_comments`;
DROP TABLE IF EXISTS `inspection_reports`;
DROP TABLE IF EXISTS `rectification_notices`;
DROP TABLE IF EXISTS `inspection_issues`;
DROP TABLE IF EXISTS `inspection_records`;

-- ----------------------------------------------------------------------------
-- inspection_records: 巡视记录（一次巡视活动的完整记录）
-- ----------------------------------------------------------------------------
CREATE TABLE `inspection_records` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `inspector_id`      BIGINT       NOT NULL COMMENT '巡视员用户ID',
  `record_type`       VARCHAR(30)  NOT NULL DEFAULT 'CASUAL' COMMENT '巡视类型: CASUAL(日常巡视)/CLASSROOM(课堂巡视)/MORAL(德育巡视)/SPECIAL(专项巡视)',
  `title`             VARCHAR(200) NOT NULL COMMENT '巡视标题',
  `description`       TEXT         DEFAULT NULL COMMENT '巡视描述',
  `location`          VARCHAR(200) DEFAULT NULL COMMENT '巡视地点',
  `target_class_id`   BIGINT       DEFAULT NULL COMMENT '关联班级ID',
  `target_teacher_id` BIGINT       DEFAULT NULL COMMENT '关联教师ID',
  `severity`          VARCHAR(20)  NOT NULL DEFAULT 'INFO' COMMENT '严重程度: INFO/WARNING/CRITICAL',
  `status`            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT(草稿)/SUBMITTED(已提交)/ARCHIVED(已归档)',
  `attachment_urls`   TEXT         DEFAULT NULL COMMENT '附件URL列表(JSON数组)',
  `record_date`       DATE         NOT NULL COMMENT '巡视日期',
  `created_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_inspector`      (`inspector_id`),
  INDEX `idx_status`         (`status`),
  INDEX `idx_record_date`    (`record_date`),
  INDEX `idx_target_class`   (`target_class_id`),
  INDEX `idx_target_teacher` (`target_teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡视记录表';

-- ----------------------------------------------------------------------------
-- inspection_issues: 问题台账（核心表，跟踪每个问题的全生命周期）
-- ----------------------------------------------------------------------------
CREATE TABLE `inspection_issues` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_id`         BIGINT       DEFAULT NULL COMMENT '关联巡视记录ID',
  `title`             VARCHAR(200) NOT NULL COMMENT '问题标题',
  `description`       TEXT         DEFAULT NULL COMMENT '问题描述',
  `category`          VARCHAR(50)  NOT NULL COMMENT '问题分类: TEACHING_QUALITY(教学质量)/CLASSROOM_DISCIPLINE(课堂纪律)/HOMEWORK_PROCRASTINATION(作业拖拉)/ATTENDANCE(出勤)/MORAL_EDUCATION(德育)/EXAM_IRREGULARITY(考试违纪)/OTHER(其他)',
  `severity`          VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM' COMMENT '严重程度: LOW/MEDIUM/HIGH/CRITICAL',
  `status`            VARCHAR(30)  NOT NULL DEFAULT 'OPEN' COMMENT '状态流转: OPEN(待处理)/ASSIGNED(已指派)/IN_PROGRESS(处理中)/RESOLVED(已解决)/VERIFIED(已验收)/REJECTED(已驳回)',
  `assigned_to`       BIGINT       DEFAULT NULL COMMENT '指派给教师用户ID',
  `assigned_class_id` BIGINT       DEFAULT NULL COMMENT '关联班级ID',
  `related_task_id`   BIGINT       DEFAULT NULL COMMENT '关联任务ID',
  `deadline`          DATE         DEFAULT NULL COMMENT '整改截止日期',
  `resolved_at`       DATETIME     DEFAULT NULL COMMENT '解决时间',
  `resolved_by`       BIGINT       DEFAULT NULL COMMENT '解决人用户ID',
  `resolve_comment`   TEXT         DEFAULT NULL COMMENT '解决说明',
  `verified_at`       DATETIME     DEFAULT NULL COMMENT '验收时间',
  `verified_by`       BIGINT       DEFAULT NULL COMMENT '验收人(巡视员)用户ID',
  `verify_comment`    TEXT         DEFAULT NULL COMMENT '验收意见',
  `attachment_urls`   TEXT         DEFAULT NULL COMMENT '附件URL列表(JSON数组)',
  `created_by`        BIGINT       NOT NULL COMMENT '创建人用户ID',
  `created_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_status`         (`status`),
  INDEX `idx_category`       (`category`),
  INDEX `idx_severity`       (`severity`),
  INDEX `idx_assigned_to`    (`assigned_to`),
  INDEX `idx_assigned_class` (`assigned_class_id`),
  INDEX `idx_created_by`     (`created_by`),
  INDEX `idx_record`         (`record_id`),
  INDEX `idx_deadline`       (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问题台账表（巡视问题全生命周期跟踪）';

-- ----------------------------------------------------------------------------
-- rectification_notices: 整改通知书（向教师发出的正式整改要求）
-- ----------------------------------------------------------------------------
CREATE TABLE `rectification_notices` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `issue_id`         BIGINT       NOT NULL COMMENT '关联问题ID',
  `title`            VARCHAR(200) NOT NULL COMMENT '通知书标题',
  `content`          TEXT         NOT NULL COMMENT '通知书正文',
  `sender_id`        BIGINT       NOT NULL COMMENT '发送人(巡视员)用户ID',
  `recipient_id`     BIGINT       NOT NULL COMMENT '接收人(教师)用户ID',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'SENT' COMMENT '状态: DRAFT(草稿)/SENT(已发送)/ACKNOWLEDGED(已确认)/COMPLIED(已完成)',
  `sent_at`          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `acknowledged_at`  DATETIME     DEFAULT NULL COMMENT '确认时间',
  `complied_at`      DATETIME     DEFAULT NULL COMMENT '完成时间',
  `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_issue`     (`issue_id`),
  INDEX `idx_recipient` (`recipient_id`),
  INDEX `idx_sender`    (`sender_id`),
  INDEX `idx_status`    (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='整改通知书表';

-- ----------------------------------------------------------------------------
-- inspection_reports: 巡视报告（自动/手动生成的报告）
-- ----------------------------------------------------------------------------
CREATE TABLE `inspection_reports` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`           VARCHAR(200) NOT NULL COMMENT '报告标题',
  `report_type`     VARCHAR(30)  NOT NULL DEFAULT 'WEEKLY' COMMENT '报告类型: WEEKLY(周报)/MONTHLY(月报)/SEMESTER(学期报)/AD_HOC(专项)',
  `period_start`    DATE         DEFAULT NULL COMMENT '统计起始日期',
  `period_end`      DATE         DEFAULT NULL COMMENT '统计结束日期',
  `content`         MEDIUMTEXT   DEFAULT NULL COMMENT '报告正文(Markdown)',
  `summary_json`    TEXT         DEFAULT NULL COMMENT '统计摘要(JSON)',
  `issue_count`     INT          DEFAULT 0 COMMENT '问题总数',
  `resolved_count`  INT          DEFAULT 0 COMMENT '已解决数',
  `notice_count`    INT          DEFAULT 0 COMMENT '整改通知数',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT(草稿)/GENERATED(生成)/PUBLISHED(已发布)',
  `generated_by`    BIGINT       DEFAULT 0 COMMENT '生成人: 0=AI自动, 其他=用户ID',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_report_type` (`report_type`),
  INDEX `idx_period`      (`period_start`, `period_end`),
  INDEX `idx_status`      (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡视报告表';

-- ----------------------------------------------------------------------------
-- inspection_issue_comments: 问题沟通记录（整改过程的留言/系统日志）
-- ----------------------------------------------------------------------------
CREATE TABLE `inspection_issue_comments` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `issue_id`   BIGINT   NOT NULL COMMENT '关联问题ID',
  `user_id`    BIGINT   NOT NULL COMMENT '评论人用户ID',
  `content`    TEXT     NOT NULL COMMENT '评论内容',
  `is_system`  TINYINT  DEFAULT 0 COMMENT '是否系统自动评论: 0=用户, 1=系统',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_issue` (`issue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问题沟通记录表';

-- ============================================================================
-- 完成标记
-- ============================================================================
SELECT 'v4_inspector_overhaul 巡视员系统5张表创建完成' AS result;
