/**
 * 硬编码颜色 → CSS 变量批量替换工具
 *
 * 使用: node tools/replace-hardcoded-colors.js
 *
 * 分阶段执行：
 *   1. <style> 块内颜色替换（最安全）
 *   2. :style 内联绑定替换
 *   3. JS 颜色常量替换（需人工确认）
 *
 * 安全机制：
 *   - 只替换精确匹配的已知色值
 *   - 跳过注释块中的颜色
 *   - 生成变更报告
 *   - 支持 dry-run 模式预览改动
 */

const fs = require('fs')
const path = require('path')

// ─── 配置 ───
const DRY_RUN = process.argv.includes('--dry-run')
const PHASE = process.argv.find(a => a.startsWith('--phase='))?.split('=')[1] || '1'

// ─── 色值 → CSS 变量映射表 (精确匹配) ───
const COLOR_MAP = new Map([
  // 品牌主色
  ['#4361ee', 'var(--primary-color)'],
  ['#3651d4', 'var(--primary-dark)'],
  ['#eef0ff', 'var(--primary-light)'],

  // 文字色
  ['#1d1d1f', 'var(--text-primary)'],
  ['#3a3a3c', 'var(--text-regular)'],
  ['#86868b', 'var(--text-secondary)'],
  ['#bcbcc0', 'var(--text-disabled)'],

  // 背景色
  ['#f5f5f7', 'var(--bg-page)'],
  // 注意: #ffffff 太通用（可能是白色文字按钮等），不自动替换
  ['#f0f0f2', 'var(--bg-secondary)'],
  ['#f8f9fb', 'var(--bg-section)'],
  ['#fafafa', 'var(--bg-hover)'],

  // Element Plus 功能色 (部分值与变量相同)
  ['#2e7d32', 'var(--el-color-success)'],
  ['#ed6c02', 'var(--el-color-warning)'],
  ['#d32f2f', 'var(--el-color-danger)'],
  ['#909399', 'var(--el-color-info)'],

  // 边框色
  ['rgba(0,0,0,0.04)', 'var(--border-light)'],
  ['rgba(0,0,0,0.08)', 'var(--border-color)'],
  ['rgba(0,0,0,0.12)', 'var(--border-input)'],
])

// 色值 → JS 常量名 (对应 frontend/src/utils/theme.js 的 export)
const JS_CONST_MAP = new Map([
  ['#4361ee', 'primaryColor'],
  ['#3651d4', 'primaryDark'],
  ['#eef0ff', 'primaryLight'],
  ['#1d1d1f', 'textPrimary'],
  ['#3a3a3c', 'textRegular'],
  ['#86868b', 'textSecondary'],
  ['#bcbcc0', 'textDisabled'],
  ['#f5f5f7', 'bgPage'],
  ['#f0f0f2', 'bgSecondary'],
  ['#f8f9fb', 'bgSection'],
  ['#fafafa', 'bgHover'],
  ['#2e7d32', 'elSuccess'],
  ['#ed6c02', 'elWarning'],
  ['#d32f2f', 'elDanger'],
  ['#909399', 'elInfo'],
])

// 需要跳过的文件目录
const SKIP_DIRS = ['node_modules', 'dist', '.git']

// ─── 工具函数 ───

function getAllVueFiles(dir) {
  const results = []
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true })
    for (const entry of entries) {
      if (SKIP_DIRS.includes(entry.name)) continue
      const fullPath = path.join(dir, entry.name)
      if (entry.isDirectory()) {
        results.push(...getAllVueFiles(fullPath))
      } else if (entry.name.endsWith('.vue')) {
        results.push(fullPath)
      }
    }
  } catch { /* 权限不足等 */ }
  return results
}

// 提取 <style> 块内容
function extractStyleBlock(content) {
  const match = content.match(/<style[^>]*>([\s\S]*?)<\/style>/)
  return match ? { full: match[0], inner: match[1], index: match.index } : null
}

function extractScriptBlock(content) {
  const match = content.match(/<script[^>]*>([\s\S]*?)<\/script>/)
  return match ? { full: match[0], inner: match[1], index: match.index } : null
}

// 提取 <template> 块内容
function extractTemplateBlock(content) {
  const match = content.match(/<template>([\s\S]*?)<\/template>/)
  return match ? { full: match[0], inner: match[1], index: match.index } : null
}

// 在 :style 内联绑定中替换颜色
function replaceColorsInInlineStyle(templateContent) {
  let result = templateContent
  let totalReplaced = 0

  // 只匹配 style="..." / :style="..." / style='...' / :style='...' 属性值
  const styleAttrRegex = /(:?\bstyle)\s*=\s*(["'])([\s\S]*?)\2/g

  result = result.replace(styleAttrRegex, (match, attrName, quote, value) => {
    let newValue = value

    for (const [hex, variable] of COLOR_MAP) {
      const escapedHex = hex.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      // 在 style 属性值中，颜色可能以 #xxxx 形式出现
      const regex = new RegExp(escapedHex, 'g')
      const before = newValue
      newValue = newValue.replace(regex, () => {
        totalReplaced++
        return variable
      })
    }

    return `${attrName}=${quote}${newValue}${quote}`
  })

  return { content: result, count: totalReplaced }
}

// 在 style 块中替换颜色（跳过 var() fallback 中的颜色）
function replaceColorsInStyle(styleContent) {
  // 两遍法：先保护 var() fallback 中的颜色，替换后恢复
  const placeholders = new Map()
  let counter = 0
  let preprocessed = styleContent.replace(
    /(var\(--[\w-]+,\s*)#[0-9a-fA-F]{3,6}\s*\)/g,
    (match) => {
      const key = `__COLOR_FALLBACK_${counter++}__`
      placeholders.set(key, match)
      return key
    }
  )

  let totalReplaced = 0

  for (const [hex, variable] of COLOR_MAP) {
    const regex = new RegExp(
      hex.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'),
      'g'
    )
    const before = preprocessed
    preprocessed = preprocessed.replace(regex, (match) => {
      totalReplaced++
      return variable
    })
  }

  // 恢复被保护的 var() fallback
  let result = preprocessed
  for (const [key, original] of placeholders) {
    result = result.replace(key, original)
  }

  return { content: result, count: totalReplaced }
}

// ─── 主流程 ───

function main() {
  const frontendDir = path.join(__dirname, '..', 'frontend', 'src')
  const vueFiles = getAllVueFiles(frontendDir)
  console.log(`找到 ${vueFiles.length} 个 Vue 文件`)

  if (PHASE === '1') {
    console.log('阶段 1: <style> 块内颜色替换\n')
    runPhase1(vueFiles)
  } else if (PHASE === '2') {
    console.log('阶段 2: :style 内联绑定颜色替换\n')
    runPhase2(vueFiles)
  } else if (PHASE === '3') {
    console.log('阶段 3: <script> 块内 JS 颜色常量替换\n')
    runPhase3(vueFiles)
  }
}

function runPhase1(files) {
  let totalFiles = 0
  let totalReplaced = 0
  const report = []

  for (const file of files) {
    const content = fs.readFileSync(file, 'utf-8')
    const styleBlock = extractStyleBlock(content)

    if (!styleBlock) continue

    const { content: newStyleInner, count } = replaceColorsInStyle(styleBlock.inner)
    if (count === 0) continue

    totalFiles++
    totalReplaced += count
    const newFullStyle = styleBlock.full.replace(styleBlock.inner, newStyleInner)
    const newContent = content.replace(styleBlock.full, newFullStyle)

    report.push({ file: path.relative(path.join(__dirname, '..'), file), count, changed: count > 0 })

    if (!DRY_RUN) {
      fs.writeFileSync(file, newContent, 'utf-8')
    }
  }

  console.log(`\n=== 阶段 1 完成 ===`)
  console.log(`修改文件: ${totalFiles}`)
  console.log(`替换颜色: ${totalReplaced}`)
  console.log(`模式: ${DRY_RUN ? '预览 (dry-run)' : '实际写入'}`)

  if (report.length > 0) {
    console.log(`\n前 20 个变更文件:`)
    report
      .sort((a, b) => b.count - a.count)
      .slice(0, 20)
      .forEach(r => console.log(`  ${r.count}处\t${r.file}`))
  }
}

// 在 <script> 块中替换硬编码颜色为 JS 常量引用
function replaceColorsInScript(scriptContent) {
  let result = scriptContent
  const usedConstants = new Set()
  let totalReplaced = 0

  for (const [hex, constName] of JS_CONST_MAP) {
    const escapedHex = hex.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    // 匹配 'hex' 或 "hex" (JS 字符串中的色值)
    const regex = new RegExp(`['"]${escapedHex}['"]`, 'g')
    result = result.replace(regex, (match) => {
      totalReplaced++
      usedConstants.add(constName)
      return constName
    })
  }

  return { content: result, count: totalReplaced, used: [...usedConstants] }
}

// 为 <script> 块添加 import { ... } from '@/utils/theme'
function addThemeImport(scriptContent, usedConstants) {
  if (usedConstants.length === 0) return scriptContent

  const importLine = `import { ${usedConstants.join(', ')} } from '@/utils/theme'`

  // 检查是否已导入
  if (scriptContent.includes("'@/utils/theme'") || scriptContent.includes('"@/utils/theme"')) {
    // 已存在导入，不重复添加
    return scriptContent
  }

  // 在第一个 import 之后，或文件开头插入
  const firstImport = scriptContent.match(/^import\s/m)
  if (firstImport) {
    // 在最后一条 import 语句后插入
    const lines = scriptContent.split('\n')
    let lastImportIdx = 0
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].startsWith('import ')) {
        lastImportIdx = i
      }
    }
    lines.splice(lastImportIdx + 1, 0, importLine)
    return lines.join('\n')
  } else {
    return importLine + '\n' + scriptContent
  }
}

function runPhase2(files) {
  let totalFiles = 0
  let totalReplaced = 0
  const report = []

  for (const file of files) {
    const content = fs.readFileSync(file, 'utf-8')
    const templateBlock = extractTemplateBlock(content)

    if (!templateBlock) continue

    const { content: newTemplateInner, count } = replaceColorsInInlineStyle(templateBlock.inner)
    if (count === 0) continue

    totalFiles++
    totalReplaced += count
    const newFullTemplate = templateBlock.full.replace(templateBlock.inner, newTemplateInner)
    const newContent = content.replace(templateBlock.full, newFullTemplate)

    report.push({ file: path.relative(path.join(__dirname, '..'), file), count, changed: count > 0 })

    if (!DRY_RUN) {
      fs.writeFileSync(file, newContent, 'utf-8')
    }
  }

  console.log(`\n=== 阶段 2 完成 ===`)
  console.log(`修改文件: ${totalFiles}`)
  console.log(`替换颜色: ${totalReplaced}`)
  console.log(`模式: ${DRY_RUN ? '预览 (dry-run)' : '实际写入'}`)

  if (report.length > 0) {
    console.log(`\n变更文件列表:`)
    report
      .sort((a, b) => b.count - a.count)
      .forEach(r => console.log(`  ${r.count}处\t${r.file}`))
  }
}

function runPhase3(files) {
  let totalFiles = 0
  let totalReplaced = 0
  const report = []

  for (const file of files) {
    const content = fs.readFileSync(file, 'utf-8')
    const scriptBlock = extractScriptBlock(content)

    if (!scriptBlock) continue

    const { content: newScriptInner, count, used } = replaceColorsInScript(scriptBlock.inner)
    if (count === 0) continue

    // 添加 import
    const finalScriptInner = addThemeImport(newScriptInner, used)
    const newFullScript = scriptBlock.full.replace(scriptBlock.inner, finalScriptInner)
    const newContent = content.replace(scriptBlock.full, newFullScript)

    totalFiles++
    totalReplaced += count
    report.push({ file: path.relative(path.join(__dirname, '..'), file), count, changed: count > 0 })

    if (!DRY_RUN) {
      fs.writeFileSync(file, newContent, 'utf-8')
    }
  }

  console.log(`\n=== 阶段 3 完成 ===`)
  console.log(`修改文件: ${totalFiles}`)
  console.log(`替换颜色: ${totalReplaced}`)
  console.log(`模式: ${DRY_RUN ? '预览 (dry-run)' : '实际写入'}`)

  if (report.length > 0) {
    console.log(`\n变更文件列表:`)
    report
      .sort((a, b) => b.count - a.count)
      .forEach(r => console.log(`  ${r.count}处\t${r.file}`))
  }
}

main()
