/**
 * 智慧课堂相关路由
 */
export default [
  // 班级管理（教师）
  {
    path: 'class',
    name: 'Class',
    redirect: '/class/list',
    meta: {
      title: '班级管理',
      icon: 'School',
      roles: ['TEACHER', 'HEAD_TEACHER', 'SUPER_ADMIN', 'ADMIN'],
    },
    children: [
      {
        path: 'list',
        name: 'ClassList',
        component: () => import('@/views/class/ClassList.vue'),
        meta: { title: '班级列表', roles: ['TEACHER', 'HEAD_TEACHER', 'SUPER_ADMIN', 'ADMIN'] },
      },
      {
        path: 'students',
        name: 'StudentList',
        component: () => import('@/views/class/StudentList.vue'),
        meta: { title: '学生管理', roles: ['TEACHER', 'HEAD_TEACHER', 'SUPER_ADMIN', 'ADMIN'] },
      },
    ],
  },
  // 学生课堂互动入口（查班级ID后跳转学生互动面板）
  {
    path: 'classroom/student-entry',
    name: 'StudentScreenEntry',
    component: () => import('@/views/class/StudentScreenEntry.vue'),
    meta: { title: '课堂互动', hidden: true, roles: ['STUDENT'] },
  },
  // 学生"我的班级"入口（查班级ID后跳转班级主页）
  {
    path: 'student/my-class',
    name: 'MyClassEntry',
    component: () => import('@/views/class/MyClassEntry.vue'),
    meta: { title: '我的班级', hidden: true, roles: ['STUDENT'] },
  },
  // 班级主页（独立路由，非班级管理子路由）
  {
    path: 'class/:id/home',
    name: 'ClassHome',
    component: () => import('@/views/class/ClassHome.vue'),
    meta: {
      title: '班级主页',
      hidden: true,
      roles: ['TEACHER', 'HEAD_TEACHER', 'STUDENT', 'INSPECTOR', 'SUPER_ADMIN', 'ADMIN'],
    },
  },
  // 智慧大屏（教师端）
  {
    path: 'class/:id/smart-screen',
    name: 'SmartScreen',
    component: () => import('@/views/class/SmartScreen.vue'),
    meta: {
      title: '智慧大屏',
      hidden: true,
      roles: ['TEACHER', 'HEAD_TEACHER', 'ADMIN', 'SUPER_ADMIN'],
      featureKey: 'feature.smart_screen_enabled',
    },
  },
  // 学生端互动面板
  {
    path: 'class/:id/smart-screen/student',
    name: 'StudentScreen',
    component: () => import('@/views/class/StudentScreen.vue'),
    meta: {
      title: '课堂互动',
      hidden: true,
      roles: ['STUDENT'],
      featureKey: 'feature.smart_screen_enabled',
    },
  },
];
