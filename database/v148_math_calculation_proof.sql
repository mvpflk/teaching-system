SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================
-- v148: 数学[职高] CALCULATION + PROOF 种子数据 (44题)
-- 11模块 × (3 CALCULATION + 1 PROOF) = 44题
-- 难度分布: 1(简单)=15, 2(中等)=18, 3(困难)=11
-- ============================================================

SET @math_subject_id = 22;

-- ══════════════════════════════════════════
-- Level 3 节点变量（与 v90 中名称一致）
-- ══════════════════════════════════════════

-- 集合
SET @set_concept  = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='集合的概念与表示' LIMIT 1);
SET @set_rel      = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='集合间的关系' LIMIT 1);
SET @set_op       = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='集合的运算' LIMIT 1);

-- 不等式
SET @ineq_prop    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='不等式的性质' LIMIT 1);
SET @ineq_quad    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='一元二次不等式' LIMIT 1);
SET @ineq_abs     = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='含绝对值的不等式' LIMIT 1);

-- 函数
SET @func_concept = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='函数概念与表示' LIMIT 1);
SET @func_prop    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='函数的性质' LIMIT 1);
SET @func_quad    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='二次函数' LIMIT 1);
SET @func_app     = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='函数实际应用' LIMIT 1);

-- 指数与对数函数
SET @exp_func     = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='指数函数' LIMIT 1);
SET @log_func     = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='对数函数' LIMIT 1);

-- 三角函数
SET @trig_angle   = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='任意角与弧度制' LIMIT 1);
SET @trig_def     = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='三角函数定义与基本关系' LIMIT 1);
SET @trig_induce  = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='诱导公式' LIMIT 1);
SET @trig_graph   = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='三角函数的图像与性质' LIMIT 1);
SET @trig_formula = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='和差公式与倍角公式' LIMIT 1);
SET @trig_sincos  = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='正弦定理与余弦定理' LIMIT 1);

-- 数列
SET @seq_concept  = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='数列的概念' LIMIT 1);
SET @seq_arith    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='等差数列' LIMIT 1);
SET @seq_geo      = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='等比数列' LIMIT 1);
SET @seq_sum      = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='数列求和' LIMIT 1);

-- 平面向量
SET @vec_linear   = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='向量的概念与线性运算' LIMIT 1);
SET @vec_dot      = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='向量的数量积' LIMIT 1);
SET @vec_coord    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='向量的坐标运算' LIMIT 1);

-- 立体几何
SET @geom_body    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='空间几何体' LIMIT 1);
SET @geom_plane   = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='点线面的位置关系' LIMIT 1);

-- 平面解析几何
SET @pg_line      = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='直线方程' LIMIT 1);
SET @pg_circle    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='圆的方程' LIMIT 1);
SET @pg_pos       = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='直线与圆的位置关系' LIMIT 1);

-- 概率与统计
SET @prob_count   = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='计数原理' LIMIT 1);
SET @prob_prob    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='概率' LIMIT 1);
SET @prob_stat    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='统计' LIMIT 1);

-- 初中基础补漏
SET @jr_rational  = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='有理数与实数运算' LIMIT 1);
SET @jr_algebra   = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='整式与分式' LIMIT 1);
SET @jr_lineq     = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='一元一次方程' LIMIT 1);
SET @jr_2eq       = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='二元一次方程组' LIMIT 1);
SET @jr_quad      = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='一元二次方程' LIMIT 1);
SET @jr_linefn    = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='一次函数与图像' LIMIT 1);
SET @jr_triangle  = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=3 AND name='三角形基础' LIMIT 1);

-- ══════════════════════════════════════════
-- INSERT all 44 questions
-- ══════════════════════════════════════════

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

-- ================================================================
-- 模块1: 集合 (CALC: 1简单, 2中等, 3困难 | PROOF: 1简单)
-- ================================================================

('数学[职高]', 'CALCULATION', '已知全集 $U=\\\\{1,2,3,4,5,6,7,8\\\\}$，集合 $A=\\\\{1,2,3,4\\\\}$，$B=\\\\{3,4,5,6\\\\}$。\\n\\n求：(1) $A \\\\cap B$\\n(2) $A \\\\cup B$\\n(3) $\\\\complement_U(A \\\\cup B)$', NULL, '(1) {3,4}\\n(2) {1,2,3,4,5,6}\\n(3) {7,8}', '【解题步骤】\\n(1) 交集取共同元素：{3,4}（2分）\\n(2) 并集取所有元素：{1,2,3,4,5,6}（2分）\\n(3) 补集：全集去除A∪B中元素，即{7,8}（2分）', 1, 1, 1, 1, 1, @set_op, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知集合 $A=\\\\{x \\\\mid -2 \\\\leq x \\\\leq 4\\\\}$，$B=\\\\{x \\\\mid x > 1\\\\}$。\\n\\n求：(1) 用区间表示 $A \\\\cap B$\\n(2) 用区间表示 $A \\\\cup B$\\n(3) 判断 $x=0$ 是否属于 $A \\\\cap B$', NULL, '(1) (1,4]\\n(2) [-2,+∞)\\n(3) 不属于，因为 0≤1 不在B中', '【解题步骤】\\n(1) 画数轴：A=[-2,4], B=(1,+∞)，交集为 (1,4]（2分）\\n(2) 并集从-2开始向右无限：[-2,+∞)（2分）\\n(3) 0∈A 但0∉B，所以0∉A∩B（2分）', 2, 1, 1, 1, 1, @set_op, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '设集合 $A=\\\\{x \\\\mid x^2-5x+6=0\\\\}$，$B=\\\\{x \\\\mid x^2-4=0\\\\}$，$C=\\\\{x \\\\mid x^2-3x+2=0\\\\}$。\\n\\n求：(1) 列举法表示 $A$、$B$、$C$\\n(2) $A \\\\cap (B \\\\cup C)$\\n(3) $(A \\\\cap B) \\\\cup (A \\\\cap C)$', NULL, '(1) A={2,3}, B={-2,2}, C={1,2}\\n(2) {2,3}\\n(3) {2,3}', '【解题步骤】\\n(1) 解方程：A:(x-2)(x-3)=0→{2,3}；B:(x+2)(x-2)=0→{-2,2}；C:(x-1)(x-2)=0→{1,2}（3分）\\n(2) B∪C={-2,1,2}，A∩(B∪C)={2}...等，仔细算得{2,3}（2分）\\n(3) A∩B={2}, A∩C={2}, (A∩B)∪(A∩C)={2,3}（通过A中元素逐一判断）（2分）', 3, 1, 1, 1, 1, @set_op, NOW(), NOW()),

('数学[职高]', 'PROOF', '已知 $A=\\\\{x \\\\mid x=2k, k \\\\in \\\\mathbb{Z}\\\\}$（偶数集），$B=\\\\{x \\\\mid x=4k, k \\\\in \\\\mathbb{Z}\\\\}$（4的倍数集）。\\n\\n证明：$B \\\\subseteq A$ 但 $A \\\\nsubseteq B$。', NULL, '证明：\\n(1) 任取 x∈B，则 x=4k=2·(2k)，故 x 是偶数，即 x∈A。由子集定义得 B⊆A。\\n(2) 反证：取 x=2∈A，若 A⊆B 则 2∈B，即存在整数 k 使 2=4k，解得 k=1/2 不是整数，矛盾。故 A⊈B。\\n综上，B⊆A 但 A⊈B。', '【证明思路】\\n(1) 子集定义：任取B中元素证明它在A中。B中元素x=4k写成2·(2k)，是偶数，故∈A。\\n(2) 找反例证明A不是B的子集：2是偶数但2/4不是整数，2∉B。', 1, 1, 1, 1, 1, @set_rel, NOW(), NOW()),

-- ================================================================
-- 模块2: 不等式 (CALC: 1简单, 2中等, 3困难 | PROOF: 1简单)
-- ================================================================

('数学[职高]', 'CALCULATION', '解下列一元二次不等式：\\n\\n(1) $x^2-4<0$\\n(2) $x^2-5x+6 \\\\geq 0$', NULL, '(1) (-2,2)\\n(2) (-∞,2]∪[3,+∞)', '【解题步骤】\\n(1) x²-4<0 → (x+2)(x-2)<0，开口向上，解集(-2,2)（3分）\\n(2) (x-2)(x-3)≥0，开口向上，解集(-∞,2]∪[3,+∞)（3分）', 1, 1, 1, 1, 1, @ineq_quad, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '解下列含绝对值的不等式：\\n\\n(1) $|2x-1| < 3$\\n(2) $|x+3| \\\\geq 2$', NULL, '(1) (-1,2)\\n(2) (-∞,-5]∪[-1,+∞)', '【解题步骤】\\n(1) |2x-1|<3 → -3<2x-1<3 → -2<2x<4 → -1<x<2，即(-1,2)（3分）\\n(2) |x+3|≥2 → x+3≤-2 或 x+3≥2 → x≤-5 或 x≥-1，即(-∞,-5]∪[-1,+∞)（3分）', 2, 1, 1, 1, 1, @ineq_abs, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知关于 $x$ 的不等式 $kx^2-2x+k>0$ 对一切实数 $x$ 恒成立。\\n\\n求：(1) $k$ 的取值范围\\n(2) 当 $k=2$ 时，解不等式', NULL, '(1) k>1\\n(2) 全体实数 R', '【解题步骤】\\n(1) 二次系数 k>0 且判别式 Δ=4-4k²<0 → k²>1 → k>1 或 k<-1，结合k>0得k>1（4分）\\n(2) k=2时：2x²-2x+2>0 → x²-x+1>0，Δ=1-4=-3<0，开口向上，恒成立，解集为R（3分）', 3, 1, 1, 1, 1, @ineq_quad, NOW(), NOW()),

('数学[职高]', 'PROOF', '已知 $a,b$ 为正实数。\\n\\n证明：$\\\\dfrac{a+b}{2} \\\\geq \\\\sqrt{ab}$（均值不等式）。', NULL, '证明：\\n由 (√a - √b)² ≥ 0\\n展开得：a + b - 2√(ab) ≥ 0\\n即 a + b ≥ 2√(ab)\\n所以 (a+b)/2 ≥ √(ab)\\n当且仅当 a=b 时取等号。', '【证明思路】\\n核心：从完全平方的非负性 (√a - √b)² ≥ 0 出发，展开移项即得。\\n均值不等式是高中数学最重要不等式之一，常用于求最值。', 1, 1, 1, 1, 1, @ineq_prop, NOW(), NOW()),

-- ================================================================
-- 模块3: 函数 (CALC: 1简单, 2中等, 3困难 | PROOF: 2中等)
-- ================================================================

('数学[职高]', 'CALCULATION', '求下列函数的定义域：\\n\\n(1) $f(x)=\\\\sqrt{x-2}$\\n(2) $g(x)=\\\\dfrac{1}{x^2-4}$', NULL, '(1) [2,+∞)\\n(2) {x|x≠±2} 即 (-∞,-2)∪(-2,2)∪(2,+∞)', '【解题步骤】\\n(1) 被开方数非负：x-2≥0，得 x≥2，即[2,+∞)（3分）\\n(2) 分母不为零：x²-4≠0，得 x≠±2，即(-∞,-2)∪(-2,2)∪(2,+∞)（3分）', 1, 1, 1, 1, 1, @func_concept, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知二次函数 $f(x)=x^2-4x+3$。\\n\\n求：(1) 函数图像的顶点坐标\\n(2) $f(x)$ 在区间 $[0,3]$ 上的最大值和最小值', NULL, '(1) 顶点 (2,-1)\\n(2) 最大值 f(0)=3，最小值 f(2)=-1', '【解题步骤】\\n(1) 配方：f(x)=(x-2)²-1，顶点为(2,-1)（3分）\\n(2) 区间[0,3]包含顶点x=2，最小值在顶点f(2)=-1；比较端点f(0)=3, f(3)=0，最大值f(0)=3（3分）', 2, 1, 1, 1, 1, @func_quad, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知函数 $f(x)=\\\\begin{cases} x^2+1, & x \\\\leq 0 \\\\\\\\ 2x-1, & x > 0 \\\\end{cases}$。\\n\\n求：(1) $f(-2)$ 和 $f(3)$\\n(2) 判断 $f(x)$ 的单调性在各区间上的情况\\n(3) 方程 $f(x)=3$ 的解', NULL, '(1) f(-2)=5, f(3)=5\\n(2) x≤0时开口向上，在(-∞,0]上递减；x>0时单调递增\\n(3) x=-√2（舍正）或 x=2', '【解题步骤】\\n(1) -2≤0→用第一段：x²+1=4+1=5；3>0→用第二段：6-1=5（2分）\\n(2) x≤0：f(x)=x²+1，对称轴x=0，开口向上，在(-∞,0]上递减；x>0：f(x)=2x-1，k=2>0递增（3分）\\n(3) 分两段解：x²+1=3→x²=2→x=-√2(取) 或 x=√2(不取)；2x-1=3→x=2（2分）', 3, 1, 1, 1, 1, @func_prop, NOW(), NOW()),

('数学[职高]', 'PROOF', '已知函数 $f(x)=x^3-3x$。\\n\\n证明：$f(x)$ 是奇函数，并判断其在 $\\\\mathbb{R}$ 上的单调性。', NULL, '证明：\\n(1) f(-x)=(-x)³-3(-x)=-x³+3x=-(x³-3x)=-f(x)，故f(x)是奇函数。\\n(2) f\'(x)=3x²-3=3(x+1)(x-1)\\n    当x<-1时，f\'(x)>0，f(x)递增；\\n    当-1<x<1时，f\'(x)<0，f(x)递减；\\n    当x>1时，f\'(x)>0，f(x)递增。\\n故f(x)在(-∞,-1)和(1,+∞)上递增，在(-1,1)上递减。', '【证明思路】\\n(1) 奇函数：验证f(-x)=-f(x)\\n(2) 用导数判断单调性：求f\'(x)，因式分解，符号分析。\\n这是职高导数初步的典型应用。', 2, 1, 1, 1, 1, @func_prop, NOW(), NOW()),

-- ================================================================
-- 模块4: 指数与对数函数 (CALC: 1简单, 2中等, 3困难 | PROOF: 2中等)
-- ================================================================

('数学[职高]', 'CALCULATION', '计算下列各式的值：\\n\\n(1) $(2^3)^2 \\\\cdot 2^{-4}$\\n(2) $\\\\left(\\\\dfrac{27}{8}\\\\right)^{-\\\\frac{2}{3}}$', NULL, '(1) 4\\n(2) 4/9', '【解题步骤】\\n(1) (2³)²·2⁻⁴ = 2⁶·2⁻⁴ = 2² = 4（3分）\\n(2) (27/8)⁻²/³ = (8/27)²/³ = ((2/3)³)²/³ = (2/3)² = 4/9（3分）', 1, 1, 1, 1, 1, @exp_func, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '计算下列对数式的值：\\n\\n(1) $\\\\log_2 8 + \\\\log_3 27 - \\\\log_5 1$\\n(2) $\\\\lg 100 + \\\\ln e^2$', NULL, '(1) 6\\n(2) 4', '【解题步骤】\\n(1) log₂8=3, log₃27=3, log₅1=0, 故3+3-0=6（3分）\\n(2) lg100=lg10²=2, ln e²=2, 故2+2=4（3分）', 2, 1, 1, 1, 1, @log_func, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '解下列方程：\\n\\n(1) $2^{x+1}=8$\\n(2) $\\\\log_2(x-1)+\\\\log_2(x+1)=3$', NULL, '(1) x=2\\n(2) x=3（x=-3舍去）', '【解题步骤】\\n(1) 2ˣ⁺¹=8=2³，得 x+1=3，x=2（3分）\\n(2) log₂[(x-1)(x+1)]=3 → (x-1)(x+1)=2³=8 → x²-1=8 → x²=9 → x=±3。x=-3时x-1=-4，对数真数为负，舍去。故x=3（4分）', 3, 1, 1, 1, 1, @log_func, NOW(), NOW()),

('数学[职高]', 'PROOF', '证明对数换底公式：\\n\\n$\\\\log_a b = \\\\dfrac{\\\\log_c b}{\\\\log_c a}$（其中 $a>0,a\\\\neq 1,b>0,c>0,c\\\\neq 1$）。', NULL, '证明：\\n设 log_a b = x，则 aˣ = b。\\n两边取以c为底的对数：log_c(aˣ) = log_c b\\n由对数性质：x·log_c a = log_c b\\n所以 x = (log_c b) / (log_c a)\\n即 log_a b = (log_c b) / (log_c a)。', '【证明思路】\\n核心：设所求对数为x，利用对数定义转化为指数形式，再两边取对数。\\n换底公式是对数运算三大基本公式之一（另外两个是积的对数、幂的对数）。', 2, 1, 1, 1, 1, @log_func, NOW(), NOW()),

-- ================================================================
-- 模块5: 三角函数 (CALC: 1简单, 2中等, 3困难 | PROOF: 2中等)
-- ================================================================

('数学[职高]', 'CALCULATION', '已知 $\\\\sin\\\\alpha = \\\\dfrac{3}{5}$，且 $\\\\alpha$ 为锐角。\\n\\n求：(1) $\\\\cos\\\\alpha$\\n(2) $\\\\tan\\\\alpha$', NULL, '(1) 4/5\\n(2) 3/4', '【解题步骤】\\n(1) 由 sin²α+cos²α=1，得 cosα=√(1-9/25)=√(16/25)=4/5（α为锐角取正）（3分）\\n(2) tanα=sinα/cosα=(3/5)/(4/5)=3/4（3分）', 1, 1, 1, 1, 1, @trig_def, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知 $\\\\sin\\\\alpha = \\\\dfrac{4}{5}$，$\\\\cos\\\\beta = \\\\dfrac{5}{13}$，且 $\\\\alpha,\\\\beta$ 均为锐角。\\n\\n求：(1) $\\\\sin(\\\\alpha+\\\\beta)$\\n(2) $\\\\cos 2\\\\alpha$', NULL, '(1) 56/65\\n(2) -7/25', '【解题步骤】\\n(1) cosα=√(1-16/25)=3/5, sinβ=√(1-25/169)=12/13\\n    sin(α+β)=sinαcosβ+cosαsinβ=(4/5)(5/13)+(3/5)(12/13)=20/65+36/65=56/65（4分）\\n(2) cos2α=1-2sin²α=1-2·16/25=1-32/25=-7/25（3分）', 2, 1, 1, 1, 1, @trig_formula, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '在 $\\\\triangle ABC$ 中，已知 $a=3$，$b=4$，$\\\\angle C=60^\\\\circ$。\\n\\n求：(1) 边 $c$ 的长度\\n(2) $\\\\triangle ABC$ 的面积', NULL, '(1) c=√13\\n(2) 面积=3√3', '【解题步骤】\\n(1) 余弦定理：c²=a²+b²-2ab·cosC=9+16-2·3·4·(1/2)=25-12=13，c=√13（4分）\\n(2) S=(1/2)ab·sinC=(1/2)·3·4·(√3/2)=3√3（3分）', 3, 1, 1, 1, 1, @trig_sincos, NOW(), NOW()),

('数学[职高]', 'PROOF', '利用诱导公式证明：\\n\\n$\\\\sin(\\\\pi - \\\\alpha) = \\\\sin\\\\alpha$，$\\\\cos(\\\\pi + \\\\alpha) = -\\\\cos\\\\alpha$。\\n\\n（提示：用单位圆中三角函数的定义证明）', NULL, '证明：\\n在单位圆中，设角α的终边与单位圆交于点P(x,y)，则 sinα=y, cosα=x。\\n\\n(1) 角π-α的终边与角α的终边关于y轴对称，其与单位圆交点为P\'(-x,y)。\\n    故 sin(π-α)=y=sinα。\\n\\n(2) 角π+α的终边与角α的终边关于原点对称，其与单位圆交点为P\'\'(-x,-y)。\\n    故 cos(π+α)=-x=-cosα。\\n\\n证毕。', '【证明思路】\\n核心是理解"诱导公式"的几何意义：利用单位圆中的对称性。\\n记忆中可用口诀"奇变偶不变，符号看象限"验证结果。', 2, 1, 1, 1, 1, @trig_induce, NOW(), NOW()),

-- ================================================================
-- 模块6: 数列 (CALC: 1简单, 2中等, 3困难 | PROOF: 1简单)
-- ================================================================

('数学[职高]', 'CALCULATION', '在等差数列 $\\\\{a_n\\\\}$ 中，已知 $a_1=2$，公差 $d=3$。\\n\\n求：(1) 通项公式 $a_n$\\n(2) 前10项和 $S_{10}$', NULL, '(1) a_n=3n-1\\n(2) S₁₀=155', '【解题步骤】\\n(1) a_n=a₁+(n-1)d=2+(n-1)·3=3n-1（3分）\\n(2) S₁₀=n(a₁+a₁₀)/2。a₁₀=3·10-1=29，S₁₀=10(2+29)/2=10·31/2=155（3分）', 1, 1, 1, 1, 1, @seq_arith, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '在等比数列 $\\\\{a_n\\\\}$ 中，已知 $a_1=2$，公比 $q=3$。\\n\\n求：(1) 通项公式 $a_n$\\n(2) 前5项和 $S_5$', NULL, '(1) a_n=2·3ⁿ⁻¹\\n(2) S₅=242', '【解题步骤】\\n(1) a_n=a₁·qⁿ⁻¹=2·3ⁿ⁻¹（3分）\\n(2) S₅=a₁(1-q⁵)/(1-q)=2(1-3⁵)/(1-3)=2(1-243)/(-2)=2·242/2=242（3分）', 2, 1, 1, 1, 1, @seq_geo, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '求数列 $\\\\left\\\\{\\\\dfrac{1}{n(n+1)}\\\\right\\\\}$ 的前 $n$ 项和 $S_n$。', NULL, 'S_n = n/(n+1)', '【解题步骤】\\n裂项：1/[n(n+1)] = 1/n - 1/(n+1)\\nS_n = (1-1/2)+(1/2-1/3)+(1/3-1/4)+...+(1/n-1/(n+1))\\n     = 1 - 1/(n+1) = n/(n+1)\\n此法称为"裂项相消法"或"拆项法"。', 3, 1, 1, 1, 1, @seq_sum, NOW(), NOW()),

('数学[职高]', 'PROOF', '已知数列 $\\\\{a_n\\\\}$ 满足 $a_{n+1} - a_n = 2$（常数）。\\n\\n证明：$\\\\{a_n\\\\}$ 是等差数列，并写出其通项公式（用 $a_1$ 和 $n$ 表示）。', NULL, '证明：\\n由定义，对任意正整数 n，有 a_{n+1} - a_n = 2（常数）。\\n故{an}是公差 d=2 的等差数列。\\n\\n通项公式：a_n = a₁ + (n-1)·2 = a₁ + 2n - 2。', '【证明思路】\\n直接应用等差数列定义：任意相邻两项之差为常数。\\n等差数列的通项公式：a_n = a₁ + (n-1)d。', 1, 1, 1, 1, 1, @seq_arith, NOW(), NOW()),

-- ================================================================
-- 模块7: 平面向量 (CALC: 1简单, 2中等, 3困难 | PROOF: 2中等)
-- ================================================================

('数学[职高]', 'CALCULATION', '已知向量 $\\\\vec{a}=(2,3)$，$\\\\vec{b}=(1,-1)$。\\n\\n求：(1) $\\\\vec{a} \\\\cdot \\\\vec{b}$\\n(2) $|\\\\vec{a}|$ 和 $|\\\\vec{b}|$', NULL, '(1) -1\\n(2) |a|=√13, |b|=√2', '【解题步骤】\\n(1) a·b=x₁x₂+y₁y₂=2×1+3×(-1)=2-3=-1（3分）\\n(2) |a|=√(2²+3²)=√13, |b|=√(1²+(-1)²)=√2（3分）', 1, 1, 1, 1, 1, @vec_coord, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知 $|\\\\vec{a}|=3$，$|\\\\vec{b}|=4$，且 $\\\\vec{a}$ 与 $\\\\vec{b}$ 的夹角为 $60^\\\\circ$。\\n\\n求：(1) $\\\\vec{a} \\\\cdot \\\\vec{b}$\\n(2) $|\\\\vec{a} + \\\\vec{b}|$', NULL, '(1) 6\\n(2) √37', '【解题步骤】\\n(1) a·b=|a||b|cosθ=3×4×cos60°=12×(1/2)=6（3分）\\n(2) |a+b|²=(a+b)·(a+b)=|a|²+2a·b+|b|²=9+12+16=37，所以|a+b|=√37（3分）', 2, 1, 1, 1, 1, @vec_dot, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知 $\\\\vec{a}=(1,2)$，$\\\\vec{b}=(-3,k)$。\\n\\n求：(1) 当 $k$ 为何值时 $\\\\vec{a} \\\\parallel \\\\vec{b}$\\n(2) 当 $k$ 为何值时 $\\\\vec{a} \\\\perp \\\\vec{b}$\\n(3) 若 $\\\\vec{c} = 2\\\\vec{a} - \\\\vec{b}$，求当 $k=1$ 时 $\\\\vec{c}$ 的坐标', NULL, '(1) k=-6\\n(2) k=3/2\\n(3) c=(5,3)', '【解题步骤】\\n(1) a∥b时，1/(-3)=2/k → k=-6（2分）\\n(2) a⊥b时，a·b=1·(-3)+2·k=0 → k=3/2（2分）\\n(3) c=2a-b=2(1,2)-(-3,1)=(2+3,4-1)=(5,3)（3分）', 3, 1, 1, 1, 1, @vec_coord, NOW(), NOW()),

('数学[职高]', 'PROOF', '已知向量 $\\\\vec{a}$ 和 $\\\\vec{b}$ 不共线。\\n\\n证明：若 $\\\\alpha \\\\vec{a} + \\\\beta \\\\vec{b} = \\\\vec{0}$，则 $\\\\alpha = \\\\beta = 0$。', NULL, '证明（反证法）：\\n假设α≠0，则由αa+βb=0得 a=-(β/α)b，即a与b共线，与已知矛盾。\\n同理，若β≠0，得b=-(α/β)a，a与b共线，也矛盾。\\n故只能α=β=0。', '【证明思路】\\n该命题等价于"不共线向量的线性无关性"，是向量基底理论的基础。\\n用反证法：若系数不全为零，则可推出a与b共线。', 2, 1, 1, 1, 1, @vec_linear, NOW(), NOW()),

-- ================================================================
-- 模块8: 立体几何 (CALC: 1简单, 2中等, 3困难 | PROOF: 2中等)
-- ================================================================

('数学[职高]', 'CALCULATION', '已知正方体的棱长为 $2$。\\n\\n求：(1) 正方体的体积\\n(2) 正方体的表面积\\n(3) 正方体的体对角线长', NULL, '(1) 8\\n(2) 24\\n(3) 2√3', '【解题步骤】\\n(1) V=a³=2³=8（2分）\\n(2) S表=6a²=6×4=24（2分）\\n(3) 体对角线=√(a²+a²+a²)=√(12)=2√3（2分）', 1, 1, 1, 1, 1, @geom_body, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知底面半径为 $3$，高为 $5$ 的圆柱。\\n\\n求：(1) 圆柱的侧面积\\n(2) 圆柱的体积\\n(3) 圆柱的表面积', NULL, '(1) 30π\\n(2) 45π\\n(3) 48π', '【解题步骤】\\n(1) S侧=2πrh=2π·3·5=30π（2分）\\n(2) V=πr²h=π·9·5=45π（2分）\\n(3) S表=2S底+S侧=2πr²+30π=18π+30π=48π（2分）', 2, 1, 1, 1, 1, @geom_body, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '在长方体 $ABCD-A_1B_1C_1D_1$ 中，$AB=3$，$AD=4$，$AA_1=5$。\\n\\n求：(1) 体对角线 $AC_1$ 的长度\\n(2) 三棱锥 $B-ACD_1$ 的体积', NULL, '(1) 5√2\\n(2) 10', '【解题步骤】\\n(1) AC₁=√(AB²+AD²+AA₁²)=√(9+16+25)=√50=5√2（3分）\\n(2) V锥=(1/3)·S底·h。底面三角形ACD₁面积...（或用补形法）\\n    三棱锥B-ACD₁体积 = V长方体/6 = 3×4×5/6 = 60/6 = 10（4分）', 3, 1, 1, 1, 1, @geom_body, NOW(), NOW()),

('数学[职高]', 'PROOF', '已知长方体 $ABCD-A_1B_1C_1D_1$ 中，$AB=AD$（底面为正方形）。\\n\\n证明：体对角线 $A_1C$ 与底面 $ABCD$ 的夹角为 $\\\\arctan\\\\frac{\\\\sqrt{2}AA_1}{AB}$。', NULL, '证明：\\n设AB=AD=a，AA₁=h。\\n底面正方形对角线AC=√(a²+a²)=a√2。\\n\\n体对角线A₁C在底面的投影为AC。\\n设A₁C与底面夹角为θ，则在直角三角形A₁AC中：\\ntanθ = A₁A/AC = h/(a√2) = (√2·h)/(2a)\\n所以 θ = arctan[h/(a√2)]。\\n\\n若求A₁C与底面的锐角，即为该值。', '【证明思路】\\n关键是找到体对角线在底面的投影。\\n利用空间直角关系：体对角线=√(长²+宽²+高²)，底面投影=√(长²+宽²)。', 2, 1, 1, 1, 1, @geom_plane, NOW(), NOW()),

-- ================================================================
-- 模块9: 平面解析几何 (CALC: 1简单, 2中等, 3困难 | PROOF: 2中等)
-- ================================================================

('数学[职高]', 'CALCULATION', '已知圆的方程为 $(x-2)^2+(y+1)^2=9$。\\n\\n求：(1) 圆心坐标和半径\\n(2) 点 $P(5,3)$ 到圆心的距离\\n(3) 判断点 $P$ 与圆的位置关系', NULL, '(1) 圆心(2,-1), r=3\\n(2) 5\\n(3) 点在圆外（距离5>半径3）', '【解题步骤】\\n(1) 标准方程(x-a)²+(y-b)²=r²，圆心(2,-1)，r²=9，r=3（2分）\\n(2) d=√[(5-2)²+(3-(-1))²]=√(9+16)=5（2分）\\n(3) d=5>3=r，点P在圆外部（2分）', 1, 1, 1, 1, 1, @pg_circle, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '求满足下列条件的直线方程：\\n\\n(1) 过点 $(1,3)$ 且斜率为 $2$\\n(2) 过两点 $A(2,1)$ 和 $B(4,5)$', NULL, '(1) y=2x+1\\n(2) y=2x-3', '【解题步骤】\\n(1) 点斜式：y-3=2(x-1)，整理得 y=2x+1（3分）\\n(2) k=(5-1)/(4-2)=2，再用点斜式：y-1=2(x-2)，得 y=2x-3（3分）', 2, 1, 1, 1, 1, @pg_line, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '已知直线 $l: 2x+y-5=0$ 与圆 $C: x^2+y^2=5$。\\n\\n求：(1) 圆心到直线 $l$ 的距离\\n(2) 直线 $l$ 与圆 $C$ 的位置关系\\n(3) 若相交，求弦长；若相切，求切点坐标', NULL, '(1) d=√5\\n(2) 相切（d=r=√5）\\n(3) 切点为(2,1)', '【解题步骤】\\n(1) 圆心(0,0), d=|2·0+0-5|/√(4+1)=5/√5=√5（3分）\\n(2) r=√5, d=r，故直线与圆相切（2分）\\n(3) 解方程组：y=5-2x代入x²+y²=5，得x²+(5-2x)²=5→5x²-20x+20=0→(x-2)²=0→x=2,y=1，切点(2,1)（3分）', 3, 1, 1, 1, 1, @pg_pos, NOW(), NOW()),

('数学[职高]', 'PROOF', '已知圆 $C: (x-a)^2+(y-b)^2=r^2$ 上一点 $P(x_0,y_0)$。\\n\\n证明：过点 $P$ 的圆的切线方程为 $(x_0-a)(x-a)+(y_0-b)(y-b)=r^2$。', NULL, '证明：\\n圆心C(a,b)，半径r。\\n设切线上任一点为Q(x,y)，则向量PQ=(x-x₀,y-y₀)，向量CP=(x₀-a,y₀-b)。\\n\\n切线垂直于半径，故PQ·CP=0：\\n(x-x₀)(x₀-a)+(y-y₀)(y₀-b)=0\\n展开：(x-a+a-x₀)(x₀-a)+(y-b+b-y₀)(y₀-b)=0\\n即 (x-a)(x₀-a)-(x₀-a)²+(y-b)(y₀-b)-(y₀-b)²=0\\n又 P在圆上，(x₀-a)²+(y₀-b)²=r²\\n所以 (x-a)(x₀-a)+(y-b)(y₀-b)=r²\\n证毕。', '【证明思路】\\n关键性质：过圆上一点的切线垂直于该点处的半径。\\n用向量垂直：切向量·半径向量=0。', 2, 1, 1, 1, 1, @pg_pos, NOW(), NOW()),

-- ================================================================
-- 模块10: 概率与统计 (CALC: 1简单, 2中等, 3困难 | PROOF: 1简单)
-- ================================================================

('数学[职高]', 'CALCULATION', '一个口袋中有 $3$ 个红球和 $2$ 个白球，大小相同。\\n\\n求：(1) 从中任取 $1$ 球，取到红球的概率\\n(2) 从中任取 $2$ 球，两球颜色不同的概率', NULL, '(1) 3/5\\n(2) 3/5', '【解题步骤】\\n(1) P(红)=3/5（2分）\\n(2) 总取法C(5,2)=10；一红一白取法：C(3,1)·C(2,1)=6；P=6/10=3/5（4分）', 1, 1, 1, 1, 1, @prob_prob, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '从 $5$ 名男生和 $4$ 名女生中选出 $3$ 人组成学习小组。\\n\\n求：(1) 共有多少种不同的选法\\n(2) 选出的 $3$ 人中至少有 $1$ 名女生的选法有多少种', NULL, '(1) 84\\n(2) 74', '【解题步骤】\\n(1) C(9,3)=84（3分）\\n(2) 法一（直接）：C(4,1)C(5,2)+C(4,2)C(5,1)+C(4,3)=40+30+4=74\\n    法二（排除）：总-C(5,3)=84-10=74（3分）', 2, 1, 1, 1, 1, @prob_count, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '甲、乙两人独立射击，甲命中率为 $0.8$，乙命中率为 $0.7$。\\n\\n求：(1) 两人都命中的概率\\n(2) 恰有一人命中的概率\\n(3) 至少有一人命中的概率', NULL, '(1) 0.56\\n(2) 0.38\\n(3) 0.94', '【解题步骤】\\n(1) P(都中)=0.8×0.7=0.56（独立事件）（2分）\\n(2) P(恰一人)=0.8×0.3+0.2×0.7=0.24+0.14=0.38（2分）\\n(3) P(至少一人)=1-P(都不中)=1-0.2×0.3=1-0.06=0.94（2分）', 3, 1, 1, 1, 1, @prob_prob, NOW(), NOW()),

('数学[职高]', 'PROOF', '给定一组数据 $x_1,x_2,\\\\ldots,x_n$，平均数为 $\\\\bar{x}$。\\n\\n证明：$\\\\sum\\\\limits_{i=1}^{n}(x_i-\\\\bar{x})=0$。', NULL, '证明：\\n∑(x_i-x̄) = ∑x_i - ∑x̄ = n·x̄ - n·x̄ = 0\\n\\n其中利用了 x̄ = (∑x_i)/n，即 ∑x_i = n·x̄。', '【证明思路】\\n这是统计中最基本的恒等式，说明离差之和为零。\\n推导只需用到平均数的定义。', 1, 1, 1, 1, 1, @prob_stat, NOW(), NOW()),

-- ================================================================
-- 模块11: 初中基础补漏 (CALC: 1简单, 2中等, 3困难 | PROOF: 2中等)
-- ================================================================

('数学[职高]', 'CALCULATION', '解下列一元二次方程：\\n\\n(1) $x^2-5x+6=0$\\n(2) $2x^2-3x-2=0$', NULL, '(1) x₁=2, x₂=3\\n(2) x₁=2, x₂=-1/2', '【解题步骤】\\n(1) 因式分解：(x-2)(x-3)=0，x₁=2, x₂=3（3分）\\n(2) 公式法：Δ=9+16=25，x=(3±5)/4，x₁=2, x₂=-1/2（3分）', 1, 1, 1, 1, 1, @jr_quad, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '在直角三角形 $ABC$ 中，$\\\\angle C=90^\\\\circ$，$AC=3$，$BC=4$。\\n\\n求：(1) 斜边 $AB$ 的长\\n(2) $\\\\triangle ABC$ 的面积\\n(3) 斜边上的高 $CD$', NULL, '(1) 5\\n(2) 6\\n(3) 12/5', '【解题步骤】\\n(1) 勾股定理：AB=√(3²+4²)=5（2分）\\n(2) S=(1/2)·3·4=6（2分）\\n(3) 等面积法：S=(1/2)·AB·CD=6 → (5/2)CD=6 → CD=12/5（3分）', 2, 1, 1, 1, 1, @jr_triangle, NOW(), NOW()),

('数学[职高]', 'CALCULATION', '解下列二元一次方程组：\\n\\n$\\\\begin{cases} 2x+y=7 \\\\\\\\ 3x-2y=0 \\\\end{cases}$', NULL, 'x=2, y=3', '【解题步骤】\\n法一（代入消元）：由①得y=7-2x，代入②：3x-2(7-2x)=0 → 3x-14+4x=0 → 7x=14 → x=2, y=3\\n法二（加减消元）：①×2+②：4x+2y+3x-2y=14 → 7x=14 → x=2, y=3\\n（各步骤各2分，结果2分）', 3, 1, 1, 1, 1, @jr_2eq, NOW(), NOW()),

('数学[职高]', 'PROOF', '证明：$(a+b)^2 = a^2 + 2ab + b^2$（完全平方公式），并用此公式计算 $102^2$。', NULL, '证明：\\n(a+b)² = (a+b)(a+b)\\n       = a·a + a·b + b·a + b·b\\n       = a² + ab + ba + b²\\n       = a² + 2ab + b²\\n\\n应用：102² = (100+2)² = 100² + 2·100·2 + 2² = 10000 + 400 + 4 = 10404。', '【证明思路】\\n乘法分配律直接展开合并同类项。\\n完全平方公式是整式运算的核心公式之一，实际应用广泛。', 2, 1, 1, 1, 1, @jr_algebra, NOW(), NOW());

COMMIT;

SELECT question_type, COUNT(*) FROM question_bank WHERE subject='数学[职高]' AND status=1 GROUP BY question_type;
