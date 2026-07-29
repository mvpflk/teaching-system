<template>
  <div class="page-container">
    <div class="page-header" style="padding:0 0 20px;margin-bottom:20px;border-bottom:1px solid var(--border-light)">
      <h3 class="page-title">班级管理</h3>
      <el-button v-if="isAdmin" type="primary" @click="showCreate">
        <el-icon><Plus /></el-icon>创建班级
      </el-button>
    </div>

    <ClassStatsCards :list="list" />

    <!-- 批量操作栏（仅超级管理员） -->
    <div v-if="isSuperAdmin && selectedIds.length > 0" class="batch-bar">
      <span>已选 <strong>{{ selectedIds.length }}</strong> 个班级</span>
      <el-select
        v-model="batchType"
        placeholder="选择目标类型"
        size="default"
        class="desktop-width"
        style="width:180px"
      >
        <el-option
          v-for="t in typeOptions"
          :key="t.typeCode"
          :value="t.typeCode"
          :label="t.typeName"
        />
      </el-select>
      <el-button
        type="primary"
        size="default"
        :disabled="!batchType"
        @click="handleBatchUpdateType"
      >
        批量标记类型
      </el-button>
      <el-button size="default" @click="selectedIds = []">取消</el-button>
    </div>

    <div class="filter-bar mb-20">
      <el-select
        v-model="filterGrade"
        placeholder="全部年级"
        clearable
        size="default"
      >
        <el-option
          v-for="g in gradeOptions"
          :key="g.id"
          :value="g.gradeName"
          :label="g.gradeName"
        />
      </el-select>
      <el-select
        v-model="filterType"
        placeholder="全部类型"
        clearable
        size="default"
        style="margin-left:8px"
      >
        <el-option
          v-for="t in typeOptions"
          :key="t.typeCode"
          :value="t.typeCode"
          :label="t.typeName"
        />
      </el-select>
      <el-select
        v-model="filterMajor"
        placeholder="全部专业"
        clearable
        size="default"
        style="margin-left:8px"
      >
        <el-option
          v-for="m in majorOptions"
          :key="m.id"
          :value="m.majorName"
          :label="m.majorName"
        />
      </el-select>
    </div>

    <div v-loading="loading" class="class-grid">
      <div v-if="list.length === 0" class="empty-state-box">
        <div class="empty-icon" style="opacity:0.3;font-size:40px">🏫</div>
        <div class="empty-text">暂无班级</div>
      </div>

      <div v-for="cls in displayList" :key="cls.id" class="class-card">
        <div class="class-card-header">
          <el-checkbox
            v-if="isSuperAdmin"
            :model-value="selectedIds.includes(cls.id)"
            style="margin-right:4px"
            @change="(v) => toggleSelect(cls.id, v)"
          />
          <div class="class-icon">{{ cls.className?.charAt(0) || '?' }}</div>
          <div class="class-info">
            <h4 class="class-name" style="cursor:pointer;color:var(--primary-color)" @click="$router.push(`/class/${cls.id}/home`)">{{ cls.className }}</h4>
            <span class="class-code">{{ cls.classCode }}</span>
          </div>
          <el-tag
            v-if="cls.classType"
            size="small"
            :type="typeTagColor(cls.classType)"
            effect="light"
            style="margin-right:4px"
          >
            {{ typeName(cls.classType) }}
          </el-tag>
          <el-tag :type="cls.status === 1 ? 'success' : 'info'" size="small" effect="light">
            {{ cls.status === 1 ? '在读' : '已毕业' }}
          </el-tag>
        </div>

        <div class="class-body">
          <div class="class-meta">
            <div class="meta-item">
              <span class="meta-label">年级</span>
              <span class="meta-value">{{ cls.grade || '-' }}</span>
            </div>
            <div v-if="typeCategory(cls.classType) === 'VOCATIONAL'" class="meta-item">
              <span class="meta-label">专业</span>
              <span class="meta-value">{{ cls.major || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">班主任</span>
              <span class="meta-value">{{ cls.headTeacherName || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">学年</span>
              <span class="meta-value">{{ cls.academicYear || '-' }}</span>
            </div>
          </div>

          <div class="class-students">
            <div class="students-count">
              <el-icon size="16"><User /></el-icon>
              <span><strong>{{ cls.studentCount || 0 }}</strong> 名学生</span>
            </div>
            <div class="progress-bar" style="margin-top:6px">
              <div class="progress-fill primary" :style="{ width: Math.min(100, (cls.studentCount || 0) / 60 * 100) + '%' }" />
            </div>
          </div>
        </div>

        <div class="class-actions">
          <el-button size="small" @click="manageStudents(cls)">学生管理</el-button>
          <el-button v-if="isAdmin" size="small" @click="showEdit(cls)">编辑</el-button>
          <el-button
            v-if="isAdmin"
            size="small"
            type="danger"
            @click="handleDelete(cls)"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>

    <ClassFormDialog
      v-model="formVisible"
      :is-edit="isEdit"
      :edit-data="editData"
      :teachers="teachers"
      :grade-options="gradeOptions"
      @saved="onFormSaved"
    />

    <ClassStudentDialog
      v-model:visible="studentVisible"
      :class-id="studentClassId"
      :class-name="studentClassName"
      @student-changed="loadList"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getClassList, deleteClass, getTeachers, batchUpdateClassType } from '@/api/classes'
import { getGrades, getMajors } from '@/api/settings'
import { getTypeConfigList } from '@/api/classTypeConfig'
import { useUserStore } from '@/stores/user'
import ClassStatsCards from './ClassStatsCards.vue'
import ClassFormDialog from './ClassFormDialog.vue'
import ClassStudentDialog from './ClassStudentDialog.vue'

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const list = ref([])
const filterGrade = ref('')
const filterType = ref('')
const filterMajor = ref('')
const gradeOptions = ref([])
const teachers = ref([])
const typeOptions = ref([])
const majorOptions = ref([])
const selectedIds = ref([])
const batchType = ref('')
const formVisible = ref(false)
const isEdit = ref(false)
const editData = ref(null)
const studentVisible = ref(false)
const studentClassId = ref(null)
const studentClassName = ref('')

const typeTagColor = (code) => {
  const t = typeOptions.value.find(o => o.typeCode === code)
  return t ? ({ GENERAL: 'info', PUGAO: '', VOCATIONAL: 'warning' }[t.category] || 'info') : 'info'
}
const typeName = (code) => {
  const t = typeOptions.value.find(o => o.typeCode === code)
  return t ? t.typeName : code
}
const typeCategory = (code) => {
  const t = typeOptions.value.find(o => o.typeCode === code)
  return t ? t.category : ''
}

const toggleSelect = (id, checked) => {
  if (checked) { if (!selectedIds.value.includes(id)) selectedIds.value.push(id) }
  else { selectedIds.value = selectedIds.value.filter(i => i !== id) }
}

const handleBatchUpdateType = async () => {
  if (!batchType.value || selectedIds.value.length === 0) return
  try {
    const res = await batchUpdateClassType({ classIds: selectedIds.value, classType: batchType.value })
    if (res.code === 200) {
      ElMessage.success(`已更新 ${res.data?.updated || selectedIds.value.length} 个班级`)
      selectedIds.value = []; batchType.value = ''
      loadList()
    }
  } catch { ElMessage.error('批量更新失败') }
}

const displayList = computed(() => {
  let arr = list.value
  if (filterGrade.value) arr = arr.filter(c => c.grade === filterGrade.value)
  if (filterType.value) arr = arr.filter(c => c.classType === filterType.value)
  if (filterMajor.value) arr = arr.filter(c => c.major === filterMajor.value)
  return arr
})

const loadGradeOptions = async () => {
  try {
    const r = await getGrades()
    if (r.code === 200) gradeOptions.value = r.data || []
  } catch { /* */ }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getClassList()
    if (res.code === 200) list.value = res.data?.records || []
  } finally { loading.value = false }
}

const loadTeachers = async () => {
  try {
    const res = await getTeachers()
    if (res.code === 200) teachers.value = res.data || []
  } catch { /* */ }
}

const showCreate = () => { isEdit.value = false; editData.value = null; formVisible.value = true }
const showEdit = (cls) => { isEdit.value = true; editData.value = cls; formVisible.value = true }
const onFormSaved = () => { formVisible.value = false; loadList() }

const handleDelete = async (cls) => {
  try { await ElMessageBox.confirm(`确定删除班级「${cls.className}」吗？`, '提示', { type: 'warning' }) } catch { return }
  try {
    const res = await deleteClass(cls.id)
    if (res.code === 200) { ElMessage.success('已删除'); loadList() }
  } catch { ElMessage.error('删除失败') }
}

const manageStudents = (cls) => {
  studentClassId.value = cls.id
  studentClassName.value = cls.className
  studentVisible.value = true
}

const loadTypeOptions = async () => {
  try { const r = await getTypeConfigList(); if (r.code === 200) typeOptions.value = r.data || [] } catch { /* */ }
}

const loadMajorOptions = async () => {
  try { const r = await getMajors(); if (r.code === 200) majorOptions.value = r.data || [] } catch { /* */ }
}

onMounted(() => { loadList(); loadTeachers(); loadGradeOptions(); loadTypeOptions(); loadMajorOptions() })
</script>

<style scoped lang="scss">
.mb-20 { margin-bottom: 20px; }
.batch-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; background: var(--primary-light);
  border-radius: var(--radius-md); margin-bottom: 16px; font-size: var(--fs-sm);
}
.filter-bar {
  display: flex; gap: 8px; margin-bottom: 20px;
  overflow-x: auto; -webkit-overflow-scrolling: touch;
}
.filter-bar > * { flex-shrink: 0; }

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.class-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 0.5px solid var(--border-light);
  overflow: hidden;
  transition: all var(--transition-base);
  cursor: default;

  &:hover {
    box-shadow: var(--shadow-base);
    border-color: var(--primary-color);
  }

  .class-card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 16px 0;

    .class-icon {
      width: 44px; height: 44px;
      border-radius: var(--radius-lg);
      background: var(--primary-light);
      color: var(--primary-color);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--fs-lg); font-weight: 500;
      flex-shrink: 0;
    }
    .class-info { flex: 1; min-width: 0; }
    .class-name { font-size: var(--fs-md); font-weight: 500; margin: 0 0 2px; color: var(--text-primary); }
    .class-code { font-size: var(--fs-xs); color: var(--text-secondary); }
  }

  .class-body { padding: 12px 16px; }
  .class-meta {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 6px 16px;
    margin-bottom: 12px;
    .meta-item { display: flex; gap: 4px; font-size: var(--fs-xs); }
    .meta-label { color: var(--text-secondary); }
    .meta-value { color: var(--text-primary); font-weight: 500; }
  }

  .class-students {
    padding: 10px 12px;
    background: var(--bg-secondary);
    border-radius: var(--radius-md);
    .students-count { display: flex; align-items: center; gap: 6px; font-size: var(--fs-sm); color: var(--text-regular); strong { color: var(--primary-color); } }
  }

  .class-actions {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid var(--border-light);
  }
}

@media (max-width: 768px) {
  .class-grid { grid-template-columns: 1fr; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .filter-bar :deep(.el-select) { width: 100%; margin-left: 0; }
  .filter-bar :deep(.el-button) { width: 100%; margin-left: 0; }
  .batch-bar { flex-direction: column; align-items: stretch; }
}
</style>
