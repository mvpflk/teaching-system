import { ref, onBeforeUnmount } from 'vue'

export function useWheelAnimation(playTick, playDing) {
  const wheelSpinning = ref(false)
  const wheelLanded = ref(false)
  const wheelDisplayName = ref('')
  const wheelIndex = ref(0)
  let wheelTimer = null
  const startWheel = (student, pool, onLanded) => {
    // 防止重复触发：清除上次动画的定时器
    if (wheelTimer) { clearTimeout(wheelTimer); wheelTimer = null }
    wheelSpinning.value = true
    wheelLanded.value = false
    wheelIndex.value = 0
    if (!pool.length) { wheelSpinning.value = false; return }
    let count = 0
    const totalSpins = 15 + Math.floor(Math.random() * 10)
    const baseInterval = 60
    const finalInterval = 400
    const runSpin = () => {
      wheelIndex.value = (wheelIndex.value + 1) % pool.length
      wheelDisplayName.value = pool[wheelIndex.value].name
      count++
      playTick()
      if (count < totalSpins) {
        const progress = count / totalSpins
        const interval = baseInterval + (finalInterval - baseInterval) * progress * progress
        wheelTimer = setTimeout(runSpin, interval)
      } else {
        wheelDisplayName.value = student.name
        wheelLanded.value = true
        playDing()
        onLanded?.()
        setTimeout(() => { wheelSpinning.value = false }, 1200)
      }
    }
    runSpin()
  }
  const skipWheel = (fallbackName) => {
    if (!wheelLanded.value) {
      clearTimeout(wheelTimer)
      wheelDisplayName.value = fallbackName || ''
      wheelLanded.value = true
      playDing()
      setTimeout(() => { wheelSpinning.value = false }, 800)
    }
  }
  onBeforeUnmount(() => { clearTimeout(wheelTimer) })
  return { wheelSpinning, wheelLanded, wheelDisplayName, wheelIndex, startWheel, skipWheel }
}