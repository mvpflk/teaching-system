-- ============================================================================
-- v191: 数学[职高] P2 修复 — 超纲题删除 + 题型标签统一
-- P2-1: 删除 Q2366 (f(x)=x³-3x 极小值点·需导数·职高不讲导数) + 替换为定义法题目
-- P2-2: 修正 question_type='多选题' → 'MULTI_CHOICE'
-- 幂等：可重复执行
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ══════════════════════════════════════════
-- P2-1：处理超纲题 Q2366
-- ══════════════════════════════════════════

-- 先查询确认 Q2366 是否存在且为导数相关题目
-- SELECT id, question_text, question_type, status FROM question_bank WHERE id = 2366;

-- 方案：将 Q2366 标记为删除（status=2），同时插入一道用定义法判断单调性的替换题
UPDATE question_bank SET status = 2 WHERE id = 2366 AND subject LIKE '%数学%';

-- 在 Q2366 原属节点（3099: 函数单调性判断[掌握]）补一道用定义法的题目
SET @n3099 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数单调性判断 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time)
SELECT '数学[职高]', 'SINGLE_CHOICE',
       '用定义法判断函数 $f(x)=x^2+2x$ 在区间 $[-1,+\\\\infty)$ 上的单调性：',
       '["A. 单调递增","B. 单调递减","C. 先减后增","D. 不单调"]',
       'A',
       '设 x₁>x₂≥-1，则 f(x₁)-f(x₂)=(x₁²+2x₁)-(x₂²+2x₂)=(x₁-x₂)(x₁+x₂+2)。由于 x₁>x₂，x₁-x₂>0；又 x₁≥-1,x₂≥-1，x₁+x₂+2≥0（仅当 x₁=x₂=-1 时为 0）。所以 f(x₁)>f(x₂)，函数在 [-1,+∞) 上单调递增。也可配方：f(x)=(x+1)²-1，对称轴 x=-1，开口向上，在 [-1,+∞) 上递增。',
       2, 1, 1, 1, 1, @n3099, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM question_bank WHERE question_text LIKE '%用定义法判断%f(x)=x^2+2x%' AND subject LIKE '%数学%');

-- ══════════════════════════════════════════
-- P2-2：统一题型标签
-- ══════════════════════════════════════════

UPDATE question_bank
SET question_type = 'MULTI_CHOICE'
WHERE question_type = '多选题'
  AND category_id IN (SELECT id FROM knowledge_nodes WHERE subject_id = 22);

-- 确认修复结果
SELECT CONCAT('P2-2: 修正了 ', ROW_COUNT(), ' 道题为 MULTI_CHOICE') AS result;

COMMIT;

-- ============================================================================
SELECT 'v191 deployed — P2 fixes complete' AS version;
-- ============================================================================
