<template>
  <div class="child-grades">
    <div class="page-header">
      <el-button text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ childName }} — 成绩记录</h2>
    </div>

    <div v-if="loading" class="sk-list"><div v-for="i in 5" :key="i" class="sk-row"><div class="sk-line w-40"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div></div></div>

    <!-- 移动端卡片 -->
    <template v-else-if="isMobile">
      <div class="mobile-grade-list">
        <div v-for="row in grades" :key="row.id" class="mobile-grade-card">
          <div class="mgc-header">
            <span class="mgc-title">{{ row.taskTitle }}</span>
            <el-tag size="small" :type="taskTypeTag(row.taskType)">{{ taskTypeLabel(row.taskType) }}</el-tag>
          </div>
          <div class="mgc-body">
            <div class="mgc-score">
              <span :class="scoreClass(row)" class="mgc-score-val">{{ row.score != null ? row.score : '-' }}</span>
              <span v-if="row.totalScore != null" class="mgc-score-total"> / {{ row.totalScore }}</span>
            </div>
            <div>
              <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </div>
            <div class="mgc-time">{{ row.submittedAt ? fmt(row.submittedAt) : '-' }}</div>
          </div>
        </div>
        <el-empty v-if="grades.length === 0" description="暂无成绩记录" :image-size="60" />
      </div>
    </template>

    <el-table
      v-else
      :data="grades"
      stripe
      empty-text="暂无成绩记录"
      style="width: 100%"
    >
      <el-table-column prop="taskTitle" label="任务名称" min-width="180" />
      <el-table-column label="任务类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="taskTypeTag(row.taskType)">{{ taskTypeLabel(row.taskType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="120" align="center">
        <template #default="{ row }">
          <span :class="scoreClass(row)">
            {{ row.score != null ? row.score : '-' }}
          </span>
          <span v-if="row.totalScore != null"> / {{ row.totalScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="提交时间" width="170">
        <template #default="{ row }">{{ row.submittedAt ? fmt(row.submittedAt) : '-' }}</template>
      </el-table-column>
    </el-table>

    <div class="footer-nav">
      <el-button type="primary" @click="$router.push(`/parent/children/${studentId}/timeline`)">
        查看成长足迹 <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getChildGrades } from '@/api/parent'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const route = useRoute()
const studentId = Number(route.params.studentId)
const childName = ref('')
const grades = ref([])
const loading = ref(false)

const TASK_TYPE_MAP = {
  PRE_CLASS: '课前预习', IN_CLASS: '课中活动', AFTER_CLASS: '课后巩固',
  FORMATIVE: '形成性评价', SUMMATIVE: '终结性评价', MORAL: '德育作业',
  LABOR: '劳动作业', SURVEY: '问卷调查', PRACTICE: '实训任务'
}
const STATUS_MAP = { PENDING: '待提交', SUBMITTED: '已提交', GRADED: '已评分', RETURNED: '已退回', EXEMPTED: '豁免' }

const taskTypeLabel = (t) => TASK_TYPE_MAP[t] || t || '-'
const taskTypeTag = (t) => {
  if (t === 'SUMMATIVE') return 'danger'
  if (t === 'FORMATIVE') return 'warning'
  if (t === 'MORAL' || t === 'LABOR') return 'success'
  return ''
}
const statusLabel = (s) => STATUS_MAP[s] || s || '-'
const statusTag = (s) => {
  if (s === 'GRADED') return 'success'
  if (s === 'SUBMITTED') return 'warning'
  if (s === 'RETURNED') return 'danger'
  return 'info'
}
const scoreClass = (row) => {
  if (row.score == null) return ''
  const pct = row.totalScore ? row.score / row.totalScore : 0
  if (pct >= 0.9) return 'score-good'
  if (pct >= 0.6) return 'score-ok'
  return 'score-low'
}
const fmt = (s) => {
  if (!s) return '-'
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getChildGrades(studentId)
    if (res.code === 200) {
      grades.value = res.data || []
      // 从路由query获取孩子姓名（由首页传入）
      childName.value = route.query.name || '孩子'
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.child-grades { max-width: 1000px; margin: 0 auto; padding: 8px; }
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: var(--fs-lg); }

.score-good { color: var(--el-color-success); font-weight: 600; }
.score-ok { color: var(--el-color-warning); font-weight: 600; }
.score-low { color: var(--el-color-danger); font-weight: 600; }

.footer-nav { margin-top: 24px; text-align: center; }

.sk-list { padding: 8px 0; }
.sk-row { display: flex; gap: 16px; padding: 16px 12px; border-bottom: 1px solid var(--border-light); }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); position: relative; overflow: hidden; }
.sk-line::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { transform: translateX(-100%) } 100% { transform: translateX(100%) } }
.w-20 { width: 20% } .w-40 { width: 40% }

@media (max-width: 768px) {
  .page-header h2 { font-size: var(--fs-md); }
  :deep(.el-table) { font-size: var(--fs-xs); }
}

/* 移动端成绩卡片 */
.mobile-grade-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-grade-card {
  padding: 14px; background: var(--bg-card); border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.mgc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 10px; }
.mgc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mgc-body { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.mgc-score { flex: 1; }
.mgc-score-val { font-size: var(--fs-xl); font-weight: 700; }
.mgc-score-total { font-size: var(--fs-sm); color: var(--text-secondary); }
.mgc-time { font-size: var(--fs-xs); color: var(--text-secondary); white-space: nowrap; }
</style>
