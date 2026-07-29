-- ============================================================================
-- v193: 数学[职高] P1-1 补题 — 函数 + 三角函数 (32题)
-- 函数: 3097表示方法(4) + 3098值域求解(4) + 3103应用题建模(4) = 12题
-- 三角函数: 3116任意角(3) + 3117弧度制(3) + 3119同角关系(4) + 3121诱导公式五六(3) + 3123正切图像(4) = 17题
-- 难度: 每节点 easy(1) 2道 + medium(2) 1~2道 + 部分 hard(3) 1道
-- 幂等：INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ══════════════════════════════════════════
-- 函数: 3节点 × 4题 = 12题
-- ══════════════════════════════════════════

-- === 3097: 函数的表示方法 [理解] ===
SET @n3097 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数的表示方法 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '下列表示函数的方法中，最适合展示函数变化趋势的是：',
 '["A. 解析法","B. 列表法","C. 图像法","D. 文字描述法"]', 'C',
 '图像法能直观看出函数的增减趋势、最大值、最小值等变化规律。解析法精确但不直观，列表法只能表示有限个点。', 1, 1, 1, 1, 1, @n3097, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '已知 $f(x)=\\\\begin{cases} x+1, & x \\\\leq 0 \\\\\\\\ 2x, & x > 0 \\\\end{cases}$，则 $f(-1)+f(1)$ 的值为：',
 '["A. 0","B. 1","C. 2","D. 3"]', 'C',
 'f(-1)用第一段：-1+1=0；f(1)用第二段：2×1=2。0+2=2。分段函数是一个函数，根据x的范围选择表达式。', 1, 1, 1, 1, 1, @n3097, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '函数 $y=|x-2|$ 写成分段函数的形式是：',
 '["A. $y=\\\\begin{cases} x-2, x \\\\geq 2 \\\\\\\\ 2-x, x < 2 \\\\end{cases}$","B. $y=\\\\begin{cases} x-2, x \\\\geq 0 \\\\\\\\ 2-x, x < 0 \\\\end{cases}$","C. $y=x-2$（对所有x）","D. $y=2-x$（对所有x）"]', 'A',
 '|x-2|当x≥2时等于x-2，当x<2时等于-(x-2)=2-x。分段点是x=2。', 2, 1, 1, 1, 1, @n3097, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '已知函数 $f(x)$ 满足下表，则 $f(f(1)) = $ _____。\n\n| x | 1 | 2 | 3 | 4 |\n|:--:|:--:|:--:|:--:|:--:|\n| f(x) | 3 | 4 | 2 | 1 |',
 '[]', '2',
 '先查表：f(1)=3。再查表：f(3)=2。所以f(f(1))=f(3)=2。列表法表示的函数通过查表求值。', 2, 1, 1, 1, 1, @n3097, NOW(), NOW());

-- === 3098: 函数值域求解 [理解] ===
SET @n3098 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数值域求解 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '函数 $f(x)=x^2+1$ 的值域是：',
 '["A. $(0,+\\\\infty)$","B. $[1,+\\\\infty)$","C. $[0,+\\\\infty)$","D. $\\\\mathbb{R}$"]', 'B',
 'x²≥0，所以x²+1≥1，值域为[1,+∞)。这是观察法：直接从解析式判断范围。', 1, 1, 1, 1, 1, @n3098, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '函数 $f(x)=2x+1$ 在区间 $[0,3]$ 上的值域是：',
 '["A. $[1,7]$","B. $[0,7]$","C. $[1,6]$","D. $[0,6]$"]', 'A',
 '一次函数在闭区间上单调，代端点即可。f(0)=1，f(3)=7，值域[1,7]。', 1, 1, 1, 1, 1, @n3098, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '函数 $y=\\\\dfrac{1}{x-1}$ 的值域是：',
 '["A. $\\\\mathbb{R}$","B. $\\\\{y \\\\mid y \\\\neq 0\\\\}$","C. $\\\\{y \\\\mid y \\\\neq 1\\\\}$","D. $(0,+\\\\infty)$"]', 'B',
 'y=1/(x-1)，分母不会为0，所以y≠0。定义域为x≠1，值域为y≠0。', 2, 1, 1, 1, 1, @n3098, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '函数 $y=\\\\sqrt{x}+2$ 的值域为 _____。',
 '[]', '[2,+∞)',
 '√x≥0，所以√x+2≥2，值域[2,+∞)。注意√x定义域为[0,+∞)。', 2, 1, 1, 1, 1, @n3098, NOW(), NOW());

-- === 3103: 函数应用题建模 [理解] ===
SET @n3103 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数应用题建模 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '用16米长的篱笆围一个矩形场地，则场地面积S关于宽x的函数关系是：',
 '["A. $S=x(16-x)$","B. $S=x(8-x)$","C. $S=x(16-2x)$","D. $S=2x(8-x)$"]', 'B',
 '周长16，长+宽=8。设宽为x，则长为8-x，S=x(8-x)。定义域0<x<8。', 1, 1, 1, 1, 1, @n3103, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '某手机话费方案：月租20元，每分钟通话0.1元。则月话费y(元)与通话时间x(分钟)的函数关系是：',
 '["A. $y=0.1x$","B. $y=20x+0.1$","C. $y=20+0.1x$","D. $y=0.1(x+20)$"]', 'C',
 '话费=月租+单价×时间，y=20+0.1x。定义域x≥0。', 1, 1, 1, 1, 1, @n3103, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '某商品进价30元，售价x元时每天卖出(150-x)件。则日利润L(x)=_____（用x表示）。',
 '[]', '(x-30)(150-x)',
 '每件利润x-30，销量150-x，总利润L=(x-30)(150-x)=-x²+180x-4500。定义域30≤x≤150。', 2, 1, 1, 1, 1, @n3103, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '将一根40cm的铁丝弯成矩形，面积最大为 _____ cm²。',
 '[]', '100',
 '设一边为x，另一边为20-x。S=x(20-x)=-x²+20x=-(x-10)²+100，x=10时最大面积100。正方形时面积最大。', 3, 1, 1, 1, 1, @n3103, NOW(), NOW());


-- ══════════════════════════════════════════
-- 三角函数: 5节点 × 3~4题 = 17题
-- ══════════════════════════════════════════

-- === 3116: 任意角的概念与表示 [了解] ===
SET @n3116 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='任意角的概念与表示 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '角 $-30^\\\\circ$ 是：',
 '["A. 第一象限角","B. 第二象限角","C. 第三象限角","D. 第四象限角"]', 'D',
 '-30°即顺时针旋转30°，终边在第四象限。或者-30°=330°也在第四象限。', 1, 1, 1, 1, 1, @n3116, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '与 $45^\\\\circ$ 终边相同的角是：',
 '["A. $-315^\\\\circ$","B. $315^\\\\circ$","C. $-45^\\\\circ$","D. $135^\\\\circ$"]', 'A',
 '终边相同角差360°的整数倍：45°-360°=-315°。315°=360°-45°≠45°。', 1, 1, 1, 1, 1, @n3116, NOW(), NOW()),

('数学[职高]', 'TRUE_FALSE', '$720^\\\\circ$ 的终边与 $0^\\\\circ$ 的终边相同。', '[]', 'T',
 '720°=2×360°，刚好转了两整圈回到起点，与0°终边相同。', 2, 1, 1, 1, 1, @n3116, NOW(), NOW());

-- === 3117: 弧度制与角度制互化 [理解] ===
SET @n3117 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='弧度制与角度制互化 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '将 $60^\\\\circ$ 化为弧度：',
 '["A. $\\\\frac{\\\\pi}{6}$","B. $\\\\frac{\\\\pi}{4}$","C. $\\\\frac{\\\\pi}{3}$","D. $\\\\frac{\\\\pi}{2}$"]', 'C',
 '180°=π rad，60°=60×π/180=π/3。', 1, 1, 1, 1, 1, @n3117, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '将 $\\\\dfrac{5\\\\pi}{6}$ 化为角度：',
 '["A. $120^\\\\circ$","B. $135^\\\\circ$","C. $150^\\\\circ$","D. $180^\\\\circ$"]', 'C',
 '5π/6×180°/π=5×30°=150°。', 1, 1, 1, 1, 1, @n3117, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '半径为 $r=5$，圆心角 $\\\\theta=\\\\dfrac{2\\\\pi}{5}$ 的扇形弧长 $l=$ _____。',
 '[]', '2π',
 'l=|θ|·r=(2π/5)×5=2π。弧长公式中的θ必须是弧度制。', 2, 1, 1, 1, 1, @n3117, NOW(), NOW());

-- === 3119: 同角三角函数基本关系 [掌握] ===
SET @n3119 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='同角三角函数基本关系 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '已知 $\\\\sin\\\\alpha = \\\\dfrac{1}{2}$，$\\\\alpha$ 为锐角，则 $\\\\cos\\\\alpha =$',
 '["A. $\\\\dfrac{1}{2}$","B. $\\\\dfrac{\\\\sqrt{2}}{2}$","C. $\\\\dfrac{\\\\sqrt{3}}{2}$","D. $\\\\sqrt{3}$"]', 'C',
 'sin²α+cos²α=1，cosα=√(1-1/4)=√(3/4)=√3/2。α为锐角，cosα取正值。', 1, 1, 1, 1, 1, @n3119, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '已知 $\\\\tan\\\\alpha = 2$，则 $\\\\dfrac{\\\\sin\\\\alpha+\\\\cos\\\\alpha}{\\\\sin\\\\alpha-\\\\cos\\\\alpha}$ 的值为：',
 '["A. 1","B. 2","C. 3","D. 4"]', 'C',
 '分子分母同除以cosα：原式=(tanα+1)/(tanα-1)=(2+1)/(2-1)=3。', 2, 1, 1, 1, 1, @n3119, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '下列各式中恒成立的是：',
 '["A. $\\\\sin^2\\\\alpha - \\\\cos^2\\\\alpha = 1$","B. $\\\\sin\\\\alpha \\\\cdot \\\\cos\\\\alpha = 1$","C. $\\\\sin^2\\\\alpha + \\\\cos^2\\\\alpha = 1$","D. $\\\\sin\\\\alpha + \\\\cos\\\\alpha = 1$"]', 'C',
 'sin²α+cos²α=1是基本恒等式。A差了一个符号，B、D一般情况不成立。', 1, 1, 1, 1, 1, @n3119, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '已知 $\\\\sin\\\\alpha = \\\\dfrac{3}{5}$，$\\\\alpha$ 在第二象限，则 $\\\\tan\\\\alpha = $ _____。',
 '[]', '-3/4',
 'cosα=-√(1-9/25)=-4/5（第二象限cos为负）。tanα=sinα/cosα=(3/5)/(-4/5)=-3/4。', 3, 1, 1, 1, 1, @n3119, NOW(), NOW());

-- === 3121: 诱导公式(五)~(六) [掌握] ===
SET @n3121 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='诱导公式(五)~(六) [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '$\\\\sin(\\\\dfrac{\\\\pi}{2}+\\\\alpha)$ 等于：',
 '["A. $\\\\sin\\\\alpha$","B. $-\\\\sin\\\\alpha$","C. $\\\\cos\\\\alpha$","D. $-\\\\cos\\\\alpha$"]', 'C',
 'sin(π/2+α)=cosα。诱导公式五：sin(π/2±α)=cosα，符号看象限。', 1, 1, 1, 1, 1, @n3121, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '$\\\\cos(\\\\dfrac{3\\\\pi}{2}-\\\\alpha)$ 化简为：',
 '["A. $\\\\sin\\\\alpha$","B. $-\\\\sin\\\\alpha$","C. $\\\\cos\\\\alpha$","D. $-\\\\cos\\\\alpha$"]', 'B',
 'cos(3π/2-α)=-sinα。3π/2-α在第三象限，cos为负，cos变sin。', 2, 1, 1, 1, 1, @n3121, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '$\\\\cos(\\\\dfrac{\\\\pi}{2}+\\\\alpha) = $ _____。',
 '[]', '-sinα',
 'cos(π/2+α)=-sinα。奇变偶不变，π/2是π/2的奇数倍，cos变sin；π/2+α在第二象限cos为负。', 2, 1, 1, 1, 1, @n3121, NOW(), NOW());

-- === 3123: 正切函数的图像与性质 [了解] ===
SET @n3123 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='正切函数的图像与性质 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '函数 $y=\\\\tan x$ 的定义域是：',
 '["A. $\\\\mathbb{R}$","B. $\\\\{x \\\\mid x \\\\neq k\\\\pi, k \\\\in \\\\mathbb{Z}\\\\}$","C. $\\\\{x \\\\mid x \\\\neq \\\\dfrac{\\\\pi}{2}+k\\\\pi, k \\\\in \\\\mathbb{Z}\\\\}$","D. $\\\\{x \\\\mid x \\\\neq k\\\\pi+\\\\dfrac{\\\\pi}{4}, k \\\\in \\\\mathbb{Z}\\\\}$"]', 'C',
 'tanx=sinx/cosx，cosx≠0即x≠π/2+kπ(k∈Z)。正切函数的图像在这些点处有垂直渐近线。', 1, 1, 1, 1, 1, @n3123, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '$\\\\tan 135^\\\\circ$ 的值为：',
 '["A. 1","B. -1","C. $\\\\sqrt{3}$","D. $-\\\\sqrt{3}$"]', 'B',
 '135°=180°-45°，在第二象限tan为负，tan135°=tan(180°-45°)=-tan45°=-1。', 1, 1, 1, 1, 1, @n3123, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '正切函数 $y=\\\\tan x$ 的最小正周期是：',
 '["A. $\\\\dfrac{\\\\pi}{2}$","B. $\\\\pi$","C. $2\\\\pi$","D. $4\\\\pi$"]', 'B',
 'tan(x+π)=tanx恒成立，最小正周期为π。而sinx和cosx的周期是2π。', 2, 1, 1, 1, 1, @n3123, NOW(), NOW()),

('数学[职高]', 'TRUE_FALSE', '正切函数 $y=\\\\tan x$ 在 $[0,\\\\pi]$ 上单调递增。', '[]', 'F',
 'tanx在x=π/2处无定义（有渐近线），在[0,π/2)上递增，在(π/2,π]上也递增，但不能说在整个[0,π]上递增。', 3, 1, 1, 1, 1, @n3123, NOW(), NOW());

COMMIT;
SELECT 'v193 deployed — 函数+三角函数 32题' AS result;
-- ============================================================================
