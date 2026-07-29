-- ============================================================================
-- v208: 英语[职高] 知识文章深度扩容 — 追加"职高生常犯错误" + "练一练"
-- 目标：17篇 priority 文章从 ~300 字扩至 600-800 字
-- 幂等：WHERE content_md NOT LIKE '%⚠️ 职高生%' 防止重复追加
-- ============================================================================

SET NAMES utf8mb4;

-- ======================================================================
-- 第一优先：语法专项 > 时态语态 (9篇)
-- ======================================================================

-- ──────────────────────────────────────────────────────────────────────
-- 1. 一般现在时 [基础]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：三单谓语忘记加 -s**\n',
    '× He go to school every day.\n',
    '√ He goes to school every day.\n',
    '**要点**：主语是 he/she/it 或单数名词时，一般现在时的谓语动词必须加 -s/-es。很多同学只记住动词原形，忽略了人称变化。\n\n',
    '**错误2：否定句/疑问句忘记用助动词**\n',
    '× He not like apples.\n',
    '√ He doesn\'t like apples.\n',
    '× Like you music?\n',
    '√ Do you like music?\n',
    '**要点**：一般现在时的否定和疑问必须借助助动词 do/does。does 用于三单主语，用了 does 后动词恢复原形。\n\n',
    '**错误3：频度副词位置放错**\n',
    '× I get up always at 7.\n',
    '√ I always get up at 7.\n',
    '× He is often late.\n',
    '√ He is often late.（注意：be 动词后）\n',
    '**要点**：频度副词（always/often/sometimes/never）放在行为动词之前、be 动词之后。\n\n',
    '### ⚡ 练一练\n\n',
    '1. She ___ (go) to school by bike every day.\n',
    '   A. go  B. goes  C. going  D. went\n',
    '   答案：B。解析：主语 she 是三单，一般现在时谓语动词加 -s。\n\n',
    '2. ___ your father like watching TV?\n',
    '   A. Do  B. Does  C. Is  D. Are\n',
    '   答案：B。解析：主语 your father 是三单，疑问句用 Does，动词 like 恢复原形。\n\n',
    '3. We ___ (not) have classes on Sundays.\n',
    '   A. don\'t  B. doesn\'t  C. aren\'t  D. isn\'t\n',
    '   答案：A。解析：主语 We 不是三单，否定用 don\'t + 动词原形。\n\n',
    '4. He ___ (always/be) late for school.\n',
    '   A. always is  B. is always  C. always be  D. be always\n',
    '   答案：B。解析：频度副词 always 放在 be 动词 is 之后。\n\n',
    '5. The earth ___ (go) around the sun.\n',
    '   A. go  B. goes  C. going  D. is going\n',
    '   答案：B。解析：客观真理用一般现在时，earth 是三单，谓语加 -s。'
)
WHERE subject_id=24 AND title='一般现在时' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 2. 一般过去时 [基础]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：规则动词过去式变错**\n',
    '× He studyed English yesterday.\n',
    '√ He studied English yesterday.\n',
    '**要点**：辅音+y 结尾的动词变过去式要改 y 为 i 再加 -ed（study→studied），不是直接加 -ed。play 例外（play→played，因为 y 前是元音）。\n\n',
    '**错误2：不规则动词过去式用错**\n',
    '× I swimmed in the river last summer.\n',
    '√ I swam in the river last summer.\n',
    '× She taked the bus to school.\n',
    '√ She took the bus to school.\n',
    '**要点**：不规则动词必须逐个记忆。按组记更有效：sing→sang, swim→swam, drink→drank；take→took, make→made, get→got。\n\n',
    '**错误3：否定句/疑问句结构混乱**\n',
    '× He didn\'t went to school yesterday.\n',
    '√ He didn\'t go to school yesterday.\n',
    '× Did you went to the park?\n',
    '√ Did you go to the park?\n',
    '**要点**：一般过去时的否定/疑问用助动词 did，用了 did 后动词恢复原形，不能再用过去式。\n\n',
    '### ⚡ 练一练\n\n',
    '1. He ___ (buy) a new bike last week.\n',
    '   A. buyed  B. bought  C. buys  D. buying\n',
    '   答案：B。解析：buy 的不规则过去式是 bought。\n\n',
    '2. They ___ (not/watch) TV yesterday evening.\n',
    '   A. didn\'t watched  B. didn\'t watch  C. not watched  D. doesn\'t watch\n',
    '   答案：B。解析：否定句用 didn\'t + 动词原形 watch。\n\n',
    '3. ___ you ___ (see) the film last night?\n',
    '   A. Did...see  B. Did...saw  C. Do...see  D. Were...see\n',
    '   答案：A。解析：疑问句用 Did + 主语 + 动词原形 see。\n\n',
    '4. She ___ (be) very happy when she heard the news.\n',
    '   A. is  B. was  C. were  D. be\n',
    '   答案：B。解析：主语 she，过去时用 was。\n\n',
    '5. He ___ (stop) his car in front of the school gate.\n',
    '   A. stoped  B. stopped  C. stops  D. stopping\n',
    '   答案：B。解析：重读闭音节 stop 双写 p 再加 -ed。'
)
WHERE subject_id=24 AND title='一般过去时' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 3. 一般将来时 [基础]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：will 后面忘记跟动词原形**\n',
    '× I will goes to Beijing tomorrow.\n',
    '√ I will go to Beijing tomorrow.\n',
    '× She will came to see me.\n',
    '√ She will come to see me.\n',
    '**要点**：will 是情态动词，后面必须接动词原形，不管主语是什么人称。\n\n',
    '**错误2：be going to 中的 be 忘记变化**\n',
    '× He going to buy a new car.\n',
    '√ He is going to buy a new car.\n',
    '× They going to play football.\n',
    '√ They are going to play football.\n',
    '**要点**：be going to 中 be 要随主语变化（am/is/are），不能漏掉。\n\n',
    '**错误3：will 和 be going to 混用不分场景**\n',
    '×（临时决定）I\'m going to help you.\n',
    '√ I\'ll help you.（听到对方需要帮助时临时决定用 will）\n',
    '×（计划好的旅行）I\'ll visit my grandparents next Sunday.\n',
    '√ I\'m going to visit my grandparents next Sunday.（提前计划用 be going to）\n',
    '**要点**：will 表临时决定/预测，be going to 表提前计划/已有迹象。\n\n',
    '### ⚡ 练一练\n\n',
    '1. There ___ a basketball match next week.\n',
    '   A. will have  B. will be  C. is going to have  D. are\n',
    '   答案：B。解析：There be 句型将来时为 There will be，不能用 have。\n\n',
    '2. —I\'m going to the supermarket.—Wait, I ___ with you.\n',
    '   A. will go  B. am going  C. go  D. went\n',
    '   答案：A。解析：临时决定用 will。\n\n',
    '3. Look at the dark clouds! It ___ rain.\n',
    '   A. will  B. is going to  C. shall  D. was going to\n',
    '   答案：B。解析：有迹象表明即将下雨，用 be going to。\n\n',
    '4. She ___ (not/will) go to the party tonight.\n',
    '   A. won\'t  B. willn\'t  C. not will  D. isn\'t\n',
    '   答案：A。解析：will not 缩写为 won\'t。\n\n',
    '5. If it rains tomorrow, we ___ (stay) at home.\n',
    '   A. stay  B. will stay  C. stayed  D. are staying\n',
    '   答案：B。解析：主将从现——if 从句用一般现在时，主句用一般将来时。'
)
WHERE subject_id=24 AND title='一般将来时' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 4. 现在进行时 [基础]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：be 动词遗漏或与主语不一致**\n',
    '× He reading a book now.\n',
    '√ He is reading a book now.\n',
    '× They playing basketball.\n',
    '√ They are playing basketball.\n',
    '**要点**：现在进行时的结构是 be + V-ing，be 动词不能省略，且要与主语人称一致。\n\n',
    '**错误2：动词-ing 形式拼写错误**\n',
    '× She is writting a letter.\n',
    '√ She is writing a letter.（以不发音 e 结尾去 e 加 -ing）\n',
    '× He is runing.\n',
    '√ He is running.（重读闭音节双写尾字母）\n',
    '× The baby is lieing on the bed.\n',
    '√ The baby is lying on the bed.（ie 变 y 加 -ing）\n',
    '**要点**：动词加 -ing 有三种变化规则，考试中常考 write/writing、run/running、lie/lying。\n\n',
    '**错误3：状态动词误用进行时**\n',
    '× I am knowing the answer.\n',
    '√ I know the answer.\n',
    '× I am wanting a new phone.\n',
    '√ I want a new phone.\n',
    '**要点**：know/like/want/need/belong 等状态动词一般不用于进行时，它们表示状态而非动作。\n\n',
    '### ⚡ 练一练\n\n',
    '1. Listen! Someone ___ (sing) in the next room.\n',
    '   A. sing  B. sings  C. is singing  D. sang\n',
    '   答案：C。解析：Listen! 表明动作正在进行，用现在进行时。\n\n',
    '2. The children ___ (swim) in the river now.\n',
    '   A. is swimming  B. are swimming  C. swim  D. swam\n',
    '   答案：B。解析：主语 children 复数，are + swimming（重读闭音节双写）。\n\n',
    '3. —Where is Tom?—He ___ a book in the library.\n',
    '   A. reads  B. is reading  C. read  D. will read\n',
    '   答案：B。解析：问"他在哪"意味着此刻的动作，用现在进行时。\n\n',
    '4. The soup ___ good. I like it.\n',
    '   A. tastes  B. is tasting  C. taste  D. is taste\n',
    '   答案：A。解析：taste 是感官动词，用一般现在时表主动，不用进行时。\n\n',
    '5. He usually ___ to school, but now he ___ a bus.\n',
    '   A. walks; is taking  B. is walking; takes  C. walks; takes  D. is walking; is taking\n',
    '   答案：A。解析：usually 一般现在时，now 现在进行时。'
)
WHERE subject_id=24 AND title='现在进行时' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 5. 过去进行时 [中等]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：was/were 与主语不一致**\n',
    '× They was watching TV at 8 last night.\n',
    '√ They were watching TV at 8 last night.\n',
    '× I were doing my homework when he came.\n',
    '√ I was doing my homework when he came.\n',
    '**要点**：过去进行时的 be 用 was（I/he/she/it）或 were（we/you/they），不可混淆。\n\n',
    '**错误2：when 和 while 的时态搭配错**\n',
    '× I was reading when he was coming.\n',
    '√ I was reading when he came.（when + 短动作用一般过去时）\n',
    '× He was singing while I was dancing.（while 后用进行时是正确用法）\n',
    '√ He was singing while I was dancing. ✅\n',
    '**要点**：when 后常接一般过去时（短动作打断），while 后常用过去进行时（持续动作）。\n\n',
    '**错误3：忘记用过去进行时而误用一般过去时**\n',
    '× I watched TV when he knocked at the door.（表"正在看电视时"不合适）\n',
    '√ I was watching TV when he knocked at the door.\n',
    '**要点**：描述背景动作（正在发生的事）用过去进行时，插入的短动作用一般过去时。\n\n',
    '### ⚡ 练一练\n\n',
    '1. I ___ (cook) dinner when the phone rang.\n',
    '   A. cooked  B. was cooking  C. am cooking  D. cook\n',
    '   答案：B。解析：电话响时"正在做饭"，长动作用过去进行时。\n\n',
    '2. While the teacher ___ (explain) the text, the students listened carefully.\n',
    '   A. explained  B. was explaining  C. is explaining  D. explains\n',
    '   答案：B。解析：while 后常用过去进行时表持续动作。\n\n',
    '3. They ___ (play) football at 4 yesterday afternoon.\n',
    '   A. played  B. were playing  C. are playing  D. play\n',
    '   答案：B。解析：过去某一具体时刻正在进行的动作，用过去进行时。\n\n',
    '4. When I ___ (come) in, she was reading.\n',
    '   A. was coming  B. came  C. come  D. am coming\n',
    '   答案：B。解析：when 后用一般过去时（短动作）。\n\n',
    '5. The sun ___ (shine) and the birds were singing.\n',
    '   A. shines  B. shone  C. was shining  D. is shining\n',
    '   答案：C。解析：描写过去场景的背景，sun 单数用 was shining。'
)
WHERE subject_id=24 AND title='过去进行时' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 6. 现在完成时 [困难]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：现在完成时与一般过去时混淆**\n',
    '× I have bought a book yesterday.\n',
    '√ I bought a book yesterday.（有明确过去时间用一般过去时）\n',
    '√ I have bought a book this week.（没有明确过去时间用现在完成时）\n',
    '**要点**：看到 yesterday / last year / in 2020 / ago 等明确过去时间，必须用一般过去时。现在完成时不与具体过去时间连用。\n\n',
    '**错误2：have been to 与 have gone to 混淆**\n',
    '× —Where is Tom? —He has been to Shanghai.\n',
    '√ —Where is Tom? —He has gone to Shanghai.（去了未回）\n',
    '× I have gone to Beijing twice.\n',
    '√ I have been to Beijing twice.（去过已回，表经历次数）\n',
    '**要点**：have been to = 去了已回，have gone to = 去了未回（人不在这里）。\n\n',
    '**错误3：过去分词形式记错**\n',
    '× I have went to the library.\n',
    '√ I have gone to the library.\n',
    '× She has wrote a letter.\n',
    '√ She has written a letter.\n',
    '**要点**：不规则动词的过去分词需额外记忆。常见三对三：go→went→gone, write→wrote→written, take→took→taken。\n\n',
    '### ⚡ 练一练\n\n',
    '1. He ___ (finish) his homework already.\n',
    '   A. finish  B. finished  C. has finished  D. is finishing\n',
    '   答案：C。解析：already 是现在完成时的标志词，用 has finished。\n\n',
    '2. —___ you ever ___ (eat) Beijing Roast Duck?—Yes, I have.\n',
    '   A. Did...eat  B. Have...eaten  C. Do...eat  D. Will...eat\n',
    '   答案：B。解析：ever 常用于现在完成时的疑问句，表"曾经"。\n\n',
    '3. My father ___ (go) to Shanghai. He\'ll be back in two days.\n',
    '   A. has been to  B. has gone to  C. went  D. goes\n',
    '   答案：B。解析："两天后才回来"说明去了未回，用 has gone to。\n\n',
    '4. I ___ (never/see) such a beautiful sunset.\n',
    '   A. never saw  B. have never seen  C. never see  D. will never see\n',
    '   答案：B。解析：never 与现在完成时连用表"从未有过的经历"。\n\n',
    '5. She ___ (read) this book last year.\n',
    '   A. has read  B. reads  C. read  D. is reading\n',
    '   答案：C。解析：last year 是明确过去时间，用一般过去时 read。'
)
WHERE subject_id=24 AND title='现在完成时' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 7. 现在完成时-for/since区别 [困难]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：for 和 since 互换混用**\n',
    '× I have studied English since three years.\n',
    '√ I have studied English for three years.（时间段用 for）\n',
    '× I have studied English for 2020.\n',
    '√ I have studied English since 2020.（时间点用 since）\n',
    '**要点**：for + 时间段（two hours / three years / a long time），since + 时间点（2020 / last week / 8 o\'clock）。\n\n',
    '**错误2：短暂动词与 for/since 连用**\n',
    '× He has bought the car for two years.\n',
    '√ He has had the car for two years.（buy 是短暂动词，改为延续性 have）\n',
    '× She has left for an hour.\n',
    '√ She has been away for an hour.（leave→be away）\n',
    '**要点**：短暂动词（buy/borrow/leave/join/die）不能与 for/since 连用，必须换成对应的延续性表达。\n\n',
    '**错误3：How long 提问的回答不匹配**\n',
    '× —How long have you been here? —Since two hours.\n',
    '√ —How long have you been here? —For two hours. / Since 2 o\'clock.\n',
    '**要点**：How long 提问"持续了多久"，回答要用 for + 时间段或 since + 时间点。\n\n',
    '### ⚡ 练一练\n\n',
    '1. He has worked here ___ 2018.\n',
    '   A. for  B. since  C. in  D. from\n',
    '   答案：B。解析：2018 是时间点，用 since。\n\n',
    '2. I have had this bike ___ three years.\n',
    '   A. since  B. for  C. in  D. from\n',
    '   答案：B。解析：three years 是时间段，用 for。\n\n',
    '3. She ___ (join) the club for two months.（改错）\n',
    '   A. has joined  B. has been in  C. joined  D. joins\n',
    '   答案：B。解析：join 是短暂动词，改为 be in。\n\n',
    '4. His grandfather ___ (die) for five years.\n',
    '   A. has died  B. has been dead  C. died  D. was dead\n',
    '   答案：B。解析：die 是短暂动词，改为 be dead。\n\n',
    '5. —How long has your mother worked in this hospital? —___ 2005.\n',
    '   A. For  B. Since  C. In  D. After\n',
    '   答案：B。解析：回答时间点 2005，用 since。'
)
WHERE subject_id=24 AND title='现在完成时-for/since区别' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 8. 过去完成时 [困难]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：无"过去的过去"对比时滥用过去完成时**\n',
    '× I had watched TV yesterday evening.（没有第二个过去时间对比）\n',
    '√ I watched TV yesterday evening.（简单叙述过去用一般过去时即可）\n',
    '√ When I got home, my mother had already cooked dinner.（"做饭"在"到家"之前）\n',
    '**要点**：过去完成时必须有一个更晚的过去动作/时间作对比。如果没有对比关系，用一般过去时。\n\n',
    '**错误2：过去完成时的结构写错**\n',
    '× She had went to bed before I came back.\n',
    '√ She had gone to bed before I came back.（过去分词 gone，不是 went）\n',
    '**要点**：过去完成时 = had + 过去分词。过去分词（gone/written/seen）与过去式不同，不可混淆。\n\n',
    '**错误3：by 短语时态用错**\n',
    '× By the end of last year, we have learned 2000 words.\n',
    '√ By the end of last year, we had learned 2000 words.\n',
    '**要点**：by + 过去时间点（by 5 o\'clock yesterday / by last month / by 2020），主句用过去完成时。\n\n',
    '### ⚡ 练一练\n\n',
    '1. When I arrived at the station, the train ___ (already/leave).\n',
    '   A. already left  B. had already left  C. has already left  D. leaves\n',
    '   答案：B。解析："离开"在"到达"之前，过去的过去用过去完成时。\n\n',
    '2. By the time he was 10, he ___ (read) many books.\n',
    '   A. read  B. has read  C. had read  D. reads\n',
    '   答案：C。解析："10岁时"是过去时间，"已经读了很多书"发生在之前，用过去完成时。\n\n',
    '3. He said he ___ (never / be) to Shanghai before.\n',
    '   A. never went  B. has never been  C. had never been  D. never goes\n',
    '   答案：C。解析：主句 said 是过去时，从句动作发生在 said 之前，用过去完成时。\n\n',
    '4. She ___ (finish) her homework before her mother came home.\n',
    '   A. finished  B. has finished  C. had finished  D. was finishing\n',
    '   答案：C。解析："完成作业"在"妈妈回家"之前。\n\n',
    '5. No sooner ___ I ___ (sit) down than the phone rang.\n',
    '   A. had...sat  B. did...sit  C. have...sat  D. was...sitting\n',
    '   答案：A。解析：no sooner...than 结构中主句用过去完成时且倒装。'
)
WHERE subject_id=24 AND title='过去完成时' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 9. 被动语态 [中等]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：被动语态中 be 动词遗漏**\n',
    '× The book written by Lu Xun.\n',
    '√ The book was written by Lu Xun.（被动语态必须有 be 动词）\n',
    '× English speak all over the world.\n',
    '√ English is spoken all over the world.\n',
    '**要点**：被动语态 = be + 过去分词。很多同学只写了过去分词，漏了 be 动词。be 动词要随主语和时态变化。\n\n',
    '**错误2：不及物动词误用被动语态**\n',
    '× The accident was happened yesterday.\n',
    '√ The accident happened yesterday.（happen 没有被动语态）\n',
    '× Great changes have been taken place in my hometown.\n',
    '√ Great changes have taken place in my hometown.（take place 没有被动）\n',
    '**要点**：不及物动词（happen/appear/rise/take place/belong）没有被动语态。\n\n',
    '**错误3：主动表被动的结构用错**\n',
    '× The car needs to repair.（主动应该用 V-ing 或 to be done）\n',
    '√ The car needs repairing. / The car needs to be repaired.（两种都对）\n',
    '× The food is tasted good.（taste 感官动词主动表被动）\n',
    '√ The food tastes good.（感官动词用主动表被动，不加 be）\n',
    '**要点**：need/require + V-ing = need/require + to be done；感官动词（taste/smell/look/feel）主动表被动。\n\n',
    '### ⚡ 练一练\n\n',
    '1. This kind of machine ___ (make) in China.\n',
    '   A. is made  B. made  C. makes  D. was made\n',
    '   答案：A。解析：机器"被制造"，一般现在时被动语态 is made。\n\n',
    '2. The building ___ (build) in 2005.\n',
    '   A. built  B. was built  C. is built  D. builds\n',
    '   答案：B。解析：2005 是过去时间，建筑被建，用 was built。\n\n',
    '3. Your homework must ___ (hand) in tomorrow.\n',
    '   A. hand  B. be hand  C. be handed  D. handed\n',
    '   答案：C。解析：情态动词被动：must be + 过去分词 handed。\n\n',
    '4. The girl ___ (see) to cross the street just now.\n',
    '   A. saw  B. was seen  C. is seen  D. sees\n',
    '   答案：B。解析："女孩被看到"，过去时间用 was seen。\n\n',
    '5. The flowers need ___.\n',
    '   A. water  B. to water  C. watering  D. be watered\n',
    '   答案：C。解析：need + V-ing 主动表被动 = need to be watered。'
)
WHERE subject_id=24 AND title='被动语态' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ======================================================================
-- 第二优先：语法专项 > 从句系列 (8篇)
-- ======================================================================

-- ──────────────────────────────────────────────────────────────────────
-- 10. 关系代词 that/which/who [中等]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：关系代词 who 和 whom 混淆**\n',
    '× The man who you met yesterday is my uncle.\n',
    '√ The man whom you met yesterday is my uncle.（whom 作宾语）\n',
    '√ The man who met you yesterday is my uncle.（who 作主语）\n',
    '**要点**：who 在从句中作主语，whom 作宾语。口语中 who 可代替 whom，但考试中作宾语最好用 whom 或省略。\n\n',
    '**错误2：that 和 which 分不清什么时候只能用 that**\n',
    '× This is the most beautiful city which I have ever visited.\n',
    '√ This is the most beautiful city that I have ever visited.（最高级后用 that）\n',
    '× Everything which he said is true.\n',
    '√ Everything that he said is true.（不定代词后用 that）\n',
    '**要点**：先行词含最高级/序数词/不定代词（all/anything/nothing/little）时，关系代词只能用 that 不用 which。\n\n',
    '**错误3：关系代词作宾语时多加了代词**\n',
    '× The book that I bought it yesterday is very interesting.\n',
    '√ The book that I bought yesterday is very interesting.（it 多余）\n',
    '**要点**：关系代词 that/which/whom 已在从句中替代了先行词，从句中不能再出现代词指代先行词。\n\n',
    '### ⚡ 练一练\n\n',
    '1. The girl ___ is standing there is my sister.\n',
    '   A. who  B. whom  C. which  D. whose\n',
    '   答案：A。解析：先行词 the girl（人），在从句中作主语，用 who。\n\n',
    '2. This is the best film ___ I have ever seen.\n',
    '   A. which  B. that  C. who  D. whom\n',
    '   答案：B。解析：最高级 the best 后用 that。\n\n',
    '3. I like the music ___ I can dance to.\n',
    '   A. who  B. whom  C. that  D. whose\n',
    '   答案：C。解析：先行词 the music（物），可用 that 或 which。\n\n',
    '4. The man ___ car was stolen reported to the police.\n',
    '   A. who  B. whom  C. which  D. whose\n',
    '   答案：D。解析：car 与 the man 有所属关系，用 whose。\n\n',
    '5. The house ___ windows are broken is empty.\n',
    '   A. which  B. that  C. whose  D. of which\n',
    '   答案：C。解析：windows 与 the house 所属关系，用 whose。'
)
WHERE subject_id=24 AND title='关系代词 that/which/who' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 11. 关系副词 when/where/why [中等]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：关系副词和关系代词分不清**\n',
    '× I still remember the day which I first met him.\n',
    '√ I still remember the day when I first met him.（先行词 the day 在从句中作时间状语用 when）\n',
    '× This is the school which I studied.\n',
    '√ This is the school where I studied.（先行词 the school 在从句中作地点状语用 where）\n',
    '**要点**：选关系代词还是关系副词，看先行词在从句中作什么成分。作主语/宾语用代词（that/which），作状语用副词（when/where/why）。\n\n',
    '**错误2：把关系副词当作疑问词来用**\n',
    '× I know the place where does he live.\n',
    '√ I know the place where he lives.（定语从句不用疑问句语序）\n',
    '**要点**：定语从句永远用陈述句语序，不能像特殊疑问句那样倒装。\n\n',
    '**错误3：the reason why 后多加 because**\n',
    '× Tell me the reason why you are late because.\n',
    '√ Tell me the reason why you are late.（why 已经表原因，后面不再加 because）\n',
    '**要点**：the reason why...中 why 已经表达了原因含义，后面不能再加 because。\n\n',
    '### ⚡ 练一练\n\n',
    '1. I\'ll never forget the day ___ I joined the army.\n',
    '   A. which  B. when  C. where  D. why\n',
    '   答案：B。解析：先行词 the day，从句中作时间状语，用 when。\n\n',
    '2. This is the park ___ we often play football.\n',
    '   A. which  B. that  C. where  D. when\n',
    '   答案：C。解析：先行词 the park，从句中作地点状语，用 where。\n\n',
    '3. The reason ___ he was late is that he got up late.\n',
    '   A. which  B. that  C. why  D. when\n',
    '   答案：C。解析：先行词 the reason，从句中作原因状语，用 why。\n\n',
    '4. I know a place ___ we can have a picnic.\n',
    '   A. which  B. that  C. where  D. when\n',
    '   答案：C。解析：a place 在从句中作地点状语（we can have a picnic there），用 where。\n\n',
    '5. Can you tell me the year ___ the first computer was invented?\n',
    '   A. which  B. that  C. when  D. where\n',
    '   答案：C。解析：the year 在从句中作时间状语，用 when。'
)
WHERE subject_id=24 AND title='关系副词 when/where/why' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 12. 非限制性定语从句 [困难]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：非限制性定语从句中用了 that**\n',
    '× My brother, that is a doctor, works in this hospital.\n',
    '√ My brother, who is a doctor, works in this hospital.（非限制性定语从句不能用 that）\n',
    '**要点**：非限制性定语从句不能用 that，指人用 who/whom，指物用 which，表所属用 whose。\n\n',
    '**错误2：which 指代整个主句时理解错误**\n',
    '× He passed the exam, that made his parents happy.\n',
    '√ He passed the exam, which made his parents happy.\n',
    '**要点**：which 指代前面整个主句时，一定用 which 且前有逗号，不能用 that。which 指代"他通过了考试"这件事。\n\n',
    '**错误3：与限制性定语从句的功能区分不清**\n',
    '×（只有一个老师）The teacher who is standing there is Mr. Li.\n',
    '√ The teacher, who is standing there, is Mr. Li.（只有一个老师，用非限制性补充说明位置）\n',
    '**要点**：限制性定语从句去掉后意思不完整（"哪个老师"不明确），非限制性去掉后意思仍然清楚。\n\n',
    '### ⚡ 练一练\n\n',
    '1. My father, ___ is a doctor, works very hard.\n',
    '   A. that  B. which  C. who  D. whom\n',
    '   答案：C。解析：非限制性定语从句不用 that，指人用 who。\n\n',
    '2. He came back late, ___ made his mother angry.\n',
    '   A. that  B. which  C. who  D. what\n',
    '   答案：B。解析：which 指代前面整件事"回来晚了"。\n\n',
    '3. ___ is known to all, the earth goes around the sun.\n',
    '   A. Which  B. As  C. That  D. What\n',
    '   答案：B。解析：as 引导非限制性定语从句，表"正如"，位置可前可后。\n\n',
    '4. I have two sisters, both of ___ are teachers.\n',
    '   A. them  B. who  C. which  D. whom\n',
    '   答案：D。解析：介词后 + whom（指人）。\n\n',
    '5. The book, ___ cover is blue, is mine.\n',
    '   A. which  B. that  C. whose  D. who\n',
    '   答案：C。解析：cover 与 the book 有所属关系，用 whose。'
)
WHERE subject_id=24 AND title='非限制性定语从句' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 13. 宾语从句 [中等]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：宾语从句中用了疑问句语序**\n',
    '× Can you tell me where is the station?\n',
    '√ Can you tell me where the station is?（从句用陈述句语序）\n',
    '× I don\'t know what does he want.\n',
    '√ I don\'t know what he wants.\n',
    '**要点**：宾语从句无论用什么引导词，都必须用陈述句语序（主语 + 谓语）。这是对口升学必考知识点！\n\n',
    '**错误2：if 和 whether 混用场景**\n',
    '× I\'m thinking about if he will come.\n',
    '√ I\'m thinking about whether he will come.（介词后只能用 whether）\n',
    '× I wonder if or not he is right.\n',
    '√ I wonder whether he is right or not.（与 or not 连用时用 whether）\n',
    '**要点**：if/whether 通常可互换，但介词后和 or not 连用时只能用 whether。\n\n',
    '**错误3：否定转移没掌握**\n',
    '× I think he is not right.\n',
    '√ I don\'t think he is right.（英语中 think 的否定要转移）
',
    '**要点**：当主句是 I think/believe/suppose 时，对从句的否定要转移到主句。即"I don\'t think + 肯定从句"。\n\n',
    '### ⚡ 练一练\n\n',
    '1. Could you tell me ___?\n',
    '   A. where is the hospital  B. where the hospital is\n',
    '   C. the hospital is where  D. is where the hospital\n',
    '   答案：B。解析：宾语从句用陈述句语序。\n\n',
    '2. I don\'t know ___ he will come or not.\n',
    '   A. if  B. whether  C. that  D. what\n',
    '   答案：B。解析：与 or not 连用时用 whether。\n\n',
    '3. He asked me ___ I had finished my homework.\n',
    '   A. if  B. what  C. which  D. who\n',
    '   答案：A。解析：问"是否完成"，用 if/whether 引导一般疑问句的宾语从句。\n\n',
    '4. I think ___ he is a good student.\n',
    '   A. which  B. that  C. what  D.不填\n',
    '   答案：D（或 B）。解析：that 引导宾语从句时可省略。\n\n',
    '5. The teacher said that the earth ___ (go) around the sun.\n',
    '   A. goes  B. went  C. is going  D. has gone\n',
    '   答案：A。解析：客观真理不受主句过去时影响，用一般现在时 goes。'
)
WHERE subject_id=24 AND title='宾语从句' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 14. 主语从句 [困难]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：that 引导主语从句时误省略**\n',
    '× He passed the exam is true.\n',
    '√ That he passed the exam is true.（that 引导主语从句不能省略）\n',
    '√ It is true that he passed the exam.（it 作形式主语时 that 也不能省略）\n',
    '**要点**：that 引导主语从句时不可省略（与宾语从句不同）。即使用了 it 作形式主语，that 也不能省。\n\n',
    '**错误2：Wh-词引导的主语从句谓语用错数**\n',
    '× What they need are more time.\n',
    '√ What they need is more time.（主语从句作主语，谓语通常用单数）\n',
    '**要点**：主语从句作主语时，谓语动词通常用单数，即使从句中的宾语是复数。\n\n',
    '**错误3：It is said/reported 结构与主句时态不搭**\n',
    '× It is said that he went abroad in 2020.（is said 和 went 时态可以不一致，这是对的）\n',
    '√ 这一结构主句用一般现在时，从句按实际时间决定——没有问题。\n',
    '**要点**：It is said/reported/believed that... 这个结构中，主句固定用一般现在时，从句时态根据实际情况确定。\n\n',
    '### ⚡ 练一练\n\n',
    '1. ___ he will come to the party is still unknown.\n',
    '   A. If  B. Whether  C. That  D. What\n',
    '   答案：B。解析：主语从句位于句首表示"是否"只能用 whether，不能用 if。\n\n',
    '2. ___ we need is more practice.\n',
    '   A. That  B. What  C. Which  D. Who\n',
    '   答案：B。解析：what 引导主语从句，在从句中作 need 的宾语。\n\n',
    '3. It is important ___ we should learn English well.\n',
    '   A. which  B. that  C. what  D. if\n',
    '   答案：B。解析：It + be + adj. + that 从句，that 不可省略。\n\n',
    '4. ___ the earth is round is known to all.\n',
    '   A. That  B. What  C. Which  D. Whether\n',
    '   答案：A。解析：that 引导主语从句表"地球是圆的"这个事实。\n\n',
    '5. It ___ reported that the meeting has been put off.\n',
    '   A. is  B. was  C. has  D. were\n',
    '   答案：A。解析：It is reported that... 是固定句型，用一般现在时。'
)
WHERE subject_id=24 AND title='主语从句' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 15. 表语从句与同位语从句 [困难]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：表语从句中 that 误省略**\n',
    '× The reason is he is ill.\n',
    '√ The reason is that he is ill.（表语从句中的 that 不可省略）\n',
    '**要点**：表语从句中 that 不能省略（与宾语从句不同）。常见句型：The reason is that... / The problem is that...\n\n',
    '**错误2：同位语从句与定语从句混淆**\n',
    '× I heard the news that was exciting.（that 在从句中作主语→定语从句）\n',
    '√ I heard the news that our team won.（that 在从句中不作成分→同位语从句）\n',
    '**要点**：区分方法：that 在从句中是否作成分。作成分→定语从句（that 可替换为 which）；不作成分→同位语从句（that 不能替换）。\n\n',
    '**错误3：that 从句前缺少抽象名词**\n',
    '× I heard that our team won the game.（这是宾语从句，不是同位语从句）\n',
    '√ I heard the news that our team won the game.（news 是抽象名词，that 从句说明 news 的内容）\n',
    '**要点**：同位语从句必须有抽象名词（news/fact/idea/suggestion/hope）在前，that 从句说明其具体内容。\n\n',
    '### ⚡ 练一练\n\n',
    '1. The problem is ___ we don\'t have enough money.\n',
    '   A. that  B. what  C. which  D.不填\n',
    '   答案：A。解析：表语从句中 that 不可省略。\n\n',
    '2. The fact ___ he didn\'t pass the exam surprised us.\n',
    '   A. which  B. that  C. what  D. who\n',
    '   答案：B。解析：同位语从句，that 说明 fact 的具体内容，不作成分。\n\n',
    '3. The suggestion ___ we should have a meeting is good.\n',
    '   A. that  B. which  C. what  D. who\n',
    '   答案：A。解析：同位语从句用 that 引导。\n\n',
    '4. That\'s ___ I disagree with you.\n',
    '   A. that  B. what  C. why  D. which\n',
    '   答案：C。解析：why 引导表语从句，表"这就是为什么…"。\n\n',
    '5. I have no idea ___ he has gone.\n',
    '   A. that  B. where  C. which  D. who\n',
    '   答案：B。解析：idea 后跟同位语从句，用 where 表"去了哪里"。'
)
WHERE subject_id=24 AND title='表语从句与同位语从句' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 16. 时间/条件/原因状语从句 [中等]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1："主将从现"规则记不住**\n',
    '× If it will rain tomorrow, we will stay at home.\n',
    '√ If it rains tomorrow, we will stay at home.（if 从句用一般现在时表将来）\n',
    '× I will call you when he will come.\n',
    '√ I will call you when he comes.（when 从句用一般现在时表将来）\n',
    '**要点**：在 if/when/unless/as soon as/before/after 引导的条件/时间状语从句中，即使主句用将来时，从句也应用一般现在时。这条规则叫"主将从现"，是考试必考点！\n\n',
    '**错误2：because 和 so 同时使用**\n',
    '× Because he was ill, so he didn\'t come to school.\n',
    '√ Because he was ill, he didn\'t come to school.\n',
    '√ He was ill, so he didn\'t come to school.\n',
    '**要点**：because 和 so 不能同时出现在同一个句子中，二者只能选一个。汉语"因为…所以…"在英语里不能同时说。\n\n',
    '**错误3：until 的肯定/否定用法混淆**\n',
    '× He left until I came back.（leave 是短暂动词，在肯定句中不能与 until 连用）\n',
    '√ He waited until I came back.（wait 是延续动词，肯定句可用 until）\n',
    '√ He didn\'t leave until I came back.（not...until = 直到…才）\n',
    '**要点**：肯定句中 until 前用延续性动词，否定句中 not...until 用短暂动词。\n\n',
    '### ⚡ 练一练\n\n',
    '1. I will wait here ___ you finish your work.\n',
    '   A. when  B. until  C. as soon as  D. if\n',
    '   答案：B。解析：wait until 表"一直等到…"。\n\n',
    '2. He didn\'t go to bed ___ he finished his homework.\n',
    '   A. until  B. when  C. if  D. while\n',
    '   答案：A。解析：not...until 表"直到…才"。\n\n',
    '3. You will fail the exam ___ you study hard.\n',
    '   A. if  B. unless  C. when  D. because\n',
    '   答案：B。解析：unless = if not，"除非你努力学习"。\n\n',
    '4. ___ he was very tired, he kept working.\n',
    '   A. Because  B. Although  C. If  D. When\n',
    '   答案：B。解析：although 表"虽然"，引导让步状语从句。\n\n',
    '5. We\'ll go for a picnic if it ___ (not rain) tomorrow.\n',
    '   A. doesn\'t rain  B. won\'t rain  C. not rains  D. didn\'t rain\n',
    '   答案：A。解析：主将从现——if 从句用一般现在时表将来。'
)
WHERE subject_id=24 AND title='时间/条件/原因状语从句' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ──────────────────────────────────────────────────────────────────────
-- 17. 让步/目的状语从句 [中等]
-- ──────────────────────────────────────────────────────────────────────
UPDATE knowledge_articles SET content_md = CONCAT(
    content_md,
    '\n\n### ⚠️ 职高生常犯错误\n\n',
    '**错误1：although 和 but 同时使用**\n',
    '× Although he was tired, but he still worked hard.\n',
    '√ Although he was tired, he still worked hard.\n',
    '√ He was tired, but he still worked hard.\n',
    '**要点**：英语中 although/though 和 but 不能连用。与 because/so 一样，汉语表达习惯在英语里要二选一。\n\n',
    '**错误2：so that 和 so...that 混淆**\n',
    '× He got up early so that he could catch the bus.（so that 表目的，正确 ✅）\n',
    '× He was so tired that he could catch the bus.（so...that 表结果，但 tired 和 catch the bus 不构成因果）\n',
    '√ He was so tired that he fell asleep.（so...that 表"如此…以至于"，要符合逻辑）\n',
    '**要点**：so that 表目的（为了），从句中常有 can/could；so...that 表结果（如此…以至于）。\n\n',
    '**错误3：so 和 such 的修饰搭配用错**\n',
    '× It was so a beautiful flower that everyone liked it.\n',
    '√ It was such a beautiful flower that everyone liked it.（such + a/an + adj. + 名词）\n',
    '√ It was so beautiful a flower that everyone liked it.（so + adj. + a/an + 名词，较少用）\n',
    '**要点**：so 修饰形容词/副词（so beautiful），such 修饰名词（such a beautiful flower）。\n\n',
    '### ⚡ 练一练\n\n',
    '1. ___ he was tired, he went on working.\n',
    '   A. Although  B. Because  C. If  D. So\n',
    '   答案：A。解析："虽然累但继续工作"，让步关系用 although。\n\n',
    '2. He studies hard ___ he can get a good job.\n',
    '   A. so that  B. so...that  C. such...that  D. because\n',
    '   答案：A。解析：表目的"为了"，用 so that，从句有 can。\n\n',
    '3. It was ___ a hot day that nobody wanted to go out.\n',
    '   A. so  B. such  C. very  D. too\n',
    '   答案：B。解析：such a hot day = so hot a day，"如此热的天"。\n\n',
    '4. Young ___ he is, he knows a lot.\n',
    '   A. although  B. as  C. but  D. because\n',
    '   答案：B。解析：as 引导倒装让步从句，表语置前："虽然年轻"。\n\n',
    '5. She got up early ___ she would miss the train.\n',
    '   A. so that  B. in order that  C. so as not to  D. so as to\n',
    '   答案：C。解析：表目的"为了不"，so as not to + 动词原形。'
)
WHERE subject_id=24 AND title='让步/目的状语从句' AND content_md NOT LIKE '%⚠️ 职高生%';

-- ======================================================================
-- 验证
-- ======================================================================
SELECT CONCAT('v208: 英语文章深度扩容完成。受影响的文章数：', ROW_COUNT()) AS result;
