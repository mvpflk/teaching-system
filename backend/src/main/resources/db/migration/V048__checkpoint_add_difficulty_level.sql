-- 闯关练习：checkpoint_config 新增 difficulty_level 字段
-- 为关卡难度梯度提供数据基础（1=基础 2=较易 3=中等 4=较难 5=挑战）

ALTER TABLE checkpoint_config
  ADD COLUMN difficulty_level TINYINT DEFAULT 2 COMMENT '关卡难度 1-5（1=基础 2=较易 3=中等 4=较难 5=挑战）'
  AFTER question_count;

-- 存量数据：已有关卡全部标记为中等（3），后续由管理员按需调整
UPDATE checkpoint_config SET difficulty_level = 3 WHERE difficulty_level IS NULL;
