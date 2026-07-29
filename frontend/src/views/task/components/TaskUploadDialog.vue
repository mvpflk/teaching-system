<template>
  <el-dialog
    v-model="visible"
    title="上传试卷"
    width="480px"
    append-to-body
    @closed="$emit('close')"
  >
    <el-form label-position="top">
      <el-form-item label="试卷名称" required>
        <el-input v-model="form.title" placeholder="如：期中测试卷" />
      </el-form-item>
      <el-form-item label="学科">
        <el-select
          v-model="form.subject"
          filterable
          placeholder="选择学科"
          style="width:100%"
        >
          <el-option
            v-for="s in subjectOptions"
            :key="s.id"
            :value="s.subjectName"
            :label="s.subjectName"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="文件格式">
        <el-radio-group v-model="format">
          <el-radio value="word">Word (.docx)</el-radio>
          <el-radio value="excel">Excel (.xlsx)</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="选择文件">
        <el-upload
          :action="uploadAction"
          :headers="uploadHeaders"
          :data="uploadParams"
          :before-upload="onBeforeUpload"
          :on-success="onUploadSuccess"
          :on-error="() => $emit('uploadError')"
          :show-file-list="true"
          :limit="1"
          drag
        >
          <el-icon><UploadFilled /></el-icon>
          <div class="upload-text">拖拽或点击上传</div>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getUploadHeaders } from '@/api/task'

const props = defineProps({
  modelValue: Boolean,
  subjectOptions: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'close', 'parsed', 'uploadError'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const format = ref('word')
const uploadHeaders = getUploadHeaders()
const uploadAction = '/api/exam-share/actions/upload'
const form = reactive({ title: '', subject: '' })
const uploadParams = computed(() => ({ title: form.title, subject: form.subject || '' }))

const onBeforeUpload = () => {
  if (!form.title.trim()) { ElMessage.warning('请输入试卷名称'); return false }
  return true
}

const onUploadSuccess = (res) => {
  if (res.code !== 200) { ElMessage.error(res.message || '解析失败'); return }
  const d = res.data
  if (d?.status !== 'parsed') { ElMessage.error('解析异常'); return }
  if (!d.questionCount) { ElMessage.warning('未识别到有效题目'); return }
  emit('parsed', {
    questions: d.questions || [],
    typeCounts: d.typeCounts || {},
    title: form.title,
    subject: form.subject
  })
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
