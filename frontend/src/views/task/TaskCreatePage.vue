<template>
  <div v-loading="pageLoading" class="page-card task-create-page">
    <div class="page-header">
      <div class="header-left">
        <el-button text class="back-btn" @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回任务列表</el-button>
        <h3 class="page-title">{{ isEdit ? '编辑任务' : '创建任务' }}<span v-if="hasUnsavedChanges && formInitDone" class="unsaved-dot" title="有未保存的修改"></span></h3>
      </div>
      <div class="header-right">
        <span v-if="lastSaved" class="auto-save-hint"><el-icon><Check /></el-icon> 草稿已保存 {{ lastSaved }}</span>
        <span class="key-hint">Ctrl+S</span>
        <span class="key-hint key-hint--publish">Ctrl+Enter 发布</span>
      </div>
    </div>

    <DraftRestoreBanner :visible="draftRestored" @discard="discardDraft" @dismiss="draftRestored = false" />

    <div v-if="pageLoading" class="form-skeleton">
      <div class="sk-section"><div class="sk-hdr"></div><div class="sk-line w-60"></div><div class="sk-row"><div class="sk-line w-30"></div><div class="sk-line w-65"></div></div><div class="sk-line w-100 h-60"></div></div>
    </div>

    <BehaviorPicker v-if="!form.taskBehavior && !isEdit && !pageLoading" :cards="behaviorCards" @select="selectBehavior" />

    <template v-if="form.taskBehavior || isEdit">
      <MobileForm :steps="createSteps" @finish="handleSave">
        <template #default="{ currentStep, isMobile: isMob }">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="task-form">
            <div v-show="!isMob || currentStep === 0">
              <div class="form-section">
                <div class="form-section__header">
                  <el-icon><InfoFilled /></el-icon> 基本信息
                  <el-tag size="small" :type="behaviorTag" style="margin-left:8px">{{ behaviorLabel }}</el-tag>
                  <el-button v-if="!isEdit" text size="small" style="margin-left:auto" @click="applyPrevConfig">沿用上次配置</el-button>
                  <el-button v-if="!isEdit" text size="small" type="primary" @click="changeBehavior">更换类型</el-button>
                </div>
                <div class="form-section__body">
                  <el-form-item label="任务标题" prop="title"><el-input v-model="form.title" placeholder="请输入任务标题" maxlength="200" /></el-form-item>
                  <el-row :gutter="12">
                    <el-col :xs="24" :sm="8">
                      <el-form-item label="年级" prop="gradeId">
                        <el-select v-model="form.gradeId" placeholder="选择年级" style="width:100%" @change="onGradeFilter">
                          <el-option v-for="g in gradeOptions" :key="g.id" :value="g.id" :label="g.gradeName" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="16">
                      <el-form-item label="目标班级" prop="targetIds">
                        <div class="class-select-wrap">
                          <el-select v-model="form.targetIds" multiple placeholder="可多选班级" style="flex:1" :disabled="!form.gradeId">
                            <el-option v-for="c in filteredClassOptions" :key="c.id" :value="c.id" :label="c.className" />
                          </el-select>
                          <el-button v-if="form.gradeId && filteredClassOptions.length > 1" size="small" text type="primary" style="flex-shrink:0" @click="selectAllClasses">全选</el-button>
                        </div>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="截止时间"><DeadlinePicker v-model="form.deadline" :quick-deadlines="quickDeadlines" /></el-form-item>
                  <el-form-item label="任务描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="输入任务描述（可选）" maxlength="5000" show-word-limit /></el-form-item>
                </div>
              </div>
            </div>
            <div v-show="!isMob || currentStep === 1">
              <TaskExamForm v-if="isExamBehavior" v-model="selectedQuestionIds" v-model:tab="examTab" v-model:subjective-questions="subjectiveQuestions" :form="form" :config="examConfig" />
              <TaskHomeworkForm v-if="isHomeworkBehavior" :form="form" :homework-config="homeworkConfig">
                <template #attachments>
                  <el-form-item label="附件 / 语音">
                    <div class="attach-voice-row">
                      <el-upload :action="UPLOAD_ACTION" :headers="uploadHeaders" name="files" :before-upload="beforeUpload" :on-success="onUploadSuccess" :on-error="onUploadError" :file-list="fileList" list-type="text" multiple style="display:inline-flex;align-items:center">
                        <el-button size="small" plain><el-icon><Plus /></el-icon> 添加附件</el-button>
                      </el-upload>
                      <VoiceRecorder @done="url => { attachmentUrls.value.push(url); ElMessage.success('语音已上传') }" />
                    </div>
                  </el-form-item>
                </template>
              </TaskHomeworkForm>
              <TaskSurveyForm v-if="isSurveyBehavior" :form="form">
                <template #builder><SurveyBuilder :schema="form.surveySchema" @update:schema="v => { form.surveySchema = v }" /></template>
              </TaskSurveyForm>
            </div>
            <div v-show="!isMob || currentStep === 2"></div>
          </el-form>
        </template>
      </MobileForm>
      <TaskCreateSaveBar :saving="saving" :is-edit="isEdit" :is-mobile="isMobile" @save-draft="handleSaveDraft" @cancel="goBack" @publish="handleSave" @publish-continue="handleSaveAndNew('publish-and-new')" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Plus, InfoFilled } from '@element-plus/icons-vue'
import { TASK_BEHAVIOR, TASK_BEHAVIOR_LABEL, TASK_BEHAVIOR_DESC, TASK_BEHAVIOR_ICON, TYPE_TO_BEHAVIOR } from '@/constants/taskType'
import { useTaskForm } from '@/composables/useTaskForm'
import { useAutoSave, autoSave as forceAutoSave } from '@/composables/useAutoSave'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/composables/useIsMobile'
import TaskExamForm from './components/TaskExamForm.vue'
import TaskHomeworkForm from './components/TaskHomeworkForm.vue'
import TaskSurveyForm from './components/TaskSurveyForm.vue'
import VoiceRecorder from '@/components/common/VoiceRecorder.vue'
import SurveyBuilder from '@/components/survey/SurveyBuilder.vue'
import MobileForm from '@/components/common/MobileForm.vue'
import DraftRestoreBanner from './components/DraftRestoreBanner.vue'
import BehaviorPicker from './components/BehaviorPicker.vue'
import DeadlinePicker from './components/DeadlinePicker.vue'
import TaskCreateSaveBar from './components/TaskCreateSaveBar.vue'
import { getMySubjects, getGrades } from '@/api/settings'
import { UPLOAD_ACTION, getUploadHeaders, getTask, createTask, updateTask, publishTask } from '@/api/task'
import { getClassList, getMyClasses } from '@/api/classes'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { isMobile } = useIsMobile()
const createSteps = [{ title: '基本信息' }, { title: '题目配置' }, { title: '发布设置' }]
const pageLoading = ref(false)
const isEdit = computed(() => !!route.params.id)
const editingId = computed(() => route.params.id ? Number(route.params.id) : null)

const subjectOptions = ref([])
const gradeOptions = ref([])
const classOptions = ref([])
const loadOptions = async () => {
  try {
    const classApi = userStore.isAdmin ? getClassList() : getMyClasses()
    const [subjRes, gradeRes, classRes] = await Promise.all([getMySubjects(), getGrades(), classApi])
    if (subjRes.code === 200) subjectOptions.value = subjRes.data
    if (gradeRes.code === 200) gradeOptions.value = gradeRes.data
    if (classRes.code === 200) classOptions.value = (classRes.data?.records || classRes.data || []).map(c => ({ id: c.id, className: c.className, grade: c.grade }))
  } catch {}
}

const { formRef, saving, form, examConfig, homeworkConfig, isExamBehavior, isHomeworkBehavior, isSurveyBehavior, isExamType, rules, filteredClassOptions, onBehaviorChange, onGradeFilter, resetForm, getPayload, validate } = useTaskForm({
  subjectOptions: computed(() => subjectOptions.value),
  gradeOptions: computed(() => gradeOptions.value),
  classOptions: computed(() => classOptions.value),
})

const behaviorCards = [
  { behavior: TASK_BEHAVIOR.EXAM, icon: TASK_BEHAVIOR_ICON.EXAM, name: TASK_BEHAVIOR_LABEL.EXAM, desc: TASK_BEHAVIOR_DESC.EXAM },
  { behavior: TASK_BEHAVIOR.HOMEWORK, icon: TASK_BEHAVIOR_ICON.HOMEWORK, name: TASK_BEHAVIOR_LABEL.HOMEWORK, desc: TASK_BEHAVIOR_DESC.HOMEWORK },
  { behavior: TASK_BEHAVIOR.SURVEY, icon: TASK_BEHAVIOR_ICON.SURVEY, name: TASK_BEHAVIOR_LABEL.SURVEY, desc: TASK_BEHAVIOR_DESC.SURVEY },
]
const behaviorLabel = computed(() => TASK_BEHAVIOR_LABEL[form.taskBehavior] || '')
const behaviorTag = computed(() => isExamBehavior.value ? 'danger' : isHomeworkBehavior.value ? '' : isSurveyBehavior.value ? 'info' : '')
const selectBehavior = (behavior) => { onBehaviorChange(behavior); if (behavior === TASK_BEHAVIOR.EXAM) activeAdvanced.value = ['advanced'] }
const changeBehavior = () => { form.taskBehavior = ''; form.taskType = ''; selectedQuestionIds.value = []; subjectiveQuestions.value = [] }

const quickDeadlines = [
  { label: '明天', minutes: 1440 }, { label: '2天', minutes: 2880 }, { label: '3天', minutes: 4320 },
  { label: '一周', minutes: 10080 }, { label: '两周', minutes: 20160 },
]
const selectAllClasses = () => { form.targetIds = filteredClassOptions.value.map(c => c.id); ElMessage.success(`已选择 ${form.targetIds.length} 个班级`) }

const selectedQuestionIds = ref([])
const subjectiveQuestions = ref([])
const examTab = ref('picker')
const activeAdvanced = ref([])

const ALLOWED_ATTACH = '.jpg,.jpeg,.png,.gif,.bmp,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.mp3,.wav,.ogg,.m4a,.aac,.mp4,.webm,.mov,.avi,.zip,.rar'
const uploadHeaders = getUploadHeaders()
const fileList = ref([])
const attachmentUrls = ref([])
const beforeUpload = (file) => {
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!ALLOWED_ATTACH.includes(ext)) { ElMessage.warning('不支持的文件格式：' + ext); return false }
  if (file.size > 10 * 1024 * 1024) { ElMessage.warning('文件不能超过10MB'); return false }
  return true
}
const onUploadSuccess = (res) => { const url = Array.isArray(res.data) ? res.data[0] : res.data?.url || res.data; if (url) { attachmentUrls.value.push(url); ElMessage.success('上传成功') } }
const onUploadError = () => { ElMessage.error('上传失败') }

const formatDateTime = (val) => {
  if (!val) return ''
  try { const d = val instanceof Date ? val : new Date(val); if (isNaN(d.getTime())) return String(val); const pad = n => String(n).padStart(2, '0'); return `${pad(d.getHours())}:${pad(d.getMinutes())}` } catch { return String(val) }
}
const lastSaved = ref('')
const draftRestored = ref(false)
const autoSaveKey = computed(() => editingId.value ? `task_edit_${editingId.value}` : `task_create_${route.query.behavior || 'default'}`)
const getAutoSaveData = () => ({ form: { ...form }, examConfig: { ...examConfig }, homeworkConfig: { ...homeworkConfig }, selectedQuestionIds: [...selectedQuestionIds.value], subjectiveQuestions: [...subjectiveQuestions.value].map(q => ({ ...q })) })
const { draft, clearDraft: clearAutoSave } = useAutoSave(autoSaveKey, getAutoSaveData, [form, examConfig, homeworkConfig, selectedQuestionIds])
watch(draft, (restored) => {
  if (!restored || isEdit.value) return
  if (restored.form) Object.assign(form, restored.form)
  if (restored.examConfig) Object.assign(examConfig, restored.examConfig)
  if (restored.homeworkConfig) Object.assign(homeworkConfig, restored.homeworkConfig)
  if (restored.selectedQuestionIds) selectedQuestionIds.value = restored.selectedQuestionIds
  if (restored.subjectiveQuestions) subjectiveQuestions.value = restored.subjectiveQuestions
  draftRestored.value = true
}, { once: true })

let saveTimer = null
watch([form, examConfig, homeworkConfig, selectedQuestionIds], () => {
  clearTimeout(saveTimer)
  saveTimer = setTimeout(() => { lastSaved.value = formatDateTime(new Date()) }, 3500)
}, { deep: true })

const handleKeydown = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSaveDraft() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSave() }
}
onMounted(() => document.addEventListener('keydown', handleKeydown))
onUnmounted(() => document.removeEventListener('keydown', handleKeydown))

const buildTaskConfig = () => {
  if (isExamBehavior.value) return JSON.stringify({ ...examConfig, attachmentUrls: attachmentUrls.value.length ? [...attachmentUrls.value] : [] })
  if (attachmentUrls.value.length) { try { const cfg = JSON.parse(getPayload().taskConfig || '{}'); cfg.attachmentUrls = [...attachmentUrls.value]; return JSON.stringify(cfg) } catch { return JSON.stringify({ attachmentUrls: [...attachmentUrls.value] }) } }
  return getPayload().taskConfig || null
}

const handleSave = async () => {
  if (!(await validate())) return
  if (isExamBehavior.value) {
    if (!examConfig.durationMinutes || examConfig.durationMinutes <= 0) { ElMessage.warning('请设置考试时长'); return }
    if (!selectedQuestionIds.value.length) { ElMessage.warning('请至少选择一道题目'); return }
  }
  saving.value = true
  try {
    const base = getPayload()
    const payload = { ...base, questionIds: isExamBehavior.value ? [...selectedQuestionIds.value] : [], taskConfig: buildTaskConfig() }
    let res; let taskIds = []
    if (editingId.value) { res = await updateTask(editingId.value, payload); if (res.code === 200) taskIds = [editingId.value] }
    else { res = await createTask(payload); if (res.code === 200) { const d = res.data; taskIds = Array.isArray(d) ? d.map(t => t.id) : d?.id ? [d.id] : [] } }
    if (res.code === 200) { for (const tid of taskIds) try { await publishTask(tid) } catch { /* */ }; ElMessage.success(editingId.value ? '任务已更新并发布' : '任务已创建并发布'); hasUnsavedChanges.value = false; clearAutoSave(); savePrevConfig(); goBack() }
    else ElMessage.error(res.message || '保存失败')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const handleSaveAndNew = async (cmd) => {
  if (!(await validate())) return
  saving.value = true
  try {
    const base = getPayload()
    const payload = { ...base, questionIds: isExamBehavior.value ? [...selectedQuestionIds.value] : [], taskConfig: buildTaskConfig() }
    const res = await createTask(payload)
    if (res.code === 200) {
      ElMessage.success('任务已创建'); hasUnsavedChanges.value = false; clearAutoSave(); savePrevConfig()
      const lastSubj = form.subject; const lastBehavior = form.taskBehavior
      resetForm(); formInitDone.value = false; selectedQuestionIds.value = []; subjectiveQuestions.value = []; draftRestored.value = false; attachmentUrls.value = []; fileList.value = []; lastSaved.value = ''
      if (lastSubj && subjectOptions.value?.some(s => s.subjectName === lastSubj)) form.subject = lastSubj
      else if (subjectOptions.value?.length) form.subject = subjectOptions.value[0].subjectName
      if (lastBehavior) form.taskBehavior = lastBehavior
      await nextTick(); formInitDone.value = true
    } else ElMessage.error(res.message || '保存失败')
  } catch { ElMessage.error('保存失败') } finally { saving.value = false }
}

const handleSaveDraft = () => { forceAutoSave(autoSaveKey.value, getAutoSaveData()); lastSaved.value = formatDateTime(new Date()); ElMessage.success('草稿已保存') }
const discardDraft = () => { clearAutoSave(); draftRestored.value = false; resetForm(); selectedQuestionIds.value = []; subjectiveQuestions.value = []; attachmentUrls.value = []; fileList.value = []; ElMessage.success('草稿已清除') }

const hasUnsavedChanges = ref(false)
const formInitDone = ref(false)
watch([form, examConfig, homeworkConfig, selectedQuestionIds], () => { if (formInitDone.value) hasUnsavedChanges.value = true }, { deep: true })
onBeforeRouteLeave((to, from, next) => {
  if (hasUnsavedChanges.value) { ElMessageBox.confirm('有未保存的修改，确定离开吗？草稿已自动保存。', '提示', { type: 'warning', confirmButtonText: '离开', cancelButtonText: '留下' }).then(() => next()).catch(() => next(false)) }
  else next()
})

const PREV_CONFIG_KEY = 'task_create_prev_config'
const savePrevConfig = () => { try { localStorage.setItem(PREV_CONFIG_KEY, JSON.stringify({ subject: form.subject, gradeId: form.gradeId, targetIds: [...form.targetIds], taskBehavior: form.taskBehavior })) } catch { /* */ } }
const loadPrevConfig = async () => {
  try { const raw = localStorage.getItem(PREV_CONFIG_KEY); if (!raw) return false; const cfg = JSON.parse(raw); if (cfg.taskBehavior && !form.taskBehavior) selectBehavior(cfg.taskBehavior); if (cfg.subject && subjectOptions.value?.some(s => s.subjectName === cfg.subject)) form.subject = cfg.subject; if (cfg.gradeId && gradeOptions.value?.some(g => g.id === cfg.gradeId)) { form.gradeId = cfg.gradeId; await nextTick(); if (cfg.targetIds?.length) form.targetIds = cfg.targetIds.filter(id => filteredClassOptions.value?.some(c => c.id === id)) }; return true } catch { return false }
}
const applyPrevConfig = async () => { const ok = await loadPrevConfig(); ElMessage[ok ? 'success' : 'info'](ok ? '已沿用上次配置' : '暂无上次配置记录') }
const goBack = () => { router.push('/teacher/tasks') }

const loadTaskData = async (taskId) => {
  pageLoading.value = true
  try {
    const r = await getTask(taskId); if (r.code !== 200 || !r.data) return; const t = r.data
    form.title = t.title || ''; form.taskType = t.taskType || ''; form.scoreType = t.scoreType || 'POINT_100'; form.subject = t.subject || ''
    form.gradeId = t.gradeId || null; form.targetIds = t.targetIds || []; form.description = t.description || ''; form.deadline = t.deadline || ''
    form.totalScore = t.totalScore || 100; form.isRequired = t.isRequired ?? 1; form.notifyParents = t.notifyParents ?? 0
    form.allowResubmit = t.allowResubmit ?? 0; form.autoWrongbook = t.autoWrongbook ?? 1; form.difficultyLevel = t.difficultyLevel || ''
    form.isAnonymous = t.isAnonymous ?? 0; form.surveySchema = t.surveySchema || ''
    const behavior = TYPE_TO_BEHAVIOR[t.taskType]; if (behavior) form.taskBehavior = behavior
    if (t.taskConfig) { try { const cfg = JSON.parse(t.taskConfig); if (TYPE_TO_BEHAVIOR[t.taskType] === TASK_BEHAVIOR.EXAM) Object.assign(examConfig, cfg); else if (TYPE_TO_BEHAVIOR[t.taskType] === TASK_BEHAVIOR.HOMEWORK) Object.assign(homeworkConfig, cfg) } catch { /* */ } }
    if (isExamBehavior.value) { if (t.passRate != null) examConfig.passRate = t.passRate; if (t.maxAttempts != null) examConfig.maxAttempts = t.maxAttempts; if (t.retakeDeadlineHours != null) examConfig.retakeDeadlineHours = t.retakeDeadlineHours }
    if (isExamBehavior.value && t.questionIds?.length) selectedQuestionIds.value = t.questionIds
  } catch { ElMessage.warning('任务数据加载失败') } finally { pageLoading.value = false }
}

onMounted(async () => {
  await loadOptions(); resetForm()
  if (route.query.type === 'PRACTICE') { router.replace('/training/create'); return }
  const behavior = route.query.behavior
  if (behavior && Object.values(TASK_BEHAVIOR).includes(behavior)) selectBehavior(behavior)
  if (!form.subject && subjectOptions.value?.length) form.subject = subjectOptions.value[0].subjectName
  if (!editingId.value && !route.query.behavior) await loadPrevConfig()
  if (editingId.value) { await nextTick(); await loadTaskData(editingId.value) }
  await nextTick(); formInitDone.value = true
})
</script>

<style scoped>
.task-create-page { max-width: 1000px; width: 100%; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 16px; margin-bottom: 16px; border-bottom: 1px solid var(--border-light); flex-wrap: wrap; gap: 8px; }
.header-left, .header-right { display: flex; align-items: center; }
.header-left { gap: 12px; }
.header-right { gap: 4px; }
.back-btn { font-size: var(--fs-sm); color: var(--text-secondary); }
.page-title { margin: 0; font-size: var(--fs-lg); font-weight: 600; color: var(--text-primary); }
.auto-save-hint { font-size: var(--fs-xs); color: var(--el-color-success); display: flex; align-items: center; gap: 4px; }
.key-hint { font-size: var(--fs-xs); color: var(--text-secondary); background: var(--bg-section); border: 1px solid var(--border-light); border-radius: 4px; padding: 1px 6px; margin-left: 8px; font-family: monospace; }
.key-hint--publish { color: var(--primary-color); border-color: var(--primary-color); }
.unsaved-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: var(--el-color-danger); margin-left: 6px; vertical-align: super; animation: dot-pulse 1.5s ease-in-out infinite; }
@keyframes dot-pulse { 0%,100% { opacity: 1 } 50% { opacity: 0.4 } }
.form-section { border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 16px; background: var(--bg-card); }
.form-section__header { display: flex; align-items: center; gap: 8px; font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); padding-bottom: 12px; margin-bottom: 12px; border-bottom: 1px solid var(--border-light); }
.form-section__header .el-icon { font-size: var(--fs-lg); color: var(--primary-color); }
.task-form { --el-form-item-margin-bottom: 12px; }
:deep(.el-form-item) { margin-bottom: 12px; }
:deep(.el-form-item__label) { font-size: var(--fs-sm); padding-bottom: 1px; }
:deep(.switches-row .el-form-item__content) { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; min-height: 32px; }
.class-select-wrap { display: flex; align-items: center; gap: 8px; width: 100%; }
.attach-voice-row { display: flex; align-items: center; gap: 16px; }
.survey-section__hint { font-size: var(--fs-xs); color: var(--text-secondary); margin: 0 0 12px 0; line-height: 1.5; }
.global-score-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; padding: 8px 0 12px; flex-shrink: 0; }
.gsb-label { font-size: var(--fs-xs); font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.gsb-pill { display: inline-flex; align-items: center; gap: 4px; padding: 3px 6px 3px 10px; border-radius: 14px; border: 1px solid var(--border-light); background: var(--bg-card); font-size: var(--fs-xs); color: var(--text-secondary); white-space: nowrap; }
.gsb-pill--other { border-style: dashed; }
.gsb-pill :deep(.el-input-number) { width: 64px; }
.gsb-pill :deep(.el-input-number .el-input__wrapper) { background: var(--bg-section); padding: 0 28px 0 6px; }
.gsb-pill :deep(.el-input-number .el-input__inner) { text-align: center; font-size: var(--fs-xs); }
.gsb-pill :deep(.el-input-number .el-input-number__decrease), .gsb-pill :deep(.el-input-number .el-input-number__increase) { width: 22px; }
:deep(.exam-source-tabs .el-tabs__item) { font-size: var(--fs-sm); padding: 0 14px; height: 32px; line-height: 32px; }
.form-skeleton { padding: 4px 0; }
.sk-section { border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 16px; margin-bottom: 16px; background: var(--bg-card); }
.sk-hdr { height: 18px; width: 120px; border-radius: 4px; background: linear-gradient(90deg,var(--skeleton-bg) 25%,var(--skeleton-highlight) 50%,var(--skeleton-bg) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; margin-bottom: 16px; }
.sk-line { height: 14px; border-radius: 4px; background: linear-gradient(90deg,var(--skeleton-bg) 25%,var(--skeleton-highlight) 50%,var(--skeleton-bg) 75%); background-size: 200% 100%; animation: sk-shimmer 1.5s infinite; margin-bottom: 12px; }
.sk-line.h-60 { height: 60px; }
.sk-row { display: flex; gap: 12px; margin-bottom: 12px; }
.sk-row .sk-line { margin-bottom: 0; }
.w-30 { width: 30%; } .w-60 { width: 60%; } .w-65 { width: 65%; } .w-100 { width: 100%; }
@keyframes sk-shimmer { 0% { background-position: 200% 0 } 100% { background-position: -200% 0 } }
@media (max-width: 768px) {
  .task-create-page { min-width: 0; width: 100%; }
  .page-title { font-size: var(--fs-lg); }
  .page-header { flex-direction: column; align-items: flex-start; gap: 6px; }
  .header-right { width: 100%; justify-content: space-between; }
  :deep(.switches-row .el-form-item__content) { gap: 6px; }
  .global-score-bar { gap: 6px; }
  .gsb-pill { font-size: var(--fs-xs); padding: 2px 4px 2px 8px; }
  .gsb-pill :deep(.el-input-number) { width: 56px; }
}
</style>
