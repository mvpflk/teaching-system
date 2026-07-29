<template>
  <div class="esm-page">
    <div class="esm-toolbar">
      <el-select
        v-model="filterSubjectId"
        placeholder="全部学科"
        clearable
        size="default"
        style="width:180px"
        @change="loadList"
      >
        <el-option
          v-for="s in subjects"
          :key="s.id"
          :label="s.subjectName"
          :value="s.id"
        />
      </el-select>
      <el-select
        v-model="filterExamType"
        placeholder="全部类型"
        clearable
        size="default"
        style="width:140px"
        @change="loadList"
      >
        <el-option label="通用" value="GENERAL" />
        <el-option label="单招" value="SINGLE_RECRUIT" />
        <el-option label="对口升学" value="COUNTERPART" />
      </el-select>
      <el-button type="primary" size="default" @click="openDialog(null)"><el-icon><Plus /></el-icon> 新建考纲</el-button>
      <el-upload
        :show-file-list="false"
        :http-request="handleFileUpload"
        accept=".md,.txt"
        style="display:inline-block;margin-left:8px"
      >
        <el-button size="default"><el-icon><Upload /></el-icon> 上传MD文件</el-button>
      </el-upload>
    </div>

    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      size="default"
    >
      <el-table-column prop="subjectId" label="学科" width="120">
        <template #default="{ row }">{{ getSubjectName(row.subjectId) }}</template>
      </el-table-column>
      <el-table-column
        prop="title"
        label="考纲标题"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column prop="examType" label="考试类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="examTypeTag(row.examType)">{{ examTypeLabel(row.examType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="knowledgeDim" label="维度" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ dimLabel(row.knowledgeDim) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="关联知识点" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="(row._nodeCount||0)>0?'success':'info'">{{ row._nodeCount || 0 }} 个</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            size="small"
            @change="toggle(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="150">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="preview(row)">预览</el-button>
          <el-button text size="small" @click="openDialog(row)">编辑</el-button>
          <el-button
            text
            size="small"
            type="danger"
            @click="del(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :total="total"
      :page-size="pageSize"
      layout="total,prev,next"
      style="margin-top:16px;justify-content:flex-end"
      @change="loadList"
    />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑考纲' : '新建考纲'"
      width="700px"
      destroy-on-close
    >
      <el-form :model="form" label-width="90px" size="default">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学科" required>
              <el-select v-model="form.subjectId" placeholder="选择学科" style="width:100%">
                <el-option
                  v-for="s in subjects"
                  :key="s.id"
                  :label="s.subjectName"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试类型">
              <el-select v-model="form.examType" style="width:100%">
                <el-option label="通用" value="GENERAL" />
                <el-option label="单招" value="SINGLE_RECRUIT" />
                <el-option label="对口升学" value="COUNTERPART" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="维度">
              <el-select v-model="form.knowledgeDim" style="width:100%">
                <el-option label="应知" value="THEORY" />
                <el-option label="应会" value="PRACTICE" />
                <el-option label="综合" value="BOTH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本">
              <el-input v-model="form.version" placeholder="如 1.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="考纲标题" />
        </el-form-item>
        <el-form-item label="考纲内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="Markdown格式考纲内容"
          />
        </el-form-item>
        <el-form-item label="关联知识点">
          <div style="max-height:200px;overflow-y:auto;border:1px solid var(--border-color);border-radius:6px;padding:8px">
            <el-tree
              ref="nodeTreeRef"
              :data="nodeTreeData"
              node-key="id"
              show-checkbox
              :props="{label:'name',children:'children'}"
              :check-strictly="false"
              :default-checked-keys="formNodeIds"
              empty-text="加载中..."
              @check="onNodeCheck"
            >
              <template #default="{ data }">
                <span>{{ data.name }}</span>
                <el-tag size="small" style="margin-left:4px" :type="data.level===1?'':(data.level===2?'success':(data.level===3?'warning':'info'))">
                  {{ ['','学科','章节','任务','知识点'][data.level||0] }}
                </el-tag>
              </template>
            </el-tree>
          </div>
          <span class="esm-hint">勾选后，AI生成时将精确引用对应考纲条目</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="考纲预览" width="750px">
      <div class="esm-preview" v-html="previewHtml" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listSyllabus, createSyllabus, updateSyllabus, deleteSyllabus, toggleSyllabusStatus, uploadSyllabusFile, getSyllabusNodeIds, saveSyllabusNodeRelations } from '@/api/examSyllabus'
import { getMySubjects } from '@/api/settings'
import { getNodeTree } from '@/api/knowledgeNode'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'

const subjects = ref([])
const tableData = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1); const pageSize = ref(20); const total = ref(0)
const filterSubjectId = ref(null); const filterExamType = ref(null)
const dialogVisible = ref(false); const editId = ref(null)
const previewVisible = ref(false); const previewHtml = ref('')
const form = ref({ subjectId: null, examType: 'GENERAL', knowledgeDim: 'BOTH', title: '', content: '', version: '1.0' })
const nodeTreeRef = ref(null)
const nodeTreeData = ref([])
const formNodeIds = ref([])

const onNodeCheck = (_node, checked) => {
  formNodeIds.value = checked.checkedKeys || []
}

const examTypeLabel = (t) => ({ GENERAL: '通用', SINGLE_RECRUIT: '单招', COUNTERPART: '对口升学' }[t] || t)
const examTypeTag = (t) => ({ GENERAL: 'info', SINGLE_RECRUIT: 'warning', COUNTERPART: 'success' }[t] || 'info')
const dimLabel = (d) => ({ THEORY: '应知', PRACTICE: '应会', BOTH: '综合' }[d] || d)
const getSubjectName = (id) => subjects.value.find(s => s.id === id)?.subjectName || ''
const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }) : ''

const loadList = async () => {
  loading.value = true
  try {
    const res = await listSyllabus({ subjectId: filterSubjectId.value, examType: filterExamType.value, page: page.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.records || []; total.value = res.data.total || 0
      // 异步加载节点计数
      for (const row of tableData.value) {
        getSyllabusNodeIds(row.id).then(r => { row._nodeCount = (r.code===200 && r.data) ? r.data.length : 0 }).catch(() => {})
      }
    }
  } catch { /* */ } finally { loading.value = false }
}

const loadSubjects = async () => {
  try { const res = await getMySubjects(); if (res.code === 200) subjects.value = res.data || [] } catch { /* */ }
}

const openDialog = async (row) => {
  if (row) {
    editId.value = row.id; form.value = { ...row }
    try {
      const res = await getSyllabusNodeIds(row.id)
      formNodeIds.value = (res.code === 200 && res.data) ? res.data : []
    } catch { formNodeIds.value = [] }
  } else {
    editId.value = null; form.value = { subjectId: filterSubjectId.value, examType: 'GENERAL', knowledgeDim: 'BOTH', title: '', content: '', version: '1.0' }
    formNodeIds.value = []
  }
  if (nodeTreeData.value.length === 0) {
    try { const res = await getNodeTree(); if (res.code === 200) nodeTreeData.value = res.data || [] } catch { /* */ }
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.subjectId || !form.value.title || !form.value.content) return ElMessage.warning('请填写必填项')
  saving.value = true
  try {
    const res = editId.value ? await updateSyllabus(editId.value, form.value) : await createSyllabus(form.value)
    if (res.code === 200) {
      const savedId = editId.value || res.data?.id
      if (savedId) await saveSyllabusNodeRelations(savedId, formNodeIds.value)
      ElMessage.success('已保存'); dialogVisible.value = false; loadList()
    } else { ElMessage.error(res.message) }
  } catch { /* */ } finally { saving.value = false }
}

const del = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '确认', { type: 'warning' })
    await deleteSyllabus(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch { /* cancelled */ }
}

const toggle = async (row) => {
  try { await toggleSyllabusStatus(row.id) } catch { row.status = row.status === 1 ? 0 : 1 }
}

const preview = (row) => {
  previewHtml.value = renderMarkdown(row.content)
  previewVisible.value = true
}

const handleFileUpload = async (options) => {
  if (!filterSubjectId.value) return ElMessage.warning('请先在上方选择一个学科')
  try {
    const res = await uploadSyllabusFile(filterSubjectId.value, filterExamType.value || 'GENERAL', options.file)
    if (res.code === 200) { ElMessage.success('考纲文件已导入'); loadList() } else { ElMessage.error(res.message || '导入失败') }
  } catch { ElMessage.error('上传失败') }
}

onMounted(() => { loadSubjects(); loadList() })
</script>

<style scoped>
.esm-page { padding: 4px 0; }
.esm-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.esm-preview { max-height: 60vh; overflow-y: auto; padding: 16px; background: var(--bg-hover); border-radius: 6px; line-height: 1.7; }
.esm-preview :deep(h1),.esm-preview :deep(h2),.esm-preview :deep(h3) { margin-top: 16px; }
.esm-preview :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; }
.esm-preview :deep(th),.esm-preview :deep(td) { border: 1px solid var(--border-color); padding: 6px 10px; text-align: left; }
.esm-preview :deep(th) { background: var(--bg-secondary); }
</style>
