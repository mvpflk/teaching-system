-- ============================================================================
-- v172: 网络应用基础 — 知识库文章种子（单元3 管理局域网）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅，高教社 2021
-- 考纲: 四川省对口升学计算机类(2023版)
-- subject_id=5，覆盖5个任务9个知识点
-- 幂等：INSERT IGNORE INTO（可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- 单元3：管理局域网
-- ═══════════════════════════════════════════════════════════════
-- 任务1 使用网络操作系统（节点1108）
-- 任务2 创建和管理域（节点1109）
-- 任务3 创建DNS和DHCP服务器（节点1110~1111）
-- 任务4 配置Internet信息服务（节点1112）
-- 任务5 应用网络命令（节点1113~1116）
-- ═══════════════════════════════════════════════════════════════

-- ============================================================
-- 文章10134: 网络操作系统基本概念（node=1108）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10134, '网络操作系统（NOS）——网络的指挥官',
'## 什么是网络操作系统？

网络操作系统（Network Operating System，NOS）是在普通操作系统基础上增加了**网络通信、资源共享、用户管理、安全控制**等网络功能的操作系统。

### 一句话理解
> 普通操作系统只管"本机工作"（如Windows 10），网络操作系统能管"全网工作"（如Windows Server）。

## 常见网络操作系统

| 系统 | 类型 | 特点 |
|:----:|:----:|:------|
| **Windows Server** | 商业 | 图形界面友好，适合中小企业，Active Directory域管理 |
| **Linux**（CentOS/Ubuntu Server） | 开源 | 命令行为主，稳定安全，适合Web服务器 |
| **Unix** | 商业 | 历史悠久，用于大型服务器，安全性极高 |

## 网络操作系统的核心功能

| 功能 | 说明 |
|:----:|:------|
| **网络通信** | 支持TCP/IP等协议栈，数据在网络上传输 |
| **资源共享** | 文件和打印机共享（Windows的SMB/CIFS协议） |
| **用户管理** | 域控制器集中管理用户账户和权限 |
| **安全服务** | 身份验证、访问控制列表（ACL） |

## 对等网 vs 客户机/服务器模式

| 对比维度 | 对等网（Peer-to-Peer） | 客户机/服务器（C/S） |
|:--------:|:---------------------:|:-------------------:|
| 管理方式 | 分散管理——每台机自己管 | 集中管理——服务器统一管 |
| 适用规模 | 小（≤10台） | 大（10台以上） |
| 成本 | 低（无需专用服务器） | 高（需专用服务器） |
| 安全性 | 低 | 高 |
| 典型场景 | 家庭/小办公室 | 学校机房/企业 |

> **考试重点**：对等网适合小规模、无专用服务器；C/S模式适合大规模、有域控制器统一管理。Windows Server是最常见的网络操作系统之一。',
'网络操作系统在普通操作系统上增加了网络通信、资源共享、用户管理和安全控制功能。常见NOS：Windows Server（商业）、Linux（开源）、Unix。对等网（≤10台）vs C/S模式（大规模集中管理）。',
5, '单元3 管理局域网', '任务1 使用网络操作系统', 1108,
'NOS记法："网络操作系统=本机OS+网络管理功能"\n\n对等网 vs C/S："小对等、大C/S、家用对等、学校C/S"',
'【必考】①NOS的概念（普通OS+网络功能）②Windows Server是商业、Linux是开源 ③对等网（≤10台/无服务器）vs C/S模式的区别',
1,
'["网络操作系统","NOS","Windows Server","Linux","对等网","C/S模式"]',
'["网络操作系统"]',
'[
  {"type":"choice","question":"以下哪个是开源免费的网络操作系统？","options":["Windows Server 2022","Linux（Ubuntu Server）","Unix","Windows 11"],"answer":"B","explanation":"Linux是开源免费的网络操作系统。Windows Server和Unix是商业系统。Windows 11是个人电脑操作系统。"},
  {"type":"choice","question":"对等网模式最适合以下哪种场景？","options":["大型企业500人","学校机房200台","家庭/小办公室5台","银行数据中心"],"answer":"C","explanation":"对等网适合10台以下的小规模网络（如家庭和小办公室），每台计算机既当客户机又当服务器。"},
  {"type":"judge","question":"Windows Server是目前常用的商业网络操作系统之一。","answer":"T","explanation":"Windows Server是Microsoft的商业网络操作系统，提供Active Directory、文件共享等服务，广泛应用于中小企业和学校。"},
  {"type":"multi","question":"网络操作系统应具备哪些核心功能？（多选）","options":["网络通信","资源共享","用户管理","游戏娱乐","安全控制"],"answer":"A,B,C,E","explanation":"NOS核心功能：网络通信、资源共享、用户管理、安全控制。游戏娱乐不属于网络操作系统的核心功能范畴。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10212, 10134, '什么是网络操作系统（NOS）？', '在普通操作系统基础上增加了网络通信、资源共享、用户管理和安全控制功能的操作系统。常见：Windows Server（商业）、Linux（开源）。', 1, 'DEFINITION'),
(10213, 10134, '对等网和C/S模式有什么区别？', '对等网：分散管理、≤10台、无专用服务器、成本低、安全性低。C/S：集中管理、10台以上、有专用服务器、安全性高。家用小对等，学校用C/S。', 2, 'COMPARISON'),
(10214, 10134, '网络操作系统的四大核心功能？', '①网络通信（TCP/IP协议栈）②资源共享（文件/打印机）③用户管理（账户/权限）④安全服务（身份验证/ACL）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10135: 域的基本概念（node=1109）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10135, '域（Domain）——Windows网络的集中管理',
'## 什么是域？

域是Windows网络中**安全管理的边界**，由一个或多个域控制器集中管理用户账户、计算机和权限。

### 一句话理解
> 域就像"学校的学生管理系统"——所有学生信息（账户）都在教务处的服务器（域控制器）上统一管理，你在任何教室（任何计算机）登录都能认。

## 核心组件

| 组件 | 英文 | 作用 |
|:----:|:----:|:------|
| **域控制器** | Domain Controller (DC) | 运行Active Directory的服务器，集中管理账户和策略 |
| **成员计算机** | Member Computer | 加入域的客户机，使用域账户登录 |
| **Active Directory** | AD | Windows的目录服务，保存所有对象的数据库 |

## 工作组 vs 域

| 对比维度 | 工作组（Workgroup） | 域（Domain） |
|:--------:|:-----------------:|:-----------:|
| 管理模式 | **分散管理**——每台机存自己的账户 | **集中管理**——域控制器统一存 |
| 账户存储 | 每台计算机本地SAM数据库 | Active Directory数据库 |
| 适用规模 | **10台以下** | **10台以上** |
| 安全性 | 低 | 高 |
| 单点登录 | 不支持——每台机都要有账户 | 支持——一个账户全网络通用 |
| 典型场景 | 家庭局域网 | 学校机房、企业办公 |

### 工作组的典型场景
> 家庭3台电脑，每台用不同的登录密码——这就是工作组。

### 域的典型场景
> 学校机房200台电脑，学生用统一的学号登录任意一台电脑——这就是域。

## 域的好处

| 好处 | 说明 |
|:----:|:------|
| **单点登录** | 一个域账户可登录域中任意计算机 |
| **集中管理** | 管理员在域控制器上统一配置安全策略（如密码策略、软件限制） |
| **漫游配置文件** | 用户在任何计算机上登录都是自己的桌面和文件 |
| **可扩展** | 支持成千上万的用户和计算机 |

> **考试重点**：域的本质是"集中管理"——账户、安全策略都由域控制器统一管理。与工作组（分散管理）对比是常考内容。',
'域是Windows网络中安全管理的边界，由域控制器（DC）集中管理账户和权限。核心组件：域控制器、Active Directory、成员计算机。工作组（≤10台分散）vs域（大规模集中）。',
5, '单元3 管理局域网', '任务2 创建和管理域', 1109,
'域记法："域=集中管理，工作组=各管各的"\n\n"单点登录"=一个账户走遍全网络，不用每台机建账户',
'【必考】①域的概念（安全边界/集中管理）②域控制器DC的作用 ③工作组vs域的区别（规模/管理方式/安全性）④单点登录的好处',
1,
'["域","Domain","域控制器","DC","Active Directory","工作组","单点登录"]',
'["网络操作系统"]',
'[
  {"type":"choice","question":"Windows域中集中管理用户账户的服务器叫什么？","options":["DNS服务器","域控制器（DC）","Web服务器","文件服务器"],"answer":"B","explanation":"域控制器运行Active Directory，在域中集中存储和管理所有用户账户、计算机和权限。"},
  {"type":"choice","question":"工作组模式适用于多少台计算机以下的网络？","options":["5台","10台","50台","100台"],"answer":"B","explanation":"工作组适用于10台以下的小型网络，超过10台建议使用域模式集中管理。"},
  {"type":"judge","question":"域模式下，用户可以用一个账户登录域中任意一台计算机。","answer":"T","explanation":"这是域的」单点登录」功能——域账户存储在域控制器上，用户在任何加入域的计算机上都能用同一账户登录。"},
  {"type":"multi","question":"域模式相比工作组有哪些优势？（多选）","options":["集中管理用户账户","支持单点登录","无需网络即可使用","可扩展支持大量用户","安全策略统一配置"],"answer":"A,B,D,E","description":"域的优势：集中管理、单点登录、可扩展、统一安全策略。域需要网络连接才能使用域账户登录。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10215, 10135, '什么是域（Domain）？', '域是Windows网络中安全管理的边界。由域控制器（DC）通过Active Directory集中管理用户账户、计算机和权限。一个域账户可登录任意加入域的计算机。', 1, 'DEFINITION'),
(10216, 10135, '工作组和域有什么区别？', '工作组：分散管理、≤10台、每台独立存账户、安全性低。域：集中管理、10台以上、域控统一存账户、安全性高、支持单点登录。', 2, 'COMPARISON'),
(10217, 10135, '域有哪些主要好处？', '①单点登录—一个账户全网通用②集中管理—管理员统一配置③漫游配置—任何电脑都是自己的桌面④可扩展—支持成千上万的用户。', 3, 'DEFINITION');

-- ============================================================
-- 文章10136: DNS服务器的功能（node=1110）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10136, 'DNS服务器——网络中的翻译官',
'## DNS服务器的核心功能

DNS服务器的主要功能是**域名解析**——将用户输入的域名转换为对应的IP地址。

| 功能 | 说明 |
|:----:|:------|
| **正向解析** | 域名 → IP地址（最常用，如 www.baidu.com → 183.2.172.185） |
| **反向解析** | IP地址 → 域名（如 183.2.172.185 → www.baidu.com） |
| **负载均衡** | 一个域名对应多个IP，DNS轮询返回不同IP分担压力 |

## DNS服务器类型

| 类型 | 说明 |
|:----:|:------|
| **本地DNS服务器** | 用户直接查询的服务器（ISP提供或学校自建） |
| **根DNS服务器** | 全球13组，知道所有顶级域（.com/.cn等）的位置 |
| **权威DNS服务器** | 存储特定域名的最终解析记录（如baidu.com的DNS） |

## 常见DNS记录类型

| 记录类型 | 作用 | 示例 |
|:--------:|:----:|:----:|
| **A记录** | 域名 → IPv4地址 | www.example.com → 192.168.1.10 |
| **AAAA记录** | 域名 → IPv6地址 | www.example.com → 2001:db8::1 |
| **MX记录** | 指定邮件服务器 | @ → mail.example.com |
| **CNAME记录** | 域名别名（指向另一个域名） | m.example.com → www.example.com |

### 记录类型记忆
> **A记录=Address（地址），AAAA=IPv6地址，MX=Mail eXchange（邮件交换）**

## 实际应用场景

- **学校自建DNS服务器**：加速校内网站访问，过滤不良网站
- **域名注册商**：提供DNS解析服务（如阿里云DNS、腾讯云DNS）
- **常用公共DNS**：114.114.114.114（国内快速）、8.8.8.8（Google全球）

> **考试重点**：DNS服务器的核心功能是域名→IP的解析。A记录=域名→IPv4，AAAA=域名→IPv6，MX=邮件服务器。',
'DNS服务器的核心功能是将域名解析为IP地址。常见记录类型：A（域名→IPv4）、AAAA（域名→IPv6）、MX（邮件服务器）。服务器类型：本地DNS、根DNS、权威DNS。',
5, '单元3 管理局域网', '任务3 创建DNS和DHCP服务器', 1110,
'DNS记法："DNS=Domain Name System=域名变IP"\n记录类型："A=Address地址，AAAA=IPv6，MX=Mail邮件"',
'【必考】①DNS核心功能：域名→IP地址 ②A记录（域名→IPv4）③MX记录（邮件服务器）④DNS服务器类型',
2,
'["DNS服务器","域名解析","A记录","AAAA记录","MX记录","域名系统"]',
'["TCP/IP协议与IP地址","DNS"]',
'[
  {"type":"choice","question":"DNS服务器中A记录的作用是什么？","options":["域名转为IPv4地址","域名转为IPv6地址","指定邮件服务器","设置域名别名"],"answer":"A","explanation":"A记录（Address Record）将域名解析为IPv4地址。AAAA记录才是用于IPv6。"},
  {"type":"choice","question":"MX记录在DNS中的作用是什么？","options":["域名解析到IPv6","指定邮件服务器","域名转发","负载均衡"],"answer":"B","explanation":"MX（Mail eXchange）记录指定处理该域名邮件的服务器地址，用于电子邮件系统。"},
  {"type":"judge","question":"全球共有13组根DNS服务器。","answer":"T","explanation":"根DNS服务器全球共有13组（编号A~M），由ICANN管理。它们知道所有顶级域DNS服务器的位置。"},
  {"type":"multi","question":"DNS服务器中常见的记录类型有哪些？（多选）","options":["A记录","AAAA记录","MX记录","DHCP记录","CNAME记录"],"answer":"A,B,C,E","explanation":"常见DNS记录：A（IPv4）、AAAA（IPv6）、MX（邮件）、CNAME（别名）。DHCP不是DNS记录类型。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10218, 10136, 'DNS服务器的核心功能是什么？', '将域名解析为IP地址（正向解析），也可将IP地址解析为域名（反向解析）。负载均衡：一个域名对应多个IP，轮询返回。', 1, 'DEFINITION'),
(10219, 10136, 'DNS的A记录、AAAA记录和MX记录各有什么作用？', 'A记录：域名→IPv4地址。AAAA记录：域名→IPv6地址。MX记录：指定域名的邮件服务器地址。', 2, 'DEFINITION'),
(10220, 10136, 'DNS服务器有哪几种类型？', '①本地DNS服务器（用户直接查询）②根DNS服务器（全球13组，知道顶级域位置）③权威DNS服务器（存储域名最终解析记录）。', 3, 'DEFINITION');

-- ============================================================
-- 文章10137: DHCP服务器的功能（node=1111）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10137, 'DHCP服务器——自动分配网络参数',
'## DHCP服务器的核心功能

DHCP服务器在网络中**自动分配IP地址**给客户端，并配置子网掩码、默认网关、DNS等参数。

| 功能 | 说明 |
|:----:|:------|
| **自动分配IP** | 客户端开机自动获取IP，无需手动配置 |
| **避免冲突** | 保证同一网络中不分配重复的IP地址 |
| **集中管理** | 在DHCP服务器上统一修改网络配置（如更换DNS） |

## DHCP分配的网络参数

| 参数 | 说明 |
|:----:|:------|
| **IP地址** | 分配给客户端的网络地址 |
| **子网掩码** | 确定网络范围 |
| **默认网关** | 访问外网的出口 |
| **DNS服务器** | 域名解析服务地址 |
| **租期** | IP地址的有效使用时间（到期自动更新） |

## DHCP的工作过程（回顾）

```
客户端                   DHCP服务器
  │── DHCP Discover ───→  │  ①广播找服务器
  │←── DHCP Offer ──────  │  ②提供可用IP
  │── DHCP Request ────→  │  ③请求使用该IP
  │←── DHCP ACK ────────  │  ④确认分配
```

## 常见故障：169.254.x.x

当客户端无法联系到DHCP服务器时（如网络故障、DHCP服务停止），Windows会自动分配一个**169.254.x.x**的APIPA地址。

| 现象 | 原因 | 解决 |
|:----:|:----:|:------|
| IP显示169.254.x.x | DHCP服务器不可达 | 检查网线、重启DHCP服务、检查网络连通性 |
| 提示"IP地址冲突" | 局域网内有重复IP | 使用DHCP自动分配、检查手动配置 |

> **故障排查口诀**："IP变成169.254，DHCP没找到"。

> **考试重点**：DHCP自动分配IP等参数，169.254.x.x是DHCP失败的典型表现。',
'DHCP服务器自动分配IP地址、子网掩码、默认网关、DNS等参数。四步过程：DORA（发现→提供→请求→确认）。IP为169.254.x.x表示DHCP获取失败。',
5, '单元3 管理局域网', '任务3 创建DNS和DHCP服务器', 1111,
'169.254.x.x记法："16(9)2(5)4=DHCP失(4)败"—看到169.254就知道DHCP出问题了\n\nDORA口诀：发现→提供→请求→确认',
'【必考】①DHCP自动分配IP/掩码/网关/DNS ②DORA四步过程 ③169.254.x.x=DHCP失败 ④DHCP避免IP冲突',
1,
'["DHCP服务器","自动分配IP","169.254.x.x","DORA","APIPA"]',
'["TCP/IP协议与IP地址","DHCP"]',
'[
  {"type":"choice","question":"当计算机的IP地址显示为169.254.x.x时，通常表示什么？","options":["网络正常","DHCP服务器获取IP失败","DNS解析错误","网卡损坏"],"answer":"B","explanation":"169.254.x.x是APIPA自动私有地址，表示DHCP服务器不可达，Windows自动分配了该地址。"},
  {"type":"choice","question":"DHCP服务器的核心作用是什么？","options":["解析域名到IP","自动分配IP地址等网络参数","加密网络通信","路由数据包"],"answer":"B","explanation":"DHCP服务器自动为网络中的客户端分配IP地址、子网掩码、默认网关和DNS等参数。"},
  {"type":"judge","question":"DHCP服务器可以避免局域网内IP地址冲突。","answer":"T","explanation":"DHCP服务器维护已分配IP的记录，不会重复分配同一IP给不同设备，从而避免IP地址冲突。"},
  {"type":"multi","question":"DHCP服务器可以为客户端分配哪些网络参数？（多选）","options":["IP地址","子网掩码","默认网关","DNS服务器","MAC地址"],"answer":"A,B,C,D","explanation":"DHCP分配IP地址、子网掩码、默认网关和DNS服务器。MAC地址是网卡硬件地址，不由DHCP分配。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10221, 10137, 'DHCP服务器的核心功能是什么？', '自动为网络客户端分配IP地址、子网掩码、默认网关、DNS服务器等参数。四步过程：Discover→Offer→Request→ACK（DORA）。', 1, 'DEFINITION'),
(10222, 10137, 'IP地址为169.254.x.x是什么意思？如何排查？', '表示DHCP服务器不可达，Windows自动分配了APIPA地址。排查：检查网线连接、重启DHCP服务、检查DHCP服务器是否在线。', 2, 'APPLICATION'),
(10223, 10137, 'DHCP的DORA四步过程是什么？', 'D=Discover（广播找服务器）→O=Offer（服务器提供IP）→R=Request（请求使用）→A=ACK（确认分配）。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10138: Internet信息服务IIS（node=1112）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10138, 'IIS——Windows下的Web服务器',
'## 什么是IIS？

IIS（Internet Information Services）是Microsoft Windows系统自带的**Web服务器软件**，用于发布网站和Web应用程序。

### 一句话理解
> IIS就是把你的电脑变成"网站服务器"的Windows自带功能——别人可以通过浏览器访问你电脑上的网页。

## IIS支持的服务

| 服务 | 说明 | 默认端口 |
|:----:|:------:|:--------:|
| **HTTP/HTTPS** | 发布网站 | 80/443 |
| **FTP** | 文件传输服务 | 21 |
| **SMTP** | 简易邮件发送 | 25 |

## 常见Web服务器对比

| 服务器 | 平台 | 开源 | 适用场景 |
|:------:|:----:|:----:|:---------|
| **IIS** | Windows | 否 | 中小型企业、学校（ASP.NET网站） |
| **Apache** | 跨平台 | 是（全球第一） | Linux服务器、PHP网站 |
| **Nginx** | 跨平台 | 是 | 高并发、静态资源、反向代理 |

## IIS的实际应用

- **学校内部网站**：OA办公系统、教学管理系统
- **ASP.NET网站发布**：Windows平台下的Web应用
- **FTP文件共享**：通过IIS搭建内部FTP服务器

### 启用IIS
> 在Windows Server/Windows中：控制面板 → 启用或关闭Windows功能 → 勾选Internet Information Services。

> **考试重点**：IIS是Microsoft Windows下的Web服务器软件，用于发布网站。知道它是微软的产品即可。',
'IIS是Microsoft Windows自带的Web服务器软件，用于发布网站（HTTP/HTTPS 80/443）、FTP服务（21）和SMTP服务（25）。常见Web服务器对比：IIS（Windows/商业）、Apache（跨平台/开源）、Nginx（跨平台/高并发）。',
5, '单元3 管理局域网', '任务4 配置Internet信息服务', 1112,
'IIS记法："IIS=I Internet Services=微软的Web服务器"\n\n"要发网站用IIS（微软）、用PHP用Apache（开源）、要高并发用Nginx"',
'【必考】①IIS是Microsoft的Web服务器软件 ②IIS可发布HTTP/HTTPS/FTP服务 ③常见Web服务器：IIS/Apache/Nginx',
1,
'["IIS","Internet信息服务","Web服务器","Windows Server","网站发布"]',
'["Internet信息服务","IIS"]',
'[
  {"type":"choice","question":"IIS是哪个公司的产品？","options":["Google","Microsoft","IBM","Oracle"],"answer":"B","explanation":"IIS（Internet Information Services）是Microsoft公司的Web服务器软件，随Windows系统提供。"},
  {"type":"choice","question":"IIS默认用于发布Web网站的端口是？","options":["21","25","80","110"],"answer":"C","explanation":"IIS默认HTTP端口80用于发布网站。21是FTP、25是SMTP、110是POP3。"},
  {"type":"judge","question":"IIS不仅支持Web服务，还支持FTP和SMTP服务。","answer":"T","explanation":"IIS集成多种服务：HTTP/HTTPS（Web网站）、FTP（文件传输）和SMTP（邮件发送）。"},
  {"type":"multi","question":"以下哪些是常见的Web服务器软件？（多选）","options":["IIS","Apache","Nginx","MySQL","Tomcat"],"answer":"A,B,C,E","explanation":"常见的Web服务器：IIS（微软）、Apache（开源第一）、Nginx（高并发）、Tomcat（Java）。MySQL是数据库，不是Web服务器。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10224, 10138, '什么是IIS？它可以提供哪些服务？', 'IIS（Internet Information Services）是微软Windows自带的Web服务器软件。支持HTTP/HTTPS（80/443）、FTP（21）、SMTP（25）等服务。', 1, 'DEFINITION'),
(10225, 10138, 'IIS、Apache和Nginx有什么区别？', 'IIS：Windows平台、商业软件、适合ASP.NET。Apache：跨平台、开源、全球使用最广、适合PHP。Nginx：跨平台、开源、高并发能力强。', 2, 'COMPARISON'),
(10226, 10138, 'IIS主要用于什么场景？', '用于在Windows服务器上发布网站和Web应用程序。常见场景：学校内部OA系统、教学管理系统、企业门户网站。', 3, 'APPLICATION');

-- ============================================================
-- 文章10139: ping命令（node=1113）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10139, 'ping命令——网络连通性的听诊器',
'## ping命令的功能

ping是**最常用的网络诊断命令**，用于测试本机与目标主机之间的网络连通性。

### 一句话理解
> ping就像"你喊一声看有没有人应"——你发一个数据包过去，对方回一个包过来，就说明网络通了。

## 工作原理

ping基于**ICMP**（Internet Control Message Protocol，网际控制报文协议）：

```
本机 ── ICMP Echo Request ──→ 目标主机
本机 ←─ ICMP Echo Reply ──── 目标主机
```

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `ping 192.168.1.1` | 测试到网关的连通性 |
| `ping www.baidu.com` | 测试到外网的连通性（还能检测DNS是否解析正常） |
| `ping 127.0.0.1` | 测试本机TCP/IP协议栈是否正常 |
| `ping -t 192.168.1.1` | 持续ping（按Ctrl+C停止） |
| `ping -n 10 192.168.1.1` | 发送10个ping包后停止 |

## 结果解读

| 输出 | 含义 |
|:----:|:------|
| `来自 192.168.1.1 的回复: 字节=32 时间<1ms TTL=64` | **网络通** ✓ |
| `请求超时。` | **网络不通** ✗ |
| `来自 192.168.1.1 的回复: 无法访问目标主机` | **路由不可达** |
| `找不到主机` | **DNS解析失败** |

## 故障排查顺序（由内到外）

| 步骤 | 命令 | 排查内容 |
|:----:|:----:|:---------|
| **①** | `ping 127.0.0.1` | 本机TCP/IP协议栈是否正常 |
| **②** | `ping 本机IP` | 网卡和IP配置是否正确 |
| **③** | `ping 网关IP` | 局域网连通性（网线/交换机） |
| **④** | `ping 外网域名` | Internet连通性 + DNS解析 |

### 排查口诀
> **"先ping自己再ping网关，最后ping百度看外网。"**

> **对口升学必考**：ping是最常用的网络诊断命令，基于ICMP协议。故障排查按"本机→网关→外网"的顺序进行。',
'ping基于ICMP协议测试网络连通性。常用用法：ping 127.0.0.1（测本机）、ping 网关（测局域网）、ping 域名（测外网+DNS）。故障排查顺序：本机→网关→外网。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1113,
'ping排查口诀："先ping自己127.0.0.1，再ping网关看局域网，最后ping百度测外网"\n\n-ping结果：有回复=通，超时=不通',
'【必考】①ping基于ICMP协议 ②ping 127.0.0.1测本机协议栈 ③故障排查顺序（本机→网关→外网）④-t持续ping、-n指定次数',
2,
'["ping","网络诊断","ICMP","连通性测试","网络命令"]',
'["常用网络命令","ping"]',
'[
  {"type":"choice","question":"ping命令基于哪个协议工作？","options":["TCP","UDP","ICMP","HTTP"],"answer":"C","explanation":"ping基于ICMP（网际控制报文协议），通过发送Echo Request和接收Echo Reply测试连通性。"},
  {"type":"choice","question":"网络故障排查时，ping命令的第一步应该做什么？","options":["ping百度","ping网关","ping 127.0.0.1","ping 114.114.114.114"],"answer":"C","explanation":"故障排查由内到外：第一步ping 127.0.0.1测试本机TCP/IP协议栈是否正常。"},
  {"type":"judge","question":"ping命令用于测试网络连通性，能收到回复说明网络是通的。","answer":"T","explanation":"ping通=本机到目标主机之间网络连通。ping不通可能的原因：网络断开、防火墙拦截、目标主机未开机。"},
  {"type":"multi","question":"以下关于ping命令的说法，正确的有哪些？（多选）","options":["基于ICMP协议","-t参数可以持续ping","-n参数指定发送次数","ping不通一定是网线断了","127.0.0.1是本机地址"],"answer":"A,B,C,E","explanation":"ping基于ICMP、-t持续、-n指定次数、127.0.0.1本机均正确。ping不通有多种原因（防火墙/未开机等），不一定是网线断了。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10227, 10139, 'ping命令的功能和原理是什么？', 'ping测试网络连通性，基于ICMP协议。本机发Echo Request→目标回Echo Reply→收到回复即通。', 1, 'DEFINITION'),
(10228, 10139, '网络故障排查时ping命令应如何按顺序使用？', '①ping 127.0.0.1（测本机协议栈）②ping本机IP（测网卡）③ping网关（测局域网）④ping百度（测外网+DNS）。由内到外逐级排查。', 2, 'PROCEDURE'),
(10229, 10139, 'ping命令的常用参数有哪些？', 'ping -t（持续ping，Ctrl+C停止）、ping -n 10（发送10个包）、ping -l 1000（设置包大小）。', 3, 'APPLICATION');

-- ============================================================
-- 文章10140: ipconfig命令（node=1114）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10140, 'ipconfig命令——查看网络配置的利器',
'## ipconfig命令的功能

ipconfig是Windows系统中**查看IP配置信息**的网络命令。

### 一句话理解
> ipconfig就像"查看你的网络身份证"——你的IP地址、子网掩码、网关是多少，一看便知。

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `ipconfig` | 查看基本IP配置（IP地址、子网掩码、默认网关） |
| `ipconfig /all` | 查看**详细配置**（含MAC地址、DNS、DHCP信息、主机名） |
| `ipconfig /release` | 释放当前DHCP获得的IP地址 |
| `ipconfig /renew` | 重新向DHCP服务器申请IP地址 |
| `ipconfig /flushdns` | 清除DNS缓存（解决域名解析异常） |

## 输出解读

### ipconfig（基本）
```
以太网适配器 本地连接:
   IPv4 地址 . . . . . . . . . . . : 192.168.1.100   ← 本机IP
   子网掩码 . . . . . . . . . . . . : 255.255.255.0   ← 子网掩码
   默认网关 . . . . . . . . . . . . : 192.168.1.1     ← 路由器IP
```

### ipconfig /all（详细）
```
   物理地址 . . . . . . . . . . . . : 00-1A-2B-3C-4D-5E  ← MAC地址
   DHCP 已启用 . . . . . . . . . . : 是                ← 是否使用DHCP
   DHCP 服务器 . . . . . . . . . . : 192.168.1.1       ← DHCP服务器
   DNS 服务器 . . . . . . . . . . : 114.114.114.114   ← DNS服务器
```

## 故障诊断

| 现象 | 含义 |
|:----:|:------|
| IP为**169.254.x.x** | DHCP获取失败，自动分配了APIPA地址 |
| 显示**"媒体已断开"** | 网线未插好或网卡被禁用 |
| DNS缓存有问题 | 能ping通IP但打不开网页 → 用`ipconfig /flushdns`清除缓存 |

### 实用场景
> ①查看本机IP地址和MAC地址 → `ipconfig /all`
> ②修改了DHCP配置后让电脑重新获取IP → 先`ipconfig /release`再`ipconfig /renew`
> ③DNS解析异常 → `ipconfig /flushdns`

> **对口升学考点**：ipconfig查看IP配置、/all查看详细信息、/release释放IP、/renew重新获取、/flushdns刷新DNS缓存。',
'ipconfig查看本机IP配置。常用参数：/all（详细信息）、/release（释放IP）、/renew（重新获取）、/flushdns（清除DNS缓存）。169.254.x.x=DHCP异常，"媒体已断开"=网线未插好。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1114,
'ipconfig参数记法："/all=全部信息，/release=释放IP，/renew=重新获取，/flushdns=刷DNS"',
'【必考】①ipconfig查看IP配置 ②/all查看MAC地址/DNS等详细信息 ③/release释放+/renew重新获取 ④/flushdns清除DNS缓存 ⑤169.254.x.x表示DHCP失败',
2,
'["ipconfig","网络配置","IP查看","DHCP","DNS缓存","网络命令"]',
'["常用网络命令","ipconfig"]',
'[
  {"type":"choice","question":"在ipconfig命令中，查看详细信息包含MAC地址和DNS应使用哪个参数？","options":["/release","/renew","/all","/flushdns"],"answer":"C","explanation":"ipconfig /all显示所有网络适配器的详细信息，包括MAC地址、DHCP服务器、DNS服务器等。"},
  {"type":"choice","question":"清除DNS缓存应使用哪个命令？","options":["ipconfig /release","ipconfig /flushdns","ipconfig /renew","ipconfig /all"],"answer":"B","explanation":"ipconfig /flushdns清除本地DNS缓存，适用于DNS解析异常时（能ping通IP但打不开网页）。"},
  {"type":"judge","question":"ipconfig /renew用于释放当前DHCP获取的IP地址。","answer":"F","explanation":"ipconfig /release才是释放IP，/renew是重新向DHCP服务器申请IP。两者常配合使用：先release释放，再renew重新获取。"},
  {"type":"multi","question":"以下哪些是ipconfig命令的常用参数？（多选）","options":["/all","/release","/renew","/flushdns","/tracert"],"answer":"A,B,C,D","explanation":"ipconfig常用参数：/all（详细信息）、/release（释放IP）、/renew（重新获取）、/flushdns（清除DNS缓存）。/tracert不是ipconfig的参数。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10230, 10140, 'ipconfig命令常用参数有哪些？', 'ipconfig（基本IP信息）、ipconfig /all（详细信息含MAC/DNS）、ipconfig /release（释放IP）、ipconfig /renew（重新获取）、ipconfig /flushdns（清除DNS缓存）。', 1, 'DEFINITION'),
(10231, 10140, 'ipconfig /all可以查看到哪些信息？', '本机所有网络适配器的详细配置：IP地址、子网掩码、默认网关、MAC地址（物理地址）、DNS服务器、DHCP状态等。', 2, 'APPLICATION'),
(10232, 10140, 'ipconfig的/release和/renew在什么场景下使用？', '修改了DHCP服务器配置后，让客户端重新获取IP：先运行ipconfig /release释放旧IP，再运行ipconfig /renew向DHCP申请新IP。', 3, 'PROCEDURE');

-- ============================================================
-- 文章10141: tracert命令（node=1115）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10141, 'tracert命令——追踪路由的导航图',
'## tracert命令的功能

tracert（Trace Route）用于**追踪从本机到目标主机所经过的每一跳路由器**——也就是你的数据包走了哪些"中转站"才到达目的地。

### 一句话理解
> tracert就像"查快递物流"——从发货到收货经过哪些中转站、每站花了多少时间，一目了然。

## 工作原理

利用IP数据包中的**TTL**（Time To Live，生存时间）字段：

- TTL初始值为1（第一跳路由器收到后TTL-1=0，回送超时信息→记录第一跳）
- TTL逐跳递增（2、3、4...），直到到达目标
- 每经过一个路由器TTL减1，TTL归零时路由器返回ICMP超时信息

```
本机 → 路由器A（TTL=1超时→记录IP）→ 路由器B（TTL=2超时→记录IP）→ ... → 目标
```

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `tracert www.baidu.com` | 追踪到百度服务器的路由路径 |
| `tracert 192.168.1.1` | 追踪到网关（通常只有1跳） |
| `tracert -d www.baidu.com` | 不解析IP为主机名（速度更快） |

## 输出解读

```
通过最多 30 个跃点跟踪到 www.baidu.com [110.242.68.66]:

  1    <1 ms    <1 ms    <1 ms  192.168.1.1        ← 第1跳：本地网关（路由器）
  2     2 ms     1 ms     2 ms  10.0.0.1           ← 第2跳：ISP接入点
  3     5 ms     4 ms     5 ms  61.148.3.45        ← 第3跳：城市骨干网
  4     *        *        *     请求超时。           ← 第4跳：某路由器不回应（常见）
  ...
 10    30 ms    31 ms    30 ms  110.242.68.66      ← 最终跳：目标服务器
```

### 输出字段含义
| 字段 | 含义 |
|:----:|:------|
| 第1列 | 跳数（从1到30） |
| 第2~4列 | 3次探测的往返时间（ms） |
| 第5列 | 该跳路由器的IP地址或主机名 |

## 故障诊断

| 现象 | 可能原因 |
|:----:|:---------|
| 某跳之后全部显示`* * * 请求超时` | 该处路由器不响应ICMP（或链路中断） |
| 跳数超过20跳仍未到达 | 目标太远，或有路由环路 |
| 某些路由器显示`*`但后续跳正常 | 个别路由器禁ping，不影响连通性 |

> **考试重点**：tracert用于追踪路由路径，原理基于TTL（生存时间）字段。最大跳数为30。',
'tracert追踪从本机到目标所经过的所有路由器。原理：利用TTL（生存时间）字段逐跳递增探测。最大30跳。每跳显示3次往返时间和路由器IP。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1115,
'tracert记法："tracert=Trace Route=追踪路径"\n\n原理记法："TTL=1去第一跳，TTL=2去第二跳...逐跳递增"\n最大跳数30（默认）',
'【必考】①tracert的功能（追踪路由路径）②基于TTL原理 ③最大30跳 ④输出结果解读（跳数+时间+IP）',
2,
'["tracert","路由追踪","TTL","网络诊断","网络命令","路由路径"]',
'["常用网络命令","tracert"]',
'[
  {"type":"choice","question":"tracert命令的主要功能是什么？","options":["测试网络连通性","查看IP配置","追踪本机到目标的路由路径","查看网络连接状态"],"answer":"C","explanation":"tracert追踪数据包从本机到目标主机经过的所有路由器（每一跳）。ping测试连通性，tracert查路径。"},
  {"type":"choice","question":"tracert命令利用IP数据包中的哪个字段来逐跳探测？","options":["TTL（生存时间）","IP标识符","协议类型","校验和"],"answer":"A","explanation":"tracert利用TTL字段：TTL=1时第一跳路由器返回超时，TTL=2时第二跳返回……逐跳递增直到目标。"},
  {"type":"judge","question":"tracert命令的默认最大跳数是30跳。","answer":"T","explanation":"tracert默认最多追踪30跳。如果超过30跳仍未到达目标，则停止追踪并提示超出最大跳数。"},
  {"type":"multi","question":"以下哪些是Windows系统自带的网络诊断命令？（多选）","options":["ping","ipconfig","tracert","netstat","msconfig"],"answer":"A,B,C,D","explanation":"ping、ipconfig、tracert、netstat都是Windows自带的网络诊断命令。msconfig是系统配置工具，不是网络命令。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10233, 10141, 'tracert命令的功能和原理是什么？', 'tracert追踪从本机到目标主机经过的每一跳路由器。原理：利用TTL（生存时间）字段逐跳递增，TTL归零时路由器返回信息，从而记录该跳IP。', 1, 'DEFINITION'),
(10234, 10141, '如何解读tracert的输出结果？', '每行显示一跳：左起跳数→3次往返时间（ms）→路由器IP。如"1 <1ms <1ms <1ms 192.168.1.1"表示第一跳是本地网关。*表示该跳路由器不响应。', 2, 'APPLICATION'),
(10235, 10141, 'tracert和ping有什么区别？', 'ping：测试网络连通性（通不通）。tracert：追踪路由路径（怎么走）。ping查"能不能到"，tracert查"怎么到"。', 3, 'COMPARISON');

-- ============================================================
-- 文章10142: netstat命令（node=1116）
-- ============================================================
INSERT IGNORE INTO knowledge_articles (id, title, content_md, excerpt, subject_id, chapter, task, node_id, memory_tips, exam_focus, difficulty, tags, syllabus_refs, quiz, status) VALUES
(10142, 'netstat命令——网络连接的监控器',
'## netstat命令的功能

netstat（Network Statistics）用于显示本机的**网络连接状态**、监听端口、路由表等网络统计信息。

### 一句话理解
> netstat就像"网络连接的全景监控"——你的电脑正在和谁通信、哪些程序在监听端口、有没有可疑连接，一目了然。

## 常用用法

| 命令 | 作用 |
|:----:|:------|
| `netstat` | 显示所有活动TCP连接 |
| `netstat -a` | 显示**所有连接和监听端口** |
| `netstat -n` | 以数字形式显示地址和端口（不解析名称） |
| `netstat -an` | **最常用组合**——-a查看所有，-n数字显示 |
| `netstat -o` | 显示每个连接对应的进程PID |
| `netstat -b` | 显示进程程序名（需要管理员权限） |

## 输出解读

```
活动连接

  协议    本地地址                外部地址                状态
  TCP    192.168.1.100:49723     110.242.68.66:443      ESTABLISHED
  TCP    0.0.0.0:80              0.0.0.0:0              LISTENING
  TCP    127.0.0.1:3306          0.0.0.0:0              LISTENING
  UDP    0.0.0.0:53              *:*                    （UDP无状态）
```

### 常见连接状态

| 状态 | 含义 |
|:----:|:------|
| **ESTABLISHED** | 已建立连接——正在通信（正常上网时的状态） |
| **LISTENING** | 正在监听——等待别人来连接（服务器在等待请求） |
| **TIME_WAIT** | 连接已关闭，等待清理（短暂出现） |
| **CLOSE_WAIT** | 对方已关闭连接，本机等待关闭 |

## 实用场景

### ① 排查端口占用
> 启动服务时提示"端口被占用"：
> ```
> netstat -ano | findstr :8080
> ```
> 查看占用8080端口的进程PID → 在任务管理器中结束该进程。

### ② 检测木马/异常连接
> 运行 `netstat -an` 查看当前所有连接：
> - 看到大量不认识的IP在连接→可能有木马
> - 看到异常端口在LISTENING→可能有后门

### ③ 查看本机开启的服务
> `netstat -an` 显示LISTENING状态的端口就是本机正在提供的服务。

> **对口升学考点**：netstat查看本机网络连接和端口状态。-an最常用（查看所有连接+数字显示）。ESTABLISHED=已连接，LISTENING=正在监听。',
'netstat显示本机网络连接和端口状态。常用参数：-a（所有连接）、-n（数字显示）、-an（最常用组合）、-o（显示PID）。ESTABLISHED（已连接）、LISTENING（正在监听）。',
5, '单元3 管理局域网', '任务5 应用网络命令', 1116,
'netstat记法："netstat=网络状态"\n-an组合="看所有连接的不带名字（数字显示）"\n\n状态记忆：ESTABLISHED=已经连上了，LISTENING=等着人来连',
'【必考】①netstat查看网络连接状态 ②-an是最常用组合参数 ③ESTABLISHED（已连接）④LISTENING（正在监听）⑤排查端口占用方法',
2,
'["netstat","网络连接","端口状态","ESTABLISHED","LISTENING","网络命令"]',
'["常用网络命令","netstat"]',
'[
  {"type":"choice","question":"netstat命令中，」LISTENING「状态表示什么？","options":["已建立连接","正在监听等待连接","连接已关闭","数据正在传输"],"answer":"B","explanation":"LISTENING表示该端口正在监听中，等待客户端发起连接。如Web服务器在80端口LISTENING等待浏览器访问。"},
  {"type":"choice","question":"要查看本机所有网络连接和监听端口，且以数字显示，最常用的命令是什么？","options":["netstat","netstat -a","netstat -an","netstat -b"],"answer":"C","explanation":"netstat -an是最常用的组合——-a显示所有连接和监听端口，-n用数字显示地址和端口（不解析主机名）。"},
  {"type":"judge","question":"ESTABLISHED状态表示该端口正在监听，等待客户端连接。","answer":"F","explanation":"ESTABLISHED表示已建立连接，正在通信中。LISTENING才是正在监听等待连接的状态。"},
  {"type":"multi","question":"以下哪些是netstat命令的常用参数？（多选）","options":["-a","-n","-o","-b","-t"],"answer":"A,B,C,D","explanation":"netstat -a（所有连接）、-n（数字显示）、-o（显示PID）、-b（显示程序名）都是常用参数。-t不是netstat的参数。"}
]',
'PUBLISHED');

INSERT IGNORE INTO knowledge_flashcards (id, article_id, front_text, back_text, sort_order, card_type) VALUES
(10236, 10142, 'netstat命令的功能和常用参数？', 'netstat显示本机网络连接状态和端口信息。常用：-a（全部连接）、-n（数字显示）、-an（最常用）、-o（显示PID）、-b（显示程序名需管理员）。', 1, 'DEFINITION'),
(10237, 10142, 'ESTABLISHED和LISTENING状态分别代表什么？', 'ESTABLISHED=已建立连接，正在通信（如浏览器正在访问网站）。LISTENING=正在监听端口，等待连接（如Web服务器等待浏览器访问）。', 2, 'DEFINITION'),
(10238, 10142, '如何用netstat排查端口占用问题？', '运行netstat -ano | findstr :端口号（如8080）。找到占用该端口的PID，在任务管理器中结束对应的进程。', 3, 'APPLICATION');

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '[v172] 单元3 知识库文章种子完成：共9篇文章，约27张记忆卡片。' AS result;
