<template>
  <div class="qpc-active-new">
    <div class="qpc-an-stats">
      <span class="qpc-an-stat">第 <em>{{ currentQuestion ? stats.drawn : '—' }}</em> 题</span>
      <span class="qpc-an-sep">|</span>
      <span class="qpc-an-stat">已叫 <em>{{ stats.called }}</em> 人</span>
      <span class="qpc-an-sep">|</span>
      <span
        class="qpc-an-stat"
        :class="{ good: stats.total > 0 && stats.correct / stats.total >= 0.6 }"
      >正确率
        <em>{{ stats.total > 0 ? Math.round((stats.correct / stats.total) * 100) : '—' }}%</em></span>
      <span class="qpc-an-sep">|</span>
      <span class="qpc-an-stat">{{ currentStudent?.name || '—' }} 累计 <em>+{{ currentStudentScore }}</em> 分</span>
    </div>
    <div class="qpc-an-progress">
      <div class="qpc-an-progress-bar">
        <div class="qpc-an-progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
      <span class="qpc-an-progress-text">{{ stats.called }}/{{ studentPoolLength }} 已叫</span>
    </div>
    <div class="qpc-an-main">
      <div class="qpc-an-qcard">
        <div class="qpc-an-qtag">
          <el-icon><Document /></el-icon> 题目
          <el-tag
            v-if="currentQuestion?.difficultyLevel"
            size="small"
            :type="
              currentQuestion.difficultyLevel <= 2
                ? 'success'
                : currentQuestion.difficultyLevel <= 3
                  ? 'warning'
                  : 'danger'
            "
            effect="plain"
            style="margin-left: 8px"
          >
            {{
              ['', '基础', '中等', '较难', '困难', '挑战'][currentQuestion.difficultyLevel] || ''
            }}
          </el-tag>
        </div>
        <p class="qpc-an-qtext" v-html="renderMath(currentQuestion?.content || '')"></p>
        <div v-if="questionQueueLength === 0" class="qpc-an-hint">
          <el-icon><Refresh /></el-icon> 题库一轮用完，下次自动洗牌
        </div>
      </div>
      <div class="qpc-an-student">
        <div class="qpc-an-avatar-area">
          <svg v-if="countdownActive" class="qpc-countdown-ring" viewBox="0 0 100 100">
            <circle
              cx="50"
              cy="50"
              r="46"
              class="qpc-cd-bg"
            />
            <circle
              cx="50"
              cy="50"
              r="46"
              class="qpc-cd-fill"
              :style="{ strokeDashoffset: countdownDashOffset }"
            />
          </svg>
          <div
            class="qpc-an-savatar-wrap"
            :class="{ 'result-correct': lastResult === true, 'result-wrong': lastResult === false }"
          >
            <el-avatar
              v-if="currentStudent?.avatarUrl"
              :size="72"
              :src="currentStudent.avatarUrl"
              class="qpc-an-savatar-img"
            />
            <div v-else class="qpc-an-savatar">{{ (currentStudent?.name || '?').charAt(0) }}</div>
          </div>
          <span v-if="countdownActive" class="qpc-countdown-text">{{ countdownSeconds }}</span>
        </div>
        <div class="qpc-an-sname">{{ currentStudent?.name || '—' }}</div>
        <div v-if="currentStudentScore > 0" class="qpc-an-sscore">
          +{{ currentStudentScore }} 分
        </div>
      </div>
    </div>
    <div class="qpc-an-score-row">
      <span class="qpc-an-score-label">分值</span>
      <el-radio-group v-model="cScoreModel" size="small" :disabled="grading">
        <el-radio-button :value="1">1</el-radio-button>
        <el-radio-button :value="2">2</el-radio-button>
        <el-radio-button :value="3">3</el-radio-button>
      </el-radio-group>
    </div>
    <div class="qpc-an-actions">
      <button class="qpc-an-btn qpc-an-btn--good" :disabled="grading" @click="$emit('grade', 1)">
        <el-icon><CircleCheck /></el-icon> 正确 +{{ cScore }}分<span class="qpc-an-btn-key">1</span>
      </button>
      <button class="qpc-an-btn qpc-an-btn--partial" :disabled="grading" @click="$emit('grade', 2)">
        <el-icon><Warning /></el-icon> 部分正确<span class="qpc-an-btn-key">2</span>
      </button>
      <button class="qpc-an-btn qpc-an-btn--bad" :disabled="grading" @click="$emit('grade', 0)">
        <el-icon><CircleClose /></el-icon> 错误<span class="qpc-an-btn-key">3</span>
      </button>
      <div class="qpc-an-actions-divider"></div>
      <button
        class="qpc-an-btn qpc-an-btn--skip"
        :disabled="grading"
        @click="$emit('switch-student')"
      >
        <el-icon><UserFilled /></el-icon> 换人<span class="qpc-an-btn-key">S</span>
      </button>
      <button
        class="qpc-an-btn qpc-an-btn--stay"
        :disabled="grading"
        @click="$emit('switch-question')"
      >
        <el-icon><Refresh /></el-icon> 换题<span class="qpc-an-btn-key">Q</span>
      </button>
    </div>
    <div class="qpc-an-end">
      <el-button
        text
        type="danger"
        @click="$emit('end')"
      >
        <el-icon><SwitchButton /></el-icon> 结束课堂
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { renderMath } from '@/composables/useQuestionHelpers';
import {
  CircleCheck,
  Warning,
  CircleClose,
  SwitchButton,
  Refresh,
  Document,
  UserFilled,
} from '@element-plus/icons-vue';
const props = defineProps({
  currentQuestion: Object,
  currentStudent: Object,
  stats: { type: Object, required: true },
  studentPoolLength: { type: Number, default: 0 },
  cScore: { type: Number, default: 1 },
  grading: Boolean,
  lastResult: Boolean,
  currentStudentScore: { type: Number, default: 0 },
  countdownActive: Boolean,
  countdownSeconds: { type: Number, default: 30 },
  countdownDashOffset: { type: Number, default: 0 },
  questionQueueLength: { type: Number, default: 0 },
  progressPercent: { type: Number, default: 0 },
});
const emit = defineEmits(['update:cScore', 'grade', 'switch-question', 'switch-student', 'end']);
const cScoreModel = computed({ get: () => props.cScore, set: (v) => emit('update:cScore', v) });
</script>

<style scoped>
.qpc-active-new {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  padding: 24px 32px;
  gap: 20px;
}
.qpc-an-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}
.qpc-an-stat {
  font-size: var(--fs-md);
  color: var(--text-secondary);
  font-weight: 500;
}
.qpc-an-stat em {
  font-style: normal;
  font-weight: 800;
  color: var(--primary-color);
}
.qpc-an-stat.good em {
  color: var(--el-color-success);
}
.qpc-an-sep {
  color: var(--border-color);
  font-size: var(--fs-lg);
}
.qpc-an-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 32px;
  width: 100%;
  max-width: 780px;
}
.qpc-an-qcard {
  width: 100%;
  background: var(--bg-card);
  border: 0.5px solid var(--border-color);
  border-radius: var(--radius-xl);
  padding: 36px 40px;
  text-align: center;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}
.qpc-an-qtag {
  font-size: var(--fs-sm);
  color: var(--primary-color);
  font-weight: 600;
  margin-bottom: 16px;
}
.qpc-an-qtext {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.6;
  color: var(--text-primary);
  margin: 0;
}
.qpc-an-hint {
  margin-top: 16px;
  font-size: var(--fs-xs);
  color: var(--text-disabled);
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
}
.qpc-an-student {
  text-align: center;
}
.qpc-an-avatar-area {
  position: relative;
  display: inline-block;
}
.qpc-an-savatar-wrap {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s ease;
}
.qpc-an-savatar-wrap.result-correct {
  box-shadow:
    0 0 0 4px var(--el-color-success),
    0 0 24px rgba(46, 125, 50, 0.3);
  animation: pulse-correct 0.6s ease;
}
.qpc-an-savatar-wrap.result-wrong {
  box-shadow:
    0 0 0 4px var(--el-color-danger),
    0 0 24px rgba(211, 47, 47, 0.3);
  animation: pulse-wrong 0.6s ease;
}
@keyframes pulse-correct {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.12);
  }
  100% {
    transform: scale(1);
  }
}
@keyframes pulse-wrong {
  0% {
    transform: scale(1);
  }
  25% {
    transform: scale(0.92) rotate(-3deg);
  }
  50% {
    transform: scale(1.08) rotate(3deg);
  }
  75% {
    transform: scale(0.96);
  }
  100% {
    transform: scale(1);
  }
}
.qpc-an-savatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--el-color-warning), #ff9800);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 800;
  color: #fff;
}
.qpc-an-savatar-img {
  width: 80px;
  height: 80px;
  border: 3px solid var(--bg-card);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.qpc-an-sname {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin-top: 4px;
}
.qpc-an-sscore {
  font-size: var(--fs-lg);
  font-weight: 700;
  color: var(--el-color-success);
  margin-top: 4px;
  animation: score-pop 0.4s ease;
}
@keyframes score-pop {
  0% {
    transform: scale(0.5);
    opacity: 0;
  }
  60% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
.qpc-an-progress {
  width: 100%;
  max-width: 780px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.qpc-an-progress-bar {
  flex: 1;
  height: 6px;
  background: var(--bg-section);
  border-radius: 3px;
  overflow: hidden;
}
.qpc-an-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-color), var(--accent-color));
  border-radius: 3px;
  transition: width 0.5s ease;
}
.qpc-an-progress-text {
  font-size: var(--fs-xs);
  color: var(--text-secondary);
  white-space: nowrap;
  font-weight: 500;
}
.qpc-an-score-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 16px;
}
.qpc-an-score-label {
  font-size: var(--fs-sm);
  color: var(--text-secondary);
  font-weight: 600;
}
.qpc-an-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  padding-bottom: 8px;
  align-items: center;
}
.qpc-an-actions-divider {
  width: 1px;
  height: 32px;
  background: var(--border-color);
  margin: 0 4px;
}
.qpc-an-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 16px 28px;
  font-size: 17px;
  font-weight: 700;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  min-width: 140px;
  justify-content: center;
}
.qpc-an-btn .el-icon {
  font-size: 22px;
}
.qpc-an-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.qpc-an-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}
.qpc-an-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
}
.qpc-an-btn--good {
  color: #fff;
  background: var(--el-color-success);
}
.qpc-an-btn--partial {
  color: #fff;
  background: var(--el-color-warning);
}
.qpc-an-btn--bad {
  color: #fff;
  background: var(--el-color-danger);
}
.qpc-an-btn--skip {
  color: var(--text-secondary);
  background: var(--bg-section);
  border: 0.5px solid var(--border-color);
}
.qpc-an-btn--stay {
  color: #fff;
  background: var(--primary-color);
}
.qpc-an-btn-key {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.15);
  color: rgba(255, 255, 255, 0.8);
  font-size: var(--fs-xs);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qpc-an-btn--skip .qpc-an-btn-key,
.qpc-an-btn--stay .qpc-an-btn-key {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.qpc-an-end {
  padding: 8px 0;
}
.qpc-countdown-ring {
  position: absolute;
  inset: -10px;
  width: calc(100% + 20px);
  height: calc(100% + 20px);
  transform: rotate(-90deg);
}
.qpc-cd-bg {
  fill: none;
  stroke: var(--border-color);
  stroke-width: 4;
}
.qpc-cd-fill {
  fill: none;
  stroke: var(--el-color-danger);
  stroke-width: 4;
  stroke-linecap: round;
  stroke-dasharray: 289.03;
  transition: stroke-dashoffset 1s linear;
}
.qpc-countdown-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: var(--fs-2xl);
  font-weight: 800;
  color: var(--el-color-danger);
  font-family: 'JetBrains Mono', monospace;
  pointer-events: none;
}
</style>
