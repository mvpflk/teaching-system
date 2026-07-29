<template>
  <div class="lesson-prep-workbench">
    <el-card v-if="groupInfo" class="group-header">
      <div class="group-info">
        <span class="group-name">{{ groupInfo.name }}</span>
        <el-tag type="success" size="small">备课组</el-tag>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="main-tabs">
      <el-tab-pane label="📋 备课活动" name="records">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>备课活动记录</span>
              <el-button type="primary" @click="showForm = true">新建备课活动</el-button>
            </div>
          </template>

          <el-form :model="filters" inline class="filter-bar">
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="filters.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
                style="width:150px"
              />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="filters.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
                style="width:150px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadRecords">查询</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loadingRecords" :data="recordList" stripe>
            <el-table-column prop="recordDate" label="活动日期" width="110" />
            <el-table-column
              prop="title"
              label="活动主题"
              min-width="200"
              show-overflow-tooltip
            />
            <el-table-column prop="participantCount" label="参与人数" width="100" />
            <el-table-column prop="totalMembers" label="组成员总数" width="100" />
            <el-table-column label="参与率" width="80">
              <template #default="{ row }">
                <template v-if="row.participationRate != null">{{ Math.round(row.participationRate * 100) }}%</template>
                <template v-else>-</template>
              </template>
            </el-table-column>
            <el-table-column
              prop="content"
              label="活动内容"
              min-width="250"
              show-overflow-tooltip
            />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="editRecord(row)">编辑</el-button>
                <el-popconfirm title="确认删除?" @confirm="deleteRecord(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="page"
              :page-size="size"
              :total="total"
              layout="prev,pager,next,total"
              @current-change="loadRecords"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="👥 组员活跃度" name="members">
        <el-card>
          <template #header><span>组员活跃度</span></template>
          <el-table v-loading="loadingMembers" :data="memberList" stripe>
            <el-table-column prop="teacherName" label="教师姓名" min-width="140" />
            <el-table-column prop="teacherNumber" label="工号" width="120" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.role === 'LEADER'" type="success" size="small">组长</el-tag>
                <el-tag v-else type="info" size="small">组员</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="activityCount" label="参与备课次数" width="130" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="✅ 待审核任务" name="reviews">
        <el-card>
          <template #header><span>待审核任务</span></template>
          <el-table v-loading="loadingReviews" :data="pendingReviewList" stripe>
            <el-table-column
              prop="title"
              label="任务名称"
              min-width="200"
              show-overflow-tooltip
            />
            <el-table-column prop="taskType" label="类型" width="120" />
            <el-table-column prop="submitterName" label="提交人" width="120" />
            <el-table-column prop="submitTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="approveTask(row)">通过</el-button>
                <el-button size="small" type="warning" @click="showRejectDialog(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingReviews && pendingReviewList.length === 0" description="暂无待审核任务" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showForm" :title="editingId ? '编辑备课活动' : '新建备课活动'" width="580px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="活动日期" required>
          <el-date-picker
            v-model="form.recordDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="活动主题" required>
          <el-input v-model="form.title" style="width:100%" />
        </el-form-item>
        <el-form-item label="参与人数">
          <el-input-number v-model="form.participantCount" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="活动内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRecord">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showReject" title="驳回任务" width="420px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入驳回原因"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReject = false">取消</el-button>
        <el-button type="primary" :loading="rejecting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyLessonPrepGroup, getLessonPrepRecords, createLessonPrepRecord, updateLessonPrepRecord, deleteLessonPrepRecord, getLessonPrepMembers, getLessonPrepPendingReviews } from '@/api/teacherLessonPrep'
import { approveReview, rejectReview } from '@/api/task'

const activeTab = ref('records')
const groupInfo = ref(null)

const recordList = ref([])
const loadingRecords = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filters = ref({ startDate: '', endDate: '' })
const showForm = ref(false)
const editingId = ref(null)
const saving = ref(false)
const form = ref({ recordDate: '', title: '', participantCount: 0, content: '' })

const memberList = ref([])
const loadingMembers = ref(false)

const pendingReviewList = ref([])
const loadingReviews = ref(false)
const showReject = ref(false)
const rejecting = ref(false)
const rejectForm = ref({ taskId: null, reason: '' })

function resetFilters() {
  filters.value = { startDate: '', endDate: '' }
  page.value = 1
  loadRecords()
}

async function loadRecords() {
  loadingRecords.value = true
  try {
    const res = await getLessonPrepRecords({ ...filters.value, page: page.value, size: size.value })
    recordList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('加载活动列表失败') }
  finally { loadingRecords.value = false }
}

async function loadMembers() {
  loadingMembers.value = true
  try {
    const res = await getLessonPrepMembers()
    memberList.value = res.data || []
  } catch { ElMessage.error('加载组员列表失败') }
  finally { loadingMembers.value = false }
}

async function loadPendingReviews() {
  loadingReviews.value = true
  try {
    const res = await getLessonPrepPendingReviews()
    pendingReviewList.value = res.data || []
  } catch { ElMessage.error('加载待审核任务失败') }
  finally { loadingReviews.value = false }
}

function editRecord(row) {
  editingId.value = row.id
  form.value = { recordDate: row.recordDate, title: row.title, participantCount: row.participantCount || 0, content: row.content || '' }
  showForm.value = true
}

async function saveRecord() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateLessonPrepRecord(editingId.value, form.value)
    } else {
      await createLessonPrepRecord(form.value)
    }
    showForm.value = false
    editingId.value = null
    form.value = { recordDate: '', title: '', participantCount: 0, content: '' }
    loadRecords()
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

async function deleteRecord(id) {
  try {
    await deleteLessonPrepRecord(id)
    ElMessage.success('已删除')
    loadRecords()
  } catch { ElMessage.error('删除失败') }
}

async function approveTask(row) {
  try {
    await approveReview(row.taskId)
    ElMessage.success('已通过')
    loadPendingReviews()
  } catch { ElMessage.error('操作失败') }
}

function showRejectDialog(row) {
  rejectForm.value = { taskId: row.taskId, reason: '' }
  showReject.value = true
}

async function confirmReject() {
  if (!rejectForm.value.reason) { ElMessage.warning('请输入驳回原因'); return }
  rejecting.value = true
  try {
    await rejectReview(rejectForm.value.taskId, rejectForm.value.reason)
    ElMessage.success('已驳回')
    showReject.value = false
    loadPendingReviews()
  } catch { ElMessage.error('操作失败') }
  finally { rejecting.value = false }
}

onMounted(async () => {
  try {
    const res = await getMyLessonPrepGroup()
    groupInfo.value = res.data
  } catch { groupInfo.value = null }
  if (groupInfo.value) {
    loadRecords()
    loadMembers()
    loadPendingReviews()
  }
})
</script>

<style scoped>
.lesson-prep-workbench { padding: 16px; }
.group-header { margin-bottom: 16px; }
.group-info { display: flex; align-items: center; gap: 8px; }
.group-name { font-size: var(--fs-lg); font-weight: 600; }
.main-tabs { margin-top: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }
</style>
