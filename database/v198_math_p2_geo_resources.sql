-- ============================================================================
-- v198: 数学[职高] P2-3 几何章节资源增强
-- 为函数图像/几何节点添加 GeoGebra/图形化学习资源链接
-- 覆盖: 立体几何(4) + 函数图像(4) + 三角图像(2) + 解析几何核心(3) = 13节点
-- 使用 JSON_SET 操作 learning_resources JSON 字段
-- ============================================================================
SET NAMES utf8mb4;

-- ══════════════════════════════════════════
-- 立体几何节点 (3149-3152)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/geometric-surface-area'
) WHERE subject_id=22 AND level=4 AND name LIKE '%常见几何体的表面积%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/3d-geometry-volumes'
) WHERE subject_id=22 AND level=4 AND name LIKE '%常见几何体的体积%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/line-plane-relationships'
) WHERE subject_id=22 AND level=4 AND name LIKE '%线面平行与垂直%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/plane-relationships'
) WHERE subject_id=22 AND level=4 AND name LIKE '%面面平行与垂直%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

-- ══════════════════════════════════════════
-- 函数图像节点 (3101, 3107, 3109)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/quadratic-function-explorer'
) WHERE subject_id=22 AND level=4 AND name LIKE '%二次函数图像与性质%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/exponential-function-graph'
) WHERE subject_id=22 AND level=4 AND name LIKE '%指数函数的图像与性质%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/logarithmic-function-graph'
) WHERE subject_id=22 AND level=4 AND name LIKE '%对数函数的图像与性质%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

-- ══════════════════════════════════════════
-- 三角函数图像节点 (3122, 3123)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/sine-cosine-transformation'
) WHERE subject_id=22 AND level=4 AND name LIKE '%正弦、余弦函数的图像与性质%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/tangent-function-graph'
) WHERE subject_id=22 AND level=4 AND name LIKE '%正切函数的图像与性质%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

-- ══════════════════════════════════════════
-- 解析几何节点 (3159, 3191, 3193)
-- ══════════════════════════════════════════

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/circle-general-form'
) WHERE subject_id=22 AND level=4 AND name LIKE '%圆的一般方程%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/ellipse-definition'
) WHERE subject_id=22 AND level=4 AND name LIKE '%椭圆的标准方程%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

UPDATE knowledge_nodes SET learning_resources = JSON_SET(
    COALESCE(learning_resources, '{}'), '$.geoGebraUrl',
    'https://www.geogebra.org/m/parabola-definition'
) WHERE subject_id=22 AND level=4 AND name LIKE '%抛物线的标准方程%'
  AND (learning_resources IS NULL OR JSON_EXTRACT(learning_resources, '$.geoGebraUrl') IS NULL);

SELECT 'v198 deployed — 13个几何/图像节点 GeoGebra 资源已添加' AS result;
-- ============================================================================
