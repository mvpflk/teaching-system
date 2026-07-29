import dayjs from 'dayjs'

/** 判断提交是否迟交 */
export function isLateSubmission(deadline) {
  if (!deadline) return false
  return dayjs().isAfter(dayjs(deadline))
}

/** 格式化截止时间 */
export function formatDeadline(deadline) {
  if (!deadline) return '无截止'
  return dayjs(deadline).format('YYYY-MM-DD HH:mm')
}

/** 截止紧急程度：expired/critical/urgent/normal/none */
export function getDeadlineUrgency(deadline) {
  if (!deadline) return 'none'
  const hours = dayjs(deadline).diff(dayjs(), 'hour')
  if (hours < 0) return 'expired'
  if (hours < 1) return 'critical'
  if (hours < 24) return 'urgent'
  return 'normal'
}

/** 迟交扣分（默认八折） */
export function calcPenaltyScore(score, deadline) {
  if (!deadline || !isLateSubmission(deadline)) return score
  return Math.round(score * 0.8)
}

/** 是否及格 */
export function isSubmissionPassing(submission, passingScore) {
  if (!submission?.score) return false
  if (!passingScore) return Number(submission.score) > 0
  return Number(submission.score) >= Number(passingScore)
}

/** 获取星期几 */
export function getWeekDay(date) {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[dayjs(date).day()]
}

/** 百分比（提交率） */
export function calcSubmitPercent(submitted, total) {
  if (!total || total === 0) return 0
  return Math.round((submitted / total) * 100)
}
