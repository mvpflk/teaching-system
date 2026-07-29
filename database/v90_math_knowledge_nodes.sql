-- ============================================================
-- v90: 数学[职高] 知识树种子数据
-- 父节点: id=10 (数学[职高], subjectId=22)
-- 结构: 10大考纲模块 + 初中基础补漏 = 11 个 level=2 章节
-- 每个 level=2 含若干 level=3 单元，每个 level=3 含 1-3 个 level=4 知识点
-- ⚠️ 执行前必须 SET NAMES utf8mb4，防止中文乱码
-- ============================================================
SET NAMES utf8mb4;
-- 考纲层次: [了解]=awareness [理解]=comprehension [掌握]=mastery
-- ============================================================

-- 先检查根节点是否已被使用
SET @math_root_id = 10;
SET @math_subject_id = 22;  -- dict_subject.id for 数学[职高]

-- ══════════════════════════════════════════
-- Level 2: 章节（11 个）
-- ══════════════════════════════════════════

INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@math_root_id, @math_subject_id, 2, '集合',          1, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '不等式',         2, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '函数',           3, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '指数与对数函数', 4, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '三角函数',       5, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '数列',           6, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '平面向量',       7, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '立体几何',       8, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '平面解析几何',   9, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '概率与统计',     10, 'ACTIVE'),
(@math_root_id, @math_subject_id, 2, '初中基础补漏',   11, 'ACTIVE');

-- 保存 level-2 节点的起始 ID（假设按序递增）
-- 用临时变量捕获插入后的 ID，配合 last_insert_id 推算

-- ══════════════════════════════════════════
-- Level 3: 单元 + Level 4: 知识点
-- ══════════════════════════════════════════

-- 为简化脚本，使用子查询动态获取刚插入的 level-2 节点ID
-- 注意: 此脚本依赖 v92_migration 已执行，且 knowledge_nodes.id 从 10 开始有 11 个 level-2

-- 集合 (level=2, sort=1)
SET @_set_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='集合' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_set_id, @math_subject_id, 3, '集合的概念与表示', 1, NULL, 'ACTIVE'),
(@_set_id, @math_subject_id, 3, '集合间的关系', 2, NULL, 'ACTIVE'),
(@_set_id, @math_subject_id, 3, '集合的运算', 3, NULL, 'ACTIVE');
SET @_set_lv3_1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_set_id AND name='集合的概念与表示');
SET @_set_lv3_2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_set_id AND name='集合间的关系');
SET @_set_lv3_3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_set_id AND name='集合的运算');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_set_lv3_1, @math_subject_id, 4, '集合的定义与元素 [了解]',   1, NULL, 'ACTIVE'),
(@_set_lv3_1, @math_subject_id, 4, '集合的表示方法 [了解]',     2, NULL, 'ACTIVE'),
(@_set_lv3_1, @math_subject_id, 4, '空集与全集 [了解]',         3, NULL, 'ACTIVE'),
(@_set_lv3_2, @math_subject_id, 4, '子集、真子集、相等 [理解]', 1, NULL, 'ACTIVE'),
(@_set_lv3_2, @math_subject_id, 4, '集合关系判断与证明 [理解]', 2, NULL, 'ACTIVE'),
(@_set_lv3_3, @math_subject_id, 4, '交集运算 [掌握]',           1, NULL, 'ACTIVE'),
(@_set_lv3_3, @math_subject_id, 4, '并集运算 [掌握]',           2, NULL, 'ACTIVE'),
(@_set_lv3_3, @math_subject_id, 4, '补集运算 [掌握]',           3, NULL, 'ACTIVE');

-- 不等式 (level=2, sort=2)
SET @_ineq_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='不等式' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_ineq_id, @math_subject_id, 3, '不等式的性质', 1, NULL, 'ACTIVE'),
(@_ineq_id, @math_subject_id, 3, '一元二次不等式', 2, NULL, 'ACTIVE'),
(@_ineq_id, @math_subject_id, 3, '含绝对值的不等式', 3, NULL, 'ACTIVE');
SET @_ineq_lv3_1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_ineq_id AND name='不等式的性质');
SET @_ineq_lv3_2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_ineq_id AND name='一元二次不等式');
SET @_ineq_lv3_3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_ineq_id AND name='含绝对值的不等式');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_ineq_lv3_1, @math_subject_id, 4, '不等式的性质与传递性 [了解]',  1, NULL, 'ACTIVE'),
(@_ineq_lv3_2, @math_subject_id, 4, '一元二次不等式的解法 [掌握]',   1, NULL, 'ACTIVE'),
(@_ineq_lv3_2, @math_subject_id, 4, '一元二次不等式的应用 [掌握]',   2, NULL, 'ACTIVE'),
(@_ineq_lv3_3, @math_subject_id, 4, '含绝对值不等式的解法 [理解]',   1, NULL, 'ACTIVE');

-- 函数 (level=2, sort=3)
SET @_func_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='函数' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_func_id, @math_subject_id, 3, '函数概念与表示', 1, NULL, 'ACTIVE'),
(@_func_id, @math_subject_id, 3, '函数的性质', 2, NULL, 'ACTIVE'),
(@_func_id, @math_subject_id, 3, '二次函数', 3, NULL, 'ACTIVE'),
(@_func_id, @math_subject_id, 3, '函数实际应用', 4, NULL, 'ACTIVE');
SET @_f1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_func_id AND name='函数概念与表示');
SET @_f2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_func_id AND name='函数的性质');
SET @_f3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_func_id AND name='二次函数');
SET @_f4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_func_id AND name='函数实际应用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_f1, @math_subject_id, 4, '函数定义与定义域 [掌握]',   1, NULL, 'ACTIVE'),
(@_f1, @math_subject_id, 4, '函数的表示方法 [理解]',     2, NULL, 'ACTIVE'),
(@_f1, @math_subject_id, 4, '函数值域求解 [理解]',       3, NULL, 'ACTIVE'),
(@_f2, @math_subject_id, 4, '函数单调性判断 [掌握]',     1, NULL, 'ACTIVE'),
(@_f2, @math_subject_id, 4, '函数奇偶性判断 [掌握]',     2, NULL, 'ACTIVE'),
(@_f3, @math_subject_id, 4, '二次函数图像与性质 [掌握]', 1, NULL, 'ACTIVE'),
(@_f3, @math_subject_id, 4, '二次函数最值问题 [掌握]',   2, NULL, 'ACTIVE'),
(@_f4, @math_subject_id, 4, '函数应用题建模 [理解]',     1, NULL, 'ACTIVE');

-- 指数与对数函数 (level=2, sort=4)
SET @_exp_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='指数与对数函数' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_exp_id, @math_subject_id, 3, '指数函数', 1, NULL, 'ACTIVE'),
(@_exp_id, @math_subject_id, 3, '对数函数', 2, NULL, 'ACTIVE');
SET @_e1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_exp_id AND name='指数函数');
SET @_e2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_exp_id AND name='对数函数');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_e1, @math_subject_id, 4, '指数运算性质 [掌握]',       1, NULL, 'ACTIVE'),
(@_e1, @math_subject_id, 4, '指数函数的图像与性质 [理解]', 2, NULL, 'ACTIVE'),
(@_e2, @math_subject_id, 4, '对数运算性质 [掌握]',       1, NULL, 'ACTIVE'),
(@_e2, @math_subject_id, 4, '对数函数的图像与性质 [理解]', 2, NULL, 'ACTIVE');

-- 三角函数 (level=2, sort=5) — 最大模块，14%分值
SET @_trig_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='三角函数' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_trig_id, @math_subject_id, 3, '任意角与弧度制', 1, NULL, 'ACTIVE'),
(@_trig_id, @math_subject_id, 3, '三角函数定义与基本关系', 2, NULL, 'ACTIVE'),
(@_trig_id, @math_subject_id, 3, '诱导公式', 3, NULL, 'ACTIVE'),
(@_trig_id, @math_subject_id, 3, '三角函数的图像与性质', 4, NULL, 'ACTIVE'),
(@_trig_id, @math_subject_id, 3, '和差公式与倍角公式', 5, NULL, 'ACTIVE'),
(@_trig_id, @math_subject_id, 3, '正弦定理与余弦定理', 6, NULL, 'ACTIVE');
SET @_t1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trig_id AND name='任意角与弧度制');
SET @_t2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trig_id AND name='三角函数定义与基本关系');
SET @_t3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trig_id AND name='诱导公式');
SET @_t4=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trig_id AND name='三角函数的图像与性质');
SET @_t5=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trig_id AND name='和差公式与倍角公式');
SET @_t6=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trig_id AND name='正弦定理与余弦定理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_t1,@math_subject_id,4,'任意角的概念与表示 [了解]',1,NULL,'ACTIVE'),
(@_t1,@math_subject_id,4,'弧度制与角度制互化 [理解]',2,NULL,'ACTIVE'),
(@_t2,@math_subject_id,4,'三角函数的定义（单位圆）[掌握]',1,NULL,'ACTIVE'),
(@_t2,@math_subject_id,4,'同角三角函数基本关系 [掌握]',2,NULL,'ACTIVE'),
(@_t3,@math_subject_id,4,'诱导公式(一)~(四) [掌握]',1,NULL,'ACTIVE'),
(@_t3,@math_subject_id,4,'诱导公式(五)~(六) [掌握]',2,NULL,'ACTIVE'),
(@_t4,@math_subject_id,4,'正弦、余弦函数的图像与性质 [理解]',1,NULL,'ACTIVE'),
(@_t4,@math_subject_id,4,'正切函数的图像与性质 [了解]',2,NULL,'ACTIVE'),
(@_t5,@math_subject_id,4,'两角和与差的正弦、余弦公式 [掌握]',1,NULL,'ACTIVE'),
(@_t5,@math_subject_id,4,'二倍角公式 [掌握]',2,NULL,'ACTIVE'),
(@_t6,@math_subject_id,4,'正弦定理 [掌握]',1,NULL,'ACTIVE'),
(@_t6,@math_subject_id,4,'余弦定理 [掌握]',2,NULL,'ACTIVE');

-- 数列 (level=2, sort=6)
SET @_seq_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='数列' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_seq_id, @math_subject_id, 3, '数列的概念', 1, NULL, 'ACTIVE'),
(@_seq_id, @math_subject_id, 3, '等差数列', 2, NULL, 'ACTIVE'),
(@_seq_id, @math_subject_id, 3, '等比数列', 3, NULL, 'ACTIVE'),
(@_seq_id, @math_subject_id, 3, '数列求和', 4, NULL, 'ACTIVE');
SET @_s1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_seq_id AND name='数列的概念');
SET @_s2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_seq_id AND name='等差数列');
SET @_s3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_seq_id AND name='等比数列');
SET @_s4=(SELECT id FROM knowledge_nodes WHERE parent_id=@_seq_id AND name='数列求和');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_s1,@math_subject_id,4,'数列的定义与分类 [了解]',1,NULL,'ACTIVE'),
(@_s2,@math_subject_id,4,'等差数列通项公式 [掌握]',1,NULL,'ACTIVE'),
(@_s2,@math_subject_id,4,'等差数列前n项和 [掌握]',2,NULL,'ACTIVE'),
(@_s3,@math_subject_id,4,'等比数列通项公式 [掌握]',1,NULL,'ACTIVE'),
(@_s3,@math_subject_id,4,'等比数列前n项和 [掌握]',2,NULL,'ACTIVE'),
(@_s4,@math_subject_id,4,'分组求和法 [理解]',1,NULL,'ACTIVE'),
(@_s4,@math_subject_id,4,'裂项相消法 [理解]',2,NULL,'ACTIVE');

-- 平面向量 (level=2, sort=7)
SET @_vec_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='平面向量' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_vec_id, @math_subject_id, 3, '向量的概念与线性运算', 1, NULL, 'ACTIVE'),
(@_vec_id, @math_subject_id, 3, '向量的数量积', 2, NULL, 'ACTIVE'),
(@_vec_id, @math_subject_id, 3, '向量的坐标运算', 3, NULL, 'ACTIVE');
SET @_v1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_vec_id AND name='向量的概念与线性运算');
SET @_v2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_vec_id AND name='向量的数量积');
SET @_v3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_vec_id AND name='向量的坐标运算');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_v1,@math_subject_id,4,'向量的定义与表示 [了解]',1,NULL,'ACTIVE'),
(@_v1,@math_subject_id,4,'向量的加减法与数乘 [理解]',2,NULL,'ACTIVE'),
(@_v2,@math_subject_id,4,'数量积的定义与计算 [掌握]',1,NULL,'ACTIVE'),
(@_v3,@math_subject_id,4,'向量的坐标表示 [掌握]',1,NULL,'ACTIVE'),
(@_v3,@math_subject_id,4,'坐标运算的应用 [掌握]',2,NULL,'ACTIVE');

-- 立体几何 (level=2, sort=8)
SET @_geom_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='立体几何' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_geom_id, @math_subject_id, 3, '空间几何体', 1, NULL, 'ACTIVE'),
(@_geom_id, @math_subject_id, 3, '点线面的位置关系', 2, NULL, 'ACTIVE');
SET @_g1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_geom_id AND name='空间几何体');
SET @_g2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_geom_id AND name='点线面的位置关系');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_g1,@math_subject_id,4,'常见几何体的表面积 [理解]',1,NULL,'ACTIVE'),
(@_g1,@math_subject_id,4,'常见几何体的体积 [理解]',2,NULL,'ACTIVE'),
(@_g2,@math_subject_id,4,'线面平行与垂直的判定 [理解]',1,NULL,'ACTIVE'),
(@_g2,@math_subject_id,4,'面面平行与垂直的判定 [理解]',2,NULL,'ACTIVE');

-- 平面解析几何 (level=2, sort=9) — 18%分值
SET @_pg_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='平面解析几何' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_pg_id, @math_subject_id, 3, '直线方程', 1, NULL, 'ACTIVE'),
(@_pg_id, @math_subject_id, 3, '圆的方程', 2, NULL, 'ACTIVE'),
(@_pg_id, @math_subject_id, 3, '直线与圆的位置关系', 3, NULL, 'ACTIVE');
SET @_p1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_pg_id AND name='直线方程');
SET @_p2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_pg_id AND name='圆的方程');
SET @_p3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_pg_id AND name='直线与圆的位置关系');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_p1,@math_subject_id,4,'直线的倾斜角与斜率 [掌握]',1,NULL,'ACTIVE'),
(@_p1,@math_subject_id,4,'直线的五种方程形式 [掌握]',2,NULL,'ACTIVE'),
(@_p2,@math_subject_id,4,'圆的标准方程 [掌握]',1,NULL,'ACTIVE'),
(@_p2,@math_subject_id,4,'圆的一般方程 [掌握]',2,NULL,'ACTIVE'),
(@_p3,@math_subject_id,4,'直线与圆的位置关系判断 [掌握]',1,NULL,'ACTIVE'),
(@_p3,@math_subject_id,4,'弦长与切线问题 [理解]',2,NULL,'ACTIVE');

-- 概率与统计 (level=2, sort=10)
SET @_prob_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='概率与统计' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_prob_id, @math_subject_id, 3, '计数原理', 1, NULL, 'ACTIVE'),
(@_prob_id, @math_subject_id, 3, '概率', 2, NULL, 'ACTIVE'),
(@_prob_id, @math_subject_id, 3, '统计', 3, NULL, 'ACTIVE');
SET @_pr1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_prob_id AND name='计数原理');
SET @_pr2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_prob_id AND name='概率');
SET @_pr3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_prob_id AND name='统计');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_pr1,@math_subject_id,4,'分类加法与分步乘法 [理解]',1,NULL,'ACTIVE'),
(@_pr1,@math_subject_id,4,'排列与组合 [理解]',2,NULL,'ACTIVE'),
(@_pr2,@math_subject_id,4,'古典概型 [掌握]',1,NULL,'ACTIVE'),
(@_pr2,@math_subject_id,4,'互斥事件与独立事件 [理解]',2,NULL,'ACTIVE'),
(@_pr3,@math_subject_id,4,'抽样方法 [了解]',1,NULL,'ACTIVE'),
(@_pr3,@math_subject_id,4,'用样本估计总体 [了解]',2,NULL,'ACTIVE');

-- 初中基础补漏 (level=2, sort=11)
SET @_jr_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@math_root_id AND name='初中基础补漏' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_jr_id, @math_subject_id, 3, '有理数与实数运算', 1, NULL, 'ACTIVE'),
(@_jr_id, @math_subject_id, 3, '整式与分式', 2, NULL, 'ACTIVE'),
(@_jr_id, @math_subject_id, 3, '一元一次方程', 3, NULL, 'ACTIVE'),
(@_jr_id, @math_subject_id, 3, '二元一次方程组', 4, NULL, 'ACTIVE'),
(@_jr_id, @math_subject_id, 3, '一元二次方程', 5, NULL, 'ACTIVE'),
(@_jr_id, @math_subject_id, 3, '一次函数与图像', 6, NULL, 'ACTIVE'),
(@_jr_id, @math_subject_id, 3, '三角形基础', 7, NULL, 'ACTIVE');
SET @_j1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_jr_id AND name='有理数与实数运算');
SET @_j2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_jr_id AND name='整式与分式');
SET @_j3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_jr_id AND name='一元一次方程');
SET @_j4=(SELECT id FROM knowledge_nodes WHERE parent_id=@_jr_id AND name='二元一次方程组');
SET @_j5=(SELECT id FROM knowledge_nodes WHERE parent_id=@_jr_id AND name='一元二次方程');
SET @_j6=(SELECT id FROM knowledge_nodes WHERE parent_id=@_jr_id AND name='一次函数与图像');
SET @_j7=(SELECT id FROM knowledge_nodes WHERE parent_id=@_jr_id AND name='三角形基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@_j1,@math_subject_id,4,'有理数四则混合运算',1,NULL,'ACTIVE'),
(@_j1,@math_subject_id,4,'实数的概念与分类',2,NULL,'ACTIVE'),
(@_j2,@math_subject_id,4,'整式加减乘除运算',1,NULL,'ACTIVE'),
(@_j2,@math_subject_id,4,'分式的约分与通分',2,NULL,'ACTIVE'),
(@_j3,@math_subject_id,4,'解一元一次方程',1,NULL,'ACTIVE'),
(@_j4,@math_subject_id,4,'代入消元法与加减消元法',1,NULL,'ACTIVE'),
(@_j5,@math_subject_id,4,'一元二次方程公式法求解',1,NULL,'ACTIVE'),
(@_j5,@math_subject_id,4,'根的判别式与韦达定理',2,NULL,'ACTIVE'),
(@_j6,@math_subject_id,4,'一次函数的图像与斜率',1,NULL,'ACTIVE'),
(@_j6,@math_subject_id,4,'一次函数与方程不等式的关系',2,NULL,'ACTIVE'),
(@_j7,@math_subject_id,4,'三角形内角和与分类',1,NULL,'ACTIVE'),
(@_j7,@math_subject_id,4,'勾股定理与简单应用',2,NULL,'ACTIVE');
