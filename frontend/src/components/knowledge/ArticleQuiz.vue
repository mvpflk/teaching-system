<template>
  <div class="quiz">
    <div v-if="!started" class="quiz-start">
      <el-button type="primary" @click="start">开始本章自测</el-button>
      <p class="subtitle">{{ questions.length }} 道题，即时反馈，不计入成绩</p>
    </div>
    <div v-else-if="current < questions.length" class="quiz-question">
      <div class="q-progress">{{ current + 1 }} / {{ questions.length }}</div>
      <h4>{{ questions[current].question }}</h4>

      <!-- 选择题 -->
      <el-radio-group
        v-if="questions[current].type === 'choice'"
        v-model="selectedAnswer"
        class="choice-group"
        @change="checkAnswer"
      >
        <el-radio
          v-for="(opt, i) in questions[current].options"
          :key="i"
          :value="String.fromCharCode(65 + i)"
          :disabled="answered"
          class="choice-item"
        >
          {{ String.fromCharCode(65 + i) }}. {{ opt }}
        </el-radio>
      </el-radio-group>

      <!-- 多选题 -->
      <el-checkbox-group
        v-if="questions[current].type === 'multi'"
        v-model="selectedMulti"
        class="multi-group"
        :disabled="answered"
      >
        <el-checkbox
          v-for="(opt, i) in questions[current].options"
          :key="i"
          :value="String.fromCharCode(65 + i)"
          class="multi-item"
        >
          {{ String.fromCharCode(65 + i) }}. {{ opt }}
        </el-checkbox>
        <el-button type="primary" size="small" :disabled="answered || selectedMulti.length === 0" style="margin-top:8px" @click="checkMulti">提交答案</el-button>
      </el-checkbox-group>

      <!-- 判断题 -->
      <div v-if="questions[current].type === 'judge'" class="judge-group">
        <el-button :type="selectedAnswer === 'T' ? 'success' : ''" :disabled="answered" size="large" @click="selectedAnswer='T';checkAnswer()">✅ 正确</el-button>
        <el-button :type="selectedAnswer === 'F' ? 'danger' : ''" :disabled="answered" size="large" @click="selectedAnswer='F';checkAnswer()">❌ 错误</el-button>
      </div>

      <!-- 填空题 -->
      <div v-if="questions[current].type === 'fill'" class="fill-group">
        <el-input v-model="fillAnswer" placeholder="请输入答案..." :disabled="answered" size="large" @keyup.enter="submitFill" />
        <el-button type="primary" :disabled="answered || !fillAnswer.trim()" style="margin-top:8px" @click="submitFill">提交答案</el-button>
      </div>

      <div v-if="answered" class="feedback">
        <el-alert :type="isCorrect ? 'success' : 'error'" :closable="false">
          {{ isCorrect ? '✅ 正确！' : '❌ 错误' }}
          <template v-if="!isCorrect">
            <span v-if="questions[current].type === 'fill'">
              你的答案：{{ fillAnswer }}，正确答案：{{ questions[current].answer }}
            </span>
            <span v-else>正确答案：{{ questions[current].answer }}</span>
          </template>
        </el-alert>
        <p v-if="questions[current].explanation" class="explanation">
          💡 {{ questions[current].explanation }}
        </p>
        <el-button type="primary" style="margin-top: 8px" @click="next">
          {{ current < questions.length - 1 ? '下一题' : '查看结果' }}
        </el-button>
      </div>
    </div>
    <div v-else class="quiz-result">
      <el-result
        :icon="score >= 60 ? 'success' : 'warning'"
        :title="'正确率 ' + Math.round(score) + '%'"
        :sub-title="
          score >= 80
            ? '掌握得很好！'
            : score >= 60
              ? '还不错，继续加油！'
              : '建议再复习一遍文章内容'
        "
      />
      <div v-if="wrongQuestions.length" class="wrong-summary">
        <h5>📝 答错的题目（{{ wrongQuestions.length }}道），建议重新复习相关章节</h5>
        <div v-for="(q, i) in wrongQuestions" :key="i" class="wrong-item">
          <strong>{{ q.question }}</strong>
          <p>正确答案：{{ q.answer }}</p>
        </div>
      </div>
      <el-button @click="start">重新测试</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { saveQuizResult } from '@/api/knowledgeBase';

const props = defineProps({
  questions: { type: Array, default: () => [] },
  articleId: { type: [Number, String], default: null },
});

const started = ref(false);
const current = ref(0);
const selectedAnswer = ref('');
const selectedMulti = ref([]);
const fillAnswer = ref('');
const answered = ref(false);
const correctCount = ref(0);
const wrongQuestions = ref([]);

const isCorrect = computed(() => {
  const q = props.questions[current.value];
  if (q.type === 'fill') {
    const userAns = (fillAnswer.value || '').trim().toLowerCase();
    const correctAns = (q.answer || '').trim().toLowerCase();
    if (userAns === correctAns) return true;
    return (q.acceptAnswers || []).some((a) => userAns === a.trim().toLowerCase());
  }
  if (q.type === 'multi') {
    const user = [...selectedMulti.value].sort().join(',');
    const correct = (q.answer || '')
      .split(',')
      .map((s) => s.trim())
      .sort()
      .join(',');
    return user === correct;
  }
  return selectedAnswer.value === q.answer;
});

const score = computed(() => {
  if (props.questions.length === 0) return 0;
  return (correctCount.value / props.questions.length) * 100;
});

function start() {
  started.value = true;
  current.value = 0;
  correctCount.value = 0;
  wrongQuestions.value = [];
  answered.value = false;
  selectedAnswer.value = '';
  fillAnswer.value = '';
}

function checkAnswer() {
  if (answered.value) return;
  answered.value = true;
  if (isCorrect.value) {
    correctCount.value++;
  } else {
    wrongQuestions.value.push(props.questions[current.value]);
  }
}

function checkMulti() {
  if (answered.value) return;
  answered.value = true;
  if (isCorrect.value) {
    correctCount.value++;
  } else {
    wrongQuestions.value.push(props.questions[current.value]);
  }
}

function submitFill() {
  if (!fillAnswer.value.trim()) return;
  selectedAnswer.value = fillAnswer.value;
  checkAnswer();
}

function next() {
  current.value++;
  answered.value = false;
  selectedAnswer.value = '';
  selectedMulti.value = [];
  fillAnswer.value = '';
  if (current.value >= props.questions.length && props.articleId) {
    saveResult();
  }
}

async function saveResult() {
  if (!props.articleId) return;
  try {
    const wrongIds = wrongQuestions.value.map((q, i) => props.questions.indexOf(q));
    await saveQuizResult(props.articleId, {
      totalQuestions: props.questions.length,
      correctCount: correctCount.value,
      wrongQuestionIds: JSON.stringify(wrongIds),
    });
  } catch {
    /* fire-and-forget */
  }
}
</script>

<style scoped>
.quiz { max-width: 600px; margin: 0 auto; }
.quiz-start { text-align: center; padding: 20px 0; }
.subtitle { color: var(--text-secondary); font-size: var(--fs-sm); }
.q-progress { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: 8px; }
.quiz-question h4 { margin: 0 0 12px; font-size: var(--fs-md); color: var(--text-primary); }
.choice-group, .multi-group { display: flex; flex-direction: column; gap: 8px; }
.choice-item, .multi-item { padding: 8px 0; }
.judge-group { display: flex; gap: 12px; justify-content: center; margin: 16px 0; }
.fill-group { margin: 12px 0; }
.feedback { margin-top: 12px; }
.explanation { font-size: var(--fs-sm); color: var(--text-regular); margin-top: 8px; background: var(--bg-section); padding: 10px 14px; border-radius: 8px; }
.wrong-summary { margin-top: 16px; padding: 12px; background: #fef0f0; border-radius: 8px; text-align: left; }
.wrong-summary h5 { margin: 0 0 8px; font-size: var(--fs-md); }
.wrong-item { margin-bottom: 8px; }
.wrong-item strong { font-size: var(--fs-sm); }
.wrong-item p { font-size: var(--fs-xs); color: var(--el-color-danger); margin: 4px 0 0; }
</style>
