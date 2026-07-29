<template>
  <div>
    <div class="diag-filter-bar">
      <el-radio-group :model-value="questionFilter" size="small" @change="$emit('filter-change', $event)">
        <el-radio-button value="all">全部 ({{ questions.length }})</el-radio-button>
        <el-radio-button value="weak">薄弱题 &lt;70% ({{ weakQuestionCount }})</el-radio-button>
        <el-radio-button value="danger">急需关注 &lt;50%</el-radio-button>
      </el-radio-group>
      <span v-if="kpQuestionFilter" style="display:inline-flex;align-items:center;gap:4px">
        <el-tag size="small" type="warning" closable @close="$emit('clear-kp-filter')">
          📌 {{ activeKpFilterName }}
        </el-tag>
      </span>
      <span v-else class="diag-filter-hint">按正确率升序排列，最弱的排前面</span>
    </div>

    <div
      v-for="q in questions"
      :key="q.qId"
      class="diag-q-item"
      :class="questionRowClass(q)"
    >
      <div class="diag-q-head" @click="$emit('toggle-expand', q.qId)">
        <div class="diag-q-head-left">
          <el-icon class="diag-q-arrow" :class="{expanded: expandedQuestions.has(q.qId)}"><ArrowRight /></el-icon>
          <span class="diag-q-idx">Q{{ q._idx }}</span>
          <el-tag size="small" effect="plain">{{ QUESTION_TYPE_LABEL[q.questionType] || q.questionType }}</el-tag>
          <span class="diag-q-rate" :style="{color:rateColor(q._rate)}">{{ q._rate }}%</span>
          <el-tag v-if="q._rate < 50" type="danger" size="small" effect="dark">需关注</el-tag>
          <el-tag v-else-if="q._rate < 70" type="warning" size="small" effect="dark">薄弱</el-tag>
        </div>
        <div class="diag-q-head-right">
          <span
            v-for="c in q._classes"
            :key="c.classId"
            class="diag-q-cls-tag"
            :style="{color:rateColor(c.correctRate)}"
          >
            {{ getClassName(c.classId, classes) }} {{ c.correctRate }}%
          </span>
          <el-icon class="diag-q-expand-icon"><ArrowDown /></el-icon>
        </div>
      </div>
      <QuestionDetailRow :question="q" :expanded="expandedQuestions.has(q.qId)" />
    </div>
    <el-empty v-if="!questions.length" description="暂无数据" />
  </div>
</template>

<script setup>
import { ArrowRight, ArrowDown } from '@element-plus/icons-vue'
import QuestionDetailRow from './QuestionDetailRow.vue'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'

const props = defineProps({
  questions: { type: Array, default: () => [] },
  weakQuestionCount: { type: Number, default: 0 },
  kpQuestionFilter: { type: [Number, null], default: null },
  activeKpFilterName: { type: String, default: '' },
  questionFilter: { type: String, default: 'all' },
  expandedQuestions: { type: Set, default: () => new Set() },
  classes: { type: Array, default: () => [] }
})
defineEmits(['filter-change', 'toggle-expand', 'clear-kp-filter'])

function rateColor(r) {
  return r >= 80 ? 'var(--el-color-success, #67c23a)' : r >= 60 ? 'var(--el-color-primary, #409eff)' : r >= 40 ? 'var(--el-color-warning, #e6a23c)' : 'var(--el-color-danger, #f56c6c)'
}

function questionRowClass(q) {
  return q._rate < 50 ? 'diag-q--danger' : q._rate < 70 ? 'diag-q--weak' : ''
}

function getClassName(cid) {
  return props.classes.find(c => c.classId === cid)?.className || ('班级' + cid)
}
</script>

<style scoped>
.diag-filter-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.diag-filter-hint { font-size: var(--fs-xs); color: var(--text-secondary); }
.diag-q-item { border: 0.5px solid var(--border-base); border-radius: 6px; margin-bottom: 6px; overflow: hidden; transition: background 0.15s; }
.diag-q--weak { border-left: 3px solid var(--warning-color); }
.diag-q--danger { border-left: 3px solid var(--danger-color); background: var(--el-color-danger-light-9, #fef0f0); }
.diag-q-head { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; cursor: pointer; user-select: none; }
.diag-q-head:hover { background: var(--bg-hover); }
.diag-q-head-left { display: flex; align-items: center; gap: 8px; flex: 1; }
.diag-q-head-right { display: flex; align-items: center; gap: 10px; }
.diag-q-arrow { transition: transform 0.2s; color: var(--text-disabled); font-size: var(--fs-xs); }
.diag-q-arrow.expanded { transform: rotate(90deg); }
.diag-q-idx { font-weight: 700; color: var(--primary-color); min-width: 30px; }
.diag-q-rate { font-weight: 700; font-size: var(--fs-md); min-width: 48px; }
.diag-q-cls-tag { font-size: var(--fs-xs); padding: 1px 6px; background: var(--bg-secondary); border-radius: 4px; }
.diag-q-expand-icon { color: var(--text-disabled); font-size: var(--fs-xs); margin-left: 4px; }

@media (max-width: 768px) {
  .diag-filter-bar { gap: 8px; }
  .diag-q-head-right { gap: 6px; flex-wrap: wrap; }
  .diag-q-cls-tag { font-size: 10px; }
}
</style>
