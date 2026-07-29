<template>
  <div class="monitor-page">
    <div class="page-header">
      <h2>🖥️ 打字竞赛驾驶舱</h2>
      <el-button :loading="loading" @click="refreshAll">刷新</el-button>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="sk-list">
      <div v-for="i in 4" :key="i" class="sk-card-comp">
        <div class="sk-line w-40" style="height:16px;margin-bottom:6px"></div>
        <div class="sk-line w-60" style="height:12px"></div>
      </div>
    </div>

    <template v-else>
      <!-- 竞赛列表 -->
      <div v-if="!selectedComp" class="comp-list">
        <h3>进行中的竞赛</h3>
        <el-empty v-if="ongoingComps.length === 0" description="暂无进行中的竞赛" />
        <div
          v-for="c in ongoingComps"
          :key="c.id"
          class="comp-card"
          @click="selectComp(c)"
        >
          <div class="comp-title">{{ c.title }}</div>
          <div class="comp-meta">文本: {{ c.textTitle }} | 创建: {{ c.createdAt }}</div>
          <el-tag type="warning" size="small">进行中</el-tag>
        </div>
        <h3 style="margin-top: 24px;">历史竞赛</h3>
        <el-empty v-if="historyComps.length === 0" description="暂无历史竞赛" />
        <div
          v-for="c in historyComps"
          :key="c.id"
          class="comp-card"
          @click="selectComp(c)"
        >
          <div class="comp-title">{{ c.title }}</div>
          <div class="comp-meta">{{ c.textTitle }} | 结束: {{ c.endTime }}</div>
          <el-tag type="info" size="small">已结束</el-tag>
          <el-button size="small" style="margin-left:8px" @click.stop="exportComp(c.id)">导出Excel</el-button>
        </div>
      </div>

      <!-- 驾驶舱详情 -->
      <div v-else class="dashboard">
        <el-button text @click="selectedComp = null">← 返回列表</el-button>
        <div class="dash-header">
          <div>
            <h3>{{ selectedComp.title }}</h3>
            <p class="dash-meta">文本: {{ selectedComp.textTitle }} | 总字符: {{ dashboard.totalChars }}</p>
          </div>
          <div class="dash-actions">
            <el-button type="primary" @click="exportComp(selectedComp.id)">导出成绩</el-button>
            <el-button v-if="selectedComp.status === 'ongoing'" type="danger" @click="handleFinish">结束竞赛</el-button>
          </div>
        </div>

        <el-table
          :data="dashboard.students"
          stripe
          border
          style="width:100%"
          max-height="500"
        >
          <el-table-column prop="name" label="学生" width="120" />
          <el-table-column label="进度" min-width="140">
            <template #default="{row}">
              <el-progress :percentage="row.totalChars ? Math.round(row.typedChars/row.totalChars*100) : 0" :stroke-width="10" />
            </template>
          </el-table-column>
          <el-table-column
            prop="speedWpm"
            label="速度(字/分)"
            width="100"
            sortable
          />
          <el-table-column label="正确率" width="100" sortable>
            <template #default="{row}">{{ row.accuracy }}%</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{row}">
              <el-tag :type="row.finished ? 'success' : 'warning'" size="small">{{ row.finished ? '已完成' : '进行中' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{row}">
              <el-button
                v-if="row.finished"
                text
                size="small"
                type="primary"
                @click="router.push(`/teacher/typing/competitions/${selectedComp.id}/replay/${row.studentId}`)"
              >
                回放
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCompetitions, getDashboard, finishCompetition, exportResults } from '@/api/typing'
import { createEventSourceWithReconnect } from '@/utils/sseTicket'
import { exportCsv, TYPING_RESULT_COLUMNS } from '@/utils/export'

const router = useRouter()
const loading = ref(false)
const selectedComp = ref(null)
const ongoingComps = ref([])
const historyComps = ref([])
const dashboard = ref({ students: [], totalChars: 0 })

async function refreshAll() {
  loading.value = true
  try {
    const res = await getCompetitions({ page: 1, size: 50 })
    if (res.code === 200) {
      const all = res.data.records || []
      ongoingComps.value = all.filter(c => c.status === 'ongoing')
      historyComps.value = all.filter(c => c.status === 'finished')
    }
  } catch { ElMessage.error('加载失败') }
  loading.value = false
}

let dashboardSSE = null
let sseClosed = false

async function connectDashboardSSE(id) {
  if (sseClosed) return
  const token = localStorage.getItem('token')
  if (!token) return
  disconnectDashboardSSE()
  sseClosed = false
  dashboardSSE = await createEventSourceWithReconnect(
    `/api/typing/competitions/${id}/subscribe`,
    {
      dashboard: (e) => { if (!sseClosed) try { dashboard.value = JSON.parse(e.data) } catch {} }
    }
  )
}

function disconnectDashboardSSE() {
  sseClosed = true
  if (dashboardSSE) { dashboardSSE.close(); dashboardSSE = null }
}

async function selectComp(c) {
  disconnectDashboardSSE()
  sseClosed = false
  selectedComp.value = c
  await fetchDashboard(c.id)
  if (c.status === 'ongoing') {
    try { await connectDashboardSSE(c.id) } catch { ElMessage.error('SSE 连接失败') }
  }
}

async function fetchDashboard(id) {
  try {
    const res = await getDashboard(id)
    if (res.code === 200) dashboard.value = res.data
  } catch {}
}

async function handleFinish() {
  await ElMessageBox.confirm('确定要结束该竞赛吗？所有学生将无法继续提交。', '确认', { type: 'warning' })
  try {
    await finishCompetition(selectedComp.value.id)
    ElMessage.success('竞赛已结束')
    disconnectDashboardSSE()
    await refreshAll()
    selectedComp.value = null
  } catch { /* canceled */ }
}

async function exportComp(id) {
  try {
    const res = await exportResults(id)
    if (res.code === 200 && res.data) {
      exportCsv(res.data, `typing_results_${id}.csv`, TYPING_RESULT_COLUMNS)
      ElMessage.success('已导出')
    }
  } catch { ElMessage.error('导出失败') }
}

onMounted(() => refreshAll())
onUnmounted(() => disconnectDashboardSSE())
</script>

<style scoped>
.monitor-page { max-width: 1100px; margin: 0 auto; padding: 16px; }

.sk-list { display: flex; flex-direction: column; gap: 12px; padding: 8px 0; }
.sk-card-comp { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 14px 16px; }
.sk-line { height: 14px; border-radius: 4px; background: linear-gradient(90deg, var(--bg-section, #f5f7fa) 25%, var(--bg-card, #fff) 50%, var(--bg-section, #f5f7fa) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.w-40 { width: 40% } .w-60 { width: 60% }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.comp-list { display: flex; flex-direction: column; gap: 12px; }
.comp-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 12px 16px; cursor: pointer; display: flex; align-items: center; gap: 12px; transition: box-shadow var(--transition-fast); }
.comp-card:hover { box-shadow: var(--shadow-base); }
.comp-title { font-weight: 600; color: var(--text-primary); flex: 1; }
.comp-meta { font-size: var(--fs-xs); color: var(--text-secondary); }
.dashboard { display: flex; flex-direction: column; gap: 16px; }
.dash-header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px; }
.dash-header h3 { margin: 0; }
.dash-meta { font-size: var(--fs-sm); color: var(--text-secondary); margin: 4px 0 0; }
.dash-actions { display: flex; gap: 8px; }

@media (max-width: 768px) {
  .monitor-page { padding: 8px; }
  .comp-card { flex-wrap: wrap; }
  .dash-header { flex-direction: column; }
}
</style>
