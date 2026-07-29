<template>
  <div class="inspector-reports">
    <div class="page-header">
      <div>
        <h3 class="page-title">巡视报告</h3>
        <span class="header-subtitle">查看和生成巡视统计数据报告</span>
      </div>
      <el-button type="primary" @click="showGenerateDialog">生成报告</el-button>
    </div>

    <template v-if="!isMobile">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="报告类型" width="120">
          <template #default="{ row }">{{ typeMap[row.reportType] || row.reportType }}</template>
        </el-table-column>
        <el-table-column label="统计周期" width="220">
          <template #default="{ row }">{{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}</template>
        </el-table-column>
        <el-table-column prop="issueCount" label="问题数" width="80" align="center" />
        <el-table-column prop="resolvedCount" label="已解决" width="80" align="center" />
        <el-table-column prop="noticeCount" label="通知书数" width="80" align="center" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="generatedAt" label="生成时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showViewDialog(row)">查看</el-button>
            <el-button v-if="row.status === 'GENERATED'" size="small" type="success" plain @click="handlePublish(row)">发布</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 移动端卡片 -->
    <div v-else class="mobile-list">
      <div v-for="row in list" :key="row.id" class="mobile-card">
        <div class="mc-header">
          <span class="mc-title">{{ row.title }}</span>
          <el-tag :type="statusTag(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
        </div>
        <div class="mc-body">
          <div class="mc-meta"><span class="mc-lbl">类型</span><span>{{ typeMap[row.reportType] || row.reportType }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">周期</span><span>{{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">数据</span><span>问题 {{ row.issueCount || 0 }} · 已解决 {{ row.resolvedCount || 0 }} · 通知书 {{ row.noticeCount || 0 }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">生成</span><span>{{ row.generatedAt || '-' }}</span></div>
        </div>
        <div class="mc-actions">
          <el-button size="small" @click="showViewDialog(row)">查看</el-button>
          <el-button v-if="row.status === 'GENERATED'" size="small" type="success" plain @click="handlePublish(row)">发布</el-button>
          <el-button v-if="row.status !== 'PUBLISHED'" size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无报告" :image-size="60" />
    </div>

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="loadData"
      />
    </div>

    <el-dialog
      v-model="generateVisible"
      title="生成巡视报告"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="报告类型">
          <el-select v-model="generateForm.reportType" placeholder="选择类型" style="width:100%">
            <el-option label="周报" value="WEEKLY" />
            <el-option label="月报" value="MONTHLY" />
            <el-option label="学期报" value="SEMESTER" />
            <el-option label="专项报告" value="AD_HOC" />
          </el-select>
        </el-form-item>
        <el-form-item label="起始日期">
          <el-date-picker
            v-model="generateForm.periodStart"
            type="date"
            placeholder="选择起始日期"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="generateForm.periodEnd"
            type="date"
            placeholder="选择截止日期"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">确认生成</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="viewVisible"
      :title="viewReport.title || '报告详情'"
      width="700px"
      top="5vh"
    >
      <div v-if="viewReport.content" class="report-content" v-html="sanitizeHtml(viewReport.content)" />
      <pre v-else class="report-json">{{ formatJson(viewReport.summaryJson) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReports, generateReport, getReport, publishReport, deleteReport } from '@/api/inspectorManage'
import { useUserStore } from '@/stores/user'
import { sanitizeHtml } from '@/utils/markdown'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

const generateVisible = ref(false)
const generateLoading = ref(false)
const generateForm = reactive({ reportType: 'WEEKLY', periodStart: '', periodEnd: '' })

const viewVisible = ref(false)
const viewReport = ref({})

const typeMap = { WEEKLY: '周报', MONTHLY: '月报', SEMESTER: '学期报', AD_HOC: '专项报告' }
const statusMap = { DRAFT: '草稿', GENERATED: '已生成', PUBLISHED: '已发布' }
const statusTag = (s) => ({ DRAFT: 'info', GENERATED: 'primary', PUBLISHED: 'success' })[s] || 'info'

const formatJson = (json) => {
  if (!json) return '暂无数据'
  try {
    const obj = typeof json === 'string' ? JSON.parse(json) : json
    return JSON.stringify(obj, null, 2)
  } catch { return typeof json === 'string' ? json : JSON.stringify(json, null, 2) }
}

const showGenerateDialog = () => {
  generateForm.reportType = 'WEEKLY'
  generateForm.periodStart = ''
  generateForm.periodEnd = ''
  generateVisible.value = true
}

const handleGenerate = async () => {
  if (!generateForm.periodStart || !generateForm.periodEnd) { ElMessage.warning('请选择统计周期'); return }
  generateLoading.value = true
  try {
    const res = await generateReport({ ...generateForm, userId: userStore.userInfo?.id })
    if (res.code === 200) { ElMessage.success('生成成功'); generateVisible.value = false; loadData() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('生成失败') }
  finally { generateLoading.value = false }
}

const showViewDialog = async (row) => {
  try {
    const res = await getReport(row.id)
    if (res.code === 200) { viewReport.value = res.data; viewVisible.value = true }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('加载失败') }
}

const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm('确定发布此报告？', '确认')
    const res = await publishReport(row.id)
    if (res.code === 200) { ElMessage.success('发布成功'); loadData() }
    else ElMessage.error(res.message)
  } catch {}
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此报告？', '确认')
    const res = await deleteReport(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
    else ElMessage.error(res.message)
  } catch {}
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getReports({ page: page.value, size: size.value })
    if (res.code === 200) { list.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.inspector-reports { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.empty-state { padding: 40px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
.report-content { white-space: pre-wrap; line-height: 1.6; font-size: var(--fs-md); }
.report-json { background: var(--bg-secondary); padding: 16px; border-radius: var(--radius-md); font-size: var(--fs-sm); overflow: auto; max-height: 60vh; }
@media (max-width: 768px) { .inspector-reports { padding: var(--spacing-md, 16px); } .page-header { flex-direction: column; align-items: stretch; gap: 8px; } }

/* 移动端卡片 */
.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card { padding: 14px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mc-body { display: flex; flex-direction: column; gap: 4px; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 56px; }
.mc-actions { display: flex; gap: 8px; flex-wrap: wrap; }
</style>
