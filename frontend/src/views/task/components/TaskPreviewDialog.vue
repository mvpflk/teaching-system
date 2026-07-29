<template>
  <el-dialog
    v-model="visible"
    title="预览作答"
    width="700px"
    append-to-body
    @close="handleClose"
  >
    <div class="preview-body">
      <div class="preview-info">
        <span class="preview-student">{{ submission.studentName || submission.studentId }}</span>
        <span
          v-if="submission.submittedAt"
          class="preview-time"
        >提交: {{ formatTime(submission.submittedAt) }}</span>
        <el-tag
          v-if="submission.status === 'GRADED'"
          size="small"
          type="success"
          style="margin-left: 8px"
        >
          已批阅
        </el-tag>
      </div>

      <!-- 已评分：显示得分和评语 -->
      <div v-if="submission.status === 'GRADED'" class="preview-score-bar">
        <span v-if="submission.score != null" class="ps-score">得分: {{ submission.score }}</span>
        <span v-if="submission.gradeLevel" class="ps-level">等级: {{ submission.gradeLevel }}</span>
        <span
          v-if="submission.gradingMessage"
          class="ps-comment"
        >评语: {{ submission.gradingMessage }}</span>
      </div>

      <!-- 学生作答内容 -->
      <div v-if="submission.content" class="answer-block">
        <div class="answer-label">作答内容:</div>
        <div class="answer-content">{{ submission.content }}</div>
      </div>

      <!-- 附件 -->
      <div v-if="submission.attachments" class="answer-attachments">
        <span class="answer-label">附件 ({{ parseAttachments(submission.attachments).length }}个):</span>
        <FilePreview
          v-for="(url, i) in parseAttachments(submission.attachments)"
          :key="i"
          :src="url"
          :filename="extractFilename(url)"
          class="attachment-preview"
        />
      </div>

      <!-- 题目作答详情 -->
      <div v-if="answers.length" class="answers-section">
        <div class="answers-title">📝 题目作答详情 ({{ answers.length }}题)</div>
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
          <div class="ad-answer-row">
            <span class="ad-label">学生答案：</span>
            <span class="ad-student">{{ a.studentAnswer || '（未作答）' }}</span>
          </div>
          <div class="ad-grade-row">
            <span class="ad-label">得分：</span>
            <span>{{
              a.autoScore != null ? a.autoScore : a.teacherScore != null ? a.teacherScore : '-'
            }}
              / {{ a.score }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="openInWorkbench">
        在工作台打开 <el-icon style="margin-left: 4px"><ArrowRight /></el-icon>
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import dayjs from 'dayjs';
import { getSubmissionAnswers } from '@/api/task';
import { QUESTION_TYPE_LABEL } from '@/constants/questionTypes';
import FilePreview from '@/components/renderers/FilePreview.vue';
import { ArrowRight } from '@element-plus/icons-vue';
import QuestionRenderer from '@/components/question/QuestionRenderer.vue';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  task: { type: Object, default: () => ({}) },
  submission: { type: Object, default: () => ({}) },
});

const emit = defineEmits(['update:modelValue', 'openWorkbench']);

const visible = ref(false);
const answers = ref([]);

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
    if (v) loadAnswers(props.task.id, props.submission.id);
  }
);
watch(visible, (v) => emit('update:modelValue', v));

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-');
const parseAttachments = (val) => {
  if (!val) return [];
  if (Array.isArray(val)) return val;
  try {
    const p = JSON.parse(val);
    return Array.isArray(p) ? p : [val];
  } catch {
    return [val];
  }
};
const extractFilename = (u) => {
  const p = u.split('/');
  return p[p.length - 1] || '附件';
};

const openInWorkbench = () => {
  visible.value = false;
  emit('openWorkbench', props.submission);
};

const handleClose = () => {
  answers.value = [];
};
</script>

<style scoped>
.preview-body {
  max-height: 65vh;
  overflow-y: auto;
}
.preview-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-md);
  margin-bottom: 12px;
}
.preview-student {
  font-weight: 600;
  color: var(--text-primary);
}
.preview-time {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-left: auto;
}
.preview-score-bar {
  display: flex;
  gap: 16px;
  padding: 10px 14px;
  background: var(--bg-section);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  font-size: var(--fs-sm);
}
.ps-score {
  font-weight: 700;
  color: var(--primary-color);
}
.ps-level,
.ps-comment {
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
  font-size: var(--fs-xs);
}
@media (max-width: 768px) {
  .preview-body {
    max-height: calc(100dvh - 140px);
  }
}
</style>
