-- 语文[职高·教材同步] 完整知识树
-- subject_id=48, 高教版2023/2024统编教材

-- L1
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, NULL, '语文[职高·教材同步]', 1, 1, 'ACTIVE');
SET @root = LAST_INSERT_ID();

-- ==================== 基础模块 上册 ====================
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @root, '基础模块 上册', 2, 1, 'ACTIVE');
SET @b1 = LAST_INSERT_ID();

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第一单元', 3, 1, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '沁园春·长沙', 4, 1, 'ACTIVE'), (48, @u, '风景谈', 4, 2, 'ACTIVE'),
(48, @u, '荷花淀', 4, 3, 'ACTIVE'), (48, @u, '*江姐（节选）', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第二单元', 3, 2, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '诗二首（雨巷/我愿意是急流）', 4, 1, 'ACTIVE'), (48, @u, '荷塘月色', 4, 2, 'ACTIVE'),
(48, @u, '*灯', 4, 3, 'ACTIVE'), (48, @u, '林黛玉进贾府', 4, 4, 'ACTIVE'),
(48, @u, '最后一片叶子', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第三单元', 3, 3, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '伐檀', 4, 1, 'ACTIVE'), (48, @u, '*无衣', 4, 2, 'ACTIVE'),
(48, @u, '种树郭橐驼传', 4, 3, 'ACTIVE'), (48, @u, '念奴娇·赤壁怀古', 4, 4, 'ACTIVE'),
(48, @u, '促织', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第四单元（整本书阅读）', 3, 4, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '《平凡的世界》', 4, 1, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第五单元', 3, 5, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '县委书记的榜样——焦裕禄', 4, 1, 'ACTIVE'),
(48, @u, '喜看稻菽千重浪——记首届国家最高科学技术奖获得者袁隆平', 4, 2, 'ACTIVE'),
(48, @u, '国家的儿子（节选）', 4, 3, 'ACTIVE'),
(48, @u, '*心有一团火，温暖众人心', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第六单元', 3, 6, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '子路、曾皙、冉有、公西华侍坐', 4, 1, 'ACTIVE'),
(48, @u, '*寡人之于国也', 4, 2, 'ACTIVE'), (48, @u, '劝学', 4, 3, 'ACTIVE'),
(48, @u, '公输', 4, 4, 'ACTIVE'), (48, @u, '*庖丁解牛', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第七单元', 3, 7, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '反对党八股（节选）', 4, 1, 'ACTIVE'), (48, @u, '拿来主义', 4, 2, 'ACTIVE'),
(48, @u, '*千篇一律与千变万化', 4, 3, 'ACTIVE'), (48, @u, '师说', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b1, '第八单元（活动单元）', 3, 8, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '语感与语言习得', 4, 1, 'ACTIVE');

-- ==================== 基础模块 下册 ====================
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @root, '基础模块 下册', 2, 2, 'ACTIVE');
SET @b2 = LAST_INSERT_ID();

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第一单元', 3, 1, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '中国人民站起来了', 4, 1, 'ACTIVE'),
(48, @u, '在庆祝中国共产党成立100周年大会上的讲话', 4, 2, 'ACTIVE'),
(48, @u, '长征胜利万岁', 4, 3, 'ACTIVE'), (48, @u, '*百合花', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第二单元', 3, 2, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '国殇', 4, 1, 'ACTIVE'), (48, @u, '烛之武退秦师', 4, 2, 'ACTIVE'),
(48, @u, '廉颇蔺相如列传（节选）', 4, 3, 'ACTIVE'),
(48, @u, '永遇乐·京口北固亭怀古', 4, 4, 'ACTIVE'),
(48, @u, '*声声慢（寻寻觅觅）', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第三单元', 3, 3, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '祝福', 4, 1, 'ACTIVE'), (48, @u, '群英会蒋干中计', 4, 2, 'ACTIVE'),
(48, @u, '*套中人', 4, 3, 'ACTIVE'), (48, @u, '雷雨（节选）', 4, 4, 'ACTIVE'),
(48, @u, '*项链', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第四单元（整本书阅读）', 3, 4, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '《乡土中国》', 4, 1, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第五单元', 3, 5, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '在马克思墓前的讲话', 4, 1, 'ACTIVE'), (48, @u, '*世间最感人的坟墓', 4, 2, 'ACTIVE'),
(48, @u, '飞向太空的航程', 4, 3, 'ACTIVE'), (48, @u, '景泰蓝的制作', 4, 4, 'ACTIVE'),
(48, @u, '*画里阴晴', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第六单元', 3, 6, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '青蒿素：人类征服疾病的一小步', 4, 1, 'ACTIVE'),
(48, @u, '青纱帐——甘蔗林', 4, 2, 'ACTIVE'),
(48, @u, '*晨昏诺日朗', 4, 3, 'ACTIVE'), (48, @u, '哦，香雪', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第七单元', 3, 7, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '归园田居（其一）', 4, 1, 'ACTIVE'),
(48, @u, '唐诗二首（将进酒/登高）', 4, 2, 'ACTIVE'),
(48, @u, '赤壁赋', 4, 3, 'ACTIVE'), (48, @u, '*项脊轩志', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '第八单元（活动单元）', 3, 8, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '跨媒介阅读与交流', 4, 1, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b2, '古诗词诵读', 3, 9, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '过华清宫绝句三首（其一）', 4, 1, 'ACTIVE'), (48, @u, '锦瑟', 4, 2, 'ACTIVE'),
(48, @u, '虞美人（春花秋月何时了）', 4, 3, 'ACTIVE'),
(48, @u, '破阵子（燕子来时新社）', 4, 4, 'ACTIVE'),
(48, @u, '苏幕遮（碧云天）', 4, 5, 'ACTIVE');

-- ==================== 拓展模块 上册 ====================
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @root, '拓展模块 上册', 2, 3, 'ACTIVE');
SET @b3 = LAST_INSERT_ID();

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b3, '第一单元', 3, 1, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '改造我们的学习', 4, 1, 'ACTIVE'), (48, @u, '"友邦惊诧"论', 4, 2, 'ACTIVE'),
(48, @u, '人生的境界', 4, 3, 'ACTIVE'), (48, @u, '*人应当坚持正义', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b3, '第二单元', 3, 2, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '鸿门宴', 4, 1, 'ACTIVE'), (48, @u, '陈情表', 4, 2, 'ACTIVE'),
(48, @u, '兰亭集序', 4, 3, 'ACTIVE'), (48, @u, '*病梅馆记', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b3, '第三单元', 3, 3, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '《中国科学技术史》序言（节选）', 4, 1, 'ACTIVE'),
(48, @u, '*东方和西方的科学', 4, 2, 'ACTIVE'),
(48, @u, '文学作为语言艺术的独特地位', 4, 3, 'ACTIVE'),
(48, @u, '*音乐就在你心中', 4, 4, 'ACTIVE'),
(48, @u, '《人间词话》六则', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b3, '第四单元', 3, 4, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '故都的秋', 4, 1, 'ACTIVE'), (48, @u, '我的母亲', 4, 2, 'ACTIVE'),
(48, @u, '*合欢树', 4, 3, 'ACTIVE'), (48, @u, '像山那样思考', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b3, '第五单元', 3, 5, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '石钟山记', 4, 1, 'ACTIVE'), (48, @u, '登泰山记', 4, 2, 'ACTIVE'),
(48, @u, '黄山记', 4, 3, 'ACTIVE'), (48, @u, '*瓦尔登湖（节选）', 4, 4, 'ACTIVE');

-- ==================== 拓展模块 下册 ====================
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @root, '拓展模块 下册', 2, 4, 'ACTIVE');
SET @b4 = LAST_INSERT_ID();

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b4, '第一单元', 3, 1, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '社会历史的决定性基础', 4, 1, 'ACTIVE'),
(48, @u, '人的正确思想是从哪里来的？', 4, 2, 'ACTIVE'),
(48, @u, '以中国式现代化全面推进中华民族伟大复兴', 4, 3, 'ACTIVE'),
(48, @u, '*实践是检验真理的唯一标准', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b4, '第二单元', 3, 2, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '琵琶行并序', 4, 1, 'ACTIVE'), (48, @u, '长亭送别', 4, 2, 'ACTIVE'),
(48, @u, '*哈姆莱特（节选）', 4, 3, 'ACTIVE'), (48, @u, '边城（节选）', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b4, '第三单元', 3, 3, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '六国论', 4, 1, 'ACTIVE'), (48, @u, '过秦论', 4, 2, 'ACTIVE'),
(48, @u, '五代史伶官传序', 4, 3, 'ACTIVE'), (48, @u, '*察今', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b4, '第四单元', 3, 4, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '阿Q正传（节选）', 4, 1, 'ACTIVE'),
(48, @u, '诗二首（炉中煤/红烛）', 4, 2, 'ACTIVE'),
(48, @u, '可爱的中国（节选）', 4, 3, 'ACTIVE'), (48, @u, '*党费', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b4, '第五单元', 3, 5, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '《水经注》序（节选）', 4, 1, 'ACTIVE'), (48, @u, '*四时用药例', 4, 2, 'ACTIVE'),
(48, @u, '稻', 4, 3, 'ACTIVE'), (48, @u, '*《作酢法》二则', 4, 4, 'ACTIVE'),
(48, @u, '阳燧照物', 4, 5, 'ACTIVE');

-- ==================== 职业模块 ====================
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @root, '职业模块', 2, 5, 'ACTIVE');
SET @b5 = LAST_INSERT_ID();

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '第一单元', 3, 1, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '七律二首 送瘟神', 4, 1, 'ACTIVE'),
(48, @u, '宁夏闽宁镇：昔日干沙滩，今日金沙滩', 4, 2, 'ACTIVE'),
(48, @u, '"探界者"钟扬', 4, 3, 'ACTIVE'),
(48, @u, '*闪亮的坐标——劳模王进喜', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '第二单元（职场应用写作一）', 3, 2, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '求职和应聘', 4, 1, 'ACTIVE'), (48, @u, '洽谈', 4, 2, 'ACTIVE'),
(48, @u, '协商', 4, 3, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '第三单元', 3, 3, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '青年在选择职业时的考虑（节选）', 4, 1, 'ACTIVE'),
(48, @u, '简单相信，傻傻坚持', 4, 2, 'ACTIVE'),
(48, @u, '品质', 4, 3, 'ACTIVE'), (48, @u, '*鉴赏家', 4, 4, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '第四单元（职场应用写作二）', 3, 4, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '活动策划', 4, 1, 'ACTIVE'), (48, @u, '市场调查', 4, 2, 'ACTIVE'),
(48, @u, '撰写报告', 4, 3, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '第五单元（走近大国工匠）', 3, 5, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '展示国家工程，了解工匠贡献', 4, 1, 'ACTIVE'),
(48, @u, '学习工匠事迹，领略工匠风采', 4, 2, 'ACTIVE'),
(48, @u, '联系生活实际，弘扬工匠精神', 4, 3, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '第六单元（微写作）', 3, 6, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '描述事物', 4, 1, 'ACTIVE'), (48, @u, '抒发情感', 4, 2, 'ACTIVE'),
(48, @u, '表达观点', 4, 3, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '第七单元', 3, 7, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '唐宋大诗人诗中的物候', 4, 1, 'ACTIVE'), (48, @u, '*动物游戏之谜', 4, 2, 'ACTIVE'),
(48, @u, '南州六月荔枝丹', 4, 3, 'ACTIVE'), (48, @u, '统筹方法', 4, 4, 'ACTIVE'),
(48, @u, '*北斗，每一颗星都在闪亮', 4, 5, 'ACTIVE');

INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES (48, @b5, '古诗词诵读', 3, 8, 'ACTIVE');
SET @u = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (subject_id, parent_id, name, level, sort_order, status) VALUES
(48, @u, '雨霖铃（寒蝉凄切）', 4, 1, 'ACTIVE'), (48, @u, '桂枝香·金陵怀古', 4, 2, 'ACTIVE'),
(48, @u, '苏幕遮（燎沉香）', 4, 3, 'ACTIVE'), (48, @u, '书愤', 4, 4, 'ACTIVE'),
(48, @u, '扬州慢（淮左名都）', 4, 5, 'ACTIVE');
