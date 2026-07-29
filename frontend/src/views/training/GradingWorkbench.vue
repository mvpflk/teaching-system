<template>
  <div class="grading-wb">
    <!-- 头部 -->
    <div class="gw-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="gw-title">{{ task?.title || '评分工作台' }}</span>
        </template>
      </el-page-header>
      <div class="gw-tabs">
        <el-radio-group v-model="statusFilter" size="small" @change="loadSubmissions">
          <el-radio-button value="all">全部({{ stats.total }})</el-radio-button>
          <el-radio-button value="SUBMITTED">待评({{ stats.pending }})</el-radio-button>
          <el-radio-button value="GRADED">已评({{ stats.graded }})</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-if="loading" v-loading="true" style="min-height:300px"></div>

    <template v-else-if="submissions.length > 0">
      <!-- 学生切换 -->
      <div class="gw-student-bar">
        <el-button :disabled="currentIndex <= 0" @click="prevStudent">&#9664; 上一位</el-button>
        <el-select v-model="currentStudentId" @change="selectStudent" style="flex:1;max-width:300px">
          <el-option v-for="s in filteredSubmissions" :key="s.id" :label="s.studentName" :value="s.id">
            <div class="student-option">
              <span>{{ s.studentName }}</span>
              <el-tag size="small" :type="s.status === 'GRADED' ? 'success' : 'warning'">
                {{ s.status === 'GRADED' ? '已评' : '待评' }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
        <el-button :disabled="currentIndex >= filteredSubmissions.length - 1" @click="nextStudent">下一位 &#9654;</el-button>
      </div>

      <!-- 步骤评分 -->
      <div class="gw-steps" v-if="currentSteps">
        <div v-for="(step, i) in currentSteps" :key="i" class="gw-step-item">
          <div class="gw-step-header">
            <span class="step-num">Step {{ i + 1 }}</span>
            <span class="step-title">{{ step.title }}</span>
            <el-tag :type="stepTagType(step.type)" size="small">{{ stepTypeLabel(step.type) }}</el-tag>
          </div>

          <!-- 学生提交内容展示 -->
          <div class="gw-step-body">
            <!-- text 类型 -->
            <div v-if="step.type === 'text'" class="step-preview-text">
              {{ studentStepData[i]?.content || '(未填写)' }}
            </div>
            <!-- file 类型 -->
            <div v-else-if="step.type === 'file'" class="step-preview-file">
              <div v-if="studentStepData[i]?.files?.length">
                <el-link v-for="(f, fi) in studentStepData[i].files" :key="fi" :href="f.url" target="_blank">
                  <el-icon><Link /></el-icon> {{ f.name }}
                </el-link>
              </div>
              <span v-else class="text-muted">(未上传文件)</span>
            </div>
            <!-- sim 类型 -->
            <div v-else-if="step.type === 'sim'" class="step-preview-sim">
              <div v-if="studentStepData[i]?.completed">&#9989; 仿真已完成</div>
              <span v-else class="text-muted">(未完成)</span>
            </div>
            <!-- ppt 类型 -->
            <div v-else-if="step.type === 'ppt'" class="step-preview-ppt">
              <div v-if="studentStepData[i]?.checkResult">
                <span>通过率: {{ studentStepData[i].checkResult.passedCount }}/{{ studentStepData[i].checkResult.totalCount }}</span>
              </div>
              <span v-else class="text-muted">(未上传文件)</span>
            </div>
            <!-- excel 类型 -->
            <div v-else-if="step.type === 'excel'" class="step-preview-excel">
              <div v-if="studentStepData[i]?.checkResult">
                <div class="cp-summary">
                  通过率: {{ studentStepData[i].checkResult.passedCount }}/{{ studentStepData[i].checkResult.totalCount }}
                  ({{ studentStepData[i].checkResult.score || 0 }}分)
                </div>
                <div v-for="c in (studentStepData[i].checkResult.checkpoints || [])" :key="c.id" class="cp-row">
                  <span :style="{ color: c.passed ? 'var(--el-color-success, #67c23a)' : 'var(--el-color-danger, #f56c6c)' }">{{ c.passed ? '✅' : '❌' }}</span>
                  <span>{{ c.desc }}</span>
                </div>
              </div>
              <span v-else class="text-muted">(未上传文件)</span>
            </div>
            <!-- choice 类型 -->
            <div v-else-if="step.type === 'choice'" class="step-preview-choice">
              <div v-for="(q, qi) in (step.config?.questions || [])" :key="qi">
                {{ qi + 1 }}. {{ q.stem }}
                <span :class="studentStepData[i]?.answers?.[qi] === q.answer ? 'correct' : 'wrong'">
                  学生答案: {{ studentStepData[i]?.answers?.[qi] || '未答' }}
                  | 正确答案: {{ q.answer }}
                </span>
              </div>
            </div>
            <!-- sql 类型 -->
            <div v-else-if="step.type === 'sql'" class="step-preview-sql">
              <div v-if="studentStepData[i]?.sql">
                <pre class="sql-code-preview">{{ studentStepData[i].sql }}</pre>
                <span :style="{ color: studentStepData[i]?.sqlResult?.passed ? 'var(--el-color-success, #67c23a)' : 'var(--el-color-danger, #f56c6c)' }">
                  {{ studentStepData[i]?.sqlResult?.passed ? '✅ 正确' : '❌ 不正确' }}
                </span>
              </div>
              <span v-else class="text-muted">(未提交)</span>
            </div>
            <!-- web 类型 -->
            <div v-else-if="step.type === 'web'" class="step-preview-web">
              <div v-if="studentStepData[i]?.html">
                <pre class="sql-code-preview">{{ studentStepData[i].html?.slice(0, 200) }}{{ (studentStepData[i].html?.length > 200) ? '…' : '' }}</pre>
                <span>已提交代码</span>
              </div>
              <span v-else class="text-muted">(未提交)</span>
            </div>
            <!-- office 类型 -->
            <div v-else-if="step.type === 'office'" class="step-preview-office">
              <div v-if="studentStepData[i]?.checkResult">
                <span>预检通过率: {{ studentStepData[i].checkResult.passedCount || 0 }}/{{ studentStepData[i].checkResult.totalCount || 0 }}</span>
              </div>
              <div v-else-if="studentStepData[i]?.fileUrl">
                <el-link :href="studentStepData[i].fileUrl" target="_blank">📄 查看文档</el-link>
              </div>
              <span v-else class="text-muted">(未上传)</span>
            </div>
          </div>

          <!-- 评分输入 -->
          <div class="gw-step-score">
            <!-- AI 评分：显示分数 + 置信度 + 理由 -->
            <template v-if="aiGrades[i]">
              <el-input-number
                :model-value="aiGrades[i].score"
                :min="0" :max="step.score?.max || 100"
                disabled size="small"
              />
              <el-popover trigger="hover" :content="aiGrades[i].reason || ''" placement="top">
                <template #reference>
                  <el-tag size="small" :type="aiConfTag(aiGrades[i].confidence)" style="margin-left:6px; cursor:help">
                    AI {{ ((aiGrades[i].confidence || 0) * 100).toFixed(0) }}%
                  </el-tag>
                </template>
              </el-popover>
            </template>
            <!-- 检查点规则评分 -->
            <template v-else-if="step.score?.method === 'auto'">
              <el-input-number
                :model-value="stepScores[i]"
                :min="0" :max="step.score?.max || 100"
                disabled size="small"
              />
              <el-tag size="small" type="success" style="margin-left:6px">自动</el-tag>
            </template>
            <!-- 手动评分 -->
            <template v-else>
              <el-input-number
                v-model="stepScores[i]"
                :min="0" :max="step.score?.max || 100"
                size="small"
              />
            </template>
            <span class="score-max">/ {{ step.score?.max || 100 }}</span>
          </div>
        </div>

        <!-- 总分 + 评语 -->
        <div class="gw-final">
          <div class="final-score">
            总分: <strong>{{ totalScore }}</strong>
          </div>
          <el-input v-model="comment" type="textarea" :rows="3" placeholder="评语（可选）" style="margin:12px 0" />
          <el-button type="primary" :loading="saving" @click="submitGrade">提交评分</el-button>
        </div>
      </div>
    </template>

    <el-empty v-else description="暂无提交记录" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Link } from '@element-plus/icons-vue'
import { getSubmissionsForGrading, submitFinalGrade } from '@/api/training'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.params.taskId)

const task = ref(null)
const submissions = ref([])
const loading = ref(false)
const saving = ref(false)
const statusFilter = ref('all')
const currentStudentId = ref(null)
const currentSteps = ref(null)
const studentStepData = ref({})
const stepScores = ref([])
const aiGrades = ref({})
const comment = ref('')

const stats = computed(() => ({
  total: submissions.value.length,
  pending: submissions.value.filter(s => s.status === 'SUBMITTED').length,
  graded: submissions.value.filter(s => s.status === 'GRADED').length
}))

const filteredSubmissions = computed(() => {
  if (statusFilter.value === 'all') return submissions.value
  return submissions.value.filter(s => s.status === statusFilter.value)
})

const currentIndex = computed(() =>
  filteredSubmissions.value.findIndex(s => s.id === currentStudentId.value)
)

function stepTagType(type) {
  return { text: '', file: 'warning', sim: 'danger', office: '', excel: 'success', ppt: 'warning', web: 'warning', choice: 'success', sql: 'danger' }[type] || ''
}
function stepTypeLabel(type) {
  return { text: '文字论述', file: '文件提交', sim: '仿真操作', office: 'Word 文档', excel: 'Excel 表格', ppt: 'PPT 演示', web: '网页制作', choice: '选择题', sql: 'SQL 查询' }[type] || type
}
function aiConfTag(confidence) {
  if (confidence == null) return 'info'
  if (confidence >= 0.85) return 'success'
  if (confidence >= 0.7) return 'warning'
  return 'danger'
}

const totalScore = computed(() => {
  if (!currentSteps.value) return 0
  return currentSteps.value.reduce((sum, step, i) => {
    return sum + (Number(stepScores.value[i]) || 0)
  }, 0)
})

async function loadSubmissions() {
  loading.value = true
  try {
    const res = await getSubmissionsForGrading(taskId, { status: statusFilter.value })
    if (res.code === 200) {
      task.value = res.data.task || {}
      submissions.value = res.data.submissions || []
      if (!currentStudentId.value && submissions.value.length > 0) {
        selectStudent(submissions.value[0].id)
      }
    }
  } catch (e) {
    // 实训模块实验期暂停（后端 410），静默展示空态，不弹错误提示
    submissions.value = []
  } finally {
    loading.value = false
  }
}

function selectStudent(id) {
  currentStudentId.value = id
  if (!submissions.value.length) { currentSteps.value = []; stepScores.value = {}; return }
  const sub = submissions.value.find(s => s.id === id)
  if (sub) {
    currentSteps.value = sub.stepDefs || (task.value?.steps || [])
    studentStepData.value = sub.stepData || {}
    stepScores.value = sub.stepScores || {}
    aiGrades.value = sub.aiGrades || {}
    comment.value = sub.comment || ''
  }
}

function prevStudent() {
  const idx = currentIndex.value
  if (idx > 0) selectStudent(filteredSubmissions.value[idx - 1].id)
}
function nextStudent() {
  const idx = currentIndex.value
  if (idx < filteredSubmissions.value.length - 1) selectStudent(filteredSubmissions.value[idx + 1].id)
}

async function submitGrade() {
  saving.value = true
  try {
    const res = await submitFinalGrade(taskId, currentStudentId.value, {
      stepScores: { ...stepScores.value },
      comment: comment.value
    })
    if (res.code === 200) {
      ElMessage.success('评分已提交')
      loadSubmissions()
    } else {
      ElMessage.error(res.message || '提交评分失败')
    }
  } catch (e) {
    ElMessage.error('提交评分失败: ' + (e.message || '网络错误'))
  } finally {
    saving.value = false
  }
}

onMounted(loadSubmissions)
</script>

<style scoped>
.grading-wb { max-width: 1000px; margin: 0 auto; padding: 16px; }
.gw-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.gw-title { font-size: var(--fs-lg); font-weight: 600; }
.gw-student-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.student-option { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.gw-step-item { border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 12px; }
.gw-step-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.step-num { font-weight: 600; color: var(--primary-color); }
.step-title { flex: 1; }
.gw-step-body { margin-bottom: 12px; padding: 12px; background: var(--bg-page); border-radius: var(--radius-sm); font-size: var(--fs-sm); }
.step-preview-text { white-space: pre-wrap; max-height: 200px; overflow-y: auto; }
.step-preview-file { display: flex; flex-direction: column; gap: 4px; }
.step-preview-choice .correct { color: var(--color-success); }
.step-preview-choice .wrong { color: var(--color-danger); }
.gw-step-score { display: flex; align-items: center; }
.score-max { margin-left: 4px; font-size: var(--fs-sm); color: var(--text-secondary); }
.gw-final { text-align: right; padding-top: 12px; border-top: 1px solid var(--border-light); }
.final-score { font-size: var(--fs-lg); }
.text-muted { color: var(--text-secondary); }
</style>
