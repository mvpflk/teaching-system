<template>
  <div class="step-sim" ref="simContainer">
    <div class="sim-placeholder" v-if="!started">
      <el-icon :size="48"><Monitor /></el-icon>
      <h4>{{ step.title || '仿真操作' }}</h4>
      <p v-if="step.description">{{ step.description }}</p>
      <el-button type="primary" size="large" @click="startSim">进入仿真环境</el-button>
      <p class="sim-hint">将全屏打开 Windows 仿真环境，完成后点击"返回"保存进度</p>
    </div>
    <div v-else class="sim-active">
      <Win7Practice
        ref="practiceRef"
        mode="practice"
        :task-json="step.config?.taskJson"
        :initial-vfs="step.config?.initialVfs"
        :time-limit="step.config?.timeLimit || 300"
        @complete="onSimComplete"
        @exit="onSimExit"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Monitor } from '@element-plus/icons-vue'
import Win7Practice from '@/views/simulation/Win7Practice.vue'

const props = defineProps({
  step: { type: Object, default: () => ({}) },
  stepIndex: { type: Number, default: 0 },
  taskId: { type: Number, default: 0 },
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const started = ref(false)
const practiceRef = ref(null)
const simContainer = ref(null)

function startSim() {
  started.value = true
}

function onSimComplete(result) {
  emit('update:modelValue', {
    ...props.modelValue,
    simResult: result,
    completed: true
  })
  ElMessage.success('仿真操作完成')
  emit('saved')
}

function onSimExit() {
  // 退出时保存
  emit('saved')
}
</script>

<style scoped>
.step-sim { min-height: 400px; }
.sim-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; padding: 48px 24px; text-align: center; color: var(--text-secondary); }
.sim-placeholder h4 { margin: 0; color: var(--text-primary); }
.sim-hint { font-size: var(--fs-xs); margin-top: 8px; }
.sim-active { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 1000; background: #000; }
</style>
