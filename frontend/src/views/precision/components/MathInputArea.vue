<template>
  <div class="math-input-area">
    <div class="input-mode-switch">
      <el-radio-group v-model="inputMode" size="small" @change="resetPhoto">
        <el-radio-button value="text">✏️ 文本输入</el-radio-button>
        <el-radio-button value="photo">📷 拍照上传</el-radio-button>
      </el-radio-group>
    </div>
    <MathFormulaEditor v-if="inputMode === 'text'" v-model="localAnswer" />
    <div v-else class="photo-upload-area">
      <el-upload :action="uploadUrl + '?questionId=' + question.questionId + '&questionType=' + question.questionType" :headers="uploadHeaders" :on-success="onPhotoUploaded" :on-error="onPhotoUploadError" :before-upload="beforePhotoUpload" :file-list="fileList" list-type="picture" :limit="1" accept="image/jpeg,image/png,image/webp">
        <el-button type="primary" plain :loading="uploading">📷 上传解答照片</el-button>
        <template #tip>
          <div class="el-upload__tip">JPG/PNG/WebP，≤5MB。拍照时确保字迹清晰</div>
        </template>
      </el-upload>
      <div v-if="ocrText" class="ocr-result">
        <div class="ocr-result-header">
          <span>🔍 AI识别结果（可编辑修正）</span>
          <el-tag v-if="ocrConfidence >= 0.85" type="success" size="small">高置信度</el-tag>
          <el-tag v-else-if="ocrConfidence >= 0.7" type="warning" size="small">建议复核</el-tag>
          <el-tag v-else type="danger" size="small">低置信度</el-tag>
        </div>
        <el-input v-model="localOcrText" type="textarea" :rows="5" placeholder="AI识别中..." />
        <div class="ocr-confirm-bar">
          <el-checkbox v-model="localOcrConfirmed">我已核对AI识别结果，确认识别内容无误</el-checkbox>
          <span v-if="!localOcrConfirmed" class="ocr-confirm-hint">请确认识别结果后再提交</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MathFormulaEditor from '@/components/precision/MathFormulaEditor.vue'

const props = defineProps({
  question: { type: Object, required: true },
  uploadUrl: { type: String, required: true },
  uploadHeaders: { type: Object, required: true }
})

const emit = defineEmits(['update:answer', 'update:ocrText', 'update:attachmentPath', 'update:ocrConfidence', 'update:ocrConfirmed'])

const inputMode = ref('text')
const localAnswer = ref('')
const fileList = ref([])
const uploading = ref(false)
const localOcrText = ref('')
const ocrText = ref('')
const localAttachmentPath = ref('')
const localOcrConfidence = ref(0)
const ocrConfidence = ref(0)
const localOcrConfirmed = ref(false)

watch(localAnswer, (val) => emit('update:answer', val))
watch(localOcrText, (val) => { ocrText.value = val; emit('update:ocrText', val) })
watch(localOcrConfirmed, (val) => emit('update:ocrConfirmed', val))

function resetPhoto() {
  localOcrText.value = ''
  fileList.value = []
  localOcrConfirmed.value = false
  emit('update:ocrText', '')
  emit('update:attachmentPath', '')
  emit('update:ocrConfirmed', false)
}

function beforePhotoUpload(file) {
  const validTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!validTypes.includes(file.type)) { ElMessage.warning('仅支持 JPG/PNG/WebP 格式'); return false }
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('文件大小不能超过 5MB'); return false }
  uploading.value = true
  return true
}

function onPhotoUploaded(response) {
  uploading.value = false
  if (response.code === 200 && response.data) {
    localOcrText.value = response.data.ocrText || ''
    localAttachmentPath.value = response.data.attachmentPath || ''
    localOcrConfidence.value = response.data.confidence || 0
    ocrConfidence.value = localOcrConfidence.value
    localOcrConfirmed.value = localOcrConfidence.value >= 0.85
    emit('update:attachmentPath', localAttachmentPath.value)
    emit('update:ocrConfidence', localOcrConfidence.value)
    ElMessage.success('上传成功，AI已识别文字')
  } else {
    ElMessage.warning(response.msg || '上传失败')
  }
}

function onPhotoUploadError() {
  uploading.value = false
  ElMessage.warning('上传失败，请重试')
}
</script>

<style scoped>
.math-input-area { margin-top: 12px; }
.input-mode-switch { margin-bottom: 12px; display: flex; justify-content: center; }
.photo-upload-area { border: 1px dashed var(--border-base); border-radius: 8px; padding: 16px; background: var(--bg-hover); }
.ocr-result { margin-top: 12px; }
.ocr-result-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: var(--fs-sm); font-weight: 500; }
.ocr-confirm-bar { margin-top: 10px; padding: 10px 12px; background: var(--primary-light); border: 1px solid var(--primary-color); border-radius: 6px; display: flex; flex-direction: column; gap: 6px; }
.ocr-confirm-hint { font-size: var(--fs-xs); color: var(--primary-color); margin-left: 24px; }
</style>
