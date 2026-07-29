<template>
  <div class="form-section">
    <div class="form-section__header"><el-icon><Setting /></el-icon> 作业设置</div>
    <div class="form-section__body">
      <el-row :gutter="12">
        <el-col v-if="form.scoreType !== 'GRADE_5'" :xs="12" :sm="8">
          <el-form-item label="满分">
            <el-input-number
              v-model="form.totalScore"
              :min="1"
              :max="300"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="选项" class="switches-row">
        <el-switch
          v-model="form.isRequired"
          :active-value="1"
          :inactive-value="0"
          active-text="必做"
          size="small"
        />
        <el-switch
          v-model="form.allowResubmit"
          :active-value="1"
          :inactive-value="0"
          active-text="可重交"
          size="small"
        />
        <el-switch v-model="homeworkConfig.allowLateSubmit" active-text="允许迟交" size="small" />
        <el-switch
          v-model="form.autoWrongbook"
          :active-value="1"
          :inactive-value="0"
          active-text="错题收录"
          size="small"
        />
        <el-switch
          v-model="form.notifyParents"
          :active-value="1"
          :inactive-value="0"
          active-text="通知家长"
          size="small"
        />
      </el-form-item>
      <slot name="attachments" />
    </div>
  </div>
</template>

<script setup>
import { Setting } from '@element-plus/icons-vue'

defineProps({
  form: { type: Object, required: true },
  homeworkConfig: { type: Object, required: true },
})
</script>

<style scoped>
.form-section { border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 16px; background: var(--bg-card); }
.form-section__header { display: flex; align-items: center; gap: 8px; font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); padding-bottom: 12px; margin-bottom: 12px; border-bottom: 1px solid var(--border-light); }
.form-section__header .el-icon { font-size: var(--fs-lg); color: var(--primary-color); }
.switches-row :deep(.el-form-item__content) { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; min-height: 32px; }
</style>
