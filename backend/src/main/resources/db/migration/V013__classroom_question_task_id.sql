-- v88: classroom_questions 增加 task_id 列（支持课堂任务关联抽问题库）
-- 问题: 本地源码比生产数据库新，ClassroomQuestion 实体含 taskId 字段但 DB 无此列
-- 修复: 2026-05-30

ALTER TABLE classroom_questions
    ADD COLUMN task_id BIGINT NULL COMMENT '关联任务ID，支持课堂任务场景下抽问题库筛选'
    AFTER id;
