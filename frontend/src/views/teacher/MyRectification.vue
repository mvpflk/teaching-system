<template>
  <div class="my-rectification">
    <div class="page-header">
      <div>
        <h3 class="page-title">我的整改任务</h3>
        <span class="header-subtitle">查看和处理分配给您的整改问题</span>
      </div>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="filters.status"
        placeholder="状态筛选"
        clearable
        @change="loadData"
      >
        <el-option label="全部" value="" />
        <el-option label="已指派" value="ASSIGNED" />
        <el-option label="处理中" value="IN_PROGRESS" />
        <el-option label="已解决" value="RESOLVED" />
        <el-option label="已验收" value="VERIFIED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column
        prop="title"
        label="问题标题"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
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
      <el-table-column label="截止日期" width="120">
        <template #default="{ row }">
          <span :class="{ expired: row.deadline && isExpired(row.deadline) }">{{ row.deadline || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ASSIGNED'"
            size="small"
            type="primary"
            @click="handleStart(row)"
          >
            开始处理
          </el-button>
          <span v-if="row.status === 'RESOLVED'" style="color: var(--text-secondary); font-size: var(--fs-sm);">等待验收</span>
          <span v-if="row.status === 'VERIFIED'" style="color: var(--success-color); font-size: var(--fs-sm);">已验收</span>
          <template v-if="row.status === 'IN_PROGRESS' || row.status === 'ASSIGNED'">
            <el-button size="small" type="success" @click="showResolveDialog(row)">提交整改</el-button>
          </template>
          <template v-if="row.status === 'REJECTED'">
            <span style="color: var(--danger-color); font-size: var(--fs-sm); margin-right: 8px;">已驳回</span>
            <el-button size="small" type="warning" @click="showResolveDialog(row)">重新提交</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && list.length === 0" class="empty-state"><el-empty description="暂无整改任务" :image-size="80" /></div>

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
      v-model="resolveVisible"
      title="提交整改说明"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="解决说明">
          <el-input
            v-model="resolveForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请描述整改措施和处理结果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolveLoading" @click="handleResolve">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyIssues, startIssueProgress, resolveIssue } from '@/api/inspectorTeacher'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ status: '' })

const resolveVisible = ref(false)
const resolveLoading = ref(false)
const resolveForm = reactive({ issueId: null, comment: '' })

const categoryMap = { TEACHING_QUALITY: '教学质量', CLASSROOM_DISCIPLINE: '课堂纪律', HOMEWORK_PROCRASTINATION: '作业拖拉', ATTENDANCE: '出勤', MORAL_EDUCATION: '德育', EXAM_IRREGULARITY: '考试违纪', OTHER: '其他' }
const severityMap = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' }
const severityTag = (s) => ({ LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' })[s] || 'info'
const statusMap = { OPEN: '待处理', ASSIGNED: '已指派', IN_PROGRESS: '处理中', RESOLVED: '已解决', VERIFIED: '已验收', REJECTED: '已驳回' }
const statusTag = (s) => ({ OPEN: 'info', ASSIGNED: 'primary', IN_PROGRESS: 'warning', RESOLVED: '', VERIFIED: 'success', REJECTED: 'danger' })[s] || 'info'

const isExpired = (deadline) => new Date(deadline) < new Date()

const handleStart = async (row) => {
  try {
    const res = await startIssueProgress(row.id)
    if (res.code === 200) { ElMessage.success('已开始处理'); loadData() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('操作失败') }
}

const showResolveDialog = (row) => {
  resolveForm.issueId = row.id
  resolveForm.comment = ''
  resolveVisible.value = true
}

const handleResolve = async () => {
  if (!resolveForm.comment.trim()) { ElMessage.warning('请填写解决说明'); return }
  resolveLoading.value = true
  try {
    const res = await resolveIssue(resolveForm.issueId, resolveForm.comment)
    if (res.code === 200) { ElMessage.success('提交成功'); resolveVisible.value = false; loadData() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('提交失败') }
  finally { resolveLoading.value = false }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.status) params.status = filters.status
    const res = await getMyIssues(params)
    if (res.code === 200) { list.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const resetFilters = () => { filters.status = ''; page.value = 1; loadData() }

onMounted(loadData)
</script>

<style scoped lang="scss">
.my-rectification { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { margin-bottom: 16px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.filter-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.empty-state { padding: 40px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
.expired { color: var(--danger-color); font-weight: 600; }
@media (max-width: 768px) { .my-rectification { padding: var(--spacing-md, 16px); } .filter-bar { flex-direction: column; align-items: stretch; } }
</style>
