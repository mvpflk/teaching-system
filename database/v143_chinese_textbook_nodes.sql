-- ============================================================================
-- v143: 语文[职高·教材同步] — dict_subject注册 + knowledge_nodes树创建
-- 蓝本: 《语文》(基础模块)上/下册 2023统编版, 倪文锦主编, 高等教育出版社
-- 结构: L1(学科) → L2(册) → L3(单元) → L4(课文) 共约43节点
-- 幂等安全: INSERT IGNORE
-- ============================================================================
SET NAMES utf8mb4;

-- ============================================================
-- Part A: dict_subject 注册独立学科 (幂等)
-- ============================================================
SET @subj_id = 40;

INSERT IGNORE INTO dict_subject (id, subject_name, status, sort_order, created_at, updated_at)
VALUES (@subj_id, '语文[职高·教材同步]', 1, 19, NOW(), NOW());

SELECT CONCAT('dict_subject registered: id=', @subj_id) AS result;

-- ============================================================
-- Part B: 新建知识树
-- ============================================================

-- L1 学科根节点
INSERT IGNORE INTO knowledge_nodes (id, parent_id, subject_id, level, name, sort_order, content, status)
VALUES (3000, NULL, @subj_id, 1, '语文[职高·教材同步]', 0,
'## 语文[职高·教材同步]\n\n'
'依据《语文》（基础模块）上/下册（2023统编版，倪文锦主编，高等教育出版社）构建的教材同步知识树。\n\n'
'**结构**：上/下册各6单元，共12单元40篇课文。\n'
'**每课内容**：课文简介、作者与背景、字词积累、内容理解、艺术手法、主题思想、教学建议\n\n'
'**使用方式**：选单元 → 选课文 → AI教学助手自动生成教案/课堂提问/课后练习',
'ACTIVE');

SET @root = 3000;

-- L2: 册
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
VALUES (@root, @subj_id, 2, '基础模块上册', 1, 'ACTIVE'),
       (@root, @subj_id, 2, '基础模块下册', 2, 'ACTIVE');

SET @v1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@root AND name='基础模块上册' AND level=2);
SET @v2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@root AND name='基础模块下册' AND level=2);

-- ============================================================
-- L3: 上册单元(6) - 带单元简介
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@v1, @subj_id, 3, '第一单元 青春赞歌', 1, '本单元以"青春"为主题，选编诗歌和散文，引导学生感受青春的美好与力量，树立积极向上的人生观。', 'ACTIVE'),
(@v1, @subj_id, 3, '第二单元 劳动光荣', 2, '本单元以"劳动"为主题，选编记叙文，引导学生认识劳动的价值，尊重劳动者。', 'ACTIVE'),
(@v1, @subj_id, 3, '第三单元 品味经典', 3, '本单元选编古代议论性散文，引导学生领略经典的思想魅力，学习古人论事说理的方法。', 'ACTIVE'),
(@v1, @subj_id, 3, '第四单元 精神追求', 4, '本单元以"精神追求"为主题，选编散文，引导学生体味作者的思想情感，思考人生意义。', 'ACTIVE'),
(@v1, @subj_id, 3, '第五单元 走进戏剧', 5, '本单元选编中外戏剧名作，引导学生了解戏剧文学的特点，感受戏剧冲突和人物形象。', 'ACTIVE'),
(@v1, @subj_id, 3, '第六单元 诗意人生', 6, '本单元选编古典诗词，引导学生感受古典诗歌的意境美和语言美，提高诗歌鉴赏能力。', 'ACTIVE');

-- L3: 下册单元(6)
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, content, status) VALUES
(@v2, @subj_id, 3, '第一单元 民族复兴', 1, '本单元以"民族复兴"为主题，选编政论性文章，引导学生理解中华民族的伟大复兴历程，增强民族自豪感。', 'ACTIVE'),
(@v2, @subj_id, 3, '第二单元 人生感悟', 2, '本单元以"人生感悟"为主题，选编散文，引导学生思考人生意义，感悟生活哲理。', 'ACTIVE'),
(@v2, @subj_id, 3, '第三单元 科学精神', 3, '本单元以"科学精神"为主题，选编科普说明文和议论性文章，引导学生感受科学之美，培养科学精神。', 'ACTIVE'),
(@v2, @subj_id, 3, '第四单元 小说经典', 4, '本单元选编中外经典小说，引导学生了解小说的文体特征，提高小说鉴赏能力。', 'ACTIVE'),
(@v2, @subj_id, 3, '第五单元 文化传承', 5, '本单元以"文化传承"为主题，选编有关中华优秀传统文化的文章，引导学生了解和传承中华文化。', 'ACTIVE'),
(@v2, @subj_id, 3, '第六单元 家国情怀', 6, '本单元选编古代议论性散文和赋，引导学生体会古代士人的家国情怀，学习论说文的写作技巧。', 'ACTIVE');

-- ============================================================
-- L4: 上册课文(19篇)
-- ============================================================
SET @u1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v1 AND name='第一单元 青春赞歌' AND level=3);
SET @u2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v1 AND name='第二单元 劳动光荣' AND level=3);
SET @u3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v1 AND name='第三单元 品味经典' AND level=3);
SET @u4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v1 AND name='第四单元 精神追求' AND level=3);
SET @u5 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v1 AND name='第五单元 走进戏剧' AND level=3);
SET @u6 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v1 AND name='第六单元 诗意人生' AND level=3);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
-- 青春赞歌(4)
(@u1, @subj_id, 4, '沁园春·长沙', 1, 'ACTIVE'),
(@u1, @subj_id, 4, '致橡树', 2, 'ACTIVE'),
(@u1, @subj_id, 4, '我愿意是急流', 3, 'ACTIVE'),
(@u1, @subj_id, 4, '*青春万岁', 4, 'ACTIVE'),
-- 劳动光荣(3)
(@u2, @subj_id, 4, '荷花淀', 1, 'ACTIVE'),
(@u2, @subj_id, 4, '县委书记的榜样——焦裕禄', 2, 'ACTIVE'),
(@u2, @subj_id, 4, '*喜看稻菽千重浪', 3, 'ACTIVE'),
-- 品味经典(3)
(@u3, @subj_id, 4, '子路、曾皙、冉有、公西华侍坐', 1, 'ACTIVE'),
(@u3, @subj_id, 4, '劝学', 2, 'ACTIVE'),
(@u3, @subj_id, 4, '师说', 3, 'ACTIVE'),
-- 精神追求(3)
(@u4, @subj_id, 4, '故都的秋', 1, 'ACTIVE'),
(@u4, @subj_id, 4, '荷塘月色', 2, 'ACTIVE'),
(@u4, @subj_id, 4, '*世间最美的坟墓', 3, 'ACTIVE'),
-- 走进戏剧(2)
(@u5, @subj_id, 4, '雷雨（节选）', 1, 'ACTIVE'),
(@u5, @subj_id, 4, '茶馆（节选）', 2, 'ACTIVE'),
-- 诗意人生(4)
(@u6, @subj_id, 4, '静女', 1, 'ACTIVE'),
(@u6, @subj_id, 4, '短歌行', 2, 'ACTIVE'),
(@u6, @subj_id, 4, '归园田居（其一）', 3, 'ACTIVE'),
(@u6, @subj_id, 4, '梦游天姥吟留别', 4, 'ACTIVE');

-- ============================================================
-- L4: 下册课文(21篇)
-- ============================================================
SET @d1 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v2 AND name='第一单元 民族复兴' AND level=3);
SET @d2 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v2 AND name='第二单元 人生感悟' AND level=3);
SET @d3 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v2 AND name='第三单元 科学精神' AND level=3);
SET @d4 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v2 AND name='第四单元 小说经典' AND level=3);
SET @d5 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v2 AND name='第五单元 文化传承' AND level=3);
SET @d6 = (SELECT id FROM knowledge_nodes WHERE parent_id=@v2 AND name='第六单元 家国情怀' AND level=3);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status) VALUES
-- 民族复兴(3)
(@d1, @subj_id, 4, '中国人民站起来了', 1, 'ACTIVE'),
(@d1, @subj_id, 4, '*记念刘和珍君', 2, 'ACTIVE'),
(@d1, @subj_id, 4, '*在庆祝中国共产党成立100周年大会上的讲话（节选）', 3, 'ACTIVE'),
-- 人生感悟(3)
(@d2, @subj_id, 4, '合欢树', 1, 'ACTIVE'),
(@d2, @subj_id, 4, '*善良', 2, 'ACTIVE'),
(@d2, @subj_id, 4, '*人生的境界', 3, 'ACTIVE'),
-- 科学精神(3)
(@d3, @subj_id, 4, '科学是美丽的', 1, 'ACTIVE'),
(@d3, @subj_id, 4, '南州六月荔枝丹', 2, 'ACTIVE'),
(@d3, @subj_id, 4, '*天文学上的旷世之争', 3, 'ACTIVE'),
-- 小说经典(3)
(@d4, @subj_id, 4, '边城（节选）', 1, 'ACTIVE'),
(@d4, @subj_id, 4, '林黛玉进贾府', 2, 'ACTIVE'),
(@d4, @subj_id, 4, '林教头风雪山神庙', 3, 'ACTIVE'),
-- 文化传承(3)
(@d5, @subj_id, 4, '中国建筑的特征', 1, 'ACTIVE'),
(@d5, @subj_id, 4, '*乡土中国（节选）', 2, 'ACTIVE'),
(@d5, @subj_id, 4, '*传统文化与文化传统', 3, 'ACTIVE'),
-- 家国情怀(4)
(@d6, @subj_id, 4, '谏太宗十思疏', 1, 'ACTIVE'),
(@d6, @subj_id, 4, '阿房宫赋', 2, 'ACTIVE'),
(@d6, @subj_id, 4, '六国论', 3, 'ACTIVE'),
(@d6, @subj_id, 4, '*国殇', 4, 'ACTIVE');

-- 汇总
SELECT CONCAT('v143 complete: L1=1 L2=2 L3=12 L4=', 
  (SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id=@subj_id AND level=4)) AS result;
