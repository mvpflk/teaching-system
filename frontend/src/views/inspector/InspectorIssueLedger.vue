<template>
  <div class="inspector-issue-ledger">
    <div class="page-header">
      <div>
        <h3 class="page-title">问题台账</h3>
        <span class="header-subtitle">全生命周期跟踪巡视问题</span>
      </div>
      <el-button type="primary" @click="$router.push('/inspector/issues/create')">新建问题</el-button>
    </div>

    <div v-if="stats.total !== undefined" class="stats-row">
      <div class="stat-card"><span class="s-val">{{ stats.total }}</span><span class="s-lbl">总数</span></div>
      <div class="stat-card danger"><span class="s-val">{{ stats.openCount }}</span><span class="s-lbl">待处理</span></div>
      <div class="stat-card warning"><span class="s-val">{{ stats.inProgressCount }}</span><span class="s-lbl">处理中</span></div>
      <div class="stat-card success"><span class="s-val">{{ stats.verifiedCount }}</span><span class="s-lbl">已验收</span></div>
      <div class="stat-card danger"><span class="s-val">{{ stats.overdueCount }}</span><span class="s-lbl">超期</span></div>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="filters.status"
        placeholder="状态"
        clearable
        @change="loadData"
      >
        <el-option label="全部" value="" />
        <el-option
          v-for="s in statusOptions"
          :key="s.value"
          :value="s.value"
          :label="s.label"
        />
      </el-select>
      <el-select
        v-model="filters.category"
        placeholder="分类"
        clearable
        @change="loadData"
      >
        <el-option label="全部" value="" />
        <el-option
          v-for="c in categoryOptions"
          :key="c.value"
          :value="c.value"
          :label="c.label"
        />
      </el-select>
      <el-select
        v-model="filters.severity"
        placeholder="严重程度"
        clearable
        @change="loadData"
      >
        <el-option label="全部" value="" />
        <el-option
          v-for="s in severityOptions"
          :key="s.value"
          :value="s.value"
          :label="s.label"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        value-format="YYYY-MM-DD"
        @change="onDateChange"
      />
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <template v-if="!isMobile">
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        style="cursor:pointer"
        @row-click="onRowClick"
      >
        <el-table-column label="标题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }"><span class="link-text">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column label="严重程度" width="90">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" size="small">{{ severityMap[row.severity] || row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="260">
          <template #default="{ row }">
            <div class="status-flow">
              <span v-for="(s, i) in flowSteps" :key="s.key" :class="['flow-node', flowClass(row.status, s.key, i)]">
                {{ s.label }}<i v-if="i < flowSteps.length - 1" class="flow-arrow">→</i>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="assignedTo" label="责任人" width="80" />
        <el-table-column label="截止日期" width="100">
          <template #default="{ row }">
            <span :class="{ expired: row.deadline && isExpired(row.deadline) }">{{ row.deadline || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 移动端卡片 -->
    <div v-else-if="isMobile" class="mobile-list">
      <div v-for="row in list" :key="row.id" class="mobile-card" @click="onRowClick(row)">
        <div class="mc-header">
          <span class="mc-title">{{ row.title }}</span>
          <el-tag :type="severityTag(row.severity)" size="small">{{ severityMap[row.severity] || row.severity }}</el-tag>
        </div>
        <div class="mc-body">
          <div class="mc-meta"><span class="mc-lbl">分类</span><span>{{ categoryMap[row.category] || row.category }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">责任人</span><span>{{ row.assignedTo || '-' }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">截止</span><span :class="{ expired: row.deadline && isExpired(row.deadline) }">{{ row.deadline || '-' }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">状态</span><span>{{ flowSteps.find(s => s.key === row.status)?.label || row.status }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">创建</span><span>{{ row.createdAt }}</span></div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无问题" :image-size="60" />
    </div>

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getIssues, getIssueStats } from '@/api/inspectorManage'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const router = useRouter()
const loading = ref(false)
const list = ref([])
const stats = ref({})
const page = ref(1)
const size = ref(20)
const total = ref(0)
const dateRange = ref(null)

const filters = reactive({ status: '', category: '', severity: '' })

const statusOptions = [
  { value: 'OPEN', label: '待处理' }, { value: 'ASSIGNED', label: '已指派' }, { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'RESOLVED', label: '已解决' }, { value: 'VERIFIED', label: '已验收' }, { value: 'REJECTED', label: '已驳回' }
]
const categoryOptions = [
  { value: 'TEACHING_QUALITY', label: '教学质量' }, { value: 'CLASSROOM_DISCIPLINE', label: '课堂纪律' },
  { value: 'HOMEWORK_PROCRASTINATION', label: '作业拖拉' }, { value: 'ATTENDANCE', label: '出勤' },
  { value: 'MORAL_EDUCATION', label: '德育' }, { value: 'EXAM_IRREGULARITY', label: '考试违纪' }, { value: 'OTHER', label: '其他' }
]
const severityOptions = [
  { value: 'LOW', label: '低' }, { value: 'MEDIUM', label: '中' }, { value: 'HIGH', label: '高' }, { value: 'CRITICAL', label: '严重' }
]

const categoryMap = { TEACHING_QUALITY: '教学质量', CLASSROOM_DISCIPLINE: '课堂纪律', HOMEWORK_PROCRASTINATION: '作业拖拉', ATTENDANCE: '出勤', MORAL_EDUCATION: '德育', EXAM_IRREGULARITY: '考试违纪', OTHER: '其他' }
const severityMap = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' }
const severityTag = (s) => ({ LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' })[s] || 'info'

const flowSteps = [
  { key: 'OPEN', label: '待处理' }, { key: 'ASSIGNED', label: '已指派' }, { key: 'IN_PROGRESS', label: '处理中' },
  { key: 'RESOLVED', label: '已解决' }, { key: 'VERIFIED', label: '已验收' }
]

const statusOrder = { OPEN: 0, ASSIGNED: 1, IN_PROGRESS: 2, RESOLVED: 3, VERIFIED: 4, REJECTED: -1 }

const flowClass = (status, stepKey, idx) => {
  const cur = statusOrder[status]
  const step = statusOrder[stepKey]
  if (status === 'REJECTED') return step === 4 ? 'rejected' : 'past'
  if (step < cur) return 'past'
  if (step === cur) return 'active'
  return 'future'
}

const isExpired = (deadline) => new Date(deadline) < new Date()

const onRowClick = (row) => router.push(`/inspector/issues/${row.id}`)
const onDateChange = (val) => {
  if (val) { filters.startDate = val[0]; filters.endDate = val[1] }
  else { filters.startDate = undefined; filters.endDate = undefined }
}

const loadStats = async () => {
  const res = await getIssueStats()
  if (res.code === 200) stats.value = res.data
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getIssues({ ...filters, page: page.value, size: size.value })
    if (res.code === 200) { list.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const resetFilters = () => { filters.status = ''; filters.category = ''; filters.severity = ''; dateRange.value = null; filters.startDate = undefined; filters.endDate = undefined; page.value = 1; loadData() }

onMounted(async () => { loadData(); loadStats() })
</script>

<style scoped lang="scss">
.inspector-issue-ledger { max-width: 1400px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.stats-row { display: flex; gap: 14px; flex-wrap: wrap; margin-bottom: 20px;
  .stat-card { padding: 14px 22px; background: var(--bg-card); border-radius: var(--radius-md); text-align: center; box-shadow: var(--shadow-sm); min-width: 80px;
    .s-val { display: block; font-size: 22px; font-weight: 700; color: var(--text-primary); }
    .s-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
    &.danger .s-val { color: var(--danger-color); }
    &.warning .s-val { color: var(--warning-color); }
    &.success .s-val { color: var(--success-color); }
  }
}
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }
.link-text { color: var(--primary-color); font-weight: 500; }
.status-flow { display: flex; align-items: center; flex-wrap: nowrap; gap: 0; font-size: var(--fs-xs); white-space: nowrap;
  .flow-node { padding: 2px 4px; border-radius: var(--radius-xs); white-space: nowrap;
    &.active { color: var(--primary-color); font-weight: 600; background: var(--primary-light, #ecf5ff); }
    &.past { color: var(--success-color); }
    &.future { color: var(--text-disabled, #c0c4cc); }
    &.rejected { color: var(--danger-color); font-weight: 600; }
  }
  .flow-arrow { color: var(--text-disabled, #c0c4cc); margin: 0 1px; font-style: normal; }
}
.expired { color: var(--danger-color); font-weight: 600; }
.empty-state { padding: 40px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
@media (max-width: 768px) {
  .inspector-issue-ledger { padding: var(--spacing-md, 16px); }
  .page-header { flex-direction: column; align-items: stretch; gap: 8px; }
  .filter-bar { flex-direction: column; align-items: stretch; :deep(.el-select), :deep(.el-date-editor) { width: 100%; } }
  .stats-row { gap: 8px; .stat-card { padding: 10px 14px; flex: 1; .s-val { font-size: var(--fs-lg); } } }
  :deep(.el-table) { font-size: var(--fs-xs); }
}

/* 移动端卡片 */
.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card { padding: 14px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); cursor: pointer; }
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mc-body { display: flex; flex-direction: column; gap: 4px; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 56px; }
</style>
