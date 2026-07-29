<template>
  <div class="group-management">
    <div class="page-header">
      <h2>分组管理</h2>
    </div>

    <el-row :gutter="20">
      <!-- 左侧分组列表 -->
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>
            <span>分组</span>
            <el-button
              size="small"
              type="primary"
              style="float:right"
              @click="showAddGroup = true"
            >
              新建分组
            </el-button>
          </template>
          <div v-if="loadingGroups" class="empty-hint" style="font-size:var(--fs-xs)"><i>加载中...</i></div>
          <div v-else-if="groups.length === 0" class="empty-hint">暂无分组，请先创建</div>
          <div
            v-for="g in groups"
            :key="g.id"
            class="group-item"
            :class="{ active: selectedGroup?.id === g.id }"
            @click="selectGroup(g)"
          >
            <span>{{ g.name }}</span>
            <el-button
              size="small"
              type="danger"
              text
              @click.stop="doDeleteGroup(g)"
            >
              删除
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧成员列表 -->
      <el-col :xs="24" :md="16">
        <el-card v-if="selectedGroup" shadow="never">
          <template #header>
            <span>成员 — {{ selectedGroup.name }}</span>
            <el-button
              size="small"
              type="primary"
              style="float:right"
              @click="showAddMember = true"
            >
              添加学生
            </el-button>
          </template>
          <el-table
            v-loading="memberLoading"
            :data="members"
            size="small"
            empty-text="暂无成员，点击右上角添加"
          >
            <el-table-column prop="studentName" label="姓名" />
            <el-table-column prop="studentNumber" label="学号" width="120" />
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="danger"
                  text
                  @click="doRemoveMember(row)"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-empty v-else description="请选择一个分组" />
      </el-col>
    </el-row>

    <!-- 新建分组弹窗 -->
    <el-dialog v-model="showAddGroup" title="新建分组" width="360px">
      <el-input v-model="newGroupName" placeholder="分组名称（如 A组、B组）" />
      <template #footer>
        <el-button @click="showAddGroup = false">取消</el-button>
        <el-button type="primary" :disabled="!newGroupName.trim()" @click="doAddGroup">创建</el-button>
      </template>
    </el-dialog>

    <!-- 添加学生弹窗 -->
    <el-dialog v-model="showAddMember" title="添加学生" width="400px">
      <el-select
        v-model="selectedStudentId"
        placeholder="搜索学生"
        filterable
        remote
        :remote-method="searchStudents"
        :loading="studentSearchLoading"
        style="width:100%"
      >
        <el-option
          v-for="s in studentOptions"
          :key="s.id"
          :label="`${s.realName} (${s.studentNumber})`"
          :value="s.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="showAddMember = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedStudentId" @click="doAddMember">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGroups, createGroup, deleteGroup, getMembers, addMember, removeMember } from '@/api/group'
import { getStudentList } from '@/api/student'
import { useRoute } from 'vue-router'

const route = useRoute()
const classId = ref(Number(route.query.classId) || 1)
const loadingGroups = ref(false)
const groups = ref([])
const selectedGroup = ref(null)
const members = ref([])
const memberLoading = ref(false)
const showAddGroup = ref(false)
const newGroupName = ref('')
const showAddMember = ref(false)
const selectedStudentId = ref(null)
const studentOptions = ref([])
const studentSearchLoading = ref(false)

const loadGroups = async () => {
  loadingGroups.value = true
  try {
    const res = await getGroups(classId.value)
    if (res.code === 200) groups.value = res.data || []
  } finally { loadingGroups.value = false }
}
const selectGroup = async (g) => {
  selectedGroup.value = g
  memberLoading.value = true
  try {
    const res = await getMembers(g.id)
    if (res.code === 200) members.value = res.data || []
  } finally { memberLoading.value = false }
}
const doAddGroup = async () => {
  try {
    const res = await createGroup(classId.value, newGroupName.value.trim())
    if (res.code === 200) { ElMessage.success('已创建'); showAddGroup.value = false; newGroupName.value = ''; loadGroups() }
  } catch { /* */ }
}
const doDeleteGroup = async (g) => {
  try {
    await ElMessageBox.confirm(`确定删除分组"${g.name}"？`, '提示', { type: 'warning' })
    await deleteGroup(g.id)
    ElMessage.success('已删除')
    if (selectedGroup.value?.id === g.id) selectedGroup.value = null
    loadGroups()
  } catch { /* cancelled */ }
}
const searchStudents = async (kw) => {
  if (!kw.trim()) { studentOptions.value = []; return }
  studentSearchLoading.value = true
  try {
    const r2 = await getStudentList({ keyword: kw, classId: classId.value, page: 1, pageSize: 20 })
    if (r2.code === 200) studentOptions.value = (r2.data.records || []).map(s => ({ id: s.id, realName: s.realName, studentNumber: s.studentNumber }))
  } finally { studentSearchLoading.value = false }
}
const doAddMember = async () => {
  try {
    await addMember(selectedGroup.value.id, selectedStudentId.value)
    ElMessage.success('已添加')
    showAddMember.value = false
    selectedStudentId.value = null
    selectGroup(selectedGroup.value)
  } catch { /* */ }
}
const doRemoveMember = async (row) => {
  try {
    await ElMessageBox.confirm(`确定将 ${row.studentName} 移出分组？`, '提示', { type: 'warning' })
    await removeMember(selectedGroup.value.id, row.studentId)
    ElMessage.success('已移除')
    selectGroup(selectedGroup.value)
  } catch { /* cancelled */ }
}

onMounted(loadGroups)
</script>

<style scoped>
.group-management { max-width: 1100px; margin: 0 auto; padding: 8px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: var(--fs-xl); }
.group-item { padding: 8px 12px; border-radius: 4px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.group-item:hover { background: var(--bg-secondary); }
.group-item.active { background: var(--primary-light); color: var(--primary-color); font-weight: 500; }
.empty-hint { color: var(--text-secondary); text-align: center; padding: 24px; }

@media (max-width: 768px) {
  :deep(.el-table .el-table__body-wrapper) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
