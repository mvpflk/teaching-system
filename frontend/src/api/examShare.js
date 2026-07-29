import request from '@/utils/request'

export function createShare(taskId) {
  return request({ url: '/exam-share/actions/create', method: 'post', params: { taskId } })
}

export function getMyShares() {
  return request({ url: '/exam-share/actions/my-shares', method: 'get' })
}

export function deleteShare(shareId) {
  return request({ url: `/exam-share/${shareId}`, method: 'delete' })
}

export function importShared(shareCode, targetClassId) {
  return request({ url: '/exam-share/actions/import', method: 'post', data: { shareCode, targetClassId } })
}

export function getLibrary() {
  return request({ url: '/exam-share/actions/library', method: 'get' })
}

export function previewShare(shareCode) {
  return request({ url: '/exam-share/actions/preview', method: 'get', params: { shareCode } })
}
