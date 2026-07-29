<template>
  <div class="step-web">
    <div class="step-desc" v-if="step.description" v-html="renderedDesc"></div>

    <!-- 要求列表 -->
    <div v-if="step.config?.requirements?.length" class="web-requirements">
      <h4>📋 要求：</h4>
      <ul><li v-for="(r, i) in step.config.requirements" :key="i">{{ r }}</li></ul>
    </div>

    <!-- 编辑模式切换 -->
    <el-radio-group v-model="editMode" size="small">
      <el-radio-button value="code">代码编辑</el-radio-button>
      <el-radio-button value="upload">文件上传</el-radio-button>
    </el-radio-group>

    <!-- 代码编辑模式：左右分栏 -->
    <div v-if="editMode === 'code'" class="code-editor-panel">
      <div class="editor-pane">
        <el-tabs v-model="activeTab" type="card" size="small">
          <el-tab-pane label="HTML" name="html">
            <textarea
              v-model="htmlCode"
              class="code-textarea"
              placeholder="<h1>Hello World</h1>"
              spellcheck="false"
              @input="updatePreview"
            ></textarea>
          </el-tab-pane>
          <el-tab-pane label="CSS" name="css">
            <textarea
              v-model="cssCode"
              class="code-textarea"
              placeholder="h1 { color: #4361ee; }"
              spellcheck="false"
              @input="updatePreview"
            ></textarea>
          </el-tab-pane>
          <el-tab-pane label="JS" name="js">
            <textarea
              v-model="jsCode"
              class="code-textarea"
              placeholder="console.log('Hello');"
              spellcheck="false"
              @input="updatePreview"
            ></textarea>
          </el-tab-pane>
        </el-tabs>
        <el-button size="small" type="primary" style="margin-top:8px" @click="saveCode">
          <el-icon><Select /></el-icon> 保存代码
        </el-button>
      </div>

      <div class="preview-pane">
        <div class="preview-header">
          <span>实时预览</span>
          <el-button size="small" text @click="refreshPreview">🔄 刷新</el-button>
        </div>
        <iframe
          ref="previewFrame"
          class="preview-frame"
          sandbox="allow-scripts"
          title="网页预览"
          srcdoc=""
        ></iframe>
      </div>
    </div>

    <!-- 文件上传模式（保留原有功能） -->
    <el-upload
      v-else
      :action="uploadUrl"
      :headers="uploadHeaders"
      :accept="'.html,.css,.js'"
      :file-list="fileList"
      :on-success="onSuccess"
      :on-remove="onRemove"
      :before-upload="beforeUpload"
      drag multiple
    >
      <el-icon><UploadFilled /></el-icon>
      <div>将网页文件拖到此处或点击上传</div>
      <template #tip>
        <div class="el-upload__tip">支持 .html / .css / .js，最多 {{ step.config?.maxFiles || 5 }} 个文件</div>
      </template>
    </el-upload>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Select } from '@element-plus/icons-vue'
import { getUploadHeaders } from '@/api/task'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  step: { type: Object, default: () => ({}) },
  stepIndex: { type: Number, default: 0 },
  taskId: { type: Number, default: 0 },
  modelValue: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const editMode = ref('code')
const activeTab = ref('html')
const previewFrame = ref(null)

// 代码状态
const htmlCode = ref(props.modelValue?.html || props.step.config?.templateHtml || '')
const cssCode = ref(props.modelValue?.css || props.step.config?.templateCss || '')
const jsCode = ref(props.modelValue?.js || props.step.config?.templateJs || '')

function buildPreviewHtml() {
  return `<!DOCTYPE html><html><head><meta charset="UTF-8"><style>${cssCode.value}</style></head><body>${htmlCode.value}<script>${jsCode.value}<\/script></body></html>`
}

function updatePreview() {
  if (!previewFrame.value) return
  const doc = previewFrame.value.contentDocument || previewFrame.value.contentWindow?.document
  if (doc) {
    doc.open()
    doc.write(buildPreviewHtml())
    doc.close()
  }
}

function refreshPreview() {
  if (previewFrame.value) {
    previewFrame.value.srcdoc = buildPreviewHtml()
  }
}

function saveCode() {
  emit('update:modelValue', {
    ...props.modelValue,
    html: htmlCode.value,
    css: cssCode.value,
    js: jsCode.value
  })
  emit('saved')
  ElMessage.success('代码已保存')
}

// 初始化预览
watch([() => props.step, editMode], async () => {
  if (editMode.value === 'code') {
    await nextTick()
    setTimeout(() => updatePreview(), 200)
  }
}, { immediate: true })

// ── 文件上传模式 ──
const uploadUrl = computed(() => `/api/training/upload/web/${props.taskId}`)
const uploadHeaders = getUploadHeaders()

const fileList = ref((props.modelValue?.files || []).map((f, i) => ({
  name: f.name || `文件${i + 1}`,
  url: f.url,
  uid: Date.now() + '-' + i
})))

function beforeUpload(file) {
  const ext = '.' + file.name.split('.').pop().toLowerCase()
  if (!['.html', '.css', '.js'].includes(ext)) {
    ElMessage.error(`不支持的文件格式: ${ext}，仅支持 .html/.css/.js`)
    return false
  }
  return true
}

function onSuccess(res, file) {
  fileList.value.push(file)
  emit('update:modelValue', {
    ...props.modelValue,
    files: fileList.value.map(f => ({ name: f.name, url: f.url || f.response?.data?.url }))
  })
  emit('saved')
}

function onRemove(file) {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid)
  emit('update:modelValue', { ...props.modelValue, files: fileList.value.map(f => ({ name: f.name, url: f.url })) })
}

const renderedDesc = computed(() => renderMarkdown(props.step.description || ''))
</script>

<style scoped>
.step-web { display: flex; flex-direction: column; gap: 12px; }
.web-requirements { padding: 12px; background: var(--bg-page); border-radius: var(--radius-sm); font-size: var(--fs-sm); }
.web-requirements h4 { margin: 0 0 8px; }
.web-requirements ul { margin: 0; padding-left: 20px; }

.code-editor-panel { display: flex; gap: 10px; height: 460px; }
.editor-pane { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.code-textarea { width: 100%; flex: 1; min-height: 320px; font-family: 'JetBrains Mono','Fira Code',monospace; font-size: var(--fs-sm); padding: 10px; border: 1px solid var(--el-border-color); border-radius: var(--radius-sm); background: #1e1e2e; color: #cdd6f4; resize: none; }
.code-textarea::placeholder { color: #6c7086; }
.preview-pane { flex: 1; display: flex; flex-direction: column; border: 1px solid var(--el-border-color); border-radius: var(--radius-sm); overflow: hidden; }
.preview-header { display: flex; justify-content: space-between; align-items: center; padding: 6px 10px; background: var(--bg-page); font-size: var(--fs-xs); font-weight: 600; }
.preview-frame { flex: 1; border: none; width: 100%; background: #fff; }
</style>
