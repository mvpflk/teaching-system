import request from '@/utils/request'

export function getNodeTreeVersion() {
  return request({ url: '/knowledge-node/version', method: 'get' })
}

export function getNodeTree() {
  return request({ url: '/knowledge-node/tree', method: 'get' })
}

export function getNodeList(params) {
  return request({ url: '/knowledge-node/list', method: 'get', params })
}

export function createNode(data) {
  return request({ url: '/knowledge-node', method: 'post', data })
}

export function updateNode(id, data) {
  return request({ url: `/knowledge-node/${id}`, method: 'put', data })
}

export function deleteNode(id) {
  return request({ url: `/knowledge-node/${id}`, method: 'delete' })
}

export function getNodeContent(nodeId, includeChildren = false) {
  return request({
    url: `/knowledge-node/${nodeId}/content`,
    method: 'get',
    params: includeChildren ? { includeChildren: true } : {}
  })
}

export function updateNodeContent(nodeId, content) {
  return request({ url: `/knowledge-node/${nodeId}/content`, method: 'put', data: { content } })
}

export function importNodes(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: '/knowledge-node/actions/import',
    method: 'post',
    data: fd,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importKnowledgeZip(subjectId, formData) {
  return request({
    url: '/knowledge-node/actions/import-zip',
    method: 'post',
    params: { subjectId },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importKnowledgeTxt(subjectId, formData) {
  return request({
    url: '/knowledge-node/actions/import-txt',
    method: 'post',
    params: { subjectId },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importKnowledgeDocx(subjectId, formData) {
  return request({
    url: '/knowledge-node/actions/import-docx',
    method: 'post',
    params: { subjectId },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function clearKnowledgeBySubject(subjectId) {
  return request({ url: `/knowledge-node/actions/clear-by-subject/${subjectId}`, method: 'delete' })
}

export function composeExam(data) {
  return request({ url: '/question-bank/actions/compose-exam', method: 'post', data })
}

// ── AI 学习资源 ──

/** AI 生成学习资源 */
export function generateResources(nodeId) {
  return request({ url: `/knowledge-node/${nodeId}/generate-resources`, method: 'post' })
}

/** 审核学习资源 */
export function reviewResource(nodeId, status, rejectReason) {
  return request({
    url: `/knowledge-node/${nodeId}/resource-status`,
    method: 'put',
    data: { status, rejectReason }
  })
}

/** 视频链接有效性检查 */
export function checkVideoLinks() {
  return request({ url: '/knowledge-node/actions/check-video-links', method: 'get' })
}

/** 获取节点学习资源（学生端） */
export function getNodeLearningResources(nodeId) {
  return request({ url: `/knowledge-node/${nodeId}/learning-resources`, method: 'get' })
}
