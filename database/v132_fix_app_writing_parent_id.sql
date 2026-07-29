-- ============================================================================
-- v132_fix_app_writing_parent_id.sql
-- 修正：将12个应用文L4节点的parent_id从862(完形填空)改为877(应用文写作)
-- 来源：v78/v131创建应用文节点时绑定了旧ID(862)，后续ID映射未同步更新子节点parent_id
-- 幂等：可重复执行
-- ============================================================================

UPDATE knowledge_nodes 
SET parent_id = 877 
WHERE parent_id = 862 
  AND subject_id = 20 
  AND level = 4;

SELECT CONCAT('已修正 ', ROW_COUNT(), ' 个应用文节点的parent_id') AS result;
