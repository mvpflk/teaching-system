<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">待审核任务</h3>
      <span class="page-subtitle">作为组长审核提交的考试任务</span>
    </div>

    <div v-if="loading" class="sk-list"><div v-for="i in 4" :key="i" class="sk-row"><div class="sk-line w-40"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div><div class="sk-line w-20"></div></div></div>
    <el-table v-else :data="list" stripe>
      <template #empty><el-empty description="暂无待审核任务" /></template>
      <el-table-column prop="title" label="任务标题" min-width="180" />
      <el-table-column label="提交人" width="100">
        <template #default="{ row }">{{ row.teacherName || '-' }}</template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">{{ row.submittedAt || row.createTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="审核状态" width="110">
        <template #default="{ row }">
          <el-tag :type="reviewTag(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="approve(row)">通过</el-button>
          <el-button size="small" type="danger" @click="showReject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="rejectVisible"
      title="拒绝原因"
      width="400px"
      destroy-on-close
      append-to-body
    >
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请填写拒绝原因（必填）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button
          type="danger"
          :disabled="!rejectReason.trim()"
          :loading="rejecting"
          @click="doReject"
        >
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingReviews, approveReview, rejectReview } from '@/api/task'

const REVIEW_LABEL = { PENDING_GROUP: '备课组长审', PENDING_TEACHING: '教研组长审', APPROVED: '已通过', REJECTED: '已拒绝' }
const REVIEW_TAG = { PENDING_GROUP: 'warning', PENDING_TEACHING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
const reviewLabel = (s) => REVIEW_LABEL[s] || s
const reviewTag = (s) => REVIEW_TAG[s] || 'info'

const loading = ref(false)
const list = ref([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)
const rejectTarget = ref(null)

const loadList = async () => {
  loading.value = true
  try {
    const res = await getPendingReviews()
    if (res.code === 200) list.value = res.data || []
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const approve = async (row) => {
  try {
    const res = await approveReview(row.id)
    if (res.code === 200) { ElMessage.success('已通过'); loadList() }
    else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('操作失败') }
}

const showReject = (row) => { rejectTarget.value = row; rejectReason.value = ''; rejectVisible.value = true }

const doReject = async () => {
  if (!rejectReason.value.trim()) return
  rejecting.value = true
  try {
    const res = await rejectReview(rejectTarget.value.id, rejectReason.value)
    if (res.code === 200) { ElMessage.success('已拒绝'); rejectVisible.value = false; loadList() }
    else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('操作失败') }
  finally { rejecting.value = false }
}

onMounted(() => { loadList() })
</script>

<style scoped>
.sk-list { padding: 8px 0; }
.sk-row { display: flex; gap: 16px; padding: 16px 12px; border-bottom: 1px solid var(--border-light); }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); position: relative; overflow: hidden; }
.sk-line::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { transform: translateX(-100%) } 100% { transform: translateX(100%) } }
.w-20 { width: 20% } .w-40 { width: 40% }

@media (max-width: 768px) {
  :deep(.el-table) { font-size: var(--fs-xs); }
}
</style>
