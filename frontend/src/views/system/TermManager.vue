<template>
  <div class="term-manager">
    <div class="page-header">
      <h3 class="page-title">学期管理</h3>
      <span class="page-subtitle">设置学年学期，关联年级入学批次，当前学期唯一</span>
    </div>

    <div style="margin-bottom:12px">
      <el-button type="primary" size="default" @click="openDialog()">新增学期</el-button>
    </div>

    <div v-if="loading" class="sk-list"><div v-for="i in 4" :key="i" class="sk-row"><div class="sk-line w-40"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div></div></div>
    <el-table v-else :data="terms" stripe>
      <el-table-column prop="name" label="学期名称" min-width="160" />
      <el-table-column label="开始日期" width="120" align="center">
        <template #default="{ row }">{{ row.startDate }}</template>
      </el-table-column>
      <el-table-column label="结束日期" width="120" align="center">
        <template #default="{ row }">{{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="当前学期" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isCurrent === 1 ? 'success' : 'info'" size="small">
            {{ row.isCurrent === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button
            size="small"
            text
            type="primary"
            @click="openDialog(row)"
          >
            编辑
          </el-button>
          <el-button
            size="small"
            text
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="visible"
      :title="form.id ? '编辑学期' : '新增学期'"
      width="460px"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="学期名称" prop="name">
          <el-input v-model="form.name" placeholder="如：2024-2025学年上学期" maxlength="50" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                placeholder="选择"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                placeholder="选择"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="设为当前学期">
          <el-switch v-model="form.isCurrent" :active-value="1" :inactive-value="0" />
          <span style="margin-left:8px;font-size:var(--fs-xs);color:var(--text-secondary)">同一时间只有一个当前学期</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTerms, createTerm, updateTerm, deleteTerm } from '@/api/settings'

const terms = ref([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, name: '', startDate: '', endDate: '', isCurrent: 0 })

const rules = {
  name: [{ required: true, message: '请输入学期名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
}

const load = async () => {
  loading.value = true
  try { const r = await getTerms(); if (r.code === 200) terms.value = r.data || [] }
  catch { ElMessage.error('加载学期列表失败') } finally { loading.value = false }
}

const openDialog = (row) => {
  if (row) Object.assign(form, { id: row.id, name: row.name, startDate: row.startDate, endDate: row.endDate, isCurrent: row.isCurrent })
  else Object.assign(form, { id: null, name: '', startDate: '', endDate: '', isCurrent: 0 })
  visible.value = true
}

const save = async () => {
  if (saving.value) return
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    const data = { name: form.name, startDate: form.startDate, endDate: form.endDate, isCurrent: form.isCurrent }
    const r = form.id ? await updateTerm(form.id, data) : await createTerm(data)
    if (r.code === 200) { ElMessage.success(form.id ? '已更新' : '已创建'); visible.value = false; load() }
    else ElMessage.error(r.message || '保存失败')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const handleDelete = async (row) => {
  try { await ElMessageBox.confirm(`删除学期「${row.name}」？`, '确认', { type: 'warning' }) } catch { return }
  try { const r = await deleteTerm(row.id); if (r.code === 200) { ElMessage.success('已删除'); load() } }
  catch { ElMessage.error('删除失败') }
}

onMounted(load)
</script>

<style scoped>
.term-manager { max-width: 800px; }
.page-header { margin-bottom: 16px; }
.page-title { font-size: var(--fs-xl); margin: 0 0 4px; }
.page-subtitle { font-size: var(--fs-xs); color: var(--text-secondary); }

.sk-list { padding: 8px 0; }
.sk-row { display: flex; gap: 16px; padding: 16px 12px; border-bottom: 1px solid var(--border-light); }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); position: relative; overflow: hidden; }
.sk-line::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { transform: translateX(-100%) } 100% { transform: translateX(100%) } }
.w-20 { width: 20% } .w-40 { width: 40% }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>
