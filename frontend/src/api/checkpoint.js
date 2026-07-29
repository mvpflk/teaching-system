import request from '@/utils/request'

// ===================== 学生端 =====================

export function listSubjects() {
  return request({ url: '/checkpoint/subjects', method: 'get' })
}

export function getOverview(subjectId) {
  return request({ url: '/checkpoint/overview', method: 'get', params: { subjectId } })
}

export function startCheckpoint(configId) {
  return request({ url: `/checkpoint/${configId}/start`, method: 'get' })
}

export function verifyKeywords(configId, answers) {
  return request({ url: `/checkpoint/${configId}/keywords/verify`, method: 'post', data: { answers } })
}

export function skipKeyword(configId, keywordIndex) {
  return request({ url: `/checkpoint/${configId}/keywords/skip`, method: 'post', data: { keywordIndex } })
}

export function submitCheckpoint(configId, data) {
  return request({ url: `/checkpoint/${configId}/submit`, method: 'post', data })
}

// ===================== Boss战 =====================

export function startBoss(configId) {
  return request({ url: '/checkpoint/boss/start', method: 'post', data: { configId } })
}

export function submitBoss(configId, answers) {
  return request({ url: '/checkpoint/boss/submit', method: 'post', data: { configId, answers } })
}

export function retryBoss(configId) {
  return request({ url: '/checkpoint/boss/retry', method: 'post', data: { configId } })
}

// ===================== 混合战 =====================

export function startMixed(configId) {
  return request({ url: '/checkpoint/mixed/start', method: 'post', data: { configId } })
}

export function submitMixed(configId, answers) {
  return request({ url: '/checkpoint/mixed/submit', method: 'post', data: { configId, answers } })
}

export function retryMixed(configId) {
  return request({ url: '/checkpoint/mixed/retry', method: 'post', data: { configId } })
}

// ===================== 记忆卡 =====================

export function listMemoryCards(subjectId) {
  return request({ url: '/checkpoint/memory-cards', method: 'get', params: { subjectId } })
}

export function getMemoryCard(cardId) {
  return request({ url: `/checkpoint/memory-cards/${cardId}`, method: 'get' })
}

export function reviewMemoryCard(cardId) {
  return request({ url: `/checkpoint/memory-cards/${cardId}/review`, method: 'post' })
}

export function getUnreviewedCount() {
  return request({ url: '/checkpoint/memory-cards/unreviewed-count', method: 'get' })
}

// ===================== 教师管理端 =====================

export function adminListCheckpoints(params) {
  return request({ url: '/checkpoint/admin/list', method: 'get', params })
}

export function adminUpdateCheckpoint(configId, data) {
  return request({ url: `/checkpoint/admin/${configId}`, method: 'put', data })
}

export function adminReviewCheckpoint(configId, action, comment) {
  return request({ url: `/checkpoint/admin/${configId}/review`, method: 'post', data: { action, comment } })
}

export function sendSOS(configId) {
  return request({ url: `/checkpoint/${configId}/sos`, method: 'post' })
}

export function recordFollowup(configId, keywordIndex, correct) {
  return request({ url: `/checkpoint/${configId}/keywords/followup`, method: 'post', data: { keywordIndex, correct } })
}

export function adminBatchApprove(subjectId) {
  return request({ url: '/checkpoint/admin/batch-approve', method: 'post', data: { subjectId } })
}
