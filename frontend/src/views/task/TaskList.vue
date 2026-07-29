<template>
  <div class="page-card">
    <TaskListHeader :is-mobile="isMobile" @create="showCardPicker = true" @refresh="refresh" />

    <TaskFilters
      v-model:active-type="activeType"
      v-model:show-mobile-filter="showMobileFilter"
      :filters="filters"
      :is-mobile="isMobile"
      :grade-list="gradeList"
      :grade-class-options="gradeClassOptions"
      :active-filter-count="activeFilterCount"
      @search="search"
      @reset="reset"
      @select-type="onSelectType"
      @type-change="onTypeTabChange"
    />

    <transition name="picker-slide">
      <div v-if="showCardPicker" class="inline-card-picker">
        <div class="inline-card-picker__header">
          <span>选择任务类型</span>
          <el-button text size="small" @click="showCardPicker = false">✕ 关闭</el-button>
        </div>
        <div class="card-grid">
          <div v-for="card in taskCards" :key="card.behavior || card.type" class="task-card-item" @click="pickCard(card)">
            <div class="card-icon-emoji">{{ card.icon }}</div>
            <div class="card-name">{{ card.name }}</div>
            <div class="card-desc">{{ card.desc }}</div>
          </div>
        </div>
      </div>
    </transition>

    <TaskBatchBar
      :selected-rows="selectedRows"
      @batch-publish="batchPublish"
      @batch-close="batchClose"
      @batch-delete="batchDelete"
      @clear-selection="clearSelection"
    />

    <template v-if="isMobile">
      <div v-loading="loading" class="card-list">
        <el-empty v-if="!list.length" description="暂无任务" />
        <MobileDataCard
          v-for="t in list" :key="t.id"
          :title="t.title" :icon="taskIconEmoji(t.taskType)"
          :badge="t.pendingGradingCount > 0 ? { text: t.pendingGradingCount + '待批', type: 'danger' } : null"
          :variant="cardVariant(t)"
          :meta-items="[ TASK_STATUS_LABEL[t.status] || t.status, t.subject, [t.grade, t.className].filter(Boolean).join(' ') ].filter(Boolean)"
          @click="handleCardClick(t)"
        >
          <template #footer>
            <span>截止: {{ formatDeadline(t.deadline) }}</span>
            <span v-if="t.pendingGradingCount > 0" class="tc-pending" @click.stop="goGrade(t)">{{ t.pendingGradingCount }} 份待批阅</span>
          </template>
          <template v-if="t.isOwner || isAdmin" #actions>
            <el-button v-if="t.status === 'DRAFT'" size="small" @click.stop="openEdit(t)">编辑</el-button>
            <el-button v-if="t.status === 'DRAFT'" size="small" type="success" @click.stop="handlePublish(t)">发布</el-button>
            <el-button v-if="t.status === 'PUBLISHED' || t.status === 'ONGOING'" size="small" type="primary" @click.stop="goGrade(t)">批改</el-button>
            <el-button size="small" type="danger" @click.stop="handleDelete(t)">删除</el-button>
          </template>
        </MobileDataCard>
      </div>
    </template>

    <el-table v-else ref="tableRef" v-loading="loading" :data="list" stripe @selection-change="onSelectionChange">
      <template #empty><el-empty description="暂无任务" /></template>
      <el-table-column type="selection" width="40" />
      <el-table-column label="任务标题" min-width="220">
        <template #default="{ row }">
          <div class="title-cell">
            <TaskIcon :type="row.taskType" :size="18" class="title-icon" />
            <div class="title-text">
              <span class="title-main">{{ row.title }}</span>
              <span class="title-sub">{{ row.subject || '' }}<template v-if="row.grade || row.className"> · {{ [row.grade, row.className].filter(Boolean).join(' ') }}</template></span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="status-dot" :class="statusDotClass(row.status)" /><span class="status-text">{{ TASK_STATUS_LABEL[row.status] || row.status }}</span>
        </template>
      </el-table-column>
      <el-table-column label="待批" width="70" align="center">
        <template #default="{ row }">
          <el-badge v-if="row.pendingGradingCount > 0" :value="row.pendingGradingCount" :max="99" class="pending-badge" />
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column v-if="reviewEnabled" label="审核" width="100">
        <template #default="{ row }">
          <span v-if="row.reviewStatus" class="status-dot" :class="reviewDotClass(row.reviewStatus)" />
          <span v-if="row.reviewStatus" class="status-text">{{ reviewLabel(row.reviewStatus) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="截止时间" width="150">
        <template #default="{ row }">{{ formatDeadline(row.deadline) }}</template>
      </el-table-column>
      <el-table-column label="提交" width="80" align="center">
        <template #default="{ row }">{{ row.submittedCount || 0 }}/{{ row.totalStudents || 0 }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-cell">
            <template v-if="row.isOwner || isAdmin">
              <el-button v-if="rowAction(row).primary" text type="primary" size="small" @click="rowAction(row).onClick(row)">{{ rowAction(row).primary }}</el-button>
              <el-dropdown v-if="rowAction(row).items.length" trigger="click" @command="(cmd) => handleMore(cmd, row)">
                <el-button text size="small" class="more-btn">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="item in rowAction(row).items" :key="item.cmd" :command="item.cmd" :divided="item.divided">{{ item.label }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <el-button v-else text size="small" @click="goGrade(row)">查看</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" background @current-change="onPageChange" />
    </div>

    <ShareCodeDialog v-model="showShareDialog" :task-id="shareTaskId" @shared="refresh" />

    <el-dialog v-model="saveTemplateVisible" title="保存为模板" width="420px" append-to-body destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="模板名称"><el-input v-model="saveTemplateForm.name" maxlength="200" /></el-form-item>
        <el-form-item label="用途分类">
          <el-radio-group v-model="saveTemplateForm.category">
            <el-radio value="TEACHING">教学类</el-radio>
            <el-radio value="CLASS_MGMT">班级管理类</el-radio>
            <el-radio value="SCHOOL_NOTICE">全校通知类</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="共享范围">
          <el-radio-group v-model="saveTemplateForm.scope">
            <el-radio value="PRIVATE">仅自己可见</el-radio>
            <el-radio value="LESSON_PREP">共享给备课组</el-radio>
            <el-radio value="TEACHING_GROUP">共享给教研组</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveTemplateVisible = false">取消</el-button>
        <el-button type="primary" @click="doSaveTemplate">确认保存</el-button>
      </template>
    </el-dialog>
  </div>

</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'
import { useIsMobile } from '@/composables/useIsMobile'
import { useTaskList, taskCards } from '@/composables/useTaskList'
import TaskListHeader from './components/TaskListHeader.vue'
import TaskFilters from './components/TaskFilters.vue'
import TaskBatchBar from './components/TaskBatchBar.vue'
import ShareCodeDialog from '@/components/common/ShareCodeDialog.vue'
import TaskIcon from '@/components/common/TaskIcon.vue'
import MobileDataCard from '@/components/common/MobileDataCard.vue'

const { isMobile } = useIsMobile()

const {
  reviewEnabled, isAdmin, tableRef, selectedRows, loading, list, total,
  pageNum, pageSize, filters, activeType, showMobileFilter,
  showCardPicker, showShareDialog, shareTaskId,
  saveTemplateVisible, saveTemplateForm,
  activeFilterCount, gradeList, gradeClassOptions,
  reviewLabel, taskIconEmoji, statusDotClass, reviewDotClass, cardVariant, formatDeadline,
  onSelectionChange, clearSelection, onTypeTabChange, onSelectType,
  search, reset, onPageChange, pickCard, handleCardClick, openEdit, goGrade,
  handlePublish, handleDelete,
  handleSaveTemplate, doSaveTemplate, handleMore, rowAction,
  batchPublish, batchClose, batchDelete, refresh, TASK_STATUS_LABEL,
} = useTaskList()
</script>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.task-card-item {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.task-card-item:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
  transform: translateY(-2px);
  box-shadow: var(--shadow-base);
}
.card-icon-emoji { font-size: 32px; margin-bottom: 6px; }
.card-name { font-size: var(--fs-md); font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.card-desc { font-size: var(--fs-xs); color: var(--text-secondary); }
@media (max-width: 480px) { .card-grid { grid-template-columns: repeat(2, 1fr); } }

.inline-card-picker {
  margin-bottom: 16px;
  border: 2px solid var(--primary-color);
  border-radius: var(--radius-md);
  padding: 16px;
  background: var(--primary-light);
}
.inline-card-picker__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 12px;
}
.inline-card-picker .card-grid { margin: 0; }
.inline-card-picker .task-card-item:hover { background: var(--bg-card); }

.picker-slide-enter-active, .picker-slide-leave-active { transition: all 0.25s ease; overflow: hidden; }
.picker-slide-enter-from, .picker-slide-leave-to { opacity: 0; max-height: 0; padding-top: 0; padding-bottom: 0; margin-bottom: 0 !important; border-width: 0; }
.picker-slide-enter-to, .picker-slide-leave-from { opacity: 1; max-height: 500px; }

.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  position: sticky;
  top: 0;
  z-index: 10;
  border: 0.5px solid var(--border-color);
}
.batch-count { font-size: var(--fs-sm); font-weight: 600; color: var(--text-primary); margin-right: 8px; }

.mobile-search-bar { display: flex; gap: 8px; margin-bottom: 8px; }
.mobile-search-input { flex: 1; min-width: 0; }
.filter-badge {
  display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px;
  border-radius: 9px; background: var(--el-color-danger); color: #fff; font-size: var(--fs-xs); margin-left: 4px; padding: 0 5px;
}

.title-cell { display: flex; align-items: center; gap: 10px; }
.title-icon { flex-shrink: 0; }
.title-text { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.title-main { font-size: var(--fs-base); color: var(--text-primary); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.title-sub { font-size: var(--fs-xs); color: var(--text-secondary); }

.action-cell { display: flex; align-items: center; justify-content: center; gap: 2px; }
.action-cell .el-button.is-text { font-size: var(--fs-sm); padding: 4px 6px; height: auto; }
.action-cell .more-btn { color: var(--text-secondary); }
.action-cell .more-btn:hover { color: var(--primary-color); }

.status-text { font-size: var(--fs-sm); color: var(--text-regular); margin-left: 4px; vertical-align: middle; }
.text-muted { font-size: var(--fs-xs); color: var(--text-secondary); }

.card-list { display: flex; flex-direction: column; gap: 12px; }
.tc-pending { font-size: var(--fs-xs); color: var(--el-color-danger); font-weight: 500; cursor: pointer; margin-bottom: 4px; }
.pending-badge { cursor: pointer; }

.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; align-items: center; gap: 16px; }

@media (max-width: 768px) {
  .batch-bar { flex-wrap: wrap; position: sticky; top: 0; z-index: 10; }
  .batch-bar .el-button { font-size: var(--fs-xs); min-height: 32px; }
}
</style>
