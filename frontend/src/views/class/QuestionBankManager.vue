<template>
  <div class="qbm-container">
    <!-- 工具栏 -->
    <div class="qbm-toolbar">
      <div class="qbm-filter">
        <el-select
          v-model="filter.subject"
          placeholder="学科"
          size="small"
          style="width:140px"
          clearable
          filterable
          @change="loadData"
        >
          <el-option
            v-for="s in filterOpts.subjects"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
        <el-select
          v-model="filter.chapter"
          placeholder="章节"
          size="small"
          style="width:140px"
          clearable
          filterable
          @change="loadData"
        >
          <el-option
            v-for="c in filterOpts.chapters"
            :key="c"
            :label="c"
            :value="c"
          />
        </el-select>
        <el-select
          v-model="filter.tag"
          placeholder="标签"
          size="small"
          style="width:120px"
          clearable
          filterable
          @change="loadData"
        >
          <el-option
            v-for="t in filterOpts.tags"
            :key="t"
            :label="t"
            :value="t"
          />
        </el-select>
        <el-input
          v-model="filter.keyword"
          placeholder="搜索题目..."
          size="small"
          style="width:180px"
          clearable
          @keyup.enter="loadData"
        />
        <el-button size="small" type="primary" @click="loadData"><el-icon><Search /></el-icon> 搜索</el-button>
      </div>
      <div class="qbm-actions">
        <el-button size="small" type="primary" @click="openAdd"><el-icon><Plus /></el-icon> 手动添加</el-button>
        <el-upload :show-file-list="false" :http-request="handleExcelUpload" accept=".xlsx,.xls">
          <el-button size="small" :loading="excelUploading"><el-icon><Upload /></el-icon> Excel导入</el-button>
        </el-upload>
        <el-popover trigger="click" :width="340" placement="bottom-start">
          <template #reference>
            <el-button size="small"><el-icon><Upload /></el-icon> txt上传</el-button>
          </template>
          <div class="qbm-popover-form">
            <div class="qbm-popover-row">
              <span>学科：</span><el-input
                v-model="txtForm.subject"
                size="small"
                placeholder="如：计算机基础"
                style="width:150px"
              />
            </div>
            <div class="qbm-popover-row">
              <span>类别：</span><el-input
                v-model="txtForm.tag"
                size="small"
                placeholder="如：课堂提问"
                style="width:150px"
              />
            </div>
            <div class="qbm-popover-row">
              <span>章节：</span><el-input
                v-model="txtForm.chapter"
                size="small"
                placeholder="可选"
                style="width:150px"
              />
            </div>
            <div class="qbm-popover-hint">txt 每行一道题目</div>
            <el-upload :show-file-list="false" :http-request="handleTxtUpload" accept=".txt">
              <el-button size="small" type="primary" :loading="txtUploading">选择文件并上传</el-button>
            </el-upload>
          </div>
        </el-popover>
        <el-button size="small" @click="openImportFromBank"><el-icon><Files /></el-icon> 从题库选取</el-button>
        <el-button size="small" type="warning" @click="showAiDialog = true">AI出题</el-button>
      </div>
    </div>

    <!-- 题目表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      size="small"
      class="qbm-table"
      empty-text="暂无题目，请手动添加或从题库导入"
    >
      <el-table-column prop="id" label="#" width="55" />
      <el-table-column
        prop="content"
        label="题目内容"
        min-width="240"
        show-overflow-tooltip
      />
      <el-table-column prop="tag" label="标签" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.tag" size="small" type="info">{{ row.tag }}</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="subject" label="学科" width="110">
        <template #default="{ row }"><span>{{ row.subject || '-' }}</span></template>
      </el-table-column>
      <el-table-column prop="chapter" label="章节" width="100">
        <template #default="{ row }"><span>{{ row.chapter || '-' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            text
            type="primary"
            @click="openEdit(row)"
          >
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-popconfirm title="确定删除该题目？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" text type="danger"><el-icon><Delete /></el-icon> 删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="total > 0" class="qbm-pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        size="small"
        @current-change="loadData"
      />
    </div>

    <!-- 添加/编辑弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑题目' : '添加题目'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="dialog.form" label-width="60px" size="small">
        <el-form-item label="题目">
          <el-input
            v-model="dialog.form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入题目内容..."
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="dialog.form.tag" placeholder="如：选择题、简答题" />
        </el-form-item>
        <el-form-item label="学科">
          <el-select
            v-model="dialog.form.subject"
            placeholder="选择学科"
            clearable
            filterable
            style="width:100%"
          >
            <el-option
              v-for="s in filterOpts.subjects"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="章节">
          <el-input v-model="dialog.form.chapter" placeholder="如：第一章" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input
            v-model="dialog.form.answer"
            type="textarea"
            :rows="2"
            placeholder="参考答案（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 从题库选取弹窗 -->
    <el-dialog
      v-model="importDialog.visible"
      title="从题库选取题目"
      width="720px"
      destroy-on-close
    >
      <div class="qbm-import-filter">
        <el-select
          v-model="importDialog.filter.subject"
          placeholder="学科"
          size="small"
          style="width:140px"
          clearable
          filterable
          @change="loadBankQuestions"
        >
          <el-option
            v-for="s in importSubjects"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
        <el-input
          v-model="importDialog.filter.keyword"
          placeholder="搜索题目"
          size="small"
          style="width:200px"
          clearable
          @keyup.enter="loadBankQuestions"
        />
        <el-button size="small" type="primary" @click="loadBankQuestions"><el-icon><Search /></el-icon> 搜索</el-button>
      </div>
      <el-table
        v-loading="importDialog.loading"
        :data="importDialog.questions"
        stripe
        size="small"
        max-height="380"
        @selection-change="importDialog.selected = $event"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="id" label="#" width="55" />
        <el-table-column
          prop="questionText"
          label="题目内容"
          min-width="260"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.questionText || row.content || '-' }}</template>
        </el-table-column>
        <el-table-column prop="subject" label="学科" width="120" />
      </el-table>
      <div v-if="importDialog.total > 0" class="qbm-import-pagination">
        <el-pagination
          v-model:current-page="importDialog.page"
          :page-size="importDialog.pageSize"
          :total="importDialog.total"
          layout="prev, pager, next"
          size="small"
          @current-change="loadBankQuestions"
        />
      </div>
      <template #footer>
        <el-button @click="importDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="importDialog.importing"
          :disabled="!importDialog.selected.length"
          @click="handleImportFromBank"
        >
          导入选中 ({{ importDialog.selected.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- AI出题对话框 -->
    <el-dialog
      v-model="showAiDialog"
      title="AI 生成题目"
      width="420px"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="学科">
          <el-select
            v-model="aiForm.subject"
            style="width:100%"
            placeholder="选择学科"
            filterable
          >
            <el-option
              v-for="s in filterOpts.subjects"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="aiForm.knowledgePoint" placeholder="输入知识点名称" />
        </el-form-item>
        <el-form-item label="题目数量">
          <el-slider
            v-model="aiForm.count"
            :min="3"
            :max="8"
            show-stops
            :step="1"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAiDialog = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="doAiGenerate">生成并入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getQuestions, addQuestion, updateQuestion, deleteQuestion, batchImportExcel, importFromQuestionBank, getQuestionFilters, batchImportTxt, aiGenerateQuiz } from '@/api/classroom'
import { getQuestionBankList } from '@/api/questionBank'
import { getSubjects } from '@/api/settings'
import { ElMessage } from 'element-plus'
import { Search, Plus, Upload, Files, Edit, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filter = reactive({ subject: '', chapter: '', tag: '', keyword: '' })
const filterOpts = reactive({ subjects: [], chapters: [], tags: [] })

const dialog = reactive({
  visible: false, isEdit: false, loading: false,
  form: { id: null, content: '', tag: '', subject: '', chapter: '', answer: '' }
})

const openAdd = () => {
  dialog.isEdit = false
  dialog.form = { id: null, content: '', tag: '', subject: '', chapter: '', answer: '' }
  dialog.visible = true
}
const openEdit = (row) => { dialog.isEdit = true; dialog.form = { ...row }; dialog.visible = true }

const handleSave = async () => {
  if (!dialog.form.content.trim()) return ElMessage.warning('请输入题目内容')
  dialog.loading = true
  try {
    const data = { ...dialog.form }; delete data.id
    let res
    if (dialog.isEdit) { res = await updateQuestion(dialog.form.id, data) }
    else { res = await addQuestion(data) }
    if (res.code === 200) { ElMessage.success(dialog.isEdit ? '更新成功' : '添加成功'); dialog.visible = false; loadData(); loadFilters() }
    else { ElMessage.error(res.message || '操作失败') }
  } catch { ElMessage.error('请求失败') } finally { dialog.loading = false }
}

const handleDelete = async (id) => {
  try {
    const res = await deleteQuestion(id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData(); loadFilters() }
    else { ElMessage.error(res.message || '删除失败') }
  } catch { ElMessage.error('请求失败') }
}

const excelUploading = ref(false)
const handleExcelUpload = async (options) => {
  excelUploading.value = true
  try {
    const formData = new FormData(); formData.append('file', options.file)
    const res = await batchImportExcel(formData)
    if (res.code === 200) { ElMessage.success('成功导入题目'); loadData(); loadFilters() }
    else { ElMessage.error(res.message || '导入失败') }
  } catch { ElMessage.error('文件上传失败') } finally { excelUploading.value = false }
}

const txtUploading = ref(false)
const txtForm = reactive({ subject: '', chapter: '', tag: '' })

const showAiDialog = ref(false)
const aiLoading = ref(false)
const aiForm = reactive({ subject: '', knowledgePoint: '', count: 5 })

const doAiGenerate = async () => {
  if (!aiForm.knowledgePoint.trim()) return ElMessage.warning('请输入知识点')
  aiLoading.value = true
  try {
    const res = await aiGenerateQuiz(aiForm)
    if (res.code === 200) { ElMessage.success(`AI 生成了 ${(res.data || []).length} 道题目并已入库`); showAiDialog.value = false; loadData() }
    else { ElMessage.error(res.message || '生成失败') }
  } catch { ElMessage.error('AI出题失败') } finally { aiLoading.value = false }
}

const handleTxtUpload = async (options) => {
  txtUploading.value = true
  try {
    const fd = new FormData(); fd.append('file', options.file)
    if (txtForm.subject) fd.append('subject', txtForm.subject)
    if (txtForm.chapter) fd.append('chapter', txtForm.chapter)
    if (txtForm.tag) fd.append('tag', txtForm.tag)
    const res = await batchImportTxt(fd)
    if (res.code === 200) { ElMessage.success(`成功导入 ${(res.data || []).length} 道题目`); loadData(); loadFilters() }
    else { ElMessage.error(res.message || '导入失败') }
  } catch { ElMessage.error('上传失败') } finally { txtUploading.value = false }
}

const importSubjects = ref([])
const importDialog = reactive({
  visible: false, loading: false, importing: false,
  questions: [], selected: [], page: 1, pageSize: 15, total: 0,
  filter: { subject: '', keyword: '' }
})

const openImportFromBank = () => {
  importDialog.visible = true; importDialog.selected = []; importDialog.page = 1
  importDialog.filter.subject = ''; importDialog.filter.keyword = ''
  loadImportSubjects(); loadBankQuestions()
}

const loadImportSubjects = async () => {
  try { const res = await getSubjects(); if (res.code === 200) importSubjects.value = (res.data || []).map(s => s.name || s).filter(Boolean) } catch { /* */ }
}

const loadBankQuestions = async () => {
  importDialog.loading = true
  try {
    const params = { page: importDialog.page, pageSize: importDialog.pageSize }
    if (importDialog.filter.subject) params.subject = importDialog.filter.subject
    if (importDialog.filter.keyword) params.keyword = importDialog.filter.keyword
    const res = await getQuestionBankList(params)
    if (res.code === 200) {
      importDialog.questions = (res.data && res.data.records) ? res.data.records : (res.data || [])
      importDialog.total = (res.data && res.data.total) ? res.data.total : importDialog.questions.length
    }
  } catch { ElMessage.error('加载题库失败') } finally { importDialog.loading = false }
}

const handleImportFromBank = async () => {
  if (!importDialog.selected.length) return
  importDialog.importing = true
  try {
    const ids = importDialog.selected.map(q => q.id)
    const res = await importFromQuestionBank({ questionIds: ids })
    if (res.code === 200) { ElMessage.success(`成功导入 ${ids.length} 道题目`); importDialog.visible = false; loadData(); loadFilters() }
    else { ElMessage.error(res.message || '导入失败') }
  } catch { ElMessage.error('请求失败') } finally { importDialog.importing = false }
}

const loadFilters = async () => {
  try {
    const res = await getQuestionFilters()
    if (res.code === 200) { filterOpts.subjects = res.data.subjects || []; filterOpts.chapters = res.data.chapters || []; filterOpts.tags = res.data.tags || [] }
  } catch { /* */ }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filter.subject) params.subject = filter.subject
    if (filter.chapter) params.chapter = filter.chapter
    if (filter.tag) params.tag = filter.tag
    if (filter.keyword) params.keyword = filter.keyword
    const res = await getQuestions(params)
    if (res.code === 200) { tableData.value = (res.data && res.data.records) ? res.data.records : (res.data || []); total.value = (res.data && res.data.total) ? res.data.total : tableData.value.length }
  } catch { ElMessage.error('加载题目失败') } finally { loading.value = false }
}

onMounted(() => { loadFilters(); loadData() })
</script>

<style scoped lang="scss">
.qbm-container { display: flex; flex-direction: column; height: 100%; }

.qbm-toolbar {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: 12px; margin-bottom: 14px; flex-wrap: wrap;
}

.qbm-filter { display: flex; gap: var(--spacing-sm); flex-wrap: wrap; align-items: center; }
.qbm-actions { display: flex; gap: var(--spacing-sm); flex-shrink: 0; }

.qbm-table { flex: 1; min-height: 0; }
.qbm-pagination { display: flex; justify-content: center; padding: 14px 0 0; }
.qbm-import-filter { display: flex; gap: var(--spacing-sm); margin-bottom: 14px; align-items: center; }
.qbm-import-pagination { display: flex; justify-content: center; padding-top: 12px; }

// popover 内表单
.qbm-popover-form { display: flex; flex-direction: column; gap: var(--spacing-sm); }
.qbm-popover-row { display: flex; align-items: center; gap: var(--spacing-xs); font-size: var(--fs-sm); }
.qbm-popover-hint { font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 2px; }

.text-muted { color: var(--text-secondary); font-size: var(--fs-xs); }

@media (max-width: 768px) {
  .qbm-toolbar { flex-direction: column; }
  .qbm-actions { width: 100%; flex-wrap: wrap; }
  .qbm-filter { width: 100%; flex-direction: column; }
  .qbm-filter :deep(.el-select), .qbm-filter :deep(.el-input) { width: 100% !important; }
}
</style>
