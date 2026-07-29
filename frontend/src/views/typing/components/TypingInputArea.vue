<template>
  <textarea
    ref="inputRef"
    :value="modelValue"
    class="typing-input"
    :disabled="disabled"
    :placeholder="hasStarted ? '继续输入…' : '开始打字…'"
    autocomplete="off"
    autocorrect="off"
    autocapitalize="off"
    spellcheck="false"
    aria-label="打字输入区"
    @paste.prevent
    @contextmenu.prevent
    @keydown.esc="$emit('reset')"
    @compositionstart="onCompositionStart"
    @compositionend="onCompositionEnd"
    @input="onInput"
  ></textarea>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  hasStarted: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'reset', 'composition-start', 'composition-end'])

const inputRef = ref(null)
const isComposing = ref(false)

// IME 组合期间（拼音→汉字）不更新 modelValue，防止中间态拼音文本
// 污染 typedText 导致：①高亮位置超前  ②长度临时超目标触发提前结束
function onInput(e) {
  if (isComposing.value) return
  emit('update:modelValue', e.target.value)
}

function onCompositionStart(e) {
  isComposing.value = true
  emit('composition-start')
}

function onCompositionEnd(e) {
  isComposing.value = false
  emit('composition-end')
  // 不在 compositionend 中发射 update:modelValue，因为浏览器会在
  // compositionend 之后自动触发一次 input 事件，届时 isComposing 已为 false，
  // onInput 会正常发射最终提交的文本，避免双重 emit
}

defineExpose({ inputRef })
</script>

<style scoped>
.typing-input { width: 100%; min-height: 80px; resize: vertical; padding: 14px 16px; font-size: var(--fs-xl); border: 1px solid var(--typing-border); border-radius: var(--radius-md); outline: none; letter-spacing: 0.5px; line-height: 1.7; transition: border-color 0.2s, box-shadow 0.2s; font-family: 'JetBrains Mono','Fira Code','Consolas','Courier New',monospace; background: var(--typing-surface); color: var(--typing-text); caret-color: var(--typing-cursor); }
.typing-input:focus { border-color: var(--typing-cursor); box-shadow: 0 0 0 2px rgba(137,180,250,0.2); }
.typing-input:disabled { opacity: 0.6; }
@media (max-width: 768px) {
  .typing-input { font-size: 17px; min-height: 60px; padding: 10px 12px; }
}
</style>
