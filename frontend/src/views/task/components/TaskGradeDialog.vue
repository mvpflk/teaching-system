<template>
  <el-dialog
    v-model="visible"
    title="评分"
    width="700px"
    append-to-body
    @close="handleClose"
  >
    <div class="grade-body">
      <div class="grade-info">
        <div class="grade-student">学生: {{ submission.studentName || submission.studentId }}</div>
        <div v-if="submission.submittedAt" class="grade-time">
          提交: {{ formatTime(submission.submittedAt) }}
        </div>
      </div>

      <!-- 学生作答内容（非考试） -->
      <div v-if="submission.content || submission.attachments" class="answer-block">
        <div class="answer-label">学生作答内容:</div>
        <div v-if="submission.content" class="answer-content">{{ submission.content }}</div>
        <div v-if="submission.attachments" class="answer-attachments">
          <span class="answer-label">附件（{{ parseAttachments(submission.attachments).length }}个）:</span>
          <FilePreview
            v-for="(url, i) in parseAttachments(submission.attachments)"
            :key="i"
            :src="url"
            :filename="extractFilename(url)"
            class="attachment-preview"
          />
        </div>
      </div>

      <!-- 题目作答详情（考试） -->
      <div v-if="answers.length" class="answers-section">
        <div class="answers-title">📝 题目作答详情</div>
        <div v-for="(a, i) in answers" :key="a.id" class="answer-detail">
          <div class="ad-header">
            <span class="ad-num">{{ i + 1 }}.</span>
            <el-tag size="small" type="info">
              {{
                QUESTION_TYPE_LABEL[a.questionType] || a.questionType
              }}
            </el-tag>
            <span class="ad-score">({{ a.score }}分)</span>
            <el-tag v-if="a.isCorrect === 1" size="small" type="success">✓ 正确</el-tag>
            <el-tag v-else-if="a.isCorrect === 0" size="small" type="danger">✗ 错误</el-tag>
            <el-tag v-else size="small" type="warning">待评分</el-tag>
          </div>
          <QuestionRenderer
            :question="a"
            mode="display"
            :show-answer="true"
            :highlight-correct="true"
            :show-meta="false"
          />
          <!-- 主观题单独打分 -->
          <div v-if="a.isCorrect === 2" class="ad-grade-row">
            <span class="ad-label">评分：</span>
            <el-input-number
              v-model="subjectiveScores[a.id]"
              :min="0"
              :max="a.score"
              :precision="1"
              size="small"
              style="width: 120px"
              @change="calcTotal"
            />
            <span class="score-hint">满分 {{ a.score }}</span>
          </div>
          <!-- 客观题显示自动得分 -->
          <div v-else class="ad-grade-row">
            <span class="ad-label">得分：</span>
            <span :class="a.autoScore != null && a.autoScore > 0 ? 'ad-correct' : 'ad-student'">{{
              a.autoScore != null ? a.autoScore : 0
            }}</span>
            <span class="score-hint">/ {{ a.score }}</span>
          </div>
        </div>
      </div>

      <el-divider />

      <el-form label-position="top">
        <!-- 德育/劳动作业：星级评价 -->
        <el-form-item v-if="isStarType" label="评分">
          <el-rate
            v-model="starScore"
            :max="5"
            show-score
            show-text
            :texts="starTexts"
            size="large"
          />
        </el-form-item>
        <el-form-item v-else label="总分">
          <span class="total-score">{{ computedTotal }}</span>
          <span class="score-hint">
            / {{ task.totalScore || 100 }}（客观题自动得分 + 主观题评分求和）</span>
        </el-form-item>
        <el-form-item v-if="!isStarType && task.scoreType === 'GRADE_5'" label="等级">
          <el-select v-model="form.gradeLevel" style="width: 200px">
            <el-option
              v-for="g in ['A', 'A-', 'B+', 'B', 'C']"
              :key="g"
              :value="g"
              :label="g"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="task.scoreType === 'PASS_FAIL'" label="评定">
          <el-radio-group v-model="form.isPassed">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评语">
          <el-input
            v-model="form.comment"
            type="textarea"
            :rows="2"
            placeholder="可选评语"
          />
        </el-form-item>
        <el-form-item v-if="isTeacher" label="推荐到展示墙">
          <el-switch v-model="form.recommend" active-text="推荐" />
          <span class="score-hint">将此提交推荐到优秀作品展示墙</span>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="footer-wrap">
        <div class="footer-nav">
          <el-button
            size="small"
            :disabled="!hasPrev"
            @click="$emit('navigate', 'prev')"
          >
            上一位
          </el-button>
          <span class="nav-hint">{{ navText }}</span>
          <el-button
            size="small"
            :disabled="!hasNext"
            @click="$emit('navigate', 'next')"
          >
            下一位
          </el-button>
        </div>
        <div class="footer-actions">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确认评分</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue';
import dayjs from 'dayjs';
import { useUserStore } from '@/stores/user';
import { getSubmissionAnswers, regradeSubmission, gradeItems } from '@/api/task';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import FilePreview from '@/components/renderers/FilePreview.vue';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';
import 'katex/dist/katex.min.css';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  task: { type: Object, default: () => ({}) },
  submission: { type: Object, default: () => ({}) },
  hasPrev: { type: Boolean, default: false },
  hasNext: { type: Boolean, default: false },
  currentIndex: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
});
const emit = defineEmits(['update:modelValue', 'saved', 'navigate']);

const visible = ref(false);
const submitting = ref(false);
const userStore = useUserStore();
const isTeacher = computed(() => userStore.isTeacher);
const answers = ref([]);

const form = reactive({ gradeLevel: '', isPassed: null, comment: '', recommend: false });
const subjectiveScores = reactive({});
const navText = ref('');
const starScore = ref(0);
const STAR_TYPES = ['MORAL', 'LABOR'];
const isStarType = computed(() => STAR_TYPES.includes(props.task?.taskType));
const starTexts = ['很差', '较差', '一般', '良好', '优秀'];

const computedTotal = computed(() => {
  if (isStarType.value) return starScore.value;
  let total = 0;
  answers.value.forEach((a) => {
    if (a.isCorrect === 2) total += Number(subjectiveScores[a.id] || 0);
    else total += Number(a.autoScore || 0);
  });
  return total;
});

const loadAnswers = async (taskId, submissionId) => {
  if (!taskId || !submissionId) return;
  try {
    const res = await getSubmissionAnswers(taskId, submissionId);
    if (res.code === 200) answers.value = res.data || [];
    else answers.value = [];
  } catch {
    answers.value = [];
  }
};

watch(
  () => props.modelValue,
  (v) => {
    visible.value = v;
    if (v) {
      form.gradeLevel = props.submission.gradeLevel || '';
      form.isPassed = props.submission.gradeLevel === 'PASS';
      form.comment = '';
      form.recommend = false;
      starScore.value = props.submission.score || 0;
      navText.value = props.totalCount > 0 ? `${props.currentIndex + 1} / ${props.totalCount}` : '';
      loadAnswers(props.task.id, props.submission.id);
    }
  }
);
watch(visible, (v) => emit('update:modelValue', v));

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');
const parseAttachments = (val) => {
  if (!val) return [];
  if (Array.isArray(val)) return val;
  try {
    const parsed = JSON.parse(val);
    return Array.isArray(parsed) ? parsed : [val];
  } catch {
    return [val];
  }
};
const extractFilename = (url) => {
  const p = url.split('/');
  return p[p.length - 1] || '附件';
};

const handleSubmit = async () => {
  if (submitting.value) return;
  submitting.value = true;
  try {
    let gradedByItems = false;
    // 如果之前已评分，先回退
    if (props.submission.status === 'GRADED') {
      await regradeSubmission(props.task.id, props.submission.id);
    }
    // 如果有主观题，使用逐题评分接口
    const hasSubjective = answers.value.some((a) => a.isCorrect === 2);
    if (hasSubjective) {
      await gradeItems(props.task.id, props.submission.id, { ...subjectiveScores });
      gradedByItems = true;
    }
    // 发送评语和推荐（含 gradedByItems 标志，告知父组件是否已通过 grade-items 评分）
    const scoreVal = computedTotal.value;
    const data = {
      comment: form.comment,
      recommend: form.recommend,
      score: scoreVal,
      gradedByItems,
    };
    if (isStarType.value) {
      data.score = starScore.value;
    } else if (props.task.scoreType === 'GRADE_5') data.gradeLevel = form.gradeLevel;
    else if (props.task.scoreType === 'PASS_FAIL')
      data.gradeLevel = form.isPassed ? 'PASS' : 'FAIL';
    else data.score = scoreVal;
    emit('saved', data);
  } finally {
    submitting.value = false;
  }
};

const handleClose = () => {
  form.gradeLevel = '';
  form.isPassed = null;
  form.comment = '';
  form.recommend = false;
  answers.value = [];
  Object.keys(subjectiveScores).forEach((k) => delete subjectiveScores[k]);
};
</script>

<style scoped>
.grade-body {
  max-height: 65vh;
  overflow-y: auto;
}
.grade-info {
  font-size: var(--fs-sm);
  color: var(--text-regular);
  margin-bottom: 8px;
}
.grade-student {
  font-weight: 600;
  color: var(--text-primary);
}
.grade-time {
  margin-top: 4px;
}
.score-hint {
  margin-left: 8px;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.answer-block {
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 8px;
}
.answer-label {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-bottom: 6px;
  font-weight: 500;
}
.answer-content {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
.answer-attachments {
  margin-top: 8px;
}
.att-link {
  display: inline-block;
  margin-right: 10px;
  font-size: var(--fs-xs);
  color: var(--primary-color);
}
.attachment-preview {
  margin-bottom: 8px;
}
.answers-section {
  margin: 12px 0;
}
.answers-title {
  font-size: var(--fs-sm);
  font-weight: 600;
  margin-bottom: 10px;
}
.answer-detail {
  padding: 10px;
  margin-bottom: 8px;
  background: var(--bg-section);
  border-radius: var(--radius-sm);
}
.ad-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.ad-num {
  font-weight: 600;
  color: var(--primary-color);
}
.ad-score {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
}
.ad-question {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  margin-bottom: 6px;
  line-height: 1.6;
}
.ad-question :deep(img) {
  display: block;
  margin: 8px 0;
}
.ad-options {
  margin-bottom: 6px;
}
.ad-opt {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  padding: 1px 0;
}
.ad-answer-row {
  font-size: var(--fs-xs);
  margin-bottom: 2px;
}
.ad-label {
  color: var(--text-secondary);
}
.ad-student {
  color: var(--primary-color);
  font-weight: 500;
}
.ad-correct {
  color: var(--el-color-success);
  font-weight: 500;
}
.ad-grade-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.total-score {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-color);
}
.footer-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.footer-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-hint {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  min-width: 40px;
  text-align: center;
}
.footer-actions {
  display: flex;
  gap: 8px;
}
@media (max-width: 768px) {
  .grade-body {
    max-height: calc(100dvh - 140px);
  }
}
</style>
