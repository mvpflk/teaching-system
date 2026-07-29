<template>
  <div class="replay-page typing-theme">
    <div v-if="!loading && !keystrokeData.length" class="empty-full">
      <el-empty description="该学生无击键数据可供回放" :image-size="80" />
    </div>
    <template v-else-if="!loading">
      <div class="page-header">
        <el-button text @click="router.back()">← 返回</el-button>
        <h3>竞赛回放 — {{ studentName }}</h3>
      </div>
      <div class="replay-stats">
        <span>速度: <strong>{{ speedWpm }} 字/分</strong></span>
        <span>正确率: <strong>{{ accuracy }}%</strong></span>
        <span>用时: <strong>{{ durationSeconds }}s</strong></span>
      </div>
      <div class="text-panel">
        <div class="text-display">
          <span v-for="(cs, i) in displayStates" :key="i" :class="'char-' + cs.state">{{ cs.char === ' ' ? ' ' : cs.char }}</span>
        </div>
      </div>
      <div class="replay-controls">
        <el-button-group>
          <el-button :disabled="player.isPlaying.value" @click="player.play()">▶ 播放</el-button>
          <el-button :disabled="!player.isPlaying.value" @click="player.pause()">⏸ 暂停</el-button>
        </el-button-group>
        <el-radio-group v-model="speed" size="small" @change="player.setSpeed">
          <el-radio-button :value="1">1x</el-radio-button>
          <el-radio-button :value="2">2x</el-radio-button>
          <el-radio-button :value="4">4x</el-radio-button>
        </el-radio-group>
        <div class="timeline">
          <span>{{ formatMs(player.currentTime.value) }}</span>
          <el-slider
            v-model="sliderTime"
            :max="player.totalDuration.value"
            :show-tooltip="false"
            style="flex:1"
            @input="player.seek"
          />
          <span>{{ formatMs(player.totalDuration.value) }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useReplayPlayer } from '@/composables/useReplayPlayer'
import { getCompetitionReplay } from '@/api/typing'
import '@/assets/typing-theme.css'

const route = useRoute()
const router = useRouter()
const compId = route.params.compId
const studentId = route.params.studentId

const loading = ref(true)
const studentName = ref('')
const textContent = ref('')
const speedWpm = ref(0)
const accuracy = ref(0)
const durationSeconds = ref(0)
const keystrokeData = ref([])
const speed = ref(1)
const sliderTime = ref(0)

const player = useReplayPlayer(keystrokeData, textContent)

watch(() => player.currentTime.value, (t) => { sliderTime.value = t })

function formatMs(ms) {
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  return `${m}:${String(s % 60).padStart(2, '0')}`
}

// 键盘快捷键：空格播放/暂停
function onKeydown(e) {
  if (e.code === 'Space' && !e.target.closest('input,textarea')) {
    e.preventDefault()
    if (player.isPlaying.value) player.pause()
    else player.play()
  }
}

onMounted(async () => {
  try {
    const res = await getCompetitionReplay(compId, studentId)
    if (res.code === 200) {
      const d = res.data
      studentName.value = d.studentName || ''
      textContent.value = (d.textContent || '').replace(/\n+/g, ' ')
      speedWpm.value = d.speedWpm || 0
      accuracy.value = d.accuracy || 0
      durationSeconds.value = d.durationSeconds || 0
      keystrokeData.value = d.keystrokeData || []
    }
  } catch { ElMessage.error('加载回放数据失败') }
  finally { loading.value = false }
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  player.pause()
})
</script>

<style scoped>
.replay-page { max-width: 900px; margin: 0 auto; padding: 16px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; align-items: center; gap: 12px; }
.page-header h3 { margin: 0; }
.replay-stats { display: flex; gap: 24px; font-size: var(--fs-md); color: var(--typing-pending); }
.replay-stats strong { color: var(--typing-text); }
.text-panel { background: var(--typing-surface); border: 1px solid var(--typing-border); border-radius: var(--radius-md); padding: 20px; max-height: 320px; overflow-y: auto; }
.text-display { font-family: 'JetBrains Mono','Fira Code','Consolas',monospace; font-size: var(--fs-xl); line-height: 1.9; letter-spacing: 0.5px; white-space: pre-wrap; }
.replay-controls { display: flex; flex-direction: column; gap: 12px; align-items: center; padding: 12px 0; }
.timeline { display: flex; align-items: center; gap: 10px; width: 100%; max-width: 600px; font-size: var(--fs-xs); color: var(--typing-pending); }
.empty-full { padding: 60px 0; }

@media (max-width: 768px) {
  .replay-page { padding: 8px; }
  .text-display { font-size: 17px; }
  .replay-stats { gap: 12px; font-size: var(--fs-xs); }
}
</style>
