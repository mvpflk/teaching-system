-- ============================================================================
-- v175: 网络应用基础 — 知识库文章种子（单元6 设计制作网页）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖5个任务13个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元6：设计制作网页
-- ═══════════════════════════════════════════════════════════════
-- 任务1 创建网站（节点1133）
-- 任务2 设计简单网页（节点1134~1138）
-- 任务3 建立列表和超链接（节点1139~1140）
-- 任务4 运用CSS（节点1141~1142）
-- 任务5 使用表单（节点1143~1145）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10159: HTML基本结构（node=1133）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10159, 'HTML基本结构——网页的骨架',
'## 什么是HTML？

HTML（HyperText Markup Language，超文本标记语言）是编写网页的**标准语言**。它使用**标签**来描述网页的结构和内容。

### 一句话理解
> HTML就像盖房子的"框架"——标签是房梁/柱子/墙壁，浏览器把这些框架渲染成你看到的网页。

## HTML基本结构

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <title>网页标题</title>
  </head>
  <body>
    网页可见内容
  </body>
</html>
```

### 各部分说明

| 标签 | 含义 | 说明 |
|:----:|:----:|:------|
| `<!DOCTYPE html>` | 文档类型声明 | 告诉浏览器使用HTML5标准解析 |
| `<html>` | HTML根标签 | 整个HTML文档的根，所有内容都在里面 |
| `<head>` | 头部区域 | 存放元数据、标题、样式、脚本等**不可见**内容 |
| `<meta charset="utf-8">` | 字符编码声明 | 告诉浏览器用UTF-8编码，支持中文显示 |
| `<title>` | 网页标题 | 显示在浏览器标签栏上 |
| `<body>` | 主体区域 | 网页**可见**内容都在这里 |

### 结构记忆
> `<html>` = 房子整体 → `<head>` = 房产证信息（看不到） → `<body>` = 房子里能看到的一切

## HTML标签基本语法

```html
<标签名 属性名="属性值">内容</标签名>
```

| 部分 | 含义 | 示例 |
|:----:|:------|:------|
| **开始标签** | 标签名称+属性 | `<a href="https://www.baidu.com">` |
| **内容** | 标签包围的文本/子标签 | `百度一下` |
| **结束标签** | 带斜杠的标签名 | `</a>` |

### 空标签
> 有些标签没有内容，称为**空标签**，不需要结束标签：
> `<br>`（换行）、`<hr>`（水平线）、`<img>`（图片）、`<input>`（输入框）

> **对口升学必考**：HTML基本结构中`<html>`是根标签，`<head>`放元数据，`<body>`放可见内容。`<title>`定义浏览器标签栏标题。',
'HTML是网页的标准标记语言。基本结构：`<html>`（根）→`<head>`（元数据/标题）→`<body>`（可见内容）。`<title>`定义标签栏标题。`<!DOCTYPE html>`声明HTML5标准。',
5, '单元6 设计制作网页', '任务1 创建网站', 1133,
'HTML结构记法："html是房子整体，head是房产证（看不见），body是屋里摆设（看得见）"\n\n空标签记法："br/hr/img/input=单身汉，没内容就自己"',
'【必考】①HTML基本结构：html/head/title/body ②title在标签栏显示 ③body放可见内容 ④空标签（br/hr/img/input）⑤`<!DOCTYPE html>`是HTML5声明',
1,
'["HTML","超文本标记语言","网页结构","html","head","body","title"]',
'["HTML网页制作基础","HTML基本结构"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10287, 10159, 'HTML的基本结构是什么？', '<!DOCTYPE html><html><head><title>标题</title></head><body>可见内容</body></html>。html=根、head=元数据、body=可见内容。', 1, 'DEFINITION'),
(10288, 10159, 'HTML中<head>和<body>各有什么作用？', '<head>存放元数据、标题、样式、脚本等不可见内容。<body>存放网页的可见内容（文字/图片/表格等）。简单说：head=房产证，body=屋里摆设。', 2, 'DEFINITION'),
(10289, 10159, '什么HTML标签属于空标签？', '空标签没有内容、不需要结束标签。常见：<br>（换行）、<hr>（水平线）、<img>（图片）、<input>（输入框）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10160: 标题与段落标签（node=1134）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10160, '标题标签 h1~h6 与段落标签 p',
'## 标题标签 h1~h6

HTML提供了6级标题，从`<h1>`到`<h6>`，重要性逐级递减。

```html
<h1>一级标题（最重要）</h1>
<h2>二级标题</h2>
<h3>三级标题</h3>
<h4>四级标题</h4>
<h5>五级标题</h5>
<h6>六级标题（最不重要）</h6>
```

### 特点

| 标签 | 字体大小 | 语义重要性 | 使用建议 |
|:----:|:--------:|:---------:|:---------|
| `<h1>` | 最大 | 最高（文章主题） | 每页只用1个 |
| `<h2>` | 较大 | 高（章节标题） | 可以有多个 |
| `<h3>`~`<h6>` | 逐渐变小 | 逐级降低 | 用于子章节 |

> **重要**：`<h1>`在SEO（搜索引擎优化）中权重最高，每页只应有一个`<h1>`。

## 段落标签 p

`<p>`标签定义一个段落，浏览器会自动在段落前后添加间距。

```html
<p>这是第一个段落。段落标签会自动在前后加空行。</p>
<p>这是第二个段落。浏览器会自动换行显示。</p>
```

### 换行 vs 段落
| 标签 | 效果 | 间距 |
|:----:|:------|:----:|
| `<p>` | 段落 | 上下有大间距 |
| `<br>` | 换行 | 无额外间距，直接换到下一行 |

> **对口升学考点**：h1最大最重要、h6最小最不重要。p标签定义段落，浏览器自动添加间距。h1每页只用一个。',
'HTML标题：h1（最大/最重要/每页一个）~h6（最小）。p标签定义段落，浏览器自动添加上下间距。注意p（段落有大间距）和br（换行无间距）的区别。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1134,
'h1~h6记法："h1最大最重要用一次，h2次之随便用，越往后越小越不重要"\n\np vs br："p=段落（上方空一行），br=换行（直接下去）"',
'【必考】①h1最大最重要、h6最小 ②h1每页只用1个 ③p定义段落 ④p和br的区别（p有间距、br无间距）',
1,
'["HTML","标题标签","h1","h2","段落标签","p","SEO"]',
'["HTML网页制作基础","标题与段落"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10290, 10160, 'HTML的h1~h6标签有什么特点？', '共6级标题，h1最大最重要（每页只用1个），h6最小最不重要。搜索引擎对h1权重最高。按h1→h2→h3→h4→h5→h6逐级使用。', 1, 'DEFINITION'),
(10291, 10160, '<p>标签的作用是什么？', '<p>定义段落，浏览器自动在段落前后添加间距。一个<p>标签包一段文字，多个<p>标签的文字会分段显示。', 2, 'DEFINITION'),
(10292, 10160, '<p>和<br>有什么区别？', '<p>（段落）：有上下间距，适合段落分隔。<br>（换行）：直接换到下一行，无额外间距。p=新段落空一行，br=直接换行。', 3, 'COMPARISON');

-- ============================================================
-- 文章10161: 换行与水平线（node=1135）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10161, '换行标签 br 与水平线 hr',
'## 换行标签 br

`<br>`标签在网页中**强制换行**，是空标签（不需结束标签）。

```html
<p>
  第一行文字<br>
  第二行文字（换行后）<br>
  第三行文字（再换行）
</p>
```

### 显示效果
```
第一行文字
第二行文字（换行后）
第三行文字（再换行）
```

> 使用`<br>`换行后没有额外间距，直接到下一行。

## 水平线标签 hr

`<hr>`标签在网页中显示一条**水平分割线**，也是空标签。

```html
<h2>章节一</h2>
<p>第一章的内容...</p>
<hr>
<h2>章节二</h2>
<p>第二章的内容...</p>
```

### hr标签的属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `width` | 水平线宽度（像素或百分比） | `<hr width="50%">` |
| `size` | 水平线粗细（像素） | `<hr size="5">` |
| `color` | 水平线颜色 | `<hr color="red">` |
| `align` | 对齐方式（left/center/right） | `<hr align="center">` |

### 显示效果
```
─── 章节一 ───
第一章的内容...
────────────────  ← <hr> 水平分割线
─── 章节二 ───
第二章的内容...
```

> **对口升学考点**：br=换行（空标签）、hr=水平线（空标签）。hr常用属性：width、size、color、align。',
'<br>强制换行（空标签，无间距）。<hr>显示水平分割线（空标签），常用属性：width（宽度）、size（粗细）、color（颜色）、align（对齐）。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1135,
'br="break换行"，hr="horizontal水平线"\n\n"br是回车键，hr是分割线"\n\nhr属性记法："宽(度)粗(细)颜色对齐——width/size/color/align"',
'【必考】①<br>强制换行（空标签）②<hr>水平线（空标签）③hr属性：width/size/color/align',
1,
'["HTML","br","hr","换行","水平线","空标签"]',
'["HTML网页制作基础","换行与水平线"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10293, 10161, '<br>标签的作用是什么？', '<br>强制换行，将文本换到下一行显示。空标签，不需要结束标签。使用后无额外间距，直接到下一行。', 1, 'DEFINITION'),
(10294, 10161, '<hr>标签的作用和常用属性？', '<hr>显示水平分割线，空标签。常用属性：width（宽度，像素或百分比）、size（粗细）、color（颜色）、align（对齐left/center/right）。', 2, 'DEFINITION'),
(10295, 10161, '<br>和<hr>有什么共同特点和区别？', '共同：都是空标签（无需结束标签）。区别：<br>换行（内容换到下一行），<hr>添加水平分割线（页面分隔）。', 3, 'COMPARISON');

-- ============================================================
-- 文章10162: 文本格式标签（node=1136）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10162, '文本格式标签——让文字变样',
'## HTML文本格式标签

HTML提供了多种标签来改变文字的样式和语义。

### 常用文本格式标签

```html
<b>加粗文字</b>
<i>斜体文字</i>
<u>下划线文字</u>
<s>删除线文字</s>
<font color="red" size="5">红色大号文字</font>
```

### 各标签效果

| 标签 | 效果 | 语义（含义） |
|:----:|:----:|:-------------|
| `<b>` | **加粗** | 仅仅是视觉加粗，无语义 |
| `<strong>` | **加粗** | 表示重要内容（语义加强） |
| `<i>` | *斜体* | 仅仅是斜体，无语义 |
| `<em>` | *斜体* | 表示强调（语义加强） |
| `<u>` | 下划线 | 添加下划线 |
| `<s>` | ~~删除线~~ | 表示已删除的内容 |
| `<font>` | 设置字体 | 通过color/size/face设置颜色/大小/字体 |

### `<font>`标签属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `color` | 文字颜色（英文名/十六进制） | `color="red"`、`color="#FF0000"` |
| `size` | 文字大小（1~7） | `size="5"`（默认3） |
| `face` | 字体名称 | `face="宋体"`、`face="Arial"` |

### 示例代码
```html
<p>
  <b>加粗</b> · <i>斜体</i> · <u>下划线</u> · <s>删除线</s><br>
  <font color="blue" size="4">蓝色4号字</font><br>
  <font color="#FF0000" size="6" face="黑体">红色6号黑体</font>
</p>
```

> **对口升学考点**：b=加粗、i=斜体、u=下划线、s=删除线。font标签的color/size/face属性。',
'文本格式标签：b（加粗）、i（斜体）、u（下划线）、s（删除线）。font标签设置文字样式：color（颜色）、size（大小1~7）、face（字体）。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1136,
'b/i/u/s记法："b=bold加粗，i=italic斜体，u=underline下划线，s=strikethrough删除线"\n\nfont属性："color颜色/size大小/face字体"',
'【必考】①b=加粗、i=斜体、u=下划线、s=删除线 ②font标签的color/size/face三个属性 ③size取值1~7（默认3）',
1,
'["HTML","文本格式","b","i","u","s","font","加粗","斜体","下划线","删除线"]',
'["HTML网页制作基础","文本格式"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10296, 10162, 'HTML中b/i/u/s标签分别有什么作用？', 'b=加粗（bold）、i=斜体（italic）、u=下划线（underline）、s=删除线（strikethrough）。用于改变文字的外观样式。', 1, 'DEFINITION'),
(10297, 10162, '<font>标签有哪些属性？各有什么作用？', 'color：文字颜色（red/#FF0000）。size：文字大小（1~7，默认3）。face：字体名称（宋体/Arial）。已不推荐使用，建议用CSS代替。', 2, 'DEFINITION'),
(10298, 10162, '<b>和<strong>有什么区别？', '<b>仅仅是视觉加粗，无语义含义。<strong>也表示加粗，但有语义强调的含义，搜索引擎和屏幕阅读器更重视<strong>。', 3, 'COMPARISON');

-- ============================================================
-- 文章10163: 图片标签 img（node=1137）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10163, '图片标签 img——在网页中插入图片',
'## img标签

`<img>`标签在网页中**插入图片**，是空标签（不需要结束标签）。

```html
<img src="图片地址" alt="替代文本">
```

### 必选属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`src`** | 图片文件的**路径/URL**（必填） | `src="logo.jpg"`、`src="https://xxx.com/1.png"` |
| **`alt`** | 图片无法显示时的**替代文本**（必填） | `alt="公司Logo"` |

### 可选属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `width` | 图片宽度（像素或百分比） | `width="200"`、`width="50%"` |
| `height` | 图片高度（像素或百分比） | `height="150"` |
| `title` | 鼠标悬停时显示的提示文字 | `title="点击查看大图"` |
| `border` | 图片边框宽度（像素） | `border="1"` |

### 完整示例
```html
<img src="campus.jpg" alt="校园风光"
     width="400" height="300" title="点击放大">
```

### src的两种路径

| 路径类型 | 说明 | 示例 |
|:--------:|:------|:------|
| **相对路径** | 相对于当前HTML文件的路径 | `src="images/photo.jpg"`、`src="../logo.png"` |
| **绝对路径** | 完整的网络URL | `src="https://www.example.com/images/1.jpg"` |

### 图片显示优化
> 建议设置`width`和`height`，这样图片加载前浏览器就知道占多大空间，防止页面布局跳动。

> **对口升学考点**：img标签的两个必选属性——src（图片路径）和alt（替代文本）。空标签。',
'<img>标签在网页中插入图片。必选属性：src（图片路径）、alt（替代文本）。可选：width（宽度）、height（高度）、title（提示文字）、border（边框）。空标签。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1137,
'img标签记法："img=image图片"\n\n必选属性2个："src告诉浏览器去哪找图片，alt告诉浏览器图片坏了显示什么"',
'【必考】①img是空标签 ②src=图片路径（必选）③alt=替代文本（必选）④相对路径vs绝对路径 ⑤width/height设置宽高',
1,
'["HTML","img","图片","src","alt","相对路径","绝对路径"]',
'["HTML网页制作基础","图片标签"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10299, 10163, 'HTML中如何插入图片？', '使用<img>标签。必选属性：src="图片路径"、alt="替代文本"。可选：width（宽）、height（高）、title（提示）、border（边框）。空标签。', 1, 'DEFINITION'),
(10300, 10163, '<img>的src属性使用相对路径和绝对路径有什么区别？', '相对路径：相对于当前HTML文件的位置，如images/logo.jpg。绝对路径：完整的URL，如https://xxx.com/logo.jpg。', 2, 'DEFINITION'),
(10301, 10163, '为什么<img>标签最好设置width和height？', '设置宽高后，浏览器在图片加载前就知道占多大空间，防止图片加载时页面布局突然跳动（布局偏移），提升用户体验。', 3, 'APPLICATION');

-- ============================================================
-- 文章10164: 表格标签 table（node=1138）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10164, '表格标签——用表格展示数据',
'## HTML表格结构

HTML表格使用`<table>`标签创建，由行（`<tr>`）和单元格（`<td>`/`<th>`）组成。

```html
<table border="1" width="80%" align="center">
  <tr>
    <th>姓名</th>
    <th>科目</th>
    <th>成绩</th>
  </tr>
  <tr>
    <td>张三</td>
    <td>计算机网络</td>
    <td>95</td>
  </tr>
  <tr>
    <td>李四</td>
    <td>计算机网络</td>
    <td>88</td>
  </tr>
</table>
```

### 表格相关标签

| 标签 | 含义 | 说明 |
|:----:|:----:|:------|
| `<table>` | 表格容器 | 整个表格的根标签 |
| `<tr>` | 行（Table Row） | 一行中可以放多个单元格 |
| `<th>` | 表头单元格（Table Header） | 自动加粗居中，用于列标题 |
| `<td>` | 普通单元格（Table Data） | 默认左对齐，存放数据 |

### table常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `border` | 表格边框宽度（像素） | `border="1"` |
| `width` | 表格宽度（像素或百分比） | `width="80%"`、`width="500"` |
| `align` | 表格对齐方式 | `align="center"` |
| `bgcolor` | 表格背景颜色 | `bgcolor="#f0f0f0"` |
| `cellpadding` | 单元格内边距（内容到边框的距离） | `cellpadding="5"` |
| `cellspacing` | 单元格间距（单元格之间的距离） | `cellspacing="0"` |

### tr/td常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `align` | 行/单元格内水平对齐 | `align="center"` |
| `valign` | 垂直对齐（top/middle/bottom） | `valign="middle"` |
| `colspan` | 跨列合并（水平合并单元格） | `colspan="2"` |
| `rowspan` | 跨行合并（垂直合并单元格） | `rowspan="3"` |

### 合并单元格示例
```html
<table border="1">
  <tr>
    <td colspan="2">合并两列</td>
    <td>普通单元格</td>
  </tr>
  <tr>
    <td rowspan="2">合并两行</td>
    <td>数据A</td>
    <td>数据B</td>
  </tr>
</table>
```

> **对口升学考点**：table=表格、tr=行、th=表头（加粗居中）、td=普通单元格。border设置边框，colspan/rowspan合并单元格。',
'HTML表格：<table>（表格）、<tr>（行）、<th>（表头加粗居中）、<td>（普通单元格）。属性：border边框、width宽度、colspan跨列合并、rowspan跨行合并。',
5, '单元6 设计制作网页', '任务2 设计简单网页', 1138,
'table结构记法："table=桌子，tr=一排（行），th和td=桌上的格子"\n\nth=表头（加粗居中），td=数据（左对齐）\ncolspan=横向合并占几列，rowspan=纵向合并占几行',
'【必考】①table/tr/th/td四个标签的关系 ②border设置边框 ③th自动加粗居中 ④colspan（跨列）和rowspan（跨行）合并单元格',
2,
'["HTML","表格","table","tr","th","td","colspan","rowspan","合并单元格"]',
'["HTML网页制作基础","表格"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10302, 10164, 'HTML表格的基本结构是什么？', '<table><tr><th>表头</th><th>表头</th></tr><tr><td>数据</td><td>数据</td></tr></table>。table=表格、tr=行、th=表头、td=单元格。', 1, 'DEFINITION'),
(10303, 10164, 'colspan和rowspan各有什么作用？', 'colspan（跨列合并）：一个单元格占据多列宽度。rowspan（跨行合并）：一个单元格占据多行高度。用于制作复杂表头。', 2, 'DEFINITION'),
(10304, 10164, '<th>和<td>有什么不同？', '<th>（表头单元格）默认加粗居中，用于列标题。<td>（数据单元格）默认左对齐，存放实际数据。', 3, 'COMPARISON');

-- ============================================================
-- 文章10165: 有序列表与无序列表（node=1139）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10165, '列表标签——有序与无序',
'## 无序列表 ul/li

无序列表使用**圆点**作为项目符号，适合表示不分先后的项目。

```html
<h3>课程列表（无序）</h3>
<ul>
  <li>计算机网络基础</li>
  <li>网页设计与制作</li>
  <li>数据库应用</li>
  <li>编程语言基础</li>
</ul>
```

### 显示效果
```
• 计算机网络基础
• 网页设计与制作
• 数据库应用
• 编程语言基础
```

`<ul>`的`type`属性可改变项目符号样式：
| type值 | 效果 |
|:------:|:------:|
| `disc` | ● 实心圆（默认） |
| `circle` | ○ 空心圆 |
| `square` | ■ 实心方块 |

## 有序列表 ol/li

有序列表使用**数字编号**，适合表示有先后顺序的项目。

```html
<h3>开机步骤（有序）</h3>
<ol>
  <li>按下电源按钮</li>
  <li>等待系统启动</li>
  <li>输入用户名和密码</li>
  <li>进入桌面</li>
</ol>
```

### 显示效果
```
1. 按下电源按钮
2. 等待系统启动
3. 输入用户名和密码
4. 进入桌面
```

`<ol>`的`type`属性可改变编号类型：
| type值 | 效果 | 示例 |
|:------:|:------|:----:|
| `1` | 阿拉伯数字（默认） | 1. 2. 3. |
| `A` | 大写字母 | A. B. C. |
| `a` | 小写字母 | a. b. c. |
| `I` | 大写罗马数字 | I. II. III. |
| `i` | 小写罗马数字 | i. ii. iii. |

> **对口升学考点**：ul=无序列表（圆点符号）、ol=有序列表（数字编号）、li=列表项。type属性改变符号/编号样式。',
'ul（无序列表）=圆点符号、ol（有序列表）=数字编号。li定义列表项。ol的type属性可改为字母（A/a）或罗马数字（I/i）。ul的type可改为disc/circle/square。',
5, '单元6 设计制作网页', '任务3 建立列表和超链接', 1139,
'ul vs ol："ul=圆点不分先后，ol=数字按顺序"\n\nol的type："1数字/A大写/a小写/I罗马大写/i罗马小写"',
'【必考】①ul=无序（圆点）②ol=有序（数字）③li=列表项 ④ol的type属性改变编号类型（1/A/a/I/i）',
1,
'["HTML","列表","ul","ol","li","无序列表","有序列表"]',
'["HTML网页制作基础","列表"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10305, 10165, '无序列表ul和有序列表ol有什么区别？', 'ul无序列表：使用圆点●作为项目符号，项目不分先后。ol有序列表：使用数字1/2/3编号，项目有先后顺序。两者都用<li>定义列表项。', 1, 'COMPARISON'),
(10306, 10165, '有序列表<ol>的type属性有哪些取值？', 'type="1"（默认数字）、type="A"（大写字母）、type="a"（小写字母）、type="I"（大写罗马）、type="i"（小写罗马）。', 2, 'DEFINITION'),
(10307, 10165, '无序列表<ul>的type属性有哪些取值？', 'type="disc"（实心圆点●默认）、type="circle"（空心圆○）、type="square"（实心方块■）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10166: 超链接标签 a（node=1140）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10166, '超链接 a——网页间的桥梁',
'## 超链接标签 a

`<a>`标签定义超链接，用于从一个页面跳转到另一个页面或其他资源。

```html
<a href="目标地址" target="打开方式">链接文本</a>
```

### a标签的核心属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`href`** | 链接目标的URL（**必填**） | `href="https://www.baidu.com"` |
| **`target`** | 链接的打开方式 | `target="_blank"`（新窗口打开） |
| `title` | 鼠标悬停时的提示文字 | `title="点击访问百度"` |

### target取值

| 值 | 说明 |
|:----:|:------|
| `_self` | 在当前窗口打开（默认） |
| `_blank` | 在**新窗口/新标签页**打开（最常用） |

## 四种超链接类型

### 1. 外部链接——链接到其他网站
```html
<a href="https://www.baidu.com" target="_blank">访问百度</a>
```

### 2. 内部链接——链接到本站其他页面
```html
<a href="about.html">关于我们</a>
<a href="news/2024.html">2024年新闻</a>
```

### 3. 锚点链接——页面内跳转
```html
<!-- 先定义锚点 -->
<h2 id="section1">第一章</h2>

<!-- 链接到锚点 -->
<a href="#section1">跳转到第一章</a>

<!-- 从其他页面跳转到锚点 -->
<a href="help.html#faq">查看常见问题</a>
```

### 4. 邮件链接——点击发送邮件
```html
<a href="mailto:admin@school.edu.cn">发送邮件给管理员</a>
```
点击后自动打开默认邮件客户端，收件人地址已填好。

### 链接的样式
> 浏览器默认样式：未访问=蓝色下划线，已访问=紫色下划线，悬停=变手形指针。

> **对口升学必考**：a标签的href（链接地址）和target（_blank新窗口）属性。四种链接类型：外部/内部/锚点/邮件。',
'<a>标签定义超链接。href（必填）指定链接地址，target="_blank"新窗口打开。四种类型：外部链接（其他网站）、内部链接（本站页面）、锚点链接（#id页面内跳转）、邮件链接（mailto:）。',
5, '单元6 设计制作网页', '任务3 建立列表和超链接', 1140,
'a标签记法："a=anchor锚"\n\n四种链接："外部连别人，内部连自己，锚点连本页，#号，邮件连邮箱mailto:"',
'【必考】①<a href="...">链接文本</a> ②target="_blank"新窗口打开 ③四种链接类型：外部/内部/锚点/邮件 ④锚点链接用#id',
2,
'["HTML","超链接","a标签","href","target","_blank","锚点链接","邮件链接"]',
'["HTML网页制作基础","超链接"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10308, 10166, '<a>标签中href和target属性的作用？', 'href（必填）：指定链接目标地址。target：指定打开方式，_blank在新窗口打开，_self（默认）在当前窗口打开。', 1, 'DEFINITION'),
(10309, 10166, '超链接有哪四种类型？各怎么实现？', '①外部：href="https://..." ②内部：href="page.html" ③锚点：href="#id" ④邮件：href="mailto:邮箱地址"。', 2, 'DEFINITION'),
(10310, 10166, 'target="_blank"和默认的_self有什么区别？', 'target="_blank"：在新窗口或新标签页打开链接（原页面保留）。默认情况：在当前窗口打开，原页面被替换。外部链接推荐使用_blank。', 3, 'COMPARISON');

-- ============================================================
-- 文章10167: CSS引入方式（node=1141）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10167, 'CSS三种引入方式——给网页穿衣服',
'## 什么是CSS？

CSS（Cascading Style Sheets，层叠样式表）用于控制网页的**外观样式**——颜色、字体、布局、背景等。

### 一句话理解
> HTML是房子的"框架结构"，CSS是房子的"装修设计"——把灰白的HTML变得漂亮。

## CSS的三种引入方式

### 1. 行内样式（Inline Style）

在HTML标签的`style`属性中直接写CSS。

```html
<p style="color: red; font-size: 20px;">红色20像素的文字</p>
```

| 优点 | 缺点 |
|:----:|:------|
| 最直接，针对单个元素 | 不利于复用，代码冗余 |
| 优先级最高 | 维护困难 |

### 2. 内部样式表（Internal Style Sheet）

在`<head>`中使用`<style>`标签定义样式，当前页面可复用。

```html
<head>
  <style>
    p {
      color: blue;
      font-size: 16px;
    }
    h1 {
      color: #333;
      text-align: center;
    }
  </style>
</head>
```

| 优点 | 缺点 |
|:----:|:------|
| 本页面可复用，不用每个标签都写 | 其他页面无法使用 |
| 结构和样式部分分离 | 多页面时重复 |

### 3. 外部样式表（External Style Sheet）

将CSS写在一个单独的`.css`文件中，在HTML中用`<link>`引入。

**style.css文件：**
```css
body {
  background-color: #f0f0f0;
  font-family: "宋体", Arial, sans-serif;
}
p {
  color: #666;
  line-height: 1.8;
}
```

**HTML文件中引入：**
```html
<head>
  <link rel="stylesheet" type="text/css" href="style.css">
</head>
```

| 优点 | 缺点 |
|:----:|:------|
| 多个页面共享同一样式 | 需要额外加载CSS文件 |
| 结构和样式完全分离 | 第一次加载稍慢 |
| 维护最方便（改一个文件全站变） | — |

## 三种方式对比

| 引入方式 | 复用性 | 维护性 | 优先级 |
|:--------:|:------:|:------:|:------:|
| **行内样式** | 最低 | 最低 | **最高** |
| **内部样式表** | 本页面 | 中等 | 中 |
| **外部样式表** | 全站 | 最高 | 最低 |

### 优先级原则
> **行内 > 内部 > 外部**（就近原则——越靠近标签的样式优先级越高）。

> **对口升学必考**：三种CSS引入方式——行内（style属性）、内部（<style>标签）、外部（<link>引入.css文件）。各自的优缺点和优先级。',
'CSS三种引入方式：行内（标签style属性）、内部（<head>中<style>）、外部（<link>引入.css文件）。行内优先级最高、复用最差。外部优先级最低、复用最好。',
5, '单元6 设计制作网页', '任务4 运用CSS', 1141,
'CSS三种方式："行内=直接写在标签里（最优先）、内部=写在head的style里（本页面用）、外部=独立.css文件（全站用）"\n\n优先级："行内>内部>外部——越靠近标签优先级越高"',
'【必考】①三种CSS引入方式 ②行内（style属性）/内部（style标签）/外部（link引入）③优先级：行内>内部>外部 ④各自优缺点',
2,
'["CSS","层叠样式表","行内样式","内部样式表","外部样式表","link","style"]',
'["HTML网页制作基础","CSS"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10311, 10167, 'CSS有哪三种引入方式？', '①行内样式：标签style属性（优先级最高）②内部样式表：<head>中<style>标签（本页面用）③外部样式表：<link>引入.css文件（全站通用）。', 1, 'DEFINITION'),
(10312, 10167, '三种CSS引入方式的优先级顺序是什么？', '行内样式（最高）> 内部样式表 > 外部样式表（最低）。原则：越靠近HTML标签的样式优先级越高——就近原则。', 2, 'COMPARISON'),
(10313, 10167, '外部样式表有什么优缺点？', '优点：多个页面可共享同一CSS文件、修改一个文件全站更新、结构与样式完全分离。缺点：需额外加载CSS文件、第一次加载稍慢。', 3, 'DEFINITION');

-- ============================================================
-- 文章10168: 常用CSS属性（node=1142）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10168, '常用CSS属性——装修网页的工具箱',
'## CSS基本语法

```css
选择器 {
  属性名: 属性值;
  属性名: 属性值;
}
```

示例：
```css
p {
  color: red;
  font-size: 16px;
}
```

## 常用CSS属性

### 文字样式

| CSS属性 | 说明 | 示例 |
|:-------:|:------|:------|
| `color` | 文字颜色 | `color: red;`、`color: #FF0000;` |
| `font-size` | 文字大小 | `font-size: 16px;`、`font-size: 1.2em;` |
| `font-weight` | 文字粗细 | `font-weight: bold;`（加粗）、`normal` |
| `font-style` | 文字风格 | `font-style: italic;`（斜体） |
| `font-family` | 字体名称 | `font-family: "宋体", Arial;` |
| `text-align` | 水平对齐 | `text-align: center;`（居中） |
| `text-decoration` | 文字装饰 | `text-decoration: underline;`（下划线） |
| `line-height` | 行高 | `line-height: 1.8;` |

### 背景样式

| CSS属性 | 说明 | 示例 |
|:-------:|:------|:------|
| `background-color` | 背景颜色 | `background-color: #f0f0f0;` |
| `background-image` | 背景图片 | `background-image: url("bg.jpg");` |

### 盒模型属性（间距和边框）

```css
div {
  margin: 20px;        /* 外边距——元素与其他元素之间的距离 */
  padding: 10px;       /* 内边距——元素内容与边框之间的距离 */
  border: 1px solid black;  /* 边框：粗细 样式 颜色 */
}
```

| CSS属性 | 说明 | 示例 |
|:-------:|:------|:------|
| **`margin`** | 外边距（元素外部间距） | `margin: 10px;`（四边）、`margin-top: 5px;` |
| **`padding`** | 内边距（元素内部间距） | `padding: 10px;`、`padding-left: 15px;` |
| **`border`** | 边框（简写属性） | `border: 1px solid #ccc;` |
| `width` | 元素宽度 | `width: 200px;`、`width: 50%;` |
| `height` | 元素高度 | `height: 100px;` |

### border详解
```css
/* 简写方式：粗细  样式  颜色 */
border: 2px solid red;     /* 实线边框 */
border: 2px dashed blue;   /* 虚线边框 */
border: 2px dotted green;  /* 点线边框 */

/* 分别设置各边 */
border-top: 1px solid #333;
border-bottom: 2px solid #ccc;
```

### 完整示例
```css
.card {
  width: 300px;
  margin: 20px auto;         /* 上下20px，左右居中 */
  padding: 15px;
  border: 1px solid #ddd;
  background-color: #fff;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
}
```

> **对口升学考点**：常用CSS属性——color（颜色）、font-size（大小）、background-color（背景色）、margin（外边距）、padding（内边距）、border（边框）。',
'CSS常用属性：color（颜色）、font-size（大小）、background-color（背景色）、margin（外边距）、padding（内边距）、border（边框粗细/样式/颜色）、text-align（对齐）、font-weight（粗细）。',
5, '单元6 设计制作网页', '任务4 运用CSS', 1142,
'常用CSS记法："color颜色/font-size字号/background-color背景色/margin外距/padding内距/border边框"\n\nborder简写："border: 粗细 样式 颜色"如"border: 1px solid red"',
'【必考】①color设置文字颜色 ②font-size设置文字大小 ③background-color设置背景色 ④margin=外边距、padding=内边距 ⑤border=边框（粗细/样式/颜色）',
2,
'["CSS","属性","color","font-size","background-color","margin","padding","border","盒模型"]',
'["HTML网页制作基础","CSS"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10314, 10168, 'CSS中控制文字样式的常用属性？', 'color（颜色）、font-size（大小）、font-weight（粗细bold/normal）、font-style（风格italic斜体）、text-align（对齐center/left/right）、text-decoration（装饰underline下划线）。', 1, 'DEFINITION'),
(10315, 10168, 'CSS中margin和padding的区别？', 'margin=外边距，元素边框到相邻元素的距离（拉开元素距离）。padding=内边距，元素内容到边框的距离（撑大元素尺寸）。', 2, 'COMPARISON'),
(10316, 10168, 'CSS中border属性的写法是什么？', 'border: 粗细 样式 颜色。如border: 1px solid red（1像素红色实线）。样式值：solid实线、dashed虚线、dotted点线。', 3, 'DEFINITION');

-- ============================================================
-- 文章10169: 表单与input类型（node=1143）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10169, '表单标签 form 与 input 输入框',
'## 表单标签 form

`<form>`标签用于创建HTML表单，收集用户输入的数据。

```html
<form action="提交地址" method="post">
  <!-- 表单控件放在这里 -->
  <input type="text" name="username">
  <input type="submit" value="提交">
</form>
```

### form常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `action` | 数据提交到的服务器地址 | `action="/login"` |
| `method` | 数据提交方式 | `method="get"`（URL可见）或`method="post"`（URL不可见） |

## input标签——最常用的表单控件

`<input>`是**空标签**，通过`type`属性变换成不同的输入控件。

### 常用input类型

```html
<!-- 文本输入框 -->
用户名：<input type="text" name="username">

<!-- 密码输入框（输入内容隐藏为●） -->
密码：<input type="password" name="pwd">

<!-- 单选框（同一name只能选一项） -->
性别：
<input type="radio" name="gender" value="male"> 男
<input type="radio" name="gender" value="female" checked> 女

<!-- 复选框（同一name可多选） -->
爱好：
<input type="checkbox" name="hobby" value="read"> 阅读
<input type="checkbox" name="hobby" value="sport"> 运动

<!-- 提交按钮 -->
<input type="submit" value="注册">

<!-- 重置按钮 -->
<input type="reset" value="重新填写">
```

### input类型汇总

| type值 | 作用 | 特点 |
|:------:|:------|:------|
| `text` | 单行文本输入 | 最常用的输入框 |
| `password` | 密码输入 | 输入内容显示为● |
| `radio` | 单选框 | 同一name只选一项 |
| `checkbox` | 复选框 | 同一name可选多项 |
| `submit` | 提交按钮 | 将表单数据发送到action地址 |
| `reset` | 重置按钮 | 将表单所有控件恢复为默认值 |

### input常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `type` | 输入框类型 | `type="text"` |
| `name` | 控件名称（提交时作为参数名） | `name="username"` |
| `value` | 控件的值（提交时的数据） | `value="张三"` |
| `checked` | 单选框/复选框默认选中 | `checked` |
| `placeholder` | 输入框提示文字 | `placeholder="请输入用户名"` |
| `readonly` | 只读（不可修改） | `readonly` |
| `disabled` | 禁用（不可操作且不提交） | `disabled` |

> **对口升学必考**：input的type属性——text（文本）、password（密码）、radio（单选）、checkbox（多选）、submit（提交）、reset（重置）。',
'<form>创建表单，action指定提交地址、method指定提交方式（get/post）。<input>的type：text文本、password密码（●）、radio单选、checkbox多选、submit提交、reset重置。',
5, '单元6 设计制作网页', '任务5 使用表单', 1143,
'input type记法："text文本/password密码/radio单选/checkbox多选/submit提交/reset重置"',
'【必考】①input常用type：text/password/radio/checkbox/submit/reset ②radio同一name只能选一个 ③checkbox同一name可多选 ④name是提交时的参数名',
2,
'["HTML","表单","form","input","text","password","radio","checkbox","submit","reset"]',
'["HTML网页制作基础","表单"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10317, 10169, '<input>标签的常用type值有哪些？', 'text（文本输入）、password（密码●）、radio（单选）、checkbox（多选）、submit（提交按钮）、reset（重置按钮）。', 1, 'DEFINITION'),
(10318, 10169, 'radio和checkbox有什么区别？', 'radio（单选框）：同一name只能选一项，用于性别/选择等互斥选项。checkbox（复选框）：同一name可选多项，用于爱好/特长等多选场景。', 2, 'COMPARISON'),
(10319, 10169, '<form>标签的action和method属性各有什么作用？', 'action：指定表单数据提交到的服务器地址。method：指定提交方式——get（数据附在URL后，可见）或post（数据在请求体中，不可见）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10170: textarea与select（node=1144）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10170, '多行文本框 textarea 与下拉列表 select',
'## 多行文本框 textarea

`<textarea>`标签用于输入**多行文本**，适合留言、评论、简介等场景。

```html
<textarea name="intro" rows="5" cols="40">
默认文本内容
</textarea>
```

### textarea常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| `name` | 控件名称 | `name="introduction"` |
| `rows` | 可见行数（高度） | `rows="5"`（显示5行） |
| `cols` | 可见列数（宽度） | `cols="40"`（每行40个字符宽） |
| `placeholder` | 提示文字 | `placeholder="请填写个人简介"` |
| `readonly` | 只读 | `readonly` |

### 与input text的区别
| 对比 | `<input type="text">` | `<textarea>` |
|:----:|:---------------------:|:------------:|
| 行数 | 单行 | **多行** |
| 大小 | 默认宽度小 | 可设置rows和cols |
| 回车 | 不能换行 | **可以换行** |
| 结束标签 | 空标签（无） | **有**`</textarea>` |

## 下拉列表 select/option

`<select>`标签创建下拉列表，`<option>`定义列表中的选项。

```html
<select name="city">
  <option value="beijing">北京</option>
  <option value="shanghai" selected>上海</option>
  <option value="chengdu">成都</option>
</select>
```

### select常用属性

| 属性 | 说明 |
|:----:|:------|
| `name` | 控件名称 |
| `size` | 可见选项数（大于1时变为列表框） |
| `multiple` | 允许按住Ctrl多选 |

### option常用属性

| 属性 | 说明 |
|:----:|:------|
| `value` | 提交时的值（不写则提交标签内文本） |
| `selected` | 默认选中项 |

### 分组显示
```html
<select name="course">
  <optgroup label="计算机类">
    <option value="network">计算机网络</option>
    <option value="web">网页设计</option>
  </optgroup>
  <optgroup label="电子类">
    <option value="circuit">电路基础</option>
  </optgroup>
</select>
```

> **对口升学考点**：textarea=多行文本（有结束标签、可换行）。select=下拉列表、option=选项、selected=默认选中。',
'<textarea>多行文本框（name/rows/cols），与input text的区别：textarea可换行。<select>下拉列表+<option>选项，selected属性默认选中，value属性为提交值。',
5, '单元6 设计制作网页', '任务5 使用表单', 1144,
'textarea vs input text："textarea=多行文本可换行，input text=单行不能换行"\n\nselect记法："select是下拉框，option是里面的选项，selected是默认选中"',
'【必考】①textarea多行文本（rows行数/cols列数）②select下拉列表+option选项 ③selected默认选中 ④value属性是提交值',
1,
'["HTML","textarea","多行文本","select","下拉列表","option","selected","表单"]',
'["HTML网页制作基础","表单"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10320, 10170, '<textarea>标签的作用和属性？', '多行文本输入框。属性：name（名称）、rows（行数高度）、cols（列数宽度）、placeholder（提示文字）。有结束标签</textarea>。', 1, 'DEFINITION'),
(10321, 10170, '<select>和<option>标签如何使用？', '<select>创建下拉列表，name属性指定控件名称。<option>定义选项，value属性指定提交值，selected属性默认选中。<optgroup>可对选项分组。', 2, 'DEFINITION'),
(10322, 10170, '<textarea>和<input type="text">有什么不同？', 'textarea：多行、可换行、有结束标签、rows/cols设大小。input text：单行、不能换行、空标签、默认宽度小。', 3, 'COMPARISON');

-- ============================================================
-- 文章10171: 多媒体标签 audio/video（node=1145）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10171, '多媒体标签——在网页中嵌入音视频',
'## audio标签——嵌入音频

`<audio>`标签在网页中嵌入**音频文件**，提供播放控制。

```html
<audio src="music.mp3" controls>
  您的浏览器不支持音频播放。
</audio>
```

### audio常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`src`** | 音频文件路径 | `src="music.mp3"` |
| **`controls`** | 显示播放控件（播放/暂停/音量） | `controls` |
| `autoplay` | 自动播放（页面加载后自动播放） | `autoplay` |
| `loop` | 循环播放 | `loop` |
| `muted` | 静音播放 | `muted` |

### 支持多种格式
```html
<audio controls>
  <source src="music.mp3" type="audio/mpeg">
  <source src="music.ogg" type="audio/ogg">
  您的浏览器不支持音频播放。
</audio>
```
浏览器会自动选择第一个支持的格式播放。

## video标签——嵌入视频

`<video>`标签在网页中嵌入**视频文件**。

```html
<video src="movie.mp4" controls width="600">
  您的浏览器不支持视频播放。
</video>
```

### video常用属性

| 属性 | 说明 | 示例 |
|:----:|:------|:------|
| **`src`** | 视频文件路径 | `src="movie.mp4"` |
| **`controls`** | 显示播放控件 | `controls` |
| `width`/`height` | 视频播放器宽高 | `width="640" height="360"` |
| `autoplay` | 自动播放 | `autoplay` |
| `loop` | 循环播放 | `loop` |
| `muted` | 静音播放 | `muted` |
| `poster` | 视频封面的图片 | `poster="cover.jpg"` |

### 多格式支持
```html
<video controls width="640">
  <source src="movie.mp4" type="video/mp4">
  <source src="movie.webm" type="video/webm">
  您的浏览器不支持视频播放。
</video>
```

### audio vs video对比

| 对比 | audio | video |
|:----:|:-----:|:-----:|
| 播放内容 | 音频（音乐/语音） | 视频（画面+声音） |
| 视觉 | 仅显示控件 | 显示画面+控件 |
| 特有属性 | — | width、height、poster |

> **必记**：controls属性显示播放控件（必须有才能让用户控制播放）。autoplay自动播放（很多浏览器限制自动播放声音）。两者都不是空标签，有结束标签。',
'<audio>嵌入音频、<video>嵌入视频。核心属性：src（文件路径）、controls（显示控件）、autoplay（自动播放）、loop（循环）。<source>标签提供多种格式供浏览器选择。',
5, '单元6 设计制作网页', '任务5 使用表单', 1145,
'audio/video记法："audio听歌，video看片"\n共同属性："src路径/controls控件/autoplay自动/loop循环"\nvideo独有："width宽/height高/poster封面"',
'【必考】①audio嵌入音频 ②video嵌入视频 ③controls=显示播放控件（必记）④autoplay=自动播放 ⑤src=文件路径',
1,
'["HTML","audio","video","多媒体","音频","视频","controls","autoplay","HTML5"]',
'["HTML网页制作基础","多媒体标签"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10323, 10171, '<audio>和<video>标签各有什么作用？', '<audio>在网页中嵌入音频文件。<video>在网页中嵌入视频文件。两者都是HTML5新增标签，支持controls/autoplay/loop等属性。', 1, 'DEFINITION'),
(10324, 10171, '<audio>和<video>的常用属性有哪些？', 'src（文件路径）、controls（显示控件首选）、autoplay（自动播放）、loop（循环播放）、muted（静音）。video还有width/height（宽高）、poster（封面）。', 2, 'DEFINITION'),
(10325, 10171, '如何让<video>支持多种视频格式？', '使用<source>标签提供多个来源：<video><source src="a.mp4" type="video/mp4"><source src="a.webm" type="video/webm">您的浏览器不支持。</video>。浏览器自动选第一个支持的格式。', 3, 'APPLICATION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v175] 单元6 知识库文章种子完成：共13篇文章，约39张记忆卡片。' AS result;
