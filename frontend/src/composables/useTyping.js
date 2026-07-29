import { ref, computed, watch, onUnmounted } from 'vue'

let _audioCtx = null
let _audioRef = 0
function _getAudioCtx() {
  if (!_audioCtx) {
    try { _audioCtx = new (window.AudioContext || window.webkitAudioContext)() } catch { return null }
  }
  return _audioCtx
}
function _retainAudioCtx() { _audioRef++ }
function _releaseAudioCtx() {
  if (--_audioRef <= 0 && _audioCtx && _audioCtx.state !== 'closed') {
    _audioCtx.close().then(() => { _audioCtx = null }).catch(() => {})
    _audioRef = 0
  }
}
function _playTone(freq, duration, type) {
  const ctx = _getAudioCtx()
  if (!ctx) return
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = type || 'sine'
  osc.frequency.value = freq
  gain.gain.setValueAtTime(0.08, ctx.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration)
  osc.connect(gain)
  gain.connect(ctx.destination)
  osc.start()
  osc.stop(ctx.currentTime + duration)
}

export function useTyping(text, options = {}) {
  const { soundEnabled = true } = options
  _retainAudioCtx()
  const typedText = ref('')
  const startTime = ref(null)
  const endTime = ref(null)
  const isFinished = ref(false)
  const backspaceCount = ref(0)
  const _now = ref(Date.now())
  const keystrokes = ref([])
  const isComposing = ref(false)
  const maxSegmentSpeed = ref(0)

  let _clock = null

  const totalChars = computed(() => text.value ? text.value.length : 0)
  const currentIndex = computed(() => typedText.value.length)

  // 退格计数 + 首次击键启动时钟 + 击键采集（IME 组合态跳过）
  watch(() => typedText.value.length, (n, o) => {
    if (o === undefined || isComposing.value) return
    const now = startTime.value ? Date.now() - startTime.value : 0
    const target = text.value || ''
    if (n < o) {
      // 退格：逐个记录被删除的位置
      backspaceCount.value += (o - n)
      for (let i = 0; i < o - n; i++) {
        keystrokes.value.push({ t: now, p: n + i, a: 'backspace' })
      }
    } else if (n > o) {
      // 新增字符：遍历所有新字符（处理 IME 多字符同时提交）
      for (let i = o; i < n; i++) {
        const ch = target[i] || ''
        const typed = typedText.value[i] || ''
        const ok = ch === typed
        keystrokes.value.push({ t: now, c: typed, p: i, ok, a: 'type' })
        if (soundEnabled) {
          _playTone(ok ? 880 : 220, ok ? 0.06 : 0.12, ok ? 'sine' : 'square')
        }
      }
    }
    if (!startTime.value && n > 0) {
      startTime.value = Date.now()
      _now.value = Date.now()
      _clock = setInterval(() => { _now.value = Date.now() }, 200)
    }
  })

  // 完成时停止时钟
  watch(isFinished, (v) => {
    if (v && _clock) { clearInterval(_clock); _clock = null }
  })

  // 逐位对比目标文本 vs 已输入文本
  const charStates = computed(() => {
    const target = text.value || ''
    const typed = typedText.value
    const states = []
    for (let i = 0; i < target.length; i++) {
      if (i < typed.length) {
        states.push({
          char: target[i],
          state: typed[i] === target[i] ? 'correct' : 'incorrect'
        })
      } else if (i === typed.length) {
        states.push({ char: target[i], state: 'current' })
      } else {
        states.push({ char: target[i], state: 'pending' })
      }
    }
    return states
  })

  const correctCount = computed(() => {
    const target = text.value || ''
    const typed = typedText.value
    let c = 0
    for (let i = 0; i < Math.min(typed.length, target.length); i++) {
      if (typed[i] === target[i]) c++
    }
    return c
  })

  const wrongCount = computed(() => {
    const target = text.value || ''
    const typed = typedText.value
    let c = 0
    for (let i = 0; i < Math.min(typed.length, target.length); i++) {
      if (typed[i] !== target[i]) c++
    }
    return c
  })

  const progressPercent = computed(() =>
    totalChars.value > 0 ? Math.round((currentIndex.value / totalChars.value) * 100) : 0
  )

  // 关键修复：通过 _now 心跳驱动，每 200ms 自动重算
  const elapsedSeconds = computed(() => {
    if (!startTime.value) return 0
    void _now.value
    return Math.floor(((endTime.value || Date.now()) - startTime.value) / 1000)
  })

  // 速度：每分钟正确字符数 (CPM)
  const speedWpm = computed(() => {
    if (elapsedSeconds.value === 0 || correctCount.value === 0) return 0
    return Math.round((correctCount.value / elapsedSeconds.value) * 60)
  })

  // 追踪最高段速
  watch(speedWpm, (val) => {
    if (val > maxSegmentSpeed.value) maxSegmentSpeed.value = val
  })

  const accuracy = computed(() => {
    const total = correctCount.value + wrongCount.value
    if (total === 0) return 100
    return parseFloat(((correctCount.value / total) * 100).toFixed(1))
  })

  const errorList = computed(() => {
    const target = text.value || ''
    const typed = typedText.value
    const errors = []
    for (let i = 0; i < Math.min(typed.length, target.length); i++) {
      if (typed[i] !== target[i]) {
        errors.push({ char: typed[i] || '空', expected: target[i], position: i })
      }
    }
    return errors
  })

  // 打完自动结束（IME 组合态期间不触发，防止拼音中间态导致提前结束）
  watch(currentIndex, (idx) => {
    if (idx >= totalChars.value && totalChars.value > 0 && !isFinished.value && !isComposing.value) {
      isFinished.value = true
      endTime.value = Date.now()
    }
  })

  function reset() {
    typedText.value = ''
    startTime.value = null
    endTime.value = null
    isFinished.value = false
    backspaceCount.value = 0
    keystrokes.value = []
    isComposing.value = false
    maxSegmentSpeed.value = 0
    _now.value = Date.now()
    if (_clock) { clearInterval(_clock); _clock = null }
  }

  function getResult() {
    return {
      totalChars: totalChars.value,
      correctChars: correctCount.value,
      wrongChars: wrongCount.value,
      backspaceCount: backspaceCount.value,
      durationSeconds: elapsedSeconds.value,
      speedWpm: speedWpm.value,
      accuracy: accuracy.value,
      errorDetails: errorList.value.slice(0, 500),
      keystrokeData: keystrokes.value.length > 50000
        ? keystrokes.value.slice(-50000) : keystrokes.value,
      maxSegmentSpeed: maxSegmentSpeed.value
    }
  }

  onUnmounted(() => {
    if (_clock) { clearInterval(_clock); _clock = null }
    _releaseAudioCtx()
  })

  return {
    typedText, currentIndex, correctCount, wrongCount, backspaceCount,
    errorList, startTime, endTime, isFinished,
    totalChars, progressPercent, elapsedSeconds, speedWpm, accuracy,
    charStates, reset, getResult,
    isComposing, maxSegmentSpeed
  }
}
