<template>
  <div class="practice-text-panel">
    <div class="ptp-row">
      <span class="ptp-label">难度</span>
      <el-radio-group :model-value="difficulty" size="small" @change="$emit('update:difficulty', $event)">
        <el-radio-button :value="1">简单</el-radio-button>
        <el-radio-button :value="2">普通</el-radio-button>
        <el-radio-button :value="3">中等</el-radio-button>
        <el-radio-button :value="4">较难</el-radio-button>
        <el-radio-button :value="5">困难</el-radio-button>
      </el-radio-group>
      <span class="ptp-label" style="margin-left:20px">语言</span>
      <el-radio-group :model-value="language" size="small" @change="$emit('update:language', $event)">
        <el-radio-button value="zh">中文</el-radio-button>
        <el-radio-button value="en">英文</el-radio-button>
        <el-radio-button value="mixed">混合</el-radio-button>
      </el-radio-group>
      <el-button size="small" type="primary" :loading="textLoading" @click="$emit('random-pick')">随机选题</el-button>
    </div>
    <div class="ptp-preview" v-if="textContent && !hasStarted">
      <span class="ptp-preview-label">当前文本预览：</span>
      <span class="ptp-preview-text">{{ textContent.slice(0, 80) }}{{ textContent.length > 80 ? '…' : '' }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  difficulty: { type: Number, default: 3 },
  language: { type: String, default: 'mixed' },
  textContent: { type: String, default: '' },
  textLoading: { type: Boolean, default: false },
  hasStarted: { type: Boolean, default: false }
})

defineEmits(['update:difficulty', 'update:language', 'random-pick'])
</script>

<style scoped>
.practice-text-panel { background: var(--typing-surface); border: 1px solid var(--typing-border); border-radius: var(--radius-md); padding: 12px 16px; }
.ptp-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.ptp-label { font-size: var(--fs-xs); color: var(--typing-pending); font-weight: 600; white-space: nowrap; }
.ptp-preview { margin-top: 8px; font-size: var(--fs-sm); color: var(--typing-pending); }
.ptp-preview-label { font-weight: 600; color: var(--typing-text); }
.ptp-preview-text { color: var(--typing-correct); }
</style>
