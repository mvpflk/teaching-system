-- v54: 签到防重复 — 加唯一约束防并发双重点击
ALTER TABLE sign_records ADD UNIQUE uk_student_sign_date (student_id, sign_date);
