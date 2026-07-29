<template>
  <div>
    <div class="ss-hero">
      <div class="ss-hero-card quiz-card" @click="$emit('selectPanel', 'quiz')">
        <div class="ss-hc-icon">
          <el-icon><Aim /></el-icon>
        </div>
        <div class="ss-hc-body">
          <div class="ss-hc-title">课堂抽问</div>
          <div class="ss-hc-desc">随机选人 · 当堂评分</div>
        </div>
        <div class="ss-hc-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <div
        class="ss-hero-card buzz-card"
        :class="{ disabled: sceneMode === 'CLASSROOM' }"
        @click="sceneMode === 'LAB' ? $emit('selectPanel', 'buzz') : null"
      >
        <div class="ss-hc-icon">
          <el-icon><Lightning /></el-icon>
        </div>
        <div class="ss-hc-body">
          <div class="ss-hc-title">抢答</div>
          <div class="ss-hc-desc">学生竞答 · 手速比拼</div>
        </div>
        <div v-if="sceneMode === 'CLASSROOM'" class="ss-hc-badge">仅微机室</div>
        <div v-else class="ss-hc-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="ss-hero-card vote-card" @click="$emit('selectPanel', 'vote')">
        <div class="ss-hc-icon">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="ss-hc-body">
          <div class="ss-hc-title">即时投票</div>
          <div class="ss-hc-desc">
            {{ sceneMode === 'LAB' ? '在线表决 · 实时统计' : '举手表决 · 手动计数' }}
          </div>
        </div>
        <div class="ss-hc-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <div class="ss-stats-bar">
      <div class="ss-stat">
        <span class="ss-stat-val">{{ stats.quizCount }}</span>
        <span class="ss-stat-label">互动次数</span>
      </div>
      <div class="ss-stat-divider"></div>
      <div class="ss-stat">
        <span class="ss-stat-val">{{ stats.correctRate }}%</span>
        <span class="ss-stat-label">正确率</span>
      </div>
      <div class="ss-stat-divider"></div>
      <div class="ss-stat">
        <span class="ss-stat-val">{{ stats.activeStudents }}<span class="ss-stat-sub">/{{ studentCount }}</span></span>
        <span class="ss-stat-label">参与人数</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowRight, Aim, Lightning, TrendCharts } from '@element-plus/icons-vue';

defineProps({
  sceneMode: { type: String, required: true },
  studentCount: { type: Number, default: 0 },
  stats: { type: Object, default: () => ({ quizCount: 0, correctRate: 0, activeStudents: 0 }) },
});

defineEmits(['selectPanel']);
</script>

<style scoped lang="scss">
$quiz-color: var(--primary-color);
$buzz-color: var(--color-buzz);
$vote-color: var(--color-vote);

.ss-hero {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.ss-hero-card {
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-xl);
  padding: 32px 28px;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 120px;
  transition:
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 16px;
    bottom: 16px;
    width: 4px;
    border-radius: 0 3px 3px 0;
    transition: all 0.3s ease;
  }

  &.quiz-card::before {
    background: $quiz-color;
  }
  &.buzz-card::before {
    background: $buzz-color;
  }
  &.vote-card::before {
    background: $vote-color;
  }

  &:hover:not(.disabled) {
    transform: translateY(-3px);
    border-color: var(--border-input);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }

  &.disabled {
    opacity: 0.45;
    cursor: not-allowed;
    &:hover {
      transform: none;
      box-shadow: none;
    }
  }
}

.ss-hc-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .el-icon {
    font-size: 28px;
  }

  .quiz-card & {
    background: rgba($quiz-color, 0.08);
    color: $quiz-color;
  }
  .buzz-card & {
    background: rgba($buzz-color, 0.1);
    color: $buzz-color;
  }
  .vote-card & {
    background: rgba($vote-color, 0.1);
    color: $vote-color;
  }
}

.ss-hc-body {
  flex: 1;
  min-width: 0;
}

.ss-hc-title {
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.ss-hc-desc {
  font-size: var(--fs-md);
  color: var(--text-secondary);
  line-height: 1.4;
}

.ss-hc-arrow {
  color: var(--text-disabled);
  font-size: var(--fs-lg);
  flex-shrink: 0;
  transition:
    transform 0.2s ease,
    color 0.2s ease;

  .ss-hero-card:hover:not(.disabled) & {
    transform: translateX(3px);
    color: var(--text-secondary);
  }
}

.ss-hc-badge {
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  background: var(--bg-secondary);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  white-space: nowrap;
  flex-shrink: 0;
}

.ss-stats-bar {
  display: flex;
  align-items: center;
  gap: 0;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px 28px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.ss-stat {
  flex: 1;
  text-align: center;
}

.ss-stat-val {
  display: block;
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.2;
}

.ss-stat-sub {
  font-size: var(--fs-lg);
  font-weight: 400;
  color: var(--text-secondary);
}

.ss-stat-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-top: 2px;
  display: block;
}

.ss-stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border-light);
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .ss-hero {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .ss-hero-card {
    padding: 24px 20px;
    min-height: 80px;
  }
  .ss-hc-title {
    font-size: 17px;
  }
  .ss-hc-icon {
    width: 44px;
    height: 44px;
    .el-icon {
      font-size: 22px;
    }
  }
  .ss-stats-bar {
    padding: 14px 16px;
  }
  .ss-stat-val {
    font-size: 22px;
  }
}
</style>
