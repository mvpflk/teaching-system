<template>
  <template v-if="sceneMode === 'LAB'">
    <div class="ss-rail-header">
      <span class="ss-rh-title">
        <el-icon><UserFilled /></el-icon>
        班级 {{ studentScores.length }} 人
      </span>
      <span v-if="absentSet.size" class="ss-rh-absent">缺席 {{ absentSet.size }}</span>
    </div>
    <div v-if="studentScores.length" class="ss-rail-list">
      <div
        v-for="(s, idx) in rankedScores"
        :key="s.studentId"
        class="ss-rail-student"
        :class="{
          absent: absentSet.has(s.studentId),
          'top-1': idx === 0,
          'top-2': idx === 1,
          'top-3': idx === 2,
          'just-scored': s._justScored,
        }"
        @click="$emit('toggleAbsent', s.studentId)"
      >
        <span class="ss-rs-rank">{{ idx < 3 ? ['🥇', '🥈', '🥉'][idx] : idx + 1 }}</span>
        <el-avatar
          v-if="s.avatarUrl"
          :size="34"
          :src="s.avatarUrl"
          class="ss-rs-avatar-img"
        />
        <span v-else class="ss-rs-avatar">{{ (s.name || '?')[0] }}</span>
        <span class="ss-rs-name">{{ s.name }}</span>
        <el-icon v-if="absentSet.has(s.studentId)" class="ss-rs-absent"><CircleClose /></el-icon>
        <span
          class="ss-rs-score"
          :class="{ earn: (s.sessionScore || 0) > 0 }"
        >+{{ s.sessionScore || 0 }}</span>
      </div>
    </div>
    <el-empty v-else description="暂无学生" :image-size="32" />
  </template>

  <template v-else>
    <div class="ss-rail-header">
      <span class="ss-rh-title">
        <el-icon><DataAnalysis /></el-icon>
        课堂摘要
      </span>
    </div>
    <div class="ss-rail-stats">
      <div class="ss-rs-item">
        <span class="ss-rs-val">{{ stats.quizCount }}</span>
        <span class="ss-rs-lbl">抽问</span>
      </div>
      <div class="ss-rs-item">
        <span class="ss-rs-val">{{ stats.correctRate }}%</span>
        <span class="ss-rs-lbl">正确率</span>
      </div>
      <div class="ss-rs-item">
        <span class="ss-rs-val">{{ stats.activeStudents }}</span>
        <span class="ss-rs-lbl">参与</span>
      </div>
    </div>

    <div class="ss-rail-section">
      <div class="ss-rs-section-title">积分 TOP5</div>
      <div v-for="(s, i) in topFive" :key="s.studentId" class="ss-rail-top-item">
        <span class="ss-rt-rank">{{ i < 3 ? ['🥇', '🥈', '🥉'][i] : i + 1 }}</span>
        <el-avatar v-if="s.avatarUrl" :size="24" :src="s.avatarUrl" />
        <span class="ss-rt-name">{{ s.name }}</span>
        <span class="ss-rt-score">+{{ s.sessionScore || 0 }}</span>
      </div>
      <div v-if="!topFive.length" class="ss-rail-empty">暂无数据</div>
    </div>
  </template>
</template>

<script setup>
import { UserFilled, DataAnalysis, CircleClose } from '@element-plus/icons-vue';

defineProps({
  sceneMode: { type: String, required: true },
  rankedScores: { type: Array, default: () => [] },
  topFive: { type: Array, default: () => [] },
  absentSet: { type: Set, default: () => new Set() },
  studentScores: { type: Array, default: () => [] },
  stats: { type: Object, default: () => ({ quizCount: 0, correctRate: 0, activeStudents: 0 }) },
});

defineEmits(['toggleAbsent']);
</script>

<style scoped lang="scss">
$buzz-color: var(--color-buzz);

.ss-rail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 12px;
  border-bottom: 0.5px solid var(--border-light);
}

.ss-rh-title {
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.ss-rh-absent {
  font-size: var(--fs-xs);
  color: var(--el-color-warning);
  font-weight: 500;
}

.ss-rail-list {
  padding: 4px 10px;
}

.ss-rail-student {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s ease;
  min-height: 44px;

  &.absent {
    opacity: 0.3;
  }

  &.top-1,
  &.top-2,
  &.top-3 {
    background: rgba($buzz-color, 0.06);
  }

  &.just-scored {
    animation: railFlash 0.8s ease;
  }

  &:hover {
    background: var(--bg-hover);
  }
}

@keyframes railFlash {
  0% {
    background: var(--primary-light);
  }
  100% {
    background: transparent;
  }
}

.ss-rs-rank {
  width: 28px;
  text-align: center;
  font-size: var(--fs-lg);
  flex-shrink: 0;
  font-weight: 600;
  color: var(--text-secondary);
}

.ss-rs-avatar {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-sm);
  font-weight: 700;
  flex-shrink: 0;

  .top-1 & {
    background: rgba($buzz-color, 0.15);
    color: $buzz-color;
  }
  .top-2 & {
    background: rgba(#94a3b8, 0.15);
    color: #64748b;
  }
  .top-3 & {
    background: rgba(#d97706, 0.12);
    color: #b45309;
  }
}

.ss-rs-avatar-img {
  flex-shrink: 0;
  border: 2px solid var(--bg-card);
}

.ss-rs-name {
  flex: 1;
  font-size: var(--fs-md);
  color: var(--text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ss-rs-absent {
  color: var(--el-color-danger);
  font-size: var(--fs-md);
  flex-shrink: 0;
}

.ss-rs-score {
  font-size: var(--fs-md);
  font-weight: 700;
  color: var(--text-disabled);
  flex-shrink: 0;

  &.earn {
    color: var(--el-color-success);
  }
}

.ss-rail-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 16px;
}

.ss-rs-item {
  text-align: center;
  padding: 12px 4px;
  background: var(--bg-section);
  border-radius: var(--radius-md);
}

.ss-rs-val {
  display: block;
  font-size: var(--fs-xl);
  font-weight: 800;
  color: var(--primary-color);
}

.ss-rs-lbl {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 2px;
  display: block;
}

.ss-rail-section {
  padding: 0 16px 12px;
}

.ss-rs-section-title {
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.ss-rail-top-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}

.ss-rt-rank {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fs-xs);
  font-weight: 700;
  flex-shrink: 0;
}

.ss-rt-name {
  flex: 1;
  font-size: var(--fs-sm);
  color: var(--text-regular);
}

.ss-rt-score {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--el-color-success);
}

.ss-rail-empty {
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  padding: 8px 0;
}
</style>
