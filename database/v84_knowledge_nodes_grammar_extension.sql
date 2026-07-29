-- v84: knowledge_nodes 扩展语法树支持
-- Applied: 2026-06-05
-- 新增 grammar_category + unlock_stage 两列
-- 插入 6 大类 + 20 子节点语法树种子数据
SET NAMES utf8mb4;

SET @en_zg = (SELECT id FROM dict_subject WHERE subject_name = '英语[职高]' AND status = 1 LIMIT 1);
SET @en_zg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @en_zg AND level = 1 LIMIT 1);

-- 1. 新增列
ALTER TABLE knowledge_nodes
  ADD COLUMN grammar_category VARCHAR(30) DEFAULT NULL
    COMMENT '语法分类: tense/passive/clause/non_finite/lexical/sentence',
  ADD COLUMN unlock_stage INT DEFAULT NULL
    COMMENT '解锁所需阶段 1-7 (仅语法节点)';

ALTER TABLE question_bank
  ADD COLUMN grammar_node_id BIGINT DEFAULT NULL
    COMMENT '关联 knowledge_nodes.id (语法节点)';

-- 2. 语法大类 (level=3)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, content, grammar_category, unlock_stage, sort_order) VALUES
(@en_zg_root, @en_zg, 3, '时态',        '# 英语时态\n\n动词时态表示动作发生的时间，是英语语法的基础。', 'tense',    2, 1),
(@en_zg_root, @en_zg, 3, '被动语态',    '# 被动语态\n\n主语是动作的承受者，结构为 be+过去分词。',      'passive',  5, 2),
(@en_zg_root, @en_zg, 3, '从句',        '# 英语从句\n\n复合句中的从属分句，包括宾语/定语/状语从句。',  'clause',   6, 3),
(@en_zg_root, @en_zg, 3, '非谓语动词',  '# 非谓语动词\n\n不定式、动名词、分词，不作谓语但保留动词特征。', 'non_finite', 7, 4),
(@en_zg_root, @en_zg, 3, '词法',        '# 英语词法\n\n冠词/介词/比较级等词汇层面的语法规则。',      'lexical',  3, 5),
(@en_zg_root, @en_zg, 3, '句型',        '# 英语句型\n\nthere be/祈使句/反意疑问句等常用句式。',        'sentence', 4, 6);

SET @tense_id    = (SELECT id FROM knowledge_nodes WHERE name='时态'       AND level=3 AND subject_id=@en_zg LIMIT 1);
SET @passive_id  = (SELECT id FROM knowledge_nodes WHERE name='被动语态'   AND level=3 AND subject_id=@en_zg LIMIT 1);
SET @clause_id   = (SELECT id FROM knowledge_nodes WHERE name='从句'       AND level=3 AND subject_id=@en_zg LIMIT 1);
SET @nonfin_id   = (SELECT id FROM knowledge_nodes WHERE name='非谓语动词' AND level=3 AND subject_id=@en_zg LIMIT 1);
SET @lexical_id  = (SELECT id FROM knowledge_nodes WHERE name='词法'       AND level=3 AND subject_id=@en_zg LIMIT 1);
SET @sent_id     = (SELECT id FROM knowledge_nodes WHERE name='句型'       AND level=3 AND subject_id=@en_zg LIMIT 1);

-- 3. 时态子节点 (7 个, level=4)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, content, grammar_category, unlock_stage, sort_order) VALUES
(@tense_id, @en_zg, 4, '一般现在时',
'## 一般现在时\n\n经常性/习惯性动作。主语三单时动词加 -s/-es。\n\n例句:\n1. She **goes** to school by bus every day.\n2. He **finishes** his homework before dinner.\n3. They **play** basketball after school.\n\n提示:"主语是第三人称单数(he/she/it)时，动词加 -s 或 -es"', 'tense', 2, 1),
(@tense_id, @en_zg, 4, '一般过去时',
'## 一般过去时\n\n过去某时间发生的动作。规则动词加 -ed。\n\n例句:\n1. She **watched** a movie last night.\n2. He **finished** his work yesterday.\n3. They **played** football after school.\n\n提示:"last night/yesterday/ago 提示一般过去时，动词加 -ed"', 'tense', 2, 2),
(@tense_id, @en_zg, 4, '一般将来时',
'## 一般将来时\n\nwill + 动词原形。\n\n例句:\n1. We **will have** a test tomorrow.\n2. She **will go** to college next year.\n3. I **will help** you with your homework.\n\n提示:"tomorrow/next year 提示一般将来时: will + 动词原形"', 'tense', 2, 3),
(@tense_id, @en_zg, 4, '现在进行时',
'## 现在进行时\n\nbe + V-ing，表示正在进行的动作。\n\n例句:\n1. Listen! She **is singing**.\n2. They **are playing** now.\n3. He **is finishing** his homework.\n\n提示:"Listen!/Look!/now 提示现在进行时: be + V-ing"', 'tense', 4, 4),
(@tense_id, @en_zg, 4, '过去进行时',
'## 过去进行时\n\nwas/were + V-ing，表示过去某时刻正进行的动作。\n\n例句:\n1. I **was watching** TV when she called.\n2. They **were playing** at 5pm.\n\n提示:"was/were + V-ing，过去某时间正在进行的动作"', 'tense', 5, 5),
(@tense_id, @en_zg, 4, '现在完成时',
'## 现在完成时\n\nhave/has + 过去分词。\n\n例句:\n1. He **has finished** his homework.\n2. They **have played** this game before.\n3. I **have studied** English for 3 years.\n\n提示:"already/ever/never/for+时段 提示现在完成时: have/has+过去分词"', 'tense', 5, 6),
(@tense_id, @en_zg, 4, '过去完成时',
'## 过去完成时\n\nhad + 过去分词。过去的过去。\n\n例句:\n1. She **had finished** homework before dinner.\n2. They **had left** when I arrived.\n\n提示:"过去的过去 → had + 过去分词"', 'tense', 7, 7);

-- 4. 被动语态子节点 (3 个)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, content, grammar_category, unlock_stage, sort_order) VALUES
(@passive_id, @en_zg, 4, '一般现在时被动', '## 一般现在时被动\n\nam/is/are + 过去分词\n\n例句:\n1. English **is spoken** around the world.\n2. The room **is cleaned** every day.', 'passive', 5, 1),
(@passive_id, @en_zg, 4, '一般过去时被动', '## 一般过去时被动\n\nwas/were + 过去分词\n\n例句:\n1. The window **was broken** by a ball.\n2. These houses **were built** last year.', 'passive', 5, 2),
(@passive_id, @en_zg, 4, '情态动词被动', '## 情态动词被动\n\ncan/must/should + be + 过去分词\n\n例句:\n1. The work **must be finished** today.\n2. Rules **should be followed**.', 'passive', 5, 3);

-- 5. 从句子节点 (3 个)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, content, grammar_category, unlock_stage, sort_order) VALUES
(@clause_id, @en_zg, 4, '宾语从句', '## 宾语从句\n\nthat/whether/wh-词引导。\n\n例句:\n1. I know **that he is right**.\n2. She asked **if I could help**.', 'clause', 6, 1),
(@clause_id, @en_zg, 4, '定语从句', '## 定语从句\n\nwho/which/that 引导。\n\n例句:\n1. The man **who is standing** there is my teacher.\n2. The book **that I bought** is interesting.', 'clause', 6, 2),
(@clause_id, @en_zg, 4, '状语从句', '## 状语从句\n\nwhen/if/because/although 引导。\n\n例句:\n1. **If it rains**, we will stay home.\n2. I was sleeping **when she called**.', 'clause', 6, 3);

-- 6. 非谓语子节点 (2 个)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, content, grammar_category, unlock_stage, sort_order) VALUES
(@nonfin_id, @en_zg, 4, '不定式', '## 不定式\n\nto + 动词原形。\n\n例句:\n1. I want **to go** home.\n2. **To study** is important.', 'non_finite', 7, 1),
(@nonfin_id, @en_zg, 4, '动名词', '## 动名词\n\nV-ing 作名词用。\n\n例句:\n1. **Swimming** is good for health.\n2. He enjoys **playing** basketball.', 'non_finite', 7, 2);

-- 7. 词法子节点 (4 个)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, content, grammar_category, unlock_stage, sort_order) VALUES
(@lexical_id, @en_zg, 4, '冠词 a/an/the', '## 冠词\n\n例句:\n1. There is **a** university.\n2. **The** sun rises in the east.\n3. He is **an** honest man.', 'lexical', 3, 1),
(@lexical_id, @en_zg, 4, '介词搭配', '## 介词搭配\n\n例句:\n1. He is good **at** playing.\n2. I am interested **in** music.\n3. She is afraid **of** dogs.', 'lexical', 3, 2),
(@lexical_id, @en_zg, 4, '比较级和最高级', '## 比较级最高级\n\n例句:\n1. This book is **more interesting** than that.\n2. He is **the tallest** in class.', 'lexical', 4, 3),
(@lexical_id, @en_zg, 4, '情态动词', '## 情态动词\n\ncan/must/may/should + 动词原形\n\n例句:\n1. You **mustn\'t** smoke here.\n2. **Can** you help me?\n3. We **should** study hard.', 'lexical', 4, 4);

-- 8. 句型子节点 (3 个)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, content, grammar_category, unlock_stage, sort_order) VALUES
(@sent_id, @en_zg, 4, 'there be 句型', '## there be\n\n表示某处有某物。\n\n例句:\n1. **Is there** any milk?\n2. There **are** many students.', 'sentence', 4, 1),
(@sent_id, @en_zg, 4, '祈使句', '## 祈使句\n\n动词原形开头。\n\n例句:\n1. **Open** the door please.\n2. **Don\'t** be late.\n3. **Let\'s** go home.', 'sentence', 4, 2),
(@sent_id, @en_zg, 4, '反意疑问句', '## 反意疑问句\n\n前肯后否·前否后肯。\n\n例句:\n1. He can swim, **can\'t he**?\n2. You like it, **don\'t you**?', 'sentence', 5, 3);
