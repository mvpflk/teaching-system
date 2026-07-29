<template>
  <div class="inspector-notices">
    <div class="page-header">
      <div>
        <h3 class="page-title">整改通知书</h3>
        <span class="header-subtitle">向教师发送正式整改要求</span>
      </div>
      <el-button type="primary" @click="showCreateDialog">新建通知书</el-button>
    </div>

    <template v-if="!isMobile">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="关联问题" width="100">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="$router.push(`/inspector/issues/${row.issueId}`)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="接收教师" width="120">
          <template #default="{ row }">{{ row.recipientId }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="发送时间" width="160">
          <template #default="{ row }">{{ row.sentAt || row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="签收时间" width="160">
          <template #default="{ row }">{{ row.acknowledgedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="完成时间" width="160">
          <template #default="{ row }">{{ row.compliedAt || '-' }}</template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 移动端卡片 -->
    <div v-else-if="isMobile" class="mobile-list">
      <div v-for="row in list" :key="row.id" class="mobile-card">
        <div class="mc-header">
          <span class="mc-title">{{ row.title }}</span>
          <el-tag :type="statusTag(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
        </div>
        <div class="mc-body">
          <div class="mc-meta"><span class="mc-lbl">关联问题</span><el-button size="small" link type="primary" @click.stop="$router.push(`/inspector/issues/${row.issueId}`)">查看</el-button></div>
          <div class="mc-meta"><span class="mc-lbl">接收教师</span><span>{{ row.recipientId }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">发送</span><span>{{ row.sentAt || row.createdAt || '-' }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">签收</span><span>{{ row.acknowledgedAt || '-' }}</span></div>
          <div class="mc-meta"><span class="mc-lbl">完成</span><span>{{ row.compliedAt || '-' }}</span></div>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无通知书" :image-size="60" />
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
      v-model="createVisible"
      title="新建整改通知书"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="关联问题">
          <el-select
            v-model="createForm.issueId"
            placeholder="选择问题"
            filterable
            style="width:100%"
            @change="onIssueSelect"
          >
            <el-option
              v-for="i in issueOptions"
              :key="i.id"
              :value="i.id"
              :label="i.title"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="createForm.title" placeholder="通知书标题" />
        </el-form-item>
        <el-form-item label="通知书正文">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入通知书正文"
          />
        </el-form-item>
        <el-form-item label="接收人">
          <el-input v-model="createForm.recipientId" disabled placeholder="自动填充问题责任人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotices, createNotice, getIssues } from '@/api/inspectorManage'
import { useIsMobile } from '@/composables/useIsMobile'

const { isMobile } = useIsMobile()

const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const createVisible = ref(false)
const createLoading = ref(false)
const issueOptions = ref([])

const createForm = reactive({ issueId: null, title: '', content: '', recipientId: null, senderId: null })

const statusMap = { SENT: '已发送', ACKNOWLEDGED: '已签收', COMPLIED: '已完成' }
const statusTag = (s) => ({ SENT: 'primary', ACKNOWLEDGED: 'success', COMPLIED: 'info' })[s] || 'info'

const onIssueSelect = (val) => {
  const issue = issueOptions.value.find(i => i.id === val)
  if (issue) {
    createForm.title = `整改通知：${issue.title}`
    createForm.recipientId = issue.assignedTo
  }
}

const handleCreate = async () => {
  if (!createForm.issueId || !createForm.title || !createForm.content) { ElMessage.warning('请填写完整信息'); return }
  createLoading.value = true
  try {
    const res = await createNotice(createForm)
    if (res.code === 200) { ElMessage.success('发送成功'); createVisible.value = false; loadData() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('发送失败') }
  finally { createLoading.value = false }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getNotices({ page: page.value, size: size.value })
    if (res.code === 200) { list.value = res.data.records || []; total.value = res.data.total || 0 }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const loadIssueOptions = async () => {
  try { const res = await getIssues({ size: 999 }); if (res.code === 200) issueOptions.value = (res.data.records || []).filter(i => i.status !== 'VERIFIED') } catch {}
}

const showCreateDialog = async () => {
  createForm.issueId = null; createForm.title = ''; createForm.content = ''; createForm.recipientId = null
  await loadIssueOptions(); createVisible.value = true
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.inspector-notices { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .page-title { font-size: var(--fs-2xl, 22px); margin: 0; }
  .header-subtitle { font-size: var(--fs-sm); color: var(--text-secondary); }
}
.empty-state { padding: 40px 0; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
@media (max-width: 768px) { .inspector-notices { padding: var(--spacing-md, 16px); } .page-header { flex-direction: column; align-items: stretch; gap: 8px; } }

/* 移动端卡片 */
.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.mobile-card { padding: 14px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-light); }
.mc-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.mc-title { font-weight: 600; font-size: var(--fs-md); color: var(--text-primary); flex: 1; }
.mc-body { display: flex; flex-direction: column; gap: 4px; }
.mc-meta { display: flex; align-items: center; gap: 8px; font-size: var(--fs-sm); }
.mc-lbl { color: var(--text-secondary); min-width: 56px; }
</style>
