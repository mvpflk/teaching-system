-- v211: 修复 teacher_id 错位问题
-- 旧代码将 users.id 写入 teacher_id，新代码写入 teachers.id
-- 需要将 classroom_questions 和 classroom_sessions 中的 teacher_id 从 users.id 映射到 teachers.id
-- ⚠️ 上线前必须执行此脚本！执行后必须确认 orphan_count 均为 0。

-- ===== 迁移前：检查无法匹配的孤立 teacher_id =====
SELECT 'BEFORE — classroom_questions orphan' AS step, COUNT(*) AS orphan_count
FROM classroom_questions cq
LEFT JOIN teachers t ON cq.teacher_id = t.user_id
WHERE t.id IS NULL;

SELECT 'BEFORE — classroom_sessions orphan' AS step, COUNT(*) AS orphan_count
FROM classroom_sessions cs
LEFT JOIN teachers t ON cs.teacher_id = t.user_id
WHERE t.id IS NULL;

-- ===== 执行映射 =====
UPDATE classroom_questions cq
JOIN teachers t ON cq.teacher_id = t.user_id
SET cq.teacher_id = t.id;

UPDATE classroom_sessions cs
JOIN teachers t ON cs.teacher_id = t.user_id
SET cs.teacher_id = t.id;

-- ===== 迁移后验证：确认所有行都已成功映射（teacher_id 已转为 teachers.id） =====
SELECT 'AFTER — classroom_questions orphan' AS step, COUNT(*) AS orphan_count
FROM classroom_questions cq
LEFT JOIN teachers t ON cq.teacher_id = t.id
WHERE t.id IS NULL;

SELECT 'AFTER — classroom_sessions orphan' AS step, COUNT(*) AS orphan_count
FROM classroom_sessions cs
LEFT JOIN teachers t ON cs.teacher_id = t.id
WHERE t.id IS NULL;

-- 添加智慧大屏功能开关（默认启用）
INSERT INTO system_settings (setting_key, setting_value, default_value, value_type, category, description, order_num)
SELECT 'feature.smart_screen_enabled', 'true', 'true', 'boolean', 'feature', '智慧大屏功能开关（抽问/抢答/投票/实时互动）', 20
FROM dual WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'feature.smart_screen_enabled');