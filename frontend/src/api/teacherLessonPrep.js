import request from '@/utils/request'

export function getMyLessonPrepGroup() { return request({ url: '/teacher/lesson-prep/my-group', method: 'get' }) }
export function getLessonPrepRecords(params) { return request({ url: '/teacher/lesson-prep/records', method: 'get', params }) }
export function createLessonPrepRecord(data) { return request({ url: '/teacher/lesson-prep/records', method: 'post', data }) }
export function updateLessonPrepRecord(id, data) { return request({ url: `/teacher/lesson-prep/records/${id}`, method: 'put', data }) }
export function deleteLessonPrepRecord(id) { return request({ url: `/teacher/lesson-prep/records/${id}`, method: 'delete' }) }
export function getLessonPrepMembers() { return request({ url: '/teacher/lesson-prep/members', method: 'get' }) }
export function getLessonPrepPendingReviews() { return request({ url: '/teacher/lesson-prep/pending-reviews', method: 'get' }) }
