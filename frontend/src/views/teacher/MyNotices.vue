<template>
  <div class="my-notices">
    <div class="page-header">
      <div>
        <h3 class="page-title">我的通知书</h3>
        <span class="header-subtitle">查看和签收整改通知书</span>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column
        prop="title"
        label="标题"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column label="关联问题" width="100">
        <template #default="{ row }">
          <el-button
            v-if="row.issueId"
            size="small"
            link
            type="primary"
            @click="$router.push(`/inspector/issues/${row.issueId}`)"
          >
            查看
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="发送时间" width="160">
        <template #default="{ row }">{{ row.sentAt || row.createdAt }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'SENT'"
            size="small"
            type="primary"
            @click="handleAcknowledge(row)"
          >
            签收
          </el-button>
          <el-button
            v-if="row.status === 'ACKNOWLEDGED'"
            size="small"
            type="success"
            @click="handleComply(row)"
          >
            完成
          </el-button>
          <span v-if="row.status === 'COMPLIED'" style="color: var(--text-secondary); font-size: var(--fs-sm);">已完成</span>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && list.length === 0" class="empty-state"><el-empty description="暂无通知书" :image-size="80" /></div>

    <div v-if="total > size" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyNotices, acknowledgeNotice, complyNotice } from '@/api/inspectorTeacher'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

const statusMap = { SENT: '已发送', ACKNOWLEDGED: '已签收', COMPLIED: '已完成' }
const statusTag = (s) => ({ SENT: 'primary', ACKNOWLEDGED: 'success', COMPLIED: 'info' })[s] || 'info'

const handleAcknowledge = async (row) => {
  try {
    const res = await acknowledgeNotice(row.id)
    if (res.code === 200) { ElMessage.success('签收成功'); loadData() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('操作失败') }
}

const handleComply = async (row) => {
  try {
    const res = await complyNotice(row.id)
    if (res.code === 200) { ElMessage.success('已完成'); loadData() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('操作失败') }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyNotices({ page: page.value, size: size.value })
    if (res.code === 200) { list.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.my-notices { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { margin-bottom: 16px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.empty-state { padding: 40px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
@media (max-width: 768px) { .my-notices { padding: var(--spacing-md, 16px); } }
</style>
