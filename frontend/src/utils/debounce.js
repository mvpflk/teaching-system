/** 防抖 — 延迟 delay 毫秒后执行，重复调用重置计时 */
export function debounce(fn, delay = 300) {
  let timer
  return function (...args) {
    clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}
