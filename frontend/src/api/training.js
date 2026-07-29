import request from '@/utils/request'

// ⚠️ 实训模块整体处于实验期暂停状态（后端桩端点返回 410/501）。
//    下方标注 @deprecated 的函数命中未实现/已禁用端点：
//    - 命中 410（前端在用）的函数带 silent:true，避免拦截器弹红色 toast，由调用方展示空态/友好提示。
//    - 命中 501（前端未调用）的函数仅保留占位，实验期结束后统一实现。

// ── 任务 CRUD ──
/** @deprecated 实验期禁用（后端 410） */
export function listTrainingTasks(params) {
  return request({ url: '/training/tasks', method: 'get', params, silent: true })
}
/** @deprecated 待实现（后端 501） */
export function getTrainingTask(id) {
  return request({ url: `/training/tasks/${id}`, method: 'get', silent: true })
}
/** @deprecated 实验期禁用（后端 410） */
export function createTrainingTask(data) {
  return request({ url: '/training/tasks', method: 'post', data, silent: true })
}
/** @deprecated 实验期禁用（后端 410） */
export function updateTrainingTask(id, data) {
  return request({ url: `/training/tasks/${id}`, method: 'put', data, silent: true })
}
/** @deprecated 待实现（后端 501） */
export function deleteTrainingTask(id) {
  return request({ url: `/training/tasks/${id}`, method: 'delete', silent: true })
}
/** @deprecated 实验期禁用（后端 410） */
export function publishTrainingTask(id) {
  return request({ url: `/training/tasks/${id}/publish`, method: 'post', silent: true })
}

// ── 任务库 ──
/** @deprecated 实验期禁用（后端 410） */
export function listTaskLibrary(params) {
  return request({ url: '/training/library', method: 'get', params, silent: true })
}
/** @deprecated 实验期禁用（后端 410） */
export function copyFromLibrary(libraryId) {
  return request({ url: `/training/library/${libraryId}/copy`, method: 'post', silent: true })
}

// ── 学生端（2026-07-18 接线 trainingService 后恢复可用：saveStepProgress 持久化服务端进度，getStudentSteps 合并 _completed/_data） ──
export function getStudentSteps(taskId) {
  return request({ url: `/training/tasks/${taskId}/steps`, method: 'get', silent: true })
}
export function saveStepProgress(taskId, stepIndex, data) {
  return request({ url: `/training/tasks/${taskId}/steps/${stepIndex}`, method: 'put', data, silent: true })
}
export function submitAllSteps(taskId) {
  return request({ url: `/training/tasks/${taskId}/submit`, method: 'post', silent: true })
}

// ── 教师评分 ──
/** @deprecated 实验期禁用（后端 410） */
export function getSubmissionsForGrading(taskId, params) {
  return request({ url: `/training/tasks/${taskId}/submissions`, method: 'get', params, silent: true })
}
/** @deprecated 待实现（后端 501） */
export function gradeStep(taskId, submissionId, stepIndex, data) {
  return request({ url: `/training/tasks/${taskId}/submissions/${submissionId}/steps/${stepIndex}/grade`, method: 'post', data, silent: true })
}
/** @deprecated 实验期禁用（后端 410） */
export function submitFinalGrade(taskId, submissionId, data) {
  return request({ url: `/training/tasks/${taskId}/submissions/${submissionId}/finalize`, method: 'post', data, silent: true })
}

// ── AI 生成（已实现，正常可用）──
export function aiGenerateSteps(data) {
  return request({ url: '/training/ai/generate-steps', method: 'post', data })
}
export function aiImportFromText(data) {
  return request({ url: '/training/ai/import-text', method: 'post', data })
}

// ── 仿真 ──
/** @deprecated 待实现（后端 501） */
export function getTrainingHub() {
  return request({ url: '/training/hub', method: 'get', silent: true })
}
