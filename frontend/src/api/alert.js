import request from '@/utils/request'

// 规则
export function getRules() {
  return request({ url: '/alert/rules', method: 'get' })
}
export function saveRule(data) {
  return request({ url: '/alert/rules', method: 'post', data })
}
export function deleteRule(id) {
  return request({ url: `/alert/rules/${id}`, method: 'delete' })
}

// 预警记录
export function getAlertRecords(params) {
  return request({ url: '/alert/records', method: 'get', params })
}
export function handleAlert(id, status) {
  return request({ url: `/alert/records/${id}/handle`, method: 'put', data: { status } })
}

// 家长端
export function getChildAlerts() {
  return request({ url: '/parent/alerts', method: 'get' })
}
export function getChildUnreadAlertCount() {
  return request({ url: '/parent/alerts/unread-count', method: 'get' })
}
