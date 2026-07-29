<template>
  <div v-loading="loading" class="survey-stats">
    <div style="display:flex;justify-content:space-between;align-items:center;margin:0 0 16px">
      <h4 style="margin:0"><el-icon class="header-icon"><DataAnalysis /></el-icon> 问卷统计（{{ totalSubs }} 份提交）</h4>
      <div>
        <el-checkbox v-model="blinded" size="small" style="margin-right:8px">盲化导出</el-checkbox>
        <el-button size="small" @click="exportCSV" :loading="exporting"><el-icon><Download /></el-icon> 导出CSV</el-button>
      </div>
    </div>
    <el-empty v-if="!questions.length" description="暂无数据" :image-size="60" />

    <div v-for="q in questions" :key="q.id" class="sq-card">
      <div class="sq-label">{{ q.label }} <el-tag size="small">{{ typeLabel(q.type) }}</el-tag></div>

      <!-- 单选/多选：饼图 -->
      <div v-if="q.type === 'radio' || q.type === 'checkbox'" class="sq-chart-row">
        <div ref="chartRefs" class="sq-chart"></div>
        <div class="sq-legend">
          <div v-for="(cnt, label) in q.counts" :key="label" class="legend-row">
            <span class="legend-dot" :style="{ background: colors[legendIdx(label)] }"></span>
            <span class="legend-label">{{ label }}</span>
            <span class="legend-cnt">{{ cnt }}</span>
          </div>
        </div>
      </div>

      <!-- 下拉（同单选） -->
      <div v-if="q.type === 'dropdown'" class="sq-chart-row">
        <div class="sq-chart"></div>
        <div class="sq-legend">
          <div v-for="(cnt, label) in q.counts" :key="label" class="legend-row">
            <span class="legend-dot" :style="{ background: colors[legendIdx(label)] }"></span>
            <span class="legend-label">{{ label }}</span>
            <span class="legend-cnt">{{ cnt }}</span>
          </div>
        </div>
      </div>

      <!-- 评分星：星级分布柱状图 -->
      <div v-if="q.type === 'rating'" class="sq-chart-row">
        <div class="sq-chart"></div>
        <div class="sq-legend">
          <div v-for="(cnt, label) in q.counts" :key="label" class="legend-row">
            <span class="legend-dot" :style="{ background: colors[Number(label) - 1] }"></span>
            <span class="legend-label">{{ label }} 星</span>
            <span class="legend-cnt">{{ cnt }}</span>
          </div>
        </div>
      </div>

      <!-- 量表：分布柱状图 -->
      <div v-if="q.type === 'scale'" class="sq-chart-row">
        <div class="sq-chart"></div>
        <div class="sq-legend">
          <div v-for="(cnt, label) in q.counts" :key="label" class="legend-row">
            <span class="legend-dot" :style="{ background: colors[Number(label) % colors.length] }"></span>
            <span class="legend-label">{{ label }}</span>
            <span class="legend-cnt">{{ cnt }}</span>
          </div>
        </div>
      </div>

      <!-- 文本题：进度条 -->
      <div v-if="q.type === 'textarea'" class="sq-textarea-stat">
        <el-progress :percentage="respondedPct(q)" :stroke-width="16">
          <span>{{ q.responded }}/{{ totalSubs }} 已作答</span>
        </el-progress>
        <div class="sq-skip">跳过率: {{ skipPct(q) }}%</div>
      </div>

      <!-- 日期：直接列出所有回答 -->
      <div v-if="q.type === 'date'" class="sq-date-list">
        <div v-for="(cnt, date) in q.counts" :key="date" class="date-row">
          <span class="date-label">{{ date }}</span>
          <el-progress
            :percentage="pct(cnt)"
            :stroke-width="12"
            :format="() => cnt + '人'"
            style="flex:1"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Download } from '@element-plus/icons-vue'
import { getSurveyStats } from '@/api/task'
import request from '@/utils/request'
import echarts, { cssVar } from '@/utils/echarts'

const props = defineProps({ taskId: { type: [Number, String], required: true } })
const loading = ref(false)
const questions = ref([])
const totalSubs = ref(0)
const blinded = ref(false)
const exporting = ref(false)
const chartRefs = ref([])
const chartInstances = []
const colors = [cssVar('--primary-color'), cssVar('--el-color-danger'), cssVar('--el-color-success'), cssVar('--el-color-warning'), cssVar('--el-color-info'), '#ff6b35', '#36cfc9', '#4cc9f0']
const legendIdx = (label) => Object.keys(questions.value.find(q => q.type !== 'textarea')?.counts || {}).indexOf(label) % colors.length

const typeLabel = (t) => ({ radio: '单选', checkbox: '多选', dropdown: '下拉', rating: '评分星', scale: '量表', date: '日期', textarea: '文本' }[t] || t)
const respondedPct = (q) => totalSubs.value ? Math.round(q.responded / totalSubs.value * 100) : 0
const skipPct = (q) => totalSubs.value ? Math.round((q.skipped || 0) / totalSubs.value * 100) : 0
const pct = (cnt) => totalSubs.value ? Math.round(cnt / totalSubs.value * 100) : 0

const load = async () => {
  loading.value = true
  try {
    const r = await getSurveyStats(props.taskId)
    if (r.code === 200) { questions.value = r.data?.questions || []; totalSubs.value = r.data?.totalSubmissions || 0 }
  } catch { /* */ }
  finally { loading.value = false; nextTick(renderCharts) }
}

const exportCSV = async () => {
  exporting.value = true
  try {
    const res = await request({
      url: `/task/${props.taskId}/survey-export`,
      method: 'get',
      params: { blinded: blinded.value },
      responseType: 'blob'
    })
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = blinded.value ? 'survey-blinded.csv' : 'survey.csv'
    a.click()
    ElMessage.success(blinded.value ? '盲化导出成功' : '导出成功')
  } catch { ElMessage.error('导出失败') }
  exporting.value = false
}

const renderCharts = () => {
  const chartEls = document.querySelectorAll('.sq-chart')
  chartEls.forEach((el, i) => {
    const q = questions.value[i]
    if (!q || q.type === 'textarea' || !q.counts) return
    const labels = Object.keys(q.counts)
    const data = labels.map(l => ({ name: l, value: q.counts[l] || 0 }))
    if (el._chart) el._chart.dispose()
    const chart = echarts.init(el)
    el._chart = chart
    chartInstances.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['40%', '70%'], data, label: { show: false }, emphasis: { label: { show: true } } }],
      color: colors
    })
    const _onResize = () => chart.resize()
    window.addEventListener('resize', _onResize)
    // store for cleanup
    el._resizeHandler = _onResize
  })
}

onMounted(load)
onUnmounted(() => {
  chartInstances.forEach(c => c?.dispose())
  document.querySelectorAll('.sq-chart').forEach(el => {
    if (el._resizeHandler) window.removeEventListener('resize', el._resizeHandler)
  })
})
</script>

<style scoped>
.sq-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 14px; }
.sq-label { font-weight: 600; font-size: var(--fs-md); margin-bottom: 10px; display: flex; align-items: center; gap: 8px; }
.sq-chart-row { display: flex; align-items: center; gap: 20px; }
.sq-chart { width: 200px; height: 200px; flex-shrink: 0; }
.sq-legend { font-size: var(--fs-sm); }
.legend-row { display: flex; align-items: center; gap: 6px; padding: 3px 0; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; }
.legend-label { min-width: 60px; color: var(--text-regular); }
.legend-cnt { color: var(--text-secondary); font-weight: 500; }
.sq-textarea-stat { padding: 8px 0; }
.sq-skip { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.sq-date-list { display: flex; flex-direction: column; gap: 6px; padding: 8px 0; }
.date-row { display: flex; align-items: center; gap: 12px; }
.date-label { font-size: var(--fs-sm); color: var(--text-primary); min-width: 100px; font-variant-numeric: tabular-nums; }

@media (max-width: 768px) {
  .sq-chart-row { flex-direction: column; align-items: flex-start; }
  .sq-chart { width: 160px; height: 160px; }
}
</style>
