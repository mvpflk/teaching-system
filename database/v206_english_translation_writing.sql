-- ============================================================================
-- v204: 英语[职高] 翻译+写作 知识文章补充（4篇L4节点文章）
-- 补充 v153 缺失的翻译板块文章，新增写作板块综合技巧文章
-- 幂等: INSERT IGNORE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- ============================================================================
-- Part 1: 翻译 > 英译汉 — 新增 L4 节点 + 文章
-- ============================================================================

-- 1a. 创建 L4 节点
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT id, 24, 4, '英译汉基本技巧', 3, 'ACTIVE'
FROM knowledge_nodes WHERE subject_id=24 AND name='英译汉' AND level=3 LIMIT 1;

-- 1b. 插入文章
SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='英译汉基本技巧' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '翻译', '英译汉', '英译汉基本技巧',
'### 概述\n\n英译汉是对口升学翻译题型的基础，要求将英语句子准确、通顺地译为中文。掌握"找主干→处理修饰→调语序"三步法，可以应对大部分翻译题。四川省对口高考翻译题通常包含5个句子，每句2-3分，共15分。\n\n### 核心翻译步骤\n\n**第一步：找主干**\n先找出句子的主谓宾（主系表）主干，这是翻译的基础骨架。\n- He (主语) gave (谓语) me (间接宾语) a book (直接宾语). → 他给了我一本书。\n- The man who is standing there (修饰) is my teacher (主干). → 站在那里的那个人是我的老师。\n\n**第二步：处理修饰**\n识别定语、状语、从句等修饰成分，理解它们修饰什么。\n- 定语从句：The book that I bought yesterday is very interesting.\n  → 我昨天买的书非常有趣。（定语"我昨天买的"前置）\n- 状语：He went to Beijing by train last week.\n  → 他上周坐火车去了北京。（时间状语前置，方式状语也前置）\n\n**第三步：调语序**\n将英语语序调整为符合中文习惯的表达。\n- 定语后置→前置：a book written by Lu Xun → 鲁迅写的一本书\n- 状语后置→前置：He works hard every day. → 他每天努力工作。\n- 被动语态→主动：The bridge was built in 2020. → 这座桥建于2020年。\n\n### 常见句型翻译表\n\n| 英语句型 | 结构 | 示例 | 翻译 |\n|---------|------|------|------|\n| It + be + adj. + to do | 形式主语句 | It is important to learn English. | 学英语很重要。 |\n| There be | 存在句 | There are 50 students in the class. | 班上有50名学生。 |\n| not...until | 直到…才 | He didn''t go to bed until 11. | 他直到11点才睡觉。 |\n| the + 比较级, the + 比较级 | 越…越… | The more you read, the more you learn. | 你读得越多，学得越多。 |\n| too...to | 太…而不能 | The box is too heavy to carry. | 这个箱子太重了搬不动。 |\n\n### ⚠️ 职高生常犯错误\n\n1. **逐词死译**——"I am reading a book." 译成"我正在读一本书"不是"我是读一本书"。be动词在此表进行时态，不译"是"。\n2. **忽略固定搭配**——"take care of" 译"照顾"不是"拿关心"；"look after"同样译"照顾"。\n3. **语序不调整**——"I met him in the park yesterday." 译成"我遇到了他在公园昨天"不通顺，应调整为"我昨天在公园遇到了他"。\n4. **长句不分主次**——复合句中把从句内容翻译得比主句还重要，应先定位主句主干再从句修饰。\n\n### ⚡ 练一练\n\n**题1：** Translate: "The book that I borrowed from the library is very useful."\n\n**题2：** Translate: "He was so tired that he fell asleep quickly."\n\n<details><summary>点击查看答案</summary>\n\n**题1答案：** 我从图书馆借的那本书非常有用。\n解析：定语从句"that I borrowed from the library"修饰"The book"，译时前置为"我从图书馆借的"。\n\n**题2答案：** 他太累了，很快就睡着了。\n解析："so...that"结构译"太…以至于"，普通语境下"以至于"可省略。"fell asleep"译"睡着了"。\n</details>',
'英译汉三步法：找主干→处理修饰→调语序。注意固定搭配识别和语序调整，避免逐词死译。常用句型如 not...until / too...to 有固定译法。',
@l4, 2, '["中等","翻译","英译汉","技巧"]', 'PUBLISHED', NOW(), NOW());

-- ============================================================================
-- Part 2: 翻译 > 汉译英 — 新增 L4 节点 + 文章
-- ============================================================================

-- 2a. 创建 L4 节点
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT id, 24, 4, '汉译英基本技巧', 3, 'ACTIVE'
FROM knowledge_nodes WHERE subject_id=24 AND name='汉译英' AND level=3 LIMIT 1;

-- 2b. 插入文章
SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='汉译英基本技巧' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '翻译', '汉译英', '汉译英基本技巧',
'### 概述\n\n汉译英要求将中文句子准确译为英文，是对词汇运用和语法构建的综合考查。核心步骤为：确定主语→选谓语→补修饰。对口升学汉译英题通常考查基础句型和考纲词汇，不追求复杂句式，语法正确是第一要求。\n\n### 核心步骤\n\n**第一步：确定主语**\n中文主语有时隐含或与英文不同，需要找准英文主语。\n- "下雨了。" → 英文需添加主语：It is raining.\n- "校园里有很多花。" → There are many flowers in the campus.\n- "学好英语很重要。" → It is important to learn English well.\n\n**第二步：选谓语**\n根据时态和语态选择正确的谓语形式，注意主谓一致。\n- "他每天跑步。" → 一般现在时：He runs every day.（三单加-s）\n- "我昨天去了北京。" → 一般过去时：I went to Beijing yesterday.\n- "他们正在踢足球。" → 现在进行时：They are playing football now.\n\n**第三步：补修饰**\n添加定语、状语等修饰成分，注意英文中修饰成分的位置。\n- "我昨天在学校门口遇见了他。" → I met him at the school gate yesterday.（时间地点放句末）\n- "穿红衣服的女孩是我的妹妹。" → The girl in red is my sister.（定语后置）\n\n### 常见句型对照\n\n| 中文句型 | 英文结构 | 示例 |\n|---------|---------|------|\n| 某处有某物 | There be + 名词 + 地点 | There is a book on the desk. |\n| 某人花费时间做某事 | It takes sb. + 时间 + to do | It takes me 30 minutes to walk to school. |\n| 越来越… | 比较级 + and + 比较级 | Our city is becoming more and more beautiful. |\n| 和…一样 | as + 原级 + as | He is as tall as his brother. |\n| 不仅…而且… | not only...but also... | She not only sings well but also dances well. |\n\n### ⚠️ 常犯错误（中式英语）\n\n1. **缺少主语**——"是晴天"不能直接译"is sunny"，必须加 It：It is sunny.\n2. **very 位置错**——"我非常喜欢"不是"I very like"而是"I like...very much"。副词 very 修饰形容词/副词，不直接修饰动词。\n3. **时态混淆**——"我去年去了北京"用一般过去时，"我去年去过北京"用现在完成时表经历。句中时间决定时态。\n4. **主谓不一致**——"He go to school"应为"He goes to school"，三单加-s是必考点。\n\n### ⚡ 练一练\n\n**题1：** Translate: "这本书是我昨天买的。"\n\n**题2：** Translate: "如果你努力学习，你就能通过考试。"\n\n<details><summary>点击查看答案</summary>\n\n**题1答案：** This book was bought by me yesterday. 或 I bought this book yesterday.\n解析：两种译法均可。被动语态"was bought"或主动语态"I bought"。注意时间"yesterday"对应一般过去时。\n\n**题2答案：** If you study hard, you will pass the exam.\n解析：条件状语从句"主将从现"——主句用将来时(will pass)，从句用一般现在时(study)。"努力学习"译"study hard"，"通过考试"译"pass the exam"。\n</details>',
'汉译英三步法：确定主语→选谓语→补修饰。注意主谓一致和时态正确，避免中式英语（very位置、缺主语）。主将从现规则需掌握。',
@l4, 2, '["中等","翻译","汉译英","技巧"]', 'PUBLISHED', NOW(), NOW());

-- ============================================================================
-- Part 3: 写作 > 应用文写作 — 新增 L4 节点 + 文章（综合技巧）
-- ============================================================================

-- 3a. 创建 L4 节点
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT id, 24, 4, '书信和通知写作', 3, 'ACTIVE'
FROM knowledge_nodes WHERE subject_id=24 AND name='应用文写作' AND level=3 LIMIT 1;

-- 3b. 插入文章
SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='书信和通知写作' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '写作', '应用文写作', '书信和通知写作',
'### 概述\n\n应用文写作是对口升学书面表达（15分）的必考题型，以书信和通知为主。书信要求格式规范、语气得体；通知要求信息清晰、要素齐全。掌握两者的标准格式和常用套语是拿高分的关键。\n\n### 一、书信格式\n\n书信格式为：**称呼→正文→祝愿语→署名→日期**\n\n```\nDear Tom,                            （称呼，左起顶格，逗号）\n\n  How are you doing? I''m writing     （正文，缩进或顶格）\n  to tell you about my school life.\n  ...\n  I hope to hear from you soon.\n\nBest wishes,                         （祝愿语，右起或左起）\nLi Hua                               （署名，右起）\nMarch 15, 2026                       （日期，右起）\n```\n\n**各部分要点：**\n- **称呼**：Dear + 人名（Dear Tom,）/ Dear Sir or Madam,（不知姓名时用）\n- **正文结构**：开头句（写信目的）→ 主体（2-3个要点）→ 结尾句（期待回复）\n- **祝愿语**：Yours sincerely,（与Dear+人名搭配）/ Yours faithfully,（与Dear Sir/Madam搭配）\n- **署名**：写在祝愿语下一行，右对齐\n- **日期**：写在署名下一行，March 15, 2026 或 15 March 2026\n\n### 二、通知格式\n\n书面通知格式：**标题→正文→落款→日期**\n\n```\nNOTICE                               （标题居中大写）\n\nAll students are required to         （正文包含：活动内容/时间/地点/对象）\ntake part in the English Speech\nContest on May 10th at 2:00 p.m.\nin the school hall.\n\nThe Student Union                    （落款，右对齐）\nMay 5, 2026                          （日期，右对齐）\n```\n\n**口头通知格式：** 开场呼语（May I have your attention, please?）→ 正文 → 结束语（That''s all. Thank you.）\n\n### 常用套语\n\n| 场景 | 书信套语 | 通知套语 |\n|------|---------|---------|\n| 开头 | I''m writing to invite you to... / Thank you for your letter. | Attention, please! / I have an announcement to make. |\n| 时间地点 | The party will be held on... at... in... | There will be a meeting on... at... in... |\n| 要求 | You are expected to... / Please remember to... | Everyone is required to... / Please be on time. |\n| 结尾 | I''m looking forward to your reply. | Don''t miss the chance! / That''s all. Thank you. |\n\n### ⚠️ 常见格式错误\n\n1. **书信缺项**——漏写日期或署名，直接扣2-3分。\n2. **祝愿语不匹配**——Dear Sir用Yours sincerely是错的，应用Yours faithfully。\n3. **通知标题格式错**——书面通知NOTICE必须居中大写，而非左起或小写。\n4. **口头/书面混用**——书面通知不需要开场呼语"May I have your attention"，口头通知不需要标题"NOTICE"。\n5. **时间介词错误**——on Monday（具体某天），at 8:00（时刻），in May（月份）。\n\n### ⚡ 练一练\n\n**题1：** 假设你是李华，写一封邀请信，邀请你的朋友Tom参加你下周六下午2点在学校的英语角活动。\n\n**题2：** 写一份书面通知，通知全校学生下周一早上8点在操场参加升旗仪式。\n\n<details><summary>点击查看答案</summary>\n\n**题1参考答案：**\nDear Tom,\n\n  How are you doing? I''m writing to invite you to take part in the English Corner activity at 2:00 p.m. next Saturday in our school. We can practice speaking English and make new friends together. I hope you can come.\n\nYours sincerely,\nLi Hua\n\n解析：格式完整（称呼/正文/祝愿语/署名），包含活动内容/时间/地点三个要素。\n\n**题2参考答案：**\nNOTICE\n\nAll students are required to attend the flag-raising ceremony at 8:00 a.m. next Monday on the school playground. Please be on time and wear school uniforms.\n\nThe Student Union\n\n解析：标题NOTICE居中大写，正文包含时间/地点/参加人三要素，落款齐全。\n</details>',
'书信格式：称呼→正文→祝愿语→署名→日期。通知格式：NOTICE(标题)→正文→落款→日期。掌握祝愿语搭配规则和常用套语。',
@l4, 3, '["掌握","应用文","写作","书信","通知"]', 'PUBLISHED', NOW(), NOW());

-- ============================================================================
-- Part 4: 写作 > 话题写作 — 新增 L4 节点 + 文章（综合技巧）
-- ============================================================================

-- 4a. 创建 L4 节点
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT id, 24, 4, '话题写作技巧', 2, 'ACTIVE'
FROM knowledge_nodes WHERE subject_id=24 AND name='话题写作' AND level=3 LIMIT 1;

-- 4b. 插入文章
SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='话题写作技巧' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '写作', '话题写作', '话题写作技巧',
'### 概述\n\n话题写作（书面表达）是对口升学英语考试的最后一道大题，占15分。要求围绕给定话题写80-100词的短文，考查词汇运用、语法正确性和逻辑连贯性。评分标准包括：内容要点（6分）、语法词汇（4分）、逻辑连贯（3分）、书写规范（2分）。掌握五步写作法可以有效提分。\n\n### 五步写作法\n\n**第一步：审题（2分钟）**\n读懂题目要求，确定文体（书信/通知/话题短文）、人称（第一/三人称）和时态（现在/过去/将来）。圈出题目中的关键要求，确保不漏要点。\n\n**第二步：列要点（3分钟）**\n在草稿纸上列出2-3个要点（中文即可），每点对应正文一段。\n- 话题"My Favorite Sport" → 要点：① what sport ② why I like it ③ how it benefits me\n- 话题"My Dream Job" → 要点：① what job ② reasons ③ what to do now\n\n**第三步：遣词造句（8分钟）**\n将要点扩展为完整的英文句子，每句5-10词。优先用自己掌握的词汇，语法正确>句式高级。\n- 要点①→ My favorite sport is swimming.\n- 要点②→ I like it because it makes me healthy and relaxed.\n- 要点③→ I go swimming twice a week with my friends.\n\n**第四步：连句成篇（5分钟）**\n用连接词将句子连接成段落，80-100词分三段：开头引出话题 → 主体展开要点 → 结尾总结。\n\n**第五步：检查（2分钟）**\n逐句检查：① 时态统一吗？② 主谓一致吗？③ 拼写对吗？④ 连接词用了吗？⑤ 字数够吗？\n\n### 常用连接词\n\n| 功能 | 连接词 | 用法 |\n|------|--------|------|\n| 顺序 | First, / Second, / Finally, | First, I get up at 6:30 every day. |\n| 递进 | Besides, / What''s more, / Moreover, | Besides, I enjoy reading books. |\n| 转折 | However, / On the other hand, | However, it is not easy. |\n| 因果 | Therefore, / As a result, | Therefore, I decided to work hard. |\n| 举例 | For example, / such as | I like sports, such as basketball and football. |\n| 总结 | In short, / All in all, / In a word, | In a word, I love my school life. |\n\n### 万能句型\n\n- **开头句**：I think / In my opinion / As far as I''m concerned / There are several reasons.\n- **主体句**：First of all, ... / In addition, ... / Most importantly, ...\n- **原因句**：The reason is that... / because... / Thanks to... I can...\n- **结尾句**：In a word, ... is important / I hope... / I will try my best to...\n\n### ⚠️ 常犯错误\n\n1. **漏掉要点**——只看了一个要求就开始写，写完后发现还有要点没覆盖。\n2. **中式英语**——"Because I like it, so I choose it"错，因为so和because不能连用；"Very much I love this"应为"I love this very much"。\n3. **时态混乱**——全文基调确定后不能随意切换，写过去的事统一用过去时。\n4. **字数不足**——只写了40-50词，内容单薄。建议分三段写够80字。\n5. **没有连接词**——段落间直接切换，缺少过渡显得生硬。每段至少用一个连接词。\n\n### ⚡ 练一练\n\n**题1：** 以"My School Life"为题，写一篇80-100词的短文，内容包括：① 每天几点上课 ② 最喜欢的科目是什么 ③ 课后喜欢做什么。\n\n<details><summary>点击查看答案</summary>\n\n**参考答案：**\nMy School Life\n\nI go to school from Monday to Friday. Classes begin at 8:00 a.m. and end at 4:30 p.m. every day.\n\nMy favorite subject is English because it is interesting and useful. I enjoy reading English stories and learning new words. Besides, my English teacher is very kind and always helps me.\n\nAfter class, I like playing basketball with my classmates. It makes me strong and relaxed. In short, my school life is colorful and I enjoy it very much.\n\n（约110词）\n解析：三段式结构清晰，使用了because / Besides / In short三个连接词，涵盖全部三个要点，时态统一为一般现在时。\n</details>',
'话题写作五步法：审题→列要点→遣词造句→连句成篇→检查。80-100词分三段，每段至少用一个连接词。语法正确比句式高级更重要。',
@l4, 3, '["掌握","话题写作","写作","技巧"]', 'PUBLISHED', NOW(), NOW());

-- ============================================================================
-- 验证
-- ============================================================================
SELECT CONCAT('v204: English translation/writing articles created (4 inserts)') AS result;

COMMIT;
