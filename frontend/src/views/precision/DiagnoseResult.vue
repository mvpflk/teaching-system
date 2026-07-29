<template>
  <div class="dr-page">
    <div class="page-header">
      <h2 class="dr-title">诊断报告</h2>
      <div class="dr-summary">
        <span :class="scoreClass">{{ report.score || 0 }}分</span>
        <span class="dr-sep">|</span>
        <span>正确 {{ report.correctCount }}/{{ report.totalQuestions }}</span>
        <span class="dr-sep">|</span>
        <span class="dr-rate">{{ correctRate }}%</span>
      </div>
    </div>

    <!-- 短板分析 -->
    <div v-if="report.level || report.advice" class="dr-analysis">
      <el-alert
        :title="'诊断结论：' + (report.level || '')"
        :description="report.advice || ''"
        :type="levelAlertType"
        show-icon
        :closable="false"
      />
    </div>

    <!-- 题型筛选 -->
    <div class="dr-filter-bar">
      <el-radio-group v-model="typeFilter" size="small" @change="onTypeFilter">
        <el-radio-button value="">全部题型</el-radio-button>
        <el-radio-button value="SINGLE_CHOICE">选择题</el-radio-button>
        <el-radio-button value="FILL_IN">填空题</el-radio-button>
        <el-radio-button value="ESSAY">问答题</el-radio-button>
      </el-radio-group>
      <el-tag
        v-if="pendingCount > 0"
        type="warning"
        size="small"
        effect="plain"
      >
        {{ pendingCount }}题待评阅
      </el-tag>
    </div>

    <!-- 逐题表格 -->
    <el-table
      :data="filteredItems"
      size="small"
      stripe
      style="width: 100%"
    >
      <el-table-column label="#" width="50" type="index" />
      <el-table-column label="题型" width="90">
        <template #default="{ row }">{{ typeLabel(row.questionType) }}</template>
      </el-table-column>
      <el-table-column label="题目" min-width="200">
        <template #default="{ row }">
          <div class="dr-qtext" v-html="renderMarkdown(row.questionText || '')" />
        </template>
      </el-table-column>
      <el-table-column label="你的答案" width="120">
        <template #default="{ row }">
          <span
            :class="{ 'dr-answer-wrong': !row.isCorrect && row.matchMode !== 'pending_review' }"
            v-html="renderMarkdown(row.studentAnswer || '未作答')"
          ></span>
        </template>
      </el-table-column>
      <el-table-column label="正确答案" width="120">
        <template #default="{ row }">
          <span v-if="row.matchMode === 'pending_review'" class="dr-pending">待评阅</span>
          <span v-else v-html="renderMarkdown(row.correctAnswer)"></span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag
            v-if="row.matchMode === 'pending_review'"
            type="warning"
            size="small"
            effect="plain"
          >
            ⏳
          </el-tag>
          <el-tag
            v-else-if="row.isCorrect"
            type="success"
            size="small"
            effect="plain"
          >
            ✅
          </el-tag>
          <el-tag
            v-else
            type="danger"
            size="small"
            effect="plain"
          >
            ❌
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="解析" width="80" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.matchMode === 'pending_review'"
            type="warning"
            size="small"
            link
            @click="openGrading(row)"
          >
            评阅
          </el-button>
          <el-button
            v-else-if="row.explanation"
            type="primary"
            size="small"
            link
            @click="openExplanation(row)"
          >
            查看
          </el-button>
          <span v-else class="dr-no-exp">-</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 模块掌握度 -->
    <div v-if="report.moduleScores?.length" class="dr-modules-section">
      <h4 class="dr-section-title">模块掌握度</h4>
      <div class="dr-module-list">
        <div v-for="m in report.moduleScores" :key="m.moduleName" class="dr-module-item">
          <div class="dr-module-header">
            <span class="dr-module-name">{{ m.moduleName }}</span>
            <span class="dr-module-accuracy" :class="moduleAccuracyClass(m.accuracy)">
              {{ m.accuracy }}% ({{ m.correct }}/{{ m.total }})
            </span>
          </div>
          <div class="dr-module-bar-bg">
            <div
              class="dr-module-bar-fill"
              :class="moduleAccuracyClass(m.accuracy)"
              :style="{ width: m.accuracy + '%' }"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 薄弱知识点 -->
    <div v-if="report.weakItems?.length" class="dr-weak-section">
      <h4 class="dr-section-title">薄弱知识点（{{ report.weakItems.length }}题答错）</h4>
      <div class="dr-weak-list">
        <div v-for="(item, i) in report.weakItems.slice(0, 5)" :key="i" class="dr-weak-item">
          <el-tag
            type="danger"
            size="small"
            effect="plain"
            class="dr-weak-tag"
          >
            {{ i + 1 }}
          </el-tag>
          <div style="flex: 1; min-width: 0">
            <span class="dr-weak-text" v-html="renderMarkdown(item.questionText || '')" />
            <WeaknessCardLink
              v-if="item.knowledgeNodeId"
              :node-id="item.knowledgeNodeId"
              :node-name="item.knowledgeNodeName"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 教师评阅对话框 -->
    <EssayGradingDialog v-model="gradingVisible" :item="gradingItem" />

    <!-- 答案解析对话框 -->
    <el-dialog
      v-model="explanationVisible"
      title="答案解析"
      width="600px"
      :close-on-click-modal="true"
    >
      <div class="dr-exp-content">
        <div class="dr-exp-row">
          <strong>题目：</strong><span v-html="renderMarkdown(selectedItem?.questionText || '')" />
        </div>
        <div class="dr-exp-row">
          <strong>你的答案：</strong>
          <span :class="selectedItem?.isCorrect ? 'dr-answer-correct' : 'dr-answer-wrong'" v-html="renderMarkdown(selectedItem?.studentAnswer || '未作答')">
          </span>
        </div>
        <div class="dr-exp-row"><strong>正确答案：</strong><span v-html="renderMarkdown(selectedItem?.correctAnswer)"></span></div>
        <div class="dr-exp-row">
          <strong>解析：</strong><span v-html="renderMarkdown(selectedItem?.explanation || '暂无解析')"></span>
        </div>
      </div>
      <template #footer>
        <el-button @click="explanationVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { renderMarkdown } from '@/utils/markdown';
import { useIsMobile } from '@/composables/useIsMobile';
import { usePrecisionStore } from '@/stores/precision';
import EssayGradingDialog from '@/components/precision/EssayGradingDialog.vue';
import WeaknessCardLink from '@/components/knowledge/WeaknessCardLink.vue';

const { isMobile } = useIsMobile();
const { getDiagnosisReport } = usePrecisionStore();

const route = useRoute();
const report = ref({});
const typeFilter = ref('');
const explanationVisible = ref(false);
const gradingVisible = ref(false);
const selectedItem = ref(null);
const gradingItem = ref(null);

const pendingCount = computed(
  () => (report.value.itemResults || []).filter((i) => i.matchMode === 'pending_review').length
);

const correctRate = computed(() => {
  const t = report.value.totalQuestions;
  return t > 0 ? Math.round(((report.value.correctCount || 0) / t) * 100) : 0;
});

const scoreClass = computed(() => {
  const s = report.value.score || 0;
  if (s >= 80) return 'dr-score dr-score-high';
  if (s >= 60) return 'dr-score dr-score-mid';
  return 'dr-score dr-score-low';
});

const levelAlertType = computed(() => {
  const level = report.value.level || '';
  if (level === '优秀') return 'success';
  if (level === '良好') return 'warning';
  return 'info'; // 发展中/起步期 不用 error 红色
});

const filteredItems = computed(() => {
  let items = report.value.itemResults || [];
  if (typeFilter.value) {
    items = items.filter((i) => i.questionType === typeFilter.value);
  }
  return items;
});

function typeLabel(type) {
  const map = {
    SINGLE_CHOICE: '单选',
    MULTI_CHOICE: '多选',
    TRUE_FALSE: '判断',
    FILL_IN: '填空',
    ESSAY: '问答',
    SHORT_ANSWER: '简答',
  };
  return map[type] || type;
}

function moduleAccuracyClass(pct) {
  if (pct >= 80) return 'dr-module-label-high';
  if (pct >= 50) return 'dr-module-label-mid';
  return 'dr-module-label-low';
}

function openExplanation(row) {
  selectedItem.value = row;
  explanationVisible.value = true;
}

function openGrading(row) {
  gradingItem.value = row;
  gradingVisible.value = true;
}

function onTypeFilter() {
  // el-table 自动响应 computed
}

onMounted(() => {
  // 优先从 Pinia Store 读取（防 URL 截断）
  const stored = getDiagnosisReport();
  if (stored) {
    report.value = stored;
    return;
  }
  // 兼容旧的 URL query 方式
  if (route.query?.data) {
    try {
      report.value = JSON.parse(decodeURIComponent(route.query.data));
    } catch (e) {
      console.error('解析诊断报告数据失败:', e);
    }
  }
});
</script>

<style scoped>
.dr-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px;
}
.dr-title {
  font-size: var(--fs-2xl, 24px);
  font-weight: 600;
  color: var(--text-primary, var(--text-primary));
  margin: 0;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}
.dr-summary {
  font-size: var(--fs-md);
  display: flex;
  align-items: center;
  gap: 8px;
}
.dr-score {
  font-size: var(--fs-xl);
  font-weight: 700;
}
.dr-score-high {
  color: var(--el-color-success);
}
.dr-score-mid {
  color: var(--el-color-warning);
}
.dr-score-low {
  color: var(--el-color-danger);
}
.dr-sep {
  color: var(--text-disabled, var(--text-disabled));
}
.dr-rate {
  color: var(--text-secondary, var(--text-secondary));
}

.dr-analysis {
  margin-bottom: 16px;
}
.dr-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.dr-qtext {
  font-size: var(--fs-sm);
  line-height: 1.6;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
.dr-answer-wrong {
  color: var(--el-color-danger);
  font-weight: 500;
}
.dr-answer-correct {
  color: var(--el-color-success);
  font-weight: 500;
}
.dr-pending {
  color: var(--el-color-warning);
  font-style: italic;
}
.dr-no-exp {
  color: var(--text-disabled, var(--text-disabled));
}

.dr-modules-section {
  margin-top: 24px;
  padding: 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-md, 8px);
}
.dr-module-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dr-module-item {
  position: relative;
}
.dr-module-header {
  display: flex;
  justify-content: space-between;
  font-size: var(--fs-sm);
  margin-bottom: 4px;
}
.dr-module-name {
  font-weight: 500;
  color: var(--text-primary, var(--text-primary));
}
.dr-module-accuracy {
  font-size: var(--fs-xs);
}
.dr-module-label-high {
  color: var(--el-color-success, #67c23a);
}
.dr-module-label-mid {
  color: var(--el-color-warning, #e6a23c);
}
.dr-module-label-low {
  color: var(--el-color-danger, #f56c6c);
}
.dr-module-bar-bg {
  height: 8px;
  background: var(--bg-secondary, var(--bg-secondary));
  border-radius: 4px;
  overflow: hidden;
}
.dr-module-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}
.dr-module-bar-fill.dr-module-label-high {
  background: var(--el-color-success, #67c23a);
}
.dr-module-bar-fill.dr-module-label-mid {
  background: var(--el-color-warning, #e6a23c);
}
.dr-module-bar-fill.dr-module-label-low {
  background: var(--el-color-danger, #f56c6c);
}

.dr-weak-section {
  margin-top: 24px;
  padding: 16px;
  background: #fff5f5;
  border: 1px solid #ffcccc;
  border-radius: var(--radius-md, 8px);
}
.dr-section-title {
  font-size: var(--fs-lg, 16px);
  font-weight: 600;
  margin: 0 0 12px;
  color: var(--text-primary, var(--text-primary));
}
.dr-weak-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dr-weak-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: var(--fs-sm);
  line-height: 1.5;
}
.dr-weak-tag {
  flex-shrink: 0;
  margin-top: 2px;
}
.dr-weak-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.dr-exp-content {
  font-size: var(--fs-md);
  line-height: 1.8;
}
.dr-exp-row {
  margin-bottom: 10px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .dr-filter-bar {
    flex-wrap: wrap;
  }
}
</style>
