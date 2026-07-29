import { ref, onMounted, onUnmounted } from 'vue'
import { MOBILE, TABLET } from '@/constants/breakpoints'

/** 响应式断点检测 — 统一断点 + 自动清理 resize 监听
 *  返回 { isMobile, isTablet } 两个 ref
 */
export function useIsMobile(breakpoint = MOBILE) {
  const isMobile = ref(false)
  const isTablet = ref(false)

  function check() {
    const w = window.innerWidth
    isMobile.value = w < MOBILE
    isTablet.value = w >= MOBILE && w < TABLET
  }

  onMounted(() => { check(); window.addEventListener('resize', check) })
  onUnmounted(() => window.removeEventListener('resize', check))

  return { isMobile, isTablet }
}
