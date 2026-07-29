<template>
  <div class="challenge-banner">
    <div class="challenge-icon">🏆</div>
    <div class="challenge-text">
      <strong>本周挑战</strong>
      <span>{{ text }}</span>
    </div>
    <div class="challenge-tip">💡 签到、完成作业、参与考试都能获得积分哦～</div>
  </div>
</template>

<script setup>
defineProps({ text: String })
</script>

<style scoped lang="scss">
.challenge-banner {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(135deg, var(--primary-color) 0%, color-mix(in srgb, var(--primary-color) 60%, var(--deco-purple, #7209b7)) 100%);
  color: var(--bg-card); border-radius: var(--radius-lg);
  padding: 16px 24px; margin-bottom: 16px;
  .challenge-icon { font-size: var(--fs-3xl); animation: bounce 1.5s ease-in-out infinite; }
  .challenge-text { flex: 1;
    strong { display: block; font-size: var(--fs-md); margin-bottom: 2px; }
    span { font-size: var(--fs-sm); opacity: 0.9; }
  }
  .challenge-tip { font-size: var(--fs-xs); opacity: 0.8; white-space: nowrap; }
}
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
@media (max-width: 768px) {
  .challenge-banner {
    flex-direction: column; align-items: flex-start; padding: 12px 16px; gap: 8px;
    .challenge-tip { white-space: normal; font-size: var(--fs-xs); }
    .challenge-icon { font-size: var(--fs-2xl); }
  }
}
</style>
