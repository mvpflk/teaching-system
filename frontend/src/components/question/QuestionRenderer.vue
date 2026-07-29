<template>
  <div class="question-renderer" :class="[`qr--${mode}`, `qr--${size}`]">
    <!-- 元信息：题型 + 难度 + 来源 + 分类路径 -->
    <div v-if="showMeta && mode === 'display'" class="qr__meta">
      <el-tag size="small">{{ typeLabel }}</el-tag>
      <el-tag
        v-if="question.difficultyLevel"
        size="small"
        :type="diffTagType"
        effect="plain"
      >
        {{ diffLabel }}
      </el-tag>
      <el-tag v-if="question.tier" size="small" effect="plain">
        {{
          TIER_LABEL[question.tier]
        }}
      </el-tag>
      <el-tag
        v-if="question.source"
        size="small"
        type="info"
        effect="plain"
      >
        {{
          sourceLabel
        }}
      </el-tag>
      <span v-if="categoryPath" class="qr__cat">{{ categoryPath }}</span>
    </div>

    <!-- 题干 -->
    <div class="qr__text" v-html="renderedText" />

    <!-- 选项（仅选择题型） -->
    <template v-if="isChoiceType">
      <div class="qr__options" :class="{ 'qr__options--multi': isMultiType }">
        <div
          v-for="(opt, idx) in parsedOptions"
          :key="idx"
          class="qr__option"
          :class="{
            'qr__option--correct': highlightCorrect && isCorrectOption(opt, idx),
          }"
        >
          <span class="qr__opt-key">{{ opt.key }}.</span>
          <span class="qr__opt-text" v-html="renderMath(opt.text)" />
        </div>
      </div>
    </template>

    <!-- 填空题型（display 模式下显示占位文本） -->
    <div v-else-if="question.questionType === 'FILL_IN'" class="qr__fill">
      <span v-html="renderedFill" />
    </div>

    <!-- 答案 -->
    <div v-if="showAnswer && question.correctAnswer" class="qr__answer">
      <b>答案：</b><span v-html="renderMath(question.correctAnswer)" />
    </div>

    <!-- 解析 -->
    <div v-if="showExplanation && question.explanation" class="qr__explanation">
      <b>解析：</b><span v-html="renderMath(question.explanation)" />
    </div>

    <!-- 图片附件 -->
    <div v-if="renderedImages" class="qr__images" v-html="renderedImages" />

    <!-- 文件附件 -->
    <div v-if="question.attachmentUrl" class="qr__attachment">
      <FilePreview :src="question.attachmentUrl" :filename="attachmentName" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import {
  renderMath,
  parseOptions,
  isChoiceType,
  isMultiType,
} from '@/composables/useQuestionHelpers';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import { isCorrectOption } from '@/utils/question';
import FilePreview from '@/components/renderers/FilePreview.vue';

const props = defineProps({
  question: { type: Object, required: true },
  mode: { type: String, default: 'display' },
  size: { type: String, default: 'default' },
  showAnswer: { type: Boolean, default: false },
  showExplanation: { type: Boolean, default: false },
  showMeta: { type: Boolean, default: true },
  highlightCorrect: { type: Boolean, default: false },
  categoryPath: { type: String, default: '' },
});

const TIER_LABEL = { BASIC: '合格', MEDIUM: '良好', ADVANCED: '优秀' };
const SOURCE_LABEL = {
  MANUAL: '手动',
  AI: 'AI',
  WORD_IMPORT: 'Word',
  EXCEL_IMPORT: 'Excel',
  PAPER_IMPORT: '整卷',
};
const DIFF = {
  1: { label: '基础', tag: 'success' },
  2: { label: '中等', tag: 'warning' },
  3: { label: '进阶', tag: 'danger' },
};

const typeLabel = computed(
  () => QUESTION_TYPE_LABEL[props.question.questionType] || props.question.questionType || '未知'
);
const diffLabel = computed(() => DIFF[props.question.difficultyLevel]?.label || '');
const diffTagType = computed(() => DIFF[props.question.difficultyLevel]?.tag || '');
const sourceLabel = computed(() => SOURCE_LABEL[props.question.source] || '其他');

const parsedOptions = computed(() => {
  try {
    return parseOptions(props.question.options);
  } catch {
    return [];
  }
});

const renderedText = computed(() => {
  let text = renderMath(props.question.questionText || '');
  // 渲染 [图片](url)
  text = text.replace(/\[图片\]\(([^)]+)\)/g, (_, url) => {
    const safe = url.replace(/["'<>]/g, '');
    if (!/^https?:\/\//i.test(safe)) return '';
    return `<img src="${safe}" style="max-width:100%;border-radius:4px;margin:4px 0" loading="lazy" />`;
  });
  return text;
});

const renderedImages = computed(() => {
  // 如果题干中没有图片，检查是否有独立图片字段
  return '';
});

const renderedFill = computed(() => {
  const text = props.question.questionText || '';
  return renderMath(text);
});

const attachmentName = computed(() => {
  const p = (props.question.attachmentUrl || '').split('/');
  return p[p.length - 1] || '附件';
});
</script>

<style scoped>
.question-renderer {
  font-size: var(--fs-sm);
  line-height: 1.7;
  color: var(--text-primary);
}

/* ── 元信息 ── */
.qr__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-bottom: 8px;
}
.qr__cat {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}

/* ── 题干 ── */
.qr__text {
  margin-bottom: 6px;
}
.qr__text :deep(img) {
  max-width: 100%;
  border-radius: 4px;
  margin: 4px 0;
}

/* ── 选项 ── */
.qr__options {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.qr__option {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding: 4px 6px;
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
  line-height: 1.7;
}
.qr__option--correct {
  background: var(--bg-success-light, #ecfdf5);
  color: var(--el-color-success);
  font-weight: 600;
}
.qr__opt-key {
  font-weight: 600;
  color: var(--text-secondary);
  min-width: 20px;
  flex-shrink: 0;
}
.qr__opt-text {
  flex: 1;
}

/* ── 答案 / 解析 ── */
.qr__answer {
  margin-top: 8px;
  padding: 6px 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
}
.qr__explanation {
  margin-top: 6px;
  padding: 6px 10px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
  color: var(--text-regular);
}

/* ── 填空 ── */
.qr__fill {
  margin: 6px 0;
}

/* ── 附件 ── */
.qr__attachment {
  margin-top: 8px;
}

/* ── 尺寸变体 ── */
.qr--small {
  font-size: var(--fs-xs);
}
.qr--large {
  font-size: var(--fs-base);
}
</style>
