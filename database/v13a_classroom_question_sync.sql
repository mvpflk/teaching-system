-- v13a: classroom_questions 新增 synced_question_bank_id 字段
-- 课堂手动创建的题目，其 ID 不在 question_bank 中，但 wrong_questions.question_id 有外键约束 REFERENCES question_bank(id)
-- 因此在答错收录时，需将 classroom_question 同步到 question_bank，并记录对应 ID
ALTER TABLE classroom_questions ADD COLUMN synced_question_bank_id BIGINT DEFAULT NULL COMMENT '同步到 question_bank 后的对应 ID';
