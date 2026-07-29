<template>
  <div>
    <el-table
      :data="sortedStudents"
      :row-class-name="tableRowClassName"
      size="default"
      stripe
      style="width:100%;border:1px solid var(--border-color)"
    >
      <el-table-column label="姓名" width="110" fixed>
        <template #default="{ row }">
          <span class="status-dot" :class="{ danger: row.warning }"></span>
          <span class="name-link" @click="$emit('openDetail', row)">{{ row.studentName }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="!selectedClassId" prop="className" label="班级" width="110">
        <template #default="{ row }">
          <span class="text-muted" style="font-size:var(--fs-xs)">{{ row.className || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最近活跃" width="120" sortable>
        <template #default="{ row }">
          <span v-if="row.lastActiveAt" :class="activeTimeClass(row.lastActiveAt)" :title="row.lastActiveAt">
            {{ formatActiveTime(row.lastActiveAt) }}
          </span>
          <span v-else class="text-muted">从未</span>
        </template>
      </el-table-column>
      <el-table-column label="本周练习" width="115" sortable>
        <template #default="{ row }">
          <span v-if="row.weeklyPracticeCount > 0" class="practice-badge" :class="row.weeklyPracticeCount >= 3 ? 'badge-ok' : 'badge-lite'">
            {{ row.weeklyPracticeCount }}次
            <span class="trend-emoji">{{ trendEmoji(row.trend) }}</span>
          </span>
          <span v-else class="text-muted">0</span>
        </template>
      </el-table-column>
      <el-table-column label="数学估分" width="85" sortable>
        <template #default="{ row }">{{ row.mathScore || '-' }}</template>
      </el-table-column>
      <el-table-column label="英语词汇" width="85" sortable>
        <template #default="{ row }">{{ row.engVocab || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <div class="action-cell">
            <el-button type="primary" size="small" @click="$emit('remindOne', row)">提醒</el-button>
            <span v-if="row.lastTestScore >= 80" class="mini-test-tag test-pass">小测通过</span>
            <span v-else-if="row.lastTestScore > 0" class="mini-test-tag test-score">{{ row.lastTestScore }}分</span>
            <span v-else class="mini-test-tag test-none">未提交</span>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top:12px;justify-content:center"
      @current-change="$emit('update:page', $event)"
    />
  </div>
</template>

<script setup>
defineProps({
  sortedStudents: Array, selectedClassId: String, tableRowClassName: Function,
  formatActiveTime: Function, activeTimeClass: Function, trendEmoji: Function,
  total: Number, page: Number, pageSize: Number
})
defineEmits(['openDetail', 'remindOne', 'update:page'])
</script>

<style scoped>
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
  background: var(--el-color-success);
}
.status-dot.danger { background: var(--el-color-danger); }
.name-link {
  cursor: pointer;
  color: var(--text-primary);
  font-weight: 600;
}
.name-link:hover { color: var(--primary-color); }
.practice-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: var(--fs-xs);
  font-weight: 600;
}
.badge-ok {
  background: var(--el-color-success-light);
  color: var(--el-color-success);
  border: 1px solid var(--el-color-success);
}
.badge-lite {
  background: var(--el-color-warning-light);
  color: var(--el-color-warning);
  border: 1px solid var(--el-color-warning);
}
.trend-emoji { font-size: var(--fs-xs); }
.action-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.mini-test-tag { font-size: 11px; line-height: 1; }
.test-pass { color: var(--el-color-success); }
.test-score { color: var(--el-color-warning); }
.test-none { color: var(--text-disabled); }
.active-now { color: var(--el-color-success); font-weight: 600; }
.active-recent { color: var(--primary-color); }
.active-old { color: var(--text-secondary); }
:deep(.row-warning) { border-left: 3px solid var(--el-color-danger); }
:deep(.row-warning:hover) { border-left: 3px solid var(--el-color-danger); }
</style>
