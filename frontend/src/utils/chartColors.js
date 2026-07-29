/**
 * ECharts 统一配色常量
 * 所有 echarts 图表应引用此文件，禁止硬编码颜色
 */
export const CHART_COLORS = {
  primary: '#4361ee',
  success: '#2e7d32',
  warning: '#ed6c02',
  danger: '#d32f2f',
  info: '#86868b',
  purple: '#7209b7',
  cyan: '#06b6d4',
  amber: '#f59e0b',
  emerald: '#059669',
  slate: '#94a3b8',
  gray: '#e0e0e0',
  rose: '#f56c6c',
  orange: '#e6a23c',
  green: '#67c23a',
  blue: '#409eff',
}

export const CHART_COLOR_ARRAY = Object.values(CHART_COLORS)

// 常用渐变
export const CHART_GRADIENT = {
  primaryArea: {
    type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
    colorStops: [
      { offset: 0, color: 'rgba(67,97,238,0.25)' },
      { offset: 1, color: 'rgba(67,97,238,0.02)' },
    ],
  },
}

// 防呆别名 — 兼容 `import { getChartColors }` 或 `(await import('...')).getChartColors()` 的误用
export function getChartColors() { return CHART_COLORS }
