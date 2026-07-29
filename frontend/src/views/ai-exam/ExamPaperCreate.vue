<template>
  <div class="epc-page">
    <div class="epc-header">
      <el-button
        text
        @click="$router.back()"
      >
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ isTraining ? '创建专题训练' : 'AI 仿真组卷' }}</h2>
    </div>

    <!-- 考试模式：三步向导 -->
    <template v-if="!isTraining">
      <el-steps
        :active="step"
        align-center
        finish-status="success"
        style="margin-bottom: 28px"
      >
        <el-step title="选择知识范围" />
        <el-step title="配置试卷结构" />
        <el-step title="生成与发布" />
      </el-steps>

      <!-- Step 1: 选择学科 + 知识范围 -->
      <el-card v-show="step === 0" shadow="never" class="epc-card">
        <template #header><span class="epc-card-title">选择学科与知识范围</span></template>
        <el-form :model="examForm" label-width="100px" size="default">
          <el-form-item label="学科" required>
            <el-select v-model="examForm.subject" style="width: 300px" @change="onSubjectChange">
              <el-option
                v-for="s in availableSubjects"
                :key="s"
                :label="s"
                :value="s"
              />
            </el-select>
            <span
              v-if="examForm.subject"
              style="margin-left: 8px; font-size: var(--fs-xs); color: #999"
            >
              可用题型：{{ availableTypes.map((t) => t.label).join('、') }}
            </span>
          </el-form-item>
          <el-form-item label="考纲范围" required>
            <div style="width:100%">
              <el-input
                v-model="examForm.outlineScope"
                type="textarea"
                :rows="2"
                placeholder="如：2025年对口升学考试大纲·{{ examForm.subject || '语文' }}·全部考点"
              />
              <div
                v-if="outlineScopeStatus === 'ok'"
                class="epc-syllabus-status epc-syllabus-ok"
              >✅ 考纲范围已填写，将注入 AI 命题参考</div>
              <div
                v-else-if="outlineScopeStatus === 'short'"
                class="epc-syllabus-status epc-syllabus-warn"
              >⚠️ 至少填写 5 个字描述考纲范围，AI 命题更精确</div>
            </div>
          </el-form-item>
          <el-form-item label="知识范围">
            <CategoryCascade ref="cascadeRef" :multiple="true" @change="onCategoryChange" />
          </el-form-item>
          <el-form-item label="额外要求">
            <el-input
              v-model="examForm.extra"
              type="textarea"
              :rows="2"
              :placeholder="extraPlaceholder"
            />
          </el-form-item>
        </el-form>
        <div style="text-align: right; margin-top: 12px">
          <el-button
            type="primary"
            :disabled="!canGoToStep2"
            @click="step = 1"
          >
            下一步 →
          </el-button>
        </div>
      </el-card>

      <!-- Step 2: 试卷结构配置 -->
      <el-card v-show="step === 1" shadow="never" class="epc-card">
        <template #header><span class="epc-card-title">配置试卷结构</span></template>
        <el-form label-width="110px" size="default">
          <el-form-item label="试卷标题">
            <el-input
              v-model="examForm.title"
              placeholder="如：2025-2026学年对口升学模拟卷"
              style="width: 400px"
            />
          </el-form-item>
          <el-form-item>
            <template #label>
              <el-tooltip content="对口升学考试满分通常为100-150分，系统会根据学科自动设置" placement="top">
                <span>满分 <el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-input-number
              v-model="examForm.totalScore"
              :min="20"
              :max="300"
              :step="10"
            />
          </el-form-item>
          <el-form-item>
            <template #label>
              <el-tooltip content="考试时长建议60-150分钟，视试卷难度和题量而定" placement="top">
                <span>考试时长(分) <el-icon><QuestionFilled /></el-icon></span>
              </el-tooltip>
            </template>
            <el-input-number
              v-model="examForm.duration"
              :min="10"
              :max="240"
              :step="5"
            />
          </el-form-item>
        </el-form>
        <el-divider content-position="left">题型与数量配置</el-divider>
        <div style="display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 16px">
          <div v-for="t in availableTypes" :key="t.key" class="epc-type-item">
            <span class="epc-type-label">{{ t.label }}</span>
            <el-input-number
              v-model="typeCounts[t.key]"
              :min="0"
              :max="30"
              size="small"
              style="width: 70px"
              controls-position="right"
            />
            题
            <span style="margin: 0 4px; color: #ccc">|</span>
            <el-input-number
              v-model="scorePresets[t.key]"
              :min="1"
              :max="60"
              size="small"
              style="width: 65px"
              controls-position="right"
            />
            分
          </div>
        </div>
        <div style="color: #888; font-size: var(--fs-xs); margin-bottom: 8px">
          共计 {{ totalCount }} 题，总分 {{ totalScore }} 分
          <el-button text size="small" style="margin-left: 8px" @click="resetToDefaultCounts">
            按真题配置
          </el-button>
        </div>
        <el-alert
          v-if="tooManyQuestions"
          type="warning"
          :closable="false"
          show-icon
          :title="`题目数量超过上限（最多${maxQuestions}题），请减少题数后再生成`"
          style="margin-bottom: 12px"
        />
        <el-divider content-position="left">难度分布</el-divider>
        <div style="display: flex; gap: 24px; align-items: center; flex-wrap: wrap">
          <div
            v-for="lv in ['EASY', 'MEDIUM', 'HARD']"
            :key="lv"
            style="display: flex; align-items: center; gap: 6px"
          >
            <span style="font-size: var(--fs-sm); min-width: 28px">{{
              { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[lv]
            }}</span>
            <el-slider
              :model-value="difficultyRatio[lv]"
              :min="0"
              :max="100"
              style="width: 120px"
              show-input
              size="small"
              @change="(val) => { difficultyRatio[lv] = val; adjustDifficulty(lv); }"
            />
            <span style="font-size: var(--fs-xs); color: #999">%</span>
          </div>
        </div>
        <div style="font-size: var(--fs-xs); margin-top: 8px">
          <span :style="{ color: difficultyTotal === 100 ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">
            合计 {{ difficultyTotal }}%
          </span>
          <span v-if="difficultyTotal !== 100" style="color: var(--el-color-danger); margin-left: 8px">
            （需等于100%）
          </span>
          <el-button text size="small" style="margin-left: 12px" @click="difficultyRatio.EASY = 30; difficultyRatio.MEDIUM = 50; difficultyRatio.HARD = 20;">
            恢复默认
          </el-button>
        </div>
        <div style="text-align: right; margin-top: 16px">
          <el-button @click="step = 0">← 上一步</el-button>
          <el-button
            type="primary"
            :disabled="!canGoToStep3"
            @click="step = 2"
          >
            下一步 →
          </el-button>
        </div>
      </el-card>

      <!-- Step 3: 生成与发布 -->
      <el-card v-show="step === 2" shadow="never" class="epc-card">
        <template #header><span class="epc-card-title">确认并生成试卷</span></template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="学科">{{ examForm.subject }}</el-descriptions-item>
          <el-descriptions-item label="满分">{{ examForm.totalScore }} 分</el-descriptions-item>
          <el-descriptions-item label="总题数">{{ totalCount }} 题</el-descriptions-item>
          <el-descriptions-item label="时长">{{ examForm.duration }} 分钟</el-descriptions-item>
          <el-descriptions-item label="题型分布" :span="2">
            <el-tag
              v-for="t in availableTypes"
              v-show="typeCounts[t.key]"
              :key="t.key"
              size="small"
              style="margin-right: 4px"
            >
              {{ t.label }} ×{{ typeCounts[t.key] }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="genError" class="epc-error">
          <el-icon><WarningFilled /></el-icon> {{ genError }}
        </div>
        <GenerationResult
          v-if="genResult"
          :result="genResult"
          @publish-as-exam="onPublishAsExam"
          @regenerate="doGenerate"
          @edit-questions="onEditExamQuestions"
        />
        <div style="text-align: right; margin-top: 16px">
          <el-button @click="step = 1">← 上一步</el-button>
          <el-button
            type="primary"
            :loading="generating"
            :disabled="!canGenerate"
            @click="doGenerate"
          >
            {{ generating ? 'AI 正在组卷...' : '开始生成' }}
          </el-button>
        </div>
        <div v-if="generating && !genResult" class="epc-progress">
          <div class="epc-progress-spinner" />
          <span>{{ progressMsg }}</span>
          <div style="margin-top:12px">
            <el-button size="small" @click="cancelGeneration">取消生成</el-button>
          </div>
          <el-alert v-if="sseDegraded" type="warning" :closable="false" show-icon style="margin-top:12px">
            <template #title>连接不稳定，已切换到备用模式，生成可能稍慢</template>
          </el-alert>
        </div>
      </el-card>
    </template>

    <!-- 训练模式：单页表单 -->
    <el-card v-else shadow="never" class="epc-card">
      <template #header><span class="epc-card-title">专题训练配置</span></template>
      <el-form :model="trainForm" label-width="100px" size="default">
        <el-form-item label="学科" required>
          <el-select v-model="trainForm.subject" style="width: 300px" @change="onSubjectChange">
            <el-option
              v-for="s in availableSubjects"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="训练标题">
          <el-input
            v-model="trainForm.title"
            :placeholder="trainTitlePlaceholder"
            style="width: 400px"
          />
        </el-form-item>
        <el-form-item label="知识范围">
          <CategoryCascade ref="trainCascadeRef" @change="onTrainCategoryChange" />
        </el-form-item>
        <el-form-item label="额外要求">
          <el-input
            v-model="trainForm.extra"
            type="textarea"
            :rows="2"
            :placeholder="extraPlaceholder"
          />
        </el-form-item>
      </el-form>
      <el-divider content-position="left">题型与数量</el-divider>
      <div style="display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 16px">
        <div v-for="t in availableTypes" :key="t.key" class="epc-type-item">
          <span class="epc-type-label">{{ t.label }}</span>
          <el-input-number
            v-model="typeCounts[t.key]"
            :min="0"
            :max="30"
            size="small"
            style="width: 70px"
            controls-position="right"
          />
          题
          <span style="margin: 0 4px; color: #ccc">|</span>
          <el-input-number
            v-model="scorePresets[t.key]"
            :min="1"
            :max="60"
            size="small"
            style="width: 65px"
            controls-position="right"
          />
          分
        </div>
      </div>
      <div style="color: #888; font-size: var(--fs-xs); margin-bottom: 8px">
        共计 {{ totalCount }} 题，总分 {{ totalScore }} 分
      </div>
      <el-alert
        v-if="tooManyQuestions"
        type="warning"
        :closable="false"
        show-icon
        :title="`题目数量超过上限（最多${maxQuestions}题），请减少题数后再生成`"
        style="margin-bottom: 12px"
      />
      <div v-if="genError" class="epc-error">
        <el-icon><WarningFilled /></el-icon> {{ genError }}
      </div>
      <GenerationResult
        v-if="genResult"
        :result="genResult"
        @publish-as-exam="onPublishAsExam"
        @regenerate="doGenerate"
        @edit-questions="onEditExamQuestions"
      />
      <div style="text-align: right; margin-top: 12px">
        <el-button
          type="primary"
          :loading="generating"
          :disabled="!trainForm.subject || totalCount <= 0"
          @click="doGenerate"
        >
          {{ generating ? 'AI 正在出题...' : '开始生成' }}
        </el-button>
      </div>
      <div v-if="generating && !genResult" class="epc-progress">
        <div class="epc-progress-spinner" />
        <span>{{ progressMsg }}</span>
        <div style="margin-top:12px">
          <el-button size="small" @click="cancelGeneration">取消生成</el-button>
        </div>
        <el-alert v-if="sseDegraded" type="warning" :closable="false" show-icon style="margin-top:12px">
          <template #title>连接不稳定，已切换到备用模式，生成可能稍慢</template>
        </el-alert>
      </div>
    </el-card>

    <!-- 题目编辑弹窗 -->
    <QuestionEditor
      v-if="questionEditorVisible"
      v-model="questionEditorVisible"
      :batch-id="editingBatchId"
      :initial-questions="editingQuestions"
      @closed="questionEditorVisible = false"
    />

    <!-- 发布考试弹窗 -->
    <el-dialog v-model="examDialogVisible" title="发布为考试任务" width="600px">
      <el-form :model="publishForm" label-width="100px">
        <el-form-item label="试卷标题"><el-input v-model="publishForm.title" /></el-form-item>
        <el-form-item label="班级" required>
          <el-select
            v-model="publishForm.classIds"
            multiple
            style="width: 100%"
            placeholder="选择班级"
          >
            <el-option
              v-for="c in classes"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="考试时长(分)">
              <el-input-number
                v-model="publishForm.durationMinutes"
                :min="10"
                :max="300"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="满分">
              <el-input-number
                v-model="publishForm.totalScore"
                :min="10"
                :max="300"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="publishForm.deadline"
            type="datetime"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <div style="padding: 0 20px 12px; font-size: var(--fs-xs); color: var(--el-color-info)">
        共发布 <strong>{{ genResult?.questions?.length || 0 }}</strong> 道题目到
        <strong>{{ publishForm.classIds.length }}</strong> 个班级
      </div>
      <template #footer>
        <el-button @click="examDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!publishForm.classIds.length"
          @click="doPublishExam"
        >
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { ArrowLeft, WarningFilled, QuestionFilled } from '@element-plus/icons-vue';
import CategoryCascade from '@/components/ai/CategoryCascade.vue';
import GenerationResult from '@/components/ai/GenerationResult.vue';
import QuestionEditor from '@/components/ai/QuestionEditor.vue';
import { trackEvent } from '@/utils/trackEvent';
import { submitExamPaper } from '@/api/simulation';
import { publishQuestionsAsExam } from '@/api/aiOutput';
import { getMyClasses } from '@/api/classes';
import { useExamPaper } from './composables/useExamPaper.js';
import { createEventSource } from '@/utils/sseTicket';
import { getAiOutputResult } from '@/api/aiOutput';
import { useUserStore } from '@/stores/user';
import { getMySubjects } from '@/api/settings';

const userStore = useUserStore();

// 从系统字典/教师任教学科动态加载（不硬编码）
const availableSubjects = ref([]);
async function loadAvailableSubjects() {
  try {
    const res = await getMySubjects();
    if (res.code === 200 && res.data) {
      availableSubjects.value = (res.data || []).map((s) => s.subjectName).filter(Boolean);
    }
  } catch {
    /* 加载失败时回退到教师任教学科 */
  }
  // 兜底：如果 API 返回空，从 userStore 取
  if (!availableSubjects.value.length) {
    const raw = userStore.teachingSubjects || [];
    availableSubjects.value = [
      ...new Set(
        raw
          .flatMap((s) => (s || '').split(/[,，、]/))
          .map((s) => s.trim())
          .filter(Boolean)
      ),
    ];
  }
}
loadAvailableSubjects();

const examForm = reactive({
  subject: '',
  title: '',
  outlineScope: '',
  extra: '',
  totalScore: 100,
  duration: 90,
});
const trainForm = reactive({ subject: '', title: '', topic: '', extra: '' });
const cascade = reactive({ subjectId: null, chapterId: null, taskId: null, kpId: null });
// 训练模式的级联选择（独立于仿真模式）
const trainCascade = reactive({ subjectId: null, chapterId: null, taskId: null, kpId: null });

// 动态学科：直接从 route.query 获取模式，避免 TDZ（isTraining 尚未定义）
const _route = useRoute();
const router = useRouter();
const currentSubject = computed(() =>
  _route.query.mode === 'training' ? trainForm.subject : examForm.subject
);

const { isTraining, availableTypes, defaultTypeCounts, extraPlaceholder } =
  useExamPaper(currentSubject);

const typeCounts = reactive({});
const scorePresets = reactive({});

// localStorage 配置记忆
const STORAGE_KEY = 'examPaperConfig';
const loadSavedConfig = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
    if (saved.totalScore) examForm.totalScore = saved.totalScore;
    if (saved.duration) examForm.duration = saved.duration;
  } catch {}
};
const saveConfig = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      totalScore: examForm.totalScore,
      duration: examForm.duration,
    }));
  } catch {}
};
loadSavedConfig();

const difficultyRatio = reactive({ EASY: 30, MEDIUM: 50, HARD: 20 });

// 难度比例联动：调整一个时自动调整其他两个，总和锁定100%
const adjustDifficulty = (changedLevel) => {
  const newVal = difficultyRatio[changedLevel];
  const otherLevels = ['EASY', 'MEDIUM', 'HARD'].filter((l) => l !== changedLevel);
  const remaining = 100 - newVal;
  const otherTotal = otherLevels.reduce((sum, l) => sum + difficultyRatio[l], 0);
  if (otherTotal === 0) {
    // 两个都是0，平分剩余
    otherLevels.forEach((l) => { difficultyRatio[l] = Math.round(remaining / 2); });
  } else {
    // 按比例分配
    otherLevels.forEach((l) => {
      difficultyRatio[l] = Math.round((difficultyRatio[l] / otherTotal) * remaining);
    });
  }
  // 修正舍入误差
  const total = difficultyRatio.EASY + difficultyRatio.MEDIUM + difficultyRatio.HARD;
  if (total !== 100) {
    const diff = 100 - total;
    const adjustTarget = otherLevels[0];
    difficultyRatio[adjustTarget] = Math.max(0, difficultyRatio[adjustTarget] + diff);
  }
};

// 难度比例总和
const difficultyTotal = computed(() => difficultyRatio.EASY + difficultyRatio.MEDIUM + difficultyRatio.HARD);

const bareSubject = (s) => (s || '').replace(/\[.*?\]/g, '').trim();

watch(
  availableTypes,
  (types) => {
    const bs = bareSubject(examForm.subject);
    for (const t of types) {
      // 数量：按学科真题规格
      if (!(t.key in typeCounts)) typeCounts[t.key] = defaultTypeCounts.value[t.key] || 5;
      // 分值：按学科真题规格（切换学科时清空后重新赋值）
      if (!(t.key in scorePresets)) {
        if (t.key === 'COMPOSITION') {
          scorePresets[t.key] = bs === '语文' ? 60 : bs === '英语' ? 15 : 40;
        } else if (t.key === 'SINGLE_CHOICE') {
          scorePresets[t.key] = bs === '语文' ? 3 : bs === '数学' ? 4 : bs === '英语' ? 1 : 2;
        } else if (t.key === 'FILL_IN') {
          scorePresets[t.key] = bs === '语文' ? 2 : bs === '数学' ? 4 : bs === '英语' ? 2 : 2;
        } else if (t.key === 'SHORT_ANSWER') {
          scorePresets[t.key] = bs === '语文' ? 8 : 10; // 语文简答8分, 英语翻译2分实际是FILL_IN
        } else if (t.key === 'CALCULATION') {
          scorePresets[t.key] = 12; // 数学解答题约12分
        } else if (t.key === 'READING_COMPREHENSION') {
          scorePresets[t.key] = 2; // 英语阅读每题2分（每篇含4小题=8分）
        } else {
          scorePresets[t.key] = 3;
        }
      }
    }
  },
  { immediate: true }
);

// 配置变更时自动保存到 localStorage
watch(
  [() => examForm.totalScore, () => examForm.duration],
  () => saveConfig()
);

const totalCount = computed(() => {
  return Object.values(typeCounts).reduce((s, v) => s + (v || 0), 0);
});
const totalScore = computed(() => {
  return Object.entries(typeCounts).reduce((s, [k, v]) => s + (v || 0) * (scorePresets[k] || 0), 0);
});

// 表单字段变更时清除旧的错误提示
watch(
  [
    () => examForm.topic,
    () => trainForm.topic,
    () => examForm.subject,
    () => trainForm.subject,
    totalCount,
  ],
  () => {
    if (genError.value) genError.value = '';
  }
);

// P1: 组卷数量上限校验（防止性能/成本风险）
const MAX_QUESTIONS_EXAM = 100;
const MAX_QUESTIONS_TRAINING = 100; // 与仿真组卷统一上限
const tooManyQuestions = computed(() => {
  return (
    (isTraining.value && totalCount.value > MAX_QUESTIONS_TRAINING) ||
    (!isTraining.value && totalCount.value > MAX_QUESTIONS_EXAM)
  );
});
const maxQuestions = computed(() =>
  isTraining.value ? MAX_QUESTIONS_TRAINING : MAX_QUESTIONS_EXAM
);

const step = ref(0);
watch(step, () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
});
const generating = ref(false);
const sseDegraded = ref(false);
const genResult = ref(null);
const genError = ref('');
const progressMsg = ref('');
let progressTimer = null;

// Step 1 → Step 2 校验：学科必选，考纲范围必填
const canGoToStep2 = computed(() => examForm.subject && examForm.outlineScope.trim());
// Step 2 → Step 3 校验：题目数量 > 0，难度比例 = 100%
const canGoToStep3 = computed(() => totalCount.value > 0 && !tooManyQuestions.value && difficultyTotal.value === 100);
// 生成校验
const canGenerate = computed(() => totalCount.value > 0 && !tooManyQuestions.value && difficultyTotal.value === 100);

/** 重置题型数量为学科默认配置 */
const resetToDefaultCounts = () => {
  for (const k of Object.keys(typeCounts)) delete typeCounts[k];
  for (const k of Object.keys(scorePresets)) delete scorePresets[k];
};

const onSubjectChange = async () => {
  const form = isTraining.value ? trainForm : examForm;
  if (genResult.value || Object.keys(typeCounts).length > 0) {
    try {
      await ElMessageBox.confirm('切换学科将清空当前的配置和生成结果，确定继续？', '确认切换', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
      })
    } catch { return }
  }
  genResult.value = null;
  genError.value = '';
  const bs = bareSubject(form.subject);
  // 按考纲自动设置满分和时长
  if (bs === '语文') {
    form.totalScore = 150;
    form.duration = 150;
  } else if (bs === '数学') {
    form.totalScore = 150;
    form.duration = 120;
  } else if (bs === '英语') {
    form.totalScore = 100;
    form.duration = 120;
  } else {
    form.totalScore = 100;
    form.duration = 90;
  }
  // 清空题型计数，让 watch 按学科重新初始化
  for (const k of Object.keys(typeCounts)) delete typeCounts[k];
  for (const k of Object.keys(scorePresets)) delete scorePresets[k];
};

const onCategoryChange = (val) => {
  cascade.subjectId = val.subjectId;
  cascade.chapterId = val.chapterId;
  cascade.taskId = val.taskId;
  cascade.kpId = val.kpId;
  cascade.categoryIds = val.categoryIds || [];
  cascade.subKpList = val.subKpList || [];
};

const onTrainCategoryChange = (val) => {
  trainCascade.subjectId = val.subjectId;
  trainCascade.chapterId = val.chapterId;
  trainCascade.taskId = val.taskId;
  trainCascade.kpId = val.kpId;
  trainCascade.categoryIds = val.categoryIds || [];
  trainCascade.subKpList = val.subKpList || [];
  // 自动填充 topic：如果有子知识点列表，拼接名称；否则用选中节点名称
  if (val.kpList && val.kpList.length > 0) {
    trainForm.topic = val.kpList.map((k) => k.name).join('、');
  } else if (val.subjectName) {
    trainForm.topic = val.subjectName;
  }
  // 自动建议训练标题
  if (!trainForm.title && val.subjectName) {
    const bs = bareSubject(trainForm.subject);
    const name = val.kpList?.length ? val.kpList[0].name : val.subjectName;
    if (bs.includes('语文')) trainForm.title = name + ' 专项训练';
    else if (bs.includes('数学')) trainForm.title = name + ' 专项训练';
    else if (bs.includes('英语')) trainForm.title = name + ' 专项训练';
  }
};

// 训练标题 placeholder：按学科动态提示
const trainTitlePlaceholder = computed(() => {
  const bs = bareSubject(trainForm.subject);
  if (bs.includes('语文')) return '如：文言文虚词专项训练';
  if (bs.includes('数学')) return '如：正弦定理与余弦定理专项训练';
  if (bs.includes('英语')) return '如：动词时态与语态专项训练';
  return '如：专项知识点训练';
});

const buildPayload = () => {
  const mode = isTraining.value ? 'training' : 'exam';
  const payload = {
    mode,
    subject: isTraining.value ? trainForm.subject : examForm.subject,
    title: isTraining.value ? trainForm.title : examForm.title,
    typeCounts: { ...typeCounts },
    scorePresets: { ...scorePresets },
  };
  if (mode === 'exam') {
    payload.totalScore = examForm.totalScore;
    payload.duration = examForm.duration;
    payload.outlineScope = examForm.outlineScope;
    payload.difficultyRatio = { ...difficultyRatio };
    // 多选章节 → 传 categoryIds 数组；单选 → 传 categoryId 单值，保持向后兼容
    if (cascade.categoryIds && cascade.categoryIds.length > 1) {
      payload.categoryIds = cascade.categoryIds;
      payload.subKpList = cascade.subKpList || [];
    } else {
      payload.categoryId = cascade.kpId || cascade.taskId || cascade.chapterId;
    }
    payload.extra = examForm.extra;
  } else {
    // 训练模式：用知识树级联选择
    if (trainCascade.categoryIds && trainCascade.categoryIds.length > 1) {
      payload.categoryIds = trainCascade.categoryIds;
      payload.subKpList = trainCascade.subKpList || [];
    } else {
      payload.categoryId = trainCascade.kpId || trainCascade.taskId || trainCascade.chapterId;
    }
    payload.topic = trainForm.topic;
    payload.extra = trainForm.extra;
  }
  return payload;
};

const startProgressAnimation = () => {
  const start = Date.now();
  progressMsg.value = '正在提交请求...';
  progressTimer = setInterval(() => {
    const elapsed = Math.floor((Date.now() - start) / 1000);
    if (elapsed < 5) progressMsg.value = '正在分析知识范围...';
    else if (elapsed < 15) progressMsg.value = 'AI 正在命题（约需 30-60 秒）...';
    else if (elapsed < 60) progressMsg.value = '即将完成，正在生成试卷...';
    else progressMsg.value = '处理时间较长，请耐心等待...';
  }, 3000);
};

const stopProgressAnimation = () => {
  if (progressTimer) {
    clearInterval(progressTimer);
    progressTimer = null;
  }
  progressMsg.value = '';
};

const outlineScopeStatus = computed(() => {
  const t = examForm.outlineScope?.trim() || ''
  if (t.length === 0) return null
  return t.length >= 5 ? 'ok' : 'short'
})

let cancelFlag = false

const cancelGeneration = () => {
  cancelFlag = true
  generating.value = false
  stopProgressAnimation()
  genError.value = ''
}

const doGenerate = async () => {
  if (!canGenerate.value) return;
  cancelFlag = false
  generating.value = true;
  sseDegraded.value = false;
  genResult.value = null;
  genError.value = '';
  startProgressAnimation();
  try {
    const payload = buildPayload();
    const res = await submitExamPaper(payload);
    if (res.code !== 200) throw new Error(res.message || '提交失败');
    const taskId = res.data?.taskId;
    if (!taskId) throw new Error('未获取到任务ID');

    let sseOk = false;
    try {
      const es = await createEventSource(`/api/ai-output/stream/${taskId}`);
      await new Promise((resolve, reject) => {
        let settled = false;
        const doResolve = (v) => {
          if (!settled) {
            settled = true;
            resolve(v);
          }
        };
        const doReject = (e) => {
          if (!settled) {
            settled = true;
            reject(e);
          }
        };
        const cleanup = () => {
          clearTimeout(timeout);
          clearTimeout(fallbackTimer);
          es.close();
        };
        const cancelCheck = setInterval(() => {
          if (cancelFlag) {
            clearInterval(cancelCheck);
            cleanup();
            doReject(new Error('已取消'));
          }
        }, 500);
        const timeout = setTimeout(() => {
          clearInterval(cancelCheck);
          cleanup();
          doReject(new Error('AI处理超时'));
        }, 240_000);
        let received = false;
        es.addEventListener('task-result', (e) => {
          received = true;
          if (settled) return;
          try {
            const d = JSON.parse(e.data);
            if (d.status === 'COMPLETED') {
              clearInterval(cancelCheck);
              clearTimeout(timeout);
              es.close();
              genResult.value = d.result;
              doResolve();
            } else if (d.status === 'FAILED') {
              clearInterval(cancelCheck);
              cleanup();
              doReject(new Error(d.error || 'AI处理失败'));
            }
          } catch (err) {
            clearInterval(cancelCheck);
            cleanup();
            doReject(err);
          }
        });
        const fallbackTimer = setTimeout(() => {
          if (!received) {
            es.close();
            doReject(new Error('SSE_DEGRADE'));
          }
        }, 5000);
      });
      sseOk = true;
    } catch (err) {
      if (err.message === 'SSE_DEGRADE') {
        sseDegraded.value = true
      } else if (err.message !== '已取消') throw err;
    }

    if (!sseOk && !genResult.value && !cancelFlag) {
      const start = Date.now();
      let interval = 3000;
      while (Date.now() - start < 240_000) {
        if (cancelFlag) break;
        await new Promise((r) => setTimeout(r, interval));
        interval = Math.min(interval * 1.5, 6000);
        const pollRes = await getAiOutputResult(taskId);
        if (pollRes.code !== 200) continue;
        const { status, result, error } = pollRes.data;
        if (status === 'COMPLETED') {
          genResult.value = result;
          break;
        }
        if (status === 'FAILED') throw new Error(error || 'AI处理失败');
      }
      if (!genResult.value && !cancelFlag) throw new Error('生成超时');
    }
  } catch (e) {
    if (e.message !== '已取消') genError.value = e.message || '生成失败';
  } finally {
    stopProgressAnimation();
    if (!cancelFlag) generating.value = false;
    // 埋点：组卷生成
    if (genResult.value) {
      const qCount = genResult.value.questions?.length || 0;
      trackEvent('EXAM_PAPER_GENERATE', {
        subject: examForm.subject || trainForm.subject || '',
        mode: isTraining.value ? 'training' : 'exam',
        questionCount: qCount,
        types: Object.entries(typeCounts)
          .filter(([, v]) => v > 0)
          .map(([k]) => k)
          .join(','),
      });
    }
  }
};

const classes = ref([]);
const cascadeRef = ref(null);
const trainCascadeRef = ref(null);
const examDialogVisible = ref(false);
const questionEditorVisible = ref(false);
const editingBatchId = ref('');
const editingQuestions = ref([]);

const onEditExamQuestions = (batchId, questions) => {
  editingBatchId.value = batchId;
  editingQuestions.value = questions || [];
  questionEditorVisible.value = true;
};
const publishForm = reactive({
  title: '',
  classIds: [],
  durationMinutes: 90,
  totalScore: 100,
  deadline: null,
});

const lastSelectedIds = ref([]);

const onPublishAsExam = async (selectedIds) => {
  lastSelectedIds.value = selectedIds?.length
    ? selectedIds
    : (genResult.value?.questions || []).map((q) => q.id);
  const qids = lastSelectedIds.value.length
    ? lastSelectedIds.value
    : (genResult.value?.questions || []).map((q) => q.id);
  if (!qids.length) return ElMessage.warning('没有可发布的题目');
  publishForm.title = genResult.value.title || examForm.title || trainForm.title;
  publishForm.totalScore = isTraining.value ? totalScore.value : examForm.totalScore;
  publishForm.durationMinutes = isTraining.value ? 60 : examForm.duration;
  publishForm.classIds = [];
  try {
    const res = await getMyClasses();
    if (res.code === 200) classes.value = res.data || [];
  } catch {
    /* */
  }
  examDialogVisible.value = true;
};

const doPublishExam = async () => {
  if (!publishForm.classIds.length) return ElMessage.warning('请选择班级');
  const qids = lastSelectedIds.value.length
    ? lastSelectedIds.value
    : (genResult.value?.questions || []).map((q) => q.id);
  try {
    const res = await publishQuestionsAsExam({
      questionIds: qids,
      title: publishForm.title,
      subject: isTraining.value ? trainForm.subject : examForm.subject,
      totalScore: publishForm.totalScore,
      classIds: publishForm.classIds,
      deadline: publishForm.deadline,
      durationMinutes: publishForm.durationMinutes,
    });
    if (res.code === 200) {
      examDialogVisible.value = false;
      trackEvent('EXAM_PAPER_PUBLISH', {
        subject: examForm.subject || trainForm.subject || '',
        mode: isTraining.value ? 'training' : 'exam',
        questionCount: qids.length,
        classCount: publishForm.classIds?.length || 0,
      });
      ElMessageBox.confirm(
        `共发布 <strong>${qids.length}</strong> 道题目到 <strong>${publishForm.classIds.length}</strong> 个班级`,
        '发布成功',
        {
          confirmButtonText: '查看已发布任务',
          cancelButtonText: '继续组卷',
          type: 'success',
          dangerouslyUseHTMLString: true,
        }
      ).then(() => {
        router.push('/teacher/tasks');
      }).catch(() => {
        /* 继续组卷，不做操作 */
      });
    }
  } catch {
    ElMessage.error('发布失败');
  }
};
</script>

<style scoped>
.epc-page {
  margin: 0 auto;
  padding: 24px;
}
.epc-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.epc-header h2 {
  margin: 0;
  font-size: var(--fs-xl);
  color: #303133;
}
.epc-card {
  margin-bottom: 20px;
}
.epc-card-title {
  font-size: var(--fs-md);
  font-weight: 700;
}
.epc-type-item {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: var(--fs-sm);
}
.epc-type-label {
  font-weight: 500;
  min-width: 48px;
}
.epc-syllabus-status { font-size: var(--fs-xs); margin-top: 4px; white-space: nowrap; }
.epc-syllabus-ok { color: var(--el-color-success); }
.epc-syllabus-warn { color: var(--el-color-warning); }
.epc-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 10px 14px;
  background: #fef0f0;
  border-radius: 8px;
  color: var(--el-color-danger, #f56c6c);
  font-size: var(--fs-sm);
}
.epc-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 12px;
}
.epc-progress-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e4e7ed;
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: epc-spin 0.7s linear infinite;
}
@keyframes epc-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
