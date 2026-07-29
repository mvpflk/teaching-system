<template>
  <el-drawer
    :model-value="modelValue"
    title="试题篮"
    direction="rtl"
    size="420px"
    @update:model-value="$emit('update:modelValue', $event)"
    @open="
      basket.hydrate();
      refreshDrafts();
    "
  >
    <div v-if="!basket.count" class="basket-empty">
      <el-empty description="暂无任何试题，去题库页挑选吧" />
    </div>
    <template v-else>
      <div class="basket-content">
        <el-alert
          v-if="basket.hydrateError"
          title="题目信息加载失败"
          type="warning"
          description="请检查网络后关闭抽屉重新打开"
          show-icon
          :closable="false"
          style="margin-bottom: 10px"
        />

        <div class="dist-bar" :title="`基础 ${d[1]} · 中等 ${d[2]} · 进阶 ${d[3]}`">
          <div class="dist-seg dist-seg--1" :style="{ width: pct(d[1]) + '%' }" />
          <div class="dist-seg dist-seg--2" :style="{ width: pct(d[2]) + '%' }" />
          <div class="dist-seg dist-seg--3" :style="{ width: pct(d[3]) + '%' }" />
        </div>
        <div class="dist-label">
          基础 {{ d[1] }} · 中等 {{ d[2] }} · 进阶 {{ d[3] }}（共 {{ basket.count }} 题）
        </div>

        <div class="type-chips">
          <el-tag
            v-for="(qs, t) in basket.byType"
            :key="t"
            size="small"
            effect="plain"
            style="cursor: pointer"
            @click="$emit('filter-type', t)"
          >
            {{ QUESTION_TYPE_LABEL[t] || t }} × {{ qs.length }}
          </el-tag>
        </div>

        <el-scrollbar class="basket-list">
          <div
            v-for="(id, idx) in basket.ids"
            :key="id"
            class="basket-item-wrap"
            draggable="true"
            :class="{ 'basket-item-wrap--dragging': dragIdx === idx }"
            @dragstart="onDragStart($event, idx)"
            @dragover.prevent="onDragOver($event, idx)"
            @drop="onDrop($event, idx)"
            @dragend="dragIdx = null"
          >
            <div class="basket-item" @click="expandedId = expandedId === id ? null : id">
              <span class="basket-item__text">
                <span class="basket-item__type">{{ typeLabel(id) }}</span>
                {{ snippet(id) }}
              </span>
              <el-button
                text
                size="small"
                type="danger"
                @click.stop="basket.remove(id)"
              >
                移除
              </el-button>
            </div>
            <!-- 点击展开查看完整题干+答案 -->
            <div v-if="expandedId === id" class="basket-item__detail">
              <div class="basket-item__full-text" v-html="fullText(id)" />
              <div v-if="optsOf(id).length" class="basket-item__opts">
                <div v-for="(opt, oi) in optsOf(id)" :key="oi" v-html="renderOpt(opt, oi)" />
              </div>
              <div v-if="answerOf(id)" class="basket-item__answer">
                <b>答案：</b><span v-html="answerOf(id)" />
              </div>
              <div v-if="pathOf(id)" class="basket-item__path">{{ pathOf(id) }}</div>
            </div>
          </div>
        </el-scrollbar>

        <!-- 草稿管理 -->
        <div class="basket-drafts">
          <div class="basket-drafts__save">
            <el-input
              v-model="draftName"
              size="small"
              placeholder="草稿名称（如：期中选择题）"
              style="flex: 1"
              @keyup.enter="onSaveDraft"
            />
            <el-button size="small" :disabled="!draftName.trim()" @click="onSaveDraft">
              保存篮
            </el-button>
          </div>
          <div v-if="draftList.length" class="basket-drafts__list">
            <el-popover trigger="click" width="260">
              <template #reference>
                <el-button size="small" text>加载草稿 ▾</el-button>
              </template>
              <div v-for="d in draftList" :key="d.name" class="draft-item">
                <span class="draft-item__info" style="cursor: pointer" @click="onLoadDraft(d.name)">
                  {{ d.name }}（{{ d.count }} 题）
                </span>
                <el-button
                  text
                  size="small"
                  type="danger"
                  @click="onDeleteDraft(d.name)"
                >
                  删
                </el-button>
              </div>
              <el-empty v-if="!draftList.length" description="暂无草稿" :image-size="40" />
            </el-popover>
          </div>
        </div>

        <div class="basket-actions">
          <el-button size="small" @click="onClear">清空</el-button>
          <div class="basket-actions__right">
            <el-button size="small" @click="previewVisible = true">预览</el-button>
            <el-button
              size="small"
              type="success"
              plain
              :loading="exportingWord"
              @click="doExportWord"
            >
              {{ exportingWord ? '生成中…' : '导出 Word' }}
            </el-button>
            <el-button size="small" type="primary" @click="$emit('compose')">去组卷</el-button>
          </div>
        </div>
      </div>
    </template>
    <!-- 试卷预览弹窗 -->
    <BasketPreviewDialog
      v-model="previewVisible"
      :questions="previewQuestions"
      :title="previewTitle"
      @export-word="doExportWord"
    />
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useQuestionBasketStore } from '@/stores/questionBasket';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import { exportExamWord } from '@/api/questionBank';
import { renderMath } from '@/composables/useQuestionHelpers';
import BasketPreviewDialog from './BasketPreviewDialog.vue';

// 默认每题分值（选题直出 Word 时使用，不经过组卷向导赋分环节）
const DEFAULT_SCORES = {
  SINGLE_CHOICE: 2,
  MULTI_CHOICE: 3,
  TRUE_FALSE: 1,
  FILL_IN: 1,
  SHORT_ANSWER: 5,
  PROGRAMMING: 10,
  CLOZE: 2,
  ESSAY: 20,
  MATCHING: 3,
  DRAG_SORT: 3,
  COMPOSITE: 10,
};

defineProps({ modelValue: Boolean });
const emit = defineEmits(['update:modelValue', 'compose', 'filter-type']);
const basket = useQuestionBasketStore();
const d = computed(() => basket.difficultyDist);
const pct = (n) => (basket.count ? Math.round((n / basket.count) * 100) : 0);
const snippet = (id) => {
  const t = basket.hydrated[id]?.questionText;
  if (!t) return `题目 #${id}（加载中…）`;
  return t.length > 40 ? t.slice(0, 40) + '…' : t;
};
const onClear = async () => {
  try {
    await ElMessageBox.confirm('清空试题篮？', '确认', { type: 'warning' });
    basket.clear();
  } catch {
    /* */
  }
};

/** 篮内直接导出 Word：不创建任务，一键下载可打印试卷 */
const exportingWord = ref(false);
const doExportWord = async () => {
  if (exportingWord.value || !basket.count) return;
  exportingWord.value = true;
  try {
    const ids = [...basket.ids];
    // 按题型汇总分值（同题型取默认值，不带每道题的逐题赋分）
    const typeScores = {};
    let totalScore = 0;
    const seen = new Set();
    for (const id of ids) {
      const q = basket.hydrated[id];
      const t = q?.questionType || 'UNKNOWN';
      if (!seen.has(t)) {
        typeScores[t] = DEFAULT_SCORES[t] || 5;
        seen.add(t);
      }
      totalScore += DEFAULT_SCORES[t] || 5;
    }
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    await exportExamWord({
      questionIds: ids,
      title: `试卷_${today}`,
      totalScore,
      durationMinutes: 60,
      perTypeScores: typeScores,
    });
    ElMessage.success('试卷已下载');
  } catch (e) {
    console.error('导出 Word 失败:', e);
    ElMessage.error('导出失败，请重试');
  } finally {
    exportingWord.value = false;
  }
};

/** 篮内单题展开查看 */
const expandedId = ref(null);

const typeLabel = (id) => {
  const t = basket.hydrated[id]?.questionType;
  return t ? QUESTION_TYPE_LABEL[t] || t : '';
};

const fullText = (id) => {
  const q = basket.hydrated[id];
  if (!q?.questionText) return '加载中…';
  return renderMath(q.questionText);
};

const answerOf = (id) => {
  const q = basket.hydrated[id];
  if (!q?.correctAnswer) return '';
  return renderMath(
    typeof q.correctAnswer === 'string' ? q.correctAnswer : JSON.stringify(q.correctAnswer)
  );
};

const pathOf = (id) => {
  const q = basket.hydrated[id];
  return q?.categoryPath || '';
};

const optsOf = (id) => {
  const q = basket.hydrated[id];
  if (!q?.options) return [];
  try {
    return Array.isArray(q.options) ? q.options : JSON.parse(q.options);
  } catch {
    return [];
  }
};
const renderOpt = (opt, oi) => {
  const text =
    typeof opt === 'string'
      ? opt.replace(/^[A-Za-hH]\s*[.、．)）:：]\s*/, '')
      : opt.text || opt.label || String(opt);
  return `${String.fromCharCode(65 + oi)}. ${renderMath(text)}`;
};

/** 拖拽调序 */
const dragIdx = ref(null);
const onDragStart = (e, idx) => {
  dragIdx.value = idx;
  e.dataTransfer.effectAllowed = 'move';
};
const onDragOver = (e, idx) => {
  e.dataTransfer.dropEffect = 'move';
};
const onDrop = (e, idx) => {
  if (dragIdx.value != null && dragIdx.value !== idx) {
    basket.reorder(dragIdx.value, idx);
  }
  dragIdx.value = null;
};

/** 草稿管理 */
const draftName = ref('');
const draftList = ref([]);
const refreshDrafts = () => {
  draftList.value = basket.listDrafts();
};
const onSaveDraft = () => {
  const name = draftName.value.trim();
  if (!name) return;
  if (basket.saveDraft(name)) {
    ElMessage.success(`已保存草稿「${name}」`);
    draftName.value = '';
    refreshDrafts();
  } else {
    ElMessage.error('保存失败');
  }
};
const onLoadDraft = (name) => {
  if (basket.count) {
    ElMessageBox.confirm('加载草稿将替换当前试题篮，确认？', '加载草稿', { type: 'warning' })
      .then(() => {
        if (basket.loadDraft(name)) {
          ElMessage.success(`已加载「${name}」`);
        }
      })
      .catch(() => {});
  } else {
    if (basket.loadDraft(name)) ElMessage.success(`已加载「${name}」`);
  }
};
const onDeleteDraft = (name) => {
  basket.deleteDraft(name);
  refreshDrafts();
};

/** 试卷预览 */
const previewVisible = ref(false);
const previewQuestions = computed(() =>
  basket.ids.map((id) => basket.hydrated[id] || { id }).filter((q) => q.questionText)
);
const previewTitle = computed(() => {
  const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  return `试卷_${today}`;
});

/** 预览关闭后自动关闭抽屉，回到列表继续选题 */
watch(previewVisible, (v) => {
  if (!v) emit('update:modelValue', false);
});
</script>

<style scoped>
.basket-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.dist-bar {
  display: flex;
  height: 8px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--bg-secondary);
}
.dist-seg--1 {
  background: var(--el-color-success);
}
.dist-seg--2 {
  background: var(--el-color-warning);
}
.dist-seg--3 {
  background: var(--el-color-danger);
}
.dist-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin: 6px 0 10px;
}
.type-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.basket-list {
  flex: 1;
  min-height: 0;
}
.basket-item-wrap {
  border-bottom: 0.5px solid var(--border-light);
}
.basket-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  cursor: pointer;
}
.basket-item__text {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  flex: 1;
  padding-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.basket-item__type {
  display: inline-block;
  font-size: 10px;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
  border-radius: 3px;
  padding: 0 4px;
  margin-right: 4px;
  vertical-align: middle;
  line-height: 18px;
}
.basket-item__detail {
  padding: 6px 0 10px 0;
}
.basket-item__full-text {
  font-size: var(--fs-sm);
  line-height: 1.7;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.basket-item__opts {
  margin: 4px 0 6px;
  padding-left: 8px;
  border-left: 2px solid var(--border-color);
}
.basket-item__opts div {
  font-size: var(--fs-xs);
  color: var(--text-regular);
  line-height: 1.7;
}
.basket-item__answer {
  font-size: var(--fs-xs);
  color: var(--el-color-success);
  line-height: 1.6;
  margin-bottom: 4px;
}
.basket-item__path {
  font-size: var(--fs-xs);
  color: var(--text-disabled);
}
.basket-drafts {
  margin-bottom: 10px;
}
.basket-drafts__save {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
}
.basket-drafts__list {
  text-align: center;
}
.draft-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}
.draft-item__info {
  flex: 1;
  font-size: var(--fs-sm);
  color: var(--text-primary);
}
.draft-item__info:hover {
  color: var(--primary-color);
}
.basket-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
.basket-actions__right {
  display: flex;
  gap: 8px;
}
</style>
