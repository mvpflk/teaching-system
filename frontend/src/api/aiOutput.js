import request from '@/utils/request'

export function submitAiOutput(params) {
  return request({ url: '/ai-output/generate', method: 'post', data: params })
}

export function getAiOutputResult(taskId) {
  return request({ url: `/ai-output/result/${taskId}`, method: 'get' })
}

export function listAiOutputs(params) {
  return request({ url: '/ai-output/list', method: 'get', params: {
    outputType: params.contentType,
    keyword: params.keyword,
    page: params.page || 1,
    pageSize: params.pageSize || 20
  } })
}

export function getAiOutput(id) {
  return request({ url: `/ai-output/${id}`, method: 'get' })
}

export function updateAiOutput(id, data) {
  return request({ url: `/ai-output/${id}`, method: 'put', data })
}

export function publishAiOutput(id) {
  return request({ url: `/ai-output/${id}/publish`, method: 'post' })
}

export function archiveAiOutput(id) {
  return request({ url: `/ai-output/${id}/archive`, method: 'post' })
}

export function exportAiOutput(id) {
  return request({ url: `/ai-output/${id}/export`, method: 'get' })
}

export function rateAiOutput(id, rating, feedback) {
  return request({ url: `/ai-output/${id}/rate`, method: 'post', data: { rating, feedback } })
}

export function publishAiOutputAsTask(id, config) {
  return request({ url: `/ai-output/${id}/publish-as-task`, method: 'post', data: config })
}

export function publishQuestionsAsExam(config) {
  return request({ url: '/ai-output/publish-questions-as-exam', method: 'post', data: config })
}
