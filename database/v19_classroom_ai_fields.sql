-- v19: AI教学助手→大屏数据管道 — classroom_questions新增AI字段
-- 日期: 2026-05-24
-- 说明: 为 classroom_questions 表新增 questionType/fromAi/intent/aiCategory 字段，
--       支持AI生成的课堂提问自动推送到大屏的"AI推荐"Tab

ALTER TABLE `classroom_questions`
    ADD COLUMN `question_type` VARCHAR(50) DEFAULT NULL COMMENT '题目类型(SHORT_ANSWER/TRUE_FALSE等)',
    ADD COLUMN `from_ai` TINYINT(1) DEFAULT 0 COMMENT 'AI教学助手推送标记(0=非AI, 1=AI生成)',
    ADD COLUMN `intent` VARCHAR(200) DEFAULT NULL COMMENT 'AI题目意图(如"检查IP概念理解")',
    ADD COLUMN `ai_category` VARCHAR(50) DEFAULT NULL COMMENT 'AI题目分类(RECALL/COMPREHEND/APPLY/EXTEND)';

-- 为 source 字段添加索引（支持 fromAi 筛选性能）
ALTER TABLE `classroom_questions`
    ADD INDEX `idx_from_ai` (`from_ai`),
    ADD INDEX `idx_source` (`source`);
