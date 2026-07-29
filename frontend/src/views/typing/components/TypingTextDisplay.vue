<template>
  <div>
    <div class="progress-row">
      <div class="progress-bar-wrap">
        <div class="progress-bar-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <span class="progress-text">{{ progressPercent }}%</span>
    </div>
    <div
      ref="textPanelRef"
      class="text-panel"
      role="log"
      aria-live="off"
      aria-label="打字原文"
    >
      <div class="text-display">
        <span
          v-for="(cs, i) in charStates"
          :key="i"
          :ref="
            (el) => {
              if (cs.state === 'current') currentCharEl = el;
            }
          "
          :class="'char-' + cs.state"
        >{{ cs.char === ' ' ? '\u00A0' : cs.char }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';

const props = defineProps({
  charStates: { type: Array, required: true },
  progressPercent: { type: Number, default: 0 },
  currentIndex: { type: Number, default: 0 },
});

const textPanelRef = ref(null);
const currentCharEl = ref(null);

watch(
  () => props.currentIndex,
  () => {
    nextTick(() => {
      if (!currentCharEl.value || !textPanelRef.value) return;

      const panel = textPanelRef.value;
      const charEl = currentCharEl.value;
      const cs = getComputedStyle(charEl);
      const lineHeight = parseFloat(cs.lineHeight);
      const fontSize = parseFloat(cs.fontSize);
      const actualLineHeight = Number.isNaN(lineHeight) ? fontSize * 1.2 : lineHeight;
      const panelRect = panel.getBoundingClientRect();
      const charRect = charEl.getBoundingClientRect();
      const distanceFromBottom = panelRect.bottom - charRect.bottom;
      const distanceFromTop = charRect.top - panelRect.top;

      // 距底部 < 2.5 行且内容已滚过面板 30%（排除文章末尾空白）
      if (distanceFromBottom < actualLineHeight * 2.5 && distanceFromTop > panelRect.height * 0.3) {
        charEl.scrollIntoView({ block: 'center', behavior: 'instant' });
      } else {
        charEl.scrollIntoView({ block: 'nearest', behavior: 'instant' });
      }
    });
  }
);
</script>

<style scoped>
.progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.progress-bar-wrap {
  flex: 1;
  height: 6px;
  background: var(--typing-border);
  border-radius: 3px;
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  background: var(--typing-correct);
  border-radius: 3px;
  transition: width 0.3s ease;
}
.progress-text {
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--typing-correct);
  min-width: 36px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.text-panel {
  background: var(--typing-surface);
  border: 1px solid var(--typing-border);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  max-height: min(280px, 35vh);
  overflow-y: auto;
}
.text-display {
  font-size: 22px;
  line-height: 1.9;
  word-break: break-all;
  letter-spacing: 0.5px;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Courier New', monospace;
}
/* ── 字符状态样式（scoped 防御性冗余） ──
   全局 typing-theme.css 已提供 .typing-theme .char-* 规则 (特异性 0,2,0)，
   但生产构建的 CSS 加载顺序可能导致 scoped .text-display[data-v] (0,2,0)
   因注入时机晚于全局样式而意外覆盖颜色。
   此处以 scoped 方式重复声明，特异性提升至 (0,3,0)，确保颜色始终生效。 */
.char-correct {
  color: var(--typing-correct);
  animation: char-bounce 150ms ease-out;
}
.char-incorrect {
  color: var(--typing-text);
  background: var(--typing-incorrect-bg);
  border-radius: 2px;
  animation: char-shake 200ms ease-out;
}
.char-current {
  background: var(--typing-current-bg);
  border-bottom: 2px solid var(--typing-cursor);
  border-radius: 2px;
  animation: cursor-pulse 1.2s ease-in-out infinite;
}
.char-pending {
  color: var(--typing-pending);
}
@media (max-width: 768px) {
  .text-display {
    font-size: 17px;
    line-height: 1.7;
  }
  .text-panel {
    max-height: min(180px, 30vh);
    padding: 12px 14px;
  }
}
</style>
