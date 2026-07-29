-- v18: credit_transactions 新增 biz_key 防重业务键
ALTER TABLE `credit_transactions`
  ADD COLUMN `biz_key` VARCHAR(100) DEFAULT NULL COMMENT '业务唯一键(如 TASK_GRADED:1:5)' AFTER `description`;

ALTER TABLE `credit_transactions`
  ADD UNIQUE INDEX `uk_biz_key` (`biz_key`);
