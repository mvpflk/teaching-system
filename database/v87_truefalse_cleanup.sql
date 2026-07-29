-- ============================================================================
-- v87: TRUE_FALSE 判分全链路修复 — 数据清洗
-- 1. 统一 correct_answer: T/F/对/错 → A/B（基于语义而非字母）
-- 2. 修复已产生的误判 student_answers
-- 3. 清理误入库的 wrong_questions 记录
-- ============================================================================

-- 步骤1: 统一 correct_answer
-- T/TRUE/对/正确/√ → A（语义=True→正确→前端A）
UPDATE question_bank SET correct_answer = 'A'
WHERE question_type = 'TRUE_FALSE'
  AND correct_answer IN ('T', 'TRUE', '对', '正确', '√');

-- F/FALSE/错/错误/× → B（语义=False→错误→前端B）
UPDATE question_bank SET correct_answer = 'B'
WHERE question_type = 'TRUE_FALSE'
  AND correct_answer IN ('F', 'FALSE', '错', '错误', '×');

-- 步骤2: 修正已误判的 student_answers（答案与正确答案相同但被判0）
UPDATE student_answers sa
JOIN question_bank qb ON sa.question_id = qb.id
SET sa.is_correct = 1
WHERE qb.question_type = 'TRUE_FALSE'
  AND sa.is_correct = 0
  AND sa.student_answer = qb.correct_answer;

-- 步骤3: 清理误入库的 wrong_questions（答案正确但被收录错题本）
UPDATE wrong_questions wq
INNER JOIN question_bank qb ON wq.question_id = qb.id
INNER JOIN task_submissions ts ON ts.student_id = wq.student_id
INNER JOIN student_answers sa ON sa.question_id = wq.question_id AND sa.submission_id = ts.id
SET wq.is_mastered = 1
WHERE qb.question_type = 'TRUE_FALSE'
  AND sa.is_correct = 1
  AND wq.is_mastered = 0
  AND sa.student_answer = qb.correct_answer;

SELECT 'v87 TRUE_FALSE cleanup completed' AS '';
