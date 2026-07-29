-- v101: 修复 system_settings 描述字段的 latin1 双重编码
-- 所有 UPDATE 必须在 utf8mb4 连接下执行！
-- 执行方式：docker exec teaching-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 teaching_system < v101_fix_encoding.sql

UPDATE system_settings SET description = 'Vant 4 移动组件库' WHERE setting_key = 'feature.vant_mobile_enabled';
UPDATE system_settings SET description = '偏科提分模块总开关' WHERE setting_key = 'feature.remedial_enabled';
UPDATE system_settings SET description = 'BBS论坛功能开关' WHERE setting_key = 'feature.bbs_enabled';
UPDATE system_settings SET description = '家长端功能开关' WHERE setting_key = 'feature.parent_enabled';
UPDATE system_settings SET description = '巡视面板功能开关' WHERE setting_key = 'feature.inspector_enabled';
UPDATE system_settings SET description = '家校消息功能开关' WHERE setting_key = 'feature.message_enabled';
UPDATE system_settings SET description = '智慧大屏/课堂互动功能开关' WHERE setting_key = 'feature.classroom_enabled';
UPDATE system_settings SET description = '打字练习/竞赛功能开关' WHERE setting_key = 'feature.typing_enabled';
UPDATE system_settings SET description = '实训任务功能开关' WHERE setting_key = 'feature.practice_enabled';
UPDATE system_settings SET description = '积分系统功能开关' WHERE setting_key = 'feature.credit_enabled';
UPDATE system_settings SET description = '英语每周新词量' WHERE setting_key = 'remedial.weekly_new_words';
UPDATE system_settings SET description = '数学开始推送学习包的周数' WHERE setting_key = 'remedial.math_start_week';
UPDATE system_settings SET description = '诊断分<该值自动入偏科组' WHERE setting_key = 'remedial.auto_group_threshold';
UPDATE system_settings SET description = '连续未达标预警周数' WHERE setting_key = 'remedial.streak_warn_weeks';
UPDATE system_settings SET description = '掌握达标正确率%' WHERE setting_key = 'remedial.pass_mastery';
UPDATE system_settings SET description = '英语每日建议时长(分钟)' WHERE setting_key = 'remedial.daily_english_min';
UPDATE system_settings SET description = '数学每日建议时长(分钟)' WHERE setting_key = 'remedial.daily_math_min';
