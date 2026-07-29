-- ============================================================================
-- v153: 英语[职高] 知识文章 — 基于考纲填充全部53个L4节点
-- 来源：docs/content/英语[职高]考纲.md
-- 幂等：INSERT IGNORE 可重复执行
-- ============================================================================

SET NAMES utf8mb4;

-- ======================================================================
-- 第一篇：词汇积累 > 高频核心300词 (4个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='动词类（be/have/do/make/take/go/get）[基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '高频核心300词', '动词类（be/have/do/make/take/go/get）',
'### 概述\n\n动词是英语句子的核心，掌握**be/have/do/make/take/go/get**这七个高频动词的用法和搭配，是词汇积累的第一步。考纲要求约2200词，其中基础词汇1700词，这七个动词及其搭配是重中之重。\n\n### 核心动词用法\n\n- **be**：系动词"是/在"——I am a student. / She is at home.\n- **have**：有/吃/进行——have breakfast / have a meeting / have to do\n- **do**：做/助动词——do homework / do business / do well in\n- **make**：制作/使——make coffee / make a decision / make sb. do\n- **take**：拿/花费/乘坐——take a book / take a bus / take care of\n- **go**：去/变成——go to school / go shopping / go bad\n- **get**：得到/到达/变得——get up / get to / get married\n\n### 要点总结\n\n1. 这些动词常与介词搭配形成固定短语（如 take care of, get along with），考试中短语搭配是高频考点\n2. 注意一词多义：take可表"拿/花费/乘坐/参加"（take a test），需根据语境判断\n3. be/do/have也可作助动词构成时态和否定疑问句，务必区分实义与助动词用法',
'动词是英语句子的核心。掌握 be/have/do/make/take/go/get 这七个高频动词的用法和搭配是词汇积累的第一步。',
@l4, 1, '["基础","动词类","核心词汇"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='名词类（time/way/day/people/place）[基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '高频核心300词', '名词类（time/way/day/people/place）',
'### 概述\n\n名词是英语词汇的基石，time/way/day/people/place 等高频名词在日常交际和考试中频繁出现。理解这些词的多重含义和习惯搭配，是提升阅读和写作能力的基础。\n\n### 核心名词用法\n\n- **time**：时间/次数/时代——What time is it? / three times / at the same time\n- **way**：方式/方法/路——in this way / on the way to / by the way\n- **day**：天/日子/节日——every day / one day / National Day\n- **people**：人们/民族——many people / the Chinese people\n- **place**：地方/位置——take place / in the first place / places of interest\n\n### 要点总结\n\n1. 时间介词搭配易错：in the morning / on Sunday / at 7 o''clock — at/in/on 要分清\n2. people 本身就是复数，不加 -s；作"民族"解时可加 -s（peoples）\n3. "the way to do"与"the way of doing"均可，考试中注意固定短语 on one''s way to',
'名词是英语词汇的基石。time/way/day/people/place 等高频名词在日常交际和考试中频繁出现，理解多重含义和习惯搭配是提升阅读和写作的基础。',
@l4, 1, '["基础","名词类","核心词汇"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='形容词副词类（good/bad/big/small/well）[基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '高频核心300词', '形容词副词类（good/bad/big/small/well）',
'### 概述\n\n形容词和副词是英语修饰语的核心类别。形容词修饰名词（a good book），副词修饰动词/形容词/句子（run fast, very good）。掌握常见形容词副词的比较级、最高级及特殊变化是考试必考内容。\n\n### 核心修饰词用法\n\n- **good/well**：好——good（形）修饰名词；well（副）修饰动词，also well作形容词表"身体好"\n- **big/small**：大/小——比较级 bigger/smaller，最高级 biggest/smallest\n- **much/many**：多——much+不可数名词，many+可数名词；so many/so much搭配\n- **bad**：坏的——比较级 worse，最高级 worst（不规则变化，易错点）\n\n### 要点总结\n\n1. 形容词→副词变化规则：一般加 -ly（careful→carefully），辅音+y改y为i加 -ly（happy→happily）\n2. good/well 的比较级和最高级相同：better/best\n3. 系动词后接形容词（feel happy, look beautiful），行为动词后接副词（run quickly）',
'形容词和副词是英语修饰语的核心类别。掌握比较级/最高级及特殊变化是考试必考内容。good/well 的比较级同为 better/best。',
@l4, 1, '["基础","形容词类","副词类","核心词汇"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='介词连词类（in/on/at/to/for/and/but）[基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '高频核心300词', '介词连词类（in/on/at/to/for/and/but）',
'### 概述\n\n介词和连词是英语的"胶水词"，连接单词、短语和句子。介词表示时间、地点、方向等关系；连词连接并列结构或引导从句。它们是单项选择和短文改错的必考点。\n\n### 核心虚词用法\n\n- **in/on/at**：时间介词——in（月份/年份/上午下午）, on（星期/具体某天）, at（钟点/小地点）\n- **to/for**：方向/目的——go to school / come to / for you / look for\n- **and/but**：并列/转折——I like apples and oranges. / She is young but very smart.\n- **because**：原因连词——because + 句子（注意不能与 so 同时使用）\n- **of/with**：所属/伴随——a photo of my family / a girl with long hair\n\n### 要点总结\n\n1. 介词固定搭配是高频考点（look at, wait for, depend on, be good at）\n2. because 和 so 不能连用，"因为…所以…"只能用其一\n3. 单项选择中注意排除混淆项：between(两者之间)/among(三者及以上)',
'介词和连词是英语的"胶水词"。in/on/at 表时间要分清，because 和 so 不能同时使用。介词 + 动词形成固定搭配是高频考点。',
@l4, 1, '["基础","介词类","连词类","核心词汇"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第一篇：词汇积累 > 考试核心500词 (3个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='动词辨析（spend/cost/take/pay/offer/provide）[中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '考试核心500词', '动词辨析（spend/cost/take/pay/offer/provide）',
'### 概述\n\n易混淆动词辨析是对口升学考试的高频考点。spend/cost/take/pay 都含"花费"意，但用法不同；offer/provide/supply 都含"提供"意，但搭配各异。掌握这些差异是词汇辨析题的关键。\n\n### 核心辨析对比\n\n| 动词 | 含义 | 常用句型 | 例子 |\n|------|------|----------|------|\n| **spend** | 花费（时间/金钱） | sb. spend ... on sth / (in) doing | I spent 10 yuan on the book. |\n| **cost** | 花费（金钱） | sth. cost sb. ... | The book cost me 10 yuan. |\n| **take** | 花费（时间） | It takes sb. time to do | It took me 2 hours to finish. |\n| **pay** | 支付（金钱） | sb. pay ... for sth. | I paid 10 yuan for the book. |\n| **offer** | 提供（主动） | offer to do / offer sb. sth. | He offered to help me. |\n| **provide** | 提供（供给） | provide sb. with sth. / provide sth. for sb. | The school provides us with books. |\n\n### 要点总结\n\n1. spend 的主语是人，cost/take 的主语是物/事，pay 的主语是人\n2. offer to do 表"主动提出做某事"，不定式不可省略\n3. say/tell/speak/talk 辨析：say+内容，tell+人，speak+语言，talk+about/to',
'易混淆动词辨析是高配考点。spend/cost/take/pay 都含"花费"意但主语和句型不同；offer/provide/supply 的搭配结构各不相同。',
@l4, 2, '["中等","动词辨析","核心词汇"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='名词辨析（chance/opportunity/ability/advantage）[中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '考试核心500词', '名词辨析（chance/opportunity/ability/advantage）',
'### 概述\n\n抽象名词辨析在完形填空和词汇选择题中出现频率较高。chance/opportunity/ability/advantage 等词含义相近但用法不同，理解其细微差异有助于精准选词。\n\n### 核心辨析对比\n\n- **chance**：机会/可能性——by chance（偶然）/ have a chance to do / There is a chance that…\n- **opportunity**：机遇——take the opportunity to do / golden opportunity（良机，比 chance 更正式）\n- **ability**：能力——have the ability to do（不定式作定语，不可用 of doing）\n- **advantage**：优势——take advantage of（利用）/ have an advantage over（比…有优势）\n- **problem/question**：problem 指难题（solve the problem），question 指待答的问题（answer the question）\n\n### 要点总结\n\n1. ability 后接 to do 而非 of doing — 这是常见改错考点\n2. chance 和 opportunity 常可互换，但 chance 更多指"偶然性"，opportunity 指"好时机"\n3. take advantage of 是固定搭配，考试中常考其宾语位置',
'抽象名词辨析在完形填空中频率较高。ability 后接 to do，chance 与 opportunity 的区别需注意，take advantage of 为固定搭配。',
@l4, 2, '["中等","名词辨析","核心词汇"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='词组搭配（take care of/look forward to/be used to）[中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '考试核心500词', '词组搭配（take care of/look forward to/be used to）',
'### 概述\n\n动词词组搭配是对口升学单选的必考内容。掌握常见动词+介词/副词的固定搭配，注意 look forward to / be used to 中的 to 是介词而非不定式符号，后面接动词-ing形式。\n\n### 核心词组搭配\n\n- **take care of** = look after（照顾）——She takes care of her grandmother.\n- **look forward to**（期待）——I look forward to hearing from you.（to 是介词，接 V-ing）\n- **be used to**（习惯于）——He is used to getting up early.（to 是介词）\n- **give up**（放弃）——give up smoking / give up doing\n- **turn on/off/up/down**（开/关/调大/调小）——turn on the TV\n- **borrow/lend**：borrow from（借入）/ lend to（借出）\n- **bring/take**：bring 带来 / take 带走\n\n### 要点总结\n\n1. look forward to / be used to / pay attention to / lead to — 这些短语中的 to 都是介词，后接 V-ing\n2. turn on/off 的代词宾语放中间：turn it on（不能说 turn on it）\n3. borrow/lend 和 bring/take 是常考的易混淆词对，注意方向性',
'动词词组搭配是单选必考。注意 look forward to / be used to 中的 to 是介词，后接 V-ing。turn on/off 的代词宾语放中间。',
@l4, 2, '["中等","词组搭配","核心词汇"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第一篇：词汇积累 > 考纲拓展词 (2个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='阅读拓展词 [了解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '考纲拓展词', '阅读拓展词',
'### 概述\n\n考纲标注"*"的拓展词汇约500词，主要在阅读篇章中出现。这些词汇不需要拼写，但要求能识别含义，不影响阅读理解。掌握这些词有助于提高阅读速度和准确度。\n\n### 常见阅读拓展词分类\n\n- **科技类**：technology, electronic, digital, device, software, network, data\n- **环境类**：environment, pollution, recycle, climate, natural, energy\n- **社会文化类**：experience, communicate, relationship, culture, traditional, celebrate\n- **人物情感类**：encourage, manage, achieve, appreciate, responsible, independent\n\n### 阅读策略\n\n- 遇到生词时先判断是否影响理解主干→如不影响可跳过\n- 利用上下文（定义/举例/反义线索）推断词义\n- 关注词根词缀：un-（不）, re-（再）, -tion（名词）, -ly（副词）\n\n### 要点总结\n\n1. 拓展词以识记为主，不需要拼写，重点是能在阅读中快速反应含义\n2. 词根词缀法是猜词最有效的方法，考纲词的词缀通常不超出基础范围\n3. 阅读中反复出现的拓展词建议记入个人词汇本积累',
'考纲标注"*"的拓展词汇约500词，主要在阅读篇章中出现。不需要拼写，要求能快速识别含义以不影响阅读理解。',
@l4, 1, '["基础","阅读拓展","核心词汇"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='写作常用词 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '词汇积累', '考纲拓展词', '写作常用词',
'### 概述\n\n写作常用词是用于书信、通知、话题短文中的高频表达。掌握这些词能在写作中准确表达观点，提升文章档次。书面表达15分，词汇运用能力直接影响得分。\n\n### 写作高频表达\n\n- **书信常用**：sincerely（真诚地）, looking forward to（期待）, hear from（收到来信）, reply（回复）\n- **观点表达**：in my opinion（在我看来）, I think/believe（我认为）, as far as I''m concerned（就我而言）\n- **逻辑衔接**：first/besides/finally（首先/此外/最后）, however（然而）, therefore（因此）, in short（总之）\n- **描述用语**：be interested in（对…感兴趣）, be good at（擅长）, take part in（参加）\n\n### 要点总结\n\n1. 写作中避免重复使用同一词，学会同义替换，如 important→significant, many→a number of\n2. 连接词是写作评分的重要依据，每段开头用 First / Besides / Finally 使结构清晰\n3. 书信和通知各有固定套语，建议将它们背上并熟练套用',
'写作常用词用于书信、通知、话题短文。掌握 sincerely, looking forward to, in my opinion 等高频表达，能提升书面表达能力。',
@l4, 2, '["中等","写作词汇","核心词汇"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 时态语态 (9个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='一般现在时 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '一般现在时',
'### 概述\n\n一般现在时表示经常性/习惯性动作或客观真理。它是英语九大时态的基础，也是对口升学必考内容，出现在单项选择和短文改错中。\n\n### 核心用法\n\n- **习惯性动作**：I get up at 7 every day.（常与 always/often/every day 连用）\n- **客观真理**：The sun rises in the east.（永恒不变的事实）\n- **主语三单**：He/She/It 作主语时谓语动词加 -s/-es——She likes music.\n- **动词变化规则**：一般加 -s（like→likes）；ch/sh/s/x/o+es（go→goes, watch→watches）\n\n### 要点总结\n\n1. 一般现在时的否定/疑问句用助动词 do/does 构成，三单用 does 后动词恢复原形\n2. 频度副词 always/often/sometimes/never 放在行为动词之前、be动词之后\n3. 复合句中主将从现的规则也涉及一般现在时（If it rains, I will stay at home.）',
'一般现在时表示经常性/习惯性动作或客观真理。三单加 -s/-es，否定/疑问句用助动词 do/does。频度副词位于行为动词前。',
@l4, 1, '["基础","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='一般过去时 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '一般过去时',
'### 概述\n\n一般过去时表示过去发生的动作或存在的状态，是记叙文中使用最多的时态。动词用过去式，规则动词加 -ed，不规则动词需单独记忆。\n\n### 核心用法\n\n- **过去动作**：I visited my grandparents yesterday. 常与 yesterday/last week/in 2023/ago 连用\n- **过去状态**：She was very happy when she heard the news.\n- **规则动词过去式**：直接加 -ed（work→worked）；以 e 结尾加 -d（like→liked）；辅音+y 改 y 为 i 加 -ed（study→studied）；重读闭音节双写尾字母加 -ed（stop→stopped）\n- **不规则动词**：go→went, have→had, do→did, make→made, take→took, get→got, see→saw\n\n### 要点总结\n\n1. 一般过去时的标志词是明确过去时间：yesterday / last week / two days ago / in 1998\n2. 不规则动词过去式是改错高频考点，建议按组记忆（sing→sang→sung）\n3. 短文改错中常考时态一致性：全文基调是过去时就不能突然切换为现在时',
'一般过去时表示过去发生的动作，用动词过去式。规则动词加 -ed，不规则动词需单独记忆。标志词：yesterday / last week / ago。',
@l4, 1, '["基础","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='一般将来时 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '一般将来时',
'### 概述\n\n一般将来时表示将来要发生的动作或存在的状态。will/shall + 动词原形和 be going to + 动词原形是两种主要表达方式。\n\n### 核心用法\n\n- **will + 动词原形**：表示临时决定或预测——I will help you. / It will rain tomorrow.\n- **be going to + 动词原形**：表示计划打算或已有迹象——I am going to buy a new bike. / Look at the clouds, it is going to rain.\n- **其他将来表达**：be about to do（即将）, be to do（按计划将发生）\n- **标志词**：tomorrow / next week / in the future / soon / this evening\n\n### 要点总结\n\n1. will 的否定式为 will not（缩写 won''t），一般疑问句将 will 提前\n2. 条件/时间状语从句中"主将从现"：主句用将来时，从句用一般现在时\n3. be going to 表"计划"，will 表"临时决定"，这是考纲要求的区分重点',
'一般将来时表示将来动作，用 will/be going to + 动词原形。will 表临时决定，be going to 表计划或已有迹象。主将从现规则重要。',
@l4, 1, '["基础","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='现在进行时 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '现在进行时',
'### 概述\n\n现在进行时表示此刻正在进行的动作或现阶段持续的状态。结构为：be (am/is/are) + 现在分词（V-ing）。\n\n### 核心用法\n\n- **此时此刻正在做**：I am reading a book now.（常与 now/at the moment/listen/look 连用）\n- **现阶段持续（不一定此刻在做）**：He is preparing for the exam these days.\n- **动词加 -ing 规则**：直接加 -ing（do→doing）；以不发音 e 结尾去 e 加 -ing（have→having）；重读闭音节双写尾字母（sit→sitting, swim→swimming）\n- **不用进行时的动词**：know, like, want, need, belong（状态/情感/所属类动词）\n\n### 要点总结\n\n1. 现在进行时标志词：now, at present, at the moment, these days\n2. 现在分词拼写是改错考点——write→writing, run→running, lie→lying\n3. 区分一般现在时表"经常"与现在进行时表"此刻正在"——对比：He reads books every day. / He is reading books now.',
'现在进行时表此刻正在进行的动作，结构为 be + V-ing。标志词：now / at the moment。注意不加 -ing 的状态动词和分词拼写规则。',
@l4, 1, '["基础","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='过去进行时 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '过去进行时',
'### 概述\n\n过去进行时表示过去某一时刻或某段时间正在进行的动作。结构为：was/were + 现在分词（V-ing），常与一般过去时配合使用，用于"一个动作发生时另一个动作正在进行"。\n\n### 核心用法\n\n- **过去某一时刻正在做**：I was watching TV at 8 last night.\n- **与一般过去时连用**：When I was reading, the telephone rang.（长动作进行时被短动作打断）\n- **过去某段时间持续**：They were building the bridge last year.\n\n### 要点总结\n\n1. 过去进行时标志词：at that time / at 7 yesterday / when + 过去进行时\n2. when 后常用一般过去时（短动作），while 后常用过去进行时（持续动作）\n3. 写作中过去进行时使叙述更生动，如 The sun was shining and birds were singing.',
'过去进行时表过去某时刻正在进行的动作，结构为 was/were + V-ing。常与一般过去时对应用（长动作被短动作打断）。',
@l4, 2, '["中等","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='现在完成时 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '现在完成时',
'### 概述\n\n现在完成时表示过去发生的动作对现在造成影响，或过去开始持续到现在的动作。结构为：have/has + 过去分词。这是对口升学语法单选题的难点和高频考点。\n\n### 核心用法\n\n- **已完成（对现在有影响）**：I have lost my key.（现在找不到了）\n- **未完成（持续至今）**：I have lived here for 10 years.\n- **经历经验**：Have you ever been to Beijing?\n- **标志词**：already/yet（已经/还）, ever/never（曾经/从未）, just（刚刚）, recently（最近）, so far（到目前为止）\n\n### 要点总结\n\n1. have been to（去过已回）vs have gone to（去了未回）是高频考点\n2. 现在完成时与一般过去时的区别：完成时强调"对现在的影响"，过去时强调"动作发生在过去"\n3. 常用句型：It''s the first time that + 现在完成时；This is the best film that I have ever seen.',
'现在完成时表过去动作对现在的影响或持续至今。结构为 have/has+过去分词。have been to 与 have gone to 的区别是高频考点。',
@l4, 3, '["困难","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='现在完成时-for/since区别 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '现在完成时-for/since区别',
'### 概述\n\nfor 和 since 都可用于现在完成时表"持续"，但用法不同。for + 时间段，since + 时间点/从句。这是对口升学语法必考点，也是学生易混淆的难点。\n\n### 核心区别\n\n- **for + 时间段**：for two years / for a long time / for three days\n- **since + 时间点/从句**：since 2020 / since last week / since I came here\n- **提问用 how long**：How long have you studied English? — For 3 years. / Since 2022.\n- **延续性动词**：现在完成时与 for/since 连用时，动词必须是延续性的——He has been a teacher for 10 years.（√）He has become a teacher for 10 years.（×）\n\n### 常见非延续动词→延续动词转换\n\n| 非延续（短暂） | 延续性替换 |\n|---------------|-----------|\n| buy → | have / own |\n| borrow → | keep |\n| leave → | be away (from) |\n| join → | be a member of / be in |\n| die → | be dead |\n| begin/start → | be on |\n\n### 要点总结\n\n1. for 和 since 的区分是单选必考点：for + 时段，since + 时点\n2. 非延续动词不能与 for/since 连用，必须转换为延续性表达\n3. 否定句中非延续动词可与 for 连用：I haven''t bought anything for months.',
'for + 时间段，since + 时间点/从句。这是对口升学必考点。注意非延续动词（buy/borrow/leave）需转换为延续性表达才能与 for/since 连用。',
@l4, 3, '["困难","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='过去完成时 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '过去完成时',
'### 概述\n\n过去完成时表示"过去的过去"，即在过去某一时间点之前已经完成的动作。结构为：had + 过去分词。它常用于复合句中说明两个过去动作的先后顺序。\n\n### 核心用法\n\n- **过去的过去**：When I arrived, the train had already left.（"离开"发生在"到达"之前）\n- **与 by/before 连用**：By the end of last year, we had learned 2000 words.\n- **no sooner...than / hardly...when**：表示"刚…就…"——No sooner had I sat down than the phone rang.（倒装结构）\n\n### 要点总结\n\n1. 过去完成时的判定关键是两个过去动作的先后顺序，较早的一个用过去完成时\n2. by + 过去时间点常用过去完成时：by 5 o''clock yesterday / by last month\n3. 短文改错中注意：如果没有明确的"过去的过去"对比，不应滥用过去完成时',
'过去完成时表示"过去的过去"，结构为 had+过去分词。判定关键是有两个过去动作，较早一个用过去完成时。by+过去时间点是标志。',
@l4, 3, '["困难","时态","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='被动语态 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '时态语态', '被动语态',
'### 概述\n\n被动语态表示主语是动作的承受者。结构为：be + 过去分词。被动语态在各时态中都有体现，是中频考点，出现在单选和改错中。\n\n### 各时态被动语态\n\n| 时态 | 主动 | 被动（be+done） |\n|------|------|----------------|\n| 一般现在 | write | am/is/are written |\n| 一般过去 | wrote | was/were written |\n| 一般将来 | will write | will be written |\n| 现在完成 | have written | have/has been written |\n| 情态动词 | can write | can be written |\n\n### 核心用法\n\n- **动作的执行者不明确或不重要**：English is spoken all over the world.\n- **主动表被动**：need/require + V-ing = need to be done（The car needs repairing. = The car needs to be repaired.）\n\n### 要点总结\n\n1. 被动语态的 be 动词随主语人称/数和时态变化，过去分词不变\n2. 不及物动词无被动语态：happen / appear / rise / die（没有 be happened）\n3. 感官动词（feel/taste/smell/look）+ 形容词表主动（The food tastes good.）',
'被动语态表示主语是动作承受者，结构为 be+过去分词。各时态的被动形式不同。不及物动词无被动，感官动词常用主动表被动。',
@l4, 2, '["中等","语态","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 非谓语动词 (3个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='动词不定式 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '非谓语动词', '动词不定式',
'### 概述\n\n动词不定式（to do）是非谓语动词的一种，可在句中作主语、宾语、宾语补足语、定语和状语。掌握不定式的功能和句型结构是语法专项的难点。\n\n### 核心用法\n\n- **作主语**：To learn English is important. / It is important to learn English.（it 作形式主语）\n- **作宾语**：I want to go. / He decided to leave. 常见后接 to do 的动词：want, hope, decide, plan, wish, agree\n- **作目的状语**：He came to borrow my book.\n- **作宾语补足语**：ask/tell/want/expect sb. to do\n- **作后置定语**：I have something to do.\n\n### 要点总结\n\n1. 感官动词（see/watch/hear）和使役动词（let/make/have）后接 to do 时省略 to——I saw him cross the street.\n2. It + be + adj. + to do 句型必考，it 作形式主语\n3. too...to（太…而不能）和 enough to（足够…去做）是高频结构——The box is too heavy to carry.',
'动词不定式（to do）作主语/宾语/宾补/定语/状语。感官/使役动词后省略 to。it 作形式主语的句型是必考点。too...to 结构需掌握。',
@l4, 3, '["困难","非谓语","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='动名词 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '非谓语动词', '动名词',
'### 概述\n\n动名词（V-ing）具有名词特性，可在句中作主语和宾语。记住哪些动词后必须接动名词是学好非谓语的关键。动名词在完形填空和改错中经常出现。\n\n### 核心用法\n\n- **作主语**：Swimming is good for health. 动名词作主语表抽象泛指\n- **作宾语（必接 V-ing 的动词）**：enjoy / finish / mind / avoid / practice / suggest / keep / consider + V-ing\n- **固定短语接 V-ing**：be used to / look forward to / pay attention to / give up / can''t help / feel like\n- **作介词宾语**：Thank you for helping me. / He is good at playing chess.\n\n### 要点总结\n\n1. remember/forget/regret + to do（未做） vs + doing（已做）——I remember locking the door.（记得锁了）vs I remember to lock the door.（记得要锁）\n2. stop to do（停下来去做）vs stop doing（停止做）——He stopped to smoke. / He stopped smoking.\n3. go + V-ing 表活动：go shopping / go swimming / go fishing',
'动名词（V-ing）作主语/宾语。enjoy/finish/mind/practice/keep + V-ing。与不定式的区别：remember/forget/stop 后不同形式含义不同。',
@l4, 2, '["中等","非谓语","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='分词作定语和状语 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '非谓语动词', '分词作定语和状语',
'### 概述\n\n分词包括现在分词（V-ing）和过去分词（V-ed/不规则）。现在分词表主动/进行，过去分词表被动/完成。分词可作定语和状语，是语法中的难点内容。\n\n### 核心用法\n\n- **现在分词作定语**：a sleeping baby（正在睡觉的婴儿） / boiling water（沸腾的水）\n- **过去分词作定语**：a broken window（被打碎的窗户） / boiled water（开水）\n- **现在分词作状语**：Walking along the street, I met an old friend.（walking 表主动伴随）\n- **过去分词作状语**：Given more time, I can do it better.（given 表被动条件）\n\n### 要点总结\n\n1. 区分现在/过去分词：The movie is interesting.（令人感兴趣的）/ I am interested in the movie.（感到感兴趣的）\n2. 分词作状语时其逻辑主语必须与句子主语一致\n3. 独立主格结构：Weather permitting, we will go hiking.（分词有自己的逻辑主语）',
'现在分词（V-ing）表主动/进行，过去分词（V-ed）表被动/完成。分词可作定语和状语，注意逻辑主语必须一致。',
@l4, 3, '["困难","非谓语","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 定语从句 (3个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='关系代词 that/which/who [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '定语从句', '关系代词 that/which/who',
'### 概述\n\n定语从句是修饰名词或代词的从句，关系代词引导定语从句并在从句中充当成分。that 指人/物，which 指物，who/whom 指人。这是对口升学语法必考内容。\n\n### 核心用法\n\n- **that**：指人/物——The book that I bought is interesting.（that 作宾语可省略）\n- **which**：指物——I like the music which is quiet.（which 作主语不可省略）\n- **who/whom**：指人——The man who is standing there is my teacher.（who 作主语）/ The girl whom you met is my sister.（whom 作宾语）\n- **whose**：表所有格——I know the boy whose mother is a doctor.\n\n### 关系代词选择规则\n\n| 先行词 | 作主语 | 作宾语 | 作定语 |\n|--------|--------|--------|--------|\n| 人 | who/that | who/whom/that | whose |\n| 物 | which/that | which/that | whose/of which |\n\n### 要点总结\n\n1. 先行词含 all/anything/nothing/little/much/最高级/序数词时，关系代词用 that 不用 which\n2. 关系代词作宾语可以省略（The book I bought is good.），作主语不能省略\n3. 介词后不能用 that——The man with whom you talked is my father.（介词后用 whom/which）',
'定语从句由关系代词引导。that 指人/物，which 指物，who 指人。先行词含 all/最高级/序数词时用 that。介词后不用 that。',
@l4, 2, '["中等","定语从句","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='关系副词 when/where/why [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '定语从句', '关系副词 when/where/why',
'### 概述\n\n关系副词 when/where/why 引导定语从句，分别在从句中作时间/地点/原因状语。关系副词 = 介词 + which，这为选择提供了替换思路。\n\n### 核心用法\n\n- **when**：先行词表时间（day/year/time）——I still remember the day when I first came here.\n- **where**：先行词表地点（place/school/room）——This is the school where I studied.\n- **why**：先行词为 reason——Tell me the reason why you are late.\n- **关系副词 = 介 + which**：when = in/on/at which；where = in/at which；why = for which\n\n### 要点总结\n\n1. 区分关系副词和关系代词：先行词在从句中作状语用关系副词，作主/宾语用关系代词\n2. the reason why 是固定句型，why 在从句中作原因状语\n3. 当先行词是 situation/point/case 时，有时用 where 引导（抽象地点）',
'关系副词 when/where/why 在定语从句中作状语。= 介词 + which。区分关系副词和关系代词是关键：作状语用副词，作主/宾语用代词。',
@l4, 2, '["中等","定语从句","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='非限制性定语从句 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '定语从句', '非限制性定语从句',
'### 概述\n\n非限制性定语从句与先行词之间用逗号隔开，对先行词进行补充说明而非限定。删去后主句意思仍然完整。只能用 who/which/as 引导，不能用 that。\n\n### 核心用法\n\n- **与限制性定语从句的区别**：\n  - 限制性：I have a brother who is a doctor.（暗示可能有多个兄弟）\n  - 非限制性：I have a brother, who is a doctor.（仅一个兄弟，补充说明身份）\n- **用 which 指代整个主句**：He passed the exam, which made his parents happy.（which 指代前面整件事）\n- **as 引导的非限制性定语从句**：As we all know, the earth goes around the sun.（as 表"正如"）\n\n### 要点总结\n\n1. 非限制性定语从句不能用 that，不能用关系副词 why\n2. 逗号是关键区分标志——有逗号就是非限制性\n3. which 指代整个主句是非限制性定语从句的重要功能，常出现在单选题中',
'非限制性定语从句用逗号隔开，补充说明。不能用 that，只能用 who/which/as。which 可指代整个主句。as 表"正如"。注意逗号是关键标志。',
@l4, 3, '["困难","定语从句","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 名词性从句 (3个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='宾语从句 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '名词性从句', '宾语从句',
'### 概述\n\n宾语从句在复合句中充当动词或介词的宾语。引导词包括 that / if/whether / wh-词。语序必须用陈述句语序，这是对口升学必考点。\n\n### 核心用法\n\n- **that 引导**：I think (that) he is right.（that 可省略）\n- **if/whether 引导**：I wonder if/whether he will come.（一般疑问句的宾语从句）\n- **wh-词引导**：Can you tell me where the station is?（特殊疑问句的宾语从句，语序不倒装）\n- **时态一致**：主句过去时→从句过去相应时；主句现在时→从句任意时\n\n### 要点总结\n\n1. 宾语从句必须用陈述句语序——"where the station is" 而不是 "where is the station"\n2. if 和 whether 可互换，但介词后用 whether / 与 or not 连用时用 whether\n3. 否定转移：I don''t think he is right.（否定在主句，语义在从句）才是正确表达',
'宾语从句作动词/介词的宾语。语序必须用陈述句语序。引导词 that 可省略，if/whether 表"是否"。时态需与主句一致。否定转移要掌握。',
@l4, 2, '["中等","名词性从句","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='主语从句 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '名词性从句', '主语从句',
'### 概述\n\n主语从句在句中充当主语。常用 it 作形式主语，将真正的主语从句置于句末。这是中高难度的语法点，常见于阅读理解的长难句分析和语法选择题中。\n\n### 核心用法\n\n- **that 引导的主语从句**：That he passed the exam is true. / It is true that he passed the exam.（it 作形式主语更常用）\n- **wh-词引导的主语从句**：What he said is very important.\n- **It + be + adj./n. + that 从句**：It is necessary that we should study hard.\n- **It + be + 过去分词 + that**：It is said that / It is reported that / It is believed that\n\n### 要点总结\n\n1. that 引导主语从句时不可省略（与宾语从句不同）\n2. It + be + 过去分词 + that 结构是高频考点（It is said that... / It is reported that...）\n3. 主语从句的谓语动词通常用单数——What they need is more time.',
'主语从句作主语。常用 it 作形式主语。that 引导主语从句不可省略。It is said/reported that... 是高频结构，谓语动词用单数。',
@l4, 3, '["困难","名词性从句","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='表语从句与同位语从句 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '名词性从句', '表语从句与同位语从句',
'### 概述\n\n表语从句放在系动词后说明主语的内容，同位语从句用 that 说明前面名词的具体内容。两者形式相近但功能不同，需仔细区分。\n\n### 核心用法\n\n- **表语从句**：系动词后——The problem is that we don''t have enough time.（用 that 引导不可省略）\n- **表语从句其他引导词**：It looks as if it is going to rain. / That''s why I was late.\n- **同位语从句**：说明抽象名词的内容——I heard the news that our team won.（the news 的具体内容就是"we won"）\n- **常见抽象名词**：news, fact, idea, suggestion, truth, hope, promise, message\n\n### 区分要点\n\n| | 表语从句 | 同位语从句 |\n|--|---------|----------|\n| 位置 | 系动词后 | 抽象名词后 |\n| 功能 | 说明主语 | 说明名词具体内容 |\n| that 能否省略 | 不可省略 | 不可省略 |\n\n### 要点总结\n\n1. 表语从句：系动词 + 连接词 + 从句（The reason is that...）\n2. 同位语从句：抽象名词 + that + 完整从句（the fact that...）注意 that 没有词义不作成分\n3. 区分 that 同位语从句与 that 定语从句：同位语从句 that 不作成分，定语从句 that 作成分',
'表语从句在系动词后说明主语，同位语从句用 that 说明抽象名词的具体内容。that 在两者中均不可省略。区分标志：先行词 + that + 完整句 = 同位语从句。',
@l4, 3, '["困难","名词性从句","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 状语从句 (2个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='时间/条件/原因状语从句 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '状语从句', '时间/条件/原因状语从句',
'### 概述\n\n状语从句在主句中作状语，表示时间、条件、原因等。引导词的选择和"主将从现"规则是考试重点。状语从句在单项选择和完形填空中每年必考。\n\n### 核心用法\n\n- **时间状语从句**：when/while/before/after/as soon as/till/until——When I arrived, he was waiting.\n- **条件状语从句**：if/unless/as long as——If it rains, we will stay at home.（主将从现）\n- **原因状语从句**：because/since/as——He didn''t come because he was ill.\n- **until/till 的用法**：肯定句用延续动词（wait until...），否定句用终止动词（not...until 直到…才）\n\n### 要点总结\n\n1. "主将从现"：主句用将来时/祈使句，状语从句用一般现在时——I will call you when he comes.\n2. because 与 so 不能同时使用，since 表"既然"，as 表"由于"语气更弱\n3. unless = if...not（除非）——You will fail unless you work hard.',
'时间/条件/原因状语从句分别用 when/if/because 等引导。"主将从现"是必考规则。because 与 so 不能连用。unless = if...not。',
@l4, 2, '["中等","状语从句","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='让步/目的状语从句 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '状语从句', '让步/目的状语从句',
'### 概述\n\n让步状语从句表"虽然/尽管"，目的状语从句表"为了/以便"。这两种从句在阅读理解和写作中频繁出现，是中等难度的语法点。\n\n### 核心用法\n\n- **让步状语从句**：although/though/even though——Although it rained, they went out.（不能与 but 连用）\n- **as 引导倒装让步**：Young as he is, he knows a lot.（表语提前）\n- **目的状语从句**：so that / in order that——He got up early so that he could catch the bus.\n- **结果状语从句**：so...that / such...that——He was so tired that he fell asleep.\n\n### 要点总结\n\n1. although/though 与 but 不能连用——"Although... , ..." 或 "... , but..." 二选一\n2. so that 引导目的状语从句，从句中常有 can/could/may/might\n3. so...that（如此…以至于）中 so 修饰形容词/副词，such 修饰名词——so beautiful that / such a beautiful flower that',
'让步状语从句用 although/though/even though 引导，不能与 but 连用。目的状语从句用 so that/in order that。so...that 表结果。',
@l4, 2, '["中等","状语从句","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 主谓一致 (1个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='就近原则与意义一致 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '主谓一致', '就近原则与意义一致',
'### 概述\n\n主谓一致指谓语动词在人称和数上与主语保持一致。三条原则是：语法一致（形式统一）、意义一致（含义统一）、就近原则（与最近的主语一致）。短文改错中主谓不一致是高频错误。\n\n### 核心用法\n\n- **语法一致**：主语单数→谓语单数；主语复数→谓语复数——The boy runs fast. / The boys run fast.\n- **就近原则**：either...or / neither...nor / not only...but also / there be——Either you or he is wrong.（谓语与最近的主语 he 一致）\n- **意义一致**：集体名词 family/class/team——作整体用时单数（My family is big），作成员用时复数（My family are all tall）\n- **不定代词**：everyone/someone/anyone/no one + 单数谓语——Everyone is here.\n\n### 要点总结\n\n1. either...or / neither...nor / not only...but also 根据最近的主语确定谓语\n2. 当主语后跟 with/together with/as well as 时，谓语与前面的主语一致——The teacher, together with the students, is going there.\n3. 时间/距离/金额做整体时用单数——Two hours is enough.',
'主谓一致的三条原则：语法一致、意义一致、就近原则。either...or / neither...nor 就近原则。with/together with 不影响主语数。',
@l4, 1, '["基础","主谓一致","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 情态动词 (2个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='情态动词基本用法 can/must/may [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '情态动词', '情态动词基本用法 can/must/may',
'### 概述\n\n情态动词本身有含义但不能单独作谓语，后接动词原形。can/could（能力/请求）、must（必须/禁止）、may/might（允许/可能）是最基础的情态动词，出现在单项选择和情景交际题中。\n\n### 核心用法\n\n- **can/could**：表能力——I can swim. / 表请求——Can I help you? / Could you open the door?（could 更委婉）\n- **must**：表必须——You must finish your homework. / 表禁止（mustn''t）——You mustn''t smoke here.\n- **may/might**：表允许——May I come in? / 表可能——It may rain tonight.\n- **need**：情态动词 need 用于否定和疑问——Need I go? / You needn''t worry.\n\n### 要点总结\n\n1. must 的否定式 mustn''t 表"禁止"，"不必"用 don''t have to 或 needn''t\n2. can''t 可表"不可能"用于否定推测，也可以表"不能"\n3. May I...? 的回答：肯定用 Yes, you may. / Sure.；否定用 No, you mustn''t. / Sorry, you can''t.',
'情态动词 can/must/may 后接动词原形。must 否定 mustn''t=禁止，"不必"用 don''t have to。can 表能力/请求/可能性。may 表允许/可能。',
@l4, 1, '["基础","情态动词","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='情态动词推测用法 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '情态动词', '情态动词推测用法',
'### 概述\n\n情态动词的推测用法是四川对口升学的难点。must have done（肯定已做）、can''t have done（不可能已做）、may have done（可能已做）——这三种推测结构表示对过去情况的判断。\n\n### 核心用法\n\n- **must have done**：对过去肯定的推测——The ground is wet. It must have rained last night.（非常肯定）\n- **can''t/couldn''t have done**：对过去否定的推测——He can''t have gone to Beijing. I saw him just now.（不可能）\n- **may/might have done**：对过去可能的推测（不确定）——She may have forgotten the meeting.\n- **should have done**：本应该做（实际没做）——You should have told me earlier.\n\n### 要点总结\n\n1. 推测语气强度：must > may > might（自信程度递减）\n2. must have done 的反义疑问句用 didn''t / hasn''t（取决于时间状语）\n3. should have done 常含"责备/遗憾"语气，写作中可以用来表达观点',
'情态动词 + have done 表对过去的推测。must have done（肯定已做），can''t have done（不可能已做），may have done（可能已做）。',
@l4, 2, '["中等","情态动词","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 虚拟语气 (1个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='虚拟语气在条件句中的用法 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '虚拟语气', '虚拟语气在条件句中的用法',
'### 概述\n\n虚拟语气表示与事实相反的假设或愿望。if 条件句的虚拟涉及三种时间（现在/过去/将来）的"倒退时态"规则，是语法中难度最高的内容之一。\n\n### 核心用法\n\n- **与现在事实相反**：If I were you, I would study harder.（从句用 did/were，主句用 would/could + do）\n- **与过去事实相反（更常考）**：If I had studied harder, I would have passed the exam.（从句用 had done，主句用 would have done）\n- **与将来相反**：If it should rain tomorrow, we would stay at home.\n- **错综时间虚拟**：If I had taken your advice, I would be happy now.（从句过去，主句现在）\n\n| 时间 | if 从句 | 主句 |\n|------|---------|------|\n| 现在 | were/did | would/could/might + do |\n| 过去 | had done | would + have done |\n| 将来 | should/were to + do | would/could + do |\n\n### 要点总结\n\n1. 虚拟语气中 be 动词统一用 were（无论单复数）——If I were you...\n2. if 可省略倒装将 were/had/should 提前——Had I known, I would have come.\n3. wish 后的宾语从句也用虚拟语气：I wish I were a bird.（与现在相反）/ I wish I had gone.（与过去相反）',
'虚拟条件句：与现在相反用过去时，与过去相反用过去完成时。if 可省略倒装。I wish 后的从句也用虚拟。be 动词统一用 were。',
@l4, 3, '["困难","虚拟语气","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第二篇：语法专项 > 情景交际 (4个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='邀请与请求 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '情景交际', '邀请与请求',
'### 概述\n\n邀请与请求是情景交际的常考话题。掌握邀请（Would you like to... / Shall we...）和请求（Could you please... / Can I...）的得体表达，是补全对话题和单项选择中的基础内容。\n\n### 核心表达\n\n- **发出邀请**：Would you like to + do? / Do you want to + do? / How about + V-ing? / Shall we + do?\n- **接受邀请**：Yes, I''d love to. / Sounds great! / That would be nice. / Sure, why not?\n- **拒绝邀请**：I''d love to, but I''m afraid... / Sorry, I have to... / Maybe another time.\n- **提出请求**：Could/Can/Will you please...? / Would you mind + V-ing?\n- **回应请求**：Sure. / Of course. / No problem.（同意）；Sorry, I''m afraid I can''t.（拒绝）\n\n### 要点总结\n\n1. Would you like to...? 的回答是 I''d love to. 不可省略 to\n2. Would you mind + V-ing? 的回答：不介意用 Not at all. / Of course not. 不用 Yes\n3. Could you please...? 比 Can you...? 更礼貌，考试中优先用 could',
'邀请用 Would you like to / Shall we；请求用 Could you please。接受邀请说 I''d love to，拒绝要礼貌回应。Would you mind 回答注意 Not at all 表不介意。',
@l4, 1, '["基础","情景交际","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='感谢与道歉 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '情景交际', '感谢与道歉',
'### 概述\n\n感谢与道歉是日常交际中最基本的礼貌用语。掌握 Thank you 的多种回应方式和 I''m sorry 的得体表达，是情景交际和补全对话的基础内容。\n\n### 核心表达\n\n- **感谢**：Thank you (very much). / Thanks a lot. / I really appreciate it. / Thank you for + V-ing\n- **回应感谢**：You''re welcome. / My pleasure. / Not at all. / That''s all right. / Don''t mention it.\n- **道歉**：I''m sorry. / I''m sorry for + V-ing. / I apologize. / Excuse me for...\n- **回应道歉**：That''s OK. / Never mind. / It doesn''t matter. / Don''t worry about it. / No problem.\n\n### 要点总结\n\n1. Thank you for + V-ing 是固定搭配——Thank you for helping me.\n2. My pleasure 是回应感谢的礼貌用语，I''m sorry to hear that 用于听到坏消息时表达同情\n3. Sorry 和 Excuse me 的区别：sorry 为已发生的错道歉，excuse me 为即将打扰别人提前说',
'感谢用语 Thank you for + V-ing，回应用 You''re welcome / My pleasure。道歉用 I''m sorry，回应用 That''s OK / Never mind。',
@l4, 1, '["基础","情景交际","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='问路与指路 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '情景交际', '问路与指路',
'### 概述\n\n问路与指路是情景交际的实用性话题。掌握问路的礼貌用语和指路的常用指令是补全对话题的常考内容。\n\n### 核心表达\n\n- **问路**：Excuse me, can/could you tell me the way to...? / How can I get to...? / Where is...? / Is there a... near here?\n- **指路**：Go straight ahead. / Turn left/right at the corner. / It''s on your left/right. / You can take Bus No. 5.\n- **距离表达**：It''s about 10 minutes'' walk. / It''s not far from here. / It''s next to / across from / behind the bank.\n- **回应不确定**：Sorry, I''m a stranger here. / You''d better ask the policeman.\n\n### 要点总结\n\n1. 问路必先以 Excuse me 开头表示礼貌\n2. 指路时注意方向介词：on the left/right, at the corner, across from（在对面）\n3. walk 可作名词：10 minutes'' walk（步行10分钟的路程）',
'问路用 Excuse me + How can I get to... / Where is... 开头。指路用 Go straight / Turn left / It''s on your right。介词 on/at/across from 要分清。',
@l4, 1, '["基础","情景交际","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='建议与劝告 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '语法专项', '情景交际', '建议与劝告',
'### 概述\n\n建议与劝告在情景交际和写作中频繁使用。掌握多种建议表达方式及其回应，能够帮助学生在不同语境中得体地给出建议。\n\n### 核心表达\n\n- **提建议**：You''d better (not) do. / Why not / Why don''t you + do? / How about / What about + V-ing? / Shall we + do? / Let''s + do.\n- **委婉建议**：I suggest/advise you to + do. / It''s a good idea to + do. / If I were you, I would + do.\n- **接受建议**：Good idea! / That sounds great. / OK, I''ll try. / You''re right.\n- **拒绝建议**：That''s a good idea, but... / I''m sorry, but I think... / I''d rather not.\n\n### 要点总结\n\n1. You''d better = You had better, 否定式 You''d better not do（not 在 better 后）\n2. Why not + 动词原形（= Why don''t you + 动词原形）— Why not have a rest?\n3. 写作中建议类句型常用：I suggest that you should... / It is advisable to...',
'建议用 You''d better / Why not / How about。You''d better 否定为 You''d better not。建议类句型在写作中很好用。',
@l4, 1, '["基础","情景交际","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第三篇：阅读理解 > 细节理解题 (2个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='直接定位文中信息 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '细节理解题', '直接定位文中信息',
'### 概述\n\n直接定位文中信息是阅读理解最基础的题型。根据题干中的关键词（人名/地名/数字/专有名词等）在原文中快速找到对应的原句，答案即为文中原话或同义改写。\n\n### 核心方法\n\n- **三步定位法**：读题干划定位词→回原文找对应处→对比选项选同义表述\n- **常见定位词**：数字（时间/价格/数量）、大写（人名/地名/书名）、专有名词\n- **同义替换**：big→large, start→begin, buy→purchase, a lot of→a great deal of\n- **干扰项特征**：偷换概念（文中提到A和B，选项将A的特征给了B）、无中生有、绝对化（all/never/every常错）\n\n### 要点总结\n\n1. 细节题答案必在文中，务必回原文定位，不凭记忆作答\n2. 注意题干中的 NOT / EXCEPT / WRONG 等否定词，易被忽略\n3. 数字题注意核对：多个数字出现时需匹配各自的条件和归属',
'直接定位文中信息是阅读最基础题型。三步定位法：读题干划关键词→原文定位→对比选项。注意同义替换和 NOT/EXCEPT 等否定词。',
@l4, 1, '["基础","阅读理解","细节理解"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='Wh-问题细节检索 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '细节理解题', 'Wh-问题细节检索',
'### 概述\n\nWh-问题细节检索是针对含 who/what/when/where/why/how 疑问词的问题，在原文中精准定位并提取对应信息的阅读技能。每种疑问词都有特定的定位策略。\n\n### 核心策略\n\n- **who**→定位人名/身份词/职业——Who is the speaker? / Who wrote the book?\n- **what**→定位事件/内容——What did he do? / What is the passage about?\n- **when**→定位时间/日期/年代——When was the building built?\n- **where**→定位地点/场所——Where does the conversation take place?\n- **why**→定位 because/as/for/in order to 等表原因/目的的词\n- **how**→定位方式/手段/程度——How did they solve the problem? / How long does it take?\n\n### 要点总结\n\n1. 细节题通常按文章顺序出题，找到上一题答案后向后找下一题\n2. 数字题注意区分：文中多处数字需核对其各自对应的条件，避免张冠李戴\n3. 题干中的关键词可能在文中以同义词/近义词形式出现，不一定是原词',
'Wh-问题检索针对 who/what/when/where/why/how 各有定位策略。细节题按文章顺序出题，注意同义替换和数字混淆。',
@l4, 1, '["基础","阅读理解","细节理解"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第三篇：阅读理解 > 推理判断题 (1个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='根据上下文推断隐含意思 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '推理判断题', '根据上下文推断隐含意思',
'### 概述\n\n推理判断题要求从作者暗示、语气态度或文中线索出发，得出文中未直接说出的结论。题干常见词为 infer（推断）/suggest（暗示）/imply（暗指）/probably（可能）。\n\n### 核心方法\n\n- **标志词识别**：infer / suggest / imply / indicate / conclude / probably / most likely\n- **推断依据**：找 but/however/though 后作者真实观点；从举例和原因中推知结论\n- **合理推断原则**：答案必须基于原文信息推导，不可加入个人经验或常识\n- **常见信号**：表情感态度的形容词（disappointed/surprised/happy）、重复出现的关键词\n\n### 要点总结\n\n1. 推理题答案不会与原文完全一致——"推一步"找隐含意思，不是抄原句\n2. 禁止过度推断：原文说"下雨"不能推断出"洪水"；所有推断必须有理有据\n3. 注意题干是问可以被 inferred（可推断的）还是 can not be inferred（不可推断的）',
'推理判断题从文中线索推断隐含结论。标志词 infer/suggest/imply。答案必须基于原文"推一步"，不能过度推断或凭常识猜测。',
@l4, 2, '["中等","阅读理解","推理判断"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第三篇：阅读理解 > 主旨大意题 (1个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='概括段落/全文大意 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '主旨大意题', '概括段落/全文大意',
'### 概述\n\n主旨大意题要求从文章整体出发，提炼中心思想或选出最恰当的标题。需关注首段末段和各段落首句，避免以个别细节代替全文主旨。\n\n### 核心方法\n\n- **找主旨句位置**：首句（总起）/末句（总结）/转折句（but/however 后）\n- **标题选择题"反向法"**：假设自己写的这个标题，文章应该包含哪些内容？覆盖这些的就是正确标题\n- **文章结构法**：总-分-总结构中首尾呼应处即为主旨\n- **常见题干**：What is the main idea of the passage? / What is the best title? / The passage mainly talks about...\n\n### 要点总结\n\n1. 正确主旨应覆盖全文核心内容，而非某一段落细节\n2. 选项范围过大（如文章讲"中国熊猫"，选项为"世界动物保护"）或过小（仅涉及某一段）都是常见干扰项\n3. 作者写作目的题（The purpose of the passage is to...）分析文体：说明文→inform，议论文→persuade，记叙文→share',
'主旨大意题概括段落/全文大意，或选最佳标题。找首/末句和转折句。正确选项覆盖全文核心，不选细节和范围过大的选项。',
@l4, 2, '["中等","阅读理解","主旨大意"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第三篇：阅读理解 > 词义猜测题 (1个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='根据上下文猜测生词含义 [基础]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '词义猜测题', '根据上下文猜测生词含义',
'### 概述\n\n词义猜测题考查根据上下文推断生词含义的能力，而非依赖词典。利用定义解释、近反义词、举例或因果逻辑关系是主要的猜词方法。\n\n### 核心方法\n\n- **定义线索法**：is / means / that is / in other words / called 后直接给出解释\n- **近反义词法**：and/or 连接近义词，but/however 引出反义词——She is diligent but her brother is lazy.（diligent 与 lazy 反义，意为勤奋）\n- **举例法**：such as / for example / like 后的例子帮助推断\n- **因果法**：because / so / therefore 提供因果线索\n- **词缀法**：un-/im-/dis-（否定前缀），-ful/-less（形容词后缀），-tion/-ment（名词后缀）\n\n### 要点总结\n\n1. 脱离上下文猜词是大忌——同一个词在不同语境意思不同\n2. 题干要求猜测的可能是词组而非单个词，注意整体含义\n3. 先看生词前后的定义句和同位语，这两处最常给出直接解释',
'词义猜测题根据上下文推断生词含义。定义线索（is/means）、反义线索（but）、举例线索（such as）是主要猜词方法。',
@l4, 1, '["基础","阅读理解","词义猜测"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第四篇：补全对话 > 补全对话 (4个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='问候告别与感谢道歉' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '补全对话', '补全对话', '问候告别与感谢道歉',
'### 概述\n\n问候告别与感谢道歉是补全对话中最基础的交际功能。掌握不同场景的得体表达，能够根据上下文判断对话的语义走向并选择正确的应答语。\n\n### 问候与告别\n\n- **初次见面**：How do you do? → How do you do? / Nice to meet you. → Nice to meet you too.\n- **熟人问候**：How are you? → I''m fine, thank you. / How is it going? → Pretty good.\n- **问候他人**：Say hello to your family. / Please give my best wishes to...\n- **告别**：Goodbye / See you later / Take care / See you tomorrow.\n\n### 感谢与道歉\n\n- **感谢**：Thank you for + V-ing. → You''re welcome. / My pleasure. / Not at all.\n- **道歉**：I''m sorry for + V-ing. / I apologize. → That''s OK. / Never mind. / It doesn''t matter.\n- **听到坏消息**：I''m sorry to hear that. 表示同情\n\n### 要点总结\n\n1. How do you do? 的回答必须是 How do you do? 不能变\n2. "Thank you for + V-ing" 和 "I''m sorry for + V-ing" 是固定句型\n3. 补全对话题注意上下文的语气和逻辑，先判断场景再选表达',
'问候告别：How do you do? / How are you? / See you later。感谢道歉：Thank you for / I''m sorry for → 对应回答。注意语境判断场景匹配。',
@l4, 1, '["基础","补全对话","交际用语"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='邀请预约与祝愿祝贺' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '补全对话', '补全对话', '邀请预约与祝愿祝贺',
'### 概述\n\n邀请预约与祝愿祝贺是补全对话的高频交际场景。邀请要懂得礼貌提出和得体回应，祝愿祝贺要会用恰当的节日和场合用语。\n\n### 邀请与预约\n\n- **发出邀请**：Would you like to + do? / Are you free this weekend? / Shall we + do?\n- **接受邀请**：Yes, I''d love to. / Sounds great. / That''s a good idea.\n- **拒绝邀请**：I''d love to, but I have to... / Sorry, I''m afraid I can''t.\n- **预约**：I''d like to make an appointment with... / Is it convenient for you?\n\n### 祝愿与祝贺\n\n- **节日祝福**：Merry Christmas! / Happy New Year! → The same to you.\n- **生日祝福**：Happy birthday! → Thank you.\n- **祝贺**：Congratulations! → Thank you. / Well done! / Good luck!\n\n### 要点总结\n\n1. "The same to you" 用于回应对方对节日的祝福（Happy New Year → The same to you）\n2. 生日/个人成就的祝福不能用 The same to you，应说 Thank you\n3. 办活动常用安排：It will be held on... at...（活动将在某时某地举行）',
'邀请：Would you like to / Are you free。祝贺：Congratulations! / Happy birthday! / Merry Christmas! 节日祝福用 The same to you 回应。',
@l4, 1, '["基础","补全对话","交际用语"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='请求帮助与提供帮助' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '补全对话', '补全对话', '请求帮助与提供帮助',
'### 概述\n\n请求帮助与提供帮助是日常交际的核心功能。礼貌提出请求和主动提供帮助都是情景交际的常考内容。\n\n### 请求帮助\n\n- **礼貌请求**：Could/Can you please + do? / Would you mind + V-ing? / I wonder if you could...\n- **回答同意**：Sure. / Of course. / No problem. / With pleasure.\n- **回答拒绝**：Sorry, I''m afraid I can''t. / I''d like to, but...\n\n### 提供帮助\n\n- **主动提供**：Can I help you? / What can I do for you? / Do you need any help? / Let me help you.\n- **接受帮助**：Yes, please. / Thank you. That''s very kind of you.\n- **谢绝帮助**：No, thanks. / I can manage it myself. / That''s very kind of you, but...\n\n### 要点总结\n\n1. Could you please + 动词原形（非 V-ing），例句：Could you please open the door?\n2. With pleasure 表"愿意/乐意帮忙"用于回应请求帮助，My pleasure 表"不客气"用于回应感谢\n3. 服务场景：Can I help you? = What can I do for you? 是店员/服务人员的标准用语',
'请求帮助：Could you please / Would you mind。提供帮助：Can I help you / Let me help you。With pleasure 表"乐意帮忙"，My pleasure 表"不客气"。',
@l4, 1, '["基础","补全对话","交际用语"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='建议劝告与禁止警告' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '补全对话', '补全对话', '建议劝告与禁止警告',
'### 概述\n\n建议劝告与禁止警告涉及不同强度的语气表达。建议用委婉的 You''d better/Why not，劝告用 You should，禁止警告用 You mustn''t / No + V-ing / Look out! 等。\n\n### 建议与劝告\n\n- **建议**：You''d better (not) do. / Why not + do? / How about + V-ing? / I suggest you (should) + do.\n- **回答建议**：Good idea. / That''s a good idea. / OK, I''ll try. / I don''t think so.\n- **劝告**：You should / ought to + do. / You''d better + do. / Don''t + do.\n\n### 禁止与警告\n\n- **禁止**：You mustn''t... / You are not allowed to... / No photos! / No smoking! / Don''t + do.\n- **警告**：Look out! / Watch out! / Be careful! / Take care! / Don''t touch!\n- **警告回应**：Thank you for warning me. / I''ll be careful.\n\n### 要点总结\n\n1. You''d better 的否定是 You''d better not（不是 You don''t better）\n2. No + V-ing 用于标语和指令（No parking / No smoking / No shouting）\n3. Look out! 和 Watch out! 是紧急警告，语气比 Be careful 更强',
'建议用 You''d better / Why not / How about。劝告 You should / You must。禁止用 You mustn''t / No + V-ing。警告用 Look out / Be careful。',
@l4, 1, '["基础","补全对话","交际用语"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第五篇：短文改错 > 短文改错 (4个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='动词时态语态改错' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '短文改错', '短文改错', '动词时态语态改错',
'### 概述\n\n动词时态语态错误是短文改错最高频的错误类型之一。通读全文把握时态基调，逐句检查动词形式是否一致是解题关键。\n\n### 常见错误类型\n\n- **时态不一致**：全文为过去时，个别句子突然变为现在时——需统一时态基调\n- **三单缺失**：He go to school every day. → goes（一般现在时三单加 -s）\n- **规则/不规则动词形式错误**：He swimmed across the river. → swam（不规则动词）\n- **时态使用错误**：I go to Beijing yesterday. → went（过去时间用过去时）\n- **被动语态缺失/错误**：The book wrote by him. → was written（被动语态 be+done）\n\n### 要点总结\n\n1. 改错第一步：通读全文确定时态基调（是过去/现在/将来），全文保持统一\n2. 注意时间标志词：yesterday→过去时，every day→一般现在时，tomorrow→将来时\n3. 主谓不一致是高频错误：特别是主语三单而谓语没加 -s 或 be 动词用错',
'时态语态改错：统一全文时态基调，检查三单加 -s，不规则动词过去式，被动语态 be+done。通读全文确定时态后逐句检查。',
@l4, 2, '["中等","短文改错","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='名词冠词介词改错' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '短文改错', '短文改错', '名词冠词介词改错',
'### 概述\n\n名词冠词介词改错涉及名词单复数、冠词多余/缺失/误用、介词搭配错误等。这些错误比较"细碎"，但也正是容易丢分的地方。\n\n### 常见错误类型\n\n- **名词单复数**：可数名词前缺冠词或未加 -s——I have book. → I have a book. / There are many student. → students\n- **不可数名词加 -s**：advice / information / news / furniture + 不可数，不能加 -s\n- **冠词缺失**：I have apple. → I have an apple.（元音前用 an）\n- **冠词多余**：He goes to the school every day.（表上学不用冠词）→ to school\n- **介词搭配错误**：in the morning / on Sunday / at night / look at / wait for / be good at\n\n### 要点总结\n\n1. 可数名词不能裸奔——要么加冠词（a/an/the），要么变复数\n2. 固定介词搭配靠积累：at home, on foot, in time, by bus 等\n3. 冠词：元音音素开头的词前用 an（an hour / an honest boy / an apple）',
'名词单复数/冠词误用/介词搭配。可数名词不能裸奔要加冠词或变复数。固定搭配如 at night / on Sunday / in the morning 要熟记。',
@l4, 2, '["中等","短文改错","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='主谓一致与非谓语改错' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '短文改错', '短文改错', '主谓一致与非谓语改错',
'### 概述\n\n主谓一致错误和非谓语动词形式错误是改错题的中频考点。主谓一致检查主语中心词的数与非谓语动词的动词形式是否匹配。\n\n### 主谓一致错误\n\n- **主语三单→谓语三单**：He like music. → likes（一般现在时三单加 -s）\n- **就近原则检查**：either...or / neither...nor 连接的主语，谓语与最近的主语一致\n- **with/together with 不增加主语量**：The teacher, together with the students, are going there. → is\n\n### 非谓语错误\n\n- **enjoy/finish/mind 后接 V-ing**：I enjoy to swim. → swimming\n- **want/hope/decide 后接 to do**：I want going home. → to go\n- **感官动词后省略 to**：I saw him to cross the street. → cross\n- **被动关系用 done**：The cup broken by the boy... → The cup broken / The cup was broken\n\n### 要点总结\n\n1. with/together with/as well as 跟进的主语不影响谓语数\n2. 记住常用"V-ing 固定搭配"：enjoy/finish/mind/avoid/practice/keep + doing\n3. 非谓语作定语时注意主动被动：a sleeping baby（主动）vs a broken window（被动）',
'主谓一致：三单加 -s，with 不影响主语数。非谓语：enjoy/finish + V-ing，want/hope + to do。感官动词后省略 to。',
@l4, 2, '["中等","短文改错","语法"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='连词形容词副词改错' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '短文改错', '短文改错', '连词形容词副词改错',
'### 概述\n\n连词形容词副词改错涉及连接词误用、形容词副词混淆、比较级错误等。这些错误虽然不如时态题显眼，但往往是拉开分数的关键。\n\n### 常见错误类型\n\n- **连词误用**：because...so 不能连用，although...but 不能连用——保留一个\n- **and/but/or 混淆**：上下文是递进用 and，转折用 but，选择用 or\n- **形容词→副词**：He runs quick. → quickly（修饰动词用副词）\n- **系动词后接形容词**：She looks happily. → happy（系动词后接形容词）\n- **比较级错误**：more better → better（比较级不加 more）；as more than → as many as\n- **比较级/最高级混淆**：两者比较用比较级（He is taller than me.），三者及以上用最高级（He is the tallest.）\n\n### 要点总结\n\n1. 形容词修饰名词/系动词后，副词修饰行为动词——形修名/系，副修动\n2. because 和 so 不能同时出现在一个句子中，although 和 but 也是\n3. good/well 的比较级是 better，最高级是 best——不规则变化需牢记',
'连词：because/so 和 although/but 不能连用。形副：形容词修饰名词和系动词，副词修饰行为动词。比较级：不叠加 more，不规则变化记牢。',
@l4, 2, '["中等","短文改错","语法"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第六篇：写作 > 应用文写作 (2个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='书信格式与常用表达 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '写作', '应用文写作', '书信格式与常用表达',
'### 概述\n\n书信是对口升学书面表达的第一大题型，15分中占重要地位。掌握书信的标准格式（称呼/正文/结束语/署名/日期）和各类型书信的常用句型是拿分的关键。\n\n### 书信格式\n\n- **日期**：右上角——March 15, 2026 或 15 March 2026\n- **称呼**：Dear... + 逗号（Dear Tom, / Dear Sir or Madam,）\n- **正文**：开头句 → 主体（要点1/2/3） → 结尾句\n- **结束语**：Yours sincerely, / Yours faithfully,（开头为Dear+人名用sincerely，Dear Sir用faithfully）\n- **署名**：最后一行的姓名\n\n### 各类型书信常用句型\n\n- **感谢信**：Thank you for... / I really appreciate... / I''m writing to express my thanks to...\n- **邀请信**：I would like to invite you to... / Would you like to...? / I hope you can come.\n- **建议信**：You''d better... / Why not...? / It''s a good idea to... / I suggest that...\n- **请假条**：I''m sorry to tell you that I can''t... / I ask for leave for... days.\n\n### 要点总结\n\n1. 格式最重要——缺称呼/结束语/署名/日期都会被扣分\n2. 结束语与称呼应匹配：Dear Sir/Madam → Yours faithfully；Dear + 人名 → Yours sincerely\n3. 正文结构建议：三段式（开头目的+主体要点+结尾期待回复）',
'书信是对口升学书面表达第一题型。格式：日期→称呼→正文→结束语→署名。常用句型：感谢/邀请/建议/请假各有固定套语。',
@l4, 4, '["掌握","应用文","写作"]', 'PUBLISHED', NOW(), NOW());

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='通知类写作 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '写作', '应用文写作', '通知类写作',
'### 概述\n\n通知类写作包括书面通知（NOTICE）和口头通知两种形式。书面通知需要标题和落款，口头通知需要开场呼语。通知写作的核心是清晰传达 what/when/where/who 四个要素。\n\n### 通知格式\n\n- **书面通知**：NOTICE（标题居中大写）→ 正文 → 发布单位（右对齐）→ 日期\n- **口头通知开头**：May I have your attention, please? / Attention, please, everyone. / I have an announcement to make.\n\n### 核心句型\n\n- **活动安排**：There will be a talk / meeting / party... on + 日期 at + 时间 in + 地点\n- **参加要求**：Everyone is expected to attend. / All are welcome. / Please be on time.\n- **时间地点表达**：on the afternoon of May 5th / at the school gate / in the meeting room\n- **通知结尾**：Don''t miss the good chance! / That''s all. Thank you.（口头通知）\n\n### 要点总结\n\n1. 通知四要素必须包含：事项（what）、时间（when）、地点（where）、参加人（who）\n2. 书面通知和口头通知格式不同，注意区分不要混用\n3. 时间介词注意：on + 具体某天（on Monday / on May 1st），at + 时刻（at 8:00），地点介词用 in/at',
'通知类写作：书面通知（NOTICE+正文+落款）和口头通知（呼语+正文+结束语）。必须包含 what/when/where/who 四要素。',
@l4, 4, '["掌握","应用文","写作"]', 'PUBLISHED', NOW(), NOW());

-- ======================================================================
-- 第六篇：写作 > 话题写作 (1个L4)
-- ======================================================================

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='观点表达与逻辑衔接 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '写作', '话题写作', '观点表达与逻辑衔接',
'### 概述\n\n话题写作要求围绕给定话题写80-100词的短文。观点表达要清晰，逻辑衔接要自然。常见的衔接词（First / Besides / Finally / However / Therefore）使文章层次分明。\n\n### 常见话题分类\n\n- **校园生活**：after-school activities, my school day, favorite subject, school rules\n- **家庭朋友**：my family, best friend, parents'' love, helping each other\n- **兴趣爱好**：my hobby, reading, sports, music, travel\n- **环境保护**：protect the environment, save water, reduce waste, plant trees\n- **未来规划**：my dream, future job, study plan, New Year''s resolution\n\n### 常用表达\n\n- **观点开头**：In my opinion, ... / I think that... / As far as I''m concerned, ...\n- **顺序展开**：First(ly), ... Second(ly), ... Finally, ... / To begin with, ... Besides/Moreover, ...\n- **转折对比**：However, ... / On the other hand, ... / In contrast, ... / But...\n- **因果**：Therefore, ... / So, ... / As a result, ... / Because of + 名词\n- **总结**：In short, ... / All in all, ... / To sum up, ... / In a word, ...\n\n### 要点总结\n\n1. 80-100词建议分三段：开头句引出话题 → 主体2-3个要点 → 结尾句总结\n2. 每段至少用一个衔接词，使文章不"生硬跳跃"\n3. 简单句加少量复合句即可，不必追求复杂句式——语法正确 > 句式高级',
'话题写作要求80-100词短文。观点表达用 In my opinion / I think。逻辑衔接用 First / Besides / Finally / However / Therefore。三段式结构。',
@l4, 4, '["掌握","话题写作","写作"]', 'PUBLISHED', NOW(), NOW());

SELECT 'v153: English knowledge articles created (53 inserts)' AS result;
