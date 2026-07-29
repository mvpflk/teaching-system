import request from '@/utils/request'

export function getTeacherList(params) {
  return request({ url: '/teacher/list', method: 'get', params })
}

export function createTeacher(data) {
  return request({ url: '/teacher', method: 'post', data })
}

export function updateTeacher(userId, data) {
  return request({ url: `/teacher/${userId}`, method: 'put', data })
}

export function deleteTeacher(userId) {
  return request({ url: `/teacher/${userId}`, method: 'delete' })
}
export function getTeacher(userId) { return request({ url: `/teacher/${userId}`, method: 'get' }) }
export function getTeacherAssignments(userId) { return request({ url: `/teacher/${userId}/assignments`, method: 'get' }) }
export function setTeacherAssignments(userId, data) { return request({ url: `/teacher/${userId}/actions/assignments`, method: 'put', data }) }
export function setHeadClass(userId, classId) { return request({ url: `/teacher/${userId}/actions/head-class`, method: 'put', data: { classId } }) }

export function getQuickComments()          { return request({ url: '/teacher/quick-comments', method: 'get' }) }
export function addQuickComment(data)       { return request({ url: '/teacher/quick-comments', method: 'post', data }) }
export function deleteQuickComment(id)      { return request({ url: `/teacher/quick-comments/${id}`, method: 'delete' }) }
