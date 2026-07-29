/**
 * 任务管理相关路由
 */
export const taskParentChildren = [
  { path: '', redirect: '/teacher/tasks/list' },
  { path: 'list', name: 'TaskList', component: () => import('@/views/task/TaskList.vue'), meta: { title: '任务管理' } },
  { path: 'create', name: 'TaskCreate', component: () => import('@/views/task/TaskCreatePage.vue'), meta: { title: '创建任务', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] } },
  { path: ':id/edit', name: 'TaskEdit', component: () => import('@/views/task/TaskCreatePage.vue'), meta: { title: '编辑任务', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] } },
  { path: 'question-bank', name: 'TaskQuestionBank', component: () => import('@/views/question-bank/QuestionBank.vue'), meta: { title: '题库管理' } },
  { path: 'library', name: 'TaskExamLibrary', component: () => import('@/views/task/ExamLibrary.vue'), meta: { title: '共享试卷' } },
  { path: 'share', name: 'TaskExamShare', component: () => import('@/views/task/ExamShare.vue'), meta: { title: '我的分享' } },
  { path: 'templates', name: 'TaskTemplates', component: () => import('@/views/task/TaskTemplateManager.vue'), meta: { title: '任务模板', featureKey: 'feature.template_enabled' } },
  { path: 'pending-review', name: 'PendingReview', component: () => import('@/views/task/PendingReview.vue'), meta: { title: '待审核', featureKey: 'feature.review_enabled' } },
  { path: 'paper-import', name: 'PaperImport', component: () => import('@/views/task/PaperImport.vue'), meta: { title: '导入试卷', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] } },
  { path: 'paper-library', name: 'PaperLibrary', component: () => import('@/views/task/PaperLibrary.vue'), meta: { title: '试卷库', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] } },
]

export default [
  // 旧作业路由 → 重定向到任务中心
  {
    path: 'homework',
    redirect: '/teacher/tasks',
    meta: { title: '作业管理' },
    children: [
      { path: 'list', redirect: '/teacher/tasks' },
      { path: 'submit/:id', redirect: '/teacher/tasks' },
      { path: 'grade/:id', redirect: '/teacher/tasks' }
    ]
  },
  // 旧考试路由 → 重定向到任务中心
  {
    path: 'exam',
    redirect: '/teacher/tasks',
    meta: { title: '考试管理' },
    children: [
      { path: 'list', redirect: '/teacher/tasks' },
      { path: 'analyses', redirect: '/teacher/tasks' },
      { path: 'results', redirect: '/teacher/tasks' },
      { path: 'do/:id', redirect: '/teacher/tasks' },
      { path: 'result/:id', redirect: '/teacher/tasks' },
      { path: 'analysis/:id', redirect: '/teacher/tasks' }
    ]
  },
  {
    path: 'teacher/tasks/:id/grade',
    name: 'TaskGrading',
    component: () => import('@/views/task/TaskGrading.vue'),
    meta: { title: '批改任务', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'teacher/tasks/:taskId/manual-entry',
    name: 'ManualEntry',
    component: () => import('@/views/task/ManualEntry.vue'),
    meta: { title: '纸质答题卡录入', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'teacher/grading/:id',
    name: 'TeacherGradingWorkbench',
    component: () => import('@/views/task/TeacherGradingWorkbench.vue'),
    meta: { title: '批阅工作台', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'], layoutWidth: 'wide' }
  },
  // 统一任务管理（父路由，AI子路由通过 index.js 注入）
  {
    path: 'teacher/tasks',
    component: () => import('@/views/task/TaskManagement.vue'),
    meta: { title: '任务管理', icon: 'List', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] },
    children: taskParentChildren
  },
  {
    path: 'student/tasks',
    name: 'StudentTasks',
    component: () => import('@/views/task/StudentTasks.vue'),
    meta: { title: '我的任务', roles: ['STUDENT'] }
  },
  {
    path: 'student/tasks/:id',
    name: 'StudentTaskDetail',
    component: () => import('@/views/task/StudentTaskDetail.vue'),
    meta: { title: '任务详情', roles: ['STUDENT'] }
  },
  {
    path: 'student/peer-reviews',
    name: 'PeerReviewList',
    component: () => import('@/views/task/PeerReviewList.vue'),
    meta: { title: '互评任务', roles: ['STUDENT'], featureKey: 'feature.re_review_enabled' }
  },
  {
    path: 'wrong-book',
    name: 'WrongBook',
    component: () => import('@/views/task/WrongBook.vue'),
    meta: { title: '错题本', icon: 'Notebook', roles: ['STUDENT'], layoutWidth: 'medium' }
  },
  {
    path: 'paper-import',
    name: 'PaperImportDirect',
    component: () => import('@/views/task/PaperImport.vue'),
    meta: { title: '导入试卷', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: 'paper-library',
    name: 'PaperLibraryDirect',
    component: () => import('@/views/task/PaperLibrary.vue'),
    meta: { title: '试卷库', roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'] }
  },
]
