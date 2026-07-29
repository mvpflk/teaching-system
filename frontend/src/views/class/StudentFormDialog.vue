<template>
  <el-dialog
    v-model="dlgVisible"
    :title="isEdit ? '编辑学生' : '添加学生'"
    width="600px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :disabled="isEdit" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="学号" prop="studentNumber">
            <el-input v-model="form.studentNumber" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              :placeholder="isEdit ? '留空不修改' : '留空则自动生成随机密码'"
              type="password"
              show-password
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" placeholder="选择" style="width:100%">
              <el-option :value="1" label="男" />
              <el-option :value="2" label="女" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="出生日期">
            <el-date-picker
              v-model="form.birthday"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width:100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="选填" maxlength="20" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="选填" maxlength="100" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="年级">
            <el-select
              v-model="formGrade"
              placeholder="选择年级"
              clearable
              style="width:100%"
              @change="onFormGradeChange"
            >
              <el-option
                v-for="g in gradeList"
                :key="g"
                :value="g"
                :label="g"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="班级" prop="classId">
            <el-select
              v-model="form.classId"
              placeholder="选择年级后筛选班级"
              clearable
              style="width:100%"
              :disabled="!formGrade"
            >
              <el-option
                v-for="c in filteredClassList"
                :key="c.id"
                :value="c.id"
                :label="c.className"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="isEdit" :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="当前类型">
            <el-select
              v-model="form.currentType"
              placeholder="自动同步班级"
              clearable
              style="width:100%"
            >
              <el-option
                v-for="t in typeOptions"
                :key="t.typeCode"
                :value="t.typeCode"
                :label="t.typeName"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="入学类型">
            <el-select
              v-model="form.enrollmentType"
              placeholder="自动同步班级"
              clearable
              style="width:100%"
            >
              <el-option
                v-for="t in typeOptions"
                :key="t.typeCode"
                :value="t.typeCode"
                :label="t.typeName"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <div v-if="selectedClass" style="margin-bottom:16px;display:flex;gap:8px;align-items:center">
        <span style="font-size:var(--fs-sm);color:var(--text-secondary)">班级信息：</span>
        <el-tag size="small">{{ selectedClass.className }}</el-tag>
        <el-tag v-if="selectedClass.classType" size="small" :type="classTypeTag(selectedClass.classType)">{{ classTypeName(selectedClass.classType) }}</el-tag>
        <span v-if="selectedClass.major" style="font-size:var(--fs-xs);color:var(--text-secondary)">专业：{{ selectedClass.major }}</span>
      </div>
      <el-form-item v-if="isEdit" label="学生状态" prop="studentStatus">
        <el-select v-model="form.studentStatus" style="width:100%">
          <el-option
            v-for="s in STATUS_OPTIONS"
            :key="s.value"
            :value="s.value"
            :label="s.label"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dlgVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">{{ isEdit ? '保存' : '添加' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useFormRules } from '@/composables/useFormRules'
import { createStudent, updateStudent } from '@/api/student'
import { getTypeConfigList } from '@/api/classTypeConfig'
import { STATUS_OPTIONS } from '@/utils/student'

const props = defineProps({
  modelValue: Boolean,
  editData: { type: Object, default: null },
  classList: { type: Array, default: () => [] },
  gradeList: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const dlgVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const formGrade = ref('')
const typeOptions = ref([])
const { required: req, selectRequired } = useFormRules()

const selectedClass = computed(() => {
  if (!form.value.classId) return null
  return props.classList.find(c => c.id === form.value.classId) || null
})

const classTypeTag = (code) => {
  const t = typeOptions.value.find(o => o.typeCode === code)
  return t ? ({ GENERAL: 'info', PUGAO: '', VOCATIONAL: 'warning' }[t.category] || 'info') : 'info'
}
const classTypeName = (code) => {
  const t = typeOptions.value.find(o => o.typeCode === code)
  return t ? t.typeName : code
}

const defaultForm = {
  username: '', realName: '', password: '',
  studentNumber: '', gender: null, birthday: '', phone: '', email: '',
  classId: null, studentStatus: 'active',
  currentType: '', enrollmentType: '',
}

const form = ref({ ...defaultForm })

const rules = {
  username: [req('用户名')],
  realName: [req('姓名')],
  studentNumber: [req('学号')],
  gender: [selectRequired('性别')],
  classId: [selectRequired('班级')],
}

const filteredClassList = computed(() => {
  if (!formGrade.value) return []
  return props.classList.filter(c => c.grade === formGrade.value)
})

const onFormGradeChange = () => { form.value.classId = null }

const initForm = () => {
  const row = props.editData
  if (row && row.id) {
    isEdit.value = true
    form.value = {
      username: row.username || '',
      realName: row.realName || '',
      password: '',
      studentNumber: row.studentNumber || '',
      gender: row.gender ?? null,
      birthday: row.birthday || '',
      phone: row.phone || '',
      email: row.email || '',
      classId: row.classId ?? null,
      studentStatus: row.status || 'active',
      currentType: row.currentType || '',
      enrollmentType: row.enrollmentType || '',
    }
    formGrade.value = row.grade || ''
  } else {
    isEdit.value = false
    form.value = { ...defaultForm }
    formGrade.value = ''
  }
}

onMounted(async () => {
  try { const r = await getTypeConfigList(); if (r.code === 200) typeOptions.value = r.data || [] } catch { /* */ }
})

watch(() => props.modelValue, (val) => { if (val) initForm() })

const handleSave = async () => {
  if (saving.value) return
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    if (isEdit.value) {
      const data = {
        realName: form.value.realName,
        studentNumber: form.value.studentNumber,
        gender: form.value.gender,
        birthday: form.value.birthday || '',
        phone: form.value.phone,
        email: form.value.email,
        classId: form.value.classId,
        studentStatus: form.value.studentStatus,
        currentType: form.value.currentType,
        enrollmentType: form.value.enrollmentType,
      }
      if (form.value.password) data.password = form.value.password
      const res = await updateStudent(props.editData.id, data)
      if (res.code === 200) ElMessage.success('已更新')
      else { ElMessage.error(res.message || '更新失败'); return }
    } else {
      const data = { ...form.value }
      const res = await createStudent(data)
      if (res.code === 200) {
        const pwd = res.data?.defaultPassword
        if (pwd && !form.value.password) {
          ElMessage.success(`添加成功！初始密码: ${pwd}`)
        } else {
          ElMessage.success('添加成功')
        }
      }
      else { ElMessage.error(res.message || '添加失败'); return }
    }
    dlgVisible.value = false
    emit('saved')
  } catch { ElMessage.error('操作失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
