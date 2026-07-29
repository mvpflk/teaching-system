-- ============================================================
-- v91: 英语[职高] 知识树种子数据
-- 父节点: id=12 (英语[职高], subjectId=24)
-- 结构: 词汇+语法+阅读+翻译+写作 = 5 个 level=2 章节
-- ⚠️ 执行前必须 SET NAMES utf8mb4，防止中文乱码
-- ============================================================
SET NAMES utf8mb4;

SET @eng_root_id = 12;
SET @eng_subject_id = 24;  -- dict_subject.id for 英语[职高]

-- ══════════════════════════════════════════
-- Level 2: 章节（5 个）
-- ══════════════════════════════════════════

INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@eng_root_id, @eng_subject_id, 2, '词汇积累',  1, 'ACTIVE'),
(@eng_root_id, @eng_subject_id, 2, '语法专项',  2, 'ACTIVE'),
(@eng_root_id, @eng_subject_id, 2, '阅读理解',  3, 'ACTIVE'),
(@eng_root_id, @eng_subject_id, 2, '翻译',      4, 'ACTIVE'),
(@eng_root_id, @eng_subject_id, 2, '写作',      5, 'ACTIVE');

-- ══════════════════════════════════════════
-- 词汇积累 (sort=1)
-- ══════════════════════════════════════════
SET @_voc_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@eng_root_id AND name='词汇积累' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_voc_id, @eng_subject_id, 3, '高频核心300词', 1, 'ACTIVE'),
(@_voc_id, @eng_subject_id, 3, '考试核心500词', 2, 'ACTIVE'),
(@_voc_id, @eng_subject_id, 3, '考纲拓展词',   3, 'ACTIVE');

-- 词汇 level-4 细分
SET @_voc_lv3_1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_voc_id AND name='高频核心300词');
SET @_voc_lv3_2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_voc_id AND name='考试核心500词');
SET @_voc_lv3_3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@_voc_id AND name='考纲拓展词');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_voc_lv3_1, @eng_subject_id, 4, '动词类（be/have/do/make/take/go/get）[基础]', 1, 'ACTIVE'),
(@_voc_lv3_1, @eng_subject_id, 4, '名词类（time/way/day/people/place）[基础]', 2, 'ACTIVE'),
(@_voc_lv3_1, @eng_subject_id, 4, '形容词副词类（good/bad/big/small/well）[基础]', 3, 'ACTIVE'),
(@_voc_lv3_1, @eng_subject_id, 4, '介词连词类（in/on/at/to/for/and/but）[基础]', 4, 'ACTIVE'),
(@_voc_lv3_2, @eng_subject_id, 4, '动词辨析（spend/cost/take/pay/offer/provide）[中等]', 1, 'ACTIVE'),
(@_voc_lv3_2, @eng_subject_id, 4, '名词辨析（chance/opportunity/ability/advantage）[中等]', 2, 'ACTIVE'),
(@_voc_lv3_2, @eng_subject_id, 4, '词组搭配（take care of/look forward to/be used to）[中等]', 3, 'ACTIVE'),
(@_voc_lv3_3, @eng_subject_id, 4, '阅读拓展词 [了解]', 1, 'ACTIVE'),
(@_voc_lv3_3, @eng_subject_id, 4, '写作常用词 [理解]', 2, 'ACTIVE');

-- ══════════════════════════════════════════
-- 语法专项 (sort=2) — 最大模块
-- ══════════════════════════════════════════
SET @_gram_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@eng_root_id AND name='语法专项' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_gram_id, @eng_subject_id, 3, '时态语态',     1, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '非谓语动词',   2, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '定语从句',     3, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '名词性从句',   4, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '状语从句',     5, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '主谓一致',     6, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '情态动词',     7, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '虚拟语气',     8, 'ACTIVE'),
(@_gram_id, @eng_subject_id, 3, '情景交际',     9, 'ACTIVE');

-- 语法 level-4 知识点
SET @_g1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='时态语态');
SET @_g2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='非谓语动词');
SET @_g3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='定语从句');
SET @_g4=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='名词性从句');
SET @_g5=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='状语从句');
SET @_g6=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='主谓一致');
SET @_g7=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='情态动词');
SET @_g8=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='虚拟语气');
SET @_g9=(SELECT id FROM knowledge_nodes WHERE parent_id=@_gram_id AND name='情景交际');

INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_g1,@eng_subject_id,4,'一般现在时 [基础]',   1,'ACTIVE'),
(@_g1,@eng_subject_id,4,'一般过去时 [基础]',   2,'ACTIVE'),
(@_g1,@eng_subject_id,4,'一般将来时 [基础]',   3,'ACTIVE'),
(@_g1,@eng_subject_id,4,'现在进行时 [基础]',   4,'ACTIVE'),
(@_g1,@eng_subject_id,4,'过去进行时 [中等]',   5,'ACTIVE'),
(@_g1,@eng_subject_id,4,'现在完成时 [困难]',   6,'ACTIVE'),
(@_g1,@eng_subject_id,4,'现在完成时-for/since区别 [困难]', 7,'ACTIVE'),
(@_g1,@eng_subject_id,4,'过去完成时 [困难]',   8,'ACTIVE'),
(@_g1,@eng_subject_id,4,'被动语态 [中等]',     9,'ACTIVE'),
(@_g2,@eng_subject_id,4,'动词不定式 [困难]',   1,'ACTIVE'),
(@_g2,@eng_subject_id,4,'动名词 [中等]',       2,'ACTIVE'),
(@_g2,@eng_subject_id,4,'分词作定语和状语 [困难]',3,'ACTIVE'),
(@_g3,@eng_subject_id,4,'关系代词 that/which/who [中等]',1,'ACTIVE'),
(@_g3,@eng_subject_id,4,'关系副词 when/where/why [中等]',2,'ACTIVE'),
(@_g3,@eng_subject_id,4,'非限制性定语从句 [困难]',3,'ACTIVE'),
(@_g4,@eng_subject_id,4,'宾语从句 [中等]',     1,'ACTIVE'),
(@_g4,@eng_subject_id,4,'主语从句 [困难]',     2,'ACTIVE'),
(@_g4,@eng_subject_id,4,'表语从句与同位语从句 [困难]',3,'ACTIVE'),
(@_g5,@eng_subject_id,4,'时间/条件/原因状语从句 [中等]',1,'ACTIVE'),
(@_g5,@eng_subject_id,4,'让步/目的状语从句 [中等]',2,'ACTIVE'),
(@_g6,@eng_subject_id,4,'就近原则与意义一致 [基础]',1,'ACTIVE'),
(@_g7,@eng_subject_id,4,'情态动词基本用法 can/must/may [基础]',1,'ACTIVE'),
(@_g7,@eng_subject_id,4,'情态动词推测用法 [中等]',2,'ACTIVE'),
(@_g8,@eng_subject_id,4,'虚拟语气在条件句中的用法 [困难]',1,'ACTIVE'),
(@_g9,@eng_subject_id,4,'邀请与请求 [基础]',1,'ACTIVE'),
(@_g9,@eng_subject_id,4,'感谢与道歉 [基础]',2,'ACTIVE'),
(@_g9,@eng_subject_id,4,'问路与指路 [基础]',3,'ACTIVE'),
(@_g9,@eng_subject_id,4,'建议与劝告 [基础]',4,'ACTIVE');

-- ══════════════════════════════════════════
-- 阅读理解 (sort=3)
-- ══════════════════════════════════════════
SET @_read_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@eng_root_id AND name='阅读理解' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_read_id, @eng_subject_id, 3, '细节理解题',  1, 'ACTIVE'),
(@_read_id, @eng_subject_id, 3, '推理判断题',  2, 'ACTIVE'),
(@_read_id, @eng_subject_id, 3, '主旨大意题',  3, 'ACTIVE'),
(@_read_id, @eng_subject_id, 3, '词义猜测题',  4, 'ACTIVE');
SET @_r1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_read_id AND name='细节理解题');
SET @_r2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_read_id AND name='推理判断题');
SET @_r3=(SELECT id FROM knowledge_nodes WHERE parent_id=@_read_id AND name='主旨大意题');
SET @_r4=(SELECT id FROM knowledge_nodes WHERE parent_id=@_read_id AND name='词义猜测题');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_r1,@eng_subject_id,4,'直接定位文中信息 [基础]',1,'ACTIVE'),
(@_r1,@eng_subject_id,4,'Wh-问题细节检索 [基础]',2,'ACTIVE'),
(@_r2,@eng_subject_id,4,'根据上下文推断隐含意思 [中等]',1,'ACTIVE'),
(@_r3,@eng_subject_id,4,'概括段落/全文大意 [中等]',1,'ACTIVE'),
(@_r4,@eng_subject_id,4,'根据上下文猜测生词含义 [基础]',1,'ACTIVE');

-- ══════════════════════════════════════════
-- 翻译 (sort=4)
-- ══════════════════════════════════════════
SET @_trans_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@eng_root_id AND name='翻译' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_trans_id, @eng_subject_id, 3, '英译汉', 1, 'ACTIVE'),
(@_trans_id, @eng_subject_id, 3, '汉译英', 2, 'ACTIVE');
SET @_tr1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trans_id AND name='英译汉');
SET @_tr2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_trans_id AND name='汉译英');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_tr1,@eng_subject_id,4,'关键词语准确理解 [基础]',1,'ACTIVE'),
(@_tr1,@eng_subject_id,4,'句式结构分析 [中等]',2,'ACTIVE'),
(@_tr2,@eng_subject_id,4,'基础词汇运用 [基础]',1,'ACTIVE'),
(@_tr2,@eng_subject_id,4,'基本句式构建 [中等]',2,'ACTIVE');

-- ══════════════════════════════════════════
-- 写作 (sort=5)
-- ══════════════════════════════════════════
SET @_write_id = (SELECT id FROM knowledge_nodes WHERE parent_id=@eng_root_id AND name='写作' AND level=2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_write_id, @eng_subject_id, 3, '应用文写作', 1, 'ACTIVE'),
(@_write_id, @eng_subject_id, 3, '话题写作',   2, 'ACTIVE');
SET @_w1=(SELECT id FROM knowledge_nodes WHERE parent_id=@_write_id AND name='应用文写作');
SET @_w2=(SELECT id FROM knowledge_nodes WHERE parent_id=@_write_id AND name='话题写作');
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
(@_w1,@eng_subject_id,4,'书信格式与常用表达 [掌握]',1,'ACTIVE'),
(@_w1,@eng_subject_id,4,'通知类写作 [掌握]',2,'ACTIVE'),
(@_w2,@eng_subject_id,4,'观点表达与逻辑衔接 [掌握]',1,'ACTIVE');
