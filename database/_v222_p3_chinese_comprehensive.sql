-- ============================================================================
-- V222-P3: 语文[职高] 题库 - 模块三：标点修辞 + 古诗词 + 文学常识 + 写作
-- 覆盖: 标点符号9节点/修辞手法辨析9节点/古诗词鉴赏7节点/
--        文学常识名句默写7节点/应用文写作13节点/话题作文10节点
-- 面向: 四川省对口高考语文命题方向
-- 幂等: INSERT IGNORE - 2026-07-25
-- ============================================================================
SET NAMES utf8mb4;
START TRANSACTION;

-- 确保"调查报告"节点存在（应用文写作新增）
SET @l3_yyw = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=3 AND name='应用文写作' LIMIT 1);
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l3_yyw, 20, 4, '调查报告', 13, NOW(), NOW());

-- ============================================================================
-- 一、标点符号 (9 nodes x 4 = 36 questions)
-- ============================================================================

-- ==================================================================
-- 顿号与逗号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='顿号与逗号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中顿号使用正确的一项是(    )', '[{"key":"A","text":"我国科学、文化、艺术、卫生、教育、和新闻出版业有了很大发展。"},{"key":"B","text":"亚马逊河、尼罗河、密西西比河、和长江是世界四大河流。"},{"key":"C","text":"做月饼的馅料有核桃仁、花生仁、芝麻、青红丝等。"},{"key":"D","text":"他喜欢看《水浒》、《三国演义》、《西游记》、和《红楼梦》。"}]', 'C', '并列词语最后两项之间用"和"不用顿号。书名号之间通常不用顿号。C项各成分是并列关系使用顿号正确。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，逗号使用不当的一项是(    )', '[{"key":"A","text":"北京，是我们伟大祖国的首都。"},{"key":"B","text":"草原上，一群群牛羊在悠闲地吃草。"},{"key":"C","text":"他，今年十八岁，是一名中职学生。"},{"key":"D","text":"今天，天气，真好啊！"}]', 'D', 'D项"今天""天气"之间不是并列关系，不需要用逗号断开。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中顿号使用错误的是(    )', '[{"key":"A","text":"亚马逊河、尼罗河和密西西比河都是世界著名河流。"},{"key":"B","text":"全校三、四年级的学生参加了运动会。"},{"key":"C","text":"他最爱吃的水果有苹果、香蕉、葡萄、等等。"},{"key":"D","text":"这个路口一、三、五禁止左转。"}]', 'C', '"等"或"等等"前不应加顿号。C项"等等"前不应再用顿号。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他最喜欢读的书有《论语》《孟子》《大学》《中庸》。"这句话中书名号之间可以不加顿号。', NULL, 'T', '书名号之间通常可以不用顿号。', 1, 1);

-- ==================================================================
-- 分号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='分号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中分号使用正确的一项是(    )', '[{"key":"A","text":"他从小喜欢读书；也喜欢运动。"},{"key":"B","text":"春天，桃花开了；夏天，荷花开了；秋天，菊花开了；冬天，梅花开了。"},{"key":"C","text":"这件事重要；我们必须做好。"},{"key":"D","text":"她性格开朗；学习认真；工作负责。"}]', 'B', '分号用于复句内部并列分句之间。B项四个分句是并列关系，使用分号正确。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，分号使用不当的是(    )', '[{"key":"A","text":"语言，人们用来抒情达意；文字，人们用来记言记事。"},{"key":"B","text":"出席会议的有教师代表；学生代表；家长代表。"},{"key":"C","text":"他考上了大学；我却落榜了；但他鼓励我继续努力。"},{"key":"D","text":"散文讲究形散神不散；小说则注重情节的完整性。"}]', 'B', 'B项是简单并列短语，用逗号或顿号即可。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他一边吃饭；一边看电视。"这句话中分号使用正确。', NULL, 'F', '"一边......一边......"连接的句子内部简单，用逗号即可。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '给下面句子选择正确的标点符号：我喜欢读小说(    )《红楼梦》让我着迷(    )我喜欢看散文(    )《背影》让我感动。', '[{"key":"A","text":"，，，，。"},{"key":"B","text":"；；；；。"},{"key":"C","text":"：；：；。"},{"key":"D","text":"：，：，。"}]', 'C', '"我喜欢读小说"后用冒号提示下文；"《红楼梦》让我着迷"后用分号。', 3, 1);

-- ==================================================================
-- 问号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='问号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中问号使用正确的一项是(    )', '[{"key":"A","text":"你了解什么是中职教育吗？"},{"key":"B","text":"我不知道他来不来？"},{"key":"C","text":"这件事该不该告诉他呢？我觉得应该告诉他。"},{"key":"D","text":"你从哪里来？我的朋友？"}]', 'A', 'A项是直接疑问句。B项"我不知道"是陈述句用句号。C项选择问句。D项称呼在后的疑问句。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，问号使用正确的是(    )', '[{"key":"A","text":"今天几号了？我不知道。"},{"key":"B","text":"你是哪个学校的？同学？"},{"key":"C","text":"什么时候出发？坐什么车去？带什么东西？这些都要考虑好。"},{"key":"D","text":"这件事？能交给我去做？"}]', 'C', 'C项三个连续疑问句各用问号，后用句号结束全句。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"你难道不知道这样做是错的吗？"这句话是反问句，问号使用正确。', NULL, 'T', '"难道......吗"是反问句式，用问号正确。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他问我明天去不去学校？"这句话的问号使用正确。', NULL, 'F', '这是间接引语，应该用句号。', 2, 1);

-- ==================================================================
-- 引号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='引号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中引号使用正确的一项是(    )', '[{"key":"A","text":"他兴奋地说："明天我们就能出发了"？"},{"key":"B","text":"妈妈问："你什么时候回家？""大概六点左右。"我回答。"},{"key":"C","text":"他果断地回答："这件事我没做过，也不了解。"他说完就走了。"},{"key":"D","text":"鲁迅在《故乡》中写道："其实地上本没有路，走的人多了，也便成了路。""}]', 'D', 'D项直接引用鲁迅的话，使用正确。A项后引号后不应同时出现问号。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中引号用法与其他三项不同的是(    )', '[{"key":"A","text":"古人云："学不可以已。"},{"key":"B","text":"这就是所谓的"工匠精神"。"},{"key":"C","text":"老舍被誉为"人民艺术家"。"},{"key":"D","text":"这种"聪明"其实是自欺欺人。"}]', 'A', 'A项直接引用。B、C、D项分别为特定概念、荣誉称号、反语。', 3, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他问我："你今天有空吗？""这句话引号使用正确。', NULL, 'F', '直接引语内部不需要再用引号嵌套。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他说："这件事很重要"，并叮嘱我要认真对待。"这句话标点使用正确。', NULL, 'F', '叙述人在引语中间断开时，引号内应使用逗号。', 2, 1);

-- ==================================================================
-- 省略号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='省略号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中省略号使用正确的一项是(    )', '[{"key":"A","text":"他翻开书本，开始朗读："春......夏......秋......冬......"},{"key":"B","text":"菜市场里有黄瓜、西红柿、土豆、茄子......等等。"},{"key":"C","text":"那个遥远的村庄......我至今难以忘怀。"},{"key":"D","text":"他支支吾吾地说："我......我......我不是故意的......""}]', 'D', 'D项省略号表示说话断断续续，使用正确。B项省略号和"等等"不能同时使用。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"教室里摆满了书架、桌椅、电脑......等教学设备。"省略号和"等"可以同时使用。', NULL, 'F', '省略号和"等""等等"不能同时使用。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，省略号使用不当的是(    )', '[{"key":"A","text":"词典可以告诉我们许多词语的含义，比如"爱""恨""幸福"......"},{"key":"B","text":"让我们点燃手中的蜡烛，为逝去的生命......祈祷。"},{"key":"C","text":"他一边走一边念叨着："如果当初......那么现在......哎。""},{"key":"D","text":"这件事你去做吧......不，还是让我来吧！"}]', 'B', 'B项"为逝去的生命......祈祷"中省略号使用不当，此处不需要省略或停顿。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '省略号占两个汉字的位置，即六个点(......)，不能分两行。', NULL, 'T', '省略号用六个小圆点表示，占两个字的位置。', 1, 1);

-- ==================================================================
-- 破折号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='破折号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中破折号使用正确的一项是(    )', '[{"key":"A","text":"亚洲——有世界上最高的山峰——珠穆朗玛峰。"},{"key":"B","text":"你今天要——注意安全——早点回家。"},{"key":"C","text":"他最喜欢的城市——成都——是一座来了就不想走的城市。"},{"key":"D","text":"这就是我——一个普普通通的中职学生——对未来——的展望。"}]', 'C', 'C项用破折号表示插入补充说明，使用正确。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中破折号的用法是表示解释说明的一项是(    )', '[{"key":"A","text":"今天天气真好啊——我们一起出去玩吧！"},{"key":"B","text":"四大发明——造纸术、印刷术、火药、指南针——贡献巨大。"},{"key":"C","text":"他急匆匆地跑来——满头大汗——对我说："快走！""},{"key":"D","text":"我们要爱护环境——保护我们共同的家园。"}]', 'B', 'B项破折号表示解释说明。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '破折号占两个汉字的位置，是一条不间断的直线。', NULL, 'T', '破折号(——)占两个字，不同于省略号的六个点。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他大声喊道："快跑——"话还没说完，石头就滚下来了。"破折号表示声音延长。', NULL, 'T', '破折号可用于表示声音的延长。', 1, 1);

-- ==================================================================
-- 书名号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='书名号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中书名号使用正确的一项是(    )', '[{"key":"A","text":"他最喜欢读《鲁迅全集》中的《狂人日记》。"},{"key":"B","text":"《今天我看了》一部很好看的电影。"},{"key":"C","text":"他写了一篇作文叫《我》的学校。"},{"key":"D","text":"这篇文章发表在《中国青年报》的《青春》栏目上。"}]', 'A', 'A项书名号嵌套使用正确。B项不是作品名称。D项"青春"是栏目名不用书名号。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '课程名称、活动名称、会议名称等应该使用书名号。', NULL, 'F', '书名号主要用于书名、篇名、报刊名、作品名等。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列选项中，书名号使用不当的是(    )', '[{"key":"A","text":"《红楼梦》是中国古典四大名著之一。"},{"key":"B","text":"他最喜欢唱《我和我的祖国》这首歌。"},{"key":"C","text":"本学期我们学习了《如何写好应用文》这个单元。"},{"key":"D","text":"这篇文章的标题是《青春与梦想》。"}]', 'C', 'C项"如何写好应用文"是教学单元名称，不应用书名号。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"《人民日报》发表了题为《做新时代的有为青年》的评论员文章。"书名号使用正确。', NULL, 'T', '报刊名和文章标题都用书名号正确。', 1, 1);

-- ==================================================================
-- 连接号与间隔号
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='连接号与间隔号' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中连接号使用正确的一项是(    )', '[{"key":"A","text":"北京——成都的火车每天有十几趟。"},{"key":"B","text":"2019-2025年是我校快速发展的时期。"},{"key":"C","text":"这个机器人身高约1、8米。"},{"key":"D","text":"他住在北京市·海淀区。"}]', 'B', 'B项用短横连接号表示时间起止，使用正确。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，间隔号使用正确的是(    )', '[{"key":"A","text":"他出生于2000·5·20。"},{"key":"B","text":"《念奴娇·赤壁怀古》是苏轼代表作。"},{"key":"C","text":"北京·上海都是特大城市。"},{"key":"D","text":"他毕业于四川·成都·第七中学。"}]', 'B', 'B项间隔号用于词牌名和题目之间，使用正确。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '间隔号(·)常用于外国人名各部分之间，如"迈克尔·乔丹"。', NULL, 'T', '间隔号用于音译的外国人名各部分之间。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '连接号和破折号的写法一样，只是使用场景不同。', NULL, 'F', '连接号是一个字或半字短线，破折号是两个字长线。', 2, 1);

-- ==================================================================
-- 标点符号综合辨析
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='标点符号综合辨析' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，标点符号使用完全正确的一项是(    )', '[{"key":"A","text":"他问我："你去不去"？"},{"key":"B","text":"我不知道你去不去？他问我。"},{"key":"C","text":"他问："你去不去？"我点点头。"},{"key":"D","text":"他问：你去不去？我说：不去。"}]', 'C', 'C项引号内为直接问句使用问号，外部使用句号。', 3, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子标点使用完全正确的是(    )', '[{"key":"A","text":"什么是友谊？什么是真诚？这些都是值得思考的问题。"},{"key":"B","text":"谁是我们的朋友？谁是我们的敌人？这是革命的首要问题。"},{"key":"C","text":"书桌上放着书、本子、和笔。"},{"key":"D","text":"他喜欢看小说、散文、诗歌、等等。"}]', 'B', 'B项连续疑问句各用问号，后用句号收束。C项"和"前不加顿号。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '给下面句子选择正确的标点：老师常说(    )学习要持之以恒(    )不能三天打鱼(    )两天晒网(    )', '[{"key":"A","text":"：，，。"},{"key":"B","text":"：；；。"},{"key":"C","text":"，；；。"},{"key":"D","text":"：，。。"}]', 'A', '冒号引出直接引述内容；并列关系用逗号；句号结束。', 3, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"她唱了一首《青春之歌》——这首歌让她想起了学生时代。"标点使用正确。', NULL, 'T', '书名号表示歌曲名，破折号表示解释说明。', 2, 1);

-- ============================================================================
-- 二、修辞手法辨析 (9 nodes x 4 = 36 questions)
-- ============================================================================

-- ==================================================================
-- 比喻
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='比喻' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了比喻修辞手法的一项是(    )', '[{"key":"A","text":"她的脸红得像苹果。"},{"key":"B","text":"她好像不喜欢这种安排。"},{"key":"C","text":"他长得像他爸爸。"},{"key":"D","text":"天色看起来像是要下雨了。"}]', 'A', 'A项将"脸"比作"苹果"，是明喻。B、D表示推测，C表示比较。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中属于暗喻的一项是(    )', '[{"key":"A","text":"她的眼睛像星星一样明亮。"},{"key":"B","text":"书籍是屹立在时间汪洋中的灯塔。"},{"key":"C","text":"月亮像白玉盘挂在天上。"},{"key":"D","text":"教室里安静得针落地的声音都能听见。"}]', 'B', '暗喻用"是"连接本体和喻体。A、C是明喻，D是夸张。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他像一头倔强的牛，说什么也不肯回头。"运用了比喻的修辞手法。', NULL, 'T', '这是明喻。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，属于借喻的一项是(    )', '[{"key":"A","text":"小姑娘的脸像熟透的苹果。"},{"key":"B","text":"最可恨那些毒蛇猛兽，吃尽了我们的血肉。"},{"key":"C","text":"老师是园丁，我们就是花朵。"},{"key":"D","text":"月亮升起来了，像刚脱水而出的玉轮冰盘。"}]', 'B', '借喻不出现本体，B项用"毒蛇猛兽"直接比喻剥削者。', 3, 1);

-- ==================================================================
-- 比拟（拟人+拟物）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='比拟（拟人+拟物）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了拟人修辞手法的一项是(    )', '[{"key":"A","text":"花儿在风中笑弯了腰。"},{"key":"B","text":"他像一只离弦的箭冲了出去。"},{"key":"C","text":"教室里静悄悄的。"},{"key":"D","text":"太阳从东方升起来了。"}]', 'A', 'A项"花儿笑弯了腰"是拟人。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，不属于拟人的一项是(    )', '[{"key":"A","text":"春天迈着轻盈的步伐走来了。"},{"key":"B","text":"那棵老槐树在风中叹息着。"},{"key":"C","text":"这条小河清得像一面镜子。"},{"key":"D","text":"月亮躲进了云层里。"}]', 'C', 'C项是比喻。A、B、D都赋予事物人的动作。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"这座山像一位巨人守卫着村庄。"这句话运用了拟人的修辞手法。', NULL, 'F', '这是比喻(暗喻)，不是拟人。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"风带着雨星，像在地上寻找什么似的，东一头西一头地乱撞。"是拟人。', NULL, 'T', '"寻找""东一头西一头地乱撞"赋予风雨人的动作。', 2, 1);

-- ==================================================================
-- 借代
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='借代' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了借代修辞手法的一项是(    )', '[{"key":"A","text":"她的笑容像阳光一样温暖。"},{"key":"B","text":"风在轻轻地歌唱。"},{"key":"C","text":"不拿群众一针一线。"},{"key":"D","text":"教室里静得连根针掉在地上都能听见。"}]', 'C', 'C项"一针一线"代指一切财物，是借代。A比喻B拟人D夸张。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，借代手法运用不当的一项是(    )', '[{"key":"A","text":"那个长头发走进来了。"},{"key":"B","text":"一群红领巾跑过来。"},{"key":"C","text":"白衣天使日夜守护着病人。"},{"key":"D","text":"他笔下功夫很好，可谓"妙笔生花"。"}]', 'A', 'A项"长头发"不能作为特征借代，语义不明确。', 3, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"巾帼不让须眉"中运用的修辞手法是(    )', '[{"key":"A","text":"比喻"},{"key":"B","text":"借代"},{"key":"C","text":"比拟"},{"key":"D","text":"对偶"}]', 'B', '"巾帼"借代指女性；"须眉"借代指男性。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"我们要爱护校园里的一草一木。"这句话运用了借代。', NULL, 'F', '"一草一木"在这里是泛指，不是借代。', 2, 1);

-- ==================================================================
-- 夸张
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='夸张' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了夸张修辞手法的一项是(    )', '[{"key":"A","text":"教室里安静得连针掉在地上的声音都能听见。"},{"key":"B","text":"他像一只兔子一样跑得很快。"},{"key":"C","text":"她的歌声很动听。"},{"key":"D","text":"今天是星期三。"}]', 'A', 'A项夸大了安静的程度，是夸张。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，属于扩大的夸张的一项是(    )', '[{"key":"A","text":"这块地方还没有巴掌大。"},{"key":"B","text":"他高兴得一蹦三尺高。"},{"key":"C","text":"她泪如雨下。"},{"key":"D","text":"白发三千丈，缘愁似个长。"}]', 'D', '"白发三千丈"是扩大夸张。A缩小夸张。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"危楼高百尺，手可摘星辰。"运用了夸张的修辞手法。', NULL, 'T', '"手可摘星辰"极度夸大了楼的高度。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"他的嗓子像铜钟一样洪亮。"这句话运用了夸张。', NULL, 'F', '这是比喻(明喻)，不是夸张。', 2, 1);

-- ==================================================================
-- 对偶
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='对偶' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了对偶修辞手法的一项是(    )', '[{"key":"A","text":"两个黄鹂鸣翠柳，一行白鹭上青天。"},{"key":"B","text":"日照香炉生紫烟，遥看瀑布挂前川。"},{"key":"C","text":"我热爱这片土地，我热爱这片天空。"},{"key":"D","text":"今天天气真好。"}]', 'A', 'A项对仗工整。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列选项中，对偶与例句相同的一项是(    )例句：横眉冷对千夫指，俯首甘为孺子牛。', '[{"key":"A","text":"两个黄鹂鸣翠柳，一行白鹭上青天。"},{"key":"B","text":"桃花潭水深千尺，不及汪伦送我情。"},{"key":"C","text":"海内存知己，天涯若比邻。"},{"key":"D","text":"举头望明月，低头思故乡。"}]', 'A', '例句对仗工整，只有A项与之一样工整。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"日出江花红胜火，春来江水绿如蓝。"运用了对偶。', NULL, 'T', '"日出"对"春来"，"江花"对"江水"，对仗工整。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"书山有路勤为径，学海无涯苦作舟。"运用了对偶和比喻。', NULL, 'T', '对偶+比喻两种修辞。', 2, 1);

-- ==================================================================
-- 排比
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='排比' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了排比修辞手法的一项是(    )', '[{"key":"A","text":"两个黄鹂鸣翠柳，一行白鹭上青天。"},{"key":"B","text":"白发三千丈，缘愁似个长。"},{"key":"C","text":"我热爱我的祖国，我热爱我的人民，我热爱我的家乡。"},{"key":"D","text":"这是什么花？这是荷花。"}]', 'C', 'C项三个"我热爱"开头的句子是排比。A对偶B夸张D设问。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列各句中，属于排比修辞的一项是(    )', '[{"key":"A","text":"天色看起来好像要下雨了。"},{"key":"B","text":"有的在游泳，有的在钓鱼，有的在划船。"},{"key":"C","text":"山朗润起来了，水涨起来了，太阳的脸红起来了。"},{"key":"D","text":"海内存知己，天涯若比邻。"}]', 'B', 'B项"有的......有的......有的......"是排比。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"生活是一面镜子，你笑它也笑，你哭它也哭。"是排比。', NULL, 'F', '只有两个"你......它......"结构，不符合排比至少三项的要求。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"青春是最美好的季节，青春是梦想开始的地方，青春是奋斗的黄金时期。"是排比。', NULL, 'T', '三个"青春是......"开头的句子是排比。', 1, 1);

-- ==================================================================
-- 反问
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='反问' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了反问修辞手法的一项是(    )', '[{"key":"A","text":"什么是幸福？幸福是一种感觉。"},{"key":"B","text":"难道学习不重要吗？"},{"key":"C","text":"你去过北京吗？"},{"key":"D","text":"他今天没有来上课。"}]', 'B', 'B项"难道......吗"是反问。A是设问。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"这件事难道不是你做的吗？"这句话的意思是(    )', '[{"key":"A","text":"不是你做的。"},{"key":"B","text":"是你做的。"},{"key":"C","text":"不确定是谁做的。"},{"key":"D","text":"没人做。"}]', 'B', '反问"难道不是......吗"表达肯定的含义。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"这难道不是一个伟大的奇迹吗？"是反问。', NULL, 'T', '"难道......吗"是反问标志句式。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '反问与设问都是无疑而问，所以二者没有区别。', NULL, 'F', '设问是自问自答，反问是问中含答。', 2, 1);

-- ==================================================================
-- 设问
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='设问' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列句子中，运用了设问修辞手法的一项是(    )', '[{"key":"A","text":"幸福是什么？幸福是一种感觉。"},{"key":"B","text":"这难道不是很好吗？"},{"key":"C","text":"你叫什么名字？"},{"key":"D","text":"今天天气很好。"}]', 'A', 'A项先问后答是设问。B项是反问。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '设问修辞手法的主要作用是(    )', '[{"key":"A","text":"增强语言气势。"},{"key":"B","text":"引起读者注意，启发思考。"},{"key":"C","text":"使语言更形象。"},{"key":"D","text":"使语言更含蓄。"}]', 'B', '设问的主要作用是引人注意、启发思考。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"是谁创造了人类世界？是我们劳动群众。"是设问。', NULL, 'T', '先问后答是典型的设问。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"你难道不觉得这样做不对吗？"是设问。', NULL, 'F', '"难道......吗"是反问句式。', 2, 1);

-- ==================================================================
-- 易混修辞辨析（借代vs借喻/比喻vs比拟/对偶vs排比/设问vs反问）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='易混修辞辨析（借代vs借喻/比喻vs比拟/对偶vs排比/设问vs反问）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列属于借代而非借喻的一项是(    )', '[{"key":"A","text":"最可恨那些毒蛇猛兽，吃尽了我们的血肉。"},{"key":"B","text":"书籍是屹立在时间汪洋中的灯塔。"},{"key":"C","text":"理想是石，敲出星星之火。"},{"key":"D","text":"一群红领巾正在打扫卫生。"}]', 'D', 'D项"红领巾"代指少先队员，是借代。A、B、C是借喻。', 3, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列对修辞手法的判断正确的一项是(    )', '[{"key":"A","text":"他像猴子一样灵活。——比拟"},{"key":"B","text":"春天来了。——比喻"},{"key":"C","text":"她的笑容像花朵一样绽放。——比喻"},{"key":"D","text":"人生自古谁无死，留取丹心照汗青。——排比"}]', 'C', 'C判断正确。A是比喻，B是陈述，D是对偶。', 3, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '对偶和排比的主要区别是(    )', '[{"key":"A","text":"对偶字数必须相同，排比无要求。"},{"key":"B","text":"对偶成对出现(两项)，排比至少三项。"},{"key":"C","text":"对偶用于诗中，排比不能。"},{"key":"D","text":"对偶写景，排比议论。"}]', 'B', '对偶是两句成对，排比至少三项。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"难道你还不了解我吗？——我当然了解你。"前面是反问后面是设问。', NULL, 'F', '前面是反问，后面是补充说明不是设问。', 3, 1);

-- ============================================================================
-- 三、古诗词鉴赏 (7 nodes x 4 = 28 questions)
-- ============================================================================

-- ==================================================================
-- 《诗经》选篇：关雎、蒹葭
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='《诗经》选篇：关雎、蒹葭' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"关关雎鸠，在河之洲"出自《诗经》中的哪一篇？(    )', '[{"key":"A","text":"《蒹葭》"},{"key":"B","text":"《关雎》"},{"key":"C","text":"《静女》"},{"key":"D","text":"《采薇》"}]', 'B', '出自《诗经·关雎》。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"蒹葭苍苍，白露为霜。所谓伊人，在水一方。"主要表达的是(    )', '[{"key":"A","text":"对恋人的热切追求"},{"key":"B","text":"对家乡的深切思念"},{"key":"C","text":"对理想境界可望而不可即的惆怅"},{"key":"D","text":"送别之情"}]', 'C', '表达对美好事物可望而不可即的惆怅。', 2, 1),
('语文[职高]', @n, 'FILL_IN', '"关关雎鸠，在河之洲。窈窕淑女，____。"', NULL, '君子好逑', '"逑"意为配偶。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"蒹葭苍苍，白露为霜"的"为"意思是"凝结成"。', NULL, 'T', '"为"是"成为、凝结成"。', 2, 1);

-- ==================================================================
-- 唐诗鉴赏：将进酒、茅屋为秋风所破歌
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='唐诗鉴赏：将进酒、茅屋为秋风所破歌' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"天生我材必有用，千金散尽还复来"出自哪位诗人？(    )', '[{"key":"A","text":"杜甫"},{"key":"B","text":"李白"},{"key":"C","text":"白居易"},{"key":"D","text":"王维"}]', 'B', '出自李白《将进酒》。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"君不见高堂明镜悲白发，朝如青丝暮成雪"表达的情感是(    )', '[{"key":"A","text":"青春易逝的感慨"},{"key":"B","text":"对社会的不满"},{"key":"C","text":"仕途不顺的抱怨"},{"key":"D","text":"思乡之情"}]', 'A', '"朝如青丝暮成雪"极写人生短暂。', 2, 1),
('语文[职高]', @n, 'FILL_IN', '"安得广厦千万间，____天下寒士俱欢颜。"（杜甫《茅屋为秋风所破歌》）', NULL, '大庇', '"大庇"意为广泛地庇护。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '李白是唐代现实主义诗人，被誉为"诗圣"。', NULL, 'F', '李白是浪漫主义诗人"诗仙"，杜甫是"诗圣"。', 1, 1);

-- ==================================================================
-- 宋词鉴赏：念奴娇·赤壁怀古、雨霖铃
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='宋词鉴赏：念奴娇·赤壁怀古、雨霖铃' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"大江东去，浪淘尽，千古风流人物"出自(    )', '[{"key":"A","text":"《雨霖铃》"},{"key":"B","text":"《念奴娇·赤壁怀古》"},{"key":"C","text":"《江城子》"},{"key":"D","text":"《声声慢》"}]', 'B', '出自苏轼《念奴娇·赤壁怀古》。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"今宵酒醒何处？杨柳岸，晓风残月"出自哪位词人？(    )', '[{"key":"A","text":"苏轼"},{"key":"B","text":"柳永"},{"key":"C","text":"辛弃疾"},{"key":"D","text":"李清照"}]', 'B', '出自柳永《雨霖铃·寒蝉凄切》。', 1, 1),
('语文[职高]', @n, 'FILL_IN', '"人生如梦，____。"（苏轼《念奴娇·赤壁怀古》）', NULL, '一尊还酹江月', '"酹"读lei，洒酒祭奠。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '苏轼和柳永都是宋代豪放词派代表。', NULL, 'F', '苏轼是豪放派，柳永是婉约派。', 1, 1);

-- ==================================================================
-- 诗歌意象与意境分析
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='诗歌意象与意境分析' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列通常与"愁思"相关的意象是(    )', '[{"key":"A","text":"杨柳、美酒"},{"key":"B","text":"战鼓、旌旗"},{"key":"C","text":"江水、孤鸿、落叶"},{"key":"D","text":"荷花、青松"}]', 'C', '江水、孤鸿、落叶是表达愁思的经典意象。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"大漠孤烟直，长河落日圆"的意境特点是(    )', '[{"key":"A","text":"雄浑壮阔"},{"key":"B","text":"凄清冷寂"},{"key":"C","text":"清新明丽"},{"key":"D","text":"幽静深远"}]', 'A', '展现大漠雄浑壮丽的画面。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '意象是融合诗人主观情感的客观物象，意境是多个意象组合形成的整体氛围。', NULL, 'T', '正确。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"枯藤老树昏鸦，小桥流水人家"的手法主要是(    )', '[{"key":"A","text":"比喻"},{"key":"B","text":"意象叠加"},{"key":"C","text":"拟人"},{"key":"D","text":"借代"}]', 'B', '名词意象直接排列，是"意象叠加"或"列锦"。', 3, 1);

-- ==================================================================
-- 诗歌表达技巧（抒情方式/描写手法/修辞）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='诗歌表达技巧（抒情方式/描写手法/修辞）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列属于借景抒情的一项是(    )', '[{"key":"A","text":"感时花溅泪，恨别鸟惊心。"},{"key":"B","text":"人生自古谁无死，留取丹心照汗青。"},{"key":"C","text":"长风破浪会有时，直挂云帆济沧海。"},{"key":"D","text":"采菊东篱下，悠然见南山。"}]', 'A', 'A项通过描写景色表达忧国忧民之情。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"遥想公瑾当年，小乔初嫁了"运用了(    )', '[{"key":"A","text":"借古讽今"},{"key":"B","text":"虚实结合"},{"key":"C","text":"托物言志"},{"key":"D","text":"衬托"}]', 'B', '回忆历史是虚写，与现实形成对比。', 3, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"感时花溅泪，恨别鸟惊心"运用了拟人。', NULL, 'T', '花和鸟被赋予人的情感。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '直抒胸臆和借景抒情不能在一首诗中共存。', NULL, 'F', '一首诗中可同时运用多种抒情方式。', 2, 1);

-- ==================================================================
-- 诗歌思想情感与观点态度
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='诗歌思想情感与观点态度' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '杜甫《茅屋为秋风所破歌》的核心思想是(    )', '[{"key":"A","text":"对自己命运不济的哀叹"},{"key":"B","text":"对社会的控诉"},{"key":"C","text":"推己及人、兼济天下"},{"key":"D","text":"思念亲人"}]', 'C', '"大庇天下寒士俱欢颜"表达推己及人的高尚情怀。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"人生得意须尽欢，莫使金樽空对月"体现的人生态度是(    )', '[{"key":"A","text":"悲观消极"},{"key":"B","text":"热爱生活、乐观豪迈"},{"key":"C","text":"逃避现实"},{"key":"D","text":"淡泊名利"}]', 'B', '热爱生活、珍惜当下、乐观自信。', 2, 1),
('语文[职高]', @n, 'FILL_IN', '"安得广厦千万间，大庇天下____俱欢颜。"', NULL, '寒士', '指贫寒之人。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"长风破浪会有时，直挂云帆济沧海"表达乐观自信的态度。', NULL, 'T', '出自李白《行路难》。', 1, 1);

-- ==================================================================
-- 诗歌语言赏析（炼字/诗眼/风格）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='诗歌语言赏析（炼字/诗眼/风格）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"春风又绿江南岸"中"绿"的妙处是(    )', '[{"key":"A","text":"写出春天的颜色"},{"key":"B","text":"形容词活用为动词，有动态色彩感"},{"key":"C","text":"表示植物生长"},{"key":"D","text":"押韵"}]', 'B', '"绿"字形容词活用为动词，充满画面感。', 3, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '李白诗歌风格的特点是(    )', '[{"key":"A","text":"沉郁顿挫"},{"key":"B","text":"豪放飘逸"},{"key":"C","text":"婉约含蓄"},{"key":"D","text":"清新自然"}]', 'B', '李白"豪放飘逸"，杜甫"沉郁顿挫"。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"诗眼"是诗歌中最能体现诗人情感的关键词句。', NULL, 'T', '诗眼是最精练传神的字词或句子。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '杜甫被称为"诗圣"，诗风豪放飘逸。', NULL, 'F', '杜甫诗风是"沉郁顿挫"。', 1, 1);

-- ============================================================================
-- 四、文学常识与名句默写 (7 nodes x 4 = 28 questions)
-- ============================================================================

-- ==================================================================
-- 先秦诗文默写：静女、采薇、侍坐、寡人之于国、劝学
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='先秦诗文默写：静女、采薇、侍坐、寡人之于国、劝学' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'FILL_IN', '"昔我往矣，____。今我来思，雨雪霏霏。"（《采薇》）', NULL, '杨柳依依', '以乐景写哀情。', 1, 1),
('语文[职高]', @n, 'FILL_IN', '"青，取之于蓝，____；冰，水为之，而寒于水。"（《劝学》）', NULL, '而青于蓝', '比喻学生超过老师。', 1, 1),
('语文[职高]', @n, 'FILL_IN', '"故不积跬步，____；不积小流，无以成江海。"（《劝学》）', NULL, '无以至千里', '强调积累之重要。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"静女其姝，俟我于城隅"出自《诗经·采薇》。', NULL, 'F', '出自《静女》。', 1, 1);

-- ==================================================================
-- 唐宋诗文默写：师说、将进酒、琵琶行、念奴娇、雨霖铃
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='唐宋诗文默写：师说、将进酒、琵琶行、念奴娇、雨霖铃' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'FILL_IN', '"师者，所以____解惑也。"（韩愈《师说》）', NULL, '传道受业', '"受"通"授"。', 1, 1),
('语文[职高]', @n, 'FILL_IN', '"同是天涯沦落人，____。"（白居易《琵琶行》）', NULL, '相逢何必曾相识', '慨叹命运相似。', 1, 1),
('语文[职高]', @n, 'FILL_IN', '"大江东去，浪淘尽，____。"（苏轼《念奴娇·赤壁怀古》）', NULL, '千古风流人物', '"风流人物"指杰出人物。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"千呼万唤始出来，犹抱琵琶半遮面"出自韩愈《师说》。', NULL, 'F', '出自白居易《琵琶行》。', 1, 1);

-- ==================================================================
-- 宋文默写：六国论
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='宋文默写：六国论' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'FILL_IN', '"六国破灭，非兵不利，战不善，____。"（苏洵《六国论》）', NULL, '弊在赂秦', '开篇立论：灭亡原因在于割地赂秦。', 2, 1),
('语文[职高]', @n, 'FILL_IN', '"以地事秦，犹____，薪不尽，火不灭。"（苏洵《六国论》）', NULL, '抱薪救火', '比喻割地赂秦的危害。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"弊在赂秦"的"赂"意思是(    )', '[{"key":"A","text":"贿赂"},{"key":"B","text":"赠送财物"},{"key":"C","text":"割让土地"},{"key":"D","text":"赔偿"}]', 'B', '"赂"本意送财物给人。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '《六国论》中心论点是"弊在赂秦"。', NULL, 'T', '苏洵借古讽今。', 2, 1);

-- ==================================================================
-- 现代诗歌默写：我爱这土地、雨巷、致橡树
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='现代诗歌默写：我爱这土地、雨巷、致橡树' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'FILL_IN', '"为什么我的眼里常含泪水？____"（艾青《我爱这土地》）', NULL, '因为我对这土地爱得深沉', '表达对祖国的热爱。', 1, 1),
('语文[职高]', @n, 'FILL_IN', '"撑着油纸伞，独自彷徨在悠长，悠长又寂寥的____。"（戴望舒《雨巷》）', NULL, '雨巷', '核心意象，象征迷惘心境。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '舒婷《致橡树》"作为树的形象和你站在一起"表达的爱情观是(    )', '[{"key":"A","text":"依附顺从"},{"key":"B","text":"对立斗争"},{"key":"C","text":"平等独立、相互扶持"},{"key":"D","text":"无私奉献"}]', 'C', '表达平等独立的爱情观。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '《我爱这土地》《雨巷》《致橡树》都是现代诗歌代表作。', NULL, 'T', '三首均为重要现代诗。', 1, 1);

-- ==================================================================
-- 文学体裁常识（诗歌/散文/小说/戏剧）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='文学体裁常识（诗歌/散文/小说/戏剧）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列属于散文体裁的是(    )', '[{"key":"A","text":"鲁迅《祝福》"},{"key":"B","text":"朱自清《荷塘月色》"},{"key":"C","text":"曹禺《雷雨》"},{"key":"D","text":"老舍《茶馆》"}]', 'B', '《荷塘月色》是散文。A小说C、D戏剧。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '小说三要素是(    )', '[{"key":"A","text":"时间、地点、人物"},{"key":"B","text":"开头、发展、结局"},{"key":"C","text":"人物、情节、环境"},{"key":"D","text":"论点、论据、论证"}]', 'C', '人物、情节、环境是小说三要素。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '文学"四分法"分为诗歌、散文、小说、戏剧。', NULL, 'T', '这是基本分类方式。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '剧本必须有尖锐的矛盾冲突和个性化语言。', NULL, 'T', '没有冲突就没有戏剧。', 2, 1);

-- ==================================================================
-- 重要作家作品（古今中外）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='重要作家作品（古今中外）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '下列对应正确的一项是(    )', '[{"key":"A","text":"鲁迅——《子夜》"},{"key":"B","text":"茅盾——《呐喊》"},{"key":"C","text":"老舍——《骆驼祥子》"},{"key":"D","text":"巴金——《雷雨》"}]', 'C', '《子夜》茅盾，《呐喊》鲁迅，《雷雨》曹禺。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"俄国革命的一面镜子"指的是(    )', '[{"key":"A","text":"普希金"},{"key":"B","text":"列夫·托尔斯泰"},{"key":"C","text":"契诃夫"},{"key":"D","text":"高尔基"}]', 'B', '托尔斯泰代表作有《战争与和平》等。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '下列属于莎士比亚四大悲剧的是(    )', '[{"key":"A","text":"《威尼斯商人》"},{"key":"B","text":"《罗密欧与朱丽叶》"},{"key":"C","text":"《哈姆雷特》"},{"key":"D","text":"《仲夏夜之梦》"}]', 'C', '四大悲剧是《哈姆雷特》《奥赛罗》《李尔王》《麦克白》。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"史家之绝唱，无韵之离骚"是鲁迅对《史记》的评价。', NULL, 'T', '既肯定史学价值也赞美文学成就。', 1, 1);

-- ==================================================================
-- 文化常识（称谓/历法/官职/科举/地理）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='文化常识（称谓/历法/官职/科举/地理）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"弱冠"指的是多少岁的男子？(    )', '[{"key":"A","text":"十五岁"},{"key":"B","text":"二十岁"},{"key":"C","text":"三十岁"},{"key":"D","text":"四十岁"}]', 'B', '男子二十岁行冠礼表示成年。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '殿试的第一名称为(    )', '[{"key":"A","text":"进士"},{"key":"B","text":"榜眼"},{"key":"C","text":"探花"},{"key":"D","text":"状元"}]', 'D', '第一名状元、第二名榜眼、第三名探花。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '"江表"指的是(    )', '[{"key":"A","text":"崤山以东"},{"key":"B","text":"函谷关以西"},{"key":"C","text":"长江以南"},{"key":"D","text":"黄河以北"}]', 'C', '长江以南地区。', 3, 1),
('语文[职高]', @n, 'TRUE_FALSE', '"二十四节气"是中国古代历法的重要组成部分。', NULL, 'T', '2016年列入联合国非遗名录。', 1, 1);

-- ============================================================================
-- 五、应用文写作 (13 nodes x 3 = 39 questions)
-- ============================================================================

-- ==================================================================
-- 通知
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='通知' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '通知标题应写在(    )', '[{"key":"A","text":"第一行居中"},{"key":"B","text":"第一行空两格"},{"key":"C","text":"左对齐"},{"key":"D","text":"右对齐"}]', 'A', '标题居中。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '通知必不可少的是(    )', '[{"key":"A","text":"祝颂语"},{"key":"B","text":"问候语"},{"key":"C","text":"落款和日期"},{"key":"D","text":"附件"}]', 'C', '通知必须有标题、称呼、正文、落款和日期。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '通知正文后应写"此致敬礼"。', NULL, 'F', '这是书信的写法，通知不需要。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '通知正文需要写清时间、地点、人员、事项等。', NULL, 'T', '通知要素要完整。', 1, 1);

-- ==================================================================
-- 启事
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='启事' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '写招领启事应(    )', '[{"key":"A","text":"详细描述物品特征"},{"key":"B","text":"简略说明，具体特征让失主核实时确认"},{"key":"C","text":"直接写失主姓名"},{"key":"D","text":"不写联系方式"}]', 'B', '防止冒领。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '符合启事规范的是(    )', '[{"key":"A","text":"寻人"},{"key":"B","text":"寻人启事"},{"key":"C","text":"关于寻人的通知"},{"key":"D","text":"寻人说明书"}]', 'B', '标题写居中写"xx启事"。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '寻物启事和招领启事写法相同。', NULL, 'F', '寻物可详述，招领只能简略。', 2, 1);

-- ==================================================================
-- 书信
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='书信' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"此致""敬礼"的正确写法是(    )', '[{"key":"A","text":"连写一行"},{"key":"B","text":""此致"空两格一行，"敬礼"顶格一行"},{"key":"C","text":""此致"顶格，"敬礼"空两格"},{"key":"D","text":""此致"不另起一行"}]', 'B', '"此致"空两格，"敬礼"顶格。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '书信称呼应(    )', '[{"key":"A","text":"空两格"},{"key":"B","text":"居中"},{"key":"C","text":"顶格加冒号"},{"key":"D","text":"写在正文中"}]', 'C', '称呼顶格写，后加冒号。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '署名在正文右下方，日期写在署名下方。', NULL, 'T', '正确。', 1, 1);

-- ==================================================================
-- 便条（请假条/留言条/托事条）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='便条（请假条/留言条/托事条）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '请假条不必包含(    )', '[{"key":"A","text":"请假原因"},{"key":"B","text":"起止时间"},{"key":"C","text":"工作经历"},{"key":"D","text":"署名和日期"}]', 'C', '请假条不需要写工作经历。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '便条与书信的主要区别是(    )', '[{"key":"A","text":"便条不需署名"},{"key":"B","text":"便条行文更简洁"},{"key":"C","text":"便条不需日期"},{"key":"D","text":"便条需盖章"}]', 'B', '便条格式更简化。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '留言条可以不写日期。', NULL, 'F', '必须写日期。', 1, 1);

-- ==================================================================
-- 计划
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='计划' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '计划正文不包括(    )', '[{"key":"A","text":"目标和任务"},{"key":"B","text":"措施和时间安排"},{"key":"C","text":"已取得的成绩"},{"key":"D","text":"背景和依据"}]', 'C', '计划是事前规划不应写已取得的成绩。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '计划目标应该(    )', '[{"key":"A","text":"笼统概括"},{"key":"B","text":"越高越好"},{"key":"C","text":"量化具体、切实可行"},{"key":"D","text":"与别人相同"}]', 'C', '目标要具体可量化。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '计划是事前规划，总结是事后回顾。', NULL, 'T', '正确。', 1, 1);

-- ==================================================================
-- 总结
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='总结' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '总结主体一般包括(    )', '[{"key":"A","text":"只有成绩"},{"key":"B","text":"成绩、经验、问题"},{"key":"C","text":"只有问题"},{"key":"D","text":"全部写未来规划"}]', 'B', '总结要全面客观。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '符合总结规范的是(    )', '[{"key":"A","text":"《2025-2026第一学期学习总结》"},{"key":"B","text":"《我的总结》"},{"key":"C","text":"《关于学习的总结报告的通知》"},{"key":"D","text":"《总结》"}]', 'A', '标题应明确时间和内容。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '总结应生动形象多用修辞。', NULL, 'F', '总结要客观平实。', 2, 1);

-- ==================================================================
-- 会议记录
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='会议记录' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '会议记录不需记录(    )', '[{"key":"A","text":"会议名称、时间、地点"},{"key":"B","text":"出席人和主持人"},{"key":"C","text":"记录人的评价感受"},{"key":"D","text":"发言要点和决议"}]', 'C', '会议记录要求客观准确。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '会议记录最后需要(    )', '[{"key":"A","text":"全体参会人签名"},{"key":"B","text":"主持人和记录人签名"},{"key":"C","text":"主持人和主要发言人签名"},{"key":"D","text":"不需要签名"}]', 'B', '主持人和记录人签名。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '会议记录和会议纪要是一回事。', NULL, 'F', '记录是原始记录，纪要是整理文件。', 2, 1);

-- ==================================================================
-- 单据（借条/收条/领条/欠条）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='单据（借条/收条/领条/欠条）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '借条金额的正确写法是(    )', '[{"key":"A","text":"只写阿拉伯数字"},{"key":"B","text":"汉字大写和阿拉伯数字并用"},{"key":"C","text":"只用汉字大写"},{"key":"D","text":"用拼音大写"}]', 'B', '大写防止篡改，小写便于阅读。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '适用欠条的是(    )', '[{"key":"A","text":"借钱"},{"key":"B","text":"收到货款"},{"key":"C","text":"已提货未付款"},{"key":"D","text":"领取物品"}]', 'C', '欠条适用于已收受物品暂未付款。', 3, 1),
('语文[职高]', @n, 'TRUE_FALSE', '借条金额写错可用涂改液修改。', NULL, 'F', '金额不得涂改，应重写。', 1, 1);

-- ==================================================================
-- 说明书
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='说明书' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '说明书语言要求(    )', '[{"key":"A","text":"生动形象"},{"key":"B","text":"准确简明、通俗易懂"},{"key":"C","text":"辞藻华丽"},{"key":"D","text":"诙谐幽默"}]', 'B', '说明书必须准确简明。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '产品说明书必不可少的是(    )', '[{"key":"A","text":"产品外观"},{"key":"B","text":"安全注意事项"},{"key":"C","text":"厂家历史"},{"key":"D","text":"价格对比"}]', 'B', '安全注意事项关乎用户安全。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '说明书常用列数字、分类别等说明方法。', NULL, 'T', '正确。', 1, 1);

-- ==================================================================
-- 求职信
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='求职信' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '求职信第一段应写(    )', '[{"key":"A","text":"罗列获奖情况"},{"key":"B","text":"表明求职意向和消息来源"},{"key":"C","text":"直接要求面试"},{"key":"D","text":"批评用人单位"}]', 'B', '礼貌表明求职意向和消息来源。', 1, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '求职信语言风格应(    )', '[{"key":"A","text":"随意亲切"},{"key":"B","text":"礼貌得体、自信不自傲"},{"key":"C","text":"过分谦虚"},{"key":"D","text":"夸张炫耀"}]', 'B', '应礼貌得体、自信不自傲。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '求职信最后应写上联系方式。', NULL, 'T', '方便用人单位联系。', 1, 1);

-- ==================================================================
-- 应聘书
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='应聘书' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '应聘书与求职信主要区别是(    )', '[{"key":"A","text":"应聘书不需署名"},{"key":"B","text":"应聘书针对具体招聘，对照岗位要求"},{"key":"C","text":"求职信更正式"},{"key":"D","text":"应聘书需手写"}]', 'B', '应聘书精准回应具体招聘信息。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '招聘要求"良好沟通能力"，应聘者应(    )', '[{"key":"A","text":"只写"我沟通能力强""},{"key":"B","text":"举例说明"},{"key":"C","text":"忽略要求"},{"key":"D","text":"复制网上的描述"}]', 'B', '用事实和事例说话。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '应聘书是针对招聘信息写的申请文书。', NULL, 'T', '正确。', 1, 1);

-- ==================================================================
-- 调查报告
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='调查报告' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '调查报告正文结构是(    )', '[{"key":"A","text":"前言、主体、结语"},{"key":"B","text":"标题、称呼、正文、落款"},{"key":"C","text":"开头、经过、结尾"},{"key":"D","text":"论题、论点、论证"}]', 'A', '由前言、主体、结语三部分组成。', 2, 1),
('语文[职高]', @n, 'SINGLE_CHOICE', '调查报告数据处理应(    )', '[{"key":"A","text":"凭主观判断"},{"key":"B","text":"用真实数据客观分析"},{"key":"C","text":"随意增减数据"},{"key":"D","text":"只选有利数据"}]', 'B', '建立在真实数据基础上。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '调查报告核心是用事实和数据说话。', NULL, 'T', '必须客观、真实、科学。', 1, 1);

-- ============================================================================
-- 六、话题作文 (10 nodes x 2 = 20 questions)
-- ============================================================================

-- ==================================================================
-- 审题与立意
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='审题与立意' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '话题作文审题首先要(    )', '[{"key":"A","text":"直接开始写"},{"key":"B","text":"圈画关键词，明确范围和限定"},{"key":"C","text":"查找名言警句"},{"key":"D","text":"写出大纲"}]', 'B', '审题第一步是圈画关键词。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '话题作文立意越新奇越好，不需考虑扣题。', NULL, 'F', '立意首先要"准确"再求"新颖"。', 2, 1);

-- ==================================================================
-- 材料作文的阅读与分析
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='材料作文的阅读与分析' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '材料作文对材料的正确做法是(    )', '[{"key":"A","text":"只看第一句"},{"key":"B","text":"细读材料提取核心信息"},{"key":"C","text":"材料不重要自由发挥"},{"key":"D","text":"照抄材料"}]', 'B', '材料是立意的依据。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '材料作文可以完全脱离材料。', NULL, 'F', '必须结合材料。', 1, 1);

-- ==================================================================
-- 议论文写作结构（引论-本论-结论）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='议论文写作结构（引论-本论-结论）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"本论"的主要作用是(    )', '[{"key":"A","text":"提出中心论点"},{"key":"B","text":"总结升华"},{"key":"C","text":"用论据论证中心论点"},{"key":"D","text":"引出话题"}]', 'C', '本论是议论文核心部分。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '本论可采用并列式、递进式或对照式。', NULL, 'T', '三种常见结构。', 2, 1);

-- ==================================================================
-- 议论文论点与论据
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='议论文论点与论据' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '属于道理论据的是(    )', '[{"key":"A","text":"司马迁发愤著《史记》"},{"key":"B","text":"袁隆平培育杂交水稻"},{"key":"C","text":""天行健，君子以自强不息"——《周易》"},{"key":"D","text":"张桂梅创办女子高中"}]', 'C', '引用经典名言是道理论据。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '议论文论点要鲜明，不能模棱两可。', NULL, 'T', '必须态度明确。', 1, 1);

-- ==================================================================
-- 议论文论证结构
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='议论文论证结构' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"总分总"中总说部分作用是(    )', '[{"key":"A","text":"列举事例"},{"key":"B","text":"提出中心论点或总起"},{"key":"C","text":"正反对比"},{"key":"D","text":"回应疑问"}]', 'B', '总说提出中心论点。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '递进式要求各分论点层层深入。', NULL, 'T', '从表及里、层层推进。', 2, 1);

-- ==================================================================
-- 开头与结尾技巧
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='开头与结尾技巧' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"人生如登山"开头的技巧是(    )', '[{"key":"A","text":"名言引入法"},{"key":"B","text":"设问引入法"},{"key":"C","text":"比喻引入法"},{"key":"D","text":"故事引入法"}]', 'C', '用比喻引出话题。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '结尾应简短有力，可首尾呼应。', NULL, 'T', '正确。', 1, 1);

-- ==================================================================
-- 记叙文写作要素与方法
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='记叙文写作要素与方法' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '记叙文六要素是(    )', '[{"key":"A","text":"时间、地点、人物、起因、经过、结果"},{"key":"B","text":"论点、论据、论证"},{"key":"C","text":"开头、发展、高潮、结局"},{"key":"D","text":"主语、谓语、宾语"}]', 'A', '六要素是叙事完整性的基本要求。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '记叙文只有顺叙一种方式。', NULL, 'F', '还有倒叙、插叙等。', 1, 1);

-- ==================================================================
-- 作文语言提升（句式变化/修辞润色）
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='作文语言提升（句式变化/修辞润色）' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '"坚持如一座灯塔"使用的修辞是(    )', '[{"key":"A","text":"拟人"},{"key":"B","text":"比喻"},{"key":"C","text":"排比"},{"key":"D","text":"借代"}]', 'B', '将"坚持"比作"灯塔"，用"如"是明喻。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '多用华丽辞藻就是语言好，不需考虑内容。', NULL, 'F', '语言表达要服务于内容。', 2, 1);

-- ==================================================================
-- 写作素材积累与运用
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='写作素材积累与运用' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '运用素材的正确方式是(    )', '[{"key":"A","text":"详细叙述"},{"key":"B","text":"简洁概述素材，分析如何证明论点"},{"key":"C","text":"只罗列不加分析"},{"key":"D","text":"随意编造"}]', 'B', '素材要"概述+分析"。', 2, 1),
('语文[职高]', @n, 'TRUE_FALSE', '素材越新奇越好，课本经典素材已过时。', NULL, 'F', '素材关键在于"适合"。', 1, 1);

-- ==================================================================
-- 卷面与书写规范
-- ==================================================================
SET @n = (SELECT id FROM knowledge_nodes WHERE subject_id=20 AND level=4 AND name='卷面与书写规范' LIMIT 1);
INSERT IGNORE INTO question_bank (subject, category_id, question_type, question_text, options, correct_answer, explanation, difficulty_level, status) VALUES
('语文[职高]', @n, 'SINGLE_CHOICE', '考场作文字迹要求(    )', '[{"key":"A","text":"必须写书法"},{"key":"B","text":"清晰工整、大小适中、行列整齐"},{"key":"C","text":"越快越好"},{"key":"D","text":"字越小越好"}]', 'B', '字迹清晰可辨最重要。', 1, 1),
('语文[职高]', @n, 'TRUE_FALSE', '作文写错字可用涂改液修改。', NULL, 'F', '考场禁止使用涂改液。', 2, 1);

-- ============================================================================
-- 完成
-- ============================================================================
COMMIT;

SELECT CONCAT('V222-P3: 语文[职高] 题库写入完成！') AS result;