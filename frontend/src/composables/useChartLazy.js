/**
 * ECharts 移动端懒初始化 — 图表进入视口后才创建实例
 *
 * 用法：
 *   import { useChartLazy } from '@/composables/useChartLazy'
 *   const { chartRef, initChart } = useChartLazy((el) => {
 *     const chart = echarts.init(el)
 *     chart.setOption(options)
 *     return chart
 *   })
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useChartLazy(initFn) {
  const chartRef = ref(null)
  let chartInstance = null
  let observer = null

  onMounted(() => {
    if (!chartRef.value) return

    // 优先使用 IntersectionObserver，fallback 到立即初始化
    if ('IntersectionObserver' in window) {
      observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              chartInstance = initFn(chartRef.value)
              observer?.disconnect()
            }
          })
        },
        { rootMargin: '100px' }
      )
      observer.observe(chartRef.value)
    } else {
      // fallback: 直接初始化
      chartInstance = initFn(chartRef.value)
    }
  })

  onUnmounted(() => {
    observer?.disconnect()
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  })

  return { chartRef, getInstance: () => chartInstance }
}
