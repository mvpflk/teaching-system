<template>
  <div class="learning-profile">
    <el-page-header @back="router.back()">
      <template #content>
        <span>学习画像</span>
      </template>
    </el-page-header>

    <div class="lp-subject-bar">
      <el-select
        v-model="selectedSubject"
        placeholder="全部学科"
        clearable
        size="small"
        style="width:180px"
        @change="loadProfile"
      >
        <el-option label="全部学科" value="" />
        <el-option
          v-for="s in subjects"
          :key="s"
          :label="s"
          :value="s"
        />
      </el-select>
    </div>

    <div v-if="loading" style="text-align:center;padding:60px">
      <el-icon class="is-loading" style="font-size:32px"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <template v-else-if="profile">
      <!-- Row 1: 总览卡片 -->
      <el-row :gutter="16" class="lp-overview">
        <el-col :xs="12" :sm="6">
          <div class="lp-stat-card">
            <el-statistic title="总任务数" :value="profile.overview?.totalTasks || 0" />
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="lp-stat-card">
            <el-statistic
              title="平均得分率"
              :value="profile.overview?.avgScore || 0"
              suffix="%"
              :precision="1"
            />
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="lp-stat-card">
            <el-statistic title="积分排名" :value="profile.overview?.ranking || '-'">
              <template v-if="profile.overview?.ranking" #suffix>/ 全校</template>
            </el-statistic>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="lp-stat-card">
            <el-statistic title="薄弱知识点" :value="profile.weakKnowledge?.length || 0" />
          </div>
        </el-col>
      </el-row>

      <!-- Row 2: 趋势图 + 雷达图 -->
      <el-row :gutter="16" class="lp-charts">
        <el-col :xs="24" :md="14">
          <div class="lp-chart-card">
            <h4>学科均分趋势</h4>
            <div ref="trendChartRef" style="width:100%;height:280px"></div>
            <el-empty v-if="!profile.scoreTrend?.length" description="暂无趋势数据" :image-size="60" />
          </div>
        </el-col>
        <el-col :xs="24" :md="10">
          <div class="lp-chart-card">
            <h4>题型能力雷达图</h4>
            <div ref="radarChartRef" style="width:100%;height:280px"></div>
            <el-empty v-if="!profile.typeAbility?.length" description="暂无题型数据" :image-size="60" />
          </div>
        </el-col>
      </el-row>

      <!-- Row 3: 最强/最弱知识点 -->
      <el-row :gutter="16" class="lp-knowledge">
        <el-col :xs="24" :md="12">
          <div class="lp-know-card lp-know-top">
            <h4>最强知识点 TOP5</h4>
            <div v-for="(item, i) in profile.topKnowledge" :key="i" class="lp-know-item">
              <span class="lp-know-rank" :style="{color:'var(--el-color-success)'}">{{ i + 1 }}</span>
              <span class="lp-know-name">{{ item.name }}</span>
              <span class="lp-know-rate" style="color:var(--el-color-success)">{{ (item.accuracy * 100).toFixed(0) }}%</span>
            </div>
            <el-empty v-if="!profile.topKnowledge?.length" description="暂无数据" :image-size="60" />
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="lp-know-card lp-know-bottom">
            <h4>最弱知识点 BOTTOM5</h4>
            <div v-for="(item, i) in profile.weakKnowledge" :key="i" class="lp-know-item">
              <span class="lp-know-rank" :style="{color:'var(--el-color-danger)'}">{{ i + 1 }}</span>
              <span class="lp-know-name">{{ item.name }}</span>
              <span class="lp-know-rate" style="color:var(--el-color-danger)">{{ (item.accuracy * 100).toFixed(0) }}%</span>
            </div>
            <el-empty v-if="!profile.weakKnowledge?.length" description="暂无数据" :image-size="60" />
          </div>
        </el-col>
      </el-row>

      <!-- Row 4: 提交行为 -->
      <div class="lp-chart-card" style="margin-top:16px">
        <h4>提交行为</h4>
        <div v-if="profile.submitBehavior" ref="submitChartRef" style="width:100%;height:260px"></div>
      </div>
    </template>

    <el-empty v-else description="暂无学习数据" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { getLearningProfile } from '@/api/task'

const router = useRouter()
const profile = ref(null), loading = ref(false)
const selectedSubject = ref('')
const subjects = ref([])

const trendChartRef = ref(null), radarChartRef = ref(null), submitChartRef = ref(null)
let trendChart = null, radarChart = null, submitChart = null

onMounted(async () => {
  await loadSubjects()
  await loadProfile()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose(); trendChart = null
  radarChart?.dispose(); radarChart = null
  submitChart?.dispose(); submitChart = null
})

function handleResize() {
  trendChart?.resize(); radarChart?.resize(); submitChart?.resize()
}

async function loadSubjects() {
  try {
    const res = await import('@/api/system').then(m => m.getDictSubjects())
    if (res?.data) subjects.value = res.data.map(s => typeof s === 'object' ? s.name : s).filter(Boolean)
  } catch { /* use defaults */ }
}

async function loadProfile() {
  loading.value = true
  try {
    const res = await getLearningProfile(selectedSubject.value || undefined)
    profile.value = res.data
    await nextTick()
    renderTrendChart()
    renderRadarChart()
    renderSubmitChart()
  } catch { profile.value = null }
  finally { loading.value = false }
}

function renderTrendChart() {
  if (!trendChartRef.value || !profile.value?.scoreTrend?.length) return
  if (trendChart) trendChart.dispose()
  import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    trendChart = echarts.init(trendChartRef.value)

    const months = profile.value.scoreTrend.map(d => d.month)
    const subjectSet = new Set()
    profile.value.scoreTrend.forEach(d => {
      Object.keys(d).forEach(k => { if (k !== 'month') subjectSet.add(k) })
    })
    const allSubjects = [...subjectSet]
    const colors = [cssVar('--primary-color'), cssVar('--el-color-success'), cssVar('--el-color-warning'), cssVar('--el-color-danger'), cssVar('--chart-5'), cssVar('--chart-6')]
    const series = allSubjects.map((s, i) => ({
      name: s, type: 'line', data: months.map(m => {
        const item = profile.value.scoreTrend.find(d => d.month === m)
        return item ? (item[s] || 0) : 0
      }),
      smooth: true, color: colors[i % colors.length]
    }))

    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, data: allSubjects },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '8%', containLabel: true },
      xAxis: { type: 'category', data: months },
      yAxis: { type: 'value', name: '均分', max: 100 },
      series
    })
  })
}

function renderRadarChart() {
  if (!radarChartRef.value || !profile.value?.typeAbility?.length) return
  if (radarChart) radarChart.dispose()
  import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    radarChart = echarts.init(radarChartRef.value)

    const TYPES = { SINGLE_CHOICE: '单选', MULTI_CHOICE: '多选', TRUE_FALSE: '判断', FILL_IN: '填空', ESSAY: '简答' }
    const indicators = profile.value.typeAbility.map(t => ({
      name: TYPES[t.type] || t.type, max: 1
    }))
    const values = profile.value.typeAbility.map(t => t.accuracy)

    radarChart.setOption({
      tooltip: {},
      radar: { indicator: indicators, center: ['50%', '52%'], radius: '65%' },
      series: [{
        type: 'radar',
        data: [{ value: values, name: '正确率', areaStyle: { color: `rgba(${cssVar('--primary-color-rgb')}, 0.2)` } }]
      }]
    })
  })
}

function renderSubmitChart() {
  if (!submitChartRef.value || !profile.value?.submitBehavior) return
  if (submitChart) submitChart.dispose()
  import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    submitChart = echarts.init(submitChartRef.value)

    const b = profile.value.submitBehavior
    submitChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}: {c}' },
        data: [
          { value: b.onTime || 0, name: '准时提交', itemStyle: { color: cssVar('--el-color-success') } },
          { value: b.late || 0, name: '迟交', itemStyle: { color: cssVar('--el-color-warning') } },
          { value: b.missing || 0, name: '未交', itemStyle: { color: cssVar('--el-color-danger') } }
        ]
      }]
    })
  })
}
</script>

<style scoped>
.learning-profile { max-width: 960px; margin: 0 auto; padding-bottom: 24px; }
.lp-subject-bar { margin: 12px 0; display: flex; align-items: center; gap: 12px; }
.lp-overview { margin-bottom: 16px; }
.lp-stat-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; text-align: center; }
.lp-charts { margin-bottom: 16px; }
.lp-chart-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 16px; }
.lp-chart-card h4 { margin: 0 0 12px; font-size: var(--fs-md); color: var(--text-primary); }
.lp-know-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 16px; }
.lp-know-card h4 { margin: 0 0 12px; font-size: var(--fs-md); color: var(--text-primary); }
.lp-know-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid var(--border-light); }
.lp-know-item:last-child { border-bottom: none; }
.lp-know-rank { font-weight: 700; font-size: var(--fs-lg); min-width: 24px; }
.lp-know-name { flex: 1; font-size: var(--fs-sm); color: var(--text-primary); }
.lp-know-rate { font-weight: 600; font-size: var(--fs-sm); }
.lp-knowledge { margin-bottom: 16px; }

@media (max-width: 768px) {
  .lp-overview .el-col { margin-bottom: 8px; }
  .lp-stat-card { padding: 12px; }
}
</style>
