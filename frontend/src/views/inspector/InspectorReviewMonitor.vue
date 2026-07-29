<template>
  <div class="inspector-review-monitor">
    <el-card>
      <template #header><span>审核流水</span></template>

      <el-form :model="filters" inline class="filter-bar">
        <el-form-item label="审核状态">
          <el-select
            v-model="filters.reviewStatus"
            placeholder="全部状态"
            clearable
            style="width:180px"
          >
            <el-option label="待备课组长审核" value="PENDING_GROUP" />
            <el-option label="待教研组长审核" value="PENDING_TEACHING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始">
          <el-date-picker
            v-model="filters.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
            style="width:140px"
          />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker
            v-model="filters.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
            style="width:140px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <template v-if="!isMobile">
        <el-table v-loading="loading" :data="list" stripe>
          <el-table-column prop="title" label="任务名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="taskType" label="类型" width="120" />
          <el-table-column prop="submitterName" label="提交人" width="120" />
          <el-table-column label="审核状态" width="150">
            <template #default="{ row }"><el-tag :type="statusType(row.reviewStatus)" size="small">{{ row.reviewStatusLabel }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="submittedAt" label="提交时间" width="170" />
          <el-table-column prop="updatedAt" label="最后更新" width="170" />
          <el-table-column label="审核耗时" width="100">
            <template #default="{ row }"><template v-if="row.reviewHours != null">{{ row.reviewHours }}h</template><template v-else>-</template></template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next,total" @current-change="loadData" />
        </div>
      </template>
      <!-- 移动端卡片 -->
      <div v-else class="mobile-list">
        <div v-for="row in list" :key="row.id" class="mobile-card">
          <div class="mc-header"><span class="mc-title">{{ row.title }}</span><el-tag :type="statusType(row.reviewStatus)" size="small">{{ row.reviewStatusLabel }}</el-tag></div>
          <div class="mc-body">
            <div class="mc-meta"><span class="mc-lbl">类型</span><span>{{ row.taskType || '-' }}</span></div>
            <div class="mc-meta"><span class="mc-lbl">提交人</span><span>{{ row.submitterName || '-' }}</span></div>
            <div class="mc-meta"><span class="mc-lbl">提交</span><span>{{ row.submittedAt || '-' }}</span></div>
            <div class="mc-meta"><span class="mc-lbl">耗时</span><span>{{ row.reviewHours != null ? row.reviewHours + 'h' : '-' }}</span></div>
          </div>
        </div>
        <el-empty v-if="!loading && list.length === 0" description="暂无审核记录" :image-size="60" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReviewFlow } from '@/api/inspectorManage'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const filters = ref({ reviewStatus: '', startDate: '', endDate: '' })

function resetFilters() {
  filters.value = { reviewStatus: '', startDate: '', endDate: '' }
  page.value = 1
  loadData()
}

const statusType = (s) => {
  const map = { PENDING_GROUP: 'warning', PENDING_TEACHING: 'primary', APPROVED: 'success', REJECTED: 'danger' }
  return map[s] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getReviewFlow({ ...filters.value, page: page.value, size: size.value })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }
@media (max-width: 768px) {
  .inspector-review-monitor { padding: var(--spacing-md, 16px); }
  .filter-bar { flex-direction: column; align-items: stretch; }
  :deep(.el-table) { font-size: var(--fs-xs); }
}

.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card { padding: 14px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mc-body { display: flex; flex-direction: column; gap: 4px; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 56px; }
</style>
