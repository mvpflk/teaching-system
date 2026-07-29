/**
 * 教师工作台相关路由
 */
export default [
  {
    path: 'teacher/alerts',
    name: 'AlertManagement',
    component: () => import('@/views/teacher/AlertManagement.vue'),
    meta: {
      title: '学业预警',
      icon: 'Bell',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
    },
  },
  {
    path: 'teacher/groups',
    name: 'GroupManagement',
    component: () => import('@/views/teacher/GroupManagement.vue'),
    meta: {
      title: '分组管理',
      icon: 'List',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
    },
  },
  {
    path: 'teacher/typing/monitor',
    name: 'TeacherTypingMonitor',
    component: () => import('@/views/typing/TeacherTypingMonitor.vue'),
    meta: {
      title: '打字竞赛驾驶舱',
      icon: 'Monitor',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
    },
  },
  {
    path: 'teacher/typing/competitions',
    name: 'TypingCompetitionManager',
    component: () => import('@/views/typing/TypingCompetitionManager.vue'),
    meta: { title: '打字竞赛管理', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] },
  },
  {
    path: 'teacher/typing/competitions/:compId/replay/:studentId',
    name: 'CompetitionReplay',
    component: () => import('@/views/typing/CompetitionReplay.vue'),
    meta: { title: '竞赛回放', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] },
  },
  {
    path: 'teacher/wrong-monitor',
    name: 'TeacherWrongMonitor',
    component: () => import('@/views/wrong/TeacherWrongMonitor.vue'),
    meta: { title: '错题监督', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] },
  },
  {
    path: 'teacher/practice-plans',
    redirect: '/training/create',
    meta: { hidden: true },
  },
  {
    path: 'teacher/research',
    name: 'ResearchWorkbench',
    component: () => import('@/views/teacher/ResearchWorkbench.vue'),
    meta: {
      title: '教研工作台',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.research_enabled',
    },
  },
  {
    path: 'teacher/research/baseline',
    name: 'ResearchBaseline',
    component: () => import('@/views/teacher/ResearchBaseline.vue'),
    meta: {
      title: '基线快照',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.research_enabled',
    },
  },
  {
    path: 'teacher/lesson-prep',
    name: 'LessonPrepWorkbench',
    component: () => import('@/views/teacher/LessonPrepWorkbench.vue'),
    meta: { title: '备课工作台', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] },
  },
  {
    path: 'teacher/activity',
    name: 'TeacherActivity',
    component: () => import('@/views/teacher/TeacherActivity.vue'),
    meta: { title: '教师行为日志', roles: ['ADMIN', 'SUPER_ADMIN', 'INSPECTOR'] },
  },
  {
    path: 'teacher/rectification',
    name: 'TeacherRectification',
    component: () => import('@/views/teacher/MyRectification.vue'),
    meta: { title: '我的整改', roles: ['TEACHER', 'HEAD_TEACHER'] },
  },
  {
    path: 'teacher/notices',
    name: 'TeacherNotices',
    component: () => import('@/views/teacher/MyNotices.vue'),
    meta: { title: '我的通知书', roles: ['TEACHER', 'HEAD_TEACHER'] },
  },
  // 教师管理
  {
    path: 'teacher',
    name: 'Teacher',
    redirect: '/teacher/list',
    meta: { title: '教师管理', icon: 'UserFilled', roles: ['SUPER_ADMIN', 'ADMIN'] },
    children: [
      {
        path: 'list',
        name: 'TeacherList',
        component: () => import('@/views/teacher/TeacherList.vue'),
        meta: { title: '教师列表', roles: ['SUPER_ADMIN', 'ADMIN'] },
      },
    ],
  },
  {
    path: 'teacher/precision-monitor',
    name: 'TeacherPrecisionMonitor',
    component: () => import('@/views/precision/TeacherPrecisionMonitor.vue'),
    meta: {
      title: '偏科监督',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.remedial_enabled',
    },
  },
  {
    path: 'teacher/simulation/tasks',
    name: 'Win7TeacherTaskList',
    component: () => import('@/views/teacher/Win7TeacherTaskList.vue'),
    meta: {
      title: '仿真任务管理',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.training_center',
    },
  },
  {
    path: 'teacher/simulation/tasks/create',
    name: 'SimTaskCreate',
    component: () => import('@/views/teacher/SimTaskEditor.vue'),
    meta: {
      title: '创建仿真任务',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.training_center',
    },
  },
  {
    path: 'teacher/simulation/tasks/edit/:id',
    name: 'SimTaskEdit',
    component: () => import('@/views/teacher/SimTaskEditor.vue'),
    meta: {
      title: '编辑仿真任务',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.training_center',
    },
  },
  {
    path: 'teacher/ai/exam-paper',
    name: 'ExamPaperHub',
    component: () => import('@/views/ai-exam/ExamPaperHub.vue'),
    meta: {
      title: 'AI智能组卷',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
    },
  },
  {
    path: 'teacher/ai/exam-paper/create',
    name: 'ExamPaperCreate',
    component: () => import('@/views/ai-exam/ExamPaperCreate.vue'),
    meta: {
      title: '创建组卷',
      hidden: true,
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
      layoutWidth: 'medium',
    },
  },
  {
    path: 'teacher/ai/diagnosis',
    name: 'DiagnosisHub',
    component: () => import('@/views/ai-exam/DiagnosisHub.vue'),
    meta: {
      title: 'AI批改诊断',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
    },
  },
  {
    path: 'teacher/ai/diagnosis/:taskId',
    name: 'DiagnosisReport',
    component: () => import('@/views/ai-exam/DiagnosisReport.vue'),
    meta: {
      title: '诊断报告',
      hidden: true,
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
    },
  },
  {
    path: 'teacher/analytics/usage',
    name: 'UsageAnalytics',
    component: () => import('@/views/system/UsageAnalytics.vue'),
    meta: { title: '功能使用分析', roles: ['ADMIN', 'SUPER_ADMIN'] },
  },
  {
    path: 'teacher/quality/comparison',
    name: 'ComparisonHub',
    component: () => import('@/views/quality/ComparisonHub.vue'),
    meta: {
      title: '质量分析',
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
    },
  },
  {
    path: 'teacher/quality/comparison/:taskId',
    name: 'ComparisonDashboard',
    component: () => import('@/views/quality/ComparisonDashboard.vue'),
    meta: {
      title: '对比分析',
      hidden: true,
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
      layoutWidth: 'wide',
    },
  },
  {
    path: 'teacher/quality/consolidation/:id',
    name: 'ConsolidationPreview',
    component: () => import('@/views/quality/ConsolidationPreview.vue'),
    meta: {
      title: '巩固材料',
      hidden: true,
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
    },
  },
  {
    path: 'teacher/quality/student/:studentId',
    name: 'StudentGrowth',
    component: () => import('@/views/quality/StudentGrowth.vue'),
    meta: {
      title: '学生成长',
      hidden: true,
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.ai_content_enabled',
    },
  },
];
