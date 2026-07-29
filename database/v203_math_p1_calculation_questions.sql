-- ============================================================================
-- v203: 数学[职高] P1 — CALCULATION 解答题种子（对标四川省对口升学高考）
-- 覆盖: 函数(8) + 指数对数(6) + 三角函数(7) + 数列(6) + 解析几何(6)
--       + 不等式(4) + 集合(2) + 概率统计(2) + 平面向量(3) = 44 题
-- 每道题均有详细分步解析（四川省中职数学教学规范）
-- 幂等: INSERT IGNORE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ══════════════════════════════════════════
-- 一、函数 (8题)
-- ══════════════════════════════════════════

SET @n3096 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数定义与定义域 [掌握]' AND level=4 LIMIT 1);
SET @n3098 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数值域求解 [理解]' AND level=4 LIMIT 1);
SET @n3099 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数单调性判断 [掌握]' AND level=4 LIMIT 1);
SET @n3100 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数奇偶性判断 [掌握]' AND level=4 LIMIT 1);
SET @n3101 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二次函数图像与性质 [掌握]' AND level=4 LIMIT 1);
SET @n3102 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二次函数最值问题 [掌握]' AND level=4 LIMIT 1);
SET @n3103 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数应用题建模 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3096: 函数定义与定义域 (easy)
('数学[职高]', 'CALCULATION', '求函数 $f(x)=\\sqrt{2x-6}+\\dfrac{1}{x-5}$ 的定义域。',
 '[]', '$\\{x \\mid x \\geq 3 \\text{ 且 } x \\neq 5\\}$',
 '【解】1. 根号下非负: $2x-6 \\geq 0$，解得 $x \\geq 3$\n2. 分母不为零: $x-5 \\neq 0$，解得 $x \\neq 5$\n3. 取交集得定义域: $\\{x \\mid x \\geq 3 \\text{ 且 } x \\neq 5\\}$',
 1, 1, 1, 1, 1, @n3096, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知 $f(x)=\\begin{cases} x^2+1, & x \\leq 0 \\\\ 2x-3, & x > 0 \\end{cases}$，求 $f(-1)+f(2)$ 的值。',
 '[]', '3',
 '【解】分段函数分别代入：\n1. $-1 \\leq 0$，用第一段: $f(-1)=(-1)^2+1=1+1=2$\n2. $2 > 0$，用第二段: $f(2)=2 \\times 2-3=4-3=1$\n3. $f(-1)+f(2)=2+1=3$',
 1, 1, 1, 1, 1, @n3096, NOW(), NOW()),

-- 3098: 函数值域求解 (medium)
('数学[职高]', 'CALCULATION', '求函数 $y=x^2-4x+6$（$x \\in [1,4]$）的值域。',
 '[]', '$[2,6]$',
 '【解】二次函数求指定区间上的值域：\n1. 配方: $y=(x^2-4x+4)+2=(x-2)^2+2$\n2. 对称轴 $x=2$，在区间 $[1,4]$ 内\n3. $x=2$ 时取最小值 $y_{\\min}=0+2=2$\n4. 比较端点: $x=1$ 时 $y=1-4+6=3$；$x=4$ 时 $y=16-16+6=6$\n5. 值域: $[2,6]$',
 2, 1, 1, 1, 1, @n3098, NOW(), NOW()),

-- 3099: 函数单调性判断 (medium)
('数学[职高]', 'CALCULATION', '判断函数 $f(x)=\\dfrac{2x+1}{x-1}$ 在区间 $(1,+\\infty)$ 上的单调性，并用定义证明。',
 '[]', '单调递减',
 '【解】用定义法证明：\n1. 设 $x_1>x_2>1$\n2. $f(x_1)-f(x_2)=\\dfrac{2x_1+1}{x_1-1}-\\dfrac{2x_2+1}{x_2-1}$\n3. 通分后分子化简为 $-3(x_1-x_2)$\n   分母 $(x_1-1)(x_2-1)>0$（因为 $x_1,x_2>1$）\n4. $x_1>x_2$，故 $x_1-x_2>0$，分子 $<0$\n5. $f(x_1)-f(x_2)<0$，即 $f(x_1)<f(x_2)$\n   所以 $f(x)$ 在 $(1,+\\infty)$ 上单调递减。',
 2, 1, 1, 1, 1, @n3099, NOW(), NOW()),

-- 3100: 函数奇偶性判断 (easy)
('数学[职高]', 'CALCULATION', '判断函数 $f(x)=x^3-2x$ 的奇偶性，并计算 $f(2)+f(-2)$ 的值。',
 '[]', '奇函数，$f(2)+f(-2)=0$',
 '【解】1. 定义域 $\\mathbb{R}$，关于原点对称\n2. $f(-x)=(-x)^3-2(-x)=-x^3+2x=-(x^3-2x)=-f(x)$\n3. 所以 $f(x)$ 是奇函数\n4. 由奇函数性质: $f(-x)=-f(x)$\n5. $f(2)+f(-2)=f(2)+(-f(2))=0$',
 1, 1, 1, 1, 1, @n3100, NOW(), NOW()),

-- 3101: 二次函数图像与性质 (medium)
('数学[职高]', 'CALCULATION', '已知二次函数的图像过点 $(0,3)$，顶点坐标为 $(2,-1)$，求该二次函数的解析式。',
 '[]', '$f(x)=x^2-4x+3$',
 '【解】设顶点式 $f(x)=a(x-2)^2-1$:\n1. 图像过 $(0,3)$: $f(0)=a(0-2)^2-1=4a-1=3$\n2. 解得 $a=1$\n3. 展开: $f(x)=(x-2)^2-1=x^2-4x+4-1=x^2-4x+3$\n4. 验证: $f(2)=4-8+3=-1$ ✓',
 2, 1, 1, 1, 1, @n3101, NOW(), NOW()),

-- 3102: 二次函数最值问题 (medium)
('数学[职高]', 'CALCULATION', '用总长为 40m 的篱笆围一个矩形场地，一边靠墙（墙足够长），求矩形场地的最大面积。',
 '[]', '最大面积 $200\\text{m}^2$，宽 $10\\text{m}$，长 $20\\text{m}$',
 '【解】设垂直于墙的边长为 $x$，则平行边长为 $40-2x$:\n1. 面积 $S=x(40-2x)=-2x^2+40x$\n2. 配方: $S=-2(x^2-20x)=-2[(x-10)^2-100]=-2(x-10)^2+200$\n3. 当 $x=10$ 时 $S_{\\max}=200$\n4. 此时长边 $=40-2\\times10=20$\n5. 答: 最大面积 $200\\text{m}^2$，宽 $10\\text{m}$，长 $20\\text{m}$',
 2, 1, 1, 1, 1, @n3102, NOW(), NOW()),

-- 3103: 函数应用题建模 (hard)
('数学[职高]', 'CALCULATION', '某商品进价每件 40 元，售价每件 60 元时月销量 200 件。售价每涨 1 元，月销量减少 5 件。问定价多少元时月利润最大？最大利润是多少？',
 '[]', '定价 70 元时利润最大，最大利润 4050 元',
 '【解】设涨价 $x$ 元:\n1. 售价 $=60+x$，销量 $=200-5x$\n2. 利润 $y=(60+x-40)(200-5x)=(20+x)(200-5x)$\n3. 展开: $y=4000-100x+200x-5x^2=-5x^2+100x+4000$\n4. 配方: $y=-5(x^2-20x)+4000=-5(x-10)^2+4050$\n5. 当 $x=10$ 时 $y_{\\max}=4050$，此时定价 $=70$ 元\n6. 答: 定价 70 元，最大月利润 4050 元。',
 3, 1, 1, 1, 1, @n3103, NOW(), NOW());

-- ══════════════════════════════════════════
-- 二、指数与对数函数 (6题)
-- ══════════════════════════════════════════

SET @n3106 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='指数运算性质 [掌握]' AND level=4 LIMIT 1);
SET @n3107 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='指数函数的图像与性质 [理解]' AND level=4 LIMIT 1);
SET @n3108 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='对数运算性质 [掌握]' AND level=4 LIMIT 1);
SET @n3109 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='对数函数的图像与性质 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3106: 指数运算性质 (medium)
('数学[职高]', 'CALCULATION', '计算：$(0.008)^{-\\frac{1}{3}}+(\\sqrt{3}-1)^0+\\lg 1-\\log_3 27$。',
 '[]', '3',
 '【解】逐项计算：\n1. $0.008=\\dfrac{8}{1000}=\\dfrac{1}{125}$，$(\\dfrac{1}{125})^{-1/3}=125^{1/3}=5$\n2. $(\\sqrt{3}-1)^0=1$（任何非零数的 0 次幂为 1）\n3. $\\lg 1=0$\n4. $\\log_3 27=\\log_3 3^3=3$\n5. 原式 $=5+1+0-3=3$',
 2, 1, 1, 1, 1, @n3106, NOW(), NOW()),

-- 3108: 对数运算性质 (medium)
('数学[职高]', 'CALCULATION', '计算：$\\log_2 32+\\lg 0.01+\\ln e^3-\\log_3 \\dfrac{1}{9}$。',
 '[]', '8',
 '【解】逐项计算：\n1. $\\log_2 32=\\log_2 2^5=5$\n2. $\\lg 0.01=\\lg 10^{-2}=-2$\n3. $\\ln e^3=3$\n4. $\\log_3 \\dfrac{1}{9}=\\log_3 3^{-2}=-2$\n5. 原式 $=5+(-2)+3-(-2)=5-2+3+2=8$',
 2, 1, 1, 1, 1, @n3108, NOW(), NOW()),

-- 3107: 指数函数 (medium)
('数学[职高]', 'CALCULATION', '解方程：$2^{x+1}=8 \\cdot 4^{x-2}$。',
 '[]', '$x=2$',
 '【解】化为同底数幂（以 2 为底）：\n1. 左边 $=2^{x+1}$\n2. 右边 $=2^3 \\cdot (2^2)^{x-2}=2^3 \\cdot 2^{2x-4}=2^{2x-1}$\n3. 由 $2^{x+1}=2^{2x-1}$ 得 $x+1=2x-1$\n4. 解得 $x=2$\n5. 检验：左边 $2^3=8$，右边 $8\\cdot4^0=8$ ✓',
 2, 1, 1, 1, 1, @n3107, NOW(), NOW()),

-- 3107: 指数不等式 (medium)
('数学[职高]', 'CALCULATION', '解不等式：$\\left(\\dfrac{1}{2}\\right)^{2x-1} > 8$。',
 '[]', '$x < -1$',
 '【解】1. $8=2^3=(\\dfrac{1}{2})^{-3}$\n2. 原不等式: $(\\dfrac{1}{2})^{2x-1} > (\\dfrac{1}{2})^{-3}$\n3. 底数 $0<\\dfrac{1}{2}<1$，指数函数递减\n   不等式方向反转: $2x-1 < -3$\n4. $2x < -2$，$x < -1$\n5. 解集: $(-\\infty,-1)$',
 2, 1, 1, 1, 1, @n3107, NOW(), NOW()),

-- 3109: 对数函数 (medium)
('数学[职高]', 'CALCULATION', '解不等式：$\\log_{\\frac{1}{2}}(x-1) > -1$。',
 '[]', '$1 < x < 3$',
 '【解】1. 对数有意义: $x-1>0$，即 $x>1$\n2. $-1=\\log_{1/2}(1/2)^{-1}=\\log_{1/2}2$\n3. 原不等式: $\\log_{1/2}(x-1) > \\log_{1/2}2$\n4. 底数 $0<\\dfrac{1}{2}<1$，对数函数递减\n   所以 $x-1 < 2$，得 $x < 3$\n5. 结合 $x>1$，解集: $(1,3)$',
 2, 1, 1, 1, 1, @n3109, NOW(), NOW()),

-- 3109: 对数方程 (medium)
('数学[职高]', 'CALCULATION', '解方程：$\\lg x+\\lg(x-3)=1$。',
 '[]', '$x=5$',
 '【解】1. 真数条件: $x>0$ 且 $x-3>0$，得 $x>3$\n2. 利用对数加法: $\\lg x+\\lg(x-3)=\\lg[x(x-3)]=1$\n3. $\\lg[x(x-3)]=1$ 即 $x(x-3)=10^1=10$\n4. $x^2-3x-10=0$，$(x-5)(x+2)=0$\n5. $x=5$ 或 $x=-2$（舍去，不满足 $x>3$）\n6. 答: $x=5$',
 2, 1, 1, 1, 1, @n3109, NOW(), NOW());

-- ══════════════════════════════════════════
-- 三、三角函数 (7题)
-- ══════════════════════════════════════════

SET @n3119 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='同角三角函数基本关系 [掌握]' AND level=4 LIMIT 1);
SET @n3120 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='诱导公式(一)~(四) [掌握]' AND level=4 LIMIT 1);
SET @n3124 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='两角和与差的正弦、余弦公式 [掌握]' AND level=4 LIMIT 1);
SET @n3125 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二倍角公式 [掌握]' AND level=4 LIMIT 1);
SET @n3126 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='正弦定理 [掌握]' AND level=4 LIMIT 1);
SET @n3127 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='余弦定理 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3119: 同角关系 (easy)
('数学[职高]', 'CALCULATION', '已知 $\\sin\\alpha=\\dfrac{3}{5}$，且 $\\alpha$ 是第二象限角，求 $\\cos\\alpha$ 和 $\\tan\\alpha$ 的值。',
 '[]', '$\\cos\\alpha=-\\dfrac{4}{5}$，$\\tan\\alpha=-\\dfrac{3}{4}$',
 '【解】利用同角关系 $\\sin^2\\alpha+\\cos^2\\alpha=1$：\n1. $\\cos^2\\alpha=1-\\sin^2\\alpha=1-\\dfrac{9}{25}=\\dfrac{16}{25}$\n2. $\\alpha$ 在第二象限，$\\cos\\alpha<0$，$\\cos\\alpha=-\\dfrac{4}{5}$\n3. $\\tan\\alpha=\\dfrac{\\sin\\alpha}{\\cos\\alpha}=\\dfrac{3/5}{-4/5}=-\\dfrac{3}{4}$',
 1, 1, 1, 1, 1, @n3119, NOW(), NOW()),

-- 3120: 诱导公式 (easy)
('数学[职高]', 'CALCULATION', '计算：$\\sin 150^\\circ+\\cos 240^\\circ-\\tan 315^\\circ$。',
 '[]', '1',
 '【解】用诱导公式化简：\n1. $\\sin 150^\\circ=\\sin(180^\\circ-30^\\circ)=\\sin 30^\\circ=\\dfrac{1}{2}$\n2. $\\cos 240^\\circ=\\cos(180^\\circ+60^\\circ)=-\\cos 60^\\circ=-\\dfrac{1}{2}$\n3. $\\tan 315^\\circ=\\tan(360^\\circ-45^\\circ)=-\\tan 45^\\circ=-1$\n4. 原式 $=\\dfrac{1}{2}+(-\\dfrac{1}{2})-(-1)=0+1=1$',
 1, 1, 1, 1, 1, @n3120, NOW(), NOW()),

-- 3124: 和差公式 (medium)
('数学[职高]', 'CALCULATION', '已知 $\\sin\\alpha=\\dfrac{4}{5}$（$\\alpha$ 为锐角），$\\cos\\beta=-\\dfrac{12}{13}$（$\\beta$ 为钝角），求 $\\sin(\\alpha+\\beta)$ 的值。',
 '[]', '$-\\dfrac{33}{65}$',
 '【解】1. $\\alpha$ 为锐角: $\\cos\\alpha=\\sqrt{1-\\frac{16}{25}}=\\dfrac{3}{5}$\n2. $\\beta$ 为钝角: $\\sin\\beta>0$，$\\sin\\beta=\\sqrt{1-\\frac{144}{169}}=\\dfrac{5}{13}$\n3. $\\sin(\\alpha+\\beta)=\\sin\\alpha\\cos\\beta+\\cos\\alpha\\sin\\beta$\n   $=\\dfrac{4}{5}\\cdot(-\\dfrac{12}{13})+\\dfrac{3}{5}\\cdot\\dfrac{5}{13}=-\\dfrac{48}{65}+\\dfrac{15}{65}=-\\dfrac{33}{65}$',
 2, 1, 1, 1, 1, @n3124, NOW(), NOW()),

-- 3125: 二倍角公式 (easy)
('数学[职高]', 'CALCULATION', '已知 $\\sin\\alpha=\\dfrac{\\sqrt{3}}{2}$（$\\alpha$ 为锐角），求 $\\sin 2\\alpha$ 和 $\\cos 2\\alpha$ 的值。',
 '[]', '$\\sin 2\\alpha=\\dfrac{\\sqrt{3}}{2}$，$\\cos 2\\alpha=-\\dfrac{1}{2}$',
 '【解】1. $\\alpha$ 为锐角，$\\sin\\alpha=\\frac{\\sqrt{3}}{2}$，故 $\\alpha=60^\\circ$\n2. $\\cos\\alpha=\\sqrt{1-\\sin^2\\alpha}=\\dfrac{1}{2}$\n3. $\\sin 2\\alpha=2\\sin\\alpha\\cos\\alpha=2\\cdot\\dfrac{\\sqrt{3}}{2}\\cdot\\dfrac{1}{2}=\\dfrac{\\sqrt{3}}{2}$\n4. $\\cos 2\\alpha=\\cos^2\\alpha-\\sin^2\\alpha=\\dfrac{1}{4}-\\dfrac{3}{4}=-\\dfrac{1}{2}$',
 1, 1, 1, 1, 1, @n3125, NOW(), NOW()),

-- 3126: 正弦定理 (hard)
('数学[职高]', 'CALCULATION', '在 $\\triangle ABC$ 中，已知 $a=2$，$b=\\sqrt{6}$，$A=45^\\circ$，求角 $B$。',
 '[]', '$B=60^\\circ$ 或 $B=120^\\circ$',
 '【解】由正弦定理 $\\dfrac{a}{\\sin A}=\\dfrac{b}{\\sin B}$:\n1. $\\dfrac{2}{\\sin 45^\\circ}=\\dfrac{\\sqrt{6}}{\\sin B}$\n2. $\\sin B=\\dfrac{\\sqrt{6}\\cdot\\frac{\\sqrt{2}}{2}}{2}=\\dfrac{\\sqrt{12}}{4}=\\dfrac{\\sqrt{3}}{2}$\n3. $\\sin B=\\dfrac{\\sqrt{3}}{2}$，且 $b>a$，$B>A=45^\\circ$\n4. $B=60^\\circ$ 或 $B=120^\\circ$（均在 $(0^\\circ,180^\\circ)$ 内）\n5. 验证 $A+B<180^\\circ$ 均满足，两解均合法。',
 3, 1, 1, 1, 1, @n3126, NOW(), NOW()),

-- 3127: 余弦定理 (medium)
('数学[职高]', 'CALCULATION', '在 $\\triangle ABC$ 中，已知 $a=3$，$b=4$，$\\angle C=60^\\circ$，求边 $c$ 和 $\\triangle ABC$ 的面积。',
 '[]', '$c=\\sqrt{13}$，面积 $=3\\sqrt{3}$',
 '【解】1. 由余弦定理: $c^2=a^2+b^2-2ab\\cos C$\n   $=9+16-2\\times3\\times4\\times\\cos 60^\\circ$\n   $=25-24\\times\\dfrac{1}{2}=25-12=13$\n2. $c=\\sqrt{13}$\n3. 面积 $S=\\dfrac{1}{2}ab\\sin C=\\dfrac{1}{2}\\times3\\times4\\times\\dfrac{\\sqrt{3}}{2}=3\\sqrt{3}$',
 2, 1, 1, 1, 1, @n3127, NOW(), NOW()),

-- 3127: 余弦定理求角 (medium)
('数学[职高]', 'CALCULATION', '在 $\\triangle ABC$ 中，已知 $a=7$，$b=5$，$c=3$，求最大角的度数。',
 '[]', '$A=120^\\circ$',
 '【解】1. 最大边 $a=7$ 对最大角 $A$\n2. 由余弦定理: $\\cos A=\\dfrac{b^2+c^2-a^2}{2bc}$\n   $=\\dfrac{25+9-49}{2\\times5\\times3}=\\dfrac{-15}{30}=-\\dfrac{1}{2}$\n3. $\\cos A=-\\dfrac{1}{2}$，$A=120^\\circ$\n4. 验证: $A+B+C=180^\\circ$，钝角三角形，最大角为钝角 ✓',
 2, 1, 1, 1, 1, @n3127, NOW(), NOW());

-- ══════════════════════════════════════════
-- 四、数列 (6题)
-- ══════════════════════════════════════════

SET @n3133 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等差数列通项公式 [掌握]' AND level=4 LIMIT 1);
SET @n3134 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等差数列前n项和 [掌握]' AND level=4 LIMIT 1);
SET @n3135 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等比数列通项公式 [掌握]' AND level=4 LIMIT 1);
SET @n3136 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='等比数列前n项和 [掌握]' AND level=4 LIMIT 1);
SET @n3137 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='分组求和法 [理解]' AND level=4 LIMIT 1);
SET @n3138 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='裂项相消法 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3133: 等差数列通项 (easy)
('数学[职高]', 'CALCULATION', '在等差数列 $\\{a_n\\}$ 中，已知 $a_3=7$，$a_7=19$，求首项 $a_1$、公差 $d$ 和通项公式。',
 '[]', '$a_1=1$，$d=3$，$a_n=3n-2$',
 '【解】1. 通项 $a_n=a_1+(n-1)d$\n2. $a_3=a_1+2d=7$ ①\n3. $a_7=a_1+6d=19$ ②\n4. ②-①: $4d=12$，$d=3$\n5. 代入①: $a_1+6=7$，$a_1=1$\n6. 通项: $a_n=1+(n-1)\\times3=3n-2$',
 1, 1, 1, 1, 1, @n3133, NOW(), NOW()),

-- 3134: 等差数列求和 (medium)
('数学[职高]', 'CALCULATION', '在等差数列 $\\{a_n\\}$ 中，$a_1=3$，$d=2$，前 $n$ 项和 $S_n=120$，求项数 $n$。',
 '[]', '$n=10$',
 '【解】$S_n=na_1+\\dfrac{n(n-1)}{2}d$\n1. 代入: $120=3n+\\dfrac{n(n-1)}{2}\\times2$\n2. $120=3n+n(n-1)=3n+n^2-n=n^2+2n$\n3. $n^2+2n-120=0$\n4. $(n+12)(n-10)=0$\n5. $n=10$（$n=-12$ 舍去）',
 2, 1, 1, 1, 1, @n3134, NOW(), NOW()),

-- 3135: 等比数列通项 (easy)
('数学[职高]', 'CALCULATION', '在等比数列 $\\{a_n\\}$ 中，已知 $a_2=6$，$a_5=162$，求首项 $a_1$ 和公比 $q$。',
 '[]', '$a_1=2$，$q=3$',
 '【解】1. 通项 $a_n=a_1q^{n-1}$\n2. $a_2=a_1q=6$ ①\n3. $a_5=a_1q^4=162$ ②\n4. ②÷①: $q^3=\\dfrac{162}{6}=27$\n5. $q=3$，代入①: $a_1\\times3=6$，$a_1=2$',
 1, 1, 1, 1, 1, @n3135, NOW(), NOW()),

-- 3136: 等比数列求和 (easy)
('数学[职高]', 'CALCULATION', '在等比数列 $\\{a_n\\}$ 中，$a_1=1$，$q=\\dfrac{1}{2}$，求前 6 项和 $S_6$。',
 '[]', '$\\dfrac{63}{32}$',
 '【解】$S_n=\\dfrac{a_1(1-q^n)}{1-q}$（$q\\neq 1$）\n1. 代入: $S_6=\\dfrac{1\\times[1-(\\frac{1}{2})^6]}{1-\\frac{1}{2}}$\n2. $(\\frac{1}{2})^6=\\dfrac{1}{64}$\n3. $S_6=\\dfrac{1-\\frac{1}{64}}{\\frac{1}{2}}=2\\times\\dfrac{63}{64}=\\dfrac{63}{32}$',
 1, 1, 1, 1, 1, @n3136, NOW(), NOW()),

-- 3137: 分组求和 (medium)
('数学[职高]', 'CALCULATION', '求数列 $\\{2n+3^n\\}$ 的前 5 项和。',
 '[]', '393',
 '【解】$a_n=2n+3^n$，分成等差部分和等比部分:\n1. $\\sum_{n=1}^{5} 2n = 2\\times\\dfrac{5\\times6}{2} = 30$\n2. $\\sum_{n=1}^{5} 3^n = 3+9+27+81+243 = 363$\n   （或 $\\dfrac{3(1-3^5)}{1-3}=\\dfrac{3\\times(-242)}{-2}=363$）\n3. $S_5 = 30+363 = 393$',
 2, 1, 1, 1, 1, @n3137, NOW(), NOW()),

-- 3138: 裂项相消 (medium)
('数学[职高]', 'CALCULATION', '求数列 $\\{\\dfrac{1}{n(n+1)}\\}$ 的前 $n$ 项和 $S_n$。',
 '[]', '$S_n=\\dfrac{n}{n+1}$',
 '【解】1. 裂项: $\\dfrac{1}{n(n+1)}=\\dfrac{1}{n}-\\dfrac{1}{n+1}$\n2. $S_n=(1-\\dfrac{1}{2})+(\\dfrac{1}{2}-\\dfrac{1}{3})+\\cdots+(\\dfrac{1}{n}-\\dfrac{1}{n+1})$\n3. 中间项全部抵消，剩下 $1-\\dfrac{1}{n+1}$\n4. $S_n=\\dfrac{n+1-1}{n+1}=\\dfrac{n}{n+1}$',
 2, 1, 1, 1, 1, @n3138, NOW(), NOW());

-- ══════════════════════════════════════════
-- 五、解析几何 (6题)
-- ══════════════════════════════════════════

SET @n3156 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='直线的倾斜角与斜率 [掌握]' AND level=4 LIMIT 1);
SET @n3157 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='直线的五种方程形式 [掌握]' AND level=4 LIMIT 1);
SET @n3158 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='圆的标准方程 [掌握]' AND level=4 LIMIT 1);
SET @n3160 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='直线与圆的位置关系判断 [掌握]' AND level=4 LIMIT 1);
SET @n3191 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='椭圆的标准方程与性质 [掌握]' AND level=4 LIMIT 1);
SET @n3192 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='双曲线的标准方程与性质 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3156: 直线斜率 (easy)
('数学[职高]', 'CALCULATION', '已知直线过点 $A(1,2)$ 和 $B(3,6)$，求直线的斜率 $k$ 和直线方程。',
 '[]', '$k=2$，直线方程为 $y=2x$',
 '【解】1. 斜率 $k=\\dfrac{y_2-y_1}{x_2-x_1}=\\dfrac{6-2}{3-1}=\\dfrac{4}{2}=2$\n2. 点斜式: $y-2=2(x-1)$\n3. 化简: $y-2=2x-2$，即 $y=2x$',
 1, 1, 1, 1, 1, @n3156, NOW(), NOW()),

-- 3157: 直线方程 (medium)
('数学[职高]', 'CALCULATION', '求过点 $P(2,-1)$ 且与直线 $3x-4y+1=0$ 垂直的直线方程。',
 '[]', '$4x+3y-5=0$',
 '【解】1. 已知直线化为 $y=\\dfrac{3}{4}x+\\dfrac{1}{4}$，斜率 $k_1=\\dfrac{3}{4}$\n2. 垂直条件: $k_1\\cdot k_2=-1$，$k_2=-\\dfrac{4}{3}$\n3. 点斜式: $y-(-1)=-\\dfrac{4}{3}(x-2)$\n4. $y+1=-\\dfrac{4}{3}x+\\dfrac{8}{3}$\n5. 两边乘 3: $3y+3=-4x+8$，整理: $4x+3y-5=0$',
 2, 1, 1, 1, 1, @n3157, NOW(), NOW()),

-- 3158: 圆的标准方程 (easy)
('数学[职高]', 'CALCULATION', '已知圆 $C$ 的圆心在 $(3,-2)$，且过点 $(6,2)$，求圆的标准方程。',
 '[]', '$(x-3)^2+(y+2)^2=25$',
 '【解】1. 标准方程: $(x-a)^2+(y-b)^2=r^2$，$(a,b)=(3,-2)$\n2. 半径 $r$ 为圆心到点 $(6,2)$ 的距离:\n   $r=\\sqrt{(6-3)^2+(2+2)^2}=\\sqrt{9+16}=\\sqrt{25}=5$\n3. 圆方程为 $(x-3)^2+(y+2)^2=25$',
 1, 1, 1, 1, 1, @n3158, NOW(), NOW()),

-- 3160: 直线与圆位置 (medium)
('数学[职高]', 'CALCULATION', '判断直线 $3x-4y+10=0$ 与圆 $x^2+y^2=4$ 的位置关系。',
 '[]', '相切',
 '【解】1. 圆心 $O(0,0)$，半径 $r=2$\n2. 圆心到直线距离:\n   $d=\\dfrac{|3\\times0-4\\times0+10|}{\\sqrt{3^2+(-4)^2}}=\\dfrac{10}{5}=2$\n3. $d=r=2$，直线与圆相切\n4. 切点唯一，无弦长',
 2, 1, 1, 1, 1, @n3160, NOW(), NOW()),

-- 3191: 椭圆 (medium)
('数学[职高]', 'CALCULATION', '已知椭圆的焦点为 $F_1(-3,0)$ 和 $F_2(3,0)$，椭圆上一点到两焦点距离之和为 10，求椭圆的标准方程。',
 '[]', '$\\dfrac{x^2}{25}+\\dfrac{y^2}{16}=1$',
 '【解】1. 焦点在 $x$ 轴上，设 $\\dfrac{x^2}{a^2}+\\dfrac{y^2}{b^2}=1$\n2. $2a=10$，$a=5$\n3. $c=3$（焦点到中心距离）\n4. $b^2=a^2-c^2=25-9=16$\n5. 方程: $\\dfrac{x^2}{25}+\\dfrac{y^2}{16}=1$',
 2, 1, 1, 1, 1, @n3191, NOW(), NOW()),

-- 3192: 双曲线 (easy)
('数学[职高]', 'CALCULATION', '已知双曲线实轴长为 6，虚轴长为 8，焦点在 $x$ 轴上，求双曲线的标准方程。',
 '[]', '$\\dfrac{x^2}{9}-\\dfrac{y^2}{16}=1$',
 '【解】1. 实轴长 $2a=6$，$a=3$\n2. 虚轴长 $2b=8$，$b=4$\n3. 焦点在 $x$ 轴，方程为 $\\dfrac{x^2}{a^2}-\\dfrac{y^2}{b^2}=1$\n4. $\\dfrac{x^2}{9}-\\dfrac{y^2}{16}=1$',
 1, 1, 1, 1, 1, @n3192, NOW(), NOW());

-- ══════════════════════════════════════════
-- 六、不等式 (4题)
-- ══════════════════════════════════════════

SET @n3085 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='不等式组的解法 [理解]' AND level=4 LIMIT 1);
SET @n3090 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='一元二次不等式的应用 [掌握]' AND level=4 LIMIT 1);
SET @n3091 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='含绝对值不等式的解法 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3090: 一元二次不等式 (easy)
('数学[职高]', 'CALCULATION', '解不等式：$x^2-5x+6 \\leq 0$。',
 '[]', '$2 \\leq x \\leq 3$',
 '【解】1. 因式分解: $x^2-5x+6=(x-2)(x-3)$\n2. 二次项系数 $a=1>0$，抛物线开口向上\n3. 根 $x=2$ 和 $x=3$\n4. 开口向上的抛物线在两根之间 $\\leq 0$\n5. 解集: $[2,3]$，即 $2 \\leq x \\leq 3$',
 1, 1, 1, 1, 1, @n3090, NOW(), NOW()),

-- 3091: 绝对值不等式 (easy)
('数学[职高]', 'CALCULATION', '解不等式：$|2x-1| < 5$。',
 '[]', '$-2 < x < 3$',
 '【解】1. $|2x-1|<5$ 等价于 $-5<2x-1<5$\n2. 两边加 1: $-4<2x<6$\n3. 两边除以 2: $-2<x<3$\n4. 解集: $(-2,3)$',
 1, 1, 1, 1, 1, @n3091, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '解不等式：$|x+3| \\geq 2$。',
 '[]', '$x \\leq -5$ 或 $x \\geq -1$',
 '【解】1. $|x+3|\\geq 2$ 等价于 $x+3 \\leq -2$ 或 $x+3 \\geq 2$\n2. 第一种: $x \\leq -5$\n3. 第二种: $x \\geq -1$\n4. 解集: $(-\\infty,-5] \\cup [-1,+\\infty)$',
 1, 1, 1, 1, 1, @n3091, NOW(), NOW()),

-- 3085: 不等式组 (easy)
('数学[职高]', 'CALCULATION', '解不等式组：$\\begin{cases} 2x-1>3 \\\\ x+2 \\leq 7 \\end{cases}$。',
 '[]', '$2 < x \\leq 5$',
 '【解】分别解两个不等式：\n1. $2x-1>3 \\Rightarrow 2x>4 \\Rightarrow x>2$\n2. $x+2 \\leq 7 \\Rightarrow x \\leq 5$\n3. 取交集: $2 < x \\leq 5$',
 1, 1, 1, 1, 1, @n3085, NOW(), NOW());

-- ══════════════════════════════════════════
-- 七、集合 (2题)
-- ══════════════════════════════════════════

SET @n3081 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='集合关系判断与证明 [理解]' AND level=4 LIMIT 1);
SET @n3083 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='并集运算 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3083: 集合运算 (easy)
('数学[职高]', 'CALCULATION', '已知集合 $A=\\{x \\mid x^2-5x+6=0\\}$，$B=\\{x \\mid x^2-4=0\\}$，求 $A\\cup B$ 和 $A\\cap B$。',
 '[]', '$A\\cup B=\\{-2,2,3\\}$，$A\\cap B=\\{2\\}$',
 '【解】1. 解 $x^2-5x+6=0$: $(x-2)(x-3)=0$，$A=\\{2,3\\}$\n2. 解 $x^2-4=0$: $x=\\pm2$，$B=\\{-2,2\\}$\n3. 并集 $A\\cup B=\\{-2,2,3\\}$\n4. 交集 $A\\cap B=\\{2\\}$',
 1, 1, 1, 1, 1, @n3083, NOW(), NOW()),

-- 3081: 补集运算 (easy)
('数学[职高]', 'CALCULATION', '已知全集 $U=\\{x \\mid x \\in \\mathbb{N}, x<10\\}$，$A=\\{1,3,5,7,9\\}$，求 $\\complement_U A$。',
 '[]', '$\\{0,2,4,6,8\\}$',
 '【解】1. $U=\\{0,1,2,3,4,5,6,7,8,9\\}$（自然数中小于 10 的）\n2. $A=\\{1,3,5,7,9\\}$\n3. $\\complement_U A$ 为 $U$ 中不在 $A$ 的元素\n4. $\\complement_U A=\\{0,2,4,6,8\\}$',
 1, 1, 1, 1, 1, @n3081, NOW(), NOW());

-- ══════════════════════════════════════════
-- 八、概率统计 (2题)
-- ══════════════════════════════════════════

SET @n3169 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='抽样方法 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 3169: 古典概型 (medium)
('数学[职高]', 'CALCULATION', '从 5 名男生和 3 名女生中任选 2 人，求恰好选到 1 男 1 女的概率。',
 '[]', '$\\dfrac{15}{28}$',
 '【解】1. 总选法: $C_8^2=\\dfrac{8\\times7}{2}=28$ 种\n2. 恰好 1 男 1 女:\n   选 1 男: $C_5^1=5$ 种\n   选 1 女: $C_3^1=3$ 种\n   共 $5\\times3=15$ 种\n3. 概率 $P=\\dfrac{15}{28}$',
 2, 1, 1, 1, 1, @n3169, NOW(), NOW()),

-- 3169: 统计 (easy)
('数学[职高]', 'CALCULATION', '已知一组数据：$5,7,8,9,11$，求这组数据的平均数和方差。',
 '[]', '平均数 $=8$，方差 $=4$',
 '【解】1. 平均数 $\\bar{x}=\\dfrac{5+7+8+9+11}{5}=\\dfrac{40}{5}=8$\n2. 方差 $s^2=\\dfrac{(5-8)^2+(7-8)^2+(8-8)^2+(9-8)^2+(11-8)^2}{5}$\n   $=\\dfrac{9+1+0+1+9}{5}=\\dfrac{20}{5}=4$',
 1, 1, 1, 1, 1, @n3169, NOW(), NOW());

-- ══════════════════════════════════════════
-- 九、平面向量 (3题) — 补边缘章节缺口
-- ══════════════════════════════════════════

SET @n3145 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='向量的加法与减法' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- 向量运算 (easy)
('数学[职高]', 'CALCULATION', '已知 $\\vec{a}=(2,1)$，$\\vec{b}=(-1,3)$，求：(1) $\\vec{a}+\\vec{b}$；(2) $2\\vec{a}-3\\vec{b}$；(3) $\\vec{a}\\cdot\\vec{b}$。',
 '[]', '(1) $(1,4)$；(2) $(7,-7)$；(3) $1$',
 '【解】(1) $\\vec{a}+\\vec{b}=(2+(-1),1+3)=(1,4)$\n(2) $2\\vec{a}-3\\vec{b}=2(2,1)-3(-1,3)$\n   $=(4,2)-(-3,9)=(7,-7)$\n(3) $\\vec{a}\\cdot\\vec{b}=2\\times(-1)+1\\times3=-2+3=1$',
 1, 1, 1, 1, 1, @n3145, NOW(), NOW()),

-- 向量的模 (easy)
('数学[职高]', 'CALCULATION', '已知 $\\vec{a}=(3,4)$，求 $|\\vec{a}|$ 和与 $\\vec{a}$ 同方向的单位向量。',
 '[]', '$|\\vec{a}|=5$，单位向量 $=(\\dfrac{3}{5},\\dfrac{4}{5})$',
 '【解】1. $|\\vec{a}|=\\sqrt{3^2+4^2}=\\sqrt{25}=5$\n2. 单位向量 $\\vec{e}=\\dfrac{\\vec{a}}{|\\vec{a}|}= (\\dfrac{3}{5},\\dfrac{4}{5})$\n3. 验证: $|\\vec{e}|=\\sqrt{\\frac{9}{25}+\\frac{16}{25}}=1$ ✓',
 1, 1, 1, 1, 1, @n3145, NOW(), NOW()),

-- 向量夹角 (medium)
('数学[职高]', 'CALCULATION', '已知 $\\vec{a}=(2,-1)$，$\\vec{b}=(1,3)$，求 $\\vec{a}$ 与 $\\vec{b}$ 的夹角 $\\theta$ 的余弦值。',
 '[]', '$\\cos\\theta=-\\dfrac{1}{5\\sqrt{2}}$',
 '【解】1. $\\vec{a}\\cdot\\vec{b}=2\\times1+(-1)\\times3=2-3=-1$\n2. $|\\vec{a}|=\\sqrt{2^2+(-1)^2}=\\sqrt{5}$\n3. $|\\vec{b}|=\\sqrt{1^2+3^2}=\\sqrt{10}$\n4. $\\cos\\theta=\\dfrac{\\vec{a}\\cdot\\vec{b}}{|\\vec{a}||\\vec{b}|}=\\dfrac{-1}{\\sqrt{5}\\cdot\\sqrt{10}}=\\dfrac{-1}{\\sqrt{50}}=-\\dfrac{1}{5\\sqrt{2}}$',
 2, 1, 1, 1, 1, @n3145, NOW(), NOW());

COMMIT;
