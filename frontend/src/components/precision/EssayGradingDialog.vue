<template>
  <el-dialog
    v-model="visible"
    title="问答题评阅"
    width="650px"
    :close-on-click-modal="false"
  >
    <div v-if="currentItem" class="eg-container">
      <div class="eg-section">
        <label class="eg-label">题目</label>
        <div class="eg-question" v-html="sanitizeHtml(currentItem.questionText || '')" />
      </div>
      <div class="eg-section">
        <label class="eg-label">学生答案</label>
        <div class="eg-answer">{{ currentItem.studentAnswer || '未作答' }}</div>
      </div>
      <div class="eg-section">
        <label class="eg-label">参考答案</label>
        <div class="eg-ref">{{ currentItem.correctAnswer || '暂无' }}</div>
      </div>
      <div class="eg-section">
        <label class="eg-label">评分</label>
        <div class="eg-grading">
          <el-rate
            v-model="score"
            :max="10"
            show-score
            :score-template="'{value}分'"
          />
          <div class="eg-score-tip">满分 10 分，6 分及以上为通过</div>
        </div>
      </div>
      <div class="eg-section">
        <label class="eg-label">评语（可选）</label>
        <el-input
          v-model="comment"
          type="textarea"
          :rows="3"
          placeholder="输入评语或修改建议..."
        />
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评分</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { sanitizeHtml } from '@/utils/markdown'

const props = defineProps({
  modelValue: Boolean,
  item: { type: Object, default: null },
  studentId: { type: [Number, String], default: null },
  submitId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue', 'submitted'])

const visible = ref(false)
const currentItem = ref(null)
const score = ref(5)
const comment = ref('')
const submitting = ref(false)

watch(() => props.modelValue, (v) => {
  visible.value = v
  if (v && props.item) {
    currentItem.value = props.item
    score.value = 5
    comment.value = ''
  }
})
watch(visible, (v) => emit('update:modelValue', v))

async function handleSubmit() {
  if (score.value < 1) { ElMessage.warning('请评分'); return }
  submitting.value = true
  try {
    // 调用教师评分接口（扩展预留）
    ElMessage.success(`已评分：${score.value}/10 分`)
    emit('submitted', { itemId: currentItem.value?.questionId, score: score.value, comment: comment.value })
    visible.value = false
  } catch { ElMessage.error('提交失败') }
  submitting.value = false
}
</script>

<style scoped>
.eg-container { font-size: var(--fs-md); }
.eg-section { margin-bottom: 16px; }
.eg-label { display: block; font-weight: 600; color: var(--text-primary, var(--text-primary)); margin-bottom: 6px; font-size: var(--fs-sm); }
.eg-question { padding: 10px; background: #f8f9fa; border-radius: 6px; line-height: 1.7; font-size: var(--fs-md); }
.eg-answer { padding: 10px; background: #fff8e1; border-radius: 6px; border-left: 3px solid #f9a825; line-height: 1.7; }
.eg-ref { padding: 10px; background: #e8f5e9; border-radius: 6px; border-left: 3px solid #66bb6a; line-height: 1.7; color: var(--text-secondary, var(--text-secondary)); }
.eg-grading { display: flex; flex-direction: column; gap: 4px; }
.eg-score-tip { font-size: var(--fs-xs); color: var(--text-secondary, var(--text-secondary)); }
</style>
