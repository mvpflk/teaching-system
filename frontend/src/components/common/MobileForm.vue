<template>
  <div class="mf-container">
    <!-- 桌面端: 普通表单布局 -->
    <div v-if="!isMobile" class="mf-desktop">
      <slot :current-step="currentStep" :is-mobile="false" />
    </div>

    <!-- 移动端: 分步表单 -->
    <div v-else class="mf-mobile">
      <div class="mf-steps">
        <div
          v-for="(step, i) in steps"
          :key="i"
          class="mf-step-dot"
          :class="{ active: i === currentStep, done: i < currentStep }"
          @click="i < currentStep && (currentStep = i)"
        >
          <span class="mf-step-num">{{ i < currentStep ? '✓' : i + 1 }}</span>
          <span class="mf-step-label">{{ step.title }}</span>
        </div>
      </div>

      <div class="mf-body">
        <slot :current-step="currentStep" :is-mobile="true" />
      </div>

      <div class="mf-bottom-bar" :style="{ paddingBottom: 'var(--safe-bottom, 0px)' }">
        <el-button
          v-if="currentStep > 0"
          size="large"
          @click="currentStep--"
        >
          上一步
        </el-button>
        <el-button
          v-if="currentStep < steps.length - 1"
          type="primary"
          size="large"
          class="mf-next-btn"
          @click="currentStep++"
        >
          下一步
        </el-button>
        <el-button
          v-else
          type="primary"
          size="large"
          class="mf-next-btn"
          @click="$emit('finish')"
        >
          完成
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

defineProps({
  steps: { type: Array, required: true } // [{ title: '步骤名' }]
})

defineEmits(['finish'])

const currentStep = ref(0)
</script>

<style scoped>
.mf-steps {
  display: flex;
  gap: 8px;
  padding: 12px 0;
  overflow-x: auto;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 16px;
}
.mf-step-dot {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  white-space: nowrap;
  border: 1px solid var(--border-light);
  flex-shrink: 0;
}
.mf-step-dot.active {
  color: var(--primary-color);
  background: var(--primary-light);
  border-color: var(--primary-color);
  cursor: default;
}
.mf-step-dot.done {
  color: var(--success-color);
  border-color: var(--success-color);
  cursor: pointer;
}
.mf-step-num {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: var(--fs-xs);
  font-weight: 600;
}
.mf-step-dot.active .mf-step-num { background: var(--primary-color); color: #fff; }
.mf-step-dot.done .mf-step-num { background: var(--success-color); color: #fff; }
.mf-body { min-height: 200px; }
.mf-bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 12px var(--mobile-page-padding, 16px);
  background: var(--bg-card);
  border-top: 0.5px solid var(--border-color);
  z-index: 100;
  gap: 12px;
}
.mf-next-btn { flex: 1; }
</style>
