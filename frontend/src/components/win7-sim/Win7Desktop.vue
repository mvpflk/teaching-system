<template>
  <div
    class="win7-desktop"
    :style="{ background: bg }"
    @click.self="$emit('desktopClick')"
    @contextmenu.prevent="$emit('desktopContextMenu', $event)"
  >
    <Win7DesktopIcon
      v-for="icon in store.desktopIcons"
      :key="icon.id"
      :icon="icon"
      @click="$emit('iconClick', icon)"
      @dblclick="$emit('iconDblClick', icon)"
      @contextmenu="$emit('iconContextMenu', icon, $event)"
    />
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'
import Win7DesktopIcon from './Win7DesktopIcon.vue'
const store = useWin7SimStore()
defineEmits(['desktopClick', 'desktopContextMenu', 'iconClick', 'iconDblClick', 'iconContextMenu'])
const bg = computed(() => store.wallpaper === 'default' ? 'linear-gradient(135deg, #0a5c9e 0%, #45b4e8 50%, #8ed6f5 100%)' : '#0078d4')
</script>
<style scoped>
.win7-desktop { position: absolute; top: 0; left: 0; right: 0; bottom: 50px; padding: 20px; display: flex; flex-direction: column; gap: 12px; flex-wrap: wrap; align-content: flex-start; overflow: hidden; }
</style>
