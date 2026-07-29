<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">👥 教研备课</h3>
      <span class="page-subtitle">管理教研组和备课组，配置组长与成员</span>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="📋 教研组管理" name="teaching">
        <div style="margin-bottom:12px">
          <el-button type="primary" size="default" @click="showTeachingDialog()">新增教研组</el-button>
        </div>
        <div v-if="tLoading" class="sk-list"><div v-for="i in 4" :key="i" class="sk-row"><div class="sk-line w-40"></div><div class="sk-line w-30"></div><div class="sk-line w-20"></div></div></div>
        <el-table
          v-else
          :data="teachingGroups"
          stripe
          empty-text="暂无教研组"
        >
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column label="学段" min-width="120">
            <template #default="{ row }">{{ stageNames(row.stageIds) }}</template>
          </el-table-column>
          <el-table-column label="学科" min-width="160">
            <template #default="{ row }">{{ subjectNames(row.subjectIds) }}</template>
          </el-table-column>
          <el-table-column label="组长" min-width="100">
            <template #default="{ row }">{{ row.leaderNames || row.leaderName || '未设置' }}</template>
          </el-table-column>
          <el-table-column label="成员数" width="70" align="center">
            <template #default="{ row }">{{ row.memberCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center">
            <template #default="{ row }">
              <el-button
                size="small"
                text
                type="primary"
                @click="showTeachingDialog(row)"
              >
                编辑
              </el-button>
              <el-button size="small" text @click="showMemberDialog(row, 'TEACHING')">成员</el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="delTeaching(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="📋 备课组管理" name="lesson">
        <div style="margin-bottom:12px">
          <el-button type="primary" size="default" @click="showLessonDialog()">新增备课组</el-button>
        </div>
        <div v-if="lLoading" class="sk-list"><div v-for="i in 4" :key="i" class="sk-row"><div class="sk-line w-40"></div><div class="sk-line w-30"></div><div class="sk-line w-20"></div></div></div>
        <el-table
          v-else
          :data="lessonGroups"
          stripe
          empty-text="暂无备课组"
        >
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column label="年级" width="80"><template #default="{ row }">{{ row.gradeId || '-' }}</template></el-table-column>
          <el-table-column label="学科" width="80"><template #default="{ row }">{{ row.subjectId || '-' }}</template></el-table-column>
          <el-table-column label="组长" width="100"><template #default="{ row }">{{ row.leaderName || '未设置' }}</template></el-table-column>
          <el-table-column label="成员数" width="70" align="center"><template #default="{ row }">{{ row.memberCount || 0 }}</template></el-table-column>
          <el-table-column label="操作" width="220" align="center">
            <template #default="{ row }">
              <el-button
                size="small"
                text
                type="primary"
                @click="showLessonDialog(row)"
              >
                编辑
              </el-button>
              <el-button size="small" text @click="showMemberDialog(row, 'LESSON_PREP')">成员</el-button>
              <el-button
                size="small"
                text
                type="danger"
                @click="delLesson(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="⚙️ 审核设置" name="settings">
        <el-form label-position="top" style="max-width:400px">
          <el-form-item label="考试审核功能">
            <el-switch
              v-model="reviewEnabled"
              active-value="true"
              inactive-value="false"
              @change="toggleReview"
            />
            <span style="margin-left:8px;font-size:var(--fs-xs);color:var(--text-secondary)">启用后，考试任务需经过备课组长和教研组长审核</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- 教研组对话框 -->
    <el-dialog
      v-model="tDialogVisible"
      :title="tEditing ? '编辑教研组' : '新增教研组'"
      width="500px"
      destroy-on-close
      append-to-body
    >
      <el-form ref="tFormRef" :model="tForm" label-position="top">
        <el-form-item label="名称"><el-input v-model="tForm.name" placeholder="如：语文教研组" /></el-form-item>
        <el-form-item label="学段（可多选）">
          <el-select
            v-model="tForm.stageIds"
            multiple
            style="width:100%"
            placeholder="选择学段"
          >
            <el-option
              v-for="s in stages"
              :key="s.id"
              :value="s.id"
              :label="s.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="涵盖学科（可多选）">
          <el-select
            v-model="tForm.subjectIds"
            multiple
            filterable
            style="width:100%"
            placeholder="选择学科"
          >
            <el-option
              v-for="s in allSubjects"
              :key="s.id"
              :value="s.id"
              :label="s.subjectName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="组长（最多2人）">
          <el-select
            v-model="tForm.leaderIds"
            multiple
            filterable
            style="width:100%"
            placeholder="选择组长"
            :multiple-limit="2"
          >
            <el-option
              v-for="t in teachers"
              :key="t.id"
              :value="t.id"
              :label="t.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="tDialogVisible = false">取消</el-button><el-button type="primary" :loading="tSaving" @click="saveTeaching">确定</el-button></template>
    </el-dialog>

    <!-- 备课组对话框 -->
    <el-dialog
      v-model="lDialogVisible"
      :title="lEditing ? '编辑备课组' : '新增备课组'"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <el-form ref="lFormRef" :model="lForm" label-position="top">
        <el-form-item label="名称"><el-input v-model="lForm.name" placeholder="如：高一语文备课组" /></el-form-item>
        <el-form-item label="所属教研组">
          <el-select
            v-model="lForm.teachingGroupId"
            clearable
            filterable
            style="width:100%"
          >
            <el-option
              v-for="g in teachingGroups"
              :key="g.id"
              :value="g.id"
              :label="g.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select
            v-model="lForm.gradeId"
            filterable
            clearable
            style="width:100%"
            placeholder="选择年级"
          >
            <el-option
              v-for="g in allGrades"
              :key="g.id"
              :value="g.id"
              :label="g.gradeName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学科">
          <el-select
            v-model="lForm.subjectId"
            filterable
            clearable
            style="width:100%"
            placeholder="选择学科"
          >
            <el-option
              v-for="s in allSubjects"
              :key="s.id"
              :value="s.id"
              :label="s.subjectName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学段">
          <el-select v-model="lForm.stageId" style="width:100%">
            <el-option
              v-for="s in stages"
              :key="s.id"
              :value="s.id"
              :label="s.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="组长">
          <el-select
            v-model="lForm.leaderId"
            filterable
            clearable
            style="width:100%"
          >
            <el-option
              v-for="t in teachers"
              :key="t.id"
              :value="t.id"
              :label="t.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="lDialogVisible = false">取消</el-button><el-button type="primary" :loading="lSaving" @click="saveLesson">确定</el-button></template>
    </el-dialog>

    <!-- 成员管理对话框 -->
    <el-dialog
      v-model="mDialogVisible"
      title="成员管理"
      width="500px"
      destroy-on-close
      append-to-body
    >
      <div style="margin-bottom:12px;display:flex;gap:8px">
        <el-select
          v-model="selTeacherId"
          filterable
          placeholder="选择教师"
          style="flex:1"
        >
          <el-option
            v-for="t in teachers"
            :key="t.id"
            :value="t.id"
            :label="t.name"
          />
        </el-select>
        <el-button type="primary" :disabled="!selTeacherId" @click="addMember">添加</el-button>
      </div>
      <el-table
        v-loading="mLoading"
        :data="memberList"
        stripe
        empty-text="暂无成员"
      >
        <el-table-column label="姓名"><template #default="{ row }">{{ row.name }}</template></el-table-column><el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              size="small"
              text
              type="danger"
              @click="removeMember(row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeachingGroups, createTeachingGroup, updateTeachingGroup, deleteTeachingGroup, addTeachingMember, removeTeachingMember, getTeachingGroupMembers,
         getLessonPrepGroups, createLessonPrepGroup, updateLessonPrepGroup, deleteLessonPrepGroup, addLessonPrepMember, removeLessonPrepMember, getLessonPrepGroupMembers } from '@/api/teachingGroups'
import { getTeacherList } from '@/api/teacher'
import { getSettings, updateSettings, getSubjects, getGrades } from '@/api/settings'

const tab = ref('teaching')
const stages = [{id:1,name:'小学'},{id:2,name:'初中'},{id:3,name:'普高'},{id:4,name:'职高'}]
const teachers = ref([])
const allSubjects = ref([])
const allGrades = ref([])
const teachingGroups = ref([]); const lessonGroups = ref([])
const tLoading = ref(false); const lLoading = ref(false)
const reviewEnabled = ref('false')

// 辅助：ID→名称映射
const subjectNameMap = computed(() => {
  const m = {}
  allSubjects.value.forEach(s => { m[s.id] = s.subjectName })
  return m
})
const stageNameMap = { 1:'小学', 2:'初中', 3:'普高', 4:'职高' }
const subjectNames = (ids) => {
  if (!ids) return '-'
  try { const arr = typeof ids === 'string' ? JSON.parse(ids) : ids; return arr.map(id => subjectNameMap.value[id] || id).join('、') || '-' }
  catch { return ids }
}
const stageNames = (ids) => {
  if (!ids) return '-'
  try { const arr = typeof ids === 'string' ? JSON.parse(ids) : ids; return arr.map(id => stageNameMap[id] || id).join('、') || '-' }
  catch { return ids }
}

// 教研组
const tDialogVisible = ref(false); const tEditing = ref(null); const tSaving = ref(false); const tFormRef = ref(null)
const parseIds = (v) => { if (!v) return []; try { return typeof v === 'string' ? JSON.parse(v) : v } catch { return [] } }
const tForm = reactive({ name:'', stageIds:[], leaderIds:[], subjectIds:[] })
const showTeachingDialog = (row) => {
  if (row) {
    tEditing.value = row
    Object.assign(tForm, { name:row.name, stageIds:parseIds(row.stageIds), leaderIds:parseIds(row.leaderIds), subjectIds:parseIds(row.subjectIds) })
  } else {
    tEditing.value = null
    Object.assign(tForm, { name:'', stageIds:[], leaderIds:[], subjectIds:[] })
  }
  tDialogVisible.value = true
}
const saveTeaching = async () => {
  if (tSaving.value) return; tSaving.value = true
  try {
    const payload = { ...tForm, stageIds: JSON.stringify(tForm.stageIds), subjectIds: JSON.stringify(tForm.subjectIds), leaderIds: JSON.stringify(tForm.leaderIds) }
    const r = tEditing.value ? await updateTeachingGroup(tEditing.value.id, payload) : await createTeachingGroup(payload)
    if (r.code===200) { ElMessage.success('已保存'); tDialogVisible.value=false; loadTeaching() }
  } catch { ElMessage.error('保存失败') } finally { tSaving.value = false }
}
const delTeaching = async (row) => { try { await ElMessageBox.confirm(`确定删除「${row.name}」？`, '确认', { type:'warning' }) } catch { return } await deleteTeachingGroup(row.id); ElMessage.success('已删除'); loadTeaching() }

// 备课组
const lDialogVisible = ref(false); const lEditing = ref(null); const lSaving = ref(false); const lFormRef = ref(null)
const lForm = reactive({ name:'', teachingGroupId:null, gradeId:null, subjectId:null, stageId:null, leaderId:null })
const showLessonDialog = (row) => {
  if (row) { lEditing.value = row; Object.assign(lForm, { name:row.name, teachingGroupId:row.teachingGroupId, gradeId:row.gradeId, subjectId:row.subjectId, stageId:row.stageId, leaderId:row.leaderId }) }
  else { lEditing.value = null; Object.assign(lForm, { name:'', teachingGroupId:null, gradeId:null, subjectId:null, stageId:null, leaderId:null }) }
  lDialogVisible.value = true
}
const saveLesson = async () => {
  if (lSaving.value) return; lSaving.value = true
  try { const r = lEditing.value ? await updateLessonPrepGroup(lEditing.value.id, lForm) : await createLessonPrepGroup(lForm); if (r.code===200) { ElMessage.success('已保存'); lDialogVisible.value=false; loadLesson() } } catch { ElMessage.error('保存失败') } finally { lSaving.value = false }
}
const delLesson = async (row) => { try { await ElMessageBox.confirm(`确定删除「${row.name}」？`, '确认', { type:'warning' }) } catch { return } await deleteLessonPrepGroup(row.id); ElMessage.success('已删除'); loadLesson() }

// 成员
const mDialogVisible = ref(false); const mGroupType = ref(''); const mGroupId = ref(null)
const selTeacherId = ref(null); const memberList = ref([]); const mLoading = ref(false)

const loadMemberList = async () => {
  mLoading.value = true
  try {
    const fn = mGroupType.value === 'TEACHING' ? getTeachingGroupMembers : getLessonPrepGroupMembers
    const r = await fn(mGroupId.value)
    if (r.code === 200) memberList.value = r.data || []
  } catch { memberList.value = [] }
  finally { mLoading.value = false }
}

const showMemberDialog = (row, type) => { mGroupType.value = type; mGroupId.value = row.id; selTeacherId.value = null; mDialogVisible.value = true; loadMemberList() }

const addMember = async () => {
  if (!selTeacherId.value) return
  const fn = mGroupType.value === 'TEACHING' ? addTeachingMember : addLessonPrepMember
  await fn(mGroupId.value, selTeacherId.value); ElMessage.success('已添加'); selTeacherId.value = null; loadMemberList()
}
const removeMember = async (row) => {
  const fn = mGroupType.value === 'TEACHING' ? removeTeachingMember : removeLessonPrepMember
  await fn(mGroupId.value, row.teacherId || row.id); ElMessage.success('已移除'); loadMemberList()
}

// 审核开关
const toggleReview = async (val) => { await updateSettings({ 'feature.review_enabled': val }); ElMessage.success('已更新') }

const loadTeaching = async () => { tLoading.value = true; try { const r = await getTeachingGroups(); if (r.code===200) teachingGroups.value = r.data||[] } finally { tLoading.value = false } }
const loadLesson = async () => { lLoading.value = true; try { const r = await getLessonPrepGroups(); if (r.code===200) lessonGroups.value = r.data||[] } finally { lLoading.value = false } }
const loadTeachers = async () => { try { const r = await getTeacherList(); if (r.code===200) teachers.value = (r.data?.records||[]).map(t=>({id:t.id,name:t.realName})) } catch{/* */} }
const loadReviewSettings = async () => { try { const r = await getSettings(); if (r.code===200) reviewEnabled.value = r.data?.['feature.review_enabled'] || 'false' } catch{/* */} }

const loadSubjects = async () => { try { const r = await getSubjects(); if (r.code===200) allSubjects.value = r.data||[] } catch { /* */ } }
const loadGrades = async (stageId) => { try { const r = await getGrades(stageId); if (r.code===200) allGrades.value = r.data||[] } catch { /* */ } }
watch(() => lForm.stageId, (sid) => { loadGrades(sid || undefined) })
onMounted(() => { loadTeaching(); loadLesson(); loadTeachers(); loadSubjects(); loadGrades(); loadReviewSettings() })
</script>

<style scoped>
.sk-list { padding: 8px 0; }
.sk-row { display: flex; gap: 16px; padding: 16px 12px; border-bottom: 1px solid var(--border-light); }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); position: relative; overflow: hidden; }
.sk-line::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { transform: translateX(-100%) } 100% { transform: translateX(100%) } }
.w-20 { width: 20% } .w-30 { width: 30% } .w-40 { width: 40% }

@media (max-width: 768px) {
  :deep(.el-tabs__item) { font-size: var(--fs-sm); padding: 0 10px!important; }
  :deep(.el-table) { font-size: var(--fs-xs); }
  :deep(.el-table .el-button) { font-size: var(--fs-xs); padding: 4px 6px; }
}
</style>
