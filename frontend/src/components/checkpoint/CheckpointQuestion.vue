<template>
  <div
    class="cq-wrap"
    :class="{
      'cq-answered': hasAnswered,
      'cq-correct': showResult && resultCorrect === true,
      'cq-wrong': showResult && resultCorrect === false,
    }"
  >
    <div class="cq-type-tag">{{ typeLabel }}</div>
    <div class="cq-text" v-html="renderMath(question.questionText)"></div>

    <!-- 选择题/判断题 -->
    <div v-if="isChoice" class="cq-options">
      <div
        v-for="(opt, i) in parsedOptions"
        :key="i"
        class="cq-opt"
        :class="{
          'cq-selected': selected === label(i),
          'cq-correct-opt': showResult && label(i) === correctLabel,
          'cq-wrong-opt': showResult && selected === label(i) && label(i) !== correctLabel,
        }"
        @click="select(label(i))"
      >
        <span class="cq-label">{{ label(i) }}</span>
        <span class="cq-opt-text" v-html="renderMath(opt)"></span>
      </div>
    </div>

    <!-- 填空题 -->
    <div v-else class="cq-fill">
      <el-input
        v-model="fillAnswer"
        placeholder="请输入答案"
        :disabled="hasAnswered"
        @keyup.enter="submit"
      />
    </div>

    <!-- 提交按钮 -->
    <div v-if="!hasAnswered" class="cq-footer">
      <el-button
        v-if="selected || fillAnswer"
        type="primary"
        size="small"
        @click="submit"
      >
        提交答案
      </el-button>
    </div>

    <!-- 已提交状态 -->
    <div v-if="hasAnswered && !showResult" class="cq-result">✓ 已提交</div>

    <!-- 最终判定结果 -->
    <div
      v-if="showResult"
      class="cq-verdict"
      :class="resultCorrect ? 'cq-verdict-ok' : 'cq-verdict-err'"
    >
      <template v-if="resultCorrect === true">✅ 正确</template>
      <template v-else-if="resultCorrect === false">
        ❌ 错误 · 正确答案：<strong
          v-html="renderMath(correctLabel || question.correctAnswer)"
        ></strong>
        <span v-if="question.explanation" class="cq-explanation">{{ question.explanation }}</span>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import katex from 'katex';
import 'katex/dist/katex.min.css';

function renderMath(text) {
  if (!text) return '';
  let html = text;
  html = html.replace(/\$\$([^$]+)\$\$/g, (_, f) => {
    try {
      return katex.renderToString(f.trim(), { displayMode: true, throwOnError: false });
    } catch {
      return _;
    }
  });
  html = html.replace(/\$([^$]+)\$/g, (_, f) => {
    try {
      return katex.renderToString(f.trim(), { displayMode: false, throwOnError: false });
    } catch {
      return _;
    }
  });
  return html;
}

const props = defineProps({
  question: { type: Object, default: () => ({}) },
  showResult: { type: Boolean, default: false },
  resultCorrect: { type: Boolean, default: null },
  correctLabel: { type: String, default: '' },
});
const emit = defineEmits(['submit']);

const selected = ref('');
const fillAnswer = ref('');
const hasAnswered = ref(false);

const isChoice = computed(() => {
  const t = props.question.questionType;
  return t === 'SINGLE_CHOICE' || t === 'MULTI_CHOICE' || t === 'TRUE_FALSE';
});

const typeLabel = computed(() => {
  const map = {
    SINGLE_CHOICE: '单选题',
    MULTI_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_IN: '填空题',
    FILL_IN_BLANK: '填空题',
    FILL_BLANK: '填空题',
  };
  return map[props.question.questionType] || props.question.questionType || '';
});

const parsedOptions = computed(() => {
  try {
    if (typeof props.question.options === 'string') return JSON.parse(props.question.options);
    if (Array.isArray(props.question.options)) return props.question.options;
    return [];
  } catch {
    return [];
  }
});

function label(i) {
  return String.fromCharCode(65 + i);
}

function select(l) {
  if (hasAnswered.value) return;
  selected.value = l;
}

function submit() {
  if (hasAnswered.value) return;
  hasAnswered.value = true;
  emit('submit', {
    questionId: props.question.id,
    questionType: props.question.questionType,
    answer: selected.value || fillAnswer.value,
  });
}
</script>

<style scoped>
.cq-wrap {
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  transition: border-color 0.3s;
  position: relative;
}
.cq-answered {
  border-color: var(--primary-color);
}
.cq-correct {
  border-color: #00b42a;
  background: rgba(0, 180, 42, 0.02);
}
.cq-wrong {
  border-color: #e74c3c;
  background: rgba(231, 76, 60, 0.02);
}
.cq-type-tag {
  display: inline-block;
  font-size: var(--fs-xs);
  font-weight: 500;
  padding: 1px 8px;
  border-radius: 999px;
  margin-bottom: 8px;
  background: var(--primary-light);
  color: var(--primary-color);
}
.cq-text {
  font-size: var(--fs-base);
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
  line-height: 1.6;
  font-weight: 500;
}
.cq-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cq-opt {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  border: 0.5px solid var(--border-color);
  cursor: pointer;
  transition: background var(--transition-base);
}
.cq-opt:hover:not(.cq-correct-opt):not(.cq-wrong-opt) {
  background: var(--bg-section);
}
.cq-selected {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.cq-correct-opt {
  border-color: #00b42a;
  background: rgba(0, 180, 42, 0.06);
}
.cq-wrong-opt {
  border-color: #e74c3c;
  background: rgba(231, 76, 60, 0.04);
}
.cq-label {
  font-weight: 600;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  min-width: 20px;
}
.cq-opt-text {
  font-size: var(--fs-base);
  color: var(--text-primary);
}
.cq-text :deep(.katex),
.cq-opt-text :deep(.katex) {
  font-size: 1.05em;
}
.cq-fill {
  margin: var(--spacing-md) 0;
}
.cq-footer {
  margin-top: var(--spacing-md);
  display: flex;
  gap: 8px;
}
.cq-result {
  margin-top: 8px;
  font-size: var(--fs-sm);
  color: var(--primary-color);
  font-weight: 500;
}
.cq-verdict {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
  font-weight: 600;
}
.cq-verdict-ok {
  background: rgba(0, 180, 42, 0.06);
  color: #00b42a;
}
.cq-verdict-err {
  background: rgba(231, 76, 60, 0.04);
  color: #e74c3c;
}
.cq-explanation {
  display: block;
  margin-top: 4px;
  font-size: var(--fs-xs);
  font-weight: 400;
  color: var(--text-secondary);
}
</style>
