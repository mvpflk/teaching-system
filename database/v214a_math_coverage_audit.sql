-- ============================================================================
-- S4: 数学题库覆盖审计
-- 找出数学[职高]每个L4节点的题库数量，定位缺口节点（<5题的优先补）
-- 执行后按 question_count ASC 排序，从0题的节点开始补
-- ============================================================================

-- 数学学科 subject_id 通过 knowledge_nodes 树反查
SET @math_subject_id = (
    SELECT DISTINCT subject_id FROM knowledge_nodes
    WHERE name = '数学[职高]' AND level = 1 AND (status = 'ACTIVE' OR status = '1' OR status IS NULL)
    LIMIT 1
);

SELECT @math_subject_id AS '数学subject_id';

-- ============================================================================
-- 核心审计: 每L4节点题目数（按缺口升序）
-- ============================================================================
SELECT
    kn.id,
    kn.name AS '知识点',
    COALESCE(qc.cnt, 0) AS '题目数',
    CASE
        WHEN COALESCE(qc.cnt, 0) = 0 THEN '🔴 零覆盖'
        WHEN COALESCE(qc.cnt, 0) < 3 THEN '🟡 不足3题'
        WHEN COALESCE(qc.cnt, 0) < 5 THEN '🟢 接近达标'
        ELSE '✅ 达标'
    END AS '状态'
FROM knowledge_nodes kn
LEFT JOIN (
    SELECT category_id, COUNT(*) AS cnt
    FROM question_bank
    WHERE status = 1
    GROUP BY category_id
) qc ON qc.category_id = kn.id
WHERE kn.subject_id = @math_subject_id
  AND kn.level = 4
  AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL)
GROUP BY kn.id, kn.name, kn.sort_order
ORDER BY COALESCE(qc.cnt, 0) ASC, kn.sort_order ASC;

-- ============================================================================
-- 汇总统计
-- ============================================================================
SELECT
    COUNT(*) AS 'L4节点总数',
    SUM(CASE WHEN COALESCE(qc.cnt, 0) = 0 THEN 1 ELSE 0 END) AS '零覆盖节点',
    SUM(CASE WHEN COALESCE(qc.cnt, 0) BETWEEN 1 AND 4 THEN 1 ELSE 0 END) AS '不足5题节点',
    SUM(CASE WHEN COALESCE(qc.cnt, 0) >= 5 THEN 1 ELSE 0 END) AS '已达标节点(≥5题)',
    SUM(COALESCE(qc.cnt, 0)) AS '总题数',
    ROUND(AVG(COALESCE(qc.cnt, 0)), 1) AS '平均每节点题数'
FROM knowledge_nodes kn
LEFT JOIN (
    SELECT category_id, COUNT(*) AS cnt FROM question_bank WHERE status = 1 GROUP BY category_id
) qc ON qc.category_id = kn.id
WHERE kn.subject_id = @math_subject_id
  AND kn.level = 4
  AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL);

-- ============================================================================
-- 按L2模块汇总（看哪个模块缺口最大）
-- ============================================================================
SELECT
    l2.id AS L2节点ID,
    l2.name AS '模块',
    COUNT(DISTINCT kn.id) AS 'L4节点数',
    COALESCE(SUM(qc.cnt), 0) AS '总题数',
    ROUND(COALESCE(AVG(qc.cnt), 0), 1) AS '均题数/节点',
    SUM(CASE WHEN COALESCE(qc.cnt, 0) = 0 THEN 1 ELSE 0 END) AS '零覆盖节点'
FROM knowledge_nodes l2
JOIN knowledge_nodes l3 ON l3.parent_id = l2.id
JOIN knowledge_nodes kn ON kn.parent_id = l3.id AND kn.level = 4
LEFT JOIN (
    SELECT category_id, COUNT(*) AS cnt FROM question_bank WHERE status = 1 GROUP BY category_id
) qc ON qc.category_id = kn.id
WHERE l2.subject_id = @math_subject_id
  AND l2.level = 2
  AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL)
GROUP BY l2.id, l2.name, l2.sort_order
ORDER BY COALESCE(AVG(qc.cnt), 0) ASC;
