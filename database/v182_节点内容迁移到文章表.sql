-- ============================================================================
-- v182: 语文[职高] 知识节点 → 知识文章迁移
-- 将 knowledge_nodes.content 迁移到 knowledge_articles
-- knowledge_articles 用于前端发现页（KnowledgeDiscover.vue）
-- ============================================================================

-- 语文[职高] subject_id = 20

-- Step 1: 删除旧的语文文章（如果有）
DELETE FROM knowledge_articles WHERE subject_id = 20;

-- Step 2: 为每个 L4 节点创建文章
-- 使用自连接查询找到 L3(任务) 和 L2(章节) 祖先名称

INSERT INTO knowledge_articles (title, content_md, excerpt, subject_id, chapter, task, difficulty, status, view_count, created_at, updated_at)
SELECT
    l4.name AS title,
    l4.content AS content_md,
    CONCAT(LEFT(COALESCE(l4.content, ''), 200), CASE WHEN CHAR_LENGTH(COALESCE(l4.content, '')) > 200 THEN '...' ELSE '' END) AS excerpt,
    20 AS subject_id,
    COALESCE(l2.name, '未分类') AS chapter,
    COALESCE(l3.name, '未分类') AS task,
    1 AS difficulty,
    'PUBLISHED' AS status,
    0 AS view_count,
    NOW() AS created_at,
    NOW() AS updated_at
FROM knowledge_nodes l4
LEFT JOIN knowledge_nodes l3 ON l4.parent_id = l3.id AND l3.level = 3
LEFT JOIN knowledge_nodes l2 ON l3.parent_id = l2.id AND l2.level = 2
WHERE l4.subject_id = 20
  AND l4.level = 4
  AND l4.content IS NOT NULL
  AND l4.content != '';

-- Step 3: 验证
SELECT CONCAT('v182: 迁移完成！') AS result;
SELECT CONCAT('knowledge_articles 语文文章数: ', COUNT(*)) AS result FROM knowledge_articles WHERE subject_id = 20;
SELECT CONCAT('knowledge_nodes 语文L4节点数: ', COUNT(*)) AS result FROM knowledge_nodes WHERE subject_id = 20 AND level = 4;
