<template>
  <div class="dia-hub">
    <div class="dia-header">
      <h2><el-icon><DataAnalysis /></el-icon> AI 批改诊断</h2>
      <p class="dia-sub">选择已完成的任务，AI 自动分析学生答题情况，生成诊断报告</p>
    </div>

    <el-card shadow="never">
      <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-input
          v-model="keyword"
          placeholder="搜索任务标题"
          clearable
          style="width:240px"
          @clear="loadTasks"
          @keyup.enter="loadTasks"
        />
        <el-select
          v-model="filterSubject"
          placeholder="学科筛选"
          clearable
          style="width:160px"
          @change="loadTasks"
        >
          <el-option
            v-for="s in availableSubjects"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
        <el-button type="primary" @click="loadTasks">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="tasks" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="任务标题" min-width="180" />
        <el-table-column label="学科" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.subject || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交人数" width="100">
          <template #default="{ row }">
            <span>{{ row.submittedCount || 0 }} / {{ row.totalStudents || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="平均分" width="80">
          <template #default="{ row }">
            <span :style="{ color: row.avgScore >= 80 ? 'var(--el-color-success, #67c23a)' : row.avgScore >= 60 ? 'var(--el-color-warning, #e6a23c)' : 'var(--el-color-danger, #f56c6c)' }">
              {{ row.avgScore != null && row.avgScore > 0 ? row.avgScore : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="诊断状态" width="120">
          <template #default="{ row }">
            <el-tag
              v-if="row.hasDiagnosis"
              type="success"
              size="small"
              effect="plain"
            >
              已有报告
            </el-tag>
            <el-tag
              v-else
              type="info"
              size="small"
              effect="plain"
            >
              未诊断
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              size="small"
              @click="openDiagnosis(row)"
            >
              <el-icon><DataAnalysis /></el-icon> 诊断分析
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !tasks.length" style="text-align:center;padding:40px;color:#999">
        <el-empty description="暂无已完成的任务" />
      </div>

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        style="margin-top:16px;justify-content:center"
        @current-change="loadTasks"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis } from '@element-plus/icons-vue'
import { listTasks } from '@/api/task'
import { getMySubjects } from '@/api/settings'

const router = useRouter()
const tasks = ref([])
const loading = ref(false)
const keyword = ref('')
const filterSubject = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 从系统字典/教师任教学科动态加载（不硬编码）
const availableSubjects = ref([])
async function loadAvailableSubjects() {
  try {
    const res = await getMySubjects()
    if (res.code === 200 && res.data) {
      availableSubjects.value = (res.data || []).map(s => s.subjectName).filter(Boolean)
    }
  } catch { /* 加载失败不影响页面使用 */ }
}
loadAvailableSubjects()

async function loadTasks() {
  loading.value = true
  try {
    // 不传 status 参数，让后端返回所有非草稿状态的任务（PUBLISHED + CLOSED 等）
    // 实际有提交数据的任务才能做诊断
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      subject: filterSubject.value || undefined
    }
    const res = await listTasks(params)
    if (res.code === 200) {
      tasks.value = (res.data?.records || res.data || [])
      total.value = res.data?.total || tasks.value.length
      // TODO: 批量查询哪些任务已有诊断报告（后续优化）
    }
  } catch { tasks.value = [] }
  finally { loading.value = false }
}

function openDiagnosis(row) {
  router.push({
    path: `/teacher/ai/diagnosis/${row.id}`,
    query: { title: row.title || '', subject: row.subject || '' }
  })
}

onMounted(() => loadTasks())
</script>

<style scoped>
.dia-hub { max-width: 960px; margin: 0 auto; padding: 24px; }
.dia-header { margin-bottom: 24px; }
.dia-header h2 { font-size: var(--fs-xl); display: flex; align-items: center; gap: 8px; margin: 0 0 6px; color: #303133; }
.dia-sub { font-size: var(--fs-sm); color: var(--text-secondary); margin: 0; }

@media (max-width: 768px) {
  .dia-hub { padding: 12px; }
  .dia-header h2 { font-size: var(--fs-lg); }
}
</style>
