-- ============================================================================
-- v5: 巡视员预警系统 — 预警规则配置 + 预警日志
-- 自动检测教学异常指标并通知巡视员/教师
-- ============================================================================

DROP TABLE IF EXISTS `inspection_alert_logs`;
DROP TABLE IF EXISTS `inspection_alert_rules`;

-- ----------------------------------------------------------------------------
-- inspection_alert_rules: 预警规则配置
-- ----------------------------------------------------------------------------
CREATE TABLE `inspection_alert_rules` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_name`         VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_type`         VARCHAR(50) NOT NULL COMMENT '规则类型: SCORE_AVG_DROP/SUBMIT_RATE_LOW/PASS_RATE_LOW/CREDIT_ANOMALY/PEER_STDDEV_HIGH/TEACHER_GRADING_BACKLOG/TEACHER_INACTIVE',
  `target_type`       VARCHAR(30) NOT NULL DEFAULT 'CLASS' COMMENT '监测对象: CLASS/TEACHER/GLOBAL',
  `threshold`         DECIMAL(10,2) NOT NULL COMMENT '阈值',
  `comparison`        VARCHAR(20) NOT NULL DEFAULT 'LT' COMMENT '比较方式: LT(<)/GT(>)',
  `time_window`       VARCHAR(20) NOT NULL DEFAULT 'CURRENT_WEEK' COMMENT '时间窗口: CURRENT_WEEK/LAST_WEEK/CURRENT_MONTH/LAST_MONTH',
  `enabled`           TINYINT     DEFAULT 1 COMMENT '是否启用',
  `notify_inspector`  TINYINT     DEFAULT 1 COMMENT '是否通知巡视员',
  `notify_teacher`    TINYINT     DEFAULT 1 COMMENT '是否通知相关教师',
  `created_at`        DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_enabled`   (`enabled`),
  INDEX `idx_rule_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警规则配置表';

-- ----------------------------------------------------------------------------
-- 插入默认预警规则（7条）
-- ----------------------------------------------------------------------------
INSERT INTO `inspection_alert_rules` (`rule_name`, `rule_type`, `target_type`, `threshold`, `comparison`, `time_window`, `enabled`, `notify_inspector`, `notify_teacher`) VALUES
('班级均分骤降',       'SCORE_AVG_DROP',         'CLASS',   10,    'GT', 'CURRENT_WEEK', 1, 1, 1),
('作业提交率过低',     'SUBMIT_RATE_LOW',        'CLASS',   50,    'LT', 'CURRENT_WEEK', 1, 1, 1),
('考试及格率过低',     'PASS_RATE_LOW',          'CLASS',   40,    'LT', 'CURRENT_WEEK', 1, 1, 1),
('积分异常',           'CREDIT_ANOMALY',         'CLASS',   500,   'GT', 'CURRENT_WEEK', 1, 1, 1),
('互评离散度过高',     'PEER_STDDEV_HIGH',       'CLASS',   20,    'GT', 'CURRENT_WEEK', 1, 1, 1),
('教师批改积压',       'TEACHER_GRADING_BACKLOG', 'TEACHER', 30,   'GT', 'CURRENT_WEEK', 1, 1, 1),
('教师不活跃',         'TEACHER_INACTIVE',       'TEACHER', 7,     'GT', 'CURRENT_WEEK', 1, 1, 1);

-- ----------------------------------------------------------------------------
-- inspection_alert_logs: 预警日志
-- ----------------------------------------------------------------------------
CREATE TABLE `inspection_alert_logs` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id`           BIGINT       NOT NULL COMMENT '关联预警规则ID',
  `rule_name`         VARCHAR(100) NOT NULL COMMENT '规则名称(冗余)',
  `alert_message`     TEXT         NOT NULL COMMENT '预警消息',
  `target_class_id`   BIGINT       DEFAULT NULL COMMENT '关联班级',
  `target_teacher_id` BIGINT       DEFAULT NULL COMMENT '关联教师',
  `metric_value`      DECIMAL(10,2) DEFAULT NULL COMMENT '触发时的指标值',
  `threshold`         DECIMAL(10,2) DEFAULT NULL COMMENT '触发阈值',
  `is_read`           TINYINT      DEFAULT 0 COMMENT '是否已读',
  `trigged_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  `created_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_rule`     (`rule_id`),
  INDEX `idx_read`     (`is_read`),
  INDEX `idx_trigged`  (`trigged_at`),
  INDEX `idx_class`    (`target_class_id`),
  INDEX `idx_teacher`  (`target_teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警日志表';

-- ============================================================================
-- 完成标记
-- ============================================================================
SELECT 'v5_inspector_alerts 预警系统2张表创建完成' AS result;
