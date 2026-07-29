<template>
  <el-dialog
    v-model="visible"
    :title="'成绩趋势 — ' + (studentName || '')"
    :width="isMobile ? '92%' : '760px'"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
  >
    <div v-loading="loading" class="trend-body">
      <div class="trend-filter">
        <el-select
          v-model="filterSubject"
          placeholder="全部学科"
          clearable
          size="small"
          class="desktop-width"
          style="width:150px"
          @change="loadData"
        >
          <el-option
            v-for="s in subjects"
            :key="s"
            :value="s"
            :label="s"
          />
        </el-select>
      </div>
      <el-empty v-if="!data.length && !loading" description="暂无成绩数据" :image-size="80" />
      <template v-else>
        <div ref="chartRef" class="chart-box"></div>
        <!-- 考试详情表 -->
        <div v-if="data.length" class="detail-table">
          <div class="table-title">📋 历次考试详情</div>
          <el-table
            :data="data"
            size="small"
            stripe
            max-height="260"
          >
            <el-table-column
              prop="taskTitle"
              label="考试名称"
              min-width="120"
              show-overflow-tooltip
            />
            <el-table-column label="日期" width="100">
              <template #default="{ row }">{{ fmtDate(row.examDate) }}</template>
            </el-table-column>
            <el-table-column label="得分率" width="100" align="center">
              <template #default="{ row }">
                <span :style="{ color: (row.studentRate||0) >= 60 ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">{{ row.studentRate != null ? fmtScore(row.studentRate) + '%' : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="班级均分率" width="110" align="center">
              <template #default="{ row }">{{ row.classAvgRate != null ? fmtScore(row.classAvgRate) + '%' : '-' }}</template>
            </el-table-column>
            <el-table-column label="排名" width="70" align="center">
              <template #default="{ row }">{{ row.rank ? row.rank + '/' + row.totalGraded : '-' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted } from 'vue'
import echarts, { cssVar } from '@/utils/echarts'
import request from '@/utils/request'
import dayjs from 'dayjs'
import { useIsMobile } from '@/composables/useIsMobile'

const props = defineProps({ modelValue: Boolean, classId: [Number, String], studentId: [Number, String], studentName: String })
const emit = defineEmits(['update:modelValue'])
const visible = ref(false), loading = ref(false), data = ref([]), subjects = ref([]), filterSubject = ref('')
const chartRef = ref(null)
const { isMobile } = useIsMobile()
let chartInstance = null

const loadData = async () => {
  if (!props.classId || !props.studentId) return
  loading.value = true
  try {
    const params = { studentId: props.studentId }
    if (filterSubject.value) params.subject = filterSubject.value
    const res = await request({ url: `/class/${props.classId}/actions/student-scores`, method: 'get', params })
    if (res.code === 200) {
      data.value = res.data || []
      subjects.value = [...new Set(data.value.map(d => d.subject).filter(Boolean))]
      await nextTick(); renderChart()
    }
  } catch { /* */ } finally { loading.value = false }
}

const renderChart = async () => {
  if (!chartRef.value || !data.value.length) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)
  const colors = (await import('@/utils/chartColors')).CHART_COLORS
  const labels = data.value.map(d => d.taskTitle.length > 8 ? d.taskTitle.substring(0, 8) + '...' : d.taskTitle)
  // 构建系列数据
  const series = [
    { name: '个人得分率', type: 'line', data: data.value.map(d => d.studentRate), smooth: true, lineStyle: { width: 3, color: colors.primary }, symbol: 'circle', symbolSize: 8 },
    { name: '班级均分率', type: 'line', data: data.value.map(d => d.classAvgRate), smooth: true, lineStyle: { width: 2, color: colors.warning, type: 'dashed' }, symbol: 'diamond', symbolSize: 6 }
  ]

  // 添加重测标记（散点）
  if (data.value.some(d => d.retakeScore != null)) {
    series.push({
      name: '重测得分',
      type: 'scatter',
      data: data.value
        .filter(d => d.retakeScore != null)
        .map(d => ({
          value: [d.taskTitle.length > 8 ? d.taskTitle.substring(0, 8) + '...' : d.taskTitle, d.retakeScore],
          symbolSize: 14,
        })),
      symbol: 'star',
      symbolSize: 14,
      itemStyle: { color: cssVar('--el-color-warning') },
      tooltip: {
        formatter: (p) => {
          const item = data.value.find(d => d.retakeScore === p.value[1])
          return `重测得分: ${p.value[1]}%<br/>${item?.retakeNote || '已达标'}`
        }
      }
    })
  }

  chartInstance.setOption({
    tooltip: { trigger: 'axis', valueFormatter: v => v + '%' },
    legend: { data: ['个人得分率', '班级均分率', ...(data.value.some(d => d.retakeScore != null) ? ['重测得分'] : [])], top: 0, textStyle: { fontSize: 12 } },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: 30, fontSize: 10 } },
    yAxis: { type: 'value', name: '得分率(%)', max: 100 },
    series,
    grid: { left: 50, right: 25, top: 35, bottom: 65 }
  })
}

const fmtDate = (d) => d ? dayjs(d).format('MM-DD') : '-'
const fmtScore = (v, decimals = 1) => v != null ? Number(v).toFixed(decimals) : '-'

watch(() => props.modelValue, (v) => { visible.value = v; if (v) { data.value = []; filterSubject.value = ''; loadData() } })
watch(visible, (v) => emit('update:modelValue', v))

onUnmounted(() => { chartInstance?.dispose() })
</script>

<style scoped>
.trend-body { min-height: 200px; }
.trend-filter { margin-bottom: 10px; }
.chart-box { width: 100%; height: 300px; }
.detail-table { margin-top: 16px; }
.table-title { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
@media (max-width: 768px) { .chart-box { height: 240px; } }
</style>
