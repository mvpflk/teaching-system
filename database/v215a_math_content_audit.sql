-- ============================================================================
-- S5: 数学知识节点 content 覆盖审计
-- 找出 content 为空的数学 L4 节点，按需填充教学内容
-- 执行后按节点排序，优先填充考纲权重 HIGH 的节点
-- ============================================================================

SET @math_subject_id = (
    SELECT DISTINCT subject_id FROM knowledge_nodes
    WHERE name = '数学[职高]' AND level = 1 AND (status = 'ACTIVE' OR status = '1' OR status IS NULL)
    LIMIT 1
);

-- ============================================================================
-- 核心审计: content 为空的 L4 节点
-- ============================================================================
SELECT
    kn.id,
    kn.name AS '知识点',
    kn.exam_weight AS '考纲权重',
    CASE
        WHEN kn.content IS NULL OR kn.content = '' THEN '🔴 无内容'
        WHEN CHAR_LENGTH(kn.content) < 100 THEN '🟡 内容过短(<100字)'
        ELSE '✅ 有内容'
    END AS '内容状态',
    CHAR_LENGTH(COALESCE(kn.content, '')) AS '内容字数'
FROM knowledge_nodes kn
WHERE kn.subject_id = @math_subject_id
  AND kn.level = 4
  AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL)
ORDER BY
    CASE WHEN kn.content IS NULL OR kn.content = '' THEN 0 ELSE 1 END ASC,
    CASE kn.exam_weight WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'LOW' THEN 2 ELSE 3 END,
    kn.sort_order ASC;

-- ============================================================================
-- 汇总统计
-- ============================================================================
SELECT
    COUNT(*) AS 'L4节点总数',
    SUM(CASE WHEN content IS NULL OR content = '' THEN 1 ELSE 0 END) AS '无内容节点',
    SUM(CASE WHEN CHAR_LENGTH(COALESCE(content, '')) BETWEEN 1 AND 99 THEN 1 ELSE 0 END) AS '内容过短(<100字)',
    SUM(CASE WHEN CHAR_LENGTH(COALESCE(content, '')) >= 100 THEN 1 ELSE 0 END) AS '有内容节点(≥100字)',
    ROUND(AVG(CHAR_LENGTH(COALESCE(content, ''))), 0) AS '平均字数'
FROM knowledge_nodes
WHERE subject_id = @math_subject_id
  AND level = 4
  AND (status = 'ACTIVE' OR status = '1' OR status IS NULL);

-- ============================================================================
-- 按 L2 模块汇总（看哪个模块 content 填充最差）
-- ============================================================================
SELECT
    l2.name AS '模块',
    COUNT(DISTINCT kn.id) AS 'L4节点数',
    SUM(CASE WHEN kn.content IS NULL OR kn.content = '' THEN 1 ELSE 0 END) AS '无内容节点',
    ROUND(AVG(CHAR_LENGTH(COALESCE(kn.content, ''))), 0) AS '平均字数'
FROM knowledge_nodes l2
JOIN knowledge_nodes l3 ON l3.parent_id = l2.id
JOIN knowledge_nodes kn ON kn.parent_id = l3.id AND kn.level = 4
WHERE l2.subject_id = @math_subject_id AND l2.level = 2
  AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL)
GROUP BY l2.id, l2.name, l2.sort_order
ORDER BY SUM(CASE WHEN kn.content IS NULL OR kn.content = '' THEN 1 ELSE 0 END) DESC;
