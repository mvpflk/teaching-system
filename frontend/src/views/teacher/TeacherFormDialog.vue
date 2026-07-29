<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑教师' : '添加教师'"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
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
          <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="isEdit" maxlength="50" /></el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" maxlength="50" /></el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="工号" prop="teacherNumber"><el-input v-model="form.teacherNumber" maxlength="30" /></el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              :placeholder="isEdit ? '留空不修改' : '默认123456'"
              type="password"
              show-password
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" placeholder="选择性别" style="width:100%">
              <el-option :value="1" label="男" /><el-option :value="2" label="女" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item v-if="isEdit" label="状态">
            <el-select v-model="form.status" style="width:100%">
              <el-option :value="1" label="正常" /><el-option :value="0" label="禁用" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item v-if="!isEdit" label="身份">
        <el-checkbox v-model="form.admin" :true-value="1" :false-value="0">管理员</el-checkbox>
        <el-checkbox
          v-model="form.inspector"
          :true-value="16"
          :false-value="0"
          style="margin-left:12px"
        >
          巡视员
        </el-checkbox>
        <el-checkbox
          v-model="form.teacher"
          :true-value="2"
          :false-value="0"
          style="margin-left:12px"
        >
          教师
        </el-checkbox>
        <span class="text-muted" style="margin-left:8px;font-size:var(--fs-xs)">可多选</span>
      </el-form-item>
      <el-form-item v-else label="身份">
        <el-tag type="success">教师</el-tag>
        <span class="text-muted" style="margin-left:8px;font-size:var(--fs-xs)">编辑模式下身份不可修改</span>
      </el-form-item>
      <el-form-item v-if="form.teacher" label="班主任">
        <el-checkbox v-model="form.isHeadTeacher">设为班主任（指定负责班级）</el-checkbox>
      </el-form-item>
      <el-form-item v-if="form.teacher && form.isHeadTeacher" label="负责班级">
        <el-select
          v-model="form.headClassId"
          placeholder="选择担任班主任的班级"
          filterable
          clearable
          style="width:100%"
        >
          <el-option
            v-for="c in classList"
            :key="c.id"
            :value="c.id"
            :label="(c.grade || '') + c.className"
          />
        </el-select>
        <span class="text-muted" style="font-size:var(--fs-xs)">提示：需先在班级管理中创建班级，并将该教师设为班主任</span>
      </el-form-item>
      <el-form-item label="任教学科" prop="subjects">
        <el-select
          v-model="form.subjects"
          multiple
          placeholder="选择学科（可多选）"
          filterable
          clearable
          style="width:100%"
        >
          <el-option
            v-for="s in subjectOptions"
            :key="s.id"
            :value="s.subjectName"
            :label="s.subjectName"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.teacher" label="任教专业">
        <el-select
          v-model="form.majors"
          multiple
          placeholder="选择专业（可多选）"
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
      <el-form-item v-if="form.teacher" label="任教年级">
        <el-select
          v-model="form.teachingGrades"
          multiple
          placeholder="选择年级（可多选）"
          filterable
          clearable
          style="width:100%"
          @change="onGradeChange"
        >
          <el-option
            v-for="g in gradeOptions"
            :key="g.id"
            :value="g.gradeName"
            :label="g.gradeName"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.teacher" label="任教班级">
        <el-select
          v-model="form.teachingClassIds"
          multiple
          placeholder="选择任教班级（可多选）"
          style="width:100%"
          :disabled="!filteredClassList.length"
        >
          <el-option
            v-for="c in filteredClassList"
            :key="c.id"
            :value="c.id"
            :label="c.className"
          />
        </el-select>
        <span class="text-muted" style="font-size:var(--fs-xs)">提示：先选年级再选班级</span>
      </el-form-item>
    </el-form>
    <TeacherGroupPanel v-if="isEdit" ref="groupPanelRef" :teacher-id="teacherIdForGroup()" />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">{{ isEdit ? '保存' : '添加' }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useFormRules } from '@/composables/useFormRules'
import { createTeacher, updateTeacher, getTeacher, getTeacherAssignments, setTeacherAssignments, setHeadClass } from '@/api/teacher'
import { getClassList } from '@/api/classes'
import { getSubjects, getGrades, getMajors } from '@/api/settings'
import TeacherGroupPanel from '@/components/common/TeacherGroupPanel.vue'
import { useKeyboardFix } from '@/composables/useKeyboardFix'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  isEdit: { type: Boolean, default: false },
  editData: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const saving = ref(false)
const formRef = ref(null)
const classList = ref([])
const subjectOptions = ref([])
const gradeOptions = ref([])
const majorOptions = ref([])
const filteredClassList = ref([])
const groupPanelRef = ref(null)
const { required: req, selectRequired } = useFormRules()

const rules = {
  username: [req('用户名')],
  realName: [req('姓名')],
  teacherNumber: [req('工号')],
  gender: [selectRequired('性别')],
  subjects: [{ type: 'array', required: true, message: '请选择任教科目', trigger: 'change' }],
}

const form = reactive({
  username: '', realName: '', password: '', teacherNumber: '',
  gender: null, subjects: [],
  admin: 0, inspector: 0, teacher: 2,
  isHeadTeacher: false, headClassId: null,
  teachingGrades: [], teachingClassIds: [], majors: [], status: 1,
})

const loadGradeOptions = async () => {
  try { const r = await getGrades(); if (r.code === 200) gradeOptions.value = r.data } catch { /* */ }
}

const onGradeChange = (grades) => {
  if (!grades || !grades.length) { filteredClassList.value = classList.value; return }
  filteredClassList.value = classList.value.filter(c => grades.includes(c.grade))
  form.teachingClassIds = form.teachingClassIds.filter(cid => filteredClassList.value.some(c => c.id === cid))
}

const computeRoleId = () => (form.admin || 0) | (form.inspector || 0) | (form.teacher || 0)
const roleIdToForm = (roleId) => { form.admin = (roleId & 1) ? 1 : 0; form.inspector = (roleId & 16) ? 16 : 0; form.teacher = (roleId & 2) ? 2 : 0 }

const syncHeadTeacher = async (userId, headClassId) => {
  try { await setHeadClass(userId, headClassId) } catch { ElMessage.error('设置班主任失败') }
}

watch(() => props.modelValue, async (val) => {
  if (!val) return
  if (props.isEdit && props.editData?.userId) {
    try {
      const userId = props.editData.userId
      const [infoRes, assignRes] = await Promise.all([getTeacher(userId), getTeacherAssignments(userId)])
      if (infoRes.code === 200 && infoRes.data) {
        const d = infoRes.data
        form.username = d.username || ''; form.realName = d.realName || ''; form.password = ''
        form.teacherNumber = d.teacherNumber || ''; form.gender = d.gender
        roleIdToForm(d.roleId || 2)
        form.status = d.status ?? 1
        form.subjects = d.subject ? [d.subject] : []
        form.isHeadTeacher = props.editData.isHeadTeacher || false
        form.headClassId = props.editData.headClassId || null
      }
      if (assignRes.code === 200 && Array.isArray(assignRes.data)) {
        const assigns = assignRes.data
        form.teachingClassIds = assigns.map(a => a.classId)
        // 根据任教班级反推年级和科目
        const grades = [...new Set(assigns.map(a => a.grade).filter(Boolean))]
        form.teachingGrades = grades
        const subjects = [...new Set(assigns.map(a => a.subject).filter(Boolean))]
        if (subjects.length) form.subjects = subjects
        // 触发年级筛选，使 filteredClassList 正确
        onGradeChange(form.teachingGrades)
      }
    } catch { /* */ }
    setTimeout(() => groupPanelRef.value?.loadTeacherGroups(), 0)
  } else {
    form.username = ''; form.realName = ''; form.password = ''
    form.teacherNumber = ''; form.gender = null; form.status = 1
    form.admin = 0; form.inspector = 0; form.teacher = 2
    form.isHeadTeacher = false; form.headClassId = null
    form.subjects = []; form.teachingClassIds = []; form.teachingGrades = []; form.majors = []
  }
})

const handleSave = async () => {
  if (saving.value) return
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  const roleId = computeRoleId()
  if (!roleId) { ElMessage.warning('请至少选择一个身份'); return }
  const defaultSubject = form.subjects.length > 0 ? form.subjects.join(',') : ''
  saving.value = true
  try {
    if (props.isEdit) {
      const data = { realName: form.realName, teacherNumber: form.teacherNumber, gender: form.gender, subject: defaultSubject, status: form.status }
      if (form.password) data.password = form.password
      const res = await updateTeacher(props.editData.userId, data)
      if (res.code !== 200) { ElMessage.error(res.message || '更新失败'); return }
      if (form.teachingClassIds.length) {
        await setTeacherAssignments(props.editData.userId, form.teachingClassIds.map(cid => ({ classId: cid, subject: defaultSubject })))
      }
      await syncHeadTeacher(props.editData.userId, form.isHeadTeacher ? form.headClassId : null)
      ElMessage.success('已更新')
    } else {
      if (!form.password) { form.password = Math.random().toString(36).slice(-6); form._showPwd = true }
      const data = { username: form.username, realName: form.realName, password: form.password, teacherNumber: form.teacherNumber, gender: form.gender, roleId, subject: defaultSubject }
      const res = await createTeacher(data)
      if (res.code !== 200) { ElMessage.error(res.message || '添加失败'); return }
      if (form.teachingClassIds.length) {
        await setTeacherAssignments(res.data.userId, form.teachingClassIds.map(cid => ({ classId: cid, subject: defaultSubject })))
      }
      await syncHeadTeacher(res.data.userId, form.isHeadTeacher ? form.headClassId : null)
      if (form._showPwd) {
        ElMessage.success(`添加成功！初始密码: ${form.password}`)
      } else {
        ElMessage.success('添加成功')
      }
    }
    visible.value = false
    emit('saved')
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

const teacherIdForGroup = () => props.editData?.id || props.editData?.userId

onMounted(async () => {
  const [clsRes, subRes, mjRes] = await Promise.all([getClassList(), getSubjects(), getMajors()])
  if (clsRes.code === 200) classList.value = clsRes.data.records
  if (subRes.code === 200) subjectOptions.value = subRes.data
  if (mjRes.code === 200) majorOptions.value = mjRes.data || []
  await loadGradeOptions()
})
useKeyboardFix()
</script>

<style scoped>
.text-muted { color: var(--text-secondary); font-size: var(--fs-xs); }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
