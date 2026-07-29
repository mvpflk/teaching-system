-- v168: 闯关学习专业隔离增强
-- 1. dict_subject 添加 is_public 字段标记公共学科
-- 2. 更新现有公共学科（语数英）的 is_public 标记

ALTER TABLE dict_subject ADD COLUMN is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公共学科(1=所有专业可见, 0=仅专业映射可见)' AFTER status;

-- 语文[职高]、数学[职高]、英语[职高] 标记为公共学科
UPDATE dict_subject SET is_public = 1 WHERE subject_name IN ('语文[职高]', '数学[职高]', '英语[职高]');
