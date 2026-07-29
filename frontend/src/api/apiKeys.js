import request from '@/utils/request'

export function getApiKeys() {
  return request({ url: '/agent/api-keys', method: 'get' })
}

export function createApiKey(data) {
  return request({ url: '/agent/api-keys', method: 'post', data })
}

export function updateApiKey(id, data) {
  return request({ url: `/agent/api-keys/${id}`, method: 'put', data })
}

export function deleteApiKey(id) {
  return request({ url: `/agent/api-keys/${id}`, method: 'delete' })
}

export function setActiveApiKey(id, active) {
  return request({ url: `/agent/api-keys/${id}/active`, method: 'put', data: { active } })
}