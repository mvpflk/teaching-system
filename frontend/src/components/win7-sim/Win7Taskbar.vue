<template>
  <div class="win7-taskbar" @click="onTaskbarClick" @contextmenu.prevent="onTaskbarRightClick">
    <Win7StartMenu :visible="startOpen" @update:visible="startOpen = $event" @launch="$emit('launch', $event)" />
    <button class="start-button" @click.stop="toggleStart">
      <span class="start-icon">⊞</span>
    </button>
    <div class="taskbar-programs">
      <div
        v-for="win in store.openWindows.filter(w => !w.minimized)"
        :key="win.id"
        class="taskbar-item"
        :class="{ active: win.zIndex === maxZ }"
        @click.stop="store.focusWindow(win.id)"
      >
        {{ win.title }}
      </div>
    </div>
    <div class="taskbar-spacer"></div>
    <div class="taskbar-notify">
      <span class="notify-time">{{ time }}</span>
      <div class="show-desktop" title="显示桌面"></div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'
import Win7StartMenu from './Win7StartMenu.vue'
const store = useWin7SimStore()
const startOpen = ref(false)
const time = computed(() => new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }))
const maxZ = computed(() => Math.max(...store.openWindows.map(w => w.zIndex), 0))
const emit = defineEmits(['launch', 'taskbarCtx'])

function onTaskbarClick(e) {
  // 点击任务栏空白区域 → 记录为找到任务栏
  store.recordAction('click', 'taskbar')
}

function toggleStart() {
  startOpen.value = !startOpen.value
  store.recordAction('click', 'startButton')
}

function onTaskbarRightClick(e) {
  store.recordAction('rightClick', 'taskbar')
  emit('taskbarCtx', e)
}
</script>
<style scoped>
.win7-taskbar { position: absolute; bottom: 0; left: 0; right: 0; height: 50px; background: linear-gradient(to bottom, rgba(70,130,200,0.7), rgba(24,24,40,0.88)); backdrop-filter: blur(10px); display: flex; align-items: center; padding: 0 8px; gap: 6px; z-index: 1000; }
.start-button { width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, #1e90ff, #0066cc); border: none; cursor: pointer; box-shadow: 0 0 8px rgba(30,144,255,0.5); flex-shrink: 0; }
.taskbar-programs { display: flex; gap: 2px; flex: 1; overflow-x: auto; }
.taskbar-item { padding: 4px 14px; background: rgba(255,255,255,0.08); border-radius: 4px; color: #fff; font-size: var(--fs-xs); cursor: pointer; white-space: nowrap; transition: background 0.15s; }
.taskbar-item:hover { background: rgba(255,255,255,0.18); }
.taskbar-item.active { background: rgba(255,255,255,0.22); border-bottom: 2px solid #5ba3e6; }
.taskbar-spacer { flex: 1; }
.taskbar-notify { display: flex; align-items: center; gap: 10px; }
.notify-time { color: #fff; font-size: var(--fs-xs); white-space: nowrap; }
.show-desktop { width: 8px; height: 50px; background: rgba(255,255,255,0.08); border-left: 1px solid rgba(255,255,255,0.12); cursor: pointer; }
</style>
