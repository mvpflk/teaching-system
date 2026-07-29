-- v223: 新增 pass_mode 列，支持达标判定策略选择
-- 默认 'objective'（仅客观题判定），与现有隐含行为一致
-- 达标逻辑仅在 pass_rate > 0 时触发，历史数据不受影响
ALTER TABLE tasks ADD COLUMN pass_mode VARCHAR(16) NOT NULL DEFAULT 'objective'
  COMMENT '达标判定策略: objective=仅客观题, all=全判定' AFTER retake_deadline_hours;
