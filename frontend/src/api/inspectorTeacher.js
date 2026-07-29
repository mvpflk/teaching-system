import request from '@/utils/request'

// —— 我的整改问题 ——
export function getMyIssues(params) { return request({ url: '/teacher/inspection/issues', method: 'get', params }) }
export function startIssueProgress(id) { return request({ url: `/teacher/inspection/issues/${id}/actions/start`, method: 'post' }) }
export function resolveIssue(id, resolveComment) { return request({ url: `/teacher/inspection/issues/${id}/actions/resolve`, method: 'post', params: { resolveComment } }) }

// —— 我的通知书 ——
export function getMyNotices(params) { return request({ url: '/teacher/inspection/notices', method: 'get', params }) }
export function acknowledgeNotice(id) { return request({ url: `/teacher/inspection/notices/${id}/actions/acknowledge`, method: 'post' }) }
export function complyNotice(id) { return request({ url: `/teacher/inspection/notices/${id}/actions/comply`, method: 'post' }) }
