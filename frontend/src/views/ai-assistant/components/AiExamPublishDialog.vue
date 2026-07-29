<template>
  <el-dialog
    :model-value="modelValue"
    title="发布为考试任务"
    width="640px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form :model="examForm" label-width="90px" size="default">
      <!-- 基本信息 -->
      <el-form-item label="试卷标题"><el-input v-model="examForm.title" /></el-form-item>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12">
          <el-form-item label="年级">
            <el-select v-model="examForm.gradeId" clearable style="width: 100%">
              <el-option
                v-for="g in examGrades"
                :key="g.id"
                :label="g.gradeName"
                :value="g.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="满分">
            <el-input-number
              v-model="examForm.totalScore"
              :min="10"
              :max="300"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="班级" required>
        <el-select v-model="examForm.classIds" multiple style="width: 100%">
          <el-option
            v-for="c in examClasses"
            :key="c.id"
            :label="c.className"
            :value="c.id"
          />
        </el-select>
      </el-form-item>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12">
          <el-form-item label="考试时长(分)">
            <el-input-number
              v-model="examForm.durationMinutes"
              :min="10"
              :max="300"
              :step="5"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="及格分">
            <el-input-number
              v-model="examForm.passingScore"
              :min="0"
              :max="300"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="截止时间">
        <el-date-picker
          v-model="examForm.deadline"
          type="datetime"
          style="width: 100%"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>

      <!-- 题型分值 -->
      <el-divider content-position="left">题型分布与分值（每题得分）</el-divider>
      <div
        v-if="examTypeStats.length"
        style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px"
      >
        <el-tag
          v-for="t in examTypeStats"
          :key="t.key"
          type="info"
          effect="plain"
          size="small"
        >
          {{ t.label }}：{{ t.count }}题
          <el-input-number
            v-model="examForm.scorePresets[t.key]"
            :min="1"
            :max="50"
            size="small"
            style="width: 60px; margin-left: 4px"
            controls-position="right"
          />
          分/题
        </el-tag>
      </div>

      <!-- 高级设置（折叠） -->
      <el-collapse v-model="activeCollapse" style="margin-top: 12px">
        <el-collapse-item name="settings">
          <template #title>
            <span style="font-size: var(--fs-sm); color: var(--text-secondary)">考试设置与防作弊（可选）</span>
          </template>
          <el-divider content-position="left" style="margin-top: 0">考试设置</el-divider>
          <el-row :gutter="8">
            <el-col
              :xs="24"
              :sm="8"
            >
              <el-checkbox v-model="examForm.shuffleQuestions">题目乱序</el-checkbox>
            </el-col>
            <el-col
              :xs="24"
              :sm="8"
            >
              <el-checkbox v-model="examForm.shuffleOptions">选项乱序</el-checkbox>
            </el-col>
            <el-col
              :xs="24"
              :sm="8"
            >
              <el-checkbox v-model="examForm.autoWrongbook">收录错题本</el-checkbox>
            </el-col>
          </el-row>
          <el-divider content-position="left">防作弊设置</el-divider>
          <el-row :gutter="8">
            <el-col
              :xs="24"
              :sm="8"
            >
              <el-checkbox v-model="examForm.fullscreenLock">全屏锁定</el-checkbox>
            </el-col>
            <el-col
              :xs="24"
              :sm="8"
            >
              <el-checkbox v-model="examForm.disableContextMenu">禁用右键</el-checkbox>
            </el-col>
            <el-col
              :xs="24"
              :sm="8"
            >
              <el-checkbox v-model="examForm.disableCopyPaste">禁止复制</el-checkbox>
            </el-col>
          </el-row>
          <el-form-item label="切屏警告上限" style="margin-top: 8px">
            <el-input-number
              v-model="examForm.maxWarnings"
              :min="1"
              :max="10"
              size="small"
            />
            <span style="margin-left: 8px; font-size: var(--fs-xs); color: var(--text-secondary)">达到上限自动终止考试</span>
          </el-form-item>
        </el-collapse-item>
      </el-collapse>

      <el-form-item style="margin-top: 12px">
        <span style="font-size: var(--fs-xs); color: var(--text-secondary)">题目将自动审核通过并发布，学生端实时可见</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button
        type="primary"
        :disabled="!examForm.classIds.length"
        @click="doPublish"
      >
        发布({{ pendingExamQids.length }}题)
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { getGrades } from '@/api/settings';
import { getMyClasses } from '@/api/classes';
import { publishQuestionsAsExam } from '@/api/aiOutput';

const TYPE_LABEL_MAP = {
  SINGLE_CHOICE: '单选',
  MULTI_CHOICE: '多选',
  TRUE_FALSE: '判断',
  FILL_IN: '填空',
  ESSAY: '简答',
  SHORT_ANSWER: '简答',
};

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  questions: { type: Array, default: () => [] },
  batchId: { type: String, default: '' },
  title: { type: String, default: '' },
  totalScorePreview: { type: Number, default: 100 },
  scorePresets: { type: Object, default: () => ({}) },
  subjectName: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'published']);

// 高级设置折叠状态
const activeCollapse = ref([]);

const examForm = reactive({
  classIds: [],
  gradeId: null,
  deadline: null,
  title: '',
  totalScore: 100,
  durationMinutes: 60,
  passingScore: 60,
  shuffleQuestions: true,
  shuffleOptions: true,
  autoWrongbook: true,
  fullscreenLock: true,
  disableContextMenu: true,
  disableCopyPaste: false,
  maxWarnings: 3,
  scorePresets: {
    SINGLE_CHOICE: 2,
    MULTI_CHOICE: 3,
    TRUE_FALSE: 1,
    FILL_IN: 2,
    SHORT_ANSWER: 5,
    ESSAY: 10,
  },
});
const examGrades = ref([]);
const examClasses = ref([]);
const pendingExamQids = ref([]);
const examTypeStats = ref([]);

const open = (opts) => {
  const { questions: qs, title: t, totalScorePreview: ts, scorePresets: sp, batchId: bid } = opts;
  const qids = qs.map((q) => q.id);
  pendingExamQids.value = qids;
  examForm.title = t || 'AI组卷';
  examForm.totalScore = ts || 100;
  examForm.classIds = [];
  examForm.gradeId = null;
  examForm.deadline = null;
  if (sp && Object.values(sp).some((v) => v > 0)) {
    Object.keys(examForm.scorePresets).forEach((k) => {
      if (sp[k] != null) examForm.scorePresets[k] = sp[k];
    });
  }
  const stats = {};
  for (const q of qs) {
    const k = q.questionType || 'SINGLE_CHOICE';
    stats[k] = (stats[k] || 0) + 1;
  }
  examTypeStats.value = Object.entries(stats).map(([k, v]) => ({
    key: k,
    label: TYPE_LABEL_MAP[k] || k,
    count: v,
  }));
  Promise.all([getGrades(), getMyClasses()])
    .then(([gRes, cRes]) => {
      if (gRes.code === 200) examGrades.value = gRes.data || [];
      if (cRes.code === 200) examClasses.value = cRes.data || [];
    })
    .catch(() => {});
  emit('update:modelValue', true);
};

const doPublish = async () => {
  if (!examForm.classIds.length) return ElMessage.warning('请选择班级');
  try {
    const taskConfig = {
      durationMinutes: examForm.durationMinutes,
      passingScore: examForm.passingScore,
      shuffleQuestions: examForm.shuffleQuestions,
      shuffleOptions: examForm.shuffleOptions,
      fullscreenLock: examForm.fullscreenLock,
      disableContextMenu: examForm.disableContextMenu,
      disableCopyPaste: examForm.disableCopyPaste,
      maxWarnings: examForm.maxWarnings,
    };
    const res = await publishQuestionsAsExam({
      questionIds: pendingExamQids.value,
      title: examForm.title,
      subject: props.subjectName,
      totalScore: examForm.totalScore,
      classIds: examForm.classIds,
      gradeId: examForm.gradeId,
      deadline: examForm.deadline,
      taskConfig: JSON.stringify(taskConfig),
      scorePresets: examForm.scorePresets,
      autoWrongbook: examForm.autoWrongbook,
    });
    if (res.code === 200) {
      const data = res.data || {};
      let msg = `考试任务已发布: ${data.title || ''}`;
      if (data.filteredCount > 0) {
        msg += ` (${data.filteredCount} 道已删除/驳回题目自动排除)`;
        ElMessage.warning(msg);
      } else {
        ElMessage.success(msg);
      }
      emit('update:modelValue', false);
      emit('published');
    }
  } catch {
    ElMessage.error('发布失败');
  }
};

defineExpose({ open });
</script>
