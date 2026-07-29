ALTER TABLE practice_plans ADD COLUMN shared TINYINT(1) DEFAULT 0 COMMENT '是否共享到学科库';
ALTER TABLE practice_plans ADD COLUMN subject VARCHAR(50) DEFAULT NULL COMMENT '所属学科（用于共享库过滤）';
