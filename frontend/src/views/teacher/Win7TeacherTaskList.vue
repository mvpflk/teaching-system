<template>
  <div class="sim-list">
    <div class="sim-list__header">
      <h3>仿真任务管理</h3>
      <div class="sim-list__actions">
        <el-input
          v-model="keyword"
          placeholder="搜索任务标题"
          clearable
          size="default"
          style="width:180px"
          @clear="applyFilter"
          @keyup.enter="applyFilter"
        />
        <el-select
          v-model="filterCategory"
          placeholder="类别"
          clearable
          size="default"
          style="width:140px"
          @change="applyFilter"
        >
          <el-option label="全部" value="" />
          <el-option label="Windows 操作" value="win7" />
          <el-option label="网络应用基础" value="network" />
        </el-select>
        <el-button type="primary" @click="$router.push('/teacher/simulation/tasks/create')">+ 创建任务</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="pagedTasks"
        style="width:100%"
        stripe
      >
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="任务名称" min-width="160" />
        <el-table-column label="类别" width="120">
          <template #default="{ row }">
            <el-tag :type="row.category==='network'?'success':'primary'" size="small" effect="plain">
              {{ row.category==='network'?'网络应用基础':'Windows 操作' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模式" width="70">
          <template #default="{ row }">
            <el-tag :type="row.mode==='exam'?'danger':''" size="small" effect="plain">{{ row.mode==='exam'?'考试':'练习' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="60" />
        <el-table-column prop="timeLimit" label="时限(秒)" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              size="small"
              @click="previewTask(row)"
            >
              预览
            </el-button>
            <el-button
              text
              type="warning"
              size="small"
              @click="editTask(row)"
            >
              编辑
            </el-button>
            <el-button
              text
              type="danger"
              size="small"
              @click="deleteTask(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filteredTasks.length" description="暂无仿真任务">
        <el-button type="primary" @click="$router.push('/teacher/simulation/tasks/create')">创建第一个任务</el-button>
      </el-empty>

      <el-pagination
        v-if="filteredTasks.length > pageSize"
        v-model:current-page="page"
        :page-size="pageSize"
        :total="filteredTasks.length"
        layout="total, prev, pager, next"
        style="margin-top:16px;justify-content:center"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSimTasks, deleteSimTask } from '@/api/simulation'

const router = useRouter()
const tasks = ref([])
const loading = ref(false)
const filterCategory = ref('')
const keyword = ref('')
const page = ref(1)
const pageSize = ref(20)

const filteredTasks = computed(() => {
  let list = tasks.value
  if (filterCategory.value) list = list.filter(t => t.category === filterCategory.value)
  if (keyword.value) list = list.filter(t => (t.title || '').includes(keyword.value))
  return list
})

const pagedTasks = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredTasks.value.slice(start, start + pageSize.value)
})

function applyFilter() { page.value = 1 }

async function loadTasks() {
  loading.value = true
  try {
    const res = await listSimTasks(filterCategory.value || null)
    if (res.code === 200) tasks.value = res.data || []
  } catch { tasks.value = [] }
  finally { loading.value = false }
}

function previewTask(row) {
  const cat = row.category || 'win7'
  router.push(`/student/training/${cat}/practice/${row.id}`)
}

function editTask(row) {
  router.push(`/teacher/simulation/tasks/edit/${row.id}`)
}

async function deleteTask(row) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${row.title}」？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }
  try {
    const res = await deleteSimTask(row.id)
    if (res.code === 200) { ElMessage.success('已删除'); loadTasks() }
    else ElMessage.error(res.message || '删除失败')
  } catch (e) { ElMessage.error('删除失败: ' + (e.message || e)) }
}

loadTasks()
</script>

<style scoped>
.sim-list { max-width: 1100px; margin: 0 auto; padding: 24px 16px; }
.sim-list__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.sim-list__header h3 { margin: 0; font-size: var(--fs-lg); color: var(--text-primary); }
.sim-list__actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

@media (max-width: 768px) {
  .sim-list { padding: 12px 8px; }
  .sim-list__header { flex-direction: column; align-items: flex-start; }
  .sim-list__actions { width: 100%; }
}
</style>
