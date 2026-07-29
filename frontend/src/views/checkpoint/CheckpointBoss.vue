<template>
  <div class="cb-page">
    <div class="cb-boss-header">
      <el-button
        text
        style="color: white"
        @click="$router.push(`/student/checkpoint/${subjectId}`)"
      >
        &larr; 返回
      </el-button>
      <div class="cb-boss-icon">👑</div>
      <div class="cb-boss-info">
        <h3>{{ data.taskName || 'Boss战' }}</h3>
        <span class="cb-boss-tag">Boss战 · 挑战模式</span>
      </div>
    </div>

    <div v-loading="loading" class="cb-body">
      <div v-if="data.retryable && data.bestScore != null" class="cb-info">
        <el-alert
          title="之前成绩"
          :description="'最佳正确数：' + data.bestScore + '，可重考（消耗2积分）'"
          type="info"
          show-icon
          :closable="false"
        />
      </div>

      <div v-if="questions.length" class="cb-questions">
        <div v-for="(q, i) in questions" :key="i" class="cb-q-item">
          <div class="cb-q-num">{{ i + 1 }}</div>
          <CheckpointQuestion :question="q" @submit="(a) => handleAnswer(i, a)" />
        </div>
      </div>

      <div v-if="answers.length >= questions.length && !submitted" class="cb-submit">
        <el-button
          :loading="submitting"
          class="cb-submit-btn"
          @click="handleSubmit"
        >
          提交全部答案
        </el-button>
      </div>

      <div v-if="result" class="cb-result">
        <el-divider />
        <div class="cb-score">
          <span class="cb-score-num">{{ result.correctCount }}/{{ result.totalCount }}</span>
          <span class="cb-score-rate">正确率 {{ result.accuracy }}%</span>
        </div>
        <div class="cb-msg">{{ result.passed ? '通过！' : '未通过（需≥80%）' }}</div>
        <div v-if="result.creditsEarned" class="cb-credit">+{{ result.creditsEarned }} 积分</div>
        <div class="cb-actions">
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
import { startBoss, submitBoss, retryBoss } from '@/api/checkpoint';
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
  const res = await startBoss(configId.value);
  if (res.code === 200) data.value = res.data;
  loading.value = false;
});

function handleAnswer(idx, ans) {
  answers.value[idx] = ans;
}

async function handleSubmit() {
  submitting.value = true;
  const res = await submitBoss(configId.value, answers.value);
  if (res.code === 200) {
    result.value = res.data;
    submitted.value = true;
  }
  submitting.value = false;
}

async function handleRetry() {
  const res = await retryBoss(configId.value);
  if (res.code === 200) {
    data.value = res.data;
    answers.value = [];
    submitted.value = false;
    result.value = null;
  }
}
</script>

<style scoped>
.cb-page {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

.cb-boss-header {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  color: white;
}
.cb-boss-icon {
  font-size: 32px;
}
.cb-boss-info h3 {
  margin: 0;
  font-size: 18px;
}
.cb-boss-tag {
  font-size: 12px;
  opacity: 0.8;
}

.cb-body {
}
.cb-info {
  margin-bottom: var(--spacing-md);
}
.cb-questions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}
.cb-q-item {
}
.cb-q-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--el-color-danger);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 6px;
}
.cb-submit {
  margin-top: var(--spacing-lg);
  text-align: center;
}
.cb-submit-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border: none;
  color: white;
  padding: 10px 32px;
  border-radius: 8px;
  font-weight: 600;
}
.cb-submit-btn:hover {
  background: linear-gradient(135deg, var(--primary-dark), var(--accent-color));
}
.cb-result {
  text-align: center;
  margin-top: var(--spacing-md);
}
.cb-score {
  margin-top: var(--spacing-md);
}
.cb-score-num {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-color);
  display: block;
}
.cb-score-rate {
  font-size: var(--fs-base);
  color: var(--text-secondary);
}
.cb-msg {
  margin-top: 8px;
  font-size: var(--fs-lg);
  font-weight: 600;
}
.cb-credit {
  margin-top: 4px;
  font-size: var(--fs-lg);
  color: var(--el-color-warning);
  font-weight: 600;
}
.cb-actions {
  margin-top: var(--spacing-md);
  display: flex;
  gap: 8px;
  justify-content: center;
}
</style>
