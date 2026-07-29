<template>
  <div v-if="visible" class="ai-status" :class="[`ai-status--${variant}`]">
    <!-- Loading -->
    <template v-if="variant === 'loading'">
      <div class="ai-status__spinner">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>
      <div class="ai-status__text">
        <slot name="loading">{{ loadingText }}</slot>
      </div>
    </template>

    <!-- Error -->
    <template v-else-if="variant === 'error'">
      <div class="ai-status__icon">
        <el-icon :size="24" color="var(--el-color-danger)"><WarningFilled /></el-icon>
      </div>
      <div class="ai-status__text">
        <slot name="error">{{ errorText }}</slot>
      </div>
      <el-button
        v-if="showRetry"
        size="small"
        type="primary"
        plain
        @click="$emit('retry')"
      >
        重试
      </el-button>
    </template>

    <!-- Low Confidence -->
    <template v-else-if="variant === 'low-confidence'">
      <div class="ai-status__banner ai-status__banner--warning">
        <el-icon :size="16" color="var(--el-color-warning)"><WarningFilled /></el-icon>
        <span class="ai-status__banner-text">
          <slot name="confidence">{{ confidenceText }}</slot>
        </span>
      </div>
    </template>

    <!-- Success (brief confirmation) -->
    <template v-else-if="variant === 'success'">
      <div class="ai-status__banner ai-status__banner--success">
        <el-icon :size="16" color="var(--el-color-success)"><CircleCheckFilled /></el-icon>
        <span class="ai-status__banner-text">
          <slot name="success">{{ successText }}</slot>
        </span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { Loading, WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue';

const props = defineProps({
  /** 'loading' | 'error' | 'low-confidence' | 'success' | 'hidden' */
  variant: { type: String, default: 'hidden' },
  loadingText: { type: String, default: 'AI 正在生成内容，请稍候…' },
  errorText: { type: String, default: 'AI 服务暂时不可用，请稍后重试' },
  confidenceText: { type: String, default: 'AI 评分的置信度较低，建议人工复核' },
  successText: { type: String, default: 'AI 内容已生成' },
  showRetry: { type: Boolean, default: true },
});

defineEmits(['retry']);

const visible = computed(() => props.variant !== 'hidden');
</script>

<style scoped>
.ai-status {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  margin: 8px 0;
  font-size: var(--fs-sm, 14px);
  transition: all 0.3s;
}

.ai-status--loading {
  background: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary);
  justify-content: center;
}

.ai-status--error {
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger);
  justify-content: center;
}

.ai-status__spinner {
  display: flex;
  align-items: center;
}

.ai-status__text {
  flex: 1;
  line-height: 1.5;
}

.ai-status__banner {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  width: 100%;
}

.ai-status__banner--warning {
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #e6a23c);
}

.ai-status__banner--success {
  background: var(--el-color-success-light-9, #f0f9eb);
  color: var(--el-color-success-dark-2, #67c23a);
}

.ai-status__banner-text {
  font-size: var(--fs-xs, 13px);
}
</style>
