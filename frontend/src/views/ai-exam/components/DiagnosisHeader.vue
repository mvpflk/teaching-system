<template>
  <div class="diag-header">
    <el-button text @click="$emit('back')"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
    <h2>诊断报告：{{ reportTitle }}</h2>
    <el-tag v-if="subjectLabel" type="info" effect="plain">{{ subjectLabel }}</el-tag>
    <el-tag v-if="classCount > 0" effect="plain">{{ classCount }} 个班 · {{ studentCount }} 人</el-tag>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
defineProps({
  reportTitle: { type: String, default: '—' },
  subjectLabel: { type: String, default: '' },
  classCount: { type: Number, default: 0 },
  studentCount: { type: Number, default: 0 }
})
defineEmits(['back'])
</script>

<style scoped>
.diag-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.diag-header h2 { margin: 0; font-size: var(--fs-xl); color: var(--text-primary); }

@media (max-width: 768px) {
  .diag-header { flex-direction: column; align-items: flex-start; }
  .diag-header h2 { font-size: var(--fs-lg); }
}
</style>
