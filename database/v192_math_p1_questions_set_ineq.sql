-- ============================================================================
-- v192: 数学[职高] P1-1 补题 — 集合 + 不等式 (20题)
-- 集合: 3077定义与元素(3) + 3078表示方法(3) + 3079空集与全集(2) + 3081关系判断(3) + 3083并集运算(3) = 14题
-- 不等式: 3090一元二次不等式应用(4) + 3091含绝对值不等式解法(4) = 8题
-- 难度: easy(1) 2~3道 + medium(2) 1道，部分节点加 hard(3) 1道
-- 幂等：INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ══════════════════════════════════════════
-- 集合: 5节点 × 2~3题 = 14题
-- ══════════════════════════════════════════

-- === 3077: 集合的定义与元素 [了解] ===
SET @n3077 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='集合的定义与元素 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '下列对象中，能组成集合的是：',
 '["A. 所有很大的数","B. 某班身高较高的同学","C. 方程 $x^2-1=0$ 的所有实数解","D. 好看的图案"]', 'C',
 '集合的元素必须是确定的。A中"很大"、B中"较高"、D中"好看"都没有明确标准。C中方程的解为x=±1，是确定的。', 1, 1, 1, 1, 1, @n3077, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '若 $a$ 是集合 $A$ 中的元素，则正确的表示是：',
 '["A. $a \\\\subset A$","B. $a \\\\in A$","C. $a \\\\notin A$","D. $A \\\\in a$"]', 'B',
 '元素与集合的关系用属于符号∈。⊂用于集合与集合之间的包含关系。', 1, 1, 1, 1, 1, @n3077, NOW(), NOW()),

('数学[职高]', 'TRUE_FALSE', '集合 {1, 2, 2, 3} 中有 4 个元素。', '[]', 'F',
 '集合中的元素具有互异性，重复元素只算一个。{1,2,2,3}={1,2,3}，共3个元素。', 1, 1, 1, 1, 1, @n3077, NOW(), NOW());

-- === 3078: 集合的表示方法 [了解] ===
SET @n3078 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='集合的表示方法 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '集合 $A=\\\\{x \\\\mid -1 < x \\\\leq 2\\\\}$ 用区间表示为：',
 '["A. $[-1,2]$","B. $(-1,2]$","C. $[-1,2)$","D. $(-1,2)$"]', 'B',
 '-1<x≤2，-1取不到用开区间(，2取得到用闭区间]，即(-1,2]。', 1, 1, 1, 1, 1, @n3078, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '方程 $x^2-4=0$ 的解集用列举法表示为：',
 '["A. $\\\\{x \\\\mid x^2-4=0\\\\}$","B. $\\\\{-2,2\\\\}$","C. $\\\\{2\\\\}$","D. $\\\\{-2\\\\}$"]', 'B',
 '方程x²-4=0的解为x=-2或x=2。列举法就是把所有元素一一列出，用花括号括起来。', 1, 1, 1, 1, 1, @n3078, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '用描述法表示"大于3的实数"：$\\\\{$ x ∣ _____ $\\\\}$。',
 '[]', 'x>3',
 '描述法格式为{x|条件}，大于3即x>3。注意用实数集R时可简写，这里填x>3即可。', 2, 1, 1, 1, 1, @n3078, NOW(), NOW());

-- === 3079: 空集与全集 [了解] ===
SET @n3079 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='空集与全集 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '下列关于空集的说法，正确的是：',
 '["A. 空集包含元素0","B. $\\\\varnothing = \\\\{0\\\\}$","C. 空集是任何集合的子集","D. 空集不是任何集合的子集"]', 'C',
 '空集∅不含任何元素，所以A和B都错。空集是任何集合的子集（这是规定），C对D错。', 1, 1, 1, 1, 1, @n3079, NOW(), NOW()),

('数学[职高]', 'TRUE_FALSE', '集合 {x | x²+1=0, x∈R} 是空集。', '[]', 'T',
 '方程x²+1=0即x²=-1，在实数范围内无解，故该集合不含任何元素，是空集。', 1, 1, 1, 1, 1, @n3079, NOW(), NOW());

-- === 3081: 集合关系判断与证明 [理解] ===
SET @n3081 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='集合关系判断与证明 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '已知 $A=\\\\{1,2,3\\\\}$，$B=\\\\{1,2,3,4,5\\\\}$，则 $A$ 与 $B$ 的关系是：',
 '["A. $A \\\\in B$","B. $A \\\\subset B$","C. $A \\\\supset B$","D. $A = B$"]', 'B',
 'A中每个元素都在B中，且B中还有4,5不在A中，所以A是B的真子集，即A⊂B。', 1, 1, 1, 1, 1, @n3081, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '设 $A=\\\\{x \\\\mid x$ 是等边三角形$\\\\}$，$B=\\\\{x \\\\mid x$ 是等腰三角形$\\\\}$，则：',
 '["A. $A \\\\subset B$","B. $B \\\\subset A$","C. $A = B$","D. $A$ 与 $B$ 没有包含关系"]', 'A',
 '等边三角形一定是等腰三角形（三边相等满足两边相等），但等腰三角形不一定是等边的。所以A⊂B。', 2, 1, 1, 1, 1, @n3081, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '集合 $\\\\{1,2\\\\}$ 的所有子集为 _____（用列举法写出全部）。',
 '[]', '∅, {1}, {2}, {1,2}',
 'n个元素的集合有2ⁿ个子集。{1,2}有2²=4个子集：∅, {1}, {2}, {1,2}。注意∅和自身都是子集。', 2, 1, 1, 1, 1, @n3081, NOW(), NOW());

-- === 3083: 并集运算 [掌握] ===
SET @n3083 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='并集运算 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '已知 $A=\\\\{1,2,3\\\\}$，$B=\\\\{3,4,5\\\\}$，则 $A \\\\cup B =$',
 '["A. $\\\\{3\\\\}$","B. $\\\\{1,2,4,5\\\\}$","C. $\\\\{1,2,3,4,5\\\\}$","D. $\\\\{1,2,3\\\\}$"]', 'C',
 '并集取两个集合所有元素（重复的只写一次）：{1,2,3}∪{3,4,5}={1,2,3,4,5}。', 1, 1, 1, 1, 1, @n3083, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '设 $A=[-2,1]$，$B=[0,3]$，则 $A \\\\cup B =$',
 '["A. $[0,1]$","B. $[-2,3]$","C. $[-2,0]$","D. $[1,3]$"]', 'B',
 '在数轴上画出A=[-2,1]和B=[0,3]，并集覆盖从-2到3的全部区间，即[-2,3]。', 2, 1, 1, 1, 1, @n3083, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '若 $A \\\\cup B = A$，则 $A$ 与 $B$ 的关系是 _____。',
 '[]', 'B⊆A',
 'A∪B=A意味着B的元素全部在A中，即B是A的子集。', 3, 1, 1, 1, 1, @n3083, NOW(), NOW());


-- ══════════════════════════════════════════
-- 不等式: 2节点 × 4题 = 8题
-- ══════════════════════════════════════════

-- === 3090: 一元二次不等式的应用 [掌握] ===
SET @n3090 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='一元二次不等式的应用 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '若二次函数 $y=x^2-2x-3$ 的图像在 $x$ 轴上方，则 $x$ 的取值范围是：',
 '["A. $-1<x<3$","B. $x<-1$ 或 $x>3$","C. $x<-3$ 或 $x>1$","D. $-3<x<1$"]', 'B',
 'y=x²-2x-3=(x-3)(x+1)>0，开口向上，解集为x<-1或x>3。', 1, 1, 1, 1, 1, @n3090, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '某商品每件成本50元，售价x元时每天销量为(200-2x)件。要使每天利润不低于1200元，售价x的取值范围是：',
 '["A. $60 \\\\leq x \\\\leq 70$","B. $50 \\\\leq x \\\\leq 100$","C. $60 \\\\leq x \\\\leq 90$","D. $70 \\\\leq x \\\\leq 80$"]', 'D',
 '利润L=(x-50)(200-2x)=-2x²+300x-10000。令L≥1200，即-2x²+300x-10000≥1200，整理得x²-150x+5600≤0。Δ=150²-4×5600=100，x=(150±10)/2=80或70。开口向上，≤0取两根之间：70≤x≤80。', 2, 1, 1, 1, 1, @n3090, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '已知 $y=\\\\sqrt{x^2-4}$ 有意义，则 $x$ 的取值范围是 _____（用区间表示）。',
 '[]', '(-∞,-2]∪[2,+∞)',
 '被开方数x²-4≥0，即x²≥4，解得x≤-2或x≥2。注意这是并集。', 1, 1, 1, 1, 1, @n3090, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '不等式 $(x-1)(x+2)(x-3) < 0$ 的解集为 _____（用区间表示）。',
 '[]', '(-∞,-2)∪(1,3)',
 '穿根法：三个根-2,1,3。x<-2时三负乘积为负✓；-2<x<1时两负一正乘积为正✗；1<x<3时一负两正乘积为负✓；x>3时三正乘积为正✗。解集(-∞,-2)∪(1,3)。', 3, 1, 1, 1, 1, @n3090, NOW(), NOW());

-- === 3091: 含绝对值不等式的解法 [理解] ===
SET @n3091 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='含绝对值不等式的解法 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '不等式 $|x| < 3$ 的解集是：',
 '["A. $x < 3$","B. $x > -3$","C. $-3 < x < 3$","D. $x < -3$ 或 $x > 3$"]', 'C',
 '|x|<a(a>0)等价于-a<x<a。|x|<3即-3<x<3。', 1, 1, 1, 1, 1, @n3091, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '不等式 $|2x-1| \\\\geq 3$ 的解集是：',
 '["A. $[-1,2]$","B. $(-\\\\infty,-1] \\\\cup [2,+\\\\infty)$","C. $(-\\\\infty,1] \\\\cup [2,+\\\\infty)$","D. $[-2,1]$"]', 'B',
 '|2x-1|≥3 → 2x-1≤-3或2x-1≥3 → 2x≤-2或2x≥4 → x≤-1或x≥2。', 2, 1, 1, 1, 1, @n3091, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '不等式 $|x-2| < 1$ 的解集用区间表示为 _____。',
 '[]', '(1,3)',
 '|x-2|<1 → -1<x-2<1 → 1<x<3，即(1,3)。', 1, 1, 1, 1, 1, @n3091, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '不等式 $|x+1| > |x-3|$ 的解集为 _____。',
 '[]', '(1,+∞)',
 '两边平方：(x+1)²>(x-3)² → x²+2x+1>x²-6x+9 → 8x>8 → x>1。或者用几何意义：|x+1|>|x-3|表示x到-1的距离大于到3的距离，x在-1和3中点1的右侧。', 3, 1, 1, 1, 1, @n3091, NOW(), NOW());

COMMIT;
SELECT 'v192 deployed — 集合+不等式 20题' AS result;
-- ============================================================================
