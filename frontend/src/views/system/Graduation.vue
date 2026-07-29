<template>
  <div class="graduation-page">
    <!-- 毕业操作（仅超管） -->
    <div v-if="isSuperAdmin" class="section">
      <h4>🎓 批量毕业操作</h4>
      <div class="graduate-controls">
        <el-select v-model="gradScope" size="default" style="width:140px">
          <el-option value="grade" label="按年级" />
          <el-option value="class" label="按班级" />
          <el-option value="all" label="全部在校生" />
        </el-select>
        <el-select
          v-if="gradScope === 'grade'"
          v-model="gradGrade"
          placeholder="选择年级"
          size="default"
          style="width:140px"
        >
          <el-option
            v-for="g in gradeOptions"
            :key="g"
            :value="g"
            :label="g"
          />
        </el-select>
        <el-select
          v-if="gradScope === 'class'"
          v-model="gradClassId"
          placeholder="选择班级"
          size="default"
          style="width:200px"
        >
          <el-option
            v-for="c in classOptions"
            :key="c.id"
            :value="c.id"
            :label="(c.grade||'')+c.className"
          />
        </el-select>
        <el-button type="warning" :loading="gradLoading" @click="doGraduate">执行毕业</el-button>
      </div>
    </div>

    <!-- 毕业生名录 -->
    <div class="section">
      <h4>🎉 毕业生名录</h4>
      <el-select
        v-model="viewGrade"
        placeholder="筛选年级"
        clearable
        size="default"
        style="width:140px;margin-bottom:12px"
        @change="loadGraduates"
      >
        <el-option
          v-for="g in gradeOptions"
          :key="g"
          :value="g"
          :label="g"
        />
      </el-select>
      <div v-if="gLoading" class="sk-list"><div v-for="i in 4" :key="i" class="sk-row"><div class="sk-line w-20"></div><div class="sk-line w-30"></div><div class="sk-line w-20"></div><div class="sk-line w-30"></div></div></div>
      <el-table
        v-else
        :data="graduates"
        stripe
        size="small"
        empty-text="暂无毕业生"
      >
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="studentNumber" label="学号" width="120" />
        <el-table-column prop="grade" label="年级" width="90" />
        <el-table-column prop="className" label="班级" width="100" />
        <el-table-column label="毕业时间" min-width="140">
          <template #default="{ row }">{{ row.graduatedAt || row.updateTime?.slice(0,10) || '-' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getStudentList, batchGraduate } from '@/api/student'
import { getGrades } from '@/api/settings'
import { getClassList } from '@/api/classes'

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)

const gradeOptions = ref([])
const classOptions = ref([])
const graduates = ref([])
const gLoading = ref(false)
const viewGrade = ref('')

// 毕业操作
const gradScope = ref('grade')
const gradGrade = ref('')
const gradClassId = ref(null)
const gradLoading = ref(false)

const loadGraduates = async () => {
  gLoading.value = true
  try {
    const params = { status: 'graduated' }
    if (viewGrade.value) params.grade = viewGrade.value
    const r = await getStudentList(params)
    if (r.code === 200) graduates.value = r.data.records || []
  } finally { gLoading.value = false }
}

const doGraduate = async () => {
  try {
    const label = gradScope.value === 'all' ? '全部在校学生' :
      gradScope.value === 'grade' ? gradGrade.value :
      classOptions.value.find(c => c.id === gradClassId.value)?.className || ''
    await ElMessageBox.confirm(`确定将「${label}」的学生全部毕业吗？毕业后学生仍可登录系统。`, '确认毕业', {
      type: 'warning', confirmButtonText: '确认毕业'
    })
    gradLoading.value = true
    const body = { scope: gradScope.value }
    if (gradScope.value === 'grade') body.grade = gradGrade.value
    if (gradScope.value === 'class') body.classId = gradClassId.value
    const r = await batchGraduate(body)
    if (r.code === 200) { ElMessage.success(r.data || '毕业完成'); loadGraduates() }
    else ElMessage.error(r.message || '操作失败')
  } catch { /* */ } finally { gradLoading.value = false }
}

onMounted(async () => {
  loadGraduates()
  try {
    const [gR, cR] = await Promise.all([
      getGrades(),
      getClassList()
    ])
    if (gR.code === 200) gradeOptions.value = (gR.data||[]).map(g => g.gradeName)
    if (cR.code === 200) classOptions.value = (cR.data.records||[]).map(c => ({ id: c.id, className: c.className, grade: c.grade||'' }))
  } catch { /* */ }
})
</script>

<style scoped lang="scss">
.graduation-page { max-width: 900px; }
.section {
  background: var(--bg-card); border-radius: var(--radius-lg); padding: 20px; margin-bottom: 20px; box-shadow: var(--shadow-sm);
  h4 { margin: 0 0 12px; font-size: var(--fs-lg); }
}
.graduate-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.sk-list { padding: 8px 0; }
.sk-row { display: flex; gap: 16px; padding: 16px 12px; border-bottom: 1px solid var(--border-light); }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); position: relative; overflow: hidden; }
.sk-line::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { transform: translateX(-100%) } 100% { transform: translateX(100%) } }
.w-20 { width: 20% } .w-30 { width: 30% }

@media (max-width: 768px) {
  .graduate-controls { flex-direction: column; align-items: stretch; }
}
</style>
