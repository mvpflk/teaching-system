-- ============================================================
-- v109: 植物保护技术 content注入 (20个知识点)
-- ============================================================
SET @s=34; SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=@s AND level=1 LIMIT 1);

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='植物病害识别与防治');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='病害类型与症状');
UPDATE knowledge_nodes SET content='# 侵染性病害与非侵染性病害的区别\n\n【一句话定义】侵染性病害由病原物(真菌/细菌/病毒/线虫)引起→具有传染性→田间呈点片发生逐渐蔓延→有病征(霉层/粉状物/菌脓等)。非侵染性病害由不良环境(温度/水分/养分/药害)引起→不传染→田间呈均匀分布→无病征。两者常相互关联:非侵染病害造成的伤口和组织衰弱为病原物侵入创造条件。\n\n【具体例子】水稻缺钾→叶片赤枯(非侵染)→植株衰弱→易感染胡麻斑病(侵染继发)。果树冻害(非侵染)→树皮开裂→腐烂病菌侵入(侵染继发)。\n\n【考点提示·笔试】辨析高频:侵染性vs非侵染性病害的三大区别(传染性/分布特点/有无病征)。' WHERE subject_id=@s AND level=4 AND name LIKE '%侵染%非侵染%';
UPDATE knowledge_nodes SET content='# 真菌/细菌/病毒病害的症状识别\n\n【一句话定义】真菌病害:病斑上有霉层/粉状物/锈状物/小黑点等病症→如小麦锈病(锈色粉状)、稻瘟病(梭形病斑灰绿色霉层)。细菌病害:水渍状斑点→菌脓(潮湿时病部溢出黄色粘液)→如水稻白叶枯病(叶缘灰白色枯死/菌脓)、白菜软腐病(组织腐烂恶臭)。病毒病害:花叶(深浅绿相间)/黄化/皱缩/矮化→无霉层菌脓→如烟草花叶病毒(TMV)/番茄黄化曲叶病毒(TYLCV)。\n\n【考点提示·笔试】必考:给出病害症状描述→判断属于真菌/细菌/病毒病害。' WHERE subject_id=@s AND level=4 AND name LIKE '%真菌%细菌%病毒%症状%';
UPDATE knowledge_nodes SET content='# 病害侵染循环与病害三角\n\n【一句话定义】侵染循环:越冬越夏→初次侵染→再次侵染→越冬越夏。病害三角:感病寄主+致病病原物+适宜环境→三者缺一不可，降雨和湿度是影响大多数病害流行的最关键因子。切断侵染循环任一环节(清除病残体/种子消毒/轮作)都能有效控制病害。\n\n【考点提示·笔试】简答高频:病害流行三要素及其在防治中的应用。' WHERE subject_id=@s AND level=4 AND name LIKE '%侵染循环%病害三角%流行%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='主要作物病害');
UPDATE knowledge_nodes SET content='# 水稻纹枯病/稻瘟病的识别与防治\n\n【一句话定义】纹枯病(真菌):叶鞘水渍状暗绿斑→云纹状→白色菌丝→褐色菌核。稻瘟病(真菌):苗瘟/叶瘟(梭形病斑黄晕灰白心)/穗颈瘟(穗颈褐色坏死→白穗损失最大)。防治:抗病品种/控氮增钾/井冈霉素(纹枯)/三环唑或稻瘟灵(稻瘟)。\n\n【考点提示·笔试】简答:水稻三大病害各属哪类(稻瘟=真菌/纹枯=真菌/白叶枯=细菌)及防治要点。' WHERE subject_id=@s AND level=4 AND name LIKE '%纹枯病%稻瘟病%';
UPDATE knowledge_nodes SET content='# 小麦锈病/赤霉病的识别与防治\n\n【一句话定义】锈病:条锈(鲜黄虚线状)/叶锈(橘红散生)/秆锈(深褐大斑)。口诀:条锈成行、叶锈乱、秆锈大红斑。赤霉病(真菌):穗部粉红霉层→籽粒皱缩含毒素DON→人畜中毒。防治:抗病品种/花期喷戊唑醇或氰烯菌酯。\n\n【考点提示·笔试】辨析:三种锈病症状区别。判断:赤霉病病粒不能食用(√)。' WHERE subject_id=@s AND level=4 AND name LIKE '%锈病%赤霉病%';
UPDATE knowledge_nodes SET content='# 油菜菌核病/棉花枯萎病的识别\n\n【一句话定义】菌核病(真菌):茎基部水渍状→白色菌丝→茎内黑色鼠粪状菌核→枯死。花期花瓣脱落粘附→病菌侵入关键期。枯萎病(真菌):维管束病害→半边枯死→茎剖面维管束变褐→土传难根除。\n\n【考点提示·笔试】简答:油菜菌核病的典型症状和防治关键期(花期)。' WHERE subject_id=@s AND level=4 AND name LIKE '%菌核病%枯萎病%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='病害发生规律');
UPDATE knowledge_nodes SET content='# 病害侵染循环各阶段\n\n【一句话定义】越冬越夏场所:病残体/种子/土壤/转主寄主。切断任一环节可有效控病:清除病残体→消灭越冬菌源、种子消毒→防止种传病害、轮作→消灭土壤病原。病害三角指导综合防治:培育抗病品种(改寄主)/种子消毒轮作(减病原)/调节播期密度(改环境)。\n\n【考点提示·笔试】简答:以水稻稻瘟病为例分析病害三角。' WHERE subject_id=@s AND level=4 AND name LIKE '%侵染循环%病害三角%';
UPDATE knowledge_nodes SET content='# 病害流行条件与预测预报\n\n【一句话定义】流行条件=病害三角+足够时间。预测预报依据:田间菌源量(孢子捕捉/病株率)+天气预报(降雨温度)+寄主生育期→预测病害趋势→指导提前防治。如稻瘟病预报/小麦条锈病预报→指导农户在发病前喷药保护。\n\n【考点提示·笔试】简答:病害预测预报的依据(菌源+气象+寄主)。' WHERE subject_id=@s AND level=4 AND name LIKE '%流行条件%预测预报%';

-- 第2章 虫害
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='植物虫害识别与防治');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='害虫口器类型与为害');
UPDATE knowledge_nodes SET content='# 咀嚼式口器害虫\n\n【一句话定义】以上颚咬食植物组织→缺刻/孔洞/钻蛀/潜叶。代表:鳞翅目幼虫(螟虫/棉铃虫)、鞘翅目(金龟子/叶甲)、直翅目(蝗虫/蝼蛄)。防治需胃毒剂或触杀剂(药剂喷植物表面→害虫取食摄入)。\n\n【考点提示·笔试】选择:给定害虫名称→判断口器类型。' WHERE subject_id=@s AND level=4 AND name LIKE '%咀嚼%';
UPDATE knowledge_nodes SET content='# 刺吸式口器害虫\n\n【一句话定义】口针束刺入组织吸食汁液→褪绿斑点/黄化/卷叶/萎蔫→传播病毒病→排泄蜜露诱发煤污病。代表:蚜虫/飞虱/叶蝉/蝽象/介壳虫。防治需内吸性杀虫剂(植物吸收后随汁液传导→害虫取食中毒)。\n\n【考点提示·笔试】辨析:咀嚼式vs刺吸式的为害症状和药剂选择差异。' WHERE subject_id=@s AND level=4 AND name LIKE '%刺吸%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='主要作物害虫');
UPDATE knowledge_nodes SET content='# 水稻螟虫\n\n【一句话定义】二化螟:幼虫淡褐背部5纵线→钻茎→枯心苗/白穗。三化螟:幼虫黄白→专食水稻。稻纵卷叶螟:幼虫黄绿→吐丝纵卷稻叶→取食叶肉留白下表皮。防治:齐泥割稻消灭越冬虫/灌水灭蛹/卵孵盛期施杀虫双或阿维菌素。\n\n【考点提示·笔试】简答:水稻螟虫为害特点和防治关键期。' WHERE subject_id=@s AND level=4 AND name LIKE '%螟虫%';
UPDATE knowledge_nodes SET content='# 小麦蚜虫与粘虫\n\n【一句话定义】蚜虫:群集吸汁→发黄卷缩→蜜露煤污→传毒(BYDV)。穗期为害直接影响粒重。粘虫:暴食性→咬食叶片仅剩主脉→大发生吃光整块麦田→具迁飞习性。蚜虫防治指标百株>500头(抗蚜威/吡虫啉)；粘虫每平方米>10头(高效氯氰菊酯)。\n\n【考点提示·笔试】选择:小麦穗蚜防治指标。' WHERE subject_id=@s AND level=4 AND name LIKE '%蚜虫%粘虫%';
UPDATE knowledge_nodes SET content='# 玉米螟的为害特点与防治\n\n【一句话定义】幼虫取食心叶→展开后排孔状→钻茎和雌穗→茎折/穗烂。防治关键期:大喇叭口期(心叶末期)→Bt颗粒剂或辛硫磷颗粒丢心。可释放赤眼蜂生物防治。\n\n【考点提示·笔试】简答:玉米螟防治关键期和常用方法。' WHERE subject_id=@s AND level=4 AND name LIKE '%玉米螟%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='害虫发生规律');
UPDATE knowledge_nodes SET content='# 害虫生活史与防治适期\n\n【一句话定义】生活史:卵→幼虫→蛹→成虫为一个世代。代数因种类和地区而异(二化螟长江2~3代/华南4~5代)。越冬虫态:二化螟幼虫在稻桩稻草中/玉米螟老熟幼虫在玉米秆中。迁飞习性:褐飞虱/粘虫春季从南方随气流北迁→需根据虫情预报及时防治。\n\n【考点提示·笔试】简答:害虫生活史与防治适期的关系(选最薄弱虫态施药)。' WHERE subject_id=@s AND level=4 AND name LIKE '%生活史%';
UPDATE knowledge_nodes SET content='# 害虫发生与环境条件\n\n【一句话定义】影响因子:温度(适温发育快/低温杀死越冬虫源)、湿度(多数需一定湿度/干旱年份蚜虫红蜘蛛重)、食料(寄主种类和生育期)、天敌(寄生蜂/瓢虫等自然控害)。耕作制度改变(单作→复种提高→食料连续→害虫加重)。\n\n【考点提示·笔试】简答:分析害虫暴发原因(从温/湿/食料/天敌四角度)。' WHERE subject_id=@s AND level=4 AND name LIKE '%环境条件%';

-- 第3章 农药
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='农药使用与综合防治');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农药基本知识');
UPDATE knowledge_nodes SET content='# 农药分类(杀虫剂/杀菌剂/除草剂/杀鼠剂)\n\n【一句话定义】杀虫剂:有机磷(敌敌畏/毒死蜱)、拟除虫菊酯(高效氯氰菊酯)、新烟碱(吡虫啉)、生物源(阿维菌素/Bt)。杀菌剂:保护性(代森锰锌/百菌清)、内吸性(三环唑/多菌灵)。除草剂:选择性(精喹禾灵)、灭生性(草甘膦)。杀鼠剂:溴敌隆/敌鼠钠盐。\n\n【考点提示·笔试】必考:给农药名→判断类别。至少每类记2~3个。' WHERE subject_id=@s AND level=4 AND name LIKE '%农药分类%';
UPDATE knowledge_nodes SET content='# 农药剂型(乳油/可湿性粉剂/悬浮剂/颗粒剂)\n\n【一句话定义】乳油(EC):原药+溶剂+乳化剂→兑水成乳白液。可湿性粉剂(WP):原药+填充剂+润湿剂→兑水成悬浮液需搅拌。悬浮剂(SC):原药微粒悬浮水中→不易沉淀。颗粒剂(GR):直接撒施→玉米螟防治丢心。水分散粒剂(WG):遇水崩解→使用方便粉尘少。\n\n【考点提示·笔试】选择:各剂型特点配对。' WHERE subject_id=@s AND level=4 AND name LIKE '%剂型%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农药安全使用');
UPDATE knowledge_nodes SET content='# 农药稀释倍数与用药量计算\n\n【一句话定义】倍数法:原药(mL)=配制药液量÷稀释倍数。如配1000倍液50L:50000÷1000=50mL。ppm换算:1ppm=1mg/L。如配50ppm液100mL需原药5mg。\n\n【具体例子】15L喷雾器配1500倍→10mL原药。20L配2000倍→10mL原药。\n\n【考点提示·笔试】必考计算:给倍数和配量→求原药用量。' WHERE subject_id=@s AND level=4 AND name LIKE '%稀释倍数%用药量%';
UPDATE knowledge_nodes SET content='# 安全间隔期与最高残留限量(MRL)\n\n【一句话定义】安全间隔期:最后一次施药到收获的间隔天数。不同农药/作物间隔期不同(毒死蜱水稻14d/蔬菜21d)。MRL:农产品中农药残留法定最高允许浓度(mg/kg)。超MRL→不合格/禁售/危害健康。出口农产品MRL更严格→需提前停药或选短间隔药剂。\n\n【考点提示·笔试】判断:收获前1天施药符合安全间隔(×)。' WHERE subject_id=@s AND level=4 AND name LIKE '%安全间隔%残留%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='综合防治策略');
UPDATE knowledge_nodes SET content='# 农业防治与物理防治\n\n【一句话定义】农业防治:抗病虫品种/轮作/合理施肥(控N增PK)/适时播种/清洁田园/冬季灌水深耕。物理防治:灯光诱杀(黑光灯/频振灯诱趋光性害虫)/色板诱杀(黄板诱蚜虫/蓝板诱蓟马)/防虫网/糖醋液诱地老虎。\n\n【考点提示·笔试】简答:至少各写3种农防和物防措施。' WHERE subject_id=@s AND level=4 AND name LIKE '%农业防治%物理防治%';
UPDATE knowledge_nodes SET content='# 生物防治(天敌/微生物/性诱剂)\n\n【一句话定义】天敌昆虫:寄生性(赤眼蜂寄生玉米螟卵)、捕食性(瓢虫捕蚜)。微生物农药:Bt(苏云金芽孢杆菌→鳞翅目幼虫中毒)、白僵菌/绿僵菌(真菌寄生)、NPV(核型多角体病毒)。性信息素:合成雌虫性信息素诱杀雄虫或迷向干扰交配(梨小食心虫/斜纹夜蛾)。\n\n【考点提示·笔试】简答:生物防治3种方法和代表例子。' WHERE subject_id=@s AND level=4 AND name LIKE '%生物防治%';
UPDATE knowledge_nodes SET content='# IPM综合防治体系\n\n【一句话定义】IPM(有害生物综合治理):协调农业/物理/生物/化学方法,将害虫密度控制在经济阈值以下,非彻底消灭。原则:监测→确定防治指标→优先非化学措施→必要时精准用选择性农药(对天敌安全)。\n\n【考点提示·笔试】简答:IPM概念/原则/实施步骤。辨析:IPM≠不用农药。' WHERE subject_id=@s AND level=4 AND name LIKE '%IPM%综合防治%';

SELECT 'v109: 植物保护技术 content注入完成' AS result;
