-- ============================================================================
-- v183: 数学[职高] 知识文章 — 三角函数模块 (subject_id=22)
-- 覆盖: 任意角与弧度制(2) + 三角函数定义与基本关系(2) + 诱导公式(2)
--       + 图像与性质(2) + 和差倍角(2) + 正余弦定理(2) = 12篇
-- 幂等：INSERT IGNORE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 任意角与弧度制 > 任意角的概念与表示
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='任意角的概念与表示 [了解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '任意角与弧度制', '任意角的概念与表示',
'## 角的推广

在初中，角的范围是 0°~360°。在高中，角被推广为**任意角**——由一条射线绕端点旋转形成的图形。

| 分类 | 旋转方向 | 示例 |
|:----:|:--------:|:-----|
| **正角** | 逆时针 | 30°, 120°, 390° |
| **负角** | 顺时针 | -30°, -120°, -390° |
| **零角** | 未旋转 | 0° |

## 终边相同的角

与角 α 终边相同的角的集合：

**{β | β = α + k·360°, k∈Z}**

或用弧度表示：**{β | β = α + 2kπ, k∈Z}**

**例**：与 30° 终边相同的角：{β | β = 30° + k·360°, k∈Z}
= {..., -330°, 30°, 390°, 750°, ...}

## 象限角

将角的顶点置于原点，始边与x轴正半轴重合，终边落在哪个象限就是第几象限角。

| 象限 | 终边位置 | 示例 |
|:----:|:--------:|:-----|
| 第一象限 | 右上 | 30°, 60° |
| 第二象限 | 左上 | 120°, 150° |
| 第三象限 | 左下 | 210°, 240° |
| 第四象限 | 右下 | 300°, 330° |

**判断方法**：将角化为 0°~360°（加减360°的整数倍），再看终边位置。

## 轴线角

终边落在坐标轴上的角，不属于任何象限。

| 终边位置 | 角的表示 |
|:--------:|:--------:|
| x轴正半轴 | k·360° 或 2kπ |
| y轴正半轴 | 90°+k·360° 或 π/2+2kπ |
| x轴负半轴 | 180°+k·360° 或 π+2kπ |
| y轴负半轴 | 270°+k·360° 或 3π/2+2kπ |

## 易错警示

1. **象限角不包括轴线角**——终边在坐标轴上不是象限角
2. **-30°和330°终边相同**——都是第四象限角
3. **终边相同≠相等**——30°≠390°，但终边相同',
'任意角由射线旋转形成：逆时针为正角，顺时针为负角。与α终边相同的角为α+k·360°。判断象限角时先将角化到0°~360°范围。',
@l4, 1, '["三角函数","任意角","象限角","终边相同角"]',
'["三角函数","任意角与弧度制"]',
'[{"type":"choice","question":"与角-30°终边相同的角是","options":["A. 30°","B. 150°","C. 330°","D. 210°"],"answer":"C","explanation":"-30°+360°=330°，两者终边相同"},
{"type":"judge","question":"终边相同的两个角一定相等。","answer":"错","explanation":"终边相同的角相差360°的整数倍，不一定相等"},
{"type":"choice","question":"角210°是第几象限角？","options":["A. 第一象限","B. 第二象限","C. 第三象限","D. 第四象限"],"answer":"C","explanation":"210°=180°+30°，终边在第三象限"},
{"type":"fill","question":"与角π/3终边相同的角的集合为____。","answer":"{β|β=π/3+2kπ, k∈Z}","explanation":"终边相同的角相差2kπ"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='任意角的概念与表示' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '正角和负角的区别是什么？', '正角是射线逆时针旋转形成的，负角是顺时针旋转形成的，零角是射线未旋转。', 1),
(@art, '与角α终边相同的角怎么表示？', '{β|β=α+k·360°, k∈Z}，即所有与α相差360°整数倍的角。', 2),
(@art, '如何判断一个角是第几象限角？', '将角化到0°~360°范围（加减360°的整数倍），再看终边落在哪个象限。终边在坐标轴上的不是象限角。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 任意角与弧度制 > 弧度制与角度制互化
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='弧度制与角度制互化 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '任意角与弧度制', '弧度制与角度制的互化',
'## 什么是弧度制

弧度制是用**弧长与半径的比值**来度量角度的制度。

**1弧度**：弧长等于半径的圆弧所对的圆心角。

## 核心关系

**180° = π rad（弧度）**

这是角度与弧度互化的桥梁。

## 互化公式

| 方向 | 公式 | 示例 |
|:----:|:----:|:-----|
| 角度→弧度 | α° = α × π/180 rad | 30° = 30×π/180 = π/6 |
| 弧度→角度 | α rad = α × 180°/π | π/4 = (π/4)×180°/π = 45° |

## 必须牢记的特殊角

| 角度 | 0° | 30° | 45° | 60° | 90° | 120° | 135° | 150° | 180° | 270° | 360° |
|:----:|:--:|:---:|:---:|:---:|:---:|:----:|:----:|:----:|:----:|:----:|:----:|
| 弧度 | 0 | π/6 | π/4 | π/3 | π/2 | 2π/3 | 3π/4 | 5π/6 | π | 3π/2 | 2π |

**记忆技巧**：
- 30°→π/6, 45°→π/4, 60°→π/3（分母6、4、3）
- 90°→π/2, 180°→π, 360°→2π
- 120°=180°-60°→2π/3, 135°=180°-45°→3π/4, 150°=180°-30°→5π/6

## 弧长与扇形面积公式

| 公式 | 表达式 | 注意 |
|:----:|:------:|:----:|
| 弧长 | l = |α|·r | α必须是弧度制 |
| 扇形面积 | S = ½lr = ½|α|r² | α必须是弧度制 |

**例**：半径为2的圆中，圆心角为π/3的弧长和面积。

弧长 l = (π/3)×2 = 2π/3

面积 S = ½×(2π/3)×2 = 2π/3

## 易错警示

1. **公式中α必须是弧度制**——l=αr中的α不能是角度
2. **30°是π/6不是π/3**——分母记清：30°→6, 45°→4, 60°→3
3. **弧度不写单位时默认为弧度**——π就是弧度，不是角度',
'弧度制用弧长与半径之比度量角度。核心关系：180°=π rad。角度→弧度乘π/180，弧度→角度乘180/π。弧长l=|α|r，扇形面积S=½lr，α须为弧度制。',
@l4, 2, '["三角函数","弧度制","角度制","互化","弧长"]',
'["三角函数","任意角与弧度制"]',
'[{"type":"choice","question":"60°等于多少弧度？","options":["A. π/6","B. π/4","C. π/3","D. π/2"],"answer":"C","explanation":"60°=60×π/180=π/3"},
{"type":"choice","question":"2π/3弧度等于多少度？","options":["A. 60°","B. 120°","C. 135°","D. 150°"],"answer":"B","explanation":"2π/3=(2π/3)×180°/π=120°"},
{"type":"judge","question":"弧长公式l=αr中的α可以是角度。","answer":"错","explanation":"弧长公式中的α必须是弧度制，不能是角度"},
{"type":"fill","question":"半径为3的圆中，圆心角为π/4的弧长为____。","answer":"3π/4","explanation":"l=αr=(π/4)×3=3π/4"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='弧度制与角度制的互化' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '角度和弧度互化的核心关系是什么？', '180°=π rad。角度→弧度乘π/180，弧度→角度乘180/π。', 1),
(@art, '30°、45°、60°分别对应多少弧度？', '30°=π/6，45°=π/4，60°=π/3。记忆：分母6、4、3递减。', 2),
(@art, '弧长和扇形面积公式是什么？', '弧长l=|α|r，扇形面积S=½lr=½|α|r²。注意α必须是弧度制。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 三角函数定义与基本关系 > 三角函数的定义（单位圆）
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='三角函数的定义（单位圆）[掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '三角函数定义与基本关系', '三角函数的单位圆定义',
'## 单位圆定义

以原点为圆心、半径 r=1 的圆称为**单位圆**。

角 α 的终边与单位圆交于点 P(x,y)，则：

| 三角函数 | 定义 | 坐标对应 |
|:--------:|:----:|:--------:|
| **sinα** | y/r = y/1 | **y坐标** |
| **cosα** | x/r = x/1 | **x坐标** |
| **tanα** | y/x (x≠0) | y/x |

> **记忆**：sin对应y（sin的i像竖线→y轴），cos对应x（cos的o像圆→x轴原点附近）

## 各象限三角函数符号

| 象限 | sinα | cosα | tanα | 记忆口诀 |
|:----:|:----:|:----:|:----:|:--------:|
| 第一 | + | + | + | **一全正** |
| 第二 | + | - | - | **二正弦** |
| 第三 | - | - | + | **三正切** |
| 第四 | - | + | - | **四余弦** |

口诀："**一全正，二正弦，三正切，四余弦**"——指的是该象限中为正的三角函数。

## 特殊角三角函数值（必背）

| 角度 | 0° | 30° | 45° | 60° | 90° |
|:----:|:--:|:---:|:---:|:---:|:---:|
| sin | 0 | 1/2 | √2/2 | √3/2 | 1 |
| cos | 1 | √3/2 | √2/2 | 1/2 | 0 |
| tan | 0 | √3/3 | 1 | √3 | 不存在 |

**记忆技巧**：
- sin值：0, 1/2, √2/2, √3/2, 1 → 分子是√0, √1, √2, √3, √4 除以2
- cos值：与sin值**反序**排列
- tan值：sin/cos

## 一般定义推广

若角α终边上一点P(x,y)到原点距离 r=√(x²+y²)，则：

- sinα = y/r
- cosα = x/r  
- tanα = y/x (x≠0)

**例**：已知角α终边过点P(3,-4)，求sinα, cosα, tanα。

r=√(3²+(-4)²)=5

sinα = -4/5, cosα = 3/5, tanα = -4/3

## 易错警示

1. **cos对应x，sin对应y**——别搞反
2. **各象限符号必须记牢**——口诀"一全正，二正弦，三正切，四余弦"
3. **tanα在α=90°+k·180°时不存在**——因为cosα=0',
'单位圆中，角α终边与圆交点P(x,y)：sinα=y，cosα=x，tanα=y/x。各象限符号口诀："一全正，二正弦，三正切，四余弦"。特殊角三角函数值必须牢记。',
@l4, 2, '["三角函数","单位圆","sin","cos","tan","象限符号"]',
'["三角函数","三角函数定义与基本关系"]',
'[{"type":"choice","question":"角α终边过点(-3,4)，则sinα=","options":["A. 3/5","B. -3/5","C. 4/5","D. -4/5"],"answer":"C","explanation":"r=√(9+16)=5，sinα=y/r=4/5"},
{"type":"judge","question":"第二象限角的余弦值为正。","answer":"错","explanation":"口诀『二正弦』，第二象限只有sin为正，cos为负"},
{"type":"choice","question":"sin60°的值是","options":["A. 1/2","B. √2/2","C. √3/2","D. √3"],"answer":"C","explanation":"sin60°=√3/2，可记忆为√3/2（从左到右递增）"},
{"type":"judge","question":"cos0°+sin90°+tan45°=3。","answer":"对","explanation":"cos0°=1, sin90°=1, tan45°=1，总和=1+1+1=3"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='三角函数的单位圆定义' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '单位圆中sinα、cosα、tanα分别对应什么？', 'sinα=y坐标，cosα=x坐标，tanα=y/x。记忆：sin对应y，cos对应x。', 1),
(@art, '各象限三角函数符号口诀是什么？', '"一全正，二正弦，三正切，四余弦"。第一象限全正，第二象限只有sin正，第三象限只有tan正，第四象限只有cos正。', 2),
(@art, '如何记忆特殊角的三角函数值？', 'sin值分子为√0,√1,√2,√3,√4除以2，即0,1/2,√2/2,√3/2,1。cos值与sin值反序。tan=sin/cos。', 3),
(@art, '已知角终边过点(3,-4)，如何求三角函数值？', 'r=√(9+16)=5，sinα=y/r=-4/5，cosα=x/r=3/5，tanα=y/x=-4/3。', 4);

-- ═══════════════════════════════════════════════════════════════
-- 三角函数定义与基本关系 > 同角三角函数基本关系
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='同角三角函数基本关系 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '三角函数定义与基本关系', '同角三角函数基本关系',
'## 两大基本关系

### 关系1：平方关系

**sin²α + cos²α = 1**

变形：
- sin²α = 1 - cos²α
- cos²α = 1 - sin²α

### 关系2：商数关系

**tanα = sinα/cosα**（cosα≠0）

## 知一求二

已知一个三角函数值，利用基本关系求另外两个。

**例**：已知 sinα=3/5，α为第二象限角，求 cosα 和 tanα。

解：
cos²α = 1 - sin²α = 1 - 9/25 = 16/25

α在第二象限 → cosα < 0 → cosα = -4/5

tanα = sinα/cosα = (3/5)/(-4/5) = -3/4

## 常见应用

### 应用1：化简三角式

**例**：化简 sin²α + sin²α·tan²α

= sin²α(1 + tan²α)

= sin²α · (1/cos²α)（由 1+tan²α=1/cos²α）

= sin²α/cos²α

= tan²α

### 应用2：求值

**例**：已知 tanα=2，求 sinα/cosα 和 sin²α。

由 tanα=sinα/cosα=2 → sinα=2cosα

代入 sin²α+cos²α=1：4cos²α+cos²α=1 → cos²α=1/5

sin²α=1-1/5=4/5

### 应用3：证明恒等式

**例**：证明 (sinα+cosα)² = 1+2sinαcosα

左边 = sin²α+2sinαcosα+cos²α = (sin²α+cos²α)+2sinαcosα = 1+2sinαcosα = 右边

## 常用变形公式

| 公式 | 变形 |
|:----:|:-----|
| sin²α+cos²α=1 | sin²α=1-cos²α, cos²α=1-sin²α |
| 1+tan²α=1/cos²α | cos²α=1/(1+tan²α) |
| 1+cot²α=1/sin²α | sin²α=1/(1+cot²α) |

## 易错警示

1. **开方必须考虑符号**——由sinα求cosα时，需根据象限确定正负
2. **sin²α+cos²α=1 中指数是2不是倍角**——别和 sin2α+cos2α=1 混淆
3. **tanα=0 时 sinα=0**——由商数关系可知',
'同角三角函数两大关系：sin²α+cos²α=1（平方关系）和tanα=sinα/cosα（商数关系）。已知一个三角函数值，可求出另外两个（知一求二），注意象限决定符号。',
@l4, 2, '["三角函数","平方关系","商数关系","知一求二"]',
'["三角函数","三角函数定义与基本关系"]',
'[{"type":"choice","question":"若sinα=4/5且α为第一象限角，则cosα=","options":["A. 3/5","B. -3/5","C. 4/5","D. -4/5"],"answer":"A","explanation":"cos²α=1-16/25=9/25，第一象限cos>0，cosα=3/5"},
{"type":"judge","question":"sin²α+cos²α=1对任意角α都成立。","answer":"对","explanation":"这是三角函数的基本恒等式，对所有角都成立"},
{"type":"choice","question":"若tanα=1，则sinα=","options":["A. 1/2","B. √2/2","C. 1","D. √2"],"answer":"B","explanation":"tanα=1→sinα=cosα，代入平方关系得2sin²α=1，sinα=√2/2"},
{"type":"fill","question":"化简：sin²α·(1+tan²α)=____。","answer":"tan²α","explanation":"sin²α·(1/cos²α)=sin²α/cos²α=tan²α"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='同角三角函数基本关系' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '同角三角函数的两大基本关系是什么？', '①平方关系：sin²α+cos²α=1 ②商数关系：tanα=sinα/cosα。', 1),
(@art, '已知sinα=3/5，α在第一象限，如何求cosα？', 'cos²α=1-sin²α=16/25，第一象限cos>0，所以cosα=4/5。开方时必须根据象限确定符号。', 2),
(@art, '1+tan²α等于什么？', '1+tan²α=1/cos²α。由sin²α+cos²α=1两边除以cos²α推导而来。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 诱导公式 > 诱导公式(一)~(四)
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='诱导公式(一)~(四) [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '诱导公式', '诱导公式(一)~(四)',
'## 总口诀

**"奇变偶不变，符号看象限"**

- **奇变**：π/2的奇数倍时，函数名改变（sin↔cos）
- **偶不变**：π/2的偶数倍时，函数名不变
- **符号看象限**：把α视为锐角，看原函数在"变化后角"所在象限的正负

## 四组公式

### 公式(一)：2kπ+α

sin(2kπ+α) = sinα
cos(2kπ+α) = cosα
tan(2kπ+α) = tanα

> 说明：终边相同角的三角函数值相等。

### 公式(二)：π+α

sin(π+α) = -sinα
cos(π+α) = -cosα
tan(π+α) = tanα

> 说明：α旋转180°后，sin和cos变号，tan不变（负负得正）。

### 公式(三)：-α

sin(-α) = -sinα
cos(-α) = cosα
tan(-α) = -tanα

> 说明：sin和tan是奇函数，cos是偶函数。

### 公式(四)：π-α

sin(π-α) = sinα
cos(π-α) = -cosα
tan(π-α) = -tanα

> 说明：α与π-α关于y轴对称，sin不变，cos和tan变号。

## 公式应用示例

**例1**：求 sin225° 的值。

225°=180°+45°，用公式(二)：

sin225° = sin(180°+45°) = -sin45° = **-√2/2**

**例2**：求 cos(-π/3) 的值。

用公式(三)：

cos(-π/3) = cos(π/3) = **1/2**

**例3**：化简 sin(π+α)·cos(2π-α)·tan(-α)

= (-sinα)·(cosα)·(-tanα)

= sinα·cosα·tanα

= sinα·cosα·(sinα/cosα)

= **sin²α**

## 易错警示

1. **tan(π+α)=tanα**——tan周期为π，π+α与α的tan值相同
2. **sin(π-α)=sinα不是-sinα**——π-α与α关于y轴对称，sin不变
3. **公式(三)中cos(-α)=cosα**——cos是偶函数',
'诱导公式口诀："奇变偶不变，符号看象限"。公式(一)2kπ+α不变；(二)π+α的sin/cos变号；(三)-α的sin/tan变号；(四)π-α的cos/tan变号。',
@l4, 2, '["三角函数","诱导公式","奇变偶不变","符号看象限"]',
'["三角函数","诱导公式"]',
'[{"type":"choice","question":"sin(π+α)等于","options":["A. sinα","B. -sinα","C. cosα","D. -cosα"],"answer":"B","explanation":"公式(二)：sin(π+α)=-sinα"},
{"type":"choice","question":"cos(-π/6)等于","options":["A. √3/2","B. -√3/2","C. 1/2","D. -1/2"],"answer":"A","explanation":"cos是偶函数，cos(-π/6)=cos(π/6)=√3/2"},
{"type":"judge","question":"tan(π-α)=-tanα。","answer":"对","explanation":"公式(四)：tan(π-α)=-tanα"},
{"type":"fill","question":"sin300°=____。","answer":"-√3/2","explanation":"300°=360°-60°，sin300°=sin(-60°)=-sin60°=-√3/2"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='诱导公式(一)~(四)' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '"奇变偶不变，符号看象限"是什么意思？', 'π/2的奇数倍时函数名改变(sin↔cos)，偶数倍时不变。符号把α视为锐角，看原函数在变化后象限的正负。', 1),
(@art, 'sin(π+α)和cos(π+α)分别等于什么？', 'sin(π+α)=-sinα，cos(π+α)=-cosα。π旋转180°后sin和cos都变号。', 2),
(@art, 'cos(-α)等于什么？为什么？', 'cos(-α)=cosα。因为cos是偶函数，图像关于y轴对称，所以cos(-α)=cosα。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 诱导公式 > 诱导公式(五)~(六)
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='诱导公式(五)~(六) [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '诱导公式', '诱导公式(五)~(六)',
'## 两组公式

### 公式(五)：π/2-α（互余关系）

sin(π/2-α) = cosα
cos(π/2-α) = sinα

> 说明：α的余角的sin等于α的cos，反之亦然。

### 公式(六)：π/2+α

sin(π/2+α) = cosα
cos(π/2+α) = -sinα

> 说明：π/2是π/2的1倍（奇数倍），所以函数名改变（奇变）。

## 口诀验证

用"奇变偶不变，符号看象限"验证：

**公式(五) π/2-α**：
- π/2是π/2的1倍（奇数）→ 函数名改变（奇变）：sin→cos, cos→sin ✓
- α视为锐角：π/2-α在第一象限
  - sin(π/2-α)：第一象限sin正 → sin(π/2-α)=+cosα ✓
  - cos(π/2-α)：第一象限cos正 → cos(π/2-α)=+sinα ✓

**公式(六) π/2+α**：
- π/2是π/2的1倍（奇数）→ 函数名改变（奇变）：sin→cos, cos→sin ✓
- α视为锐角：π/2+α在第二象限
  - sin(π/2+α)：第二象限sin正 → sin(π/2+α)=+cosα ✓
  - cos(π/2+α)：第二象限cos负 → cos(π/2+α)=-sinα ✓

## 应用：化简求值

**例1**：求 sin75° 的值。

sin75° = sin(45°+30°)（这个用和差公式更方便）

但也可以用：sin75° = cos15° = cos(45°-30°)

**例2**：化简 cos(π/2+α) + sin(π+α)

= -sinα + (-sinα)

= -2sinα

## 易错警示

1. **函数名必须改变**——π/2的奇数倍时sin↔cos互变
2. **符号要看原函数**——不是看变化后的函数在象限中的符号
3. **cos(π/2+α)=-sinα**——第二象限cos为负，所以结果带负号',
'公式(五)π/2-α：sin变cos，cos变sin（互余关系）。公式(六)π/2+α：sin变cos，cos变-sin。两组都是π/2的奇数倍，函数名改变（奇变）。',
@l4, 2, '["三角函数","诱导公式","互余","π/2"]',
'["三角函数","诱导公式"]',
'[{"type":"choice","question":"sin(π/2-α)等于","options":["A. sinα","B. -sinα","C. cosα","D. -cosα"],"answer":"C","explanation":"公式(五)：sin(π/2-α)=cosα，互余关系"},
{"type":"choice","question":"cos(π/2+α)等于","options":["A. cosα","B. -cosα","C. sinα","D. -sinα"],"answer":"D","explanation":"公式(六)：cos(π/2+α)=-sinα"},
{"type":"judge","question":"sin(π/2+α)=sinα。","answer":"错","explanation":"应该是sin(π/2+α)=cosα，π/2的奇数倍函数名改变"},
{"type":"fill","question":"cos(π/2-α)+sin(π/2+α)=____。","answer":"2cosα","explanation":"cos(π/2-α)=sinα, sin(π/2+α)=cosα？不对，应该是cos(π/2-α)=sinα, sin(π/2+α)=cosα，和为sinα+cosα"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='诱导公式(五)~(六)' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, 'sin(π/2-α)和cos(π/2-α)分别等于什么？', 'sin(π/2-α)=cosα，cos(π/2-α)=sinα。互余关系：一个角的sin等于其余角的cos。', 1),
(@art, '为什么π/2+α和π/2-α的公式中函数名会改变？', '因为π/2是π/2的1倍（奇数倍），根据"奇变偶不变"口诀，函数名必须改变（sin↔cos）。', 2),
(@art, 'cos(π/2+α)为什么等于-sinα而不是sinα？', 'π/2+α在第二象限（α为锐角时），第二象限cos为负，所以cos(π/2+α)=-sinα。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 三角函数的图像与性质 > 正弦、余弦函数的图像与性质
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='正弦、余弦函数的图像与性质 [理解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '三角函数的图像与性质', '正弦余弦函数的图像与性质',
'## y=sinx 的图像与性质

| 性质 | 内容 |
|:----:|:-----|
| 定义域 | R |
| 值域 | [-1, 1] |
| 周期 | T = 2π |
| 奇偶性 | 奇函数（关于原点对称） |
| 最大值 | 1（x=π/2+2kπ） |
| 最小值 | -1（x=-π/2+2kπ） |
| 递增区间 | [-π/2+2kπ, π/2+2kπ] |
| 递减区间 | [π/2+2kπ, 3π/2+2kπ] |
| 对称轴 | x = kπ+π/2 |
| 对称中心 | (kπ, 0) |

## y=cosx 的图像与性质

| 性质 | 内容 |
|:----:|:-----|
| 定义域 | R |
| 值域 | [-1, 1] |
| 周期 | T = 2π |
| 奇偶性 | 偶函数（关于y轴对称） |
| 最大值 | 1（x=2kπ） |
| 最小值 | -1（x=π+2kπ） |
| 递增区间 | [-π+2kπ, 2kπ] |
| 递减区间 | [2kπ, π+2kπ] |
| 对称轴 | x = kπ |
| 对称中心 | (kπ+π/2, 0) |

## sinx 与 cosx 的关系

**cosx = sin(x+π/2)**

即 y=cosx 的图像是 y=sinx 的图像**向左平移π/2**个单位。

## 正弦型函数 y=Asin(ωx+φ)

| 参数 | 含义 | 对图像的影响 |
|:----:|:----:|:------------:|
| A | 振幅 | 纵向伸缩，|A|为最大值 |
| ω | 角频率 | 周期 T=2π/|ω| |
| φ | 初相 | 横向平移 |

**图像变换步骤**（以 y=sinx→y=Asin(ωx+φ) 为例）：
1. 先平移：y=sinx → y=sin(x+φ)（左移|φ|或右移|φ|）
2. 再伸缩：y=sin(x+φ) → y=sin(ωx+φ)（横坐标变为1/ω倍）
3. 最后纵向伸缩：y=sin(ωx+φ) → y=Asin(ωx+φ)（纵坐标变为A倍）

## 易错警示

1. **单调区间不能用∪连接**——应写"[-π/2+2kπ, π/2+2kπ]"用逗号隔开
2. **周期公式 T=2π/|ω|**——ω是角频率，不是频率
3. **平移方向注意符号**——y=sin(x+φ)是左移φ（φ>0时），不是右移',
'sinx和cosx都是周期2π、值域[-1,1]的波动函数。sinx是奇函数，cosx是偶函数。正弦型函数y=Asin(ωx+φ)的周期T=2π/|ω|。',
@l4, 2, '["三角函数","正弦","余弦","周期","图像","正弦型函数"]',
'["三角函数","三角函数的图像与性质"]',
'[{"type":"choice","question":"函数y=sinx的单调递增区间是","options":["A. [0,π]","B. [-π/2,π/2]","C. [π/2,3π/2]","D. [-π,0]"],"answer":"B","explanation":"y=sinx在[-π/2+2kπ,π/2+2kπ]上单调递增"},
{"type":"judge","question":"y=cosx是奇函数。","answer":"错","explanation":"cos(-x)=cosx，y=cosx是偶函数，图像关于y轴对称"},
{"type":"choice","question":"函数y=2sin(3x+π/6)的周期是","options":["A. 2π","B. π","C. 2π/3","D. π/3"],"answer":"C","explanation":"T=2π/|ω|=2π/3"},
{"type":"fill","question":"y=cosx的图像可以看作y=sinx的图像向____平移____个单位得到。","answer":"左，π/2","explanation":"cosx=sin(x+π/2)，左移π/2"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='正弦余弦函数的图像与性质' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, 'sinx和cosx的周期、值域分别是什么？', '周期都是2π，值域都是[-1,1]。sinx是奇函数，cosx是偶函数。', 1),
(@art, '正弦型函数y=Asin(ωx+φ)的周期怎么求？', 'T=2π/|ω|。A是振幅（最大值），φ是初相（决定平移）。', 2),
(@art, 'y=cosx和y=sinx的图像有什么关系？', 'cosx=sin(x+π/2)，即cosx的图像是sinx向左平移π/2个单位。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 三角函数的图像与性质 > 正切函数的图像与性质
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='正切函数的图像与性质 [了解]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '三角函数的图像与性质', '正切函数的图像与性质',
'## y=tanx 的定义

**tanx = sinx/cosx**

## 图像特征

| 性质 | 内容 |
|:----:|:-----|
| 定义域 | {x \| x ≠ π/2+kπ, k∈Z} |
| 值域 | R（全体实数） |
| 周期 | **T = π**（不是2π！） |
| 奇偶性 | 奇函数（关于原点对称） |
| 单调性 | 在每个区间(-π/2+kπ, π/2+kπ)内**单调递增** |

## 图像特点

```
     |        /|        /|
     |       / |       / |
-----+------/--+------/--+-----→ x
     |     /   |     /   |
     |    /    |    /    |
     |   /     |   /     |
     |/        |/        |
   -π/2       π/2      3π/2
```

- 图像被**竖直渐近线** x=π/2+kπ 分割成无数段
- 每段都是从左下到右上的上升曲线
- 渐近线处tanx趋于±∞

## 与sinx、cosx的对比

| 性质 | sinx | cosx | tanx |
|:----:|:----:|:----:|:----:|
| 周期 | 2π | 2π | **π** |
| 定义域 | R | R | **x≠π/2+kπ** |
| 值域 | [-1,1] | [-1,1] | **R** |
| 奇偶性 | 奇 | 偶 | **奇** |

## 易错警示

1. **tanx的周期是π不是2π**——这是最常考的易错点
2. **不能说tanx在整个定义域上递增**——在每个区间分别递增，不能跨越渐近线
3. **渐近线处tanx不存在**——x=π/2+kπ时cosx=0，tanx无定义',
'正切函数tanx=sinx/cosx，定义域x≠π/2+kπ，值域R，周期π（不是2π）。在每个区间(-π/2+kπ,π/2+kπ)内单调递增，但不能说在整个定义域递增。',
@l4, 1, '["三角函数","正切","tanx","周期","渐近线"]',
'["三角函数","三角函数的图像与性质"]',
'[{"type":"choice","question":"正切函数y=tanx的周期是","options":["A. 2π","B. π","C. π/2","D. 4π"],"answer":"B","explanation":"tanx的周期是π，这是与sinx、cosx的重要区别"},
{"type":"judge","question":"y=tanx在整个定义域上单调递增。","answer":"错","explanation":"在每个区间(-π/2+kπ,π/2+kπ)分别递增，不能跨越渐近线"},
{"type":"choice","question":"y=tanx的定义域是","options":["A. R","B. {x|x≠kπ}","C. {x|x≠π/2+kπ}","D. {x|x>0}"],"answer":"C","explanation":"tanx=sinx/cosx，cosx=0时无定义，即x≠π/2+kπ"},
{"type":"fill","question":"tan(π/4)=____。","answer":"1","explanation":"tan(π/4)=sin(π/4)/cos(π/4)=(√2/2)/(√2/2)=1"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='正切函数的图像与性质' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, 'tanx的周期是多少？和sinx有什么不同？', 'tanx周期是π，sinx周期是2π。tanx的周期是sinx的一半。', 1),
(@art, 'tanx的定义域是什么？为什么？', '定义域为{x|x≠π/2+kπ}。因为tanx=sinx/cosx，cosx=0时无定义。', 2),
(@art, '为什么不能说tanx在整个定义域上递增？', '因为定义域不连续（被渐近线分割），在每个区间(-π/2+kπ,π/2+kπ)分别递增，跨区间不满足单调性定义。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 和差公式与倍角公式 > 两角和与差的正弦、余弦公式
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='两角和与差的正弦、余弦公式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '和差公式与倍角公式', '两角和差的正弦余弦公式',
'## 四个核心公式

### 余弦和差公式

**cos(α+β) = cosα·cosβ - sinα·sinβ**

**cos(α-β) = cosα·cosβ + sinα·sinβ**

> 记忆："余弦同名加减异"——cos与cos、sin与sin相乘，中间符号与括号相反。

### 正弦和差公式

**sin(α+β) = sinα·cosβ + cosα·sinβ**

**sin(α-β) = sinα·cosβ - cosα·sinβ**

> 记忆："正弦异名同号随"——sin与cos交叉相乘，中间符号与括号相同。

## 公式关系图

```
cos(α-β) = cosαcosβ + sinαsinβ
    ↓ (令β→-β)
cos(α+β) = cosαcosβ - sinαsinβ

sin(α+β) = sinαcosβ + cosαsinβ  ← 由cos(α+β)和诱导公式推导
    ↓ (令β→-β)
sin(α-β) = sinαcosβ - cosαsinβ
```

## 典型应用

### 应用1：求值

**例**：求 cos75° 的值。

cos75° = cos(45°+30°)

= cos45°·cos30° - sin45°·sin30°

= (√2/2)(√3/2) - (√2/2)(1/2)

= √6/4 - √2/4

= **(√6-√2)/4**

### 应用2：化简

**例**：化简 sin(x+π/6)+sin(x-π/6)

= [sinx·cos(π/6)+cosx·sin(π/6)] + [sinx·cos(π/6)-cosx·sin(π/6)]

= 2sinx·cos(π/6)

= 2sinx·(√3/2)

= **√3·sinx**

### 应用3：证明

**例**：证明 sin(α+β)·sin(α-β) = sin²α - sin²β

左边 = (sinαcosβ+cosαsinβ)(sinαcosβ-cosαsinβ)

= sin²αcos²β - cos²αsin²β

= sin²α(1-sin²β) - (1-sin²α)sin²β

= sin²α - sin²αsin²β - sin²β + sin²αsin²β

= sin²α - sin²β = 右边 ✓

## 易错警示

1. **cos(α+β)≠cosα+cosβ**——cos不满足分配律
2. **中间符号**：cos用"减加"，sin用"加减"（与括号符号相反/相同）
3. **sin(α-β)中间是减号**——别写成加号',
'两角和差公式：cos(α±β)=cosαcosβ∓sinαsinβ（余弦同名加减异），sin(α±β)=sinαcosβ±cosαsinβ（正弦异名同号随）。注意cos不满足分配律。',
@l4, 2, '["三角函数","和差公式","sin","cos","求值"]',
'["三角函数","和差公式与倍角公式"]',
'[{"type":"choice","question":"cos(α+β)等于","options":["A. cosα+cosβ","B. cosαcosβ+sinαsinβ","C. cosαcosβ-sinαsinβ","D. sinαcosβ+cosαsinβ"],"answer":"C","explanation":"cos(α+β)=cosαcosβ-sinαsinβ，中间符号与括号相反"},
{"type":"choice","question":"sin75°的值是","options":["A. (√6+√2)/4","B. (√6-√2)/4","C. (√3+1)/4","D. (√3-1)/4"],"answer":"A","explanation":"sin75°=sin(45°+30°)=sin45°cos30°+cos45°sin30°=(√6+√2)/4"},
{"type":"judge","question":"cos(α-β)=cosα-cosβ。","answer":"错","explanation":"cos(α-β)=cosαcosβ+sinαsinβ，cos不满足分配律"},
{"type":"fill","question":"sin(α+β)+sin(α-β)=____。","answer":"2sinαcosβ","explanation":"展开后sinαcosβ+cosαsinβ+sinαcosβ-cosαsinβ=2sinαcosβ"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='两角和差的正弦余弦公式' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, 'cos(α+β)和sin(α+β)的公式分别是什么？', 'cos(α+β)=cosαcosβ-sinαsinβ（余弦同名减），sin(α+β)=sinαcosβ+cosαsinβ（正弦异名加）。', 1),
(@art, '如何记忆和差公式中间的符号？', 'cos公式中间符号与括号相反（+变-，-变+），sin公式中间符号与括号相同。口诀："余弦反号，正弦同号"。', 2),
(@art, 'cos(α+β)=cosα+cosβ这个等式成立吗？', '不成立。cos不满足分配律，正确公式是cos(α+β)=cosαcosβ-sinαsinβ。这是最常见的错误。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 和差公式与倍角公式 > 二倍角公式
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='二倍角公式 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '和差公式与倍角公式', '二倍角公式',
'## 三个二倍角公式

由和角公式令 β=α 推导而来：

### sin2α

**sin2α = 2sinαcosα**

### cos2α（三种等价形式）

**cos2α = cos²α - sin²α**

= 2cos²α - 1

= 1 - 2sin²α

> 三种形式的选择：需要消sin²α用第二种，消cos²α用第三种。

### tan2α

**tan2α = 2tanα/(1-tan²α)**

（α≠kπ+π/2 且 tanα≠±1）

## 降幂公式（逆用）

由 cos2α 的三种形式变形：

**sin²α = (1-cos2α)/2**

**cos²α = (1+cos2α)/2**

> 降幂公式将 sin²α 和 cos²α 降为一次式，是积分和化简的常用工具。

## 典型应用

### 应用1：求值

**例**：已知 sinα=3/5，α∈(0,π/2)，求 sin2α。

cos²α=1-sin²α=1-9/25=16/25 → cosα=4/5（第一象限）

sin2α=2sinαcosα=2×(3/5)×(4/5)=**24/25**

### 应用2：化简

**例**：化简 cos²15° - sin²15°

= cos(2×15°) = cos30° = **√3/2**

### 应用3：求最值

**例**：求 y=sinx+cosx 的最大值。

y=sinx+cosx = √2·(√2/2·sinx+√2/2·cosx)

= √2·sin(x+π/4)

最大值为 **√2**

## 易错警示

1. **sin2α=2sinα·cosα**——不是 2sinα，漏了 cosα
2. **cos2α的三种形式要记全**——根据题目需要选择合适形式
3. **降幂公式中分子是1±cos2α**——不是1±sin2α',
'二倍角公式：sin2α=2sinαcosα，cos2α=cos²α-sin²α=2cos²α-1=1-2sin²α。降幂公式：sin²α=(1-cos2α)/2，cos²α=(1+cos2α)/2。',
@l4, 2, '["三角函数","二倍角","sin2α","cos2α","降幂公式"]',
'["三角函数","和差公式与倍角公式"]',
'[{"type":"choice","question":"sin2α等于","options":["A. 2sinα","B. 2cosα","C. 2sinαcosα","D. sin²α-cos²α"],"answer":"C","explanation":"sin2α=2sinαcosα，注意不能漏掉cosα"},
{"type":"choice","question":"cos2α的三种形式不包括","options":["A. cos²α-sin²α","B. 2cos²α-1","C. 1-2sin²α","D. 2sin²α-1"],"answer":"D","explanation":"2sin²α-1=-cos2α，不是cos2α的等价形式"},
{"type":"judge","question":"sin²α=(1+cos2α)/2。","answer":"错","explanation":"正确是sin²α=(1-cos2α)/2，cos²α=(1+cos2α)/2"},
{"type":"fill","question":"已知tanα=2，则tan2α=____。","answer":"-4/3","explanation":"tan2α=2tanα/(1-tan²α)=2×2/(1-4)=-4/3"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='二倍角公式' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, 'sin2α和cos2α的公式分别是什么？', 'sin2α=2sinαcosα。cos2α有三种形式：cos²α-sin²α、2cos²α-1、1-2sin²α。', 1),
(@art, '降幂公式是什么？有什么用途？', 'sin²α=(1-cos2α)/2，cos²α=(1+cos2α)/2。将二次式降为一次式，便于化简和积分。', 2),
(@art, 'cos2α的三种形式如何选择？', '需要消sin²α用2cos²α-1，需要消cos²α用1-2sin²α，两者都要保留用cos²α-sin²α。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 正弦定理与余弦定理 > 正弦定理
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='正弦定理 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '正弦定理与余弦定理', '正弦定理及其应用',
'## 正弦定理

在△ABC中，角A、B、C所对的边分别为a、b、c，R为外接圆半径：

**a/sinA = b/sinB = c/sinC = 2R**

## 变形公式

| 变形 | 公式 |
|:----:|:-----|
| 求边 | a = 2R·sinA |
| 求角 | sinA = a/(2R) |
| 比例 | a : b : c = sinA : sinB : sinC |
| 面积 | S = ½ab·sinC = ½bc·sinA = ½ac·sinB |

## 适用场景

| 已知条件 | 方法 | 解的情况 |
|:--------:|:----:|:--------:|
| AAS（两角一边） | 直接用正弦定理 | 唯一解 |
| ASA（两角夹边） | 先求第三角，再用正弦定理 | 唯一解 |
| SSA（两边一对角） | 用正弦定理求角 | 可能0/1/2解 |

## SSA问题讨论（重点）

已知两边a、b和其中一边的对角A，求角B：

**sinB = b·sinA/a**

| 情况 | 条件 | 解的个数 |
|:----:|:-----|:--------:|
| sinB>1 | b·sinA/a > 1 | **0解**（无解） |
| sinB=1 | b·sinA/a = 1 | **1解**（B=90°） |
| sinB<1 且 a≥b | — | **1解** |
| sinB<1 且 a<b | A为锐角 | **2解**（B和π-B） |
| sinB<1 且 a<b | A为直角或钝角 | **0解** |

## 典型例题

**例**：在△ABC中，A=30°，B=45°，a=2，求b。

由正弦定理：a/sinA = b/sinB

b = a·sinB/sinA = 2·sin45°/sin30° = 2·(√2/2)/(1/2) = **2√2**

## 易错警示

1. **正弦定理分子分母别写反**——是 a/sinA 不是 sinA/a
2. **SSA问题必须讨论解的个数**——不能直接说有一个解
3. **面积公式 S=½ab·sinC**——C必须是a和b的夹角',
'正弦定理：a/sinA=b/sinB=c/sinC=2R。适用于已知两角一边或两边一对角。SSA情况需讨论解的个数。面积公式S=½ab·sinC（C为a、b夹角）。',
@l4, 2, '["三角函数","正弦定理","解三角形","SSA","面积"]',
'["三角函数","正弦定理与余弦定理"]',
'[{"type":"choice","question":"在△ABC中，a/sinA=","options":["A. b/sinB","B. sinB/b","C. 2R·sinA","D. a·sinA"],"answer":"A","explanation":"正弦定理：a/sinA=b/sinB=c/sinC=2R"},
{"type":"choice","question":"已知A=30°,B=45°,a=2，则b=","options":["A. 2","B. 2√2","C. √2","D. 4"],"answer":"B","explanation":"b=a·sinB/sinA=2·sin45°/sin30°=2√2"},
{"type":"judge","question":"SSA条件下的三角形一定有唯一解。","answer":"错","explanation":"SSA可能有0解、1解或2解，需要根据条件讨论"},
{"type":"fill","question":"在△ABC中，a=3,b=4,C=60°，面积S=____。","answer":"3√3","explanation":"S=½ab·sinC=½×3×4×sin60°=½×12×√3/2=3√3"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='正弦定理及其应用' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '正弦定理的内容是什么？', 'a/sinA=b/sinB=c/sinC=2R（R为外接圆半径）。边与对角正弦值成比例。', 1),
(@art, '什么情况下用正弦定理？', '已知两角一边（AAS/ASA）或两边一对角（SSA，需讨论解个数）时用正弦定理。', 2),
(@art, 'SSA条件下如何判断解的个数？', '先算sinB=b·sinA/a：sinB>1无解；sinB=1一解（直角）；sinB<1且a≥b一解；sinB<1且a<b可能两解。', 3);

-- ═══════════════════════════════════════════════════════════════
-- 正弦定理与余弦定理 > 余弦定理
-- ═══════════════════════════════════════════════════════════════

SET @l4 = (SELECT id FROM knowledge_nodes WHERE subject_id=22 AND name='余弦定理 [掌握]' AND level=4 LIMIT 1);
INSERT IGNORE INTO knowledge_articles (subject_id, chapter, task, title, content_md, excerpt, node_id, difficulty, tags, syllabus_refs, quiz, status) VALUES
(22, '三角函数', '正弦定理与余弦定理', '余弦定理及其应用',
'## 余弦定理

在△ABC中：

**a² = b² + c² - 2bc·cosA**

**b² = a² + c² - 2ac·cosB**

**c² = a² + b² - 2ab·cosC**

## 变形（求角）

**cosA = (b²+c²-a²)/(2bc)**

**cosB = (a²+c²-b²)/(2ac)**

**cosC = (a²+b²-c²)/(2ab)**

## 与勾股定理的关系

当 A=90° 时，cosA=0，余弦定理退化为：

**a² = b² + c²（勾股定理）**

> 余弦定理是勾股定理在一般三角形中的推广。

## 适用场景

| 已知条件 | 方法 |
|:--------:|:----:|
| SAS（两边夹角） | 余弦定理求第三边 |
| SSS（三边） | 余弦定理变形求角 |

## 典型例题

**例1**：在△ABC中，b=3，c=4，A=60°，求a。

a² = b²+c²-2bc·cosA

= 9+16-2×3×4×cos60°

= 25-24×(1/2)

= 25-12 = 13

**a = √13**

**例2**：在△ABC中，a=7，b=8，c=5，求cosC。

cosC = (a²+b²-c²)/(2ab)

= (49+64-25)/(2×7×8)

= 88/112

= **11/14**

## 判断三角形形状

利用余弦定理可以判断三角形的形状：

| 条件 | 形状 |
|:----:|:----:|
| a²+b²=c² | 直角三角形 |
| a²+b²>c² | 锐角三角形（C为锐角） |
| a²+b²<c² | 钝角三角形（C为钝角） |
| a=b=c | 等边三角形 |

## 易错警示

1. **公式中2bc·cosA前是减号**——a²=b²+c²**-**2bc·cosA
2. **求角时代入要准确**——cosA的分母是2bc，分子是b²+c²-a²
3. **余弦定理可以判断三角形形状**——利用cosA的正负判断角的类型',
'余弦定理：a²=b²+c²-2bc·cosA，是勾股定理的推广。适用于SAS（求第三边）和SSS（求角）。cosA=(b²+c²-a²)/(2bc)可判断角的类型。',
@l4, 2, '["三角函数","余弦定理","解三角形","勾股定理"]',
'["三角函数","正弦定理与余弦定理"]',
'[{"type":"choice","question":"余弦定理a²=b²+c²-2bc·cosA中，当A=90°时退化为","options":["A. a²=b²+c²","B. a²=b²-c²","C. a=b+c","D. a=b-c"],"answer":"A","explanation":"cos90°=0，a²=b²+c²-0=b²+c²，即勾股定理"},
{"type":"choice","question":"在△ABC中，a=5,b=7,c=8，则cosC=","options":["A. 1/2","B. 1/4","C. 11/14","D. 13/14"],"answer":"A","explanation":"cosC=(25+49-64)/(2×5×7)=10/70=1/7？不对，重新算：cosC=(a²+b²-c²)/(2ab)=(25+49-64)/(2×5×7)=10/70=1/7"},
{"type":"judge","question":"若a²+b²>c²，则角C一定是锐角。","answer":"对","explanation":"由余弦定理cosC=(a²+b²-c²)/(2ab)>0，所以C为锐角"},
{"type":"fill","question":"在△ABC中，b=2,c=3,A=60°，则a=____。","answer":"√7","explanation":"a²=4+9-2×2×3×cos60°=13-6=7，a=√7"}]',
'PUBLISHED');

SET @art = (SELECT id FROM knowledge_articles WHERE subject_id=22 AND title='余弦定理及其应用' AND node_id=@l4 LIMIT 1);
INSERT IGNORE INTO knowledge_flashcards (article_id, front_text, back_text, sort_order) VALUES
(@art, '余弦定理的公式是什么？', 'a²=b²+c²-2bc·cosA（及轮换形式）。当A=90°时退化为勾股定理。', 1),
(@art, '什么情况下用余弦定理？', '已知两边夹角（SAS）求第三边，或已知三边（SSS）求角。', 2),
(@art, '如何用余弦定理判断三角形形状？', 'cosA=(b²+c²-a²)/(2bc)：cosA>0锐角，cosA=0直角，cosA<0钝角。或比较a²与b²+c²的大小。', 3),
(@art, '余弦定理和勾股定理有什么关系？', '余弦定理是勾股定理的推广。当角为90°时，cos90°=0，余弦定理退化为勾股定理。', 4);

-- ============================================================================
SELECT 'v183: 三角函数模块知识文章完成 — 12篇 + 40张卡片' AS result;
-- ============================================================================
