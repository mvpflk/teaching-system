<template>
  <div v-if="suggestion" class="ai-suggestion-card" :class="{ 'ai-sug-low-confidence': isLowConfidence }">
    <div class="ai-sug-header">
      <el-tag type="warning" size="small">AI 建议</el-tag>
      <span class="ai-sug-score">建议分数: <b>{{ suggestion.score || suggestion.suggestedScore }}</b></span>
      <span v-if="suggestion.confidence !== undefined" class="ai-sug-conf" :class="{ 'ai-sug-conf-low': isLowConfidence }">置信度: {{ (suggestion.confidence * 100).toFixed(0) }}%</span>
    </div>
    <div v-if="isLowConfidence" class="ai-sug-warning">
      <el-icon><WarningFilled /></el-icon>
      <span>建议教师复核 — AI 对此答案的评分置信度较低，请人工确认后提交</span>
    </div>
    <div class="ai-sug-body">{{ suggestion.comment || suggestion.comments }}</div>
    <div v-if="suggestion.explanation" class="ai-sug-explanation">
      <el-collapse>
        <el-collapse-item title="评分理由">{{ suggestion.explanation }}</el-collapse-item>
      </el-collapse>
    </div>
    <div class="ai-sug-actions">
      <el-button size="small" type="primary" @click="$emit('apply')">应用</el-button>
      <el-button size="small" @click="$emit('ignore')">忽略</el-button>
    </div>
  </div>
</template>

<script setup>
import { WarningFilled } from '@element-plus/icons-vue'

defineProps({
  suggestion: { type: Object, default: null },
  isLowConfidence: { type: Boolean, default: false },
})

defineEmits(['apply', 'ignore'])
</script>

<style scoped>
.ai-suggestion-card {
  margin-top: 8px; background: var(--bg-section);
  border: 1px solid var(--border-light); border-radius: var(--radius-md);
  padding: 12px 14px; transition: border-color 0.3s, background 0.3s, box-shadow 0.3s;
}
.ai-sug-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.ai-sug-score { font-size: var(--fs-sm); }
.ai-sug-conf { font-size: var(--fs-xs); color: var(--text-secondary); }
.ai-sug-body {
  font-size: var(--fs-sm); color: var(--text-primary); line-height: 1.7;
  background: var(--bg-card); padding: 8px 10px; border-radius: var(--radius-sm);
  white-space: pre-wrap; word-break: break-word;
}
.ai-sug-explanation { margin-top: 8px; }
.ai-sug-actions { display: flex; gap: 8px; margin-top: 10px; justify-content: flex-end; }
.ai-sug-low-confidence {
  border: 2px solid var(--el-color-danger) !important;
  background: var(--bg-danger-light) !important;
  animation: ai-sug-pulse 2s ease-in-out infinite;
}
@keyframes ai-sug-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.3); }
  50% { box-shadow: 0 0 0 8px rgba(211, 47, 47, 0.05); }
}
.ai-sug-warning {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; margin-bottom: 10px;
  background: var(--bg-danger-light); border: 1.5px solid var(--el-color-danger);
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm); font-weight: 600; color: var(--el-color-danger);
  line-height: 1.6;
}
.ai-sug-warning .el-icon { font-size: var(--fs-lg); flex-shrink: 0; animation: icon-shake 0.6s ease-in-out; }
.ai-sug-conf-low {
  color: var(--el-color-danger); font-weight: 700;
  background: var(--bg-danger-light); padding: 2px 8px; border-radius: 10px;
  border: 1px solid rgba(211, 47, 47, 0.3);
}
</style>
