-- ============================================================================
-- v187: 语文[职高] 自测题补全 Batch2 (95节点)
-- 生成方式: 按节点内容专属出题 · CONCAT追加 · 真实答案+解析
-- 幂等: UPDATE可重复执行(跳过已有完整答案的节点)
-- ============================================================================
SET @s = 20;

-- ═══════════════════════════════════════════════════════════
-- 字音字形 (8)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列词语中加点字读音全部正确的一项是（　　）**\nA. 参差(cān chā) ／ 深恶痛绝(è)\nB. 载歌载舞(zài) ／ 着手(zhuó)\nC. 氛围(fèn) ／ 呱呱坠地(guā)\nD. 莘莘学子(xīn) ／ 酗酒(xiōng)\n答案：B  解析：A参差cēn cī／恶wù；C氛fēn／呱gū；D莘shēn／酗xù。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '多音字辨析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"己""已""巳"三个字的正确区分是（　　）**\nA. 己(jǐ)出头、已(yǐ)半封、巳(sì)全封\nB. 己(jǐ)半封、已(yǐ)全封、巳(sì)出头\nC. 己(jǐ)全封、已(yǐ)出头、巳(sì)半封\nD. 三个字字形完全相同\n答案：A  解析：口诀"己开已半巳全封"——己完全开口，已半封闭，巳全封闭。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '形近字辨析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列加点字读音全部正确的一项是（　　）**\nA. 气氛(fèn) ／ 比较(jiǎo)\nB. 召开(zhāo) ／ 潜力(qiǎn)\nC. 肖像(xiào) ／ 档次(dàng)\nD. 办公室(shǐ) ／ 酗酒(xiōng)\n答案：C  解析：A氛fēn／较jiào；B召zhào／潜qián；D室shì／酗xù。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '易读错字' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列词语中没有错别字的一项是（　　）**\nA. 再接再励\nB. 穿流不息\nC. 一筹莫展\nD. 九宵云外\n答案：C  解析：A应为"再接再厉"(厉通砺)；B应为"川流不息"(川=河流)；D应为"九霄云外"(霄从雨)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '易写错字' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"的""地""得"三个字的正确用法是（　　）**\nA. "的"用于状语前、"地"用于定语前、"得"用于补语前\nB. "的"用于定语前、"地"用于状语前、"得"用于补语前\nC. 三个字可以随意互换\nD. "的"和"地"可以互换，"得"单独使用\n答案：B  解析："的"是定语的标志(美丽的校园)，"地"是状语的标志(慢慢地走)，"得"是补语的标志(跑得快)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '同音字辨析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列拼音书写规范正确的一项是（　　）**\nA. 西安(Xīān)\nB. 花儿(huāer)\nC. 秀丽(xiòu)\nD. 天安门(Tiān''ānmén)\n答案：D  解析：A应为Xī''ān(隔音符号)；B应为huār(儿化音)；C应为xiù(iu并列标在后)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '拼音拼写规则' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列汉字中属于形声字的是（　　）**\nA. 日（象形）\nB. 上（指事）\nC. 江（氵形旁+工声旁）\nD. 休（会意）\n答案：C  解析：形声字由形旁(表意)和声旁(表音)组成，"江"的氵是形旁、工是声旁。汉字80%以上是形声字。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '汉字结构知识' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**四川方言中最常见的语音问题是（　　）**\nA. 把"知识(zhīshi)"读成"zīsi"（平翘舌不分）\nB. 把"北京(běijīng)"读成"běijīn"（前后鼻音不分）\nC. 把"黄(huáng)"读成"fáng"（h/f不分）\nD. 以上都是\n答案：D  解析：四川方言三大典型问题：平翘舌不分、鼻边音不分(n/l)、前后鼻音不分、部分地区h/f不分。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '四川方言辨正' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 词语运用 (5)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中加点词语使用最恰当的一项是（　　）**\nA. 经过努力，他的成绩有了明显增长\nB. 我们要养成讲卫生的习惯\nC. 北京的秋天是最美的季节\nD. 成败取决于是否努力\n答案：B  解析：A"增长"应为"提高"；C主宾搭配不当；D"成败"是两面，"是否努力"也是两面，正确但B最简单直接。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '近义词辨析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列成语使用正确的一项是（　　）**\nA. 他的表演差强人意，让观众大失所望\nB. 这对夫妻相敬如宾，感情很好\nC. 我们学校的建筑美轮美奂\nD. 我必定鼎力相助\n答案：B  解析：A"差强人意"是大体使人满意，与"大失所望"矛盾；C"美轮美奂"仅用于建筑物；D"鼎力相助"是敬辞，用于请对方帮助。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '成语使用正误' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列关联词语使用正确的一项是（　　）**\nA. 他不但学习好，而且他妹妹学习也好\nB. 虽然天气不好，所以运动会取消了\nC. 无论天气不好，他都会准时到校\nD. 既然来了，就安心坐下吧\n答案：D  解析：A主语不同时"不但"应放主语前；B"虽然"应配"但是"；C"无论"后面需要并列成分(如"无论刮风下雨")。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '关联词语搭配' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列词语中感情色彩与其他三项不同的是（　　）**\nA. 成果\nB. 后果\nC. 怂恿\nD. 顽固\n答案：A  解析：A"成果"是褒义词；B"后果"、C"怂恿"、D"顽固"都是贬义词。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '词语的感情色彩与语体色彩' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列歇后语的"谜底"正确的是（　　）**\nA. 外甥打灯笼——照旧(舅)\nB. 猪八戒照镜子——自找难看\nC. 孔夫子搬家——全是书\nD. 泥菩萨过江——自身强壮\n答案：A  解析：A是谐音类歇后语；B应为"里外不是人"；C应为"净是书(输)"；D应为"自身难保"。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '常用熟语与惯用语' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 病句辨析 (6)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中有语病的一项是（　　）**\nA. 他是学校里有名的优秀教师\nB. 一位优秀的有着20年经验的女教师走了进来\nC. 教室里坐满了学生\nD. 这本书我读了三遍\n答案：B  解析：多层定语次序不当，正确顺序应为"一位有着20年经验的女优秀教师"(领属→数量→性质→属性)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '语序不当' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中"搭配不当"的是（　　）**\nA. 我们要提高学习效率\nB. 成功取决于是否努力\nC. 他的写作水平有了明显增长\nD. 她穿着一件红色连衣裙\n答案：C  解析：水平不能说"增长"，应说"提高"。搭配不当是最常见的病句类型。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '搭配不当' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子没有语病的一项是（　　）**\nA. 通过这次学习，使我提高了认识\nB. 我们要积极响应\nC. 大家共同努力，终于完成了任务\nD. 在大家的努力下，使得问题得到解决\n答案：C  解析：A"通过"和"使"并用导致缺主语；B缺宾语(应加"学校的号召")；D"使得"多余导致缺主语。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '成分残缺或赘余' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子有语病的一项是（　　）**\nA. 本着节约为原则，我们减少了开支\nB. 本着节约的原则，我们减少了开支\nC. 以节约为原则，我们减少了开支\nD. 因为要节约，我们减少了开支\n答案：A  解析："本着……为原则"是句式杂糅，应改为"本着……的原则"或"以……为原则"。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '结构混乱' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中有歧义的一项是（　　）**\nA. 他走了一个多小时了\nB. 教室里安静极了\nC. 她昨天去了北京\nD. 明天下午开会\n答案：A  解析："走"可以是"行走"(散步走了一个多小时)或"离开"(离开了一个多小时)，有两种理解。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '表意不明（歧义句）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子有语病的一项是（　　）**\nA. 他是众多参赛者中获胜的唯一一人\nB. 为了防止安全事故再次发生，学校加强了管理\nC. 约三千人左右参加了大会\nD. 以上句子都有语病\n答案：D  解析：A"众多"与"唯一"矛盾；B"防止"已含否定，"防止……不再"双重否定变肯定；C"约"与"左右"重复。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '不合逻辑' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 标点符号 (9)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中标点使用正确的一项是（　　）**\nA. 他买了苹果、香蕉、和橘子\nB. 我们要不断学习、不断进步\nC. 这所学校有计算机、会计、旅游三个专业\nD. 这个班有七、八个人\n答案：C  解析：A顿号与"和"重复；B并列谓语间应用逗号；D概数间不用顿号。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '顿号与逗号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中标点使用正确的一项是（　　）**\nA. 春天花开了；夏天草绿了\nB. 他不仅学习刻苦，成绩优异；而且还乐于助人，深受同学喜爱\nC. 我喜欢吃苹果；梨；香蕉\nD. 因为下雨了；所以我不去了\n答案：B  解析：A简单并列分句用逗号即可；C并列词语间用顿号；D因果复句用逗号。分号用于分句内部已用了逗号的情况。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '分号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中问号使用正确的是（　　）**\nA. 你是学计算机专业？还是学会计专业？\nB. 难道你不知道吗。\nC. 他问我去不去看电影？\nD. 这是什么东西？\n答案：D  解析：A选择问句只在句末用一个问号；B反问句应用问号；C是陈述句不是疑问句，应用句号。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '问号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中引号使用正确的一项是（　　）**\nA. 鲁迅的"呐喊"收录了多篇名作\nB. 他说："我很开心"。\nC. 这次"学雷锋"活动很有意义\nD. 老师说："今天我们来学习《师说》。"\n答案：D  解析：A书名应用书名号《》；B句号应在引号内；C活动名称不应用引号，引号即可但不如D规范。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '引号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**省略号的正确写法是（　　）**\nA. 三个圆点…\nB. 六个圆点……（占两格）\nC. 任意数量的点\nD. 十二个圆点\n答案：B  解析：省略号的标准写法是六个圆点(……)，占两个字的位置。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '省略号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中破折号使用正确的是（　　）**\nA. 我的母校——成都七中——是一所百年名校\nB. 他跑过来-对我说\nC. 今天天气真好——\nD. 北京——中国的首都\n答案：D  解析：破折号(——)占两格，用于解释说明、话题转换、声音延长。A中用了两个破折号表示插入语，也是正确用法，但D是最典型的用法。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '破折号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列书名号使用正确的一项是（　　）**\nA. 学校组织了一次《学雷锋》活动\nB. 鲁迅的《呐喊》收录了《狂人日记》等名篇\nC. 他在《中国青年报》上发表了《青春》一文\nD. 本学期开设《语文》课程\n答案：B  解析：A活动名称不用书名号；C报刊名用书名号，但篇名也用书名号正确；D课程名称可用可不用。B是最规范用法。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '书名号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列标点使用正确的一项是（　　）**\nA. 他的电话是010—12345678\nB. 李白(701—762)\nC. 间隔号"·"用于外国人名中文译名的名姓之间\nD. 以上都对\n答案：D  解析：连接号(—)用于连接数字；间隔号(·)用于外国人名(卡尔·马克思)和书名分界。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '连接号与间隔号' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中标点符号使用全部正确的一项是（　　）**\nA. 老师问："你们读过《红楼梦》吗？"\nB. 他喜欢打球、游泳、和跑步\nC. 这个人是谁？我不认识。\nD. "你好，"他说："很高兴见到你。"\n答案：A  解析：B顿号和"和"重复；C"这个人是谁"不是疑问句，用逗号；D"他说"在引语中间时后面用逗号不用冒号。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '标点符号综合辨析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 修辞手法辨析 (9)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"月光如流水一般，静静地泻在这一片叶子和花上"使用的修辞手法是（　　）**\nA. 拟人\nB. 借代\nC. 比喻（明喻）\nD. 夸张\n答案：C  解析：有本体(月光)、喻体(流水)、比喻词(如)，是典型的明喻。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '比喻' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"太阳露出了笑脸"使用的修辞手法是（　　）**\nA. 比喻\nB. 拟人\nC. 借代\nD. 夸张\n答案：B  解析：赋予太阳"露出笑脸"这一人的特征，是拟人手法。拟人与比喻的区别：拟人把物当人写，比喻是用一物比另一物。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '比拟（拟人+拟物）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"红领巾"代指少先队员，使用的修辞手法是（　　）**\nA. 比喻\nB. 借代\nC. 拟人\nD. 夸张\n答案：B  解析：借代基于相关性(红领巾是少先队员的标志物)，借喻基于相似性。借代不能改为明喻(不能说"少先队员像红领巾一样")。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '借代' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"飞流直下三千尺，疑是银河落九天"使用的修辞手法是（　　）**\nA. 比喻\nB. 拟人\nC. 夸张\nD. 对偶\n答案：C  解析："三千尺"是扩大夸张，极言瀑布之高。夸张可分为扩大夸张、缩小夸张和超前夸张。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '夸张' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"海内存知己，天涯若比邻"使用的修辞手法是（　　）**\nA. 排比\nB. 对偶\nC. 反复\nD. 设问\n答案：B  解析：上下两句字数相等、结构相同、意义相关，是典型的对偶(正对)。对偶只有两句，排比需要三句及以上。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '对偶' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中使用排比修辞的是（　　）**\nA. 两个黄鹂鸣翠柳，一行白鹭上青天\nB. 山朗润起来了，水涨起来了，太阳的脸红起来了\nC. 横眉冷对千夫指，俯首甘为孺子牛\nD. 大江东去，浪淘尽\n答案：B  解析：B有三个结构相似的短语，是排比。A是对偶(两句)，C是对偶(反对)，D没有排比。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '排比' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"难道你就觉得它只是树？"属于哪种修辞（　　）**\nA. 设问\nB. 反问\nC. 疑问\nD. 借问\n答案：B  解析：反问的特征是无疑而问，答案在问句的反面——用否定形式表达肯定意思("它不只是树")。反问不需要回答。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '反问' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"什么是幸福？幸福是一种内心的感受。"使用的修辞是（　　）**\nA. 反问\nB. 设问\nC. 疑问\nD. 排比\n答案：B  解析：设问的特征是自问自答，目的是引起注意和思考。反问不需要回答(答案在问句中)，设问需要自己回答。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '设问' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"红领巾"是借代，"像红苹果一样的脸蛋"是借喻，两者的根本区别是（　　）**\nA. 借代有比喻词，借喻没有\nB. 借代基于相似性，借喻基于相关性\nC. 借代基于相关性(不能改为明喻)，借喻基于相似性(可改为明喻)\nD. 两者没有区别\n答案：C  解析：判断方法——试着把该词换成"像……"的比喻句，能换的是借喻，不能换的是借代。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '易混修辞辨析（借代vs借喻/比喻vs比拟/对偶vs排比/设问vs反问）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 社科类文本阅读 (6)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**《师说》中"孔子师郯子、苌弘、师襄、老聃"运用了什么论证方法（　　）**\nA. 比喻论证\nB. 道理论证\nC. 举例论证\nD. 对比论证\n答案：C  解析：用"孔子向多位老师学习"这一具体事例来论证"圣人无常师"的观点，是典型的举例论证。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '论述文阅读（论点/论据/论证）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"这座桥全长50.82米，宽9.6米"运用的说明方法是（　　）**\nA. 举例子\nB. 列数字\nC. 打比方\nD. 分类别\n答案：B  解析：用具体数据说明事物特征，是列数字的说明方法，作用是准确、有说服力。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '说明文阅读（说明方法/说明顺序）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**在做信息筛选题时，下列做法正确的是（　　）**\nA. 凭自己的知识储备判断\nB. 看到原文出现过的词语就直接选\nC. 先读题干→定位原文→逐词比对选项与原文\nD. 选"差不多"的选项即可\n答案：C  解析：信息筛选题的关键是"于文有据"，必须回到原文逐词比对。常见陷阱包括偷换概念、以偏概全、混淆时态。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '信息筛选与整合' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**原文说"节能技术目前仍有局限"，下列推断合理的是（　　）**\nA. 节能技术未来一定不会有突破\nB. 未来可能继续研发节能技术\nC. 所有节能技术都不可靠\nD. 只有中国的节能技术有局限\n答案：B  解析：A过度推断(原文只说"目前有局限"未说未来)；C以偏概全(原文说"有局限"不是说"都不可靠")；D无中生有。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '分析推理与判断' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**议论文中，开头提出论点、中间展开论证、结尾总结升华，这种结构是（　　）**\nA. 并列式\nB. 递进式\nC. 总分总式\nD. 对照式\n答案：C  解析：总(开头)→分(主体)→总(结尾)是最常见的议论文结构。论证结构还有并列式、递进式、对照式。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '文章结构与论证逻辑' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**理解文中重要句子的含义时，正确的做法是（　　）**\nA. 只看这个句子本身\nB. 结合上下文语境，分析表层和深层含义\nC. 用自己的生活经验代替理解\nD. 只关注句子的修辞手法\n答案：B  解析：含义理解需要联系上下文，既要把握字面意思(表层)，也要挖掘深层含义(象征/隐喻/作者态度)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '重要概念与关键句理解' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 文学作品阅读 (7)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**小说三要素不包括下列哪一项（　　）**\nA. 人物\nB. 情节\nC. 论点\nD. 环境\n答案：C  解析：小说三要素是人物、情节、环境。"论点"是议论文的要素。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '小说阅读（人物·情节·环境）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**散文"形散神不散"中"神"指的是（　　）**\nA. 神灵\nB. 作者的外貌\nC. 贯穿全文的中心思想和情感主线\nD. 散文的字数\n答案：C  解析：散文的"形"是自由灵活的形式和材料，"神"是统一的主题和情感。"形散"是手段，"神聚"是目的。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '散文阅读（形散神聚·情景交融）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**《白杨礼赞》借白杨树歌颂北方农民，这种手法是（　　）**\nA. 对比\nB. 象征\nC. 夸张\nD. 反衬\n答案：B  解析：象征是用具体形象暗示抽象概念。"白杨"象征坚韧不屈的精神。象征与比喻的区别：象征是表现手法(篇章级)，比喻是修辞手法(语句级)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '表现手法辨析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"月亮圆得让人想家"运用了什么表达方式（　　）**\nA. 记叙\nB. 描写+抒情\nC. 说明\nD. 议论\n答案：B  解析："月亮圆"是描写，"让人想家"是抒情，描写和抒情结合是散文中常见的表达方式。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '表达方式及其作用' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**分析人物形象时，下列做法正确的是（　　）**\nA. 只贴标签如"善良、勇敢"即可\nB. 给出特征+引用原文具体描写作为依据\nC. 只看外貌描写忽略语言动作\nD. 只分析主角不关注次要人物\n答案：B  解析：人物形象分析必须"有理有据"——先概括性格特征，再引用文中的具体描写(外貌/语言/动作/心理)作为支撑。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '人物形象分析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**赏析"月光如流水一般，静静地泻在这一片叶子和花上"中的"泻"字，最恰当的分析是（　　）**\nA. 写出了月光很亮\nB. "泻"字比"照"更精妙，既有流水般的动态感又有月光洒落的轻柔感\nC. 用了夸张手法\nD. 说明月亮很大\n答案：B  解析：语言品味要具体分析词语的表达效果——"泻"字将静态的月光写活了，化静为动，体现语言的精妙。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '语言品味与赏析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**概括文章主题时，正确的做法是（　　）**\nA. 只说"文章写了什么故事"\n回答：B. 说明作者通过叙述什么、表达了什么思想情感\nC. 照抄原文结尾段\nD. 只写自己的读后感\n答案：B  解析：主题概括的答题结构是"本文通过叙述(描写)……，表达了/表现了/批判了……"。A只写了内容，C不动脑，D脱离文本。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '主题理解与概括' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 常见文言实词虚词 (9)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"故"在下列句子中表示"所以"意思的是（　　）**\nA. 温故而知新\nB. 故人西辞黄鹤楼\nC. 故天将降大任于是人也\nD. 此病故不愈\n答案：C  解析：A旧的知识；B老朋友；C所以；D本来。一词多义需要结合语境判断。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '一词多义' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"妻子"在古汉语中的意思是（　　）**\nA. 配偶(仅指妻)\nB. 妻子和儿女\nC. 儿子的妻子\nD. 女性的朋友\n答案：B  解析：古汉语中"妻子"是两个词——"妻"(妻子)和"子"(儿女)。今义缩小为仅指配偶。这是典型的古今异义。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '古今异义' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"沛公军霸上"中"军"的用法是（　　）**\nA. 名词作动词(驻军)\nB. 名词作状语\nC. 形容词作名词\nD. 使动用法\n答案：A  解析："军"本是名词"军队"，在此处活用为动词"驻军"。名词作动词是词类活用中最常见的类型。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '词类活用' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"说"通"悦"属于哪种文言现象（　　）**\nA. 古今异义\nB. 一词多义\nC. 通假字\nD. 词类活用\n答案：C  解析：通假字是本有其字而不用，用音同或音近的字代替。"学而时习之，不亦说乎"的"说"通"悦"(高兴)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '通假字' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"学而时习之"中"之"的用法是（　　）**\nA. 助词"的"\nB. 代词(代学过的知识)\nC. 动词"到"\nD. 取消句子独立性\n答案：B  解析："之"作代词，代指前面提到的"学过的知识"。"之"有代词、助词(的)、动词(到)、取消独立性四种主要用法。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '常见虚词（之·其·以·而）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"青，取之于蓝，而青于蓝"中两个"于"的意思分别是（　　）**\nA. 从／比\nB. 在／对于\nC. 比／从\nD. 被／在\n答案：A  解析：第一个"于"表"从"(从蓝草中提取)，第二个"于"表"比"(比蓝草更青)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '常见虚词（于·为·乃·则）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列句子中属于判断句的是（　　）**\nA. 战于长勺\nB. 陈胜者，阳城人也\nC. 何陋之有\nD. 吾属今为之虏矣\n答案：B  解析："……者，……也"是判断句的标志。A是状语后置，C是宾语前置(倒装句)，D是被动句。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '文言句式（判断句·被动句）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"何陋之有"的正确翻译是（　　）**\nA. 有什么简陋的呢\nB. 什么简陋的有\nC. 哪里有简陋\nD. 简陋有什么\n答案：A  解析："何陋之有"是宾语前置句(倒装句)，正常语序为"有何陋"。翻译时必须还原为现代汉语语序。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '文言句式（倒装句·省略句）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**考纲要求掌握的常见文言虚词有多少个（　　）**\nA. 12个\nB. 15个\nC. 18个\nD. 20个\n答案：C  解析：四川省对口高考要求掌握18个常见文言虚词：而、何、乎、乃、其、且、若、所、为、焉、也、以、因、于、与、则、者、之。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '常见文言虚词（18个）：而何乎乃其且若所为焉也以因于与则者之' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 文言文翻译与理解 (7)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**文言翻译的首要原则是（　　）**\nA. 意译为主，直译为辅\nB. 直译为主，意译为辅\nC. 只直译不意译\nD. 随意翻译即可\n答案：B  解析：直译为主(逐词对应)、意译为辅(直译不通时用意译)，做到信(准确)达(通顺)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '翻译原则与步骤' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**翻译"沛公安在"时，正确的处理步骤是（　　）**\nA. 直接译为"沛公在哪里"，无需调整\nB. 先识别为宾语前置(安在=在安)→调整为正常语序→翻译\nC. 把"安"删掉不译\nD. 保留"安在"的语序不调整\n答案：B  解析：翻译技巧五字诀中，"调"用于倒装句——先识别倒装类型，还原为正常语序后再翻译。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '翻译技巧（留·补·换·调·删）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**概括《劝学》的中心思想，最准确的是（　　）**\nA. 学习很重要所以我们要学习\nB. 文章论证了学习的意义、作用和方法，勉励人们坚持学习\nC. 荀子批评了不好好学习的人\nD. 学习可以改变人的性格\n答案：B  解析：文意理解要从文章整体把握——《劝学》从学习的意义(改变自己)、作用(弥补不足)、方法(积累坚持)三个层面论证，主题是"学不可以已"。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '文意理解与概括' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**《廉颇蔺相如列传》中"先国家之急而后私仇"体现了蔺相如什么品质（　　）**\nA. 胆小怕事\nB. 顾全大局、以国为重\nC. 追求个人利益\nD. 争强好胜\n答案：B  解析：蔺相如面对廉颇的挑衅选择退让，是因为他把国家利益放在个人恩怨之上。这句话是全文的主题句。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '《廉颇蔺相如列传》精读' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**《子路、曾皙、冉有、公西华侍坐》中，孔子最赞赏的弟子是（　　）**\nA. 子路(治国强兵)\nB. 冉有(使民富足)\nC. 公西华(做司仪)\nD. 曾皙(暮春咏归)\n答案：D  解析：孔子听罢曾皙的回答感叹"吾与点也"(我赞同曾皙啊)，因为曾皙描绘的礼乐治国的理想画面最符合孔子的政治理想。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '《子路、曾皙、冉有、公西华侍坐》精读' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**《劝学》中"青，取之于蓝，而青于蓝"用来比喻什么（　　）**\nA. 颜色很好看\nB. 学习可以超越原有的水平\nC. 蓝草比青色好看\nD. 染料制作的工艺\n答案：B  解析：荀子用这个比喻论证学习的意义——通过学习，学生可以超越老师，后人可以超越前人。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '《劝学》精读' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**《师说》的中心论点是（　　）**\nA. 三人行必有我师\nB. 古之学者必有师\nC. 弟子不必不如师\nD. 道之所存，师之所存也\n答案：B  解析："古之学者必有师"是《师说》开篇提出的中心论点。C和D是分论点，用来支撑中心论点。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '《师说》精读' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 古诗词鉴赏 (7)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**《诗经》中"关关雎鸠，在河之洲"运用的表现手法是（　　）**\nA. 赋\nB. 比\nC. 兴\nD. 赋和兴\n答案：C  解析："兴"是先言他物以引起所咏之词——以雎鸠鸟的和鸣声起兴，引出君子对淑女的思念。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '《诗经》选篇：关雎、蒹葭' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**李白《将进酒》"天生我材必有用"表达的情感是（　　）**\nA. 消极悲观\nB. 怀才不遇后的自信与豪放\nC. 对现实的不满\nD. 对朋友的劝诫\n答案：B  解析：李白的豪放诗风在此句体现得淋漓尽致——虽怀才不遇，仍自信满满。整首诗的情感基调是豪放中带愤懑。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '唐诗鉴赏：将进酒、茅屋为秋风所破歌' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**苏轼与柳永的词风分别属于（　　）**\nA. 都是豪放派\nB. 都是婉约派\nC. 苏轼豪放派、柳永婉约派\nD. 苏轼婉约派、柳永豪放派\n答案：C  解析：苏轼《念奴娇》"大江东去"是豪放派代表，柳永《雨霖铃》"杨柳岸晓风残月"是婉约派代表。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '宋词鉴赏：念奴娇·赤壁怀古、雨霖铃' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列意象中，通常用来表达"思乡"之情的是（　　）**\nA. 月\nB. 剑\nC. 红日\nD. 东风\n答案：A  解析：常见意象的含义——月(思乡)、柳(送别)、酒(愁绪/豪情)、雁(思乡/书信)。诗歌意象是理解情感的重要钥匙。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '诗歌意象与意境分析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**"借景抒情"和"托物言志"的区别是（　　）**\nA. 没有区别\nB. 借景抒情侧重表达情感，托物言志侧重表达志向\nC. 借景抒情只能写诗，托物言志只能写散文\nD. 借景抒情是修辞，托物言志是文体\n答案：B  解析：借景抒情(如《荷塘月色》)重在抒发情感，托物言志(如《白杨礼赞》)重在表达志向品格。两者都是常见的诗歌表达技巧。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '诗歌表达技巧（抒情方式/描写手法/修辞）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**杜甫《茅屋为秋风所破歌》表达的核心情感是（　　）**\nA. 对自家房屋损坏的抱怨\nB. 推己及人、忧国忧民的仁者情怀\nC. 对秋天的不喜欢\nD. 对个人怀才不遇的感慨\n答案：B  解析："安得广厦千万间，大庇天下寒士俱欢颜"——杜甫由自家茅屋漏雨联想到天下寒士的困境，体现了推己及人的仁者胸怀。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '诗歌思想情感与观点态度' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**古诗中"春风又绿江南岸"的"绿"字被称为"诗眼"，其妙处在于（　　）**\nA. 用了绿色这个颜色\nB. 形容词用作动词，化静为动，写出春天到来的过程\nC. 绿色比别的颜色好看\nD. 只是为了押韵\n答案：B  解析：炼字(推敲字词)是诗歌语言赏析的核心——"绿"字由形容词转为动词，把春风"使江南变绿"的动态过程写活了。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '诗歌语言赏析（炼字/诗眼/风格）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 应用文写作 (12)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列哪一项不属于通知的必备要素（　　）**\nA. 标题\nB. 称呼\nC. 祝颂语\nD. 落款和日期\n答案：C  解析：通知的格式要素是标题+称呼+正文+落款+日期。祝颂语是书信的要素，通知不需要。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '通知' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**寻物启事和招领启事的主要区别是（　　）**\nA. 完全没有区别\nB. 招领启事不写物品的具体特征(防冒领)，寻物启事要写详细特征\nC. 寻物启事不需要联系方式\nD. 招领启事要写物品的详细特征\n答案：B  解析：招领启事要防止冒领，所以只写拾到的时间和地点，不写具体特征。寻物启事则要尽量写详细以便辨认。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '启事' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**书信格式中"此致""敬礼"的正确书写位置是（　　）**\nA. "此致"顶格，"敬礼"空两格\nB. "此致"空两格，"敬礼"顶格\nC. 都顶格\nD. 都空两格\n答案：B  解析："此致"在正文后另起一行空两格，"敬礼"另起一行顶格写。这是书信格式的标准要求。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '书信' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**请假条的核心要素不包括（　　）**\nA. 称呼\nB. 请假原因和时间\nC. 祝颂语(此致敬礼)\nD. 署名和日期\n答案：C  解析：便条(请假条/留言条/托事条)比书信简略，不需要完整的祝颂语，只需"恳请批准"等简短用语即可。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '便条（请假条/留言条/托事条）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**借条中金额的正确写法是（　　）**\nA. 只用阿拉伯数字写\nB. 阿拉伯数字+大写数字(壹贰叁…)都要写\nC. 只写大写数字\nD. 口头约定即可\n答案：B  解析：单据的金额必须阿拉伯数字和大写数字(壹贰叁肆伍陆柒捌玖拾佰仟萬)同时写，防止篡改。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '单据（借条/收条/领条/欠条）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**计划的五个核心要素是（　　）**\nA. 称呼、正文、祝颂语、署名、日期\nB. 标题、前言、目标、措施步骤、署名日期\nC. 论点、论据、论证、结论、建议\nD. 时间、地点、人物、事件、结果\n答案：B  解析：计划的结构要素是标题+前言(背景目的)+目标(量化可考核)+措施与步骤(具体可操作)+署名和日期。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '计划' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**总结与计划的区别是（　　）**\nA. 没有区别\nB. 计划是事前规划(做什么)，总结是事后回顾(做得怎样)\nC. 总结是事前规划，计划是事后回顾\nD. 计划比总结更重要\n答案：B  解析：计划是"事前"对未来工作的安排，总结是"事后"对已完成工作的回顾与分析。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '总结' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**产品说明书语言的核心要求是（　　）**\nA. 华丽优美\nB. 准确科学、条理清晰、通俗易懂\nC. 充满感情色彩\nD. 多用修辞手法\n答案：B  解析：说明书的目的是让用户正确使用产品，语言必须准确(不含糊)、条理(按操作顺序)、通俗(避免专业术语)。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '说明书' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**会议记录的必备要素包括（　　）**\nA. 会议名称、时间、地点、参会人、会议内容、决议\nB. 称呼、正文、祝颂语\nC. 论点、论据、论证\nD. 标题、前言、主体、结尾\n答案：A  解析：会议记录需要"记实、记准、记全"——时间、地点、出席人、主持人、记录人、会议内容、讨论发言、决议事项缺一不可。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '会议记录' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**求职信的正文四段式不包括（　　）**\nA. 表明求职意向和消息来源\nB. 自我介绍和能力展示\nC. 表达诚意和请求面试\nD. 批评目标公司的不足\n答案：D  解析：求职信正文应包含：求职意向+自我介绍+表达诚意+请求面试。求职信的语气要礼貌得体、自信但不自夸。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '求职信' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**求职信与应聘书的主要区别是（　　）**\nA. 完全没有区别\nB. 求职信是广谱投递(主动询问)，应聘书是针对具体招聘逐条回应\nC. 求职信更长\nD. 应聘书不需要自我介绍\n答案：B  解析：求职信是"请问你们需要人吗"，应聘书是"我符合你们的要求，我来应聘"——后者要逐条对照招聘要求说明自己的匹配度。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '应聘书' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**调查报告的核心特征是（　　）**\nA. 用华丽的语言表达观点\nB. 用数据说话，基于调查得出结论\nC. 只写结论不写过程\nD. 与一般的议论文完全相同\n答案：B  解析：调查报告的结构是标题+前言(调查背景)+正文(数据和发现)+结论与建议。"用数据说话"是其最显著的特征。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '调查报告' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

-- ═══════════════════════════════════════════════════════════
-- 话题作文 (12) — 排除已处理的审题与立意、材料作文(见下方注)
-- ═══════════════════════════════════════════════════════════
UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**话题作文审题时最关键的步骤是（　　）**\nA. 看字数要求即可\nB. 圈画关键词，明确话题范围和写作方向\nC. 只看标题不看材料\nD. 随便选一个方向就写\n答案：B  解析：审题四看——看要求(字数/文体)、看文体、看范围("我的"不能写别人的事)、看关键词。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '审题与立意' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**面对材料作文时，正确的处理步骤是（　　）**\nA. 读完材料直接开始写\nB. 提取材料关键信息→确定写作角度→确立中心观点→拟提纲→写作\nC. 忽略材料，自己随便写\nD. 只摘抄材料作为文章内容\n答案：B  解析：材料作文需要先读懂材料，抓住材料的关键信息和倾向性，再确定自己的写作角度和中心论点。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '材料作文的阅读与分析' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**议论文的基本结构是（　　）**\nA. 记叙文六要素\nB. 引论(开头)→本论(主体)→结论(结尾)\nC. 标题→称呼→正文→落款\nD. 调查→数据→结论\n答案：B  解析：引论提出论点，本论用分论点+论据论证，结论总结升华。这是标准的议论文三段式结构。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '议论文写作结构（引论-本论-结论）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**记叙文的六要素是（　　）**\nA. 论点、论据、论证、结论、建议、展望\nB. 标题、前言、主体、结尾、署名、日期\nC. 时间、地点、人物、起因、经过、结果\nD. 诗歌、散文、小说、戏剧、报告、书信\n答案：C  解析：记叙文以记人叙事为主，六要素完整才能把一件事写清楚。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '记叙文写作要素与方法' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**下列能使作文语言更具表现力的方法是（　　）**\nA. 全部用短句\nB. 长短句结合+恰当运用修辞+整散句结合\nC. 多用生僻词汇\nD. 每句话都加成语\n答案：B  解析：语言提升的核心是句式变化(长短结合、整散结合)和修辞润色(比喻/排比/引用)，以表达清晰为前提，不为文采而文采。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '作文语言提升（句式变化/修辞润色）' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**以下哪个论据属于"事实论据"（　　）**\nA. "业精于勤荒于嬉"(韩愈名言)\nB. 袁隆平几十年如一日研究杂交水稻最终成功\nC. "天行健，君子以自强不息"\nD. "学而不思则罔，思而不学则殆"\n答案：B  解析：事实论据是具体事例(袁隆平的故事)，道理论据是名言警句(A、C、D都是道理论据)。两种论据要配合使用。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '议论文论点与论据' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**以下论证结构属于"递进式"的是（　　）**\nA. 开头提出观点，然后从经济、文化、社会三个并列角度论证\nB. 先讲个人层面→再到社会层面→最后到国家层面(层层深入)\nC. 先讲正面→再讲反面(正反对比)\nD. 先总述→再分述→最后总结\n答案：B  解析：递进式由浅入深、由小到大层层推进。A是并列式，C是对照式，D是总分式。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '议论文论证结构' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**一个好的作文开头不应该（　　）**\nA. 开门见山，直接亮出观点\nB. 引用名言或设问引起兴趣\nC. 大段抄写题目材料\nD. 简洁有力地点明主旨\n答案：C  解析：开头应简洁有力，2-3行内引出中心论点。大段抄材料是最差的开头方式，没有自己的观点。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '开头与结尾技巧' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**积累写作素材的正确方法是（　　）**\nA. 背大量素材但不用\nB. 以少胜多，每个类型准备3-4个典型素材并学会"一材多用"\nC. 考试前临时找素材\nD. 只用课本上的例子\n答案：B  解析：素材积累要"以少胜多"——精选典型素材(人物/事件/名言)，学会从不同角度使用同一个素材论证不同观点。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '写作素材积累与运用' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

UPDATE knowledge_nodes SET content = CONCAT(content, '\n\n【即时自测】\n**作文卷面规范中，正确的做法是（　　）**\nA. 用铅笔书写\nB. 大面积涂改，涂黑疙瘩\nC. 用0.5mm黑色签字笔书写，字占格子2/3，段落5-8段为宜\nD. 全文只分2段\n答案：C  解析：卷面分约占5-8分。字迹工整、标点占格、段落分明(5-8段)、少涂改是基本要求。'), updated_at = NOW() WHERE subject_id = @s AND level = 4 AND name = '卷面与书写规范' AND (content NOT LIKE '%【即时自测】%' OR content LIKE '%答案：略%');

SELECT 'v187: 自测题补全Batch2完成！' AS result;
SELECT COUNT(*) AS batch2_count FROM knowledge_nodes WHERE subject_id = @s AND level = 4 AND content LIKE '%【即时自测】%' AND content LIKE '%答案：%';
