USE teaching_system;

-- 新增 stage_ids JSON 列
ALTER TABLE teaching_group ADD COLUMN stage_ids JSON NULL COMMENT '学段ID列表（多选）' AFTER stage_id;

-- 将现有 stage_id 迁移为 JSON 数组
UPDATE teaching_group SET stage_ids = JSON_ARRAY(stage_id) WHERE stage_id IS NOT NULL;

-- 删除旧列
ALTER TABLE teaching_group DROP COLUMN stage_id;

SELECT 'v21 教研组学段多选完成！' AS message;
