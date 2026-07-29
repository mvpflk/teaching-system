-- ============================================================================
-- v150_fixed: knowledge_nodes 新增 learning_resources 列（JSON类型）
--       为数学[职高]前10个核心L3知识点填充视频+例题+练习资源
--       幂等安全：动态检测列是否存在
--       COALESCE(..., -1) 防止子查询返回NULL进入JSON_ARRAY
-- ============================================================================
SET NAMES utf8mb4;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_nodes'
  AND COLUMN_NAME = 'learning_resources');
SET @sql_cmd = IF(@col_exists = 0,
  'ALTER TABLE knowledge_nodes ADD COLUMN learning_resources JSON DEFAULT NULL COMMENT ''学习资源:{videoUrl,exampleIds:[],practiceIds:[]}'' AFTER deprecation_note',
  'SELECT ''SKIP'' AS msg');
PREPARE stmt FROM @sql_cmd; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1. 集合的概念与表示
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='集合的概念与表示' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+集合的概念与表示',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%设集合A={x|-1<x≤3}%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%"x>2"是"x>3"%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%已知集合A={x|x^2-3x+2=0}%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%"a=0"是"ab=0"%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%从1,2,3,4,5中随机取%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%数据：1,2,2,3,4,4,5的众数%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%C₅²%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%A₄²%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 2. 集合间的关系
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='集合间的关系' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+集合间的关系+子集交集并集补集',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%设集合A={x|-1<x≤3}%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%"x>2"是"x>3"%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%已知集合A={x|x^2-3x+2=0}%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%充分不必要条件%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%从1,2,3,4,5中随机取%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%同时掷两枚骰子%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%数据2,3,4,5,6,a的平均数%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%必然事件的概率为1%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 3. 集合的运算
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='集合的运算' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+集合的运算+并集交集补集',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%则A∪B=%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式(x+2)(x-3)<0的解集%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%函数y=√(3-x)+√(x-1)的定义域%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式|2x-3|≤1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%从1,2,3,4,5中随机取%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%C₅²%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%3x²的导数%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%x³-3x在x=1%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 4. 不等式的性质
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='不等式的性质' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+不等式的性质+基本不等式',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式(x+2)(x-3)<0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%函数y=√(3-x)+√(x-1)%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式|2x-3|≤1%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%直线2x-y+3=0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%圆x^2+y^2-4x+2y=0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%椭圆x^2/16+y^2/9=1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%点P(1,2)到直线3x+4y-5=0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%过点(2,1)且斜率为1/2%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 5. 一元二次不等式
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='一元二次不等式' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+一元二次不等式+解法',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式(x+2)(x-3)<0的解集%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%函数y=√(3-x)+√(x-1)的定义域%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式|2x-3|≤1的解集%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%圆的半径为2，则其表面积%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%圆柱底面半径r=2，高h=3%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%正方体的棱长为3%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%圆锥底面半径r=3，母线l=5%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%从6名同学中选3名%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 6. 含绝对值的不等式
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='含绝对值的不等式' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+含绝对值的不等式+解法',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式|2x-3|≤1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式(x+2)(x-3)<0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%函数y=√(3-x)+√(x-1)%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%(x+1)^4展开式%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%(x+2)^3展开式%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%x^2-2x在区间%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%曲线y=x^2在点(1,1)%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%-x^2+4x+1%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 7. 函数的概念与表示
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数概念与表示' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+函数概念与表示+定义域值域',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%3x²的导数%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%x³-3x在x=1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%曲线y=x²在点(1,1)%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%x²-2x在区间%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%-x²+4x+1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%导数大于0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%直线2x-y+3=0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%点P(1,2)到直线3x+4y-5=0%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 8. 函数的性质
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='函数的性质' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+函数的性质+单调性奇偶性',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%x³-3x在x=1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%x²-2x在区间%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%-x²+4x+1%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%导数大于0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%3x²的导数%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%曲线y=x²在点%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%已知|a|=3%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%向量a=(1,2)%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 9. 二次函数
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二次函数' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+二次函数+图像与性质',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%-x²+4x+1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%x²-2x在区间%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%曲线y=x²在点(1,1)%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%不等式(x+2)(x-3)<0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%3x²%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%圆x^2+y^2-4x+2y=0%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%椭圆x^2/16+y^2/9=1%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%过点(2,1)且斜率为1/2%' LIMIT 1), -1)
  )
) WHERE id = @l3;

-- 10. 指数函数
SET @l3 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='指数函数' AND level=3 LIMIT 1);
UPDATE knowledge_nodes SET learning_resources = JSON_OBJECT(
  'videoUrl', 'https://search.bilibili.com/all?keyword=职高数学+指数函数+图像与性质',
  'exampleIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%3x²的导数%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%已知等差数列%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%从甲、乙、丙3人中选2人%' LIMIT 1), -1)
  ),
  'practiceIds', JSON_ARRAY(
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%C₅²%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%展开式%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%同时掷两枚骰子%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%数据2,3,4,5,6,a%' LIMIT 1), -1),
    COALESCE((SELECT id FROM question_bank WHERE subject='数学[职高]' AND question_text LIKE '%已知|a|=3%' LIMIT 1), -1)
  )
) WHERE id = @l3;

SELECT 'v150_fixed: DONE' AS result;
