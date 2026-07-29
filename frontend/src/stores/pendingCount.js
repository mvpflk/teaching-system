import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getStudentTaskPendingCount } from '@/api/task'

export const usePendingCountStore = defineStore('pendingCount', () => {
  const pendingData = ref({ count: 0, urgent: 0, warning: 0 })
  let timer = null

  const badgeData = computed(() => {
    const { count, urgent, warning } = pendingData.value
    if (urgent > 0) return { value: urgent, type: 'urgent' }
    if (warning > 0) return { value: warning, type: 'warning' }
    return { value: count, type: 'normal' }
  })

  const fetch = async () => {
    try {
      const r = await getStudentTaskPendingCount()
      if (r.code === 200) pendingData.value = r.data || { count: 0, urgent: 0, warning: 0 }
    } catch { /* */ }
  }

  const startPolling = (intervalMs = 300000) => {
    fetch()
    if (timer) clearInterval(timer)
    timer = setInterval(fetch, intervalMs)
  }

  const stopPolling = () => {
    if (timer) { clearInterval(timer); timer = null }
    pendingData.value = { count: 0, urgent: 0, warning: 0 }
  }

  return { pendingData, badgeData, fetch, startPolling, stopPolling }
})