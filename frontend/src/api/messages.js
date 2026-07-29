import request from '@/utils/request'

export function getConversations() {
  return request({ url: '/messages/conversations', method: 'get' })
}

export function getConversationMessages(otherUserId) {
  return request({ url: `/messages/conversations/${otherUserId}`, method: 'get' })
}

export function sendMessage(data) {
  return request({ url: '/messages/send', method: 'post', data })
}

export function getUnreadMessageCount() {
  return request({ url: '/messages/unread-count', method: 'get' })
}
