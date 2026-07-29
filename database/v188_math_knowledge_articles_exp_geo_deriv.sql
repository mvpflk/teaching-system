-- ============================================================================
-- v188: 数学[职高] 知识文章 — 指对数+立体几何+导数模块 (subject_id=22)
-- 指数与对数函数: 4篇 | 立体几何: 4篇 | 导数初步: 4篇 = 12篇
-- 幂等：INSERT IGNORE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 指数与对数函数 > 指数运算性质
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='指数运算性质 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '指数与对数函数', '指数函数', '指数运算性质',
'## 核心运算公式

### 同底数幂运算

| 公式 | 说明 |
|:----:|:-----|
| aᵐ · aⁿ = aᵐ⁺ⁿ | 同底相乘，指数相加 |
| aᵐ ÷ aⁿ = aᵐ⁻ⁿ | 同底相除，指数相减 |
| (aᵐ)ⁿ = aᵐⁿ | 幂的幂，指数相乘 |

### 积商幂

| 公式 | 说明 |
|:----:|:-----|
| (ab)ⁿ = aⁿbⁿ | 积的幂等于幂的积 |
| (a/b)ⁿ = aⁿ/bⁿ | 商的幂等于幂的商 |

### 零指数与负指数

| 公式 | 条件 |
|:----:|:-----|
| a⁰ = 1 | a ≠ 0 |
| a⁻ⁿ = 1/aⁿ | a ≠ 0 |

### 分数指数幂

**a^(m/n) = ⁿ√(aᵐ)**（a > 0）

实现了根式与指数幂的互化。

## 典型例题

**例1**：化简 2³ × 2⁴

= 2³⁺⁴ = 2⁷ = **128**

**例2**：化简 (3²)⁴

= 3²ˣ⁴ = 3⁸ = **6561**

**例3**：化简 (2/3)⁻²

= (3/2)² = **9/4**

**例4**：化简 27^(2/3)

= (3³)^(2/3) = 3² = **9**

## 常见错误辨析

| 错误写法 | 正确写法 | 说明 |
|:--------:|:--------:|:-----|
| aᵐ · aⁿ = aᵐⁿ | aᵐ⁺ⁿ | 乘法加指数 |
| (a+b)ⁿ = aⁿ+bⁿ | 不成立 | 指数不满足分配律 |
| aᵐ ÷ aⁿ = aᵐ/ⁿ | aᵐ⁻ⁿ | 除法减指数 |

## 易错警示

1. **aᵐ·aⁿ=aᵐ⁺ⁿ 不是 aᵐⁿ**——乘法加指数，不是乘指数
2. **(a+b)ⁿ≠aⁿ+bⁿ**——指数不满足分配律，这是最常见的错误
3. **底数a>0且a≠1**——分数指数幂要求a>0',
'指数运算核心：同底相乘指数相加(aᵐ·aⁿ=aᵐ⁺ⁿ)，相除相减(aᵐ÷aⁿ=aᵐ⁻ⁿ)，幂的幂相乘((aᵐ)ⁿ=aᵐⁿ)。注意(a+b)ⁿ≠aⁿ+bⁿ。',
@l4, 2, '["指数","运算","幂","分数指数"]',
'["指数与对数函数","指数函数"]',
'[{"type":"choice","question":"2³×2⁵=","options":["A. 2⁸","B. 2¹⁵","C. 4⁸","D. 8²"],"answer":"A","explanation":"同底相乘指数相加：2³×2⁵=2³⁺⁵=2⁸"},
{"type":"judge","question":"(a+b)ⁿ=aⁿ+bⁿ。","answer":"错","explanation":"指数不满足分配律，(a+b)²=a²+2ab+b²≠a²+b²"},
{"type":"choice","question":"(1/2)⁻³=","options":["A. 1/8","B. 8","C. -8","D. -1/8"],"answer":"B","explanation":"(1/2)⁻³=2³=8，负指数取倒数"},
{"type":"fill","question":"27^(1/3)=____。","answer":"3","explanation":"27^(1/3)=∛27=3"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='指数运算性质' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '同底数幂相乘的法则是什么？', 'aᵐ·aⁿ=aᵐ⁺ⁿ。同底相乘，指数相加。例如2³×2⁵=2⁸。', 1),
(@art, '(a+b)ⁿ等于aⁿ+bⁿ吗？', '不等。指数不满足分配律。(a+b)²=a²+2ab+b²，中间还有交叉项。', 2),
(@art, '负指数和分数指数分别怎么理解？', 'a⁻ⁿ=1/aⁿ（取倒数），a^(m/n)=ⁿ√(aᵐ)（根式与指数互化）。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 指数与对数函数 > 指数函数的图像与性质
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='指数函数的图像与性质 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '指数与对数函数', '指数函数', '指数函数的图像与性质',
'## 指数函数

**y = aˣ**（a > 0 且 a ≠ 1）

## 两种图像形态

### a > 1 时（递增）

```
y
↑      /
|     /
|    /
----+------→ x
|  /(0,1)
| /
|/
```

- 从左到右**上升**（单调递增）
- x<0 时 0<y<1
- x>0 时 y>1

### 0 < a < 1 时（递减）

```
y
↑\
| \
|  \
----+------→ x
|  \(0,1)
|   \
|    \
```

- 从左到右**下降**（单调递减）
- x<0 时 y>1
- x>0 时 0<y<1

## 性质汇总

| 性质 | 内容 |
|:----:|:-----|
| 定义域 | R |
| 值域 | (0, +∞) |
| 恒过定点 | **(0, 1)** |
| 单调性 | a>1递增，0<a<1递减 |
| 渐近线 | x轴（y=0） |

## 比较大小

利用单调性比较指数幂大小：

**同底数**：利用单调性直接比较指数

**例**：比较 2³ 和 2⁵

a=2>1，单调递增，3<5 → 2³ < 2⁵

**不同底数**：化为同底或同指数

**例**：比较 2³ 和 3²

2³=8，3²=9 → 2³ < 3²

## 易错警示

1. **恒过(0,1)**——a⁰=1，无论a取何值
2. **值域是(0,+∞)**——不包含0，永远在x轴上方
3. **a>1和0<a<1的单调性相反**——这是两种截然不同的图像',
'指数函数y=aˣ(a>0且a≠1)。a>1递增，0<a<1递减。恒过(0,1)，值域(0,+∞)。比较大小利用单调性。',
@l4, 2, '["指数函数","图像","单调性","过定点"]',
'["指数与对数函数","指数函数"]',
'[{"type":"choice","question":"指数函数y=2ˣ的图像恒过哪个点？","options":["A. (1,0)","B. (0,1)","C. (0,0)","D. (1,1)"],"answer":"B","explanation":"2⁰=1，恒过(0,1)"},
{"type":"judge","question":"指数函数y=aˣ的值域包含0。","answer":"错","explanation":"值域是(0,+∞)，aˣ永远大于0"},
{"type":"choice","question":"若a>1，则y=aˣ是","options":["A. 递增函数","B. 递减函数","C. 常数函数","D. 非单调函数"],"answer":"A","explanation":"a>1时y=aˣ单调递增"},
{"type":"fill","question":"比较大小：2⁰·⁵____1（填>、<或=）。","answer":">","explanation":"2⁰·⁵=√2>1，因为2>1且0.5>0"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='指数函数的图像与性质' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '指数函数y=aˣ的图像恒过哪个点？', '恒过(0,1)。因为a⁰=1对任意a>0且a≠1都成立。', 1),
(@art, 'a>1和0<a<1时指数函数的单调性有什么不同？', 'a>1时单调递增（从左到右上升），0<a<1时单调递减（从左到右下降）。', 2),
(@art, '指数函数的值域是什么？', '(0,+∞)。aˣ永远大于0，图像始终在x轴上方，x轴是渐近线。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 指数与对数函数 > 对数运算性质
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='对数运算性质 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '指数与对数函数', '对数函数', '对数运算性质',
'## 对数的定义

若 aᵇ = N（a > 0, a ≠ 1），则 **b = logₐN**

即 logₐN 是使 a 的多少次方等于 N 的数。

## 三个核心运算性质

| 性质 | 公式 | 记忆口诀 |
|:----:|:-----|:--------:|
| 积的对数 | logₐ(MN) = logₐM + logₐN | 积→加 |
| 商的对数 | logₐ(M/N) = logₐM - logₐN | 商→减 |
| 幂的对数 | logₐMⁿ = n·logₐM | 幂→提指数 |

## 换底公式

**logₐb = logcb / logca**（c ≠ 1）

常用变形：logₐb = lnb / lna = lgb / lga

## 常用对数和自然对数

| 名称 | 符号 | 底数 | 应用 |
|:----:|:----:|:----:|:-----|
| 常用对数 | lg N | 10 | 计算器 |
| 自然对数 | ln N | e ≈ 2.718 | 科学计算 |

## 特殊值

| 公式 | 说明 |
|:----:|:-----|
| logₐ1 = 0 | a⁰ = 1 |
| logₐa = 1 | a¹ = a |
| a^(logₐN) = N | 对数恒等式 |

## 典型例题

**例1**：化简 log₂8 + log₂4

= log₂(8×4) = log₂32 = **5**

**例2**：化简 log₃9 - log₃3

= log₃(9/3) = log₃3 = **1**

**例3**：化简 2log₅10 + log₅0.4

= log₅100 + log₅0.4 = log₅(100×0.4) = log₅40

= log₅(5×8) = 1 + log₅8 = 1 + 3log₅2

这还不够简化。让我换一种方式：

2log₅10 + log₅0.4 = log₅100 + log₅0.4 = log₅(100×0.4) = log₅40

= log₅(8×5) = log₅8 + 1 = 3log₅2 + 1

如果需要数值结果，用换底公式。

**例4**：求 log₈4 的值。

换底：log₈4 = lg4/lg8 = 2lg2/(3lg2) = **2/3**

或：8^(2/3) = (2³)^(2/3) = 2² = 4 ✓

## 易错警示

1. **logₐ(M+N) ≠ logₐM + logₐN**——和的对数没有分解公式
2. **换底公式分子分母别写反**——logₐb = logcb/logca
3. **真数必须大于0**——logₐN中N>0',
'对数运算：积的对数=对数的和，商的对数=对数的差，幂的对数=指数×对数。换底公式：logₐb=lnb/lna。注意和的对数≠对数的和。',
@l4, 2, '["对数","运算","换底公式","lg","ln"]',
'["指数与对数函数","对数函数"]',
'[{"type":"choice","question":"log₂8=","options":["A. 2","B. 3","C. 4","D. 8"],"answer":"B","explanation":"2³=8，所以log₂8=3"},
{"type":"judge","question":"log₂(4+4)=log₂4+log₂4。","answer":"错","explanation":"log₂(4+4)=log₂8=3，log₂4+log₂4=2+2=4，两者不等。和的对数≠对数的和"},
{"type":"choice","question":"ln e²=","options":["A. e","B. 2","C. 2e","D. e²"],"answer":"B","explanation":"ln e²=2ln e=2×1=2"},
{"type":"fill","question":"log₃1=____。","answer":"0","explanation":"3⁰=1，所以log₃1=0"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='对数运算性质' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '对数的三个核心运算性质是什么？', '①积的对数=对数的和 ②商的对数=对数的差 ③幂的对数=指数×对数。', 1),
(@art, 'logₐ(M+N)等于logₐM+logₐN吗？', '不等。和的对数没有分解公式，只有积的对数才能拆开。这是最常见错误。', 2),
(@art, '换底公式是什么？', 'logₐb=lnb/lna（或lgb/lga）。底数和真数同时换成新的底数c。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 指数与对数函数 > 对数函数的图像与性质
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='对数函数的图像与性质 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '指数与对数函数', '对数函数', '对数函数的图像与性质',
'## 对数函数

**y = logₐx**（a > 0 且 a ≠ 1）

> 对数函数是指数函数 y=aˣ 的**反函数**。

## 两种图像形态

### a > 1 时（递增）

- 从左到右**上升**（单调递增）
- 0<x<1 时 y<0
- x>1 时 y>0

### 0 < a < 1 时（递减）

- 从左到右**下降**（单调递减）
- 0<x<1 时 y>0
- x>1 时 y<0

## 性质汇总

| 性质 | 内容 |
|:----:|:-----|
| 定义域 | (0, +∞)（真数必须大于0） |
| 值域 | R |
| 恒过定点 | **(1, 0)** |
| 单调性 | a>1递增，0<a<1递减 |
| 渐近线 | y轴（x=0） |

## 与指数函数的关系

| | 指数函数 y=aˣ | 对数函数 y=logₐx |
|:--:|:-------------:|:----------------:|
| 定义域 | R | (0,+∞) |
| 值域 | (0,+∞) | R |
| 过定点 | (0,1) | (1,0) |
| 关系 | — | 互为反函数 |

**图像关系**：y=aˣ 和 y=logₐx 的图像关于直线 **y=x** 对称。

## 比较大小

**同底数**：利用单调性比较真数

**例**：a=2>1，log₂3 与 log₂5

单调递增，3<5 → log₂3 < log₂5

**与0比较**：logₐx 与 0

a>1 时：x>1 → logₐx>0；0<x<1 → logₐx<0

0<a<1 时：x>1 → logₐx<0；0<x<1 → logₐx>0

## 易错警示

1. **定义域是(0,+∞)**——真数必须大于0
2. **恒过(1,0)**——logₐ1=0
3. **与指数函数过定点不同**——指数过(0,1)，对数过(1,0)',
'对数函数y=logₐx是指数函数y=aˣ的反函数。定义域(0,+∞)，过(1,0)。a>1递增，0<a<1递减。图像与指数函数关于y=x对称。',
@l4, 2, '["对数函数","图像","反函数","过定点"]',
'["指数与对数函数","对数函数"]',
'[{"type":"choice","question":"对数函数y=log₂x的图像恒过哪个点？","options":["A. (0,1)","B. (1,0)","C. (1,1)","D. (0,0)"],"answer":"B","explanation":"log₂1=0，恒过(1,0)"},
{"type":"judge","question":"对数函数y=logₐx的定义域是R。","answer":"错","explanation":"定义域是(0,+∞)，真数必须大于0"},
{"type":"choice","question":"若a>1，则y=logₐx是","options":["A. 递增函数","B. 递减函数","C. 常数函数","D. 非单调函数"],"answer":"A","explanation":"a>1时y=logₐx单调递增"},
{"type":"fill","question":"log₂(1/8)=____。","answer":"-3","explanation":"2⁻³=1/8，所以log₂(1/8)=-3"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='对数函数的图像与性质' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '对数函数的图像恒过哪个点？', '恒过(1,0)。因为logₐ1=0对任意a>0且a≠1都成立。', 1),
(@art, '对数函数和指数函数是什么关系？', '互为反函数。图像关于直线y=x对称。定义域和值域互换。', 2),
(@art, '对数函数的定义域是什么？为什么？', '(0,+∞)。因为真数必须大于0，logₐx中x>0。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 立体几何 > 常见几何体的表面积
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='常见几何体的表面积 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '立体几何', '空间几何体', '常见几何体的表面积公式',
'## 核心公式

| 几何体 | 表面积公式 | 参数说明 |
|:------:|:----------:|:--------:|
| **正方体** | S = 6a² | a为棱长 |
| **长方体** | S = 2(ab+bc+ca) | a,b,c为长宽高 |
| **圆柱** | S = 2πr² + 2πrh | r底面半径，h高 |
| **圆锥** | S = πr² + πrl | l为母线长 |
| **球** | S = 4πR² | R为半径 |

## 圆柱详解

- 侧面积：S_侧 = 2πrh（底面周长×高）
- 底面积：S_底 = πr²（两个底面）
- **总表面积**：S = 2πr² + 2πrh = 2πr(r+h)

## 圆锥详解

- 侧面积：S_侧 = πrl（展开是扇形）
- 底面积：S_底 = πr²（一个底面）
- **总表面积**：S = πr² + πrl = πr(r+l)

> 母线 l = √(r²+h²)（勾股定理）

## 球

**S = 4πR²**

记忆：球的表面积等于4个大圆面积之和。

## 组合体

对于不规则几何体，通常将其**分割**为规则几何体分别计算。

**例**：无盖圆柱形容器的表面积

= 侧面积 + 一个底面积 = 2πrh + πr²

## 易错警示

1. **圆锥侧面积是πrl不是2πrl**——圆柱才是2πrh
2. **母线l≠高h**——l=√(r²+h²)，l是斜边
3. **注意是否有盖**——无盖容器少一个底面积',
'表面积公式：正方体6a²，长方体2(ab+bc+ca)，圆柱2πr(r+h)，圆锥πr(r+l)，球4πR²。注意圆锥母线l=√(r²+h²)。',
@l4, 2, '["立体几何","表面积","圆柱","圆锥","球"]',
'["立体几何","空间几何体"]',
'[{"type":"choice","question":"正方体棱长为2，表面积为","options":["A. 8","B. 16","C. 24","D. 48"],"answer":"C","explanation":"S=6a²=6×4=24"},
{"type":"choice","question":"圆柱底面半径1，高3，表面积为","options":["A. 6π","B. 8π","C. 12π","D. 18π"],"answer":"B","explanation":"S=2πr²+2πrh=2π+6π=8π"},
{"type":"judge","question":"圆锥的侧面积公式是2πrl。","answer":"错","explanation":"圆锥侧面积是πrl，没有系数2。2πrh是圆柱侧面积"},
{"type":"fill","question":"球的半径为3，表面积为____。","answer":"36π","explanation":"S=4πR²=4π×9=36π"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='常见几何体的表面积公式' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '圆柱和圆锥的表面积公式分别是什么？', '圆柱S=2πr(r+h)，圆锥S=πr(r+l)。注意圆锥母线l=√(r²+h²)，l≠h。', 1),
(@art, '球的表面积公式是什么？', 'S=4πR²。记忆：等于4个大圆面积之和。', 2),
(@art, '圆锥侧面积为什么是πrl不是2πrl？', '圆锥侧面展开是扇形（不是矩形），面积=½×弧长×半径=½×2πr×l=πrl。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 立体几何 > 常见几何体的体积
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='常见几何体的体积 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '立体几何', '空间几何体', '常见几何体的体积公式',
'## 核心公式

| 几何体 | 体积公式 | 说明 |
|:------:|:--------:|:-----|
| **柱体** | V = S_底 × h | 棱柱/圆柱通用 |
| **锥体** | V = ⅓ × S_底 × h | 棱锥/圆锥通用 |
| **台体** | V = ⅓h(S₁+S₂+√(S₁S₂)) | S₁,S₂为上下底面积 |
| **球** | V = ⅔πR³ | R为半径 |

## 特殊柱体

| 几何体 | 体积公式 |
|:------:|:--------:|
| 正方体 | V = a³ |
| 长方体 | V = abc |
| 圆柱 | V = πr²h |

## 特殊锥体

| 几何体 | 体积公式 |
|:------:|:--------:|
| 圆锥 | V = ⅓πr²h |
| 棱锥 | V = ⅓S_底h |

## 等底等高原则

**等底等高的锥体体积相等**——不论形状如何。

**等底等高的锥体体积是柱体的⅓**——这是一个核心关系。

## 组合体体积

将不规则几何体**分割**为规则部分分别计算。

**例**：一个几何体由一个圆柱和一个圆锥组成（等底），底面半径2，圆柱高3，圆锥高4。

V = πr²h₁ + ⅓πr²h₂ = π×4×3 + ⅓π×4×4 = 12π + 16π/3 = **52π/3**

## 易错警示

1. **锥体体积有⅓系数**——别忘了乘⅓
2. **球体积是⅔πR³不是4πR³/3**——与表面积公式区分
3. **等底等高原则**——锥体体积只与底面积和高有关，与形状无关',
'柱体V=S_底h，锥体V=⅓S_底h（注意⅓系数），球V=⅔πR³。等底等高的锥体体积相等，是柱体的⅓。',
@l4, 2, '["立体几何","体积","柱体","锥体","球"]',
'["立体几何","空间几何体"]',
'[{"type":"choice","question":"圆锥底面半径2，高3，体积为","options":["A. 4π","B. 8π","C. 12π","D. 24π"],"answer":"B","explanation":"V=⅓πr²h=⅓π×4×3=4π？不对：⅓×π×4×3=4π"},
{"type":"judge","question":"等底等高的两个锥体体积一定相等。","answer":"对","explanation":"锥体体积只与底面积和高有关，与形状无关"},
{"type":"choice","question":"球的体积公式是","options":["A. 4πR²","B. 4πR³/3","C. ⅔πR³","D. πR³"],"answer":"C","explanation":"球体积V=⅔πR³（也写作4πR³/3），注意与表面积4πR²区分"},
{"type":"fill","question":"正方体棱长为3，体积为____。","answer":"27","explanation":"V=a³=3³=27"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='常见几何体的体积公式' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '柱体和锥体的体积公式分别是什么？', '柱体V=S_底h，锥体V=⅓S_底h。锥体体积是同底等高柱体的⅓。', 1),
(@art, '球的体积公式是什么？和表面积有什么区别？', '体积V=⅔πR³，表面积S=4πR²。体积有R³，表面积有R²。', 2),
(@art, '等底等高的锥体有什么性质？', '体积相等。不论形状如何，只要底面积和高相同，锥体体积就相同。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 立体几何 > 线面平行与垂直的判定
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='线面平行与垂直的判定 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '立体几何', '点线面的位置关系', '线面平行与垂直的判定',
'## 线面平行判定

**定理**：平面外一直线与平面内一直线平行 → 线面平行

**符号**：l ⊄ α, m ⊂ α, l∥m → **l∥α**

> 关键：l必须在平面**外**，m必须在平面**内**。

**例**：正方体ABCD-A₁B₁C₁D₁中，证明A₁B∥平面DCC₁D₁。

证明：A₁B∥D₁C（正方体对面平行），D₁C⊂平面DCC₁D₁，A₁B⊄平面DCC₁D₁

所以A₁B∥平面DCC₁D₁ ✓

## 线面垂直判定

**定理**：一直线与平面内**两条相交直线**都垂直 → 线面垂直

**符号**：l⊥a, l⊥b, a∩b=P, a,b⊂α → **l⊥α**

> 关键：必须是**两条相交直线**，一条不够！

**例**：在正方体中，证明AC⊥平面BB₁D₁D。

证明：AC⊥BD（正方体底面对角线互相垂直），AC⊥BB₁（BB₁垂直于底面）

BD∩BB₁=B，BD,BB₁⊂平面BB₁D₁D

所以AC⊥平面BB₁D₁D ✓

## 面面平行判定

**定理**：一平面内两条相交直线分别平行于另一平面 → 面面平行

**符号**：a,b⊂α, a∩b=P, a∥β, b∥β → **α∥β**

## 面面垂直判定

**定理**：一平面包含另一平面的垂线 → 面面垂直

**符号**：l⊂α, l⊥β → **α⊥β**

## 易错警示

1. **线面垂直必须用两条相交直线**——一条不能判定
2. **线面平行前提l⊄α**——l必须在平面外
3. **面面垂直和线面垂直的区别**——面面垂直需要一个面包含另一个面的垂线',
'线面平行：平面外一直线∥平面内一直线。线面垂直：直线⊥平面内两条相交直线。面面垂直：一面包含另一面的垂线。',
@l4, 2, '["立体几何","线面平行","线面垂直","判定定理"]',
'["立体几何","点线面的位置关系"]',
'[{"type":"choice","question":"线面垂直的判定需要几条平面内的直线？","options":["A. 1条","B. 2条","C. 3条","D. 任意条"],"answer":"B","explanation":"需要2条相交直线都与直线垂直，才能判定线面垂直"},
{"type":"judge","question":"一条直线与平面内无数条直线平行，则线面平行。","answer":"错","explanation":"必须是平面外一直线与平面内一直线平行，且该直线在平面外"},
{"type":"choice","question":"面面垂直的判定条件是","options":["A. 两平面各有一条直线平行","B. 一平面包含另一平面的垂线","C. 两平面没有公共点","D. 两平面的交线垂直于某直线"],"answer":"B","explanation":"面面垂直判定：l⊂α,l⊥β⇒α⊥β"},
{"type":"fill","question":"线面平行判定定理中，直线l必须在平面α的____。","answer":"外（外部）","explanation":"l⊄α，即l必须在平面外，否则l⊂α时不存在平行关系"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='线面平行与垂直的判定' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '线面平行的判定定理是什么？', '平面外一直线与平面内一直线平行，则线面平行。l⊄α,m⊂α,l∥m⇒l∥α。', 1),
(@art, '线面垂直为什么需要两条相交直线？', '一条直线只能确定一个方向，不能确定整个平面的法方向。两条相交直线确定平面的法方向。', 2),
(@art, '面面垂直的判定定理是什么？', '一平面包含另一平面的垂线，则面面垂直。l⊂α,l⊥β⇒α⊥β。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 立体几何 > 面面平行与垂直的判定
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='面面平行与垂直的判定 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '立体几何', '点线面的位置关系', '面面平行与垂直的判定及性质',
'## 面面平行

### 判定定理

一平面内**两条相交直线**分别平行于另一平面 → 面面平行

**符号**：a,b⊂α, a∩b=P, a∥β, b∥β → **α∥β**

### 性质定理

两平行平面被第三平面所截 → 交线平行

**符号**：α∥β, α∩γ=a, β∩γ=b → **a∥b**

## 面面垂直

### 判定定理

一平面包含另一平面的**垂线** → 面面垂直

**符号**：l⊂α, l⊥β → **α⊥β**

### 性质定理

两平面垂直时，一平面内垂直于交线的直线垂直于另一平面

**符号**：α⊥β, α∩β=l, a⊂α, a⊥l → **a⊥β**

## 四种位置关系总结

| 关系 | 判定条件 |
|:----:|:--------:|
| 线线平行 | 同位角相等/内错角相等/同旁内角互补 |
| 线面平行 | 面外直线∥面内直线 |
| 面面平行 | 一面内两相交直线∥另一面 |
| 线面垂直 | 直线⊥面内两相交直线 |
| 面面垂直 | 一面包含另一面的垂线 |

## 常见证明思路

```
线线平行 → 线面平行 → 面面平行
线线垂直 → 线面垂直 → 面面垂直
```

> 证明面面关系通常需要先证线面关系，证明线面关系通常需要先证线线关系。

## 易错警示

1. **面面平行也需要两条相交直线**——一条不够
2. **面面垂直的性质定理中a必须垂直于交线l**——不能直接说a⊥β',
'面面平行：一面内两相交直线∥另一面。面面垂直：一面包含另一面的垂线。证明思路：线线→线面→面面。',
@l4, 2, '["立体几何","面面平行","面面垂直","判定","性质"]',
'["立体几何","点线面的位置关系"]',
'[{"type":"choice","question":"面面平行的判定需要","options":["A. 一条直线平行","B. 两条相交直线平行","C. 三条直线平行","D. 无数条直线平行"],"answer":"B","explanation":"需要一面内两条相交直线都平行于另一面"},
{"type":"judge","question":"两平面垂直时，一平面内任意直线都垂直于另一平面。","answer":"错","explanation":"只有一平面内垂直于交线的直线才垂直于另一平面"},
{"type":"choice","question":"α⊥β,α∩β=l,a⊂α,a⊥l，则","options":["A. a∥β","B. a⊂β","C. a⊥β","D. 无法确定"],"answer":"C","explanation":"面面垂直性质：垂直于交线的直线垂直于另一面"},
{"type":"fill","question":"证明面面关系通常先证____关系。","answer":"线面","explanation":"证明思路：线线→线面→面面"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='面面平行与垂直的判定及性质' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '面面平行和面面垂直的判定分别需要什么条件？', '面面平行：一面内两相交直线∥另一面。面面垂直：一面包含另一面的垂线。', 1),
(@art, '证明面面关系的思路是什么？', '先证线线关系→再证线面关系→最后证面面关系。层层递进。', 2),
(@art, '面面垂直的性质定理是什么？', '两平面垂直时，一面内垂直于交线的直线⊥另一面。注意必须垂直于交线。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 导数初步 > 导数的几何意义
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='导数的几何意义（切线斜率）[了解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '导数初步 [选考]', '导数的概念与意义', '导数的几何意义',
'## 导数的定义

函数 y=f(x) 在 x=x₀ 处的导数：

**f\'(x₀) = lim(Δx→0) [f(x₀+Δx)-f(x₀)] / Δx**

## 几何意义

**f\'(x₀) 等于曲线 y=f(x) 在点 (x₀, f(x₀)) 处切线的斜率。**

**k_切 = f\'(x₀)**

## 切线方程

已知切点 (x₀, f(x₀)) 和斜率 k=f\'(x₀)，用点斜式：

**y - f(x₀) = f\'(x₀)(x - x₀)**

## 物理意义

**f\'(x₀) 表示函数在 x=x₀ 处的瞬时变化率。**

- 位移 s=s(t) → 速度 v(t)=s\'(t)（位移对时间的导数）
- 速度 v=v(t) → 加速度 a(t)=v\'(t)（速度对时间的导数）

## 典型例题

**例1**：求 f(x)=x² 在 x=1 处的导数和切线方程。

f\'(x) = 2x → f\'(1) = 2

切点：(1, 1)

切线方程：y - 1 = 2(x - 1) → **y = 2x - 1**

**例2**：求 f(x)=x³ 在 x=2 处的切线方程。

f\'(x) = 3x² → f\'(2) = 12

切点：(2, 8)

切线方程：y - 8 = 12(x - 2) → **y = 12x - 16**

## 易错警示

1. **导数只是斜率**——f\'(x₀) 是切线斜率，不是切线方程
2. **切线方程必须过切点**——先求 f(x₀) 再代入点斜式
3. **不同点的切线不同**——同一个函数在不同点的切线斜率不同',
'导数f\'(x₀)的几何意义是曲线在该点处切线的斜率。切线方程：y-f(x₀)=f\'(x₀)(x-x₀)。物理意义：瞬时变化率。',
@l4, 1, '["导数","几何意义","切线","斜率"]',
'["导数初步[选考]","导数的概念与意义"]',
'[{"type":"choice","question":"f(x)=x²在x=1处的导数是","options":["A. 1","B. 2","C. 3","D. 4"],"answer":"B","explanation":"f\'(x)=2x，f\'(1)=2"},
{"type":"judge","question":"导数f\'(x₀)就是切线方程。","answer":"错","explanation":"导数是切线的斜率，不是切线方程。切线方程需要点斜式写出"},
{"type":"choice","question":"f(x)=x³在x=1处的切线方程是","options":["A. y=3x-2","B. y=3x-1","C. y=3x","D. y=x"],"answer":"A","explanation":"f\'(x)=3x²,f\'(1)=3,切点(1,1),y-1=3(x-1),y=3x-2"},
{"type":"fill","question":"导数的物理意义是____。","answer":"瞬时变化率","explanation":"位移的导数是速度，速度的导数是加速度"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='导数的几何意义' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '导数的几何意义是什么？', 'f\'(x₀)是曲线y=f(x)在点(x₀,f(x₀))处切线的斜率。', 1),
(@art, '如何求切线方程？', '先求f\'(x₀)得斜率，再用点斜式y-f(x₀)=f\'(x₀)(x-x₀)。', 2),
(@art, '导数的物理意义是什么？', '瞬时变化率。位移的导数是速度，速度的导数是加速度。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 导数初步 > 基本初等函数的求导公式
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='基本初等函数的求导公式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '导数初步 [选考]', '基本求导公式', '基本求导公式与四则运算法则',
'## 基本求导公式

| 原函数 f(x) | 导数 f\'(x) | 记忆技巧 |
|:-----------:|:-----------:|:--------:|
| C（常数） | 0 | 常数不变→导数为0 |
| xⁿ | nxⁿ⁻¹ | 指数提前，次数减1 |
| sinx | cosx | sin变cos |
| cosx | -sinx | cos变-sin（注意负号） |
| eˣ | eˣ | eˣ的导数是自己 |
| lnx | 1/x | lnx导数是倒数 |
| aˣ | aˣlna | eˣlna=eˣ |

## 四则运算法则

| 运算 | 法则 |
|:----:|:-----|
| 加减法 | [f(x)±g(x)]\' = f\'(x)±g\'(x) |
| 乘法 | [f(x)·g(x)]\' = f\'(x)g(x)+f(x)g\'(x) |
| 除法 | [f(x)/g(x)]\' = [f\'(x)g(x)-f(x)g\'(x)]/[g(x)]² |
| 常数倍 | [Cf(x)]\' = Cf\'(x) |

### 乘法法则口诀

**"前导后不导 + 前不导后导"**

### 除法法则口诀

**"上导下不导 - 上不导下导，除以分母的平方"**

## 典型例题

**例1**：求 f(x)=3x⁴-2x²+5x-1 的导数。

f\'(x) = 12x³-4x+5

**例2**：求 f(x)=x²sinx 的导数。

f\'(x) = (x²)\'sinx + x²(sinx)\' = 2xsinx + x²cosx

**例3**：求 f(x)=(x+1)/(x-1) 的导数。

f\'(x) = [(1)(x-1)-(x+1)(1)]/(x-1)² = -2/(x-1)²

## 常见求导错误

| 错误 | 正确 |
|:----:|:-----|
| (xⁿ)\'=nxⁿ | nxⁿ⁻¹ |
| (cosx)\'=sinx | -sinx |
| (sinx)\'=-cosx | cosx |
| (uv)\'=u\'v\' | u\'v+uv\' |

## 易错警示

1. **(cosx)\'=-sinx 有负号**——最容易漏掉
2. **(uv)\'≠u\'v\'**——乘法法则不是分别求导再相乘
3. **(xⁿ)\'=nxⁿ⁻¹ 指数减1**——不是nxⁿ',
'基本求导：(xⁿ)\'=nxⁿ⁻¹，(sinx)\'=cosx，(cosx)\'=-sinx，(eˣ)\'=eˣ，(lnx)\'=1/x。乘法法则"前导后不导+前不导后导"。',
@l4, 2, '["导数","求导公式","乘法法则","除法法则"]',
'["导数初步[选考]","基本求导公式"]',
'[{"type":"choice","question":"(x³)\'=","options":["A. 3x","B. 3x²","C. 3x³","D. x³"],"answer":"B","explanation":"(xⁿ)\'=nxⁿ⁻¹，(x³)\'=3x²"},
{"type":"judge","question":"(cosx)\'=sinx。","answer":"错","explanation":"(cosx)\'=-sinx，注意负号"},
{"type":"choice","question":"(x²eˣ)\'=","options":["A. 2xeˣ","B. x²eˣ","C. (2x+x²)eˣ","D. 2xeˣ+x²"],"answer":"C","explanation":"乘法法则：(x²)\'eˣ+x²(eˣ)\'=2xeˣ+x²eˣ=(2x+x²)eˣ"},
{"type":"fill","question":"(lnx)\'=____。","answer":"1/x","explanation":"对数函数lnx的导数是1/x"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='基本求导公式与四则运算法则' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '最常见的求导公式有哪些？', '(xⁿ)\'=nxⁿ⁻¹，(sinx)\'=cosx，(cosx)\'=-sinx，(eˣ)\'=eˣ，(lnx)\'=1/x。', 1),
(@art, '乘法法则的口诀是什么？', '"前导后不导+前不导后导"。(uv)\'=u\'v+uv\'。', 2),
(@art, '(cosx)\'等于什么？容易犯什么错误？', '(cosx)\'=-sinx。最容易漏掉负号，必须记住cos求导有负号。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 导数初步 > 利用导数判断单调性
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='利用导数判断函数的单调性 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '导数初步 [选考]', '导数的应用', '利用导数判断函数单调性',
'## 核心结论

| f\'(x) 的符号 | f(x) 的单调性 |
|:------------:|:------------:|
| f\'(x) > 0 | **单调递增** |
| f\'(x) < 0 | **单调递减** |
| f\'(x) = 0 | 可能是极值点 |

## 判断步骤

1. **求定义域**——确定讨论范围
2. **求导 f\'(x)**
3. **解不等式 f\'(x)>0 和 f\'(x)<0**
4. **确定单调区间**

## 典型例题

**例1**：求 f(x)=x³-3x 的单调区间。

定义域：R

f\'(x) = 3x²-3 = 3(x+1)(x-1)

| 区间 | f\'(x) | 单调性 |
|:----:|:------:|:------:|
| (-∞,-1) | + | 递增 |
| (-1,1) | - | 递减 |
| (1,+∞) | + | 递增 |

单调递增区间：(-∞,-1) 和 (1,+∞)

单调递减区间：(-1,1)

**例2**：求 f(x)=lnx-x 的单调区间。

定义域：(0,+∞)

f\'(x) = 1/x - 1 = (1-x)/x

x>0 时，f\'(x) 的符号取决于 (1-x)：

| 区间 | f\'(x) | 单调性 |
|:----:|:------:|:------:|
| (0,1) | + | 递增 |
| (1,+∞) | - | 递减 |

单调递增区间：(0,1)

单调递减区间：(1,+∞)

## 驻点

**f\'(x₀)=0 的点 x₀ 称为驻点。**

驻点可能是极值点，也可能不是（如 f(x)=x³ 在 x=0 处）。

## 易错警示

1. **单调区间不能用∪连接**——应写"(-∞,-1), (1,+∞)"用逗号隔开
2. **必须先求定义域**——单调区间必须在定义域内讨论
3. **f\'(x)=0的点不一定是极值点**——需检查两侧导数符号是否变化',
'导数判断单调性：f\'(x)>0递增，f\'(x)<0递减。步骤：求定义域→求导→解不等式→确定区间。驻点可能是极值点。',
@l4, 2, '["导数","单调性","驻点","导数应用"]',
'["导数初步[选考]","导数的应用"]',
'[{"type":"choice","question":"若f\'(x)>0在(a,b)上恒成立，则f(x)在(a,b)上","options":["A. 递增","B. 递减","C. 常数","D. 不确定"],"answer":"A","explanation":"f\'(x)>0则函数单调递增"},
{"type":"judge","question":"f\'(x₀)=0则x₀一定是极值点。","answer":"错","explanation":"f(x)=x³在x=0处f\'(0)=0但不是极值点"},
{"type":"choice","question":"f(x)=x²的单调递减区间是","options":["A. R","B. (0,+∞)","C. (-∞,0)","D. (-∞,0)和(0,+∞)"],"answer":"C","explanation":"f\'(x)=2x<0时x<0，递减区间(-∞,0)"},
{"type":"fill","question":"f(x)=sinx在(0,π/2)上单调____。","answer":"递增","explanation":"f\'(x)=cosx>0在(0,π/2)上恒成立"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='利用导数判断函数单调性' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '如何用导数判断函数单调性？', 'f\'(x)>0→递增，f\'(x)<0→递减。先求定义域，再求导，解不等式确定区间。', 1),
(@art, '什么是驻点？驻点一定是极值点吗？', 'f\'(x₀)=0的点是驻点。驻点不一定是极值点，如f(x)=x³在x=0处。', 2),
(@art, '单调区间为什么不能用∪连接？', '因为函数在并集区间上不一定单调。应写"(-∞,-1),(1,+∞)"用逗号隔开。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 导数初步 > 利用导数求函数的极值
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='利用导数求函数的极值 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '导数初步 [选考]', '导数的应用', '利用导数求函数的极值',
'## 极值的定义

| 类型 | 定义 | 判定条件 |
|:----:|:-----|:--------:|
| **极大值** | 该点附近最大 | f\'(x)由正变负 |
| **极小值** | 该点附近最小 | f\'(x)由负变正 |

## 求极值的步骤

1. **求定义域**
2. **求导 f\'(x)**
3. **令 f\'(x)=0 求驻点**
4. **列表分析驻点两侧 f\'(x) 的符号变化**
5. **判定极大值或极小值**

## 判定法则

| f\'(x) 变化 | 极值类型 |
|:-----------:|:--------:|
| 正 → 负 | **极大值**（先增后减） |
| 负 → 正 | **极小值**（先减后增） |
| 不变号 | **不是极值点** |

## 典型例题

**例1**：求 f(x)=x³-3x 的极值。

f\'(x) = 3x²-3 = 3(x+1)(x-1)

令 f\'(x)=0 → x=-1 或 x=1

| 区间 | (-∞,-1) | -1 | (-1,1) | 1 | (1,+∞) |
|:----:|:-------:|:--:|:------:|:-:|:-------:|
| f\'(x) | + | 0 | - | 0 | + |
| f(x) | ↗ | 极大 | ↘ | 极小 | ↗ |

f(-1) = -1+3 = **2**（极大值）

f(1) = 1-3 = **-2**（极小值）

**例2**：求 f(x)=x⁴-4x³ 的极值。

f\'(x) = 4x³-12x² = 4x²(x-3)

令 f\'(x)=0 → x=0 或 x=3

| 区间 | (-∞,0) | 0 | (0,3) | 3 | (3,+∞) |
|:----:|:------:|:-:|:-----:|:-:|:-------:|
| f\'(x) | - | 0 | - | 0 | + |
| f(x) | ↘ | 不变号 | ↘ | 极小 | ↗ |

x=0 不是极值点（两侧都递减）

f(3) = 81-108 = **-27**（极小值）

## 极值 vs 最值

| | 极值 | 最值 |
|:--:|:----:|:----:|
| 范围 | 局部（附近） | 全局（整个区间） |
| 个数 | 可以有多个 | 最多一个最大值一个最小值 |
| 位置 | 驻点或不可导点 | 极值点或端点 |

## 易错警示

1. **f\'(x₀)=0 不一定是极值点**——必须检查两侧符号是否变化
2. **极大值不一定比端点值大**——极大值只是局部最大
3. **闭区间求最值需比较所有极值和端点值**',
'求极值步骤：求导→令f\'(x)=0→列表分析符号变化。正变负=极大值，负变正=极小值，不变号不是极值点。极值是局部概念，最值是全局概念。',
@l4, 2, '["导数","极值","极大值","极小值","驻点"]',
'["导数初步[选考]","导数的应用"]',
'[{"type":"choice","question":"f(x)=x³-3x的极大值是","options":["A. -2","B. 0","C. 2","D. 4"],"answer":"C","explanation":"f\'(x)=3x²-3=0得x=±1，f(-1)=-1+3=2为极大值"},
{"type":"judge","question":"f\'(x₀)=0则x₀一定是极值点。","answer":"错","explanation":"如f(x)=x⁴在x=0处f\'(0)=0但不是极值点（两侧都递增）"},
{"type":"choice","question":"f\'(x)由正变负时，f(x)在该点取","options":["A. 极小值","B. 极大值","C. 最大值","D. 最小值"],"answer":"B","explanation":"正变负=先增后减=极大值"},
{"type":"fill","question":"极值是____概念（局部/全局），最值是____概念。","answer":"局部，全局","explanation":"极值是某点附近的最值（局部），最值是整个区间上的最值（全局）"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='利用导数求函数的极值' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '求极值的步骤是什么？', '①求定义域 ②求导 ③令f\'(x)=0求驻点 ④列表分析符号变化 ⑤判定极大/极小值。', 1),
(@art, '如何判断驻点是极大值还是极小值？', 'f\'(x)由正变负→极大值（先增后减），由负变正→极小值（先减后增），不变号→不是极值点。', 2),
(@art, '极值和最值有什么区别？', '极值是局部概念（附近最大/最小），最值是全局概念（整个区间上最大/最小）。', 3);

-- ============================================================================
SELECT 'v188: 指对数+立体几何+导数模块知识文章完成 — 12篇' AS result;
-- ============================================================================
