<template>
  <!-- idle 状态直接输出裸按钮，无任何包裹，确保与旁边 el-upload 完全对齐 -->
  <el-button
    v-if="status === 'idle'"
    size="small"
    plain
    :disabled="!supported"
    @click="start"
  >
    <el-icon><Microphone /></el-icon> 开始录音
  </el-button>
  <span v-if="status === 'idle' && !supported" class="vr-hint">当前浏览器不支持录音</span>

  <span v-else-if="status === 'recording'" class="vr-recording">
    <span class="vr-dot"></span>
    <span class="vr-timer">{{ formattedTime }}</span>
    <el-button size="small" type="danger" @click="stop">停止</el-button>
  </span>

  <span v-else-if="status === 'uploading'" class="vr-uploading">
    <el-icon class="vr-spin"><Loading /></el-icon> 上传中...
  </span>

  <span v-else-if="status === 'done'" class="vr-done">
    <el-icon color="var(--el-color-success)"><CircleCheckFilled /></el-icon>
    <span>语音已上传</span>
    <el-button
      size="small"
      text
      type="primary"
      @click="reset"
    >重新录制</el-button>
  </span>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Microphone, Loading, CircleCheckFilled } from '@element-plus/icons-vue'
import { UPLOAD_ACTION, getUploadHeaders } from '@/api/task'

const emit = defineEmits(['done'])

const supported = ref(false)
const status = ref('idle') // idle | recording | uploading | done
const seconds = ref(0)
const blob = ref(null)
let mediaRecorder = null
let timer = null
let chunks = []

const formattedTime = computed(() => {
  const m = Math.floor(seconds.value / 60)
  const s = String(seconds.value % 60).padStart(2, '0')
  return `${m}:${s}`
})

onMounted(() => {
  supported.value = !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && window.MediaRecorder)
})

onUnmounted(() => {
  stopTimer()
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
})

function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
}

async function start() {
  try {
    chunks = []
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    // 优先 webm，回退到 mp4
    let mimeType = ''
    for (const m of ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']) {
      if (MediaRecorder.isTypeSupported(m)) { mimeType = m; break }
    }
    mediaRecorder = new MediaRecorder(stream, mimeType ? { mimeType } : {})
    mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) chunks.push(e.data) }
    mediaRecorder.onstop = () => {
      stream.getTracks().forEach(t => t.stop())
      const ext = mimeType.includes('mp4') ? '.m4a' : '.webm'
      blob.value = new Blob(chunks, { type: mimeType || 'audio/webm' })
      upload(blob.value, ext)
    }
    mediaRecorder.start()
    status.value = 'recording'
    seconds.value = 0
    timer = setInterval(() => { seconds.value++ }, 1000)
  } catch (e) {
    ElMessage.error('无法访问麦克风，请在浏览器设置中允许录音权限')
  }
}

function stop() {
  stopTimer()
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
}

async function upload(blobData, ext) {
  status.value = 'uploading'
  try {
    const formData = new FormData()
    const filename = `voice_${Date.now()}${ext}`
    formData.append('files', new File([blobData], filename, { type: blobData.type }))

    const headers = getUploadHeaders()
    // NOTE: axios won't auto-set Content-Type for multipart/form-data; let the browser set it
    const res = await fetch(UPLOAD_ACTION, {
      method: 'POST',
      headers: { Authorization: headers.Authorization },
      body: formData
    })
    const json = await res.json()
    if (json.code === 200) {
      const url = Array.isArray(json.data) ? json.data[0] : json.data
      emit('done', url)
      status.value = 'done'
    } else {
      ElMessage.error(json.message || '上传失败')
      status.value = 'idle'
    }
  } catch {
    ElMessage.error('网络错误，上传失败')
    status.value = 'idle'
  }
}

function reset() {
  status.value = 'idle'
  seconds.value = 0
  blob.value = null
  chunks = []
}

defineExpose({ reset })
</script>

<style scoped>
.vr-recording { display: inline-flex; align-items: center; gap: 8px; }
.vr-uploading, .vr-done { display: inline-flex; align-items: center; gap: 6px; font-size: var(--fs-sm); color: var(--text-secondary); }
.vr-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--el-color-danger); animation: vr-pulse 1s infinite; }
@keyframes vr-pulse { 0%,100% { opacity:1; } 50% { opacity:0.3; } }
.vr-timer { font-variant-numeric: tabular-nums; font-size: var(--fs-md); font-weight: 600; color: var(--el-color-danger); min-width: 48px; }
.vr-spin { animation: vr-rotate 1s linear infinite; }
@keyframes vr-rotate { from { transform:rotate(0deg); } to { transform:rotate(360deg); } }
.vr-hint { font-size: var(--fs-xs); color: var(--text-secondary); }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
