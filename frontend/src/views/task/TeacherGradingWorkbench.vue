<template>
  <div class="workbench">
    <WorkbenchHeader :task="task" :graded-count="gradedCount" :total-count="totalCount" :progress-pct="progressPct" @back="router.back()" />
    <div v-loading="loading" class="wb-body">
      <WorkbenchStudentList :students="filteredStudents" :loading="loading" :current-id="current?.id" :keyword="keyword" :search-query="searchQuery"
        :graded-count="gradedCount" :total-count="totalCount" :progress-pct="progressPct" :sub-page="subPage" :sub-total="subTotal" :sub-page-size="subPageSize"
        @select="selectStudent" @update:keyword="onKeywordChange" @update:search-query="searchQuery = $event" @update:sub-page="subPage = $event; load()" />
      <div class="wb-right">
        <div v-if="!current" class="wb-placeholder"><el-empty description="选择左侧学生开始批阅" :image-size="100" /></div>
        <div v-else class="grading-area" @mouseenter="showShortcutHint = true" @mouseleave="showShortcutHint = false">
          <transition name="hint-fade"><div v-if="showShortcutHint" class="ga-shortcut-hint"><span><kbd>&larr;</kbd><kbd>&rarr;</kbd> 切换学生</span><span><kbd>Ctrl+Enter</kbd> 保存评分</span><span><kbd>Esc</kbd> 跳过</span></div></transition>
          <div class="ga-header"><span class="ga-student">{{ current.studentName || current.studentId }}</span><el-tag :type="statusTag(current.status)" size="small">{{ statusLabel(current.status) }}</el-tag><span v-if="current.submittedAt" class="ga-time">提交: {{ fmt(current.submittedAt) }}</span></div>
          <div v-loading="loadingAnswers" class="ga-answers-section">
            <template v-if="answerCount"><div class="ans-summary">共 {{ answerCount }} 题<span v-if="subjectiveCount">，含 {{ subjectiveCount }} 道主观题</span></div><WorkbenchAnswerCard v-for="(a, i) in answers" :key="a.id" :question="a" :index="i" :expanded="expandedAnswer === i" @toggle="expandedAnswer = expandedAnswer === i ? null : i" /></template>
            <div v-if="current.content && !answerCount" class="ga-content">{{ current.content }}</div>
            <div v-if="current.attachments" class="ga-attachments"><div class="ga-label">附件 ({{ parseAttachments(current.attachments).length }})</div><div class="att-grid"><div v-for="(url, i) in parseAttachments(current.attachments)" :key="i" class="att-item"><el-image v-if="isImageFile(url)" :src="url" fit="cover" class="att-img" :preview-src-list="parseAttachments(current.attachments).filter(isImageFile)" /><a v-else :href="url" target="_blank" class="att-link"><el-icon><Document /></el-icon>{{ getFn(url) }}</a></div></div></div>
            <el-empty v-if="!answerCount && !current.content && !current.attachments" description="该学生无作答内容" :image-size="60" />
          </div>
          <WorkbenchGradingControls :score="isStarTask ? starScore : gradeScore" :comment="gradeComment" :explanation="gradeExplanation"
            :max-score="task.totalScore || 100" :is-star-task="isStarTask" :subjective-count="subjectiveCount" :ai-grading="aiGrading"
            :quick-comments="quickComments" :auto-saving="autoSaving" :batch-comment="batchComment" :task-rubric="taskRubric"
            :ungraded-count="ungradedCount" :saving="saving" :batch-saving="batchSaving" @update:score="onScoreUpdate"
            @update:comment="gradeComment = $event" @update:batch-comment="batchComment = $event" @save="saveGrade" @skip="skipStudent"
            @ai-grade="askAiSuggestion" @apply-quick-comment="gradeComment = $event" @manage-quick-comments="quickDialogVisible = true"
            @batch-mark-all="batchMarkAll" @update:rubric-scores="rubricScores = $event" />
          <AiSuggestionCard :suggestion="aiSuggestion" :is-low-confidence="isLowConfidence" @apply="applyAiSuggestion" @ignore="aiSuggestion = null" />
        </div>
      </div>
    </div>
    <div v-if="isMobile" class="wb-mobile-bar" :style="{ paddingBottom: 'var(--safe-bottom)' }">
      <el-button size="large" :disabled="currentIndex <= 0" @click="selectPrevStudent"><el-icon><ArrowLeft /></el-icon></el-button>
      <div class="wb-mobile-bar-center"><span class="wb-mobile-student">{{ current?.studentName || current?.studentId }}</span><el-input-number v-if="current" v-model="current.score" :min="0" :max="100" size="large" controls-position="right" class="wb-mobile-score" placeholder="评分" /></div>
      <el-button size="large" type="primary" :disabled="currentIndex >= filteredStudents.length - 1" @click="selectNextStudent">下一个<el-icon><ArrowRight /></el-icon></el-button>
    </div>
    <QuickCommentManager :visible="quickDialogVisible" :adding="quickAdding" :comments="quickComments" @close="quickDialogVisible = false" @add="handleAddQuick" @delete="handleDeleteQuick" />
  </div>

</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useIsMobile } from '@/composables/useIsMobile'
import { useSubmissionStatus } from '@/composables/useSubmissionStatus'
import { getTask, getTaskSubmissions, gradeSubmission, getSubmissionAnswers, getTaskRubric, gradeRubric } from '@/api/task'
import { getQuickComments, addQuickComment, deleteQuickComment } from '@/api/teacher'
import { submitAiGrading, pollAiTask } from '@/api/ai'
import dayjs from 'dayjs'
import WorkbenchHeader from '@/views/task/components/WorkbenchHeader.vue'
import WorkbenchStudentList from '@/views/task/components/WorkbenchStudentList.vue'
import WorkbenchAnswerCard from '@/views/task/components/WorkbenchAnswerCard.vue'
import WorkbenchGradingControls from '@/views/task/components/WorkbenchGradingControls.vue'
import AiSuggestionCard from '@/views/task/components/AiSuggestionCard.vue'
import QuickCommentManager from '@/views/task/components/QuickCommentManager.vue'

const route = useRoute(), router = useRouter(), taskId = route.params.id
const task = ref({}), taskRubric = ref(null), rubricScores = ref(null)
const students = ref([]), loading = ref(false), current = ref(null)
const searchQuery = ref(''), keyword = ref(''), subPage = ref(1), subPageSize = ref(20), subTotal = ref(0)
let keywordTimer = null
const onKeywordChange = (val) => { keyword.value = val; clearTimeout(keywordTimer); keywordTimer = setTimeout(() => { subPage.value = 1; load() }, 500) }
const gradeScore = ref(0), gradeComment = ref(''), gradeExplanation = ref(''), starScore = ref(0)
const saving = ref(false), autoSaving = ref(false), autoSaveTimer = ref(null), showShortcutHint = ref(false)
let shortcutHintTimer = null
const { isMobile } = useIsMobile()
const { statusLabel, statusTag } = useSubmissionStatus()

const currentIndex = computed(() => { if (!current.value) return -1; return filteredStudents.value.findIndex(s => s.id === current.value.id) })
const selectPrevStudent = () => { const idx = currentIndex.value; if (idx > 0) selectStudent(filteredStudents.value[idx - 1]) }
const selectNextStudent = () => { const idx = currentIndex.value; if (idx < filteredStudents.value.length - 1) selectStudent(filteredStudents.value[idx + 1]) }

const quickComments = ref([]), quickDialogVisible = ref(false), quickAdding = ref(false)
const loadQuickComments = async () => { try { const r = await getQuickComments(); if (r.code === 200) quickComments.value = r.data || [] } catch { } }
const handleAddQuick = async (text) => { quickAdding.value = true; try { const r = await addQuickComment({ commentText: text }); if (r.code === 200) quickComments.value.push(r.data) } catch { ElMessage.error('添加失败') } finally { quickAdding.value = false } }
const handleDeleteQuick = async (id) => { try { const r = await deleteQuickComment(id); if (r.code === 200) quickComments.value = quickComments.value.filter(q => q.id !== id) } catch { ElMessage.error('删除失败') } }

const STAR_TYPES = ['MORAL', 'LABOR']
const isStarTask = computed(() => STAR_TYPES.includes(task.value?.taskType))
const starTexts = ['很差', '较差', '一般', '良好', '优秀']
const totalCount = computed(() => subTotal.value || students.value.length)
const gradedCount = computed(() => students.value.filter(s => s.status === 'GRADED').length)
const progressPct = computed(() => totalCount.value ? Math.round(gradedCount.value / totalCount.value * 100) : 0)
const filteredStudents = computed(() => { const q = (searchQuery.value || '').toLowerCase(); return q ? students.value.filter(s => (s.studentName || '').toLowerCase().includes(q)) : students.value })
const fmt = (t) => t ? dayjs(t).format('MM-DD HH:mm') : '-'
const parseAttachments = (val) => { if (!val) return []; if (Array.isArray(val)) return val; try { return JSON.parse(val) } catch { return [val] } }
const getFn = (url) => { const p = url.split('/'); return p[p.length - 1] || '文件' }
const navigateStudent = (dir) => { if (!current.value) return; const idx = students.value.findIndex(s => s.id === current.value?.id); const range = dir === 'next' ? students.value.slice(idx + 1) : students.value.slice(0, idx).reverse(); const target = range.find(s => s.status !== 'GRADED'); if (target) selectStudent(target) }
const scheduleAutoSave = () => { if (autoSaveTimer.value) clearTimeout(autoSaveTimer.value); autoSaving.value = true; autoSaveTimer.value = setTimeout(() => { if (current.value) saveGrade(true); autoSaving.value = false }, 2000) }

const handleKeydown = (e) => {
  if (!current.value) return; const tag = e.target.tagName; const isInput = tag === 'INPUT' || tag === 'TEXTAREA'
  if (e.key === 'ArrowRight') { e.preventDefault(); navigateStudent('next') }
  else if (e.key === 'ArrowLeft') { e.preventDefault(); navigateStudent('prev') }
  else if (e.key === 'Enter' && !isInput) { e.preventDefault(); saveGrade() }
  else if (e.key === 'Enter' && isInput && e.ctrlKey) { e.preventDefault(); saveGrade() }
  else if (e.key === 'Escape') { e.preventDefault(); skipStudent() }
}

const load = async () => { loading.value = true
  try { const params = { page: subPage.value, size: subPageSize.value }; if (keyword.value) params.keyword = keyword.value
    const [tr, sr] = await Promise.all([getTask(taskId), getTaskSubmissions(taskId, params)])
    if (tr.code === 200) { task.value = tr.data; if (tr.data.rubricId) loadRubric() }
    if (sr.code === 200) { students.value = (sr.data?.records || sr.data || []).filter(s => s.status !== 'EXEMPTED'); subTotal.value = sr.data?.total || 0 }
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const loadRubric = async () => { try { const res = await getTaskRubric(taskId); if (res.code === 200) taskRubric.value = res.data } catch { taskRubric.value = null } }

const answers = ref([]), loadingAnswers = ref(false), expandedAnswer = ref(null)
const loadAnswers = async () => { if (!current.value) return; loadingAnswers.value = true
  try { const r = await getSubmissionAnswers(taskId, current.value.id); if (r.code === 200) answers.value = r.data || []; else answers.value = [] } catch { answers.value = [] } finally { loadingAnswers.value = false }
}
const isImageFile = (url) => /\.(jpg|jpeg|png|gif|bmp|webp|svg)(\?|$)/i.test(url)
const SUBJECTIVE_TYPES = ['SHORT_ANSWER', 'PROGRAMMING', 'FILE_UPLOAD', 'AUDIO_VIDEO', 'ESSAY', 'COMPOSITE']
const isSubjective = (q) => SUBJECTIVE_TYPES.includes(q.questionType)
const answerCount = computed(() => answers.value.length)
const subjectiveCount = computed(() => answers.value.filter(isSubjective).length)

const selectStudent = (s) => { current.value = s; gradeScore.value = s.score || 0; starScore.value = s.score || 0; gradeComment.value = ''; answers.value = []; expandedAnswer.value = null; loadAnswers(); showShortcutHint.value = true; clearTimeout(shortcutHintTimer); shortcutHintTimer = setTimeout(() => { showShortcutHint.value = false }, 3000) }
const skipStudent = () => { if (!current.value) return; const next = students.value.slice(students.value.findIndex(s => s.id === current.value?.id) + 1).find(s => s.status !== 'GRADED'); if (next) selectStudent(next) }

const ungradedCount = computed(() => students.value.filter(s => s.status !== 'GRADED').length)
const batchSaving = ref(false), batchComment = ref('已阅')
const batchMarkAll = async () => { const pending = students.value.filter(s => s.status !== 'GRADED')
  if (!pending.length) { ElMessage.info('所有学生已批阅完成'); return }
  const hasSubj = subjectiveCount.value > 0
  let msg = `将为 ${pending.length} 名未批阅学生填满分并标记「${batchComment.value}」`
  if (hasSubj) msg += '\n⚠️ 该任务含主观题，批量给满分可能不合理，请谨慎操作！'
  try { await ElMessageBox.confirm(msg + '，确认？', '批量批阅', { confirmButtonText: '确定', cancelButtonText: '取消', type: hasSubj ? 'warning' : 'info' }) } catch { return }
  batchSaving.value = true; let ok = 0
  for (const s of pending) { try { await gradeSubmission(taskId, s.id, { comment: batchComment.value, score: task.value.totalScore || 100 }); s.status = 'GRADED'; s.score = task.value.totalScore || 100; ok++ } catch { } }
  batchSaving.value = false; ElMessage.success(`已批量标记 ${ok}/${pending.length} 名`)
}

const onScoreUpdate = (val) => { if (isStarTask.value) starScore.value = val; else gradeScore.value = val; scheduleAutoSave() }
const saveGrade = async (silent) => { if (!current.value || saving.value) return; saving.value = true
  try { const data = { comment: gradeComment.value }; if (gradeExplanation.value) data.explanation = gradeExplanation.value; data.score = isStarTask.value ? starScore.value : gradeScore.value
    const res = await gradeSubmission(taskId, current.value.id, data)
    if (res.code === 200) { if (taskRubric.value && rubricScores.value) { try { await gradeRubric(taskId, current.value.id, rubricScores.value) } catch { } }
      current.value.status = 'GRADED'; current.value.score = isStarTask.value ? starScore.value : gradeScore.value
      if (!silent) ElMessage({ message: '已保存 ✓', type: 'success', duration: 1000 })
      const next = students.value.slice(students.value.findIndex(s => s.id === current.value?.id) + 1).find(s => s.status !== 'GRADED')
      if (next && !silent) selectStudent(next); else if (!silent) ElMessage.info('全部学生已批阅完成') }
  } catch { if (!silent) ElMessage.error('保存失败') } finally { saving.value = false }
}

const aiGrading = ref(false), aiSuggestion = ref(null)
const askAiSuggestion = async () => { if (!current.value) return; aiGrading.value = true; aiSuggestion.value = null
  try { const subId = current.value.id, qId = current.value.questionId || (task.value.questionIds?.[0]); if (!subId) { ElMessage.warning('无法获取提交ID'); return }; const { result } = await pollAiTask(() => submitAiGrading(subId, qId || 0)); aiSuggestion.value = result }
  catch (e) { ElMessage.error(e.message || 'AI服务调用失败') } finally { aiGrading.value = false } }
const isLowConfidence = computed(() => { if (!aiSuggestion.value) return false; const c = aiSuggestion.value.confidence; return c !== undefined && c < 0.85 })
const applyAiSuggestion = () => { if (!aiSuggestion.value) return; const score = aiSuggestion.value.score || aiSuggestion.value.suggestedScore; if (score != null) gradeScore.value = Number(score); const comment = aiSuggestion.value.comment || aiSuggestion.value.comments; if (comment) gradeComment.value = comment; if (aiSuggestion.value.explanation) gradeExplanation.value = aiSuggestion.value.explanation; aiSuggestion.value = null; ElMessage.success('已应用AI建议') }

onMounted(async () => { await load(); loadQuickComments(); window.addEventListener('keydown', handleKeydown); if (route.query.studentId) { await nextTick(); const target = students.value.find(s => s.studentId == route.query.studentId); if (target) selectStudent(target) } })
onUnmounted(() => { window.removeEventListener('keydown', handleKeydown); if (autoSaveTimer.value) clearTimeout(autoSaveTimer.value); clearTimeout(shortcutHintTimer) })
</script>

<style scoped>
.workbench { margin: 0 auto; padding: 16px; }
.wb-body { display: flex; gap: 16px; height: calc(100vh - 200px); }
.wb-right { flex: 1; border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 20px; overflow-y: auto; }
.wb-placeholder { display: flex; align-items: center; justify-content: center; height: 100%; }
.ga-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--border-light); }
.ga-student { font-size: var(--fs-lg); font-weight: 600; }
.ga-time { font-size: var(--fs-xs); color: var(--text-secondary); margin-left: auto; }
.ga-content { background: var(--bg-section); padding: 16px; border-radius: var(--radius-md); font-size: var(--fs-md); line-height: 1.8; white-space: pre-wrap; margin-bottom: 16px; max-height: 300px; overflow-y: auto; }
.ga-attachments { margin-bottom: 16px; }
.ga-label { font-size: var(--fs-sm); color: var(--text-secondary); font-weight: 500; margin-bottom: 4px; }
.ga-answers-section { margin-bottom: 14px; max-height: 380px; overflow-y: auto; border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 12px; background: var(--bg-section); }
.ans-summary { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px dashed var(--border-light); }
.att-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; }
.att-item { flex-shrink: 0; }
.att-img { width: 80px; height: 80px; border-radius: var(--radius-sm); border: 1px solid var(--border-light); cursor: pointer; object-fit: cover; }
.att-link { display: inline-flex; align-items: center; gap: 4px; font-size: var(--fs-xs); color: var(--primary-color); text-decoration: none; margin: 4px 8px 0 0; }
.att-link:hover { text-decoration: underline; }
.ga-shortcut-hint { display: flex; gap: 16px; padding: 8px 14px; margin-bottom: 10px; background: rgba(67, 97, 238, 0.08); border: 0.5px solid rgba(67, 97, 238, 0.15); border-radius: var(--radius-sm); font-size: var(--fs-sm); color: var(--primary-color); align-items: center; justify-content: center; }
.ga-shortcut-hint kbd { display: inline-block; padding: 1px 6px; font-size: var(--fs-xs); font-family: monospace; background: var(--bg-card); border: 0.5px solid var(--border-color); border-radius: 3px; color: var(--text-primary); margin: 0 2px; }
.hint-fade-enter-active { transition: opacity 0.3s ease-out; }
.hint-fade-leave-active { transition: opacity 0.5s ease-in; }
.hint-fade-enter-from, .hint-fade-leave-to { opacity: 0; }
@media (max-width: 768px) { .ga-answers-section { max-height: 280px; } .wb-body { flex-direction: column; height: auto; } .wb-right { padding: 12px; } .ga-header { flex-wrap: wrap; } .att-img { width: 60px; height: 60px; } }
@media (max-width: 768px) { .wb-mobile-bar { position: fixed; bottom: 56px; left: 0; right: 0; z-index: 99; display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: var(--bg-card); border-top: 0.5px solid var(--border-color); } .wb-mobile-bar-center { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; } .wb-mobile-student { font-size: var(--fs-sm); font-weight: 500; color: var(--text-primary); } .wb-mobile-score { width: 110px; } }
</style>
