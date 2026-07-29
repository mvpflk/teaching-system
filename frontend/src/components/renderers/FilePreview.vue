<template>
  <div class="fp-wrap">
    <div v-if="loading" class="fp-loading"><el-icon class="fp-spin"><Loading /></el-icon> 加载中...</div>

    <!-- 图片预览 -->
    <div v-if="type === 'image'" ref="imgWrap" class="fp-image">
      <img
        ref="imgEl"
        :src="src"
        :style="{ transform: `scale(${scale}) translate(${tx}px,${ty}px)` }"
        @load="onLoad"
        @error="onError"
      />
    </div>

    <!-- PDF -->
    <iframe
      v-else-if="type === 'pdf'"
      :src="src"
      class="fp-iframe"
      @load="onLoad"
      @error="onError"
    />

    <!-- 音频 -->
    <div v-else-if="type === 'audio'" class="fp-audio">
      <div class="fp-audio-icon">🎵</div>
      <div class="fp-audio-name">{{ displayName }}</div>
      <audio
        :src="src"
        controls
        preload="metadata"
        class="fp-audio-player"
        @loadedmetadata="onLoad"
        @error="onError"
      >
        您的浏览器不支持音频播放
      </audio>
    </div>

    <!-- 视频 -->
    <div v-else-if="type === 'video'" class="fp-video">
      <video
        :src="src"
        controls
        preload="metadata"
        class="fp-video-player"
        @loadedmetadata="onLoad"
        @error="onError"
      >
        您的浏览器不支持视频播放
      </video>
    </div>

    <!-- 降级下载 -->
    <div v-else class="fp-fallback">
      <div class="fp-icon">{{ type === 'cad' ? '📐' : '📄' }}</div>
      <p>{{ fallbackText }}</p>
      <el-button type="primary" size="small" @click="download">下载文件</el-button>
    </div>

    <!-- 工具栏 -->
    <div v-if="type === 'image'" class="fp-tools">
      <el-button-group size="small">
        <el-button @click="zoomIn">+</el-button>
        <el-button @click="zoomOut">-</el-button>
        <el-button @click="zoomReset">1:1</el-button>
      </el-button-group>
      <slot name="tools" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  src: { type: String, required: true },
  filename: { type: String, default: '' },
})
const emit = defineEmits(['loaded', 'error'])

const loading = ref(true), scale = ref(1), tx = ref(0), ty = ref(0), imgEl = ref(null)

const ext = computed(() => (props.filename || props.src).split('.').pop()?.toLowerCase())
const displayName = computed(() => props.filename || props.src.split('/').pop() || '音频文件')

const type = computed(() => {
  const e = ext.value
  if (['jpg','jpeg','png','gif','bmp','webp','svg'].includes(e)) return 'image'
  if (e === 'pdf') return 'pdf'
  if (['mp3','wav','ogg','m4a','aac','flac','wma'].includes(e)) return 'audio'
  if (['mp4','webm','mov','avi','mkv','flv','wmv'].includes(e)) return 'video'
  if (['dwg','dxf','step','stp'].includes(e)) return 'cad'
  return 'unknown'
})

const fallbackText = computed(() => {
  if (type.value === 'cad') return 'CAD 文件 — 预览功能开发中'
  if (['doc','docx','xls','xlsx','ppt','pptx'].includes(ext.value)) return 'Office 文档 — 请下载后查看'
  return '不支持在线预览，请下载后查看'
})

const onLoad = () => { loading.value = false; emit('loaded') }
const onError = () => { loading.value = false; emit('error') }
const zoomIn = () => { scale.value = Math.min(5, scale.value + 0.25) }
const zoomOut = () => { scale.value = Math.max(0.25, scale.value - 0.25) }
const zoomReset = () => { scale.value = 1; tx.value = 0; ty.value = 0 }
const download = () => { const a = document.createElement('a'); a.href = props.src; a.download = props.filename || 'file'; a.click() }

let lastDist = 0
const onTouch = (e) => {
  if (e.touches.length === 2) {
    const d = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY)
    if (lastDist) { scale.value = Math.max(0.25, Math.min(5, scale.value * (d / lastDist))) }
    lastDist = d
  }
}
onMounted(() => { if (type.value === 'image') window.addEventListener('touchmove', onTouch, { passive: true }) })
onUnmounted(() => { window.removeEventListener('touchmove', onTouch) })
</script>

<style scoped>
.fp-wrap { position: relative; max-width: 100%; overflow: hidden; background: var(--bg-section); border-radius: var(--radius-md); min-height: 60px; }
.fp-loading { display: flex; align-items: center; justify-content: center; padding: 40px; color: var(--text-secondary); font-size: var(--fs-sm); }
.fp-spin { animation: spin 1s linear infinite; margin-right: 8px; }
@keyframes spin { to { transform: rotate(360deg); } }
.fp-image { overflow: hidden; touch-action: none; display: flex; align-items: center; justify-content: center; min-height: 200px; }
.fp-image img { max-width: 100%; transition: transform 0.15s; cursor: grab; }
.fp-iframe { width: 100%; height: 500px; border: none; }
.fp-audio { padding: 20px; text-align: center; }
.fp-audio-icon { font-size: 36px; margin-bottom: 8px; }
.fp-audio-name { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 12px; word-break: break-all; }
.fp-audio-player { width: 100%; max-width: 400px; }
.fp-video { padding: 0; }
.fp-video-player { width: 100%; max-height: 400px; display: block; border-radius: var(--radius-md); background: #000; }
.fp-fallback { text-align: center; padding: 30px 20px; }
.fp-icon { font-size: 40px; margin-bottom: 8px; }
.fp-fallback p { color: var(--text-secondary); font-size: var(--fs-sm); margin-bottom: 12px; }
.fp-tools { display: flex; justify-content: center; padding: 8px; gap: 8px; background: var(--bg-card); border-top: 1px solid var(--border-light); }

@media (max-width: 768px) {
  .fp-iframe { height: 350px; }
  .fp-video-player { max-height: 280px; }
  .fp-audio { padding: 16px; }
  .fp-tools { flex-wrap: wrap; }
}
</style>
