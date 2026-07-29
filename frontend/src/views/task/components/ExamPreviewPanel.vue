<template>
  <div class="exam-preview-panel">
    <!-- 面板标题 -->
    <div class="epp-header">
      <div class="epp-header-left">
        <el-icon><Tickets /></el-icon>
        <span class="epp-title">{{ title || '试卷预览' }}</span>
      </div>
      <div class="epp-header-right">
        <span class="epp-stat">题目: <b>{{ questions.length }}</b> 题</span>
        <span class="epp-stat">总分:
          <b
            :class="{
              'score-full': totalScore >= targetScore,
              'score-short': totalScore < targetScore,
            }"
          >{{ totalScore }}</b>
          / {{ targetScore }}</span>
      </div>
    </div>

    <!-- 进度条 -->
    <el-progress
      v-if="targetScore > 0"
      :percentage="Math.min(100, Math.round((totalScore / targetScore) * 100))"
      :stroke-width="6"
      :color="totalScore >= targetScore ? 'var(--el-color-success)' : 'var(--primary-color)'"
      class="epp-progress"
    />

    <!-- 分数差距提示 -->
    <div v-if="targetScore > 0 && totalScore < targetScore" class="epp-hint epp-hint--warn">
      还差 <b>{{ targetScore - totalScore }}</b> 分达到 {{ targetScore }} 分
    </div>
    <div v-else-if="targetScore > 0 && totalScore >= targetScore" class="epp-hint epp-hint--ok">
      题目已满足满分要求
    </div>

    <!-- 空状态 -->
    <div v-if="!questions.length" class="epp-empty">
      <el-icon class="epp-empty-icon"><DocumentAdd /></el-icon>
      <p>从左侧题库选择题目</p>
      <p class="epp-empty-sub">或使用自由组题 / AI 生成来创建题目</p>
    </div>

    <!-- 题目卡片列表 -->
    <div v-else ref="listRef" class="epp-list">
      <div
        v-for="(q, i) in questions"
        :key="q.id"
        class="eq-card"
        :class="{ 'eq-card--expanded': expanded === q.id, 'eq-card--dragging': dragging === q.id }"
        :draggable="!editing"
        @dragstart="onDragStart($event, i)"
        @dragover.prevent="onDragOver($event, i)"
        @drop="onDrop($event, i)"
        @dragend="dragging = null"
      >
        <!-- 折叠态/默认视图 -->
        <div class="eq-row" @click="expanded = expanded === q.id ? null : q.id">
          <span class="eq-handle" title="拖拽排序">
            <el-icon><Rank /></el-icon>
          </span>
          <span class="eq-num">{{ i + 1 }}</span>
          <TaskIcon
            :type="questionTypeToTaskType(q.questionType)"
            :size="16"
            class="eq-type-icon"
          />
          <span class="eq-text">{{ truncate(q.questionText, 60) }}</span>
          <el-tag size="small" type="info" class="eq-type-tag">
            {{
              QUESTION_TYPE_LABEL[q.questionType] || q.questionType
            }}
          </el-tag>
          <span class="eq-score">{{ q._score || 0 }}分</span>
          <span class="eq-actions" @click.stop>
            <el-button
              size="small"
              text
              title="编辑"
              @click="expanded = expanded === q.id ? null : q.id"
            >
              <el-icon><EditPen /></el-icon>
            </el-button>
            <el-button
              size="small"
              text
              type="danger"
              title="移除"
              @click="$emit('remove', q.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </span>
        </div>

        <!-- 展开态：编辑 -->
        <div v-if="expanded === q.id" class="eq-detail" @click.stop>
          <div class="eq-detail-body">
            <QuestionRenderer
              :question="q"
              mode="display"
              size="default"
              :show-answer="true"
              :highlight-correct="true"
              :show-meta="false"
            />
            <div class="eq-score-row">
              <span class="eq-label">分值：</span>
              <el-input-number
                v-model="q._score"
                :min="1"
                :max="50"
                size="small"
                style="width: 90px"
                @change="$emit('updateScore', q.id, q._score)"
              />
              <span class="eq-label">分</span>
              <el-button
                size="small"
                type="primary"
                plain
                style="margin-left: 8px"
                @click="expanded = null"
              >
                完成
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { EditPen, Delete, Tickets, DocumentAdd, Rank } from '@element-plus/icons-vue';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import TaskIcon from '@/components/common/TaskIcon.vue';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';

const props = defineProps({
  title: { type: String, default: '' },
  questions: { type: Array, default: () => [] },
  targetScore: { type: Number, default: 100 },
});

const emit = defineEmits(['remove', 'reorder', 'updateScore']);

const expanded = ref(null);
const dragging = ref(null);
const editing = ref(false);

const totalScore = computed(() => {
  return props.questions.reduce((sum, q) => sum + (Number(q._score) || 0), 0);
});

const truncate = (text, len) => {
  if (!text) return '';
  return text.length > len ? text.substring(0, len) + '...' : text;
};

const questionTypeToTaskType = (qt) => {
  const map = {
    SINGLE_CHOICE: 'PRE_CLASS',
    MULTI_CHOICE: 'IN_CLASS',
    TRUE_FALSE: 'AFTER_CLASS',
    FILL_IN: 'FORMATIVE',
    SUBJECTIVE: 'SUMMATIVE',
    ESSAY: 'SUMMATIVE',
  };
  return map[qt] || 'PRE_CLASS';
};

// ── HTML5 Drag & Drop ──
const onDragStart = (e, idx) => {
  dragging.value = props.questions[idx]?.id;
  e.dataTransfer.effectAllowed = 'move';
  e.dataTransfer.setData('text/plain', String(idx));
};

const onDragOver = (e, idx) => {
  e.dataTransfer.dropEffect = 'move';
};

const onDrop = (e, toIdx) => {
  const fromIdx = parseInt(e.dataTransfer.getData('text/plain'));
  if (fromIdx !== toIdx && !isNaN(fromIdx)) {
    emit('reorder', fromIdx, toIdx);
  }
  dragging.value = null;
};
</script>

<style scoped>
.exam-preview-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  overflow: hidden;
}

.epp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-section);
}
.epp-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.epp-title {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--text-primary);
}
.epp-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.epp-stat {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.epp-stat b {
  color: var(--text-primary);
}
.score-full {
  color: var(--el-color-success) !important;
}
.score-short {
  color: var(--el-color-warning) !important;
}

.epp-progress {
  padding: 0 16px;
  margin-top: 8px;
}

.epp-hint {
  padding: 6px 16px;
  font-size: var(--fs-xs);
}
.epp-hint--warn {
  color: var(--el-color-warning);
}
.epp-hint--ok {
  color: var(--el-color-success);
}

.epp-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--fs-base);
}
.epp-empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
  opacity: 0.4;
}
.epp-empty-sub {
  font-size: var(--fs-sm);
  margin-top: 4px;
  opacity: 0.7;
}

.epp-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

/* 题目卡片 */
.eq-card {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
  transition: all var(--transition-fast);
  user-select: none;
}
.eq-card:hover {
  border-color: var(--border-color);
}
.eq-card--expanded {
  border-color: var(--primary-color);
}
.eq-card--dragging {
  opacity: 0.5;
}

.eq-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
}
.eq-handle {
  cursor: grab;
  color: var(--text-secondary);
  font-size: var(--fs-md);
}
.eq-handle:active {
  cursor: grabbing;
}
.eq-num {
  font-weight: 700;
  font-size: var(--fs-sm);
  color: var(--primary-color);
  min-width: 24px;
}
.eq-type-icon {
  flex-shrink: 0;
}
.eq-text {
  flex: 1;
  font-size: var(--fs-sm);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.eq-type-tag {
  flex-shrink: 0;
}
.eq-score {
  font-weight: 600;
  font-size: var(--fs-sm);
  color: var(--primary-color);
  min-width: 36px;
  text-align: right;
}
.eq-actions {
  display: flex;
  gap: 0;
  flex-shrink: 0;
}

/* 展开编辑区 */
.eq-detail {
  border-top: 1px solid var(--border-light);
}
.eq-detail-body {
  padding: 12px 16px;
}
.eq-full-text {
  font-size: var(--fs-base);
  color: var(--text-primary);
  line-height: 1.7;
  margin-bottom: 10px;
}
.eq-options {
  margin-bottom: 8px;
}
.eq-opt {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  padding: 2px 0;
}
.eq-opt--correct {
  color: var(--el-color-success);
  font-weight: 600;
}
.eq-answer {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  margin-bottom: 10px;
}
.eq-label {
  color: var(--text-secondary);
  font-size: var(--fs-xs);
}
.eq-score-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .epp-header {
    flex-direction: column;
    gap: 6px;
    align-items: flex-start;
  }
  .eq-text {
    max-width: 140px;
  }
}
</style>
