<template>
  <div v-if="ranking.length" class="cer">
    <h4 style="font-size:var(--fs-md);margin:0 0 8px">🏅 班级周榜</h4>
    <div v-for="(r, i) in ranking.slice(0, 10)" :key="r.studentId || i" class="cer-row">
      <span class="cer-rank">{{ i + 1 }}</span>
      <span class="cer-name">{{ r.studentName }}</span>
      <span class="cer-score">{{ r.vocabKnown || 0 }}词 {{ r.streak || 0 }}天</span>
    </div>
  </div>
</template>
<script setup>defineProps({ ranking: Array })</script>
<style scoped>
.cer { padding: 14px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; margin-bottom: 14px; }
.cer-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: var(--fs-sm); border-bottom: 1px solid var(--border-light); }
.cer-rank { width: 20px; font-weight: 700; color: var(--text-secondary); }
.cer-name { flex: 1; }
.cer-score { font-size: var(--fs-xs); color: var(--text-secondary); }
</style>
