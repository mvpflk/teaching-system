<template>
  <div v-loading="loading" class="task-stats">
    <el-row :gutter="16">
      <!-- 每日提交折线 -->
      <el-col :xs="24" :md="12">
        <div class="chart-card"><div class="chart-title">每日提交</div><div ref="lineChart" class="chart-box"></div></div>
      </el-col>
      <!-- 各班均分柱状 -->
      <el-col :xs="24" :md="12">
        <div class="chart-card"><div class="chart-title">各班平均分</div><div ref="barChart" class="chart-box"></div></div>
      </el-col>
      <!-- 分数分布饼图 -->
      <el-col :xs="24" :md="12">
        <div class="chart-card"><div class="chart-title">分数分布</div><div ref="pieChart" class="chart-box"></div></div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { getTaskStats } from '@/api/task'
import echarts, { cssVar } from '@/utils/echarts'

const props = defineProps({ taskId: { type: [Number, String], required: true } })
const loading = ref(false)
const lineChart = ref(null), barChart = ref(null), pieChart = ref(null)
let lineInst = null, barInst = null, pieInst = null

const dispose = (inst) => { try { inst?.dispose() } catch { /* */ } }
const cleanup = () => { dispose(lineInst); dispose(barInst); dispose(pieInst) }

const load = async () => {
  loading.value = true
  try {
    const r = await getTaskStats(props.taskId)
    if (r.code === 200) {
      await nextTick()
      const data = r.data
      renderLine(echarts, data.daily || [])
      renderBar(echarts, data.classStats || [])
      renderPie(echarts, data.dist || [])
    }
  } finally { loading.value = false }
}

const renderLine = (echarts, daily) => {
  if (!lineChart.value) return
  dispose(lineInst); lineInst = echarts.init(lineChart.value)
  lineInst.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: daily.map(d => d.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: daily.map(d => d.count), smooth: true, areaStyle: {} }],
    grid: { left: 40, right: 10, top: 10, bottom: 30 }
  })
}

const renderBar = (echarts, classAvg) => {
  if (!barChart.value || !classAvg.length) return
  dispose(barInst); barInst = echarts.init(barChart.value)
  barInst.setOption({
    tooltip: { trigger: 'axis', formatter: (p) => `${p[0].name}<br/>均分: ${p[0].value} · ${classAvg[p[0].dataIndex]?.count || 0}人` },
    xAxis: { type: 'category', data: classAvg.map(d => d.className || ('班级' + d.classId)) },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: classAvg.map(d => d.avgScore ?? d.avg ?? 0), itemStyle: { color: cssVar('--el-color-success') } }],
    grid: { left: 40, right: 10, top: 10, bottom: 30 }
  })
}

const renderPie = (echarts, dist) => {
  if (!pieChart.value) return
  dispose(pieInst); pieInst = echarts.init(pieChart.value)
  pieInst.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'], data: dist,
      label: { show: true, formatter: '{b}: {c}人' }
    }],
    color: [cssVar('--el-color-success'), cssVar('--primary-color'), cssVar('--el-color-warning'), cssVar('--el-color-danger')]
  })
}

const onResize = () => { lineInst?.resize(); barInst?.resize(); pieInst?.resize() }

watch(() => props.taskId, load)
onMounted(() => { load(); window.addEventListener('resize', onResize) })
onUnmounted(() => { cleanup(); window.removeEventListener('resize', onResize) })
</script>

<style scoped>
.chart-card { background: var(--bg-card); border:1px solid var(--border-light); border-radius: var(--radius-md); padding:16px; margin-bottom:16px; }
.chart-title { font-weight:600; font-size:var(--fs-md); margin-bottom:8px; }
.chart-box { width:100%; height:280px; }
@media (max-width: 768px) { .chart-box { height:200px; } }
</style>
