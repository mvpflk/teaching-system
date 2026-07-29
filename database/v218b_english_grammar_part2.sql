-- ============================================================================
-- V218 Part 2: 从句 + 主谓一致 + 情态动词 + 虚拟语气 + 情景交际
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- 三、定语从句 (3 nodes × 8 = ~24 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='关系代词 that/which/who [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The man ___ is standing over there is my uncle.', '[{"key":"A","text":"which"},{"key":"B","text":"who"},{"key":"C","text":"what"},{"key":"D","text":"where"}]', 'B', '先行词the man指人，在从句中作主语，关系代词用who/that。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'This is the book ___ I bought yesterday.', '[{"key":"A","text":"who"},{"key":"B","text":"whom"},{"key":"C","text":"which"},{"key":"D","text":"where"}]', 'C', '先行词the book指物，在从句中作宾语，关系代词用which/that。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The girl ___ you met yesterday is my classmate.', '[{"key":"A","text":"which"},{"key":"B","text":"who"},{"key":"C","text":"whose"},{"key":"D","text":"whom"}]', 'B,D', '先行词the girl指人，在从句中作宾语，可用who/that/whom。此处who和whom均可。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'I like the house ___ has a big garden.', NULL, 'which/that', '先行词house指物，从句缺少主语，用which或that引导定语从句。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Everything ___ he said is true.', '[{"key":"A","text":"which"},{"key":"B","text":"what"},{"key":"C","text":"that"},{"key":"D","text":"who"}]', 'C', '先行词是不定代词everything时，关系代词只能用that，不能用which。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She is the most beautiful girl ___ I have ever seen.', '[{"key":"A","text":"which"},{"key":"B","text":"who"},{"key":"C","text":"that"},{"key":"D","text":"whom"}]', 'C', '先行词被最高级修饰时，关系代词只能用that。这是that的特殊用法规则。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='关系副词 when/where/why [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'This is the school ___ I studied three years ago.', '[{"key":"A","text":"which"},{"key":"B","text":"that"},{"key":"C","text":"where"},{"key":"D","text":"when"}]', 'C', '先行词the school表地点，从句中不缺主语宾语(缺地点状语)，用关系副词where。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I will never forget the day ___ we first met.', '[{"key":"A","text":"which"},{"key":"B","text":"when"},{"key":"C","text":"where"},{"key":"D","text":"that"}]', 'B', '先行词the day表时间，从句不缺主语宾语，用关系副词when。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'This is the reason ___ he was late.', NULL, 'why', '先行词reason，从句不缺成分，用关系副词why引导定语从句。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Do you know the factory ___ your father works?', '[{"key":"A","text":"that"},{"key":"B","text":"which"},{"key":"C","text":"where"},{"key":"D","text":"what"}]', 'C', '先行词factory表地点，work是不及物动词，从句主谓完整，缺地点状语用where。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='非限制性定语从句 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'My mother, ___ is a teacher, often helps me with my homework.', '[{"key":"A","text":"that"},{"key":"B","text":"which"},{"key":"C","text":"who"},{"key":"D","text":"what"}]', 'C', '非限制性定语从句(有逗号)不能用that，指人用who。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Beijing, ___ is the capital of China, is a beautiful city.', '[{"key":"A","text":"that"},{"key":"B","text":"which"},{"key":"C","text":"what"},{"key":"D","text":"where"}]', 'B', '非限制性定语从句指物用which，不能用that。先行词是专有名词Beijing。', 3, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'My father, that is 45 years old, works in a factory. (判断正误)', NULL, 'F', '非限制性定语从句不能用that，应改为who。逗号后的定语从句只能用which/who/whom/whose。', 3, 1);

-- ==================================================================
-- 四、名词性从句 (3 nodes × 7 = ~21 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='宾语从句 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I don''t know ___ he will come tomorrow.', '[{"key":"A","text":"if"},{"key":"B","text":"that"},{"key":"C","text":"what"},{"key":"D","text":"which"}]', 'A', 'whether/if引导宾语从句表示"是否"。whether可与or not连用，if较口语化。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She said ___ she was very busy.', '[{"key":"A","text":"what"},{"key":"B","text":"that"},{"key":"C","text":"which"},{"key":"D","text":"if"}]', 'B', '陈述句作宾语从句，用that引导。that在从句中不作成分，可省略。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'Can you tell me ___ the post office is?', NULL, 'where', '宾语从句中用陈述语序，疑问词where保留但句子用正常语序。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I wonder ___ he has passed the exam.', '[{"key":"A","text":"that"},{"key":"B","text":"what"},{"key":"C","text":"whether"},{"key":"D","text":"which"}]', 'C', 'wonder表示"想知道"，后接whether/if表示是否。', 2, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'He asked me where did I live. (判断正误)', NULL, 'F', '宾语从句必须用陈述语序。正确应为He asked me where I lived。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'I don''t know ___ (谁) broke the window.', NULL, 'who', '宾语从句中who既是疑问词又是从句主语，语序不变。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='主语从句 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '___ he said is true.', '[{"key":"A","text":"That"},{"key":"B","text":"Which"},{"key":"C","text":"What"},{"key":"D","text":"Whether"}]', 'C', 'what引导主语从句并在从句中作宾语(他说了什么)。that引导主语从句时不作成分。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ we need more time is obvious.', '[{"key":"A","text":"What"},{"key":"B","text":"Which"},{"key":"C","text":"That"},{"key":"D","text":"Whether"}]', 'C', '从句we need more time是完整的陈述句，不缺成分，用That引导主语从句。', 3, 1),
('英语[职高]', @n, 'FILL_IN', '___ she will come is not sure yet.', NULL, 'Whether', 'whether引导主语从句表示"是否"。注意：if不能引导主语从句，只能用whether。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'It is important ___ we should study hard.', '[{"key":"A","text":"what"},{"key":"B","text":"which"},{"key":"C","text":"that"},{"key":"D","text":"whether"}]', 'C', 'It is+adj+that从句是主语从句的常用句型(形式主语it)。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='表语从句与同位语从句 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The problem is ___ we can''t find enough money.', '[{"key":"A","text":"what"},{"key":"B","text":"which"},{"key":"C","text":"that"},{"key":"D","text":"whether"}]', 'C', '表语从句中不缺成分，用that引导(一般不省略)。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The news ___ our team won made us very happy.', '[{"key":"A","text":"which"},{"key":"B","text":"that"},{"key":"C","text":"what"},{"key":"D","text":"whether"}]', 'B', '同位语从句解释抽象名词news的内容，从句完整用that引导(不可省略)。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'The question is ___ we should start now or later.', NULL, 'whether', '表语从句表示"是否"，用whether引导。注意不能用if。', 3, 1);

-- ==================================================================
-- 五、状语从句 (2 nodes × 8 = ~16 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='时间/条件/原因状语从句 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I will call you ___ I arrive home.', '[{"key":"A","text":"until"},{"key":"B","text":"as soon as"},{"key":"C","text":"while"},{"key":"D","text":"since"}]', 'B', 'as soon as=一...就... 时间状语从句中用一般现在时表将来(主将从现)。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ it rains tomorrow, we will stay at home.', '[{"key":"A","text":"Because"},{"key":"B","text":"If"},{"key":"C","text":"When"},{"key":"D","text":"After"}]', 'B', 'if引导条件状语从句表示"如果"。条件状语从句也用一般现在时表将来。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He didn''t come to school ___ he was ill.', NULL, 'because', 'because引导原因状语从句表示直接原因。注意because和so不能同时用。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ she was tired, she kept working.', '[{"key":"A","text":"Because"},{"key":"B","text":"If"},{"key":"C","text":"Although"},{"key":"D","text":"Unless"}]', 'C', 'although引导让步状语从句，表示"尽管"。注意although和but不能同时用。', 2, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'If it will rain tomorrow, we will cancel the trip. (判断正误)', NULL, 'F', '条件状语从句中不能用将来时，正确为If it rains tomorrow。主将从现原则。', 3, 1);
