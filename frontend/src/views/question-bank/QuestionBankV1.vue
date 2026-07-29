<template>
  <div class="page-card">
    <QuestionBankHeader :is-admin="isAdmin" @create="openCreate" @compose="openCompose" @download-template="downloadTemplate" @import-word="openImportWord" @import-excel="openImportExcel" @clear="handleBatchClear" />

    <el-tabs v-model="statusTab" style="margin-bottom:4px" @tab-change="pageNum=1;loadList()">
      <el-tab-pane label="已采用" name="1" />
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="全部" name="-1" />
    </el-tabs>

    <QuestionBankSearch
      :is-mobile="isMobile" :show-search="showSearch" :category-tree="filteredCategoryTree"
      :chapters="chapters" :tasks="tasks" :kps="kps" :type-labels="QUESTION_TYPE_LABEL"
      :subject-id="filterSubjectId" :chapter-id="filterChapterId" :task-id="filterTaskId"
      :kp-id="filterKpId" :question-type="search.questionType" :keyword="search.keyword"
      @update:subject-id="filterSubjectId = $event" @update:chapter-id="filterChapterId = $event"
      @update:task-id="filterTaskId = $event" @update:kp-id="filterKpId = $event"
      @update:question-type="search.questionType = $event" @update:keyword="search.keyword = $event"
      @subject-change="onSubjectFilter" @chapter-change="onChapterFilter" @task-change="onTaskFilter"
      @apply-filter="applyCategoryFilter" @search="loadList" @clear-filter="clearCategoryFilter"
    />

    <div class="stat-row">
      <span class="stat-chip">共 <b>{{ total }}</b> 道题</span>
      <el-button v-if="isMobile" size="small" text @click="showSearch = !showSearch">
        {{ showSearch ? '收起筛选' : '展开筛选' }}
      </el-button>
    </div>

    <template v-if="isMobile">
      <div v-loading="loading" class="card-list">
        <el-empty v-if="!list.length" description="暂无题目">
          <template #description>
            <p>暂无题目</p>
            <p style="font-size:var(--fs-xs);color:var(--text-secondary)">点击上方「添加题目」或「导入Word」开始</p>
          </template>
        </el-empty>
        <div v-for="q in list" :key="q.id" class="question-card">
          <div class="qc-header">
            <el-tag size="small">{{ QUESTION_TYPE_LABEL[q.questionType] }}</el-tag>
            <el-tag v-if="getQuestionSource(q) === 'BANK'" size="small" type="warning" effect="plain">题库</el-tag>
            <el-tag v-else-if="getQuestionSource(q) === 'ai_generated'" size="small" type="success" effect="plain">AI生成</el-tag>
            <span class="qc-category">{{ getCategoryPathText(q.categoryId) }}</span>
          </div>
          <div class="qc-text">{{ q.questionText?.substring(0, 80) }}{{ q.questionText?.length > 80 ? '...' : '' }}</div>
          <div class="qc-actions">
            <el-button size="small" @click="showPreview(q)">预览</el-button>
            <el-button v-if="canEdit(q)" size="small" @click="openEdit(q)">编辑</el-button>
            <el-button v-if="canEdit(q)" size="small" type="danger" @click="handleDelete(q)">删除</el-button>
          </div>
        </div>
      </div>
    </template>
    <template v-else>
      <div class="qb-table-wrap">
        <div v-if="selectedIds.length" class="batch-bar">
          <span>已选 {{ selectedIds.length }} 题</span>
          <template v-if="statusTab === '0'">
            <el-button size="small" type="primary" @click="handleBatchAiReview"><el-icon><Cpu /></el-icon>AI智能审核</el-button>
            <el-button size="small" type="success" @click="handleBatchApprove">批量通过</el-button>
            <el-button size="small" type="warning" @click="handleBatchReject">批量驳回</el-button>
          </template>
          <el-button size="small" type="danger" @click="batchDelete">批量删除</el-button>
          <el-button size="small" @click="clearSelection">取消选择</el-button>
        </div>
        <el-table ref="tableRef" v-loading="loading" :data="list" stripe size="small" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="40" />
          <el-table-column label="题型" width="100">
            <template #default="{ row }">
              <template v-if="isAdmin">
                <el-select v-model="row.questionType" size="small" style="width:85px" @change="(val) => onTypeChange(row, val)">
                  <el-option v-for="(label, key) in QUESTION_TYPE_LABEL" :key="key" :value="key" :label="label" />
                </el-select>
              </template>
              <template v-else>{{ QUESTION_TYPE_LABEL[row.questionType] }}</template>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="80" align="center">
            <template #default="{ row }">
              <span class="difficulty-stars">{{ '★'.repeat(row.difficultyLevel || 1) }}<span class="difficulty-dim">{{ '☆'.repeat(5 - (row.difficultyLevel || 1)) }}</span></span>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <template v-if="isAdmin">
                <el-select v-model="row.categoryId" size="small" style="width:180px" placeholder="分类" clearable @change="quickUpdate(row)">
                  <el-option v-for="opt in flatCategoryOptions" :key="opt.id" :value="opt.id" :label="opt.name" />
                </el-select>
              </template>
              <el-tooltip v-else :content="getCategoryPathText(row.categoryId)" placement="top" :show-after="500">
                <span class="category-path">{{ getCategoryPathText(row.categoryId) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="questionText" label="题目内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="来源" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="getQuestionSource(row) === 'BANK'" size="small" type="warning" effect="plain">题库</el-tag>
              <el-tag v-else-if="getQuestionSource(row) === 'ai_generated'" size="small" type="success" effect="plain">AI</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="命题者" width="80">
            <template #default="{ row }">{{ row.creatorName || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="70" fixed="right" align="center">
            <template #default="{ row }">
              <el-dropdown trigger="click" @command="(cmd) => onOperation(cmd, row)">
                <el-button size="small" text circle><el-icon><MoreFilled /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="preview"><el-icon><View /></el-icon> 预览</el-dropdown-item>
                    <el-dropdown-item v-if="canEdit(row)" command="edit"><el-icon><Edit /></el-icon> 编辑</el-dropdown-item>
                    <el-dropdown-item v-if="row.status === 0" command="approve"><span style="color:var(--el-color-success)"><el-icon><CircleCheck /></el-icon> 通过</span></el-dropdown-item>
                    <el-dropdown-item v-if="row.status === 0" command="editApprove"><span style="color:var(--el-color-primary)"><el-icon><Edit /></el-icon> 编辑后通过</span></el-dropdown-item>
                    <el-dropdown-item v-if="row.status === 0" command="reject"><span style="color:var(--el-color-danger)"><el-icon><CloseBold /></el-icon> 驳回</span></el-dropdown-item>
                    <el-dropdown-item v-if="canEdit(row)" command="delete" divided><span style="color:var(--el-color-danger)"><el-icon><Delete /></el-icon> 删除</span></el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[10,20,50,100]" :total="total" layout="total, sizes, prev, pager, next, jumper" background small @current-change="loadList" @size-change="loadList" />
        </div>
      </div>
    </template>

    <QuestionFormDialog v-model="formVisible" :category-tree="filteredCategoryTree" :edit-data="editData" @saved="loadList" />
    <WordImportDialog v-model="importWordVisible" :category-tree="categoryTree" @imported="loadList" />
    <ExcelImportDialog v-model="importExcelVisible" :category-tree="categoryTree" @imported="loadList" />
    <ComposeExamWizard v-if="composeVisible" v-model="composeVisible" />
    <QuestionPreviewDialog v-model="previewVisible" :question="preview" @edit="openEdit" />
    <QuestionEditApproveDialog :visible="editDialogVisible" :question="editingQuestion" :type-labels="QUESTION_TYPE_LABEL" @update:visible="editDialogVisible = $event" @confirm="confirmEditApprove" />
    <QuestionReviewDialog :visible="reviewDialogVisible" :reviewing="reviewing" :result="reviewResult" :type-labels="QUESTION_TYPE_LABEL" @update:visible="reviewDialogVisible = $event" @approve-all="approveReviewedQuestions" />
  </div>
</template>

<script setup>
import { useQuestionBank } from '@/composables/useQuestionBank'
import { useQuestionBasketStore } from '@/stores/questionBasket'
import { ElMessage } from 'element-plus'
import QuestionBankHeader from './components/QuestionBankHeader.vue'
import QuestionBankSearch from './components/QuestionBankSearch.vue'
import QuestionFormDialog from './components/QuestionFormDialog.vue'
import WordImportDialog from './components/WordImportDialog.vue'
import ExcelImportDialog from './components/ExcelImportDialog.vue'
import ComposeExamWizard from './components/ComposeExamWizard.vue'
import QuestionPreviewDialog from './components/QuestionPreviewDialog.vue'
import QuestionEditApproveDialog from './components/QuestionEditApproveDialog.vue'
import QuestionReviewDialog from './components/QuestionReviewDialog.vue'

const {
  isAdmin, isMobile, canEdit,
  filteredCategoryTree, flatCategoryOptions, categoryTree,
  filterSubjectId, filterChapterId, filterTaskId, filterKpId,
  chapters, tasks, kps,
  loading, list, total, pageNum, pageSize, search, statusTab,
  showSearch, tableRef, selectedIds,
  formVisible, editData, previewVisible, preview,
  reviewDialogVisible, reviewing, reviewResult,
  editingQuestion, editDialogVisible,
  importWordVisible, importExcelVisible, composeVisible, composeRef,
  loadList, openCreate, openEdit, showPreview,
  openComposeWizard, openImportWord, openImportExcel, downloadTemplate,
  handleBatchClear, handleDelete, onOperation, onTypeChange,
  quickUpdate, onSubjectFilter, onChapterFilter, onTaskFilter,
  clearCategoryFilter, applyCategoryFilter, getCategoryPathText,
  handleBatchApprove, handleBatchReject, handleBatchAiReview,
} = useQuestionBank()

const basket = useQuestionBasketStore()

/** 统一组卷入口：从试题篮读取，篮空时提示 */
const openCompose = () => {
  if (!basket.count) { ElMessage.warning('请先在题库中选择题目加入试题篮'); return }
  composeVisible.value = true
}

const getQuestionSource = (q) => {
  try {
    if (q.contentJson) {
      const cj = typeof q.contentJson === 'string' ? JSON.parse(q.contentJson) : q.contentJson
      return cj.type || ''
    }
  } catch {}
  return ''
}

const {
  approveReviewedQuestions, clearSelection, batchDelete, onSelectionChange,
  handleEditApprove, confirmEditApprove,
  QUESTION_TYPE_LABEL
} = useQuestionBank()
</script>

<style scoped>
.stat-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; align-items: center; }
.stat-chip { background: var(--bg-secondary); padding: 3px 12px; border-radius: var(--radius-lg); font-size: var(--fs-xs); color: var(--text-secondary); }
.stat-chip b { color: var(--primary-color); font-weight: 600; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }
.card-list { display: flex; flex-direction: column; gap: 8px; }
.question-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 12px; }
.qc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.qc-category { font-size: var(--fs-xs); color: var(--text-secondary); }
.qc-text { font-size: var(--fs-sm); color: var(--text-primary); margin-bottom: 8px; line-height: 1.5; }
.qc-actions { display: flex; gap: 8px; justify-content: flex-end; }
.qb-table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.difficulty-stars { font-size: var(--fs-xs); color: #f7ba2a; letter-spacing: 1px; }
.difficulty-dim { color: var(--text-disabled); }
.category-path { display: inline-block; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--fs-xs); color: var(--text-secondary); }
.batch-bar { display: flex; align-items: center; gap: 10px; padding: 8px 16px; background: var(--bg-secondary); border-radius: var(--radius-md); margin-bottom: 8px; font-size: var(--fs-sm); font-weight: 500; border: 0.5px solid var(--border-color); animation: slideDown 0.15s ease-out; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }
</style>
