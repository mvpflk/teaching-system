<template>
  <div class="ph-card">
    <div class="ph-card-title">周末线上小测</div>
    <p class="ph-card-desc">4道原题变数字 + 3道AI变式 + 3道遗忘检测</p>
    <van-button
      type="warning"
      block
      round
      :loading="testLoading"
      style="background: var(--el-color-warning); border-color: var(--el-color-warning)"
      @click="startOnlineTest"
    >
      开始小测
    </van-button>
    <div v-if="testResult" class="ph-test-result">
      <van-tag :type="testResult.passed ? 'success' : 'danger'" size="medium" effect="plain">
        {{ testResult.passed ? '通过' : '未通过' }} · {{ testResult.score }}分
      </van-tag>
      <span class="ph-test-rate">{{ testResult.correctCount || testResult.correctRate || 0 }}%正确率</span>
      <div v-if="testResult.itemResults?.length" class="ph-item-results" style="margin-top: 12px">
        <van-collapse v-model="testDetailOpen" accordion>
          <van-collapse-item title="查看答案解析" name="detail" class="ph-detail-collapse">
            <div class="ph-scoring-rule">
              <van-notice-bar
                left-icon="info-o"
                scrollable
                :text="testResult.scoringRule || '判分规则'"
                background="var(--primary-light)"
                color="var(--primary-color)"
              />
            </div>
            <div
              v-for="(item, ii) in testResult.itemResults"
              :key="ii"
              class="ph-item-row"
              :class="itemRowClass(item)"
            >
              <div class="ph-item-head">
                <span class="ph-item-num">第{{ ii + 1 }}题</span>
                <span class="ph-item-type-tag">{{ typeLabel(item.questionType) }}</span>
                <van-tag
                  v-if="item.isCorrect"
                  type="success"
                  size="mini"
                  effect="plain"
                >
                  正确
                </van-tag>
                <van-tag
                  v-else-if="item.matchMode === 'pending_review'"
                  type="warning"
                  size="mini"
                  effect="plain"
                >
                  待评阅
                </van-tag>
                <van-tag
                  v-else-if="item.matchMode === 'unanswered'"
                  size="mini"
                  effect="plain"
                  color="var(--text-disabled)"
                >
                  未作答
                </van-tag>
                <van-tag
                  v-else
                  type="danger"
                  size="mini"
                  effect="plain"
                >
                  错误
                </van-tag>
                <span v-if="item.matchMode === 'fuzzy'" class="ph-match-badge">模糊匹配</span>
              </div>
              <div class="ph-item-body">
                <div class="ph-item-row-ans">
                  <span class="ph-ans-label">你的答案：</span>
                  <span
                    class="ph-ans-value"
                    :class="{
                      'ph-ans-strikethrough':
                        !item.isCorrect &&
                        item.matchMode !== 'pending_review' &&
                        item.matchMode !== 'unanswered',
                    }"
                    v-html="renderMath(item.studentAnswer || '(空)')"
                  ></span>
                </div>
                <div
                  v-if="!item.isCorrect && item.correctAnswer"
                  class="ph-item-row-ans ph-correct-row"
                >
                  <span class="ph-ans-label">参考答案：</span>
                  <span
                    class="ph-ans-value ph-ans-correct"
                    v-html="renderMath(item.correctAnswer)"
                  ></span>
                </div>
                <div v-if="item.explanation" class="ph-item-explain">
                  <span class="ph-explain-label">解析：</span><span v-html="renderMath(item.explanation)" />
                </div>
              </div>
            </div>
          </van-collapse-item>
        </van-collapse>
      </div>
    </div>
    <div v-if="testQuestions.length" class="ph-test-qs">
      <div v-for="(q, i) in testQuestions" :key="i" class="ph-test-q">
        <div class="ph-test-q-label">
          Q{{ i + 1 }}
          <span class="ph-test-q-type">({{ typeLabel(q.type || q.questionType) }})</span>
          <DifficultyBadge :difficulty-level="q.difficultyLevel" :tier="q.tier" />
        </div>
        <div
          class="ph-test-q-text"
          v-html="
            q.questionText ? sanitizeMathHtml(renderMath(q.questionText)) : '(题目内容加载中...)'
          "
        />
        <div v-if="isMultiType(q.questionType)" class="ph-diag-opts">
          <div
            v-for="(opt, oi) in parseOptions(q.options) || []"
            :key="oi"
            class="ph-diag-opt"
            :class="{
              selected: (Array.isArray(testAnswers[i]) ? testAnswers[i] : []).includes(opt.key),
            }"
            @click="toggleMulti(i, opt.key)"
          >
            <span class="ph-diag-opt-letter">{{ opt.key }}</span>
            <span class="ph-diag-opt-text" v-html="renderMath(opt.text)" />
            <van-checkbox
              :model-value="(Array.isArray(testAnswers[i]) ? testAnswers[i] : []).includes(opt.key)"
            />
          </div>
        </div>
        <van-radio-group
          v-else-if="isChoiceType(q.questionType)"
          v-model="testAnswers[i]"
          class="ph-diag-opts"
        >
          <div
            v-for="(opt, oi) in parseOptions(q.options) || []"
            :key="oi"
            class="ph-diag-opt"
            :class="{ selected: testAnswers[i] === opt.key }"
            @click="testAnswers[i] = opt.key"
          >
            <span class="ph-diag-opt-letter">{{ opt.key }}</span>
            <span class="ph-diag-opt-text" v-html="renderMath(opt.text)" />
            <van-radio :name="opt.key" />
          </div>
        </van-radio-group>
        <MathFormulaEditor v-else-if="isMathInputType(q.questionType)" v-model="testAnswers[i]" />
        <van-field
          v-else
          v-model="testAnswers[i]"
          placeholder="请输入答案"
          clearable
        />
      </div>
      <van-button
        type="primary"
        block
        round
        size="small"
        :loading="testSubmitting"
        style="
          background: var(--primary-color);
          border-color: var(--primary-color);
          margin-top: 12px;
        "
        @click="submitTest"
      >
        提交小测
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { showToast } from 'vant';
import 'vant/es/toast/style';
import {
  typeLabel,
  isChoiceType,
  isMultiType,
  isMathInputType,
  parseOptions,
  renderMath,
} from '@/composables/useQuestionHelpers';
import { sanitizeMathHtml } from '@/utils/markdown';
import { getOnlineTest, submitOnlineTest } from '@/api/precision';
import DifficultyBadge from '@/components/common/DifficultyBadge.vue';
import MathFormulaEditor from '@/components/precision/MathFormulaEditor.vue';

const props = defineProps({
  subject: { type: Object, required: true },
});

const testLoading = ref(false);
const testSubmitting = ref(false);
const testQuestions = ref([]);
const testAnswers = ref([]);
const testResult = ref(null);
const testDetailOpen = ref([]);

function toggleMulti(idx, key) {
  if (!Array.isArray(testAnswers.value[idx])) {
    testAnswers.value[idx] = [];
  }
  const arr = [...testAnswers.value[idx]];
  const pos = arr.indexOf(key);
  if (pos >= 0) arr.splice(pos, 1);
  else arr.push(key);
  testAnswers.value[idx] = arr;
}

function itemRowClass(item) {
  return {
    'ph-item-correct': item.isCorrect,
    'ph-item-wrong':
      !item.isCorrect && item.matchMode !== 'pending_review' && item.matchMode !== 'unanswered',
    'ph-item-pending': item.matchMode === 'pending_review',
    'ph-item-empty': item.matchMode === 'unanswered',
  };
}

async function startOnlineTest() {
  testLoading.value = true;
  testResult.value = null;
  try {
    const res = await getOnlineTest(props.subject.key);
    testQuestions.value = res.data?.questions || [];
    testAnswers.value = testQuestions.value.map((q) => (isMultiType(q.questionType) ? [] : ''));
  } catch {
    showToast('获取失败');
  }
  testLoading.value = false;
}

async function submitTest() {
  testSubmitting.value = true;
  try {
    const answers = testQuestions.value.map((q, i) => {
      const a = testAnswers.value[i];
      return {
        questionId: q.questionId,
        answer: Array.isArray(a) ? a.sort().join(',') : a || '',
        questionType: q.questionType || 'FILL_IN',
        expected: q.expected || q.correctAnswer || '',
        source: q.source || '',
      };
    });
    const res = await submitOnlineTest({ subject: props.subject.key, answers });
    testResult.value = res.data;
    if (res.data?.passed) showToast('通过');
  } catch {
    showToast('提交失败');
  }
  testSubmitting.value = false;
}
</script>

<style scoped>
.ph-card {
  margin: 0 16px;
  padding: 20px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-md, 8px);
}
.ph-card-title {
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--text-primary, var(--text-primary));
  margin-bottom: 6px;
}
.ph-card-desc {
  font-size: var(--fs-sm);
  color: var(--text-secondary, var(--text-secondary));
  margin: 0 0 16px;
  line-height: 1.5;
}
.ph-test-result {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.ph-test-rate {
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--text-secondary));
}
.ph-test-qs {
  margin-top: 12px;
}
.ph-test-q {
  margin-bottom: 16px;
}
.ph-test-q-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  margin-bottom: 6px;
}
.ph-test-q-type {
  font-weight: 400;
  color: var(--text-secondary);
}
.ph-test-q-text {
  margin-bottom: 6px;
  font-size: var(--fs-lg);
  line-height: 1.6;
  word-break: break-word;
}
.ph-test-q-text :deep(.katex) {
  font-size: 1.05em;
}
.ph-diag-opts {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ph-diag-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-sm, 4px);
  cursor: pointer;
  transition: background 0.1s;
  background: var(--bg-card, #fff);
}
.ph-diag-opt:hover {
  background: var(--bg-hover, var(--bg-hover));
}
.ph-diag-opt.selected {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.ph-diag-opt-letter {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary, var(--bg-secondary));
  border-radius: 50%;
  font-size: var(--fs-xs);
  font-weight: 700;
  color: var(--text-secondary, var(--text-secondary));
  flex-shrink: 0;
}
.ph-diag-opt-text {
  flex: 1;
  font-size: var(--fs-md);
  color: var(--text-primary, var(--text-primary));
}
.ph-item-results {
  width: 100%;
  text-align: left;
}
.ph-scoring-rule {
  margin-bottom: 10px;
}
.ph-detail-collapse {
  background: transparent;
}
.ph-detail-collapse :deep(.van-collapse-item__content) {
  background: transparent;
  padding: 8px 0;
}
.ph-item-row {
  padding: 12px 14px;
  margin-bottom: 8px;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--border-base);
  background: var(--bg-card);
}
.ph-item-row.ph-item-correct {
  border-left-color: var(--el-color-success);
  background: var(--el-color-success-light);
}
.ph-item-row.ph-item-wrong {
  border-left-color: var(--el-color-danger);
  background: var(--el-color-danger-light);
}
.ph-item-row.ph-item-pending {
  border-left-color: var(--el-color-warning);
  background: var(--el-color-warning-light);
}
.ph-item-row.ph-item-empty {
  border-left-color: var(--text-disabled);
  background: var(--bg-hover);
}
.ph-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.ph-item-num {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--text-primary, var(--text-primary));
}
.ph-item-type-tag {
  font-size: var(--fs-xs);
  color: var(--text-disabled, var(--text-disabled));
  background: var(--bg-secondary, var(--bg-secondary));
  padding: 2px 8px;
  border-radius: var(--radius-full, 999px);
}
.ph-item-body {
  font-size: var(--fs-sm);
  line-height: 1.6;
}
.ph-item-row-ans {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.ph-ans-label {
  color: var(--text-secondary, var(--text-secondary));
  flex-shrink: 0;
}
.ph-ans-value {
  color: var(--text-primary, var(--text-primary));
  font-weight: 500;
  word-break: break-all;
}
.ph-ans-strikethrough {
  text-decoration: line-through;
  color: var(--el-color-danger);
}
.ph-correct-row {
  margin-top: 2px;
}
.ph-ans-correct {
  color: var(--el-color-success);
  font-weight: 600;
  text-decoration: none;
}
.ph-item-explain {
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--text-secondary));
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--border-base, #e8e8ed);
}
.ph-explain-label {
  font-weight: 600;
  color: var(--text-regular, var(--text-regular));
}
.ph-match-badge {
  font-size: 10px;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 1px 6px;
  border-radius: var(--radius-full, 999px);
}
</style>
