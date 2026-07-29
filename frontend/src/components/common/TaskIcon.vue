<template>
  <svg
    :width="size"
    :height="size"
    :viewBox="viewBox"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="task-icon"
    :class="[`task-icon--${variant}`]"
    :style="{ color: color || taskTypeColor(type) }"
  >
    <!-- 课前预习 - 打开的书本 -->
    <template v-if="type === 'PRE_CLASS'">
      <path
        d="M4 6h4l4 4 4-4h4v12H4V6z"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path d="M12 6v12" stroke="currentColor" stroke-width="2" />
    </template>
    <!-- 课中活动 - 笔/编辑 -->
    <template v-else-if="type === 'IN_CLASS'">
      <path
        d="M15.232 5.232l3.536 3.536m-2.121-4.95a1.5 1.5 0 012.121 2.121L7.5 17.207l-3.5.707.707-3.5L15.232 3.818z"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </template>
    <!-- 课后作业 - 书+书签 -->
    <template v-else-if="type === 'AFTER_CLASS'">
      <path
        d="M4 4h16v16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M12 4v8l2-1.5L16 12V4"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </template>
    <!-- 形成性评价 - 剪贴板 -->
    <template v-else-if="type === 'FORMATIVE'">
      <path
        d="M9 5H5a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-4"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <rect
        x="9"
        y="3"
        width="6"
        height="4"
        rx="1"
        stroke="currentColor"
        stroke-width="2"
      />
      <path
        d="M9 13h6M9 17h4"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </template>
    <!-- 终结性评价 - 奖杯 -->
    <template v-else-if="type === 'SUMMATIVE'">
      <path
        d="M6 4h12v3a6 6 0 01-12 0V4z"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <path
        d="M6 4h3a5 5 0 015 5v12M15 4h3a3 3 0 013 3v1a3 3 0 01-3 3h-1"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <rect
        x="8"
        y="12"
        width="8"
        height="2"
        rx="1"
        stroke="currentColor"
        stroke-width="2"
      />
      <rect
        x="9"
        y="16"
        width="6"
        height="2"
        rx="1"
        stroke="currentColor"
        stroke-width="2"
      />
    </template>
    <!-- 实训任务 - 电脑 -->
    <template v-else-if="type === 'PRACTICE'">
      <rect
        x="3"
        y="4"
        width="18"
        height="12"
        rx="2"
        stroke="currentColor"
        stroke-width="2"
      />
      <path
        d="M8 20h8M12 16v4"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </template>
    <!-- 仿真任务 - 显示器+齿轮 -->
    <template v-else-if="type === 'SIMULATION'">
      <rect
        x="2"
        y="3"
        width="20"
        height="14"
        rx="2"
        stroke="currentColor"
        stroke-width="2"
      />
      <path
        d="M8 21h8M12 17v4"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
      <circle
        cx="12"
        cy="10"
        r="2"
        stroke="currentColor"
        stroke-width="1.5"
        fill="none"
      />
    </template>
    <!-- 德育作业 - 心形 -->
    <template v-else-if="type === 'MORAL'">
      <path
        d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </template>
    <!-- 劳动作业 - 扳手 -->
    <template v-else-if="type === 'LABOR'">
      <path
        d="M14.7 6.3a5.5 5.5 0 00-8.4 8.4l-3.3 3.3 2.1 2.1 3.3-3.3a5.5 5.5 0 008.4-8.4l-4.5 4.5a1 1 0 01-1.4-1.4l4.5-4.5z"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </template>
    <!-- 问卷调查 - 图表 -->
    <template v-else-if="type === 'SURVEY'">
      <rect
        x="3"
        y="3"
        width="18"
        height="18"
        rx="2"
        stroke="currentColor"
        stroke-width="2"
      />
      <path
        d="M8 13v4M12 9v8M16 11v6"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </template>

    <!-- 状态图标 -->
    <template v-else-if="variant === 'status'">
      <!-- 已提交 - 对勾 -->
      <template v-if="type === 'SUBMITTED'">
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="2"
        />
        <path
          d="M8 12l3 3 5-5"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </template>
      <!-- 已评分 - 星形 -->
      <template v-else-if="type === 'GRADED'">
        <path
          d="M12 2l2.4 7.4h7.8l-6.3 4.6 2.4 7.4L12 16.8l-6.3 4.6 2.4-7.4-6.3-4.6h7.8L12 2z"
          stroke="currentColor"
          stroke-width="2"
          stroke-linejoin="round"
        />
      </template>
      <!-- 已退回 -->
      <template v-else-if="type === 'RETURNED'">
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="2"
        />
        <path
          d="M12 7v8M9 10l3-3 3 3"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </template>
      <!-- 待处理 - 时钟 -->
      <template v-else-if="type === 'PENDING'">
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="2"
        />
        <path
          d="M12 6v6l4 2"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </template>
      <!-- 已豁免 -->
      <template v-else-if="type === 'EXEMPTED'">
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="2"
        />
        <path
          d="M12 7v5l3 3"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </template>
    </template>

    <!-- 兜底：默认剪贴板 -->
    <template v-else>
      <rect
        x="4"
        y="2"
        width="16"
        height="20"
        rx="2"
        stroke="currentColor"
        stroke-width="2"
      />
      <path
        d="M9 8h6M9 12h6M9 16h3"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </template>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, required: true },
  size: { type: Number, default: 20 },
  variant: { type: String, default: 'type' },
  color: { type: String, default: '' }
})

// 24x24 viewBox for type icons, 24x24 for status
const viewBox = computed(() => '0 0 24 24')
</script>

<script>
export const taskTypeColor = (type) => {
  const map = {
    PRE_CLASS: '#4361ee',
    IN_CLASS: '#4a9e2e',
    AFTER_CLASS: '#2d7dd2',
    FORMATIVE: '#7209b7',
    SUMMATIVE: '#cf4545',
    PRACTICE: '#c97d2d',
    SIMULATION: '#e67e22',
    MORAL: '#e8308a',
    LABOR: '#6b4fa0',
    SURVEY: '#0ea5a5',
    SUBMITTED: '#409eff',
    GRADED: '#4a9e2e',
    RETURNED: '#cf4545',
    PENDING: '#909399',
    EXEMPTED: '#a0a5b5'
  }
  return map[type] || '#6b7a8a'
}

export const taskTypeIcon = (type) => type
</script>

<style scoped>
.task-icon {
  flex-shrink: 0;
  vertical-align: middle;
}
.task-icon--type {
  /* default type style */
}
.task-icon--status {
  /* status style handled by color prop */
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
