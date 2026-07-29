-- v118: 畜禽营养与饲料 第三版 邱以亮/伏桂华 4项目23任务
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES (NULL, 37, 1, '畜禽营养与饲料[职高]', 1, 'ACTIVE');
SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=37 AND level=1 LIMIT 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r,37,2,'项目1 畜禽营养基础',1,'ACTIVE'),(@r,37,2,'项目2 营养物质的利用',2,'ACTIVE'),(@r,37,2,'项目3 饲料的加工与利用',3,'ACTIVE'),(@r,37,2,'项目4 营养需要与配合饲料配制',4,'ACTIVE');
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,37,3,'任务1.1 动物与植物的组成成分',1,'ACTIVE'),(@p,37,3,'任务1.2 畜禽对饲料的消化',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%1.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,37,4,'动植物体化学元素组成比较(均以C/H/O/N为主) [掌握]',1,'ACTIVE'),(@t,37,4,'动植物体内化合物的差异(植物体碳水化合物多/动物体蛋白质脂肪多) [掌握]',2,'ACTIVE'),(@t,37,4,'六大营养素概述(水/蛋白/脂肪/碳水/矿物/维生素) [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%1.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,37,4,'消化和吸收的概念与区别 [掌握]',1,'ACTIVE'),(@t,37,4,'畜禽三种消化方式:物理消化/化学消化/微生物消化 [掌握]',2,'ACTIVE'),(@t,37,4,'消化力/饲料可消化性概念与消化率计算 [掌握]',3,'ACTIVE');

SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,37,3,'任务2.1 蛋白质的营养作用及其含量测定',1,'ACTIVE'),(@p,37,3,'任务2.2 糖类的营养作用及其含量测定',2,'ACTIVE'),(@p,37,3,'任务2.3 脂肪的营养作用及其含量测定',3,'ACTIVE'),(@p,37,3,'任务2.4 矿物质的营养作用及其含量测定',4,'ACTIVE'),(@p,37,3,'任务2.5 维生素的营养作用及其缺乏症',5,'ACTIVE'),(@p,37,3,'任务2.6 水的营养作用及其含量测定',6,'ACTIVE'),(@p,37,3,'任务2.7 畜禽对能量的利用',7,'ACTIVE'),(@p,37,3,'任务2.8 机体中营养物质的相互关系',8,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,37,4,'必需/非必需/限制性氨基酸概念与理想蛋白质 [掌握]',1,'ACTIVE'),(@t,37,4,'单胃动物蛋白质消化特点与反刍动物NPN利用机理(尿素→NH3→微生物蛋白) [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'糖类的组成(单糖/双糖/多糖)与营养作用/粗纤维利用/无氮浸出物(NFE)概念 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'脂肪营养作用与必需脂肪酸(亚油酸/亚麻酸)/饲料脂肪对畜产品品质影响 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,37,4,'常量元素Ca/P/Na/Cl/Mg/S/K与微量元素Fe/Cu/Zn/Mn/Se/I/Co功能及缺乏症 [掌握]',1,'ACTIVE'),(@t,37,4,'钙磷比例(1:1~2:1)与维生素D的调节关系 [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.5%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'脂溶性维生素(A/D/E/K)与水溶性(B族/C)来源/功能/缺乏症 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.6%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'水的营养作用/体内水运行及需水量 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.7%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,37,4,'能量转化:总能(GE)→消化能(DE)→代谢能(ME)→净能(NE) [掌握]',1,'ACTIVE'),(@t,37,4,'维持净能与生产净能的概念/日粮能量水平对畜禽生产性能影响 [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.8%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'三大有机物相互转化/能氮比/矿物质维生素氨基酸间协同与拮抗关系 [理解]',1,'ACTIVE');

SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,37,3,'任务3.1 饲料的分类及常用饲料的识别',1,'ACTIVE'),(@p,37,3,'任务3.2 粗饲料及其加工处理',2,'ACTIVE'),(@p,37,3,'任务3.3 青绿饲料的识别与利用',3,'ACTIVE'),(@p,37,3,'任务3.4 青贮饲料的调制与利用',4,'ACTIVE'),(@p,37,3,'任务3.5 能量饲料的识别与利用',5,'ACTIVE'),(@p,37,3,'任务3.6 蛋白质饲料的识别与利用',6,'ACTIVE'),(@p,37,3,'任务3.7 矿物质饲料的利用',7,'ACTIVE'),(@p,37,3,'任务3.8 饲料添加剂的利用',8,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'国际饲料分类8大类与中国饲料分类16亚类 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'粗饲料种类与秸秆氨化(尿素3%-5%密封2-4周)/碱化(NaOH)处理技术 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'青绿饲料营养特性与亚硝酸盐/氢氰酸中毒预防 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'青贮原理(厌氧乳酸发酵/pH降低抑制杂菌)与制作(含水量65%-75%/切碎压实密封) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.5%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'谷实类(玉米/小麦)/糠麸类(麦麸/米糠)/块根块茎类能量饲料营养特性 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.6%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'植物性蛋白(豆粕/棉粕/菜粕)营养特性与抗营养因子/鱼粉掺假检验 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.7%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'食盐/石粉/磷酸氢钙/贝壳粉的特性与用法/矿物质盐砖工艺 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.8%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'营养性饲料添加剂(氨基酸/维生素/微量元素)种类与使用规范 [掌握]',1,'ACTIVE');

SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,37,3,'任务4.1 畜禽的营养需要',1,'ACTIVE'),(@p,37,3,'任务4.2 饲养标准与全价配合饲料的配制',2,'ACTIVE'),(@p,37,3,'任务4.3 浓缩饲料的配制',3,'ACTIVE'),(@p,37,3,'任务4.4 预混料的配制',4,'ACTIVE'),(@p,37,3,'任务4.5 饲养试验的设计与结果分析',5,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'维持需要与生产需要(生长/育肥/繁殖/泌乳/产蛋/产毛)概念 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,37,4,'饲养标准(NRC/中国)概念与使用/配合饲料分类(全价/浓缩/预混/精补) [掌握]',1,'ACTIVE'),(@t,37,4,'配方设计方法:方形法(对角线法)与试差法(逐步逼近法) [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'浓缩饲料概念/组成(蛋白质+矿物质+维生素+添加剂占20%-40%)/配制方法 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'微量元素/维生素/复合预混料配制要点与载体选择原则 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.5%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,37,4,'饲养试验设计原则(随机分组/对照组/重复)与结果分析方法 [了解]',1,'ACTIVE');

INSERT INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at) VALUES
(37,'DUIKOU','BOTH','畜禽营养与饲料考纲(2023版)','# 畜禽营养与饲料 考试大纲 教材:邱以亮/伏桂华第三版 ISBN 978-7-04-056185-2 分值约50分(25%)\n4项目23任务:项目1畜禽营养基础(化学组成/消化吸收/消化率)/项目2营养物质利用(蛋白质NPN/碳水化合物粗纤维/脂肪/矿物质CaP微量元素/维生素脂溶水溶/能量GEDEMENE转化/营养物质相互关系)/项目3饲料加工利用(8大类分类/粗饲料氨化碱化/青绿饲料防中毒/青贮原理制作/能量蛋白矿物质饲料/添加剂)/项目4营养需要与配合饲料(维持生产需要/饲养标准/全价浓缩预混料/方形法试差法/饲养试验)','2023',1,NOW(),NOW());
INSERT INTO exam_syllabus_node_relation (syllabus_id, node_id) SELECT es.id, kn.id FROM exam_syllabus es JOIN knowledge_nodes kn ON es.subject_id=kn.subject_id AND kn.level=1 WHERE es.subject_id=37;
SELECT CONCAT('畜禽营养与饲料: ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=37 AND level=2),'项目 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=37 AND level=3),'任务 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=37 AND level=4),'知识点') AS result;
