<template>
  <div class="ps-wrap" :class="{ 'ps-comparison': isComparison }">
    <div v-if="isComparison" class="ps-comparison-badge">🔍 考点对比题 · 区分易混淆概念</div>
    <div class="ps-scene">{{ question.scene }}</div>
    <div v-if="isComparison && question.compareConcepts" class="ps-compare-tags">
      <span v-for="(c, i) in question.compareConcepts" :key="i" class="ps-compare-tag">{{ c }}</span>
    </div>
    <div class="ps-options">
      <div
        v-for="(opt, i) in options"
        :key="i"
        class="ps-opt"
        :class="{
          'ps-selected': selected === label(i),
          'ps-correct': showResult && label(i) === correctLabel,
          'ps-wrong': showResult && selected === label(i) && label(i) !== correctLabel,
          'ps-trap': showResult && label(i) === question.trapOption && question.trapOption !== correctLabel
        }"
        @click="selectOption(label(i))"
      >
        <span class="ps-label">{{ label(i) }}</span>
        <span class="ps-text">{{ opt }}</span>
      </div>
    </div>
    <div v-if="showResult" class="ps-result">
      <div class="ps-analysis">{{ question.analysis || '无解析' }}</div>
      <div v-if="isComparison && question.trapReason" class="ps-trap-notice">
        💡 {{ question.trapReason }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  question: { type: Object, default: () => ({}) },
  showResult: { type: Boolean, default: false }
})

const emit = defineEmits(['answer'])

const selected = ref('')

const isComparison = computed(() => props.question.questionType === 'COMPARISON')

const options = computed(() => {
  if (props.question.options && Array.isArray(props.question.options)) return props.question.options
  return []
})

const correctLabel = computed(() => props.question.answer || '')

function label(i) { return String.fromCharCode(65 + i) }

function selectOption(l) {
  if (props.showResult) return
  selected.value = l
  emit('answer', { answer: l })
}
</script>

<style scoped>
.ps-wrap {
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}
.ps-comparison {
  border-left: 3px solid #f39c12;
  background: rgba(243, 156, 18, 0.02);
}
.ps-comparison-badge {
  font-size: var(--fs-xs);
  color: #f39c12;
  margin-bottom: 8px;
  font-weight: 500;
}
.ps-scene {
  font-size: var(--fs-base); color: var(--text-primary);
  margin-bottom: var(--spacing-md); padding: 10px 12px;
  background: var(--bg-section); border-radius: var(--radius-sm);
  line-height: 1.6;
}
.ps-compare-tags {
  display: flex; gap: 6px; margin-bottom: var(--spacing-md); flex-wrap: wrap;
}
.ps-compare-tag {
  padding: 2px 10px; border-radius: 999px;
  font-size: var(--fs-xs); font-weight: 500;
  background: rgba(243, 156, 18, 0.08); color: #f39c12;
}
.ps-options { display: flex; flex-direction: column; gap: 8px; }
.ps-opt {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 10px 12px; border-radius: var(--radius-sm);
  border: 0.5px solid var(--border-color); cursor: pointer;
  transition: background var(--transition-base);
}
.ps-opt:hover:not(.ps-correct):not(.ps-wrong) { background: var(--bg-section); }
.ps-selected { border-color: var(--primary-color); background: var(--primary-light); }
.ps-correct { border-color: #00b42a; background: rgba(0, 180, 42, 0.06); }
.ps-wrong { border-color: #e74c3c; background: rgba(231, 76, 60, 0.04); }
.ps-trap {
  border-color: #f39c12; border-style: dashed;
  background: rgba(243, 156, 18, 0.03);
}
.ps-label {
  font-weight: 600; font-size: var(--fs-sm); color: var(--text-secondary);
  min-width: 20px;
}
.ps-text { font-size: var(--fs-base); color: var(--text-primary); }
.ps-result { margin-top: var(--spacing-md); padding: 10px; background: var(--bg-section); border-radius: var(--radius-sm); }
.ps-analysis { font-size: var(--fs-sm); color: var(--text-regular); line-height: 1.6; }
.ps-trap-notice { margin-top: 8px; font-size: var(--fs-xs); color: #f39c12; line-height: 1.5; }
</style>
