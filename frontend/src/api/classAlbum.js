import request from '@/utils/request'

export function uploadPhoto(file, classId, caption) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('classId', classId)
  if (caption) fd.append('caption', caption)
  return request({ url: '/class-album/upload', method: 'post', data: fd })
}

export function getAlbumPhotos(classId, page = 1, pageSize = 12) {
  return request({ url: `/class-album/class/${classId}`, method: 'get', params: { page, pageSize } })
}

export function likePhoto(id) {
  return request({ url: `/class-album/photo/${id}/like`, method: 'post' })
}

export function commentPhoto(id, content) {
  return request({ url: `/class-album/photo/${id}/comment`, method: 'post', data: { content } })
}

export function getComments(photoId) {
  return request({ url: `/class-album/photo/${photoId}/comments`, method: 'get' })
}

export function reviewPhoto(id, action) {
  return request({ url: `/class-album/photo/${id}/review`, method: 'post', data: { action } })
}

export function getPendingPhotos() {
  return request({ url: '/class-album/pending', method: 'get' })
}

export function deletePhoto(id) {
  return request({ url: `/class-album/photo/${id}`, method: 'delete' })
}
