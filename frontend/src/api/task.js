import request from '@/utils/request'

// ── 教师端 ────────────────────────────────────────
export function listTasks(params)       { return request({ url: '/task/list', method: 'get', params }) }
export function getTask(id)             { return request({ url: `/task/${id}`, method: 'get' }) }
export function createTask(data)        { return request({ url: '/task', method: 'post', data }) }
export function updateTask(id, data)    { return request({ url: `/task/${id}`, method: 'put', data }) }
export function deleteTask(id)          { return request({ url: `/task/${id}`, method: 'delete' }) }
export function copyTask(id)            { return request({ url: `/task/${id}/actions/copy`, method: 'post' }) }
export function forcedPreview(params)   { return request({ url: '/task/forced-preview', method: 'get', params }) }
export function publishTask(id)         { return request({ url: `/task/${id}/actions/publish`, method: 'post' }) }
export function closeTask(id)           { return request({ url: `/task/${id}/actions/close`, method: 'post' }) }
export function clearTaskCache()        { return request({ url: '/task/actions/clear-cache', method: 'post' }) }
export function reopenTask(id)          { return request({ url: `/task/${id}/actions/reopen`, method: 'post' }) }
export function getTaskQuestions(id)    { return request({ url: `/task/${id}/questions`, method: 'get' }) }
export function getTaskSubmissions(id, params) { return request({ url: `/task/${id}/submissions`, method: 'get', params }) }
export function addTaskQuestions(id, questionIds) { return request({ url: `/task/${id}/actions/add-questions`, method: 'post', data: { questionIds } }) }
export function removeTaskQuestions(id, questionIds) { return request({ url: `/task/${id}/actions/remove-questions`, method: 'post', data: { questionIds } }) }
export function exportTaskScores(id) { return request({ url: `/task/${id}/actions/export`, method: 'get', responseType: 'blob' }) }
export function restartUnfinishedStudents(id) { return request({ url: `/task/${id}/actions/restart-unfinished`, method: 'post' }) }
export function regradeSubmissionResult(submissionId) { return request({ url: `/task/submissions/${submissionId}/regrade`, method: 'post' }) }
export function batchRegradeSubmissions(data) { return request({ url: '/task/submissions/batch-regrade', method: 'post', data }) }

// ── 学生端 ────────────────────────────────────────
export function getStudentTaskPendingCount() { return request({ url: '/student/tasks/pending-count', method: 'get' }) }
export function getStudentTasks(params) { return request({ url: '/student/tasks/pending', method: 'get', params }) }
export function getStudentCompletedTasks(params) { return request({ url: '/student/tasks/completed', method: 'get', params }) }
export function getStudentTaskDetail(id){ return request({ url: `/student/tasks/${id}`, method: 'get' }) }
export function startStudentExam(id)       { return request({ url: `/student/tasks/${id}/actions/start`, method: 'post' }) }
export function getStudentTaskQuestions(id){ return request({ url: `/student/tasks/${id}/questions`, method: 'get' }) }
export function submitTask(id, data)    { return request({ url: `/student/tasks/${id}/actions/submit`, method: 'post', data }) }
export function reportCheatWarning(taskId, eventType, sync = false) { return request({ url: `/student/tasks/${taskId}/actions/cheat-warning`, method: 'post', data: { eventType: eventType || 'UNKNOWN', sync } }) }
export function saveDraft(taskId, content) { return request({ url: '/student/drafts/save', method: 'post', data: { taskId, content } }) }
export function loadDraft(taskId) { return request({ url: `/student/drafts/${taskId}`, method: 'get' }) }
export function deleteDraft(taskId) { return request({ url: `/student/drafts/${taskId}`, method: 'delete' }) }

// 学习画像
export function getLearningProfile(subject) {
  return request({ url: '/student/tasks/profile/learning', method: 'get', params: { subject } })
}

// 反思（持久化到后端）
export function saveReflection(taskId, submissionId, reflection) {
  return request({ url: `/student/tasks/${taskId}/submissions/${submissionId}/reflection`, method: 'put', data: { reflection } })
}
export function loadReflection(taskId) {
  return request({ url: `/student/tasks/${taskId}`, method: 'get' })
}

// ── 评分 ──────────────────────────────────────────
export function gradeSubmission(taskId, submissionId, data) {
  return request({ url: `/task/${taskId}/actions/grade`, method: 'post', data: { ...data, submissionId } }) }
export function autoGradeSubmission(taskId, submissionId) {
  return request({ url: `/task/${taskId}/submissions/${submissionId}/actions/auto-grade`, method: 'post' }) }
export function regradeSubmission(taskId, submissionId) {
  return request({ url: `/task/${taskId}/submissions/${submissionId}/actions/regrade`, method: 'post' }) }
export function gradeItems(taskId, submissionId, scores) {
  return request({ url: `/task/${taskId}/submissions/${submissionId}/actions/grade-items`, method: 'post', data: scores }) }
export function getSubmissionAnswers(taskId, submissionId) {
  return request({ url: `/task/${taskId}/submissions/${submissionId}/answers`, method: 'get' }) }
export function getSubmissionBoard(taskId) {
  return request({ url: `/task/${taskId}/submission-board`, method: 'get' }) }
export function remindUnsubmitted(taskId) {
  return request({ url: `/task/${taskId}/actions/remind-unsubmitted`, method: 'post' }) }
export function resendToPending(taskId) {
  return request({ url: `/task/${taskId}/actions/resend-to-pending`, method: 'post' }) }
export function allowExtraSubmit(taskId, studentId) {
  return request({ url: `/task/${taskId}/student/${studentId}/allow-extra-submit`, method: 'post' }) }
export function adjustPassRate(taskId, data) {
  return request({ url: `/task/${taskId}/actions/adjust-pass-rate`, method: 'post', data }) }
export function getScoreAnalysis(taskId) {
  return request({ url: `/task/${taskId}/actions/score-analysis`, method: 'get' }) }
export function getSurveyStats(taskId) {
  return request({ url: `/task/${taskId}/survey-stats`, method: 'get' }) }
export function getTaskStats(taskId) {
  return request({ url: `/task/${taskId}/stats`, method: 'get' }) }
export function generateRemedialTask(taskId, data) {
  return request({ url: `/task/${taskId}/actions/generate-remedial`, method: 'post', data }) }

// ── 审核流程 ────────────────────────────────────────
export function submitForReview(taskId) { return request({ url: `/task/${taskId}/actions/submit-review`, method: 'post' }) }
export function approveReview(taskId) { return request({ url: `/task/${taskId}/actions/approve`, method: 'post' }) }
export function rejectReview(taskId, reason) { return request({ url: `/task/${taskId}/actions/reject`, method: 'post', data: { reason } }) }
export function getPendingReviews() { return request({ url: '/task/actions/pending-review', method: 'get' }) }

// ── 兼容旧名（映射到新 Task API） ───────────────────────
export function getExamList(params)      { return listTasks(params) }
export function getHomeworkList(params)  { return listTasks(params) }
export function getQuestions(examId)     { return getTaskQuestions(examId) }
// 兼容旧名：建议调用方传分页参数，此兜底默认 50 条
export function getSubmissionList(hid, params) { return getTaskSubmissions(hid, { page: 1, size: 50, ...params }) }

// ── 量规评分 ──────────────────────────────────────────
export function getTaskRubric(taskId) { return request({ url: `/task/${taskId}/rubric`, method: 'get' }) }
export function gradeRubric(taskId, submissionId, data) {
  return request({ url: `/task/${taskId}/submissions/${submissionId}/actions/grade-rubric`, method: 'post', data })
}

// ── 上传 ────────────────────────────────────────────
export const UPLOAD_ACTION = '/api/upload/actions/homework'
export function getUploadHeaders() { return { Authorization: `Bearer ${localStorage.getItem('token') || ''}` } }
