/**
 * 未引用 Vue 组件检测脚本
 *
 * 扫描 src/ 下所有 .vue 文件，检查其是否被其他源文件引用（import 或 <component> 标签）。
 *
 * 策略：对每个 .vue 文件，检查它的三种引用模式是否出现在「其他」源文件中。
 *   - @/views/xxx/Xxx.vue （完整路径）
 *   - @/views/xxx/Xxx    （无扩展名）
 *   - ./Xxx.vue 或 <Xxx> （父组件同目录引用）
 *
 * 用法：
 *   node scripts/find-unused-components.mjs
 *   node scripts/find-unused-components.mjs --list   # 仅输出路径列表
 */

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const srcDir = path.resolve(__dirname, '../src')

// ── 工具函数 ──

/** 递归收集所有 .vue 文件路径（相对 src/） */
function collectVueFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    if (entry.name.startsWith('.')) continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      files.push(...collectVueFiles(full))
    } else if (entry.isFile() && entry.name.endsWith('.vue')) {
      files.push(path.relative(srcDir, full).replace(/\\/g, '/'))
    }
  }
  return files.sort()
}

/** 读取单个文件内容 */
function readFile(filePath) {
  try { return fs.readFileSync(filePath, 'utf-8') } catch { return '' }
}

/** 检查一个 .vue 文件的路径是否在给定内容中被引用 */
function countRefsInContent(content, vueFile) {
  const basename = path.basename(vueFile, '.vue')
  // 将 kebab-case 转换为 PascalCase
  const pascalName = basename.replace(/-([a-z])/g, (_, c) => c.toUpperCase())
  const patterns = [
    `@/${vueFile}`,                          // @/views/xxx/Xxx.vue
    `@/${vueFile.replace(/\.vue$/, '')}`,    // @/views/xxx/Xxx（无扩展名）
    `./${basename}.vue`,                     // ./Xxx.vue
    `./${basename}`,                         // ./Xxx
    `<${pascalName}`,                        // <ComposeExamWizard (模板标签)
    `</${pascalName}>`,                      // </ComposeExamWizard>
    `/${basename}.vue`,                      // ./components/Xxx.vue（含路径的相对引用）
    `/${basename}'`,                         // ./some/path/Xxx' (import 结尾)
    `/${basename}"`,                         // "./some/path/Xxx"
  ]
  let count = 0
  for (const p of patterns) {
    if (!p) continue
    let idx = 0
    while ((idx = content.indexOf(p, idx)) !== -1) { count++; idx++ }
  }
  return count
}

// ── 主流程 ──

function main() {
  console.log('\n🔍 扫描未引用的 Vue 组件\n')

  const allVueFiles = collectVueFiles(srcDir)
  console.log(`📄 共 ${allVueFiles.length} 个 .vue 文件\n`)

  // 读取所有源文件，按文件组织
  const allSourceFiles = []
  function collectFiles(dir) {
    const entries = fs.readdirSync(dir, { withFileTypes: true })
    for (const entry of entries) {
      if (entry.name.startsWith('.')) continue
      const full = path.join(dir, entry.name)
      if (entry.isDirectory()) {
        if (!['node_modules', '.git', 'dist'].includes(entry.name)) collectFiles(full)
      } else if (entry.isFile() && /\.(vue|js|mjs|cjs|ts)$/.test(entry.name)) {
        allSourceFiles.push({ path: full, content: readFile(full) })
      }
    }
  }
  collectFiles(srcDir)

  // ── 对每个 .vue 文件，检查是否被其他文件引用 ──
  const results = []
  for (const vueFile of allVueFiles) {
    const vueFullPath = path.join(srcDir, vueFile)
    let totalRefs = 0

    for (const sf of allSourceFiles) {
      if (sf.path === vueFullPath) continue // 跳过自身
      totalRefs += countRefsInContent(sf.content, vueFile)
    }

    results.push({ file: vueFile, count: totalRefs })
  }

  // ── 输出 ──
  results.sort((a, b) => a.count - b.count)

  const unused = results.filter(r => r.count === 0)
  const lowRef = results.filter(r => r.count === 1)

  if (unused.length === 0 && lowRef.length === 0) {
    console.log('✅ 所有 .vue 文件均有外部引用。')
    return
  }

  if (unused.length > 0) {
    console.log(`🚫 完全未被外部引用（0 次）：${unused.length} 个\n`)
    for (const r of unused) {
      console.log(`   📄 ${r.file}`)
    }
  }

  if (lowRef.length > 0) {
    console.log(`\n⚠️  仅 1 次外部引用（建议人工复核是否可内联）：${lowRef.length} 个\n`)
    for (const r of lowRef) {
      console.log(`   📄 ${r.file}`)
    }
  }

  console.log(`\n📋 操作步骤：`)
  console.log(`   1. 对 "0 次" 列表：在 IDE 中搜索文件名，确认无引用后删除。`)
  console.log(`   2. 对 "1 次" 列表：内联合并入唯一引用方，或确认后删除。`)
  console.log(`   3. 删除命令：rm src/components/common/XXX.vue`)
  console.log()

  if (process.argv.includes('--list')) {
    for (const r of unused) console.log(r.file)
  }
}

main()
