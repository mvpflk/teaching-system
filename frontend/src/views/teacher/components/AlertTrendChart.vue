<template>
  <div v-if="trendData.length > 1" class="am-trend-card">
    <div class="am-trend-header">近7天预警趋势</div>
    <div ref="chartRef" class="am-trend-chart"></div>
  </div>
</template>

<script setup>
import { ref, watch, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  trendData: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

async function render() {
  if (!chartRef.value || props.trendData.length < 2) return
  try {
    const { default: echarts, cssVar } = await import('@/utils/echarts')
    if (chart) chart.dispose()
    chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 16, top: 16, bottom: 30 },
      xAxis: { type: 'category', data: props.trendData.map(d => d.date), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
      series: [
        { name: '低分', type: 'bar', stack: 'total', data: props.trendData.map(d => d.lowScore || 0), itemStyle: { color: cssVar('--el-color-danger') } },
        { name: '缺交', type: 'bar', stack: 'total', data: props.trendData.map(d => d.missing || 0), itemStyle: { color: cssVar('--el-color-warning') } },
      ]
    })
  } catch { /* echarts load failed */ }
}

watch(() => props.trendData, () => nextTick(render), { deep: false })
onUnmounted(() => chart?.dispose())
</script>

<style scoped>
.am-trend-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 16px; }
.am-trend-header { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.am-trend-chart { height: 160px; }
</style>
