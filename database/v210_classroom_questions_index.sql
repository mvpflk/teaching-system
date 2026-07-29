-- v210: 抽问题库联合索引 — teacher_id + from_ai（加速教师 AI 推荐题目查询）
-- 使用存储过程做幂等保护，重复执行不会报错
CREATE PROCEDURE IF NOT EXISTS v210_add_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'classroom_questions'
          AND INDEX_NAME = 'idx_teacher_fromai'
    ) THEN
        ALTER TABLE `classroom_questions` ADD INDEX `idx_teacher_fromai` (`teacher_id`, `from_ai`);
    END IF;
END;

CALL v210_add_index();
DROP PROCEDURE IF EXISTS v210_add_index;
