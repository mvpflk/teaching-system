import request from '@/utils/request'

export function getProfile() { return request({ url: '/profile/actions/info', method: 'get' }) }
export function updateProfile(data) { return request({ url: '/profile/actions/update', method: 'put', data }) }
export function updatePassword(data) { return request({ url: '/profile/actions/password', method: 'put', data }) }
export function uploadAvatar(formData) { return request({ url: '/profile/actions/avatar', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } }) }
export function adminResetPassword(userId, data) { return request({ url: `/teacher/${userId}/actions/reset-password`, method: 'put', data }) }
