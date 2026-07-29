import request from '@/utils/request'

export function getWrongList(params) {
  return request({ url: '/wrong/list', method: 'get', params })
}

export function toggleWrongMastered(id, mastered) {
  return request({ url: `/wrong/${id}/actions/${mastered ? 'mastered' : 'unmastered'}`, method: 'put' })
}

export function generateDerivedPractice() {
  return request({ url: '/wrong/actions/derived-practice', method: 'post' })
}

export function getPracticeStatus(sessionId) {
  return request({ url: `/wrong/practice/${sessionId}/status`, method: 'get' })
}

export function getPracticeSession(sessionId) {
  return request({ url: `/wrong/practice/${sessionId}`, method: 'get' })
}

export function submitPracticeSession(sessionId, answers) {
  return request({ url: `/wrong/practice/${sessionId}/submit`, method: 'post', data: { answers } })
}

export function getWeakPoints() {
  return request({ url: '/wrong/weak-points', method: 'get' })
}

export function deleteWrongQuestion(id) {
  return request({ url: `/wrong/${id}`, method: 'delete' })
}

export function batchDeleteWrongQuestions(ids) {
  return request({ url: '/wrong/actions/batch-delete', method: 'delete', data: { ids } })
}

export function getWeaknessAnalysis(subject) {
  return request({ url: '/student/tasks/wrong-questions/weakness-analysis', method: 'get', params: { subject } })
}

export function getStudentStats() {
  return request({ url: '/wrong/student-stats', method: 'get' })
}

export function recordPractice(id, correct) {
  return request({ url: `/wrong/${id}/actions/practice?correct=${correct}`, method: 'post' })
}

// ── 教师监督 ──
export function getTeacherSummary() {
  return request({ url: '/wrong/teacher/summary', method: 'get' })
}

export function getTeacherStudentList() {
  return request({ url: '/wrong/teacher/students', method: 'get' })
}

export function getTeacherWeakPoints() {
  return request({ url: '/wrong/teacher/weak-points', method: 'get' })
}

// ── 教师干预（Phase 2）──
export function getTeacherStudentWrongDetail(studentId, mastered = 0) {
  return request({ url: `/wrong/teacher/students/${studentId}/wrong-detail`, method: 'get', params: { mastered } })
}

export function notifyStudentReview(studentId) {
  return request({ url: `/wrong/teacher/notify/${studentId}`, method: 'post' })
}

export function getTeacherWeakPointsTrend(weeks = 4) {
  return request({ url: '/wrong/teacher/weak-points/trend', method: 'get', params: { weeks } })
}
