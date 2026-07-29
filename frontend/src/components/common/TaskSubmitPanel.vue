<template>
  <div class="submit-panel" :class="{ 'submit-panel--exam': showAnswers }">
    <!-- 考试模式：固定顶部倒计时栏 -->
    <div
      v-if="showAnswers && examStarted && durationMinutes > 0"
      class="fixed-timer-bar"
      :class="{ 'timer-flash': examSeconds <= URGENT_THRESHOLD }"
      role="timer"
      :aria-live="examSeconds <= CRITICAL_THRESHOLD ? 'assertive' : 'polite'"
    >
      <el-icon><Clock /></el-icon>
      <span class="fixed-timer-text">{{ countdownChinese }}</span>
      <span v-if="examSeconds <= URGENT_THRESHOLD && examSeconds > CRITICAL_THRESHOLD" class="fixed-timer-hint">时间紧迫</span>
      <span v-else-if="examSeconds <= CRITICAL_THRESHOLD" class="fixed-timer-hint">即将结束！</span>
    </div>

    <!-- 断网提示 -->
    <el-alert
      v-if="!isOnline"
      title="当前网络不可用，请检查连接"
      type="error"
      :closable="false"
      show-icon
      class="network-alert"
    />

    <!-- 截止倒计时 -->
    <div v-if="deadline" class="countdown-bar" :class="urgencyClass">
      <el-icon><Clock /></el-icon>
      <span v-if="isExpired">已截止 {{ formatDeadline(deadline) }}</span>
      <span v-else-if="isUrgent">剩余 {{ countdownText }}</span>
      <span v-else>截止 {{ formatDeadline(deadline) }}</span>
    </div>

    <!-- 问卷模式 -->
    <div v-if="isSurvey" class="survey-mode">
      <div class="survey-progress">共 {{ surveyQuestions.length }} 题</div>
      <div v-for="(q, qi) in surveyQuestions" :key="q.id" class="survey-q">
        <div class="survey-q-label">
          {{ qi + 1 }}. {{ q.label }}
          <span v-if="q.required" class="survey-required">*必填</span>
        </div>
        <el-radio-group v-if="q.type === 'radio'" v-model="surveyAnswers[q.id]" class="survey-q-body">
          <el-radio v-for="opt in q.options" :key="opt" :value="opt">{{ opt }}</el-radio>
        </el-radio-group>
        <el-checkbox-group v-else-if="q.type === 'checkbox'" v-model="surveyAnswers[q.id]" class="survey-q-body">
          <el-checkbox v-for="opt in q.options" :key="opt" :value="opt">{{ opt }}</el-checkbox>
        </el-checkbox-group>
        <el-select
          v-else-if="q.type === 'dropdown'"
          v-model="surveyAnswers[q.id]"
          placeholder="请选择…"
          clearable
          class="survey-q-body"
          style="width:100%"
        >
          <el-option v-for="opt in q.options" :key="opt" :value="opt">{{ opt }}</el-option>
        </el-select>
        <el-rate
          v-else-if="q.type === 'rating'"
          v-model="surveyAnswers[q.id]"
          :max="5"
          show-score
          class="survey-q-body"
        />
        <el-slider
          v-else-if="q.type === 'scale'"
          v-model="surveyAnswers[q.id]"
          :min="0"
          :max="10"
          :step="1"
          show-input
          class="survey-q-body"
        />
        <el-date-picker
          v-else-if="q.type === 'date'"
          v-model="surveyAnswers[q.id]"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          class="survey-q-body"
          style="width:100%"
        />
        <el-input
          v-else-if="q.type === 'textarea'"
          v-model="surveyAnswers[q.id]"
          type="textarea"
          :rows="3"
          :placeholder="'请输入' + q.label"
        />
      </div>
    </div>

    <!-- 提交内容（非问卷模式） -->
    <el-form
      v-if="!isSurvey"
      ref="formRef"
      :model="form"
      label-position="top"
      class="submit-form"
    >
      <el-form-item v-if="showContent && !isSurvey" label="提交内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="8"
          placeholder="输入你的答案或说明..."
          :disabled="terminated || submitting || isExpired"
        />
      </el-form-item>

      <!-- 考试模式 -->
      <div v-if="showAnswers" class="exam-protected">
        <ExamAnswerPanel
          v-model:current-q="currentQ"
          :questions="questions"
          :answers="answers"
          :multi-answers="multiAnswers"
          :sort-answers="sortAnswers"
          :match-answers="matchAnswers"
          :cloze-answers="clozeAnswers"
          :flagged-questions="flaggedQuestions"
          :duration-minutes="durationMinutes"
          :exam-started="examStarted"
          :max-warnings="maxWarnings"
          :cheat-count="cheatCount"
          :terminated="terminated"
          :grading-message="props.gradingMessage"
          :submitting="submitting"
          :is-expired="isExpired"
          :exam-seconds="examSeconds"
          @submit="handleSubmit"
          @toggle-flag="toggleFlag"
        />
      </div>

      <el-form-item v-if="showAttachments && !isSurvey" label="附件上传">
        <el-upload
          ref="uploadRef"
          :action="UPLOAD_ACTION"
          :headers="uploadHeaders"
          name="files"
          :auto-upload="true"
          :limit="8"
          :on-exceed="() => ElMessage.warning('最多上传8个文件')"
          :on-progress="onUploadProgress"
          :on-success="onUploadSuccess"
          :on-error="onUploadError"
          :before-upload="beforeUpload"
          :disabled="terminated || submitting || isExpired"
        >
          <el-button size="small" :disabled="terminated || submitting || isExpired">
            <el-icon><Plus /></el-icon>选择文件
          </el-button>
        </el-upload>
        <div class="upload-hint">支持图片/文档/音视频，单文件≤10MB</div>
        <div v-if="form.attachments.length || Object.keys(uploadProgress).length" class="uploaded-files">
          <div v-if="form.attachments.length" class="uploaded-title">已上传 {{ form.attachments.length }} 个文件</div>
          <div v-for="(url, i) in form.attachments" :key="i" class="uploaded-item">
            <FilePreview :src="url" :filename="getFileName(url)" />
            <el-button
              size="small"
              type="danger"
              link
              @click="removeFile(i)"
            >
              删除
            </el-button>
          </div>
          <!-- 上传进度条 -->
          <div v-for="(pct, fname) in uploadProgress" :key="fname" class="upload-progress-row">
            <span class="up-name">{{ fname }}</span>
            <el-progress :percentage="pct" :stroke-width="6" :show-text="pct === 100" />
          </div>
        </div>
      </el-form-item>
    </el-form>

    <div class="submit-actions">
      <transition name="fade">
        <span v-if="draftSaved" class="draft-saved-hint">✓ 草稿已保存</span>
      </transition>
      <el-button v-if="isExpired" type="info" disabled>已截止，无法提交</el-button>
      <el-button
        v-else
        type="primary"
        :loading="submitting"
        :disabled="submitting"
        size="large"
        class="submit-btn-main"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中...' : '提交任务' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { formatDeadline, getDeadlineUrgency } from '@/utils/taskUtils'
import { UPLOAD_ACTION, getUploadHeaders } from '@/api/task'
import request from '@/utils/request'
import { useCheatMonitor } from '@/composables/useCheatMonitor'
import FilePreview from '@/components/renderers/FilePreview.vue'
import ExamAnswerPanel from './ExamAnswerPanel.vue'

const URGENT_THRESHOLD = 300
const CRITICAL_THRESHOLD = 60
const DEADLINE_URGENT_THRESHOLD = 600
const MAX_FILE_SIZE = 10 * 1024 * 1024
const DRAFT_SAVE_DEBOUNCE = 3000

const props = defineProps({
  taskId: { type: [Number, String], required: true },
  taskConfig: { type: [Object, String], default: null },
  taskType: { type: String, default: '' },
  surveySchema: { type: [Object, String], default: null },
  deadline: { type: String, default: '' },
  examStarted: { type: Boolean, default: false },
  durationMinutes: { type: Number, default: 0 },
  showContent: { type: Boolean, default: true },
  showAttachments: { type: Boolean, default: true },
  showAnswers: { type: Boolean, default: false },
  gradingMessage: { type: String, default: '' },
  questions: { type: Array, default: () => [] },
})
const isSurvey = computed(() => props.taskType === 'SURVEY')
const surveyQuestions = computed(() => {
  if (!isSurvey.value || !props.surveySchema) return []
  try { return typeof props.surveySchema === 'string' ? JSON.parse(props.surveySchema) : props.surveySchema } catch { return [] }
})
const surveyAnswers = reactive({})

const emit = defineEmits(['submit'])

const { cheatCount, maxWarnings, terminated, clearCache: clearCheatCache, flushPending, activate: activateCheat, deactivate: deactivateCheat } = useCheatMonitor(
  computed(() => props.taskId),
  computed(() => props.taskConfig),
)

// 考试开始时激活防作弊监控
watch(() => props.examStarted, (v) => {
  if (v) {
    activateCheat()
  } else {
    deactivateCheat()
  }
}, { immediate: true })

// 切屏达上限时自动提交当前作答（带重试）
watch(terminated, (v) => {
  if (v && !submitting.value) {
    handleSubmitWithRetry(3)
  }
})

const formRef = ref(null)
const form = reactive({ content: '', attachments: [] })
const answers = reactive({})
const multiAnswers = reactive({})
const sortAnswers = reactive({})
const optionMappings = reactive({})
const matchAnswers = reactive({})
const clozeAnswers = reactive({})
const submitting = ref(false)
const currentQ = ref(0)
const flaggedQuestions = reactive({})
const toggleFlag = (idx) => { flaggedQuestions[idx] = !flaggedQuestions[idx] }
const currentQuestion = computed(() => props.questions[currentQ.value] || null)
const isQuestionAnswered = (q) => {
  const a = answers[q.id]; const m = multiAnswers[q.id]; const s = sortAnswers[q.id]
  return (a !== undefined && a !== '') || (m !== undefined && m.length > 0) ||
    (s !== undefined && s.length > 0) || (clozeAnswers[q.id] !== undefined && clozeAnswers[q.id].some(v => v))
}

// 考试倒计时
const examSeconds = ref(0)
const countdownTimer = ref(null)
const countdownDisplay = computed(() => countdownChinese.value)
const countdownChinese = computed(() => {
  const h = Math.floor(examSeconds.value / 3600)
  const m = Math.floor((examSeconds.value % 3600) / 60)
  const s = examSeconds.value % 60
  if (h > 0) return `${h}小时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
})
const isCountdownUrgent = computed(() => examSeconds.value > 0 && examSeconds.value <= URGENT_THRESHOLD)
const isCountdownCritical = computed(() => examSeconds.value > 0 && examSeconds.value <= CRITICAL_THRESHOLD)
watch(() => props.examStarted, (v) => {
  if (v && props.durationMinutes > 0) {
    const totalSecs = props.durationMinutes * 60
    const startKey = `exam_start_${props.taskId}`
    const savedStart = localStorage.getItem(startKey)
    const elapsed = savedStart ? Math.floor((Date.now() - parseInt(savedStart)) / 1000) : 0
    examSeconds.value = Math.max(1, totalSecs - elapsed)
    countdownTimer.value = setInterval(() => {
      if (examSeconds.value > 0) examSeconds.value--
      else { clearInterval(countdownTimer.value); ElMessage.warning('考试时间到，正在自动提交...'); handleSubmit() }
    }, 1000)
  }
}, { immediate: true })
onUnmounted(() => { if (countdownTimer.value) clearInterval(countdownTimer.value) })
const uploadHeaders = getUploadHeaders()

// 草稿：后端存储（跨设备同步）
import { saveDraft, loadDraft, deleteDraft } from '@/api/task'
let draftTimer = null
const draftData = () => JSON.stringify({
  content: form.content,
  answers: { ...answers }, multiAnswers: { ...multiAnswers },
  sortAnswers: { ...sortAnswers }, matchAnswers: { ...matchAnswers }, clozeAnswers: { ...clozeAnswers },
  surveyAnswers: isSurvey.value ? { ...surveyAnswers } : {},
  attachments: form.attachments
})
const draftSaved = ref(false)
let draftSavedTimer = null
const autoSaveDraft = () => {
  if (!props.taskId) return
  clearTimeout(draftTimer)
  draftTimer = setTimeout(async () => {
    const data = draftData()
    try {
      await saveDraft(props.taskId, data)
      draftSaved.value = true
      clearTimeout(draftSavedTimer)
      draftSavedTimer = setTimeout(() => { draftSaved.value = false }, 2000)
    } catch {
      // API 失败时 fallback 到 localStorage（弱网/离线场景）
      try { localStorage.setItem(`draft_${props.taskId}`, data) } catch { /* */ }
    }
  }, DRAFT_SAVE_DEBOUNCE)
}
const clearDraftFn = () => {
  clearCheatCache()
  localStorage.removeItem(`exam_start_${props.taskId}`)
  localStorage.removeItem(`draft_${props.taskId}`)
  if (props.taskId) deleteDraft(props.taskId).catch(() => {})
}

// watch form changes for auto-save
watch([form, answers, multiAnswers, sortAnswers, matchAnswers, clozeAnswers, surveyAnswers], autoSaveDraft, { deep: true })

// 恢复草稿 + 获取上传白名单
onMounted(async () => {
  // 动态获取文件上传白名单（后端不可达时 fallback 硬编码列表）
  try {
    const extRes = await request.get('/settings/allowed-upload-exts')
    if (extRes.code === 200 && extRes.data?.length) ALLOWED_EXTS.value = extRes.data.join(',')
  } catch { /* fallback to FALLBACK_EXTS */ }
  if (!props.taskId) return
  let restored = false
  try {
    const r = await loadDraft(props.taskId)
    if (r.code === 200 && r.data?.content) {
      const saved = JSON.parse(r.data.content)
      if (saved && (saved.content || saved.attachments?.length || Object.keys(saved.answers || {}).length || Object.keys(saved.surveyAnswers || {}).length)) {
        try { await ElMessageBox.confirm('发现云端草稿，是否恢复？', '恢复草稿', { confirmButtonText: '恢复', cancelButtonText: '暂不恢复', type: 'info' }) } catch { ElMessage.info('草稿已保留，可稍后恢复'); return }
        if (saved.content) form.content = saved.content
        if (saved.answers) Object.assign(answers, saved.answers)
        if (saved.multiAnswers) Object.assign(multiAnswers, saved.multiAnswers)
        if (saved.sortAnswers) Object.assign(sortAnswers, saved.sortAnswers)
        if (saved.matchAnswers) Object.assign(matchAnswers, saved.matchAnswers)
        if (saved.clozeAnswers) Object.assign(clozeAnswers, saved.clozeAnswers)
        if (saved.surveyAnswers) Object.assign(surveyAnswers, saved.surveyAnswers)
        if (saved.attachments) form.attachments = saved.attachments
        ElMessage.success('已恢复草稿')
        restored = true
      }
    }
  } catch { /* */ }
  // 云端不可用时尝试本地 localStorage 草稿
  if (!restored) {
    try {
      const local = localStorage.getItem(`draft_${props.taskId}`)
      if (local) {
        const saved = JSON.parse(local)
        if (saved && (saved.content || Object.keys(saved.answers || {}).length)) {
          try { await ElMessageBox.confirm('发现本地草稿，是否恢复？', '恢复草稿', { confirmButtonText: '恢复', cancelButtonText: '暂不恢复', type: 'info' }) } catch { return }
          const restore = (d) => { if (d.content) form.content = d.content; const targets = { answers, multiAnswers, sortAnswers, matchAnswers, clozeAnswers, surveyAnswers }; Object.entries(targets).forEach(([key, ref]) => { if (d[key]) Object.assign(ref, d[key]) }); if (d.attachments) form.attachments = d.attachments }
          restore(saved)
          ElMessage.success('本地草稿已恢复')
        }
      }
    } catch { /* */ }
  }
})

// 考试计时（基于 durationMinutes）
const examEndTime = ref(Number.MAX_SAFE_INTEGER)
const countdownText = ref('')
const isExpired = computed(() => {
  if (props.durationMinutes > 0 && props.examStarted) {
    if (examEndTime.value === Number.MAX_SAFE_INTEGER) return false // 计时器尚未启动
    return Date.now() >= examEndTime.value
  }
  return getDeadlineUrgency(props.deadline) === 'expired'
})
const isCritical = computed(() => {
  if (props.durationMinutes > 0 && props.examStarted) return examEndTime.value - Date.now() <= URGENT_THRESHOLD * 1000
  return getDeadlineUrgency(props.deadline) === 'critical'
})
const isUrgent = computed(() => {
  if (props.durationMinutes > 0 && props.examStarted) return examEndTime.value - Date.now() <= DEADLINE_URGENT_THRESHOLD * 1000
  const u = getDeadlineUrgency(props.deadline)
  return u === 'critical' || u === 'urgent'
})
const urgencyClass = computed(() => {
  if (isExpired.value) return 'is-expired'
  if (isUrgent.value) return 'is-urgent'
  return ''
})

// 监听 examStarted 启动考试计时
watch(() => props.examStarted, (v) => {
  if (v && props.durationMinutes > 0) {
    // E3: 优先从 localStorage 恢复开始时间（页面刷新场景），否则用当前时间
    const savedStart = localStorage.getItem(`exam_start_${props.taskId}`)
    const startTime = savedStart ? parseInt(savedStart, 10) : Date.now()
    examEndTime.value = startTime + props.durationMinutes * 60000
    updateCountdown()
  }
})

let timer = null
const updateCountdown = () => {
  const endTime = props.durationMinutes > 0 && props.examStarted ? examEndTime.value : (props.deadline ? new Date(props.deadline).getTime() : 0)
  if (!endTime) { countdownText.value = props.durationMinutes > 0 ? '考试未开始' : ''; return }
  const diff = endTime - Date.now()
  if (diff <= 0) { countdownText.value = props.durationMinutes > 0 ? '考试时间到' : '已截止'; return }
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  countdownText.value = props.durationMinutes > 0 ? `${h}小时${m}分${s}秒` : `${h}小时${m}分钟`
}

const startAdaptiveTimer = () => {
  const interval = isCritical.value ? 1000 : isUrgent.value ? 5000 : 30000
  timer = setInterval(() => { updateCountdown(); if (isCritical.value || isUrgent.value) { clearInterval(timer); startAdaptiveTimer() } }, interval)
}
onMounted(() => { updateCountdown(); startAdaptiveTimer() })
onUnmounted(() => { if (timer) clearInterval(timer) })

const parseOpts = (opts) => {
  if (!opts) return []
  try { return typeof opts === 'string' ? JSON.parse(opts) : opts } catch { return [] }
}

const FALLBACK_EXTS = '.jpg,.jpeg,.png,.gif,.bmp,.webp,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.rar,.zip,.mp3,.wav,.ogg,.m4a,.aac,.mp4,.webm,.mov,.avi,.mkv,.flv,.txt,.csv,.java,.py,.cpp,.c,.js,.ts,.html,.css,.md,.xml,.json'
const ALLOWED_EXTS = ref(FALLBACK_EXTS)
const beforeUpload = (file) => {
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!ALLOWED_EXTS.value.includes(ext)) { ElMessage.warning('不支持的文件格式：' + ext); return false }
  if (file.size > MAX_FILE_SIZE) { ElMessage.warning('文件不能超过10MB'); return false }
  return true
}
const isOnline = ref(navigator.onLine)
const onOnline = () => { isOnline.value = true; ElMessage.success('网络已恢复，可以继续作答'); autoSaveDraft(); flushPending() }
const onOffline = () => { isOnline.value = false }
// 打印拦截：考试模式下弹出警告
const onBeforePrint = () => {
  if (props.showAnswers && props.examStarted && !terminated.value) {
    ElMessage.warning('考试内容受防作弊保护，已对打印输出做模糊处理')
  }
}
onMounted(() => {
  window.addEventListener('online', onOnline)
  window.addEventListener('offline', onOffline)
  window.addEventListener('beforeprint', onBeforePrint)
})
onUnmounted(() => {
  window.removeEventListener('online', onOnline)
  window.removeEventListener('offline', onOffline)
  window.removeEventListener('beforeprint', onBeforePrint)
})

const uploadProgress = reactive({})
const onUploadProgress = (evt, file) => {
  uploadProgress[file.name] = Math.round((evt.loaded / evt.total) * 100)
}
const onUploadSuccess = (res, file) => {
  delete uploadProgress[file.name]
  const url = Array.isArray(res.data) ? res.data[0] : (res.data?.url || res.data)
  if (url) { form.attachments.push(url); ElMessage.success('文件上传成功') }
  else ElMessage.error('上传失败：未获取到文件地址')
}
const onUploadError = (err, file) => {
  delete uploadProgress[file.name]
  ElMessage.error('文件上传失败，请检查网络后重试')
}
const getFileName = (url) => { const p = (url || '').split('/'); return p[p.length - 1] || '文件' }
const removeFile = (i) => { form.attachments.splice(i, 1) }

// 初始化复杂题型的作答状态
watch(() => props.questions, (qs) => {
  if (!qs?.length) return
  qs.forEach(q => {
    if (q.optionMapping) optionMappings[q.id] = q.optionMapping
    if (q.questionType === 'DRAG_SORT') {
      const items = parseOpts(q.options)
      if (items.length && !sortAnswers[q.id]) sortAnswers[q.id] = [...items].sort(() => Math.random() - 0.5)
    }
    if (q.questionType === 'MATCHING') {
      if (!matchAnswers[q.id]) matchAnswers[q.id] = {}
    }
    if (q.questionType === 'CLOZE') {
      const blanks = (q.questionText.match(/_{3,}|【.+?】/g) || []).length
      if (!clozeAnswers[q.id]) clozeAnswers[q.id] = new Array(blanks).fill('')
    }
  })
}, { immediate: true })

const handleSubmit = async () => {
  if (submitting.value) return
  if (props.showAnswers) {
    try { await ElMessageBox.confirm('提交后不可再修改答案，确定提交试卷吗？', '确认提交', { confirmButtonText: '确定提交', cancelButtonText: '再检查一下', type: 'warning' }) } catch { return }
  }
  submitting.value = true
  try {
    const payload = { content: form.content, attachments: form.attachments }
    if (isSurvey.value) {
      const requiredMissed = surveyQuestions.value.filter(q => q.required && (!surveyAnswers[q.id] || (Array.isArray(surveyAnswers[q.id]) && !surveyAnswers[q.id].length)))
      if (requiredMissed.length) { ElMessage.warning(`请完成必填项：${requiredMissed.map(q => q.label).join('、')}`); submitting.value = false; return }
      payload.content = JSON.stringify(Object.fromEntries(
        Object.entries(surveyAnswers).map(([k, v]) => [k, v === undefined ? '' : v])
      ))
    }
    if (props.showAnswers) {
      const ans = {}
      // 单选：通过 optionMapping 将视觉字母转回原始字母
      // TRUE_FALSE 前端硬编码 A=正确/B=错误，与后端 options 打乱无关，须跳过映射
      Object.keys(answers).forEach(qid => {
        const v = answers[qid]
        const qType = props.questions?.find(q => String(q.id) === qid)?.questionType
        if (v && optionMappings[qid] && qType !== 'TRUE_FALSE') {
          const idx = v.charCodeAt(0) - 65  // A→0, B→1, ...
          if (idx >= 0 && idx < optionMappings[qid].length) {
            ans[qid] = String.fromCharCode(65 + optionMappings[qid][idx])
            return
          }
        }
        ans[qid] = v
      })
      // 多选：逐字母转换
      Object.keys(multiAnswers).forEach(qid => {
        const items = multiAnswers[qid]
        if (items?.length && optionMappings[qid]) {
          ans[qid] = items.map(v => {
            const idx = v.charCodeAt(0) - 65
            return idx >= 0 && idx < optionMappings[qid].length
              ? String.fromCharCode(65 + optionMappings[qid][idx]) : v
          }).sort().join(',')
        } else {
          ans[qid] = items?.sort().join(',') || ''
        }
      })
      Object.keys(sortAnswers).forEach(qid => { ans[qid] = sortAnswers[qid].join(',') })
      Object.keys(matchAnswers).forEach(qid => {
        const pairs = []
        Object.entries(matchAnswers[qid]).forEach(([k, v]) => { if (v) pairs.push(k + '-' + v) })
        ans[qid] = pairs.join(',')
      })
      Object.keys(clozeAnswers).forEach(qid => { ans[qid] = clozeAnswers[qid].join(',') })
      payload.answers = ans
    }
    emit('submit', payload)
    // submitting 由父组件通过 finishSubmit() 重置，防止竞态
  } catch (e) {
    /* */
    ElMessage.error('提交失败，请重试')
    submitting.value = false
  }
}

// 带重试的自动提交（用于切屏终止等场景）
const handleSubmitWithRetry = async (maxRetries = 3) => {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      await handleSubmit()
      return
    } catch {
      if (attempt < maxRetries - 1) {
        await new Promise(r => setTimeout(r, 1000))
        ElMessage.warning(`自动提交失败，正在重试(${attempt + 2}/${maxRetries})...`)
      } else {
        ElMessage.error('自动提交多次失败，请手动点击「提交任务」按钮或刷新页面后重试')
      }
    }
  }
}

// 暴露 clearDraft 供父组件提交成功后调用
const finishSubmit = () => { submitting.value = false }
defineExpose({ clearDraft: clearDraftFn, finishSubmit })
</script>

<style scoped>
/* D1: 默认 720 (作业/问卷)；考试模式 modifier 在 ≥1280 放宽 */
.submit-panel { max-width: 720px; margin: 0 auto; }
/* 固定顶部倒计时栏 — V2: 全部走 token；M1: safe-top + height 52 + 动画 1.2s 加大振幅 */
.fixed-timer-bar {
  position: fixed; top: var(--safe-top, 0px); left: 0; right: 0; z-index: 9999;
  height: 52px; min-height: 52px;
  display: flex; align-items: center; justify-content: center; gap: 10px;
  background: var(--el-color-danger); color: var(--el-color-white, #fff); font-weight: 700;
  box-shadow: var(--shadow-base);
}
.fixed-timer-text { font-size: 26px; font-family: 'Consolas', 'JetBrains Mono', monospace; letter-spacing: 2px; }
.fixed-timer-hint { font-size: var(--fs-sm); opacity: 0.9; }
.fixed-timer-bar.timer-flash { animation: timer-bar-flash 1.2s infinite; }
@keyframes timer-bar-flash {
  0%, 100% { background: var(--el-color-danger); opacity: 1; }
  50% { background: var(--el-color-warning); opacity: 0.2; }
}
.submit-panel { padding-top: 0; }
.fixed-timer-bar + .network-alert { margin-top: 60px; }
.network-alert { margin-bottom: 12px; }
.survey-mode { margin-bottom: 16px; }
.survey-progress { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 12px; font-weight: 500; }
.survey-q { margin-bottom: 20px; padding: 16px; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); }
.survey-q-label { font-weight: 600; color: var(--text-primary); margin-bottom: 10px; font-size: var(--fs-base); }
.survey-required { color: var(--el-color-danger); font-size: var(--fs-xs); margin-left: 6px; font-weight: 400; }
.survey-q-body { display: flex; flex-direction: column; gap: 6px; }
.countdown-bar {
  display: flex; align-items: center; gap: 6px; padding: 10px 16px;
  border-radius: var(--radius-md); margin-bottom: 16px;
  background: var(--bg-section); color: var(--text-regular); font-size: var(--fs-sm);
}
.countdown-bar.is-urgent { background: var(--bg-warning-light); color: var(--el-color-warning); }
.countdown-bar.is-expired { background: var(--bg-danger-light); color: var(--el-color-danger); }

.submit-form { margin-bottom: 10px; }
/* 死 CSS 清理：原 .answers-section / .answer-item / .q-title / .q-score / .q-options /
   .sort-list / .sort-row / .sort-idx / .sort-text / .match-row / .match-left / .match-arrow /
   .cloze-text / .cloze-blank / .cloze-label / .multi-banner / .q-opt-letter /
   .q-options--multi / .q-opt-letter--multi / :deep(.cloze-slot) 全部删除 —
   均为 ExamAnswerPanel 抽离前的残留，本组件 template 无引用 */
/* 强制所有radio/checkbox左对齐 */
.exam-mode :deep(.el-radio), .exam-mode :deep(.el-checkbox),
.submit-form :deep(.el-radio), .submit-form :deep(.el-checkbox) { justify-content: flex-start; text-align: left; }
.upload-hint { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.uploaded-files { margin-top: 12px; }
.uploaded-title { font-size: var(--fs-sm); font-weight: 500; color: var(--text-secondary); margin-bottom: 8px; }
.uploaded-item { margin-bottom: 8px; position: relative; }
.uploaded-item :deep(.el-button) { position: absolute; top: 4px; right: 4px; z-index: 1; }
.upload-progress-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.up-name { font-size: var(--fs-xs); color: var(--text-secondary); min-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.code-lang { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 4px; }
.code-input :deep(textarea) { font-family: 'Consolas','Monaco','Courier New',monospace; font-size: var(--fs-sm); }
.submit-actions { margin-top: 20px; display: flex; align-items: center; gap: 12px; }
/* D10: 提交按钮桌面全日均可用，移动端全宽 */
.submit-btn-main { width: 100%; }

/* D1+D3: 考试模式容器放宽 + 计时器字号放大（>=1280 viewport） */
@media (min-width: 1280px) {
  .submit-panel--exam {
    max-width: min(1280px, calc(100vw - 96px));
    --exam-timer-fs: 36px;
  }
}

/* 移动端 */
@media (max-width: 768px) {
  .submit-panel { padding: 0 4px; }
  /* D3: 移动端计时器字号仅考试容器内缩到 22px */
  .submit-panel--exam { --exam-timer-fs: 22px; }
  .countdown-bar { padding: 8px 10px; font-size: var(--fs-xs); }
  /* M5: 提交动作 + 草稿提示移动端纵向 */
  .submit-actions { flex-direction: column; align-items: stretch; }
  .submit-btn-main { width: 100%; }
}

/* 暗色模式适配 */
.dark {
  .el-button.is-disabled { opacity: 0.45; }
}
/* 打印防护：考试模式下禁止打印试卷内容 — D2 max-width:none 不影响此防护 */
@media print {
  .exam-protected { display: none !important; }
  .fixed-timer-bar { display: none !important; }
  .submit-panel { opacity: 0.15; filter: blur(4px); }
  .submit-panel::after {
    content: '考试内容受防作弊保护，不可打印';
    position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
    font-size: 48px; color: var(--el-color-danger); font-weight: 700; opacity: 1;
    white-space: nowrap; z-index: 99999;
  }
}
/* task submit panel内radio/checkbox强制左对齐 */
:deep(.el-radio), :deep(.el-checkbox) { justify-content: flex-start; text-align: left; }
:deep(.el-radio__label), :deep(.el-checkbox__label) { text-align: left; }
</style>
