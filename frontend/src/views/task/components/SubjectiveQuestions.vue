<template>
  <div class="subjective-area">
    <el-button size="small" type="primary" @click="visible = true">+ 添加主观题</el-button>
    <span v-if="questions.length > 0" class="subjective-count">
      已添加 {{ questions.length }} 道
    </span>
    <div v-if="questions.length" class="subjective-list">
      <div v-for="(q, i) in questions" :key="q._cid" class="subjective-item">
        <span class="sq-index">{{ i+1 }}.</span>
        <el-tag size="small">{{ typeLabel(q.questionType) }}</el-tag>
        <span class="sq-text">{{ q.questionText }}</span>
        <el-button
          size="small"
          text
          type="danger"
          @click="removeItem(i)"
        >
          删除
        </el-button>
      </div>
    </div>

    <el-dialog
      v-model="visible"
      title="添加主观题"
      width="500px"
      append-to-body
    >
      <el-form label-position="top" size="small">
        <el-form-item label="题型">
          <el-select v-model="form.questionType" style="width:100%">
            <el-option label="简答题" value="SHORT_ANSWER" />
            <el-option label="编程题" value="PROGRAMMING" />
            <el-option label="论述题" value="ESSAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容">
          <el-input
            v-model="form.questionText"
            type="textarea"
            :rows="3"
            placeholder="请输入题目内容"
          />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input
            v-model="form.correctAnswer"
            type="textarea"
            :rows="2"
            placeholder="参考答案（可选）"
          />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number
            v-model="form.score"
            :min="1"
            :max="50"
            style="width:120px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="addItem">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  questions: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:questions'])

const visible = ref(false)
const form = reactive({
  questionText: '', correctAnswer: '', questionType: 'SHORT_ANSWER', score: 10,
})

const typeLabel = (t) =>
  ({ SHORT_ANSWER: '简答', PROGRAMMING: '编程', ESSAY: '论述' })[t] || t

const addItem = () => {
  if (!form.questionText.trim()) { ElMessage.warning('请输入题目内容'); return }
  const item = {
    _cid: 'sub_' + Date.now(),
    questionText: form.questionText,
    questionType: form.questionType,
    correctAnswer: form.correctAnswer,
    score: form.score,
    source: 'manual',
  }
  emit('update:questions', [...props.questions, item])
  form.questionText = ''
  form.correctAnswer = ''
  visible.value = false
  ElMessage.success('主观题已添加')
}

const removeItem = (i) => {
  const arr = [...props.questions]
  arr.splice(i, 1)
  emit('update:questions', arr)
}
</script>

<style scoped>
.subjective-area { padding: 8px 0; }
.subjective-count { margin-left: 10px; font-size: var(--fs-xs); color: var(--text-secondary); }
.subjective-list { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; }
.subjective-item { display: flex; align-items: center; gap: 8px; padding: 6px 10px; background: var(--bg-section); border-radius: 6px; font-size: var(--fs-sm); }
.sq-index { font-weight: 600; color: var(--text-secondary); }
.sq-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

@media (max-width: 768px) {
  :deep(.el-form-item .el-input),
  :deep(.el-form-item .el-select) {
    width: 100%;
  }
}
</style>
