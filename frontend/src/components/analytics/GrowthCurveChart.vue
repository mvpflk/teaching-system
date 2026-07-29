<template>
  <div ref="chartRef" class="gc-chart" style="width:100%;height:300px"></div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import echarts from '@/utils/echarts'
import { CHART_COLORS } from '@/utils/chartColors'
import { getStudentGrowthCurve, getTeacherGrowthCurve } from '@/api/analytics'

const props = defineProps({ studentId: { type: Number, default: null }, subject: { type: String, default: '' } })
const chartRef = ref(null)
let chart = null

async function load() {
  if (!chartRef.value) return
  const isTeacher = props.studentId != null
  const fn = isTeacher ? () => getTeacherGrowthCurve(props.studentId, props.subject) : () => getStudentGrowthCurve(props.subject)
  try {
    const res = await fn()
    if (res.code !== 200 || !res.data?.length) return
    const data = res.data
    if (chart) chart.dispose()
    chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: 20, right: 20, bottom: 30, left: 40, containLabel: true },
      xAxis: { type: 'category', data: data.map(d => d.week), axisLabel: { fontSize: 10, rotate: 30 } },
      yAxis: { type: 'value', name: '掌握度%', min: 0, max: 100, axisLabel: { fontSize: 10 } },
      series: [{
        type: 'line', data: data.map(d => d.masteryPercent), smooth: true,
        lineStyle: { color: CHART_COLORS.primary, width: 2 },
        itemStyle: { color: CHART_COLORS.primary },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(67,97,238,0.08)' }, { offset: 1, color: 'rgba(67,97,238,0)' }]) },
        markPoint: { symbol: 'circle', symbolSize: 8, data: [{ type: 'max', name: '最高', label: { show: true, fontSize: 11, fontWeight: 600, color: CHART_COLORS.primary, formatter: '{c}%' }, itemStyle: { color: CHART_COLORS.primary, borderColor: '#fff', borderWidth: 2 } }] }
      }]
    })
  } catch {}
}

let resizeHandler
onMounted(async () => { await nextTick(); load(); resizeHandler = () => chart?.resize(); window.addEventListener('resize', resizeHandler) })
onUnmounted(() => { window.removeEventListener('resize', resizeHandler); chart?.dispose() })
watch(() => props.studentId, load)
watch(() => props.subject, load)
</script>
