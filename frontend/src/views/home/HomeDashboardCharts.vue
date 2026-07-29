<template>
  <!-- 骨架屏 -->
  <div v-if="!dashData" class="charts-grid mb-24">
    <div v-for="i in 4" :key="i" class="chart-card chart-card--skeleton">
      <div class="skeleton-header">
        <div class="skeleton-line skeleton-line--title"></div>
        <div class="skeleton-line skeleton-line--value"></div>
      </div>
      <div class="skeleton-chart">
        <div class="skeleton-ring"></div>
      </div>
    </div>
  </div>

  <div v-else class="charts-grid mb-24">
    <!-- 作业提交率 -->
    <div class="chart-card">
      <div class="chart-card__icon chart-card__icon--primary">
        <el-icon><DocumentCopy /></el-icon>
      </div>
      <div class="chart-card__header">
        <h4 class="chart-card__title">作业提交率</h4>
        <span class="chart-card__value" :style="{ color: submissionRateColor }">
          {{ submissionRate }}<span class="chart-card__unit">%</span>
        </span>
      </div>
      <div ref="hwChartRef" class="chart-mini"></div>
    </div>

    <!-- 考试通过率 -->
    <div class="chart-card">
      <div class="chart-card__icon chart-card__icon--success">
        <el-icon><Trophy /></el-icon>
      </div>
      <div class="chart-card__header">
        <h4 class="chart-card__title">考试通过率</h4>
        <span class="chart-card__value" :style="{ color: passRateColor }">
          {{ passRate }}<span class="chart-card__unit">%</span>
        </span>
      </div>
      <div ref="examChartRef" class="chart-mini"></div>
    </div>

    <!-- 签到趋势 -->
    <div class="chart-card">
      <div class="chart-card__icon chart-card__icon--warning">
        <el-icon><Calendar /></el-icon>
      </div>
      <div class="chart-card__header">
        <h4 class="chart-card__title">签到趋势</h4>
        <span class="chart-card__value">
          {{ signTotal }}<span class="chart-card__unit">次</span>
        </span>
      </div>
      <div ref="signChartRef" class="chart-mini"></div>
    </div>

    <!-- 积分分布 -->
    <div v-if="dashData.creditDistribution" class="chart-card">
      <div class="chart-card__icon chart-card__icon--info">
        <el-icon><Coin /></el-icon>
      </div>
      <div class="chart-card__header">
        <h4 class="chart-card__title">积分分布</h4>
        <span class="chart-card__value">
          {{ creditTotal }}<span class="chart-card__unit">人</span>
        </span>
      </div>
      <div ref="creditChartRef" class="chart-mini"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted, computed } from 'vue'
import echarts from '@/utils/echarts'
import { CHART_COLORS } from '@/utils/chartColors'
import { DocumentCopy, Trophy, Calendar, Coin } from '@element-plus/icons-vue'

const props = defineProps({
  dashData: { type: Object, default: null },
  submissionRate: [Number, String],
  passRate: [Number, String],
  submissionRateColor: String,
  passRateColor: String
})

const hwChartRef = ref(null)
const examChartRef = ref(null)
const signChartRef = ref(null)
const creditChartRef = ref(null)
const chartInstances = []

const signTotal = computed(() => {
  const t = props.dashData?.signTrend
  if (!t || !t.length) return 0
  return t.reduce((s, i) => s + (i.count || 0), 0)
})

const creditTotal = computed(() => {
  const d = props.dashData?.creditDistribution
  if (!d || !d.length) return 0
  return d.reduce((s, i) => s + (i.count || 0), 0)
})

const renderCharts = (data) => {
  if (hwChartRef.value) {
    const c = echarts.init(hwChartRef.value)
    chartInstances.push(c)
    c.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['65%', '85%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data: [
          { value: data.submittedStudents || 0, name: '已提交', itemStyle: { color: CHART_COLORS.emerald } },
          { value: Math.max(0, (data.totalStudents || 0) - (data.submittedStudents || 0)), name: '未提交', itemStyle: { color: 'var(--ed-chart-gray, var(--bg-secondary))' } }
        ]
      }]
    })
  }

  if (examChartRef.value) {
    const c = echarts.init(examChartRef.value)
    chartInstances.push(c)
    const pr = Math.round((data.examPassRate || 0) * 100)
    c.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['65%', '85%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data: [
          { value: pr, name: '通过', itemStyle: { color: CHART_COLORS.emerald } },
          { value: 100 - pr, name: '未通过', itemStyle: { color: 'var(--ed-chart-gray, var(--bg-secondary))' } }
        ]
      }]
    })
  }

  if (signChartRef.value && data.signTrend) {
    const c = echarts.init(signChartRef.value)
    chartInstances.push(c)
    c.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 0, right: 0, top: 4, bottom: 0 },
      xAxis: { show: false, type: 'category', data: data.signTrend.map(s => s.date.slice(5)) },
      yAxis: { show: false, type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        data: data.signTrend.map(s => s.count),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'var(--primary-color)' },
              { offset: 1, color: 'var(--primary-light)' }
            ]
          },
          borderRadius: [3, 3, 0, 0]
        },
        barWidth: '60%'
      }]
    })
  }

  if (creditChartRef.value && data.creditDistribution) {
    const c = echarts.init(creditChartRef.value)
    chartInstances.push(c)
    c.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 0, right: 0, top: 4, bottom: 0 },
      xAxis: { show: false, type: 'category', data: data.creditDistribution.map(d => d.range) },
      yAxis: { show: false, type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        data: data.creditDistribution.map(d => d.count),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'var(--info-color)' },
              { offset: 1, color: 'var(--info-light)' }
            ]
          },
          borderRadius: [3, 3, 0, 0]
        },
        barWidth: '60%'
      }]
    })
  }
}

watch(() => props.dashData, (data) => {
  if (data) nextTick(() => renderCharts(data))
}, { immediate: true })

onUnmounted(() => {
  chartInstances.forEach(c => { try { c.dispose() } catch {} })
  chartInstances.length = 0
})
</script>

<style scoped>
.charts-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--ed-chart-grid-gap, 16px);
}

.chart-card {
  position: relative;
  background: var(--ed-chart-bg, var(--bg-card));
  border-radius: var(--ed-chart-radius, var(--ed-radius-xl, 16px));
  padding: var(--ed-chart-padding, 20px);
  border: 1px solid var(--ed-chart-border, var(--border-light));
  display: flex;
  flex-direction: column;
  gap: var(--ed-chart-gap, 12px);
  transition: all var(--ed-transition-normal, 0.25s ease);
  overflow: hidden;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--ed-chart-hover-shadow, var(--ed-shadow-md, 0 4px 16px rgba(0, 0, 0, 0.08)));
  border-color: var(--ed-chart-hover-border, var(--primary-light));
}

/* === 图标 === */

.chart-card__icon {
  width: var(--ed-chart-icon-size, 36px);
  height: var(--ed-chart-icon-size, 36px);
  border-radius: var(--ed-chart-icon-radius, var(--ed-radius-md, 8px));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.chart-card__icon--primary {
  background: var(--ed-chart-icon-primary-bg, var(--primary-light));
  color: var(--ed-chart-icon-primary-color, var(--primary-color));
}

.chart-card__icon--success {
  background: var(--ed-chart-icon-success-bg, var(--success-light));
  color: var(--ed-chart-icon-success-color, var(--success-color));
}

.chart-card__icon--warning {
  background: var(--ed-chart-icon-warning-bg, var(--warning-light));
  color: var(--ed-chart-icon-warning-color, var(--warning-color));
}

.chart-card__icon--info {
  background: var(--ed-chart-icon-info-bg, var(--info-light));
  color: var(--ed-chart-icon-info-color, var(--info-color));
}

/* === 头部 === */

.chart-card__header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chart-card__title {
  margin: 0;
  font-size: var(--ed-chart-title-size, var(--ed-fs-sm, 13px));
  font-weight: var(--ed-chart-title-weight, 500);
  color: var(--ed-chart-title-color, var(--text-secondary));
  line-height: 1.4;
}

.chart-card__value {
  font-size: var(--ed-chart-value-size, 24px);
  font-weight: var(--ed-chart-value-weight, 700);
  color: var(--ed-chart-value-color, var(--text-primary));
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}

.chart-card__unit {
  font-size: var(--ed-chart-unit-size, var(--ed-fs-sm, 13px));
  font-weight: var(--ed-fw-medium, 500);
  color: var(--ed-chart-unit-color, var(--text-secondary));
  margin-left: 2px;
}

/* === 图表 === */

.chart-mini {
  height: var(--ed-chart-height, 80px);
  width: 100%;
}

/* === 骨架屏 === */

.chart-card--skeleton {
  pointer-events: none;
}

.skeleton-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.skeleton-line {
  height: 12px;
  border-radius: var(--ed-radius-sm, 4px);
  background: linear-gradient(
    90deg,
    var(--ed-skeleton-from, var(--bg-section)) 25%,
    var(--ed-skeleton-via, var(--bg-secondary)) 50%,
    var(--ed-skeleton-to, var(--bg-section)) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line--title {
  width: 60%;
  height: 12px;
}

.skeleton-line--value {
  width: 40%;
  height: 24px;
}

.skeleton-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
}

.skeleton-ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(
    90deg,
    var(--ed-skeleton-from, var(--bg-section)) 25%,
    var(--ed-skeleton-via, var(--bg-secondary)) 50%,
    var(--ed-skeleton-to, var(--bg-section)) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  position: relative;
}

.skeleton-ring::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--ed-chart-bg, var(--bg-card));
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* === 响应式 === */

@media (max-width: 1024px) {
  .charts-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-card {
    padding: var(--ed-chart-padding-mobile, 16px);
  }

  .chart-card__value {
    font-size: var(--ed-chart-value-size-mobile, 20px);
  }
}
</style>
