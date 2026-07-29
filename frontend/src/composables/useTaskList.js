import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSettingsStore } from '@/stores/settings'
import { useIsMobile } from '@/composables/useIsMobile'
import {
  listTasks, deleteTask, copyTask, publishTask, closeTask,
  clearTaskCache, reopenTask, submitForReview,
} from '@/api/task'
import { TASK_TYPE_FILTER_LABEL, TASK_STATUS_LABEL } from '@/constants/taskType'
import { formatDeadline } from '@/utils/taskUtils'
import { getMySubjects, getGrades } from '@/api/settings'
import { getClassList } from '@/api/classes'

const REVIEW_LABEL = {
  NOT_SUBMITTED: '未提交',
  PENDING_GROUP: '备课组长审',
  PENDING_TEACHING: '教研组长审',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
}

const TASK_ICON_EMOJI = {
  PRE_CLASS: '📖', IN_CLASS: '📝', AFTER_CLASS: '📚',
  FORMATIVE: '📋', SUMMATIVE: '🏆', PRACTICE: '💻',
  MORAL: '❤️', LABOR: '🔧', SURVEY: '📊',
}

const TYPE_TAB_MAP = {
  ALL: '',
  EXAM: 'FORMATIVE,SUMMATIVE',
  HOMEWORK: 'PRE_CLASS,IN_CLASS,AFTER_CLASS,MORAL,LABOR',
  PRACTICE: 'PRACTICE',
  SIMULATION: 'SIMULATION',
  SURVEY: 'SURVEY',
}

export const taskCards = [
  { behavior: 'EXAM', name: '考试/测验', desc: '限时答题、自动评分、防作弊', icon: '📝' },
  { behavior: 'HOMEWORK', name: '作业/任务', desc: '文本提交、迟交扣分、教师评分', icon: '📋' },
  { behavior: 'SURVEY', name: '问卷调查', desc: '匿名作答、自动统计', icon: '📊' },
  { type: 'PRACTICE', name: '实训任务', desc: '步骤化实操、作品提交', isPractice: true, icon: '💻' },
]

export function useTaskList() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()
  const settingsStore = useSettingsStore()
  const { isMobile } = useIsMobile()

  const reviewEnabled = computed(() => settingsStore.isEnabled('feature.review_enabled'))
  const isAdmin = computed(() => userStore.isAdmin)

  const tableRef = ref(null)
  const selectedRows = ref([])
  const loading = ref(false)
  const list = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(20)
  const subjectOptions = ref([])
  const gradeOptions = ref([])
  const classOptions = ref([])

  const filters = reactive({ taskType: '', status: '', grade: '', className: '', search: '' })
  const activeType = ref('ALL')
  const showMobileFilter = ref(false)
  const showCardPicker = ref(false)
  const showShareDialog = ref(false)
  const shareTaskId = ref(null)

  const saveTemplateVisible = ref(false)
  const saveTemplateTarget = ref(null)
  const saveTemplateForm = reactive({ name: '', scope: 'PRIVATE', category: 'TEACHING' })

  const activeFilterCount = computed(() => {
    let n = 0
    if (filters.taskType) n++
    if (filters.status) n++
    if (filters.grade) n++
    if (filters.className) n++
    return n
  })

  const gradeList = computed(() =>
    gradeOptions.value.map((g) => g.gradeName || g.name).filter(Boolean).sort()
  )

  const gradeClassOptions = computed(() => {
    if (!filters.grade) return []
    return classOptions.value
      .filter((c) => c.grade === filters.grade)
      .map((c) => c.className).sort()
  })

  const reviewLabel = (s) => REVIEW_LABEL[s] || s
  const taskIconEmoji = (type) => TASK_ICON_EMOJI[type] || '📋'

  const statusDotClass = (s) => ({ DRAFT: 'info', PUBLISHED: 'warning', ONGOING: 'success', CLOSED: '' }[s] || '')
  const reviewDotClass = (s) => ({
    APPROVED: 'success', PENDING_GROUP: 'warning', PENDING_TEACHING: 'warning',
    REJECTED: 'danger', NOT_SUBMITTED: '',
  }[s] || '')
  const cardVariant = (t) => {
    if (t.pendingGradingCount > 0) return 'warning'
    if (t.status === 'DRAFT' || t.status === 'CLOSED') return 'muted'
    return 'default'
  }

  const onSelectionChange = (rows) => { selectedRows.value = rows }
  const clearSelection = () => {
    tableRef.value?.clearSelection()
    selectedRows.value = []
  }

  const syncFiltersFromQuery = () => {
    const q = route.query
    if (q.taskType) filters.taskType = q.taskType
    if (q.status) filters.status = q.status
    if (q.grade) filters.grade = q.grade
    if (q.className) filters.className = q.className
    if (q.search) filters.search = q.search
    for (const [tab, val] of Object.entries(TYPE_TAB_MAP)) {
      if (val === q.taskType) { activeType.value = tab; break }
    }
  }

  const onTypeTabChange = (name) => {
    filters.taskType = TYPE_TAB_MAP[name] || ''
    search()
  }

  const onSelectType = () => {
    activeType.value = 'ALL'
    search()
  }

  const loadData = async () => {
    loading.value = true
    try {
      const params = { page: pageNum.value, size: pageSize.value }
      if (filters.taskType) params.taskType = filters.taskType
      if (filters.status) params.status = filters.status
      if (filters.search) params.search = filters.search
      if (filters.grade) params.grade = filters.grade
      if (filters.className) params.className = filters.className
      const res = await listTasks(params)
      if (res.code === 200) {
        let records = res.data?.records || []
        if (!filters.taskType) records = records.filter((t) => t.taskType !== 'SIMULATION')
        list.value = records
        total.value = res.data?.total || 0
      }
    } catch { ElMessage.error('加载任务列表失败') }
    finally { loading.value = false }
  }

  const loadOptions = async () => {
    try {
      const [subjRes, gradeRes, classRes] = await Promise.all([
        getMySubjects(), getGrades(), getClassList(),
      ])
      if (subjRes.code === 200) subjectOptions.value = subjRes.data
      if (gradeRes.code === 200) gradeOptions.value = gradeRes.data || []
      if (classRes.code === 200) classOptions.value = classRes.data?.records || classRes.data || []
    } catch { ElMessage.warning('筛选选项加载失败') }
  }

  const search = () => { pageNum.value = 1; loadData() }

  const onPageChange = (page) => { pageNum.value = page; loadData() }

  const reset = () => {
    filters.taskType = ''; filters.status = ''; filters.grade = ''
    filters.className = ''; filters.search = ''
    activeType.value = 'ALL'
    search()
  }

  const pickCard = (card) => {
    showCardPicker.value = false
    if (card.isPractice) router.push('/training/create')
    else router.push({ name: 'TaskCreate', query: { behavior: card.behavior } })
  }

  const handleCardClick = (t) => { if (t.pendingGradingCount > 0) goGrade(t) }
  const openEdit = (row) => router.push({ name: 'TaskEdit', params: { id: row.id } })
  const goGrade = (row) => {
    if (row.taskType === 'PRACTICE') router.push(`/training/${row.id}/grade`)
    else router.push(`/teacher/tasks/${row.id}/grade`)
  }

  const handleShare = (row) => {
    shareTaskId.value = row.id
    showShareDialog.value = true
  }

  const handlePublish = async (row) => {
    try { await ElMessageBox.confirm(`确认发布「${row.title}」？`, '发布任务') } catch { return }
    try {
      const res = await publishTask(row.id)
      if (res.code === 200) { ElMessage.success('已发布'); loadData() }
    } catch { ElMessage.error('发布失败') }
  }

  const handleSubmitReview = async (row) => {
    try {
      await ElMessageBox.confirm(
        `确认提交审核「${row.title}」？\n提交后将由备课组长和教研组长依次审核。`, '提交审核'
      )
    } catch { return }
    try {
      const res = await submitForReview(row.id)
      if (res.code === 200) { ElMessage.success('已提交审核'); loadData() }
      else ElMessage.error(res.message || '提交失败')
    } catch { ElMessage.error('提交失败') }
  }

  const handleClose = async (row) => {
    try { await ElMessageBox.confirm(`确认关闭「${row.title}」？`, '关闭任务') } catch { return }
    try {
      const res = await closeTask(row.id)
      if (res.code === 200) { ElMessage.success('已关闭'); loadData() }
    } catch (e) {
      if (e?.response?.data?.message?.includes('仅已发布') || e?.message?.includes('仅已发布')) {
        ElMessage.info('该任务已被自动关闭'); loadData()
      } else ElMessage.error('关闭失败')
    }
  }

  const handleReopen = async (row) => {
    try { await ElMessageBox.confirm(`重新打开「${row.title}」后可编辑并重新发布？`, '重新打开') } catch { return }
    try {
      const res = await reopenTask(row.id)
      if (res.code === 200) { ElMessage.success('已重新打开为草稿'); loadData() }
    } catch { ElMessage.error('操作失败') }
  }

  const handleCopy = async (row) => {
    try {
      const res = await copyTask(row.id)
      if (res.code === 200) { ElMessage.success(`已复制为「${res.data.title}」，请在草稿中编辑`); loadData() }
    } catch { ElMessage.error('复制失败') }
  }

  const handleDelete = async (row) => {
    try { await ElMessageBox.confirm(`删除「${row.title}」后不可恢复`, '确认删除', { type: 'warning' }) } catch { return }
    try {
      const res = await deleteTask(row.id)
      if (res.code === 200) { ElMessage.success('已删除'); loadData() }
    } catch { ElMessage.error('删除失败') }
  }

  const handleSaveTemplate = (row) => {
    saveTemplateTarget.value = row
    saveTemplateForm.name = row.title
    saveTemplateForm.scope = 'PRIVATE'
    saveTemplateForm.category = 'TEACHING'
    saveTemplateVisible.value = true
  }

  const doSaveTemplate = async () => {
    try {
      const r = await (await import('@/api/taskTemplate')).saveAsTemplate({
        taskId: saveTemplateTarget.value.id,
        name: saveTemplateForm.name,
        scope: saveTemplateForm.scope,
        category: saveTemplateForm.category,
      })
      if (r.code === 200) { ElMessage.success('已保存为模板'); saveTemplateVisible.value = false }
    } catch { ElMessage.error('保存失败') }
  }

  const rowAction = (row) => {
    if (row.status === 'DRAFT') return {
      primary: '编辑', onClick: openEdit,
      items: [
        { cmd: 'publish', label: '发布' },
        ...(reviewEnabled.value && (row.taskType === 'FORMATIVE' || row.taskType === 'SUMMATIVE')
          ? [{ cmd: 'review', label: '提交审核' }] : []),
        { cmd: 'copy', label: '复制' },
        { cmd: 'template', label: '存模板' },
        { cmd: 'delete', label: '删除', divided: true },
      ],
    }
    if (row.status === 'PUBLISHED' || row.status === 'ONGOING') return {
      primary: '批改', onClick: goGrade,
      items: [
        { cmd: 'share', label: '分享' }, { cmd: 'copy', label: '复制' },
        { cmd: 'template', label: '存模板' }, { cmd: 'close', label: '关闭' },
        { cmd: 'delete', label: '删除', divided: true },
      ],
    }
    if (row.status === 'CLOSED') return {
      primary: '查看成绩', onClick: goGrade,
      items: [
        { cmd: 'reopen', label: '重新打开' }, { cmd: 'copy', label: '复制' },
        { cmd: 'template', label: '存模板' }, { cmd: 'delete', label: '删除', divided: true },
      ],
    }
    return { primary: null, items: [] }
  }

  const handleMore = (cmd, row) => ({
    publish: handlePublish, review: handleSubmitReview, close: handleClose,
    reopen: handleReopen, share: handleShare, template: handleSaveTemplate,
    delete: handleDelete, copy: handleCopy,
  })[cmd]?.(row)

  const batchPublish = async () => {
    const drafts = selectedRows.value.filter((r) => r.status === 'DRAFT')
    if (!drafts.length) return
    try { await ElMessageBox.confirm(`确认发布 ${drafts.length} 个任务？`, '批量发布') } catch { return }
    let ok = 0
    for (const r of drafts) { try { const res = await publishTask(r.id); if (res.code === 200) ok++ } catch { /* skip */ } }
    ElMessage.success(`已发布 ${ok}/${drafts.length}`)
    clearSelection(); loadData()
  }

  const batchClose = async () => {
    const active = selectedRows.value.filter((r) => r.status === 'PUBLISHED' || r.status === 'ONGOING')
    if (!active.length) return
    try { await ElMessageBox.confirm(`确认关闭 ${active.length} 个任务？`, '批量关闭') } catch { return }
    let ok = 0
    for (const r of active) { try { const res = await closeTask(r.id); if (res.code === 200) ok++ } catch { /* skip */ } }
    ElMessage.success(`已关闭 ${ok}/${active.length}`)
    clearSelection(); loadData()
  }

  const batchDelete = async () => {
    try { await ElMessageBox.confirm(`确认删除 ${selectedRows.value.length} 个任务？不可恢复！`, '批量删除', { type: 'warning' }) } catch { return }
    let ok = 0
    for (const r of selectedRows.value) { try { const res = await deleteTask(r.id); if (res.code === 200) ok++ } catch { /* skip */ } }
    ElMessage.success(`已删除 ${ok}/${selectedRows.value.length}`)
    clearSelection(); loadData()
  }

  const refresh = async () => {
    try { await clearTaskCache() } catch { /* ignore */ }
    loadData()
  }

  let refreshTimer = null
  onMounted(() => {
    syncFiltersFromQuery()
    loadData()
    loadOptions()
    refreshTimer = setInterval(refresh, 60000)
  })
  onUnmounted(() => { if (refreshTimer) clearInterval(refreshTimer) })

  return {
    reviewEnabled, isAdmin, tableRef, selectedRows, loading, list, total,
    pageNum, pageSize, subjectOptions, filters, activeType, showMobileFilter,
    showCardPicker, showShareDialog, shareTaskId,
    saveTemplateVisible, saveTemplateForm, saveTemplateTarget,
    activeFilterCount, gradeList, gradeClassOptions,
    reviewLabel, taskIconEmoji, statusDotClass, reviewDotClass, cardVariant, formatDeadline,
    onSelectionChange, clearSelection, onTypeTabChange, onSelectType,
    search, reset, onPageChange, pickCard, handleCardClick, openEdit, goGrade,
    handlePublish, handleSubmitReview, handleClose, handleReopen,
    handleCopy, handleDelete, handleSaveTemplate, doSaveTemplate,
    handleShare, rowAction, handleMore,
    batchPublish, batchClose, batchDelete, refresh, TASK_STATUS_LABEL,
  }
}
