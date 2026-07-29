<template>
  <div class="stat-grid mb-24">
    <template v-if="isStudent">
      <HomeTrendStat
        label="待完成任务"
        :value="stats.pendingHomework"
        :trend-direction="studentTrends[0]?.direction"
        :trend-value="studentTrends[0]?.value"
        :trend-label="studentTrends[0]?.label"
      />
      <HomeTrendStat
        label="待完成考试"
        :value="stats.pendingExam"
        :trend-direction="studentTrends[1]?.direction"
        :trend-value="studentTrends[1]?.value"
        :trend-label="studentTrends[1]?.label"
      />
      <HomeTrendStat
        label="我的积分"
        :value="stats.totalCredits"
        :value-color="'var(--warning-color)'"
        :trend-direction="studentTrends[2]?.direction"
        :trend-value="studentTrends[2]?.value"
        :trend-label="studentTrends[2]?.label"
      />
      <HomeTrendStat
        label="待办总计"
        :value="stats.pendingHomework + stats.pendingExam"
        :trend-direction="studentTrends[3]?.direction"
        :trend-value="studentTrends[3]?.value"
        :trend-label="studentTrends[3]?.label"
      />
    </template>
    <template v-else>
      <HomeTrendStat
        label="已布置作业"
        :value="stats.totalHomework"
        :trend-direction="teacherTrends[0]?.direction"
        :trend-value="teacherTrends[0]?.value"
        :trend-label="teacherTrends[0]?.label"
      />
      <HomeTrendStat
        label="学生总数"
        :value="stats.totalStudents"
        :trend-direction="teacherTrends[1]?.direction"
        :trend-value="teacherTrends[1]?.value"
        :trend-label="teacherTrends[1]?.label"
      />
      <HomeTrendStat
        label="作业提交率"
        :value="submissionRate + '%'"
        :value-color="submissionRateColor"
        :trend-direction="teacherTrends[2]?.direction"
        :trend-value="teacherTrends[2]?.value"
        :trend-label="teacherTrends[2]?.label"
      />
      <HomeTrendStat
        label="考试通过率"
        :value="passRate + '%'"
        :value-color="passRateColor"
        :trend-direction="teacherTrends[3]?.direction"
        :trend-value="teacherTrends[3]?.value"
        :trend-label="teacherTrends[3]?.label"
      />
    </template>
  </div>
</template>

<script setup>
import HomeTrendStat from './HomeTrendStat.vue';

defineProps({
  isStudent: Boolean,
  stats: Object,
  submissionRate: [Number, String],
  passRate: [Number, String],
  submissionRateColor: String,
  passRateColor: String,
  submissionRateClass: String,
  passRateClass: String,
  // 向后兼容：趋势数据，未传则不显示趋势箭头
  studentTrends: { type: Array, default: () => [] },
  teacherTrends: { type: Array, default: () => [] },
});
</script>

<style scoped lang="scss">
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
