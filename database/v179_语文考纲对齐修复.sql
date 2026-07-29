-- ============================================================================
-- v179: 语文[职高] 考纲对齐修复 — 四川省对口升学2014版
-- 基于审查报告 docs/语文职高-考纲对齐审查报告-2026-06-30.md
-- 修复项:
--   1. 新增 L3「古诗词鉴赏」+ 7篇指定篇目L4
--   2. 新增 L3「文学常识与名句默写」+ 14篇默写篇目L4
--   3. 新增 L3「标点符号」+ 9种常用标点L4
--   4. 补齐应用文缺失3种(单据/说明书/会议记录) + 拆分条据
--   5. 新增 L3「修辞手法辨析」+ 8种修辞+易混辨析L4
--   6. 修正 exam_syllabus 考纲文本: 虚词15→18个 + 补充能力层级
-- 幂等：所有 INSERT 使用 IGNORE，UPDATE 使用条件判断
-- ============================================================================

SET @yy_zg_sid = (SELECT id FROM dict_subject WHERE subject_name = '语文[职高]' AND status = 1 LIMIT 1);
SET @yy_zg_root = (SELECT id FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 1 LIMIT 1);

-- 获取现有 L2/L3 节点ID（用于挂载新节点）
SET @l2_jczs = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '基础知识与运用' LIMIT 1);
SET @l2_xdyd = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '现代文阅读' LIMIT 1);
SET @l2_wyyd = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '文言文阅读' LIMIT 1);
SET @l2_xz   = (SELECT id FROM knowledge_nodes WHERE parent_id = @yy_zg_root AND name = '写作' LIMIT 1);

SET @l3_yysc = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '常见文言实词虚词' LIMIT 1);
SET @l3_wyfy = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '文言文翻译与理解' LIMIT 1);
SET @l3_yysc_l2 = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '词语运用' LIMIT 1);
SET @l3_bjcf = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '病句辨析与修辞' LIMIT 1);
SET @l3_yyw = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_xz AND name = '应用文写作' LIMIT 1);
SET @l3_htzw = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_xz AND name = '话题作文' LIMIT 1);

-- ============================================================================
-- Part 1: 新增 L3 节点
-- ============================================================================

-- 1a. L3「古诗词鉴赏」→ 挂在 L2「文言文阅读」下
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_wyyd, @yy_zg_sid, 3, '古诗词鉴赏', 3, NOW(), NOW());

-- 1b. L3「文学常识与名句默写」→ 挂在 L2「文言文阅读」下
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_wyyd, @yy_zg_sid, 3, '文学常识与名句默写', 4, NOW(), NOW());

-- 1c. L3「标点符号」→ 挂在 L2「基础知识与运用」下
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_jczs, @yy_zg_sid, 3, '标点符号', 4, NOW(), NOW());

-- 1d. 拆分「病句辨析与修辞」→ 新增独立 L3「修辞手法辨析」
--     原 L3「病句辨析与修辞」改名为「病句辨析」
UPDATE knowledge_nodes SET name = '病句辨析' WHERE id = @l3_bjcf AND name = '病句辨析与修辞';

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES (@l2_jczs, @yy_zg_sid, 3, '修辞手法辨析', 5, NOW(), NOW());

-- 重新获取可能变化的节点ID
SET @l3_gscjs = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '古诗词鉴赏' LIMIT 1);
SET @l3_wxcs = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_wyyd AND name = '文学常识与名句默写' LIMIT 1);
SET @l3_bd = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '标点符号' LIMIT 1);
SET @l3_xc = (SELECT id FROM knowledge_nodes WHERE parent_id = @l2_jczs AND name = '修辞手法辨析' LIMIT 1);

-- ============================================================================
-- Part 2: 古诗词鉴赏 L4 — 7篇指定阅读篇目
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_gscjs, @yy_zg_sid, 4, '《诗经》选篇：关雎、蒹葭', 1, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '唐诗鉴赏：将进酒、茅屋为秋风所破歌', 2, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '宋词鉴赏：念奴娇·赤壁怀古、雨霖铃', 3, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌意象与意境分析', 4, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌表达技巧（抒情方式/描写手法/修辞）', 5, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌思想情感与观点态度', 6, NOW(), NOW()),
(@l3_gscjs, @yy_zg_sid, 4, '诗歌语言赏析（炼字/诗眼/风格）', 7, NOW(), NOW());

-- ============================================================================
-- Part 3: 文学常识与名句默写 L4 — 14篇默写篇目 + 文学常识
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_wxcs, @yy_zg_sid, 4, '先秦诗文默写：静女、采薇、侍坐、寡人之于国、劝学', 1, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '唐宋诗文默写：师说、将进酒、琵琶行、念奴娇、雨霖铃', 2, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '宋文默写：六国论', 3, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '现代诗歌默写：我爱这土地、雨巷、致橡树', 4, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '文学体裁常识（诗歌/散文/小说/戏剧）', 5, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '重要作家作品（古今中外）', 6, NOW(), NOW()),
(@l3_wxcs, @yy_zg_sid, 4, '文化常识（称谓/历法/官职/科举/地理）', 7, NOW(), NOW());

-- ============================================================================
-- Part 4: 标点符号 L4 — 9种常用标点
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_bd, @yy_zg_sid, 4, '顿号与逗号', 1, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '分号', 2, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '问号', 3, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '引号', 4, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '省略号', 5, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '破折号', 6, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '书名号', 7, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '连接号与间隔号', 8, NOW(), NOW()),
(@l3_bd, @yy_zg_sid, 4, '标点符号综合辨析', 9, NOW(), NOW());

-- ============================================================================
-- Part 5: 修辞手法辨析 L4 — 8种修辞 + 易混辨析
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_xc, @yy_zg_sid, 4, '比喻', 1, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '比拟（拟人+拟物）', 2, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '借代', 3, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '夸张', 4, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '对偶', 5, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '排比', 6, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '反问', 7, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '设问', 8, NOW(), NOW()),
(@l3_xc, @yy_zg_sid, 4, '易混修辞辨析（借代vs借喻/比喻vs比拟/对偶vs排比/设问vs反问）', 9, NOW(), NOW());

-- ============================================================================
-- Part 6: 应用文写作 — 补齐缺失3种 + 2种细化 + 拆分条据
-- ============================================================================

-- 6a. 补充缺失的应用文类型
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_yyw, @yy_zg_sid, 4, '单据（借条/收条/领条/欠条）', 7, NOW(), NOW()),
(@l3_yyw, @yy_zg_sid, 4, '说明书', 8, NOW(), NOW()),
(@l3_yyw, @yy_zg_sid, 4, '会议记录', 9, NOW(), NOW());

-- 6b. 将「条据」拆分为「便条」和「单据」（如果存在）
--     单据内容已在上方新建，这里更新旧「条据」节点名称为「便条」
UPDATE knowledge_nodes
SET name = '便条（请假条/留言条/托事条）'
WHERE parent_id = @l3_yyw AND name = '条据';

-- 6c. 补充话题作文写作技巧L4
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_htzw, @yy_zg_sid, 4, '审题与立意', 1, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '材料作文的阅读与分析', 2, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '议论文写作结构（引论-本论-结论）', 3, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '记叙文写作要素与方法', 4, NOW(), NOW()),
(@l3_htzw, @yy_zg_sid, 4, '作文语言提升（句式变化/修辞润色）', 5, NOW(), NOW());

-- ============================================================================
-- Part 7: 文言文阅读 L4 补充 — 指定篇目 + 虚词
-- ============================================================================
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, created_at, updated_at)
VALUES
(@l3_wyfy, @yy_zg_sid, 4, '《廉颇蔺相如列传》精读', 3, NOW(), NOW()),
(@l3_wyfy, @yy_zg_sid, 4, '《子路、曾皙、冉有、公西华侍坐》精读', 4, NOW(), NOW()),
(@l3_wyfy, @yy_zg_sid, 4, '《劝学》精读', 5, NOW(), NOW()),
(@l3_wyfy, @yy_zg_sid, 4, '《师说》精读', 6, NOW(), NOW()),
(@l3_yysc, @yy_zg_sid, 4, '常见文言虚词（18个）：而何乎乃其且若所为焉也以因于与则者之', 3, NOW(), NOW());

-- ============================================================================
-- Part 8: 修正 exam_syllabus 考纲文本
-- ============================================================================
UPDATE exam_syllabus
SET content = CONCAT(
    '一、基础知识与运用：字音字形辨析（正确读音与拼写规则/多音字/形近字辨识）、词语运用（近义词辨析/成语理解与运用/感情色彩辨析）、病句辨析（语序不当/搭配不当/成分残缺或赘余/结构混乱/表意不明/不合逻辑六大类型）、标点符号（顿号/逗号/分号/问号/引号/省略号/破折号/书名号/连接号）、修辞手法辨析（比喻/比拟/借代/夸张/对偶/排比/反问/设问8种）。',
    '二、现代文阅读：社科类文本阅读（考查信息提取、分析推理）、文学作品阅读（散文与小说，考查形象分析、表达技巧、语言品味、主题理解）。',
    '三、文言文阅读：常见文言实词(120个)虚词(18个)的理解、文言文翻译(直译为主意译为辅)、文意理解与分析；指定篇目(劝学/师说/廉颇蔺相如列传/侍坐/诗经/唐诗宋词)。',
    '四、古诗词鉴赏：诗歌意象与意境分析、表达技巧鉴赏、思想情感评价、语言赏析。',
    '五、文学常识与名句默写：文学体裁常识、重要作家作品、文化常识、14篇必背名句名篇默写。',
    '六、写作：应用文写作(便条/单据/启事/通知/计划/总结/说明书/会议记录/求职信/应聘书10种，考查格式规范与语言得体)、话题作文(600字左右，考查审题立意/内容具体/语言通顺/结构完整)。',
    '能力层级分布：识记(A)10% + 理解(B)25% + 综合分析(C)15% + 表达应用(D)45% + 鉴赏评价(E)5%。试卷150分/150分钟。'
),
    updated_at = NOW()
WHERE subject_id = @yy_zg_sid AND exam_type = 'DUIKOU';

-- ============================================================================
-- 验证
-- ============================================================================
SELECT CONCAT('v179: 语文[职高] 考纲对齐修复完成！') AS result;
SELECT CONCAT('节点总数: ', COUNT(*)) AS stat FROM knowledge_nodes WHERE subject_id = @yy_zg_sid;
SELECT CONCAT('L3任务: ', COUNT(*)) FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 3;
SELECT CONCAT('L4知识点: ', COUNT(*)) FROM knowledge_nodes WHERE subject_id = @yy_zg_sid AND level = 4;

-- 列出所有L3节点（确认结构）
SELECT CONCAT('  L3: ', name) FROM knowledge_nodes
WHERE subject_id = @yy_zg_sid AND level = 3
ORDER BY sort_order, id;

-- 确认考纲文本已更新
SELECT CONCAT('考纲文本长度: ', CHAR_LENGTH(content), '字符（原~280字符，现应>600字符）')
FROM exam_syllabus WHERE subject_id = @yy_zg_sid AND exam_type = 'DUIKOU';
