-- ============================================================================
-- v145: 考纲结构化元数据 — 用 JSON 替代脆弱的 Markdown 正则解析
-- 幂等执行（列已存在则忽略错误，UPDATE 可重复执行）
-- ============================================================================
SET NAMES utf8mb4;

ALTER TABLE exam_syllabus ADD COLUMN syllabus_meta TEXT COMMENT '结构化考纲元数据JSON';

-- 语文：背诵篇目 + 分值分布 + 作文主题
UPDATE exam_syllabus SET syllabus_meta = JSON_OBJECT(
  'reciteList', '静女/采薇/寡人之于国也/劝学/师说/将进酒/琵琶行/念奴娇·赤壁怀古',
  'scoreDistribution', '基础知识与运用约20%(30分)/现代文阅读约40%(60分)/文言文阅读约8%(12分)/写作约32%(48分)',
  'compositionThemes', '工匠精神/积累坚持/责任担当/创新突破',
  'examDuration', '150分钟',
  'totalScore', '150'
) WHERE subject_id=20;

-- 数学：分值分布 + 解答题顺序
UPDATE exam_syllabus SET syllabus_meta = JSON_OBJECT(
  'scoreDistribution', '集合5-8%/不等式5-10%/函数10-12%/指对数5-8%/三角14%/数列8-10%/向量5-8%/立几10%/解析几何18%/概率5-8%/导数选考5%',
  'answerOrder', '函数→三角→数列/解析→解析几何→立几→应用/导数',
  'calculationRule', '计算题结果应为整数或简分数，避免复杂小数',
  'examDuration', '120分钟',
  'totalScore', '150'
) WHERE subject_id=22;

-- 英语：单选考点排布 + 阅读话题顺序 + 题型分值
UPDATE exam_syllabus SET syllabus_meta = JSON_OBJECT(
  'singleChoiceOrder', '情景交际→冠词→代词→介词→主谓一致→动词短语→连词→反意/感叹→动词辨析→情态→非谓语→名从→定从→虚拟/倒装→综合',
  'readingTopics', '校园→社会→科技→人物→实用',
  'languageAppNote', '多模态语篇(标识牌/广告/票务/地图)，非传统完形填空',
  'examDuration', '120分钟',
  'totalScore', '100'
) WHERE subject_id=24;

SELECT 'v145: syllabus_meta populated for subject 20,22,24' AS result;
