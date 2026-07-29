-- ============================================================================
-- V055: 计算机学科 AI 出题优化
--   1. 计算机学科参考样题写入 teacher_reference_questions
--   2. 知识节点 content 微调（高频考点补充）
-- ============================================================================

-- ═══ Section 1: 计算机学科参考样题 ═══
-- 每个学科每种题型 1-2 道，共 ~20 道，用于 AI 出题的 Few-Shot 风格对齐

INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('信息技术应用基础', 'SINGLE_CHOICE', '{"questionText": "计算机的五大特点不包括以下哪项（   ）。", "options": ["A. 运算速度快", "B. 计算精确性高", "C. 存储容量大", "D. 自主创新能力"], "correctAnswer": "D"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('信息技术应用基础', 'SINGLE_CHOICE', '{"questionText": "计算机最广泛的应用领域是（   ）。", "options": ["A. 科学计算", "B. 信息处理（数据处理）", "C. 过程控制", "D. 人工智能"], "correctAnswer": "B"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('信息技术应用基础', 'SINGLE_CHOICE', '{"questionText": "在 Windows 7 中，以下关于文件的说法，正确的是（   ）。", "options": ["A. 文件名不能包含空格", "B. 文件名最多256个字符", "C. 同一文件夹下不能有同名文件", "D. 文件扩展名决定文件的默认打开方式"], "correctAnswer": "D"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('信息技术应用基础', 'MULTI_CHOICE', '{"questionText": "下列属于计算机输入设备的有（   ）。", "options": ["A. 键盘", "B. 鼠标", "C. 显示器", "D. 扫描仪", "E. 打印机"], "correctAnswer": "ABD"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('信息技术应用基础', 'TRUE_FALSE', '{"questionText": "在 Windows 7 中，回收站是硬盘上的一块区域。", "options": ["正确", "错误"], "correctAnswer": "A"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('信息技术应用基础', 'TRUE_FALSE', '{"questionText": "冯·诺依曼体系结构的计算机采用存储程序工作原理。", "options": ["正确", "错误"], "correctAnswer": "A"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('网络应用基础', 'SINGLE_CHOICE', '{"questionText": "OSI七层参考模型从下到上第三层是（   ）。", "options": ["A. 物理层", "B. 数据链路层", "C. 网络层", "D. 传输层"], "correctAnswer": "C"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('网络应用基础', 'SINGLE_CHOICE', '{"questionText": "以下IP地址中，属于C类地址的是（   ）。", "options": ["A. 10.0.0.1", "B. 172.16.0.1", "C. 192.168.1.1", "D. 224.0.0.1"], "correctAnswer": "C"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('网络应用基础', 'MULTI_CHOICE', '{"questionText": "下列属于网络传输介质的有（   ）。", "options": ["A. 双绞线", "B. 光纤", "C. 同轴电缆", "D. 无线电波", "E. 交换机"], "correctAnswer": "ABCD"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('网络应用基础', 'TRUE_FALSE', '{"questionText": "TCP协议是面向连接的可靠传输协议。", "options": ["正确", "错误"], "correctAnswer": "A"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('办公应用基础', 'SINGLE_CHOICE', '{"questionText": "在 Word 中，若要设置段落的首行缩进2字符，应在（   ）对话框中设置。", "options": ["A. 字体", "B. 段落", "C. 页面设置", "D. 样式"], "correctAnswer": "B"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('办公应用基础', 'SINGLE_CHOICE', '{"questionText": "在 Excel 中，公式 =SUM(A1:A10) 的功能是（   ）。", "options": ["A. 求A1到A10的平均值", "B. 求A1到A10的最大值", "C. 求A1到A10的和", "D. 统计A1到A10的个数"], "correctAnswer": "C"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('办公应用基础', 'MULTI_CHOICE', '{"questionText": "在 PowerPoint 中，可以插入的对象包括（   ）。", "options": ["A. 图片", "B. 音频", "C. 视频", "D. 图表", "E. SmartArt图形"], "correctAnswer": "ABCDE"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('办公应用基础', 'TRUE_FALSE', '{"questionText": "在 Excel 中，单元格引用 $A$1 是绝对引用。", "options": ["正确", "错误"], "correctAnswer": "A"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('Access', 'SINGLE_CHOICE', '{"questionText": "在 Access 中，数据表的主键的作用是（   ）。", "options": ["A. 提高查询速度", "B. 唯一标识每一条记录", "C. 建立表间关系", "D. 设置字段格式"], "correctAnswer": "B"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('Access', 'SINGLE_CHOICE', '{"questionText": "SQL 语句中，用于从数据表中查询数据的命令是（   ）。", "options": ["A. INSERT", "B. UPDATE", "C. DELETE", "D. SELECT"], "correctAnswer": "D"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('Access', 'MULTI_CHOICE', '{"questionText": "Access 2010 数据库中，可以包含的对象有（   ）。", "options": ["A. 表", "B. 查询", "C. 窗体", "D. 报表", "E. 宏"], "correctAnswer": "ABCDE"}', 'SYSTEM', 1);
INSERT IGNORE INTO teacher_reference_questions (subject, question_type, content_json, source, enabled) VALUES
('Access', 'TRUE_FALSE', '{"questionText": "在 Access 中，一个表只能有一个主键。", "options": ["正确", "错误"], "correctAnswer": "A"}', 'SYSTEM', 1);

-- ═══ Section 2: 验证 ═══
SELECT subject, question_type, COUNT(*) as cnt FROM teacher_reference_questions WHERE source = 'SYSTEM' GROUP BY subject, question_type ORDER BY subject, question_type;
