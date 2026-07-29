<template>
  <div v-loading="loading" class="tpg-page">
    <div class="tpg-header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h3>实训评阅 — {{ task?.title }}</h3>
      <el-button type="primary" size="small" @click="showDownloadDialog">📦 打包下载</el-button>
    </div>

    <!-- 左右分栏 -->
    <div class="tpg-split">
      <!-- 左：学生列表 -->
      <div class="tpg-left">
        <el-input
          v-model="searchText"
          placeholder="搜索学生"
          size="small"
          clearable
          style="margin-bottom:8px"
        />
        <div
          v-for="s in filteredStudents"
          :key="s.studentId"
          class="student-row"
          :class="{ active: currentStudentId === s.studentId }"
          @click="selectStudent(s)"
        >
          <span class="s-name">{{ s.studentName }}</span>
          <el-tag :type="statusInfo(s.status).type" size="small">
            {{ statusInfo(s.status).label }}
          </el-tag>
        </div>
        <el-empty v-if="!filteredStudents.length" description="暂无提交" />
      </div>

      <!-- 右：步骤详情 -->
      <div v-if="current" class="tpg-right">
        <div class="right-header">
          <span class="rh-name">{{ current.studentName }}</span>
          <span class="rh-status">状态: {{ current.status }}</span>
          <el-input-number
            v-model="overallScore"
            :min="0"
            :max="100"
            :precision="1"
            size="small"
            style="width:120px"
            placeholder="总分"
          />
          <el-input
            v-model="overallComment"
            size="small"
            placeholder="总评语"
            style="flex:1;max-width:300px"
          />
          <el-button
            type="success"
            size="small"
            :loading="saving"
            @click="doGrade"
          >
            提交评分
          </el-button>
        </div>
        <RadarChart
          v-if="radarData.length"
          :dimensions="radarData"
          title="能力雷达图"
          size="small"
          style="margin-top:12px"
        />
        <div v-for="(step, si) in current.steps" :key="si" class="grade-step">
          <div class="gs-header">
            <span class="gs-idx">步骤 {{ pad(si + 1) }}: {{ step.title }}</span>
            <el-input-number
              v-model="step._score"
              :min="0"
              :max="20"
              :precision="1"
              size="small"
              style="width:80px"
              placeholder="得分"
            />
          </div>
          <div class="gs-desc">{{ step.description }}</div>
          <div v-if="step.images?.length" class="grade-images">
            <img
              v-for="(url, ii) in step.images"
              :key="ii"
              :src="url"
              class="grade-image-item"
              @click="previewImage(url)"
            />
          </div>
          <div v-if="step.files?.length" class="gs-files">
            <a
              v-for="(f, fi) in step.files"
              :key="fi"
              :href="f.url"
              target="_blank"
              class="gs-file"
            >📎 {{ f.name }}</a>
          </div>
          <el-input
            v-model="step._comment"
            size="small"
            placeholder="步骤评语（可选）"
            style="margin-top:6px"
          />
        </div>
      </div>
      <el-empty v-else description="请选择左侧学生" class="tpg-empty" />
    </div>

    <!-- 下载对话框 -->
    <el-dialog v-model="dlVisible" title="打包下载实训" width="450px">
      <el-form label-width="80px">
        <el-form-item label="班级">
          <el-select v-model="dlClassId" placeholder="选择班级" @change="loadDlStudents">
            <el-option
              v-for="c in classOptions"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学生">
          <el-select
            v-model="dlStudentIds"
            placeholder="全部已提交"
            multiple
            clearable
          >
            <el-option
              v-for="s in dlStudentOptions"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlVisible = false">取消</el-button>
        <el-button type="primary" :loading="dlGenerating" @click="doDownload">生成ZIP</el-button>
      </template>
    </el-dialog>

    <!-- 图片全屏预览 -->
    <el-dialog
      v-model="imgPreviewVisible"
      :fullscreen="isMobile"
      :show-close="true"
      title="图片预览"
    >
      <img :src="imgPreviewSrc" style="width:100%;height:auto" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listSteps, gradePractice, getPracticeSubmissions } from '@/api/practice'
import RadarChart from '@/components/common/RadarChart.vue'
import { getClassList, getStudents } from '@/api/classes'
import { startDownload, getDownloadStatus } from '@/api/practice'
import { getTask } from '@/api/task'
import { useIsMobile } from '@/composables/useIsMobile'

const route = useRoute()
const taskId = Number(route.params.taskId)
const loading = ref(false)
const saving = ref(false)
const task = ref(null)
const students = ref([])
const current = ref(null)
const currentStudentId = ref(null)
const searchText = ref('')
const overallScore = ref(0)
const overallComment = ref('')
const radarData = ref([])

const dlVisible = ref(false)
const dlGenerating = ref(false)
const pollTimer = ref(null)
const dlClassId = ref(null)
const dlStudentIds = ref([])
const dlStudentOptions = ref([])
const classOptions = ref([])

const { isMobile } = useIsMobile()

const imgPreviewVisible = ref(false)
const imgPreviewSrc = ref('')
function previewImage(src) { imgPreviewSrc.value = src; imgPreviewVisible.value = true }

const statusMap = {
  PENDING: { label: '待提交', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'warning' },
  GRADED: { label: '已评分', type: 'success' },
  RETURNED: { label: '已退回', type: 'warning' },
  WITHDRAWN: { label: '已撤回', type: 'info' }
}
function statusInfo(status) { return statusMap[status] || { label: status, type: '' } }

const filteredStudents = computed(() => {
  if (!searchText.value) return students.value
  const kw = searchText.value.toLowerCase()
  return students.value.filter(s => s.studentName?.toLowerCase().includes(kw))
})

function pad(n) { return n < 10 ? '0' + n : String(n) }

async function load() {
  loading.value = true
  try {
    const [tRes, sRes] = await Promise.all([getTask(taskId), getPracticeSubmissions(taskId)])
    if (tRes.code === 200) task.value = tRes.data
    if (sRes.code === 200) {
      const subs = sRes.data?.records || sRes.data || []
      const list = []
      for (const sub of subs) {
        list.push({
          studentId: sub.studentId,
          studentName: sub.studentName || `学生${sub.studentId}`,
          status: sub.status || 'PENDING',
          submissionId: sub.id,
          steps: [],
          _loaded: false
        })
      }
      students.value = list
    }
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

async function selectStudent(s) {
  currentStudentId.value = s.studentId
  radarData.value = []
  if (!s._loaded) {
    try {
      const res = await listSteps(taskId, s.studentId)
      s.steps = res.code === 200 ? (res.data || []).map(st => ({ ...st, _score: 0, _comment: '' })) : []
      s._loaded = true
    } catch { /* */ }
  }
  current.value = s
  overallScore.value = s.overallScore || 0
  overallComment.value = s.overallComment || ''
}

async function doGrade() {
  if (!current.value) return
  saving.value = true
  try {
    const stepGrades = (current.value.steps || []).map(s => ({
      stepId: s.stepId,
      stepScore: s._score || 0,
      stepComment: s._comment || ''
    }))
    const res = await gradePractice({
      submissionId: current.value.submissionId,
      overallScore: overallScore.value,
      overallComment: overallComment.value,
      stepGrades
    })
    if (res.code === 200) {
      current.value.status = 'GRADED'
      ElMessage.success('评分已保存')
      if (res.data?.skillScore !== undefined) {
        radarData.value = [
          { name: '技能水平', score: res.data.skillScore, max: 10 },
          { name: '职业素养', score: res.data.profScore, max: 10 },
          { name: '应用价值', score: res.data.valueScore, max: 10 },
          { name: '创新创意', score: res.data.innovScore, max: 10 },
          { name: '团队合作', score: res.data.teamScore, max: 10 }
        ]
      }
    } else {
      ElMessage.error(res.message || '评分失败')
    }
  } catch { ElMessage.error('评分失败') }
  finally { saving.value = false }
}

// ── 下载 ──
function showDownloadDialog() {
  dlVisible.value = true
  loadClasses()
}
async function loadClasses() {
  try {
    const res = await getClassList()
    if (res.code === 200) classOptions.value = res.data || []
  } catch { /* */ }
}
async function loadDlStudents() {
  if (!dlClassId.value) return
  try {
    const res = await getStudents(dlClassId.value)
    if (res.code === 200) dlStudentOptions.value = (res.data || []).map(s => ({ id: s.id, name: s.realName || s.name }))
  } catch { /* */ }
}
async function doDownload() {
  if (dlGenerating.value) return
  if (!dlClassId.value) { ElMessage.warning('请选择班级'); return }
  dlGenerating.value = true
  try {
    const res = await startDownload(taskId, dlClassId.value, dlStudentIds.value.length ? dlStudentIds.value : null)
    if (res.code !== 200) { ElMessage.error(res.message); return }
    const tId = res.data.taskId
    ElMessage.info('正在生成ZIP...')
    pollTimer.value = setInterval(async () => {
      const s = await getDownloadStatus(tId)
      if (s.code === 200 && s.data.status === 'COMPLETED') {
        clearInterval(pollTimer.value)
        dlGenerating.value = false; dlVisible.value = false
        window.open(s.data.result.downloadUrl)
        ElMessage.success('下载完成')
      } else if (s.data?.status === 'FAILED') {
        clearInterval(pollTimer.value)
        dlGenerating.value = false
        ElMessage.error(s.data.error || '生成失败')
      }
    }, 2000)
  } catch { ElMessage.error('下载失败'); dlGenerating.value = false }
}

onMounted(() => load())
onBeforeUnmount(() => { if (pollTimer.value) clearInterval(pollTimer.value) })
</script>

<style scoped>
.tpg-page { max-width: 1400px; margin: 0 auto; padding: 12px; display: flex; flex-direction: column; gap: 12px; min-height: calc(100vh - 64px); }
.tpg-header { display: flex; align-items: center; gap: 12px; }
.tpg-header h3 { margin: 0; flex: 1; }
.tpg-split { display: flex; gap: 12px; flex: 1; min-height: 0; }
.tpg-left { width: 220px; flex-shrink: 0; overflow-y: auto; background: var(--bg-card); border-radius: var(--radius-md); padding: 8px; border: 1px solid var(--border-color); }
.student-row { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border-radius: 6px; cursor: pointer; font-size: var(--fs-sm); }
.student-row:hover { background: var(--bg-section); }
.student-row.active { background: var(--primary-light); font-weight: 500; }
.s-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tpg-right { flex: 1; overflow-y: auto; background: var(--bg-card); border-radius: var(--radius-md); padding: 12px; border: 1px solid var(--border-color); display: flex; flex-direction: column; gap: 10px; }
.right-header { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding-bottom: 8px; border-bottom: 1px solid var(--border-light); }
.rh-name { font-weight: 600; font-size: var(--fs-md); }
.rh-status { font-size: var(--fs-xs); color: var(--text-secondary); }
.grade-step { background: var(--bg-section); border-radius: var(--radius-sm); padding: 10px; }
.gs-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.gs-idx { font-size: var(--fs-sm); font-weight: 500; }
.gs-desc { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 6px; white-space: pre-wrap; }
.grade-images { display: flex; gap: 6px; flex-wrap: wrap; }
.grade-image-item { width: 72px; height: 56px; border-radius: 4px; object-fit: cover; }
.gs-files { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.gs-file { font-size: var(--fs-xs); color: var(--primary-color); text-decoration: none; }
.tpg-empty { flex: 1; }

@media (max-width: 768px) {
  .tpg-split { flex-direction: column; }
  .tpg-left { width: 100%; max-height: 200px; flex-shrink: 1; }
  .tpg-right { flex: 1; }
}

@media (max-width: 768px) {
  .grade-images { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .grade-image-item { width: 100%; aspect-ratio: 1; object-fit: cover; cursor: pointer; border-radius: var(--radius-sm); }
}
</style>
