<template>
  <div class="am-empty">
    <div class="am-empty-icon">
      <svg viewBox="0 0 120 120" width="120" height="120">
        <circle cx="60" cy="60" r="50" fill="var(--bg-section)" stroke="var(--border-light)" stroke-width="2" />
        <path d="M60 30 L60 65" stroke="var(--primary-color)" stroke-width="4" stroke-linecap="round" />
        <circle cx="60" cy="78" r="4" fill="var(--primary-color)" />
      </svg>
    </div>
    <div class="am-empty-title">暂无预警记录</div>
    <div class="am-empty-desc">
      {{ hasFilters ? '当前筛选条件下无预警，试试调整筛选条件' : '所有学生学业状态正常，继续保持' }}
    </div>
    <el-button v-if="hasFilters" size="small" @click="emit('clear-filters')">清除筛选</el-button>
  </div>
</template>

<script setup>
defineProps({
  hasFilters: { type: Boolean, default: false }
})
const emit = defineEmits(['clear-filters'])
</script>

<style scoped>
.am-empty { text-align: center; padding: 60px 0; }
.am-empty-icon { margin-bottom: 16px; }
.am-empty-title { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.am-empty-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 16px; }
</style>
