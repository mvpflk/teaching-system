<template>
  <el-dialog
    v-model="visible"
    title="试卷赋分"
    width="560px"
    append-to-body
    @closed="$emit('close')"
  >
    <div v-if="questions.length" class="score-body">
      <el-alert type="info" :closable="false" style="margin-bottom:14px">
        共解析 <b>{{ questions.length }}</b> 道题，请为各题型赋分
      </el-alert>
      <div v-for="(count, type) in typeCounts" :key="type" class="score-type-row">
        <span class="st-label">{{ QUESTION_TYPE_LABEL[type] || type }} ({{ count }}题)</span>
        <template v-if="type === 'ESSAY'">
          <span class="st-hint">每题单独赋分：</span>
        </template>
        <template v-else>
          <span class="st-hint">每题</span>
          <el-input-number
            v-model="typeScores[type]"
            :min="1"
            :max="50"
            size="small"
            style="width:80px"
          />
          <span class="st-hint">分 x {{ count }} = {{ (typeScores[type]||0) * count }}分</span>
        </template>
      </div>
      <div v-if="typeCounts['ESSAY']" class="essay-scores">
        <div class="st-label mt-8">作答题逐题赋分：</div>
        <div v-for="q in questions.filter(p=>p.questionType==='ESSAY')" :key="q.id" class="essay-row">
          <span class="essay-text">{{ q.questionText }}</span>
          <el-input-number
            v-model="essayScores[q.id]"
            :min="1"
            :max="50"
            size="small"
            style="width:80px"
          /> 分
        </div>
      </div>
      <el-divider />
      <div class="score-total">总分：<b>{{ total }}</b> 分</div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirm">确认创建试卷</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'

const props = defineProps({
  modelValue: Boolean,
  questions: { type: Array, default: () => [] },
  typeCounts: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'close', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const typeScores = reactive({})
const essayScores = reactive({})

const total = computed(() => {
  let t = 0
  props.questions.forEach(q => {
    if (q.questionType === 'ESSAY') t += Number(essayScores[q.id] || 0)
    else t += Number(typeScores[q.questionType] || 0)
  })
  return t
})

watch(() => props.typeCounts, (tc) => {
  Object.keys(tc).forEach(key => {
    if (key !== 'ESSAY') typeScores[key] = 0
  })
}, { immediate: true })

watch(() => props.questions, (qs) => {
  Object.entries(props.typeCounts || {}).forEach(([t]) => {
    if (t === 'ESSAY') {
      qs.filter(q => q.questionType === 'ESSAY').forEach(q => { essayScores[q.id] = 10 })
    } else {
      typeScores[t] = t === 'SINGLE_CHOICE' ? 2 : t === 'MULTI_CHOICE' ? 3 : 1
    }
  })
}, { immediate: true })

const confirm = () => {
  const scores = {}
  props.questions.forEach(q => {
    if (q.questionType === 'ESSAY') scores[q.id] = essayScores[q.id] || 0
    else scores[q.id] = typeScores[q.questionType] || 0
  })
  emit('confirm', scores)
}
</script>

<style scoped>
.score-type-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.st-label { font-size: var(--fs-sm); font-weight: 500; min-width: 100px; }
.st-hint { font-size: var(--fs-xs); color: var(--text-secondary); }
.essay-scores { margin-top: 4px; }
.essay-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: var(--fs-sm); }
.essay-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 300px; }
.score-total { text-align: right; font-size: var(--fs-md); }
.mt-8 { margin-top: 8px; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
