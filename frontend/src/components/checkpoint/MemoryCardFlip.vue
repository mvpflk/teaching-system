<template>
  <div class="mcf-wrap" @click="toggle">
    <div class="mcf-inner" :class="{ 'mcf-flipped': flipped }">
      <div class="mcf-front">
        <div class="mcf-type-tag">{{ typeTag }}</div>
        <div class="mcf-keyword">{{ keyword.front }}</div>
        <div class="mcf-hint">点击翻转查看释义</div>
      </div>
      <div class="mcf-back">
        <div class="mcf-type-tag mcf-type-back">{{ typeTag }}</div>
        <div class="mcf-keyword-back">{{ keyword.front }}</div>
        <div class="mcf-divider"></div>
        <div class="mcf-meaning">{{ keyword.back }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  keyword: { type: Object, default: () => ({ front: '', back: '', type: 'concept' }) }
})

const flipped = ref(false)

const typeTag = computed(() => props.keyword.type === 'number' ? '数字' : '概念')

function toggle() {
  flipped.value = !flipped.value
  if (flipped.value) {
    setTimeout(() => { flipped.value = false }, 5000)
  }
}
</script>

<style scoped>
.mcf-wrap { perspective: 800px; width: 280px; height: 180px; cursor: pointer; margin: 0 auto; }
.mcf-inner {
  width: 100%; height: 100%; position: relative;
  transform-style: preserve-3d; transition: transform 0.3s ease-in-out;
}
.mcf-flipped { transform: rotateY(180deg); }
.mcf-front, .mcf-back {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  backface-visibility: hidden; border-radius: var(--radius-md);
  border: 0.5px solid var(--border-light);
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; padding: var(--spacing-md);
  background: var(--bg-card);
}
.mcf-back { transform: rotateY(180deg); }
.mcf-type-tag {
  position: absolute; top: 8px; left: 8px;
  padding: 1px 8px; border-radius: 999px;
  font-size: var(--fs-xs); font-weight: 500;
  background: var(--primary-light); color: var(--primary-color);
}
.mcf-type-back { background: rgba(231, 76, 60, 0.08); color: #e74c3c; }
.mcf-keyword { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); }
.mcf-hint { margin-top: 12px; font-size: var(--fs-xs); color: var(--text-secondary); }
.mcf-keyword-back { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); }
.mcf-divider { width: 40px; height: 2px; background: var(--border-color); margin: 8px 0; }
.mcf-meaning { font-size: var(--fs-base); color: var(--text-regular); text-align: center; line-height: 1.5; }
</style>
