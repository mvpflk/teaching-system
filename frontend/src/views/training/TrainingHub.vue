<template>
  <div class="training-hub">
    <div class="hub-header">
      <h3>实训中心</h3>
      <p class="hub-desc">选择实训模块，开始动手操作练习</p>
    </div>

    <div class="hub-grid">
      <TrainingCategoryCard
        v-for="cat in visibleCards"
        :key="cat.key"
        v-bind="cat"
        @click="enterCategory(cat)"
      />
    </div>

    <TrainingRecentList
      v-if="recentTasks.length > 0"
      :tasks="recentTasks"
      @select="(t) => router.push(`/training/${t.id}/do`)"
    />

    <!-- 考纲覆盖度仪表盘（教师端） -->
    <SyllabusCoverage v-if="isTeacher" />

    <el-empty v-if="!visibleCards.length" description="暂无可用实训模块" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useIsMobile } from '@/composables/useIsMobile'
import SyllabusCoverage from './components/SyllabusCoverage.vue'
import { useUserStore } from '@/stores/user'
import TrainingCategoryCard from './components/TrainingCategoryCard.vue'
import TrainingRecentList from './components/TrainingRecentList.vue'
import { listTrainingTasks } from '@/api/training'

const router = useRouter()
const { isMobile } = useIsMobile()
const userStore = useUserStore()
const recentTasks = ref([])
const isTeacher = computed(() => userStore.isTeacher)

const HUB_CARDS = computed(() => [
  { key: 'tasks', title: '步骤式实训', description: '文字论述、文件提交、选择题等通用实训', icon: 'Document',
    route: isTeacher.value ? '/training/create' : '/student/tasks', taskCount: 0 },
  { key: 'simulation', title: '仿真实训', description: 'Windows 操作、网络命令配置', icon: 'Monitor',
    route: isTeacher.value ? '/teacher/simulation/tasks' : '/student/training/network', majorOnly: '计算机', taskCount: 0 },
  { key: 'typing', title: '打字实训', description: '中英文录入练习与竞赛', icon: 'EditPen',
    route: isTeacher.value ? '/teacher/typing/competitions' : '/typing', taskCount: 0 }
])

const visibleCards = computed(() => {
  return HUB_CARDS.value.filter(cat => {
    if (isMobile.value && cat.majorOnly === '计算机') return false
    return true
  })
})

function enterCategory(cat) {
  router.push(cat.route).catch(() => {})
}

onMounted(async () => {
  try {
    const res = await listTrainingTasks({ page: 1, size: 5 })
    if (res.code === 200 && res.data?.records) {
      recentTasks.value = res.data.records || []
    }
  } catch { /* 静默 */ }
})
</script>

<style scoped>
.training-hub { max-width: 1000px; margin: 0 auto; padding: 24px 16px; }
.hub-header { margin-bottom: 24px; }
.hub-header h3 { font-size: 22px; color: var(--text-primary); margin: 0 0 6px; }
.hub-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin: 0; }
.hub-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
@media (max-width: 768px) {
  .training-hub { padding: 12px 8px; }
  .hub-grid { grid-template-columns: 1fr; }
}
</style>
