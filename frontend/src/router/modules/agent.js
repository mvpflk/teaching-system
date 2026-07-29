/**
 * AI 对话助手相关路由
 */

export default [
  {
    path: 'teacher/agent/chat',
    name: 'AgentChat',
    component: () => import('@/views/agent/AgentPage.vue'),
    meta: { title: 'AI 对话助手', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN', 'INSPECTOR'] }
  },
]
