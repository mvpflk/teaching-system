<template>
  <div class="child-homework">
    <div class="page-header">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ childName }} — 作业列表</h2>
    </div>

    <div v-if="loading" class="sk-list"><div v-for="i in 5" :key="i" class="sk-row"><div class="sk-line w-40"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div></div></div>
    <el-empty v-else-if="homework.length === 0" description="最近30天暂无作业" />

    <!-- 移动端卡片 -->
    <template v-else-if="isMobile">
      <div class="mobile-hw-list">
        <div v-for="row in homework" :key="row.id" class="mobile-hw-card">
          <div class="mhc-header">
            <span class="mhc-title">{{ row.title }}</span>
            <el-tag size="small">{{ row.subject || '通用' }}</el-tag>
          </div>
          <div class="mhc-body">
            <div class="mhc-meta"><span class="mhc-meta-label">类型</span><span>{{ taskTypeLabel(row.taskType) }}</span></div>
            <div class="mhc-meta"><span class="mhc-meta-label">截止</span><span>{{ row.deadline ? fmt(row.deadline) : '-' }}</span></div>
            <div class="mhc-meta">
              <span class="mhc-meta-label">状态</span>
              <el-tag size="small" :type="row.submitted ? 'success' : 'warning'">{{ row.submitted ? '已提交' : '未提交' }}</el-tag>
            </div>
            <div class="mhc-meta">
              <span class="mhc-meta-label">得分</span>
              <span v-if="row.score != null" :style="{ color: row.score >= (row.totalScore || 100) * 0.6 ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">{{ row.score }} / {{ row.totalScore || '-' }}</span>
              <span v-else class="text-muted">-</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-table
      v-else
      :data="homework"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="title" label="作业名称" min-width="180" />
      <el-table-column prop="subject" label="科目" width="80" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ taskTypeLabel(row.taskType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="截止时间" width="160">
        <template #default="{ row }">{{ row.deadline ? fmt(row.deadline) : '-' }}</template>
      </el-table-column>
      <el-table-column label="提交状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.submitted ? 'success' : 'warning'">
            {{ row.submitted ? '已提交' : '未提交' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="100">
        <template #default="{ row }">
          <span v-if="row.score != null" :style="{ color: row.score >= (row.totalScore || 100) * 0.6 ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">
            {{ row.score }} / {{ row.totalScore || '-' }}
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getChildHomework } from '@/api/parent'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const route = useRoute()
const childName = route.query?.name || '孩子'
const homework = ref([])
const loading = ref(false)

const TASK_TYPE_LABELS = { PRE_CLASS: '作业', IN_CLASS: '作业', AFTER_CLASS: '作业', FORMATIVE: '考试', SUMMATIVE: '考试', MORAL: '作业', LABOR: '作业', SURVEY: '问卷', PRACTICE: '实训' }
const taskTypeLabel = (t) => TASK_TYPE_LABELS[t] || t || '-'

const fmt = (s) => {
  if (!s) return '-'
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getChildHomework(route.params.studentId)
    if (res.code === 200) homework.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.child-homework { max-width: 1000px; margin: 0 auto; padding: 8px; }
.page-header { margin-bottom: 24px; display: flex; align-items: center; gap: 16px; }
.page-header h2 { margin: 0; font-size: 22px; }
.text-muted { color: var(--text-secondary); }
.sk-list { display: flex; flex-direction: column; gap: 12px; }
.sk-row { display: flex; gap: 16px; }
.sk-line { height: 16px; border-radius: 4px; background: linear-gradient(90deg, var(--skeleton-highlight) 25%, var(--skeleton-bg) 50%, var(--skeleton-highlight) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; }
.w-20 { width: 20%; } .w-40 { width: 40%; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* 移动端作业卡片 */
.mobile-hw-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-hw-card {
  padding: 14px; background: var(--bg-card); border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.mhc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 10px; }
.mhc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mhc-body { display: flex; flex-direction: column; gap: 6px; }
.mhc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mhc-meta-label { color: var(--text-secondary); min-width: 36px; }
</style>
