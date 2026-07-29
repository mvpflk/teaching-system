<template>
  <span v-if="label" class="diff-badge" :class="'diff-' + level">{{ label }}</span>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({
  difficultyLevel: { type: Number, default: 2 },
  tier: { type: String, default: '' },
  outOfSyllabus: { type: Boolean, default: false }
})
const level = computed(() => props.outOfSyllabus ? 'out' : (props.difficultyLevel || 2))
const labels = { 1: '基础', 2: '中等', 3: '进阶', out: '考纲外' }
const label = computed(() => labels[level.value] || '')
</script>
<style scoped>
.diff-badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; line-height: 1.5; }
.diff-1 { background: #dbeafe; color: #1d4ed8; }
.diff-2 { background: #e8f5e9; color: var(--el-color-success); }
.diff-3 { background: #fef3c7; color: #b45309; }
.diff-out { background: #fee2e2; color: #dc2626; }
</style>
