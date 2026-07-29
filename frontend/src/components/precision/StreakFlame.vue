<template>
  <div class="sf" @click="$emit('useFreeze')">
    <span class="sf-flame">{{ icon }}</span>
    <span class="sf-num">{{ streak }} 天</span>
    <span v-if="freezeCards > 0" class="sf-freeze">🧊 x{{ freezeCards }}</span>
  </div>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({ streak: Number, freezeCards: Number })
defineEmits(['useFreeze'])
const icon = computed(() => props.streak >= 30 ? '💎' : props.streak >= 7 ? '🔥🔥🔥' : props.streak >= 4 ? '🔥🔥' : props.streak >= 1 ? '🔥' : '💤')
</script>
<style scoped>
.sf { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: var(--bg-card, #fff); border: 1px solid var(--border-base, #e8e8ed); border-radius: 8px; cursor: default; }
.sf-flame { font-size: 22px; }
.sf-num { font-size: var(--fs-lg); font-weight: 700; color: var(--el-color-warning); flex: 1; }
.sf-freeze { font-size: var(--fs-md); color: var(--primary-color); cursor: pointer; }
</style>
