<template>
  <div class="esb" :class="'esb-stage-' + stage">
    <div
      v-for="s in 7"
      :key="s"
      class="esb-item"
      :class="{ done: s < stage, current: s === stage, locked: s > stage }"
    >
      <span class="esb-icon">{{ s <= stage ? '⭐' : '🔒' }}</span>
      <span class="esb-name">{{ names[s] }}</span>
      <div v-if="s <= stage" class="esb-fill" :style="{width: s < stage ? '100%' : (normalizeProgress(progress?.vocab) || 0) + '%'}"></div>
    </div>
  </div>
</template>
<script setup>
defineProps({ stage: { type: Number, default: 1 }, progress: { type: Object, default: () => ({}) } })
const names = ['', '生存词汇', '核心时态', '词汇扩展', '语法基础', '语法进阶', '阅读突破', '综合精通']
// R112修复：兼容0-1和0-100两种进度格式
function normalizeProgress(val) {
  if (val == null) return 0
  if (val > 1) return val  // 已经是百分比格式
  return val * 100  // 0-1格式转百分比
}
</script>
<style scoped>
.esb { display: flex; gap: 4px; margin-bottom: 14px; flex-wrap: wrap; }
.esb-item { flex: 1; min-width: 60px; text-align: center; padding: 8px 4px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 4px; font-size: var(--fs-xs); position: relative; }
.esb-item.current { border-color: var(--primary-color); background: var(--primary-light); font-weight: 600; }
.esb-item.done { border-color: var(--el-color-success); background: #f6ffed; }
.esb-item.locked { opacity: .4; }
.esb-icon { display: block; font-size: var(--fs-md); }
.esb-name { display: block; font-size: 10px; color: var(--text-secondary, var(--text-secondary)); margin-top: 2px; }
.esb-fill { position: absolute; bottom: 0; left: 0; height: 3px; background: var(--el-color-success); border-radius: 0 0 4px 4px; }
</style>
