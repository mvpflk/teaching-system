<template>
  <div class="monitor-page">
    <div class="monitor-header">
      <h3>📊 系统运行监控</h3>
      <div class="header-actions">
        <span v-if="lastRefresh" class="refresh-hint">更新于 {{ lastRefresh }}</span>
        <el-button
          size="small"
          type="primary"
          :loading="loading"
          @click="loadData"
        >
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          size="small"
          style="margin-left:8px"
        />
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="card-grid cols-4">
      <div class="stat-card">
        <div class="card-icon" style="background:var(--bg-success-light)">
          <el-icon size="22" color="var(--success-color)"><Clock /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val small">{{ data.uptime?.uptimeDisplay || '-' }}</span>
          <span class="card-lbl">系统运行时间</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="card-icon" style="background:var(--primary-light)">
          <el-icon size="22" color="var(--primary-color)"><Cpu /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ cpuDisplay }}</span>
          <span class="card-lbl">CPU 使用率</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="card-icon" style="background:var(--bg-warning-light)">
          <el-icon size="22" color="var(--el-color-warning)"><Connection /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ data.dbPool?.activeConnections ?? '-' }}/{{ data.dbPool?.maximumPoolSize ?? '-' }}</span>
          <span class="card-lbl">数据库连接</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="card-icon" style="background:var(--bg-danger-light)">
          <el-icon size="22" color="var(--danger-color)"><Monitor /></el-icon>
        </div>
        <div class="card-body">
          <span class="card-val">{{ data.thread?.liveThreads ?? '-' }}</span>
          <span class="card-lbl">活跃线程</span>
        </div>
      </div>
    </div>

    <!-- 指标行 -->
    <div class="chart-row">
      <div class="chart-box">
        <h4>💾 堆内存</h4>
        <div ref="heapChartRef" class="chart-canvas"></div>
        <div v-if="data.jvm?.heap" class="chart-footer">
          <span>已用: {{ data.jvm.heap.usedMB }} MB / {{ data.jvm.heap.maxMB }} MB</span>
          <span :class="heapPercent > 85 ? 'text-danger' : heapPercent > 70 ? 'text-warning' : ''">
            已用 {{ data.jvm.heap.usedPercent }}%
          </span>
        </div>
      </div>
      <div class="chart-box">
        <h4>🖥️ CPU 负载</h4>
        <div ref="cpuChartRef" class="chart-canvas"></div>
        <div v-if="data.cpu" class="chart-footer">
          <span>物理核心 {{ data.cpu.availableProcessors }} 核</span>
          <span>{{ cpuDisplay }} / 系统 {{ data.cpu.systemCpuLoad ?? '-' }}%</span>
        </div>
      </div>
      <div class="chart-box">
        <h4>🗄️ 连接池</h4>
        <div ref="poolChartRef" class="chart-canvas"></div>
        <div v-if="data.dbPool" class="chart-footer">
          <span>活跃 {{ data.dbPool.activeConnections }}</span>
          <span>空闲 {{ data.dbPool.idleConnections }} / 等待 {{ data.dbPool.threadsAwaitingConnection }}</span>
        </div>
      </div>
    </div>

    <!-- 详情表格区 -->
    <div class="detail-grid">
      <div class="detail-card">
        <h4>💻 JVM 详情</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="JVM 名称">{{ data.jvm?.jvmName }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ data.jvm?.jvmVersion }}</el-descriptions-item>
          <el-descriptions-item label="堆初始">{{ data.jvm?.heap?.initMB }} MB</el-descriptions-item>
          <el-descriptions-item label="堆已提交">{{ data.jvm?.heap?.committedMB }} MB</el-descriptions-item>
          <el-descriptions-item label="非堆已用">{{ data.jvm?.nonHeap?.usedMB }} MB</el-descriptions-item>
          <el-descriptions-item label="可用处理器">{{ data.jvm?.availableProcessors }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="detail-card">
        <h4>🧵 线程 / GC</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="当前线程">{{ data.thread?.liveThreads }}</el-descriptions-item>
          <el-descriptions-item label="峰值线程">{{ data.thread?.peakThreads }}</el-descriptions-item>
          <el-descriptions-item label="守护线程">{{ data.thread?.daemonThreads }}</el-descriptions-item>
          <el-descriptions-item label="累计启动">{{ data.thread?.totalStartedThreads }}</el-descriptions-item>
          <el-descriptions-item label="GC 总次数">{{ data.gc?.totalGcCount }}</el-descriptions-item>
          <el-descriptions-item label="GC 总耗时">{{ data.gc?.totalGcTimeMs }} ms</el-descriptions-item>
        </el-descriptions>
        <el-collapse style="margin-top:8px">
          <el-collapse-item title="GC 详情" name="gc">
            <div v-for="g in data.gc?.gcList" :key="g.name" class="gc-item">
              <span class="gc-name">{{ g.name }}</span>
              <span>次数: {{ g.collectionCount ?? '-' }}</span>
              <span>耗时: {{ g.collectionTimeMs ?? '-' }} ms</span>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
      <div class="detail-card">
        <h4>💿 磁盘</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="路径">{{ data.disk?.path }}</el-descriptions-item>
          <el-descriptions-item label="总量">{{ data.disk?.totalGB }} GB</el-descriptions-item>
          <el-descriptions-item label="已用">
            <span :class="(data.disk?.usedPercent || 0) > 85 ? 'text-danger' : ''">
              {{ data.disk?.usedGB }} GB ({{ data.disk?.usedPercent }}%)
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="剩余">{{ data.disk?.freeGB }} GB</el-descriptions-item>
          <el-descriptions-item label="可写">{{ data.disk?.writable ? '✅ 是' : '❌ 否' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="detail-card">
        <h4>🔌 Actuator 端点</h4>
        <div style="display:flex;flex-wrap:wrap;gap:8px;padding:8px 0">
          <el-tag
            v-for="ep in actuatorEndpoints"
            :key="ep.path"
            :type="ep.type"
            hit
            style="cursor:pointer"
            @click="openActuator(ep.path)"
          >
            {{ ep.label }}
          </el-tag>
        </div>
        <div class="actuator-note">
          <el-icon><InfoFilled /></el-icon>
          <span>点击标签跳转，需管理员权限</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import echarts, { cssVar } from '@/utils/echarts'
import { getSystemStatus } from '@/api/system'

const loading = ref(false)
const data = ref({})
const lastRefresh = ref('')
const autoRefresh = ref(false)
let timer = null

const heapChartRef = ref(null)
const cpuChartRef = ref(null)
const poolChartRef = ref(null)
let heapChart = null
let cpuChart = null
let poolChart = null

const actuatorEndpoints = [
  { label: '健康检查', path: '/actuator/health', type: 'success' },
  { label: '指标', path: '/actuator/metrics', type: '' },
  { label: 'JVM 指标', path: '/actuator/metrics/jvm.memory.used', type: '' },
  { label: '线程转储', path: '/actuator/threaddump', type: 'warning' },
  { label: '环境变量', path: '/actuator/env', type: '' },
  { label: '日志级别', path: '/actuator/loggers', type: '' },
]

const cpuDisplay = computed(() => {
  const v = data.value.cpu?.processCpuLoad
  return v != null ? v + '%' : '-'
})

const heapPercent = computed(() => data.value.jvm?.heap?.usedPercent ?? 0)

// ── 加载数据 ──
async function loadData() {
  loading.value = true
  try {
    const res = await getSystemStatus()
    if (res.code === 200) {
      data.value = res.data || {}
      lastRefresh.value = new Date().toLocaleTimeString()
      await nextTick()
      renderCharts()
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch { ElMessage.error('请求失败') }
  finally { loading.value = false }
}

// ── ECharts ──
function renderCharts() {
  renderHeapChart()
  renderCpuChart()
  renderPoolChart()
}

function getHeapOption() {
  const heap = data.value.jvm?.heap
  if (!heap) return {}
  const pct = heap.usedPercent
  return {
    series: [{
      type: 'gauge',
      startAngle: 210,
      endAngle: -30,
      center: ['50%', '55%'],
      radius: '80%',
      min: 0, max: 100,
      splitNumber: 5,
      progress: { show: true, width: 6, itemStyle: { color: pct > 85 ? cssVar('--el-color-danger') : pct > 70 ? cssVar('--el-color-warning') : cssVar('--el-color-success') } },
      axisLine: { lineStyle: { width: 6, color: [[pct / 100, cssVar('--primary-color')], [1, '#e4e7ed']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      detail: { show: false },
      data: [{ value: pct, name: '' }]
    }],
    graphic: [{
      type: 'text', left: 'center', top: '42%',
      style: { text: pct + '%', fill: pct > 85 ? cssVar('--el-color-danger') : cssVar('--primary-color'), fontSize: 24, fontWeight: 'bold', textAlign: 'center' }
    }]
  }
}

function getCpuOption() {
  const pct = data.value.cpu?.processCpuLoad ?? 0
  return {
    series: [{
      type: 'gauge',
      startAngle: 210, endAngle: -30,
      center: ['50%', '55%'], radius: '80%',
      min: 0, max: 100, splitNumber: 5,
      progress: { show: true, width: 6, itemStyle: { color: pct > 80 ? cssVar('--el-color-danger') : pct > 60 ? cssVar('--el-color-warning') : cssVar('--el-color-success') } },
      axisLine: { lineStyle: { width: 6, color: [[pct / 100, cssVar('--primary-color')], [1, '#e4e7ed']] } },
      axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
      detail: { show: false },
      data: [{ value: pct }]
    }],
    graphic: [{
      type: 'text', left: 'center', top: '42%',
      style: { text: pct + '%', fill: cssVar('--primary-color'), fontSize: 24, fontWeight: 'bold', textAlign: 'center' }
    }]
  }
}

function getPoolOption() {
  const db = data.value.dbPool || {}
  const active = db.activeConnections || 0
  const idle = db.idleConnections || 0
  const max = db.maximumPoolSize || 100
  const total = active + idle
  return {
    series: [{
      type: 'gauge',
      startAngle: 210, endAngle: -30,
      center: ['50%', '55%'], radius: '80%',
      min: 0, max: max, splitNumber: 4,
      progress: { show: true, width: 6, itemStyle: { color: active > max * 0.8 ? cssVar('--el-color-danger') : cssVar('--el-color-success') } },
      axisLine: { lineStyle: { width: 6, color: [[total / max, cssVar('--primary-color')], [1, '#e4e7ed']] } },
      axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
      detail: { show: false },
      data: [{ value: total }]
    }],
    graphic: [{
      type: 'text', left: 'center', top: '38%',
      style: { text: total + '/' + max, fill: cssVar('--primary-color'), fontSize: 20, fontWeight: 'bold', textAlign: 'center' }
    }, {
      type: 'text', left: 'center', top: '55%',
      style: { text: '活跃: ' + active + ' | 空闲: ' + idle, fill: elInfo, fontSize: 10, textAlign: 'center' }
    }]
  }
}

function renderHeapChart() {
  if (heapChart) heapChart.dispose()
  const el = heapChartRef.value
  if (!el) return
  heapChart = echarts.init(el)
  heapChart.setOption(getHeapOption())
}

function renderCpuChart() {
  if (cpuChart) cpuChart.dispose()
  const el = cpuChartRef.value
  if (!el) return
  cpuChart = echarts.init(el)
  cpuChart.setOption(getCpuOption())
}

function renderPoolChart() {
  if (poolChart) poolChart.dispose()
  const el = poolChartRef.value
  if (!el) return
  poolChart = echarts.init(el)
  poolChart.setOption(getPoolOption())
}

function openActuator(path) {
  window.open(window.location.origin + '/api' + path, '_blank')
}

// ── 自动刷新 ──
onMounted(() => { loadData() })
onUnmounted(() => { clearInterval(timer) })

// 自动刷新开关
import { watch } from 'vue'
import { elInfo } from '@/utils/theme'
watch(autoRefresh, (v) => {
  clearInterval(timer)
  if (v) timer = setInterval(loadData, 10000)
})
</script>

<style scoped>
.monitor-page { margin: 0 auto; padding: 16px; display: flex; flex-direction: column; gap: 16px; }
.monitor-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.monitor-header h3 { margin: 0; }
.refresh-hint { font-size: var(--fs-xs); color: var(--text-secondary); }
.card-grid { display: grid; gap: 12px; }
.cols-4 { grid-template-columns: repeat(4, 1fr); }
.stat-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 14px; border: 1px solid var(--border-color);
  display: flex; align-items: center; gap: 12px; }
.card-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.card-body { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }
.card-val { font-size: var(--fs-xl); font-weight: 700; color: var(--text-primary); }
.card-val.small { font-size: var(--fs-md); }
.card-lbl { font-size: var(--fs-xs); color: var(--text-secondary); white-space: nowrap; }

.chart-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.chart-box { background: var(--bg-card); border-radius: var(--radius-md); padding: 12px; border: 1px solid var(--border-color); }
.chart-box h4 { margin: 0 0 4px 0; font-size: var(--fs-md); }
.chart-canvas { height: 160px; }
.chart-footer { display: flex; justify-content: space-between; font-size: var(--fs-xs); color: var(--text-secondary); padding-top: 4px; }

.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.detail-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 12px; border: 1px solid var(--border-color); }
.detail-card h4 { margin: 0 0 8px 0; font-size: var(--fs-md); }

.text-danger { color: var(--el-color-danger, #f56c6c); font-weight: 600; }
.text-warning { color: var(--el-color-warning, #e6a23c); font-weight: 600; }
.gc-item { display: flex; gap: 12px; font-size: var(--fs-sm); padding: 4px 0; }
.gc-name { font-weight: 500; min-width: 80px; }
.actuator-note { display: flex; align-items: center; gap: 4px; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 8px; }

@media (max-width: 900px) {
  .cols-4 { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
  .detail-grid { grid-template-columns: 1fr; }
}
</style>
