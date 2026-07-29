-- ============================================================================
-- V219: 英语[职高] 词汇题库 P1 — 9节点 × 6题 = 54题
-- 覆盖: 动词/名词/形容词/介词/辨析/词组/阅读词/写作词
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ==================================================================
-- 一、动词类 (1 node × 7)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='动词类（be/have/do/make/take/go/get）[基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Please ___ your homework before watching TV.', '[{"key":"A","text":"make"},{"key":"B","text":"do"},{"key":"C","text":"take"},{"key":"D","text":"get"}]', 'B', 'do homework是固定搭配。make通常表示制造。do用于工作/任务类名词。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Could you ___ a photo of us?', '[{"key":"A","text":"make"},{"key":"B","text":"do"},{"key":"C","text":"take"},{"key":"D","text":"get"}]', 'C', 'take a photo拍照是固定搭配。make a photo是中式英语。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'She ___ (做) a cake for her mother''s birthday.', NULL, 'made', 'make a cake做蛋糕。make表示制作/创造。过去式made。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I need to ___ ready for the exam.', '[{"key":"A","text":"make"},{"key":"B","text":"do"},{"key":"C","text":"get"},{"key":"D","text":"take"}]', 'C', 'get ready准备好。get+形容词表示"变得"。get ready for是常用短语。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Let''s ___ shopping this afternoon.', '[{"key":"A","text":"go"},{"key":"B","text":"make"},{"key":"C","text":"take"},{"key":"D","text":"get"}]', 'A', 'go+doing表示去做某事。go shopping/swimming/fishing都是go+动名词。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ (有) two brothers and one sister.', NULL, 'has', 'have/has表示拥有。he是第三人称单数用has。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'It ___ me two hours to finish the work.', '[{"key":"A","text":"spent"},{"key":"B","text":"took"},{"key":"C","text":"cost"},{"key":"D","text":"paid"}]', 'B', 'It takes sb+时间+to do sth是固定句型。take表示花费时间。', 2, 1);

-- ==================================================================
-- 二、名词类 (1 node × 6)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='名词类（time/way/day/people/place）[基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'What ___ is it? It''s 3 o''clock.', '[{"key":"A","text":"day"},{"key":"B","text":"time"},{"key":"C","text":"way"},{"key":"D","text":"place"}]', 'B', 'What time is it?问几点钟。time表示时间点。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Can you tell me the ___ to the station?', '[{"key":"A","text":"time"},{"key":"B","text":"place"},{"key":"C","text":"way"},{"key":"D","text":"people"}]', 'C', 'the way to某地"去某地的路"。way表示道路/方法。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'There are many ___ (人) in the park on Sunday.', NULL, 'people', 'people是集合名词，表示"人们"，本身就表复数。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'This is a good ___ to have a picnic.', '[{"key":"A","text":"way"},{"key":"B","text":"time"},{"key":"C","text":"place"},{"key":"D","text":"day"}]', 'C', 'a good place to do sth做某事的好地方。place表示地点。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'What a beautiful ___ (天)! Let''s go out.', NULL, 'day', 'What a+形容词+名词感叹句。day表示一天/日子。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'He spends a lot of ___ playing computer games.', '[{"key":"A","text":"time"},{"key":"B","text":"times"},{"key":"C","text":"day"},{"key":"D","text":"way"}]', 'A', 'spend time doing sth花时间做某事。time不可数，不加s。', 1, 1);

-- ==================================================================
-- 三、形容词副词类 (1 node × 6)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='形容词副词类（good/bad/big/small/well）[基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'She sings very ___. Everyone likes her voice.', '[{"key":"A","text":"good"},{"key":"B","text":"well"},{"key":"C","text":"bad"},{"key":"D","text":"nice"}]', 'B', '修饰动词sing用副词well(好地)。good是形容词不能修饰动词。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'This room is too ___. We need a bigger one.', '[{"key":"A","text":"big"},{"key":"B","text":"small"},{"key":"C","text":"good"},{"key":"D","text":"well"}]', 'B', 'need a bigger one说明现在这个太小了。small是形容词。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He is a ___ (好的) student. He always helps others.', NULL, 'good', 'good修饰名词student。good at doing sth擅长做某事。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The weather is ___ today. Let''s stay at home.', '[{"key":"A","text":"good"},{"key":"B","text":"well"},{"key":"C","text":"bad"},{"key":"D","text":"big"}]', 'C', 'stay at home暗示天气不好。bad是形容词修饰weather。', 1, 1),
('英语[职高]', @n, 'TRUE_FALSE', 'He speaks English good. (判断正误)', NULL, 'F', '修饰动词speak应用副词well。正确:He speaks English well。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'I don''t feel ___ (好) today. I need to see a doctor.', NULL, 'well', 'feel well身体感觉好。well作形容词表示"健康的"。feel good表示心情好。', 2, 1);

-- ==================================================================
-- 四、介词连词类 (1 node × 6)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='介词连词类（in/on/at/to/for/and/but）[基础]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'We usually get up ___ 7 o''clock.', '[{"key":"A","text":"in"},{"key":"B","text":"on"},{"key":"C","text":"at"},{"key":"D","text":"for"}]', 'C', 'at+具体时间点(7点)。in+月份/年/季节。on+具体日期。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'My birthday is ___ June 15th.', '[{"key":"A","text":"in"},{"key":"B","text":"on"},{"key":"C","text":"at"},{"key":"D","text":"to"}]', 'B', 'on+具体日期(6月15日)。in+月份(June)。有具体日期用on。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'He is interested ___ music.', NULL, 'in', 'be interested in对...感兴趣。固定搭配介词in。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I like apples ___ I don''t like bananas.', '[{"key":"A","text":"and"},{"key":"B","text":"but"},{"key":"C","text":"or"},{"key":"D","text":"so"}]', 'B', '前后句意转折用but。喜欢苹果但不喜欢香蕉。and表并列。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'She is good ___ playing the piano.', NULL, 'at', 'be good at擅长... 固定搭配介词at。', 1, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'I have been here ___ Monday.', '[{"key":"A","text":"in"},{"key":"B","text":"on"},{"key":"C","text":"since"},{"key":"D","text":"at"}]', 'C', 'since+时间点(周一)。现在完成时的标志。for+时间段。', 2, 1);

-- ==================================================================
-- 五、动词辨析 (1 node × 6)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='动词辨析（spend/cost/take/pay/offer/provide）[中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'I ___ 50 yuan on this book yesterday.', '[{"key":"A","text":"cost"},{"key":"B","text":"took"},{"key":"C","text":"spent"},{"key":"D","text":"paid"}]', 'C', 'sb spend money on sth某人花钱买某物。cost主语是物，pay常与for搭配。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'This coat ___ me 200 yuan.', '[{"key":"A","text":"spent"},{"key":"B","text":"cost"},{"key":"C","text":"took"},{"key":"D","text":"paid"}]', 'B', 'sth cost sb money某物花了某人多少钱。cost主语是物，过去式cost。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He ___ (付钱) for the meal and left.', NULL, 'paid', 'pay for sth为某物付款。过去式paid。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'The company ___ free lunch to all workers.', '[{"key":"A","text":"provides"},{"key":"B","text":"offers"},{"key":"C","text":"gives"},{"key":"D","text":"all of above"}]', 'D', 'provide/offer/give都可以表示提供。provide sth to sb, offer sb sth, give sb sth。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'She ___ (主动提出) to help me with my English.', NULL, 'offered', 'offer to do sth主动提出做某事。过去式offered。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'It ___ three hours to drive to Beijing.', '[{"key":"A","text":"spent"},{"key":"B","text":"cost"},{"key":"C","text":"took"},{"key":"D","text":"paid"}]', 'C', 'It takes+时间+to do sth花时间做某事。take表示花费时间。', 2, 1);

-- ==================================================================
-- 六、名词辨析 (1 node × 6)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='名词辨析（chance/opportunity/ability/advantage）[中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'This is a great ___ for you to learn English.', '[{"key":"A","text":"chance"},{"key":"B","text":"ability"},{"key":"C","text":"advantage"},{"key":"D","text":"way"}]', 'A', 'a great chance一个好机会。chance和opportunity都表示机会。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'She has the ___ to speak three languages.', '[{"key":"A","text":"chance"},{"key":"B","text":"ability"},{"key":"C","text":"advantage"},{"key":"D","text":"opportunity"}]', 'B', 'have the ability to do有能力做某事。ability表示能力。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'Being tall gives him an ___ (优势) in basketball.', NULL, 'advantage', 'give sb an advantage给某人优势。advantage表示优势/有利条件。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Don''t miss this ___. It may never come again.', '[{"key":"A","text":"advantage"},{"key":"B","text":"ability"},{"key":"C","text":"opportunity"},{"key":"D","text":"way"}]', 'C', 'miss an opportunity错过机会。opportunity比chance更正式。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'He took the ___ (机会) to study abroad.', NULL, 'chance', 'take the chance抓住机会。chance较口语化，opportunity较正式。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'There is no ___ that he will win.', '[{"key":"A","text":"advantage"},{"key":"B","text":"ability"},{"key":"C","text":"chance"},{"key":"D","text":"way"}]', 'C', 'There is no chance that...不可能... chance表示可能性。', 2, 1);

-- ==================================================================
-- 七、词组搭配 (1 node × 6)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='词组搭配（take care of/look forward to/be used to）[中等]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Can you ___ my cat while I am away?', '[{"key":"A","text":"look for"},{"key":"B","text":"take care of"},{"key":"C","text":"look forward to"},{"key":"D","text":"give up"}]', 'B', 'take care of照顾。look for寻找，look forward to期待，give up放弃。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'I am looking forward ___ (介词) seeing you.', NULL, 'to', 'look forward to期待，to是介词后接doing。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'He used to ___ early, but now he gets up late.', '[{"key":"A","text":"get up"},{"key":"B","text":"getting up"},{"key":"C","text":"got up"},{"key":"D","text":"gets up"}]', 'A', 'used to do过去常常做(现在不了)。后接动词原形。be used to doing习惯做。', 3, 1),
('英语[职高]', @n, 'FILL_IN', 'Please ___ (注意) what the teacher says.', NULL, 'pay attention to', 'pay attention to注意。to是介词，后接名词/动名词。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Never ___ your dream. Keep trying!', '[{"key":"A","text":"give up"},{"key":"B","text":"give in"},{"key":"C","text":"give out"},{"key":"D","text":"give away"}]', 'A', 'give up放弃。give in屈服，give out分发/耗尽，give away赠送/泄露。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'She is used to ___ (live) in the city now.', NULL, 'living', 'be used to doing习惯做某事。to是介词接doing。', 3, 1);

-- ==================================================================
-- 八、阅读拓展词 (1 node × 5)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='阅读拓展词 [了解]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'The word "enormous" means ___.', '[{"key":"A","text":"very small"},{"key":"B","text":"very large"},{"key":"C","text":"very fast"},{"key":"D","text":"very old"}]', 'B', 'enormous=巨大的，同义词huge/giant。反义词tiny。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '"Finally" means ___.', '[{"key":"A","text":"at first"},{"key":"B","text":"suddenly"},{"key":"C","text":"at last"},{"key":"D","text":"usually"}]', 'C', 'finally=at last=in the end最终。是阅读理解高频词。', 1, 1),
('英语[职高]', @n, 'FILL_IN', 'The word "___" (立即) means right away or at once.', NULL, 'immediately', 'immediately=立刻，同义词right away/at once/instantly。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'What does "especially" mean?', '[{"key":"A","text":"特别地"},{"key":"B","text":"普通地"},{"key":"C","text":"偶然地"},{"key":"D","text":"迅速地"}]', 'A', 'especially=particularly特别地/尤其。用于强调。', 1, 1),
('英语[职高]', @n, 'FILL_IN', '"___" (然而) introduces a contrasting idea.', NULL, 'However', 'However=然而，表示转折。通常放在句首，后面加逗号。', 2, 1);

-- ==================================================================
-- 九、写作常用词 (1 node × 6)
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=4 AND name='写作常用词 [理解]' LIMIT 1);
INSERT INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]', @n, 'SINGLE_CHOICE', 'Which word is more formal for writing?', '[{"key":"A","text":"get"},{"key":"B","text":"obtain"},{"key":"C","text":"have"},{"key":"D","text":"make"}]', 'B', '写作中obtain比get更正式。书面语多用正式词汇。', 2, 1),
('英语[职高]', @n, 'FILL_IN', '___ (首先), we should prepare the materials.', NULL, 'Firstly', 'Firstly/First of all用于列举观点的开头。写作中常见的序数词。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', '___ addition, we need more time.', '[{"key":"A","text":"On"},{"key":"B","text":"At"},{"key":"C","text":"In"},{"key":"D","text":"For"}]', 'C', 'In addition此外。写作中用于补充观点。同义词Besides/Moreover。', 2, 1),
('英语[职高]', @n, 'FILL_IN', '___ (总之), I believe studying hard will pay off.', NULL, 'In conclusion', 'In conclusion/To sum up总之。作文结尾段常用开头短语。', 2, 1),
('英语[职高]', @n, 'SINGLE_CHOICE', 'Which is the best way to say "我认为" in formal writing?', '[{"key":"A","text":"I think"},{"key":"B","text":"In my opinion"},{"key":"C","text":"I guess"},{"key":"D","text":"Maybe"}]', 'B', 'In my opinion比I think更正式。I guess和Maybe太口语化。', 2, 1),
('英语[职高]', @n, 'FILL_IN', 'Increasing numbers of people ___ (相信) that education is important.', NULL, 'believe', 'writing常用believe/consider/maintain代替think。', 2, 1);

COMMIT;
