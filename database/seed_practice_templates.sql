-- ============================================================================
-- 系统预置实训模板（信息技术应用基础）
-- 需要 mysql --default-character-set=utf8mb4 执行
-- ============================================================================

INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
(
  'Word排版实训 — 制作个人简历',
  '学生使用Word完成一份标准个人简历的制作，包括页面设置、表格布局、文字格式化、图片插入等技能点。',
  '信息技术应用基础',
  'word',
  '[{"title":"设置页面格式","description":"设置A4纸张，页边距上下2.5cm、左右3cm，添加页眉\"个人简历\"","attachmentMode":"REFERENCE","referenceImages":[],"referenceAttachments":[]},{"title":"制作简历表格","description":"使用表格布局简历结构：个人信息区、教育背景区、技能特长区、实习经历区。合并单元格调整布局。","attachmentMode":"TEMPLATE","referenceImages":[],"referenceAttachments":[]},{"title":"填写并格式化内容","description":"填写个人真实信息，设置字体（标题黑体三号、正文宋体小四），行间距1.5倍","attachmentMode":"REFERENCE","referenceImages":[],"referenceAttachments":[]},{"title":"插入证件照","description":"在个人信息区插入一张证件照，调整大小和位置，设置文字环绕方式为\"四周型\"","attachmentMode":"REFERENCE","referenceImages":[],"referenceAttachments":[]},{"title":"美化排版与提交","description":"添加边框和底纹美化表格，检查整体排版，确认无误后导出PDF提交","attachmentMode":"REFERENCE","referenceImages":[],"referenceAttachments":[]}]',
  '[{"dimension":"process_quality","dimensionLabel":"过程质量","weight":0.5,"sortOrder":0,"criteria":"[{\"level\":0,\"label\":\"未完成\",\"description\":\"未提交或仅完成了不到30%\"},{\"level\":1,\"label\":\"初级\",\"description\":\"页面设置基本正确，表格有结构\"},{\"level\":2,\"label\":\"中级\",\"description\":\"页面设置正确，表格布局合理\"},{\"level\":3,\"label\":\"良好\",\"description\":\"所有步骤完成，排版整齐\"},{\"level\":4,\"label\":\"优秀\",\"description\":\"排版精美，细节到位\"},{\"level\":5,\"label\":\"卓越\",\"description\":\"可作为简历模板使用\"}]"},{"dimension":"product_result","dimensionLabel":"作品成果","weight":0.5,"sortOrder":1,"criteria":"[{\"level\":0,\"label\":\"未提交\",\"description\":\"未提交最终作品\"},{\"level\":1,\"label\":\"不合格\",\"description\":\"格式严重错误\"},{\"level\":2,\"label\":\"基本合格\",\"description\":\"信息完整但排版较乱\"},{\"level\":3,\"label\":\"合格\",\"description\":\"信息完整、排版规范\"},{\"level\":4,\"label\":\"良好\",\"description\":\"美观大方、重点突出\"},{\"level\":5,\"label\":\"优秀\",\"description\":\"专业级别简历\"}]"}]',
  'DUAL_DIMENSION',
  'SYSTEM',
  0
);

INSERT INTO practice_templates (title, description, subject, category, steps_json, rubrics_json, scoring_model, source, use_count) VALUES
(
  'Excel数据处理 — 学生成绩统计分析',
  '使用Excel完成学生成绩表的制作与数据分析，包括数据录入、公式计算、图表制作等核心技能。',
  '信息技术应用基础',
  'excel',
  '[{"title":"创建成绩表结构","description":"建立包含学号、姓名、语文、数学、英语、专业课、总分、平均分、排名列的成绩表","attachmentMode":"TEMPLATE","referenceImages":[],"referenceAttachments":[]},{"title":"输入基础数据","description":"输入至少15名学生的各科成绩数据（可使用随机合理数据）","attachmentMode":"REFERENCE","referenceImages":[],"referenceAttachments":[]},{"title":"使用公式计算","description":"使用SUM计算总分、AVERAGE计算平均分、RANK计算排名。注意使用绝对引用和相对引用。","attachmentMode":"TEMPLATE","referenceImages":[],"referenceAttachments":[]},{"title":"数据分析","description":"使用COUNTIF统计各科及格人数，MAX/MIN找各科最高最低分，AVERAGE计算各科平均分","attachmentMode":"REFERENCE","referenceImages":[],"referenceAttachments":[]},{"title":"制作图表","description":"制作各科平均分的柱状图，要求有标题、图例、数据标签","attachmentMode":"REFERENCE","referenceImages":[],"referenceAttachments":[]}]',
  '[{"dimension":"process_quality","dimensionLabel":"过程质量","weight":0.5,"sortOrder":0,"criteria":"[{\"level\":0,\"label\":\"未完成\",\"description\":\"表格结构不完整\"},{\"level\":1,\"label\":\"初级\",\"description\":\"表格结构正确，部分公式有误\"},{\"level\":2,\"label\":\"中级\",\"description\":\"公式基本正确，图表可用\"},{\"level\":3,\"label\":\"良好\",\"description\":\"公式正确，图表规范\"},{\"level\":4,\"label\":\"优秀\",\"description\":\"效率高，使用高级函数\"},{\"level\":5,\"label\":\"卓越\",\"description\":\"技巧娴熟，有创造力\"}]"},{"dimension":"product_result","dimensionLabel":"作品成果","weight":0.5,"sortOrder":1,"criteria":"[{\"level\":0,\"label\":\"未提交\",\"description\":\"未提交最终文件\"},{\"level\":1,\"label\":\"不合格\",\"description\":\"数据大量错误\"},{\"level\":2,\"label\":\"基本合格\",\"description\":\"数据基本正确\"},{\"level\":3,\"label\":\"合格\",\"description\":\"数据正确，有图表\"},{\"level\":4,\"label\":\"良好\",\"description\":\"数据准确，图表美观\"},{\"level\":5,\"label\":\"优秀\",\"description\":\"可作为成绩管理模板\"}]"}]',
  'DUAL_DIMENSION',
  'SYSTEM',
  0
);
