-- v117: 农林牧渔4科完全按教材目录重建
-- 植物生产与环境(第四版)宋志伟 7项目20任务
SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=36 AND level=1 LIMIT 1);

-- L2: 7项目
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r,36,2,'项目1 植物生产与环境概述',1,'ACTIVE'),(@r,36,2,'项目2 植物的生长发育',2,'ACTIVE'),(@r,36,2,'项目3 植物生产与土壤培肥',3,'ACTIVE'),(@r,36,2,'项目4 植物生产与科学用水',4,'ACTIVE'),(@r,36,2,'项目5 植物生产与光能利用',5,'ACTIVE'),(@r,36,2,'项目6 植物生产与温度调控',6,'ACTIVE'),(@r,36,2,'项目7 植物生产与农业气象',7,'ACTIVE');

-- P1: 任务1.1 1.2
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@p,36,3,'任务1.1 植物生长与植物生产',1,'ACTIVE'),(@p,36,3,'任务1.2 植物生产的两大要素',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%1.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'植物的生长(营养生长/生殖生长)概念 [掌握]',1,'ACTIVE'),(@t,36,4,'植物生长的周期性(大周期/昼夜周期/季节周期) [掌握]',2,'ACTIVE'),(@t,36,4,'植物生长的相关性(根冠比/顶端优势) [掌握]',3,'ACTIVE'),(@t,36,4,'植物的极性与再生 [了解]',4,'ACTIVE'),(@t,36,4,'植物的休眠与衰老 [了解]',5,'ACTIVE'),(@t,36,4,'成花过程:春化作用/光周期现象/花芽分化 [掌握]',6,'ACTIVE'),(@t,36,4,'植物生产的概念与特点 [了解]',7,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%1.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'自然要素(温/光/水/气/土)对植物生产的影响 [掌握]',1,'ACTIVE'),(@t,36,4,'农业生产要素(品种/肥料/灌溉/植保/农机) [了解]',2,'ACTIVE');

-- P2: 5任务(2.1~2.5)
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,36,3,'任务2.1 植物的细胞',1,'ACTIVE'),(@p,36,3,'任务2.2 植物的组织',2,'ACTIVE'),(@p,36,3,'任务2.3 植物的营养器官',3,'ACTIVE'),(@p,36,3,'任务2.4 植物的生殖器官',4,'ACTIVE'),(@p,36,3,'任务2.5 植物的生长物质',5,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'细胞壁/细胞膜/细胞质/细胞核的结构与功能 [掌握]',1,'ACTIVE'),(@t,36,4,'叶绿体与线粒体(双层膜/含DNA)的功能 [掌握]',2,'ACTIVE'),(@t,36,4,'内质网/高尔基体/核糖体/液泡的功能 [理解]',3,'ACTIVE'),(@t,36,4,'细胞繁殖:有丝分裂/减数分裂/无丝分裂 [掌握]',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'分生组织(顶端/侧生/居间)的类型与功能 [掌握]',1,'ACTIVE'),(@t,36,4,'成熟组织:保护/薄壁/机械/输导/分泌组织 [掌握]',2,'ACTIVE'),(@t,36,4,'组织系统(皮系统/维管系统/基本系统) [了解]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'根的类型(直根系/须根系)与根尖分区(根冠/分生区/伸长区/根毛区) [掌握]',1,'ACTIVE'),(@t,36,4,'茎的形态与分枝方式(单轴/合轴/假二叉) [掌握]',2,'ACTIVE'),(@t,36,4,'叶的组成(叶片/叶柄/托叶)与气孔开闭调节 [掌握]',3,'ACTIVE'),(@t,36,4,'营养器官变态:根变态/茎变态(块茎/根茎/鳞茎)/叶变态 [掌握]',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'花的组成(花萼/花冠/雄蕊群/雌蕊群)与花序类型 [掌握]',1,'ACTIVE'),(@t,36,4,'种子的结构(双子叶/单子叶)与类型 [掌握]',2,'ACTIVE'),(@t,36,4,'果实的发育与类型(真果/假果/单果/聚合果/聚花果) [掌握]',3,'ACTIVE'),(@t,36,4,'种子萌发的条件与发芽率/发芽势计算 [掌握]',4,'ACTIVE'),(@t,36,4,'传粉方式与双受精过程及生物学意义 [理解]',5,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.5%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'五大植物激素(IAA/GA/CTK/ABA/ETH)的合成部位与主要生理作用 [掌握]',1,'ACTIVE'),(@t,36,4,'植物生长物质的概念与分类(激素/生长调节剂) [理解]',2,'ACTIVE'),(@t,36,4,'植物生长调节剂在农业中的应用(促根/防落果/催熟/矮化) [掌握]',3,'ACTIVE');

-- P3: 4任务(3.1~3.4)
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,36,3,'任务3.1 土壤的基本组成',1,'ACTIVE'),(@p,36,3,'任务3.2 土壤的基本性质',2,'ACTIVE'),(@p,36,3,'任务3.3 植物营养与科学施肥',3,'ACTIVE'),(@p,36,3,'任务3.4 作物减肥增效技术',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'土壤三相(固/液/气)组成及理想比例(约50:25:25) [掌握]',1,'ACTIVE'),(@t,36,4,'土壤矿物质(原生/次生矿物)与有机质来源 [掌握]',2,'ACTIVE'),(@t,36,4,'土壤生物的组成与作用 [了解]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'土壤质地分类(砂土/壤土/黏土)与农业生产特性 [掌握]',1,'ACTIVE'),(@t,36,4,'土壤结构(团粒结构)与孔隙性/耕性 [掌握]',2,'ACTIVE'),(@t,36,4,'土壤酸碱性与缓冲性/pH对养分有效性的影响 [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'植物必需营养元素(16种:大量9/微量7)与缺素症识别 [掌握]',1,'ACTIVE'),(@t,36,4,'化肥种类:氮肥(尿素/碳铵)/磷肥(过磷酸钙)/钾肥(氯化钾/硫酸钾) [掌握]',2,'ACTIVE'),(@t,36,4,'有机肥种类(粪尿肥/堆沤肥/绿肥/饼肥)与特点 [掌握]',3,'ACTIVE'),(@t,36,4,'微生物肥料的概念与使用注意事项 [了解]',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'测土配方施肥的基本程序(取土→化验→配方→施肥) [掌握]',1,'ACTIVE'),(@t,36,4,'水肥一体化技术的概念与优点 [了解]',2,'ACTIVE'),(@t,36,4,'有机肥替代化肥的意义与技术路径 [了解]',3,'ACTIVE');

-- P4: 2任务(4.1~4.2)
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,36,3,'任务4.1 植物生产的水分条件',1,'ACTIVE'),(@p,36,3,'任务4.2 植物生产的水分调控',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'大气水分(绝对湿度/相对湿度/饱和差)的表示方法 [掌握]',1,'ACTIVE'),(@t,36,4,'降水(降水量/降水强度/降水变率/降水保证率)的概念 [掌握]',2,'ACTIVE'),(@t,36,4,'土壤水分蒸发的过程与影响因素 [了解]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'植物需水规律与需水临界期的概念 [掌握]',1,'ACTIVE'),(@t,36,4,'蒸腾作用的概念/部位(气孔为主)/生理意义(拉力/降温/促矿质运输) [掌握]',2,'ACTIVE'),(@t,36,4,'农田水分调控技术(合理灌溉/排水防涝/保墒) [掌握]',3,'ACTIVE');

-- P5: 3任务(5.1~5.3)
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目5%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,36,3,'任务5.1 植物的新陈代谢',1,'ACTIVE'),(@p,36,3,'任务5.2 植物生产的光照条件',2,'ACTIVE'),(@p,36,3,'任务5.3 植物生产的光环境调控',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%5.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'光合作用过程(光反应/暗反应)与总方程式 6CO2+12H2O→C6H12O6+6O2+6H2O [掌握]',1,'ACTIVE'),(@t,36,4,'有氧呼吸与无氧呼吸(酒精发酵/乳酸发酵)的过程与ATP产量 [掌握]',2,'ACTIVE'),(@t,36,4,'光合作用与呼吸作用的关系(原料/产物/能量/场所对比) [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%5.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'四季与昼夜变化对植物生长发育的影响 [了解]',1,'ACTIVE'),(@t,36,4,'光照强度/日照长度(长日/短日/日中性植物)对植物影响 [掌握]',2,'ACTIVE'),(@t,36,4,'光与植物形态建成(光形态建成/黄化现象) [理解]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%5.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'光合性能(光合速率/光合生产率/叶面积系数)概念 [掌握]',1,'ACTIVE'),(@t,36,4,'植物光适应类型:C3植物/C4植物/CAM植物比较 [掌握]',2,'ACTIVE'),(@t,36,4,'光能利用率的计算公式与提高途径 [掌握]',3,'ACTIVE'),(@t,36,4,'设施栽培光环境调控(补光/遮光/光质选择) [了解]',4,'ACTIVE');

-- P6: 2任务(6.1~6.2)
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目6%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,36,3,'任务6.1 植物生产的温度条件',1,'ACTIVE'),(@p,36,3,'任务6.2 植物生产的温度调控',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%6.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'土壤热性质(热容量/导热率/热扩散率)的概念 [掌握]',1,'ACTIVE'),(@t,36,4,'土壤温度的日变化和年变化规律 [掌握]',2,'ACTIVE'),(@t,36,4,'空气温度变化(日/年/垂直变化)与逆温现象 [掌握]',3,'ACTIVE'),(@t,36,4,'温度指标:三基点温度/农业界限温度(0/5/10/15/20℃)/积温(活动/有效) [掌握]',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%6.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'温度对种子萌发/营养生长/生殖生长的影响 [掌握]',1,'ACTIVE'),(@t,36,4,'温周期现象的概念与农业生产意义 [掌握]',2,'ACTIVE'),(@t,36,4,'温度调控技术:地膜覆盖/设施增温/通风降温/灌水调温/熏烟防霜 [掌握]',3,'ACTIVE');

-- P7: 2任务(7.1~7.2)
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目7%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,36,3,'任务7.1 植物生产的气象条件',1,'ACTIVE'),(@p,36,3,'任务7.2 气候与农业小气候',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%7.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'农业气象要素(太阳辐射/温度/水分/气压/风)概述 [了解]',1,'ACTIVE'),(@t,36,4,'干旱(大气/土壤/生理干旱)的成因与防御措施 [掌握]',2,'ACTIVE'),(@t,36,4,'洪涝/霜冻/冷害/冻害/热害/风灾/雹灾的防御 [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%7.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,36,4,'天气系统(气团/锋面/气旋/反气旋)的基本概念 [了解]',1,'ACTIVE'),(@t,36,4,'气候形成因素与小气候概念(农田小气候/设施小气候) [了解]',2,'ACTIVE'),(@t,36,4,'二十四节气的名称/顺序/太阳黄经/对应主要农事活动 [掌握]',3,'ACTIVE');

-- 考纲
INSERT INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at) VALUES
(36,'DUIKOU','BOTH','植物生产与环境考纲(2023版)','# 植物生产与环境 考试大纲\n\n教材:宋志伟第四版 ISBN 978-7-04-057936-9\n分值:约70分(35%)\n\n## 项目1 植物生产与环境概述\n植物生长概念/周期性/相关性/极性与再生/休眠与衰老/春化作用/光周期现象/花芽分化/自然要素/生产要素\n\n## 项目2 植物的生长发育\n细胞结构/有丝减数分裂/分生组织成熟组织/根茎叶变态/花种子果实/五大激素与生长调节剂应用\n\n## 项目3 植物生产与土壤培肥\n土壤三相/质地/结构/酸碱缓冲性/16元素缺素症/化肥有机肥/测土配方/水肥一体化\n\n## 项目4 植物生产与科学用水\n空气湿度/降水/蒸腾作用/需水规律/灌溉排水保墒\n\n## 项目5 植物生产与光能利用\n光合呼吸作用/光照条件/光能利用率/C3C4CAM植物/光环境调控\n\n## 项目6 植物生产与温度调控\n土壤热性质/气温变化/三基点/界限温度/积温/地膜设施调温\n\n## 项目7 植物生产与农业气象\n气象灾害防御/天气系统/二十四节气\n\n应会技能:显微镜/种子生活力/千粒重/切接/病虫害识别(占技能150分对应部分)','2023',1,NOW(),NOW());
INSERT INTO exam_syllabus_node_relation (syllabus_id, node_id) SELECT es.id, @r FROM exam_syllabus es WHERE es.subject_id=36;
SELECT CONCAT('植物生产与环境: ',COUNT(CASE WHEN level=2 THEN 1 END),'项目 ',COUNT(CASE WHEN level=3 THEN 1 END),'任务 ',COUNT(CASE WHEN level=4 THEN 1 END),'知识点') FROM knowledge_nodes WHERE subject_id=36 AND status='ACTIVE';
