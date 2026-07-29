<template>
  <div class="step-ppt">
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

    <div v-if="step.config?.checkpoints?.length" class="requirements">
      <h5>📋 操作要求</h5>
      <ul>
        <li v-for="(cp, i) in step.config.checkpoints" :key="i">
          <el-tag size="small">{{ cp.desc }}</el-tag>
        </li>
      </ul>
    </div>

    <el-upload
      :action="uploadUrl"
      :headers="uploadHeaders"
      :data="uploadData"
      accept=".pptx"
      :on-success="onUploadSuccess"
      :on-error="onUploadError"
      drag
    >
      <el-icon><UploadFilled /></el-icon>
      <div>将 PPT 文件拖到此处或点击上传</div>
      <template #tip><div class="el-upload__tip">仅支持 .pptx 格式</div></template>
    </el-upload>

    <div v-if="checkResult" class="check-result">
      <el-alert
        :title="`自动评估: ${checkResult.passedCount || 0}/${checkResult.totalCount || 0} 项通过 (${checkResult.score || 0}分)`"
        :type="checkResult.passedCount === checkResult.totalCount ? 'success' : 'warning'"
        :closable="false" show-icon
      />
      <div v-for="c in (checkResult.checkpoints || [])" :key="c.id" class="checkpoint-item">
        <span :style="{ color: c.passed ? 'var(--el-color-success, #67c23a)' : 'var(--el-color-danger, #f56c6c)' }">{{ c.passed ? '✅' : '❌' }}</span>
        <el-tag size="small" :type="cpType(c.type)">{{ cpTypeLabel(c.type) }}</el-tag>
        <span class="cp-desc">{{ c.desc }}</span>
        <span v-if="!c.passed && c.detail" class="cp-detail">{{ c.detail }}</span>
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

const uploadUrl = computed(() => `/api/training/upload/ppt/${props.taskId}`)
const uploadHeaders = getUploadHeaders()
const uploadData = computed(() => ({
  checkpoints: JSON.stringify(props.step.config?.checkpoints || [])
}))
const checkResult = ref(props.modelValue?.checkResult || null)

function onUploadSuccess(res) {
  if (res.code === 200) {
    checkResult.value = res.data
    emit('update:modelValue', { ...props.modelValue, fileUrl: res.data?.fileUrl, checkResult: res.data })
    emit('saved')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function onUploadError() { ElMessage.error('文件上传失败，请检查网络连接') }

function cpType(type) {
  const map = { slide_count: 'success', master: '', animation: 'warning', transition: 'info' }
  return map[type] || ''
}
function cpTypeLabel(type) {
  const map = { slide_count: '幻灯片数', master: '母版', animation: '动画', transition: '切换' }
  return map[type] || type
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
.step-ppt { display: flex; flex-direction: column; gap: 16px; }
.step-desc { color: var(--text-secondary); font-size: var(--fs-sm); line-height: 1.8; padding: 16px; background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%); border-radius: 12px; border-left: 4px solid var(--primary-color, #4361ee); }
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
.requirements { padding: 16px; background: var(--bg-page); border-radius: 12px; }
.requirements h5 { margin: 0 0 12px; font-size: 14px; font-weight: 600; }
.requirements ul { margin: 0; padding-left: 20px; }
.requirements li { margin: 6px 0; font-size: var(--fs-sm); }
.check-result { padding: 16px; background: var(--bg-page); border-radius: 12px; margin-top: 8px; }
.checkpoint-item { padding: 6px 0; font-size: var(--fs-sm); display: flex; gap: 8px; align-items: flex-start; }
.cp-desc { flex: 1; }
.cp-detail { color: var(--el-color-info); font-size: var(--fs-xs); }
</style>
