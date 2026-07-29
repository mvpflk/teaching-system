<template>
  <div>
    <div ref="chartRef" class="kr-chart" style="width:100%;height:280px"></div>
    <div class="kr-legend">
      <span class="kr-legend-item"><span class="kr-dot kr-dot--weak"></span>薄弱</span>
      <span class="kr-legend-item"><span class="kr-dot kr-dot--learning"></span>发展中</span>
      <span class="kr-legend-item"><span class="kr-dot kr-dot--mastered"></span>已掌握</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import echarts from '@/utils/echarts'
import { CHART_COLORS } from '@/utils/chartColors'
import { getStudentKnowledgeRadar, getTeacherKnowledgeRadar } from '@/api/analytics'

const props = defineProps({ studentId: { type: Number, default: null }, subject: { type: String, default: '' } })
const chartRef = ref(null)
let chart = null

async function load() {
  if (!chartRef.value) return
  const isTeacher = props.studentId != null
  const fn = isTeacher ? () => getTeacherKnowledgeRadar(props.studentId, props.subject) : () => getStudentKnowledgeRadar(props.subject)
  try {
    const res = await fn()
    if (res.code !== 200 || !res.data?.length) return
    const data = res.data.slice(0, 8) // TOP8知识点
    if (chart) chart.dispose()
    chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: {
        formatter: (params) => {
          if (!params.name) return ''
          const d = data.find(item => ('KP#' + item.nodeId) === params.name || item.nodeName === params.name)
          if (!d) return params.name + '<br/>掌握度: ' + params.value + '%'
          const statusLabel = { weak: '薄弱', learning: '发展中', mastered: '已掌握' }[d.status] || d.status || ''
          return `<b>${d.nodeName || params.name}</b><br/>掌握度: ${d.masteryPercent}%<br/>状态: ${statusLabel}`
        }
      },
      legend: { show: false },
      radar: {
        center: ['50%', '50%'], radius: '65%',
        indicator: data.map(d => ({ name: (d.nodeName || 'KP#' + d.nodeId).length > 6 ? (d.nodeName || '').substring(0, 5) + '…' : (d.nodeName || 'KP#' + d.nodeId), max: 100 })),
        axisName: { fontSize: 10, color: CHART_COLORS.info }
      },
      series: [{
        type: 'radar',
        data: [{ value: data.map(d => d.masteryPercent), name: '掌握度', areaStyle: { color: 'rgba(67,97,238,0.15)' }, lineStyle: { color: CHART_COLORS.primary }, itemStyle: { color: CHART_COLORS.primary } }]
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

<style scoped>
.kr-chart { width: 100%; height: 280px; }
.kr-legend { display: flex; justify-content: center; gap: 16px; margin-top: 4px; font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); }
.kr-legend-item { display: flex; align-items: center; gap: 4px; }
.kr-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; }
.kr-dot--weak { background: var(--el-color-danger); }
.kr-dot--learning { background: var(--el-color-warning); }
.kr-dot--mastered { background: var(--el-color-success); }
</style>
