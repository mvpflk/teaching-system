-- ============================================================================
-- V218 Part 3: 状语从句续 + 主谓一致 + 情态动词 + 虚拟语气 + 情景交际
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- 五续、让步/目的状语从句
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='让步/目的状语从句 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He got up early ___ he could catch the first bus.', '[{"key":"A","text":"because"},{"key":"B","text":"so that"},{"key":"C","text":"although"},{"key":"D","text":"unless"}]', 'B', 'so that引导目的状语从句表示"以便/为了"。从句中常用can/could/will/would。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ he is young, he knows a lot about computers.', '[{"key":"A","text":"If"},{"key":"B","text":"Because"},{"key":"C","text":"Though"},{"key":"D","text":"Since"}]', 'C', 'though/although引导让步状语从句。"尽管他年轻"表转折，不能用because。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'She worked hard ___ she could pass the exam.', NULL, 'so that', '目的状语从句so that+情态动词。区别于结果状语从句so...that...。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ the weather was bad, they still went out.', '[{"key":"A","text":"Because"},{"key":"B","text":"If"},{"key":"C","text":"Even though"},{"key":"D","text":"So that"}]', 'C', 'Even though=即使/尽管，比although语气更强。后面still与之呼应。', 3, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'Although he is poor, but he is happy. (判断正误)', NULL, 'F', 'although和but不能同时使用。应去掉but或although之一。', 3, 1);

-- ==================================================================
-- 五续、状语从句补充
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='时间/条件/原因状语从句 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'FILL_IN', 'You won''t pass the exam ___ you study hard.', NULL, 'unless', 'unless=if not除非/如果不。条件从句中同样用一般现在时表将来。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ you have finished your homework, you can go out to play.', '[{"key":"A","text":"Until"},{"key":"B","text":"Since"},{"key":"C","text":"Unless"},{"key":"D","text":"As soon as"}]', 'B', 'since=既然，引导原因状语从句，表示已知的原因。比because语气轻。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'I was reading ___ my mother was cooking.', NULL, 'while', 'while表示"在...期间"，从句常用进行时，表示两个动作同时进行。', 2, 1);

-- ==================================================================
-- 六、主谓一致 (1 node × 6 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='就近原则与意义一致 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Neither he nor I ___ a teacher.', '[{"key":"A","text":"is"},{"key":"B","text":"am"},{"key":"C","text":"are"},{"key":"D","text":"be"}]', 'B', 'neither...nor...采用就近原则，谓语与最近的主语I保持一致，用am。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'There ___ a book and two pens on the desk.', '[{"key":"A","text":"is"},{"key":"B","text":"are"},{"key":"C","text":"have"},{"key":"D","text":"has"}]', 'A', 'there be句型采用就近原则，最接近的主语是a book(单数)，用is。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'Not only the students but also the teacher ___ (like) playing basketball.', NULL, 'likes', 'not only...but also...就近原则，the teacher是单数，动词用likes。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The police ___ looking for the lost child.', '[{"key":"A","text":"is"},{"key":"B","text":"are"},{"key":"C","text":"was"},{"key":"D","text":"has"}]', 'B', 'police是集合名词，表示"警察们"，谓语用复数。类似：people/cattle。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Maths ___ my favorite subject.', '[{"key":"A","text":"are"},{"key":"B","text":"is"},{"key":"C","text":"were"},{"key":"D","text":"have"}]', 'B', 'maths/physics/news等以s结尾的学科名词作单数看待，谓语用单数。', 2, 1);

-- ==================================================================
-- 七、情态动词 (2 nodes × 8 = ~14 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词基本用法 can/must/may [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'You ___ smoke here. It''s not allowed.', '[{"key":"A","text":"can"},{"key":"B","text":"mustn''t"},{"key":"C","text":"may"},{"key":"D","text":"needn''t"}]', 'B', 'mustn''t表示"禁止，不允许"。It''s not allowed提示此处是禁止吸烟。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ I use your phone?', '[{"key":"A","text":"Must"},{"key":"B","text":"Should"},{"key":"C","text":"May"},{"key":"D","text":"Need"}]', 'C', 'May I...?是礼貌请求允许的句型。Can I...也常用但May更正式。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'You ___ (not have to) come tomorrow. It''s a holiday.', NULL, 'don''t have to', 'don''t have to=不必(没有义务)。区别于mustn''t(禁止)。假期了没有来的必要。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She ___ be at home because the light is on.', '[{"key":"A","text":"can"},{"key":"B","text":"may"},{"key":"C","text":"must"},{"key":"D","text":"need"}]', 'C', 'must表示肯定推测(一定是)，证据是灯亮着。否定推测用can''t。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'We ___ respect our parents.', '[{"key":"A","text":"can"},{"key":"B","text":"may"},{"key":"C","text":"should"},{"key":"D","text":"would"}]', 'C', 'should表示建议/义务，"应该"。尊重父母是义务性的。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ play the piano when he was five.', NULL, 'could', 'could是can的过去式，表示过去的能力。when he was five提示过去时间。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '---Must I finish the work today? ---No, you ___.', '[{"key":"A","text":"mustn''t"},{"key":"B","text":"can''t"},{"key":"C","text":"needn''t"},{"key":"D","text":"shouldn''t"}]', 'C', 'Must I...?的否定回答用No, you needn''t(=don''t have to)。mustn''t是禁止，不能用于回答。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词推测用法 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He ___ be at home now. I just saw him in the library.', '[{"key":"A","text":"must"},{"key":"B","text":"can''t"},{"key":"C","text":"may"},{"key":"D","text":"should"}]', 'B', 'can''t表示否定推测(不可能)。刚在图书馆看见他，所以不可能在家。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The ground is wet. It ___ last night.', '[{"key":"A","text":"must rain"},{"key":"B","text":"must have rained"},{"key":"C","text":"can rain"},{"key":"D","text":"should rain"}]', 'B', 'must have done表示对过去的肯定推测(昨晚一定下雨了)。证据是地湿。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'She looks tired. She ___ (熬夜) last night.', NULL, 'must have stayed up', 'must have done对过去的推测。熬夜stay up late。', 3, 1);
