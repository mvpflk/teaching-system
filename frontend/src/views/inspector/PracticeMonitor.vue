<template>
  <div class="practice-monitor">
    <div class="page-header">
      <h3>📊 实训监控</h3>
    </div>

    <div v-if="loading" class="stat-grid">
      <div v-for="i in 4" :key="i" class="stat-card sk-card"><div class="sk-line w-60" /><div class="sk-line w-40" /></div>
    </div>
    <div v-else class="stat-grid">
      <div class="stat-card"><div class="stat-icon" style="background:var(--primary-color)">📋</div><div class="stat-info"><div class="stat-value">{{ summary.totalTasks }}</div><div class="stat-label">实训任务总数</div></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:var(--el-color-success)">📤</div><div class="stat-info"><div class="stat-value">{{ summary.avgSubmitRate }}%</div><div class="stat-label">平均提交率</div></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:var(--el-color-warning)">⭐</div><div class="stat-info"><div class="stat-value">{{ summary.avgScore }}</div><div class="stat-label">平均分</div></div></div>
      <div class="stat-card"><div class="stat-icon" style="background:var(--el-color-danger)">⚠️</div><div class="stat-info"><div class="stat-value">{{ summary.overdueCount }}</div><div class="stat-label">逾期数</div></div></div>
    </div>

    <el-table
      v-loading="loading"
      :data="classData"
      stripe
      @expand-change="onExpand"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div v-if="row._tasks">
            <el-tag v-for="t in row._tasks" :key="t.id" style="margin:2px">{{ t.title }}</el-tag>
          </div>
          <el-empty v-else description="暂无任务详情" :image-size="32" />
        </template>
      </el-table-column>
      <el-table-column prop="className" label="班级" min-width="120" />
      <el-table-column prop="taskCount" label="实训任务数" width="100" />
      <el-table-column prop="submittedCount" label="已提交" width="80" />
      <el-table-column label="提交率" width="100">
        <template #default="{ row }">
          <el-progress :percentage="row.submitRate || 0" :stroke-width="8" :status="row.submitRate >= 80 ? 'success' : 'warning'" />
        </template>
      </el-table-column>
      <el-table-column label="平均分" width="80">
        <template #default="{ row }">{{ row.avgScore || '-' }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPracticeStats } from '@/api/inspector'

const loading = ref(true)
const summary = ref({ totalTasks: 0, avgSubmitRate: 0, avgScore: 0, overdueCount: 0 })
const classData = ref([])

async function load() {
  loading.value = true
  try {
    const res = await getPracticeStats()
    if (res.code === 200) {
      summary.value = res.data.summary || {}
      classData.value = (res.data.classes || []).map(c => ({ ...c, _tasks: [] }))
    }
  } finally { loading.value = false }
}

function onExpand(row) {
  if (row._tasks && row._tasks.length) return
}

onMounted(() => load())
</script>

<style scoped>
.practice-monitor { max-width: 1200px; margin: 0 auto; padding: 16px; }
.page-header { margin-bottom: 20px; h3 { margin: 0; } }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 14px; padding: 18px; background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); }
.stat-icon { width: 48px; height: 48px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 22px; }
.stat-value { font-size: var(--fs-2xl); font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.sk-card { display: flex; flex-direction: column; gap: 8px; padding: 16px; }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.w-40 { width: 40% } .w-60 { width: 60% }
@media (max-width: 768px) {
  .practice-monitor { padding: var(--spacing-md, 16px); }
  .stat-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 8px; }
}
</style>
