-- ============================================================================
-- v173: 网络应用基础 — 知识库文章种子（单元4 畅游Internet）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖5个任务9个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元4：畅游Internet
-- ═══════════════════════════════════════════════════════════════
-- 任务1 接入Internet（节点1117~1118）
-- 任务2 应用WWW服务（节点1119~1122）
-- 任务3 应用FTP服务（节点1123）
-- 任务4 应用Email服务（节点1124）
-- 任务5 应用远程登录服务（节点1125）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10143: Internet的概念与发展（node=1117）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10143, 'Internet——全球最大的网际网',
'## 什么是Internet？

Internet（因特网）是全球最大的、开放的、由众多网络互连而成的**网际网**。

### 核心特征
- 基于**TCP/IP**协议族
- 全球互联，无中心管理节点
- 提供WWW、Email、FTP等服务

> Internet ≠ WWW：Internet是"高速公路网"，WWW是"跑在高速上的快递服务"。

## Internet发展简史

| 时间 | 里程碑 | 说明 |
|:----:|:------:|:------|
| **1969年** | **ARPANET诞生** | 美国国防部高级研究计划局，最初4个节点 |
| **1974年** | TCP/IP雏形 | Kahn和Cerf提出TCP/IP协议 |
| **1983年** | **TCP/IP正式启用** | ARPANET全面切换到TCP/IP，现代Internet诞生 |
| **1989年** | WWW发明 | Tim Berners-Lee提出万维网概念 |
| **1991年** | WWW发布 | 第一个网站上线 |
| **1994年** | **中国全功能接入** | 通过中科院高能所接入Internet |
| 1998年 | Google成立 | 搜索引擎时代 |
| 今天 | 物联网/云计算/AI | 数十亿设备在线 |

## 中国互联网的关键节点

| 时间 | 事件 |
|:----:|:------|
| **1994年4月20日** | **中国全功能接入Internet**（通过中科院高能所64K专线） |
| CNNIC成立 | 中国互联网络信息中心，管理.cn域名和IP地址 |

## Internet提供的主要服务

| 服务 | 英文 | 说明 |
|:----:|:----:|:------|
| **万维网** | **WWW** | 网页浏览（最广泛使用的服务） |
| **电子邮件** | **Email** | 收发邮件 |
| **文件传输** | **FTP** | 上传和下载文件 |
| **远程登录** | **Telnet/SSH** | 远程管理计算机 |
| **即时通信** | IM | QQ、微信等 |

> **必考**：Internet的前身是**ARPANET**，Internet基于**TCP/IP**协议。中国在**1994年**全功能接入Internet。',
'Internet是全球最大的网际网，基于TCP/IP协议。前身是1969年的ARPANET。1983年TCP/IP正式启用。中国于1994年全功能接入。提供的服务：WWW、Email、FTP等。',
5, '单元4 畅游Internet', '任务1 接入Internet', 1117,
'互联网记法："1969ARPANET诞生，1983TCP/IP立功，1994中国接入"\n\nInternet≠WWW：Internet是路，WWW是车',
'【必考】①Internet前身=ARPANET ②基于TCP/IP协议 ③中国1994年接入 ④Internet提供的主要服务',
1,
'["Internet","因特网","ARPANET","TCP/IP","互联网历史","中国接入"]',
'["Internet基础","Internet概念与发展"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10239, 10143, 'Internet的起源和发展关键时间点？', '1969年ARPANET诞生（4个节点）→1983年TCP/IP正式启用→1991年WWW发布→1994年中国全功能接入。', 1, 'PROCEDURE'),
(10240, 10143, 'Internet的核心特征是什么？', '①基于TCP/IP协议族②全球互联无中心管理③提供WWW/Email/FTP等服务。Internet是网络基础设施，不等于WWW。', 2, 'DEFINITION'),
(10241, 10143, '中国互联网的关键节点是什么？', '1994年4月20日通过中科院高能所64K专线全功能接入Internet。CNNIC（中国互联网络信息中心）管理.cn域名。', 3, 'DEFINITION');

-- ============================================================
-- 文章10144: Internet接入方式（node=1118）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10144, 'Internet接入方式——从56K到千兆',
'## 常见Internet接入方式对比

| 接入方式 | 传输介质 | 典型速率 | 适用场景 |
|:--------:|:--------:|:--------:|:---------|
| **电话拨号** | 电话线+Modem | 56Kbps | ❌ 已淘汰 |
| **ADSL** | 电话线（频分复用） | 2~8Mbps下行 | ❌ 早期家庭宽带 |
| **光纤（FTTH）** | **光纤** | **100Mbps~1Gbps** | ✅ **当前家庭/学校主流** |
| **LAN接入** | 以太网双绞线 | 100Mbps~1Gbps | ✅ 小区/校园宽带 |
| **4G/5G** | 无线蜂窝网络 | 100Mbps~1Gbps | ✅ 手机/移动设备 |
| **Wi-Fi** | 无线 | 可达数百Mbps | ✅ 家庭/公共热点 |

## 各接入方式详解

### 光纤宽带（FTTH）——当前最主流
- **FTTH** = Fiber To The Home（光纤到户）
- **设备**：光猫（ONU）+ 路由器
- **特点**：速度快、稳定、抗干扰
- **速率**：100M/200M/500M/1000Mbps

### ADSL——已被光纤取代
- 利用电话线高频段传输数据，不影响通话
- 下行速率大于上行（非对称）
- 受距离影响大（离局端越远越慢）

### 4G/5G移动网络
| 世代 | 速率 | 时延 |
|:----:|:----:|:----:|
| 4G LTE | 100~150Mbps | 30~50ms |
| **5G** | **1~10Gbps** | **1~10ms** |

### 各接入方式所需设备

| 接入方式 | 所需设备 |
|:--------:|:---------|
| 电话拨号 | 56K Modem（已淘汰） |
| ADSL | ADSL Modem + 电话线 |
| **光纤** | **光猫（ONU）+ 路由器** |
| LAN接入 | 网卡 + 网线 |
| 4G/5G | SIM卡 + 蜂窝模块 |
| Wi-Fi | 无线路由器/AP + 无线网卡 |

> **考试重点**：当前家庭宽带的主流是光纤（FTTH），需要光猫和路由器。ADSL已基本淘汰。',
'Internet接入方式从电话拨号（56Kbps）发展到光纤（100Mbps~1Gbps）。当前主流是FTTH光纤到户，需光猫+路由器。其他方式：ADSL（将淘汰）、4G/5G、Wi-Fi。',
5, '单元4 畅游Internet', '任务1 接入Internet', 1118,
'接入方式发展："56K电话→ADSL→光纤千兆"\n主流=FTTH光纤到户=光猫+路由器\n\nFTTH记法："Fiber To The Home=光纤到家"',
'【必考】①光纤FTTH是当前主流 ②光纤需光猫+路由器 ③ADSL已基本淘汰 ④各接入方式的设备区别',
1,
'["Internet接入","FTTH","光纤","ADSL","4G","5G","宽带接入"]',
'["Internet基础","Internet接入方式"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10242, 10144, '当前主流的Internet接入方式是什么？需要哪些设备？', '光纤FTTH（Fiber To The Home）是当前主流。需要光猫（ONU）将光信号转为电信号，再接路由器供多设备上网。速率可达1Gbps。', 1, 'DEFINITION'),
(10243, 10144, '常见的Internet接入方式有哪些？', '①光纤FTTH（当前主流）②ADSL（已淘汰）③LAN接入（小区/校园）④4G/5G（移动）⑤Wi-Fi（无线）。从56K拨号到千兆光纤。', 2, 'DEFINITION'),
(10244, 10144, 'ADSL和光纤在原理上有什么不同？', 'ADSL：利用电话线高频段传输数据，速率受距离影响大，非对称。光纤：利用光信号在光纤中传输，速率高、抗干扰、不受距离影响。光纤全面替代了ADSL。', 3, 'COMPARISON');

-- ============================================================
-- 文章10145: WWW万维网概念（node=1119）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10145, 'WWW万维网——Internet上最亮的星',
'## 什么是WWW？

WWW（World Wide Web，万维网）是基于**HTTP协议**的**超文本信息系统**，通过超链接将全球信息互联。

### 一句话理解
> 你每天用浏览器打开的网页，就是WWW。WWW是Internet上**最广泛使用的服务**。

## 发明者

| 人物 | 国籍 | 成就 | 时间 |
|:----:|:----:|:------|:----:|
| **Tim Berners-Lee** | 英国 | 发明WWW、提出URL/HTTP/HTML | **1989年提出，1991年发布** |

- 1989年在欧洲核子研究中心（CERN）提出万维网概念
- 1991年8月6日第一个网站在CERN上线
- 他将这项发明无偿公开，未申请专利

> 如果Tim Berners-Lee为WWW申请了专利，今天的互联网可能完全不同。他是互联网的"大慈善家"。

## WWW三要素

| 要素 | 全称 | 作用 |
|:----:|:----:|:------|
| **URL** | 统一资源定位符 | 告诉浏览器"去哪里找"——定位资源 |
| **HTTP** | 超文本传输协议 | 告诉浏览器"怎么传输"——传输协议 |
| **HTML** | 超文本标记语言 | 告诉浏览器"怎么显示"——编写网页 |

### 三要素关系
> **URL**定位资源 → **HTTP**获取资源 → **HTML**展示资源

## 重要概念区分

| 概念 | 关系 |
|:----:|:------|
| **Internet** | 全球计算机网络基础设施 |
| **WWW** | 运行在Internet上的**服务之一** |
| **浏览器** | 访问WWW服务的**客户端软件** |

> Internet ≠ WWW。Internet是高速公路网，WWW是跑在上面的快递服务。Internet还包括Email、FTP、在线游戏等非Web服务。',
'WWW是Internet上最广泛使用的服务，发明者Tim Berners-Lee（1989年）。三要素：URL（定位）、HTTP（传输）、HTML（显示）。Internet≠WWW（基础设施≠其上服务）。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1119,
'WWW三要素："用URL找→用HTTP传→用HTML展示"\n\nTim Berners-Lee记法："1989年提出，1991年上线"\n\nInternet vs WWW："Internet是马路，WWW是路上的车"',
'【必考】①WWW的发明者Tim Berners-Lee（1989年）②三要素：URL+HTTP+HTML ③Internet≠WWW',
2,
'["WWW","万维网","Tim Berners-Lee","HTTP","HTML","URL","超文本"]',
'["Internet基础","WWW"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10245, 10145, 'WWW（万维网）由谁在什么时候发明的？', '英国科学家Tim Berners-Lee。1989年在CERN提出万维网概念，1991年发布了第一个网站。他无偿公开了这项发明。', 1, 'DEFINITION'),
(10246, 10145, 'WWW三要素是什么？各有什么作用？', '①URL（统一资源定位符）—定位资源去哪找②HTTP（超文本传输协议）—规定如何传输③HTML（超文本标记语言）—编写网页内容。', 2, 'DEFINITION'),
(10247, 10145, 'Internet和WWW有什么区别？', 'Internet是全球互联的网络基础设施（高速公路网）。WWW是运行在Internet上的一个服务（跑在路上的快递）。Internet还包括Email、FTP等服务。', 3, 'COMPARISON');

-- ============================================================
-- 文章10146: URL统一资源定位符（node=1120）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10146, 'URL——Internet上的门牌号',
'## 什么是URL？

URL（Uniform Resource Locator，统一资源定位符）在Internet上**唯一定位一个资源**。

### 一句话理解
> URL就像"门牌号+房间号"——告诉浏览器要去哪个服务器、找哪个文件。

## URL格式

```
协议://主机名(域名或IP):端口/路径?查询参数#锚点
```

### 完整格式详解
```
https://www.example.com:443/teacher/index.html?id=123#top
└─┬──┘ └───────┬───────┘ └┬┘ └────────┬─────────┘└─┬─┘└┬┘
  协议          主机名      端口         路径         参数  锚点
```

| 部分 | 含义 | 示例 |
|:----:|:------|:------|
| **协议** | 使用的应用层协议 | `https`、`http`、`ftp` |
| **主机名** | 服务器的域名或IP | `www.example.com` |
| **端口** | 服务器上的端口号（可省略） | `:443`（HTTPS默认） |
| **路径** | 服务器上资源的位置 | `/teacher/index.html` |
| **查询参数** | 传递给服务器的参数 | `?id=123` |
| **锚点** | 网页内的书签跳转 | `#top` |

## URL示例解析

| URL | 解析 |
|:----|:-----|
| `https://www.baidu.com/` | 协议https，域名www.baidu.com，省略路径表示首页 |
| `http://192.168.1.1/login.html` | 协议http，IP地址，路径login.html |
| `ftp://files.example.com/software/` | 协议ftp，目录路径software |
| `https://item.jd.com/10001234.html` | 京东商品页面，路径包含商品ID |

## 常见URL协议

| 协议 | 默认端口 | 用途 |
|:----:|:--------:|:------|
| `http://` | 80 | 普通网页访问 |
| `https://` | 443 | 安全网页访问 |
| `ftp://` | 21 | 文件传输访问 |
| `mailto:` | — | 邮件链接（打开邮件客户端） |

> **考试重点**：URL格式为`协议://域名/路径`。各部分含义要能对应识别。',
'URL在Internet上唯一定位资源。格式：协议://主机名:端口/路径?参数#锚点。常见协议：http（80）、https（443）、ftp（21）。路径表示资源在服务器上的位置。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1120,
'URL记法："协议://域名/路径"\n\n就像快递地址：协议=快递方式、主机名=城市、路径=小区门牌',
'【必考】①URL格式：协议://域名/路径 ②各部分的含义识别 ③常见协议前缀（http/https/ftp）',
2,
'["URL","统一资源定位符","HTTP","HTTPS","域名","路径"]',
'["Internet基础","URL"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10248, 10146, '什么是URL？它的基本格式是什么？', 'URL（统一资源定位符）在Internet上唯一定位资源。基本格式：协议://主机名/路径。完整格式：协议://主机名:端口/路径?参数#锚点。', 1, 'DEFINITION'),
(10249, 10146, 'URL由哪些部分组成？各部分的含义？', '①协议（http/https/ftp）②主机名（域名或IP）③端口（默认可省略）④路径（服务器上的位置）⑤查询参数（?key=value）⑥锚点（#位置）。', 2, 'DEFINITION'),
(10250, 10146, 'URL中http://、https://、ftp://的区别？', 'http://（端口80，明文）、https://（端口443，加密）、ftp://（端口21，文件传输）。前缀告诉浏览器使用什么协议来访问资源。', 3, 'COMPARISON');

-- ============================================================
-- 文章10147: 浏览器的基本使用（node=1121）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10147, 'Web浏览器——通往WWW的大门',
'## 什么是浏览器？

浏览器是访问WWW服务的**客户端软件**，负责发送HTTP请求并渲染展示网页内容。

### 一句话理解
> 浏览器就像"一扇窗户"——你通过它看到Internet上万维网世界的一切。

## 常见浏览器

| 浏览器 | 内核 | 特点 |
|:------:|:----:|:------|
| **Microsoft Edge** | Chromium（Blink） | Windows 11默认，与Chrome兼容 |
| **Google Chrome** | Blink | 全球使用最广 |
| **Mozilla Firefox** | Gecko | 开源注重隐私 |
| **Apple Safari** | WebKit | macOS/iOS默认 |
| 360浏览器 | Trident+Blink | 国产双核 |

## 浏览器基本操作

| 操作 | 说明 | 快捷键 |
|:----:|:------|:------:|
| **地址栏输入URL** | 直接访问网站 | Alt+D |
| **收藏夹/书签** | 保存常用网站 | Ctrl+D |
| **历史记录** | 查看访问过的网页 | Ctrl+H |
| **下载管理** | 管理下载的文件 | Ctrl+J |
| **刷新页面** | 重新加载当前页面 | **F5** |
| **前进/后退** | 切换浏览历史 | Alt+← / Alt+→ |
| **新标签页** | 打开新标签 | Ctrl+T |

## 浏览器常见设置

| 设置项 | 说明 |
|:------:|:------|
| **主页设置** | 启动时自动打开的页面 |
| **清除浏览数据** | 清除历史记录、缓存、Cookie（Ctrl+Shift+Del） |
| **安全级别** | 高/中/低（影响脚本和ActiveX运行） |
| **弹窗拦截** | 阻止自动弹出的广告窗口 |
| **Cookie管理** | 允许或禁止网站保存登录状态 |

### 隐私保护
> 定期清除浏览历史、缓存和Cookie可以保护个人隐私。**无痕模式/隐私模式**下浏览器不保存历史记录。

> **考试重点**：浏览器是访问WWW的客户端软件。F5刷新、Ctrl+D收藏、Ctrl+H历史、Ctrl+J下载。常用浏览器：Edge、Chrome、Firefox、Safari。',
'浏览器是访问WWW服务的客户端软件。常见浏览器：Edge、Chrome、Firefox、Safari。操作：地址栏输入URL、F5刷新、Ctrl+D收藏、Ctrl+H历史、Ctrl+J下载。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1121,
'快捷键记法："F5=刷新，Ctrl+D=收藏，Ctrl+H=历史，Ctrl+J=下载，Ctrl+T=新标签"\n清除数据："Ctrl+Shift+Del=清除浏览数据"',
'【必考】①浏览器的概念（访问WWW的客户端软件）②常见浏览器名称 ③基本快捷键（F5刷新/Ctrl+D收藏/Ctrl+H历史）',
1,
'["浏览器","Web浏览器","Edge","Chrome","Firefox","Safari","网页浏览"]',
'["Internet基础","浏览器"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10251, 10147, '常见的Web浏览器有哪些？', '①Microsoft Edge（Windows默认）②Google Chrome（全球最广）③Mozilla Firefox（开源隐私）④Apple Safari（苹果默认）。', 1, 'DEFINITION'),
(10252, 10147, '浏览器的常用快捷键有哪些？', 'F5刷新、Ctrl+D收藏、Ctrl+H历史记录、Ctrl+J下载管理、Ctrl+T新标签页、Alt+D定位地址栏、Ctrl+Shift+Del清除浏览数据。', 2, 'APPLICATION'),
(10253, 10147, '浏览器的主要功能是什么？', '作为客户端软件向Web服务器发送HTTP请求，接收服务器返回的HTML/CSS/JavaScript代码，解析渲染为可视化的网页内容。', 3, 'DEFINITION');

-- ============================================================
-- 文章10148: 搜索引擎的使用（node=1122）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10148, '搜索引擎——互联网的信息导航',
'## 什么是搜索引擎？

搜索引擎是帮助用户在Internet上查找信息的**信息检索系统**。

### 一句话理解
> 搜索引擎像"图书馆的图书检索系统"——你输入想找的内容，它告诉你哪些网页包含这些内容。

## 常见搜索引擎

| 搜索引擎 | 网址 | 特点 |
|:--------:|:----:|:------|
| **百度** | www.baidu.com | 中文搜索最常用，国内市场第一 |
| **Google** | www.google.com | 全球最大搜索引擎 |
| **必应** | www.bing.com | 微软出品，国外搜索结果好 |
| 搜狗 | www.sogou.com | 国产，接入微信搜索 |

## 基本搜索技巧

| 技巧 | 语法 | 示例 | 作用 |
|:----:|:----:|:------|:----:|
| **精确匹配** | 加英文双引号 | `"计算机网络定义"` | 搜索结果必须包含完整短语 |
| **排除词** | 减号 | `计算机病毒 -木马` | 排除包含"木马"的结果 |
| **站内搜索** | **site:** | **`site:edu.cn 招生`** | 只在.edu.cn网站内搜索 |
| **文件类型** | **filetype:** | **`计算机基础 filetype:pdf`** | 只搜索PDF文件 |
| 通配符 | 星号`*` | `计算机*技术` | 匹配任意词 |

> **注意**：搜索语法中的冒号、引号、减号都是**英文半角**符号。

## 搜索引擎工作原理

```
①爬虫（Spider/Crawler）→按链接抓取网页内容
        ↓
②索引（Index）→提取关键词建立索引数据库
        ↓
③排序（Rank）→根据相关性（百度：超链分析；Google：PageRank）返回结果
        ↓
④用户输入关键词→在索引中匹配→展示排序结果
```

## 高级搜索

| 百度高级搜索 | 说明 |
|:------------:|:------|
| `intitle:关键词` | 搜索标题中包含关键词的网页 |
| `inurl:关键词` | 搜索URL中包含关键词的网页 |
| `date:2024` | 限定时间范围 |

### 搜索建议
> 使用**多个关键词**比单个关键词更精准。如搜"四川对口升学计算机考试大纲"比只搜"考试大纲"效果好得多。

> **考试重点**：搜索引擎的使用（关键词搜索、高级搜索语法）。filetype:搜索文件类型、site:限定网站。百度是国内最常用搜索引擎。',
'搜索引擎帮用户在Internet上查找信息。常见：百度（中文最常用）、Google（全球最大）、必应。搜索技巧：""精确匹配、-排除词、site:站内搜、filetype:文件类型。',
5, '单元4 畅游Internet', '任务2 应用WWW服务', 1122,
'搜索技巧口诀："双引号精准找，减号排除掉，site站内搜，filetype看文件类型"\n工作原理："爬虫抓→建索引→排顺序→返回结果"',
'【必考】①百度是国内最常用搜索引擎 ②filetype:按文件类型搜索 ③site:限定网站搜索 ④关键词搜索用多个关键词更精准',
1,
'["搜索引擎","百度","Google","搜索技巧","filetype","site","信息检索"]',
'["Internet基础","搜索引擎"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10254, 10148, '常用的搜索引擎有哪些？', '百度（www.baidu.com）—中文最常用、Google（www.google.com）—全球最大、必应（www.bing.com）—微软出品。', 1, 'DEFINITION'),
(10255, 10148, '搜索引擎的常用搜索语法有哪些？', '①"关键词"—精确匹配②site:域名—站内搜索③filetype:格式—按文件类型搜索④减号—排除关键词。语法符号必须用英文半角。', 2, 'APPLICATION'),
(10256, 10148, '搜索引擎的三步工作原理是什么？', '①爬虫（Spider）按链接抓取网页②建立索引数据库（提取关键词）③按相关性算法排序返回结果。百度用超链分析，Google用PageRank。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10149: FTP文件传输协议（node=1123）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10149, 'FTP文件传输——在Internet上搬文件',
'## FTP在Internet中的作用

FTP（File Transfer Protocol）用于在Internet上将文件从一台计算机传输到另一台计算机——**上传**和**下载**。

### 一句话理解
> 你从某个网站下载软件安装包，或者把自己的文件上传到云服务器，背后就是FTP在工作。

## FTP回顾

| 知识点 | 说明 |
|:------:|:------|
| **端口** | 控制连接 **21**，数据连接 **20** |
| **传输层协议** | TCP（保证文件完整） |
| **功能** | 上传（本地→服务器）、下载（服务器→本地） |

## FTP传输模式

| 模式 | 适用于 | 说明 |
|:----:|:------:|:------|
| **ASCII模式** | 纯文本文件（.txt/.html/.css） | 自动转换不同系统的换行符 |
| **二进制模式** | 非文本文件（图片/程序/压缩包） | 原样传输，不做转换 |

> **错误提示**：如果下载的图片打不开或程序无法运行，通常是FTP传输模式选错了——应该用二进制模式。

## FTP的三种访问方式

| 方式 | 优点 | 缺点 |
|:----:|:------|:------|
| **命令行** | Windows自带`ftp`命令，无需安装 | 操作不便 |
| **浏览器** | 方便快捷，直接输入`ftp://...` | 功能有限 |
| **FTP客户端软件** | 图形界面、支持拖拽、断点续传 | 需安装 |

### 常用FTP客户端

| 软件 | 平台 | 特点 |
|:----:|:----:|:------|
| **FileZilla** | Windows/Mac/Linux | 免费开源，最常用 |
| CuteFTP | Windows | 老牌商业软件 |
| FlashFXP | Windows | 支持多线程 |

> **考试重点**：FTP功能=文件上传/下载，端口21。FileZilla是常用的FTP客户端。',
'FTP在Internet上实现文件上传和下载。端口21（控制）、20（数据）。两种传输模式：ASCII（文本）和二进制（非文本）。三种访问方式：命令行、浏览器、FTP客户端（FileZilla最常用）。',
5, '单元4 畅游Internet', '任务3 应用FTP服务', 1123,
'FTP两种模式："文本用ASCII，图片程序用二进制"\n访问方式："命令/浏览器/客户端"\nFileZilla="文件小动物🔵🟡"—最常用FTP客户端',
'【必考】①FTP功能=文件上传下载 ②端口21 ③两种传输模式（ASCII/二进制）的区别 ④FileZilla是常用FTP客户端',
1,
'["FTP","文件传输","上传","下载","FileZilla","ASCII","二进制"]',
'["Internet基础","FTP"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10257, 10149, 'FTP在Internet上的作用是什么？', '在Internet上实现文件的上传（本地→服务器）和下载（服务器→本地）。默认端口21（控制连接），基于TCP传输保证文件完整。', 1, 'DEFINITION'),
(10258, 10149, 'FTP的ASCII模式和二进制模式有什么区别？', 'ASCII模式适用于文本文件，自动转换换行符。二进制模式适用于非文本文件（图片/程序/压缩包），原样传输不转换。图片下载后打不开通常是模式选错了。', 2, 'COMPARISON'),
(10259, 10149, '有哪些方式可以访问FTP服务器？', '①命令行：Windows自带ftp命令②浏览器：地址栏输入ftp://服务器地址③FTP客户端：FileZilla等图形界面工具，支持拖拽和断点续传。', 3, 'APPLICATION');

-- ============================================================
-- 文章10150: 电子邮件系统（node=1124）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10150, '电子邮件——Internet上最古老的服务',
'## 电子邮件基础

### 邮箱地址格式
```
用户名@域名
示例：admin@school.edu.cn
      └─┬─┘└─────┬──────┘
      用户账号    邮件服务器域名
```
- **@** 读作"at"（在），表示"在...上"
- `admin@school.edu.cn` = "admin在学校邮件服务器上的邮箱"

### 电子邮件系统组成

| 组件 | 英文 | 作用 | 生活类比 |
|:----:|:----:|:------|:--------:|
| **邮件用户代理** | **MUA** | 用户使用的邮件客户端 | 你写信/收信的工具 |
| **邮件传输代理** | **MTA** | 邮件服务器，负责转发 | 邮局的邮递员/分拣机 |
| **邮件投递代理** | **MDA** | 将邮件投递到用户邮箱 | 送到你家信箱的快递员 |

## 邮件收发流程

```
发件人(Outlook) ──SMTP(25)──→ 发件服务器(MTA)
                                    │
                              SMTP(25) 互联网转发
                                    ↓
收件人(Web邮箱) ←──POP3(110)─── 收件服务器(MTA)
```

### 完整过程示例
> 张三用163邮箱发邮件给李四的QQ邮箱：
> 1. 张三在163网页写邮件点击发送
> 2. 发送到163邮件服务器（MTA）
> 3. 163服务器通过SMTP将邮件转发到QQ邮件服务器
> 4. 李四打开QQ邮箱查看（通过POP3/IMAP从服务器取邮件）

## Web邮箱 vs 客户端

| 方式 | 配置 | 使用场景 |
|:----:|:----:|:---------|
| **Web邮箱** | 无需配置，浏览器访问网页即可 | 临时/移动使用 |
| **邮件客户端**（Outlook/Foxmail） | 需配置SMTP和POP3服务器 | 日常办公/批量管理 |

### 客户端配置要点
> 使用邮件客户端时需填写的参数：
> - **SMTP服务器**：发件服务器地址（如 smtp.163.com）
> - **POP3服务器**：收件服务器地址（如 pop.163.com）
> - 对应的端口：SMTP=25，POP3=110

> **考试重点**：邮箱格式`用户名@域名`。SMTP=发邮件（25），POP3=收邮件（110）。Web邮箱无需配置，客户端需配置SMTP/POP3。',
'邮箱地址格式：用户名@域名。邮件系统由MUA（客户端）、MTA（传输代理）、MDA（投递代理）组成。SMTP（端口25）发邮件，POP3（端口110）收邮件。Web邮箱无需配置。',
5, '单元4 畅游Internet', '任务4 应用Email服务', 1124,
'@记法："@=at=在……上面"\n\nSMTP发=PUSH（推），POP3收=PULL（拉）\n\n"发邮件找SMTP（25），收邮件找POP3（110）"',
'【必考】①邮箱格式：用户名@域名 ②SMTP发邮件（25）③POP3收邮件（110）④Web邮箱vs客户端的区别',
2,
'["电子邮件","Email","SMTP","POP3","MUA","MTA","MDA","邮箱"]',
'["Internet基础","电子邮件"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10260, 10150, '电子邮件地址的格式是什么？', '用户名@域名。如admin@school.edu.cn。@读作"at"，表示"在……上"。', 1, 'DEFINITION'),
(10261, 10150, '电子邮件系统由哪些组件组成？', '①MUA（Mail User Agent）—用户使用的邮件客户端②MTA（Mail Transfer Agent）—服务器间转发邮件③MDA（Mail Delivery Agent）—投递邮件到用户邮箱。', 2, 'DEFINITION'),
(10262, 10150, 'Web邮箱和邮件客户端有什么区别？', 'Web邮箱：浏览器直接访问，无需配置，方便临时使用。邮件客户端（Outlook/Foxmail）：需配置SMTP和POP3服务器，适合日常办公批量管理邮件。', 3, 'COMPARISON');

-- ============================================================
-- 文章10151: 远程登录（node=1125）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10151, '远程登录——远程管理计算机',
'## 什么是远程登录？

远程登录是指通过网络从本地计算机登录到**远程计算机**并执行操作的技术。

### 一句话理解
> 你在家里通过电脑连接到学校机房的服务器，像坐在机房面前一样操作它——这就是远程登录。

## Telnet

| 属性 | 说明 |
|:----:|:------|
| **端口** | **23** |
| **传输方式** | **明文传输**（包括用户名和密码） |
| **安全性** | 低——数据易被窃听 |
| **状态** | ✅ 已基本被SSH取代 |

### Telnet的使用
```cmd
telnet 192.168.1.100       # 远程登录到192.168.1.100
telnet www.example.com 80  # 测试目标主机的80端口是否开放
```

> 在Windows中Telnet客户端默认未安装，需在"启用或关闭Windows功能"中开启。

## SSH（安全外壳协议）

| 属性 | 说明 |
|:----:|:------|
| **端口** | **22** |
| **传输方式** | **加密传输** |
| **安全性** | 高——数据加密防止窃听 |
| **状态** | ✅ 当前远程管理的**主流标准** |

### SSH的典型应用
```cmd
ssh root@192.168.1.100     # SSH登录到Linux服务器
```

### SSH的优势
> ①加密传输——密码和命令不泄露 ②身份验证——防止中间人攻击 ③支持文件传输（SFTP）和端口转发

## Telnet vs SSH

| 对比维度 | Telnet | SSH |
|:--------:|:------:|:---:|
| **端口** | **23** | **22** |
| **加密** | ❌ 无（明文） | ✅ 加密 |
| **安全性** | 低 | 高 |
| **速度** | 略快（无加密开销） | 略慢（加密解密） |
| **适用场景** | 简单测试/老旧设备 | 服务器远程管理（主流） |
| **状态** | 基本淘汰 | 当前标准 |

> **对口升学考点**：Telnet=明文传输（端口23），SSH=加密传输（端口22）。SSH更安全，是目前远程登录的主流方式。',
'远程登录通过网络远程操作另一台计算机。Telnet（端口23，明文传输，不安全，已淘汰）vs SSH（端口22，加密传输，安全，当前主流）。Linux服务器远程管理主要使用SSH。',
5, '单元4 畅游Internet', '任务5 应用远程登录服务', 1125,
'Telnet vs SSH："Telnet=23/明文/淘汰，SSH=22/加密/主流"\n\n"SSH比Telnet多了一个S=Secure（安全）=加密"',
'【必考】①远程登录的概念 ②Telnet端口23、明文传输 ③SSH端口22、加密传输 ④SSH比Telnet更安全，是当前主流',
1,
'["远程登录","Telnet","SSH","23端口","22端口","加密传输","远程管理"]',
'["Internet基础","远程登录"]',
'[]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10263, 10151, '什么是远程登录？Telnet和SSH分别使用什么端口？', '远程登录通过网络操作远程计算机。Telnet端口23（明文传输）。SSH端口22（加密传输）。', 1, 'DEFINITION'),
(10264, 10151, 'Telnet和SSH有什么区别？', 'Telnet：端口23、明文传输（含密码）、不安全、已淘汰。SSH：端口22、加密传输、安全、当前远程管理主流标准。SSH=Telnet+S（Secure）。', 2, 'COMPARISON'),
(10265, 10151, '为什么SSH取代了Telnet？', 'Telnet所有数据（包括密码）都是明文传输，在网络上可以被轻松窃听。SSH对数据进行加密，即使被截获也无法破解。安全性是SSH取代Telnet的根本原因。', 3, 'SCENARIO');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v173] 单元4 知识库文章种子完成：共9篇文章，约27张记忆卡片。' AS result;
