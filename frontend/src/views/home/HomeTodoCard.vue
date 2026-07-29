<template>
  <div class="todo-card" :class="{ 'todo-card--urgent': urgency === 'danger', 'todo-card--warning': urgency === 'warning' }" @click="$emit('click', homework)">
    <div class="todo-card__bar" :class="'todo-card__bar--' + urgency"></div>
    <div class="todo-card__body">
      <div class="todo-card__header">
        <span class="todo-card__title">{{ homework.title }}</span>
        <el-tag size="small" :type="taskTypeTag">{{ taskTypeLabel }}</el-tag>
      </div>
      <div v-if="homework.className" class="todo-card__class">{{ homework.className }}</div>
      <div v-if="totalStudents > 0" class="todo-card__progress">
        <el-progress
          :percentage="submitPercent"
          :color="progressColor"
          :stroke-width="6"
        />
        <span class="todo-card__progress-text">{{ homework.submitCount || 0 }}/{{ totalStudents }} 已提交</span>
      </div>
      <div v-if="homework.deadline" class="todo-card__footer">
        <span class="todo-card__deadline" :class="{ 'text--danger': urgency === 'danger', 'text--warning': urgency === 'warning' }">
          截止 {{ formatDeadline(homework.deadline) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  homework: { type: Object, required: true }
})

defineEmits(['click'])

const taskTypeTag = computed(() => {
  const map = { EXAM: 'danger', HOMEWORK: 'primary', PRACTICE: 'success', SURVEY: 'warning', MORAL: 'info', LABOR: 'info' }
  return map[props.homework.taskType] || 'info'
})

const taskTypeLabel = computed(() => {
  const map = {
    FORMATIVE: '考试', SUMMATIVE: '考试', EXAM: '考试',
    IN_CLASS: '作业', AFTER_CLASS: '作业', PRE_CLASS: '预习',
    HOMEWORK: '作业', PRACTICE: '实训', SURVEY: '问卷',
    MORAL: '德育', LABOR: '劳动'
  }
  return map[props.homework.taskType] || props.homework.taskType || '任务'
})

const totalStudents = computed(() => props.homework.totalStudents || 0)
const submitPercent = computed(() => {
  if (totalStudents.value <= 0) return 0
  return Math.round(((props.homework.submitCount || 0) / totalStudents.value) * 100)
})

const progressColor = computed(() => {
  const p = submitPercent.value
  if (p >= 80) return 'var(--success-color)'
  if (p >= 40) return 'var(--warning-color)'
  return 'var(--danger-color)'
})

const urgency = computed(() => {
  if (!props.homework.deadline) return 'normal'
  const remaining = dayjs(props.homework.deadline).diff(dayjs(), 'hour')
  if (remaining < 0) return 'danger'
  if (remaining < 24) return 'warning'
  return 'normal'
})

const formatDeadline = (t) => {
  if (!t) return '-'
  const d = dayjs(t)
  const remaining = d.diff(dayjs(), 'hour')
  if (remaining < 0) return '已截止'
  if (remaining < 24) return `还剩 ${remaining} 小时`
  return d.format('MM-DD HH:mm')
}
</script>

<style scoped lang="scss">
.todo-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 0.5px solid var(--border-color);
  display: flex;
  overflow: hidden;
  cursor: pointer;
  transition: border-color var(--transition-fast), transform var(--transition-fast);

  &:hover {
    border-color: var(--primary-color);
    transform: translateY(-1px);
  }

  &__bar {
    width: 4px;
    flex-shrink: 0;

    &--danger { background: var(--danger-color); }
    &--warning { background: var(--warning-color); }
    &--normal { background: var(--success-color); }
  }

  &__body {
    flex: 1;
    padding: 14px 16px;
    min-width: 0;
  }

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
  }

  &__title {
    font-size: var(--fs-sm);
    font-weight: 500;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }

  &__class {
    font-size: var(--fs-xs);
    color: var(--text-secondary);
    margin-bottom: 8px;
  }

  &__progress {
    margin-bottom: 8px;

    &-text {
      font-size: var(--fs-xs);
      color: var(--text-secondary);
      margin-top: 4px;
      display: inline-block;
    }
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__deadline {
    font-size: var(--fs-xs);
    color: var(--text-secondary);
  }

  .text--danger { color: var(--danger-color); font-weight: 500; }
  .text--warning { color: var(--warning-color); font-weight: 500; }
}

/* 移动端 — 卡片全宽 + 紧凑内边距 */
@media (max-width: 768px) {
  .todo-card {
    width: 100%;
    margin-bottom: 8px;

    .todo-card__body {
      padding: 10px 12px;
    }
  }
}
</style>
