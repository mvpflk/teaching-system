-- v102: 补全仿真任务到35个（原始12个 + 新增23个）
-- 使用 INSERT IGNORE 防止重复

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('切换输入法', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 972, '{"title":"切换输入法","description":"使用Ctrl+Shift切换到搜狗拼音输入法","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"按Ctrl+Shift","hint":"同时按下Ctrl键和Shift键","validate":{"event":"keyDown","target":"Ctrl+Shift"}}]}', 'practice', 1, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('窗口操作练习', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 974, '{"title":"窗口操作练习","description":"对资源管理器窗口依次进行最大化、还原、最小化操作","difficulty":1,"timeLimit":90,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击桌面上的计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"最大化","hint":"点击标题栏□按钮","validate":{"event":"click","target":"maximize"}},{"id":3,"name":"还原","hint":"再次点击□按钮","validate":{"event":"click","target":"restore"}},{"id":4,"name":"最小化","hint":"点击标题栏─按钮","validate":{"event":"click","target":"minimize"}}]}', 'practice', 1, 90, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('多窗口切换', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 974, '{"title":"多窗口切换","description":"同时打开资源管理器和记事本，使用Alt+Tab切换","difficulty":2,"timeLimit":90,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"打开记事本","hint":"开始菜单→记事本","validate":{"event":"launch","target":"notepad"}},{"id":3,"name":"Alt+Tab切换","hint":"按Alt+Tab在窗口间切换","validate":{"event":"keyDown","target":"Alt+Tab"}}]}', 'practice', 2, 90, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('使用剪贴板', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 978, '{"title":"使用剪贴板","description":"复制桌面上的文件，粘贴到文档文件夹中","difficulty":2,"timeLimit":120,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"复制文件","hint":"选中文件按Ctrl+C","validate":{"event":"keyDown","target":"Ctrl+C"}},{"id":3,"name":"导航到文档","hint":"在目录树中点击文档","validate":{"event":"click","target":"tree:文档"}},{"id":4,"name":"粘贴","hint":"按Ctrl+V","validate":{"event":"keyDown","target":"Ctrl+V"}}]}', 'practice', 2, 120, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('调整任务栏', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 972, '{"title":"调整任务栏","description":"右键任务栏→属性→勾选自动隐藏","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"右键任务栏","hint":"在任务栏空白处右键","validate":{"event":"rightClick","target":"taskbar"}},{"id":2,"name":"选择属性","hint":"点击属性选项","validate":{"event":"menuSelect","target":"taskbarProperties"}}]}', 'practice', 1, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('使用帮助系统', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 979, '{"title":"使用帮助系统","description":"按F1键打开Windows帮助和支持","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"按F1键","hint":"按下键盘上的F1功能键","validate":{"event":"keyDown","target":"F1"}}]}', 'practice', 1, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('安全模式识别', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 968, '{"title":"安全模式识别","description":"了解Windows安全模式的启动方式和用途","difficulty":3,"timeLimit":120,"steps":[{"id":1,"name":"打开开始菜单","hint":"点击开始按钮","validate":{"event":"click","target":"startButton"}},{"id":2,"name":"关机选项","hint":"点击关机旁边的箭头","validate":{"event":"click","target":"shutdownMenu"}},{"id":3,"name":"重新启动","hint":"选择重新启动","validate":{"event":"menuSelect","target":"restart"}}]}', 'practice', 3, 120, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('浏览目录树', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 981, '{"title":"浏览目录树","description":"依次展开C盘→Users→Student→桌面","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"展开C盘","hint":"点击C盘左侧箭头","validate":{"event":"click","target":"tree:C:"}},{"id":3,"name":"展开Users","hint":"展开Users文件夹","validate":{"event":"click","target":"tree:Users"}},{"id":4,"name":"选择桌面","hint":"点击桌面","validate":{"event":"click","target":"tree:桌面"}}]}', 'practice', 1, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('永久删除文件', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 987, '{"title":"永久删除文件","description":"Shift+Delete永久删除（不经过回收站）","difficulty":2,"timeLimit":60,"steps":[{"id":1,"name":"选中文件","hint":"单击选中文件","validate":{"event":"click","target":"file:临时文件.txt"}},{"id":2,"name":"Shift+Delete","hint":"按住Shift+Delete","validate":{"event":"keyDown","target":"Shift+Delete"}}]}', 'practice', 2, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('设置文件属性', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 992, '{"title":"设置文件属性","description":"将文件设置为只读属性","difficulty":2,"timeLimit":90,"steps":[{"id":1,"name":"右键文件","hint":"右键点击文件","validate":{"event":"rightClick","target":"file:笔记.txt"}},{"id":2,"name":"选择属性","hint":"点击属性","validate":{"event":"menuSelect","target":"properties"}},{"id":3,"name":"勾选只读","hint":"勾选只读复选框","validate":{"event":"click","target":"checkbox:readonly"}},{"id":4,"name":"确认","hint":"点击确定","validate":{"event":"click","target":"confirm"}}]}', 'practice', 2, 90, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('通配符搜索文件', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 993, '{"title":"通配符搜索文件","description":"搜索框中输入*.txt搜索所有文本文件","difficulty":2,"timeLimit":90,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"点击搜索框","hint":"点击搜索框","validate":{"event":"click","target":"searchBox"}},{"id":3,"name":"输入通配符","hint":"输入 *.txt 回车","validate":{"event":"input","target":"search:*.txt"}}]}', 'practice', 2, 90, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('多选文件批量删除', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 987, '{"title":"多选文件批量删除","description":"按住Ctrl键同时选中3个文件后批量删除","difficulty":2,"timeLimit":120,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"Ctrl多选","hint":"按住Ctrl逐个点击3个文件","validate":{"event":"keyDown","target":"Ctrl+click"}},{"id":3,"name":"按Delete","hint":"按Delete键删除","validate":{"event":"keyDown","target":"Delete"}}]}', 'practice', 2, 120, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('地址栏直接导航', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 984, '{"title":"地址栏直接导航","description":"在地址栏输入C:\\\\Windows并回车","difficulty":2,"timeLimit":60,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"点击地址栏","hint":"点击地址栏","validate":{"event":"click","target":"addressBar"}},{"id":3,"name":"输入路径","hint":"输入C:\\\\Windows回车","validate":{"event":"input","target":"address:C:\\\\Windows"}}]}', 'practice', 2, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('切换查看方式', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 981, '{"title":"切换查看方式","description":"将查看方式切换为详细信息","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"右键空白处","hint":"在文件列表空白处右键","validate":{"event":"rightClick","target":"panel:empty"}},{"id":3,"name":"选择详细信息","hint":"查看→详细信息","validate":{"event":"menuSelect","target":"viewDetails"}}]}', 'practice', 1, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('使用库管理', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 994, '{"title":"使用库管理","description":"将文件夹添加到文档库中","difficulty":3,"timeLimit":120,"steps":[{"id":1,"name":"打开资源管理器","hint":"双击计算机图标","validate":{"event":"dblClick","target":"desktop_icon:computer"}},{"id":2,"name":"右键文件夹","hint":"右键课程资料","validate":{"event":"rightClick","target":"file:课程资料"}},{"id":3,"name":"添加到库","hint":"包含到库中→文档","validate":{"event":"menuSelect","target":"addToLibrary"}}]}', 'practice', 3, 120, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('更换桌面背景', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 998, '{"title":"更换桌面背景","description":"右键桌面→个性化→更换为纯色背景","difficulty":1,"timeLimit":90,"steps":[{"id":1,"name":"右键桌面","hint":"右键桌面空白处","validate":{"event":"rightClick","target":"desktop"}},{"id":2,"name":"选择个性化","hint":"点击个性化","validate":{"event":"menuSelect","target":"personalize"}},{"id":3,"name":"选择纯色","hint":"选择纯色背景","validate":{"event":"click","target":"bg:solid"}}]}', 'practice', 1, 90, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('调整屏幕分辨率', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 999, '{"title":"调整屏幕分辨率","description":"右键桌面→屏幕分辨率→改为1366×768","difficulty":1,"timeLimit":90,"steps":[{"id":1,"name":"右键桌面","hint":"右键桌面空白处","validate":{"event":"rightClick","target":"desktop"}},{"id":2,"name":"屏幕分辨率","hint":"点击屏幕分辨率","validate":{"event":"menuSelect","target":"screenResolution"}},{"id":3,"name":"选择分辨率","hint":"选择1366×768","validate":{"event":"click","target":"resolution:1366x768"}}]}', 'practice', 1, 90, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('程序和功能', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1006, '{"title":"程序和功能","description":"打开控制面板→程序和功能，查看已安装程序","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"打开控制面板","hint":"开始菜单→控制面板","validate":{"event":"launch","target":"control"}},{"id":2,"name":"程序和功能","hint":"点击程序和功能","validate":{"event":"click","target":"cp:programs"}}]}', 'practice', 1, 60, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('CMD-copy+del组合', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1029, '{"title":"CMD-copy+del组合","description":"CMD将文件复制到D盘，删除原文件","difficulty":3,"timeLimit":180,"steps":[{"id":1,"name":"打开CMD","hint":"开始菜单→CMD","validate":{"event":"launch","target":"cmd"}},{"id":2,"name":"复制文件","hint":"copy notes.txt D:\\\\","validate":{"event":"cmdExecute","target":"copy"}},{"id":3,"name":"确认复制","hint":"dir D:\\\\","validate":{"event":"cmdExecute","target":"dir"}},{"id":4,"name":"删除原文件","hint":"del notes.txt","validate":{"event":"cmdExecute","target":"del"}}]}', 'practice', 3, 180, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('CMD-综合任务', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1028, '{"title":"CMD-综合任务","description":"在D盘创建backup文件夹，切换目录，确认创建","difficulty":4,"timeLimit":240,"steps":[{"id":1,"name":"切换到D盘","hint":"D:","validate":{"event":"cmdExecute","target":"cd"}},{"id":2,"name":"创建backup","hint":"md backup","validate":{"event":"cmdExecute","target":"md"}},{"id":3,"name":"进入backup","hint":"cd backup","validate":{"event":"cmdExecute","target":"cd"}},{"id":4,"name":"返回D盘","hint":"cd ..","validate":{"event":"cmdExecute","target":"cd"}},{"id":5,"name":"确认目录","hint":"dir","validate":{"event":"cmdExecute","target":"dir"}}]}', 'practice', 4, 240, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('记事本-保存文件', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1032, '{"title":"记事本-保存文件","description":"打开记事本，输入文字，保存到桌面","difficulty":2,"timeLimit":120,"steps":[{"id":1,"name":"打开记事本","hint":"开始菜单→附件→记事本","validate":{"event":"launch","target":"notepad"}},{"id":2,"name":"输入文字","hint":"输入任意文字","validate":{"event":"input","target":"notepad"}}]}', 'practice', 2, 120, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('画图工具', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1036, '{"title":"画图工具","description":"打开画图，画一个简单图形","difficulty":1,"timeLimit":120,"steps":[{"id":1,"name":"打开画图","hint":"开始菜单→附件→画图","validate":{"event":"launch","target":"paint"}}]}', 'practice', 1, 120, 1, 1);

INSERT IGNORE INTO tasks (title, task_type, subject, status, teacher_id, school_id, stage_id, is_required, auto_wrongbook) VALUES
('写字板使用', 'FORMATIVE', '信息技术应用基础', 'PUBLISHED', 1, 1, 4, 1, 1);
SET @task_id = LAST_INSERT_ID();
INSERT IGNORE INTO simulation_tasks (task_id, node_id, task_json, mode, difficulty, time_limit, created_by, school_id) VALUES
(@task_id, 1032, '{"title":"写字板使用","description":"打开写字板，了解其与记事本的区别","difficulty":1,"timeLimit":60,"steps":[{"id":1,"name":"打开写字板","hint":"开始菜单→附件→写字板","validate":{"event":"launch","target":"wordpad"}}]}', 'practice', 1, 60, 1, 1);
