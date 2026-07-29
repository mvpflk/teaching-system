import request from '@/utils/request'

export function getCategories() {
  return request({ url: '/bbs/categories', method: 'get' })
}

export function getPosts(params) {
  return request({ url: '/bbs/posts', method: 'get', params })
}

export function getPostDetail(id) {
  return request({ url: `/bbs/posts/${id}`, method: 'get' })
}

export function createPost(data) {
  return request({ url: '/bbs/posts', method: 'post', data })
}

export function updatePost(id, data) {
  return request({ url: `/bbs/posts/${id}`, method: 'put', data })
}

export function deletePost(id) {
  return request({ url: `/bbs/posts/${id}`, method: 'delete' })
}

export function toggleSticky(id) {
  return request({ url: `/bbs/posts/${id}/actions/sticky`, method: 'post' })
}

export function toggleHighlight(id) {
  return request({ url: `/bbs/posts/${id}/actions/highlight`, method: 'post' })
}

export function getReplies(postId, params = {}) {
  return request({ url: `/bbs/posts/${postId}/replies`, method: 'get', params })
}

export function createReply(postId, data) {
  return request({ url: `/bbs/posts/${postId}/replies`, method: 'post', data })
}

export function deleteReply(replyId) {
  return request({ url: `/bbs/replies/${replyId}`, method: 'delete' })
}

export function toggleLike(data) {
  return request({ url: '/bbs/actions/toggle-like', method: 'post', data })
}

export function toggleBookmark(data) {
  return request({ url: '/bbs/actions/toggle-bookmark', method: 'post', data })
}

export function createMoralPost(data) {
  return request({ url: '/bbs/actions/post/moral', method: 'post', data })
}

export function useStickyCoupon(id) {
  return request({ url: `/bbs/posts/${id}/actions/sticky-coupon`, method: 'post' })
}

export function getHotPosts(limit = 5) {
  return request({ url: '/bbs/hot-posts', method: 'get', params: { limit } })
}

export function getActiveUsers(limit = 5) {
  return request({ url: '/bbs/active-users', method: 'get', params: { limit } })
}

export function getMyReplies() {
  return request({ url: '/bbs/actions/my-replies', method: 'get' })
}

export function getMyPosts() {
  return request({ url: '/bbs/actions/my-posts', method: 'get' })
}

export function getMyBookmarks() {
  return request({ url: '/bbs/actions/my-bookmarks', method: 'get' })
}

// 管理员操作
export function muteUser(data) {
  return request({ url: '/bbs/admin/mute', method: 'post', data })
}
export function unmuteUser(userId) {
  return request({ url: `/bbs/admin/unmute/${userId}`, method: 'post' })
}
export function getMutedUsers() {
  return request({ url: '/bbs/admin/muted-users', method: 'get' })
}
