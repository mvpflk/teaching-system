<template>
  <div
    v-if="!win.minimized"
    class="win7-window"
    :style="{ left: win.maximized ? '0' : win.x + 'px', top: win.maximized ? '0' : win.y + 'px', width: win.maximized ? '100%' : win.w + 'px', height: win.maximized ? 'calc(100% - 50px)' : win.h + 'px', zIndex: win.zIndex }"
    @mousedown="store.focusWindow(win.id)"
  >
    <div class="win-titlebar" @mousedown.left="startDrag">
      <div class="win-title">{{ win.title }}</div>
      <div class="win-controls">
        <span class="win-btn win-min" @click.stop="doMinimize">─</span>
        <span class="win-btn win-max" @click.stop="doToggleMax">□</span>
        <span class="win-btn win-close" @click.stop="store.closeWindow(win.id)">✕</span>
      </div>
    </div>
    <div class="win-body"><slot /></div>
    <div v-if="$slots.statusbar" class="win-statusbar"><slot name="statusbar" /></div>
  </div>
</template>
<script setup>
import { useWin7SimStore } from '@/stores/win7Sim'
const props = defineProps({ win: Object })
const store = useWin7SimStore()

let dragging = false, ox = 0, oy = 0

function doMinimize() {
  store.recordAction('click', 'minimize')
  store.toggleMinimize(props.win.id)
}
function doToggleMax() {
  if (props.win.maximized) {
    store.recordAction('click', 'restore')
  } else {
    store.recordAction('click', 'maximize')
  }
  store.toggleMaximize(props.win.id)
}

function startDrag(e) {
  if (props.win.maximized) return
  dragging = true; ox = e.clientX - props.win.x; oy = e.clientY - props.win.y
  const onDrag = (ev) => { if (dragging) { props.win.x = Math.max(-50, ev.clientX - ox); props.win.y = Math.max(0, ev.clientY - oy) } }
  const stopDrag = () => { dragging = false; document.removeEventListener('mousemove', onDrag); document.removeEventListener('mouseup', stopDrag) }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}
</script>
<style scoped>
.win7-window { position: absolute; background: #fff; border-radius: 8px 8px 0 0; box-shadow: 0 2px 20px rgba(0,0,0,0.2); display: flex; flex-direction: column; overflow: hidden; min-width: 280px; }
.win-titlebar { background: linear-gradient(to bottom, rgba(220,230,245,0.95), rgba(200,215,235,0.95)); padding: 6px 10px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #b8cfe0; cursor: default; }
.win-title { font-size: var(--fs-xs); color: #333; }
.win-controls { display: flex; gap: 4px; }
.win-btn { width: 24px; height: 20px; border-radius: 3px; border: 1px solid #ccc; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 10px; cursor: pointer; color: #555; }
.win-btn:hover { background: #e0e0e0; }
.win-close:hover { background: #e81123; color: #fff; border-color: #c00; }
.win-body { flex: 1; overflow: auto; background: #fff; }
.win-statusbar { background: #f0f4f8; border-top: 1px solid #e0e0e0; padding: 2px 8px; font-size: 10px; color: #888; }
</style>
