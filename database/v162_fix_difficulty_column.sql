-- ============================================================================
-- v162: Fix difficulty column type mismatch
-- Problem: v153-160 migrations inserted string values ('BASIC','MEDIUM','HARD','MASTER')
--          into TINYINT column, which silently became 0 in MySQL non-strict mode.
-- Fix: Revert difficulty from TINYINT to reflect the actual intended numeric mapping
--      (BASIC=1, MEDIUM=2, HARD=3, MASTER=4) based on the tags column content.
-- ============================================================================
SET NAMES utf8mb4;

UPDATE knowledge_articles SET difficulty = 1
WHERE difficulty = 0 AND subject_id = 24
  AND (tags LIKE '%"基础"%' OR tags LIKE '%"BASIC"%');

UPDATE knowledge_articles SET difficulty = 2
WHERE difficulty = 0 AND subject_id = 24
  AND (tags LIKE '%"中等"%' OR tags LIKE '%"MEDIUM"%');

UPDATE knowledge_articles SET difficulty = 3
WHERE difficulty = 0 AND subject_id = 24
  AND (tags LIKE '%"困难"%' OR tags LIKE '%"HARD"%');

UPDATE knowledge_articles SET difficulty = 4
WHERE difficulty = 0 AND subject_id = 24
  AND (tags LIKE '%"掌握"%' OR tags LIKE '%"MASTER"%');

-- Fallback: if still 0, mark as 1 (BASIC)
UPDATE knowledge_articles SET difficulty = 1
WHERE difficulty = 0 AND subject_id = 24;

SELECT difficulty, COUNT(*) AS cnt FROM knowledge_articles WHERE subject_id = 24 GROUP BY difficulty ORDER BY difficulty;
