-- ============================================================
-- v100: 数学[职高] 导数初步模块 + 种子题
-- 考纲要求：导数的几何意义、基本求导公式、利用导数求单调区间和极值（选考）
-- ============================================================
SET NAMES utf8mb4;

-- Level 2 章节：导数初步
SET @math_root_id = 10;
SET @math_subject_id = 22;
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@math_root_id, @math_subject_id, 2, '导数初步 [选考]', 12, 'ACTIVE');

SET @deriv_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='导数初步 [选考]' AND level=2);

-- Level 3 单元
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@deriv_id, @math_subject_id, 3, '导数的概念与意义', 1, 'ACTIVE'),
(@deriv_id, @math_subject_id, 3, '基本求导公式', 2, 'ACTIVE'),
(@deriv_id, @math_subject_id, 3, '导数的应用', 3, 'ACTIVE');

SET @_d1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@deriv_id AND name='导数的概念与意义');
SET @_d2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@deriv_id AND name='基本求导公式');
SET @_d3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@deriv_id AND name='导数的应用');

INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_d1, @math_subject_id, 4, '导数的几何意义（切线斜率）[了解]', 1, 'ACTIVE'),
(@_d1, @math_subject_id, 4, '导数的物理意义（瞬时变化率）[了解]', 2, 'ACTIVE'),
(@_d2, @math_subject_id, 4, '基本初等函数的求导公式 [掌握]', 1, 'ACTIVE'),
(@_d2, @math_subject_id, 4, '导数的四则运算法则 [掌握]', 2, 'ACTIVE'),
(@_d3, @math_subject_id, 4, '利用导数判断函数的单调性 [掌握]', 1, 'ACTIVE'),
(@_d3, @math_subject_id, 4, '利用导数求函数的极值 [掌握]', 2, 'ACTIVE');

-- 导数种子题目 5 题
INSERT INTO question_bank (question_type, question_text, options, correct_answer, explanation, subject, difficulty_level, status, version, is_latest, category_id) VALUES
('SINGLE_CHOICE', '函数$f(x)=x^2$在x=1处的导数值等于：',
 '["A. 1","B. 2","C. 3","D. 0"]', 'B', 'f\'(x)=2x，代入x=1得f\'(1)=2。', '数学[职高]', 1, 1, 1, 1, @_d2),
('SINGLE_CHOICE', '函数$f(x)=x^3-3x$的单调递增区间是：',
 '["A. (-∞,-1)∪(1,+∞)","B. (-1,1)","C. (0,+∞)","D. (-∞,0)"]', 'A', 'f\'(x)=3x²-3=3(x+1)(x-1)。x<-1或x>1时f\'(x)>0，单调递增。', '数学[职高]', 2, 1, 1, 1, @_d3),
('SINGLE_CHOICE', '函数$f(x)=x^3-3x^2+1$的极小值是：',
 '["A. 1","B. -1","C. -3","D. 3"]', 'C', 'f\'(x)=3x²-6x=3x(x-2)。x=0时极大值1，x=2时极小值-3。', '数学[职高]', 3, 1, 1, 1, @_d3),
('FILL_IN', '曲线$y=x^2$在点(1,1)处的切线方程为：',
 NULL, 'y=x', '斜率k=f\'(1)=2，切线方程y-1=2(x-1)即y=2x-1=y。简化即y=2x-1。', '数学[职高]', 2, 1, 1, 1, @_d1),
('TRUE_FALSE', '若f\'(x₀)=0，则x₀一定是函数f(x)的极值点。',
 NULL, 'B', '导数零点不一定是极值点。例如f(x)=x³，f\'(0)=0但x=0不是极值点。', '数学[职高]', 2, 1, 1, 1, @_d3);
