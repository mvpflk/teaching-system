/**
 * 移动端键盘遮挡修复 — visualViewport 监听
 * iOS Safari 键盘弹出时不触发 window.resize，导致固定定位的底部栏遮挡输入框。
 * 此 composable 监听 visualViewport 变化，将当前焦点元素滚入可视区域。
 *
 * 用法：
 *   import { useKeyboardFix } from '@/composables/useKeyboardFix'
 *   useKeyboardFix()  // 在组件 onMounted 中调用
 */
import { onMounted, onUnmounted } from 'vue'

export function useKeyboardFix() {
  let cleanup = null

  onMounted(() => {
    if (!window.visualViewport) return

    const handler = () => {
      const active = document.activeElement
      if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA' || active.isContentEditable)) {
        // 小幅延迟等待键盘完全弹出后再滚动
        setTimeout(() => {
          active.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }, 300)
      }
    }

    window.visualViewport.addEventListener('resize', handler)
    cleanup = () => window.visualViewport.removeEventListener('resize', handler)
  })

  onUnmounted(() => {
    cleanup?.()
  })
}
