<template>
  <div class="inspector-records">
    <div class="page-header">
      <div>
        <h3 class="page-title">巡视记录</h3>
        <span class="header-subtitle">管理日常巡视、课堂巡视等记录</span>
      </div>
      <el-button type="primary" @click="$router.push('/inspector/records/create')">新建记录</el-button>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="filters.recordType"
        placeholder="巡视类型"
        clearable
        @change="loadData"
      >
        <el-option label="全部" value="" />
        <el-option label="日常巡视" value="CASUAL" />
        <el-option label="课堂巡视" value="CLASSROOM" />
        <el-option label="德育巡视" value="MORAL" />
        <el-option label="专项巡视" value="SPECIAL" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="onDateChange"
      />
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <template v-if="!isMobile">
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        style="cursor:pointer"
        @row-click="onRowClick"
      >
        <el-table-column prop="recordDate" label="巡视日期" width="110" />
        <el-table-column
          prop="title"
          label="标题"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ typeMap[row.recordType] || row.recordType }}</template>
        </el-table-column>
        <el-table-column label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" size="small">{{ severityMap[row.severity] || row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="$router.push(`/inspector/records/${row.id}/edit`)">编辑</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              @click.stop="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 移动端卡片 -->
    <div v-else-if="isMobile" class="mobile-list">
      <div v-for="row in list" :key="row.id" class="mobile-card" @click="onRowClick(row)">
        <div class="mc-header">
          <span class="mc-title">{{ row.title }}</span>
          <el-tag :type="statusTag(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
        </div>
        <div class="mc-body">
          <div class="mc-meta"><span class="mc-lbl">日期</span><span>{{ row.recordDate }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">类型</span><span>{{ typeMap[row.recordType] || row.recordType }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">严重程度</span><el-tag :type="severityTag(row.severity)" size="small">{{ severityMap[row.severity] || row.severity }}</el-tag></div>
        </div>
        <div class="mc-actions">
          <el-button size="small" @click.stop="$router.push(`/inspector/records/${row.id}/edit`)">编辑</el-button>
          <el-button size="small" type="danger" plain @click.stop="handleDelete(row)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无巡视记录" :image-size="60" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRecords, deleteRecord } from '@/api/inspectorManage'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ recordType: '' })
const dateRange = ref(null)

const typeMap = { CASUAL: '日常巡视', CLASSROOM: '课堂巡视', MORAL: '德育巡视', SPECIAL: '专项巡视' }
const severityMap = { INFO: '信息', WARNING: '警告', CRITICAL: '严重' }
const severityTag = (s) => ({ INFO: 'info', WARNING: 'warning', CRITICAL: 'danger' })[s] || 'info'
const statusMap = { DRAFT: '草稿', SUBMITTED: '已提交', ARCHIVED: '已归档' }
const statusTag = (s) => ({ DRAFT: 'info', SUBMITTED: 'primary', ARCHIVED: 'success' })[s] || 'info'

const onDateChange = (val) => {
  if (val) { filters.startDate = val[0]; filters.endDate = val[1] }
  else { filters.startDate = undefined; filters.endDate = undefined }
}

const onRowClick = (row) => { if (row.status === 'DRAFT') window.location.href = `/inspector/records/${row.id}/edit` }

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除此巡视记录？', '确认')
    const res = await deleteRecord(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadData() }
    else ElMessage.error(res.message)
  } catch {}
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRecords({ ...filters, page: page.value, size: size.value })
    if (res.code === 200) { list.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const resetFilters = () => { filters.recordType = ''; dateRange.value = null; filters.startDate = undefined; filters.endDate = undefined; page.value = 1; loadData() }

onMounted(loadData)
</script>

<style scoped lang="scss">
.inspector-records { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }
.empty-state { padding: 40px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
@media (max-width: 768px) {
  .inspector-records { padding: var(--spacing-md, 16px); }
  .page-header { flex-direction: column; align-items: stretch; gap: 8px; }
  .filter-bar { flex-direction: column; align-items: stretch; :deep(.el-select), :deep(.el-date-editor) { width: 100%; } }
}

/* 移动端卡片 */
.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card {
  padding: 14px; background: var(--bg-card); border-radius: var(--radius-md);
  border: 1px solid var(--border-light); cursor: pointer;
}
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mc-body { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 56px; }
.mc-actions { display: flex; gap: 8px; }
</style>
