<template>
  <div class="history-page">
    <div class="page-header">
      <h3>⌨️ 打字历史</h3>
      <el-button @click="router.push('/typing')">去练习</el-button>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="sk-list">
      <div class="sk-card">
        <div class="sk-line w-40" style="height:16px;margin-bottom:12px"></div>
        <div class="sk-chart-placeholder"></div>
      </div>
      <div class="sk-card">
        <div class="sk-line w-30" style="height:16px;margin-bottom:12px"></div>
        <div
          v-for="i in 3"
          :key="i"
          class="sk-row"
          style="padding:10px 0"
        >
          <div class="sk-line w-25" style="height:14px"></div>
          <div class="sk-line w-15" style="height:14px"></div>
          <div class="sk-line w-15" style="height:14px"></div>
          <div class="sk-line w-15" style="height:14px"></div>
          <div class="sk-line w-10" style="height:14px"></div>
        </div>
      </div>
    </div>

    <template v-else>
      <!-- 趋势图 -->
      <div class="chart-card">
        <h4>速度 & 正确率趋势</h4>
        <div ref="chartRef" class="chart-box"></div>
        <div v-if="!hasData" class="empty-state">
          <el-empty description="暂无打字记录" :image-size="80">
            <el-button type="primary" size="small" @click="router.push('/typing')">去练习</el-button>
          </el-empty>
        </div>
      </div>

      <!-- 高频错误 -->
      <div v-if="wrongWords.length" class="wrong-card">
        <h4>高频错误字符</h4>
        <div class="tags-wrap">
          <el-tag
            v-for="(w, i) in wrongWords"
            :key="i"
            :type="i < 10 ? 'danger' : 'warning'"
            size="small"
            class="wrong-tag"
          >
            {{ w.char }} ({{ w.count }}次)
          </el-tag>
        </div>
      </div>

      <!-- 历史记录表 -->
      <div class="history-table">
        <div class="table-header">
          <h4>练习记录</h4>
          <el-radio-group v-model="modeFilter" size="small">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="practice">自由练习</el-radio-button>
            <el-radio-button value="competition">竞赛</el-radio-button>
          </el-radio-group>
        </div>
        <el-table
          :data="filteredRecords"
          stripe
          border
          max-height="450"
        >
          <el-table-column label="时间" width="160">
            <template #default="{row}">{{ row.createdAt }}</template>
          </el-table-column>
          <el-table-column label="模式" width="80">
            <template #default="{row}">
              <el-tag :type="row.mode==='competition'?'warning':''" size="small">{{ row.mode==='competition'?'竞赛':'练习' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="speedWpm"
            label="速度(字/分)"
            width="100"
            sortable
          />
          <el-table-column label="正确率" width="90" sortable>
            <template #default="{row}">{{ row.accuracy }}%</template>
          </el-table-column>
          <el-table-column prop="durationSeconds" label="用时(s)" width="80" />
          <el-table-column prop="correctChars" label="正确" width="70" />
          <el-table-column prop="wrongChars" label="错误" width="70" />
          <el-table-column prop="backspaceCount" label="退格" width="70" />
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentHistory, getWrongWords } from '@/api/typing'
import echarts, { cssVar } from '@/utils/echarts'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const wrongWords = ref([])
const hasData = ref(false)
const chartRef = ref(null)
let chartInstance = null
const modeFilter = ref('')
const filteredRecords = computed(() => {
  if (!modeFilter.value) return records.value
  return records.value.filter(r => r.mode === modeFilter.value)
})

onMounted(async () => {
  loading.value = true
  try {
    const [hRes, wRes] = await Promise.all([getStudentHistory(), getWrongWords()])
    if (hRes.code === 200) {
      records.value = hRes.data || []
      hasData.value = records.value.length > 0
    }
    if (wRes.code === 200) wrongWords.value = wRes.data || []
  } catch {}
  loading.value = false
  await nextTick()
  renderChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
})

function onResize() {
  chartInstance?.resize()
}

function renderChart() {
  if (!chartRef.value || records.value.length < 2) return
  try {
    const data = [...records.value].reverse()
    const dom = chartRef.value
    if (chartInstance) { chartInstance.dispose(); chartInstance = null }
    chartInstance = echarts.init(dom)
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['速度(字/分)', '正确率(%)'], bottom: 0 },
      grid: { top: 20, right: 40, bottom: 40, left: 50 },
      xAxis: { type: 'category', data: data.map(r => r.createdAt?.slice(0,10) || r.date || ''), axisLabel: { fontSize: 11, rotate: 25 } },
      yAxis: [
        { type: 'value', name: '字/分', max: 200, axisLabel: { fontSize: 10 }, splitLine: { lineStyle: { type: 'dashed' } } },
        { type: 'value', name: '%', max: 100, axisLabel: { fontSize: 10 } }
      ],
      series: [
        {
          name: '速度(字/分)', type: 'line', data: data.map(r => r.speedWpm || 0),
          smooth: true, symbol: 'circle', symbolSize: 5, color: cssVar('--primary-color'),
          lineStyle: { width: 2 }, itemStyle: { color: cssVar('--primary-color') }
        },
        {
          name: '正确率(%)', type: 'line', yAxisIndex: 1, data: data.map(r => r.accuracy || 0),
          smooth: true, symbol: 'circle', symbolSize: 5, color: cssVar('--el-color-success'),
          lineStyle: { width: 2 }, itemStyle: { color: cssVar('--el-color-success') }
        }
      ]
    })
    window.addEventListener('resize', onResize)
  } catch {}
}
</script>

<style scoped>
.history-page { max-width: 960px; margin: 0 auto; padding: 16px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
.chart-card, .wrong-card, .history-table { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 16px; }
.chart-card h4, .wrong-card h4, .history-table h4 { margin: 0; }
.table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-header h4 { margin: 0; }
.chart-box { width: 100%; min-height: 280px; margin-top: 8px; }

.sk-list { display: flex; flex-direction: column; gap: 16px; }
.sk-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 16px; }
.sk-chart-placeholder { height: 200px; background: var(--bg-secondary); border-radius: var(--radius-md); }
.w-10 { width: 10% } .w-15 { width: 15% } .w-25 { width: 25% } .w-30 { width: 30% } .w-40 { width: 40% }

.wrong-tag { margin: 3px; }
.tags-wrap { line-height: 2; }

@media (max-width: 768px) {
  .history-page { padding: 8px; gap: 8px; }
}
</style>
