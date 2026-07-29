<template>
  <div class="aia-page">
    <el-alert
      v-if="!featureEnabled"
      title="AI 教学助手功能未启用"
      type="warning"
      :closable="false"
      description="请联系管理员在系统设置中开启「AI 教学助手」功能。"
      show-icon
      style="margin-bottom: 24px"
    />
    <template v-if="featureEnabled">
      <AiGuideDialog v-model="showGuide" />
      <div class="aia-header">
        <h2>
          <el-icon><MagicStick /></el-icon> AI 教学助手
          <el-button
            size="small"
            text
            style="margin-left: auto; font-size: var(--fs-md)"
            title="重新查看引导"
            @click="showGuide = true"
          >
            <el-icon><QuestionFilled /></el-icon>
          </el-button>
        </h2>
        <span class="aia-sub">基于知识库生成教学设计、知识清单、实训方案、练习与课堂提问</span>
      </div>

      <el-card shadow="never" class="aia-card">
        <template #header>
          <div class="aia-card-title"><span class="aia-step">1</span> 选择知识点</div>
        </template>
        <CategoryCascade ref="cascadeRef" @change="onCategoryChange" />
        <div style="margin-top: 12px">
          <KnowledgePreview
            :category-id="cascade.kpId || cascade.taskId || cascade.chapterId"
            :include-children="!cascade.kpId"
          />
        </div>
        <div class="aia-row">
          <el-button size="small" :disabled="!cascade.kpId" @click="editDialogRef?.open()">
            <el-icon><Edit /></el-icon> 编辑内容
          </el-button>
          <el-button size="small" @click="importDialogRef?.open()">
            <el-icon><Upload /></el-icon> 导入知识库
          </el-button>
          <el-button size="small" type="success" @click="downloadTemplate">
            <el-icon><Download /></el-icon> 下载导入模板
          </el-button>
          <el-button
            v-if="userStore.isAdmin && cascade.subjectId"
            size="small"
            type="danger"
            plain
            @click="handleClearSubject"
          >
            <el-icon><Delete /></el-icon> 清空该学科数据
          </el-button>
        </div>
      </el-card>

      <AiLearningResources
        :node-id="lrNodeId"
        :node-label="lrNodeLabel"
        :is-admin="userStore.isAdmin"
      />
      <AiContentTypeSelector
        :is-mobile="isMobile"
        :show-practice-plans="showPracticePlan"
        :subject-name="cascade.subjectName"
        @update:content-type="contentType = $event"
        @update:design-style="designStyle = $event"
        @update:design-focus="designFocus = $event"
        @update:question-tier="questionTier = $event"
        @update:question-dim="questionDim = $event"
        @update:question-counts="Object.assign(questionCounts, $event)"
        @update:score-presets="Object.assign(scorePresets, $event)"
      />

      <el-card shadow="never" class="aia-card">
        <template #header>
          <div class="aia-card-title"><span class="aia-step">3</span> 生成结果</div>
        </template>
        <el-button
          type="primary"
          size="large"
          :loading="generating"
          :disabled="!canGenerate"
          class="aia-gen-btn aia-gen-btn-desktop"
          @click="doGenerate(canGenerate)"
        >
          <el-icon v-if="!generating"><MagicStick /></el-icon>
          {{ generating ? 'AI 正在生成' + typeLabel + '...' : '开始生成' }}
        </el-button>
        <div v-if="contentEmpty && canGenerate" class="aia-empty-warning">
          <el-icon><WarningFilled /></el-icon>
          该知识点暂无知识库内容，AI 生成将不基于您的教学内容
        </div>
        <AiGenerationProgress
          :generating="generating"
          :progress-message="progressMessage"
          :error="genError"
          :has-result="!!genResult"
          @retry="doGenerate(canGenerate)"
        />
        <GenerationResult
          v-if="genResult"
          :result="genResult"
          @edit="editResult"
          @publish="publishResult"
          @export-word="exportWord(null)"
          @regenerate="doGenerate(canGenerate)"
          @publish-as-task="publishTaskDialogRef?.open(genResult)"
          @publish-as-exam="onPublishAsExam"
          @view-in-bank="router.push('/teacher/tasks/question-bank')"
          @edit-questions="onEditQuestions"
          @generate-practice="onGeneratePractice"
        />
        <QuestionEditor
          v-model="questionEditorVisible"
          :batch-id="editingBatchId"
          :initial-questions="editingQuestions"
          @closed="questionEditorVisible = false"
          @saved="onQuestionsSaved"
        />
        <el-dialog
          v-model="editDialogVisible"
          title="编辑内容"
          width="700px"
          append-to-body
          destroy-on-close
        >
          <el-tabs v-model="editTab">
            <el-tab-pane label="编辑" name="edit">
              <el-input v-model="editContent" type="textarea" :rows="16" />
            </el-tab-pane>
            <el-tab-pane label="预览" name="preview">
              <div class="edit-preview" v-html="renderMarkdown(editContent)"></div>
            </el-tab-pane>
          </el-tabs>
          <template #footer>
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="doSaveEdit">保存</el-button>
          </template>
        </el-dialog>
      </el-card>

      <el-card shadow="never" class="aia-card">
        <template #header><div class="aia-card-title">历史记录</div></template>
        <ContentHistory
          :items="historyItems"
          :loading="historyLoading"
          :has-more="historyHasMore"
          @filter="onHistoryFilter"
          @search="onHistorySearch"
          @view="viewHistory"
          @publish="publishHistory"
          @archive="archiveHistory"
          @export-word="exportWord"
          @generate-practice="onGeneratePractice"
          @load-more="loadMore"
        />
      </el-card>

      <!-- 移动端粘性生成栏 -->
      <div v-if="isMobile" class="aia-mobile-bar">
        <div class="aia-mobile-bar-info">
          <span class="aia-mobile-bar-type">{{ typeLabel }}</span>
          <span class="aia-mobile-bar-kp">{{
            cascade.kpName || cascade.chapterName || '请先选择知识点'
          }}</span>
        </div>
        <el-button
          type="primary"
          :loading="generating"
          :disabled="!canGenerate"
          class="aia-mobile-bar-btn"
          @click="doGenerate(canGenerate)"
        >
          <el-icon v-if="!generating"><MagicStick /></el-icon>
          {{ generating ? '生成中' : '开始生成' }}
        </el-button>
      </div>
    </template>

    <KnowledgeEditDialog ref="editDialogRef" :category-id="cascade.kpId" @saved="() => {}" />
    <ImportZipDialog ref="importDialogRef" @imported="onKnowledgeImported" />
    <PublishPracticeTaskDialog ref="publishTaskDialogRef" @published="loadHistory" />
    <AiExamPublishDialog
      ref="examDialogRef"
      v-model="examDialogVisible"
      :subject-name="cascade.subjectName"
      :total-score-preview="totalScorePreview"
      :score-presets="scorePresets"
      @published="loadHistory"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import {
  MagicStick,
  Edit,
  Upload,
  Download,
  Delete,
  WarningFilled,
  QuestionFilled,
} from '@element-plus/icons-vue';
import CategoryCascade from '@/components/ai/CategoryCascade.vue';
import KnowledgePreview from '@/components/ai/KnowledgePreview.vue';
import KnowledgeEditDialog from '@/components/ai/KnowledgeEditDialog.vue';
import ImportZipDialog from '@/components/ai/ImportZipDialog.vue';
import PublishPracticeTaskDialog from '@/components/ai/PublishPracticeTaskDialog.vue';
import GenerationResult from '@/components/ai/GenerationResult.vue';
import QuestionEditor from '@/components/ai/QuestionEditor.vue';
import ContentHistory from '@/components/ai/ContentHistory.vue';
import { renderMarkdown } from '@/utils/markdown';
import { clearKnowledgeBySubject, getNodeContent } from '@/api/knowledgeNode';
import { getMyTeachingAssignments } from '@/api/settings';
import { useUserStore } from '@/stores/user';
import { useSettingsStore } from '@/stores/settings';
import { useIsMobile } from '@/composables/useIsMobile';
import { useAiGeneration } from '@/composables/useAiGeneration';
import { useAiHistory } from '@/composables/useAiHistory';
import { useAiEditor } from '@/composables/useAiEditor';
import AiGuideDialog from './components/AiGuideDialog.vue';
import AiLearningResources from './components/AiLearningResources.vue';
import AiContentTypeSelector from './components/AiContentTypeSelector.vue';
import AiGenerationProgress from './components/AiGenerationProgress.vue';
import AiExamPublishDialog from './components/AiExamPublishDialog.vue';

const router = useRouter();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const { isMobile } = useIsMobile();
const showPracticePlan = computed(() => userStore.isAdmin || userStore.showPracticePlans);
const featureEnabled = computed(() => settingsStore.isEnabled('feature.ai_content_enabled'));

// Cascade
const cascade = reactive({
  subjectId: null,
  chapterId: null,
  taskId: null,
  kpId: null,
  subjectName: '',
  chapterName: '',
  kpName: '',
  kpList: [],
  categoryIds: [],
  subKpList: [],
});
const lrNodeId = computed(() => cascade.kpId || cascade.taskId || cascade.chapterId);
const lrNodeLabel = computed(
  () =>
    cascade.kpName ||
    (cascade.taskId
      ? cascade.subjectName + ' › ' + (cascade.chapterName || '') + ' › 任务'
      : cascade.chapterId
        ? cascade.subjectName + ' › ' + (cascade.chapterName || '章节')
        : '')
);
const cascadeRef = ref(null);

// 知识库内容状态（异步检查，null=未检查，true=空内容，false=有内容）
const contentEmpty = ref(false);

// Content config
const contentType = ref('TEACHING_DESIGN');
const designStyle = ref('STANDARD');
const designFocus = ref('BALANCED');
const questionTier = ref('BALANCED');
const questionDim = ref('BOTH');
const questionCounts = reactive({
  SINGLE_CHOICE: 5,
  MULTI_CHOICE: 3,
  TRUE_FALSE: 3,
  FILL_IN: 3,
  ESSAY: 1,
});
const scorePresets = reactive({
  SINGLE_CHOICE: 2,
  MULTI_CHOICE: 3,
  TRUE_FALSE: 1,
  FILL_IN: 2,
  ESSAY: 10,
});
const isQuestionType = computed(() =>
  ['COMPREHENSIVE_EXERCISES', 'CLASSROOM_QUESTIONS', 'KNOWLEDGE_PRACTICE'].includes(
    contentType.value
  )
);
const totalScorePreview = computed(() =>
  Object.entries(questionCounts).reduce((s, [k, v]) => s + (v || 0) * (scorePresets[k] || 0), 0)
);

const sourceOutputId = ref(null);

// Stage hint
const teachingAssignments = ref([]);
const getSubjectName = () => cascade.subjectName || '';
const deriveStageHint = () => {
  const grades = teachingAssignments.value
    .filter((a) => a.subject === cascade.subjectName)
    .map((a) => a.grade)
    .filter(Boolean);
  if (grades.length > 0) {
    const g = grades[0];
    if (g.includes('中职') || g.includes('职高') || g.includes('中专')) return '中职';
    if (['高一', '高二', '高三', '高中'].some((k) => g.includes(k))) return '高中';
    if (['初一', '初二', '初三', '初中', '七年', '八年', '九年'].some((k) => g.includes(k)))
      return '初中';
    if (['一年', '二年', '三年', '四年', '五年', '六年', '小学'].some((k) => g.includes(k)))
      return '小学';
  }
  const s = cascade.subjectName || '';
  if (s.includes('[普高]')) return '高中';
  if (s.includes('[职高]')) return '中职';
  if (s.includes('[初中]')) return '初中';
  return '';
};

const onGeneratePractice = (checklistId) => {
  sourceOutputId.value = checklistId;
  contentType.value = 'KNOWLEDGE_PRACTICE';
  // 联动路径题量固定为配套练习默认配比（CLOZE 归主观题需手批，默认不出）
  const kpCounts = { FILL_IN: 7, TRUE_FALSE: 4, SINGLE_CHOICE: 5 };
  const kpScores = { FILL_IN: 2, TRUE_FALSE: 1, SINGLE_CHOICE: 2 };
  // 先移除新对象中没有的旧 key，再合并新值（避免逐个 delete 触发多次 watch）
  Object.keys(questionCounts).forEach((k) => {
    if (!(k in kpCounts)) delete questionCounts[k];
  });
  Object.assign(questionCounts, kpCounts);
  Object.keys(scorePresets).forEach((k) => {
    if (!(k in kpScores)) delete scorePresets[k];
  });
  Object.assign(scorePresets, kpScores);
  nextTick(() => {
    doGenerate(true);
  });
};

const onCategoryChange = (val) => {
  Object.assign(cascade, {
    subjectId: val.subjectId,
    chapterId: val.chapterId,
    taskId: val.taskId,
    kpId: val.kpId,
    kpName: val.kpName || '',
    chapterName: val.chapterName || '',
    subjectName: val.subjectName || '',
    kpList: val.kpList || [],
    categoryIds: val.categoryIds || [],
    subKpList: val.subKpList || [],
  });
  genResult.value = null;
  genError.value = '';
  sourceOutputId.value = null;
  // 异步检查选中节点是否有知识库内容
  const nodeId = val.kpId || val.taskId || val.chapterId;
  if (nodeId) {
    // 选中具体知识点(kpId)时只查自身，选中章节/任务时需查子节点
    const includeChildren = !cascade.kpId;
    getNodeContent(nodeId, includeChildren)
      .then((res) => {
        contentEmpty.value = !res.data?.content;
      })
      .catch(() => {
        contentEmpty.value = true;
      });
  } else {
    contentEmpty.value = false;
  }
};
const canGenerate = computed(
  () =>
    (cascade.kpId || cascade.taskId || cascade.chapterId) &&
    contentType.value &&
    !(contentType.value === 'PRACTICE_PLAN' && !showPracticePlan.value)
);
const typeLabel = computed(() => {
  const map = {
    TEACHING_DESIGN: '教学设计',
    KNOWLEDGE_CHECKLIST: '知识清单',
    PRACTICE_PLAN: '实训方案',
    COMPREHENSIVE_EXERCISES: '综合练习',
    CLASSROOM_QUESTIONS: '课堂提问',
    KNOWLEDGE_PRACTICE: '配套练习',
  };
  return map[contentType.value] || '内容';
});

// Composables
const { generating, genResult, genError, genId, progressMessage, doGenerate } = useAiGeneration({
  contentType,
  designStyle,
  designFocus,
  questionCounts,
  questionTier,
  questionDim,
  scorePresets,
  isQuestionType,
  cascade,
  getSubjectName,
  deriveStageHint,
  sourceOutputId,
  onSuccess: () => loadHistory(),
});
const {
  historyItems,
  historyLoading,
  loadHistory,
  loadMore,
  onHistoryFilter,
  onHistorySearch,
  viewHistory,
  publishHistory,
  archiveHistory,
  historyHasMore,
} = useAiHistory({ genResult, genId });
const {
  editDialogVisible,
  editContent,
  editTab,
  editResult,
  doSaveEdit,
  questionEditorVisible,
  editingBatchId,
  editingQuestions,
  onEditQuestions,
  onQuestionsSaved,
  parseOptions,
} = useAiEditor({ genResult, genId });

// Refs & misc
const editDialogRef = ref(null);
const importDialogRef = ref(null);
const publishTaskDialogRef = ref(null);
const examDialogRef = ref(null);
const examDialogVisible = ref(false);
const showGuide = ref(!localStorage.getItem('aia_guide_viewed'));

const exportWord = async (row) => {
  const { downloadFile } = await import('@/utils/request');
  let url = '',
    filename = 'AI产出.docx';
  if (row?.batchId && row?.questionCount) {
    url = `/api/question-bank/actions/by-batch/${row.batchId}/export`;
    filename = `试卷_${row.title || row.batchId}.docx`;
  } else if (row?.id) url = `/api/ai-output/${row.id}/export`;
  else if (genId.value) url = `/api/ai-output/${genId.value}/export`;
  else return;
  try {
    await downloadFile(url, filename);
  } catch (e) {
    ElMessage.error('导出失败' + (e.message ? ': ' + e.message : ''));
  }
};

const downloadTemplate = async () => {
  const { downloadFile } = await import('@/utils/request');
  try {
    await downloadFile('/api/knowledge-node/actions/zip-template/download', '知识点导入模板.zip');
  } catch {
    ElMessage.error('下载失败');
  }
};

const publishResult = async () => {
  if (!genId.value) return;
  try {
    const { publishAiOutput } = await import('@/api/aiOutput');
    await ElMessageBox.confirm('发布后，学生可在「历史记录」中查看此内容。确定发布？', '确认发布', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      type: 'info',
    });
    const res = await publishAiOutput(genId.value);
    if (res.code === 200) {
      ElMessage.success('已发布');
      loadHistory();
    }
  } catch {
    /* */
  }
};

const onPublishAsExam = async (selectedIds) => {
  const latestQuestions = genResult.value?.questions || [];
  const qids = selectedIds?.length ? selectedIds : latestQuestions.map((q) => q.id);
  if (!qids.length) return ElMessage.warning('没有可发布的题目');
  examDialogRef.value?.open({
    questions: latestQuestions.filter((q) => qids.includes(q.id)),
    title: genResult.value?.title || 'AI组卷',
    totalScorePreview: totalScorePreview.value,
    scorePresets: { ...scorePresets },
  });
};

const onKnowledgeImported = () => {
  ElMessage.success('知识库已导入，请重新选择知识点');
  cascadeRef.value?.refresh();
};
const handleClearSubject = async () => {
  try {
    await ElMessageBox.prompt(
      '将清空该学科下所有章节、任务、知识点及关联的AI产出和题库题目，此操作不可恢复。\n请输入「确认清空」以继续：',
      '危险操作',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'warning',
        inputPattern: /^确认清空$/,
        inputErrorMessage: '请输入「确认清空」',
        inputPlaceholder: '输入「确认清空」',
      }
    );
    const res = await clearKnowledgeBySubject(cascade.subjectId);
    if (res.code === 200) {
      ElMessage.success('该学科知识节点已清空');
      cascadeRef.value?.refresh();
    }
  } catch {
    /* 用户取消或输入不正确 */
  }
};

onMounted(async () => {
  if (!settingsStore.loaded) {
    await settingsStore.fetchFeatureFlags();
  }
  if (!featureEnabled.value) return;
  loadHistory();
  try {
    const res = await getMyTeachingAssignments();
    if (res.code === 200) teachingAssignments.value = res.data || [];
  } catch {
    /* */
  }
});
</script>

<style scoped>
.aia-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}
.aia-header {
  margin-bottom: 24px;
}
.aia-header h2 {
  margin: 0;
  font-size: 22px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}
.aia-sub {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-top: 6px;
  display: block;
}
.aia-card {
  margin-bottom: 20px;
}
.aia-card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--fs-md);
  font-weight: 700;
}
.aia-step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--primary-color);
  color: var(--text-on-primary);
  font-size: var(--fs-sm);
  font-weight: 700;
}
.aia-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.aia-gen-btn {
  height: 48px;
  padding: 0 40px;
  font-size: var(--fs-lg);
  border-radius: 12px;
}
.aia-empty-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 8px 12px;
  background: var(--bg-warning-light);
  border-radius: 8px;
  color: var(--el-color-warning);
  font-size: var(--fs-sm);
}
.edit-preview {
  padding: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  min-height: 300px;
  max-height: 500px;
  overflow-y: auto;
  font-size: var(--fs-md);
  line-height: 1.8;
}
.edit-preview img {
  max-width: 100%;
}
/* 移动端粘性生成栏：桌面端隐藏 */
.aia-mobile-bar {
  display: none;
}

@media (max-width: 767px) {
  .aia-page {
    padding: 8px;
    padding-bottom: 130px;
  }
  .aia-header {
    margin-bottom: 12px;
    padding: 12px 14px;
    background: linear-gradient(135deg, var(--primary-light), var(--bg-card));
    border-radius: var(--radius-lg);
    border: 0.5px solid var(--border-color);
  }
  .aia-header h2 {
    font-size: var(--fs-lg);
  }
  .aia-sub {
    font-size: var(--fs-xs);
    margin-top: 4px;
  }
  .aia-card {
    margin-bottom: 10px;
  }
  .aia-card :deep(.el-card__body) {
    padding: 12px;
  }
  .aia-card :deep(.el-card__header) {
    padding: 10px 12px;
  }
  .aia-step {
    width: 22px;
    height: 22px;
    font-size: var(--fs-xs);
  }
  /* 桌面端生成按钮在移动端隐藏，改用粘性栏 */
  .aia-gen-btn-desktop {
    display: none;
  }
  .aia-row {
    gap: 6px;
    margin-top: 8px;
  }
  .aia-row .el-button {
    flex: 1;
    min-width: calc(50% - 6px);
    font-size: var(--fs-xs);
    padding: 6px 8px;
  }
  /* 粘性生成栏 */
  .aia-mobile-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    position: fixed;
    bottom: calc(56px + env(safe-area-inset-bottom, 0px));
    left: 0;
    right: 0;
    padding: 10px 14px;
    background: var(--bg-card);
    border-top: 0.5px solid var(--border-color);
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
    z-index: 1000;
  }
  .aia-mobile-bar-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }
  .aia-mobile-bar-type {
    font-size: var(--fs-sm);
    font-weight: 600;
    color: var(--text-primary);
  }
  .aia-mobile-bar-kp {
    font-size: var(--fs-xs);
    color: var(--text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .aia-mobile-bar-btn {
    flex-shrink: 0;
    height: 44px;
    padding: 0 24px;
    font-size: var(--fs-md);
    font-weight: 600;
    border-radius: var(--radius-lg);
  }
  .aia-mobile-bar-btn.is-disabled {
    opacity: 0.5;
  }
}
</style>
