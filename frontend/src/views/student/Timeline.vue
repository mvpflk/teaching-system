<template>
  <div class="page-card">
    <el-page-header class="mb-16" @back="router.back()">
      <template #content><span class="page-title">🌱 成长足迹</span></template>
    </el-page-header>

    <div v-loading="loading">
      <el-timeline v-if="events.length">
        <el-timeline-item
          v-for="e in events"
          :key="e.id"
          :timestamp="fmtTime(e.createdAt)"
          :type="iconType(e.eventType)"
          :icon="iconName(e.eventType)"
          placement="top"
        >
          <div class="tl-card">
            <div class="tl-title">{{ e.title }}</div>
            <div v-if="e.description" class="tl-desc">{{ e.description }}</div>
            <el-button
              v-if="e.link"
              size="small"
              text
              type="primary"
              @click="router.push(e.link)"
            >
              查看
            </el-button>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="成长足迹为空，完成作业、获得表扬后将自动记录" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const events = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const r = await request({ url: '/student/timeline', method: 'get' })
    if (r.code === 200) events.value = r.data || []
  } catch { /* */ }
  finally { loading.value = false }
}

const fmtTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth()+1}月${d.getDate()}日 ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const iconType = (t) => ({ submit: 'primary', grade: 'success', moral: 'warning', album_mention: 'info' }[t] || '')
const iconName = (t) => {
  const m = { submit: 'EditPen', grade: 'Trophy', moral: 'Star', album_mention: 'ChatDotSquare' }
  return m[t] || 'MoreFilled'
}

onMounted(load)
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.page-title { font-size: var(--fs-lg); font-weight: 600; }
.tl-card { background: var(--bg-card); padding: 12px 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.tl-title { font-weight: 500; color: var(--text-primary); font-size: var(--fs-base); }
.tl-desc { color: var(--text-secondary); font-size: var(--fs-sm); margin-top: 4px; }

@media (max-width: 768px) {
  .tl-card { padding: 10px 12px; }
  .tl-title { font-size: var(--fs-sm); }
}
</style>
