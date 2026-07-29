<template>
  <div class="pp-page">
    <van-nav-bar
      title="提分练习"
      left-text="返回"
      left-arrow
      fixed
      placeholder
      @click-left="$router.back()"
    />

    <van-tabs
      v-model:active="tabIndex"
      color="var(--primary-color)"
      title-active-color="var(--primary-color)"
      sticky
    >
      <van-tab title="考点地图">
        <div class="pp-section">
          <div
            v-for="node in syllabusNodes"
            :key="node.nodeId"
            class="pp-node"
            :class="'node-' + (node.status || 'weak')"
            @click="startNodePractice(node)"
          >
            <div class="pp-node-head">
              <span class="pp-node-name">{{ node.name }}</span>
              <span class="pp-node-pct">{{ node.masteryPercent || 0 }}%</span>
            </div>
            <van-progress
              :percentage="node.masteryPercent || 0"
              stroke-width="6"
              :show-pivot="false"
              :color="
                node.status === 'mastered'
                  ? 'var(--el-color-success)'
                  : node.status === 'learning'
                    ? 'var(--el-color-warning)'
                    : 'var(--text-disabled)'
              "
            />
          </div>
          <van-empty
            v-if="!syllabusNodes.length"
            description="暂无考点数据，先完成诊断测试"
            :image-size="48"
          />
        </div>
      </van-tab>

      <van-tab title="答题">
        <div v-if="!currentNode" class="pp-section">
          <van-empty description="在「考点地图」中点击知识点开始练习" :image-size="48" />
        </div>
        <div v-else class="pp-section">
          <div class="pp-card">
            <div class="pp-card-header">
              <span class="pp-node-badge">{{ currentNode.name }}</span>
              <span
                v-if="questions.length"
                class="pp-q-progress"
              >第 {{ qIndex + 1 }}/{{ questions.length }} 题</span>
            </div>

            <div v-if="questions.length" class="pp-question">
              <div
                class="pp-q-header"
                style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px"
              >
                <DifficultyBadge
                  :difficulty-level="questions[qIndex]?.difficultyLevel"
                  :tier="questions[qIndex]?.tier"
                />
              </div>
              <p
                class="pp-q-text"
                v-html="sanitizeMathHtml(renderMath(questions[qIndex]?.questionText || ''))"
              ></p>

              <div v-if="questions[qIndex]?.options?.length" class="pp-opts">
                <van-radio-group v-model="currentAnswer">
                  <div
                    v-for="(opt, oi) in questions[qIndex].options"
                    :key="oi"
                    class="pp-opt"
                    :class="{ selected: currentAnswer === String.fromCharCode(65 + oi) }"
                    @click="currentAnswer = String.fromCharCode(65 + oi)"
                  >
                    <span class="pp-opt-letter">{{ String.fromCharCode(65 + oi) }}</span>
                    <span
                      class="pp-opt-text"
                      v-html="sanitizeMathHtml(renderMath(stripOptionPrefix(opt)))"
                    ></span>
                    <van-radio :name="String.fromCharCode(65 + oi)" />
                  </div>
                </van-radio-group>
              </div>
              <van-field
                v-else
                v-model="currentAnswer"
                type="text"
                placeholder="输入你的答案..."
                class="pp-input"
              />
            </div>

            <div v-if="hints.length" class="pp-hints">
              <van-collapse v-model="activeHint" accordion>
                <van-collapse-item title="提示1 · 指出考点（不扣分）" name="1" class="pp-hint-item">
                  <p class="pp-hint-text">{{ hints[0]?.hint }}</p>
                </van-collapse-item>
                <van-collapse-item
                  title="提示2 · 给出第一步（扣20%分值）"
                  name="2"
                  class="pp-hint-item"
                >
                  <p class="pp-hint-text">{{ hints[1]?.hint }}</p>
                </van-collapse-item>
                <van-collapse-item
                  title="提示3 · 完整思路（扣50%分值）"
                  name="3"
                  class="pp-hint-item"
                >
                  <p class="pp-hint-text">{{ hints[2]?.hint }}</p>
                </van-collapse-item>
              </van-collapse>
            </div>
          </div>

          <div class="pp-bottom">
            <van-button
              v-if="!result"
              type="primary"
              block
              round
              :disabled="!currentAnswer?.trim()"
              style="background: var(--primary-color); border-color: var(--primary-color)"
              @click="checkAnswer"
            >
              检查答案
            </van-button>
            <div v-else class="pp-feedback">
              <van-tag :type="result.correct ? 'success' : 'danger'" size="medium" effect="plain">
                {{ result.correct ? '正确' : '错误' }}
              </van-tag>
              <span
                v-if="!result.correct"
                class="pp-correct-answer"
              >正确答案：<span v-html="sanitizeMathHtml(renderMath(result.correctAnswer))"></span></span>
              <div v-if="result.explanation" class="pp-explanation">
                <span class="pp-explanation-label">解析：</span><span v-html="sanitizeMathHtml(renderMath(result.explanation))"></span>
              </div>
            </div>

            <div class="pp-nav">
              <van-button
                size="small"
                plain
                hairline
                :disabled="qIndex === 0"
                @click="prevQuestion"
              >
                上一题
              </van-button>
              <van-button
                v-if="qIndex < questions.length - 1"
                size="small"
                type="primary"
                style="background: var(--primary-color); border-color: var(--primary-color)"
                @click="nextQuestion"
              >
                下一题
              </van-button>
              <van-button
                v-else
                size="small"
                type="success"
                style="background: var(--el-color-success); border-color: var(--el-color-success)"
                @click="finishPractice"
              >
                完成
              </van-button>
            </div>
          </div>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import { showToast } from 'vant';
import 'vant/es/toast/style';
import { getSyllabusMap, getPracticeQuestions } from '@/api/precision';
import katex from 'katex';
import 'katex/dist/katex.min.css';
import { sanitizeMathHtml } from '@/utils/markdown';
import DifficultyBadge from '@/components/common/DifficultyBadge.vue';

const route = useRoute();
const tabIndex = ref(0);
const syllabusNodes = ref([]);
const currentNode = ref(null),
  questions = ref([]),
  qIndex = ref(0);
const currentAnswer = ref(''),
  result = ref(null),
  hints = ref([]),
  activeHint = ref([]);

// 剥离选项文本中已有的字母前缀（如 "A. (-2,3)" → "(-2,3)"），避免与前端生成的字母重复
function stripOptionPrefix(text) {
  if (!text) return '';
  return String(text).replace(/^[A-H][.．、)）]\s*/, '');
}

function renderMath(text) {
  if (!text) return '';
  let html = String(text);
  // R112修复：KaTeX 失败时清理不支持的 LaTeX 命令，避免原始代码泄露
  html = html.replace(/\$\$([^$]+)\$\$/g, (_, f) => {
    try {
      return katex.renderToString(f.trim(), { displayMode: true, throwOnError: false });
    } catch {
      return sanitizeLatexFallback(f);
    }
  });
  html = html.replace(/\$([^$]+)\$/g, (_, f) => {
    try {
      return katex.renderToString(f.trim(), { displayMode: false, throwOnError: false });
    } catch {
      return sanitizeLatexFallback(f);
    }
  });
  return html;
}
// KaTeX 渲染失败兜底：移除常见LaTeX命令，保留纯文本
function sanitizeLatexFallback(formula) {
  return formula
    .replace(/\\[a-zA-Z]+(\\{[^}]*\\})*/g, '') // 移除 \command{...}
    .replace(/\\[a-zA-Z]+/g, '') // 移除 \command
    .replace(/[$_{}^~&]/g, '') // 移除特殊字符
    .trim();
}

async function loadSyllabus() {
  try {
    const res = await getSyllabusMap(route.query.subject || '数学');
    if (res.code === 200) syllabusNodes.value = res.data || [];
  } catch (e) {
    console.error('加载考点地图失败:', e);
  }
}
loadSyllabus();

async function startNodePractice(node) {
  currentNode.value = node;
  questions.value = [];
  // TODO: placeholder hints — should be fetched from API per question/node
  hints.value = [
    { hint: '本题考查' + node.name + '的基本概念和公式应用。' },
    { hint: '先明确已知条件，列出数值和未知量。' },
    { hint: '1.列出公式 2.代入数据 3.解出结果。' },
  ];
  qIndex.value = 0;
  result.value = null;
  currentAnswer.value = '';
  activeHint.value = [];
  tabIndex.value = 1;

  // 从API获取真实题目
  try {
    const res = await getPracticeQuestions(node.nodeId, route.query.subject || '数学');
    if (res.code === 200 && res.data?.length) {
      questions.value = res.data;
    } else {
      // 题库无题时给出提示
      questions.value = [
        {
          questionText: '该知识点暂未收录题目，请等待题库更新。',
          options: [],
          questionType: 'FILL_IN',
        },
      ];
    }
  } catch {
    showToast('获取题目失败');
  }
}
function checkAnswer() {
  // 选择题：比较选项字母
  const q = questions.value[qIndex.value];
  const correct =
    currentAnswer.value.trim().toUpperCase() ===
    String(q.correctAnswer || q.expected || '')
      .trim()
      .toUpperCase();
  result.value = {
    correct,
    correctAnswer: q.correctAnswer || q.expected || '未知',
    explanation: q.explanation || '',
  };
  showToast(correct ? '回答正确' : '看看正确答案');
}
function prevQuestion() {
  qIndex.value--;
  currentAnswer.value = '';
  result.value = null;
}
function nextQuestion() {
  qIndex.value++;
  currentAnswer.value = '';
  result.value = null;
}
function finishPractice() {
  showToast('练习完成');
  tabIndex.value = 0;
}
</script>

<style scoped>
.pp-page {
  min-height: 100vh;
  background: var(--bg-page, var(--bg-page));
  padding-bottom: 32px;
}
.pp-section {
  padding: 12px 16px;
}
.pp-node {
  padding: 14px 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-md, 8px);
  margin-bottom: 8px;
  cursor: pointer;
}
.pp-node:hover {
  border-color: var(--primary-color);
}
.pp-node.node-weak {
  border-left: 3px solid var(--el-color-danger);
}
.pp-node.node-learning {
  border-left: 3px solid var(--el-color-warning);
}
.pp-node.node-mastered {
  border-left: 3px solid var(--el-color-success);
}
.pp-node-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.pp-node-name {
  font-size: var(--fs-md);
  font-weight: 500;
  color: var(--text-primary, var(--text-primary));
}
.pp-node-pct {
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--text-secondary));
}

.pp-card {
  padding: 20px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-md, 8px);
  margin-bottom: 16px;
}
.pp-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pp-node-badge {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 4px 10px;
  border-radius: var(--radius-full, 999px);
}
.pp-q-progress {
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--text-secondary));
}
.pp-q-text {
  font-size: var(--fs-md);
  line-height: 1.7;
  color: var(--text-primary, var(--text-primary));
  margin: 0 0 16px;
}

.pp-opts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.pp-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--border-base, #e8e8ed);
  border-radius: var(--radius-sm, 4px);
  cursor: pointer;
  transition: background 0.1s;
}
.pp-opt:hover {
  background: var(--bg-hover, var(--bg-hover));
}
.pp-opt.selected {
  border-color: var(--primary-color);
  background: var(--primary-light);
}
.pp-opt-letter {
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
.pp-opt-text {
  flex: 1;
  font-size: var(--fs-md);
  color: var(--text-primary, var(--text-primary));
}

.pp-input {
  margin-bottom: 16px;
  border-radius: var(--radius-sm, 4px);
}

.pp-hints {
  margin: 16px 0;
}
.pp-hint-item {
  background: var(--bg-secondary, var(--bg-secondary));
  border-radius: var(--radius-sm, 4px);
  margin-bottom: 4px;
}
.pp-hint-text {
  font-size: var(--fs-sm);
  color: var(--text-regular, var(--text-regular));
  line-height: 1.6;
}

.pp-bottom {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.pp-feedback {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}
.pp-correct-answer {
  font-size: var(--fs-sm);
  color: var(--el-color-success);
  font-weight: 500;
}
.pp-explanation {
  width: 100%;
  font-size: var(--fs-xs);
  color: var(--text-secondary, var(--text-secondary));
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px dashed var(--border-base, #e8e8ed);
}
.pp-explanation-label {
  font-weight: 600;
  color: var(--text-regular, var(--text-regular));
}
.pp-nav {
  display: flex;
  gap: 8px;
  justify-content: center;
  padding-top: 4px;
}
</style>
