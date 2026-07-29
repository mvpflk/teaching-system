<template>
  <div class="stats-bar">
    <div class="stat"><span class="stat-label">速度</span><span class="stat-val">{{ speedWpm }} <small>字/分</small></span></div>
    <div class="stat"><span class="stat-label">正确率</span><span class="stat-val" :class="accuracyClass">{{ accuracy }}%</span></div>
    <div class="stat"><span class="stat-label">进度</span><span class="stat-val">{{ typedLength }}/{{ totalChars }}</span></div>
    <div class="stat"><span class="stat-label">用时</span><span class="stat-val">{{ elapsedDisplay }}</span></div>
    <div class="stat"><span class="stat-label">错误</span><span class="stat-val wrong-val">{{ wrongCount }}</span></div>
  </div>
</template>

<script setup>
defineProps({
  speedWpm: { type: Number, default: 0 },
  accuracy: { type: Number, default: 100 },
  accuracyClass: { type: String, default: '' },
  typedLength: { type: Number, default: 0 },
  totalChars: { type: Number, default: 0 },
  elapsedDisplay: { type: String, default: '0秒' },
  wrongCount: { type: Number, default: 0 }
})
</script>

<style scoped>
.stats-bar { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.stat { text-align: center; padding: 10px 6px; background: var(--typing-stats-bg); border-radius: var(--radius-md); border: 1px solid var(--typing-border); }
.stat-label { display: block; font-size: var(--fs-xs); color: var(--typing-pending); }
.stat-val { display: block; font-size: var(--fs-lg); font-weight: 700; color: var(--typing-text); margin-top: 2px; font-variant-numeric: tabular-nums; }
.stat-val small { font-size: var(--fs-xs); font-weight: 400; }
.wrong-val { color: var(--typing-incorrect); }
@media (max-width: 768px) {
  .stats-bar { grid-template-columns: repeat(3, 1fr); gap: 6px; }
  .stat { padding: 6px 4px; }
  .stat-val { font-size: var(--fs-md); }
}
</style>
