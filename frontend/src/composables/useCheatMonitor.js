import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reportCheatWarning } from '@/api/task'

const CHEAT_CACHE_KEY = (taskId) => `cheat_cache_${taskId}`

export function useCheatMonitor(taskId, taskConfig) {
  const cheatCount = ref(0)
  const maxWarnings = ref(0)
  const terminated = ref(false)

  const fullscreenLock = ref(false)
  const disableContextMenu = ref(false)
  const disableCopyPaste = ref(false)
  const isMobile = /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent)

  let toastFirst = 3
  let examActive = false

  const supportsFullscreen = (() => {
    try {
      return !!(document.documentElement.requestFullscreen || document.documentElement.webkitRequestFullscreen)
    } catch { return false }
  })()

  const parseConfig = () => {
    if (!taskConfig || !taskConfig.value) return
    const cfg = typeof taskConfig.value === 'string'
      ? JSON.parse(taskConfig.value)
      : taskConfig.value
    maxWarnings.value = parseInt(cfg.maxCheatWarnings || cfg.maxWarnings || cfg.max_cheat_warnings || 0) || 0
    fullscreenLock.value = cfg.fullscreenLock === true || cfg.fullscreenLock === 'true'
    disableContextMenu.value = cfg.disableContextMenu === true || cfg.disableContextMenu === 'true'
    disableCopyPaste.value = cfg.disableCopyPaste === true || cfg.disableCopyPaste === 'true'
    if (cfg.toastFirstThreshold) toastFirst = parseInt(cfg.toastFirstThreshold) || 3
  }

  // ─── 拦截器 ───

  const onContextMenu = (e) => {
    if (disableContextMenu.value) e.preventDefault()
  }
  const onCopyPaste = (e) => {
    if (disableCopyPaste.value) e.preventDefault()
  }
  const onSelectStart = (e) => {
    if (disableCopyPaste.value) e.preventDefault()
  }
  const onContextMenuMobile = (e) => {
    if (disableContextMenu.value && isMobile) e.preventDefault()
  }

  // ─── 缓存（仅保存 terminated 状态，cheatCount 由服务端权威） ───

  const loadCache = () => {
    try {
      const raw = localStorage.getItem(CHEAT_CACHE_KEY(taskId.value))
      if (raw) {
        const data = JSON.parse(raw)
        terminated.value = data.terminated || false
      }
    } catch { /* ignore */ }
  }

  const saveCache = () => {
    try {
      localStorage.setItem(CHEAT_CACHE_KEY(taskId.value), JSON.stringify({
        terminated: terminated.value
      }))
    } catch { /* ignore */ }
  }

  const clearCache = () => {
    try { localStorage.removeItem(CHEAT_CACHE_KEY(taskId.value)) } catch { /* ignore */ }
  }

  // ─── 服务端同步 ───

  let lastEventType = 'UNKNOWN'

  const syncCheat = async () => {
    try {
      const res = await reportCheatWarning(taskId.value, lastEventType, false)  // sync=false: 真实切屏事件，递增计数
      if (res.code === 200 && res.data) {
        cheatCount.value = res.data.cheatWarnings || 0
        terminated.value = res.data.terminated || false
        saveCache()
      }
    } catch {
      saveCache()
    }
  }

  const flushPending = async () => {
    try {
      const res = await reportCheatWarning(taskId.value, lastEventType, true)  // sync=true: 仅同步状态，不递增计数
      if (res.code === 200 && res.data) {
        cheatCount.value = res.data.cheatWarnings || 0
        terminated.value = res.data.terminated || false
      }
    } catch { /* offline */ }
    saveCache()
  }

  // ─── 核心作弊处理 ───

  const handleCheat = async () => {
    if (terminated.value) return

    await syncCheat()

    if (terminated.value) {
      ElMessageBox.alert('切屏已达上限，考试已终止（本次答题记为0分）。如有疑问请联系任课教师。', '考试终止', {
        confirmButtonText: '知道了',
        type: 'error',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showClose: false,
      })
    } else if (cheatCount.value <= toastFirst) {
      ElMessage.warning(`切屏警告 (${cheatCount.value}/${maxWarnings.value})：考试期间请勿切换窗口或标签页！`)
    } else {
      ElMessageBox.alert(`已切屏 ${cheatCount.value} 次（上限 ${maxWarnings.value} 次），继续切屏将导致考试终止！`, '严重警告', {
        confirmButtonText: '我知道了',
        type: 'warning',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showClose: false,
      })
    }
  }

  // ─── 宽限期（3秒，避免误触发） ───

  let blurTimeout = null
  const CHEAT_GRACE_MS = 3000

  // Page Visibility API — 任何切屏都计数
  const onVisibilityChange = () => {
    if (!examActive) return
    if (document.hidden && !terminated.value) {
      lastEventType = 'VISIBILITY_HIDDEN'
      blurTimeout = setTimeout(() => {
        if (document.hidden) {
          handleCheat()
        }
      }, CHEAT_GRACE_MS)
    } else if (!document.hidden && blurTimeout) {
      clearTimeout(blurTimeout)
      blurTimeout = null
    }
  }

  // Fullscreen change — 仅当 maxWarnings>0 时计数
  const onFullscreenChange = () => {
    if (!examActive || maxWarnings.value <= 0) return
    const inFullscreen = document.fullscreenElement || document.webkitFullscreenElement
    if (!inFullscreen && !terminated.value) {
      lastEventType = 'FULLSCREEN_EXIT'
      blurTimeout = setTimeout(() => {
        if (!document.fullscreenElement && !document.webkitFullscreenElement) {
          handleCheat()
        }
      }, CHEAT_GRACE_MS)
    } else if (inFullscreen && blurTimeout) {
      clearTimeout(blurTimeout)
      blurTimeout = null
    }
  }

  // ESC 键拦截 — 阻止退出全屏
  const onKeyDown = (e) => {
    if (e.key === 'Escape' && (document.fullscreenElement || document.webkitFullscreenElement)
        && !terminated.value) {
      e.preventDefault()
      e.stopPropagation()
    }
  }

  // 页面关闭/刷新拦截 — 考试中离开需确认
  const onBeforeUnload = (e) => {
    if (!examActive || terminated.value) return
    e.preventDefault()
    e.returnValue = '考试进行中，确定要离开吗？未提交的答案将丢失。'
    return e.returnValue
  }

  // ─── 全屏请求 ───

  const requestFullscreen = () => {
    try {
      const el = document.documentElement
      if (el.requestFullscreen) {
        el.requestFullscreen().catch(() => {})
      } else if (el.webkitRequestFullscreen) {
        el.webkitRequestFullscreen()
      }
    } catch { /* 全屏不支持 */ }
  }

  // ─── 多标签页协调（BroadcastChannel）───
  // 防止同一考试在多个标签页打开导致切屏重复计数

  let examChannel = null
  let duplicateTabWarned = false

  const setupExamChannel = () => {
    if (typeof BroadcastChannel === 'undefined') return
    try {
      examChannel = new BroadcastChannel('teaching_exam_' + taskId.value)
      examChannel.onmessage = (e) => {
        if (e.data?.type === 'TAB_JOINED' && examActive && !duplicateTabWarned) {
          duplicateTabWarned = true
          ElMessage.warning({
            message: '检测到同一考试在多个标签页中打开，请关闭其他标签页只保留一个，否则切屏可能被重复计数',
            duration: 8000,
            showClose: true,
          })
        }
      }
    } catch { /* BroadcastChannel not supported */ }
  }

  const teardownExamChannel = () => {
    try { examChannel?.close() } catch { /* ignore */ }
    examChannel = null
    duplicateTabWarned = false
  }

  // ─── 移动端防护 ───

  const applyMobileDefenses = () => {
    if (!isMobile) return
    const style = document.createElement('style')
    style.id = 'cheat-mobile-defenses'
    style.textContent = `
      .submit-panel, .submit-panel * {
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
        user-select: none;
        -webkit-touch-callout: none;
        -webkit-tap-highlight-color: transparent;
      }
      .submit-panel input, .submit-panel textarea {
        -webkit-user-select: text;
        user-select: text;
      }
    `
    document.head.appendChild(style)
  }

  const removeMobileDefenses = () => {
    const style = document.getElementById('cheat-mobile-defenses')
    if (style) style.remove()
  }

  // ─── 激活/停用 ───

  const activate = () => {
    examActive = true
    duplicateTabWarned = false
    // 通知其他标签页：本标签页已进入考试
    try { examChannel?.postMessage({ type: 'TAB_JOINED', timestamp: Date.now() }) } catch { /* ignore */ }
  }

  const deactivate = () => {
    examActive = false
  }

  // ─── 生命周期 ───

  let retryTimer = null

  onMounted(() => {
    parseConfig()
    loadCache()

    // 从后端同步最新状态
    flushPending()

    // 多标签页协调（防止重复计数）
    setupExamChannel()

    // 注册事件（必须在全屏操作前注册，避免错过事件）
    document.addEventListener('visibilitychange', onVisibilityChange)
    document.addEventListener('fullscreenchange', onFullscreenChange)
    document.addEventListener('webkitfullscreenchange', onFullscreenChange)

    // 全屏锁定：仅在尚未全屏时请求（父组件已请求过全屏，重复调用无用户手势会失败并导致退出）
    const alreadyFullscreen = !!(document.fullscreenElement || document.webkitFullscreenElement)
    if ((fullscreenLock.value || maxWarnings.value > 0) && supportsFullscreen && !alreadyFullscreen) {
      requestFullscreen()
    }

    // 移动端防作弊
    applyMobileDefenses()
    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('beforeunload', onBeforeUnload)
    document.addEventListener('contextmenu', onContextMenu)
    document.addEventListener('copy', onCopyPaste)
    document.addEventListener('paste', onCopyPaste)
    document.addEventListener('cut', onCopyPaste)
    document.addEventListener('selectstart', onSelectStart)
    if (isMobile) {
      document.addEventListener('contextmenu', onContextMenuMobile, { capture: true })
    }

    // 定期同步
    retryTimer = setInterval(() => {
      if (examActive && !terminated.value) {
        flushPending()
      }
    }, 30000)
  })

  onUnmounted(() => {
    document.removeEventListener('visibilitychange', onVisibilityChange)
    document.removeEventListener('fullscreenchange', onFullscreenChange)
    document.removeEventListener('webkitfullscreenchange', onFullscreenChange)
    document.removeEventListener('keydown', onKeyDown)
    document.removeEventListener('beforeunload', onBeforeUnload)
    document.removeEventListener('contextmenu', onContextMenu)
    document.removeEventListener('copy', onCopyPaste)
    document.removeEventListener('paste', onCopyPaste)
    document.removeEventListener('cut', onCopyPaste)
    document.removeEventListener('selectstart', onSelectStart)
    if (isMobile) {
      document.removeEventListener('contextmenu', onContextMenuMobile, { capture: true })
    }
    removeMobileDefenses()
    teardownExamChannel()
    if (retryTimer) clearInterval(retryTimer)
    if (blurTimeout) clearTimeout(blurTimeout)
    examActive = false
  })

  return { cheatCount, maxWarnings, terminated, clearCache, flushPending, isMobile, activate, deactivate }
}
