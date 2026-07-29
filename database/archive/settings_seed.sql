INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('exam.max_duration', '180', '最大考试时长(分钟)'),
('exam.min_duration', '10', '最小时长(分钟)'),
('exam.max_cheat_warnings', '3', '最大切屏警告次数'),
('exam.cheat_detection_enabled', 'true', '是否启用防作弊检测'),
('homework.late_hours_limit', '48', '允许迟交小时数'),
('homework.late_penalty', '0.5', '迟交扣分比例(0-1)'),
('credit.leaderboard_enabled', 'true', '是否启用积分排行榜'),
('credit.daily_sign_reset_hour', '0', '签到重置时间(0-23点)'),
('credit.daily_sign_credits', '5', '每日签到获得积分数'),
('bbs.allow_anonymous', 'false', '是否允许匿名发帖'),
('bbs.post_credit_reward', '5', '发帖奖励积分'),
('bbs.reply_credit_reward', '2', '回复奖励积分');
