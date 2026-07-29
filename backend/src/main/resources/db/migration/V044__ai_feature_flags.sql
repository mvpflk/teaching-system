-- V044__ai_feature_flags.sql
-- 修复 AI 功能开关的默认值

INSERT INTO system_settings (setting_key, setting_value, default_value, value_type, category, description, is_editable)
VALUES ('feature.sse_enabled', 'true', 'true', 'boolean', 'AI', 'SSE 实时推送开关（AI 生成进度流式推送）', 1)
ON DUPLICATE KEY UPDATE setting_value = 'true', description = VALUES(description);

-- ai_content_enabled：仅当当前值为 'false' 时才改为 'true'（不覆盖管理员手工关闭的情况）
UPDATE system_settings
SET setting_value = 'true'
WHERE setting_key = 'feature.ai_content_enabled'
  AND setting_value = 'false';
