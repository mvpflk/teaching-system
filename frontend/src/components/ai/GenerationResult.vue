<template>
  <div v-if="result" class="gr-wrap">
    <!-- Content type: Markdown -->
    <template v-if="result.type === 'content'">
      <div class="gr-header">
        <span class="gr-title">{{ result.title || '生成结果' }}</span>
        <div class="gr-actions">
          <el-button size="small" @click="$emit('edit')">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button
            size="small"
            type="success"
            title="发布到历史，学生可查看"
            @click="$emit('publish')"
          >
            <el-icon><CircleCheck /></el-icon> 发布到历史
          </el-button>
          <el-button size="small" type="primary" @click="$emit('exportWord')">
            <el-icon><Download /></el-icon> 导出Word
          </el-button>
          <el-button size="small" @click="$emit('regenerate')">
            <el-icon><Refresh /></el-icon> 重新生成
          </el-button>
          <el-button
            v-if="result.outputType === 'PRACTICE_PLAN'"
            size="small"
            type="warning"
            @click="$emit('publishAsTask')"
          >
            <el-icon><Upload /></el-icon> 发布为实训任务
          </el-button>
          <el-button
            v-if="result.outputType === 'KNOWLEDGE_CHECKLIST' && result.id"
            size="small"
            type="success"
            @click="$emit('generatePractice', result.id)"
          >
            <el-icon><Notebook /></el-icon> 生成配套练习
          </el-button>
        </div>
      </div>
      <div class="gr-rating">
        <span class="gr-rating-label">评分：</span>
        <el-rate
          v-model="rating"
          :max="5"
          size="small"
          show-text
          :texts="['需改进', '一般', '不错', '很好', '优秀']"
          @change="onRate"
        />
        <el-button
          v-if="!showFeedbackInput"
          text
          size="small"
          style="margin-left: 8px"
          @click="showFeedbackInput = true"
        >
          反馈
        </el-button>
        <el-input
          v-if="showFeedbackInput"
          v-model="feedback"
          size="small"
          placeholder="可选：简单说两句"
          style="width: 200px; margin-left: 8px"
          @blur="onRate"
        />
      </div>
      <div class="gr-content" v-html="renderedContent"></div>
    </template>

    <!-- Question type: question list -->
    <template v-else-if="result.type === 'questions'">
      <div class="gr-header">
        <span class="gr-title">已生成 {{ result.count || result.questions?.length || 0 }} 道题目</span>
        <span class="gr-note">状态：AI草稿，需审核后加入题库</span>
        <div class="gr-meta-tags">
          <el-tag
            v-if="result._filteredGap > 0"
            size="small"
            type="warning"
            effect="plain"
          >
            已过滤 {{ result._filteredGap }} 题格式异常
          </el-tag>
          <el-tag
            v-if="result._difficultyAnomaly"
            size="small"
            type="danger"
            effect="dark"
          >
            难度分布异常
          </el-tag>
          <el-tag
            v-if="result._difficultyAudit"
            size="small"
            type="info"
            effect="plain"
          >
            {{ result._difficultyAudit }}
          </el-tag>
        </div>
        <div class="gr-actions">
          <el-button size="small" text @click="toggleAll">
            {{ selectedIds.length === (result.questions || []).length ? '取消全选' : '全选' }}
          </el-button>
          <el-button size="small" @click="showDrawer = true">预览组卷</el-button>
          <el-button
            size="small"
            type="primary"
            @click="$emit('editQuestions', result.batchId || result.id, result.questions)"
          >
            <el-icon><Edit /></el-icon> 编辑试卷
          </el-button>
          <el-button
            v-if="result.questions?.length"
            size="small"
            type="info"
            @click="$emit('viewInBank')"
          >
            <el-icon><Files /></el-icon> 题库编辑
          </el-button>
          <el-button
            size="small"
            type="warning"
            :disabled="selectedIds.length === 0"
            @click="$emit('publishAsExam', selectedIds)"
          >
            <el-icon><Upload /></el-icon> 组卷发布({{ selectedIds.length }})
          </el-button>
        </div>
      </div>
      <div
        v-for="(q, i) in pagedQuestions"
        :key="i"
        class="gr-qitem"
        :class="{ selected: selectedIds.includes(q.id) }"
        @click="toggleQ(q.id)"
      >
        <el-checkbox
          :model-value="selectedIds.includes(q.id)"
          style="margin-right: 6px"
          @click.stop
          @change="toggleQ(q.id)"
        />
        <span class="gr-qno">{{ i + 1 }}.</span>
        <div class="gr-qbody">
          <span class="gr-qtext" v-html="renderMath(q.questionText || q.content || '无内容')" />
          <div v-if="q.options?.length" class="gr-qopts">
            <span
              v-for="(o, oi) in parseOptions(q.options)"
              :key="oi"
              class="gr-qopt"
            >{{ ['A', 'B', 'C', 'D', 'E', 'F'][oi] }}.
              <span v-html="renderMath(typeof o === 'string' ? o : o.text || o)" /></span>
          </div>
        </div>
        <el-tag
          v-if="q.intent"
          size="small"
          type="info"
          effect="plain"
          style="max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
        >
          {{ q.intent }}
        </el-tag>
        <el-tag size="small" type="info">{{ typeLabel(q.questionType) }}</el-tag>
        <el-tag
          v-if="q._quality != null"
          size="small"
          :type="q._quality >= 60 ? 'success' : 'danger'"
          effect="plain"
        >
          {{ q._quality >= 60 ? '质量' : '偏低' }} {{ q._quality }}
        </el-tag>
        <el-tag
          v-if="q.score"
          size="small"
          type="warning"
          effect="plain"
          style="margin-left: 2px"
        >
          {{ q.score }}分
        </el-tag>
        <el-tag
          v-if="q.tier"
          size="small"
          :type="q.tier === 'BASIC' ? 'success' : q.tier === 'ADVANCED' ? 'danger' : 'warning'"
          style="margin-left: 4px"
        >
          {{ { BASIC: '基础', MEDIUM: '中等', ADVANCED: '提高' }[q.tier] }}
        </el-tag>
        <el-tag
          v-if="q.knowledgeDim"
          size="small"
          type="info"
          style="margin-left: 2px"
        >
          {{ { THEORY: '应知', PRACTICE: '应会' }[q.knowledgeDim] }}
        </el-tag>
      </div>
      <!-- 分页 -->
      <div v-if="(result.questions || []).length > pageSize" class="gr-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="(result.questions || []).length"
          layout="prev, pager, next"
          small
        />
      </div>
    </template>

    <!-- 组卷预览抽屉 -->
    <el-drawer
      v-model="showDrawer"
      title="组卷预览"
      :size="isMobile ? '85%' : '480px'"
      direction="rtl"
    >
      <div class="gr-drawer">
        <div class="gr-exam-header">
          <h3>{{ result.title || '试卷预览' }}</h3>
          <el-tag size="small">
            共 {{ selectedIds.length || (result.questions || []).length }} 题
          </el-tag>
        </div>
        <div v-for="(q, i) in previewQuestions" :key="i" class="gr-exam-item">
          <div class="gr-exam-qno">
            {{ i + 1 }}. <el-tag size="small">{{ typeLabel(q.questionType) }}</el-tag>
            <span
              v-if="q.tier"
              style="font-size: var(--fs-xs); color: var(--text-secondary)"
            >[{{ { BASIC: '基础', MEDIUM: '中等', ADVANCED: '提高' }[q.tier] }}]</span>
          </div>
          <div class="gr-exam-qtext" v-html="renderMath(q.questionText)" />
          <div v-if="q.options?.length" class="gr-exam-opts">
            <span
              v-for="(o, oi) in parseOptions(q.options)"
              :key="oi"
              style="display: block; padding: 2px 0"
            >{{ ['A', 'B', 'C', 'D', 'E', 'F'][oi] }}. <span v-html="renderMath(stripPrefix(o))" /></span>
          </div>
          <div class="gr-exam-answer">
            答案：<span v-html="renderMath(q.correctAnswer)" />
            <span
              v-if="q.explanation"
              style="color: var(--text-secondary)"
            >(<span v-html="renderMath(q.explanation)" />)</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { renderMarkdown } from '@/utils/markdown';
import { renderMath } from '@/composables/useQuestionHelpers';
import { rateAiOutput } from '@/api/aiOutput';
import { useIsMobile } from '@/composables/useIsMobile';
import { Edit, CircleCheck, Download, Refresh, Notebook, Files } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const props = defineProps({ result: Object });
defineEmits([
  'edit',
  'publish',
  'exportWord',
  'regenerate',
  'publishAsTask',
  'publishAsExam',
  'editQuestions',
  'viewInBank',
  'generatePractice',
]);

const { isMobile } = useIsMobile();
const renderedContent = computed(() => renderMarkdown(props.result?.content));
const showDrawer = ref(false);
const selectedIds = ref([]);
const currentPage = ref(1);
const pageSize = 10;

// 分页后的题目列表
const pagedQuestions = computed(() => {
  const all = props.result?.questions || [];
  const start = (currentPage.value - 1) * pageSize;
  return all.slice(start, start + pageSize);
});

const previewQuestions = computed(() => {
  const all = props.result?.questions || [];
  if (!selectedIds.value.length) return all;
  return all.filter((q) => selectedIds.value.includes(q.id));
});
const toggleQ = (id) => {
  const idx = selectedIds.value.indexOf(id);
  if (idx >= 0) selectedIds.value.splice(idx, 1);
  else selectedIds.value.push(id);
};
const toggleAll = () => {
  const all = (props.result?.questions || []).map((q) => q.id);
  selectedIds.value = selectedIds.value.length === all.length ? [] : all;
};
const rating = ref(0);
const feedback = ref('');
import { typeLabel, parseOptions } from '@/utils/questionHelpers';

/** 防御性移除选项文本开头的字母前缀（与后端 stripOptionPrefix 一致） */
const stripPrefix = (text) => {
  if (!text) return '';
  // 规则1: 字母 + 显式分隔符 → 剥离选项标签
  let cleaned = text.replace(/^[A-Da-d]\s*[.、．)）:：-]\s*/, '').trim();
  // 规则2: 如果没有显式分隔符但匹配 "字母+空格+中文" 模式 → 剥离
  if (cleaned === text) {
    cleaned = text.replace(/^[A-Da-d]\s+(?=[一-鿿])/, '').trim();
  }
  return cleaned;
};
const showFeedbackInput = ref(false);

watch(
  () => props.result,
  (r) => {
    rating.value = r?.rating || 0;
    feedback.value = r?.feedback || '';
    showFeedbackInput.value = false;
    currentPage.value = 1; // 结果变化时重置分页
  }
);

const onRate = async () => {
  const id = props.result?.id;
  if (!id) return;
  try {
    await rateAiOutput(id, rating.value || null, feedback.value || null);
    ElMessage.success('已评价');
  } catch {
    /* */
  }
};
</script>

<style scoped>
.gr-wrap {
  margin-top: 4px;
}
.gr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
}
.gr-title {
  font-size: var(--fs-lg);
  font-weight: 700;
}
.gr-note {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.gr-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.gr-meta-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.gr-content {
  padding: 20px;
  background: var(--bg-section);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: var(--fs-md);
  line-height: 1.9;
  max-height: 600px;
  overflow-y: auto;
}
.gr-content :deep(h1) {
  font-size: var(--fs-xl);
  margin: 0 0 12px;
}
.gr-content :deep(h2) {
  font-size: 17px;
  margin: 16px 0 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-light);
}
.gr-content :deep(h3) {
  font-size: var(--fs-md);
  margin: 12px 0 6px;
}
.gr-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
}
.gr-content :deep(th),
.gr-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
  font-size: var(--fs-sm);
}
.gr-content :deep(th) {
  background: var(--bg-section);
  font-weight: 600;
}
.gr-qitem {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
  font-size: var(--fs-sm);
}
.gr-qno {
  font-weight: 700;
  color: var(--primary-color);
  flex-shrink: 0;
}
.gr-qbody {
  flex: 1;
  min-width: 0;
}
.gr-qtext {
  display: block;
}
.gr-qopts {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin-top: 6px;
  padding: 6px 0;
}
.gr-qopt {
  font-size: var(--fs-xs);
  color: var(--text-regular);
}
.gr-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

@media (max-width: 767px) {
  .gr-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .gr-actions {
    width: 100%;
    flex-wrap: wrap;
    gap: 4px;
  }
  .gr-actions .el-button {
    flex: 1;
    min-width: calc(50% - 4px);
    font-size: var(--fs-xs);
    padding: 6px 8px;
  }
  .gr-content {
    padding: 12px;
    max-height: 400px;
    font-size: var(--fs-sm);
  }
  .gr-qitem {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
  .gr-qbody {
    width: 100%;
  }
  .gr-qopts {
    flex-direction: column;
    gap: 4px;
  }
  .gr-rating {
    flex-wrap: wrap;
  }
}
</style>
