-- ============================================================================
-- v138: 英语[职高] L1+L2+L3 基础内容填充
-- 幂等安全：UPDATE + content IS NULL
-- ⚠️ 不覆盖v84语法节点（已有内容）
-- ============================================================================
SET @subj = 24;

-- L1 根节点
UPDATE knowledge_nodes SET content =
'## 英语[职高]\n\n'
'四川省对口升学考试英语科目，依据教育部《中等职业学校英语课程标准（2020年版）》命制。考查词汇、语法、阅读理解、翻译和写作五大模块。\n\n'
'**词汇量**：约2000词。**分值**：词汇语法约35% / 阅读约30% / 翻译约15% / 写作约20%。\n\n'
'**难度**：基础约60% / 中等约30% / 较难约10%。\n\n'
'**题型**：单选+完形+阅读+翻译+写作。'
WHERE subject_id=@subj AND level=1 AND content IS NULL;

-- ============================================================
-- v75 L2 章节 (4个)
-- ============================================================
UPDATE knowledge_nodes SET content =
'## 基础知识\n\n考查基础词汇与语法运用能力，包括词义辨析、时态语态、非谓语动词、从句等核心语法，以及日常情景交际用语。以单选题为主，分值占比约35%。\n\n【重难点】时态的正确选择、非谓语动词形式的判断、从句连接词的使用。'
WHERE subject_id=@subj AND name='基础知识' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 阅读理解\n\n考查3-4篇短文（每篇100-150词）的阅读理解能力。题型包括细节理解、推理判断、主旨概括和词义猜测。以单选题为主，兼顾信息匹配等任务型阅读，分值占比约30%。\n\n【重难点】根据题干定位信息、在文中区分"直接信息"与"需要推断"的内容。'
WHERE subject_id=@subj AND name='阅读理解' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 翻译\n\n考查英译汉和汉译英的句子翻译能力。英译汉要求准确理解关键词语和句式结构；汉译英要求选择恰当的基础词汇和基本句型。以填空题或书写题呈现，分值占比约15%。\n\n【重难点】英译汉时识别复杂句式；汉译英时避免中式英语。'
WHERE subject_id=@subj AND name='翻译' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 写作\n\n考查应用文写作（约80词）和话题写作（约100词）。应用文以书信/通知/便条为主，要求格式规范；话题写作围绕校园生活/家庭朋友/未来规划等，要求观点清楚、语言基本正确。分值约20%。\n\n【重难点】书信格式完整性；避免中式英语逐字翻译。'
WHERE subject_id=@subj AND name='写作' AND level=2 AND content IS NULL;

-- ============================================================
-- v91 L2 章节 (2个)
-- ============================================================
UPDATE knowledge_nodes SET content =
'## 词汇积累\n\n词汇是英语学习的基础。本模块按考纲要求分类整理约2000个核心词汇，分为高频核心300词、考试核心500词和考纲拓展词三个层次，循序渐进积累。\n\n【学习重点】动词辨析（spend/cost/take等）、词组搭配（take care of/look forward to等）、易混淆词对比。\n【记忆方法】词根词缀法、语境记忆法、分类记忆法。'
WHERE subject_id=@subj AND name='词汇积累' AND level=2 AND content IS NULL;

UPDATE knowledge_nodes SET content =
'## 语法专项\n\n英语语法的系统学习和专项训练，涵盖十大语法专题：时态语态、非谓语动词、定语从句、名词性从句、状语从句、主谓一致、情态动词、虚拟语气和情景交际。以单选题为主要考查形式。\n\n【学习重点】九大时态的构成和使用场景、非谓语动词的三种形式及功能、各类从句的连接词和语序、主谓一致三原则。'
WHERE subject_id=@subj AND name='语法专项' AND level=2 AND content IS NULL;

-- ============================================================
-- v91 L3 内容 (21个L3节点)
-- ============================================================

-- 词汇积累 L3
UPDATE knowledge_nodes SET content='## 高频核心300词\n\n英语中使用频率最高的约300个基础词汇，包括动词（be/have/do/make/go/get等）、名词（time/way/people等）、形容词副词（good/big/well等）、介词连词（in/on/at/and/but等）。是听、说、读、写的基础。学习方法：在句子和语境中记忆，避免孤立背单词。' WHERE subject_id=@subj AND name='高频核心300词' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 考试核心500词\n\n对口升学考试中出现频率较高的约500个词汇，包括易混淆动词辨析（spend/cost/take/pay）、名词辨析（chance/opportunity/ability）、高频词组搭配（take care of/look forward to/be used to）等。重点掌握词义差异和常见搭配。' WHERE subject_id=@subj AND name='考试核心500词' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 考纲拓展词\n\n在基础300词和核心500词的基础上，拓展阅读理解常见词和写作常用表达词。阅读拓展词包括environment/technology/experience等；写作常用词包括常用连接词（however/therefore/besides）和书信常用语（sincerely/looking forward to）。' WHERE subject_id=@subj AND name='考纲拓展词' AND level=3 AND content IS NULL;

-- 语法专项 L3 (9个)
UPDATE knowledge_nodes SET content='## 时态语态\n\n英语9大基本时态：一般现在时/过去时/将来时、现在/过去进行时、现在/过去完成时、过去将来时。被动语态：be+过去分词。考查时态在具体语境中的选择和主动语态与被动语态的转换。' WHERE subject_id=@subj AND name='时态语态' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 非谓语动词\n\n非谓语动词的三种形式：不定式(to do)、动名词(V-ing)、分词(现在分词/过去分词)。考查在句中的语法功能（主语、宾语、定语、状语等）和正确形式的选择。' WHERE subject_id=@subj AND name='非谓语动词' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 定语从句\n\n用从句修饰名词或代词。考查关系代词（that/which/who/whom/whose）和关系副词（when/where/why）的正确选择，以及限制性与非限制性定语从句的区分。' WHERE subject_id=@subj AND name='定语从句' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 名词性从句\n\n包括宾语从句、主语从句、表语从句和同位语从句。考查引导词（that/whether/if/wh-词）的选择和从句的陈述句语序。' WHERE subject_id=@subj AND name='名词性从句' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 状语从句\n\n包括时间（when/while）、条件（if/unless）、原因（because/since）、让步（although/though）、目的（so that/in order that）状语从句。考查连接词的选择和时态搭配。' WHERE subject_id=@subj AND name='状语从句' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 主谓一致\n\n三个原则：语法一致（主语单复数决定谓语形式）、就近原则（either…or/neither…nor/there be句型）、意义一致（集体名词/数量词作主语）。考查谓语动词单复数的正确选择。' WHERE subject_id=@subj AND name='主谓一致' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 情态动词\n\n基本用法：can/could(能力/请求)、must(必须)、may/might(允许/可能)、should(应该)、need(需要)。推测用法：must have done(肯定已做)、can''t have done(不可能已做)。' WHERE subject_id=@subj AND name='情态动词' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 虚拟语气\n\n表示与事实相反的假设。考查点在条件状语从句中：与现在相反(If I were…, I would…)、与过去相反(If I had done…, I would have done…)。注意"错综时间"和"省略if的倒装"。' WHERE subject_id=@subj AND name='虚拟语气' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 情景交际\n\n考查日常交际用语的掌握。包括邀请与请求（Would you like to…/Could you please…）、感谢与道歉（Thank you for…/I''m sorry…）、问路与指路、建议与劝告等场景。' WHERE subject_id=@subj AND name='情景交际' AND level=3 AND content IS NULL;

-- 阅读理解 L3 (4个)
UPDATE knowledge_nodes SET content='## 细节理解题\n\n考查从原文中直接定位和提取具体信息的能力。题干通常包含who/what/when/where/why/how等疑问词，答案可直接在文中找到对应语句。' WHERE subject_id=@subj AND name='细节理解题' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 推理判断题\n\n考查根据文本信息进行合理推断的能力。题干常含infer/suggest/probably等词，答案不直接出现在文中，需要从上下文推知。' WHERE subject_id=@subj AND name='推理判断题' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 主旨大意题\n\n考查概括段落或全文核心内容的能力。题干通常为"What is the main idea of…"、"The best title is…"。答题关键是找到中心句（常位于首句或末句）。' WHERE subject_id=@subj AND name='主旨大意题' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 词义猜测题\n\n考查根据上下文语境推断生词含义的能力。答题技巧：利用同义词/反义词线索、利用定义或解释、利用举例、利用构词法（词根词缀）。' WHERE subject_id=@subj AND name='词义猜测题' AND level=3 AND content IS NULL;

-- 翻译 L3 (2个)
UPDATE knowledge_nodes SET content='## 英译汉\n\n将英语句子翻译为通顺的中文。考查关键词语的准确理解和英文句式结构的识别。注意：译文要符合中文表达习惯，不能生硬直译。' WHERE subject_id=@subj AND name='英译汉' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 汉译英\n\n将中文句子翻译为语法正确的英文。考查基础词汇的选用和基本句型的构建。注意：选用考纲词汇，避免中式英语，检查拼写和语法。' WHERE subject_id=@subj AND name='汉译英' AND level=3 AND content IS NULL;

-- 写作 L3 (2个)
UPDATE knowledge_nodes SET content='## 应用文写作\n\n考查书信和通知等常见应用文体的写作。书信格式：称呼(Dear…)+正文+结束语(Yours sincerely)+署名+日期。通知格式：标题(NOTICE)+正文(时间/地点/事项)+发布单位+日期。字数约80词。' WHERE subject_id=@subj AND name='应用文写作' AND level=3 AND content IS NULL;
UPDATE knowledge_nodes SET content='## 话题写作\n\n围绕给定话题（校园生活、家庭朋友、环境保护、未来规划等）写一篇约80-100词的短文。要求观点清楚、内容完整、语法基本正确、有基本的逻辑衔接。' WHERE subject_id=@subj AND name='话题写作' AND level=3 AND content IS NULL;

SELECT 'v138: L1+L2+L3 内容填充完成！' AS result;
