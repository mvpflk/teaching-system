-- v15: task_submissions 新增 reflection 字段，学习反思持久化
ALTER TABLE `task_submissions`
  ADD COLUMN `reflection` TEXT DEFAULT NULL COMMENT '学生学习反思' AFTER `extra_submit_allowed`;
