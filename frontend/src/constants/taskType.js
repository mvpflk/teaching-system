// 统一任务类型 — 对应后端 TaskCategory / ScoreType / 状态枚举
// 所有试卷/作业/题库/任务页面共用

// ═══════════════════════════════════════════
// 任务行为（教师视角3种）→ 后端映射为9种具体类型
// ═══════════════════════════════════════════
export const TASK_BEHAVIOR = {
  EXAM: 'EXAM',
  HOMEWORK: 'HOMEWORK',
  SURVEY: 'SURVEY',
}

export const TASK_BEHAVIOR_LABEL = {
  EXAM: '考试/测验',
  HOMEWORK: '作业/任务',
  SURVEY: '问卷调查',
}

export const TASK_BEHAVIOR_DESC = {
  EXAM: '限时答题、自动评客观题、防作弊',
  HOMEWORK: '文本/文件提交、迟交扣分、教师评分',
  SURVEY: '匿名作答、自动统计、可视化结果',
}

export const TASK_BEHAVIOR_ICON = {
  EXAM: '📝',
  HOMEWORK: '📋',
  SURVEY: '📊',
}

// 行为 → 具体类型数组（用于过滤）
export const BEHAVIOR_TO_TYPES = {
  EXAM: ['FORMATIVE', 'SUMMATIVE'],
  HOMEWORK: ['PRE_CLASS', 'IN_CLASS', 'AFTER_CLASS', 'MORAL', 'LABOR'],
  SURVEY: ['SURVEY'],
}

// 具体类型 → 行为（编辑模式回填）
export const TYPE_TO_BEHAVIOR = {
  FORMATIVE: 'EXAM', SUMMATIVE: 'EXAM',
  PRE_CLASS: 'HOMEWORK', IN_CLASS: 'HOMEWORK', AFTER_CLASS: 'HOMEWORK',
  MORAL: 'HOMEWORK', LABOR: 'HOMEWORK',
  SURVEY: 'SURVEY',
}

// 行为 → 默认评分体系
export const BEHAVIOR_DEFAULT_SCORE_TYPE = {
  EXAM: 'POINT_100',
  HOMEWORK: 'POINT_100',
  SURVEY: 'PASS_FAIL',
}

export const TASK_TYPES = {
  PRE_CLASS: 'PRE_CLASS',
  IN_CLASS: 'IN_CLASS',
  AFTER_CLASS: 'AFTER_CLASS',
  FORMATIVE: 'FORMATIVE',
  SUMMATIVE: 'SUMMATIVE',
  MORAL: 'MORAL',
  LABOR: 'LABOR',
  SURVEY: 'SURVEY',
  PRACTICE: 'PRACTICE',
}

export const TASK_TYPE_LABEL = {
  PRE_CLASS: '作业',
  IN_CLASS: '作业',
  AFTER_CLASS: '作业',
  FORMATIVE: '考试',
  SUMMATIVE: '考试',
  MORAL: '作业',
  LABOR: '作业',
  SURVEY: '问卷',
  PRACTICE: '实训',
}

// 细粒度标签 — 用于需要精确筛选的下拉框
export const TASK_TYPE_FILTER_LABEL = {
  PRE_CLASS: '作业·课前预习',
  IN_CLASS: '作业·课中活动',
  AFTER_CLASS: '作业·课后巩固',
  FORMATIVE: '考试·单元测验',
  SUMMATIVE: '考试·期末考试',
  MORAL: '作业·德育',
  LABOR: '作业·劳动',
  SURVEY: '问卷',
  PRACTICE: '实训',
  SIMULATION: '仿真',
}

export const TASK_TYPE_TAG = {
  PRE_CLASS: 'info',
  IN_CLASS: 'success',
  AFTER_CLASS: '',
  FORMATIVE: 'warning',
  SUMMATIVE: 'danger',
  MORAL: '',
  LABOR: 'success',
  SURVEY: 'info',
  PRACTICE: '',
  SIMULATION: 'warning',
}

// 任务类型图标标识 — TaskIcon.vue 用此映射查找 SVG
export const TASK_TYPE_ICON = {
  PRE_CLASS: 'PRE_CLASS',
  IN_CLASS: 'IN_CLASS',
  AFTER_CLASS: 'AFTER_CLASS',
  FORMATIVE: 'FORMATIVE',
  SUMMATIVE: 'SUMMATIVE',
  MORAL: 'MORAL',
  LABOR: 'LABOR',
  SURVEY: 'SURVEY',
  PRACTICE: 'PRACTICE',
}

export const SCORE_TYPES = {
  POINT_100: 'POINT_100',
  GRADE_5: 'GRADE_5',
  PASS_FAIL: 'PASS_FAIL',
  CUSTOM_RUBRIC: 'CUSTOM_RUBRIC',
}

export const SCORE_TYPE_LABEL = {
  POINT_100: '百分制',
  GRADE_5: '五级制',
  PASS_FAIL: '通过/不通过',
  CUSTOM_RUBRIC: '自定义评分细则(实训)',
}

export const TASK_STATUS = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  ONGOING: 'ONGOING',
  CLOSED: 'CLOSED',
}

export const TASK_STATUS_LABEL = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ONGOING: '进行中',
  CLOSED: '已关闭',
}

export const TASK_STATUS_TAG = {
  DRAFT: 'info',
  PUBLISHED: '',
  ONGOING: 'success',
  CLOSED: 'warning',
}

export const TARGET_TYPE_LABEL = {
  CLASS: '全班',
  GROUP: '小组',
  INDIVIDUAL: '个人',
}

export const SUBMISSION_STATUS = {
  PENDING: 'PENDING',
  SUBMITTED: 'SUBMITTED',
  GRADED: 'GRADED',
  RETURNED: 'RETURNED',
  EXEMPTED: 'EXEMPTED',
}

export const SUBMISSION_STATUS_LABEL = {
  PENDING: '未提交',
  SUBMITTED: '已提交',
  GRADED: '已评分',
  RETURNED: '退回修改',
  EXEMPTED: '已豁免',
}

export const SUBMISSION_STATUS_TAG = {
  PENDING: 'info',
  SUBMITTED: '',
  GRADED: 'success',
  RETURNED: 'warning',
  EXEMPTED: 'danger',
}

export const SOURCE_TYPE_LABEL = {
  HOMEWORK: '作业',
  EXAM: '考试',
  PRACTICAL: '实训',
  TASK: '任务',
}

// ── 问卷模板（预置 5 套常用问卷） ──
export const SURVEY_TEMPLATES = [
  { key: 'moral_eval', label: '📋 德育评价问卷', questions: [
    { type: 'radio', title: '你在本学期参与了哪些德育活动？', options: ['主题班会', '志愿者活动', '社区服务', '宪法/安全讲座', '其他'] },
    { type: 'radio', title: '你对自己德育表现的评价', options: ['优秀', '良好', '一般', '待改进'] },
    { type: 'text', title: '你最受益的一次德育活动是什么？请简述原因' }
  ]},
  { key: 'labor_practice', label: '🧹 劳动实践问卷', questions: [
    { type: 'checkbox', title: '你参与了哪些劳动实践？', options: ['教室值日', '校园大扫除', '家务劳动', '社区义务劳动', '专业实训'] },
    { type: 'radio', title: '劳动实践的频率', options: ['每天', '每周2-3次', '每周1次', '偶尔'] },
    { type: 'text', title: '通过劳动你学到了什么？' }
  ]},
  { key: 'class_satisfaction', label: '📊 课堂满意度调查', questions: [
    { type: 'radio', title: '你对本课程的整体满意度', options: ['非常满意', '满意', '一般', '不满意'] },
    { type: 'radio', title: '教学内容难易程度', options: ['偏难', '适中', '偏易'] },
    { type: 'radio', title: '教师授课方式是否符合你的学习习惯', options: ['完全符合', '基本符合', '不太符合'] },
    { type: 'text', title: '你希望课程在哪些方面改进？' }
  ]},
  { key: 'class_activity', label: '🎉 班级活动调查', questions: [
    { type: 'checkbox', title: '你希望班级组织哪些活动？', options: ['春游/秋游', '体育比赛', '文艺汇演', '读书分享会', '职业体验'] },
    { type: 'radio', title: '你能接受的活动费用范围', options: ['50元以内', '50-100元', '100-200元', '200元以上'] },
    { type: 'text', title: '对班级活动的其他建议' }
  ]},
  { key: 'study_habit', label: '📖 学习习惯自评', questions: [
    { type: 'radio', title: '每天课后学习时长', options: ['不足30分钟', '30-60分钟', '1-2小时', '2小时以上'] },
    { type: 'radio', title: '遇到难题时的做法', options: ['自己查资料', '问同学', '问老师', '放弃'] },
    { type: 'radio', title: '是否有做笔记的习惯', options: ['有，很详细', '有，比较简单', '偶尔', '从不'] },
    { type: 'radio', title: '对自己学习习惯的评价', options: ['很好', '较好', '一般', '需要改进'] }
  ]}
]

export const QUESTION_TYPE_LABEL = {
  SINGLE_CHOICE: '单选题',
  MULTI_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_IN: '填空题',
  SUBJECTIVE: '主观题',
  COMPOSITE: '综合题',
  CLOZE: '完形填空',
  MATCHING: '匹配题',
  DRAG_SORT: '拖拽排序',
}
