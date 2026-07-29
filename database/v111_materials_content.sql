-- v111: 建筑材料 content注入 (23个知识点)
SET @s=28; SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=@s AND level=1 LIMIT 1);

-- 第1章 材料的基本性质
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='材料的基本性质');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='物理性质');
UPDATE knowledge_nodes SET content='# 密度/表观密度/堆积密度的区别\n\n【一句话定义】密度(ρ=m/V实):材料在绝对密实状态下单位体积的质量(不含孔隙)→用于计算孔隙率。表观密度(ρ0=m/V0):材料在自然状态下单位体积的质量(含内部孔隙)→用于计算自重荷载。堆积密度:散粒材料在自然堆积状态下单位体积的质量(含颗粒间空隙+颗粒内孔隙)→用于计算堆场体积和运输量。三者大小关系:密度>表观密度>堆积密度(因包含的空隙体积依次增大)。\n\n【具体例子】一块标准砖(240×115×53mm)干燥质量2.5kg:表观密度=2.5/(0.24×0.115×0.053)≈1710kg/m³。碎石松散堆积:1m³碎石质量约1550kg→堆积密度=1550kg/m³。\n\n【考点提示·笔试】必考辨析:三个密度的定义/公式/大小顺序/工程应用。' WHERE subject_id=@s AND level=4 AND name LIKE '%密度%表观密度%堆积密度%';
UPDATE knowledge_nodes SET content='# 吸水率与含水率的计算\n\n【一句话定义】质量吸水率Wm=(吸水饱和质量-干燥质量)/干燥质量×100%。体积吸水率Wv=Wm×ρ0(表观密度)。含水率(自然状态)=(含水质量-干燥质量)/干燥质量×100%反映材料当前的潮湿程度。吸水率反映材料的孔隙率和孔隙特征(开口孔隙多→吸水率大→抗冻性差)。\n\n【考点提示·笔试】计算:给干燥质量和吸水饱和质量→求吸水率。' WHERE subject_id=@s AND level=4 AND name LIKE '%吸水率%含水率%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='力学性质');
UPDATE knowledge_nodes SET content='# 抗压/抗拉/抗弯/抗剪强度\n\n【一句话定义】强度是材料在外力作用下抵抗破坏的能力(MPa=N/mm²)。抗压强度:压缩荷载→破坏时单位面积最大压力(混凝土/砖/石材主要承受压力)。抗拉强度:拉伸荷载→钢材抗拉强度高/混凝土很低(约为抗压1/10)。抗弯(抗折)强度:弯曲荷载→梁板等受弯构件。抗剪强度:剪切荷载→螺栓/焊缝等。脆性材料(混凝土/砖)抗压>>抗拉;韧性材料(钢材)抗压≈抗拉。\n\n【考点提示·笔试】辨析:不同材料的主要强度类型(混凝土看抗压/钢材看抗拉)。' WHERE subject_id=@s AND level=4 AND name LIKE '%抗压%抗拉%抗弯%抗剪%';
UPDATE knowledge_nodes SET content='# 弹性/塑性/脆性/韧性的区分\n\n【一句话定义】弹性:卸载后变形完全恢复(钢材在弹性阶段)。塑性:卸载后变形不能恢复(粘土/沥青)。脆性:破坏前无明显变形/突然断裂(混凝土/玻璃/砖/石材)。韧性:破坏前能吸收较大能量/有显著变形(钢材/木材)。工程选用:受冲击荷载结构选韧性材料(钢材);受压构件可选脆性材料(砖柱/混凝土柱)。\n\n【考点提示·笔试】辨析:给材料名称→判断变形特征。' WHERE subject_id=@s AND level=4 AND name LIKE '%弹性%塑性%脆性%韧性%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='耐久性');
UPDATE knowledge_nodes SET content='# 材料耐久性的影响因素\n\n【一句话定义】耐久性:材料在长期使用过程中抵抗环境侵蚀保持原有性能的能力。主要影响因素:物理作用(冻融循环/干湿交替/温度变化→冻融破坏最严重)、化学作用(酸雨/硫酸盐/碳化/碱骨料反应→钢筋锈蚀和混凝土劣化)、生物作用(霉菌/虫蛀/腐朽→木材和有机材料)、机械作用(磨损/冲击)。\n\n【考点提示·笔试】简答:影响耐久性的四类因素及代表性破坏形式。' WHERE subject_id=@s AND level=4 AND name LIKE '%耐久性%';

-- 第2章 气硬性胶凝材料
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='气硬性胶凝材料');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='石灰');
UPDATE knowledge_nodes SET content='# 石灰的熟化与硬化过程\n\n【一句话定义】石灰的熟化:生石灰CaO+水→Ca(OH)2+大量热(放热反应/体积膨胀2~3.5倍)。块状生石灰→熟化成石灰膏(含大量游离水)→陈伏2周以上(使过火石灰充分消解防止后期爆灰)。硬化:Ca(OH)2与空气中CO2反应→CaCO3+水(碳化硬化/速度极慢)/同时Ca(OH)2结晶析出(结晶硬化)。\n\n【考点提示·笔试】简答:石灰陈伏的目的(消除过火石灰危害)。石灰硬化慢的原因。' WHERE subject_id=@s AND level=4 AND name LIKE '%石灰%熟化%';
UPDATE knowledge_nodes SET content='# 石灰的性质与应用\n\n【一句话定义】石灰特性:保水性好(和易性好)、可塑性好、硬化缓慢强度低(28天强度仅0.5~1MPa)、硬化时体积收缩大(须掺砂或纤维防开裂)、耐水性差(遇水溶解→仅用于地上干燥环境)。应用:石灰砂浆(砌筑抹灰)、石灰土(灰土基础→石灰+粘土夯实)、灰砂砖(蒸压养护)、碳化石灰板(轻质隔墙)。\n\n【考点提示·笔试】简答:石灰的特性(优点和缺点)及工程应用。' WHERE subject_id=@s AND level=4 AND name LIKE '%石灰%性质%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='石膏');
UPDATE knowledge_nodes SET content='# 建筑石膏的水化与硬化\n\n【一句话定义】建筑石膏(半水石膏CaSO4·0.5H2O)由二水石膏(CaSO4·2H2O)在107~170℃煅烧脱水而成。水化:半水石膏+水→二水石膏(胶凝+结晶)。特性:凝结硬化快(初凝3~5min/终凝<30min)/体积微膨胀(不收缩不裂缝)/孔隙率高(质轻保温吸声)/防火性好(二水石膏含结晶水遇火蒸发吸热)。\n\n【考点提示·笔试】简答:石膏凝结快的原因和应用注意事项(须在初凝前用完)。石膏制品的防火原理。' WHERE subject_id=@s AND level=4 AND name LIKE '%石膏%水化%';
UPDATE knowledge_nodes SET content='# 石膏制品的特点与应用\n\n【一句话定义】常见石膏制品:纸面石膏板(内隔墙吊顶)、石膏砌块(非承重内隔墙)、装饰石膏板(天花板)、石膏线脚(装饰线条)。特点:质轻/保温隔声/防火/可加工性好(锯/钉/刨)/不耐水(仅用于室内干燥环境)→耐水石膏板(掺有机硅等防水剂)可用于潮湿房间。\n\n【考点提示·笔试】选择:石膏制品的适用环境(室内干燥)和不适用环境(室外/潮湿)。' WHERE subject_id=@s AND level=4 AND name LIKE '%石膏制品%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水玻璃');
UPDATE knowledge_nodes SET content='# 水玻璃的组成与特性\n\n【一句话定义】水玻璃(硅酸钠Na2O·nSiO2/泡花碱):n为模数(SiO2/Na2O摩尔比/一般为2.0~3.5)。特性:耐酸性强(不与酸反应→用作耐酸砂浆和耐酸混凝土胶凝材料)、耐热性好(可配制耐热混凝土≤1200℃)、粘结力强(用作涂料/砂浆掺料)。固化需加入硬化剂(氟硅酸钠Na2SiF6促进SiO2胶体析出固化)。\n\n【考点提示·笔试】选择:水玻璃的特性和用途配对(耐酸/耐热/粘结)。' WHERE subject_id=@s AND level=4 AND name LIKE '%水玻璃%';

-- 第3章 水泥与混凝土
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='水泥与混凝土');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='硅酸盐水泥');
UPDATE knowledge_nodes SET content='# 硅酸盐水泥的矿物组成与特性\n\n【一句话定义】硅酸盐水泥熟料四种主要矿物:C3S(硅酸三钙/含量50%~60%→决定早期强度/水化快放热多)、C2S(硅酸二钙/20%~25%→决定后期强度/水化慢放热少)、C3A(铝酸三钙/7%~15%→水化最快放热最多/决定凝结速度/耐硫酸盐性差)、C4AF(铁铝酸四钙/10%~15%→降低烧成温度/耐磨性好)。\n\n【考点提示·笔试】必考:四种矿物的名称/含量/主要特性配对。口诀:"三钙早强(C3S),二钙晚强(C2S),铝钙快凝(C3A),铁钙耐磨(C4AF)"。' WHERE subject_id=@s AND level=4 AND name LIKE '%矿物组成%';
UPDATE knowledge_nodes SET content='# 水泥的技术性质\n\n【一句话定义】细度:水泥颗粒粗细程度(比表面积≥300m²/kg或80μm筛余≤10%)→细度越大水化越快早期强度越高但成本增加。凝结时间:初凝≥45min(保证有足够施工时间)/终凝≤600min(10h/尽快硬化)。体积安定性:水泥硬化后体积变化是否均匀→不良(游离CaO/MgO过量→后期膨胀开裂/雷氏夹法和沸煮法检验)。强度等级:按3d和28d抗压抗折强度划分(42.5/52.5/62.5等)。\n\n【考点提示·笔试】必考:四大技术性质(细度/凝结时间/安定性/强度)的指标要求。' WHERE subject_id=@s AND level=4 AND name LIKE '%技术性质%细度%凝结%强度%';
UPDATE knowledge_nodes SET content='# 水泥的选用原则\n\n【一句话定义】六大通用水泥选用:硅酸盐水泥(早期强度高/适合冬季施工和预应力混凝土)、普通水泥(最常用/适合一般工程)、矿渣水泥(耐硫酸盐/适合地下和水工)、火山灰水泥(抗渗性好/适合水工大体积)、粉煤灰水泥(水化热低/适合大体积混凝土)、复合水泥(掺两种以上混合材)。选水泥三原则:工程要求(强度等级/抗渗/抗冻等)→施工条件(工期/气候)→经济合理。\n\n【考点提示·笔试】简答:给出工程场景→选择合适水泥品种并说明理由。' WHERE subject_id=@s AND level=4 AND name LIKE '%选用%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='普通混凝土组成');
UPDATE knowledge_nodes SET content='# 粗细骨料的质量要求\n\n【一句话定义】粗骨料(石子/粒径>4.75mm):最大粒径(≤构件截面最小尺寸1/4/≤钢筋净距3/4/≤板厚1/2)→颗粒级配(连续级配好/空隙率低省水泥)、含泥量(≤1%高强度混凝土→影响骨料与水泥浆粘结强度)、针片状颗粒含量(≤15%→降低流动性增加空隙率)。细骨料(砂/粒径<4.75mm):细度模数(粗砂3.7~3.1/中砂3.0~2.3/细砂2.2~1.6)→中砂最优、含泥量(一般≤3%)。\n\n【考点提示·笔试】选择:石子最大粒径的限值规定。砂的细度模数分类。' WHERE subject_id=@s AND level=4 AND name LIKE '%骨料%';
UPDATE knowledge_nodes SET content='# 混凝土拌合物和易性及其影响因素\n\n【一句话定义】和易性(工作性):混凝土拌合物易于施工(搅拌/运输/浇筑/振捣)并获得均匀密实混凝土的性能。包含三方面:流动性(坍落度/维勃稠度衡量)、粘聚性(不产生分层离析)、保水性(不产生泌水)。影响因素:单位用水量(最主要/用水量↑流动性↑但强度↓)、水灰比(过小干涩过大离析)、砂率(最佳砂率使骨料总表面积和空隙率均衡)、水泥品种和外加剂(减水剂可显著改善)。\n\n【考点提示·笔试】简答高频:和易性三方面含义和主要影响因素。' WHERE subject_id=@s AND level=4 AND name LIKE '%和易性%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='混凝土配合比设计');
UPDATE knowledge_nodes SET content='# 水灰比对混凝土强度的影响\n\n【一句话定义】鲍罗米公式:fcu,28=αa·fce(W/C-αb)(碎石混凝土:αa=0.53/αb=0.20)。水灰比W/C越小→混凝土强度越高(W/C每减少0.05/28d强度约增5~8MPa)。但W/C过小(如<0.35)→拌合物干涩难以密实→反而降低强度→存在最优W/C。工程中W/C一般在0.40~0.65之间。水灰比还影响耐久性(低W/C→密实→抗渗抗冻好)。\n\n【考点提示·笔试】必考:水灰比与强度的关系(反比)。鲍罗米公式的计算应用。' WHERE subject_id=@s AND level=4 AND name LIKE '%水灰比%';
UPDATE knowledge_nodes SET content='# 配合比设计三参数\n\n【一句话定义】混凝土配合比设计的三个基本参数:水灰比W/C(由强度和耐久性要求确定)→用水量W(由坍落度和骨料种类确定)→砂率Sp(由水灰比和骨料情况确定)。设计步骤:确定配制强度(fcu,o=fcu,k+1.645σ)→确定水灰比→确定用水量→计算水泥用量(C=W/(W/C))→确定砂率→用质量法或体积法计算砂石用量→试配调整。\n\n【考点提示·笔试】必考简答:配合比三参数和设计步骤。计算:给设计强度→求配制强度→确定W/C→计算各材料用量。' WHERE subject_id=@s AND level=4 AND name LIKE '%配合比%参数%';

-- 第4章 建筑钢材与功能材料
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='建筑钢材与功能材料');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='建筑钢材');
UPDATE knowledge_nodes SET content='# 钢材的拉伸性能\n\n【一句话定义】低碳钢拉伸应力-应变曲线四个阶段:弹性阶段(σ=Eε/卸载后恢复)→屈服阶段(应力不增加应变继续→σs下屈服强度为设计取值标准)→强化阶段(应力随应变增加→σb抗拉强度)→颈缩阶段(局部截面缩小→断裂)。屈强比σs/σb:反映钢材利用率(0.6~0.75适当,太高脆性破坏无预兆)。断后伸长率δ:反映塑性变形能力(δ>16%为塑性材料)。\n\n【考点提示·笔试】必考简答:拉伸曲线四阶段+三指标(屈服/抗拉/伸长率)。' WHERE subject_id=@s AND level=4 AND name LIKE '%拉伸%屈服%抗拉%伸长率%';
UPDATE knowledge_nodes SET content='# HPB300/HRB400钢筋的牌号含义\n\n【一句话定义】HPB300:热轧(Hot rolled)光圆(Plain)钢筋(Bars)/屈服强度标准值≥300MPa。HRB400:热轧带肋(Ribbed)钢筋/屈服强度≥400MPa(三级钢/最常用)。HRB500:屈服强度≥500MPa(四级钢/高强钢筋)。HPB300(光圆/表面光滑→与混凝土粘结力低→主要用作箍筋和分布筋)。HRB400(带肋/月牙肋→与混凝土粘结力强→主要用作受力主筋)。\n\n【考点提示·笔试】必考选择:各牌号含义。HPB300与HRB400的区别(光圆vs带肋/强度/用途)。' WHERE subject_id=@s AND level=4 AND name LIKE '%HPB%HRB%牌号%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='防水材料');
UPDATE knowledge_nodes SET content='# SBS/APP改性沥青防水卷材\n\n【一句话定义】SBS改性:掺入SBS(苯乙烯-丁二烯-苯乙烯热塑性弹性体)→改善低温柔性(可达-25℃)/弹性好→适合寒冷地区屋面。APP改性:掺入APP(无规聚丙烯)→改善耐高温性能(可达130℃)/抗老化好→适合炎热地区屋面。施工方法:热熔法(用喷灯加热卷材底面熔融后铺贴)/冷粘法(专用粘结剂)。\n\n【考点提示·笔试】辨析:SBS(低温/弹性)vs APP(高温/耐老化)的选用。' WHERE subject_id=@s AND level=4 AND name LIKE '%SBS%APP%改性沥青%防水%';
UPDATE knowledge_nodes SET content='# 防水涂料与密封材料\n\n【一句话定义】防水涂料:聚氨酯防水涂料(反应固化型/弹性好粘结强→地下室/卫生间)、聚合物水泥防水涂料(JS涂料/水泥+聚合物乳液→无毒环保/适合室内)、丙烯酸防水涂料(水性环保/适合外露屋面)。密封材料:硅酮密封胶(耐候/粘结好→幕墙接缝)、聚硫密封胶(耐油→机场跑道)、丙烯酸密封胶(水性→室内接缝)。\n\n【考点提示·笔试】选择:不同部位防水材料选型(屋面/SBS卷材;卫生间/聚氨酯或JS涂料)。' WHERE subject_id=@s AND level=4 AND name LIKE '%防水涂料%密封%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='保温隔热材料');
UPDATE knowledge_nodes SET content='# 导热系数与热阻\n\n【一句话定义】导热系数λ(W/(m·K)):材料传导热量的能力/λ越小保温性越好/保温材料λ≤0.23。热阻R(m²·K/W)=材料厚度d÷λ/R越大保温越好。多层材料总热阻R总=R1+R2+...+Rn(叠加原理)。影响λ因素:表观密度(ρ0小→孔隙多→λ小)/含水率(水λ大→含水↑→保温↓)/温度。\n\n【考点提示·笔试】计算:给厚度和导热系数→求热阻。保温材料选择标准(λ≤0.23)。' WHERE subject_id=@s AND level=4 AND name LIKE '%导热系数%热阻%';
UPDATE knowledge_nodes SET content='# 常用保温材料\n\n【一句话定义】模塑聚苯板(EPS):聚苯乙烯珠粒加热预发泡后在模具中成型→导热系数0.035~0.041/质轻易加工→外墙外保温和屋面保温。挤塑聚苯板(XPS):连续挤出成型→闭孔率高/λ=0.028~0.033/抗压高吸水率极低→适合地下室外墙保温和倒置式屋面。岩棉板:玄武岩高温熔融甩丝→A级不燃/λ=0.036~0.041/吸声好→防火要求高部位。玻璃棉:玻璃熔融离心成纤维→λ=0.033~0.042/A级不燃→钢结构厂房屋面墙面保温。\n\n【考点提示·笔试】辨析:有机保温(EPS/XPS)与无机保温(岩棉/玻璃棉)的优缺点。' WHERE subject_id=@s AND level=4 AND name LIKE '%保温材料%聚苯板%岩棉%玻璃棉%';

SELECT 'v111: 建筑材料 content注入完成' AS result;
