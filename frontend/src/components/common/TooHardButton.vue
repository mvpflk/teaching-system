<template>
  <div class="too-hard-wrapper">
    <button class="too-hard-btn" @click="showHint">太难了？</button>
    <el-dialog
      v-model="hintVisible"
      title="解题提示"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="hint-body" v-html="renderMarkdown(hintText)"></div>
      <template #footer>
        <el-button @click="hintVisible = false; $emit('retry')">我再试试</el-button>
        <el-button type="warning" @click="confirmSkip">还是跳过</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
const props = defineProps({ knowledgeSummary: { type: String, default: '' }, questionId: { type: Number, required: true } })
const emit = defineEmits(['skip', 'retry'])
const hintVisible = ref(false)
const hintText = computed(() => props.knowledgeSummary || '这道题涉及的知识点可能较难。建议回顾相关章节后再尝试，或者可以先跳过，后续在学习包中针对性练习。')
const showHint = () => { hintVisible.value = true }
const confirmSkip = () => {
  hintVisible.value = false
  try { import('@/api/questionSkipLog').then(api => { api.logSkip({ questionId: props.questionId, reason: 'TOO_HARD' }) }) } catch {}
  emit('skip')
}
</script>
<style scoped>
.too-hard-btn { padding: 4px 12px; background: transparent; border: 1px solid var(--el-border-color); border-radius: 6px; color: #9ca3af; font-size: 0.8rem; cursor: pointer; transition: all 0.15s; }
.too-hard-btn:hover { color: #f59e0b; border-color: #f59e0b; }
.hint-body { font-size: 0.9rem; line-height: 1.8; color: var(--el-text-color-regular); }
</style>
