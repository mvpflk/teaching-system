-- ============================================================
-- v166: 考纲驱动实训模板 — 计算机专业4科核心任务模板
-- 覆盖: 信息技术应用基础(5) / 网络应用基础(6) / 办公应用基础(5) / Access(4)
-- ============================================================

-- 模板1: 信息技术应用基础 — Windows 文件管理
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('Windows 文件与文件夹管理', '掌握文件/文件夹的创建、复制、移动、重命名、删除及回收站操作', '信息技术应用基础', 'operation',
 '[{"title":"创建文件夹结构","type":"text","description":"在D盘创建如下文件夹结构: D:\\练习\\文档\\、D:\\练习\\图片\\、D:\\练习\\音乐\\","scoreMax":20},{"title":"新建文件并保存","type":"file","description":"在「文档」文件夹中新建一个文本文件，输入你的姓名和班级信息","scoreMax":20},{"title":"复制与移动操作","type":"text","description":"将「文档」中的文件复制到「图片」文件夹，再将原文件移动到「音乐」文件夹","scoreMax":20},{"title":"重命名与删除","type":"text","description":"将「图片」中的文件重命名为「备份.txt」，删除「音乐」中的原文件，查看回收站","scoreMax":20},{"title":"搜索与快捷方式","type":"text","description":"使用Windows搜索功能查找「备份.txt」，在桌面创建指向D:\\练习的快捷方式","scoreMax":20}]',
 '[{"dimension":"process_quality","dimension_label":"操作规范性","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"所有操作步骤正确且高效"}]},{"dimension":"product_result","dimension_label":"成果完整性","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"文件夹结构和文件完整正确"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板2: 信息技术应用基础 — CMD常用命令
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('CMD 常用命令操作', '掌握DIR/MD/CD/COPY/DEL等基础命令行操作', '信息技术应用基础', 'operation',
 '[{"title":"启动命令提示符","type":"text","description":"使用Win+R打开运行窗口，输入cmd启动命令提示符，记录当前路径","scoreMax":10},{"title":"DIR 查看目录","type":"text","description":"使用DIR命令查看当前目录下的文件和文件夹，记录看到的文件数量","scoreMax":20},{"title":"MD创建与CD切换","type":"text","description":"使用MD命令创建文件夹 test_folder，使用CD命令进入该文件夹，用DIR确认","scoreMax":20},{"title":"COPY 复制文件","type":"text","description":"在test_folder中，使用COPY CON命令创建一个文件，用DIR确认文件存在","scoreMax":25},{"title":"DEL删除与返回","type":"text","description":"使用DEL命令删除刚创建的文件，CD..返回上级目录","scoreMax":25}]',
 '[{"dimension":"process_quality","dimension_label":"命令掌握","weight":0.6,"criteria":[{"level":"A","label":"优秀","description":"所有命令语法正确，参数使用恰当"}]},{"dimension":"product_result","dimension_label":"操作结果","weight":0.4,"criteria":[{"level":"A","label":"优秀","description":"文件和文件夹操作全部成功"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板3: 网络应用基础 — IP地址与子网划分
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('IP地址分类与子网掩码计算', '掌握IPv4地址分类、子网掩码作用和子网划分方法', '网络应用基础', 'operation',
 '[{"title":"识别IP地址类别","type":"choice","description":"判断给出的IP地址属于A/B/C类","scoreMax":20},{"title":"计算网络地址","type":"text","description":"给定IP地址和子网掩码，计算网络地址（AND运算）","scoreMax":20},{"title":"计算广播地址","type":"text","description":"计算该子网的广播地址","scoreMax":20},{"title":"可用主机范围","type":"text","description":"给出该子网中可分配给主机的IP地址范围","scoreMax":20},{"title":"子网划分实践","type":"text","description":"给定一个C类网络和子网数量需求，设计子网划分方案","scoreMax":20}]',
 '[{"dimension":"skill_","dimension_label":"计算技能","weight":0.6,"criteria":[{"level":"A","label":"优秀","description":"IP地址运算全部正确"}]},{"dimension":"prof_","dimension_label":"专业规范","weight":0.4,"criteria":[{"level":"A","label":"优秀","description":"划分方案合理规范"}]}]',
 'COMPETITION', 'SYSTEM', 0);

-- 模板4: 网络应用基础 — ping/ipconfig网络命令
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('网络诊断命令 ping 与 ipconfig', '掌握常用网络诊断命令的使用和结果分析', '网络应用基础', 'operation',
 '[{"title":"ipconfig查看配置","type":"sim","description":"在仿真环境中使用ipconfig命令查看本机IP配置，记录IPv4地址和默认网关","scoreMax":20},{"title":"ping回环测试","type":"sim","description":"使用ping 127.0.0.1测试TCP/IP协议栈是否正常","scoreMax":20},{"title":"ping网关","type":"sim","description":"使用ping命令测试与默认网关的连通性","scoreMax":20},{"title":"ping外网","type":"sim","description":"使用ping baidu.com测试与外网的连通性，观察DNS解析过程","scoreMax":20},{"title":"ipconfig /all分析","type":"text","description":"执行ipconfig /all，分析MAC地址、DHCP状态、DNS服务器等详细信息","scoreMax":20}]',
 '[{"dimension":"process_quality","dimension_label":"命令使用","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"命令使用正确，参数恰当"}]},{"dimension":"product_result","dimension_label":"结果分析","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"能正确解读命令输出结果"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板5: 网络应用基础 — HTML网页制作
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('HTML基础网页制作', '使用HTML+CSS制作一个包含标题、段落、图片和超链接的简单网页', '网络应用基础', 'design',
 '[{"title":"创建HTML基本结构","type":"web","description":"使用HTML标签创建基本页面结构: html/head/title/body","scoreMax":20,"config":{"templateHtml":"<h1>我的网页</h1>"}},{"title":"添加标题和段落","type":"web","description":"添加h1-h3标题标签和p段落标签，输入一段自我介绍文字","scoreMax":20},{"title":"插入图片","type":"web","description":"使用img标签插入一张图片，设置src和alt属性","scoreMax":20},{"title":"创建超链接","type":"web","description":"使用a标签创建至少3个不同类型的超链接（外部链接、内部锚点、邮件链接）","scoreMax":20},{"title":"CSS美化页面","type":"web","description":"使用CSS设置页面背景色、字体颜色、边距等样式，让页面更美观","scoreMax":20}]',
 '[{"dimension":"process_quality","dimension_label":"代码规范","weight":0.4,"criteria":[{"level":"A","label":"优秀","description":"标签使用正确，属性完整"}]},{"dimension":"product_result","dimension_label":"页面效果","weight":0.6,"criteria":[{"level":"A","label":"优秀","description":"页面结构完整，样式美观"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板6: 网络应用基础 — tracert路由追踪
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('路由追踪 tracert 命令', '使用tracert命令追踪数据包到达目标主机的路径', '网络应用基础', 'operation',
 '[{"title":"tracert基本使用","type":"sim","description":"使用tracert baidu.com追踪到达百度服务器的路由路径","scoreMax":25},{"title":"分析路由跳数","type":"text","description":"记录追踪结果中的跳数（hops），分析经过了多少个中间路由器","scoreMax":25},{"title":"tracert -d 参数","type":"sim","description":"使用tracert -d参数不解析主机名，比较与默认tracert的区别","scoreMax":25},{"title":"对比不同目标","type":"text","description":"追踪两个不同网站(如百度、淘宝)，比较路由路径的差异","scoreMax":25}]',
 '[{"dimension":"process_quality","dimension_label":"命令使用","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"命令及参数正确"}]},{"dimension":"product_result","dimension_label":"结果分析","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"能正确解读路由路径"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板7: 办公应用基础 — Word 文档排版
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('Word 文档格式排版', '掌握字体段落格式、页面设置、页眉页脚和目录生成', '办公应用基础', 'office',
 '[{"title":"设置字体和段落格式","type":"office","description":"设置标题为黑体二号加粗居中，正文为宋体小四，段落首行缩进2字符，1.5倍行距","scoreMax":25},{"title":"页面设置与页眉页脚","type":"office","description":"设置A4纸张、上下边距2.5cm、左右边距2cm，添加页眉「实训报告」、页脚页码","scoreMax":25},{"title":"插入表格","type":"office","description":"插入一个4行3列的表格，设置边框样式，合并标题行","scoreMax":25},{"title":"生成目录","type":"office","description":"为文档中的标题应用「标题1」「标题2」样式，在文档开头自动生成目录","scoreMax":25}]',
 '[{"dimension":"process_quality","dimension_label":"操作规范","weight":0.4,"criteria":[{"level":"A","label":"优秀","description":"排版符合规范要求"}]},{"dimension":"product_result","dimension_label":"文档效果","weight":0.6,"criteria":[{"level":"A","label":"优秀","description":"文档格式美观规范"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板8: 办公应用基础 — Excel 公式与函数
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('Excel 常用函数应用', '掌握SUM/AVERAGE/MAX/MIN/IF/VLOOKUP等常用函数', '办公应用基础', 'office',
 '[{"title":"数据录入与格式","type":"excel","description":"创建学生成绩表，包含姓名/语文/数学/英语/计算机四列成绩，设置表头加粗居中","scoreMax":15,"config":{"checkpoints":[{"type":"format","target":"A1:E1","expected":"bold","desc":"表头加粗","score":5}]}},{"title":"SUM与AVERAGE函数","type":"excel","description":"使用SUM函数计算每位学生的总分，使用AVERAGE函数计算各科平均分","scoreMax":25,"config":{"checkpoints":[{"type":"formula","target":"E2","expected":"SUM","desc":"使用SUM函数计算总分","score":10},{"type":"formula","target":"B11","expected":"AVERAGE","desc":"使用AVERAGE计算平均分","score":10}]}},{"title":"MAX/MIN/IF函数","type":"excel","description":"使用MAX和MIN找出最高分和最低分，使用IF函数判断及格情况","scoreMax":25,"config":{"checkpoints":[{"type":"formula","target":"F2","expected":"IF","desc":"使用IF函数判断及格","score":10}]}},{"title":"VLOOKUP查询","type":"excel","description":"使用VLOOKUP函数根据姓名查找对应学生的成绩","scoreMax":20,"config":{"checkpoints":[{"type":"formula","target":"H2","expected":"VLOOKUP","desc":"使用VLOOKUP函数","score":10}]}},{"title":"创建图表","type":"excel","description":"根据成绩数据创建柱形图，设置图表标题和数据标签","scoreMax":15,"config":{"checkpoints":[{"type":"chart","desc":"创建柱形图","score":10}]}}]',
 '[{"dimension":"process_quality","dimension_label":"函数使用","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"函数语法和参数正确"}]},{"dimension":"product_result","dimension_label":"结果准确性","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"计算结果正确，图表清晰"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板9: 办公应用基础 — PPT 演示文稿
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('PPT 演示文稿制作', '掌握幻灯片创建、母版应用、动画效果和放映设置', '办公应用基础', 'office',
 '[{"title":"创建幻灯片结构","type":"ppt","description":"创建至少5张幻灯片，包含标题页、目录页、3个内容页和结束页","scoreMax":20,"config":{"checkpoints":[{"id":"1","type":"slide_count","expected":"5","desc":"至少5张幻灯片","score":10}]}},{"title":"应用主题和母版","type":"ppt","description":"为演示文稿选择一个合适的设计主题，修改母版中的字体和颜色方案","scoreMax":20,"config":{"checkpoints":[{"id":"2","type":"master","desc":"应用幻灯片母版","score":10}]}},{"title":"添加动画效果","type":"ppt","description":"为至少3个对象添加进入动画，设置合适的动画时序","scoreMax":20,"config":{"checkpoints":[{"id":"3","type":"animation","desc":"包含动画效果","score":10}]}},{"title":"设置切换效果","type":"ppt","description":"为每张幻灯片设置不同的切换效果","scoreMax":20,"config":{"checkpoints":[{"id":"4","type":"transition","desc":"包含切换效果","score":10}]}},{"title":"排练计时与放映","type":"ppt","description":"使用排练计时功能设置自动播放时间，测试幻灯片放映","scoreMax":20}]',
 '[{"dimension":"process_quality","dimension_label":"设计规范","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"设计规范美观"}]},{"dimension":"product_result","dimension_label":"演示效果","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"动画和切换流畅"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);


-- 模板11: 信息技术应用基础 — 计算机硬件识别
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('计算机硬件部件识别', '识别计算机主机内部主要硬件部件及其功能参数', '信息技术应用基础', 'operation',
 '[{"title":"CPU识别","type":"choice","description":"识别CPU的外观特征，了解主频、核心数等关键参数","scoreMax":20},{"title":"主板与接口","type":"choice","description":"识别主板上的CPU插槽、内存插槽、PCIe插槽、SATA接口等","scoreMax":20},{"title":"内存与硬盘","type":"choice","description":"区分DDR4/DDR5内存、SSD/HDD硬盘的外观和接口","scoreMax":20},{"title":"显卡与电源","type":"choice","description":"识别独立显卡与集成显卡，了解电源功率计算","scoreMax":20},{"title":"整机组装顺序","type":"text","description":"写出计算机整机组装的正确顺序和注意事项","scoreMax":20}]',
 '[{"dimension":"process_quality","dimension_label":"识别准确度","weight":0.6,"criteria":[{"level":"A","label":"优秀","description":"所有部件识别正确"}]},{"dimension":"product_result","dimension_label":"组装理解","weight":0.4,"criteria":[{"level":"A","label":"优秀","description":"组装顺序正确，逻辑清晰"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板12: 办公应用基础 — Excel 数据透视表
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('Excel 数据分析与透视表', '掌握排序、筛选、分类汇总和数据透视表', '办公应用基础', 'office',
 '[{"title":"数据排序","type":"excel","description":"对销售数据按销售额降序排列","scoreMax":20},{"title":"自动筛选","type":"excel","description":"使用自动筛选功能，筛选出特定地区的销售记录","scoreMax":20},{"title":"分类汇总","type":"excel","description":"按产品类别进行分类汇总，计算各类别的销售额合计","scoreMax":25},{"title":"创建数据透视表","type":"excel","description":"创建数据透视表，以地区为行、产品类别为列、销售额为值","scoreMax":35}]',
 '[{"dimension":"process_quality","dimension_label":"操作技能","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"操作步骤正确"}]},{"dimension":"product_result","dimension_label":"分析结果","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"数据分析结果准确"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板13: 网络应用基础 — OSI七层模型
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('OSI七层模型与TCP/IP协议栈', '掌握OSI七层模型各层功能和TCP/IP四层对应关系', '网络应用基础', 'design',
 '[{"title":"绘制OSI模型","type":"text","description":"画出OSI七层模型结构图，标注每层名称（应用层/表示层/会话层/传输层/网络层/数据链路层/物理层）","scoreMax":20},{"title":"各层功能","type":"text","description":"简述每层的主要功能和典型协议或设备","scoreMax":25},{"title":"TCP/IP对比","type":"text","description":"画出TCP/IP四层模型，标注与OSI七层的对应关系","scoreMax":25},{"title":"数据封装过程","type":"text","description":"描述数据从应用层到物理层的封装过程（每一层添加什么头部信息）","scoreMax":30}]',
 '[{"dimension":"process_quality","dimension_label":"理论理解","weight":0.6,"criteria":[{"level":"A","label":"优秀","description":"OSI模型理解准确，对应关系正确"}]},{"dimension":"product_result","dimension_label":"表达能力","weight":0.4,"criteria":[{"level":"A","label":"优秀","description":"描述清晰，图示规范"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板14: 办公应用基础 — Word 邮件合并
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('Word 邮件合并', '掌握邮件合并三要素：主文档、数据源、合并域', '办公应用基础', 'office',
 '[{"title":"准备数据源","type":"excel","description":"在Excel中创建数据源，包含姓名/单位/地址等字段","scoreMax":20},{"title":"创建主文档","type":"office","description":"在Word中创建邀请函模板，设置页面和格式","scoreMax":25},{"title":"插入合并域","type":"office","description":"使用邮件合并功能，在文档中插入姓名、单位等合并域","scoreMax":30},{"title":"完成合并","type":"text","description":"预览合并结果，完成邮件合并并保存","scoreMax":25}]',
 '[{"dimension":"process_quality","dimension_label":"操作技能","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"三要素配置正确"}]},{"dimension":"product_result","dimension_label":"合并效果","weight":0.5,"criteria":[{"level":"A","label":"优秀","description":"合并结果正确完整"}]}]',
 'DUAL_DIMENSION', 'SYSTEM', 0);

-- 模板10: Access — 数据表创建与查询（SQL内的单引号已用双写转义）
INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
('Access 数据表创建与SQL查询', '掌握数据库表创建、字段属性设置和SQL查询语句', 'Access', 'operation',
 '[{"title":"分析数据需求","type":"text","description":"设计学生管理系统数据库，列出需要的表和字段","scoreMax":20},{"title":"创建数据表","type":"sql","description":"使用CREATE TABLE创建学生表并设置主键","scoreMax":25,"config":{"schema":"CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(50), gender CHAR(2), class VARCHAR(30));","expectedSql":"CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(50), gender CHAR(2), class VARCHAR(30))"}},{"title":"插入与查询","type":"sql","description":"插入3条学生记录，查询所有男生","scoreMax":25,"config":{"schema":"CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(50), gender CHAR(2), class VARCHAR(30));INSERT INTO students VALUES(1, ''张三'', ''男'', ''计算机1班'');INSERT INTO students VALUES(2, ''李四'', ''女'', ''计算机1班'');INSERT INTO students VALUES(3, ''王五'', ''男'', ''计算机2班'');","expectedSql":"SELECT * FROM students WHERE gender=''男'' ORDER BY name"}},{"title":"UPDATE修改","type":"sql","description":"将张三的班级改为计算机2班","scoreMax":25,"config":{"schema":"CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(50), class VARCHAR(30));INSERT INTO students VALUES(1, ''张三'', ''计算机1班'');","expectedSql":"SELECT * FROM students WHERE class=''计算机2班''"}},{"title":"DELETE删除","type":"sql","description":"删除指定班级的所有记录","scoreMax":25,"config":{"schema":"CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(50), class VARCHAR(30));INSERT INTO students VALUES(1, ''待删除'', ''计算机2班'');INSERT INTO students VALUES(2, ''保留'', ''计算机1班'');","expectedSql":"SELECT COUNT(*) FROM students"}}]',
 '[{"dimension":"skill_","dimension_label":"SQL技能","weight":0.6,"criteria":[{"level":"A","label":"优秀","description":"SQL语法正确，查询结果准确"}]},{"dimension":"prof_","dimension_label":"设计规范","weight":0.4,"criteria":[{"level":"A","label":"优秀","description":"表设计合理，字段类型恰当"}]}]',
 'COMPETITION', 'SYSTEM', 0);
