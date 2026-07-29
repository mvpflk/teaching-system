-- ============================================================
-- v108: 农学科目2/3/4 content注入 (农作物生产技术+植物保护技术+农业生物技术)
-- ============================================================

SET @s=33; SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=@s AND level=1 LIMIT 1);
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='主要粮食作物');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='水稻栽培技术');
UPDATE knowledge_nodes SET content='# 水稻生育期划分与器官建成\n\n【一句话定义】水稻一生分为营养生长期(秧田期+返青期+分蘖期)和生殖生长期(幼穗分化期+抽穗开花期+灌浆成熟期)。器官建成规律:发芽→出叶→分蘖→拔节→幼穗分化→抽穗→开花→灌浆→成熟。亩穗数取决于分蘖期管理,每穗粒数取决于幼穗分化期管理,千粒重取决于灌浆期管理。\n\n【具体例子】四川盆地水稻一般在4月上旬播种育秧、5月中旬移栽、7月下旬~8月上旬抽穗、9月中下旬收获。移栽后7~10天开始分蘖,晒田控制无效分蘖可提高成穗率。\n\n【考点提示·笔试】简答题:水稻产量三要素(穗数/粒数/粒重)的决定时期。' WHERE subject_id=@s AND level=4 AND name LIKE '%水稻生育%';
UPDATE knowledge_nodes SET content='# 水稻育秧技术与合理密植\n\n【一句话定义】育秧方式:湿润育秧(秧板保持湿润/二叶一心后灌水上畦)、旱育秧(全程旱管/秧苗矮壮根系发达)、盘育秧(塑料秧盘/规格化秧块/适合机插)。壮秧标准:叶龄适宜/茎基扁平/白根多/无病虫害。合理密植:杂交稻每亩1.2~1.5万穴(宽行窄株/30cm×16cm)、常规稻每亩1.8~2.5万穴。\n\n【具体例子】机插秧要求秧龄15~20天/苗高12~18cm,盘育秧便于机器取秧。人工插秧的"东西向行,南北向株"有利于通风透光。\n\n【考点提示·笔试】选择/判断题:旱育秧的特点(根系发达/秧苗矮壮/移栽后返青快)。' WHERE subject_id=@s AND level=4 AND name LIKE '%育秧%';
UPDATE knowledge_nodes SET content='# 水稻水肥管理与晒田技术\n\n【一句话定义】水分管理:薄水插秧→浅水活棵→湿润分蘖→够苗晒田→有水孕穗→寸水抽穗→干湿灌浆→断水收获。晒田(烤田)技术:当茎蘖数达到预期穗数80%~90%时开始晒田→控制无效分蘖/促进根系下扎/改善通风透光/减轻病虫害。晒田标准:田面开细裂/白根上翻/叶色转淡。\n\n【具体例子】杂交中稻预期穗数18万/亩,当苗数达到15~16万/亩时开始晒田,晒7~10天后复水。晒田过重影响幼穗分化,过轻起不到控蘖效果。\n\n【考点提示·笔试】简答高频:水稻晒田的时期、目的和标准。' WHERE subject_id=@s AND level=4 AND name LIKE '%水肥%晒田%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='小麦栽培技术');
UPDATE knowledge_nodes SET content='# 小麦播种期与播种量确定\n\n【一句话定义】冬小麦适播期:日均温16~18℃(四川盆地10月下旬~11月上旬)。播种量(kg/亩)=计划基本苗×千粒重÷(100×发芽率×出苗率)。适期播种:基本苗14~18万/亩,播量8~12kg/亩,播深3~5cm。过早→冬前旺长受冻;过晚→冬前分蘖少穗数不足。\n\n【考点提示·笔试】计算题:给定千粒重、发芽率、计划基本苗,求播种量。' WHERE subject_id=@s AND level=4 AND name LIKE '%小麦播种%';
UPDATE knowledge_nodes SET content='# 小麦冬前管理与春季追肥\n\n【一句话定义】冬前管理:促根增蘖、培育壮苗。措施:冬灌(夜冻昼消时/d保证安全越冬)、镇压(压实弥缝保墒)、中耕(破除板结)。春季追肥:返青期尿素8~10kg/亩促春蘖成穗、拔节期5~8kg/亩增穗粒数、孕穗期喷KH2PO4提千粒重。\n\n【考点提示·笔试】简答:小麦冬前管理的主要措施及作用。' WHERE subject_id=@s AND level=4 AND name LIKE '%冬前管理%';
UPDATE knowledge_nodes SET content='# 小麦产量构成因素\n\n【一句话定义】小麦产量=亩穗数×穗粒数×千粒重÷1000。穗数(播种~返青)→茎蘖成穗,粒数(拔节~孕穗)→幼穗分化,粒重(抽穗~成熟)→灌浆充实。三因素互补:穗数多→穗粒数减,粒数多→粒重降。高产需协调三因素最优组合。\n\n【考点提示·笔试】简答:小麦产量三因素的形成时期和特点。' WHERE subject_id=@s AND level=4 AND name LIKE '%产量构成%';

SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='玉米栽培技术');
UPDATE knowledge_nodes SET content='# 玉米需肥规律与穗期管理\n\n【一句话定义】玉米需肥:苗期10%、拔节至大喇叭口期60%以上/关键追肥期、抽雄后减少。大喇叭口期追攻穗肥(尿素15~20kg/亩)→促大穗多粒、培土防倒、防治玉米螟(大喇叭口期是最佳防治时期/用Bt或辛硫磷颗粒丢心)。\n\n【考点提示·笔试】选择/判断:玉米需肥最多的时期是大喇叭口期(√)。玉米螟防治最佳时期是大喇叭口期(√)。' WHERE subject_id=@s AND level=4 AND name LIKE '%需肥%穗期%';
UPDATE knowledge_nodes SET content='# 玉米合理密植与种植方式\n\n【一句话定义】密植原则:紧凑型宜密(4000~5000株/亩)/平展型宜稀(3000~4000株/亩)。种植方式:等行距(行距60~70cm)和宽窄行(宽行80~90cm+窄行40~50cm/适合高产田/改善通风透光便于套种)。\n\n【考点提示·笔试】简答:玉米合理密植的原则(品种/地力/播期决定密度)。' WHERE subject_id=@s AND level=4 AND name LIKE '%密植%';

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='主要经济作物');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='油菜栽培技术');
UPDATE knowledge_nodes SET content='# 油菜育苗移栽与施肥技术\n\n【一句话定义】育苗:苗床播量0.5~0.6kg/亩(供8~10亩大田),苗龄30~35天/5~6叶移栽。移栽密度6000~8000株/亩。施肥:基肥(有机肥+PK+硼砂)、提苗肥(尿素5~8kg)、薹肥(尿素8~10kg)。油菜对硼敏感:缺硼→"花而不实"。基施硼砂0.5~1kg/亩或蕾薹期喷0.2%硼砂。\n\n【考点提示·笔试】简答高频:油菜缺硼的典型症状和防治措施。' WHERE subject_id=@s AND level=4 AND name LIKE '%油菜育苗%';
UPDATE knowledge_nodes SET content='# 油菜硼肥施用与菌核病防治\n\n【一句话定义】硼是油菜最敏感微量元素→缺硼:只开花/角果不发育/结实率极低。硼肥:基施0.5~1kg/亩硼砂或蕾薹期喷0.2%硼砂。菌核病:茎基部水渍状→白色菌丝→黑色菌核。防治:轮作(与禾本科2~3年)/花期喷多菌灵或菌核净。\n\n【考点提示·笔试】选择:油菜"花而不实"的原因是缺硼(√)。' WHERE subject_id=@s AND level=4 AND name LIKE '%硼肥%菌核%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='棉花栽培技术');
UPDATE knowledge_nodes SET content='# 棉花整枝打杈技术\n\n【一句话定义】整枝五项:打顶心(7月中旬/留12~14果枝)/打边心(分批摘果枝尖)/抹赘芽(及时抹除)/打老叶(后期摘下部老叶)/去空枝(剪无蕾铃果枝)。目的:控制株型/调节养分分配/改善通风透光/促棉铃吐絮。\n\n【考点提示·笔试】简答:棉花整枝的五项操作及其作用。' WHERE subject_id=@s AND level=4 AND name LIKE '%整枝%';
UPDATE knowledge_nodes SET content='# 棉花蕾铃脱落原因与防止\n\n【一句话定义】蕾铃脱落率通常60%~70%。原因:生理脱落(光合产物不足)/肥水不当(氮过多旺长)/病虫害/不良环境。防止:合理施肥(控N增PK)/适时整枝/化控(缩节胺或矮壮素减少徒长)/及时防病虫。\n\n【考点提示·笔试】简答:棉花蕾铃脱落的主要原因和防止措施。' WHERE subject_id=@s AND level=4 AND name LIKE '%蕾铃脱落%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='甘薯与马铃薯栽培');
UPDATE knowledge_nodes SET content='# 甘薯育苗与栽插技术\n\n【一句话定义】甘薯温床育苗:种薯斜排→覆土3~5cm→床温25~30℃→约30天出苗→苗高20~25cm剪苗。栽插:垄作(垄距70~80cm)→斜插或船底形插(入土2~3节)→每亩3500~4500株。深栽结薯少而大,浅栽结薯多而小。\n\n【考点提示·笔试】选择:甘薯繁殖方式(无性繁殖/种薯育苗→剪苗栽插不是种子直播)。' WHERE subject_id=@s AND level=4 AND name LIKE '%甘薯%';
UPDATE knowledge_nodes SET content='# 马铃薯切块催芽与培土\n\n【一句话定义】切块:每块30~50g/保留1~2芽眼/切刀消毒(0.1%高锰酸钾或75%酒精)→伤口晾干拌草木灰。催芽:湿沙层积→15~18℃/湿度70%→15~20天芽长1~2cm。培土:齐苗后5~7cm→现蕾期8~10cm→防止块茎见光变绿产生龙葵素(有毒)。\n\n【考点提示·笔试】简答:马铃薯切块注意事项(芽眼/消毒)和培土的作用。' WHERE subject_id=@s AND level=4 AND name LIKE '%马铃薯%';

-- 耕作制度
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='耕作制度');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='种植制度');
UPDATE knowledge_nodes SET content='# 轮作/连作的利弊与茬口安排\n\n【一句话定义】轮作优点:减轻病虫草害(改变生态→打破病虫草生活史)/均衡利用土壤养分/改善土壤理化性质(豆科固氮/深浅根交替)。连作缺点:病虫害逐年加重/养分偏耗/自毒物质积累(西瓜/番茄分泌自毒物质)。水旱轮作(稻-油/稻-麦)→有效减轻土传病害。\n\n【考点提示·笔试】简答:轮作增产的原因(至少写3点)。' WHERE subject_id=@s AND level=4 AND name LIKE '%轮作%连作%';
UPDATE knowledge_nodes SET content='# 间作/套作/混作的区别与模式\n\n【一句话定义】间作(同时分行播种/共生期长→玉米间作大豆)。套作(前作后期播后作/共生期短→小麦套种玉米/麦收前20~30天点播)。混作(不分行混播→小麦豌豆混种)。四川常见模式:麦/玉/豆三熟制。\n\n【考点提示·笔试】辨析高频:间作vs套作的主要区别(共生期长短不同)。' WHERE subject_id=@s AND level=4 AND name LIKE '%间作%套作%混作%';
UPDATE knowledge_nodes SET content='# 复种指数的概念与计算\n\n【一句话定义】复种指数(%)=全年播种总面积÷耕地面积×100%。一年一熟=100%/一年两熟=200%/两年三熟=150%。四川主要农区可达200%~250%。提高途径:选用早熟品种/育苗移栽缩短本田期/地膜覆盖早播早熟。\n\n【考点提示·笔试】必考计算:给定农户耕地面积和各季播种面积→计算复种指数。' WHERE subject_id=@s AND level=4 AND name LIKE '%复种指数%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='养地制度');
UPDATE knowledge_nodes SET content='# 土壤耕作措施(耕/耙/耱/压)\n\n【一句话定义】耕(犁地/翻耕)→翻转土层/掩埋残茬/深20~30cm。耙→破碎土块/平整地面。耱→细碎表土/压实保墒。压→压碎土块/弥合裂缝。播前整地工序:耕翻→耙地→耱地→作畦→播种→镇压。\n\n【考点提示·笔试】选择:四种耕作措施的名称和作用配对。' WHERE subject_id=@s AND level=4 AND name LIKE '%土壤耕作%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='农田培肥');
UPDATE knowledge_nodes SET content='# 有机肥与绿肥的培肥作用\n\n【一句话定义】有机肥培肥机制:增加有机质改善结构/提供缓释养分/增强保水保肥/激活微生物。绿肥独特作用:豆科绿肥(紫云英/苕子/田菁)根瘤固氮→每1000kg鲜草≈5~8kg纯N。长期单施化肥→有机质下降/板结酸化;配合有机肥→维持地力。\n\n【考点提示·笔试】简答:有机肥对土壤的培肥作用(至少写3点)。' WHERE subject_id=@s AND level=4 AND name LIKE '%有机肥%绿肥%培肥%';

SELECT 'v108: 农作物生产技术 content注入完成' AS result;
