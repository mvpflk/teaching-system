<template>
  <div class="stat-strip">
    <div class="stat-pill" :class="{ 'is-active': modelValue === 'all' }" @click="$emit('update:modelValue', 'all'); $emit('toggle', 'all')">
      <span class="stat-pill-num">{{ overview.studentCount || 0 }}</span>
      <span class="stat-pill-label">参与学生</span>
    </div>
    <div class="stat-pill pill-warn" :class="{ 'is-active': modelValue === 'active' }" @click="$emit('update:modelValue', 'active'); $emit('toggle', 'active')">
      <span class="stat-pill-num">{{ overview.mathActive || 0 }}</span>
      <span class="stat-pill-label">本周数学活跃</span>
    </div>
    <div class="stat-pill" :class="{ 'is-active': modelValue === 'engActive' }" @click="$emit('update:modelValue', 'engActive'); $emit('toggle', 'engActive')">
      <span class="stat-pill-num">{{ overview.englishActive || 0 }}</span>
      <span class="stat-pill-label">本周英语活跃</span>
    </div>
    <div class="stat-pill pill-danger" :class="{ 'is-active': modelValue === 'warning' }" @click="$emit('update:modelValue', 'warning'); $emit('toggle', 'warning')">
      <span class="stat-pill-num">{{ overview.unmasteredCount || 0 }}</span>
      <span class="stat-pill-label">需关注</span>
    </div>
  </div>
</template>

<script setup>
defineProps({ overview: { type: Object, required: true }, modelValue: { type: String, default: 'all' } })
defineEmits(['update:modelValue', 'toggle'])
</script>

<style scoped>
.stat-strip {
  display: flex;
  gap: 12px;
  margin-bottom: var(--spacing-md);
}
.stat-pill {
  flex: 1;
  text-align: center;
  padding: 10px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  min-width: 0;
}
.stat-pill:hover {
  border-color: var(--primary-color);
}
.stat-pill.is-active {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.stat-pill-num {
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}
.stat-pill-label {
  display: block;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 2px;
}
.pill-warn .stat-pill-num {
  color: var(--el-color-warning);
}
.pill-danger .stat-pill-num {
  color: var(--el-color-danger);
}
</style>
