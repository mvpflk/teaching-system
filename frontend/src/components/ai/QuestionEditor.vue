<template>
  <el-drawer
    v-model="visible"
    title="编辑试卷"
    size="780px"
    direction="rtl"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="qe-toolbar">
      <span class="qe-count">共 {{ questions.length }} 题</span>
      <el-button size="small" type="primary" @click="addNew">
        <el-icon><Plus /></el-icon> 新增题目
      </el-button>
      <el-button size="small" type="success" @click="showImportDialog = true">
        <el-icon><Files /></el-icon> 从题库导入
      </el-button>
      <el-button size="small" :loading="saving" @click="saveAll">
        <el-icon><Check /></el-icon> 保存所有修改
      </el-button>
    </div>

    <div v-if="questions.length === 0" style="text-align: center; padding: 60px 0; color: #999">
      暂无题目，请点击"新增题目"或"从题库导入"
    </div>

    <div v-else class="qe-body">
      <div class="qe-list">
        <div
          v-for="(q, i) in questions"
          :key="q.id || i"
          class="qe-list-item"
          :class="{ active: editingIndex === i }"
          @click="selectQuestion(i)"
        >
          <span class="qe-list-no">{{ i + 1 }}.</span>
          <span class="qe-list-text">{{ truncateText(q.questionText || '新题目') }}</span>
          <el-tag size="small" :type="q._modified ? 'warning' : 'info'">
            {{ typeLabel(q.questionType) }}
          </el-tag>
          <el-button
            size="small"
            text
            type="danger"
            @click.stop="removeQuestion(i)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <QuestionEditPanel
        v-if="editingIndex !== null && editingQuestion"
        :question="editingQuestion"
        :question-index="editingIndex"
        @update:question="onQuestionUpdate"
      />
    </div>

    <QuestionImportDialog
      v-model="showImportDialog"
      :subject="currentSubject"
      :category-id="currentCategoryId"
      @import="doImportQuestions"
    />
  </el-drawer>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Files, Check, Delete } from '@element-plus/icons-vue';
import {
  getQuestionsByBatch,
  createQuestion,
  updateQuestion,
  deleteQuestion,
} from '@/api/questionBank';
import { useUserStore } from '@/stores/user';
import QuestionEditPanel from './QuestionEditPanel.vue';
import QuestionImportDialog from './QuestionImportDialog.vue';

const props = defineProps({
  modelValue: Boolean,
  batchId: String,
  initialQuestions: { type: Array, default: () => [] },
});

const emit = defineEmits(['update:modelValue', 'closed', 'saved']);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const userStore = useUserStore();
const questions = ref([]);
const editingIndex = ref(null);
const editingQuestion = ref(null);
const saving = ref(false);
const showImportDialog = ref(false);

const typeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选' },
  { value: 'MULTI_CHOICE', label: '多选' },
  { value: 'TRUE_FALSE', label: '判断' },
  { value: 'FILL_IN', label: '填空' },
  { value: 'SHORT_ANSWER', label: '简答' },
];
const TYPE_CN = Object.fromEntries(typeOptions.map((t) => [t.value, t.label]));
const typeLabel = (t) => TYPE_CN[t] || t;

// 从已有题目中获取当前学科和知识点，传给导入对话框做默认筛选
const currentSubject = computed(() => {
  for (const q of questions.value) {
    if (q.subject) return q.subject;
  }
  return '';
});
const currentCategoryId = computed(() => {
  for (const q of questions.value) {
    if (q.categoryId) return q.categoryId;
  }
  return null;
});

const onQuestionUpdate = (updated) => {
  if (editingQuestion.value) {
    Object.assign(editingQuestion.value, updated);
  }
};

const truncateText = (text) => {
  if (!text) return '';
  const cleaned = text
    .replace(/<[^>]+>/g, '')
    .replace(/^[\d.、)\s]+/, '')
    .trim();
  return cleaned.length > 50 ? cleaned.substring(0, 50) + '...' : cleaned;
};

const selectQuestion = (i) => {
  saveCurrentEdit();
  editingIndex.value = i;
  editingQuestion.value = reactive(JSON.parse(JSON.stringify(questions.value[i])));
};

const saveCurrentEdit = () => {
  if (editingIndex.value !== null && editingQuestion.value) {
    const updated = JSON.parse(JSON.stringify(editingQuestion.value));
    updated._modified = true;
    questions.value[editingIndex.value] = updated;
  }
};

watch(editingIndex, (newVal, oldVal) => {
  if (oldVal !== null) saveCurrentEdit();
});

const addNew = () => {
  saveCurrentEdit();
  const newQ = {
    id: null,
    questionText: '',
    questionType: 'SINGLE_CHOICE',
    options: ['', '', '', ''],
    correctAnswer: 'A',
    explanation: '',
    difficultyLevel: 2,
    subject: '',
    categoryId: null,
    _modified: true,
    _isNew: true,
  };
  questions.value.push(newQ);
  selectQuestion(questions.value.length - 1);
};

const removeQuestion = async (i) => {
  try {
    await ElMessageBox.confirm('确定删除该题目？此操作不可恢复。', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }

  const q = questions.value[i];
  if (q.id && !q._isNew && !q._imported) {
    try {
      await deleteQuestion(q.id);
    } catch (e) {
      ElMessage.warning('该题目删除失败（可能已被发布使用），已保留');
      return;
    }
  }
  questions.value.splice(i, 1);
  if (editingIndex.value === i) {
    editingIndex.value = null;
    editingQuestion.value = null;
  } else if (editingIndex.value > i) {
    editingIndex.value--;
  }
};

const saveAll = async () => {
  saveCurrentEdit();
  saving.value = true;
  let savedCount = 0;
  try {
    for (const q of questions.value) {
      if (!q._modified) continue;
      const payload = {
        questionText: q.questionText,
        questionType: q.questionType,
        options: Array.isArray(q.options) ? JSON.stringify(q.options) : q.options || '[]',
        correctAnswer: Array.isArray(q.correctAnswer)
          ? q.correctAnswer.join(',')
          : String(q.correctAnswer || ''),
        explanation: q.explanation || '',
        difficultyLevel: q.difficultyLevel || 2,
        subject: q.subject || '',
        categoryId: q.categoryId || null,
      };

      if (q.id && !q._isNew) {
        if (q._imported) {
          // 从题库导入的题：不修改原题，创建新副本
          const res = await createQuestion(payload);
          if (res.code === 200 && res.data?.id) {
            q.id = res.data.id;
            q._imported = false;
          } else {
            continue; // 创建失败则跳过
          }
        } else {
          if (q.contentJson) {
            payload.contentJson =
              typeof q.contentJson === 'string' ? q.contentJson : JSON.stringify(q.contentJson);
          }
          await updateQuestion(q.id, payload);
        }
      } else if (q._isNew) {
        payload.contentJson = JSON.stringify({
          batchId: props.batchId,
          type: 'ai_generated',
          text: q.questionText,
        });
        payload.status = 0;
        const res = await createQuestion(payload);
        if (res.code === 200 && res.data?.id) {
          q.id = res.data.id;
          q._isNew = false;
        }
      }
      q._modified = false;
      savedCount++;
    }
    ElMessage.success(`已保存 ${savedCount} 道题目`);
    emit('saved', questions.value);
    emit('closed');
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'));
  } finally {
    saving.value = false;
  }
};

const hasUnsavedChanges = () => questions.value.some((q) => q._modified);
const handleClose = () => {
  saveCurrentEdit();
  const emptyNewCount = questions.value.filter(
    (q) => q._isNew && (!q.questionText || !q.questionText.trim())
  ).length;
  if (emptyNewCount > 0) {
    questions.value = questions.value.filter(
      (q) => !(q._isNew && (!q.questionText || !q.questionText.trim()))
    );
  }
  if (hasUnsavedChanges()) {
    saveAll();
  } else {
    emit('saved', questions.value);
    emit('closed');
  }
};

const doImportQuestions = (selected) => {
  saveCurrentEdit();
  const existingIds = new Set(questions.value.map((q) => q.id).filter(Boolean));
  let importedCount = 0;
  let skippedCount = 0;
  for (const item of selected) {
    if (existingIds.has(item.id)) {
      skippedCount++;
      continue;
    }
    questions.value.push({
      id: item.id,
      questionText: item.questionText,
      questionType: item.questionType,
      options: parseOptions(item.options),
      correctAnswer: item.correctAnswer,
      explanation: item.explanation || '',
      difficultyLevel: item.difficultyLevel || 2,
      subject: item.subject || '',
      categoryId: item.categoryId || null,
      _modified: false, // 已入库的题无需回写 question_bank
      _isNew: false,
      _imported: true, // 标记为从题库导入
    });
    existingIds.add(item.id);
    importedCount++;
  }
  let msg = `已导入 ${importedCount} 道题目`;
  if (skippedCount > 0) msg += `，跳过 ${skippedCount} 道重复`;
  ElMessage.success(msg);
  showImportDialog.value = false;
};

const parseOptions = (opts) => {
  if (!opts) return [];
  if (Array.isArray(opts)) return opts;
  if (typeof opts === 'string') {
    try {
      const parsed = JSON.parse(opts);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  return [];
};

watch(
  () => props.modelValue,
  async (val) => {
    if (!val) return;
    if (props.batchId) {
      try {
        const res = await getQuestionsByBatch(props.batchId);
        if (res.code === 200 && res.data?.length) {
          // 优先用 initialQuestions（含题库混搭题），比batchId查询全
          if (props.initialQuestions?.length > res.data.length) {
            questions.value = JSON.parse(JSON.stringify(props.initialQuestions));
          } else {
            questions.value = res.data.map((q) => ({
              ...q,
              options: parseOptions(q.options),
              _modified: false,
              _isNew: false,
            }));
          }
        } else if (props.initialQuestions?.length) {
          questions.value = JSON.parse(JSON.stringify(props.initialQuestions));
        }
      } catch {
        questions.value = JSON.parse(JSON.stringify(props.initialQuestions));
      }
    } else if (props.initialQuestions?.length) {
      questions.value = JSON.parse(JSON.stringify(props.initialQuestions));
    }
    editingIndex.value = questions.value.length > 0 ? 0 : null;
    editingQuestion.value =
      questions.value.length > 0 ? reactive(JSON.parse(JSON.stringify(questions.value[0]))) : null;
  }
);
</script>

<style scoped>
.qe-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light, #ebeef5);
  flex-wrap: wrap;
}
.qe-count {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary, #1a1a1a);
  margin-right: auto;
}
.qe-body {
  display: flex;
  gap: 16px;
  height: calc(100vh - 180px);
}
.qe-list {
  width: 280px;
  flex-shrink: 0;
  overflow-y: auto;
  border-right: 1px solid var(--border-light, #ebeef5);
  padding-right: 8px;
}
.qe-list-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
}
.qe-list-item:hover {
  background: var(--bg-hover, #f5f7fa);
}
.qe-list-item.active {
  background: var(--primary-light, #ecf0ff);
}
.qe-list-no {
  font-weight: 700;
  color: var(--primary-color);
  flex-shrink: 0;
  min-width: 24px;
}
.qe-list-text {
  flex: 1;
  font-size: var(--fs-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
