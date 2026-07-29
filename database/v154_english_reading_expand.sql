-- ============================================================================
-- v154: 英语[职高] 阅读理解扩至10篇（原文5→10）
-- 1. 细节理解题 +1 L4
-- 2. 推理判断题 +1 L4
-- 3. 主旨大意题 +1 L4
-- 4. 新增 L3 综合阅读技巧 +2 L4
-- 幂等: INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;

-- ══════════════════════════════════════════
-- 新增 L3: 综合阅读技巧 (sort=5)
-- ══════════════════════════════════════════
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT 880, 24, 3, '综合阅读技巧', 5, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_nodes WHERE parent_id=880 AND name='综合阅读技巧' AND level=3);

SET @l3_strategy = (SELECT id FROM knowledge_nodes WHERE parent_id=880 AND name='综合阅读技巧' AND level=3 LIMIT 1);

-- 题型判别与策略选择 [掌握] L4
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_strategy, 24, 4, '题型判别与策略选择 [掌握]',
'【一句话定义】快速识别阅读理解四大题型（细节/推断/主旨/词义猜测），选择对应的解题策略。

【具体说明】细节题: 题干含Who/What/When/Where/Why/How→回文定位、找关键词同义替换。推断题: 题干含infer/suggest/imply/probably→文中线索+逻辑推理、不选原文照抄项。主旨题: 题干含main idea/best title/purpose→首尾段+各段首句、排除以偏概全。词义猜测题: 生词/划线词→上下文线索（定义/举例/对比/因果）。

【常见错误】①细节题被"词对"迷惑（选项含与原文相同词汇但不符题意）；②推断题过度推理（选择"可能正确但原文无依据"的选项）；③主旨题被细节段落带偏。

【考试方向】2024年真题阅读理解20题×2分=40分，四类题型均有覆盖。细节题占约50%最多，推断题约25%，主旨+猜测约25%。',
1, 'ACTIVE');

-- 时间分配与答题顺序 [掌握] L4
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_strategy, 24, 4, '时间分配与答题顺序 [掌握]',
'【一句话定义】合理分配20道阅读理解题的作答时间，采用最优答题顺序实现分数最大化。

【具体说明】总时间: 阅读理解建议分配35-40分钟。单篇平均7-8分钟: 2分钟通读全文（首尾段+各段首句）、4分钟逐题定位作答、1-2分钟检查。答题顺序: 先细节题（最容易拿分）→词义猜测题→推断题→主旨题（需全文理解）。跳题策略: 单题卡壳超过2分钟先跳过、标记后返回。

【常见错误】①在难题上纠结超时导致简单题没时间做；②先读文章再看题（浪费时间），应先读题干再看文；③每字每句都读（太慢），应扫读定位。

【考试方向】时间管理是阅读拿高分的关键。细节题平均40秒/题，推断题60秒/题，主旨题90秒/题。建议每天2篇阅读限时训练（15分钟内完成）。',
2, 'ACTIVE');

-- ══════════════════════════════════════════
-- 细节理解题 +1 L4: 细节理解进阶训练 [中等]
-- ══════════════════════════════════════════
SET @l3_detail = 2955;
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_detail, 24, 4, '细节理解进阶训练 [中等]',
'【一句话定义】在基础定位之上，训练同义转换、干扰项排除和跨段落信息整合能力。

【具体说明】同义转换: 选项用不同词表达原文意思（原文"not until 1990"→选项"after 1990"）。干扰项类型: ①词对干扰（含原文词汇但意思不同）；②范围扩大/缩小（all→some/some→all）；③张冠李戴（把A的特征说成B）。跨段整合: 答案分散在两个段落中、需要综合两段信息才能作答。

【常见错误】①看到选项中出现原文一模一样的词就选（可能是陷阱）；②只看一段就选答案而答案在后一段；③题干否定词（except/not/incorrect）被忽略。

【考试方向】中等难度细节题在2024年真题中约占8-10题。标志: "Which of the following is TRUE?" "What can we learn from...?" 答案需要对多个信息点做综合判断。',
3, 'ACTIVE');

-- ══════════════════════════════════════════
-- 推理判断题 +1 L4: 推断题技巧综合 [困难]
-- ══════════════════════════════════════════
SET @l3_infer = 2956;
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_infer, 24, 4, '推断题技巧综合 [困难]',
'【一句话定义】综合运用多种推理策略，从字里行间获取隐含信息，区分可推断和不可推断的选项。

【具体说明】推理方向: ①因果推理（文中因→推理果/文中果→推理因）；②态度推理（作者用词褒贬推断态度）；③来源推理（推断文章出处newspaper/magazine/textbook）。排除法: ①太绝对的选项（always/never/all/only）通常错；②与常识一致但文中未提的不能选；③原文直接陈述的不是"推断"而是"细节"。

【常见错误】①把细节题当推断题做（选出原文原话）；②过度推理（脑补文中没写的信息）；③忽视作者用词的情感色彩（positive/negative/neutral）。

【考试方向】困难难度、区分度最高。2024年真题约3-4题。标志词: infer/suggest/imply/conclude/probably/most likely。建议优先做细节题、最后做推断题。',
2, 'ACTIVE');

-- ══════════════════════════════════════════
-- 主旨大意题 +1 L4: 主旨大意进阶训练 [困难]
-- ══════════════════════════════════════════
SET @l3_main = 2957;
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_main, 24, 4, '主旨大意进阶训练 [困难]',
'【一句话定义】应对隐含主旨和"最佳标题"类难题，区分主旨与细节、主旨与过度概括。

【具体说明】主旨题两种形式: ①概括主旨（main idea of the passage）——综合各段大意；②最佳标题（best title）——简短有力、吸引人、覆盖全文。解题三步: ①读首尾段和各段首句；②排除只含一个段落的选项（太窄）；③排除范围过大/文中没提到的选项（太宽）。隐含主旨技巧: 文章首段可能只是引子（举例/设问）,主旨可能在末段转折后。

【常见错误】①选择"文中某段的细节"而非"全文的主旨"；②选择"正确的常识"而非"文章的观点"；③最佳标题题选太笼统的（"About Sports"不如"Benefits of Team Sports"）。

【考试方向】每篇阅读必考0-1题，通常最后一题。每年真题约3-4题。给标题题、选main idea题各一半。',
2, 'ACTIVE');

-- ══════════════════════════════════════════
-- 5篇知识文章
-- ══════════════════════════════════════════

-- 1. 细节理解进阶训练
SET @l4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l3_detail AND name='细节理解进阶训练 [中等]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '细节理解题', '细节理解进阶训练',
'### 概述\n\n细节理解题占阅读理解总题量的约50%（10题/20题），是所有题型中占比最高、也最容易拿分的。但如果只会"找原词"而不懂同义替换和干扰项识别，仍会被中等难度细节题难住。\n\n### 同义替换（Paraphrase）\n\n选项通常不会照搬原文原词，而是用不同的词表达相同的意思。\n- 原文 "not until 1990" → 选项 "after 1990"\n- 原文 "the majority of students" → 选项 "most students"\n- 原文 "as a result" → 选项 "therefore"\n- 原文 "be responsible for" → 选项 "be in charge of"\n\n### 干扰项三大类型\n\n1. **词对干扰**：选项中出现与原文相同的词汇，但意思完全不同。例如原文说"Tom likes reading books"，选项说"Tom likes writing books"——"books"相同但"reading"≠"writing"。\n2. **范围干扰**：扩大或缩小原文范围。原文"some students"→选项"all students"（扩大）；原文"in the whole country"→选项"in Beijing"（缩小）。\n3. **张冠李戴**：把人物的特征或行为张冠李戴。A做的事说成B做的。\n\n### 跨段落信息整合\n\n中等难度细节题的答案有时不在同一段。例如：\n- 题干问 "Why did Tom feel sad?"\n- 原因在第1段（He failed the exam），结果在第2段（He felt sad）\n- 需要综合两段信息才能作答\n\n### 要点总结\n\n1. 否定词题（except/not/incorrect）用排除法，逐一验证选项\n2. "Which of the following is TRUE?" 类题往往只有一个选项完全正确且与原文一致\n3. 看到选项中有原文一模一样的词时，警惕词对干扰——确认意思是否一致',
'细节题占阅读50%，进阶训练同义替换、干扰项排除和跨段整合。三大干扰项：词对干扰、范围干扰、张冠李戴。',
@l4, 2, '["中等","阅读理解","细节题"]', 'PUBLISHED', NOW(), NOW());

-- 2. 推断题技巧综合
SET @l4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l3_infer AND name='推断题技巧综合 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '推理判断题', '推断题技巧综合',
'### 概述\n\n推理判断题是阅读理解中区分度最高的题型，考查"读懂字里行间"的能力。答案不直接出现在文中，需要根据已知信息进行逻辑推断。2024年真题约3-4题。\n\n### 三大推理方向\n\n1. **因果推理**：从结果推原因，或从原因推结果。\n   - 文中说 "He missed the bus and arrived late for the interview" → 推断 "He might have lost the job opportunity"\n2. **态度推理**：根据用词的褒贬色彩推断作者态度。\n   - 褒义用词（wonderful/excellent/important）→ 正面态度\n   - 贬义用词（terrible/disappointing/dangerous）→ 负面态度\n   - 中性用词→客观中立\n3. **来源推理**：推断文章出处。\n   - 新闻报道→ newspaper\n   - 实验结果→ science magazine / research paper\n   - 个人故事→ personal blog / magazine\n\n### 正确vs错误选项特征\n\n| 正确选项 | 错误选项 |\n|---------|---------|\n| 基于文中线索的合理推论 | 原文直接陈述（这是细节题） |\n| 语气较委婉（maybe/probably/might） | 语气绝对（must/always/never/all） |\n| 与文章整体基调一致 | 违背文章主旨或作者态度 |\n| 有逻辑依据 | 无中生有的"脑补" |\n\n### 要点总结\n\n1. 标志词：infer / suggest / imply / conclude / probably / most likely\n2. 推理不可过度——选择最直接、最合理的推论，而非最"有创意"的\n3. 排除法最有效：先排除原文陈述项（细节）、再排除绝对项（all/never）、最后选最合理项',
'推理判断题区分度最高（infer/suggest/imply标志词）。三大方向：因果推理、态度推理、来源推理。排除绝对选项和原文陈述项。',
@l4, 3, '["困难","阅读理解","推理题"]', 'PUBLISHED', NOW(), NOW());

-- 3. 主旨大意进阶训练
SET @l4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l3_main AND name='主旨大意进阶训练 [困难]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '主旨大意题', '主旨大意进阶训练',
'### 概述\n\n主旨大意题考查对文章整体内容的理解和概括能力。每篇阅读通常有1题，2024年真题共3-4题。两类常见形式：选main idea和选best title。\n\n### Main Idea vs Best Title\n\n**Main Idea（概括主旨）**：完整的句子，概括文章的核心观点。\n- 正确：The article explains how regular exercise improves both physical and mental health.\n- 错误（太窄）：Exercise is good for the heart.（只说了physical）\n\n**Best Title（最佳标题）**：短语形式，简短有力、吸引读者。\n- 正确：Exercise: A Key to Health and Happiness\n- 错误（太泛）：About Exercise / Sports\n\n### 解题三步法\n\n1. **通览首尾段+各段首句**：80%的主旨出现在首段末句或末段首句。\n2. **排除法**：\n   - 选项只涉及一个段落→以偏概全，排除\n   - 选项文中完全没提→无中生有，排除\n   - 选项范围过大→过于笼统，排除\n3. **验证**：剩下的选项是否覆盖了全文每段的大意？\n\n### 隐含主旨技巧\n\n有时文章首段只是引子（举例、设问、讲故事），真正的观点在末段。\n- 首段："Tom started running every morning..."（举例引入）\n- 末段："In fact, regular exercise not only improves health but also boosts mood."（真正的主旨）\n- 不要被首段的生动例子带偏\n\n### 要点总结\n\n1. 主旨题通常是每篇最后一题，做完全部细节题后再做\n2. 给标题题：短而精、覆盖全文、有吸引力\n3. 各段首句连起来读一次，就能把握文章脉络',
'主旨大意题选main idea或best title。解题三步：通览首尾段→排除（太窄/太宽/无关）→验证全覆盖。隐含主旨可能在末段。',
@l4, 3, '["困难","阅读理解","主旨题"]', 'PUBLISHED', NOW(), NOW());

-- 4. 题型判别与策略选择
SET @l4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l3_strategy AND name='题型判别与策略选择 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '综合阅读技巧', '题型判别与策略选择',
'### 概述\n\n做阅读理解的第一步不是"读文章"，而是"看题干，辨题型"。不同题型有不同的解题策略，选对策略就能事半功倍。\n\n### 四大题型快速识别\n\n| 题型 | 题干关键词 | 解题策略 |\n|------|-----------|---------|\n| 细节题 | Who/What/When/Where/Why/How/Which/TRUE/Not | 回文定位→找同义替换 |\n| 推断题 | infer/suggest/imply/conclude/probably | 找线索→合理推测→排除原文直接陈述项 |\n| 主旨题 | main idea/best title/mainly about/purpose | 读首尾段+各段首句→排除以偏概全 |\n| 词义猜测 | The underlined word/...means/closest to | 上下文线索（定义/举例/对比/因果） |\n\n### 答题顺序建议\n\n1. **先读题，后看文**：每篇阅读先花30秒读题干（不读选项），知道要问什么\n2. **先细节，后主旨**：先做细节题和词义猜测（容易拿分），最后做主旨题（需全文理解）\n3. **遇难则跳**：单题卡壳超过2分钟先标记跳过，全部做完再回看\n\n### 要点总结\n\n1. 细节题做对是"拿分"，推断题做对是"拉开差距"\n2. 如果时间不够（剩下5分钟），优先做细节题——它们最容易拿分\n3. 读完题干后，带着问题去文章中扫读，比先读文章再看题更高效',
'四大题型识别：细节题找同义替换、推断题合理推测、主旨题概括全文、词义猜测看上下文。先读题干后读文，先细节后主旨。',
@l4, 4, '["掌握","阅读理解","技巧"]', 'PUBLISHED', NOW(), NOW());

-- 5. 时间分配与答题顺序
SET @l4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@l3_strategy AND name='时间分配与答题顺序 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, status, created_at, updated_at) VALUES
(24, '阅读理解', '综合阅读技巧', '时间分配与答题顺序',
'### 概述\n\n20道阅读理解题需要在35-40分钟内完成，平均每篇7-8分钟。合理的时间分配和科学的答题顺序，直接影响最终得分。\n\n### 时间分配建议\n\n| 阶段 | 时间 | 任务 |\n|------|:----:|------|\n| 通读 | 2分钟 | 读首尾段+各段首句，把握文章大意 |\n| 答题 | 4分钟 | 逐题定位、作答（细节题40秒/题，推断题60秒/题） |\n| 检查 | 1-2分钟 | 确认答案填涂、复查主旨题 |\n\n### 时间不够怎么办\n\n- **剩15分钟**：正常做，每篇7.5分钟\n- **剩10分钟**：优先做细节题（最快拿分），跳过不确定的推断题\n- **剩5分钟**：扫读细节题题干，回到文中快速定位，全选C/D保底（不要空题）\n- **剩2分钟**：检查答案是否填涂正确、卷面是否整洁\n\n### 日常训练方法\n\n- 每天 **2篇限时训练**（15分钟内完成1篇+对答案）\n- 每次训练记录：用时、正确率、错题类型（细节/推断/主旨/猜测）\n- 一周分析一次错题分布，针对性补弱\n\n### 要点总结\n\n1. 不要逐字逐句读文章——先读题干、带着问题扫读定位\n2. 一道题卡2分钟以上立即跳过，后面可能有两道简单题等着你\n3. 所有答案必须在文章中找到明确依据，不要凭"感觉"选',
'阅读理解35-40分钟完成20题，每篇7-8分钟。优先做细节题，卡壳超2分钟跳过。日常每天2篇限时训练。',
@l4, 4, '["掌握","阅读理解","技巧"]', 'PUBLISHED', NOW(), NOW());

SELECT CONCAT('v154: reading expanded from 5→10 articles (5 new L4 + 5 articles)') AS result;
