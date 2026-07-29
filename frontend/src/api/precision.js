import request from '@/utils/request'

export function getDashboard() {
  return request({ url: '/precision/dashboard', method: 'get' })
}

export function getDiagnosis(subject) {
  return request({ url: '/precision/diagnose', method: 'get', params: { subject } })
}

export function submitDiagnosis(data) {
  return request({ url: '/precision/diagnose/submit', method: 'post', data })
}

/**
 * 逐题提交诊断答案（即时反馈）
 */
export function submitAnswer(data) {
  return request.post('/precision/diagnose/answer', data)
}

export function getWeeklyPack(subject, week) {
  return request({ url: '/precision/weekly-pack', method: 'get', params: { subject, week }, responseType: 'text' })
}

/** 获取学习包结构化题目（在线答题模式，无时间门控） */
export function getPackQuestions(subject) {
  return request({ url: '/precision/pack-questions', method: 'get', params: { subject } })
}

export function getOnlineTest(subject) {
  return request({ url: '/precision/online-test', method: 'get', params: { subject } })
}

export function submitOnlineTest(data) {
  return request({ url: '/precision/online-test/submit', method: 'post', data })
}

export function getReport(subject) {
  return request({ url: '/precision/report', method: 'get', params: { subject } })
}

export function getPracticeQuestions(nodeId, subject) {
  return request({ url: '/precision/practice-questions', method: 'get', params: { nodeId, subject } })
}

export function getSyllabusMap(subject) {
  return request({ url: '/precision/syllabus-map', method: 'get', params: { subject } })
}

export function aiQa(question) {
  return request({ url: '/precision/ai-qa', method: 'post', data: { question } })
}

export function getTeacherOverview() {
  return request({ url: '/precision/teacher/overview', method: 'get' })
}

export function getTeacherStudents(groupId, subject) {
  const params = {}
  if (groupId != null) params.groupId = groupId
  if (subject != null) params.subject = subject
  return request({ url: '/precision/teacher/students', method: 'get', params })
}

export function remindAll(subject, classId) {
  const data = { subject }
  if (classId) data.classId = classId
  return request({ url: '/precision/teacher/remind-all', method: 'post', data })
}

export function composeRemedial(groupId, subject, classId) {
  return request({ url: '/precision/teacher/compose', method: 'post', data: { groupId, subject, classId } })
}

export function getTeacherWeakTop(subject, topN = 5) {
  return request({ url: '/precision/teacher/weak-top', method: 'get', params: { subject, topN } })
}

export function remindStudent(studentId, subject) {
  return request({ url: '/precision/teacher/remind-student', method: 'post', data: { studentId, subject } })
}

export function checkRemedialAccess() {
  return request({ url: '/precision/access', method: 'get' })
}

export function getClassWeaknesses(classId) {
  return request({ url: '/precision/teacher/class-weaknesses', method: 'get', params: { classId } })
}

/** 获取单题详情 */
export function getQuestionDetail(questionId) {
  return request({ url: `/question-bank/${questionId}`, method: 'get' })
}
