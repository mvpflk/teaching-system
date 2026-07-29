<template>
  <el-dialog
    v-model="visible"
    title="⚡ 一键示例卷"
    width="800px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="reset"
  >
    <!-- Step 1: 确认学科 -->
    <div v-if="step === 'config'" class="qe-config">
      <div class="qe-config-icon">⚡</div>
      <h3>自动识别学科，一键生成示例试卷</h3>
      <p class="qe-config-desc">系统将按学科特点自动选择题型组合，无需手动配置</p>
      <div class="qe-subject-row">
        <span class="qe-label">任教学科：</span>
        <el-select
          v-model="selectedSubject"
          placeholder="选择学科"
          size="large"
          style="width: 260px"
          :loading="loadingSubjects"
        >
          <el-option
            v-for="s in subjects"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>
      </div>
      <div v-if="selectedSubject" class="qe-template-preview">
        <span class="qe-label">题型组合：</span>
        <span class="qe-template-text">{{ templateDesc }}</span>
      </div>
      <div class="qe-actions">
        <el-button
          size="large"
          type="primary"
          :loading="generating"
          @click="startGenerate"
        >
          🚀 开始生成
        </el-button>
      </div>
    </div>

    <!-- Step 2: 生成中 -->
    <div v-else-if="step === 'generating'" class="qe-loading">
      <div class="qe-loading-icon">⏳</div>
      <h3>AI 正在生成试卷…</h3>
      <p class="qe-loading-desc">根据「{{ selectedSubject }}」学科模板，预计 3~8 秒</p>
      <div class="qe-skeleton">
        <div
          v-for="i in 5"
          :key="i"
          class="sk-line"
          :style="{ width: 80 - i * 10 + '%' }"
        ></div>
      </div>
    </div>

    <!-- Step 3: 结果展示 -->
    <div v-else-if="step === 'result'" class="qe-result">
      <!-- 顶栏 -->
      <div class="qe-result-top">
        <el-tag type="primary" size="small">{{ selectedSubject }}</el-tag>
        <span class="qe-result-count">共 {{ questions.length }} 题</span>
        <span class="qe-result-types">{{ typeSummary }}</span>
        <span class="qe-result-meta">耗时 {{ elapsed }}s · AI 生成</span>
      </div>

      <!-- 题目列表 -->
      <div v-if="questions.length" class="qe-question-list">
        <div
          v-for="(q, i) in questions"
          :key="i"
          class="qe-q-item"
          :class="{ 'qe-q-editing': editingIndex === i }"
        >
          <!-- 正常态 -->
          <div v-if="editingIndex !== i" class="qe-q-header" @click="toggleExpand(i)">
            <span class="qe-q-num">{{ i + 1 }}.</span>
            <el-tag :type="typeTag(q.questionType)" size="small">
              {{
                typeLabel(q.questionType)
              }}
            </el-tag>
            <span class="qe-q-text" v-html="renderMath(q.questionText)" />
            <span v-if="q.correctAnswer" class="qe-q-answer-hint">答案: <span v-html="renderMath(q.correctAnswer)" /></span>
            <span class="qe-q-opts">
              <el-button
                text
                size="small"
                type="primary"
                @click.stop="startEdit(i)"
              >✏️ 编辑</el-button>
              <el-button
                text
                size="small"
                type="danger"
                @click.stop="removeQuestion(i)"
              >🗑</el-button>
            </span>
          </div>

          <!-- 展开详情 -->
          <div v-if="expandedIndex === i && editingIndex !== i" class="qe-q-body">
            <div v-if="q.options && q.options.length" class="qe-q-options">
              <div
                v-for="(opt, j) in parseOptions(q.options)"
                :key="j"
                class="qe-opt"
                :class="{ 'qe-opt-correct': String.fromCharCode(65 + j) === q.correctAnswer }"
              >
                {{ String.fromCharCode(65 + j) }}. <span v-html="renderMath(opt)" />
              </div>
            </div>
            <div v-if="q.explanation" class="qe-q-explanation">
              <span class="qe-label-sm">解析：</span><span v-html="renderMath(q.explanation)" />
            </div>
          </div>

          <!-- 编辑态 -->
          <div v-if="editingIndex === i" class="qe-q-edit">
            <div class="qe-edit-field">
              <span class="qe-edit-label">题干</span>
              <el-input
                v-model="editForm.questionText"
                type="textarea"
                :rows="2"
                size="small"
              />
            </div>
            <div v-if="editForm.options && editForm.options.length" class="qe-edit-field">
              <span class="qe-edit-label">选项</span>
              <div class="qe-edit-options">
                <div v-for="(opt, j) in editForm.options" :key="j" class="qe-edit-opt-row">
                  <span class="qe-edit-opt-letter">{{ String.fromCharCode(65 + j) }}.</span>
                  <el-input v-model="editForm.options[j]" size="small" />
                </div>
              </div>
            </div>
            <div class="qe-edit-row">
              <span class="qe-edit-label">答案</span>
              <el-input v-model="editForm.correctAnswer" size="small" style="width: 80px" />
              <span class="qe-edit-label" style="margin-left: 16px">解析</span>
              <el-input v-model="editForm.explanation" size="small" />
            </div>
            <div class="qe-edit-actions">
              <el-button size="small" type="primary" @click="confirmEdit(i)">✓ 确认</el-button>
              <el-button size="small" @click="cancelEdit">取消</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="生成失败，请重试" :image-size="80" />

      <!-- 底部操作栏 -->
      <div v-if="questions.length" class="qe-result-actions">
        <span class="qe-save-hint">
          <el-tooltip content="仅保存到题库，不创建考试任务" placement="top">
            <el-icon><QuestionFilled /></el-icon>
          </el-tooltip>
          修改后保存到题库，或重新生成换一套
        </span>
        <el-button :loading="generating" @click="startGenerate">🔄 重新生成</el-button>
        <el-button type="default" :loading="saving" @click="saveToBank">💾 仅保存到题库</el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="saveAndCreateTask"
        >
          📋 保存并发布考试
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { QuestionFilled } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { submitQuickExam, getQuickExamSubjects, pollAiTask } from '@/api/ai';
import { batchSaveQuestions } from '@/api/questionBank';

const props = defineProps({ modelValue: Boolean });
const emit = defineEmits(['update:modelValue']);
const router = useRouter();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const step = ref('config');
const subjects = ref([]);
const loadingSubjects = ref(false);
const selectedSubject = ref('');
const generating = ref(false);
const saving = ref(false);
const questions = ref([]);
const elapsed = ref(0);
const expandedIndex = ref(null);
const editingIndex = ref(null);
const editForm = ref({});

const TEMPLATES = {
  语文: {
    desc: '单选×2 + 判断×2 + 填空×1 + 微阅读×1（6题）',
    types: ['SINGLE_CHOICE', 'TRUE_FALSE', 'FILL_IN', 'READING_COMPREHENSION'],
  },
  数学: {
    desc: '单选×2 + 判断×1 + 填空×1 + 计算×2（6题）',
    types: ['SINGLE_CHOICE', 'TRUE_FALSE', 'FILL_IN', 'CALCULATION'],
  },
  英语: {
    desc: '单选×2 + 完形×1 + 阅读×1 + 翻译填空×2（6题）',
    types: ['SINGLE_CHOICE', 'CLOZE', 'READING_COMPREHENSION', 'FILL_IN'],
  },
};


const templateDesc = computed(() => {
  for (const [key, t] of Object.entries(TEMPLATES)) {
    if (selectedSubject.value.includes(key)) return t.desc;
  }
  return '单选×3 + 判断×2 + 填空×1（6题）— 专业课模板';
});

const typeSummary = computed(() => {
  const map = {};
  questions.value.forEach((q) => {
    const t = q.questionType || '未知';
    map[t] = (map[t] || 0) + 1;
  });
  return Object.entries(map)
    .map(([k, v]) => `${TYPE_LABEL[k] || k}×${v}`)
    .join(' · ');
});

import { TYPE_LABEL, typeLabel, typeTag, parseOptions } from '@/utils/questionHelpers';
import { renderMath } from '@/composables/useQuestionHelpers';

watch(
  () => props.modelValue,
  async (v) => {
    if (!v) return;
    step.value = 'config';
    questions.value = [];
    editingIndex.value = null;
    expandedIndex.value = null;
    loadingSubjects.value = true;
    try {
      const r = await getQuickExamSubjects();
      subjects.value = (r.data || []).filter(Boolean);
      if (subjects.value.length) selectedSubject.value = subjects.value[0];
    } catch {
      subjects.value = [];
    } finally {
      loadingSubjects.value = false;
    }
  }
);

const startGenerate = async () => {
  if (!selectedSubject.value) {
    ElMessage.warning('请选择学科');
    return;
  }
  generating.value = true;
  step.value = 'generating';
  const start = Date.now();
  try {
    const { result } = await pollAiTask(() => submitQuickExam(selectedSubject.value), {
      timeout: 60000,
    });
    questions.value = result?.questions || [];
    elapsed.value = ((Date.now() - start) / 1000).toFixed(1);
    step.value = 'result';
    expandedIndex.value = null;
    editingIndex.value = null;
  } catch (e) {
    ElMessage.error(e.message || '生成失败');
    step.value = 'config';
  } finally {
    generating.value = false;
  }
};

const toggleExpand = (i) => {
  expandedIndex.value = expandedIndex.value === i ? null : i;
};
const startEdit = (i) => {
  editingIndex.value = i;
  expandedIndex.value = i;
  editForm.value = { ...questions.value[i], options: parseOptions(questions.value[i].options) };
};
const cancelEdit = () => {
  editingIndex.value = null;
  editForm.value = {};
};
const confirmEdit = (i) => {
  questions.value[i] = { ...questions.value[i], ...editForm.value };
  editingIndex.value = null;
  editForm.value = {};
  ElMessage.success('已修改');
};
const removeQuestion = (i) => {
  questions.value.splice(i, 1);
  if (editingIndex.value === i) cancelEdit();
};

const saveToBank = async () => {
  saving.value = true;
  try {
    const r = await batchSaveQuestions({
      questions: questions.value,
      subject: selectedSubject.value,
    });
    if (r.code === 200) {
      ElMessage.success(`已保存 ${questions.value.length} 题到题库`);
      visible.value = false;
    }
  } catch (e) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const saveAndCreateTask = async () => {
  saving.value = true;
  try {
    const r = await batchSaveQuestions({
      questions: questions.value,
      subject: selectedSubject.value,
    });
    if (r.code === 200) {
      ElMessage.success(`已保存 ${questions.value.length} 题`);
      visible.value = false;
      const ids = (r.data?.ids || []).join(',');
      router.push({
        path: '/teacher/task/create',
        query: { questionIds: ids, subject: selectedSubject.value },
      });
    }
  } catch (e) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const reset = () => {
  step.value = 'config';
  questions.value = [];
  editingIndex.value = null;
  expandedIndex.value = null;
};
</script>

<style scoped>
/* Step 1 */
.qe-config {
  text-align: center;
  padding: 20px 0;
}
.qe-config-icon {
  font-size: 56px;
  margin-bottom: 12px;
}
.qe-config h3 {
  margin: 0 0 8px;
  font-size: var(--fs-lg);
}
.qe-config-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 24px;
}
.qe-subject-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}
.qe-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  font-weight: 500;
}
.qe-template-preview {
  margin-bottom: 24px;
}
.qe-template-text {
  font-size: var(--fs-sm);
  background: var(--bg-section);
  padding: 6px 14px;
  border-radius: 6px;
}
.qe-actions {
  margin-top: 8px;
}

/* Step 2 */
.qe-loading {
  text-align: center;
  padding: 30px 0;
}
.qe-loading-icon {
  font-size: 48px;
  animation: qe-spin 1.5s linear infinite;
}
@keyframes qe-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
.qe-loading h3 {
  margin: 12px 0 4px;
}
.qe-loading-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 24px;
}
.qe-skeleton {
  max-width: 400px;
  margin: 0 auto;
}
.sk-line {
  height: 14px;
  border-radius: 4px;
  margin: 0 auto 10px;
  background: linear-gradient(
    90deg,
    var(--bg-section) 25%,
    var(--bg-card) 50%,
    var(--bg-section) 75%
  );
  background-size: 200% 100%;
  animation: sk-shimmer 1.5s infinite;
}
@keyframes sk-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* Step 3 */
.qe-result-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
}
.qe-result-count {
  font-size: var(--fs-sm);
  font-weight: 600;
}
.qe-result-types {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.qe-result-meta {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-left: auto;
}

.qe-question-list {
  max-height: 420px;
  overflow-y: auto;
  border: 0.5px solid var(--border-light);
  border-radius: 8px;
  margin-bottom: 14px;
}
.qe-q-item {
  border-bottom: 0.5px solid var(--border-light);
  background: var(--bg-card);
}
.qe-q-item:last-child {
  border-bottom: none;
}
.qe-q-item.qe-q-editing {
  background: #fffbe6;
}

.qe-q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: var(--fs-sm);
}
.qe-q-header:hover {
  background: var(--bg-section);
}
.qe-q-num {
  font-weight: 700;
  min-width: 22px;
}
.qe-q-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.qe-q-answer-hint {
  font-size: var(--fs-xs);
  color: var(--el-color-success);
  background: #f0fdf4;
  padding: 1px 8px;
  border-radius: 4px;
  white-space: nowrap;
}
.qe-q-opts {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.qe-q-body {
  padding: 10px 14px 12px 48px;
  background: var(--bg-section);
}
.qe-q-options {
  margin-bottom: 6px;
}
.qe-opt {
  font-size: var(--fs-xs);
  padding: 2px 0;
  color: var(--text-secondary);
}
.qe-opt-correct {
  color: var(--el-color-success);
  font-weight: 600;
}
.qe-q-explanation {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  line-height: 1.6;
}
.qe-label-sm {
  font-weight: 500;
  color: var(--text-primary);
}

.qe-q-edit {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.qe-edit-field {
}
.qe-edit-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  font-weight: 500;
  display: block;
  margin-bottom: 2px;
}
.qe-edit-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.qe-edit-opt-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.qe-edit-opt-letter {
  font-size: var(--fs-xs);
  font-weight: 600;
  min-width: 22px;
  color: var(--text-secondary);
}
.qe-edit-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qe-edit-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.qe-result-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.qe-save-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-right: auto;
}
</style>
