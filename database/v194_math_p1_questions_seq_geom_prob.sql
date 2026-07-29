-- ============================================================================
-- v194: 数学[职高] P1-1 补题 — 数列 + 解析几何 + 概率统计 (26题)
-- 数列: 3132定义分类(4) + 3137分组求和(4) + 3138裂项相消(4) = 12题
-- 解析几何: 3161弦长切线(4) + 3192双曲线(4) + 3193抛物线(4) = 12题
-- 概率统计: 3169抽样方法(2) = 2题
-- 难度: 每节点 easy(1) 2道 + medium(2) 1~2道 + hard(3) 1道
-- 幂等：INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ══════════════════════════════════════════
-- 数列: 3节点 × 4题 = 12题
-- ══════════════════════════════════════════

-- === 3132: 数列的定义与分类 [了解] ===
SET @n3132 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='数列的定义与分类 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '下列是数列 $\\\\{a_n\\\\}$ 通项公式的是：',
 '["A. $a_1=1, a_2=2$","B. $a_n = 2n-1$","C. $a_1+a_2=5$","D. $S_n=n^2$"]', 'B',
 '通项公式是用n表示第n项的式子。A是列举前两项；C是和的关系；D是前n项和公式。B中a_n=2n-1可以直接算出任意一项。', 1, 1, 1, 1, 1, @n3132, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '已知数列 $\\\\{a_n\\\\}$ 的通项公式为 $a_n = n^2$，则 $a_3 = $',
 '["A. 6","B. 8","C. 9","D. 12"]', 'C',
 'a₃=3²=9。通项公式中的n代入3即可得到第3项。', 1, 1, 1, 1, 1, @n3132, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '数列 1, 3, 5, 7, 9, ... 的一个通项公式是：',
 '["A. $a_n = 2n$","B. $a_n = 2n-1$","C. $a_n = 2n+1$","D. $a_n = n+2$"]', 'B',
 'n=1时a₁=2×1-1=1；n=2时a₂=3；n=3时a₃=5...符合。这是首项为1公差为2的等差数列。', 1, 1, 1, 1, 1, @n3132, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '已知数列 $a_1=2$，$a_{n+1}=a_n+3$，则 $a_4 = $ _____。',
 '[]', '11',
 '递推：a₂=2+3=5，a₃=5+3=8，a₄=8+3=11。递推公式给出相邻项的关系。', 2, 1, 1, 1, 1, @n3132, NOW(), NOW());

-- === 3137: 分组求和法 [理解] ===
SET @n3137 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='分组求和法 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '数列 $1, -2, 3, -4, 5$ 的前5项和为：',
 '["A. 0","B. 3","C. 5","D. 15"]', 'B',
 'S₅=1+(-2)+3+(-4)+5=(1-2)+(3-4)+5=(-1)+(-1)+5=3。将正负项分组求和。', 1, 1, 1, 1, 1, @n3137, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '数列 $\\\\{2^n+3^n\\\\}$ 的前n项和应分别对两部分求和，这种方法称为：',
 '["A. 裂项相消法","B. 分组求和法","C. 倒序相加法","D. 错位相减法"]', 'B',
 '当数列由几个可分别求和的子数列组成时，将各项分组分别求和再相加，称为分组求和法。2ⁿ和3ⁿ分别是等比数列。', 1, 1, 1, 1, 1, @n3137, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '数列 $1, 1+2, 1+2+3, \\\\ldots$ 的第n项可以写成等差数列求和，则 $a_n = $ _____（用n表示）。',
 '[]', 'n(n+1)/2',
 'a_n=1+2+3+...+n=n(n+1)/2。这是将每组内部视为等差数列求和后再看整体规律。', 2, 1, 1, 1, 1, @n3137, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '求数列 $\\\\{n+2^n\\\\}$ 的前 $n$ 项和，其中 $\\\\sum n = \\\\dfrac{n(n+1)}{2}$，$\\\\sum 2^n = 2^{n+1}-2$，则 $S_n = $ _____。',
 '[]', 'n(n+1)/2+2^(n+1)-2',
 '分组求和：S_n=(1+2+...+n)+(2+2²+...+2ⁿ)=n(n+1)/2+(2^(n+1)-2)。', 3, 1, 1, 1, 1, @n3137, NOW(), NOW());

-- === 3138: 裂项相消法 [理解] ===
SET @n3138 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='裂项相消法 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '$\\\\dfrac{1}{1 \\\\times 2} + \\\\dfrac{1}{2 \\\\times 3} + \\\\dfrac{1}{3 \\\\times 4}$ 的值为：',
 '["A. $\\\\dfrac{1}{4}$","B. $\\\\dfrac{3}{4}$","C. $\\\\dfrac{2}{3}$","D. $\\\\dfrac{1}{3}$"]', 'B',
 '裂项：1/(n(n+1))=1/n-1/(n+1)。原式=(1-1/2)+(1/2-1/3)+(1/3-1/4)=1-1/4=3/4。', 1, 1, 1, 1, 1, @n3138, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '数列 $\\\\{\\\\dfrac{1}{n(n+1)}\\\\}$ 的前10项和为：',
 '["A. $\\\\dfrac{9}{10}$","B. $\\\\dfrac{10}{11}$","C. $\\\\dfrac{11}{10}$","D. $\\\\dfrac{10}{9}$"]', 'B',
 '前10项和=(1-1/2)+(1/2-1/3)+...+(1/10-1/11)=1-1/11=10/11。中间项全部抵消。', 2, 1, 1, 1, 1, @n3138, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '数列 $\\\\{\\\\dfrac{1}{n(n+2)}\\\\}$ 的前 $n$ 项和 $S_n = $ _____。',
 '[]', '3/4-(2n+3)/(2(n+1)(n+2))',
 '裂项：1/(n(n+2))=(1/2)(1/n-1/(n+2))。S_n=(1/2)[(1-1/3)+(1/2-1/4)+(1/3-1/5)+...+(1/n-1/(n+2))]=(1/2)[1+1/2-1/(n+1)-1/(n+2)]=3/4-(2n+3)/(2(n+1)(n+2))。', 3, 1, 1, 1, 1, @n3138, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '裂项相消法的核心思想是：',
 '["A. 将数列各项相加后再分组","B. 将每一项拆成两项之差，中间项相互抵消","C. 将数列倒过来写再相加","D. 将数列乘以公比后相减"]', 'B',
 '裂项相消法的核心是把通项拆成两个式子的差，求和时中间各项互相抵消，只留首尾几项。', 1, 1, 1, 1, 1, @n3138, NOW(), NOW());


-- ══════════════════════════════════════════
-- 平面解析几何: 3节点 × 4题 = 12题
-- ══════════════════════════════════════════

-- === 3161: 弦长与切线问题 [理解] ===
SET @n3161 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='弦长与切线问题 [理解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '直线 $y=x$ 被圆 $x^2+y^2=4$ 所截得的弦长为：',
 '["A. $2\\\\sqrt{2}$","B. $4$","C. $2\\\\sqrt{3}$","D. $2$"]', 'B',
 '圆心(0,0)到直线x-y=0的距离d=|0-0|/√(1²+1²)=0/√2=0。直线过圆心，弦长=直径=2r=4。', 2, 1, 1, 1, 1, @n3161, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '过圆 $x^2+y^2=5$ 上一点 $P(1,2)$ 的切线方程是：',
 '["A. $x+2y=5$","B. $2x+y=5$","C. $x-2y=5$","D. $x+2y=3$"]', 'A',
 '过圆x²+y²=r²上点(x₀,y₀)的切线方程为x₀x+y₀y=r²。代入得1·x+2·y=5，即x+2y=5。', 1, 1, 1, 1, 1, @n3161, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '圆 $(x-1)^2+(y+2)^2=9$ 的圆心到直线 $3x-4y+5=0$ 的距离为 _____。',
 '[]', '16/5',
 '圆心(1,-2)，d=|3×1-4×(-2)+5|/√(9+16)=|3+8+5|/5=16/5。', 2, 1, 1, 1, 1, @n3161, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '若直线 $y=kx+2$ 与圆 $x^2+y^2=4$ 相切，则 $k = $ _____。',
 '[]', '0',
 '圆心(0,0)到直线kx-y+2=0的距离d=|2|/√(k²+1)=r=2。∴2/√(k²+1)=2→√(k²+1)=1→k²+1=1→k=0。直线y=2与圆相切。', 3, 1, 1, 1, 1, @n3161, NOW(), NOW());

-- === 3192: 双曲线的标准方程与性质 [掌握] ===
SET @n3192 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='双曲线的标准方程与性质 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '双曲线 $\\\\dfrac{x^2}{9}-\\\\dfrac{y^2}{16}=1$ 的实半轴长为：',
 '["A. 3","B. 4","C. 5","D. 9"]', 'A',
 '标准方程x²/a²-y²/b²=1中a²=9，a=3（实半轴长）；b²=16，b=4（虚半轴长）。', 1, 1, 1, 1, 1, @n3192, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '双曲线 $\\\\dfrac{x^2}{4}-\\\\dfrac{y^2}{5}=1$ 的焦点坐标为：',
 '["A. $(\\\\pm 1, 0)$","B. $(\\\\pm 2, 0)$","C. $(\\\\pm 3, 0)$","D. $(\\\\pm \\\\sqrt{5}, 0)$"]', 'C',
 'c²=a²+b²=4+5=9，c=3。焦点在x轴上，F(±3,0)。', 2, 1, 1, 1, 1, @n3192, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '双曲线 $\\\\dfrac{y^2}{9}-\\\\dfrac{x^2}{16}=1$ 的渐近线方程为：',
 '["A. $y=\\\\pm \\\\dfrac{3}{4}x$","B. $y=\\\\pm \\\\dfrac{4}{3}x$","C. $y=\\\\pm \\\\dfrac{9}{16}x$","D. $y=\\\\pm \\\\dfrac{16}{9}x$"]', 'A',
 '焦点在y轴上：y²/a²-x²/b²=1，渐近线y=±(a/b)x=±(3/4)x。注意：焦点在y轴时渐近线是y=±(a/b)x而非±(b/a)x。', 1, 1, 1, 1, 1, @n3192, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '双曲线 $\\\\dfrac{x^2}{a^2}-\\\\dfrac{y^2}{b^2}=1$ 的离心率 $e = $ _____（用a,b,c表示）。',
 '[]', 'c/a',
 '双曲线离心率e=c/a，其中c²=a²+b²。e>1是双曲线的特征，区别于椭圆的0<e<1。', 2, 1, 1, 1, 1, @n3192, NOW(), NOW());

-- === 3193: 抛物线的标准方程与性质 [掌握] ===
SET @n3193 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='抛物线的标准方程与性质 [掌握]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '抛物线 $y^2=8x$ 的焦点坐标为：',
 '["A. $(0,2)$","B. $(2,0)$","C. $(4,0)$","D. $(0,4)$"]', 'B',
 '标准形式y²=2px，2p=8，p=4。焦点(p/2,0)=(2,0)，开口向右。', 1, 1, 1, 1, 1, @n3193, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '抛物线 $x^2=-4y$ 的准线方程是：',
 '["A. $y=-1$","B. $y=1$","C. $x=-1$","D. $x=1$"]', 'B',
 '标准形式x²=-2py，2p=4，p=2。开口向下，准线y=p/2=1。注意：开口向下时准线在焦点上方。', 2, 1, 1, 1, 1, @n3193, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '抛物线 $y^2=12x$ 上一点P到焦点的距离为9，则点P的横坐标为：',
 '["A. 3","B. 6","C. 9","D. 12"]', 'B',
 'p=6，准线x=-3。抛物线定义：PF=点P到准线的距离。设P(x,y)，则x-(-3)=x+3=9，x=6。', 3, 1, 1, 1, 1, @n3193, NOW(), NOW()),

('数学[职高]', 'FILL_IN', '抛物线 $x^2=8y$ 上到焦点距离最小的点的坐标为 _____。',
 '[]', '(0,0)',
 '抛物线顶点(0,0)到焦点的距离=p/2=2，是所有点中到焦点距离最小的（顶点到焦点距离=p/2，其他点都更大）。', 2, 1, 1, 1, 1, @n3193, NOW(), NOW());


-- ══════════════════════════════════════════
-- 概率与统计: 1节点 × 2题 = 2题
-- ══════════════════════════════════════════

-- === 3169: 抽样方法 [了解] ===
SET @n3169 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='抽样方法 [了解]' AND level=4 LIMIT 1);

INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status, version, is_latest, school_id, category_id, create_time, update_time) VALUES

('数学[职高]', 'SINGLE_CHOICE', '从1000名学生中按学号每隔20个抽取1人进行调查，这种抽样方法是：',
 '["A. 简单随机抽样","B. 系统抽样","C. 分层抽样","D. 整群抽样"]', 'B',
 '系统抽样（等距抽样）是将总体编号后按固定间隔抽取样本。每隔20个抽1人即抽样间距=20。', 1, 1, 1, 1, 1, @n3169, NOW(), NOW()),

('数学[职高]', 'SINGLE_CHOICE', '某校有高一300人、高二400人、高三300人，按比例从各年级抽取共50人进行调查，应使用：',
 '["A. 简单随机抽样","B. 系统抽样","C. 分层抽样","D. 方便抽样"]', 'C',
 '分层抽样是将总体按特征分成若干层，在各层中按比例独立抽取样本。高一:高二:高三=3:4:3，各抽取15、20、15人。', 2, 1, 1, 1, 1, @n3169, NOW(), NOW());

COMMIT;
SELECT 'v194 deployed — 数列+解析几何+概率 26题' AS result;
-- ============================================================================
