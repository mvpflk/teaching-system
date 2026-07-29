import request from '@/utils/request'

export function getSimTaskDefinition(id) {
  return request.get(`/simulation/tasks/${id}/definition`)
}

export function createSimTask(data) {
  return request.post('/simulation/tasks', data)
}

export function reportSimProgress(data) {
  return request.post('/simulation/progress', data)
}

export function getSimRecording(submissionId) {
  return request.get(`/simulation/recordings/${submissionId}`)
}

/** 获取实训大厅分类数据 */
export function getTrainingHub() {
  return request.get('/simulation/training-hub')
}

/** 列出仿真任务（可选按 category 过滤） */
export function listSimTasks(category) {
  const params = category ? { category } : {}
  return request.get('/simulation/tasks/list', { params })
}

/** 更新仿真任务 */
export function updateSimTask(id, data) {
  return request.put(`/simulation/tasks/${id}`, data)
}

/** 删除仿真任务 */
export function deleteSimTask(id) {
  return request.delete(`/simulation/tasks/${id}`)
}

/** 提交组卷任务 */
export function submitExamPaper(data) {
  return request.post('/ai-output/actions/generate', data)
}

/** 开始考试 — 创建 submission 返回 submissionId */
export function startSimExam(simTaskId) {
  return request.post(`/simulation/tasks/${simTaskId}/start-exam`)
}

/** 触发AI诊断 */
export function startDiagnosis(taskId) {
  return request.post(`/ai-output/actions/diagnose/${taskId}`)
}

/** 获取诊断结果 */
export function getDiagnosisResult(taskId) {
  return request.get(`/ai-output/actions/diagnose/${taskId}/result`)
}

/** 按任务ID获取学生提交列表 */
export function getTaskSubmissions(taskId) {
  return request.get(`/task/${taskId}/submissions`)
}

/** 提交巩固材料生成 */
export function requestConsolidation(data) {
  return request.post('/ai-output/consolidation', data)
}

/** 批量获取任务组摘要（列表预览 — 每班均分+及格率） */
export function getBatchComparisonSummary(groups) {
  return request.post('/teacher/comparison/summary', { groups })
}
