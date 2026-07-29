<template>
  <div class="rte-editor">
    <div class="rte-toolbar">
      <button type="button" title="加粗" @mousedown.prevent="exec('bold')"><b>B</b></button>
      <button type="button" title="斜体" @mousedown.prevent="exec('italic')"><i>I</i></button>
      <button type="button" title="下划线" @mousedown.prevent="exec('underline')"><u>U</u></button>
      <button type="button" title="删除线" @mousedown.prevent="exec('strikeThrough')"><s>S</s></button>
      <span class="rte-sep" />
      <button type="button" title="标题" @mousedown.prevent="exec('formatBlock', 'h3')">H</button>
      <button type="button" title="无序列表" @mousedown.prevent="exec('insertUnorderedList')">≡</button>
      <button type="button" title="有序列表" @mousedown.prevent="exec('insertOrderedList')">1.</button>
      <button type="button" title="引用" @mousedown.prevent="exec('formatBlock', 'blockquote')">&ldquo;</button>
      <span class="rte-sep" />
      <button type="button" title="代码" @mousedown.prevent="wrapCode"><span>&lt;/&gt;</span></button>
      <button type="button" title="链接" @click="insertLink"><el-icon><Link /></el-icon></button>
      <button type="button" title="图片" @click="insertImage"><el-icon><PictureFilled /></el-icon></button>
      <span class="rte-spacer" />
      <button type="button" title="清除格式" @mousedown.prevent="exec('removeFormat')"><el-icon><Close /></el-icon></button>
    </div>
    <div
      ref="editorRef"
      class="rte-content"
      :contenteditable="!disabled"
      :data-placeholder="placeholder"
      @input="onInput"
      @paste="onPaste"
      @keydown.enter="onEnter"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Link, PictureFilled, Close } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入内容...' },
  rows: { type: Number, default: 8 },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])
const editorRef = ref(null)

// 初始化内容
onMounted(() => {
  if (editorRef.value && props.modelValue) {
    editorRef.value.innerHTML = DOMPurify.sanitize(props.modelValue)
  }
})

watch(() => props.modelValue, (val) => {
  if (editorRef.value && editorRef.value.innerHTML !== val && !editorRef.value.isEqualNode(document.activeElement)) {
    editorRef.value.innerHTML = DOMPurify.sanitize(val || '')
  }
})

const onInput = () => {
  if (!editorRef.value) return
  emit('update:modelValue', editorRef.value.innerHTML)
}

// 执行 document.execCommand
const exec = (cmd, value) => {
  editorRef.value?.focus()
  document.execCommand(cmd, false, value || null)
  onInput()
}

// 插入链接
const insertLink = () => {
  const url = prompt('请输入链接地址：', 'https://')
  if (!url) return
  editorRef.value?.focus()
  const selection = window.getSelection()
  const text = selection?.toString() || '链接'
  document.execCommand('createLink', false, url)
  onInput()
}

// 插入图片
const insertImage = () => {
  const url = prompt('请输入图片地址：', 'https://')
  if (!url) return
  editorRef.value?.focus()
  document.execCommand('insertImage', false, url)
  onInput()
}

// 行内代码
const wrapCode = () => {
  editorRef.value?.focus()
  const selection = window.getSelection()
  if (selection && selection.rangeCount > 0 && !selection.isCollapsed) {
    const range = selection.getRangeAt(0)
    const code = document.createElement('code')
    code.textContent = selection.toString()
    range.deleteContents()
    range.insertNode(code)
    onInput()
  }
}

// 粘贴时自动去除富文本格式，仅保留纯文本
const onPaste = (e) => {
  e.preventDefault()
  const text = e.clipboardData?.getData('text/plain') || ''
  document.execCommand('insertText', false, text)
  onInput()
}

// 回车自动处理（blockquote 内换行）
const onEnter = (e) => {
  // 如果在空引用行内按回车，退出引用
  const sel = window.getSelection()
  if (sel && sel.anchorNode) {
    const blockquote = sel.anchorNode.parentElement?.closest('blockquote')
    if (blockquote && sel.anchorNode.textContent?.trim() === '') {
      e.preventDefault()
      document.execCommand('outdent')
      onInput()
    }
  }
}

// 暴露方法供父组件调用
defineExpose({
  focus: () => editorRef.value?.focus(),
  clear: () => { if (editorRef.value) { editorRef.value.innerHTML = ''; emit('update:modelValue', '') } },
})
</script>

<style scoped>
.rte-editor {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-card);
}

.rte-toolbar {
  display: flex;
  align-items: center;
  gap: 1px;
  padding: 6px 8px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
}

.rte-toolbar button {
  min-width: 30px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--fs-sm);
  color: var(--text-regular);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  transition: background 0.15s, color 0.15s;
}

.rte-toolbar button:hover {
  background: var(--bg-card);
  color: var(--primary-color);
}

.rte-toolbar button b { font-weight: 700; }
.rte-toolbar button i { font-style: italic; }

.rte-sep {
  width: 1px;
  height: 20px;
  background: var(--border-light);
  margin: 0 4px;
}

.rte-spacer { flex: 1; }

.rte-content {
  padding: 12px;
  min-height: 180px;
  max-height: 500px;
  overflow-y: auto;
  font-size: var(--fs-sm, 14px);
  line-height: 1.7;
  color: var(--text-primary);
  outline: none;
  word-break: break-word;
}

.rte-content[contenteditable="true"]:empty::before {
  content: attr(data-placeholder);
  color: var(--text-secondary);
  pointer-events: none;
}

.rte-content[contenteditable="false"] {
  background: var(--bg-section);
  cursor: not-allowed;
  opacity: 0.7;
}

/* 内容样式 */
.rte-content :deep(h3) { font-size: var(--fs-lg); font-weight: 600; margin: 12px 0 4px; }
.rte-content :deep(h4) { font-size: var(--fs-lg); font-weight: 600; margin: 8px 0 4px; }
.rte-content :deep(blockquote) {
  border-left: 3px solid var(--primary-color);
  padding: 4px 12px;
  margin: 8px 0;
  color: var(--text-secondary);
  background: var(--bg-section);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
.rte-content :deep(ul), .rte-content :deep(ol) { padding-left: 24px; margin: 4px 0; }
.rte-content :deep(li) { margin: 2px 0; }
.rte-content :deep(code) {
  background: var(--bg-section);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: var(--fs-sm);
}
.rte-content :deep(a) { color: var(--primary-color); text-decoration: underline; }
.rte-content :deep(img) { max-width: 100%; border-radius: var(--radius-md); margin: 8px 0; }
.rte-content :deep(pre) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 12px;
  overflow-x: auto;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: var(--fs-sm);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .rte-toolbar { gap: 0; padding: 4px 6px; }
  .rte-toolbar button { min-width: 26px; height: 26px; font-size: var(--fs-xs); }
  .rte-content { min-height: 140px; max-height: 350px; padding: 10px; }
}
</style>
