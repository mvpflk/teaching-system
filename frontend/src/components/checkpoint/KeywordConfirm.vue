<template>
  <div class="kc-wrap">
    <div class="kc-header">
      <span class="kc-title">🔑 核心概念确认</span>
      <span class="kc-sub">回忆并填入本知识点最关键的术语（已跳过 {{ skippedCount }}/3）</span>
    </div>

    <div class="kc-form">
      <div
        v-for="(kw, idx) in keywords"
        :key="idx"
        class="kc-item"
        :class="{ 'kc-correct': corrects[idx], 'kc-skipped-item': skips[idx] }"
      >
        <div class="kc-left">
          <label class="kc-label">{{ kw.term }}</label>
          <span v-if="kw.context" class="kc-context" :title="kw.context">{{ truncateContext(kw.context) }}</span>
        </div>
        <input
          v-model="answers[idx]"
          class="kc-input"
          :class="{ 'is-error': errors[idx], 'is-correct': corrects[idx], 'is-skipped': skips[idx] }"
          :disabled="corrects[idx] || skips[idx]"
          :placeholder="'填入 ' + (kw.blank || kw.term)"
          @keyup.enter="handleSubmit"
        />
        <span v-if="corrects[idx]" class="kc-icon kc-ok" @click="showFollowup(idx)">✓</span>
        <span
          v-if="corrects[idx] && kw.followup"
          class="kc-followup-dot"
          title="追问"
          @click="showFollowup(idx)"
        >❓</span>
        <span v-if="skips[idx]" class="kc-icon kc-skipped">跳过</span>
        <button
          v-if="!corrects[idx] && !skips[idx] && skippedCount < 3"
          class="kc-skip-btn"
          @click="handleSkip(idx)"
        >
          跳过(-1分)
        </button>
        <span v-if="errors[idx] && totalErrors[idx] >= 3" class="kc-hint-answer">答案：{{ kw.term }}</span>
      </div>
    </div>

    <div class="kc-actions">
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="allAnswered"
        @click="handleSubmit"
      >
        {{ allAnswered ? '已全部通过' : '提交确认' }}
      </el-button>
      <el-button
        v-if="attempts >= 5"
        plain
        type="warning"
        @click="$emit('sos')"
      >
        求助教师
      </el-button>
      <el-button v-if="attempts >= 5 && !allAnswered" plain @click="$emit('paypass')">扣1分直接过关</el-button>
    </div>

    <div v-if="submitMessage" class="kc-msg" :class="submitAllCorrect ? 'kc-msg-ok' : 'kc-msg-err'">
      {{ submitMessage }}
    </div>

    <!-- 追问弹窗 -->
    <el-dialog
      v-model="followupVisible"
      title="追问"
      width="380px"
      :close-on-click-modal="false"
    >
      <div v-if="currentFollowup" class="kc-followup-dialog">
        <p class="kc-followup-q">{{ currentFollowup.question }}</p>
        <el-radio-group v-model="followupAnswer" class="kc-followup-options">
          <el-radio v-for="(opt, i) in currentFollowup.options" :key="i" :value="optLabel(i)">{{ optLabel(i) }}. {{ opt }}</el-radio>
        </el-radio-group>
        <div class="kc-followup-actions">
          <el-button type="primary" size="small" @click="submitFollowup">确认</el-button>
        </div>
        <div v-if="followupResult !== null" class="kc-followup-result" :class="followupResult ? 'kc-fr-ok' : 'kc-fr-err'">
          {{ followupResult ? '回答正确！' : ('答案是 ' + currentFollowup.answer + (currentFollowup.tip ? '（提示：' + currentFollowup.tip + '）' : '')) }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  keywords: { type: Array, default: () => [] },
  attempts: { type: Number, default: 0 }
})

const emit = defineEmits(['verify', 'skip', 'sos', 'paypass', 'followup'])

const answers = ref([])
const errors = ref([])
const corrects = ref([])
const skips = ref([])
const totalErrors = ref([])
const submitMessage = ref('')
const submitAllCorrect = ref(false)
const submitting = ref(false)

const followupVisible = ref(false)
const currentFollowup = ref(null)
const currentFollowupIdx = ref(-1)
const followupAnswer = ref('')
const followupResult = ref(null)

watch(() => props.keywords, (kws) => {
  answers.value = kws.map(() => '')
  errors.value = kws.map(() => false)
  corrects.value = kws.map(() => false)
  skips.value = kws.map(() => false)
  totalErrors.value = kws.map(() => 0)
}, { immediate: true })

const skippedCount = computed(() => skips.value.filter(Boolean).length)
const allAnswered = computed(() => props.keywords.every((_, i) => corrects.value[i] || skips.value[i]))

function optLabel(i) { return String.fromCharCode(65 + i) }
function truncateContext(ctx) {
  if (!ctx) return ''
  return ctx.length > 30 ? ctx.substring(0, 30) + '…' : ctx
}

function handleSkip(idx) {
  skips.value[idx] = true
  emit('skip', idx)
}

function showFollowup(idx) {
  const kw = props.keywords[idx]
  if (!kw || !kw.followup) return
  currentFollowup.value = kw.followup
  currentFollowupIdx.value = idx
  followupAnswer.value = ''
  followupResult.value = null
  followupVisible.value = true
}

function submitFollowup() {
  if (!currentFollowup.value) return
  const correct = followupAnswer.value === currentFollowup.value.answer
  followupResult.value = correct
  emit('followup', { keywordIndex: currentFollowupIdx.value, correct })
}

async function handleSubmit() {
  submitting.value = true
  const payload = answers.value.map((v, i) => ({ index: i, value: v, term: props.keywords[i].term, acceptAliases: props.keywords[i].acceptAliases || [] }))
  emit('verify', payload, (results) => {
    const newErrors = props.keywords.map(() => false)
    const newCorrects = [...corrects.value]
    const newTotalErrors = [...totalErrors.value]
    let allOk = true
    results.forEach(r => {
      if (r.correct) {
        newCorrects[r.index] = true
      } else if (!skips.value[r.index]) {
        newErrors[r.index] = true
        if (r.totalErrors) newTotalErrors[r.index] = r.totalErrors
        allOk = false
      }
    })
    errors.value = newErrors
    corrects.value = newCorrects
    totalErrors.value = newTotalErrors

    if (allOk) {
      submitMessage.value = '全部正确！'
      submitAllCorrect.value = true
    } else {
      submitMessage.value = '部分错误，请重新填写标红的词'
      submitAllCorrect.value = false
    }
    submitting.value = false
  })
}

defineExpose({ answers, errors, corrects, skips })
</script>

<style scoped>
.kc-wrap {
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-md); margin-top: var(--spacing-md);
}
.kc-header { margin-bottom: var(--spacing-md); }
.kc-title { font-weight: 600; font-size: var(--fs-lg); color: var(--primary-color); }
.kc-sub { margin-left: 8px; font-size: var(--fs-xs); color: var(--text-secondary); }
.kc-form { display: flex; flex-direction: column; gap: 10px; }
.kc-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: var(--radius-sm);
  background: var(--bg-section); position: relative; flex-wrap: wrap;
}
.kc-skipped-item { opacity: 0.55; }
.kc-correct { background: rgba(0, 180, 42, 0.06); }
.kc-left { display: flex; flex-direction: column; min-width: 80px; }
.kc-label { font-size: var(--fs-sm); color: var(--text-primary); font-weight: 600; }
.kc-context {
  font-size: var(--fs-xs); color: var(--text-secondary); line-height: 1.3;
  max-width: 120px; overflow: hidden; text-overflow: ellipsis;
  white-space: nowrap;
}
.kc-input {
  flex: 1; height: 34px; padding: 0 10px;
  border: 1px solid var(--border-input); border-radius: var(--radius-sm);
  font-size: var(--fs-base); background: #fff; outline: none;
}
.kc-input:focus { border-color: var(--primary-color); }
.kc-input.is-error { border-color: #e74c3c; animation: shake 0.3s; }
.kc-input.is-correct { border-color: #00b42a; }
.kc-input.is-skipped { border-color: var(--border-color); background: var(--bg-section); }
.kc-icon { font-size: var(--fs-md); font-weight: 600; min-width: 18px; }
.kc-ok { color: var(--primary-color); cursor: default; }
.kc-followup-dot { font-size: var(--fs-xs); cursor: pointer; opacity: 0.7; }
.kc-followup-dot:hover { opacity: 1; }
.kc-skipped { color: var(--text-secondary); font-size: var(--fs-xs); }
.kc-skip-btn {
  font-size: var(--fs-xs); color: var(--text-secondary); border: none;
  background: none; cursor: pointer; white-space: nowrap;
}
.kc-hint-answer {
  font-size: var(--fs-xs); color: #e74c3c; width: 100%; margin-left: 88px; font-weight: 600;
}
.kc-actions { margin-top: var(--spacing-md); display: flex; gap: 8px; flex-wrap: wrap; }
.kc-msg { margin-top: 8px; font-size: var(--fs-sm); }
.kc-msg-ok { color: #00b42a; }
.kc-msg-err { color: #e74c3c; }

.kc-followup-dialog { }
.kc-followup-q { font-size: var(--fs-base); font-weight: 500; color: var(--text-primary); margin-bottom: var(--spacing-md); line-height: 1.6; }
.kc-followup-options { display: flex; flex-direction: column; gap: 8px; }
.kc-followup-actions { margin-top: var(--spacing-md); }
.kc-followup-result { margin-top: var(--spacing-md); font-size: var(--fs-sm); padding: 8px; border-radius: var(--radius-sm); }
.kc-fr-ok { background: rgba(0, 180, 42, 0.06); color: #00b42a; }
.kc-fr-err { background: rgba(231, 76, 60, 0.04); color: #e74c3c; }

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}
</style>
