-- ============================================================================
-- v190: 数学[职高] P0 补题 — 指数与对数函数 + 和差倍角公式 (24题)
-- P0-1: 指数运算性质(4) + 指数函数图像与性质(4) + 对数运算性质(4) + 对数函数图像与性质(4) = 16题
-- P0-2: 两角和差公式(4) + 二倍角公式(4) = 8题
-- 难度分布：每节点 2道easy(1) + 1道medium(2) + 1道hard(3)
-- 幂等：INSERT IGNORE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

SET @math_subject_id = 22;

-- ══════════════════════════════════════════
-- P0-1：指数与对数函数 4节点 × 4题 = 16题
-- ══════════════════════════════════════════

-- === 3106: 指数运算性质 [掌握] ===
SET @n3106 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='指数运算性质 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 指数运算 选择题×2 + 填空题×2

('数学[职高]', 'SINGLE_CHOICE', '化简 $a^3 \\\\cdot a^2 \\\\cdot a^{-1}$ 的结果是：',
 '["A. $a^4$","B. $a^5$","C. $a^6$","D. $a^{-6}$"]', 'A',
 '同底数幂相乘，指数相加：3+2+(-1)=4，即 a⁴。注意负指数同样适用。', 1, 1, 1, 1, 1, @n3106, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '计算 $(2a^2)^3 \\\\div 2a^3$ 的结果是：',
 '["A. $4a^3$","B. $2a^3$","C. $4a^6$","D. $8a^3$"]', 'A',
 '(2a²)³ = 8a⁶，8a⁶÷2a³ = 4a³。幂的乘方指数相乘，同底相除指数相减。', 2, 1, 1, 1, 1, @n3106, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '计算：$16^{\\\\frac{3}{4}} = $ _____。',
 '[]', '8',
 '16^(3/4) = (2⁴)^(3/4) = 2³ = 8。分数指数幂：a^(m/n) = ⁿ√(aᵐ)。', 1, 1, 1, 1, 1, @n3106, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '若 $10^x = 2$，$10^y = 3$，则 $10^{2x-y} = $ _____（用分数表示）。',
 '[]', '4/3',
 '10^(2x-y) = (10ˣ)² ÷ 10ʸ = 2² ÷ 3 = 4/3。利用指数运算法则：同底数幂相除。', 3, 1, 1, 1, 1, @n3106, NOW(), NOW());

-- === 3107: 指数函数的图像与性质 [理解] ===
SET @n3107 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='指数函数的图像与性质 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '函数 $y = 2^x$ 的图像恒过定点：',
 '["A. $(0,0)$","B. $(1,0)$","C. $(0,1)$","D. $(1,1)$"]', 'C',
 '指数函数 y=aˣ 恒过 (0,1)，因为 a⁰=1 对任意 a>0 且 a≠1 成立。', 1, 1, 1, 1, 1, @n3107, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '函数 $y = \\\\left(\\\\frac{1}{2}\\\\right)^x$ 的单调性是：',
 '["A. 在 $\\\\mathbb{R}$ 上单调递增","B. 在 $\\\\mathbb{R}$ 上单调递减","C. 在 $(0,+\\\\infty)$ 上递增，在 $(-\\\\infty,0)$ 上递减","D. 没有单调性"]', 'B',
 'y=(1/2)ˣ 中底数 0<1/2<1，在 R 上单调递减。a>1 递增，0<a<1 递减。', 1, 1, 1, 1, 1, @n3107, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '设 $a=2^{0.3}$，$b=2^{0.1}$，$c=\\\\left(\\\\frac{1}{2}\\\\right)^{-0.2}$，则 $a,b,c$ 的大小关系是：',
 '["A. $a > b > c$","B. $a > c > b$","C. $b > a > c$","D. $c > b > a$"]', 'A',
 'y=2ˣ 在 R 上递增，0.3>0.1，所以 a>b。c=(1/2)^(-0.2)=2^0.2，b=2^0.1，故 a>c>b，即 a>b>c。', 2, 1, 1, 1, 1, @n3107, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '函数 $y=2^{x-1}+1$ 的值域为 _____。',
 '[]', '(1,+∞)',
 'y=2^(x-1) > 0，y=2^(x-1)+1 > 1，值域为 (1,+∞)。注意指数函数加常数后图像向上平移。', 3, 1, 1, 1, 1, @n3107, NOW(), NOW());

-- === 3108: 对数运算性质 [掌握] ===
SET @n3108 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='对数运算性质 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '$\\\\log_3 27$ 的值为：',
 '["A. 2","B. 3","C. 9","D. 27"]', 'B',
 'log₃27 = log₃3³ = 3。因为 3³=27，对数是指数的逆运算。', 1, 1, 1, 1, 1, @n3108, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '已知 $\\\\lg 2 = a$，$\\\\lg 3 = b$，则 $\\\\lg 12$ 等于：',
 '["A. $a+b$","B. $2a+b$","C. $a+2b$","D. $2a+2b$"]', 'B',
 'lg12 = lg(4×3) = lg4 + lg3 = lg2² + lg3 = 2lg2 + lg3 = 2a+b。积的对数=对数的和。', 2, 1, 1, 1, 1, @n3108, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '计算：$\\\\lg 5 + \\\\lg 20 = $ _____。',
 '[]', '2',
 'lg5 + lg20 = lg(5×20) = lg100 = 2。积的对数等于对数的和，再化简。', 1, 1, 1, 1, 1, @n3108, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '若 $\\\\log_2 [\\\\log_3 (\\\\log_4 x)] = 0$，则 $x = $ _____。',
 '[]', '64',
 '由外向内解：log₂[log₃(log₄x)]=0 → log₃(log₄x)=1 → log₄x=3 → x=4³=64。注意每个等号都要用到对数等于0或1时真数分别为1和底数。', 3, 1, 1, 1, 1, @n3108, NOW(), NOW());

-- === 3109: 对数函数的图像与性质 [理解] ===
SET @n3109 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='对数函数的图像与性质 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '函数 $y = \\\\log_2 (x-1)$ 的定义域是：',
 '["A. $(0,+\\\\infty)$","B. $[1,+\\\\infty)$","C. $(1,+\\\\infty)$","D. $\\\\mathbb{R}$"]', 'C',
 '对数真数必须大于0：x-1>0，即 x>1。定义域为 (1,+∞)。注意与 y=log₂x 定义域的区别。', 1, 1, 1, 1, 1, @n3109, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '函数 $y = \\\\log_a x$ 的图像恒过定点：',
 '["A. $(0,1)$","B. $(1,0)$","C. $(0,0)$","D. $(1,1)$"]', 'B',
 '对数函数 y=logₐx 恒过 (1,0)，因为 logₐ1=0。注意与指数函数 y=aˣ 过 (0,1) 区分。', 1, 1, 1, 1, 1, @n3109, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '设 $a=\\\\log_{0.5} 2$，$b=\\\\log_{0.5} 3$，则 $a$ 与 $b$ 的大小关系是：',
 '["A. $a > b$","B. $a < b$","C. $a = b$","D. 无法比较"]', 'A',
 'y=log₀.₅x 中底数 0<0.5<1，函数在 (0,+∞) 上单调递减。2<3，所以 log₀.₅2 > log₀.₅3，即 a>b。', 2, 1, 1, 1, 1, @n3109, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '不等式 $\\\\log_3 (2x-1) < 2$ 的解集为 _____（用区间表示）。',
 '[]', '(1/2, 5)',
 'log₃(2x-1)<2 → 0<2x-1<3²=9 → 1<2x<10 → 1/2<x<5。解集为 (1/2,5)。注意：对数不等式必须先保证真数>0，再利用单调性去对数。', 3, 1, 1, 1, 1, @n3109, NOW(), NOW());


-- ══════════════════════════════════════════
-- P0-2：和差公式与倍角公式 2节点 × 4题 = 8题
-- ══════════════════════════════════════════

-- === 3124: 两角和与差的正弦、余弦公式 [掌握] ===
SET @n3124 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='两角和与差的正弦、余弦公式 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '$\\\\sin 75^\\\\circ$ 的值为：',
 '["A. $\\\\frac{\\\\sqrt{6}-\\\\sqrt{2}}{4}$","B. $\\\\frac{\\\\sqrt{6}+\\\\sqrt{2}}{4}$","C. $\\\\frac{\\\\sqrt{3}}{2}$","D. $\\\\frac{1}{2}$"]', 'B',
 'sin75° = sin(45°+30°) = sin45°cos30°+cos45°sin30° = (√2/2)(√3/2)+(√2/2)(1/2) = √6/4+√2/4 = (√6+√2)/4。', 1, 1, 1, 1, 1, @n3124, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '已知 $\\\\sin\\\\alpha = \\\\frac{4}{5}$，$\\\\cos\\\\beta = \\\\frac{5}{13}$，且 $\\\\alpha,\\\\beta$ 均为锐角，则 $\\\\sin(\\\\alpha+\\\\beta)$ 的值为：',
 '["A. $\\\\frac{56}{65}$","B. $\\\\frac{33}{65}$","C. $\\\\frac{63}{65}$","D. $\\\\frac{16}{65}$"]', 'A',
 'cosα=√(1-16/25)=3/5，sinβ=√(1-25/169)=12/13。sin(α+β)=sinαcosβ+cosαsinβ=(4/5)(5/13)+(3/5)(12/13)=20/65+36/65=56/65。', 2, 1, 1, 1, 1, @n3124, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '$\\\\cos 15^\\\\circ$ 的值为：',
 '["A. $\\\\frac{\\\\sqrt{6}-\\\\sqrt{2}}{4}$","B. $\\\\frac{\\\\sqrt{6}+\\\\sqrt{2}}{4}$","C. $\\\\frac{\\\\sqrt{3}-1}{2}$","D. $\\\\frac{\\\\sqrt{3}+1}{2}$"]', 'B',
 'cos15°=cos(45°-30°)=cos45°cos30°+sin45°sin30°=(√2/2)(√3/2)+(√2/2)(1/2)=(√6+√2)/4。注意 cos 公式中间符号与括号相反。', 2, 1, 1, 1, 1, @n3124, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '化简：$\\\\sin 20^\\\\circ \\\\cos 10^\\\\circ + \\\\cos 20^\\\\circ \\\\sin 10^\\\\circ = $ _____。',
 '[]', '1/2',
 '原式 = sin(20°+10°) = sin30° = 1/2。这是两角和的正弦公式：sin(α+β)=sinαcosβ+cosαsinβ。', 3, 1, 1, 1, 1, @n3124, NOW(), NOW());

-- === 3125: 二倍角公式 [掌握] ===
SET @n3125 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二倍角公式 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '已知 $\\\\sin\\\\alpha = \\\\frac{4}{5}$，且 $\\\\alpha$ 为锐角，则 $\\\\sin 2\\\\alpha$ 的值为：',
 '["A. $\\\\frac{24}{25}$","B. $\\\\frac{12}{25}$","C. $\\\\frac{7}{25}$","D. $\\\\frac{8}{25}$"]', 'A',
 'cosα=√(1-16/25)=3/5。sin2α=2sinαcosα=2×(4/5)×(3/5)=24/25。', 1, 1, 1, 1, 1, @n3125, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '已知 $\\\\cos\\\\alpha = \\\\frac{3}{5}$，则 $\\\\cos 2\\\\alpha$ 的值为：',
 '["A. $\\\\frac{7}{25}$","B. $-\\\\frac{7}{25}$","C. $\\\\frac{24}{25}$","D. $\\\\frac{18}{25}$"]', 'B',
 'cos2α=2cos²α-1=2×(9/25)-1=18/25-1=-7/25。也可用 cos2α=1-2sin²α=1-2×(16/25)=-7/25。', 1, 1, 1, 1, 1, @n3125, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '若 $\\\\tan\\\\alpha = 2$，则 $\\\\tan 2\\\\alpha = $ _____。',
 '[]', '-4/3',
 'tan2α = 2tanα/(1-tan²α) = 2×2/(1-4) = 4/(-3) = -4/3。', 2, 1, 1, 1, 1, @n3125, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '函数 $y = \\\\sin x \\\\cos x$ 的最大值为 _____。',
 '[]', '1/2',
 'y=sinx·cosx=(1/2)sin2x。因为 sin2x 的最大值为 1，所以 y 的最大值为 1/2。利用二倍角公式的逆用：sin2α=2sinαcosα。', 3, 1, 1, 1, 1, @n3125, NOW(), NOW());

COMMIT;

-- ============================================================================
-- 验证查询
-- ============================================================================
SELECT 'v190 deployed' AS version;
SELECT category_id, COUNT(*) AS cnt FROM question_bank WHERE category_id IN (
    SELECT id FROM knowledge_nodes WHERE subject_id=22
    AND name IN ('指数运算性质 [掌握]','指数函数的图像与性质 [理解]','对数运算性质 [掌握]','对数函数的图像与性质 [理解]','两角和与差的正弦、余弦公式 [掌握]','二倍角公式 [掌握]')
    AND level=4
) GROUP BY category_id;
-- ============================================================================
