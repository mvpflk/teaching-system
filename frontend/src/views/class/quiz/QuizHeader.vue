<template>
  <div class="qp-header">
    <el-button text @click="$emit('back')"><el-icon><ArrowLeft /></el-icon>返回</el-button>
    <span class="qp-title"><el-icon><Aim /></el-icon> 课堂抽问</span>
    <el-tag
      v-if="sceneMode === 'LAB'"
      size="small"
      effect="plain"
      :type="stage === 2 ? 'warning' : stage === 3 ? 'danger' : ''"
    >
      {{ {1:'选题',2:'抽人',3:'评分'}[stage] }}
    </el-tag>
    <el-tag
      v-else
      size="small"
      effect="dark"
      :type="cState === 'idle' ? 'info' : 'warning'"
    >
      {{ cState === 'idle' ? '待开始' : '进行中' }}
    </el-tag>
  </div>
</template>

<script setup>
import { ArrowLeft, Aim } from '@element-plus/icons-vue'

defineProps({
  sceneMode: String,
  stage: Number,
  cState: String
})
defineEmits(['back'])
</script>

<style scoped>
.qp-header {
  display: flex; align-items: center; gap: 10px;
  padding-bottom: 14px; border-bottom: 0.5px solid var(--border-light); margin-bottom: 14px;
}
.qp-title {
  font-size: var(--fs-lg); font-weight: 700; flex: 1;
  display: flex; align-items: center; gap: var(--spacing-xs);
  color: var(--text-primary);
}
</style>
