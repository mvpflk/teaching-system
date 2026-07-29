<template>
  <el-dialog
    v-model="visible"
    title="从Word导入题目"
    width="580px"
    append-to-body
  >
    <div class="import-category-section">
      <div class="import-section-title">导入到分类</div>
      <div class="import-category-row">
        <el-select
          v-model="importSubjectId"
          placeholder="选择学科"
          size="default"
          clearable
          @change="onSubjectChange"
        >
          <el-option
            v-for="s in filteredCategoryTree"
            :key="s.id"
            :value="s.id"
            :label="s.name"
          />
        </el-select>
        <span class="import-arrow">→</span>
        <el-select
          v-model="importChapterId"
          placeholder="选择章节"
          size="default"
          :disabled="!importSubjectId"
          clearable
          @change="onChapterChange"
        >
          <el-option
            v-for="c in chapters"
            :key="c.id"
            :value="c.id"
            :label="c.name"
          />
        </el-select>
        <span class="import-arrow">→</span>
        <el-select
          v-model="importTaskId"
          placeholder="选择任务"
          size="default"
          :disabled="!importChapterId"
          clearable
          @change="onTaskChange"
        >
          <el-option
            v-for="t in tasks"
            :key="t.id"
            :value="t.id"
            :label="t.name"
          />
        </el-select>
        <span class="import-arrow">→</span>
        <el-select
          v-model="importKpId"
          placeholder="知识点（可选）"
          size="default"
          :disabled="!importTaskId"
          clearable
        >
          <el-option
            v-for="k in kps"
            :key="k.id"
            :value="k.id"
            :label="k.name"
          />
        </el-select>
      </div>
    </div>
    <div style="margin-bottom:12px;display:flex;gap:8px;align-items:center">
      <el-button
        text
        type="primary"
        size="small"
        @click="downloadTemplate"
      >
        <el-icon><Download /></el-icon>下载Word模板
      </el-button>
      <span style="font-size:var(--fs-xs);color:var(--text-secondary)">提示：文件名与知识点同名可自动归类</span>
    </div>
    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :on-change="onFilesChanged"
      accept=".docx"
      multiple
    >
      <el-icon class="el-icon--upload" size="40"><UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽 Word 文件到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">支持多选 .docx 文件批量导入<br>文件名与知识点同名时自动匹配分类</div>
      </template>
    </el-upload>
    <!-- 文件列表 -->
    <div v-if="fileList.length > 0" style="margin-top:12px">
      <div style="font-size:var(--fs-sm);font-weight:600;margin-bottom:6px">待导入文件 ({{ fileList.length }})</div>
      <el-table :data="fileList" size="small" max-height="260">
        <el-table-column
          label="文件名"
          prop="name"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column label="匹配知识点" min-width="160">
          <template #default="{ row }">
            <el-tag v-if="row.matchedKp" size="small" type="success">{{ row.matchedKp }}</el-tag>
            <el-tooltip v-else-if="row.matchedTask" content="匹配到任务级，建议精确到知识点" placement="top">
              <el-tag size="small" type="warning">{{ row.matchedTask }}</el-tag>
            </el-tooltip>
            <div v-else style="display:flex;flex-direction:column;gap:4px">
              <span style="color:var(--danger-color);font-size:var(--fs-xs);font-weight:600">⚠ 未匹配</span>
              <span v-if="row.suggestions && row.suggestions.length" style="font-size:var(--fs-xs);color:var(--text-secondary)">
                相似: {{ row.suggestions.join(' / ') }}
              </span>
              <span v-else style="font-size:var(--fs-xs);color:var(--text-secondary)">未找到相似知识点</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.done" style="color:var(--success-color);font-size:var(--fs-xs)">
              ✓ {{ row.count }}题
              <span v-if="row.skippedDup > 0" style="color:var(--warning-color)"> 已跳过{{ row.skippedDup }}题重复</span>
            </span>
            <span v-else-if="row.error" style="color:var(--danger-color);font-size:var(--fs-xs)">{{ row.error }}</span>
            <span v-else style="color:var(--text-secondary);font-size:var(--fs-xs)">待上传</span>
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        v-if="hasUnmatched && !uploading"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top:8px"
      >
        <template #title>
          {{ unmatchedCount }} 个文件未匹配到知识点：请将文件名改为知识点名称，或在上方手动选择兜底分类后上传
        </template>
      </el-alert>
      <div style="margin-top:10px;display:flex;justify-content:flex-end">
        <el-button size="small" @click="clearFiles">清空</el-button>
        <el-button
          type="primary"
          size="small"
          :loading="uploading"
          :disabled="fileList.length === 0"
          @click="uploadAll"
        >
          上传全部 ({{ fileList.filter(f => !f.done).length }})
        </el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button v-if="importResult" type="primary" @click="visible = false; emit('imported')">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCategoryCascade } from '@/composables/useCategoryCascade'
import { importWordQuestions, importWordQuestionsBatch } from '@/api/questionBank'
import { getMySubjects } from '@/api/settings'
import { downloadFile } from '@/utils/request'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin || userStore.isSuperAdmin)

const props = defineProps({
  modelValue: Boolean,
  categoryTree: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'imported'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const mySubjects = ref([])
const teacherSubjectSet = computed(() => new Set(mySubjects.value.map(s => Number(s.id)).filter(Boolean)))
const treeSubjectSet = computed(() => {
  const ids = new Set()
  for (const n of (props.categoryTree || [])) { if (n.level === 1 && n.subjectId) ids.add(n.subjectId) }
  return ids
})
const filteredCategoryTree = computed(() => {
  if (isAdmin.value) return props.categoryTree
  const allowed = new Set([...teacherSubjectSet.value, ...treeSubjectSet.value])
  return props.categoryTree.filter(node => allowed.has(node.subjectId))
})

watch(() => props.modelValue, async (v) => {
  if (v) { reset(); buildKpMap(); try { const r = await getMySubjects(); if (r.code===200) mySubjects.value = r.data||[] } catch { /* */ } }
})

const cascade = useCategoryCascade(filteredCategoryTree)
const { selectedSubjectId: importSubjectId, selectedChapterId: importChapterId, selectedTaskId: importTaskId, selectedKpId: importKpId, chapters, tasks, kps, categoryId, onSubjectChange, onChapterChange, onTaskChange } = cascade

// === 知识点名称→ID 索引（用于文件名自动匹配）===
const kpMap = ref({})  // { "知识点名称": category_id }
const taskMap = ref({}) // { "任务名称": category_id }

const buildKpMap = () => {
  const kp = {}, tk = {}
  const walk = (nodes) => {
    for (const n of nodes) {
      if (n.level === 4 && n.name) kp[n.name] = n.id      // 知识点
      if (n.level === 3 && n.name) tk[n.name] = n.id       // 任务
      if (n.children && n.children.length) walk(n.children)
    }
  }
  walk(props.categoryTree)
  kpMap.value = kp
  taskMap.value = tk
}

const matchFilename = (filename) => {
  const name = filename.replace(/\.docx?$/i, '').trim()
  // 1. 精确匹配知识点
  if (kpMap.value[name]) return { type: 'kp', id: kpMap.value[name], label: name }
  // 2. 精确匹配任务
  if (taskMap.value[name]) return { type: 'task', id: taskMap.value[name], label: name }
  // 3. 去除编号前缀（如 "01", "1.", "1、"）
  const clean = name.replace(/^[\d\s._、.，-]+/, '').trim()
  if (clean !== name && kpMap.value[clean]) return { type: 'kp', id: kpMap.value[clean], label: clean }
  if (clean !== name && taskMap.value[clean]) return { type: 'task', id: taskMap.value[clean], label: clean }
  // 4. 规范化后模糊匹配：忽略空格/下划线/中划线差异
  const norm = (s) => s.replace(/[\s_\-—]+/g, '').toLowerCase()
  const nameNorm = norm(name)
  for (const [kn, kid] of Object.entries(kpMap.value)) {
    if (norm(kn) === nameNorm) return { type: 'kp', id: kid, label: kn }
  }
  for (const [tn, tid] of Object.entries(taskMap.value)) {
    if (norm(tn) === nameNorm) return { type: 'task', id: tid, label: tn }
  }
  // 5. 查找相似知识点作为建议
  const suggestions = []
  for (const kn of Object.keys(kpMap.value)) {
    if (kn.includes(name) || name.includes(kn)) suggestions.push(kn)
    if (suggestions.length >= 3) break
  }
  return { type: null, id: null, label: null, suggestions }
}

const hasUnmatched = computed(() => fileList.some(f => !f.matchedKp && !f.matchedTask && !f.done))
const unmatchedCount = computed(() => fileList.filter(f => !f.matchedKp && !f.matchedTask && !f.done).length)

// === 多文件管理 ===
const uploadRef = ref(null)
const uploading = ref(false)

const fileList = reactive([]) // [{ name, raw, matchedKp, matchedTask, categoryId, done, count, error }]

const onFilesChanged = (uploadFile) => {
  if (!uploadFile || !uploadFile.raw) return
  const match = matchFilename(uploadFile.name)
  fileList.push({
    name: uploadFile.name,
    raw: uploadFile.raw,
    matchedKp: match && match.type === 'kp' ? match.label : null,
    matchedTask: match && match.type === 'task' ? match.label : null,
    suggestions: match && match.suggestions ? match.suggestions : [],
    categoryId: (match && match.id) || categoryId.value,
    done: false,
    count: 0,
    error: null
  })
}

const clearFiles = () => {
  fileList.splice(0, fileList.length)
  uploadRef.value?.clearFiles()
}

const uploadAll = async () => {
  if (uploading.value) return
  const pending = fileList.filter(f => !f.done)
  if (pending.length === 0) return

  uploading.value = true
  let total = 0

  try {
    // 尝试批量接口
    const formData = new FormData()
    pending.forEach(f => formData.append('files', f.raw))
    const mappings = pending.map(f => ({ categoryId: f.categoryId || null }))
    // 如果待上传数量<=1，用单文件接口；否则用批量
    if (pending.length === 1) {
      const fd = new FormData()
      fd.append('file', pending[0].raw)
      if (pending[0].categoryId) fd.append('categoryId', pending[0].categoryId)
      const res = await importWordQuestions(fd)
      if (res.code === 200) {
        pending[0].done = true
        pending[0].count = res.data.imported
        total = res.data.imported
      } else {
        pending[0].error = res.message || '失败'
      }
    } else {
      const res = await importWordQuestionsBatch(formData, JSON.stringify(mappings))
      if (res.code === 200) {
        const fileResults = res.data.files || []
        pending.forEach((f, i) => {
          const fr = i < fileResults.length ? fileResults[i] : null
          if (fr && fr.success !== false) {
            f.done = true
            f.count = fr.imported || 0
            f.skippedDup = fr.skippedDup || 0
          } else {
            f.error = (fr && fr.error) || '未知错误'
          }
        })
        total = res.data.imported
      } else {
        pending.forEach(f => { f.error = res.message || '批量导入失败' })
      }
    }
    ElMessage.success(`导入完成：${total} 道题`)
    emit('imported')
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

const downloadTemplate = () => {
  downloadFile('/question-bank/actions/template/download', '题库导入模板.docx')
    .catch(() => {
      const content = `[单选题] 以下哪个是输出设备？\nA. 键盘\nB. 鼠标\nC. 显示器\nD. 扫描仪\n答案：C\n\n[多选题] 以下哪些是偶数？\nA. 2\nB. 3\nC. 4\nD. 5\n答案：A,C\n\n[判断题] CPU是计算机的核心部件。\n答案：T\n`
      const blob = new Blob([content], { type: 'text/plain' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = '题库导入模板.txt'; a.click()
      window.URL.revokeObjectURL(url)
      ElMessage.info('后端模板暂不可用，已生成本地文本模板')
    })
}

const reset = () => {
  fileList.splice(0, fileList.length)
  cascade.reset()
}

defineExpose({ reset })
</script>

<style scoped>
.import-category-section {
  background: var(--bg-section); border: 1px solid var(--border-light); border-radius: var(--radius-md);
  padding: 14px 16px; margin-bottom: 16px;
}
.import-section-title {
  font-size: var(--fs-sm); font-weight: 600; color: var(--text-regular); margin-bottom: 10px;
  display: flex; align-items: center; gap: 6px;
}
.import-section-title::before {
  content: ''; display: inline-block; width: 4px; height: 14px;
  background: var(--primary-color); border-radius: 2px;
}
.import-category-row { display: flex; align-items: center; gap: 0; }
.import-category-row .el-select { flex: 1; min-width: 0; }
.import-arrow {
  flex-shrink: 0; width: 28px; text-align: center;
  font-size: var(--fs-lg); color: var(--text-secondary); line-height: 1;
}

@media (max-width: 768px) {
  :deep(.el-table .el-table__body-wrapper) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
