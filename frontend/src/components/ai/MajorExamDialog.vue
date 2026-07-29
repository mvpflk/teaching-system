<template>
  <el-dialog
    v-model="visible"
    title="🏢 专业大类综合卷"
    width="850px"
    :close-on-click-modal="false"
    destroy-on-close
    @closed="reset"
  >
    <!-- Step 1: 选择专业大类 -->
    <div v-if="step === 'config'" class="me-config">
      <div class="me-config-icon">🏢</div>
      <h3>跨学科综合组卷</h3>
      <p class="me-config-desc">按专业大类下所有专业课的考纲知识点比例，自动生成一张综合试卷</p>

      <div class="me-major-row">
        <span class="me-label">专业大类：</span>
        <el-select
          v-model="selectedMajorId"
          placeholder="选择专业大类"
          size="large"
          style="width: 260px"
          :loading="loadingMajors"
          @change="onMajorChange"
        >
          <el-option
            v-for="m in majors"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          />
        </el-select>
      </div>

      <!-- 大类下的学科列表 -->
      <div v-if="majorSubjects.length" class="me-subject-list">
        <div class="me-subject-title">涵盖专业课（共 {{ majorSubjects.length }} 门）</div>
        <div class="me-subject-tags">
          <el-tag
            v-for="s in majorSubjects"
            :key="s.id"
            size="small"
            type="success"
            effect="plain"
          >
            {{ s.name }} <span style="opacity: 0.6; font-size: 10px">({{ s.nodeCount }}节点)</span>
          </el-tag>
        </div>
        <div class="me-subject-hint">各学科题目比例按知识点数量自动分配，考纲分值优先</div>
      </div>

      <!-- 题型配置（可调） -->
      <div v-if="majorSubjects.length" class="me-type-config">
        <div class="me-subject-title">题型与数量</div>
        <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 10px">
          <div v-for="t in defaultTypes" :key="t.key" class="me-type-item">
            <span class="me-type-label">{{ t.label }}</span>
            <el-input-number
              v-model="typeCounts[t.key]"
              :min="0"
              :max="50"
              size="small"
              style="width: 70px"
              controls-position="right"
            />
            题
          </div>
        </div>
        <span class="me-type-total">共计 {{ totalCount }} 题</span>
      </div>

      <div class="me-actions">
        <el-button
          size="large"
          type="primary"
          :disabled="!selectedMajorId || totalCount <= 0"
          :loading="generating"
          @click="startGenerate"
        >
          🚀 开始生成综合卷
        </el-button>
      </div>
    </div>

    <!-- Step 2: 生成中 -->
    <div v-else-if="step === 'generating'" class="me-loading">
      <div class="me-loading-icon">⏳</div>
      <h3>AI 正在跨学科组卷…</h3>
      <p class="me-loading-desc">
        合并 {{ majorSubjects.length }} 门专业课的知识体系{{ batchProgress }}
      </p>
      <div class="me-skeleton">
        <div
          v-for="i in 6"
          :key="i"
          class="sk-line"
          :style="{ width: 85 - i * 10 + '%' }"
        ></div>
      </div>
    </div>

    <!-- Step 3: 结果展示（复用 QuickExamDialog 的结果模式） -->
    <div v-else-if="step === 'result'" class="me-result">
      <div class="me-result-top">
        <el-tag type="success" size="small">{{ majorName }} · 综合卷</el-tag>
        <span class="me-result-count">共 {{ questions.length }} 题</span>
        <span class="me-result-meta">耗时 {{ elapsed }}s · AI 生成</span>
      </div>

      <div v-if="questions.length" class="me-question-list">
        <div
          v-for="(q, i) in questions"
          :key="i"
          class="me-q-item"
          :class="{ 'me-q-editing': editingIndex === i }"
        >
          <div v-if="editingIndex !== i" class="me-q-header" @click="toggleExpand(i)">
            <span class="me-q-num">{{ i + 1 }}.</span>
            <el-tag :type="typeTag(q.questionType)" size="small">
              {{ typeLabel(q.questionType) }}
            </el-tag>
            <span class="me-q-text" v-html="renderMath(q.questionText)" />
            <span v-if="q.correctAnswer" class="me-q-answer-hint">答案: <span v-html="renderMath(q.correctAnswer)" /></span>
            <span class="me-q-opts">
              <el-button
                text
                size="small"
                type="primary"
                @click.stop="startEdit(i)"
              >✏️</el-button>
              <el-button
                text
                size="small"
                type="danger"
                @click.stop="removeQuestion(i)"
              >🗑</el-button>
            </span>
          </div>
          <div v-if="expandedIndex === i && editingIndex !== i" class="me-q-body">
            <div v-if="q.options && q.options.length" class="me-q-options">
              <div
                v-for="(opt, j) in parseOptions(q.options)"
                :key="j"
                class="me-opt"
                :class="{ 'me-opt-correct': String.fromCharCode(65 + j) === q.correctAnswer }"
              >
                {{ String.fromCharCode(65 + j) }}. <span v-html="renderMath(opt)" />
              </div>
            </div>
            <div v-if="q.explanation" class="me-q-explanation">解析：<span v-html="renderMath(q.explanation)" /></div>
          </div>
          <div v-if="editingIndex === i" class="me-q-edit">
            <el-input
              v-model="editForm.questionText"
              type="textarea"
              :rows="2"
              size="small"
            />
            <div v-if="editForm.options" class="me-edit-options">
              <div v-for="(opt, j) in editForm.options" :key="j" class="me-edit-opt-row">
                <span>{{ String.fromCharCode(65 + j) }}.</span>
                <el-input v-model="editForm.options[j]" size="small" />
              </div>
            </div>
            <div class="me-edit-row">
              <span>答案</span><el-input v-model="editForm.correctAnswer" size="small" style="width: 80px" />
              <span>解析</span><el-input v-model="editForm.explanation" size="small" />
            </div>
            <div class="me-edit-actions">
              <el-button size="small" type="primary" @click="confirmEdit(i)">确认</el-button>
              <el-button size="small" @click="cancelEdit">取消</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="生成失败，请重试" :image-size="80" />

      <div v-if="questions.length" class="me-result-actions">
        <span class="me-save-hint">修改后点击保存写入题库</span>
        <el-button :loading="generating" @click="startGenerate">🔄 重新生成</el-button>
        <el-button type="success" :loading="saving" @click="saveToBank">💾 保存到题库</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { submitMajorExam, getMajors, getMajorSubjects } from '@/api/ai';
import { pollAiTask } from '@/api/ai';
import { batchSaveQuestions } from '@/api/questionBank';

const props = defineProps({ modelValue: Boolean });
const emit = defineEmits(['update:modelValue']);

const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) });

const step = ref('config');
const majors = ref([]);
const loadingMajors = ref(false);
const selectedMajorId = ref(null);
const majorName = ref('');
const majorSubjects = ref([]);
const generating = ref(false);
const saving = ref(false);
const questions = ref([]);
const elapsed = ref(0);
const expandedIndex = ref(null);
const editingIndex = ref(null);
const editForm = ref({});

const defaultTypes = [
  { key: 'SINGLE_CHOICE', label: '单选' },
  { key: 'MULTI_CHOICE', label: '多选' },
  { key: 'TRUE_FALSE', label: '判断' },
  { key: 'FILL_IN', label: '填空' },
  { key: 'SHORT_ANSWER', label: '简答' },
];
const typeCounts = ref({
  SINGLE_CHOICE: 10,
  MULTI_CHOICE: 5,
  TRUE_FALSE: 5,
  FILL_IN: 5,
  SHORT_ANSWER: 3,
});
const totalCount = computed(() => Object.values(typeCounts.value).reduce((a, b) => a + b, 0));

const batchProgress = computed(() => {
  const t = totalCount.value;
  if (t <= 25) return `，预计 15~30 秒`;
  const batches = Math.ceil(t / 25);
  return `，分 ${batches} 批生成（共 ${t} 题），预计 ${batches * 20}~${batches * 40} 秒`;
});

import { typeLabel, typeTag, parseOptions } from '@/utils/questionHelpers';
import { renderMath } from '@/composables/useQuestionHelpers';

watch(
  () => props.modelValue,
  async (v) => {
    if (!v) return;
    step.value = 'config';
    questions.value = [];
    loadingMajors.value = true;
    try {
      const r = await getMajors();
      majors.value = r.data || [];
    } catch {
      majors.value = [];
    } finally {
      loadingMajors.value = false;
    }
  }
);

const onMajorChange = async (id) => {
  majorSubjects.value = [];
  if (!id) return;
  const m = majors.value.find((x) => x.id === id);
  majorName.value = m?.name || '';
  try {
    const r = await getMajorSubjects(id);
    majorSubjects.value = r.data || [];
  } catch {
    majorSubjects.value = [];
  }
};

const startGenerate = async () => {
  if (!selectedMajorId.value) {
    ElMessage.warning('请选择专业大类');
    return;
  }
  generating.value = true;
  step.value = 'generating';
  const start = Date.now();
  try {
    const { result } = await pollAiTask(
      () => submitMajorExam(selectedMajorId.value, { ...typeCounts.value }),
      { timeout: 300000 }
    );
    questions.value = result?.questions || [];
    elapsed.value = ((Date.now() - start) / 1000).toFixed(1);
    step.value = 'result';
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
};

const saveToBank = async () => {
  saving.value = true;
  try {
    const r = await batchSaveQuestions({
      questions: questions.value,
      subject: majorName.value + '综合',
    });
    if (r.code === 200) {
      ElMessage.success(`已保存 ${questions.value.length} 题`);
      visible.value = false;
    }
  } catch {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const reset = () => {
  step.value = 'config';
  questions.value = [];
  majorSubjects.value = [];
  editingIndex.value = null;
  expandedIndex.value = null;
};
</script>

<style scoped>
.me-config {
  text-align: center;
  padding: 16px 0;
}
.me-config-icon {
  font-size: 52px;
  margin-bottom: 8px;
}
.me-config h3 {
  margin: 0 0 6px;
  font-size: var(--fs-lg);
}
.me-config-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 20px;
}
.me-major-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}
.me-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  font-weight: 500;
}
.me-subject-list {
  margin: 0 auto 16px;
  max-width: 600px;
  text-align: left;
  background: var(--bg-section);
  padding: 14px;
  border-radius: 8px;
}
.me-subject-title {
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.me-subject-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.me-subject-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.me-type-config {
  margin: 0 auto 20px;
  max-width: 600px;
  text-align: left;
  background: var(--bg-section);
  padding: 14px;
  border-radius: 8px;
}
.me-type-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.me-type-label {
  font-size: var(--fs-xs);
  min-width: 40px;
  color: var(--text-secondary);
}
.me-type-total {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  font-weight: 600;
}
.me-actions {
  margin-top: 8px;
}

.me-loading {
  text-align: center;
  padding: 30px 0;
}
.me-loading-icon {
  font-size: 48px;
  animation: me-spin 1.5s linear infinite;
}
@keyframes me-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
.me-loading h3 {
  margin: 12px 0 4px;
}
.me-loading-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 20px;
}
.me-skeleton {
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

.me-result-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
}
.me-result-count {
  font-size: var(--fs-sm);
  font-weight: 600;
}
.me-result-meta {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-left: auto;
}

.me-question-list {
  max-height: 420px;
  overflow-y: auto;
  border: 0.5px solid var(--border-light);
  border-radius: 8px;
  margin-bottom: 14px;
}
.me-q-item {
  border-bottom: 0.5px solid var(--border-light);
  background: var(--bg-card);
}
.me-q-item:last-child {
  border-bottom: none;
}
.me-q-item.me-q-editing {
  background: #fffbe6;
}
.me-q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: var(--fs-sm);
}
.me-q-header:hover {
  background: var(--bg-section);
}
.me-q-num {
  font-weight: 700;
  min-width: 22px;
}
.me-q-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.me-q-answer-hint {
  font-size: var(--fs-xs);
  color: var(--el-color-success);
  background: #f0fdf4;
  padding: 1px 8px;
  border-radius: 4px;
}
.me-q-opts {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.me-q-body {
  padding: 10px 14px 12px 48px;
  background: var(--bg-section);
}
.me-q-options {
  margin-bottom: 6px;
}
.me-opt {
  font-size: var(--fs-xs);
  padding: 2px 0;
  color: var(--text-secondary);
}
.me-opt-correct {
  color: var(--el-color-success);
  font-weight: 600;
}
.me-q-explanation {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.me-q-edit {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.me-edit-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.me-edit-opt-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.me-edit-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.me-edit-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}
.me-result-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.me-save-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-right: auto;
}
</style>
