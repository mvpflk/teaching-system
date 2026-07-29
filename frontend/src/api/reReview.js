import request from '@/utils/request'

export function requestReReview(data) {
  return request({ url: '/re-reviews/actions/request', method: 'post', data })
}
