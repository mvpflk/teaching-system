<template>
  <el-dialog
    v-model="visible"
    title="从Excel导入题目"
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
    <div style="margin-bottom:12px">
      <el-button
        text
        type="primary"
        size="small"
        @click="downloadTemplate"
      >
        <el-icon><Download /></el-icon>下载Excel模板
      </el-button>
    </div>
    <el-upload
      drag
      :auto-upload="false"
      :on-change="onFileChange"
      accept=".xlsx"
      :limit="1"
    >
      <el-icon class="el-icon--upload" size="40"><UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽 Excel 文件到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip">
          仅支持 .xlsx 格式<br>
          模板列：题型（SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/FILL_IN/SHORT_ANSWER）、题干、选项A~E、正确答案、解析、学科
        </div>
      </template>
    </el-upload>
    <div v-if="importResult" class="import-result">
      <el-divider />
      <el-alert :title="'解析完成！成功导入 ' + importResult.imported + ' 道题'" type="success" :closable="false" />
      <el-table
        :data="importResult.questions"
        size="small"
        max-height="300"
        style="margin-top:10px"
      >
        <el-table-column label="题型" width="55">
          <template #default="{ row }">{{ QUESTION_TYPE_LABEL[row.questionType] }}</template>
        </el-table-column>
        <el-table-column
          prop="questionText"
          label="题干"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column prop="correctAnswer" label="答案" width="80" />
      </el-table>
    </div>
    <div v-if="pendingFile && !importResult" style="margin-top:12px;display:flex;justify-content:flex-end">
      <el-button type="primary" size="small" @click="doImport">确认导入</el-button>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button v-if="importResult" type="primary" @click="visible = false; emit('imported')">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCategoryCascade } from '@/composables/useCategoryCascade'
import { importExcelQuestions } from '@/api/questionBank'
import { getMySubjects } from '@/api/settings'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'
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
  if (v) {
    reset()
    try { const r = await getMySubjects(); if (r.code===200) mySubjects.value = r.data||[] } catch { /* */ }
  }
})

const cascade = useCategoryCascade(filteredCategoryTree)
const { selectedSubjectId: importSubjectId, selectedChapterId: importChapterId, selectedTaskId: importTaskId, selectedKpId: importKpId, chapters, tasks, kps, categoryId, onSubjectChange, onChapterChange, onTaskChange } = cascade

const importResult = ref(null)

const downloadTemplate = () => {
  downloadFile('/question-bank/actions/excel-template/download', '题库导入模板.xlsx')
    .catch(() => ElMessage.error('下载模板失败'))
}

const pendingFile = ref(null)

const onFileChange = (uploadFile) => {
  pendingFile.value = uploadFile
}

const doImport = async () => {
  if (!pendingFile.value) { ElMessage.warning('请先选择文件'); return }
  const formData = new FormData()
  formData.append('file', pendingFile.value.raw)
  if (categoryId.value) formData.append('categoryId', categoryId.value)
  try {
    const res = await importExcelQuestions(formData)
    if (res.code === 200) {
      importResult.value = res.data
      ElMessage.success(res.message || '导入成功')
    } else {
      ElMessage.error(res.message || '解析失败')
    }
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.message || '未知错误'))
  }
}

const reset = () => {
  importResult.value = null
  pendingFile.value = null
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
