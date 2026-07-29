<template>
  <div ref="chartRef" :style="chartStyle"></div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  dimensions: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  size: { type: String, default: 'medium' }
})

const chartRef = ref(null)
let chart = null

const sizeMap = { small: { w: 300, h: 260 }, medium: { w: 420, h: 340 }, large: { w: 560, h: 420 } }
const chartStyle = computed(() => {
  const s = sizeMap[props.size] || sizeMap.medium
  return { width: s.w + 'px', height: s.h + 'px' }
})

function render() {
  if (!chart || !props.dimensions.length) return
  const option = {
    title: props.title ? { text: props.title, left: 'center', textStyle: { fontSize: 14 } } : undefined,
    radar: {
      indicator: props.dimensions.map(d => ({ name: d.name, max: d.max || 10 })),
      center: ['50%', props.title ? '55%' : '50%'],
      radius: '65%'
    },
    series: [{
      type: 'radar',
      data: [{ value: props.dimensions.map(d => d.score || 0), name: '得分', areaStyle: { opacity: 0.2 } }]
    }]
  }
  chart.setOption(option, true)
}

const _onChartResize = () => chart?.resize()

onMounted(() => {
  chart = echarts.init(chartRef.value)
  render()
  window.addEventListener('resize', _onChartResize)
})

watch(() => props.dimensions, render, { deep: true })
onUnmounted(() => { window.removeEventListener('resize', _onChartResize); chart?.dispose() })
</script>
