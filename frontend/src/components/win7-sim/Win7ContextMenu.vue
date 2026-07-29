<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="context-menu"
      :style="{ left: x + 'px', top: y + 'px' }"
      @click.stop
      @contextmenu.prevent
    >
      <template v-for="(item, i) in items" :key="i">
        <div v-if="item.divider" class="context-divider"></div>
        <div
          v-else
          class="context-item"
          :class="{ disabled: item.disabled }"
          @click="!item.disabled && handleClick(item)"
        >
          <span>{{ item.label }}</span>
          <span v-if="item.children" class="arrow">▸</span>
        </div>
      </template>
    </div>
  </Teleport>
</template>
<script setup>
import { watch, onBeforeUnmount } from 'vue'
const props = defineProps({ visible: Boolean, x: Number, y: Number, items: { type: Array, default: () => [] } })
const emit = defineEmits(['close', 'action'])

function handleClick(item) { emit('action', item.key || item.label); emit('close') }

function clickOutside() { if (props.visible) emit('close') }

watch(() => props.visible, (v) => {
  if (v) setTimeout(() => document.addEventListener('click', clickOutside, { once: true }), 0)
})
onBeforeUnmount(() => document.removeEventListener('click', clickOutside))
</script>
<style scoped>
.context-menu { position: fixed; min-width: 180px; background: rgba(250,250,250,0.97); border: 1px solid #c8c8c8; border-radius: 4px; box-shadow: 2px 2px 12px rgba(0,0,0,0.15); padding: 4px 0; font-size: var(--fs-xs); z-index: 9999; }
.context-item { padding: 5px 16px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; }
.context-item:hover { background: #e8f0fe; }
.context-item.disabled { color: #999; cursor: default; background: transparent; }
.context-divider { height: 1px; background: #e0e0e0; margin: 4px 8px; }
.arrow { color: #888; }
</style>
