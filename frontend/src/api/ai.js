import request from '@/utils/request'

/** 提交AI评分建议任务（异步，返回 taskId） */
export function submitAiGrading(submissionId, questionId) {
  return request({ url: '/ai/grading/suggestion', method: 'get', params: { submissionId, questionId } })
}

/** 轮询 AI 任务结果 */
export function getAiTaskResult(taskId) {
  return request({ url: `/ai/result/${taskId}`, method: 'get' })
}

/** 获取AI生成的草稿列表 */
export function getAiDrafts() {
  return request({ url: '/ai/questions/drafts', method: 'get' })
}

/** 获取AI使用统计（管理员） */
export function getAiStats() {
  return request({ url: '/ai/admin/stats', method: 'get' })
}

/** 薄弱知识点针对性练习 */
export function generateRemedial(data) {
  return request({ url: '/ai/questions/remedial', method: 'post', data })
}

/** 一键示例卷 — 提交生成 */
export function submitQuickExam(subject) {
  return request({ url: '/ai/quick-exam', method: 'post', data: { subject } })
}

/** 一键示例卷 — 获取教师任教学科 */
export function getQuickExamSubjects() {
  return request({ url: '/ai/quick-exam/subjects', method: 'get' })
}

// ═══════════ 专业大类综合卷 ═══════════

/** 获取专业大类列表 */
export function getMajors() {
  return request({ url: '/ai/major-exam/majors', method: 'get' })
}

/** 获取大类下的专业课学科 */
export function getMajorSubjects(majorId) {
  return request({ url: '/ai/major-exam/subjects', method: 'get', params: { majorId } })
}

/** 提交专业大类综合卷生成 */
export function submitMajorExam(majorId, typeCounts) {
  return request({ url: '/ai/major-exam', method: 'post', data: { majorId, typeCounts } })
}

// ===== 轮询工具 =====

/**
 * 提交 AI 任务并轮询等待结果
 * @param {Function} submitFn — 提交函数，返回 { taskId }
 * @param {Object} options — { interval: 轮询间隔ms, timeout: 超时ms, getResult: 自定义结果查询函数 }
 * @returns Object — { status, result, error }
 */
export async function pollAiTask(submitFn, options = {}) {
  const { interval = 2000, timeout = 30_000, getResult } = options
  const resultFn = getResult || getAiTaskResult

  // 1. 提交任务
  const submitRes = await submitFn()
  if (submitRes.code !== 200) throw new Error(submitRes.message || '提交失败')
  const taskId = submitRes.data?.taskId
  if (!taskId) throw new Error('未获取到任务ID')

  // 2. 轮询等待
  const start = Date.now()
  while (Date.now() - start < timeout) {
    await sleep(interval)
    const res = await resultFn(taskId)
    if (res.code !== 200) continue
    const { status, result, error } = res.data
    if (status === 'COMPLETED') return { status, result }
    if (status === 'FAILED') throw new Error(error || 'AI处理失败')
    if (status === 'TIMEOUT') throw new Error(error || 'AI处理超时，请重试')
    // PENDING — 继续轮询
  }
  throw new Error('AI处理超时，请重试')
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}
