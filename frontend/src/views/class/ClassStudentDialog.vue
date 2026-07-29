<template>
  <el-dialog
    v-model="localVisible"
    :title="'学生管理 - ' + (className || '')"
    width="650px"
    append-to-body
  >
    <div class="flex-between mb-16">
      <span style="font-size:var(--fs-sm);color:var(--text-secondary)">共 {{ studentList.length }} 名学生</span>
      <el-button size="small" type="primary" @click="showAddStudent"><el-icon><Plus /></el-icon>添加学生</el-button>
    </div>
    <el-table
      v-loading="studentLoading"
      :data="studentList"
      stripe
      size="small"
    >
      <el-table-column prop="studentNumber" label="学号" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="username" label="用户名" width="100" />
      <el-table-column prop="grade" label="年级" width="70" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button
            text
            type="danger"
            size="small"
            @click="handleRemoveStudent(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog
      v-model="addStudentVisible"
      title="添加学生到班级"
      width="450px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="studentFormRef"
        :model="studentForm"
        :rules="studentRules"
        @submit.prevent="handleAddStudent"
      >
        <el-form-item prop="studentId">
          <el-select
            v-model="studentForm.studentId"
            placeholder="选择学生"
            filterable
            style="width:100%"
          >
            <el-option
              v-for="s in availableStudents"
              :key="s.id"
              :value="s.id"
              :label="s.studentNumber + ' - ' + s.realName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addStudentVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddStudent">确认添加</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFormRules } from '@/composables/useFormRules'
import { getStudents, addStudent, removeStudent, getAvailableStudents } from '@/api/classes'

const props = defineProps({
  visible: { type: Boolean, default: false },
  classId: { type: Number, default: null },
  className: { type: String, default: '' }
})

const emit = defineEmits(['update:visible', 'student-changed'])

const localVisible = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v)
})

const { required: req, selectRequired: selReq } = useFormRules()
const studentFormRef = ref(null)
const studentForm = reactive({ studentId: null })
const studentRules = { studentId: [selReq('学生')] }

const studentList = ref([])
const studentLoading = ref(false)
const addStudentVisible = ref(false)
const availableStudents = ref([])

const loadStudents = async () => {
  if (!props.classId) return
  studentLoading.value = true
  try {
    const res = await getStudents(props.classId)
    if (res.code === 200) studentList.value = res.data || []
  } finally { studentLoading.value = false }
}

const showAddStudent = async () => {
  try {
    const res = await getAvailableStudents()
    if (res.code === 200) availableStudents.value = res.data || []
  } catch { ElMessage.error('获取学生列表失败'); return }
  addStudentVisible.value = true
  studentForm.studentId = null
}

const handleAddStudent = async () => {
  if (!studentFormRef.value) return
  try { await studentFormRef.value.validate() } catch { return }
  try {
    const res = await addStudent(props.classId, studentForm.studentId)
    if (res.code === 200) {
      ElMessage.success('已添加')
      addStudentVisible.value = false
      await loadStudents()
      emit('student-changed')
    }
  } catch { ElMessage.error('添加学生失败') }
}

const handleRemoveStudent = async (stu) => {
  try {
    await ElMessageBox.confirm(`确定将学生「${stu.realName}」移出班级吗？`, '提示', { type: 'warning' })
  } catch { return }
  try {
    const res = await removeStudent(props.classId, stu.id)
    if (res.code === 200) {
      ElMessage.success('已移出班级')
      await loadStudents()
      emit('student-changed')
    }
  } catch { ElMessage.error('移出失败') }
}

watch(() => props.visible, (v) => {
  if (v && props.classId) loadStudents()
})
</script>

<style scoped lang="scss">
.mb-16 { margin-bottom: 16px; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }

@media (max-width: 768px) {
  :deep(.el-table .el-table__body-wrapper) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
