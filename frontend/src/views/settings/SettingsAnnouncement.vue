<template>
  <el-row :gutter="24">
    <el-col :span="12">
      <div class="dict-section">
        <div class="dict-header">
          <h4>年级字典</h4>
          <el-button
            v-if="isSuperAdmin"
            size="small"
            type="primary"
            @click="showGradeDialog()"
          >
            添加年级
          </el-button>
        </div>
        <el-table :data="gradeList" size="small" stripe>
          <el-table-column prop="gradeName" label="年级名称" />
          <el-table-column prop="sortOrder" label="排序" width="70" />
          <el-table-column v-if="isSuperAdmin" label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" text @click="showGradeDialog(row)">编辑</el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="delGrade(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-col>
    <el-col :span="12">
      <div class="dict-section">
        <div class="dict-header">
          <h4>学科字典</h4>
          <el-button
            v-if="isSuperAdmin"
            size="small"
            type="primary"
            @click="showSubjectDialog()"
          >
            添加学科
          </el-button>
        </div>
        <el-table :data="subjectList" size="small" stripe>
          <el-table-column prop="subjectName" label="学科名称" />
          <el-table-column prop="sortOrder" label="排序" width="70" />
          <el-table-column v-if="isSuperAdmin" label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" text @click="showSubjectDialog(row)">编辑</el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="delSubject(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-col>
  </el-row>

  <!-- 年级弹窗 -->
  <el-dialog
    v-model="gradeVisible"
    :title="gradeForm.id ? '编辑年级' : '添加年级'"
    width="400px"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
  >
    <el-form
      ref="gradeFormRef"
      :model="gradeForm"
      :rules="dictRules"
      label-position="top"
    >
      <el-form-item label="名称" prop="gradeName"><el-input v-model="gradeForm.gradeName" placeholder="如：2026级" /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="gradeForm.sortOrder" :min="0" :max="99" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="gradeVisible = false">取消</el-button>
      <el-button type="primary" :loading="gradeSaving" @click="saveGrade">保存</el-button>
    </template>
  </el-dialog>

  <!-- 学科弹窗 -->
  <el-dialog
    v-model="subjectVisible"
    :title="subjectForm.id ? '编辑学科' : '添加学科'"
    width="400px"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
  >
    <el-form
      ref="subjectFormRef"
      :model="subjectForm"
      :rules="dictRules"
      label-position="top"
    >
      <el-form-item label="名称" prop="subjectName"><el-input v-model="subjectForm.subjectName" placeholder="如：Java程序设计" /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="subjectForm.sortOrder" :min="0" :max="99" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="subjectVisible = false">取消</el-button>
      <el-button type="primary" :loading="subjectSaving" @click="saveSubject">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFormRules } from '@/composables/useFormRules'
import { getGrades, createGrade, updateGrade, deleteGrade, getSubjects, createSubject, updateSubject, deleteSubject } from '@/api/settings'

defineProps({
  isSuperAdmin: { type: Boolean, default: false }
})

const { required: req } = useFormRules()
const dictRules = { gradeName: [req('名称')], subjectName: [req('名称')] }

// ===== 年级管理 =====
const gradeList = ref([])
const gradeVisible = ref(false)
const gradeSaving = ref(false)
const gradeFormRef = ref(null)
const gradeForm = reactive({ id: null, gradeName: '', sortOrder: 0 })

const loadGrades = async () => {
  try {
    const res = await getGrades()
    if (res.code === 200) gradeList.value = res.data
  } catch { ElMessage.error('加载年级列表失败') }
}

const showGradeDialog = (row) => {
  if (row) { gradeForm.id = row.id; gradeForm.gradeName = row.gradeName; gradeForm.sortOrder = row.sortOrder || 0 }
  else { gradeForm.id = null; gradeForm.gradeName = ''; gradeForm.sortOrder = 0 }
  gradeVisible.value = true
}

const saveGrade = async () => {
  if (gradeSaving.value) return
  if (!gradeFormRef.value) return
  try { await gradeFormRef.value.validate() } catch { return }
  gradeSaving.value = true
  try {
    const data = { gradeName: gradeForm.gradeName, sortOrder: gradeForm.sortOrder, status: 1 }
    const res = gradeForm.id
      ? await updateGrade(gradeForm.id, data)
      : await createGrade(data)
    if (res.code === 200) { ElMessage.success(res.message || '已保存'); gradeVisible.value = false; await loadGrades() }
    else { ElMessage.error(res.message || '保存失败') }
  } catch { ElMessage.error('保存年级失败') }
  finally { gradeSaving.value = false }
}

const delGrade = async (row) => {
  try {
    await ElMessageBox.confirm(`删除年级「${row.gradeName}」？`, '确认', { type: 'warning' })
    const res = await deleteGrade(row.id)
    if (res.code === 200) { ElMessage.success('已删除'); await loadGrades() }
  } catch { /* cancelled */ }
}

// ===== 学科管理 =====
const subjectList = ref([])
const subjectVisible = ref(false)
const subjectSaving = ref(false)
const subjectFormRef = ref(null)
const subjectForm = reactive({ id: null, subjectName: '', sortOrder: 0 })

const loadSubjects = async () => {
  try {
    const res = await getSubjects()
    if (res.code === 200) subjectList.value = res.data
  } catch { ElMessage.error('加载学科列表失败') }
}

const showSubjectDialog = (row) => {
  if (row) { subjectForm.id = row.id; subjectForm.subjectName = row.subjectName; subjectForm.sortOrder = row.sortOrder || 0 }
  else { subjectForm.id = null; subjectForm.subjectName = ''; subjectForm.sortOrder = 0 }
  subjectVisible.value = true
}

const saveSubject = async () => {
  if (subjectSaving.value) return
  if (!subjectFormRef.value) return
  try { await subjectFormRef.value.validate() } catch { return }
  subjectSaving.value = true
  try {
    const data = { subjectName: subjectForm.subjectName, sortOrder: subjectForm.sortOrder, status: 1 }
    const res = subjectForm.id
      ? await updateSubject(subjectForm.id, data)
      : await createSubject(data)
    if (res.code === 200) { ElMessage.success(res.message || '已保存'); subjectVisible.value = false; await loadSubjects() }
    else { ElMessage.error(res.message || '保存失败') }
  } catch { ElMessage.error('保存学科失败') }
  finally { subjectSaving.value = false }
}

onMounted(() => { loadGrades(); loadSubjects() })

const delSubject = async (row) => {
  try {
    await ElMessageBox.confirm(`删除学科「${row.subjectName}」？`, '确认', { type: 'warning' })
    const res = await deleteSubject(row.id)
    if (res.code === 200) { ElMessage.success('已删除'); await loadSubjects() }
  } catch { /* cancelled */ }
}

</script>

<style scoped lang="scss">
.dict-section { margin-bottom: 16px; }
.dict-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.dict-header h4 { margin: 0; font-size: var(--fs-md); }

@media (max-width: 768px) {
  :deep(.el-row) { display: flex; flex-direction: column; }
  :deep(.el-col) { max-width: 100%; flex: 0 0 100%; }
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>
