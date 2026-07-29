<template>
  <el-card v-if="aiText" shadow="never" class="diag-card" style="margin-top:16px">
    <template #header>
      <div class="diag-card-head">
        <span class="diag-card-title">🤖 AI 学情分析</span>
        <el-button text size="small" type="primary" :loading="aiRunning" @click="$emit('trigger-analysis')">
          重新生成
        </el-button>
      </div>
    </template>
    <div class="diag-ai-content markdown-body" v-html="renderMd(aiText)"></div>
  </el-card>
  <el-collapse v-else-if="!aiRunning" v-model="collapseActive" style="margin-top:16px">
    <el-collapse-item title="🤖 AI 学情分析" name="ai">
      <div style="text-align:center;padding:10px 0">
        <el-icon :size="28" color="var(--el-text-color-placeholder)"><MagicStick /></el-icon>
        <p style="color:var(--text-secondary, #909399);margin:6px 0 12px">点击生成 AI 深度分析（班级总结 + 薄弱点建议 + 学生关怀）</p>
        <el-button type="primary" :loading="aiRunning" @click="$emit('trigger-analysis')">
          <el-icon><MagicStick /></el-icon> AI 分析
        </el-button>
      </div>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup>
import { ref } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'
const renderMd = t => renderMarkdown(t || '')
const collapseActive = ref([])
defineProps({
  aiText: { type: String, default: '' },
  aiRunning: { type: Boolean, default: false }
})
defineEmits(['trigger-analysis'])
</script>

<style scoped>
.diag-card { margin-bottom: 0; }
.diag-card-title { font-size: var(--fs-md); font-weight: 700; }
.diag-card-head { display: flex; align-items: center; justify-content: space-between; }
.diag-ai-content { font-size: var(--fs-md); line-height: 1.8; color: var(--text-primary); padding: 8px 0; }
.diag-ai-content :deep(h2) { font-size: var(--fs-lg); margin: 12px 0 6px; }
.diag-ai-content :deep(p) { margin: 4px 0; }
.diag-ai-content :deep(li) { margin: 2px 0; }
</style>
