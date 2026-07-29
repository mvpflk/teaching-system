<template>
  <div class="qp-body qp-grade-stage">
    <div class="qp-grade-student">
      <div class="qp-gs-avatar">{{ pickedStudent?.studentName?.charAt(0) || '?' }}</div>
      <div class="qp-gs-name">{{ pickedStudent?.studentName }}</div>
    </div>
    <div class="qp-grade-question">
      <div class="qp-grade-question-label">题目：</div>
      <div class="qp-grade-question-text" v-html="renderMath(questionText)"></div>
    </div>
    <div v-if="studentAnswer" class="qp-answer">
      <div class="qp-answer-label">学生回答：</div>
      <div class="qp-answer-text">{{ studentAnswer }}</div>
    </div>
    <div v-else class="qp-answer-waiting"><el-icon><Loading /></el-icon>等待学生提交答案...</div>

    <!-- 分数选择器 -->
    <div class="qp-grade-score-select">
      <span class="qp-grade-score-label">分值：</span>
      <el-radio-group v-model="score" size="small">
        <el-radio-button :value="1">1分</el-radio-button>
        <el-radio-button :value="2">2分</el-radio-button>
        <el-radio-button :value="3">3分</el-radio-button>
      </el-radio-group>
    </div>

    <div class="qp-grade-btns">
      <el-button type="success" size="large" :loading="grading === 1" class="qp-grade-btn" @click="$emit('grade', 1, score)">
        <el-icon><CircleCheck /></el-icon> 回答正确<span class="qp-grade-score">+{{ score }}分</span>
      </el-button>
      <el-button type="warning" size="large" :loading="grading === 2" class="qp-grade-btn" @click="$emit('grade', 2, score)">
        <el-icon><Warning /></el-icon> 部分正确<span class="qp-grade-score">+{{ score }}分</span>
      </el-button>
      <el-button type="danger" size="large" :loading="grading === 0" class="qp-grade-btn" @click="$emit('grade', 0, 0)">
        <el-icon><CircleClose /></el-icon> 回答错误<span class="qp-grade-score">→ 错题本</span>
      </el-button>
    </div>
    <div class="qp-grade-skip"><button class="qp-link" @click="$emit('skip')">跳过评分，回到选题</button></div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { CircleCheck, Warning, CircleClose, Loading } from '@element-plus/icons-vue'
import { renderMath } from '@/composables/useQuestionHelpers'

defineProps({
  pickedStudent: Object,
  questionText: String,
  studentAnswer: String,
  grading: Number
})

defineEmits(['grade', 'skip'])

const score = ref(1)
</script>

<style scoped>
.qp-body { flex: 1; overflow-y: auto; }
.qp-grade-stage { text-align: center; }
.qp-grade-student { margin: 20px 0 20px; }
.qp-gs-avatar { width: 88px; height: 88px; border-radius: var(--radius-full); background: var(--primary-light); display: flex; align-items: center; justify-content: center; font-size: 40px; font-weight: 800; color: var(--primary-color); margin: 0 auto 12px; }
.qp-gs-name { font-size: 28px; font-weight: 800; color: var(--text-primary); }
.qp-grade-question { background: var(--bg-section); border-radius: var(--radius-md); border: 0.5px solid var(--border-light); padding: 14px var(--spacing-md); margin-top: var(--spacing-sm); text-align: left; }
.qp-grade-question-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: var(--spacing-xs); font-weight: 600; }
.qp-grade-question-text { font-size: var(--fs-md); color: var(--text-regular); line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.qp-answer { background: var(--bg-section); border-radius: var(--radius-md); border: 0.5px solid var(--border-light); padding: 14px var(--spacing-md); margin-top: 12px; text-align: left; }
.qp-answer-label { font-size: var(--fs-xs); color: var(--text-secondary); margin-bottom: var(--spacing-xs); }
.qp-answer-text { font-size: var(--fs-md); color: var(--text-regular); line-height: 1.6; white-space: pre-wrap; }
.qp-answer-waiting { text-align: center; padding: 12px; color: var(--text-secondary); font-size: var(--fs-sm); margin-top: 10px; display: flex; align-items: center; justify-content: center; gap: var(--spacing-xs); }
.qp-grade-score-select { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 16px 0 8px; }
.qp-grade-score-label { font-size: var(--fs-sm); color: var(--text-secondary); font-weight: 600; }
.qp-grade-btns { display: flex; gap: var(--spacing-md); justify-content: center; flex-wrap: wrap; }
.qp-grade-btn { min-width: 160px; padding: 20px 28px; border-radius: var(--radius-lg); height: auto; flex-direction: column; gap: var(--spacing-xs); font-size: var(--fs-lg); }
.qp-grade-score { font-size: var(--fs-sm); opacity: 0.85; }
.qp-grade-skip { margin-top: var(--spacing-lg); padding-top: 14px; border-top: 0.5px solid var(--border-light); text-align: center; }
.qp-link { display: inline-flex; align-items: center; gap: 5px; font-size: var(--fs-sm); font-family: inherit; color: var(--text-secondary); padding: 6px 14px; border-radius: var(--radius-md); border: 0.5px solid transparent; background: transparent; cursor: pointer; transition: all var(--transition-base); margin-top: 10px; }
.qp-link:hover { color: var(--primary-color); background: var(--primary-light); border-color: var(--primary-color); }
</style>
