-- ============================================================================
-- v169: dict_subject 新增 card_profile_group 字段
-- 用于学科分组的卡片生成 Prompt 路由
--   public-math     → 公式定理基础优先(≥40% DEFINITION)
--   public-language → 字词/语法/默写基础优先(≥50% DEFINITION)
--   major           → AI根据节点content自主判断应知/应会配比
--   NULL            → 代码层兜底为 major
-- ============================================================================
SET NAMES utf8mb4;

ALTER TABLE dict_subject
  ADD COLUMN card_profile_group VARCHAR(20) DEFAULT NULL
    COMMENT '卡片Prompt分组: public-math / public-language / major, NULL→major兜底';

-- 公共-数理：数学[职高]
UPDATE dict_subject SET card_profile_group = 'public-math'
  WHERE subject_name = '数学[职高]' AND status = 1;

-- 公共-语言：语文[职高]、英语[职高]
UPDATE dict_subject SET card_profile_group = 'public-language'
  WHERE subject_name IN ('语文[职高]', '英语[职高]') AND status = 1;

-- 其余学科不设值(NULL)，代码层兜底为 "major"
