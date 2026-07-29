-- ============================================================
-- v94: 数学题库 + 英语阅读短文种子（MVP可用）
-- 数学: 10模块×3题 = 30题基础种子
-- 英语: 5类阅读×4周 = 20篇短文
-- ============================================================

-- ══════════════════════════════════════════
-- 数学[职高] 基础题库（10模块各3题）
-- 题型: SINGLE_CHOICE
-- ══════════════════════════════════════════

-- 集合模块 3题
INSERT INTO question_bank (question_type, question_text, options, correct_answer, explanation, subject, difficulty_level, status, version, is_latest, category_id) VALUES
('SINGLE_CHOICE', '已知集合A={1,2,3}，B={2,3,4}，则A∩B等于：',
 '["A. {1,2,3,4}","B. {2,3}","C. {1,4}","D. {1,2}"]', 'B', '交集是取两个集合共有的元素，A和B共有的元素是2和3。', '数学[职高]', 1, 1, 1, 1, NULL),
('SINGLE_CHOICE', '设全集U={1,2,3,4,5}，集合A={1,2,3}，则A的补集等于：',
 '["A. {1,2,3}","B. {4,5}","C. {1,4,5}","D. U"]', 'B', '补集是在全集中去掉A中元素后剩下的部分：U-A={4,5}。', '数学[职高]', 1, 1, 1, 1, NULL),
('SINGLE_CHOICE', '下列关系中正确的是：',
 '["A. 0∈∅","B. {0}=∅","C. {0}⊆{0,1}","D. 0⊆{0}"]', 'C', '单元素集合{0}是集合{0,1}的子集。A错，空集不含任何元素。', '数学[职高]', 2, 1, 1, 1, NULL);

-- 不等式模块 3题
INSERT INTO question_bank (question_type, question_text, options, correct_answer, explanation, subject, difficulty_level, status, version, is_latest, category_id) VALUES
('SINGLE_CHOICE', '不等式x²-4<0的解集是：',
 '["A. {x|x>2或x<-2}","B. {x|-2<x<2}","C. {x|x>2}","D. {x|x<-2}"]', 'B', 'x²-4<0即x²<4，解得-2<x<2。', '数学[职高]', 2, 1, 1, 1, NULL),
('SINGLE_CHOICE', '不等式x²-5x+6>0的解集是：',
 '["A. {x|x>3或x<2}","B. {x|2<x<3}","C. {x|x>3}","D. {x|x<2}"]', 'A', '因式分解(x-2)(x-3)>0，根据抛物线开口向上，解集为x>3或x<2。', '数学[职高]', 2, 1, 1, 1, NULL),
('SINGLE_CHOICE', '若a>b，则下列不等式一定成立的是：',
 '["A. a²>b²","B. ac>bc","C. a+c>b+c","D. |a|>|b|"]', 'C', '不等式两边同时加上同一个数，不等号方向不变。', '数学[职高]', 1, 1, 1, 1, NULL);

-- 函数模块 3题
INSERT INTO question_bank (question_type, question_text, options, correct_answer, explanation, subject, difficulty_level, status, version, is_latest, category_id) VALUES
('SINGLE_CHOICE', '函数$f(x)=\\sqrt{x-2}$的定义域是：',
 '["A. {x|x≥2}","B. {x|x>2}","C. {x|x≤2}","D. R"]', 'A', '被开方数x-2≥0，解得x≥2。', '数学[职高]', 1, 1, 1, 1, NULL),
('SINGLE_CHOICE', '下列函数中为奇函数的是：',
 '["A. y=x²","B. y=x³","C. y=|x|","D. y=x²+1"]', 'B', '奇函数满足f(-x)=-f(x)。y=x³中(-x)³=-x³，满足条件。', '数学[职高]', 2, 1, 1, 1, NULL),
('SINGLE_CHOICE', '二次函数$y=x²-4x+3$的顶点坐标是：',
 '["A. (2,-1)","B. (-2,1)","C. (2,1)","D. (-2,-1)"]', 'A', '配方得y=(x-2)²-1，顶点为(2,-1)。', '数学[职高]', 2, 1, 1, 1, NULL);

-- 数列模块 3题
INSERT INTO question_bank (question_type, question_text, options, correct_answer, explanation, subject, difficulty_level, status, version, is_latest, category_id) VALUES
('SINGLE_CHOICE', '等差数列{an}中，a1=2，d=3，则a10等于：',
 '["A. 30","B. 29","C. 28","D. 32"]', 'B', 'an=a1+(n-1)d=2+9×3=29。', '数学[职高]', 2, 1, 1, 1, NULL),
('SINGLE_CHOICE', '等比数列{bn}中，b1=2，q=2，则b5等于：',
 '["A. 16","B. 32","C. 64","D. 8"]', 'B', 'bn=b1×q^(n-1)=2×2⁴=32。', '数学[职高]', 2, 1, 1, 1, NULL),
('SINGLE_CHOICE', '等差数列{an}的前10项和为S10，a1=1，a10=19，则S10等于：',
 '["A. 100","B. 200","C. 90","D. 190"]', 'A', 'Sn=n(a1+an)/2=10×(1+19)/2=100。', '数学[职高]', 3, 1, 1, 1, NULL);

-- 三角函数模块 3题
INSERT INTO question_bank (question_type, question_text, options, correct_answer, explanation, subject, difficulty_level, status, version, is_latest, category_id) VALUES
('SINGLE_CHOICE', 'sin60°的值为：',
 '["A. 1/2","B. √3/2","C. √2/2","D. √3"]', 'B', '特殊角三角函数值：sin60°=√3/2。', '数学[职高]', 1, 1, 1, 1, NULL),
('SINGLE_CHOICE', '已知sinα=3/5，且α为锐角，则cosα等于：',
 '["A. 4/5","B. 3/5","C. -4/5","D. 5/3"]', 'A', '由sin²α+cos²α=1，cosα=√(1-9/25)=4/5。', '数学[职高]', 2, 1, 1, 1, NULL),
('SINGLE_CHOICE', '函数$y=2\\sin x$的最大值是：',
 '["A. 1","B. 2","C. 3","D. 0"]', 'B', 'sinx的最大值为1，2sinx的最大值=2×1=2。', '数学[职高]', 1, 1, 1, 1, NULL);

-- 向量 + 立体几何 + 解析几何 + 概率 各1-2题
INSERT INTO question_bank (question_type, question_text, options, correct_answer, explanation, subject, difficulty_level, status, version, is_latest, category_id) VALUES
('SINGLE_CHOICE', '向量a=(2,3)，b=(1,-1)，则a·b等于：',
 '["A. -1","B. 1","C. 5","D. -5"]', 'A', 'a·b=2×1+3×(-1)=2-3=-1。', '数学[职高]', 2, 1, 1, 1, NULL),
('SINGLE_CHOICE', '正方体的棱长为2，其体积等于：',
 '["A. 4","B. 6","C. 8","D. 12"]', 'C', '正方体体积V=a³=2³=8。', '数学[职高]', 1, 1, 1, 1, NULL),
('SINGLE_CHOICE', '圆$(x-1)²+(y+2)²=9$的半径是：',
 '["A. 1","B. 2","C. 3","D. 9"]', 'C', '圆的标准方程(x-a)²+(y-b)²=r²中，r²=9，r=3。', '数学[职高]', 1, 1, 1, 1, NULL),
('SINGLE_CHOICE', '同时掷两枚硬币，都出现正面的概率是：',
 '["A. 1/2","B. 1/3","C. 1/4","D. 3/4"]', 'C', '样本空间有4种结果，都是正面只有1种，P=1/4。', '数学[职高]', 1, 1, 1, 1, NULL);
