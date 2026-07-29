<template>
  <div class="practice-page">
    <!-- 顶栏 -->
    <div class="p-header">
      <el-button text @click="router.back()">← 返回</el-button>
      <h3>衍生练习</h3>
    </div>

    <!-- 薄弱知识点 -->
    <div v-if="weakPoints.length" class="weak-section">
      <span class="weak-label">薄弱知识点：</span>
      <el-tag
        v-for="(wp, i) in weakPoints"
        :key="i"
        :type="i<3?'danger':'warning'"
        size="small"
        class="weak-tag"
      >
        {{ wp.name }} ({{ wp.frequency }}次)
      </el-tag>
    </div>

    <!-- 进度 -->
    <el-progress :percentage="progressPct" :stroke-width="6" class="p-progress" />

    <!-- 未开始状态 -->
    <div v-if="!sessionId" class="start-box">
      <el-empty description="分析错题，生成针对性衍生练习" />
      <el-button
        type="primary"
        size="large"
        :loading="generating"
        @click="doGenerate"
      >
        开始生成衍生练习
      </el-button>
    </div>

    <!-- AI生成中 -->
    <div v-else-if="generating" class="generating-box">
      <el-icon class="gen-spin" :size="32"><Loading /></el-icon>
      <p class="gen-text">AI 正在出题，请稍候...</p>
      <p class="gen-hint">根据你的薄弱知识点智能生成针对性练习</p>
    </div>

    <!-- 答题区 -->
    <div v-else-if="!submitted" class="question-area">
      <div class="q-header">第 {{ currentIdx + 1 }} / {{ questions.length }} 题</div>
      <div class="q-card">
        <div class="q-type"><el-tag size="small">{{ typeLabel(questions[currentIdx]?.questionType) }}</el-tag></div>
        <div class="q-text">{{ questions[currentIdx]?.questionText }}</div>

        <!-- 选择题选项 -->
        <div v-if="isChoice(questions[currentIdx]?.questionType)" class="q-options">
          <div
            v-for="(opt, oi) in questions[currentIdx]?.options"
            :key="oi"
            class="q-opt"
            :class="{ selected: isSelected(oi) }"
            @click="toggleOption(oi)"
          >
            <span class="opt-letter">{{ String.fromCharCode(65 + oi) }}</span>
            <span class="opt-text">{{ opt }}</span>
          </div>
        </div>

        <!-- 填空/简答 -->
        <div v-else class="q-input-area">
          <el-input
            v-model="currentAnswer"
            type="textarea"
            :rows="3"
            placeholder="输入你的答案..."
          />
        </div>
      </div>

      <div class="q-nav">
        <el-button :disabled="currentIdx === 0" @click="currentIdx--">上一题</el-button>
        <span class="q-idx">{{ currentIdx + 1 }} / {{ questions.length }}</span>
        <el-button v-if="currentIdx < questions.length - 1" type="primary" @click="currentIdx++">下一题</el-button>
        <el-button
          v-else
          type="success"
          :loading="submitting"
          @click="doSubmit"
        >
          提交全部
        </el-button>
      </div>
    </div>

    <!-- 结果面板 -->
    <div v-else class="result-panel">
      <div class="result-score">
        <el-progress
          type="circle"
          :percentage="score"
          :color="scoreColor"
          :width="100"
        />
        <div class="score-text">{{ correctCount }} / {{ totalQuestions }} 正确</div>
        <div v-if="previousAvgScore != null && previousAvgScore > 0" class="score-trend" :class="score >= previousAvgScore ? 'trend-up' : 'trend-down'">
          {{ score >= previousAvgScore ? '↑' : '↓' }} 历史平均 {{ previousAvgScore }}%
        </div>
      </div>
      <div
        v-for="(r, i) in results"
        :key="i"
        class="result-card"
        :class="{ 'rc-correct': r.isCorrect, 'rc-wrong': !r.isCorrect }"
      >
        <div class="rc-header">
          <el-tag :type="r.isCorrect?'success':'danger'" size="small">{{ r.isCorrect ? '✓ 正确' : '✗ 错误' }}</el-tag>
          <span class="rc-idx">第 {{ i + 1 }} 题</span>
        </div>
        <div class="rc-text">{{ questions[i]?.questionText }}</div>
        <div v-if="r.studentAnswer && r.studentAnswer !== ''" class="rc-answer">你的答案：{{ r.studentAnswer }}</div>
        <div class="rc-correct-answer">正确答案：{{ r.correctAnswer }}</div>
        <div v-if="r.explanation" class="rc-explain">{{ r.explanation }}</div>
      </div>
      <div class="result-actions">
        <el-button type="primary" @click="doGenerate">再来一组</el-button>
        <el-button @click="router.push('/wrong-book')">返回错题本</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'
import { generateDerivedPractice, getPracticeStatus, getPracticeSession, submitPracticeSession, getStudentStats } from '@/api/wrong'

const router = useRouter()
const generating = ref(false)
const submitting = ref(false)
const sessionId = ref(null)
const questions = ref([])
const weakPoints = ref([])
const totalQuestions = ref(0)
const currentIdx = ref(0)
const submitted = ref(false)
const results = ref([])
const correctCount = ref(0)
const score = ref(0)
const previousAvgScore = ref(0)
const answersMap = ref({})

const progressPct = computed(() => questions.value.length
  ? Math.round(((submitted.value ? questions.value.length : currentIdx.value) / questions.value.length) * 100) : 0)
const scoreColor = computed(() => score.value >= 80 ? 'var(--el-color-success)' : score.value >= 60 ? 'var(--el-color-warning)' : 'var(--el-color-danger)')
const currentAnswer = computed({
  get: () => answersMap.value[currentIdx.value] || '',
  set: (v) => { answersMap.value[currentIdx.value] = v }
})

function typeLabel(t) {
  return QUESTION_TYPE_LABEL[t] || t
}
function isChoice(t) { return t === 'SINGLE_CHOICE' || t === 'MULTI_CHOICE' || t === 'TRUE_FALSE' }
function isSelected(oi) {
  const letter = String.fromCharCode(65 + oi)
  const type = questions.value[currentIdx.value]?.questionType
  if (type === 'MULTI_CHOICE') {
    return currentAnswer.value.includes(letter)
  }
  return currentAnswer.value === letter
}
function toggleOption(oi) {
  const letter = String.fromCharCode(65 + oi)
  const type = questions.value[currentIdx.value]?.questionType
  if (type === 'MULTI_CHOICE') {
    // 多选：切换选中状态
    const selected = currentAnswer.value ? currentAnswer.value.split('') : []
    const idx = selected.indexOf(letter)
    if (idx >= 0) selected.splice(idx, 1)
    else selected.push(letter)
    currentAnswer.value = selected.sort().join('')
  } else {
    currentAnswer.value = letter
  }
}

async function doGenerate() {
  generating.value = true
  try {
    // 0. 先查是否有未掌握的错题
    try {
      const stats = await getStudentStats()
      if (stats?.code === 200 && stats.data) {
        const unmastered = stats.data.unmastered || 0
        if (unmastered === 0) {
          ElMessage.warning('您还没有未掌握的错题，请先完成考试或练习任务，积累错题后再来')
          return
        }
      }
    } catch { /* 检查失败，继续尝试生成 */ }

    // 1. 提交生成任务
    const res = await generateDerivedPractice()
    if (res.code !== 200 || !res.data?.sessionId) {
      ElMessage.error(res.message || '生成失败')
      return
    }
    sessionId.value = res.data.sessionId

    // 2. 如果状态为 generating，轮询等待AI出题
    if (res.data.status === 'generating') {
      ElMessage.info('AI 正在出题，请稍候...')
      const deadline = Date.now() + 60_000
      let done = false
      while (Date.now() < deadline && !done) {
        try {
          const statusRes = await getPracticeStatus(sessionId.value)
          if (statusRes.code === 200 && statusRes.data) {
            if (statusRes.data.status === 'ongoing') {
              questions.value = statusRes.data.items || []
              weakPoints.value = statusRes.data.weakPoints || []
              totalQuestions.value = statusRes.data.totalQuestions || questions.value.length
              done = true
            }
          }
        } catch { /* 请求异常，sleep后重试 */ }
        if (!done) await sleep(2000)
      }
      if (!done) {
        // 超时：尝试通过 getPracticeSession 加载已有题目
        const fallback = await getPracticeSession(sessionId.value)
        if (fallback.code === 200 && fallback.data) {
          questions.value = fallback.data.items || []
          weakPoints.value = fallback.data.weakPoints || []
          totalQuestions.value = fallback.data.totalQuestions || questions.value.length
          ElMessage.warning('AI出题超时，已加载现有题库题目')
        }
      }
    } else if (res.data.status === 'ongoing') {
      // 直接完成（无需AI），通过getPracticeSession加载题目
      const sessionRes = await getPracticeSession(sessionId.value)
      if (sessionRes.code === 200 && sessionRes.data) {
        questions.value = sessionRes.data.items || []
        weakPoints.value = sessionRes.data.weakPoints || []
        totalQuestions.value = sessionRes.data.totalQuestions || questions.value.length
      }
    }

    if (questions.value.length) {
      currentIdx.value = 0; submitted.value = false; results.value = []; answersMap.value = {}
      ElMessage.success(`已生成 ${questions.value.length} 道衍生练习`)
    } else {
      ElMessage.error('未生成有效题目，请重试')
    }
  } catch (e) {
    const msg = e?.message || ''
    if (msg.includes('未掌握的错题') || msg.includes('无需衍生')) {
      ElMessage.warning('您还没有未掌握的错题，请先完成考试或练习任务积累错题后再来')
    } else {
      ElMessage.error(msg || '生成失败，请重试')
    }
  }
  finally { generating.value = false }
}

function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)) }

async function doSubmit() {
  submitting.value = true
  const answers = questions.value.map((q, i) => ({
    itemId: q.itemId,
    answer: answersMap.value[i] || ''
  }))
  try {
    const res = await submitPracticeSession(sessionId.value, answers)
    if (res.code === 200 && res.data) {
      results.value = res.data.results || []
      correctCount.value = res.data.correctCount || 0
      score.value = res.data.score || 0
      previousAvgScore.value = res.data.previousAvgScore || 0
      submitted.value = true
      ElMessage.success(`得分: ${score.value}%`)
    }
  } catch { ElMessage.error('提交失败') }
  finally { submitting.value = false }
}

onMounted(() => { /* 页面加载，等待用户点击生成 */ })
</script>

<style scoped>
.practice-page { margin: 0 auto; padding: 16px; display: flex; flex-direction: column; gap: 14px; min-height: calc(100vh - 64px); }
.p-header { display: flex; align-items: center; gap: 12px; }
.p-header h3 { margin: 0; }
.weak-section { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.weak-label { font-size: var(--fs-sm); color: var(--text-secondary); white-space: nowrap; }
.weak-tag { margin: 1px; }
.p-progress { margin: 4px 0; }
.start-box { text-align: center; padding: 40px 0; }
.generating-box { text-align: center; padding: 48px 0; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.gen-spin { color: var(--primary-color); animation: gen-spin 1.5s linear infinite; }
@keyframes gen-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.gen-text { font-size: var(--fs-lg); color: var(--text-primary); font-weight: 500; margin: 0; }
.gen-hint { font-size: var(--fs-sm); color: var(--text-secondary); margin: 0; }
.question-area { display: flex; flex-direction: column; gap: 12px; }
.q-header { font-size: var(--fs-md); color: var(--text-secondary); font-weight: 500; }
.q-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 18px; }
.q-type { margin-bottom: 10px; }
.q-text { font-size: var(--fs-lg); line-height: 1.7; color: var(--text-primary); margin-bottom: 14px; }
.q-options { display: flex; flex-direction: column; gap: 8px; }
.q-opt { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border: 1px solid var(--border-light); border-radius: var(--radius-sm); cursor: pointer; transition: all var(--transition-fast); }
.q-opt:hover { border-color: var(--primary-color); background: var(--primary-light); }
.q-opt.selected { border-color: var(--primary-color); background: var(--primary-light); font-weight: 600; }
.opt-letter { width: 24px; height: 24px; border-radius: var(--radius-full); background: var(--bg-section); display: flex; align-items: center; justify-content: center; font-size: var(--fs-sm); font-weight: 600; color: var(--text-secondary); flex-shrink: 0; }
.opt-text { flex: 1; color: var(--text-primary); }
.q-input-area { margin-top: 8px; }
.q-nav { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 8px 0; }
.q-idx { font-size: var(--fs-sm); color: var(--text-secondary); min-width: 60px; text-align: center; }
.result-panel { display: flex; flex-direction: column; gap: 12px; }
.result-score { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 16px 0; }
.score-text { font-size: var(--fs-md); color: var(--text-primary); }
.score-trend { font-size: var(--fs-sm); padding: 2px 12px; border-radius: 12px; }
.trend-up { color: var(--el-color-success); background: var(--el-color-success-light-9, #f0f9eb); }
.trend-down { color: var(--el-color-danger); background: var(--el-color-danger-light-9, #fef0f0); }
.result-card { background: var(--bg-card); border-radius: var(--radius-md); padding: 14px; border-left: 4px solid var(--border-color); }
.result-card.rc-correct { border-left-color: var(--el-color-success); }
.result-card.rc-wrong { border-left-color: var(--el-color-danger); }
.rc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.rc-idx { font-size: var(--fs-xs); color: var(--text-secondary); }
.rc-text { font-size: var(--fs-md); color: var(--text-primary); line-height: 1.6; margin-bottom: 8px; }
.rc-answer { font-size: var(--fs-sm); color: var(--el-color-danger); margin-bottom: 4px; }
.rc-correct-answer { font-size: var(--fs-sm); color: var(--el-color-success); font-weight: 600; margin-bottom: 4px; }
.rc-explain { font-size: var(--fs-xs); color: var(--text-secondary); background: var(--bg-section); padding: 8px 10px; border-radius: var(--radius-sm); margin-top: 4px; }
.result-actions { display: flex; justify-content: center; gap: 12px; padding: 12px 0; }

@media (max-width: 768px) {
  .practice-page { padding: 8px; gap: 10px; }
  .q-text { font-size: var(--fs-md); }
}
</style>
