-- v164: practice_plans 任务库扩展
-- 新增 library_type（PRIVATE/SHARED/PRESET）和 tags（JSON标签）
ALTER TABLE practice_plans
  ADD COLUMN library_type VARCHAR(20) DEFAULT 'PRIVATE'
    COMMENT 'PRIVATE/SHARED/PRESET - 任务库分类',
  ADD COLUMN tags JSON COMMENT '标签["word","排版","表格"]';

-- 已有 shared=true 的方案标记为 SHARED
UPDATE practice_plans SET library_type = 'SHARED' WHERE shared = 1 AND library_type = 'PRIVATE';
