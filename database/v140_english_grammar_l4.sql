-- ============================================================================
-- v140: 英语[职高] 语法专项 L4知识点教学内容
-- 覆盖28个语法L4节点：时态语态(9)+非谓语动词(3)+定语从句(3)+名词性从句(3)
--   +状语从句(2)+主谓一致(1)+情态动词(2)+虚拟语气(1)+情景交际(4)
-- 匹配方式: subject_id=24 + L3 name查询parent_id + L4 name匹配
-- 幂等安全: UPDATE可重复执行
-- ============================================================================
SET NAMES utf8mb4;

-- ############################################
-- 时态语态 (9个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='时态语态' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n一般现在时表示经常性习惯性动作、客观真理或现在的状态，谓语动词用原形（主语三单时加-s/-es），借助do/does构成否定和疑问。\n\n【具体说明】\n① 动词三单变化规则：一般+s(works)；s/x/ch/sh/o结尾+es(watches,goes)；辅音+y结尾变y为i+es(studies)。② 频度副词常与之连用：always(总是)/usually(通常)/often(经常)/sometimes(有时)/never(从不)，置于be后、实义动词前。③ 客观真理用一般现在时：The earth moves around the sun. ④ 主将从现：在时间/条件状语从句中用一般现在时代替一般将来时（If it rains tomorrow, I will stay home.）。\n\n【常见错误】\n1. 主语三单时动词忘加-s/-es→He go to school（×）应改为He goes to school\n2. 频度副词位置错误→He often is late（×）应改为He is often late\n\n【考试方向】\n单选：选择正确动词形式；考查三单和主将从现。'
WHERE parent_id=@l3 AND name='一般现在时 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n一般过去时表示过去某个时间发生的动作或存在的状态，谓语动词用过去式，常与last/ago/yesterday等时间状语连用。\n\n【具体说明】\n① 规则动词过去式：一般+ed(worked)；e结尾+d(lived)；辅音+y结尾变y为i+ed(studied)；重读闭音节双写尾字母+ed(stopped)。② 常见不规则动词：go→went, come→came, see→saw, take→took, have→had, do→did, make→made, get→got, say→said, know→knew。③ 否定和疑问借助did：I didn''t go to school yesterday. / Where did you go? ④ 时间标志词：yesterday/last week/three days ago/in 2020/just now。\n\n【常见错误】\n1. 否定句/疑问句中用了did后动词忘恢复原形→I didn''t went（×）应改为I didn''t go\n2. 不规则动词过去式记错→teached（×）应改为taught\n\n【考试方向】\n单选/完形：根据时间状语选择正确时态；辨析不规则动词过去式。'
WHERE parent_id=@l3 AND name='一般过去时 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n一般将来时表示将来要发生的动作或存在的状态，常用will+V(原形)或be going to+V(原形)表示。\n\n【具体说明】\n① will+V：表示纯粹的将来，临时决定。He will come tomorrow. / I will help you. ② be going to+V：表示计划打算或根据迹象即将发生。We are going to have a picnic this Sunday. / Look at the dark clouds! It is going to rain. ③ there be的将来时：There will be / There is going to be。④ 短暂性动词(come/go/leave/arrive)可用现在进行时表将来：The bus is coming. ⑤ 否定：won''t=will not，isn''t going to。\n\n【常见错误】\n1. will与be going to混淆→临时决定用will，有计划的用be going to\n2. There will have（×）→There will be才是正确表达\n\n【考试方向】\n单选：选择正确的将来时表达；区分will(临时决定)和be going to(计划)。'
WHERE parent_id=@l3 AND name='一般将来时 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n现在进行时表示说话时正在进行的动作或当前一段时间内持续的动作，结构为am/is/are +V-ing。\n\n【具体说明】\n① 结构：am用于I(is用于I时仅在特殊场合)，is用于he/she/it，are用于we/you/they。② V-ing变化规则：一般+ing(working)；e结尾去e+ing(making)；重读闭音节双写尾字母+ing(running,swimming)；ie结尾变ie为y+ing(dying,lying)。③ 时间标志词：now/right now/at the moment/Look!/Listen!/these days。④ 表示渐变：It is getting warmer and warmer. / He is growing older. ⑤ 与always连用表抱怨：He is always complaining!（他总是抱怨！）\n\n【常见错误】\n1. 漏be动词→He working（×）应改为He is working\n2. 静态动词（like,know,want）不能用于进行时→I''m liking it（×）应改为I like it\n\n【考试方向】\n单选：选择正确的be+doing形式；区分一般现在时和现在进行时。'
WHERE parent_id=@l3 AND name='现在进行时 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n过去进行时表示过去某一时刻或某一时段正在进行的动作，结构为was/were +V-ing。\n\n【具体说明】\n① 结构：I/he/she/it用was，we/you/they用were。② 时间标志词：at this time yesterday / at 8 o''clock last night / when引导的从句。③ when与while区别：when后可接短暂或延续动词（He was reading when I came in），while后只能接延续动词（While I was reading, he fell asleep）。④ 用于描述过去某时正在进行的背景动作：It was raining when we left. ⑤ 过去进行时常与一般过去时搭配，表示一个动作正在进行时另一个动作发生。\n\n【常见错误】\n1. was/were用混→We was playing（×）应改为We were playing\n2. while+短暂动词→While he came in（×）应改为When he came in\n\n【考试方向】\n单选/完形：when与while的区别；根据上下文判断时态。'
WHERE parent_id=@l3 AND name='过去进行时 [中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n现在完成时表示过去发生的动作对现在造成的影响或结果，或从过去延续到现在的动作状态，结构为have/has +过去分词(done)。\n\n【具体说明】\n① 结构：第三人称单数用has，其余用have + done。② 用法一（影响现在）：I have lost my key.（现在找不到钥匙了）。③ 用法二（持续到现在）：I have lived here for 10 years.（现在还住这里）。④ 标志词：already(已经)/yet(还·用于否疑句末)/ever(曾经)/never(从未)/just(刚刚)/recently(最近)/so far(到目前为止)。⑤ have/has been to(去过已归)与have/has gone to(去了未归)的区别：He has been to Beijing.(去过北京，已回来) / He has gone to Beijing.(去北京了，还没回来)。\n\n【常见错误】\n1. 已过去的具体时间不能与现在完成时连用→I have seen him yesterday（×）yesterday必须用一般过去时\n2. have been to与have gone to混淆→been to"去过已回"，gone to"去了未回"\n\n【考试方向】\n单选：辨析一般过去时与现在完成时；标志词提示。'
WHERE parent_id=@l3 AND name='现在完成时 [困难]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n在现在完成时中，for后接时间段（一段时间长度），since后接时间点+过去式句子，这是考试的高频辨析点。\n\n【具体说明】\n① for+时间段：for two hours / for three years / for a long time / for ages。② since+时间点：since 2019 / since last Monday / since I was born。③ since+一般过去时从句（主句用现在完成时）：It has been five years since he left. ④ since还可作连词引出原因从句。⑤ 对for+时间段提问用How long：How long have you been here?—For two weeks. ⑥ 短暂性动词（come/leave/die/buy/borrow）不能与for+时间段连用，需转换为延续性表达：buy→have，borrow→keep，leave→be away，die→be dead。\n\n【常见错误】\n1. for与since混用→for two years ago（×）应为since two years ago\n2. 短暂性动词直接接for→He has borrowed the book for a week（×）应为has kept\n\n【考试方向】\n单选：for/since选词填空；短暂动词与for连用的改写。'
WHERE parent_id=@l3 AND name='现在完成时-for/since区别 [困难]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n过去完成时表示在过去某一时间或动作之前已经完成的动作，即"过去的过去"，结构为had+过去分词(done)。\n\n【具体说明】\n① when/as soon as/when/before/after从句中，先发生的动作用过去完成时。When I got to the station, the train had left. ② 时间标志词：by the time / by the end of / before +过去时间点。By the end of last term, we had learned 2000 words. ③ "这是某人第几次做某事"的句型：It was the +序数词+time+that sb had done：It was the first time that I had been to Chengdu. ④ 表示愿望的宾语从句(hope/think/expect)中had done表未实现的期望：I had hoped to see him face to face.（实际上没见到）。\n\n【常见错误】\n1. 两个动作都用了过去完成时→只有先发生的用过去完成时，后发生的用一般过去时\n2. 不表示"过去的过去"时滥用→简单叙述过去事件不需要过去完成时\n\n【考试方向】\n单选：给两个过去动作判断哪个先发生；by the time句型。'
WHERE parent_id=@l3 AND name='过去完成时 [困难]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n被动语态表示主语是动作的承受者，结构为be+过去分词(done)，其中be随人称、数和时态变化。\n\n【具体说明】\n① 各时态被动结构：一般现在时 am/is/are done；一般过去时 was/were done；一般将来时 will be done；现在进行时 am/is/are being done；现在完成时 have/has been done；含情态动词 can/must be done。② 主动变被动：宾语变主语，谓语变be done，主语变by宾语。He wrote the book → The book was written by him. ③ 双宾语动词(give/send/show)通常把指人的间接宾语变主语：He gave me a book → I was given a book. ④ 感官动词(make/see/hear)的被动to要还原：He was seen to enter the room. ⑤ 不及物动词无被动：happen/take place/appear/belong to。\n\n【常见错误】\n1. 不及物动词误用被动→The accident was happened（×）应改为happened\n2. 使役动词被动漏to→He was made clean（×）应改为was made to clean\n\n【考试方向】\n单选：选择正确被动形式；主动改被动转换。'
WHERE parent_id=@l3 AND name='被动语态 [中等]';

-- ############################################
-- 非谓语动词 (3个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='非谓语动词' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n动词不定式(to+动词原形)是非谓语动词的一种，可在句中充当主语、宾语、宾语补足语和目的状语等成分，表示将来的、具体的动作。\n\n【具体说明】\n① 作主语：To learn English well is important. → 常用形式主语It：It is important to learn English well. ② 作宾语：want/decide/hope/wish/refuse/promise/plan to do。He decided to study abroad. ③ 作宾补：ask/tell/want/wish/encourage sb to do。The teacher told us to finish homework. ④ 作目的状语：I got up early to catch the first bus. = in order to/so as to。⑤ 疑问词+to do：what to do / where to go / how to solve。⑥ 使役动词let/make/have和感官动词see/watch/hear后作宾补时省略to（被动语态to还原）。\n\n【常见错误】\n1. want doing→want to do（want后不接动名词）\n2. let sb to do→let sb do（使役动词后不加to）\n\n【考试方向】\n单选：不定式作宾语/宾补/目的状语的选词；固定搭配中to do的考查。'
WHERE parent_id=@l3 AND name='动词不定式 [困难]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n动名词(V-ing)是非谓语动词的一种，具有动词和名词双重性质，在句中主要充当主语和宾语，表示一般的、抽象的动作或习惯。\n\n【具体说明】\n① 作主语：Reading is my hobby. → 形式主语：It is no use crying over spilt milk. ② 作动词宾语（必考）：enjoy/mind/miss/finish/practice/suggest/admit/avoid/consider/deny/keep/imagine+V-ing。I enjoy reading novels. Do you mind opening the window? ③ 作介词宾语：look forward to / be used to / pay attention to / devote to +V-ing（to是介词！）。④ 动名词与不定式区别：remember to do(记得要做)/remember doing(记得做过)；stop to do(停下来去做)/stop doing(停止做)。⑤ 固定句型：have difficulty/trouble/fun (in) doing。\n\n【常见错误】\n1. enjoy to do→enjoy doing（enjoy后必接动名词）\n2. look forward to doing写成look forward to do（这个to是介词）\n\n【考试方向】\n单选：动词后接to do还是doing的辨析；介词to+V-ing的判断。'
WHERE parent_id=@l3 AND name='动名词 [中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n分词包括现在分词(V-ing)和过去分词(V-ed)，可在句中作定语和状语。现在分词表主动/进行，过去分词表被动/完成。\n\n【具体说明】\n① 现在分词作定语：表主动/进行。the sleeping boy（正在睡觉的男孩）= the boy who is sleeping。② 过去分词作定语：表被动/完成。the broken window（被打碎的窗户）= the window that was broken；a developed country（发达国家）vs a developing country（发展中国家）。③ 现在分词作状语：表主动/伴随/时间/原因。Seeing the teacher, the students stopped talking.（主动）。④ 过去分词作状语：表被动/完成。Seen from the top, the city looks beautiful.（被动）。⑤ 分词逻辑主语必须与主句主语一致：Walking in the park, a dog bit me.（×，walking的主语是人不是狗）。\n\n【常见错误】\n1. 现在分词与过去分词混淆→exciting(令人兴奋的)vs excited(感到兴奋的)\n2. 分词逻辑主语不一致→Seeing from the hill, the lake is beautiful（×）\n\n【考试方向】\n单选：选择正确的分词形式作定语或状语；现在分词和过去分词的辨析。'
WHERE parent_id=@l3 AND name='分词作定语和状语 [困难]';

-- ############################################
-- 定语从句 (3个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='定语从句' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n关系代词that/which/who/whom/whose引导定语从句修饰先行词。that指人或物，which指物，who/whom指人，whose表所属。\n\n【具体说明】\n① who指人在从句中作主语：The boy who is reading is my brother. ② whom指人在从句中作宾语（可省略或换who/that）：The man (whom) you met is my uncle. ③ which指物作主语或宾语：The book which is on the desk is mine. ④ that指人或物，不可用于非限制性从句：This is the best film that I have seen. ⑤ 只能用that的情况：先行词有最高级/序数词/the only/the very修饰；先行词是all/anything/nothing等不定代词；先行词既有人又有物。⑥ 只能用which的情况：介词后(in which)；非限制性定语从句。\n\n【常见错误】\n1. that与which混用→非限制性定语从句用that\n2. 关系代词赘余→The man who he came（×）who与he不能同时作主语\n\n【考试方向】\n单选：选择正确关系代词；只能用that/which的特殊情况。'
WHERE parent_id=@l3 AND name='关系代词 that/which/who [中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n关系副词when/where/why引导定语从句，在从句中作状语，分别表示时间、地点和原因，相当于"介词+which"。\n\n【具体说明】\n① when=in/on/at which（表时间）：I still remember the day when(on which) I first came to school. ② where=in/at/to which（表地点）：This is the factory where(in which) my father works. ③ why=for which（表原因·先行词只有reason）：Do you know the reason why(for which) he was late? ④ 关系副词vs关系代词：看从句是否缺主语/宾语。缺主语/宾语用that/which；不缺主语/宾语（只缺状语）用when/where/why。This is the school that/which I visited（visit后缺宾语，用that/which）vs This is the school where I work（work不及物，不缺宾语，用where）。\n\n【常见错误】\n1. where与which不分→visit a place缺宾语用which，work in a place缺状语用where\n2. why只能修饰reason→其他名词不能用why\n\n【考试方向】\n单选：关系副词与关系代词的辨析；where/which的选择是高频考点。'
WHERE parent_id=@l3 AND name='关系副词 when/where/why [中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n非限制性定语从句是对先行词的附加说明（而非限定），主从句之间用逗号隔开，关系词不能用that，which可指代整个主句。\n\n【具体说明】\n① 逗号是关键标志：My mother, who is a teacher, loves reading. ② 不能用that：必须用which指物、who指人。③ which可指代整个主句（独特功能）：He passed the exam, which made his parents very happy.（which指"他通过了考试"整件事）。④ as也可引导非限制性从句：As we all know, the earth is round. = As is known to all, ... ⑤ "介词+which"常见于非限制定语从句：He has three sons, two of whom are doctors. ⑥ 与限制性定语从句对比：The students who worked hard passed the exam（只有努力的学生通过）vs The students, who worked hard, passed the exam（所有学生都通过，且他们努力）。\n\n【常见错误】\n1. 非限制性从句用that→非限不能用that替换which/who\n2. 漏写逗号→非限制性定语从句的逗号是必须的\n\n【考试方向】\n单选/改错：考查逗号后that的纠错；which指代整句的理解。'
WHERE parent_id=@l3 AND name='非限制性定语从句 [困难]';

-- ############################################
-- 名词性从句 (3个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='名词性从句' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n宾语从句是在动词、介词或形容词后作宾语的从句，核心规则是陈述语序（主+谓），that可省略，whether/if表"是否"。\n\n【具体说明】\n① that引导：陈述事实，that可省。I know (that) he is right. ② whether/if引导表"是否"：I don''t know whether/if he will come. → 但介词后只用whether，or not前只用whether，不定式前只用whether：It depends on whether it rains. ③ wh-词引导：I don''t know what he wants. / Can you tell me where he lives? ④ 核心规则：宾语从句必须是陈述语序（主语+谓语），不能倒装。Can you tell me where is the bank?（×）→ where the bank is?（√）⑤ 主句过去时→从句相应退为过去范畴（时态一致）：He said he was tired. ⑥ 客观真理永远不变：The teacher said that the sun rises in the east. ⑦ think/believe/suppose的否定前移：I don''t think he is right.（我认为他不对）。\n\n【常见错误】\n1. 宾语从句语序错误→I don''t know what does he want（×）应为what he wants\n2. if/whether后接or not用法混淆→whether...or not正确，if...or not不推荐\n\n【考试方向】\n单选：选择正确引导词；判断宾语从句语序是否正确。'
WHERE parent_id=@l3 AND name='宾语从句 [中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n主语从句是在句中作主语的从句，常用it作形式主语引导，真正主语放句末，多用that或what引导。\n\n【具体说明】\n① that引导（that不可省）：That he will come is certain. → 常用it形式主语：It is certain that he will come. ② what引导（what在从句中充当成分）：What he said was true.（他说的话是真的）。what=the thing(s) that。③ 常见it作形式主语句型：It is+adj/n+that从句（It is important that...）；It is said/reported/known/believed that...（据说/据报道/众所周知/人们相信...）；It happens that（碰巧...）；It doesn''t matter+wh-从句。④ 主语从句缺成分用what，不缺成分陈述事实用that。⑤ whatever/whoever/whichever等引导：Whatever he does is right. ⑥ 主语从句作主语时谓语动词用单数。\n\n【常见错误】\n1. that与what混淆→That he said（×）缺成分应改为What he said\n2. 主语从句首的that省略→That引导主语从句that不能省略\n\n【考试方向】\n单选：what与that的选择；it作形式主语的句型识别。'
WHERE parent_id=@l3 AND name='主语从句 [困难]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n表语从句位于系动词be/look/seem后说明主语内容；同位语从句跟在名词后说明该名词的具体内容，常用that引导且不可省略。\n\n【具体说明】\n① 表语从句引导词：that(不可省)/whether/as if/wh-词。The truth is that he lied. / The question is whether we can finish on time. / That''s why he was late. ② 同位语从句：抽象名词后说具体内容。常见先行词：news/idea/fact/truth/hope/doubt/problem/report。I heard the news that our team won.（our team won就是news的具体内容）。③ 同位语从句vs定语从句：同位语从句that无意义只起连接作用（从句本身说明先行词内容）；定语从句that代替先行词在从句中作成分。The news that he told me was true.（定从·he told me that the news，that在从句中作宾语）vs The news that he won the game is true.（同位·he won the game=the news）。\n\n【常见错误】\n1. 同位语从句与定语从句混淆→that在定从中作成分，在同位从句中不作成分\n2. 表语从句原因误用because→The reason is because...（×）应为The reason is that...\n\n【考试方向】\n单选：表语从句引导词选择；同位语从句与定语从句的辨析。'
WHERE parent_id=@l3 AND name='表语从句与同位语从句 [困难]';

-- ############################################
-- 状语从句 (2个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='状语从句' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n时间/条件/原因状语从句是最常见的三种状语从句类型，考查连接词的准确选择以及时态搭配（特别是主将从现）。\n\n【具体说明】\n① 时间状语从句：when(当…时·从句可短暂可延续)/while(在…期间·从句必须延续)/before(在…之前)/after(在…之后)/as soon as(一…就)/until(直到…才·not...until)。② 条件状语从句：if(如果)/unless(除非=if...not)/as long as(只要)。主将从现：If it doesn''t rain tomorrow, we will go hiking. / I won''t go unless you call me. ③ 原因状语从句：because(因为·回答why·语气最强·不与so连用)/since(既然·已知原因·放句首)/as(由于·已知原因·语气弱·放句首)/for(因为·并列连词·表推测·放句中)。④ since引导的时间/原因从句区分：Since he left(since时间) / Since he is ill, he can''t come(since原因)。\n\n【常见错误】\n1. because与so连用→Because he was ill, so he didn''t come（×）去掉一个\n2. unless从句用将来时→Unless it will rain（×）主将从现，应为Unless it rains\n\n【考试方向】\n单选：选择正确连接词；if句型主将从现考查；because/so不能连用。'
WHERE parent_id=@l3 AND name='时间/条件/原因状语从句 [中等]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n让步状语从句表示"尽管/即使"之意，常由although/though引导；目的状语从句表示"为了/以便"，常由so that/in order that引导。\n\n【具体说明】\n① 让步状语从句：although/though(虽然·不能与but连用)/even though/even if(即使)/no matter+wh-词=wh-ever(无论)。Although he is young, he knows a lot.（不能用but）② "no matter+wh-词"只能引导让步从句，"wh-ever"还可引导名词性从句。You can ask for help no matter who you are. / Whoever breaks the law will be punished.（名词性从句主语）。③ 目的状语从句：so that/in order that+从句（从句中常含can/may/will等情态动词）。He got up early so that he could catch the bus. ④ so that也可引导结果从句表"因此"（无逗号/无情态动词）：He got up late, so that he missed the bus. ⑤ 目的状语还可用in order to/so as to+动词原形（非从句形式，区别在于前者可放句首后者不可）。\n\n【常见错误】\n1. although与but连用→Although he is poor, but he is happy（×）去掉but\n2. so that目的vs结果混淆→目的从句有情态动词(can/may)，结果从句没有\n\n【考试方向】\n单选：让步状语从句引导词选择；although/but不连用是高频改错点。'
WHERE parent_id=@l3 AND name='让步/目的状语从句 [中等]';

-- ############################################
-- 主谓一致 (1个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='主谓一致' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n就近原则指谓语动词与最近的主语在人称和数上一致（either...or/neither...nor/there be）；意义一致指谓语形式取决于主语的实际含义（集体名词/数量词等）。\n\n【具体说明】\n① 就近原则：either...or / neither...nor / not only...but also / there be句型中，谓语与最近的主语一致。Neither he nor I am wrong. / There is a book and two pens on the desk. ② 意义一致：集体名词（family/team/class/group/audience）作主语时，强调整体用单数，强调成员用复数。My family is a large one. / My family are watching TV. ③ 表示时间/金钱/距离/重量作主语（看作整体），谓语用单数。Ten years is a long time. / Fifty dollars is enough. ④ 语法一致基础规则：many a + 单数名词 + 单数谓语；a number of + 复数名词 + 复数谓语；the number of + 复数名词 + 单数谓语；each/every/either/neither of... + 单数谓语。\n\n【常见错误】\n1. there be句型只按第一个名词判断→There is a desk and two chairs正确\n2. the number of与a number of混淆→the number of用单数，a number of用复数\n\n【考试方向】\n单选：选择谓语正确形式；就近原则和意义一致的综合考查。'
WHERE parent_id=@l3 AND name='就近原则与意义一致 [基础]';

-- ############################################
-- 情态动词 (2个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='情态动词' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n情态动词can/could/must/may/might表示能力、许可、必须等含义，不能独立作谓语，后接动词原形。\n\n【具体说明】\n① can/could：表能力（I can swim. / I could swim at the age of 5.=过去有能力）；表请求允许（Could/Can I use your pen? —could更客气）；表可能性（Anyone can make mistakes.）。② must：表必须（主观·You must finish it today.）；否定mustn''t表禁止（You mustn''t smoke here.）。have to表必须（客观·不得不）。I have to go because my mother is ill. ③ may/might：表允许（May I come in?）；表可能性（might比may可能性更小）。He may/might come tomorrow. ④ should：表应该（You should study harder.）；ought to=should。⑤ need：need do(情态动词·用于否定和疑问·Need I go?) vs need to do(实义动词)。⑥ must提问否定回答用needn''t(不必)，不用mustn''t(禁止)。—Must I go now? —No, you needn''t. ⑦ can''t表"不可能"的否定推测。\n\n【常见错误】\n1. must提问否定回答用mustn''t→Must I go? —No, you needn''t/don''t have to\n2. could/would礼貌请求与过去能力混淆→Could you help me?（礼貌请求，非过去）\n\n【考试方向】\n单选：情态动词基本含义辨析；must/mustn''t/needn''t的使用。'
WHERE parent_id=@l3 AND name='情态动词基本用法 can/must/may [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n情态动词表示推测时，must表示极肯定的推测（一定/肯定），can''t/couldn''t表示否定推测（不可能），may/might表示可能性的推测。\n\n【具体说明】\n① 对现在事实推测（情态动词+do/be）：must be/do(肯定·He must be at home now.)；can''t be/do(不可能·He can''t be at home.灯都没亮)；may be/do(可能)。② 对过去事实推测"情态动词+have done"（必考）：must have done(肯定已做·The ground is wet. It must have rained last night.)；can''t/couldn''t have done(不可能已做·He can''t have seen me yesterday because I was out all day.)；may/might have done(过去可能做了)；should/ought to have done(本该做而未做·表遗憾/责备)；needn''t have done(本不必做却做了)。③ could/might have done除表过去可能性，还可表本可以做而未做（虚拟）。④ couldn''t be better/worse等最高级表达——否定+比较级=最高级：I couldn''t agree more.（我非常同意）。\n\n【常见错误】\n1. must have done与should have done混淆→must是推测肯定，should是责备未做\n2. 对现在推测不会用情态动词→用maybe(副词)代替may be，是语法错误\n\n【考试方向】\n单选：情态动词+have done的辨析（must/can''t/should/needn''t have done是考频最高点）。'
WHERE parent_id=@l3 AND name='情态动词推测用法 [中等]';

-- ############################################
-- 虚拟语气 (1个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='虚拟语气' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n虚拟语气在条件句中表示与事实相反的假设，与现在相反用If...were/did...,...would do；与过去相反用If...had done...,...would have done。\n\n【具体说明】\n① 与现在事实相反：If+主语+were/did..., 主语+would/should/could/might+do。If I were you, I would accept the offer.（我是你，我会接受·实际上我不是你）If he had time now, he would come.（现在没时间）② 与过去事实相反：If+主语+had done..., 主语+would/should/could/might+have done。If I had got up earlier, I wouldn''t have been late.（实际上起晚了，迟到了）③ 省略if的倒装：将were/had/should移到主语前。Were I you(=If I were you), I would go. / Had I known(=If I had known), I would have told you. ④ 错综时间（混合虚拟）：If he had taken the medicine yesterday(过去), he would be well now(现在). ⑤ 含蓄条件：without/but for/otherwise/or都隐含假设。Without your help, I couldn''t have succeeded.\n\n【常见错误】\n1. If I was you→If I were you（虚拟语气be一律用were）\n2. 与过去相反忘了用had done→If I knew earlier（×）应改为If I had known earlier\n\n【考试方向】\n单选：选择正确的虚拟形式；省略if的倒装识别。'
WHERE parent_id=@l3 AND name='虚拟语气在条件句中的用法 [困难]';

-- ############################################
-- 情景交际 (4个L4)
-- ############################################

SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='情景交际' AND level=3 LIMIT 1);

UPDATE knowledge_nodes SET content =
'【一句话定义】\n邀请与请求是日常交际中的高频场景，正确使用Would you like to...?/Could you please...?等句型体现英语交际礼貌原则。\n\n【具体说明】\n① 邀请句型：Would you like to+do...?（你愿意...吗？）接受：Yes, I''d love to. / Sounds great! 拒绝：I''d love to, but...(需给理由)。Would you like some tea?（用some不用any，期待肯定回答）。② 请求句型：Could/Can you please+do...? / Would you mind+doing...? 回应mind：No, not at all./Of course not.(不介意·同意) —I''m sorry, but...(介意·拒绝·需说明理由)。③ 建议回应：Shall we...? / Let''s..., shall we? 回答：That''s a good idea./Why not?（表赞同）。④ I would like to do= I''d like to do比I want to do更礼貌。⑤ 情态词比较：Could比Can更客气，Would比Will更礼貌，在交际英语中多用过去式表达委婉语气。\n\n【常见错误】\n1. Would you like后接doing→应为to do（Do you feel like doing才接doing）\n2. Would you mind...? 回答混淆→"Yes"是介意（拒绝），"No/Not at all"是不介意（同意）\n\n【考试方向】\n单选：补全对话，选择得体的邀请/请求表达；Would you mind的回答是高频陷阱。'
WHERE parent_id=@l3 AND name='邀请与请求 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n感谢与道歉是日常交际基本礼仪，考查Thank you for.../I''m sorry...等核心表达及其得体的应答方式。\n\n【具体说明】\n① 感谢用语：Thank you(for sth/doing). / Thanks a lot. / I really appreciate it. / That''s very kind of you. ② 感谢应答：You''re welcome./Not at all./Don''t mention it./My pleasure./It''s my pleasure./That''s all right./No problem. ③ 道歉用语：I''m sorry. / Excuse me.(打扰/失陪/借过) / I beg your pardon.(请原谅/没听清)。Sorry for+doing或excuse me for+doing。④ 道歉应答：That''s all right./Never mind./It doesn''t matter./Forget it./No problem. ⑤ Excuse me vs I''m sorry：Excuse me是事前礼貌（打扰对方前）；I''m sorry是事后道歉（做错事后）。⑥ "Thank you all the same."用于对方没帮上忙但仍要感谢的场合。\n\n【常见错误】\n1. Thank you应答用No→英语中No不能做感谢应答（应为You''re welcome等）\n2. Excuse me与I''m sorry混用→打扰前用Excuse me，出错后用I''m sorry\n\n【考试方向】\n单选：补全对话，选择正确的感谢/道歉应答；判断Excuse me与sorry的使用场景。'
WHERE parent_id=@l3 AND name='感谢与道歉 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n问路与指路考查用英语询问方位和给予方向指示的能力，核心句型为Excuse me, how can I get to...?和Go straight.../Turn left/right...等。\n\n【具体说明】\n① 问路句型：Excuse me, where is the nearest...? / Excuse me, how can I get to...? / Excuse me, can you tell me the way to...? / Excuse me, which bus can I take to...? ② 指路句型：Go straight(down this road). / Turn left/right at the second crossing. / It''s on your left/right. / It''s next to/opposite/behind/in front of... / Take the No.5 bus and get off at... / You can''t miss it. ③ 表示''不知道''：Sorry, I''m new here myself. / Sorry, I don''t know. ④ 方位介词短语：at the corner of(在…拐角)/between...and...(在…之间)/across from(在…对面)/next to(紧挨着)。⑤ 距离表达：It''s about 5 minutes'' walk. / It''s far from here, you''d better take a bus.\n\n【常见错误】\n1. "在左边"误用in the left→应为on the left（on表表面/旁边）\n2. turn left写成turn the left→没有the（方向前不加冠词）\n\n【考试方向】\n单选/补全对话：问路与指路情景补全；方位介词的选择。'
WHERE parent_id=@l3 AND name='问路与指路 [基础]';

UPDATE knowledge_nodes SET content =
'【一句话定义】\n建议与劝告考查用英语提出建议和说服对方的交际能力，核心句型为You''d better.../Why not...?/How about...?/You should...等。\n\n【具体说明】\n① 建议句型：You''d better(not)+do...（你最好/最好别...·语气较强）。Why don''t you+do...? / Why not+do...?（为什么不...呢？）。How about/What about+doing...?（...怎么样？）。Shall we+do...? / Let''s+do（我们...吧）。You should+do（你应该...）。② 接受建议：Good idea!/That sounds great!/Sounds good!/Why not?（表示赞同）。③ 拒绝建议：I''d love to, but... / Sorry, I can''t. / I''m afraid not. ④ 劝告句型：Don''t+do...（别...）；You should/ought to do...；It''s better to do...；I advise you to do...。⑤ You''d better=You had better，后接动词原形，否定是had better not do。⑥ prefer to do...rather than do...（宁愿...而不愿...）。\n\n【常见错误】\n1. Why not+doing→Why not go?（√）Why not going?（×）Why not后接动词原形\n2. How about+to do→How about going?（√）How about to go?（×）\n\n【考试方向】\n单选/补全对话：选择最恰当的建议表达；Why not/How about/What about后接形式的考查。'
WHERE parent_id=@l3 AND name='建议与劝告 [基础]';

SELECT 'v140: Grammar L4 done' AS result;
