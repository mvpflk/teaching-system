import request from '@/utils/request'

// ── 系统信息 ──
export function getMaintenanceInfo() { return request({ url: '/settings/admin/maintenance/info', method: 'get' }) }
export function importMaintenance(data) { return request({ url: '/settings/admin/maintenance/import', method: 'post', data }) }
export function clearMaintenance(data) { return request({ url: '/settings/admin/maintenance/clear', method: 'post', data }) }

// ── 审计日志 ──
export function getAuditLogs(params) { return request({ url: '/settings/admin/audit-logs', method: 'get', params }) }
export function getAuditEventDist(params) { return request({ url: '/settings/admin/audit-logs/analysis/event-distribution', method: 'get', params }) }
export function getAuditActiveUsers(params) { return request({ url: '/settings/admin/audit-logs/analysis/active-users', method: 'get', params }) }
export function getAuditHourlyTrend(params) { return request({ url: '/settings/admin/audit-logs/analysis/hourly-trend', method: 'get', params }) }

// ── 管理仪表盘 ──
export function getAdminOverview() { return request({ url: '/settings/admin/overview', method: 'get' }) }
export function getParamCategories() { return request({ url: '/settings/admin/params/categories', method: 'get' }) }
export function getAdminParams(params) { return request({ url: '/settings/admin/params', method: 'get', params }) }
export function updateAdminParams(data) { return request({ url: '/settings/admin/params/batch', method: 'put', data }) }

// ── 系统 Logo ──
export function getSystemLogo() { return request({ url: '/system/logo', method: 'get' }) }
export function uploadLogo(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/settings/logo', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
export function deleteLogo() { return request({ url: '/settings/logo', method: 'delete' }) }

// ── 系统运行监控 ──
export function getSystemStatus() { return request({ url: '/system/status', method: 'get' }) }
