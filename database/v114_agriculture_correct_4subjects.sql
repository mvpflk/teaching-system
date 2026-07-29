-- ============================================================
-- v114: 农林牧渔类[职高] 4科 — 对标四川省2023版官方考纲
-- 来源: sceea.cn 20230928 川教考院通知
-- 考试: 总分350(应知200+应会150) 纸笔考试150分钟
-- 组考院校: 成都农业科技职业学院
-- ============================================================

SET @s1=36; SET @s2=37; SET @s3=38; SET @s4=39;

-- ═══ 创建4个根节点 ═══
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(NULL, @s1, 1, '植物生产与环境[职高]', 1, 'ACTIVE'),
(NULL, @s2, 1, '畜禽营养与饲料[职高]', 1, 'ACTIVE'),
(NULL, @s3, 1, '动物解剖生理[职高]', 1, 'ACTIVE'),
(NULL, @s4, 1, '农业经营与管理[职高]', 1, 'ACTIVE');
SET @r1=(SELECT id FROM knowledge_nodes WHERE subject_id=@s1 AND level=1 LIMIT 1);
SET @r2=(SELECT id FROM knowledge_nodes WHERE subject_id=@s2 AND level=1 LIMIT 1);
SET @r3=(SELECT id FROM knowledge_nodes WHERE subject_id=@s3 AND level=1 LIMIT 1);
SET @r4=(SELECT id FROM knowledge_nodes WHERE subject_id=@s4 AND level=1 LIMIT 1);

-- ════════════════════════════════════════════════════════════
-- 科目1: 植物生产与环境 (35%≈70分, 7章×18单元×45知识点)
-- 教材: 宋志伟 第四版 ISBN 978-7-04-057936-9 高教社 2020
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r1, @s1, 2, '植物生产与环境概述', 1, 'ACTIVE'),
(@r1, @s1, 2, '植物的生长发育', 2, 'ACTIVE'),
(@r1, @s1, 2, '植物生产与土壤培肥', 3, 'ACTIVE'),
(@r1, @s1, 2, '植物生产与科学用水', 4, 'ACTIVE'),
(@r1, @s1, 2, '植物生产与光能利用', 5, 'ACTIVE'),
(@r1, @s1, 2, '植物生产与温度调控', 6, 'ACTIVE'),
(@r1, @s1, 2, '植物生产与农业气象', 7, 'ACTIVE');

-- 各章 Level3+Level4
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与环境概述');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '植物生长发育基本概念', 1, 'ACTIVE'),
(@ch, @s1, 3, '环境因素对植物的影响', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物生长发育基本概念');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '营养生长与生殖生长的概念及关系 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '植物生长的周期性(大周期/昼夜/季节) [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '植物生长的相关性(根冠比/顶端优势) [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='环境因素对植物的影响');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '温/光/水/气/肥五大环境因子概述 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '环境因子间的相互作用与综合影响 [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物的生长发育');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '植物细胞与组织', 1, 'ACTIVE'),
(@ch, @s1, 3, '植物的营养器官与生殖器官', 2, 'ACTIVE'),
(@ch, @s1, 3, '植物激素与生长调节剂', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物细胞与组织');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '植物细胞结构(细胞壁/膜/质/核/叶绿体/线粒体) [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '分生组织与成熟组织的类型与功能 [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '维管束的组成与功能 [了解]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物的营养器官与生殖器官');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '根的类型/根尖分区/根的吸收功能 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '茎的形态与分枝方式/茎的变态 [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '叶的形态与气孔功能 [掌握]', 3, 'ACTIVE'),
(@u, @s1, 4, '花的结构/传粉受精/种子结构与萌发 [掌握]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物激素与生长调节剂');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '五大激素(IAA/GA/CTK/ABA/ETH)的生理作用 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '植物生长调节剂在农业中的应用 [掌握]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与土壤培肥');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '土壤基本组成与性质', 1, 'ACTIVE'),
(@ch, @s1, 3, '肥料种类与科学施用', 2, 'ACTIVE'),
(@ch, @s1, 3, '测土配方施肥', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='土壤基本组成与性质');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '土壤三相组成(固/液/气) [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '土壤质地分类(砂土/壤土/黏土)与农业生产特性 [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '土壤有机质的作用与pH值 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='肥料种类与科学施用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '化肥种类(氮/磷/钾/复合肥)及代表品种 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '有机肥种类(粪尿肥/堆肥/绿肥)与特点 [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '微生物肥的概念与使用注意事项 [了解]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='测土配方施肥');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '测土配方施肥的基本程序 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '水肥一体化技术的概念与优点 [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与科学用水');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '水分与植物生长', 1, 'ACTIVE'),
(@ch, @s1, 3, '科学灌溉技术', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水分与植物生长');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '植物需水规律与需水临界期 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '蒸腾作用的概念/意义与调节 [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '空气湿度与降水的表示方法 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='科学灌溉技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '合理灌溉的指标(形态/生理/土壤) [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '主要灌溉方式(沟灌/喷灌/滴灌/渗灌)比较 [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与光能利用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '光合作用与呼吸作用', 1, 'ACTIVE'),
(@ch, @s1, 3, '光环境调控与光能利用率', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='光合作用与呼吸作用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '光合作用的过程/影响因素/生产应用 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '呼吸作用与光合作用的关系(区别与联系) [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '呼吸作用在农产品贮藏中的应用 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='光环境调控与光能利用率');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '太阳辐射与光照对植物生长发育的影响 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '光能利用率的概念与提高途径 [掌握]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与温度调控');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '温度对植物的影响', 1, 'ACTIVE'),
(@ch, @s1, 3, '温度调控技术', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='温度对植物的影响');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '三基点温度(最低/最适/最高) [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '农业界限温度(0/5/10/15/20℃)的意义 [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '积温的概念与计算(活动积温/有效积温) [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='温度调控技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '土壤温度与空气温度的变化规律 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '设施栽培中的温度调控措施(地膜/大棚/温室) [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与农业气象');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '主要农业气象灾害', 1, 'ACTIVE'),
(@ch, @s1, 3, '二十四节气与农事', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='主要农业气象灾害');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '干旱的类型与防御措施 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '涝灾/风灾/雹灾的危害与防御 [掌握]', 2, 'ACTIVE'),
(@u, @s1, 4, '极端温度灾害(霜冻/冷害/热害)的防御 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='二十四节气与农事');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '二十四节气的名称与顺序 [掌握]', 1, 'ACTIVE'),
(@u, @s1, 4, '主要节气与四川盆地农事活动的对应关系 [了解]', 2, 'ACTIVE'),
(@u, @s1, 4, '我国气候特点与农业区划概况 [了解]', 3, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目2: 畜禽营养与饲料 (25%≈50分, 4章×13单元×35知识点)
-- 教材: 邱以亮/伏桂华 第三版 ISBN 978-7-04-055610-0 高教社 2021
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r2, @s2, 2, '畜禽营养基础', 1, 'ACTIVE'),
(@r2, @s2, 2, '营养物质的利用', 2, 'ACTIVE'),
(@r2, @s2, 2, '饲料及其加工利用', 3, 'ACTIVE'),
(@r2, @s2, 2, '营养需要与配合饲料配制', 4, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='畜禽营养基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '动植物体的化学组成', 1, 'ACTIVE'),
(@ch, @s2, 3, '消化与吸收', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='动植物体的化学组成');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '动植物体化学元素与化合物的组成差异 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '水/蛋白质/脂肪/碳水化合物/矿物质/维生素六大营养素 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='消化与吸收');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '消化/吸收/消化率的概念与计算 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '畜禽的三种消化方式(物理/化学/微生物) [掌握]', 2, 'ACTIVE'),
(@u, @s2, 4, '影响消化力的因素 [掌握]', 3, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='营养物质的利用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '蛋白质的营养', 1, 'ACTIVE'),
(@ch, @s2, 3, '碳水化合物的营养', 2, 'ACTIVE'),
(@ch, @s2, 3, '脂肪的营养', 3, 'ACTIVE'),
(@ch, @s2, 3, '矿物质的营养', 4, 'ACTIVE'),
(@ch, @s2, 3, '维生素的营养', 5, 'ACTIVE'),
(@ch, @s2, 3, '能量的转化与利用', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='蛋白质的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '必需/非必需氨基酸与限制性氨基酸概念 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '单胃动物蛋白质消化特点 [掌握]', 2, 'ACTIVE'),
(@u, @s2, 4, '反刍动物非蛋白氮(NPN)利用原理 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='碳水化合物的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '碳水化合物的组成与营养作用 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '单胃与反刍动物碳水化合物消化代谢特点 [掌握]', 2, 'ACTIVE'),
(@u, @s2, 4, '粗纤维的营养作用与合理利用 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='脂肪的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '脂肪的营养作用 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '饲料脂肪对畜产品(肉/乳/蛋)品质的影响 [了解]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='矿物质的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '常量元素(Ca/P/Na/Cl/K/Mg/S)的功能与缺乏症 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '微量元素(Fe/Cu/Co/Mn/Zn/Se/I)的功能与缺乏症 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='维生素的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '脂溶性维生素(A/D/E/K)的来源/功能/缺乏症 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '水溶性维生素(B族/C)的来源/功能/缺乏症 [掌握]', 2, 'ACTIVE'),
(@u, @s2, 4, '应激对维生素需求的影响 [了解]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='能量的转化与利用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '总能→消化能→代谢能→净能的转化关系 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '能量在畜禽生产中的分配(维持/生产) [掌握]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='饲料及其加工利用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '饲料分类与特性', 1, 'ACTIVE'),
(@ch, @s2, 3, '饲料加工与调制', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='饲料分类与特性');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '国际饲料分类法(8大类)与中国饲料分类法 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '粗饲料/青绿饲料/青贮饲料的营养特性 [掌握]', 2, 'ACTIVE'),
(@u, @s2, 4, '能量饲料/蛋白质饲料/矿物质饲料的特性 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='饲料加工与调制');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '青贮饲料制作原理与方法(含水量65%~75%) [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '粗饲料氨化/碱化处理技术 [掌握]', 2, 'ACTIVE'),
(@u, @s2, 4, '饲料添加剂(营养性/药物性/一般)的种类与使用规范 [掌握]', 3, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='营养需要与配合饲料配制');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '畜禽营养需要', 1, 'ACTIVE'),
(@ch, @s2, 3, '配合饲料配制技术', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='畜禽营养需要');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '维持需要与生产需要的概念 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '饲养标准的概念与使用方法 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='配合饲料配制技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '全价配合饲料配方设计原则与方形法计算 [掌握]', 1, 'ACTIVE'),
(@u, @s2, 4, '浓缩饲料与添加剂预混料的概念与配制 [掌握]', 2, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目3: 动物解剖生理 (10%≈20分, 4章×12单元×28知识点)
-- 教材: 孟婷/徐金花 第四版 ISBN 978-7-04-057246-9 高教社 2021
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r3, @s3, 2, '动物体基本结构', 1, 'ACTIVE'),
(@r3, @s3, 2, '运动系统', 2, 'ACTIVE'),
(@r3, @s3, 2, '消化系统', 3, 'ACTIVE'),
(@r3, @s3, 2, '呼吸系统', 4, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='动物体基本结构');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '细胞与基本组织', 1, 'ACTIVE'),
(@ch, @s3, 3, '解剖学方位术语', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='细胞与基本组织');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '动物细胞结构(细胞膜/质/核/线粒体/核糖体) [掌握]', 1, 'ACTIVE'),
(@u, @s3, 4, '四种基本组织(上皮/结缔/肌肉/神经)的结构与功能 [掌握]', 2, 'ACTIVE'),
(@u, @s3, 4, '器官/系统/有机体的概念 [了解]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='解剖学方位术语');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '畜体方位术语(背侧/腹侧/前/后/近端/远端等) [掌握]', 1, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='运动系统');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '骨骼系统', 1, 'ACTIVE'),
(@ch, @s3, 3, '骨连结与关节', 2, 'ACTIVE'),
(@ch, @s3, 3, '肌肉系统概述', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='骨骼系统');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '骨的主要成分(有机物+无机物)与结构 [掌握]', 1, 'ACTIVE'),
(@u, @s3, 4, '骨的分类(长骨/短骨/扁骨)与功能 [了解]', 2, 'ACTIVE'),
(@u, @s3, 4, '牛全身骨骼特征与骨性标志 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='骨连结与关节');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '骨连结的类型(纤维/软骨/滑膜) [了解]', 1, 'ACTIVE'),
(@u, @s3, 4, '关节的基本结构与分类 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='肌肉系统概述');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '重要肌肉的形态识别与功能 [了解]', 1, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='消化系统');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '消化管的结构', 1, 'ACTIVE'),
(@ch, @s3, 3, '消化腺与消化生理', 2, 'ACTIVE'),
(@ch, @s3, 3, '反刍动物消化特点', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='消化管的结构');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '消化管管壁的一般结构(黏膜/黏膜下层/肌层/浆膜) [掌握]', 1, 'ACTIVE'),
(@u, @s3, 4, '小肠三段(十二指肠→空肠→回肠)的顺序 [掌握]', 2, 'ACTIVE'),
(@u, @s3, 4, '腹腔与腹膜腔的概念 [了解]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='消化腺与消化生理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '唾液腺/肝/胰的形态位置与生理功能 [掌握]', 1, 'ACTIVE'),
(@u, @s3, 4, '消化方式(机械/化学/生物)与各段消化特点 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='反刍动物消化特点');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '瘤胃微生物发酵消化的原理 [掌握]', 1, 'ACTIVE'),
(@u, @s3, 4, '皱胃黏膜上皮类型(单层柱状上皮) [掌握]', 2, 'ACTIVE'),
(@u, @s3, 4, '反刍的生理过程与意义 [掌握]', 3, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='呼吸系统');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '呼吸器官的结构', 1, 'ACTIVE'),
(@ch, @s3, 3, '呼吸生理', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='呼吸器官的结构');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '肺的形态与体内位置 [掌握]', 1, 'ACTIVE'),
(@u, @s3, 4, '鼻腔/咽/喉/气管/支气管的结构 [了解]', 2, 'ACTIVE'),
(@u, @s3, 4, '肺泡是气体交换的主要场所 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='呼吸生理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '呼吸运动的发生机理与生理意义 [掌握]', 1, 'ACTIVE'),
(@u, @s3, 4, '肺通气量/肺活量的概念 [了解]', 2, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目4: 农业经营与管理 (30%≈60分, 6章×16单元×35知识点)
-- 教材: 刘强/乔永信 第二版(2024年前)/第三版(2025起) 高教社
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r4, @s4, 2, '农业经营概述', 1, 'ACTIVE'),
(@r4, @s4, 2, '农产品市场营销', 2, 'ACTIVE'),
(@r4, @s4, 2, '农业资源与资金管理', 3, 'ACTIVE'),
(@r4, @s4, 2, '农业生产管理', 4, 'ACTIVE'),
(@r4, @s4, 2, '农业政策与法规', 5, 'ACTIVE'),
(@r4, @s4, 2, '农业创业与效益评价', 6, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业经营概述');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '农业的概念与特点', 1, 'ACTIVE'),
(@ch, @s4, 3, '农业经营方式', 2, 'ACTIVE'),
(@ch, @s4, 3, '新型农业经营主体', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业的概念与特点');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农业的概念/分类(农/林/牧/渔)与地位 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农业生产的特点(地域性/季节性/周期性/生物性) [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业经营方式');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '家庭经营/合作经营/企业经营的特点 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '适度规模经营的概念与意义 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='新型农业经营主体');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '专业大户/家庭农场/农民合作社/龙头企业 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农业社会化服务体系 [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农产品市场营销');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '市场调查与预测', 1, 'ACTIVE'),
(@ch, @s4, 3, '农产品营销策略', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='市场调查与预测');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '市场调查的方法(问卷/访谈/观察) [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农产品市场预测的基本方法 [了解]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农产品营销策略');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农产品4P营销组合(产品/价格/渠道/促销) [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农产品品牌建设与电子商务营销 [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业资源与资金管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '农业自然资源管理', 1, 'ACTIVE'),
(@ch, @s4, 3, '农业资金管理', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业自然资源管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '土地/水/生物资源的合理利用与保护 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农业资源的可持续利用原则 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业资金管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农业资金的来源与分类 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农业成本核算与控制方法 [掌握]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业生产管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '种植业生产管理', 1, 'ACTIVE'),
(@ch, @s4, 3, '养殖业生产管理', 2, 'ACTIVE'),
(@ch, @s4, 3, '农产品质量管理', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='种植业生产管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '种植制度(轮作/间套作)与生产计划编制 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '田间管理的主要内容与技术要求 [掌握]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='养殖业生产管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '畜禽养殖的生产周期与饲养管理要点 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '饲料供应计划与养殖成本控制 [了解]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农产品质量管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农产品质量标准与认证(无公害/绿色/有机) [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农产品质量追溯体系 [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业政策与法规');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '农业政策', 1, 'ACTIVE'),
(@ch, @s4, 3, '农业法规', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业政策');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '国家惠农政策(粮食直补/农资补贴/农机补贴) [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '乡村振兴战略的主要内容 [了解]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业法规');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农村土地承包法与土地流转 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '农产品质量安全法 [了解]', 2, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业创业与效益评价');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '农业创业基础', 1, 'ACTIVE'),
(@ch, @s4, 3, '农业经济效益评价', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业创业基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农业创业项目选择与可行性分析 [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '创业计划书的编制要点 [了解]', 2, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业经济效益评价');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农业经济效益的主要评价指标(产量/产值/纯收入/投入产出比) [掌握]', 1, 'ACTIVE'),
(@u, @s4, 4, '盈亏平衡分析与敏感性分析 [了解]', 2, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 4科考纲(官方内容)
-- ════════════════════════════════════════════════════════════
INSERT IGNORE INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at) VALUES
(@s1, 'DUIKOU', 'BOTH', '植物生产与环境考纲',
 '本课程占专业知识考试约35%(约70分)。涵盖植物生长发育基本概念与环境因子、植物细胞组织与器官、植物激素与生长调节剂、土壤三相组成与培肥、科学用水与灌溉技术、光合呼吸作用与光能利用、温度调控与积温计算、农业气象灾害与二十四节气等内容。应知重点:植物生长周期性/相关性、五大激素生理作用、土壤质地与有机质、化肥有机肥科学施用、蒸腾作用调节、光能利用率提高途径、三基点温度与积温。应会技能:显微镜使用、种子生活力测定(红墨水法)、千粒重测定、木本植物切接、病虫害识别与防治。',
 '2023', 1, NOW(), NOW()),
(@s2, 'DUIKOU', 'BOTH', '畜禽营养与饲料考纲',
 '本课程占专业知识考试约25%(约50分)。涵盖动植物体化学组成差异、消化吸收与消化率计算、六类营养物质(蛋白质/碳水化合物/脂肪/矿物质/维生素/能量)的营养作用与代谢特点、国际与中国饲料分类法、各类饲料(粗/青绿/青贮/能量/蛋白/矿物质)特性、饲料加工调制(青贮/氨化/碱化)、饲料添加剂种类与使用、畜禽维持需要与生产需要、全价配合饲料配方设计(方形法)、浓缩饲料与预混料配制。应知重点:必需氨基酸与限制性氨基酸、非蛋白氮利用、粗纤维营养作用、常量/微量元素缺乏症、脂溶/水溶性维生素功能、总能→净能转化。',
 '2023', 1, NOW(), NOW()),
(@s3, 'DUIKOU', 'BOTH', '动物解剖生理考纲',
 '本课程占专业知识考试约10%(约20分)。涵盖动物细胞结构与细胞器功能(线粒体/核糖体)、四种基本组织(上皮/结缔/肌肉/神经)、解剖学方位术语、骨的主要成分与结构、牛全身骨骼特征与骨性标志、骨连结类型与关节结构、消化管管壁一般结构、小肠三段顺序(十二指肠→空肠→回肠)、唾液腺/肝/胰的形态位置与功能、反刍动物消化特点、皱胃黏膜上皮类型、肺的形态与位置、呼吸运动发生机理与生理意义。应会技能:器官识别及系统归类、细菌平板划线分离、缝合技术(结节缝合/执刀式持针)、畜禽品种识别。',
 '2023', 1, NOW(), NOW()),
(@s4, 'DUIKOU', 'BOTH', '农业经营与管理考纲',
 '本课程占专业知识考试约30%(约60分)。涵盖农业概念/分类/特点、农业经营方式(家庭/合作/企业)、新型农业经营主体(专业大户/家庭农场/合作社/龙头企业)、农产品市场调查与4P营销组合、农业资源(土地/水/生物)合理利用、农业资金来源与成本核算、种植业/养殖业生产管理、农产品质量安全与三品认证(无公害/绿色/有机)、国家惠农政策、农村土地承包法与土地流转、农产品质量安全法、农业创业项目选择与可行性分析、农业经济效益评价指标与盈亏平衡分析。',
 '2023', 1, NOW(), NOW());

-- ════════════════════════════════════════════════════════════
-- 考纲-根节点关联
-- ════════════════════════════════════════════════════════════
INSERT IGNORE INTO exam_syllabus_node_relation (syllabus_id, node_id)
SELECT es.id, kn.id FROM exam_syllabus es JOIN knowledge_nodes kn ON es.subject_id=kn.subject_id AND kn.level=1
WHERE es.subject_id IN (36,37,38,39);

-- ════════════════════════════════════════════════════════════
-- 4科种子题库(各8题=32题)
-- ════════════════════════════════════════════════════════════
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
-- 植物生产与环境 8题
('植物生产与环境[职高]','SINGLE_CHOICE','植物生长的"顶端优势"是指：', '["A. 侧芽生长抑制顶芽","B. 顶芽生长抑制侧芽","C. 根尖生长抑制茎尖","D. 地下部生长抑制地上部"]', 'B', '顶端优势=主茎顶芽优先生长而侧芽生长受抑制的现象，与生长素的极性运输有关。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','下列属于五大植物激素之一、能促进果实成熟的是：', '["A. 生长素(IAA)","B. 赤霉素(GA)","C. 脱落酸(ABA)","D. 乙烯(ETH)"]', 'D', '乙烯(C2H4)是气体激素，主要生理作用=促进果实成熟和器官脱落。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','最理想的农业土壤质地类型是：', '["A. 砂土","B. 黏土","C. 壤土","D. 砾石土"]', 'C', '壤土砂黏适中，兼具砂土的通透性和黏土的保水保肥性，是最理想的质地。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','蒸腾作用对植物的主要意义不包括：', '["A. 产生蒸腾拉力促进水分运输","B. 降低叶温防止灼伤","C. 促进矿质元素吸收和运输","D. 增加光合产物合成"]', 'D', '蒸腾作用不直接合成光合产物。其三大意义=蒸腾拉力/降温/促矿质运输。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','农业界限温度中，春季日平均气温稳定通过多少度表示春季开始、越冬作物返青？', '["A. 0℃","B. 5℃","C. 10℃","D. 15℃"]', 'B', '5℃是多数越冬作物(冬小麦等)开始返青生长的界限温度。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','活动积温的计算方法是：', '["A. 日平均气温之和","B. 日平均气温与生物学下限温度之差之和","C. 日最高气温之和","D. 日均温减去10℃后的和"]', 'A', '活动积温=某一时期内日平均气温≥生物学下限温度的各日日平均气温之和。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','测土配方施肥的核心原则是：', '["A. 多施有机肥","B. 缺什么补什么，缺多少补多少","C. 一次性施足基肥","D. 以水调肥"]', 'B', '测土配方=土壤测试→缺什么补什么/缺多少补多少→精准施肥减量增效。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','二十四节气中，"芒种"通常在每年的：', '["A. 5月上旬","B. 5月下旬","C. 6月上旬","D. 6月下旬"]', 'C', '芒种(6月5~7日)=有芒作物成熟/夏播作物播种时节。"芒种忙忙栽"。',2,1),
-- 畜禽营养与饲料 8题
('畜禽营养与饲料[职高]','SINGLE_CHOICE','下列氨基酸中属于猪的必需氨基酸的是：', '["A. 丙氨酸","B. 谷氨酸","C. 赖氨酸","D. 甘氨酸"]', 'C', '赖氨酸是单胃动物(猪/禽)最常见的限制性氨基酸，体内不能合成必须由饲料供给。',1,1),
('畜禽营养与饲料[职高]','SINGLE_CHOICE','反刍动物瘤胃微生物可以利用的非蛋白氮(NPN)是：', '["A. 氨基酸","B. 尿素","C. 蛋白质","D. 多肽"]', 'B', '瘤胃微生物能将尿素(CO(NH2)2)等NPN水解为NH3，再利用NH3合成微生物蛋白。',2,1),
('畜禽营养与饲料[职高]','SINGLE_CHOICE','青贮饲料制作的关键条件是：', '["A. 高温通气","B. 厌氧环境+适宜含水量","C. 阳光直射","D. 添加抗生素"]', 'B', '青贮原理=乳酸菌在厌氧条件下发酵产生乳酸降低pH→抑制杂菌→保质。适宜含水量65%~75%。',2,1),
('畜禽营养与饲料[职高]','SINGLE_CHOICE','饲料总能→消化能→代谢能→净能的转化中，损失最大的是：', '["A. 粪能","B. 尿能+甲烷能","C. 热增耗","D. 消化能"]', 'A', '粪能损失占总能的20%~60%(因饲料种类而异)，是能量转化过程中损失最大的环节。',2,1),
('畜禽营养与饲料[职高]','SINGLE_CHOICE','畜禽缺钙的典型症状是：', '["A. 贫血","B. 佝偻病/骨软症","C. 甲状腺肿大","D. 白肌病"]', 'B', '钙是骨骼主要成分→缺钙幼畜佝偻病(骨变形)/成畜骨软症。贫血=缺铁/白肌病=缺硒/甲状腺肿=缺碘。',1,1),
('畜禽营养与饲料[职高]','SINGLE_CHOICE','国际饲料分类法将饲料分为几大类？', '["A. 5大类","B. 8大类","C. 10大类","D. 12大类"]', 'B', '国际饲料分类法(IFN):8大类=粗饲料/青绿饲料/青贮饲料/能量饲料/蛋白质饲料/矿物质饲料/维生素饲料/饲料添加剂。',1,1),
('畜禽营养与饲料[职高]','SINGLE_CHOICE','用方形法(对角线法)配制两种饲料原料的混合料时，正方形中心写：', '["A. 第一种原料的粗蛋白含量","B. 第二种原料的粗蛋白含量","C. 混合料的目标粗蛋白含量","D. 两种原料蛋白含量的平均值"]', 'C', '方形法:中心写目标值/四角写原料值→对角差→比例。适用于两种原料配制。',2,1),
('畜禽营养与饲料[职高]','SINGLE_CHOICE','下列属于脂溶性维生素的是：', '["A. 维生素B1","B. 维生素C","C. 维生素A","D. 维生素B12"]', 'C', '脂溶性=维生素A/D/E/K(可蓄积/过量中毒);水溶性=B族/C(不易蓄积/需持续供给)。',1,1),
-- 动物解剖生理 8题
('动物解剖生理[职高]','SINGLE_CHOICE','被称为"细胞的动力工厂"的细胞器是：', '["A. 内质网","B. 高尔基体","C. 线粒体","D. 核糖体"]', 'C', '线粒体进行有氧呼吸产生ATP(能量货币)→细胞动力工厂。核糖体=合成蛋白质。',1,1),
('动物解剖生理[职高]','SINGLE_CHOICE','小肠各段的正确顺序是：', '["A. 空肠→十二指肠→回肠","B. 十二指肠→空肠→回肠","C. 十二指肠→回肠→空肠","D. 回肠→空肠→十二指肠"]', 'B', '小肠三段依次为:十二指肠→空肠→回肠(长度依次增加/直径依次减小)。',1,1),
('动物解剖生理[职高]','SINGLE_CHOICE','反刍动物皱胃(真胃)的黏膜上皮类型是：', '["A. 复层扁平上皮","B. 单层柱状上皮","C. 变移上皮","D. 假复层纤毛柱状上皮"]', 'B', '皱胃是反刍动物唯一有腺体的胃/黏膜上皮=单层柱状上皮(分泌胃酸和酶)。前三胃(瘤/网/瓣)为复层扁平上皮。',2,1),
('动物解剖生理[职高]','SINGLE_CHOICE','骨的主要成分中，使骨具有硬度的成分是：', '["A. 有机质(骨胶原)","B. 无机盐(钙盐/磷酸盐)","C. 水分","D. 脂肪"]', 'B', '无机盐(主要是羟基磷灰石Ca10(PO4)6(OH)2)使骨坚硬; 有机质(骨胶原)使骨具有弹性和韧性。',1,1),
('动物解剖生理[职高]','SINGLE_CHOICE','气体交换的主要场所是：', '["A. 气管","B. 支气管","C. 细支气管","D. 肺泡"]', 'D', '肺泡壁极薄(单层扁平上皮)/外有丰富毛细血管网→O2和CO2在此进行气体交换。',1,1),
('动物解剖生理[职高]','SINGLE_CHOICE','下列组织中具有收缩功能的是：', '["A. 上皮组织","B. 结缔组织","C. 肌肉组织","D. 神经组织"]', 'C', '肌肉组织由肌细胞(肌纤维)组成→具有收缩和舒张功能。上皮=保护/结缔=支持连接/神经=传导兴奋。',1,1),
('动物解剖生理[职高]','SINGLE_CHOICE','牛的角属于哪种骨？', '["A. 长骨","B. 短骨","C. 扁骨","D. 额骨的一部分突起"]', 'D', '牛角是额骨的骨质突起(角突)，外面包裹角鞘(表皮衍生物)。属于骨性标志。',2,1),
('动物解剖生理[职高]','SINGLE_CHOICE','呼吸运动的原动力来自：', '["A. 肺的弹性回缩","B. 呼吸肌(肋间肌+膈肌)的舒缩","C. 胸腔内负压","D. 血中CO2浓度变化"]', 'B', '呼吸肌收缩→胸腔容积扩大→肺被动扩张→吸气; 呼吸肌舒张→胸腔缩小→肺弹性回缩→呼气。',2,1),
-- 农业经营与管理 8题
('农业经营与管理[职高]','SINGLE_CHOICE','农业生产最显著的特点是：', '["A. 经济再生产和自然再生产相交织","B. 高度机械化","C. 完全不受自然条件影响","D. 产品同质化"]', 'A', '农业生产=自然再生产(生物生长)+经济再生产(人类投入管理)交织，这是区别于工业生产的根本特点。',1,1),
('农业经营与管理[职高]','SINGLE_CHOICE','下列属于新型农业经营主体的是：', '["A. 个体小农户","B. 家庭农场","C. 流动商贩","D. 农产品消费者"]', 'B', '新型经营主体=专业大户/家庭农场/农民专业合作社/农业龙头企业。',1,1),
('农业经营与管理[职高]','SINGLE_CHOICE','农产品4P营销组合中的4P不包括：', '["A. 产品(Product)","B. 价格(Price)","C. 利润(Profit)","D. 渠道(Place)"]', 'C', '4P=产品(Product)/价格(Price)/渠道(Place)/促销(Promotion)。利润是营销结果而非策略要素。',1,1),
('农业经营与管理[职高]','SINGLE_CHOICE','"三品一标"中的三品不包括：', '["A. 无公害农产品","B. 绿色食品","C. 有机农产品","D. 进口农产品"]', 'D', '三品=无公害农产品/绿色食品/有机农产品; 一标=农产品地理标志。',2,1),
('农业经营与管理[职高]','SINGLE_CHOICE','农村土地"三权分置"是指哪三权？', '["A. 所有权/承包权/经营权","B. 所有权/使用权/处置权","C. 占有权/收益权/抵押权","D. 承包权/流转权/继承权"]', 'A', '三权分置=集体所有权+农户承包权+实际经营者经营权。放活经营权促进土地流转。',2,1),
('农业经营与管理[职高]','SINGLE_CHOICE','农业经济效益评价中，投入产出比的计算公式是：', '["A. 投入÷产出","B. 产出÷投入","C. (产出-投入)÷投入","D. 投入×产出"]', 'B', '投入产出比=总产出(产值/收入)÷总投入(成本)。比值>1表示盈利，<1表示亏损。',2,1),
('农业经营与管理[职高]','SINGLE_CHOICE','国家粮食直补政策的主要目的是：', '["A. 限制粮食生产","B. 提高农民种粮积极性保障粮食安全","C. 鼓励抛荒","D. 降低农产品价格"]', 'B', '粮食直补是国家将补贴直接发放给种粮农民→降低生产成本/提高种粮收入/保障国家粮食安全。',1,1),
('农业经营与管理[职高]','SINGLE_CHOICE','农产品质量追溯体系的核心功能是：', '["A. 降低生产成本","B. 实现"从田间到餐桌"全程可追溯","C. 减少劳动力投入","D. 增加化肥施用量"]', 'B', '追溯体系记录生产/加工/运输/销售全过程信息→问题产品可快速定位召回→保障消费者权益。',2,1);

SELECT 'v114: 农林牧渔类4科创建完成' AS result;
SELECT ds.subject_name AS '科目', (SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=ds.id) AS 节点, (SELECT COUNT(*) FROM question_bank WHERE subject=ds.subject_name) AS 题库 FROM dict_subject ds WHERE ds.id BETWEEN 36 AND 39;
