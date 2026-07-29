/**
 * ECharts 树摇实例 — 按需注册本项目使用的图表类型和组件
 * 替代 import * as echarts from 'echarts'，减少 ~500KB 包体积
 */
import * as echarts from 'echarts/core'
import { BarChart, PieChart, LineChart, RadarChart, GaugeChart, ScatterChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  ToolboxComponent, DataZoomComponent
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart, PieChart, LineChart, RadarChart, GaugeChart, ScatterChart,
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
  ToolboxComponent, DataZoomComponent,
  LabelLayout, UniversalTransition,
  CanvasRenderer
])

/**
 * 解析 CSS 变量为实际颜色值
 * Canvas/ECharts 不识别 var(--xxx)，渲染前需转为 hex/rgba
 * @param {string} name - CSS 变量名称，如 '--primary-color'
 * @param {string} fallback - 解析失败时的后备色
 * @returns {string} 解析后的颜色值
 */
export function cssVar(name, fallback = '#000000') {
  if (typeof window === 'undefined') return fallback
  try {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
  } catch {
    return fallback
  }
}

export default echarts
