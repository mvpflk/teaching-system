<template>
  <div class="online-practice">
    <div class="practice-header">
      <h3>在线答题模式</h3>
      <van-button plain size="small" @click="$emit('complete')">返回打印模式</van-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <van-loading type="spinner" />
      <p>加载题目中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!submitted && questions.length === 0" class="empty-state">
      <van-empty description="暂无在线答题题目，请先生成学习包" />
      <van-button type="primary" size="small" @click="$emit('complete')">返回</van-button>
    </div>

    <!-- 答题模式 -->
    <div v-else-if="!submitted && questions.length > 0" class="question-list">
      <div v-for="(q, index) in questions" :key="q.questionId || index" class="question-item">
        <div class="question-header">
          <span class="question-index">{{ index + 1 }}.</span>
          <van-tag :type="getQuestionTypeTag(q.questionType)" size="small">
            {{ getQuestionTypeLabel(q.questionType) }}
          </van-tag>
          <span
            v-if="q.difficultyLevel"
            class="difficulty-dot"
            :style="{ background: diffColor(q.difficultyLevel) }"
          />
        </div>
        <div class="question-text" v-html="renderMarkdown(q.questionText || '')" />

        <!-- 选择题选项 -->
        <div v-if="q.options && q.options.length" class="question-options">
          <template v-if="q.questionType === 'SINGLE_CHOICE' || q.questionType === 'TRUE_FALSE'">
            <van-radio-group v-model="answers[index]">
              <van-radio
                v-for="(opt, optIdx) in q.options"
                :key="optIdx"
                :name="String.fromCharCode(65 + optIdx)"
              >
                <span v-html="renderMarkdown(stripPrefix(opt))" />
              </van-radio>
            </van-radio-group>
          </template>
          <template v-else-if="q.questionType === 'MULTI_CHOICE'">
            <van-checkbox-group v-model="multiAnswers[index]">
              <van-checkbox
                v-for="(opt, optIdx) in q.options"
                :key="optIdx"
                :name="String.fromCharCode(65 + optIdx)"
              >
                <span v-html="renderMarkdown(stripPrefix(opt))" />
              </van-checkbox>
            </van-checkbox-group>
          </template>
          <van-field v-else v-model="answers[index]" placeholder="请输入答案" />
        </div>
        <van-field
          v-else
          v-model="answers[index]"
          :placeholder="q.questionType === 'ESSAY' ? '请输入你的回答...' : '请输入答案...'"
          type="textarea"
          :rows="3"
        />
      </div>

      <van-button
        type="primary"
        block
        :loading="submitting"
        :disabled="submitting"
        @click="submitAnswers"
      >
        提交答案
      </van-button>
    </div>

    <!-- 结果模式 -->
    <div v-else-if="submitted && result" class="result-area">
      <div class="result-summary">
        <van-circle
          :rate="result.correctRate || 0"
          :text="(result.correctRate || 0) + '%'"
          color="var(--primary-color)"
          layer-color="var(--bg-secondary)"
          size="90"
          :stroke-width="50"
        />
        <div class="result-text">
          <p>正确 {{ result.correctCount || 0 }} / {{ result.totalQuestions || 0 }} 题</p>
          <p v-if="result.passed" class="result-pass">🎉 已达标</p>
          <p v-else class="result-fail">继续加油</p>
        </div>
      </div>

      <div v-if="result.itemResults?.length" class="result-details">
        <div
          v-for="(item, i) in result.itemResults"
          :key="i"
          class="result-item"
          :class="
            item.isCorrect
              ? 'ri-correct'
              : item.matchMode === 'pending_review'
                ? 'ri-pending'
                : 'ri-wrong'
          "
        >
          <div class="ri-head">
            <span>第{{ i + 1 }}题</span>
            <van-tag v-if="item.isCorrect" type="success" size="mini">正确</van-tag>
            <van-tag v-else-if="item.matchMode === 'pending_review'" type="warning" size="mini">
              待评阅
            </van-tag>
            <van-tag v-else-if="item.matchMode === 'unanswered'" size="mini" color="#9ca3af">
              未作答
            </van-tag>
            <van-tag v-else type="danger" size="mini">错误</van-tag>
          </div>
          <div class="ri-answer">
            <span>你的答案：<strong
              :class="item.isCorrect ? 'text-green' : 'text-red'"
              v-html="renderMarkdown(item.studentAnswer || '(空)')"
            ></strong></span>
            <span v-if="!item.isCorrect && item.correctAnswer">正确答案：<strong
              class="text-green"
              v-html="renderMarkdown(item.correctAnswer)"
            ></strong></span>
          </div>
          <div
            v-if="item.explanation"
            class="ri-explain"
            v-html="renderMarkdown(item.explanation)"
          ></div>
        </div>
      </div>

      <van-button
        plain
        hairline
        size="small"
        style="margin-top: 12px"
        @click="$emit('complete')"
      >
        返回学习包
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { showToast } from 'vant';
import 'vant/es/toast/style';
import { getPackQuestions, submitOnlineTest } from '@/api/precision';
import { renderMarkdown } from '@/utils/markdown';

const props = defineProps({
  subject: { type: Object, required: true },
  weekNo: { type: Number, required: true },
});

defineEmits(['complete']);

const loading = ref(false);
const submitting = ref(false);
const submitted = ref(false);
const questions = ref([]);
const answers = ref({});
const multiAnswers = ref({});
const result = ref(null);

onMounted(async () => {
  await loadQuestions();
});

async function loadQuestions() {
  loading.value = true;
  try {
    const res = await getPackQuestions(props.subject.key);
    if (res.code === 200 && res.data?.length) {
      questions.value = res.data;
    } else {
      questions.value = [];
    }
  } catch (e) {
    console.error('加载答题题目失败:', e);
    showToast('加载题目失败，请先生成学习包');
  } finally {
    loading.value = false;
  }
}

async function submitAnswers() {
  // 校验至少回答一题
  const hasAnswer = questions.value.some((q, i) => {
    const ans = answers.value[i];
    const multi = multiAnswers.value[i];
    return (ans !== undefined && ans !== '') || (multi && multi.length > 0);
  });
  if (!hasAnswer) {
    showToast('请至少回答一道题目');
    return;
  }

  submitting.value = true;
  try {
    const answerList = questions.value.map((q, index) => {
      let answer = answers.value[index] || '';
      if (q.questionType === 'MULTI_CHOICE' && multiAnswers.value[index]?.length) {
        answer = multiAnswers.value[index].sort().join(',');
      }
      return { questionId: q.questionId, answer, questionType: q.questionType };
    });

    const res = await submitOnlineTest({
      subject: props.subject.key,
      answers: answerList,
      revealAnswers: true,
    });

    if (res.code === 200) {
      result.value = res.data;
      submitted.value = true;
    } else {
      showToast(res.msg || '提交失败');
    }
  } catch (e) {
    console.error('提交答案失败:', e);
    showToast('提交失败，请重试');
  } finally {
    submitting.value = false;
  }
}

function stripPrefix(text) {
  if (!text) return '';
  return String(text).replace(/^[A-H][.．、)）]\s*/, '');
}

function diffColor(level) {
  const map = {
    1: 'var(--el-color-success)',
    2: 'var(--el-color-success)',
    3: 'var(--el-color-warning)',
    4: 'var(--el-color-danger)',
    5: 'var(--el-color-danger)',
  };
  return map[level] || 'var(--text-disabled)';
}

function getQuestionTypeTag(type) {
  const map = {
    SINGLE_CHOICE: 'primary',
    MULTI_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_IN: 'danger',
    CLOZE: 'danger',
    ESSAY: '',
  };
  return map[type] || 'default';
}

function getQuestionTypeLabel(type) {
  const map = {
    SINGLE_CHOICE: '单选',
    MULTI_CHOICE: '多选',
    TRUE_FALSE: '判断',
    FILL_IN: '填空',
    CLOZE: '完形',
    ESSAY: '问答',
    CALCULATION: '计算',
  };
  return map[type] || type;
}
</script>

<style scoped>
.online-practice {
  padding: 16px;
  background: var(--bg-card, #fff);
  border-radius: var(--radius-md, 8px);
}
.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.practice-header h3 {
  margin: 0;
  font-size: var(--fs-lg);
  color: var(--text-primary);
}
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}
.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.question-item {
  padding: 16px;
  background: var(--bg-page, #f5f7fa);
  border-radius: var(--radius-sm, 4px);
}
.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.question-index {
  font-weight: 600;
  color: var(--text-primary);
}
.difficulty-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.question-text {
  margin-bottom: 12px;
  line-height: 1.6;
  color: var(--text-regular);
}
.question-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-area {
  text-align: center;
}
.result-summary {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
}
.result-text p {
  margin: 4px 0;
  font-size: var(--fs-md);
}
.result-pass {
  color: var(--el-color-success);
  font-weight: 600;
}
.result-fail {
  color: var(--el-color-warning);
}

.result-details {
  text-align: left;
  margin-top: 12px;
}
.result-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--border-base);
  background: var(--bg-card);
}
.result-item.ri-correct {
  border-left-color: var(--el-color-success);
  background: #f1f8f1;
}
.result-item.ri-wrong {
  border-left-color: var(--el-color-danger);
  background: #fff5f5;
}
.result-item.ri-pending {
  border-left-color: var(--el-color-warning);
  background: #fffdf5;
}
.ri-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: var(--fs-sm);
}
.ri-answer {
  font-size: var(--fs-sm);
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 4px;
}
.ri-explain {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  padding-top: 6px;
  border-top: 1px dashed var(--border-base);
}
.text-green {
  color: #15803d;
}
.text-red {
  color: #dc2626;
}
</style>
