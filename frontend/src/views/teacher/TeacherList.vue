<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">教师管理</h3>
      <el-button type="primary" @click="showCreate">
        <el-icon><Plus /></el-icon>添加教师
      </el-button>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form :model="search" inline>
        <el-form-item label="搜索">
          <el-input
            v-model="search.keyword"
            placeholder="姓名/用户名"
            clearable
            @clear="doSearch"
            @keyup.enter="doSearch"
          />
        </el-form-item>
        <el-form-item label="学科">
          <el-select
            v-model="search.subject"
            placeholder="全部"
            clearable
            filterable
            @change="doSearch"
          >
            <el-option
              v-for="s in subjectOptions"
              :key="s.id"
              :value="s.subjectName"
              :label="s.subjectName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="组长">
          <el-select
            v-model="search.isLeader"
            placeholder="全部"
            clearable
            @change="doSearch"
          >
            <el-option :value="true" label="是" />
            <el-option :value="false" label="否" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table v-loading="loading" :data="list" stripe>
      <template #empty><el-empty description="暂无教师数据" :image-size="60" /></template>
      <el-table-column prop="realName" label="姓名" width="90" />
      <el-table-column prop="username" label="用户名" width="90" />
      <el-table-column prop="teacherNumber" label="工号" width="100" />
      <el-table-column label="性别" width="55">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="200">
        <template #default="{ row }">
          <div style="display:flex;gap:4px;flex-wrap:wrap">
            <el-tag v-if="row.roleId & 8" type="danger" size="small">超级管理员</el-tag>
            <el-tag v-if="row.roleId & 16" type="info" size="small">巡视员</el-tag>
            <el-tag v-if="row.roleId & 1" type="danger" size="small">管理员</el-tag>
            <el-tag v-if="row.roleId & 2" type="warning" size="small">教师</el-tag>
            <el-tag v-if="(row.roleId & 2) && row.isHeadTeacher" type="warning" size="small">班主任</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="组长职务" width="120">
        <template #default="{ row }">
          <template v-if="leaderMap[row.userId]">
            <el-tag
              v-for="g in leaderMap[row.userId]"
              :key="g.id"
              size="small"
              type="warning"
              class="leader-tag"
            >
              {{ g.name }}
            </el-tag>
          </template>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="任教班级" min-width="150">
        <template #default="{ row }">
          <span v-if="row.teachingClasses && row.teachingClasses.length" class="class-tags">
            <el-tag
              v-for="tc in row.teachingClasses"
              :key="tc.id"
              size="small"
              type="info"
              class="class-tag"
            >
              {{ (tc.grade || '') + tc.className }}
            </el-tag>
          </span>
          <span v-else class="text-muted">未配置</span>
        </template>
      </el-table-column>
      <el-table-column label="任教学科" min-width="120">
        <template #default="{ row }">
          <span v-if="row.teachingClasses && row.teachingClasses.length" class="class-tags">
            <el-tag
              v-for="sub in [...new Set(row.teachingClasses.map(tc => tc.subject).filter(Boolean))]"
              :key="sub"
              size="small"
              type="success"
              class="class-tag"
            >{{ sub }}</el-tag>
          </span>
          <span v-else-if="row.subjects && row.subjects.length" class="class-tags">
            <el-tag
              v-for="sub in row.subjects"
              :key="sub"
              size="small"
              type="success"
              class="class-tag"
            >{{ sub }}</el-tag>
          </span>
          <span v-else class="text-muted">未配置</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="65">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="handleResetPassword(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <TeacherFormDialog
      v-model="formVisible"
      :is-edit="isEdit"
      :edit-data="editData"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherList, deleteTeacher } from '@/api/teacher'
import { getSubjects } from '@/api/settings'
import { adminResetPassword } from '@/api/profile'
import { getTeachingGroups, getLessonPrepGroups } from '@/api/teachingGroups'
import TeacherFormDialog from './TeacherFormDialog.vue'

const loading = ref(false)
const list = ref([])
const fullList = ref([])
const search = reactive({ keyword: '', subject: '', isLeader: null })
const subjectOptions = ref([])
const leaderMap = ref({})

const formVisible = ref(false)
const isEdit = ref(false)
const editData = ref(null)

const loadSubjectOptions = async () => {
  try { const res = await getSubjects(); if (res.code === 200) subjectOptions.value = res.data } catch { /* */ } }

const buildTeacherIdToUserId = () => {
  const m = {}
  fullList.value.forEach(t => { if (t.id && t.userId) m[t.id] = t.userId })
  return m
}

const resolveLeaders = (leaderIds) => {
  if (!leaderIds) return []
  try { return typeof leaderIds === 'string' ? JSON.parse(leaderIds) : leaderIds } catch { return [] }
}

const loadGroups = async () => {
  try {
    const [tRes, lRes] = await Promise.all([getTeachingGroups(), getLessonPrepGroups()])
    const t2u = buildTeacherIdToUserId()
    const map = {}
    const addLeaders = (group, type) => {
      const ids = resolveLeaders(group.leaderIds)
      ids.forEach(tid => {
        const uid = t2u[tid] || tid
        const arr = map[uid] || []
        arr.push({ id: group.id, name: group.name, type })
        map[uid] = arr
      })
    }
    if (tRes.code === 200) (tRes.data||[]).forEach(g => addLeaders(g, '教研组长'))
    if (lRes.code === 200) (lRes.data||[]).forEach(g => addLeaders(g, '备课组长'))
    leaderMap.value = map
  } catch { /* */ }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getTeacherList({})
    if (res.code === 200) {
      fullList.value = res.data.records || []
      applyFilter()
    }
  } finally { loading.value = false }
}

const applyFilter = () => {
  let arr = fullList.value
  if (search.keyword) {
    const kw = search.keyword.toLowerCase()
    arr = arr.filter(t => t.realName?.toLowerCase().includes(kw) || t.username?.toLowerCase().includes(kw))
  }
  if (search.subject) {
    arr = arr.filter(t => {
      if (!t.teachingClasses?.length) return false
      return t.teachingClasses.some(tc => tc.subject === search.subject)
    })
  }
  if (search.isLeader === true) arr = arr.filter(t => leaderMap.value[t.userId]?.length > 0)
  else if (search.isLeader === false) arr = arr.filter(t => !leaderMap.value[t.userId]?.length)
  list.value = arr
}

const doSearch = () => applyFilter()

const showCreate = () => { isEdit.value = false; editData.value = null; formVisible.value = true }
const showEdit = (row) => { isEdit.value = true; editData.value = row; formVisible.value = true }
const onSaved = () => { formVisible.value = false; loadList() }

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除教师「${row.realName}」吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    const res = await deleteTeacher(row.userId)
    if (res.code === 200) { ElMessage.success('已删除'); await loadList() }
  } catch { /* cancelled */ }
}

const handleResetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `为「${row.realName}」重置密码（留空则自动生成）`,
      '重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '输入新密码，留空则自动生成' }
    )
    const res = await adminResetPassword(row.userId, { newPassword: value || '' })
    if (res.code === 200) {
      ElMessage.success(`密码已重置为: ${res.data.newPassword}`)
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  } catch { /* cancelled */ }
}

onMounted(() => { loadList(); loadSubjectOptions(); loadGroups() })
</script>

<style scoped>
.search-card { margin-bottom: 16px; }
.search-card :deep(.el-card__body) { padding: 16px 20px; }
.class-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.class-tag { max-width: 140px; overflow: hidden; text-overflow: ellipsis; }
.text-muted { color: var(--text-secondary); font-size: var(--fs-xs); }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
  .page-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  :deep(.el-form--inline .el-form-item) { margin-right: 0; width: 100%; margin-bottom: 8px; }
  :deep(.el-form--inline .el-select), :deep(.el-form--inline .el-input) { width: 100%; }
  :deep(.el-form--inline .el-button) { width: 100%; margin-left: 0; }
}
</style>
