-- ============================================================================
-- v139: 英语[职高] 词汇积累 L4知识点教学内容
-- 覆盖9个词汇L4节点：高频核心300词(4)+考试核心500词(3)+考纲拓展词(2)
-- 匹配方式: subject_id=24 + L3 name查询parent_id + L4 name匹配
-- 幂等安全: UPDATE可重复执行
-- ============================================================================
SET NAMES utf8mb4;

-- ############################################
-- 高频核心300词 (4个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='高频核心300词' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\nbe/have/do/make/take/go/get是英语使用频率最高的核心动词，掌握其基本含义、变形和常见搭配是进行日常表达的基础。\n\n【具体说明】\n① be(am/is/are→was/were→been)：表示"是/在"，He is a student. / I am at home. ② have(has→had→had)：表示"有/吃/经历"，I have a dream. / Have breakfast. ③ do(does→did→done)：助动词+实义动词"做"，Do you like it? / I did my homework. ④ make(made→made)：表示"制作/使得"，Make a cake. / It makes me happy. ⑤ take(took→taken)：表示"拿/花费/乘坐"，Take a bus. / It takes two hours. ⑥ go(went→gone)：表示"去/变得"，Go to school. / The food went bad. ⑦ get(got→got/gotten)：表示"得到/变得/到达"，Get a job. / Get up. / Get to the station.\n\n【常见错误】\n1. be动词与实义动词连用→He is go to school（×）应改为He goes to school\n2. have作助动词与实义动词混淆→I have do it（×）应改为I have done it\n\n【考试方向】\n单选/完形：考查各动词的基本搭配和变形；have/make/take等动词的固定短语。'
WHERE parent_id=@l3 AND name='动词类（be/have/do/make/take/go/get）[基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\ntime/way/day/people/place是英语最基础的高频名词，在日常交流和考试中反复出现，需掌握其多义性和常用搭配。\n\n【具体说明】\n① time：时间/次数/时代。What time is it? / three times a week / in ancient times。常用搭配：on time(准时)/in time(及时)/have a good time(玩得开心)。② way：方式/路/方面。the way to school / in this way / by the way(顺便说)。③ day：天/白天/节日。every day / day and night / New Year''s Day。④ people：人们/人民/民族。many people / the Chinese people / the peoples of Asia。⑤ place：地方/位置。a beautiful place / take place(发生) / in place of(代替)。\n\n【常见错误】\n1. time的可数与不可数混淆→many times(很多次,可数) vs much time(很多时间,不可数)\n2. in the way(挡路)与on the way(在路上)与by the way(顺便)混淆\n\n【考试方向】\n单选：名词多义辨析；固定搭配中名词的选择；take place/in time等词组考查。'
WHERE parent_id=@l3 AND name='名词类（time/way/day/people/place）[基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\ngood/bad/big/small/well是英语最基础的形容词和副词，good修饰名词，well作副词修饰动词或作形容词表示"身体好"。\n\n【具体说明】\n① good：好的（形容词修饰名词）。a good idea / be good at / be good for(对……有好处) / be good to(对……友善)。② bad：坏的/严重的/糟糕的。bad weather / be bad at / be bad for(对……有害) / not bad(还不错)。③ big：大的（尺寸/规模/重要性）。a big city / a big decision / make a big difference(产生重大影响)。④ small：小的（尺寸/数量）。a small room / small talk(闲聊)。⑤ well：好地（副词）→ do well in(在……做得好)；身体好的（形容词）→ I''m not feeling well.。as well=too(也)。well-known(著名的)。\n\n【常见错误】\n1. good与well混淆→He plays piano good（×）应改为He plays piano well（修饰动词用副词）\n2. well作形容词仅指身体健康→He is a well student（×）应改为good\n\n【考试方向】\n单选：good/well词性辨析；be good at/for/to的介词选择；as well用法。'
WHERE parent_id=@l3 AND name='形容词副词类（good/bad/big/small/well）[基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\nin/on/at/to/for/and/but是英语最基础的介词和连词，介词表时空/方向/对象关系，连词表并列/转折逻辑，是构建句子的重要纽带。\n\n【具体说明】\n① in：在……里面/在(大地点)/用(语言)。in the room / in Beijing / in English / in the morning。② on：在……上面/在(具体某天)。on the desk / on Monday / on time。③ at：在(具体点/小地点)/在(时刻)。at the door / at school / at 7 o''clock / at night。④ to：向/到/对。go to school / from…to / be kind to。⑤ for：为/对于/持续/因为。for you / for two hours / thank you for。⑥ and：和、并且（并列递进）。bread and milk / and so on。⑦ but：但是（转折）。but also（与not only连用）。\n\n【常见错误】\n1. in/on/at时间介词混淆→in Monday（×）应改为on Monday；at morning（×）应改为in the morning\n2. but与however连用→Although…but（×）二者不能同时出现在一个句子中\n\n【考试方向】\n单选：时间/地点介词辨析(in/on/at)；连词although与but、because与so不连用规则。'
WHERE parent_id=@l3 AND name='介词连词类（in/on/at/to/for/and/but）[基础]';

-- ############################################
-- 考试核心500词 (3个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='考试核心500词' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\nspend/cost/take/pay/offer/provide都表示"花费/支付/提供"，但主语、宾语和搭配结构各不相同，是考试的高频辨析点。\n\n【具体说明】\n① spend(spent→spent)：人作主语，spend…on sth / spend…(in) doing。I spent two hours on homework. / She spent the weekend reading. ② cost(cost→cost)：物作主语，sth cost sb+钱/代价。The book cost me 30 yuan. / Carelessness cost him the job. ③ take(took→taken)：it作形式主语，It takes sb+时间+to do。It takes me an hour to get to school. ④ pay(paid→paid)：人作主语，pay…for sth。I paid 50 yuan for the meal. / pay attention to(注意)。⑤ offer：主动提供，offer sb sth / offer to do。He offered me a cup of tea. ⑥ provide：供给，provide sb with sth / provide sth for sb。The school provides students with books.\n\n【常见错误】\n1. 主语与动词不匹配→I cost 50 yuan（×）cost主语必须是物，应为I spent/payed\n2. offer与provide搭配混淆→offer sb with sth（×）应改为provide sb with sth\n\n【考试方向】\n单选：选择正确的"花费"动词；offer/provide搭配辨析；常与"花费"时间/金钱题干一起出题。'
WHERE parent_id=@l3 AND name='动词辨析（spend/cost/take/pay/offer/provide）[中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\nchance/opportunity/ability/advantage均为高频抽象名词，考试常通过语境辨析它们与动词、介词的搭配使用。\n\n【具体说明】\n① chance：机会/可能性（偏偶然）。have a chance to do / by chance(偶然) / take a chance(冒险一试)。There is a good chance that…（很有可能……）。② opportunity：机会（偏正式/好时机）。have the opportunity to do / seize the opportunity / equal opportunity。注意：chance与opportunity常可互换，但chance还表"可能性""偶然"。③ ability：能力（天赋+后天），ability to do sth。He has the ability to solve the problem. 辨析：able为形容词be able to do。④ advantage：优势/好处。take advantage of(利用) / have an advantage over(优于) / to one''s advantage(对某人有利)。反义词：disadvantage。\n\n【常见错误】\n1. ability后接of doing→the ability of solving（×）应改为the ability to solve\n2. take advantage of误用→take the advantage to do（×）应为take advantage of sth\n\n【考试方向】\n单选/完形：名词搭配选择；chance与opportunity语境区分；ability to do与be able to do转换。'
WHERE parent_id=@l3 AND name='名词辨析（chance/opportunity/ability/advantage）[中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\ntake care of/look forward to/be used to等高频词组搭配是考试必考内容，重点是介词用法、后续动词形式以及相近词组的区分。\n\n【具体说明】\n① take care of=look after 照顾/处理。Please take care of my dog. / I will take care of the problem. 注意：careful为形容词，be careful with/of。② look forward to 期待（to是介词！后接V-ing/名词）。I look forward to hearing from you.（听力/书信类高频）。③ be used to 习惯于（to是介词！后接V-ing/名词）。I am used to getting up early. 辨析：used to do(过去常常做，现已不做)；be used to do(被用来做，被动语态)。④ 其他必考搭配：pay attention to(注意)、make progress in(在……进步)、take part in(参加)、make a difference(有影响)、play a role in(起作用)。\n\n【常见错误】\n1. look forward to后接动词原形→look forward to see you（×）应为look forward to seeing you\n2. used to do与be used to doing混淆→I used to get up early now（×）应为I am used to\n\n【考试方向】\n单选/完形：词组中to是介词还是不定式的判断；used to/be used to/be used to do三者的辨析。'
WHERE parent_id=@l3 AND name='词组搭配（take care of/look forward to/be used to）[中等]';

-- ############################################
-- 考纲拓展词 (2个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='考纲拓展词' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n阅读拓展词包括阅读理解中常见的主题词汇（科技/环境/社会/文化等），虽不要求全部拼写但需能辨识词义以帮助理解文章。\n\n【具体说明】\n① 科技类：technology(技术)、digital(数字的)、invention(发明)、research(研究)、AI(人工智能/artificial intelligence)。② 环境类：environment(环境)、pollution(污染)、climate(气候)、recycle(回收)、protect(保护)。③ 社会文化类：culture(文化)、tradition(传统)、volunteer(志愿者)、communication(交流)、global(全球的)。④ 个人成长类：experience(经验/经历)、challenge(挑战)、attitude(态度)、confident(自信的)。⑤ 关键词注意：注意词根词缀，如-tion(名词后缀)、-ful(形容词后缀)、un-/in-(否定前缀)，帮助推测词义。\n\n【常见错误】\n1. 按中文逐字翻译理解→environment不只是"环境"，在不同语境可指"自然环境""社会环境"\n2. 忽略上下文直接取字典第一义→experience可指"经验"(不可数)或"经历"(可数)\n\n【考试方向】\n阅读理解：词义猜测题可能涉及这些拓展词；利用上下文线索和构词法推断词义。'
WHERE parent_id=@l3 AND name='阅读拓展词 [了解]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n写作常用词包括应用文和话题写作中常用的连接词、书信套语和观点表达词，熟悉这些词汇能提升写作的逻辑性和规范性。\n\n【具体说明】\n① 连接词（逻辑衔接）：表递进→what''s more / besides / in addition；表转折→however / on the other hand / instead；表因果→therefore / as a result / because of；表总结→in a word / in conclusion / to sum up。② 书信套语：开头→I am writing to tell you... / How are you doing?；结尾→Looking forward to your reply. / Best wishes! / Yours sincerely。③ 观点表达：in my opinion / as far as I know / I think/believe that / personally speaking。④ 举例/解释：for example / such as / that is to say / in other words。⑤ 提醒：写作时优先使用考纲内基础词汇（如important比significant更稳妥），确保拼写正确。\n\n【常见错误】\n1. 连接词误用→用了however后又加but（重复），正确只用其中一个\n2. 书信结尾looking forward to后忘记V-ing→Looking forward to hear from you（×）应为hearing\n\n【考试方向】\n写作：应用文格式中书信套语的使用；话题写作中连接词的恰当运用；评分中考虑逻辑衔接。'
WHERE parent_id=@l3 AND name='写作常用词 [理解]';

SELECT 'v139: Vocab L4 done' AS result;
