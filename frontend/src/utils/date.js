/** 日期格式化：yyyy-MM-dd HH:mm */
export function fmtFull(s) {
  if (!s) return '-'
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
