import request from '@/utils/request'

// —— 巡视记录 ——
export function createRecord(data) { return request({ url: '/inspector/manage/records', method: 'post', data }) }
export function getRecords(params) { return request({ url: '/inspector/manage/records', method: 'get', params }) }
export function getRecord(id) { return request({ url: `/inspector/manage/records/${id}`, method: 'get' }) }
export function updateRecord(id, data) { return request({ url: `/inspector/manage/records/${id}`, method: 'put', data }) }
export function deleteRecord(id) { return request({ url: `/inspector/manage/records/${id}`, method: 'delete' }) }

// —— 问题台账 ——
export function createIssue(data) { return request({ url: '/inspector/manage/issues', method: 'post', data }) }
export function getIssues(params) { return request({ url: '/inspector/manage/issues', method: 'get', params }) }
export function getIssue(id) { return request({ url: `/inspector/manage/issues/${id}`, method: 'get' }) }
export function updateIssue(id, data) { return request({ url: `/inspector/manage/issues/${id}`, method: 'put', data }) }
export function deleteIssue(id) { return request({ url: `/inspector/manage/issues/${id}`, method: 'delete' }) }
export function assignIssue(id, teacherId, deadline) { return request({ url: `/inspector/manage/issues/${id}/actions/assign`, method: 'post', params: { teacherId, deadline } }) }
export function startIssueProgress(id, teacherId) { return request({ url: `/inspector/manage/issues/${id}/actions/start`, method: 'post', params: { teacherId } }) }
export function resolveIssue(id, teacherId, resolveComment) { return request({ url: `/inspector/manage/issues/${id}/actions/resolve`, method: 'post', params: { teacherId, resolveComment } }) }
export function verifyIssue(id, inspectorId, approved, verifyComment) { return request({ url: `/inspector/manage/issues/${id}/actions/verify`, method: 'post', params: { inspectorId, approved, verifyComment } }) }
export function getIssueComments(issueId) { return request({ url: `/inspector/manage/issues/${issueId}/actions/comment`, method: 'get' }) }
export function addIssueComment(id, userId, content, isSystem) { return request({ url: `/inspector/manage/issues/${id}/actions/comment`, method: 'post', params: { userId, content, isSystem } }) }
export function getIssueStats() { return request({ url: '/inspector/manage/issues/stats', method: 'get' }) }

// —— 整改通知书 ——
export function createNotice(data) { return request({ url: '/inspector/manage/notices', method: 'post', data }) }
export function getNotices(params) { return request({ url: '/inspector/manage/notices', method: 'get', params }) }

// —— 巡视报告 ——
export function getReports(params) { return request({ url: '/inspector/manage/reports', method: 'get', params }) }
export function generateReport(params) { return request({ url: '/inspector/manage/reports/actions/generate', method: 'post', params }) }
export function getReport(id) { return request({ url: `/inspector/manage/reports/${id}`, method: 'get' }) }
export function publishReport(id) { return request({ url: `/inspector/manage/reports/${id}/actions/publish`, method: 'post' }) }
export function deleteReport(id) { return request({ url: `/inspector/manage/reports/${id}`, method: 'delete' }) }

// —— 课堂巡课 ——
export function createClassroomPatrol(data) { return request({ url: '/inspector/manage/classroom-patrols', method: 'post', data }) }
export function getClassroomPatrols(params) { return request({ url: '/inspector/manage/classroom-patrols', method: 'get', params }) }
export function getClassroomPatrol(id) { return request({ url: `/inspector/manage/classroom-patrols/${id}`, method: 'get' }) }
export function updateClassroomPatrol(id, data) { return request({ url: `/inspector/manage/classroom-patrols/${id}`, method: 'put', data }) }
export function deleteClassroomPatrol(id) { return request({ url: `/inspector/manage/classroom-patrols/${id}`, method: 'delete' }) }
export function getRecentPatrols(params) { return request({ url: '/inspector/manage/classroom-patrols/recent', method: 'get', params }) }

// —— 德育巡视 ——
export function createMoralInspection(data) { return request({ url: '/inspector/manage/moral-inspections', method: 'post', data }) }
export function getMoralInspections(params) { return request({ url: '/inspector/manage/moral-inspections', method: 'get', params }) }
export function getMoralInspection(id) { return request({ url: `/inspector/manage/moral-inspections/${id}`, method: 'get' }) }
export function updateMoralInspection(id, data) { return request({ url: `/inspector/manage/moral-inspections/${id}`, method: 'put', data }) }
export function deleteMoralInspection(id) { return request({ url: `/inspector/manage/moral-inspections/${id}`, method: 'delete' }) }
export function getRecentMoralInspections(params) { return request({ url: '/inspector/manage/moral-inspections/recent', method: 'get', params }) }

// —— 教研活动 ——
export function createResearchActivity(data) { return request({ url: '/inspector/manage/research-activities', method: 'post', data }) }
export function getResearchActivities(params) { return request({ url: '/inspector/manage/research-activities', method: 'get', params }) }
export function getResearchActivity(id) { return request({ url: `/inspector/manage/research-activities/${id}`, method: 'get' }) }
export function updateResearchActivity(id, data) { return request({ url: `/inspector/manage/research-activities/${id}`, method: 'put', data }) }
export function deleteResearchActivity(id) { return request({ url: `/inspector/manage/research-activities/${id}`, method: 'delete' }) }
export function getResearchActivityStats() { return request({ url: '/inspector/manage/research-activities/stats', method: 'get' }) }

// —— 家长反馈 ——
export function createParentFeedback(data) { return request({ url: '/inspector/manage/parent-feedback', method: 'post', data }) }
export function getParentFeedback(params) { return request({ url: '/inspector/manage/parent-feedback', method: 'get', params }) }
export function getParentFeedbackById(id) { return request({ url: `/inspector/manage/parent-feedback/${id}`, method: 'get' }) }
export function updateParentFeedback(id, data) { return request({ url: `/inspector/manage/parent-feedback/${id}`, method: 'put', data }) }
export function deleteParentFeedback(id) { return request({ url: `/inspector/manage/parent-feedback/${id}`, method: 'delete' }) }
export function getLatestParentFeedback() { return request({ url: '/inspector/manage/parent-feedback/latest', method: 'get' }) }

// —— 审核流水 ——
export function getReviewFlow(params) { return request({ url: '/inspector/manage/review-flow', method: 'get', params }) }
