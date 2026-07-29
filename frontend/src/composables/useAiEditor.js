import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { updateAiOutput } from '@/api/aiOutput';

const parseOptions = (opts) => {
  if (!opts) return [];
  if (Array.isArray(opts)) return opts;
  if (typeof opts === 'string') {
    try {
      return Array.isArray(JSON.parse(opts)) ? JSON.parse(opts) : [];
    } catch {
      return [];
    }
  }
  return [];
};

export function useAiEditor({ genResult, genId }) {
  const editDialogVisible = ref(false);
  const editContent = ref('');
  const editTab = ref('edit');

  const editResult = () => {
    const outputId = genId.value || genResult.value?.outputId;
    if (!outputId) return;
    editContent.value = genResult.value?.content || '';
    editDialogVisible.value = true;
  };

  const doSaveEdit = async () => {
    try {
      await updateAiOutput(genId.value, { content: editContent.value });
      genResult.value.content = editContent.value;
      ElMessage.success('已更新');
      editDialogVisible.value = false;
    } catch {
      ElMessage.error('保存失败');
    }
  };

  // ── Question editor ──
  const questionEditorVisible = ref(false);
  const editingBatchId = ref('');
  const editingQuestions = ref([]);

  const onEditQuestions = (batchId, questions) => {
    editingBatchId.value = batchId;
    editingQuestions.value = questions || [];
    questionEditorVisible.value = true;
  };

  const onQuestionsSaved = async (updatedQuestions) => {
    if (genResult.value?.type === 'questions') {
      genResult.value.questions = updatedQuestions;
      genResult.value.count = updatedQuestions.length;
      editingQuestions.value = updatedQuestions;
    }
  };

  return {
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
  };
}
