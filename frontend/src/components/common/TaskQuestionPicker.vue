<template>
  <div class="question-picker">
    <div class="picker-toolbar">
      <el-input
        v-model="search"
        placeholder="搜索题目..."
        size="small"
        clearable
        style="width: 140px"
        @input="load"
      />
      <el-select
        v-model="categoryId"
        placeholder="分类"
        size="small"
        clearable
        style="width: 110px"
        @change="load"
      >
        <el-option
          v-for="c in categories"
          :key="c.id"
          :value="c.id"
          :label="c.name"
        />
      </el-select>
      <el-select
        v-model="questionType"
        placeholder="题型"
        size="small"
        clearable
        style="width: 100px"
        @change="load"
      >
        <el-option
          v-for="(label, key) in QUESTION_TYPE_LABEL"
          :key="key"
          :value="key"
          :label="label"
        />
      </el-select>
      <el-tag type="primary" size="small">{{ modelValue.length }}题已选</el-tag>
      <el-button v-if="modelValue.length" size="small" @click="showPreview">
        <el-icon><View /></el-icon> 预览已选题
      </el-button>
    </div>
    <div v-loading="qLoading" class="picker-list">
      <div v-if="questionList.length === 0" class="picker-empty">暂无题目，请先在题库中添加</div>
      <el-checkbox-group :model-value="modelValue" @update:model-value="onCheckChange">
        <div
          v-for="q in questionList"
          :key="q.id"
          class="picker-item"
          :class="{ 'picker-selected': modelValue.includes(q.id) }"
        >
          <el-checkbox :value="q.id">
            {{ q.questionText?.substring(0, 60)
            }}{{ q.questionText?.length > 60 ? '...' : '' }}
          </el-checkbox>
          <el-tag size="small" type="info">
            {{
              QUESTION_TYPE_LABEL[q.questionType] || q.questionType
            }}
          </el-tag>
          <el-button
            size="small"
            text
            type="primary"
            class="picker-detail-btn"
            @click.stop="showDetail(q)"
          >
            <el-icon><View /></el-icon> 详情
          </el-button>
        </div>
      </el-checkbox-group>
    </div>
    <div v-if="qTotal > qPageSize" style="margin-top: 8px; display: flex; justify-content: center">
      <el-pagination
        v-model:current-page="qPage"
        :page-size="qPageSize"
        :total="qTotal"
        layout="prev, pager, next"
        small
        @current-change="() => load(false)"
      />
    </div>

    <!-- 题目详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="题目详情"
      width="640px"
      :close-on-click-modal="false"
      append-to-body
    >
      <template v-if="detail">
        <QuestionRenderer
          :question="detail"
          mode="display"
          show-answer
          show-explanation
          highlight-correct
        />
      </template>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 已选题预览 -->
    <el-dialog
      v-model="previewVisible"
      v-loading="previewLoading"
      title="已选题目预览"
      width="700px"
      append-to-body
    >
      <div
        v-if="selectedQuestions.length === 0"
        style="text-align: center; padding: 20px; color: var(--text-secondary)"
      >
        暂未选题
      </div>
      <div v-for="(q, i) in selectedQuestions" :key="q.id" class="preview-item">
        <div class="pi-header">
          <span class="pi-idx">{{ i + 1 }}.</span>
          <el-tag size="small">{{ QUESTION_TYPE_LABEL[q.questionType] || q.questionType }}</el-tag>
          <el-button
            size="small"
            type="danger"
            text
            @click="removeQuestion(q.id)"
          >
            <el-icon><Delete /></el-icon> 移除
          </el-button>
        </div>
        <QuestionRenderer
          :question="q"
          mode="display"
          show-answer
          highlight-correct
        />
      </div>
      <template #footer>
        <div class="pi-footer">
          <span class="pi-footer-text">共 {{ selectedQuestions.length }} 题</span>
          <el-button @click="previewVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { View, Delete } from '@element-plus/icons-vue';
import { getQuestionBankList } from '@/api/questionBank';
import { getNodeList } from '@/api/knowledgeNode';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';

const props = defineProps({ modelValue: { type: Array, default: () => [] } });
const emit = defineEmits(['update:modelValue', 'selection-change']);

/** 同步更新 modelValue 并传递完整题目数据给父组件 */
const onCheckChange = (newIds) => {
  const questions = {};
  const listMap = new Map(questionList.value.map((q) => [q.id, q]));
  for (const id of newIds) {
    if (listMap.has(id)) {
      questions[id] = { ...listMap.get(id) };
      selectedCache.value[id] = questions[id];
    } else if (selectedCache.value[id]) {
      questions[id] = selectedCache.value[id];
    }
  }
  // 清理已取消选择的缓存
  for (const id of Object.keys(selectedCache.value)) {
    if (!newIds.includes(Number(id))) delete selectedCache.value[id];
  }
  emit('update:modelValue', newIds);
  emit('selection-change', { ids: newIds, questions });
};

const DIFFICULTY_LABEL = { 1: '★', 2: '★★', 3: '★★★', 4: '★★★★', 5: '★★★★★' };

const search = ref('');
const categoryId = ref(null);
const questionType = ref('');
const categories = ref([]);
const questionList = ref([]);
const qLoading = ref(false);
const qPage = ref(1);
const qPageSize = ref(20);
const qTotal = ref(0);
/** 已选题目的完整缓存（跨题型/分类切换不丢失） */
const selectedCache = ref({});
const detailVisible = ref(false);
const detail = ref(null);

let loadTimer = null;
const load = (resetPage = true) => {
  clearTimeout(loadTimer);
  loadTimer = setTimeout(async () => {
    qLoading.value = true;
    try {
      if (resetPage) qPage.value = 1;
      const params = { page: qPage.value, pageSize: qPageSize.value };
      if (search.value) params.keyword = search.value;
      if (categoryId.value) params.categoryId = categoryId.value;
      if (questionType.value) params.questionType = questionType.value;
      const r = await getQuestionBankList(params);
      if (r.code === 200) {
        questionList.value = r.data?.records || [];
        qTotal.value = r.data?.total || 0;
      }
      // 将新加载的题目合并到已选缓存（保证跨题型切换已选题不丢失）
      for (const q of questionList.value) {
        if (props.modelValue.includes(q.id)) {
          selectedCache.value[q.id] = { ...selectedCache.value[q.id], ...q };
        }
      }
      // 同步缓存：清理已取消选择的题目
      syncCache();
    } catch {
      ElMessage.error('加载题目失败');
    } finally {
      qLoading.value = false;
    }
  }, 300);
};

const parsedOptions = computed(() => {
  if (!detail.value?.options) return [];
  try {
    return JSON.parse(detail.value.options);
  } catch {
    return [];
  }
});

/** 解析选项 JSON — 给预览模板用 */
const stripOpt = (opt) => (opt || '').replace(/^[A-Z][.、．)\-：:\s]{1,2}/, '').trim();
const parseOptions = (opts) => {
  if (!opts) return [];
  if (typeof opts === 'string')
    try {
      return JSON.parse(opts);
    } catch {
      return [];
    }
  return Array.isArray(opts) ? opts : [];
};
const isCorrect = (q, idx) => {
  if (!q?.correctAnswer) return false;
  const letter = String.fromCharCode(65 + idx);
  return q.correctAnswer
    .trim()
    .toUpperCase()
    .split(',')
    .map((s) => s.trim())
    .includes(letter);
};

const isCorrectOption = (opt, idx) => {
  if (!detail.value?.correctAnswer) return false;
  const ans = detail.value.correctAnswer.trim();
  if (detail.value.questionType === 'TRUE_FALSE') {
    return opt.startsWith(ans);
  }
  if (['SINGLE_CHOICE', 'MULTI_CHOICE'].includes(detail.value.questionType)) {
    const letter = String.fromCharCode(65 + idx);
    return ans
      .split(',')
      .map((s) => s.trim().toUpperCase())
      .includes(letter);
  }
  return false;
};

const previewVisible = ref(false);
const previewLoading = ref(false);
const selectedQuestions = ref([]);

const showPreview = async () => {
  if (props.modelValue.length === 0) {
    selectedQuestions.value = [];
    previewVisible.value = true;
    return;
  }
  previewLoading.value = true;
  try {
    const missing = props.modelValue.filter((id) => !selectedCache.value[id]);
    if (missing.length > 0) {
      let page = 1;
      while (missing.length > 0 && page <= 10) {
        const r = await getQuestionBankList({ page, pageSize: 200 });
        if (r.code === 200) {
          for (const q of r.data?.records || []) {
            selectedCache.value[q.id] = { ...selectedCache.value[q.id], ...q };
            const midx = missing.indexOf(q.id);
            if (midx >= 0) missing.splice(midx, 1);
          }
          if ((r.data?.records || []).length < 200) break;
        } else break;
        page++;
      }
    }
    selectedQuestions.value = props.modelValue.map((id) => selectedCache.value[id]).filter(Boolean);
  } catch {
    selectedQuestions.value = [];
  } finally {
    previewLoading.value = false;
    previewVisible.value = true;
  }
};
const removeQuestion = (id) => {
  emit(
    'update:modelValue',
    props.modelValue.filter((v) => v !== id)
  );
};
const showDetail = (q) => {
  detail.value = q;
  detailVisible.value = true;
};

const syncCache = () => {
  for (const id of Object.keys(selectedCache.value)) {
    if (!props.modelValue.includes(Number(id))) delete selectedCache.value[id];
  }
};
// 外部清空 modelValue 时同步清缓存（正常选中通过 onCheckChange 同步处理）
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal.length === 0) selectedCache.value = {};
  }
);
defineExpose({
  selectedCache,
  questionList,
  clearCache: () => {
    selectedCache.value = {};
  },
});

onMounted(async () => {
  try {
    const r = await getNodeList();
    if (r.code === 200) categories.value = (r.data || []).filter((c) => c.parentId != null);
  } catch {
    ElMessage.error('加载分类失败');
  }
  load();
});
</script>

<style scoped lang="scss">
.question-picker {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  .picker-toolbar {
    display: flex;
    gap: 10px;
    align-items: center;
    padding: 10px 12px;
    border-bottom: 1px solid var(--border-light);
    background: var(--bg-section);
  }
  .picker-count {
    font-size: var(--fs-sm);
    color: var(--text-secondary);
    margin-left: auto;
  }
  .picker-list {
    max-height: 340px;
    overflow-y: auto;
    padding: 8px 12px;
  }
  .picker-empty {
    text-align: center;
    padding: 24px 0;
    color: var(--text-secondary);
    font-size: var(--fs-sm);
  }
  .picker-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
  }
}

.qd-section {
  margin-bottom: 16px;
}
.qd-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  font-weight: 500;
  margin-bottom: 6px;
}
.qd-text {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  line-height: 1.8;
  white-space: pre-wrap;
}
.qd-meta {
  margin-bottom: 16px;
}
.qd-option {
  padding: 6px 12px;
  margin-bottom: 4px;
  background: var(--bg-section);
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
  color: var(--text-primary);
  &.is-correct {
    background: var(--bg-success-light);
    color: var(--el-color-success);
    font-weight: 600;
  }
}
.qd-answer {
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--el-color-success);
}

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}

.picker-detail-btn {
  margin-left: auto;
  font-weight: 500;
  color: var(--primary-color) !important;
  &:hover {
    background: var(--primary-light, #ecf0ff);
  }
}
.picker-selected {
  background: var(--primary-light, #e6f0ff);
  border-left: 3px solid var(--primary-color);
}
.preview-item {
  border-bottom: 1px solid var(--border-light);
  padding: 10px 0;
}
.pi-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.pi-idx {
  font-weight: 600;
}
.pi-body {
  font-size: var(--fs-md);
  line-height: 1.6;
  margin-bottom: 4px;
}
.pi-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.pi-opt {
  font-size: var(--fs-xs);
  padding: 3px 8px;
  background: var(--bg-section);
  border-radius: 4px;
}
.opt-letter {
  font-weight: 600;
}
.pi-answer {
  font-size: var(--fs-xs);
  color: var(--el-color-success);
}
.pi-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.pi-footer-text {
  color: var(--text-secondary);
  font-size: var(--fs-xs);
}
</style>
