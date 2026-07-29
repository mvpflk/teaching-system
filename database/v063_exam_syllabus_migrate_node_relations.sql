-- ============================================================================
-- V063: exam_syllabus_node_relation 数据迁移
-- 从考纲 title/content 匹配 knowledge_nodes.name，填充关联表
-- 幂等: INSERT IGNORE 跳过已有关联
-- 执行后请运行末尾的验证 SQL
-- ============================================================================
START TRANSACTION;

-- ============================================================================
-- 策略1: 考纲 knowledge_dim 精确匹配 L3/L4 节点名（优先）
-- 例如: exam_syllabus.knowledge_dim = '三角函数' → knowledge_nodes.name LIKE '%三角函数%'
-- ============================================================================
INSERT IGNORE INTO exam_syllabus_node_relation (syllabus_id, node_id)
SELECT DISTINCT es.id, kn.id
FROM exam_syllabus es
JOIN knowledge_nodes kn ON kn.subject_id = es.subject_id
    AND kn.level >= 3
    AND kn.status IN ('ACTIVE', '1') OR kn.status IS NULL
    AND kn.name = es.knowledge_dim
WHERE es.knowledge_dim IS NOT NULL AND es.knowledge_dim != ''
  AND es.knowledge_dim != 'BOTH' AND es.knowledge_dim != 'THEORY' AND es.knowledge_dim != 'PRACTICE';

-- ============================================================================
-- 策略2: 考纲 title 关键词模糊匹配 L3/L4 节点名
-- ============================================================================
INSERT IGNORE INTO exam_syllabus_node_relation (syllabus_id, node_id)
SELECT DISTINCT es.id, kn.id
FROM exam_syllabus es
JOIN knowledge_nodes kn ON kn.subject_id = es.subject_id
    AND kn.level >= 3
    AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL)
    AND kn.name LIKE CONCAT('%', SUBSTRING_INDEX(es.title, ' ', 1), '%')
WHERE es.title IS NOT NULL AND es.title != '';

-- ============================================================================
-- 策略3: 考纲 content 中包含节点名（更精确但覆盖更少）
-- ============================================================================
INSERT IGNORE INTO exam_syllabus_node_relation (syllabus_id, node_id)
SELECT DISTINCT es.id, kn.id
FROM exam_syllabus es
JOIN knowledge_nodes kn ON kn.subject_id = es.subject_id
    AND kn.level >= 3
    AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL)
    AND CHAR_LENGTH(kn.name) >= 4  -- 节点名至少4字，避免误匹配
    AND es.content LIKE CONCAT('%', kn.name, '%')
WHERE es.content IS NOT NULL AND es.content != '';

-- ============================================================================
-- 策略4: 考纲 syllabus_meta JSON 中引用节点名
-- ============================================================================
INSERT IGNORE INTO exam_syllabus_node_relation (syllabus_id, node_id)
SELECT DISTINCT es.id, kn.id
FROM exam_syllabus es
JOIN knowledge_nodes kn ON kn.subject_id = es.subject_id
    AND kn.level >= 3
    AND (kn.status = 'ACTIVE' OR kn.status = '1' OR kn.status IS NULL)
    AND CHAR_LENGTH(kn.name) >= 4
WHERE es.syllabus_meta IS NOT NULL
  AND es.syllabus_meta != ''
  AND es.syllabus_meta LIKE CONCAT('%', kn.name, '%');

COMMIT;

-- ============================================================================
-- 验证 SQL（执行迁移后手动运行）:
-- ============================================================================

-- 1. 查看新关联数量
-- SELECT COUNT(*) AS total_relations FROM exam_syllabus_node_relation;

-- 2. 查看各学科关联覆盖
-- SELECT kn.subject_id, COUNT(DISTINCT esnr.syllabus_id) AS matched_syllabi,
--        COUNT(DISTINCT esnr.node_id) AS matched_nodes
-- FROM exam_syllabus_node_relation esnr
-- JOIN exam_syllabus es ON es.id = esnr.syllabus_id
-- JOIN knowledge_nodes kn ON kn.id = esnr.node_id
-- GROUP BY kn.subject_id
-- ORDER BY kn.subject_id;

-- 3. 查看仍未关联的考纲（需手动处理）
-- SELECT es.id, es.subject_id, es.title, es.knowledge_dim
-- FROM exam_syllabus es
-- LEFT JOIN exam_syllabus_node_relation esnr ON es.id = esnr.syllabus_id
-- WHERE esnr.syllabus_id IS NULL
--   AND es.subject_id IS NOT NULL
-- ORDER BY es.subject_id, es.title;

-- 4. 手动关联示例（根据验证结果修正）
-- INSERT INTO exam_syllabus_node_relation (syllabus_id, node_id)
-- VALUES (考纲ID, 知识点节点ID);
