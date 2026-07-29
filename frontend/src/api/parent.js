import request from '@/utils/request'

export function getMyChildren() {
  return request({ url: '/parent/children', method: 'get' })
}

export function getChildGrades(studentId) {
  return request({ url: `/parent/children/${studentId}/grades`, method: 'get' })
}

export function getChildTimeline(studentId) {
  return request({ url: `/parent/children/${studentId}/timeline`, method: 'get' })
}

export function getChildHomework(studentId) {
  return request({ url: `/parent/children/${studentId}/homework`, method: 'get' })
}

export function getChildAlerts() {
  return request({ url: '/parent/alerts', method: 'get' })
}

export function getUnreadAlertCount() {
  return request({ url: '/parent/alerts/unread-count', method: 'get' })
}

export function acknowledgeAlert(alertId) {
  return request({ url: `/parent/alerts/${alertId}/acknowledge`, method: 'post' })
}

export function bindChild(data) {
  return request({ url: '/parent/bind', method: 'post', data })
}

export function getChildPractices(studentId) {
  return request({ url: `/parent/child-practices/${studentId}`, method: 'get' })
}
