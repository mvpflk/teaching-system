import request from '@/utils/request'

/**
 * 用户行为事件埋点（fire-and-forget，不阻塞主逻辑）
 * @param {'EXAM_PAPER_GENERATE'|'EXAM_PAPER_PUBLISH'|'EXAM_PAPER_EDIT'|'DIAGNOSIS_START'|'DIAGNOSIS_VIEW_LAYER'|'DIAGNOSIS_SATISFACTION'} eventType
 * @param {Object} data - flexible metadata
 */
export function trackEvent(eventType, data = {}) {
  request.post('/user-events/track', { eventType, eventData: data }).catch(() => {})
}
