-- ============================================================================
-- v160: English[职高] 语言应用/完形填空 — 填补题型空白
-- 新增 L2: 语言应用 (sort=7), 2个L4 + 2篇文章
-- ============================================================================
SET NAMES utf8mb4;
SET @eng_root = 12;

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT @eng_root, 24, 2, '语言应用', 7, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_nodes WHERE subject_id=24 AND name='语言应用' AND level=2);

SET @l2 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='语言应用' AND level=2 LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
VALUES (@l2, 24, 3, '语言应用', 1, 'ACTIVE');

SET @l3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l2 AND name='语言应用' AND level=3 LIMIT 1);

-- L4: 标识牌/广告/票务
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3, 24, 4, '标识牌广告票务 [基础]',
'【一句话定义】语言应用题型中考查标识牌、广告、票务等真实语篇的阅读理解能力。占15分（10题×1.5分）。

【具体说明】标识牌: 公共场所标识（Exit/Pull/Push/No Smoking/Wet Paint）、公共信息（Opening hours/Admission free）。
广告: 招聘广告（Wanted/Full-time/Part-time）、促销广告（50% off/Buy one get one free）、活动宣传（Concert/Film/Sports event）。
票务: 电影票/车票/门票信息（Date/Duration/Seat/Price/Fare）。

【常见错误】①标识牌语境判断错误（Push/Pull方向搞混）；②广告中折扣计算错误（50% off = 半价）；③票务信息提取遗漏（时间/座位/价格三要素缺一）。
【考试方向】语言应用题通常前4-5题。2024年出现标识牌2题+广告2题+票务1题。',
1, 'ACTIVE');

-- L4: 图表/日程/菜单
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3, 24, 4, '图表日程菜单 [中等]',
'【一句话定义】语言应用中涉及图表信息提取、日程安排理解和菜单阅读的题型。

【具体说明】图表: 柱状图/折线图/饼图的数据读取（增长/下降/比例→find/rate/percentage）、表格信息对比（Comparison/Feature）。
日程: 课程表（Timetable/Mon-Fri/Period）、活动安排（Agenda/Schedule）、节目表（TV guide/Program）。
菜单与指南: 餐厅菜单（Starter/Main course/Dessert/Price）、使用说明（Instruction/Directions）、药品说明（Dosage/3 times a day）。

【常见错误】①图表题只读数字不看单位（time/hours/days混淆）；②日程题忽略时间顺序（before/after/at搞混）；③菜单阅读不分餐别（starter/main/dessert）。
【考试方向】语言应用中后5-6题。2024年出现图表2题+日程2题+菜单指南1-2题。',
2, 'ACTIVE');

-- 2篇知识文章
SET @l4_1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l3 AND name='标识牌广告票务 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语言应用', '语言应用', '标识牌广告票务',
'### 概述\n\n语言应用是第I卷的第二大题（10题×1.5分=15分），考查真实语篇的理解和信息提取能力。与普通阅读理解不同，语言应用的语篇更短、更生活化，包括标识牌、广告、票务、菜单、图表等。\n\n### 标识牌（Signs）\n\n公共场所最常见的语言应用形式。\n\n| 标识牌 | 含义 | 常考场景 |\n|--------|------|----------|\n| Exit / Entrance | 出口/入口 | 商场/地铁 |\n| Pull / Push | 拉/推 | 门 |\n| No Smoking / No Parking | 禁止吸烟/停车 | 公共场所 |\n| Wet Paint | 油漆未干 | 公园/施工 |\n| Out of Order | 故障/暂停使用 | 电梯/厕所 |\n| Admission Free | 免费入场 | 博物馆/展览 |\n| Opening Hours: 9:00-17:00 | 营业时间 | 商店/景点 |\n\n### 广告（Advertisements）\n\n招聘广告、促销广告、活动宣传是常考类型。\n\n- **招聘广告关键词**：Wanted（招聘）、Full-time/Part-time（全职/兼职）、Experience required（有经验者优先）、Salary/Pay（薪资）、Contact（联系方式）\n- **促销广告关键词**：50% off（五折）、Buy one get one free（买一送一）、Discount（折扣）、Special offer（特价）、While stocks last（售完即止）\n- **活动宣传**：Concert/Competition/Festival（音乐会/比赛/节日）、Date & Time、Venue（地点）、Ticket price\n\n### 票务（Tickets）\n\n电影票、车票、景点门票含大量考点信息。\n\n| 票务信息 | 英文表达 |\n|----------|----------|\n| 日期 | Date: June 15, 2026 |\n| 时间 | Time: 7:30 pm |\n| 座位 | Seat: Row 8, Seat 12 |\n| 票价 | Price: ¥80 / Adult: ¥50 / Child: ¥25 |\n| 有效期 | Valid until / Expiry date |\n\n### 要点总结\n\n1. 标识牌题：从语境中判断含义，不要逐字翻译（如 Wet Paint = 油漆未干 ≠ 湿油漆）\n2. 广告题：先读问题再找信息，注意折扣计算（50% off = 原价一半）\n3. 票务题：时间/价格/地点三要素缺一不可，注意区分日期和时间格式',
'标识牌(Exit/Pull/No Smoking)、广告(Wanted/50% off)、票务(Date/Time/Price)。三大语言应用题型，占15分。从语境判断含义，先读问题再找信息。',
@l4_1, 1, '["基础","语言应用","标识","广告","票务"]', 'PUBLISHED', NOW(), NOW());

SET @l4_2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l3 AND name='图表日程菜单 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语言应用', '语言应用', '图表日程菜单',
'### 概述\n\n图表、日程、菜单是语言应用中较难的题型。它们不仅考查词汇量，还考查信息提取、数据比较和时间顺序理解能力。\n\n### 图表（Charts & Tables）\n\n常考柱状图(bar chart)、折线图(line graph)、饼图(pie chart)和表格(table)。\n\n- **柱状图/折线图**：关注趋势（increase/rise/grow/go up vs decrease/fall/drop/go down）、最高值（the highest/the most）、最低值（the lowest/the least）\n- **饼图**：关注比例（percentage/rate/proportion）、最大占比（the largest share）和比较（more than/less than/twice as many as）\n- **表格**：关注行/列标题、对比不同项的相同点和差异\n\n### 日程（Schedules）\n\n课程表、活动安排、节目表、时间表。核心是**时间顺序**。\n\n- **课程表**：Subject（科目）、Period（节次）、From...to（时间范围）、Room（教室）\n- **活动安排**：Activity（活动）、Time（时间）、Location（地点）、Speaker/Host（主讲人）\n- **电视节目表**：Channel（频道）、Program（节目）、Type（类型：News/Drama/Sports/Movie）\n\n常见考题：What time does the first class begin? / Which activity is at 2 pm? / How long does the program last?\n\n### 菜单与指南（Menus & Guides）\n\n- **餐厅菜单结构**：Starter（前菜）→ Main course（主菜）→ Dessert（甜点）→ Drink（饮品）\n- **药品说明**：Dosage（用量）、Take 3 times a day（每日3次）、After meals（饭后）、Side effects（副作用）\n- **使用说明**：Instructions/Directions、Step 1/2/3（步骤）、Warning（警告）、Keep away from children（远离儿童）\n\n### 要点总结\n\n1. 图表题：先读标题和轴标注（X轴/Y轴各代表什么），再看单位（数字+单位都有用）\n2. 日程题：注意时间介词（at 2 pm / on Monday / in the morning / from...to）\n3. 菜单题：分清三道菜的英文名称和对应的价格信息',
'图表(bar/pie/table)、日程(timetable/agenda)、菜单(starter/main/dessert)。信息提取与数据比较。注意时间介词和单位。',
@l4_2, 2, '["中等","语言应用","图表","日程","菜单"]', 'PUBLISHED', NOW(), NOW());

SELECT 'v160: 语言应用 created (2 L4 + 2 articles)' AS result;
