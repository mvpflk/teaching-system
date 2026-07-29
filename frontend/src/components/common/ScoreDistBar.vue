<template>
  <div class="dist-bars">
    <div v-for="d in distribution" :key="d.label || d.range" class="dist-item">
      <span class="dist-range">{{ d.label || d.range }}</span>
      <div class="dist-track"><div class="dist-fill" :style="{ width: maxVal > 0 ? (d.count / maxVal * 100) + '%' : '0%' }"></div></div>
      <span class="dist-count">{{ d.count }}{{ suffix }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  distribution: { type: Array, default: () => [] },
  suffix: { type: String, default: '人' }
})

const maxVal = computed(() => {
  if (!props.distribution?.length) return 0
  return Math.max(...props.distribution.map(d => d.count), 1)
})
</script>

<style scoped>
.dist-bars { display: flex; flex-direction: column; gap: 5px; }
.dist-item { display: flex; align-items: center; gap: 6px; font-size: var(--fs-xs); }
.dist-range { width: 50px; text-align: right; color: var(--text-secondary); flex-shrink: 0; }
.dist-track { flex: 1; height: 16px; background: var(--bg-secondary); border-radius: var(--radius-xs); overflow: hidden; }
.dist-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-xs); transition: width 0.5s; min-width: 2px; }
.dist-count { width: 30px; text-align: right; color: var(--text-regular); font-weight: 600; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
