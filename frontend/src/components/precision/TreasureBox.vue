<template>
  <div class="tb">
    <div class="tb-box" :class="{ open: visible }">📦</div>
    <div v-if="visible" class="tb-content">
      <p class="tb-score">+{{ result.creditsEarned || 0 }} 积分</p>
      <p>🔥 连续 {{ result.streak || 0 }} 天</p>
      <p class="tb-owl">{{ result.owlMessage || '👍' }}</p>
    </div>
  </div>
</template>
<script setup>
defineProps({ result: Object, visible: { type: Boolean, default: false } })
</script>
<style scoped>
.tb { text-align: center; padding: 20px 0; }
.tb-box { font-size: 48px; transition: transform .5s; }
.tb-box.open { animation: tb-bounce .6s ease; }
@keyframes tb-bounce { 0%,100%{transform:scale(1)} 50%{transform:scale(1.3)} }
.tb-content { margin-top: 12px; font-size: var(--fs-md); line-height: 2; }
.tb-score { font-size: var(--fs-2xl); font-weight: 700; color: var(--primary-color); }
.tb-owl { color: var(--el-color-warning); font-size: var(--fs-sm); }
</style>
