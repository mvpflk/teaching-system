/**
 * 系统管理相关路由
 */
export default [
  // 个人中心
  {
    path: 'profile',
    name: 'Profile',
    component: () => import('@/views/profile/Profile.vue'),
    meta: { title: '个人中心', icon: 'User' },
  },
  // 我的 API Key（全员可访问，放在 system-management 外部防止父路由 SUPER_ADMIN 拦截）
  {
    path: 'my/api-keys',
    name: 'UserApiKeys',
    component: () => import('@/views/settings/ApiKeys.vue'),
    meta: {
      title: '我的 API Key',
      roles: ['STUDENT', 'TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
    },
  },
  // 系统管理中心（超级管理员专属）
  {
    path: 'system-management',
    name: 'SystemManagement',
    redirect: '/system-management/overview',
    component: () => import('@/views/system/SystemManagementCenter.vue'),
    meta: { title: '系统管理中心', icon: 'Tools', roles: ['SUPER_ADMIN'] },
    children: [
      {
        path: 'overview',
        name: 'DashboardOverview',
        component: () => import('@/views/system/DashboardOverview.vue'),
        meta: { title: '总览看板', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'basic',
        name: 'BasicSettings',
        component: () => import('@/views/system/BasicSettings.vue'),
        meta: { title: '基础设置', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'dynamic-params',
        name: 'DynamicParams',
        component: () => import('@/views/system/DynamicParams.vue'),
        meta: { title: '动态参数', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'credit-rules',
        name: 'CreditRules',
        component: () => import('@/views/system/CreditRules.vue'),
        meta: { title: '积分规则', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'terms',
        name: 'TermManager',
        component: () => import('@/views/system/TermManager.vue'),
        meta: { title: '学期管理', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'audit-logs',
        name: 'AuditLogs',
        component: () => import('@/views/system/AuditLogs.vue'),
        meta: { title: '审计日志', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'feedback',
        name: 'FeedbackManagement',
        component: () => import('@/views/system/FeedbackManagement.vue'),
        meta: { title: '用户反馈', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'maintenance',
        name: 'SystemMaintenance',
        component: () => import('@/views/system/SystemMaintenance.vue'),
        meta: { title: '系统运维', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'remedial-class-config',
        name: 'RemedialClassConfig',
        component: () => import('@/views/system/RemedialClassConfig.vue'),
        meta: { title: '偏科提分配置', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'graduation',
        name: 'Graduation',
        component: () => import('@/views/system/Graduation.vue'),
        meta: { title: '毕业管理', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'class-type-config',
        name: 'ClassTypeConfig',
        component: () => import('@/views/system/ClassTypeConfig.vue'),
        meta: { title: '班级类型配置', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'teaching-groups',
        name: 'TeachingGroupManager',
        component: () => import('@/views/system/TeachingGroupManager.vue'),
        meta: { title: '教研备课管理', roles: ['SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'ai-config',
        name: 'AiConfig',
        component: () => import('@/views/settings/AiConfig.vue'),
        meta: { title: 'AI 配置', roles: ['SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER', 'TEACHER'] },
      },
      {
        path: 'category-manager',
        name: 'CategoryManager',
        component: () => import('@/views/settings/SettingsCategoryManager.vue'),
        meta: { title: '知识点分类管理', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'knowledge-aging',
        name: 'KnowledgeAging',
        component: () => import('@/views/system/KnowledgeAging.vue'),
        meta: { title: '知识时效性管理', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'exam-syllabus',
        name: 'ExamSyllabusManager',
        component: () => import('@/views/system/ExamSyllabusManager.vue'),
        meta: { title: '考纲管理', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'typing-texts',
        name: 'TypingTextManager',
        component: () => import('@/views/typing/TypingTextManager.vue'),
        meta: { title: '打字文本管理', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'typing-settings',
        name: 'TypingSettings',
        component: () => import('@/views/typing/TypingSettings.vue'),
        meta: { title: '打字功能配置', roles: ['SUPER_ADMIN'] },
      },
      {
        path: 'monitor',
        name: 'MonitorDashboard',
        component: () => import('@/views/system/MonitorDashboard.vue'),
        meta: { title: '系统监控', roles: ['SUPER_ADMIN'], layoutWidth: 'wide' },
      },
    ],
  },
];
