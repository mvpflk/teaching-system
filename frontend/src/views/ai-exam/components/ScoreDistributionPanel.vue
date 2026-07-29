<template>
  <div v-for="seg in distribution" :key="seg.label" class="diag-dist-bar">
    <span class="diag-dist-label">{{ seg.label }}</span>
    <div class="diag-dist-track"><div class="diag-dist-fill" :style="{width:Math.max(seg.pct,2)+'%',background:seg.color}" /></div>
    <span class="diag-dist-count">{{ seg.count }}人 ({{ seg.pct }}%)</span>
  </div>
</template>

<script setup>
defineProps({
  distribution: { type: Array, default: () => [] }
})
</script>

<style scoped>
.diag-dist-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: var(--fs-xs); }
.diag-dist-label { min-width: 44px; color: var(--text-regular); text-align: right; }
.diag-dist-track { flex: 1; height: 14px; background: var(--bg-secondary); border-radius: 7px; overflow: hidden; }
.diag-dist-fill { height: 100%; border-radius: 7px; transition: width 0.4s; }
.diag-dist-count { min-width: 65px; color: var(--text-secondary); }
</style>
