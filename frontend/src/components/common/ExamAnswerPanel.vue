<template>
  <div class="exam-mode">
    <!-- D8: 子级 exam-timer 已删除，时间信息由父级 TaskSubmitPanel.vue 的 .fixed-timer-bar 承担
         前置条件：附录 C 已修 examStarted localStorage 持久化（路由守卫与计时器不再被刷新拆散） -->
    <div class="exam-sticky-header">
      <div v-if="maxWarnings > 0" class="cheat-monitor" :class="{ terminated: terminated }">
        <el-icon><WarningFilled /></el-icon>
        <span v-if="terminated" class="cheat-terminated">{{ gradingMessage || '切屏已达上限，考试已终止并记为0分' }}</span>
        <span v-else class="cheat-count">切屏警告：{{ cheatCount }}/{{ maxWarnings }}</span>
        <span v-if="!terminated && cheatCount > 0" class="cheat-hint">点击页面任意位置返回考试</span>
      </div>

      <div class="q-nav">
        <div
          v-for="(q, idx) in questions"
          :key="q.id"
          class="q-nav-item"
          :class="{ active: idx === currentQ, answered: isQuestionAnswered(q), flagged: flaggedQuestions[idx] }"
          role="button"
          :tabindex="idx === currentQ ? 0 : -1"
          :aria-label="'第'+(idx+1)+'题' + (isQuestionAnswered(q) ? '(已答)' : '') + (flaggedQuestions[idx] ? '(已标记)' : '')"
          :aria-current="idx === currentQ ? 'step' : undefined"
          @click="currentQ = idx"
          @keydown.enter="currentQ = idx"
          @keydown.space.prevent="currentQ = idx"
        >
          {{ idx + 1 }}
        </div>
      </div>
      <div class="q-nav-global-submit">
        <span class="q-progress">{{ currentQ + 1 }} / {{ questions.length }}</span>
        <el-button
          v-if="currentQ < questions.length - 1"
          type="success"
          size="small"
          :loading="submitting"
          :disabled="terminated || submitting || isExpired"
          @click="handleSubmit"
        >
          {{ terminated ? '考试已终止' : '提交试卷' }}
        </el-button>
      </div>
    </div>

    <!-- 当前题目 -->
    <div v-if="currentQuestion" class="q-current">
      <div class="q-header">
        <span class="q-title" v-html="(currentQ+1)+'. '+renderMath(renderImages(currentQuestion.questionText))"></span>
        <el-tag size="small" :type="getTypeTag(currentQuestion.questionType)">{{ QUESTION_TYPE_LABEL[currentQuestion.questionType] || currentQuestion.questionType }}</el-tag>
        <span class="q-score">{{ currentQuestion.score }}分</span>
        <el-button
          size="small"
          text
          type="warning"
          :disabled="terminated"
          :aria-pressed="!!flaggedQuestions[currentQ]"
          :title="flaggedQuestions[currentQ] ? '已标记，再次点击取消标记' : '标记此题以便稍后复查'"
          :aria-label="flaggedQuestions[currentQ] ? '已标记，再次点击取消标记' : '标记此题以便稍后复查'"
          :class="{ 'flag-active': flaggedQuestions[currentQ] }"
          @click="toggleFlag(currentQ)"
        >
          <el-icon><Flag /></el-icon>
          {{ flaggedQuestions[currentQ] ? '已标记' : '标记' }}
        </el-button>
      </div>

      <div v-if="currentQuestion.questionType === 'MULTI_CHOICE'" class="multi-banner">
        <el-icon><WarningFilled /></el-icon>
        <span>多项选择题 — 请选择<strong>所有</strong>正确答案，选错或漏选均不得分</span>
      </div>

      <div v-if="hasUndo" style="margin-bottom:8px">
        <el-button
          size="small"
          text
          type="primary"
          @click="undoAnswer"
        >
          ↩ 撤销修改
        </el-button>
      </div>
      <template v-if="currentQuestion.questionType === 'SINGLE_CHOICE'">
        <el-radio-group v-model="answers[currentQuestion.id]" :disabled="terminated || submitting || isExpired" class="q-options">
          <el-radio
            v-for="(opt, oi) in parseOpts(currentQuestion.options)"
            :key="oi"
            :value="String.fromCharCode(65 + oi)"
            size="large"
          >
            <span class="q-opt-letter">{{ String.fromCharCode(65 + oi) }}.</span> <span v-html="renderMath(cleanOptLabel(opt))" />
          </el-radio>
        </el-radio-group>
      </template>
      <template v-else-if="currentQuestion.questionType === 'MULTI_CHOICE'">
        <el-checkbox-group v-model="multiAnswers[currentQuestion.id]" :disabled="terminated || submitting || isExpired" class="q-options q-options--multi">
          <el-checkbox
            v-for="(opt, oi) in parseOpts(currentQuestion.options)"
            :key="oi"
            :value="String.fromCharCode(65 + oi)"
            size="large"
          >
            <span class="q-opt-letter q-opt-letter--multi">{{ String.fromCharCode(65 + oi) }}.</span> <span v-html="renderMath(cleanOptLabel(opt))" />
          </el-checkbox>
        </el-checkbox-group>
      </template>
      <template v-else-if="currentQuestion.questionType === 'TRUE_FALSE'">
        <el-radio-group v-model="answers[currentQuestion.id]" :disabled="terminated || submitting || isExpired" class="q-options">
          <el-radio value="A">正确</el-radio>
          <el-radio value="B">错误</el-radio>
        </el-radio-group>
      </template>
      <template v-else-if="currentQuestion.questionType === 'DRAG_SORT'">
        <div class="sort-list">
          <div v-for="(item, si) in sortAnswers[currentQuestion.id]" :key="si" class="sort-row">
            <span class="sort-idx">{{ si + 1 }}</span>
            <span class="sort-text">{{ item }}</span>
            <el-button size="small" :disabled="si===0" @click="swapSort(currentQuestion.id, si, si-1)">↑</el-button>
            <el-button size="small" :disabled="si===sortAnswers[currentQuestion.id].length-1" @click="swapSort(currentQuestion.id, si, si+1)">↓</el-button>
          </div>
        </div>
      </template>
      <template v-else-if="currentQuestion.questionType === 'MATCHING'">
        <div v-for="(lp, mi) in parseMatchOptions(currentQuestion.options)" :key="mi" class="match-row">
          <span class="match-left">{{ lp.left }}</span>
          <span class="match-arrow">→</span>
          <el-select
            v-model="matchAnswers[currentQuestion.id][mi]"
            placeholder="选择"
            size="small"
            class="match-select"
          >
            <el-option
              v-for="rp in shuffleRight(currentQuestion.options)"
              :key="rp"
              :value="rp"
              :label="rp"
            />
          </el-select>
        </div>
      </template>
      <template v-else-if="currentQuestion.questionType === 'CLOZE'">
        <div class="cloze-inline">
          <template v-for="(seg, si) in parsedClozeSegments" :key="si">
            <span v-if="seg.type === 'text'" class="cloze-text-inline" v-html="sanitizeHtml(seg.content)" />
            <span v-else class="cloze-blank-inline">
              <el-input
                v-model="clozeAnswers[currentQuestion.id][seg.index]"
                size="small"
                class="cloze-input"
                :placeholder="'空格' + (seg.index + 1)"
              />
              <span class="cloze-label-inline">{{ seg.index + 1 }}</span>
            </span>
          </template>
        </div>
      </template>
      <template v-else-if="currentQuestion.questionType === 'PROGRAMMING'">
        <div class="code-lang">{{ getCodeLang(currentQuestion) }}</div>
        <el-input
          v-model="answers[currentQuestion.id]"
          type="textarea"
          :rows="examRows"
          class="code-input"
          placeholder="在此编写代码…"
          :disabled="terminated || submitting || isExpired"
        />
      </template>
      <template v-else>
        <el-input v-model="answers[currentQuestion.id]" :disabled="terminated || submitting || isExpired" placeholder="请输入答案" />
      </template>

      <div class="q-nav-btns">
        <el-button :disabled="terminated || currentQ === 0" @click="currentQ--">上一题</el-button>
        <span class="q-progress">{{ currentQ + 1 }} / {{ questions.length }}</span>
        <el-button
          v-if="currentQ < questions.length - 1"
          type="primary"
          :disabled="terminated"
          @click="currentQ++"
        >
          下一题
        </el-button>
        <el-button
          v-else
          type="success"
          :loading="submitting"
          :disabled="terminated || submitting || isExpired"
          @click="handleSubmit"
        >
          {{ terminated ? '考试已终止' : '提交试卷' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { WarningFilled, Flag } from '@element-plus/icons-vue'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'
import { renderMath } from '@/composables/useQuestionHelpers'
import { sanitizeHtml } from '@/utils/markdown'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

// D7: 编程题桌面 18 行、移动 10 行
const examRows = computed(() => isMobile.value ? 10 : 18)

const props = defineProps({
  questions: { type: Array, default: () => [] },
  currentQ: { type: Number, default: 0 },
  answers: { type: Object, default: () => ({}) },
  multiAnswers: { type: Object, default: () => ({}) },
  sortAnswers: { type: Object, default: () => ({}) },
  matchAnswers: { type: Object, default: () => ({}) },
  clozeAnswers: { type: Object, default: () => ({}) },
  flaggedQuestions: { type: Object, default: () => ({}) },
  durationMinutes: { type: Number, default: 0 },
  examStarted: { type: Boolean, default: false },
  maxWarnings: { type: Number, default: 0 },
  cheatCount: { type: Number, default: 0 },
  terminated: { type: Boolean, default: false },
  gradingMessage: { type: String, default: '' },
  submitting: { type: Boolean, default: false },
  isExpired: { type: Boolean, default: false },
  examSeconds: { type: Number, default: 0 },
})

const emit = defineEmits(['submit', 'update:currentQ', 'toggleFlag'])

const currentQ = computed({
  get: () => props.currentQ,
  set: (v) => emit('update:currentQ', v),
})

const currentQuestion = computed(() => props.questions[currentQ.value] || null)

// D8: 已删除子级倒计时相关常量/computed (URGENT_THRESHOLD / CRITICAL_THRESHOLD / isCountdownUrgent / isCountdownCritical / countdownDisplay)
// 时间信息现由父级 TaskSubmitPanel.vue 的 .fixed-timer-bar 单点承担

const isQuestionAnswered = (q) => {
  const a = props.answers[q.id]; const m = props.multiAnswers[q.id]; const s = props.sortAnswers[q.id]
  return (a !== undefined && a !== '') || (m !== undefined && m.length > 0) ||
    (s !== undefined && s.length > 0) || (props.clozeAnswers[q.id] !== undefined && props.clozeAnswers[q.id].some(v => v))
}

const toggleFlag = (idx) => { emit('toggleFlag', idx) }

// ── 答案撤销 ──
const answerHistory = ref({}) // { [questionId]: previousAnswer }

// 切题时自动保存当前题目的答案快照
watch(currentQ, (newQ, oldQ) => {
  if (oldQ !== undefined) {
    const val = props.answers[oldQ]
    if (val !== undefined) answerHistory.value[oldQ] = val
    const mVal = props.multiAnswers[oldQ]
    if (mVal !== undefined) answerHistory.value[oldQ] = [...mVal]
  }
})

const hasUndo = computed(() => {
  const qId = currentQuestion.value?.id
  return qId !== undefined && answerHistory.value[qId] !== undefined
})

function undoAnswer() {
  const qId = currentQuestion.value?.id
  if (qId !== undefined && answerHistory.value[qId] !== undefined) {
    props.answers[qId] = answerHistory.value[qId]
    delete answerHistory.value[qId]
  }
}

const unansweredCount = computed(() => {
  if (!props.questions) return 0
  return props.questions.filter(q => !isQuestionAnswered(q)).length
})

const handleSubmit = async () => {
  const n = unansweredCount.value
  if (n > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${n} 道题未作答，确定提交吗？`,
        '确认提交',
        { confirmButtonText: '确定提交', cancelButtonText: '继续作答', type: 'warning' }
      )
    } catch { return }
  }
  emit('submit')
}

const parseOpts = (opts) => {
  if (!opts) return []
  try {
    const parsed = typeof opts === 'string' ? JSON.parse(opts) : opts
    if (!Array.isArray(parsed)) return []
    // 兼容两种格式：["文本"] 和 [{"key":"A","text":"文本"}]
    return parsed.map(o => {
      if (typeof o === 'string') return o
      if (o && typeof o === 'object') return o.text || o.label || o.value || String(o)
      return String(o)
    })
  } catch { return [] }
}
const cleanOptLabel = (opt) => (opt || '').replace(/^[A-Z][.、．)\-：:\s]{1,2}/, '').trim()
const getTypeTag = (type) => ({ SINGLE_CHOICE:'', MULTI_CHOICE:'warning', TRUE_FALSE:'success', FILL_IN:'info', SHORT_ANSWER:'', ESSAY:'danger' }[type] || '')
const renderImages = (text) => {
  if (!text) return ''
  // D12: 内联 style → class，由 CSS 统一控制 .q-image
  return text.replace(/\[图片\]\(([^)]+)\)/g, '<img src="$1" class="q-image" loading="lazy" />')
}

const swapSort = (qid, i, j) => { const arr = props.sortAnswers[qid]; const t = arr[i]; arr[i] = arr[j]; arr[j] = t }
const parseMatchOptions = (opts) => {
  try { return JSON.parse(opts || '[]') } catch { return [] }
}
const shuffleRight = (opts) => {
  const pairs = parseMatchOptions(opts)
  const right = pairs.map(p => p.right)
  return [...new Set(right)].sort(() => Math.random() - 0.5)
}
const parsedClozeSegments = computed(() => {
  const text = currentQuestion.value?.questionText || ''
  const parts = []
  let lastIdx = 0
  const re = /_{3,}|【(.+?)】/g
  let m, ci = 0
  while ((m = re.exec(text)) !== null) {
    if (m.index > lastIdx) parts.push({ type: 'text', content: text.slice(lastIdx, m.index) })
    parts.push({ type: 'blank', index: ci })
    ci++
    lastIdx = m.index + m[0].length
  }
  if (lastIdx < text.length) parts.push({ type: 'text', content: text.slice(lastIdx) })
  return parts
})
const getCodeLang = (q) => {
  try { return JSON.parse(q.answerSchema || '{}').language || '' } catch { return '' }
}
</script>

<style scoped>
.exam-mode { display: flex; flex-direction: column; }
.exam-sticky-header { position: sticky; top: 0; z-index: 10; background: var(--bg-card); padding: 0 0 8px 0; border-bottom: 1px solid var(--border-light); margin-bottom: 16px; }
/* D8: 已删除子级 .exam-timer / .timer-text / .timer-hint / @keyframes pulse 旧 CSS
     时间信息现由父级 TaskSubmitPanel.vue 的 .fixed-timer-bar 单点承担 */
.cheat-monitor { display: flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: var(--radius-md); background: var(--bg-warning-light); color: var(--el-color-warning); margin-bottom: 8px; font-size: var(--fs-sm); }
.cheat-monitor.terminated { background: var(--bg-danger-light); color: var(--el-color-danger); }
.cheat-count { font-weight: 600; }
.cheat-terminated { font-weight: 700; }
.cheat-hint { margin-left: auto; font-size: var(--fs-xs, 12px); opacity: 0.8; }
/* D4: 题号导航 grid 16 列（桌面默认），移动端 8 列见 media query */
.q-nav { display: grid; grid-template-columns: repeat(16, 1fr); gap: 8px; margin-bottom: 8px; }
.q-nav-global-submit { display: flex; align-items: center; justify-content: center; gap: 16px; margin-bottom: 0; }
.q-nav-item { height: 48px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: var(--fs-md); font-weight: 700; cursor: pointer; background: var(--bg-section); color: var(--text-secondary); border: 2px solid transparent; transition: all 0.2s; }
.q-nav-item.active { background: var(--primary-color); color: var(--el-color-white, #fff); border-color: var(--primary-color); box-shadow: var(--shadow-base); }
.q-nav-item.answered { background: var(--bg-success-light); color: var(--el-color-success); border-color: var(--el-color-success); }
.q-nav-item.answered.active { background: var(--primary-color); color: var(--el-color-white, #fff); }
.q-nav-item.flagged { border-color: var(--el-color-warning); }
/* a11y: 题号键盘 focus 可见 */
.q-nav-item:focus-visible { outline: 2px solid var(--primary-color); outline-offset: 2px; }
/* D2: 解除 880 套娃，padding 加大；行宽由 .q-text-inner 限 */
.q-current { background: var(--bg-card); border: 0.5px solid var(--border-color); border-radius: var(--radius-xl); padding: 40px 56px; min-height: 260px; width: 100%; box-shadow: var(--shadow-base); }
.q-text-inner { max-width: 920px; }
.q-header { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 20px; padding-bottom: 14px; border-bottom: 0.5px solid var(--border-color); }
.q-header .q-title { font-size: var(--fs-lg); font-weight: 700; color: var(--text-primary); flex: 1; min-width: 200px; }
.q-score { font-weight: 700; color: var(--primary-color); font-size: var(--fs-md); white-space: nowrap; }
.q-options { display: flex; flex-direction: column; gap: 14px; margin-top: 12px; }
/* V4: 选项 padding 加大 + 选中强化 border-left + 位移 + 背景；a11y focus 可见
   FIX-1: !important 覆盖全局 index.scss 强制规则，根治移动端长文本重叠 */
.q-options :deep(.el-radio), .q-options :deep(.el-checkbox) { margin-right: 0 !important; display: flex !important; align-items: flex-start !important; justify-content: flex-start !important; text-align: left !important; padding: 16px 20px; border-radius: var(--radius-md); border: 1.5px solid var(--border-light); transition: all 0.15s; height: auto; width: 100%; box-sizing: border-box; }
.q-options :deep(.el-radio:hover), .q-options :deep(.el-checkbox:hover) { background: var(--bg-hover); border-color: var(--primary-color); }
.q-options :deep(.el-radio.is-checked), .q-options :deep(.el-checkbox.is-checked) { background: var(--bg-success-light); border-color: var(--primary-color); border-left: 3px solid var(--primary-color); transform: translateX(2px); }
.q-options :deep(.el-radio:focus-visible), .q-options :deep(.el-checkbox:focus-visible) { outline: 2px solid var(--primary-color); outline-offset: 2px; }
.q-options :deep(.el-radio__label), .q-options :deep(.el-checkbox__label) { white-space: normal !important; line-height: 1.6 !important; text-align: left !important; font-size: var(--fs-lg); flex: 1 1 auto !important; min-width: 0 !important; max-width: 100% !important; word-break: break-word !important; overflow-wrap: anywhere !important; padding-left: 8px; }
.q-options :deep(.el-radio__input), .q-options :deep(.el-checkbox__input) { align-self: flex-start !important; flex-shrink: 0 !important; margin-top: 2px; margin-right: 0; }
.q-opt-letter { font-weight: 700; color: var(--primary-color); margin-right: 4px; font-size: var(--fs-lg); }
.q-options--multi :deep(.el-checkbox) { border-color: var(--el-color-success); border-width: 2px; }
.q-options--multi :deep(.el-checkbox.is-checked) { background: var(--bg-success-light); border-color: var(--el-color-success); border-left: 3px solid var(--el-color-success); }
.q-opt-letter--multi { color: var(--el-color-success); }
/* V3: Flag 标记按钮 — 已标记态加警告色 */
.flag-active { color: var(--el-color-warning); font-weight: 600; }
/* V1: .multi-banner 去掉硬编码 fallback */
.multi-banner { display: flex; align-items: center; gap: 8px; padding: 10px 16px; margin-bottom: 12px; border-radius: var(--radius-md); background: var(--bg-warning-light); border: 1px solid var(--el-color-warning-light-5); color: var(--el-color-warning); font-size: var(--fs-md); font-weight: 500; }
.multi-banner .el-icon { font-size: var(--fs-lg); flex-shrink: 0; }
.multi-banner strong { color: var(--el-color-danger); }
.q-nav-btns { display: flex; align-items: center; justify-content: space-between; margin-top: 24px; padding-top: 14px; border-top: 0.5px solid var(--border-color); }
.q-nav-btns .el-button { font-size: var(--fs-md); }
.q-progress { font-size: var(--fs-md); font-weight: 600; color: var(--text-secondary); }
.sort-list { display: flex; flex-direction: column; gap: 4px; }
.sort-row { display: flex; align-items: center; gap: 6px; padding: 6px 8px; background: var(--bg-section); border-radius: var(--radius-sm); }
.sort-idx { width: 24px; text-align: center; font-weight: 600; color: var(--primary-color); }
.sort-text { flex: 1; font-size: var(--fs-sm); }
.match-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.match-left { min-width: 80px; font-size: var(--fs-sm); font-weight: 500; }
.match-arrow { color: var(--text-secondary); }
/* D5: 匹配下拉桌面 220px，移动 calc(50% - 24px) 见 media query */
.match-select { width: 220px; }
.cloze-inline { font-size: var(--fs-md); line-height: 2.6; margin-bottom: 8px; padding: 12px 16px; background: var(--bg-section); border-radius: var(--radius-md); }
.cloze-text-inline { display: inline; }
.cloze-blank-inline { display: inline-flex; align-items: center; vertical-align: middle; margin: 0 4px; position: relative; }
/* D6: 完型填空桌面 120px / 高 36px，去掉 !important */
.cloze-input { width: 120px; display: inline-flex; }
.cloze-input :deep(.el-input__inner) { height: 36px; font-size: var(--fs-md); text-align: center; }
.cloze-label-inline { position: absolute; top: -14px; left: 50%; transform: translateX(-50%); font-size: 10px; color: var(--text-secondary); white-space: nowrap; }
.code-lang { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 4px; }
.code-input :deep(textarea) { font-family: 'Consolas','Monaco','Courier New',monospace; font-size: var(--fs-sm); }
/* D12: 题图样式提取，桌面 max-width 680px 居中 */
.q-image { max-width: 680px; border-radius: var(--radius-md); margin: 6px 0; display: block; }
:deep(.el-radio), :deep(.el-checkbox) { justify-content: flex-start; text-align: left; }
:deep(.el-radio__label), :deep(.el-checkbox__label) { text-align: left; }

/* 移动端：题号 8 列、触控 44px、计时器已随 D8 删除、MATCHING 50%、CLOZE 100px、底部按钮纵向 */
@media (max-width: 768px) {
  .q-current { padding: 18px; border-radius: var(--radius-md); }
  .q-nav { grid-template-columns: repeat(8, 1fr); gap: 6px; }
  .q-nav-item { min-height: 44px; font-size: var(--fs-sm); }
  .q-nav-global-submit { flex-direction: column; gap: 8px; }
  .q-options :deep(.el-radio), .q-options :deep(.el-checkbox) { padding: 10px 12px; align-items: flex-start !important; }
  .q-options :deep(.el-radio__label), .q-options :deep(.el-checkbox__label) { font-size: var(--fs-md); line-height: 1.55 !important; flex: 1 1 auto !important; min-width: 0 !important; max-width: 100% !important; word-break: break-word !important; overflow-wrap: anywhere !important; white-space: normal !important; }
  .multi-banner { font-size: var(--fs-sm); padding: 8px 12px; }
  /* M3: 移动端匹配下拉占 50% */
  .match-select { width: calc(50% - 24px); }
  /* M4: 移动端完型填空 100px */
  .cloze-input { width: 100px; }
  /* V6: 底部按钮纵向 */
  .q-nav-btns { flex-direction: column-reverse; gap: 10px; }
  .q-nav-btns > * { width: 100%; }
  .q-image { max-width: 100%; }
}
</style>
