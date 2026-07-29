<template>
  <div class="qpc-summary-overlay">
    <div class="qpc-summary">
      <div class="qpc-summary-header">
        <h2>📊 本轮课堂抽问总结</h2>
        <p class="qpc-summary-sub">共 {{ stats.drawn }} 题 · {{ stats.called }} 人参与</p>
      </div>
      <div class="qpc-summary-stats">
        <div class="qpc-ss-item">
          <span class="qpc-ss-val" :style="{ color: stats.total > 0 && stats.correct / stats.total >= 0.6 ? 'var(--el-color-success)' : 'var(--el-color-warning)' }">
            {{ stats.total > 0 ? Math.round(stats.correct / stats.total * 100) : 0 }}%
          </span>
          <span class="qpc-ss-lbl">正确率</span>
        </div>
        <div class="qpc-ss-item">
          <span class="qpc-ss-val">{{ stats.called }}</span>
          <span class="qpc-ss-lbl">参与人数</span>
        </div>
        <div class="qpc-ss-item">
          <span class="qpc-ss-val">{{ stats.drawn }}</span>
          <span class="qpc-ss-lbl">题目总数</span>
        </div>
        <div class="qpc-ss-item">
          <span class="qpc-ss-val">{{ stats.correct }}</span>
          <span class="qpc-ss-lbl">回答正确</span>
        </div>
      </div>
      <div v-if="leaderboard.length" class="qpc-summary-board">
        <h3>🏆 积分排行</h3>
        <div v-for="(s, i) in leaderboard" :key="s.studentId" class="qpc-sb-item">
          <span class="qpc-sb-rank">{{ i < 3 ? ['🥇','🥈','🥉'][i] : i + 1 }}</span>
          <el-avatar v-if="s.avatarUrl" :size="28" :src="s.avatarUrl" />
          <span v-else class="qpc-sb-avatar">{{ (s.name || '?').charAt(0) }}</span>
          <span class="qpc-sb-name">{{ s.name }}</span>
          <span class="qpc-sb-score">+{{ s.score }}分</span>
        </div>
      </div>
      <div class="qpc-summary-actions">
        <el-button type="primary" size="large" @click="$emit('restart')">再来一轮</el-button>
        <el-button size="large" @click="$emit('exit')">返回</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  stats: Object,
  leaderboard: { type: Array, default: () => [] }
})
defineEmits(['restart', 'exit'])
</script>

<style scoped>
.qpc-summary-overlay { position: fixed; inset: 0; z-index: 999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.8); animation: summaryFadeIn 0.4s ease; }
@keyframes summaryFadeIn { from { opacity: 0; } to { opacity: 1; } }
.qpc-summary { background: var(--bg-card); border-radius: 20px; padding: 40px 48px; max-width: 600px; width: 90%; text-align: center; box-shadow: 0 20px 60px rgba(0,0,0,0.3); animation: summarySlide 0.5s ease; }
@keyframes summarySlide { from { opacity: 0; transform: translateY(30px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
.qpc-summary-header { margin-bottom: 28px; }
.qpc-summary-header h2 { font-size: 28px; font-weight: 800; color: var(--text-primary); margin: 0 0 8px; }
.qpc-summary-sub { font-size: var(--fs-md); color: var(--text-secondary); margin: 0; }
.qpc-summary-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
.qpc-ss-item { padding: 16px 8px; background: var(--bg-section); border-radius: 12px; }
.qpc-ss-val { display: block; font-size: 28px; font-weight: 800; color: var(--primary-color); line-height: 1.2; }
.qpc-ss-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.qpc-summary-board { text-align: left; margin-bottom: 24px; }
.qpc-summary-board h3 { font-size: var(--fs-lg); font-weight: 700; margin: 0 0 12px; color: var(--text-primary); }
.qpc-sb-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border-light); }
.qpc-sb-item:last-child { border-bottom: none; }
.qpc-sb-rank { width: 28px; text-align: center; font-size: var(--fs-lg); flex-shrink: 0; }
.qpc-sb-avatar { width: 28px; height: 28px; border-radius: 50%; background: var(--bg-secondary); display: flex; align-items: center; justify-content: center; font-size: var(--fs-xs); font-weight: 700; color: var(--text-secondary); flex-shrink: 0; }
.qpc-sb-name { flex: 1; font-size: var(--fs-md); font-weight: 500; color: var(--text-primary); }
.qpc-sb-score { font-size: var(--fs-md); font-weight: 700; color: var(--el-color-success); flex-shrink: 0; }
.qpc-summary-actions { display: flex; gap: 12px; justify-content: center; }
</style>
