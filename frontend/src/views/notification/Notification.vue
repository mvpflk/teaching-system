<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">消息通知</h3>
      <el-button v-if="notifStore.unreadCount > 0" size="small" @click="handleMarkAllRead">全部已读</el-button>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="全部" name="all">
        <div v-if="list.length === 0" class="empty"><el-empty description="暂无消息" :image-size="60" /></div>
        <div
          v-for="n in list"
          :key="n.id"
          class="notif-card"
          :class="{ unread: n.isRead !== 1 }"
          @click="handleMarkRead(n)"
        >
          <div class="notif-left">
            <div class="notif-icon" :class="n.type">
              <el-icon><component :is="iconForType(n.type)" /></el-icon>
            </div>
          </div>
          <div class="notif-body">
            <div class="notif-title">{{ n.title }}</div>
            <div v-if="n.content" class="notif-content">{{ n.content }}</div>
            <div class="notif-time">{{ formatTime(n.createTime) }}</div>
          </div>
          <div v-if="n.isRead !== 1" class="notif-dot"></div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <div v-if="total > pageSize" class="notif-pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNotificationList } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import dayjs from 'dayjs'

const router = useRouter()
const notifStore = useNotificationStore()

const tab = ref('all')
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const handleMarkAllRead = async () => {
  await notifStore.markAllRead()
  list.value.forEach(n => { n.isRead = 1 })
  ElMessage.success('已全部标记为已读')
}

const iconForType = (t) => {
  if (/SHOWCASE/i.test(t)) return 'Trophy'
  if (/TASK/i.test(t)) return 'EditPen'
  if (/bbs/i.test(t)) return 'ChatDotSquare'
  if (/credit|redeem|delivery/i.test(t)) return 'Coin'
  if (/MORAL/i.test(t)) return 'Medal'
  return 'Bell'
}

const handleMarkRead = async (n) => {
  if (n.isRead !== 1) {
    await notifStore.markOneRead(n.id)
    n.isRead = 1
  }
  if (n.relatedId) {
    if (/SHOWCASE/i.test(n.type)) {
      router.push(`/showcase?detailId=${n.relatedId}`)
    } else if (/^exam/i.test(n.type)) {
      router.push('/student/tasks')
    } else if (/^bbs/i.test(n.type)) {
      router.push(`/bbs/post/${n.relatedId}`)
    } else if (/^homework/i.test(n.type)) {
      router.push('/student/tasks')
    } else if (/^credit|^redeem|^delivery/i.test(n.type)) {
      router.push('/credit/index')
    } else if (/^MORAL/i.test(n.type)) {
      router.push('/credit/moral-rank')
    }
  }
}

const formatTime = (t) => dayjs(t).format('YYYY-MM-DD HH:mm')

const loadList = async () => {
  try {
    const res = await getNotificationList({ page: page.value, pageSize: pageSize.value })
    if (res.code === 200) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { ElMessage.error('加载通知失败') }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.notif-card {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 16px; border-bottom: 1px solid var(--border-light); cursor: pointer;
  transition: background 0.2s; position: relative;
  &:hover { background: var(--bg-section); }
  &.unread { background: var(--primary-light); }
}
.notif-icon {
  width: 40px; height: 40px; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center; font-size: var(--fs-lg);
  &.homework { background: var(--primary-light); color: var(--primary-color); }
  &.exam { background: var(--bg-success-light); color: var(--success-color); }
  &.SHOWCASE_RECOMMEND, &.SHOWCASE_NEW { background: var(--bg-warning-light); color: var(--el-color-warning); }
  &.system { background: var(--bg-danger-light); color: var(--warning-color); }
}
.notif-body { flex: 1; }
.notif-title { font-weight: 500; font-size: var(--fs-md); margin-bottom: 4px; }
.notif-content { font-size: var(--fs-sm); color: var(--text-regular); margin-bottom: 4px; }
.notif-time { font-size: var(--fs-xs); color: var(--text-secondary); }
.notif-dot {
  width: 8px; height: 8px; border-radius: var(--radius-full); background: var(--primary-color);
  flex-shrink: 0; margin-top: 6px;
}
.empty { padding: 40px 0; }
.notif-pager { display: flex; justify-content: center; padding: 16px 0; }

@media (max-width: 768px) {
  .notif-card { padding: 12px; }
}
</style>
