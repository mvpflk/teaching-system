-- v156: task_submissions 增加 grading_message 列
-- 用于记录考试切屏终止/超时终止时的系统提示信息
ALTER TABLE task_submissions ADD COLUMN grading_message TEXT COMMENT '评分/终止提示信息' AFTER grade_type;
