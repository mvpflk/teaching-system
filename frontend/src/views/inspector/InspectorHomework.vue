<template>
  <div class="inspector-homework">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">作业完成看板</h3>
        <span v-if="total > 0" class="header-subtitle">共 {{ total }} 条记录</span>
      </div>

      <div class="filter-bar">
        <el-select
          v-model="filters.grade"
          placeholder="年级"
          clearable
          size="default"
          @change="onGradeChange"
        >
          <el-option
            v-for="g in gradeOptions"
            :key="g"
            :value="g"
            :label="g"
          />
        </el-select>
        <el-select
          v-model="filters.classId"
          placeholder="班级"
          clearable
          size="default"
          @change="onFilterChange"
        >
          <el-option
            v-for="c in filteredClassOptions"
            :key="c.id"
            :value="c.id"
            :label="(c.grade||'') + c.className"
          />
        </el-select>
        <el-select
          v-model="filters.taskId"
          placeholder="选择任务"
          clearable
          filterable
          size="default"
          @change="onFilterChange"
        >
          <el-option
            v-for="t in taskOptions"
            :key="t.id"
            :value="t.id"
            :label="t.title"
          />
        </el-select>
        <el-button size="default" @click="resetFilters"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        empty-text="暂无作业数据"
        highlight-current-row
      >
        <el-table-column
          prop="title"
          label="任务"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="subject"
          label="学科"
          width="120"
          align="center"
        />
        <el-table-column
          prop="grade"
          label="年级"
          width="90"
          align="center"
        />
        <el-table-column
          prop="className"
          label="班级"
          width="100"
          align="center"
        />
        <el-table-column
          prop="teacherName"
          label="教师"
          width="90"
          align="center"
        />
        <el-table-column
          prop="studentCount"
          label="应提交"
          width="75"
          align="center"
        />
        <el-table-column
          prop="submittedCount"
          label="已提交"
          width="75"
          align="center"
        />
        <el-table-column
          prop="notSubmittedCount"
          label="未提交"
          width="75"
          align="center"
        >
          <template #default="{ row }">
            <span :style="{ color: row.notSubmittedCount > 0 ? 'var(--danger-color)' : 'var(--success-color)' }">{{ row.notSubmittedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交率" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.submitRate >= 80 ? 'var(--success-color)' : row.submitRate >= 50 ? 'var(--warning-color)' : 'var(--danger-color)' }">
              {{ row.submitRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="gradedCount"
          label="已批改"
          width="75"
          align="center"
        />
        <el-table-column label="批改率" width="75" align="center">
          <template #default="{ row }">{{ row.gradeRate }}%</template>
        </el-table-column>
        <el-table-column
          prop="avgScore"
          label="均分"
          width="70"
          align="center"
        />
        <el-table-column label="及格率" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: (row.passRate||0) >= 60 ? 'var(--success-color)' : 'var(--danger-color)' }">{{ row.passRate }}%</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="list.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无作业数据" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getScoreAnalysis } from '@/api/inspector'
import { listTasks } from '@/api/task'
import { getGrades } from '@/api/settings'
import { getClassList } from '@/api/classes'

const list = ref([])
const loading = ref(false)
const total = computed(() => list.value.length)

const taskOptions = ref([])
const classOptions = ref([])
const gradeOptions = ref([])

const filters = reactive({ grade: '', classId: null, taskId: null })

const filteredClassOptions = computed(() => {
  if (!filters.grade) return classOptions.value
  return classOptions.value.filter(c => c.grade === filters.grade)
})

const onGradeChange = () => { filters.classId = null; loadData() }
const onFilterChange = () => loadData()
const resetFilters = () => {
  filters.grade = ''; filters.classId = null; filters.taskId = null
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (filters.grade) params.grade = filters.grade
    if (filters.classId) params.classId = filters.classId
    if (filters.taskId) params.taskId = filters.taskId
    const res = await getScoreAnalysis(params)
    if (res.code === 200) {
      const data = res.data
      list.value = (data.classStats || []).map(c => ({
        ...c,
        title: data.taskTitle || '任务',
        taskId: data.taskId || c.taskId,
        subject: data.subject || c.subject || '-',
        teacherName: data.teacherName || '-',
        studentCount: c.studentCount || 0,
        notSubmittedCount: Math.max(0, (c.studentCount || 0) - (c.submittedCount || 0)),
        submitRate: c.studentCount > 0 ? Math.round((c.submittedCount || 0) / c.studentCount * 100) : 0,
        gradeRate: c.submittedCount > 0 ? Math.round((c.gradedCount || 0) / c.submittedCount * 100) : 0
      }))
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const openDetail = () => {} // 详情已通过行内数据展示，无需弹窗

onMounted(async () => {
  loadData()
  try {
    const [gradeRes, clsRes, taskRes] = await Promise.all([
      getGrades(), getClassList(), listTasks({ size: 999 })
    ])
    if (gradeRes.code === 200) gradeOptions.value = (gradeRes.data || []).map(g => g.gradeName)
    if (clsRes.code === 200) classOptions.value = (clsRes.data.records || []).map(c => ({
      id: c.id, className: c.className, grade: c.grade || ''
    }))
    if (taskRes.code === 200) taskOptions.value = (taskRes.data.records || []).filter(t => t.status !== 'DRAFT')
  } catch { /* */ }
})
</script>

<style scoped lang="scss">
.inspector-homework { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.empty-state { padding: 40px 0; }
@media (max-width: 768px) {
  .inspector-homework { padding: var(--spacing-md, 16px); }
  .filter-bar {
    flex-direction: column; align-items: stretch;
    :deep(.el-select) { width: 100%; }
    :deep(.el-button) { width: 100%; margin-left: 0; }
  }
}
</style>
