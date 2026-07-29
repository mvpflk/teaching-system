-- Clear and reseed test data
SET FOREIGN_KEY_CHECKS=0;
DELETE FROM student_answers;
DELETE FROM backup_exam_results;
DELETE FROM backup_exam_questions;
DELETE FROM backup_exams;
DELETE FROM credit_transactions;
DELETE FROM sign_records;
DELETE FROM credit_rules;
DELETE FROM title_levels;
DELETE FROM backup_homework_submissions;
DELETE FROM backup_homework_assignments;
DELETE FROM student_class_history;
DELETE FROM student_stage_change_log;
DELETE FROM students;
DELETE FROM teachers;
DELETE FROM classes;
DELETE FROM users;
DELETE FROM schools;
DELETE FROM stages;

-- 基础数据
INSERT INTO schools (id, name, code) VALUES (1, 'Default School', 'DEFAULT001');
INSERT INTO stages (id, code, name, sort_order, grade_years) VALUES (4, 'VOCATIONAL', 'Vocational', 4, 3);

INSERT INTO users (id, username, password, real_name, role_id, school_id, current_stage_id, status) VALUES
(1, 'admin', 'admin123', 'Admin', 1, 1, 4, 1),
(2, 'teacher1', 'test123', 'Teacher Zhang', 2, 1, 4, 1),
(3, 'student1', 'test123', 'Student Li', 4, 1, 4, 1);

INSERT INTO teachers (id, user_id, teacher_number, school_id, subject) VALUES
(1, 2, 'T001', 1, 'Java');

INSERT INTO classes (id, class_name, class_code, grade, major, school_id, stage_id, status) VALUES
(1, 'CS2025-01', 'CS2025-01', '2025', 'CS', 1, 4, 1);

INSERT INTO students (id, user_id, student_number, class_id, school_id, current_stage_id, original_stage_id, total_credits, title_level, current_streak) VALUES
(1, 3, 'S2025001', 1, 1, 4, 4, 150, 2, 3);

INSERT INTO student_class_history (student_id, class_id, stage_id, school_id, start_date) VALUES
(1, 1, 4, 1, CURRENT_DATE);

INSERT INTO credit_rules (id, rule_code, rule_name, action_type, credit_value, max_daily_count, status) VALUES
(1, 'SIGN_DAILY', 'Daily Sign', 'sign', 5, 1, 1),
(2, 'SIGN_STREAK_3', '3-day Streak', 'sign', 15, NULL, 1);

INSERT INTO title_levels (id, level_number, level_name, min_credits, max_credits) VALUES
(1, 1, 'Bronze', 0, 99),
(2, 2, 'Silver', 100, 299),
(3, 3, 'Gold', 300, 599),
(4, 4, 'Diamond', 600, 999),
(5, 5, 'King', 1000, NULL);
SET FOREIGN_KEY_CHECKS=1;
