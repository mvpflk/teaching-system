<template>
  <div
    class="desktop-icon"
    @click.stop="$emit('click')"
    @dblclick.stop="$emit('dblclick')"
    @contextmenu.prevent.stop="$emit('contextmenu')"
  >
    <div class="icon-image">{{ icon.icon }}</div>
    <span class="icon-label">{{ icon.name }}</span>
  </div>
</template>
<script setup>
defineProps({ icon: Object })
defineEmits(['click', 'dblclick', 'contextmenu'])
</script>
<style scoped>
.desktop-icon { width: 72px; text-align: center; cursor: pointer; padding: 6px; border-radius: 4px; border: 1px solid transparent; }
.desktop-icon:hover { background: rgba(255,255,255,0.15); border-color: rgba(255,255,255,0.3); }
.icon-image { width: 40px; height: 40px; margin: 0 auto 4px; display: flex; align-items: center; justify-content: center; font-size: 28px; background: rgba(255,255,255,0.1); border-radius: 4px; }
.icon-label { color: #fff; font-size: var(--fs-xs); text-shadow: 0 1px 2px rgba(0,0,0,0.5); word-break: break-all; }
</style>
