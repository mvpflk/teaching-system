-- database/v200_task_retake.sql
-- 任务达标得分率 + 强制重测
-- 设计文档: docs/superpowers/specs/2026-07-03-test-pass-rate-retake-design.md

-- 1. tasks 表新增达标配置列
ALTER TABLE `tasks`
  ADD COLUMN `pass_rate` TINYINT(3) UNSIGNED NOT NULL DEFAULT 0
    COMMENT '达标得分率(%), 0=不启用达标模式, 50-100=启用',
  ADD COLUMN `max_attempts` TINYINT(3) UNSIGNED NOT NULL DEFAULT 1
    COMMENT '最大尝试次数(含首次), 1=不重测, 2/3=启用重测',
  ADD COLUMN `retake_deadline_hours` SMALLINT(5) UNSIGNED DEFAULT NULL
    COMMENT '重测截止时间(小时), 从首次提交完成时开始计算, NULL=不限';

-- 2. task_submissions 表新增列 + 索引
ALTER TABLE `task_submissions`
  ADD COLUMN `attempt_number` INT NOT NULL DEFAULT 1
    COMMENT '第几次作答（1=首次，2=重测1，3=重测2）',
  ADD COLUMN `is_official` TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '是否为正式成绩（首次=1，重测=0）',
  ADD INDEX `idx_task_student_attempt` (`task_id`, `student_id`, `attempt_number`);

-- 防重唯一索引（防止并发创建重测）
ALTER TABLE `task_submissions`
  ADD UNIQUE INDEX `uk_task_student_attempt` (`task_id`, `student_id`, `attempt_number`);
