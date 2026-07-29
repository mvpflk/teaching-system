<template>
  <div class="audit-logs">
    <div class="page-header">
      <h3 class="page-title">审计日志</h3>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- Tab 1: 日志列表 -->
      <el-tab-pane label="📄 日志列表" name="list">
        <div class="filter-bar">
          <el-input
            v-model="filters.username"
            placeholder="用户名"
            clearable
            size="default"
            @change="onFilter"
          />
          <el-select
            v-model="filters.eventType"
            placeholder="事件类型"
            clearable
            size="default"
            @change="onFilter"
          >
            <el-option
              v-for="t in eventTypes"
              :key="t"
              :value="t"
              :label="t"
            />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="default"
            value-format="YYYY-MM-DD"
            @change="onFilter"
          />
          <el-button size="default" @click="resetFilters">重置</el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="records"
          stripe
          size="small"
        >
          <el-table-column
            prop="id"
            label="ID"
            width="60"
            align="center"
          />
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column prop="role" label="角色" width="110" />
          <el-table-column prop="eventType" label="事件类型" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="eventTagType(row.eventType)">{{ row.eventType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="description"
            label="描述"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column prop="ipAddress" label="IP" width="120" />
          <el-table-column label="结果" width="70" align="center">
            <template #default="{ row }"><span :style="{ color: row.status === 'SUCCESS' ? 'var(--success-color)' : 'var(--danger-color)' }">{{ row.status === 'SUCCESS' ? '成功' : '失败' }}</span></template>
          </el-table-column>
          <el-table-column label="时间" width="160" align="center">
            <template #default="{ row }">{{ fmt(row.createdTime) }}</template>
          </el-table-column>
        </el-table>
        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next, total"
            size="default"
            @current-change="loadLogs"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 2: 行为分析 -->
      <el-tab-pane label="📊 行为分析" name="analysis">
        <!-- 时间筛选 -->
        <div class="filter-bar">
          <el-radio-group v-model="timeRange" size="default" @change="loadAnalysis">
            <el-radio-button value="today">今日</el-radio-button>
            <el-radio-button value="7d">近7天</el-radio-button>
            <el-radio-button value="30d">近30天</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="timeRange === 'custom'"
            v-model="customRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            size="default"
            value-format="YYYY-MM-DD"
            @change="loadAnalysis"
          />
        </div>

        <!-- A. 事件分布 -->
        <div class="analysis-section">
          <h4>📊 事件类型分布</h4>
          <div ref="distChartRef" class="chart-box" style="height:300px"></div>
        </div>

        <!-- B. 活跃用户 TOP10 -->
        <div class="analysis-section">
          <h4>👤 活跃用户 TOP10</h4>
          <el-table
            v-loading="aLoading"
            :data="activeUsers"
            stripe
            size="small"
          >
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="role" label="角色" width="100" />
            <el-table-column
              prop="count"
              label="操作次数"
              width="100"
              align="center"
            >
              <template #default="{ row }"><strong>{{ row.count }}</strong></template>
            </el-table-column>
            <el-table-column prop="lastOperation" label="最后操作" min-width="150" />
          </el-table>
        </div>

        <!-- C. 24小时操作趋势 -->
        <div class="analysis-section">
          <h4>⏰ 24小时操作趋势</h4>
          <div ref="hourChartRef" class="chart-box" style="height:240px"></div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs, getAuditEventDist, getAuditActiveUsers, getAuditHourlyTrend } from '@/api/system'
import dayjs from 'dayjs'
import echarts from '@/utils/echarts'

// ── 日志列表 ──
const activeTab = ref('list')
const records = ref([]); const loading = ref(false); const total = ref(0)
const pageNum = ref(1); const pageSize = 20
const filters = reactive({ username: '', eventType: '', dateRange: null })
const eventTypes = ['USER_LOGIN','USER_LOGOUT','EXAM_PUBLISH','EXAM_CREATE','PARAM_UPDATE','DATA_RESET','DATA_EXPORT','CREDIT_ADJUST','HOMEWORK_ASSIGN','OTHER']
const eventTagType = t => ({ USER_LOGIN:'', USER_LOGOUT:'info', PARAM_UPDATE:'warning', DATA_RESET:'danger', EXAM_PUBLISH:'success', CREDIT_ADJUST:'warning' }[t]||'')
const fmt = t => t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'

const onFilter = () => { pageNum.value = 1; loadLogs() }
const resetFilters = () => { filters.username=''; filters.eventType=''; filters.dateRange=null; pageNum.value=1; loadLogs() }

const loadLogs = async () => {
  loading.value = true
  try {
    const params = { page: pageNum.value, size: pageSize }
    if (filters.username) params.username = filters.username
    if (filters.eventType) params.eventType = filters.eventType
    if (filters.dateRange?.length === 2) { params.startTime = filters.dateRange[0]; params.endTime = filters.dateRange[1] }
    const r = await getAuditLogs(params)
    if (r.code === 200) { records.value = r.data.records||[]; total.value = r.data.total||0 }
    else ElMessage.error(r.message||'加载失败')
  } catch { ElMessage.error('加载审计日志失败') }
  finally { loading.value = false }
}

// ── 行为分析 ──
const timeRange = ref('7d'); const customRange = ref(null)
const activeUsers = ref([]); const aLoading = ref(false)
const distChartRef = ref(null); const hourChartRef = ref(null)
let distChart = null; let hourChart = null

const getTimeParams = () => {
  let start, end
  const now = dayjs()
  if (timeRange.value === 'today') { start = now.format('YYYY-MM-DD'); end = now.format('YYYY-MM-DD') }
  else if (timeRange.value === '7d') { start = now.subtract(6,'day').format('YYYY-MM-DD'); end = now.format('YYYY-MM-DD') }
  else if (timeRange.value === '30d') { start = now.subtract(29,'day').format('YYYY-MM-DD'); end = now.format('YYYY-MM-DD') }
  else if (timeRange.value === 'custom' && customRange.value?.length === 2) { start = customRange.value[0]; end = customRange.value[1] }
  return { startTime: start, endTime: end }
}

const loadAnalysis = async () => {
  aLoading.value = true
  const tp = getTimeParams()
  try {
    const [distR, usersR, hourR] = await Promise.all([
      getAuditEventDist(tp),
      getAuditActiveUsers({ ...tp, limit: 10 }),
      getAuditHourlyTrend(tp)
    ])
    if (usersR.code === 200) activeUsers.value = usersR.data||[]

    await nextTick()
    if (distChartRef.value) {
      if (distChart) distChart.dispose()
      distChart = echarts.init(distChartRef.value)
      if (distR.code === 200 && distR.data?.length) {
        distChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 12 } },
          series: [{
            type: 'pie', radius: ['45%','72%'], center: ['40%','50%'],
            label: { formatter: '{b}\n{c}' },
            data: distR.data.map(d => ({ name: d.eventType, value: d.count }))
          }]
        }, true)
      }
    }
    if (hourChartRef.value) {
      if (hourChart) hourChart.dispose()
      hourChart = echarts.init(hourChartRef.value)
      if (hourR.code === 200 && hourR.data?.length) {
        hourChart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 40, right: 20, top: 10, bottom: 30 },
          xAxis: { type: 'category', data: hourR.data.map(d => d.hour), axisLabel: { rotate: 45, fontSize: 10 } },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{
            type: 'line', smooth: true,
            data: hourR.data.map(d => d.count),
            areaStyle: { color: 'rgba(67,97,238,0.15)' },
            lineStyle: { color: 'var(--primary-color)' },
            itemStyle: { color: 'var(--primary-color)' }
          }]
        }, true)
      }
    }
  } catch { ElMessage.error('加载分析数据失败') }
  finally { aLoading.value = false }
}

const onTabChange = (name) => {
  if (name === 'analysis') { nextTick(loadAnalysis) }
}

onMounted(() => { loadLogs() })
onUnmounted(() => { distChart?.dispose(); hourChart?.dispose() })
</script>

<style scoped lang="scss">
.audit-logs { max-width: 1200px; }
.page-header { margin-bottom: 8px; .page-title { font-size: var(--fs-xl,20px); margin: 0; } }
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; align-items: center; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }

.analysis-section {
  background: var(--bg-card,var(--bg-card)); border-radius: var(--radius-lg,12px);
  padding: 20px; margin-bottom: 16px; box-shadow: var(--shadow-sm);
  h4 { margin: 0 0 12px; font-size: var(--fs-md); color: var(--text-primary); }
}
.chart-box { width: 100%; }

@media (max-width: 768px) {
  .filter-bar { flex-direction: column; align-items: stretch; }
  .filter-bar :deep(.el-select), .filter-bar :deep(.el-input), .filter-bar :deep(.el-date-picker) { width: 100%; }
  .filter-bar :deep(.el-button) { width: 100%; margin-left: 0; }
  .chart-box { height: 240px !important; }
  :deep(.el-table) { font-size: var(--fs-xs); }
  :deep(.el-tabs__item) { font-size: var(--fs-xs); padding: 0 10px !important; }
  :deep(.el-tabs__nav-wrap) { overflow-x: auto; }
}
</style>
