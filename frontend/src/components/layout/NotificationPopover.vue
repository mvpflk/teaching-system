<template>
  <el-popover
    placement="bottom"
    :width="360"
    trigger="click"
    @show="onOpen"
  >
    <template #reference>
      <el-badge :value="notifStore.unreadCount" :hidden="notifStore.unreadCount === 0" class="notif-badge">
        <el-button text circle class="icon-btn">
          <el-icon size="20"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>
    <div v-loading="loading" class="notif-panel">
      <div class="notif-header">
        <span style="font-weight:500;font-size:var(--fs-md)">消息</span>
        <el-button
          v-if="notifStore.unreadCount > 0"
          text
          size="small"
          @click="handleMarkAllRead"
        >
          全部已读
        </el-button>
      </div>
      <div v-if="!loading && notifications.length === 0" class="notif-empty">暂无消息</div>
      <div
        v-for="n in notifications.slice(0, 8)"
        :key="n.id"
        class="notif-item"
        :class="{ unread: n.isRead !== 1 }"
        @click="goNotif(n)"
      >
        <div v-if="n.isRead !== 1" class="notif-dot" />
        <div class="notif-body">
          <div class="notif-title">{{ n.title }}</div>
          <div class="notif-time">{{ formatTime(n.createTime) }}</div>
        </div>
      </div>
      <div v-if="notifications.length > 8" class="notif-more">
        <el-button text size="small" @click="router.push('/notification')">查看全部</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import dayjs from 'dayjs'

const router = useRouter()
const notifStore = useNotificationStore()
const notifications = ref([])
const loading = ref(false)

const onOpen = async () => {
  loading.value = true
  await notifStore.fetchLatestList()
  notifications.value = notifStore.latestList
  loading.value = false
}

const handleMarkAllRead = async () => {
  await notifStore.markAllRead()
  notifications.value.forEach(n => { n.isRead = 1 })
  ElMessage.success('已全部标记为已读')
}

const resolveRoute = (n) => {
  const t = n.type || ''
  const id = n.relatedId
  if (/SHOWCASE/i.test(t)) return id ? `/showcase?detailId=${id}` : '/showcase'
  if (/^bbs/i.test(t)) return id ? `/bbs/post/${id}` : '/bbs'
  if (/^exam/i.test(t)) return '/student/tasks'
  if (/^homework/i.test(t)) return '/student/tasks'
  if (/TASK_PUBLISHED|TASK_CLOSED|TASK_DEADLINE/i.test(t)) return '/student/tasks'
  if (/TASK_SUBMITTED|TASK_SUBMITTED_FOR_REVIEW|TASK_REVIEW_/i.test(t)) return '/teacher/tasks'
  if (/TASK_GRAD/i.test(t)) return '/student/tasks'
  if (/^credit|^redeem|^delivery/i.test(t)) return '/credit/index'
  if (/^MORAL/i.test(t)) return '/credit/moral-rank'
  return '/notification'
}

const goNotif = async (n) => {
  if (n.isRead !== 1) {
    await notifStore.markOneRead(n.id)
    n.isRead = 1
  }
  const target = resolveRoute(n)
  // 同一路由时加时间戳参数强制刷新
  if (router.currentRoute.value.fullPath === target || router.currentRoute.value.path === target) {
    router.replace({ path: target, query: { _t: Date.now() } })
  } else {
    router.push(target)
  }
}

const formatTime = (t) => dayjs(t).format('MM-DD HH:mm')
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
