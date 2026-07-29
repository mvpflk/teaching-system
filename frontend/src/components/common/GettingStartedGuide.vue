<template>
  <div v-if="visible" class="gsg-wrap">
    <div class="gsg-card">
      <div class="gsg-header">
        <span class="gsg-title">{{ roleTitle }}</span>
        <el-button text size="small" @click="dismiss">知道了</el-button>
      </div>
      <p class="gsg-desc">{{ roleDesc }}</p>
      <div class="gsg-actions">
        <div
          v-for="item in roleFeatures"
          :key="item.label"
          class="gsg-action-item"
          @click="go(item.route)"
        >
          <span class="gsg-action-icon">{{ item.icon }}</span>
          <span class="gsg-action-label">{{ item.label }}</span>
          <el-icon class="gsg-action-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const GUIDE_KEY = 'getting_started_dismissed_' + (userStore.user?.id || '0')
const visible = ref(!localStorage.getItem(GUIDE_KEY))

const isStudent = computed(() => userStore.isStudent)
const isAdmin = computed(() => userStore.isAdmin)
const isHeadTeacher = computed(() => userStore.isHeadTeacher)

const roleTitle = computed(() => {
  if (isStudent.value) return '🎓 学习助手 · 快速上手'
  if (isHeadTeacher.value) return '🏫 班主任 · 工作台'
  if (isAdmin.value) return '⚙️ 管理面板 · 快速导航'
  return '📚 教师工作台 · 常用功能'
})

const roleDesc = computed(() => {
  if (isStudent.value) return '点击下方功能卡片可直接跳转，快速开始你的学习之旅'
  if (isHeadTeacher.value) return '班主任常用功能入口，点击直达'
  if (isAdmin.value) return '系统管理常用入口，点击直达'
  return '教师常用功能入口，点击直达'
})

const roleFeatures = computed(() => {
  if (isStudent.value) return [
    { label: '我的任务', icon: '📝', route: '/student/tasks' },
    { label: '偏科提分', icon: '📈', route: '/student/precision' },
    { label: '错题本', icon: '📕', route: '/wrong-book' },
    { label: '我的成长', icon: '🌱', route: '/student/growth' },
    { label: '积分中心', icon: '💰', route: '/credit/index' },
  ]
  if (isHeadTeacher.value) return [
    { label: '班级管理', icon: '🏫', route: '/class/list' },
    { label: '布置作业', icon: '📝', route: '/teacher/tasks/create?behavior=HOMEWORK' },
    { label: '偏科监督', icon: '📊', route: '/teacher/precision-monitor' },
    { label: '家校消息', icon: '💬', route: '/messages' },
    { label: '积分中心', icon: '💰', route: '/credit/index' },
  ]
  if (isAdmin.value) return [
    { label: '班级管理', icon: '🏫', route: '/class/list' },
    { label: '用户管理', icon: '👥', route: '/student/list' },
    { label: '系统设置', icon: '⚙️', route: '/settings' },
    { label: '数据概览', icon: '📊', route: '/system-management/overview' },
    { label: '任务管理', icon: '📋', route: '/teacher/tasks/list' },
  ]
  // 教师
  return [
    { label: '布置作业', icon: '📝', route: '/teacher/tasks/create?behavior=HOMEWORK' },
    { label: '批改作业', icon: '✅', route: '/teacher/tasks/list?status=IN_PROGRESS' },
    { label: '题库管理', icon: '📚', route: '/teacher/tasks/question-bank' },
    { label: '偏科监督', icon: '📊', route: '/teacher/precision-monitor' },
    { label: '师生论坛', icon: '💬', route: '/bbs' },
  ]
})

function go(route) {
  router.push(route)
}

function dismiss() {
  visible.value = false
  localStorage.setItem(GUIDE_KEY, '1')
}
</script>

<style scoped>
.gsg-wrap { margin-bottom: 16px; }
.gsg-card {
  background: var(--primary-light, var(--primary-light));
  border: 0.5px solid var(--primary-color, var(--primary-color));
  border-radius: var(--radius-md, 8px);
  padding: 14px 18px;
}
.gsg-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.gsg-title { font-size: var(--fs-md); font-weight: 600; color: var(--primary-color, var(--primary-color)); }
.gsg-desc { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin: 0 0 12px 0; }
.gsg-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.gsg-action-item {
  display: flex; align-items: center; gap: 6px; padding: 6px 12px;
  background: var(--bg-card, #fff); border: 0.5px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-sm, 4px); cursor: pointer; transition: all 0.15s;
  font-size: var(--fs-sm); color: var(--text-primary, var(--text-primary));
}
.gsg-action-item:hover { border-color: var(--primary-color, var(--primary-color)); background: #fff; }
.gsg-action-icon { font-size: var(--fs-lg); }
.gsg-action-label { font-weight: 500; }
.gsg-action-arrow { font-size: var(--fs-md); color: var(--text-disabled, var(--text-disabled)); margin-left: 2px; }

@media (max-width: 768px) {
  .gsg-actions { flex-direction: column; }
}
</style>
