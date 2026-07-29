<template>
  <div class="crq-page">
    <div class="crq-header">
      <h2>卡片审核</h2>
      <span v-if="filterStatus === 'PENDING'" class="crq-badge">{{ total }} 张待审核</span>
      <span
        v-else
        class="crq-badge"
        style="background: var(--el-color-success-light-5); color: #166534"
      >{{ total }} 张已通过</span>
      <span class="crq-summary">
        AI评估 {{ evalProgress ? evalProgress.evaluated : '?' }}/{{
          evalProgress ? evalProgress.total : '?'
        }}
        · 待审核 {{ evalProgress ? evalProgress.pending : '?' }}
      </span>
    </div>

    <div v-if="evalProgress && evalProgress.evaluated < evalProgress.total" class="crq-progress">
      <div class="crq-progress-info">
        <span>🔄 AI 评估中...</span>
        <span>{{ evalProgress.evaluated }} / {{ evalProgress.total }} 张已完成</span>
        <span v-if="evalProgress.pending > 0"> · {{ evalProgress.pending }} 张待审核</span>
      </div>
      <el-progress
        :percentage="Math.round((evalProgress.evaluated / evalProgress.total) * 100)"
        :stroke-width="8"
        :show-text="true"
      />
    </div>

    <div v-if="regenProgress && regenProgress.status !== 'idle'" class="crq-progress">
      <div class="crq-progress-info">
        <span v-if="regenProgress.status === 'running'">📝 卡片重生中...</span>
        <span v-else-if="regenProgress.status === 'done'">✅ 卡片重生完成</span>
        <span v-else-if="regenProgress.status === 'error'">❌ 卡片重生失败</span>
        <span>{{ regenProgress.currentBatch || 0 }} / {{ regenProgress.totalBatches || 0 }} 批</span>
        <span>· {{ regenProgress.generated || 0 }} 篇成功</span>
        <span v-if="regenProgress.failed > 0"> · {{ regenProgress.failed }} 篇失败</span>
      </div>
      <el-progress
        v-if="regenProgress.status === 'running' && regenProgress.totalBatches > 0"
        :percentage="Math.round(((regenProgress.currentBatch || 0) / regenProgress.totalBatches) * 100)"
        :stroke-width="8"
        :show-text="true"
        status="success"
      />
    </div>

    <div class="crq-toolbar">
      <el-radio-group v-model="filterStatus" size="small" @change="loadQueue">
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
      </el-radio-group>
      <el-select
        v-model="filterSubject"
        placeholder="按学科筛选"
        clearable
        style="width: 180px"
        @change="loadQueue"
      >
        <el-option
          v-for="s in subjects"
          :key="s.id"
          :label="s.name"
          :value="s.id"
        />
      </el-select>
      <el-button type="primary" :loading="loading" @click="loadQueue">刷新</el-button>
      <el-button
        type="success"
        :disabled="selectedIds.length === 0"
        @click="batchApprove"
      >
        批量通过 ({{ selectedIds.length }})
      </el-button>
      <el-button
        type="warning"
        :loading="evalLoading"
        @click="triggerBatchEval"
      >
        AI 评估选中
      </el-button>
      <el-button
        type="danger"
        :loading="evalAllLoading"
        @click="triggerEvalAll"
      >
        一键评估全部未评分卡片
      </el-button>
      <el-popconfirm
        :title="regenConfirmTitle"
        confirm-button-text="确定清空"
        cancel-button-text="取消"
        @confirm="triggerRegenAll"
      >
        <template #reference>
          <el-button type="danger" :loading="regenLoading" plain>
            {{ regenButtonText }}
          </el-button>
        </template>
      </el-popconfirm>
    </div>

    <el-empty v-if="!loading && cards.length === 0" description="暂无待审核卡片" />

    <el-table
      v-else
      v-loading="loading"
      :data="cards"
      stripe
      style="width: 100%"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="42" />
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="正面" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="crq-front">{{ row.frontText }}</span>
        </template>
      </el-table-column>
      <el-table-column label="背面" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="crq-back">{{ row.backText }}</span>
        </template>
      </el-table-column>
      <el-table-column label="路径" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.contextPath || '-' }}</template>
      </el-table-column>
      <el-table-column
        prop="qualityScore"
        label="AI评分"
        width="80"
        sortable
      >
        <template #default="{ row }">
          <el-tag :type="scoreType(row.qualityScore)" size="small">
            {{
              row.qualityScore || '-'
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AI建议" width="120">
        <template #default="{ row }">
          <el-popover
            v-if="row.aiComment"
            placement="left"
            :width="360"
            trigger="click"
          >
            <template #reference>
              <el-button size="small" text type="primary">查看建议</el-button>
            </template>
            <div class="crq-ai-detail">
              <p><strong>评分:</strong> {{ row._ai.totalScore || row.qualityScore }}分</p>
              <p><strong>评语:</strong> {{ row._ai.comment }}</p>
              <p v-if="row._ai.suggestion"><strong>建议:</strong> {{ row._ai.suggestion }}</p>
              <div v-if="row._ai.improvedVersion" class="crq-improved">
                <strong>AI 改写版本:</strong>
                <p>{{ row._ai.improvedVersion }}</p>
              </div>
            </div>
          </el-popover>
          <span v-else class="crq-no-ai">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="crq-actions">
            <el-button size="small" type="primary" @click="approveOne(row)">通过</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              @click="rejectOne(row)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="hasImprovedVersion(row)"
              size="small"
              type="success"
              plain
              @click="adoptAndApprove(row)"
            >
              采纳AI
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      class="crq-pager"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      @current-change="loadQueue"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getReviewQueue,
  batchReviewCards,
  batchEvaluateCards,
  batchEvaluateAllCards,
  regenerateAllFlashcards,
  getRegenerationProgress,
  getEvaluationProgress,
  getSubjectsGrouped,
} from '@/api/knowledgeBase';

const cards = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = 20;
const loading = ref(false);
const evalLoading = ref(false);
const evalAllLoading = ref(false);
const evalProgress = ref(null); // { total, evaluated, pending }
const progressTimer = ref(null);
const regenProgress = ref(null); // v169: { status, total, generated, failed, currentBatch, totalBatches }
const regenTimer = ref(null);
const filterStatus = ref('PENDING');
const filterSubject = ref(null);
const selectedIds = ref([]);
const subjects = ref([]);

// v169: 根据学科筛选动态调整提示文案
const regenConfirmTitle = computed(() => {
  if (filterSubject.value) {
    const s = subjects.value.find((x) => x.id === filterSubject.value);
    const name = s?.name || '该学科';
    return `确定要清空「${name}」的所有知识卡片并通过 AI 重新生成吗？此操作不可撤销。`;
  }
  return '确定要清空所有学科的全部知识卡片并通过 AI 重新生成吗？此操作不可撤销。';
});
const regenButtonText = computed(() => {
  if (filterSubject.value) {
    const s = subjects.value.find((x) => x.id === filterSubject.value);
    return `清空并重生「${s?.name || '该学科'}」卡片`;
  }
  return '清空并重生全部卡片';
});

const onSelectionChange = (val) => {
  selectedIds.value = val.map((r) => r.id);
};

const scoreType = (s) => {
  if (s == null) return 'info';
  if (s >= 70) return 'success';
  if (s >= 40) return 'warning';
  return 'danger';
};

const parseComment = (json) => {
  try {
    return JSON.parse(json) || {};
  } catch {
    return {};
  }
};

const hasImprovedVersion = (row) => !!(row._ai?.improvedVersion && row._ai.improvedVersion.trim());

const loadQueue = async () => {
  loading.value = true;
  try {
    const res = await getReviewQueue({
      status: filterStatus.value,
      subjectId: filterSubject.value,
      page: page.value,
      size: pageSize,
    });
    if (res.code === 200) {
      // 预处理：解析 aiComment JSON，避免模板中重复 JSON.parse
      cards.value = (res.data.items || []).map((card) => ({
        ...card,
        _ai: parseComment(card.aiComment),
      }));
      total.value = res.data.total || 0;
    }
  } catch {
    ElMessage.error('加载失败');
  }
  loading.value = false;
};

const approveOne = async (row) => {
  try {
    await batchReviewCards({ cardIds: [row.id], action: 'APPROVED', adoptAiVersion: false });
    ElMessage.success('已通过');
    loadQueue();
  } catch {
    ElMessage.error('操作失败');
  }
};

const rejectOne = async (row) => {
  try {
    await ElMessageBox.confirm('确定拒绝此卡片？拒绝后不再向学生推荐。', '确认', {
      type: 'warning',
    });
    await batchReviewCards({ cardIds: [row.id], action: 'REJECTED', adoptAiVersion: false });
    ElMessage.success('已拒绝');
    loadQueue();
  } catch {
    /* 取消 */
  }
};

const adoptAndApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定用 AI 改写版本替换原内容并审核通过？', '确认', {
      type: 'info',
    });
    await batchReviewCards({ cardIds: [row.id], action: 'APPROVED', adoptAiVersion: true });
    ElMessage.success('已采纳AI改写并通过');
    loadQueue();
  } catch {
    /* 取消 */
  }
};

const batchApprove = async () => {
  if (selectedIds.value.length === 0) return;
  try {
    await ElMessageBox.confirm(`确定批量通过 ${selectedIds.value.length} 张卡片？`, '批量审核', {
      type: 'info',
    });
    await batchReviewCards({
      cardIds: selectedIds.value,
      action: 'APPROVED',
      adoptAiVersion: false,
    });
    ElMessage.success(`已通过 ${selectedIds.value.length} 张`);
    selectedIds.value = [];
    loadQueue();
  } catch {
    /* 取消 */
  }
};

const triggerBatchEval = async () => {
  if (selectedIds.value.length === 0) return ElMessage.warning('请先选择卡片');
  evalLoading.value = true;
  try {
    await batchEvaluateCards({ cardIds: selectedIds.value });
    ElMessage.success(`已提交 ${selectedIds.value.length} 张卡片的AI评估，请稍后刷新查看结果`);
    selectedIds.value = [];
  } catch {
    ElMessage.error('提交失败');
  }
  evalLoading.value = false;
};

const pollProgress = async () => {
  try {
    const res = await getEvaluationProgress();
    if (res.code === 200) evalProgress.value = res.data;
  } catch {
    /* 静默 */
  }
};

const startProgressPolling = () => {
  pollProgress();
  progressTimer.value = setInterval(pollProgress, 3000);
};

const stopProgressPolling = () => {
  if (progressTimer.value) {
    clearInterval(progressTimer.value);
    progressTimer.value = null;
  }
};

const triggerEvalAll = async () => {
  evalAllLoading.value = true;
  try {
    const res = await batchEvaluateAllCards();
    if (res.code === 200) {
      ElMessage.success(res.message || `已提交 ${res.data.submittedCount} 张卡片`);
      startProgressPolling();
    } else {
      ElMessage.info(res.message || '没有需要评估的卡片');
    }
  } catch {
    ElMessage.error('提交失败');
  }
  evalAllLoading.value = false;
};

// v169: 清空并重生全部卡片
const regenLoading = ref(false);
const triggerRegenAll = async () => {
  regenLoading.value = true;
  try {
    const res = await regenerateAllFlashcards(filterSubject.value || undefined);
    if (res.code === 200) {
      ElMessage.success(res.message);
      // 开始轮询重生进度
      pollRegenProgress();
      regenTimer.value = setInterval(pollRegenProgress, 3000);
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  } catch {
    ElMessage.error('操作失败');
  }
  regenLoading.value = false;
};

const pollRegenProgress = async () => {
  try {
    const res = await getRegenerationProgress(filterSubject.value || undefined);
    if (res.code === 200) {
      regenProgress.value = res.data;
      if (res.data.status === 'done' || res.data.status === 'error') {
        if (regenTimer.value) { clearInterval(regenTimer.value); regenTimer.value = null; }
        if (res.data.status === 'done') {
          ElMessage.success(`卡片重生完成: ${res.data.generated || 0} 篇成功`);
          startProgressPolling(); // 追踪AI评估进度
          loadQueue();
        }
      }
    }
  } catch { /* 静默 */ }
};

onMounted(async () => {
  pollProgress();
  pollRegenProgress(); // v169: 页面刷新后恢复重生进度
  if (evalProgress.value && evalProgress.value.evaluated < evalProgress.value.total)
    startProgressPolling();
  try {
    const res = await getSubjectsGrouped();
    if (res.code === 200) {
      const list = [];
      (res.data.publicSubjects || []).forEach((s) =>
        list.push({ id: s.id, name: s.name })
      );
      (res.data.majorSubjects || []).forEach((s) =>
        list.push({ id: s.id, name: s.name })
      );
      subjects.value = list;
    }
  } catch {
    /* 静默 */
  }
  loadQueue();
});

onBeforeUnmount(() => {
  stopProgressPolling();
  if (regenTimer.value) { clearInterval(regenTimer.value); regenTimer.value = null; }
});
</script>

<style scoped>
.crq-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.crq-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.crq-header h2 {
  margin: 0;
  font-size: var(--fs-xl);
}
.crq-badge {
  background: var(--el-color-warning-light-3);
  color: var(--el-color-warning);
  padding: 2px 12px;
  border-radius: 12px;
  font-size: var(--fs-sm);
  font-weight: 600;
}
.crq-progress {
  margin-bottom: 16px;
  padding: 14px 18px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-md);
}
.crq-progress-info {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.crq-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
.crq-front {
  font-weight: 500;
  color: var(--text-primary);
}
.crq-back {
  color: var(--text-secondary);
  font-size: var(--fs-sm);
}
.crq-ai-detail {
  font-size: var(--fs-sm);
  line-height: 1.6;
}
.crq-ai-detail p {
  margin: 4px 0;
}
.crq-improved {
  margin-top: 8px;
  padding: 8px;
  background: var(--bg-success-light);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 6px;
}
.crq-no-ai {
  color: var(--text-disabled);
}
.crq-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.crq-summary {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-left: auto;
}
.crq-pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .crq-page { max-width: 100%; padding: 12px; }
  .crq-header { flex-wrap: wrap; gap: 8px; }
  .crq-header h2 { font-size: var(--fs-lg); }
  .crq-summary { margin-left: 0; width: 100%; }
  .crq-progress-info { flex-direction: column; gap: 4px; }
  .crq-toolbar {
    flex-wrap: nowrap; overflow-x: auto; padding-bottom: 8px;
    -webkit-overflow-scrolling: touch;
  }
  .crq-toolbar :deep(.el-button) { flex-shrink: 0; }
  .crq-toolbar :deep(.el-select) { width: 120px !important; flex-shrink: 0; }
  .crq-pager { margin-top: 12px; }
}
</style>
