<template>
  <div class="mode-toolbar">
    <el-radio-group v-model="localMode" :disabled="hasStarted" size="small" class="mode-switch">
      <el-radio-button value="practice"><el-icon><EditPen /></el-icon> 自由练习</el-radio-button>
      <el-radio-button value="competition"><el-icon><Trophy /></el-icon> 竞赛模式</el-radio-button>
    </el-radio-group>
    <span v-if="inCompetition && countdown" class="cd-inline">
      <span class="cd-icon">⏱</span> 竞赛剩余: <strong>{{ countdown }}</strong>
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { EditPen, Trophy } from '@element-plus/icons-vue'

const props = defineProps({
  mode: { type: String, required: true },
  inCompetition: { type: Boolean, default: false },
  countdown: { type: String, default: '' },
  hasStarted: { type: Boolean, default: false }
})

const emit = defineEmits(['update:mode'])

const localMode = computed({
  get: () => props.mode,
  set: (v) => emit('update:mode', v)
})
</script>

<style scoped>
.mode-toolbar { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
.mode-switch { flex-shrink: 0; }
.cd-inline { margin-left: auto; color: var(--typing-cursor); font-weight: 600; font-size: var(--fs-sm); }
.cd-icon { margin-right: 4px; }
</style>
