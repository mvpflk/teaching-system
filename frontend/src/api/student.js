import request from '@/utils/request'

export function getStudentList(params) {
  return request({ url: '/student/list', method: 'get', params })
}

export function createStudent(data) {
  return request({ url: '/student', method: 'post', data })
}

export function updateStudent(id, data) {
  return request({ url: `/student/${id}`, method: 'put', data })
}

export function deleteStudent(id) {
  return request({ url: `/student/${id}`, method: 'delete' })
}

export function batchDeleteStudents(ids) {
  return request({ url: '/student/actions/batch-delete', method: 'post', data: { ids } })
}

export function updateStudentStatus(id, status) {
  return request({ url: `/student/${id}/actions/status`, method: 'put', data: { status } })
}

export function batchImportStudents(formData) {
  return request({ url: '/student/actions/batch-import', method: 'post', data: formData })
}

export function batchGraduate(data) {
  return request({ url: '/student/actions/batch-graduate', method: 'post', data })
}

export function getStudentGrowth(studentId, params) {
  return request({ url: `/student/growth/${studentId}`, method: 'get', params })
}
