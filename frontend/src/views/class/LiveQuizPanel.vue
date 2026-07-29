<template>
  <div class="live-quiz-panel">
    <!-- 顶部返回栏 -->
    <div class="lqp-header">
      <el-button
        text
        @click="$emit('back')"
      >
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="lqp-title">随堂速答</span>
    </div>

    <!-- ==================== 阶段1: 命题(Idle) ==================== -->
    <template v-if="phase === 'idle'">
      <div class="lqp-mode-tabs">
        <el-radio-group v-model="mode" size="large">
          <el-radio-button value="choice">🎯 选择题</el-radio-button>
          <el-radio-button value="truefalse">✅ 判断题</el-radio-button>
          <el-radio-button value="pick">👤 纯抽人</el-radio-button>
        </el-radio-group>
      </div>

      <template v-if="mode === 'pick'">
        <div class="lqp-pick-area">
          <p class="lqp-pick-desc">从全班随机抽取一名学生，仅点名不答题</p>
          <el-button
            type="warning"
            size="large"
            :loading="picking"
            @click="doPick"
          >
            <el-icon><User /></el-icon> 🎲 随机抽取
          </el-button>
          <div v-if="pickedStudent" class="lqp-picked">
            <el-icon><Aim /></el-icon>
            <span>{{ pickedStudent.studentName }}</span>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="lqp-form">
          <el-input
            v-model="questionText"
            :rows="3"
            type="textarea"
            placeholder="输入题目内容..."
            class="lqp-input"
            maxlength="500"
            show-word-limit
          />
          <template v-if="mode === 'choice'">
            <div class="lqp-options">
              <div v-for="(opt, i) in options" :key="i" class="lqp-option-row">
                <span class="lqp-opt-letter">{{ getOptionLetter(i) }}.</span>
                <el-input
                  v-model="options[i]"
                  :placeholder="`选项 ${getOptionLetter(i)}`"
                  maxlength="200"
                />
              </div>
            </div>
          </template>
          <div class="lqp-answer-row">
            <span class="lqp-answer-label">正确答案：</span>
            <el-radio-group v-model="correctAnswer" class="lqp-answer-group">
              <template v-if="mode === 'choice'">
                <el-radio-button v-for="i in 4" :key="i" :value="getOptionLetter(i - 1)">
                  {{ getOptionLetter(i - 1) }}
                </el-radio-button>
              </template>
              <template v-else>
                <el-radio-button value="对">✓ 对</el-radio-button>
                <el-radio-button value="错">✗ 错</el-radio-button>
              </template>
            </el-radio-group>
          </div>
          <div class="lqp-duration-row">
            <span class="lqp-duration-label">作答倒计时：</span>
            <el-input-number
              v-model="durationSeconds"
              :min="10"
              :max="300"
              :step="10"
              size="large"
            />
            <span class="lqp-duration-unit">秒</span>
          </div>
          <el-button
            type="primary"
            size="large"
            class="lqp-publish-btn"
            :loading="publishing"
            @click="doPublish"
          >
            <el-icon><Promotion /></el-icon> 📢 发布作答
          </el-button>
        </div>
      </template>
    </template>

    <!-- ==================== 阶段2: 收集(Publishing) ==================== -->
    <template v-else-if="phase === 'publishing'">
      <div class="lqp-collecting">
        <div class="lqp-collect-header">
          <el-icon class="lqp-collect-icon"><Clock /></el-icon>
          <span>收集中...</span>
          <span class="lqp-collect-count">{{ results.submitted }}人已提交</span>
          <span class="lqp-collect-timer">{{ remainingSeconds }}s</span>
        </div>
        <div class="lqp-question-display">{{ questionText }}</div>
        <div class="lqp-results">
          <div v-for="(count, letter) in results.options" :key="letter" class="lqp-result-row">
            <span class="lqp-r-label">{{ letter }}</span>
            <div class="lqp-r-bar">
              <div class="lqp-r-fill" :style="{ width: barWidth(count) + '%' }"></div>
            </div>
            <span class="lqp-r-count">{{ count }}人</span>
          </div>
        </div>
        <div class="lqp-actions">
          <el-button type="danger" @click="doEnd(false)">
            <el-icon><SwitchButton /></el-icon> ⏹ 结束作答
          </el-button>
          <el-button type="primary" @click="doEnd(true)">
            <el-icon><View /></el-icon> 👁 显示正确答案
          </el-button>
        </div>
      </div>
    </template>

    <!-- ==================== 阶段3: 结果(Ended) ==================== -->
    <template v-else-if="phase === 'ended'">
      <div class="lqp-ended">
        <div class="lqp-ended-header">
          <el-icon><CircleCheck /></el-icon> 作答结束
          <span class="lqp-ended-count">共 {{ results.total }} 人参与</span>
        </div>
        <div class="lqp-question-display">{{ questionText }}</div>
        <div v-if="revealed && results.correctAnswer" class="lqp-correct-answer">
          ✅ 正确答案：<strong>{{ results.correctAnswer }}</strong>
        </div>
        <div class="lqp-results">
          <div v-for="(count, letter) in results.options" :key="letter" class="lqp-result-row">
            <span class="lqp-r-label">{{ letter }}</span>
            <div class="lqp-r-bar">
              <div
                class="lqp-r-fill"
                :class="{ 'is-correct': revealed && letter === results.correctAnswer }"
                :style="{ width: barWidth(count) + '%' }"
              ></div>
            </div>
            <span class="lqp-r-count">{{ count }}人</span>
            <span
              v-if="results.total > 0"
              class="lqp-r-pct"
            >{{ Math.round((count / results.total) * 100) }}%</span>
          </div>
        </div>
        <div v-if="revealed" class="lqp-accuracy">
          正确率：<strong>{{ results.accuracy }}%</strong> ({{ results.correctCount }}/{{
            results.total
          }})
        </div>
        <div class="lqp-actions">
          <el-button @click="resetForm">
            <el-icon><Edit /></el-icon> 📋 再出一题
          </el-button>
          <el-button type="primary" @click="$emit('back')">返回</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue';
import { startLiveQuiz, endLiveQuiz, pickLiveQuizStudent } from '@/api/classroom';
import { ElMessage } from 'element-plus';
import {
  ArrowLeft,
  Promotion,
  SwitchButton,
  View,
  CircleCheck,
  Edit,
  User,
  Aim,
  Clock,
} from '@element-plus/icons-vue';

const props = defineProps({
  classId: { type: [Number, String], required: true },
  sseConn: { type: Object, default: null },
});

const emit = defineEmits(['back', 'scored']);

// 状态机
const phase = ref('idle'); // idle | publishing | ended
const mode = ref('choice'); // choice | truefalse | pick

// 命题数据
const questionText = ref('');
const correctAnswer = ref('A');
const options = ref(['', '', '', '']);
const durationSeconds = ref(30);
const sessionId = ref(null);

// 发布状态
const publishing = ref(false);
const picking = ref(false);
const pickedStudent = ref(null);

// 倒计时
const remainingSeconds = ref(0);
let countdownTimer = null;

// 结果
const results = ref({
  total: 0,
  submitted: 0,
  options: { A: 0, B: 0, C: 0, D: 0 },
  correctCount: 0,
  accuracy: 0,
});
const revealed = ref(false);

// 已抽过的学生ID
const pickedStudentIds = ref([]);

const getOptionLetter = (i) => String.fromCharCode(65 + i);

const barWidth = (count) => {
  const total = Object.values(results.value.options).reduce((a, b) => a + b, 0);
  return total > 0 ? (count / total) * 100 : 0;
};

// SSE监听 progress（字段名与后端submitLiveQuizAnswer返回对齐）
watch(
  () => props.sseConn,
  (conn) => {
    if (!conn) return;
    const handler = (e) => {
      try {
        const d = JSON.parse(e.data);
        if (d.sessionId === sessionId.value) {
          results.value.submitted = d.totalAnswered || 0;
          // 将后端 { A: 5, B: 3 } 格式的 optionCounts 映射为前端需要的 options
          if (d.optionCounts) {
            const oc = d.optionCounts;
            results.value.options = {
              A: oc.A || 0,
              B: oc.B || 0,
              C: oc.C || 0,
              D: oc.D || 0,
            };
          }
        }
      } catch {
        /* */
      }
    };
    conn.addEventListener('live-quiz:progress', handler);
  },
  { immediate: true }
);

async function doPublish() {
  if (!questionText.value.trim()) return ElMessage.warning('请输入题目');
  if (mode.value === 'choice' && options.value.some((o) => !o.trim()))
    return ElMessage.warning('请填写所有选项');
  if (!correctAnswer.value) return ElMessage.warning('请选择正确答案');

  publishing.value = true;
  try {
    const res = await startLiveQuiz({
      classId: props.classId,
      questionText: questionText.value,
      mode: mode.value,
      options: mode.value === 'choice' ? options.value : [],
      correctAnswer: correctAnswer.value,
      durationSeconds: durationSeconds.value,
    });
    if (res.code === 200) {
      sessionId.value = res.data.sessionId;
      phase.value = 'publishing';
      remainingSeconds.value = durationSeconds.value;
      results.value = {
        total: 0,
        submitted: 0,
        options: { A: 0, B: 0, C: 0, D: 0 },
        correctCount: 0,
        accuracy: 0,
      };
      revealed.value = false;

      clearInterval(countdownTimer);
      countdownTimer = setInterval(() => {
        remainingSeconds.value--;
        if (remainingSeconds.value <= 0) {
          clearInterval(countdownTimer);
          doEnd(true);
        }
      }, 1000);
    } else {
      ElMessage.warning(res.msg || '发布失败');
    }
  } catch {
    ElMessage.error('发布失败');
  } finally {
    publishing.value = false;
  }
}

async function doEnd(reveal) {
  if (!sessionId.value) return;
  clearInterval(countdownTimer);
  try {
    const res = await endLiveQuiz({ sessionId: sessionId.value, revealAnswer: reveal });
    if (res.code === 200) {
      const r = res.data.results || {};
      // 转换后端字段名：totalAnswered→total, optionCounts→options, accuracy小数→百分比
      const oc = r.optionCounts || {};
      results.value = {
        total: r.totalAnswered || 0,
        correctCount: r.correctCount || 0,
        accuracy: r.accuracy ? Math.round(r.accuracy * 100) : 0,
        submitted: r.totalAnswered || 0,
        options: {
          A: oc.A || 0,
          B: oc.B || 0,
          C: oc.C || 0,
          D: oc.D || 0,
        },
        correctAnswer: r.correctAnswer || '',
      };
      revealed.value = reveal;
      phase.value = 'ended';
    }
  } catch {
    ElMessage.error('操作失败');
  }
}

async function doPick() {
  picking.value = true;
  try {
    const res = await pickLiveQuizStudent({
      classId: props.classId,
      excludeStudentIds: pickedStudentIds.value,
    });
    if (res.code === 200) {
      pickedStudent.value = res.data;
      pickedStudentIds.value.push(res.data.studentId);
      emit('scored', { studentId: res.data.studentId });
      setTimeout(() => {
        pickedStudent.value = null;
      }, 3000);
    }
  } catch {
    ElMessage.error('抽取失败');
  } finally {
    picking.value = false;
  }
}

function resetForm() {
  phase.value = 'idle';
  questionText.value = '';
  options.value = ['', '', '', ''];
  correctAnswer.value = 'A';
  sessionId.value = null;
  results.value = {
    total: 0,
    submitted: 0,
    options: { A: 0, B: 0, C: 0, D: 0 },
    correctCount: 0,
    accuracy: 0,
  };
  revealed.value = false;
  pickedStudent.value = null;
  pickedStudentIds.value = [];
}

onBeforeUnmount(() => {
  clearInterval(countdownTimer);
});
</script>

<style scoped lang="scss">
.live-quiz-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.lqp-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.lqp-title {
  font-size: var(--fs-xl);
  font-weight: 700;
}
.lqp-mode-tabs {
  display: flex;
  justify-content: center;
}
.lqp-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.lqp-input {
  :deep(.el-textarea__inner) {
    min-height: 80px;
    font-size: var(--fs-md);
  }
}
.lqp-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.lqp-option-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lqp-opt-letter {
  font-weight: 700;
  color: var(--primary-color);
  min-width: 24px;
}
.lqp-answer-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.lqp-answer-label {
  font-weight: 600;
  white-space: nowrap;
}
.lqp-duration-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.lqp-duration-label {
  font-weight: 600;
  white-space: nowrap;
}
.lqp-duration-unit {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.lqp-publish-btn {
  width: 100%;
  margin-top: 8px;
}

// 纯抽人
.lqp-pick-area {
  text-align: center;
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.lqp-pick-desc {
  color: var(--text-secondary);
  font-size: var(--fs-sm);
}
.lqp-picked {
  font-size: var(--fs-2xl);
  font-weight: 800;
  color: var(--el-color-warning);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 32px;
  background: var(--el-color-warning-light-9);
  border-radius: var(--radius-lg);
}

// 收集中
.lqp-collecting,
.lqp-ended {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.lqp-collect-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--el-color-warning);
}
.lqp-collect-icon {
  animation: spin 1s linear infinite;
}
.lqp-collect-count {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  font-weight: 400;
  margin-left: auto;
}
.lqp-collect-timer {
  font-size: var(--fs-lg);
  font-weight: 700;
  font-family: monospace;
  color: var(--el-color-danger);
}
.lqp-question-display {
  font-size: var(--fs-md);
  padding: 14px;
  background: var(--bg-section);
  border-radius: var(--radius-md);
  line-height: 1.6;
}
.lqp-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.lqp-result-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.lqp-r-label {
  width: 24px;
  font-weight: 700;
  color: var(--primary-color);
  text-align: center;
}
.lqp-r-bar {
  flex: 1;
  height: 24px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.lqp-r-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: var(--radius-sm);
  min-width: 2px;
  transition: width 0.4s ease;
}
.lqp-r-fill.is-correct {
  background: var(--el-color-success);
}
.lqp-r-count {
  width: 36px;
  text-align: right;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.lqp-r-pct {
  width: 36px;
  text-align: right;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}
.lqp-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}
.lqp-actions .el-button {
  flex: 1;
}

// 结果
.lqp-ended-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--el-color-success);
}
.lqp-ended-count {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  font-weight: 400;
  margin-left: auto;
}
.lqp-correct-answer {
  font-size: var(--fs-lg);
  padding: 12px;
  background: var(--el-color-success-light-9);
  border-radius: var(--radius-md);
  text-align: center;
}
.lqp-accuracy {
  text-align: center;
  font-size: var(--fs-md);
  color: var(--text-secondary);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
