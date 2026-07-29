<template>
  <el-dialog
    v-model="visible"
    title="从题库导入题目"
    width="640px"
    append-to-body
    @close="reset"
  >
    <div class="qid-search">
      <el-input
        v-model="importKeyword"
        placeholder="搜索题干关键词"
        clearable
        style="width: 200px; margin-right: 8px"
        @keyup.enter="search(1)"
      />
      <el-select
        v-model="importSubject"
        placeholder="学科"
        clearable
        style="width: 160px; margin-right: 8px"
      >
        <el-option
          v-for="s in subjects"
          :key="s"
          :label="s"
          :value="s"
        />
      </el-select>
      <el-select
        v-model="importType"
        placeholder="题型"
        clearable
        style="width: 110px; margin-right: 8px"
      >
        <el-option label="单选" value="SINGLE_CHOICE" />
        <el-option label="多选" value="MULTI_CHOICE" />
        <el-option label="判断" value="TRUE_FALSE" />
        <el-option label="填空" value="FILL_IN" />
        <el-option label="简答" value="SHORT_ANSWER" />
        <el-option label="论述" value="ESSAY" />
        <el-option label="完形填空" value="CLOZE" />
        <el-option label="计算" value="CALCULATION" />
        <el-option label="证明" value="PROOF" />
        <el-option label="写作" value="COMPOSITION" />
      </el-select>
      <el-button type="primary" size="small" @click="search(1)">搜索</el-button>
    </div>

    <div style="max-height: 400px; overflow-y: auto; margin-top: 12px">
      <div
        v-for="item in bankList"
        :key="item.id"
        class="qid-item"
        :class="{ selected: selected.includes(item.id) }"
        @click="toggle(item.id)"
      >
        <el-checkbox :model-value="selected.includes(item.id)" style="margin-right: 8px" />
        <span class="qid-text">{{ truncateText(item.questionText) }}</span>
        <el-tag size="small" type="info">{{ typeLabel(item.questionType) }}</el-tag>
      </div>
      <div
        v-if="bankList.length === 0 && searched && !loading"
        style="text-align: center; padding: 20px; color: #999"
      >
        未找到匹配题目
      </div>
      <div v-if="loading" style="text-align: center; padding: 20px; color: #999">搜索中...</div>
    </div>

    <!-- 分页 -->
    <div v-if="searched && total > pageSize" class="qid-pagination">
      <el-button size="small" :disabled="page <= 1" @click="search(page - 1)">上一页</el-button>
      <span class="qid-page-info">{{ page }} / {{ Math.ceil(total / pageSize) }} 页（共 {{ total }} 题）</span>
      <el-button
        size="small"
        :disabled="page * pageSize >= total"
        @click="search(page + 1)"
      >
        下一页
      </el-button>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selected.length" @click="doImport">
        导入选中({{ selected.length }})
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { getQuestionBankList } from '@/api/questionBank';
import { useUserStore } from '@/stores/user';

const props = defineProps({
  modelValue: Boolean,
  subject: { type: String, default: '' },
  categoryId: { type: Number, default: null },
});

const emit = defineEmits(['update:modelValue', 'import']);

const userStore = useUserStore();
const subjects = computed(() => userStore.teachingSubjects);

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const importKeyword = ref('');
const importSubject = ref('');
const importType = ref('');
const bankList = ref([]);
const selected = ref([]);
const searched = ref(false);
const loading = ref(false);
const page = ref(1);
const pageSize = 50;
const total = ref(0);

const TYPE_CN = {
  SINGLE_CHOICE: '单选',
  MULTI_CHOICE: '多选',
  TRUE_FALSE: '判断',
  FILL_IN: '填空',
  SHORT_ANSWER: '简答',
  ESSAY: '论述',
  CLOZE: '完形填空',
  CALCULATION: '计算',
  PROOF: '证明',
  COMPOSITION: '写作',
};
const typeLabel = (t) => TYPE_CN[t] || t;

const truncateText = (text) => {
  if (!text) return '';
  const cleaned = text
    .replace(/<[^>]+>/g, '')
    .replace(/^[\d.、)\s]+/, '')
    .trim();
  return cleaned.length > 50 ? cleaned.substring(0, 50) + '...' : cleaned;
};

const search = async (targetPage) => {
  if (targetPage != null) page.value = targetPage;
  loading.value = true;
  try {
    const res = await getQuestionBankList({
      keyword: importKeyword.value || undefined,
      subject: importSubject.value || props.subject || undefined,
      categoryId: props.categoryId || undefined,
      questionType: importType.value || undefined,
      status: 1,
      page: page.value,
      pageSize,
    });
    const data = res.data || {};
    bankList.value = data.records || [];
    total.value = data.total || 0;
    searched.value = true;
    selected.value = [];
  } catch {
    /* 网络错误静默处理 */
  } finally {
    loading.value = false;
  }
};

const toggle = (id) => {
  const idx = selected.value.indexOf(id);
  if (idx >= 0) selected.value.splice(idx, 1);
  else selected.value.push(id);
};

const doImport = () => {
  const items = bankList.value.filter((item) => selected.value.includes(item.id));
  if (!items.length) return;
  emit('import', items);
  ElMessage.success(`已导入 ${items.length} 道题目`);
  visible.value = false;
};

const reset = () => {
  importKeyword.value = '';
  importType.value = '';
  bankList.value = [];
  selected.value = [];
  searched.value = false;
  page.value = 1;
  total.value = 0;
};

// 打开对话框时自动加载默认数据
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      importSubject.value = props.subject || '';
      search();
    }
  }
);
</script>

<style scoped>
.qid-search {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.qid-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
}
.qid-item:hover {
  background: var(--bg-hover, #f5f7fa);
}
.qid-item.selected {
  background: var(--primary-light, #ecf0ff);
}
.qid-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 4px;
  border-top: 1px solid var(--border-light);
  margin-top: 4px;
}
.qid-page-info {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.qid-text {
  flex: 1;
  font-size: var(--fs-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 8px;
}
</style>
