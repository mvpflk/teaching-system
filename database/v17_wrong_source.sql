-- v17: wrong_questions 新增 source_type, source_task_id 用于展示错题来源
ALTER TABLE `wrong_questions`
  ADD COLUMN `source_type` VARCHAR(30) DEFAULT NULL COMMENT '来源类型: EXAM/HOMEWORK/QUIZ/BUZZ/PRACTICE' AFTER `is_mastered`,
  ADD COLUMN `source_task_id` BIGINT DEFAULT NULL COMMENT '来源任务ID(考试/作业ID)' AFTER `source_type`;
