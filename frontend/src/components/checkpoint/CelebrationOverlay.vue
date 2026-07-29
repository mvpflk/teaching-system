<template>
  <Transition name="overlay">
    <div v-if="visible" class="co-overlay" @click.self="$emit('close')">
      <div class="co-card">
        <div class="co-stars">
          <span
            v-for="i in 3"
            :key="i"
            class="co-star"
            :class="{ 'co-star-filled': i <= stars }"
            :style="{ animationDelay: `${i * 200}ms` }"
          >★</span>
        </div>
        <div class="co-title">通关成功！</div>
        <div v-if="credits > 0" class="co-credits">+{{ credits }} 积分</div>
        <div class="co-accuracy">正确率 {{ accuracy }}%</div>
        <div class="co-actions">
          <button v-if="hasMore" class="co-btn co-btn-primary" @click="$emit('next')">
            进入下一关
          </button>
          <button class="co-btn co-btn-secondary" @click="$emit('close')">返回总览</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  credits: { type: Number, default: 0 },
  accuracy: { type: Number, default: 0 },
  hasMore: { type: Boolean, default: true },
});

defineEmits(['close', 'next']);

const stars = computed(() => {
  if (props.accuracy >= 90) return 3;
  if (props.accuracy >= 70) return 2;
  if (props.accuracy >= 50) return 1;
  return 0;
});
</script>

<style scoped>
.co-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
}
.co-card {
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  text-align: center;
  max-width: 360px;
  width: 90%;
  animation: slideUp 0.4s ease-out;
}
.co-stars {
  margin-bottom: 24px;
}
.co-star {
  font-size: 40px;
  color: #e4e4e7;
  margin: 0 4px;
  display: inline-block;
  opacity: 0;
  animation: starPop 0.4s ease-out forwards;
}
.co-star-filled {
  color: #f59e0b;
}
.co-title {
  font-size: 24px;
  font-weight: 700;
  color: #18181b;
  margin-bottom: 16px;
}
.co-credits {
  font-size: 20px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 8px;
  animation: creditBounce 0.6s ease-out 0.6s;
}
.co-accuracy {
  font-size: 14px;
  color: #71717a;
  margin-bottom: 32px;
}
.co-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.co-btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}
.co-btn-primary {
  background: var(--primary-color);
  color: white;
}
.co-btn-primary:hover {
  background: var(--primary-dark);
}
.co-btn-secondary {
  background: transparent;
  color: #71717a;
  border: 1px solid #e4e4e7;
}
.co-btn-secondary:hover {
  background: #f4f4f5;
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
@keyframes starPop {
  0% {
    transform: scale(0) rotate(-30deg);
    opacity: 0;
  }
  60% {
    transform: scale(1.2) rotate(5deg);
  }
  100% {
    transform: scale(1) rotate(0);
    opacity: 1;
  }
}
@keyframes creditBounce {
  0%,
  100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-8px);
  }
  60% {
    transform: translateY(-4px);
  }
}
.overlay-enter-active,
.overlay-leave-active {
  transition: opacity 0.3s;
}
.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
}
</style>
