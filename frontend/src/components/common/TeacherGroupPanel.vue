<template>
  <div class="group-section">
    <el-divider style="margin:8px 0 16px" />
    <h4 style="margin:0 0 12px;font-size:var(--fs-md)">教研/备课组成员</h4>
    <div v-if="teacherGroups.length===0 && teacherLessonGroups.length===0" class="text-muted" style="margin-bottom:12px">暂未加入任何教研组或备课组</div>
    <div v-if="teacherGroups.length" style="margin-bottom:10px">
      <span style="font-size:var(--fs-sm);font-weight:500">教研组：</span>
      <el-tag
        v-for="g in teacherGroups"
        :key="'t'+g.id"
        size="small"
        type="warning"
        closable
        style="margin:2px 4px"
        @close="leaveGroup('TEACHING', g.id)"
      >
        {{ g.name }}
      </el-tag>
    </div>
    <div v-if="teacherLessonGroups.length" style="margin-bottom:10px">
      <span style="font-size:var(--fs-sm);font-weight:500">备课组：</span>
      <el-tag
        v-for="g in teacherLessonGroups"
        :key="'l'+g.id"
        size="small"
        type="success"
        closable
        style="margin:2px 4px"
        @close="leaveGroup('LESSON_PREP', g.id)"
      >
        {{ g.name }}
      </el-tag>
    </div>
    <div style="display:flex;gap:8px;margin-top:8px">
      <el-select
        v-model="joinGroupId"
        placeholder="选择教研组"
        filterable
        clearable
        size="small"
        style="flex:1"
      >
        <el-option
          v-for="g in availableTeachingGroups"
          :key="g.id"
          :value="g.id"
          :label="g.name"
        />
      </el-select>
      <el-button
        size="small"
        type="primary"
        :disabled="!joinGroupId"
        @click="joinGroup('TEACHING')"
      >
        加入
      </el-button>
    </div>
    <div style="display:flex;gap:8px;margin-top:6px">
      <el-select
        v-model="joinLessonGroupId"
        placeholder="选择备课组"
        filterable
        clearable
        size="small"
        style="flex:1"
      >
        <el-option
          v-for="g in availableLessonGroups"
          :key="g.id"
          :value="g.id"
          :label="g.name"
        />
      </el-select>
      <el-button
        size="small"
        type="primary"
        :disabled="!joinLessonGroupId"
        @click="joinGroup('LESSON_PREP')"
      >
        加入
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeachingGroups, addTeachingMember, removeTeachingMember, getTeacherGroupIds, getTeacherLessonGroupIds,
         getLessonPrepGroups, addLessonPrepMember, removeLessonPrepMember } from '@/api/teachingGroups'

const props = defineProps({ teacherId: { type: Number, default: null } })

const allTeachingGroups = ref([]); const allLessonGroups = ref([])
const teacherGroups = ref([]); const teacherLessonGroups = ref([])
const joinGroupId = ref(null); const joinLessonGroupId = ref(null)

const availableTeachingGroups = computed(() => {
  const ids = teacherGroups.value.map(g => g.id)
  return allTeachingGroups.value.filter(g => !ids.includes(g.id))
})
const availableLessonGroups = computed(() => {
  const ids = teacherLessonGroups.value.map(g => g.id)
  return allLessonGroups.value.filter(g => !ids.includes(g.id))
})

let groupsLoaded = false
const loadGroups = async () => {
  try {
    const [tRes, lRes] = await Promise.all([getTeachingGroups(), getLessonPrepGroups()])
    if (tRes.code === 200) allTeachingGroups.value = tRes.data || []
    if (lRes.code === 200) allLessonGroups.value = lRes.data || []
    groupsLoaded = true
  } catch { /* */ }
}

const loadTeacherGroups = async () => {
  if (!props.teacherId) return
  if (!groupsLoaded) await loadGroups()
  try {
    const [tRes, lRes] = await Promise.all([
      getTeacherGroupIds(props.teacherId),
      getTeacherLessonGroupIds(props.teacherId)
    ])
    if (tRes.code === 200) {
      const ids = tRes.data || []
      teacherGroups.value = allTeachingGroups.value.filter(g => ids.includes(g.id)).map(g => ({ id: g.id, name: g.name }))
    }
    if (lRes.code === 200) {
      const ids = lRes.data || []
      teacherLessonGroups.value = allLessonGroups.value.filter(g => ids.includes(g.id)).map(g => ({ id: g.id, name: g.name }))
    }
  } catch { /* */ }
}

const joinGroup = async (type) => {
  const gid = type === 'TEACHING' ? joinGroupId.value : joinLessonGroupId.value
  if (!gid || !props.teacherId) return
  const fn = type === 'TEACHING' ? addTeachingMember : addLessonPrepMember
  try { await fn(gid, props.teacherId); ElMessage.success('已加入'); if (type==='TEACHING') joinGroupId.value=null; else joinLessonGroupId.value=null; await loadTeacherGroups() }
  catch { ElMessage.error('加入失败') }
}

const leaveGroup = async (type, groupId) => {
  if (!props.teacherId) return
  const fn = type === 'TEACHING' ? removeTeachingMember : removeLessonPrepMember
  try { await fn(groupId, props.teacherId); ElMessage.success('已移出'); await loadTeacherGroups() }
  catch { ElMessage.error('移出失败') }
}

onMounted(() => { loadGroups() })

defineExpose({ loadTeacherGroups })
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
