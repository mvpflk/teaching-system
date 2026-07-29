import { ref, computed, watch, onUnmounted } from 'vue'

export function useReplayPlayer(keystrokeData, textContent) {
  const currentTime = ref(0)
  const isPlaying = ref(false)
  const playbackSpeed = ref(1)
  const totalDuration = ref(0)

  let _raf = null
  let _lastFrame = 0
  // 脏索引：避免每帧从头扫描击键数据
  let _displayIdx = 0
  let _typedIdx = 0

  watch(() => keystrokeData.value, (data) => {
    if (data && data.length > 0) {
      totalDuration.value = data[data.length - 1].t
    } else {
      totalDuration.value = 0
    }
    currentTime.value = 0
    _displayIdx = 0
    _typedIdx = 0
  }, { immediate: true })

  const displayStates = computed(() => {
    const target = textContent.value || ''
    const states = Array.from({ length: target.length }, (_, i) => ({
      char: target[i],
      state: 'pending'
    }))
    const data = keystrokeData.value || []
    let pos = 0
    // 从脏索引继续，避免每次从头遍历
    for (let i = _displayIdx; i < data.length; i++) {
      const k = data[i]
      if (k.t > currentTime.value) break
      _displayIdx = i + 1
      if (k.a === 'backspace') {
        pos = Math.max(0, pos - 1)
        if (pos < states.length) states[pos].state = 'pending'
      } else if (k.a === 'type') {
        if (pos < states.length) {
          states[pos].state = k.ok ? 'correct' : 'incorrect'
          pos++
        }
      }
    }
    if (pos < states.length) {
      states[pos].state = 'current'
    }
    return states
  })

  const typedCount = computed(() => {
    const data = keystrokeData.value || []
    let count = 0
    for (let i = _typedIdx; i < data.length; i++) {
      const k = data[i]
      if (k.t > currentTime.value) break
      _typedIdx = i + 1
      if (k.a === 'type') count++
      else if (k.a === 'backspace') count = Math.max(0, count - 1)
    }
    return count
  })

  const progressPercent = computed(() =>
    textContent.value ? Math.round((typedCount.value / textContent.value.length) * 100) : 0
  )

  function play() {
    isPlaying.value = true
    _lastFrame = performance.now()
    tick()
  }

  function pause() {
    isPlaying.value = false
    if (_raf) { cancelAnimationFrame(_raf); _raf = null }
  }

  function tick() {
    if (!isPlaying.value) return
    const now = performance.now()
    const delta = (now - _lastFrame) * playbackSpeed.value
    _lastFrame = now
    // 防止标签页后台化后大跳帧：超过 500ms 的 delta 视为异常，跳过
    if (delta > 500) {
      _raf = requestAnimationFrame(tick)
      return
    }
    currentTime.value = Math.min(currentTime.value + delta, totalDuration.value)
    if (currentTime.value >= totalDuration.value) {
      pause()
      return
    }
    _raf = requestAnimationFrame(tick)
  }

  function seek(time) {
    currentTime.value = Math.max(0, Math.min(time, totalDuration.value))
    // seek 时重置脏索引，因为可能跳到任意位置
    _displayIdx = 0
    _typedIdx = 0
  }

  function setSpeed(speed) {
    playbackSpeed.value = speed
  }

  onUnmounted(() => {
    pause()
  })

  return {
    currentTime, isPlaying, playbackSpeed, totalDuration,
    displayStates, typedCount, progressPercent,
    play, pause, seek, setSpeed
  }
}
