<template>
  <div class="page-card">
    <el-page-header class="mb-16" @back="router.back()">
      <template #breadcrumb>
        <el-breadcrumb separator=">">
          <el-breadcrumb-item :to="{ name: 'StudentTasks' }">我的任务</el-breadcrumb-item>
          <el-breadcrumb-item>{{ task.title || '加载中...' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </template>
      <template #content>
        <div class="detail-header">
          <TaskIcon :type="task.taskType" :size="22" />
          <span class="task-title">{{ task.title || '加载中...' }}</span>
          <span v-if="task.taskType" class="tc-type-label">{{ TASK_TYPE_LABEL[task.taskType] }}</span>
          <el-tag v-if="taskSource" size="small" type="success" effect="plain" style="margin-left:8px">AI 生成</el-tag>
        </div>
      </template>
    </el-page-header>

    <!-- 骨架加载 -->
    <div v-if="loading" class="std-sk">
      <div class="std-sk-row"><div class="std-sk-line w-20"></div><div class="std-sk-line w-15"></div><div class="std-sk-line w-20"></div><div class="std-sk-line w-15"></div></div>
      <div class="std-sk-block"></div>
    </div>

    <div v-if="task.id" class="task-body">
      <div class="task-info">
        <div class="info-item"><span class="label">学科</span><span>{{ task.subject || '通用' }}</span></div>
        <div class="info-item"><span class="label">满分</span><span>{{ task.totalScore }}分</span></div>
        <div class="info-item"><span class="label">截止</span><span :class="{urgent:isUrgent}">{{ formatDeadline(task.deadline) }} <template v-if="task.deadline && isUrgent"><el-icon class="urgent-icon"><WarningFilled /></el-icon> 即将截止</template></span></div>
        <div class="info-item"><span class="label">评分</span><span>{{ SCORE_TYPE_LABEL[task.scoreType]||task.scoreType }}</span></div>
        <div v-if="examMode" class="info-item"><span class="label">试题</span><span>{{ questions.length }}题</span></div>
      </div>

      <div v-if="task.description" class="task-desc">{{ task.description }}</div>
      <div v-if="taskAttachments.length" class="task-attachments">
        <div class="attachment-label"><el-icon class="att-icon"><Paperclip /></el-icon> 教师附件（{{ taskAttachments.length }}个）</div>
        <FilePreview
          v-for="(url, i) in taskAttachments"
          :key="i"
          :src="url"
          :filename="getFileName(url)"
          class="attachment-preview"
        />
      </div>

      <!-- 已提交：展示学生作答内容+附件 -->
      <div v-if="hasSubmitted && (task.content || studentAttachments.length)" class="submitted-block">
        <div class="submitted-label">你的作答</div>
        <div v-if="task.content" class="submitted-content">{{ task.content }}</div>
        <div v-if="studentAttachments.length" class="submitted-attachments">
          <div class="attachment-label"><el-icon class="att-icon"><Paperclip /></el-icon> 你上传的附件（{{ studentAttachments.length }}个）</div>
          <FilePreview
            v-for="(url, i) in studentAttachments"
            :key="i"
            :src="url"
            :filename="getFileName(url)"
            class="attachment-preview"
          />
        </div>
      </div>

      <!-- 已提交状态提示 -->
      <el-alert
        v-if="hasSubmitted"
        title="已提交"
        type="success"
        show-icon
        :closable="false"
        class="mb-16"
      >
        <template #default>
          你的作答已提交，等待教师评分
          <el-button
            v-if="task.allowResubmit"
            size="small"
            type="warning"
            style="margin-left:12px"
            @click="handleResubmit"
          >
            重新提交
          </el-button>
        </template>
      </el-alert>
      <el-alert
        v-if="task.submissionStatus === 'RETURNED'"
        title="教师已退回"
        type="warning"
        show-icon
        :closable="false"
        class="mb-16"
      >
        <template #default>请根据教师反馈修改后重新提交</template>
      </el-alert>

      <!-- 考试模式：先确认后答题 -->
      <div v-if="examMode && !examStarted && !hasSubmitted" class="exam-start-box">
        <div class="exam-start-header">
          <TaskIcon :type="task.taskType" :size="36" />
          <h2 class="exam-start-title">{{ task.title }}</h2>
          <span v-if="task.subject" class="exam-start-subject">{{ task.subject }}</span>
        </div>
        <div class="exam-rules-card">
          <h3 class="exam-rules-heading">📋 在线考试须知</h3>
          <div class="exam-rules">
            <div class="exam-rules-group group-info">
              <div class="exam-rules-group-title">基本信息</div>
              <div class="exam-rule-item"><b>试题数量</b> {{ questions.length }} 题</div>
              <div class="exam-rule-item"><b>满分</b> {{ task.totalScore }} 分</div>
              <div v-if="examDuration" class="exam-rule-item"><b>时长</b> {{ examDuration }} 分钟，到时自动交卷</div>
            </div>
            <div class="exam-rules-group group-integrity">
              <div class="exam-rules-group-title">诚信考试约定</div>
              <div v-if="cheatConfig.fullscreenLock" class="exam-rule-item">建议保持全屏，专注答题</div>
              <div v-if="cheatConfig.disableContextMenu" class="exam-rule-item">右键菜单已关闭</div>
              <div v-if="cheatConfig.disableCopyPaste" class="exam-rule-item">复制粘贴已关闭，请独立作答</div>
              <div v-if="cheatConfig.maxWarnings > 0" class="exam-rule-item">切屏上限 <b>{{ cheatConfig.maxWarnings }} 次</b>，超出自动交卷</div>
              <div v-if="!cheatConfig.fullscreenLock && !cheatConfig.disableContextMenu && !cheatConfig.disableCopyPaste && !cheatConfig.maxWarnings" class="exam-rule-item">本次考试无特殊限制，请自觉遵守考试纪律，独立完成</div>
            </div>
            <div class="exam-rules-group group-config">
              <div class="exam-rules-group-title">题目设置</div>
              <div class="exam-rule-item">题目顺序{{ cheatConfig.shuffleQuestions ? '已随机打乱' : '保持原始顺序' }}</div>
              <div class="exam-rule-item">选项顺序{{ cheatConfig.shuffleOptions ? '已随机打乱' : '保持原始顺序' }}</div>
            </div>
          </div>
        </div>
        <el-button
          type="primary"
          size="large"
          class="start-btn"
          :loading="startingExam"
          @click="startExam"
        >
          <el-icon><Edit /></el-icon> 开始答题
        </el-button>
        <div v-if="fullscreenStatus === 'enabled'" class="fullscreen-status fullscreen-ok">
          <el-icon><SuccessFilled /></el-icon> 全屏模式已开启，祝你答题顺利 💪
        </div>
        <div v-else-if="fullscreenStatus === 'failed'" class="fullscreen-status fullscreen-warn">
          <el-icon><WarningFilled /></el-icon> 全屏模式未能开启，建议手动按 F11 进入全屏以获得更好体验
        </div>
        <div v-else-if="fullscreenStatus === 'unsupported'" class="fullscreen-status fullscreen-warn">
          <el-icon><WarningFilled /></el-icon> 当前浏览器不支持全屏模式，建议使用 Chrome 或 Edge 浏览器
        </div>
      </div>
      <!-- 已评分/已退回：展示成绩卡片 -->
      <TaskResultView
        v-if="task.submissionStatus === 'GRADED' || task.submissionStatus === 'RETURNED'"
        :task="task"
        :submission="submission"
        :is-mobile="isMobile"
        @resubmit="handleResubmit"
      />

      <RelatedCards
        v-if="task.submissionStatus === 'GRADED' || task.submissionStatus === 'RETURNED'"
        :submission-id="submission.id"
      />

      <el-divider v-if="!examMode && !hasSubmitted" />

      <TaskSubmitPanel
        v-if="!hasSubmitted && (!examMode || examStarted)"
        ref="submitPanelRef"
        :task-id="task.id"
        :task-config="task.taskConfig"
        :task-type="task.taskType"
        :survey-schema="task.surveySchema"
        :deadline="task.deadline"
        :exam-started="examMode && examStarted"
        :duration-minutes="examDuration"
        :grading-message="task.gradingMessage"
        :show-answers="examMode"
        :show-content="!examMode"
        :show-attachments="!examMode"
        :questions="questions"
        @submit="handleSubmit"
      />
    </div>

    <!-- 重测对话框 -->
    <el-dialog
      v-model="retakeDialogVisible"
      title="📝 需要重测"
      :width="isMobile ? '92%' : '400px'"
      :close-on-click-modal="false"
      append-to-body
    >
      <p>本次得分 <strong>{{ lastScore }}%</strong>，低于达标线 <strong>{{ passRate }}%</strong></p>
      <p>系统已自动安排重测，还有 <strong>{{ remainingAttempts }}</strong> 次机会提升至达标水平，加油 💪</p>
      <p style="color:var(--text-secondary);font-size:13px;">重测截止：{{ retakeDeadline }}</p>
      <template #footer>
        <el-button @click="retakeDialogVisible = false">稍后再说</el-button>
        <el-button type="primary" @click="startRetake">开始重测 🚀</el-button>
      </template>
    </el-dialog>
  </div>

</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Paperclip, WarningFilled } from '@element-plus/icons-vue'
import { getStudentTaskDetail, getStudentTaskQuestions, submitTask, startStudentExam } from '@/api/task'
import { getTaskSource } from '@/api/agent'
import { TASK_TYPE_LABEL, SCORE_TYPE_LABEL } from '@/constants/taskType'
import { formatDeadline, getDeadlineUrgency } from '@/utils/taskUtils'
import { useIsMobile } from '@/composables/useIsMobile'
import TaskSubmitPanel from '@/components/common/TaskSubmitPanel.vue'
import TaskResultView from '@/components/common/TaskResultView.vue'
import RelatedCards from '@/components/knowledge/RelatedCards.vue'
import FilePreview from '@/components/renderers/FilePreview.vue'
import TaskIcon from '@/components/common/TaskIcon.vue'

const { isMobile } = useIsMobile()

const EXAM_TYPES = ['FORMATIVE', 'SUMMATIVE']

const route = useRoute()
const router = useRouter()
const task = ref({})
const loading = ref(false)
const taskSource = ref(null)
const questions = ref([])
const examStarted = ref(false)
const isUrgent = computed(() => getDeadlineUrgency(task.value.deadline) === 'urgent' || getDeadlineUrgency(task.value.deadline) === 'critical')
const examMode = computed(() => EXAM_TYPES.includes(task.value.taskType))
const hasSubmitted = computed(() => {
  const status = task.value.submissionStatus
  return status && status !== 'PENDING' && status !== 'EXEMPTED' && status !== 'RETURNED'
})
const taskAttachments = computed(() => {
  try { const cfg = JSON.parse(task.value.taskConfig || '{}'); return cfg.attachmentUrls || [] } catch { return [] }
})
const studentAttachments = computed(() => {
  if (!task.value.attachments) return []
  try { return JSON.parse(task.value.attachments) } catch { return task.value.attachments || [] }
})
const getFileName = (url) => { const parts = (url || '').split('/'); return parts[parts.length - 1] || '附件' }
const examDuration = computed(() => {
  if (!task.value.taskConfig) return 0
  try { const cfg = JSON.parse(task.value.taskConfig); return parseInt(cfg.durationMinutes) || 0 } catch { return 0 }
})
const submitPanelRef = ref(null)
const submission = computed(() => ({
  id: task.value.submissionId,
  score: task.value.score,
  scoreJson: task.value.scoreJson,
  status: task.value.submissionStatus,
}))

// ── 重测对话框 ──────────────────────────────────────────
const retakeDialogVisible = ref(false)
const lastScore = ref(0)
const passRate = ref(0)
const remainingAttempts = ref(0)
const retakeDeadline = ref('')

const startRetake = () => {
  retakeDialogVisible.value = false
  // 重新加载页面或跳转到重测页面
  router.push({ name: 'StudentTaskDetail', params: { id: route.params.id }, query: { retake: '1' } })
}

const load = async () => {
  loading.value = true
  try {
    const r = await getStudentTaskDetail(route.params.id)
    if (r.code === 200) task.value = r.data
    if (examMode.value) {
      const qr = await getStudentTaskQuestions(route.params.id)
      if (qr.code === 200) questions.value = qr.data || []
    }
    // A-4: 查询任务是否由 AI 助手生成
    const sr = await getTaskSource(route.params.id)
    if (sr.code === 200 && sr.data.source === 'ai') taskSource.value = sr.data
    // 附录C: 刷新后从 localStorage 恢复 examStarted + examActiveState（D8 前置必修）
    if (examMode.value && task.value.submissionStatus !== 'GRADED') {
      const startKey = `exam_start_${route.params.id}`
      const savedStart = localStorage.getItem(startKey)
      if (savedStart && !Number.isNaN(parseInt(savedStart, 10))) {
        examStarted.value = true
        const { examActiveState } = await import('@/router/index.js')
        examActiveState.active = true
        examActiveState.taskId = route.params.id
      }
    }
  } catch { ElMessage.error('加载任务详情失败') } finally { loading.value = false }
}

const fullscreenStatus = ref('inactive') // 'inactive' | 'enabled' | 'failed' | 'unsupported'
const startingExam = ref(false)
const cheatConfig = computed(() => {
  try {
    const cfg = JSON.parse(task.value.taskConfig || '{}')
    return {
      fullscreenLock: cfg.fullscreenLock === true,
      disableContextMenu: cfg.disableContextMenu === true,
      disableCopyPaste: cfg.disableCopyPaste === true,
      maxWarnings: parseInt(cfg.maxWarnings || cfg.maxCheatWarnings || 0) || 0,
      shuffleQuestions: cfg.shuffleQuestions === true,
      shuffleOptions: cfg.shuffleOptions === true,
    }
  } catch { return {} }
})

const startExam = async () => {
  // 检测是否有之前的 PENDING 记录（可能是中途退出的）
  const hasPending = task.value.submissionStatus === 'PENDING'
  if (hasPending) {
    try {
      await ElMessageBox.confirm(
        '检测到你之前已开始过本次考试但未提交。中途离开可能被记录为异常行为。确定继续答题吗？',
        '继续考试',
        { confirmButtonText: '继续答题', cancelButtonText: '取消', type: 'warning' }
      )
    } catch { return }
  } else {
    try {
      await ElMessageBox.confirm(`共 ${questions.value.length} 题，满分 ${task.value.totalScore} 分。开始后将启用防作弊监控，请确认准备就绪。`, '开始考试', {
        confirmButtonText: '开始答题', cancelButtonText: '再准备一下', type: 'info'
      })
    } catch { return }
  }

  // 全屏锁定必须在 await API 之前执行，保留用户点击手势上下文
  fullscreenStatus.value = 'inactive'
  const fsSupport = !!(document.documentElement.requestFullscreen || document.documentElement.webkitRequestFullscreen)
  if (!fsSupport) {
    fullscreenStatus.value = 'unsupported'
  } else if (cheatConfig.value.fullscreenLock || cheatConfig.value.maxWarnings > 0) {
    try {
      await (document.documentElement.requestFullscreen?.() || Promise.resolve())
      fullscreenStatus.value = 'enabled'
    } catch (e) {
      fullscreenStatus.value = 'failed'
    }
  }

  startingExam.value = true
  try {
    await startStudentExam(route.params.id)
    examStarted.value = true
    localStorage.setItem(`exam_start_${route.params.id}`, Date.now().toString())
    localStorage.removeItem(`cheat_cache_${route.params.id}`)
    // 激活考试状态（防止路由导航离开）
    const { examActiveState } = await import('@/router/index.js')
    examActiveState.active = true
    examActiveState.taskId = route.params.id
  } catch (e) { ElMessage.error(e?.response?.data?.message || e?.message || '开始考试失败，请重试') } finally { startingExam.value = false }
}

const handleSubmit = async (payload) => {
  try {
    const r = await submitTask(route.params.id, payload)
    if (r.code === 200) {
      submitPanelRef.value?.clearDraft()
      // 停用考试状态
      try {
        const { examActiveState } = await import('@/router/index.js')
        examActiveState.active = false
        examActiveState.taskId = null
      } catch { /* ignore */ }
      ElMessage.success('提交成功')

      // 检查是否需要重测
      if (r.data && r.data.attemptNumber > 1 && !r.data.passed) {
        lastScore.value = r.data.score
        passRate.value = r.data.passRate
        remainingAttempts.value = r.data.remainingAttempts
        retakeDeadline.value = r.data.retakeDeadline
        retakeDialogVisible.value = true
        return
      }

      // 考试/试题类型任务 — 引导去错题本
      if (examMode.value || questions.value.length > 0) {
        try {
          await ElMessageBox.confirm(
            '提交成功！是否去错题本查看本次错题并练习？',
            '提交成功',
            { confirmButtonText: '去错题本', cancelButtonText: '稍后再说', type: 'success' }
          )
          router.push({ name: 'WrongBook' })
        } catch {
          router.push({ name: 'StudentTasks' })
        }
      } else {
        router.push({ name: 'StudentTasks' })
      }
    } else {
      ElMessage.error(r.message || '提交失败')
    }
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message
    if (msg && msg.includes('409')) ElMessage.warning('已提交过，请勿重复提交')
    else if (e?.response?.status === 409) ElMessage.warning('已提交过，请勿重复提交')
    else ElMessage.error(msg || '提交失败，请检查网络')
  } finally {
    submitPanelRef.value?.finishSubmit()
  }
}
const handleResubmit = () => {
  task.value.submissionStatus = null
  task.value.score = null
  task.value.scoreJson = null
  // 重新加载任务状态，确保 UI 正确刷新
  load()
  ElMessage.success('已重置提交状态，请重新提交')
}

onMounted(load)

// 组件卸载时清理考试状态
onUnmounted(async () => {
  try {
    // E2: 使用 ESM import 替代 require，避免 Vite 构建报错
    const { examActiveState } = await import('@/router/index.js')
    if (examActiveState.taskId === route.params.id) {
      examActiveState.active = false
      examActiveState.taskId = null
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }

.detail-header { display: flex; align-items: center; gap: 10px; }
.task-title { font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); }
.tc-type-label { font-size: var(--fs-sm); color: var(--text-secondary); background: var(--bg-section); padding: 2px 10px; border-radius: 10px; }

.task-info { display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 16px; }
.info-item { display: flex; flex-direction: column; gap: 2px; }
.info-item .label { font-size: var(--fs-xs); color: var(--text-secondary); }
.info-item span:last-child { font-size: var(--fs-base); color: var(--text-primary); font-weight: 500; }
.info-item .urgent { color: var(--el-color-danger); }
.urgent-icon { font-size: var(--fs-md); vertical-align: -2px; }

.task-desc { background: var(--bg-section); padding: 12px 16px; border-radius: var(--radius-md); font-size: var(--fs-sm); color: var(--text-regular); line-height: 1.8; white-space: pre-wrap; word-break: break-word; }
.submitted-block { background: var(--bg-section); padding: 12px 16px; border-radius: var(--radius-md); margin-bottom: 16px; }
.submitted-label { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.submitted-content { font-size: var(--fs-sm); color: var(--text-regular); white-space: pre-wrap; word-break: break-word; margin-bottom: 12px; }
.submitted-attachments { margin-top: 8px; }
.task-attachments { margin-top: 12px; }
.attachment-label { font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 8px; font-weight: 500; display: flex; align-items: center; gap: 4px; }
.att-icon { font-size: var(--fs-md); }
.attachment-preview { margin-bottom: 8px; }

/* D9: 须知页卡片化 + 视觉层次重构 */
.exam-start-box { text-align: center; padding: 32px 40px; max-width: 760px; margin: 24px auto; background: var(--bg-card); border-radius: var(--radius-xl); box-shadow: var(--shadow-base); border-left: 4px solid var(--primary-color); }
.exam-start-header { display: flex; flex-direction: column; align-items: center; gap: 8px; margin-bottom: 20px; }
.exam-start-title { font-size: var(--fs-xl); font-weight: 700; color: var(--text-primary); margin: 0; }
.exam-start-subject { font-size: var(--fs-sm); color: var(--text-secondary); background: var(--bg-section); padding: 2px 12px; border-radius: 10px; }
/* 考试须知卡片容器 — 替代原 el-alert 黄色框 */
.exam-rules-card { text-align: left; background: var(--bg-section); border-radius: var(--radius-lg); padding: 20px 24px; margin-bottom: 16px; }
.exam-rules-heading { margin: 0 0 16px 0; font-size: var(--fs-base); font-weight: 700; color: var(--text-primary); }
/* 须知分组卡片 — 左侧色条 + 卡片间距加大 */
.exam-rules-group { margin: 12px 0; padding: 14px 18px; background: var(--bg-card); border-radius: var(--radius-md); border-left: 3px solid transparent; }
.exam-rules-group:first-child { margin-top: 0; }
.exam-rules-group:last-child { margin-bottom: 0; }
.group-info { border-left-color: var(--primary-color); }
.group-integrity { border-left-color: var(--el-color-success); }
.group-config { border-left-color: var(--el-color-warning); }
/* V7: 分组标题 — 字号加大 + 明确醒目 */
.exam-rules-group-title { font-weight: 700; color: var(--text-primary); margin-bottom: 10px; font-size: var(--fs-md); }
.start-btn { margin-top: 20px; animation: pulse-ring 2s ease-in-out infinite; }
.fullscreen-status { display: flex; align-items: center; gap: 6px; margin-top: 12px; padding: 8px 12px; border-radius: var(--radius-md); font-size: var(--fs-sm); }
.fullscreen-ok { background: var(--bg-success-light, #e8f8e8); color: var(--el-color-success); }
.fullscreen-warn { background: var(--bg-warning-light, #fdf6ec); color: var(--el-color-warning); }
@keyframes pulse-ring {
  0%, 100% { box-shadow: 0 0 0 0 rgba(var(--primary-color-rgb, 67, 97, 238), 0.3); }
  50% { box-shadow: 0 0 0 10px rgba(var(--primary-color-rgb, 67, 97, 238), 0); }
}
.exam-rules { font-size: var(--fs-sm); color: var(--text-regular); }
.exam-rule-item { margin: 6px 0; line-height: 1.7; color: var(--text-regular); }
.exam-rule-item b { color: var(--text-primary); }

/* 骨架加载 */
.std-sk { padding: 8px 0; }
.std-sk-row { display: flex; gap: 16px; margin-bottom: 16px; }
.std-sk-line { height: 50px; border-radius: 8px; background: linear-gradient(90deg, var(--bg-section, #eee) 25%, var(--bg-card, #f5f5f5) 50%, var(--bg-section, #eee) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; }
.std-sk-block { height: 100px; border-radius: 8px; background: linear-gradient(90deg, var(--bg-section, #eee) 25%, var(--bg-card, #f5f5f5) 50%, var(--bg-section, #eee) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; }
.w-15 { width: 15%; } .w-20 { width: 20%; }
@keyframes sk-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

@media (max-width: 768px) {
  .task-info { gap: 8px; }
  .info-item { min-width: calc(50% - 8px); }
  .task-title { font-size: var(--fs-base); }
  .detail-header { flex-wrap: wrap; }
  /* D9 + M6: 须知页移动端收紧 */
  .exam-start-box { padding: 16px; }
  .exam-rules-card { padding: 14px 16px; }
  .exam-rules-group { padding: 10px 12px; }
  .exam-rule-item { font-size: 13px; margin: 3px 0; }
}
</style>
