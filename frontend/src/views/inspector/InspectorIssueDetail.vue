<template>
  <div v-loading="loading" class="inspector-issue-detail">
    <div class="back-bar">
      <el-button text @click="$router.push('/inspector/issues')"><el-icon><ArrowLeft /></el-icon>返回台账</el-button>
    </div>

    <div class="detail-layout">
      <div class="detail-left">
        <h2 class="issue-title">{{ issue.title }}</h2>

        <el-descriptions
          :column="2"
          border
          size="small"
          class="detail-desc"
        >
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(issue.status)" size="small">{{ statusMap[issue.status] || issue.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="分类">{{ categoryMap[issue.category] || issue.category }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">
            <el-tag :type="severityTag(issue.severity)" size="small">{{ severityMap[issue.severity] || issue.severity }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联班级">{{ issue.assignedClassId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联巡视记录">{{ issue.recordId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="责任人">{{ issue.assignedTo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="截止日期">
            <span :class="{ expired: issue.deadline && isExpired(issue.deadline) }">{{ issue.deadline || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ issue.createdBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ issue.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">问题描述</div>
        <div class="desc-content">{{ issue.description || '暂无描述' }}</div>
      </div>

      <div class="detail-right">
        <div class="section-title">状态时间线</div>
        <el-timeline>
          <el-timeline-item
            v-for="(c, i) in timelineComments"
            :key="c.id"
            :timestamp="c.createdAt"
            :type="i === 0 ? 'primary' : ''"
            :color="i === 0 ? 'var(--primary-color)' : '#e0e0e0'"
            :hollow="i !== 0"
          >
            <span v-html="sanitizeHtml(c.content)" />
          </el-timeline-item>
        </el-timeline>

        <div class="section-title">操作</div>
        <div class="action-buttons">
          <template v-if="isInspector">
            <el-button v-if="issue.status === 'OPEN'" type="primary" @click="showAssignDialog">指派责任人</el-button>
            <el-button v-if="issue.status === 'RESOLVED'" type="success" @click="showVerifyDialog(true)">验收通过</el-button>
            <el-button v-if="issue.status === 'RESOLVED'" type="danger" @click="showVerifyDialog(false)">驳回</el-button>
          </template>
        </div>
      </div>
    </div>

    <div class="section-title">沟通记录</div>
    <div class="comment-list">
      <div v-for="c in userComments" :key="c.id" :class="['comment-item', { 'system-comment': c.isSystem }]">
        <el-avatar :size="32">{{ (c.userName || '?')[0] }}</el-avatar>
        <div class="comment-body">
          <div class="comment-meta">
            <strong>{{ c.userName || '系统' }}</strong>
            <span class="comment-time">{{ c.createdAt }}</span>
          </div>
          <div class="comment-content">{{ c.content }}</div>
        </div>
      </div>
      <div v-if="userComments.length === 0" class="empty-comments">暂无沟通记录</div>
    </div>

    <div class="comment-input">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        placeholder="输入评论..."
      />
      <el-button type="primary" :disabled="!newComment.trim()" @click="handleAddComment">发送</el-button>
    </div>

    <el-dialog
      v-model="assignVisible"
      title="指派责任人"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="选择教师">
          <el-select
            v-model="assignForm.teacherId"
            placeholder="选择教师"
            filterable
            style="width:100%"
          >
            <el-option
              v-for="t in teacherOptions"
              :key="t.id"
              :value="t.id"
              :label="t.realName || t.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="assignForm.deadline"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssign">确认指派</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="verifyVisible"
      :title="verifyApproved ? '验收通过' : '驳回'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="验收意见">
          <el-input
            v-model="verifyForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入验收意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button :type="verifyApproved ? 'success' : 'danger'" :loading="verifyLoading" @click="handleVerify">
          {{ verifyApproved ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getIssue, assignIssue, verifyIssue, addIssueComment, getIssueComments } from '@/api/inspectorManage'
import { getTeacherList } from '@/api/teacher'
import { sanitizeHtml } from '@/utils/markdown'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const issue = ref({})
const comments = ref([])
const newComment = ref('')
const assignVisible = ref(false)
const assignLoading = ref(false)
const verifyVisible = ref(false)
const verifyLoading = ref(false)
const verifyApproved = ref(true)
const teacherOptions = ref([])

const assignForm = reactive({ teacherId: null, deadline: '' })
const verifyForm = reactive({ comment: '' })

const isInspector = computed(() => userStore.isInspector)

const statusMap = { OPEN: '待处理', ASSIGNED: '已指派', IN_PROGRESS: '处理中', RESOLVED: '已解决', VERIFIED: '已验收', REJECTED: '已驳回' }
const statusTag = (s) => ({ OPEN: 'info', ASSIGNED: 'primary', IN_PROGRESS: 'warning', RESOLVED: '', VERIFIED: 'success', REJECTED: 'danger' })[s] || 'info'
const categoryMap = { TEACHING_QUALITY: '教学质量', CLASSROOM_DISCIPLINE: '课堂纪律', HOMEWORK_PROCRASTINATION: '作业拖拉', ATTENDANCE: '出勤', MORAL_EDUCATION: '德育', EXAM_IRREGULARITY: '考试违纪', OTHER: '其他' }
const severityMap = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' }
const severityTag = (s) => ({ LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' })[s] || 'info'

const isExpired = (deadline) => new Date(deadline) < new Date()

const timelineComments = computed(() => comments.value.filter(c => c.isSystem).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)))
const userComments = computed(() => comments.value.filter(c => !c.isSystem).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)))

const showAssignDialog = () => { assignForm.teacherId = null; assignForm.deadline = ''; assignVisible.value = true }
const showVerifyDialog = (approved) => { verifyApproved.value = approved; verifyForm.comment = ''; verifyVisible.value = true }

const handleAssign = async () => {
  if (!assignForm.teacherId) { ElMessage.warning('请选择教师'); return }
  assignLoading.value = true
  try {
    const res = await assignIssue(route.params.id, assignForm.teacherId, assignForm.deadline)
    if (res.code === 200) { ElMessage.success('指派成功'); assignVisible.value = false; loadIssue() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('指派失败') }
  finally { assignLoading.value = false }
}

const handleVerify = async () => {
  verifyLoading.value = true
  try {
    const res = await verifyIssue(route.params.id, userStore.userInfo?.id, verifyApproved.value, verifyForm.comment)
    if (res.code === 200) { ElMessage.success(verifyApproved.value ? '验收通过' : '已驳回'); verifyVisible.value = false; loadIssue() }
    else ElMessage.error(res.message)
  } catch { ElMessage.error('操作失败') }
  finally { verifyLoading.value = false }
}

const handleAddComment = async () => {
  if (!newComment.value.trim()) return
  const res = await addIssueComment(route.params.id, userStore.userInfo?.id, newComment.value, 0)
  if (res.code === 200) { newComment.value = ''; loadComments() }
  else ElMessage.error(res.message)
}

const loadIssue = async () => {
  loading.value = true
  try {
    const [issueRes, commentRes] = await Promise.all([getIssue(route.params.id), getIssueComments(route.params.id)])
    if (issueRes.code === 200) issue.value = issueRes.data
    if (commentRes.code === 200) comments.value = commentRes.data || []
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}
const loadComments = async () => {
  try { const res = await getIssueComments(route.params.id); if (res.code === 200) comments.value = res.data || [] } catch {}
}

onMounted(async () => {
  loading.value = true
  try {
    const [tchRes] = await Promise.all([getTeacherList({ size: 999 })])
    if (tchRes.code === 200) teacherOptions.value = ((tchRes.data && (tchRes.data.records || tchRes.data)) || []).map(t => ({ id: t.id, realName: t.realName || t.name }))
    await loadIssue()
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
})
</script>

<style scoped lang="scss">
.inspector-issue-detail { max-width: 1400px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.back-bar { margin-bottom: 16px; }
.detail-layout { display: flex; gap: 24px; margin-bottom: 24px;
  .detail-left { flex: 3; min-width: 0; }
  .detail-right { flex: 2; min-width: 260px; }
}
.issue-title { font-size: var(--fs-xl); font-weight: 700; margin: 0 0 16px; color: var(--text-primary); }
.detail-desc { margin-bottom: 20px; }
.section-title { font-size: var(--fs-md); font-weight: 600; margin: 20px 0 12px; color: var(--text-primary); border-left: 3px solid var(--primary-color); padding-left: 10px; }
.desc-content { background: var(--bg-card); padding: 16px; border-radius: var(--radius-md); white-space: pre-wrap; font-size: var(--fs-md); line-height: 1.6; color: var(--text-regular); }
.expired { color: var(--danger-color); font-weight: 600; }
.action-buttons { display: flex; flex-direction: column; gap: 10px; }
.comment-list { margin-bottom: 16px;
  .comment-item { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--border-color, #eee);
    &.system-comment { background: var(--bg-secondary, #f5f7fa); padding: 12px; border-radius: var(--radius-md); margin-bottom: 8px; }
  }
  .comment-body { flex: 1; }
  .comment-meta { margin-bottom: 4px; .comment-time { font-size: var(--fs-xs); color: var(--text-secondary); margin-left: 10px; } }
  .comment-content { font-size: var(--fs-md); color: var(--text-regular); white-space: pre-wrap; }
}
.empty-comments { text-align: center; color: var(--text-secondary); padding: 20px; }
.comment-input { display: flex; gap: 10px; align-items: flex-start; :deep(.el-textarea) { flex: 1; } }
@media (max-width: 768px) {
  .inspector-issue-detail { padding: var(--spacing-md, 16px); }
  .detail-layout { flex-direction: column; }
}
</style>
