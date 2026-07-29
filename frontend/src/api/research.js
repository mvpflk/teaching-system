import request from '@/utils/request'

/** 标记为标准化试卷 */
export function markStandardized(paperId, paperRole, parallelPaperId) {
  const body = { paperRole: paperRole || 'COMMON' }
  if (parallelPaperId) body.parallelPaperId = parallelPaperId
  return request({ url: `/research/papers/${paperId}/mark-standardized`, method: 'post', data: body })
}

/** 取消标准化标记 */
export function unmarkStandardized(paperId) {
  return request({ url: `/research/papers/${paperId}/unmark-standardized`, method: 'post' })
}

/** 锁定试卷 */
export function lockPaper(paperId) {
  return request({ url: `/research/papers/${paperId}/lock`, method: 'post' })
}

/** 解锁试卷 */
export function unlockPaper(paperId) {
  return request({ url: `/research/papers/${paperId}/unlock`, method: 'post' })
}

/** 拍摄基线快照 */
export function captureBaseline(snapshotLabel) {
  return request({ url: '/research/baseline/capture', method: 'post', data: { snapshotLabel: snapshotLabel || 'PRETEST' } })
}

/** 导出基线快照CSV（可选researchGroup过滤） */
export function exportBaselineCsv(snapshotLabel, researchGroup) {
  return request({
    url: '/research/baseline/export',
    method: 'get',
    params: { snapshotLabel: snapshotLabel || 'PRETEST', researchGroup },
    responseType: 'blob'
  })
}

/** 获取基线快照摘要（可选researchGroup过滤） */
export function getBaselineSummary(snapshotLabel, researchGroup) {
  return request({
    url: '/research/baseline/summary',
    method: 'get',
    params: { snapshotLabel: snapshotLabel || 'PRETEST', researchGroup }
  })
}

// ── P0-1: 答题卡OCR ──

/** OCR识别答题卡 */
export function ocrAnswerSheet(taskId, studentId, file) {
  const formData = new FormData()
  formData.append('studentId', studentId)
  formData.append('file', file)
  return request({
    url: `/task/${taskId}/answer-sheet/ocr`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 手动录入答案（OCR降级方案） */
export function manualEntryAnswers(taskId, studentId, answers) {
  return request({
    url: `/task/${taskId}/answer-sheet/manual-entry`,
    method: 'post',
    data: { studentId, answers }
  })
}

/** 获取OCR记录列表 */
export function listOcrRecords(taskId, status) {
  return request({
    url: `/task/${taskId}/answer-sheet/records`,
    method: 'get',
    params: { status }
  })
}

/** 教师复核OCR结果 */
export function reviewOcr(taskId, ocrId, confirmed, note) {
  return request({
    url: `/task/${taskId}/answer-sheet/review/${ocrId}`,
    method: 'post',
    data: { confirmed, note }
  })
}

/** OCR准确率统计 */
export function getOcrAccuracyStats(taskId) {
  return request({
    url: `/task/${taskId}/answer-sheet/accuracy`,
    method: 'get'
  })
}
