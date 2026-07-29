import request from '@/utils/request'

export function getTemplateList() {
  return request({ url: '/templates/list', method: 'get' })
}

export function createTemplate(data) {
  return request({ url: '/templates', method: 'post', data })
}

export function updateTemplate(id, data) {
  return request({ url: `/templates/${id}`, method: 'put', data })
}

export function deleteTemplate(id) {
  return request({ url: `/templates/${id}`, method: 'delete' })
}
