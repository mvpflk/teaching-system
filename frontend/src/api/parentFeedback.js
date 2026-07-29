import request from '@/utils/request'

export function getFeedbackForms(params) { return request({ url: '/inspector/feedback/forms', method: 'get', params }) }
export function createFeedbackForm(data) { return request({ url: '/inspector/feedback/forms', method: 'post', data }) }
export function updateFeedbackForm(id, data) { return request({ url: `/inspector/feedback/forms/${id}`, method: 'put', data }) }
export function deleteFeedbackForm(id) { return request({ url: `/inspector/feedback/forms/${id}`, method: 'delete' }) }
export function sendFeedbackForm(id) { return request({ url: `/inspector/feedback/forms/${id}/send`, method: 'post' }) }
export function closeFeedbackForm(id) { return request({ url: `/inspector/feedback/forms/${id}/close`, method: 'post' }) }
export function getFeedbackResponses(id) { return request({ url: `/inspector/feedback/forms/${id}/responses`, method: 'get' }) }
export function getFeedbackStats(id) { return request({ url: `/inspector/feedback/forms/${id}/stats`, method: 'get' }) }
export function generateFeedbackSummary(id) { return request({ url: `/inspector/feedback/forms/${id}/generate-summary`, method: 'post' }) }

export function getPendingFeedbackForms() { return request({ url: '/parent/feedback/pending', method: 'get' }) }
export function submitFeedbackResponse(data) { return request({ url: '/parent/feedback/respond', method: 'post', data }) }
