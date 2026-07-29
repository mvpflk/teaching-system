<template>
  <div class="ss-topbar">
    <el-button text class="ss-back" @click="$emit('back')">
      <el-icon><ArrowLeft /></el-icon>返回班级
    </el-button>
    <span class="ss-title">{{ className }} · 智慧课堂</span>
    <div class="ss-conn-status" :class="connStatusClass" :title="connStatusTitle">
      <span class="ss-conn-dot"></span>
      <span class="ss-conn-text">{{ connStatusLabel }}</span>
      <span v-if="onlineCount > 0" class="ss-conn-online">
        <el-icon><UserFilled /></el-icon>{{ onlineCount }}人在线
      </span>
      <el-button
        v-if="showReconnect"
        text
        size="small"
        type="warning"
        @click="$emit('reconnect')"
      >
        手动重连
      </el-button>
    </div>
    <div class="ss-topbar-right">
      <span class="ss-clock">{{ now }}</span>
      <el-radio-group
        :model-value="sceneMode"
        size="small"
        class="ss-mode-toggle"
        @change="$emit('update:sceneMode', $event)"
      >
        <el-radio-button value="LAB">微机室</el-radio-button>
        <el-radio-button value="CLASSROOM">教室</el-radio-button>
      </el-radio-group>
      <el-button size="small" class="ss-action-btn" @click="$emit('openAnalytics')">
        <el-icon><DataAnalysis /></el-icon>数据
      </el-button>
      <el-button size="small" class="ss-action-btn" @click="$emit('openTask')">
        <el-icon><List /></el-icon>任务
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft, UserFilled, DataAnalysis, List } from '@element-plus/icons-vue';

defineProps({
  sceneMode: { type: String, required: true },
  connStatusClass: { type: String, required: true },
  onlineCount: { type: Number, default: 0 },
  className: { type: String, default: '' },
  now: { type: String, default: '' },
  connStatusLabel: { type: String, required: true },
  connStatusTitle: { type: String, default: '' },
  showReconnect: { type: Boolean, default: false },
});

defineEmits(['update:sceneMode', 'reconnect', 'back', 'openAnalytics', 'openTask']);
</script>

<style scoped lang="scss">
.ss-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 28px;
  background: var(--bg-card);
  border-bottom: 0.5px solid var(--border-color);
  z-index: 20;
  min-height: 52px;
  flex-shrink: 0;
}

.ss-back {
  font-size: var(--fs-md);
  flex-shrink: 0;
}

.ss-title {
  flex: 1;
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.02em;
}

.ss-topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.ss-conn-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: var(--fs-xs);
  font-weight: 500;
  flex-shrink: 0;
  transition: all 0.3s ease;

  &.conn-ok {
    background: var(--el-color-success-light-9);
    color: var(--el-color-success);
  }
  &.conn-busy {
    background: var(--el-color-warning-light-9);
    color: var(--el-color-warning);
  }
  &.conn-err {
    background: var(--el-color-danger-light-9);
    color: var(--el-color-danger);
  }
  &.conn-off {
    background: var(--bg-secondary);
    color: var(--text-disabled);
  }
}

.ss-conn-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  .conn-ok & {
    background: var(--el-color-success);
  }
  .conn-busy & {
    background: var(--el-color-warning);
    animation: pulse-dot 1s infinite;
  }
  .conn-err & {
    background: var(--el-color-danger);
  }
  .conn-off & {
    background: var(--text-disabled);
  }
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.ss-conn-text {
  white-space: nowrap;
}

.ss-conn-online {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: var(--fs-xs);
  opacity: 0.85;
  white-space: nowrap;
}

.ss-clock {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-secondary);
  font-family: 'JetBrains Mono', 'SF Mono', 'Consolas', monospace;
  letter-spacing: 0.04em;
  min-width: 48px;
  text-align: right;
}

.ss-mode-toggle {
  :deep(.el-radio-button__inner) {
    padding: 6px 18px;
    font-size: var(--fs-md);
  }
}

.ss-action-btn {
  font-size: var(--fs-md);
  min-height: 40px;
}

@media (max-width: 900px) {
  .ss-topbar {
    padding: 10px 16px;
    gap: 10px;
  }
  .ss-title {
    font-size: var(--fs-lg);
  }
  .ss-clock {
    display: none;
  }
}
</style>
