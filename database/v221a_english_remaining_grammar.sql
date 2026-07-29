-- ============================================================================
-- V221a: 英语剩余节点 — 语法平行节点20 + 写作3 = 23节点
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- 时态7节点 (一般现在/过去/将来/现在进行/过去进行/现在完成/过去完成)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般现在时' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The earth ___ around the sun.', '[{"key":"A","text":"move"},{"key":"B","text":"moves"},{"key":"C","text":"moved"},{"key":"D","text":"moving"}]', 'B', '客观真理用一般现在时。earth单数用moves。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ (go) to school at 7:00 every morning.', NULL, 'goes', 'every morning+一般现在时。he三单用goes。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'She don''t like apples. (判断正误)', NULL, 'F', '三单否定用doesn''t。正确:She doesn''t like apples。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般过去时' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'They ___ a film last night.', '[{"key":"A","text":"watch"},{"key":"B","text":"watched"},{"key":"C","text":"watching"},{"key":"D","text":"watches"}]', 'B', 'last night过去时。规则动词+ed。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ (see) his teacher yesterday.', NULL, 'saw', 'yesterday过去时。see不规则过去式saw。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'She ___ (not come) to school last Monday.', NULL, 'did not come', '过去时否定did not+原形。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般将来时' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'We ___ a picnic if it is fine tomorrow.', '[{"key":"A","text":"have"},{"key":"B","text":"will have"},{"key":"C","text":"had"},{"key":"D","text":"having"}]', 'B', 'tomorrow将来时。will+原形。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'There ___ (be) a meeting next Monday.', NULL, 'will be', 'next Monday将来时。there be将来式there will be。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='现在进行时' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Listen! The baby ___ .', '[{"key":"A","text":"cry"},{"key":"B","text":"is crying"},{"key":"C","text":"cried"},{"key":"D","text":"cries"}]', 'B', 'Listen!提示现在进行时。baby单数用is crying。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'Look! They ___ (run) on the playground.', NULL, 'are running', 'Look!+现在进行时。they复数用are running。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='过去进行时' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I ___ TV when he called.', '[{"key":"A","text":"watch"},{"key":"B","text":"was watching"},{"key":"C","text":"watched"},{"key":"D","text":"am watching"}]', 'B', 'when he called过去时间点。那时正在看用过去进行时。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'They ___ (play) football at 4 p.m. yesterday.', NULL, 'were playing', '过去具体时间用过去进行时。they用were playing。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='现在完成时' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I ___ this movie twice.', '[{"key":"A","text":"see"},{"key":"B","text":"have seen"},{"key":"C","text":"saw"},{"key":"D","text":"seeing"}]', 'B', 'twice暗示经历。现在完成时have/has done。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'She ___ (be) to Shanghai before.', NULL, 'has been', 'before是现在完成时标志。have been to去过。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='过去完成时' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The film ___ when we got to the cinema.', '[{"key":"A","text":"started"},{"key":"B","text":"had started"},{"key":"C","text":"has started"},{"key":"D","text":"starts"}]', 'B', '电影开始发生在到达之前。过去的过去用过去完成时had done。', 3, 1);

-- ==================================================================
-- 被动语态3节点
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般现在时被动' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The classroom ___ every day.', '[{"key":"A","text":"cleans"},{"key":"B","text":"is cleaned"},{"key":"C","text":"cleaned"},{"key":"D","text":"cleaning"}]', 'B', '教室是被打扫的。一般现在时被动:am/is/are+done。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般过去时被动' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The letter ___ yesterday.', '[{"key":"A","text":"sends"},{"key":"B","text":"sent"},{"key":"C","text":"was sent"},{"key":"D","text":"is sent"}]', 'C', '信是被寄的。yesterday过去时被动:was/were+done。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词被动' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The work ___ finished tomorrow.', '[{"key":"A","text":"must"},{"key":"B","text":"must be"},{"key":"C","text":"must is"},{"key":"D","text":"be"}]', 'B', '情态动词被动:情态动词+be+done。must be done必须被完成。', 3, 1);

-- ==================================================================
-- 从句3节点
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='宾语从句' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I think ___ he is right.', '[{"key":"A","text":"what"},{"key":"B","text":"that"},{"key":"C","text":"if"},{"key":"D","text":"which"}]', 'B', 'think后接that引导的宾语从句。陈述句用that，可省略。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='定语从句' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The boy ___ is playing over there is my cousin.', '[{"key":"A","text":"which"},{"key":"B","text":"who"},{"key":"C","text":"what"},{"key":"D","text":"where"}]', 'B', '先行词boy指人用who/that。which指物。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='状语从句' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I will call you ___ I get home.', '[{"key":"A","text":"until"},{"key":"B","text":"when"},{"key":"C","text":"while"},{"key":"D","text":"since"}]', 'B', 'when引导时间状语从句。状语从句用一般现在时表将来。', 2, 1);

-- ==================================================================
-- 词法4节点 (冠词/介词搭配/比较级/情态)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='冠词 a/an/the' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'There is ___ apple on the table.', '[{"key":"A","text":"a"},{"key":"B","text":"an"},{"key":"C","text":"the"},{"key":"D","text":"x"}]', 'B', 'apple以元音音素开头用an。a用于辅音音素开头。', 1, 1),
('英语[职高]', @n, 'FILL_IN', '___ sun rises in the east.', NULL, 'The', '世界上独一无二的事物(太阳)前用定冠词the。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='介词搭配' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'She is good ___ math.', '[{"key":"A","text":"in"},{"key":"B","text":"at"},{"key":"C","text":"on"},{"key":"D","text":"for"}]', 'B', 'be good at擅长。固定搭配at。be good to对...好。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He arrived ___ the station at 8:00.', NULL, 'at', 'arrive at+小地方。arrive in+大地方(城市/国家)。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='比较级和最高级' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Tom is ___ than his brother.', '[{"key":"A","text":"tall"},{"key":"B","text":"taller"},{"key":"C","text":"tallest"},{"key":"D","text":"more tall"}]', 'B', 'than前用比较级。单音节形容词+er。tall→taller。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'She is the ___ (tall) girl in her class.', NULL, 'tallest', 'in her class表示范围，用最高级。tall→tallest。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='情态动词' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'You ___ finish your homework before playing.', '[{"key":"A","text":"can"},{"key":"B","text":"may"},{"key":"C","text":"should"},{"key":"D","text":"need"}]', 'C', 'should表示义务应该。can表能力，may表许可。', 1, 1);

-- ==================================================================
-- 句型3节点 (there be/祈使句/反意疑问句)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='there be 句型' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'There ___ many students in the classroom.', '[{"key":"A","text":"is"},{"key":"B","text":"are"},{"key":"C","text":"have"},{"key":"D","text":"has"}]', 'B', 'there be中be动词与最近的名词保持一致。many students复数用are。', 1, 1),
('英语[职高]', @n, 'FILL_IN', '___ there any milk in the glass?', NULL, 'Is', 'milk不可数名词视作单数。一般疑问句Is there。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='祈使句' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', '___ in the library.', '[{"key":"A","text":"Not talk"},{"key":"B","text":"Don''t talk"},{"key":"C","text":"Not talking"},{"key":"D","text":"No talk"}]', 'B', '祈使句否定用Don''t+动词原形。图书馆不准说话用Don''t talk。', 1, 1),
('英语[职高]', @n, 'FILL_IN', '___ (be) careful when you cross the road.', NULL, 'Be', '祈使句以动词原形开头。Be careful小心。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='反意疑问句' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He is a student, ___?', '[{"key":"A","text":"is he"},{"key":"B","text":"isn''t he"},{"key":"C","text":"does he"},{"key":"D","text":"doesn''t he"}]', 'B', '反意疑问句前肯定后否定。前is后isn''t he。', 3, 1);

-- ==================================================================
-- 写作3节点 (书信/通知/观点)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='书信格式与常用表达 [掌握]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The correct way to start a formal letter is ___.', '[{"key":"A","text":"Hello friend"},{"key":"B","text":"Dear Sir or Madam"},{"key":"C","text":"Hi there"},{"key":"D","text":"Hey"}]', 'B', '正式信件开头用Dear Sir/Madam。Dear+姓名用于一般书信。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'A formal letter usually ends with "Yours ___"(真诚地).', NULL, 'sincerely', 'Yours sincerely/Yours faithfully是正式信件结尾。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'In a letter, "I am writing to..." is used to ___.', '[{"key":"A","text":"end the letter"},{"key":"B","text":"state the purpose"},{"key":"C","text":"give thanks"},{"key":"D","text":"introduce yourself"}]', 'B', 'I am writing to...=我写信是为了...，表明写信目的。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='通知类写作 [掌握]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'A notice usually begins with ___.', '[{"key":"A","text":"Dear friends"},{"key":"B","text":"NOTICE"},{"key":"C","text":"Hello"},{"key":"D","text":"Attention"}]', 'B', '通知标题用NOTICE居中大写。Attention用于口头通知。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'At the end of a notice, you should write the name of the ___ and the date.', NULL, 'organizer', '通知落款写组织者名称和日期。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='观点表达与逻辑衔接 [掌握]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'To introduce your first point in an essay, you should use ___.', '[{"key":"A","text":"However"},{"key":"B","text":"First of all"},{"key":"C","text":"In conclusion"},{"key":"D","text":"For example"}]', 'B', 'First of all引出第一个观点。However表转折，In conclusion总结。', 2, 1),
('英语[职高]', @n, 'FILL_IN', '___ (总之), I believe that exercise is important for health.', NULL, 'In conclusion', '总结用In conclusion/To sum up。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'To give an example, you should write ___.', '[{"key":"A","text":"In conclusion"},{"key":"B","text":"For instance"},{"key":"C","text":"However"},{"key":"D","text":"Therefore"}]', 'B', 'For instance/For example用于举例子。', 2, 1);

COMMIT;
