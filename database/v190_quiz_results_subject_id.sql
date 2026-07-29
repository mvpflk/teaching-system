-- ============================================================================
-- v190: knowledge_quiz_results 加 subject_id 列，实现按学科精确统计
-- 幂等：IF NOT EXISTS / IGNORE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;

ALTER TABLE knowledge_quiz_results
  ADD COLUMN subject_id BIGINT DEFAULT NULL AFTER article_id,
  ADD INDEX idx_student_subject (student_id, subject_id, created_at);

-- 回填已有数据的 subject_id
UPDATE knowledge_quiz_results r
  JOIN knowledge_articles a ON r.article_id = a.id
  SET r.subject_id = a.subject_id
  WHERE r.subject_id IS NULL;
