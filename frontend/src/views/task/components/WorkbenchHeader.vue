<template>
  <div class="wb-header">
    <el-page-header class="mb-16" @back="$emit('back')">
      <template #content>
        <span class="wb-title"><TaskIcon type="IN_CLASS" :size="22" /> 批阅工作台</span>
        <el-tag size="small" style="margin-left:8px">{{ task.title }}</el-tag>
      </template>
    </el-page-header>
    <div class="wb-stats">
      <span>已批 {{ gradedCount }} / {{ totalCount }}</span>
      <el-progress :percentage="progressPct" :stroke-width="8" style="width:200px;margin-left:12px" />
    </div>
    <div class="wb-shortcuts">
      <span class="kbd"><kbd>&larr;</kbd><kbd>&rarr;</kbd> 切换学生</span>
      <span class="kbd"><kbd>Ctrl+Enter</kbd> 保存</span>
      <span class="kbd"><kbd>Esc</kbd> 跳过</span>
    </div>
  </div>
</template>

<script setup>
import TaskIcon from '@/components/common/TaskIcon.vue'

defineProps({
  task: { type: Object, required: true },
  gradedCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  progressPct: { type: Number, default: 0 },
})

defineEmits(['back'])
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.wb-header { margin-bottom: 12px; }
.wb-title { font-size: var(--fs-lg); font-weight: 600; }
.wb-stats { display: flex; align-items: center; font-size: var(--fs-sm); color: var(--text-secondary); }
.wb-shortcuts {
  display: flex; gap: 16px; padding: 8px 12px; margin-top: 8px;
  background: var(--bg-section); border-radius: var(--radius-sm);
  font-size: var(--fs-sm); color: var(--text-secondary);
}
.wb-shortcuts .kbd { display: inline-flex; align-items: center; gap: 4px; }
.wb-shortcuts kbd {
  display: inline-block; padding: 2px 7px; font-size: var(--fs-xs); font-family: monospace;
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 3px;
  color: var(--text-primary);
}
@media (max-width: 768px) {
  .wb-shortcuts { display: none; }
}
</style>
