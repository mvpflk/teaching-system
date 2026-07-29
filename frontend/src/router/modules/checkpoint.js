/**
 * 闯关学习系统路由
 */
export default [
  {
    path: 'student/checkpoint',
    name: 'CheckpointHub',
    component: () => import('@/views/checkpoint/SubjectPicker.vue'),
    meta: { title: '闯关学习', icon: 'Flag', roles: ['STUDENT'], featureKey: 'feature.checkpoint_enabled' }
  },
  {
    path: 'student/checkpoint/:subjectId',
    name: 'CheckpointOverview',
    component: () => import('@/views/checkpoint/CheckpointOverview.vue'),
    meta: { title: '关卡总览', roles: ['STUDENT'], featureKey: 'feature.checkpoint_enabled', hidden: true }
  },
  {
    path: 'student/checkpoint/:subjectId/:configId',
    name: 'CheckpointPlay',
    component: () => import('@/views/checkpoint/CheckpointPlay.vue'),
    meta: { title: '闯关', roles: ['STUDENT'], featureKey: 'feature.checkpoint_enabled', hidden: true, layoutWidth: 'medium' }
  },
  {
    path: 'student/checkpoint/:subjectId/boss/:configId',
    name: 'CheckpointBoss',
    component: () => import('@/views/checkpoint/CheckpointBoss.vue'),
    meta: { title: 'Boss战', roles: ['STUDENT'], featureKey: 'feature.checkpoint_enabled', hidden: true }
  },
  {
    path: 'student/checkpoint/:subjectId/mixed/:configId',
    name: 'CheckpointMixed',
    component: () => import('@/views/checkpoint/CheckpointMixed.vue'),
    meta: { title: '混合战', roles: ['STUDENT'], featureKey: 'feature.checkpoint_enabled', hidden: true }
  },
  {
    path: 'student/checkpoint/memory-cards/:subjectId',
    name: 'MemoryCardDashboard',
    component: () => import('@/views/checkpoint/MemoryCardDashboard.vue'),
    meta: { title: '我的记忆卡', roles: ['STUDENT'], featureKey: 'feature.checkpoint_enabled' }
  }
]
