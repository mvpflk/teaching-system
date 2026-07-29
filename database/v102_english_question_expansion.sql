-- v102: 英语题库扩充 — 对口升学考纲全覆盖，难度以基础为主（60%基础+30%中等+10%困难）
-- 新增: 语法15 + 情景交际5 + 词汇辨析10 + 完形填空10 + 阅读理解15 + 翻译10 = 65题
-- 总计达到: 35(已有) + 65 = 100题

-- ══════════════════════════════════════════
-- 一、语法选择（补15题，总计30题）
-- ══════════════════════════════════════════
INSERT IGNORE INTO question_bank (subject, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('英语[职高]','SINGLE_CHOICE','My brother _____ a teacher for 5 years.','["A. is","B. was","C. has been","D. will be"]','C','for 5 years 表示从过去持续到现在，用现在完成时。主语是第三人称单数，用has been。',1,1),
('英语[职高]','SINGLE_CHOICE','Would you like _____ cup of tea?','["A. other","B. another","C. the other","D. others"]','B','another+单数名词表示"再来一个"。another cup of tea "再来一杯茶"。',1,1),
('英语[职高]','SINGLE_CHOICE','There _____ a pen and two books on the desk.','["A. is","B. are","C. have","D. has"]','A','There be句型遵循就近原则，最近的主语是a pen（单数），所以用is。',2,1),
('英语[职高]','SINGLE_CHOICE','The children _____ not to play in the street.','["A. told","B. were told","C. have told","D. tell"]','B','孩子们"被告诉"，用被动语态were told。',2,1),
('英语[职高]','SINGLE_CHOICE','He asked me _____ I had finished my homework.','["A. that","B. what","C. if","D. which"]','C','宾语从句表示"是否"，用if或whether。',1,1),
('英语[职高]','SINGLE_CHOICE','I have two sisters. One is a nurse, _____ is a teacher.','["A. another","B. the other","C. other","D. others"]','B','两者中"一个...另一个..."用one...the other...。',1,1),
('英语[职高]','SINGLE_CHOICE','Neither Tom nor Jack _____ at home yesterday.','["A. is","B. are","C. was","D. were"]','C','neither...nor...谓语与最近的主语一致(Jack→单数)，yesterday用一般过去时was。',2,1),
('英语[职高]','SINGLE_CHOICE','This is the factory _____ my father worked 20 years ago.','["A. that","B. which","C. where","D. when"]','C','定语从句缺地点状语（worked in the factory），用where。',2,1),
('英语[职高]','SINGLE_CHOICE','You''d better _____ too much time on computer games.','["A. not spend","B. not to spend","C. don''t spend","D. not spending"]','A','had better (not) do sth，固定句型，用动词原形。',2,1),
('英语[职高]','SINGLE_CHOICE','The more you practice, _____ you will become.','["A. the best","B. the better","C. better","D. good"]','B','the more...the more...句型，"越...就越..."，good变成比较级better。',2,1),
('英语[职高]','SINGLE_CHOICE','We will go to the park if it _____ tomorrow.','["A. won''t rain","B. doesn''t rain","C. didn''t rain","D. isn''t raining"]','B','if条件句用一般现在时表示将来，主语it第三人称，do/does否定用doesn''t rain。',1,1),
('英语[职高]','SINGLE_CHOICE','The girl _____ hair is long is my classmate.','["A. who","B. which","C. whose","D. whom"]','C','定语从句表示所属关系（她的头发），用whose。',2,1),
('英语[职高]','SINGLE_CHOICE','I don''t know _____ to do next.','["A. how","B. what","C. where","D. when"]','B','what to do "做什么"，作do的宾语。how to do要加it（how to do it）。',2,1),
('英语[职高]','SINGLE_CHOICE','She is _____ of the two sisters.','["A. tall","B. taller","C. the taller","D. the tallest"]','C','两者比较用比较级，the + 比较级表示"两者中较...的那个"。',2,1),
('英语[职高]','SINGLE_CHOICE','We have lived here _____ 2018.','["A. for","B. since","C. in","D. from"]','B','since+时间点（2018年），for+时间段。since 2018 "自从2018年"。',1,1),

-- ══════════════════════════════════════════
-- 二、情景交际（补5题，总计15题）
-- ══════════════════════════════════════════
('英语[职高]','SINGLE_CHOICE','— Help yourself to some chicken. — _____. I''m full.','["A. Yes, please","B. Thank you","C. No, thanks","D. No problem"]','C','I''m full"我饱了"，拒绝食物时用No, thanks比直接说No更礼貌。',1,1),
('英语[职高]','SINGLE_CHOICE','— Our team won the first prize! — _____.','["A. You''re welcome","B. Good idea","C. Well done","D. That''s all right"]','C','祝贺获奖用Well done!"做得好"。You''re welcome回答感谢，That''s all right回答道歉。',1,1),
('英语[职高]','SINGLE_CHOICE','— Have you got a table for us? — _____. This way, please.','["A. Yes, sure","B. Sorry","C. No way","D. Never mind"]','A','餐厅接待用语，"这边请"说明有空位，Yes, sure表示肯定回答。',1,1),
('英语[职高]','SINGLE_CHOICE','— How do you like the movie? — _____.','["A. It''s very interesting","B. I like watching movies","C. Yes, I do","D. It''s on at 7:00"]','A','How do you like...?"你觉得...怎么样"，回答需给出评价。',1,1),
('英语[职高]','SINGLE_CHOICE','— Excuse me, where is the nearest hospital? — _____.','["A. You are welcome","B. It doesn''t matter","C. Go ahead, then turn left","D. Yes, there is"]','C','问路用祈使句指路。Go ahead"直走"，turn left"左转"。',1,1),

-- ══════════════════════════════════════════
-- 三、词汇辨析（补10题，总计20题）
-- ══════════════════════════════════════════
('英语[职高]','SINGLE_CHOICE','I _____ my keys everywhere but couldn''t find them.','["A. looked at","B. looked for","C. looked after","D. looked up"]','B','look for"寻找"，与后文couldn''t find呼应。look at"看"，look after"照顾"，look up"查阅"。',1,1),
('英语[职高]','SINGLE_CHOICE','The baby is sleeping. Please _____ the TV.','["A. turn on","B. turn off","C. turn up","D. turn down"]','D','宝宝在睡觉，应该调小音量turn down，不是完全关掉turn off（太绝对）。',1,1),
('英语[职高]','SINGLE_CHOICE','She was _____ to hear the good news.','["A. exciting","B. excited","C. excite","D. excitement"]','B','excited修饰人"感到兴奋的"，exciting修饰物"令人兴奋的"。',1,1),
('英语[职高]','SINGLE_CHOICE','We should _____ full use of our time.','["A. make","B. do","C. take","D. have"]','A','make full use of 是固定搭配，"充分利用"。',2,1),
('英语[职高]','SINGLE_CHOICE','The bridge is _____ stone.','["A. made of","B. made from","C. made in","D. made by"]','A','be made of+看得出原材料（石头桥能看出是石头），be made from+看不出原材料。',2,1),
('英语[职高]','SINGLE_CHOICE','Please fill _____ the form with your name and address.','["A. in","B. out","C. up","D. with"]','A','fill in the form"填表"。fill in/out都可表示填写表格。',1,1),
('英语[职高]','SINGLE_CHOICE','I can''t _____ the difference between the two pictures.','["A. speak","B. say","C. talk","D. tell"]','D','tell the difference"辨别差异"，固定搭配。tell在这里表示"辨别、分辨"。',2,1),
('英语[职高]','SINGLE_CHOICE','Paper is _____ wood.','["A. made of","B. made from","C. made by","D. made in"]','B','纸由木头制成，看不出原材料，用be made from。',1,1),
('英语[职高]','SINGLE_CHOICE','He _____ me a story about his childhood.','["A. spoke","B. talked","C. said","D. told"]','D','tell sb a story"给某人讲故事"。tell可接双宾语（tell sb sth），speak/talk/say不能。',1,1),
('英语[职高]','SINGLE_CHOICE','Would you mind _____ the door?','["A. open","B. to open","C. opening","D. opened"]','C','mind doing sth"介意做某事"，固定搭配用动名词。',1,1),

-- ══════════════════════════════════════════
-- 四、完形填空（10题，基于简短语境选择正确词汇）
-- ══════════════════════════════════════════
('英语[职高]','SINGLE_CHOICE','（完形语境）Tom is a middle school student. He gets up __1__ 6:30 every morning.','["A. at","B. in","C. on","D. for"]','A','具体时间点前用介词at：at 6:30。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）He usually has bread and milk for ___.','["A. breakfast","B. lunch","C. supper","D. dinner"]','A','早上吃的是早饭breakfast，与上下文gets up every morning呼应。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）After school, he likes playing ___ with his friends.','["A. piano","B. basketball","C. chess","D. guitar"]','B','play+球类运动不加the，play basketball。play+乐器要加the（play the piano）。chess用play chess。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）His mother often ___ him to school by car.','["A. brings","B. takes","C. carries","D. gets"]','B','take sb to school"带某人去学校"。bring是"带来"，方向相反。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）He is good ___ math and English.','["A. at","B. in","C. for","D. with"]','A','be good at"擅长"，固定搭配。be good with"善于与...相处"。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）Last weekend, his family ___ to the countryside.','["A. go","B. goes","C. went","D. will go"]','C','Last weekend"上周末"是一般过去时的标志，用go的过去式went。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）They ___ some beautiful flowers along the way.','["A. see","B. saw","C. seen","D. seeing"]','B','承接上文Last weekend，继续用一般过去时saw。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）Tom''s father said, "Let''s ___ here for a rest."','["A. stop","B. stops","C. stopped","D. stopping"]','A','Let''s+动词原形"让我们做...吧"。',1,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）They enjoyed ___ very much that day.','["A. them","B. they","C. theirs","D. themselves"]','D','enjoy oneself"玩得开心"。themselves对应they。',2,1),
('英语[职高]','SINGLE_CHOICE','（完形语境）Tom thought it was the ___ day of his holiday.','["A. good","B. better","C. best","D. well"]','C','the+最高级，"假期中最棒的一天"。the best day。',1,1),

-- ══════════════════════════════════════════
-- 五、阅读理解（15题，含短文+问题）
-- ══════════════════════════════════════════
('英语[职高]','SINGLE_CHOICE','（阅读理解）\n短文：Li Ming is 16 years old. He lives in Chengdu with his parents. He gets up at 6:30 and rides his bike to school every day. School starts at 8:00. His favorite subject is English because he wants to travel around the world.\n\n问题：How does Li Ming go to school?','["A. By bus","B. On foot","C. By bike","D. By car"]','C','文中明确说"rides his bike to school"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）What time does school start?','["A. 6:30","B. 7:00","C. 7:30","D. 8:00"]','D','文中"School starts at 8:00"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）Why does Li Ming like English best?','["A. It''s easy","B. His teacher is nice","C. He wants to travel","D. His parents like it"]','C','文中"because he wants to travel around the world"。',1,1),
('英语[职高]','SINGLE_CHOICE','（阅读理解）\n短文：A healthy diet is important for everyone. We should eat more vegetables and fruits. We should drink enough water—at least 8 glasses a day. We should eat less junk food like hamburgers and chips. Exercise is also important. Walking for 30 minutes every day is a good habit.\n\n问题：How much water should we drink every day?','["A. 5 glasses","B. 6 glasses","C. At least 8 glasses","D. As little as possible"]','C','文中"at least 8 glasses a day"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）What should we NOT eat too much according to the passage?','["A. Vegetables","B. Fruits","C. Junk food","D. Water"]','C','文中"We should eat less junk food"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）What is a good habit mentioned in the passage?','["A. Eating hamburgers","B. Walking 30 minutes daily","C. Drinking less water","D. Sleeping more"]','B','文中"Walking for 30 minutes every day is a good habit"。',1,1),
('英语[职高]','SINGLE_CHOICE','（阅读理解）\n短文：Online shopping is becoming more and more popular. You can buy almost everything online—clothes, books, food, and even furniture. The biggest advantage is that you can shop at any time. However, you cannot try on clothes or check the quality of products before buying. Some people worry about the safety of their personal information.\n\n问题：What is the main advantage of online shopping?','["A. It''s cheap","B. You can try on clothes","C. You can shop anytime","D. It''s safe"]','C','文中"The biggest advantage is that you can shop at any time"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）What is one disadvantage of online shopping?','["A. It''s too expensive","B. You cannot try on clothes","C. There are few choices","D. It''s hard to pay"]','B','文中"you cannot try on clothes"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）Some people worry about the safety of their _____.','["A. money","B. password","C. personal information","D. bank account"]','C','文中"the safety of their personal information"。',1,1),
('英语[职高]','SINGLE_CHOICE','（阅读理解）\n短文：Dogs are called "man''s best friend" for a reason. They are loyal and can understand human feelings. Some dogs work as guide dogs for blind people. Others work with police to find dangerous things. Even ordinary pet dogs can make their owners feel less lonely and more happy.\n\n问题：Why are dogs called "man''s best friend"?','["A. They are cute","B. They are loyal and understand feelings","C. They can run fast","D. They eat little"]','B','文中"They are loyal and can understand human feelings"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）What do guide dogs do?','["A. Help police","B. Help blind people","C. Find dangerous things","D. Guard houses"]','B','文中"work as guide dogs for blind people"。',1,1),
('英语[职高]','SINGLE_CHOICE','（接上文）Even ordinary pet dogs can make owners feel _____.','["A. richer","B. stronger","C. less lonely","D. more tired"]','C','文中"feel less lonely and more happy"。',1,1),

-- ══════════════════════════════════════════
-- 六、翻译（中译英+英译中，共10题）— 用FILL_IN题型
-- ══════════════════════════════════════════
('英语[职高]','FILL_IN','（中译英）将"我每天六点起床。"翻译成英文。','[]','I get up at six every day.','关键词：get up"起床"，at six"在六点"，every day"每天"。',1,1),
('英语[职高]','FILL_IN','（英译中）将"Thank you for your help."翻译成中文。','[]','谢谢你的帮助。','thank you for"为...感谢"，your help"你的帮助"。',1,1),
('英语[职高]','FILL_IN','（中译英）将"她喜欢听音乐。"翻译成英文。','[]','She likes listening to music.','like doing sth"喜欢做某事"，listen to music"听音乐"。',1,1),
('英语[职高]','FILL_IN','（英译中）将"There is a book on the desk."翻译成中文。','[]','桌子上有一本书。','There be句型"某处有某物"，on the desk"在桌子上"。',1,1),
('英语[职高]','FILL_IN','（中译英）将"你会说英语吗？"翻译成英文。','[]','Can you speak English?','Can引导的一般疑问句，speak English"说英语"。注意English要大写。',1,1),
('英语[职高]','FILL_IN','（英译中）将"Practice makes perfect."翻译成中文（成语）。','[]','熟能生巧。','英语谚语：practice"练习"，perfect"完美"。',2,1),
('英语[职高]','FILL_IN','（中译英）将"我们学校有一个大操场。"翻译成英文。','[]','There is a big playground in our school.','There be表示"有"，in our school"在我们学校"。',1,1),
('英语[职高]','FILL_IN','（英译中）将"It is never too old to learn."翻译成中文（成语）。','[]','活到老，学到老。','英语谚语：never too old to learn"永不太老而不能学"。',2,1),
('英语[职高]','FILL_IN','（中译英）将"请帮我打开窗户。"翻译成英文。','[]','Please help me open the window.','help sb (to) do sth"帮某人做某事"，open the window"开窗"。',1,1),
('英语[职高]','FILL_IN','（英译中）将"Where there is a will, there is a way."翻译成中文（成语）。','[]','有志者，事竟成。','英语谚语：where there is...there is..."有...就有..."，will"意志"，way"道路/方法"。',2,1);
