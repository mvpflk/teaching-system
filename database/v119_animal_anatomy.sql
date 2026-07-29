-- v119: 畜禽解剖生理 第四版 孟婷/徐金花 ISBN 978-7-04-055119-8 9项目20任务
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES (NULL, 38, 1, '畜禽解剖生理[职高]', 1, 'ACTIVE');
SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=38 AND level=1 LIMIT 1);

INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r,38,2,'项目1 畜禽体基本结构识别',1,'ACTIVE'),(@r,38,2,'项目2 运动系统的解剖生理特征识别',2,'ACTIVE'),(@r,38,2,'项目3 消化系统的解剖生理特征识别',3,'ACTIVE'),(@r,38,2,'项目4 呼吸系统的解剖生理特征识别',4,'ACTIVE'),(@r,38,2,'项目5 泌尿系统的解剖生理特征识别',5,'ACTIVE'),(@r,38,2,'项目6 生殖系统的解剖生理特征识别',6,'ACTIVE'),(@r,38,2,'项目7 循环系统的解剖生理特征识别',7,'ACTIVE'),(@r,38,2,'项目8 其他系统的解剖生理特征识别',8,'ACTIVE'),(@r,38,2,'项目9 家禽的解剖生理特征识别',9,'ACTIVE');

-- P1: 2任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务1.1 动物细胞的识别',1,'ACTIVE'),(@p,38,3,'任务1.2 基本组织的识别',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%1.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'动物细胞的基本结构(细胞膜/细胞质/细胞核)及各部分功能 [掌握]',1,'ACTIVE'),(@t,38,4,'线粒体:细胞的动力工厂(有氧呼吸产ATP) [掌握]',2,'ACTIVE'),(@t,38,4,'核糖体:蛋白质合成场所 [掌握]',3,'ACTIVE'),(@t,38,4,'内质网(粗面合成蛋白/滑面合成脂类)与高尔基体(分拣包装分泌) [理解]',4,'ACTIVE'),(@t,38,4,'细胞的生命活动(新陈代谢/应激性/生长繁殖) [了解]',5,'ACTIVE'),(@t,38,4,'细胞分化/凋亡/坏死的概念 [了解]',6,'ACTIVE'),(@t,38,4,'显微镜的结构与使用方法 [掌握]',7,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%1.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'上皮组织的分类(单层扁平/单层立方/单层柱状/复层扁平/变移上皮)与分布 [掌握]',1,'ACTIVE'),(@t,38,4,'结缔组织的分类(疏松/致密/脂肪/软骨/骨/血液/淋巴)与功能 [掌握]',2,'ACTIVE'),(@t,38,4,'三种肌肉组织(骨骼肌/心肌/平滑肌)的光镜结构差异与分布 [掌握]',3,'ACTIVE'),(@t,38,4,'神经组织的组成(神经元+神经胶质细胞)与神经元结构(胞体/树突/轴突) [掌握]',4,'ACTIVE'),(@t,38,4,'器官/系统/有机体的概念 [了解]',5,'ACTIVE'),(@t,38,4,'解剖学常用方位术语(背/腹/前/后/近端/远端/内/外/三个切面) [掌握]',6,'ACTIVE');

-- P2: 3任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务2.1 骨的识别',1,'ACTIVE'),(@p,38,3,'任务2.2 骨连结的识别',2,'ACTIVE'),(@p,38,3,'任务2.3 动物体全身肌肉的识别',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'骨的主要成分(有机质:骨胶原/无机盐:钙盐磷酸盐)与物理特性 [掌握]',1,'ACTIVE'),(@t,38,4,'骨的分类:长骨(四肢)/短骨(腕跗)/扁骨(头肋)/不规则骨(椎骨) [掌握]',2,'ACTIVE'),(@t,38,4,'牛全身骨骼特征(头骨/脊柱/肋骨/胸骨/四肢骨)与骨性标志 [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'骨连结的三种类型:纤维连结/软骨连结/滑膜连结(关节) [掌握]',1,'ACTIVE'),(@t,38,4,'关节的基本结构(关节面+关节软骨/关节囊/关节腔/滑液)与辅助结构 [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%2.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'肌肉的辅助结构(筋膜/滑膜囊/腱鞘) [了解]',1,'ACTIVE'),(@t,38,4,'主要皮肌(面皮肌/颈皮肌/躯干皮肌)与全身主要肌肉群位置 [了解]',2,'ACTIVE');

-- P3: 3任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务3.1 消化管的识别',1,'ACTIVE'),(@p,38,3,'任务3.2 消化腺的识别',2,'ACTIVE'),(@p,38,3,'任务3.3 胃肠运动及小肠吸收的观察',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'内脏/腹腔与腹膜的概念及其相互关系 [掌握]',1,'ACTIVE'),(@t,38,4,'消化系统的组成(口腔→咽→食管→胃→小肠→大肠→肛门) [掌握]',2,'ACTIVE'),(@t,38,4,'消化管管壁的一般结构:黏膜层→黏膜下层→肌层→浆膜(外膜) [掌握]',3,'ACTIVE'),(@t,38,4,'单室胃(猪/马/犬)形态与分部(贲门部/胃底部/幽门部) [掌握]',4,'ACTIVE'),(@t,38,4,'反刍动物复胃结构:瘤胃(最大/微生物发酵)→网胃(蜂窝状)→瓣胃(百叶状)→皱胃(真胃/分泌胃酸) [掌握]',5,'ACTIVE'),(@t,38,4,'小肠三段顺序:十二指肠→空肠→回肠(长度递增/直径递减) [掌握]',6,'ACTIVE'),(@t,38,4,'大肠分段:盲肠→结肠→直肠(草食动物盲肠发达/肉食动物盲肠退化) [掌握]',7,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'唾液腺(腮腺/颌下腺/舌下腺)的形态位置与功能 [掌握]',1,'ACTIVE'),(@t,38,4,'肝的形态位置与生理功能(分泌胆汁/代谢解毒/糖原储存) [掌握]',2,'ACTIVE'),(@t,38,4,'胰的形态位置与外分泌(消化酶)和内分泌(胰岛素/胰高血糖素)功能 [掌握]',3,'ACTIVE'),(@t,38,4,'肝小叶的结构(中央静脉/肝细胞索/肝血窦/胆小管) [掌握]',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%3.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'消化方式:机械消化/化学消化/微生物消化的协同 [掌握]',1,'ACTIVE'),(@t,38,4,'各段消化道的主要消化酶与消化产物 [掌握]',2,'ACTIVE'),(@t,38,4,'反刍动物瘤胃微生物发酵原理(VFA生成)与食管沟功能 [掌握]',3,'ACTIVE'),(@t,38,4,'小肠是营养物质吸收的主要部位(绒毛增大表面积) [掌握]',4,'ACTIVE');

-- P4: 2任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务4.1 呼吸器官的识别',1,'ACTIVE'),(@p,38,3,'任务4.2 呼吸过程及其生理功能',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'上呼吸道组成:鼻腔→咽→喉→气管→支气管 [掌握]',1,'ACTIVE'),(@t,38,4,'肺的形态(左肺右肺分叶)与体内位置(胸腔内纵隔两侧) [掌握]',2,'ACTIVE'),(@t,38,4,'肺泡是气体交换的主要场所(单层扁平上皮+丰富毛细血管) [掌握]',3,'ACTIVE'),(@t,38,4,'胸腔/胸膜腔/纵隔的概念与胸膜腔负压的生理意义 [理解]',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%4.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'呼吸运动的发生机理:吸气(肋间外肌+膈肌收缩→胸腔扩大)/呼气(肌肉舒张→胸腔缩小) [掌握]',1,'ACTIVE'),(@t,38,4,'气体交换的动力(分压差)与O2/CO2在血液中的运输形式 [掌握]',2,'ACTIVE'),(@t,38,4,'肺通气量(潮气量×呼吸频率)与肺活量概念 [了解]',3,'ACTIVE'),(@t,38,4,'呼吸运动的神经调节(延髓呼吸中枢/肺牵张反射) [理解]',4,'ACTIVE');

-- P5: 2任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目5%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务5.1 泌尿器官的识别',1,'ACTIVE'),(@p,38,3,'任务5.2 尿分泌的观察',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%5.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'泌尿系统的组成:肾→输尿管→膀胱→尿道 [掌握]',1,'ACTIVE'),(@t,38,4,'肾的形态位置(腹膜后/脊柱两侧)与大体结构(皮质/髓质/肾盂) [掌握]',2,'ACTIVE'),(@t,38,4,'肾单位(肾小体+肾小管)是尿液生成的结构和功能单位 [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%5.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'尿的生成过程:肾小球滤过→肾小管和集合管重吸收→分泌排泄 [掌握]',1,'ACTIVE'),(@t,38,4,'尿的理化性质(颜色/透明度/比重/pH)与正常成分 [了解]',2,'ACTIVE');

-- P6: 2任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目6%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务6.1 雄性生殖系统解剖生理识别',1,'ACTIVE'),(@p,38,3,'任务6.2 雌性生殖系统解剖生理识别',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%6.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'雄性生殖器官:睾丸(产生精子和雄激素)/附睾/输精管/副性腺/阴茎 [掌握]',1,'ACTIVE'),(@t,38,4,'性成熟的概念与影响因素 [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%6.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'雌性生殖器官:卵巢(产生卵子和雌激素)/输卵管/子宫/阴道 [掌握]',1,'ACTIVE'),(@t,38,4,'发情周期(发情前期/发情期/发情后期/间情期)的概念 [了解]',2,'ACTIVE'),(@t,38,4,'受精/妊娠/分娩的基本概念 [了解]',3,'ACTIVE');

-- P7: 4任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目7%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务7.1 循环器官的识别',1,'ACTIVE'),(@p,38,3,'任务7.2 心脏活动的观察',2,'ACTIVE'),(@p,38,3,'任务7.3 血液微循环的观察',3,'ACTIVE'),(@p,38,3,'任务7.4 血液组分的识别',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%7.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'心脏的形态位置(胸腔纵隔内/偏左)与内部结构(四腔:左右心房+左右心室) [掌握]',1,'ACTIVE'),(@t,38,4,'血管分类:动脉(离心)/静脉(回心)/毛细血管(物质交换) [掌握]',2,'ACTIVE'),(@t,38,4,'体循环(大循环)与肺循环(小循环)的路径 [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%7.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'心动周期(心房收缩→心室收缩→全心舒张)与心率概念 [掌握]',1,'ACTIVE'),(@t,38,4,'心音(第一心音/第二心音)的产生与心输出量概念 [了解]',2,'ACTIVE'),(@t,38,4,'心肌的生理特性(自律性/兴奋性/传导性/收缩性) [掌握]',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%7.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'微循环的组成(微动脉→毛细血管→微静脉)与功能 [了解]',1,'ACTIVE'),(@t,38,4,'动脉血压的概念(收缩压/舒张压)与影响因素 [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%7.4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'血液的组成:血浆(55%)+血细胞(45%) [掌握]',1,'ACTIVE'),(@t,38,4,'红细胞(无核双凹圆盘状/含血红蛋白运输O2)/白细胞(免疫防御)/血小板(止血凝血)的形态与功能 [掌握]',2,'ACTIVE'),(@t,38,4,'血液凝固的基本过程(凝血酶原→凝血酶→纤维蛋白原→纤维蛋白) [了解]',3,'ACTIVE');

-- P8: 4任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目8%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,38,3,'任务8.1 免疫系统的解剖生理特征识别',1,'ACTIVE'),(@p,38,3,'任务8.2 神经系统的解剖生理特征识别',2,'ACTIVE'),(@p,38,3,'任务8.3 内分泌系统的解剖生理特征识别',3,'ACTIVE'),(@p,38,3,'任务8.4 被皮系统和感觉器官的解剖生理特征识别',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%8.1%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'免疫器官:中枢免疫器官(骨髓/胸腺/法氏囊)与外周免疫器官(淋巴结/脾脏) [掌握]',1,'ACTIVE'),(@t,38,4,'免疫细胞(淋巴细胞T/B/NK/巨噬细胞)的类型与功能 [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%8.2%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'中枢神经系统:脑(大脑/小脑/间脑/脑干)与脊髓的结构与功能 [掌握]',1,'ACTIVE'),(@t,38,4,'周围神经系统:脑神经(12对)/脊神经/自主神经(交感+副交感) [了解]',2,'ACTIVE'),(@t,38,4,'反射弧的组成(感受器→传入神经→中枢→传出神经→效应器) [掌握]',3,'ACTIVE'),(@t,38,4,'条件反射与非条件反射的区别 [掌握]',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%8.3%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'主要内分泌器官:脑垂体(促激素)/甲状腺(甲状腺素T3T4)/肾上腺(肾上腺素/皮质醇)/胰腺(胰岛素/胰高血糖素) [掌握]',1,'ACTIVE'),(@t,38,4,'激素的概念与作用特点(微量/高效/特异性/无始动作用) [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%8.4%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'皮肤的结构(表皮/真皮/皮下组织)与衍生物(毛/蹄/角/汗腺/皮脂腺/乳腺) [掌握]',1,'ACTIVE'),(@t,38,4,'视觉器官(眼球壁三层结构/晶状体/玻璃体)与听觉器官(外耳/中耳/内耳)基本结构 [了解]',2,'ACTIVE');

-- P9: 1任务
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '项目9%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@p,38,3,'家禽的解剖生理特征识别',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%家禽%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,38,4,'家禽消化系统特点(嗉囊贮存/肌胃研磨/泄殖腔)与哺乳动物区别 [掌握]',1,'ACTIVE'),(@t,38,4,'家禽呼吸系统特点(气囊/双重呼吸) [掌握]',2,'ACTIVE'),(@t,38,4,'家禽泌尿生殖系统特点(无膀胱/卵生/左侧卵巢发达) [掌握]',3,'ACTIVE');

-- 考纲
INSERT INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at) VALUES
(38,'DUIKOU','BOTH','畜禽解剖生理考纲(2023版)',
 '# 畜禽解剖生理 考试大纲 教材:孟婷/徐金花第四版 ISBN 978-7-04-055119-8 分值约20分(10%)\n9项目20任务\n\n## 项目1 畜禽体基本结构识别\n细胞结构(膜/质/核/细胞器)/四种基本组织(上皮/结缔/肌肉/神经)/器官系统概念/解剖方位术语\n\n## 项目2 运动系统\n骨成分与分类/牛骨骼特征/关节结构/主要肌肉\n\n## 项目3 消化系统\n消化管结构(黏膜→浆膜)/复胃(瘤网瓣皱)/小肠三段/消化腺(肝/胰/唾液腺)/消化方式/反刍特点\n\n## 项目4 呼吸系统\n呼吸道组成/肺形态与肺泡/呼吸运动机理/气体交换与运输\n\n## 项目5 泌尿系统\n肾/输尿管/膀胱/尿道/肾单位/尿生成过程\n\n## 项目6 生殖系统\n雄性(睾丸附睾)/雌性(卵巢输卵管子宫)/发情周期\n\n## 项目7 循环系统\n心脏四腔/体循环肺循环/心动周期/血液组成(红细胞白细胞血小板)/凝血\n\n## 项目8 其他系统\n免疫(骨髓胸腺淋巴结脾)/神经(脊髓脑脑神经反射弧)/内分泌(垂体甲状腺肾上腺胰岛)/被皮感官(皮肤眼球耳)\n\n## 项目9 家禽解剖特点\n嗉囊肌胃泄殖腔/气囊双重呼吸/无膀胱卵生左侧卵巢'
 ,'2023',1,NOW(),NOW());
INSERT INTO exam_syllabus_node_relation (syllabus_id, node_id) SELECT es.id, kn.id FROM exam_syllabus es JOIN knowledge_nodes kn ON es.subject_id=kn.subject_id AND kn.level=1 WHERE es.subject_id=38;

SELECT CONCAT('畜禽解剖生理: ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=38 AND level=2),'项目 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=38 AND level=3),'任务 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=38 AND level=4),'知识点') AS result;
