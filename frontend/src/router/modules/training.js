/**
 * 实训中心路由模块
 */
export default [
  // ── 学生端 ──
  {
    path: 'student/training',
    name: 'TrainingHub',
    component: () => import('@/views/training/TrainingHub.vue'),
    meta: { title: '实训中心', roles: ['STUDENT'], featureKey: 'feature.training_center' }
  },
  {
    path: 'student/training/network',
    name: 'NetworkTaskList',
    component: () => import('@/views/training/NetworkTaskList.vue'),
    meta: { title: '网络实训任务', roles: ['STUDENT'], featureKey: 'feature.training_center', hidden: true }
  },
  {
    path: 'student/training/network/practice/:id',
    name: 'NetworkPractice',
    component: () => import('@/views/simulation/Win7Practice.vue'),
    meta: { title: '网络命令实训', roles: ['STUDENT'], featureKey: 'feature.training_center', hidden: true }
  },

  // ── 旧路由重定向（兼容旧版 bookmark/链接）──
  {
    path: 'student/simulation',
    redirect: '/student/training',
    meta: { hidden: true }
  },
  {
    path: 'student/simulation/practice/:id',
    redirect: to => `/student/training/win7/practice/${to.params.id}`,
    meta: { hidden: true }
  },
  {
    path: 'student/simulation/exam/:id',
    redirect: '/student/training',
    meta: { hidden: true }
  },

  // ── 教师/管理端 ──
  {
    path: '/training',
    meta: { title: '实训中心', icon: 'Monitor', featureKey: 'feature.training_center' },
    children: [
      {
        path: '',
        name: 'TrainingHubTeacher',
        component: () => import('@/views/training/TrainingHub.vue'),
        meta: { title: '实训中心', icon: 'Monitor' }
      },
      {
        path: 'tasks',
        name: 'TrainingTaskList',
        component: () => import('@/views/training/TrainingHub.vue'),
        meta: { title: '实训任务' }
      },
      {
        path: 'create',
        name: 'TrainingTaskCreate',
        component: () => import('@/views/training/TaskEditor.vue'),
        meta: { title: '创建实训任务', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] },
        beforeEnter: async (_to, _from, next) => {
          const { useUserStore } = await import('@/stores/user')
          const userStore = useUserStore()
          if (userStore.isAdmin) return next()
          if (!userStore.showPracticePlans) {
            const { ElMessage } = await import('element-plus')
            ElMessage.warning('实训方案仅对职高专业课教师开放')
            return next('/home')
          }
          next()
        },
      },
      {
        path: ':taskId/do',
        name: 'TrainingStepPlayer',
        component: () => import('@/views/training/StepPlayer.vue'),
        meta: { title: '实训操作', roles: ['STUDENT', 'TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] }
      },
      {
        path: ':taskId/grade',
        name: 'TrainingGrading',
        component: () => import('@/views/training/GradingWorkbench.vue'),
        meta: { title: '实训评分', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'], layoutWidth: 'wide' }
      },
      {
        path: 'library',
        name: 'TrainingLibrary',
        component: () => import('@/views/training/TrainingHub.vue'),
        meta: { title: '实训任务库' }
      },
      {
        path: 'simulation',
        name: 'TrainingSimulation',
        component: () => import('@/views/training/TrainingHub.vue'),
        meta: { title: '仿真实训' }
      }
    ]
  }
]
