import { ref } from 'vue'
import { getTeacherDashboard } from '@/api/dashboard'

export function useHomeCharts(selectedClassId) {
  const dashData = ref(null)

  const submissionRate = ref(0)
  const passRate = ref(0)

  const submissionRateColor = ref('var(--success-color)')
  const passRateColor = ref('var(--success-color)')
  const submissionRateClass = ref('success')
  const passRateClass = ref('success')

  const loadTeacherCharts = async (stats) => {
    try {
      const params = {}
      if (selectedClassId.value) params.classId = selectedClassId.value
      const r = await getTeacherDashboard(params)
      if (r.code === 200) {
        dashData.value = r.data
        stats.totalStudents = r.data.totalStudents || 0
        stats.totalHomework = r.data.totalHomework || 0
        stats.pendingReview = r.data.pendingReview || 0
        stats.pendingPublish = r.data.pendingPublish || 0
        stats.pendingAiReview = r.data.pendingAiReview || 0
        stats.qualityAlerts = r.data.qualityAlerts || []
        const sr = Math.round((r.data.homeworkSubmissionRate || 0) * 100)
        const pr = Math.round((r.data.examPassRate || 0) * 100)
        submissionRate.value = sr
        passRate.value = pr
        submissionRateColor.value = sr >= 60 ? 'var(--success-color)' : sr >= 30 ? 'var(--warning-color)' : 'var(--danger-color)'
        passRateColor.value = pr >= 60 ? 'var(--success-color)' : pr >= 30 ? 'var(--warning-color)' : 'var(--danger-color)'
        submissionRateClass.value = sr >= 60 ? 'success' : sr >= 30 ? 'warning' : 'danger'
        passRateClass.value = pr >= 60 ? 'success' : pr >= 30 ? 'warning' : 'danger'
      }
    } catch { /* */ }
  }

  return {
    dashData, submissionRate, passRate,
    submissionRateColor, passRateColor,
    submissionRateClass, passRateClass,
    loadTeacherCharts
  }
}
