-- 备课组审核积压预警（PENDING_GROUP任务超过48小时未处理）
INSERT IGNORE INTO inspection_alert_rules (rule_name, rule_type, target_type, threshold, comparison,
time_window, enabled, notify_inspector, notify_teacher)
VALUES ('备课组审核积压', 'GROUP_REVIEW_BACKLOG', 'GLOBAL', 48, 'GT', 'CURRENT_WEEK', 1, 1, 1);

-- 教研组活动频率过低（30天无教研活动）
INSERT IGNORE INTO inspection_alert_rules (rule_name, rule_type, target_type, threshold, comparison,
time_window, enabled, notify_inspector, notify_teacher)
VALUES ('教研组活动频率过低', 'TEACHING_RESEARCH_INACTIVE', 'GLOBAL', 30, 'GT', 'CURRENT_MONTH', 1, 1, 1);

-- 备课组活动频率过低（14天无备课记录）
INSERT IGNORE INTO inspection_alert_rules (rule_name, rule_type, target_type, threshold, comparison,
time_window, enabled, notify_inspector, notify_teacher)
VALUES ('备课组活动频率过低', 'LESSON_PREP_INACTIVE', 'GLOBAL', 14, 'GT', 'CURRENT_WEEK', 1, 1, 1);
