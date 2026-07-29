import request from '@/utils/request'

// ─── 学生端 ───
export function listArticles(params) {
  return request({ url: '/knowledge-base/articles', method: 'get', params })
}
export function getArticle(id) {
  return request({ url: `/knowledge-base/articles/${id}`, method: 'get' })
}
export function getChapterTree(subjectId) {
  return request({ url: '/knowledge-base/chapters', method: 'get', params: { subjectId } })
}
export function getTags(subjectId) {
  return request({ url: '/knowledge-base/tags', method: 'get', params: { subjectId } })
}
export function getTodayReview() {
  return request({ url: '/knowledge-base/review/today', method: 'get' })
}
export function rateFlashcard(flashcardId, rating) {
  return request({ url: '/knowledge-base/review/rate', method: 'post', data: { flashcardId, rating } })
}
export function startLearning(articleId) {
  return request({ url: `/knowledge-base/articles/${articleId}/start-learning`, method: 'post' })
}
export function getProgress(subjectId) {
  return request({ url: '/knowledge-base/progress', method: 'get', params: { subjectId } })
}
export function toggleFavorite(articleId) {
  return request({ url: `/knowledge-base/articles/${articleId}/favorite`, method: 'post' })
}
export function getFavorites() {
  return request({ url: '/knowledge-base/favorites', method: 'get' })
}
export function searchArticles(keyword, subjectId, limit = 20) {
  return request({ url: '/knowledge-base/search', method: 'get', params: { keyword, subjectId, limit } })
}

// ─── 管理端 ───
export function getAdminArticles(params) {
  return request({ url: '/knowledge-base/admin/articles', method: 'get', params })
}
export function getAdminArticle(id) {
  return request({ url: `/knowledge-base/admin/articles/${id}`, method: 'get' })
}
export function createArticle(data) {
  return request({ url: '/knowledge-base/admin/articles', method: 'post', data })
}
export function updateArticle(id, data) {
  return request({ url: `/knowledge-base/admin/articles/${id}`, method: 'put', data })
}
export function deleteArticle(id) {
  return request({ url: `/knowledge-base/admin/articles/${id}`, method: 'delete' })
}
export function importMarkdown(basePath, subjectId) {
  return request({ url: '/knowledge-base/admin/import', method: 'post', data: { basePath, subjectId } })
}
export function generateFlashcards(articleId) {
  return request({ url: `/knowledge-base/admin/articles/${articleId}/generate-flashcards`, method: 'post' })
}
export function generateAllFlashcards() {
  return request({ url: '/knowledge-base/admin/generate-all-flashcards', method: 'post' })
}
export function getAdminStats(subjectId) {
  return request({ url: '/knowledge-base/admin/stats', method: 'get', params: { subjectId } })
}
export function getClassStats(subjectId) {
  return request({ url: '/knowledge-base/admin/class-stats', method: 'get', params: { subjectId } })
}

export function saveQuizResult(articleId, data) {
  return request({ url: `/knowledge-base/articles/${articleId}/quiz-result`, method: 'post', data })
}
export function getQuizHistory(articleId) {
  return request({ url: `/knowledge-base/articles/${articleId}/quiz-history`, method: 'get' })
}

export function getWeakAnalysis(subjectId) {
  return request({ url: '/knowledge-base/weak-analysis', method: 'get', params: { subjectId } })
}
export function getRecommendations(subjectId) {
  return request({ url: '/knowledge-base/recommendations', method: 'get', params: { subjectId } })
}
export function getDailyStats(subjectId = 24) {
  return request({ url: '/knowledge-base/daily-stats', method: 'get', params: { subjectId } })
}

/**
 * 每日卡片推荐
 */
export function getDailyCard() {
  return request({ url: '/knowledge-base/daily-card', method: 'get', timeout: 3000 })
}

/**
 * 提交后错题卡片推荐
 */
export function getRelatedCards(submissionId, limit = 5) {
  return request({ url: `/knowledge-base/submissions/${submissionId}/related-cards`, method: 'get', params: { limit }, timeout: 3000 })
}

/**
 * 按知识点获取卡片列表
 */
export function getCardsByNodeId(nodeId, limit = 5) {
  return request({ url: `/knowledge-base/nodes/${nodeId}/cards`, method: 'get', params: { limit }, timeout: 3000 })
}

/**
 * 考前突击包
 */
export function getExamPrepPack(taskId) {
  return request({ url: '/knowledge-base/exam-prep-pack', method: 'get', params: { taskId }, timeout: 3000 })
}

/**
 * 按专业分组返回学科列表
 */
export function getSubjectsGrouped() {
  return request({ url: '/knowledge-base/subjects-grouped', method: 'get' })
}

// ── v167: 卡片审核 + 考纲权重 + AI评估 ──
export function getReviewQueue(params) {
  return request({ url: '/knowledge-base/flashcards/review-queue', method: 'get', params })
}
export function batchReviewCards(data) {
  return request({ url: '/knowledge-base/flashcards/batch-review', method: 'post', data })
}
export function setExamWeight(data) {
  return request({ url: '/knowledge-base/nodes/set-exam-weight', method: 'post', data })
}
export function batchEvaluateCards(data) {
  return request({ url: '/knowledge-base/flashcards/batch-evaluate', method: 'post', data })
}
export function batchEvaluateAllCards() {
  return request({ url: '/knowledge-base/flashcards/batch-evaluate-all', method: 'post' })
}
export function getEvaluationProgress() {
  return request({ url: '/knowledge-base/flashcards/evaluation-progress', method: 'get' })
}
export function aiGenerateFlashcards(articleId) {
  return request({ url: `/knowledge-base/articles/${articleId}/ai-generate-flashcards`, method: 'post' })
}

// ── v169: 卡片清空 + 批量重生 ──
export function clearAllFlashcards(subjectId) {
  return request({ url: '/knowledge-base/flashcards/clear-all', method: 'delete', params: { subjectId } })
}
export function regenerateAllFlashcards(subjectId) {
  return request({ url: '/knowledge-base/flashcards/regenerate-all', method: 'post', params: { subjectId } })
}
export function getRegenerationProgress(subjectId) {
  return request({ url: '/knowledge-base/flashcards/regeneration-progress', method: 'get', params: { subjectId } })
}

/**
 * 学生端：知识清单（教师 AI 生成后发布）
 */
export function listChecklists(params) {
  return request({ url: '/knowledge-base/checklists', method: 'get', params })
}
export function getChecklistDetail(id) {
  return request({ url: `/knowledge-base/checklists/${id}`, method: 'get' })
}
