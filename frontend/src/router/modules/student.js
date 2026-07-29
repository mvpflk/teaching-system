/**
 * 学生端相关路由
 */
export default [
  {
    path: 'typing',
    name: 'StudentTyping',
    component: () => import('@/views/typing/StudentTyping.vue'),
    meta: { title: '打字练习', icon: 'Keyboard', roles: ['STUDENT'], layoutWidth: 'medium' }
  },
  {
    path: 'student/typing-history',
    name: 'StudentTypingHistory',
    component: () => import('@/views/typing/StudentTypingHistory.vue'),
    meta: { title: '打字历史', roles: ['STUDENT'] },
  },
  {
    path: 'student/typing/texts',
    name: 'StudentTextLibrary',
    component: () => import('@/views/typing/StudentTextLibrary.vue'),
    meta: { title: '练习素材库', roles: ['STUDENT'] }
  },
  {
    path: 'student/derived-practice',
    name: 'StudentDerivedPractice',
    component: () => import('@/views/wrong/StudentPractice.vue'),
    meta: { title: '衍生练习', roles: ['STUDENT'], layoutWidth: 'narrow' }
  },
  // 积分系统
  {
    path: 'credit',
    name: 'Credit',
    redirect: '/credit/index',
    meta: { title: '积分中心', icon: 'Coin', featureKey: 'feature.credit_enabled' },
    children: [
      {
        path: 'index',
        name: 'CreditIndex',
        component: () => import('@/views/credit/CreditIndex.vue'),
        meta: { title: '我的积分' }
      },
      {
        path: 'shop',
        name: 'CreditShop',
        component: () => import('@/views/credit/CreditShop.vue'),
        meta: { title: '积分商城', featureKey: 'feature.shop_enabled' }
      },
      {
        path: 'ranking',
        name: 'CreditRanking',
        component: () => import('@/views/credit/CreditRanking.vue'),
        meta: { title: '积分排行' }
      },
      {
        path: 'admin',
        name: 'CreditAdmin',
        component: () => import('@/views/credit/AdminCredit.vue'),
        meta: { title: '积分管理', roles: ['SUPER_ADMIN', 'ADMIN'] }
      },
      {
        path: 'moral-rank',
        name: 'MoralStarRanking',
        component: () => import('@/views/credit/MoralStarRanking.vue'),
        meta: { title: '德育行为积分榜' }
      }
    ]
  },
  // BBS论坛
  {
    path: 'bbs',
    name: 'Bbs',
    redirect: '/bbs/category/1',
    meta: { title: '师生论坛', icon: 'ChatDotSquare', featureKey: 'feature.bbs_enabled' },
    children: [
      {
        path: 'category/:id?',
        name: 'BbsHome',
        component: () => import('@/views/bbs/BbsHome.vue'),
        meta: { title: '师生论坛' }
      },
      {
        path: 'post/:id',
        name: 'BbsPostDetail',
        component: () => import('@/views/bbs/BbsPostDetail.vue'),
        meta: { title: '帖子详情' }
      },
      {
        path: 'create',
        name: 'BbsCreatePost',
        component: () => import('@/views/bbs/BbsCreatePost.vue'),
        meta: { title: '发布帖子' }
      },
      {
        path: 'edit/:id',
        name: 'BbsEditPost',
        component: () => import('@/views/bbs/BbsCreatePost.vue'),
        meta: { title: '编辑帖子' }
      }
    ]
  },
  // 优秀作品展示墙
  {
    path: 'showcase',
    name: 'Showcase',
    component: () => import('@/views/showcase/Showcase.vue'),
    meta: { title: '作品展示墙', icon: 'Medal' }
  },
  // 个人成长
  {
    path: 'student/timeline',
    name: 'StudentTimeline',
    component: () => import('@/views/student/Timeline.vue'),
    meta: { title: '成长足迹', roles: ['STUDENT'] }
  },
  {
    path: 'student/learning-profile',
    name: 'StudentLearningProfile',
    component: () => import('@/views/student/StudentLearningProfile.vue'),
    meta: { title: '学习画像', roles: ['STUDENT'] }
  },
  {
    path: 'student/report',
    name: 'GrowthReport',
    component: () => import('@/views/student/GrowthReport.vue'),
    meta: { title: '成长报告', roles: ['STUDENT'] }
  },
  {
    path: 'student/growth',
    name: 'StudentGrowthHub',
    component: () => import('@/views/student/StudentGrowth.vue'),
    meta: { title: '我的成长', roles: ['STUDENT'] }
  },
  {
    path: 'student/precision',
    name: 'PrecisionHub',
    component: () => import('@/views/precision/PrecisionHub.vue'),
    meta: { title: '偏科提分', roles: ['STUDENT'], featureKey: 'feature.remedial_enabled' }
  },
  {
    path: 'student/precision/practice',
    name: 'PrecisionPractice',
    component: () => import('@/views/precision/PrecisionPractice.vue'),
    meta: { title: '提分练习', roles: ['STUDENT'], featureKey: 'feature.remedial_enabled' }
  },
  {
    path: 'student/precision/report',
    name: 'PrecisionReport',
    component: () => import('@/views/precision/PrecisionReport.vue'),
    meta: { title: '进步报告', roles: ['STUDENT'], featureKey: 'feature.remedial_enabled' }
  },
  {
    path: 'student/precision/diagnose-result',
    name: 'DiagnoseResult',
    component: () => import('@/views/precision/DiagnoseResult.vue'),
    meta: { title: '诊断报告', roles: ['STUDENT'], featureKey: 'feature.remedial_enabled', hidden: true }
  },
  {
    path: 'precision/english',
    name: 'PrecisionEnglish',
    component: () => import('@/views/precision/PrecisionEnglish.vue'),
    meta: { title: '英语学习', featureKey: 'feature.english_remedial', layoutWidth: 'narrow' }
  },
  {
    path: 'precision/english/drill',
    name: 'PrecisionEnglishDrill',
    component: () => import('@/views/precision/PrecisionEnglishDrill.vue'),
    meta: { title: '英语练习', featureKey: 'feature.english_remedial' }
  },
  {
    path: 'student/training/win7',
    name: 'Win7TaskList',
    component: () => import('@/views/simulation/Win7TaskList.vue'),
    meta: { title: 'Windows 操作实训', roles: ['STUDENT'], featureKey: 'feature.training_center', hidden: true }
  },
  {
    path: 'student/training/win7/practice/:id',
    name: 'Win7Practice',
    component: () => import('@/views/simulation/Win7Practice.vue'),
    meta: { title: 'Windows 仿真练习', roles: ['STUDENT'], featureKey: 'feature.training_center', hidden: true }
  },
  {
    path: 'student/training/win7/exam/:id',
    name: 'Win7Exam',
    component: () => import('@/views/simulation/Win7Exam.vue'),
    meta: { title: 'Windows 仿真考试', roles: ['STUDENT'], featureKey: 'feature.training_center', hidden: true }
  },
]
