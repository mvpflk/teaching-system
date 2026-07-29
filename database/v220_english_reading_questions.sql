-- ============================================================================
-- V220: 英语[职高] 阅读理解题库 P2 — 5篇短文 × 6题 = 30题
-- 每篇短文覆盖2-3个阅读技能节点
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- Passage 1: 日常生活 — 覆盖 直接定位/Wh-细节/词义猜测
-- ==================================================================

SET @n1 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='直接定位文中信息 [基础]' LIMIT 1);
SET @n2 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='Wh-问题细节检索 [基础]' LIMIT 1);
SET @n3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='根据上下文猜测生词含义 [基础]' LIMIT 1);

INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n1, 'SINGLE_CHOICE', '阅读短文回答问题:\n\nTom is a 16-year-old high school student. Every morning, he gets up at 6:30 and takes the bus to school. He usually has bread and milk for breakfast. After school, he plays basketball with his friends for an hour. In the evening, he does his homework from 7:00 to 9:00. He goes to bed at 10:00.\n\nWhat time does Tom get up?', '[{"key":"A","text":"6:00"},{"key":"B","text":"6:30"},{"key":"C","text":"7:00"},{"key":"D","text":"7:30"}]', 'B', '文中第一句明确说He gets up at 6:30。直接定位文中信息即可找到答案。', 1, 1),
('英语[职高]', @n2, 'SINGLE_CHOICE', '(同短文) How does Tom go to school?', '[{"key":"A","text":"On foot"},{"key":"B","text":"By bike"},{"key":"C","text":"By bus"},{"key":"D","text":"By car"}]', 'C', '文中takes the bus to school=乘公交上学。How提问交通方式。', 1, 1),
('英语[职高]', @n1, 'SINGLE_CHOICE', '(同短文) What does Tom do after school?', '[{"key":"A","text":"Does homework"},{"key":"B","text":"Plays basketball"},{"key":"C","text":"Watches TV"},{"key":"D","text":"Reads books"}]', 'B', '文中After school, he plays basketball。直接定位。', 1, 1),
('英语[职高]', @n3, 'SINGLE_CHOICE', '(同短文) The word "usually" in the text means ___.', '[{"key":"A","text":"从不"},{"key":"B","text":"有时"},{"key":"C","text":"通常"},{"key":"D","text":"总是"}]', 'C', 'usually=通常。根据上下文描述他每天的习惯，可知usually表示大多数情况下如此。', 1, 1),
('英语[职高]', @n2, 'FILL_IN', '(同短文) How long does Tom do his homework in the evening? ___ hours.', NULL, '2', 'from 7:00 to 9:00=两小时。Wh-细节检索:找时间段的起止点。', 1, 1);

-- ==================================================================
-- Passage 2: 学校生活 — 覆盖 推断/主旨/细节进阶
-- ==================================================================

SET @n4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='根据上下文推断隐含意思 [中等]' LIMIT 1);
SET @n5 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='概括段落/全文大意 [中等]' LIMIT 1);
SET @n6 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='细节理解进阶训练 [中等]' LIMIT 1);

INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n5, 'SINGLE_CHOICE', '阅读短文回答问题:\n\nLi Hua is a new student at No.1 Vocational School. At first, she felt very lonely because she had no friends. But she didn''t give up. She joined the school basketball club and made many new friends there. Now she loves her school life and studies hard every day. She wants to become a nurse in the future.\n\nWhat is the main idea of this passage?', '[{"key":"A","text":"Li Hua wants to be a doctor"},{"key":"B","text":"Li Hua overcame loneliness by joining a club and now enjoys school"},{"key":"C","text":"Basketball is the most popular sport"},{"key":"D","text":"No.1 Vocational School is very big"}]', 'B', '全文讲Li Hua从孤独到通过参加社团融入校园的过程。A错(护士不是医生)，C和D文中未提及。', 2, 1),
('英语[职高]', @n4, 'SINGLE_CHOICE', '(同短文) From the passage, we can infer that Li Hua is ___.', '[{"key":"A","text":"lazy"},{"key":"B","text":"active and determined"},{"key":"C","text":"shy and quiet"},{"key":"D","text":"angry"}]', 'B', '推断题:她主动参加社团(active)，没有放弃(determined)。这些需要从行为推断而非直接找到。', 2, 1),
('英语[职高]', @n6, 'SINGLE_CHOICE', '(同短文) What club did Li Hua join?', '[{"key":"A","text":"Football club"},{"key":"B","text":"Music club"},{"key":"C","text":"Basketball club"},{"key":"D","text":"Art club"}]', 'C', '细节定位:文中明确说She joined the school basketball club。', 1, 1),
('英语[职高]', @n4, 'FILL_IN', '(同短文) At first, Li Hua felt ___ because she had no friends.', NULL, 'lonely', '推断+细节:文中at first she felt very lonely。需要从文中提取具体形容词。', 2, 1),
('英语[职高]', @n5, 'TRUE_FALSE', '(同短文) The passage mainly talks about how to play basketball. (判断正误)', NULL, 'F', '主旨判断:文章主题是Li Hua的校园适应经历，不是篮球技巧。', 2, 1);

-- ==================================================================
-- Passage 3: 说明文 — 覆盖 主旨进阶/推断综合/细节
-- ==================================================================

SET @n7 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='主旨大意进阶训练 [困难]' LIMIT 1);
SET @n8 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='推断题技巧综合 [困难]' LIMIT 1);

INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n7, 'SINGLE_CHOICE', '阅读短文回答问题:\n\nBreakfast is the most important meal of the day. Studies show that students who eat breakfast perform better in school. They have more energy and can focus better in class. However, many teenagers skip breakfast because they want to sleep longer. Doctors suggest that a good breakfast should include bread, milk, eggs and some fruit. If you don''t have time in the morning, you can prepare your breakfast the night before.\n\nThe best title for this passage is ___.', '[{"key":"A","text":"How to Cook Eggs"},{"key":"B","text":"The Importance of Breakfast"},{"key":"C","text":"Why Students Are Tired"},{"key":"D","text":"Different Kinds of Food"}]', 'B', '主旨题:全文围绕早餐的重要性展开。A偏题(只提了鸡蛋)，C偏题，D范围太大。', 2, 1),
('英语[职高]', @n8, 'SINGLE_CHOICE', '(同短文) We can infer from the passage that ___.', '[{"key":"A","text":"all students eat breakfast every day"},{"key":"B","text":"skipping breakfast may affect school performance"},{"key":"C","text":"doctors don''t care about breakfast"},{"key":"D","text":"breakfast is the least important meal"}]', 'B', '推断:从studies show students who eat breakfast perform better可推断不吃早餐会影响学习。', 3, 1),
('英语[职高]', @n7, 'SINGLE_CHOICE', '(同短文) According to doctors, a good breakfast should NOT include ___.', '[{"key":"A","text":"bread"},{"key":"B","text":"eggs"},{"key":"C","text":"cola"},{"key":"D","text":"fruit"}]', 'C', '细节:医生建议的早餐包含bread/milk/eggs/fruit，不包含碳酸饮料。', 2, 1),
('英语[职高]', @n8, 'TRUE_FALSE', '(同短文) The writer thinks breakfast is unimportant. (判断正误)', NULL, 'F', '推断:作者从开头就强调breakfast is the most important meal，显然认为早餐很重要。', 2, 1);

-- ==================================================================
-- Passage 4: 环保话题 — 覆盖 词义猜测/细节/主旨
-- ==================================================================

SET @n9 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='题型判别与策略选择 [掌握]' LIMIT 1);

INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n3, 'SINGLE_CHOICE', '阅读短文回答问题:\n\nMore and more people are using reusable bags instead of plastic ones. Plastic bags are harmful to the environment because they take hundreds of years to break down. They often end up in oceans and hurt sea animals. Many countries have already banned free plastic bags in supermarkets. Using reusable bags is a small step, but it can make a big difference.\n\nThe word "banned" in the passage means ___.', '[{"key":"A","text":"allowed"},{"key":"B","text":"encouraged"},{"key":"C","text":"forbidden"},{"key":"D","text":"produced"}]', 'C', 'banned=禁止。从上下文(harmful/hurt animals)可推断禁止塑料袋是环保措施。', 2, 1),
('英语[职高]', @n6, 'SINGLE_CHOICE', '(同短文) Why are plastic bags harmful to the environment?', '[{"key":"A","text":"They are too expensive"},{"key":"B","text":"They take hundreds of years to break down"},{"key":"C","text":"They are very heavy"},{"key":"D","text":"They look ugly"}]', 'B', '细节题:文中because they take hundreds of years to break down。', 1, 1),
('英语[职高]', @n5, 'SINGLE_CHOICE', '(同短文) The writer''s purpose is to ___.', '[{"key":"A","text":"sell reusable bags"},{"key":"B","text":"encourage people to use reusable bags"},{"key":"C","text":"describe how to make plastic bags"},{"key":"D","text":"explain why shopping is fun"}]', 'B', '主旨/写作目的:文章旨在鼓励人们用环保袋替代塑料袋。', 2, 1),
('英语[职高]', @n9, 'SINGLE_CHOICE', 'This question type "The word X means..." is a ___.', '[{"key":"A","text":"主旨大意题"},{"key":"B","text":"词义猜测题"},{"key":"C","text":"细节理解题"},{"key":"D","text":"推理判断题"}]', 'B', '题型判别:考查生词含义属于词义猜测题。解题策略:根据上下文语境推断词义。', 2, 1),
('英语[职高]', @n9, 'SINGLE_CHOICE', 'When answering a "main idea" question, you should ___.', '[{"key":"A","text":"read only the first sentence"},{"key":"B","text":"look for the sentence that best summarizes the whole passage"},{"key":"C","text":"guess randomly"},{"key":"D","text":"focus on one detail"}]', 'B', '策略选择:主旨题需要在通读全文后找到最能概括全文的句子。', 2, 1);

-- ==================================================================
-- Passage 5: 科技 — 覆盖 细节/推断/时间策略
-- ==================================================================

SET @n10 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='时间分配与答题顺序 [掌握]' LIMIT 1);

INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n1, 'SINGLE_CHOICE', '阅读短文回答问题:\n\nSmartphones have changed our lives in many ways. We use them to communicate, take photos, shop online and even study. However, spending too much time on smartphones can cause problems. It may hurt our eyes and affect our sleep. Experts suggest that teenagers should spend no more than two hours a day on their phones. The key is to use smartphones wisely, not to give them up completely.\n\nAccording to the passage, smartphones can be used for ___.', '[{"key":"A","text":"only making calls"},{"key":"B","text":"communicating and studying"},{"key":"C","text":"only playing games"},{"key":"D","text":"only taking photos"}]', 'B', '细节定位:文中communicate/take photos/shop online/study都是用途，B最全面。', 1, 1),
('英语[职高]', @n4, 'FILL_IN', '(同短文) Experts advise teenagers to use phones ___ than two hours a day.', NULL, 'less', '推断:no more than two hours=不超过两小时，即少于两小时。', 2, 1),
('英语[职高]', @n7, 'SINGLE_CHOICE', '(同短文) The main idea is that ___.', '[{"key":"A","text":"smartphones are useless"},{"key":"B","text":"we should use smartphones wisely"},{"key":"C","text":"teenagers should not use phones"},{"key":"D","text":"phones are too expensive"}]', 'B', '主旨:文章强调智慧地使用手机(to use wisely)，而非完全放弃。', 2, 1),
('英语[职高]', @n10, 'SINGLE_CHOICE', 'When reading a long passage, you should ___.', '[{"key":"A","text":"read every word slowly"},{"key":"B","text":"read the questions first, then scan for answers"},{"key":"C","text":"skip the passage and guess"},{"key":"D","text":"read only the last paragraph"}]', 'B', '时间分配策略:先读问题→定位关键词→快速扫描找答案，节省时间。', 2, 1),
('英语[职高]', @n10, 'TRUE_FALSE', 'You should spend most of your time on one difficult question. (判断正误)', NULL, 'F', '时间策略:难题先跳过，做完容易题再回头。不能在一道题上花太多时间。', 2, 1),
('英语[职高]', @n6, 'SINGLE_CHOICE', '(同短文) How many hours should teenagers spend on phones per day according to experts?', '[{"key":"A","text":"no more than one"},{"key":"B","text":"no more than two"},{"key":"C","text":"at least three"},{"key":"D","text":"as many as possible"}]', 'B', '细节定位:文中no more than two hours a day。', 1, 1);

COMMIT;
