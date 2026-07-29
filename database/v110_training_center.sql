-- v110: 实训中心 Phase 1 — category 字段 + feature 开关
-- 依赖：v100_simulation.sql（表结构）、v101_simulation_seed.sql（种子数据）

-- 1. 新增 category 列
ALTER TABLE simulation_tasks
  ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'win7'
  COMMENT '实训分类: win7/network/agri/build/web'
  AFTER mode;

-- 2. 新增索引
ALTER TABLE simulation_tasks ADD INDEX idx_category (category);

-- 3. 更新已有数据
UPDATE simulation_tasks SET category = 'win7' WHERE category IS NULL OR category = '';

-- 4. Feature 开关（保留 feature.win7_simulation 兼容旧版）
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('feature.training_center', 'true', '实训中心模块开关（包含 Windows + 网络实训）')
ON DUPLICATE KEY UPDATE setting_value = 'true';
