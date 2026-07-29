<template>
  <div class="flashcard-wrapper">
    <div
      class="flashcard"
      :class="{ flipped }"
      @click="flipped = !flipped"
      @touchstart="touchStart"
      @touchend="touchEnd"
    >
      <div class="card-inner">
        <div class="card-front">
          <div class="card-label">🃏 问题</div>
          <div class="card-text">"{{ card.frontText }}"</div>
          <div class="card-hint">点击翻转</div>
        </div>
        <div class="card-back">
          <div class="card-label">💡 答案</div>
          <div class="card-text">{{ card.backText }}</div>
        </div>
      </div>
    </div>
    <div v-if="flipped && showRating" class="rating-bar">
      <button class="rating-btn rating-btn--forgot" :disabled="rating" @click.stop="onRate(1)">
        <span class="rating-emoji">😞</span>
        <span class="rating-label">忘了</span>
      </button>
      <button class="rating-btn rating-btn--hard" :disabled="rating" @click.stop="onRate(2)">
        <span class="rating-emoji">🤔</span>
        <span class="rating-label">困难</span>
      </button>
      <button class="rating-btn rating-btn--good" :disabled="rating" @click.stop="onRate(3)">
        <span class="rating-emoji">😊</span>
        <span class="rating-label">记得</span>
      </button>
      <button class="rating-btn rating-btn--easy" :disabled="rating" @click.stop="onRate(4)">
        <span class="rating-emoji">🎯</span>
        <span class="rating-label">简单</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ card: Object, showRating: { type: Boolean, default: true } })
const emit = defineEmits(['rate'])
const flipped = ref(false)
const rating = ref(false)

// FIX: 卡片切换时重置翻转和评分状态，确保下一张卡片显示正面
watch(() => props.card?.id, () => {
  flipped.value = false
  rating.value = false
})

function onRate(val) {
  if (rating.value) return
  rating.value = true
  emit('rate', val)
}

let startX = 0
function touchStart(e) { startX = e.changedTouches[0].clientX }
function touchEnd(e) {
  const dx = e.changedTouches[0].clientX - startX
  if (Math.abs(dx) > 80) flipped.value = !flipped.value
}
</script>

<style scoped>
.flashcard-wrapper { max-width: 400px; margin: 0 auto; }
.flashcard { perspective: 800px; cursor: pointer; user-select: none; }
.card-inner {
  position: relative; width: 100%; min-height: 220px;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}
.flipped .card-inner { transform: rotateY(180deg); }
.card-front, .card-back {
  position: absolute; width: 100%; height: 100%; backface-visibility: hidden;
  border-radius: 12px; padding: 24px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; box-sizing: border-box;
}
.card-front {
  background: var(--primary-color, var(--primary-color)); color: #fff;
  border: 0.5px solid var(--primary-color, var(--primary-color));
}
.card-back {
  background: var(--bg-card); color: var(--text-primary);
  transform: rotateY(180deg); border: 0.5px solid var(--border-light);
}
.card-label { font-size: var(--fs-xs); opacity: 0.7; margin-bottom: 8px; letter-spacing: 0.5px; }
.card-text { font-size: var(--fs-lg); text-align: center; line-height: 1.7; }
.card-hint { font-size: var(--fs-xs); opacity: 0.5; margin-top: 16px; }
.rating-bar {
  display: flex; justify-content: center; margin-top: 16px;
  border-radius: 8px; overflow: hidden;
  border: 0.5px solid var(--border-base);
}
.rating-btn {
  flex: 1; min-width: 0; padding: 10px 8px; border: none; border-radius: 0;
  background: var(--bg-card); color: var(--text-secondary);
  font-size: var(--fs-xs); font-family: inherit; cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.rating-btn:not(:last-child) {
  border-right: 0.5px solid var(--border-base);
}
.rating-btn:hover:not(:disabled) { background: var(--bg-hover); }
.rating-btn--forgot:hover:not(:disabled) { background: #fef2f2; color: #dc2626; }
.rating-btn--hard:hover:not(:disabled) { background: #fff7ed; color: #ea580c; }
.rating-btn--good:hover:not(:disabled) { background: var(--primary-light); color: var(--primary-color); }
.rating-btn--easy {
  background: var(--primary-color); color: #fff;
}
.rating-btn--easy:hover:not(:disabled) { background: var(--primary-dark); }
.rating-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.rating-emoji { font-size: var(--fs-lg); line-height: 1; }
.rating-label { font-size: var(--fs-xs); letter-spacing: 0.3px; }
</style>
