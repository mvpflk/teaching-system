<template>
  <div
    ref="cmdWindow"
    class="cmd-window"
    @click="focusWindow"
  >
    <div ref="outputEl" class="cmd-output">
      <div class="cmd-line dim">Microsoft Windows [版本 6.1.7601]</div>
      <div class="cmd-line dim">版权所有 (c) 2009 Microsoft Corporation。</div>
      <div class="cmd-line dim">&nbsp;</div>
      <div v-for="(line, i) in history" :key="i" class="cmd-line">
        <span v-if="line.type === 'input'" class="cmd-prompt">{{ prompt }}</span>
        <span :class="line.type === 'input' ? 'cmd-input' : line.type === 'error' ? 'cmd-error' : 'cmd-result'" v-html="line.text"></span>
      </div>
    </div>
    <div class="cmd-input-line">
      <span class="cmd-prompt">{{ prompt }}</span>
      <span class="cmd-typed">{{ typedText }}</span>
      <span class="cmd-cursor">_</span>
    </div>
    <!-- 隐藏输入框：专门处理键盘输入，绕过中文输入法拦截 -->
    <input
      ref="hiddenInput"
      class="cmd-hidden-input"
      autocomplete="off"
      autocorrect="off"
      autocapitalize="off"
      spellcheck="false"
      @keydown="onKeyDown"
      @input.prevent="onCompositionInput"
      @compositionstart="onCompositionStart"
      @compositionend="onCompositionEnd"
    />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'
import { createWin7Commands } from '@/constants/win7Commands'

const store = useWin7SimStore()
const currentDir = ref('C:\\Users\\Student')
const history = ref([])
const typedText = ref('')
const outputEl = ref(null)
const cmdWindow = ref(null)
const hiddenInput = ref(null)
const isComposing = ref(false)
const prompt = computed(() => `${currentDir.value}>`)

const commands = createWin7Commands(store, currentDir)

function execute(input) {
  if (!input) return
  history.value.push({ type: 'input', text: input })

  const parts = input.split(/\s+/)
  const targetCmd = parts[0].toLowerCase()

  let cmd = targetCmd
  let args = parts.slice(1).join(' ')
  let fn = commands[cmd]

  if (parts.length >= 2 && parts[1]) {
    const second = parts[1].toLowerCase()
    if (second.startsWith('/') || second.startsWith('-')) {
      const twoWord = `${parts[0].toLowerCase()} ${second}`
      if (commands[twoWord]) { fn = commands[twoWord]; cmd = twoWord; args = parts.slice(2).join(' ') }
    }
  }

  if (!fn) {
    history.value.push({ type: 'error', text: `  '${parts[0]}' 不是内部或外部命令，也不是可运行的程序。` })
  } else {
    const output = fn(args)
    if (cmd === 'cls') {
      history.value = []
    } else if (output && output.length > 0) {
      history.value.push(...output.map(l => ({ ...l, text: '  ' + l.text })))
    }
    store.recordAction('cmdExecute', targetCmd, { args, dir: currentDir.value })
  }

  typedText.value = ''
  nextTick(() => { if (outputEl.value) outputEl.value.scrollTop = outputEl.value.scrollHeight })
}

// ── 判断当前 CMD 是否顶层窗口 ──
function isTopmostCmd() {
  const wins = [...store.openWindows]
  if (!wins.length) return false
  const top = wins.reduce((a, b) => (b.zIndex > a.zIndex ? b : a), wins[0])
  return top?.app === 'cmd'
}

// ── 键盘处理：用隐藏 input 接收所有按键，天然绕过中文输入法拦截 ──
function onKeyDown(e) {
  if (isComposing.value) return  // IME 组合中，不处理 keydown

  const key = e.key
  const ctrl = e.ctrlKey

  e.stopPropagation()

  // Ctrl+L → 清屏
  if (ctrl && key === 'l') { e.preventDefault(); history.value = []; return }
  // 其他 Ctrl 组合键放行
  if (ctrl && (key === 'c' || key === 'v' || key === 'x' || key === 'a')) { e.preventDefault(); return }

  if (key === 'Enter') { e.preventDefault(); execute(typedText.value.trim()); return }
  if (key === 'Backspace') { e.preventDefault(); typedText.value = typedText.value.slice(0, -1); return }
  if (key === 'Escape') { e.preventDefault(); typedText.value = ''; return }
  if (key === 'Tab') { e.preventDefault(); return }
  if (key.startsWith('Arrow')) { e.preventDefault(); return }
  if (key === 'Home' || key === 'End' || key === 'PageUp' || key === 'PageDown') { e.preventDefault(); return }
  if (key === 'Insert' || key === 'Delete') { e.preventDefault(); if (key === 'Delete') typedText.value = typedText.value.slice(0, -1); return }
  if (key.startsWith('F') && key.length <= 3) { e.preventDefault(); return }

  // 普通可打印字符（大小写字母、数字、符号）
  if (key.length === 1 && !ctrl) {
    e.preventDefault()
    typedText.value += key
    return
  }

  e.preventDefault()
}

// ── 中文输入法处理 ──
function onCompositionStart() {
  isComposing.value = true
}
function onCompositionEnd(e) {
  isComposing.value = false
  // 输入法组合完成后，将组合文字加入 typedText
  const composed = e.data || ''
  if (composed) {
    typedText.value += composed
  }
  // 清空隐藏 input
  if (hiddenInput.value) hiddenInput.value.value = ''
}
function onCompositionInput(e) {
  // 组合过程中，input 的值是 IME 的中间态，不需要处理
  // 等待 compositionend 拿到最终结果
}

// ── 聚焦管理 ──
function focusWindow() {
  if (isTopmostCmd()) {
    hiddenInput.value?.focus()
  }
}

// 挂载后立即聚焦到隐藏 input
onMounted(() => {
  hiddenInput.value?.focus()
  setTimeout(() => hiddenInput.value?.focus(), 100)
  setTimeout(() => hiddenInput.value?.focus(), 300)
})

// CMD 顶层时自动聚焦
let observeInterval = null
onMounted(() => {
  observeInterval = setInterval(() => {
    if (isTopmostCmd()) {
      if (document.activeElement !== hiddenInput.value) {
        hiddenInput.value?.focus()
      }
    }
  }, 300)
})
onBeforeUnmount(() => {
  if (observeInterval) clearInterval(observeInterval)
})
</script>

<style scoped>
.cmd-window {
  background: #000; color: #0f0; font-family: 'Consolas', 'Courier New', monospace;
  font-size: var(--fs-sm); height: 100%; display: flex; flex-direction: column;
  outline: none; user-select: text;
}
.cmd-window:focus { outline: none; }
.cmd-output { flex: 1; overflow-y: auto; padding: 8px 12px; white-space: pre-wrap; }
.cmd-line { line-height: 1.4; }
.cmd-prompt { color: #fff; user-select: none; }
.cmd-input { color: #ff0; }
.cmd-result { color: #aaa; }
.cmd-error { color: #f44; }
.cmd-line.dim { color: #888; }
.cmd-input-line {
  display: flex; align-items: center; padding: 4px 12px;
  border-top: 1px solid #222; min-height: 22px;
}
.cmd-typed { color: #ff0; white-space: pre; }
.cmd-cursor { color: #0f0; animation: blink 1s step-end infinite; }
@keyframes blink { 0%, 50% { visibility: visible; } 51%, 100% { visibility: hidden; } }
/* 隐藏输入框：接收键盘但不可见 */
.cmd-hidden-input {
  position: absolute; left: 0; top: 0;
  width: 1px; height: 1px;
  opacity: 0; border: none; outline: none;
  background: transparent; color: transparent;
  caret-color: transparent;
  pointer-events: none;
  font-size: 1px;
}
</style>
