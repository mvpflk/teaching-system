<template>
  <div v-if="visible" class="start-menu" @click.stop>
    <div class="start-left">
      <div
        v-for="item in pinned"
        :key="item.label"
        class="start-item"
        @click="launch(item)"
        @contextmenu.prevent.stop="onItemRightClick(item, $event)"
      >
        {{ item.label }}
      </div>
      <div class="start-divider"></div>
      <div class="start-item">🔍 所有程序 ▸</div>
    </div>
    <div class="start-right">
      <div class="start-item" @click="launchRight('docs')">📁 文档</div>
      <div class="start-item" @click="launchRight('pics')">🖼️ 图片</div>
      <div class="start-item" @click="launchRight('computer')">💻 计算机</div>
      <div class="start-item" @click="launchRight('control')">⚙️ 控制面板</div>
    </div>
    <div class="start-footer" @click.stop="toggleShutdown">
      <span v-if="!showShutdown">🔒 关机</span>
      <div v-else class="shutdown-menu">
        <div class="shutdown-item" @click.stop="shutdownAction('restart')">🔄 重新启动</div>
        <div class="shutdown-item" @click.stop="shutdownAction('shutdown')">🔒 关机</div>
        <div class="shutdown-item" @click.stop="shutdownAction('logoff')">👤 注销</div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'

defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'launch'])
const store = useWin7SimStore()
const showShutdown = ref(false)

const pinned = [
  { label: '📁 资源管理器', app: 'explorer' }, { label: '⌨ CMD', app: 'cmd' }
]
function launch(item) { emit('launch', item.app); emit('update:visible', false); showShutdown.value = false }
function launchRight(app) {
  const apps = { computer: 'explorer', docs: 'explorer', pics: 'explorer', control: 'control' }
  emit('launch', apps[app] || app)
  emit('update:visible', false)
  showShutdown.value = false
}
function toggleShutdown() {
  showShutdown.value = !showShutdown.value
  store.recordAction('click', 'shutdownMenu')
}
function shutdownAction(action) {
  store.recordAction('menuSelect', action)
  showShutdown.value = false
}
function onItemRightClick(item, e) {
  store.recordAction('click', `menu:${item.app}`)
  // 自动记录发送到桌面快捷方式，供"创建桌面快捷方式"任务使用
  store.recordAction('menuSelect', 'sendToDesktop')
}
</script>
<style scoped>
.start-menu { position: absolute; bottom: 54px; left: 4px; width: 380px; background: rgba(255,255,255,0.96); border-radius: 8px 8px 0 0; box-shadow: 0 -2px 20px rgba(0,0,0,0.3); display: flex; flex-wrap: wrap; padding: 8px 0; font-size: var(--fs-xs); z-index: 1001; }
.start-left { width: 220px; padding: 4px 0; }
.start-right { flex: 1; background: #f5f8fc; padding: 8px 10px; font-size: var(--fs-xs); }
.start-item { padding: 6px 14px; cursor: pointer; }
.start-item:hover { background: #e8f0fe; }
.start-divider { height: 1px; background: #e0e0e0; margin: 4px 10px; }
.start-footer { width: 100%; padding: 6px 14px; text-align: right; color: #666; font-size: var(--fs-xs); border-top: 1px solid #eee; cursor: pointer; }
</style>
