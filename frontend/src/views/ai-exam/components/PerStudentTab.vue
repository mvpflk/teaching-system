<template>
  <div>
    <div class="diag-filter-bar">
      <el-radio-group :model-value="studentFilter" size="small" @change="$emit('student-filter-change', $event)">
        <el-radio-button value="all">全部 ({{ students.length }})</el-radio-button>
        <el-radio-button value="已达标">已达标 ≥85</el-radio-button>
        <el-radio-button value="成长中">成长中 70-84</el-radio-button>
        <el-radio-button value="发展中">发展中 60-69</el-radio-button>
        <el-radio-button value="起步期">起步期 &lt;60</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="students" stripe size="small" max-height="600">
      <el-table-column label="#" type="index" width="50" />
      <el-table-column prop="name" label="姓名" width="90" />
      <el-table-column prop="className" label="班级" width="110" />
      <el-table-column label="得分" width="80" sortable sort-by="score">
        <template #default="{row}">
          <span :style="{color:rateColor((row.score||0)),fontWeight:700}">{{ row.score ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="80">
        <template #default="{row}">
          <span :class="'diag-label-tag diag-label--' + row.label">{{ row.label }}</span>
        </template>
      </el-table-column>
      <el-table-column label="薄弱知识点" min-width="180">
        <template #default="{row}">
          <template v-if="row.weakPoints?.length">
            <el-popover
              v-for="wp in row.weakPoints"
              :key="wp.kpId"
              trigger="hover"
              placement="top"
              :width="240"
            >
              <template #reference>
                <el-tag size="small" type="danger" effect="plain" style="margin:2px;cursor:pointer">
                  {{ wp.kpName }}
                </el-tag>
              </template>
              <div>
                <div style="font-weight:600;margin-bottom:4px">{{ wp.kpName }}</div>
                <div style="font-size:var(--fs-xs);color:#606266">错误率 {{ wp.errorRate }}% ({{ wp.wrong }}/{{ wp.total }}题)</div>
              </div>
            </el-popover>
          </template>
          <span v-else style="color:var(--el-color-success, #67c23a);font-size:var(--fs-xs)">无·已全部掌握</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
defineProps({
  students: { type: Array, default: () => [] },
  studentFilter: { type: String, default: 'all' }
})
defineEmits(['student-filter-change'])

function rateColor(r) {
  return r >= 80 ? 'var(--el-color-success, #67c23a)' : r >= 60 ? 'var(--el-color-primary, #409eff)' : r >= 40 ? 'var(--el-color-warning, #e6a23c)' : 'var(--el-color-danger, #f56c6c)'
}
</script>

<style scoped>
.diag-filter-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.diag-label-tag { font-size: var(--fs-xs); padding: 2px 8px; border-radius: 10px; font-weight: 500; white-space: nowrap; }
.diag-label--已达标 { background: var(--el-color-success-light-9, #e8f5e9); color: var(--el-color-success-dark-2, var(--el-color-success)); }
.diag-label--成长中 { background: var(--el-color-primary-light-9, #e3f2fd); color: var(--el-color-primary-dark-2, #1565c0); }
.diag-label--发展中 { background: var(--el-color-warning-light-9, #fff3e0); color: var(--el-color-warning-dark-2, #e65100); }
.diag-label--起步期 { background: var(--el-color-danger-light-9, #fce4ec); color: var(--el-color-danger-dark-2, #c62828); }
</style>
