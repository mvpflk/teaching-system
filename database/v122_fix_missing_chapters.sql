-- v122: 补全农业经营与管理缺失的5章(第2/3/6/8/12章)
SET @r=(SELECT id FROM knowledge_nodes WHERE subject_id=39 AND level=1 LIMIT 1);

-- 第2章
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r,39,2,'第2章 我国农业和农村经济发展',2,'ACTIVE');
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农业和农村经济发展');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 我国社会主义农业经济的发展历程',1,'ACTIVE'),(@p,39,3,'第二节 新时期我国农业和农村经济发展',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%发展历程%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'土地改革→农业合作化→人民公社→家庭联产承包责任制→市场化改革各阶段特征 [了解]',1,'ACTIVE'),(@t,39,4,'家庭联产承包责任制的历史意义与当前地位 [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%新时期%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'农业供给侧结构性改革的方向与重点任务 [了解]',1,'ACTIVE'),(@t,39,4,'乡村振兴战略(产业兴旺/生态宜居/乡风文明/治理有效/生活富裕) [掌握]',2,'ACTIVE'),(@t,39,4,'城乡融合发展与农业农村现代化的关系 [了解]',3,'ACTIVE');

-- 第3章
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@r,39,2,'第3章 社会主义新农村建设',3,'ACTIVE');
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%新农村建设');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 背景内涵和现实意义',1,'ACTIVE'),(@p,39,3,'第二节 目标内容原则',2,'ACTIVE'),(@p,39,3,'第三节 新农村建设的实践',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%背景%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'社会主义新农村建设的时代背景与核心内涵 [掌握]',1,'ACTIVE'),(@t,39,4,'新农村建设对解决三农问题的现实意义 [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%目标%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'新农村建设目标(生产发展/生活宽裕/乡风文明/村容整洁/管理民主) [掌握]',1,'ACTIVE'),(@t,39,4,'新农村建设基本原则(因地制宜/农民主体/规划先行) [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%实践%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'新农村建设的典型模式与实践案例 [了解]',1,'ACTIVE');

-- 第6章
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@r,39,2,'第6章 农业宏观管理',6,'ACTIVE');
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农业宏观管理');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 农业宏观管理及其职能',1,'ACTIVE'),(@p,39,3,'第二节 管理手段',2,'ACTIVE'),(@p,39,3,'第三节 我国农业行政管理组织',3,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%职能%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业宏观管理的概念与主要职能(计划/组织/指挥/协调/控制) [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%管理手段%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业宏观管理手段:计划/经济(价格税收信贷)/法律/行政手段 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%行政%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'我国农业行政管理组织体系:农业农村部→省农业厅→市农业局→县农业局 [了解]',1,'ACTIVE');

-- 第8章
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@r,39,2,'第8章 农业经济合同的订立变更和终止',8,'ACTIVE');
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%经济合同%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 合同含义及内容',1,'ACTIVE'),(@p,39,3,'第二节 合同签订',2,'ACTIVE'),(@p,39,3,'第三节 履行担保和违约责任',3,'ACTIVE'),(@p,39,3,'第四节 变更转让和终止',4,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%含义%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'农业经济合同的概念/特征与主要内容条款 [掌握]',1,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%签订%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'合同签订程序(要约→承诺)与注意事项 [掌握]',1,'ACTIVE'),(@t,39,4,'常见农业合同类型(土地承包/购销/技术服务/雇佣合同) [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%履行%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'合同履行原则与担保方式(保证/抵押/质押/留置/定金) [掌握]',1,'ACTIVE'),(@t,39,4,'违约责任的承担方式(继续履行/赔偿损失/支付违约金) [掌握]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%变更%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@t,39,4,'合同变更/转让/终止的条件与法律后果 [了解]',1,'ACTIVE');

-- 第12章
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES(@r,39,2,'第12章 农产品开发',12,'ACTIVE');
SET @p=(SELECT id FROM knowledge_nodes WHERE parent_id=@r AND name LIKE '%农产品开发');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@p,39,3,'第一节 开发含义与方法',1,'ACTIVE'),(@p,39,3,'第二节 产品定价',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%含义%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'农产品开发概念与类型(新产品/老产品改进/品牌开发) [掌握]',1,'ACTIVE'),(@t,39,4,'农产品开发基本方法(市场导向/技术驱动/资源利用) [了解]',2,'ACTIVE');
SET @t=(SELECT id FROM knowledge_nodes WHERE parent_id=@p AND name LIKE '%定价%');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@t,39,4,'农产品定价方法:成本导向/竞争导向/需求导向定价 [掌握]',1,'ACTIVE'),(@t,39,4,'农产品定价策略(渗透/撇脂/折扣/心理定价) [了解]',2,'ACTIVE');

-- 修正所有章节sort_order
UPDATE knowledge_nodes SET sort_order=2 WHERE subject_id=39 AND level=2 AND name LIKE '%第2章%';
UPDATE knowledge_nodes SET sort_order=3 WHERE subject_id=39 AND level=2 AND name LIKE '%第3章%';
UPDATE knowledge_nodes SET sort_order=4 WHERE subject_id=39 AND level=2 AND name LIKE '%第4章%';
UPDATE knowledge_nodes SET sort_order=5 WHERE subject_id=39 AND level=2 AND name LIKE '%第5章%';
UPDATE knowledge_nodes SET sort_order=6 WHERE subject_id=39 AND level=2 AND name LIKE '%第6章%';
UPDATE knowledge_nodes SET sort_order=7 WHERE subject_id=39 AND level=2 AND name LIKE '%第7章%';
UPDATE knowledge_nodes SET sort_order=8 WHERE subject_id=39 AND level=2 AND name LIKE '%第8章%';
UPDATE knowledge_nodes SET sort_order=9 WHERE subject_id=39 AND level=2 AND name LIKE '%第9章%';
UPDATE knowledge_nodes SET sort_order=10 WHERE subject_id=39 AND level=2 AND name LIKE '%第10章%';
UPDATE knowledge_nodes SET sort_order=11 WHERE subject_id=39 AND level=2 AND name LIKE '%第11章%';
UPDATE knowledge_nodes SET sort_order=12 WHERE subject_id=39 AND level=2 AND name LIKE '%第12章%';
UPDATE knowledge_nodes SET sort_order=13 WHERE subject_id=39 AND level=2 AND name LIKE '%第13章%';

SELECT CONCAT('农业经营与管理补全: ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=39 AND level=2),'章 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=39 AND level=3),'节 ',(SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=39 AND level=4),'知识点') AS result;
