<template>
  <div class="topology-designer">
    <div class="td-header">
      <h4>🔗 网络拓扑设计器</h4>
      <el-tag size="small" type="info">网络应用基础 — 单元1-2</el-tag>
    </div>

    <div class="td-hint">提示：拖拽设备到画布 | 双击设备开始连线 | 点击空白取消</div>

    <div class="td-body">
      <!-- 设备库 -->
      <div class="device-palette">
        <h5>设备库</h5>
        <div class="device-list">
          <div
            v-for="d in devices"
            :key="d.type"
            class="device-item"
            draggable="true"
            @dragstart="onDragStart($event, d)"
          >
            <span class="d-icon">{{ d.icon }}</span>
            <span class="d-label">{{ d.label }}</span>
          </div>
        </div>
        <el-button size="small" style="margin-top:8px;width:100%" @click="clearCanvas">清空画布</el-button>
        <el-button size="small" type="primary" style="margin-top:4px;width:100%" @click="autoCheck">自动检查</el-button>
      </div>

      <!-- 画布 -->
      <div
        class="topology-canvas"
        @drop.prevent="onDrop"
        @dragover.prevent
        @click="deselectAll"
      >
        <svg class="connection-layer" v-if="connections.length">
          <line
            v-for="(conn, i) in connections"
            :key="i"
            :x1="conn.x1" :y1="conn.y1" :x2="conn.x2" :y2="conn.y2"
            stroke="#4361ee" stroke-width="2"
            marker-end="url(#arrowhead)"
          />
          <defs>
            <marker id="arrowhead" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
              <polygon points="0 0, 8 3, 0 6" fill="#4361ee" />
            </marker>
          </defs>
        </svg>

        <div
          v-for="(item, i) in placedDevices"
          :key="i"
          class="placed-device"
          :class="{ selected: selectedIndex === i }"
          :style="{ left: item.x + 'px', top: item.y + 'px' }"
          @mousedown.stop="selectDevice(i)"
          @dblclick="startConnect(i)"
        >
          <span class="pd-icon">{{ item.icon }}</span>
          <span class="pd-label">{{ item.label }}</span>
          <el-icon v-if="connectingFrom === i" class="connect-indicator"><Link /></el-icon>
        </div>

        <el-empty v-if="!placedDevices.length" description="拖拽设备到画布开始设计" :image-size="60" />
      </div>
    </div>

    <!-- 连接提示 -->
    <div v-if="connectingFrom !== null" class="connect-hint">
      请点击目标设备完成连线（点击空白取消）
    </div>

    <!-- 连线列表（可删除） -->
    <div v-if="connections.length" class="conn-list">
      <span v-for="(c, i) in connections" :key="i" class="conn-tag">
        连线{{ i + 1 }}: {{ getDeviceName(c.fromType) }} → {{ getDeviceName(c.toType) }}
        <el-icon class="del-icon" @click="connections.splice(i,1)"><Close /></el-icon>
      </span>
    </div>

    <!-- 检查结果 -->
    <div v-if="checkResult" class="check-panel">
      <el-alert :title="checkResult.title" :type="checkResult.ok ? 'success' : 'warning'" :closable="false" show-icon />
      <ul v-if="checkResult.details?.length">
        <li v-for="(d, i) in checkResult.details" :key="i">{{ d }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Link, Close } from '@element-plus/icons-vue'

const devices = [
  { type: 'router', icon: '📡', label: '路由器' },
  { type: 'switch', icon: '🔀', label: '交换机' },
  { type: 'hub', icon: '🔌', label: '集线器' },
  { type: 'pc', icon: '🖥️', label: 'PC' },
  { type: 'server', icon: '🗄️', label: '服务器' },
  { type: 'firewall', icon: '🛡️', label: '防火墙' },
  { type: 'cloud', icon: '☁️', label: '云/Internet' },
  { type: 'printer', icon: '🖨️', label: '打印机' },
]

const placedDevices = ref([])
const connections = ref([])
const selectedIndex = ref(null)
const connectingFrom = ref(null)
const checkResult = ref(null)
const dragDevice = ref(null)

function onDragStart(e, device) {
  dragDevice.value = device
  e.dataTransfer.effectAllowed = 'copy'
}

function onDrop(e) {
  if (!dragDevice.value) return
  const canvas = e.currentTarget
  const rect = canvas.getBoundingClientRect()
  const x = e.clientX - rect.left - 24
  const y = e.clientY - rect.top - 24
  placedDevices.value.push({ ...dragDevice.value, x: Math.max(0, x), y: Math.max(0, y) })
  dragDevice.value = null
}

function selectDevice(i) {
  if (connectingFrom.value !== null && connectingFrom.value !== i) {
    // 完成连接
    const from = placedDevices.value[connectingFrom.value]
    const to = placedDevices.value[i]
    connections.value.push({
      x1: from.x + 24, y1: from.y + 24,
      x2: to.x + 24, y2: to.y + 24,
      fromType: from.type,
      toType: to.type
    })
    connectingFrom.value = null
  } else {
    selectedIndex.value = i
  }
}

function deselectAll() {
  selectedIndex.value = null
  connectingFrom.value = null
}

function startConnect(i) {
  connectingFrom.value = i
  selectedIndex.value = i
}

function getDeviceName(type) {
  const map = { router:'路由', switch:'交换', hub:'集线器', pc:'PC', server:'服务器', firewall:'防火墙', cloud:'云', printer:'打印机' }
  return map[type] || type
}

function clearCanvas() {
  placedDevices.value = []
  connections.value = []
  checkResult.value = null
}

function autoCheck() {
  const details = []
  const deviceTypes = placedDevices.value.map(d => d.type)

  // 检查1：是否有路由器
  if (!deviceTypes.includes('router')) details.push('❌ 缺少路由器 — 网络需要路由器连接不同网络')
  else details.push('✅ 路由器已配置')

  // 检查2：是否有交换机
  if (!deviceTypes.includes('switch') && !deviceTypes.includes('hub'))
    details.push('⚠️ 建议添加交换机或集线器连接局域网设备')

  // 检查3：是否有终端设备
  if (!deviceTypes.includes('pc') && !deviceTypes.includes('server'))
    details.push('⚠️ 建议添加至少一台PC或服务器')

  // 检查4：连线数量
  if (connections.value.length === 0) details.push('❌ 设备之间没有连线')
  else details.push(`✅ 已创建 ${connections.value.length} 条连线`)

  // 检查5：互联网连接
  const hasCloudConn = connections.value.some(c => c.fromType === 'cloud' || c.toType === 'cloud')
  if (deviceTypes.includes('cloud') && !hasCloudConn)
    details.push('⚠️ 云/Internet设备未连接到网络')

  const allOk = details.every(d => d.startsWith('✅'))
  checkResult.value = {
    ok: allOk,
    title: allOk ? '🎉 拓扑结构合理！' : `拓扑检查：${details.filter(d => d.startsWith('✅')).length}/${details.length} 项通过`,
    details
  }
}
</script>

<style scoped>
.topology-designer { padding: 16px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-color); }
.td-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.td-header h4 { margin: 0; }
.td-body { display: flex; gap: 12px; min-height: 400px; }

.device-palette { width: 140px; flex-shrink: 0; padding: 12px; background: var(--bg-page); border-radius: var(--radius-sm); }
.device-palette h5 { margin: 0 0 8px; font-size: var(--fs-sm); }
.device-list { display: flex; flex-direction: column; gap: 6px; }
.device-item { display: flex; align-items: center; gap: 6px; padding: 8px; background: #fff; border: 1px solid var(--el-border-color); border-radius: var(--radius-sm); cursor: grab; font-size: var(--fs-xs); transition: transform 0.15s; }
.device-item:hover { transform: translateY(-1px); box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.device-item:active { cursor: grabbing; }
.d-icon { font-size: 20px; }
.d-label { font-weight: 600; }

.topology-canvas { flex: 1; position: relative; background: #fafbfc; border: 2px dashed var(--el-border-color); border-radius: var(--radius-sm); min-height: 400px; overflow: hidden; }
.connection-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; }

.placed-device { position: absolute; display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 8px; background: #fff; border: 2px solid var(--el-border-color); border-radius: var(--radius-md); cursor: pointer; font-size: var(--fs-xs); transition: border-color 0.2s; z-index: 2; }
.placed-device:hover { border-color: var(--primary-color); }
.placed-device.selected { border-color: var(--primary-color); box-shadow: 0 0 8px rgba(67,97,238,0.3); }
.pd-icon { font-size: 28px; }
.pd-label { font-weight: 600; font-size: 11px; }
.connect-indicator { position: absolute; top: -6px; right: -6px; color: var(--primary-color); animation: pulse 1s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

.connect-hint { padding: 8px; background: #e8f0fe; color: var(--primary-color); border-radius: var(--radius-sm); text-align: center; font-size: var(--fs-sm); margin-top: 8px; }
.check-panel { margin-top: 12px; }
.check-panel ul { margin: 8px 0 0; padding-left: 20px; font-size: var(--fs-sm); }
.check-panel li { margin: 2px 0; }
.td-hint { padding: 6px 12px; background: #e8f0fe; border-radius: 4px; margin-bottom: 8px; font-size: 12px; color: var(--primary-color); }
.conn-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.conn-tag { padding: 2px 8px; background: var(--bg-page); border-radius: 4px; font-size: 12px; display: flex; align-items: center; gap: 4px; }
.del-icon { cursor: pointer; color: var(--el-color-danger, #f56c6c); }
</style>
