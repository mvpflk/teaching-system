-- ============================================================================
-- v152: 英语[职高]知识树重构 — 翻译→补全对话+短文改错
-- 1. 翻译 L2 及子节点 → DEPRECATED
-- 2. 新增 补全对话 L2 (sort=4) + 短文改错 L2 (sort=5)
-- 3. 写作 L2 sort_order 6→6（不变）
-- 4. 新增 L3/L4 节点含教学内容
-- 幂等: 基于 name+subject_id 防重，DEPRECATED 可重复执行
-- ============================================================================
SET NAMES utf8mb4;

SET @eng_root = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND level=1 LIMIT 1);

-- ══════════════════════════════════════════
-- 1. 翻译 → DEPRECATED
-- ══════════════════════════════════════════
SET @translate_id = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='翻译' AND level=2 LIMIT 1);

UPDATE knowledge_nodes SET status='DEPRECATED', updated_at=NOW()
WHERE id = @translate_id AND status='ACTIVE';

-- 子节点也 DEPRECATED
UPDATE knowledge_nodes SET status='DEPRECATED', updated_at=NOW()
WHERE parent_id = @translate_id AND status='ACTIVE';

-- ══════════════════════════════════════════
-- 2. 新增 L2: 补全对话 (sort=4)
-- ══════════════════════════════════════════
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT @eng_root, 24, 2, '补全对话', 4, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_nodes WHERE subject_id=24 AND name='补全对话' AND level=2);

SET @l2_dialog = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='补全对话' AND level=2 LIMIT 1);

-- L3: 补全对话
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
VALUES (@l2_dialog, 24, 3, '补全对话', 1, 'ACTIVE');

SET @l3_dialog = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='补全对话' AND level=3 LIMIT 1);

-- L4: 补全对话子知识点 (4个)
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_dialog, 24, 4, '问候告别与感谢道歉',
'【一句话定义】日常交际中最基本的礼貌用语，涵盖见面问候、告别寒暄、感谢回应和道歉接受等场景。

【具体说明】问候: Good morning/afternoon/evening, How are you?/How is it going?；应答: I''m fine/Very well/Not bad；告别: Goodbye/See you/ Take care；感谢: Thank you/Thanks a lot/Many thanks；道歉: I''m sorry/I apologize/Excuse me。

【常见错误】①"How are you?"机械应答"I''m fine"而不考虑实际语境；②感谢和道歉的回应混淆（用"You''re welcome"回应道歉）。

【考试方向】补全对话场景题，通常出现在第II卷。每年必考1-2题问候/感谢类对话。',
1, 'ACTIVE');

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_dialog, 24, 4, '邀请预约与祝愿祝贺',
'【一句话定义】表达邀请和预约的常用句型，以及表达祝愿和祝贺的场景用语。

【具体说明】邀请: Would you like to.../Do you want to.../How about...；应答: I''d love to/Sorry, I''m afraid not；预约: I''d like to make an appointment/Is it convenient to...；祝愿: Good luck/Congratulations/Wish you success/Happy birthday。

【常见错误】①邀请接受/拒绝的礼貌程度不当（拒绝时缺少理由）；②Congratulations和Good luck的使用场景混淆（前者用于已达成的事，后者用于即将发生的事）。

【考试方向】补全对话高频场景。邀请类常结合时间/地点信息，需从选项中匹配上下文。',
2, 'ACTIVE');

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_dialog, 24, 4, '请求帮助与提供帮助',
'【一句话定义】表达请求对方帮助或主动提供帮助的交际用语，及其恰当回应。

【具体说明】请求: Can/Could you please.../Would you mind.../I wonder if...；提供: Can I help you/Let me.../Would you like me to...；应答: Sure/Of course/Certainly/That''s very kind of you/No, thanks。

【常见错误】①Could you please与Would you mind的答语混淆（后者肯定回答用"No, not at all"）；②提供帮助接受与拒绝的礼貌表达使用不当。

【考试方向】补全对话常见考点。涉及购物/问路/就餐等具体场景中的请求与提供用语。',
3, 'ACTIVE');

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_dialog, 24, 4, '建议劝告与禁止警告',
'【一句话定义】表达建议、劝告、禁止和警告的交际用语，涵盖委婉建议到直接警告的不同语气层级。

【具体说明】建议: You''d better/Why not/How about/Shall we/I suggest；劝告: You should/ought to/It''s better to；禁止: You mustn''t/Don''t/No smoking；警告: Look out/Watch out/Be careful。

【常见错误】①You''d better和You should的语气程度把握不当；②建议的回答混淆（接受用Good idea，拒绝用I''m afraid I can''t）。

【考试方向】补全对话中建议类题目出现频率最高，常设置在校园生活和健康话题场景中。',
4, 'ACTIVE');

-- ══════════════════════════════════════════
-- 3. 新增 L2: 短文改错 (sort=5)，
--    写作保持 sort=6
-- ══════════════════════════════════════════
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
SELECT @eng_root, 24, 2, '短文改错', 5, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_nodes WHERE subject_id=24 AND name='短文改错' AND level=2);

SET @l2_error = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='短文改错' AND level=2 LIMIT 1);

-- L3: 短文改错
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order, status)
VALUES (@l2_error, 24, 3, '短文改错', 1, 'ACTIVE');

SET @l3_error = (SELECT id FROM knowledge_nodes WHERE subject_id=24 AND name='短文改错' AND level=3 LIMIT 1);

-- L4: 短文改错子知识点 (4个)
INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_error, 24, 4, '动词时态语态改错',
'【一句话定义】短文改错中最高频的错误类型（占比约30%），考查动词时态一致性和语态正确使用。

【具体说明】时态错误: 一般现在时第三人称单数-s缺失、一般过去时动词形式错误、完成时have/has+过去分词结构错误、时态前后不一致（如yesterday却用一般现在时）。语态错误: 主动被动混淆，如The book was written by him写成The book wrote by him。

【常见错误】①时间状语与时态不匹配（last year≠一般现在时）；②不规则动词过去式记错（如go→went误用goed）；③情态动词后未跟动词原形。

【考试方向】短文改错必考1-2处，标记为"基础"难度。通读全文确定时态基调后逐句检查。',
1, 'ACTIVE');

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_error, 24, 4, '名词冠词介词改错',
'【一句话定义】名词单复数、冠词使用和介词搭配的改错类型，占短文改错约25%。

【具体说明】名词: 可数不可数混淆（information/furniture不用-s）、单复数形式错误（child→children）、名词所有格错误（''s的使用）。冠词: a/an混淆、the的缺失或多余、零冠词与定冠词混用。介词: 固定搭配错误（depend on写成depend of）、时间介词混淆（in Monday→on Monday）。

【常见错误】①可数名词单数缺少冠词（a/an/the）；②介词in/on/at时间用法混淆；③there be句型后接可数名词单数时缺a/an。

【考试方向】短文改错基础题，每次考试至少出现1处名词或冠词错误。注意上下文的一致性。',
2, 'ACTIVE');

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_error, 24, 4, '主谓一致与非谓语改错',
'【一句话定义】主语和谓语在"数"上的一致性错误，以及非谓语动词（不定式/动名词/分词）使用错误的识别与改正。

【具体说明】主谓一致: 第三人称单数-s缺失、就近原则（either...or/neither...nor/there be）、集合名词（team/class/family）作主语谓语的数。非谓语: 不定式to do与动名词V-ing混用（enjoy doing/like to do）、分词主动被动混淆（boring vs bored, exciting vs excited）。

【常见错误】①主语后有with/together with仍按复数处理谓语；②非谓语逻辑主语与句子主语不一致；③现在分词和过去分词作定语混淆。

【考试方向】短文改错中等难度题。主谓一致每年必考1处，非谓语约两年出现1次。注意找主语中心词。',
3, 'ACTIVE');

INSERT IGNORE INTO knowledge_nodes (parent_id, subject_id, level, name, content, sort_order, status) VALUES
(@l3_error, 24, 4, '连词形容词副词改错',
'【一句话定义】连词误用、形容词副词混淆、比较级和最高级错误等改错类型。

【具体说明】连词: because/so不能同时用、although/but不能同时用、and/or在否定句中选择。形容词/副词: 形容词修饰名词/系动词后、副词修饰动词/形容词/其他副词、good/well/hard/hardly等易混词。比较级: more + 形容词er重复（more better）、than后代词格错误。

【常见错误】①because和so在同一句中出现（中文直译"因为...所以..."）；②系动词后应用形容词而非副词（He looks happily→He looks happy）；③比较级形式错误。

【考试方向】短文改错第3-4题位置，中等难度。注意检查逻辑连词是否匹配句子关系（因果/转折/并列）。',
4, 'ACTIVE');

-- ══════════════════════════════════════════
-- 4. 确认写作 sort_order
-- ══════════════════════════════════════════
UPDATE knowledge_nodes SET sort_order=6, updated_at=NOW()
WHERE subject_id=24 AND name='写作' AND level=2 AND sort_order!=6;

SELECT CONCAT('v152: 翻译→DEPRECATED, 补全对话(sort=4)+短文改错(sort=5) created, 写作(sort=6)') AS result;
