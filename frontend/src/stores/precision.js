import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 偏科提分诊断报告临时存储
 * 替代 URL query 传 JSON（URL 长度限制 2048 字符，诊断报告可达 30KB+）
 */
export const usePrecisionStore = defineStore('precision', () => {
  const diagnosisReport = ref(null)

  function setDiagnosisReport(report) {
    diagnosisReport.value = report
  }

  function getDiagnosisReport() {
    const r = diagnosisReport.value
    diagnosisReport.value = null // 读完即清
    return r
  }

  return { diagnosisReport, setDiagnosisReport, getDiagnosisReport }
})
