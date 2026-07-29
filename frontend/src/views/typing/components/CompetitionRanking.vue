<template>
  <div
    class="ranking-sidebar"
    role="log"
    aria-live="polite"
    aria-label="实时排名"
  >
    <h4>实时排名</h4>
    <div v-if="ranking.length === 0" class="empty-rank">暂无选手数据</div>
    <div
      v-for="(r, idx) in ranking"
      :key="r.studentId"
      class="rank-row"
      :class="{ me: r.studentId === myStudentId }"
    >
      <span class="rank-num">{{ idx + 1 }}</span>
      <span class="rank-name">{{ r.nickname }}<small v-if="r.className" class="rank-class">{{ r.className }}</small></span>
      <span class="rank-progress">{{ r.progressPercent ?? '--' }}%</span>
      <span class="rank-speed">{{ r.speedWpm ?? '--' }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  ranking: { type: Array, default: () => [] },
  myStudentId: { type: Number, default: null }
})
</script>

<style scoped>
.ranking-sidebar { width: 210px; flex-shrink: 0; background: var(--typing-surface); border: 1px solid var(--typing-border); border-radius: var(--radius-md); padding: 12px; max-height: 500px; overflow-y: auto; }
.ranking-sidebar h4 { margin: 0 0 10px; font-size: var(--fs-md); color: var(--typing-text); }
.rank-row { display: flex; align-items: center; gap: 6px; padding: 5px 6px; border-radius: var(--radius-sm); font-size: var(--fs-xs); color: var(--typing-text); }
.rank-row.me { background: var(--typing-current-bg); }
.rank-num { width: 20px; font-weight: 700; color: var(--typing-pending); font-size: var(--fs-xs); }
.rank-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-progress { color: var(--typing-cursor); font-weight: 600; width: 32px; text-align: right; font-size: var(--fs-xs); }
.rank-speed { color: var(--typing-pending); width: 32px; text-align: right; font-size: var(--fs-xs); }
.rank-class { display: block; font-size: 10px; color: var(--typing-pending); font-weight: 400; }
.empty-rank { text-align: center; color: var(--typing-pending); padding: 20px; font-size: var(--fs-xs); }
@media (max-width: 768px) {
  .ranking-sidebar { width: 100%; max-height: 200px; }
}
</style>
