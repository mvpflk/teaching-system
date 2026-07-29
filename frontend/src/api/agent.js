import request from '@/utils/request'

export function listSessions() {
  return request({ url: '/agent/sessions', method: 'get' })
}

export function deleteSession(id) {
  return request({ url: `/agent/sessions/${id}`, method: 'delete' })
}

export function listTools() {
  return request({ url: '/agent/tools', method: 'get' })
}

export function submitFeedback(data) {
  return request({ url: '/agent/feedback', method: 'post', data })
}

/** G-4: 用户确认/取消写操作 */
export function confirmWrite(data) {
  return request({ url: '/agent/confirm', method: 'post', data })
}
export function getSessionMessages(sessionId) {
  return request({ url: `/agent/sessions/${sessionId}/messages`, method: 'get' })
}

export function getTaskSource(taskId) {
  return request({ url: `/agent/task-source/${taskId}`, method: 'get' })
}

export function getDailyUsage() {
  return request({ url: '/agent/usage', method: 'get' })
}

/** OCR 拍照识别 */
export function gradeOcr(data) {
  return request({ url: '/agent/grade-ocr', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } })
}

/** 异步回传作答记录 */
export function submitAnswer(data) {
  return request({ url: '/agent/submit-answer', method: 'post', data })
}
