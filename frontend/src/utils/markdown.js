import MarkdownIt from 'markdown-it'
import markdownItMultimdTable from 'markdown-it-multimd-table'
import DOMPurify from 'dompurify'
import katex from 'katex'
import hljs from 'highlight.js'
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/github-dark.min.css'

// ── markdown-it 配置 ──
const md = new MarkdownIt({
  html: false,
  breaks: true,       // 单换行 → <br>
  linkify: true,
  typographer: false,
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try { return hljs.highlight(code, { language: lang }).value } catch { /* */ }
    }
    try { return hljs.highlightAuto(code).value } catch { return md.utils.escapeHtml(code) }
  }
})
md.use(markdownItMultimdTable, {
  multiline: true,
  rowspan: true,
  headerless: false,
  multibody: true,
  aotolabel: true
})

// ── 流式安全：动态补丁未闭合的 Markdown 语法 ──
function patchIncomplete(text) {
  let patched = text
  // 代码块未闭合
  const fences = (patched.match(/```/g) || []).length
  if (fences % 2 !== 0) patched += '\n```'
  // 加粗 ** 未闭合
  const bolds = (patched.match(/\*\*/g) || []).length
  if (bolds % 2 !== 0) patched += '**'
  // 行内代码 ` 未闭合
  const ticks = (patched.match(/(?<!`)`(?!`)/g) || []).length
  if (ticks % 2 !== 0) patched += '`'
  return patched
}

// ── 预处理：修复 AI 常见输出错误 ──
function preprocessAI(md) {
  if (!md) return ''
  let text = md

  // LaTeX 格式统一：\(...\) → $...$、\[...\] → $$...$$
  // DeepSeek 等模型天然输出标准 LaTeX 定界符，但 markdown-it 会把 \( 的反斜杠
  // 当作转义符吃掉变成 (，导致 KaTeX 永远收不到公式。必须在 md.render() 之前转换。
  text = text.replace(/\\\[/g, '\n$$').replace(/\\\]/g, '$$\n')
  text = text.replace(/\\\(/g, '$').replace(/\\\)/g, '$')
  // \begin{...}...\end{...} → $$...$$（align/cases/matrix 等环境）
  text = text.replace(/\\begin\{([^}]+)\}/g, '$$\\begin{$1}')
  text = text.replace(/\\end\{([^}]+)\}/g, '\\end{$1}$$')

  // 标题前补空行
  text = text.replace(/([^\n])\n(#{1,6}\s)/g, '$1\n\n$2')

  // 表格分隔行修复 — 处理 AI 输出的各种非法分隔行格式
  // 例：|-:|:----:|:----|:----| → |---|------|------|------|
  // 例：|--|------|------|* | → |---|------|------|---|
  text = text.replace(/^\|([-:|*\s]+)\|$/gm, (match) => {
    // 按 | 拆分
    const parts = match.split('|')
    const fixed = parts.map(p => {
      // 去掉空白和非分隔符字符，只保留 -
      let clean = p.replace(/[^\-:]/g, '')
      // 确保至少 3 个 -
      if (clean.length < 3) clean = '---'
      return clean
    }).filter(p => p.trim())
    return '|' + fixed.join('|') + '|'
  })

  // 跨行 ** 合并（** 被换行撕裂）
  text = text.replace(/\*\*\s*\n\s*([^*\n]+)\*\*/g, '**$1**')
  // 表格行首尾缺 | 补全
  text = text.replace(/^([^|\n][^\n]*\|[^\n]+)$/gm, '| $1')
  text = text.replace(/^(\|[^\n]+[^|\n])$/gm, '$1 |')
  return text
}

// ── 安全渲染 ──
export function renderMarkdown(mdText) {
  if (!mdText) return ''
  try {
    const preprocessed = preprocessAI(mdText)
    const patched = patchIncomplete(preprocessed)

    // LaTeX 保护 — 先保护 \$ 防止被正则误匹配
    const safeText = patched.replace(/\\\$/g, '☺DOLLAR☺')
    const blocks = []
    let protectedText = safeText.replace(/\$\$([\s\S]+?)\$\$/g, (_, f) => {
      blocks.push({ type: 'display', formula: f.trim().replace(/☺DOLLAR☺/g, '\\$') })
      return '☺LB' + (blocks.length - 1) + 'B☺'
    })
    protectedText = protectedText.replace(/\$([\s\S]+?)\$/g, (_, f) => {
      blocks.push({ type: 'inline', formula: f.trim().replace(/☺DOLLAR☺/g, '\\$') })
      return '☺LI' + (blocks.length - 1) + 'I☺'
    })
    // 恢复未被匹配的 \$（代码块等场景）
    protectedText = protectedText.replace(/☺DOLLAR☺/g, '\\$')

    let html = md.render(protectedText)

    // KaTeX 恢复
    html = html.replace(/☺LB(\d+)B☺/g, (_, i) => {
      try { return katex.renderToString(blocks[+i].formula, { displayMode: true, throwOnError: false }) }
      catch { return '$$' + blocks[+i].formula + '$$' }
    })
    html = html.replace(/☺LI(\d+)I☺/g, (_, i) => {
      try { return katex.renderToString(blocks[+i].formula, { displayMode: false, throwOnError: false }) }
      catch { return '$' + blocks[+i].formula + '$' }
    })

    html = DOMPurify.sanitize(html, purifyConfig)

    // 代码块包装：复制按钮
    html = html.replace(/<pre><code( class="language-([^"]*)")?>/g, (_, cls, lang) => {
      const label = lang || 'code'
      return `<div class="code-block-wrapper"><div class="code-block-header"><span>${label}</span><button class="code-copy-btn" onclick="var b=this,p=b.parentElement.nextElementSibling;navigator.clipboard.writeText(p.textContent).then(function(){b.textContent='已复制';setTimeout(function(){b.textContent='复制'},2000)})">复制</button></div><pre><code${cls || ''}>`
    })
    html = html.replace(/<\/code><\/pre>/g, '</code></pre></div>')

    // 表格包装：防溢出
    html = html.replace(/<table>/g, '<div class="table-wrapper"><table>')
    html = html.replace(/<\/table>/g, '</table></div>')

    return html
  } catch {
    return mdText.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/\n/g, '<br>')
  }
}

// ── 仅用于流式增量渲染（轻量，不重复处理已有消息）──
export function renderStreamingMarkdown(text) {
  return renderMarkdown(text)
}

// ── DOMPurify 配置 ──
const SAFE_TAGS = ['b','i','u','p','br','a','img','ul','ol','li','blockquote','code','pre','table','thead','tbody','tr','td','th','h1','h2','h3','h4','h5','h6','strong','em','span','div','hr','sup','sub','del','caption','colgroup','col','section','nav','header','footer','main','article','aside','details','summary','mark','small']
const SAFE_ATTRS = ['href','src','alt','title','class','id','target','style','aria-hidden','colspan','rowspan','align']
const purifyConfig = {
  ALLOWED_TAGS: [...SAFE_TAGS, 'annotation','semantics','math','mrow','mi','mo','mn','msup','msub','mfrac','msqrt','mover','munder','mtable','mtr','mtd','mstyle','mspace','mpadded','mphantom','merror','menclose','svg','path'],
  ALLOWED_ATTR: [...SAFE_ATTRS, 'data-value','data-formula','viewBox','d','fill','stroke','stroke-width','xmlns']
}

// ── 其他工具 ──
export function renderBbsContent(text) {
  if (!text) return ''
  const isHtml = /<[a-z][\s\S]*>/i.test(text)
  let html = isHtml ? DOMPurify.sanitize(text, purifyConfig) : text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\n/g,'<br>')
  html = html.replace(/@(\S+?)(?=\s|$|[,.;:!?，。；：！？<])/g, '<span class="mention">@$1</span>')
  return html
}
export function sanitizeHtml(html) { return html ? DOMPurify.sanitize(html, purifyConfig) : '' }
export function sanitizeMathHtml(html) { return html ? DOMPurify.sanitize(html, purifyConfig) : '' }
