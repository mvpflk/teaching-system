<template>
  <div class="credit-rules-page">
    <div class="page-header">
      <h3 class="page-title">积分规则管理</h3>
      <span class="page-subtitle">管理积分获取与消耗规则，修改即时生效</span>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-row :gutter="12" align="middle">
        <el-col :xs="24" :sm="6">
          <el-input
            v-model="filterKey"
            placeholder="搜索规则键"
            clearable
            size="default"
            @input="onFilter"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-input
            v-model="filterDesc"
            placeholder="搜索描述"
            clearable
            size="default"
            @input="onFilter"
          />
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-button type="primary" size="default" @click="openCreate">
            <el-icon><Plus /></el-icon> 新增规则
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="pagedList" stripe>
      <el-table-column label="规则键" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ RULE_CODE_LABEL[row.ruleCode] || row.ruleCode }}</template>
      </el-table-column>
      <el-table-column
        prop="ruleName"
        label="行为描述"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column
        prop="creditValue"
        label="积分值"
        width="80"
        align="center"
      >
        <template #default="{ row }">
          <span :style="{ color: row.creditValue > 0 ? 'var(--success-color)' : 'var(--danger-color)', fontWeight:600 }">
            {{ row.creditValue > 0 ? '+' : '' }}{{ row.creditValue }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        prop="actionType"
        label="适用角色"
        width="110"
        align="center"
      />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            active-color="var(--success-color)"
            size="small"
            @change="toggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="180"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            size="small"
            text
            type="primary"
            @click="openEdit(row)"
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

    <!-- 分页 -->
    <div v-if="filteredList.length > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="filteredList.length"
        layout="prev, pager, next"
        size="default"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="formTitle"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        class="rule-form"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则键 (rule_code)" prop="ruleCode">
              <el-input v-model="form.ruleCode" placeholder="如 HOMEWORK_SUBMIT" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行为名称" prop="ruleName">
              <el-input v-model="form.ruleName" placeholder="如 提交作业" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="积分值">
              <el-input-number
                v-model="form.creditValue"
                :min="-100"
                :max="100"
                controls-position="right"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="每日上限">
              <el-input-number
                v-model="form.maxDailyCount"
                :min="0"
                :max="999"
                controls-position="right"
                style="width:100%"
                placeholder="不限"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="form.actionType" style="width:100%">
                <el-option value="task" label="任务" />
                <el-option value="homework" label="作业(旧)" />
                <el-option value="exam" label="考试(旧)" />
                <el-option value="sign" label="签到" />
                <el-option value="assist" label="互助" />
                <el-option value="daily" label="每日" />
                <el-option value="BEHAVIOR" label="德育" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="规则说明"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFormRules } from '@/composables/useFormRules'
import { getAdminRules, createAdminRule, updateAdminRule, deleteAdminRule } from '@/api/credit'

const list = ref([])
const loading = ref(false)
const RULE_CODE_LABEL = {
  HOMEWORK_ON_TIME: '按时完成作业', HOMEWORK_LATE: '迟交作业', TEST_HOMEWORK: '完成作业',
  EXAM_PERFECT: '考试满分', EXAM_EXCELLENT: '考试成绩优秀', EXAM_GOOD: '考试成绩良好', TEST_EXAM: '考试成绩优秀',
  SIGN_DAILY: '每日签到', SIGN_STREAK_3: '连续签到3天', SIGN_STREAK_7: '连续签到7天', SIGN_STREAK_30: '连续签到30天',
  ASSIST_HELP: '帮助同学解题', MORAL_BEHAVIOR: '德育行为表扬',
  DAILY_FIRST: '每日首位完成',
  SHOWCASE_CLASS: '优秀作品-班级展示', SHOWCASE_MULTI: '优秀作品-跨班展示', SHOWCASE_SCHOOL: '优秀作品-全校展示'
}

const filterKey = ref('')
const filterDesc = ref('')
const pageNum = ref(1)
const pageSize = 10

const { required: req } = useFormRules()
const formRules = { ruleCode: [req('规则键')], ruleName: [req('行为名称')] }
const formRef = ref(null)

// 表单
const formVisible = ref(false)
const formTitle = computed(() => editingId.value ? '编辑规则' : '新增规则')
const editingId = ref(null)
const saving = ref(false)
const form = ref(getEmptyForm())

function getEmptyForm() {
  return {
    ruleCode: '', ruleName: '', creditValue: 5, actionType: 'homework',
    maxDailyCount: 0, description: '', status: 1
  }
}

// 筛选 + 分页
const filteredList = computed(() => {
  let arr = list.value
  if (filterKey.value) arr = arr.filter(r => r.ruleCode && r.ruleCode.toLowerCase().includes(filterKey.value.toLowerCase()))
  if (filterDesc.value) arr = arr.filter(r => (r.ruleName || '').includes(filterDesc.value) || (r.description || '').includes(filterDesc.value))
  return arr
})
const pagedList = computed(() => {
  const start = (pageNum.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})

const onFilter = () => { pageNum.value = 1 }

// 加载
const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminRules()
    if (res.code === 200) list.value = res.data || []
  } finally { loading.value = false }
}

// 新增
const openCreate = () => {
  editingId.value = null
  form.value = getEmptyForm()
  formVisible.value = true
}

// 编辑
const openEdit = (row) => {
  editingId.value = row.id
  form.value = {
    ruleCode: row.ruleCode || '',
    ruleName: row.ruleName || '',
    creditValue: row.creditValue || 0,
    actionType: row.actionType || 'homework',
    maxDailyCount: row.maxDailyCount || 0,
    description: row.description || '',
    status: row.status ?? 1
  }
  formVisible.value = true
}

// 保存
const handleSave = async () => {
  if (saving.value) return
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    let res
    if (editingId.value) {
      res = await updateAdminRule(editingId.value, form.value)
    } else {
      res = await createAdminRule(form.value)
    }
    if (res.code === 200) {
      ElMessage.success(editingId.value ? '更新成功' : '创建成功')
      formVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally { saving.value = false }
}

// 开关状态
const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    const res = await updateAdminRule(row.id, { ...row, status: newStatus })
    if (res.code === 200) {
      row.status = newStatus
      ElMessage.success(newStatus ? '已启用' : '已禁用')
    }
  } catch { ElMessage.error('操作失败') }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除规则 "${row.ruleName}"？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    const res = await deleteAdminRule(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      loadData()
    }
  } catch { /* 取消 */ }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.credit-rules-page { max-width: 1100px; }

.page-header {
  margin-bottom: 16px;
  .page-title { font-size: var(--fs-xl, 20px); margin: 0 0 4px; }
  .page-subtitle { font-size: var(--fs-xs, 12px); color: var(--text-secondary); }
}

.toolbar { margin-bottom: 16px; }

.pagination-wrap {
  display: flex; justify-content: center; margin-top: 16px;
}

:deep(.el-table) { font-size: var(--fs-sm); }

.rule-form {
  :deep(.el-form-item) { margin-bottom: 12px; }
  :deep(.el-form-item__label) { padding-bottom: 2px; }
}

@media (max-width: 768px) {
  .credit-rules-page { max-width: 100%; }
  .toolbar .el-col { margin-bottom: 8px; }
}
</style>
