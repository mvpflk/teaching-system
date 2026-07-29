<template>
  <div v-if="isStarTask" class="ga-score-row">
    <span class="ga-label">评分:</span>
    <el-rate v-model="scoreModel" :max="5" show-score show-text :texts="starTexts" size="large" />
  </div>
  <div v-else class="ga-score-row">
    <span class="ga-label">得分:</span>
    <el-input-number v-model="scoreModel" :min="0" :max="maxScore" :precision="1" size="small" style="width:130px" />
    <span class="ga-hint">/ {{ maxScore || 100 }}</span>
    <el-button v-if="subjectiveCount > 0" size="small" type="info" :loading="aiGrading" style="margin-left:8px" @click="$emit('ai-grade')">
      <el-icon><MagicStick /></el-icon>AI 评分建议
    </el-button>
  </div>
  <div class="ga-score-row">
    <span class="ga-label">评语:</span>
    <el-input v-model="commentModel" type="textarea" :rows="2" placeholder="可选评语" size="small" />
  </div>
  <div v-if="explanation" class="ga-score-row">
    <span class="ga-label">评分理由:</span>
    <span class="ga-explanation-text">{{ explanation }}</span>
  </div>
  <div class="ga-quick-row">
    <span class="ga-label" />
    <div class="quick-tags">
      <el-tag v-for="qc in quickComments" :key="qc.id" class="quick-tag" :disable-transitions="false" @click="$emit('apply-quick-comment', qc.commentText)">
        {{ qc.commentText }}
      </el-tag>
      <el-button size="small" :icon="Setting" circle title="管理快捷评语" @click="$emit('manage-quick-comments')" />
    </div>
  </div>
  <div class="ga-actions">
    <span class="ga-shortcuts"><kbd>←</kbd><kbd>→</kbd>切换 <kbd>Enter</kbd>保存 <kbd>Esc</kbd>跳过</span>
    <span v-if="autoSaving" class="ga-autosave">自动保存中...</span>
    <el-button size="small" @click="$emit('skip')">跳过</el-button>
    <el-select v-if="quickComments.length" v-model="batchCommentModel" size="small" style="width:160px" placeholder="选择评语">
      <el-option v-for="qc in quickComments" :key="qc.id" :value="qc.commentText" :label="qc.commentText" />
    </el-select>
    <el-button size="small" type="warning" :loading="batchSaving" @click="$emit('batch-mark-all')">批量标记已阅 ({{ ungradedCount }})</el-button>
    <RubricGradingPanel v-if="taskRubric" :rubric="taskRubric" @update:scores="$emit('update:rubric-scores', $event)" />
    <el-button size="small" type="primary" :loading="saving" @click="$emit('save')">保存并下一人</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Setting, MagicStick } from '@element-plus/icons-vue'
import RubricGradingPanel from '@/components/grading/RubricGradingPanel.vue'

const props = defineProps({
  score: { type: Number, default: 0 },
  comment: { type: String, default: '' },
  explanation: { type: String, default: '' },
  maxScore: { type: Number, default: 100 },
  isStarTask: { type: Boolean, default: false },
  subjectiveCount: { type: Number, default: 0 },
  aiGrading: { type: Boolean, default: false },
  quickComments: { type: Array, default: () => [] },
  autoSaving: { type: Boolean, default: false },
  batchComment: { type: String, default: '已阅' },
  taskRubric: { type: Object, default: null },
  ungradedCount: { type: Number, default: 0 },
  saving: { type: Boolean, default: false },
  batchSaving: { type: Boolean, default: false },
})

const emit = defineEmits([
  'update:score', 'update:comment', 'update:batchComment',
  'save', 'skip', 'ai-grade', 'apply-quick-comment',
  'manage-quick-comments', 'batch-mark-all', 'update:rubric-scores',
])

const scoreModel = computed({
  get: () => props.score,
  set: (v) => emit('update:score', v),
})

const commentModel = computed({
  get: () => props.comment,
  set: (v) => emit('update:comment', v),
})

const batchCommentModel = computed({
  get: () => props.batchComment,
  set: (v) => emit('update:batchComment', v),
})

const starTexts = ['很差', '较差', '一般', '良好', '优秀']
</script>

<style scoped>
.ga-score-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.ga-hint { font-size: var(--fs-xs); color: var(--text-secondary); }
.ga-quick-row { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.quick-tags { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; flex: 1; }
.quick-tag { cursor: pointer; user-select: none; }
.quick-tag:hover { opacity: 0.8; }
.ga-actions { display: flex; gap: 10px; align-items: center; justify-content: flex-end; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--border-light); }
.ga-shortcuts { font-size: var(--fs-xs); color: var(--text-secondary); margin-right: auto; opacity: 0.7; }
.ga-shortcuts kbd { display: inline-block; padding: 1px 5px; font-size: 10px; font-family: monospace; background: var(--bg-section); border: 1px solid var(--border-light); border-radius: 3px; margin: 0 2px; }
.ga-autosave { font-size: var(--fs-xs); color: var(--el-color-info); animation: autosave-pulse 1s ease-in-out infinite; margin-right: auto; }
@keyframes autosave-pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
.ga-explanation-text { font-size: var(--fs-sm); color: var(--text-primary); font-weight: 500; }
</style>
