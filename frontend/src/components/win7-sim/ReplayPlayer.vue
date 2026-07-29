<template>
  <div class="replay-player">
    <div class="rp-controls">
      <button @click="speed = Math.max(1, speed / 2)">⏪</button>
      <button @click="playing = !playing">{{ playing ? '⏸' : '▶' }}</button>
      <button @click="speed = Math.min(8, speed * 2)">⏩</button>
      <span class="rp-time">{{ currentTs }}s / {{ totalTs }}s</span>
      <span class="rp-speed">×{{ speed }}</span>
      <input
        v-model="seekTs"
        type="range"
        :min="0"
        :max="totalTs"
        class="rp-seek"
        @input="seekTo"
      />
    </div>
    <div class="rp-timeline">
      <div
        v-for="(evt, i) in events"
        :key="i"
        class="rp-event"
        :class="{ current: evt.ts <= currentTs, suspect: isSuspect(evt, i) }"
        @click="jumpTo(evt.ts)"
      >
        <span class="rp-seq">{{ evt.seq }}</span>
        <span class="rp-type">{{ evt.type }}</span>
        <span class="rp-target">{{ truncate(evt.target || '', 25) }}</span>
        <span class="rp-ts">{{ evt.ts }}s</span>
        <button class="rp-note-btn" title="添加评注" @click.stop="addNote(evt)">💬</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
const props = defineProps({ events: { type: Array, default: () => [] }, duration: { type: Number, default: 0 } })
const emit = defineEmits(['addNote'])

const playing = ref(false), speed = ref(1), currentTs = ref(0), seekTs = ref(0)
const totalTs = computed(() => Math.max(props.duration, ...props.events.map(e => e.ts || 0), 1))
let timer = null

watch(playing, (v) => {
  clearInterval(timer)
  if (v) timer = setInterval(() => { currentTs.value = Math.min(totalTs.value, currentTs.value + speed.value); seekTs.value = currentTs.value; if (currentTs.value >= totalTs.value) playing.value = false }, 1000)
})
function jumpTo(ts) { currentTs.value = ts; seekTs.value = ts }
function seekTo() { currentTs.value = seekTs.value }
function isSuspect(evt, i) {
  if (i === 0) return false
  const prev = props.events[i - 1]
  return evt.ts - prev.ts > 60000 // 停顿 > 1 分钟
}
function addNote(evt) { emit('addNote', evt) }
function truncate(s, n) { return s && s.length > n ? s.slice(0, n) + '…' : s }
onBeforeUnmount(() => clearInterval(timer))
</script>
<style scoped>
.replay-player { display: flex; flex-direction: column; height: 100%; background: #f8fafc; font-size: var(--fs-xs); }
.rp-controls { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: #fff; border-bottom: 1px solid #e0e0e0; }
.rp-controls button { width: 32px; height: 32px; border: 1px solid #ccc; border-radius: 4px; background: #fff; cursor: pointer; font-size: var(--fs-md); }
.rp-time { font-weight: 600; color: var(--primary-color); }
.rp-speed { color: #999; font-size: var(--fs-xs); }
.rp-seek { flex: 1; }
.rp-timeline { flex: 1; overflow-y: auto; padding: 6px; }
.rp-event { display: flex; gap: 8px; align-items: center; padding: 4px 8px; border-radius: 3px; font-size: var(--fs-xs); border-bottom: 1px solid #f0f0f0; }
.rp-event.current { background: #e8f0fe; }
.rp-event.suspect { background: #fff3cd; }
.rp-event:hover { background: #f0f4f8; }
.rp-seq { width: 28px; color: #999; }
.rp-type { width: 80px; font-weight: 500; }
.rp-target { flex: 1; color: #666; }
.rp-ts { color: #999; font-size: 10px; }
.rp-note-btn { background: none; border: none; cursor: pointer; font-size: var(--fs-xs); }
</style>
