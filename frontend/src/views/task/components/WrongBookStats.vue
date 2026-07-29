<template>
  <div class="stats-row">
    <div class="stat-item">
      <div class="stat-num" :class="{ 'stat-warn': stats.unmastered > 0 }">{{ stats.unmastered }}</div>
      <div class="stat-label">未掌握</div>
    </div>
    <div class="stat-item">
      <div class="stat-num">{{ stats.mastered }}</div>
      <div class="stat-label">已掌握</div>
    </div>
    <div class="stat-item">
      <div class="stat-num">{{ stats.weekPractice }}</div>
      <div class="stat-label">本周练习</div>
    </div>
    <div class="stat-item">
      <div class="stat-num" :class="{ 'stat-fire': stats.streak >= 3 }">{{ stats.streak }}</div>
      <div class="stat-label">{{ stats.streak >= 3 ? '🔥 连续天数' : '连续天数' }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({ stats: { type: Object, default: () => ({}) } })
</script>
