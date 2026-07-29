/**
 * 仿真任务常量 — SimTaskEditor / Win7TeacherTaskList 共用
 */

export const SIM_CATEGORIES = [
  { value: 'win7', label: 'Windows 操作', subject: '信息技术应用基础' },
  { value: 'network', label: '网络应用基础', subject: '网络应用基础' },
]

export const SIM_MODES = [
  { value: 'practice', label: '练习' },
  { value: 'exam', label: '考试' },
]

export const WIN7_EVENTS = [
  { value: 'click', label: '单击 (click)' },
  { value: 'dblClick', label: '双击 (dblClick)' },
  { value: 'rightClick', label: '右键 (rightClick)' },
  { value: 'keyDown', label: '按键 (keyDown)' },
  { value: 'launch', label: '启动应用 (launch)' },
  { value: 'cmdExecute', label: '执行命令 (cmdExecute)' },
  { value: 'menuSelect', label: '菜单选择 (menuSelect)' },
  { value: 'input', label: '输入 (input)' },
]

export const NETWORK_EVENTS = [
  { value: 'launch', label: '启动应用 (launch)' },
  { value: 'cmdExecute', label: '执行命令 (cmdExecute)' },
]

export const NETWORK_TARGETS = [
  { value: 'cmd', label: 'CMD 窗口' },
  { value: 'ping', label: 'ping 命令' },
  { value: 'ipconfig', label: 'ipconfig 命令' },
  { value: 'tracert', label: 'tracert 命令' },
  { value: 'netstat', label: 'netstat 命令' },
  { value: 'nslookup', label: 'nslookup 命令' },
  { value: 'netsh', label: 'netsh 命令' },
]

export const WIN7_TARGETS = [
  { value: 'desktop_icon:computer', label: '桌面图标: 计算机' },
  { value: 'desktop_icon:recycle', label: '桌面图标: 回收站' },
  { value: 'desktop_icon:documents', label: '桌面图标: 我的文档' },
  { value: 'desktop', label: '桌面空白处' },
  { value: 'startButton', label: '开始按钮' },
  { value: 'taskbar', label: '任务栏' },
  { value: 'maximize', label: '最大化按钮' },
  { value: 'restore', label: '还原按钮' },
  { value: 'minimize', label: '最小化按钮' },
  { value: 'tree:桌面', label: '目录树: 桌面' },
  { value: 'tree:C:', label: '目录树: C盘' },
  { value: 'tree:Users', label: '目录树: Users' },
  { value: 'tree:文档', label: '目录树: 文档' },
  { value: 'drive:C', label: '驱动器: C盘' },
  { value: 'drive:D', label: '驱动器: D盘' },
  { value: 'panel:empty', label: '文件区域空白处' },
  { value: 'addressBar', label: '地址栏' },
  { value: 'searchBox', label: '搜索框' },
  { value: 'navUp', label: '向上导航' },
  { value: 'refresh', label: '刷新' },
  { value: 'file:笔记.txt', label: '文件: 笔记.txt' },
  { value: 'file:临时文件.txt', label: '文件: 临时文件.txt' },
  { value: 'file:新建文件夹', label: '文件: 新建文件夹' },
  { value: 'file:课程资料', label: '文件: 课程资料' },
  { value: 'Ctrl+C', label: 'Ctrl+C (复制)' },
  { value: 'Ctrl+V', label: 'Ctrl+V (粘贴)' },
  { value: 'Ctrl+Shift', label: 'Ctrl+Shift (切换输入法)' },
  { value: 'Delete', label: 'Delete (删除到回收站)' },
  { value: 'Shift+Delete', label: 'Shift+Delete (永久删除)' },
  { value: 'F1', label: 'F1 (帮助)' },
  { value: 'F2', label: 'F2 (重命名)' },
  { value: 'Alt+Tab', label: 'Alt+Tab (窗口切换)' },
  { value: 'Enter', label: 'Enter (确认)' },
  { value: 'Ctrl+click', label: 'Ctrl+点击 (多选)' },
  { value: 'sendToDesktop', label: '发送到桌面' },
  { value: 'sortByName', label: '按名称排序' },
  { value: 'viewDetails', label: '查看→详细信息' },
  { value: 'personalize', label: '个性化' },
  { value: 'screenResolution', label: '屏幕分辨率' },
  { value: 'properties', label: '属性' },
  { value: 'addToLibrary', label: '添加到库' },
  { value: 'taskbarProperties', label: '任务栏属性' },
  { value: 'cp:programs', label: '控制面板: 程序和功能' },
  { value: 'bg:solid', label: '桌面背景: 纯色' },
  { value: 'resolution:1366x768', label: '分辨率: 1366×768' },
  { value: 'checkbox:readonly', label: '只读复选框' },
  { value: 'confirm', label: '确认按钮' },
  { value: 'confirmDelete', label: '确认删除' },
  { value: 'menu:notepad', label: '开始菜单: 记事本' },
  { value: 'shutdownMenu', label: '关机菜单' },
  { value: 'restart', label: '重新启动' },
]

export const WIN7_APPS = [
  { value: 'explorer', label: '资源管理器 (explorer)' },
  { value: 'notepad', label: '记事本 (notepad)' },
  { value: 'cmd', label: '命令提示符 (cmd)' },
  { value: 'paint', label: '画图 (paint)' },
  { value: 'calculator', label: '计算器 (calculator)' },
  { value: 'control', label: '控制面板 (control)' },
]

export const NETWORK_COMMAND_GROUPS = [
  { label: 'ping — 网络连通性测试', commands: [
    { name: 'ping 网关', cmd: 'ping 192.168.1.1', desc: '测试默认网关连通性' },
    { name: 'ping 百度', cmd: 'ping www.baidu.com', desc: '测试互联网连通性' },
    { name: 'ping 127.0.0.1', cmd: 'ping 127.0.0.1', desc: '测试本地回环地址' },
    { name: 'ping 不存在主机', cmd: 'ping 10.0.0.99', desc: '测试到不可达主机' },
    { name: 'ping -t', cmd: 'ping -t 192.168.1.1', desc: '连续 ping 测试' },
  ]},
  { label: 'ipconfig — IP 配置查看', commands: [
    { name: 'ipconfig', cmd: 'ipconfig', desc: '查看基本 IP 配置' },
    { name: 'ipconfig /all', cmd: 'ipconfig /all', desc: '查看详细配置' },
    { name: 'ipconfig /release', cmd: 'ipconfig /release', desc: '释放 IP 地址' },
    { name: 'ipconfig /renew', cmd: 'ipconfig /renew', desc: '重新获取 IP' },
    { name: 'ipconfig /flushdns', cmd: 'ipconfig /flushdns', desc: '清除 DNS 缓存' },
  ]},
  { label: 'tracert — 路由追踪', commands: [
    { name: 'tracert 网关', cmd: 'tracert 192.168.1.1', desc: '追踪到网关的路由' },
    { name: 'tracert 百度', cmd: 'tracert www.baidu.com', desc: '追踪到百度的路由' },
    { name: 'tracert DNS', cmd: 'tracert 8.8.8.8', desc: '追踪到 DNS 服务器' },
    { name: 'tracert -d', cmd: 'tracert -d 8.8.8.8', desc: '不解析主机名的追踪' },
    { name: 'tracert -h', cmd: 'tracert -h 3 www.baidu.com', desc: '限制最大跃点数' },
  ]},
  { label: 'netstat — 网络连接统计', commands: [
    { name: 'netstat', cmd: 'netstat', desc: '查看活动连接' },
    { name: 'netstat -a', cmd: 'netstat -a', desc: '查看所有连接' },
    { name: 'netstat -n', cmd: 'netstat -n', desc: '数字形式显示地址' },
    { name: 'netstat -an', cmd: 'netstat -an', desc: '查看监听端口' },
    { name: 'netstat -ano', cmd: 'netstat -ano', desc: '查看进程 ID' },
  ]},
  { label: 'nslookup / netsh — DNS 和网络配置', commands: [
    { name: 'nslookup', cmd: 'nslookup www.baidu.com', desc: '查询域名对应 IP' },
    { name: 'nslookup 指定DNS', cmd: 'nslookup www.baidu.com 8.8.8.8', desc: '指定 DNS 服务器查询' },
    { name: 'netsh 静态IP', cmd: 'netsh', desc: '配置静态 IP 地址' },
    { name: 'netsh DNS', cmd: 'netsh', desc: '配置 DNS 服务器' },
    { name: 'netsh DHCP', cmd: 'netsh', desc: '切换为自动获取 IP' },
  ]},
]

export const VALIDATE_TYPES = [
  { value: 'event', label: '事件验证' },
  { value: 'vfs', label: 'VFS 验证', win7Only: true },
  { value: 'window', label: '窗口验证', win7Only: true },
]
