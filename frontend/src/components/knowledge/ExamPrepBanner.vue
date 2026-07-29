<template>
  <div v-if="!hidden" class="epb-banner">
    <div class="epb-header">
      <span class="epb-title">⏰ 距「{{ taskTitle }}」还有 {{ daysLeft }} 天</span>
    </div>

    <div v-if="loading" class="epb-skel"><el-skeleton :rows="2" animated /></div>

    <template v-else-if="weakPoints.length">
      <div class="epb-list">
        <div v-for="w in weakPoints" :key="w.nodeId" class="epb-item">
          <span class="epb-item-name">{{ w.nodeName || '知识点' }}</span>
          <span class="epb-item-cards">{{ w.cardCount }} 张卡片 · {{ w.estimatedMinutes }} 分钟</span>
        </div>
        <div v-for="s in skipPoints" :key="s.nodeId" class="epb-item epb-skip">
          <span class="epb-item-name">{{ s.nodeName }}</span>
          <span class="epb-item-label">已掌握 {{ s.masteryPercent || 0 }}% ，可跳过</span>
        </div>
      </div>
      <div class="epb-footer">
        <span>共 {{ totalCards }} 张卡片 · 预计 {{ totalMinutes }} 分钟</span>
        <el-button size="small" type="primary" @click="router.push(`/student/tasks/${taskId}`)">去完成考试</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getExamPrepPack } from '@/api/knowledgeBase'

const props = defineProps({
  taskId: { type: Number, required: true },
  taskTitle: { type: String, default: '' },
  deadline: { type: String, default: '' },
  taskType: { type: String, default: '' }
})

const router = useRouter()
const loading = ref(true)
const hidden = ref(true)
const daysLeft = ref(0)
const weakPoints = ref([])
const skipPoints = ref([])
const totalCards = ref(0)
const totalMinutes = ref(0)

const init = async () => {
  const isExam = ['FORMATIVE', 'SUMMATIVE'].includes(props.taskType)
  if (!isExam || !props.deadline) { hidden.value = true; return }
  const days = Math.ceil((new Date(props.deadline) - new Date()) / 86400000)
  if (days > 3 || days < 0) { hidden.value = true; return }
  daysLeft.value = days

  try {
    const r = await getExamPrepPack(props.taskId)
    if (r.code === 200 && r.data && r.data.weakPoints?.length) {
      weakPoints.value = r.data.weakPoints
      skipPoints.value = r.data.skipPoints || []
      totalCards.value = r.data.totalCards
      totalMinutes.value = r.data.totalEstimatedMinutes
      hidden.value = false
    } else {
      hidden.value = true
    }
  } catch { hidden.value = true }
  finally { loading.value = false }
}

watch(() => props.taskId, init, { immediate: true })
</script>

<style scoped lang="scss">
.epb-banner {
  background: linear-gradient(135deg, #fef7e8, #fff5e6);
  border: 1px solid var(--el-color-warning, #e6a23c);
  border-left: 4px solid var(--el-color-warning, #e6a23c);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  margin-bottom: 16px;
}

.epb-header { margin-bottom: 10px; }

.epb-title {
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--el-color-warning, #e6a23c);
}

.epb-skel { padding: 8px 0; }

.epb-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.epb-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-sm);
}

.epb-item-name {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.epb-item-cards {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  font-weight: 500;
}

.epb-skip {
  opacity: 0.55;
}

.epb-item-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.epb-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
</style>
