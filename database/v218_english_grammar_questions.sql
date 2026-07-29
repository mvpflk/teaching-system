-- ============================================================================
-- V218: 英语[职高] 语法题库 — P0 30节点 × ~9题 = 270题
-- 覆盖: 时态8节点/语态非谓语5节点/从句8节点/主谓一致情态虚拟情景9节点
-- 2026-07-25 · 幂等: INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- 一、时态 (8 nodes × 9 = ~72 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般现在时 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'She ___ to school by bus every day.', '[{"key":"A","text":"go"},{"key":"B","text":"goes"},{"key":"C","text":"going"},{"key":"D","text":"gone"}]', 'B', '一般现在时第三人称单数(She)动词加-s/es。every day表示经常性动作。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The sun ___ in the east.', '[{"key":"A","text":"rise"},{"key":"B","text":"rises"},{"key":"C","text":"rose"},{"key":"D","text":"rising"}]', 'B', '客观真理用一般现在时。太阳从东边升起是客观事实。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Tom ___ football every weekend.', '[{"key":"A","text":"play"},{"key":"B","text":"playing"},{"key":"C","text":"plays"},{"key":"D","text":"is play"}]', 'C', 'Tom是第三人称单数，一般现在时动词加s。every weekend是频率状语。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Water ___ at 100 degrees Celsius.', '[{"key":"A","text":"boil"},{"key":"B","text":"boils"},{"key":"C","text":"boiled"},{"key":"D","text":"boiling"}]', 'B', '科学事实用一般现在时，water是不可数名词，谓语用第三人称单数。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'He don''t like coffee. (判断正误)', NULL, 'F', '第三人称单数否定用doesn''t，正确为He doesn''t like coffee。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'She ___ (watch) TV every evening. (用所给词的正确形式填空)', NULL, 'watches', '一般现在时，主语She是第三人称单数，动词watch加es→watches。以s/x/sh/ch/o结尾加es。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'My father ___ in a hospital. He is a doctor.', '[{"key":"A","text":"work"},{"key":"B","text":"works"},{"key":"C","text":"working"},{"key":"D","text":"worked"}]', 'B', 'my father是第三人称单数，表示职业用一般现在时。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'The earth moves around the sun. (判断正误)', NULL, 'T', '客观真理性事实用一般现在时。The earth是第三人称单数，moves正确。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'What ___ your sister do?', '[{"key":"A","text":"do"},{"key":"B","text":"does"},{"key":"C","text":"is"},{"key":"D","text":"are"}]', 'B', 'your sister是第三人称单数，疑问句用does+动词原形。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般过去时 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'She ___ to Beijing last summer.', '[{"key":"A","text":"go"},{"key":"B","text":"goes"},{"key":"C","text":"went"},{"key":"D","text":"going"}]', 'C', 'last summer是过去时间状语，go的过去式为went（不规则变化）。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'They ___ a movie yesterday evening.', '[{"key":"A","text":"watch"},{"key":"B","text":"watched"},{"key":"C","text":"watching"},{"key":"D","text":"watches"}]', 'B', 'yesterday evening是过去时间，规则动词watch+ed构成过去式。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ (not go) to school yesterday because he was ill.', NULL, 'did not go', '一般过去时否定：did not + 动词原形。不是went的否定，必须用原形go。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I ___ born in 2008.', '[{"key":"A","text":"am"},{"key":"B","text":"was"},{"key":"C","text":"were"},{"key":"D","text":"is"}]', 'B', '过去具体时间点(2008年)用一般过去时，I用was。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'He goed to the park yesterday. (判断正误)', NULL, 'F', 'go是不规则动词，过去式为went而非goed。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'When ___ you finish your homework last night?', '[{"key":"A","text":"do"},{"key":"B","text":"does"},{"key":"C","text":"did"},{"key":"D","text":"are"}]', 'C', 'last night是过去时间，疑问句用did+主语+动词原形。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'She ___ (buy) a new dress last week.', NULL, 'bought', 'last week是过去时间，buy的过去式为bought（不规则变化）。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'There ___ a big tree in front of our house ten years ago.', '[{"key":"A","text":"is"},{"key":"B","text":"was"},{"key":"C","text":"are"},{"key":"D","text":"were"}]', 'B', 'ten years ago是过去时间，a big tree是单数，there be句型过去式用was。', 1, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='一般将来时 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'They ___ a meeting tomorrow afternoon.', '[{"key":"A","text":"have"},{"key":"B","text":"will have"},{"key":"C","text":"had"},{"key":"D","text":"having"}]', 'B', 'tomorrow afternoon是将来时间，用will+动词原形。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I think it ___ rain tomorrow.', '[{"key":"A","text":"is going"},{"key":"B","text":"will"},{"key":"C","text":"is"},{"key":"D","text":"has"}]', 'B', '表示预测用will+动词原形。be going to也表示将来，但这里是will rain。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'She will comes to the party tonight. (判断正误)', NULL, 'F', 'will后必须用动词原形，comes应改为come。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'We ___ visit our grandparents next Sunday.', '[{"key":"A","text":"are going to"},{"key":"B","text":"go"},{"key":"C","text":"went"},{"key":"D","text":"going"}]', 'A', 'be going to表示计划好的将来动作。next Sunday是将来时间。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'There ___ (be) a football match next Friday.', NULL, 'will be', 'next Friday是将来时间，there be句型的将来式为there will be。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Look at those clouds. It ___ rain soon.', '[{"key":"A","text":"will"},{"key":"B","text":"is going to"},{"key":"C","text":"is"},{"key":"D","text":"was"}]', 'B', '根据现有迹象(云)推测即将发生的事，用be going to。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'My mother ___ (buy) me a new phone next month.', NULL, 'will buy', 'next month是将来的明确时间，用will+动词原形表示将来动作。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The train ___ at 8:00 tomorrow morning.', '[{"key":"A","text":"leave"},{"key":"B","text":"leaves"},{"key":"C","text":"left"},{"key":"D","text":"will leaving"}]', 'B', '按时刻表发生的将来动作(火车出发)可用一般现在时表将来。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='现在进行时 [基础]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Listen! The birds ___ in the tree.', '[{"key":"A","text":"sing"},{"key":"B","text":"is singing"},{"key":"C","text":"are singing"},{"key":"D","text":"sang"}]', 'C', 'Listen!提示动作正在发生，the birds是复数，用are singing。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Look! She ___ a red dress today.', '[{"key":"A","text":"wears"},{"key":"B","text":"is wearing"},{"key":"C","text":"wear"},{"key":"D","text":"wore"}]', 'B', 'Look!表示正在看，此刻正在进行的动作用现在进行时。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ (read) a book now. Don''t disturb him.', NULL, 'is reading', 'now+Don''t disturb提示正在进行的动作，现在进行时be+V-ing。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'She is knowing the answer. (判断正误)', NULL, 'F', 'know是状态动词，不用于进行时。正确应为She knows the answer。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'We ___ for the bus at the moment.', '[{"key":"A","text":"wait"},{"key":"B","text":"waits"},{"key":"C","text":"are waiting"},{"key":"D","text":"waited"}]', 'C', 'at the moment=此刻，现在进行时标志。we是复数，用are waiting。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'It''s 7 p.m. Mom ___ (cook) dinner in the kitchen.', NULL, 'is cooking', '具体时间点(7 p.m.)描述正在发生的事，mom是单数，用is cooking。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'They ___ football. Don''t make so much noise.', '[{"key":"A","text":"play"},{"key":"B","text":"plays"},{"key":"C","text":"are playing"},{"key":"D","text":"played"}]', 'C', 'Don''t make noise暗示动作正在进行中，用现在进行时。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Hurry up! Everyone ___ for you.', '[{"key":"A","text":"wait"},{"key":"B","text":"waits"},{"key":"C","text":"is waiting"},{"key":"D","text":"waited"}]', 'C', 'Hurry up!说明此刻别人正在等你。everyone是不定代词，谓语用单数is waiting。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='过去进行时 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I ___ my homework when you called me.', '[{"key":"A","text":"do"},{"key":"B","text":"was doing"},{"key":"C","text":"did"},{"key":"D","text":"am doing"}]', 'B', 'when you called是过去时间点，那一刻正在进行的动作用过去进行时was/were+V-ing。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'While she ___ TV, her mother came back.', '[{"key":"A","text":"watches"},{"key":"B","text":"is watching"},{"key":"C","text":"was watching"},{"key":"D","text":"watched"}]', 'C', 'while引导的时间状语从句常用过去进行时，表示在过去某个时间段内持续的动作。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'They ___ (have) dinner at 7 p.m. yesterday.', NULL, 'were having', '过去具体时间点(yesterday at 7 p.m.)正在进行的动作，用过去进行时。they用were。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'What ___ you ___ at this time yesterday?', '[{"key":"A","text":"do, do"},{"key":"B","text":"were, doing"},{"key":"C","text":"did, do"},{"key":"D","text":"are, doing"}]', 'B', 'at this time yesterday是过去具体时间点，问那时正在做什么用过去进行时。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I ___ along the street when it began to rain.', '[{"key":"A","text":"walk"},{"key":"B","text":"walked"},{"key":"C","text":"was walking"},{"key":"D","text":"am walking"}]', 'C', 'when it began to rain是过去时间点，主句用过去进行时表示那时正在走的动作。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='现在完成时 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I ___ my homework already.', '[{"key":"A","text":"finish"},{"key":"B","text":"finished"},{"key":"C","text":"have finished"},{"key":"D","text":"finishing"}]', 'C', 'already是现在完成时标志词，表示动作已经完成，结构为have/has+过去分词。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She ___ never ___ to Shanghai.', '[{"key":"A","text":"has, been"},{"key":"B","text":"have, gone"},{"key":"C","text":"is, going"},{"key":"D","text":"was, went"}]', 'A', 'never是现在完成时标志。have been to去过(已回)，have gone to去了(未回)。这里表示经历。', 2, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'He has gone to Beijing and he will come back tomorrow. (判断正误)', NULL, 'F', 'has gone to表示去了还没回来，与will come back tomorrow矛盾。应改为has been to。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'We ___ (live) here since 2020.', NULL, 'have lived', 'since+时间点是现在完成时标志，表示从过去持续到现在的动作。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'They have worked in this factory ___ 2015.', '[{"key":"A","text":"for"},{"key":"B","text":"since"},{"key":"C","text":"in"},{"key":"D","text":"from"}]', 'B', 'since+时间点，for+时间段。2015是时间点，用since。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'I ___ (not see) him for three years.', NULL, 'have not seen', 'for+时间段(three years)是现在完成时标志，表示到现在为止的三年里没见过他。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'So far we ___ 20 units of this book.', '[{"key":"A","text":"learned"},{"key":"B","text":"have learned"},{"key":"C","text":"learn"},{"key":"D","text":"are learning"}]', 'B', 'so far=到目前为止，是现在完成时常用标志词。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='现在完成时-for/since区别 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He has worked here ___ ten years.', '[{"key":"A","text":"since"},{"key":"B","text":"for"},{"key":"C","text":"from"},{"key":"D","text":"in"}]', 'B', 'ten years是时间段(十年)，用for。since+时间点(如since 2014)。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She has been ill ___ last Monday.', '[{"key":"A","text":"for"},{"key":"B","text":"since"},{"key":"C","text":"in"},{"key":"D","text":"at"}]', 'B', 'last Monday是具体时间点(上周一)，用since。for+时间段(如for a week)。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'I have known her ___ we were children.', NULL, 'since', 'since+从句(过去时)表示从过去某个时间点开始延续到现在。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='过去完成时 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'By the time we arrived, the movie ___.', '[{"key":"A","text":"started"},{"key":"B","text":"has started"},{"key":"C","text":"had started"},{"key":"D","text":"starts"}]', 'C', 'by the time+过去时间点，主句用过去完成时had+过去分词，表示"过去的过去"。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She said she ___ the book before.', '[{"key":"A","text":"read"},{"key":"B","text":"has read"},{"key":"C","text":"had read"},{"key":"D","text":"reads"}]', 'C', '主句said是过去时，宾语从句中"读书"发生在"说"之前(过去的过去)，用过去完成时。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'When I got to the station, the train ___ (leave).', NULL, 'had left', '火车离开发生在到达车站之前(过去的过去)，用过去完成时had left。', 3, 1);

-- ==================================================================
-- 二、语态+非谓语 (5 nodes × 8 = ~42 questions)
-- ==================================================================

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='被动语态 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'English ___ all over the world.', '[{"key":"A","text":"speaks"},{"key":"B","text":"is spoken"},{"key":"C","text":"spoke"},{"key":"D","text":"speaking"}]', 'B', 'English是"被说"，不是自己说。被动语态：be+过去分词。一般现在时被动am/is/are+过去分词。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The window ___ by Tom yesterday.', '[{"key":"A","text":"breaks"},{"key":"B","text":"broke"},{"key":"C","text":"was broken"},{"key":"D","text":"is broken"}]', 'C', 'yesterday是过去时间，window是"被打破"的。过去时被动：was/were+过去分词。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'These books ___ (write) by Lu Xun.', NULL, 'were written', '书是被写的，Lu Xun已故用过去时。these books是复数，用were written。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'A new hospital ___ in our city next year.', '[{"key":"A","text":"will build"},{"key":"B","text":"will be built"},{"key":"C","text":"builds"},{"key":"D","text":"built"}]', 'B', 'next year是将来时间，医院是被建造的。将来时被动：will be+过去分词。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The room ___ every day.', '[{"key":"A","text":"cleans"},{"key":"B","text":"is cleaned"},{"key":"C","text":"cleaned"},{"key":"D","text":"cleaning"}]', 'B', '房间是被打扫的，every day用一般现在时。被动：is+过去分词。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'English is speaking in Canada. (判断正误)', NULL, 'F', 'English是被说的，应改为English is spoken。主动表被动是常见错误。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'The cake ___ (make) by my mother yesterday.', NULL, 'was made', 'yesterday过去时间，蛋糕是被做的，单数用was made。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='动词不定式 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'She wants ___ a teacher when she grows up.', '[{"key":"A","text":"be"},{"key":"B","text":"to be"},{"key":"C","text":"being"},{"key":"D","text":"been"}]', 'B', 'want后接动词不定式to do。常见接to do的动词：want/decide/hope/wish/plan。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'It is important ___ English every day.', '[{"key":"A","text":"practice"},{"key":"B","text":"practicing"},{"key":"C","text":"to practice"},{"key":"D","text":"practiced"}]', 'C', 'It is+形容词+to do是固定句型，不定式作真正主语。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He decided ___ (go) abroad for further study.', NULL, 'to go', 'decide后接to do，decide to do sth是固定搭配。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The teacher told us ___ in class.', '[{"key":"A","text":"not talk"},{"key":"B","text":"not to talk"},{"key":"C","text":"not talking"},{"key":"D","text":"don''t talk"}]', 'B', 'tell sb not to do sth是固定句型，不定式的否定在to前加not。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I have a lot of homework ___.', '[{"key":"A","text":"do"},{"key":"B","text":"doing"},{"key":"C","text":"to do"},{"key":"D","text":"done"}]', 'C', 'have sth to do是固定搭配，不定式作后置定语修饰homework。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He got up early ___ (catch) the first bus.', NULL, 'to catch', '不定式表示目的，"为了赶上"，to do作目的状语。', 2, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='动名词 [中等]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'He enjoys ___ basketball after school.', '[{"key":"A","text":"play"},{"key":"B","text":"to play"},{"key":"C","text":"playing"},{"key":"D","text":"played"}]', 'C', 'enjoy后接动名词V-ing。常见接doing的动词：enjoy/mind/finish/practice/suggest。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Would you mind ___ the window?', '[{"key":"A","text":"open"},{"key":"B","text":"to open"},{"key":"C","text":"opening"},{"key":"D","text":"opened"}]', 'C', 'mind doing sth介意做某事，固定搭配。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'She practices ___ (speak) English every morning.', NULL, 'speaking', 'practice后接动名词doing。常见接doing的动词口诀:完成实践值得忙(finish/practice/be worth/be busy)。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ is good for your health.', '[{"key":"A","text":"Run"},{"key":"B","text":"Running"},{"key":"C","text":"Ran"},{"key":"D","text":"To run"}]', 'B', '动名词作主语表示泛指的动作。Running在此是主语。不定式也可作主语但to run更强调具体一次。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I look forward to ___ you.', '[{"key":"A","text":"see"},{"key":"B","text":"seeing"},{"key":"C","text":"seen"},{"key":"D","text":"saw"}]', 'B', 'look forward to中的to是介词，后接名词/动名词。类似：be used to doing, pay attention to doing。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='分词作定语和状语 [困难]' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The boy ___ under the tree is my brother.', '[{"key":"A","text":"sit"},{"key":"B","text":"sits"},{"key":"C","text":"sitting"},{"key":"D","text":"sat"}]', 'C', '现在分词作后置定语，the boy sitting=the boy who is sitting，主动含义。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ from the top of the hill, the city looks beautiful.', '[{"key":"A","text":"See"},{"key":"B","text":"Seeing"},{"key":"C","text":"Seen"},{"key":"D","text":"Saw"}]', 'C', 'the city是"被看"的，用过去分词Seen表被动。分词作状语时，其逻辑主语必须与主句主语一致。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'The house ___ (build) last year is very big.', NULL, 'built', '房子是被建造的，过去分词built作后置定语表被动和完成。', 3, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ the news, she burst into tears.', '[{"key":"A","text":"Hear"},{"key":"B","text":"Heard"},{"key":"C","text":"Hearing"},{"key":"D","text":"To hear"}]', 'C', 'she是"听到"这个动作的发出者(主动)，用现在分词Hearing作时间状语。=When she heard。', 3, 1);

SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='不定式' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I went to the library ___ some books.', '[{"key":"A","text":"borrow"},{"key":"B","text":"borrowing"},{"key":"C","text":"to borrow"},{"key":"D","text":"borrowed"}]', 'C', '不定式to borrow表示目的。去图书馆的目的是借书。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The best way ___ English is to use it every day.', '[{"key":"A","text":"learn"},{"key":"B","text":"to learn"},{"key":"C","text":"learning"},{"key":"D","text":"learned"}]', 'B', 'the way to do sth是固定搭配，不定式作后置定语修饰way。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He is too young ___ (go) to school.', NULL, 'to go', 'too...to...结构表示"太...而不能..."，too young to go太年轻不能上学。', 2, 1);

COMMIT;
