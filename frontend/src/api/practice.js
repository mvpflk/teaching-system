import request from '@/utils/request'

// 步骤 CRUD
export function createStep(data) {
  return request({ url: '/practice/step', method: 'post', data })
}
export function updateStep(stepId, data) {
  return request({ url: `/practice/step/${stepId}`, method: 'put', data })
}
export function deleteStep(stepId) {
  return request({ url: `/practice/step/${stepId}`, method: 'delete' })
}
export function reorderSteps(taskId, stepIds) {
  return request({ url: `/practice/order/${taskId}`, method: 'post', data: { stepIds } })
}
export function listSteps(taskId, studentId) {
  return request({ url: `/practice/task/${taskId}/steps`, method: 'get', params: studentId ? { studentId } : {} })
}
export function submitPractice(taskId) {
  return request({ url: `/practice/submit/${taskId}`, method: 'post' })
}
export function withdrawPractice(taskId) {
  return request({ url: `/practice/withdraw/${taskId}`, method: 'post' })
}

// 下载
export function startDownload(taskId, classId, studentIds) {
  return request({ url: `/practice/task/${taskId}/download`, method: 'post', params: { classId, studentIds } })
}
export function getDownloadStatus(taskId) {
  return request({ url: `/practice/download/status/${taskId}`, method: 'get' })
}

// 评分
export function gradePractice(data) {
  return request({ url: '/practice/actions/grade', method: 'post', data })
}

// 实训方案
export function listPlans(params) { return request({ url: '/practice/plans', method: 'get', params }) }
export function getPlan(id) { return request({ url: `/practice/plans/${id}`, method: 'get' }) }
export function createPlan(data) { return request({ url: '/practice/plans', method: 'post', data }) }
export function updatePlan(id, data) { return request({ url: `/practice/plans/${id}`, method: 'put', data }) }
export function deletePlan(id) { return request({ url: `/practice/plans/${id}`, method: 'delete' }) }
export function getRubrics(planId) { return request({ url: `/practice/plans/${planId}/rubrics`, method: 'get' }) }
export function publishPlan(planId, data) { return request({ url: `/practice/plans/${planId}/actions/publish`, method: 'post', data }) }

// 导入
export function importPlanZip(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/practice/plans/actions/import-zip', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
export function importPlanExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/practice/plans/actions/import-excel', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}

// 实训提交列表（教师用）
export function getPracticeSubmissions(taskId) { return request({ url: `/practice/task/${taskId}/submissions`, method: 'get' }) }

// 批量导入 Markdown
export function batchImportPlans(markdown) {
  return request({ url: '/practice/plans/actions/batch-import', method: 'post', data: { markdown } })
}
// 获取学科共享库方案
export function listSharedPlans() {
  return request({ url: '/practice/plans/shared', method: 'get' })
}
// 更新方案共享状态
export function updatePlanShareStatus(planId, data) {
  return request({ url: `/practice/plans/${planId}`, method: 'put', data })
}

// ═══════════ 模板库 ═══════════
export function listTemplates(params) {
  return request({ url: '/practice/templates', method: 'get', params })
}
export function getTemplate(id) {
  return request({ url: `/practice/templates/${id}`, method: 'get' })
}
export function applyTemplate(id) {
  return request({ url: `/practice/templates/${id}/apply`, method: 'post' })
}
export function savePlanAsTemplate(planId) {
  return request({ url: `/practice/plans/${planId}/save-as-template`, method: 'post' })
}


// ═══════════ AI 生成 ═══════════
export function aiGeneratePlan(data) {
  return request({ url: '/practice/plans/ai-generate', method: 'post', data })
}

