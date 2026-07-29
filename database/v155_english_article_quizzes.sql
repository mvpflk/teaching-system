-- ============================================================================
-- v155: English[职高] article quizzes — fill quiz JSON column
-- Uses JSON_ARRAY/OBJECT to avoid escaping issues
-- Idempotent: re-runnable
-- ============================================================================
SET NAMES utf8mb4;

-- 词汇积累 > 高频核心300词 (5 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','以下哪个是 make 的常用搭配？','type','choice','options',JSON_ARRAY('make a decision','make a bus','make to school','make up late'),'answer','make a decision','explanation','make a decision 意为"做决定"，是固定搭配。'),
  JSON_OBJECT('question','"花费"的正确表达是：','type','choice','options',JSON_ARRAY('It takes me 30 min to walk','I take 30 min to walk','Take 30 min walking','30 min takes me'),'answer','It takes me 30 min to walk','explanation','It takes + 人 + 时间 + to do 是固定句式。'),
  JSON_OBJECT('question','get 在下列哪句中表示"变得"？','type','choice','options',JSON_ARRAY('Get up at 7','Get to school','Get married','Get a present'),'answer','Get married','explanation','get + 形容词/过去分词 = 变得…。get married = 结婚。'),
  JSON_OBJECT('question','have a meeting 中 have 的意思是？','type','choice','options',JSON_ARRAY('有','吃','进行','不得不'),'answer','进行','explanation','have a meeting = 开会。have 有多义性：have breakfast(吃), have a book(有), have to(不得不)。'),
  JSON_OBJECT('question','go+V-ing 结构的正确例子是：','type','choice','options',JSON_ARRAY('go swim','go to swim','go swimming','go to swimming'),'answer','go swimming','explanation','go + V-ing 表示"去做某事"，如 go shopping / go fishing。')
) WHERE subject_id=24 AND task='高频核心300词';

-- 词汇积累 > 考试核心500词 (4 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','"这本书花了我 50 元"的正确译法是：','type','choice','options',JSON_ARRAY('I spent 50 yuan on this book','This book cost me 50 yuan','It took me 50 yuan to buy','I paid 50 yuan for this book'),'answer','I spent 50 yuan on this book','explanation','spend + 钱 + on + 物。cost 主语是物；take 主语是 it；pay + 钱 + for + 物。'),
  JSON_OBJECT('question','opportunity 和 chance 的区别是？','type','choice','options',JSON_ARRAY('完全同义','opportunity 指好机会，chance 可指可能性','chance 更正式','opportunity 只用于商务'),'answer','opportunity 指好机会，chance 可指可能性','explanation','chance 可表"可能性"（There is a chance that...），opportunity 专指"良机"。'),
  JSON_OBJECT('question','look forward to 后面接什么？','type','choice','options',JSON_ARRAY('动词原形','动词-ing','过去式','不定式'),'answer','动词-ing','explanation','look forward to 的 to 是介词，后接名词或 V-ing。类似：be used to, pay attention to。'),
  JSON_OBJECT('question','"妈妈对我说"的正确译法是：','type','choice','options',JSON_ARRAY('Mom said to me','Mom told me','Mom spoke to me','Mom talked to me'),'answer','Mom said to me','explanation','say to sb = 对某人说。tell sb 不带 to，speak to sb 强调"说话动作"，talk to sb = 交谈。')
) WHERE subject_id=24 AND task='考试核心500词';

-- 词汇积累 > 考纲拓展词 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','environment 的中文意思是？','type','choice','options',JSON_ARRAY('设备','环境','娱乐','入口'),'answer','环境','explanation','常考搭配：protect the environment（保护环境）。'),
  JSON_OBJECT('question','in my opinion 在写作中的意思是？','type','choice','options',JSON_ARRAY('在我看来','另一方面','总的来说','例如'),'answer','在我看来','explanation','常用于话题写作开头，表达个人观点。'),
  JSON_OBJECT('question','experience 作"经验"解时，以下用法正确的是？','type','choice','options',JSON_ARRAY('He has many experiences','He has much experience','He has a lot of experiences','Experiences are important'),'answer','He has much experience','explanation','experience 作"经验"不可数，用 much / a lot of。作"经历"时可数：I had many interesting experiences。')
) WHERE subject_id=24 AND task='考纲拓展词';

-- 语法专项 > 时态语态 (5 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','She ____ to school every day.','type','choice','options',JSON_ARRAY('go','goes','going','went'),'answer','goes','explanation','every day → 一般现在时，主语 she 三单，动词加 -s。'),
  JSON_OBJECT('question','They ____ football yesterday afternoon.','type','choice','options',JSON_ARRAY('play','played','will play','are playing'),'answer','played','explanation','yesterday afternoon → 一般过去时，动词用过去式。'),
  JSON_OBJECT('question','I ____ this book for two weeks.','type','choice','options',JSON_ARRAY('have','have had','am having','had'),'answer','have had','explanation','for two weeks → 现在完成时，表持续到现在的动作。'),
  JSON_OBJECT('question','The bridge ____ last year.','type','choice','options',JSON_ARRAY('built','was built','builds','is built'),'answer','was built','explanation','桥被建造 → 被动语态。last year → 一般过去时。'),
  JSON_OBJECT('question','If it ____ tomorrow, we will stay home.','type','choice','options',JSON_ARRAY('rain','rains','will rain','rained'),'answer','rains','explanation','if 条件状语从句：主将从现（主句将来时，从句一般现在时）。')
) WHERE subject_id=24 AND task='时态语态';

-- 语法专项 > 非谓语动词 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','I enjoy ____ in the river.','type','choice','options',JSON_ARRAY('swim','to swim','swimming','swam'),'answer','swimming','explanation','enjoy 后接 V-ing（动名词）。类似：finish, mind, practice, avoid。'),
  JSON_OBJECT('question','He wants ____ a doctor.','type','choice','options',JSON_ARRAY('be','being','to be','been'),'answer','to be','explanation','want 后接不定式 to do。类似：hope, decide, plan, wish。'),
  JSON_OBJECT('question','I saw him ____ the street.','type','choice','options',JSON_ARRAY('cross','to cross','crossing','crossed'),'answer','cross','explanation','感官动词 see/watch/hear + sb do（不带to，表全过程）。+ doing 表"正在做"。')
) WHERE subject_id=24 AND task='非谓语动词';

-- 语法专项 > 定语从句 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','The book ____ I bought yesterday is interesting.','type','choice','options',JSON_ARRAY('who','which','whom','what'),'answer','which','explanation','先行词 book（物），关系代词用 which 或 that。'),
  JSON_OBJECT('question','The man ____ is standing over there is my teacher.','type','choice','options',JSON_ARRAY('which','whom','who','whose'),'answer','who','explanation','先行词 man（人），在从句中作主语，用 who/that。'),
  JSON_OBJECT('question','I remember the day ____ I first came here.','type','choice','options',JSON_ARRAY('which','when','where','why'),'answer','when','explanation','先行词 the day（时间），关系副词用 when。')
) WHERE subject_id=24 AND task='定语从句';

-- 语法专项 > 名词性从句 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','I don''t know ____ he will come or not.','type','choice','options',JSON_ARRAY('if','whether','that','what'),'answer','whether','explanation','whether...or not 是固定搭配，不能用 if 替换。'),
  JSON_OBJECT('question','____ is important that we practice English daily.','type','choice','options',JSON_ARRAY('This','That','It','What'),'answer','It','explanation','It 作形式主语，真正主语是 that 从句。It + be + 形容词 + that...。'),
  JSON_OBJECT('question','The question is ____ we can finish on time.','type','choice','options',JSON_ARRAY('whether','if','that','what'),'answer','whether','explanation','表语从句中 whether 比 if 更正式，可与 or not 连用。')
) WHERE subject_id=24 AND task='名词性从句';

-- 语法专项 > 状语从句 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','____ it rains, we will cancel the picnic.','type','choice','options',JSON_ARRAY('Because','If','Although','Unless'),'answer','If','explanation','if 引导条件状语从句，主将从现原则。'),
  JSON_OBJECT('question','He didn''t go to school ____ he was ill.','type','choice','options',JSON_ARRAY('so','but','because','or'),'answer','because','explanation','because 引导原因状语从句。注意 because 和 so 不能同时用。'),
  JSON_OBJECT('question','____ he is young, he knows a lot.','type','choice','options',JSON_ARRAY('Because','So','Although','If'),'answer','Although','explanation','although 引导让步状语从句。although 和 but 不能同时用。')
) WHERE subject_id=24 AND task='状语从句';

-- 语法专项 > 主谓一致 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','Either you or he ____ wrong.','type','choice','options',JSON_ARRAY('is','are','were','have'),'answer','is','explanation','就近原则：谓语与最近的主语 he 一致，用单数 is。'),
  JSON_OBJECT('question','The teacher, together with the students, ____ going.','type','choice','options',JSON_ARRAY('is','are','were','have'),'answer','is','explanation','主语是 the teacher（单数）。together with... 是附属成分不影响主语数。'),
  JSON_OBJECT('question','Mathematics ____ my favorite subject.','type','choice','options',JSON_ARRAY('is','are','were','have'),'answer','is','explanation','mathmatics 以 -s 结尾但指单一学科，视为单数。')
) WHERE subject_id=24 AND task='主谓一致';

-- 语法专项 > 情态动词 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','You ____ cross the street when the light is red.','type','choice','options',JSON_ARRAY('must','mustn''t','may','can'),'answer','mustn''t','explanation','mustn''t = 禁止。红灯时禁止过马路。'),
  JSON_OBJECT('question','He ____ be at home. The light is on.','type','choice','options',JSON_ARRAY('must','can''t','may','need'),'answer','must','explanation','must 表推测"一定"。灯亮 → 他一定在家。否定推测用 can''t。'),
  JSON_OBJECT('question','____ you help me with this box?','type','choice','options',JSON_ARRAY('Must','May','Could','Need'),'answer','Could','explanation','Could you...? 是比 Can you? 更礼貌的请求句式。')
) WHERE subject_id=24 AND task='情态动词';

-- 语法专项 > 虚拟语气 (2 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','If I ____ you, I would study harder.','type','choice','options',JSON_ARRAY('am','was','were','be'),'answer','were','explanation','与现相反虚拟语气，be 动词一律用 were（不管主语人称）。'),
  JSON_OBJECT('question','If he had studied hard, he ____ the exam.','type','choice','options',JSON_ARRAY('passed','would pass','would have passed','passes'),'answer','would have passed','explanation','与过去相反虚拟语气：从句 had done，主句 would have done。')
) WHERE subject_id=24 AND task='虚拟语气';

-- 语法专项 > 情景交际 (4 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','Would you like to go to the cinema? 的最佳回答是：','type','choice','options',JSON_ARRAY('Yes, I''d love to','Yes, I like','Yes, please','Yes, I do'),'answer','Yes, I''d love to','explanation','Would you like to...? 肯定回答用 I''d love to / I''d like to。'),
  JSON_OBJECT('question','Thank you for your help. 的恰当回应是：','type','choice','options',JSON_ARRAY('No, thanks','You''re welcome','That''s right','I''m fine'),'answer','You''re welcome','explanation','感谢回应：You''re welcome / My pleasure / Not at all。'),
  JSON_OBJECT('question','I''m sorry for breaking your cup. 的恰当回应是：','type','choice','options',JSON_ARRAY('You''re welcome','It doesn''t matter','That''s right','I think so'),'answer','It doesn''t matter','explanation','道歉回应：It doesn''t matter / That''s OK / Never mind。'),
  JSON_OBJECT('question','How about going swimming this weekend? 的最佳回答是：','type','choice','options',JSON_ARRAY('Good idea','Yes, I do','I''d like it','That''s all right'),'answer','Good idea','explanation','How about...? 表建议，肯定回答用 Good idea / Sounds great / Great!。')
) WHERE subject_id=24 AND task='情景交际';

-- 阅读理解 > 细节理解题 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','"词对干扰"是指：','type','choice','options',JSON_ARRAY('选项出现原文相同词但意思不同','选项有很多生词','选项与原文用词完全不同','选项太短无法判断'),'answer','选项出现原文相同词但意思不同','explanation','词对干扰是最常见陷阱——学生看到熟悉词就选，但意思可能不同。'),
  JSON_OBJECT('question','否定问句(except/not/incorrect)最好用哪种方法？','type','choice','options',JSON_ARRAY('选最短选项','选最长选项','排除法逐一验证','选含原文词汇的选项'),'answer','排除法逐一验证','explanation','排除法逐一排除符合原文的选项，剩下的就是答案。'),
  JSON_OBJECT('question','细节题的答案在文中以什么形式出现？','type','choice','options',JSON_ARRAY('需要推断的隐含信息','文章主旨','直接陈述的事实','作者观点'),'answer','直接陈述的事实','explanation','细节题考查直接陈述或同义替换后的具体信息。')
) WHERE subject_id=24 AND task='细节理解题';

-- 阅读理解 > 推理判断题 (2 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','以下哪个不是推理判断题的标志词？','type','choice','options',JSON_ARRAY('infer','suggest','imply','mainly about'),'answer','mainly about','explanation','mainly about 是主旨大意题的标志词。infer/suggest/imply 是推断题标志。'),
  JSON_OBJECT('question','推断题的常见失分原因是：','type','choice','options',JSON_ARRAY('选原文直接陈述的选项','选择最合理的推论','排除绝对选项','选最有创意的答案'),'answer','选原文直接陈述的选项','explanation','原文直接陈述的是细节，不是推断。过度推理和选原文原句都是失分原因。')
) WHERE subject_id=24 AND task='推理判断题';

-- 阅读理解 > 主旨大意题 (2 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','做主旨题时，首先应该读文章的哪个部分？','type','choice','options',JSON_ARRAY('只读第一句','只读最后一段','首尾段和各段首句','通读全文所有句子'),'answer','首尾段和各段首句','explanation','首尾段和各段首句通常包含主旨，这是最高效策略。'),
  JSON_OBJECT('question','best title 和 main idea 的区别是？','type','choice','options',JSON_ARRAY('没有区别','标题是短语，主旨是句子','标题包含数据','主旨比标题短'),'answer','标题是短语，主旨是句子','explanation','best title 是短语（如"Benefits of Team Sports"），main idea 是句子。')
) WHERE subject_id=24 AND task='主旨大意题';

-- 阅读理解 > 词义猜测题 (2 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','以下哪项不是有效的上下文线索？','type','choice','options',JSON_ARRAY('定义线索(means)','对比线索(but)','字母数量线索','举例线索(such as)'),'answer','字母数量线索','explanation','词义猜测靠上下文语义，不能靠单词字母数量。'),
  JSON_OBJECT('question','The food was palatable, not like the terrible meal we had. palatable 的意思是？','type','choice','options',JSON_ARRAY('难吃的','可口的','便宜的','昂贵的'),'answer','可口的','explanation','通过 not like the terrible meal 推断 palatable 与 terrible 相反，即"可口的"。')
) WHERE subject_id=24 AND task='词义猜测题';

-- 阅读理解 > 综合阅读技巧 (2 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','每篇阅读理解建议分配多长时间？','type','choice','options',JSON_ARRAY('3-4分钟','7-8分钟','10-12分钟','15分钟'),'answer','7-8分钟','explanation','20题阅读总用时35-40分钟，平均每篇7-8分钟。'),
  JSON_OBJECT('question','时间不够时，以下哪个策略最合理？','type','choice','options',JSON_ARRAY('放弃阅读直接蒙','优先做细节题','优先做主旨题','每道题都认真做'),'answer','优先做细节题','explanation','细节题最容易拿分且占阅读约50%，时间不够应优先完成。')
) WHERE subject_id=24 AND task='综合阅读技巧';

-- 补全对话 > 补全对话 (4 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','How do you do? 的最佳回答是：','type','choice','options',JSON_ARRAY('How do you do?','Fine, thank you.','I''m OK.','How are you?'),'answer','How do you do?','explanation','How do you do? 是正式初次见面问候，回答也用 How do you do?。'),
  JSON_OBJECT('question','Would you mind opening the window? 的恰当回答是：','type','choice','options',JSON_ARRAY('Yes, please.','No, not at all.','Yes, I mind.','No, thanks.'),'answer','No, not at all.','explanation','Would you mind...? 不介意用 No, not at all。介意用 Yes, I do mind。'),
  JSON_OBJECT('question','Congratulations on your success! 的最佳回答是：','type','choice','options',JSON_ARRAY('Good luck!','Thank you!','The same to you.','I''m sorry.'),'answer','Thank you!','explanation','祝贺和赞美回应用 Thank you。Good luck 用于祝福即将发生的事。'),
  JSON_OBJECT('question','I''m afraid I can''t go to the party. 的恰当回应是：','type','choice','options',JSON_ARRAY('What a pity!','That''s all right.','Congratulations!','Good luck!'),'answer','What a pity!','explanation','表达遗憾用 What a pity / What a shame。That''s all right 用于回报道歉或感谢。')
) WHERE subject_id=24 AND task='补全对话';

-- 短文改错 > 短文改错 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','短文改错中最常见的错误类型是？','type','choice','options',JSON_ARRAY('拼写错误','标点错误','动词时态错误','大写错误'),'answer','动词时态错误','explanation','动词时态/语态错误占改错约30%，是最高频考点。'),
  JSON_OBJECT('question','短文改错中下列哪项是错误的做法？','type','choice','options',JSON_ARRAY('先通读全文确定时态','逐句检查语法一致性','只看划线部分不看上下文','注意冠词和介词搭配'),'answer','只看划线部分不看上下文','explanation','改错必须结合上下文，不能只看单独的句子。'),
  JSON_OBJECT('question','以下哪项是常见的连词错误？','type','choice','options',JSON_ARRAY('because 和 so 同时出现','and 和 but 都用','or 用于否定句','either...or 连接主语'),'answer','because 和 so 同时出现','explanation','because 和 so 不能出现在同一句中——中文直译"因为…所以…"导致的错误。')
) WHERE subject_id=24 AND task='短文改错';

-- 写作 > 应用文写作 (3 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','英语书信的正确格式顺序是：','type','choice','options',JSON_ARRAY('称呼→正文→结束语→署名→日期','日期→称呼→正文→结束语→署名','署名→日期→称呼→正文→结束语','正文→称呼→日期→署名→结束语'),'answer','日期→称呼→正文→结束语→署名','explanation','英语书信：右上角日期 → 左起称呼 → 正文 → 结束语 → 署名。'),
  JSON_OBJECT('question','称呼为 Dear Sir or Madam，结尾应用：','type','choice','options',JSON_ARRAY('Yours sincerely','Yours faithfully','Best wishes','Yours'),'answer','Yours faithfully','explanation','Dear Sir/Madam → Yours faithfully。Dear + 人名 → Yours sincerely。'),
  JSON_OBJECT('question','通知(NOTICE)必须包含的四要素是：','type','choice','options',JSON_ARRAY('标题、署名、日期、电话','what/when/where/who','问候、正文、结束语、签名','主题、地点、费用、联系方式'),'answer','what/when/where/who','explanation','通知核心四要素：事项(what)、时间(when)、地点(where)、参加人(who)。')
) WHERE subject_id=24 AND task='应用文写作';

-- 写作 > 话题写作 (2 questions)
UPDATE knowledge_articles SET quiz = JSON_ARRAY(
  JSON_OBJECT('question','话题写作推荐的结构是：','type','choice','options',JSON_ARRAY('一段到底','三段式(开头→主体→结尾)','五段式','按时间顺序写'),'answer','三段式(开头→主体→结尾)','explanation','80-100词推荐三段式：开头引出话题、主体2-3要点、结尾总结。'),
  JSON_OBJECT('question','以下哪个衔接词表示"转折"关系？','type','choice','options',JSON_ARRAY('First','Besides','However','Finally'),'answer','However','explanation','However = 然而/但是（转折）。First = 首先，Besides = 此外（递进），Finally = 最后（顺序）。')
) WHERE subject_id=24 AND task='话题写作';

SELECT CONCAT('v155: quiz populated for ', (SELECT COUNT(*) FROM knowledge_articles WHERE subject_id=24 AND quiz IS NOT NULL), ' articles') AS result;
