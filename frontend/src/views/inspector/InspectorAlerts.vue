<template>
  <div class="inspector-alerts">
    <div class="page-header">
      <h3 class="page-title">预警中心</h3>
      <span class="header-subtitle">教学异常监控与预警管理</span>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="预警日志" name="logs">
        <div class="filter-bar">
          <el-radio-group v-model="logFilter" @change="loadLogs">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button :value="0">未读</el-radio-button>
            <el-radio-button :value="1">已读</el-radio-button>
          </el-radio-group>
          <el-button @click="loadLogs"><el-icon><Refresh /></el-icon>刷新</el-button>
          <el-button type="warning" @click="handleMarkAllRead">全部已读</el-button>
        </div>

        <template v-if="!isMobile">
          <el-table v-loading="logLoading" :data="logList" stripe style="cursor:pointer" @row-click="handleRowClick">
            <el-table-column prop="triggedAt" label="触发时间" width="160" />
            <el-table-column prop="ruleName" label="规则名称" width="120" />
            <el-table-column prop="alertMessage" label="预警消息" min-width="300" show-overflow-tooltip />
            <el-table-column prop="targetClassId" label="关联班级" width="100">
              <template #default="{ row }">{{ row.targetClassId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="targetTeacherId" label="关联教师" width="100">
              <template #default="{ row }">{{ row.targetTeacherId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="metricValue" label="指标值" width="80" align="center" />
            <el-table-column prop="threshold" label="阈值" width="80" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isRead" size="small" type="info">已读</el-tag>
                <el-tag v-else size="small" type="danger">未读</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!logLoading && logList.length === 0" class="empty-state"><el-empty description="暂无预警日志" :image-size="80" /></div>
          <div v-if="logTotal > logPageSize" class="pagination-wrap">
            <el-pagination v-model:current-page="logPage" v-model:page-size="logPageSize" :total="logTotal" layout="prev, pager, next, total" @current-change="loadLogs" />
          </div>
        </template>
        <template v-else>
          <div class="mobile-list">
            <div v-for="row in logList" :key="row.id" class="mobile-card" @click="handleRowClick(row)">
              <div class="mc-header">
                <span class="mc-title">{{ row.ruleName }}</span>
                <el-tag v-if="row.isRead" size="small" type="info">已读</el-tag>
                <el-tag v-else size="small" type="danger">未读</el-tag>
              </div>
              <div class="mc-body">
                <div class="mc-msg">{{ row.alertMessage }}</div>
                <div class="mc-meta"><span class="mc-lbl">时间</span><span>{{ row.triggedAt }}</span></div>
                <div class="mc-meta"><span class="mc-lbl">班级</span><span>{{ row.targetClassId || '-' }}</span></div>
                <div class="mc-meta"><span class="mc-lbl">教师</span><span>{{ row.targetTeacherId || '-' }}</span></div>
              </div>
            </div>
            <el-empty v-if="!logLoading && logList.length === 0" description="暂无预警日志" :image-size="60" />
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane label="预警规则" name="rules">
        <el-table v-loading="ruleLoading" :data="ruleList" stripe>
          <el-table-column prop="ruleName" label="规则名称" width="140" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ ruleTypeMap[row.ruleType] || row.ruleType }}</template>
          </el-table-column>
          <el-table-column label="监测对象" width="100">
            <template #default="{ row }">{{ row.targetType === 'CLASS' ? '班级' : row.targetType === 'TEACHER' ? '教师' : row.targetType }}</template>
          </el-table-column>
          <el-table-column label="阈值" width="100">
            <template #default="{ row }">
              <el-input-number
                v-model="row.threshold"
                :min="0"
                :max="99999"
                size="small"
                controls-position="right"
                style="width:100px"
                @change="val => handleRuleChange(row, 'threshold', val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="比较方式" width="100">
            <template #default="{ row }">{{ row.comparison === 'LT' ? '低于' : row.comparison === 'GT' ? '高于' : row.comparison }}</template>
          </el-table-column>
          <el-table-column label="时间窗口" width="120">
            <template #default="{ row }">{{ timeWindowMap[row.timeWindow] || row.timeWindow }}</template>
          </el-table-column>
          <el-table-column label="启用" width="80" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.enabled"
                :active-value="1"
                :inactive-value="0"
                @change="val => handleRuleChange(row, 'enabled', val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="通知巡视员" width="110" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.notifyInspector"
                :active-value="1"
                :inactive-value="0"
                @change="val => handleRuleChange(row, 'notifyInspector', val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="通知教师" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.notifyTeacher"
                :active-value="1"
                :inactive-value="0"
                @change="val => handleRuleChange(row, 'notifyTeacher', val)"
              />
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!ruleLoading && ruleList.length === 0" class="empty-state"><el-empty description="暂无规则" :image-size="80" /></div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAlertRules, updateAlertRule, getAlertLogs, markAlertRead, markAllAlertsRead } from '@/api/inspector'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const activeTab = ref('logs')

const logFilter = ref('')
const logLoading = ref(false)
const logList = ref([])
const logPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)

const ruleLoading = ref(false)
const ruleList = ref([])

const ruleTypeMap = {
  SCORE_AVG_DROP: '均分骤降', SUBMIT_RATE_LOW: '提交率低', PASS_RATE_LOW: '及格率低',
  CREDIT_ANOMALY: '积分异常', PEER_STDDEV_HIGH: '离散度过高',
  TEACHER_GRADING_BACKLOG: '批改积压', TEACHER_INACTIVE: '教师不活跃'
}
const timeWindowMap = {
  CURRENT_WEEK: '本周', LAST_WEEK: '上周', CURRENT_MONTH: '本月', LAST_MONTH: '上月'
}

const debounceTimers = {}

const handleRuleChange = (row, field, val) => {
  if (debounceTimers[row.id]) clearTimeout(debounceTimers[row.id])
  debounceTimers[row.id] = setTimeout(async () => {
    try {
      const data = { [field]: val }
      const res = await updateAlertRule(row.id, data)
      if (res.code === 200) ElMessage.success('更新成功')
      else ElMessage.error(res.message)
    } catch { ElMessage.error('更新失败') }
  }, 500)
}

const handleRowClick = async (row) => {
  if (!row.isRead) {
    await markAlertRead(row.id)
    row.isRead = 1
  }
}

const handleMarkAllRead = async () => {
  try {
    const res = await markAllAlertsRead()
    if (res.code === 200) { ElMessage.success(`已标记 ${res.data || 0} 条已读`); loadLogs() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('操作失败') }
}

const loadLogs = async () => {
  logLoading.value = true
  try {
    const params = { page: logPage.value, size: logPageSize.value }
    if (logFilter.value !== '') params.isRead = logFilter.value
    const res = await getAlertLogs(params)
    if (res.code === 200) {
      logList.value = (res.data.records || []).map(r => ({ ...r, isRead: r.isRead === 1 }))
      logTotal.value = res.data.total || 0
    }
  } catch { ElMessage.error('加载失败') }
  finally { logLoading.value = false }
}

const loadRules = async () => {
  ruleLoading.value = true
  try {
    const res = await getAlertRules()
    if (res.code === 200) ruleList.value = (res.data || []).map(r => ({ ...r, isRead: r.isRead === 1 }))
  } catch { ElMessage.error('加载失败') }
  finally { ruleLoading.value = false }
}

onMounted(() => { loadLogs(); loadRules() })
</script>

<style scoped lang="scss">
.inspector-alerts { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header {
  display: flex; align-items: baseline; gap: 12px; margin-bottom: 4px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; align-items: center; }
.empty-state { padding: 40px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
.el-table :deep(.el-table__row) { transition: background 0.2s; }
@media (max-width: 768px) {
  .inspector-alerts { padding: var(--spacing-md, 16px); }
  .filter-bar { flex-direction: column; align-items: stretch; :deep(.el-radio-group) { width: 100%; justify-content: center; } }
}

.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card { padding: 14px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); cursor: pointer; }
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); }
.mc-body { display: flex; flex-direction: column; gap: 4px; }
.mc-msg { font-size: var(--fs-sm); color: var(--text-regular); margin-bottom: 4px; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 36px; }
</style>
