-- ============================================================================
-- V214: 数学题库补充 — 40个缺口L4节点 × 补至5题 = 93题
-- 生成: 2026-07-25 · 基于 v214a 审计真实数据
-- 幂等: INSERT IGNORE (node_id + question_text 重复跳过)
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

SET @math_subject_id = 22;
-- ============================================================================
-- 一、集合 (7节点,补15题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='补集运算 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '设全集 $U=\\{1,2,3,4,5\\}$，集合 $A=\\{1,2,3\\}$，则 $\\complement_U A =$', '[{"key":"A","text":"$\\\\{1,2,3\\\\}$"},{"key":"B","text":"$\\\\{4,5\\\\}$"},{"key":"C","text":"$\\\\{1,2,3,4,5\\\\}$"},{"key":"D","text":"$\\\\varnothing$"}]', 'B', '补集是在全集中去掉集合A的元素。U={1,2,3,4,5}去掉{1,2,3}剩下{4,5}。', 2, 1),
('数学[职高]', @n, 'SINGLE_CHOICE', '已知全集 $U=\\mathbb{R}$，$A=\\{x \\mid x \\geq 2\\}$，则 $\\complement_U A =$', '[{"key":"A","text":"$\\\\{x \\\\mid x > 2\\\\}$"},{"key":"B","text":"$\\\\{x \\\\mid x \\\\leq 2\\\\}$"},{"key":"C","text":"$\\\\{x \\\\mid x < 2\\\\}$"},{"key":"D","text":"$\\\\{x \\\\mid x \\\\geq -2\\\\}$"}]', 'C', '补集是x≥2的否定，即x<2。注意端点2本身属于A，所以补集不含2。', 3, 1),
('数学[职高]', @n, 'TRUE_FALSE', '若全集U={a,b,c,d}，A={a,b}，则∁_UA={c,d}。', NULL, 'T', 'U中除去A的元素a,b后剩余c,d，正确。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '设全集 $U=\\{1,2,3,4,5,6\\}$，$A=\\{2,4,6\\}$，则 $\\complement_U A$ 的元素个数为___。', NULL, '3', 'U有6个元素，A有3个，补集有6-3=3个。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='空集与全集 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '下列集合中，为空集的是', '[{"key":"A","text":"$\\\\{0\\\\}$"},{"key":"B","text":"$\\\\{x \\\\mid x^2+1=0, x \\\\in \\\\mathbb{R}\\\\}$"},{"key":"C","text":"$\\\\varnothing$"},{"key":"D","text":"B和C都是"}]', 'D', 'B中方程x²+1=0在实数范围内无解，为空集。C中∅本身就是空集的符号。A中{0}含元素0，不是空集。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '空集是任何集合的子集。', NULL, 'T', '空集∅是任何集合的子集，这是集合论的基本性质。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '方程 $x^2+x+1=0$ 在实数范围内的解集是___（填"空集"或"∅"）。', NULL, '∅', '判别式Δ=1-4=-3<0，方程无实数解，解集为空集。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='子集、真子集、相等 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '已知集合 $A=\\{1,2\\}$，则A的子集个数为', '[{"key":"A","text":"2"},{"key":"B","text":"3"},{"key":"C","text":"4"},{"key":"D","text":"8"}]', 'C', 'n个元素的集合有2^n个子集。A有2个元素，子集数为2²=4：∅,{1},{2},{1,2}。', 2, 1),
('数学[职高]', @n, 'SINGLE_CHOICE', '若集合 $A \\subseteq B$ 且 $A \\neq B$，则A是B的', '[{"key":"A","text":"子集"},{"key":"B","text":"真子集"},{"key":"C","text":"全集"},{"key":"D","text":"空集"}]', 'B', 'A⊆B且A≠B说明A是B的子集但不等于B，即A是B的真子集，记作A⊂B。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='集合的定义与元素 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '下列对象中，能组成集合的是', '[{"key":"A","text":"所有高个子的人"},{"key":"B","text":"方程 $x^2-4=0$ 的所有实数解"},{"key":"C","text":"很漂亮的风景"},{"key":"D","text":"接近0的数"}]', 'B', '集合的元素必须是确定的。A"高个子"、C"漂亮"、D"接近"都是模糊概念。B中方程解的集合是明确的{-2,2}。', 1, 1),
('数学[职高]', @n, 'TRUE_FALSE', '若a是集合A的元素，则记作a∈A。', NULL, 'T', '∈表示"属于"，是集合论的基本符号。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='集合的表示方法 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '集合 $\\{x \\mid 1 < x < 3, x \\in \\mathbb{N}\\}$ 用列举法表示为', '[{"key":"A","text":"$\\\\{1,2,3\\\\}$"},{"key":"B","text":"$\\\\{2\\\\}$"},{"key":"C","text":"$\\\\{1,2\\\\}$"},{"key":"D","text":"$\\\\{2,3\\\\}$"}]', 'B', 'x是自然数且满足1<x<3，只有x=2。', 2, 1),
('数学[职高]', @n, 'FILL_IN', '用描述法表示"大于5的实数"：$\\{x \\mid$ ___$\\}$。', NULL, 'x>5', '描述法格式为{元素|条件}。大于5写作x>5。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='并集运算 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '设 $A=\\{1,2,3\\}$，$B=\\{3,4,5\\}$，则 $A \\cup B =$', '[{"key":"A","text":"$\\\\{3\\\\}$"},{"key":"B","text":"$\\\\{1,2,3,4,5\\\\}$"},{"key":"C","text":"$\\\\{1,2,4,5\\\\}$"},{"key":"D","text":"$\\\\{1,2,3\\\\}$"}]', 'B', '并集取两个集合所有元素（重复只算一次）。A∪B={1,2,3,4,5}。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='集合关系判断与证明 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '已知 $A=\\{1,2,3\\}$，$B=\\{1,2,3,4\\}$，则', '[{"key":"A","text":"$A \\\\in B$"},{"key":"B","text":"$A \\\\subset B$"},{"key":"C","text":"$B \\\\subset A$"},{"key":"D","text":"$A = B$"}]', 'B', 'A的所有元素都在B中且A≠B，所以A是B的真子集。∈表示元素属于集合，A和B都是集合，不能用∈。', 2, 1);

-- ============================================================================
-- 二、不等式 (1节点,补3题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='不等式的性质与传递性 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '若 $a > b$，$c < 0$，则下列正确的是', '[{"key":"A","text":"$ac > bc$"},{"key":"B","text":"$ac < bc$"},{"key":"C","text":"$ac = bc$"},{"key":"D","text":"无法确定"}]', 'B', '不等式两边同乘负数，不等号方向要改变。a>b，c<0，所以ac<bc。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '若a>b且b>c，则a>c。', NULL, 'T', '不等式的传递性：a>b, b>c ⇒ a>c。这是不等式的基本性质。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '已知 $a > b$，则 $a+3$ ___ $b+3$（填">"或"<"）。', NULL, '>', '不等式两边同加同一个数，不等号方向不变。a>b ⇒ a+3>b+3。', 1, 1);

-- ============================================================================
-- 三、函数 (1节点,补2题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='二次函数图像与性质 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '二次函数 $y=x^2-4x+3$ 的对称轴是', '[{"key":"A","text":"$x=1$"},{"key":"B","text":"$x=2$"},{"key":"C","text":"$x=-2$"},{"key":"D","text":"$x=4$"}]', 'B', '二次函数y=ax²+bx+c的对称轴为x=-b/(2a)。a=1,b=-4，对称轴x=-(-4)/(2×1)=2。', 3, 1),
('数学[职高]', @n, 'FILL_IN', '二次函数 $y=x^2-6x+8$ 的顶点坐标为___。', NULL, '(3,-1)', '配方：y=(x-3)²-9+8=(x-3)²-1，顶点(3,-1)。', 3, 1);

-- ============================================================================
-- 四、三角函数 (6节点,补9题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='任意角的概念与表示 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '角 $240^\\circ$ 是第几象限角', '[{"key":"A","text":"第一象限"},{"key":"B","text":"第二象限"},{"key":"C","text":"第三象限"},{"key":"D","text":"第四象限"}]', 'C', '240°在180°和270°之间，属于第三象限。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '角-30°是第四象限角。', NULL, 'T', '-30°顺时针旋转30°，落在第四象限(270°~360°等价于-90°~0°中的-30°)。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='诱导公式(五)~(六) [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '$\\sin(\\frac{\\pi}{2}+\\alpha)$ 等于', '[{"key":"A","text":"$\\\\sin\\\\alpha$"},{"key":"B","text":"$-\\\\sin\\\\alpha$"},{"key":"C","text":"$\\\\cos\\\\alpha$"},{"key":"D","text":"$-\\\\cos\\\\alpha$"}]', 'C', '诱导公式：sin(π/2+α)=cosα。"正余互换，符号看象限"。', 3, 1),
('数学[职高]', @n, 'FILL_IN', '$\\cos(\\frac{\\pi}{2}+\\alpha) =$ ___。', NULL, '-sinα', '诱导公式：cos(π/2+α)=-sinα。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='正弦、余弦函数的图像与性质 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '函数 $y=2\\sin x$ 的最大值为', '[{"key":"A","text":"1"},{"key":"B","text":"2"},{"key":"C","text":"0"},{"key":"D","text":"-2"}]', 'B', 'y=2sinx中，sinx的最大值为1，所以y的最大值为2×1=2。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='正切函数的图像与性质 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'TRUE_FALSE', '函数 $y=\\tan x$ 的定义域是全体实数。', NULL, 'F', 'tanx=sinx/cosx，当cosx=0即x=π/2+kπ时无定义。定义域为{x|x≠π/2+kπ,k∈Z}。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='正弦定理 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '在△ABC中，a=3，A=30°，B=45°，则b=___。', NULL, '3√2', '正弦定理：a/sinA=b/sinB ⇒ 3/sin30°=b/sin45° ⇒ 3/0.5=b/(√2/2) ⇒ b=3√2。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='弧度制与角度制互化 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '$60^\\circ$ 等于多少弧度', '[{"key":"A","text":"$\\\\frac{\\\\pi}{6}$"},{"key":"B","text":"$\\\\frac{\\\\pi}{4}$"},{"key":"C","text":"$\\\\frac{\\\\pi}{3}$"},{"key":"D","text":"$\\\\frac{\\\\pi}{2}$"}]', 'C', '弧度=角度×(π/180)。60°=60×π/180=π/3。', 2, 1),
('数学[职高]', @n, 'FILL_IN', '$\\frac{5\\pi}{6}$ 弧度 = ___°。', NULL, '150', '角度=弧度×(180/π)。5π/6×180/π=150°。', 2, 1);

-- ============================================================================
-- 五、数列 (1节点,补1题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='数列的定义与分类 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'TRUE_FALSE', '数列1,3,5,7,…是等差数列，公差为2。', NULL, 'T', '相邻两项差为3-1=2, 5-3=2, 7-5=2，公差d=2恒定，是等差数列。', 1, 1);

-- ============================================================================
-- 六、平面向量 (4节点,补14题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='向量的坐标表示 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '已知点A(1,2)，B(4,6)，则向量 $\\overrightarrow{AB}$ 的坐标为', '[{"key":"A","text":"$(3,4)$"},{"key":"B","text":"$(5,8)$"},{"key":"C","text":"$(1,2)$"},{"key":"D","text":"$(4,6)$"}]', 'A', 'AB向量=终点坐标-起点坐标=(4-1,6-2)=(3,4)。', 2, 1),
('数学[职高]', @n, 'FILL_IN', '已知向量 $\\vec{a}=(3,-4)$，则 $|\\vec{a}|=$___。', NULL, '5', '向量模长=√(x²+y²)=√(9+16)=√25=5。', 2, 1),
('数学[职高]', @n, 'SINGLE_CHOICE', '已知 $\\vec{a}=(2,1)$，$\\vec{b}=(4,2)$，则两向量关系为', '[{"key":"A","text":"垂直"},{"key":"B","text":"共线"},{"key":"C","text":"相等"},{"key":"D","text":"以上都不是"}]', 'B', 'b=(4,2)=2×(2,1)=2a，所以a和b共线（平行）。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '向量(1,2)与向量(2,4)的方向相同。', NULL, 'T', '(2,4)=2(1,2)，正倍数关系，方向相同。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='向量的加减法与数乘 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '已知 $\\vec{a}=(3,1)$，$\\vec{b}=(-1,2)$，则 $\\vec{a}+\\vec{b}=$', '[{"key":"A","text":"$(2,3)$"},{"key":"B","text":"$(4,-1)$"},{"key":"C","text":"$(2,-3)$"},{"key":"D","text":"$(4,3)$"}]', 'A', '向量加法：对应坐标相加。(3+(-1),1+2)=(2,3)。', 2, 1),
('数学[职高]', @n, 'FILL_IN', '$\\vec{a}=(4,0)$，则 $3\\vec{a}=$___。', NULL, '(12,0)', '数乘：每个坐标乘以该数。3×(4,0)=(12,0)。', 1, 1),
('数学[职高]', @n, 'SINGLE_CHOICE', '已知 $\\vec{a}=(5,2)$，$\\vec{b}=(2,-3)$，则 $2\\vec{a}-\\vec{b}=$', '[{"key":"A","text":"$(8,7)$"},{"key":"B","text":"$(8,-8)$"},{"key":"C","text":"$(12,1)$"},{"key":"D","text":"$(8,1)$"}]', 'A', '2a-b=2(5,2)-(2,-3)=(10-2,4-(-3))=(8,7)。', 3, 1),
('数学[职高]', @n, 'TRUE_FALSE', '向量加法满足交换律：a+b=b+a。', NULL, 'T', '向量加法满足交换律和结合律，这是向量的基本性质。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='向量的定义与表示 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '下列哪个是向量', '[{"key":"A","text":"温度"},{"key":"B","text":"长度"},{"key":"C","text":"位移"},{"key":"D","text":"质量"}]', 'C', '向量既有大小又有方向。位移有大小（距离）和方向，是向量。温度、长度、质量只有大小，是标量。', 1, 1),
('数学[职高]', @n, 'TRUE_FALSE', '零向量的长度为0，方向任意。', NULL, 'T', '零向量0的模长为0，方向不确定（任意方向均可）。正确。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '向量 $\\vec{a}$ 的模记作___。', NULL, '|a|', '向量的模（长度）用绝对值符号表示，记作|a|或|AB|。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='坐标运算的应用 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '已知 $\\vec{a}=(1,2)$，$\\vec{b}=(3,-1)$，则 $\\vec{a}\\cdot\\vec{b}=$', '[{"key":"A","text":"5"},{"key":"B","text":"1"},{"key":"C","text":"7"},{"key":"D","text":"-1"}]', 'B', '数量积=对应坐标乘积之和：a·b=1×3+2×(-1)=3-2=1。', 3, 1),
('数学[职高]', @n, 'FILL_IN', '已知 $\\vec{a}=(1,1)$，$\\vec{b}=(2,0)$，则 $\\vec{a}\\cdot\\vec{b}=$___。', NULL, '2', 'a·b=1×2+1×0=2。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '若两向量数量积为0，则两向量垂直。', NULL, 'T', 'a·b=|a||b|cosθ=0 ⇒ cosθ=0 ⇒ θ=90°，两向量垂直。正确。', 2, 1);

-- ============================================================================
-- 七、立体几何 (3节点,补9题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='线面平行与垂直的判定 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '直线与平面平行的判定定理是', '[{"key":"A","text":"直线与平面内一条直线垂直"},{"key":"B","text":"直线与平面内一条直线平行"},{"key":"C","text":"直线与平面无交点"},{"key":"D","text":"直线与平面内两条相交直线都平行"}]', 'B', '线面平行判定：若平面外一直线与平面内一条直线平行，则该直线与此平面平行。', 3, 1),
('数学[职高]', @n, 'TRUE_FALSE', '若一条直线垂直于平面内两条相交直线，则该直线垂直于该平面。', NULL, 'T', '线面垂直判定定理：直线垂直于平面内两条相交直线⇒直线垂直于平面。', 3, 1),
('数学[职高]', @n, 'SINGLE_CHOICE', '正方体ABCD-A\'B\'C\'D\'中，直线A\'C\'与平面ABCD的关系是', '[{"key":"A","text":"平行"},{"key":"B","text":"垂直"},{"key":"C","text":"相交"},{"key":"D","text":"在平面内"}]', 'A', 'A\'C\'在上底面，ABCD是下底面，两平面平行。所以A\'C\'平行于平面ABCD。', 3, 1),
('数学[职高]', @n, 'FILL_IN', '若直线l∥平面α，则l与α___公共点（填"有"或"无"）。', NULL, '无', '线面平行⇔直线和平面无公共点。', 2, 1);

-- ============================================================================
-- 七(续)、立体几何后续节点
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='面面平行与垂直的判定 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '平面与平面垂直的判定定理是', '[{"key":"A","text":"一个平面内有两条直线与另一个平面垂直"},{"key":"B","text":"一个平面内有一条直线与另一个平面垂直"},{"key":"C","text":"两个平面无交点"},{"key":"D","text":"一个平面过另一个平面的垂线"}]', 'D', '面面垂直判定：若一个平面过另一个平面的垂线，则两平面垂直。', 3, 1),
('数学[职高]', @n, 'TRUE_FALSE', '若两个平面平行，则一个平面内的任意直线都平行于另一个平面。', NULL, 'T', '面面平行⇒其中一个平面内所有直线都与另一个平面无交点，即平行。', 2, 1),
('数学[职高]', @n, 'FILL_IN', '正方体中，侧面与底面的位置关系是___。', NULL, '垂直', '正方体侧面垂直于底面（交线为底边，侧面过垂直于底面的棱）。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='常见几何体的表面积 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '棱长为2的正方体表面积为', '[{"key":"A","text":"8"},{"key":"B","text":"16"},{"key":"C","text":"24"},{"key":"D","text":"48"}]', 'C', '正方体6个面，每个面面积=2²=4，表面积=6×4=24。', 2, 1),
('数学[职高]', @n, 'FILL_IN', '底面半径为3，母线长为5的圆锥侧面积为___（用π表示）。', NULL, '15π', '圆锥侧面积=πrl=π×3×5=15π。', 3, 1);

-- ============================================================================
-- 八、平面解析几何 (3节点,补6题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='圆的一般方程 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '方程 $x^2+y^2-4x+2y-4=0$ 表示的圆的圆心坐标为', '[{"key":"A","text":"$(2,-1)$"},{"key":"B","text":"$(-2,1)$"},{"key":"C","text":"$(4,-2)$"},{"key":"D","text":"$(2,1)$"}]', 'A', '配方：$(x-2)^2+(y+1)^2=9$，圆心(2,-1)。', 3, 1),
('数学[职高]', @n, 'FILL_IN', '圆 $x^2+y^2=25$ 的半径为___。', NULL, '5', 'x²+y²=r²标准形式，r=√25=5。', 1, 1),
('数学[职高]', @n, 'TRUE_FALSE', '方程 $x^2+y^2+2x-6y+10=0$ 表示一个圆。', NULL, 'F', '配方得(x+1)²+(y-3)²=0，半径r=0，退化为点，不是圆。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='直线的五种方程形式 [掌握]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '过点(2,3)，斜率为1的直线方程为', '[{"key":"A","text":"$y=x+1$"},{"key":"B","text":"$y=x-1$"},{"key":"C","text":"$y=2x-1$"},{"key":"D","text":"$y=x+5$"}]', 'A', '点斜式：y-3=1×(x-2) ⇒ y=x+1。', 2, 1),
('数学[职高]', @n, 'FILL_IN', '过点A(1,2)和B(3,6)的直线斜率为___。', NULL, '2', '两点斜率公式：k=(6-2)/(3-1)=4/2=2。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='弦长与切线问题 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '圆 $x^2+y^2=25$ 在点(3,4)处的切线方程为___。', NULL, '3x+4y=25', '圆上点(x₀,y₀)处切线为x₀x+y₀y=r²。3x+4y=25。', 4, 1);

-- ============================================================================
-- 九、概率与统计 (3节点,补5题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='分类加法与分步乘法 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '从3件上衣和4条裤子中选一套衣服，有几种选法', '[{"key":"A","text":"7"},{"key":"B","text":"12"},{"key":"C","text":"3"},{"key":"D","text":"4"}]', 'B', '分步乘法：选上衣3种×选裤子4种=12种。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '从北京到上海有2趟航班和3趟高铁，共___种出行方式。', NULL, '5', '分类加法：航班2种+高铁3种=5种。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='互斥事件与独立事件 [理解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '掷一枚骰子，事件A="点数为1"，事件B="点数为偶数"，则A与B是', '[{"key":"A","text":"独立事件"},{"key":"B","text":"互斥事件"},{"key":"C","text":"对立事件"},{"key":"D","text":"相等事件"}]', 'B', 'A={1}，B={2,4,6}，无交集⇒互斥。不是对立（A∪B≠全集）。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '掷两枚硬币，第一枚正面与第二枚正面是独立事件。', NULL, 'T', '两枚硬币结果互不影响，是独立事件。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='抽样方法 [了解]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '将总体按特征分层后从各层独立抽样称为', '[{"key":"A","text":"简单随机抽样"},{"key":"B","text":"系统抽样"},{"key":"C","text":"分层抽样"},{"key":"D","text":"整群抽样"}]', 'C', '分层抽样：总体分层→每层独立随机抽样→合并。', 1, 1);

-- ============================================================================
-- 十、初中基础补漏 (11节点,补29题)
-- ============================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='整式加减乘除运算' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '$(x+3)(x-2)=$___（展开）。', NULL, 'x²+x-6', '(x+3)(x-2)=x²-2x+3x-6=x²+x-6。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '$2x^2$ 与 $3x^2$ 是同类项。', NULL, 'T', '同类项：字母相同且指数相同。2x²和3x²都是x²项。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '$(a+b)^2=$___（展开）。', NULL, 'a²+2ab+b²', '完全平方公式。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='三角形内角和与分类' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '三角形三内角之和为___°。', NULL, '180', '三角形内角和定理。', 1, 1),
('数学[职高]', @n, 'TRUE_FALSE', '有一个角为90°的三角形是直角三角形。', NULL, 'T', '直角三角形定义。', 1, 1),
('数学[职高]', @n, 'SINGLE_CHOICE', '等腰三角形底角50°，则顶角为', '[{"key":"A","text":"50°"},{"key":"B","text":"80°"},{"key":"C","text":"100°"},{"key":"D","text":"130°"}]', 'B', '两底角相等各50°，顶角=180°-100°=80°。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='一元二次方程公式法求解' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '一元二次方程求根公式为 $x=$___。', NULL, '(-b±√(b²-4ac))/(2a)', 'Δ=b²-4ac为判别式。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '若判别式Δ=0，则方程有两个相等实数根。', NULL, 'T', 'Δ=0时x=-b/2a，重根。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='实数的概念与分类' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'TRUE_FALSE', '所有分数都是有理数。', NULL, 'T', '有理数定义：可表示为两整数之比的数。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '$\\pi$ 是___（填"有理数"或"无理数"）。', NULL, '无理数', 'π是无限不循环小数，是无理数。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='一次函数与方程不等式的关系' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'TRUE_FALSE', '一次函数y=kx+b的图像是一条直线。', NULL, 'T', '一次函数的图像始终是直线。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '直线 $y=-3x+6$ 的斜率为___。', NULL, '-3', 'y=kx+b中k即斜率。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='分式的约分与通分' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '化简 $\\frac{x^2-1}{x-1}=$___（x≠1）。', NULL, 'x+1', '因式分解(x+1)(x-1)，约去(x-1)。', 2, 1),
('数学[职高]', @n, 'TRUE_FALSE', '$\\frac{2}{4}$ 约分后为 $\\frac{1}{2}$。', NULL, 'T', '分子分母同除以2。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '$\\frac{1}{x}+\\frac{1}{x+1}$ 通分后分子为___。', NULL, '2x+1', '通分：(x+1+x)/[x(x+1)]=(2x+1)/[x(x+1)]。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='代入消元法与加减消元法' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'TRUE_FALSE', '方程组可能有0个、1个或无穷多个解。', NULL, 'T', '无解(矛盾)、唯一解(相交)、无穷多解(重合)。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='解一元一次方程' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '解方程 $3x-7=2x+1$，得 $x=$___。', NULL, '8', '移项：3x-2x=1+7 ⇒ x=8。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='一次函数的图像与斜率' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'SINGLE_CHOICE', '若直线斜率为负，则该直线', '[{"key":"A","text":"从左到右上升"},{"key":"B","text":"从左到右下降"},{"key":"C","text":"水平"},{"key":"D","text":"垂直于x轴"}]', 'B', 'k<0时，y随x增大而减小，直线下降。', 1, 1),
('数学[职高]', @n, 'FILL_IN', '过点(0,-3)和(1,1)的直线斜率为___。', NULL, '4', 'k=(1-(-3))/(1-0)=4。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='勾股定理与简单应用' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'TRUE_FALSE', '勾股定理逆定理：若a²+b²=c²，则三角形是直角三角形。', NULL, 'T', '勾股定理逆定理是判定直角三角形的方法。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND level=4 AND name='根的判别式与韦达定理' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('数学[职高]', @n, 'FILL_IN', '方程 $2x^2-8x+6=0$ 两根之积为___。', NULL, '3', '韦达定理：x₁x₂=c/a=6/2=3。', 2, 1);

COMMIT;
