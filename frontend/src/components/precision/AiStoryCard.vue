<template>
  <div v-if="story" class="asc">
    <h4 style="font-size:var(--fs-md);margin:0 0 8px">📖 AI 语境故事</h4>
    <p class="asc-text">{{ story.content }}</p>
    <div v-for="(q, i) in (story.questions || [])" :key="i" class="asc-q">
      <span>{{ i + 1 }}. {{ q.questionText }}</span>
      <el-radio-group v-model="answers[i]" size="small" style="display:flex;gap:8px;margin-top:4px">
        <el-radio v-for="(opt, oi) in (q.options || [])" :key="oi" :value="String.fromCharCode(65 + oi)">{{ opt }}</el-radio>
      </el-radio-group>
    </div>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ story: Object })
const emit = defineEmits(['answer-change'])
const answers = ref({})
// R112修复：story 切换时重置答案，避免跨故事残留
watch(() => props.story, () => { answers.value = {} })
// 暴露答案变更供父组件收集
watch(answers, (v) => emit('answer-change', v), { deep: true })
</script>
<style scoped>
.asc { padding: 14px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; margin-bottom: 14px; }
.asc-text { line-height: 1.8; font-size: var(--fs-md); margin-bottom: 12px; }
.asc-q { margin-top: 10px; font-size: var(--fs-sm); }
</style>
