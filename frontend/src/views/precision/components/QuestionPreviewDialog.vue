<template>
  <van-dialog
    :show="visible"
    title="答题"
    :show-confirm-button="false"
    close-on-click-overlay
    style="width: 90vw; max-width: 500px"
    @update:show="$emit('update:visible', $event)"
    @closed="closePreview"
  >
    <div
      v-if="question"
      class="lr-preview"
      style="padding: 16px; max-height: 60vh; overflow-y: auto"
    >
      <div class="lr-preview-meta">
        <van-tag type="primary" size="mini">{{ typeLabel(question.questionType) }}</van-tag>
        <DifficultyBadge
          v-if="question.difficultyLevel"
          :difficulty-level="question.difficultyLevel"
        />
        <span v-if="question._inline" class="lr-inline-badge">AI 生成</span>
      </div>
      <div
        style="margin: 12px 0; line-height: 1.7; word-break: break-word"
        v-html="renderMarkdown(question.questionText || '')"
      ></div>

      <template v-if="!previewAnswered">
        <div v-if="previewIsChoice" class="options-list">
          <div
            v-for="(opt, oi) in parseOptions(question.options)"
            :key="oi"
            :class="['option-item', { selected: previewAnswer === opt.key }]"
            @click="previewAnswer = opt.key"
          >
            <strong>{{ opt.key }}.</strong> <span v-html="renderMarkdown(opt.text)" />
          </div>
        </div>
        <van-field
          v-else
          v-model="previewAnswer"
          type="textarea"
          :rows="3"
          placeholder="请输入你的答案..."
        />
        <van-button
          type="primary"
          block
          round
          size="small"
          :loading="previewSubmitting"
          :disabled="!previewAnswer || previewSubmitting"
          style="
            margin-top: 10px;
            background: var(--primary-color);
            border-color: var(--primary-color);
          "
          @click="submitPreviewAnswer"
        >
          提交答案
        </van-button>
      </template>

      <template v-else>
        <div
          :class="['feedback-card', previewIsCorrect ? 'fb-correct' : 'fb-wrong']"
          style="margin-top: 10px"
        >
          <div class="fb-icon">{{ previewIsCorrect ? '✅' : '❌' }}</div>
          <div class="fb-title">{{ previewIsCorrect ? '回答正确！' : '回答错误' }}</div>
          <div class="fb-answer-row">
            <span>你的答案：<strong
              :class="previewIsCorrect ? 'text-green' : 'text-red'"
              v-html="renderMarkdown(previewAnswer)"
            ></strong></span>
            <span v-if="!previewIsCorrect">
              正确答案：<strong
                class="text-green"
                v-html="renderMarkdown(question._feedbackAnswer || question.correctAnswer)"
              ></strong>
            </span>
          </div>
          <div
            v-if="question._feedbackExplanation || question.explanation"
            class="fb-explanation"
            style="margin-top: 8px"
          >
            <div class="fb-ex-title">📖 解析</div>
            <div
              v-html="renderMarkdown(question._feedbackExplanation || question.explanation)"
            ></div>
          </div>
        </div>
      </template>
    </div>
  </van-dialog>
</template>

<script setup>
import { ref, computed } from 'vue';
import { showToast } from 'vant';
import 'vant/es/toast/style';
import { typeLabel, parseOptions } from '@/composables/useQuestionHelpers';
import { renderMarkdown } from '@/utils/markdown';
import { submitAnswer } from '@/api/precision';
import DifficultyBadge from '@/components/common/DifficultyBadge.vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  question: { type: Object, default: null },
  subject: { type: Object, required: true },
});
const emit = defineEmits(['update:visible', 'answer-submitted']);

const previewAnswer = ref('');
const previewAnswered = ref(false);
const previewIsCorrect = ref(false);
const previewSubmitting = ref(false);

const previewIsChoice = computed(() => {
  const t = props.question?.questionType;
  return ['SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE'].includes(t);
});

async function submitPreviewAnswer() {
  const q = props.question;
  if (!q || !previewAnswer.value || previewSubmitting.value) return;
  previewSubmitting.value = true;

  if (q.id > 0 && q.questionType && !q._inline) {
    try {
      const payload = {
        questionId: q.id,
        answer: String(previewAnswer.value || ''),
        subject: props.subject.key,
        questionType: q.questionType,
      };
      const res = await submitAnswer(payload);
      if (res.code === 200) {
        previewIsCorrect.value = res.data.correct === true;
        if (res.data.explanation) q._feedbackExplanation = res.data.explanation;
        if (res.data.correctAnswer) q._feedbackAnswer = res.data.correctAnswer;
        previewAnswered.value = true;
        emit('answer-submitted', { correct: previewIsCorrect.value, question: q });
      } else {
        showToast(res.msg || '提交失败');
      }
    } catch {
      showToast('提交失败，请重试');
    }
    previewSubmitting.value = false;
    return;
  }

  const correctAns = (q.correctAnswer || '').trim().toLowerCase();
  const userAns = String(previewAnswer.value || '')
    .trim()
    .toLowerCase();
  previewIsCorrect.value = userAns === correctAns;
  previewAnswered.value = true;
  previewSubmitting.value = false;
  emit('answer-submitted', { correct: previewIsCorrect.value, question: q });
}

function closePreview() {
  previewAnswered.value = false;
  previewAnswer.value = '';
  previewIsCorrect.value = false;
  previewSubmitting.value = false;
}
</script>

<style scoped>
.lr-preview-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.lr-inline-badge {
  font-size: 10px;
  color: var(--accent-color);
  background: var(--bg-deco-purple-light);
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: auto;
}
.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}
.option-item {
  padding: 12px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.option-item:hover {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.option-item.selected {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.feedback-card {
  border-radius: 10px;
  padding: 24px;
  text-align: center;
}
.feedback-card.fb-correct {
  background: var(--el-color-success-light);
  border: 1px solid var(--el-color-success);
}
.feedback-card.fb-wrong {
  background: var(--el-color-danger-light);
  border: 1px solid var(--el-color-danger);
}
.fb-icon {
  font-size: 2rem;
  margin-bottom: 8px;
}
.fb-title {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 10px;
}
.fb-answer-row {
  font-size: 0.85rem;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.text-green {
  color: var(--el-color-success);
}
.text-red {
  color: var(--el-color-danger);
}
.fb-explanation {
  background: var(--bg-card);
  border-radius: 6px;
  padding: 12px;
  text-align: left;
  border-left: 3px solid var(--primary-color);
  margin-bottom: 10px;
  font-size: 0.85rem;
}
.fb-ex-title {
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 4px;
  font-size: 0.8rem;
}
</style>
