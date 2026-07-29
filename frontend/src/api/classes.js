import request from '@/utils/request'

export function getClassList(grade) {
  return request({ url: '/class/list', method: 'get', params: grade ? { grade } : {} })
}

export function getClassDetail(id) {
  return request({ url: `/class/${id}`, method: 'get' })
}

export function createClass(data) {
  return request({ url: '/class', method: 'post', data })
}

export function updateClass(id, data) {
  return request({ url: `/class/${id}`, method: 'put', data })
}

export function deleteClass(id) {
  return request({ url: `/class/${id}`, method: 'delete' })
}

export function getStudents(classId) {
  return request({ url: `/class/${classId}/students`, method: 'get' })
}

export function addStudent(classId, studentIds) {
  // studentIds可以是单个数字或数组
  const ids = Array.isArray(studentIds) ? studentIds : [studentIds]
  return request({ url: `/class/${classId}/students`, method: 'post', data: { studentIds: ids } })
}

export function removeStudent(classId, studentId) {
  return request({ url: `/class/${classId}/students/${studentId}`, method: 'delete' })
}

export function getAvailableStudents() {
  return request({ url: '/class/actions/available-students', method: 'get' })
}

export function getTeachers() {
  return request({ url: '/class/teachers', method: 'get' })
}

export function getMyClasses() {
  return request({ url: '/class/actions/my', method: 'get' })
}

export function batchUpdateClassType(data) {
  return request({ url: '/class/actions/batch-update-type', method: 'put', data })
}

/** 班级任务详情聚合 */
export function getTaskDetail(classId, taskId) {
  return request({ url: `/class/${classId}/actions/task-detail`, method: 'get', params: { taskId } })
}
