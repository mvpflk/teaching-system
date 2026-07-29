<template>
  <div class="api-status-bar">
    <div class="status-left">
      <span class="status-dot" :class="status" />
      <span class="model-name">{{ statusText }}</span>
      <span v-if="status === 'connected'" class="status-separator">·</span>
      <span v-if="status === 'connected' && hasCustomKey" class="usage-badge">自有 Key</span>
      <span v-else-if="status === 'connected'" class="usage-badge">
        {{ dailyUsage }}/{{ dailyLimit }} 次
      </span>
    </div>
    <div class="status-right">
      <el-button text size="small" @click="goToConfig">
        <el-icon><Setting /></el-icon> 模型配置
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Setting } from '@element-plus/icons-vue';

const props = defineProps({
  modelName: { type: String, default: 'DeepSeek V4 Pro' },
  status: { type: String, default: 'connected' },
  dailyUsage: { type: Number, default: 0 },
  dailyLimit: { type: Number, default: 20 },
  hasCustomKey: { type: Boolean, default: false },
});

const router = useRouter();

const statusText = computed(() => {
  switch (props.status) {
    case 'connected':
      return props.modelName;
    case 'error':
      return 'API 连接异常';
    case 'unconfigured':
      return '未配置 API Key';
    default:
      console.warn(`[ApiStatusBar] Unknown status: ${props.status}`);
      return props.modelName;
  }
});

function goToConfig() {
  router.push('/my/api-keys');
}
</script>

<style scoped>
.api-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 16px;
  border-top: 0.5px solid var(--border-light);
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--bg-page);
}

.status-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.connected {
  background: var(--el-color-success);
}

.status-dot.error {
  background: var(--el-color-danger);
}

.status-dot.unconfigured {
  background: var(--text-disabled);
}

.model-name {
  font-size: 11px;
  color: var(--text-secondary);
}

.usage-badge {
  font-size: 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-xs);
  padding: 1px 6px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.status-separator {
  margin: 0 2px;
  color: var(--text-disabled);
}
</style>
