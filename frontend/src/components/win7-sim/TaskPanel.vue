<template>
  <div v-if="store.currentTask" class="task-panel">
    <div class="tp-header">
      <span class="tp-title">📋 {{ store.currentTask.title }}</span>
      <span class="tp-timer">⏱ {{ formattedTime }}</span>
    </div>

    <!-- 完成总结 -->
    <div v-if="allDone" class="tp-complete">
      <div class="tp-complete-icon">🎉</div>
      <div class="tp-complete-title">全部完成！</div>
      <div class="tp-complete-stat">
        用时 {{ formattedTime }} · {{ store.taskSteps.length }} 个步骤
      </div>
      <el-button
        type="primary"
        size="small"
        style="margin-top:8px"
        @click="$router.back()"
      >
        返回列表
      </el-button>
    </div>

    <!-- 步骤列表 -->
    <template v-else>
      <p class="tp-desc">{{ store.currentTask.description }}</p>
      <div class="tp-steps">
        <div
          v-for="(step, i) in store.taskSteps"
          :key="i"
          class="tp-step"
          :class="{ done: step.completed, active: i === activeStep }"
        >
          <span class="tp-step-icon">{{ step.completed ? '✅' : (i === activeStep ? '▶' : '○') }}</span>
          <span class="tp-step-name">{{ step.name }}</span>
          <button v-if="!step.completed && i === activeStep" class="tp-hint-btn" @click="useHint(i)">💡 提示</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useWin7SimStore } from '@/stores/win7Sim'
const store = useWin7SimStore()
const activeStep = ref(0)
const elapsed = ref(0)
let timer = null

const formattedTime = computed(() => {
  const m = Math.floor(elapsed.value / 60), s = elapsed.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const allDone = computed(() => store.taskSteps.length > 0 && store.taskSteps.every(s => s.completed))

watch(() => store.taskSteps, (steps) => {
  const idx = steps.findIndex(s => !s.completed)
  activeStep.value = idx >= 0 ? idx : steps.length
}, { deep: true })

function useHint(i) {
  store.usedHints.push(i)
  store.showFeedback('success', `提示: ${store.taskSteps[i].hint}`)
}

timer = setInterval(() => { elapsed.value++ }, 1000)
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.task-panel { position: absolute; top: 10px; right: 10px; width: 280px; background: rgba(255,255,255,0.96); border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.15); z-index: 500; overflow: hidden; }
.tp-header { background: var(--primary-color); color: #fff; padding: 10px 14px; display: flex; justify-content: space-between; align-items: center; font-size: var(--fs-sm); }
.tp-title { font-weight: 600; }
.tp-timer { font-size: var(--fs-xs); opacity: 0.9; }
.tp-desc { padding: 10px 14px; margin: 0; font-size: var(--fs-xs); color: var(--text-regular); border-bottom: 1px solid var(--border-light); }
.tp-steps { padding: 8px 0; max-height: 300px; overflow-y: auto; }
.tp-step { display: flex; align-items: center; gap: 8px; padding: 6px 14px; font-size: var(--fs-xs); }
.tp-step.done { color: var(--text-secondary); }
.tp-step.active { background: var(--primary-light); color: var(--primary-color); font-weight: 500; }
.tp-step-icon { width: 18px; text-align: center; }
.tp-step-name { flex: 1; }
.tp-hint-btn { padding: 1px 8px; font-size: 10px; border: 1px solid var(--primary-color); border-radius: 3px; background: #fff; color: var(--primary-color); cursor: pointer; }

.tp-complete { padding: 24px 16px; text-align: center; }
.tp-complete-icon { font-size: 36px; margin-bottom: 8px; }
.tp-complete-title { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.tp-complete-stat { font-size: var(--fs-xs); color: var(--text-secondary); }
</style>
