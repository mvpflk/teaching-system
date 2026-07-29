<template>
  <div class="class-profile">
    <div class="back-bar">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
    </div>

    <div v-loading="loading" class="profile-content">
      <div class="profile-header">
        <div class="profile-icon">🏫</div>
        <div>
          <h2 class="profile-name">班级档案 — {{ profile.grade }} {{ profile.className }}</h2>
          <div class="profile-meta">班级ID: {{ profile.classId }} · {{ profile.studentCount }} 名学生</div>
        </div>
      </div>

      <div class="stat-grid">
        <div class="stat-card"><span class="s-val">{{ profile.grade }}</span><span class="s-lbl">年级</span></div>
        <div class="stat-card"><span class="s-val">{{ profile.className }}</span><span class="s-lbl">班级</span></div>
        <div class="stat-card"><span class="s-val">{{ profile.studentCount }}</span><span class="s-lbl">学生数</span></div>
      </div>

      <div class="chart-section">
        <div class="chart-box">
          <h4>五维评价雷达图</h4>
          <div id="radar-chart" style="width:100%;height:360px" />
        </div>
        <div class="chart-box">
          <h4>近5周均分趋势</h4>
          <div id="score-trend-chart" style="width:100%;height:280px" />
        </div>
        <div class="chart-box">
          <h4>近5周提交率趋势</h4>
          <div id="submit-trend-chart" style="width:100%;height:280px" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import echarts, { cssVar } from '@/utils/echarts'
import { getClassProfile } from '@/api/inspector'

const route = useRoute()
const loading = ref(true)
const profile = ref({})
let radarChart = null, scoreChart = null, submitChart = null

const resizeHandler = () => { radarChart?.resize(); scoreChart?.resize(); submitChart?.resize() }

const initCharts = () => {
  const dims = profile.value.dimensions || {}
  const dimLabels = ['教学成绩', '课堂纪律', '作业完成', '互评参与', '积分表现']
  const dimValues = [
    dims.teaching?.score ?? 50, dims.discipline?.score ?? 50,
    dims.homework?.score ?? 50, dims.peerReview?.score ?? 50,
    dims.credit?.score ?? 50
  ]
  const recentScores = profile.value.recentScores || []
  const recentRates = profile.value.recentSubmissionRates || []

  radarChart = echarts.init(document.getElementById('radar-chart'))
  radarChart.setOption({
    tooltip: {}, radar: {
      indicator: dimLabels.map(n => ({ name: n, max: 100 })),
      shape: 'circle', center: ['50%', '50%'], radius: '65%'
    },
    series: [{
      type: 'radar', data: [{ value: dimValues, name: '评价维度', areaStyle: { color: 'rgba(64,158,255,0.2)' } }],
      lineStyle: { color: cssVar('--primary-color'), width: 2 }, itemStyle: { color: cssVar('--primary-color') }
    }]
  })

  scoreChart = echarts.init(document.getElementById('score-trend-chart'))
  scoreChart.setOption({
    tooltip: { trigger: 'axis' }, grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: recentScores.map(s => s.period) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', data: recentScores.map(s => s.avgScore), smooth: true, areaStyle: { opacity: 0.15 }, itemStyle: { color: cssVar('--primary-color') } }]
  })

  submitChart = echarts.init(document.getElementById('submit-trend-chart'))
  submitChart.setOption({
    tooltip: { trigger: 'axis' }, grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: recentRates.map(s => s.period) },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{ type: 'line', data: recentRates.map(s => s.rate), smooth: true, areaStyle: { opacity: 0.15 }, itemStyle: { color: '#67C23A' } }]
  })
}

onMounted(async () => {
  window.addEventListener('resize', resizeHandler)
  try {
    const res = await getClassProfile(route.params.id)
    if (res.code === 200) { profile.value = res.data; setTimeout(initCharts, 100) }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  radarChart?.dispose(); scoreChart?.dispose(); submitChart?.dispose()
})
</script>

<style scoped lang="scss">
.class-profile { max-width: 1200px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.back-bar { margin-bottom: 16px; }
.profile-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.profile-icon { font-size: 40px; }
.profile-name { font-size: var(--fs-xl); font-weight: 700; margin: 0; }
.profile-meta { font-size: var(--fs-sm); color: var(--text-secondary); margin-top: 4px; }

.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
.stat-card { text-align: center; padding: 16px; background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.stat-card .s-val { display: block; font-size: 22px; font-weight: 700; color: var(--text-primary); }
.stat-card .s-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }

.chart-section { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.chart-box { background: var(--bg-card); border-radius: var(--radius-md); padding: 16px; box-shadow: var(--shadow-sm); h4 { margin: 0 0 12px; font-size: var(--fs-md); color: var(--text-primary); } }
.chart-box:first-child { grid-column: 1 / -1; }

@media (max-width: 768px) {
  .class-profile { padding: var(--spacing-md, 16px); }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-section { grid-template-columns: 1fr; }
}
</style>
