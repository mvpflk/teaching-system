<template>
  <div v-if="viewMode === 'board'" v-loading="boardLoading" class="submission-board">
    <div class="board-summary">
      <div class="bs-item">
        <span class="bs-num">{{ board.total }}</span><span class="bs-label">班级总人数</span>
      </div>
      <div class="bs-item submitted">
        <span class="bs-num">{{ board.submitted }}</span><span class="bs-label">已提交</span>
      </div>
      <div class="bs-item unsubmitted">
        <span class="bs-num">{{ board.unsubmitted }}</span><span class="bs-label">未提交</span>
      </div>
      <div class="bs-item graded">
        <span class="bs-num">{{ board.graded }}</span><span class="bs-label">已批阅</span>
      </div>
      <div class="bs-item" :class="{ danger: board.cheating > 0 }">
        <span class="bs-num">{{ board.cheating }}</span><span class="bs-label">切屏行为</span>
      </div>
      <div class="bs-item" :class="{ danger: board.terminated > 0 }">
        <span class="bs-num">{{ board.terminated }}</span><span class="bs-label">作弊终止</span>
      </div>
      <div class="bs-item">
        <span class="bs-num">{{ board.avgScore }}</span><span class="bs-label">平均分</span>
      </div>
    </div>
    <div class="board-actions">
      <el-button
        size="small"
        type="warning"
        :disabled="board.unsubmitted === 0"
        :loading="reminding"
        @click="doRemindUnsubmitted"
      >
        提醒未提交学生({{ board.unsubmitted }})
      </el-button>
      <el-button
        size="small"
        type="danger"
        :disabled="board.unsubmitted + board.terminated === 0"
        :loading="restarting"
        @click="doRestartUnfinished"
      >
        一键重启未完成({{ board.unsubmitted + board.terminated }})
      </el-button>
      <el-button
        size="small"
        type="warning"
        :disabled="board.submitted + board.graded === 0"
        @click="$emit('show-batch-regrade')"
      >
        批量更正成绩
      </el-button>
      <div class="board-actions-spacer" />
      <el-select
        v-model="boardStatusFilter"
        placeholder="全部状态"
        clearable
        size="small"
        style="width: 120px"
      >
        <el-option label="未提交" value="NOT_STARTED" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已提交" value="SUBMITTED" />
        <el-option label="已批阅" value="GRADED" />
        <el-option label="作弊终止" value="TERMINATED" />
      </el-select>
    </div>
    <el-empty v-if="!filteredBoardRows.length" description="暂无匹配的学生数据" :image-size="60" />
    <el-table
      v-else
      :data="filteredBoardRows"
      stripe
      border
      max-height="50vh"
      size="small"
      class="board-table"
      @row-click="onBoardRowClick"
    >
      <el-table-column prop="studentNo" label="学号" width="85" />
      <el-table-column prop="studentName" label="姓名" width="90" />
      <el-table-column
        label="状态"
        width="100"
        sortable
        :sort-method="(a, b) => boardStatusOrder(a) - boardStatusOrder(b)"
      >
        <template #default="{ row }">
          <el-tag :type="boardStatusTag(row)" size="small" effect="dark">
            {{
              row.statusLabel
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="分数"
        width="70"
        sortable
        :sort-method="(a, b) => (a.score ?? -1) - (b.score ?? -1)"
      >
        <template #default="{ row }">{{ row.score != null ? row.score : '-' }}</template>
      </el-table-column>
      <el-table-column
        label="提交时间"
        width="145"
        sortable
        :sort-method="(a, b) => (a.submittedAt || '').localeCompare(b.submittedAt || '')"
      >
        <template #default="{ row }">
          {{
            row.submittedAt ? row.submittedAt.substring(0, 16).replace('T', ' ') : '-'
          }}
        </template>
      </el-table-column>
      <el-table-column label="异常" width="85">
        <template #default="{ row }">
          <span v-if="row.cheatTerminated" class="abnormal-danger">作弊终止</span>
          <span
            v-else-if="row.cheatWarnings > 0"
            class="abnormal-warning"
          >切屏{{ row.cheatWarnings }}次</span>
          <span v-else class="abnormal-ok">正常</span>
        </template>
      </el-table-column>
      <el-table-column label="达标状态" width="130" align="center">
        <template #default="{ row }">
          <template v-if="!row.passRate">
            <span style="color: #909399">—</span>
          </template>
          <template v-else>
            <el-tag v-if="row.passed" type="success" size="small">✅ 已达标</el-tag>
            <el-tag v-else-if="row.canRetake" type="warning" size="small">🔄 可再练</el-tag>
            <el-tag v-else type="info" size="small">📌 待巩固</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'SUBMITTED' || row.status === 'GRADED'"
            size="small"
            type="warning"
            :loading="regradingRow === row.studentId"
            @click="$emit('regrade', row)"
          >
            重评
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getSubmissionBoard, remindUnsubmitted, restartUnfinishedStudents } from '@/api/task';

const props = defineProps({
  taskId: { type: [String, Number], required: true },
  isMobile: { type: Boolean, default: false },
  viewMode: { type: String, default: 'submissions' },
  regradingRow: { type: [String, Number, null], default: null },
});

const emit = defineEmits(['regrade', 'refresh', 'show-batch-regrade']);

const router = useRouter();

const board = ref({
  rows: [],
  total: 0,
  submitted: 0,
  unsubmitted: 0,
  graded: 0,
  terminated: 0,
  cheating: 0,
  avgScore: 0,
});
const boardLoading = ref(false);
const reminding = ref(false);
const restarting = ref(false);
const boardStatusFilter = ref('');

const boardStatusOrderMap = {
  NOT_STARTED: 0,
  IN_PROGRESS: 1,
  SUBMITTED: 2,
  GRADED: 3,
  TERMINATED: 4,
};
const boardStatusOrder = (row) => {
  if (row.cheatTerminated) return 4;
  return boardStatusOrderMap[row.status] ?? 5;
};

const filteredBoardRows = computed(() => {
  if (!boardStatusFilter.value) return board.value.rows;
  return board.value.rows.filter((r) => {
    if (boardStatusFilter.value === 'TERMINATED')
      return r.cheatTerminated || r.status === 'TERMINATED';
    return r.status === boardStatusFilter.value;
  });
});

function boardStatusTag(row) {
  if (row.cheatTerminated) return 'danger';
  if (row.status === 'NOT_STARTED' || row.status === 'PENDING') return 'info';
  if (row.status === 'GRADED') return 'success';
  if (row.status === 'SUBMITTED') return 'warning';
  return '';
}

const onBoardRowClick = (row) => {
  router.push(`/teacher/grading/${props.taskId}?studentId=${row.studentId}`);
};

async function loadBoard() {
  boardLoading.value = true;
  try {
    const res = await getSubmissionBoard(props.taskId);
    if (res.code === 200) board.value = res.data;
  } catch {
    /* */
  }
  boardLoading.value = false;
}

async function doRemindUnsubmitted() {
  reminding.value = true;
  try {
    const res = await remindUnsubmitted(props.taskId);
    if (res.code === 200) ElMessage.success(res.message || '已发送提醒');
  } catch {
    ElMessage.error('操作失败');
  }
  reminding.value = false;
}

async function doRestartUnfinished() {
  const count = board.value.unsubmitted + board.value.terminated;
  if (count === 0) return;
  try {
    await ElMessageBox.confirm(
      `将为 ${count} 名未完成学生重置考试权限（切屏终止的学生将被清除作弊记录，进行中学生将被重置计时）。已完成的学生不受影响。确定继续？`,
      '确认一键重启',
      { confirmButtonText: '确定重启', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return;
  }

  restarting.value = true;
  try {
    const res = await restartUnfinishedStudents(props.taskId);
    if (res.code === 200) {
      ElMessage.success(res.message || '操作完成');
      await loadBoard();
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  } catch {
    ElMessage.error('操作失败');
  }
  restarting.value = false;
}

watch(
  () => props.viewMode,
  (v) => {
    if (v === 'board') loadBoard();
    else boardStatusFilter.value = '';
  }
);

defineExpose({ loadBoard });
</script>

<style scoped>
.submission-board {
  margin-bottom: 16px;
}
.board-table :deep(.el-table__body tr) {
  cursor: pointer;
}
.board-table :deep(.el-table__body tr:hover) {
  background: var(--bg-hover-light, #f5f7fa) !important;
}
.board-summary {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.bs-item {
  flex: 1;
  min-width: 80px;
  text-align: center;
  padding: 10px 8px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}
.bs-num {
  font-size: 22px;
  font-weight: 700;
  display: block;
  line-height: 1.2;
}
.bs-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.bs-item.submitted .bs-num {
  color: var(--el-color-primary);
}
.bs-item.unsubmitted .bs-num {
  color: var(--el-color-warning);
}
.bs-item.graded .bs-num {
  color: var(--el-color-success);
}
.bs-item.danger .bs-num {
  color: var(--el-color-danger);
}
.board-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.board-actions-spacer {
  flex: 1;
}
.abnormal-danger {
  color: var(--el-color-danger);
  font-weight: 600;
  font-size: var(--fs-xs);
}
.abnormal-warning {
  color: var(--el-color-warning);
  font-size: var(--fs-xs);
}
.abnormal-ok {
  color: var(--el-color-success);
  font-size: var(--fs-xs);
}

@media (max-width: 768px) {
  .board-summary {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }
  .bs-item {
    min-width: 0;
    padding: 8px 6px;
  }
  .bs-num {
    font-size: var(--fs-lg);
  }
  .bs-label {
    font-size: 10px;
  }
  .board-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .board-actions .el-button,
  .board-actions .el-select {
    width: 100%;
  }
  :deep(.el-table .el-table__body-wrapper) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
