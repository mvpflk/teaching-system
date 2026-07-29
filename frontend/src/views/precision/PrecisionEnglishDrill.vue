<template>
  <div class="ped-page">
    <div class="ped-header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <span class="ped-progress-text">{{ currentIdx + 1 }} / {{ questions.length }}</span>
    </div>

    <div class="ped-progress-bar"><div class="ped-fill" :style="{width: pct + '%'}"></div></div>

    <!-- 🦉 小P学伴 -->
    <OwlBuddy v-if="owlMessage" :message="owlMessage" :mood="owlMood" />

    <!-- 题目区 -->
    <div v-if="currentQ" class="ped-card">
      <VocabDrillItem
        v-if="currentQ.type !== 'grammar'"
        v-model="currentAnswer"
        :question="currentQ"
        :direction="currentQ.direction"
        :hint="hint"
        :hint-level="hintsUsed"
        :feedback="feedback"
        @submit="submitAnswer"
      />
      <GrammarDrillItem
        v-else
        v-model="currentAnswer"
        :question="currentQ"
        :hint="hint"
        :hint-level="hintsUsed"
        :feedback="feedback"
        @submit="submitAnswer"
      />

      <!-- 按钮区 -->
      <div class="ped-btns">
        <el-button
          v-if="!feedback"
          type="primary"
          :disabled="!currentAnswer.trim()"
          :loading="submitting"
          @click="submitAnswer"
        >
          提交
        </el-button>
        <el-button
          v-if="!feedback && !showAnswer"
          text
          size="small"
          type="info"
          @click="confirmShowAnswer"
        >
          还是不会？看看答案
        </el-button>
        <el-button
          v-if="feedback"
          type="primary"
          :loading="submitting"
          @click="nextQuestion"
        >
          {{ currentIdx < questions.length - 1 ? '下一题' : '完成' }}
        </el-button>
      </div>
    </div>

    <!-- 完成总结弹窗 -->
    <el-dialog
      v-model="showComplete"
      title="🎉 练习完成"
      width="360px"
      :close-on-click-modal="false"
    >
      <TreasureBox v-if="showBox" :result="completeResult" :visible="showBox" />
      <DrillCompleteSummary
        v-else
        :result="completeResult"
        @again="restartDrill"
        @back="goBack"
      />
      <template #footer>
        <el-button @click="restartDrill">再来一组</el-button>
        <el-button type="primary" @click="goBack">返回仪表盘</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { submitDrillAnswer, completeDrill } from '@/api/precisionEnglish'
import VocabDrillItem from '@/components/precision/VocabDrillItem.vue'
import GrammarDrillItem from '@/components/precision/GrammarDrillItem.vue'
import OwlBuddy from '@/components/precision/OwlBuddy.vue'
import TreasureBox from '@/components/precision/TreasureBox.vue'
import DrillCompleteSummary from '@/components/precision/DrillCompleteSummary.vue'

const router = useRouter()
const questions = ref([])
const currentIdx = ref(0)
const currentAnswer = ref('')
const hint = ref('')
const feedback = ref(null)
const showAnswer = ref(false)
const hintsUsed = ref(0)
const results = ref([])
const showComplete = ref(false)
const showBox = ref(true)
const completeResult = ref({})
const owlMessage = ref('')
const owlMood = ref('normal')
const submitting = ref(false)  // R112修复：防重复提交
const drillStartTime = ref(Date.now())  // P0修复：记录开始时间用于准确计时
let boxTimeoutId = null

const currentQ = computed(() => questions.value[currentIdx.value])
const pct = computed(() => questions.value.length ? Math.round(((currentIdx.value + 1) / questions.value.length) * 100) : 0)

onMounted(() => {
  try {
    const raw = sessionStorage.getItem('english_daily_task')
    if (raw) {
      const task = JSON.parse(raw)
      if (task._ts && Date.now() - task._ts > 30 * 60 * 1000) {
        sessionStorage.removeItem('english_daily_task')
      } else {
        const all = [...(task.vocabQuestions || []).map(q => ({...q, type: 'vocab', direction: q.direction || 'en2cn'})),
          ...(task.grammarQuestions || []).map(q => ({...q, type: 'grammar'}))]
        questions.value = all
      }
    }
  } catch (e) {
    console.error('加载每日任务失败:', e)
  }
  owlMessage.value = '🦉 开始今天的练习吧！每题答错都有提示引导你找到正确答案。'
  owlMood.value = 'normal'
})

onBeforeUnmount(() => {
  if (boxTimeoutId) { clearTimeout(boxTimeoutId); boxTimeoutId = null }
})

const submitAnswer = async () => {
  if (!currentQ.value || !currentAnswer.value.trim() || submitting.value) return
  submitting.value = true  // R112修复：防重复提交
  owlMessage.value = ''
  try {
    const res = await submitDrillAnswer({
      questionId: currentQ.value.questionId || -(currentIdx.value + 1),
      answer: currentAnswer.value.trim(),
      hintLevel: hintsUsed.value,
      questionType: currentQ.value.type === 'grammar' ? 'SINGLE_CHOICE' : 'FILL_IN',
      subject: '英语[职高]',
      word: currentQ.value.word || null,
      direction: currentQ.value.direction || null
    })
    const d = res.data || {}
    feedback.value = d
    if (d.correct) {
      results.value.push({ ...currentQ.value, correct: true, hintLevel: hintsUsed.value })
      owlMessage.value = d.message || '回答正确！'
      owlMood.value = 'correct'
      hintsUsed.value = 0
    } else if (d.hintLevel < 3 && !showAnswer.value) {
      hintsUsed.value = d.hintLevel
      hint.value = d.hint || ''
      owlMessage.value = d.message || '试试看这个提示？'
      owlMood.value = 'hint'
      currentAnswer.value = ''
    } else {
      results.value.push({ ...currentQ.value, correct: false, hintLevel: hintsUsed.value })
      owlMessage.value = d.message || '看看正确答案，明天再来！'
      owlMood.value = 'reveal'
      hintsUsed.value = 0
    }
  } catch { owlMessage.value = '🦉 网络出了点问题，再试一次？'; owlMood.value = 'normal'; ElMessage.error('提交失败') }
  finally { submitting.value = false }  // R112修复
}

const confirmShowAnswer = async () => {
  try {
    await ElMessageBox.confirm('确定要直接看答案吗？建议再试试看哦！', '确认', {
      confirmButtonText: '看答案', cancelButtonText: '再想想', type: 'warning'
    })
    showAnswer.value = true
    hintsUsed.value = 3
    hint.value = ''  // P1-5: 清除旧提示
    if (!currentAnswer.value.trim()) currentAnswer.value = '(查看答案)'
    await submitAnswer()
  } catch (e) {
    console.error('显示答案失败:', e)
  }
}

const nextQuestion = async () => {
  if (currentIdx.value < questions.value.length - 1) {
    currentIdx.value++; currentAnswer.value = ''; hint.value = ''; feedback.value = null; showAnswer.value = false
    hintsUsed.value = 0  // P0-3: 跨题重置
    owlMessage.value = ''; owlMood.value = 'normal'
  } else {
    try {
      const todayKey = new Date().toISOString().slice(0, 10)
      const groupSeq = parseInt(sessionStorage.getItem('english_drill_group_' + todayKey) || '0') + 1  // P2-3
      sessionStorage.setItem('english_drill_group_' + todayKey, String(groupSeq))
      const elapsedSeconds = Math.max(1, Math.round((Date.now() - drillStartTime.value) / 1000))
      const res = await completeDrill({
        answers: results.value.map((r, i) => ({ questionId: r.questionId || -(i + 1), correct: r.correct, hintLevel: r.hintLevel })),
        groupSeq,
        elapsedSeconds
      })
      completeResult.value = res.data || {}
      showBox.value = true
      showComplete.value = true
      // P0-1: 2秒后从宝箱切换到总结
      boxTimeoutId = setTimeout(() => { showBox.value = false; boxTimeoutId = null }, 2500)
    } catch { ElMessage.error('提交失败') }
  }
}

const restartDrill = () => {
  if (boxTimeoutId) { clearTimeout(boxTimeoutId); boxTimeoutId = null }  // R112修复：清理残留timeout
  showComplete.value = false; showBox.value = true; currentIdx.value = 0; currentAnswer.value = ''
  hint.value = ''; feedback.value = null; showAnswer.value = false
  results.value = []; hintsUsed.value = 0; owlMessage.value = ''; owlMood.value = 'normal'; submitting.value = false
  drillStartTime.value = Date.now()  // P0修复：重置计时
}

const goBack = () => router.push('/precision/english')
</script>

<style scoped>
.ped-page { max-width: 700px; margin: 0 auto; padding: 16px; min-height: 100vh; background: var(--bg-page, var(--bg-page)); }
.ped-header { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.ped-progress-text { font-size: var(--fs-md); color: var(--text-secondary, var(--text-secondary)); }
.ped-progress-bar { height: 4px; background: var(--bg-secondary, var(--bg-secondary)); border-radius: 2px; margin-bottom: 16px; }
.ped-fill { height: 100%; background: var(--primary-color); border-radius: 2px; transition: width .3s; }
.ped-card { padding: 0; margin-bottom: 14px; }
.ped-btns { display: flex; gap: 10px; margin-top: 14px; align-items: center; padding: 0 20px 16px; }
</style>
