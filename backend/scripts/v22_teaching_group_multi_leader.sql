USE teaching_system;

ALTER TABLE teaching_group ADD COLUMN leader_ids JSON NULL COMMENT '组长ID列表（最多2人）' AFTER leader_id;

UPDATE teaching_group SET leader_ids = JSON_ARRAY(leader_id) WHERE leader_id IS NOT NULL;

ALTER TABLE teaching_group DROP COLUMN leader_id;

-- 也给备课组同样处理
ALTER TABLE lesson_prep_group ADD COLUMN leader_ids JSON NULL COMMENT '组长ID列表（最多2人）' AFTER leader_id;

UPDATE lesson_prep_group SET leader_ids = JSON_ARRAY(leader_id) WHERE leader_id IS NOT NULL;

ALTER TABLE lesson_prep_group DROP COLUMN leader_id;

SELECT 'v22 组长多选完成！' AS message;
