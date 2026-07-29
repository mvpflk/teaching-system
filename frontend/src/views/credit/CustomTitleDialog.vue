<template>
  <el-dialog
    v-model="visible"
    title="🎭 自定义称号"
    width="400px"
    destroy-on-close
    append-to-body
  >
    <div style="text-align:center;padding:12px 0">
      <p style="color:var(--text-secondary);margin-bottom:16px">
        请输入你想要的称号（最长20字），有效期为7天
      </p>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        @submit.prevent="handleConfirm"
      >
        <el-form-item prop="title">
          <el-input
            v-model="formData.title"
            placeholder="如：全栈达人"
            maxlength="20"
            show-word-limit
            size="large"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="settingTitle" @click="handleConfirm">确认设置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useFormRules } from '@/composables/useFormRules'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  settingTitle: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const { required: req } = useFormRules()
const formRef = ref(null)
const formData = reactive({ title: '' })
const formRules = { title: [req('称号')] }
const handleConfirm = async () => {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  emit('confirm', formData.title.trim())
  formData.title = ''
}
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
