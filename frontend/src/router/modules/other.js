/**
 * 通用路由（首页、通知、消息等）
 */
export default [
  {
    path: 'home',
    name: 'Home',
    component: () => import('@/views/home/Home.vue'),
    meta: { title: '首页', icon: 'House' }
  },
  // 通知
  {
    path: 'notification',
    name: 'Notification',
    component: () => import('@/views/notification/Notification.vue'),
    meta: { title: '消息通知', icon: 'Bell' }
  },
  {
    path: 'messages',
    name: 'MessageCenter',
    component: () => import('@/views/common/MessageCenter.vue'),
    meta: { title: '家校消息', icon: 'ChatDotSquare', featureKey: 'feature.message_enabled' }
  },
]
