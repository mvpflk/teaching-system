<template>
  <div class="vdi">
    <el-tag size="small" effect="plain" :type="direction === 'cn2en' ? 'warning' : 'info'">{{ direction === 'cn2en' ? '中译英' : '英译中' }}</el-tag>
    <p v-if="direction === 'cn2en'" class="vdi-word">
      {{ question?.meaning || '⚠️ 释义缺失' }}
      <span v-if="!question?.meaning" style="font-size:var(--fs-xs);color:#999;display:block">请告诉老师这个单词需要补充释义</span>
    </p>
    <p v-else class="vdi-word">{{ question?.word || '' }}</p>
    <el-input
      :model-value="modelValue"
      placeholder="输入答案..."
      size="large"
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="$emit('submit', modelValue)"
    />
    <div v-if="hint" class="vdi-hint">{{ hint }}</div>
    <div v-if="feedback" class="vdi-fb" :class="{ correct: feedback.correct }">
      <p v-if="feedback.correct">✅ {{ feedback.message || '回答正确！' }}</p>
      <div v-else>
        <p><strong>正确答案：</strong>{{ feedback.correctAnswer || '（请查看解析）' }}</p>
        <p v-if="feedback.explanation" class="vdi-explanation">{{ feedback.explanation }}</p>
      </div>
    </div>
    <slot />
  </div>
</template>
<script setup>
defineProps({ question: Object, hint: String, hintLevel: Number, feedback: Object, direction: String, modelValue: String })
defineEmits(['update:modelValue', 'submit'])
</script>
<style scoped>
.vdi { padding: 20px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; }
.vdi-word { font-size: var(--fs-2xl); font-weight: 700; text-align: center; margin: 14px 0; color: var(--text-primary, var(--text-primary)); }
.vdi-hint { padding: 10px; background: #fff8e1; border-radius: 4px; font-size: var(--fs-sm); margin: 10px 0; color: var(--el-color-warning); }
.vdi-fb { padding: 10px; border-radius: 4px; margin: 10px 0; background: #fff2f0; }
.vdi-fb.correct { background: #f6ffed; }
.vdi-explanation { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); margin-top: 6px; padding-top: 6px; border-top: 1px solid var(--border-light, #eee); }
</style>
