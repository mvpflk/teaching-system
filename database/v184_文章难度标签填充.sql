-- ============================================================================
-- v184: 语文[职高] 文章难度分级 + 标签填充
-- 难度分布对齐考纲：容易40% / 较容易30% / 较难20% / 难题10%
-- 标签从章节层级 + 知识点关键词自动提取
-- 幂等：UPDATE 可重复执行
-- ============================================================================

SET @yy_zg_sid = 20;

-- ============================================================
-- Part 1: 按 L3 任务类型分配 difficulty（1-5级）
-- ============================================================

-- difficulty=1 (识记A级·约10%) — 字音字形、文学常识默写
UPDATE knowledge_articles
SET difficulty = 1, updated_at = NOW()
WHERE subject_id = @yy_zg_sid
  AND task IN ('字音字形', '文学常识与名句默写');

-- difficulty=2 (理解B级·约25%) — 词语运用、标点符号、实词虚词
UPDATE knowledge_articles
SET difficulty = 2, updated_at = NOW()
WHERE subject_id = @yy_zg_sid
  AND task IN ('词语运用', '标点符号', '常见文言实词虚词');

-- difficulty=3 (综合分析C级+表达应用D级·约60%) — 病句辨析、修辞手法、社科类阅读、应用文写作、文言翻译
UPDATE knowledge_articles
SET difficulty = 3, updated_at = NOW()
WHERE subject_id = @yy_zg_sid
  AND task IN ('病句辨析', '病句辨析与修辞', '修辞手法辨析', '社科类文本阅读', '应用文写作', '文言文翻译与理解');

-- difficulty=4 (表达应用D级·约20%) — 文学作品阅读、话题作文
UPDATE knowledge_articles
SET difficulty = 4, updated_at = NOW()
WHERE subject_id = @yy_zg_sid
  AND task IN ('文学作品阅读', '话题作文');

-- difficulty=5 (鉴赏评价E级·约5%) — 古诗词鉴赏
UPDATE knowledge_articles
SET difficulty = 5, updated_at = NOW()
WHERE subject_id = @yy_zg_sid
  AND task IN ('古诗词鉴赏');


-- ============================================================
-- Part 2: 按章节 + 任务 + 内容特征填充 tags（JSON数组）
-- 每个文章 3-5 个标签，支持前端标签云筛选
-- ============================================================

-- 2a. 基础知识类 — 字音字形
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '基础知识', '字音字形',
    CASE
        WHEN title LIKE '%多音字%' THEN '多音字'
        WHEN title LIKE '%形近字%' THEN '形近字'
        WHEN title LIKE '%易读错%' THEN '易读错字'
        WHEN title LIKE '%易写错%' THEN '易写错字'
        WHEN title LIKE '%同音字%' THEN '同音字'
        WHEN title LIKE '%拼音%' THEN '拼音规则'
        WHEN title LIKE '%汉字结构%' THEN '汉字结构'
        WHEN title LIKE '%方言%' THEN '方言辨正'
        ELSE '语音文字'
    END,
    '选择题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '字音字形' AND tags IS NULL;

-- 2b. 词语运用
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '基础知识', '词语运用',
    CASE
        WHEN title LIKE '%近义词%' THEN '近义词辨析'
        WHEN title LIKE '%成语%' THEN '成语'
        WHEN title LIKE '%关联词%' THEN '关联词'
        WHEN title LIKE '%感情色彩%' OR title LIKE '%语体色彩%' THEN '词语色彩'
        WHEN title LIKE '%熟语%' OR title LIKE '%惯用语%' THEN '熟语惯用语'
        ELSE '词语'
    END,
    '选择题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '词语运用' AND tags IS NULL;

-- 2c. 病句辨析
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '基础知识', '病句辨析',
    CASE
        WHEN title LIKE '%语序%' THEN '语序不当'
        WHEN title LIKE '%搭配%' THEN '搭配不当'
        WHEN title LIKE '%残缺%' OR title LIKE '%赘余%' THEN '成分残缺赘余'
        WHEN title LIKE '%结构混乱%' THEN '结构混乱'
        WHEN title LIKE '%歧义%' OR title LIKE '%表意不明%' THEN '歧义句'
        WHEN title LIKE '%不合逻辑%' THEN '不合逻辑'
        ELSE '病句'
    END,
    '选择题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task IN ('病句辨析', '病句辨析与修辞') AND tags IS NULL;

-- 2d. 标点符号
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '基础知识', '标点符号',
    CASE
        WHEN title LIKE '%顿号%' OR title LIKE '%逗号%' THEN '顿号逗号'
        WHEN title LIKE '%分号%' THEN '分号'
        WHEN title LIKE '%问号%' THEN '问号'
        WHEN title LIKE '%引号%' THEN '引号'
        WHEN title LIKE '%省略号%' THEN '省略号'
        WHEN title LIKE '%破折号%' THEN '破折号'
        WHEN title LIKE '%书名号%' THEN '书名号'
        WHEN title LIKE '%连接号%' OR title LIKE '%间隔号%' THEN '连接号间隔号'
        WHEN title LIKE '%综合%' THEN '标点综合'
        ELSE '标点'
    END,
    '选择题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '标点符号' AND tags IS NULL;

-- 2e. 修辞手法辨析
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '基础知识', '修辞手法',
    CASE
        WHEN title LIKE '%比喻%' AND title NOT LIKE '%比拟%' THEN '比喻'
        WHEN title LIKE '%比拟%' THEN '比拟'
        WHEN title LIKE '%借代%' THEN '借代'
        WHEN title LIKE '%夸张%' THEN '夸张'
        WHEN title LIKE '%对偶%' THEN '对偶'
        WHEN title LIKE '%排比%' THEN '排比'
        WHEN title LIKE '%反问%' THEN '反问'
        WHEN title LIKE '%设问%' THEN '设问'
        WHEN title LIKE '%易混%' OR title LIKE '%辨析%' THEN '修辞辨析'
        ELSE '修辞'
    END,
    '选择题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '修辞手法辨析' AND tags IS NULL;

-- 2f. 社科类文本阅读
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '现代文阅读', '社科类文本',
    CASE
        WHEN title LIKE '%论述文%' OR title LIKE '%论点%' THEN '论述文'
        WHEN title LIKE '%说明文%' OR title LIKE '%说明方法%' THEN '说明文'
        WHEN title LIKE '%信息筛选%' THEN '信息筛选'
        WHEN title LIKE '%分析推理%' OR title LIKE '%判断%' THEN '分析推理'
        WHEN title LIKE '%结构%' OR title LIKE '%论证逻辑%' THEN '文章结构'
        WHEN title LIKE '%概念%' OR title LIKE '%关键句%' THEN '关键句理解'
        ELSE '社科阅读'
    END,
    '选择题+简答题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '社科类文本阅读' AND tags IS NULL;

-- 2g. 文学作品阅读
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '现代文阅读', '文学作品',
    CASE
        WHEN title LIKE '%小说%' THEN '小说'
        WHEN title LIKE '%散文%' THEN '散文'
        WHEN title LIKE '%表现手法%' THEN '表现手法'
        WHEN title LIKE '%表达方式%' THEN '表达方式'
        WHEN title LIKE '%人物形象%' THEN '人物分析'
        WHEN title LIKE '%语言品味%' OR title LIKE '%赏析%' THEN '语言赏析'
        WHEN title LIKE '%主题%' OR title LIKE '%概括%' THEN '主题概括'
        ELSE '文学阅读'
    END,
    '简答题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '文学作品阅读' AND tags IS NULL;

-- 2h. 常见文言实词虚词
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '文言文阅读', '实词虚词',
    CASE
        WHEN title LIKE '%一词多义%' THEN '一词多义'
        WHEN title LIKE '%古今异义%' THEN '古今异义'
        WHEN title LIKE '%词类活用%' THEN '词类活用'
        WHEN title LIKE '%通假字%' THEN '通假字'
        WHEN title LIKE '%之%其%以%而%' OR title LIKE '%于%为%乃%则%' THEN '虚词用法'
        WHEN title LIKE '%18个%' THEN '虚词汇总'
        WHEN title LIKE '%判断句%' OR title LIKE '%被动句%' OR title LIKE '%倒装句%' OR title LIKE '%省略句%' THEN '文言句式'
        ELSE '文言词汇'
    END,
    '选择题+翻译题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '常见文言实词虚词' AND tags IS NULL;

-- 2i. 文言文翻译与理解
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '文言文阅读', '翻译理解',
    CASE
        WHEN title LIKE '%翻译原则%' OR title LIKE '%步骤%' THEN '翻译原则'
        WHEN title LIKE '%翻译技巧%' OR title LIKE '%留%补%换%' THEN '翻译技巧'
        WHEN title LIKE '%文意理解%' OR title LIKE '%概括%' THEN '文意理解'
        WHEN title LIKE '%廉颇%' THEN '史记名篇'
        WHEN title LIKE '%侍坐%' THEN '论语名篇'
        WHEN title LIKE '%劝学%' THEN '荀子名篇'
        WHEN title LIKE '%师说%' THEN '韩愈名篇'
        ELSE '文言翻译'
    END,
    '翻译题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '文言文翻译与理解' AND tags IS NULL;

-- 2j. 古诗词鉴赏
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '文言文阅读', '古诗词鉴赏',
    CASE
        WHEN title LIKE '%诗经%' THEN '诗经'
        WHEN title LIKE '%唐诗%' OR title LIKE '%将进酒%' OR title LIKE '%茅屋%' THEN '唐诗'
        WHEN title LIKE '%宋词%' OR title LIKE '%赤壁怀古%' OR title LIKE '%雨霖铃%' THEN '宋词'
        WHEN title LIKE '%意象%' OR title LIKE '%意境%' THEN '意象意境'
        WHEN title LIKE '%表达技巧%' OR title LIKE '%抒情%' THEN '表达技巧'
        WHEN title LIKE '%思想情感%' OR title LIKE '%态度%' THEN '思想情感'
        WHEN title LIKE '%语言赏析%' OR title LIKE '%炼字%' OR title LIKE '%诗眼%' THEN '语言赏析'
        ELSE '诗词鉴赏'
    END,
    '简答题+默写题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '古诗词鉴赏' AND tags IS NULL;

-- 2k. 文学常识与名句默写
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '文言文阅读', '文学常识', '名句默写',
    CASE
        WHEN title LIKE '%先秦%' THEN '先秦诗文'
        WHEN title LIKE '%唐宋%' THEN '唐宋诗文'
        WHEN title LIKE '%宋文%' OR title LIKE '%六国论%' THEN '宋文'
        WHEN title LIKE '%现代诗%' OR title LIKE '%我爱这土地%' OR title LIKE '%雨巷%' OR title LIKE '%致橡树%' THEN '现代诗歌'
        WHEN title LIKE '%文学体裁%' THEN '文学体裁'
        WHEN title LIKE '%作家%' OR title LIKE '%作品%' THEN '作家作品'
        WHEN title LIKE '%文化常识%' OR title LIKE '%称谓%' OR title LIKE '%科举%' THEN '文化常识'
        ELSE '文学常识'
    END,
    '默写题+选择题'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '文学常识与名句默写' AND tags IS NULL;

-- 2l. 应用文写作
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '写作', '应用文',
    CASE
        WHEN title LIKE '%通知%' THEN '通知'
        WHEN title LIKE '%启事%' THEN '启事'
        WHEN title LIKE '%书信%' THEN '书信'
        WHEN title LIKE '%便条%' OR title LIKE '%请假%' OR title LIKE '%留言%' THEN '便条'
        WHEN title LIKE '%单据%' OR title LIKE '%借条%' OR title LIKE '%收条%' THEN '单据'
        WHEN title LIKE '%计划%' THEN '计划'
        WHEN title LIKE '%总结%' THEN '总结'
        WHEN title LIKE '%说明书%' THEN '说明书'
        WHEN title LIKE '%求职信%' THEN '求职信'
        WHEN title LIKE '%应聘书%' THEN '应聘书'
        WHEN title LIKE '%会议记录%' THEN '会议记录'
        WHEN title LIKE '%调查报告%' THEN '调查报告'
        ELSE '应用文格式'
    END,
    '小作文'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '应用文写作' AND tags IS NULL;

-- 2m. 话题作文
UPDATE knowledge_articles
SET tags = JSON_ARRAY(
    '写作', '话题作文',
    CASE
        WHEN title LIKE '%审题%' OR title LIKE '%立意%' THEN '审题立意'
        WHEN title LIKE '%材料作文%' THEN '材料作文'
        WHEN title LIKE '%论点%' OR title LIKE '%论据%' THEN '论点论据'
        WHEN title LIKE '%论证结构%' OR title LIKE '%议论文写作结构%' THEN '论证结构'
        WHEN title LIKE '%开头%' OR title LIKE '%结尾%' THEN '开头结尾'
        WHEN title LIKE '%语言提升%' OR title LIKE '%语言表达%' THEN '语言表达'
        WHEN title LIKE '%素材%' THEN '写作素材'
        WHEN title LIKE '%卷面%' OR title LIKE '%书写%' THEN '卷面书写'
        WHEN title LIKE '%记叙文%' THEN '记叙文'
        ELSE '作文技巧'
    END,
    '大作文'),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND task = '话题作文' AND tags IS NULL;


-- ============================================================
-- Part 3: 验证
-- ============================================================

SELECT 'v184: 文章难度分级 + 标签填充完成！' AS result;

-- 难度分布统计
SELECT CONCAT('难度', difficulty, ': ', COUNT(*), '篇 (', ROUND(COUNT(*)*100.0/(SELECT COUNT(*) FROM knowledge_articles WHERE subject_id = @yy_zg_sid), 0), '%)') AS 难度分布
FROM knowledge_articles WHERE subject_id = @yy_zg_sid
GROUP BY difficulty ORDER BY difficulty;

-- 标签统计（JSON格式，验证用）
SELECT CONCAT('有标签文章: ', COUNT(*)) AS result FROM knowledge_articles WHERE subject_id = @yy_zg_sid AND tags IS NOT NULL;

-- 空标签/空难度检查
SELECT CONCAT('无标签文章: ', COUNT(*)) FROM knowledge_articles WHERE subject_id = @yy_zg_sid AND (tags IS NULL OR tags = '');
SELECT CONCAT('无难度文章: ', COUNT(*)) FROM knowledge_articles WHERE subject_id = @yy_zg_sid AND difficulty IS NULL;
