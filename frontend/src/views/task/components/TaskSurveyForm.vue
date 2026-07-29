<template>
  <div class="form-section">
    <div class="form-section__header">
      <div style="display:flex;align-items:center;gap:6px;flex:1"><el-icon><EditPen /></el-icon> 问卷设计</div>
      <el-switch
        v-model="form.isAnonymous"
        :active-value="1"
        :inactive-value="0"
        active-text="匿名作答"
        size="small"
        inline-prompt
      />
    </div>
    <div class="form-section__body">
      <p class="survey-section__hint">从左侧题型面板选择题型添加到问卷，或从模板快速载入预设问题</p>
      <slot name="builder" />
    </div>
  </div>
</template>

<script setup>
import { EditPen } from '@element-plus/icons-vue'

defineProps({
  form: { type: Object, required: true },
})
</script>

<style scoped>
.form-section { border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 16px; background: var(--bg-card); }
.form-section__header { display: flex; align-items: center; gap: 8px; font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); padding-bottom: 12px; margin-bottom: 12px; border-bottom: 1px solid var(--border-light); }
.form-section__header .el-icon { font-size: var(--fs-lg); color: var(--primary-color); }
.survey-section__hint { font-size: var(--fs-xs); color: var(--text-secondary); margin: 0 0 12px 0; line-height: 1.5; }
</style>
