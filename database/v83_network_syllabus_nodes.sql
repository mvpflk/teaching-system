-- ============================================================================
-- v83: 网络应用基础 — 知识节点树（subject_id=5）
-- 教材: 《计算机网络应用基础》杨泉波/程弋可/李梁雅, 高教社 2021
-- 结构: 6单元 → 27任务 → 知识点(对照考纲提取)
-- ============================================================================

-- ═══ Level 2: 单元 ═══
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (5, 5, 2, '单元1 初识计算机网络', 1);
SET @u1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (5, 5, 2, '单元2 组建局域网', 2);
SET @u2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (5, 5, 2, '单元3 管理局域网', 3);
SET @u3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (5, 5, 2, '单元4 畅游Internet', 4);
SET @u4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (5, 5, 2, '单元5 运用网络安全技术', 5);
SET @u5 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (5, 5, 2, '单元6 设计制作网页', 6);
SET @u6 = LAST_INSERT_ID();

-- ═══ Level 3: 任务 — 单元1 初识计算机网络 ═══
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u1, 5, 3, '任务1 走进计算机网络', 1);
SET @t1_1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u1, 5, 3, '任务2 认识数据通信', 2);
SET @t1_2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u1, 5, 3, '任务3 剖析计算机网络体系结构', 3);
SET @t1_3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u1, 5, 3, '任务4 认识传输介质', 4);
SET @t1_4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u1, 5, 3, '任务5 认识网络接口及网络设备', 5);
SET @t1_5 = LAST_INSERT_ID();

-- ═══ Level 3: 任务 — 单元2 组建局域网 ═══
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u2, 5, 3, '任务1 组建典型局域网', 1);
SET @t2_1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u2, 5, 3, '任务2 配置TCP/IP协议', 2);
SET @t2_2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u2, 5, 3, '任务3 组建虚拟局域网', 3);
SET @t2_3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u2, 5, 3, '任务4 组建无线局域网', 4);
SET @t2_4 = LAST_INSERT_ID();

-- ═══ Level 3: 任务 — 单元3 管理局域网 ═══
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u3, 5, 3, '任务1 使用网络操作系统', 1);
SET @t3_1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u3, 5, 3, '任务2 创建和管理域', 2);
SET @t3_2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u3, 5, 3, '任务3 创建DNS和DHCP服务器', 3);
SET @t3_3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u3, 5, 3, '任务4 配置Internet信息服务', 4);
SET @t3_4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u3, 5, 3, '任务5 应用网络命令', 5);
SET @t3_5 = LAST_INSERT_ID();

-- ═══ Level 3: 任务 — 单元4 畅游Internet ═══
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u4, 5, 3, '任务1 接入Internet', 1);
SET @t4_1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u4, 5, 3, '任务2 应用WWW服务', 2);
SET @t4_2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u4, 5, 3, '任务3 应用FTP服务', 3);
SET @t4_3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u4, 5, 3, '任务4 应用Email服务', 4);
SET @t4_4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u4, 5, 3, '任务5 应用远程登录服务', 5);
SET @t4_5 = LAST_INSERT_ID();

-- ═══ Level 3: 任务 — 单元5 运用网络安全技术 ═══
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u5, 5, 3, '任务1 认识加密和认证技术', 1);
SET @t5_1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u5, 5, 3, '任务2 防治计算机病毒', 2);
SET @t5_2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u5, 5, 3, '任务3 使用防火墙', 3);
SET @t5_3 = LAST_INSERT_ID();

-- ═══ Level 3: 任务 — 单元6 设计制作网页 ═══
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u6, 5, 3, '任务1 创建网站', 1);
SET @t6_1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u6, 5, 3, '任务2 设计简单网页', 2);
SET @t6_2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u6, 5, 3, '任务3 建立列表和超链接', 3);
SET @t6_3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u6, 5, 3, '任务4 运用CSS', 4);
SET @t6_4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@u6, 5, 3, '任务5 使用表单', 5);
SET @t6_5 = LAST_INSERT_ID();

-- ═══════════════════════════════════════════════════════════════
-- Level 4: 知识点（仅提取考纲中明确要求的内容, 不跨教材不虚构）
-- ═══════════════════════════════════════════════════════════════

-- ── 单元1 > 任务1: 走进计算机网络 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_1, 5, 4, '计算机网络的定义与功能', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_1, 5, 4, '计算机网络的组成(通信子网+资源子网)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_1, 5, 4, '计算机网络的分类(按覆盖范围/传输介质/拓扑)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_1, 5, 4, '网络拓扑结构(总线型/星型/环型/树型/网状)', 4);

-- ── 单元1 > 任务2: 认识数据通信 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_2, 5, 4, '数据通信基本概念', 1);

-- ── 单元1 > 任务3: 剖析计算机网络体系结构 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_3, 5, 4, 'OSI七层模型及各层功能', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_3, 5, 4, 'TCP/IP四层模型', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_3, 5, 4, 'OSI与TCP/IP对应关系', 3);

-- ── 单元1 > 任务4: 认识传输介质 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_4, 5, 4, '有线传输介质(双绞线/同轴电缆/光纤)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_4, 5, 4, '无线传输介质(无线电波/红外线/蓝牙/Wi-Fi)', 2);

-- ── 单元1 > 任务5: 认识网络接口及网络设备 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_5, 5, 4, '网卡(NIC)与MAC地址', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_5, 5, 4, '集线器(Hub)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_5, 5, 4, '交换机(Switch)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_5, 5, 4, '路由器(Router)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t1_5, 5, 4, '调制解调器(Modem)', 5);

-- ── 单元2 > 任务1: 组建典型局域网 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_1, 5, 4, '局域网的概念与组成', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_1, 5, 4, '以太网标准IEEE 802.3', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_1, 5, 4, 'MAC地址格式(48位/12位十六进制)', 3);

-- ── 单元2 > 任务2: 配置TCP/IP协议 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'IP地址的概念与点分十进制表示', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'IP地址分类(A/B/C类)与默认子网掩码', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, '私有IP地址范围', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, '子网掩码的作用(区分网络位和主机位)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'IPv6概念(128位/冒号十六进制)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'TCP协议(面向连接/可靠传输)', 6);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'UDP协议(无连接/高效)', 7);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'HTTP/HTTPS协议与端口(80/443)', 8);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'FTP协议与端口(21)', 9);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'SMTP/POP3邮件协议与端口(25/110)', 10);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'DNS域名解析与端口(53)', 11);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_2, 5, 4, 'DHCP动态主机配置', 12);

-- ── 单元2 > 任务3: 组建虚拟局域网 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_3, 5, 4, 'VLAN的概念与作用(隔离广播/提高安全性)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_3, 5, 4, 'VLAN划分方式(基于端口/基于MAC)', 2);

-- ── 单元2 > 任务4: 组建无线局域网 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t2_4, 5, 4, '无线局域网基础(Wi-Fi标准)', 1);

-- ── 单元3 > 任务1: 使用网络操作系统 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_1, 5, 4, '网络操作系统基本概念', 1);

-- ── 单元3 > 任务2: 创建和管理域 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_2, 5, 4, '域的基本概念', 1);

-- ── 单元3 > 任务3: 创建DNS和DHCP服务器 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_3, 5, 4, 'DNS服务器的功能(域名→IP解析)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_3, 5, 4, 'DHCP服务器的功能(自动分配IP)', 2);

-- ── 单元3 > 任务4: 配置Internet信息服务 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_4, 5, 4, 'Internet信息服务(IIS)基本概念', 1);

-- ── 单元3 > 任务5: 应用网络命令 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_5, 5, 4, 'ping命令(测试网络连通性)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_5, 5, 4, 'ipconfig命令(查看IP配置)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_5, 5, 4, 'tracert命令(追踪路由路径)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t3_5, 5, 4, 'netstat命令(查看网络连接状态)', 4);

-- ── 单元4 > 任务1: 接入Internet ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_1, 5, 4, 'Internet的概念与发展(ARPANET→全球互联网)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_1, 5, 4, 'Internet接入方式(ADSL/光纤/LAN/4G/5G/Wi-Fi)', 2);

-- ── 单元4 > 任务2: 应用WWW服务 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_2, 5, 4, 'WWW万维网概念', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_2, 5, 4, 'URL统一资源定位符(协议://域名/路径)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_2, 5, 4, '浏览器的基本使用', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_2, 5, 4, '搜索引擎的使用', 4);

-- ── 单元4 > 任务3: 应用FTP服务 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_3, 5, 4, 'FTP文件传输协议的功能与使用', 1);

-- ── 单元4 > 任务4: 应用Email服务 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_4, 5, 4, '电子邮件系统(SMTP发/POP3收)', 1);

-- ── 单元4 > 任务5: 应用远程登录服务 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t4_5, 5, 4, '远程登录(Telnet/SSH)基本概念', 1);

-- ── 单元5 > 任务1: 认识加密和认证技术 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t5_1, 5, 4, '网络安全威胁(病毒/木马/DDoS/SQL注入/XSS)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t5_1, 5, 4, '对称加密(DES/AES)与非对称加密(RSA)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t5_1, 5, 4, '数字证书与数字签名', 3);

-- ── 单元5 > 任务2: 防治计算机病毒 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t5_2, 5, 4, '计算机病毒的概念与特征', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t5_2, 5, 4, '杀毒软件的安装与配置', 2);

-- ── 单元5 > 任务3: 使用防火墙 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t5_3, 5, 4, '防火墙的概念与类型(包过滤/应用代理)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t5_3, 5, 4, '安全上网习惯(不打开可疑链接/定期更换密码/HTTPS)', 2);

-- ── 单元6 > 任务1: 创建网站 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_1, 5, 4, 'HTML基本结构(html/head/title/body)', 1);

-- ── 单元6 > 任务2: 设计简单网页 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_2, 5, 4, '标题标签(h1~h6)与段落标签(p)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_2, 5, 4, '换行标签(br)与水平线(hr)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_2, 5, 4, '文本格式标签(b/i/u/font)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_2, 5, 4, '图片标签(img)及属性(src/alt)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_2, 5, 4, '表格标签(table/tr/td/th)及属性(border/width/align)', 5);

-- ── 单元6 > 任务3: 建立列表和超链接 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_3, 5, 4, '有序列表(ol/li)与无序列表(ul/li)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_3, 5, 4, '超链接(a href)类型(内部/外部/锚点/邮件)', 2);

-- ── 单元6 > 任务4: 运用CSS ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_4, 5, 4, 'CSS引入方式(行内样式/内部样式表/外部样式表)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_4, 5, 4, '常用CSS属性(color/font-size/background-color/margin/padding/border)', 2);

-- ── 单元6 > 任务5: 使用表单 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_5, 5, 4, '表单标签(form)与input类型(text/password/radio/checkbox/submit/reset)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_5, 5, 4, '多行文本框(textarea)与下拉列表(select)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@t6_5, 5, 4, '多媒体标签(audio/video)', 3);

-- 打印所有新插入节点的ID清单
SELECT '=== 网络应用基础 节点ID清单 ===' AS '';
SELECT id, parent_id, level, name FROM knowledge_nodes WHERE subject_id = 5 ORDER BY level, id;
