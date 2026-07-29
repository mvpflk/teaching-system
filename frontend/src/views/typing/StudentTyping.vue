<template>
  <div v-loading="loading" class="typing-page typing-theme">
    <TypingHeader
      :level="level"
      :exp-percent="expPercent"
      :exp-to-next="expToNext"
      :is-dark="isDark"
      @toggle-theme="toggleTheme"
      @go-back="router.back()"
      @go-materials="router.push('/student/typing/texts')"
    />

    <TypingModeToolbar
      :mode="practiceMode"
      :in-competition="inCompetition"
      :countdown="countdown"
      :has-started="hasStarted"
      @update:mode="practiceMode = $event"
    />

    <PracticeTextPanel
      v-if="practiceMode === 'practice' && !inCompetition && !hasStarted"
      :difficulty="selectedDifficulty"
      :language="selectedLanguage"
      :text-content="textContent"
      :text-loading="textLoading"
      :has-started="hasStarted"
      @update:difficulty="selectedDifficulty = $event"
      @update:language="selectedLanguage = $event"
      @random-pick="loadPracticeText"
    />

    <div class="typing-layout">
      <div class="typing-main">
        <TypingTextDisplay
          :char-states="charStates"
          :progress-percent="progressPercent"
          :current-index="currentIndex"
        />

        <TypingInputArea
          ref="inputAreaRef"
          v-model="typedText"
          :disabled="isFinished"
          :has-started="hasStarted"
          @reset="handleReset"
          @composition-start="isComposing = true"
          @composition-end="isComposing = false"
        />

        <TypingStatsBar
          :speed-wpm="speedWpm"
          :accuracy="accuracy"
          :accuracy-class="accuracyClass"
          :typed-length="typedText.length"
          :total-chars="totalChars"
          :elapsed-display="elapsedDisplay"
          :wrong-count="wrongCount"
        />

        <TypingErrorInline
          :display-errors="displayErrors"
          :show-all-errors="showAllErrors"
          :error-list-length="errorList.length"
          @toggle-show-all="showAllErrors = !showAllErrors"
        />
      </div>

      <CompetitionRanking
        v-if="inCompetition"
        :ranking="ranking"
        :my-student-id="myStudentId"
      />
    </div>

    <TypingResultDialog
      :visible="showResult"
      :speed-wpm="speedWpm"
      :max-segment-speed="maxSegmentSpeed"
      :accuracy="accuracy"
      :accuracy-class="accuracyClass"
      :elapsed-display="elapsedDisplay"
      :wrong-count="wrongCount"
      :backspace-count="backspaceCount"
      :last-record="lastRecord"
      :compare-speed-diff="compareSpeedDiff"
      :compare-acc-diff="compareAccDiff"
      :speed-trend="speedTrend"
      :trend-loading="trendLoading"
      :error-list="errorList"
      :in-competition="inCompetition"
      @close="showResult = false"
      @retry="practiceAgain"
      @view-history="router.push('/student/typing-history')"
    />

    <div v-if="!hasStarted && !isFinished" class="shortcut-hint">
      <span>Esc 重置</span>
      <span>退格 修正</span>
      <span>输入即开始</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTyping } from '@/composables/useTyping'
import { useTypingTheme } from '@/composables/useTypingTheme'
import { useTypingCompetition } from '@/composables/useTypingCompetition'
import { useUserStore } from '@/stores/user'
import '@/assets/typing-theme.css'
import {
  getCurrentCompetition, submitResult, saveRecord,
  getPracticeText, getStudentLevels, getStudentSpeedTrend
} from '@/api/typing'

import TypingHeader from './components/TypingHeader.vue'
import TypingModeToolbar from './components/TypingModeToolbar.vue'
import PracticeTextPanel from './components/PracticeTextPanel.vue'
import TypingTextDisplay from './components/TypingTextDisplay.vue'
import TypingInputArea from './components/TypingInputArea.vue'
import TypingStatsBar from './components/TypingStatsBar.vue'
import TypingErrorInline from './components/TypingErrorInline.vue'
import CompetitionRanking from './components/CompetitionRanking.vue'
import TypingResultDialog from './components/TypingResultDialog.vue'

const router = useRouter()
const route = useRoute()

const practiceMode = ref(route.query.mode === 'competition' ? 'competition' : 'practice')
const selectedDifficulty = ref(3)
const selectedLanguage = ref('mixed')
const textLoading = ref(false)

const { isDark, toggleTheme } = useTypingTheme()
const {
  ranking, countdown, compEndTime, competitionId, inCompetition,
  connectRankingSSE, disconnectRankingSSE,
  connectAnnounceSSE, disconnectAnnounceSSE,
  startCompPolling, startProgressReport,
  startCountdown, cleanup
} = useTypingCompetition()

const loading = ref(true)
const hasStarted = ref(false)
const textContent = ref('')
const currentTextId = ref(null)
const showResult = ref(false)
const userStore = useUserStore()
const myStudentId = computed(() => userStore.userInfo?.id)
const level = ref({ levelId: 1, exp: 0 })
const totalForLevel = computed(() => level.value.levelId * 100)
const expToNext = computed(() => Math.max(1, totalForLevel.value - level.value.exp))
const expPercent = computed(() => Math.min(100, Math.round(level.value.exp / totalForLevel.value * 100)))

const showAllErrors = ref(false)
const speedTrend = ref([])
const trendLoading = ref(false)
const lastRecord = ref(null)
const inputAreaRef = ref(null)

const elapsedDisplay = computed(() => {
  const s = elapsedSeconds.value
  const m = Math.floor(s / 60)
  const sec = s % 60
  return m > 0 ? `${m}分${sec}秒` : `${sec}秒`
})

const accuracyClass = computed(() => {
  if (accuracy.value >= 95) return 'acc-great'
  if (accuracy.value >= 80) return 'acc-good'
  return 'acc-bad'
})

const compareSpeedDiff = computed(() => {
  if (!lastRecord.value) return 0
  return Math.round(speedWpm.value - (lastRecord.value.speedWpm || 0))
})
const compareAccDiff = computed(() => {
  if (!lastRecord.value) return 0
  return parseFloat((accuracy.value - (lastRecord.value.accuracy || 0)).toFixed(1))
})

const recentErrors = computed(() => errorList.value.slice(-8))
const displayErrors = computed(() => showAllErrors.value ? errorList.value : recentErrors.value)

const {
  typedText, currentIndex, correctCount, wrongCount, backspaceCount,
  errorList, isFinished, progressPercent, elapsedSeconds, speedWpm, accuracy,
  charStates, reset, getResult, isComposing, maxSegmentSpeed, totalChars: totalCharsFromEngine
} = useTyping(textContent, { soundEnabled: true })

const totalChars = computed(() => totalCharsFromEngine.value)

watch(() => typedText.value.length, (n) => {
  if (n > 0 && !hasStarted.value) hasStarted.value = true
})

watch(practiceMode, () => {
  if (!hasStarted.value || isFinished.value) loadText()
})

watch(isFinished, async (val) => {
  if (!val) return
  showResult.value = true
  const result = getResult()
  try {
    const stored = localStorage.getItem('typing-last-practice')
    if (stored) lastRecord.value = JSON.parse(stored)
  } catch {}
  if (inCompetition.value && competitionId.value) {
    try {
      await submitResult(competitionId.value, result)
      ElMessage.success('成绩已提交')
    } catch { ElMessage.error('提交失败') }
  } else {
    try {
      await saveRecord({ textId: currentTextId.value, mode: 'practice', ...result })
      localStorage.setItem('typing-last-practice', JSON.stringify({
        speedWpm: result.speedWpm,
        accuracy: result.accuracy,
        date: new Date().toLocaleDateString()
      }))
    } catch {}
  }
  try {
    const trendRes = await getStudentSpeedTrend(20)
    if (trendRes.code === 200) {
      speedTrend.value = trendRes.data || []
    }
  } catch {}
})

function handleReset() {
  reset()
  hasStarted.value = false
  inputAreaRef.value?.inputRef?.focus()
}

function practiceAgain() {
  showResult.value = false
  loadText()
}

async function loadPracticeText() {
  textLoading.value = true
  try {
    const res = await getPracticeText(undefined, selectedDifficulty.value, selectedLanguage.value)
    textContent.value = (res.data?.content || '光阴似箭，日月如梭。少壮不努力，老大徒伤悲。书山有路勤为径，学海无涯苦作舟。').replace(/\n+/g, ' ')
    currentTextId.value = res.data?.id || null
    reset()
    hasStarted.value = false
    showResult.value = false
    await nextTick()
    inputAreaRef.value?.inputRef?.focus()
  } catch {
    textContent.value = '光阴似箭，日月如梭。少壮不努力，老大徒伤悲。书山有路勤为径，学海无涯苦作舟。'
  }
  textLoading.value = false
}

async function loadText() {
  reset()
  hasStarted.value = false
  showResult.value = false
  if (practiceMode.value === 'practice') {
    inCompetition.value = false
    competitionId.value = null
    try {
      const textId = route.query.textId ? Number(route.query.textId) : undefined
      const t = await getPracticeText(textId, selectedDifficulty.value, selectedLanguage.value)
      textContent.value = (t.data?.content || '光阴似箭，日月如梭。少壮不努力，老大徒伤悲。书山有路勤为径，学海无涯苦作舟。').replace(/\n+/g, ' ')
      currentTextId.value = t.data?.id || null
    } catch {
      textContent.value = '光阴似箭，日月如梭。少壮不努力，老大徒伤悲。书山有路勤为径，学海无涯苦作舟。'
      currentTextId.value = null
    }
    await nextTick()
    inputAreaRef.value?.inputRef?.focus()
    return
  }
  try {
    const res = await getCurrentCompetition()
    if (res.code === 200 && res.data) {
      inCompetition.value = true
      competitionId.value = res.data.id
      compEndTime.value = res.data.endTime
      textContent.value = (res.data.textContent || '').replace(/\n+/g, ' ')
      currentTextId.value = res.data.textId || null
      startProgressReport(correctCount, textContent, speedWpm, accuracy, backspaceCount, progressPercent, isFinished)
      await connectRankingSSE()
    } else {
      inCompetition.value = false
      competitionId.value = null
      const textId = route.query.textId ? Number(route.query.textId) : undefined
      const t = await getPracticeText(textId, selectedDifficulty.value, selectedLanguage.value)
      textContent.value = (t.data?.content || '光阴似箭，日月如梭。少壮不努力，老大徒伤悲。书山有路勤为径，学海无涯苦作舟。').replace(/\n+/g, ' ')
      currentTextId.value = t.data?.id || null
    }
  } catch {
    textContent.value = '光阴似箭，日月如梭。少壮不努力，老大徒伤悲。书山有路勤为径，学海无涯苦作舟。'
    currentTextId.value = null
  }
  await nextTick()
  inputAreaRef.value?.inputRef?.focus()
}

onMounted(async () => {
  try {
    const lv = await getStudentLevels()
    if (lv.code === 200) level.value = lv.data
  } catch {}
  connectAnnounceSSE()
  startCompPolling(() => {
    practiceMode.value = 'competition'
    loadText()
  })
  await loadText()
  loading.value = false
  startCountdown()
})

onUnmounted(() => {
  cleanup()
  document.documentElement.removeAttribute('data-theme')
})
</script>

<style scoped>
.typing-page { margin: 0 auto; padding: 16px; display: flex; flex-direction: column; gap: 10px; min-height: calc(100vh - 64px); }
.typing-layout { display: flex; gap: 12px; flex: 1; min-height: 0; }
.typing-main { flex: 1; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.shortcut-hint { position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); display: flex; gap: 16px; padding: 6px 16px; background: var(--typing-surface); border: 1px solid var(--typing-border); border-radius: 20px; font-size: var(--fs-xs); color: var(--typing-pending); z-index: 10; }
@media (max-width: 768px) {
  .typing-page { padding: 8px; gap: 8px; }
  .typing-layout { flex-direction: column; }
  .shortcut-hint { bottom: 8px; font-size: 10px; gap: 10px; padding: 4px 12px; }
}
</style>
