<template>
  <div class="step-choice">
    <div class="step-desc" v-if="step.description" v-html="renderedDesc"></div>
    <div v-if="questions.length === 0" class="empty-questions">
      <el-empty description="该步骤暂无题目" />
    </div>
    <div v-for="(q, qi) in questions" :key="qi" class="choice-question">
      <div class="q-stem">{{ qi + 1 }}. {{ q.stem }}</div>
      <el-radio-group
        v-if="q.type !== 'MULTI_CHOICE'"
        :model-value="answers[qi]"
        @update:model-value="(v) => { answers[qi] = v; onAnswerChange() }"
      >
        <el-radio v-for="(opt, oi) in q.options" :key="oi" :value="String.fromCharCode(65 + oi)">
          {{ String.fromCharCode(65 + oi) }}. {{ typeof opt === 'string' ? opt : opt.text }}
        </el-radio>
      </el-radio-group>
      <el-checkbox-group
        v-else
        :model-value="answers[qi] || []"
        @update:model-value="(v) => { answers[qi] = v; onAnswerChange() }"
      >
        <el-checkbox v-for="(opt, oi) in q.options" :key="oi" :label="String.fromCharCode(65 + oi)">
          {{ String.fromCharCode(65 + oi) }}. {{ typeof opt === 'string' ? opt : opt.text }}
        </el-checkbox>
      </el-checkbox-group>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  step: { type: Object, default: () => ({}) },
  stepIndex: { type: Number, default: 0 },
  taskId: { type: Number, default: 0 },
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const questions = computed(() => {
  const qs = props.step.config?.questions || []
  return qs
})
const answers = ref(props.modelValue?.answers || {})

function onAnswerChange() {
  emit('update:modelValue', { ...props.modelValue, answers: { ...answers.value } })
  emit('saved')
}

const renderedDesc = computed(() => renderMarkdown(props.step.description || ''))
</script>

<style scoped>
.step-choice { display: flex; flex-direction: column; gap: 8px; }
.step-desc { color: var(--text-secondary); font-size: var(--fs-sm); line-height: 1.6; margin-bottom: 8px; }
.choice-question { margin-bottom: 16px; padding: 12px; background: var(--bg-card); border-radius: 8px; }
.q-stem { font-weight: 500; margin-bottom: 8px; line-height: 1.5; }
.empty-questions { padding: 24px 0; }
</style>
