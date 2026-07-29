-- v101: Windows 仿真模块 — Feature 开关 + 预置任务数据
-- 依赖：v100_simulation.sql（表结构）

-- Feature 开关
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('feature.win7_simulation', 'true', 'Windows 7 仿真操作实训模块开关')
ON DUPLICATE KEY UPDATE setting_value = 'true';

-- ═══ 预置仿真任务（35个）═══
-- 每个 INSERT 创建 Task + SimulationTask

-- ===== 单元4：认识操作系统（10个任务）=====

-- 4.1 识别桌面元素
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('识别桌面元素', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 970, '{"title":"识别桌面元素","description":"指出桌面上的图标、任务栏和开始按钮的位置","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"找到桌面图标","hint":"观察屏幕左侧的图标区域","validate":{"event":"click","target":"desktop_icon:computer"}},{"id":2,"name":"找到任务栏","hint":"任务栏在屏幕底部","validate":{"event":"click","target":"taskbar"}},{"id":3,"name":"点击开始按钮","hint":"任务栏左侧的圆形按钮","validate":{"event":"click","target":"startButton"}}]}', 'practice', 1, 60, 1, 1);

-- 4.2 排列桌面图标
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('排列桌面图标', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 971, '{"title":"排列桌面图标","description":"右键桌面空白处，按名称排列图标","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"右键桌面","hint":"在桌面空白处点击右键","validate":{"event":"rightClick","target":"desktop"}},{"id":2,"name":"选择排序方式","hint":"在弹出的菜单中选择排序方式","validate":{"event":"menuSelect","target":"sortByName"}}]}', 'practice', 1, 60, 1, 1);

-- 4.3 创建快捷方式
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('创建桌面快捷方式', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 977, '{"title":"创建桌面快捷方式","description":"为记事本创建一个桌面快捷方式","difficulty":2,"timeLimit":90,"steps":[{"id":1,"name":"打开开始菜单","hint":"点击左下角开始按钮","validate":{"event":"click","target":"startButton"}},{"id":2,"name":"找到记事本","hint":"在所有程序中找到记事本","validate":{"event":"click","target":"menu:notepad"}},{"id":3,"name":"发送到桌面","hint":"右键→发送到→桌面快捷方式","validate":{"event":"menuSelect","target":"sendToDesktop"}}]}', 'practice', 2, 90, 1, 1);

-- ===== 单元5：管理 Windows 资源（12个任务）=====

-- 5.1 新建文件夹
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('在桌面新建文件夹', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 987, '{"title":"在桌面新建文件夹","description":"在桌面上创建一个名为\\"作业\\"的文件夹","difficulty":1,"timeLimit":120,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击桌面上的\\"计算机\\"图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"导航到桌面","hint":"在地址栏或目录树中找到桌面路径","validate":{"event":"click","target":"tree:桌面"}},{"id":3,"name":"新建文件夹","hint":"在空白处右键→新建→文件夹","validate":{"event":"rightClick","target":"panel:empty"}},{"id":4,"name":"命名为\\"作业\\"","hint":"输入\\"作业\\"后按回车","validate":{"vfs":{"path":"C:/Users/Student/桌面/作业","type":"folder"}}}]}', 'practice', 1, 120, 1, 1);

-- 5.2 重命名文件
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('重命名文件', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 987, '{"title":"重命名文件","description":"将桌面上的\\"新建文件夹\\"重命名为\\"课程资料\\"","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"选中文件夹","hint":"单击选中桌面上的文件夹","validate":{"event":"click","target":"file:新建文件夹"}},{"id":2,"name":"按F2重命名","hint":"按键盘上的F2键进入重命名模式","validate":{"event":"keyDown","target":"F2"}},{"id":3,"name":"输入新名称","hint":"输入\\"课程资料\\"后按回车","validate":{"vfs":{"path":"C:/Users/Student/桌面/课程资料","type":"folder"}}}]}', 'practice', 1, 60, 1, 1);

-- 5.3 复制粘贴文件
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('复制文件到D盘', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 987, '{"title":"复制文件到D盘","description":"将桌面的\\"笔记.txt\\"复制到D盘根目录","difficulty":2,"timeLimit":120,"steps":[{"id":1,"name":"复制文件","hint":"右键文件→复制，或Ctrl+C","validate":{"event":"rightClick","target":"file:笔记.txt"}},{"id":2,"name":"打开D盘","hint":"在资源管理器中点击D盘","validate":{"event":"click","target":"drive:D"}},{"id":3,"name":"粘贴文件","hint":"右键空白处→粘贴，或Ctrl+V","validate":{"vfs":{"path":"D:/笔记.txt","type":"file"}}}]}', 'practice', 2, 120, 1, 1);

-- 5.4 删除文件到回收站
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('删除文件到回收站', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 987, '{"title":"删除文件","description":"删除桌面上的\\"临时文件.txt\\"，然后从回收站中还原","difficulty":2,"timeLimit":90,"steps":[{"id":1,"name":"删除文件","hint":"右键文件→删除，或按Delete键","validate":{"event":"rightClick","target":"file:临时文件.txt"}},{"id":2,"name":"确认删除","hint":"在确认对话框中点击\\"是\\"","validate":{"event":"click","target":"confirmDelete"}},{"id":3,"name":"打开回收站","hint":"双击桌面上的回收站图标","validate":{"event":"dblClick","target":"desktop_icon:recycle"}},{"id":4,"name":"还原文件","hint":"右键文件→还原","validate":{"vfs":{"path":"C:/Users/Student/桌面/临时文件.txt","type":"file"}}}]}', 'practice', 2, 90, 1, 1);

-- ===== 单元6：CMD 命令（8个任务）=====

-- 6.1 dir 命令
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('CMD-列出目录内容(dir)', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1027, '{"title":"CMD-列出目录内容","description":"打开CMD，使用dir命令查看当前目录内容","difficulty":1,"timeLimit":90,"steps":[{"id":1,"name":"打开CMD","hint":"开始菜单→所有程序→命令提示符","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"输入dir命令","hint":"输入 dir 后按回车","validate":{"event":"cmdExecute","target":"dir"}}]}', 'practice', 1, 90, 1, 1);

-- 6.2 cd 命令
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('CMD-切换目录(cd)', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1028, '{"title":"CMD-切换目录","description":"使用cd命令进入Windows目录，再用dir查看内容","difficulty":2,"timeLimit":90,"steps":[{"id":1,"name":"进入Windows目录","hint":"输入 cd C:\\\\Windows 后按回车","validate":{"event":"cmdExecute","target":"cd"}},{"id":2,"name":"查看内容","hint":"输入 dir 查看Windows目录下的文件","validate":{"event":"cmdExecute","target":"dir"}}]}', 'practice', 2, 90, 1, 1);

-- 6.3 md + rd 命令
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('CMD-创建和删除目录', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1028, '{"title":"CMD-创建和删除目录","description":"在D盘用md创建test文件夹，用dir确认，再用rd删除","difficulty":2,"timeLimit":120,"steps":[{"id":1,"name":"切换到D盘","hint":"输入 D: 后按回车","validate":{"event":"cmdExecute","target":"cd"}},{"id":2,"name":"创建test目录","hint":"输入 md test 后按回车","validate":{"event":"cmdExecute","target":"md"}},{"id":3,"name":"确认创建成功","hint":"输入 dir 查看","validate":{"event":"cmdExecute","target":"dir"}},{"id":4,"name":"删除test目录","hint":"输入 rd test 后按回车","validate":{"event":"cmdExecute","target":"rd"}}]}', 'practice', 2, 120, 1, 1);

-- ===== 单元7：Windows 附件（5个任务）=====

-- 7.1 记事本
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('使用记事本', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1032, '{"title":"使用记事本","description":"打开记事本，输入一段文字，保存到桌面","difficulty":1,"timeLimit":120,"steps":[{"id":1,"name":"打开记事本","hint":"开始菜单→所有程序→附件→记事本","validate":{"event":"launch","target":"notepad"}},{"id":2,"name":"输入文字","hint":"在记事本中输入任意文字","validate":{"event":"input","target":"notepad"}}]}', 'practice', 1, 120, 1, 1);

-- 计算器
INSERT INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('使用计算器', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1039, '{"title":"使用计算器","description":"打开计算器，计算 128 + 256","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"打开计算器","hint":"开始菜单→所有程序→附件→计算器","validate":{"event":"launch","target":"calculator"}}]}', 'practice', 1, 60, 1, 1);
