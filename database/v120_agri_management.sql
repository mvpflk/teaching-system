-- v120: 农业经营与管理 第二版 刘强/乔永信 13章 传统章-节结构
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES (NULL, 39, 1, '农业经营与管理[职高]', 1, 'ACTIVE');
SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=39 AND level=1 LIMIT 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r,39,2,'第1章 农业概述',1,'ACTIVE'),(@r,39,2,'第4章 现代农业经营方式',2,'ACTIVE'),(@r,39,2,'第5章 现代农业生产模式',3,'ACTIVE'),(@r,39,2,'第7章 农业生产资源的合理配置',4,'ACTIVE'),(@r,39,2,'第9章 农产品质量管理',5,'ACTIVE'),(@r,39,2,'第10章 农业经营效益管理',6,'ACTIVE'),(@r,39,2,'第11章 农产品市场分析',7,'ACTIVE'),(@r,39,2,'第13章 农产品营销',8,'ACTIVE');

-- Ch1:2节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农业概述');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 农业的含义/发展与作用',1,'ACTIVE'),(@p,39,3,'第二节 农业生产的本质/特点与概况',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%含义%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'农业的概念(狭义种植业/广义农林牧渔)与国民经济地位 [掌握]',1,'ACTIVE'),(@t,39,4,'农业发展阶段:原始农业→传统农业→现代农业 [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%本质%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'农业生产特点:地域性/季节性/周期性/生物性(自然+经济再生产) [掌握]',1,'ACTIVE'),(@t,39,4,'我国农业资源概况(土地/水/气候/生物资源) [了解]',2,'ACTIVE');

-- Ch4:2节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%现代农业经营方式');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 现阶段我国农业经营的主要形式',1,'ACTIVE'),(@p,39,3,'第二节 我国农业经营方式的发展',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%主要形式%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'农户家庭经营型(主体形式/优势灵活/局限规模小) [掌握]',1,'ACTIVE'),(@t,39,4,'联合-协作经营型/股份合作经营型/现代股份公司型/农科工贸一体化型 [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%发展%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'农业经营规模化/集约化/企业化/产业化/社会化/专业化的含义 [掌握]',1,'ACTIVE'),(@t,39,4,'新型主体:专业大户/家庭农场/农民合作社(一人一票/盈余返还)/龙头企业 [掌握]',2,'ACTIVE');

-- Ch5:6节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%现代农业生产模式');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 立体农业',1,'ACTIVE'),(@p,39,3,'第二节 生态农业',2,'ACTIVE'),(@p,39,3,'第三节 设施农业',3,'ACTIVE'),(@p,39,3,'第四节 观光农业',4,'ACTIVE'),(@p,39,3,'第五节 都市农业',5,'ACTIVE'),(@p,39,3,'第六节 农业科技园区',6,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%立体%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'立体农业概念/类型(间套作/稻鱼共生/林下经济)与综合效益 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%生态%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'生态农业概念/原理/十大典型模式(北方四位一体/南方猪沼果等) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%设施%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'设施农业类型(温室/大棚/地膜覆盖)与优势(反季节/高产/高效) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%观光%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'观光农业概念/类型(采摘园/农庄/花卉观赏)与经营要点 [了解]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%都市%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'都市农业概念/功能(生产/生态/生活)与特点 [了解]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%科技园区%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业科技园区功能(示范/培训/孵化/观光)与发展模式 [了解]',1,'ACTIVE');

-- Ch7:5节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%生产资源的合理配置');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 生产资源配置',1,'ACTIVE'),(@p,39,3,'第二节 土地资源开发利用',2,'ACTIVE'),(@p,39,3,'第三节 劳动力资源开发利用',3,'ACTIVE'),(@p,39,3,'第四节 农业资金管理',4,'ACTIVE'),(@p,39,3,'第五节 农业科技资源开发利用',5,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%资源配置%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业生产资源分类(自然资源/经济资源)与配置原则(因地制宜/效益优先) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%土地%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'土地资源特性(不可替代/面积有限/位置固定)与合理利用原则 [掌握]',1,'ACTIVE'),(@t,39,4,'耕地保护制度:18亿亩红线/占补平衡/基本农田保护 [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%劳动力%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业劳动力特点(季节性/分散性/技能性)与合理利用途径(培训/转移/兼业) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%资金管理%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业资金分类(固定/流动)与来源(自有/借贷/财政/项目) [掌握]',1,'ACTIVE'),(@t,39,4,'成本核算:总成本=固定成本+可变成本/单位成本=总成本÷产量 [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%科技资源%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业科技进步内容(良种/栽培/施肥/植保/机械化/信息化)与贡献率 [了解]',1,'ACTIVE');

-- Ch9:3节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农产品质量管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 质量管理含义和特点',1,'ACTIVE'),(@p,39,3,'第二节 管理和控制方法',2,'ACTIVE'),(@p,39,3,'第三节 相关法律法规',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%含义%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农产品质量(外观/营养/安全/加工品质)与质量管理特点 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%管理和控制%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'三品一标:无公害/绿色(A级AA级)/有机农产品+地理标志 [掌握]',1,'ACTIVE'),(@t,39,4,'HACCP体系原理(危害分析→关键控制点)与追溯体系简介 [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%法规%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'《农产品质量安全法》/《食品安全法》农产品生产相关规定 [掌握]',1,'ACTIVE');

-- Ch10:3节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农业经营效益管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 成本与效益',1,'ACTIVE'),(@p,39,3,'第二节 农业经济核算',2,'ACTIVE'),(@p,39,3,'第三节 农业经济效益评价',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%成本%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农产品成本构成(物质/人工/土地/服务费用)/经济效益=总产出-总投入 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%经济核算%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'核算指标:产量/产值/成本/利润/土地产出率/劳动生产率 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%效益评价%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'投入产出比(产出÷投入)/投资回收期/盈亏平衡点计算 [掌握]',1,'ACTIVE'),(@t,39,4,'提高效益途径:增产/提质/降本/拓销/政策补贴 [掌握]',2,'ACTIVE');

-- Ch11:4节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农产品市场分析');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 农产品市场',1,'ACTIVE'),(@p,39,3,'第二节 需求与供给',2,'ACTIVE'),(@p,39,3,'第三节 市场调查概述',3,'ACTIVE'),(@p,39,3,'第四节 市场预测概述',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%农产品市场%' AND name NOT LIKE '%需求%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农产品市场分类(批发/零售/期货)与特点(季节性/分散性/风险性) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%需求%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农产品需求价格弹性/收入弹性与供给影响因素 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%市场调查%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'市场调查内容(需求/供给/环境)与方法(问卷/访谈/观察/资料分析) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%市场预测%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'定性预测(专家意见/销售人员综合)与定量预测(移动平均/回归分析)简介 [了解]',1,'ACTIVE');

-- Ch13:2节
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农产品营销');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 销售渠道/销售组织与电子商务',1,'ACTIVE'),(@p,39,3,'第二节 产品的促销',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%销售渠道%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农产品传统销售渠道(生产者→批发→零售→消费者)与电商直销(淘宝/京东/拼多多/抖音) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%促销%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'4P营销组合:产品/价格/渠道/促销 [掌握]',1,'ACTIVE'),(@t,39,4,'农产品品牌建设(命名/注册/包装)与促销方式(广告/展销/试吃/直播) [了解]',2,'ACTIVE');

INSERT INTO exam_syllabus (subject_id, exam_type, knowledge_dim, title, content, version, status, created_at, updated_at) VALUES
(39,'DUIKOU','BOTH','农业经营与管理考纲(2023版)','# 农业经营与管理 教材:刘强/乔永信第二版 ISBN 978-7-04-021115-3 分值约60分(30%)\n8章(第1/4/5/7/9/10/11/13章)\n\n第1章:农业含义分类/基础地位/发展史/生产特点/资源概况\n第4章:家庭经营/联合协作/股份合作/公司制/农科工贸一体化/规模化集约化产业化/新型主体(家庭农场合作社龙头企业)\n第5章:立体农业/生态农业十大模式/设施农业/观光/都市农业/科技园区\n第7章:资源分类/土地资源保护(18亿亩占补平衡)/劳动力/资金管理(固定流动/成本核算)/科技资源\n第9章:质量含义/三品一标(无公害/绿色/有机/地标)/HACCP追溯/质量安全法\n第10章:成本效益/经济核算指标/投入产出比/投资回收期/盈亏平衡/提高效益途径\n第11章:市场分类/需求弹性/供给因素/市场调查方法/市场预测\n第13章:销售渠道/电商/4P营销/品牌建设/促销方式','2023',1,NOW(),NOW());
INSERT INTO exam_syllabus_node_relation (syllabus_id, node_id) SELECT es.id, kn.id FROM exam_syllabus es JOIN knowledge_nodes kn ON es.subject_id=kn.subject_id AND kn.level=1 WHERE es.subject_id=39;
SELECT CONCAT('农业经营与管理: ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=39 AND level=2),'章 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=39 AND level=3),'节 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=39 AND level=4),'知识点') AS result;
