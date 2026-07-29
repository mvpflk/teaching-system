<template>
  <div class="gdi">
    <el-tag size="small" effect="plain" type="warning">语法 · {{ question?.grammarNodeName || '' }}</el-tag>
    <p class="gdi-q">{{ question?.questionText || '' }}</p>
    <div
      v-if="question?.options"
      ref="optsEl"
      class="gdi-opts"
      tabindex="0"
      @keydown="onKey"
    >
      <div
        v-for="(opt, oi) in question.options"
        :key="oi"
        class="gdi-opt"
        :class="{ selected: modelValue === String.fromCharCode(65 + oi) }"
        @click="$emit('update:modelValue', String.fromCharCode(65 + oi))"
      >
        <span class="gdi-letter">{{ String.fromCharCode(65 + oi) }}</span><span>{{ opt.replace(/^[A-D][.、．]\s*/, '') }}</span>
      </div>
    </div>
    <div v-if="hint" class="gdi-hint">{{ hint }}</div>
    <div v-if="feedback" class="gdi-fb" :class="{ correct: feedback.correct }">
      <p v-if="feedback.correct">✅ {{ feedback.message || '回答正确！' }}</p>
      <div v-else>
        <p><strong>正确答案：</strong>{{ feedback.correctAnswer || '（请查看解析）' }}</p>
        <p v-if="feedback.explanation" class="gdi-explanation">{{ feedback.explanation }}</p>
      </div>
    </div>
    <slot />
  </div>
</template>
<script setup>
import { ref } from 'vue'
const props = defineProps({ question: Object, hint: String, hintLevel: Number, feedback: Object, modelValue: String })
const emit = defineEmits(['update:modelValue', 'submit'])
const optsEl = ref(null)

function onKey(e) {
  const idx = 'ABCD'.indexOf(e.key.toUpperCase())
  if (idx >= 0 && idx < (props.question?.options?.length || 0)) {
    emit('update:modelValue', String.fromCharCode(65 + idx))
  }
  if (e.key === 'Enter') emit('submit', props.modelValue)
}
</script>
<style scoped>
.gdi { padding: 20px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; }
.gdi-q { font-size: var(--fs-lg); line-height: 1.7; margin: 12px 0; }
.gdi-opts { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; }
.gdi-opt { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border: 1px solid var(--border-base, #e8e8ed); border-radius: 4px; cursor: pointer; }
.gdi-opt:hover { background: var(--bg-hover, var(--bg-hover)); }
.gdi-opt.selected { border-color: var(--primary-color); background: var(--primary-light); }
.gdi-letter { width: 22px; font-weight: 700; color: var(--primary-color); }
.gdi-hint { padding: 10px; background: #fff8e1; border-radius: 4px; font-size: var(--fs-sm); margin: 10px 0; color: var(--el-color-warning); }
.gdi-fb { padding: 10px; border-radius: 4px; margin: 10px 0; background: #fff2f0; }
.gdi-fb.correct { background: #f6ffed; }
.gdi-explanation { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin-top: 6px; padding-top: 6px; border-top: 1px solid var(--border-light, #eee); }
</style>
