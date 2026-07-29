<template>
  <div class="child-timeline">
    <div class="page-header">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ childName }} — 成长足迹</h2>
    </div>

    <el-timeline v-if="timeline.length > 0" v-loading="loading">
      <el-timeline-item
        v-for="item in timeline"
        :key="item.id"
        :timestamp="fmt(item.createdAt)"
        placement="top"
        :type="eventColor(item.eventType)"
      >
        <el-card shadow="hover" class="timeline-card">
          <div class="event-title">{{ item.title }}</div>
          <div v-if="item.description" class="event-desc">{{ item.description }}</div>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-if="!loading && timeline.length === 0" description="暂无成长足迹" />

    <div class="footer-nav">
      <el-button type="primary" @click="$router.push(`/parent/children/${studentId}/grades?name=${encodeURIComponent(childName)}`)">
        <el-icon><ArrowLeft /></el-icon> 返回成绩
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getChildTimeline } from '@/api/parent'

const route = useRoute()
const studentId = Number(route.params.studentId)
const childName = ref(route.query.name || '孩子')
const timeline = ref([])
const loading = ref(false)

const EVENT_COLORS = {
  SUBMIT: 'primary', GRADED: 'success', MORAL_PRAISE: 'warning',
  ACHIEVEMENT: 'success', GROWTH_REPORT: 'info', CLASS_ACTIVITY: ''
}
const eventColor = (t) => EVENT_COLORS[t] || ''

const fmt = (s) => {
  if (!s) return '-'
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getChildTimeline(studentId)
    if (res.code === 200) timeline.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.child-timeline { max-width: 800px; margin: 0 auto; padding: 8px; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: var(--fs-lg); }

.timeline-card { margin-bottom: 4px; }
.event-title { font-size: var(--fs-md); font-weight: 500; }
.event-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 4px; }

.footer-nav { margin-top: 24px; text-align: center; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
