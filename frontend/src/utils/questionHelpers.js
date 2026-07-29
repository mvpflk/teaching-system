/**
 * 题目工具函数 — 统一管理题型中文映射、选项解析等
 * 抽取自 GenerationResult.vue / QuickExamDialog.vue / MajorExamDialog.vue
 */

export const TYPE_LABEL = {
  SINGLE_CHOICE: '单选',
  MULTI_CHOICE: '多选',
  TRUE_FALSE: '判断',
  FILL_IN: '填空',
  CLOZE: '完形',
  READING_COMPREHENSION: '阅读',
  CALCULATION: '计算',
  PROOF: '证明',
  COMPOSITION: '作文',
  SHORT_ANSWER: '简答',
  ESSAY: '问答',
};

/** 题型 → 中文名称 */
export const typeLabel = (t) => TYPE_LABEL[t] || t || '';

/** 题型 → Element Plus Tag 类型 */
export const typeTag = (t) => {
  if (t === 'CALCULATION' || t === 'PROOF') return 'warning';
  if (t === 'READING_COMPREHENSION' || t === 'CLOZE') return '';
  if (t === 'SHORT_ANSWER' || t === 'ESSAY') return 'warning';
  return 'info';
};

/**
 * 防御性解析 options：兼容数组和 JSON 字符串两种格式
 * @param {any} opts
 * @returns {string[]}
 */
export const parseOptions = (opts) => {
  if (!opts) return [];
  if (Array.isArray(opts)) return opts;
  if (typeof opts === 'string') {
    try {
      const p = JSON.parse(opts);
      return Array.isArray(p) ? p : [];
    } catch {
      return [];
    }
  }
  return [];
};
