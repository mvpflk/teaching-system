<template>
  <div v-loading="loading" class="sg-page">
    <div class="sg-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h2>{{ data.studentName || '学生' }} · {{ data.subject || '' }} 学习成长</h2>
    </div>
    <el-card v-if="data.points?.length" shadow="never" class="sg-card">
      <template #header><span>成绩趋势</span></template>
      <div ref="chartRef" style="height:280px"></div>
    </el-card>
    <el-card v-if="data.weakPoints?.length" shadow="never" class="sg-card">
      <template #header><span>薄弱知识点 Top 5</span></template>
      <div class="sg-wp-list">
        <div v-for="(wp, i) in data.weakPoints" :key="wp.kpId" class="sg-wp-row">
          <span class="sg-wp-idx">{{ i + 1 }}</span>
          <span class="sg-wp-name">{{ wp.kpName }}</span>
          <el-progress
            :percentage="wp.errorRate"
            :stroke-width="8"
            color="var(--el-color-danger)"
            class="sg-wp-bar"
          />
          <span class="sg-wp-rate">{{ wp.errorRate }}%</span>
          <el-button
            size="small"
            type="primary"
            :loading="generatingKpId === wp.kpId"
            @click="sendConsolidation(wp)"
          >
            巩固包
          </el-button>
        </div>
      </div>
    </el-card>
    <EmptyState
      v-if="!loading && !data.points?.length"
      description="暂无测试记录"
      action-text="返回"
      @action="$router.back()"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getStudentGrowth } from '@/api/student'
import { useConsolidationGenerator } from '@/composables/useConsolidationGenerator'
import { useChartLazy } from '@/composables/useChartLazy'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const data = ref({})
const consolidation = useConsolidationGenerator()
const generatingKpId = ref(0)

// 图表懒初始化：进入视口后才 import echarts 并创建实例，离开页面自动 dispose
const { chartRef } = useChartLazy((el) => {
  return import('@/utils/echarts').then(({ default: echarts, cssVar }) => {
    const prev = echarts.getInstanceByDom(el)
    if (prev) prev.dispose()
    const chart = echarts.init(el)
    chart.setOption(buildOption(echarts, cssVar))
    return chart
  })
})

function buildOption(echarts, cssVar) {
  const titles = data.value.points.map(p => (p.title || '').substring(0, 12))
  const hasClassAvg = data.value.points.some(p => p.classAvgRate != null || p.classAvgScore != null)
  const series = [{
    name: '我的成绩',
    type: 'line', data: data.value.points.map(p => p.rate || 0),
    smooth: true, symbol: 'circle', symbolSize: 8,
    lineStyle: { color: cssVar('--primary-color'), width: 2 },
    itemStyle: { color: cssVar('--primary-color') }
  }]
  if (hasClassAvg) {
    const avgData = data.value.points.map(p => p.classAvgRate != null ? p.classAvgRate : p.classAvgScore)
    series.push({
      name: '班级均分',
      type: 'line', data: avgData,
      smooth: true, symbol: 'diamond', symbolSize: 7,
      lineStyle: { color: cssVar('--text-secondary'), type: 'dashed', width: 2 },
      itemStyle: { color: cssVar('--text-secondary') }
    })
  }
  return {
    tooltip: { trigger: 'axis' },
    legend: hasClassAvg ? { data: ['我的成绩', '班级均分'], bottom: 0 } : undefined,
    grid: { left: 40, right: 20, top: 20, bottom: hasClassAvg ? 30 : 20 },
    xAxis: { type: 'category', data: titles, axisLabel: { fontSize: 11 } },
    yAxis: { max: 100, axisLabel: { formatter: '{value}分' } },
    series
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getStudentGrowth(route.params.studentId, { subject: route.query.subject || '' })
    if (res.code === 200) data.value = res.data || {}
    await nextTick()
  } catch { /* */ }
  finally { loading.value = false }
}

async function sendConsolidation(wp) {
  if (consolidation.generating.value) return
  generatingKpId.value = wp.kpId
  try {
    const outputId = await consolidation.generate({
      taskId: data.value.points?.[0]?.taskId || 0,
      knowledgeNodeIds: [wp.kpId],
      subject: data.value.subject || ''
    })
    if (outputId) router.push(`/teacher/quality/consolidation/${outputId}`)
  } finally {
    generatingKpId.value = 0
  }
}

onMounted(load)
</script>

<style scoped>
.sg-page { max-width: 800px; margin: 0 auto; padding: 24px; }
.sg-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.sg-header h2 { margin: 0; font-size: var(--fs-lg); }
.sg-card { margin-bottom: 20px; }
.sg-wp-list { display: flex; flex-direction: column; gap: 4px; }
.sg-wp-row { display: flex; align-items: center; gap: 8px; padding: 8px 0; }
.sg-wp-idx { font-weight: 700; color: var(--primary-color); width: 20px; flex-shrink: 0; }
.sg-wp-name { min-width: 110px; font-size: var(--fs-sm); flex-shrink: 0; }
.sg-wp-bar { flex: 1; min-width: 60px; }
.sg-wp-rate { font-size: var(--fs-xs); color: var(--el-color-danger); min-width: 38px; text-align: right; flex-shrink: 0; }

/* 移动端适配 */
@media (max-width: 768px) {
  .sg-page { padding: 12px; }
  .sg-header { gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
  .sg-header h2 { font-size: var(--fs-md); }
  .sg-wp-row { flex-wrap: wrap; gap: 6px; padding: 6px 0; }
  .sg-wp-name { min-width: 0; flex: 1 1 100%; }
  .sg-wp-bar { flex: 1 1 100%; order: 5; }
  .sg-wp-rate { order: 4; }
  .sg-wp-row :deep(.el-button) { order: 6; }
}
</style>