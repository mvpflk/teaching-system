<template>
  <div v-if="achievements && achievements.length" class="aw">
    <h4 style="font-size:var(--fs-md);margin:0 0 8px">🎖️ 成就 ({{ achievements.length }})</h4>
    <div class="aw-grid">
      <el-tag
        v-for="a in achievements"
        :key="a"
        size="small"
        effect="plain"
        type="success"
      >
        {{ a }}
      </el-tag>
    </div>
  </div>
</template>
<script setup>defineProps({ achievements: Array })</script>
<style scoped>
.aw { padding: 14px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; margin-bottom: 14px; }
.aw-grid { display: flex; gap: 6px; flex-wrap: wrap; }
</style>
