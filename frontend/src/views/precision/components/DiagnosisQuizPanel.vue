<template>
  <div class="diag-quiz">
    <div class="quiz-progress">
      <div class="progress-bar">
        <div
          class="progress-fill"
          :style="{ width: (currentIndex / questions.length) * 100 + '%' }"
        ></div>
      </div>
      <span class="progress-text">第 {{ currentIndex + 1 }} 题 / 共 {{ questions.length }} 题</span>
    </div>
    <div class="step-dots">
      <span
        v-for="(q, i) in questions"
        :key="q.questionId"
        :class="[
          'dot',
          {
            done: answeredQuestions[i]?.correct === true,
            wrong: answeredQuestions[i]?.correct === false,
            skipped: answeredQuestions[i]?.skipped,
            pending: answeredQuestions[i]?.matchMode === 'pending_review',
            current: i === currentIndex && !answeredQuestions[i],
          },
        ]"
      >
        {{
          answeredQuestions[i]?.skipped
            ? '⊘'
            : answeredQuestions[i]?.correct === true
              ? '✓'
              : answeredQuestions[i]?.correct === false
                ? '✗'
                : answeredQuestions[i]?.matchMode === 'pending_review'
                  ? '📝'
                  : i + 1
        }}
      </span>
    </div>

    <div
      v-if="currentIndex < questions.length"
      class="quiz-card"
      :class="{ 'feedback-shown': feedbackState }"
    >
      <template v-if="!feedbackState">
        <div class="question-header">
          <span class="q-type-tag">{{ typeLabel(questions[currentIndex].questionType) }}</span>
          <DifficultyBadge
            :difficulty-level="questions[currentIndex]?.difficultyLevel"
            :tier="questions[currentIndex]?.tier"
          />
        </div>
        <div
          class="question-body"
          v-html="renderMarkdown(questions[currentIndex].questionText || '')"
        ></div>
        <div v-if="isMultiType(questions[currentIndex].questionType)" class="options-list">
          <div
            v-for="(opt, oi) in parseOptions(questions[currentIndex].options)"
            :key="oi"
            :class="['option-item', { selected: currentMultiAnswer.includes(opt.key) }]"
            @click="
              currentMultiAnswer.includes(opt.key)
                ? (currentMultiAnswer = currentMultiAnswer.filter((k) => k !== opt.key))
                : (currentMultiAnswer = [...currentMultiAnswer, opt.key])
            "
          >
            <strong>{{ opt.key }}.</strong> <span v-html="renderMarkdown(opt.text)" />
          </div>
        </div>
        <div v-else-if="isChoiceType(questions[currentIndex].questionType)" class="options-list">
          <div
            v-for="(opt, oi) in parseOptions(questions[currentIndex].options)"
            :key="oi"
            :class="['option-item', { selected: currentAnswer === opt.key }]"
            @click="currentAnswer = opt.key"
          >
            <strong>{{ opt.key }}.</strong> <span v-html="renderMarkdown(opt.text)" />
          </div>
        </div>
        <div
          v-else-if="isMathInputType(questions[currentIndex].questionType)"
          class="calc-input-area"
        >
          <!-- 步骤引导 -->
          <div class="calc-steps-guide">
            <div class="calc-step-item"><span class="step-num">1</span> 列出已知条件和所用公式</div>
            <div class="calc-step-item">
              <span class="step-num">2</span> 写出推导过程（公式用工具栏插入或 \$...\$ 包裹）
            </div>
            <div class="calc-step-item"><span class="step-num">3</span> 给出最终答案</div>
          </div>

          <!-- LaTeX 工具栏 + 输入区 + 实时预览 -->
          <MathFormulaEditor v-model="currentAnswer" />

          <!-- 初次使用提示 -->
          <div v-if="showLatexTip" class="calc-latex-tip">
            <div class="tip-header" @click="showLatexTip = false">
              <van-icon name="question-o" />
              <span>初次使用公式编辑器？点击查看快速指南</span>
              <van-icon name="cross" />
            </div>
            <div class="tip-body">
              <table class="latex-quick-ref">
                <tr>
                  <th>写法</th>
                  <th>效果</th>
                  <th>写法</th>
                  <th>效果</th>
                </tr>
                <tr>
                  <td><code>\$x^2\$</code></td>
                  <td>x²</td>
                  <td><code>\$\\frac{a}{b}\$</code></td>
                  <td>a/b 分式</td>
                </tr>
                <tr>
                  <td><code>\$\\sqrt{3}\$</code></td>
                  <td>√3</td>
                  <td><code>\$\\pm\$</code></td>
                  <td>±</td>
                </tr>
                <tr>
                  <td><code>\$\\geq\$</code></td>
                  <td>≥</td>
                  <td><code>\$\\leq\$</code></td>
                  <td>≤</td>
                </tr>
                <tr>
                  <td><code>\$\\times\$</code></td>
                  <td>×</td>
                  <td><code>\$\\neq\$</code></td>
                  <td>≠</td>
                </tr>
                <tr>
                  <td><code>\$\\sin\$</code></td>
                  <td>sin</td>
                  <td><code>\$\\pi\$</code></td>
                  <td>π</td>
                </tr>
              </table>
              <p class="tip-footer">也可以直接写汉字和数字，只在需要公式时用上方工具栏插入符号。</p>
            </div>
          </div>
        </div>
        <van-field
          v-else
          v-model="currentAnswer"
          type="textarea"
          :rows="3"
          :placeholder="
            questions[currentIndex].questionType === 'ESSAY' ? '请输入你的回答...' : '请输入答案...'
          "
        />
        <div class="quiz-actions">
          <TooHardButton
            :question-id="questions[currentIndex]?.id"
            :knowledge-summary="questions[currentIndex]?.knowledgeSummary"
            @skip="$emit('skip')"
          />
          <button
            class="btn-submit"
            :disabled="
              isMultiType(questions[currentIndex].questionType)
                ? !currentMultiAnswer.length
                : !currentAnswer && currentAnswer !== 0
            "
            @click="submitAnswer"
          >
            提交答案
          </button>
        </div>
      </template>

      <template v-else>
        <div
          :class="[
            'feedback-card',
            feedbackState.correct === true
              ? 'fb-correct'
              : feedbackState.correct === false
                ? 'fb-wrong'
                : 'fb-pending',
          ]"
        >
          <div class="fb-icon">
            {{
              feedbackState.correct === true ? '✅' : feedbackState.correct === false ? '❌' : '📝'
            }}
          </div>
          <div class="fb-title">
            {{
              feedbackState.correct === true
                ? '回答正确！'
                : feedbackState.correct === false
                  ? '回答错误'
                  : '已收到'
            }}
          </div>
          <div class="fb-answer-row">
            <span>你的答案：<strong :class="feedbackState.correct ? 'text-green' : 'text-red'">{{
              currentMultiAnswer.length ? currentMultiAnswer.join(',') : currentAnswer
            }}</strong></span>
            <span v-if="feedbackState.correct === false">正确答案：<strong class="text-green" v-html="renderMarkdown(feedbackState.correctAnswer)"></strong></span>
          </div>
          <div v-if="feedbackState.explanation" class="fb-explanation">
            <div class="fb-ex-title">📖 解析</div>
            <div v-html="renderMarkdown(feedbackState.explanation)"></div>
          </div>
          <div v-if="feedbackState.matchMode === 'pending_review'" class="fb-desc">
            你的回答已提交，等待教师评阅。
          </div>
          <button class="btn-next" @click="goToNextQuestion">
            {{ currentIndex < questions.length - 1 ? '下一题 →' : '查看诊断报告' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { renderMarkdown } from '@/utils/markdown';
import {
  typeLabel,
  isChoiceType,
  isMultiType,
  isMathInputType,
  parseOptions,
} from '@/composables/useQuestionHelpers';
import DifficultyBadge from '@/components/common/DifficultyBadge.vue';
import TooHardButton from '@/components/common/TooHardButton.vue';
import MathFormulaEditor from '@/components/precision/MathFormulaEditor.vue';

const props = defineProps({
  questions: { type: Array, required: true },
  answeredQuestions: { type: Array, required: true },
  currentIndex: { type: Number, required: true },
});

const emit = defineEmits(['submit', 'skip', 'next', 'finish']);

const currentAnswer = ref('');
const currentMultiAnswer = ref([]);
const feedbackState = ref(null);
const showLatexTip = ref(true);

function submitAnswer() {
  emit('submit', {
    answer: currentAnswer.value,
    multiAnswer: currentMultiAnswer.value,
    feedback: feedbackState.value,
  });
}

function goToNextQuestion() {
  if (props.currentIndex >= props.questions.length - 1) {
    emit('finish');
  } else {
    currentAnswer.value = '';
    currentMultiAnswer.value = [];
    feedbackState.value = null;
    emit('next');
  }
}

function setFeedback(feedback) {
  feedbackState.value = feedback;
}

defineExpose({ setFeedback, currentAnswer, currentMultiAnswer });
</script>

<style scoped>
.quiz-progress {
  margin-bottom: 16px;
}
.progress-bar {
  width: 100%;
  height: 6px;
  background: var(--el-border-color);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}
.progress-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: 3px;
  transition: width 0.3s;
}
.progress-text {
  font-size: 0.85rem;
  color: var(--el-text-color-secondary);
}
.step-dots {
  display: flex;
  gap: 6px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.step-dots .dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 600;
  background: #e5e7eb;
  color: #9ca3af;
}
.step-dots .dot.current {
  background: var(--primary-color);
  color: #fff;
  box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.2);
}
.step-dots .dot.done {
  background: var(--el-color-success);
  color: #fff;
}
.step-dots .dot.wrong {
  background: var(--el-color-danger);
  color: #fff;
}
.step-dots .dot.skipped {
  background: var(--el-color-warning);
  color: #fff;
}
.step-dots .dot.pending {
  background: var(--accent-color);
  color: #fff;
}
.quiz-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  padding: 20px;
}
.quiz-card.feedback-shown {
  border-color: transparent;
}
.question-header {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.q-type-tag {
  padding: 2px 10px;
  background: var(--primary-color);
  color: #fff;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}
.question-body {
  font-size: var(--fs-md);
  line-height: 1.7;
  color: var(--text-primary);
  margin-bottom: 12px;
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
  background: #f0f4ff;
}
.option-item.selected {
  border-color: var(--primary-color);
  background: #eff6ff;
}
.quiz-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color);
}
.btn-submit {
  padding: 8px 24px;
  background: var(--primary-color);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
}
.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.feedback-card {
  border-radius: 10px;
  padding: 24px;
  text-align: center;
}
.feedback-card.fb-correct {
  background: #f0fdf4;
  border: 1px solid #86efac;
}
.feedback-card.fb-wrong {
  background: #fef2f2;
  border: 1px solid #fca5a5;
}
.feedback-card.fb-pending {
  background: #eff6ff;
  border: 1px solid #93c5fd;
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
  color: #15803d;
}
.text-red {
  color: #dc2626;
}
.fb-explanation {
  background: #fff;
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
.fb-desc {
  font-size: 0.85rem;
  color: var(--el-text-color-secondary);
  margin-bottom: 10px;
}
.btn-next {
  padding: 10px 40px;
  background: var(--primary-color);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  margin-top: 8px;
}
.calc-input-area {
  margin-top: 12px;
}
.calc-steps-guide {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.calc-step-item {
  flex: 1;
  min-width: 100px;
  padding: 8px 10px;
  background: linear-gradient(135deg, var(--primary-light), #eef2ff);
  border-radius: 8px;
  font-size: 0.78rem;
  color: var(--text-regular);
  line-height: 1.5;
}
.calc-step-item .step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary-color);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  margin-right: 4px;
}
.calc-latex-tip {
  margin-top: 10px;
  border: 1px solid var(--el-color-warning-light);
  border-radius: 8px;
  overflow: hidden;
}
.calc-latex-tip .tip-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #fffbeb;
  cursor: pointer;
  font-size: 0.8rem;
  color: #92400e;
}
.calc-latex-tip .tip-body {
  padding: 10px 12px;
  background: #fff;
}
.latex-quick-ref {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
}
.latex-quick-ref th,
.latex-quick-ref td {
  padding: 4px 8px;
  border: 1px solid var(--border-base);
  text-align: center;
}
.latex-quick-ref th {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-weight: 600;
}
.latex-quick-ref code {
  background: #f0f4ff;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.72rem;
  color: var(--primary-color);
}
.tip-footer {
  margin: 8px 0 0;
  font-size: 0.75rem;
  color: var(--text-secondary);
}
</style>
