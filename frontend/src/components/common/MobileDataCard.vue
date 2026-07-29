<template>
  <div v-if="loading" class="mobile-data-card md-card--loading">
    <div class="md-card__header">
      <div v-if="icon" class="md-card__icon-sk" />
      <div class="md-card__body">
        <div class="sk-line w-60" />
        <div class="sk-line w-40" style="margin-top: 4px" />
      </div>
      <div class="md-card__badge-sk" />
    </div>
    <div class="md-card__meta"><div class="sk-line w-30" /></div>
    <div class="md-card__footer"><div class="sk-line w-20" /></div>
  </div>
  <div
    v-else
    class="mobile-data-card"
    :class="[`md-card--${variant}`, { 'md-card--clickable': clickable }]"
    :style="borderLeftColor ? { borderLeftColor } : {}"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    :aria-label="title"
    @click="clickable && $emit('click')"
    @keydown.enter="clickable && $emit('click')"
    @keydown.space.prevent="clickable && $emit('click')"
  >
    <div class="md-card__header">
      <div v-if="icon" class="md-card__icon">{{ icon }}</div>
      <div class="md-card__body">
        <div class="md-card__title">{{ title }}</div>
        <div v-if="subtitle" class="md-card__subtitle">{{ subtitle }}</div>
      </div>
      <div v-if="badge" class="md-card__badge">
        <el-tag :type="badge.type" size="small">{{ badge.text }}</el-tag>
      </div>
    </div>
    <div v-if="$slots.meta || metaItems.length" class="md-card__meta">
      <slot name="meta">
        <span v-for="(m, i) in metaItems" :key="i" class="md-card__meta-item">{{ m }}</span>
      </slot>
    </div>
    <div v-if="$slots.footer || $slots.actions" class="md-card__footer">
      <div class="md-card__footer-info">
        <slot name="footer" />
      </div>
      <div v-if="$slots.actions" class="md-card__actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' },
  badge: { type: Object, default: null },
  variant: { type: String, default: 'default' },
  borderLeftColor: { type: String, default: '' },
  clickable: { type: Boolean, default: true },
  metaItems: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
});
defineEmits(['click']);
</script>

<style scoped>
.mobile-data-card {
  padding: 14px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  transition: all 0.15s;
  margin-bottom: 8px;
}
.md-card--clickable {
  cursor: pointer;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}
.md-card--clickable:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.md-card--clickable:active {
  background: var(--bg-secondary);
  transform: scale(0.98);
  transition: transform 0.1s;
}
.md-card--warning {
  border-left: 3px solid var(--el-color-warning);
}
.md-card--success {
  border-left: 3px solid var(--el-color-success);
}
.md-card--danger {
  border-left: 3px solid var(--el-color-danger);
}
.md-card--muted {
  border-left: 3px solid var(--border-color);
  opacity: 0.85;
}

.md-card__header {
  display: flex;
  gap: 10px;
  margin-bottom: 6px;
  align-items: flex-start;
}
.md-card__icon {
  font-size: 22px;
  flex-shrink: 0;
  line-height: 1.4;
}
.md-card__body {
  flex: 1;
  min-width: 0;
}
.md-card__title {
  font-size: var(--fs-base, 14px);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
}
.md-card__subtitle {
  font-size: var(--fs-xs, 11px);
  color: var(--text-secondary);
  margin-top: 2px;
}
.md-card__badge {
  flex-shrink: 0;
}

.md-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.md-card__meta-item {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

.md-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.md-card__footer-info {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.md-card__actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.md-card__actions :deep(.el-button) {
  font-size: var(--fs-xs);
  /* 触控目标遵循全局 44px 规范，仅通过 padding 收紧横向占用 */
  min-height: 40px;
  padding: 0 12px;
}

/* 骨架屏 */
.md-card--loading {
  pointer-events: none;
}
.sk-line {
  height: 14px;
  background: var(--bg-secondary);
  border-radius: var(--radius-xs);
  position: relative;
  overflow: hidden;
}
.sk-line::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: sk-shimmer 1.6s infinite;
}
@keyframes sk-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
.w-60 {
  width: 60%;
}
.w-40 {
  width: 40%;
}
.w-30 {
  width: 30%;
}
.w-20 {
  width: 20%;
}
.md-card__icon-sk {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-xs);
  background: var(--bg-secondary);
  flex-shrink: 0;
}
.md-card__badge-sk {
  width: 40px;
  height: 20px;
  border-radius: 10px;
  background: var(--bg-secondary);
  flex-shrink: 0;
}
</style>
