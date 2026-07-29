<template>
  <div class="student-weakness-panel">
    <!-- 图表区：薄弱 TOP10 + 诊断趋势 -->
    <div class="charts-grid">
      <div class="chart-card">
        <h4 class="section-title">班级薄弱知识点 TOP10</h4>
        <div ref="weakChartRef" class="chart-container"></div>
        <EmptyState
          v-if="weakNodes.length === 0"
          icon="DataAnalysis"
          :title="classId ? '暂无薄弱数据' : '选择班级查看薄弱分析'"
          :description="classId ? '可能是因为该班级无错题记录' : '在上方筛选栏中选择目标班级'"
        />
      </div>
      <div class="chart-card">
        <h4 class="section-title">诊断平均分趋势</h4>
        <div ref="trendChartRef" class="chart-container"></div>
        <EmptyState
          v-if="diagnosisTrend.length === 0"
          icon="TrendCharts"
          :title="classId ? '暂无诊断趋势数据' : '选择班级查看趋势'"
          :description="classId ? '暂无诊断记录' : '在上方筛选栏中选择目标班级'"
        />
      </div>
    </div>

    <!-- 全班成长曲线 -->
    <div v-if="classId" class="chart-card chart-card--full" style="margin-top:16px">
      <h4 class="section-title">全班成长曲线</h4>
      <div ref="classGrowthRef" class="chart-container chart-container--tall"></div>
      <EmptyState
        v-if="classCurveStudents.length === 0"
        icon="TrendCharts"
        title="暂无成长数据"
        description="该班级暂无学生成长曲线记录"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted, nextTick } from 'vue'
import { getClassWeaknesses } from '@/api/precision'
import { getTeacherClassGrowthCurves } from '@/api/analytics'
import EmptyState from '@/components/common/EmptyState.vue'
import echarts from '@/utils/echarts'
import { CHART_COLORS, CHART_COLOR_ARRAY, CHART_GRADIENT } from '@/utils/chartColors'

const props = defineProps({
  classId: { type: [String, Number], default: '' },
  filterSubject: { type: String, default: '' }
})

const emit = defineEmits(['update:weakNodes'])

const weakChartRef = ref(null)
const trendChartRef = ref(null)
const classGrowthRef = ref(null)
let weakChart = null
let trendChart = null
let classGrowthChart = null
const weakNodes = ref([])
const diagnosisTrend = ref([])
const classCurveStudents = ref([])

async function loadWeaknessCharts() {
  if (!props.classId) return
  const res = await getClassWeaknesses(props.classId)
  if (res.code === 200) {
    weakNodes.value = res.data?.weakNodes || []
    diagnosisTrend.value = res.data?.diagnosisTrend || []
    emit('update:weakNodes', weakNodes.value)
  }
  await nextTick()
  renderWeakChart()
  renderTrendChart()
  loadClassGrowthChart()
}

async function loadClassGrowthChart() {
  if (!props.classId) return
  const res = await getTeacherClassGrowthCurves(props.classId, props.filterSubject || null)
  if (res.code !== 200 || !res.data?.length) { classCurveStudents.value = []; return }
  classCurveStudents.value = res.data
  await nextTick()
  renderClassGrowthChart()
}

function renderWeakChart() {
  if (!weakChartRef.value) return
  if (weakChart && !weakChart.isDisposed()) weakChart.dispose()
  if (weakNodes.value.length === 0) return

  const names = weakNodes.value.map(n => n.name)
  const counts = weakNodes.value.map(n => n.errorCount)

  weakChart = echarts.init(weakChartRef.value)
  weakChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '16%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, fontSize: 11, interval: 0 } },
    yAxis: { type: 'value', name: '错误次数', minInterval: 1 },
    series: [{
      type: 'bar',
      data: counts,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: CHART_COLORS.danger },
          { offset: 1, color: CHART_COLORS.rose }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      label: { show: true, position: 'top', fontSize: 12 }
    }]
  })
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart && !trendChart.isDisposed()) trendChart.dispose()
  if (diagnosisTrend.value.length === 0) return

  const dates = diagnosisTrend.value.map(d => d.date)
  const scores = diagnosisTrend.value.map(d => d.averageScore)

  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '8%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', name: '平均分', min: 0, max: 100 },
    series: [{
      type: 'line',
      data: scores,
      smooth: true,
      lineStyle: { width: 3, color: CHART_COLORS.primary },
      itemStyle: { color: CHART_COLORS.primary },
      areaStyle: { color: CHART_GRADIENT.primaryArea },
      markLine: { data: [{ type: 'average', name: '平均' }], label: { fontSize: 11 } },
      markPoint: { data: [{ type: 'max', name: '最高分' }, { type: 'min', name: '最低分' }] }
    }]
  })
}

function renderClassGrowthChart() {
  if (!classGrowthRef.value) return
  if (classGrowthChart && !classGrowthChart.isDisposed()) classGrowthChart.dispose()
  const data = classCurveStudents.value
  if (!data.length) return

  const weekSet = new Set()
  data.forEach(s => (s.curve || []).forEach(p => weekSet.add(p.week)))
  const weeks = [...weekSet].sort()

  const series = []
  data.forEach((s, i) => {
    const curveMap = {}
    ;(s.curve || []).forEach(p => { curveMap[p.week] = p.masteryPercent })
    const values = weeks.map(w => curveMap[w] ?? null)
    if (s.isAverage) {
      series.push({
        type: 'line', name: s.studentName, data: values, smooth: true,
        lineStyle: { width: 3, color: CHART_COLORS.danger },
        itemStyle: { color: CHART_COLORS.danger },
        symbol: 'diamond', symbolSize: 6,
        z: 10,
        emphasis: { focus: 'series' }
      })
    } else {
      series.push({
        type: 'line', name: s.studentName, data: values, smooth: true,
        lineStyle: { width: 1, color: CHART_COLOR_ARRAY[i % CHART_COLOR_ARRAY.length], opacity: 0.35 },
        itemStyle: { color: CHART_COLOR_ARRAY[i % CHART_COLOR_ARRAY.length] },
        symbol: 'none',
        emphasis: { focus: 'series', lineStyle: { width: 2.5, opacity: 1 } }
      })
    }
  })

  classGrowthChart = echarts.init(classGrowthRef.value)
  classGrowthChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { type: 'scroll', bottom: 0, textStyle: { fontSize: 11 }, data: data.map(s => s.studentName) },
    grid: { left: '3%', right: '6%', bottom: '12%', top: '6%', containLabel: true },
    xAxis: { type: 'category', data: weeks, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', name: '掌握度%', min: 0, max: 100, axisLabel: { fontSize: 10 } },
    series
  })
}

function handleResize() {
  if (weakChart && !weakChart.isDisposed()) weakChart.resize()
  if (trendChart && !trendChart.isDisposed()) trendChart.resize()
  if (classGrowthChart && !classGrowthChart.isDisposed()) classGrowthChart.resize()
}

watch(() => props.classId, loadWeaknessCharts)
watch(() => props.filterSubject, () => { if (props.classId) loadClassGrowthChart() })

onUnmounted(() => {
  if (weakChart && !weakChart.isDisposed()) weakChart.dispose()
  if (trendChart && !trendChart.isDisposed()) trendChart.dispose()
  if (classGrowthChart && !classGrowthChart.isDisposed()) classGrowthChart.dispose()
})

defineExpose({ loadWeaknessCharts, loadClassGrowthChart, handleResize })
</script>

<style scoped>
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: var(--spacing-md);
}
.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
}
.chart-card--full {
  margin-bottom: var(--spacing-md);
}
.chart-container {
  width: 100%;
  height: 280px;
}
.chart-container--tall {
  height: 340px;
}
.section-title {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .chart-container {
    height: 220px;
  }
  .chart-container--tall {
    height: 250px;
  }
  .chart-card {
    padding: 12px;
  }
}
@media (max-width: 420px) {
  .chart-container {
    height: 200px;
  }
  .chart-container--tall {
    height: 210px;
  }
}
</style>
