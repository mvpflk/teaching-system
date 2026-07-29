<template>
  <el-dialog
    v-model="analyticsVisible"
    title="课堂互动数据分析"
    :width="isMobile ? '95%' : '800px'"
    destroy-on-close
    append-to-body
    @opened="loadAnalytics"
  >
    <div class="analytics-filter">
      <el-radio-group v-model="analyticsRange" size="small" @change="loadAnalytics">
        <el-radio-button value="7d">最近7天</el-radio-button>
        <el-radio-button value="30d">最近30天</el-radio-button>
        <el-radio-button value="90d">本学期</el-radio-button>
      </el-radio-group>
    </div>
    <div v-if="analyticsLoading" style="text-align:center;padding:40px">
      <el-icon class="is-loading" style="font-size:32px"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
    <template v-else-if="analyticsData">
      <el-row :gutter="16" class="analytics-overview">
        <el-col :xs="12" :sm="6">
          <div class="analytics-stat">
            <span class="as-val">{{ (analyticsData.coverageRate * 100).toFixed(0) }}%</span>
            <span class="as-label">覆盖率 ({{ analyticsData.participatedStudents }}/{{ analyticsData.totalStudents }})</span>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="analytics-stat">
            <span class="as-val">{{ analyticsData.studentDistribution?.length || 0 }}</span>
            <span class="as-label">参与人数</span>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="analytics-stat">
            <span class="as-val">{{ analyticsData.knowledgeAccuracy?.length || 0 }}</span>
            <span class="as-label">互动知识点</span>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="analytics-stat">
            <span class="as-val">{{ analyticsData.topQuestions?.length || 0 }}</span>
            <span class="as-label">热门题目</span>
          </div>
        </el-col>
      </el-row>
      <div class="analytics-section">
        <h4>学生参与分布</h4>
        <div ref="aDistChartRef" style="width:100%;height:280px"></div>
      </div>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <div class="analytics-section">
            <h4>知识点正确率</h4>
            <div ref="aKnowledgeChartRef" style="width:100%;height:260px"></div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="analytics-section">
            <h4>互动趋势</h4>
            <div ref="aTrendChartRef" style="width:100%;height:260px"></div>
          </div>
        </el-col>
      </el-row>
      <div v-if="analyticsData.topQuestions?.length" class="analytics-section">
        <h4>热门题目 TOP5</h4>
        <div v-for="(q, qi) in analyticsData.topQuestions" :key="q.id" class="analytics-q-item">
          <span class="aq-rank">{{ qi + 1 }}</span>
          <span class="aq-content">{{ q.content }}</span>
          <el-tag size="small">使用 {{ q.usageCount }} 次</el-tag>
        </div>
      </div>
    </template>
    <el-empty v-else description="暂无数据" />
  </el-dialog>
</template>

<script setup>
import { ref, nextTick, onUnmounted } from 'vue'
import { getClassroomAnalytics } from '@/api/classroom'
import { Loading } from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { primaryColor, textSecondary, elSuccess } from '@/utils/theme'

const props = defineProps({ classId: [String, Number], isMobile: Boolean })

const analyticsVisible = ref(false)
const analyticsLoading = ref(false)
const analyticsData = ref(null)
const analyticsRange = ref('30d')
const aDistChartRef = ref(null)
const aKnowledgeChartRef = ref(null)
const aTrendChartRef = ref(null)
let aDistChart = null
let aKnowledgeChart = null
let aTrendChart = null

const open = () => { analyticsVisible.value = true }
defineExpose({ open })

async function loadAnalytics() {
  analyticsLoading.value = true
  try {
    const res = await getClassroomAnalytics(props.classId, analyticsRange.value)
    analyticsData.value = res.data
    await nextTick()
    renderAnalyticsCharts()
  } catch { analyticsData.value = null }
  finally { analyticsLoading.value = false }
}

function renderAnalyticsCharts() {
  renderDistChart()
  renderKnowledgeChart()
  renderTrendChart()
}

function getChartColors() {
  const style = getComputedStyle(document.documentElement)
  return {
    text: style.getPropertyValue('--text-secondary').trim() || textSecondary,
    primary: style.getPropertyValue('--primary-color').trim() || primaryColor,
    green: style.getPropertyValue('--el-color-success').trim() || elSuccess,
  }
}

function renderDistChart() {
  if (!aDistChartRef.value || !analyticsData.value?.studentDistribution?.length) return
  if (aDistChart) aDistChart.dispose()
  const colors = getChartColors()
  aDistChart = echarts.init(aDistChartRef.value)
  const dist = analyticsData.value.studentDistribution.slice(0, 15)
  aDistChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '8%', top: '5%', containLabel: true },
    xAxis: { type: 'category', data: dist.map(d => d.studentName), axisLabel: { rotate: 30, fontSize: 11, color: colors.text } },
    yAxis: { type: 'value', name: '互动次数', minInterval: 1, nameTextStyle: { color: colors.text }, axisLabel: { color: colors.text } },
    series: [{ type: 'bar', data: dist.map(d => d.count), itemStyle: { color: colors.primary }, label: { show: true, position: 'top' } }]
  })
}

function renderKnowledgeChart() {
  if (!aKnowledgeChartRef.value || !analyticsData.value?.knowledgeAccuracy?.length) return
  if (aKnowledgeChart) aKnowledgeChart.dispose()
  const colors = getChartColors()
  aKnowledgeChart = echarts.init(aKnowledgeChartRef.value)
  const ka = analyticsData.value.knowledgeAccuracy
  aKnowledgeChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '8%', top: '5%', containLabel: true },
    xAxis: { type: 'value', name: '正确率', max: 1, nameTextStyle: { color: colors.text }, axisLabel: { color: colors.text } },
    yAxis: { type: 'category', data: ka.map(d => d.knowledge), axisLabel: { fontSize: 11, color: colors.text }, inverse: true },
    series: [{ type: 'bar', data: ka.map(d => d.accuracy), itemStyle: { color: colors.green }, label: { show: true, position: 'right', formatter: '{c}' } }]
  })
}

function renderTrendChart() {
  if (!aTrendChartRef.value || !analyticsData.value?.interactionTrend?.length) return
  if (aTrendChart) aTrendChart.dispose()
  const colors = getChartColors()
  aTrendChart = echarts.init(aTrendChartRef.value)
  const trend = analyticsData.value.interactionTrend
  aTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['抽问', '抢答', '投票'], bottom: 0, textStyle: { color: colors.text } },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: trend.map(d => d.week), axisLabel: { color: colors.text } },
    yAxis: { type: 'value', minInterval: 1, nameTextStyle: { color: colors.text }, axisLabel: { color: colors.text } },
    series: [
      { name: '抽问', type: 'line', data: trend.map(d => d.quiz), smooth: true },
      { name: '抢答', type: 'line', data: trend.map(d => d.buzz), smooth: true },
      { name: '投票', type: 'line', data: trend.map(d => d.poll), smooth: true }
    ]
  })
}

onUnmounted(() => {
  aDistChart?.dispose()
  aKnowledgeChart?.dispose()
  aTrendChart?.dispose()
})
</script>

<style scoped lang="scss">
.analytics-filter { margin-bottom: 16px; text-align: center; }
.analytics-overview { margin-bottom: 16px; }
.analytics-stat { background: var(--bg-card); border: 0.5px solid var(--border-light); border-radius: var(--radius-md); padding: 14px; text-align: center; }
.as-val { display: block; font-size: 22px; font-weight: 700; color: var(--el-color-primary); }
.as-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; display: block; }
.analytics-section { margin-bottom: 20px; }
.analytics-section h4 { margin: 0 0 10px; font-size: var(--fs-md); color: var(--text-primary); }
.analytics-q-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 0.5px solid var(--border-light); }
.analytics-q-item:last-child { border-bottom: none; }
.aq-rank { font-weight: 700; font-size: var(--fs-md); color: var(--el-color-primary); min-width: 24px; }
.aq-content { flex: 1; font-size: var(--fs-sm); color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
