/**
 * 教务巡视相关路由
 */
export default [
  // 教务巡视（仅 INSPECTOR/ADMIN/SUPER_ADMIN）
  {
    path: 'inspector',
    name: 'Inspector',
    redirect: '/inspector/dashboard',
    meta: {
      title: '巡视监督',
      icon: 'Monitor',
      roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'],
      hidden: true,
      featureKey: 'feature.inspector_enabled',
    },
    children: [
      {
        path: 'dashboard',
        name: 'InspectorDashboard',
        component: () => import('@/views/inspector/InspectorDashboard.vue'),
        meta: { title: '监督面板', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'exams',
        name: 'InspectorTaskAnalysis',
        component: () => import('@/views/inspector/InspectorTaskAnalysis.vue'),
        meta: { title: '任务分析', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'teachers',
        name: 'InspectorTeachers',
        component: () => import('@/views/inspector/InspectorTeachers.vue'),
        meta: { title: '教师活跃度', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'classes',
        name: 'InspectorClasses',
        component: () => import('@/views/inspector/InspectorClasses.vue'),
        meta: { title: '班级对比', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'records',
        name: 'InspectorRecords',
        component: () => import('@/views/inspector/InspectorRecords.vue'),
        meta: { title: '巡视记录', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'records/create',
        name: 'InspectorRecordCreate',
        component: () => import('@/views/inspector/InspectorRecordForm.vue'),
        meta: {
          title: '新建巡视记录',
          roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'],
          hidden: true,
        },
      },
      {
        path: 'records/:id/edit',
        name: 'InspectorRecordEdit',
        component: () => import('@/views/inspector/InspectorRecordForm.vue'),
        meta: {
          title: '编辑巡视记录',
          roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'],
          hidden: true,
        },
      },
      {
        path: 'issues',
        name: 'InspectorIssueLedger',
        component: () => import('@/views/inspector/InspectorIssueLedger.vue'),
        meta: { title: '问题台账', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'issues/create',
        name: 'InspectorIssueCreate',
        component: () => import('@/views/inspector/InspectorIssueCreate.vue'),
        meta: {
          title: '新建问题',
          roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'],
          hidden: true,
        },
      },
      {
        path: 'issues/:id',
        name: 'InspectorIssueDetail',
        component: () => import('@/views/inspector/InspectorIssueDetail.vue'),
        meta: {
          title: '问题详情',
          roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'],
          hidden: true,
        },
      },
      {
        path: 'notices',
        name: 'InspectorNotices',
        component: () => import('@/views/inspector/InspectorNotices.vue'),
        meta: { title: '整改通知', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'reports',
        name: 'InspectorReports',
        component: () => import('@/views/inspector/InspectorReports.vue'),
        meta: { title: '巡视报告', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'alerts',
        name: 'InspectorAlerts',
        component: () => import('@/views/inspector/InspectorAlerts.vue'),
        meta: { title: '预警中心', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'teachers/:id',
        name: 'InspectorTeacherProfile',
        component: () => import('@/views/inspector/InspectorTeacherProfile.vue'),
        meta: {
          title: '教师档案',
          roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'],
          hidden: true,
        },
      },
      {
        path: 'classes/:id',
        name: 'InspectorClassProfile',
        component: () => import('@/views/inspector/InspectorClassProfile.vue'),
        meta: {
          title: '班级档案',
          roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'],
          hidden: true,
        },
      },
      {
        path: 'classroom-patrols',
        name: 'InspectorClassroomPatrols',
        component: () => import('@/views/inspector/InspectorClassroomPatrols.vue'),
        meta: { title: '课堂巡课', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'moral-inspections',
        name: 'InspectorMoralInspections',
        component: () => import('@/views/inspector/InspectorMoralInspections.vue'),
        meta: { title: '德育巡视', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'review-monitor',
        name: 'InspectorReviewMonitor',
        component: () => import('@/views/inspector/InspectorReviewMonitor.vue'),
        meta: { title: '审核监控', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'research-activities',
        name: 'InspectorTeachingResearch',
        component: () => import('@/views/inspector/InspectorTeachingResearch.vue'),
        meta: { title: '教研活动', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'parent-feedback',
        name: 'InspectorParentFeedback',
        component: () => import('@/views/inspector/InspectorParentFeedback.vue'),
        meta: { title: '家长反馈', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'ai-assistant',
        name: 'InspectorAiAssistant',
        component: () => import('@/views/inspector/InspectorAiAssistant.vue'),
        meta: { title: 'AI巡视助手', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'practice-monitor',
        name: 'PracticeMonitor',
        component: () => import('@/views/inspector/PracticeMonitor.vue'),
        meta: { title: '实训监控', roles: ['INSPECTOR', 'REGION_ADMIN', 'SUPER_ADMIN', 'ADMIN'] },
      },
    ],
  },
];
