<template>
  <div class="exam-page">
    <div v-if="loading" class="status-msg">加载考试...</div>
    <div v-else-if="error" class="status-msg">{{ error }} <button @click="loadExam">重试</button></div>
    <template v-else>
      <Win7Simulation mode="exam" />
      <div v-if="!store.examSubmitted" class="exam-submit-bar">
        <span class="exam-timer" :class="{ 'exam-timer--danger': remaining <= 30 }">
          ⏱ {{ formattedRemaining }}
        </span>
        <el-button type="danger" size="small" @click="handleSubmit">交卷</el-button>
      </div>
      <div v-else class="exam-done">
        <h2>考试已提交</h2>
        <p class="exam-score">自动评分：{{ examResult.autoScore }} 分</p>
        <p :class="examResult.success ? 'exam-pass' : 'exam-fail'">
          {{ examResult.success ? '✅ 通过' : '❌ 未通过' }}
        </p>
        <p class="exam-duration">用时：{{ formattedElapsed }}</p>
        <el-button type="primary" @click="$router.back()">返回</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useWin7SimStore } from '@/stores/win7Sim'
import { getSimTaskDefinition, startSimExam, reportSimProgress } from '@/api/simulation'
import Win7Simulation from '@/components/win7-sim/Win7Simulation.vue'

const route = useRoute()
const router = useRouter()
const store = useWin7SimStore()
const loading = ref(true)
const error = ref(null)
const examResult = ref({ autoScore: 0, success: false })
const elapsed = ref(0)
const remaining = ref(0)
const timeLimit = ref(120)
let timer = null
let batchTimer = null

const formattedRemaining = computed(() => {
  const m = Math.floor(remaining.value / 60), s = remaining.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const formattedElapsed = computed(() => {
  const m = Math.floor(elapsed.value / 60), s = elapsed.value % 60
  return `${m}分${String(s).padStart(2, '0')}秒`
})

async function loadExam() {
  loading.value = true; error.value = null
  try {
    const id = route.params.id
    const [defRes, examRes] = await Promise.all([
      getSimTaskDefinition(id),
      startSimExam(id)
    ])
    if (defRes.code !== 200) { error.value = defRes.message || '加载失败'; return }
    if (examRes.code !== 200) { error.value = examRes.message || '无法开始考试'; return }

    store.resetAll()
    if (defRes.data.initialVfs) Object.assign(store.fileSystem, defRes.data.initialVfs)
    const submissionId = examRes.data.submissionId
    store.startExam(defRes.data.taskJson, submissionId)

    timeLimit.value = defRes.data.taskJson?.timeLimit || 120
    remaining.value = timeLimit.value
    timer = setInterval(() => {
      elapsed.value++
      remaining.value = Math.max(0, remaining.value - 1)
      if (remaining.value <= 0) handleAutoSubmit()
    }, 1000)
    batchTimer = setInterval(batchUpload, 30000)
  } catch (e) { error.value = '网络错误: ' + (e.message || e) }
  finally { loading.value = false }
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确定要交卷吗？提交后不可修改。', '交卷确认', {
      confirmButtonText: '交卷', cancelButtonText: '再想想', type: 'warning'
    })
  } catch { return }
  doSubmit()
}

async function handleAutoSubmit() {
  clearInterval(timer)
  try {
    await ElMessageBox.alert('考试时间已到，系统已自动交卷。', '时间到', { type: 'info', showCancelButton: false })
  } catch {}
  doSubmit()
}

async function doSubmit() {
  clearInterval(timer); clearInterval(batchTimer)
  const result = store.submitExam()
  examResult.value = { ...result, durationSeconds: elapsed.value }
  try {
    await reportSimProgress({
      submissionId: store.getSubmissionId(),
      events: result.events,
      eventCount: result.eventCount,
      autoScore: result.autoScore,
      success: result.success,
      durationSeconds: elapsed.value
    })
  } catch (e) { console.error('上报失败', e) }
}

async function batchUpload() {
  if (store.examSubmitted) return
  const events = store.getIncrementalEvents()
  if (!events.length) return
  try {
    await reportSimProgress({
      submissionId: store.getSubmissionId(),
      events,
      eventCount: events.length,
      durationSeconds: elapsed.value
    })
  } catch { /* 静默失败 */ }
}

onMounted(loadExam)
onBeforeUnmount(() => { clearInterval(timer); clearInterval(batchTimer) })
</script>

<style scoped>
.exam-page { width: 100%; height: 100vh; overflow: hidden; position: relative; }
.status-msg { display: flex; align-items: center; justify-content: center; height: 100%; color: #fff; font-size: var(--fs-lg); flex-direction: column; gap: 12px; background: #0078d4; }
.status-msg button { padding: 8px 20px; border: 1px solid #fff; border-radius: 4px; background: rgba(255,255,255,0.1); color: #fff; cursor: pointer; }
.exam-submit-bar { position: absolute; top: 8px; right: 8px; display: flex; align-items: center; gap: 12px; background: rgba(0,0,0,0.75); padding: 8px 16px; border-radius: 6px; z-index: 500; }
.exam-timer { color: #fff; font-size: var(--fs-md); font-weight: 600; }
.exam-timer--danger { color: #e81123; animation: timer-pulse 1s ease-in-out infinite; }
@keyframes timer-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.exam-done { display: flex; align-items: center; justify-content: center; height: 100%; flex-direction: column; gap: 8px; background: #0078d4; color: #fff; }
.exam-done h2 { margin: 0; }
.exam-score { font-size: var(--fs-xl); font-weight: 700; }
.exam-pass { color: #4caf50; font-size: var(--fs-lg); }
.exam-fail { color: #f44336; font-size: var(--fs-lg); }
.exam-duration { font-size: var(--fs-sm); opacity: 0.8; }
</style>
