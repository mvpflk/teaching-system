<template>
  <div class="ch-page">
    <div class="ch-header">
      <div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap">
        <h2 style="margin:0"><el-icon><Histogram /></el-icon> 质量分析</h2>
        <el-radio-group v-model="mode" size="small" @change="onModeChange">
          <el-radio-button value="horizontal">📊 横向对比</el-radio-button>
          <el-radio-button value="vertical">📈 纵向趋势</el-radio-button>
        </el-radio-group>
      </div>
      <p v-if="mode==='horizontal'" class="ch-sub">选择一场已截止的考试/练习，查看班级对比分析和学生成长数据</p>
      <p v-else class="ch-sub">选择班级和学科，追踪历次考试成绩变化趋势</p>
    </div>

    <!-- 纵向趋势模式 -->
    <el-card v-if="mode==='vertical'" shadow="never">
      <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-select
          v-model="trendClassId"
          placeholder="选择班级"
          clearable
          style="width:180px"
        >
          <el-option
            v-for="c in teachingClasses"
            :key="c.classId"
            :label="c.className"
            :value="c.classId"
          />
        </el-select>
        <el-select
          v-model="trendSubject"
          placeholder="学科筛选"
          clearable
          style="width:150px"
          @change="loadTrend"
        >
          <el-option
            v-for="s in trendSubjectOptions"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
        <el-button type="primary" :disabled="!trendClassId" @click="loadTrend">查询趋势</el-button>
      </div>

      <div v-if="trendData?.points?.length">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom:16px"
        >
          共 {{ trendData.points.length }} 次考试 · 最近均分 {{ trendData.points[trendData.points.length-1].avgScore }} 分
        </el-alert>
        <div ref="trendChart" style="height:320px"></div>
      </div>
      <el-empty v-if="trendLoaded && !trendData?.points?.length" :description="trendData?.message || '暂无考试记录'" />
    </el-card>

    <!-- 横向对比模式 -->
    <el-card v-if="mode==='horizontal'" shadow="never">
      <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-input
          v-model="keyword"
          placeholder="搜索任务标题"
          clearable
          style="width:220px"
          @clear="load"
          @keyup.enter="load"
        />
        <el-select
          v-model="filterSubject"
          placeholder="学科筛选"
          clearable
          style="width:150px"
          @change="load"
        >
          <el-option
            v-for="s in subjectOptions"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
        <el-button type="primary" @click="load">搜索</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedGroups"
        stripe
        row-key="key"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="ch-expand-body">
              <template v-if="summaryData[row.taskIds.join(',')]?.classes?.length">
                <div class="ch-summary-cards">
                  <div
                    v-for="c in summaryData[row.taskIds.join(',')].classes"
                    :key="c.classId"
                    class="ch-summary-card"
                    :class="{ 'ch-summary-card--best': c._isBest }"
                  >
                    <div class="ch-summary-card-name">{{ c.className }}</div>
                    <div class="ch-summary-card-score">{{ c.avgScore }}<span class="ch-summary-card-unit">分</span></div>
                    <div class="ch-summary-card-sub">及格率 {{ c.passRate }}% · {{ c.studentCount }}人</div>
                  </div>
                </div>
              </template>
              <span v-else-if="summaryData[row.taskIds.join(',')]?.message" class="ch-summary-empty">
                {{ summaryData[row.taskIds.join(',')].message }}
              </span>
              <span v-else-if="loadingSummary" class="ch-summary-empty">加载中…</span>
              <span v-else class="ch-summary-empty">暂无数据</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务标题" min-width="200">
          <template #default="{ row }">
            <span>{{ row.title }}</span>
            <el-tag
              v-if="row.hasMultiple"
              type="info"
              size="small"
              effect="plain"
              style="margin-left:6px"
            >
              {{ row.list.length }}个班
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="学科" width="120" />
        <el-table-column label="班级" width="160">
          <template #default="{ row }">
            <el-tag
              v-for="t in row.list"
              :key="t.id"
              size="small"
              effect="plain"
              style="margin-right:4px;margin-bottom:2px"
            >
              {{ t.className || '班级' + t.id }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交" width="100">
          <template #default="{ row }">
            <span>{{ row.totalSubmissions || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            <span>{{ row.deadlines?.[0] || row.createdAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              size="small"
              @click="goComparison(row)"
            >
              <el-icon><Histogram /></el-icon> 对比分析
            </el-button>
            <el-button
              text
              type="success"
              size="small"
              :loading="diagnosingId === row.id"
              @click="goDiagnosis(row)"
            >
              <el-icon><DataAnalysis /></el-icon> AI诊断
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !tasks.length" style="text-align:center;padding:40px;color:#999">
        <el-empty description="暂无已截止的考试/练习">
          <el-button type="primary" @click="$router.push('/teacher/tasks/list')">去发布任务</el-button>
        </el-empty>
      </div>

      <el-pagination
        v-if="groupedTasks.length > pageSize"
        v-model:current-page="page"
        :page-size="pageSize"
        :total="groupedTasks.length"
        layout="prev, pager, next"
        style="margin-top:16px;justify-content:center"
        @current-change="fetchSummary"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Histogram, DataAnalysis } from '@element-plus/icons-vue'
import { listTasks } from '@/api/task'
import { startDiagnosis, getBatchComparisonSummary } from '@/api/simulation'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const tasks = ref([])
const loading = ref(false)
const keyword = ref('')
const filterSubject = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const diagnosingId = ref(null)
const summaryData = ref({})       // key → { classes[], ... }
const loadingSummary = ref(false)

// ── 纵向趋势 ──
const mode = ref('horizontal')
const trendClassId = ref(null)
const trendSubject = ref('')
const trendData = ref(null)
const trendLoaded = ref(false)
const trendChart = ref(null)
let _trendChartInstance = null
const teachingClasses = computed(() => userStore.teacherSummary?.teachingClasses || [])
const trendSubjectOptions = computed(() => {
  if (!trendData.value?.subjects?.length) return [...new Set(teachingClasses.value.map(c => c.subject).filter(Boolean))]
  return trendData.value.subjects
})

// 当前页分组（用于表格和摘要请求）
const pagedGroups = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return groupedTasks.value.slice(start, start + pageSize.value)
})

// 从已加载任务中动态提取学科选项
const subjectOptions = computed(() => {
  return [...new Set(tasks.value.map(t => t.subject).filter(Boolean))].sort()
})

// 同标题任务自动分组（同一试卷拆给不同班级的情况）
const groupedTasks = computed(() => {
  const map = new Map()
  for (const t of tasks.value) {
    const key = (t.title || '') + '|' + (t.subject || '')
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(t)
  }
  const groups = []
  for (const [key, list] of map) {
    const [title, subject] = key.split('|')
    groups.push({
      key,
      title,
      subject,
      list,
      taskIds: list.map(t => t.id).sort((a, b) => a - b),
      // 显示最早和最晚的创建时间
      createdAt: list.map(t => t.createdAt).sort()[0],
      deadlines: [...new Set(list.map(t => t.deadline).filter(Boolean))],
      totalSubmissions: list.reduce((s, t) => s + (t.submittedCount || 0), 0),
      hasMultiple: list.length > 1,
    })
  }
  return groups
})

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: 100, status: 'CLOSED' }
    if (keyword.value) params.keyword = keyword.value
    if (filterSubject.value) params.subject = filterSubject.value
    const res = await listTasks(params)
    if (res.code === 200) {
      const data = res.data
      tasks.value = data?.records || data || []
      total.value = data?.total || tasks.value.length
      await nextTick()
      fetchSummary()
    }
  } catch { tasks.value = [] }
  finally { loading.value = false }
}

async function fetchSummary() {
  const groups = pagedGroups.value
  if (!groups.length) return
  loadingSummary.value = true
  try {
    const res = await getBatchComparisonSummary(
      groups.map(g => ({ taskIds: g.taskIds }))
    )
    if (res.code === 200) {
      const map = {}
      for (const item of (res.data || [])) {
        // 标记最高分班级
        if (item.classes?.length) {
          const bestScore = Math.max(...item.classes.map(c => c.avgScore || 0))
          item.classes.forEach(c => { c._isBest = c.avgScore === bestScore && item.classes.length > 1 })
        }
        map[item.taskIds] = item
      }
      summaryData.value = map
    }
  } catch { /* 摘要加载失败不影响主列表 */ }
  finally { loadingSummary.value = false }
}

function goComparison(row) {
  // row 可能是分组后的 group 或原始 task
  const ids = row.taskIds ? row.taskIds.join(',') : String(row.id)
  const titles = Array.isArray(row.list) ? row.list.map(t => t.title)[0] : row.title
  router.push({
    path: `/teacher/quality/comparison/${row.taskIds ? row.taskIds[0] : row.id}`,
    query: { ids, title: titles || '', subject: row.subject || '' }
  })
}

async function goDiagnosis(row) {
  // 取第一个任务ID触发诊断（同标题任务共用同一试卷）
  const diagnId = row.taskIds ? row.taskIds[0] : row.id
  diagnosingId.value = diagnId
  try {
    const res = await startDiagnosis(diagnId)
    if (res.code === 200) {
      ElMessage.success('诊断任务已启动，即将跳转到报告页')
      setTimeout(() => router.push(`/teacher/ai/diagnosis/${diagnId}`), 800)
    } else {
      ElMessage.error(res.message || '诊断启动失败')
    }
  } catch {
    ElMessage.error('诊断请求失败')
  } finally {
    diagnosingId.value = null
  }
}

// ─── 纵向趋势 ───

async function loadTrend() {
  if (!trendClassId.value) return
  loading.value = true
  try {
    const params = { classId: trendClassId.value }
    if (trendSubject.value) params.subject = trendSubject.value
    const res = await request.get('/teacher/comparison/trend', { params })
    if (res.code === 200) {
      trendData.value = res.data || {}
      trendLoaded.value = true
      // 若未指定学科且返回了学科列表, 默认选第一个
      if (!trendSubject.value && trendData.value.subjects?.length) {
        trendSubject.value = trendData.value.subjects[0]
      }
      await nextTick()
      renderTrendChart()
    }
  } catch { /* */ }
  finally { loading.value = false }
}

function renderTrendChart() {
  if (!trendChart.value || !trendData.value?.points?.length) return
  import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    if (_trendChartInstance) { _trendChartInstance.dispose(); _trendChartInstance = null }
    const prev = echarts.getInstanceByDom(trendChart.value)
    if (prev) prev.dispose()
    _trendChartInstance = echarts.init(trendChart.value)

    const points = trendData.value.points
    // 难度标记: 简单=circle, 中等=roundRect, 较难=triangle
    const markers = { '简单': 'circle', '中等': 'roundRect', '较难': 'triangle' }
    const dates = points.map(p => (p.date || '').substring(0, 10))

    _trendChartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const p = params[0]
          const pt = points[p.dataIndex]
          return `${pt.title || dates[p.dataIndex]}<br/>`
            + `均分: ${pt.avgScore} · 及格率: ${pt.passRate}%<br/>`
            + `人数: ${pt.studentCount} · 难度: ${pt.difficulty || '未知'}`
        }
      },
      grid: { left: 50, right: 50, top: 20, bottom: 30 },
      xAxis: {
        type: 'category', data: dates,
        axisLabel: { rotate: points.length > 6 ? 30 : 0, fontSize: 11 }
      },
      yAxis: [
        { type: 'value', max: 100, axisLabel: { formatter: '{value}分' } },
        { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } }
      ],
      series: [{
        name: '均分', type: 'line', data: points.map(p => p.avgScore),
        smooth: true, symbolSize: 10,
        itemStyle: { color: cssVar('--primary-color') },
        lineStyle: { width: 2 },
        markLine: {
          silent: true,
          data: [{ type: 'average', name: '平均线', lineStyle: { color: cssVar('--el-color-warning'), type: 'dashed' } }]
        }
      }, {
        name: '及格率', type: 'line', yAxisIndex: 1, data: points.map(p => p.passRate),
        smooth: true, symbolSize: 8, symbol: 'diamond',
        itemStyle: { color: cssVar('--el-color-success') },
        lineStyle: { color: cssVar('--el-color-success'), width: 1.5, type: 'dotted' }
      }],
      // 双Y轴
      legend: { data: ['均分', '及格率'], bottom: 0 }
    })
  })
}

function onModeChange() {
  if (mode.value === 'vertical') {
    // 默认选第一个任教班级
    if (teachingClasses.value.length && !trendClassId.value) {
      trendClassId.value = teachingClasses.value[0].classId
      loadTrend()
    }
  }
}

onMounted(() => { if (mode.value === 'horizontal') load() })
onUnmounted(() => {
  if (_trendChartInstance) { _trendChartInstance.dispose(); _trendChartInstance = null }
})
</script>

<style scoped>
.ch-page { max-width: 1000px; margin: 0 auto; padding: 24px; }
.ch-header { margin-bottom: 24px; }
.ch-header h2 { font-size: var(--fs-xl); display: flex; align-items: center; gap: 8px; margin: 0 0 6px; color: var(--text-primary); }
.ch-sub { font-size: var(--fs-sm); color: var(--text-secondary); margin: 0; }

/* 展开行 — 摘要卡片 */
.ch-expand-body { padding: 8px 0 4px 0; }
.ch-summary-cards { display: flex; gap: 12px; flex-wrap: wrap; }
.ch-summary-card {
  width: 160px; padding: 10px 12px;
  border: 0.5px solid var(--border-base); border-radius: 8px;
  text-align: center; background: var(--bg-hover);
}
.ch-summary-card--best { border-color: var(--success-color); background: #f0f9eb; }
.ch-summary-card-name { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 2px; }
.ch-summary-card-score { font-size: 22px; font-weight: 700; color: var(--primary-color); }
.ch-summary-card--best .ch-summary-card-score { color: var(--success-color); }
.ch-summary-card-unit { font-size: var(--fs-xs); font-weight: 400; color: var(--text-secondary); }
.ch-summary-card-sub { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }
.ch-summary-empty { font-size: var(--fs-xs); color: var(--text-disabled); }
</style>
