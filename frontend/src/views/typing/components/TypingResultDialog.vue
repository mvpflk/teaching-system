<template>
  <el-dialog
    :model-value="visible"
    title="打字完成"
    width="580px"
    :close-on-click-modal="false"
    class="result-dialog"
    @update:model-value="$emit('close')"
  >
    <div class="result-content">
      <div class="result-grid">
        <div class="result-item ri-speed">
          <span class="ri-label">平均速度</span>
          <strong>{{ speedWpm }}<small> 字/分</small></strong>
        </div>
        <div class="result-item ri-peak">
          <span class="ri-label">最高段速</span>
          <strong>{{ maxSegmentSpeed }}<small> 字/分</small></strong>
        </div>
        <div class="result-item ri-acc">
          <span class="ri-label">正确率</span>
          <strong :class="accuracyClass">{{ accuracy }}%</strong>
        </div>
        <div class="result-item ri-time">
          <span class="ri-label">用时</span>
          <strong>{{ elapsedDisplay }}</strong>
        </div>
        <div class="result-item ri-errors">
          <span class="ri-label">出错字符</span>
          <strong>{{ wrongCount }}</strong>
        </div>
        <div class="result-item ri-back">
          <span class="ri-label">退格次数</span>
          <strong>{{ backspaceCount }}</strong>
        </div>
        <div v-if="lastRecord" class="result-item ri-compare">
          <span class="ri-label">上次练习</span>
          <strong>
            <span :class="compareSpeedDiff >= 0 ? 'compare-up' : 'compare-down'">{{ compareSpeedDiff >= 0 ? '+' : '' }}{{ compareSpeedDiff }}</span>
            <small> 字/分</small>
          </strong>
        </div>
      </div>

      <div v-if="lastRecord" class="result-compare-msg">
        <el-alert
          :type="compareSpeedDiff >= 0 ? 'success' : 'warning'"
          :closable="false"
          show-icon
        >
          <template #title>
            {{ compareSpeedDiff >= 0
              ? `🎉 速度比上次快了 ${compareSpeedDiff} 字/分，正确率 ${compareAccDiff >= 0 ? '+' + compareAccDiff : compareAccDiff}%，继续加油！`
              : `💪 速度比上次慢了 ${Math.abs(compareSpeedDiff)} 字/分，多练几次就会进步的！`
            }}
          </template>
        </el-alert>
      </div>

      <div v-if="speedTrend.length > 1" class="trend-section">
        <h5>速度趋势</h5>
        <div v-loading="trendLoading" class="trend-chart-wrap">
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>
      </div>

      <div v-if="errorList.length" class="result-errors">
        <h5>错误字符（{{ errorList.length }} 个）</h5>
        <div class="error-list-scroll">
          <span
            v-for="(e, i) in errorList"
            :key="i"
            class="result-err-chip"
          >"{{ e.char || '空' }}" → "{{ e.expected }}"</span>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button v-if="!inCompetition" type="primary" @click="$emit('retry')">再练一篇</el-button>
      <el-button @click="$emit('view-history')">查看历史</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  speedWpm: { type: Number, default: 0 },
  maxSegmentSpeed: { type: Number, default: 0 },
  accuracy: { type: Number, default: 100 },
  accuracyClass: { type: String, default: '' },
  elapsedDisplay: { type: String, default: '0秒' },
  wrongCount: { type: Number, default: 0 },
  backspaceCount: { type: Number, default: 0 },
  lastRecord: { type: Object, default: null },
  compareSpeedDiff: { type: Number, default: 0 },
  compareAccDiff: { type: Number, default: 0 },
  speedTrend: { type: Array, default: () => [] },
  trendLoading: { type: Boolean, default: false },
  errorList: { type: Array, default: () => [] },
  inCompetition: { type: Boolean, default: false }
})

defineEmits(['close', 'retry', 'view-history'])

// #19 修复：图表渲染逻辑移入拥有 trendChartRef 的子组件，避免 querySelector
const trendChartRef = ref(null)
let trendChartInstance = null

async function renderSpeedTrend() {
  if (!props.visible || props.speedTrend.length < 2) return
  await nextTick()
  if (!trendChartRef.value) return
  try {
    const { default: echarts, cssVar } = await import('@/utils/echarts')
    if (trendChartInstance) trendChartInstance.dispose()
    trendChartInstance = echarts.init(trendChartRef.value)
    const dates = props.speedTrend.map(d => d.date || '')
    const speeds = props.speedTrend.map(d => d.speedWpm || 0)
    const accuracies = props.speedTrend.map(d => d.accuracy || 0)
    trendChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['速度(字/分)', '正确率(%)'], bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8 },
      grid: { left: '3%', right: '8%', bottom: '28%', top: '5%', containLabel: true },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10, rotate: 20 } },
      yAxis: [
        { type: 'value', name: '字/分', min: 0, position: 'left' },
        { type: 'value', name: '%', min: 0, max: 100, position: 'right' }
      ],
      series: [
        { name: '速度(字/分)', type: 'line', data: speeds, smooth: true,
          lineStyle: { width: 2, color: cssVar('--primary-color') }, itemStyle: { color: cssVar('--primary-color') },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [
            {offset:0,color:'rgba(67,97,238,0.25)'},{offset:1,color:'rgba(67,97,238,0.02)'}
          ])}
        },
        { name: '正确率(%)', type: 'line', data: accuracies, smooth: true, yAxisIndex: 1,
          lineStyle: { width: 2, color: cssVar('--el-color-success') }, itemStyle: { color: cssVar('--el-color-success') },
          areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [
            {offset:0,color:'rgba(46,125,50,0.2)'},{offset:1,color:'rgba(46,125,50,0.02)'}
          ])}
        }
      ]
    })
  } catch { /* echarts load failed */ }
}

watch(() => props.speedTrend, () => { renderSpeedTrend() })
watch(() => props.visible, (v) => { if (v) renderSpeedTrend() })

onUnmounted(() => {
  if (trendChartInstance) { trendChartInstance.dispose(); trendChartInstance = null }
})
</script>

<style scoped>
.result-content { display: flex; flex-direction: column; gap: 10px; }
.result-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
.result-item { text-align: center; padding: 12px 8px; background: var(--typing-stats-bg); border-radius: var(--radius-md); border: 1px solid var(--typing-border); }
.ri-label { display: block; font-size: var(--fs-xs); color: var(--typing-pending); }
.result-item strong { display: block; font-size: 22px; margin-top: 4px; font-variant-numeric: tabular-nums; }
.result-item strong small { font-size: var(--fs-xs); font-weight: 400; }
.ri-speed strong { color: var(--typing-cursor); }
.ri-peak strong { color: var(--typing-current); }
.ri-acc strong { color: var(--typing-correct); }
.ri-time strong { color: var(--typing-text); }
.ri-errors strong, .ri-back strong { color: var(--typing-incorrect); }
.ri-compare strong { font-size: 18px; }
.compare-up { color: var(--el-color-success); }
.compare-down { color: var(--el-color-danger); }
.result-compare-msg { margin: 8px 0; }
.trend-section { margin-bottom: 12px; }
.trend-section h5 { margin: 0 0 6px; font-size: var(--fs-md); }
.trend-chart-wrap { position: relative; }
.trend-chart { width: 100%; height: 160px; }
.result-errors h5 { margin: 12px 0 6px; font-size: var(--fs-sm); }
.error-list-scroll { max-height: 120px; overflow-y: auto; display: flex; flex-wrap: wrap; gap: 4px; }
.result-err-chip { font-size: var(--fs-xs); padding: 3px 8px; background: var(--typing-incorrect-bg); color: var(--typing-incorrect); border-radius: 10px; }
</style>
