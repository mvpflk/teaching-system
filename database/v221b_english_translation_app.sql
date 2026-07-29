-- ============================================================================
-- V221b: 英语剩余节点 — 翻译4 + 语言应用2 = 6节点
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- 英译汉 (2 nodes)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='关键词语准确理解 [基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '"He is interested in music." 的正确翻译是:', '[{"key":"A","text":"他对音乐感兴趣"},{"key":"B","text":"他很有趣"},{"key":"C","text":"音乐对他感兴趣"},{"key":"D","text":"他喜欢运动"}]', 'A', 'be interested in=对...感兴趣。关键词interested是"感兴趣"不是"有趣"。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '"She takes after her mother." 的正确翻译是:', '[{"key":"A","text":"她跟在她妈妈后面"},{"key":"B","text":"她长得像她妈妈"},{"key":"C","text":"她带走了她妈妈"},{"key":"D","text":"她照顾她妈妈"}]', 'B', 'take after=长得像/性格像。不能字面翻译为"跟在后面"。', 2, 1),
('英语[职高]', @n, 'FILL_IN', '"You should take it easy." 译为:你___。', NULL, '别紧张/放轻松', 'take it easy=放轻松别紧张。固定词组不能逐字翻译。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '"The meeting was put off." 的正确翻译是:', '[{"key":"A","text":"会议被推迟了"},{"key":"B","text":"会议被取消了"},{"key":"C","text":"会议开始了"},{"key":"D","text":"会议结束了"}]', 'A', 'put off=推迟。cancel取消，put on穿上。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='句式结构分析 [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '"It is important that we learn English well." 的正确译文是:', '[{"key":"A","text":"重要的是我们要学好英语"},{"key":"B","text":"它很重要我们学英语"},{"key":"C","text":"我们学英语很重要"},{"key":"D","text":"英语很重要"}]', 'A', 'It is+adj+that从句:It是形式主语，真正主语是that从句。翻译时先译从句再译形容词。', 2, 1),
('英语[职高]', @n, 'FILL_IN', '"He didn''t leave until his mother came back." 译为:___。', NULL, '直到他妈妈回来他才离开', 'not...until...句型:直到...才...。否定转移，汉语用肯定表达。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '下列哪个句子应该用"倒译法"(从后往前译)?', '[{"key":"A","text":"I like apples"},{"key":"B","text":"Here comes the bus"},{"key":"C","text":"She is a teacher"},{"key":"D","text":"He runs fast"}]', 'B', 'Here comes the bus是倒装句，翻译时应恢复为正常语序"公交车来了"。', 3, 1);

-- ==================================================================
-- 汉译英 (2 nodes)
-- ==================================================================
SET @n2 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='基础词汇运用 [基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n2, 'SINGLE_CHOICE', '"我每天六点起床"的正确翻译是:', '[{"key":"A","text":"I at six get up every day"},{"key":"B","text":"I get up at six every day"},{"key":"C","text":"I every day at six get up"},{"key":"D","text":"I get up every day at six"}]', 'B', '英语语序:主语+谓语+时间状语。时间通常放在句尾。', 1, 1),
('英语[职高]', @n2, 'FILL_IN', '"她擅长唱歌" 译为:She is good at ___.', NULL, 'singing', 'be good at+doing。介词at后接动名词。sing→singing。', 2, 1),
('英语[职高]', @n2, 'SINGLE_CHOICE', '"我们学校有2000名学生"的正确翻译是:', '[{"key":"A","text":"Our school have 2000 students"},{"key":"B","text":"There are 2000 students in our school"},{"key":"C","text":"Our school has 2000 student"},{"key":"D","text":"There is 2000 students"}]', 'B', 'there be句型表示"某处有"。students复数用are。', 2, 1);

SET @n3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='基本句式构建 [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n3, 'SINGLE_CHOICE', '"我认为你是对的"应译为:', '[{"key":"A","text":"I think you are right"},{"key":"B","text":"I think you right"},{"key":"C","text":"I am think you are right"},{"key":"D","text":"You are right I think"}]', 'A', 'I think+宾语从句。主谓宾结构，从句用陈述语序。', 2, 1),
('英语[职高]', @n3, 'FILL_IN', '"他太年轻了不能开车" 译为:He is ___ young ___ drive.', NULL, 'too...to...', 'too+adj+to do结构。太...而不能...。', 2, 1),
('英语[职高]', @n3, 'SINGLE_CHOICE', '"我一到家就给你打电话"的正确翻译是:', '[{"key":"A","text":"I call you as soon as I get home"},{"key":"B","text":"I will call you as soon as I will get home"},{"key":"C","text":"I will call you as soon as I get home"},{"key":"D","text":"I call you when I will get home"}]', 'C', 'as soon as引导时间状语从句，主将从现:主句将来时will call,从句现在时get。', 3, 1);

-- ==================================================================
-- 语言应用 (2 nodes)
-- ==================================================================
SET @n4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='标识牌广告票务 [基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n4, 'SINGLE_CHOICE', 'What does the sign "NO SMOKING" mean?', '[{"key":"A","text":"你可以抽烟"},{"key":"B","text":"禁止吸烟"},{"key":"C","text":"请少抽烟"},{"key":"D","text":"吸烟区"}]', 'B', 'NO+动名词表示禁止。NO SMOKING=禁止吸烟。类似:NO PARKING禁止停车。', 1, 1),
('英语[职高]', @n4, 'SINGLE_CHOICE', '"EXIT" on a sign means ___.', '[{"key":"A","text":"入口"},{"key":"B","text":"出口"},{"key":"C","text":"厕所"},{"key":"D","text":"电梯"}]', 'B', 'EXIT=出口。ENTRANCE=入口。TOILET=厕所。', 1, 1),
('英语[职高]', @n4, 'FILL_IN', '"Staff Only" means ___ (闲人免进/仅限员工).', NULL, '仅限员工', 'Staff=员工,Only=仅。标识牌常用:仅限员工/闲人免进。', 1, 1),
('英语[职高]', @n4, 'SINGLE_CHOICE', 'At a train station, "Platform 3" means ___.', '[{"key":"A","text":"3号站台"},{"key":"B","text":"3楼"},{"key":"C","text":"3号出口"},{"key":"D","text":"3号售票窗口"}]', 'A', 'Platform=站台。train station火车站的标识。', 1, 1);

SET @n5 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='图表日程菜单 [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n5, 'SINGLE_CHOICE', 'Look at the menu:\n\nMain Course:\n- Beef Steak  ￥45\n- Fried Chicken  ￥28\n- Fish and Chips  ￥32\n\nHow much is the cheapest main course?', '[{"key":"A","text":"45 yuan"},{"key":"B","text":"28 yuan"},{"key":"C","text":"32 yuan"},{"key":"D","text":"105 yuan"}]', 'B', '阅读菜单找最低价:Fried Chicken 28元最便宜。cheapest=最便宜的。', 2, 1),
('英语[职高]', @n5, 'SINGLE_CHOICE', 'School Timetable:\n\n8:00-8:45  Math\n8:55-9:40  English\n10:00-10:45  P.E.\n\nHow long is the break after English class?', '[{"key":"A","text":"10 minutes"},{"key":"B","text":"15 minutes"},{"key":"C","text":"20 minutes"},{"key":"D","text":"5 minutes"}]', 'C', '英语课9:40结束，体育课10:00开始，中间休息20分钟。', 2, 1),
('英语[职高]', @n5, 'FILL_IN', 'The bus ___ (时刻表) shows the departure and arrival times.', NULL, 'schedule/timetable', 'schedule/timetable=时刻表。常见于车站机场。', 2, 1);

COMMIT;
