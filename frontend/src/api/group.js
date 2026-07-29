import request from '@/utils/request'

export function getGroups(classId) {
  return request({ url: `/groups/class/${classId}`, method: 'get' })
}
export function createGroup(classId, name) {
  return request({ url: `/groups/class/${classId}`, method: 'post', data: { name } })
}
export function deleteGroup(groupId) {
  return request({ url: `/groups/${groupId}`, method: 'delete' })
}
export function getMembers(groupId) {
  return request({ url: `/groups/${groupId}/members`, method: 'get' })
}
export function addMember(groupId, studentId) {
  return request({ url: `/groups/${groupId}/members`, method: 'post', data: { studentId } })
}
export function removeMember(groupId, studentId) {
  return request({ url: `/groups/${groupId}/members/${studentId}`, method: 'delete' })
}
