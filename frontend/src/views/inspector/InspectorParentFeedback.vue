<template>
  <div class="inspector-feedback">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="📋 问卷管理" name="forms">
        <div class="toolbar">
          <el-button type="primary" @click="openCreate">+ 新建问卷</el-button>
        </div>

        <el-table
          v-loading="formsLoading"
          :data="forms"
          stripe
          @filter-change="handleFilter"
        >
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column label="班级" width="120">
            <template #default="{ row }">{{ getClassName(row.classId) }}</template>
          </el-table-column>
          <el-table-column prop="period" label="周期" width="100" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发送时间" width="150">
            <template #default="{ row }">{{ row.sentAt ? fmt(row.sentAt) : '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="viewResponses(row)">查看</el-button>
              <el-button v-if="row.status === 'DRAFT'" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                size="small"
                type="primary"
                :loading="sendingId === row.id"
                @click="doSend(row.id)"
              >
                发送
              </el-button>
              <el-button
                v-if="row.status === 'SENT'"
                size="small"
                type="warning"
                :loading="closingId === row.id"
                @click="doClose(row.id)"
              >
                关闭
              </el-button>
              <el-popconfirm v-if="row.status !== 'SENT'" title="确认删除?" @confirm="doDelete(row.id)">
                <template #reference><el-button size="small" type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="formsPage"
            :page-size="formsSize"
            :total="formsTotal"
            layout="prev,pager,next,total"
            @current-change="loadForms"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="📊 汇总报告" name="summary">
        <el-form :model="summaryFilters" inline class="filter-bar">
          <el-form-item label="班级">
            <el-select
              v-model="summaryFilters.classId"
              placeholder="全部"
              clearable
              filterable
              style="width:160px"
            >
              <el-option
                v-for="c in classList"
                :key="c.id"
                :label="c.className"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="周期">
            <el-input
              v-model="summaryFilters.period"
              placeholder="如 2026-W22"
              clearable
              style="width:150px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadSummaries">查询</el-button>
            <el-button @click="summaryFilters = { classId: null, period: '' }; loadSummaries()">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="summariesLoading" :data="summaries" stripe>
          <el-table-column label="班级" width="120">
            <template #default="{ row }">{{ getClassName(row.classId) }}</template>
          </el-table-column>
          <el-table-column prop="period" label="周期" width="100" />
          <el-table-column prop="totalFeedback" label="反馈数" width="80" />
          <el-table-column prop="positiveCount" label="正面" width="60" />
          <el-table-column prop="negativeCount" label="负面" width="60" />
          <el-table-column label="满意率" width="80">
            <template #default="{ row }">
              <el-tag :type="row.totalFeedback > 0 && (row.positiveCount / row.totalFeedback) >= 0.8 ? 'success' : 'warning'" size="small">
                {{ row.totalFeedback > 0 ? Math.round(row.positiveCount / row.totalFeedback * 100) : 0 }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="summaryText"
            label="摘要"
            min-width="200"
            show-overflow-tooltip
          />
        </el-table>
        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="summaryPage"
            :page-size="summarySize"
            :total="summaryTotal"
            layout="prev,pager,next,total"
            @current-change="loadSummaries"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建/编辑问卷弹窗 -->
    <el-dialog v-model="showForm" :title="editingForm ? '编辑问卷' : '新建问卷'" width="500px">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="班级" required>
          <el-select v-model="formData.classId" filterable style="width:100%">
            <el-option
              v-for="c in classList"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="formData.title" placeholder="如 2026年春季学期家长满意度调查" />
        </el-form-item>
        <el-form-item label="周期" required>
          <el-input v-model="formData.period" placeholder="如 2026-S1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="savingForm" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看回复弹窗 -->
    <el-dialog v-model="showResponses" :title="'回复详情 - ' + (responsesForm?.title || '')" width="700px">
      <div v-if="statsLoading" class="sk-list">
        <div
          v-for="i in 3"
          :key="i"
          class="sk-line w-60"
          style="margin-bottom:8px;height:16px"
        />
      </div>
      <div v-else-if="stats" class="stats-grid">
        <div class="stat-box"><span class="stat-num">{{ stats.responseRate || 0 }}%</span><span class="stat-lbl">回收率</span></div>
        <div class="stat-box"><span class="stat-num">{{ stats.avgSatisfaction || '-' }}</span><span class="stat-lbl">满意度</span></div>
        <div class="stat-box"><span class="stat-num">{{ stats.avgTeachingQuality || '-' }}</span><span class="stat-lbl">教学质量</span></div>
        <div class="stat-box"><span class="stat-num">{{ stats.avgHomeworkLoad || '-' }}</span><span class="stat-lbl">作业量</span></div>
        <div class="stat-box"><span class="stat-num">{{ stats.avgCommunication || '-' }}</span><span class="stat-lbl">沟通</span></div>
      </div>
      <el-button
        size="small"
        type="primary"
        :loading="generating"
        style="margin-bottom:12px"
        @click="doGenerateSummary"
      >
        生成汇总报告
      </el-button>
      <el-table
        v-loading="responsesLoading"
        :data="responses"
        stripe
        max-height="400"
      >
        <el-table-column label="家长ID" prop="parentId" width="80" />
        <el-table-column label="满意度" width="80">
          <template #default="{ row }">
            <el-rate
              :model-value="row.satisfaction"
              disabled
              size="small"
              :max="5"
            />
          </template>
        </el-table-column>
        <el-table-column label="教学质量" width="80">
          <template #default="{ row }">
            <el-rate
              :model-value="row.teachingQuality"
              disabled
              size="small"
              :max="5"
            />
          </template>
        </el-table-column>
        <el-table-column label="作业量" width="80">
          <template #default="{ row }">
            <el-rate
              :model-value="row.homeworkLoad"
              disabled
              size="small"
              :max="5"
            />
          </template>
        </el-table-column>
        <el-table-column label="沟通" width="80">
          <template #default="{ row }">
            <el-rate
              :model-value="row.communication"
              disabled
              size="small"
              :max="5"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="comment"
          label="建议"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ row.createdAt ? fmt(row.createdAt) : '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getClassList } from '@/api/classes'
import { getParentFeedback } from '@/api/inspectorManage'
import {
  getFeedbackForms, createFeedbackForm, updateFeedbackForm, deleteFeedbackForm,
  sendFeedbackForm, closeFeedbackForm, getFeedbackResponses, getFeedbackStats, generateFeedbackSummary
} from '@/api/parentFeedback'

const activeTab = ref('forms')

// ── 问卷管理 ──
const forms = ref([])
const formsLoading = ref(false)
const formsPage = ref(1)
const formsSize = ref(20)
const formsTotal = ref(0)
const sendingId = ref(null)
const closingId = ref(null)
const showForm = ref(false)
const editingForm = ref(null)
const savingForm = ref(false)
const formData = ref({ classId: null, title: '', period: '' })

const classList = ref([])

const statusTag = (s) => s === 'SENT' ? 'primary' : s === 'CLOSED' ? 'info' : 'default'
const statusLabel = (s) => s === 'DRAFT' ? '草稿' : s === 'SENT' ? '已发送' : s === 'CLOSED' ? '已关闭' : s || '-'
const getClassName = (id) => classList.value.find(c => c.id === id)?.className || '?'

const fmt = (s) => {
  if (!s) return '-'
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadForms() {
  formsLoading.value = true
  try {
    const res = await getFeedbackForms({ page: formsPage.value, size: formsSize.value })
    if (res.code === 200) {
      forms.value = res.data.records || []
      formsTotal.value = res.data.total || 0
    }
  } finally { formsLoading.value = false }
}

function openCreate() {
  editingForm.value = null
  formData.value = { classId: null, title: '', period: '' }
  showForm.value = true
}

function openEdit(row) {
  editingForm.value = row
  formData.value = { classId: row.classId, title: row.title, period: row.period }
  showForm.value = true
}

async function saveForm() {
  if (!formData.value.classId || !formData.value.title || !formData.value.period) {
    ElMessage.warning('请填写完整信息'); return
  }
  savingForm.value = true
  try {
    if (editingForm.value) {
      await updateFeedbackForm(editingForm.value.id, formData.value)
    } else {
      await createFeedbackForm(formData.value)
    }
    showForm.value = false
    ElMessage.success('保存成功')
    loadForms()
  } catch { ElMessage.error('操作失败') }
  finally { savingForm.value = false }
}

async function doSend(id) {
  sendingId.value = id
  try {
    const res = await sendFeedbackForm(id)
    if (res.code === 200) { ElMessage.success('已发送'); loadForms() }
    else ElMessage.error(res.message || '发送失败')
  } catch { ElMessage.error('发送失败') }
  finally { sendingId.value = null }
}

async function doClose(id) {
  closingId.value = id
  try {
    const res = await closeFeedbackForm(id)
    if (res.code === 200) { ElMessage.success('已关闭'); loadForms() }
    else ElMessage.error(res.message || '关闭失败')
  } catch { ElMessage.error('关闭失败') }
  finally { closingId.value = null }
}

async function doDelete(id) {
  try {
    await deleteFeedbackForm(id)
    ElMessage.success('已删除')
    loadForms()
  } catch { ElMessage.error('删除失败') }
}

// ── 查看回复 ──
const showResponses = ref(false)
const responsesForm = ref(null)
const responses = ref([])
const responsesLoading = ref(false)
const stats = ref(null)
const statsLoading = ref(false)
const generating = ref(false)

async function viewResponses(row) {
  responsesForm.value = row
  showResponses.value = true
  responsesLoading.value = true
  statsLoading.value = true
  try {
    const [resR, resS] = await Promise.all([
      getFeedbackResponses(row.id),
      getFeedbackStats(row.id)
    ])
    if (resR.code === 200) responses.value = resR.data || []
    if (resS.code === 200) stats.value = resS.data
  } finally { responsesLoading.value = false; statsLoading.value = false }
}

async function doGenerateSummary() {
  if (!responsesForm.value) return
  generating.value = true
  try {
    const res = await generateFeedbackSummary(responsesForm.value.id)
    if (res.code === 200) {
      ElMessage.success('汇总报告已生成')
      stats.value = res.data
    } else ElMessage.error(res.message || '生成失败')
  } catch { ElMessage.error('生成失败') }
  finally { generating.value = false }
}

function handleFilter() {}

// ── 汇总报告列表 ──
const summaries = ref([])
const summariesLoading = ref(false)
const summaryPage = ref(1)
const summarySize = ref(20)
const summaryTotal = ref(0)
const summaryFilters = ref({ classId: null, period: '' })

async function loadSummaries() {
  summariesLoading.value = true
  try {
    const res = await getParentFeedback({ ...summaryFilters.value, page: summaryPage.value, size: summarySize.value })
    if (res.code === 200) {
      summaries.value = res.data.records || []
      summaryTotal.value = res.data.total || 0
    }
  } finally { summariesLoading.value = false }
}

onMounted(async () => {
  const res = await getClassList()
  classList.value = res.data || []
  loadForms()
  loadSummaries()
})
</script>

<style scoped lang="scss">
.inspector-feedback { max-width: 1280px; margin: 0 auto; padding: var(--spacing-lg, 24px); }
.toolbar { margin-bottom: 16px; }
.filter-bar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }
.stats-grid { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.stat-box { flex: 1; min-width: 80px; text-align: center; padding: 12px 8px; background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.stat-num { display: block; font-size: 22px; font-weight: 700; color: var(--el-color-primary); }
.stat-lbl { display: block; font-size: var(--fs-xs); color: var(--text-secondary); margin-top: 4px; }
.sk-line { height: 14px; background: var(--bg-secondary); border-radius: var(--radius-xs); animation: sk-shimmer 1.6s infinite; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
</style>
