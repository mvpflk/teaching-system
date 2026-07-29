-- ============================================================================
-- v127: 语文[职高] L4知识点节点扩充
-- 在现有L3任务节点下创建细分知识点（level=4）
-- 对标信息技术应用基础学科的节点密度
-- 幂等：INSERT IGNORE
-- ============================================================================

SET @subject_id = 20; -- 语文[职高]

-- 获取L3任务节点ID
SET @l3_pinyin = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '字音字形' AND level = 3 LIMIT 1);
SET @l3_ciyu = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '词语运用' AND level = 3 LIMIT 1);
SET @l3_bingju = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '病句辨析与修辞' AND level = 3 LIMIT 1);
SET @l3_sheke = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '社科类文本阅读' AND level = 3 LIMIT 1);
SET @l3_wenxue = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '文学作品阅读' AND level = 3 LIMIT 1);
SET @l3_shici = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '常见文言实词虚词' AND level = 3 LIMIT 1);
SET @l3_fanyi = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '文言文翻译与理解' AND level = 3 LIMIT 1);
SET @l3_zuowen = (SELECT id FROM knowledge_nodes WHERE subject_id = @subject_id AND name = '话题作文' AND level = 3 LIMIT 1);

-- ============================================================
-- 字音字形（@l3_pinyin） → 8个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_pinyin, @subject_id, 4, '多音字辨析', 1, NOW(), NOW()),
(@l3_pinyin, @subject_id, 4, '形近字辨析', 2, NOW(), NOW()),
(@l3_pinyin, @subject_id, 4, '易读错字', 3, NOW(), NOW()),
(@l3_pinyin, @subject_id, 4, '易写错字', 4, NOW(), NOW()),
(@l3_pinyin, @subject_id, 4, '同音字辨析', 5, NOW(), NOW()),
(@l3_pinyin, @subject_id, 4, '拼音拼写规则', 6, NOW(), NOW()),
(@l3_pinyin, @subject_id, 4, '汉字结构知识', 7, NOW(), NOW()),
(@l3_pinyin, @subject_id, 4, '四川方言辨正', 8, NOW(), NOW());

-- ============================================================
-- 词语运用（@l3_ciyu） → 5个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_ciyu, @subject_id, 4, '近义词辨析', 1, NOW(), NOW()),
(@l3_ciyu, @subject_id, 4, '成语使用正误', 2, NOW(), NOW()),
(@l3_ciyu, @subject_id, 4, '关联词语搭配', 3, NOW(), NOW()),
(@l3_ciyu, @subject_id, 4, '词语的感情色彩与语体色彩', 4, NOW(), NOW()),
(@l3_ciyu, @subject_id, 4, '常用熟语与惯用语', 5, NOW(), NOW());

-- ============================================================
-- 病句辨析与修辞（@l3_bingju） → 10个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_bingju, @subject_id, 4, '语序不当', 1, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '搭配不当', 2, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '成分残缺或赘余', 3, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '结构混乱', 4, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '表意不明（歧义句）', 5, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '不合逻辑', 6, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '比喻与借代', 7, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '拟人与夸张', 8, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '排比与对偶', 9, NOW(), NOW()),
(@l3_bingju, @subject_id, 4, '设问、反问与反复', 10, NOW(), NOW());

-- ============================================================
-- 社科类文本阅读（@l3_sheke） → 6个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_sheke, @subject_id, 4, '论述文阅读（论点·论据·论证）', 1, NOW(), NOW()),
(@l3_sheke, @subject_id, 4, '说明文阅读（说明方法·说明顺序）', 2, NOW(), NOW()),
(@l3_sheke, @subject_id, 4, '信息筛选与整合', 3, NOW(), NOW()),
(@l3_sheke, @subject_id, 4, '分析推理与判断', 4, NOW(), NOW()),
(@l3_sheke, @subject_id, 4, '文章结构与论证逻辑', 5, NOW(), NOW()),
(@l3_sheke, @subject_id, 4, '重要概念与关键句理解', 6, NOW(), NOW());

-- ============================================================
-- 文学作品阅读（@l3_wenxue） → 7个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_wenxue, @subject_id, 4, '小说阅读（人物·情节·环境）', 1, NOW(), NOW()),
(@l3_wenxue, @subject_id, 4, '散文阅读（形散神聚·情景交融）', 2, NOW(), NOW()),
(@l3_wenxue, @subject_id, 4, '表现手法辨析', 3, NOW(), NOW()),
(@l3_wenxue, @subject_id, 4, '表达方式及其作用', 4, NOW(), NOW()),
(@l3_wenxue, @subject_id, 4, '人物形象分析', 5, NOW(), NOW()),
(@l3_wenxue, @subject_id, 4, '语言品味与赏析', 6, NOW(), NOW()),
(@l3_wenxue, @subject_id, 4, '主题理解与概括', 7, NOW(), NOW());

-- ============================================================
-- 常见文言实词虚词（@l3_shici） → 8个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_shici, @subject_id, 4, '一词多义', 1, NOW(), NOW()),
(@l3_shici, @subject_id, 4, '古今异义', 2, NOW(), NOW()),
(@l3_shici, @subject_id, 4, '词类活用', 3, NOW(), NOW()),
(@l3_shici, @subject_id, 4, '通假字', 4, NOW(), NOW()),
(@l3_shici, @subject_id, 4, '常见虚词（之·其·以·而）', 5, NOW(), NOW()),
(@l3_shici, @subject_id, 4, '常见虚词（于·为·乃·则）', 6, NOW(), NOW()),
(@l3_shici, @subject_id, 4, '文言句式（判断句·被动句）', 7, NOW(), NOW()),
(@l3_shici, @subject_id, 4, '文言句式（倒装句·省略句）', 8, NOW(), NOW());

-- ============================================================
-- 文言文翻译与理解（@l3_fanyi） → 3个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_fanyi, @subject_id, 4, '翻译原则与步骤', 1, NOW(), NOW()),
(@l3_fanyi, @subject_id, 4, '翻译技巧（留·补·换·调·删）', 2, NOW(), NOW()),
(@l3_fanyi, @subject_id, 4, '文意理解与概括', 3, NOW(), NOW());

-- ============================================================
-- 话题作文（@l3_zuowen） → 7个L4知识点
-- ============================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_zuowen, @subject_id, 4, '审题立意', 1, NOW(), NOW()),
(@l3_zuowen, @subject_id, 4, '议论文论点与论据', 2, NOW(), NOW()),
(@l3_zuowen, @subject_id, 4, '议论文论证结构', 3, NOW(), NOW()),
(@l3_zuowen, @subject_id, 4, '开头与结尾技巧', 4, NOW(), NOW()),
(@l3_zuowen, @subject_id, 4, '语言表达技巧', 5, NOW(), NOW()),
(@l3_zuowen, @subject_id, 4, '写作素材积累与运用', 6, NOW(), NOW()),
(@l3_zuowen, @subject_id, 4, '卷面与书写规范', 7, NOW(), NOW());

SELECT 'v127: 语文[职高]L4知识点节点扩充完成！' AS result;
SELECT CONCAT('字音字形: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_pinyin
UNION ALL SELECT CONCAT('词语运用: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_ciyu
UNION ALL SELECT CONCAT('病句修辞: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_bingju
UNION ALL SELECT CONCAT('社科类阅读: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_sheke
UNION ALL SELECT CONCAT('文学作品阅读: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_wenxue
UNION ALL SELECT CONCAT('实词虚词: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_shici
UNION ALL SELECT CONCAT('翻译理解: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_fanyi
UNION ALL SELECT CONCAT('话题作文: ', COUNT(*), ' 个L4') FROM knowledge_nodes WHERE parent_id = @l3_zuowen
UNION ALL SELECT CONCAT('应用文写作: ', COUNT(*), ' 个L4（v78已创建）') FROM knowledge_nodes WHERE parent_id = 862;
