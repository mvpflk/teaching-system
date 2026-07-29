import request from '@/utils/request'

export function getMyResearchGroup() { return request({ url: '/teacher/research/my-group', method: 'get' }) }
export function getResearchActivities(params) { return request({ url: '/teacher/research/activities', method: 'get', params }) }
export function createResearchActivity(data) { return request({ url: '/teacher/research/activities', method: 'post', data }) }
export function updateResearchActivity(id, data) { return request({ url: `/teacher/research/activities/${id}`, method: 'put', data }) }
export function deleteResearchActivity(id) { return request({ url: `/teacher/research/activities/${id}`, method: 'delete' }) }
export function getResearchMembers() { return request({ url: '/teacher/research/members', method: 'get' }) }
export function getPendingReviews() { return request({ url: '/teacher/research/pending-reviews', method: 'get' }) }
