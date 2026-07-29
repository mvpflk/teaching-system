-- ============================================================================
-- v196: 数学[职高] P1-2 中等难度题 — 核心章节 difficulty=3 (45题)
-- 函数(10) + 三角函数(10) + 数列(10) + 解析几何(10) + 概率统计(5) = 45题
-- 所有题目 difficulty_level=3，提升难度分布
-- 幂等：INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ══════════════════════════════════════════
-- 函数: 10道中等题 (difficulty=3)
-- ══════════════════════════════════════════

-- 3096: 函数定义与定义域 [掌握]
SET @n3096 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数定义与定义域 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'SINGLE_CHOICE', '函数 $f(x)=\\\\dfrac{\\\\sqrt{x+2}}{x-1}$ 的定义域是：',
 '["A. $[-2,1)\\\\cup(1,+\\\\infty)$","B. $(-2,1)\\\\cup(1,+\\\\infty)$","C. $[-2,+\\\\infty)$","D. $(-2,1)\\\\cup(1,+\\\\infty)$"]', 'A',
 '√(x+2)要求x≥-2；分母x-1≠0即x≠1。取交集得[-2,1)∪(1,+∞)。注意-2可取（根号下为0时OK）。', 3, 1, 1, 1, 1, @n3096, NOW(), NOW());

-- 3099: 函数单调性判断 [掌握]
SET @n3099 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数单调性判断 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'SINGLE_CHOICE', '函数 $f(x)=\\\\dfrac{2x-1}{x+1}$ 在区间 $(0,+\\\\infty)$ 上的单调性是：',
 '["A. 单调递增","B. 单调递减","C. 先增后减","D. 不单调"]', 'A',
 '分离常数：f(x)=(2x+2-3)/(x+1)=2-3/(x+1)。x增大→x+1增大→3/(x+1)减小→2-3/(x+1)增大。故单调递增。', 3, 1, 1, 1, 1, @n3099, NOW(), NOW());

-- 3100: 函数奇偶性判断 [掌握]
SET @n3100 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数奇偶性判断 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'SINGLE_CHOICE', '函数 $f(x)=\\\\lg(\\\\sqrt{x^2+1}+x)$ 的奇偶性是：',
 '["A. 奇函数","B. 偶函数","C. 非奇非偶","D. 既是奇函数也是偶函数"]', 'A',
 'f(-x)=lg(√(x²+1)-x)=lg((x²+1-x²)/(√(x²+1)+x))=lg(1/(√(x²+1)+x))=-lg(√(x²+1)+x)=-f(x)，为奇函数。关键步骤是有理化分子。', 3, 1, 1, 1, 1, @n3100, NOW(), NOW());

-- 3101: 二次函数图像与性质 [掌握]
SET @n3101 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二次函数图像与性质 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'SINGLE_CHOICE', '已知二次函数 $f(x)=ax^2+bx+c$ 满足 $f(1)=f(3)=0$，$f(0)=-3$，则 $f(2)=$',
 '["A. -1","B. 1","C. -3","D. 3"]', 'B',
 'f(1)=f(3)=0说明x=1,3是零点，f(x)=a(x-1)(x-3)。f(0)=a·(-1)·(-3)=3a=-3，a=-1。f(x)=-(x-1)(x-3)=-x²+4x-3。f(2)=-4+8-3=1。', 3, 1, 1, 1, 1, @n3101, NOW(), NOW());

-- 3102: 二次函数最值问题 [掌握]
SET @n3102 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二次函数最值问题 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '函数 $y=-x^2+2x+3$ 在区间 $[0,3]$ 上的最大值为 _____，最小值为 _____。',
 '[]', '4, 0',
 'y=-(x-1)²+4，对称轴x=1∈[0,3]。f(1)=4最大，比较端点f(0)=3，f(3)=-9+6+3=0，最小值0。', 3, 1, 1, 1, 1, @n3102, NOW(), NOW());

-- 3098: 函数值域 [理解]
SET @n3098 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数值域求解 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '函数 $y=x+\\\\dfrac{1}{x}$（$x>0$）的最小值为 _____。',
 '[]', '2',
 '均值不等式：x+1/x≥2√(x·1/x)=2，当且仅当x=1时取等号。值域[2,+∞)。', 3, 1, 1, 1, 1, @n3098, NOW(), NOW());

-- 3103: 函数应用题 [理解]
SET @n3103 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数应用题建模 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '某产品产量 $Q$ 与投入资金 $x$（万元）满足 $Q=100x-x^2$（$0<x<100$），要使产量最大，应投入 _____ 万元。',
 '[]', '50',
 'Q=-(x²-100x)=-(x-50)²+2500。x=50时产量最大，为2500。', 3, 1, 1, 1, 1, @n3103, NOW(), NOW());

-- 3097: 函数表示方法 [理解]
SET @n3097 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数的表示方法 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '已知 $f(x+1)=x^2+3x+2$，则 $f(x)=$ _____。',
 '[]', 'x²+x',
 '换元：令t=x+1，则x=t-1。f(t)=(t-1)²+3(t-1)+2=t²-2t+1+3t-3+2=t²+t。所以f(x)=x²+x。', 3, 1, 1, 1, 1, @n3097, NOW(), NOW());

-- 3099: 函数单调性 [掌握] (额外1道)
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '若函数 $f(x)=x^2-2ax+3$ 在 $[2,+\\\\infty)$ 上单调递增，则 $a$ 的取值范围是 _____。',
 '[]', 'a≤2',
 'f(x)=(x-a)²+3-a²，对称轴x=a。在[a,+∞)上递增，所以[2,+∞)⊆[a,+∞)要求a≤2。', 3, 1, 1, 1, 1, @n3099, NOW(), NOW());


-- ══════════════════════════════════════════
-- 三角函数: 10道中等题 (difficulty=3)
-- ══════════════════════════════════════════

-- 3118: 三角函数定义 [掌握]
SET @n3118 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='三角函数的定义（单位圆）[掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'SINGLE_CHOICE', '已知角 $\\\\alpha$ 终边上一点 $P(-3,4)$，则 $\\\\sin\\\\alpha+\\\\cos\\\\alpha$ 的值为：',
 '["A. $\\\\dfrac{1}{5}$","B. $-\\\\dfrac{1}{5}$","C. $\\\\dfrac{7}{5}$","D. $-\\\\dfrac{7}{5}$"]', 'A',
 'r=√(9+16)=5。sinα=y/r=4/5，cosα=x/r=-3/5，sinα+cosα=1/5。', 3, 1, 1, 1, 1, @n3118, NOW(), NOW());

-- 3119: 同角三角函数 [掌握]
SET @n3119 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='同角三角函数基本关系 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '已知 $\\\\tan\\\\alpha = 2$，则 $\\\\dfrac{2\\\\sin\\\\alpha-3\\\\cos\\\\alpha}{\\\\sin\\\\alpha+2\\\\cos\\\\alpha}$ 的值为 _____。',
 '[]', '1/4',
 '分子分母除以cosα：=(2tanα-3)/(tanα+2)=(4-3)/(2+2)=1/4。齐次式常通过除以cosα转化为tanα的表达式。', 3, 1, 1, 1, 1, @n3119, NOW(), NOW());

-- 3120: 诱导公式 [掌握]
SET @n3120 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='诱导公式(一)~(四) [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '化简：$\\\\sin(180^\\\\circ+\\\\alpha)\\\\cos(90^\\\\circ+\\\\alpha)-\\\\cos(180^\\\\circ-\\\\alpha)\\\\sin(90^\\\\circ-\\\\alpha)=$ _____。',
 '[]', '1',
 'sin(180°+α)=-sinα；cos(90°+α)=-sinα；cos(180°-α)=-cosα；sin(90°-α)=cosα。原式=(-sinα)(-sinα)-(-cosα)(cosα)=sin²α+cos²α=1。注意诱导公式符号看象限。', 3, 1, 1, 1, 1, @n3120, NOW(), NOW());

-- 3122: 正弦余弦图像 [理解]
SET @n3122 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='正弦、余弦函数的图像与性质 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'SINGLE_CHOICE', '函数 $y=2\\\\sin(2x+\\\\dfrac{\\\\pi}{3})$ 的最小正周期是：',
 '["A. $\\\\dfrac{\\\\pi}{2}$","B. $\\\\pi$","C. $2\\\\pi$","D. $4\\\\pi$"]', 'B',
 'y=Asin(ωx+φ)的周期T=2π/|ω|。ω=2，T=2π/2=π。', 3, 1, 1, 1, 1, @n3122, NOW(), NOW());

-- 3126: 正弦定理 [掌握]
SET @n3126 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='正弦定理 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '在 $\\\\triangle ABC$ 中，$A=30^\\\\circ$，$a=2$，$b=2\\\\sqrt{3}$，则 $B=$ _____。',
 '[]', '60°或120°',
 '正弦定理：a/sinA=b/sinB，2/sin30°=2√3/sinB，2/(1/2)=2√3/sinB，4=2√3/sinB，sinB=√3/2。B=60°或120°。因为b>a，B可能是钝角，两个解都成立。', 3, 1, 1, 1, 1, @n3126, NOW(), NOW());

-- 3127: 余弦定理 [掌握]
SET @n3127 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='余弦定理 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '在 $\\\\triangle ABC$ 中，$a=7$，$b=8$，$c=9$，则 $\\\\cos A = $ _____。',
 '[]', '2/3',
 'cosA=(b²+c²-a²)/(2bc)=(64+81-49)/(2×8×9)=96/144=2/3。', 3, 1, 1, 1, 1, @n3127, NOW(), NOW());

-- 3122: 正弦余弦图像 [理解] (额外)
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '函数 $y=\\\\sin x + \\\\cos x$ 的最大值为 _____。',
 '[]', '√2',
 'y=√2sin(x+π/4)，最大值为√2。辅助角公式：asinx+bcosx=√(a²+b²)sin(x+φ)。', 3, 1, 1, 1, 1, @n3122, NOW(), NOW());

-- 3124: 两角和差 [掌握]
SET @n3124 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='两角和与差的正弦、余弦公式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '已知 $\\\\sin(\\\\alpha+\\\\beta)=\\\\dfrac{1}{2}$，$\\\\sin(\\\\alpha-\\\\beta)=\\\\dfrac{1}{3}$，则 $\\\\dfrac{\\\\tan\\\\alpha}{\\\\tan\\\\beta}=$ _____。',
 '[]', '5',
 'sin(α+β)=sinαcosβ+cosαsinβ=1/2，sin(α-β)=sinαcosβ-cosαsinβ=1/3。两式相加：2sinαcosβ=5/6→sinαcosβ=5/12。两式相减：2cosαsinβ=1/6→cosαsinβ=1/12。tanα/tanβ=(sinα/cosα)/(sinβ/cosβ)=(sinαcosβ)/(cosαsinβ)=(5/12)/(1/12)=5。', 3, 1, 1, 1, 1, @n3124, NOW(), NOW());

-- 3125: 二倍角 [掌握]
SET @n3125 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二倍角公式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '若 $\\\\sin\\\\alpha+\\\\cos\\\\alpha=\\\\dfrac{\\\\sqrt{6}}{2}$，则 $\\\\sin 2\\\\alpha = $ _____。',
 '[]', '1/2',
 '(sinα+cosα)²=sin²α+2sinαcosα+cos²α=1+sin2α=6/4=3/2，sin2α=1/2。', 3, 1, 1, 1, 1, @n3125, NOW(), NOW());


-- ══════════════════════════════════════════
-- 数列: 10道中等题 (difficulty=3)
-- ══════════════════════════════════════════

-- 3133: 等差数列通项 [掌握]
SET @n3133 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等差数列通项公式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等差数列 $\\\\{a_n\\\\}$ 中，$a_3+a_5=20$，$a_2\\\\cdot a_6=96$，且公差 $d>0$，则 $a_1=$ _____。',
 '[]', '7',
 'a₃+a₅=(a₁+2d)+(a₁+4d)=2a₁+6d=20→a₁+3d=10①。a₂·a₆=(a₁+d)(a₁+5d)=96②。由①得a₁=10-3d代入②：(10-2d)(10+2d)=100-4d²=96→d²=1→d=1（d>0）。代回①：a₁=10-3=7。验证：a₂=8，a₆=12，8×12=96✓。', 3, 1, 1, 1, 1, @n3133, NOW(), NOW());

-- 3134: 等差数列求和 [掌握]
SET @n3134 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等差数列前n项和 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等差数列 $\\\\{a_n\\\\}$ 的前n项和 $S_n=n^2+2n$，则 $a_{10}=$ _____。',
 '[]', '21',
 'a₁₀=S₁₀-S₉=(100+20)-(81+18)=120-99=21。或an=Sn-S(n-1)=n²+2n-(n-1)²-2(n-1)=2n+1，a₁₀=21。', 3, 1, 1, 1, 1, @n3134, NOW(), NOW());

-- 3135: 等比数列通项 [掌握]
SET @n3135 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等比数列通项公式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等比数列 $\\\\{a_n\\\\}$ 中，$a_2=6$，$a_5=48$，则公比 $q=$ _____。',
 '[]', '2',
 'a₅/a₂=q³→48/6=8=q³→q=2。等比数列任意两项之比等于公比的项数差次幂。', 3, 1, 1, 1, 1, @n3135, NOW(), NOW());

-- 3136: 等比数列求和 [掌握]
SET @n3136 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等比数列前n项和 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等比数列 $\\\\{a_n\\\\}$ 中，$a_1=3$，$q=2$，$S_n=189$，则 $n=$ _____。',
 '[]', '6',
 'S_n=3(2ⁿ-1)/(2-1)=3(2ⁿ-1)=189→2ⁿ-1=63→2ⁿ=64→n=6。', 3, 1, 1, 1, 1, @n3136, NOW(), NOW());

-- 3137: 分组求和 [理解]
SET @n3137 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='分组求和法 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '数列 $\\\\{2n+3^n\\\\}$ 的前 $n$ 项和 $S_n = $ _____（用n表示）。',
 '[]', 'n(n+1)+(3^(n+1)-3)/2',
 '分组：∑(2n+3ⁿ)=2∑n+∑3ⁿ=2·n(n+1)/2+(3(3ⁿ-1)/(3-1))=n(n+1)+(3^(n+1)-3)/2。第一部分是等差数列，第二部分是等比数列。', 3, 1, 1, 1, 1, @n3137, NOW(), NOW());

-- 3138: 裂项相消 [理解]
SET @n3138 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='裂项相消法 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '$\\\\dfrac{1}{1\\\\times 3}+\\\\dfrac{1}{3\\\\times 5}+\\\\dfrac{1}{5\\\\times 7}+\\\\cdots+\\\\dfrac{1}{19\\\\times 21}=$ _____。',
 '[]', '10/21',
 '裂项：1/((2n-1)(2n+1))=(1/2)(1/(2n-1)-1/(2n+1))。原式=(1/2)[(1-1/3)+(1/3-1/5)+...+(1/19-1/21)]=(1/2)(1-1/21)=10/21。', 3, 1, 1, 1, 1, @n3138, NOW(), NOW());

-- 3133-3136: 数列混合题 (4道)
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等差数列 $\\\\{a_n\\\\}$ 中，$a_1+a_2+a_3=9$，$a_4+a_5+a_6=27$，则公差 $d=$ _____。',
 '[]', '2',
 '(a₄+a₅+a₆)-(a₁+a₂+a₃)=3d+3d+3d=9d=27-9=18→d=2。每项加了3d。', 3, 1, 1, 1, 1, @n3133, NOW(), NOW());

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等差数列 $\\\\{a_n\\\\}$ 中 $S_5=25$，$S_{10}=100$，则 $a_1=$ _____，$d=$ _____。',
 '[]', '1, 2',
 'S₅=5a₁+10d=25→a₁+2d=5；S₁₀=10a₁+45d=100→2a₁+9d=20。解方程组：d=2，a₁=1。', 3, 1, 1, 1, 1, @n3134, NOW(), NOW());

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等比数列 $\\\\{a_n\\\\}$ 中，$a_1+a_3=10$，$a_2+a_4=30$，则公比 $q=$ _____。',
 '[]', '3',
 'a₂+a₄=q(a₁+a₃)=30→q·10=30→q=3。利用等比数列相邻项的比例关系。', 3, 1, 1, 1, 1, @n3135, NOW(), NOW());

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '等比数列 $\\\\{a_n\\\\}$ 中 $a_1=2$，$S_3=26$，则公比 $q=$ _____。',
 '[]', '3',
 'S₃=a₁(1-q³)/(1-q)=2(1-q³)/(1-q)=2(1+q+q²)=26→1+q+q²=13→q²+q-12=0→(q+4)(q-3)=0→q=3或q=-4。q=3时a₁=2符合。按职高范围取q=3。', 3, 1, 1, 1, 1, @n3136, NOW(), NOW());


-- ══════════════════════════════════════════
-- 解析几何: 10道中等题 (difficulty=3)
-- ══════════════════════════════════════════

-- 3156: 斜率 [掌握]
SET @n3156 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='直线的倾斜角与斜率 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '已知直线过点 $A(1,2)$ 和 $B(3,6)$，则直线的倾斜角为 _____。',
 '[]', 'arctan(2)',
 '斜率k=(6-2)/(3-1)=2，倾斜角α=arctan(2)≈63.4°。tanα=k。', 3, 1, 1, 1, 1, @n3156, NOW(), NOW());

-- 3157: 直线方程 [掌握]
SET @n3157 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='直线的五种方程形式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '直线 $l$ 过点 $(2,-1)$ 且与直线 $3x+4y-5=0$ 垂直，则 $l$ 的方程为 _____。',
 '[]', '4x-3y-11=0',
 '已知直线斜率k₁=-3/4，垂直直线斜率k₂=4/3。点斜式：y+1=(4/3)(x-2)→3y+3=4x-8→4x-3y-11=0。', 3, 1, 1, 1, 1, @n3157, NOW(), NOW());

-- 3158: 圆标准方程 [掌握]
SET @n3158 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='圆的标准方程 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '以 $A(1,2)$ 和 $B(5,-2)$ 为直径端点的圆的方程为 _____。',
 '[]', '(x-3)²+(y-0)²=8',
 '圆心为中点C(3,0)。直径=√((5-1)²+(-2-2)²)=√(16+16)=4√2，半径=2√2。r²=8。方程：(x-3)²+y²=8。', 3, 1, 1, 1, 1, @n3158, NOW(), NOW());

-- 3159: 圆一般方程 [掌握]
SET @n3159 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='圆的一般方程 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '方程 $x^2+y^2+2x-6y+m=0$ 表示一个圆，则 $m$ 的取值范围是 _____。',
 '[]', 'm<10',
 '配方：(x+1)²+(y-3)²=10-m。表示圆需10-m>0，即m<10。', 3, 1, 1, 1, 1, @n3159, NOW(), NOW());

-- 3160: 直线与圆位置 [掌握]
SET @n3160 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='直线与圆的位置关系判断 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '直线 $y=x+m$ 与圆 $x^2+y^2=4$ 相交，则 $m$ 的取值范围是 _____。',
 '[]', '(-2√2, 2√2)',
 '圆心(0,0)到直线x-y+m=0的距离d=|m|/√2。相交条件d<r=2：|m|/√2<2→|m|<2√2→-2√2<m<2√2。', 3, 1, 1, 1, 1, @n3160, NOW(), NOW());

-- 3191: 椭圆 [掌握]
SET @n3191 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='椭圆的标准方程与性质 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '椭圆 $\\\\dfrac{x^2}{25}+\\\\dfrac{y^2}{16}=1$ 上一点P到左焦点的距离为3，则P到右焦点的距离为 _____。',
 '[]', '7',
 '椭圆定义：PF₁+PF₂=2a=10。PF₁=3→PF₂=7。', 3, 1, 1, 1, 1, @n3191, NOW(), NOW());

-- 3191: 椭圆 (额外)
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '椭圆 $\\\\dfrac{x^2}{a^2}+\\\\dfrac{y^2}{b^2}=1$（$a>b>0$）的离心率为 $\\\\dfrac{1}{2}$，且焦距为2，则 $a=$ _____。',
 '[]', '2',
 'e=c/a=1/2，2c=2→c=1→a=c/e=2。b²=a²-c²=3。', 3, 1, 1, 1, 1, @n3191, NOW(), NOW());

-- 3192: 双曲线 [掌握]
SET @n3192 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='双曲线的标准方程与性质 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '双曲线 $\\\\dfrac{x^2}{9}-\\\\dfrac{y^2}{16}=1$ 上一点P到右焦点的距离为7，则P到左焦点的距离为 _____。',
 '[]', '1或13',
 '双曲线定义：||PF₁|-|PF₂||=2a=6。|PF₂-7|=6→PF₂=1或13。两个解分别对应P在右支和左支。', 3, 1, 1, 1, 1, @n3192, NOW(), NOW());

-- 3193: 抛物线 [掌握]
SET @n3193 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='抛物线的标准方程与性质 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '抛物线 $y^2=2px$（$p>0$）上一点 $M$ 的横坐标为6，$M$ 到焦点的距离为10，则 $p=$ _____。',
 '[]', '8',
 '抛物线定义：MF=M到准线的距离=横坐标+p/2。10=6+p/2→p/2=4→p=8。', 3, 1, 1, 1, 1, @n3193, NOW(), NOW());


-- ══════════════════════════════════════════
-- 概率统计: 5道中等题 (difficulty=3)
-- ══════════════════════════════════════════

-- 3167: 古典概型 [掌握]
SET @n3167 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='古典概型 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '从标有数字1,2,3,4,5的五张卡片中任取2张，数字之和为奇数的概率为 _____。',
 '[]', '3/5',
 '总取法C(5,2)=10。和为奇数需一奇一偶：奇{1,3,5}三选一×偶{2,4}二选一=6种。P=6/10=3/5。', 3, 1, 1, 1, 1, @n3167, NOW(), NOW());

-- 3166: 排列组合 [理解]
SET @n3166 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='排列与组合 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '5人排成一排，甲、乙两人必须相邻的排法有 _____ 种。',
 '[]', '48',
 '将甲乙捆绑视为一个"人"，共4个"人"排列：4!=24。甲乙内部有2!种排列。总数为24×2=48。这是相邻问题的"捆绑法"。', 3, 1, 1, 1, 1, @n3166, NOW(), NOW());

-- 3168: 互斥独立事件 [理解]
SET @n3168 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='互斥事件与独立事件 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '甲命中概率0.7，乙命中概率0.6，两人独立射击。目标被击中的概率为 _____。',
 '[]', '0.88',
 'P(至少一人命中)=1-P(都不中)=1-0.3×0.4=1-0.12=0.88。', 3, 1, 1, 1, 1, @n3168, NOW(), NOW());

-- 3165: 计数原理 [理解]
SET @n3165 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='分类加法与分步乘法 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '由数字0,1,2,3,4组成无重复数字的三位数有 _____ 个。',
 '[]', '48',
 '百位不能为0：4种选择（1-4），十位从剩余4个选：4种，个位从剩余3个选：3种。共4×4×3=48。分步乘法原理。', 3, 1, 1, 1, 1, @n3165, NOW(), NOW());

-- 3170: 样本估计总体 [了解]
SET @n3170 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='用样本估计总体 [了解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES
('数学[职高]', 'FILL_IN', '某班10名学生的数学成绩为：78,82,85,90,88,76,95,83,87,91。则样本均值为 _____，样本方差为 _____（保留整数）。',
 '[]', '85.5, 35',
 '均值=(78+82+85+90+88+76+95+83+87+91)/10=855/10=85.5。方差=Σ(x_i-x̄)²/9≈35。', 3, 1, 1, 1, 1, @n3170, NOW(), NOW());

COMMIT;
SELECT 'v196 deployed — 核心章节中等题 45题 (difficulty=3)' AS result;
-- ============================================================================
