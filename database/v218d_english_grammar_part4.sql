-- ============================================================================
-- V218 Part 4: 虚拟语气 + 情景交际 + 主谓一致补遗
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- 八、虚拟语气 (1 node × 8 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='虚拟语气在条件句中的用法 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'If I ___ you, I would study harder.', '[{"key":"A","text":"am"},{"key":"B","text":"was"},{"key":"C","text":"were"},{"key":"D","text":"be"}]', 'C', '与现在事实相反的虚拟语气：if从句用过去式(be动词一律用were)，主句用would+动词原形。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'If he ___ earlier, he would have caught the train.', '[{"key":"A","text":"came"},{"key":"B","text":"had come"},{"key":"C","text":"comes"},{"key":"D","text":"coming"}]', 'B', '与过去事实相反的虚拟语气：从句用had done，主句用would have done。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'I wish I ___ (know) the answer.', NULL, 'knew', 'wish后的宾语从句用虚拟语气。与现在事实相反用过去式(knew)。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'If it ___ tomorrow, we would stay at home.', '[{"key":"A","text":"rains"},{"key":"B","text":"rained"},{"key":"C","text":"will rain"},{"key":"D","text":"had rained"}]', 'B', '与将来事实可能相反的虚拟：从句用过去式rained或were to rain或should rain。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She wishes she ___ a bird.', '[{"key":"A","text":"is"},{"key":"B","text":"was"},{"key":"C","text":"were"},{"key":"D","text":"will be"}]', 'C', 'wish后从句中be动词用were(不管主语人称)。这是虚拟语气的固定用法。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'If I had known the truth, I ___ (tell) you.', NULL, 'would have told', '与过去事实相反：从句had known，主句would have told。', 3, 1);

-- ==================================================================
-- 九、情景交际 (4 nodes × 7 = ~28 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='邀请与请求 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '---Would you like to come to my party? ---___.', '[{"key":"A","text":"Yes, I would"},{"key":"B","text":"I''d love to"},{"key":"C","text":"No, I wouldn''t"},{"key":"D","text":"Of course not"}]', 'B', '接受邀请常用I''d love to。Yes, I would是直接翻译，不地道。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '---Could you please help me with this box? ---___.', '[{"key":"A","text":"No, I couldn''t"},{"key":"B","text":"With pleasure"},{"key":"C","text":"You''re welcome"},{"key":"D","text":"Don''t mention it"}]', 'B', '接受请求用With pleasure(很乐意)。You''re welcome是回答感谢的。', 1, 1),
('英语[职高]', @n, 'FILL_IN', '---Shall we go swimming? ---___ (好主意)!', NULL, 'Good idea', 'Shall we...?提出建议，同意用Good idea!/Sounds great!', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '---Would you mind opening the window? ---___. I''ll do it right now.', '[{"key":"A","text":"Yes, I would"},{"key":"B","text":"Of course not"},{"key":"C","text":"You''re welcome"},{"key":"D","text":"Never mind"}]', 'B', 'Would you mind...?表示"你介意...吗?"，不介意用Of course not/Certainly not。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='感谢与道歉 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '---Thank you for helping me. ---___.', '[{"key":"A","text":"No, thanks"},{"key":"B","text":"You''re welcome"},{"key":"C","text":"Yes, please"},{"key":"D","text":"I''m fine"}]', 'B', '回答感谢用You''re welcome/My pleasure/Not at all。不能回答No, thanks。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '---I''m sorry I broke your cup. ---___.', '[{"key":"A","text":"You''re welcome"},{"key":"B","text":"It doesn''t matter"},{"key":"C","text":"Of course"},{"key":"D","text":"With pleasure"}]', 'B', '回应道歉用It doesn''t matter/Never mind/That''s OK。You''re welcome是回应感谢的。', 1, 1),
('英语[职高]', @n, 'FILL_IN', '---Sorry for being late. ---___ (没关系).', NULL, 'Never mind', 'Never mind/It doesn''t matter都是回复道歉的常用表达。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='问路与指路 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '---Excuse me, ___ is the nearest hospital? ---Go along this street and turn left.', '[{"key":"A","text":"what"},{"key":"B","text":"how"},{"key":"C","text":"where"},{"key":"D","text":"when"}]', 'C', '问路用where is...或how can I get to...。回答常用Go along/Turn left-right。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'The hospital is ___ (在...对面) the post office.', NULL, 'opposite', 'opposite=在对面。指路常用across from(美式)/opposite(英式)。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '---How can I get to the railway station? ---You can ___ Bus No.5.', '[{"key":"A","text":"take"},{"key":"B","text":"by"},{"key":"C","text":"on"},{"key":"D","text":"ride"}]', 'A', 'take a bus搭公交车(动词短语)。by bus是介词短语(不能单独作谓语)。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='建议与劝告 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '---I have a headache. ---You ___ see a doctor.', '[{"key":"A","text":"should"},{"key":"B","text":"can"},{"key":"C","text":"may"},{"key":"D","text":"must"}]', 'A', '给建议用should(应该)。You should do sth是劝告常用句型。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '---Why ___ join an English club to practice speaking?', '[{"key":"A","text":"don''t you"},{"key":"B","text":"you don''t"},{"key":"C","text":"not you"},{"key":"D","text":"you not"}]', 'A', 'Why don''t you do sth?=为什么不...? 提出建议的常用句型。', 1, 1),
('英语[职高]', @n, 'FILL_IN', '---What ___ going for a walk? ---Sounds great!', NULL, 'about', 'What about/How about doing sth?是提建议的句型，后接动名词。', 1, 1);

-- ==================================================================
-- 十、补遗: 主谓一致补充 + 情态动词补充
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='就近原则与意义一致 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'FILL_IN', 'Either you or he ___ (be) wrong.', NULL, 'is', 'either...or...就近原则，与he保持一致，用is。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词基本用法 can/must/may [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'This ___ be Tom''s book because his name is on it.', '[{"key":"A","text":"can"},{"key":"B","text":"may"},{"key":"C","text":"must"},{"key":"D","text":"could"}]', 'C', '名字在书上提供确凿证据，must表示肯定推测(一定是)。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词推测用法 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He ___ have gone to Beijing. I''m not sure.', '[{"key":"A","text":"must"},{"key":"B","text":"can"},{"key":"C","text":"may"},{"key":"D","text":"should"}]', 'C', 'may have done表示对过去的可能性推测(可能做了)。I''m not sure提示不确定。', 3, 1);

COMMIT;

SELECT 'V218 ALL PARTS DONE' AS result;
