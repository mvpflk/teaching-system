<template>
  <div class="am-filter">
    <el-select
      :model-value="filters.classId"
      placeholder="全部班级"
      clearable
      size="small"
      style="width:140px"
      @change="emit('update:classId', $event)"
    >
      <el-option v-for="c in classOptions" :key="c.id" :label="c.className" :value="c.id" />
    </el-select>
    <el-select
      :model-value="filters.alertType"
      placeholder="预警类型"
      clearable
      size="small"
      style="width:140px"
      @change="emit('update:alertType', $event)"
    >
      <el-option label="低分预警" value="LOW_SCORE" />
      <el-option label="缺交预警" value="MISSING" />
      <el-option label="成绩骤降" value="SCORE_DROP" />
    </el-select>
    <el-input
      :model-value="filters.studentName"
      placeholder="学生姓名"
      clearable
      size="small"
      style="width:140px"
      @keyup.enter="emit('search')"
      @clear="emit('search')"
      @update:model-value="emit('update:studentName', $event)"
    />
    <el-select
      :model-value="filters.handledStatus"
      placeholder="处理状态"
      clearable
      size="small"
      style="width:140px"
      @change="emit('update:handledStatus', $event)"
    >
      <el-option label="未读" value="UNREAD" />
      <el-option label="已读" value="READ" />
      <el-option label="已联系家长" value="CONTACTED" />
      <el-option label="已忽略" value="IGNORED" />
    </el-select>
    <el-button type="primary" size="small" @click="emit('search')">查询</el-button>
    <div class="am-filter-spacer"></div>
    <el-button
      v-if="selectedIdsLength"
      size="small"
      type="success"
      @click="emit('batch-action', 'READ')"
    >
      批量已读 ({{ selectedIdsLength }})
    </el-button>
    <el-button
      v-if="selectedIdsLength"
      size="small"
      type="warning"
      @click="emit('batch-action', 'CONTACTED')"
    >
      批量已联系 ({{ selectedIdsLength }})
    </el-button>
  </div>
</template>

<script setup>
defineProps({
  filters: { type: Object, required: true },
  classOptions: { type: Array, default: () => [] },
  selectedIdsLength: { type: Number, default: 0 }
})

const emit = defineEmits([
  'update:classId', 'update:alertType', 'update:handledStatus',
  'update:studentName', 'search', 'batch-action'
])
</script>

<style scoped>
.am-filter { display: flex; gap: 8px; align-items: center; margin-bottom: 14px; flex-wrap: wrap; }
.am-filter-spacer { flex: 1; }
@media (max-width: 768px) {
  .am-filter { flex-direction: column; align-items: stretch; }
  .am-filter :deep(.el-select) { width: 100%; }
  .am-filter :deep(.el-button) { width: 100%; }
}
</style>
