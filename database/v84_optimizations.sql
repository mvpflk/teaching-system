-- v84_optimizations.sql
-- R81 四大优化 — 数据库迁移
-- 方向一：教师AI纠错 — edited_by_teacher 字段
-- 方向三：难度透明化 — question_skip_log 跳过日志表

-- 1. QuestionBank 新增 editedByTeacher 字段（区别AI原文和教师修正）
ALTER TABLE question_bank ADD COLUMN edited_by_teacher TINYINT DEFAULT 0 COMMENT '是否经教师编辑: 0=AI原文, 1=教师修正';

-- 2. 题目跳过日志表
CREATE TABLE IF NOT EXISTS question_skip_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL COMMENT '题目ID(question_bank.id)',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    reason VARCHAR(30) NOT NULL COMMENT '跳过原因: TOO_HARD/OUT_OF_SYLLABUS/UNCLEAR',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_question (question_id),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目跳过日志';
