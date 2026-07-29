<template>
  <div class="cm-page">
    <div class="cm-mixed-header">
      <div class="cm-mixed-icon">⚡</div>
      <div class="cm-mixed-info">
        <h3>{{ data.taskName || '跨章混合战' }}</h3>
        <span class="cm-mixed-tag">混合战 · 综合挑战</span>
      </div>
    </div>
    <div class="cm-header">
      <el-button
        text
        @click="$router.push(`/student/checkpoint/${subjectId}`)"
      >
        &larr; 返回总览
      </el-button>
    </div>

    <div v-loading="loading" class="cm-body">
      <div v-if="data.retryable && data.bestScore != null" class="cm-info">
        <el-alert
          title="之前成绩"
          :description="'最佳正确数：' + data.bestScore"
          type="info"
          show-icon
          :closable="false"
        />
      </div>

      <div v-if="questions.length" class="cm-questions">
        <div v-for="(q, i) in questions" :key="i" class="cm-q-item">
          <div class="cm-q-num">{{ i + 1 }}</div>
          <CheckpointQuestion :question="q" @submit="(a) => handleAnswer(i, a)" />
        </div>
      </div>

      <div v-if="answers.length >= questions.length && !submitted" class="cm-submit">
        <el-button
          class="cm-submit-btn"
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          提交全部答案
        </el-button>
      </div>

      <div v-if="result" class="cm-result">
        <el-divider />
        <div class="cm-score">
          <span class="cm-score-num">{{ result.correctCount }}/{{ result.totalCount }}</span>
          <span class="cm-score-rate">正确率 {{ result.accuracy }}%</span>
        </div>
        <div class="cm-msg">{{ result.passed ? '通过！' : '未通过（需≥80%）' }}</div>
        <div v-if="result.creditsEarned" class="cm-credit">+{{ result.creditsEarned }} 积分</div>
        <div class="cm-actions">
          <el-button
            v-if="result.retryable"
            type="warning"
            @click="handleRetry"
          >
            重考 (-2积分)
          </el-button>
          <el-button @click="$router.push(`/student/checkpoint/${subjectId}`)">返回总览</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { startMixed, submitMixed, retryMixed } from '@/api/checkpoint';
import CheckpointQuestion from '@/components/checkpoint/CheckpointQuestion.vue';

const route = useRoute();
const subjectId = computed(() => Number(route.params.subjectId));
const configId = computed(() => Number(route.params.configId));

const loading = ref(true);
const data = ref({});
const answers = ref([]);
const submitted = ref(false);
const submitting = ref(false);
const result = ref(null);

const questions = computed(() => data.value.questions || []);

onMounted(async () => {
  const res = await startMixed(configId.value);
  if (res.code === 200) data.value = res.data;
  loading.value = false;
});

function handleAnswer(idx, ans) {
  answers.value[idx] = ans;
}

async function handleSubmit() {
  submitting.value = true;
  const res = await submitMixed(configId.value, answers.value);
  if (res.code === 200) {
    result.value = res.data;
    submitted.value = true;
  }
  submitting.value = false;
}

async function handleRetry() {
  const res = await retryMixed(configId.value);
  if (res.code === 200) {
    data.value = res.data;
    answers.value = [];
    submitted.value = false;
    result.value = null;
  }
}
</script>

<style scoped>
.cm-page {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}
.cm-mixed-header {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  color: white;
}
.cm-mixed-icon {
  font-size: 32px;
}
.cm-mixed-info h3 {
  margin: 0;
  font-size: 18px;
  color: white;
}
.cm-mixed-tag {
  font-size: 12px;
  opacity: 0.8;
}
.cm-header {
  margin-bottom: var(--spacing-md);
  display: flex;
  align-items: center;
  gap: 8px;
}
.cm-tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(67, 97, 238, 0.08);
  color: var(--primary-color);
  font-size: var(--fs-xs);
  font-weight: 600;
}
.cm-info {
  margin-bottom: var(--spacing-md);
}
.cm-questions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}
.cm-q-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 6px;
}
.cm-submit {
  margin-top: var(--spacing-lg);
  text-align: center;
}
.cm-submit-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color)) !important;
  border: none !important;
}
.cm-result {
  text-align: center;
  margin-top: var(--spacing-md);
}
.cm-score-num {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-color);
  display: block;
}
.cm-score-rate {
  font-size: var(--fs-base);
  color: var(--text-secondary);
}
.cm-msg {
  margin-top: 8px;
  font-size: var(--fs-lg);
  font-weight: 600;
}
.cm-credit {
  margin-top: 4px;
  font-size: var(--fs-lg);
  color: var(--el-color-warning);
  font-weight: 600;
}
.cm-actions {
  margin-top: var(--spacing-md);
  display: flex;
  gap: 8px;
  justify-content: center;
}
</style>
