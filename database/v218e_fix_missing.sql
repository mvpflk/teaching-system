SET NAMES utf8mb4;
START TRANSACTION;

-- 定语从句 (3 nodes)
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='关系代词 that/which/who [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The man ___ is standing there is my uncle.', '[{"key":"A","text":"which"},{"key":"B","text":"who"},{"key":"C","text":"what"},{"key":"D","text":"where"}]', 'B', '先行词the man指人，从句缺主语，用who/that。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'This is the book ___ I bought yesterday.', '[{"key":"A","text":"who"},{"key":"B","text":"which"},{"key":"C","text":"what"},{"key":"D","text":"where"}]', 'B', '先行词book指物，从句缺宾语，用which/that。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Everything ___ he said is true.', '[{"key":"A","text":"which"},{"key":"B","text":"what"},{"key":"C","text":"that"},{"key":"D","text":"who"}]', 'C', '先行词是不定代词everything时只能用that。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'I like the house ___ has a big garden.', NULL, 'which/that', '先行词house指物，从句缺主语，用which或that。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She is the kindest person ___ I know.', '[{"key":"A","text":"which"},{"key":"B","text":"that"},{"key":"C","text":"what"},{"key":"D","text":"whom"}]', 'B', '先行词被最高级修饰时只能用that。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='关系副词 when/where/why [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'This is the school ___ I studied three years ago.', '[{"key":"A","text":"which"},{"key":"B","text":"where"},{"key":"C","text":"when"},{"key":"D","text":"that"}]', 'B', '先行词school表地点，从句主谓宾完整缺状语，用where。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I will never forget the day ___ we first met.', '[{"key":"A","text":"which"},{"key":"B","text":"when"},{"key":"C","text":"where"},{"key":"D","text":"that"}]', 'B', '先行词day表时间，从句不缺成分，用when。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'This is the reason ___ he was late.', NULL, 'why', '先行词reason，从句完整，用why引导定语从句。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='非限制性定语从句 [困难]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'My mother, ___ is a teacher, helps me with homework.', '[{"key":"A","text":"that"},{"key":"B","text":"which"},{"key":"C","text":"who"},{"key":"D","text":"what"}]', 'C', '非限制性定语从句(有逗号)不能用that，指人用who。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Beijing, ___ is the capital of China, is beautiful.', '[{"key":"A","text":"that"},{"key":"B","text":"which"},{"key":"C","text":"what"},{"key":"D","text":"where"}]', 'B', '非限制性定语从句指物用which，不能用that。', 3, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'My father, that is 45, works in a factory. (判断正误)', NULL, 'F', '非限制性定语从句不能用that，应改为who。', 3, 1);

-- 名词性从句 (3 nodes)
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='宾语从句 [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I wonder ___ he will come tomorrow.', '[{"key":"A","text":"that"},{"key":"B","text":"whether"},{"key":"C","text":"what"},{"key":"D","text":"which"}]', 'B', 'wonder表示想知道，后接whether/if。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She said ___ she was very busy.', '[{"key":"A","text":"what"},{"key":"B","text":"that"},{"key":"C","text":"which"},{"key":"D","text":"if"}]', 'B', '陈述句作宾语从句用that引导，that可省略。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'Can you tell me ___ the post office is?', NULL, 'where', '宾语从句用陈述语序。疑问词保留但语序正常。', 2, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'He asked me where did I live. (判断正误)', NULL, 'F', '宾语从句必须用陈述语序。正确:where I lived。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='主语从句 [困难]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '___ he said is true.', '[{"key":"A","text":"That"},{"key":"B","text":"Which"},{"key":"C","text":"What"},{"key":"D","text":"Whether"}]', 'C', 'what引导主语从句并在从句中作宾语。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ we need more time is obvious.', '[{"key":"A","text":"What"},{"key":"B","text":"That"},{"key":"C","text":"Which"},{"key":"D","text":"Whether"}]', 'B', '从句完整不缺成分，用That引导主语从句。', 3, 1),
('英语[职高]', @n, 'FILL_IN', '___ she will come is not sure yet.', NULL, 'Whether', 'whether引导主语从句。注意if不能引导主语从句。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='表语从句与同位语从句 [困难]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The problem is ___ we lack money.', '[{"key":"A","text":"what"},{"key":"B","text":"that"},{"key":"C","text":"which"},{"key":"D","text":"whether"}]', 'B', '表语从句完整用that引导。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The news ___ our team won excited us.', '[{"key":"A","text":"which"},{"key":"B","text":"that"},{"key":"C","text":"what"},{"key":"D","text":"whether"}]', 'B', '同位语从句解释news的内容，从句完整用that。', 3, 1);

-- 状语从句续 (2 nodes)
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='时间/条件/原因状语从句 [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I will call you ___ I arrive home.', '[{"key":"A","text":"until"},{"key":"B","text":"as soon as"},{"key":"C","text":"while"},{"key":"D","text":"since"}]', 'B', 'as soon as=一...就... 时间状语从句用一般现在时表将来。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ it rains tomorrow, we will stay at home.', '[{"key":"A","text":"Because"},{"key":"B","text":"If"},{"key":"C","text":"When"},{"key":"D","text":"After"}]', 'B', 'if引导条件状语从句，用一般现在时表将来。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He didn''t come ___ he was ill.', NULL, 'because', 'because引导原因状语从句。不能与so同时用。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'You won''t pass ___ you study hard.', NULL, 'unless', 'unless=if not除非。条件从句一般现在时表将来。', 2, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'If it will rain tomorrow, we will stay home. (判断正误)', NULL, 'F', '条件状语从句不能用将来时。正确:If it rains。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='让步/目的状语从句 [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He got up early ___ he could catch the bus.', '[{"key":"A","text":"because"},{"key":"B","text":"so that"},{"key":"C","text":"although"},{"key":"D","text":"unless"}]', 'B', 'so that引导目的状语从句，从句常用can/could。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ the weather was bad, they went out.', '[{"key":"A","text":"Because"},{"key":"B","text":"Even though"},{"key":"C","text":"If"},{"key":"D","text":"So that"}]', 'B', 'Even though=即使，引导让步状语从句。', 3, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'Although he is poor, but he is happy. (判断正误)', NULL, 'F', 'although和but不能同时用。去掉其中之一。', 3, 1);

-- 动名词补充
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='动名词' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Would you mind ___ the window?', '[{"key":"A","text":"open"},{"key":"B","text":"to open"},{"key":"C","text":"opening"},{"key":"D","text":"opened"}]', 'C', 'mind doing sth介意做某事，固定搭配。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ is good for your health.', '[{"key":"A","text":"Run"},{"key":"B","text":"Running"},{"key":"C","text":"To run"},{"key":"D","text":"Ran"}]', 'B', '动名词作主语表示泛指的动作。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'I look forward to ___ (see) you.', NULL, 'seeing', 'look forward to中的to是介词，后接doing。', 3, 1);

-- 情态动词补 (2 nodes)
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词基本用法 can/must/may [基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'You ___ smoke here. It is not allowed.', '[{"key":"A","text":"can"},{"key":"B","text":"mustn''t"},{"key":"C","text":"may"},{"key":"D","text":"needn''t"}]', 'B', 'mustn''t表示禁止。not allowed提示禁止。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ I borrow your pen?', '[{"key":"A","text":"Must"},{"key":"B","text":"Should"},{"key":"C","text":"May"},{"key":"D","text":"Need"}]', 'C', 'May I...?礼貌请求允许。Can I也常用。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '---Must I finish it today? ---No, you ___.', '[{"key":"A","text":"mustn''t"},{"key":"B","text":"needn''t"},{"key":"C","text":"can''t"},{"key":"D","text":"shouldn''t"}]', 'B', 'Must提问否定用needn''t/don''t have to。mustn''t是禁止。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ play piano when he was five.', NULL, 'could', 'can的过去式could表示过去的能力。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词推测用法 [中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He ___ be at home. I just saw him outside.', '[{"key":"A","text":"must"},{"key":"B","text":"can''t"},{"key":"C","text":"may"},{"key":"D","text":"should"}]', 'B', 'can''t表示否定推测(不可能)。刚在外面看见他。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The ground is wet. It ___ last night.', '[{"key":"A","text":"must rain"},{"key":"B","text":"must have rained"},{"key":"C","text":"can rain"},{"key":"D","text":"should rain"}]', 'B', 'must have done对过去的肯定推测(一定做了)。', 3, 1);

-- 主谓一致补
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='就近原则与意义一致 [基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Neither he nor I ___ a teacher.', '[{"key":"A","text":"is"},{"key":"B","text":"am"},{"key":"C","text":"are"},{"key":"D","text":"be"}]', 'B', 'neither...nor就近原则，与I一致用am。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'There ___ a book and two pens on the desk.', '[{"key":"A","text":"is"},{"key":"B","text":"are"},{"key":"C","text":"has"},{"key":"D","text":"have"}]', 'A', 'there be就近原则，a book是单数用is。', 2, 1);

COMMIT;
