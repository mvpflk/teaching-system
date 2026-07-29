<template>
  <div ref="readerRef" class="tr-reader">
    <div class="tr-content" v-html="safeHtml" />
    <div v-if="!safeHtml" class="tr-empty">
      <el-empty description="暂无应知内容" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import DOMPurify from 'dompurify';
import katex from 'katex';
import 'katex/dist/katex.min.css';

function renderMath(html) {
  if (!html) return '';
  let result = html;
  result = result.replace(/\$\$([^$]+)\$\$/g, (_, f) => {
    try {
      return katex.renderToString(f.trim(), { displayMode: true, throwOnError: false });
    } catch {
      return _;
    }
  });
  result = result.replace(/\$([^$]+)\$/g, (_, f) => {
    try {
      return katex.renderToString(f.trim(), { displayMode: false, throwOnError: false });
    } catch {
      return _;
    }
  });
  return result;
}

const props = defineProps({
  detailHtml: { type: String, default: '' },
  highlightKeywordIdx: { type: Number, default: -1 },
});

const readerRef = ref(null);

const safeHtml = computed(() => {
  if (!props.detailHtml) return '';
  const withMath = renderMath(props.detailHtml);
  return DOMPurify.sanitize(withMath, {
    ALLOWED_TAGS: [
      'p',
      'strong',
      'br',
      'em',
      'u',
      'div',
      'details',
      'summary',
      'ul',
      'li',
      'span',
      'h4',
      'svg',
      'math',
      'semantics',
      'mrow',
      'mi',
      'mo',
      'mn',
      'msup',
      'msqrt',
      'mfrac',
      'annotation',
    ],
    ALLOWED_ATTR: ['class', 'open', 'aria-hidden', 'mathvariant', 'stretchy'],
  });
});

defineExpose({ scrollToKeyword });
function scrollToKeyword() {
  const el = readerRef.value;
  if (!el) return;
  const strong = el.querySelector('.key-concept, .key-number');
  if (strong) {
    strong.scrollIntoView({ behavior: 'smooth', block: 'center' });
    strong.style.background = 'rgba(67, 97, 238, 0.12)';
    setTimeout(() => {
      strong.style.background = '';
    }, 2000);
  }
}
</script>

<style scoped>
.tr-reader {
  padding: var(--spacing-md);
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  line-height: 1.85;
  font-size: var(--fs-base);
  color: var(--text-primary);
}
/* 核心概念关键词：定义段中的粗体 */
.tr-content :deep(.key-concept) {
  color: var(--primary-color);
  font-weight: 700;
  font-size: 1.05em;
  background: linear-gradient(to bottom, transparent 60%, rgba(67, 97, 238, 0.08) 60%);
  padding: 0 2px;
}
/* 数字类关键词 */
.tr-content :deep(.key-number) {
  color: #e74c3c;
  font-weight: 700;
  font-size: 1.05em;
  background: linear-gradient(to bottom, transparent 60%, rgba(231, 76, 60, 0.06) 60%);
  padding: 0 2px;
}
/* 定义段 — 学生必读核心 */
.tr-content :deep(.kp-definition) {
  padding: 12px 14px;
  background: var(--bg-section);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-md);
}
.tr-content :deep(.kp-definition p) {
  margin-bottom: 6px;
}
.tr-content :deep(.kp-definition ul) {
  padding-left: 20px;
  margin: 4px 0;
}
.tr-content :deep(.kp-definition li) {
  margin-bottom: 4px;
}

/* 例子折叠块 */
.tr-content :deep(.kp-example) {
  margin-bottom: var(--spacing-md);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  background: var(--bg-card);
  cursor: pointer;
}
.tr-content :deep(.kp-example summary) {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  font-weight: 500;
  cursor: pointer;
  user-select: none;
}
.tr-content :deep(.kp-example[open]) {
  background: var(--bg-section);
}
.tr-content :deep(.kp-example p) {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  margin-top: 8px;
}

/* 常见错误折叠块 */
.tr-content :deep(.kp-pitfalls) {
  margin-bottom: var(--spacing-md);
  border: 0.5px solid rgba(231, 76, 60, 0.15);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
}
.tr-content :deep(.kp-pitfalls summary) {
  font-size: var(--fs-sm);
  color: #e74c3c;
  font-weight: 500;
  cursor: pointer;
}
.tr-content :deep(.kp-pitfalls[open]) {
  background: rgba(231, 76, 60, 0.02);
}

/* 考试提示 */
.tr-content :deep(.kp-exam-tip) {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  padding: 6px 10px;
  background: rgba(243, 156, 18, 0.05);
  border-left: 2px solid #f39c12;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
.tr-content :deep(.kp-exam-label) {
  font-weight: 600;
  color: #f39c12;
}

.tr-content :deep(p) {
  margin-bottom: 8px;
}
.tr-content :deep(h4) {
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--text-primary);
  margin: 12px 0 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--border-light);
}
.tr-empty {
  padding: 20px 0;
}

@media (max-width: 768px) {
  .tr-reader {
    padding: var(--spacing-sm);
    line-height: 1.9;
  }
  .tr-content :deep(.key-concept),
  .tr-content :deep(.key-number) {
    font-size: 1.02rem;
  }
  .tr-content :deep(.kp-definition) {
    padding: 8px 10px;
  }
}
</style>
