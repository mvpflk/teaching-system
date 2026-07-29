<template>
  <Transition name="fb">
    <div v-if="store.scoreFeedback" class="score-feedback" :class="store.scoreFeedback.type">
      <span class="fb-icon">{{ store.scoreFeedback.type === 'success' ? '✅' : '❌' }}</span>
      <span class="fb-msg">{{ store.scoreFeedback.message }}</span>
    </div>
  </Transition>
</template>
<script setup>
import { useWin7SimStore } from '@/stores/win7Sim'
const store = useWin7SimStore()
</script>
<style scoped>
.score-feedback { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); padding: 16px 24px; border-radius: 10px; font-size: var(--fs-md); z-index: 10000; display: flex; align-items: center; gap: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); pointer-events: none; }
.score-feedback.success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
.score-feedback.error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.fb-enter-active { animation: fbIn 0.3s ease; }
.fb-leave-active { animation: fbOut 0.3s ease; }
@keyframes fbIn { from { opacity: 0; transform: translate(-50%, -50%) scale(0.8); } to { opacity: 1; transform: translate(-50%, -50%) scale(1); } }
@keyframes fbOut { from { opacity: 1; } to { opacity: 0; } }
</style>
