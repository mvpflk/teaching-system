<template>
  <div v-if="generating && !hasResult" class="aia-progress">
    <div class="aia-progress-steps">
      <template v-for="(step, i) in steps" :key="i">
        <div class="aia-step-item">
          <div :class="['aia-step-node', { done: currentStep > i, active: currentStep === i }]">
            <el-icon v-if="currentStep > i" :size="14"><Check /></el-icon>
            <span v-else-if="currentStep === i" class="aia-step-pulse" />
            <span v-else class="aia-step-num">{{ i + 1 }}</span>
          </div>
          <span :class="['aia-step-label', { active: currentStep === i, done: currentStep > i }]">{{
            step
          }}</span>
        </div>
        <div v-if="i < steps.length - 1" :class="['aia-step-line', { done: currentStep > i }]" />
      </template>
    </div>
    <div class="aia-progress-text">{{ progressMessage }}</div>
  </div>
  <div v-if="error && !generating" class="aia-error-wrap">
    <div class="aia-error">
      <el-icon class="aia-error-icon"><WarningFilled /></el-icon>
      <div class="aia-error-body">
        <span class="aia-error-text">{{ error }}</span>
        <span class="aia-error-tip">可尝试减少知识点数量、缩短知识库内容或稍后重试</span>
      </div>
    </div>
    <el-button size="small" type="primary" @click="$emit('retry')">
      <el-icon><Refresh /></el-icon> 重新生成
    </el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { WarningFilled, Refresh, Check } from '@element-plus/icons-vue';

const props = defineProps({
  generating: { type: Boolean, default: false },
  progressMessage: { type: String, default: '' },
  error: { type: String, default: '' },
  hasResult: { type: Boolean, default: false },
});

defineEmits(['retry']);

const steps = ['提交请求', '读取知识库', 'AI 生成', '整理结果'];

const currentStep = computed(() => {
  const msg = props.progressMessage || '';
  if (!msg || msg.includes('提交')) return 0;
  if (msg.includes('读取') || msg.includes('准备')) return 1;
  if (msg.includes('整理') || msg.includes('即将完成') || msg.includes('完成')) return 3;
  return 2;
});
</script>

<style scoped>
.aia-progress {
  margin-top: 16px;
  padding: 20px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
}
.aia-progress-steps {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}
.aia-step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.aia-step-node {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-disabled);
  font-size: var(--fs-xs);
  font-weight: 600;
  transition: all var(--transition-base);
}
.aia-step-node.done {
  border-color: var(--primary-color);
  background: var(--primary-color);
  color: var(--text-on-primary);
}
.aia-step-node.active {
  border-color: var(--primary-color);
  background: var(--primary-light);
  color: var(--primary-color);
}
.aia-step-pulse {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary-color);
  animation: aia-pulse 1.2s ease-in-out infinite;
}
@keyframes aia-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.4);
    opacity: 0.5;
  }
}
.aia-step-line {
  width: 28px;
  height: 2px;
  background: var(--border-color);
  margin-top: 13px;
  transition: background var(--transition-base);
  flex-shrink: 0;
}
.aia-step-line.done {
  background: var(--primary-color);
}
.aia-step-label {
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  transition: color var(--transition-base);
}
.aia-step-label.active {
  color: var(--primary-color);
  font-weight: 600;
}
.aia-step-label.done {
  color: var(--text-secondary);
}
.aia-progress-text {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  text-align: center;
  min-height: 1.5em;
  margin-top: 12px;
}

/* Error */
.aia-error-wrap {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.aia-error {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 14px;
  background: var(--bg-danger-light);
  border-radius: var(--radius-md);
  width: 100%;
}
.aia-error-icon {
  color: var(--el-color-danger);
  font-size: var(--fs-lg);
  flex-shrink: 0;
  margin-top: 1px;
}
.aia-error-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.aia-error-text {
  font-size: var(--fs-sm);
  color: var(--el-color-danger);
  font-weight: 500;
}
.aia-error-tip {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

@media (max-width: 767px) {
  .aia-step-line {
    width: 16px;
  }
  .aia-step-label {
    font-size: 10px;
  }
  .aia-step-node {
    width: 24px;
    height: 24px;
  }
  .aia-step-line {
    margin-top: 11px;
  }
}
</style>
