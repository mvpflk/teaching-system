/**
 * 题目相关工具函数
 */

/** 安全解析选项 JSON，返回字符串数组 */
export function parseOptions(opts) {
  if (!opts) return [];
  if (Array.isArray(opts)) {
    return opts.map((o) =>
      o && typeof o === 'object' ? o.text || o.label || o.value || String(o) : String(o)
    );
  }
  if (typeof opts === 'string') {
    try {
      const parsed = JSON.parse(opts);
      // 兼容两种格式：["文本"] 和 [{"key":"A","text":"文本"}]
      return Array.isArray(parsed)
        ? parsed.map((o) =>
            o && typeof o === 'object' ? o.text || o.label || o.value || String(o) : String(o)
          )
        : [];
    } catch {
      return [];
    }
  }
  return [];
}

/** 去除选项前缀字母（A. / B、 / C． / D）等所有常见格式） */
export function cleanOptionText(opt) {
  if (opt && typeof opt === 'object') return opt.text || opt.label || opt.value || String(opt);
  return String(opt).replace(/^[A-Za-hH]\s*[.、．)）:：]\s*/, '');
}

/** 判断答案是否匹配选项 */
export function isCorrectOption(opt, idx, correctAnswer, questionType) {
  if (!correctAnswer) return false;
  const ans = correctAnswer.trim();
  if (questionType === 'TRUE_FALSE') return String(opt).startsWith(ans);
  if (['SINGLE_CHOICE', 'MULTI_CHOICE'].includes(questionType)) {
    const letter = String.fromCharCode(65 + idx);
    return ans
      .split(',')
      .map((s) => s.trim().toUpperCase())
      .includes(letter);
  }
  return false;
}
