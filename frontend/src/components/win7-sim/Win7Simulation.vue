<template>
  <div
    ref="simRoot"
    class="win7-simulation"
    tabindex="0"
    @contextmenu.prevent="onDesktopCtx($event)"
    @keydown="onGlobalKeyDown"
  >
    <Win7Desktop
      @desktop-click="ctxMenu.visible = false"
      @desktop-context-menu="onDesktopCtx"
      @icon-click="onIconClick"
      @icon-dbl-click="onIconDblClick"
      @icon-context-menu="onIconCtx"
    />

    <Win7Window v-for="win in store.openWindows" :key="win.id" :win="win">
      <Win7FileExplorer v-if="win.app === 'explorer'" />
      <Win7ControlPanel v-else-if="win.app === 'control'" />
      <Win7Cmd v-else-if="win.app === 'cmd'" />
      <Win7TaskManager v-else-if="win.app === 'taskmanager'" />
      <div v-else class="win-placeholder">{{ win.title }} — 功能开发中</div>
    </Win7Window>

    <TaskPanel v-if="mode === 'practice'" />
    <ScoreFeedback />

    <Win7ContextMenu
      :visible="ctxMenu.visible"
      :x="ctxMenu.x"
      :y="ctxMenu.y"
      :items="ctxMenu.items"
      @close="ctxMenu.visible = false"
      @action="onCtxAction"
    />

    <Win7Taskbar @launch="onLaunchApp" @taskbar-ctx="onTaskbarCtx" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'
import Win7Desktop from './Win7Desktop.vue'
import Win7Window from './Win7Window.vue'
import Win7FileExplorer from './Win7FileExplorer.vue'
import Win7ControlPanel from './Win7ControlPanel.vue'
import Win7Cmd from './Win7Cmd.vue'
import Win7TaskManager from './Win7TaskManager.vue'
import Win7ContextMenu from './Win7ContextMenu.vue'
import Win7Taskbar from './Win7Taskbar.vue'
import TaskPanel from './TaskPanel.vue'
import ScoreFeedback from './ScoreFeedback.vue'

const props = defineProps({ mode: { type: String, default: 'practice' } })
const store = useWin7SimStore()
const simRoot = ref(null)

const ctxMenu = reactive({ visible: false, x: 0, y: 0, items: [] })

// ═══════ 全局键盘事件处理 ═══════
function onGlobalKeyDown(e) {
  // 在输入框内不拦截
  if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return

  // 阻止 F1/F2 浏览器默认行为
  if (e.key === 'F1' || e.key === 'F2') e.preventDefault()

  // Ctrl+Shift (切换输入法)
  if (e.ctrlKey && e.shiftKey && !e.altKey) {
    store.recordAction('keyDown', 'Ctrl+Shift')
    return
  }
  // Ctrl+C (复制)
  if (e.ctrlKey && e.key === 'c') {
    store.recordAction('keyDown', 'Ctrl+C')
    return
  }
  // Ctrl+V (粘贴)
  if (e.ctrlKey && e.key === 'v') {
    store.recordAction('keyDown', 'Ctrl+V')
    return
  }
  // Alt+Tab (窗口切换) — 注意浏览器无法完全拦截
  if (e.altKey && e.key === 'Tab') {
    e.preventDefault()
    store.recordAction('keyDown', 'Alt+Tab')
    return
  }
  // Shift+Delete (永久删除)
  if (e.shiftKey && e.key === 'Delete') {
    store.recordAction('keyDown', 'Shift+Delete')
    return
  }
  // Delete 单独按
  if (e.key === 'Delete') {
    store.recordAction('keyDown', 'Delete')
    return
  }
  // F1 / F2
  if (e.key === 'F1') {
    store.recordAction('keyDown', 'F1')
    return
  }
  if (e.key === 'F2') {
    store.recordAction('keyDown', 'F2')
    return
  }
  // Enter
  if (e.key === 'Enter') {
    store.recordAction('keyDown', 'Enter')
    return
  }
  // Ctrl 单独（多选场景用 Ctrl+click）
  if (e.ctrlKey && e.key === 'Control') {
    store.recordAction('keyDown', 'Ctrl+click')
    return
  }
}

// 全局键盘事件 — 使用 document 级别监听确保始终捕获
onMounted(() => {
  document.addEventListener('keydown', onGlobalKeyDown)
  simRoot.value?.focus()
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onGlobalKeyDown)
})

// ── 桌面右键菜单 ──
function onDesktopCtx(e) {
  if (!e || !e.clientX) return
  store.recordAction('rightClick', 'desktop')
  ctxMenu.visible = true; ctxMenu.x = e.clientX; ctxMenu.y = e.clientY
  ctxMenu.items = [
    { label: '查看', key: 'view' },
    { label: '排序方式', key: 'sortByName' },
    { label: '', divider: true },
    { label: '新建', key: 'new' },
    { label: '', divider: true },
    { label: '个性化', key: 'personalize' },
    { label: '屏幕分辨率', key: 'screenResolution' }
  ]
}

// ── 桌面图标 ──
function onIconClick(icon) {
  store.recordAction('click', `desktop_icon:${icon.id}`)
}
function onIconDblClick(icon) {
  const apps = {
    computer: { app: 'explorer', title: '计算机', w: 760, h: 500 },
    documents: { app: 'explorer', title: '我的文档', w: 700, h: 450 }
  }
  const cfg = apps[icon.id]
  if (cfg) store.openWindow(cfg.app, cfg.title, cfg)
  store.recordAction('dblClick', `desktop_icon:${icon.id}`)
  checkTaskStep()
}
function onIconCtx(icon, e) {
  ctxMenu.visible = true; ctxMenu.x = e.clientX; ctxMenu.y = e.clientY
  ctxMenu.items = [
    { label: '打开', key: `open_${icon.id}` },
    { label: '', divider: true },
    { label: '发送到', key: 'sendTo' },
    { label: '创建快捷方式', key: 'shortcut' },
    { label: '重命名', key: 'rename' },
    { label: '', divider: true },
    { label: '属性', key: 'properties' }
  ]
  store.recordAction('rightClick', `desktop_icon:${icon.id}`)
}

// ── 任务栏右键 ──
function onTaskbarCtx(e) {
  ctxMenu.visible = true; ctxMenu.x = e.clientX; ctxMenu.y = e.clientY
  ctxMenu.items = [
    { label: '工具栏', children: true },
    { label: '层叠窗口', key: 'cascadeWindows' },
    { label: '堆叠显示窗口', key: 'stackWindows' },
    { label: '并排显示窗口', key: 'sideBySide' },
    { label: '', divider: true },
    { label: '属性', key: 'taskbarProperties' },
    { label: '', divider: true },
    { label: '锁定任务栏', key: 'lockTaskbar' }
  ]
}

// ── 右键动作 ──
function onCtxAction(key) {
  store.recordAction('menuSelect', key)
  if (key === 'personalize') {
    store.openWindow('control', '个性化', { w: 650, h: 450 })
    // 个性化窗口内直接记录 bg:solid 供更换桌面背景任务使用
    setTimeout(() => {
      store.recordAction('click', 'bg:solid')
    }, 300)
  }
  if (key === 'open_computer') store.openWindow('explorer', '计算机', { w: 760, h: 500 })
  if (key === 'screenResolution') {
    store.openWindow('control', '屏幕分辨率', { w: 500, h: 400 })
    setTimeout(() => {
      store.recordAction('click', 'resolution:1366x768')
    }, 300)
  }
  if (key === 'taskbarProperties') store.openWindow('control', '任务栏属性', { w: 500, h: 400 })
  if (key === 'restart') {
    // 安全模式识别任务 — 重启操作
    store.recordAction('menuSelect', 'restart')
  }
  checkTaskStep()
}

// ── 开始菜单启动 ──
function onLaunchApp(app) {
  const apps = {
    explorer: { title: '资源管理器', w: 760, h: 500 },
    cmd: { title: '命令提示符', w: 660, h: 420 },
    control: { title: '控制面板', w: 650, h: 450 }
  }
  const cfg = apps[app]
  if (cfg) store.openWindow(app, cfg.title, cfg)
  else store.openWindow(app, app, { w: 600, h: 400 })
  store.recordAction('launch', app)
}

// ── 任务步骤自动检测 ──
function checkTaskStep() {
  if (!store.currentTask || props.mode !== 'practice') return
  const nextIdx = store.taskSteps.findIndex(s => !s.completed)
  if (nextIdx < 0) return
  const step = store.taskSteps[nextIdx]
  if (store.validateStep(step)) {
    step.completed = true
    store.showFeedback('success', `✅ ${step.name} — 完成！`)
  }
}
</script>

<style scoped>
.win7-simulation { position: relative; width: 100%; height: 100vh; overflow: hidden; background: #0078d4; font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif; user-select: none; }
.win-placeholder { display: flex; align-items: center; justify-content: center; height: 100%; color: #999; font-size: var(--fs-md); }
</style>
