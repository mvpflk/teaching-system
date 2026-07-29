<template>
  <div class="ph-card">
    <div class="ph-card-title">摸底诊断</div>
    <p v-if="diagPhase !== 2 && !diagResult" class="ph-card-desc">
      覆盖{{ subject.label }}核心知识模块，精确定位薄弱点
    </p>

    <template v-if="diagPhase === 0">
      <div v-if="diagError" class="ph-error-box">
        <span class="ph-error-text">{{ diagError }}</span>
        <van-button size="small" plain @click="startDiagnosis">重试</van-button>
      </div>
      <van-notice-bar
        left-icon="info-o"
        scrollable
        text="判分标准：选择题/判断 → 选项字母精确匹配；填空题 → 去标点多答案模糊匹配；问答题 → 仅检查完成度，待教师评阅"
        background="var(--primary-light)"
        color="var(--primary-color)"
        class="ph-notice-rules"
      />
      <van-button
        type="primary"
        block
        round
        :loading="diagLoading"
        style="background: var(--primary-color); border-color: var(--primary-color)"
        @click="startDiagnosis"
      >
        开始诊断测试
      </van-button>
    </template>

    <DiagnosisQuizPanel
      v-else-if="diagPhase === 2"
      ref="quizPanelRef"
      :questions="diagQuestions"
      :answered-questions="answeredQuestions"
      :current-index="currentIndex"
      @submit="handleSubmitAnswer"
      @skip="handleSkip"
      @next="goToNextQuestion"
      @finish="finishDiagnosis"
    >
      <template #math-input="{ question }">
        <MathInputArea
          :question="question"
          :upload-url="uploadUrl"
          :upload-headers="uploadHeaders"
          @update:answer="currentAnswer = $event"
          @update:ocr-text="photoOcrText = $event"
          @update:attachment-path="photoAttachmentPath = $event"
          @update:ocr-confidence="photoOcrConfidence = $event"
          @update:ocr-confirmed="photoOcrConfirmed = $event"
        />
      </template>
    </DiagnosisQuizPanel>

    <DiagnosisResultPanel
      v-if="diagPhase === 3 && diagResult"
      :result="diagResult"
      @reset="resetDiagnosis"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import 'vant/es/toast/style';
import { ElMessage } from 'element-plus';
import { getDiagnosis, submitDiagnosis, submitAnswer } from '@/api/precision';
import { usePrecisionStore } from '@/stores/precision';
import DiagnosisQuizPanel from './DiagnosisQuizPanel.vue';
import DiagnosisResultPanel from './DiagnosisResultPanel.vue';
import MathInputArea from './MathInputArea.vue';

const props = defineProps({
  subject: { type: Object, required: true },
});
defineEmits(['diagnosis-complete']);

const router = useRouter();
const { setDiagnosisReport } = usePrecisionStore();

const diagPhase = ref(0);
const diagLoading = ref(false);
const diagQuestions = ref([]);
const diagResult = ref(null);
const diagError = ref('');
const currentIndex = ref(0);
const currentAnswer = ref('');
const answeredQuestions = ref([]);
const quizPanelRef = ref(null);

const photoOcrText = ref('');
const photoAttachmentPath = ref('');
const photoOcrConfidence = ref(0);
const photoOcrConfirmed = ref(false);

const uploadUrl = import.meta.env.VITE_API_BASE_URL
  ? import.meta.env.VITE_API_BASE_URL + '/api/precision/upload-answer'
  : '/api/precision/upload-answer';
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token') || '';
  return { Authorization: 'Bearer ' + token };
});

function handleSubmitAnswer({ answer, multiAnswer }) {
  const q = diagQuestions.value[currentIndex.value];
  const rawAnswer = multiAnswer.length ? multiAnswer.sort().join(',') : String(answer || '');
  const payload = {
    questionId: q.questionId,
    answer: rawAnswer,
    subject: props.subject.key,
    questionType: q.questionType,
  };

  submitAnswer(payload)
    .then((res) => {
      if (res.code === 200) {
        quizPanelRef.value?.setFeedback(res.data);
        answeredQuestions.value[currentIndex.value] = {
          questionId: q.questionId,
          answer: payload.answer,
          correct: res.data.correct,
          matchMode: res.data.matchMode,
          skipped: false,
        };
      } else {
        ElMessage.warning(res.msg || '操作失败');
      }
    })
    .catch(() => showToast('提交失败，请重试'));
}

function handleSkip() {
  answeredQuestions.value[currentIndex.value] = {
    questionId: diagQuestions.value[currentIndex.value].questionId,
    answer: '',
    correct: false,
    skipped: true,
  };
  currentAnswer.value = '';
  if (currentIndex.value < diagQuestions.value.length - 1) currentIndex.value++;
}

function goToNextQuestion() {
  if (currentIndex.value >= diagQuestions.value.length - 1) {
    finishDiagnosis();
  } else {
    currentAnswer.value = '';
    currentIndex.value++;
  }
}

async function finishDiagnosis() {
  const answers = answeredQuestions.value.filter(Boolean).map((a) => ({
    questionId: a.questionId,
    answer: a.answer || '',
    questionType:
      diagQuestions.value.find((q) => q.questionId === a.questionId)?.questionType || '',
  }));
  try {
    const res = await submitDiagnosis({ subject: props.subject.key, answers });
    if (res.code === 200) {
      diagPhase.value = 3;
      diagResult.value = res.data;
      setDiagnosisReport(res.data.diagnosisReport || res.data);
      router.push({ name: 'DiagnoseResult' });
      showToast('诊断完成');
    } else {
      ElMessage.warning(res.msg || '操作失败');
    }
  } catch {
    showToast('提交诊断失败');
  }
}

async function startDiagnosis() {
  diagLoading.value = true;
  diagResult.value = null;
  diagError.value = '';
  try {
    const res = await getDiagnosis(props.subject.key);
    if (res.code === 200 && res.data?.cooldown) {
      const cd = res.data;
      diagError.value = `${cd.message || '诊断冷冻期'}（距离下次诊断还有 ${cd.remainingDays || '?'} 天）`;
      diagLoading.value = false;
      return;
    }
    if (res.code === 200 && res.data?.questions?.length) {
      const questions = res.data.questions;
      currentIndex.value = 0;
      currentAnswer.value = '';
      answeredQuestions.value = new Array(questions.length).fill(null);
      diagQuestions.value = questions;
      diagPhase.value = 2;
    } else {
      diagError.value = res.data?.warning || '该学科题库题目不足，请联系管理员导入种子数据';
    }
  } catch {
    diagError.value = '获取诊断题目失败，请检查网络后重试';
  }
  diagLoading.value = false;
}

function resetDiagnosis() {
  diagPhase.value = 0;
  diagResult.value = null;
  diagQuestions.value = [];
  currentIndex.value = 0;
  currentAnswer.value = '';
  answeredQuestions.value = [];
}

defineExpose({ resetDiagnosis });
</script>

<style scoped>
.ph-card {
  margin: 0 16px;
  padding: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
}
.ph-card-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.ph-card-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin: 0 0 16px;
  line-height: 1.5;
}
.ph-error-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  margin-bottom: 12px;
  background: #fff3f0;
  border: 1px solid #ffccc7;
  border-radius: var(--radius-sm);
}
.ph-error-text {
  font-size: var(--fs-xs);
  color: var(--el-color-danger);
  flex: 1;
  margin-right: 8px;
}
.ph-notice-rules {
  margin-bottom: 12px;
  border-radius: var(--radius-sm);
}
</style>
