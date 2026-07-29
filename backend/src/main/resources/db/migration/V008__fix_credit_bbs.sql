-- v82: 签到防重 + 积分交易biz_key + 原子积分更新
-- 2026-05-27 R59
-- ★ 先删重再加约束，避免 ALTER 因重复数据失败

-- 1. 清理可能的重复签到记录（保留最早的一条）
DELETE s1 FROM sign_records s1
INNER JOIN sign_records s2
WHERE s1.id > s2.id AND s1.student_id = s2.student_id AND s1.sign_date = s2.sign_date;

-- 2. sign_records 添加唯一约束防并发重复签到
ALTER TABLE sign_records ADD UNIQUE INDEX uk_student_sign_date (student_id, sign_date);

-- 3. credit_transactions 添加 biz_key 列（防重放）
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS biz_key VARCHAR(100) DEFAULT NULL;
ALTER TABLE credit_transactions ADD INDEX idx_biz_key (biz_key);
