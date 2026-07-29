<template>
  <div class="page-card qb-v2">
    <QuestionBankHeader :is-admin="isAdmin" @create="openCreate" @compose="openCompose"
      @download-template="downloadTemplate" @import-word="openImportWord" @import-excel="openImportExcel"
      @clear="handleBatchClear" />

    <div class="qb-v2__body">
      <aside class="qb-v2__tree" :class="{ 'qb-v2__tree--collapsed': treeCollapsed }">
        <div v-show="!treeCollapsed" class="qb-v2__tree-inner">
          <BankTreePanel ref="treePanelRef" v-model:mode="treeMode" :tree="filteredCategoryTree"
            :selected-node-id="listState.categoryId.value" @select="onTreeSelect" />
        </div>
      </aside>

      <!-- 折叠切换按钮 -->
      <button class="qb-v2__tree-toggle" @click="treeCollapsed = !treeCollapsed"
        :title="treeCollapsed ? '展开知识树' : '折叠知识树'">
        {{ treeCollapsed ? '▶' : '◀' }}
      </button>

      <main class="qb-v2__main">
        <el-tabs v-model="listState.statusTab.value" @tab-change="listState.applyFilters()">
          <el-tab-pane label="已采用" name="1" />
          <el-tab-pane label="待审核" name="0" />
          <el-tab-pane label="全部" name="-1" />
        </el-tabs>

        <BankFilterChips :model="listState.filters" @change="onChipsChange" />

        <div class="qb-v2__toolbar">
          <el-input v-model="listState.filters.keyword" placeholder="搜索题干关键词" clearable
            style="width:220px" size="small" @keyup.enter="listState.applyFilters()" @clear="listState.applyFilters()" />
          <el-checkbox v-model="showAnswerAll" size="small">显示答案</el-checkbox>
          <el-checkbox size="small" :model-value="isAllSelected" :indeterminate="isIndeterminate"
            @change="toggleSelectAll">全选本页</el-checkbox>
          <el-button v-if="selectedNodePath" size="small" text type="primary" @click="clearNode">
            {{ selectedNodePath }} ✕
          </el-button>
          <span class="qb-v2__total">共 <b>{{ listState.total.value }}</b> 道题</span>
        </div>

        <div v-if="selectedIds.length" class="batch-bar">
          <span>已选 {{ selectedIds.length }} 题</span>
          <el-button size="small" type="primary" @click="batchAddToBasket">全部入篮</el-button>
          <template v-if="listState.statusTab.value === '0'">
            <el-button size="small" type="primary" @click="handleBatchAiReview">AI智能审核</el-button>
            <el-button size="small" type="success" @click="handleBatchApprove">批量通过</el-button>
            <el-button size="small" type="warning" @click="handleBatchReject">批量驳回</el-button>
          </template>
          <el-button size="small" type="danger" @click="batchDelete">批量删除</el-button>
          <el-button size="small" @click="clearSelection">取消选择</el-button>
        </div>

        <QuestionCardList
          :list="listState.list.value" :loading="listState.loading.value" :error="listState.error.value"
          :total="listState.total.value" v-model:page-num="listState.pageNum.value"
          v-model:page-size="listState.pageSize.value" :usage="listState.usage.value"
          :show-answer-all="showAnswerAll" :category-path-of="getCategoryPathText"
          @page="listState.loadList()" @preview="showPreview" @retry="listState.loadList()">
          <template #card-actions="{ q }">
            <el-checkbox v-if="isAdmin || listState.statusTab.value === '0'" size="small"
              :model-value="selectedIds.includes(q.id)" @change="toggleSelect(q.id)" />
            <el-button v-if="canEdit(q)" text size="small" @click="openEdit(q)">编辑</el-button>
          </template>
        </QuestionCardList>
      </main>
    </div>

    <QuestionFormDialog v-model="formVisible" :category-tree="filteredCategoryTree" :edit-data="editData" @saved="listState.loadList()" />
    <WordImportDialog v-model="importWordVisible" :category-tree="categoryTree" @imported="listState.loadList()" />
    <ExcelImportDialog v-model="importExcelVisible" :category-tree="categoryTree" @imported="listState.loadList()" />
    <ComposeExamWizard v-if="composeVisible" v-model="composeVisible" />
    <QuestionPreviewDialog v-model="previewVisible" :question="preview" @edit="openEdit" />
    <QuestionEditApproveDialog :visible="editDialogVisible" :question="editingQuestion" :type-labels="QUESTION_TYPE_LABEL" @update:visible="editDialogVisible = $event" @confirm="confirmEditApprove" />
    <QuestionReviewDialog :visible="reviewDialogVisible" :reviewing="reviewing" :result="reviewResult" :type-labels="QUESTION_TYPE_LABEL" @update:visible="reviewDialogVisible = $event" @approve-all="approveReviewedQuestions" />

    <BasketFloatBar @open="basketDrawerVisible = true" />
    <BasketDrawer v-model="basketDrawerVisible" @compose="openCompose" @filter-type="onBasketFilterType" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useQuestionBank } from '@/composables/useQuestionBank'
import { useQuestionBankList } from '@/composables/useQuestionBankList'
import { useQuestionBasketStore } from '@/stores/questionBasket'
import { getCategoryPath } from '@/utils/category'
import QuestionBankHeader from './components/QuestionBankHeader.vue'
import QuestionFormDialog from './components/QuestionFormDialog.vue'
import WordImportDialog from './components/WordImportDialog.vue'
import ExcelImportDialog from './components/ExcelImportDialog.vue'
import ComposeExamWizard from './components/ComposeExamWizard.vue'
import QuestionPreviewDialog from './components/QuestionPreviewDialog.vue'
import QuestionEditApproveDialog from './components/QuestionEditApproveDialog.vue'
import QuestionReviewDialog from './components/QuestionReviewDialog.vue'
import BankTreePanel from './components/BankTreePanel.vue'
import BankFilterChips from './components/BankFilterChips.vue'
import QuestionCardList from './components/QuestionCardList.vue'
import BasketFloatBar from '@/components/question-basket/BasketFloatBar.vue'
import BasketDrawer from '@/components/question-basket/BasketDrawer.vue'

const route = useRoute()
const router = useRouter()
const basket = useQuestionBasketStore()
const listState = useQuestionBankList()
const {
  isAdmin, currentUserId, canEdit, categoryTree, mySubjects, filteredCategoryTree, pathMap,
  formVisible, editData, previewVisible, preview, editingQuestion, editDialogVisible,
  reviewDialogVisible, reviewing, reviewResult,
  importWordVisible, importExcelVisible, composeVisible, composeRef,
  openCreate, openEdit, showPreview, downloadTemplate,
  openImportWord, openImportExcel, handleBatchClear,
  handleBatchApprove, handleBatchReject, handleBatchAiReview,
  approveReviewedQuestions, batchDelete,
  loadCategoryTree, loadMySubjects, confirmEditApprove, QUESTION_TYPE_LABEL,
} = useQuestionBank({ autoLoad: false })

// 覆盖 clearSelection：旧版依赖 el-table ref，新版无 tableRef
const clearSelection = () => { selectedIds.value = [] }

const treeMode = ref('chapter')
const treeCollapsed = ref(false)
const treePanelRef = ref(null)
const showAnswerAll = ref(false)
const basketDrawerVisible = ref(false)
const selectedIds = ref([])
const selectedNodePath = ref('')

const getCategoryPathText = (catId) => getCategoryPath(catId, pathMap.value)
const toggleSelect = (id) => {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter(x => x !== id) : [...selectedIds.value, id]
}

/** 全选/取消全选当前页 */
const currentPageIds = computed(() => listState.list.value.map(q => q.id))
const isAllSelected = computed(() => currentPageIds.value.length > 0 && currentPageIds.value.every(id => selectedIds.value.includes(id)))
const isIndeterminate = computed(() => !isAllSelected.value && currentPageIds.value.some(id => selectedIds.value.includes(id)))
const toggleSelectAll = (val) => {
  if (val) {
    for (const id of currentPageIds.value) {
      if (!selectedIds.value.includes(id)) selectedIds.value = [...selectedIds.value, id]
    }
  } else {
    const set = new Set(currentPageIds.value)
    selectedIds.value = selectedIds.value.filter(id => !set.has(id))
  }
}

/** 批量将已选题加入试题篮 */
const batchAddToBasket = () => {
  for (const id of selectedIds.value) basket.add(id)
  ElMessage.success(`已将 ${selectedIds.value.length} 题加入试题篮`)
  clearSelection()
}
const onChipsChange = (next) => { Object.assign(listState.filters, next); selectedIds.value = []; listState.applyFilters() }
const onTreeSelect = (node) => {
  listState.categoryId.value = node?.id || null
  selectedNodePath.value = node?.id ? getCategoryPathText(node.id) : ''
  selectedIds.value = []
  listState.applyFilters()
}
const clearNode = () => { onTreeSelect(null) }

/** 统一组卷入口：从试题篮读取题目，篮空时提示 */
const openCompose = () => {
  if (!basket.count) { ElMessage.warning('请先在题库中选择题目加入试题篮'); return }
  basketDrawerVisible.value = false
  composeVisible.value = true
}
const onBasketFilterType = (type) => {
  listState.filters.questionType = type
  basketDrawerVisible.value = false
  selectedIds.value = []
  listState.applyFilters()
}

/** 筛选条件 ↔ URL query 双向同步（刷新/分享不丢状态） */
const STRING_PARAMS = ['questionType', 'tier', 'knowledgeDim', 'source', 'sort', 'keyword']
const INT_PARAMS = ['difficultyLevel']
const syncToQuery = () => {
  const q = {}
  for (const k of STRING_PARAMS) { if (listState.filters[k]) q[k] = listState.filters[k] }
  for (const k of INT_PARAMS) { if (listState.filters[k]) q[k] = listState.filters[k] }
  if (listState.statusTab.value !== '1') q.status = listState.statusTab.value
  if (listState.categoryId.value) q.cat = listState.categoryId.value
  router.replace({ query: Object.keys(q).length ? q : undefined }).catch(() => {})
}
const syncFromQuery = () => {
  const q = route.query
  for (const k of STRING_PARAMS) { if (q[k]) listState.filters[k] = q[k] }
  for (const k of INT_PARAMS) { if (q[k]) listState.filters[k] = Number(q[k]) }
  if (q.status) listState.statusTab.value = q.status
  if (q.cat) listState.categoryId.value = Number(q.cat)
}

onMounted(async () => {
  // 从 URL 恢复筛选状态（刷新/分享不丢筛选条件）
  syncFromQuery()
  // 并行加载知识树和教师任教科目（两者共同决定 filteredCategoryTree）
  await Promise.all([loadCategoryTree(), loadMySubjects()])
  if (currentUserId.value) basket.init(currentUserId.value)
  // 非管理员默认限定所授学科（与知识树过滤逻辑一致）
  if (!isAdmin.value && mySubjects.value.length === 1) {
    listState.subjectFilter.value = mySubjects.value[0].subjectName || mySubjects.value[0].name || ''
  }
  listState.loadList()
  if (route.query.action === 'compose' && route.query.kpId) {
    const kpId = Number(route.query.kpId)
    listState.categoryId.value = kpId
    treeMode.value = 'kp'
    selectedNodePath.value = getCategoryPathText(kpId)
    selectedIds.value = []
    listState.applyFilters()
    ElMessage.info(`已按薄弱知识点「${route.query.kpName || ''}」筛选`)
  }
})

// 兜底：Pinia 持久化恢复后 userId 才就绪的场景
watch(currentUserId, (uid) => {
  if (uid && !basket.initialized) basket.init(uid)
})

// 筛选条件变更 → 同步到 URL（防抖 300ms，避免频繁 replace）
let _syncTimer = null
watch([() => listState.filters, () => listState.statusTab.value, () => listState.categoryId.value], () => {
  clearTimeout(_syncTimer)
  _syncTimer = setTimeout(syncToQuery, 300)
}, { deep: true })
</script>

<style scoped>
.qb-v2__body { display: flex; gap: 0; }
.qb-v2__tree { width: 240px; flex-shrink: 0; overflow: hidden; max-height: 70vh; transition: width .2s ease; }
.qb-v2__tree--collapsed { width: 0; }
.qb-v2__tree-inner { width: 224px; padding-right: 16px; height: 100%; overflow: auto; }
.qb-v2__tree-toggle { flex-shrink: 0; width: 20px; cursor: pointer; border: none; background: var(--bg-secondary); color: var(--text-secondary); font-size: 10px; display: flex; align-items: center; justify-content: center; padding: 0; transition: background .15s; }
.qb-v2__tree-toggle:hover { background: var(--border-color); color: var(--primary-color); }
.qb-v2__main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 10px; margin-left: 8px; }
.qb-v2__toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.qb-v2__total { margin-left: auto; font-size: var(--fs-xs); color: var(--text-secondary); }
.qb-v2__total b { color: var(--primary-color); }
.batch-bar { display: flex; align-items: center; gap: 10px; padding: 8px 16px; background: var(--bg-secondary); border-radius: var(--radius-md); font-size: var(--fs-sm); }
@media (max-width: 768px) { .qb-v2__tree { display: none; } }
</style>
