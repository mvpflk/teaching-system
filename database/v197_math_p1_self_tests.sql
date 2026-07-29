-- ============================================================================
-- v197: 数学[职高] P1-3 即时自测 — 核心章节 L4 节点 content 追加【即时自测】
-- 覆盖: 函数(8) + 指数对数(4) + 三角函数(12) + 数列(8) + 解析几何核心(6) = 38节点
-- 每节点追加 1-2 道选择题 + 答案 + 简短解析
-- 幂等: 用 CONCAT 追加，使用 WHERE content NOT LIKE '%【即时自测】%' 防重复
-- ============================================================================
SET NAMES utf8mb4;

-- ══════════════════════════════════════════
-- 函数模块 (8节点)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 函数f(x)=√(x+3)的定义域是？\nA. (-3,+∞)  B. [-3,+∞)  C. (3,+∞)  D. [-3,+∞)\n答案：B。被开方数x+3≥0，x≥-3，闭区间。\n\n2. 若f(2x+1)=4x+5，则f(1)=？\n答案：将x=0代入，f(1)=5。')
WHERE subject_id=22 AND level=4 AND name LIKE '%函数定义与定义域%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 分段函数是一个函数还是多个函数？\nA. 一个  B. 多个  C. 看情况  D. 以上都不对\n答案：A。分段函数是一个函数的不同区间表示。\n\n2. f(x)=|x|可以写成什么形式？\n答案：f(x)=x(x≥0)或-x(x<0)。')
WHERE subject_id=22 AND level=4 AND name LIKE '%函数的表示方法%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 函数y=x²的值域是？\nA. R  B. [0,+∞)  C. (0,+∞)  D. (-∞,0]\n答案：B。平方恒≥0。\n\n2. f(x)=1/(x-1)+2的值域是？\n答案：{y|y≠2}。分离常数法。')
WHERE subject_id=22 AND level=4 AND name LIKE '%函数值域求解%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. f(x)=2x+1在R上的单调性是？\nA. 递增  B. 递减  C. 先增后减  D. 不单调\n答案：A。k=2>0，一次函数单调递增。\n\n2. 复合函数"同增异减"口诀的含义是？\n答案：内外层单调性相同则复合为增，相反则复合为减。')
WHERE subject_id=22 AND level=4 AND name LIKE '%函数单调性判断%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. f(x)=x³是？\nA. 奇函数  B. 偶函数  C. 非奇非偶  D. 既是奇又是偶\n答案：A。f(-x)=-x³=-f(x)。\n\n2. 判断奇偶性前必须先检验什么？\n答案：定义域是否关于原点对称。不对称直接判为非奇非偶。')
WHERE subject_id=22 AND level=4 AND name LIKE '%函数奇偶性判断%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. y=2x²-4x+1的对称轴是？\nA. x=1  B. x=-1  C. x=2  D. x=-2\n答案：A。x=-b/(2a)=4/4=1。\n\n2. a>0时抛物线开口向？\n答案：向上，函数有最小值。')
WHERE subject_id=22 AND level=4 AND name LIKE '%二次函数图像与性质%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. f(x)=x²-2x+3在[0,3]上的最小值是？\nA. 2  B. 3  C. 6  D. 8\n答案：A。f(x)=(x-1)²+2，x=1时最小值为2。\n\n2. 闭区间上求最值为什么不能只代端点？\n答案：对称轴可能在区间内，顶点处可能取到更值。')
WHERE subject_id=22 AND level=4 AND name LIKE '%二次函数最值问题%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 函数应用题建模的正确顺序是？\nA. 审题→设变量→建模→定义域→求解  B. 设变量→审题→建模→求解→定义域\n答案：A。先审题理解题意再设变量。\n\n2. 利润=？×？\n答案：单件利润×销量。')
WHERE subject_id=22 AND level=4 AND name LIKE '%函数应用题建模%' AND content NOT LIKE '%【即时自测】%';

-- ══════════════════════════════════════════
-- 指数与对数函数 (4节点)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. a³·a²=？\nA. a⁵  B. a⁶  C. a  D. 2a⁵\n答案：A。同底幂相乘指数相加。\n\n2. (2³)²÷2⁴=？\n答案：2⁶÷2⁴=2²=4。')
WHERE subject_id=22 AND level=4 AND name LIKE '%指数运算性质%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. y=2ˣ的图像恒过哪个点？\nA. (0,1)  B. (1,0)  C. (0,0)  D. (1,1)\n答案：A。a⁰=1。\n\n2. 0.5ˣ的单调性是？\n答案：单调递减（0<底数<1）。')
WHERE subject_id=22 AND level=4 AND name LIKE '%指数函数的图像与性质%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. log₂8=？\nA. 2  B. 3  C. 4  D. 16\n答案：B。2³=8。\n\n2. lg100+lg0.01=？\n答案：2+(-2)=0。')
WHERE subject_id=22 AND level=4 AND name LIKE '%对数运算性质%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. y=log₂(x-3)的定义域是？\nA. (3,+∞)  B. [3,+∞)  C. (0,+∞)  D. R\n答案：A。真数x-3>0，即x>3。\n\n2. 对数函数恒过哪个定点？\n答案：(1,0)。因为logₐ1=0。')
WHERE subject_id=22 AND level=4 AND name LIKE '%对数函数的图像与性质%' AND content NOT LIKE '%【即时自测】%';

-- ══════════════════════════════════════════
-- 三角函数 (12节点)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. -30°是第几象限角？\nA. 一  B. 二  C. 三  D. 四\n答案：D。顺时针转30°在第四象限。\n\n2. 与45°终边相同的角是？\n答案：45°+k·360°，k∈Z。')
WHERE subject_id=22 AND level=4 AND name LIKE '%任意角的概念%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 60°=？rad\nA. π/6  B. π/4  C. π/3  D. π/2\n答案：C。60×π/180=π/3。\n\n2. 弧长公式l=？（a为弧度）\n答案：l=|a|·r。')
WHERE subject_id=22 AND level=4 AND name LIKE '%弧度制%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 单位圆中sinα等于？\nA. x坐标  B. y坐标  C. 半径  D. 弧长\n答案：B。sinα是终边与单位圆交点的y坐标。\n\n2. sin²α+cos²α=？\n答案：1。')
WHERE subject_id=22 AND level=4 AND name LIKE '%三角函数的定义%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. sinα=3/5，α为锐角，则cosα=？\nA. 4/5  B. 3/5  C. -4/5  D. 5/3\n答案：A。cosα=√(1-9/25)=4/5。\n\n2. tanα=sinα/cosα这个公式何时不能直接用？\n答案：cosα=0时（α=π/2+kπ）。')
WHERE subject_id=22 AND level=4 AND name LIKE '%同角三角函数基本关系%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. sin(180°-30°)=？\nA. sin30°  B. -sin30°  C. cos30°  D. -cos30°\n答案：A。口诀"奇变偶不变，符号看象限"。\n\n2. cos(π+α)=？\n答案：-cosα。')
WHERE subject_id=22 AND level=4 AND name LIKE '%诱导公式(一)~(四)%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. sin(π/2+α)=？\nA. sinα  B. -sinα  C. cosα  D. -cosα\n答案：C。π/2是π/2的奇数倍，sin变cos。\n\n2. cos(3π/2-α)=？\n答案：-sinα。')
WHERE subject_id=22 AND level=4 AND name LIKE '%诱导公式(五)~(六)%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. y=sinx的最小正周期是？\nA. π  B. 2π  C. π/2  D. 4π\n答案：B。sin(x+2π)=sinx。\n\n2. y=cosx的值域是？\n答案：[-1,1]。')
WHERE subject_id=22 AND level=4 AND name LIKE '%正弦、余弦函数的图像与性质%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. y=tanx的定义域是？\nA. R  B. {x|x≠kπ+π/2}  C. {x|x≠kπ}  D. [0,π]\n答案：B。cosx≠0即x≠kπ+π/2。\n\n2. tanx的最小正周期是？\n答案：π。')
WHERE subject_id=22 AND level=4 AND name LIKE '%正切函数的图像与性质%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. sin(α+β)的展开公式中sinαcosβ前面的符号是？\nA. +  B. -\n答案：A。sin(α+β)=sinαcosβ+cosαsinβ。\n\n2. cos(α-β)中间符号与括号中符号的关系是？\n答案：相反。cos公式中间符号与括号反号。')
WHERE subject_id=22 AND level=4 AND name LIKE '%两角和与差的正弦、余弦公式%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. sin2α=？\nA. 2sinα  B. sinαcosα  C. 2sinαcosα  D. sin²α\n答案：C。倍角公式sin2α=2sinαcosα。\n\n2. cos2α有几种等价形式？\n答案：3种。cos²α-sin²α=2cos²α-1=1-2sin²α。')
WHERE subject_id=22 AND level=4 AND name LIKE '%二倍角公式%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 正弦定理的内容是？\nA. a/sinA=b/sinB=c/sinC  B. a²=b²+c²-2bccosA\n答案：A。各边与其对角的正弦之比相等，等于外接圆直径2R。\n\n2. 已知两角和一边，能否解三角形？\n答案：能。用正弦定理。')
WHERE subject_id=22 AND level=4 AND name LIKE '%正弦定理%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 余弦定理求角C的公式是？\nA. cosC=(a²+b²-c²)/(2ab)  B. cosC=(a²+b²+c²)/(2ab)\n答案：A。余弦定理变形：cosC=(a²+b²-c²)/(2ab)。\n\n2. 已知三边，能否求角？\n答案：能。用余弦定理。')
WHERE subject_id=22 AND level=4 AND name LIKE '%余弦定理%' AND content NOT LIKE '%【即时自测】%';

-- ══════════════════════════════════════════
-- 数列 (8节点)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 数列{a_n}中，a_n=2n-1，则a₅=？\nA. 7  B. 8  C. 9  D. 10\n答案：C。a₅=2×5-1=9。\n\n2. 给出前几项写通项公式的关键是？\n答案：找规律。观察n与项的数值关系。')
WHERE subject_id=22 AND level=4 AND name LIKE '%数列的定义与分类%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. a₁=2,d=3，则a₅=？\nA. 14  B. 15  C. 16  D. 17\n答案：A。a₅=2+4×3=14。\n\n2. 等差数列通项公式？\n答案：a_n=a₁+(n-1)d。')
WHERE subject_id=22 AND level=4 AND name LIKE '%等差数列通项公式%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. a₁=1,a₁₀=19，则S₁₀=？\nA. 100  B. 95  C. 200  D. 190\n答案：A。S₁₀=10(1+19)/2=100。\n\n2. 等差数列前n项和公式？\n答案：S_n=n(a₁+a_n)/2=na₁+n(n-1)d/2。')
WHERE subject_id=22 AND level=4 AND name LIKE '%等差数列前n项和%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. a₁=2,q=3，则a₄=？\nA. 18  B. 54  C. 162  D. 27\n答案：B。a₄=2×3³=54。\n\n2. 等比数列公比q可以为0吗？\n答案：不可以。q=0从第二项起全为0，不构成等比数列。')
WHERE subject_id=22 AND level=4 AND name LIKE '%等比数列通项公式%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. a₁=1,q=2，则S₅=？\nA. 31  B. 15  C. 63  D. 32\n答案：A。S₅=1×(2⁵-1)/(2-1)=31。\n\n2. 等比数列求和公式中分母是什么？\n答案：1-q（q≠1）。')
WHERE subject_id=22 AND level=4 AND name LIKE '%等比数列前n项和%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 分组求和法适用于什么数列？\nA. 等差+等比混合  B. 只有等差  C. 只有等比  D. 任意数列\n答案：A。将数列拆成几个可分别求和的子数列。\n\n2. 数列{n+2ⁿ}用什么方法求和？\n答案：分组求和法（等差+等比）。')
WHERE subject_id=22 AND level=4 AND name LIKE '%分组求和法%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 1/(1×2)+1/(2×3)=？\nA. 1/3  B. 2/3  C. 1/2  D. 3/4\n答案：B。(1-1/2)+(1/2-1/3)=1-1/3=2/3。\n\n2. 裂项相消法的核心思路是？\n答案：将通项拆成两项之差，求和时中间项抵消。')
WHERE subject_id=22 AND level=4 AND name LIKE '%裂项相消法%' AND content NOT LIKE '%【即时自测】%';

-- ══════════════════════════════════════════
-- 解析几何核心 (6节点)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 过(1,2)和(3,6)的直线斜率k=？\nA. 1  B. 2  C. 3  D. 4\n答案：B。k=(6-2)/(3-1)=2。\n\n2. 倾斜角α与斜率k的关系？\n答案：k=tanα（α≠90°）。')
WHERE subject_id=22 AND level=4 AND name LIKE '%直线的倾斜角与斜率%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 过点(1,2)斜率为3的直线方程（点斜式）是？\nA. y-2=3(x-1)  B. y+2=3(x+1)\n答案：A。y-y₀=k(x-x₀)。\n\n2. 直线的五种方程形式中哪种最适合已知两点？\n答案：两点式或先求斜率再用点斜式。')
WHERE subject_id=22 AND level=4 AND name LIKE '%直线的五种方程形式%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 圆(x-2)²+(y+3)²=16的圆心和半径是？\nA. (2,-3),4  B. (-2,3),16  C. (2,-3),16  D. (-2,3),4\n答案：A。圆心(2,-3)，r²=16，r=4。\n\n2. 圆的标准方程一般形式？\n答案：(x-a)²+(y-b)²=r²。')
WHERE subject_id=22 AND level=4 AND name LIKE '%圆的标准方程%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 点P(3,4)到直线3x-4y+5=0的距离是？\nA. 2  B. -2  C. 0.4  D. 4\n答案：C。d=|9-16+5|/5=|-2|/5=0.4。\n\n2. 直线与圆相交时，弦长公式？\n答案：弦长=2√(r²-d²)，d为圆心到直线的距离。')
WHERE subject_id=22 AND level=4 AND name LIKE '%直线与圆的位置关系判断%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 椭圆x²/25+y²/16=1的长半轴a=？\nA. 4  B. 5  C. 3  D. 25\n答案：B。a²=25，a=5。\n\n2. 椭圆上任意点到两焦点的距离之和=？\n答案：2a（常数）。')
WHERE subject_id=22 AND level=4 AND name LIKE '%椭圆的标准方程%' AND content NOT LIKE '%【即时自测】%';

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n1. 抛物线y²=8x的焦点到准线的距离p=？\nA. 2  B. 4  C. 8  D. 16\n答案：B。2p=8，p=4。\n\n2. 抛物线上点到焦点的距离等于？\n答案：该点到准线的距离。')
WHERE subject_id=22 AND level=4 AND name LIKE '%抛物线的标准方程%' AND content NOT LIKE '%【即时自测】%';

SELECT 'v197 deployed — 38核心节点即时自测已追加' AS result;
-- ============================================================================
