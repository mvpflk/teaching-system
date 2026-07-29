<template>
  <el-dialog
    :model-value="modelValue"
    title="试卷预览"
    width="800px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @open="scrollToTop"
  >
    <div v-if="!questions.length" class="preview-empty">
      <el-empty description="试题篮为空，请先选题" />
    </div>
    <div v-else class="preview-body">
      <!-- 试卷头 -->
      <div class="preview-header">
        <div class="preview-header__line">
          <span>姓名：____________</span>
          <span>班级：____________</span>
          <span>得分：____________</span>
        </div>
        <h2 class="preview-header__title">{{ title }}</h2>
        <div class="preview-header__info">
          共 <b>{{ questions.length }}</b> 题 · 满分 <b>{{ totalScore }}</b> 分 · 时间 {{ durationMinutes }} 分钟
        </div>
      </div>

      <!-- 按题型分组展示 -->
      <template v-for="(group, gIdx) in groupedQuestions" :key="gIdx">
        <div class="preview-section">
          <h3 class="preview-section__title">
            {{ group.label }}（共 {{ group.questions.length }} 题，每题 {{ group.score }} 分，计 {{ group.subTotal }} 分）
          </h3>
          <div v-for="(q, qIdx) in group.questions" :key="q.id" class="preview-q">
            <div class="preview-q__num">{{ qIdx + 1 }}.</div>
            <div class="preview-q__body">
              <div class="preview-q__text" v-html="renderQText(q)" />
              <!-- 选项 -->
              <div v-if="hasOptions(q)" class="preview-q__opts">
                <div v-for="(opt, oi) in parseOpts(q)" :key="oi" class="preview-q__opt">
                  {{ String.fromCharCode(65 + oi) }}. {{ opt }}
                </div>
              </div>
              <!-- 填空/简答作答区 -->
              <div v-if="needsBlank(q)" class="preview-q__blank">
                <div v-for="n in blankCount(q)" :key="n" class="preview-q__blank-line">
                  {{ n }}. _________________________________
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" @click="$emit('export-word')">导出 Word</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { sanitizeHtml } from '@/utils/markdown'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'

const props = defineProps({
  modelValue: Boolean,
  questions: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  durationMinutes: { type: Number, default: 60 },
})
defineEmits(['update:modelValue', 'export-word'])

const DEFAULT_SCORES = { SINGLE_CHOICE: 2, MULTI_CHOICE: 3, TRUE_FALSE: 1, FILL_IN: 1, SHORT_ANSWER: 5, PROGRAMMING: 10, CLOZE: 2, ESSAY: 20, MATCHING: 3, DRAG_SORT: 3, COMPOSITE: 10 }
const TYPE_ORDER = ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'FILL_IN', 'SHORT_ANSWER', 'CLOZE', 'PROGRAMMING', 'MATCHING', 'DRAG_SORT', 'ESSAY', 'COMPOSITE']

const totalScore = computed(() => {
  let s = 0
  for (const q of props.questions) {
    s += DEFAULT_SCORES[q.questionType] || 5
  }
  return s
})

const groupedQuestions = computed(() => {
  const groups = {}
  for (const q of props.questions) {
    const t = q.questionType || 'UNKNOWN'
    if (!groups[t]) groups[t] = []
    groups[t].push(q)
  }
  // 按约定题型顺序排列
  const result = []
  for (const t of TYPE_ORDER) {
    if (groups[t] && groups[t].length) {
      const score = DEFAULT_SCORES[t] || 5
      result.push({
        type: t,
        label: QUESTION_TYPE_LABEL[t] || t,
        questions: groups[t],
        score,
        subTotal: groups[t].length * score,
      })
    }
  }
  // 未排序的题型放最后
  for (const t of Object.keys(groups)) {
    if (!TYPE_ORDER.includes(t)) {
      const score = DEFAULT_SCORES[t] || 5
      result.push({ type: t, label: QUESTION_TYPE_LABEL[t] || t, questions: groups[t], score, subTotal: groups[t].length * score })
    }
  }
  return result
})

const renderQText = (q) => sanitizeHtml((q.questionText || '').replace(/\n/g, '<br>'))

const hasOptions = (q) => ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'MATCHING'].includes(q.questionType)

const parseOpts = (q) => {
  try {
    const raw = q.options
    const parsed = Array.isArray(raw) ? raw : (typeof raw === 'string' ? JSON.parse(raw || '[]') : [])
    // 兼容两种格式：["文本"] 和 [{"key":"A","text":"文本"}]
    return parsed.map(o => {
      if (typeof o === 'string') return o
      if (o && typeof o === 'object') return o.text || o.label || o.value || String(o)
      return String(o)
    })
  } catch { return [] }
}

const needsBlank = (q) => ['FILL_IN', 'SHORT_ANSWER'].includes(q.questionType)
const blankCount = (q) => {
  if (q.questionType === 'FILL_IN') {
    // 统计题干中下划线或括号数量
    const matches = q.questionText?.match(/[（(]|_/g)
    return Math.max(1, matches ? Math.floor(matches.length / 2) : 1)
  }
  return 1
}

const scrollToTop = () => {
  setTimeout(() => {
    const el = document.querySelector('.preview-body')
    if (el) el.scrollTop = 0
  }, 100)
}
</script>

<style scoped>
.preview-body { max-height: 65vh; overflow-y: auto; padding: 20px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); }
.preview-header { text-align: center; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 2px solid var(--text-primary); }
.preview-header__line { display: flex; gap: 32px; font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 16px; justify-content: center; }
.preview-header__title { font-size: var(--fs-xl); font-weight: 700; margin: 0 0 8px; color: var(--text-primary); }
.preview-header__info { font-size: var(--fs-sm); color: var(--text-secondary); }
.preview-section { margin-bottom: 20px; }
.preview-section__title { font-size: var(--fs-md); font-weight: 600; margin: 0 0 10px; color: var(--text-primary); }
.preview-q { display: flex; gap: 8px; margin-bottom: 14px; padding: 8px 0; }
.preview-q__num { font-weight: 700; color: var(--primary-color); min-width: 28px; text-align: right; }
.preview-q__body { flex: 1; min-width: 0; }
.preview-q__text { font-size: var(--fs-sm); line-height: 1.8; color: var(--text-primary); margin-bottom: 6px; }
.preview-q__opts { display: flex; flex-wrap: wrap; gap: 4px 24px; margin-top: 6px; }
.preview-q__opt { font-size: var(--fs-sm); color: var(--text-regular); min-width: 120px; }
.preview-q__blank { margin-top: 8px; }
.preview-q__blank-line { font-size: var(--fs-sm); color: var(--text-secondary); line-height: 2; }
.preview-empty { padding: 60px 0; }
</style>
