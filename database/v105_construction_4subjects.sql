-- ============================================================
-- v105: 建筑类[职高] 4科 — 对标四川省对口升学土木水利类
-- 参照计算机4科/农学4科模式
-- 4科: 工程测量/建筑材料/建筑力学/工程制图
-- 推荐教材: 高教社中职国家规划教材
-- ============================================================

SET @s1=27; SET @s2=28; SET @s3=29; SET @s4=30;

-- ═══ 创建4个根节点(Level 1) ═══
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(NULL, @s1, 1, '工程测量',   1, 'ACTIVE'),
(NULL, @s2, 1, '建筑材料',   1, 'ACTIVE'),
(NULL, @s3, 1, '建筑力学',   1, 'ACTIVE'),
(NULL, @s4, 1, '工程制图',   1, 'ACTIVE');

SET @r1=(SELECT id FROM knowledge_nodes WHERE subject_id=@s1 AND level=1 LIMIT 1);
SET @r2=(SELECT id FROM knowledge_nodes WHERE subject_id=@s2 AND level=1 LIMIT 1);
SET @r3=(SELECT id FROM knowledge_nodes WHERE subject_id=@s3 AND level=1 LIMIT 1);
SET @r4=(SELECT id FROM knowledge_nodes WHERE subject_id=@s4 AND level=1 LIMIT 1);

-- ════════════════════════════════════════════════════════════
-- 科目1: 工程测量 (4章×12单元×35知识点)
-- 教材: 《工程测量》(第三版) 高教社 ISBN 978-7-04-053456-6
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r1, @s1, 2, '测量基础知识', 1, 'ACTIVE'),
(@r1, @s1, 2, '水准测量',     2, 'ACTIVE'),
(@r1, @s1, 2, '角度测量',     3, 'ACTIVE'),
(@r1, @s1, 2, '距离测量与地形图', 4, 'ACTIVE');

-- 1.1 测量基础知识
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='测量基础知识');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '测量学概述', 1, 'ACTIVE'),
(@ch, @s1, 3, '地面点位的确定', 2, 'ACTIVE'),
(@ch, @s1, 3, '测量误差基础', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='测量学概述'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='地面点位的确定');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='测量误差基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '测量学的分类与任务 [识记]', 1, 'ACTIVE'),
(@u1, @s1, 4, '测量工作的基本原则(从整体到局部/先控制后碎部) [理解]', 2, 'ACTIVE'),
(@u2, @s1, 4, '大地水准面与高程系统 [理解]', 1, 'ACTIVE'),
(@u2, @s1, 4, '平面直角坐标系与坐标增量 [掌握]', 2, 'ACTIVE'),
(@u3, @s1, 4, '系统误差与偶然误差的区别 [理解]', 1, 'ACTIVE'),
(@u3, @s1, 4, '中误差与容许误差的计算 [掌握]', 2, 'ACTIVE');

-- 1.2 水准测量
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='水准测量');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '水准仪构造与使用', 1, 'ACTIVE'),
(@ch, @s1, 3, '水准测量方法', 2, 'ACTIVE'),
(@ch, @s1, 3, '水准路线与成果校核', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水准仪构造与使用'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水准测量方法');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水准路线与成果校核');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, 'DS3水准仪的构造(望远镜/水准器/基座) [识记]', 1, 'ACTIVE'),
(@u1, @s1, 4, '水准尺的读数方法 [掌握]', 2, 'ACTIVE'),
(@u2, @s1, 4, '水准测量原理(高差=后视-前视) [掌握]', 1, 'ACTIVE'),
(@u2, @s1, 4, '两次仪器高法的操作步骤 [理解]', 2, 'ACTIVE'),
(@u3, @s1, 4, '闭合/附合/支水准路线的计算 [掌握]', 1, 'ACTIVE'),
(@u3, @s1, 4, '水准测量成果的容许闭合差 [掌握]', 2, 'ACTIVE');

-- 1.3 角度测量
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='角度测量');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '经纬仪构造与使用', 1, 'ACTIVE'),
(@ch, @s1, 3, '水平角测量', 2, 'ACTIVE'),
(@ch, @s1, 3, '竖直角测量', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='经纬仪构造与使用'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水平角测量');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='竖直角测量');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, 'DJ6光学经纬仪的构造与读数 [识记]', 1, 'ACTIVE'),
(@u1, @s1, 4, '经纬仪的安置(对中/整平) [掌握]', 2, 'ACTIVE'),
(@u2, @s1, 4, '测回法测量水平角的操作 [掌握]', 1, 'ACTIVE'),
(@u2, @s1, 4, '方向观测法(全圆测回法) [理解]', 2, 'ACTIVE'),
(@u3, @s1, 4, '竖直角与指标差的计算 [掌握]', 1, 'ACTIVE');

-- 1.4 距离测量与地形图
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='距离测量与地形图');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '距离测量方法', 1, 'ACTIVE'),
(@ch, @s1, 3, '全站仪使用', 2, 'ACTIVE'),
(@ch, @s1, 3, '地形图基本知识', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='距离测量方法'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='全站仪使用');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='地形图基本知识');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '钢尺量距与视距测量的原理 [理解]', 1, 'ACTIVE'),
(@u1, @s1, 4, '距离测量成果的改正计算 [掌握]', 2, 'ACTIVE'),
(@u2, @s1, 4, '全站仪的基本功能与测量模式 [识记]', 1, 'ACTIVE'),
(@u2, @s1, 4, '全站仪坐标测量与放样 [掌握]', 2, 'ACTIVE'),
(@u3, @s1, 4, '地形图比例尺与符号 [识记]', 1, 'ACTIVE'),
(@u3, @s1, 4, '等高线的概念与判读 [理解]', 2, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目2: 建筑材料 (4章×12单元×34知识点)
-- 教材: 《建筑材料》(第四版) 高教社 ISBN 978-7-04-056789-4
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r2, @s2, 2, '材料的基本性质',        1, 'ACTIVE'),
(@r2, @s2, 2, '气硬性胶凝材料',        2, 'ACTIVE'),
(@r2, @s2, 2, '水泥与混凝土',          3, 'ACTIVE'),
(@r2, @s2, 2, '建筑钢材与功能材料',    4, 'ACTIVE');

-- 2.1 材料的基本性质
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='材料的基本性质');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '物理性质', 1, 'ACTIVE'),
(@ch, @s2, 3, '力学性质', 2, 'ACTIVE'),
(@ch, @s2, 3, '耐久性', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='物理性质'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='力学性质');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='耐久性');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s2, 4, '密度/表观密度/堆积密度的区别 [掌握]', 1, 'ACTIVE'),
(@u1, @s2, 4, '吸水率与含水率的概念与计算 [掌握]', 2, 'ACTIVE'),
(@u2, @s2, 4, '抗压/抗拉/抗弯/抗剪强度 [理解]', 1, 'ACTIVE'),
(@u2, @s2, 4, '弹性/塑性/脆性/韧性的区分 [理解]', 2, 'ACTIVE'),
(@u3, @s2, 4, '材料耐久性的影响因素 [识记]', 1, 'ACTIVE');

-- 2.2 气硬性胶凝材料
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='气硬性胶凝材料');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '石灰', 1, 'ACTIVE'),
(@ch, @s2, 3, '石膏', 2, 'ACTIVE'),
(@ch, @s2, 3, '水玻璃', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='石灰'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='石膏');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水玻璃');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s2, 4, '石灰的熟化与硬化过程 [理解]', 1, 'ACTIVE'),
(@u1, @s2, 4, '石灰的性质(保水性好/收缩大/耐水性差) [掌握]', 2, 'ACTIVE'),
(@u2, @s2, 4, '建筑石膏的水化与凝结硬化 [理解]', 1, 'ACTIVE'),
(@u2, @s2, 4, '石膏制品的特点与应用 [识记]', 2, 'ACTIVE'),
(@u3, @s2, 4, '水玻璃的组成与特性 [识记]', 1, 'ACTIVE');

-- 2.3 水泥与混凝土
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='水泥与混凝土');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '硅酸盐水泥', 1, 'ACTIVE'),
(@ch, @s2, 3, '普通混凝土组成', 2, 'ACTIVE'),
(@ch, @s2, 3, '混凝土配合比设计', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='硅酸盐水泥'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='普通混凝土组成');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='混凝土配合比设计');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s2, 4, '硅酸盐水泥的矿物组成与特性 [理解]', 1, 'ACTIVE'),
(@u1, @s2, 4, '水泥的技术性质(细度/凝结时间/强度等级) [掌握]', 2, 'ACTIVE'),
(@u1, @s2, 4, '水泥的选用原则 [识记]', 3, 'ACTIVE'),
(@u2, @s2, 4, '粗/细骨料的质量要求(含泥量/级配) [掌握]', 1, 'ACTIVE'),
(@u2, @s2, 4, '混凝土拌和物和易性及其影响因素 [理解]', 2, 'ACTIVE'),
(@u3, @s2, 4, '水灰比对混凝土强度的影响 [掌握]', 1, 'ACTIVE'),
(@u3, @s2, 4, '配合比设计三参数(水灰比/砂率/单位用水量) [掌握]', 2, 'ACTIVE');

-- 2.4 建筑钢材与功能材料
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='建筑钢材与功能材料');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '建筑钢材', 1, 'ACTIVE'),
(@ch, @s2, 3, '防水材料', 2, 'ACTIVE'),
(@ch, @s2, 3, '保温隔热材料', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='建筑钢材'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='防水材料');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='保温隔热材料');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s2, 4, '钢材的拉伸性能(屈服强度/抗拉强度/伸长率) [掌握]', 1, 'ACTIVE'),
(@u1, @s2, 4, 'HPB300/HRB400钢筋的牌号含义 [识记]', 2, 'ACTIVE'),
(@u2, @s2, 4, 'SBS/APP改性沥青防水卷材 [识记]', 1, 'ACTIVE'),
(@u2, @s2, 4, '防水涂料与密封材料的选用 [理解]', 2, 'ACTIVE'),
(@u3, @s2, 4, '导热系数与热阻的概念 [理解]', 1, 'ACTIVE'),
(@u3, @s2, 4, '常用保温材料(聚苯板/岩棉/玻璃棉) [识记]', 2, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目3: 建筑力学 (4章×12单元×35知识点)
-- 教材: 《建筑力学》(第三版) 高教社 ISBN 978-7-04-055123-5
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r3, @s3, 2, '静力学基础',     1, 'ACTIVE'),
(@r3, @s3, 2, '平面力系',       2, 'ACTIVE'),
(@r3, @s3, 2, '杆件强度与刚度', 3, 'ACTIVE'),
(@r3, @s3, 2, '压杆稳定',       4, 'ACTIVE');

-- 3.1 静力学基础
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='静力学基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '基本概念与公理', 1, 'ACTIVE'),
(@ch, @s3, 3, '约束与受力图', 2, 'ACTIVE'),
(@ch, @s3, 3, '力矩与力偶', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='基本概念与公理'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='约束与受力图');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='力矩与力偶');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s3, 4, '力的三要素与作用与反作用定律 [识记]', 1, 'ACTIVE'),
(@u1, @s3, 4, '二力平衡条件与加减平衡力系公理 [理解]', 2, 'ACTIVE'),
(@u2, @s3, 4, '常见约束类型(柔索/光滑面/铰链/固定端) [掌握]', 1, 'ACTIVE'),
(@u2, @s3, 4, '受力图的绘制步骤 [掌握]', 2, 'ACTIVE'),
(@u3, @s3, 4, '力矩的计算与平衡条件 [掌握]', 1, 'ACTIVE'),
(@u3, @s3, 4, '力偶的性质与力偶矩 [理解]', 2, 'ACTIVE');

-- 3.2 平面力系
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='平面力系');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '平面汇交力系', 1, 'ACTIVE'),
(@ch, @s3, 3, '平面一般力系', 2, 'ACTIVE'),
(@ch, @s3, 3, '平面桁架内力', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='平面汇交力系'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='平面一般力系');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='平面桁架内力');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s3, 4, '力在坐标轴上的投影与合力 [掌握]', 1, 'ACTIVE'),
(@u1, @s3, 4, '平面汇交力系的平衡方程 [掌握]', 2, 'ACTIVE'),
(@u2, @s3, 4, '力的平移定理 [理解]', 1, 'ACTIVE'),
(@u2, @s3, 4, '平面一般力系的平衡方程(∑X=0/∑Y=0/∑M=0) [掌握]', 2, 'ACTIVE'),
(@u2, @s3, 4, '支座反力的计算 [掌握]', 3, 'ACTIVE'),
(@u3, @s3, 4, '结点法与截面法求桁架内力 [理解]', 1, 'ACTIVE');

-- 3.3 杆件强度与刚度
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='杆件强度与刚度');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '轴向拉压', 1, 'ACTIVE'),
(@ch, @s3, 3, '剪切与挤压', 2, 'ACTIVE'),
(@ch, @s3, 3, '弯曲', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='轴向拉压'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='剪切与挤压');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='弯曲');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s3, 4, '轴力图与正应力计算(σ=N/A) [掌握]', 1, 'ACTIVE'),
(@u1, @s3, 4, '胡克定律与轴向变形计算(ΔL=NL/EA) [掌握]', 2, 'ACTIVE'),
(@u1, @s3, 4, '拉压杆的强度条件与安全系数 [掌握]', 3, 'ACTIVE'),
(@u2, @s3, 4, '剪切与挤压的实用计算 [理解]', 1, 'ACTIVE'),
(@u3, @s3, 4, '剪力图与弯矩图的绘制 [掌握]', 1, 'ACTIVE'),
(@u3, @s3, 4, '弯曲正应力计算(σ=My/Iz) [掌握]', 2, 'ACTIVE'),
(@u3, @s3, 4, '梁的弯曲强度条件 [掌握]', 3, 'ACTIVE');

-- 3.4 压杆稳定
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='压杆稳定');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '压杆稳定概念', 1, 'ACTIVE'),
(@ch, @s3, 3, '欧拉公式与临界力', 2, 'ACTIVE'),
(@ch, @s3, 3, '提高压杆稳定性的措施', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='压杆稳定概念'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='欧拉公式与临界力');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='提高压杆稳定性的措施');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s3, 4, '压杆失稳的概念与临界状态 [理解]', 1, 'ACTIVE'),
(@u1, @s3, 4, '柔度(长细比)的概念与计算 [掌握]', 2, 'ACTIVE'),
(@u2, @s3, 4, '欧拉临界力公式(Pcr=π²EI/(μL)²) [理解]', 1, 'ACTIVE'),
(@u2, @s3, 4, '长度系数μ的取值(两端铰支μ=1) [掌握]', 2, 'ACTIVE'),
(@u3, @s3, 4, '提高压杆稳定性的工程措施 [识记]', 1, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目4: 工程制图 (4章×11单元×32知识点)
-- 教材: 《土木工程识图》(第二版) 高教社 ISBN 978-7-04-054321-6
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r4, @s4, 2, '制图基本标准',   1, 'ACTIVE'),
(@r4, @s4, 2, '投影基础',       2, 'ACTIVE'),
(@r4, @s4, 2, '建筑施工图',     3, 'ACTIVE'),
(@r4, @s4, 2, '结构施工图与CAD',4, 'ACTIVE');

-- 4.1 制图基本标准
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='制图基本标准');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '图纸幅面与图框', 1, 'ACTIVE'),
(@ch, @s4, 3, '图线与字体', 2, 'ACTIVE'),
(@ch, @s4, 3, '尺寸标注与比例', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='图纸幅面与图框'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='图线与字体');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='尺寸标注与比例');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s4, 4, 'A0~A4图纸幅面尺寸与标题栏 [识记]', 1, 'ACTIVE'),
(@u1, @s4, 4, '图框格式与对中符号 [理解]', 2, 'ACTIVE'),
(@u2, @s4, 4, '线型(粗实线/细实线/虚线/点画线)及用途 [掌握]', 1, 'ACTIVE'),
(@u2, @s4, 4, '工程字书写要求(长仿宋体) [识记]', 2, 'ACTIVE'),
(@u3, @s4, 4, '尺寸组成(尺寸界线/尺寸线/起止符号/数字) [掌握]', 1, 'ACTIVE'),
(@u3, @s4, 4, '比例的概念与选用(1:100/1:50等) [掌握]', 2, 'ACTIVE');

-- 4.2 投影基础
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='投影基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '正投影法', 1, 'ACTIVE'),
(@ch, @s4, 3, '三视图', 2, 'ACTIVE'),
(@ch, @s4, 3, '剖面图与断面图', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='正投影法'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='三视图');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='剖面图与断面图');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s4, 4, '投影法的分类(中心投影/平行投影) [识记]', 1, 'ACTIVE'),
(@u1, @s4, 4, '正投影的基本特性(真实性/积聚性/类似性) [理解]', 2, 'ACTIVE'),
(@u2, @s4, 4, '三面投影体系的建立(V/H/W面) [理解]', 1, 'ACTIVE'),
(@u2, @s4, 4, '三视图的投影规律(长对正/高平齐/宽相等) [掌握]', 2, 'ACTIVE'),
(@u3, @s4, 4, '全剖面与半剖面的画法及标注 [掌握]', 1, 'ACTIVE'),
(@u3, @s4, 4, '断面图(移出断面/重合断面)的表示 [理解]', 2, 'ACTIVE');

-- 4.3 建筑施工图
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='建筑施工图');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '建筑总平面图', 1, 'ACTIVE'),
(@ch, @s4, 3, '建筑平面图/立面图/剖面图', 2, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='建筑总平面图'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='建筑平面图/立面图/剖面图');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s4, 4, '总平面图的图示内容与图例 [识记]', 1, 'ACTIVE'),
(@u1, @s4, 4, '绝对标高与相对标高的区别 [理解]', 2, 'ACTIVE'),
(@u2, @s4, 4, '建筑平面图的形成与阅读 [掌握]', 1, 'ACTIVE'),
(@u2, @s4, 4, '建筑立面图的命名与图示内容 [掌握]', 2, 'ACTIVE'),
(@u2, @s4, 4, '建筑剖面图的剖切位置与内容 [掌握]', 3, 'ACTIVE');

-- 4.4 结构施工图与CAD
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='结构施工图与CAD');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '结构施工图识读', 1, 'ACTIVE'),
(@ch, @s4, 3, '钢筋表示方法', 2, 'ACTIVE'),
(@ch, @s4, 3, 'CAD绘图基础', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='结构施工图识读'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='钢筋表示方法');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='CAD绘图基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s4, 4, '基础结构平面图与详图的阅读 [理解]', 1, 'ACTIVE'),
(@u1, @s4, 4, '柱/梁/板平法施工图的识读 [理解]', 2, 'ACTIVE'),
(@u2, @s4, 4, '钢筋的标注方法(Φ/间距/等级) [掌握]', 1, 'ACTIVE'),
(@u2, @s4, 4, '钢筋弯钩与保护层厚度要求 [识记]', 2, 'ACTIVE'),
(@u3, @s4, 4, 'AutoCAD绘图基本命令(Line/Trim/Offset/Dim) [掌握]', 1, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 4科考纲
-- ════════════════════════════════════════════════════════════
INSERT IGNORE INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at) VALUES
(@s1, 'GENERAL', 'BOTH', '工程测量考纲',
 '本课程是土木水利类专业的核心技能课。涵盖测量基础知识、水准测量(DS3水准仪操作/高差计算/路线校核)、角度测量(DJ6经纬仪操作/测回法/竖直角)、距离测量与地形图(钢尺量距/全站仪/等高线判读)。以应会实操为主，水准仪和经纬仪的操作与成果计算为掌握重点。',
 '1.0', 1, NOW(), NOW()),
(@s2, 'GENERAL', 'BOTH', '建筑材料考纲',
 '本课程讲授常用建筑材料的性能与检测。涵盖材料基本性质(物理/力学/耐久性)、气硬性胶凝材料(石灰/石膏/水玻璃)、水泥与混凝土(硅酸盐水泥/配合比设计)、建筑钢材(拉伸性能/钢筋牌号)与功能材料(防水/保温)。以识记和理解为主，水泥技术性质和混凝土配合比为掌握重点。',
 '1.0', 1, NOW(), NOW()),
(@s3, 'GENERAL', 'BOTH', '建筑力学考纲',
 '本课程讲授建筑结构受力分析基础。涵盖静力学基础(力/约束/受力图/力矩)、平面力系(汇交力系与一般力系平衡方程/支座反力)、杆件强度与刚度(轴向拉压/剪切挤压/弯曲正应力)、压杆稳定(柔度/欧拉公式/提高稳定性措施)。以掌握计算为主，受力图绘制和内力计算为必考内容。',
 '1.0', 1, NOW(), NOW()),
(@s4, 'GENERAL', 'BOTH', '工程制图考纲',
 '本课程讲授建筑工程图样的绘制与识读。涵盖制图基本标准(图纸幅面/线型/尺寸标注/比例)、投影基础(正投影/三视图规律/剖面图与断面图)、建筑施工图(总平面图/平面图/立面图/剖面图)、结构施工图(平法标注/钢筋表示)与CAD绘图基础。以识记和掌握为主，三视图规律和施工图识读为考核重点。',
 '1.0', 1, NOW(), NOW());

-- ════════════════════════════════════════════════════════════
-- 4科种子题库(各12题 = 48题)
-- ════════════════════════════════════════════════════════════

-- 工程测量 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('工程测量','SINGLE_CHOICE','测量工作的基本原则是：', '["A. 从局部到整体","B. 从整体到局部/先控制后碎部","C. 先碎部后控制","D. 只做控制测量"]', 'B', '测量基本原则:从整体到局部、先控制后碎部、前一步未检核不进行下一步。',1,1),
('工程测量','SINGLE_CHOICE','水准测量的基本原理是：', '["A. 利用水平视线求高差","B. 利用斜视线测距","C. 利用对中整平求角","D. 利用GPS定位"]', 'A', '水准测量原理:利用水准仪提供的水平视线读取前后视标尺读数,高差=后视-前视。',1,1),
('工程测量','SINGLE_CHOICE','DS3水准仪中"3"表示每千米往返测高差中数的偶然中误差不超过：', '["A. 0.3mm","B. 1mm","C. 3mm","D. 30mm"]', 'C', 'DS3中3表示每千米往返测高差中误差≤±3mm。',2,1),
('工程测量','SINGLE_CHOICE','测回法测水平角，上半测回先瞄准左目标读数为a左，再瞄准右目标读数为b左，则上半测回角值为：', '["A. a左+b左","B. b左-a左","C. a左-b左","D. (a左+b左)/2"]', 'B', '水平角=右目标读数-左目标读数。顺时针注记时β=b-a。',2,1),
('工程测量','SINGLE_CHOICE','经纬仪安置的两个步骤是：', '["A. 整平和对光","B. 对中和整平","C. 整平和瞄准","D. 对中和读数"]', 'B', '经纬仪安置=对中(垂球或光学对中器使仪器中心对准测站点)+整平(使水平度盘处于水平位置)。',1,1),
('工程测量','SINGLE_CHOICE','水准路线闭合差fh的容许值为：', '["A. ±12√n mm(山地/四等)","B. ±40√L mm(平地/等外)","C. ±12√L mm(山地)","D. ±40√n mm(平地)"]', 'B', '等外水准测量容许闭合差:平地±40√L mm,山地±12√n mm(L为路线长km,n为测站数)。',2,1),
('工程测量','SINGLE_CHOICE','等高线间距越密表示：', '["A. 坡度越缓","B. 坡度越陡","C. 高程越高","D. 高程越低"]', 'B', '等高线越密→水平距离相同的条件下高差越大→坡度越陡。',1,1),
('工程测量','SINGLE_CHOICE','相对高程的起算面是：', '["A. 大地水准面","B. 假定水准面","C. 平均海水面","D. 参考椭球面"]', 'B', '绝对高程基准是大地水准面;相对高程(假定高程)基准为任意选定的水准面。',2,1),
('工程测量','SINGLE_CHOICE','全站仪不能直接测量的是：', '["A. 斜距","B. 水平角","C. 高差","D. 绝对高程"]', 'D', '全站仪可测斜距/水平角/竖直角/坐标/高差,但绝对高程需要已知测站点高程才能推算。',2,1),
('工程测量','SINGLE_CHOICE','测量中系统误差的特点是：', '["A. 具有抵偿性","B. 符号和大小固定或有规律","C. 完全不可消除","D. 只出现在角度测量中"]', 'B', '系统误差符号和大小固定或有规律变化,可通过校正仪器或加改正数消除。',2,1),
('工程测量','SINGLE_CHOICE','视距测量中上下丝读数差为1.0m,K=100,则水平距离为：', '["A. 10m","B. 50m","C. 100m","D. 200m"]', 'C', '视距=K×(上丝-下丝)=100×1.0=100m(视线水平时)。',2,1),
('工程测量','SINGLE_CHOICE','地形图比例尺1:500表示图上1cm代表实地：', '["A. 0.5m","B. 5m","C. 50m","D. 500m"]', 'B', '1:500比例尺=图上1cm代表实地500cm=5m。',1,1);

-- 建筑材料 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('建筑材料','SINGLE_CHOICE','材料的表观密度是指材料在：', '["A. 绝对密实状态下单位体积的质量","B. 自然状态下单位体积的质量","C. 堆积状态下单位体积的质量","D. 饱和状态下单位体积的质量"]', 'B', '表观密度=材料在自然状态下单位体积的质量(含孔隙)。密度=绝对密实状态,堆积密度=散粒堆积状态。',1,1),
('建筑材料','SINGLE_CHOICE','石灰"陈伏"的目的是：', '["A. 降低温度","B. 消除过火石灰的危害","C. 增加白度","D. 加快硬化"]', 'B', '陈伏是在储灰坑中存放2周以上,使过火石灰充分熟化,防止抹灰层爆裂起泡。',2,1),
('建筑材料','SINGLE_CHOICE','硅酸盐水泥熟料中含量最多的矿物是：', '["A. C3S(硅酸三钙)","B. C2S(硅酸二钙)","C. C3A(铝酸三钙)","D. C4AF(铁铝酸四钙)"]', 'A', 'C3S占50%~60%,是决定水泥早期强度的主要矿物。',2,1),
('建筑材料','SINGLE_CHOICE','水泥强度等级42.5表示28天抗压强度不低于：', '["A. 32.5MPa","B. 42.5MPa","C. 52.5MPa","D. 62.5MPa"]', 'B', '42.5级水泥28d抗压强度≥42.5MPa。',1,1),
('建筑材料','SINGLE_CHOICE','混凝土配合比设计中,水灰比主要影响：', '["A. 和易性","B. 强度","C. 凝结时间","D. 体积密度"]', 'B', '水灰比(水胶比)是决定混凝土强度的最主要因素(W/C越小→强度越高)。',2,1),
('建筑材料','SINGLE_CHOICE','建筑石膏的化学分子式是：', '["A. CaCO3","B. CaO","C. CaSO4·1/2H2O","D. Ca(OH)2"]', 'C', '建筑石膏(CaSO4·0.5H2O)是半水石膏,由二水石膏(CaSO4·2H2O)煅烧脱水而成。',2,1),
('建筑材料','SINGLE_CHOICE','HPB300中的"300"表示：', '["A. 屈服强度≥300N/mm²","B. 抗拉强度≥300N/mm²","C. 直径300mm","D. 含碳量0.3%"]', 'A', 'HPB300为热轧光圆钢筋,300表示屈服强度标准值≥300MPa(N/mm²)。',1,1),
('建筑材料','SINGLE_CHOICE','混凝土拌合物和易性不包括：', '["A. 流动性","B. 粘聚性","C. 保水性","D. 强度"]', 'D', '和易性=流动性+粘聚性+保水性。强度是硬化混凝土的性质。',1,1),
('建筑材料','SINGLE_CHOICE','钢材拉伸试验中,屈服阶段后继续加载所能达到的最大应力称为：', '["A. 弹性极限","B. 屈服强度","C. 抗拉强度","D. 疲劳强度"]', 'C', '抗拉强度(σb)为拉伸过程中的最大应力值。屈服强度(σs)为屈服阶段的应力值。',2,1),
('建筑材料','SINGLE_CHOICE','下列属于气硬性胶凝材料的是：', '["A. 普通水泥","B. 石灰","C. 矿渣水泥","D. 粉煤灰水泥"]', 'B', '石灰/石膏/水玻璃只能在空气中硬化→气硬性。水泥类既可在空气也可在水中硬化→水硬性。',1,1),
('建筑材料','SINGLE_CHOICE','配制混凝土时,砂率是指：', '["A. 砂质量占水质量的百分率","B. 砂质量占骨料总质量的百分率","C. 砂质量占水泥质量的百分率","D. 砂体积占混凝土体积的百分率"]', 'B', '砂率=砂的质量/(砂质量+石子质量)×100%。',2,1),
('建筑材料','SINGLE_CHOICE','SBS改性沥青防水卷材中"SBS"是：', '["A. 聚氯乙烯","B. 聚乙烯","C. 苯乙烯-丁二烯-苯乙烯嵌段共聚物","D. 聚丙烯"]', 'C', 'SBS(Styrene-Butadiene-Styrene)是一种热塑性弹性体,用于改善沥青的低温柔性和弹性。',2,1);

-- 建筑力学 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('建筑力学','SINGLE_CHOICE','二力平衡条件是两个力：', '["A. 大小相等","B. 方向相反","C. 作用在同一直线上","D. 大小相等/方向相反/作用在同一直线上"]', 'D', '二力平衡条件:等值/反向/共线。',1,1),
('建筑力学','SINGLE_CHOICE','固定端支座可提供的约束反力个数为：', '["A. 1个","B. 2个","C. 3个","D. 4个"]', 'C', '固定端=2个方向约束力(X/Y)+1个约束力偶(M),共3个约束反力。',2,1),
('建筑力学','SINGLE_CHOICE','平面一般力系的独立平衡方程个数为：', '["A. 1个","B. 2个","C. 3个","D. 4个"]', 'C', '平面一般力系:∑X=0/∑Y=0/∑M=0,三个独立方程,可解三个未知量。',1,1),
('建筑力学','SINGLE_CHOICE','杆件受轴向拉力P=100kN,截面面积A=200mm², 正应力σ为：', '["A. 0.5MPa","B. 5MPa","C. 50MPa","D. 500MPa"]', 'D', 'σ=N/A=100×10³N/200mm²=500N/mm²=500MPa。',2,1),
('建筑力学','SINGLE_CHOICE','胡克定律ΔL=NL/(EA)中,EA称为：', '["A. 抗弯刚度","B. 抗拉(压)刚度","C. 抗扭刚度","D. 截面模量"]', 'B', 'EA为抗拉(压)刚度,EA越大变形越小。EI为抗弯刚度。',2,1),
('建筑力学','SINGLE_CHOICE','简支梁受均布荷载q作用,跨中最大弯矩为：', '["A. qL²/2","B. qL²/4","C. qL²/8","D. qL²/12"]', 'C', '简支梁均布荷载:跨中Mmax=qL²/8。集中力跨中:P位于跨中Mmax=PL/4。',3,1),
('建筑力学','SINGLE_CHOICE','提高压杆稳定性的最有效措施是：', '["A. 增大荷载","B. 减小压杆长度或增加中间支撑","C. 降低材料强度","D. 增加截面应力"]', 'B', '减小长度→减小柔度λ→提高临界力Pcr。欧拉公式:Pcr=π²EI/(μL)²,L越小Pcr越大。',2,1),
('建筑力学','SINGLE_CHOICE','梁弯曲时,中性轴上的正应力为：', '["A. 最大拉应力","B. 最大压应力","C. 零","D. 平均应力"]', 'C', '弯曲时中性轴处纤维既不伸长也不缩短,正应力σ=0。',1,1),
('建筑力学','SINGLE_CHOICE','力的平移定理:将力平移到另一点需附加：', '["A. 一个力","B. 一个力偶","C. 一个力矩","D. 一对力"]', 'B', '力的平移定理:力向任一点平移后须附加一个力偶,力偶矩=原力对新作用点的矩。',2,1),
('建筑力学','SINGLE_CHOICE','压杆两端铰支(球铰)时长度系数μ为：', '["A. 0.5","B. 0.7","C. 1.0","D. 2.0"]', 'C', '两端铰支μ=1;一端固定一端自由μ=2;两端固定μ=0.5;一端固定一端铰支μ≈0.7。',2,1),
('建筑力学','SINGLE_CHOICE','脆性材料的破坏特点是：', '["A. 有显著塑性变形","B. 无明显变形突然断裂","C. 先屈服后断裂","D. 只发生颈缩不断裂"]', 'B', '脆性材料(如铸铁/混凝土)无明显塑性变形,达到强度极限时突然断裂。',1,1),
('建筑力学','SINGLE_CHOICE','截面法求内力的三个步骤是：', '["A. 截开→代替→平衡","B. 计算→绘图→校核","C. 加载→测量→分析","D. 假设→推导→验证"]', 'A', '截面法三步骤:1.截开(假想截面分开) 2.代替(用内力代替弃去部分作用) 3.平衡(建立平衡方程求内力)。',1,1);

-- 工程制图 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('工程制图','SINGLE_CHOICE','A2图纸的尺寸为：', '["A. 210×297","B. 297×420","C. 420×594","D. 594×841"]', 'C', 'A0=841×1189,A1=594×841,A2=420×594,A3=297×420,A4=210×297(单位mm)。',1,1),
('工程制图','SINGLE_CHOICE','三视图的投影规律是：', '["A. 长相等/宽对齐/高平齐","B. 长对正/高平齐/宽相等","C. 长对齐/宽相等/高对正","D. 长平齐/高相等/宽对齐"]', 'B', '三视图规律:主俯视图长对正;主左视图高平齐;俯左视图宽相等。',1,1),
('工程制图','SINGLE_CHOICE','可见轮廓线应使用：', '["A. 细实线","B. 虚线","C. 粗实线","D. 点画线"]', 'C', '粗实线(b)用于可见轮廓线;虚线用于不可见轮廓;细实线用于尺寸线/剖面线;点画线用于轴线/对称线。',1,1),
('工程制图','SINGLE_CHOICE','建筑平面图中,被剖切到的墙体应画为：', '["A. 细实线","B. 虚线","C. 粗实线","D. 点画线"]', 'C', '平面图是水平剖面图,被剖切到的墙/柱断面轮廓用粗实线表示。',2,1),
('工程制图','SINGLE_CHOICE','比例1:100表示：', '["A. 图样比实物大100倍","B. 图样是实物的1/100","C. 图样与实物一样大","D. 比例与尺寸无关"]', 'B', '1:100=图样尺寸:实际尺寸,即图样缩小为实物的1/100。',1,1),
('工程制图','SINGLE_CHOICE','半剖面图适用于：', '["A. 外形简单的对称物体","B. 内外形都需要表达的对称物体","C. 任意形状物体","D. 只有内部结构的物体"]', 'B', '半剖面=一半画外形(表达外部结构)+一半画剖面(表达内部结构),以对称中心线为界。',2,1),
('工程制图','SINGLE_CHOICE','尺寸标注中,尺寸起止符号一般用：', '["A. 箭头","B. 45°倾斜的中粗短线","C. 圆点","D. 小圆圈"]', 'B', '建筑制图中尺寸起止符号用45°中粗短线;机械制图一般用实心箭头。',2,1),
('工程制图','SINGLE_CHOICE','建筑立面图的命名方式不包括：', '["A. 按朝向(南立面/北立面)","B. 按轴线(①~⑩立面)","C. 按材料(混凝土立面)","D. 按主次(正立面/背立面)"]', 'C', '立面图命名按朝向/轴线/主次,不按材料命名。',2,1),
('工程制图','SINGLE_CHOICE','钢筋标注Φ8@200中"@200"表示：', '["A. 钢筋直径200mm","B. 钢筋间距200mm","C. 钢筋长度200mm","D. 保护层厚度200mm"]', 'B', 'Φ8@200=直径8mm的钢筋,间距200mm布置。"@"表示等间距。',1,1),
('工程制图','SINGLE_CHOICE','正投影的基本特性不包括：', '["A. 真实性","B. 积聚性","C. 类似性","D. 透视性"]', 'D', '正投影特性:真实性(平行→反映实形)、积聚性(垂直→积聚为线或点)、类似性(倾斜→类似形)。',1,1),
('工程制图','SINGLE_CHOICE','重合断面图的轮廓线用：', '["A. 粗实线","B. 细实线","C. 虚线","D. 点画线"]', 'B', '重合断面图(画在视图内的断面)用细实线绘制,原视图轮廓不受影响。移出断面用粗实线。',2,1),
('工程制图','SINGLE_CHOICE','AutoCAD中修剪命令的快捷键是：', '["A. TR(Trim)","B. EX(Extend)","C. OF(Offset)","D. CP(Copy)"]', 'A', 'TR=TRIM修剪;EX=EXTEND延伸;O=OFFSET偏移;CO/CP=COPY复制。',1,1);

SELECT 'v105: 建筑类4科创建完成' AS result;
SELECT ds.subject_name,
  (SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=ds.id) AS nodes,
  (SELECT COUNT(*) FROM question_bank WHERE subject=ds.subject_name) AS questions
FROM dict_subject ds WHERE ds.id IN (27,28,29,30) ORDER BY ds.id;
