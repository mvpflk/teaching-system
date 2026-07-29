import request from '@/utils/request'

// ── 权限 ──
export function checkPermission() { return request({ url: '/typing/check-permission', method: 'get' }) }
export function getTypingMajors() { return request({ url: '/typing/settings/majors', method: 'get' }) }
export function updateTypingMajors(data) { return request({ url: '/typing/settings/majors', method: 'put', data }) }

// ── 文本管理 ──
export function getTexts(params) { return request({ url: '/typing/texts', method: 'get', params }) }
export function createText(data) { return request({ url: '/typing/texts', method: 'post', data }) }
export function updateText(id, data) { return request({ url: `/typing/texts/${id}`, method: 'put', data }) }
export function deleteText(id) { return request({ url: `/typing/texts/${id}`, method: 'delete' }) }

// ── 竞赛 ──
export function getCompetitions(params) { return request({ url: '/typing/competitions', method: 'get', params }) }
export function createCompetition(data) { return request({ url: '/typing/competitions', method: 'post', data }) }
export function startCompetition(id) { return request({ url: `/typing/competitions/${id}/start`, method: 'put' }) }
export function finishCompetition(id) { return request({ url: `/typing/competitions/${id}/finish`, method: 'put' }) }
export function deleteCompetition(id) { return request({ url: `/typing/competitions/${id}`, method: 'delete' }) }
export function getCurrentCompetition() { return request({ url: '/typing/competitions/current', method: 'get' }) }
export function getRanking(competitionId) { return request({ url: `/typing/competitions/${competitionId}/ranking`, method: 'get' }) }
export function getDashboard(competitionId) { return request({ url: `/typing/competitions/${competitionId}/dashboard`, method: 'get' }) }
export function submitResult(competitionId, data) { return request({ url: `/typing/competitions/${competitionId}/submit`, method: 'post', data }) }
export function exportResults(competitionId) { return request({ url: `/typing/competitions/${competitionId}/export`, method: 'get' }) }

// ── 过程 ──
export function reportProgress(data) { return request({ url: '/typing/session/progress', method: 'post', data }) }
export function getPracticeText(textId, difficulty, language) { return request({ url: '/typing/practice/text', method: 'get', params: { textId, difficulty, language } }) }
export function saveRecord(data) { return request({ url: '/typing/records', method: 'post', data }) }

// ── 学生素材库 ──
export function getStudentTexts(params) { return request({ url: '/typing/student/texts', method: 'get', params }) }
export function getStudentTextCategories() { return request({ url: '/typing/student/texts/categories', method: 'get' }) }

// ── 竞赛回放 ──
export function getCompetitionReplay(compId, studentId) { return request({ url: `/typing/competitions/${compId}/replay/${studentId}`, method: 'get' }) }

// ── 速度趋势 ──
export function getStudentSpeedTrend(limit = 20) {
  return request({ url: '/typing/statistics', method: 'get', params: { limit } })
}

// ── 历史/游戏化 ──
export function getStudentHistory() { return request({ url: '/typing/student/history', method: 'get' }) }
export function getWrongWords() { return request({ url: '/typing/student/wrong-words', method: 'get' }) }
export function getStudentLevels() { return request({ url: '/typing/student/levels', method: 'get' }) }
export function addExp(data) { return request({ url: '/typing/student/exp', method: 'post', data }) }
