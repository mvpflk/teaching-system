import request from '@/utils/request'

export function getDashboard() { return request({ url: '/inspector/actions/dashboard', method: 'get' }) }
export function getTeacherActivity() { return request({ url: '/inspector/actions/teacher-activity', method: 'get' }) }
export function getScoreAnalysis(params) { return request({ url: '/inspector/actions/score-analysis', method: 'get', params }) }
export function getPeerReviewStats(params) { return request({ url: '/inspector/actions/peer-review-stats', method: 'get', params }) }
export function getReviewProgress() { return request({ url: '/inspector/actions/review-progress', method: 'get' }) }
export function getCreditStats() { return request({ url: '/inspector/actions/credit-stats', method: 'get' }) }

// ── 预警系统 ─────────────────────────────────────
export function getAlertRules() { return request({ url: '/inspector/actions/alerts/rules', method: 'get' }) }
export function updateAlertRule(id, data) { return request({ url: `/inspector/actions/alerts/rules/${id}`, method: 'put', data }) }
export function getAlertLogs(params) { return request({ url: '/inspector/actions/alerts/logs', method: 'get', params }) }
export function markAlertRead(id) { return request({ url: `/inspector/actions/alerts/read/${id}`, method: 'post' }) }
export function markAllAlertsRead() { return request({ url: '/inspector/actions/alerts/read-all', method: 'post' }) }

// ── 趋势与档案 ────────────────────────────────────
export function getDashboardTrend(period) { return request({ url: '/inspector/actions/dashboard-trend', method: 'get', params: { period } }) }
export function getTeacherProfile(teacherId) { return request({ url: `/inspector/actions/teacher-profile/${teacherId}`, method: 'get' }) }
export function getClassProfile(classId) { return request({ url: `/inspector/actions/class-profile/${classId}`, method: 'get' }) }

// ── AI 助手 ───────────────────────────────────────
export function getWeeklySummary(params) { return request({ url: '/inspector/ai/weekly-summary', method: 'get', params }) }
export function getAnomalies() { return request({ url: '/inspector/ai/anomalies', method: 'get' }) }
export function getRecommendations() { return request({ url: '/inspector/ai/recommendations', method: 'get' }) }
export function getTeachingResearchAnalysis() { return request({ url: '/inspector/ai/teaching-research-analysis', method: 'get' }) }
export function getLessonPrepAnalysis() { return request({ url: '/inspector/ai/lesson-prep-analysis', method: 'get' }) }

// ── 教研质量 ───────────────────────────────────────
export function getTeachingGroupQuality() { return request({ url: '/inspector/teaching-quality', method: 'get' }) }

// ── 实训监控 ───────────────────────────────────────
export function getPracticeStats() { return request({ url: '/inspector/practice-stats', method: 'get' }) }
