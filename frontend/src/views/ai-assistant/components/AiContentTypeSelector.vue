<template>
  <!-- Desktop -->
  <template v-if="!isMobile">
    <el-card shadow="never" class="aia-card">
      <template #header>
        <div class="aia-card-title aia-collapse-header" @click="typeCollapsed = !typeCollapsed">
          <span class="aia-step">2</span> 选择生成类型
          <span v-if="typeCollapsed" class="aia-type-summary">{{ currentTypeLabel }}</span>
          <el-icon class="aia-collapse-icon" :class="{ 'is-collapsed': typeCollapsed }">
            <ArrowDown />
          </el-icon>
        </div>
      </template>
      <div v-show="!typeCollapsed">
        <el-radio-group v-model="contentType" class="aia-type-group">
          <el-radio-button value="TEACHING_DESIGN">
            <el-icon><Document /></el-icon> 教学设计
          </el-radio-button>
          <el-radio-button value="KNOWLEDGE_CHECKLIST">
            <el-icon><List /></el-icon> 知识清单
          </el-radio-button>
          <el-radio-button v-if="showPracticePlans" value="PRACTICE_PLAN">
            <el-icon><Setting /></el-icon> 实训方案
          </el-radio-button>
          <el-radio-button value="COMPREHENSIVE_EXERCISES">
            <el-icon><Files /></el-icon> 综合练习
          </el-radio-button>
          <el-radio-button value="CLASSROOM_QUESTIONS">
            <el-icon><ChatDotRound /></el-icon> 课堂提问
          </el-radio-button>
          <el-radio-button value="KNOWLEDGE_PRACTICE">
            <el-icon><Notebook /></el-icon> 配套练习
          </el-radio-button>
        </el-radio-group>
        <div
          v-if="
            contentType === 'COMPREHENSIVE_EXERCISES' ||
              contentType === 'CLASSROOM_QUESTIONS' ||
              contentType === 'KNOWLEDGE_PRACTICE'
          "
          class="aia-options"
          style="margin-top: 12px"
        >
          <span class="aia-opt-label">难度分布：</span>
          <el-radio-group v-model="questionTier" size="small">
            <el-radio-button value="BASIC">基础为主</el-radio-button>
            <el-radio-button value="BALANCED">均衡</el-radio-button>
            <el-radio-button value="ADVANCED">提高为主</el-radio-button>
          </el-radio-group>
          <template v-if="!isCulturalSubject">
            <span style="margin-left: 16px" class="aia-opt-label">应知应会：</span>
            <el-radio-group v-model="questionDim" size="small">
              <el-radio-button value="BOTH">全覆盖</el-radio-button>
              <el-radio-button value="THEORY">偏应知</el-radio-button>
              <el-radio-button value="PRACTICE">偏应会</el-radio-button>
            </el-radio-group>
          </template>
        </div>
      </div>
    </el-card>

    <!-- 题型数量 + 每题分值配置（仅综合练习/配套练习） -->
    <el-card
      v-if="contentType === 'COMPREHENSIVE_EXERCISES' || contentType === 'KNOWLEDGE_PRACTICE'"
      shadow="never"
      class="aia-card"
    >
      <template #header><div class="aia-card-title">题型与分值配置</div></template>
      <div class="aia-options">
        <div style="display: flex; flex-wrap: wrap; gap: 12px">
          <div v-for="t in questionTypeOptions" :key="t.key" class="type-config-item">
            <span class="type-config-label">{{ t.label }}</span>
            <el-input-number
              v-model="questionCounts[t.key]"
              :min="0"
              :max="t.key === 'SINGLE_CHOICE' ? 50 : 20"
              size="small"
              style="width: 65px; margin-left: 4px"
              controls-position="right"
            />
            题
            <span style="margin: 0 4px; color: var(--text-disabled)">|</span>
            <el-input-number
              v-model="scorePresets[t.key]"
              :min="1"
              :max="50"
              size="small"
              style="width: 65px"
              controls-position="right"
            />
            分/题
          </div>
        </div>
        <div
          v-if="totalQuestionCount <= 0"
          style="color: var(--el-color-danger); margin-top: 6px; font-size: var(--fs-xs)"
        >
          请至少选择一种题型（数量 &gt; 0）
        </div>
        <div v-else style="color: var(--text-secondary); margin-top: 6px; font-size: var(--fs-xs)">
          共 {{ totalQuestionCount }} 题，总分 {{ totalScorePreview }} 分
        </div>
      </div>
    </el-card>
  </template>

  <!-- Mobile -->
  <template v-else>
    <el-card shadow="never" class="aia-card aia-card-mobile">
      <template #header>
        <div class="aia-card-title aia-collapse-header" @click="typeCollapsed = !typeCollapsed">
          <span class="aia-step">2</span> 选择生成类型
          <span v-if="typeCollapsed" class="aia-type-summary">{{ currentTypeLabel }}</span>
          <el-icon class="aia-collapse-icon" :class="{ 'is-collapsed': typeCollapsed }">
            <ArrowDown />
          </el-icon>
        </div>
      </template>
      <el-radio-group
        v-show="!typeCollapsed"
        v-model="contentType"
        class="aia-type-group-mobile"
        @change="onMobileTypeChange"
      >
        <el-radio-button value="TEACHING_DESIGN">
          <el-icon><Document /></el-icon> 教学设计
        </el-radio-button>
        <el-radio-button value="KNOWLEDGE_CHECKLIST">
          <el-icon><List /></el-icon> 知识清单
        </el-radio-button>
        <el-radio-button v-if="showPracticePlans" value="PRACTICE_PLAN">
          <el-icon><Setting /></el-icon> 实训方案
        </el-radio-button>
        <el-radio-button value="COMPREHENSIVE_EXERCISES">
          <el-icon><Files /></el-icon> 综合练习
        </el-radio-button>
        <el-radio-button value="CLASSROOM_QUESTIONS">
          <el-icon><ChatDotRound /></el-icon> 课堂提问
        </el-radio-button>
        <el-radio-button value="KNOWLEDGE_PRACTICE">
          <el-icon><Notebook /></el-icon> 配套练习
        </el-radio-button>
      </el-radio-group>

      <!-- 教学设计：风格与侧重 -->
      <template v-if="contentType === 'TEACHING_DESIGN'">
        <div class="aia-mobile-section">
          <span class="aia-section-label">输出风格</span>
          <el-radio-group v-model="designStyle" size="small">
            <el-radio-button value="CONCISE">精简</el-radio-button>
            <el-radio-button value="STANDARD">标准</el-radio-button>
            <el-radio-button value="DETAILED">详细</el-radio-button>
          </el-radio-group>
        </div>
        <div class="aia-mobile-section">
          <span class="aia-section-label">内容侧重</span>
          <el-radio-group v-model="designFocus" size="small">
            <el-radio-button value="BALANCED">均衡</el-radio-button>
            <el-radio-button value="THEORY">偏应知</el-radio-button>
            <el-radio-button value="PRACTICE">偏应会</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 练习题类：难度与维度 -->
      <template
        v-if="
          contentType === 'COMPREHENSIVE_EXERCISES' ||
            contentType === 'CLASSROOM_QUESTIONS' ||
            contentType === 'KNOWLEDGE_PRACTICE'
        "
      >
        <div class="aia-mobile-section">
          <span class="aia-section-label">难度分布</span>
          <el-radio-group v-model="questionTier" size="small">
            <el-radio-button value="BASIC">基础</el-radio-button>
            <el-radio-button value="BALANCED">均衡</el-radio-button>
            <el-radio-button value="ADVANCED">提高</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="!isCulturalSubject" class="aia-mobile-section">
          <span class="aia-section-label">应知应会</span>
          <el-radio-group v-model="questionDim" size="small">
            <el-radio-button value="BOTH">全覆盖</el-radio-button>
            <el-radio-button value="THEORY">偏应知</el-radio-button>
            <el-radio-button value="PRACTICE">偏应会</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 综合练习/配套练习：题型与分值 -->
      <template
        v-if="contentType === 'COMPREHENSIVE_EXERCISES' || contentType === 'KNOWLEDGE_PRACTICE'"
      >
        <div class="aia-mobile-section">
          <span class="aia-section-label">题型与分值</span>
          <div class="aia-type-config-mobile">
            <div class="type-config-header">
              <span class="type-config-label">题型</span>
              <span class="type-config-col">数量</span>
              <span class="type-config-col">分值</span>
            </div>
            <div v-for="t in questionTypeOptions" :key="t.key" class="type-config-row">
              <span class="type-config-label">{{ t.label }}</span>
              <div class="type-config-field">
                <el-input-number
                  v-model="questionCounts[t.key]"
                  :min="0"
                  :max="t.key === 'SINGLE_CHOICE' ? 50 : 20"
                  size="small"
                  class="type-config-num"
                  controls-position="right"
                />
                <span class="type-config-unit">题</span>
              </div>
              <div class="type-config-field">
                <el-input-number
                  v-model="scorePresets[t.key]"
                  :min="1"
                  :max="50"
                  size="small"
                  class="type-config-num"
                  controls-position="right"
                />
                <span class="type-config-unit">分</span>
              </div>
            </div>
          </div>
          <div v-if="totalQuestionCount <= 0" class="aia-total-warn">请至少选择一种题型</div>
          <div v-else class="aia-total-info">
            共 {{ totalQuestionCount }} 题，总分 {{ totalScorePreview }} 分
          </div>
        </div>
      </template>
    </el-card>
  </template>

  <!-- 教学设计专属：风格与侧重 (desktop) -->
  <el-card v-if="!isMobile && contentType === 'TEACHING_DESIGN'" shadow="never" class="aia-card">
    <template #header><div class="aia-card-title">风格与侧重</div></template>
    <div class="aia-options">
      <span class="aia-opt-label">输出风格：</span>
      <el-radio-group v-model="designStyle" size="small">
        <el-radio-button value="CONCISE">精简实用</el-radio-button>
        <el-radio-button value="STANDARD">标准完整</el-radio-button>
        <el-radio-button value="DETAILED">详细全面</el-radio-button>
      </el-radio-group>
      <span style="margin-left: 20px" class="aia-opt-label">内容侧重：</span>
      <el-radio-group v-model="designFocus" size="small">
        <el-radio-button value="BALANCED">均衡</el-radio-button>
        <el-radio-button value="THEORY">偏应知</el-radio-button>
        <el-radio-button value="PRACTICE">偏应会</el-radio-button>
      </el-radio-group>
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue';
import {
  Document,
  List,
  Setting,
  Files,
  ChatDotRound,
  Notebook,
  ArrowDown,
} from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/user';

const props = defineProps({
  isMobile: { type: Boolean, default: false },
  showPracticePlans: { type: Boolean, default: true },
  subjectName: { type: String, default: '' },
});

const emit = defineEmits([
  'update:contentType',
  'update:designStyle',
  'update:designFocus',
  'update:questionTier',
  'update:questionDim',
  'update:questionCounts',
  'update:scorePresets',
]);

const userStore = useUserStore();
const showPracticePlan = computed(() => {
  if (userStore.isAdmin) return true;
  return userStore.showPracticePlans;
});

const contentType = ref('TEACHING_DESIGN');
const designStyle = ref('STANDARD');
const designFocus = ref('BALANCED');
const questionTier = ref('BALANCED');
const questionDim = ref('BOTH');

const COMPREHENSIVE_DEFAULTS = {
  SINGLE_CHOICE: 5,
  MULTI_CHOICE: 3,
  TRUE_FALSE: 3,
  FILL_IN: 3,
  ESSAY: 1,
  CLOZE: 0,
};
const COMPREHENSIVE_SCORES = {
  SINGLE_CHOICE: 2,
  MULTI_CHOICE: 3,
  TRUE_FALSE: 1,
  FILL_IN: 2,
  ESSAY: 10,
  CLOZE: 5,
};
const KP_PRACTICE_DEFAULTS = {
  SINGLE_CHOICE: 5,
  TRUE_FALSE: 4,
  FILL_IN: 7,
  CLOZE: 0,
  MULTI_CHOICE: 0,
  ESSAY: 0,
};
const KP_PRACTICE_SCORES = {
  SINGLE_CHOICE: 2,
  TRUE_FALSE: 1,
  FILL_IN: 2,
  CLOZE: 5,
  MULTI_CHOICE: 3,
  ESSAY: 10,
};
const questionCounts = reactive({ ...COMPREHENSIVE_DEFAULTS });
const scorePresets = reactive({ ...COMPREHENSIVE_SCORES });

const questionTypeOptions = computed(() => {
  const all = [
    { key: 'SINGLE_CHOICE', label: '单选' },
    { key: 'MULTI_CHOICE', label: '多选' },
    { key: 'TRUE_FALSE', label: '判断' },
    { key: 'FILL_IN', label: '填空' },
    { key: 'ESSAY', label: '简答' },
  ];
  if (contentType.value === 'KNOWLEDGE_PRACTICE') {
    all.push({ key: 'CLOZE', label: '完形' });
  }
  return all;
});

const totalQuestionCount = computed(() =>
  Object.values(questionCounts).reduce((s, v) => s + (v || 0), 0)
);
const totalScorePreview = computed(() =>
  Object.entries(questionCounts).reduce((s, [k, v]) => s + (v || 0) * (scorePresets[k] || 0), 0)
);

const typeOptions = computed(() => {
  const all = [
    { value: 'TEACHING_DESIGN', label: '教学设计', desc: '完整的课堂教学方案', icon: 'Document' },
    { value: 'KNOWLEDGE_CHECKLIST', label: '知识清单', desc: '结构化知识点梳理', icon: 'List' },
    { value: 'PRACTICE_PLAN', label: '实训方案', desc: '动手实操步骤指南', icon: 'Setting' },
    {
      value: 'COMPREHENSIVE_EXERCISES',
      label: '综合练习',
      desc: '课堂/课后练习题目',
      icon: 'Files',
    },
    { value: 'CLASSROOM_QUESTIONS', label: '课堂提问', desc: '互动问答题目', icon: 'ChatDotRound' },
    {
      value: 'KNOWLEDGE_PRACTICE',
      label: '配套练习',
      desc: '清单配套的填空/判断/选择练习',
      icon: 'Notebook',
    },
  ];
  return showPracticePlan.value ? all : all.filter((o) => o.value !== 'PRACTICE_PLAN');
});

// 文化课（语文/数学/英语）不分应知应会，自动隐藏该模块以节约空间
const CULTURAL_SUBJECTS = ['语文', '数学', '英语'];
const isCulturalSubject = computed(() => {
  const s = props.subjectName || '';
  return CULTURAL_SUBJECTS.some((c) => s.includes(c));
});

// 类型选择卡片折叠状态（移动端选择类型后自动折叠）
const typeCollapsed = ref(false);
const currentTypeLabel = computed(
  () => typeOptions.value.find((o) => o.value === contentType.value)?.label || ''
);
const onMobileTypeChange = () => {
  if (props.isMobile) typeCollapsed.value = true;
};

watch([contentType, showPracticePlan], ([ct, sp]) => {
  if (ct === 'PRACTICE_PLAN' && !sp) contentType.value = 'TEACHING_DESIGN';
});

watch(contentType, (v) => {
  emit('update:contentType', v);
  if (v === 'KNOWLEDGE_PRACTICE') {
    Object.assign(questionCounts, { ...KP_PRACTICE_DEFAULTS });
    Object.assign(scorePresets, { ...KP_PRACTICE_SCORES });
  } else if (v === 'COMPREHENSIVE_EXERCISES') {
    Object.assign(questionCounts, { ...COMPREHENSIVE_DEFAULTS });
    Object.assign(scorePresets, { ...COMPREHENSIVE_SCORES });
  }
  // 主动通知父组件，确保题型配比同步
  emit('update:questionCounts', { ...questionCounts });
  emit('update:scorePresets', { ...scorePresets });
});
watch(designStyle, (v) => emit('update:designStyle', v));
watch(designFocus, (v) => emit('update:designFocus', v));
watch(questionTier, (v) => emit('update:questionTier', v));
watch(questionDim, (v) => emit('update:questionDim', v));
watch(questionCounts, (v) => emit('update:questionCounts', { ...v }), { deep: true });
watch(scorePresets, (v) => emit('update:scorePresets', { ...v }), { deep: true });

// expose computed values for parent
defineExpose({ contentType, totalQuestionCount, totalScorePreview });
</script>

<style scoped>
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
.aia-type-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.aia-options {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.aia-opt-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary, #666);
  white-space: nowrap;
}
.type-config-item {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  background: var(--bg-secondary, #f8f9fa);
  border-radius: 8px;
  font-size: var(--fs-sm);
}
.type-config-label {
  font-weight: 500;
  min-width: 28px;
}

/* 折叠头部 */
.aia-collapse-header {
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}
.aia-type-summary {
  font-size: var(--fs-sm);
  font-weight: 500;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 2px 10px;
  border-radius: 10px;
  margin-left: 4px;
}
.aia-collapse-icon {
  margin-left: auto;
  font-size: 14px;
  color: var(--text-secondary);
  transition: transform var(--transition-fast);
}
.aia-collapse-icon.is-collapsed {
  transform: rotate(-90deg);
}

/* 移动端卡片紧凑样式 */
.aia-card-mobile {
  padding: 0;
}
.aia-card-mobile :deep(.el-card__body) {
  padding: 12px;
}
.aia-card-mobile :deep(.el-card__header) {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-lighter);
}

/* 移动端类型选择组 */
.aia-type-group-mobile {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.aia-type-group-mobile .el-radio-button {
  height: 32px;
  line-height: 30px;
  padding: 0 10px;
  font-size: var(--fs-xs);
}
.aia-type-group-mobile .el-radio-button .el-icon {
  margin-right: 2px;
  font-size: 14px;
}

/* 移动端配置区块 */
.aia-mobile-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-lighter);
}
.aia-mobile-section:first-of-type {
  border-top: none;
  padding-top: 0;
}
.aia-section-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  font-weight: 500;
}
.aia-mobile-section .el-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.aia-mobile-section .el-radio-button {
  height: 28px;
  line-height: 26px;
  padding: 0 8px;
  font-size: var(--fs-xs);
}

/* 移动端题型配置 — CSS Grid 布局，输入框填满可用空间 */
.aia-type-config-mobile {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.type-config-header {
  display: grid;
  grid-template-columns: 44px 1fr 1fr;
  gap: 4px 8px;
  padding: 0 12px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  font-weight: 500;
}
.type-config-row {
  display: grid;
  grid-template-columns: 44px 1fr 1fr;
  align-items: center;
  gap: 4px 8px;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-radius: 8px;
  font-size: var(--fs-xs);
}
.type-config-row .type-config-label {
  min-width: 0;
  font-weight: 600;
  flex-shrink: 0;
}
.type-config-field {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.type-config-num {
  flex: 1;
  min-width: 0;
  width: auto;
}
.type-config-unit {
  color: var(--text-secondary);
  flex-shrink: 0;
  font-size: var(--fs-xs);
}
.type-config-col {
  text-align: center;
}
.aia-total-warn {
  color: var(--danger-color);
  margin-top: 6px;
  font-size: var(--fs-xs);
}
.aia-total-info {
  color: var(--text-secondary);
  margin-top: 6px;
  font-size: var(--fs-xs);
}
</style>
