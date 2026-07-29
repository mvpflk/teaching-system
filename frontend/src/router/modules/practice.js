/**
 * 实训路由 — 已统一重定向到实训中心 /training
 * Phase 1: 路由统一，旧URL保留兼容（bookmark/外部链接自动跳转）
 */
export default [
  {
    path: 'practice-student/:taskId',
    redirect: to => `/training/${to.params.taskId}/do`,
    meta: { hidden: true }
  },
  {
    path: 'teacher/practice-grading/:taskId',
    redirect: to => `/training/${to.params.taskId}/grade`,
    meta: { hidden: true }
  },
]
