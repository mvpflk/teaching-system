<template>
  <el-dialog
    :model-value="visible"
    :title="studentName + ' · 学习成长'"
    width="760px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-secondary)">加载中...</div>
    <div v-else-if="error" style="text-align:center;padding:40px;color:var(--text-secondary)">
      <p>{{ error }}</p>
      <el-button size="small" style="margin-top:8px" @click="$emit('retry')">重试</el-button>
    </div>
    <template v-else>
      <div v-if="encouragement" class="encourage-box">
        <span class="encourage-icon">{{ encouragement.title?.charAt(0) || '🌟' }}</span>
        <div>
          <div class="encourage-title">{{ encouragement.title }}</div>
          <div class="encourage-detail">{{ encouragement.detail }}</div>
        </div>
      </div>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <h4 class="chart-subtitle">成长曲线</h4>
          <GrowthCurveChart :student-id="studentId" :subject="subject" @error="emitError('成长曲线加载失败')" />
        </el-col>
        <el-col :xs="24" :sm="12">
          <h4 class="chart-subtitle">知识雷达</h4>
          <KnowledgeRadarChart :student-id="studentId" :subject="subject" @error="emitError('知识雷达加载失败')" />
        </el-col>
      </el-row>
      <div v-if="studentRow" class="detail-info">
        <h4 class="chart-subtitle" style="margin-top:16px">学生概览</h4>
        <div class="detail-grid">
          <div><span class="detail-label">最近练习</span><span>{{ studentRow.lastActiveAt ? formatActiveTime(studentRow.lastActiveAt) : '无记录' }}</span></div>
          <div><span class="detail-label">本周练习</span><span>{{ studentRow.weeklyPracticeCount || 0 }} 次</span></div>
          <div><span class="detail-label">数学估分</span><span>{{ studentRow.mathScore || '-' }}</span></div>
          <div><span class="detail-label">英语词汇</span><span>{{ studentRow.engVocab || '-' }}</span></div>
          <div><span class="detail-label">连续周数</span><span>{{ studentRow.streakWeeks || 0 }} 周</span></div>
          <div><span class="detail-label">最新小测</span><span>{{ studentRow.lastTestScore ? studentRow.lastTestScore + '分' : '未提交' }}</span></div>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import GrowthCurveChart from '@/components/analytics/GrowthCurveChart.vue'
import KnowledgeRadarChart from '@/components/analytics/KnowledgeRadarChart.vue'

defineProps({
  visible: Boolean, loading: Boolean, error: String,
  studentName: String, studentId: String, encouragement: Object,
  studentRow: Object, subject: String
})
const emit = defineEmits(['update:visible', 'retry'])
const emitError = (msg) => {} // 图表错误静默降级

function formatActiveTime(isoStr) {
  if (!isoStr) return '从未'
  const now = Date.now()
  const t = new Date(isoStr).getTime()
  const diffMin = Math.floor((now - t) / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return diffMin + '分钟前'
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return diffHour + '小时前'
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return diffDay + '天前'
  return diffDay + '天前'
}
</script>

<style scoped>
.encourage-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: var(--primary-light);
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
}
.encourage-icon { font-size: var(--fs-2xl); }
.encourage-title { font-weight: 600; color: var(--primary-color); }
.encourage-detail { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.chart-subtitle { font-size: var(--fs-sm); font-weight: 600; margin: 0 0 8px 0; color: var(--text-primary); }
.detail-info { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-color); }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; font-size: var(--fs-sm); }
.detail-label { display: block; color: var(--text-secondary); font-size: var(--fs-xs); }
</style>
