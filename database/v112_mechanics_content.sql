-- v112: 建筑力学 content注入 (24个知识点)
SET @s=29; SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=@s AND level=1 LIMIT 1);
SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='静力学基础');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='基本概念与公理');
UPDATE knowledge_nodes SET content='# 力的三要素与作用与反作用定律\n\n【一句话定义】力的三要素:大小(N/kN)、方向、作用点。作用与反作用定律:两物体间的作用力与反作用力大小相等/方向相反/沿同一直线/分别作用在两个不同物体上(不可抵消)。注意与二力平衡区别:二力平衡作用在同一物体上。\n\n【考点提示·笔试】辨析高频:作用反作用vs二力平衡(作用对象不同)。' WHERE subject_id=@s AND level=4 AND name LIKE '%力的三要素%';
UPDATE knowledge_nodes SET content='# 二力平衡条件与加减平衡力系公理\n\n【一句话定义】二力平衡:作用于同一刚体上的两个力,大小相等/方向相反/作用在同一直线上→刚体平衡。加减平衡力系公理:加减任意平衡力系不改变原力系对刚体的作用效果→力系简化基础。二力杆:两端铰接/中间不受力/自重不计→两端力必定沿杆轴线等值反向。\n\n【考点提示·笔试】必考:二力杆判断(两端铰接+中间无力+自重不计→力沿杆轴)。' WHERE subject_id=@s AND level=4 AND name LIKE '%二力平衡%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='约束与受力图');
UPDATE knowledge_nodes SET content='# 常见约束类型及约束反力\n\n【一句话定义】柔索约束:只能受拉→约束反力沿柔索方向背离物体。光滑面约束:只能受压→约束反力沿接触面法线指向物体。铰链约束:限制两个方向移动→两个约束分力(X/Y)。固定端约束:限制移动和转动→X/Y/M三个约束反力。辊轴支座:仅限制法向移动→一个约束反力。\n\n【考点提示·笔试】必考:各约束类型反力个数和方向→给结构画受力图。' WHERE subject_id=@s AND level=4 AND name LIKE '%约束%';
UPDATE knowledge_nodes SET content='# 受力图的绘制步骤\n\n【一句话定义】步骤:取隔离体(明确对象)→画主动力(自重/外荷载)→逐处解除约束并代之以约束反力(按约束类型确定方向)→检查。注意:约束反力方向不确定时可先假设,计算结果为正则方向正确,为负则反向。\n\n【考点提示·笔试】必考实操:给结构简图→画受力图。' WHERE subject_id=@s AND level=4 AND name LIKE '%受力图%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='力矩与力偶');
UPDATE knowledge_nodes SET content='# 力矩的计算与平衡条件\n\n【一句话定义】力矩M=±F×d(逆正顺负)。合力矩定理:合力对任一点矩=各分力对该点矩的代数和。力矩平衡:∑M=0。\n\n【考点提示·笔试】计算:给力和力臂→求力矩。' WHERE subject_id=@s AND level=4 AND name LIKE '%力矩%';
UPDATE knowledge_nodes SET content='# 力偶的性质与力偶矩\n\n【一句话定义】力偶:等值/反向/不共线的两力→力偶矩M=±Fd(与矩心无关/可任意搬移)。力偶无合力(不能用一个力等效)/力偶只能用力偶平衡。\n\n【考点提示·笔试】判断:力偶能否用单力平衡(×)/力偶对任一点矩都相等(√)。' WHERE subject_id=@s AND level=4 AND name LIKE '%力偶%';

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='平面力系');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='平面汇交力系');
UPDATE knowledge_nodes SET content='# 力在坐标轴上的投影与合力\n\n【一句话定义】投影:Fx=Fcosα/Fy=Fsinα。合力投影定理:Rx=∑Fx/Ry=∑Fy。合力大小R=√(Rx²+Ry²);方向tanθ=|Ry/Rx|。\n\n【考点提示·笔试】必考计算:给各分力→求投影→求合力。' WHERE subject_id=@s AND level=4 AND name LIKE '%投影%合力%';
UPDATE knowledge_nodes SET content='# 平面汇交力系的平衡方程\n\n【一句话定义】平衡方程:∑Fx=0/∑Fy=0(两个独立方程可解两个未知量)。步骤:取隔离体→画受力图→选坐标轴→列方程→求解。\n\n【考点提示·笔试】必考计算:汇交力系求未知力。' WHERE subject_id=@s AND level=4 AND name LIKE '%汇交%平衡%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='平面一般力系');
UPDATE knowledge_nodes SET content='# 力的平移定理\n\n【一句话定义】力可平移到任一点但须附加一个力偶(力偶矩=原力对新点力矩)。逆:力+力偶=力(平移)。是力系简化的理论基础。\n\n【考点提示·笔试】简答:力的平移定理内容和应用。' WHERE subject_id=@s AND level=4 AND name LIKE '%力的平移%';
UPDATE knowledge_nodes SET content='# 平面一般力系的平衡方程\n\n【一句话定义】三种形式:(1)∑X=0/∑Y=0/∑M=0(基本式)(2)二矩式(AB不垂直于X轴)(3)三矩式(ABC不共线)。三个独立方程可解三个未知量。选矩心原则:尽量选在多个未知力交点以简化计算。\n\n【考点提示·笔试】必考计算:给梁→求支座反力(三方程三未知量)。' WHERE subject_id=@s AND level=4 AND name LIKE '%一般力系%平衡方程%';
UPDATE knowledge_nodes SET content='# 支座反力的计算\n\n【一句话定义】简支梁:一端固定铰支座+一端活动铰支座(共3个未知反力)。悬臂梁:固定端(3反力)→可直接求解。计算步骤:取隔离体→画受力图(均布荷载→集中力作用于中点)→列平衡方程→解反力→校核。\n\n【考点提示·笔试】必考计算:给简支梁/外伸梁受集中力+均布荷载→求支座反力。' WHERE subject_id=@s AND level=4 AND name LIKE '%支座反力%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='平面桁架内力');
UPDATE knowledge_nodes SET content='# 结点法与截面法\n\n【一句话定义】结点法:逐次取结点隔离体(每次≤2个未知力)→列∑X=0/∑Y=0→求解全部杆内力。截面法:用一个截面截断桁架(≤3根未知力杆)→取一侧隔离体→列平衡方程→求解指定杆内力。零杆判断:结点无荷载且两杆不共线→其中一杆为零杆。\n\n【考点提示·笔试】选择:两种方法适用条件对比。判断常见桁架中的零杆。' WHERE subject_id=@s AND level=4 AND name LIKE '%结点法%截面法%';

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='杆件强度与刚度');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='轴向拉压');
UPDATE knowledge_nodes SET content='# 轴力图与正应力计算\n\n【一句话定义】轴力N(拉正压负)。正应力σ=N/A(均匀分布/MPa)。轴力图:表示各截面轴力沿杆长变化。强度条件σmax≤[σ]。\n\n【考点提示·笔试】必考计算:画轴力图→求最大正应力→强度校核。' WHERE subject_id=@s AND level=4 AND name LIKE '%轴力图%正应力%';
UPDATE knowledge_nodes SET content='# 胡克定律与轴向变形计算\n\n【一句话定义】σ=Eε(应力应变成正比/E弹性模量)。ΔL=NL/(EA)(EA为抗拉刚度)。仅在弹性范围(不超过比例极限)内适用。\n\n【考点提示·笔试】必考计算:给N/L/A/E求ΔL。' WHERE subject_id=@s AND level=4 AND name LIKE '%胡克定律%';
UPDATE knowledge_nodes SET content='# 拉压杆的强度条件\n\n【一句话定义】σmax=Nmax/A≤[σ]。[σ]=σu/n(塑性σu=σs/脆性σu=σb)。三类问题:校核强度/选择截面(A≥Nmax/[σ])/确定许可荷载(Nmax≤A[σ])。\n\n【考点提示·笔试】必考三类计算。' WHERE subject_id=@s AND level=4 AND name LIKE '%强度条件%拉压%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='剪切与挤压');
UPDATE knowledge_nodes SET content='# 剪切与挤压的实用计算\n\n【一句话定义】剪切:两横向力作用下两部分沿平行截面错动→τ=Q/Aj≤[τ]。挤压:连接件与被连接件接触面传递压力→σjy=Pjy/Ajy≤[σjy]。Ajy取接触面在挤压力方向的投影面积。\n\n【考点提示·笔试】必考:螺栓/铆钉/键连接的剪切和挤压强度。分清单剪双剪。' WHERE subject_id=@s AND level=4 AND name LIKE '%剪切%挤压%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='弯曲');
UPDATE knowledge_nodes SET content='# 剪力图与弯矩图的绘制\n\n【一句话定义】剪力图(Q)和弯矩图(M):利用微分关系dQ/dx=-q/dM/dx=Q快速作图。规律:无荷载Q=常数M=直线/均布荷载Q=直线M=二次抛物线/Q=0处M取极值/集中力处Q突变M转折/集中力偶处M突变。\n\n【考点提示·笔试】必考实操:给荷载图→快速画出Q图和M图。' WHERE subject_id=@s AND level=4 AND name LIKE '%剪力图%弯矩图%';
UPDATE knowledge_nodes SET content='# 弯曲正应力计算\n\n【一句话定义】纯弯曲:σ=My/Iz(y为点到中性轴距离)。最大正应力σmax=Mmax/Wz。矩形Wz=bh²/6,圆形Wz=πd³/32。强度条件:σmax≤[σ]。\n\n【考点提示·笔试】必考计算:给弯矩/截面→求最大正应力→强度校核。' WHERE subject_id=@s AND level=4 AND name LIKE '%弯曲正应力%';
UPDATE knowledge_nodes SET content='# 梁的弯曲强度条件\n\n【一句话定义】σmax=Mmax/Wz≤[σ]。提高措施:选合理截面(工字钢/箱形优于矩形→增大Wz)/变截面梁/改善受力(增支座)。\n\n【考点提示·笔试】简答:提高弯曲强度的工程措施。' WHERE subject_id=@s AND level=4 AND name LIKE '%弯曲强度%';

SET @ch=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name='压杆稳定');
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='压杆稳定概念');
UPDATE knowledge_nodes SET content='# 压杆失稳的概念\n\n【一句话定义】失稳(屈曲):细长压杆当压力达到临界力Pcr时,突然发生显著弯曲丧失承载能力。失稳时应力可能远低于材料强度(细长杆)。\n\n【考点提示·笔试】辨析:失稳vs强度破坏的区别。' WHERE subject_id=@s AND level=4 AND name LIKE '%失稳%';
UPDATE knowledge_nodes SET content='# 柔度(长细比)的计算\n\n【一句话定义】λ=μL/i。i=√(I/A)为惯性半径。λ越大→越细长→越易失稳。分类:大柔度杆(λ≥λp→欧拉公式)/中柔度杆(λs≤λ<λp→经验)/小柔度杆(λ<λs→强度)。\n\n【考点提示·笔试】必考计算:给条件→求λ→判断类型。' WHERE subject_id=@s AND level=4 AND name LIKE '%柔度%长细比%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='欧拉公式与临界力');
UPDATE knowledge_nodes SET content='# 欧拉临界力公式\n\n【一句话定义】Pcr=π²EI/(μL)²(大柔度杆)。σcr=π²E/λ²。提高途径:增大I(空心管/工字钢)/减小L(加中间撑)/减小μ(加强端部约束)。\n\n【考点提示·笔试】必考计算:给E/I/μ/L→求Pcr。简答:提高稳定性的措施。' WHERE subject_id=@s AND level=4 AND name LIKE '%欧拉公式%临界力%';
UPDATE knowledge_nodes SET content='# 长度系数μ的取值\n\n【一句话定义】μ反映杆端约束:两端铰支μ=1/一端固定一端自由μ=2(最不利)/两端固定μ=0.5(最优)/一端固定一端铰支μ≈0.7。约束越强μ越小→临界力越大。\n\n【考点提示·笔试】必考:给约束→判断μ→代入欧拉公式。' WHERE subject_id=@s AND level=4 AND name LIKE '%长度系数%μ%';
SET @u=(SELECT id FROM knowledge_nodes WHERE parent_id=@ch AND name='提高压杆稳定性的措施');
UPDATE knowledge_nodes SET content='# 提高压杆稳定性的工程措施\n\n【一句话定义】措施:(1)减小杆长(最有效→加中间支撑)(2)加强约束(减小μ→固结代替铰结)(3)选合理截面(空心管>实心圆)(4)用高E材料(钢材E=206GPa)(5)避免压杆(改拉杆)。\n\n【考点提示·笔试】简答高频:提高压杆稳定性的措施。' WHERE subject_id=@s AND level=4 AND name LIKE '%提高%压杆稳定%';

SELECT 'v112: 建筑力学 content注入完成' AS result;
