<template>
  <template v-if="loading && !records.length">
    <div class="am-sk">
      <div v-for="n in 5" :key="n" class="am-sk-row">
        <div class="am-sk-line" style="width:80px"></div>
        <div class="am-sk-line" style="width:100px"></div>
        <div class="am-sk-line" style="flex:1"></div>
        <div class="am-sk-line" style="width:60px"></div>
      </div>
    </div>
  </template>
  <el-card v-else-if="records.length" v-loading="loading" shadow="never" class="am-table-card">
    <el-table :data="records" stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" :selectable="() => true" />
      <el-table-column prop="studentName" label="学生" width="100" fixed="left">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="emit('view-detail', row)">
            {{ row.studentName }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="className" label="班级" width="110" />
      <el-table-column label="预警类型" width="130">
        <template #default="{ row }">
          <el-tag :type="row.alertType === 'LOW_SCORE' ? 'danger' : 'warning'" size="small" effect="dark">
            {{ row.ruleName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="严重程度" width="100" align="center">
        <template #default="{ row }">
          <span :class="['am-severity', 'am-sev-' + getSeverity(row)]">{{ getSeverityLabel(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="alertSummary" label="预警详情" min-width="240" show-overflow-tooltip />
      <el-table-column prop="createTime" label="触发时间" width="160">
        <template #default="{ row }">{{ fmtFull(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="通知" width="120" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.notifiedTeacher" size="small" type="success" class="am-notify-tag">班主任</el-tag>
          <el-tag v-if="row.notifiedParents" size="small" type="warning" class="am-notify-tag">家长</el-tag>
          <span v-if="!row.notifiedTeacher && !row.notifiedParents" class="am-text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.handledStatus)">{{ statusLabel(row.handledStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.handledStatus === 'UNREAD'" size="small" type="primary" link @click="emit('mark-read', row)">标记已读</el-button>
          <el-button v-if="row.handledStatus !== 'CONTACTED' && row.handledStatus !== 'IGNORED'" size="small" type="success" link @click="emit('mark-contacted', row)">已联系</el-button>
          <el-button v-if="row.handledStatus !== 'IGNORED'" size="small" type="info" link @click="emit('ignore', row)">忽略</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="am-pagination">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @update:current-page="emit('page-change', $event)"
      />
    </div>
  </el-card>
</template>

<script setup>
import { fmtFull } from '@/utils/date'

const props = defineProps({
  records: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 20 }
})

const emit = defineEmits([
  'page-change', 'selection-change',
  'mark-read', 'mark-contacted', 'ignore', 'view-detail'
])

const STATUS_MAP = { UNREAD: '未读', READ: '已读', CONTACTED: '已联系家长', IGNORED: '已忽略' }
const statusLabel = (s) => STATUS_MAP[s] || s
const statusTag = (s) => ({ UNREAD: 'danger', CONTACTED: 'success', IGNORED: 'info' }[s] || '')

const getSeverity = (row) => {
  const n = row.minConsecutive || 2
  if (n >= 5) return 'high'
  if (n >= 3) return 'medium'
  return 'low'
}
const getSeverityLabel = (row) => ({ high: '严重', medium: '警告', low: '关注' }[getSeverity(row)] || '关注')

const onSelectionChange = (rows) => {
  emit('selection-change', rows.map(r => r.id))
}
</script>

<style scoped>
.am-sk { display: flex; flex-direction: column; gap: 10px; padding: 16px; }
.am-sk-row { display: flex; gap: 12px; }
.am-sk-line { height: 24px; border-radius: 4px; background: linear-gradient(90deg, var(--bg-section) 25%, var(--bg-card) 50%, var(--bg-section) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.am-table-card :deep(.el-card__body) { padding: 0; }
.am-notify-tag { margin-right: 4px; }
.am-text-muted { color: var(--text-secondary); font-size: var(--fs-sm); }
.am-pagination { display: flex; justify-content: center; padding: 16px 0; }
.am-severity { font-size: var(--fs-xs); font-weight: 600; padding: 2px 8px; border-radius: 10px; }
.am-sev-high { background: var(--bg-danger-light); color: var(--el-color-danger); }
.am-sev-medium { background: var(--bg-warning-light); color: var(--el-color-warning); }
.am-sev-low { background: var(--primary-light); color: var(--primary-color); }
</style>
