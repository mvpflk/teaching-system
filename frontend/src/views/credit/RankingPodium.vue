<template>
  <div v-if="ranking.length >= 3" class="podium">
    <div class="podium-item second">
      <div class="medal-badge"><span class="medal-icon">🥈</span></div>
      <el-avatar :size="56" shape="circle" class="podium-avatar silver">{{ ranking[1]?.realName?.charAt(0) || '?' }}</el-avatar>
      <div class="name">{{ ranking[1]?.realName }}</div>
      <div class="credits">⭐ {{ ranking[1]?.totalCredits || 0 }} 分</div>
      <div class="title-tag">{{ ranking[1]?.titleName || '初出茅庐' }}</div>
      <div v-if="ranking[1]?.currentStreak" class="streak-badge">🔥 {{ ranking[1].currentStreak }}天</div>
    </div>
    <div class="podium-item first">
      <div class="crown">👑</div>
      <div class="glow-ring"></div>
      <div class="medal-badge"><span class="medal-icon">🥇</span></div>
      <el-avatar :size="64" shape="circle" class="podium-avatar gold">{{ ranking[0]?.realName?.charAt(0) || '?' }}</el-avatar>
      <div class="name champion-name">{{ ranking[0]?.realName }}</div>
      <div class="credits champion-credits">⭐ {{ ranking[0]?.totalCredits || 0 }} 分</div>
      <div class="title-tag">{{ ranking[0]?.titleName || '初出茅庐' }}</div>
      <div v-if="ranking[0]?.currentStreak" class="streak-badge">🔥 {{ ranking[0].currentStreak }}天</div>
    </div>
    <div class="podium-item third">
      <div class="medal-badge"><span class="medal-icon">🥉</span></div>
      <el-avatar :size="56" shape="circle" class="podium-avatar bronze">{{ ranking[2]?.realName?.charAt(0) || '?' }}</el-avatar>
      <div class="name">{{ ranking[2]?.realName }}</div>
      <div class="credits">⭐ {{ ranking[2]?.totalCredits || 0 }} 分</div>
      <div class="title-tag">{{ ranking[2]?.titleName || '初出茅庐' }}</div>
      <div v-if="ranking[2]?.currentStreak" class="streak-badge">🔥 {{ ranking[2].currentStreak }}天</div>
    </div>
  </div>
</template>

<script setup>
defineProps({ ranking: Array })
</script>

<style scoped lang="scss">
.podium {
  display: flex; justify-content: center; align-items: flex-end;
  gap: 24px; padding: 40px 20px 28px;
  background: linear-gradient(180deg, var(--primary-light) 0%, var(--bg-secondary) 60%, transparent 100%);
  border-radius: var(--radius-lg); margin-bottom: 20px; position: relative;

  .podium-item {
    text-align: center; position: relative; display: flex;
    flex-direction: column; align-items: center; gap: 4px;

    &.first .podium-avatar.gold { box-shadow: 0 0 0 4px var(--gold, #ffd700), 0 0 20px rgba(255,215,0,0.4); }
    &.second .podium-avatar.silver { box-shadow: 0 0 0 3px var(--silver, #c0c0c0); }
    &.third .podium-avatar.bronze { box-shadow: 0 0 0 3px var(--bronze, #cd7f32); }

    .crown {
      font-size: var(--fs-3xl); position: absolute; top: -36px;
      left: 50%; transform: translateX(-50%);
      animation: crown-float 2s ease-in-out infinite;
    }
    @keyframes crown-float {
      0%, 100% { transform: translateX(-50%) translateY(0); }
      50% { transform: translateX(-50%) translateY(-6px); }
    }
    .glow-ring {
      position: absolute; top: 50%; left: 50%;
      width: 100px; height: 100px;
      transform: translate(-50%, -50%);
      border-radius: var(--radius-full);
      background: radial-gradient(circle, color-mix(in srgb, var(--gold, #ffd700) 15%, transparent) 0%, transparent 70%);
      animation: glow-pulse 2s ease-in-out infinite;
    }
    @keyframes glow-pulse {
      0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
      50% { transform: translate(-50%, -50%) scale(1.3); opacity: 1; }
    }
    .medal-badge { margin-bottom: 4px; .medal-icon { font-size: var(--fs-3xl); } }
    .name { font-weight: 600; color: var(--text-primary); font-size: var(--fs-md); }
    .champion-name { font-size: var(--fs-lg); }
    .credits { font-size: var(--fs-xs); color: var(--text-secondary); }
    .champion-credits { font-size: var(--fs-md); color: var(--warning-color); font-weight: 600; }
    .title-tag { font-size: var(--fs-xs); color: var(--text-secondary); }
    .streak-badge { font-size: var(--fs-xs); color: var(--warning-color); font-weight: 500; margin-top: 2px; }
  }
}

@media (max-width: 768px) {
  .podium {
    gap: 8px; padding: 20px 4px 12px;
    .podium-item {
      gap: 2px;
      .medal-badge { margin-bottom: 0; .medal-icon { font-size: var(--fs-2xl); } }
      .name { font-size: var(--fs-xs); }
      .champion-name { font-size: var(--fs-md); }
      .credits { font-size: 10px; }
      .champion-credits { font-size: var(--fs-xs); }
      .title-tag { font-size: 10px; }
      .streak-badge { font-size: 10px; }
      .crown { font-size: var(--fs-2xl); top: -28px; }
      :deep(.el-avatar) {
        width: 40px !important; height: 40px !important; font-size: var(--fs-lg);
      }
    }
    .podium-item.first :deep(.el-avatar) {
      width: 48px !important; height: 48px !important;
    }
  }
}
</style>
