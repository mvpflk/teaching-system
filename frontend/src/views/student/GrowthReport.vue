<template>
  <div id="growth-report-print" class="report-page">
    <div class="no-print report-header">
      <el-page-header class="mb-16" @back="router.back()">
        <template #content><span class="page-title">📋 成长报告</span></template>
      </el-page-header>
      <el-button type="primary" @click="window.print()"><el-icon><Printer /></el-icon> 打印报告</el-button>
    </div>

    <div v-loading="loading" class="report-body">
      <!-- 基本信息 -->
      <div class="report-card">
        <h3 class="card-title">📌 基本信息</h3>
        <div class="info-grid">
          <div class="info-cell"><span class="lbl">姓名</span><span>{{ data.basicInfo?.name }}</span></div>
          <div class="info-cell"><span class="lbl">学号</span><span>{{ data.basicInfo?.studentNumber }}</span></div>
          <div class="info-cell"><span class="lbl">班级</span><span>{{ data.basicInfo?.className }}</span></div>
          <div class="info-cell"><span class="lbl">年级</span><span>{{ data.basicInfo?.grade }}</span></div>
        </div>
      </div>

      <!-- 任务统计 -->
      <div class="report-card">
        <h3 class="card-title">📊 任务统计</h3>
        <div class="stat-cards">
          <div class="stat-card"><div class="stat-num">{{ data.taskStats?.submitted || 0 }}</div><div class="stat-label">已提交</div></div>
          <div class="stat-card"><div class="stat-num">{{ data.taskStats?.graded || 0 }}</div><div class="stat-label">已评分</div></div>
          <div class="stat-card"><div class="stat-num">{{ data.taskStats?.avgScore }}</div><div class="stat-label">平均分</div></div>
          <div class="stat-card"><div class="stat-num">{{ data.taskStats?.totalTasks || 0 }}</div><div class="stat-label">总任务</div></div>
        </div>
      </div>

      <!-- 积分趋势 -->
      <div class="report-card">
        <h3 class="card-title">📈 积分变化</h3>
        <div ref="creditChartRef" style="width:100%;height:200px"></div>
        <div class="credit-total">当前积分：{{ creditTotal }}</div>
      </div>

      <!-- 班主任寄语 -->
      <div v-if="data.teacherRemark" class="report-card">
        <h3 class="card-title">💬 班主任寄语</h3>
        <div class="remark-box">{{ data.teacherRemark }}</div>
      </div>

      <!-- 成长足迹 -->
      <div class="report-card">
        <h3 class="card-title">🌱 近期足迹</h3>
        <div v-if="data.timeline?.length" class="timeline-mini">
          <div v-for="e in data.timeline" :key="e.id" class="tl-row">
            <span class="tl-dot">{{ iconDot(e.eventType) }}</span>
            <span class="tl-text">{{ e.title }}</span>
            <span class="tl-time">{{ fmtDate(e.createdAt) }}</span>
          </div>
        </div>
        <EmptyState v-else title="暂无足迹" :icon="Clock" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Printer, Clock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const data = ref({})

const load = async () => {
  loading.value = true
  try { const r = await request({ url: '/student/report', method: 'get' }); if (r.code === 200) data.value = r.data || {} } catch { /* */ }
  finally { loading.value = false }
  renderCreditChart()
}

const creditTotal = computed(() => {
  const trend = data.value.creditTrend || []
  return trend.length ? trend[trend.length - 1].credits : 0
})

const creditChartRef = ref(null)
let creditChart = null

function renderCreditChart() {
  if (!creditChartRef.value || !(data.value.creditTrend || []).length) return
  import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    if (creditChart) creditChart.dispose()
    creditChart = echarts.init(creditChartRef.value)
    const trend = data.value.creditTrend || []
    creditChart.setOption({
      tooltip: { trigger: 'axis', formatter: p => `${p[0].axisValue}<br/>${p[0].marker} 积分: ${p[0].value}` },
      grid: { left: 40, right: 16, top: 16, bottom: 28 },
      xAxis: { type: 'category', data: trend.map(t => t.date?.slice(5)), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: trend.map(t => t.credits || 0), itemStyle: { color: cssVar('--primary-color'), borderRadius: [3, 3, 0, 0] }, barMaxWidth: 20 }]
    })
  })
}

onUnmounted(() => { creditChart?.dispose(); creditChart = null })

const fmtDate = (t) => t ? t.slice(0, 10) : ''
const iconDot = (t) => ({ submit: '📝', grade: '⭐', moral: '🏅', album_mention: '💬' }[t] || '•')

onMounted(load)
</script>

<style scoped>
.report-page { max-width: 800px; margin: 0 auto; padding: 16px; }
.mb-16 { margin-bottom: 16px; }
.page-title { font-size: var(--fs-lg); font-weight: 600; }
.report-body { margin-top: 16px; }
.report-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 20px; margin-bottom: 16px; }
.card-title { font-size: var(--fs-md); font-weight: 600; margin: 0 0 14px 0; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.info-cell { display: flex; flex-direction: column; gap: 2px; }
.info-cell .lbl { font-size: var(--fs-xs); color: var(--text-secondary); }
.info-cell span:last-child { font-size: var(--fs-md); font-weight: 500; }

.stat-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 12px; }
.stat-card { background: var(--bg-section); border-radius: var(--radius-md); padding: 16px; text-align: center; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--primary-color); }
.stat-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }

.credit-total { text-align: center; margin-top: 12px; font-weight: 600; font-size: var(--fs-lg); color: var(--primary-color); }

.remark-box { background: var(--bg-section); padding: 16px; border-radius: var(--radius-md); font-size: var(--fs-md); line-height: 1.8; color: var(--text-regular); white-space: pre-wrap; }

.timeline-mini { font-size: var(--fs-sm); }
.tl-row { display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border-light); }
.tl-dot { font-size: var(--fs-lg); }
.tl-text { flex: 1; color: var(--text-regular); }
.tl-time { color: var(--text-secondary); font-size: var(--fs-xs); white-space: nowrap; }

@media print {
  .no-print { display: none !important; }
  .report-page { padding: 0; max-width: 100%; }
  .report-card { break-inside: avoid; border: 1px solid #ddd; }
  .credit-chart { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
}

@media (max-width: 768px) {
  .report-page { padding: 8px; }
  .info-grid { grid-template-columns: 1fr 1fr; }
  .stat-cards { grid-template-columns: 1fr 1fr; }
  .stat-num { font-size: 22px; }
}
</style>
