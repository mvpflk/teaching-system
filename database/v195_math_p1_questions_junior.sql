-- ============================================================================
-- v195: 数学[职高] P1-1 补题 — 初中基础补漏 (23题)
-- 3179实数分类(2) + 3181分式约分通分(2) + 3182解一元一次方程(3) +
-- 3183代入加减消元(3) + 3185判别式韦达定理(3) + 3186一次函数图像斜率(3) +
-- 3187一次函数与方程不等式(2) + 3188三角形内角和分类(2) + 3189勾股定理应用(3)
-- 难度: 基础内容以 easy(1) 为主，少量 medium(2)
-- 幂等：INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ══════════════════════════════════════════
-- 3179: 实数的概念与分类
-- ══════════════════════════════════════════

SET @n3179 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='实数的概念与分类' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '下列数中属于无理数的是：',
 '["A. $\\\\dfrac{1}{3}$","B. $\\\\sqrt{4}$","C. $\\\\pi$","D. $0.5$"]', 'C',
 'π是无限不循环小数，属于无理数。1/3和0.5是有理数，√4=2是有理数。', 1, 1, 1, 1, 1, @n3179, NOW(), NOW()),

('数学[职高]', 'TRUE_FALSE', '$\\\\sqrt{2}$ 是有理数。', '[]', 'F',
 '√2≈1.414...是无限不循环小数，是无理数。最早由毕达哥拉斯学派发现。', 1, 1, 1, 1, 1, @n3179, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3181: 分式的约分与通分
-- ══════════════════════════════════════════

SET @n3181 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='分式的约分与通分' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '化简 $\\\\dfrac{x^2-4}{x-2}$（$x \\\\neq 2$）的结果是：',
 '["A. $x-2$","B. $x+2$","C. $x$","D. $-x-2$"]', 'B',
 '分子因式分解：x²-4=(x+2)(x-2)，约去(x-2)，得x+2。前提x≠2。', 1, 1, 1, 1, 1, @n3181, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '通分：$\\\\dfrac{1}{x} + \\\\dfrac{1}{x+1} = $ _____（写成一个分式）。',
 '[]', '(2x+1)/(x(x+1))',
 '公分母为x(x+1)，通分：[(x+1)+x]/[x(x+1)]=(2x+1)/[x(x+1)]。', 2, 1, 1, 1, 1, @n3181, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3182: 解一元一次方程
-- ══════════════════════════════════════════

SET @n3182 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='解一元一次方程' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '方程 $3x-7=2x+1$ 的解是：',
 '["A. $x=6$","B. $x=8$","C. $x=-6$","D. $x=-8$"]', 'B',
 '3x-7=2x+1 → 3x-2x=1+7 → x=8。', 1, 1, 1, 1, 1, @n3182, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '解方程：$2(x-1)+3=5x-4$，得 $x=$ _____。',
 '[]', '5/3',
 '2x-2+3=5x-4 → 2x+1=5x-4 → 5=3x → x=5/3。', 1, 1, 1, 1, 1, @n3182, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '方程 $\\\\dfrac{x}{2} - \\\\dfrac{x-1}{3} = 1$ 的解为 $x=$ _____。',
 '[]', '4',
 '通分：3x/6-2(x-1)/6=1 → (3x-2x+2)/6=1 → (x+2)/6=1 → x+2=6 → x=4。', 2, 1, 1, 1, 1, @n3182, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3183: 代入消元法与加减消元法
-- ══════════════════════════════════════════

SET @n3183 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='代入消元法与加减消元法' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '方程组 $\\\\begin{cases} x+y=5 \\\\\\\\ x-y=1 \\\\end{cases}$ 的解是：',
 '["A. $(2,3)$","B. $(3,2)$","C. $(1,4)$","D. $(4,1)$"]', 'B',
 '加减消元：两式相加得2x=6，x=3；代入①得3+y=5，y=2。解为(3,2)。', 1, 1, 1, 1, 1, @n3183, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '解方程组 $\\\\begin{cases} 2x+y=7 \\\\\\\\ x-2y=-4 \\\\end{cases}$，得 $x=$ _____，$y=$ _____。',
 '[]', 'x=2, y=3',
 '由②得x=2y-4，代入①：2(2y-4)+y=7 → 4y-8+y=7 → 5y=15 → y=3，x=2·3-4=2。', 2, 1, 1, 1, 1, @n3183, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '解方程组 $\\\\begin{cases} 3x+2y=12 \\\\\\\\ 2x-2y=2 \\\\end{cases}$，得 $x=$ _____。',
 '[]', '14/5',
 '加减消元：①+②得5x=14，x=14/5，代入y=2x-1=23/5。解法：用加减消元法直接消去y。', 2, 1, 1, 1, 1, @n3183, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3185: 根的判别式与韦达定理
-- ══════════════════════════════════════════

SET @n3185 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='根的判别式与韦达定理' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '方程 $x^2-5x+6=0$ 的判别式 $\\\\Delta$ 的值是：',
 '["A. 1","B. 25","C. -1","D. 49"]', 'A',
 'Δ=b²-4ac=25-24=1>0，方程有两个不相等的实数根。', 1, 1, 1, 1, 1, @n3185, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '若一元二次方程 $x^2+mx+4=0$ 有两个相等的实数根，则 $m=$',
 '["A. $\\\\pm 2$","B. 4","C. $\\\\pm 4$","D. 2"]', 'C',
 'Δ=m²-16=0，m=±4。有两个相等实根等价于Δ=0。', 2, 1, 1, 1, 1, @n3185, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '已知 $x_1, x_2$ 是方程 $x^2-6x+8=0$ 的两根，则 $x_1+x_2=$ _____，$x_1 x_2=$ _____。',
 '[]', '6, 8',
 '韦达定理：x₁+x₂=-b/a=6，x₁x₂=c/a=8。不用解方程即可得出。', 1, 1, 1, 1, 1, @n3185, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3186: 一次函数的图像与斜率
-- ══════════════════════════════════════════

SET @n3186 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='一次函数的图像与斜率' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '一次函数 $y=2x-3$ 的图像与 $y$ 轴的交点坐标是：',
 '["A. $(0,2)$","B. $(0,-3)$","C. $(-3,0)$","D. $(2,0)$"]', 'B',
 'y=kx+b中，b=-3是y轴截距，交点(0,-3)。令x=0也可得。', 1, 1, 1, 1, 1, @n3186, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '若一次函数 $y=kx+b$ 中 $k<0, b>0$，则该函数的图像经过：',
 '["A. 一二三象限","B. 一二四象限","C. 一三四象限","D. 二三四象限"]', 'B',
 'k<0图像下降，b>0与y轴正半轴相交。图像从第二象限开始，经过第一象限，到第四象限。即一二四象限。', 2, 1, 1, 1, 1, @n3186, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '过点 $(1,3)$ 和 $(2,5)$ 的直线斜率为 _____。',
 '[]', '2',
 'k=(5-3)/(2-1)=2/1=2。斜率=Δy/Δx。', 1, 1, 1, 1, 1, @n3186, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3187: 一次函数与方程不等式的关系
-- ══════════════════════════════════════════

SET @n3187 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='一次函数与方程不等式的关系' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '从图像上看，一次函数 $y=2x-4$ 与 $x$ 轴交点的横坐标等于方程 $2x-4=0$ 的解，该解为：',
 '["A. $x=-2$","B. $x=0$","C. $x=2$","D. $x=4$"]', 'C',
 '2x-4=0，x=2。图像与x轴交点坐标(2,0)，横坐标就是方程的解。', 1, 1, 1, 1, 1, @n3187, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '不等式 $3x-6 > 0$ 的解集为 _____。',
 '[]', 'x>2',
 '3x>6，x>2。几何意义：直线y=3x-6在x轴上方对应的x的范围。', 1, 1, 1, 1, 1, @n3187, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3188: 三角形内角和与分类
-- ══════════════════════════════════════════

SET @n3188 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='三角形内角和与分类' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '三角形三个内角之和等于：',
 '["A. $90^\\\\circ$","B. $180^\\\\circ$","C. $270^\\\\circ$","D. $360^\\\\circ$"]', 'B',
 '三角形内角和恒等于180°，这是平面几何的基本定理。', 1, 1, 1, 1, 1, @n3188, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '已知三角形两个内角分别为 $50^\\\\circ$ 和 $60^\\\\circ$，则第三个内角为 _____°。',
 '[]', '70',
 '180°-50°-60°=70°。', 1, 1, 1, 1, 1, @n3188, NOW(), NOW());

-- ══════════════════════════════════════════
-- 3189: 勾股定理与简单应用
-- ══════════════════════════════════════════

SET @n3189 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='勾股定理与简单应用' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '在直角三角形中，两直角边分别为3和4，则斜边长为：',
 '["A. 5","B. 6","C. 7","D. $\\\\sqrt{7}$"]', 'A',
 '勾股定理：c²=3²+4²=9+16=25，c=5。这是最经典的勾股数3-4-5。', 1, 1, 1, 1, 1, @n3189, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '下列哪组数能构成直角三角形的三边长？',
 '["A. 1, 2, 3","B. 2, 3, 4","C. 6, 8, 10","D. 3, 4, 6"]', 'C',
 '验证：6²+8²=36+64=100=10²。A:1+2=3不能构成三角形；B:4+9=13≠16；D:9+16=25≠36。', 1, 1, 1, 1, 1, @n3189, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '等腰直角三角形斜边长为 $5\\\\sqrt{2}$，则直角边长为 _____。',
 '[]', '5',
 '设直角边长为a，则a²+a²=50→2a²=50→a²=25→a=5。等腰直角三角形的三边比为1:1:√2。', 2, 1, 1, 1, 1, @n3189, NOW(), NOW());

COMMIT;
SELECT 'v195 deployed — 初中基础补漏 23题' AS result;
-- ============================================================================
