import request from '@/utils/request'

export function getTypeConfigList(stageId) {
  return request({ url: '/class-type-config/list', method: 'get', params: stageId ? { stageId } : {} })
}

export function getTypeConfigDetail(id) {
  return request({ url: `/class-type-config/${id}`, method: 'get' })
}

export function createTypeConfig(data) {
  return request({ url: '/class-type-config', method: 'post', data })
}

export function updateTypeConfig(id, data) {
  return request({ url: `/class-type-config/${id}`, method: 'put', data })
}

export function deleteTypeConfig(id) {
  return request({ url: `/class-type-config/${id}`, method: 'delete' })
}


