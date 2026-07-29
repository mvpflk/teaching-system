-- v155: 修复计算机专业的学科映射数据
-- 问题：计算机专业(major_id=1)在dict_major_subject中缺少公共基础课映射

INSERT IGNORE INTO dict_major_subject (major_id, subject_id, sort_order) VALUES
(1, 20, 1),  -- 语文[职高]
(1, 22, 2),  -- 数学[职高]
(1, 24, 3),  -- 英语[职高]
(1, 4,  4),  -- 信息技术应用基础
(1, 5,  5);  -- 网络应用基础
