import request from '@/utils/request'

export function getNotificationList(params) {
  return request({ url: '/notification/list', method: 'get', params })
}

export function markRead(id) {
  return request({ url: `/notification/${id}/actions/read`, method: 'put' })
}

export function markAllRead() {
  return request({ url: '/notification/actions/read-all', method: 'put' })
}

export function createNotification(data) {
  return request({ url: '/notification/actions/create', method: 'post', data })
}
