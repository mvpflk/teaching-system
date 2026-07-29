-- ============================================================================
-- v150: 英语[职高]考纲 content 修正 — 对齐2014官方考纲+2024真题
-- 修正8项偏差: 时长60→120分, 删除翻译, 阅读40%/写作15%/词汇语法30%,
--   难度40/30/20/10, 增加补全对话/短文改错, 增加第I/II卷结构
-- 幂等: UPDATE 可重复执行
-- ============================================================================
SET NAMES utf8mb4;

UPDATE exam_syllabus
SET content = '一、考试形式与试卷结构：闭卷笔试，考试时间120分钟，满分100分。试卷分第I卷（客观题，70分）和第II卷（主观题，30分）。第I卷含三部分：I.单项选择（15题×1分=15分）、II.完形填空/语言应用（10题×1.5分=15分,多模态语篇如标识牌/广告/票务/地图）、III.阅读理解（20题×2分=40分）。第II卷含三部分：I.补全对话（5题×1分=5分）、II.短文改错（5题×2分=10分）、III.书面表达（1题×15分=15分）。难度分布：容易约40%/较易约30%/中等约20%/较难约10%。词汇量约2200词（基础1700+拓展500）。参照教材：《英语》（基础模块）上/下册，高等教育出版社。'

'二、词汇积累：高频核心300词(动词be/have/do/make/take/get,名词time/way/day/people/place,形容词good/bad/big/small,介词in/on/at/to/for/of,连词and/but/because)；考试核心500词(动词辨析spend/cost/take/pay,名词辨析chance/opportunity/ability,词组take care of/look forward to/be used to/give up/turn on-off,易混淆词borrow/lend/bring/take/hear/listen)；考纲拓展词(阅读拓展词如environment/technology/experience,写作常用词如sincerely/in my opinion/looking forward to)。'

'三、语法专项(十大专题)：时态语态(一般现在时/过去时/将来时,进行时,完成时,被动语态)；非谓语动词(不定式to do作主语/宾语/目的状语,动名词V-ing作主语/宾语,分词作定语和状语)；定语从句(关系代词that/which/who/whom/whose,关系副词when/where/why,限制性与非限制性)；名词性从句(宾语从句语序,主语从句it作形式主语,表语从句,同位语从句)；状语从句(时间/条件/原因/让步/目的)；主谓一致(语法一致/就近原则/意义一致)；情态动词(基本用法can/could/must/may + 推测用法must have done/can''t have done)；虚拟语气(条件句与现在/过去事实相反)；情景交际(邀请Would you like to,感谢Thank you for,问路How can I get to,建议You''d better/Why not)。'

'四、阅读理解(共20题×2分=40分,题材含校园/社会/科技/人物/生活)：细节理解题(直接定位文中信息+Wh-问题细节检索)；推理判断题(根据上下文推断隐含信息,信号词probably/suggest/infer/imply)；主旨大意题(概括段落/全文主旨,首句末句定位中心句)；词义猜测题(上下文推断,同义/反义/举例/解释线索)。'

'五、补全对话(5题×1分=5分)：常考交际功能(问候与告别/感谢与道歉/预约与邀请/祝愿与祝贺/请求与提供帮助/同意与不同意/建议与劝告/禁止与警告)；技能要求(根据上下文判断对话走向,选择符合英语交际习惯的应答语,识别多余干扰选项)。'

'六、短文改错(5题×2分=10分,约100词短文)：常考错误类型(动词时态语态/名词单复数/主谓不一致/冠词多余缺失误用/介词搭配/形容词副词混淆/连词误用/非谓语形式错误)；技能要求(通读全文把握时态基调,逐句检查语法一致性,注意逻辑关系)。'

'七、写作(1题×15分=15分,80-100词)：应用文写作(书信格式称呼+正文+结束语+署名+日期,通知格式标题+正文+时间+地点+要求+单位+日期,常考类型感谢信/邀请信/请假条/通知/便条)；话题写作(校园生活/家庭朋友/兴趣爱好/环境保护/未来规划,观点表达In my opinion/I think,逻辑衔接First/Besides/Finally/However/Therefore)。',
    version = '2.1',
    updated_at = NOW()
WHERE subject_id = (SELECT id FROM dict_subject WHERE subject_name = '英语[职高]' AND status = 1 LIMIT 1)
  AND exam_type = 'DUIKOU'
  AND title LIKE '%对口升学%英语%';

-- 同步更新 syllabus_meta: 更新分值分布
UPDATE exam_syllabus
SET syllabus_meta = JSON_SET(
    syllabus_meta,
    '$.scoreDistribution',
    '英语知识运用约30%(30分:单选15+语言应用15)/阅读理解约40%(40分)/补全对话约5%(5分)/短文改错约10%(10分)/书面表达约15%(15分)'
)
WHERE subject_id = (SELECT id FROM dict_subject WHERE subject_name = '英语[职高]' AND status = 1 LIMIT 1)
  AND exam_type = 'DUIKOU';

SELECT 'v150: English syllabus content & meta corrected to v2.1 (aligned with 2014 syllabus + 2024 exam)' AS result;
