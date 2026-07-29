<template>
  <div class="result-view">
    <div class="score-card" :class="scoreCardClass">
      <div v-if="submission.isExemplar" class="exemplar-badge">🌟 优秀范文</div>
      <div class="score-display">
        <template v-if="isStarTask">
          <el-rate
            :model-value="starValue"
            disabled
            show-score
            text-color="#ff9900"
            size="large"
          />
          <span class="score-label">{{ starText }}</span>
        </template>
        <template v-else-if="task.scoreType === 'GRADE_5'">
          <span class="score-value grade">{{ submission.gradeLevel || '-' }}</span>
          <span class="score-label">等级</span>
        </template>
        <template v-else-if="task.scoreType === 'PASS_FAIL'">
          <span class="score-value pass-fail">{{ isPassed ? '通过' : '未通过' }}</span>
          <span class="score-label">{{ isPassed ? '恭喜通过！' : '继续努力' }}</span>
        </template>
        <template v-else>
          <span class="score-value">{{ submission.score != null ? submission.score : '-' }}</span>
          <span class="score-total">/ {{ task.totalScore }}</span>
          <span class="score-label">{{ scorePercent }}%</span>
        </template>
      </div>
      <div v-if="submission.peerScore != null" class="peer-score">
        互评均分: {{ submission.peerScore }}
      </div>
      <div v-if="taskPassingScore">
        <el-tag
          :type="isPassed ? 'success' : 'danger'"
          size="small"
        >
          通过线: {{ taskPassingScore }}
        </el-tag>
      </div>
    </div>

    <div v-if="classStats && classStats.avgScore != null" class="compare-section">
      <div class="compare-title">成绩对比</div>
      <div class="compare-row">
        <span class="compare-label">你的得分</span>
        <span class="compare-val">{{ submission.score || 0 }}分</span>
      </div>
      <div class="compare-row">
        <span class="compare-label">班级平均</span>
        <span class="compare-val">{{ classStats.avgScore }}分</span>
      </div>
      <div class="compare-row">
        <span class="compare-label">最高分</span>
        <span class="compare-val">{{ classStats.maxScore }}分</span>
      </div>
      <div class="compare-row">
        <span class="compare-label">最低分</span>
        <span class="compare-val">{{ classStats.minScore }}分</span>
      </div>
      <div class="compare-bar-wrap">
        <div class="compare-bar-bg">
          <div class="compare-bar-fill" :style="{ width: scorePercent + '%' }" />
          <div
            v-if="classStats.avgScore != null"
            class="compare-bar-avg"
            :style="{ left: (classStats.avgScore / task.totalScore) * 100 + '%' }"
          />
        </div>
        <div class="compare-bar-labels">
          <span>0</span>
          <span v-if="classStats.avgScore != null" class="avg-marker">平均线</span>
          <span>{{ task.totalScore }}</span>
        </div>
      </div>
    </div>

    <div v-if="dimensions.length" class="dim-section">
      <div class="dim-title">评分维度</div>
      <div v-for="d in dimensions" :key="d.name" class="dim-row">
        <span class="dim-name">{{ d.name }}</span>
        <div class="dim-bar-wrap">
          <div class="dim-bar" :style="{ width: dimPercent(d) + '%' }"></div>
        </div>
        <span class="dim-val">{{ d.score }}/{{ d.max || 100 }}</span>
      </div>
      <div v-if="comment" class="dim-comment">💬 {{ comment }}</div>
      <div v-if="explanation" class="dim-explanation">
        <el-collapse>
          <el-collapse-item title="评分理由">
            {{ explanation }}
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>

    <div class="info-section">
      <div class="info-row">
        <span class="label">提交</span><span>{{ fmt(submission.submittedAt) }}</span>
      </div>
      <div v-if="submission.gradedAt" class="info-row">
        <span class="label">评分</span><span>{{ fmt(submission.gradedAt) }}</span>
      </div>
      <div v-if="submission.gradeType" class="info-row">
        <span class="label">方式</span>
        <el-tag size="small">
          {{
            submission.gradeType === 'AUTO'
              ? '自动'
              : submission.gradeType === 'TEACHER'
                ? '教师'
                : '同伴'
          }}
        </el-tag>
      </div>
    </div>

    <div v-if="answers.length" class="answers-section">
      <el-collapse v-model="answersExpanded">
        <el-collapse-item name="answers">
          <template #title>
            <div class="answers-collapse-title">
              <span>📋 答题详情</span>
              <el-tag
                size="small"
                type="info"
                style="margin-left: 8px"
              >
                {{ answers.length }} 题
              </el-tag>
            </div>
          </template>
          <template v-if="answers.length">
            <div
              class="answer-row"
              :class="{ 'ans-correct': currentAnswer.isCorrect === 1, 'ans-wrong': currentAnswer.isCorrect === 0 }"
            >
              <div class="ans-header">
                <span class="ans-idx">第{{ currentAnswerIdx + 1 }}题</span>
                <el-tag size="small">
                  {{ QUESTION_TYPE_LABEL[currentAnswer.questionType] || currentAnswer.questionType }}
                </el-tag>
                <el-tag
                  :type="currentAnswer.isCorrect === 1 ? 'success' : currentAnswer.isCorrect === 0 ? 'danger' : 'info'"
                  size="small"
                >
                  {{ currentAnswer.isCorrect === 1 ? '✓ 正确' : currentAnswer.isCorrect === 0 ? '✗ 错误' : '待评分' }}
                </el-tag>
                <span class="ans-score">{{ currentAnswer.autoScore != null ? currentAnswer.autoScore : currentAnswer.score || 0 }}分</span>
              </div>
              <div class="ans-body">{{ currentAnswer.questionText }}</div>
              <div class="ans-meta">
                <span>你的答案: <b>{{ currentAnswer.studentAnswer || '未作答' }}</b></span>
                <span v-if="currentAnswer.correctAnswer">正确答案: <b class="ans-ca">{{ currentAnswer.correctAnswer }}</b></span>
              </div>
              <div v-if="currentAnswer.explanation" class="ans-explanation">解析：{{ currentAnswer.explanation }}</div>
              <div v-if="currentAnswer.knowledgePointName" class="ans-kp">知识点：{{ currentAnswer.knowledgePointName }}</div>
            </div>
            <div class="ans-nav">
              <el-button size="small" :disabled="currentAnswerIdx === 0" @click="currentAnswerIdx--">上一题</el-button>
              <span class="ans-nav-progress">{{ currentAnswerIdx + 1 }} / {{ answers.length }}</span>
              <el-button size="small" :disabled="currentAnswerIdx >= answers.length - 1" @click="currentAnswerIdx++">下一题</el-button>
            </div>
          </template>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="ai-section">
      <div class="ai-title">📝 学习反思</div>
      <el-input
        v-model="reflection"
        type="textarea"
        :rows="2"
        placeholder="你对这次任务的反思或总结…"
        size="small"
      />
      <el-button
        size="small"
        type="primary"
        text
        :loading="savingReflection"
        style="margin-top: 6px"
        @click="handleSaveReflection"
      >
        保存反思
      </el-button>
    </div>

    <el-alert
      v-if="submission.status === 'RETURNED'"
      title="已退回修改"
      type="warning"
      show-icon
      :closable="false"
      class="mb-12"
    />
    <el-alert
      v-if="submission.status === 'EXEMPTED'"
      title="已豁免"
      type="info"
      show-icon
      :closable="false"
      class="mb-12"
    />

    <div class="result-actions">
      <el-button
        v-if="task.allowResubmit && submission.status !== 'EXEMPTED'"
        type="primary"
        @click="$emit('resubmit')"
      >
        {{ submission.status === 'RETURNED' ? '修改重交' : '重新提交' }}
      </el-button>
      <el-button
        v-if="submission.status === 'GRADED'"
        type="warning"
        @click="appealVisible = true"
      >
        申请复议
      </el-button>
      <el-button
        v-if="submission.includeInPortfolio"
        type="success"
        disabled
      >
        已收录成长档案
      </el-button>
    </div>

    <el-dialog
      v-model="appealVisible"
      title="申请复议"
      width="90%"
      append-to-body
    >
      <el-input
        v-model="appealReason"
        type="textarea"
        :rows="5"
        placeholder="请说明复议理由..."
      />
      <template #footer>
        <el-button @click="appealVisible = false">取消</el-button>
        <el-button type="primary" :loading="appealing" @click="submitAppeal">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { requestReReview } from '@/api/reReview';
import {
  saveReflection as apiSaveReflection,
  loadReflection,
  getSubmissionAnswers,
  getTaskStats,
} from '@/api/task';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';

const props = defineProps({
  task: { type: Object, required: true },
  submission: { type: Object, required: true },
  isMobile: { type: Boolean, default: false },
});
defineEmits(['resubmit']);

const appealVisible = ref(false),
  appealReason = ref(''),
  appealing = ref(false),
  aiSuggest = ref(null);
const answers = ref([]);

const classStats = ref(null);

const currentAnswerIdx = ref(0);

const currentAnswer = computed(() => answers.value[currentAnswerIdx.value] || {});
const loadAnswers = async () => {
  try {
    const res = await getSubmissionAnswers(props.task.id, props.submission.id);
    if (res.code === 200) answers.value = res.data || [];
  } catch {
    /* */
  }
};
const loadClassStats = async () => {
  try {
    const r = await getTaskStats(props.task.id);
    if (r.code === 200) classStats.value = r.data;
  } catch {
    /* */
  }
};
onMounted(() => {
  if (props.submission.status === 'GRADED' || props.submission.status === 'SUBMITTED')
    loadAnswers();
  loadClassStats();
});

const STAR_TYPES = ['MORAL', 'LABOR'];
const isStarTask = computed(() => STAR_TYPES.includes(props.task?.taskType));
const starValue = computed(() => Math.min(5, Math.max(1, Number(props.submission.score) || 0)));
const starText = ['', '需努力', '有进步', '良好', '优秀', '非常棒！'][starValue.value] || '';

const scorePercent = computed(() => {
  if (props.submission.score == null || !props.task.totalScore) return '-';
  return Math.round((Number(props.submission.score) / Number(props.task.totalScore)) * 100);
});
const taskPassingScore = computed(() => {
  if (!props.task.taskConfig) return null;
  try {
    const cfg = JSON.parse(props.task.taskConfig);
    return cfg.passingScore || null;
  } catch {
    return null;
  }
});
const isPassed = computed(() => {
  if (isStarTask.value) return starValue.value >= 3;
  if (props.task.scoreType === 'PASS_FAIL') return props.submission.gradeLevel === 'PASS';
  const ps = taskPassingScore.value;
  if (!ps) return true;
  return Number(props.submission.score) >= Number(ps);
});
const parsedScore = computed(() => {
  try {
    return JSON.parse(props.submission.scoreJson || '{}');
  } catch {
    return {};
  }
});
const dimensions = computed(() => {
  const d = parsedScore.value;
  if (!d || d.direct) return [];
  return Object.entries(d)
    .filter(([k, v]) => k !== 'comment' && v && typeof v === 'object' && v.name)
    .map(([k, v]) => ({
      name: v.name,
      score: v.weighted || v.score || 0,
      max: 100,
      weight: v.weight,
    }));
});
const comment = computed(() => parsedScore.value.comment || '');
const explanation = computed(() => parsedScore.value.explanation || '');
const dimPercent = (d) => Math.min(100, ((d.score || 0) / (d.max || 100)) * 100);
const scoreCardClass = computed(() => {
  if (props.submission.status === 'EXEMPTED') return 'is-exempted';
  return isPassed.value ? 'is-passed' : 'is-failed';
});
const submitAppeal = async () => {
  if (!appealReason.value.trim()) {
    ElMessage.warning('请填写理由');
    return;
  }
  appealing.value = true;
  try {
    const res = await requestReReview({
      submissionId: props.submission.id,
      reason: appealReason.value.trim(),
    });
    if (res.code === 200) {
      ElMessage.success('已提交');
      appealVisible.value = false;
    }
  } finally {
    appealing.value = false;
  }
};
const reflection = ref('');
const savingReflection = ref(false);
const answersExpanded = ref([]);
// 组件挂载时从 API 加载已有反思
onMounted(async () => {
  try {
    const res = await loadReflection(props.task.id);
    if (res.code === 200 && res.data?.reflection) {
      reflection.value = res.data.reflection;
    }
  } catch {
    /* 加载失败忽略 */
  }
});
const handleSaveReflection = async () => {
  if (!reflection.value.trim()) {
    ElMessage.warning('请输入反思内容');
    return;
  }
  savingReflection.value = true;
  try {
    await apiSaveReflection(props.task.id, props.submission.id, reflection.value.trim());
    ElMessage.success('已保存');
  } catch {
    ElMessage.error('保存失败');
  } finally {
    savingReflection.value = false;
  }
};
const fmt = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '-');
</script>

<style scoped>
.result-view {
  max-width: 500px;
  margin: 0 auto;
}
.score-card {
  text-align: center;
  padding: 32px 20px;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  position: relative;
}
.score-card.is-passed {
  background: var(--bg-success-light);
}
.score-card.is-failed {
  background: var(--bg-danger-light);
}
.score-card.is-exempted {
  background: var(--bg-section);
}
.is-passed .score-value {
  color: var(--el-color-success);
}
.is-failed .score-value {
  color: var(--el-color-danger);
}
.score-value {
  font-size: 48px;
  font-weight: 700;
  color: var(--primary-color);
}
.score-value.grade {
  font-size: 56px;
}
.score-value.pass-fail {
  font-size: 32px;
}
.score-total {
  font-size: var(--fs-xl);
  color: var(--text-secondary);
}
.score-label {
  display: block;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-top: 4px;
}
.exemplar-badge {
  position: absolute;
  top: 8px;
  right: 12px;
  padding: 2px 10px;
  border-radius: var(--radius-xs);
  font-size: var(--fs-xs);
  font-weight: 600;
  background: var(--el-color-warning);
  color: var(--bg-card);
}
.peer-score {
  margin-top: 4px;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}

.dim-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 16px;
}
.dim-title {
  font-weight: 600;
  font-size: var(--fs-sm);
  margin-bottom: 10px;
  color: var(--text-primary);
}
.dim-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: var(--fs-sm);
}
.dim-name {
  min-width: 80px;
  color: var(--text-regular);
}
.dim-bar-wrap {
  flex: 1;
  height: 8px;
  background: var(--bg-section);
  border-radius: 4px;
  overflow: hidden;
}
.dim-bar {
  height: 100%;
  background: var(--primary-color);
  border-radius: 4px;
  transition: width var(--transition-base);
}
.dim-val {
  min-width: 60px;
  text-align: right;
  color: var(--primary-color);
  font-weight: 500;
}
.dim-comment {
  margin-top: 8px;
  font-size: var(--fs-sm);
  color: var(--text-secondary);
}

.info-section {
  margin-bottom: 16px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-light);
  font-size: var(--fs-sm);
}
.info-row .label {
  color: var(--text-secondary);
}

.ai-section {
  background: var(--bg-section);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 16px;
}
.ai-title {
  font-size: var(--fs-sm);
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.result-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 16px;
}
.mb-12 {
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .score-card {
    padding: 20px 12px;
  }
  .score-value {
    font-size: 36px;
  }
  .score-value.grade {
    font-size: 42px;
  }
  .result-actions {
    flex-direction: column;
  }
  .dim-name {
    min-width: 60px;
    font-size: var(--fs-xs);
  }
}

.answers-section {
  margin-top: 16px;
}
.answers-collapse-title {
  display: flex;
  align-items: center;
  font-weight: 600;
  font-size: var(--fs-base);
}
.answer-row {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.answer-row.ans-correct {
  border-left: 4px solid var(--el-color-success);
  background: #f6fdf6;
}
.answer-row.ans-wrong {
  border-left: 4px solid var(--el-color-danger);
  background: #fef6f6;
}
.ans-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.ans-idx {
  font-weight: 600;
  font-size: var(--fs-sm);
}
.ans-score {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-left: auto;
}
.ans-body {
  font-size: var(--fs-md);
  line-height: 1.6;
  margin-bottom: 6px;
}
.ans-meta {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  display: flex;
  gap: 16px;
}
.ans-ca {
  color: var(--el-color-success);
}
.ans-explanation {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 4px;
  padding: 6px 8px;
  background: var(--bg-section);
  border-radius: var(--radius-sm);
}
.ans-kp {
  font-size: var(--fs-xs);
  color: var(--el-color-primary);
  margin-top: 2px;
}
.ans-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
}
.ans-nav-progress {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--text-secondary);
}

.compare-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 16px;
}
.compare-title {
  font-weight: 600;
  font-size: var(--fs-sm);
  margin-bottom: 10px;
  color: var(--text-primary);
}
.compare-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: var(--fs-sm);
}
.compare-label {
  color: var(--text-secondary);
}
.compare-val {
  font-weight: 600;
}
.compare-bar-wrap {
  margin-top: 10px;
}
.compare-bar-bg {
  position: relative;
  height: 8px;
  background: var(--bg-section);
  border-radius: 4px;
  overflow: visible;
}
.compare-bar-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: 4px;
  transition: width var(--transition-base);
}
.compare-bar-avg {
  position: absolute;
  top: -4px;
  width: 2px;
  height: 16px;
  background: var(--el-color-danger);
  transform: translateX(-50%);
}
.compare-bar-labels {
  display: flex;
  justify-content: space-between;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-top: 2px;
}
.avg-marker {
  color: var(--el-color-danger);
  font-size: 10px;
}
</style>
