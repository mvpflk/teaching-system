<template>
  <div class="step-office">
    <div class="step-desc" v-if="step.description" v-html="renderedDesc"></div>
    <div v-if="step.resourceFile" class="resource-download">
      <el-button type="primary" size="small" @click="downloadAllMaterials">
        <el-icon><Download /></el-icon>
        下载全部素材(ZIP)
      </el-button>
    </div>
    <div v-if="step.sampleImages && step.sampleImages.length > 0" class="sample-images-section">
      <h4 class="sample-title">📷 样张参考（点击可放大）</h4>
      <div class="sample-images-grid">
        <div v-for="(img, i) in step.sampleImages" :key="i" class="sample-image-item">
          <el-image
            :src="apiUrl(img)"
            :preview-src-list="step.sampleImages.map(apiUrl)"
            :initial-index="i"
            :alt="`样张${i+1}`"
            fit="contain"
            class="sample-image"
            preview-teleported
          />
          <div class="sample-label-row">
            <span class="sample-label">样张{{ i + 1 }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-upload
      :action="uploadUrl"
      :headers="uploadHeaders"
      accept=".docx"
      :on-success="onUploadSuccess"
      drag
    >
      <el-icon><UploadFilled /></el-icon>
      <div>将 Word 文档拖到此处或点击上传</div>
      <template #tip>
        <div class="el-upload__tip">仅支持 .docx 格式</div>
      </template>
    </el-upload>

    <!-- checkpoint 预检结果 -->
    <div v-if="checkResult" class="check-result">
      <el-alert
        :title="`预检: ${checkResult.passedCount || 0}/${checkResult.totalCount || 0} 项通过`"
        :type="checkResult.passedCount === checkResult.totalCount ? 'success' : 'warning'"
        :closable="false"
      />
      <div v-for="c in (checkResult.checkpoints || [])" :key="c.id" class="checkpoint-item">
        <span :style="{ color: c.matched ? 'var(--el-color-success, #67c23a)' : 'var(--el-color-danger, #f56c6c)' }">
          {{ c.matched ? '✅' : '❌' }}
        </span>
        <span>{{ c.desc }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Download } from '@element-plus/icons-vue'
import { getUploadHeaders } from '@/api/task'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  step: { type: Object, default: () => ({}) },
  stepIndex: { type: Number, default: 0 },
  taskId: { type: Number, default: 0 },
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const uploadUrl = computed(() => `/api/training/upload/office/${props.taskId}`)
const uploadHeaders = getUploadHeaders()
const checkResult = ref(props.modelValue?.checkResult || null)

function onUploadSuccess(res) {
  if (res.code === 200) {
    checkResult.value = res.data
    emit('update:modelValue', {
      ...props.modelValue,
      fileUrl: res.data?.fileUrl,
      checkResult: res.data
    })
    emit('saved')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const renderedDesc = computed(() => renderMarkdown(props.step.description || ''))

async function downloadAllMaterials() {
  const rf = props.step.resourceFile
  if (!rf) return
  const prefix = rf.substring(0, rf.lastIndexOf('.'))
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/training/materials/download?prefix=${encodeURIComponent(prefix)}`, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    })
    if (!res.ok) throw new Error('下载失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '实训素材.zip'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('素材下载失败')
  }
}

function apiUrl(path) {
  return path && path.startsWith('/api/') ? path : '/api' + (path || '')
}
</script>

<style scoped>
.step-office { display: flex; flex-direction: column; gap: 16px; }
.step-desc { color: var(--text-secondary); font-size: var(--fs-sm); line-height: 1.8; padding: 16px; background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%); border-radius: 12px; border-left: 4px solid var(--primary-color, #4361ee); }
.step-desc p { margin: 0 0 8px; }
.step-desc p:last-child { margin-bottom: 0; }
.resource-download { display: flex; align-items: center; gap: 8px; }
.resource-download .el-button { display: flex; align-items: center; gap: 6px; }
.sample-images-section { margin-top: 8px; }
.sample-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0 0 12px; }
.sample-images-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.sample-image-item { border: 1px solid var(--border-light); border-radius: 8px; background: var(--bg-page); display: flex; flex-direction: column; align-items: center; }
.sample-image { width: 100%; max-height: 320px; display: block; cursor: zoom-in; }
.sample-image :deep(img) { object-fit: contain; max-height: 320px; width: 100%; }
.sample-label { display: block; text-align: center; padding: 6px; font-size: 12px; color: var(--text-secondary); background: rgba(0,0,0,0.02); }
.sample-label-row { display: flex; align-items: center; justify-content: space-between; padding: 4px 8px; background: rgba(0,0,0,0.02); }
.sample-label-row .sample-label { flex: 1; text-align: left; font-size: 12px; color: var(--text-secondary); background: none; padding: 4px; }
.check-result { padding: 16px; background: var(--bg-page); border-radius: 12px; margin-top: 8px; }
.checkpoint-item { padding: 6px 0; font-size: var(--fs-sm); display: flex; gap: 8px; align-items: center; }
</style>
