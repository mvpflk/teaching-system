-- ============================================================================
-- v158: English[职高] difficulty distribution rebalance
-- Target: BASIC 24 / MEDIUM 18 / HARD 12 / MASTER 5 (towards 40:30:20:10)
-- ============================================================================
SET NAMES utf8mb4;

-- MEDIUM → BASIC
UPDATE knowledge_articles SET difficulty=1 WHERE subject_id=24 AND task='细节理解题';
UPDATE knowledge_articles SET difficulty=1 WHERE subject_id=24 AND task='主谓一致';
UPDATE knowledge_articles SET difficulty=1 WHERE subject_id=24 AND task='情态动词';
UPDATE knowledge_articles SET difficulty=1 WHERE subject_id=24 AND task='状语从句';
UPDATE knowledge_articles SET difficulty=1 WHERE subject_id=24 AND task='语法速查';

-- MEDIUM → HARD
UPDATE knowledge_articles SET difficulty=3 WHERE subject_id=24 AND task='虚拟语气';
UPDATE knowledge_articles SET difficulty=3 WHERE subject_id=24 AND title='非限制性定语从句';
UPDATE knowledge_articles SET difficulty=3 WHERE subject_id=24 AND title='过去完成时';
UPDATE knowledge_articles SET difficulty=3 WHERE subject_id=24 AND title='主谓一致与非谓语改错';

SELECT difficulty, COUNT(*) AS cnt FROM knowledge_articles WHERE subject_id=24 GROUP BY difficulty ORDER BY difficulty;
