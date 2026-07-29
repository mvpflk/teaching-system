-- ============================================================================
-- v77: 语文/英语作文写作评分量规模板
-- 普高+职高，共4套Rubric
-- 幂等：INSERT IGNORE
-- ============================================================================

-- 语文普高作文
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (100, '高考语文作文评分标准', 1, 3, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(100, '内容', 0.33, '切合题意、中心突出、内容充实、思想健康、感情真挚',
 '{"A": "切合题意，中心突出，内容充实，感情真挚", "B": "符合题意，中心明确，内容较充实，感情真实", "C": "基本符合题意，中心基本明确，内容单薄", "D": "偏离题意，中心不明确，内容不当"}'),
(100, '表达', 0.33, '符合文体要求、结构严谨、语言流畅、字迹工整',
 '{"A": "结构严谨，语言流畅，符合文体要求，字迹工整", "B": "结构完整，语言通顺，基本符合文体要求，字迹清楚", "C": "结构基本完整，语言基本通顺，字迹基本清楚", "D": "结构混乱，语言不通顺，语病多，字迹潦草"}'),
(100, '发展', 0.34, '深刻、丰富、有文采、有创新',
 '{"A": "观点深刻，材料丰富，语言有文采，见解新颖", "B": "观点较深刻，材料较丰富，语言较有文采", "C": "略有深刻性或丰富性", "D": "无明显特色"}');

-- 语文职高作文
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (101, '对口升学语文作文评分标准', 1, 4, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(101, '内容', 0.38, '符合题意、内容具体、感情真实、思想健康',
 '{"A": "切合题意，内容具体充实，感情真实", "B": "符合题意，内容较具体，感情较真实", "C": "基本符合题意，内容不够具体", "D": "偏离题意，内容空泛"}'),
(101, '语言', 0.37, '语句通顺、表达清晰、格式规范、标点正确',
 '{"A": "语句通顺流畅，表达清晰准确", "B": "语句较通顺，表达较清晰", "C": "语句基本通顺，偶有语病", "D": "语句不通顺，语病较多"}'),
(101, '结构', 0.25, '条理清楚、分段合理、结构完整',
 '{"A": "条理清晰，层次分明，结构严谨", "B": "条理较清晰，结构完整", "C": "条理基本清晰，结构基本完整", "D": "条理不清，结构混乱"}');

-- 英语普高写作
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (102, '高考英语书面表达评分标准', 1, 3, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(102, '内容要点', 0.33, '覆盖所有内容要点，表达清楚',
 '{"A": "覆盖所有要点，内容充实", "B": "覆盖大部分要点，内容较充实", "C": "遗漏部分要点，内容基本清楚", "D": "遗漏大部分要点，内容不清"}'),
(102, '语言质量', 0.34, '词汇丰富，句式多样，衔接自然',
 '{"A": "词汇丰富准确，句式灵活多样，衔接紧凑", "B": "词汇较丰富，句式有一定变化，衔接较自然", "C": "词汇基本够用，句式单一，衔接不够自然", "D": "词汇贫乏，表达困难"}'),
(102, '语法准确性', 0.33, '语法错误少，拼写和标点规范',
 '{"A": "语法结构准确，拼写标点正确", "B": "少量语法错误，不影响理解", "C": "一些语法错误，个别影响理解", "D": "语法错误较多，影响理解"}');

-- 英语职高写作
INSERT IGNORE INTO rubric (id, name, school_id, stage_id, created_by, scope, created_at)
VALUES (103, '对口升学英语写作评分标准', 1, 4, 1, 'PUBLIC', NOW());

INSERT IGNORE INTO rubric_dimension (rubric_id, name, weight, description, levels_json)
VALUES
(103, '内容完整', 0.33, '覆盖主要内容点，表达基本意思',
 '{"A": "全面覆盖要点，内容完整", "B": "覆盖大部分要点", "C": "覆盖部分要点", "D": "内容不完整"}'),
(103, '语言表达', 0.34, '语句基本通顺，用词基本准确',
 '{"A": "语句通顺，用词准确", "B": "语句较通顺，用词较准确", "C": "语句基本通顺，有少量错误", "D": "错误较多，影响理解"}'),
(103, '格式规范', 0.33, '格式正确，书写规范，拼写基本正确',
 '{"A": "格式正确规范，书写整洁", "B": "格式较规范", "C": "格式基本规范", "D": "格式不规范"}');

SELECT 'v77: 作文写作评分量规模板创建完成！' AS result;
SELECT CONCAT(r.name, ' (', COUNT(d.id), '个维度)') AS detail
FROM rubric r LEFT JOIN rubric_dimension d ON r.id = d.rubric_id
WHERE r.id BETWEEN 100 AND 103
GROUP BY r.id, r.name;
