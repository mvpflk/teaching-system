import request from '@/utils/request'

export function getPendingReviews() {
  return request({ url: '/peer-reviews/pending', method: 'get' })
}

export function submitReview(reviewId, data) {
  return request({ url: `/peer-reviews/${reviewId}/actions/submit`, method: 'post', data })
}

export function assignPeerReviews(taskId) {
  return request({ url: `/peer-reviews/${taskId}/actions/assign`, method: 'post' })
}

export function getPeerReviewProgress(taskId) {
  return request({ url: `/peer-reviews/${taskId}/progress`, method: 'get' })
}

export function getPeerReviewDetails(submissionId) {
  return request({ url: `/peer-reviews/submissions/${submissionId}`, method: 'get' })
}

export function fusePeerScores(taskId) {
  return request({ url: `/peer-reviews/${taskId}/actions/fuse-scores`, method: 'post' })
}

export function getPeerReviewQuality(taskId) {
  return request({ url: `/peer-reviews/${taskId}/actions/quality`, method: 'get' })
}

export function getPeerComments(submissionId) {
  return request({ url: `/peer-reviews/submissions/${submissionId}/peer-comments`, method: 'get' })
}
