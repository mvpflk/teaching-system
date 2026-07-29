<template>
  <div class="feedback-management">
    <div class="page-header">
      <h3 class="page-title">用户反馈管理</h3>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="filterStatus" size="default" @change="onFilter">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="OPEN">待处理</el-radio-button>
        <el-radio-button value="RESOLVED">已解决</el-radio-button>
        <el-radio-button value="CLOSED">已关闭</el-radio-button>
      </el-radio-group>
    </div>

    <el-table
      v-loading="loading"
      :data="records"
      stripe
      size="small"
    >
      <el-table-column
        type="index"
        label="#"
        width="50"
        align="center"
      />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'BUG' ? 'danger' : row.type === 'SUGGESTION' ? 'success' : 'info'">
            {{ row.type === 'BUG' ? 'Bug' : row.type === 'SUGGESTION' ? '建议' : '其他' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="title"
        label="标题"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'OPEN' ? 'warning' : row.status === 'RESOLVED' ? 'success' : 'info'">
            {{ row.status === 'OPEN' ? '待处理' : row.status === 'RESOLVED' ? '已解决' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="pageUrl"
        label="来源页面"
        width="160"
        show-overflow-tooltip
      />
      <el-table-column label="提交时间" width="160" align="center">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="100"
        align="center"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            size="small"
            @click="openDetail(row)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        size="default"
        @current-change="loadData"
      />
    </div>

    <el-dialog
      v-model="detailVisible"
      title="反馈详情"
      width="560px"
      destroy-on-close
    >
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="类型">
            <el-tag size="small" :type="detail.type === 'BUG' ? 'danger' : detail.type === 'SUGGESTION' ? 'success' : 'info'">
              {{ detail.type === 'BUG' ? 'Bug 反馈' : detail.type === 'SUGGESTION' ? '功能建议' : '其他' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status === 'OPEN' ? '待处理' : detail.status === 'RESOLVED' ? '已解决' : '已关闭' }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="详细描述" :span="2">
            <div style="white-space: pre-wrap; max-height: 200px; overflow-y: auto;">{{ detail.content || '（无）' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="来源页面" :span="2">
            <span style="font-size: var(--fs-xs); color: var(--text-secondary)">{{ detail.pageUrl || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="浏览器">{{ detail.browserInfo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmt(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.resolvedAt" label="处理时间" :span="2">{{ fmt(detail.resolvedAt) }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 16px;">
          <p style="margin: 0 0 8px; font-weight: 600;">管理员备注</p>
          <el-input
            v-model="adminNote"
            type="textarea"
            :rows="3"
            placeholder="添加处理备注..."
          />
        </div>

        <div style="margin-top: 16px; text-align: right;">
          <el-button
            v-if="detail.status === 'OPEN'"
            type="success"
            :loading="updating"
            @click="resolveFeedback"
          >
            标记为已解决
          </el-button>
          <el-button :loading="updating" @click="closeFeedback">关闭</el-button>
          <el-button
            v-if="adminNote !== detail.adminNote"
            type="primary"
            :loading="updating"
            @click="saveNote"
          >
            保存备注
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import dayjs from 'dayjs'

const records = ref([]); const loading = ref(false); const total = ref(0)
const pageNum = ref(1); const pageSize = 20
const filterStatus = ref('')

const detail = ref(null); const detailVisible = ref(false)
const adminNote = ref(''); const updating = ref(false)

const fmt = t => t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'

const onFilter = () => { pageNum.value = 1; loadData() }

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: pageNum.value, size: pageSize }
    if (filterStatus.value) params.status = filterStatus.value
    const r = await request({ url: '/feedback', method: 'get', params })
    if (r.code === 200) { records.value = r.data.records || []; total.value = r.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const openDetail = (row) => {
  detail.value = { ...row }
  adminNote.value = row.adminNote || ''
  detailVisible.value = true
}

const saveNote = async () => {
  updating.value = true
  try {
    await request({ url: `/feedback/${detail.value.id}`, method: 'put', data: { adminNote: adminNote.value } })
    detail.value.adminNote = adminNote.value
    ElMessage.success('已保存')
  } catch { ElMessage.error('保存失败') }
  finally { updating.value = false }
}

const resolveFeedback = async () => {
  updating.value = true
  try {
    const r = await request({ url: `/feedback/${detail.value.id}`, method: 'put', data: { status: 'RESOLVED' } })
    if (r.code === 200) {
      detail.value.status = 'RESOLVED'
      ElMessage.success('已标记为已解决')
      loadData()
    }
  } catch { ElMessage.error('操作失败') }
  finally { updating.value = false }
}

const closeFeedback = async () => {
  updating.value = true
  try {
    const r = await request({ url: `/feedback/${detail.value.id}`, method: 'put', data: { status: 'CLOSED' } })
    if (r.code === 200) {
      detail.value.status = 'CLOSED'
      ElMessage.success('已关闭')
      loadData()
    }
  } catch { ElMessage.error('操作失败') }
  finally { updating.value = false }
}

onMounted(() => { loadData() })
</script>

<style scoped lang="scss">
.feedback-management { max-width: 1200px; }
.page-header { margin-bottom: 8px; .page-title { font-size: var(--fs-xl, 20px); margin: 0; } }
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }
</style>
