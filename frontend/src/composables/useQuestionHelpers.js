import { sanitizeHtml } from '@/utils/markdown';
import katex from 'katex';

export function typeLabel(qt) {
  const map = {
    SINGLE_CHOICE: '单选',
    MULTI_CHOICE: '多选',
    TRUE_FALSE: '判断',
    FILL_IN: '填空',
    ESSAY: '问答',
    英译中: '英译中',
    中译英: '中译英',
    语法选择: '语法',
  };
  return map[qt] || qt || '未知';
}

export const isChoiceType = (type) =>
  ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE'].includes(type);
export const isMultiType = (type) => type === 'MULTI_CHOICE';
export const isRadioType = (type) => ['SINGLE_CHOICE', 'TRUE_FALSE'].includes(type);
export const isMathInputType = (type) => ['CALCULATION', 'PROOF'].includes(type);
export const isMathStepType = (type) => ['CALCULATION', 'PROOF'].includes(type);

export function parseOptions(raw) {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return raw.map((s, i) => {
      if (s && typeof s === 'object' && s.text !== undefined) {
        return { key: s.key || String.fromCharCode(65 + i), text: s.text };
      }
      const str = String(s).trim();
      const m = str.match(/^([A-Za-z])\s*[.、．)）:：]\s*/);
      if (m) return { key: m[1].toUpperCase(), text: str.substring(m[0].length) };
      return { key: String.fromCharCode(65 + i), text: str };
    });
  }
  try {
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? parseOptions(arr) : [];
  } catch {
    return [];
  }
}

export function sanitizeLatexFallback(formula) {
  return formula
    .replace(/\\[a-zA-Z]+(\\{[^}]*\\})*/g, '')
    .replace(/\\[a-zA-Z]+/g, '')
    .replace(/[$_{}^~&]/g, '')
    .trim();
}

/**
 * 去除 KaTeX 输出的 .katex-mathml（MathML 语义标注），
 * 避免 textContent / 复制粘贴出现公式三倍重复。
 * 视觉层 .katex-html 保留不变。
 */
function stripMathml(html) {
  return html.replace(/<span class="katex-mathml">[\s\S]*?<\/span>/g, '');
}

/**
 * 智能检测文本中的公式片段并尝试 KaTeX 渲染。
 *
 * 数据库中大量数学题未用 $...$ 包裹公式（如 "x²-5x+6=0"），
 * 此函数按中文拆分后对每个非中文片段尝试 KaTeX.renderToString，
 * 成功则保留渲染结果，失败则回退为原文。
 *
 * 规则：
 *   - 已有 $...$ 的文本跳过（由 renderMath 主流程处理）
 *   - 含 {} 的片段跳过（集合表示法会被 LaTeX 吃掉花括号）
 *   - 不含等号/^/数学函数等特征词的片段跳过
 *   - 使用 throwOnError:true 异常捕获区分是否可渲染
 */
function autoRenderPlainMath(text) {
  if (!text || /\$[^$]+\$/.test(text)) return text;
  // 解码 HTML 实体字面量（数据中存的是 &gt; 而非 > 本身）
  // 处理单层 &gt; 和双层 &amp;gt; 两种情况
  text = text
    .replace(/&amp;gt;/g, '>')
    .replace(/&amp;lt;/g, '<')
    .replace(/&amp;amp;/g, '&')
    .replace(/&gt;/g, '>')
    .replace(/&lt;/g, '<')
    .replace(/&amp;/g, '&');

  // 拆分：中文/常见标点为分隔符（不含空格，否则 a > b 会被拆散）
  const segs = [];
  let buf = '';
  for (const ch of text) {
    if (/[\u4e00-\u9fff\u3400-\u4dbf\uf900-\ufaff，。、；：""''！？（）【】《》「」]/.test(ch)) {
      if (buf) {
        segs.push({ type: 'try', val: buf });
        buf = '';
      }
      segs.push({ type: 'raw', val: ch });
    } else {
      buf += ch;
    }
  }
  if (buf) segs.push({ type: 'try', val: buf });

  const hasMathChars =
    /[=^]|sin|cos|tan|log|lg|ln|lim|sqrt|∪|∩|∈|⊆|⊇|∅|∁|⊥|∥|∠|△|°|√|∑|∏|π|α|β|γ|θ|Δ|φ|·|×|÷|²|³|⁰|¹|⁴|⁵|⁶|⁷|⁸|⁹|[a-zA-Z]\d|\d[a-zA-Z]|E=/;

  return segs
    .map((seg) => {
      if (seg.type !== 'try') return seg.val;
      const v = seg.val;
      // 含 {} 跳过（集合表示 vs LaTeX 分组）
      if (/[{}]/.test(v)) return v;
      // 不含数学特征词的跳过
      if (!hasMathChars.test(v)) return v;
      // 单字符跳过（> < 等单独渲染会在 KaTeX 中报错）
      if (v.trim().length <= 1) return v;
      // 仅含 > < 等弱特征但无 = ^ sin 等强特征的，跳过渲染（KaTeX 对 > 单独渲染报错）
      if (/^[^=^]*$/.test(v.trim()) && /[><±]/.test(v) && !/[=^]|sin|cos|tan|log/.test(v)) return v;
      // 直接 KaTeX 渲染，失败则回退原文（throwOnError:false 不抛异常，靠 katex-error 判断）
      const trimmed = v.trim();
      try {
        const rendered = katex.renderToString(trimmed, { throwOnError: false, displayMode: false });
        if (rendered.includes('katex-error')) return v;
        return stripMathml(rendered);
      } catch {
        return v;
      }
    })
    .join('');
}

export function renderMath(text) {
  if (!text) return '';
  // 智能检测公式已暂时禁用（hash 碰撞+缓存混合导致 KaTeX 报错），
  // 仅保留 $...$ 包裹的公式渲染。后续待 root cause 明确后重新启用。
  const s = sanitizeHtml(String(text));
  let html = s.replace(/\$\$([^$]+)\$\$/g, (_, f) => {
    try {
      return stripMathml(
        katex.renderToString(f.trim(), { throwOnError: false, displayMode: true })
      );
    } catch {
      return sanitizeLatexFallback(f);
    }
  });
  html = html.replace(/\$([^$]+)\$/g, (_, f) => {
    try {
      return stripMathml(katex.renderToString(f, { throwOnError: false, displayMode: false }));
    } catch {
      return sanitizeLatexFallback(f);
    }
  });
  // 清除 KaTeX 渲染失败的红色报错 span，回退为纯文本
  html = html.replace(/<span class="katex-error"[^>]*>[\s\S]*?<\/span>/g, (m) => {
    const div = document.createElement('div');
    div.innerHTML = m;
    return div.textContent || '';
  });
  return html;
}
