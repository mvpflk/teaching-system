<template>
  <div v-if="Object.keys(syllabus).length > 0" class="sb-bar">
    <span class="sb-dim">{{ dimLabel }}</span>
    <span class="sb-sep">|</span>
    <span class="sb-level">{{ level }}</span>
    <span class="sb-sep">|</span>
    <span class="sb-exam-tip">常考：{{ examTip }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  syllabus: { type: Object, default: () => ({}) }
})

const dimLabel = computed(() => {
  const map = { THEORY: '应知', PRACTICE: '应会', BOTH: '综合' }
  return map[props.syllabus.dim] || props.syllabus.dim || '应知'
})

const level = computed(() => props.syllabus.level || '了解')
const examTip = computed(() => props.syllabus.examTip || '通用题型')
</script>

<style scoped>
.sb-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 6px 12px;
  background: var(--primary-light);
  border-radius: var(--radius-sm);
  font-size: var(--fs-xs);
  color: var(--primary-color);
  font-weight: 500;
  flex-wrap: wrap;
}
.sb-dim {
  background: var(--primary-color);
  color: #fff;
  padding: 1px 8px;
  border-radius: 999px;
}
.sb-sep { color: var(--text-secondary); }
.sb-exam-tip { color: var(--text-secondary); }
</style>
