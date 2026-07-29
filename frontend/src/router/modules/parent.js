/**
 * 家长端相关路由
 */
export default [
  {
    path: 'parent/children',
    name: 'ParentHome',
    component: () => import('@/views/parent/ParentHome.vue'),
    meta: { title: '我的孩子', icon: 'UserFilled', roles: ['PARENT', 'ADMIN', 'SUPER_ADMIN'], featureKey: 'feature.parent_enabled' }
  },
  {
    path: 'parent/children/:studentId/grades',
    name: 'ChildGrades',
    component: () => import('@/views/parent/ChildGrades.vue'),
    meta: { title: '孩子成绩', icon: 'Document', roles: ['PARENT', 'ADMIN', 'SUPER_ADMIN'], featureKey: 'feature.parent_enabled' }
  },
  {
    path: 'parent/children/:studentId/timeline',
    name: 'ChildTimeline',
    component: () => import('@/views/parent/ChildTimeline.vue'),
    meta: { title: '成长足迹', icon: 'Sunrise', roles: ['PARENT', 'ADMIN', 'SUPER_ADMIN'], featureKey: 'feature.parent_enabled' }
  },
  {
    path: 'parent/children/:studentId/homework',
    name: 'ChildHomework',
    component: () => import('@/views/parent/ChildHomework.vue'),
    meta: { title: '作业列表', icon: 'Edit', roles: ['PARENT', 'ADMIN', 'SUPER_ADMIN'], featureKey: 'feature.parent_enabled' }
  },
  {
    path: 'parent/children/:studentId/practice',
    name: 'ChildPractice',
    component: () => import('@/views/parent/ChildPractice.vue'),
    meta: { title: '实训记录', roles: ['PARENT', 'ADMIN', 'SUPER_ADMIN'], featureKey: 'feature.parent_enabled' }
  },
  {
    path: 'parent/feedback/:formId',
    name: 'ParentFeedbackForm',
    component: () => import('@/views/parent/ParentFeedbackForm.vue'),
    meta: { title: '反馈问卷', roles: ['PARENT', 'ADMIN', 'SUPER_ADMIN'], featureKey: 'feature.parent_enabled' }
  },
]
