<template>
  <div class="step-text">
    <div class="step-desc" v-if="step.description" v-html="renderedDesc"></div>
    <el-input
      :model-value="modelValue?.content || ''"
      @update:model-value="emit('update:modelValue', { ...modelValue, content: $event })"
      type="textarea"
      :rows="10"
      :placeholder="placeholder"
      @change="emit('saved')"
    />
    <div class="word-count" v-if="step.config?.minWords">
      {{ (modelValue?.content || '').length }} / {{ step.config.minWords }} 字
      <span v-if="(modelValue?.content || '').length < step.config.minWords" class="insufficient">（未达标）</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  step: { type: Object, default: () => ({}) },
  stepIndex: { type: Number, default: 0 },
  taskId: { type: Number, default: 0 },
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const renderedDesc = computed(() => renderMarkdown(props.step.description || ''))
const placeholder = computed(() => {
  return '请输入...' + (props.step.config?.minWords ? `（不少于${props.step.config.minWords}字）` : '')
})
</script>

<style scoped>
.step-text { display: flex; flex-direction: column; gap: 8px; }
.step-desc { color: var(--text-secondary); font-size: var(--fs-sm); line-height: 1.6; }
.word-count { font-size: var(--fs-xs); color: var(--text-secondary); }
.insufficient { color: var(--color-warning, #e6a23c); }
</style>
