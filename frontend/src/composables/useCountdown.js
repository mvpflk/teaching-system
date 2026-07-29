import { ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'

export function useCountdown(onTimeout) {
  const countdownActive = ref(false)
  const countdownSeconds = ref(30)
  const countdownMax = ref(30)
  let countdownTimer = null
  const circumference = 2 * Math.PI * 46
  const countdownDashOffset = computed(() => {
    const progress = countdownSeconds.value / countdownMax.value
    return circumference * (1 - progress)
  })
  const startCountdown = (onTick) => {
    countdownMax.value = countdownSeconds.value || 30
    countdownSeconds.value = countdownMax.value
    countdownActive.value = true
    countdownTimer = setInterval(() => {
      countdownSeconds.value--
      if (countdownSeconds.value <= 5 && countdownSeconds.value > 0) onTick?.()
      if (countdownSeconds.value <= 0) {
        clearInterval(countdownTimer)
        countdownActive.value = false
        ElMessage.warning('⏰ 时间到！')
        onTimeout?.()
      }
    }, 1000)
  }
  const stopCountdown = () => {
    clearInterval(countdownTimer)
    countdownActive.value = false
  }
  onBeforeUnmount(() => { clearInterval(countdownTimer) })
  return { countdownActive, countdownSeconds, countdownMax, countdownDashOffset, startCountdown, stopCountdown }
}