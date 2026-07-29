import request from '@/utils/request'
import { createEventSource } from '@/utils/sseTicket'

// SSE 订阅
export function subscribeClassroom(classId) {
  return createEventSource(`/api/classroom/class/${classId}/subscribe`)
}

// 轮询班级活动状态
export function pollClassState(classId) {
  return request({ url: `/classroom/class/${classId}/poll-state`, method: 'get' })
}

// ===== 抽问 =====
export function startQuiz(data) {
  return request({ url: '/classroom/quiz/start', method: 'post', data })
}
export function gradeQuiz(data) {
  return request({ url: '/classroom/quiz/grade', method: 'post', data })
}

// ===== 抽问答案 =====
export function submitQuizAnswer(data) {
  return request({ url: `/classroom/sessions/${data.sessionId}/answer`, method: 'post', data })
}

// ===== 抢答 =====
export function startBuzz(data) {
  return request({ url: '/classroom/buzz/start', method: 'post', data })
}
export function submitBuzz(data) {
  return request({ url: '/classroom/buzz/submit', method: 'post', data })
}
export function gradeBuzz(data) {
  return request({ url: '/classroom/buzz/grade', method: 'post', data })
}

// ===== 投票 =====
export function startPoll(data) {
  return request({ url: '/classroom/poll/start', method: 'post', data })
}
export function submitVote(data) {
  return request({ url: '/classroom/poll/vote', method: 'post', data })
}
export function endPoll(sessionId, manualCounts) {
  return request({ url: `/classroom/poll/end`, method: 'post', data: { sessionId, manualCounts } })
}

// ===== 会话管理 =====
export function getSessions(classId) {
  return request({ url: '/classroom/sessions', method: 'get', params: { classId } })
}
export function getClassroomScores(classId) {
  return request({ url: '/classroom/scores', method: 'get', params: { classId } })
}

// ===== 题目管理 =====
export function getQuestions(params) {
  return request({ url: '/classroom/questions', method: 'get', params })
}
export function getQuestionFilters() {
  return request({ url: '/classroom/questions/filters', method: 'get' })
}
export function addQuestion(data) {
  return request({ url: '/classroom/questions', method: 'post', data })
}
export function updateQuestion(id, data) {
  return request({ url: `/classroom/questions/${id}`, method: 'put', data })
}
export function deleteQuestion(id) {
  return request({ url: `/classroom/questions/${id}`, method: 'delete' })
}
export function batchImportQuestions(data) {
  return request({ url: '/classroom/questions/batch-import', method: 'post', data })
}
export function batchImportExcel(data) {
  return request({ url: '/classroom/questions/batch-import-excel', method: 'post', data })
}
export function importFromQuestionBank(data) {
  return request({ url: '/classroom/questions/import-from-bank', method: 'post', data })
}
export function batchImportTxt(data) {
  return request({ url: '/classroom/questions/batch-import-txt', method: 'post', data })
}

// ===== 缺勤 =====
export function markAbsent(classId, studentIds) {
  return request({ url: '/classroom/students/absent', method: 'post', data: { classId, studentIds } })
}
export function getAbsentStudents(classId) {
  return request({ url: '/classroom/students/absent', method: 'get', params: { classId } })
}
export function clearAbsentStudents(classId) {
  return request({ url: '/classroom/students/absent', method: 'delete', params: { classId } })
}

// ===== AI 推荐/出题 =====
export function getAiRecommended(classId) {
  return request({ url: '/classroom/questions/ai-recommended', method: 'get', params: { classId } })
}
export function aiGenerateQuiz(classId, data) {
  const payload = typeof classId === 'object' ? classId : { ...data, classId }
  return request({ url: '/classroom/quiz/ai-generate', method: 'post', data: payload })
}

// ===== 互动分析 =====
export function getClassroomAnalytics(classId, dateRange) {
  const range = typeof dateRange === 'string' ? dateRange : (dateRange?.dateRange || '30d')
  return request({ url: '/classroom/analytics', method: 'get', params: { classId, dateRange: range } })
}

// ===== 课堂任务 =====
export function getActiveClassTasks(classId) {
  return request({ url: '/classroom/tasks/active', method: 'get', params: { classId } })
}
export function startClassTask(classId, taskId) {
  return request({ url: `/classroom/tasks/${taskId}/actions/start`, method: 'post', params: { classId } })
}
export function getTaskProgress(classId, taskId) {
  return request({ url: `/classroom/tasks/${taskId}/progress`, method: 'get', params: { classId } })
}

export function removeFromQuizPool(classId, studentId) {
  return request({ url: '/classroom/quiz/remove-student', method: 'post', data: { classId, studentId } })
}

export function resetQuizPool(classId) {
  return request({ url: '/classroom/quiz/reset-pool', method: 'post', data: { classId } })
}

export function getOnlineCount(classId) {
  return request({ url: `/classroom/class/${classId}/online-count`, method: 'get' })
}

export function startQuickReview(classId, params) {
  return request({ url: `/classroom/class/${classId}/quick-review/start`, method: 'post', data: params })
}

export function recordQuickReview(sessionId, studentId, cardIndex, correct) {
  return request({ url: `/classroom/quick-review/${sessionId}/record`, method: 'post', data: { studentId, cardIndex, correct } })
}

// ── 随堂速答 (LIVE_QUIZ) ──
export function startLiveQuiz(data) { return request({ url: '/classroom/live-quiz/start', method: 'post', data }) }
export function submitLiveQuizAnswer(data) { return request({ url: '/classroom/live-quiz/submit', method: 'post', data }) }
export function endLiveQuiz(data) { return request({ url: '/classroom/live-quiz/end', method: 'post', data }) }
export function pickLiveQuizStudent(data) { return request({ url: '/classroom/live-quiz/pick', method: 'post', data }) }
