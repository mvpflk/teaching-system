-- ============================================================
-- v104: 农学类[职高] 4科分离 — 对标四川省对口升学农林牧渔类
-- 参照计算机4科模式: 信息技术/网络/办公/Access
-- 农学4科: 植物生产与环境/农作物生产技术/植物保护技术/农业生物技术
-- 推荐教材: 高教社中职国家规划教材
-- ============================================================

-- ═══ 1. 注册4个学科 ═══
INSERT IGNORE INTO dict_subject (subject_name, status) VALUES
('植物生产与环境[职高]', 1),
('农作物生产技术[职高]', 1),
('植物保护技术[职高]', 1),
('农业生物技术[职高]', 1);

SET @s1=(SELECT id FROM dict_subject WHERE subject_name='植物生产与环境[职高]' LIMIT 1);
SET @s2=(SELECT id FROM dict_subject WHERE subject_name='农作物生产技术[职高]' LIMIT 1);
SET @s3=(SELECT id FROM dict_subject WHERE subject_name='植物保护技术[职高]' LIMIT 1);
SET @s4=(SELECT id FROM dict_subject WHERE subject_name='农业生物技术[职高]' LIMIT 1);

-- ═══ 2. 创建4个根节点(Level 1) ═══
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(NULL, @s1, 1, '植物生产与环境[职高]', 1, 'ACTIVE'),
(NULL, @s2, 1, '农作物生产技术[职高]', 1, 'ACTIVE'),
(NULL, @s3, 1, '植物保护技术[职高]', 1, 'ACTIVE'),
(NULL, @s4, 1, '农业生物技术[职高]', 1, 'ACTIVE');

SET @r1=(SELECT id FROM knowledge_nodes WHERE subject_id=@s1 AND level=1 LIMIT 1);
SET @r2=(SELECT id FROM knowledge_nodes WHERE subject_id=@s2 AND level=1 LIMIT 1);
SET @r3=(SELECT id FROM knowledge_nodes WHERE subject_id=@s3 AND level=1 LIMIT 1);
SET @r4=(SELECT id FROM knowledge_nodes WHERE subject_id=@s4 AND level=1 LIMIT 1);

-- ════════════════════════════════════════════════════════════
-- 科目1: 植物生产与环境 (6章×17单元×60知识点)
-- 教材: 《植物生产与环境》(第四版) 高教社 ISBN 978-7-04-058057-0
-- ════════════════════════════════════════════════════════════

-- Level 2: 章节
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r1, @s1, 2, '植物细胞与组织', 1, 'ACTIVE'),
(@r1, @s1, 2, '植物的营养器官', 2, 'ACTIVE'),
(@r1, @s1, 2, '植物的生殖器官', 3, 'ACTIVE'),
(@r1, @s1, 2, '植物生理代谢',   4, 'ACTIVE'),
(@r1, @s1, 2, '植物生长发育',   5, 'ACTIVE'),
(@r1, @s1, 2, '土壤与肥料',     6, 'ACTIVE');

-- 1.1 植物细胞与组织
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物细胞与组织');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '植物细胞的结构与功能', 1, 'ACTIVE'),
(@ch, @s1, 3, '细胞分裂', 2, 'ACTIVE'),
(@ch, @s1, 3, '植物组织类型', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物细胞的结构与功能');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='细胞分裂');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物组织类型');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '细胞壁/细胞膜/细胞质/细胞核 [识记]', 1, 'ACTIVE'),
(@u1, @s1, 4, '叶绿体与线粒体的功能 [理解]', 2, 'ACTIVE'),
(@u1, @s1, 4, '内质网/高尔基体/核糖体 [识记]', 3, 'ACTIVE'),
(@u2, @s1, 4, '有丝分裂的过程与意义 [理解]', 1, 'ACTIVE'),
(@u2, @s1, 4, '减数分裂与配子形成 [理解]', 2, 'ACTIVE'),
(@u3, @s1, 4, '分生组织与成熟组织的类型 [识记]', 1, 'ACTIVE'),
(@u3, @s1, 4, '维管束的组成与功能 [理解]', 2, 'ACTIVE');

-- 1.2 植物的营养器官
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物的营养器官');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '根的形态与功能', 1, 'ACTIVE'),
(@ch, @s1, 3, '茎的形态与功能', 2, 'ACTIVE'),
(@ch, @s1, 3, '叶的形态与功能', 3, 'ACTIVE'),
(@ch, @s1, 3, '营养器官的变态', 4, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='根的形态与功能');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='茎的形态与功能');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='叶的形态与功能');
SET @u4=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='营养器官的变态');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '直根系与须根系 [识记]', 1, 'ACTIVE'),
(@u1, @s1, 4, '根尖分区(根冠/分生区/伸长区/根毛区) [理解]', 2, 'ACTIVE'),
(@u1, @s1, 4, '根的吸收作用 [掌握]', 3, 'ACTIVE'),
(@u2, @s1, 4, '茎的形态与分枝方式 [识记]', 1, 'ACTIVE'),
(@u2, @s1, 4, '茎的初生结构与维管束 [理解]', 2, 'ACTIVE'),
(@u3, @s1, 4, '叶的形态与叶脉类型 [识记]', 1, 'ACTIVE'),
(@u3, @s1, 4, '叶绿体与光合色素 [理解]', 2, 'ACTIVE'),
(@u3, @s1, 4, '气孔的结构与功能 [理解]', 3, 'ACTIVE'),
(@u4, @s1, 4, '根的变态(贮藏根/气生根) [识记]', 1, 'ACTIVE'),
(@u4, @s1, 4, '茎的变态(块茎/根茎/鳞茎) [识记]', 2, 'ACTIVE');

-- 1.3 植物的生殖器官
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物的生殖器官');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '花的形态与发育', 1, 'ACTIVE'),
(@ch, @s1, 3, '种子的结构与萌发', 2, 'ACTIVE'),
(@ch, @s1, 3, '果实的发育与类型', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='花的形态与发育');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='种子的结构与萌发');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='果实的发育与类型');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '花的基本组成(花萼/花冠/雄蕊群/雌蕊群) [识记]', 1, 'ACTIVE'),
(@u1, @s1, 4, '传粉与受精过程 [理解]', 2, 'ACTIVE'),
(@u2, @s1, 4, '双子叶与单子叶种子的结构 [理解]', 1, 'ACTIVE'),
(@u2, @s1, 4, '种子萌发的条件 [掌握]', 2, 'ACTIVE'),
(@u2, @s1, 4, '发芽率与发芽势的计算 [掌握]', 3, 'ACTIVE'),
(@u3, @s1, 4, '果实的类型与发育 [识记]', 1, 'ACTIVE');

-- 1.4 植物生理代谢
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生理代谢');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '水分代谢', 1, 'ACTIVE'),
(@ch, @s1, 3, '矿质营养', 2, 'ACTIVE'),
(@ch, @s1, 3, '光合作用', 3, 'ACTIVE'),
(@ch, @s1, 3, '呼吸作用', 4, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水分代谢');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='矿质营养');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='光合作用');
SET @u4=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='呼吸作用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '水分吸收与运输途径 [理解]', 1, 'ACTIVE'),
(@u1, @s1, 4, '蒸腾作用的概念与调节 [掌握]', 2, 'ACTIVE'),
(@u2, @s1, 4, '16种必需元素与缺素症识别 [掌握]', 1, 'ACTIVE'),
(@u2, @s1, 4, '氮磷钾的生理作用与缺乏症状 [掌握]', 2, 'ACTIVE'),
(@u3, @s1, 4, '光反应与暗反应的过程 [理解]', 1, 'ACTIVE'),
(@u3, @s1, 4, '光合作用的影响因素与生产应用 [掌握]', 2, 'ACTIVE'),
(@u4, @s1, 4, '有氧呼吸与无氧呼吸比较 [理解]', 1, 'ACTIVE'),
(@u4, @s1, 4, '呼吸作用在农产品贮藏中的应用 [掌握]', 2, 'ACTIVE');

-- 1.5 植物生长发育
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生长发育');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '植物激素与调节剂', 1, 'ACTIVE'),
(@ch, @s1, 3, '植物生长的周期性', 2, 'ACTIVE'),
(@ch, @s1, 3, '成花生理', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物激素与调节剂');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物生长的周期性');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='成花生理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '五大激素(生长素/赤霉素/细胞分裂素/脱落酸/乙烯) [掌握]', 1, 'ACTIVE'),
(@u1, @s1, 4, '植物生长调节剂的应用 [掌握]', 2, 'ACTIVE'),
(@u2, @s1, 4, '昼夜周期与季节周期 [识记]', 1, 'ACTIVE'),
(@u2, @s1, 4, '顶端优势与根冠比 [理解]', 2, 'ACTIVE'),
(@u3, @s1, 4, '春化作用的概念与应用 [理解]', 1, 'ACTIVE'),
(@u3, @s1, 4, '光周期现象(长日/短日/日中性植物) [掌握]', 2, 'ACTIVE');

-- 1.6 土壤与肥料
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='土壤与肥料');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s1, 3, '土壤基本性质', 1, 'ACTIVE'),
(@ch, @s1, 3, '土壤肥力与管理', 2, 'ACTIVE'),
(@ch, @s1, 3, '肥料种类与施用', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='土壤基本性质');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='土壤肥力与管理');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='肥料种类与施用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s1, 4, '土壤质地(砂土/壤土/黏土) [掌握]', 1, 'ACTIVE'),
(@u1, @s1, 4, '土壤有机质与pH值 [理解]', 2, 'ACTIVE'),
(@u2, @s1, 4, '土壤肥力四因素(水/肥/气/热) [理解]', 1, 'ACTIVE'),
(@u2, @s1, 4, '土壤耕性与宜耕期 [掌握]', 2, 'ACTIVE'),
(@u3, @s1, 4, '化肥种类(氮/磷/钾/复合肥) [掌握]', 1, 'ACTIVE'),
(@u3, @s1, 4, '有机肥(粪尿肥/堆肥/绿肥) [理解]', 2, 'ACTIVE'),
(@u3, @s1, 4, '施肥方法与测土配方 [掌握]', 3, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目2: 农作物生产技术 (3章×9单元×25知识点)
-- 教材: 《农作物生产技术》(第三版) 高教社 ISBN 978-7-04-052847-2
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r2, @s2, 2, '主要粮食作物', 1, 'ACTIVE'),
(@r2, @s2, 2, '主要经济作物', 2, 'ACTIVE'),
(@r2, @s2, 2, '耕作制度',     3, 'ACTIVE');

-- 2.1 主要粮食作物
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='主要粮食作物');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '水稻栽培技术', 1, 'ACTIVE'),
(@ch, @s2, 3, '小麦栽培技术', 2, 'ACTIVE'),
(@ch, @s2, 3, '玉米栽培技术', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水稻栽培技术');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='小麦栽培技术');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='玉米栽培技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s2, 4, '水稻生育期划分与器官建成 [理解]', 1, 'ACTIVE'),
(@u1, @s2, 4, '水稻育秧技术与合理密植 [掌握]', 2, 'ACTIVE'),
(@u1, @s2, 4, '水稻水肥管理与晒田技术 [掌握]', 3, 'ACTIVE'),
(@u2, @s2, 4, '小麦播种期与播种量确定 [掌握]', 1, 'ACTIVE'),
(@u2, @s2, 4, '小麦冬前管理与春季追肥 [掌握]', 2, 'ACTIVE'),
(@u2, @s2, 4, '小麦产量构成因素(穗数/粒数/粒重) [理解]', 3, 'ACTIVE'),
(@u3, @s2, 4, '玉米需肥规律与穗期管理 [掌握]', 1, 'ACTIVE'),
(@u3, @s2, 4, '玉米合理密植与种植方式 [掌握]', 2, 'ACTIVE');

-- 2.2 主要经济作物
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='主要经济作物');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '油菜栽培技术', 1, 'ACTIVE'),
(@ch, @s2, 3, '棉花栽培技术', 2, 'ACTIVE'),
(@ch, @s2, 3, '甘薯与马铃薯栽培', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='油菜栽培技术');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='棉花栽培技术');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='甘薯与马铃薯栽培');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s2, 4, '油菜育苗移栽与施肥技术 [掌握]', 1, 'ACTIVE'),
(@u1, @s2, 4, '油菜硼肥施用与菌核病防治 [掌握]', 2, 'ACTIVE'),
(@u2, @s2, 4, '棉花整枝打杈技术 [掌握]', 1, 'ACTIVE'),
(@u2, @s2, 4, '棉花蕾铃脱落原因与防止 [理解]', 2, 'ACTIVE'),
(@u3, @s2, 4, '甘薯育苗与栽插技术 [掌握]', 1, 'ACTIVE'),
(@u3, @s2, 4, '马铃薯切块催芽与培土 [掌握]', 2, 'ACTIVE');

-- 2.3 耕作制度
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='耕作制度');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s2, 3, '种植制度', 1, 'ACTIVE'),
(@ch, @s2, 3, '养地制度', 2, 'ACTIVE'),
(@ch, @s2, 3, '农田培肥', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='种植制度');
SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='养地制度');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农田培肥');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s2, 4, '轮作/连作的利弊与茬口安排 [掌握]', 1, 'ACTIVE'),
(@u1, @s2, 4, '间作/套作/混作的区别与模式 [掌握]', 2, 'ACTIVE'),
(@u1, @s2, 4, '复种指数的概念与计算 [掌握]', 3, 'ACTIVE'),
(@u2, @s2, 4, '土壤耕作措施(耕/耙/耱/压) [掌握]', 1, 'ACTIVE'),
(@u3, @s2, 4, '有机肥与绿肥的培肥作用 [理解]', 1, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目3: 植物保护技术 (3章×9单元×26知识点)
-- 教材: 《植物保护技术》(第三版) 高教社 ISBN 978-7-04-054359-9
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r3, @s3, 2, '植物病害识别与防治', 1, 'ACTIVE'),
(@r3, @s3, 2, '植物虫害识别与防治', 2, 'ACTIVE'),
(@r3, @s3, 2, '农药使用与综合防治', 3, 'ACTIVE');

-- 3.1 植物病害识别与防治
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='植物病害识别与防治');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '病害类型与症状', 1, 'ACTIVE'),
(@ch, @s3, 3, '主要作物病害', 2, 'ACTIVE'),
(@ch, @s3, 3, '病害发生规律', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='病害类型与症状'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='主要作物病害');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='病害发生规律');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s3, 4, '侵染性病害与非侵染性病害的区别 [理解]', 1, 'ACTIVE'),
(@u1, @s3, 4, '真菌病害症状(霉层/粉状物/锈斑/黑点) [掌握]', 2, 'ACTIVE'),
(@u1, @s3, 4, '细菌病害与病毒病害的识别 [掌握]', 3, 'ACTIVE'),
(@u2, @s3, 4, '水稻纹枯病/稻瘟病的识别与防治 [掌握]', 1, 'ACTIVE'),
(@u2, @s3, 4, '小麦锈病/赤霉病的识别与防治 [掌握]', 2, 'ACTIVE'),
(@u2, @s3, 4, '油菜菌核病/棉花枯萎病的识别 [理解]', 3, 'ACTIVE'),
(@u3, @s3, 4, '病害侵染循环与病害三角 [理解]', 1, 'ACTIVE'),
(@u3, @s3, 4, '病害流行条件与预测预报 [理解]', 2, 'ACTIVE');

-- 3.2 植物虫害识别与防治
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='植物虫害识别与防治');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '害虫口器类型与为害', 1, 'ACTIVE'),
(@ch, @s3, 3, '主要作物害虫', 2, 'ACTIVE'),
(@ch, @s3, 3, '害虫发生规律', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='害虫口器类型与为害'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='主要作物害虫');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='害虫发生规律');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s3, 4, '咀嚼式口器害虫(鳞翅目/鞘翅目/直翅目) [识记]', 1, 'ACTIVE'),
(@u1, @s3, 4, '刺吸式口器害虫(蚜虫/叶蝉/飞虱/蝽象) [识记]', 2, 'ACTIVE'),
(@u2, @s3, 4, '水稻螟虫(二化螟/三化螟/稻纵卷叶螟) [掌握]', 1, 'ACTIVE'),
(@u2, @s3, 4, '小麦蚜虫与粘虫的防治 [掌握]', 2, 'ACTIVE'),
(@u2, @s3, 4, '玉米螟的为害特点与防治 [掌握]', 3, 'ACTIVE'),
(@u3, @s3, 4, '害虫生活史(世代/越冬/迁飞) [理解]', 1, 'ACTIVE'),
(@u3, @s3, 4, '害虫发生与环境条件的关系 [理解]', 2, 'ACTIVE');

-- 3.3 农药使用与综合防治
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='农药使用与综合防治');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s3, 3, '农药基本知识', 1, 'ACTIVE'),
(@ch, @s3, 3, '农药安全使用', 2, 'ACTIVE'),
(@ch, @s3, 3, '综合防治策略', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农药基本知识'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农药安全使用');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='综合防治策略');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s3, 4, '农药分类(杀虫剂/杀菌剂/除草剂/杀鼠剂) [掌握]', 1, 'ACTIVE'),
(@u1, @s3, 4, '农药剂型(乳油/可湿性粉剂/悬浮剂/颗粒剂) [识记]', 2, 'ACTIVE'),
(@u2, @s3, 4, '农药稀释倍数与用药量计算 [掌握]', 1, 'ACTIVE'),
(@u2, @s3, 4, '安全间隔期与最高残留限量 [掌握]', 2, 'ACTIVE'),
(@u3, @s3, 4, '农业防治与物理防治措施 [理解]', 1, 'ACTIVE'),
(@u3, @s3, 4, '生物防治(天敌/微生物/性诱剂) [掌握]', 2, 'ACTIVE'),
(@u3, @s3, 4, 'IPM综合防治体系的构建 [理解]', 3, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 科目4: 农业生物技术 (3章×9单元×24知识点)
-- 教材: 《农业生物技术》(第二版) 高教社 ISBN 978-7-04-055256-0
-- ════════════════════════════════════════════════════════════
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r4, @s4, 2, '遗传基本规律', 1, 'ACTIVE'),
(@r4, @s4, 2, '作物育种方法', 2, 'ACTIVE'),
(@r4, @s4, 2, '品种与种子生产', 3, 'ACTIVE');

-- 4.1 遗传基本规律
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='遗传基本规律');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '孟德尔遗传定律', 1, 'ACTIVE'),
(@ch, @s4, 3, '连锁遗传与性别决定', 2, 'ACTIVE'),
(@ch, @s4, 3, '数量性状与变异', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='孟德尔遗传定律'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='连锁遗传与性别决定');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='数量性状与变异');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s4, 4, '分离定律的实质与验证 [理解]', 1, 'ACTIVE'),
(@u1, @s4, 4, '自由组合定律与杂交后代比例计算 [掌握]', 2, 'ACTIVE'),
(@u2, @s4, 4, '基因连锁与交换的遗传效应 [理解]', 1, 'ACTIVE'),
(@u2, @s4, 4, '性染色体与伴性遗传 [理解]', 2, 'ACTIVE'),
(@u3, @s4, 4, '数量性状与质量性状的区别 [理解]', 1, 'ACTIVE'),
(@u3, @s4, 4, '基因突变与染色体变异的类型 [识记]', 2, 'ACTIVE');

-- 4.2 作物育种方法
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='作物育种方法');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '选择育种', 1, 'ACTIVE'),
(@ch, @s4, 3, '杂交育种', 2, 'ACTIVE'),
(@ch, @s4, 3, '其他育种技术', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='选择育种'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='杂交育种');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='其他育种技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s4, 4, '单株选择与混合选择的方法 [掌握]', 1, 'ACTIVE'),
(@u1, @s4, 4, '系统育种程序 [理解]', 2, 'ACTIVE'),
(@u2, @s4, 4, '杂交亲本选配原则 [掌握]', 1, 'ACTIVE'),
(@u2, @s4, 4, '杂种优势的表现与利用(F1/F2) [理解]', 2, 'ACTIVE'),
(@u3, @s4, 4, '辐射诱变育种与化学诱变 [识记]', 1, 'ACTIVE'),
(@u3, @s4, 4, '多倍体育种与单倍体育种 [识记]', 2, 'ACTIVE');

-- 4.3 品种与种子生产
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='品种与种子生产');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@ch, @s4, 3, '品种与良种繁育', 1, 'ACTIVE'),
(@ch, @s4, 3, '种子检验与加工', 2, 'ACTIVE'),
(@ch, @s4, 3, '植物组织培养', 3, 'ACTIVE');
SET @u1=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='品种与良种繁育'); SET @u2=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='种子检验与加工');
SET @u3=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物组织培养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u1, @s4, 4, '品种的概念与良种标准 [识记]', 1, 'ACTIVE'),
(@u1, @s4, 4, '品种混杂退化的原因与防杂保纯措施 [掌握]', 2, 'ACTIVE'),
(@u2, @s4, 4, '种子质量检验指标(纯度/净度/发芽率/含水量) [掌握]', 1, 'ACTIVE'),
(@u2, @s4, 4, '种子加工(精选/包衣/包装)流程 [识记]', 2, 'ACTIVE'),
(@u3, @s4, 4, '植物组织培养的基本原理(细胞全能性) [理解]', 1, 'ACTIVE'),
(@u3, @s4, 4, '外植体选择与无菌接种技术 [掌握]', 2, 'ACTIVE'),
(@u3, @s4, 4, '组培苗的驯化与移栽 [理解]', 3, 'ACTIVE');

-- ════════════════════════════════════════════════════════════
-- 4科考纲(参照计算机 GENERAL 模式)
-- ════════════════════════════════════════════════════════════
INSERT IGNORE INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at) VALUES
(@s1, 'GENERAL', 'BOTH', '植物生产与环境考纲',
 '本课程是农林牧渔类专业的核心基础课。涵盖植物细胞组织、营养器官与生殖器官的形态结构、水分代谢与矿质营养、光合与呼吸作用、植物激素与生长发育、土壤肥料等内容。以识记和理解为主，光合作用、激素应用和施肥技术为掌握重点。',
 '1.0', 1, NOW(), NOW()),
(@s2, 'GENERAL', 'BOTH', '农作物生产技术考纲',
 '本课程讲授主要农作物的栽培技术。涵盖水稻/小麦/玉米三大粮食作物和油菜/棉花等经济作物的生育期、播种育苗、田间管理、水肥运筹、病虫害防治及收获技术。掌握各作物的关键栽培技术和产量构成因素分析。',
 '1.0', 1, NOW(), NOW()),
(@s3, 'GENERAL', 'BOTH', '植物保护技术考纲',
 '本课程讲授农作物病虫草害的识别与防治。涵盖真菌/细菌/病毒病害的症状识别与流行规律、咀嚼式和刺吸式口器害虫的为害特点与发生规律、农药分类/剂型/稀释计算/安全使用、农业/物理/化学/生物综合防治策略。',
 '1.0', 1, NOW(), NOW()),
(@s4, 'GENERAL', 'BOTH', '农业生物技术考纲',
 '本课程讲授遗传育种与生物技术基础。涵盖孟德尔遗传定律与连锁交换、选择育种与杂交育种方法、诱变/倍性育种简介、品种防杂保纯与种子质量检验、植物组织培养原理与操作技术。',
 '1.0', 1, NOW(), NOW());

-- ════════════════════════════════════════════════════════════
-- 4科种子题库(各科12题单选 = 48题)
-- ════════════════════════════════════════════════════════════

-- 植物生产与环境 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('植物生产与环境[职高]','SINGLE_CHOICE','植物细胞中进行光合作用的细胞器是：', '["A. 线粒体","B. 叶绿体","C. 内质网","D. 高尔基体"]', 'B', '叶绿体含有叶绿素，是光合作用的场所。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','有丝分裂中期染色体的排列位置是：', '["A. 细胞两极","B. 赤道板","C. 细胞膜内侧","D. 核膜周围"]', 'B', '有丝分裂中期着丝粒排列在赤道板上。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','须根系是哪种作物的根系？', '["A. 大豆","B. 棉花","C. 小麦","D. 油菜"]', 'C', '小麦/水稻/玉米等单子叶植物为须根系。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','下列属于茎变态的是：', '["A. 萝卜肉质根","B. 甘薯块根","C. 马铃薯块茎","D. 豌豆叶卷须"]', 'C', '马铃薯块茎有芽眼属于茎变态，萝卜/甘薯为根变态。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','一朵完全花应具备的四部分是：', '["A. 花梗/花托/花萼/花冠","B. 花萼/花冠/雄蕊群/雌蕊群","C. 花萼/花瓣/花丝/子房","D. 花托/花萼/花瓣/柱头"]', 'B', '完全花=花萼+花冠+雄蕊群+雌蕊群。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','种子萌发所需的外部条件不包括：', '["A. 充足水分","B. 适宜温度","C. 充足氧气","D. 充足光照"]', 'D', '大多数种子萌发不需要光照。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','光合作用中O2来源于：', '["A. CO2","B. H2O","C. C6H12O6","D. 叶绿素"]', 'B', '水光解: 2H2O→4H++4e-+O2↑。',3,1),
('植物生产与环境[职高]','SINGLE_CHOICE','下列属于大量元素的是：', '["A. 铁(Fe)","B. 硼(B)","C. 氮(N)","D. 锌(Zn)"]', 'C', 'N是大量元素，Fe/B/Zn为微量元素。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','促进果实成熟的主要激素是：', '["A. 生长素","B. 赤霉素","C. 细胞分裂素","D. 乙烯"]', 'D', '乙烯(C2H4)促进果实成熟和器官脱落。',2,1),
('植物生产与环境[职高]','SINGLE_CHOICE','冬小麦需经低温才能开花称为：', '["A. 光周期现象","B. 春化作用","C. 顶端优势","D. 向性运动"]', 'B', '低温诱导开花=春化作用。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','最理想的农业土壤质地是：', '["A. 砂土","B. 黏土","C. 壤土","D. 砾土"]', 'C', '壤土砂黏适中，保水保肥又通透。',1,1),
('植物生产与环境[职高]','SINGLE_CHOICE','过磷酸钙属于哪类肥料？', '["A. 氮肥","B. 磷肥","C. 钾肥","D. 复合肥"]', 'B', '过磷酸钙主要成分Ca(H2PO4)2·H2O，属磷肥。',2,1);

-- 农作物生产技术 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('农作物生产技术[职高]','SINGLE_CHOICE','水稻晒田的主要目的是：', '["A. 增加产量","B. 控制无效分蘖","C. 促进开花","D. 防治虫害"]', 'B', '晒田可抑制无效分蘖，促进根系下扎。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','小麦适宜播种深度一般为：', '["A. 1~2cm","B. 3~5cm","C. 8~10cm","D. 12~15cm"]', 'B', '小麦播深3~5cm，过深出苗难，过浅易受旱。',1,1),
('农作物生产技术[职高]','SINGLE_CHOICE','玉米需肥最多的时期是：', '["A. 苗期","B. 拔节期","C. 大喇叭口期","D. 成熟期"]', 'C', '大喇叭口期营养生长与生殖生长并进，需肥高峰。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','复种指数为200%表示：', '["A. 一年一熟","B. 一年两熟","C. 两年三熟","D. 两年一熟"]', 'B', '复种指数=全年播种面积/耕地面积×100%，200%=一年两熟。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','轮作的主要优点不包括：', '["A. 减轻病虫害","B. 均衡利用土壤养分","C. 简化田间管理","D. 改善土壤理化性质"]', 'C', '轮作可减轻病虫、均衡养分、改善土壤，但不会简化管理。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','小麦产量构成三因素是：', '["A. 穗数/穗粒数/千粒重","B. 株高/穗长/粒数","C. 播种量/施肥量/灌水量","D. 分蘖数/叶面积/穗数"]', 'A', '小麦产量=亩穗数×穗粒数×千粒重。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','水稻"够苗晒田"的指标是达到预期穗数的：', '["A. 50%","B. 70%","C. 80%~90%","D. 100%"]', 'C', '当茎蘖数达到预期穗数的80%~90%时开始晒田。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','油菜缺硼的典型症状是：', '["A. 叶片发黄","B. 花而不实","C. 根部腐烂","D. 茎秆倒伏"]', 'B', '油菜对硼敏感，缺硼导致"花而不实"（只开花不结籽）。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','间作与套作的主要区别在于：', '["A. 是否在同一块田","B. 共生期的长短","C. 作物种类不同","D. 施肥量不同"]', 'B', '间作共生期长(同时播种)，套作共生期短(前作收获前播种)。',2,1),
('农作物生产技术[职高]','SINGLE_CHOICE','棉花整枝打杈不包括：', '["A. 打顶心","B. 打边心","C. 抹赘芽","D. 打老叶"]', 'D', '棉花整枝包括打顶心/打边心/抹赘芽/去空枝，不包括打老叶。',3,1),
('农作物生产技术[职高]','SINGLE_CHOICE','甘薯育苗最常用的方法是：', '["A. 种子直播","B. 薯块温床育苗","C. 扦插育苗","D. 组织培养"]', 'B', '甘薯生产上主要采用薯块温床(酿热/电热)育苗。',1,1),
('农作物生产技术[职高]','SINGLE_CHOICE','马铃薯切块催芽每块至少保留：', '["A. 1个芽眼","B. 2个芽眼","C. 3个芽眼","D. 不需芽眼"]', 'A', '马铃薯切块每块至少保留1~2个健壮芽眼。',1,1);

-- 植物保护技术 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('植物保护技术[职高]','SINGLE_CHOICE','真菌性病害的典型症状是：', '["A. 叶片均匀黄化","B. 病斑上有霉层或粉状物","C. 整株萎蔫维管束不变色","D. 叶片皱缩呈花叶"]', 'B', '真菌病害常出现霉层/粉状物/锈状物等病症。',2,1),
('植物保护技术[职高]','SINGLE_CHOICE','蚜虫的口器类型为：', '["A. 咀嚼式","B. 刺吸式","C. 虹吸式","D. 舐吸式"]', 'B', '蚜虫用口针刺入植物组织吸取汁液，属刺吸式口器。',1,1),
('植物保护技术[职高]','SINGLE_CHOICE','农药稀释1000倍配制50L药液需原药：', '["A. 5mL","B. 10mL","C. 50mL","D. 100mL"]', 'C', '50000÷1000=50mL。',2,1),
('植物保护技术[职高]','SINGLE_CHOICE','利用赤眼蜂防治玉米螟属于：', '["A. 农业防治","B. 物理防治","C. 化学防治","D. 生物防治"]', 'D', '利用天敌昆虫防治害虫属于生物防治。',1,1),
('植物保护技术[职高]','SINGLE_CHOICE','病害三角不包括：', '["A. 感病寄主","B. 病原物","C. 农药残留","D. 适宜环境"]', 'C', '病害三角=感病寄主+病原物+适宜环境条件。',2,1),
('植物保护技术[职高]','SINGLE_CHOICE','水稻二化螟的为害方式是：', '["A. 吸食汁液","B. 钻蛀茎秆","C. 咬食叶片","D. 为害根部"]', 'B', '二化螟幼虫钻入稻茎取食，造成枯心苗或白穗。',2,1),
('植物保护技术[职高]','SINGLE_CHOICE','农药安全间隔期是指：', '["A. 两次施药的间隔","B. 最后一次施药到收获的天数","C. 施药到药效显现的天数","D. 不同农药混用的间隔"]', 'B', '安全间隔期是最后一次施药到作物收获的间隔天数。',2,1),
('植物保护技术[职高]','SINGLE_CHOICE','下列属于细菌性病害的是：', '["A. 小麦锈病","B. 水稻白叶枯病","C. 玉米大斑病","D. 油菜菌核病"]', 'B', '水稻白叶枯病由细菌(Xanthomonas oryzae)引起。',3,1),
('植物保护技术[职高]','SINGLE_CHOICE','病毒病的典型症状是：', '["A. 病斑上有霉层","B. 叶片皱缩花叶/矮化","C. 根部和茎基部腐烂","D. 维管束变褐"]', 'B', '病毒病典型症状为花叶/黄化/皱缩/矮化，无霉层等病症。',2,1),
('植物保护技术[职高]','SINGLE_CHOICE','灯光诱杀害虫属于：', '["A. 农业防治","B. 物理防治","C. 化学防治","D. 生物防治"]', 'B', '利用害虫趋光性用黑光灯/频振灯诱杀属物理防治。',1,1),
('植物保护技术[职高]','SINGLE_CHOICE','乳油(EC)属于农药的：', '["A. 原药","B. 剂型","C. 助剂","D. 稀释液"]', 'B', '乳油是一种常见农药剂型。其他剂型还有可湿性粉剂/悬浮剂等。',1,1),
('植物保护技术[职高]','SINGLE_CHOICE','IPM的核心思想是：', '["A. 全部使用化学农药","B. 协调使用多种防治方法","C. 只使用生物农药","D. 完全不用农药"]', 'B', 'IPM(有害生物综合治理)强调协调使用农业/物理/化学/生物等多种方法。',2,1);

-- 农业生物技术 12题
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('农业生物技术[职高]','SINGLE_CHOICE','杂合子(Aa)自交后代中纯合子比例为：', '["A. 1/4","B. 1/2","C. 3/4","D. 1"]', 'B', 'Aa×Aa→1AA:2Aa:1aa，纯合子(AA+aa)占1/2。',2,1),
('农业生物技术[职高]','SINGLE_CHOICE','品种间有性杂交属于哪种育种方法？', '["A. 诱变育种","B. 杂交育种","C. 倍性育种","D. 选择育种"]', 'B', '品种间有性杂交是杂交育种的基本方法。',2,1),
('农业生物技术[职高]','SINGLE_CHOICE','种子质量检验的四项主要指标是：', '["A. 纯度/净度/发芽率/含水量","B. 纯度/千粒重/发芽率/色泽","C. 净度/千粒重/含水量/色泽","D. 纯度/净度/千粒重/发芽势"]', 'A', '国家标准规定四项必检指标为纯度/净度/发芽率/含水量。',2,1),
('农业生物技术[职高]','SINGLE_CHOICE','植物组织培养的理论基础是：', '["A. 细胞学说","B. 细胞全能性","C. 进化论","D. 中心法则"]', 'B', '细胞全能性是指植物体细胞含有全套遗传信息，在适宜条件下可发育为完整植株。',1,1),
('农业生物技术[职高]','SINGLE_CHOICE','品种混杂退化的主要原因不包括：', '["A. 机械混杂","B. 生物学混杂","C. 自然突变","D. 合理轮作"]', 'D', '合理轮作不会导致品种退化，反而有利于保持品种特性。',2,1),
('农业生物技术[职高]','SINGLE_CHOICE','秋水仙素在育种中的主要作用是：', '["A. 诱导基因突变","B. 诱导染色体加倍","C. 促进杂交结实","D. 打破种子休眠"]', 'B', '秋水仙素抑制纺锤体形成，使染色体加倍获得多倍体。',3,1),
('农业生物技术[职高]','SINGLE_CHOICE','单株选择法相比混合选择法的优点是：', '["A. 操作简单","B. 选择效果好","C. 种子量大","D. 周期短"]', 'B', '单株选择通过对优良单株分别脱粒种植，选择效果优于混合选择。',2,1),
('农业生物技术[职高]','SINGLE_CHOICE','杂种优势在哪个世代最明显？', '["A. F0","B. F1","C. F2","D. F3"]', 'B', '杂种优势在F1代最明显，F2代开始衰退（性状分离）。',2,1),
('农业生物技术[职高]','SINGLE_CHOICE','组培中常用的外植体消毒剂是：', '["A. 酒精+升汞","B. 蒸馏水","C. 生长素溶液","D. 蔗糖溶液"]', 'A', '70%酒精+0.1%升汞(HgCl2)是组培中最常用的外植体表面消毒组合。',2,1),
('农业生物技术[职高]','SINGLE_CHOICE','下列不属于无性繁殖的是：', '["A. 嫁接","B. 扦插","C. 种子繁殖","D. 组织培养"]', 'C', '种子繁殖属于有性繁殖(经过受精过程)。',1,1),
('农业生物技术[职高]','SINGLE_CHOICE','连锁遗传中交换值越大表示：', '["A. 两基因距离越近","B. 两基因距离越远","C. 完全连锁","D. 基因突变率越高"]', 'B', '交换值(重组率)与基因间距离成正比，交换值越大基因距离越远。',3,1),
('农业生物技术[职高]','SINGLE_CHOICE','种子防杂保纯中"空间隔离"主要防止：', '["A. 机械混杂","B. 生物学混杂(串粉)","C. 种子带菌","D. 品种退化"]', 'B', '空间隔离主要防止不同品种间的花粉传播(生物学混杂)。',2,1);

SELECT 'v104: 农学类4科分离创建完成' AS result;
SELECT subject_name, (SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=ds.id) AS nodes, (SELECT COUNT(*) FROM question_bank WHERE subject=ds.subject_name) AS questions FROM dict_subject ds WHERE ds.subject_name LIKE '%[职高]' AND ds.id>=32 ORDER BY ds.id;
