<template>
  <div class="cp-page">
    <!-- 面包屑导航 -->
    <div class="cp-breadcrumb">
      <span @click="$router.push('/student/checkpoint')">闯关学习</span>
      <span class="cp-breadcrumb-sep">/</span>
      <span @click="$router.push(`/student/checkpoint/${subjectId}`)">{{ subjectName }}</span>
      <span class="cp-breadcrumb-sep">/</span>
      <span class="cp-breadcrumb-current">{{ data.taskName }}</span>
    </div>

    <!-- 顶部导航 -->
    <div class="cp-header">
      <el-button text @click="$router.push(`/student/checkpoint/${subjectId}`)">
        &larr; 返回总览
      </el-button>
      <div class="cp-title-area">
        <h3>{{ data.taskName }}</h3>
        <span class="cp-chapter">{{ data.chapterName }}</span>
      </div>
      <!-- 总进度条 -->
      <div v-if="data.totalKnowledgePoints" class="cp-overall-progress">
        <div class="cp-og-text">
          知识点 {{ resolvedKpCount || 0 }} / {{ data.totalKnowledgePoints || 0 }}
          <span
            v-if="data.totalKeywords"
            style="color: var(--text-secondary); font-size: var(--fs-xs)"
          >
            · 关键词已确认：{{ confirmedKeywordCount }} / {{ data.totalKeywords }}
          </span>
        </div>
        <div class="cp-progress-bar">
          <div class="cp-progress-fill" :style="{ width: kpProgressPercent + '%' }"></div>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="cp-body">
      <!-- Step 0: 考纲锚点 -->
      <div v-if="currentStep === 0 && keyPoints.length > 0">
        <SyllabusBadge
          :syllabus="{
            dim: 'THEORY',
            level: '理解',
            examTip: '覆盖' + data.totalKnowledgePoints + '个知识点',
          }"
        />
        <p style="font-size: var(--fs-sm); color: var(--text-regular); margin: 12px 0">
          本关包含 <strong>{{ data.totalKnowledgePoints }}</strong> 个知识点，
          <strong>{{ data.totalKeywords }}</strong> 个关键概念需要掌握。
          <strong>{{ checkpointQuestions.length }}</strong> 道验证题覆盖全部知识点。
        </p>
        <div class="cp-action">
          <el-button type="primary" @click="currentStep = 1">开始学习全部知识点 →</el-button>
        </div>
      </div>

      <!-- Step 1: 遍历每个知识点 -->
      <div v-if="currentStep === 1 && keyPoints.length > 0">
        <div class="cp-kp-navigator">
          <div class="cp-kp-nav-left">
            <el-button
              text
              size="small"
              :disabled="currentKpIndex === 0"
              @click="prevKp"
            >
              ← 上一个
            </el-button>
          </div>
          <div class="cp-kp-nav-center">
            <span
              v-for="(kp, i) in keyPoints"
              :key="i"
              class="cp-kp-dot"
              :class="{
                'cp-kp-done': kpStatuses[i] === 'done',
                'cp-kp-active': i === currentKpIndex,
                'cp-kp-skip': kpStatuses[i] === 'skipped',
              }"
              @click="goKp(i)"
            >{{ i + 1 }}</span>
          </div>
          <div class="cp-kp-nav-right">
            <el-button
              v-if="kpStatuses[currentKpIndex] === 'done'"
              type="primary"
              size="small"
              @click="nextKp"
            >
              {{ currentKpIndex < keyPoints.length - 1 ? '下一个 →' : '全部完成！开始验证闯关' }}
            </el-button>
          </div>
        </div>

        <!-- 当前知识点卡片 -->
        <div v-if="currentKeyPoint" class="cp-kp-card">
          <div class="cp-kp-badge">知识点 {{ currentKpIndex + 1 }}</div>
          <div class="cp-section-title">{{ currentKeyPoint.title }}</div>

          <!-- 知识点内容（Markdown 渲染） -->
          <TheoryReader :detail-html="currentKeyPoint.detailHtml || ''" />

          <!-- 该知识点的关键词确认（有关键词时） -->
          <KeywordConfirm
            v-if="
              currentKeyPoint.keywords &&
                currentKeyPoint.keywords.length > 0 &&
                kpStatuses[currentKpIndex] !== 'done' &&
                kpStatuses[currentKpIndex] !== 'skipped'
            "
            :ref="
              (el) => {
                if (el) keywordRefs[currentKpIndex] = el;
              }
            "
            :keywords="currentKeyPoint.keywords"
            :attempts="data.attempts || 0"
            :global-offset="currentKpKeywordOffset"
            @verify="handleKeywordsVerify"
            @skip="handleKeywordSkip"
            @sos="handleSOS"
            @paypass="handlePayPass"
          />

          <!-- 无关键词时：手动点击"已阅读"确认 -->
          <div
            v-if="
              (!currentKeyPoint.keywords || currentKeyPoint.keywords.length === 0) &&
                kpStatuses[currentKpIndex] === 'pending'
            "
            class="cp-no-kw-confirm"
          >
            <p>这个知识点没有需要填空确认的关键概念。</p>
            <el-button type="primary" @click="markCurrentKpDone">
              已阅读，{{
                currentKpIndex < keyPoints.length - 1
                  ? '进入下一个知识点 →'
                  : '完成全部知识点学习 →'
              }}
            </el-button>
          </div>

          <div v-if="kpStatuses[currentKpIndex] === 'done'" class="cp-kp-done-badge">
            ✓ 已掌握 — 关键词确认通过
          </div>
          <div v-if="kpStatuses[currentKpIndex] === 'skipped'" class="cp-kp-skip-badge">
            已跳过（-1积分）
          </div>
          <div v-if="kpStatuses[currentKpIndex] === 'pending'" class="cp-kp-pending-hint">
            ⚡ 阅读后请在下方的核心概念确认中填入关键术语，全部正确后自动进入下一个知识点
          </div>
        </div>
      </div>

      <!-- Step 2: 验证闯关（所有知识点确认后） -->
      <div v-if="currentStep === 2 && checkpointQuestions.length > 0">
        <div class="cp-section-title">
          验证闯关 — 从全部 {{ data.totalKnowledgePoints }} 个知识点中抽题
        </div>
        <p style="font-size: var(--fs-sm); color: var(--text-secondary); margin-bottom: 12px">
          共 {{ checkpointQuestions.length }} 题 · 全部答对即通关 · 答错可重试
        </p>
        <CheckpointQuestion
          v-for="(q, i) in checkpointQuestions"
          :key="i"
          :question="q"
          :show-result="finalSubmitted"
          :result-correct="verificationResults[i]"
          :correct-label="q.correctAnswer || ''"
          @submit="(ans) => handleSingleSubmit(i, ans)"
        />
        <div v-if="allVerificationDone && !finalSubmitted" class="cp-action">
          <el-button type="primary" size="large" @click="handleFinalSubmit">
            提交全部验证 →
          </el-button>
        </div>
      </div>
      <div v-if="currentStep === 2 && checkpointQuestions.length === 0">
        <el-empty description="暂无可用的闯关题目" :image-size="60" />
        <div class="cp-action">
          <el-button @click="$router.push(`/student/checkpoint/${subjectId}`)">返回总览</el-button>
        </div>
      </div>
    </div>

    <!-- 通关结果弹层 -->
    <CelebrationOverlay
      v-if="resultVisible && resultPassed"
      :credits="resultCredits"
      :accuracy="resultAccuracy"
      :has-more="false"
      @close="resultVisible = false"
      @next="handleRetry"
    />

    <!-- 未通关干预弹层 -->
    <Transition name="overlay">
      <div
        v-if="resultVisible && !resultPassed"
        class="fail-overlay"
        @click.self="resultVisible = false"
      >
        <div class="fail-card">
          <div class="fail-icon">📖</div>
          <div class="fail-title">继续加油！</div>
          <div class="fail-stats">
            正确 {{ resultCorrectCount }}/{{ resultTotalCount }} 题 · 正确率 {{ resultAccuracy }}%
          </div>
          <div class="fail-message">
            {{ resultIntervention?.message || '差一点就过关了，再试一次吧！' }}
          </div>
          <div class="fail-actions">
            <el-button type="primary" @click="handleRetry">再试一次</el-button>
            <el-button @click="resultVisible = false">关闭</el-button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue';
import { useRoute } from 'vue-router';
import {
  startCheckpoint,
  verifyKeywords,
  submitCheckpoint,
  skipKeyword,
  sendSOS,
} from '@/api/checkpoint';
import { ElMessage } from 'element-plus';
import SyllabusBadge from '@/components/checkpoint/SyllabusBadge.vue';
import TheoryReader from '@/components/checkpoint/TheoryReader.vue';
import KeywordConfirm from '@/components/checkpoint/KeywordConfirm.vue';
import CheckpointQuestion from '@/components/checkpoint/CheckpointQuestion.vue';
import CelebrationOverlay from '@/components/checkpoint/CelebrationOverlay.vue';

const route = useRoute();
const subjectId = computed(() => Number(route.params.subjectId));
const configId = computed(() => Number(route.params.configId));
const subjectName = computed(() => data.value.subjectName || '');

const currentStep = ref(0);
const currentKpIndex = ref(0);
const loading = ref(true);
const data = ref({});

// 每个 KP 的状态: 'pending' | 'done' | 'skipped'
const kpStatuses = reactive([]);
const keywordRefs = reactive({});
const verifiedKeywordIndices = reactive(new Set());

// 验证题
const verificationAnswered = ref([]); // boolean[] — 每题是否已答
const submissionAnswers = ref([]); // 实际答案数据
const verificationResults = ref([]); // boolean[] — 每題判分结果
const finalSubmitted = ref(false); // 是否已提交最终批改

const resultVisible = ref(false);
const resultPassed = ref(false);
const resultCredits = ref(0);
const resultCorrectCount = ref(0);
const resultTotalCount = ref(0);
const resultAccuracy = ref(0);
const resultDetails = ref([]);
const resultIntervention = ref(null);

const keyPoints = computed(() => data.value.keyPoints || []);
const checkpointQuestions = computed(() => data.value.checkpointQuestions || []);

const currentKeyPoint = computed(() => keyPoints.value[currentKpIndex.value] || null);

// 当前 KP 在全局关键词数组中的偏移
const currentKpKeywordOffset = computed(() => {
  let offset = 0;
  for (let i = 0; i < currentKpIndex.value; i++) {
    offset += keyPoints.value[i]?.keywords?.length || 0;
  }
  return offset;
});

const confirmedKeywordCount = computed(() => {
  const total = 0;
  // 统计 keywordRefs 中每个 KP 的正确数
  const prevCorrect = data.value.previouslyCorrect || {};
  return Object.keys(prevCorrect).length + verifiedKeywordIndices.size;
});

const resolvedKpCount = computed(
  () => kpStatuses.filter((s) => s === 'done' || s === 'skipped').length
);

const kpProgressPercent = computed(() => {
  if (!data.value.totalKnowledgePoints) return 0;
  return Math.round((resolvedKpCount.value * 100) / data.value.totalKnowledgePoints);
});

const allKpsResolved = computed(
  () =>
    keyPoints.value.length > 0 &&
    keyPoints.value.every((_, i) => kpStatuses[i] === 'done' || kpStatuses[i] === 'skipped')
);

const allVerificationDone = computed(
  () =>
    checkpointQuestions.value.length > 0 &&
    verificationAnswered.value.filter((v) => v === true).length >= checkpointQuestions.value.length
);

onMounted(async () => {
  const res = await startCheckpoint(configId.value);
  if (res.code === 200) {
    data.value = res.data;
    // 初始化状态数组
    kpStatuses.length = keyPoints.value.length;
    keyPoints.value.forEach((_, i) => {
      // 该 KP 有关键词时才检查是否已全部确认；无关键词的 KP 始终设为 pending
      const kwCount = _.keywords?.length || 0;
      if (kwCount === 0) {
        kpStatuses[i] = 'pending';
      } else {
        const kpStart = currentKpKeywordOffsetFor(i);
        const kpEnd = kpStart + kwCount;
        const prevCorrect = res.data.previouslyCorrect || {};
        let allDone = true;
        for (let j = kpStart; j < kpEnd; j++) {
          if (!prevCorrect[j]) {
            allDone = false;
            break;
          }
        }
        kpStatuses[i] = allDone ? 'done' : 'pending';
      }
    });
    // 断点续学：如果之前有进度，跳转到上次看到的知识点
    const savedIndex = res.data.lastNodeIndex;
    if (savedIndex && savedIndex > 0 && savedIndex < keyPoints.value.length) {
      currentKpIndex.value = savedIndex;
      const allDone = keyPoints.value.every((_, i) => kpStatuses[i] === 'done');
      if (allDone) {
        currentStep.value = 2;
      }
    }
  }
  loading.value = false;
});

function currentKpKeywordOffsetFor(i) {
  let offset = 0;
  for (let j = 0; j < i; j++) {
    offset += keyPoints.value[j]?.keywords?.length || 0;
  }
  return offset;
}

function prevKp() {
  if (currentKpIndex.value > 0) currentKpIndex.value--;
}
function nextKp() {
  if (currentKpIndex.value < keyPoints.value.length - 1) {
    currentKpIndex.value++;
  } else if (allKpsResolved.value) {
    currentStep.value = 2;
  }
}
function goKp(i) {
  currentKpIndex.value = i;
}

async function handleKeywordsVerify(answers, callback) {
  // 将 answers 的局部 index 转换为全局 index
  const globalOffset = currentKpKeywordOffset.value;
  const globalAnswers = answers.map((a) => ({ ...a, index: a.index + globalOffset }));

  const res = await verifyKeywords(configId.value, globalAnswers);
  if (res.code === 200) {
    callback(res.data.results);
    if (res.data.allKeywordsCovered) {
      markCurrentKpDone();
      ElMessage.success(`知识点 ${currentKpIndex.value + 1} 已掌握！`);
      if (allKpsResolved.value) {
        ElMessage.success('全部知识点已掌握！进入验证闯关');
        setTimeout(() => {
          currentStep.value = 2;
        }, 1000);
      }
    }
  }
}

async function handleKeywordSkip(idx) {
  try {
    const res = await skipKeyword(configId.value, idx + currentKpKeywordOffset.value);
    if (res.code === 200) {
      ElMessage.warning('已跳过该关键词，消耗 1 积分');
      if (res.data.keywordsPassed) {
        markCurrentKpDone();
      }
    } else {
      ElMessage.error(res.msg || '跳过失败');
    }
  } catch (e) {
    ElMessage.error('跳过失败');
  }
}

function handleSOS() {
  sendSOS(configId.value)
    .then((res) => {
      if (res.code === 200) ElMessage.success('已通知任课教师');
      else ElMessage.error('通知发送失败');
    })
    .catch(() => ElMessage.error('通知发送失败'));
}

async function handlePayPass() {
  const globalOffset = currentKpKeywordOffset.value;
  const kwCount = currentKeyPoint.value?.keywords?.length || 0;
  try {
    for (let i = 0; i < kwCount; i++) {
      const globalIdx = globalOffset + i;
      const res = await skipKeyword(configId.value, globalIdx);
      if (res.code !== 200) {
        ElMessage.error(res.msg || '扣分过关失败');
        return;
      }
    }
    ElMessage.warning('已消耗 ' + kwCount + ' 积分，直接过关');
    markCurrentKpDone();
  } catch (e) {
    ElMessage.error('扣分过关失败');
  }
}

function markCurrentKpDone() {
  kpStatuses[currentKpIndex.value] = 'done';
}

function handleSingleSubmit(idx, answer) {
  verificationAnswered.value[idx] = true;
  submissionAnswers.value[idx] = answer;
}

async function handleFinalSubmit() {
  const answers = submissionAnswers.value
    .filter((a) => a && a.questionId)
    .map((a) => ({ questionId: a.questionId, answer: a.answer }));

  const res = await submitCheckpoint(configId.value, { answers });
  if (res.code === 200) {
    const d = res.data;
    // 逐题填充判分结果
    if (d.details) {
      verificationResults.value = d.details.map((dt) => dt.correct);
    }
    finalSubmitted.value = true;
    resultPassed.value = d.passed;
    resultCredits.value = d.creditsEarned || 0;
    resultCorrectCount.value = d.correctCount || 0;
    resultTotalCount.value = d.totalCount || 0;
    resultAccuracy.value = d.accuracy || 0;
    resultDetails.value = d.details || [];
    resultIntervention.value = d.intervention || null;
    resultVisible.value = true;
  }
}

async function handleRetry() {
  resultVisible.value = false;
  currentStep.value = 0;
  currentKpIndex.value = 0;
  verificationAnswered.value.length = 0;
  submissionAnswers.value.length = 0;
  verificationResults.value.length = 0;
  finalSubmitted.value = false;
  // 重新加载数据
  loading.value = true;
  const res = await startCheckpoint(configId.value);
  if (res.code === 200) {
    data.value = res.data;
    kpStatuses.length = keyPoints.value.length;
    keyPoints.value.forEach((_, i) => {
      const kwCount = _.keywords?.length || 0;
      if (kwCount === 0) {
        kpStatuses[i] = 'pending';
      } else {
        const kpStart = currentKpKeywordOffsetFor(i);
        const kpEnd = kpStart + kwCount;
        const prevCorrect = res.data.previouslyCorrect || {};
        let allDone = true;
        for (let j = kpStart; j < kpEnd; j++) {
          if (!prevCorrect[j]) {
            allDone = false;
            break;
          }
        }
        kpStatuses[i] = allDone ? 'done' : 'pending';
      }
    });
  }
  loading.value = false;
}
</script>

<style scoped>
.cp-page {
  margin: 0 auto;
  padding: var(--spacing-lg);
}
.cp-breadcrumb {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}
.cp-breadcrumb span:not(.cp-breadcrumb-current) {
  cursor: pointer;
}
.cp-breadcrumb span:not(.cp-breadcrumb-current):hover {
  color: var(--primary-color);
}
.cp-breadcrumb-sep {
  margin: 0 8px;
  color: var(--text-disabled);
}
.cp-breadcrumb-current {
  color: var(--text-primary);
  font-weight: 500;
}
.cp-header {
  margin-bottom: var(--spacing-md);
}
.cp-title-area {
  margin: 8px 0;
}
.cp-title-area h3 {
  font-size: var(--fs-xl);
  color: var(--text-primary);
  display: inline;
}
.cp-chapter {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  margin-left: 8px;
}

.cp-overall-progress {
  margin-top: 12px;
}
.cp-og-text {
  font-size: var(--fs-sm);
  color: var(--text-primary);
  margin-bottom: 4px;
}
.cp-progress-bar {
  height: 6px;
  background: var(--bg-secondary);
  border-radius: 3px;
  overflow: hidden;
}
.cp-progress-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: 3px;
  transition: width 0.4s ease;
}

.cp-body {
  margin-top: var(--spacing-md);
}

/* KP 导航器 */
.cp-kp-navigator {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
  position: sticky;
  top: 0;
  z-index: 10;
}
.cp-kp-nav-left,
.cp-kp-nav-right {
  min-width: 80px;
}
.cp-kp-nav-center {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}
.cp-kp-dot {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: var(--fs-xs);
  font-weight: 600;
  cursor: pointer;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  transition: all 0.2s;
}
.cp-kp-done {
  background: var(--bg-success-light);
  color: var(--el-color-success);
}
.cp-kp-active {
  background: var(--primary-color);
  color: #fff;
  transform: scale(1.15);
}
.cp-kp-skip {
  background: var(--bg-warning-light);
  color: var(--el-color-warning);
}

/* KP 卡片 */
.cp-kp-card {
  background: var(--bg-card);
  border: 0.5px solid var(--border-light);
  border-left: 3px solid var(--primary-color);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}
.cp-kp-badge {
  font-size: var(--fs-xs);
  color: var(--primary-color);
  font-weight: 600;
  margin-bottom: 4px;
}
.cp-kp-done-badge {
  text-align: center;
  font-size: var(--fs-lg);
  color: var(--el-color-success);
  font-weight: 600;
  padding: var(--spacing-md);
}
.cp-kp-skip-badge {
  text-align: center;
  font-size: var(--fs-lg);
  color: var(--el-color-warning);
  font-weight: 600;
  padding: var(--spacing-md);
}
.cp-kp-pending-hint {
  text-align: center;
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  padding: 8px 12px;
  margin-top: 8px;
  background: rgba(67, 97, 238, 0.04);
  border-radius: var(--radius-sm);
}

.cp-no-kw-confirm {
  text-align: center;
  padding: var(--spacing-lg);
  margin-top: var(--spacing-md);
  background: rgba(67, 97, 238, 0.04);
  border: 1px dashed var(--primary-color);
  border-radius: var(--radius-md);
}
.cp-no-kw-confirm p {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-md);
}

.cp-section-title {
  font-weight: 600;
  font-size: var(--fs-lg);
  margin-bottom: var(--spacing-md);
  color: var(--text-primary);
}
.cp-action {
  margin-top: var(--spacing-md);
  display: flex;
  gap: 8px;
}

/* 失败干预弹层 */
.fail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
}
.fail-card {
  background: white;
  border-radius: 16px;
  padding: 40px 36px;
  text-align: center;
  max-width: 360px;
  width: 90%;
  animation: slideUp 0.4s ease-out;
}
.fail-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.fail-title {
  font-size: 20px;
  font-weight: 700;
  color: #18181b;
  margin-bottom: 8px;
}
.fail-stats {
  font-size: 14px;
  color: #71717a;
  margin-bottom: 16px;
}
.fail-message {
  font-size: 15px;
  color: var(--primary-color);
  font-weight: 500;
  margin-bottom: 24px;
  padding: 12px;
  background: #f0f4ff;
  border-radius: 8px;
}
.fail-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
