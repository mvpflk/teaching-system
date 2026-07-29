import request from '@/utils/request'

export function getQuestionBankList(params) {
  return request({ url: '/question-bank/list', method: 'get', params })
}

export function createQuestion(data) {
  return request({ url: '/question-bank', method: 'post', data })
}

export function updateQuestion(id, data) {
  return request({ url: `/question-bank/${id}`, method: 'put', data })
}

export function deleteQuestion(id) {
  return request({ url: `/question-bank/${id}`, method: 'delete' })
}

export function batchClearQuestions() {
  return request({ url: '/question-bank/actions/batch-clear', method: 'delete' })
}

export function importWordQuestions(formData) {
  return request({ url: '/question-bank/actions/import-word', method: 'post', data: formData })
}

export function importWordQuestionsBatch(formData, mappings) {
  formData.append('mappings', mappings)
  return request({ url: '/question-bank/actions/import-word-batch', method: 'post', data: formData })
}

export function importExcelQuestions(formData) {
  return request({ url: '/question-bank/actions/import-excel', method: 'post', data: formData })
}

export function matchQuestions(data) {
  return request({ url: '/question-bank/actions/match', method: 'post', data })
}

/** 按批次ID获取题目（AI生成后编辑用） */
export function getQuestionsByBatch(batchId) {
  return request.get(`/question-bank/actions/by-batch/${batchId}`)
}

/** AI题目审核通过 */
export function approveAiQuestion(id) {
  return request.put(`/question-bank/${id}/approve`)
}

/** AI题目审核拒绝 */
export function rejectAiQuestion(id) {
  return request.delete(`/question-bank/${id}/reject`)
}

/** AI智能审核题目 */
export function aiReviewQuestions(data) {
  return request.post('/question-bank/actions/ai-review', data)
}

/** 批量审核通过 */
export function batchApproveQuestions(ids) {
  return request.post('/question-bank/actions/batch-approve', ids)
}

/** 批量驳回 */
export function batchRejectQuestions(ids) {
  return request.post('/question-bank/actions/batch-reject', ids)
}

/** 批量删除题目 */
export function batchDeleteQuestions(ids) {
  return request.post('/question-bank/actions/batch-delete', ids)
}

/** 一键示例卷 — 批量保存题目到题库 */
export function batchSaveQuestions(data) {
  return request({ url: '/question-bank/batch-save', method: 'post', data })
}

/** 按 ID 批量取题（试题篮补全），POST 避免 URL 过长 */
export function getQuestionsByIds(ids) {
  return request({ url: '/question-bank/actions/by-ids', method: 'post', data: { ids } })
}

/** 批量统计题目组卷使用次数 {id: count} */
export function getQuestionUsageStats(ids) {
  return request({ url: '/question-bank/actions/usage-stats', method: 'post', data: { ids } })
}

/** 导出 Word 试卷（POST + 文件下载） */
export function exportExamWord(data) {
  return request({
    url: '/question-bank/actions/export-word',
    method: 'post',
    data,
    responseType: 'blob',
  }).then(blob => {
    const objectUrl = window.URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
    const a = document.createElement('a')
    a.href = objectUrl
    // 从 Content-Disposition 提取文件名，回退默认
    const disposition = blob?.headers?.['content-disposition'] || ''
    const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
    a.download = match ? decodeURIComponent(match[1].replace(/['"]/g, '')) : '试卷_' + Date.now() + '.docx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(objectUrl)
    return { code: 200 }
  })
}
