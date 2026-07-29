<template>
  <div v-show="expanded" class="diag-q-body">
    <QuestionRenderer
      :question="question"
      mode="display"
      :show-answer="true"
      :show-meta="false"
    />
    <div class="diag-q-meta">
      <span class="diag-q-meta-label">题型：</span>
      <span class="diag-q-meta-val">{{
        QUESTION_TYPE_LABEL[question.questionType] || question.questionType || '—'
      }}</span>
      <span class="diag-q-meta-label" style="margin-left: 16px">关联知识点：</span>
      <el-tag
        size="small"
        :type="question._mappingQuality === 'estimated' ? 'warning' : 'info'"
        effect="plain"
      >
        {{ question.kpName || '—' }}
      </el-tag>
      <span
        v-if="question._mappingQuality === 'estimated'"
        style="color: var(--color-warning, #e6a23c); font-size: var(--fs-xs); margin-left: 2px"
      >（算法推测）</span>
    </div>
    <div v-if="question.topWrongAnswers?.length" class="diag-q-wrong">
      <span class="diag-q-meta-label">高频错答：</span>
      <span
        v-for="(w, wi) in question.topWrongAnswers"
        :key="wi"
        style="margin-right: 6px; font-size: var(--fs-xs); color: var(--el-color-danger, #f56c6c)"
      >{{ w }}</span>
    </div>
    <div class="diag-q-bars">
      <div v-for="c in question._classes" :key="c.classId" class="diag-q-bar-row">
        <span class="diag-q-bar-cls">{{ getClassName(c.classId) }}</span>
        <div class="diag-q-bar-track">
          <div
            class="diag-q-bar-fill"
            :style="{
              width: Math.max(c.correctRate, 2) + '%',
              background: rateColor(c.correctRate),
            }"
          />
        </div>
        <span class="diag-q-bar-val">{{ c.correctRate }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';

const props = defineProps({
  question: { type: Object, default: () => ({}) },
  expanded: { type: Boolean, default: false },
});

function getOptionLetter(idx) {
  return String.fromCharCode(65 + idx);
}

function stripOptionPrefix(opt) {
  if (!opt) return '';
  return opt.replace(/^[A-Za-z]\s*[.、．)）:：]\s*/, '');
}

function rateColor(r) {
  return r >= 80
    ? 'var(--el-color-success, #67c23a)'
    : r >= 60
      ? 'var(--el-color-primary, #409eff)'
      : r >= 40
        ? 'var(--el-color-warning, #e6a23c)'
        : 'var(--el-color-danger, #f56c6c)';
}

function getClassName(cid) {
  const cls = props.question._classes || [];
  // class name is embedded in _classes entries
  return cls.find((c) => c.classId === cid)?.className || '班级' + cid;
}
</script>

<style scoped>
.diag-q-body {
  padding: 0 12px 12px 44px;
  overflow: hidden;
  transition:
    max-height 0.25s ease-out,
    opacity 0.2s ease-out;
}
.diag-q-stem {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  line-height: 1.6;
  margin: 0 0 10px;
}
.diag-q-meta {
  font-size: var(--fs-xs);
  margin-bottom: 6px;
}
.diag-q-meta-label {
  color: var(--text-secondary);
}
.diag-q-meta-val {
  color: var(--success-color);
  font-weight: 500;
}
.diag-q-wrong {
  margin-bottom: 8px;
}
.diag-q-bars {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.diag-q-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-xs);
}
.diag-q-bar-cls {
  min-width: 80px;
  color: var(--text-regular);
}
.diag-q-bar-track {
  flex: 1;
  height: 10px;
  background: var(--bg-secondary);
  border-radius: 5px;
  overflow: hidden;
}
.diag-q-bar-fill {
  height: 100%;
  border-radius: 5px;
  min-width: 2px;
  transition: width 0.3s;
}
.diag-q-bar-val {
  min-width: 40px;
  text-align: right;
  color: var(--text-primary);
  font-weight: 500;
}
</style>
