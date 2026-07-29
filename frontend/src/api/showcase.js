import request from '@/utils/request'

/** 分页查询展示作品 */
export function getShowcaseList(params) {
  return request({ url: '/showcase', method: 'get', params })
}

/** 获取作品详情 */
export function getShowcaseDetail(id) {
  return request({ url: `/showcase/${id}`, method: 'get' })
}

/** 点赞/取消点赞 */
export function toggleLike(id) {
  return request({ url: `/showcase/${id}/like`, method: 'post' })
}

/** 本周之星 */
export function getWeeklyStars() {
  return request({ url: '/showcase/weekly-stars', method: 'get' })
}

/** 获取评论列表 */
export function getComments(workId) {
  return request({ url: `/showcase/${workId}/comments`, method: 'get' })
}

/** 发表评论 */
export function addComment(workId, content) {
  return request({ url: `/showcase/${workId}/comments`, method: 'post', data: { content } })
}
