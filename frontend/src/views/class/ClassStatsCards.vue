<template>
  <div class="stat-row mb-20">
    <div class="stat-chip">总班级数：<strong>{{ list.length }}</strong></div>
    <div class="stat-chip">在读：<strong>{{ activeCount }}</strong></div>
    <div class="stat-chip">已毕业：<strong>{{ graduatedCount }}</strong></div>
    <div class="stat-chip">总学生数：<strong>{{ totalStudents }}</strong></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  list: { type: Array, required: true }
})

const activeCount = computed(() => props.list.filter(c => c.status === 1).length)
const graduatedCount = computed(() => props.list.filter(c => c.status !== 1).length)
const totalStudents = computed(() => props.list.reduce((s, c) => s + (c.studentCount || 0), 0))
</script>

<style scoped lang="scss">
.stat-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  .stat-chip {
    background: var(--bg-secondary);
    padding: 6px 14px;
    border-radius: var(--radius-xl);
    font-size: var(--fs-sm);
    color: var(--text-secondary);
    strong { color: var(--text-primary); font-weight: 500; }
  }
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
