// 统一题型常量 — 与后端 QuestionTypeEnum 保持严格一致
// 禁止硬编码字符串判断题型，始终使用此文件中的常量

export const QUESTION_TYPES = {
  SINGLE_CHOICE: 'SINGLE_CHOICE',
  MULTI_CHOICE: 'MULTI_CHOICE',
  TRUE_FALSE: 'TRUE_FALSE',
  FILL_IN: 'FILL_IN',
  DRAG_SORT: 'DRAG_SORT',
  MATCHING: 'MATCHING',
  CLOZE: 'CLOZE',
  SHORT_ANSWER: 'SHORT_ANSWER',
  PROGRAMMING: 'PROGRAMMING',
  FILE_UPLOAD: 'FILE_UPLOAD',
  AUDIO_VIDEO: 'AUDIO_VIDEO',
  ESSAY: 'ESSAY',
  COMPOSITE: 'COMPOSITE',
  CLASSROOM_MANUAL: 'CLASSROOM_MANUAL',
}

export const QUESTION_TYPE_LABEL = {
  SINGLE_CHOICE: '单选',
  MULTI_CHOICE: '多选',
  TRUE_FALSE: '判断',
  FILL_IN: '填空',
  DRAG_SORT: '拖拽排序',
  MATCHING: '连线匹配',
  CLOZE: '完形填空',
  SHORT_ANSWER: '简答',
  PROGRAMMING: '编程题',
  FILE_UPLOAD: '文件上传',
  AUDIO_VIDEO: '音视频作答',
  ESSAY: '论述/作文',
  COMPOSITE: '综合题',
  CLASSROOM_MANUAL: '课堂手动题目',
}

export const QUESTION_TYPE_TAG = {
  SINGLE_CHOICE: '',
  MULTI_CHOICE: 'warning',
  TRUE_FALSE: 'success',
  FILL_IN: 'info',
  DRAG_SORT: '',
  MATCHING: 'warning',
  CLOZE: 'info',
  SHORT_ANSWER: 'danger',
  PROGRAMMING: 'danger',
  FILE_UPLOAD: '',
  AUDIO_VIDEO: 'warning',
  ESSAY: 'danger',
  COMPOSITE: '',
  CLASSROOM_MANUAL: 'info',
}

// 需要选项的题型（渲染 radio/checkbox 而非文本框）
export const OPTION_TYPES = ['SINGLE_CHOICE', 'MULTI_CHOICE']

// 主观题（需教师手工评分）
export const SUBJECTIVE_TYPES = ['SHORT_ANSWER', 'PROGRAMMING', 'FILE_UPLOAD', 'AUDIO_VIDEO', 'ESSAY', 'COMPOSITE', 'DRAG_SORT', 'MATCHING', 'CLOZE']

// 固定1分的题型
export const FIXED_ONE_POINT_TYPES = ['TRUE_FALSE', 'FILL_IN']

// 综合题（含多个子题，需递归查询 question_composite_items）
export const COMPOSITE_TYPES = ['COMPOSITE']

// 兼容旧代码 — 保留旧命名映射
export { QUESTION_TYPES as default }
