-- v111: 实训中心 Phase 1 — 25个网络CMD预置任务种子数据
-- 依赖：v110_training_center.sql（category 字段）、v83_network_syllabus_nodes.sql（知识节点）
-- 覆盖：单元2任务2（TCP/IP配置）+ 单元3任务5（应用网络命令）+ 单元4任务5（远程登录）

-- Feature 开关
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('feature.training_center', 'true', '实训中心模块开关（包含 Windows + 网络实训）')
ON DUPLICATE KEY UPDATE setting_value = 'true';

-- ══════════════════════════════════════════════════════════════════
-- 公共 networkConfig：所有网络任务的基础虚拟网络拓扑
-- localIP=192.168.1.100, gateway=192.168.1.1, dns=8.8.8.8
-- remoteHosts: 192.168.1.1(在线), www.baidu.com(在线→220.181.38.148), 10.0.0.99(离线), 8.8.8.8(在线)
-- ══════════════════════════════════════════════════════════════════

-- ===== 单元3任务5：应用网络命令（ping × 5）=====

-- 1. ping-测试默认网关连通性
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ping-测试默认网关连通性', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1113, '{"title":"测试默认网关连通性","description":"使用ping命令测试与默认网关192.168.1.1的连通性，判断本机到网关的网络是否正常。","difficulty":1,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online","10.0.0.99":"offline","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"ping 默认网关","hint":"输入 ping 192.168.1.1 后按回车","validate":{"event":"cmdExecute","target":"ping"}}]}', 'practice', 1, 90, 'network', 1, 1);

-- 2. ping-测试百度连通性
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ping-测试百度连通性', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1113, '{"title":"测试百度连通性","description":"使用ping命令测试与百度服务器的连通性，观察域名解析和响应时间。","difficulty":1,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online","10.0.0.99":"offline","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"ping 百度域名","hint":"输入 ping www.baidu.com 后按回车","validate":{"event":"cmdExecute","target":"ping"}}]}', 'practice', 1, 90, 'network', 1, 1);

-- 3. ping-测试本地回环地址
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ping-测试本地回环地址', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1113, '{"title":"测试本地回环地址","description":"使用ping 127.0.0.1测试本机TCP/IP协议栈是否正常工作。回环地址是诊断网络问题的第一步。","difficulty":1,"timeLimit":60,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"127.0.0.1":"online","192.168.1.1":"online","www.baidu.com":"online","10.0.0.99":"offline","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"ping 127.0.0.1","hint":"输入 ping 127.0.0.1 后按回车","validate":{"event":"cmdExecute","target":"ping"}}]}', 'practice', 1, 60, 'network', 1, 1);

-- 4. ping-测试不存在的主机
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ping-测试不存在的主机', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1113, '{"title":"测试不存在的主机","description":"使用ping命令测试一个不存在的IP地址10.0.0.99，观察请求超时的现象，理解ping超时的含义。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online","10.0.0.99":"offline","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"ping 不存在的地址","hint":"输入 ping 10.0.0.99 后按回车，观察超时提示","validate":{"event":"cmdExecute","target":"ping"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 5. ping-连续测试（-t 参数）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ping-连续测试(-t参数)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1113, '{"title":"ping连续测试(-t参数)","description":"使用ping -t命令对网关进行连续连通性测试。了解-t参数的作用（持续发送直到Ctrl+C停止）。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online","10.0.0.99":"offline","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"ping -t 连续测试","hint":"输入 ping -t 192.168.1.1 后按回车","validate":{"event":"cmdExecute","target":"ping"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- ===== 单元3任务5：应用网络命令（ipconfig × 5）=====

-- 6. ipconfig-查看IP配置
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ipconfig-查看IP配置', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1114, '{"title":"查看IP配置","description":"使用ipconfig命令查看本机的IPv4地址、子网掩码和默认网关等基本网络配置信息。","difficulty":1,"timeLimit":60,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 ipconfig","hint":"输入 ipconfig 后按回车，查看IP配置信息","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 1, 60, 'network', 1, 1);

-- 7. ipconfig-查看详细配置（/all）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ipconfig-查看详细配置(/all)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1114, '{"title":"查看详细IP配置(/all)","description":"使用ipconfig /all命令查看完整的网络配置信息，包括MAC地址、DHCP状态、DNS服务器等。","difficulty":1,"timeLimit":60,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 ipconfig /all","hint":"输入 ipconfig /all 后按回车，查看详细配置","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 1, 60, 'network', 1, 1);

-- 8. ipconfig-释放IP地址（/release）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ipconfig-释放IP地址(/release)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1114, '{"title":"释放IP地址(/release)","description":"使用ipconfig /release命令释放当前DHCP获取的IP地址，观察IP变为0.0.0.0的过程。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 ipconfig /release","hint":"输入 ipconfig /release 后按回车","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 9. ipconfig-重新获取IP（/renew）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ipconfig-重新获取IP(/renew)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1114, '{"title":"重新获取IP地址(/renew)","description":"使用ipconfig /renew命令向DHCP服务器重新申请IP地址，观察IP地址恢复到正常配置。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"0.0.0.0","subnetMask":"255.255.255.0","gateway":"","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 ipconfig /renew","hint":"输入 ipconfig /renew 后按回车，等待获取IP","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 10. ipconfig-清除DNS缓存（/flushdns）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('ipconfig-清除DNS缓存(/flushdns)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1114, '{"title":"清除DNS缓存(/flushdns)","description":"使用ipconfig /flushdns命令清除DNS解析缓存。当网站IP变更但本地仍解析到旧地址时，可使用此命令。","difficulty":2,"timeLimit":60,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 ipconfig /flushdns","hint":"输入 ipconfig /flushdns 后按回车","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 2, 60, 'network', 1, 1);

-- ===== 单元3任务5：应用网络命令（tracert × 5）=====

-- 11. tracert-追踪到网关
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('tracert-追踪到网关', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1115, '{"title":"追踪到网关的路由","description":"使用tracert命令追踪从本机到默认网关的路由路径，了解数据包经过的跃点。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"tracert 到网关","hint":"输入 tracert 192.168.1.1 后按回车","validate":{"event":"cmdExecute","target":"tracert"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 12. tracert-追踪到百度
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('tracert-追踪到百度', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1115, '{"title":"追踪到百度的路由","description":"使用tracert命令追踪从本机到百度服务器的完整路由路径，观察数据包经过多少跳到达目标。","difficulty":2,"timeLimit":120,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"tracert 到百度","hint":"输入 tracert www.baidu.com 后按回车","validate":{"event":"cmdExecute","target":"tracert"}}]}', 'practice', 2, 120, 'network', 1, 1);

-- 13. tracert-追踪到DNS服务器
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('tracert-追踪到DNS服务器', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1115, '{"title":"追踪到DNS服务器的路由","description":"使用tracert命令追踪到公共DNS服务器8.8.8.8的路由路径。","difficulty":3,"timeLimit":120,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"tracert 到DNS服务器","hint":"输入 tracert 8.8.8.8 后按回车","validate":{"event":"cmdExecute","target":"tracert"}}]}', 'practice', 3, 120, 'network', 1, 1);

-- 14. tracert-不解析主机名（-d）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('tracert-不解析主机名(-d)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1115, '{"title":"tracert不解析主机名(-d)","description":"使用tracert -d参数追踪路由，不将IP地址解析为主机名，加快追踪速度。","difficulty":3,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"tracert -d","hint":"输入 tracert -d 8.8.8.8 后按回车","validate":{"event":"cmdExecute","target":"tracert"}}]}', 'practice', 3, 90, 'network', 1, 1);

-- 15. tracert-指定最大跃点（-h）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('tracert-指定最大跃点(-h)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1115, '{"title":"tracert指定最大跃点(-h)","description":"使用tracert -h参数限制路由追踪的最大跃点数，控制在指定跳数内停止追踪。","difficulty":3,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"192.168.1.1":"online","www.baidu.com":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"tracert -h 3","hint":"输入 tracert -h 3 www.baidu.com 后按回车","validate":{"event":"cmdExecute","target":"tracert"}}]}', 'practice', 3, 90, 'network', 1, 1);

-- ===== 单元3任务5：应用网络命令（netstat × 5）=====

-- 16. netstat-查看活动连接
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('netstat-查看活动连接', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1116, '{"title":"查看活动网络连接","description":"使用netstat命令查看当前计算机的活动TCP连接，包括本地地址、外部地址和连接状态。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 netstat","hint":"输入 netstat 后按回车，观察活动连接","validate":{"event":"cmdExecute","target":"netstat"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 17. netstat-查看所有连接（-a）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('netstat-查看所有连接(-a)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1116, '{"title":"查看所有网络连接(-a)","description":"使用netstat -a命令查看所有TCP和UDP连接，包括LISTENING状态的端口，了解哪些服务正在监听网络请求。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"netstat -a","hint":"输入 netstat -a 后按回车","validate":{"event":"cmdExecute","target":"netstat"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 18. netstat-数字显示（-n）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('netstat-数字显示(-n)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1116, '{"title":"数字显示网络连接(-n)","description":"使用netstat -n命令以数字形式显示地址和端口号，不进行名称解析，查看更快更直接。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"netstat -n","hint":"输入 netstat -n 后按回车","validate":{"event":"cmdExecute","target":"netstat"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 19. netstat-查看监听端口（-an）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('netstat-查看监听端口(-an)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1116, '{"title":"查看所有监听端口(-an)","description":"使用netstat -an命令查看所有连接和监听端口（数字格式），这是网络排错最常用的命令组合。","difficulty":3,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"netstat -an","hint":"输入 netstat -an 后按回车，观察所有端口","validate":{"event":"cmdExecute","target":"netstat"}}]}', 'practice', 3, 90, 'network', 1, 1);

-- 20. netstat-查看进程ID（-ano）
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('netstat-查看进程ID(-ano)', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1116, '{"title":"查看连接及进程ID(-ano)","description":"使用netstat -ano命令查看所有网络连接及对应的进程PID，可用于排查哪个程序占用了某个端口。","difficulty":3,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"netstat -ano","hint":"输入 netstat -ano 后按回车，查看PID列","validate":{"event":"cmdExecute","target":"netstat"}}]}', 'practice', 3, 90, 'network', 1, 1);

-- ===== 单元4任务5：远程登录（nslookup × 2）=====

-- 21. nslookup-查询域名IP
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('nslookup-查询域名IP', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1125, '{"title":"nslookup查询域名IP","description":"使用nslookup命令查询百度的IP地址，了解DNS域名解析的工作原理。","difficulty":2,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"www.baidu.com":"online","mail.163.com":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"nslookup 查询","hint":"输入 nslookup www.baidu.com 后按回车","validate":{"event":"cmdExecute","target":"nslookup"}}]}', 'practice', 2, 90, 'network', 1, 1);

-- 22. nslookup-指定DNS服务器
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('nslookup-指定DNS服务器查询', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1125, '{"title":"指定DNS服务器查询","description":"使用nslookup命令指定特定的DNS服务器（如8.8.8.8）来查询域名IP，观察不同DNS服务器的解析结果。","difficulty":3,"timeLimit":90,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{"www.baidu.com":"online","8.8.8.8":"online"}},"steps":[{"id":1,"name":"打开命令提示符","hint":"点击开始菜单，搜索CMD，打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"nslookup 指定DNS","hint":"输入 nslookup www.baidu.com 8.8.8.8 后按回车","validate":{"event":"cmdExecute","target":"nslookup"}}]}', 'practice', 3, 90, 'network', 1, 1);

-- ===== 单元2任务2：配置TCP/IP协议（netsh × 3）=====

-- 23. 配置静态IP地址
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('配置静态IP地址', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1093, '{"title":"配置静态IP地址","description":"使用netsh命令为网卡配置静态IP地址为192.168.1.10，子网掩码255.255.255.0，网关192.168.1.1。","difficulty":3,"timeLimit":120,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符（管理员）","hint":"以管理员身份打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 netsh 设置IP","hint":"输入命令设置静态IP地址","validate":{"event":"cmdExecute","target":"netsh"}},{"id":3,"name":"验证配置","hint":"输入 ipconfig 验证IP是否生效","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 3, 120, 'network', 1, 1);

-- 24. 配置DNS服务器地址
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('配置DNS服务器地址', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1103, '{"title":"配置DNS服务器地址","description":"使用netsh命令为网卡设置首选DNS为8.8.8.8，备用DNS为8.8.4.4。DNS负责域名→IP的解析。","difficulty":3,"timeLimit":120,"category":"network","networkConfig":{"localIP":"192.168.1.100","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"192.168.1.1","dhcp":true,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符（管理员）","hint":"以管理员身份打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 netsh 设置DNS","hint":"输入命令设置DNS服务器","validate":{"event":"cmdExecute","target":"netsh"}},{"id":3,"name":"验证DNS配置","hint":"输入 ipconfig /all 查看DNS是否更新","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 3, 120, 'network', 1, 1);

-- 25. 切换为自动获取IP
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('切换为自动获取IP', 'FORMATIVE', '网络应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, category, created_by, school_id) VALUES
(@task_id, 1096, '{"title":"切换为自动获取IP(DHCP)","description":"使用netsh命令将网卡从静态IP切换为DHCP自动获取模式，观察IP地址的变化。","difficulty":2,"timeLimit":120,"category":"network","networkConfig":{"localIP":"192.168.1.10","subnetMask":"255.255.255.0","gateway":"192.168.1.1","dns":"8.8.8.8","dhcp":false,"mac":"00-1A-2B-3C-4D-5E","remoteHosts":{}},"steps":[{"id":1,"name":"打开命令提示符（管理员）","hint":"以管理员身份打开命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"执行 netsh 切换DHCP","hint":"输入命令切换为自动获取IP","validate":{"event":"cmdExecute","target":"netsh"}},{"id":3,"name":"验证DHCP","hint":"输入 ipconfig /all 查看DHCP是否启用","validate":{"event":"cmdExecute","target":"ipconfig"}}]}', 'practice', 2, 120, 'network', 1, 1);
