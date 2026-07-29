/**
 * 路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { ElMessage, ElMessageBox } from 'element-plus'

// 考试中状态追踪（防止考试中途误离开）
export const examActiveState = { active: false, taskId: null }

// 路由模块
import taskRoutes, { taskParentChildren } from './modules/task.js'
import aiRoutes, { aiTaskChildren } from './modules/ai.js'
import agentRoutes from './modules/agent.js'
import classroomRoutes from './modules/classroom.js'
import practiceRoutes from './modules/practice.js'
import inspectorRoutes from './modules/inspector.js'
import teacherRoutes from './modules/teacher.js'
import parentRoutes from './modules/parent.js'
import systemRoutes from './modules/system.js'
import checkpointRoutes from './modules/checkpoint.js'
import studentRoutes from './modules/student.js'
import trainingRoutes from './modules/training.js'
import otherRoutes from './modules/other.js'
import knowledgeBaseRoutes from './modules/knowledgeBase.js'

// 注入 AI 子路由到任务管理父路由
const taskParent = taskRoutes.find(r => r.path === 'teacher/tasks')
if (taskParent) {
  taskParent.children = [...taskParentChildren, ...aiTaskChildren]
}

// 配置进度条
NProgress.configure({ showSpinner: false })

// 路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/home',
    meta: { requiresAuth: true },
    children: [
      ...otherRoutes,
      ...taskRoutes,
      ...aiRoutes,
      ...agentRoutes,
      ...studentRoutes,
      ...checkpointRoutes,
      ...trainingRoutes,
      ...practiceRoutes,
      ...classroomRoutes,
      ...teacherRoutes,
      ...parentRoutes,
      ...systemRoutes,
      ...inspectorRoutes,
      ...knowledgeBaseRoutes,
    ]
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  NProgress.start()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 教学管理系统` : '教学管理系统'

  const token = localStorage.getItem('token')

  // 登录页特殊处理：已登录则跳转首页
  if (to.path === '/login') {
    if (token) return next('/home')
    return next()
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth !== false) {
    if (!token) {
      ElMessage.warning('请先登录')
      return next({ name: 'Login', query: { redirect: to.fullPath } })
    }

    // 检查角色权限
    const roles = to.meta.roles
    if (roles && roles.length > 0) {
      const { useUserStore } = await import('@/stores/user')
      const userStore = useUserStore()
      if (!userStore.role) {
        try { await userStore.getInfo() } catch { /* 网络异常时保留当前状态 */ }
      }
      if (!roles.includes(userStore.role)) {
        ElMessage.warning('无权访问该页面')
        return next('/home')
      }
    }

    // 检查功能开关
    const featureKey = to.meta.featureKey
    if (featureKey) {
      const { useSettingsStore } = await import('@/stores/settings')
      const settingsStore = useSettingsStore()
      if (!settingsStore.loaded) await settingsStore.fetchFeatureFlags()
      if (!settingsStore.isEnabled(featureKey)) {
        ElMessage.warning('该功能暂未开放')
        return next('/home')
      }
    }

    // AI 组卷/诊断：任意学科教师或管理员可访问
    const AI_EXAM_ROUTES = ['/teacher/ai/exam-paper', '/teacher/ai/diagnosis']
    if (AI_EXAM_ROUTES.some(p => to.path.startsWith(p))) {
      const { useUserStore } = await import('@/stores/user')
      const userStore = useUserStore()
      if (!userStore.showAiCultureModules) {
        ElMessage.warning('AI 组卷/诊断功能暂未对您开放，请联系管理员')
        return next('/home')
      }
    }

    // 偏科提分：班级级权限检查（含英语子模块 /precision/english 路径）
    if (to.path.startsWith('/student/precision') || to.path.startsWith('/teacher/precision')
        || to.path.startsWith('/precision/english')) {
      const { useUserStore } = await import('@/stores/user')
      const userStore = useUserStore()
      if (!userStore.canAccessRemedial) {
        await userStore.fetchRemedialAccess()
      }
      if (!userStore.canAccessRemedial) {
        ElMessage.warning('偏科提分模块暂未对您所在班级开放')
        return next('/home')
      }
    }
  }

  // 移动端屏蔽不适页面
  const MOBILE_BLOCKED = [
    '/teacher/tasks/templates', '/teacher/list', '/teacher/groups',
    '/system-management', '/credit/admin', '/credit/shop',
    '/typing', '/classroom', '/student/training'
  ]
  const isMobile = window.innerWidth < 768
  if (isMobile && MOBILE_BLOCKED.some(p => to.path.startsWith(p))) {
    return next('/home')
  }

  // 考试中导航拦截：防止考试中途误离开
  if (examActiveState.active && from.name === 'StudentTaskDetail') {
    try {
      await ElMessageBox.confirm(
        '考试进行中，离开页面可能导致考试终止。确定要离开吗？',
        '离开考试',
        {
          confirmButtonText: '确定离开',
          cancelButtonText: '留在考试',
          type: 'warning',
        }
      )
      // 确认离开：停用考试状态
      examActiveState.active = false
      examActiveState.taskId = null
    } catch {
      // 取消：留在当前页面
      return next(false)
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
