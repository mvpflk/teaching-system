<template>
  <div class="wb-left">
    <div class="wb-left-header">
      <span>学生列表</span>
      <span class="wb-left-count">已批 {{ gradedCount }}/{{ totalCount }}</span>
    </div>
    <el-progress :percentage="progressPct" :stroke-width="3" :show-text="false" class="wb-left-progress" />
    <el-input
      :model-value="keyword"
      placeholder="搜索答案内容…"
      size="small"
      clearable
      class="wb-search"
      @update:model-value="$emit('update:keyword', $event)"
    />
    <el-input
      :model-value="searchQuery"
      placeholder="搜索学生姓名…"
      size="small"
      clearable
      class="wb-search"
      @update:model-value="$emit('update:searchQuery', $event)"
    />
    <div class="student-list">
      <div
        v-for="s in students"
        :key="s.id"
        class="stu-row"
        :class="{ active: currentId === s.id, graded: s.status === 'GRADED' }"
        @click="$emit('select', s)"
      >
        <span class="status-dot" :class="statusDotClass(s.status)" />
        <span class="stu-name">{{ s.studentName || s.studentId }}</span>
        <span v-if="s.score != null" class="stu-score">{{ s.score }}分</span>
      </div>
    </div>
    <el-empty v-if="!students.length" description="无匹配学生" :image-size="60" />
    <div v-if="subTotal > subPageSize" class="wb-pagination">
      <el-pagination
        :current-page="subPage"
        layout="total, prev, next"
        :total="subTotal"
        :page-size="subPageSize"
        @update:current-page="$emit('update:subPage', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { useSubmissionStatus } from '@/composables/useSubmissionStatus'

const { statusDotClass } = useSubmissionStatus()

defineProps({
  students: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  currentId: { type: [Number, String], default: null },
  keyword: { type: String, default: '' },
  searchQuery: { type: String, default: '' },
  gradedCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  progressPct: { type: Number, default: 0 },
  subPage: { type: Number, default: 1 },
  subTotal: { type: Number, default: 0 },
  subPageSize: { type: Number, default: 20 },
})

defineEmits(['select', 'update:keyword', 'update:searchQuery', 'update:subPage'])
</script>

<style scoped>
.wb-left-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px 0; font-size: var(--fs-xs); color: var(--text-secondary); }
.wb-left-count { font-weight: 500; }
.wb-left-progress { padding: 0 14px; margin-top: -4px; margin-bottom: 4px; }
.wb-left { width: 260px; flex-shrink: 0; border: 1px solid var(--border-light); border-radius: var(--radius-md); overflow: hidden; display: flex; flex-direction: column; }
.wb-search { margin: 10px; width: auto; }
.student-list { flex: 1; overflow-y: auto; }
.stu-row { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-bottom: 1px solid var(--bg-section); cursor: pointer; font-size: var(--fs-sm); }
.stu-row:hover { background: var(--bg-section); }
.stu-row.active { background: var(--primary-light); border-left: 3px solid var(--primary-color); }
.stu-row.graded { opacity: 0.7; }
.stu-name { flex: 1; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stu-score { color: var(--primary-color); font-weight: 600; font-size: var(--fs-xs); }
@media (max-width: 768px) {
  .wb-left { width: 100%; max-height: none; }
  .student-list { display: flex; flex-direction: row; overflow-x: auto; gap: 6px; padding: 8px 14px; -webkit-overflow-scrolling: touch; }
  .stu-row { flex-shrink: 0; padding: 6px 12px; border-radius: 20px; border: 1px solid var(--border-light); font-size: var(--fs-xs); white-space: nowrap; gap: 6px; }
  .stu-row.active { background: var(--primary-color); color: #fff; border-color: var(--primary-color); }
}
</style>
