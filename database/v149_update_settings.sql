-- ============================================================================
-- v149: 数学正式学习包起始周默认值修正
-- 幂等：UPDATE
-- ============================================================================
UPDATE system_settings SET setting_value = '1'
WHERE setting_key = 'remedial.math_start_week' AND setting_value = '6';
SELECT 'v149: math_start_week 已更新为 1' AS result;
