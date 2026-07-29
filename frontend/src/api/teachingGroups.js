import request from '@/utils/request'

export function getTeachingGroups() { return request({ url: '/teaching-group/list', method: 'get' }) }
export function createTeachingGroup(data) { return request({ url: '/teaching-group', method: 'post', data }) }
export function updateTeachingGroup(id, data) { return request({ url: `/teaching-group/${id}`, method: 'put', data }) }
export function deleteTeachingGroup(id) { return request({ url: `/teaching-group/${id}`, method: 'delete' }) }
export function addTeachingMember(id, teacherId) { return request({ url: `/teaching-group/${id}/actions/add-member`, method: 'post', data: { teacherId } }) }
export function removeTeachingMember(id, teacherId) { return request({ url: `/teaching-group/${id}/actions/remove-member/${teacherId}`, method: 'delete' }) }
export function setTeachingLeader(id, teacherId) { return request({ url: `/teaching-group/${id}/actions/set-leader`, method: 'post', data: { teacherId } }) }
export function removeTeachingLeader(id, teacherId) { return request({ url: `/teaching-group/${id}/actions/remove-leader/${teacherId}`, method: 'delete' }) }
export function getTeachingGroupMembers(id) { return request({ url: `/teaching-group/${id}/members`, method: 'get' }) }
export function getLessonPrepGroupMembers(id) { return request({ url: `/lesson-prep-group/${id}/members`, method: 'get' }) }
export function getMyTeachingGroups() { return request({ url: '/teaching-group/actions/my-groups', method: 'get' }) }
export function getTeacherGroupIds(teacherId) { return request({ url: `/teaching-group/actions/teacher/${teacherId}/group-ids`, method: 'get' }) }
export function getTeacherLessonGroupIds(teacherId) { return request({ url: `/lesson-prep-group/actions/teacher/${teacherId}/group-ids`, method: 'get' }) }

export function getLessonPrepGroups() { return request({ url: '/lesson-prep-group/list', method: 'get' }) }
export function createLessonPrepGroup(data) { return request({ url: '/lesson-prep-group', method: 'post', data }) }
export function updateLessonPrepGroup(id, data) { return request({ url: `/lesson-prep-group/${id}`, method: 'put', data }) }
export function deleteLessonPrepGroup(id) { return request({ url: `/lesson-prep-group/${id}`, method: 'delete' }) }
export function addLessonPrepMember(id, teacherId) { return request({ url: `/lesson-prep-group/${id}/actions/add-member`, method: 'post', data: { teacherId } }) }
export function removeLessonPrepMember(id, teacherId) { return request({ url: `/lesson-prep-group/${id}/actions/remove-member/${teacherId}`, method: 'delete' }) }
export function setLessonPrepLeader(id, teacherId) { return request({ url: `/lesson-prep-group/${id}/actions/set-leader`, method: 'post', data: { teacherId } }) }
