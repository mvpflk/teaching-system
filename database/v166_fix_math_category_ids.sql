-- ============================================================
-- v166: 修复数学题目 category_id — 从旧ID/L1根重映射到正确L4节点
-- 问题: 186道数学题category_id指向旧节点(已删)或L1根(id=10)
-- 知识树: 132个节点完整(id=3063~3193)
-- 日期: 2026-06-22
-- ⚠️ 执行前必须 SET NAMES utf8mb4
-- ============================================================
SET NAMES utf8mb4;

-- ══════════════════════════════════════════
-- Part 1: 单组批量映射 — 旧category_id整组→新L4节点
-- ══════════════════════════════════════════

-- 2774 (11题·集合运算) → 3082 交集运算[掌握]
UPDATE question_bank SET category_id = 3082 WHERE category_id = 2774 AND subject LIKE '%数学%';

-- 1316 (12题·不等式) → 3089 一元二次不等式解法[掌握]
UPDATE question_bank SET category_id = 3089 WHERE category_id = 1316 AND subject LIKE '%数学%';

-- 2810 (2题·单调性) → 3099 函数单调性判断[掌握]
UPDATE question_bank SET category_id = 3099 WHERE category_id = 2810 AND subject LIKE '%数学%';

-- 2826 (2题·向量夹角) → 3144 数量积计算[掌握]
UPDATE question_bank SET category_id = 3144 WHERE category_id = 2826 AND subject LIKE '%数学%';

-- 2867 (2题·切线斜率) → 3156 斜率[掌握]
UPDATE question_bank SET category_id = 3156 WHERE category_id = 2867 AND subject LIKE '%数学%';

-- 1323 (1题·圆方程) → 3158 圆标准方程[掌握]
UPDATE question_bank SET category_id = 3158 WHERE category_id = 1323 AND subject LIKE '%数学%';

-- 2778 (1题·立体几何) → 3151 线面平行垂直判定[理解]
UPDATE question_bank SET category_id = 3151 WHERE category_id = 2778 AND subject LIKE '%数学%';

-- 2860 (1题) → 3099 函数单调性
UPDATE question_bank SET category_id = 3099 WHERE category_id = 2860 AND subject LIKE '%数学%';


-- ══════════════════════════════════════════
-- Part 2: 需拆分的组 — 逐题精确定位
-- ══════════════════════════════════════════

-- ── 1318 (30题·函数+导数+三角混杂) ──

-- 定义域题 → 3096 函数定义与定义域[掌握]
UPDATE question_bank SET category_id = 3096 WHERE id IN (2344, 2648, 2774, 2778);
-- 奇偶性 → 3100 函数奇偶性判断[掌握]
UPDATE question_bank SET category_id = 3100 WHERE id IN (2345, 2651, 2780, 2783);
-- 二次函数顶点/最值 → 3101/3102
UPDATE question_bank SET category_id = 3101 WHERE id = 2346;           -- 顶点坐标
UPDATE question_bank SET category_id = 3102 WHERE id IN (2652, 2782, 2837, 2845);  -- 最值
-- 单调性 → 3099
UPDATE question_bank SET category_id = 3099 WHERE id IN (2366, 2649, 2653, 2696);
-- 导数 → 3096 (职高数学无独立导数字段，归入函数定义域)
UPDATE question_bank SET category_id = 3096 WHERE id IN (2365, 2693, 2694, 2695, 2834, 2835);
-- 三角函数混杂 → 三角L4
UPDATE question_bank SET category_id = 3118 WHERE id = 2352;           -- y=2sinx max → 三角函数定义[掌握]
UPDATE question_bank SET category_id = 3118 WHERE id IN (2664, 2797);  -- sin值域/3sinx → 定义
UPDATE question_bank SET category_id = 3122 WHERE id IN (2779, 2794);  -- 周期/sin图像 → 正弦余弦图像[理解]
-- 奇函数计算 → 3100
UPDATE question_bank SET category_id = 3100 WHERE id = 2781;           -- f(-2)计算

-- ── 2777 (19题·数列) ──

-- 等差数列 → 3133通项[掌握] / 3134前n项和[掌握]
UPDATE question_bank SET category_id = 3133 WHERE id IN (2347, 2654, 2657, 2798, 2804, 2840);  -- 通项公式
UPDATE question_bank SET category_id = 3134 WHERE id IN (2349, 2656, 2802, 2841);               -- 前n项和
UPDATE question_bank SET category_id = 3133 WHERE id IN (2658, 2800);                            -- 等差性质→通项
-- 等比数列 → 3135通项[掌握] / 3136前n项和[掌握]
UPDATE question_bank SET category_id = 3135 WHERE id IN (2348, 2655, 2801, 2803, 2805);  -- 通项公式
UPDATE question_bank SET category_id = 3136 WHERE id IN (2659, 2799);                     -- 前n项和

-- ── 2864 (13题·直线/距离/方程混杂) ──

-- 直线方程/斜率 → 3156/3157
UPDATE question_bank SET category_id = 3156 WHERE id IN (2360, 2361, 2685, 2821, 2822);  -- 斜率/距离
UPDATE question_bank SET category_id = 3157 WHERE id = 2686;                              -- 直线方程
-- 点线距离 → 3160 直线与圆位置[掌握]
UPDATE question_bank SET category_id = 3160 WHERE id IN (2368, 2680, 2683, 2704);
-- 切线/方程 → 3184 一元二次方程公式法求解 (初中基础补漏)
UPDATE question_bank SET category_id = 3184 WHERE id = 2836;
-- 导数 → 3096
UPDATE question_bank SET category_id = 3096 WHERE id = 2817;

-- ── 1321 (8题·平面向量) ──

UPDATE question_bank SET category_id = 3142 WHERE id = 2353;           -- 向量定义[了解]
UPDATE question_bank SET category_id = 3144 WHERE id IN (2355, 2671, 2672, 2810);  -- 数量积计算[掌握]
UPDATE question_bank SET category_id = 3143 WHERE id = 2668;           -- 加减法[理解]
UPDATE question_bank SET category_id = 3145 WHERE id = 2669;           -- 坐标表示[掌握]
UPDATE question_bank SET category_id = 3146 WHERE id = 2807;           -- 坐标运算应用[掌握]

-- ── 2829 (8题·圆/椭圆/圆柱) ──

UPDATE question_bank SET category_id = 3158 WHERE id IN (2357, 2359);           -- 圆标准方程[掌握]
UPDATE question_bank SET category_id = 3159 WHERE id = 2812;                    -- 圆一般方程[掌握]
UPDATE question_bank SET category_id = 3191 WHERE id = 2819;                    -- 椭圆[掌握]
UPDATE question_bank SET category_id = 3150 WHERE id IN (2676, 2681);           -- 体积[理解]
UPDATE question_bank SET category_id = 3149 WHERE id = 2818;                    -- 表面积[理解]

-- ── 2861 (7题·立体几何) ──

UPDATE question_bank SET category_id = 3150 WHERE id IN (2356, 2358, 2675, 2679);  -- 体积[理解]
UPDATE question_bank SET category_id = 3149 WHERE id = 2815;                        -- 表面积[理解]
UPDATE question_bank SET category_id = 3152 WHERE id IN (2674, 2816);               -- 面面关系[理解]

-- ── 2776 (3题) ──

UPDATE question_bank SET category_id = 3127 WHERE id = 2369;           -- 余弦定理[掌握]
UPDATE question_bank SET category_id = 3150 WHERE id = 2813;           -- 体积
UPDATE question_bank SET category_id = 3126 WHERE id = 2843;           -- 正弦定理[掌握]

-- ── 1324 (9题·概率/统计) ──

UPDATE question_bank SET category_id = 3167 WHERE id IN (2362, 2363, 2690, 2825, 2827);  -- 古典概型[掌握]
UPDATE question_bank SET category_id = 3166 WHERE id = 2823;           -- 排列组合[理解]
UPDATE question_bank SET category_id = 3168 WHERE id = 2687;           -- 随机事件[理解]
UPDATE question_bank SET category_id = 3170 WHERE id IN (2839, 2846);  -- 样本估计总体[了解]


-- ══════════════════════════════════════════
-- Part 3: 56道category_id=10 → 按内容关键字匹配L4
-- ══════════════════════════════════════════

-- 三角函数 (sin/cos/tan/正弦/余弦)
UPDATE question_bank SET category_id = 3118 WHERE id IN (2350, 2351, 2661, 2662, 2663, 2666, 2790, 2796);  -- 定义
UPDATE question_bank SET category_id = 3120 WHERE id IN (2667, 2791, 2792);                                   -- 诱导公式
UPDATE question_bank SET category_id = 3127 WHERE id = 2793;                                                  -- 余弦定理
UPDATE question_bank SET category_id = 3126 WHERE id = 2795;                                                  -- 正弦定理(勾股)

-- 充要条件/集合关系
UPDATE question_bank SET category_id = 3080 WHERE id IN (2340, 2770, 2772);  -- 子集/条件→子集[理解]

-- 集合运算(补集)
UPDATE question_bank SET category_id = 3084 WHERE id = 2634;  -- 补集运算[掌握]

-- 统计/数据
UPDATE question_bank SET category_id = 3170 WHERE id IN (2364, 2688, 2689, 2692, 2824, 2826);  -- 样本估计总体

-- 不等式
UPDATE question_bank SET category_id = 3088 WHERE id IN (2647, 2777);  -- 不等式性质[了解]

-- 函数
UPDATE question_bank SET category_id = 3096 WHERE id = 2650;  -- f(x)求值→定义域

-- 向量
UPDATE question_bank SET category_id = 3142 WHERE id = 2670;                 -- 向量定义
UPDATE question_bank SET category_id = 3144 WHERE id IN (2806, 2808);        -- 数量积
UPDATE question_bank SET category_id = 3146 WHERE id = 2809;                 -- 坐标运算

-- 立体几何
UPDATE question_bank SET category_id = 3149 WHERE id = 2677;                 -- 长方体→表面积
UPDATE question_bank SET category_id = 3150 WHERE id IN (2678, 2814);        -- 体对角线→体积

-- 解析几何(距离)
UPDATE question_bank SET category_id = 3156 WHERE id = 2682;                 -- 斜率

-- 概率/排列组合
UPDATE question_bank SET category_id = 3168 WHERE id = 2691;                 -- 随机事件
UPDATE question_bank SET category_id = 3166 WHERE id IN (2828, 2829, 2832, 2833);  -- 排列组合
UPDATE question_bank SET category_id = 3165 WHERE id IN (2830, 2831);        -- 二项式→计数原理

-- 导数(职高无独立导数节点→归入单调性)
UPDATE question_bank SET category_id = 3099 WHERE id IN (2698, 2700);

-- 初中基础: 有理数/整式运算
UPDATE question_bank SET category_id = 3178 WHERE id IN (2701, 2702, 2705, 2784, 2785, 2786, 2787, 2788, 2789);
UPDATE question_bank SET category_id = 3180 WHERE id = 2703;                 -- 整式
UPDATE question_bank SET category_id = 3184 WHERE id = 2706;                 -- 无理数→公式法

-- 计算/等式判断
UPDATE question_bank SET category_id = 3178 WHERE id IN (2370, 2665);

-- ── 补漏: Part2中遗漏的3题 ──
UPDATE question_bank SET category_id = 3191 WHERE id = 2684;  -- 椭圆→椭圆[掌握]
UPDATE question_bank SET category_id = 3099 WHERE id = 2699;  -- 极小值→单调性[掌握]
UPDATE question_bank SET category_id = 3160 WHERE id = 2820;  -- 点线距离→位置关系[掌握]


-- ══════════════════════════════════════════
-- Part 4: 验证
-- ══════════════════════════════════════════
SELECT '=== 验证结果 ===' AS '';
SELECT
  CASE
    WHEN kn.id IS NULL THEN '❌ 指向不存在节点'
    WHEN kn.level IS NULL THEN '⚠ 节点无level'
    WHEN kn.level = 1 THEN '⚠ L1根节点(学科级)'
    ELSE CONCAT('✅ L', CAST(kn.level AS CHAR), ' ', kn.name)
  END AS 状态,
  COUNT(*) AS 题目数
FROM question_bank qb
LEFT JOIN knowledge_nodes kn ON qb.category_id = kn.id
WHERE qb.subject LIKE '%数学%' AND qb.status = 1
GROUP BY 状态
ORDER BY 状态;
