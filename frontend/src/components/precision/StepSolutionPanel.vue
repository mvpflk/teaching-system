<template>
  <div v-if="steps.length" class="step-panel">
    <div class="step-header">📝 解题步骤 ({{ completedCount }}/{{ steps.length }})</div>
    <div
      v-for="(s, i) in steps"
      :key="i"
      class="step-row"
      :class="'step--' + s.status"
    >
      <span class="step-icon">{{ s.status === 'done' ? '✅' : s.status === 'review' ? '❌' : '⬜' }}</span>
      <span class="step-text">{{ s.text }}</span>
      <el-tag size="small" type="info">{{ s.score }}分</el-tag>
    </div>
    <div class="step-summary">总分：{{ steps.filter(s=>s.status==='done').reduce((a,s)=>a+(s.score||0),0) }}/{{ totalScore }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ correctAnswer: String, studentAnswer: { type: String, default: '' }, readonly: { type: Boolean, default: true } })
const steps = computed(() => {
  if (!props.correctAnswer) return []
  const lines = props.correctAnswer.split('\n').filter(l => l.trim())
  return lines.map((line, i) => {
    const match = line.match(/^\((\d+)\)\s*(.+)/)
    const scoreMatch = line.match(/(\d+)分/)
    return { text: match ? match[2] : line, score: scoreMatch ? parseInt(scoreMatch[1]) : 2, status: 'pending' }
  })
})
const completedCount = computed(() => steps.value.filter(s => s.status === 'done').length)
const totalScore = computed(() => steps.value.reduce((a, s) => a + (s.score || 0), 0))
</script>

<style scoped>
.step-panel { border: 1px solid #e4e7ed; border-radius: 8px; padding: 12px; margin-top: 8px; }
.step-header { font-weight: 600; margin-bottom: 8px; font-size: var(--fs-md); }
.step-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: var(--fs-sm); }
.step--pending .step-icon { color: #c0c4cc; }
.step--done .step-icon { color: var(--el-color-success, #67c23a); }
.step--review .step-icon { color: var(--el-color-danger, #f56c6c); }
.step-text { flex: 1; }
.step-summary { margin-top: 8px; font-weight: 600; color: var(--primary-color); font-size: var(--fs-sm); }
</style>
