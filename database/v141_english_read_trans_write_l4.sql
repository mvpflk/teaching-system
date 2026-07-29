-- ============================================================================
-- v141: 英语[职高] 阅读理解+翻译+写作 L4知识点教学内容
-- 12个L4节点，匹配方式：parent_id(L3名) + name(L4名)
-- 幂等安全：UPDATE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;

-- ============================================================
-- 阅读理解 — 细节理解题 (2个L4)
-- ============================================================
SET @l3_xjlj = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='细节理解题' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n直接定位文中信息指根据题干中的关键词（人名/地名/数字/专有名词）在原文中快速找到对应的原句，答案即为文中原话或同义改写。\n\n【具体说明】\n① 解题三步法：读题干并划定位词→回原文找对应处→对比选项选出同义表述。如题干问"Where does Tom live?"，原文有"Tom lives in Chengdu."→直接选含Chengdu的选项。\n② 常见同义替换：big→large, start→begin, buy→purchase。\n③ 干扰项特征：偷换概念（张冠李戴）、无中生有（文中未提及）、绝对化表述（all/never/every常错）。\n\n【常见错误】\n1. 看到选项中有原文词语就直接选→必须核对是否同义替换还是偷换概念\n2. 凭记忆答题不回原文定位→必须回原文找到依据，细节题答案必在文中\n\n【考试方向】\n单选题/信息匹配题，要求选出与原文信息一致的选项。'
WHERE parent_id = @l3_xjlj AND name = '直接定位文中信息 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\nWh-问题细节检索是针对含who/what/when/where/why/how疑问词的问题，在原文中精准定位并提取对应信息的能力。\n\n【具体说明】\n① 各Wh-词定位策略：who→人名/身份词，when→时间/日期，where→地点/场所，why→because/for/to引出的原因目的，how→方式手段。\n② 数字题注意：原文可能多处出现数字，需核对每个数字的时间/归属/条件是否与题干匹配。\n③ 顺序原则：细节题通常按文章顺序出题，找到第一题答案后可继续向后找下一题。\n\n【常见错误】\n1. 定位到错误段落→题干关键词可能出现在多段，需逐段核对\n2. 混淆相似数字→原文有"30 students"，选项是"13 students"，看错数字\n\n【考试方向】\n单选题选出与题干Wh-问题匹配的正确信息。'
WHERE parent_id = @l3_xjlj AND name = 'Wh-问题细节检索 [基础]';

-- ============================================================
-- 阅读理解 — 推理判断题 (1个L4)
-- ============================================================
SET @l3_tlpd = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='推理判断题' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n根据上下文推断隐含意思指从作者暗示、语气态度或文中线索出发，得出文中未直接说出的结论，而非主观臆测。\n\n【具体说明】\n① 标志词：infer(推断)/suggest(暗示)/imply(暗指)/probably(可能)。题干如"We can infer from the passage that..."。\n② 推断依据：从but/however/though等转折词后找作者真实观点；从举例和原因中推知结论。原文"She smiled and gave him a big hug."→可推断she was happy。\n③ 合理推断原则：答案一定是基于原文信息推导的，不能加入个人生活经验或常识判断。\n\n【常见错误】\n1. 直接选原文原句→推理题答案不会与原文完全一致，需要"推一步"\n2. 过度推断→原文只说了"下雨"，不能推断出"洪水"\n\n【考试方向】\n单选题选出从文中可以推断出的正确结论。'
WHERE parent_id = @l3_tlpd AND name = '根据上下文推断隐含意思 [中等]';

-- ============================================================
-- 阅读理解 — 主旨大意题 (1个L4)
-- ============================================================
SET @l3_zzdy = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='主旨大意题' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n概括段落/全文大意指从文章整体出发，提炼中心思想或选出最恰当的标题，而非关注个别细节。\n\n【具体说明】\n① 找主旨句位置：首句（总起）/末句（总结）/转折句（but/however后）。如首句"Smoking is harmful to health in many ways."→全文主旨为吸烟危害。\n② 标题选择题：最佳标题应覆盖全文核心内容而非某一段落。可用"反向法"——假设题目是自己写的，文章应包含哪些内容。\n③ 文章结构法：总-分-总结构中首尾呼应处即为主旨所在。\n\n【常见错误】\n1. 选细节当主旨→选项内容是正确的但只是某一段的内容，不够概括\n2. 选范围过大的选项→如文章讲"中国的熊猫"，选"世界动物保护"则太宽泛\n\n【考试方向】\n单选题选出短文的最佳标题(main idea/best title)或作者写作目的。'
WHERE parent_id = @l3_zzdy AND name = '概括段落/全文大意 [中等]';

-- ============================================================
-- 阅读理解 — 词义猜测题 (1个L4)
-- ============================================================
SET @l3_cycc = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='词义猜测题' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n根据上下文猜测生词含义指利用生词前后的定义解释、近反义词、举例或因果逻辑关系推断词义，而非依赖词典。\n\n【具体说明】\n① 定义线索法：is/means/that is/in other words后直接给出解释。如"An autobiography is a book that a person writes about his own life."→autobiography=自传。\n② 近反义词法：and/or连接近义词，but/however引出的反义词。如"She is diligent, but her brother is lazy."→diligent与lazy反义，意为勤奋。\n③ 举例与因果法：such as/for example举例说明，because/so因果关系推导。\n\n【常见错误】\n1. 脱离上下文凭熟词生义猜测→同一个词在不同语境意思不同\n2. 忽略题干要求的"加引号"的词组→词组整体可能有固定含义\n\n【考试方向】\n单选题选出划线生词/词组在文中最接近的含义。'
WHERE parent_id = @l3_cycc AND name = '根据上下文猜测生词含义 [基础]';

-- ============================================================
-- 翻译 — 英译汉 (2个L4)
-- ============================================================
SET @l3_yyh = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='英译汉' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n关键词语准确理解指在英译汉时准确把握句中实义词（动词/名词/形容词）在语境中的确切含义，避免望文生义和生硬直译。\n\n【具体说明】\n① 多义动词辨义：take可表"拿/花费/乘坐"——take a test(参加考试)不能译"拿考试"；make可表"做/使/挣钱"——make a decision(做决定)。\n② 固定搭配识别：look after(照顾)≠看后面，give up(放弃)≠给上去，run into(偶遇)≠跑进去。\n③ 否定与特殊结构：not...until(直到…才)，nothing but(只是)，too...to(太…而不能)。\n\n【常见错误】\n1. 逐词死译→"a heavy rain"译"一场重的雨"应为"大雨"\n2. 忽略固定搭配→"take part in"译"拿部分在…里"应为"参加"\n\n【考试方向】\n翻译填空题：将英语句子中加下划线的部分译为中文。'
WHERE parent_id = @l3_yyh AND name = '关键词语准确理解 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n句式结构分析指识别英语句子的主干（主谓宾）和修饰成分（定语/状语/从句），理清各成分的修饰关系，使译文通顺符合中文表达习惯。\n\n【具体说明】\n① 长句拆解：先找谓语动词→确定主句主干→再分析从句修饰关系。如"The boy who is wearing a red coat is my brother."→主干：The boy is my brother. 从句修饰boy。\n② 常见结构调序：定语从句汉语习惯前置（The book that I bought→我买的书）；状语从句一般前置（when he arrived→当他到达时…）；被动语态转主动（It was built in 2000→建于2000年）。\n③ 比较与强调结构：as...as(和…一样)，more...than(比…更)，It is...that(正是…)。\n\n【常见错误】\n1. 按英文语序直接翻译→中文"我遇到了他昨天在街上"不通，应调为"我昨天在街上遇到了他"\n2. 长句不分主次→所有信息平铺直叙，必须区分主干与从属\n\n【考试方向】\n翻译填空题：将英语长句中指定部分译为通顺的中文。'
WHERE parent_id = @l3_yyh AND name = '句式结构分析 [中等]';

-- ============================================================
-- 翻译 — 汉译英 (2个L4)
-- ============================================================
SET @l3_hyy = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='汉译英' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n基础词汇运用指在汉译英时选用考纲范围内的恰当词汇，注意词性、搭配和习惯用法，避免中式英语。\n\n【具体说明】\n① 动词选择关注及物性：arrive需接in/at（I arrived in Beijing），reach直接接宾语；spend...doing vs take sb. time to do结构差别。\n② 常见动词搭配：参加→take part in(活动)/join(组织)/attend(会议)；看→see(看见)/watch(观看)/read(阅读)；穿→wear(状态)/put on(动作)。\n③ 形容词表感觉：interesting(令人有趣的)≠interested(感到有趣的)；-ed结尾表"感到…"，-ing结尾表"令人…"。\n\n【常见错误】\n1. 中文思维直译→"我很喜欢"译"I very like"应为"I like...very much"\n2. 混淆-ed与-ing→"我很兴奋"是I''m excited，不是I''m exciting\n\n【考试方向】\n翻译填空题/书写题：将汉语句子中加下划线部分译为正确的英文。'
WHERE parent_id = @l3_hyy AND name = '基础词汇运用 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n基本句式构建指根据中文句意，正确构建英语五大基本句型（主谓/主谓宾/主系表/主谓双宾/主谓宾补），确保句子语法完整。\n\n【具体说明】\n① 五大句型：SV(He runs.)/SVO(She likes music.)/SVP(He is tall.)/SVOO(She gave me a book.)/SVOC(They made him monitor.)。\n② 语序规则：英语主语+谓语+宾语句序固定。中文"昨天我在学校见到了他"→译成"I met him at school yesterday."（时间地点放句末）。\n③ There be句型：表示"某处有某物"——There is a book on the desk.注意be动词与后面名词数一致。\n\n【常见错误】\n1. 缺主语→"下雪了"译"It is snowy"不是"snowy"\n2. 双宾语位置错→"给我书"是give me the book，不是give the book to me(口语可用但考试应优先双宾)\n\n【考试方向】\n翻译书写题：将汉语句子译成语法正确的英文句子。'
WHERE parent_id = @l3_hyy AND name = '基本句式构建 [中等]';

-- ============================================================
-- 写作 — 应用文写作 (2个L4)
-- ============================================================
SET @l3_yyw = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='应用文写作' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n书信格式与常用表达指掌握英文书信的标准结构（称呼/正文/结束语/署名/日期）及各类书信的常用句型和礼貌用语。\n\n【具体说明】\n① 格式结构：信头(右上角日期→March 15, 2026)→称呼(Dear...,逗号)→正文(开头句+主体+结尾句)→结束语(Yours sincerely,/Yours faithfully,)+署名。\n② 常用开头句：I''m writing to tell you/invite you/ask about...; How are you doing?; Thank you for your letter.\n③ 常用结尾句：Looking forward to your reply.; I hope to hear from you soon.; Best wishes to you.\n④ 文体区分：感谢信的Thank you for.../I really appreciate...；邀请信的I would like to invite you to...；建议信的You''d better/Why not/It''s a good idea to...\n\n【常见错误】\n1. 格式缺项→漏写日期或署名\n2. 结束语与称呼不匹配→称呼Dear Sir后用Yours faithfully；Dear+人名后用Yours sincerely\n\n【考试方向】\n应用文写作题：根据提示写一封约80词英文书信。'
WHERE parent_id = @l3_yyw AND name = '书信格式与常用表达 [掌握]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n通知类写作指掌握英文通知（书面通知/口头通知）的格式规范、核心要素和常用句型，能用简洁语言传达事项信息。\n\n【具体说明】\n① 书面通知格式：标题NOTICE(居中大写)→正文(时间/地点/活动/要求)→落款(发布单位+日期)。正文第一句常用"Attention, please!"或直接陈述。\n② 口头通知开头：May I have your attention, please?; Attention, please, everyone.; I have an announcement to make.\n③ 核心要素句：There will be a talk/meeting/party...; It will be held in the school hall at 2:00 p.m. on Friday.; Everyone is expected to attend.; Please be on time.\n④ 时间地点表达：on the afternoon of May 5th; in the meeting room on the 3rd floor; at the school gate at 8:00 a.m. 注意介词的准确使用。\n\n【常见错误】\n1. 遗漏通知要素→必须包含what/when/where/who四个要点\n2. 文体混淆→书面通知用标题和落款，口头通知有开场呼语，两者格式不同\n\n【考试方向】\n应用文写作题：根据提示写一份约80词英文通知。'
WHERE parent_id = @l3_yyw AND name = '通知类写作 [掌握]';

-- ============================================================
-- 写作 — 话题写作 (1个L4)
-- ============================================================
SET @l3_htxz = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='话题写作' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n观点表达与逻辑衔接指围绕给定话题清晰陈述个人观点，并用连接词使段落之间和句子之间逻辑流畅、层次分明。\n\n【具体说明】\n① 常用连接词分类：表顺序→first/firstly, second/secondly, finally；表递进→besides, what''s more, in addition；表转折→however, on the other hand, but；表因果→therefore, so, as a result；表总结→in a word, all in all, in short。\n② 观点表达句模板：In my opinion, ...; I think/believe that...; As far as I am concerned, ...; There are several reasons for this. First, ... Second, ...\n③ 常见话题素材：校园生活→I enjoy taking part in after-school activities.; 家庭朋友→My parents always encourage me to try my best.; 未来规划→I dream of becoming a teacher in the future.\n\n【常见错误】\n1. 逻辑断裂→上一段讲好处，下一段突然讲别的内容，缺少过渡句\n2. 连接词滥用→每句都用and/then开头，应灵活使用多种连接词\n\n【考试方向】\n话题写作题：就校园生活/家庭朋友/未来规划等话题写80-100词短文。'
WHERE parent_id = @l3_htxz AND name = '观点表达与逻辑衔接 [掌握]';

SELECT 'v141: Reading+Translation+Writing L4 done' AS result;
