<template>
  <div class="content-renderer">
    <!-- knowledge_card -->
    <div v-if="type === 'knowledge_card'" class="ct-knowledge-card">
      <div class="ct-header">
        <span class="ct-subject">{{ data.subject }}</span>
        <span v-if="data.exam_weight" class="ct-weight">{{ data.exam_weight }}</span>
      </div>
      <h3 class="ct-title">{{ data.title }}</h3>
      <p class="ct-summary">{{ data.summary }}</p>
      <div v-if="data.key_points" class="ct-key-points">
        <div v-for="(kp, i) in data.key_points" :key="i" class="ct-kp-item">
          <span class="ct-kp-label">{{ kp.label }}</span>
          <span class="ct-kp-content">{{ kp.content }}</span>
        </div>
      </div>
      <div v-if="data.examples" class="ct-examples">
        <div v-for="(ex, i) in data.examples" :key="i" class="ct-example">
          <code>{{ ex.input }}</code> → <strong>{{ ex.output }}</strong>
          <p class="ct-explain">{{ ex.explanation }}</p>
        </div>
      </div>
      <div v-if="data.common_mistakes" class="ct-mistakes">
        <span v-for="(m, i) in data.common_mistakes" :key="i" class="ct-mistake-tag">{{ m }}</span>
      </div>
      <div v-if="data.related_knowledge" class="ct-related">
        <span v-for="(rk, i) in data.related_knowledge" :key="i" class="ct-related-tag">{{
          rk
        }}</span>
      </div>
    </div>

    <!-- vocabulary_list -->
    <div v-else-if="type === 'vocabulary_list'" class="ct-vocabulary">
      <div class="ct-header">
        <span class="ct-subject">{{ data.subject }}</span>
      </div>
      <h3 class="ct-title">{{ data.title }}</h3>
      <div class="ct-vocab-grid">
        <div v-for="(item, i) in data.items" :key="i" class="ct-vocab-card">
          <div class="ct-vocab-word">
            {{ item.word }} <span class="ct-pos">{{ item.pos }}</span>
          </div>
          <div class="ct-vocab-meaning">{{ item.meaning }}</div>
          <div class="ct-vocab-example">{{ item.example }}</div>
          <div class="ct-vocab-example-cn">{{ item.example_cn }}</div>
        </div>
      </div>
    </div>

    <!-- exercise_set -->
    <div v-else-if="type === 'exercise_set'" class="ct-exercise">
      <div class="ct-header">
        <span class="ct-subject">{{ data.subject }}</span>
        <span class="ct-diff">难度 {{ data.difficulty }}/5</span>
        <span v-if="totalQuestions > 0" class="ct-score">
          得分 {{ correctCount }}/{{ totalQuestions }}
        </span>
      </div>
      <h3 class="ct-title">{{ data.title }}</h3>
      <!-- OCR 上传区（数学计算题） -->
      <div v-if="data._ocrReady" class="ct-ocr-upload">
        <el-upload
          :show-file-list="false"
          :before-upload="
            (f) => {
              uploadOcr(f);
              return false;
            }
          "
          accept="image/*"
        >
          <el-button size="small" :loading="ocrLoading">
            <el-icon><Camera /></el-icon> 拍照上传计算过程
          </el-button>
        </el-upload>
        <div v-if="ocrResult" class="ct-ocr-result">{{ ocrResult }}</div>
      </div>
      <div v-for="(q, i) in data.questions" :key="q.id" class="ct-question-card">
        <div class="ct-q-stem">{{ i + 1 }}. <span v-html="renderMath(q.stem)" /></div>
        <div v-if="q.options" class="ct-q-options">
          <div
            v-for="opt in q.options"
            :key="opt"
            :class="[
              'ct-q-option',
              {
                'ct-q-selected': answers[q.id] === opt,
                'ct-q-correct': answers[q.id] && q.answer && isCorrect(q, opt),
                'ct-q-wrong': answers[q.id] === opt && q.answer && !isCorrect(q, opt),
              },
            ]"
            @click="selectAnswer(q.id, opt, q)"
          >
            <span v-html="renderMath(typeof opt === 'string' ? opt : opt.text || opt.label || String(opt))" />
          </div>
        </div>
        <div v-if="answers[q.id]" class="ct-q-feedback">
          <span
            v-if="q.answer && isCorrect(q, answers[q.id])"
            class="ct-q-result correct"
          >✓ 正确</span>
          <span v-else-if="q.answer" class="ct-q-result wrong">✗ 正确答案：<span v-html="renderMath(q.answer)" /></span>
          <div v-if="q.explanation" class="ct-q-explanation" v-html="renderMath(q.explanation)" />
          <el-button
            size="small"
            text
            type="primary"
            class="ct-q-redo"
            @click="redoAnswer(q.id)"
          >
            重做
          </el-button>
        </div>
      </div>
    </div>

    <!-- step_by_step -->
    <div v-else-if="type === 'step_by_step'" class="ct-steps">
      <div class="ct-header">
        <span class="ct-subject">{{ data.subject }}</span>
      </div>
      <h3 class="ct-title">{{ data.title }}</h3>
      <div v-for="s in data.steps" :key="s.num" class="ct-step">
        <div class="ct-step-num">{{ s.num }}</div>
        <div class="ct-step-body">
          <div class="ct-step-title">{{ s.title }}</div>
          <div class="ct-step-content">{{ s.content }}</div>
          <div v-if="s.formula" class="ct-step-formula">{{ s.formula }}</div>
        </div>
      </div>
      <div v-if="data.answer" class="ct-answer">答案：{{ data.answer }}</div>
    </div>

    <!-- analysis_report -->
    <div v-else-if="type === 'analysis_report'" class="ct-report">
      <div class="ct-header">
        <span class="ct-subject">{{ data.subject }}</span>
      </div>
      <h3 class="ct-title">{{ data.title }}</h3>
      <p class="ct-summary">{{ data.summary }}</p>
      <div v-if="data.metrics" class="ct-metrics">
        <div v-for="m in data.metrics" :key="m.label" class="ct-metric">
          <span class="ct-metric-label">{{ m.label }}</span>
          <span class="ct-metric-value">{{ m.value }}</span>
          <span
            v-if="m.trend"
            :class="['ct-metric-trend', m.trend.startsWith('-') ? 'down' : 'up']"
          >{{ m.trend }}</span>
        </div>
      </div>
      <div v-if="data.weak_points" class="ct-weak-points">
        <div v-for="w in data.weak_points" :key="w.node" class="ct-weak-item">
          <span class="ct-weak-node">{{ w.node }}</span>
          <el-progress
            :percentage="w.mastery"
            :status="w.mastery < 50 ? 'exception' : 'warning'"
            :stroke-width="12"
          />
          <span :class="['ct-weak-severity', w.severity]">{{
            w.severity === 'high' ? '高优' : '中优'
          }}</span>
        </div>
      </div>
    </div>

    <!-- comparison -->
    <div v-else-if="type === 'comparison'" class="ct-comparison">
      <h3 class="ct-title">{{ data.title }}</h3>
      <el-table
        :data="data.rows"
        size="small"
        border
        max-height="400"
      >
        <el-table-column :label="data.headers[0]" prop="label" width="140" />
        <el-table-column v-for="(h, i) in data.headers.slice(1)" :key="i" :label="h">
          <template #default="{ row }">{{ row.values[i] }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- learning_path -->
    <div v-else-if="type === 'learning_path'" class="ct-path">
      <div class="ct-header">
        <span class="ct-subject">{{ data.subject }}</span>
      </div>
      <h3 class="ct-title">{{ data.title }}</h3>
      <div class="ct-path-nodes">
        <div v-for="n in data.nodes" :key="n.name" :class="['ct-path-node', n.status]">
          <div class="ct-path-node-icon">
            {{ n.status === 'mastered' ? '✓' : n.status === 'in_progress' ? '●' : '○' }}
          </div>
          <div class="ct-path-node-name">{{ n.name }}</div>
          <el-progress
            v-if="n.mastery > 0"
            type="circle"
            :percentage="n.mastery"
            :width="40"
            :stroke-width="4"
          />
        </div>
      </div>
    </div>

    <!-- unknown type fallback -->
    <div v-else class="ct-fallback">
      <pre>{{ rawContent }}</pre>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, ref } from 'vue';
import { useUserStore } from '@/stores/user';
import { renderMath } from '@/composables/useQuestionHelpers';
import { gradeOcr, submitAnswer as apiSubmitAnswer } from '@/api/agent';

const props = defineProps({
  content: { type: String, default: '' },
});

const userStore = useUserStore();
const answers = reactive({});
const ocrLoading = ref(false);
const ocrResult = ref('');

/** 判断选项是否匹配正确答案（支持 A.xxx / A 两种格式） */
function isCorrect(q, opt) {
  if (!q.answer) return false;
  const ans = q.answer.trim();
  // 精确匹配选项文本 或 匹配选项前缀（如 A.)
  return opt === ans || opt.startsWith(ans + '.') || opt.startsWith(ans + ' ');
}

function selectAnswer(qid, val, q) {
  if (answers[qid]) return;
  answers[qid] = val;
  // 异步回传作答记录
  submitAnswer(qid, val, q);
}

function redoAnswer(qid) {
  delete answers[qid];
}

/** 计算总题数 */
const totalQuestions = computed(() => {
  try {
    const d = JSON.parse(props.content);
    return d.questions?.length || 0;
  } catch {
    return 0;
  }
});

/** 计算答对数 */
const correctCount = computed(() => {
  try {
    const d = JSON.parse(props.content);
    if (!d.questions) return 0;
    return d.questions.filter((q) => {
      const a = answers[q.id];
      return a && isCorrect(q, a);
    }).length;
  } catch {
    return 0;
  }
});

/** OCR 拍照上传 */
async function uploadOcr(file) {
  ocrLoading.value = true;
  ocrResult.value = '';
  try {
    const form = new FormData();
    form.append('image', file);
    form.append('question', data.value.title || '');
    const resp = await gradeOcr(form);
    if (resp.code === 200 && resp.data) {
      ocrResult.value = resp.data.comment || resp.data.recognized || JSON.stringify(resp.data);
    } else {
      ocrResult.value = 'OCR 识别失败，请重试';
    }
  } catch {
    ocrResult.value = '网络错误，请重试';
  } finally {
    ocrLoading.value = false;
  }
}

/** 异步回传作答到后端（轻量记录） */
async function submitAnswer(qid, selected, q) {
  try {
    await apiSubmitAnswer({
      questionId: qid,
      selectedAnswer: selected,
      correctAnswer: q.answer || '',
      isCorrect: q.answer ? isCorrect(q, selected) : null,
      questionStem: q.stem?.substring(0, 200),
    });
  } catch {
    /* 静默失败，不影响前端体验 */
  }
}

const rawContent = computed(() => props.content);

const parsed = computed(() => {
  try {
    return JSON.parse(props.content);
  } catch {
    return null;
  }
});

const data = computed(() => parsed.value || {});

const type = computed(() => {
  if (!parsed.value) return '';
  return parsed.value.type || '';
});
</script>

<style scoped>
.content-renderer {
  font-size: 14px;
  line-height: 1.7;
}
.ct-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.ct-subject {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-section);
  padding: 2px 8px;
  border-radius: 4px;
}
.ct-weight {
  font-size: 12px;
  color: var(--primary-color);
}
.ct-title {
  margin: 0 0 12px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}
.ct-summary {
  color: var(--text-secondary);
  margin-bottom: 12px;
}
.ct-key-points {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.ct-kp-item {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-section);
  border-radius: 8px;
}
.ct-kp-label {
  font-weight: 600;
  white-space: nowrap;
  color: var(--primary-color);
  min-width: 60px;
}
.ct-kp-content {
  color: var(--text-primary);
}
.ct-examples {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.ct-example {
  padding: 8px 12px;
  background: var(--bg-section);
  border-radius: 8px;
}
.ct-explain {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}
.ct-mistakes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.ct-mistake-tag {
  background: color-mix(in srgb, var(--el-color-warning) 12%, transparent);
  color: var(--el-color-warning);
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
}
.ct-related {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.ct-related-tag {
  background: var(--bg-section);
  color: var(--primary-color);
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
}
.ct-vocab-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}
.ct-vocab-card {
  padding: 12px;
  background: var(--bg-section);
  border-radius: 8px;
}
.ct-vocab-word {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.ct-pos {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 400;
}
.ct-vocab-meaning {
  color: var(--text-primary);
  margin: 4px 0;
}
.ct-vocab-example {
  font-style: italic;
  color: var(--text-secondary);
  font-size: 13px;
}
.ct-vocab-example-cn {
  color: var(--text-secondary);
  font-size: 12px;
}
.ct-question-card {
  padding: 12px;
  background: var(--bg-section);
  border-radius: 8px;
  margin-bottom: 10px;
}
.ct-q-stem {
  font-weight: 600;
  margin-bottom: 8px;
}
.ct-q-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}
.ct-q-option {
  padding: 6px 10px;
  background: var(--bg-color);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  border: 1.5px solid transparent;
}
.ct-q-option:hover {
  border-color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 6%, var(--bg-color));
}
.ct-q-selected {
  border-color: var(--primary-color);
}
.ct-q-correct {
  border-color: var(--success-color) !important;
  background: color-mix(in srgb, var(--success-color) 10%, var(--bg-color)) !important;
}
.ct-q-wrong {
  border-color: var(--el-color-danger) !important;
  background: color-mix(in srgb, var(--el-color-danger) 10%, var(--bg-color)) !important;
}
.ct-q-feedback {
  margin-top: 8px;
}
.ct-q-result {
  font-weight: 700;
  font-size: 14px;
}
.ct-q-result.correct {
  color: var(--success-color);
}
.ct-q-result.wrong {
  color: var(--el-color-danger);
}
.ct-q-explanation {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-color);
  padding: 6px 10px;
  border-radius: 6px;
}
.ct-q-hint {
  font-size: 13px;
  color: var(--text-secondary);
}
.ct-score {
  margin-left: auto;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 10%, transparent);
  padding: 2px 10px;
  border-radius: 12px;
}
.ct-q-redo {
  margin-top: 4px;
}
.ct-ocr-upload {
  margin-bottom: 12px;
  padding: 10px 14px;
  background: var(--bg-section);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.ct-ocr-result {
  font-size: 13px;
  color: var(--text-secondary);
  max-width: 400px;
}
.ct-step {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.ct-step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}
.ct-step-body {
  flex: 1;
}
.ct-step-title {
  font-weight: 600;
  color: var(--text-primary);
}
.ct-step-content {
  color: var(--text-secondary);
}
.ct-step-formula {
  background: var(--bg-color);
  padding: 6px 10px;
  border-radius: 6px;
  margin-top: 4px;
  font-family: monospace;
}
.ct-answer {
  margin-top: 12px;
  padding: 8px 12px;
  background: color-mix(in srgb, var(--el-color-success) 12%, transparent);
  border-radius: 8px;
  color: var(--el-color-success);
  font-weight: 600;
}
.ct-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.ct-metric {
  padding: 12px;
  background: var(--bg-section);
  border-radius: 8px;
  text-align: center;
}
.ct-metric-label {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
}
.ct-metric-value {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.ct-metric-trend {
  font-size: 12px;
  font-weight: 600;
}
.ct-metric-trend.up {
  color: var(--success-color);
}
.ct-metric-trend.down {
  color: var(--el-color-danger);
}
.ct-weak-points {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ct-weak-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--bg-section);
  border-radius: 8px;
}
.ct-weak-node {
  flex: 1;
  font-weight: 500;
}
.ct-weak-severity {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}
.ct-weak-severity.high {
  background: color-mix(in srgb, var(--el-color-danger) 10%, transparent);
  color: var(--el-color-danger);
}
.ct-weak-severity.medium {
  background: color-mix(in srgb, var(--el-color-warning) 12%, transparent);
  color: var(--el-color-warning);
}
.ct-path-nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ct-path-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--bg-section);
  border-radius: 8px;
}
.ct-path-node-icon {
  font-size: 18px;
  width: 28px;
  text-align: center;
}
.ct-path-node.mastered {
  border-left: 3px solid var(--success-color);
}
.ct-path-node.mastered .ct-path-node-icon {
  color: var(--success-color);
}
.ct-path-node.in_progress {
  border-left: 3px solid var(--primary-color);
}
.ct-path-node.in_progress .ct-path-node-icon {
  color: var(--primary-color);
}
.ct-path-node.locked {
  opacity: 0.6;
}
.ct-path-node-name {
  flex: 1;
  font-weight: 500;
}
.ct-fallback {
  background: var(--bg-section);
  padding: 12px;
  border-radius: 8px;
}
.ct-fallback pre {
  margin: 0;
  white-space: pre-wrap;
  font-size: 12px;
}
</style>
