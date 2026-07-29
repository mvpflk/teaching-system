<template>
  <el-dialog
    v-model="dlgVisible"
    :title="isEdit ? '编辑班级' : '创建班级'"
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
          <el-form-item label="学段" prop="stageId">
            <el-select
              v-model="form.stageId"
              placeholder="选择学段"
              :disabled="isEdit && !isSuperAdmin"
              style="width:100%"
              @change="onStageChange"
            >
              <el-option
                v-for="s in stageOptions"
                :key="s.id"
                :value="s.id"
                :label="s.name"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="年级" prop="grade">
            <el-select
              v-model="form.grade"
              placeholder="选择年级"
              filterable
              clearable
              style="width:100%"
            >
              <el-option
                v-for="g in gradeOptions"
                :key="g.id"
                :value="g.gradeName"
                :label="g.gradeName"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="班级名称" prop="className"><el-input v-model="form.className" placeholder="如：计算机1班" maxlength="50" /></el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="班级编号" prop="classCode"><el-input v-model="form.classCode" placeholder="如：JSJ001" maxlength="30" /></el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="班级类型">
            <el-select
              v-model="form.classType"
              placeholder="选择类型"
              clearable
              style="width:100%"
              @change="onTypeChange"
            >
              <el-option value="general" label="普高班" />
              <el-option value="vocational" label="职高班" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col v-if="form.classType === 'vocational'" :xs="24" :sm="12">
          <el-form-item label="专业">
            <el-select
              v-model="form.major"
              placeholder="请选择专业"
              filterable
              clearable
              style="width:100%"
            >
              <el-option
                v-for="m in majorOptions"
                :key="m.id"
                :value="m.majorName"
                :label="m.majorName"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="关联学期">
            <el-select
              v-model="selectedTermId"
              placeholder="选择学期（自动填充学年+学期）"
              clearable
              filterable
              style="width:100%"
              @change="onTermChange"
            >
              <el-option
                v-for="t in termOptions"
                :key="t.id"
                :value="t.id"
                :label="t.name"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="学年"><el-input v-model="form.academicYear" placeholder="如：2025-2026" maxlength="20" /></el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="学期">
            <el-select v-model="form.semester" style="width:100%">
              <el-option label="第一学期" value="上" /><el-option label="第二学期" value="下" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width:100%">
          <el-option :value="1" label="在读" /><el-option :value="0" label="已毕业" />
        </el-select>
      </el-form-item>
      <el-form-item label="课题组别" prop="researchGroup">
        <el-select v-model="form.researchGroup" placeholder="课题研究用（可选）" clearable style="width:100%">
          <el-option value="EXPERIMENT" label="实验班" />
          <el-option value="CONTROL" label="对照班" />
        </el-select>
      </el-form-item>
      <el-form-item label="班主任">
        <el-select
          v-model="form.headTeacherId"
          placeholder="选择班主任"
          filterable
          clearable
          style="width:100%"
        >
          <el-option
            v-for="t in teachers"
            :key="t.id"
            :value="t.id"
            :label="t.name + ' (' + t.username + ')'"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dlgVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">{{ isEdit ? '保存' : '创建' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { onMounted } from 'vue'
import { useFormRules } from '@/composables/useFormRules'
import { createClass, updateClass } from '@/api/classes'
import { getMajors, getTermList } from '@/api/settings'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

const stageOptions = [
  { id: 1, name: '小学' }, { id: 2, name: '初中' },
  { id: 3, name: '普高' }, { id: 4, name: '职高' },
]

const majorOptions = ref([])
const termOptions = ref([])
const selectedTermId = ref(null)

const loadOptions = async () => {
  if (majorOptions.value.length === 0) {
    try { const r = await getMajors(); if (r.code === 200) majorOptions.value = r.data || [] } catch { /* */ }
  }
  if (termOptions.value.length === 0) {
    try { const r = await getTermList(userStore.schoolId || 1); if (r.code === 200) termOptions.value = r.data || [] } catch { /* */ }
  }
}

// 选择学期 → 自动解析学年+学期
const onTermChange = (id) => {
  const t = termOptions.value.find(t => t.id === id)
  if (!t) return
  // 解析名称: "2025-2026学年第二学期" → year: "2025-2026", sem: "下"
  const m = t.name.match(/^(\d{4}-\d{4})/)
  if (m) form.academicYear = m[1]
  if (t.name.includes('第一学期') || t.name.includes('上学期')) form.semester = '上'
  else if (t.name.includes('第二学期') || t.name.includes('下学期')) form.semester = '下'
}

onMounted(() => { loadOptions() })

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  isEdit: { type: Boolean, default: false },
  editData: { type: Object, default: null },
  teachers: { type: Array, default: () => [] },
  gradeOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const dlgVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const saving = ref(false)
const formRef = ref(null)
const { required: req } = useFormRules()

const createEmptyForm = () => ({
  stageId: null, className: '', classCode: '', grade: '', major: '',
  academicYear: '', semester: '上', status: 1, headTeacherId: null,
  classType: '', researchGroup: '',
})

const form = reactive(createEmptyForm())

const rules = {
  stageId: [req('学段')],
  className: [req('班级名称')],
  classCode: [req('班级编号')],
  grade: [req('年级')],
}

const initializeForm = () => {
  if (props.isEdit && props.editData) {
    Object.assign(form, {
      stageId: props.editData.stageId,
      className: props.editData.className,
      classCode: props.editData.classCode,
      grade: props.editData.grade,
      major: props.editData.major,
      academicYear: props.editData.academicYear,
      semester: props.editData.semester || '上',
      status: props.editData.status,
      headTeacherId: props.editData.headTeacherId,
      classType: props.editData.classType || '',
      researchGroup: props.editData.researchGroup || '',
    })
  } else {
    Object.assign(form, createEmptyForm())
  }
}

const onStageChange = () => { form.major = '' }
const onTypeChange = () => { if (form.classType !== 'vocational') form.major = '' }

watch(() => props.modelValue, (v) => { if (v) { initializeForm(); loadOptions() } })

const handleSave = async () => {
  if (saving.value) return
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    const payload = { ...form }
    const res = props.isEdit
      ? await updateClass(props.editData?.id, payload)
      : await createClass(payload)
    if (res.code === 200) {
      ElMessage.success(props.isEdit ? '已保存' : '创建成功')
      dlgVisible.value = false
      emit('saved')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch { ElMessage.error('操作失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-row) { margin-left: 0 !important; margin-right: 0 !important; }
  :deep(.el-col) { padding-left: 0 !important; padding-right: 0 !important; }
}
</style>
