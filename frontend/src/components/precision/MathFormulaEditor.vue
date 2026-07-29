<template>
  <div class="math-editor">
    <div class="math-toolbar">
      <el-button
        v-for="btn in row1"
        :key="btn.label"
        size="small"
        text
        :title="btn.label"
        @click="insert(btn.template)"
      >
        {{ btn.symbol }}
      </el-button>
    </div>
    <div class="math-toolbar">
      <el-button
        v-for="btn in row2"
        :key="btn.label"
        size="small"
        text
        :title="btn.label"
        @click="insert(btn.template)"
      >
        {{ btn.symbol }}
      </el-button>
    </div>
    <el-input
      ref="inputRef"
      :model-value="modelValue"
      type="textarea"
      :rows="4"
      placeholder="输入你的答案（可包含公式）"
      @update:model-value="$emit('update:modelValue', $event)"
    />
    <div v-if="modelValue" class="math-preview">
      <span class="preview-label">预览：</span><span v-html="renderSafe(modelValue)" />
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue';
import katex from 'katex';
import 'katex/dist/katex.min.css';
const props = defineProps({ modelValue: { type: String, default: '' } });
const emit = defineEmits(['update:modelValue']);
const inputRef = ref(null);
const row1 = [
  { symbol: 'x²', label: '平方', template: '$^{2}$' },
  { symbol: '√x', label: '根号', template: '$\\sqrt{}$' },
  { symbol: 'x/y', label: '分数', template: '$\\frac{}{}$' },
  { symbol: 'xᵧ', label: '下标', template: '$_{}$' },
  { symbol: 'sin', label: '正弦', template: '$\\sin$' },
  { symbol: 'cos', label: '余弦', template: '$\\cos$' },
  { symbol: 'tan', label: '正切', template: '$\\tan$' },
  { symbol: 'π', label: '圆周率', template: '$\\pi$' },
];
const row2 = [
  { symbol: 'log', label: '对数', template: '$\\log$' },
  { symbol: '±', label: '正负号', template: '$\\pm$' },
  { symbol: '≠', label: '不等于', template: '$\\neq$' },
  { symbol: '°', label: '度', template: '$^{\\circ}$' },
  { symbol: '→', label: '箭头', template: '$\\rightarrow$' },
  { symbol: '≥', label: '大于等于', template: '$\\geq$' },
  { symbol: '≤', label: '小于等于', template: '$\\leq$' },
  { symbol: '∑', label: '求和', template: '$\\sum_{}^{}$' },
];
function insert(template) {
  const el = inputRef.value?.$el?.querySelector('textarea') || inputRef.value?.$el;
  if (!el) {
    emit('update:modelValue', (props.modelValue || '') + template);
    return;
  }
  const start = el.selectionStart;
  const end = el.selectionEnd;
  const text = props.modelValue || '';
  emit('update:modelValue', text.substring(0, start) + template + text.substring(end));
  nextTick(() => {
    el.selectionStart = el.selectionEnd = start + template.length;
    el.focus();
  });
}
function renderSafe(text) {
  if (!text) return '';
  try {
    let result = '';
    let last = 0;
    const regex = /\$([^$]+)\$/g;
    let match;
    while ((match = regex.exec(text)) !== null) {
      result += text.substring(last, match.index);
      try {
        result += katex.renderToString(match[1].trim(), { throwOnError: false });
      } catch {
        result += match[0];
      }
      last = match.index + match[0].length;
    }
    result += text.substring(last);
    // 无 $ 但含 LaTeX 命令 (\...) 时，整体尝试渲染为行内公式
    if (result === text && /\\[a-zA-Z]/.test(text)) {
      try {
        const rendered = katex.renderToString(text.trim(), { throwOnError: false, displayMode: false });
        if (rendered && rendered.includes('<')) return rendered;
      } catch { /* ignore */ }
    }
    return result;
  } catch {
    return text;
  }
}
</script>

<style scoped>
.math-editor {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}
.math-toolbar {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.math-toolbar .el-button {
  min-width: 32px;
  height: 28px;
  font-size: var(--fs-md);
  padding: 0 6px;
}
.math-preview {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: var(--fs-md);
  min-height: 24px;
}
.preview-label {
  color: var(--el-color-info);
  font-size: var(--fs-xs);
  margin-right: 8px;
}
</style>
