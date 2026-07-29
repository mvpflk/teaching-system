<template>
  <div class="answer-item" :class="{ 'ans-expanded': expanded, 'ans-subjective': isSubjective }">
    <div class="ai-header" @click="$emit('toggle')">
      <span class="ai-num">{{ index + 1 }}.</span>
      <el-tag :type="tagType" size="small">{{ tagLabel }}</el-tag>
      <span class="ai-type-tag">{{ typeLabel }}</span>
      <span class="ai-max-score">分值{{ question.score || 0 }}</span>
      <el-icon class="ai-expand"><ArrowDown v-if="!expanded" /><ArrowUp v-else /></el-icon>
    </div>
    <div v-show="expanded" class="ai-body">
      <QuestionRenderer
        :question="question"
        mode="display"
        :show-answer="!isSubjective"
        :highlight-correct="!isSubjective"
        :show-meta="false"
      />
      <div class="ai-answer" :class="{ 'ai-answer-big': isSubjective }">
        <span class="ai-label">学生作答：</span>
        <div class="ai-ans-text" :class="{ 'ai-ans-subjective': isSubjective }">
          {{ question.studentAnswer || '(未作答)' }}
        </div>
      </div>
      <div v-if="!isSubjective" class="ai-correct">
        <span class="ai-label">正确答案：</span>
        <span class="ai-correct-text">{{ question.correctAnswer || '-' }}</span>
      </div>
      <div v-if="question.autoScore != null && !isSubjective" class="ai-auto-score">
        自动评分 {{ question.autoScore }}/{{ question.score || 0 }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';
import { QUESTION_TYPE_LABEL, SUBJECTIVE_TYPES } from '@/constants/questionTypes';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';

const props = defineProps({
  question: { type: Object, required: true },
  index: { type: Number, default: 0 },
  expanded: { type: Boolean, default: false },
});

defineEmits(['toggle']);

const isSubjective = computed(() => SUBJECTIVE_TYPES.includes(props.question.questionType));

const typeLabel = computed(
  () => QUESTION_TYPE_LABEL[props.question.questionType] || props.question.questionType || '未知'
);

const tagType = computed(() => {
  if (props.question.isCorrect === 1) return 'success';
  if (props.question.isCorrect === 2) return 'warning';
  return 'danger';
});

const tagLabel = computed(() => {
  if (props.question.isCorrect === 1) return '正确';
  if (props.question.isCorrect === 2) return '主观';
  return '错误';
});
</script>

<style scoped>
.answer-item {
  margin-bottom: 4px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.answer-item.ans-subjective {
  border-left: 3px solid var(--el-color-warning);
}
.ai-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
  background: var(--bg-card);
}
.ai-header:hover {
  background: var(--bg-section);
}
.ai-num {
  font-weight: 600;
  min-width: 20px;
}
.ai-type-tag {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  background: var(--bg-section);
  padding: 1px 6px;
  border-radius: 3px;
}
.ai-max-score {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.ai-expand {
  margin-left: auto;
  color: var(--text-secondary);
}
.ai-body {
  padding: 10px 12px;
  background: var(--bg-card);
  border-top: 1px solid var(--border-light);
}
.ai-qtext {
  font-size: var(--fs-sm);
  line-height: 1.7;
  margin-bottom: 8px;
  padding: 8px 10px;
  background: var(--bg-section);
  border-radius: var(--radius-sm);
}
.ai-options {
  margin-bottom: 8px;
}
.ai-opt {
  font-size: var(--fs-xs);
  padding: 2px 8px;
  color: var(--text-secondary);
}
.ai-answer {
  margin-bottom: 6px;
}
.ai-answer-big {
  padding: 8px 10px;
  background: var(--bg-warning-light);
  border-radius: var(--radius-sm);
}
.ai-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  font-weight: 500;
}
.ai-ans-text {
  font-size: var(--fs-sm);
  color: var(--el-color-primary);
  margin-top: 2px;
  line-height: 1.6;
}
.ai-ans-subjective {
  font-size: var(--fs-md);
  min-height: 40px;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-primary);
  line-height: 1.8;
  padding: 6px 0;
}
.ai-correct {
  margin-bottom: 4px;
}
.ai-correct-text {
  font-size: var(--fs-sm);
  color: var(--el-color-success);
}
.ai-auto-score {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
</style>
