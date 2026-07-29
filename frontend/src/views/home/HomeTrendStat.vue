<template>
  <div class="trend-stat card-hover">
    <div class="trend-stat__label">{{ label }}</div>
    <div class="trend-stat__value" :style="{ color: valueColor }">{{ value }}</div>
    <div v-if="trendValue !== undefined" class="trend-stat__trend" :class="trendDirection">
      <span>{{ trendDirection === 'up' ? '↑' : trendDirection === 'down' ? '↓' : '→' }} {{ trendValue }}</span>
      <span v-if="trendLabel" class="trend-label">{{ trendLabel }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  trendDirection: { type: String, default: 'none', validator: v => ['up', 'down', 'none'].includes(v) },
  trendValue: { type: [String, Number], default: undefined },
  trendLabel: { type: String, default: '' },
  valueColor: { type: String, default: '' }
})
</script>

<style scoped lang="scss">
.trend-stat {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 0.5px solid var(--border-color);
  transition: border-color var(--transition-fast);
  display: flex;
  flex-direction: column;

  &__label {
    font-size: var(--fs-xs);
    color: var(--text-secondary);
    margin-bottom: 4px;
  }

  &__value {
    font-size: 28px;
    font-weight: 500;
    color: var(--text-primary);
    line-height: 1.2;
  }

  &__trend {
    font-size: var(--fs-xs);
    margin-top: 6px;
    display: flex;
    align-items: center;
    gap: 6px;

    &.up {
      color: var(--success-color);
    }
    &.down {
      color: var(--danger-color);
    }
    &.none {
      color: var(--text-secondary);
    }
  }

  .trend-label {
    color: var(--text-secondary);
  }
}

/* 移动端 — 统计卡片缩小 */
@media (max-width: 768px) {
  .trend-stat {
    padding: 14px 16px;

    &__value {
      font-size: 22px;
    }
  }
}
</style>
