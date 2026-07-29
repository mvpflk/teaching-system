import request from '@/utils/request'

// 学生端
export function getStudentGrowthCurve(subject) {
  return request({ url: '/analytics/student/growth-curve', method: 'get', params: { subject } })
}
export function getStudentKnowledgeRadar(subject) {
  return request({ url: '/analytics/student/knowledge-radar', method: 'get', params: { subject } })
}
export function getStudentAchievements() {
  return request({ url: '/analytics/student/achievements', method: 'get' })
}
export function getStudentDailyEncouragement() {
  return request({ url: '/analytics/student/daily-encouragement', method: 'get' })
}
export function getStudentSummary(subject) {
  return request({ url: '/analytics/student/summary', method: 'get', params: subject ? { subject } : {} })
}
export function getStudentAvailableSubjects() {
  return request({ url: '/analytics/student/available-subjects', method: 'get' })
}

// 教师端
export function getTeacherGrowthCurve(studentId, subject) {
  return request({ url: '/analytics/teacher/growth-curve', method: 'get', params: { studentId, subject } })
}
export function getTeacherKnowledgeRadar(studentId, subject) {
  return request({ url: '/analytics/teacher/knowledge-radar', method: 'get', params: { studentId, subject } })
}
export function getTeacherEncouragementPreview(studentId) {
  return request({ url: '/analytics/teacher/encouragement-preview', method: 'get', params: { studentId } })
}
export function getTeacherClassGrowthCurves(classId, subject) {
  return request({ url: '/analytics/teacher/class-growth-curves', method: 'get', params: { classId, subject } })
}

export function exportClassScores(classId, subject, startDate, endDate, blinded = false) {
  return request({ url: '/analytics/teacher/export-scores', method: 'get', params: { classId, subject, startDate, endDate, blinded }, responseType: 'blob' })
}

// E5: 课题研究数据导出
export function exportResearchData(blinded = false) {
  return request({ url: '/analytics/research/export', method: 'get', params: { blinded }, responseType: 'blob' })
}

// E6: 知识点掌握趋势
export function getKnowledgeTrend(classId, knowledgeNodeId, subject, startDate, endDate) {
  return request({ url: '/analytics/teacher/knowledge-trend', method: 'get', params: { classId, knowledgeNodeId, subject, startDate, endDate } })
}
