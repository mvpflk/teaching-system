-- ============================================================================
-- v132: 数学[职高] L1+L2+L3教学内容 + 圆锥曲线L4节点补充
-- 幂等安全：INSERT IGNORE + UPDATE
-- ============================================================================
SET @subj = 22;

-- ============================================================
-- Part A: 补齐圆锥曲线L4节点（题库有但知识树无）
-- ============================================================
SET @_pg_id = (SELECT id FROM knowledge_nodes WHERE subject_id=@subj AND name='平面解析几何' AND level=2 LIMIT 1);

-- 新增L3：圆锥曲线
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@_pg_id, @subj, 3, '圆锥曲线', 4, NOW(), NOW());

SET @_cone_l3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_pg_id AND name='圆锥曲线' AND level=3 LIMIT 1);

-- 新增L4节点
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@_cone_l3, @subj, 4, '椭圆的标准方程与性质 [掌握]', 1, NOW(), NOW()),
(@_cone_l3, @subj, 4, '双曲线的标准方程与性质 [掌握]', 2, NOW(), NOW()),
(@_cone_l3, @subj, 4, '抛物线的标准方程与性质 [掌握]', 3, NOW(), NOW());

-- 圆锥曲线L4内容
UPDATE knowledge_nodes SET content =
'【一句话定义】\n椭圆是平面上到两个定点（焦点）的距离之和为常数（大于两焦点距离）的点的轨迹。\n\n【具体说明】\n① 标准方程：焦点在x轴：x²/a²+y²/b²=1(a>b>0)；焦点在y轴：y²/a²+x²/b²=1(a>b>0)。a为长半轴，b为短半轴，c为半焦距，a²=b²+c²。\n② 基本性质：对称轴x轴和y轴，对称中心(0,0)；顶点(±a,0)和(0,±b)；焦点(±c,0)；离心率e=c/a(0<e<1)，e越小椭圆越接近圆。\n③ 通径：过焦点且垂直于长轴的弦，长为2b²/a。\n【常见错误】\n1. 混淆a,b,c关系→椭圆a²=b²+c²(a最大)，双曲线c²=a²+b²(c最大)\n2. 椭圆标准方程不化简→未化为标准形式前误判焦点轴\n\n【考试方向】\n选择题/填空题：求椭圆的标准方程、焦点坐标、离心率。'
WHERE parent_id = @_cone_l3 AND name = '椭圆的标准方程与性质 [掌握]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n双曲线是平面上到两个定点的距离之差的绝对值为常数（小于两焦点距离）的点的轨迹。\n\n【具体说明】\n① 标准方程：焦点在x轴：x²/a²−y²/b²=1(a>0,b>0)；焦点在y轴：y²/a²−x²/b²=1。c²=a²+b²(c最大)。\n② 基本性质：渐近线y=±(b/a)x；顶点(±a,0)；离心率e=c/a(e>1)；对称中心(0,0)。\n③ 等轴双曲线：a=b时，方程为x²−y²=a²，渐近线y=±x互相垂直。\n【常见错误】\n1. 双曲线a,b,c关系记错→双曲线c²=a²+b²(c最大)，椭圆是a²=b²+c²(a最大)\n2. 渐近线方程写错→焦点在x轴y=±(b/a)x，焦点在y轴y=±(a/b)x\n\n【考试方向】\n选择题/填空题：求双曲线方程、渐近线方程、离心率。'
WHERE parent_id = @_cone_l3 AND name = '双曲线的标准方程与性质 [掌握]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n抛物线是平面上到一定点（焦点）和一定直线（准线）距离相等的点的轨迹。\n\n【具体说明】\n① 四种标准方程：y²=2px(焦点(p/2,0)开口向右)；y²=−2px(开口向左)；x²=2py(焦点(0,p/2)开口向上)；x²=−2py(开口向下)，其中p>0为焦准距。\n② 基本性质：顶点在原点(0,0)；对称轴为坐标轴；离心率e=1。\n③ 焦半径公式：抛物线上一点到焦点的距离等于该点到准线的距离。\n【常见错误】\n1. 抛物线开口方向判断错误→y²=2px开口向右(x≥0)；y²=−2px开口向左(x≤0)\n2. 焦点坐标写错→y²=2px的焦点是(p/2,0)不是(p,0)\n\n【考试方向】\n选择题/填空题：求抛物线方程、焦点坐标、准线方程。'
WHERE parent_id = @_cone_l3 AND name = '抛物线的标准方程与性质 [掌握]';

-- ============================================================
-- Part B: L1根节点内容
-- ============================================================
UPDATE knowledge_nodes SET content =
'## 数学[职高]\n\n'
'四川省对口升学考试数学科目，依据教育部《中等职业学校数学课程标准（2020年版）》和《四川省对口升学考试大纲（2014年版）》命制。\n\n'
'**考查模块**：集合、不等式、函数、指数与对数函数、三角函数、数列、平面向量、立体几何、平面解析几何(含圆锥曲线)、概率与统计、导数初步(选考)、初中基础补漏共12个模块。\n\n'
'**能力层级**：了解→理解→掌握（掌握是考查重点）。\n\n'
'**难度分布**：容易约40% / 中等约40% / 较难约20%。\n\n'
'**题型**：单选+多选+判断+填空为主，导数模块偶有简单解答题。'
WHERE subject_id=@subj AND level=1 AND content IS NULL;

-- ============================================================
-- Part C: L2章节内容 (12个)
-- ============================================================
UPDATE knowledge_nodes SET content =
'## 集合\n\n考查集合的表示方法、集合间关系（子集/真子集/相等）和集合的三种基本运算（交集/并集/补集）。以客观题为主，分值约5-8%。\n\n【重难点】集合符号的正确使用、空集的理解、Venn图辅助分析。\n【常见错误】混淆∈（元素与集合）和⊆（集合与集合）；遗忘空集是任何集合的子集。'
WHERE subject_id=@subj AND name='集合' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 不等式\n\n考查不等式的基本性质、一元二次不等式和含绝对值不等式的解法及解集的区间表示。分值约5-10%。\n\n【重难点】一元二次不等式的因式分解法和图像法；绝对值不等式中不等号方向与开闭区间的对应。\n【常见错误】不等式两边乘负数时忘记改变不等号方向；解集写成集合形式时混淆开区间和闭区间。'
WHERE subject_id=@subj AND name='不等式' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 函数\n\n函数是数学核心概念。考查函数定义域值域的求解、单调性和奇偶性的判断、二次函数的图像与最值应用。分值约10-12%。\n\n【重难点】复合函数定义域求法、分段函数、二次函数在闭区间上的最值。\n【常见错误】求定义域漏条件（分母≠0/根号下≥0/真数>0）；判断奇偶性前不验证定义域是否关于原点对称。'
WHERE subject_id=@subj AND name='函数' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 指数与对数函数\n\n考查指数和对数的运算性质、指对数函数的图像特征和简单比较大小。分值约5-8%。\n\n【重难点】换底公式的应用、指对数函数大小比较（利用单调性）、指对数互化。\n【常见错误】混淆指数运算性质（a^m·a^n=a^(m+n)正确；a^m·a^n=a^(mn)错误）；对数运算漏条件（真数>0）。'
WHERE subject_id=@subj AND name='指数与对数函数' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 三角函数\n\n考查任意角与弧度制、三角函数定义、诱导公式、图像与性质、和差倍角公式及正余弦定理。分值最大，约14%。\n\n【重难点】诱导公式的口诀应用("奇变偶不变，符号看象限")、正弦型函数y=Asin(ωx+φ)的图像变换、利用正余弦定理解三角形。\n【常见错误】弧度与角度混淆；诱导公式符号判断错误（未注意象限）；特殊角三角函数值记混。'
WHERE subject_id=@subj AND name='三角函数' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 数列\n\n考查等差等比数列的通项公式和前n项和公式及其应用，以及简单的数列求和技巧。分值约8-10%。\n\n【重难点】等差/等比数列的判定、已知S_n求a_n（a_n=S_n−S_(n-1)，n≥2）、裂项相消法。\n【常见错误】n=1时a₁=S₁的特殊情况漏判；等比数列求和时分母1−q≠0的条件；公式中n的含义弄错。'
WHERE subject_id=@subj AND name='数列' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 平面向量\n\n考查向量的基本运算（加减法、数乘）、数量积的几何意义和坐标表示、向量平行与垂直的判定。分值约5-8%。\n\n【重难点】数量积的坐标公式ā·b̄=x₁x₂+y₁y₂的灵活运用；利用向量法解决几何问题（证明垂直/平行）。\n【常见错误】向量数量积与实数乘法混淆；模长公式√(x²+y²)遗忘开根号；向量夹角公式分母遗漏。'
WHERE subject_id=@subj AND name='平面向量' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 立体几何\n\n考查常见几何体（正方体/长方体/圆柱/圆锥/球）的表面积和体积计算，及简单空间线面关系的判定。分值约5-8%。\n\n【重难点】不规则几何体的计算（分割法/补形法）、线面垂直的判定定理的实际应用。\n【常见错误】常见几何体体积公式混淆（球体积4πR³/3与表面积4πR²混淆）；计算单位不统一。'
WHERE subject_id=@subj AND name='立体几何' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 平面解析几何\n\n考查直线的五种方程形式、圆的方程及直线与圆的位置关系，含圆锥曲线（椭圆/双曲线/抛物线）基础。分值约18%。\n\n【重难点】点到直线距离公式的运用、弦长问题、圆锥曲线标准方程中参数关系的辨析。\n【常见错误】直线方程形式选择不当（根据已知条件选最合适形式）；圆的一般方程判断圆心和半径时忘除以2或开根号。'
WHERE subject_id=@subj AND name='平面解析几何' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 概率与统计\n\n考查分类加法/分步乘法计数原理、排列组合基本计算、古典概型概率和基本统计量的计算。分值约5-8%。\n\n【重难点】排列与组合的区别（有序为排列，无序为组合）、古典概型中基本事件总数的正确计数。\n【常见错误】混淆排列A和组合C的使用场景；概率计算时分母（等可能事件总数）算错；平均数与中位数概念混淆。'
WHERE subject_id=@subj AND name='概率与统计' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 初中基础补漏\n\n为中职数学学习提供初中数学基础回顾，包括实数运算、整式分式、方程、一次函数和三角形基础。非考试直接考查内容，但对后续学习有支撑作用。\n\n【学习建议】重点掌握一元二次方程的解法（因式分解/公式法/配方法）和根的判别式，这些是函数模块的前置知识。'
WHERE subject_id=@subj AND name='初中基础补漏' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 导数初步（选考）\n\n考查导数的几何意义、基本求导公式（幂/三角/指数/对数函数）和利用导数求单调区间与极值。选考模块，部分年份考查。分值约5%。\n\n【重难点】导数与函数单调性的关系（f\'(x)>0↔增）；极值点的判断（驻点且导数符号由正变负为极大值点）。\n【常见错误】求导公式记错（x^n→nx^(n-1)，非n·x）；混淆极值点与驻点的概念。'
WHERE subject_id=@subj AND name='导数初步 [选考]' AND level=2 AND content IS NULL;

SELECT 'v132: 圆锥曲线L4(3个新节点) + L1+L2内容 全部完成！' AS result;
