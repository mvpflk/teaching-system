<template>
  <div class="text-mgr">
    <div class="page-header">
      <h3>打字文本管理</h3>
      <el-button type="primary" @click="openDialog(null)">新增文本</el-button>
    </div>

    <div class="filters">
      <el-input
        v-model="keyword"
        placeholder="搜索标题"
        clearable
        style="width:200px"
        @change="loadData"
      />
      <el-select
        v-model="filterType"
        placeholder="类型"
        clearable
        style="width:140px"
        @change="loadData"
      >
        <el-option label="练习" value="practice" />
        <el-option label="竞赛" value="competition" />
      </el-select>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="sk-list">
      <div v-for="i in 5" :key="i" class="sk-row">
        <div class="sk-line w-8" style="height:14px"></div>
        <div class="sk-line w-20" style="height:14px"></div>
        <div class="sk-line w-35" style="height:14px"></div>
        <div class="sk-line w-10" style="height:14px"></div>
        <div class="sk-line w-8" style="height:14px"></div>
      </div>
    </div>

    <el-table
      v-else
      :data="records"
      stripe
      border
    >
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="content" label="内容预览" min-width="200">
        <template #default="{row}">{{ row.content?.substring(0, 60) }}{{ row.content?.length > 60 ? '...' : '' }}</template>
      </el-table-column>
      <el-table-column prop="language" label="语言" width="80" />
      <el-table-column prop="category" label="分类" width="90" />
      <el-table-column prop="difficulty" label="难度" width="70" />
      <el-table-column prop="type" label="类型" width="90">
        <template #default="{row}">
          <el-tag :type="row.type==='competition'?'warning':''" size="small">{{ row.type==='competition'?'竞赛':'练习' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :total="total"
      :page-size="size"
      layout="total,prev,next"
      class="mt-16"
      @current-change="loadData"
    />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑文本' : '新增文本'"
      width="640px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="输入文本标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="输入打字原文内容"
          />
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="form.language">
            <el-option label="英文" value="en" />
            <el-option label="中文" value="zh" />
            <el-option label="混合" value="mixed" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="form.difficulty" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="如: 编程、文学、日常" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="练习" value="practice" />
            <el-option label="竞赛" value="competition" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTexts, createText, updateText, deleteText } from '@/api/typing'

const loading = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterType = ref('')
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = ref({ title: '', content: '', language: 'mixed', difficulty: 1, category: '', type: 'practice' })
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTexts({ page: page.value, size: size.value, type: filterType.value, keyword: keyword.value })
    if (res.code === 200) { records.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  loading.value = false
}

function openDialog(row) {
  if (row) {
    editing.value = row
    form.value = { ...row }
  } else {
    editing.value = null
    form.value = { title: '', content: '', language: 'mixed', difficulty: 1, category: '', type: 'practice' }
  }
  dialogVisible.value = true
}

function resetForm() {
  editing.value = null
  form.value = { title: '', content: '', language: 'mixed', difficulty: 1, category: '', type: 'practice' }
}

async function handleSave() {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    if (editing.value) {
      await updateText(editing.value.id, form.value)
      ElMessage.success('已更新')
    } else {
      await createText(form.value)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    loadData()
  } catch { ElMessage.error('保存失败') }
  saving.value = false
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」？`, '确认', { type: 'warning' })
  try {
    await deleteText(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* canceled or error */ }
}

onMounted(loadData)
</script>

<style scoped>
.text-mgr { max-width: 1100px; margin: 0 auto; padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.filters { display: flex; gap: 12px; margin-bottom: 16px; }

.sk-list { padding: 8px 0; }
.w-8 { width: 8% } .w-10 { width: 10% } .w-20 { width: 20% } .w-35 { width: 35% }

@media (max-width: 768px) {
  .text-mgr { padding: 8px; }
  .filters { flex-direction: column; }
  .filters .el-input, .filters .el-select { width: 100% !important; }
}
</style>
