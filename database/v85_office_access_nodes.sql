-- ============================================================================
-- v85: 办公应用基础 + Access — 知识节点树 (subject_id=6,17)
-- 教材: 《办公应用基础》陈继红/张岚/蔡慧, 高教社 2021, ISBN 9787040559606
-- 结构: 4单元 → 25任务 → 知识点(对照考纲提取)
-- subject=6(单元1-3) + subject=17(单元4)
-- ============================================================================

-- ═══════════════════════════════════════════
-- Part 1: 办公应用基础 (subject_id=6)
-- ═══════════════════════════════════════════

-- Level 2: 单元 (parent_id=6)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (6, 6, 2, '单元1 文字处理(Word 2010)', 1);
SET @wu1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (6, 6, 2, '单元2 电子表格处理(Excel 2010)', 2);
SET @wu2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (6, 6, 2, '单元3 演示文稿应用(PPT 2010)', 3);
SET @wu3 = LAST_INSERT_ID();

-- Level 3: 任务 — 单元1 文字处理
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu1, 6, 3, '任务1 录入和编辑文档', 1);
SET @wt1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu1, 6, 3, '任务2 设置文档格式', 2);
SET @wt2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu1, 6, 3, '任务3 使用表格', 3);
SET @wt3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu1, 6, 3, '任务4 图文混排', 4);
SET @wt4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu1, 6, 3, '任务5 完善文档', 5);
SET @wt5 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu1, 6, 3, '任务6 使用邮件合并', 6);
SET @wt6 = LAST_INSERT_ID();

-- Level 3: 任务 — 单元2 电子表格处理
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu2, 6, 3, '任务1 采集数据', 1);
SET @et1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu2, 6, 3, '任务2 修饰工作表', 2);
SET @et2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu2, 6, 3, '任务3 使用公式和函数', 3);
SET @et3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu2, 6, 3, '任务4 分析管理数据', 4);
SET @et4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu2, 6, 3, '任务5 制作图表', 5);
SET @et5 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu2, 6, 3, '任务6 打印工作表', 6);
SET @et6 = LAST_INSERT_ID();

-- Level 3: 任务 — 单元3 演示文稿应用
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu3, 6, 3, '任务1 创建演示文稿', 1);
SET @pt1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu3, 6, 3, '任务2 使用幻灯片母版', 2);
SET @pt2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu3, 6, 3, '任务3 丰富演示文稿内容', 3);
SET @pt3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu3, 6, 3, '任务4 使用动画效果', 4);
SET @pt4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wu3, 6, 3, '任务5 放映和发布演示文稿', 5);
SET @pt5 = LAST_INSERT_ID();

-- ══════════════════════════════════════════
-- Level 4: 知识点 — 办公应用基础
-- ══════════════════════════════════════════

-- ── 单元1 > 任务1: 录入和编辑文档 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, 'Word 2010工作界面(标题栏/功能区/标尺/编辑区/状态栏)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '文档视图(页面/阅读版式/Web版式/大纲/草稿)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '文档基本操作(新建/打开/保存/另存为/关闭)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '文档保护(打开密码/修改密码/限制编辑)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '打印设置(打印预览/打印机/页码范围/份数)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '光标定位与文本选择(鼠标/键盘/快捷键)', 6);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '文本编辑(插入与改写/移动与复制/删除)', 7);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '查找与替换(Ctrl+F/H,支持格式替换和特殊格式)', 8);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '撤消(Ctrl+Z)与恢复(Ctrl+Y)', 9);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt1, 6, 4, '插入特殊符号/日期和时间/页码', 10);

-- ── 单元1 > 任务2: 设置文档格式 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '字符格式(字体/字号/字形B/I/U/颜色/字符间距/文字效果)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '段落格式(对齐方式/缩进/间距/行距)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '项目符号/编号列表/多级列表', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '格式刷的使用(单击刷一次/双击多次)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '样式(内置样式套用与修改)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '边框和底纹(字符/段落/页面)', 6);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '页面格式(纸张大小/方向/页边距/分栏)', 7);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '页眉/页脚/页码(首页不同/奇偶页不同)', 8);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '分页符(Ctrl+Enter)/分节符/分栏符', 9);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt2, 6, 4, '目录自动生成(基于标题样式)', 10);

-- ── 单元1 > 任务3: 使用表格 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt3, 6, 4, '表格的创建(插入表格/绘制表格)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt3, 6, 4, '表格操作(插入删除行列/合并拆分单元格)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt3, 6, 4, '表格属性(行高/列宽/单元格对齐)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt3, 6, 4, '表格边框/底纹/样式套用', 4);

-- ── 单元1 > 任务4: 图文混排 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt4, 6, 4, '图片插入与文字环绕方式(嵌入型/四周型/紧密型/浮于上方/衬于下方)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt4, 6, 4, '形状(自选图形)插入与编辑', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt4, 6, 4, '艺术字插入与设置', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt4, 6, 4, '文本框(横排/竖排)使用', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt4, 6, 4, 'SmartArt图形(流程/层次/循环/关系)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt4, 6, 4, '公式编辑器与图表插入', 6);

-- ── 单元1 > 任务5: 完善文档 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt5, 6, 4, '修订功能(跟踪文档修改)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt5, 6, 4, '批注(添加对文档内容的评论)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt5, 6, 4, '文档比较与合并', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt5, 6, 4, '脚注和尾注', 4);

-- ── 单元1 > 任务6: 使用邮件合并 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt6, 6, 4, '邮件合并(主文档+数据源→批量生成)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt6, 6, 4, '数据源类型(Excel表格/Access数据库/Outlook联系人)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@wt6, 6, 4, '合并域插入→预览→完成合并', 3);

-- ── 单元2 > 任务1: 采集数据 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et1, 6, 4, 'Excel 2010工作界面(名称栏/编辑栏/工作表标签/单元格)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et1, 6, 4, '工作簿/工作表/单元格概念', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et1, 6, 4, '工作表操作(新建/删除/重命名/复制移动/标签颜色)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et1, 6, 4, '视图(普通/分页预览/页面布局)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et1, 6, 4, '数据类型(文本/数值/日期时间/货币/百分比)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et1, 6, 4, '数据填充(填充柄/自定义序列/等差等比序列)', 6);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et1, 6, 4, '数据有效性(限制输入范围和类型)', 7);

-- ── 单元2 > 任务2: 修饰工作表 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et2, 6, 4, '单元格格式(Ctrl+1:数字/对齐/字体/边框/填充)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et2, 6, 4, '合并单元格与自动换行', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et2, 6, 4, '表格格式套用', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et2, 6, 4, '条件格式(突出显示/数据条/色阶/图标集)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et2, 6, 4, '冻结窗格(冻结首行/首列/指定位置)', 5);

-- ── 单元2 > 任务3: 使用公式和函数 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, '公式构成(以=开头/运算符/操作数)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, '运算符(算术+ - * / ^ /比较= > < >= <= <>/文本&/引用: ,)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, '单元格引用(相对A1/绝对$A$1/混合$A1/A$1)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'SUM函数(求和)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'AVERAGE函数(平均值)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'MAX/MIN函数(最大/最小值)', 6);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'COUNT/COUNTA/COUNTIF函数(计数)', 7);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'IF函数(条件判断)', 8);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'SUMIF函数(条件求和)', 9);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'VLOOKUP函数(垂直查找)', 10);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'RANK函数(排名)', 11);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'LEFT/MID/RIGHT函数(文本截取)', 12);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et3, 6, 4, 'TODAY/NOW/YEAR/MONTH/DAY函数(日期)', 13);

-- ── 单元2 > 任务4: 分析管理数据 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et4, 6, 4, '排序(单字段/多字段/自定义排序)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et4, 6, 4, '筛选(自动筛选/按条件筛选/高级筛选)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et4, 6, 4, '分类汇总(先排序后汇总)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et4, 6, 4, '合并计算(多工作表数据汇总)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et4, 6, 4, '数据透视表(行/列/值/筛选区域,值汇总方式)', 5);

-- ── 单元2 > 任务5: 制作图表 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et5, 6, 4, '图表组成(图表区/绘图区/数据系列/坐标轴/图例/标题)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et5, 6, 4, '常用图表类型(柱形图/折线图/饼图/条形图)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et5, 6, 4, '图表操作(创建/移动/调整大小/更改类型)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et5, 6, 4, '图表格式(样式/坐标轴格式/数据标签)', 4);

-- ── 单元2 > 任务6: 打印工作表 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et6, 6, 4, '页面设置(页边距/纸张方向/缩放比例)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et6, 6, 4, '打印标题(顶端标题行/左端标题列)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et6, 6, 4, '分页符(插入/删除)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@et6, 6, 4, '工作表保护(保护工作簿/保护工作表/隐藏)', 4);

-- ── 单元3 > 任务1: 创建演示文稿 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt1, 6, 4, 'PowerPoint 2010工作界面与视图(普通/浏览/阅读/放映)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt1, 6, 4, '演示文稿操作(新建/打开/保存/另存为)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt1, 6, 4, '幻灯片操作(新建/复制/移动/删除/版式选择)', 3);

-- ── 单元3 > 任务2: 使用幻灯片母版 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt2, 6, 4, '幻灯片版式(标题幻灯片/标题和内容/两栏内容/空白)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt2, 6, 4, '幻灯片母版(统一设置字体/背景/Logo)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt2, 6, 4, '主题/模板(预定义配色方案和字体组合)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt2, 6, 4, '背景设置(纯色/渐变/图片/纹理)', 4);

-- ── 单元3 > 任务3: 丰富演示文稿内容 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt3, 6, 4, '文字(占位符输入/文本框插入/字体段落)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt3, 6, 4, '图片/艺术字/形状插入', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt3, 6, 4, 'SmartArt图形/图表插入', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt3, 6, 4, '音频/视频插入', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt3, 6, 4, '超链接(链接到本文档/网页/其他文件)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt3, 6, 4, '动作按钮(前进/后退/首页/结束)', 6);

-- ── 单元3 > 任务4: 使用动画效果 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt4, 6, 4, '动画类型(进入/强调/退出/动作路径)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt4, 6, 4, '幻灯片切换效果(切出/淡出/推进/覆盖/揭开)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt4, 6, 4, '动画时序设置(单击开始/与上一动画同时/上一动画之后)', 3);

-- ── 单元3 > 任务5: 放映和发布演示文稿 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt5, 6, 4, '放映方式(从头开始F5/从当前开始Shift+F5)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt5, 6, 4, '排练计时(记录每张幻灯片演示时间)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt5, 6, 4, '自定义放映(选择幻灯片子集)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@pt5, 6, 4, '演示文稿打包(将文件打包到文件夹或CD)', 4);

-- ═══════════════════════════════════════════
-- Part 2: Access 数据库 (subject_id=17)
-- ═══════════════════════════════════════════

-- Level 2: 单元 (parent_id=7, root is Access node)
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (7, 17, 2, '单元4 数据库应用(Access 2010)', 1);
SET @au1 = LAST_INSERT_ID();

-- Level 3: 任务
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务1 认识数据库系统', 1);
SET @at1 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务2 建立数据表', 2);
SET @at2 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务3 设置字段属性', 3);
SET @at3 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务4 浏览数据', 4);
SET @at4 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务5 创建表间关系', 5);
SET @at5 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务6 创建选择查询', 6);
SET @at6 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务7 创建操作查询', 7);
SET @at7 = LAST_INSERT_ID();
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@au1, 17, 3, '任务8 创建SQL查询', 8);
SET @at8 = LAST_INSERT_ID();

-- ══════════════════════════════════════════
-- Level 4: 知识点 — Access
-- ══════════════════════════════════════════

-- ── 任务1: 认识数据库系统 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '数据(Data)/数据库(DB)/数据库管理系统(DBMS)/数据库系统(DBS)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '数据库系统的特点(数据结构化/共享性高/独立性高)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '数据模型三层次(概念模型→逻辑模型→物理模型)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '实体/属性/码/域/实体型/实体集', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '实体间联系(一对一1:1/一对多1:n/多对多m:n)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '关系型数据库(关系/元组/属性/候选码/主键/外键)', 6);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '三种关系运算(选择Selection/投影Projection/连接Join)', 7);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, 'Access 2010六大对象(表/查询/窗体/报表/宏/模块)', 8);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, 'Access文件扩展名(.accdb/.mdb)', 9);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at1, 17, 4, '国产数据库(达梦/人大金仓/南大通用/神通/OceanBase)', 10);

-- ── 任务2: 建立数据表 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at2, 17, 4, 'Access常用数据类型(文本/备注/数字/日期时间/货币/自动编号/是/否/OLE对象/超链接/附件)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at2, 17, 4, '创建数据表方法(数据表视图/设计视图/表模板)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at2, 17, 4, '修改表结构(添加删除字段/修改数据类型/调整字段顺序)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at2, 17, 4, '数据库运算符(算术+ - * / \\ Mod ^ /关系= > < >= <= <>/逻辑AND OR NOT)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at2, 17, 4, '特殊运算符(BETWEEN...AND/IN/LIKE/IS NULL)', 5);

-- ── 任务3: 设置字段属性 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at3, 17, 4, '字段大小(限制文本长度/数字范围)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at3, 17, 4, '格式属性(控制数据显示格式)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at3, 17, 4, '输入掩码(限制输入格式)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at3, 17, 4, '默认值与有效性规则(限制输入条件)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at3, 17, 4, '必填字段/允许零长度字符串/索引/标题', 5);

-- ── 任务4: 浏览数据 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at4, 17, 4, '记录浏览(数据表视图/导航按钮/定位)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at4, 17, 4, '记录编辑(修改/撤消Esc/删除Delete)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at4, 17, 4, '数据表格式设置(行高列宽/冻结列/隐藏列/字体/网格线)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at4, 17, 4, '数据排序(升序/降序/多字段排序)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at4, 17, 4, '数据筛选(按选定内容筛选/按窗体筛选/高级筛选)', 5);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at4, 17, 4, '查找与替换/数据导入导出(Excel↔Access)', 6);

-- ── 任务5: 创建表间关系 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at5, 17, 4, '主键(Primary Key)概念与创建', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at5, 17, 4, '索引(Index)概念与类型(有重复/无重复)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at5, 17, 4, '表间关系类型(一对一/一对多/多对多需中间表)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at5, 17, 4, '参照完整性(级联更新/级联删除)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at5, 17, 4, '连接类型(内部连接/左外部连接/右外部连接)', 5);

-- ── 任务6: 创建选择查询 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at6, 17, 4, '查询的五大类型(选择查询/参数查询/交叉表查询/操作查询/SQL查询)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at6, 17, 4, '创建查询方法(查询向导/设计视图QBE/SQL语句)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at6, 17, 4, '查询条件(准则)设置(同行AND/不同行OR)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at6, 17, 4, '查询中的计算字段(表达式字段)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at6, 17, 4, '分组统计(总计行Group By/Sum/Avg/Count/Max/Min)', 5);

-- ── 任务7: 创建操作查询 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at7, 17, 4, '更新查询(批量修改数据)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at7, 17, 4, '删除查询(批量删除记录)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at7, 17, 4, '追加查询(批量添加记录)', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at7, 17, 4, '生成表查询(将查询结果生成新表)', 4);

-- ── 任务8: 创建SQL查询 ──
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at8, 17, 4, 'SELECT查询语句(SELECT/FROM/WHERE/GROUP BY/HAVING/ORDER BY)', 1);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at8, 17, 4, 'CREATE TABLE创建表语句(字段名/数据类型/约束/PRIMARY KEY)', 2);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at8, 17, 4, 'INSERT INTO插入语句', 3);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at8, 17, 4, 'UPDATE更新语句(SET/WHERE)', 4);
INSERT INTO knowledge_nodes (parent_id, subject_id, level, name, sort_order) VALUES (@at8, 17, 4, 'DELETE删除语句(FROM/WHERE)', 5);

-- 打印节点清单
SELECT '=== 办公应用基础 & Access 节点ID清单 ===' AS '';
SELECT id, subject_id, parent_id, level, name FROM knowledge_nodes WHERE subject_id IN (6, 17) ORDER BY subject_id, level, id;
