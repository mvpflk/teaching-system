<template>
  <div class="ss-rail-header" style="margin-top: 4px">
    <span class="ss-rh-title">
      <el-icon><UserFilled /></el-icon>
      班级 {{ studentScores.length }} 人
    </span>
    <span v-if="absentSet.size" class="ss-rh-absent">缺席 {{ absentSet.size }}</span>
  </div>
  <div v-if="studentScores.length" class="ss-rail-list">
    <div
      v-for="s in studentScores"
      :key="s.studentId"
      class="ss-rail-student"
      :class="{ absent: absentSet.has(s.studentId) }"
      @click="$emit('toggleAbsent', s.studentId)"
    >
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
  <el-empty v-else description="暂无学生" :image-size="28" />
</template>

<script setup>
import { UserFilled, CircleClose } from '@element-plus/icons-vue';

defineProps({
  studentScores: { type: Array, default: () => [] },
  absentSet: { type: Set, default: () => new Set() },
});

defineEmits(['toggleAbsent']);
</script>

<style scoped lang="scss">
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

  &:hover {
    background: var(--bg-hover);
  }
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
</style>
