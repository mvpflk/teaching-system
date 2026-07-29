-- ============================================================================
-- v75: 语文/英语文化课知识节点框架
-- 普高→全国考纲结构 / 职高→四川省对口升学考纲结构
-- 仅创建level2(章节)+level3(任务)框架，知识点留待教师填充
-- 幂等：使用 INSERT IGNORE 防重复
-- ============================================================================

SET @yy_pg = (SELECT id FROM dict_subject WHERE subject_name = '语文[普高]' AND status = 1 LIMIT 1);
SET @yy_zg = (SELECT id FROM dict_subject WHERE subject_name = '语文[职高]' AND status = 1 LIMIT 1);
SET @en_pg = (SELECT id FROM dict_subject WHERE subject_name = '英语[普高]' AND status = 1 LIMIT 1);
SET @en_zg = (SELECT id FROM dict_subject WHERE subject_name = '英语[职高]' AND status = 1 LIMIT 1);

-- 获取各学科level=1根节点ID
SET @yy_pg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_pg AND level = 1 LIMIT 1);
SET @yy_zg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg AND level = 1 LIMIT 1);
SET @en_pg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @en_pg AND level = 1 LIMIT 1);
SET @en_zg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @en_zg AND level = 1 LIMIT 1);

-- ============================================================================
-- 普高语文[全国考纲] (subject_id=@yy_pg, root=@yy_pg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
-- 现代文阅读
(@yy_pg_root, @yy_pg, 2, '现代文阅读', 1, NOW(), NOW()),
(@yy_pg_root, @yy_pg, 2, '古代诗文阅读', 2, NOW(), NOW()),
(@yy_pg_root, @yy_pg, 2, '语言文字运用', 3, NOW(), NOW()),
(@yy_pg_root, @yy_pg, 2, '写作', 4, NOW(), NOW());

-- 获取刚创建的章节ID
SET @yy_pg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '现代文阅读' LIMIT 1);
SET @yy_pg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '古代诗文阅读' LIMIT 1);
SET @yy_pg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '语言文字运用' LIMIT 1);
SET @yy_pg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_pg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@yy_pg_ch1, @yy_pg, 3, '论述类文本阅读', 1, NOW(), NOW()),
(@yy_pg_ch1, @yy_pg, 3, '实用类文本阅读', 2, NOW(), NOW()),
(@yy_pg_ch1, @yy_pg, 3, '文学类文本阅读', 3, NOW(), NOW()),
(@yy_pg_ch2, @yy_pg, 3, '文言文阅读', 1, NOW(), NOW()),
(@yy_pg_ch2, @yy_pg, 3, '古代诗歌鉴赏', 2, NOW(), NOW()),
(@yy_pg_ch2, @yy_pg, 3, '名篇名句默写', 3, NOW(), NOW()),
(@yy_pg_ch3, @yy_pg, 3, '正确使用词语(成语)', 1, NOW(), NOW()),
(@yy_pg_ch3, @yy_pg, 3, '辨析并修改病句', 2, NOW(), NOW()),
(@yy_pg_ch3, @yy_pg, 3, '语言表达简明连贯得体', 3, NOW(), NOW()),
(@yy_pg_ch4, @yy_pg, 3, '审题立意', 1, NOW(), NOW()),
(@yy_pg_ch4, @yy_pg, 3, '议论文写作', 2, NOW(), NOW()),
(@yy_pg_ch4, @yy_pg, 3, '记叙文写作', 3, NOW(), NOW());

-- ============================================================================
-- 普高英语[全国考纲] (subject_id=@en_pg, root=@en_pg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_pg_root, @en_pg, 2, '听力理解', 1, NOW(), NOW()),
(@en_pg_root, @en_pg, 2, '阅读理解', 2, NOW(), NOW()),
(@en_pg_root, @en_pg, 2, '语言知识运用', 3, NOW(), NOW()),
(@en_pg_root, @en_pg, 2, '写作', 4, NOW(), NOW());

SET @en_pg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '听力理解' LIMIT 1);
SET @en_pg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '阅读理解' LIMIT 1);
SET @en_pg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '语言知识运用' LIMIT 1);
SET @en_pg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_pg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_pg_ch1, @en_pg, 3, '短对话理解', 1, NOW(), NOW()),
(@en_pg_ch1, @en_pg, 3, '长对话与独白', 2, NOW(), NOW()),
(@en_pg_ch2, @en_pg, 3, '细节理解', 1, NOW(), NOW()),
(@en_pg_ch2, @en_pg, 3, '推理判断', 2, NOW(), NOW()),
(@en_pg_ch2, @en_pg, 3, '主旨大意', 3, NOW(), NOW()),
(@en_pg_ch3, @en_pg, 3, '完形填空', 1, NOW(), NOW()),
(@en_pg_ch3, @en_pg, 3, '语法填空', 2, NOW(), NOW()),
(@en_pg_ch4, @en_pg, 3, '短文改错', 1, NOW(), NOW()),
(@en_pg_ch4, @en_pg, 3, '书面表达', 2, NOW(), NOW());

-- ============================================================================
-- 职高语文[四川省对口升学] (subject_id=@yy_zg, root=@yy_zg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@yy_zg_root, @yy_zg, 2, '基础知识与运用', 1, NOW(), NOW()),
(@yy_zg_root, @yy_zg, 2, '现代文阅读', 2, NOW(), NOW()),
(@yy_zg_root, @yy_zg, 2, '文言文阅读', 3, NOW(), NOW()),
(@yy_zg_root, @yy_zg, 2, '写作', 4, NOW(), NOW());

SET @yy_zg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '基础知识与运用' LIMIT 1);
SET @yy_zg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '现代文阅读' LIMIT 1);
SET @yy_zg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '文言文阅读' LIMIT 1);
SET @yy_zg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@yy_zg_ch1, @yy_zg, 3, '字音字形', 1, NOW(), NOW()),
(@yy_zg_ch1, @yy_zg, 3, '词语运用', 2, NOW(), NOW()),
(@yy_zg_ch1, @yy_zg, 3, '病句辨析与修辞', 3, NOW(), NOW()),
(@yy_zg_ch2, @yy_zg, 3, '社科类文本阅读', 1, NOW(), NOW()),
(@yy_zg_ch2, @yy_zg, 3, '文学作品阅读', 2, NOW(), NOW()),
(@yy_zg_ch3, @yy_zg, 3, '常见文言实词虚词', 1, NOW(), NOW()),
(@yy_zg_ch3, @yy_zg, 3, '文言文翻译与理解', 2, NOW(), NOW()),
(@yy_zg_ch4, @yy_zg, 3, '应用文写作', 1, NOW(), NOW()),
(@yy_zg_ch4, @yy_zg, 3, '话题作文', 2, NOW(), NOW());

-- ============================================================================
-- 职高英语[四川省对口升学] (subject_id=@en_zg, root=@en_zg_root)
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_zg_root, @en_zg, 2, '基础知识', 1, NOW(), NOW()),
(@en_zg_root, @en_zg, 2, '阅读理解', 2, NOW(), NOW()),
(@en_zg_root, @en_zg, 2, '翻译', 3, NOW(), NOW()),
(@en_zg_root, @en_zg, 2, '写作', 4, NOW(), NOW());

SET @en_zg_ch1 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '基础知识' LIMIT 1);
SET @en_zg_ch2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '阅读理解' LIMIT 1);
SET @en_zg_ch3 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '翻译' LIMIT 1);
SET @en_zg_ch4 = (SELECT id FROM knowledge_nodes WHERE parent_id = @en_zg_root AND name = '写作' LIMIT 1);

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@en_zg_ch1, @en_zg, 3, '词汇与语法', 1, NOW(), NOW()),
(@en_zg_ch1, @en_zg, 3, '情景交际', 2, NOW(), NOW()),
(@en_zg_ch2, @en_zg, 3, '短文阅读', 1, NOW(), NOW()),
(@en_zg_ch2, @en_zg, 3, '任务型阅读', 2, NOW(), NOW()),
(@en_zg_ch3, @en_zg, 3, '英译汉', 1, NOW(), NOW()),
(@en_zg_ch3, @en_zg, 3, '汉译英', 2, NOW(), NOW()),
(@en_zg_ch4, @en_zg, 3, '应用文写作(书信/通知)', 1, NOW(), NOW()),
(@en_zg_ch4, @en_zg, 3, '话题写作', 2, NOW(), NOW());

SELECT 'v75: 文化课知识节点框架创建完成！' AS result;
SELECT CONCAT('普高语文: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @yy_pg;
SELECT CONCAT('普高英语: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @en_pg;
SELECT CONCAT('职高语文: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @yy_zg;
SELECT CONCAT('职高英语: ', COUNT(*), ' 个节点') FROM knowledge_nodes WHERE subject_id = @en_zg;
