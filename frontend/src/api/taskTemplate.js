import request from '@/utils/request'

export function getTemplateList(params) {
  return request({ url: '/task-templates', method: 'get', params })
}
export function getTemplate(id) {
  return request({ url: `/task-templates/${id}`, method: 'get' })
}
export function saveAsTemplate(data) {
  return request({ url: '/task-templates/actions/save-from-task', method: 'post', data })
}
export function createTaskFromTemplate(id) {
  return request({ url: `/task-templates/${id}/actions/create-task`, method: 'post' })
}
export function updateTemplateScope(id, scope) {
  return request({ url: `/task-templates/${id}/actions/scope`, method: 'put', data: { scope } })
}
export function createTemplate(data) {
  return request({ url: '/task-templates', method: 'post', data })
}
export function deleteTemplate(id) {
  return request({ url: `/task-templates/${id}`, method: 'delete' })
}
