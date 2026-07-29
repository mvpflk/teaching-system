import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/composables/useIsMobile'
import { useCategoryCascade } from '@/composables/useCategoryCascade'
import { getQuestionBankList, deleteQuestion, updateQuestion, batchClearQuestions, batchDeleteQuestions, approveAiQuestion, rejectAiQuestion, aiReviewQuestions, batchApproveQuestions, batchRejectQuestions } from '@/api/questionBank'
import { getNodeTree } from '@/api/knowledgeNode'
import { getMySubjects } from '@/api/settings'
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes'
import { buildPathMap, getCategoryPath, flattenCategoryTree } from '@/utils/category'
import { downloadFile } from '@/utils/request'

export function useQuestionBank({ autoLoad = true } = {}) {
  const userStore = useUserStore()
  const isAdmin = computed(() => userStore.isAdmin)
  const isTeacher = computed(() => userStore.isTeacher)
  const isSuperAdmin = computed(() => userStore.isSuperAdmin)
  const currentUserId = computed(() => userStore.userInfo?.id || null)
  const { isMobile } = useIsMobile()
  const canEdit = (row) => isAdmin.value || currentUserId.value === row.createdBy

  const CHOICE_TYPES = ['SINGLE_CHOICE', 'MULTI_CHOICE']
  const OPTIONED_TYPES = [...CHOICE_TYPES, 'TRUE_FALSE']

  function needsOptionsReset(from, to) {
    if (from === to) return false
    if (CHOICE_TYPES.includes(from) && to === 'TRUE_FALSE') return true
    if (from === 'TRUE_FALSE' && CHOICE_TYPES.includes(to)) return true
    if (['FILL_IN', 'SHORT_ANSWER', 'ESSAY'].includes(to) && OPTIONED_TYPES.includes(from)) return true
    if (OPTIONED_TYPES.includes(to) && ['FILL_IN', 'SHORT_ANSWER', 'ESSAY'].includes(from)) return 'optional'
    return false
  }

  async function onTypeChange(row, newType) {
    const oldType = row._prevType || row.questionType
    const reset = needsOptionsReset(oldType, newType)
    let confirmMsg = ''
    if (reset === true) {
      confirmMsg = `从"${QUESTION_TYPE_LABEL[oldType]}"改为"${QUESTION_TYPE_LABEL[newType]}"，选项结构不兼容，将清空现有选项。确认修改？`
    } else if (reset === 'optional') {
      confirmMsg = `改为"${QUESTION_TYPE_LABEL[newType]}"需要重新设置选项。确认修改？`
    } else {
      confirmMsg = `确认将题型改为"${QUESTION_TYPE_LABEL[newType]}"？`
    }
    try {
      await ElMessageBox.confirm(confirmMsg, '修改题型', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' })
    } catch {
      row.questionType = oldType
      return
    }
    const payload = { questionType: newType }
    if (reset === true) {
      payload.options = newType === 'TRUE_FALSE' ? JSON.stringify(['正确', '错误']) : ''
    }
    try {
      await updateQuestion(row.id, payload)
      row._prevType = newType
      if (reset === true) row.options = payload.options
      ElMessage.success('已更新')
    } catch { row.questionType = oldType; ElMessage.error('更新失败') }
  }

  const quickUpdate = async (row) => {
    try {
      await updateQuestion(row.id, { questionType: row.questionType, categoryId: row.categoryId })
      ElMessage.success('已更新')
    } catch { ElMessage.error('更新失败') }
  }

  const showSearch = ref(false)
  const categoryTree = ref([])
  const mySubjects = ref([])

  const teacherSubjectIds = computed(() => new Set(mySubjects.value.map(s => Number(s.id)).filter(Boolean)))
  const treeSubjectIds = computed(() => {
    const ids = new Set()
    for (const n of categoryTree.value) {
      if (n.level === 1 && n.subjectId) ids.add(n.subjectId)
    }
    return ids
  })
  const allowedSubjectIds = computed(() => {
    if (teacherSubjectIds.value.size > 0) return teacherSubjectIds.value
    if (treeSubjectIds.value.size > 0) return treeSubjectIds.value
    return new Set()
  })
  const filteredCategoryTree = computed(() => {
    if (isAdmin.value || isSuperAdmin.value) return categoryTree.value
    return categoryTree.value.filter(node => allowedSubjectIds.value.has(node.subjectId))
  })
  const flatCategoryOptions = computed(() => flattenCategoryTree(filteredCategoryTree.value))
  const pathMap = computed(() => buildPathMap(filteredCategoryTree.value))

  const cascade = useCategoryCascade(filteredCategoryTree)
  const { selectedSubjectId: filterSubjectId, selectedChapterId: filterChapterId, selectedTaskId: filterTaskId, selectedKpId: filterKpId, chapters, tasks, kps, categoryId: cascadeCategoryId } = cascade

  const loadCategoryTree = async () => {
    try {
      const res = await getNodeTree()
      if (res.code === 200) categoryTree.value = res.data
    } catch { categoryTree.value = [] }
  }

  const getCategoryPathText = (catId) => getCategoryPath(catId, pathMap.value)
  const clearCategoryFilter = () => { cascade.reset(); pageNum.value = 1; loadList() }
  const onSubjectFilter = (val) => { cascade.onSubjectChange(val); applyCategoryFilter() }
  const onChapterFilter = (val) => { cascade.onChapterChange(val); applyCategoryFilter() }
  const onTaskFilter = (val) => { cascade.onTaskChange(val); applyCategoryFilter() }
  const applyCategoryFilter = () => { pageNum.value = 1; loadList() }

  const loading = ref(false)
  const list = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(20)
  const search = reactive({ questionType: '', keyword: '' })
  const statusTab = ref('1')

  const tableRef = ref(null)
  const selectedIds = ref([])
  const onSelectionChange = (rows) => { selectedIds.value = rows.map(r => r.id) }
  const clearSelection = () => { tableRef.value?.clearSelection(); selectedIds.value = [] }
  const batchDelete = async () => {
    try { await ElMessageBox.confirm('确认删除 ' + selectedIds.value.length + ' 道题目？不可恢复！', '批量删除', { type: 'warning' }) } catch { return }
    try {
      const res = await batchDeleteQuestions(selectedIds.value)
      if (res.code === 200) {
        ElMessage.success('已删除 ' + (res.data?.deleted || selectedIds.value.length) + ' 题')
      }
    } catch { ElMessage.error('批量删除失败') }
    clearSelection(); loadList()
  }

  const loadList = async () => {
    loading.value = true
    try {
      const params = { page: pageNum.value, pageSize: pageSize.value }
      if (cascadeCategoryId.value) params.categoryId = cascadeCategoryId.value
      if (search.questionType) params.questionType = search.questionType
      if (search.keyword) params.keyword = search.keyword
      if (statusTab.value && statusTab.value !== '-1') params.status = Number(statusTab.value)
      const res = await getQuestionBankList(params)
      if (res.code === 200) {
        list.value = (res.data.records || []).map(r => { r._prevType = r.questionType; return r })
        total.value = res.data.total || 0
      }
    } finally { loading.value = false }
  }

  const formVisible = ref(false)
  const editData = ref(null)
  const openCreate = () => { editData.value = null; formVisible.value = true }
  const openEdit = (row) => { editData.value = row; formVisible.value = true }

  const previewVisible = ref(false)
  const preview = ref({})
  const showPreview = (row) => { preview.value = row; previewVisible.value = true }

  const onOperation = async (cmd, row) => {
    if (cmd === 'preview') showPreview(row)
    else if (cmd === 'edit') openEdit(row)
    else if (cmd === 'delete') handleDelete(row)
    else if (cmd === 'approve') await handleApprove(row)
    else if (cmd === 'editApprove') handleEditApprove(row)
    else if (cmd === 'reject') await handleReject(row)
  }

  const handleApprove = async (row) => {
    try {
      const res = await approveAiQuestion(row.id)
      if (res.code === 200) { ElMessage.success('已通过'); await loadList() }
    } catch { ElMessage.error('操作失败') }
  }

  const handleReject = async (row) => {
    try {
      await ElMessageBox.confirm('确定驳回这道题吗？驳回后将从题库删除。', '确认', { type: 'warning' })
      const res = await rejectAiQuestion(row.id)
      if (res.code === 200) { ElMessage.success('已驳回'); await loadList() }
    } catch { /* cancelled */ }
  }

  const reviewDialogVisible = ref(false)
  const reviewing = ref(false)
  const reviewResult = ref(null)

  const editingQuestion = ref(null)
  const editDialogVisible = ref(false)

  const handleEditApprove = (row) => {
    editingQuestion.value = { ...row }
    editDialogVisible.value = true
  }

  const confirmEditApprove = async () => {
    if (!editingQuestion.value) return
    try {
      const updateRes = await updateQuestion(editingQuestion.value.id, {
        ...editingQuestion.value,
        editedByTeacher: 1,
        status: 1
      })
      if (updateRes.code !== 200) { ElMessage.error(updateRes.message || '保存失败'); return }
      await approveAiQuestion(editingQuestion.value.id)
      editDialogVisible.value = false
      ElMessage.success('已修正并通过')
      await loadList()
    } catch { /* request拦截器已显示错误消息 */ }
  }

  const handleDelete = async (row) => {
    try {
      await ElMessageBox.confirm('确定删除这道题吗？', '确认', { type: 'warning' })
      const res = await deleteQuestion(row.id)
      if (res.code === 200) { ElMessage.success('已删除'); await loadList() }
    } catch { /* cancelled */ }
  }

  const handleBatchClear = async () => {
    try {
      await ElMessageBox.confirm('确定要批量清空题库中所有题目吗？此操作不可恢复！', '危险操作', { type: 'error', confirmButtonText: '确认清空' })
      const res = await batchClearQuestions()
      if (res.code === 200) { ElMessage.success(res.message || '已清空'); await loadList() }
    } catch { /* cancelled */ }
  }

  const handleBatchApprove = async () => {
    try {
      await ElMessageBox.confirm(
        '确认批量通过 ' + selectedIds.value.length + ' 道题目？通过后题目将进入"已采用"题库。',
        '批量通过', { type: 'info', confirmButtonText: '确认通过' }
      )
      const res = await batchApproveQuestions(selectedIds.value)
      if (res.code === 200) {
        const data = res.data
        ElMessage.success('已通过 ' + data.approved + ' 题' + (data.failed > 0 ? '，' + data.failed + ' 题失败' : ''))
        clearSelection()
        await loadList()
      }
    } catch { /* cancelled */ }
  }

  const handleBatchReject = async () => {
    try {
      await ElMessageBox.confirm(
        '确认批量驳回 ' + selectedIds.value.length + ' 道题目？驳回后题目将被移除。',
        '批量驳回', { type: 'warning', confirmButtonText: '确认驳回' }
      )
      const res = await batchRejectQuestions(selectedIds.value)
      if (res.code === 200) {
        const data = res.data
        ElMessage.success('已驳回 ' + data.rejected + ' 题' + (data.failed > 0 ? '，' + data.failed + ' 题失败' : ''))
        clearSelection()
        await loadList()
      }
    } catch { /* cancelled */ }
  }

  const handleBatchAiReview = async () => {
    reviewDialogVisible.value = true
    reviewing.value = true
    reviewResult.value = null
    try {
      const res = await aiReviewQuestions({
        questionIds: selectedIds.value,
        autoApprove: false
      })
      if (res.code === 200) {
        reviewResult.value = res.data
      } else {
        ElMessage.error(res.message || 'AI审核失败')
        reviewDialogVisible.value = false
      }
    } catch (e) {
      ElMessage.error('AI审核请求失败')
      reviewDialogVisible.value = false
    } finally {
      reviewing.value = false
    }
  }

  const approveReviewedQuestions = async () => {
    if (!reviewResult.value) return
    const approvedIds = reviewResult.value.results
      .filter(r => r.verdict === 'APPROVED')
      .map(r => r.id)
    if (approvedIds.length === 0) {
      ElMessage.warning('没有可入库的题目')
      return
    }
    try {
      await ElMessageBox.confirm(
        '确认将 ' + approvedIds.length + ' 道通过审核的题目全部入库？',
        '确认入库', { type: 'info' }
      )
      const res = await batchApproveQuestions(approvedIds)
      if (res.code === 200) {
        ElMessage.success('已入库 ' + res.data.approved + ' 题')
        reviewResult.value.autoApproved = res.data.approved
        clearSelection()
        await loadList()
      }
    } catch { /* cancelled */ }
  }

  const importWordVisible = ref(false)
  const openImportWord = () => { importWordVisible.value = true }
  const importExcelVisible = ref(false)
  const openImportExcel = () => { importExcelVisible.value = true }

  const downloadTemplate = (type) => {
    const url = type === 'word'
      ? '/question-bank/actions/template/download'
      : '/question-bank/actions/excel-template/download'
    const filename = type === 'word' ? '题库导入模板.docx' : '题库导入模板.xlsx'
    downloadFile(url, filename).catch(() => ElMessage.error('下载模板失败'))
  }

  const composeVisible = ref(false)
  const composeRef = ref(null)
  const openComposeWizard = () => { composeVisible.value = true }

  const loadMySubjects = async () => {
    try { const r = await getMySubjects(); if (r.code===200) mySubjects.value = r.data||[] } catch { /* */ }
  }

  if (autoLoad) {
    onMounted(() => { loadList(); loadCategoryTree(); loadMySubjects() })
  }

  return {
    isAdmin, isTeacher, isSuperAdmin, currentUserId, isMobile, canEdit,
    categoryTree, mySubjects, filteredCategoryTree, flatCategoryOptions, pathMap,
    filterSubjectId, filterChapterId, filterTaskId, filterKpId,
    chapters, tasks, kps, cascadeCategoryId,
    loading, list, total, pageNum, pageSize, search, statusTab,
    showSearch, tableRef, selectedIds,
    formVisible, editData, previewVisible, preview,
    reviewDialogVisible, reviewing, reviewResult,
    editingQuestion, editDialogVisible,
    importWordVisible, importExcelVisible, composeVisible, composeRef,
    loadList, openCreate, openEdit, showPreview,
    openComposeWizard, openImportWord, openImportExcel, downloadTemplate,
    handleApprove, handleReject, handleBatchClear,
    handleBatchApprove, handleBatchReject, handleBatchAiReview,
    approveReviewedQuestions, handleDelete, onOperation, onTypeChange,
    quickUpdate, onSubjectFilter, onChapterFilter, onTaskFilter,
    clearCategoryFilter, applyCategoryFilter, getCategoryPathText,
    needsOptionsReset, clearSelection, onSelectionChange, batchDelete,
    loadCategoryTree, loadMySubjects,
    QUESTION_TYPE_LABEL, handleEditApprove, confirmEditApprove
  }
}
