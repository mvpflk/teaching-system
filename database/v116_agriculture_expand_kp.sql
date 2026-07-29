-- v116: 农林牧渔4科知识点大幅扩充 (143→~250)
SET @s1=36; SET @s2=37; SET @s3=38; SET @s4=39;
SET @r1=(SELECT id FROM knowledge_nodes WHERE subject_id=@s1 AND level=1);
SET @r2=(SELECT id FROM knowledge_nodes WHERE subject_id=@s2 AND level=1);
SET @r3=(SELECT id FROM knowledge_nodes WHERE subject_id=@s3 AND level=1);
SET @r4=(SELECT id FROM knowledge_nodes WHERE subject_id=@s4 AND level=1);

-- ═══ 植物生产与环境: 43→~88 ═══
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与环境概述');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物生长发育基本概念');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '春化作用的概念/类型(种子春化/绿体春化)与生产应用 [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '光周期现象:长日植物/短日植物/日中性植物代表作物 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, '花芽分化的概念与影响因素(温度/光照/营养/激素) [了解]', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='环境因素对植物的影响');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '光照强度/光质/日照长度对植物形态建成的影响 [掌握]', 3, 'ACTIVE'),
(@u, @s1, 4, '温周期现象与植物生长的昼夜节律 [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '水分胁迫(干旱/涝害)对植物生理代谢的影响 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, 'CO2浓度与风对植物光合作用和蒸腾的影响 [了解]', 6, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物的生长发育');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物细胞与组织');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '细胞膜选择透过性与物质跨膜运输方式(被动/主动/胞吞胞吐) [理解]', 4, 'ACTIVE'),
(@u, @s1, 4, '内质网(粗面/滑面)与高尔基体的协调功能 [理解]', 5, 'ACTIVE'),
(@u, @s1, 4, '保护组织(表皮/周皮)/机械组织(厚角/厚壁)的结构特点 [掌握]', 6, 'ACTIVE'),
(@u, @s1, 4, '导管(被子植物)/管胞(裸子植物)的输水功能比较 [理解]', 7, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物的营养器官与生殖器官');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '根尖四个分区(根冠/分生区/伸长区/根毛区)的结构与功能 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, '茎的初生生长(顶端分生组织)与次生生长(形成层/年轮) [理解]', 6, 'ACTIVE'),
(@u, @s1, 4, '叶的解剖结构(表皮/栅栏组织/海绵组织/维管束) [理解]', 7, 'ACTIVE'),
(@u, @s1, 4, '花的类型(完全/不完全/两性/单性)与花序类型 [掌握]', 8, 'ACTIVE'),
(@u, @s1, 4, '果实的发育与类型(真果/假果/单果/聚合果/聚花果) [掌握]', 9, 'ACTIVE'),
(@u, @s1, 4, '被子植物双受精的过程(精子+卵=胚/精子+极核=胚乳) [理解]', 10, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='植物激素与生长调节剂');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '生长素极性运输与顶端优势调控机制 [理解]', 3, 'ACTIVE'),
(@u, @s1, 4, '赤霉素促进茎伸长和α-淀粉酶合成(啤酒发芽) [理解]', 4, 'ACTIVE'),
(@u, @s1, 4, '脱落酸(ABA)促进气孔关闭与种子休眠(抗逆激素) [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, '激素间协同(生长素+赤霉素)与拮抗(生长素vs细胞分裂素) [理解]', 6, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与土壤培肥');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='土壤基本组成与性质');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '土壤三相的理想体积比(固:液:气≈50:25:25) [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '土壤团粒结构的概念/形成条件与肥力意义 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, '土壤酸碱性的分级(<4.5强酸~>9.0强碱)与指示植物 [了解]', 6, 'ACTIVE'),
(@u, @s1, 4, '土壤缓冲性的概念(抵抗pH变化的能力) [了解]', 7, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='肥料种类与科学施用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '尿素/碳铵/硝铵的含氮量/性质/施用方法比较 [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '过磷酸钙与钙镁磷肥的性质/适用土壤/施用方法 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, '氯化钾与硫酸钾的区别(忌氯作物:烟草/马铃薯/葡萄) [掌握]', 6, 'ACTIVE'),
(@u, @s1, 4, '复合肥(磷酸二铵/磷酸二氢钾)的养分含量与特点 [了解]', 7, 'ACTIVE'),
(@u, @s1, 4, '绿肥(紫云英/苕子/田菁)种植翻压与固氮量 [掌握]', 8, 'ACTIVE'),
(@u, @s1, 4, '施肥量计算:目标产量法=(目标需肥量-土壤供肥量)/肥料利用率 [掌握]', 9, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='测土配方施肥');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '土壤样品采集方法(蛇形/棋盘/对角线/采样深度0-20cm) [掌握]', 3, 'ACTIVE'),
(@u, @s1, 4, '养分丰缺指标分级(极低/低/中/高/极高)与施肥建议 [了解]', 4, 'ACTIVE'),
(@u, @s1, 4, '肥料利用率=(施肥区吸收-无肥区吸收)/施肥量×100% [掌握]', 5, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与科学用水');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水分与植物生长');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '水分在植物体内运输途径(根毛→皮层→中柱→导管→叶脉→气孔) [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '作物的需水量与需水模系数(各生育阶段需水占比) [理解]', 5, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='科学灌溉技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '土壤含水量表示(质量含水量/容积含水量/相对含水量) [掌握]', 3, 'ACTIVE'),
(@u, @s1, 4, '田间持水量与萎蔫系数的概念及灌溉意义 [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '灌溉定额(净灌溉定额/毛灌溉定额)计算 [掌握]', 5, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与光能利用');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='光合作用与呼吸作用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, 'C3植物与C4植物光合效率差异及代表作物(小麦C3/玉米C4) [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '光饱和点/光补偿点概念及在合理密植中的应用 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, 'CO2补偿点/饱和点概念及温室增施CO2技术 [理解]', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='光环境调控与光能利用率');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '光能利用率计算公式与我国农田现状(约0.5%-1%) [掌握]', 3, 'ACTIVE'),
(@u, @s1, 4, '提高光能利用率的措施(合理密植/间套复种/高光效品种) [掌握]', 4, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与温度调控');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='温度对植物的影响');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '温周期现象与昼夜变温对植物生长的促进 [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '高温热害与低温冷害的作物危害机理 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, '有效积温K=N(T-C)的生育期预测应用 [掌握]', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='温度调控技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '地膜覆盖的增温保墒机理与适用作物 [掌握]', 3, 'ACTIVE'),
(@u, @s1, 4, '日光温室结构特点与温室效应原理 [了解]', 4, 'ACTIVE'),
(@u, @s1, 4, '防霜冻方法(熏烟/灌水/覆盖/风机扰动) [掌握]', 5, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r1 AND name='植物生产与农业气象');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='主要农业气象灾害');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '干旱指标(降水量距平/土壤湿度)与抗旱技术 [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '洪涝等级(轻涝/中涝/重涝)与灾后补救 [掌握]', 5, 'ACTIVE'),
(@u, @s1, 4, '干热风对小麦危害机理与防御(抗性品种/灌溉/化学调控) [掌握]', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='二十四节气与农事');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s1, 4, '二十四节气歌诀与对应太阳黄经度数 [掌握]', 4, 'ACTIVE'),
(@u, @s1, 4, '四川盆地主要作物(水稻/小麦/油菜/玉米)的关键农事节气 [掌握]', 5, 'ACTIVE');

-- ═══ 畜禽营养与饲料: 30→~60 ═══
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='畜禽营养基础');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='动植物体的化学组成');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '动植物体化学组成差异:植物体碳水化合物多/动物体蛋白质脂肪多 [掌握]', 3, 'ACTIVE'),
(@u, @s2, 4, '动植物体均以C/H/O/N为主(占干物质90%以上) [理解]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='消化与吸收');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '消化率=(食入营养-粪中营养)/食入营养×100% [掌握]', 4, 'ACTIVE'),
(@u, @s2, 4, '单胃动物(猪/禽)与反刍动物(牛/羊)消化系统结构差异 [掌握]', 5, 'ACTIVE'),
(@u, @s2, 4, '影响消化率的饲料因素(粗纤维/加工方式/抗营养因子) [掌握]', 6, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='营养物质的利用');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='蛋白质的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '理想蛋白质(氨基酸平衡模式)的概念及应用 [理解]', 4, 'ACTIVE'),
(@u, @s2, 4, '瘤胃微生物蛋白(MCP)合成条件与营养价值 [掌握]', 5, 'ACTIVE'),
(@u, @s2, 4, '过瘤胃保护蛋白(bypass protein)概念与加工方法 [了解]', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='碳水化合物的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '无氮浸出物(NFE)=淀粉+可溶性糖 [掌握]', 4, 'ACTIVE'),
(@u, @s2, 4, '瘤胃VFA(乙酸/丙酸/丁酸)的生成与利用 [掌握]', 5, 'ACTIVE'),
(@u, @s2, 4, '粗纤维在反刍动物日粮中的最低需要量(有效纤维) [掌握]', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='脂肪的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '必需脂肪酸(亚油酸/亚麻酸/花生四烯酸)与缺乏症 [掌握]', 3, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='矿物质的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '钙磷代谢与维生素D(1,25-(OH)2-D3)的关系 [掌握]', 3, 'ACTIVE'),
(@u, @s2, 4, '硒-维生素E协同抗氧化与白肌病/渗出性素质防治 [掌握]', 4, 'ACTIVE'),
(@u, @s2, 4, '铜/铁/钴的造血功能与贫血症防治 [掌握]', 5, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='维生素的营养');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '维生素A与视觉紫红质合成/夜盲症防治 [掌握]', 4, 'ACTIVE'),
(@u, @s2, 4, '维生素D与钙磷代谢/佝偻病防治 [掌握]', 5, 'ACTIVE'),
(@u, @s2, 4, '维生素E抗氧化功能与繁殖性能 [掌握]', 6, 'ACTIVE'),
(@u, @s2, 4, 'B族维生素(硫胺素/核黄素/烟酸)的辅酶功能 [理解]', 7, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='能量的转化与利用');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '代谢能ME≈消化能DE×0.82(反刍)/0.96(猪禽) [掌握]', 3, 'ACTIVE'),
(@u, @s2, 4, '热增耗(HI)的概念与冷热应激管理应用 [了解]', 4, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='饲料及其加工利用');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='饲料分类与特性');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '粗饲料氨化(尿素3%-5%密封2-4周)与碱化(NaOH法) [掌握]', 4, 'ACTIVE'),
(@u, @s2, 4, '青绿饲料:亚硝酸盐/氢氰酸中毒预防爆炸病预防 [掌握]', 5, 'ACTIVE'),
(@u, @s2, 4, '饼粕类(豆粕/棉粕/菜粕)营养特性与抗营养因子 [掌握]', 6, 'ACTIVE'),
(@u, @s2, 4, '动物性蛋白饲料(鱼粉/肉骨粉/血粉)营养价值 [了解]', 7, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='饲料加工与调制');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '秸秆微贮技术(微生物发酵)原理与操作 [了解]', 5, 'ACTIVE'),
(@u, @s2, 4, '颗粒饲料优点(减少浪费/提高采食/便于运输) [了解]', 6, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r2 AND name='营养需要与配合饲料配制');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='畜禽营养需要');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '生长育肥猪各阶段营养需要特点 [掌握]', 3, 'ACTIVE'),
(@u, @s2, 4, '蛋鸡产蛋期营养需要变化(产蛋率/蛋重/采食量) [掌握]', 4, 'ACTIVE'),
(@u, @s2, 4, 'NRC/中国饲养标准的查阅使用方法 [了解]', 5, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='配合饲料配制技术');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s2, 4, '试差法配制多原料全价饲料的计算步骤 [掌握]', 5, 'ACTIVE'),
(@u, @s2, 4, '预混料载体(脱脂米糠/麦饭石/玉米芯粉)选择原则 [了解]', 6, 'ACTIVE'),
(@u, @s2, 4, '混合均匀度要求(CV≤10%/预混料≤5%) [了解]', 7, 'ACTIVE');

-- ═══ 动物解剖生理: 23→~48 ═══
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='动物体基本结构');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='细胞与基本组织');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '细胞质基质的组成与功能(糖酵解场所/细胞骨架) [理解]', 4, 'ACTIVE'),
(@u, @s3, 4, '上皮组织分类(单层/复层/假复层)与分布 [掌握]', 5, 'ACTIVE'),
(@u, @s3, 4, '结缔组织分类(疏松/致密/脂肪/软骨/骨/血液) [掌握]', 6, 'ACTIVE'),
(@u, @s3, 4, '三种肌肉(骨骼肌/心肌/平滑肌)光镜结构差异 [掌握]', 7, 'ACTIVE'),
(@u, @s3, 4, '神经元结构(胞体/树突/轴突)与神经胶质细胞功能 [掌握]', 8, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='解剖学方位术语');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '三个基本切面(矢状面/额状面/横切面)的定义 [掌握]', 2, 'ACTIVE'),
(@u, @s3, 4, '畜体体表分区(头/颈/躯干/四肢)与骨性标志 [了解]', 3, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='运动系统');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='骨骼系统');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '脊柱组成(颈7/胸13/腰6/荐5/尾不定)[掌握]', 4, 'ACTIVE'),
(@u, @s3, 4, '前肢骨骼(肩胛骨→肱骨→桡骨+尺骨→腕骨→掌骨→指骨) [掌握]', 5, 'ACTIVE'),
(@u, @s3, 4, '后肢骨骼(髋骨→股骨→胫骨+腓骨→跗骨→跖骨→趾骨) [掌握]', 6, 'ACTIVE'),
(@u, @s3, 4, '牛/猪/马头骨特征比较(角突/吻突/面嵴) [了解]', 7, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='骨连结与关节');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '滑膜关节辅助结构(韧带/关节盘/关节盂缘) [了解]', 3, 'ACTIVE'),
(@u, @s3, 4, '脊柱连接方式(椎间盘+椎间关节/枕寰/寰枢) [了解]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='肌肉系统概述');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '肌肉辅助结构(筋膜/滑膜囊/腱鞘) [了解]', 2, 'ACTIVE'),
(@u, @s3, 4, '主要皮肌(面皮肌/颈皮肌/躯干皮肌)位置与功能 [了解]', 3, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='消化系统');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='消化管的结构');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '食管的三个狭窄部位与临床意义 [了解]', 4, 'ACTIVE'),
(@u, @s3, 4, '单室胃形态与分部(贲门部/胃底部/幽门部) [掌握]', 5, 'ACTIVE'),
(@u, @s3, 4, '大肠分段(盲肠→结肠→直肠)与形态特征 [掌握]', 6, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='消化腺与消化生理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '肝小叶结构(中央静脉/肝细胞索/肝血窦/胆小管) [掌握]', 3, 'ACTIVE'),
(@u, @s3, 4, '胆汁生成/贮存/排放途径及乳化脂肪作用 [掌握]', 4, 'ACTIVE'),
(@u, @s3, 4, '胰液成分(胰蛋白酶/胰脂肪酶/胰淀粉酶/碳酸氢盐) [掌握]', 5, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='反刍动物消化特点');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '食管沟结构与功能(犊牛吮乳时直接将乳送入皱胃) [掌握]', 4, 'ACTIVE'),
(@u, @s3, 4, '瘤胃内环境(pH5.5-7.0/38-41℃/厌氧)维持机制 [掌握]', 5, 'ACTIVE'),
(@u, @s3, 4, '网胃蜂窝状结构与创伤性网胃心包炎机理 [了解]', 6, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r3 AND name='呼吸系统');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='呼吸器官的结构');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '喉软骨(会厌/甲状/环状/杓状)与发声功能 [了解]', 4, 'ACTIVE'),
(@u, @s3, 4, '胸膜与胸膜腔概念/胸膜腔负压生理意义 [理解]', 5, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='呼吸生理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s3, 4, '气体交换动力(分压差)与血液运输(O2-Hb/CO2-碳酸氢盐) [掌握]', 3, 'ACTIVE'),
(@u, @s3, 4, '呼吸运动的神经调节(延髓中枢/肺牵张反射) [理解]', 4, 'ACTIVE');

-- ═══ 农业经营与管理: 28→~55 ═══
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业经营概述');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业的概念与特点');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '狭义农业(种植业)与广义农业(农林牧渔副)的区别 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '自然再生产与经济再生产交织的具体表现 [理解]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业经营方式');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '家庭经营优势(决策灵活/责任心强)与局限性 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '公司+基地+农户模式的运行机制与利益分配 [掌握]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='新型农业经营主体');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '家庭农场认定标准(规模/劳动力/收入)与扶持政策 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '合作社民主管理(一人一票/盈余返还) [掌握]', 4, 'ACTIVE'),
(@u, @s4, 4, '龙头企业类型(加工型/流通型/服务型)与带动作用 [了解]', 5, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农产品市场营销');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='市场调查与预测');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '问卷设计结构(标题/说明/正文/背景)与题型 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '市场细分(地理/消费者/用途)与目标市场选择 [掌握]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农产品营销策略');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '定价方法:成本导向/竞争导向/需求导向 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '销售渠道:传统渠道与电商直销比较 [掌握]', 4, 'ACTIVE'),
(@u, @s4, 4, '包装策略(分级/礼品/绿色)与品牌命名原则 [了解]', 5, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业资源与资金管理');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业自然资源管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '耕地占补平衡与基本农田保护制度 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '农业面源污染(化肥/农药/畜禽粪便)危害与防治 [掌握]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业资金管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '农业贷款种类(信用/抵押/担保/扶贫小额信贷) [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '农产品成本构成(物质/人工/土地/服务费用) [掌握]', 4, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业生产管理');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='种植业生产管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '作物布局原则(因地制宜/用地养地/市场导向) [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '农事历编制方法与季节安排 [掌握]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='养殖业生产管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '料肉比(饲料转化率)概念与计算 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '全进全出制饲养管理优点 [掌握]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农产品质量管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, 'HACCP体系原理(危害分析→关键控制点) [了解]', 3, 'ACTIVE'),
(@u, @s4, 4, '绿色食品A级与AA级区别(限量vs禁止化学合成) [掌握]', 4, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业政策与法规');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业政策');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '耕地地力保护补贴对象与标准 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '农产品价格支持(最低收购价/临时收储/目标价格补贴) [了解]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业法规');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '种子生产经营许可与假劣种子法律责任 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '农药管理条例:高毒农药禁限用与违规处罚 [掌握]', 4, 'ACTIVE'),
(@u, @s4, 4, '动物防疫法:强制免疫与检疫申报规定 [了解]', 5, 'ACTIVE');

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r4 AND name='农业创业与效益评价');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业创业基础');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, 'SWOT分析法(优势/劣势/机会/威胁)在创业中的应用 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '资金筹措渠道(自有/合伙/银行贷款/政府扶持/众筹) [了解]', 4, 'ACTIVE');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农业经济效益评价');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@u, @s4, 4, '投资回收期=投资总额÷年均净收益 [掌握]', 3, 'ACTIVE'),
(@u, @s4, 4, '土地产出率与劳动生产率计算与比较 [掌握]', 4, 'ACTIVE'),
(@u, @s4, 4, '农产品商品率=商品量÷总产量×100% [了解]', 5, 'ACTIVE');

SELECT 'v116: 知识点扩充完成' AS result;
SELECT subject_id AS 科目, COUNT(CASE WHEN level=4 THEN 1 END) AS 扩充后L4 FROM knowledge_nodes WHERE subject_id BETWEEN 36 AND 39 AND status='ACTIVE' GROUP BY subject_id ORDER BY subject_id;
