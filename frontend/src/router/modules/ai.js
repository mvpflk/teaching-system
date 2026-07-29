/**
 * AI 教学助手相关路由
 */

// 任务管理下的 AI 助手子路由（注入到 teacher/tasks 父路由的 children 中，见 index.js）
export const aiTaskChildren = [
  {
    path: 'ai-assistant',
    name: 'AiAssistant',
    component: () => import('@/views/ai-assistant/AiAssistant.vue'),
    meta: { title: 'AI 教学助手', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'], featureKey: 'feature.ai_content_enabled' }
  },
]

export default []
