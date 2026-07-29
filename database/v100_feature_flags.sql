-- v100: Feature 开关初始数据 (2026-06-01)
-- 关闭 5 个模块: BBS论坛、家长端、巡视面板、积分系统、家校消息
-- 开启 3 个模块: 智慧大屏、打字竞赛、实训任务

INSERT INTO `system_settings` (`setting_key`, `setting_value`, `description`, `category`, `value_type`, `is_editable`, `order_num`, `school_id`)
VALUES
('feature.bbs_enabled',       'false', 'BBS论坛功能开关',              'feature', 'boolean', 1, 1, 1),
('feature.parent_enabled',    'false', '家长端功能开关',               'feature', 'boolean', 1, 2, 1),
('feature.inspector_enabled', 'false', '巡视面板功能开关',              'feature', 'boolean', 1, 3, 1),
('feature.credit_enabled',    'false', '积分系统功能开关',              'feature', 'boolean', 1, 4, 1),
('feature.message_enabled',   'false', '家校消息功能开关',              'feature', 'boolean', 1, 5, 1),
('feature.classroom_enabled', 'true',  '智慧大屏/课堂互动功能开关',      'feature', 'boolean', 1, 6, 1),
('feature.typing_enabled',    'true',  '打字练习/竞赛功能开关',         'feature', 'boolean', 1, 7, 1),
('feature.practice_enabled',  'true',  '实训任务功能开关',              'feature', 'boolean', 1, 8, 1)
ON DUPLICATE KEY UPDATE
  `setting_value` = VALUES(`setting_value`),
  `description`   = VALUES(`description`),
  `updated_at`    = CURRENT_TIMESTAMP;
