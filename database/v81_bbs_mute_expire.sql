-- v81: BBS禁言增加过期时间，支持有期限禁言
ALTER TABLE `bbs_muted_users` ADD COLUMN `expire_time` datetime DEFAULT NULL COMMENT '禁言过期时间，NULL表示永久' AFTER `reason`;
