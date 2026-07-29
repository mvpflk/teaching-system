import request from '@/utils/request'

export function listSyllabus(params) {
  return request({ url: '/settings/exam-syllabus/list', method: 'get', params })
}

export function getSyllabusById(id) {
  return request({ url: `/settings/exam-syllabus/${id}`, method: 'get' })
}

export function getSyllabusBySubject(subjectId, examType = 'GENERAL') {
  return request({ url: `/settings/exam-syllabus/by-subject/${subjectId}`, method: 'get', params: { examType } })
}

export function createSyllabus(data) {
  return request({ url: '/settings/exam-syllabus', method: 'post', data })
}

export function updateSyllabus(id, data) {
  return request({ url: `/settings/exam-syllabus/${id}`, method: 'put', data })
}

export function deleteSyllabus(id) {
  return request({ url: `/settings/exam-syllabus/${id}`, method: 'delete' })
}

export function toggleSyllabusStatus(id) {
  return request({ url: `/settings/exam-syllabus/${id}/toggle`, method: 'put' })
}

export function getSyllabusNodeIds(id) {
  return request({ url: `/settings/exam-syllabus/${id}/nodes`, method: 'get' })
}

export function saveSyllabusNodeRelations(id, nodeIds) {
  return request({ url: `/settings/exam-syllabus/${id}/nodes`, method: 'put', data: nodeIds })
}

export function uploadSyllabusFile(subjectId, examType, file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: '/settings/exam-syllabus/upload',
    method: 'post',
    params: { subjectId, examType },
    data: fd,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
