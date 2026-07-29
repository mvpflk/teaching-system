<template>
  <div class="alert-mgmt">
    <div class="am-header">
      <div class="am-header-left">
        <h2 class="am-title">学业预警</h2>
        <el-tag v-if="lastScanTime" size="small" type="info">上次扫描：{{ fmtFull(lastScanTime) }}</el-tag>
      </div>
      <div class="am-header-actions">
        <el-button size="small" @click="showRuleDialog = true"><el-icon><Setting /></el-icon> 规则设置</el-button>
        <el-popover placement="bottom" :width="320" trigger="click">
          <template #reference><el-button size="small"><el-icon><Bell /></el-icon> 通知说明</el-button></template>
          <div style="font-size:var(--fs-sm);line-height:1.8">
            <p style="font-weight:600;margin-bottom:8px">📢 预警通知机制</p>
            <p>• <b>班主任</b>：触发预警后系统自动发送通知</p>
            <p>• <b>家长</b>：关联家长同时收到通知</p>
            <p>• <b>通知方式</b>：站内消息（系统通知栏）</p>
            <p style="color:var(--text-secondary);margin-top:8px">通知由系统自动触发，无需手动配置。</p>
          </div>
        </el-popover>
        <el-button type="primary" size="small" :loading="scanning" @click="doScan">
          <el-icon><Refresh /></el-icon> {{ scanning ? '扫描中...' : '扫描预警' }}
        </el-button>
      </div>
    </div>

    <AlertStatsDashboard :stats />
    <AlertTrendChart :trend-data="trendData" />
    <AlertFilterBar :filters :class-options="myClasses" :selected-ids-length="selectedIds.length"
      @update:class-id="filters.classId = $event" @update:alert-type="filters.alertType = $event"
      @update:handled-status="filters.handledStatus = $event" @update:student-name="filters.studentName = $event"
      @search="onFilterChange" @batch-action="batchHandle" />
    <AlertEmptyState v-if="!loading && records.length === 0" :has-filters="hasAnyFilter" @clear-filters="resetFilters" />
    <AlertTable :records :loading :total :page="filters.page" :page-size="filters.pageSize"
      @page-change="onPageChange"       @selection-change="onSelectionChange"
      @mark-read="confirmHandle($event, 'READ')" @mark-contacted="confirmHandle($event, 'CONTACTED')"
      @ignore="confirmHandle($event, 'IGNORED')" @view-detail="showStudentDetail" />
    <StudentDetailDrawer :visible="detailVisible" :student="detailStudent" :history="detailHistory"
      @close="detailVisible = false" />
    <AlertRulesDialog :visible="showRuleDialog" :rules :adding-rule="addingRule" :new-rule="newRule"
      @close="showRuleDialog = false" @add-rule="addingRule = true" @cancel-add="addingRule = false"
      @save-rules="doSaveRules" @confirm-add="doAddRule" @delete-rule="doDeleteRule" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Bell, Refresh } from '@element-plus/icons-vue'
import { getAlertRecords, handleAlert } from '@/api/alert'
import { getMyClasses } from '@/api/classes'
import request from '@/utils/request'
import { useAlertFilters } from '@/composables/useAlertFilters'
import { useAlertRules } from '@/composables/useAlertRules'
import AlertStatsDashboard from './components/AlertStatsDashboard.vue'
import AlertTrendChart from './components/AlertTrendChart.vue'
import AlertFilterBar from './components/AlertFilterBar.vue'
import AlertTable from './components/AlertTable.vue'
import AlertEmptyState from './components/AlertEmptyState.vue'
import StudentDetailDrawer from './components/StudentDetailDrawer.vue'
import AlertRulesDialog from './components/AlertRulesDialog.vue'

const { filters, selectedIds, hasAnyFilter, resetFilters, updateFilter, clearSelection } = useAlertFilters()
const { rules, showRuleDialog, addingRule, newRule, loadRules, addRule, saveAllRules, removeRule } = useAlertRules()

const loading = ref(false); const scanning = ref(false); const records = ref([]); const total = ref(0)
const lastScanTime = ref(null); const myClasses = ref([]); const summaryData = ref({})
const detailVisible = ref(false); const detailStudent = ref(null); const detailHistory = ref([]); const trendData = ref([])

const stats = computed(() => {
  const s = summaryData.value || {}
  return { total: total.value, unread: s.unread || 0, lowScore: s.lowScore || 0, missing: s.missing || 0, contacted: s.contacted || 0 }
})

import { fmtFull } from '@/utils/date'

const loadRecords = async () => {
  loading.value = true
  try {
    const params = { page: filters.page, pageSize: filters.pageSize }
    if (filters.classId) params.classId = filters.classId
    if (filters.alertType) params.alertType = filters.alertType
    if (filters.handledStatus) params.handledStatus = filters.handledStatus
    if (filters.studentName) params.studentName = filters.studentName
    const res = await getAlertRecords(params)
    if (res.code === 200) { records.value = res.data.records || []; total.value = res.data.total || 0; summaryData.value = res.data.summary || {} }
  } finally { loading.value = false }
}

const onFilterChange = () => { filters.page = 1; loadRecords() }
const onPageChange = (val) => { filters.page = val; loadRecords() }
// AlertTable emit 的是 ID 数组（已做过 rows.map(r => r.id)），直接赋值即可
const onSelectionChange = (ids) => { selectedIds.value = ids }

const confirmHandle = async (row, status) => {
  const label = { UNREAD:'未读', READ:'已读', CONTACTED:'已联系家长', IGNORED:'已忽略' }[status] || status
  try { await ElMessageBox.confirm(`确定将该预警标记为"${label}"？`, '确认操作', { type:'info', confirmButtonText:'确定', cancelButtonText:'取消' }); await doHandle(row.id, status) } catch { /* */ }
}

const doHandle = async (id, status) => { try { await handleAlert(id, status); ElMessage.success('已更新'); loadRecords() } catch { ElMessage.error('操作失败') } }

const batchHandle = async (status) => {
  const label = { UNREAD:'未读', READ:'已读', CONTACTED:'已联系家长', IGNORED:'已忽略' }[status] || status
  try { await ElMessageBox.confirm(`确定将 ${selectedIds.value.length} 条预警标记为"${label}"？`, '批量操作', { type:'info' }); for (const id of selectedIds.value) await handleAlert(id, status); ElMessage.success(`已批量更新 ${selectedIds.value.length} 条`); clearSelection(); loadRecords() } catch { /* */ }
}

const showStudentDetail = async (row) => {
  detailStudent.value = row; detailVisible.value = true
  try { const res = await getAlertRecords({ classId:row.classId, page:1, pageSize:50 }); if (res.code === 200) detailHistory.value = (res.data.records || []).filter(r => r.studentId === row.studentId) } catch { detailHistory.value = [] }
}

const doScan = async () => {
  scanning.value = true
  try { const res = await request.post('/alert/actions/scan'); if (res.code === 200) { ElMessage.success(`扫描完成，新增 ${res.data?.alertCount || 0} 条预警`); lastScanTime.value = new Date().toISOString(); loadRecords() } }
  catch (e) { ElMessage.error(e?.response?.data?.message || '扫描失败') } finally { scanning.value = false }
}

const loadClasses = async () => { try { const res = await getMyClasses(); if (res.code === 200) myClasses.value = res.data || [] } catch { /* */ } }
const loadScanInfo = async () => { try { const res = await request.get('/alert/records', { params:{ page:1, pageSize:1 } }); if (res.code === 200 && res.data?.records?.length) lastScanTime.value = res.data.records[0].createTime } catch { /* */ } }
const loadTrend = async () => { try { const res = await request.get('/alert/trend'); if (res.code === 200 && res.data?.length) trendData.value = res.data } catch { /* */ } }

const doSaveRules = () => saveAllRules(rules.value)
const doAddRule = () => addRule(newRule)
const doDeleteRule = (rule) => removeRule(rule)

onMounted(() => { loadRecords(); loadRules(); loadClasses(); loadScanInfo(); loadTrend() })
</script>

<style scoped>
.alert-mgmt { margin: 0 auto; padding: 8px; }
.am-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.am-header-left { display: flex; align-items: center; gap: 10px; }
.am-title { margin: 0; font-size: var(--fs-xl); font-weight: 700; }
.am-header-actions { display: flex; gap: 6px; }
@media (max-width: 768px) { .am-header { flex-direction: column; align-items: flex-start; } }
</style>
