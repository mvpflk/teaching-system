/** CSV 值转义（逗号、双引号、换行）+ 防公式注入 */
function escapeCsvValue(val) {
  const s = val != null ? String(val) : ''
  // 防 CSV 注入：以 = + - @ 开头时加单引号前缀
  const safe = /^[=+\-@]/.test(s) ? "'" + s : s
  if (safe.includes(',') || safe.includes('"') || safe.includes('\n') || safe.includes('\r')) {
    return '"' + safe.replace(/"/g, '""') + '"'
  }
  return safe
}

export function exportCsv(rows, filename, columns) {
  const header = columns.map(c => c.label).join(',') + '\n'
  const csv = header + rows.map(r => columns.map(c => escapeCsvValue(r[c.key])).join(',')).join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 100)
}

export const TYPING_RESULT_COLUMNS = [
  { key: 'studentName', label: '学生' },
  { key: 'username', label: '账号' },
  { key: 'speedWpm', label: '速度(字/分)' },
  { key: 'accuracy', label: '正确率%' },
  { key: 'durationSeconds', label: '用时秒' },
  { key: 'correctChars', label: '正确字符' },
  { key: 'wrongChars', label: '错误字符' },
  { key: 'backspaceCount', label: '退格次数' },
  { key: 'score', label: '得分' },
  { key: 'finishedAt', label: '完成时间' }
]
